package deltix.connector.fix.message;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import deltix.anvil.util.TypeConstants;
import deltix.anvil.util.annotation.Alphanumeric;
import deltix.anvil.util.annotation.Optional;
import deltix.anvil.util.annotation.Timestamp;
import deltix.ember.message.trade.CustomAttribute;
import deltix.util.collections.generated.ObjectList;

public abstract class FixOrderStateMessage implements FixMessage {
    @Optional
    protected CharSequence account = null;

    @Optional
    protected CharSequence orderId = null;

    @Optional
    @Alphanumeric
    protected long currency = TypeConstants.ALPHANUMERIC_NULL;

    @Optional
    protected CharSequence symbol = null;

    @Optional
    protected CharSequence securityType = null;

    @Optional
    protected CharSequence quoteId = null;

    @Optional
    @Alphanumeric
    protected long exchangeId = TypeConstants.ALPHANUMERIC_NULL;

    @Optional
    protected byte side = TypeConstants.BYTE_NULL;

    @Optional
    @Timestamp
    protected long transactTime = TypeConstants.TIMESTAMP_NULL;

    @Optional
    @Decimal
    protected long quantity = TypeConstants.DECIMAL64_NULL;

    @Optional
    @Decimal
    protected long minQuantity = TypeConstants.DECIMAL64_NULL;

    @Optional
    @Decimal
    protected long displayQuantity = TypeConstants.DECIMAL64_NULL;

    @Optional
    protected byte orderType = TypeConstants.BYTE_NULL;

    @Optional
    @Decimal
    protected long limitPrice = TypeConstants.DECIMAL64_NULL;

    @Optional
    @Decimal
    protected long stopPrice = TypeConstants.DECIMAL64_NULL;

    @Optional
    protected byte timeInForce = TypeConstants.BYTE_NULL;

    @Optional
    @Timestamp
    protected long expireTime = TypeConstants.TIMESTAMP_NULL;

    @Optional
    @Timestamp
    protected long expireDate = TypeConstants.TIMESTAMP_NULL;

    public void setAccount(@Optional CharSequence account) {
        this.account = account;
    }

    @Optional
    public CharSequence getAccount() {
        return account;
    }

    public boolean hasAccount() {
        return account != null;
    }

    public void setOrderId(@Optional CharSequence orderId) {
        this.orderId = orderId;
    }

    @Optional
    public CharSequence getOrderId() {
        return orderId;
    }

    public boolean hasOrderId() {
        return orderId != null;
    }

    public void setCurrency(@Optional @Alphanumeric long currency) {
        this.currency = currency;
    }

    @Optional
    @Alphanumeric
    public long getCurrency() {
        return currency;
    }

    public boolean hasCurrency() {
        return currency != TypeConstants.ALPHANUMERIC_NULL;
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

    public void setSecurityType(@Optional CharSequence securityType) {
        this.securityType = securityType;
    }

    @Optional
    public CharSequence getSecurityType() {
        return securityType;
    }

    public boolean hasSecurityType() {
        return securityType != null;
    }

    public void setQuoteId(@Optional CharSequence quoteId) {
        this.quoteId = quoteId;
    }

    @Optional
    public CharSequence getQuoteId() {
        return quoteId;
    }

    public boolean hasQuoteId() {
        return quoteId != null;
    }

    public void setExchangeId(@Optional @Alphanumeric long exchangeId) {
        this.exchangeId = exchangeId;
    }

    @Optional
    @Alphanumeric
    public long getExchangeId() {
        return exchangeId;
    }

    public boolean hasExchangeId() {
        return exchangeId != TypeConstants.ALPHANUMERIC_NULL;
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

    public void setQuantity(@Optional @Decimal long quantity) {
        this.quantity = quantity;
    }

    @Optional
    @Decimal
    public long getQuantity() {
        return quantity;
    }

    public boolean hasQuantity() {
        return !Decimal64Utils.isNaN(quantity);
    }

    public void setMinQuantity(@Optional @Decimal long minQuantity) {
        this.minQuantity = minQuantity;
    }

    @Optional
    @Decimal
    public long getMinQuantity() {
        return minQuantity;
    }

    public boolean hasMinQuantity() {
        return !Decimal64Utils.isNaN(minQuantity);
    }

    public void setDisplayQuantity(@Optional @Decimal long displayQuantity) {
        this.displayQuantity = displayQuantity;
    }

    @Optional
    @Decimal
    public long getDisplayQuantity() {
        return displayQuantity;
    }

    public boolean hasDisplayQuantity() {
        return !Decimal64Utils.isNaN(displayQuantity);
    }

    public void setOrderType(@Optional byte orderType) {
        this.orderType = orderType;
    }

    @Optional
    public byte getOrderType() {
        return orderType;
    }

    public boolean hasOrderType() {
        return orderType != TypeConstants.BYTE_NULL;
    }

    public void setLimitPrice(@Optional @Decimal long limitPrice) {
        this.limitPrice = limitPrice;
    }

    @Optional
    @Decimal
    public long getLimitPrice() {
        return limitPrice;
    }

    public boolean hasLimitPrice() {
        return !Decimal64Utils.isNaN(limitPrice);
    }

    public void setStopPrice(@Optional @Decimal long stopPrice) {
        this.stopPrice = stopPrice;
    }

    @Optional
    @Decimal
    public long getStopPrice() {
        return stopPrice;
    }

    public boolean hasStopPrice() {
        return !Decimal64Utils.isNaN(stopPrice);
    }

    public void setTimeInForce(@Optional byte timeInForce) {
        this.timeInForce = timeInForce;
    }

    @Optional
    public byte getTimeInForce() {
        return timeInForce;
    }

    public boolean hasTimeInForce() {
        return timeInForce != TypeConstants.BYTE_NULL;
    }

    public void setExpireTime(@Optional @Timestamp long expireTime) {
        this.expireTime = expireTime;
    }

    @Optional
    @Timestamp
    public long getExpireTime() {
        return expireTime;
    }

    public boolean hasExpireTime() {
        return expireTime != TypeConstants.TIMESTAMP_NULL;
    }

    public void setExpireDate(@Optional @Timestamp long expireDate) {
        this.expireDate = expireDate;
    }

    @Optional
    @Timestamp
    public long getExpireDate() {
        return expireDate;
    }

    public boolean hasExpireDate() {
        return expireDate != TypeConstants.TIMESTAMP_NULL;
    }

    @Override
    public void applyAttributes(ObjectList<CustomAttribute> attributes) {
    }

    @Override
    public void reuse() {
        this.account = null;
        this.orderId = null;
        this.currency = TypeConstants.ALPHANUMERIC_NULL;
        this.symbol = null;
        this.securityType = null;
        this.quoteId = null;
        this.exchangeId = TypeConstants.ALPHANUMERIC_NULL;
        this.side = TypeConstants.BYTE_NULL;
        this.transactTime = TypeConstants.TIMESTAMP_NULL;
        this.quantity = TypeConstants.DECIMAL64_NULL;
        this.minQuantity = TypeConstants.DECIMAL64_NULL;
        this.displayQuantity = TypeConstants.DECIMAL64_NULL;
        this.orderType = TypeConstants.BYTE_NULL;
        this.limitPrice = TypeConstants.DECIMAL64_NULL;
        this.stopPrice = TypeConstants.DECIMAL64_NULL;
        this.timeInForce = TypeConstants.BYTE_NULL;
        this.expireTime = TypeConstants.TIMESTAMP_NULL;
        this.expireDate = TypeConstants.TIMESTAMP_NULL;
    }

    @Override
    public void validate() {
    }
}
