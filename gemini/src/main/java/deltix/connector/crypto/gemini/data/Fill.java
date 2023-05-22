package deltix.connector.crypto.gemini.data;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.gflog.api.AppendableEntry;
import com.epam.deltix.gflog.api.Loggable;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import deltix.anvil.util.AppendableEntryBuilder;

public final class Fill implements Loggable {
    @JsonProperty("trade_id")
    private String tradeId;

    @JsonProperty("liquidity")
    private String liquidity;

    @JsonProperty("price")
    @JsonDeserialize(using = DecimalDeserializer.class)
    @Decimal
    private long price;

    @JsonProperty("amount")
    @JsonDeserialize(using = DecimalDeserializer.class)
    @Decimal
    private long amount;

    @JsonProperty("fee")
    @JsonDeserialize(using = DecimalDeserializer.class)
    @Decimal
    private long fee;

    @JsonProperty("fee_currency")
    private String feeCurrency;

    public String getTradeId() {
        return tradeId;
    }

    public String getLiquidity() {
        return liquidity;
    }

    public long getPrice() {
        return price;
    }

    public long getAmount() {
        return amount;
    }

    public long getFee() {
        return fee;
    }

    public String getFeeCurrency() {
        return feeCurrency;
    }

    @Override
    public void appendTo(AppendableEntry entry) {
        entry.append("Fill{");
        entry.append("tradeId='").append(tradeId).append('\'');
        entry.append(", liquidity='").append(liquidity).append('\'');
        entry.append(", price=").append(Decimal64Utils.toString(price));
        entry.append(", amount=").append(Decimal64Utils.toString(amount));
        entry.append(", fee=").append(Decimal64Utils.toString(fee));
        entry.append(", feeCurrency='").append(feeCurrency).append('\'');
        entry.append('}');
    }

    @Override
    public String toString() {
        AppendableEntryBuilder entry = new AppendableEntryBuilder(256);
        appendTo(entry);
        return entry.toString();
    }
}
