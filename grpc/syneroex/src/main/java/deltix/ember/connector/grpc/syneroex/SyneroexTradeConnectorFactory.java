package deltix.ember.connector.grpc.syneroex;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.gflog.api.LogLevel;
import deltix.anvil.util.AsciiStringBuilder;
import deltix.anvil.util.TypeConstants;
import deltix.anvil.util.annotation.Duration;
import deltix.anvil.util.annotation.Hashed;
import deltix.anvil.util.annotation.Optional;
import deltix.anvil.util.annotation.Required;
import deltix.anvil.util.buffer.Buffer;
import deltix.anvil.util.clock.EpochClock;
import deltix.connector.common.core.BaseTradeConnectorSettings;
import deltix.connector.common.smd.CommonContractProcessor;
import deltix.connector.common.smd.ContractMetadata;
import deltix.efix.endpoint.log.DailyRollingFileMessageLog;
import deltix.efix.endpoint.log.EmptyMessageLog;
import deltix.efix.endpoint.log.MessageLog;
import deltix.efix.endpoint.log.filter.Filter;
import deltix.efix.endpoint.log.layout.TimeLayout;
import deltix.efix.schedule.AgileSessionSchedule;
import deltix.efix.schedule.ContinuousSessionSchedule;
import deltix.efix.schedule.SessionSchedule;
import deltix.ember.connector.grpc.syneroex.config.GrpcSettings;
import deltix.ember.connector.grpc.syneroex.config.LogSettings;
import deltix.ember.connector.grpc.syneroex.config.Schedule;
import deltix.ember.connector.grpc.syneroex.session.SessionContext;
import deltix.ember.connector.grpc.syneroex.session.SessionState;
import deltix.ember.service.connector.TradeConnector;
import deltix.ember.service.connector.TradeConnectorContext;
import deltix.ember.service.connector.TradeConnectorFactory;

import java.io.File;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class SyneroexTradeConnectorFactory extends BaseTradeConnectorSettings implements TradeConnectorFactory {
    protected static final String LOGGER_NAME = "ember.connector.syneroex";
    private static final Log LOG = LogFactory.getLog(LOGGER_NAME);

    private @Optional LogSettings log = new LogSettings();
    private @Optional GrpcSettings grpc = new GrpcSettings();
    private @Optional Schedule schedule;

    private @Required String host;
    private @Required int port;

    private @Required String username;
    private @Required @Hashed String password;

    private @Optional int queueSize = 2 * 1024;
    private @Optional @Duration int reconnectInterval = 10_000;
    private @Optional @Duration int connectTimeout = 30_000;

    private @Optional String CAfile;

    @Override
    public TradeConnector create(final TradeConnectorContext connectorContext) {
        return new SyneroexTradeConnector(connectorContext,
                                        new ContractMetadata<>(new CommonContractProcessor(attributeKey)),
                                        createContext(connectorContext),
                                        createSessionContext(connectorContext));
    }

    protected SyneroexContext createContext(final TradeConnectorContext connectorContext) {
        return new SyneroexContext(attributeKey, workDir, TypeConstants.ALPHANUMERIC_NULL);
    }

    protected SessionContext createSessionContext(final TradeConnectorContext connectorContext) {
        final EpochClock clock = connectorContext.getEpochClock();

        final MessageLog messageLog = createMessageLog();
        final SessionState sessionState = createSessionState();
        final SessionSchedule sessionSchedule = createSessionSchedule();

        return new SessionContext(host, port, username, password)
                .clock(clock)
                .log(messageLog)
                .state(sessionState)
                .CAfile(CAfile != null ? new File(CAfile) : null)
                .reconnectInterval(reconnectInterval)
                .connectTimeout(connectTimeout)
                .queueSize(queueSize)
                .logger(LOG)
                .schedule(sessionSchedule)
                .maxInboundMessageSize(grpc.getMaxInboundMessageSize());
    }

    private MessageLog createMessageLog() {
        if (log.getLogFile() == null) {
            return EmptyMessageLog.INSTANCE;
        }

        final Path logFilePath = workDir.resolve(log.getLogFile());
        return new DailyRollingFileMessageLog(log.getLogBufferSize(), logFilePath, new BufferSizeFilter(log.getLogBufferSize()), new TimeLayout(), log.getLogMaxFiles(), log.getLogMaxFileSize());
    }

    private SessionSchedule createSessionSchedule() {
        SessionSchedule sessionSchedule = ContinuousSessionSchedule.INSTANCE;

        if (schedule != null) {
            final ZoneId zoneId = schedule.getZoneId();
            final List<AgileSessionSchedule.Interval> intervals = new ArrayList<>();

            for (final Schedule.Interval interval : schedule.getIntervals()) {
                final LocalTime startTime = interval.getStartTime();
                final LocalTime endTime = interval.getEndTime();

                final DayOfWeek startDay = interval.getStartDay();
                final DayOfWeek endDay = interval.getEndDay();

                intervals.add(new AgileSessionSchedule.Interval(startTime, endTime, startDay, endDay));
            }

            sessionSchedule = new AgileSessionSchedule(zoneId, intervals.toArray(new AgileSessionSchedule.Interval[0]));
        }

        return sessionSchedule;
    }

    private SessionState createSessionState() {
        return new SessionState();
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public void setReconnectInterval(int reconnectInterval) {
        this.reconnectInterval = reconnectInterval;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setCAfile(String CAfile) {
        this.CAfile = CAfile;
    }

    // region Helper Classes

    private static final class BufferSizeFilter implements Filter {
        private final AsciiStringBuilder builder = new AsciiStringBuilder(8 * 1024);
        private final int bufferSize;

        public BufferSizeFilter(int bufferSize) {
            this.bufferSize = bufferSize;
        }

        @Override
        public boolean filter(boolean inbound, long time, Buffer message, int offset, int length) {
            if (length >= bufferSize) {
                builder.clear().append(message.byteArray(), offset, length);
                LOG.log(LogLevel.WARN).append("Message size (").append(length).append(") exceed log buffer size (").append(bufferSize).append(")\n\t").appendLast(builder);
                return true;
            }
            return false;
        }
    }

    // endregion
}
