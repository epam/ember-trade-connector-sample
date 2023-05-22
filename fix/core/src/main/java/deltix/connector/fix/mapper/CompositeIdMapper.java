package deltix.connector.fix.mapper;

import deltix.anvil.util.AsciiStringBuilder;
import deltix.anvil.util.CharSequenceUtil;
import deltix.anvil.util.TypeConstants;
import deltix.anvil.util.codec.AlphanumericCodec;
import deltix.ember.message.trade.*;

/**
 * Implementation that uses Ember's {Source,OrderID} to identify orders on FIX side
 */
@SuppressWarnings("WeakerAccess")
public class CompositeIdMapper<
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

    protected final char sourceAndRequestIdSeparator;

    protected final AsciiStringBuilder orderIdFlyweight = new AsciiStringBuilder(32);
    protected final AsciiStringBuilder originalOrderIdFlyweight = new AsciiStringBuilder(32);


    public CompositeIdMapper(char sourceAndRequestIdSeparator) {
        this.sourceAndRequestIdSeparator = sourceAndRequestIdSeparator;
    }

    public CompositeIdMapper() {
        this('$');
    }

    /// region Requests
    @Override
    public void requestToMessage(final OrderNewRequest request, final FixNewOrderRequest message) {
        message.setOrderId(makeCompositeRequestId(request.getSourceId(), sourceAndRequestIdSeparator, request.getOrderId(), orderIdFlyweight));
    }

    @Override
    public void requestToMessage(final OrderReplaceRequest request, final FixReplaceOrderRequest message) {
        message.setOrderId(makeCompositeRequestId(request.getSourceId(), sourceAndRequestIdSeparator, request.getOrderId(), orderIdFlyweight));
        message.setOriginalOrderId(makeCompositeRequestId(request.getSourceId(), sourceAndRequestIdSeparator, request.getOriginalOrderId(), originalOrderIdFlyweight));
    }

    @Override
    public void requestToMessage(final OrderCancelRequest request, final FixCancelOrderRequest message) {
        message.setOrderId(makeCompositeRequestId(request.getSourceId(), sourceAndRequestIdSeparator, request.getRequestId(), orderIdFlyweight));
        message.setOriginalOrderId(makeCompositeRequestId(request.getSourceId(), sourceAndRequestIdSeparator, request.getOrderId(), originalOrderIdFlyweight));
    }

    @Override
    public void requestToMessage(final OrderStatusRequest request, final FixOrderStatusRequest message) {
        message.setOrderId(makeCompositeRequestId(request.getSourceId(), sourceAndRequestIdSeparator, request.getOrderId(), orderIdFlyweight));
    }

    // endregion

    /// region Events

    @Override
    public void messageToEvent(final FixExecutionReport message, final MutableOrderEvent event) {
        event.setDestinationId(message.hasOrderId() ? getSourceId(message.getOrderId(), sourceAndRequestIdSeparator) : TypeConstants.ALPHANUMERIC_NULL);
        event.setOrderId(message.hasOrderId() ? getRequestId(message.getOrderId(), sourceAndRequestIdSeparator, orderIdFlyweight) : null);
    }

    @Override
    public void messageToEvent(final FixExecutionReport message, final MutableOrderPendingCancelEvent event) {
        if (message.hasOriginalOrderId()) {
            event.setDestinationId(getSourceId(message.getOriginalOrderId(), sourceAndRequestIdSeparator));
            event.setOrderId(getRequestId(message.getOriginalOrderId(), sourceAndRequestIdSeparator, originalOrderIdFlyweight));
            event.setRequestId(message.hasOrderId() ? getRequestId(message.getOrderId(), sourceAndRequestIdSeparator, orderIdFlyweight) : null);
        } else {
            event.setDestinationId(message.hasOrderId() ? getSourceId(message.getOrderId(), sourceAndRequestIdSeparator) : TypeConstants.ALPHANUMERIC_NULL);
            event.setOrderId(message.hasOrderId() ? getRequestId(message.getOrderId(), sourceAndRequestIdSeparator, originalOrderIdFlyweight) : null);
            event.setRequestId(null);
        }
    }

    @Override
    public void messageToEvent(final FixExecutionReport message, final MutableOrderCancelEvent event) {
        if (message.hasOriginalOrderId()) {
            event.setDestinationId(getSourceId(message.getOriginalOrderId(), sourceAndRequestIdSeparator));
            event.setOrderId(getRequestId(message.getOriginalOrderId(), sourceAndRequestIdSeparator, originalOrderIdFlyweight));
            event.setRequestId(message.hasOrderId() ? getRequestId(message.getOrderId(), sourceAndRequestIdSeparator, orderIdFlyweight) : null);
        } else {
            event.setDestinationId(message.hasOrderId() ? getSourceId(message.getOrderId(), sourceAndRequestIdSeparator) : TypeConstants.ALPHANUMERIC_NULL);
            event.setOrderId(message.hasOrderId() ? getRequestId(message.getOrderId(), sourceAndRequestIdSeparator, originalOrderIdFlyweight) : null);
            event.setRequestId(null);
        }
    }

    @Override
    public void messageToEvent(final FixExecutionReport message, final MutableOrderPendingReplaceEvent event) {
        messageToEvent(message, (MutableOrderEvent) event);
        event.setOriginalOrderId(message.hasOriginalOrderId() ? getRequestId(message.getOriginalOrderId(), sourceAndRequestIdSeparator, originalOrderIdFlyweight) : null);
    }

    @Override
    public void messageToEvent(final FixExecutionReport message, final MutableOrderReplaceEvent event) {
        messageToEvent(message, (MutableOrderEvent) event);
        event.setOriginalOrderId(message.hasOriginalOrderId() ? getRequestId(message.getOriginalOrderId(), sourceAndRequestIdSeparator, originalOrderIdFlyweight) : null);
    }

    @Override
    public void messageToEvent(final FixNewOrderRequest refMessage, final MutableOrderRejectEvent event) {
        event.setDestinationId(refMessage.hasOrderId() ? getSourceId(refMessage.getOrderId(), sourceAndRequestIdSeparator) : TypeConstants.ALPHANUMERIC_NULL);
        event.setOrderId(refMessage.hasOrderId() ? getRequestId(refMessage.getOrderId(), sourceAndRequestIdSeparator, orderIdFlyweight) : null);
    }

    @Override
    public void messageToEvent(final FixCancelOrderRequest refMessage, final MutableOrderCancelRejectEvent event) {
        event.setDestinationId(refMessage.hasOriginalOrderId() ? getSourceId(refMessage.getOriginalOrderId(), sourceAndRequestIdSeparator) : TypeConstants.ALPHANUMERIC_NULL);
        event.setOrderId(refMessage.hasOriginalOrderId() ? getRequestId(refMessage.getOriginalOrderId(), sourceAndRequestIdSeparator, originalOrderIdFlyweight) : null);
        event.setRequestId(refMessage.hasOrderId() ? getRequestId(refMessage.getOrderId(), sourceAndRequestIdSeparator, orderIdFlyweight) : null);
    }

    @Override
    public void messageToEvent(final FixReplaceOrderRequest refMessage, final MutableOrderReplaceRejectEvent event) {
        event.setDestinationId(refMessage.hasOrderId() ? getSourceId(refMessage.getOrderId(), sourceAndRequestIdSeparator) : TypeConstants.ALPHANUMERIC_NULL);
        event.setOrderId(refMessage.hasOrderId() ? getRequestId(refMessage.getOrderId(), sourceAndRequestIdSeparator, orderIdFlyweight) : null);
        event.setOriginalOrderId(refMessage.hasOriginalOrderId() ? getRequestId(refMessage.getOriginalOrderId(), sourceAndRequestIdSeparator, originalOrderIdFlyweight) : null);
    }

    @Override
    public void messageToEvent(final FixCancelReject message, final MutableOrderCancelRejectEvent event) {
        event.setDestinationId(message.hasOriginalOrderId() ? getSourceId(message.getOriginalOrderId(), sourceAndRequestIdSeparator) : TypeConstants.ALPHANUMERIC_NULL);
        event.setOrderId(message.hasOriginalOrderId() ? getRequestId(message.getOriginalOrderId(), sourceAndRequestIdSeparator, originalOrderIdFlyweight) : null);
        event.setRequestId(message.hasOrderId() ? getRequestId(message.getOrderId(), sourceAndRequestIdSeparator, orderIdFlyweight) : null);
    }

    @Override
    public void messageToEvent(final FixCancelReject message, final MutableOrderReplaceRejectEvent event) {
        event.setDestinationId(message.hasOrderId() ? getSourceId(message.getOrderId(), sourceAndRequestIdSeparator) : TypeConstants.ALPHANUMERIC_NULL);
        event.setOrderId(message.hasOrderId() ? getRequestId(message.getOrderId(), sourceAndRequestIdSeparator, orderIdFlyweight) : null);
        event.setOriginalOrderId(message.hasOriginalOrderId() ? getRequestId(message.getOriginalOrderId(), sourceAndRequestIdSeparator, originalOrderIdFlyweight) : null);
    }

    // endregion

    //region Ids Transformation

    protected static CharSequence makeCompositeRequestId(final long sourceId, final char separator, final CharSequence requestId, final AsciiStringBuilder flyweight) {
        flyweight.clear();
        flyweight.appendAlphanumeric(sourceId);
        flyweight.append(separator);
        flyweight.append(requestId);
        return flyweight;
    }

    protected static long getSourceId(final CharSequence compositeRequestId, final char separator) {
        int index = CharSequenceUtil.indexOf(compositeRequestId, separator);
        if (index == -1) {
            return TypeConstants.ALPHANUMERIC_NULL;
        }

        return AlphanumericCodec.encode(compositeRequestId, 0, index);
    }

    protected static CharSequence getRequestId(final CharSequence compositeRequestId, final char separator, final AsciiStringBuilder flyweight) {
        int index = CharSequenceUtil.indexOf(compositeRequestId, separator);
        if (index == -1) {
            return compositeRequestId;
        }

        flyweight.clear();
        flyweight.append(compositeRequestId, index + 1, compositeRequestId.length());
        return flyweight;
    }

    //endregion

    // region Order Events

    @Override
    public void onOrderPendingNewEvent(final OrderPendingNewEvent event) {
    }

    @Override
    public void onOrderNewEvent(final OrderNewEvent event) {
    }

    @Override
    public void onOrderRejectEvent(final OrderRejectEvent event) {
    }

    @Override
    public void onOrderPendingCancelEvent(final OrderPendingCancelEvent event) {
    }

    @Override
    public void onOrderCancelEvent(final OrderCancelEvent event) {
    }

    @Override
    public void onOrderCancelRejectEvent(final OrderCancelRejectEvent event) {
    }

    @Override
    public void onOrderPendingReplaceEvent(final OrderPendingReplaceEvent event) {
    }

    @Override
    public void onOrderReplaceEvent(final OrderReplaceEvent event) {
    }

    @Override
    public void onOrderReplaceRejectEvent(final OrderReplaceRejectEvent event) {
    }

    @Override
    public void onOrderTradeReportEvent(final OrderTradeReportEvent event) {
    }

    @Override
    public void onOrderTradeCancelEvent(final OrderTradeCancelEvent event) {
    }

    @Override
    public void onOrderTradeCorrectEvent(final OrderTradeCorrectEvent event) {
    }

    @Override
    public void onOrderStatusEvent(final OrderStatusEvent event) {
    }

    @Override
    public void onOrderRestateEvent(final OrderRestateEvent event) {
    }

    // endregion

}
