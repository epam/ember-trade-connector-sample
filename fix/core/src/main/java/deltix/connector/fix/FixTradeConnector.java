package deltix.connector.fix;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import deltix.anvil.message.MutableShutdownResponse;
import deltix.anvil.message.NodeStatus;
import deltix.anvil.message.NodeStatusEvent;
import deltix.anvil.message.ShutdownRequest;
import deltix.anvil.service.ServiceWorkerAware;
import deltix.anvil.util.ByteSequence;
import deltix.anvil.util.Reusable;
import deltix.anvil.util.buffer.Buffer;
import deltix.anvil.util.counter.Counter;
import deltix.connector.common.Messages;
import deltix.connector.common.smd.ContractMetadata;
import deltix.connector.common.smd.ContractProcessor;
import deltix.connector.common.util.RequestValidator;
import deltix.connector.fix.codec.FixCodec;
import deltix.connector.fix.codec.FixRejectCodec;
import deltix.connector.fix.mapper.IdMapper;
import deltix.connector.fix.message.FixMessage;
import deltix.connector.fix.message.FixOrderStateMessage;
import deltix.connector.fix.message.FixReject;
import deltix.connector.fix.session.SessionException;
import deltix.efix.endpoint.connector.ConnectionException;
import deltix.efix.endpoint.session.Session;
import deltix.efix.endpoint.session.SessionContext;
import deltix.efix.endpoint.state.SessionStatus;
import deltix.efix.endpoint.store.MessageStore;
import deltix.efix.message.Header;
import deltix.efix.message.Message;
import deltix.efix.message.field.TimeInForce;
import deltix.efix.message.field.*;
import deltix.ember.message.smd.*;
import deltix.ember.message.trade.*;
import deltix.ember.service.Ember;
import deltix.ember.service.InstrumentSnapshot;
import deltix.ember.service.connector.TradeConnector;
import deltix.ember.service.connector.TradeConnectorContext;
import deltix.ember.service.connector.TradeConnectorStatusIndicator;
import edu.umd.cs.findbugs.annotations.DefaultAnnotationForParameters;
import edu.umd.cs.findbugs.annotations.NonNull;

@DefaultAnnotationForParameters(NonNull.class)
@SuppressWarnings("WeakerAccess")
public abstract class FixTradeConnector<
        Contract extends deltix.connector.common.smd.Contract,
        FixNewOrderRequest extends deltix.connector.fix.message.FixNewOrderRequest,
        FixCancelOrderRequest extends deltix.connector.fix.message.FixCancelOrderRequest,
        FixReplaceOrderRequest extends deltix.connector.fix.message.FixReplaceOrderRequest,
        FixOrderStatusRequest extends deltix.connector.fix.message.FixOrderStatusRequest,
        FixExecutionReport extends deltix.connector.fix.message.FixExecutionReport,
        FixCancelReject extends deltix.connector.fix.message.FixCancelReject
        > extends Session implements TradeConnector, ServiceWorkerAware {

    protected static final Log LOG = LogFactory.getLog(Session.class);

    protected final RejectHandler rejectHandler = new RejectHandler();

    protected final IdMapper<FixNewOrderRequest, FixCancelOrderRequest, FixReplaceOrderRequest, FixOrderStatusRequest, FixExecutionReport, FixCancelReject> idMapper;

    protected final Messages messages = new Messages();

    protected final FixReject reject = new FixReject();
    protected final FixNewOrderRequest fixNewOrderRequest;
    protected final FixCancelOrderRequest fixCancelOrderRequest;
    protected final FixReplaceOrderRequest fixReplaceOrderRequest;
    protected final FixOrderStatusRequest fixOrderStatusRequest;
    protected final FixExecutionReport fixExecutionReport;
    protected final FixCancelReject fixCancelReject;

    protected final FixCodec<FixReject> fixRejectCodec = new FixRejectCodec<>();
    protected final FixCodec<FixNewOrderRequest> fixNewOrderRequestCodec;
    protected final FixCodec<FixCancelOrderRequest> fixCancelOrderRequestCodec;
    protected final FixCodec<FixReplaceOrderRequest> fixReplaceOrderRequestCodec;
    protected final FixCodec<FixOrderStatusRequest> fixOrderStatusRequestCodec;
    protected final FixCodec<FixExecutionReport> fixExecutionReportCodec;
    protected final FixCodec<FixCancelReject> fixCancelRejectCodec;

    protected final long id;
    protected final String name;

    protected final ContractMetadata<Contract> metadata;
    protected final InstrumentSnapshot instrumentSnapshot;
    protected final RequestValidator requestValidator;

    private final Ember ember;

    private final Counter statusIndicator;

    protected NodeStatus nodeStatus = NodeStatus.BOOTSTRAP;
    protected deltix.ember.message.trade.SessionStatus sessionStatus = deltix.ember.message.trade.SessionStatus.DISCONNECTED;

    protected boolean shutdownRequested;
    protected boolean keepSessionAlive;

    public FixTradeConnector(final TradeConnectorContext connectorContext,
                             final SessionContext sessionContext,
                             final ContractMetadata<Contract> metadata,
                             final RequestValidator requestValidator,
                             final IdMapper<FixNewOrderRequest, FixCancelOrderRequest, FixReplaceOrderRequest, FixOrderStatusRequest, FixExecutionReport, FixCancelReject> idMapper,
                             final FixNewOrderRequest fixNewOrderRequest,
                             final FixCancelOrderRequest fixCancelOrderRequest,
                             final FixReplaceOrderRequest fixReplaceOrderRequest,
                             final FixOrderStatusRequest fixOrderStatusRequest,
                             final FixExecutionReport fixExecutionReport,
                             final FixCancelReject fixCancelReject,
                             final FixCodec<FixNewOrderRequest> fixNewOrderRequestCodec,
                             final FixCodec<FixCancelOrderRequest> fixCancelOrderRequestCodec,
                             final FixCodec<FixReplaceOrderRequest> fixReplaceOrderRequestCodec,
                             final FixCodec<FixOrderStatusRequest> fixOrderStatusRequestCodec,
                             final FixCodec<FixExecutionReport> fixExecutionReportCodec,
                             final FixCodec<FixCancelReject> fixCancelRejectCodec) {
        super(sessionContext);

        this.id = connectorContext.getId();
        this.name = connectorContext.getName();
        this.metadata = metadata;
        this.requestValidator = requestValidator;
        this.instrumentSnapshot = connectorContext.getInstrumentSnapshot();
        this.ember = connectorContext.getEmber();
        this.idMapper = idMapper;
        this.statusIndicator = connectorContext.getStatusIndicator();

        this.fixNewOrderRequest = fixNewOrderRequest;
        this.fixCancelOrderRequest = fixCancelOrderRequest;
        this.fixReplaceOrderRequest = fixReplaceOrderRequest;
        this.fixOrderStatusRequest = fixOrderStatusRequest;
        this.fixExecutionReport = fixExecutionReport;
        this.fixCancelReject = fixCancelReject;

        this.fixNewOrderRequestCodec = fixNewOrderRequestCodec;
        this.fixCancelOrderRequestCodec = fixCancelOrderRequestCodec;
        this.fixReplaceOrderRequestCodec = fixReplaceOrderRequestCodec;
        this.fixOrderStatusRequestCodec = fixOrderStatusRequestCodec;
        this.fixExecutionReportCodec = fixExecutionReportCodec;
        this.fixCancelRejectCodec = fixCancelRejectCodec;
    }

    public FixTradeConnector(final TradeConnectorContext connectorContext,
                             final SessionContext sessionContext,
                             final ContractProcessor<Contract> processor,
                             final RequestValidator requestValidator,
                             final IdMapper<FixNewOrderRequest, FixCancelOrderRequest, FixReplaceOrderRequest, FixOrderStatusRequest, FixExecutionReport, FixCancelReject> idMapper,
                             final FixNewOrderRequest fixNewOrderRequest,
                             final FixCancelOrderRequest fixCancelOrderRequest,
                             final FixReplaceOrderRequest fixReplaceOrderRequest,
                             final FixOrderStatusRequest fixOrderStatusRequest,
                             final FixExecutionReport fixExecutionReport,
                             final FixCancelReject fixCancelReject,
                             final FixCodec<FixNewOrderRequest> fixNewOrderRequestCodec,
                             final FixCodec<FixCancelOrderRequest> fixCancelOrderRequestCodec,
                             final FixCodec<FixReplaceOrderRequest> fixReplaceOrderRequestCodec,
                             final FixCodec<FixOrderStatusRequest> fixOrderStatusRequestCodec,
                             final FixCodec<FixExecutionReport> fixExecutionReportCodec,
                             final FixCodec<FixCancelReject> fixCancelRejectCodec) {
        this(connectorContext, sessionContext, new ContractMetadata<>(processor), requestValidator, idMapper,
             fixNewOrderRequest, fixCancelOrderRequest, fixReplaceOrderRequest, fixOrderStatusRequest, fixExecutionReport, fixCancelReject,
             fixNewOrderRequestCodec, fixCancelOrderRequestCodec, fixReplaceOrderRequestCodec, fixOrderStatusRequestCodec, fixExecutionReportCodec, fixCancelRejectCodec);
    }

    @Override
    public final long getId() {
        return id;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public void open() {
        try {
            super.open();
            instrumentSnapshot.forEach(metadata);
        } catch (Exception e) {
            processError(e);
            throw e;
        }
    }

    @Override
    public void close() {
        try {
            super.close();
        } catch (Exception e) {
            processError(e);
            throw e;
        }
    }

    @Override
    public int doFirst(int workDone) {
        return work();
    }

    @Override
    public int doLast(int workDone) {
        if (workDone == 0) {
            flush();
        }

        return 0;
    }

    @Override
    public void onCloseSignal() {
        super.deactivate();
    }

    @Override
    public boolean readyToClose() {
        return !super.active();
    }

    @Override
    public void onException(Throwable e) {
        logException(e);
    }

    @Override
    protected int doSendMessages() {
        return 0;
    }

    protected <T extends FixMessage> void sendAppMessageIfLeader(final ByteSequence msgType,
                                                                 final FixCodec<T> codec,
                                                                 final T message) {

        if (nodeStatus == NodeStatus.LEADER) {
            if (state.status() != SessionStatus.APPLICATION_CONNECTED) {
                throw SessionException.SESSION_NOT_CONNECTED;
            }

            message.validate();

            builder.wrap(messageBuffer);
            codec.encode(message, builder);

            sendAppMessage(msgType, messageBuffer, 0, builder.size());
        }
    }

    @Override
    public void onNodeStatusEvent(NodeStatusEvent event) {
        final NodeStatus previous = nodeStatus;
        final NodeStatus next = event.getNodeStatus();
        nodeStatus = next;

        keepSessionAlive = (next == NodeStatus.LEADER && !shutdownRequested);

        if (next == NodeStatus.LEADER) {
            fireSessionStatusEvent(sessionStatus);
        }

        LOG.info("Connector %s switched from %s to %s role")
                .with(name)
                .with(previous)
                .with(next);
    }

    @Override
    public void onShutdownRequest(final ShutdownRequest request) {
        shutdownRequested = true;
        keepSessionAlive = false;

        if (state.status() == SessionStatus.DISCONNECTED) {
            fireShutdownResponse();
        }
    }

    @Override
    protected boolean canStartSession(long now) {
        return keepSessionAlive;
    }

    @Override
    protected boolean canEndSession(long now) {
        return !keepSessionAlive;
    }

    @Override
    protected void onError(Throwable e) {
        if (e instanceof ConnectionException) {
            logConnectionException(e);
        } else {
            logException(e);
        }
    }

    @Override
    protected void onStatusUpdate(SessionStatus previous, SessionStatus next) {
        LOG.info("Connector %s status has been changed %s -> %s")
                .with(name)
                .with(previous)
                .with(next);

        if (next == SessionStatus.APPLICATION_CONNECTED) {
            statusIndicator.setOrdered(TradeConnectorStatusIndicator.CONNECTED);
            sessionStatus = deltix.ember.message.trade.SessionStatus.CONNECTED;
            fireSessionStatusEvent(sessionStatus);
        } else if (previous == SessionStatus.APPLICATION_CONNECTED) {
            statusIndicator.setOrdered(TradeConnectorStatusIndicator.DISCONNECTED);
            sessionStatus = deltix.ember.message.trade.SessionStatus.DISCONNECTED;
            fireSessionStatusEvent(sessionStatus);
        } else if (shutdownRequested && next == SessionStatus.DISCONNECTED) {
            fireShutdownResponse();
        }
    }

    @Override
    protected void onAdminMessage(Header header, Message message) {
        if (MsgType.REJECT.equals(header.msgType())) {
            onReject(header, message);
        }
    }

    @Override
    protected void onAppMessage(Header header, Message message) {
        if (nodeStatus != NodeStatus.LEADER) {
            return;
        }

        ByteSequence msgType = header.msgType();
        try {
            if (MsgType.EXECUTION_REPORT.equals(msgType)) {
                onExecutionReport(header, message);
            } else if (MsgType.ORDER_CANCEL_REJECT.equals(msgType)) {
                onCancelReject(header, message);
            } else if (MsgType.BUSINESS_MESSAGE_REJECT.equals(msgType)) {
                onBusinessReject(header, message);
            } else if (MsgType.NEWS.equals(msgType)) {
                onNews(header, message);
            } else {
                onCustomMessage(header, message);
            }
        } catch (final Throwable e) {
            LOG.warn("Connector %s can't handle app message with msg type: %s, msg seq num: %s. Error: %s")
                    .with(name)
                    .with(msgType)
                    .with(header.msgSeqNum())
                    .with(e);
        }
    }

    //region Receiving Reject

    protected void onReject(Header header, Message message) {
        FixReject reject = decode(fixRejectCodec, this.reject, message);

        LOG.warn("Connector %s has received Reject(3) with ref msg seq num: %s, text: %s")
                .with(name)
                .with(reject.getRefMsgSeqNum())
                .with(reject.getText());

        boolean read = rejectHandler.onReject(header, reject);
        if (!read) {
            throw SessionException.MESSAGE_NOT_FOUND_IN_STORE;
        }
    }

    //endregion

    //region Receiving Business Reject

    protected void onBusinessReject(Header header, Message message) {
        FixReject reject = decode(fixRejectCodec, this.reject, message);

        LOG.warn("Connector %s has received BusinessReject(j) with ref msg seq num: %s, text: %s")
                .with(name)
                .with(reject.getRefMsgSeqNum())
                .with(reject.getText());

        boolean read = rejectHandler.onReject(header, reject);
        if (!read) {
            throw SessionException.MESSAGE_NOT_FOUND_IN_STORE;
        }
    }

    protected void onMessageReject(Header header, FixReject reject, ByteSequence refMsgType, Buffer refBody, int offset, int length) {
        Message message = this.message;
        message.parse(refBody, offset, length);

        if (MsgType.ORDER_SINGLE.equals(refMsgType)) {
            onNewOrderMessageReject(header, reject, message);
        } else if (MsgType.ORDER_CANCEL_REQUEST.equals(refMsgType)) {
            onCancelOrderMessageReject(header, reject, message);
        } else if (MsgType.ORDER_CANCEL_REPLACE_REQUEST.equals(refMsgType)) {
            onReplaceOrderMessageReject(header, reject, message);
        } else if (MsgType.ORDER_STATUS_REQUEST.equals(refMsgType)) {
            onOrderStatusMessageReject(header, reject, message);
        } else {
            onCustomMessageReject(header, reject, refMsgType, message);
        }
    }

    protected void onNewOrderMessageReject(Header header, FixReject reject, Message message) {
        FixNewOrderRequest refMessage = decode(fixNewOrderRequestCodec, fixNewOrderRequest, message);
        MutableOrderRejectEvent event = messages.orderRejectEvent();

        makeOrderRejectEvent(header, reject, refMessage, event);
        fireOrderRejectEvent(event);
    }

    protected void onCancelOrderMessageReject(Header header, FixReject reject, Message message) {
        FixCancelOrderRequest refMessage = decode(fixCancelOrderRequestCodec, fixCancelOrderRequest, message);
        MutableOrderCancelRejectEvent event = messages.orderCancelRejectEvent();

        makeOrderCancelRejectEvent(header, reject, refMessage, event);
        fireOrderCancelRejectEvent(event);
    }

    protected void onReplaceOrderMessageReject(Header header, FixReject reject, Message message) {
        FixReplaceOrderRequest refMessage = decode(fixReplaceOrderRequestCodec, fixReplaceOrderRequest, message);
        MutableOrderReplaceRejectEvent event = messages.orderReplaceRejectEvent();

        makeOrderReplaceRejectEvent(header, reject, refMessage, event);
        fireOrderReplaceRejectEvent(event);
    }

    @SuppressWarnings("UnusedParameters")
    protected void onOrderStatusMessageReject(Header header, FixReject reject, Message message) {
        LOG.warn("Connector %s skipping BusinessReject(j) for OrderStatusRequest(H), msg seq num: %s")
                .with(name)
                .with(reject.getRefMsgSeqNum());
    }

    @SuppressWarnings("UnusedParameters")
    protected void onCustomMessageReject(Header header, FixReject reject, ByteSequence refMsgType, Message message) {
        LOG.warn("Connector %s skipping BusinessReject(j) for message with msg type: %s, msg seq num: %s")
                .with(name)
                .with(refMsgType)
                .with(reject.getRefMsgSeqNum());
    }

    @SuppressWarnings("UnusedParameters")
    protected void makeOrderRejectEvent(Header header, FixReject message, FixNewOrderRequest refMessage, MutableOrderRejectEvent event) {
        idMapper.messageToEvent(refMessage, event);

        event.setSourceId(id);
        event.setReason(message.getText());
        event.setTimestamp(clock.time());
    }

    protected void makeOrderCancelRejectEvent(Header header, FixReject message, FixCancelOrderRequest refMessage, MutableOrderCancelRejectEvent event) {
        idMapper.messageToEvent(refMessage, event);

        event.setReason(message.getText());
        event.setSourceId(id);
        event.setExternalOrderId(refMessage.getExternalOrderId());
        event.setOrderStatus(null); // we do not know order status here
        event.setTimestamp(clock.time());
        event.setSequenceNumber(header.msgSeqNum());

        event.setAttributes(null);
    }

    protected void makeOrderReplaceRejectEvent(Header header, FixReject message, FixReplaceOrderRequest refMessage, MutableOrderReplaceRejectEvent event) {
        idMapper.messageToEvent(refMessage, event);

        event.setReason(message.getText());
        event.setSourceId(id);
        event.setExternalOrderId(refMessage.getExternalOrderId());
        event.setOrderStatus(null); // we do not know order status for entire replacement chain
        event.setTimestamp(clock.time());
        event.setSequenceNumber(header.msgSeqNum());

        event.setAttributes(null);
    }

    //endregion

    //region Receiving Execution Report

    protected void onExecutionReport(Header header, Message message) {
        FixExecutionReport report = decode(fixExecutionReportCodec, fixExecutionReport, message);
        Contract contract = getContract(report);
        byte type = report.getExecutionType();

        switch (type) {
            case ExecType.PENDING_NEW:
                onOrderPendingNewMessage(header, report, contract, messages.orderPendingNewEvent());
                break;

            case ExecType.NEW:
                onOrderNewMessage(header, report, contract, messages.orderNewEvent());
                break;

            case ExecType.REJECTED:
                onOrderRejectMessage(header, report, contract, messages.orderRejectEvent());
                break;

            case ExecType.PENDING_CANCEL:
                onOrderPendingCancelMessage(header, report, contract, messages.orderPendingCancelEvent());
                break;

            case ExecType.CANCELED:
            case ExecType.EXPIRED:
            case ExecType.DONE_FOR_DAY:
                onOrderCancelMessage(header, report, contract, messages.orderCancelEvent());
                break;

            case ExecType.PENDING_REPLACE:
                onOrderPendingReplaceMessage(header, report, contract, messages.orderPendingReplaceEvent());
                break;

            case ExecType.REPLACE:
                onOrderReplaceMessage(header, report, contract, messages.orderReplaceEvent());
                break;

            case ExecType.ORDER_STATUS:
                onOrderStatusMessage(header, report, contract, messages.orderStatusEvent());
                break;

            case ExecType.TRADE:
            case ExecType.FILL:
            case ExecType.PARTIAL_FILL:
                onTradeReportMessage(header, report, contract, messages.tradeReportEvent());
                break;

            case ExecType.TRADE_CANCEL:
                onTradeCancelMessage(header, report, contract, messages.tradeCancelEvent());
                break;

            case ExecType.TRADE_CORRECT:
                onTradeCorrectMessage(header, report, contract, messages.tradeCorrectEvent());
                break;

            default:
                onCustomExecutionMessage(header, report, contract);
        }
    }

    protected void onOrderPendingNewMessage(Header header, FixExecutionReport message, Contract contract, MutableOrderPendingNewEvent event) {
        makeOrderPendingNewEvent(header, message, event, contract);
        fireOrderPendingNewEvent(event);
    }

    protected void onOrderNewMessage(Header header, FixExecutionReport message, Contract contract, MutableOrderNewEvent event) {
        makeOrderNewEvent(header, message, contract, event);
        fireOrderNewEvent(event);
    }

    protected void onOrderRejectMessage(Header header, FixExecutionReport message, Contract contract, MutableOrderRejectEvent event) {
        makeOrderRejectEvent(header, message, contract, event);
        fireOrderRejectEvent(event);
    }

    protected void onOrderPendingCancelMessage(Header header, FixExecutionReport message, Contract contract, MutableOrderPendingCancelEvent event) {
        makeOrderPendingCancelEvent(header, message, contract, event);
        fireOrderPendingCancelEvent(event);
    }

    protected void onOrderCancelMessage(Header header, FixExecutionReport message, Contract contract, MutableOrderCancelEvent event) {
        makeOrderCancelEvent(header, message, event, contract);
        fireOrderCancelEvent(event);
    }

    protected void onOrderPendingReplaceMessage(Header header, FixExecutionReport message, Contract contract, MutableOrderPendingReplaceEvent event) {
        makeOrderPendingReplaceEvent(header, message, contract, event);
        fireOrderPendingReplaceEvent(event);
    }

    protected void onOrderReplaceMessage(Header header, FixExecutionReport message, Contract contract, MutableOrderReplaceEvent event) {
        makeOrderReplaceEvent(header, message, contract, event);
        fireOrderReplaceEvent(event);
    }

    protected void onOrderStatusMessage(Header header, FixExecutionReport message, Contract contract, MutableOrderStatusEvent event) {
        makeOrderStatusEvent(header, message, contract, event);
        fireOrderStatusEvent(event);
    }

    protected void onTradeReportMessage(Header header, FixExecutionReport message, Contract contract, MutableOrderTradeReportEvent event) {
        makeTradeReportEvent(header, message, contract, event);
        fireOrderTradeReportEvent(event);
    }

    protected void onTradeCancelMessage(Header header, FixExecutionReport message, Contract contract, MutableOrderTradeCancelEvent event) {
        makeTradeCancelEvent(header, message, contract, event);
        fireOrderTradeCancelEvent(event);
    }

    protected void onTradeCorrectMessage(Header header, FixExecutionReport message, Contract contract, MutableOrderTradeCorrectEvent event) {
        makeTradeCorrectEvent(header, message, contract, event);
        fireOrderTradeCorrectEvent(event);
    }

    protected void onCustomExecutionMessage(Header header, @SuppressWarnings("unused") FixExecutionReport message, @SuppressWarnings("unused") Contract contract) {
        LOG.info("Connector %s skipping ExecutionReport(8) with msg seq num: %s")
                .with(name)
                .with(header.msgSeqNum());
    }

    protected void makeOrderPendingNewEvent(Header header, FixExecutionReport message, MutableOrderPendingNewEvent event, Contract contract) {
        idMapper.messageToEvent(message, event);
        makeOrderEvent(header, message, contract, event);
    }

    protected void makeOrderNewEvent(Header header, FixExecutionReport message, Contract contract, MutableOrderNewEvent event) {
        idMapper.messageToEvent(message, event);
        makeOrderEvent(header, message, contract, event);
    }

    protected void makeOrderRejectEvent(Header header, FixExecutionReport message, Contract contract, MutableOrderRejectEvent event) {
        idMapper.messageToEvent(message, event);
        makeOrderEvent(header, message, contract, event);
        event.setReason(message.getText());
        event.setVendorRejectCode(message.getRejectCode());
    }

    protected void makeOrderPendingCancelEvent(Header header, FixExecutionReport message, Contract contract, MutableOrderPendingCancelEvent event) {
        idMapper.messageToEvent(message, event);
        makeOrderEvent(header, message, contract, event);
        event.setReason(message.getText());
    }

    protected void makeOrderCancelEvent(Header header, FixExecutionReport message, MutableOrderCancelEvent event, Contract contract) {
        idMapper.messageToEvent(message, event);
        makeOrderEvent(header, message, contract, event);
        event.setReason(message.getText());
    }

    protected void makeOrderPendingReplaceEvent(Header header, FixExecutionReport message, Contract contract, MutableOrderPendingReplaceEvent event) {
        idMapper.messageToEvent(message, event);
        makeOrderEvent(header, message, contract, event);
    }

    protected void makeOrderReplaceEvent(Header header, FixExecutionReport message, Contract contract, MutableOrderReplaceEvent event) {
        idMapper.messageToEvent(message, event);
        makeOrderEvent(header, message, contract, event);
    }

    protected void makeOrderStatusEvent(Header header, FixExecutionReport message, Contract contract, MutableOrderStatusEvent event) {
        idMapper.messageToEvent(message, event);
        makeOrderEvent(header, message, contract, event);
        event.setOrderUnknown(message.getRejectCode() == OrdRejReason.UNKNOWN_ORDER);
    }

    protected void makeTradeReportEvent(Header header, FixExecutionReport message, Contract contract, MutableOrderTradeReportEvent event) {
        idMapper.messageToEvent(message, event);
        makeTradeEvent(header, message, contract, event);
    }

    protected void makeTradeCancelEvent(Header header, FixExecutionReport message, Contract contract, MutableOrderTradeCancelEvent event) {
        idMapper.messageToEvent(message, event);
        makeTradeEvent(header, message, contract, event);
        event.setReferenceEventId(message.getExecutionReferenceId());

    }

    protected void makeTradeCorrectEvent(Header header, FixExecutionReport message, Contract contract, MutableOrderTradeCorrectEvent event) {
        idMapper.messageToEvent(message, event);
        makeTradeEvent(header, message, contract, event);
        event.setReferenceEventId(message.getExecutionReferenceId());
    }

    protected void makeTradeEvent(Header header, FixExecutionReport message, Contract contract, MutableOrderTradeEvent event) {
        makeOrderEvent(header, message, contract, event);

        event.setMultiLegReportingType(FixUtil.getMultiLegReportingType(message.getMultiLegReportingType()));
        event.setTradePrice(toOrderPrice(message.getExecutionPrice(), contract));
        event.setTradeQuantity(toOrderQuantity(message.getExecutionQuantity(), contract));
        event.setTradeDate(message.getTradeDate());
        event.setSettlementDate(message.getSettlementDate());
        //setAggressorSide();
    }

    protected void makeOrderEvent(Header header, FixExecutionReport message, Contract contract, MutableOrderEvent event) {
        // NB: Account and ExchangeId are not extracted from ExecutionReport by default

        event.setSourceId(id);
        event.setEventId(message.getExecutionId());
        event.setExternalOrderId(message.getExternalOrderId());
        event.setOrderStatus(FixUtil.getOrderStatus(message.getOrderStatus()));
        event.setTimestamp(clock.time());
        event.setSequenceNumber(header.msgSeqNum());

        event.setSymbol(contract.getSymbol());
        event.setInstrumentType(contract.getSecurityType());
        event.setSide(FixUtil.getSide(message.getSide()));

        event.setQuantity(toOrderQuantity(message.getQuantity(), contract));
        event.setMinQuantity(toOrderQuantity(message.getMinQuantity(), contract));
        event.setDisplayQuantity(toOrderQuantity(message.getDisplayQuantity(), contract));
        event.setRemainingQuantity(toOrderQuantity(message.getRemainingQuantity(), contract));
        event.setCumulativeQuantity(toOrderQuantity(message.getCumulativeQuantity(), contract));

        event.setAveragePrice(toOrderPrice(message.getAveragePrice(), contract));
        event.setStopPrice(toOrderPrice(message.getStopPrice(), contract));
        event.setLimitPrice(toOrderPrice(message.getLimitPrice(), contract));
        event.setOrderType(FixUtil.getOrderType(message.getOrderType()));

        event.setTimeInForce(FixUtil.getTimeInForce(message.getTimeInForce()));
        event.setExpireTime(FixUtil.getExpireTime(message.getExpireTime(), message.getExpireDate()));

        event.setAttributes(null);
    }

    //endregion

    //region Receiving Cancel Reject

    protected void onCancelReject(Header header, Message message) {
        FixCancelReject reject = decode(fixCancelRejectCodec, fixCancelReject, message);
        byte rejectType = reject.getRejectType();

        switch (rejectType) {
            case CxlRejResponseTo.ORDER_CANCEL_REQUEST:
                onOrderCancelRejectMessage(header, reject, messages.orderCancelRejectEvent());
                break;

            case CxlRejResponseTo.ORDER_CANCEL_REPLACE_REQUEST:
                onOrderReplaceRejectMessage(header, reject, messages.orderReplaceRejectEvent());
                break;

            default:
                onCustomCancelRejectMessage(header, reject);
        }
    }

    protected void onOrderCancelRejectMessage(Header header, FixCancelReject message, MutableOrderCancelRejectEvent event) {
        makeOrderCancelRejectEvent(header, message, event);
        fireOrderCancelRejectEvent(event);
    }


    protected void onOrderReplaceRejectMessage(Header header, FixCancelReject message, MutableOrderReplaceRejectEvent event) {
        makeOrderReplaceRejectEvent(header, message, event);
        fireOrderReplaceRejectEvent(event);
    }

    @SuppressWarnings("UnusedParameters")
    protected void onCustomCancelRejectMessage(Header header, FixCancelReject message) {
        LOG.info("Connector %s skipping CancelReject(9) with msg seq num: %s")
                .with(name)
                .with(header.msgSeqNum());
    }

    protected void makeOrderCancelRejectEvent(Header header, FixCancelReject message, MutableOrderCancelRejectEvent event) {
        idMapper.messageToEvent(message, event);
        makeOrderEvent(header, message, event);

        event.setReason(message.getText());
        event.setVendorRejectCode(message.getRejectCode());
    }

    protected void makeOrderReplaceRejectEvent(Header header, FixCancelReject message, MutableOrderReplaceRejectEvent event) {
        idMapper.messageToEvent(message, event);
        makeOrderEvent(header, message, event);

        event.setReason(message.getText());
        event.setVendorRejectCode(message.getRejectCode());
    }

    protected void makeOrderEvent(Header header, FixCancelReject message, MutableOrderEvent event) {
        event.setSourceId(id);
        event.setExternalOrderId(message.getExternalOrderId());
        event.setOrderStatus(null);
        event.setTimestamp(clock.time());
        event.setSequenceNumber(header.msgSeqNum());

        event.setAttributes(null);
    }

    //endregion

    // region Receiving News Message

    @SuppressWarnings("UnusedParameters")
    protected void onNews(final Header header, final Message message) {
        final ByteSequence headline = message.getString(Tag.Headline, wrapper, null);

        LOG.info("Received News: %s")
                .with(headline);
    }

    // endregion

    // region Receiving Custom Message

    @SuppressWarnings("UnusedParameters")
    protected void onCustomMessage(final Header header, final Message message) {
        LOG.warn("Skipping app message with type: %s")
                .with(header.msgType());
    }

    // endregion

    //region Sending New Order Request

    @Override
    public void onNewOrderRequest(OrderNewRequest request) {
        try {
            sendNewOrderRequest(request);
        } catch (final Throwable e) {
            LOG.warn("New order request was rejected due to %s").with(e);

            MutableOrderRejectEvent event = messages.orderRejectEvent();
            Messages.makeOrderRejectEvent(clock.time(), e.getMessage(), request, event);
            fireOrderRejectEvent(event);
        }
    }

    protected void sendNewOrderRequest(OrderNewRequest request) {
        requestValidator.validateSubmit(request);

        CharSequence symbol = request.getSymbol();
        Contract contract = getContractBySymbol(symbol);

        FixNewOrderRequest message = reuse(fixNewOrderRequest);
        makeNewOrderMessage(request, contract, message);
        message.applyAttributes(request.getAttributes());

        sendAppMessageIfLeader(MsgType.ORDER_SINGLE, fixNewOrderRequestCodec, message);
    }

    protected void makeNewOrderMessage(OrderNewRequest request, Contract contract, FixNewOrderRequest message) {
        idMapper.requestToMessage(request, message);
        makeOrderRequestMessage(request, contract, message);
    }

    protected void makeOrderRequestMessage(OrderEntryRequest request, Contract contract, FixOrderStateMessage message) {
        // NB: request Account and ExchangeId are not set to message by default

        message.setCurrency(request.getCurrency());
        message.setSymbol(contract.getBrokerSymbol());
        message.setSecurityType(FixUtil.getSecurityType(contract.getSecurityType()));
        message.setSide(FixUtil.getSide(request.getSide()));
        message.setQuantity(toFixQuantity(request.getQuantity(), contract));
        message.setMinQuantity(toFixQuantity(request.getMinQuantity(), contract));
        message.setDisplayQuantity(toFixQuantity(request.getDisplayQuantity(), contract));

        message.setOrderType(FixUtil.getOrderType(request.getOrderType()));
        message.setLimitPrice(toFixPrice(request.getLimitPrice(), contract));
        message.setStopPrice(toFixPrice(request.getStopPrice(), contract));
        message.setQuoteId(request.getQuoteId());

        byte timeInForce = FixUtil.getTimeInForce(request.getTimeInForce());
        message.setTimeInForce(timeInForce);
        if (timeInForce == TimeInForce.GOOD_TILL_DATE) {
            message.setExpireTime(request.getExpireTime());
            message.setExpireDate(request.getExpireTime());
        }

        message.setTransactTime(clock.time());
    }

    //endregion

    //region Sending Cancel Order Request

    @Override
    public void onCancelOrderRequest(OrderCancelRequest request) {
        try {
            sendCancelOrderRequest(request);
        } catch (final Throwable e) {
            LOG.warn("Cancel order request was rejected due to %s").with(e);

            MutableOrderCancelRejectEvent event = messages.orderCancelRejectEvent();
            Messages.makeOrderCancelRejectEvent(clock.time(), e.getMessage(), request, event);
            fireOrderCancelRejectEvent(event);
        }
    }

    protected void sendCancelOrderRequest(OrderCancelRequest request) {
        requestValidator.validateCancel(request);

        CharSequence symbol = request.getSymbol();
        Contract contract = getContractBySymbol(symbol);

        FixCancelOrderRequest message = reuse(fixCancelOrderRequest);
        makeCancelOrderMessage(request, contract, message);
        message.applyAttributes(request.getAttributes());

        sendAppMessageIfLeader(MsgType.ORDER_CANCEL_REQUEST, fixCancelOrderRequestCodec, message);
    }

    protected void makeCancelOrderMessage(OrderCancelRequest request, Contract contract, FixCancelOrderRequest message) {
        idMapper.requestToMessage(request, message);
        message.setExternalOrderId(request.getExternalOrderId());

        message.setSymbol(contract.getBrokerSymbol());
        message.setRemainingQuantity(toFixQuantity(request.getQuantity(), contract));
        message.setSide(FixUtil.getSide(request.getSide()));
        message.setTransactTime(clock.time());
    }

    //endregion

    //region Sending Replace Order Request

    @Override
    public void onReplaceOrderRequest(OrderReplaceRequest request) {
        try {
            sendReplaceOrderRequest(request);
        } catch (final Throwable e) {
            LOG.warn("Replace order request was rejected due to %s").with(e);

            MutableOrderReplaceRejectEvent event = messages.orderReplaceRejectEvent();
            Messages.makeOrderReplaceRejectEvent(clock.time(), e.getMessage(), request, event);
            fireOrderReplaceRejectEvent(event);
        }
    }

    protected void sendReplaceOrderRequest(OrderReplaceRequest request) {
        requestValidator.validateModify(request);

        CharSequence symbol = request.getSymbol();
        Contract contract = getContractBySymbol(symbol);

        FixReplaceOrderRequest message = reuse(fixReplaceOrderRequest);
        makeReplaceOrderMessage(request, contract, message);
        message.applyAttributes(request.getAttributes());

        sendAppMessageIfLeader(MsgType.ORDER_CANCEL_REPLACE_REQUEST, fixReplaceOrderRequestCodec, message);
    }

    protected void makeReplaceOrderMessage(OrderReplaceRequest request, Contract contract, FixReplaceOrderRequest message) {
        idMapper.requestToMessage(request, message);
        message.setExternalOrderId(request.getExternalOrderId());
        makeOrderRequestMessage(request, contract, message);
    }

    //endregion

    //region Sending Order Status Request

    @Override
    public void onOrderStatusRequest(OrderStatusRequest request) {
        try {
            sendOrderStatusRequest(request);
        } catch (final Throwable e) {
            LOG.warn("Order status request was rejected due to %s").with(e);
        }
    }

    protected void sendOrderStatusRequest(OrderStatusRequest request) {
        CharSequence symbol = request.getSymbol();
        Contract contract = getContractBySymbol(symbol);

        FixOrderStatusRequest message = reuse(fixOrderStatusRequest);
        makeOrderStatusRequest(request, contract, message);

        sendAppMessageIfLeader(MsgType.ORDER_STATUS_REQUEST, fixOrderStatusRequestCodec, message);
    }

    protected void makeOrderStatusRequest(OrderStatusRequest request, Contract contract, FixOrderStatusRequest message) {
        idMapper.requestToMessage(request, message);
        message.setExternalOrderId(request.getExternalOrderId());
        message.setSymbol(contract.getBrokerSymbol());
        message.setTransactTime(clock.time());
    }

    //endregion

    //region Security Metadata

    @Override
    public void onBondUpdate(BondUpdate update) {
        metadata.onBondUpdate(update);
    }

    @Override
    public void onCurrencyUpdate(CurrencyUpdate update) {
        metadata.onCurrencyUpdate(update);
    }

    @Override
    public void onCustomInstrumentUpdate(CustomInstrumentUpdate update) {
        metadata.onCustomInstrumentUpdate(update);
    }

    @Override
    public void onEquityUpdate(EquityUpdate update) {
        metadata.onEquityUpdate(update);
    }

    @Override
    public void onEtfUpdate(EtfUpdate update) {
        metadata.onEtfUpdate(update);
    }

    @Override
    public void onFutureUpdate(FutureUpdate update) {
        metadata.onFutureUpdate(update);
    }

    @Override
    public void onIndexUpdate(IndexUpdate update) {
        metadata.onIndexUpdate(update);
    }

    @Override
    public void onOptionUpdate(OptionUpdate update) {
        metadata.onOptionUpdate(update);
    }

    @Override
    public void onSyntheticUpdate(SyntheticUpdate update) {
        metadata.onSyntheticUpdate(update);
    }

    protected Contract getContractBySymbol(CharSequence symbol) {
        Contract contract = metadata.getContractBySymbol(symbol);
        if (contract == null) {
            throw new IllegalArgumentException(String.format("Can't find contract by symbol \"%s\"", symbol));
        }

        return contract;
    }

    protected Contract getContract(FixExecutionReport message) {
        Contract contract = metadata.getContractByBrokerSymbol(message.getSymbol());
        if (contract == null) {
            throw new IllegalArgumentException(String.format("Can't find contract by broker symbol \"%s\"", message.getSymbol()));
        }

        return contract;
    }
    //endregion

    protected <M extends FixMessage> M decode(FixCodec<M> codec, M message, Message map) {
        //noinspection DataFlowIssue
        message = reuse(message);
        codec.decode(message, map);
        return message;
    }

    @Decimal
    protected long toFixQuantity(@Decimal long orderQuantity, Contract contract) {
        long quantity = orderQuantity;
        if (contract.hasQuantityMultiplier()) {
            quantity = Decimal64Utils.multiply(quantity, contract.getQuantityMultiplier());
        }

        if (contract.hasQuantityPrecision()) {
            quantity = Decimal64Utils.round(quantity, contract.getQuantityPrecision());
        }

        return quantity;
    }

    @Decimal
    protected long toOrderQuantity(@Decimal long fixQuantity, Contract contract) {
        return contract.hasQuantityMultiplier() ?
                Decimal64Utils.divide(fixQuantity, contract.getQuantityMultiplier()) :
                fixQuantity;
    }

    @Decimal
    protected long toFixPrice(@Decimal long orderPrice, Contract contract) {
        if (Decimal64Utils.isNaN(orderPrice))
            return orderPrice;

        long price = orderPrice;
        if (contract.hasPriceMultiplier()) {
            price = Decimal64Utils.multiply(price, contract.getPriceMultiplier());
        }

        if (contract.hasPricePrecision()) {
            price = Decimal64Utils.round(price, contract.getPricePrecision());
        }

        return price;
    }

    @Decimal
    protected long toOrderPrice(@Decimal long fixPrice, Contract contract) {
        if (Decimal64Utils.isNaN(fixPrice))
            return fixPrice;

        return contract.hasPriceMultiplier() ?
                Decimal64Utils.divide(fixPrice, contract.getPriceMultiplier()) :
                fixPrice;
    }

    // region Fire Ember Events

    protected void fireShutdownResponse() {
        if (nodeStatus == NodeStatus.LEADER) {
            final MutableShutdownResponse response = new MutableShutdownResponse();
            response.setServiceId(id);

            ember.onShutdownResponse(response);
        }
    }

    protected void fireSessionStatusEvent(final deltix.ember.message.trade.SessionStatus status) {
        if (nodeStatus == NodeStatus.LEADER) {
            final MutableSessionStatusEvent event = messages.sessionStatusEvent();
            event.setSourceId(id);
            event.setStatus(status);
            event.setTimestamp(clock.time());
            ember.onSessionStatusEvent(event);
        }
    }

    protected void fireOrderPendingNewEvent(final OrderPendingNewEvent event) {
        if (nodeStatus == NodeStatus.LEADER) {
            onOrderPendingNewEvent(event);
            ember.onOrderPendingNewEvent(event);
        }
    }

    protected void fireOrderNewEvent(final OrderNewEvent event) {
        if (nodeStatus == NodeStatus.LEADER) {
            onOrderNewEvent(event);
            ember.onOrderNewEvent(event);
        }
    }

    protected void fireOrderRejectEvent(final OrderRejectEvent event) {
        if (nodeStatus == NodeStatus.LEADER) {
            onOrderRejectEvent(event);
            ember.onOrderRejectEvent(event);
        }
    }

    protected void fireOrderPendingCancelEvent(final OrderPendingCancelEvent event) {
        if (nodeStatus == NodeStatus.LEADER) {
            onOrderPendingCancelEvent(event);
            ember.onOrderPendingCancelEvent(event);
        }
    }

    protected void fireOrderCancelEvent(final OrderCancelEvent event) {
        if (nodeStatus == NodeStatus.LEADER) {
            onOrderCancelEvent(event);
            ember.onOrderCancelEvent(event);
        }
    }

    protected void fireOrderCancelRejectEvent(final OrderCancelRejectEvent event) {
        if (nodeStatus == NodeStatus.LEADER) {
            onOrderCancelRejectEvent(event);
            ember.onOrderCancelRejectEvent(event);
        }
    }

    protected void fireOrderPendingReplaceEvent(final OrderPendingReplaceEvent event) {
        if (nodeStatus == NodeStatus.LEADER) {
            onOrderPendingReplaceEvent(event);
            ember.onOrderPendingReplaceEvent(event);
        }
    }

    protected void fireOrderReplaceEvent(final OrderReplaceEvent event) {
        if (nodeStatus == NodeStatus.LEADER) {
            onOrderReplaceEvent(event);
            ember.onOrderReplaceEvent(event);
        }
    }

    protected void fireOrderReplaceRejectEvent(final OrderReplaceRejectEvent event) {
        if (nodeStatus == NodeStatus.LEADER) {
            onOrderReplaceRejectEvent(event);
            ember.onOrderReplaceRejectEvent(event);
        }
    }

    protected void fireOrderTradeReportEvent(final OrderTradeReportEvent event) {
        if (nodeStatus == NodeStatus.LEADER) {
            onOrderTradeReportEvent(event);
            ember.onOrderTradeReportEvent(event);
        }
    }

    protected void fireOrderTradeCancelEvent(final OrderTradeCancelEvent event) {
        if (nodeStatus == NodeStatus.LEADER) {
            onOrderTradeCancelEvent(event);
            ember.onOrderTradeCancelEvent(event);
        }
    }

    protected void fireOrderTradeCorrectEvent(final OrderTradeCorrectEvent event) {
        if (nodeStatus == NodeStatus.LEADER) {
            onOrderTradeCorrectEvent(event);
            ember.onOrderTradeCorrectEvent(event);
        }
    }

    protected void fireOrderStatusEvent(final OrderStatusEvent event) {
        if (nodeStatus == NodeStatus.LEADER) {
            onOrderStatusEvent(event);
            ember.onOrderStatusEvent(event);
        }
    }

    @SuppressWarnings("unused")
    protected void fireOrderRestateEvent(final OrderRestateEvent event) {
        if (nodeStatus == NodeStatus.LEADER) {
            onOrderRestateEvent(event);
            ember.onOrderRestateEvent(event);
        }
    }

    // endregion

    // region Order Events

    @Override
    public void onOrderPendingNewEvent(final OrderPendingNewEvent event) {
        idMapper.onOrderPendingNewEvent(event);
    }

    @Override
    public void onOrderNewEvent(final OrderNewEvent event) {
        idMapper.onOrderNewEvent(event);
    }

    @Override
    public void onOrderRejectEvent(final OrderRejectEvent event) {
        idMapper.onOrderRejectEvent(event);
    }

    @Override
    public void onOrderPendingCancelEvent(final OrderPendingCancelEvent event) {
        idMapper.onOrderPendingCancelEvent(event);
    }

    @Override
    public void onOrderCancelEvent(final OrderCancelEvent event) {
        idMapper.onOrderCancelEvent(event);
    }

    @Override
    public void onOrderCancelRejectEvent(final OrderCancelRejectEvent event) {
        idMapper.onOrderCancelRejectEvent(event);
    }

    @Override
    public void onOrderPendingReplaceEvent(final OrderPendingReplaceEvent event) {
        idMapper.onOrderPendingReplaceEvent(event);
    }

    @Override
    public void onOrderReplaceEvent(final OrderReplaceEvent event) {
        idMapper.onOrderReplaceEvent(event);
    }

    @Override
    public void onOrderReplaceRejectEvent(final OrderReplaceRejectEvent event) {
        idMapper.onOrderReplaceRejectEvent(event);
    }

    @Override
    public void onOrderTradeReportEvent(final OrderTradeReportEvent event) {
        idMapper.onOrderTradeReportEvent(event);
    }

    @Override
    public void onOrderTradeCancelEvent(final OrderTradeCancelEvent event) {
        idMapper.onOrderTradeCancelEvent(event);
    }

    @Override
    public void onOrderTradeCorrectEvent(final OrderTradeCorrectEvent event) {
        idMapper.onOrderTradeCorrectEvent(event);
    }

    @Override
    public void onOrderStatusEvent(final OrderStatusEvent event) {
        idMapper.onOrderStatusEvent(event);
    }

    @Override
    public void onOrderRestateEvent(final OrderRestateEvent event) {
        idMapper.onOrderRestateEvent(event);
    }

    // endregion

    protected void logConnectionException(Throwable e) {
        LOG.warn("Connector %s has connection problem: %s")
                .with(name)
                .with(e.getMessage());
    }

    protected void logException(Throwable e) {
        LOG.warn("Connector %s has problem: %s")
                .with(name)
                .with(e);
    }

    protected static <T extends Reusable> T reuse(T reusable) {
        reusable.reuse();
        return reusable;
    }

    protected class RejectHandler implements MessageStore.Visitor {

        private Header header;
        private FixReject message;


        protected boolean onReject(Header header, FixReject message) {
            this.header = header;
            this.message = message;
            return store.read(message.getRefMsgSeqNum(), this);
        }

        @Override
        public void onMessage(int seqNum, long time, ByteSequence msgType, Buffer body, int offset, int length) {
            onMessageReject(header, message, msgType, body, offset, length);
        }

    }
}
