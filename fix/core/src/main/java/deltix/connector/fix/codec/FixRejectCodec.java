package deltix.connector.fix.codec;

import deltix.anvil.util.AsciiStringFlyweight;
import deltix.connector.fix.message.FixReject;
import deltix.efix.message.Message;
import deltix.efix.message.builder.MessageBuilder;
import deltix.efix.message.field.Tag;

public class FixRejectCodec<M extends FixReject> implements FixCodec<M> {
    private final AsciiStringFlyweight text = new AsciiStringFlyweight();

    @Override
    public void encode(FixReject request, MessageBuilder builder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void decode(FixReject response, Message message) {
        response.setRefMsgSeqNum(message.getInt(Tag.RefSeqNum));
        response.setText(message.getString(Tag.Text, this.text, null));
    }
}
