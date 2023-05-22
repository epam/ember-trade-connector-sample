package deltix.connector.crypto.gemini;

import deltix.anvil.util.annotation.Duration;
import deltix.anvil.util.annotation.Hashed;
import deltix.anvil.util.annotation.Optional;
import deltix.anvil.util.annotation.Required;
import deltix.connector.common.core.BaseTradeConnectorSettings;

@SuppressWarnings("unused")
public class GeminiConnectorSettings extends BaseTradeConnectorSettings {
    protected @Required String restUrl;
    protected @Optional String websocketUrl;

    protected @Required String apiKey;
    protected @Required @Hashed String apiSecret;

    protected @Optional @Duration long reconnectInterval = 5_000L; // 5 sec

    public GeminiConnectorSettings() {
        setAttributeKey("gemini");
    }

    public String getRestUrl() {
        return restUrl;
    }

    public void setRestUrl(String restUrl) {
        this.restUrl = restUrl;
    }

    public String getWebsocketUrl() {
        return websocketUrl;
    }

    public void setWebsocketUrl(String websocketUrl) {
        this.websocketUrl = websocketUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public long getReconnectInterval() {
        return reconnectInterval;
    }

    public void setReconnectInterval(long reconnectInterval) {
        this.reconnectInterval = reconnectInterval;
    }
}
