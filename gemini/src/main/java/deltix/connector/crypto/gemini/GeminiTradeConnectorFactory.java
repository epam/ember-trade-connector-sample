package deltix.connector.crypto.gemini;

import deltix.anvil.util.CharSequenceUtil;
import deltix.connector.common.smd.CommonContractProcessor;
import deltix.connector.common.smd.ContractMetadata;
import deltix.ember.service.connector.TradeConnectorContext;
import deltix.ember.service.connector.TradeConnectorFactory;

@SuppressWarnings("unused")
public class GeminiTradeConnectorFactory extends GeminiConnectorSettings implements TradeConnectorFactory  {

    @Override
    public GeminiTradeConnector create(final TradeConnectorContext context) {
        checkSettings(this);
        return new GeminiTradeConnector(this, context, new ContractMetadata<>(new CommonContractProcessor(attributeKey)));
    }

    private void checkSettings(GeminiConnectorSettings settings) {
        if (CharSequenceUtil.isEmptyOrNull(settings.getRestUrl()))
            throw new IllegalArgumentException("Configuration is missing REST URL");
        if (CharSequenceUtil.isEmptyOrNull(settings.getWebsocketUrl()))
            throw new IllegalArgumentException("Configuration is missing WebSocket URL");
        if (CharSequenceUtil.isEmptyOrNull(settings.getApiKey()))
            throw new IllegalArgumentException("Configuration is missing API Key");
        if (CharSequenceUtil.isEmptyOrNull(settings.getApiSecret()))
            throw new IllegalArgumentException("Configuration is missing API Secret");
    }
}

