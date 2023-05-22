package deltix.connector.fix.codec;

import deltix.connector.fix.message.FixMessage;
import deltix.efix.message.builder.MessageBuilder;

/**
 * FIX codec encodes/decodes high level application message to/from low level FIX message.
 *
 * @param <M> message type
 */
public interface FixCodec<M extends FixMessage> {

    void encode(M message, MessageBuilder builder);

    void decode(M message, deltix.efix.message.Message map);

}
