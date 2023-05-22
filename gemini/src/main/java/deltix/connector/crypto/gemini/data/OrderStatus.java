package deltix.connector.crypto.gemini.data;


import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.gflog.api.AppendableEntry;
import com.epam.deltix.gflog.api.Loggable;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import deltix.anvil.util.AppendableEntryBuilder;

import java.util.EnumSet;

/*
{
  "order_id" : "44375938",
  "id" : "44375938",
  "symbol" : "ethusd",
  "exchange" : "gemini",
  "avg_execution_price" : "0.00",
  "side" : "buy",
  "type" : "exchange limit",
  "timestamp" : "1494871426",
  "timestampms" : 1494871426935,
  "is_live" : true,
  "is_cancelled" : false,
  "is_hidden" : false,
  "was_forced" : false,
  "executed_amount" : "0",
  "remaining_amount" : "500",
  "options" : [ "maker-or-cancel" ],
  "price" : "30.50",
  "original_amount" : "500"
}
*/

@SuppressWarnings("unused")
@JsonIgnoreProperties(ignoreUnknown=true)
public final class OrderStatus implements Loggable, BaseOrderStatus {
    @JsonProperty("id")
    private String id;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("client_order_id")
    private String clientOrderId;

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("exchange")
    private String exchange;

    @JsonProperty("avg_execution_price")
    @JsonDeserialize(using = DecimalDeserializer.class)
    @Decimal
    private long avgExecutionPrice;

    @JsonProperty("side")
    private OrderSide side;

    @JsonProperty("type")
    private String type;

    @JsonProperty("timestampms")
    private long timestamp;

    @JsonProperty("is_live")
    private boolean live;

    @JsonProperty("is_cancelled")
    private boolean cancelled;

    @JsonProperty("is_hidden")
    private boolean hidden;

    @JsonProperty("was_forced")
    private boolean wasForced;

    @JsonProperty("executed_amount")
    @JsonDeserialize(using = DecimalDeserializer.class)
    @Decimal
    private long executedAmount;

    @JsonProperty("remaining_amount")
    @JsonDeserialize(using = DecimalDeserializer.class)
    @Decimal
    private long remainingAmount;

    @JsonProperty("options")
    private EnumSet<OrderOptions> options;

    @JsonProperty("original_amount")
    @JsonDeserialize(using = DecimalDeserializer.class)
    @Decimal
    private long originalAmount;

    @JsonProperty("price")
    @JsonDeserialize(using = DecimalDeserializer.class)
    @Decimal
    private long price;

    @Override
    public String getEventId() {
        return id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String getOrderId() {
        return orderId;
    }

    @Override
    public String getClientOrderId() {
        return clientOrderId;
    }

    @Override
    public String getSymbol() {
        return symbol;
    }

    public String getExchange() {
        return exchange;
    }

    @Override
    public long getAvgExecutionPrice() {
        return avgExecutionPrice;
    }

    @Override
    public OrderSide getSide() {
        return side;
    }

    public String getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean isLive() {
        return live;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isHidden() {
        return hidden;
    }

    public boolean isWasForced() {
        return wasForced;
    }

    @Override
    public long getExecutedAmount() {
        return executedAmount;
    }

    @Override
    public long getRemainingAmount() {
        return remainingAmount;
    }

    public EnumSet<OrderOptions> getOptions() {
        return options;
    }

    @Override
    public long getOriginalAmount() {
        return originalAmount;
    }

    public double getPrice() {
        return price;
    }

    @Override
    public void appendTo(AppendableEntry entry) {
        entry.append("OrderStatus{");
        entry.append("id='").append(id).append('\'');
        entry.append(", orderId='").append(orderId).append('\'');
        entry.append(", clientOrderId=").append(clientOrderId);
        entry.append(", symbol='").append(symbol).append('\'');
        entry.append(", exchange='").append(exchange).append('\'');
        entry.append(", avgExecutionPrice=").appendDecimal64(avgExecutionPrice);
        entry.append(", side='").append(side).append('\'');
        entry.append(", type='").append(type).append('\'');
        entry.append(", timestamp=").append(timestamp);
        entry.append(", live=").append(live);
        entry.append(", cancelled=").append(cancelled);
        entry.append(", hidden=").append(hidden);
        entry.append(", wasForced=").append(wasForced);
        entry.append(", executedAmount=").appendDecimal64(executedAmount);
        entry.append(", remainingAmount=").appendDecimal64(remainingAmount);
        entry.append(", options=").append(options != null ? options.toString() : null);
        entry.append(", originalAmount=").appendDecimal64(originalAmount);
        entry.append(", price=").append(price);
        entry.append('}');
    }

    @Override
    public String toString() {
        AppendableEntryBuilder entry = new AppendableEntryBuilder(256);
        appendTo(entry);
        return entry.toString();
    }
}
