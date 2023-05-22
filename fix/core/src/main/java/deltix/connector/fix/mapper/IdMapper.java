package deltix.connector.fix.mapper;

import deltix.ember.message.trade.*;
import deltix.ember.service.OrderEventHandler;

/**
 * Interface responsible for encoding Ember order identify {Source, ID} into FIX message and retrieving it back
 */
public interface IdMapper<
        FixNewOrderRequest extends deltix.connector.fix.message.FixNewOrderRequest,
        FixCancelOrderRequest extends deltix.connector.fix.message.FixCancelOrderRequest,
        FixReplaceOrderRequest extends deltix.connector.fix.message.FixReplaceOrderRequest,
        FixOrderStatusRequest extends deltix.connector.fix.message.FixOrderStatusRequest,
        FixExecutionReport extends deltix.connector.fix.message.FixExecutionReport,
        FixCancelReject extends deltix.connector.fix.message.FixCancelReject>
        extends OrderEventHandler {

    void requestToMessage(OrderNewRequest request, FixNewOrderRequest message);

    void requestToMessage(OrderReplaceRequest request, FixReplaceOrderRequest message);

    void requestToMessage(OrderCancelRequest request, FixCancelOrderRequest message);

    void requestToMessage(OrderStatusRequest request, FixOrderStatusRequest message);


    void messageToEvent(FixNewOrderRequest refMessage, MutableOrderRejectEvent event);

    void messageToEvent(FixCancelOrderRequest refMessage, MutableOrderCancelRejectEvent event);

    void messageToEvent(FixReplaceOrderRequest refMessage, MutableOrderReplaceRejectEvent event);


    void messageToEvent(FixExecutionReport message, MutableOrderEvent event);


    void messageToEvent(FixExecutionReport message, MutableOrderPendingCancelEvent event);

    void messageToEvent(FixExecutionReport message, MutableOrderCancelEvent event);

    void messageToEvent(FixExecutionReport message, MutableOrderPendingReplaceEvent event);

    void messageToEvent(FixExecutionReport message, MutableOrderReplaceEvent event);

    default void messageToEvent(FixExecutionReport message, MutableOrderStatusEvent event) {
        messageToEvent(message, (MutableOrderEvent) event);
    }


    void messageToEvent(FixCancelReject message, MutableOrderCancelRejectEvent event);

    void messageToEvent(FixCancelReject message, MutableOrderReplaceRejectEvent event);

    @Override
    default void onOrderPendingNewEvent(OrderPendingNewEvent event) {
    }

    @Override
    default void onOrderNewEvent(OrderNewEvent event) {
    }

    @Override
    default void onOrderRejectEvent(OrderRejectEvent event) {
    }

    @Override
    default void onOrderPendingCancelEvent(OrderPendingCancelEvent event) {
    }

    @Override
    default void onOrderCancelEvent(OrderCancelEvent event) {
    }

    @Override
    default void onOrderCancelRejectEvent(OrderCancelRejectEvent event) {
    }

    @Override
    default void onOrderPendingReplaceEvent(OrderPendingReplaceEvent event) {
    }

    @Override
    default void onOrderReplaceEvent(OrderReplaceEvent event) {
    }

    @Override
    default void onOrderReplaceRejectEvent(OrderReplaceRejectEvent event) {
    }

    @Override
    default void onOrderTradeReportEvent(OrderTradeReportEvent event) {
    }

    @Override
    default void onOrderTradeCancelEvent(OrderTradeCancelEvent event) {
    }

    @Override
    default void onOrderTradeCorrectEvent(OrderTradeCorrectEvent event) {
    }

    @Override
    default void onOrderStatusEvent(OrderStatusEvent event) {
    }

    @Override
    default void onOrderRestateEvent(OrderRestateEvent event) {
    }

}
