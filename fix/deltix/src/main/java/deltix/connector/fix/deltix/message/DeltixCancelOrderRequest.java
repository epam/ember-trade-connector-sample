package deltix.connector.fix.deltix.message;

import deltix.anvil.util.annotation.Optional;
import deltix.connector.fix.message.FixCancelOrderRequest;
import deltix.efix.message.field.Tag;
import deltix.ember.message.trade.CustomAttribute;
import deltix.ember.service.valid.InvalidOrderException;
import deltix.util.collections.generated.ObjectList;

public class DeltixCancelOrderRequest extends FixCancelOrderRequest {
    @Optional
    protected CharSequence senderSubId = null;

    @Optional
    protected CharSequence execBroker = null;

    public void setSenderSubId(@Optional CharSequence senderSubId) {
        this.senderSubId = senderSubId;
    }

    @Optional
    public CharSequence getSenderSubId() {
        return senderSubId;
    }

    public boolean hasSenderSubId() {
        return senderSubId != null;
    }

    public void setExecBroker(@Optional CharSequence execBroker) {
        this.execBroker = execBroker;
    }

    @Optional
    public CharSequence getExecBroker() {
        return execBroker;
    }

    public boolean hasExecBroker() {
        return execBroker != null;
    }

    @Override
    public void applyAttributes(ObjectList<CustomAttribute> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return;
        }

        for (int i = 0; i < attributes.size(); i++) {
            CustomAttribute attribute = attributes.get(i);
            int key = attribute.getKey();
            switch (key) {
                case Tag.ExecBroker:
                case 6076:
                    this.setExecBroker(attribute.getValue());
                    break;
            }
        }
    }

    @Override
    public void reuse() {
        super.reuse();

        this.senderSubId = null;
        this.execBroker = null;
    }

    @Override
    public void validate() {
        if (!hasOrderId()) {
            throw new InvalidOrderException("Required field 'orderId' is missing.");
        }
        if (!hasTransactTime()) {
            throw new InvalidOrderException("Required field 'transactTime' is missing.");
        }
    }
}
