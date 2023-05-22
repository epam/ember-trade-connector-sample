package deltix.connector.common.smd;

import deltix.ember.message.smd.*;


public interface ContractProcessor<T extends Contract> {

    T onBondUpdate(BondUpdate update);

    T onCurrencyUpdate(CurrencyUpdate update);

    T onCustomInstrumentUpdate(CustomInstrumentUpdate update);

    T onEquityUpdate(EquityUpdate update);

    T onETFUpdate(EtfUpdate update);

    T onFutureUpdate(FutureUpdate update);

    T onIndexUpdate(IndexUpdate update);

    T onOptionUpdate(OptionUpdate update);

    T onSyntheticUpdate(SyntheticUpdate update);

    T onCfdUpdate(CfdUpdate update);

}
