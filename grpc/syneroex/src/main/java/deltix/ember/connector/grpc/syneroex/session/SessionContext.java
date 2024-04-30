package deltix.ember.connector.grpc.syneroex.session;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import deltix.anvil.util.Factory;
import deltix.anvil.util.clock.EpochClock;
import deltix.anvil.util.clock.SystemEpochClock;
import deltix.efix.endpoint.log.EmptyMessageLog;
import deltix.efix.endpoint.log.MessageLog;
import deltix.efix.schedule.ContinuousSessionSchedule;
import deltix.efix.schedule.SessionSchedule;
import io.grpc.*;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;

import static java.lang.Integer.getInteger;

public class SessionContext {

    private static final String INBOUND_PING_TIMEOUT_PROP_NAME = "syneroex.inbound.ping.timeout";
    private static final String OUTBOUND_PING_INTERVAL_PROP_NAME = "syneroex.outbound.ping.interval";

    private static final String CONNECT_TIMEOUT_PROP_NAME    = "syneroex.connect.timeout";
    private static final String DISCONNECT_TIMEOUT_PROP_NAME = "syneroex.disconnect.timeout";

    private static final Log LOG = LogFactory.getLog(Session.class);

    private final String host;
    private final int port;
    private final String username;
    private final String password;

    private File CAfile;

    private int queueSize = 4 * 1024;
    private int maxInboundMessageSize = 0; // zero means that gRPC default value will be used
    private int keepAliveTime = 0;  // zero means that gRPC default value will be used

    private Log logger = LOG;
    private EpochClock clock = SystemEpochClock.INSTANCE;

    private SessionControl control;

    private SessionSchedule schedule = ContinuousSessionSchedule.INSTANCE;
    private SessionState state = new SessionState();
    private MessageLog log = EmptyMessageLog.INSTANCE;

    private Factory<ManagedChannel> channelFactory;

    private int inboundPingTimeout = getInteger(INBOUND_PING_TIMEOUT_PROP_NAME, 10_000);
    private int outboundPingInterval = getInteger(OUTBOUND_PING_INTERVAL_PROP_NAME, 10_000);

    private int connectTimeout = getInteger(CONNECT_TIMEOUT_PROP_NAME, 10_000);
    private int disconnectTimeout = getInteger(DISCONNECT_TIMEOUT_PROP_NAME, 5_000);
    private int reconnectInterval = 10_000; // 10 sec

    private boolean logHeartbeat = false;


    public SessionContext(String host, int port,
                          String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public void conclude() {
        if (channelFactory == null) {
            channelFactory = () -> {
                final ManagedChannelBuilder<?> channelBuilder = Grpc.newChannelBuilderForAddress(host(), port(), createChannelCredentials(CAfile()));
                if (maxInboundMessageSize() > 0) {
                    channelBuilder.maxInboundMessageSize(maxInboundMessageSize());
                }
                if (keepAliveTime() > 0) {
                    channelBuilder.keepAliveTime(keepAliveTime(), TimeUnit.MILLISECONDS);
                }
                return channelBuilder.build();
            };
        }
    }

    private static ChannelCredentials createChannelCredentials(File cafile) {
        try {
            return cafile != null ? TlsChannelCredentials.newBuilder().trustManager(cafile).build() : TlsChannelCredentials.create();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // region Getters and Setters

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public File CAfile() {
        return CAfile;
    }

    public SessionContext CAfile(File CAfile) {
        this.CAfile = CAfile;
        return this;
    }

    public boolean logHeartbeat() {
        return logHeartbeat;
    }

    public SessionContext logHeartbeat(boolean logHeartbeat) {
        this.logHeartbeat = logHeartbeat;
        return this;
    }

    public int reconnectInterval() {
        return reconnectInterval;
    }

    public SessionContext reconnectInterval(int reconnectInterval) {
        this.reconnectInterval = reconnectInterval;
        return this;
    }

    public int queueSize() {
        return queueSize;
    }

    public SessionContext queueSize(int queueSize) {
        this.queueSize = queueSize;
        return this;
    }

    public int maxInboundMessageSize() {
        return maxInboundMessageSize;
    }

    public SessionContext maxInboundMessageSize(int maxInboundMessageSize) {
        this.maxInboundMessageSize = maxInboundMessageSize;
        return this;
    }

    public int keepAliveTime() {
        return keepAliveTime;
    }

    public SessionContext keepAliveTime(int keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
        return this;
    }

    public Log logger() {
        return logger;
    }

    public SessionContext logger(Log logger) {
        this.logger = logger;
        return this;
    }

    public SessionControl control() {
        return control;
    }

    public SessionContext control(SessionControl control) {
        this.control = control;
        return this;
    }

    public EpochClock clock() {
        return clock;
    }

    public SessionContext clock(EpochClock clock) {
        this.clock = clock;
        return this;
    }

    public SessionSchedule schedule() {
        return schedule;
    }

    public SessionContext schedule(SessionSchedule schedule) {
        this.schedule = schedule;
        return this;
    }

    public SessionState state() {
        return state;
    }

    public SessionContext state(SessionState state) {
        this.state = state;
        return this;
    }

    public MessageLog log() {
        return log;
    }

    public SessionContext log(MessageLog log) {
        this.log = log;
        return this;
    }

    public Factory<ManagedChannel> channelFactory() {
        return channelFactory;
    }

    public SessionContext setChannelFactory(Factory<ManagedChannel> channelFactory) {
        this.channelFactory = channelFactory;
        return this;
    }

    public int inboundPingTimeout() {
        return inboundPingTimeout;
    }

    public SessionContext inboundPingTimeout(int inboundPingInterval) {
        this.inboundPingTimeout = inboundPingInterval;
        return this;
    }

    public int outboundPingInterval() {
        return outboundPingInterval;
    }

    public SessionContext outboundPingInterval(int heartbeatInterval) {
        this.outboundPingInterval = heartbeatInterval;
        return this;
    }

    public int connectTimeout() {
        return connectTimeout;
    }

    public SessionContext connectTimeout(int logonTimeout) {
        this.connectTimeout = logonTimeout;
        return this;
    }

    public int disconnectTimeout() {
        return disconnectTimeout;
    }

    public SessionContext disconnectTimeout(int logoutTimeout) {
        this.disconnectTimeout = logoutTimeout;
        return this;
    }

    // endregion
}
