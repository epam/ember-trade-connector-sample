package deltix.connector.common.util;

import deltix.ember.message.trade.OrderCancelRequest;
import deltix.ember.message.trade.OrderNewRequest;
import deltix.ember.message.trade.OrderReplaceRequest;

public interface RequestValidator {
    RequestValidator NOOP_VALIDATOR = new RequestValidator() {
        @Override
        public void validateSubmit(OrderNewRequest request) {
        }

        @Override
        public void validateModify(OrderReplaceRequest request) {
        }

        @Override
        public void validateCancel(OrderCancelRequest request) {
        }
    };

    void validateSubmit(OrderNewRequest request);
    void validateModify(OrderReplaceRequest request);
    void validateCancel(OrderCancelRequest request);
}
