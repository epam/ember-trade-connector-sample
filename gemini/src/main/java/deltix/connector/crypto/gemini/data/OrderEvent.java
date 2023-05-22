package deltix.connector.crypto.gemini.data;


import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.gflog.api.AppendableEntry;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@SuppressWarnings("unused")
public class OrderEvent extends Message implements BaseOrderStatus {
    @JsonProperty("socket_sequence")
    private long socketSequence;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("api_session")
    private String apiSession;

    @JsonProperty("client_order_id")
    private String clientOrderId;

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("side")
    private OrderSide side;

    @JsonProperty("behavior")
    private String behavior;

    @JsonProperty("order_type")
    private String orderType;

    @JsonProperty("timestampms")
    private long timestamp;

    @JsonProperty("is_live")
    private boolean live;

    @JsonProperty("is_cancelled")
    private boolean cancelled;

    @JsonProperty("is_hidden")
    private boolean hidden;

    @JsonProperty("avg_execution_price")
    @JsonDeserialize(using = DecimalDeserializer.class)
    @Decimal
    private long avgExecutionPrice;

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

    @JsonProperty("original_amount")
    @JsonDeserialize(using = DecimalDeserializer.class)
    @Decimal
    private long originalAmount;

    @JsonProperty("price")
    @JsonDeserialize(using = DecimalDeserializer.class)
    @Decimal
    private long price;

    @JsonProperty("total_spend")
    @JsonDeserialize(using = DecimalDeserializer.class)
    @Decimal
    private long totalSpend;

    public long getSocketSequence() {
        return socketSequence;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getApiSession() {
        return apiSession;
    }

    public String getClientOrderId() {
        return clientOrderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public OrderSide getSide() {
        return side;
    }

    public String getBehavior() {
        return behavior;
    }

    public String getOrderType() {
        return orderType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isLive() {
        return live;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isHidden() {
        return hidden;
    }

    public long getAvgExecutionPrice() {
        return avgExecutionPrice;
    }

    public boolean isWasForced() {
        return wasForced;
    }

    public long getExecutedAmount() {
        return executedAmount;
    }

    public long getRemainingAmount() {
        return remainingAmount;
    }

    public long getOriginalAmount() {
        return originalAmount;
    }

    public long getPrice() {
        return price;
    }

    public long getTotalSpend() {
        return totalSpend;
    }

    @Override
    public void appendToEntry(AppendableEntry entry) {
        entry.append("socketSequence=").append(socketSequence);
        entry.append(", orderId='").append(orderId).append('\'');
        entry.append(", eventId='").append(eventId).append('\'');
        entry.append(", apiSession='").append(apiSession).append('\'');
        entry.append(", clientOrderId='").append(clientOrderId).append('\'');
        entry.append(", symbol='").append(symbol).append('\'');
        entry.append(", side=").append(side);
        entry.append(", behavior='").append(behavior).append('\'');
        entry.append(", orderType='").append(orderType).append('\'');
        entry.append(", timestamp=").append(timestamp);
        entry.append(", live=").append(live);
        entry.append(", cancelled=").append(cancelled);
        entry.append(", hidden=").append(hidden);
        entry.append(", avgExecutionPrice=").appendDecimal64(avgExecutionPrice);
        entry.append(", wasForced=").append(wasForced);
        entry.append(", executedAmount=").appendDecimal64(executedAmount);
        entry.append(", remainingAmount=").appendDecimal64(remainingAmount);
        entry.append(", originalAmount=").appendDecimal64(originalAmount);
        entry.append(", price=").appendDecimal64(price);
        entry.append(", totalSpend=").appendDecimal64(totalSpend);
    }

    // region Concrete Order Events

    public static final class InitialOrderEvent extends OrderEvent {
    }

    public static final class ClosedOrderEvent extends OrderEvent {
    }

    public static final class BookedOrderEvent extends OrderEvent {
    }

    public static final class AcceptedOrderEvent extends OrderEvent {
    }

    // endregion
}
