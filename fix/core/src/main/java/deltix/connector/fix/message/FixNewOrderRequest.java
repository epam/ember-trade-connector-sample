package deltix.connector.fix.message;

import deltix.ember.message.trade.CustomAttribute;
import deltix.util.collections.generated.ObjectList;

public class FixNewOrderRequest extends FixOrderStateMessage {
    @Override
    public void applyAttributes(ObjectList<CustomAttribute> attributes) {
    }

    @Override
    public void reuse() {
        super.reuse();
    }

    @Override
    public void validate() {
    }
}
