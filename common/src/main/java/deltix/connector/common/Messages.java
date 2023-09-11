package deltix.connector.common;

import deltix.anvil.util.Reusable;
import deltix.anvil.util.TypeConstants;
import deltix.anvil.util.annotation.Timestamp;
import deltix.ember.message.trade.*;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class Messages {

    private final MutableSessionStatusEvent sessionStatusEvent = new MutableSessionStatusEvent();

    private final MutableOrderNewRequest newOrderRequest = new MutableOrderNewRequest();
    private final MutableOrderCancelRequest cancelOrderRequest = new MutableOrderCancelRequest();
    private final MutableOrderMassCancelRequest massCancelOrderRequest = new MutableOrderMassCancelRequest();
    private final MutableOrderReplaceRequest replaceOrderRequest = new MutableOrderReplaceRequest();

    private final MutableOrderPendingNewEvent orderPendingNewEvent = new MutableOrderPendingNewEvent();
    private final MutableOrderNewEvent orderNewEvent = new MutableOrderNewEvent();
    private final MutableOrderRejectEvent orderRejectEvent = new MutableOrderRejectEvent();
    private final MutableOrderPendingCancelEvent orderPendingCancelEvent = new MutableOrderPendingCancelEvent();
    private final MutableOrderCancelEvent orderCancelEvent = new MutableOrderCancelEvent();
    private final MutableOrderCancelRejectEvent orderCancelRejectEvent = new MutableOrderCancelRejectEvent();
    private final MutableOrderPendingReplaceEvent orderPendingReplaceEvent = new MutableOrderPendingReplaceEvent();
    private final MutableOrderReplaceEvent orderReplaceEvent = new MutableOrderReplaceEvent();
    private final MutableOrderReplaceRejectEvent orderReplaceRejectEvent = new MutableOrderReplaceRejectEvent();
    private final MutableOrderStatusEvent orderStatusEvent = new MutableOrderStatusEvent();
    private final MutableOrderTradeReportEvent tradeReportEvent = new MutableOrderTradeReportEvent();
    private final MutableOrderTradeCancelEvent tradeCancelEvent = new MutableOrderTradeCancelEvent();
    private final MutableOrderTradeCorrectEvent tradeCorrectEvent = new MutableOrderTradeCorrectEvent();

    public MutableSessionStatusEvent sessionStatusEvent() {
        return reuse(sessionStatusEvent);
    }

    public MutableOrderNewRequest newOrderRequest() {
        return reuse(newOrderRequest);
    }

    public MutableOrderCancelRequest cancelOrderRequest() {
        return reuse(cancelOrderRequest);
    }

    public MutableOrderReplaceRequest replaceOrderRequest() {
        return reuse(replaceOrderRequest);
    }

    public MutableOrderMassCancelRequest massCancelOrderRequest() {
        return reuse(massCancelOrderRequest);
    }

    public MutableOrderPendingNewEvent orderPendingNewEvent() {
        return reuse(orderPendingNewEvent);
    }

    public MutableOrderNewEvent orderNewEvent() {
        return reuse(orderNewEvent);
    }

    public MutableOrderRejectEvent orderRejectEvent() {
        return reuse(orderRejectEvent);
    }

    public MutableOrderPendingCancelEvent orderPendingCancelEvent() {
        return reuse(orderPendingCancelEvent);
    }

    public MutableOrderCancelEvent orderCancelEvent() {
        return reuse(orderCancelEvent);
    }

    public MutableOrderCancelRejectEvent orderCancelRejectEvent() {
        return reuse(orderCancelRejectEvent);
    }

    public MutableOrderPendingReplaceEvent orderPendingReplaceEvent() {
        return reuse(orderPendingReplaceEvent);
    }

    public MutableOrderReplaceEvent orderReplaceEvent() {
        return reuse(orderReplaceEvent);
    }

    public MutableOrderReplaceRejectEvent orderReplaceRejectEvent() {
        return reuse(orderReplaceRejectEvent);
    }

    public MutableOrderStatusEvent orderStatusEvent() {
        return reuse(orderStatusEvent);
    }

    public MutableOrderTradeReportEvent tradeReportEvent() {
        return reuse(tradeReportEvent);
    }

    public MutableOrderTradeCancelEvent tradeCancelEvent() {
        return reuse(tradeCancelEvent);
    }

    public MutableOrderTradeCorrectEvent tradeCorrectEvent() {
        return reuse(tradeCorrectEvent);
    }


    public static void makeOrderRejectEvent(@Timestamp(TimeUnit.NANOSECONDS) long timestampNs, CharSequence reason, OrderNewRequest request, MutableOrderRejectEvent event) {
        event.setReason(reason);
        event.setAveragePrice(TypeConstants.DECIMAL64_NULL);
        event.setCumulativeQuantity(TypeConstants.DECIMAL64_NULL);
        event.setDisplayQuantity(request.getDisplayQuantity());
        event.setExpireTime(request.getExpireTime());
        event.setLimitPrice(request.getLimitPrice());
        event.setMinQuantity(request.getMinQuantity());
        event.setQuantity(request.getQuantity());
        event.setRemainingQuantity(TypeConstants.DECIMAL64_NULL);
        event.setStopPrice(request.getStopPrice());
        event.setInstrumentType(request.getInstrumentType());
        event.setOrderType(request.getOrderType());
        event.setSide(request.getSide());
        event.setTimeInForce(request.getTimeInForce());
        event.setAccount(request.getAccount());
        event.setEventId(null);
        event.setExchangeId(request.getExchangeId());
        event.setSymbol(request.getSymbol());
        event.setTimestampNs(timestampNs);
        event.setOrderStatus(OrderStatus.REJECTED);
        event.setExternalOrderId(null);
        event.setOrderId(request.getOrderId());
        event.setDestinationId(request.getSourceId());
        event.setSourceId(request.getDestinationId());

        event.setAttributes(request.getAttributes());
    }

    public static void makeOrderReplaceRejectEvent(@Timestamp(TimeUnit.NANOSECONDS) long timestampNs, CharSequence reason, OrderReplaceRequest request, MutableOrderReplaceRejectEvent event) {
        event.setOriginalOrderId(request.getOriginalOrderId());
        event.setReason(reason);
        event.setTimestampNs(timestampNs);
        event.setExternalOrderId(request.getExternalOrderId());
        event.setOrderId(request.getOrderId());
        event.setDestinationId(request.getSourceId());
        event.setSourceId(request.getDestinationId());

        event.setAttributes(request.getAttributes());
    }

    public static void makeOrderCancelRejectEvent(@Timestamp(TimeUnit.NANOSECONDS) long timestampNs, CharSequence reason, OrderCancelRequest request, MutableOrderCancelRejectEvent event) {
        event.setReason(reason);
        event.setRequestId(request.getRequestId());
        event.setTimestampNs(timestampNs);
        event.setExternalOrderId(request.getExternalOrderId());
        event.setOrderId(request.getOrderId());
        event.setDestinationId(request.getSourceId());
        event.setSourceId(request.getDestinationId());

        event.setAttributes(request.getAttributes());
    }

    protected static <T extends Reusable> T reuse(T reusable) {
        reusable.reuse();
        return reusable;
    }

}
