package deltix.connector.crypto.gemini.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum OrderOptions {
    @JsonProperty("maker-or-cancel")
    MAKER_OR_CANCEL,

    @JsonProperty("immediate-or-cancel")
    IMMEDIATE_OR_CANCEL,

    @JsonProperty("fill-or-kill")
    FILL_OR_KILL,

    @JsonProperty("auction-only")
    AUCTION_ONLY,

    @JsonProperty("indication-of-interest")
    INDICATION_OF_INTEREST
}
