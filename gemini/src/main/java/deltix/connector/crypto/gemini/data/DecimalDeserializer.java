package deltix.connector.crypto.gemini.data;

import com.epam.deltix.dfp.Decimal64Utils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class DecimalDeserializer extends JsonDeserializer<Long> {
    @Override
    public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        final String value = p.getText();
        if (value == null) {
            return Decimal64Utils.NaN;
        }
        return Decimal64Utils.parse(value);
    }
}