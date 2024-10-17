package deltix.connector.common.smd;

import com.epam.deltix.dfp.Decimal64Utils;
import deltix.anvil.util.CharSequenceUtil;
import deltix.anvil.util.annotation.Alphanumeric;
import deltix.ember.message.smd.InstrumentAttribute;
import deltix.ember.message.smd.InstrumentUpdate;
import deltix.ember.message.smd.SyntheticLeg;
import deltix.ember.message.smd.SyntheticUpdate;
import deltix.util.collections.CollectionUtil;
import deltix.util.collections.generated.ObjectList;


public class CommonContractProcessor extends BaseContractProcessor<Contract> {

    public CommonContractProcessor(String attributeKey) {
        super(attributeKey);
    }

    @Override
    protected Contract getContract(InstrumentUpdate update) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("[ContractProcessor] %s").with(update);
        }

        final String symbol = update.getSymbol().toString();
        final @Alphanumeric long currency = update.getCurrency();

        String brokerSymbol = symbol;

        long priceMultiplier = Decimal64Utils.NaN;
        long pricePrecision = Decimal64Utils.NaN;
        long quantityMultiplier = Decimal64Utils.NaN;
        long quantityPrecision = Decimal64Utils.NaN;

        final ObjectList<InstrumentAttribute> attributes = update.getAttributes();

        if (attributes != null && !attributes.isEmpty()) {
            for (int i = 0; i < attributes.size(); i++) {
                InstrumentAttribute attribute = attributes.get(i);

                if (!attribute.hasKey() || !attribute.hasValue())
                    continue;

                CharSequence key = attribute.getKey();
                CharSequence value = attribute.getValue();

                if (CharSequenceUtil.equals(symbolKey, key)) {
                    brokerSymbol = value.toString();
                } else if (CharSequenceUtil.equals(priceMultiplierKey, key)) {
                    priceMultiplier = Decimal64Utils.tryParse(value, Decimal64Utils.NULL);
                    if (priceMultiplier == Decimal64Utils.NULL) {
                        LOG.error("[Contract Processor] Skip instrument update: (%s) - invalid Price Multiplier: %s").with(symbol).with(value);
                        return null;
                    }
                } else if (CharSequenceUtil.equals(pricePrecisionKey, key)) {
                    pricePrecision = Decimal64Utils.tryParse(value, Decimal64Utils.NULL);
                    if (pricePrecision == Decimal64Utils.NULL) {
                        LOG.error("[Contract Processor] Skip instrument update: (%s) - invalid Price Precision: %s").with(symbol).with(value);
                        return null;
                    }
                    pricePrecision = transformPrecision(pricePrecision);
                } else if (CharSequenceUtil.equals(quantityMultiplierKey, key)) {
                    quantityMultiplier = Decimal64Utils.tryParse(value, Decimal64Utils.NULL);
                    if (quantityMultiplier == Decimal64Utils.NULL) {
                        LOG.error("[Contract Processor] Skip instrument update: (%s) - invalid Quantity Multiplier: %s").with(symbol).with(value);
                        return null;
                    }
                } else if (CharSequenceUtil.equals(quantityPrecisionKey, key)) {
                    quantityPrecision = Decimal64Utils.tryParse(value, Decimal64Utils.NULL);
                    if (quantityPrecision == Decimal64Utils.NULL) {
                        LOG.error("[Contract Processor] Skip instrument update: (%s) - invalid Quantity Precision: %s").with(symbol).with(value);
                        return null;
                    }
                    quantityPrecision = transformPrecision(quantityPrecision);
                }
            }
        }

        ObjectList<SyntheticLeg> legs = null;
        if (update instanceof SyntheticUpdate) {
            legs = CollectionUtil.copy(((SyntheticUpdate) update).getLegs(), SyntheticLeg::copy);
        }

        return new Contract(symbol, brokerSymbol, update.getInstrumentType(),
                            priceMultiplier, pricePrecision, quantityMultiplier, quantityPrecision,
                            currency, legs);
    }

}
