package deltix.connector.crypto.gemini.data;

import com.epam.deltix.gflog.api.AppendableEntry;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("unused")
public final class OrderCancelledEvent extends OrderEvent {
    @JsonProperty("reason")
    private String reason;

    @JsonProperty("cancel_command_id")
    private String cancelCommandId;

    public String getReason() {
        return reason;
    }

    public String getCancelCommandId() {
        return cancelCommandId;
    }

    @Override
    public void appendToEntry(AppendableEntry entry) {
        super.appendToEntry(entry);
        entry.append(", reason='").append(reason).append('\'');
        entry.append(", cancelCommandId='").append(cancelCommandId).append('\'');
    }
}
