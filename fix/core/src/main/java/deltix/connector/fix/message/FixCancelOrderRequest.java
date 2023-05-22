package deltix.connector.fix.message;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import deltix.anvil.util.TypeConstants;
import deltix.anvil.util.annotation.Optional;
import deltix.anvil.util.annotation.Required;
import deltix.anvil.util.annotation.Timestamp;
import deltix.ember.message.trade.CustomAttribute;
import deltix.ember.service.valid.InvalidOrderException;
import deltix.util.collections.generated.ObjectList;

public class FixCancelOrderRequest implements FixMessage {
    @Required
    protected CharSequence orderId = null;

    @Optional
    protected CharSequence originalOrderId = null;

    @Optional
    protected CharSequence externalOrderId = null;

    @Optional
    protected CharSequence symbol = null;

    @Optional
    protected byte side = TypeConstants.BYTE_NULL;

    @Optional
    @Decimal
    protected long remainingQuantity = TypeConstants.DECIMAL64_NULL;

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

    public void setOriginalOrderId(@Optional CharSequence originalOrderId) {
        this.originalOrderId = originalOrderId;
    }

    @Optional
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

    public void setSymbol(@Optional CharSequence symbol) {
        this.symbol = symbol;
    }

    @Optional
    public CharSequence getSymbol() {
        return symbol;
    }

    public boolean hasSymbol() {
        return symbol != null;
    }

    public void setSide(@Optional byte side) {
        this.side = side;
    }

    @Optional
    public byte getSide() {
        return side;
    }

    public boolean hasSide() {
        return side != TypeConstants.BYTE_NULL;
    }

    public void setRemainingQuantity(@Optional @Decimal long remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
    }

    @Optional
    @Decimal
    public long getRemainingQuantity() {
        return remainingQuantity;
    }

    public boolean hasRemainingQuantity() {
        return !Decimal64Utils.isNaN(remainingQuantity);
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
        this.originalOrderId = null;
        this.externalOrderId = null;
        this.symbol = null;
        this.side = TypeConstants.BYTE_NULL;
        this.remainingQuantity = TypeConstants.DECIMAL64_NULL;
        this.transactTime = TypeConstants.TIMESTAMP_NULL;
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
