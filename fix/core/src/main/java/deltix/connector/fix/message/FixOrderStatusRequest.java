package deltix.connector.fix.message;

import deltix.anvil.util.TypeConstants;
import deltix.anvil.util.annotation.Optional;
import deltix.anvil.util.annotation.Required;
import deltix.anvil.util.annotation.Timestamp;
import deltix.ember.message.trade.CustomAttribute;
import deltix.ember.service.valid.InvalidOrderException;
import deltix.util.collections.generated.ObjectList;

public class FixOrderStatusRequest implements FixMessage {
    @Required
    protected CharSequence orderId = null;

    @Optional
    protected CharSequence externalOrderId = null;

    @Required
    protected CharSequence symbol = null;

    @Required
    @Timestamp
    protected long transactTime = TypeConstants.TIMESTAMP_NULL;

    public void setOrderId(@Required CharSequence orderId) {
        this.orderId = orderId;
    }

    @Required
    public CharSequence getOrderId() {
        return orderId;
    }

    public boolean hasOrderId() {
        return orderId != null;
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

    public void setSymbol(@Required CharSequence symbol) {
        this.symbol = symbol;
    }

    @Required
    public CharSequence getSymbol() {
        return symbol;
    }

    public boolean hasSymbol() {
        return symbol != null;
    }

    public void setTransactTime(@Required @Timestamp long transactTime) {
        this.transactTime = transactTime;
    }

    @Required
    @Timestamp
    public long getTransactTime() {
        return transactTime;
    }

    public boolean hasTransactTime() {
        return transactTime != TypeConstants.TIMESTAMP_NULL;
    }

    @Override
    public void applyAttributes(ObjectList<CustomAttribute> attributes) {
    }

    @Override
    public void reuse() {
        this.orderId = null;
        this.externalOrderId = null;
        this.symbol = null;
        this.transactTime = TypeConstants.TIMESTAMP_NULL;
    }

    @Override
    public void validate() {
        if (!hasSymbol()) {
            throw new InvalidOrderException("Required field 'symbol' is missing.");
        }
        if (!hasOrderId()) {
            throw new InvalidOrderException("Required field 'orderId' is missing.");
        }
        if (!hasTransactTime()) {
            throw new InvalidOrderException("Required field 'transactTime' is missing.");
        }
    }
}
