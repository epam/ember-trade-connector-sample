package deltix.connector.fix.deltix;

import deltix.connector.common.smd.CommonContractProcessor;
import deltix.connector.common.smd.Contract;
import deltix.connector.common.util.RequestValidator;
import deltix.connector.fix.FixTradeConnector;
import deltix.connector.fix.FixUtil;
import deltix.connector.fix.codec.FixCancelRejectCodec;
import deltix.connector.fix.codec.FixExecutionReportCodec;
import deltix.connector.fix.codec.FixOrderStatusRequestCodec;
import deltix.connector.fix.deltix.codec.DeltixCancelOrderRequestCodec;
import deltix.connector.fix.deltix.codec.DeltixNewOrderRequestCodec;
import deltix.connector.fix.deltix.codec.DeltixReplaceOrderRequestCodec;
import deltix.connector.fix.deltix.message.DeltixCancelOrderRequest;
import deltix.connector.fix.deltix.message.DeltixNewOrderRequest;
import deltix.connector.fix.deltix.message.DeltixReplaceOrderRequest;
import deltix.connector.fix.mapper.CompositeIdMapper;
import deltix.connector.fix.message.FixCancelReject;
import deltix.connector.fix.message.FixExecutionReport;
import deltix.connector.fix.message.FixOrderStateMessage;
import deltix.connector.fix.message.FixOrderStatusRequest;
import deltix.efix.endpoint.session.SessionContext;
import deltix.efix.message.Header;
import deltix.efix.message.builder.MessageBuilder;
import deltix.efix.message.field.Tag;
import deltix.ember.message.trade.*;
import deltix.ember.service.connector.TradeConnectorContext;
import edu.umd.cs.findbugs.annotations.DefaultAnnotationForParameters;
import edu.umd.cs.findbugs.annotations.NonNull;

@DefaultAnnotationForParameters(NonNull.class)
final class DeltixTradeConnector extends FixTradeConnector<
        Contract,
        DeltixNewOrderRequest,
        DeltixCancelOrderRequest,
        DeltixReplaceOrderRequest,
        FixOrderStatusRequest,
        FixExecutionReport,
        FixCancelReject> {

    public static final int TAG_CANCEL_ON_DISCONNECT = 10001;

    private final String username;
    private final String password;
    private final String execBrokerId;
    private final boolean cancelOnDisconnect;

    DeltixTradeConnector(final TradeConnectorContext connectorContext,
                         final SessionContext sessionContext,
                         final String attributeKey,
                         final String username,
                         final String password,
                         final String execBrokerId,
                         final boolean cancelOnDisconnect) {
        super(
                connectorContext,
                sessionContext,
                new CommonContractProcessor(attributeKey),
                RequestValidator.NOOP_VALIDATOR,
                new CompositeIdMapper<>(),
                new DeltixNewOrderRequest(),
                new DeltixCancelOrderRequest(),
                new DeltixReplaceOrderRequest(),
                new FixOrderStatusRequest(),
                new FixExecutionReport(),
                new FixCancelReject(),
                new DeltixNewOrderRequestCodec<>(),
                new DeltixCancelOrderRequestCodec<>(),
                new DeltixReplaceOrderRequestCodec<>(),
                new FixOrderStatusRequestCodec<>(),
                new FixExecutionReportCodec<>(),
                new FixCancelRejectCodec<>()
        );

        this.username = username;
        this.password = password;
        this.execBrokerId = execBrokerId;
        this.cancelOnDisconnect = cancelOnDisconnect;
    }

    @Override
    protected void makeLogon(boolean resetSeqNum, MessageBuilder builder) {
        super.makeLogon(resetSeqNum, builder);

        FixUtil.addNullableCharSequence(Tag.Username, username, builder);
        FixUtil.addNullableCharSequence(Tag.Password, password, builder);
        builder.addBoolean(TAG_CANCEL_ON_DISCONNECT, cancelOnDisconnect);
    }

    @Override
    protected void makeNewOrderMessage(OrderNewRequest request, Contract contract, DeltixNewOrderRequest message) {
        super.makeNewOrderMessage(request, contract, message);

        // pass TraderID in SenderSubID(50)
        message.setSenderSubId(request.getTraderId());
        message.setExecBroker(execBrokerId);
    }

    @Override
    protected void makeCancelOrderMessage(OrderCancelRequest request, Contract contract, DeltixCancelOrderRequest message) {
        super.makeCancelOrderMessage(request, contract, message);

        // pass TraderID in SenderSubID(50)
        message.setSenderSubId(request.getTraderId());
        message.setExecBroker(execBrokerId);
    }

    @Override
    protected void makeReplaceOrderMessage(OrderReplaceRequest request, Contract contract, DeltixReplaceOrderRequest message) {
        super.makeReplaceOrderMessage(request, contract, message);

        // pass TraderID in SenderSubID(50)
        message.setSenderSubId(request.getTraderId());
        message.setExecBroker(execBrokerId);
    }

    @Override
    protected void makeOrderRequestMessage(OrderEntryRequest request, Contract contract, FixOrderStateMessage message) {
        super.makeOrderRequestMessage(request, contract, message);

        // populate Account and ExchangeId from request
        message.setAccount(request.getAccount());
        message.setExchangeId(request.getExchangeId());
    }

    @Override
    protected void makeOrderEvent(Header header, FixExecutionReport message, Contract contract, MutableOrderEvent event) {
        super.makeOrderEvent(header, message, contract, event);

        // extract Account and ExchangeId from ExecutionReport
        event.setAccount(message.getAccount());
        event.setExchangeId(message.getExchangeId());
    }
}
