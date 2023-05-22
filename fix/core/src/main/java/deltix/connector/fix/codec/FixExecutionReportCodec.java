package deltix.connector.fix.codec;

import deltix.anvil.util.AsciiStringFlyweight;
import deltix.anvil.util.TypeConstants;
import deltix.connector.fix.message.FixExecutionReport;
import deltix.efix.message.Message;
import deltix.efix.message.builder.MessageBuilder;
import deltix.efix.message.field.Tag;

public class FixExecutionReportCodec<M extends FixExecutionReport> implements FixCodec<M> {
    private final AsciiStringFlyweight symbol = new AsciiStringFlyweight();

    private final AsciiStringFlyweight orderId = new AsciiStringFlyweight();

    private final AsciiStringFlyweight originalOrderId = new AsciiStringFlyweight();

    private final AsciiStringFlyweight executionReferenceId = new AsciiStringFlyweight();

    private final AsciiStringFlyweight text = new AsciiStringFlyweight();

    private final AsciiStringFlyweight externalOrderId = new AsciiStringFlyweight();

    private final AsciiStringFlyweight quoteId = new AsciiStringFlyweight();

    private final AsciiStringFlyweight executionId = new AsciiStringFlyweight();

    private final AsciiStringFlyweight securityType = new AsciiStringFlyweight();

    private final AsciiStringFlyweight account = new AsciiStringFlyweight();

    @Override
    public void encode(FixExecutionReport request, MessageBuilder builder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void decode(FixExecutionReport response, Message message) {
        response.setRemainingQuantity(message.getDecimal64(Tag.LeavesQty, TypeConstants.DECIMAL64_NULL));
        response.setSymbol(message.getString(Tag.Symbol, this.symbol, null));
        response.setOrderType(message.getByte(Tag.OrdType, TypeConstants.BYTE_NULL));
        response.setOrderId(message.getString(Tag.ClOrdID, this.orderId, null));
        response.setDisplayQuantity(message.getDecimal64(Tag.MaxFloor, TypeConstants.DECIMAL64_NULL));
        response.setExecutionType(message.getByte(Tag.ExecType));
        response.setOrderStatus(message.getByte(Tag.OrdStatus, TypeConstants.BYTE_NULL));
        response.setCumulativeQuantity(message.getDecimal64(Tag.CumQty, TypeConstants.DECIMAL64_NULL));
        response.setMinQuantity(message.getDecimal64(Tag.MinQty, TypeConstants.DECIMAL64_NULL));
        response.setOriginalOrderId(message.getString(Tag.OrigClOrdID, this.originalOrderId, null));
        response.setRejectCode(message.getInt(Tag.OrdRejReason, TypeConstants.INT_NULL));
        response.setExecutionReferenceId(message.getString(Tag.ExecRefID, this.executionReferenceId, null));
        response.setExecutionPrice(message.getDecimal64(Tag.LastPx, TypeConstants.DECIMAL64_NULL));
        response.setCurrency(message.getAlphanumeric(Tag.Currency, TypeConstants.ALPHANUMERIC_NULL));
        response.setExpireDate(message.getDate(Tag.ExpireDate, TypeConstants.TIMESTAMP_NULL));
        response.setText(message.getString(Tag.Text, this.text, null));
        response.setTimeInForce(message.getByte(Tag.TimeInForce, TypeConstants.BYTE_NULL));
        response.setSide(message.getByte(Tag.Side, TypeConstants.BYTE_NULL));
        response.setQuantity(message.getDecimal64(Tag.OrderQty, TypeConstants.DECIMAL64_NULL));
        response.setLimitPrice(message.getDecimal64(Tag.Price, TypeConstants.DECIMAL64_NULL));
        response.setExecutionQuantity(message.getDecimal64(Tag.LastQty, TypeConstants.DECIMAL64_NULL));
        response.setExternalOrderId(message.getString(Tag.OrderID, this.externalOrderId, null));
        response.setTradeDate(message.getDate(Tag.TradeDate, TypeConstants.TIMESTAMP_NULL));
        response.setSettlementDate(message.getDate(Tag.SettlDate, TypeConstants.TIMESTAMP_NULL));
        response.setQuoteId(message.getString(Tag.QuoteID, this.quoteId, null));
        response.setExchangeId(message.getAlphanumeric(Tag.ExDestination, TypeConstants.ALPHANUMERIC_NULL));
        response.setExecutionId(message.getString(Tag.ExecID, this.executionId));
        response.setStopPrice(message.getDecimal64(Tag.StopPx, TypeConstants.DECIMAL64_NULL));
        response.setExpireTime(message.getTimestamp(Tag.ExpireTime, TypeConstants.TIMESTAMP_NULL));
        response.setMultiLegReportingType(message.getByte(Tag.MultiLegReportingType, TypeConstants.BYTE_NULL));
        response.setSecurityType(message.getString(Tag.SecurityType, this.securityType, null));
        response.setTransactTime(message.getTimestamp(Tag.TransactTime, TypeConstants.TIMESTAMP_NULL));
        response.setAveragePrice(message.getDecimal64(Tag.AvgPx, TypeConstants.DECIMAL64_NULL));
        response.setAccount(message.getString(Tag.Account, this.account, null));
    }
}
