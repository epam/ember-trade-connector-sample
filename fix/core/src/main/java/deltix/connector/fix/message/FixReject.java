package deltix.connector.fix.message;

import deltix.anvil.util.TypeConstants;
import deltix.anvil.util.annotation.Optional;
import deltix.anvil.util.annotation.Required;
import deltix.ember.message.trade.CustomAttribute;
import deltix.ember.service.valid.InvalidOrderException;
import deltix.util.collections.generated.ObjectList;

public class FixReject implements FixMessage {
    @Required
    protected int refMsgSeqNum = TypeConstants.INT_NULL;

    @Optional
    protected CharSequence text = null;

    public void setRefMsgSeqNum(@Required int refMsgSeqNum) {
        this.refMsgSeqNum = refMsgSeqNum;
    }

    @Required
    public int getRefMsgSeqNum() {
        return refMsgSeqNum;
    }

    public boolean hasRefMsgSeqNum() {
        return refMsgSeqNum != TypeConstants.INT_NULL;
    }

    public void setText(@Optional CharSequence text) {
        this.text = text;
    }

    @Optional
    public CharSequence getText() {
        return text;
    }

    public boolean hasText() {
        return text != null;
    }

    @Override
    public void applyAttributes(ObjectList<CustomAttribute> attributes) {
    }

    @Override
    public void reuse() {
        this.refMsgSeqNum = TypeConstants.INT_NULL;
        this.text = null;
    }

    @Override
    public void validate() {
        if (!hasRefMsgSeqNum()) {
            throw new InvalidOrderException("Required field 'refMsgSeqNum' is missing.");
        }
    }
}
