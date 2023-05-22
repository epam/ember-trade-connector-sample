package deltix.connector.fix.message;

import deltix.anvil.util.TypeConstants;
import deltix.anvil.util.annotation.Optional;
import deltix.anvil.util.annotation.Required;
import deltix.anvil.util.annotation.Timestamp;
import deltix.ember.message.trade.CustomAttribute;
import deltix.ember.service.valid.InvalidOrderException;
import deltix.util.collections.generated.ObjectList;

public class FixCancelReject implements FixMessage {
    @Required
    protected CharSequence orderId = null;

    @Required
    protected CharSequence originalOrderId = null;

    @Optional
    protected CharSequence externalOrderId = null;

    @Optional
    protected CharSequence text = null;

    @Optional
    protected byte orderStatus = TypeConstants.BYTE_NULL;

    @Required
    protected byte rejectType = TypeConstants.BYTE_NULL;

    @Optional
    protected int rejectCode = TypeConstants.INT_NULL;

    @Optional
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

    public void setOrderStatus(@Optional byte orderStatus) {
        this.orderStatus = orderStatus;
    }

    @Optional
    public byte getOrderStatus() {
        return orderStatus;
    }

    public boolean hasOrderStatus() {
        return orderStatus != TypeConstants.BYTE_NULL;
    }

    public void setRejectType(@Required byte rejectType) {
        this.rejectType = rejectType;
    }

    @Required
    public byte getRejectType() {
        return rejectType;
    }

    public boolean hasRejectType() {
        return rejectType != TypeConstants.BYTE_NULL;
    }

    public void setRejectCode(@Optional int rejectCode) {
        this.rejectCode = rejectCode;
    }

    @Optional
    public int getRejectCode() {
        return rejectCode;
    }

    public boolean hasRejectCode() {
        return rejectCode != TypeConstants.INT_NULL;
    }

    public void setTransactTime(@Optional @Timestamp long transactTime) {
        this.transactTime = transactTime;
    }

    @Optional
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
        this.text = null;
        this.orderStatus = TypeConstants.BYTE_NULL;
        this.rejectType = TypeConstants.BYTE_NULL;
        this.rejectCode = TypeConstants.INT_NULL;
        this.transactTime = TypeConstants.TIMESTAMP_NULL;
    }

    @Override
    public void validate() {
        if (!hasOriginalOrderId()) {
            throw new InvalidOrderException("Required field 'originalOrderId' is missing.");
        }
        if (!hasOrderId()) {
            throw new InvalidOrderException("Required field 'orderId' is missing.");
        }
        if (!hasRejectType()) {
            throw new InvalidOrderException("Required field 'rejectType' is missing.");
        }
    }
}
