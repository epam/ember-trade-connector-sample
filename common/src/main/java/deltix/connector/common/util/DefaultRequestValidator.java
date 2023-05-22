package deltix.connector.common.util;

import deltix.ember.message.trade.*;
import deltix.ember.service.valid.InvalidOrderException;
import deltix.ember.service.valid.InvalidOrderStateException;

import java.util.EnumSet;

public final class DefaultRequestValidator implements RequestValidator {

    private final EnumSet<Side> sides;
    private final EnumSet<OrderType> orderTypes;
    private final EnumSet<TimeInForce> timeInForces;

    private final boolean displayQtyAllowed;
    private final boolean minQtyAllowed;

    private final boolean fastCancelAllowed;
    private final boolean fastReplaceAllowed;

    private DefaultRequestValidator(EnumSet<Side> sides,
                                    EnumSet<OrderType> orderTypes,
                                    EnumSet<TimeInForce> timeInForces,
                                    boolean displayQtyAllowed, boolean minQtyAllowed,
                                    boolean fastCancelAllowed, boolean fastReplaceAllowed) {
        this.sides = sides;
        this.orderTypes = orderTypes;
        this.timeInForces = timeInForces;
        this.displayQtyAllowed = displayQtyAllowed;
        this.minQtyAllowed = minQtyAllowed;
        this.fastCancelAllowed = fastCancelAllowed;
        this.fastReplaceAllowed = fastReplaceAllowed;
    }

    public static RequestValidatorBuilder createBuilder() {
        return new RequestValidatorBuilder();
    }

    // region RequestValidator IMPL

    @Override
    public void validateSubmit(final OrderNewRequest request) {
        if (sides != null && !sides.isEmpty() && !sides.contains(request.getSide()))
            throw new InvalidOrderException("Unsupported order side: " + request.getSide());

        if (timeInForces != null && !timeInForces.isEmpty() && !timeInForces.contains(request.getTimeInForce()))
            throw new InvalidOrderException("Unsupported Time In Force: " + request.getTimeInForce());

        if (orderTypes != null && !orderTypes.isEmpty() && !orderTypes.contains(request.getOrderType()))
            throw new InvalidOrderException("Unsupported order type: " + request.getOrderType());

        if (!displayQtyAllowed && request.hasDisplayQuantity())
            throw new InvalidOrderException("Display quantity is not allowed");

        if (!minQtyAllowed && request.hasMinQuantity())
            throw new InvalidOrderException("Minimum quantity is not allowed");
    }

    @Override
    public void validateModify(final OrderReplaceRequest request) {
        if (!fastReplaceAllowed && !request.hasExternalOrderId())
            throw InvalidOrderStateException.CANT_MODIFY_UNACKNOWLEDGED_ORDER;

        if (!displayQtyAllowed && request.hasDisplayQuantity())
            throw new InvalidOrderException("Display quantity is not allowed");

        if (!minQtyAllowed && request.hasMinQuantity())
            throw new InvalidOrderException("Minimum quantity is not allowed");
    }

    @Override
    public void validateCancel(final OrderCancelRequest request) {
        if (!fastCancelAllowed && !request.hasExternalOrderId())
            throw InvalidOrderStateException.CANT_CANCEL_UNACKNOWLEDGED_ORDER;
    }

    // endregion

    // region Builder

    public static final class RequestValidatorBuilder {
        private EnumSet<Side> sides;
        private EnumSet<OrderType> orderTypes;
        private EnumSet<TimeInForce> timeInForces;
        private boolean displayQtyAllowed = true;
        private boolean minQtyAllowed = true;
        private boolean fastCancelAllowed = true;
        private boolean fastReplaceAllowed = true;

        private RequestValidatorBuilder() {
        }

        public DefaultRequestValidator build() {
            return new DefaultRequestValidator(sides, orderTypes, timeInForces, displayQtyAllowed, minQtyAllowed, fastCancelAllowed, fastReplaceAllowed);
        }

        public RequestValidatorBuilder sides(final EnumSet<Side> sides) {
            this.sides = sides;
            return this;
        }

        public RequestValidatorBuilder orderTypes(final EnumSet<OrderType> orderTypes) {
            this.orderTypes = orderTypes;
            return this;
        }

        public RequestValidatorBuilder timeInForces(final EnumSet<TimeInForce> timeInForces) {
            this.timeInForces = timeInForces;
            return this;
        }

        public RequestValidatorBuilder allowDisplayQty(final boolean displayQtyAllowed) {
            this.displayQtyAllowed = displayQtyAllowed;
            return this;
        }

        public RequestValidatorBuilder allowMinQty(final boolean minQtyAllowed) {
            this.minQtyAllowed = minQtyAllowed;
            return this;
        }

        public RequestValidatorBuilder allowFastCancel(final boolean fastCancelAllowed) {
            this.fastCancelAllowed = fastCancelAllowed;
            return this;
        }

        public RequestValidatorBuilder allowFastReplace(final boolean fastReplaceAllowed) {
            this.fastReplaceAllowed = fastReplaceAllowed;
            return this;
        }
    }

    // endregion
}
