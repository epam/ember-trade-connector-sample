package deltix.connector.fix.deltix.codec;

import deltix.anvil.util.AsciiStringFlyweight;
import deltix.connector.fix.codec.FixCodec;
import deltix.connector.fix.deltix.message.DeltixReplaceOrderRequest;
import deltix.efix.message.Message;
import deltix.efix.message.builder.MessageBuilder;
import deltix.efix.message.field.Tag;

public class DeltixReplaceOrderRequestCodec<M extends DeltixReplaceOrderRequest> implements FixCodec<M> {
    private final AsciiStringFlyweight orderId = new AsciiStringFlyweight();

    private final AsciiStringFlyweight originalOrderId = new AsciiStringFlyweight();

    @Override
    public void encode(DeltixReplaceOrderRequest request, MessageBuilder builder) {
        if (request.hasSenderSubId()) {
            builder.addString(Tag.SenderSubID, request.getSenderSubId());
        }

        builder.addString(Tag.OrigClOrdID, request.getOriginalOrderId());

        if (request.hasSymbol()) {
            builder.addString(Tag.Symbol, request.getSymbol());
        }

        if (request.hasOrderType()) {
            builder.addByte(Tag.OrdType, request.getOrderType());
        }

        if (request.hasSide()) {
            builder.addByte(Tag.Side, request.getSide());
        }

        if (request.hasQuantity()) {
            builder.addDecimal64(Tag.OrderQty, request.getQuantity());
        }

        if (request.hasLimitPrice()) {
            builder.addDecimal64(Tag.Price, request.getLimitPrice());
        }

        if (request.hasOrderId()) {
            builder.addString(Tag.ClOrdID, request.getOrderId());
        }

        if (request.hasDisplayQuantity()) {
            builder.addDecimal64(Tag.MaxFloor, request.getDisplayQuantity());
        }

        if (request.hasExternalOrderId()) {
            builder.addString(Tag.OrderID, request.getExternalOrderId());
        }

        if (request.hasExecBroker()) {
            builder.addString(Tag.ExecBroker, request.getExecBroker());
        }

        if (request.hasQuoteId()) {
            builder.addString(Tag.QuoteID, request.getQuoteId());
        }

        if (request.hasExchangeId()) {
            builder.addAlphanumeric(Tag.ExDestination, request.getExchangeId());
        }

        if (request.hasStopPrice()) {
            builder.addDecimal64(Tag.StopPx, request.getStopPrice());
        }

        if (request.hasExpireTime()) {
            builder.addTimestamp(Tag.ExpireTime, request.getExpireTime());
        }

        if (request.hasMinQuantity()) {
            builder.addDecimal64(Tag.MinQty, request.getMinQuantity());
        }

        if (request.hasSecurityType()) {
            builder.addString(Tag.SecurityType, request.getSecurityType());
        }

        if (request.hasTransactTime()) {
            builder.addTimestamp(Tag.TransactTime, request.getTransactTime());
        }

        if (request.hasCurrency()) {
            builder.addAlphanumeric(Tag.Currency, request.getCurrency());
        }

        if (request.hasExpireDate()) {
            builder.addDate(Tag.ExpireDate, request.getExpireDate());
        }

        if (request.hasTimeInForce()) {
            builder.addByte(Tag.TimeInForce, request.getTimeInForce());
        }

        if (request.hasAccount()) {
            builder.addString(Tag.Account, request.getAccount());
        }

    }

    @Override
    public void decode(DeltixReplaceOrderRequest response, Message message) {
        response.setOrderId(message.getString(Tag.ClOrdID, this.orderId, null));
        response.setOriginalOrderId(message.getString(Tag.OrigClOrdID, this.originalOrderId));
    }
}
