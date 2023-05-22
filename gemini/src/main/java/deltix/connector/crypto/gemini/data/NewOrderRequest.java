package deltix.connector.crypto.gemini.data;


import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.gflog.api.AppendableEntry;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.EnumSet;

/*
{
    // Request-specific items
    "client_order_id": "20150102-4738721", // A client-specified order token
    "symbol": "btcusd",       // Or any symbol from the /symbols api
    "amount": "34.12",        // Once again, a quoted number
    "price": "622.13",
    "side": "buy",            // must be "buy" or "sell"
    "type": "exchange limit",  // the order type; only "exchange limit" supported
    "options": ["maker-or-cancel"] // execution options; may be omitted for a standard limit order
}
*/

@SuppressWarnings("unused")
public final class NewOrderRequest extends DefaultRequest {

    @JsonProperty("client_order_id")
    private String clientOrderId;

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("amount")
    @JsonInclude
    @JsonSerialize(using = DecimalSerializer.class)
    @Decimal
    private long amount;

    @JsonProperty("price")
    @JsonInclude
    @JsonSerialize(using = DecimalSerializer.class)
    @Decimal
    private long price;

    @JsonProperty("min_amount")
    @JsonInclude
    @JsonSerialize(using = DecimalSerializer.class)
    @Decimal
    private long minAmount;

    @JsonProperty("side")
    private OrderSide side;

    @JsonProperty("type")
    private String type;

    @JsonProperty("options")
    private EnumSet<OrderOptions> options;

    public String getClientOrderId() {
        return clientOrderId;
    }

    public void setClientOrderId(String clientOrderId) {
        this.clientOrderId = clientOrderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public @Decimal long getAmount() {
        return amount;
    }

    public void setAmount(@Decimal long amount) {
        this.amount = amount;
    }

    public @Decimal long getPrice() {
        return price;
    }

    public void setPrice(@Decimal long price) {
        this.price = price;
    }

    public OrderSide getSide() {
        return side;
    }

    public void setSide(OrderSide side) {
        this.side = side;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public EnumSet<OrderOptions> getOptions() {
        return options;
    }

    public void setOptions(EnumSet<OrderOptions> options) {
        this.options = options;
    }

    public long getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(long minAmount) {
        this.minAmount = minAmount;
    }

    @Override
    public void appendToEntry(AppendableEntry entry) {
        super.appendToEntry(entry);
        entry.append(", clientOrderId='").append(clientOrderId).append('\'');
        entry.append(", symbol='").append(symbol).append('\'');
        entry.append(", amount=").appendDecimal64(amount);
        entry.append(", price=").appendDecimal64(price);
        entry.append(", side=").append(side);
        entry.append(", type='").append(type).append('\'');
        entry.append(", options=").append(options != null ? options.toString() : null);
    }
}
