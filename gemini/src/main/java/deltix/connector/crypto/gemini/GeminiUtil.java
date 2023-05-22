package deltix.connector.crypto.gemini;

import deltix.connector.crypto.gemini.data.Fill;
import deltix.connector.crypto.gemini.data.OrderSide;
import deltix.ember.message.trade.AggressorIndicator;
import deltix.ember.message.trade.Side;
import deltix.ember.service.valid.InvalidOrderException;
import org.asynchttpclient.RequestBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class GeminiUtil {
    public static final String APIKEY_HEADER    = "X-GEMINI-APIKEY";
    public static final String PAYLOAD_HEADER   = "X-GEMINI-PAYLOAD";
    public static final String SIGNATURE_HEADER = "X-GEMINI-SIGNATURE";

    public static final NonceGenerator NONCE_GENERATOR = new CurrentTimeNonceGenerator();

    private final static char[] HEX_ARRAY_LOWER_CASE = "0123456789abcdef".toCharArray();


    public static void setRequestPayload(GeminiContext context, RequestBuilder requestBuilder, String payload) {
        final String payloadBase64 = Base64.getEncoder().encodeToString(payload.getBytes());
        final String signature = context.getApiKeyContext().calculateSignature(payloadBase64);
        requestBuilder
                .setHeader(APIKEY_HEADER, context.getApiKeyContext().getApiKey())
                .setHeader(PAYLOAD_HEADER, payloadBase64)
                .setHeader(SIGNATURE_HEADER, signature);
    }

    public static String calculateSignature(Mac mac, String preSign) {
        byte[] result = mac.doFinal(preSign.getBytes());
        return bytesToHexLowerCase(result);
    }

    public static String bytesToHexLowerCase(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY_LOWER_CASE[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY_LOWER_CASE[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static Mac createMac(String secret, String algorithm) {
        return createMac(secret.getBytes(), algorithm);
    }

    public static Mac createMac(byte[] secret, String algorithm) {
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret, algorithm);
        try {
            Mac mac = Mac.getInstance(secretKeySpec.getAlgorithm());
            mac.init(secretKeySpec);
            return mac;
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }


    public static OrderSide toGeminiSide(Side side) {
        switch (side) {
            case BUY: return OrderSide.BUY;
            case SELL: return OrderSide.SELL;
            default:
                throw new InvalidOrderException("Unsupported order side: " + side);
        }
    }

    public static Side fromGeminiSide(OrderSide side) {
        switch (side) {
            case BUY: return Side.BUY;
            case SELL: return Side.SELL;
            default:
                throw new InvalidOrderException("Unsupported order side: " + side);
        }
    }

    public static AggressorIndicator toAggressorIndicator(Fill fill) {
        final String liquidity = fill.getLiquidity();
        if ("Maker".equals(liquidity)) {
            return AggressorIndicator.ORDER_INITIATOR_IS_PASSIVE;
        } else if ("Taker".equals(liquidity)) {
            return AggressorIndicator.ORDER_INITIATOR_IS_AGGRESSOR;
        }
        return null;
    }


    // region Helper Classes

    private static final class CurrentTimeNonceGenerator implements NonceGenerator {
        private long lastNonce;

        @Override
        public synchronized long nextNonce() {
            long currentTime = System.currentTimeMillis();
            lastNonce = currentTime > lastNonce ? currentTime : lastNonce + 1;
            return lastNonce;
        }
    }

    // endregion
}
