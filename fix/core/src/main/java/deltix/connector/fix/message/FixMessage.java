package deltix.connector.fix.message;

import deltix.anvil.util.Reusable;
import deltix.ember.message.trade.CustomAttribute;
import deltix.util.collections.generated.ObjectList;

public interface FixMessage extends Reusable {

    void validate();

    void applyAttributes(ObjectList<CustomAttribute> attributes);

}
