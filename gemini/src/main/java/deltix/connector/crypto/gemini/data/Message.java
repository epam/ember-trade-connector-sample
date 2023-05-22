package deltix.connector.crypto.gemini.data;


import com.epam.deltix.gflog.api.AppendableEntry;
import com.epam.deltix.gflog.api.Loggable;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import deltix.anvil.util.AppendableEntryBuilder;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Message.SubscriptionAckEvent.class, name = "subscription_ack"),
        @JsonSubTypes.Type(value = Message.HeartbeatEvent.class, name = "heartbeat"),
        @JsonSubTypes.Type(value = OrderEvent.InitialOrderEvent.class, name = "initial"),
        @JsonSubTypes.Type(value = OrderEvent.AcceptedOrderEvent.class, name = "accepted"),
        @JsonSubTypes.Type(value = OrderEvent.BookedOrderEvent.class, name = "booked"),
        @JsonSubTypes.Type(value = OrderEvent.ClosedOrderEvent.class, name = "closed"),
        @JsonSubTypes.Type(value = OrderTradeEvent.class, name = "fill"),
        @JsonSubTypes.Type(value = OrderCancelRejectedEvent.class, name = "cancel_rejected"),
        @JsonSubTypes.Type(value = OrderRejectedEvent.class, name = "rejected"),
        @JsonSubTypes.Type(value = OrderCancelledEvent.class, name = "cancelled")
})
public abstract class Message implements Loggable {

    @Override
    public void appendTo(AppendableEntry entry) {
        entry.append(getClass().getSimpleName()).append('{');
        appendToEntry(entry);
        entry.append('}');
    }

    protected void appendToEntry(AppendableEntry entry) {
    }

    @Override
    public String toString() {
        AppendableEntryBuilder entry = new AppendableEntryBuilder(256);
        appendTo(entry);
        return entry.toString();
    }

    public static final class SubscriptionAckEvent extends Message {
    }

    public static final class HeartbeatEvent extends Message {
    }
}
