package deltix.connector.common.smd;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import deltix.ember.message.smd.*;

public abstract class BaseContractProcessor<T extends Contract> implements ContractProcessor<T> {

    protected static final Log LOG = LogFactory.getLog(ContractProcessor.class);

    protected final String symbolKey;

    protected final String priceMultiplierKey;
    protected final String pricePrecisionKey;
    protected final String quantityMultiplierKey;
    protected final String quantityPrecisionKey;

    public BaseContractProcessor(String attributeKey) {
        this(attributeKey + "Symbol",
                attributeKey + "PriceMultiplier", attributeKey + "PricePrecision",
                attributeKey + "QuantityMultiplier", attributeKey + "QuantityPrecision");
    }

    public BaseContractProcessor(String symbolKey,
                                String priceMultiplierKey, String pricePrecisionKey,
                                String quantityMultiplierKey, String quantityPrecisionKey) {
        this.symbolKey = symbolKey;

        this.priceMultiplierKey = priceMultiplierKey;
        this.pricePrecisionKey = pricePrecisionKey;
        this.quantityMultiplierKey = quantityMultiplierKey;
        this.quantityPrecisionKey = quantityPrecisionKey;
    }

    @Override
    public T onBondUpdate(BondUpdate update) {
        return getContract(update);
    }

    @Override
    public T onCurrencyUpdate(CurrencyUpdate update) {
        return getContract(update);
    }

    @Override
    public T onCustomInstrumentUpdate(CustomInstrumentUpdate update) {
        return getContract(update);
    }

    @Override
    public T onEquityUpdate(EquityUpdate update) {
        return getContract(update);
    }

    @Override
    public T onETFUpdate(EtfUpdate update) {
        return getContract(update);
    }

    @Override
    public T onFutureUpdate(FutureUpdate update) {
        return getContract(update);
    }

    @Override
    public T onIndexUpdate(IndexUpdate update) {
        return getContract(update);
    }

    @Override
    public T onOptionUpdate(OptionUpdate update) {
        return getContract(update);
    }

    @Override
    public T onSyntheticUpdate(SyntheticUpdate update) {
        return getContract(update);
    }

    @Override
    public T onCfdUpdate(CfdUpdate update) {
        return getContract(update);
    }

    protected abstract T getContract(InstrumentUpdate update);

    @Decimal
    protected static long transformPrecision(@Decimal long precision) {
        int reversePrecision = Decimal64Utils.toInt(Decimal64Utils.negate(precision));
        return Decimal64Utils.scaleByPowerOfTen(Decimal64Utils.ONE, reversePrecision);
    }

}
