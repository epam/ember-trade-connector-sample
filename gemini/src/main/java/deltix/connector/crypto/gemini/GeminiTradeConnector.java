package deltix.connector.crypto.gemini;

import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import deltix.anvil.message.ShutdownRequest;
import deltix.anvil.util.CharSequenceUtil;
import deltix.anvil.util.CloseHelper;
import deltix.anvil.util.codec.AlphanumericCodec;
import deltix.connector.common.Messages;
import deltix.connector.common.core.BaseTradeConnector;
import deltix.connector.common.smd.Contract;
import deltix.connector.common.smd.ContractMetadata;
import deltix.connector.common.util.ConnectorUtil;
import deltix.connector.common.util.DefaultRequestValidator;
import deltix.connector.crypto.gemini.data.OrderEvent;
import deltix.connector.crypto.gemini.data.*;
import deltix.ember.message.trade.OrderStatus;
import deltix.ember.message.trade.OrderStatusRequest;
import deltix.ember.message.trade.*;
import deltix.ember.service.connector.TradeConnectorContext;
import deltix.util.annotations.Alphanumeric;
import edu.umd.cs.findbugs.annotations.DefaultAnnotationForParameters;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.EnumSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Trade Connector implementation for Gemini Crypto Exchange.
 * REST API and WebSocket API
 * - https://docs.gemini.com/rest-api/
 * - https://docs.gemini.com/websocket-api/
 * <p>
 * Connector utilizes AsyncHttpClient (backed by Netty) to work both with HTTP and WebSocket.
 * https://github.com/AsyncHttpClient/async-http-client
 */
@DefaultAnnotationForParameters(NonNull.class)
@SuppressWarnings("JavadocLinkAsPlainText")
public class GeminiTradeConnector extends BaseTradeConnector<Contract> {
    private static final Log LOG = LogFactory.getLog(GeminiTradeConnector.class);
    private static final long HEARTBEAT_INTERVAL = TimeUnit.SECONDS.toMillis(15);

    private static final char CLORDID_DELIMITER = '$';
    private static final String EXCHANGE_LIMIT_ORDER_TYPE = "exchange limit";

    // Request Validator validates incoming Ember order requests and rejects them if they do not meet exchange capabilities
    private static final DefaultRequestValidator REQUEST_VALIDATOR =
            DefaultRequestValidator.createBuilder()
                    .allowDisplayQty(false)  // Gemini does not support Display Quantity (aka Iceberg orders)
                    .allowMinQty(true)       // Gemini supports Min Quantity
                    .allowFastCancel(false)  // Gemini does not allow cancelling order by client order id
                    .allowFastReplace(false) // order modification is not supported by exchange
                    .orderTypes(EnumSet.of(OrderType.LIMIT)) // current example only supports Limit orders
                    .sides(EnumSet.of(Side.BUY, Side.SELL))
                    .timeInForces(EnumSet.of(TimeInForce.GOOD_TILL_CANCEL, TimeInForce.IMMEDIATE_OR_CANCEL, TimeInForce.FILL_OR_KILL, TimeInForce.AT_THE_OPENING)).build();

    private final long reconnectInterval;

    private final ScheduledExecutorService scheduledExecutor;
    private volatile ScheduledFuture<?> heartbeatTask;

    private final GeminiHttpClient httpClient;
    private final GeminiWsClient wsClient;

    public GeminiTradeConnector(GeminiConnectorSettings settings,
                                TradeConnectorContext connectorContext,
                                ContractMetadata<Contract> metadata) {
        super(connectorContext, metadata, REQUEST_VALIDATOR);
        this.reconnectInterval = settings.getReconnectInterval(); // millis

        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

        final GeminiContext context = new GeminiContext(0, scheduledExecutor, new ApiKeyContext(settings.getApiKey(), settings.getApiSecret(), GeminiUtil.NONCE_GENERATOR, "HmacSHA384"), LOG);
        httpClient = new GeminiHttpClient(context, settings.getRestUrl());
        wsClient = new GeminiWsClient(context, settings.getWebsocketUrl(), new GeminiWebSocketEventListener());
    }

    @Override
    protected Log getLog() {
        return LOG;
    }

    @Override
    protected Contract getContractByBrokerSymbol(CharSequence brokerSymbol) {
        // Gemini returns lowercase symbol, but internally we use UPPERCASE symbols for mapping
        final String symbol = brokerSymbol.toString().toUpperCase();
        return super.getContractByBrokerSymbol(symbol);
    }

    //region Connectivity

    private void doConnect() {
        getLog().info("Connect connector");
        wsClient.open();
    }

    @Override
    protected void doClose() {
        getLog().info("Close connector");
        cancelHeartbeat();
        scheduledExecutor.shutdownNow();
        CloseHelper.close(wsClient);
    }

    @Override
    protected void leadershipChanged(boolean isLeader) {
        super.leadershipChanged(isLeader);
        if (isLeader) {
            doConnect();
        }
    }

    @Override
    public void onShutdownRequest(ShutdownRequest request) {
        super.onShutdownRequest(request);

        if (wsClient.isConnected()) {
            wsClient.disconnect("Shutdown");
        } else {
            handleSessionStateChanged(SessionStatus.DISCONNECTED);
        }
    }

    /**
     * Trade connector status follows WebSocket connection status.
     * I.e. connector becomes disconnected when WebSocket connection goes down.
     * @param newStatus new connector status
     */
    private void handleSessionStateChanged(SessionStatus newStatus) {
        setStatus(newStatus);

        if (newStatus == SessionStatus.CONNECTED) {
            scheduleHeartbeat();
        } else if (newStatus == SessionStatus.DISCONNECTED) {
            cancelHeartbeat();

            if (shutdownRequested) {
                fireShutdownResponse();
            } else {
                scheduleReconnect();
            }
        }
    }

    private void scheduleReconnect() {
        scheduledExecutor.schedule(this::doReconnect, reconnectInterval, TimeUnit.MILLISECONDS);
    }

    private synchronized void doReconnect() {
        getLog().info("Reconnect connector");
        if (!shutdownRequested) {
            wsClient.open();
        }
    }

    // endregion

    // region New Order Request

    @Override
    protected void sendNewOrderRequest(OrderNewRequest request, Contract contract) {
        final String orderId = CharSequenceUtil.toString(request.getOrderId());

        httpClient.newOrder(makeNewOrderRequest(request, contract), (orderStatus, throwable) -> {
            if (getLog().isDebugEnabled()) {
                getLog().debug("New Order Response: %s, Error: %s").with(orderStatus).with(throwable);
            }

            // notify order rejection in case of failure
            // IMPORTANT: in general we should analyze exact reason of failure:
            // for example, we should NOT reject order in case of Request Timeout or Internal Server error
            if (throwable != null) {
                synchronized (GeminiTradeConnector.this) {
                    notifyOrderReject(orderId, request.getSourceId(), throwable.getMessage());
                }
            }
        });
    }

    private NewOrderRequest makeNewOrderRequest(OrderNewRequest orderRequest, Contract contract) {
        final NewOrderRequest request = new NewOrderRequest();
        request.setSymbol(contract.getBrokerSymbol());
        request.setClientOrderId(getClientOrderId(orderRequest));
        request.setSide(GeminiUtil.toGeminiSide(orderRequest.getSide()));
        request.setType(EXCHANGE_LIMIT_ORDER_TYPE);

        EnumSet<OrderOptions> orderOptions = EnumSet.noneOf(OrderOptions.class);

        if (ConnectorUtil.isPostOnly(orderRequest.getAttributes()))
            orderOptions.add(OrderOptions.MAKER_OR_CANCEL);

        switch (orderRequest.getTimeInForce()) {
            case IMMEDIATE_OR_CANCEL:
                orderOptions.add(OrderOptions.IMMEDIATE_OR_CANCEL);
                break;
            case FILL_OR_KILL:
                request.setOptions(EnumSet.of(OrderOptions.FILL_OR_KILL));
                break;
            case AT_THE_OPENING:
                request.setOptions(EnumSet.of(OrderOptions.AUCTION_ONLY));
                break;
        }

        if (!orderOptions.isEmpty())
            request.setOptions(orderOptions);

        if (orderRequest.hasMinQuantity()) {
            request.setMinAmount(toBrokerQuantity(orderRequest.getMinQuantity(), contract));
        }

        request.setAmount(toBrokerQuantity(orderRequest.getQuantity(), contract));

        if (orderRequest.getOrderType() == OrderType.LIMIT) {
            request.setPrice(toBrokerPrice(orderRequest.getLimitPrice(), contract));
        }

        return request;
    }

    private void notifyOrderReject(String orderId, @Alphanumeric long destinationId, String reason) {
        final MutableOrderRejectEvent emberEvent = messages.orderRejectEvent();

        emberEvent.setSourceId(id); // event source is connector itself
        emberEvent.setDestinationId(destinationId); // event destination is order request source
        emberEvent.setOrderId(orderId);
        emberEvent.setTimestampNs(clock.timeNs());
        emberEvent.setReason(reason);

        fireOrderRejectEvent(emberEvent);
    }

    // endregion

    // region Cancel Order Request

    @Override
    protected void sendCancelOrderRequest(OrderCancelRequest request, Contract contract) {
        final String orderId = CharSequenceUtil.toString(request.getOrderId());

        httpClient.cancelOrder(makeCancelOrderRequest(request), (orderStatus, throwable) -> {
            if (getLog().isDebugEnabled()) {
                getLog().debug("Cancel Order Response: %s, Error: %s").with(orderStatus).with(throwable);
            }

            // notify cancel rejection in case of failure
            if (throwable != null) {
                synchronized (GeminiTradeConnector.this) {
                    notifyCancelReject(orderId, request.getSourceId(), throwable.getMessage());
                }
            }
        });
    }

    private static CancelOrderRequest makeCancelOrderRequest(OrderCancelRequest cancelRequest) {
        final CancelOrderRequest request = new CancelOrderRequest();
        request.setExternalOrderId(CharSequenceUtil.toString(cancelRequest.getExternalOrderId()));
        return request;
    }

    private void notifyCancelReject(String orderId, @Alphanumeric long destinationId, String reason) {
        final MutableOrderCancelRejectEvent emberEvent = messages.orderCancelRejectEvent();

        emberEvent.setSourceId(id); // event source is connector itself
        emberEvent.setDestinationId(destinationId); // event destination is order request source
        emberEvent.setOrderId(orderId);
        emberEvent.setTimestampNs(clock.timeNs());
        emberEvent.setReason(reason);

        fireOrderCancelRejectEvent(emberEvent);
    }

    // endregion

    // region Replace Order Request

    @Override
    public void onReplaceOrderRequest(OrderReplaceRequest request) {
        final MutableOrderReplaceRejectEvent event = messages.orderReplaceRejectEvent();
        Messages.makeOrderReplaceRejectEvent(clock.timeNs(), "Cancel/Replace is not supported", request, event);
        fireOrderReplaceRejectEvent(event);
    }

    @Override
    protected void sendReplaceOrderRequest(OrderReplaceRequest request, Contract contract) {
        // Gemini does not support cancel/replace
    }

    // endregion

    // region Order Status Request

    /**
     * Upon (re)connection Ember requests statuses for active orders.
     * Connector should check order status and notify current state of the order.
     * @param request order status request from Ember
     */
    @Override
    public void onOrderStatusRequest(OrderStatusRequest request) {
        if (!request.hasExternalOrderId()) {
            getLog().warn("Skip order status request for order without external order id: %s:%s")
                    .withAlphanumeric(request.getSourceId())
                    .with(request.getOrderId());
            return; // skip requesting order status for order without external id
        }

        httpClient.loadOrderStatus(makeOrderStatusRequest(request), (orderStatus, throwable) -> {
            if (getLog().isDebugEnabled()) {
                getLog().debug("Order Status Response: %s, Error: %s").with(orderStatus).with(throwable);
            }

            if (orderStatus != null) {
                synchronized (GeminiTradeConnector.this) {
                    notifyOrderStatus(orderStatus);
                }
            }
        });
    }

    private static deltix.connector.crypto.gemini.data.OrderStatusRequest makeOrderStatusRequest(OrderStatusRequest statusRequest) {
        final deltix.connector.crypto.gemini.data.OrderStatusRequest request = new deltix.connector.crypto.gemini.data.OrderStatusRequest();
        request.setExternalOrderId(CharSequenceUtil.toString(statusRequest.getExternalOrderId()));
        return request;
    }

    // endregion

    // region WebSocket Event Handling

    /**
     * Handles order update events received via WebSocket channel.
     * @param message order update event
     */
    private void handleMessage(final Message message) {
        if (message instanceof deltix.connector.crypto.gemini.data.OrderEvent.AcceptedOrderEvent) {
            final deltix.connector.crypto.gemini.data.OrderEvent.AcceptedOrderEvent event = (deltix.connector.crypto.gemini.data.OrderEvent.AcceptedOrderEvent) message;
            final MutableOrderNewEvent emberEvent = messages.orderNewEvent();
            populateEvent(event, emberEvent);

            fireOrderNewEvent(emberEvent);
        } else if (message instanceof deltix.connector.crypto.gemini.data.OrderTradeEvent) {
            final deltix.connector.crypto.gemini.data.OrderTradeEvent event = (deltix.connector.crypto.gemini.data.OrderTradeEvent) message;
            final MutableOrderTradeReportEvent emberEvent = messages.tradeReportEvent();

            assert event.getFill() != null;

            populateEvent(event, emberEvent);

            final Contract contract = getContractByBrokerSymbol(event.getSymbol());

            emberEvent.setEventId(event.getFill().getTradeId());
            emberEvent.setTradePrice(toOrderPrice(event.getFill().getPrice(), contract));
            emberEvent.setTradeQuantity(toOrderQuantity(event.getFill().getAmount(), contract));

            emberEvent.setCommission(event.getFill().getFee());
            emberEvent.setCommissionCurrency(AlphanumericCodec.encode(event.getFill().getFeeCurrency()));

            emberEvent.setAggressorSide(GeminiUtil.toAggressorIndicator(event.getFill()));

            fireOrderTradeReportEvent(emberEvent);
        } else if (message instanceof OrderCancelledEvent) {
            final OrderCancelledEvent event = (OrderCancelledEvent) message;
            final MutableOrderCancelEvent emberEvent = messages.orderCancelEvent();

            populateEvent(event, emberEvent);
            emberEvent.setReason(event.getReason());

            fireOrderCancelEvent(emberEvent);
        } else if (message instanceof OrderRejectedEvent) {
            final OrderRejectedEvent event = (OrderRejectedEvent) message;
            final MutableOrderRejectEvent emberEvent = messages.orderRejectEvent();

            populateEvent(event, emberEvent);
            emberEvent.setReason(event.getReason());

            fireOrderRejectEvent(emberEvent);
        } else if (message instanceof OrderCancelRejectedEvent) {
            final OrderCancelRejectedEvent event = (OrderCancelRejectedEvent) message;
            final MutableOrderCancelRejectEvent emberEvent = messages.orderCancelRejectEvent();

            populateEvent(event, emberEvent);
            emberEvent.setReason(event.getReason());

            fireOrderCancelRejectEvent(emberEvent);
        } else if (message instanceof deltix.connector.crypto.gemini.data.OrderEvent.InitialOrderEvent) {
            notifyOrderStatus((OrderEvent) message);
        } else {
            getLog().info("Skip event: %s").with(message);
        }
    }

    private void populateEvent(final BaseOrderStatus event, final MutableOrderEvent emberEvent) {
        // split ClientOrderID to extract DestinationId and Ember OrderId
        final String clientOrderId = event.getClientOrderId();
        final int index = clientOrderId.indexOf(CLORDID_DELIMITER);
        final long destinationId = AlphanumericCodec.encode(clientOrderId.substring(0, index));
        final String orderId = clientOrderId.substring(index + 1);

        final Contract contract = getContractByBrokerSymbol(event.getSymbol());

        emberEvent.setSourceId(id); // event source is connector itself
        emberEvent.setDestinationId(destinationId); // event destination is order request source
        emberEvent.setOrderId(orderId);
        emberEvent.setExternalOrderId(event.getOrderId());

        emberEvent.setTimestampNs(clock.timeNs());
        emberEvent.setOriginalTimestamp(event.getTimestamp());

        emberEvent.setSymbol(contract.getSymbol());
        emberEvent.setInstrumentType(contract.getSecurityType());

        emberEvent.setSide(GeminiUtil.fromGeminiSide(event.getSide()));
        emberEvent.setEventId(event.getEventId());

        emberEvent.setQuantity(toOrderQuantity(event.getOriginalAmount(), contract));
        emberEvent.setRemainingQuantity(toOrderQuantity(event.getRemainingAmount(), contract));
        emberEvent.setCumulativeQuantity(toOrderQuantity(event.getExecutedAmount(), contract));
        emberEvent.setAveragePrice(toOrderPrice(event.getAvgExecutionPrice(), contract));
    }

    private void notifyOrderStatus(final BaseOrderStatus orderStatus) {
        final MutableOrderStatusEvent emberEvent = messages.orderStatusEvent();
        populateEvent(orderStatus, emberEvent);

        if (orderStatus.isLive()) {
            if (Decimal64Utils.isPositive(orderStatus.getExecutedAmount())) {
                emberEvent.setOrderStatus(OrderStatus.PARTIALLY_FILLED);
            } else {
                emberEvent.setOrderStatus(OrderStatus.NEW);
            }
        } else if (orderStatus.isCancelled()) {
            emberEvent.setOrderStatus(OrderStatus.CANCELED);
        } else if (Decimal64Utils.isEqual(orderStatus.getOriginalAmount(), orderStatus.getExecutedAmount())) {
            emberEvent.setOrderStatus(OrderStatus.COMPLETELY_FILLED);
        }

        fireOrderStatusEvent(emberEvent);
    }

    /**
     * Ember order identity consists of SourceId and OrderId.
     * Therefore, SourceId and OrderId are concatenated to build unique order identifier.
     */
    private static String getClientOrderId(final OrderEntryRequest request) {
        // concatenate SourceId and OrderId and pass as Gemini ClientOrder ID
        return AlphanumericCodec.decode(request.getSourceId()) + CLORDID_DELIMITER + request.getOrderId();
    }

    // endregion

    // region Heartbeats

    /**
     * Gemini requires to send heartbeats.
     * If the exchange does not receive a message for 30 seconds,
     * then it will assume there has been an interruption in service, and cancel all outstanding orders.
     * This feature is often referred to as "Cancel on Disconnect" (or "Cancel All After") on connection-oriented exchange protocols.
     * https://docs.gemini.com/rest-api/#private-api-invocation
     */
    private void scheduleHeartbeat() {
        cancelHeartbeat();

        getLog().debug("Schedule heartbeats");
        heartbeatTask  = scheduledExecutor.scheduleAtFixedRate(this::sendHeartbeat, 0, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
    }

    private void cancelHeartbeat() {
        final ScheduledFuture<?> task = heartbeatTask;
        if (task != null) {
            getLog().debug("Cancel heartbeats");
            task.cancel(true);
        }
        heartbeatTask = null;
    }

    private void sendHeartbeat() {
        try {
            httpClient.sendHeartbeat();
        } catch (Exception e) {
            getLog().error("Heartbeat submission failure: %s").with(e);
        }
    }

    // endregion

    // region Helper Classes

    private final class GeminiWebSocketEventListener implements GeminiWsClient.WebSocketEventListener {
        @Override
        public void onMessage(final Message message) {
            if (getLog().isDebugEnabled()) {
                getLog().debug("Message: %s").with(message);
            }

            synchronized (GeminiTradeConnector.this) {
                handleMessage(message);
            }
        }

        @Override
        public void onStatusChange(SessionStatus newStatus) {
            synchronized (GeminiTradeConnector.this) {
                handleSessionStateChanged(newStatus);
            }
        }
    }

    // endregion
}
