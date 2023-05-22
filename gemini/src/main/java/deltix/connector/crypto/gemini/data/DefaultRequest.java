package deltix.connector.crypto.gemini.data;

import com.epam.deltix.gflog.api.AppendableEntry;
import com.epam.deltix.gflog.api.Loggable;
import com.fasterxml.jackson.annotation.JsonProperty;
import deltix.anvil.util.AppendableEntryBuilder;

/*
{
    // Standard headers`
    "request": "/v1/order/new",
    "nonce": <nonce>,
}
*/
public class DefaultRequest implements Loggable {
    @JsonProperty("request")
    private String request;

    @JsonProperty("nonce")
    private long nonce;

    public DefaultRequest() {
    }

    public DefaultRequest(String request) {
        this.request = request;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public long getNonce() {
        return nonce;
    }

    public void setNonce(long nonce) {
        this.nonce = nonce;
    }

    @Override
    public void appendTo(AppendableEntry entry) {
        entry.append(getClass().getSimpleName()).append('{');
        entry.append("request=").append(request);
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
}
