package deltix.connector.crypto.gemini.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum OrderSide {
    @JsonProperty("buy")
    BUY,

    @JsonProperty("sell")
    SELL
}
