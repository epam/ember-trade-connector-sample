package deltix.connector.crypto.gemini.data;

import com.epam.deltix.dfp.Decimal64Utils;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class DecimalSerializer extends JsonSerializer<Long> {
    @Override
    public void serialize(Long value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null || Decimal64Utils.isNull(value) || Decimal64Utils.isNaN(value)) {
            gen.writeNull();
        } else {
            gen.writeString(Decimal64Utils.toString(value));
        }
    }
}