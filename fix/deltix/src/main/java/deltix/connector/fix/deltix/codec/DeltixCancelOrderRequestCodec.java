package deltix.connector.fix.deltix.codec;

import deltix.anvil.util.AsciiStringFlyweight;
import deltix.connector.fix.codec.FixCodec;
import deltix.connector.fix.deltix.message.DeltixCancelOrderRequest;
import deltix.efix.message.Message;
import deltix.efix.message.builder.MessageBuilder;
import deltix.efix.message.field.Tag;

public class DeltixCancelOrderRequestCodec<M extends DeltixCancelOrderRequest> implements FixCodec<M> {
    private final AsciiStringFlyweight originalOrderId = new AsciiStringFlyweight();

    private final AsciiStringFlyweight orderId = new AsciiStringFlyweight();

    @Override
    public void encode(DeltixCancelOrderRequest request, MessageBuilder builder) {
        if (request.hasSenderSubId()) {
            builder.addString(Tag.SenderSubID, request.getSenderSubId());
        }

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

        if (request.hasExecBroker()) {
            builder.addString(Tag.ExecBroker, request.getExecBroker());
        }

    }

    @Override
    public void decode(DeltixCancelOrderRequest response, Message message) {
        response.setOriginalOrderId(message.getString(Tag.OrigClOrdID, this.originalOrderId, null));
        response.setOrderId(message.getString(Tag.ClOrdID, this.orderId));
    }
}
