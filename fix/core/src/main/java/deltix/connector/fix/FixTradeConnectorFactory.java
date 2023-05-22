package deltix.connector.fix;

import deltix.anvil.util.annotation.Alphanumeric;
import deltix.anvil.util.clock.EpochClock;
import deltix.anvil.util.counter.CounterFactory;
import deltix.efix.FixVersion;
import deltix.efix.SessionId;
import deltix.efix.SessionType;
import deltix.efix.endpoint.control.EndpointSessionControl;
import deltix.efix.endpoint.log.DailyRollingFileMessageLog;
import deltix.efix.endpoint.log.MessageLog;
import deltix.efix.endpoint.log.filter.Filter;
import deltix.efix.endpoint.log.filter.HeartbeatFilter;
import deltix.efix.endpoint.session.SessionContext;
import deltix.efix.endpoint.state.MappedSessionState;
import deltix.efix.endpoint.state.SessionState;
import deltix.efix.endpoint.store.MessageStore;
import deltix.efix.schedule.AgileSessionSchedule;
import deltix.efix.schedule.ContinuousSessionSchedule;
import deltix.efix.schedule.SessionSchedule;
import deltix.ember.service.connector.TradeConnectorContext;
import deltix.ember.service.connector.TradeConnectorFactory;
import deltix.ember.service.connector.TradeConnectorTag;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


@SuppressWarnings("rawtypes")
public abstract class FixTradeConnectorFactory<T extends FixTradeConnector> extends FixTradeConnectorSettings implements TradeConnectorFactory {

    public static final int MAX_HEARTBEAT_DELAY = 15_000;
    protected final FixVersion fixVersion;

    protected FixTradeConnectorFactory(final FixVersion fixVersion) {
        this.fixVersion = fixVersion;
    }

    @Override
    public final T create(final TradeConnectorContext connectorContext) {
        final SessionContext sessionContext = createSessionContext(connectorContext);
        installControl(connectorContext, sessionContext);

        final T connector = create(connectorContext, sessionContext, getAttributeKey());
        installAgent(connectorContext, connector);

        return connector;
    }

    protected abstract T create(final TradeConnectorContext connectorContext,
                                final SessionContext sessionContext, final String attributeKey);


    // region Session Context
    protected SessionContext createSessionContext(final TradeConnectorContext connectorContext) {
        final SessionId sessionId = new SessionId(
                senderCompId,
                senderSubId,
                targetCompId,
                targetSubId
        );

        final EpochClock clock = connectorContext.getEpochClock();

        final MessageLog messageLog = createMessageLog();
        final SessionState sessionState = createSessionState();
        final MessageStore messageStore = createMessageStore();
        final SessionSchedule sessionSchedule = createSessionSchedule();

        return new SessionContext(host, port, SessionType.INITIATOR, fixVersion, sessionId)
                .logonTimeout(logonTimeout)
                .logoutTimeout(logoutTimeout)
                .resetSeqNumsOnLogon(resetSeqNums)
                .clock(clock)
                .log(messageLog)
                .state(sessionState)
                .store(messageStore)
                .schedule(sessionSchedule)
                .maxHeartbeatDelay(MAX_HEARTBEAT_DELAY);
    }

    protected MessageLog createMessageLog() {
        if (logFile == null) {
            return null;
        }

        final Path logFilePath = workDir.resolve(logFile);
        final Filter filter = logHeartbeat ? Filter.ALL_PASS : new HeartbeatFilter();

        return new DailyRollingFileMessageLog(logBufferSize, logFilePath, filter);
    }

    protected SessionState createSessionState() {
        if (stateFile == null) {
            return null;
        }

        final Path stateFilePath = workDir.resolve(stateFile);
        return new MappedSessionState(stateFilePath);
    }

    protected MessageStore createMessageStore() {
        return null;
    }

    protected SessionSchedule createSessionSchedule() {
        SessionSchedule sessionSchedule = ContinuousSessionSchedule.INSTANCE;

        if (schedule != null) {
            final ZoneId zoneId = schedule.zoneId;
            final List<AgileSessionSchedule.Interval> intervals = new ArrayList<>();

            for (final Schedule.Interval interval : schedule.intervals) {
                final LocalTime startTime = interval.startTime;
                final LocalTime endTime = interval.endTime;

                final DayOfWeek startDay = interval.startDay;
                final DayOfWeek endDay = interval.endDay;


                intervals.add(new AgileSessionSchedule.Interval(startTime, endTime, startDay, endDay));
            }

            sessionSchedule = new AgileSessionSchedule(
                    zoneId,
                    intervals.toArray(new AgileSessionSchedule.Interval[0])
            );
        }

        return sessionSchedule;
    }


    // endregion

    private void installControl(final TradeConnectorContext connectorContext, final SessionContext sessionContext) {
        final Consumer<Object> agent = connectorContext.getAgent();

        if (agent != null) {
            final @Alphanumeric long id = connectorContext.getId();
            final long tag = TradeConnectorTag.TAG;
            final SessionId sessionIdentity = sessionContext.sessionId();
            final CounterFactory counterFactory = connectorContext.getCounterFactory();
            final EndpointSessionControl control = new EndpointSessionControl(id, tag, id, sessionIdentity, counterFactory);

            sessionContext.control(control);
        }
    }

    private void installAgent(final TradeConnectorContext connectorContext, final T connector) {
        final Consumer<Object> agent = connectorContext.getAgent();
        if (agent != null) {
            agent.accept(connector);
        }
    }

}
