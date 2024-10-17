package deltix.ember.connector.grpc.syneroex;

import com.epam.deltix.gflog.api.Log;
import com.syneroex.*;
import deltix.anvil.message.ShutdownRequest;
import deltix.anvil.service.ServiceWorkerAware;
import deltix.anvil.util.CharSequenceParser;
import deltix.anvil.util.CharSequenceUtil;
import deltix.anvil.util.CloseHelper;
import deltix.anvil.util.annotation.Alphanumeric;
import deltix.anvil.util.codec.AlphanumericCodec;
import deltix.connector.common.core.BaseTradeConnector;
import deltix.connector.common.smd.Contract;
import deltix.connector.common.smd.ContractMetadata;
import deltix.connector.common.util.DefaultRequestValidator;
import deltix.ember.connector.grpc.syneroex.session.Session;
import deltix.ember.connector.grpc.syneroex.session.SessionContext;
import deltix.ember.connector.grpc.syneroex.session.SyneroexListener;
import deltix.ember.connector.grpc.syneroex.util.SyneroexMessage;
import deltix.ember.message.smd.InstrumentType;
import deltix.ember.message.trade.*;
import deltix.ember.service.connector.TradeConnectorContext;
import deltix.ember.util.CustomAttributeListBuilder;
import deltix.util.collections.generated.ObjectList;
import edu.umd.cs.findbugs.annotations.DefaultAnnotationForParameters;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.EnumSet;

@DefaultAnnotationForParameters(NonNull.class)
public class SyneroexTradeConnector extends BaseTradeConnector<Contract> implements ServiceWorkerAware {

    private static final int POST_ONLY_TAG = 6018;
    private static final int BROKER_TAG    = 6076;

    // GTD not supported because no identification when it expires
    private static final DefaultRequestValidator REQUEST_VALIDATOR =
        DefaultRequestValidator.createBuilder()
            .allowDisplayQty(true)
            .allowMinQty(false)
            .allowFastCancel(false)
            .allowFastReplace(false)
            .orderTypes(EnumSet.of(OrderType.LIMIT, OrderType.MARKET))
            .sides(EnumSet.of(Side.BUY, Side.SELL))
            .timeInForces(EnumSet.of(TimeInForce.GOOD_TILL_CANCEL, TimeInForce.FILL_OR_KILL)).build();

    private final SyneroexContext context;
    private final Log logger;
    private final Session session;

    private final CustomAttributeListBuilder attributeBuilder = new CustomAttributeListBuilder(2);

    SyneroexTradeConnector(TradeConnectorContext connectorContext,
                           ContractMetadata<Contract> metadata,
                           SyneroexContext context,
                           SessionContext sessionContext) {
        super(connectorContext, metadata, REQUEST_VALIDATOR);

        this.logger = sessionContext.logger();
        this.session = new Session(sessionContext, new InnerSyneroexListener());
        this.context = context;
    }

    @Override
    protected Log getLog() {
        return logger;
    }

    // region Trade Requests

    @Override
    protected void sendNewOrderRequest(OrderNewRequest request, Contract contract) {
        final CreateOrdersRequest.Builder newOrder = CreateOrdersRequest.newBuilder();

        newOrder.setSymbol(contract.getBrokerSymbol());
        newOrder.setClientOrderId(getClientOrderId(request));

        newOrder.setSide(SyneroexUtil.toSyneroexSide(request.getSide()));
        newOrder.setOrderType(SyneroexUtil.toSyneroexOrderType(request.getOrderType()));
        newOrder.setTimeInForce(SyneroexUtil.toSyneroexTIF(request.getTimeInForce()));

        newOrder.setQuantity(SyneroexUtil.toDecimalValue(request.getQuantity()));
        if (request.getOrderType() == OrderType.LIMIT) {
            newOrder.setPrice(SyneroexUtil.toDecimalValue(request.getLimitPrice()));
        }

        // set order post only flag - assuming that is passed in 6018 attribute/tag
        final ObjectList<CustomAttribute> attributes = request.getAttributes();
        if (attributes != null && !attributes.isEmpty()) {
            for (int i = 0; i < attributes.size(); i++) {
                final CustomAttribute attribute = attributes.get(i);
                if (attribute.getKey() == POST_ONLY_TAG) {
                    newOrder.setPostOnly(CharSequenceParser.parseBoolean(attribute.getValue()));
                    break; // stop iterating
                }
            }
        }

        session.createOrder(newOrder.build());
    }

    @Override
    protected void sendCancelOrderRequest(OrderCancelRequest request, Contract contract) {
        final CancelOrdersRequest.Builder cancelOrder = CancelOrdersRequest.newBuilder();

        cancelOrder.setClientOrderId(getClientOrderId(request));
        cancelOrder.setOrderId(CharSequenceUtil.toString(request.getExternalOrderId()));

        session.cancelOrder(cancelOrder.build());
    }

    @Override
    protected void sendReplaceOrderRequest(OrderReplaceRequest request, Contract contract) {
        final ReplaceOrdersRequest.Builder replaceOrder = ReplaceOrdersRequest.newBuilder();

        replaceOrder.setSymbol(contract.getBrokerSymbol());
        replaceOrder.setClientOrderId(getClientOrderId(request));
        replaceOrder.setOrderId(CharSequenceUtil.toString(request.getExternalOrderId()));

        replaceOrder.setQuantity(SyneroexUtil.toDecimalValue(request.getQuantity()));
        if (request.getOrderType() == OrderType.LIMIT) {
            replaceOrder.setPrice(SyneroexUtil.toDecimalValue(request.getLimitPrice()));
        }

        session.replaceOrder(replaceOrder.build());
    }

    @Override
    public void onOrderStatusRequest(OrderStatusRequest request) {
        if (!isLeader()) {
            return;
        }

        final OrderQueryRequest.Builder queryRequest = OrderQueryRequest.newBuilder();

        queryRequest.setClientOrderId(getClientOrderId(request.getOrderId(), request.getSourceId()));
        queryRequest.setOrderId(CharSequenceUtil.toString(request.getExternalOrderId()));

        session.queryOrder(queryRequest.build());
    }

    private static String getClientOrderId(OrderRequest request) {
        return getClientOrderId(request.getOrderId(), request.getSourceId());
    }

    private static String getClientOrderId(CharSequence orderId, @Alphanumeric long sourceId) {
        // order identity consists of two parts: OrderId and SourceId
        // i.e. combination of these properties is unique across Ember OMS
        return orderId + "$" + AlphanumericCodec.decode(sourceId);
    }

    private void makeOrderEvent(String clientOrderId, MutableOrderEvent event) {
        final int index = clientOrderId.lastIndexOf('$');
        final String orderId = clientOrderId.substring(0, index);
        final @Alphanumeric long sourceId = AlphanumericCodec.encode(clientOrderId.substring(index + 1));

        event.setOrderId(orderId);
        event.setDestinationId(sourceId); // event destination is order source
        event.setSourceId(getId()); // event source is connector name
        event.setTimestamp(clock.time());
    }

    private void makeOrderEvent(OrderResponse response, MutableOrderEvent event) {
        makeOrderEvent(response.getClientOrderId(), event);

        event.setExternalOrderId(response.getOrderId());
        event.setCumulativeQuantity(SyneroexUtil.fromDecimalValue(response.getCumQuantity()));
        event.setAveragePrice(SyneroexUtil.fromDecimalValue(response.getAvgPrice()));
        event.setOrderStatus(SyneroexUtil.toOrderStatus(response.getOrderState()));

        event.setOriginalTimestamp(response.getTransactTime()); // transaction time on server

        // pass broker in event attribute
        attributeBuilder.clear();
        attributeBuilder.addText(BROKER_TAG, response.getBroker());
        event.setAttributes(attributeBuilder.build());
    }

    // endregion

    // region ServiceWorkerAware

    private void handleOrderResponse(OrderResponse response) {
        switch (response.getExecType()) {
            case NEW_EVENT:
                final MutableOrderNewEvent newEvent = messages.orderNewEvent();
                makeOrderEvent(response, newEvent);

                fireOrderNewEvent(newEvent);
                break;
            case CANCELED_EVENT:
                final MutableOrderCancelEvent cancelEvent = messages.orderCancelEvent();
                makeOrderEvent(response, cancelEvent);
                cancelEvent.setReason(response.getReason());

                fireOrderCancelEvent(cancelEvent);
                break;
            case REJECTED_EVENT:
                final MutableOrderRejectEvent rejectEvent = messages.orderRejectEvent();
                makeOrderEvent(response, rejectEvent);
                rejectEvent.setReason(response.getReason());

                fireOrderRejectEvent(rejectEvent);
                break;
            case REPLACED_EVENT:
                final MutableOrderReplaceEvent replaceEvent = messages.orderReplaceEvent();
                makeOrderEvent(response, replaceEvent);

                fireOrderReplaceEvent(replaceEvent);
                break;
            case TRADE_EVENT:
                final MutableOrderTradeReportEvent tradeReportEvent = messages.tradeReportEvent();
                makeOrderEvent(response, tradeReportEvent);
                // here we are assuming that server reports "total" fill - i.e. only CumQty and AvgPrice
                // therefore no need to populate TradeQuantity, TradePrice and EventId (for individual fills)

                // example how retrieve Contract for inbound message symbol
                final Contract contract = getContractByBrokerSymbol(response.getSymbol());
                if (contract.getSecurityType() == InstrumentType.SYNTHETIC) {
                    tradeReportEvent.setMultiLegReportingType(MultiLegReportingType.MULTI_LEG_SECURITY);
                }

                fireOrderTradeReportEvent(tradeReportEvent);
                break;
        }
    }

    private void handleCreateOrder(CreateOrdersRequest request, CreateOrdersResponse response, SyneroexMessage error) {
        // in case of success notify that order is open
        if (response.getSuccess()) {
            final MutableOrderNewEvent event = messages.orderNewEvent();
            // populate order event properties
            makeOrderEvent(response.getClientOrderId(), event);
            event.setExternalOrderId(response.getOrderId());
            // send event to Ember OMS
            fireOrderNewEvent(event);
        } else {
            final MutableOrderRejectEvent event = messages.orderRejectEvent();

            // populate order event properties
            makeOrderEvent(response.getClientOrderId(), event);
            event.setExternalOrderId(response.getOrderId());
            event.setReason(response.getReason());
            event.setDeltixRejectCode(DeltixRejectCodes.UNKNOWN);

            // send event to Ember OMS
            fireOrderRejectEvent(event);
        }
    }

    private void handleCancelOrder(CancelOrdersRequest request, CancelOrdersResponse response, SyneroexMessage error) {
        // TODO: handle
    }

    private void handleReplaceOrder(ReplaceOrdersRequest request, ReplaceOrdersResponse response, SyneroexMessage error) {
        // TODO: handle
    }

    private void handleQueryOrder(OrderQueryRequest request, OrderQueryResponse response, SyneroexMessage error) {
        final MutableOrderStatusEvent event = messages.orderStatusEvent();

        makeOrderEvent(response.getClientOrderId(), event);
        event.setExternalOrderId(response.getOrderId());

        event.setCumulativeQuantity(SyneroexUtil.fromDecimalValue(response.getCumQuantity()));
        event.setAveragePrice(SyneroexUtil.fromDecimalValue(response.getAvgPrice()));
        event.setOrderStatus(SyneroexUtil.toOrderStatus(response.getOrderState()));

        fireOrderStatusEvent(event);
    }

    // endregion

    // region ServiceWorkerAware

    @Override
    public int doLast(int workDone) {
        session.keepSessionAlive(keepSessionAlive);
        workDone += session.work();
        return workDone;
    }

    @Override
    public boolean readyToClose() {
        return sessionStatus == SessionStatus.DISCONNECTED;
    }

    @Override
    public void onException(Throwable e) {
        processError(e);
    }

    @Override
    public void onCloseSignal() {
        session.deactivate();
    }

    // endregion

    //region Connectivity

    @Override
    protected void doOpen() {
        getLog().info("Open connector");
        session.open();
    }

    @Override
    protected void doClose() {
        getLog().info("Close connector");
        CloseHelper.close(session);
    }

    @Override
    public void onShutdownRequest(ShutdownRequest request) {
        super.onShutdownRequest(request);

        if (session.isDisconnected()) {
            handleSessionStateChanged(SessionStatus.DISCONNECTED);
        }
    }

    private void handleSessionStateChanged(SessionStatus newStatus) {
        setStatus(newStatus);

        if (newStatus == SessionStatus.DISCONNECTED && shutdownRequested) {
            fireShutdownResponse();
        }
    }

    //endregion

    // region Helper Classes

    private final class InnerSyneroexListener implements SyneroexListener {
        @Override
        public void onAppMessage(OrderResponse message) {
            try {
                handleOrderResponse(message);
            } catch (Exception e) {
                getLog().error("Handle order event failure: %s\n%s\n%s").with(e.getMessage()).with(SyneroexUtil.messageToString(message)).with(e);
            }
        }

        @Override
        public void onCreateOrder(CreateOrdersRequest request, CreateOrdersResponse response, SyneroexMessage error) {
            try {
                handleCreateOrder(request, response, error);
            } catch (Exception e) {
                getLog().error("Handle create order (%s) failure: %s\n%s\n%s").with(request.getClientOrderId()).with(e.getMessage()).with(e);
            }
        }

        @Override
        public void onReplaceOrder(ReplaceOrdersRequest request, ReplaceOrdersResponse response, SyneroexMessage error) {
            try {
                handleReplaceOrder(request, response, error);
            } catch (Exception e) {
                getLog().error("Handle replace order (%s) failure: %s\n%s\n%s").with(request.getClientOrderId()).with(e.getMessage()).with(e);
            }
        }

        @Override
        public void onCancelOrder(CancelOrdersRequest request, CancelOrdersResponse response, SyneroexMessage error) {
            try {
                handleCancelOrder(request, response, error);
            } catch (Exception e) {
                getLog().error("Handle cancel order (%s) failure: %s\n%s\n%s").with(request.getClientOrderId()).with(e.getMessage()).with(e);
            }
        }

        @Override
        public void onQueryOrder(OrderQueryRequest request, OrderQueryResponse response, SyneroexMessage error) {
            try {
                handleQueryOrder(request, response, error);
            } catch (Exception e) {
                getLog().error("Handle query order (%s) failure: %s\n%s\n%s").with(request.getClass().getSimpleName()).with(e.getMessage()).with(e);
            }
        }

        @Override
        public void onConnected() {
            handleSessionStateChanged(SessionStatus.CONNECTED);
        }

        @Override
        public void onDisconnected() {
            handleSessionStateChanged(SessionStatus.DISCONNECTED);
        }
    }

    // endregion
}
