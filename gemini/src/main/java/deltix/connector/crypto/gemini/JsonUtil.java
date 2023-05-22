package deltix.connector.crypto.gemini;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class JsonUtil {

    private static final ThreadLocal<DecimalFormat> DECIMAL_FORMAT = ThreadLocal.withInitial(() -> {
        final DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.setMaximumFractionDigits(15);
        decimalFormat.setGroupingUsed(false);

        DecimalFormatSymbols symbols = decimalFormat.getDecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        decimalFormat.setDecimalFormatSymbols(symbols);
        return decimalFormat;
    });

    public static String format(double value) {
        return DECIMAL_FORMAT.get().format(value);
    }

    public static ObjectMapper createBaseObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // to prevent exception when encountering unknown property:
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // to allow coercion of JSON empty String ("") to null Object value:
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        // will prevent use of scientific notation for BigDecimal
        mapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
        return mapper;
    }

    public static void initObjectMapper(ObjectMapper objectMapper) {
        registerDecimalSerializers(objectMapper);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
    }

    public static void registerDecimalSerializers(ObjectMapper objectMapper) {
        SimpleModule simpleModule = new SimpleModule();
        JsonSerializer<Float> floatJsonSerializer = new JsonSerializer<>() {
            @Override
            public void serialize(Float value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
                if (value == null)
                    return;

                final double aDouble = Double.parseDouble(value.toString());
                jsonGenerator.writeNumber(format(aDouble));
            }
        };
        JsonSerializer<Double> doubleJsonSerializer = new JsonSerializer<>() {
            @Override
            public void serialize(Double value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
                if (value == null)
                    return;

                jsonGenerator.writeNumber(format(value));
            }
        };

        simpleModule.addSerializer(float.class, floatJsonSerializer);
        simpleModule.addSerializer(Float.class, floatJsonSerializer);
        simpleModule.addSerializer(double.class, doubleJsonSerializer);
        simpleModule.addSerializer(Double.class, doubleJsonSerializer);

        objectMapper.registerModule(simpleModule);
    }

    public static <T> T decodeJson(ObjectMapper objectMapper, byte[] buffer, JavaType type) {
        try {
            return objectMapper.readValue(buffer, type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String getNodeStringValue(ObjectMapper objectMapper, JsonNode nodeValue) {
        String stringValue;
        if (nodeValue.isTextual()) {
            stringValue = nodeValue.asText();
        } else if (nodeValue.isFloatingPointNumber()) {
            stringValue = valueAsString(objectMapper, nodeValue.numberValue());
        } else {
            stringValue = valueAsString(objectMapper, nodeValue);
        }
        return stringValue;
    }

    public static String valueAsString(ObjectMapper objectMapper, Object value) throws UncheckedIOException {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
