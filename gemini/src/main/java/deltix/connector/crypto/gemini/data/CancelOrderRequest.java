package deltix.connector.crypto.gemini.data;

import com.epam.deltix.gflog.api.AppendableEntry;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class CancelOrderRequest extends DefaultRequest {
    @JsonProperty("order_id")
    private String externalOrderId;

    @SuppressWarnings("unused")
    public String getExternalOrderId() {
        return externalOrderId;
    }

    public void setExternalOrderId(String externalOrderId) {
        this.externalOrderId = externalOrderId;
    }

    @Override
    public void appendToEntry(AppendableEntry entry) {
        super.appendToEntry(entry);
        entry.append(", externalOrderId='").append(externalOrderId).append('\'');
    }
}
