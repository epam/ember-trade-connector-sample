package deltix.connector.fix.message;

import deltix.anvil.util.annotation.Optional;
import deltix.anvil.util.annotation.Required;
import deltix.ember.message.trade.CustomAttribute;
import deltix.ember.service.valid.InvalidOrderException;
import deltix.util.collections.generated.ObjectList;

public class FixReplaceOrderRequest extends FixOrderStateMessage {
    @Required
    protected CharSequence originalOrderId = null;

    @Optional
    protected CharSequence externalOrderId = null;

    public void setOriginalOrderId(@Required CharSequence originalOrderId) {
        this.originalOrderId = originalOrderId;
    }

    @Required
    public CharSequence getOriginalOrderId() {
        return originalOrderId;
    }

    public boolean hasOriginalOrderId() {
        return originalOrderId != null;
    }

    public void setExternalOrderId(@Optional CharSequence externalOrderId) {
        this.externalOrderId = externalOrderId;
    }

    @Optional
    public CharSequence getExternalOrderId() {
        return externalOrderId;
    }

    public boolean hasExternalOrderId() {
        return externalOrderId != null;
    }

    @Override
    public void applyAttributes(ObjectList<CustomAttribute> attributes) {
    }

    @Override
    public void reuse() {
        super.reuse();

        this.originalOrderId = null;
        this.externalOrderId = null;
    }

    @Override
    public void validate() {
        if (!hasOriginalOrderId()) {
            throw new InvalidOrderException("Required field 'originalOrderId' is missing.");
        }
    }
}
