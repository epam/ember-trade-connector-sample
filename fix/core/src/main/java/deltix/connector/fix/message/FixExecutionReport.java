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

@SuppressWarnings("unused")
public class FixExecutionReport extends FixOrderStateMessage {
    @Optional
    protected CharSequence originalOrderId = null;

    @Optional
    protected CharSequence externalOrderId = null;

    @Required
    protected CharSequence executionId = null;

    @Optional
    protected CharSequence executionReferenceId = null;

    @Optional
    protected CharSequence text = null;

    @Required
    protected byte executionType = TypeConstants.BYTE_NULL;

    @Optional
    protected byte orderStatus = TypeConstants.BYTE_NULL;

    @Optional
    protected byte multiLegReportingType = TypeConstants.BYTE_NULL;

    @Optional
    @Decimal
    protected long remainingQuantity = TypeConstants.DECIMAL64_NULL;

    @Optional
    @Decimal
    protected long cumulativeQuantity = TypeConstants.DECIMAL64_NULL;

    @Optional
    @Decimal
    protected long executionQuantity = TypeConstants.DECIMAL64_NULL;

    @Optional
    @Decimal
    protected long averagePrice = TypeConstants.DECIMAL64_NULL;

    @Optional
    @Decimal
    protected long executionPrice = TypeConstants.DECIMAL64_NULL;

    @Optional
    @Timestamp
    protected long tradeDate = TypeConstants.TIMESTAMP_NULL;

    @Optional
    @Timestamp
    protected long settlementDate = TypeConstants.TIMESTAMP_NULL;

    @Optional
    protected int rejectCode = TypeConstants.INT_NULL;

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

    public void setExecutionId(@Required CharSequence executionId) {
        this.executionId = executionId;
    }

    @Required
    public CharSequence getExecutionId() {
        return executionId;
    }

    public boolean hasExecutionId() {
        return executionId != null;
    }

    public void setExecutionReferenceId(@Optional CharSequence executionReferenceId) {
        this.executionReferenceId = executionReferenceId;
    }

    @Optional
    public CharSequence getExecutionReferenceId() {
        return executionReferenceId;
    }

    public boolean hasExecutionReferenceId() {
        return executionReferenceId != null;
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

    public void setExecutionType(@Required byte executionType) {
        this.executionType = executionType;
    }

    @Required
    public byte getExecutionType() {
        return executionType;
    }

    public boolean hasExecutionType() {
        return executionType != TypeConstants.BYTE_NULL;
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

    public void setMultiLegReportingType(@Optional byte multiLegReportingType) {
        this.multiLegReportingType = multiLegReportingType;
    }

    @Optional
    public byte getMultiLegReportingType() {
        return multiLegReportingType;
    }

    public boolean hasMultiLegReportingType() {
        return multiLegReportingType != TypeConstants.BYTE_NULL;
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

    public void setCumulativeQuantity(@Optional @Decimal long cumulativeQuantity) {
        this.cumulativeQuantity = cumulativeQuantity;
    }

    @Optional
    @Decimal
    public long getCumulativeQuantity() {
        return cumulativeQuantity;
    }

    public boolean hasCumulativeQuantity() {
        return !Decimal64Utils.isNaN(cumulativeQuantity);
    }

    public void setExecutionQuantity(@Optional @Decimal long executionQuantity) {
        this.executionQuantity = executionQuantity;
    }

    @Optional
    @Decimal
    public long getExecutionQuantity() {
        return executionQuantity;
    }

    public boolean hasExecutionQuantity() {
        return !Decimal64Utils.isNaN(executionQuantity);
    }

    public void setAveragePrice(@Optional @Decimal long averagePrice) {
        this.averagePrice = averagePrice;
    }

    @Optional
    @Decimal
    public long getAveragePrice() {
        return averagePrice;
    }

    public boolean hasAveragePrice() {
        return !Decimal64Utils.isNaN(averagePrice);
    }

    public void setExecutionPrice(@Optional @Decimal long executionPrice) {
        this.executionPrice = executionPrice;
    }

    @Optional
    @Decimal
    public long getExecutionPrice() {
        return executionPrice;
    }

    public boolean hasExecutionPrice() {
        return !Decimal64Utils.isNaN(executionPrice);
    }

    public void setTradeDate(@Optional @Timestamp long tradeDate) {
        this.tradeDate = tradeDate;
    }

    @Optional
    @Timestamp
    public long getTradeDate() {
        return tradeDate;
    }

    public boolean hasTradeDate() {
        return tradeDate != TypeConstants.TIMESTAMP_NULL;
    }

    public void setSettlementDate(@Optional @Timestamp long settlementDate) {
        this.settlementDate = settlementDate;
    }

    @Optional
    @Timestamp
    public long getSettlementDate() {
        return settlementDate;
    }

    public boolean hasSettlementDate() {
        return settlementDate != TypeConstants.TIMESTAMP_NULL;
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

    @Override
    public void applyAttributes(ObjectList<CustomAttribute> attributes) {
    }

    @Override
    public void reuse() {
        super.reuse();

        this.originalOrderId = null;
        this.externalOrderId = null;
        this.executionId = null;
        this.executionReferenceId = null;
        this.text = null;
        this.executionType = TypeConstants.BYTE_NULL;
        this.orderStatus = TypeConstants.BYTE_NULL;
        this.multiLegReportingType = TypeConstants.BYTE_NULL;
        this.remainingQuantity = TypeConstants.DECIMAL64_NULL;
        this.cumulativeQuantity = TypeConstants.DECIMAL64_NULL;
        this.executionQuantity = TypeConstants.DECIMAL64_NULL;
        this.averagePrice = TypeConstants.DECIMAL64_NULL;
        this.executionPrice = TypeConstants.DECIMAL64_NULL;
        this.tradeDate = TypeConstants.TIMESTAMP_NULL;
        this.settlementDate = TypeConstants.TIMESTAMP_NULL;
        this.rejectCode = TypeConstants.INT_NULL;
    }

    @Override
    public void validate() {
        if (!hasExecutionType()) {
            throw new InvalidOrderException("Required field 'executionType' is missing.");
        }
        if (!hasExecutionId()) {
            throw new InvalidOrderException("Required field 'executionId' is missing.");
        }
    }
}
