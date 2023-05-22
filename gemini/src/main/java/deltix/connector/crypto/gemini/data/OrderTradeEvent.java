package deltix.connector.crypto.gemini.data;

import com.epam.deltix.gflog.api.AppendableEntry;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class OrderTradeEvent extends OrderEvent {
    @JsonProperty("fill")
    @SuppressWarnings("unused")
    private Fill fill;

    public Fill getFill() {
        return fill;
    }

    @Override
    public void appendToEntry(AppendableEntry entry) {
        super.appendToEntry(entry);
        entry.append(", fill=").append(fill);
    }
}
