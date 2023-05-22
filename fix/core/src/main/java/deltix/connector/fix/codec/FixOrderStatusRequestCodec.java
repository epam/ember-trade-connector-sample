package deltix.connector.fix.codec;

import deltix.anvil.util.AsciiStringFlyweight;
import deltix.connector.fix.message.FixOrderStatusRequest;
import deltix.efix.message.Message;
import deltix.efix.message.builder.MessageBuilder;
import deltix.efix.message.field.Tag;

public class FixOrderStatusRequestCodec<M extends FixOrderStatusRequest> implements FixCodec<M> {
    private final AsciiStringFlyweight symbol = new AsciiStringFlyweight();

    private final AsciiStringFlyweight orderId = new AsciiStringFlyweight();

    private final AsciiStringFlyweight externalOrderId = new AsciiStringFlyweight();

    @Override
    public void encode(FixOrderStatusRequest request, MessageBuilder builder) {
        builder.addString(Tag.Symbol, request.getSymbol());
        builder.addString(Tag.ClOrdID, request.getOrderId());
        builder.addTimestamp(Tag.TransactTime, request.getTransactTime());

        if (request.hasExternalOrderId()) {
            builder.addString(Tag.OrderID, request.getExternalOrderId());
        }

    }

    @Override
    public void decode(FixOrderStatusRequest response, Message message) {
        response.setSymbol(message.getString(Tag.Symbol, this.symbol));
        response.setOrderId(message.getString(Tag.ClOrdID, this.orderId));
        response.setExternalOrderId(message.getString(Tag.OrderID, this.externalOrderId, null));
    }
}
