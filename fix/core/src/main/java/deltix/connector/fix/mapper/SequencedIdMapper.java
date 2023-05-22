package deltix.connector.fix.mapper;

import deltix.anvil.util.AsciiStringBuilder;
import deltix.anvil.util.AsciiStringFlyweight;
import deltix.anvil.util.CharSequenceParser;
import deltix.anvil.util.annotation.Alphanumeric;
import deltix.ember.message.trade.*;

@SuppressWarnings("unused")
public class SequencedIdMapper<
        FixNewOrderRequest extends deltix.connector.fix.message.FixNewOrderRequest,
        FixCancelOrderRequest extends deltix.connector.fix.message.FixCancelOrderRequest,
        FixReplaceOrderRequest extends deltix.connector.fix.message.FixReplaceOrderRequest,
        FixOrderStatusRequest extends deltix.connector.fix.message.FixOrderStatusRequest,
        FixExecutionReport extends deltix.connector.fix.message.FixExecutionReport,
        FixCancelReject extends deltix.connector.fix.message.FixCancelReject
        > implements IdMapper<
        FixNewOrderRequest,
        FixCancelOrderRequest,
        FixReplaceOrderRequest,
        FixOrderStatusRequest,
        FixExecutionReport,
        FixCancelReject> {

    protected static final long REQUEST_SEQ_BASE = 900_000_000_000_000L;
    protected static final long REQUEST_ID_INVALID = Long.MIN_VALUE;

    protected final OrderIdCache orderIdCache;
    protected final CancelIdCache cancelIdCache;

    protected final AsciiStringBuilder orderIdBuilder = new AsciiStringBuilder(20);
    protected final AsciiStringBuilder orderOriginalIdBuilder = new AsciiStringBuilder(20);

    protected final OrderIdFlyweight orderOrderIdFlyweight = new OrderIdFlyweight();
    protected final OrderIdFlyweight originalOrderOrderIdFlyweight = new OrderIdFlyweight();
    protected final AsciiStringFlyweight requestIdFlyweight = new AsciiStringFlyweight();

    // initial request sequence - 10M unique ids per day
    // it is used for cancel request id if Ember does not provide sequence in OrderCancelRequest (mass cancel case)
    protected long requestSequence = REQUEST_SEQ_BASE + (System.currentTimeMillis() / (24 * 3_600_000) * 10_000_000);


    public SequencedIdMapper() {
        this(64 * 1024, 8 * 1024, 8 * 1024);
    }

    public SequencedIdMapper(final int orderIdActiveCacheInitialCapacity,
                             final int orderIdInactiveCacheCapacity,
                             final int cancelIdCacheCapacity) {

        this.orderIdCache = new OrderIdCache(orderIdActiveCacheInitialCapacity, orderIdInactiveCacheCapacity);
        this.cancelIdCache = new CancelIdCache(cancelIdCacheCapacity);
    }

    // region Data Request -> FIX Request

    @Override
    public void requestToMessage(final OrderNewRequest request, final FixNewOrderRequest message) {
        final @Alphanumeric long sourceId = request.getSourceId();
        final CharSequence orderId = request.getOrderId();
        final long sequence = request.getSequence();

        orderIdCache.put(sequence, sourceId, orderId);

        final AsciiStringBuilder externalOrderId = orderIdBuilder.clear().append(sequence);
        message.setOrderId(externalOrderId);
    }

    @Override
    public void requestToMessage(final OrderReplaceRequest request, final FixReplaceOrderRequest message) {
        final @Alphanumeric long sourceId = request.getSourceId();
        final CharSequence orderId = request.getOrderId();
        final CharSequence originalOrderId = request.getOriginalOrderId();

        final long sequence = request.getSequence();
        final long originalSequence = orderIdCache.get(sourceId, originalOrderId);

        orderIdCache.put(sequence, sourceId, orderId);

        final AsciiStringBuilder externalOrderId = orderIdBuilder.clear().append(sequence);
        final AsciiStringBuilder externalOriginalOrderId = orderOriginalIdBuilder.clear().append(originalSequence);

        message.setOrderId(externalOrderId);
        message.setOriginalOrderId(externalOriginalOrderId);
    }

    @Override
    public void requestToMessage(final OrderCancelRequest request, final FixCancelOrderRequest message) {
        final @Alphanumeric long sourceId = request.getSourceId();
        final CharSequence requestId = request.getRequestId();
        final CharSequence orderId = request.getOrderId();

        // IMPORTANT: in case of mass cancel Ember does not provide sequence for OrderCancelRequest
        final long requestSequence = request.hasSequence() ? request.getSequence() : nextRequestId();
        final long orderSequence = orderIdCache.get(sourceId, orderId);

        cancelIdCache.put(requestSequence, requestId);

        final AsciiStringBuilder externalOrderId = orderIdBuilder.clear().append(requestSequence);
        final AsciiStringBuilder externalOriginalOrderId = orderOriginalIdBuilder.clear().append(orderSequence);

        message.setOrderId(externalOrderId);
        message.setOriginalOrderId(externalOriginalOrderId);
    }

    protected long nextRequestId() {
        return ++requestSequence;
    }

    @Override
    public void requestToMessage(final OrderStatusRequest request, final FixOrderStatusRequest message) {
        final @Alphanumeric long sourceId = request.getSourceId();
        final CharSequence orderId = request.getOrderId();
        final long sequence = orderIdCache.get(sourceId, orderId);

        final AsciiStringBuilder externalOrderId = orderIdBuilder.clear().append(sequence);
        message.setOrderId(externalOrderId);
    }

    // endregion

    // region FIX Request -> Data Event

    @Override
    public void messageToEvent(final FixNewOrderRequest message, final MutableOrderRejectEvent event) {
        final CharSequence externalOrderId = message.getOrderId();
        final long sequence = CharSequenceParser.parseLong(externalOrderId);

        orderIdCache.get(sequence, orderOrderIdFlyweight);

        event.setDestinationId(orderOrderIdFlyweight.getSourceId());
        event.setOrderId(orderOrderIdFlyweight.getOrderId());
    }

    @Override
    public void messageToEvent(final FixReplaceOrderRequest message, final MutableOrderReplaceRejectEvent event) {
        final CharSequence externalOrderId = message.getOrderId();
        final CharSequence externalOriginalOrderId = message.getOriginalOrderId();

        final long sequence = CharSequenceParser.parseLong(externalOrderId);
        final long originalSequence = CharSequenceParser.parseLong(externalOriginalOrderId);

        orderIdCache.get(sequence, orderOrderIdFlyweight);
        orderIdCache.get(originalSequence, originalOrderOrderIdFlyweight);

        event.setDestinationId(orderOrderIdFlyweight.getSourceId());
        event.setOrderId(orderOrderIdFlyweight.getOrderId());
        event.setOriginalOrderId(originalOrderOrderIdFlyweight.getOrderId());
    }

    @Override
    public void messageToEvent(final FixCancelOrderRequest message, final MutableOrderCancelRejectEvent event) {
        final CharSequence externalOrderId = message.getOrderId();
        final CharSequence externalOriginalOrderId = message.getOriginalOrderId();

        final long requestSequence = CharSequenceParser.parseLong(externalOrderId);
        final long orderSequence = CharSequenceParser.parseLong(externalOriginalOrderId);

        final AsciiStringFlyweight requestId = cancelIdCache.get(requestSequence, requestIdFlyweight);
        orderIdCache.get(orderSequence, orderOrderIdFlyweight);

        event.setRequestId(requestId);
        event.setOrderId(orderOrderIdFlyweight.getOrderId());
        event.setDestinationId(orderOrderIdFlyweight.getSourceId());
    }

    // endregion


    // region FIX Event -> Data Event

    @Override
    public void messageToEvent(final FixExecutionReport message, final MutableOrderEvent event) {
        final CharSequence externalOrderId = message.getOrderId();
        final long sequence = CharSequenceParser.parseLong(externalOrderId);

        orderIdCache.get(sequence, orderOrderIdFlyweight);

        event.setOrderId(orderOrderIdFlyweight.getOrderId());
        event.setDestinationId(orderOrderIdFlyweight.getSourceId());
    }

    @Override
    public void messageToEvent(final FixExecutionReport message, final MutableOrderPendingCancelEvent event) {
        final CharSequence externalOrderId = message.hasOriginalOrderId() ? message.getOriginalOrderId() : message.getOrderId();

        final long orderSequence = CharSequenceParser.parseLong(externalOrderId);
        orderIdCache.get(orderSequence, orderOrderIdFlyweight);

        event.setOrderId(orderOrderIdFlyweight.getOrderId());
        event.setDestinationId(orderOrderIdFlyweight.getSourceId());

        AsciiStringFlyweight requestId = null;

        if (message.hasOriginalOrderId()) {
            final CharSequence externalRequestId = message.getOrderId();
            final long requestSequence = parseRequestId(externalRequestId);

            requestId = cancelIdCache.get(requestSequence, requestIdFlyweight);
        }

        event.setRequestId(requestId);
    }

    @Override
    public void messageToEvent(final FixExecutionReport message, final MutableOrderCancelEvent event) {
        final CharSequence externalOrderId = message.hasOriginalOrderId() ? message.getOriginalOrderId() : message.getOrderId();

        final long orderIdSequence = CharSequenceParser.parseLong(externalOrderId);
        orderIdCache.get(orderIdSequence, orderOrderIdFlyweight);

        event.setOrderId(orderOrderIdFlyweight.getOrderId());
        event.setDestinationId(orderOrderIdFlyweight.getSourceId());

        AsciiStringFlyweight requestId = null;

        if (message.hasOriginalOrderId()) {
            final CharSequence externalRequestId = message.getOrderId();
            final long requestSequence = parseRequestId(externalRequestId);

            requestId = cancelIdCache.get(requestSequence, requestIdFlyweight);
        }

        event.setRequestId(requestId);
    }

    @Override
    public void messageToEvent(final FixExecutionReport message, final MutableOrderPendingReplaceEvent event) {
        final CharSequence externalOrderId = message.getOrderId();
        final CharSequence externalOriginalOrderId = message.getOriginalOrderId();

        final long orderSequence = CharSequenceParser.parseLong(externalOrderId);
        final long originalOrderSequence = CharSequenceParser.parseLong(externalOriginalOrderId);

        orderIdCache.get(orderSequence, orderOrderIdFlyweight);
        orderIdCache.get(originalOrderSequence, originalOrderOrderIdFlyweight);

        event.setDestinationId(orderOrderIdFlyweight.getSourceId());
        event.setOrderId(orderOrderIdFlyweight.getOrderId());
        event.setOriginalOrderId(originalOrderOrderIdFlyweight.getOrderId());
    }

    @Override
    public void messageToEvent(final FixExecutionReport message, final MutableOrderReplaceEvent event) {
        final CharSequence externalOrderId = message.getOrderId();
        final CharSequence externalOriginalOrderId = message.getOriginalOrderId();

        final long orderSequence = CharSequenceParser.parseLong(externalOrderId);
        final long originalOrderSequence = CharSequenceParser.parseLong(externalOriginalOrderId);

        orderIdCache.get(orderSequence, orderOrderIdFlyweight);
        orderIdCache.get(originalOrderSequence, originalOrderOrderIdFlyweight);

        event.setDestinationId(orderOrderIdFlyweight.getSourceId());
        event.setOrderId(orderOrderIdFlyweight.getOrderId());
        event.setOriginalOrderId(originalOrderOrderIdFlyweight.getOrderId());
    }

    @Override
    public void messageToEvent(final FixExecutionReport message, final MutableOrderStatusEvent event) {
        messageToEvent(message, (MutableOrderEvent) event);
    }

    @Override
    public void messageToEvent(final FixCancelReject message, final MutableOrderCancelRejectEvent event) {
        final CharSequence externalRequestId = message.getOrderId();
        final CharSequence externalOrderId = message.getOriginalOrderId();

        final long requestSequence = parseRequestId(externalRequestId);
        final long orderSequence = CharSequenceParser.parseLong(externalOrderId);

        orderIdCache.get(orderSequence, orderOrderIdFlyweight);
        final AsciiStringFlyweight requestId = cancelIdCache.get(requestSequence, requestIdFlyweight);

        event.setDestinationId(orderOrderIdFlyweight.getSourceId());
        event.setOrderId(orderOrderIdFlyweight.getOrderId());
        event.setRequestId(requestId);
    }

    @Override
    public void messageToEvent(final FixCancelReject message, final MutableOrderReplaceRejectEvent event) {
        final CharSequence externalOrderId = message.getOrderId();
        final CharSequence externalOriginalOrderId = message.getOriginalOrderId();

        final long orderSequence = CharSequenceParser.parseLong(externalOrderId);
        final long originalOrderSequence = CharSequenceParser.parseLong(externalOriginalOrderId);

        orderIdCache.get(orderSequence, orderOrderIdFlyweight);
        orderIdCache.get(originalOrderSequence, originalOrderOrderIdFlyweight);

        event.setDestinationId(orderOrderIdFlyweight.getSourceId());
        event.setOrderId(orderOrderIdFlyweight.getOrderId());
        event.setOriginalOrderId(originalOrderOrderIdFlyweight.getOrderId());
    }

    // endregion

    // region Order Events

    @Override
    public void onOrderPendingNewEvent(final OrderPendingNewEvent event) {
    }

    @Override
    public void onOrderNewEvent(final OrderNewEvent event) {
    }

    @Override
    public void onOrderRejectEvent(final OrderRejectEvent event) {
        // for events source and destination are swapped
        orderIdCache.remove(event.getDestinationId(), event.getOrderId());
    }

    @Override
    public void onOrderPendingCancelEvent(final OrderPendingCancelEvent event) {
    }

    @Override
    public void onOrderCancelEvent(final OrderCancelEvent event) {
        // for events source and destination are swapped
        orderIdCache.remove(event.getDestinationId(), event.getOrderId());
    }

    @Override
    public void onOrderCancelRejectEvent(final OrderCancelRejectEvent event) {
    }

    @Override
    public void onOrderPendingReplaceEvent(final OrderPendingReplaceEvent event) {
    }

    @Override
    public void onOrderReplaceEvent(final OrderReplaceEvent event) {
        // for events source and destination are swapped
        orderIdCache.remove(event.getDestinationId(), event.getOriginalOrderId());
    }

    @Override
    public void onOrderReplaceRejectEvent(final OrderReplaceRejectEvent event) {
        // for events source and destination are swapped
        orderIdCache.remove(event.getDestinationId(), event.getOrderId());
    }

    @Override
    public void onOrderTradeReportEvent(final OrderTradeReportEvent event) {
        if (event.getOrderStatus() == OrderStatus.COMPLETELY_FILLED) {
            // for events source and destination are swapped
            orderIdCache.remove(event.getDestinationId(), event.getOrderId());
        }
    }

    @Override
    public void onOrderTradeCancelEvent(final OrderTradeCancelEvent event) {
    }

    @Override
    public void onOrderTradeCorrectEvent(final OrderTradeCorrectEvent event) {
    }

    @Override
    public void onOrderStatusEvent(final OrderStatusEvent event) {
        final OrderStatus status = event.getOrderStatus();
        if (status == OrderStatus.REJECTED ||
                status == OrderStatus.CANCELED ||
                status == OrderStatus.COMPLETELY_FILLED) {

            // for events source and destination are swapped
            orderIdCache.remove(event.getDestinationId(), event.getOrderId());
        }
    }

    @Override
    public void onOrderRestateEvent(final OrderRestateEvent event) {
    }

    // endregion

    protected long parseRequestId(final CharSequence requestId) {
        long sequence = REQUEST_ID_INVALID;

        if (isValidLong(requestId)) {
            try {
                sequence = CharSequenceParser.parseLong(requestId);
            } catch (final NumberFormatException e) {
                // skip
            }
        }

        return sequence;
    }

    protected static boolean isValidLong(final CharSequence sequence) {
        if (sequence.length() > 20) {
            return false;
        }

        for (int i = 0, length = sequence.length(); i < length; i++) {
            final char c = sequence.charAt(i);

            if (c < '0' | c > '9') {
                return false;
            }
        }

        return true;
    }

}
