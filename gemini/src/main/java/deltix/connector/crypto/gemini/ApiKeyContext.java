package deltix.connector.crypto.gemini;

import javax.crypto.Mac;

public class ApiKeyContext implements NonceGenerator {
    private final String apiKey;
    private final NonceGenerator nonceGenerator;

    protected final Mac mac;

    public ApiKeyContext(String apiKey, String apiSecret, NonceGenerator nonceGenerator, String algorithm) {
        this.apiKey = apiKey;
        this.nonceGenerator = nonceGenerator;
        mac = createMac(apiSecret, algorithm);
    }

    protected Mac createMac(String apiSecret, String algorithm) {
        return GeminiUtil.createMac(apiSecret, algorithm);
    }

    public synchronized String calculateSignature(String preSign) {
        return GeminiUtil.calculateSignature(mac, preSign);
    }

    public String getApiKey() {
        return apiKey;
    }

    @Override
    public long nextNonce() {
        return nonceGenerator.nextNonce();
    }
}
