package deltix.connector.common.smd;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import deltix.anvil.util.annotation.Alphanumeric;
import deltix.ember.message.smd.InstrumentType;

public class Contract {

    protected final String symbol;
    protected final String brokerSymbol;
    protected final InstrumentType securityType;

    protected final long priceMultiplier;
    protected final long pricePrecision;

    protected final long quantityMultiplier;
    protected final long quantityPrecision;

    protected final @Alphanumeric long currency;

    public Contract(String symbol, String brokerSymbol, InstrumentType securityType,
                    @Decimal long priceMultiplier, @Decimal long pricePrecision,
                    @Decimal long quantityMultiplier, @Decimal long quantityPrecision,
                    @Alphanumeric long currency) {
        this.symbol = symbol;
        this.brokerSymbol = brokerSymbol;
        this.securityType = securityType;

        this.priceMultiplier = priceMultiplier;
        this.pricePrecision = pricePrecision;

        this.quantityMultiplier = quantityMultiplier;
        this.quantityPrecision = quantityPrecision;

        this.currency = currency;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getBrokerSymbol() {
        return brokerSymbol;
    }

    public InstrumentType getSecurityType() {
        return securityType;
    }

    public boolean hasPriceMultiplier() {
        return !Decimal64Utils.isNaN(priceMultiplier);
    }

    @Decimal
    public long getPriceMultiplier() {
        return priceMultiplier;
    }

    public boolean hasPricePrecision() {
        return !Decimal64Utils.isNaN(pricePrecision);
    }

    @Decimal
    public long getPricePrecision() {
        return pricePrecision;
    }

    public boolean hasQuantityMultiplier() {
        return !Decimal64Utils.isNaN(quantityMultiplier);
    }

    @Decimal
    public long getQuantityMultiplier() {
        return quantityMultiplier;
    }

    public boolean hasQuantityPrecision() {
        return !Decimal64Utils.isNaN(quantityPrecision);
    }

    @Decimal
    public long getQuantityPrecision() {
        return quantityPrecision;
    }

    @Alphanumeric
    public long getCurrency() {
        return currency;
    }

}
