package deltix.connector.fix.codec;

import deltix.anvil.util.AsciiStringFlyweight;
import deltix.connector.fix.message.FixCancelOrderRequest;
import deltix.efix.message.Message;
import deltix.efix.message.builder.MessageBuilder;
import deltix.efix.message.field.Tag;

public class FixCancelOrderRequestCodec<M extends FixCancelOrderRequest> implements FixCodec<M> {
    private final AsciiStringFlyweight originalOrderId = new AsciiStringFlyweight();

    private final AsciiStringFlyweight orderId = new AsciiStringFlyweight();

    @Override
    public void encode(FixCancelOrderRequest request, MessageBuilder builder) {
        builder.addString(Tag.ClOrdID, request.getOrderId());
        builder.addTimestamp(Tag.TransactTime, request.getTransactTime());

        if (request.hasRemainingQuantity()) {
            builder.addDecimal64(Tag.OrderQty, request.getRemainingQuantity());
        }

        if (request.hasSymbol()) {
            builder.addString(Tag.Symbol, request.getSymbol());
        }

        if (request.hasSide()) {
            builder.addByte(Tag.Side, request.getSide());
        }

        if (request.hasOriginalOrderId()) {
            builder.addString(Tag.OrigClOrdID, request.getOriginalOrderId());
        }

        if (request.hasExternalOrderId()) {
            builder.addString(Tag.OrderID, request.getExternalOrderId());
        }

    }

    @Override
    public void decode(FixCancelOrderRequest response, Message message) {
        response.setOriginalOrderId(message.getString(Tag.OrigClOrdID, this.originalOrderId, null));
        response.setOrderId(message.getString(Tag.ClOrdID, this.orderId));
    }
}
