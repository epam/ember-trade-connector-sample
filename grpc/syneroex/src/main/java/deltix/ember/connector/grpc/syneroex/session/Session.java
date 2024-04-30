package deltix.ember.connector.grpc.syneroex.session;

import com.epam.deltix.gflog.api.Log;
import com.google.protobuf.Message;
import com.syneroex.*;
import deltix.anvil.util.CloseHelper;
import deltix.anvil.util.Disposable;
import deltix.anvil.util.TimeoutException;
import deltix.anvil.util.annotation.Timestamp;
import deltix.anvil.util.clock.EpochClock;
import deltix.efix.endpoint.SessionComponent;
import deltix.efix.endpoint.connector.ConnectionException;
import deltix.efix.endpoint.log.MessageLog;
import deltix.efix.schedule.SessionSchedule;
import deltix.ember.connector.grpc.syneroex.SyneroexUtil;
import deltix.ember.connector.grpc.syneroex.util.SyneroexMessage;
import deltix.util.collections.ReusableObjectPool;
import deltix.util.lang.Util;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;


public class Session implements Disposable {

    private final ArrayBlockingQueue<Message> eventQueue;

    private final SessionContext context;
    private final SessionControl control;
    private final SyneroexListener listener;
    private final Log log;

    private final EpochClock clock;
    private final SessionState state;
    private final MessageLog messageLog;
    private final SessionSchedule schedule;

    private final int inboundPingTimeout;
    private final int outboundPingInterval;
    private final int connectTimeout;
    private final int disconnectTimeout;

    private final MessageFormatter formatter = new MessageFormatter();
    private final ArrayList<Disposable> openResources = new ArrayList<>();
    private final ReusableObjectPool<RequestAnswer> requestAnswerPool = ReusableObjectPool.synchronizedPool(ReusableObjectPool.create(RequestAnswer::new,8));

    private boolean keepSessionAlive = false;
    private volatile boolean closing = false;
    private boolean enabled = true;

    private ManagedChannel managedChannel;
    private AuthenticationServiceGrpc.AuthenticationServiceStub authenticationServiceStub;
    private OrderServiceGrpc.OrderServiceStub orderServiceStub;

    public Session(SessionContext context, SyneroexListener listener) {
        context.conclude();

        this.listener = listener;
        this.eventQueue = new ArrayBlockingQueue<>(context.queueSize());

        this.context = context;
        this.control = context.control();
        this.log = context.logger();

        this.clock = context.clock();
        this.state = context.state();
        this.messageLog = context.log();
        this.schedule = context.schedule();

        this.inboundPingTimeout = context.inboundPingTimeout();
        this.outboundPingInterval = context.outboundPingInterval();
        this.connectTimeout = context.connectTimeout();
        this.disconnectTimeout = context.disconnectTimeout();
    }

    // region Connectivity

    @Override
    public void open() {
        try {
            doOpen();
        } catch (Exception e) {
            processError(e);
            throw e;
        }
    }

    @Override
    public void close() {
        try {
            doClose();
        } catch (Exception e) {
            processError(e);
            throw e;
        }
    }

    public int work() {
        int work = doWork();
        if (work <= 0) {
            flush();
        }

        return work;
    }

    public boolean isDisconnected() {
        return state.status() == SessionStatus.DISCONNECTED;
    }

    public boolean active() {
        return (state.status() != SessionStatus.DISCONNECTED) || !closing;
    }

    public void deactivate() {
        closing = true;
    }

    private void doOpen() {
        Disposable[] resources = {state, messageLog};
        for (Disposable resource : resources) {
            resource.open();
            openResources.add(resource);
        }

        createChannel();

        log.info().append(state).commit();
    }

    private void doClose() {
        try {
            shutdownChannel();
            CloseHelper.close(openResources);
        } finally {
            openResources.clear();
        }
    }

    private int doWork() {
        int work = 0;

        long now = clock.time();

        work += checkSession(now);
        work += receiveMessages(now);
        work += processTimers(now);

        return work;
    }

    private void flush() {
        flush(state);
        flush(messageLog);
    }

    private void flush(SessionComponent component) {
        try {
            component.flush();
        } catch (Exception e) {
            processError(e);
        }
    }

    private void connect() {
        log.info("Connect session");

        // initiate authentication
        authenticate();
    }

    private void disconnect(final CharSequence cause) {
        log.warn("Disconnect session: %s").with(cause);

        // initiate disconnect
        if (state.status() == SessionStatus.APPLICATION_CONNECTED) {
            unsubscribe();
        } else {
            updateStatus(SessionStatus.DISCONNECTED);
        }
    }

    private void createChannel() {
        log.debug("Open gRPC channel");

        managedChannel = context.channelFactory().create();

        authenticationServiceStub = AuthenticationServiceGrpc.newStub(managedChannel);
        orderServiceStub = OrderServiceGrpc.newStub(managedChannel);
    }

    private void shutdownChannel() {
        log.debug("Shutdown gRPC channel");

        try {
            managedChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Channel shutdown has been interrupted:\n%s").with(e);
        } finally {
            managedChannel = null;

            authenticationServiceStub = null;
            orderServiceStub = null;
        }
    }

    private void applyToken(String token) {
        Metadata tokenMetadata = new Metadata();
        tokenMetadata.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer " + token);

        orderServiceStub = OrderServiceGrpc.newStub(managedChannel).withInterceptors(MetadataUtils.newAttachHeadersInterceptor(tokenMetadata));
    }

    // endregion

    // region Command & Control

    private void enable() {
        enabled = true;
        control.updateEnabled(true);
    }

    private void disable() {
        enabled = false;
        control.updateEnabled(false);
    }

    public boolean keepSessionAlive() {
        return keepSessionAlive;
    }

    public void keepSessionAlive(boolean keepSessionAlive) {
        this.keepSessionAlive = keepSessionAlive;
    }

    // endregion

    private int checkSession(@Timestamp final long now) {
        int work = 0;

        try {
            if (state.status() == SessionStatus.DISCONNECTED) {
                work += checkSessionStart(now);
            } else {
                work += checkSessionEnd(now);
            }
        } catch (Exception e) {
            work += 1;
            processError(e);
        }

        return work;
    }

    private int receiveMessages(@Timestamp final long now) {
        int work = 0;

        if (state.status() != SessionStatus.DISCONNECTED) {
            try {
                work += processMessageQueue();
            } catch (Exception e) {
                work += 1;
                processError(e);
            }
        }

        return work;
    }

    private int processTimers(@Timestamp final long now) {
        int work = 0;

        try {
            SessionStatus status = state.status();
            if (status == SessionStatus.APPLICATION_CONNECTED) {
                work += checkInPingTimeout(now);
                work += checkOutPingTimeout(now);
            } else if (status.isConnecting()) {
                work += checkConnectTimeout(now);
            } else if (status.isDisconnecting()) {
                work += checkDisconnectTimeout(now);
            }
        } catch (Exception e) {
            work += 1;
            processError(e);
        }

        return work;
    }

    private int checkSessionStart(@Timestamp final long now) {
        int work = 0;
        final long start = schedule.getStartTime(now);
        final long lastSessionStart = state.sessionStartTime();

        if (keepSessionAlive && enabled && now >= start && !closing) {
            // have enough time passed from last attempt?
            if (now >= (lastSessionStart + context.reconnectInterval())) {
                // initiate connect
                connect();

                state.sessionStartTime(now);

                work += 1;
            }
        }

        return work;
    }

    private int checkSessionEnd(@Timestamp final long now) {
        int work = 0;

        final long end = schedule.getEndTime(state.sessionStartTime());
        if (!keepSessionAlive || !enabled || now >= end || closing) {
            final SessionStatus status = state.status();
            if (status != SessionStatus.DISCONNECTED && status != SessionStatus.UNSUBSCRIBE_SENT) {
                disconnect("Session end");
                work += 1;
            }
        }

        return work;
    }

    private int checkConnectTimeout(@Timestamp final long now) {
        int work = 0;
        long elapsed = now - state.sessionStartTime();
        if (elapsed >= connectTimeout) {
            throw new TimeoutException(String.format("Connect timeout %s ms. Elapsed %s ms", connectTimeout, elapsed));
        }

        return work;
    }

    private int checkDisconnectTimeout(@Timestamp final long now) {
        int work = 0;
        long elapsed = now - state.lastSentTime();
        if (elapsed >= disconnectTimeout) {
            throw new TimeoutException(String.format("Disconnect timeout %s ms. Elapsed %s ms", disconnectTimeout, elapsed));
        }
        return work;
    }

    private int checkInPingTimeout(@Timestamp final long now) {
        int work = 0;
        long elapsed = now - state.lastReceivedTime();
        if (elapsed >= inboundPingTimeout) {
            throw new TimeoutException(String.format("Heartbeat timeout: %s ms. Elapsed time since last received message %s ms", inboundPingTimeout, elapsed));
        }

        return work;
    }

    private int checkOutPingTimeout(@Timestamp final long now) {
        int work = 0;
        long elapsed = now - state.lastSentTime();
        if (elapsed >= outboundPingInterval) {
            sendPing();
            work += 1;
        }

        return work;
    }

    // region Process Message

    private void addToMessageQueue(final Message message) {
        try {
            eventQueue.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("InterruptedException during event queue.put() (queue overflow)", e);
        }
    }

    public int processMessageQueue() {
        int workCount = 0;
        Message message;
        while ((message = eventQueue.poll()) != null) {
            processMessage(message);
            workCount++;
        }
        return workCount;
    }

    private void processMessage(Message message) {
        final long time = clock.time();
        state.lastReceivedTime(time);

        onMessage(message);
    }

    private void onMessage(Message message) {
        if (message instanceof OrderResponse) { // order event
            logMessage(true, message);

            if (state.status().isConnecting()) {
                updateStatus(SessionStatus.APPLICATION_CONNECTED);
            }

            final OrderResponse response = (OrderResponse) message;
            listener.onAppMessage(response);
        } else if (message instanceof RequestAnswer) { // request/response
            final RequestAnswer answer = (RequestAnswer) message;
            try {
                // log inbound message if succeeded
                final Message response = answer.response();
                if (response != null) {
                    logMessage(true, response);
                }

                handleRequestAnswer(answer);
            } finally {
                requestAnswerPool.release(answer);
            }
        } else { // unknown message
            logMessage(true, message);
        }
    }

    private void handleRequestAnswer(RequestAnswer answer) {
        switch (answer.requestType()) {
            case CREATE_ORDER:
                listener.onCreateOrder((CreateOrdersRequest) answer.request(), (CreateOrdersResponse) answer.response(), answer);
                break;
            case CANCEL_ORDER:
                listener.onCancelOrder((CancelOrdersRequest) answer.request(), (CancelOrdersResponse) answer.response(), answer);
                break;
            case REPLACE_ORDER:
                listener.onReplaceOrder((ReplaceOrdersRequest) answer.request(), (ReplaceOrdersResponse) answer.response(), answer);
                break;
            case QUERY_ORDER:
                listener.onQueryOrder((OrderQueryRequest) answer.request(), (OrderQueryResponse) answer.response(), answer);
                break;
            case AUTHENTICATE:
                if (answer.isError()) {
                    updateStatus(SessionStatus.AUTH_REJECTED);
                    disconnect(answer.message());
                } else {
                    final AuthenticationResponse response = (AuthenticationResponse) answer.response();
                    if (response.getSuccess()) {
                        // update status
                        updateStatus(SessionStatus.AUTH_ACKNOWLEDGED);
                        // subscribe for order events
                        subscribe();
                    } else {
                        updateStatus(SessionStatus.AUTH_REJECTED);
                        disconnect(answer.message());
                    }
                }
                break;
            case SUBSCRIBE:
                if (answer.isError()) {
                    updateStatus(SessionStatus.SUBSCRIBE_REJECTED);
                    disconnect(answer.message());
                }
                break;
            case UNSUBSCRIBE:
                if (answer.isError()) {
                    updateStatus(SessionStatus.UNSUBSCRIBE_REJECTED);
                    disconnect(answer.message());
                } else {
                    disconnect("Unsubscribe Ack");
                }
                break;
        }
    }

    // endregion

    // region Send Session Messages

    private void authenticate() {
        state.lastSentTime(clock.time());

        final AuthenticationRequest authRequest =
                AuthenticationRequest.newBuilder().setUser(context.username()).setPassword(context.password()).build();
        authenticationServiceStub.authenticate(authRequest, new RequestAnswerObserver<>(RequestType.AUTHENTICATE, null));

        logMessage(false, authRequest);

        updateStatus(SessionStatus.AUTH_SENT);
    }

    private void subscribe() {
        state.lastSentTime(clock.time());

        state.lastSubscriptionName(String.valueOf(state.lastSentTime()));

        final SubscriptionRequest subscribeRequest = SubscriptionRequest.newBuilder()
                .setName(state.lastSubscriptionName())
                .build();
        orderServiceStub.subscribe(subscribeRequest, new SubscribeObserver());

        logMessage(false, subscribeRequest);

        updateStatus(SessionStatus.SUBSCRIBE_SENT);
    }

    private void unsubscribe() {
        state.lastSentTime(clock.time());

        final SubscriptionRequest unsubscribeRequest = SubscriptionRequest.newBuilder()
                .setName(state.lastSubscriptionName())
                .build();
        orderServiceStub.unsubscribe(unsubscribeRequest, new RequestAnswerObserver<>(RequestType.UNSUBSCRIBE, null));

        logMessage(false, unsubscribeRequest);

        updateStatus(SessionStatus.UNSUBSCRIBE_SENT);
    }

    private void sendPing() {
        state.lastSentTime(clock.time());

        final PingRequest ping = PingRequest.getDefaultInstance();
        orderServiceStub.ping(ping, new RequestAnswerObserver<>(RequestType.PING, null));

        logMessage(false, ping);
    }

    // endregion

    // region Send App Messages

    public void createOrder(CreateOrdersRequest request) {
        state.lastSentTime(clock.time());

        orderServiceStub.submit(request, new RequestAnswerObserver<>(RequestType.CREATE_ORDER, request));

        logMessage(false, request);
    }

    public void replaceOrder(ReplaceOrdersRequest request) {
        state.lastSentTime(clock.time());

        orderServiceStub.replace(request, new RequestAnswerObserver<>(RequestType.REPLACE_ORDER, request));

        logMessage(false, request);
    }

    public void cancelOrder(CancelOrdersRequest request) {
        state.lastSentTime(clock.time());

        orderServiceStub.cancel(request, new RequestAnswerObserver<>(RequestType.CANCEL_ORDER, request));

        logMessage(false, request);
    }

    public void queryOrder(OrderQueryRequest request) {
        state.lastSentTime(clock.time());

        orderServiceStub.queryOrder(request, new RequestAnswerObserver<>(RequestType.QUERY_ORDER, request));

        logMessage(false, request);
    }

    // endregion

    // region Utilities

    private void updateStatus(SessionStatus status) {
        SessionStatus old = state.status();
        state.status(status);
        onStatusUpdate(old, status);
    }

    private void processError(Throwable e) {
        final Throwable unwrapped = Util.unwrap(e);
        if (unwrapped instanceof ConnectionException ||
            unwrapped instanceof UnknownHostException ||
            unwrapped instanceof TimeoutException) {

            if (state.status() != SessionStatus.DISCONNECTED) {
                disconnect(e.getMessage()); // force shutdown
            }
        }

        log.error("Error occurred: %s").with(e);
    }

    private void onStatusUpdate(SessionStatus previous, SessionStatus current) {
        log.info("Session status changed: %s -> %s").with(previous).with(current);

        if (previous != current) {
            if (current == SessionStatus.APPLICATION_CONNECTED) {
                listener.onConnected();
            } else if (current == SessionStatus.DISCONNECTED) {
                listener.onDisconnected();
            }
        }
    }

    private RequestAnswer borrowAnswer(RequestType requestType, Message request) {
        final RequestAnswer answer = requestAnswerPool.borrow();
        answer.reuse();
        return answer.requestType(requestType).request(request);
    }

    private void logMessage(boolean inbound, Message message) {
        if (log.isTraceEnabled()) {
            log.trace("(%s) Class: %s\n%s").with(inbound ? "INB" : "OUT").with(message.getClass().getSimpleName()).with(SyneroexUtil.messageToString(message));
        }
    }

    // endregion

    // region Helper Classes

    private final class RequestAnswerObserver<T extends Message> implements StreamObserver<T> {
        private final RequestType requestType;
        private final Message request;

        public RequestAnswerObserver(RequestType requestType, Message request) {
            this.requestType = requestType;
            this.request = request;
        }

        @Override
        public void onNext(T value) {
            addToMessageQueue(borrowAnswer(requestType, request).response(value));
        }

        @Override
        public void onError(Throwable t) {
            log.error("[%s] failure: %s\n%s").with(requestType).with(t.getMessage()).with(t);
            addToMessageQueue(borrowAnswer(requestType, request).error(t));
        }

        @Override
        public void onCompleted() {
            log.trace("onCompleted: %s").with(requestType);
        }
    }

    private final class SubscribeObserver implements StreamObserver<OrderResponse> {
        @Override
        public void onNext(OrderResponse message) {
            addToMessageQueue(message);
        }

        @Override
        public void onError(Throwable t) {
            addToMessageQueue(borrowAnswer(RequestType.SUBSCRIBE, null).error(t));
        }

        @Override
        public void onCompleted() {
            addToMessageQueue(borrowAnswer(RequestType.UNSUBSCRIBE, null));
        }
    }

    // endregion

    // region Helper Messages

    private enum RequestType {
        CREATE_ORDER, CANCEL_ORDER, REPLACE_ORDER, QUERY_ORDER,
        AUTHENTICATE, SUBSCRIBE, UNSUBSCRIBE, PING,
        LIST_SECURITIES
    }

    private static final class RequestAnswer extends SyneroexMessage {
        private RequestType requestType;
        private Message request;
        private Message response;

        public RequestAnswer() {
            reuse();
        }

        @Override
        public void reuse() {
            super.reuse();
            this.requestType = null;
            this.request = null;
            this.response = null;
        }

        public boolean isError() {
            return throwable() != null;
        }

        @Override
        public RequestAnswer error(Throwable throwable) {
            return (RequestAnswer) super.error(throwable);
        }

        public RequestType requestType() {
            return requestType;
        }

        public RequestAnswer requestType(RequestType requestType) {
            this.requestType = requestType;
            return this;
        }

        public Message request() {
            return request;
        }

        public RequestAnswer request(Message request) {
            this.request = request;
            return this;
        }

        public Message response() {
            return response;
        }

        public RequestAnswer response(Message response) {
            this.response = response;
            return this;
        }
    }

    // endregion
}
