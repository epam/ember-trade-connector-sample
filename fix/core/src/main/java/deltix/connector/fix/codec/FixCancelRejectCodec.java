package deltix.connector.fix.codec;

import deltix.anvil.util.AsciiStringFlyweight;
import deltix.anvil.util.TypeConstants;
import deltix.connector.fix.message.FixCancelReject;
import deltix.efix.message.Message;
import deltix.efix.message.builder.MessageBuilder;
import deltix.efix.message.field.Tag;

public class FixCancelRejectCodec<M extends FixCancelReject> implements FixCodec<M> {
    private final AsciiStringFlyweight originalOrderId = new AsciiStringFlyweight();

    private final AsciiStringFlyweight orderId = new AsciiStringFlyweight();

    private final AsciiStringFlyweight externalOrderId = new AsciiStringFlyweight();

    private final AsciiStringFlyweight text = new AsciiStringFlyweight();

    @Override
    public void encode(FixCancelReject request, MessageBuilder builder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void decode(FixCancelReject response, Message message) {
        response.setOriginalOrderId(message.getString(Tag.OrigClOrdID, this.originalOrderId));
        response.setRejectCode(message.getInt(Tag.CxlRejReason, TypeConstants.INT_NULL));
        response.setOrderId(message.getString(Tag.ClOrdID, this.orderId));
        response.setTransactTime(message.getTimestamp(Tag.TransactTime, TypeConstants.TIMESTAMP_NULL));
        response.setOrderStatus(message.getByte(Tag.OrdStatus, TypeConstants.BYTE_NULL));
        response.setRejectType(message.getByte(Tag.CxlRejResponseTo));
        response.setExternalOrderId(message.getString(Tag.OrderID, this.externalOrderId, null));
        response.setText(message.getString(Tag.Text, this.text, null));
    }
}
