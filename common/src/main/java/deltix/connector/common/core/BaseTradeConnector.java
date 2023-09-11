package deltix.connector.common.core;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import deltix.anvil.message.MutableShutdownResponse;
import deltix.anvil.message.NodeStatus;
import deltix.anvil.message.NodeStatusEvent;
import deltix.anvil.message.ShutdownRequest;
import deltix.anvil.util.clock.EpochClock;
import deltix.anvil.util.counter.Counter;
import deltix.connector.common.Messages;
import deltix.connector.common.smd.ContractMetadata;
import deltix.connector.common.util.RequestValidator;
import deltix.ember.message.smd.*;
import deltix.ember.message.trade.*;
import deltix.ember.service.Ember;
import deltix.ember.service.InstrumentSnapshot;
import deltix.ember.service.connector.TradeConnector;
import deltix.ember.service.connector.TradeConnectorContext;
import deltix.ember.service.connector.TradeConnectorStatusIndicator;
import edu.umd.cs.findbugs.annotations.DefaultAnnotationForParameters;
import edu.umd.cs.findbugs.annotations.NonNull;

@DefaultAnnotationForParameters(NonNull.class)
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class BaseTradeConnector<Contract extends deltix.connector.common.smd.Contract>
        implements TradeConnector {

    private static final Log LOG = LogFactory.getLog(TradeConnector.class);

    protected final Messages messages = new Messages();

    protected final long id;
    protected final String name;

    protected final ContractMetadata<Contract> metadata;
    protected final InstrumentSnapshot instrumentSnapshot;
    protected final RequestValidator requestValidator;
    protected final EpochClock clock;

    private final Ember ember;
    private final Counter statusIndicator;

    protected NodeStatus nodeStatus = NodeStatus.BOOTSTRAP;
    protected deltix.ember.message.trade.SessionStatus sessionStatus = deltix.ember.message.trade.SessionStatus.DISCONNECTED;

    protected boolean shutdownRequested;
    protected boolean keepSessionAlive;


    public BaseTradeConnector(final TradeConnectorContext connectorContext,
                              final ContractMetadata<Contract> metadata,
                              final RequestValidator requestValidator) {
        this.id = connectorContext.getId();
        this.name = connectorContext.getName();
        this.metadata = metadata;
        this.clock = connectorContext.getEpochClock();
        this.requestValidator = requestValidator;
        this.instrumentSnapshot = connectorContext.getInstrumentSnapshot();
        this.ember = connectorContext.getEmber();
        this.statusIndicator = connectorContext.getStatusIndicator();
    }

    protected boolean isLeader() {
        return nodeStatus == NodeStatus.LEADER;
    }

    protected Log getLog() {
        return LOG;
    }

    protected void setStatus(SessionStatus newStatus) {
        if (sessionStatus != newStatus) {
            getLog().info("Session status changed: %s -> %s").with(sessionStatus).with(newStatus);

            sessionStatus = newStatus;
            fireSessionStatusEvent(sessionStatus);
            boolean connected = newStatus == SessionStatus.CONNECTED;
            statusIndicator.setOrdered(connected ? TradeConnectorStatusIndicator.CONNECTED : TradeConnectorStatusIndicator.DISCONNECTED);
        }
    }

    protected void leadershipChanged(boolean isLeader) {
    }

    @Override
    public void onNodeStatusEvent(NodeStatusEvent event) {
        final NodeStatus previous = nodeStatus;
        final NodeStatus next = event.getNodeStatus();
        nodeStatus = next;

        keepSessionAlive = (next == NodeStatus.LEADER && !shutdownRequested);

        if (previous != next) {
            leadershipChanged(next == NodeStatus.LEADER);
        }

        getLog().info("Connector %s switched from %s to %s role").with(name).with(previous).with(next);
    }

    @Override
    public void onShutdownRequest(final ShutdownRequest request) {
        shutdownRequested = true;
        keepSessionAlive = false;
    }

    //region Service Impl

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void open() {
        try {
            doOpen();
            instrumentSnapshot.forEach(metadata);
        } catch (Exception e) {
            processError(e);
            throw e;
        }
    }

    @Override
    public void close() {
        try {
            doClose();
        } catch (Exception e) {
            processError(e);
            throw e;
        }
    }

    protected void doOpen() {}

    protected void doClose() {}

    protected void processError(Throwable e) {
        getLog().error("Connector unexpected failure: %s").with(e);
    }

    //endregion

    //region New Order Request

    @Override
    public void onNewOrderRequest(OrderNewRequest request) {
        try {
            handleNewOrderRequest(request);
        } catch (final Throwable e) {
            handleNewOrderException(request, e);
        }
    }

    protected void handleNewOrderRequest(OrderNewRequest request) {
        Contract contract = getContractBySymbol(request.getSymbol());

        if (isLeader()) {
            validateNewOrderRequest(request, contract);
            sendNewOrderRequest(request, contract);
        }
    }

    protected void handleNewOrderException(OrderNewRequest request, Throwable e) {
        getLog().warn("Error processing order %s:%s: %s").withAlphanumeric(request.getSourceId()).with(request.getOrderId()).with(e);

        if (isLeader()) {
            MutableOrderRejectEvent event = messages.orderRejectEvent();
            Messages.makeOrderRejectEvent(clock.timeNs(), e.getMessage(), request, event);
            fireOrderRejectEvent(event);
        }
    }

    protected void validateNewOrderRequest(OrderNewRequest request, Contract contract) {
        requestValidator.validateSubmit(request);
    }

    protected abstract void sendNewOrderRequest(OrderNewRequest request, Contract contract);


    protected void discardOrder(OrderStatusRequest request) {
        final long sourceId = request.getSourceId();
        final CharSequence orderId = request.getOrderId();

        getLog().info("Discarding order due to Cancel-on-Disconnect %s:%s").withAlphanumeric(sourceId).with(orderId);

        MutableOrderCancelEvent discardEvent = messages.orderCancelEvent();
        discardEvent.setEventId(orderId);
        discardEvent.setOrderStatus(OrderStatus.CANCELED);
        discardEvent.setOrderId(orderId);
        discardEvent.setExternalOrderId(request.getExternalOrderId());
        discardEvent.setTimestampNs(clock.timeNs());
        discardEvent.setSourceId(request.getDestinationId());
        discardEvent.setDestinationId(sourceId);
        discardEvent.setReason("Discard order due to Cancel-On-Disconnect");

        fireOrderCancelEvent(discardEvent);
    }

    //endregion

    //region Cancel Order Request

    @Override
    public void onCancelOrderRequest(OrderCancelRequest request) {
        try {
            handleCancelOrderRequest(request);
        } catch (final Throwable e) {
            handleCancelException(request, e);
        }
    }

    protected void handleCancelOrderRequest(OrderCancelRequest request) {
        Contract contract = getContractBySymbol(request.getSymbol());

        if (isLeader()) {
            validateCancelOrderRequest(request, contract);
            sendCancelOrderRequest(request, contract);
        }
    }


    protected void handleCancelException(OrderCancelRequest request, Throwable e) {
        getLog().warn("Error processing cancel for %s:%s: %s").withAlphanumeric(request.getSourceId()).with(request.getOrderId()).with(e);

        if (isLeader()) {
            MutableOrderCancelRejectEvent event = messages.orderCancelRejectEvent();
            Messages.makeOrderCancelRejectEvent(clock.timeNs(), e.getMessage(), request, event);
            fireOrderCancelRejectEvent(event);
        }
    }

    protected void validateCancelOrderRequest(OrderCancelRequest request, Contract contract) {
        requestValidator.validateCancel(request);
    }

    protected abstract void sendCancelOrderRequest(OrderCancelRequest request, Contract contract);

    //endregion

    //region Replace Order Request

    @Override
    public void onReplaceOrderRequest(OrderReplaceRequest request) {
        try {
            handleReplaceOrderRequest(request);
        } catch (final Throwable e) {
            handleReplaceOrderException(request, e);
        }
    }

    protected void handleReplaceOrderException(OrderReplaceRequest request, Throwable e) {
        getLog().warn("Error processing replace order %s:%s: %s").withAlphanumeric(request.getSourceId()).with(request.getOrderId()).with(e);

        if (isLeader()) {
            MutableOrderReplaceRejectEvent event = messages.orderReplaceRejectEvent();
            Messages.makeOrderReplaceRejectEvent(clock.timeNs(), e.getMessage(), request, event);
            fireOrderReplaceRejectEvent(event);
        }
    }

    protected void handleReplaceOrderRequest(OrderReplaceRequest request) {
        Contract contract = getContractBySymbol(request.getSymbol());

        if (isLeader()) {
            validateReplaceOrderRequest(request, contract);
            sendReplaceOrderRequest(request, contract);
        }
    }

    protected void validateReplaceOrderRequest(OrderReplaceRequest request, Contract contract) {
        requestValidator.validateModify(request);
    }

    protected abstract void sendReplaceOrderRequest(OrderReplaceRequest request, Contract contract);

    //endregion

    //region Security Metadata

    @Override
    public void onBondUpdate(BondUpdate update) {
        metadata.onBondUpdate(update);
    }

    @Override
    public void onCurrencyUpdate(CurrencyUpdate update) {
        metadata.onCurrencyUpdate(update);
    }

    @Override
    public void onCustomInstrumentUpdate(CustomInstrumentUpdate update) {
        metadata.onCustomInstrumentUpdate(update);
    }

    @Override
    public void onEquityUpdate(EquityUpdate update) {
        metadata.onEquityUpdate(update);
    }

    @Override
    public void onEtfUpdate(EtfUpdate update) {
        metadata.onEtfUpdate(update);
    }

    @Override
    public void onFutureUpdate(FutureUpdate update) {
        metadata.onFutureUpdate(update);
    }

    @Override
    public void onIndexUpdate(IndexUpdate update) {
        metadata.onIndexUpdate(update);
    }

    @Override
    public void onOptionUpdate(OptionUpdate update) {
        metadata.onOptionUpdate(update);
    }

    @Override
    public void onSyntheticUpdate(SyntheticUpdate update) {
        metadata.onSyntheticUpdate(update);
    }

    protected Contract getContractBySymbol(CharSequence symbol) {
        Contract contract = metadata.getContractBySymbol(symbol);
        if (contract == null) {
            throw new IllegalArgumentException(String.format("Can't find contract by symbol \"%s\"", symbol));
        }
        return contract;
    }

    protected Contract getContractByBrokerSymbol(CharSequence brokerSymbol) {
        Contract contract = metadata.getContractByBrokerSymbol(brokerSymbol);
        if (contract == null) {
            throw new IllegalArgumentException(String.format("Can't find contract by broker symbol \"%s\"", brokerSymbol));
        }
        return contract;
    }

    //endregion

    // region Fire Ember Events

    protected void fireShutdownResponse() {
        if (isLeader()) {
            final MutableShutdownResponse response = new MutableShutdownResponse();
            response.setServiceId(id);

            ember.onShutdownResponse(response);
        }
    }

    protected void fireSessionStatusEvent(final deltix.ember.message.trade.SessionStatus status) {
        if (isLeader()) {
            final MutableSessionStatusEvent event = messages.sessionStatusEvent();
            event.setSourceId(id);
            event.setStatus(status);
            event.setTimestampNs(clock.timeNs());
            ember.onSessionStatusEvent(event);
        }
    }

    protected void fireOrderPendingNewEvent(final OrderPendingNewEvent event) {
        if (isLeader()) {
            onOrderPendingNewEvent(event);
            ember.onOrderPendingNewEvent(event);
        }
    }

    protected void fireOrderNewEvent(final OrderNewEvent event) {
        if (isLeader()) {
            onOrderNewEvent(event);
            ember.onOrderNewEvent(event);
        }
    }

    protected void fireOrderRejectEvent(final OrderRejectEvent event) {
        if (isLeader()) {
            onOrderRejectEvent(event);
            ember.onOrderRejectEvent(event);
        }
    }

    protected void fireOrderPendingCancelEvent(final OrderPendingCancelEvent event) {
        if (isLeader()) {
            onOrderPendingCancelEvent(event);
            ember.onOrderPendingCancelEvent(event);
        }
    }

    protected void fireOrderCancelEvent(final OrderCancelEvent event) {
        if (isLeader()) {
            onOrderCancelEvent(event);
            ember.onOrderCancelEvent(event);
        }
    }

    protected void fireOrderCancelRejectEvent(final OrderCancelRejectEvent event) {
        if (isLeader()) {
            onOrderCancelRejectEvent(event);
            ember.onOrderCancelRejectEvent(event);
        }
    }

    protected void fireOrderPendingReplaceEvent(final OrderPendingReplaceEvent event) {
        if (isLeader()) {
            onOrderPendingReplaceEvent(event);
            ember.onOrderPendingReplaceEvent(event);
        }
    }

    protected void fireOrderReplaceEvent(final OrderReplaceEvent event) {
        if (isLeader()) {
            onOrderReplaceEvent(event);
            ember.onOrderReplaceEvent(event);
        }
    }

    protected void fireOrderReplaceRejectEvent(final OrderReplaceRejectEvent event) {
        if (isLeader()) {
            onOrderReplaceRejectEvent(event);
            ember.onOrderReplaceRejectEvent(event);
        }
    }

    protected void fireOrderTradeReportEvent(final OrderTradeReportEvent event) {
        if (isLeader()) {
            onOrderTradeReportEvent(event);
            ember.onOrderTradeReportEvent(event);
        }
    }

    protected void fireOrderTradeCancelEvent(final OrderTradeCancelEvent event) {
        if (isLeader()) {
            onOrderTradeCancelEvent(event);
            ember.onOrderTradeCancelEvent(event);
        }
    }

    protected void fireOrderTradeCorrectEvent(final OrderTradeCorrectEvent event) {
        if (isLeader()) {
            onOrderTradeCorrectEvent(event);
            ember.onOrderTradeCorrectEvent(event);
        }
    }

    protected void fireOrderStatusEvent(final OrderStatusEvent event) {
        if (isLeader()) {
            onOrderStatusEvent(event);
            ember.onOrderStatusEvent(event);
        }
    }

    protected void fireOrderRestateEvent(final OrderRestateEvent event) {
        if (isLeader()) {
            onOrderRestateEvent(event);
            ember.onOrderRestateEvent(event);
        }
    }

    // endregion

    // region Price & Quantity

    @Decimal
    protected long toBrokerQuantity(@Decimal long orderQuantity, Contract contract) {
        if (Decimal64Utils.isNaN(orderQuantity))
            return orderQuantity;

        long quantity = orderQuantity;
        if (contract.hasQuantityMultiplier()) {
            quantity = Decimal64Utils.multiply(quantity, contract.getQuantityMultiplier());
        }

        if (contract.hasQuantityPrecision()) {
            quantity = Decimal64Utils.round(quantity, contract.getQuantityPrecision());
        }

        return quantity;
    }

    @Decimal
    protected long toOrderQuantity(@Decimal long brokerQuantity, Contract contract) {
        if (Decimal64Utils.isNaN(brokerQuantity))
            return brokerQuantity;

        return contract.hasQuantityMultiplier() ? Decimal64Utils.divide(brokerQuantity, contract.getQuantityMultiplier()) : brokerQuantity;
    }

    @Decimal
    protected long toBrokerPrice(@Decimal long orderPrice, Contract contract) {
        if (Decimal64Utils.isNaN(orderPrice))
            return orderPrice;

        long price = orderPrice;
        if (contract.hasPriceMultiplier()) {
            price = Decimal64Utils.multiply(price, contract.getPriceMultiplier());
        }

        if (contract.hasPricePrecision()) {
            price = Decimal64Utils.round(price, contract.getPricePrecision());
        }

        return price;
    }

    @Decimal
    protected long toOrderPrice(@Decimal long brokerPrice, Contract contract) {
        if (Decimal64Utils.isNaN(brokerPrice))
            return brokerPrice;

        return contract.hasPriceMultiplier() ? Decimal64Utils.divide(brokerPrice, contract.getPriceMultiplier()) : brokerPrice;
    }

    // endregion

}
