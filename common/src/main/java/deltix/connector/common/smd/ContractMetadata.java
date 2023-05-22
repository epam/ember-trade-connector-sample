package deltix.connector.common.smd;

import deltix.ember.message.smd.*;
import deltix.ember.service.InstrumentUpdateHandler;
import deltix.util.collections.CharSequenceToObjectMapQuick;

import java.util.function.Function;


public class ContractMetadata<T extends Contract> implements InstrumentUpdateHandler {

    protected final CharSequenceToObjectMapQuick<T> mapBySymbol;
    protected final CharSequenceToObjectMapQuick<T> mapByBrokerSymbol;

    protected final ContractProcessor<T> processor;
    protected final Function<T, String> symbolGetter;
    protected final Function<T, String> brokerSymbolGetter;

    public ContractMetadata(final ContractProcessor<T> processor) {
        this(processor, Contract::getSymbol, Contract::getBrokerSymbol, 512);
    }

    public ContractMetadata(final ContractProcessor<T> processor,
                            final Function<T, String> symbolGetter,
                            final Function<T, String> brokerSymbolGetter) {
       this(processor, symbolGetter, brokerSymbolGetter, 512);
    }

    public ContractMetadata(final ContractProcessor<T> processor,
                            final Function<T, String> symbolGetter,
                            final Function<T, String> brokerSymbolGetter,
                            final int initSize) {
        this.mapBySymbol = new CharSequenceToObjectMapQuick<>(initSize);
        this.mapByBrokerSymbol = new CharSequenceToObjectMapQuick<>(initSize);
        this.processor = processor;
        this.symbolGetter = symbolGetter;
        this.brokerSymbolGetter = brokerSymbolGetter;
    }

    public T getContractBySymbol(CharSequence symbol) {
        return mapBySymbol.get(symbol, null);
    }

    public T getContractByBrokerSymbol(CharSequence symbol) {
        return mapByBrokerSymbol.get(symbol, null);
    }

    @Override
    public void onBondUpdate(BondUpdate update) {
        T contract = processor.onBondUpdate(update);
        updateContract(contract);
    }

    @Override
    public void onCurrencyUpdate(CurrencyUpdate update) {
        T contract = processor.onCurrencyUpdate(update);
        updateContract(contract);
    }

    @Override
    public void onCustomInstrumentUpdate(CustomInstrumentUpdate update) {
        T contract = processor.onCustomInstrumentUpdate(update);
        updateContract(contract);
    }

    @Override
    public void onEquityUpdate(EquityUpdate update) {
        T contract = processor.onEquityUpdate(update);
        updateContract(contract);
    }

    @Override
    public void onEtfUpdate(EtfUpdate update) {
        T contract = processor.onETFUpdate(update);
        updateContract(contract);
    }

    @Override
    public void onFutureUpdate(FutureUpdate update) {
        T contract = processor.onFutureUpdate(update);
        updateContract(contract);
    }

    @Override
    public void onIndexUpdate(IndexUpdate update) {
        T contract = processor.onIndexUpdate(update);
        updateContract(contract);
    }

    @Override
    public void onOptionUpdate(OptionUpdate update) {
        T contract = processor.onOptionUpdate(update);
        updateContract(contract);
    }

    @Override
    public void onSyntheticUpdate(SyntheticUpdate update) {
        T contract = processor.onSyntheticUpdate(update);
        updateContract(contract);
    }

    @Override
    public void onCfdUpdate(CfdUpdate update) {
        T contract = processor.onCfdUpdate(update);
        updateContract(contract);
    }

    protected void updateContract(T contract) {
        if (contract != null) {
            final String symbol = symbolGetter.apply(contract);
            final String brokerSymbol = brokerSymbolGetter.apply(contract);

            mapBySymbol.put(symbol, contract);
            mapByBrokerSymbol.put(brokerSymbol, contract);
        }
    }

}
