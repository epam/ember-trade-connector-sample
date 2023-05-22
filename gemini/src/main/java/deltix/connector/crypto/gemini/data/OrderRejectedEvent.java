package deltix.connector.crypto.gemini.data;

import com.epam.deltix.gflog.api.AppendableEntry;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("unused")
public final class OrderRejectedEvent extends OrderEvent {
    @JsonProperty("reason")
    private String reason;

    public String getReason() {
        return reason;
    }

    @Override
    public void appendToEntry(AppendableEntry entry) {
        super.appendToEntry(entry);
        entry.append(", reason='").append(reason).append('\'');
    }
}
