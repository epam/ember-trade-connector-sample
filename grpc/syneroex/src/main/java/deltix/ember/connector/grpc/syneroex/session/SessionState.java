package deltix.ember.connector.grpc.syneroex.session;

import com.epam.deltix.gflog.api.AppendableEntry;
import com.epam.deltix.gflog.api.Loggable;
import deltix.efix.endpoint.SessionComponent;

public class SessionState implements SessionComponent, Loggable {
    protected SessionStatus status = SessionStatus.DISCONNECTED;
    protected long lastReceivedTime = Long.MIN_VALUE;
    protected long lastSentTime = Long.MIN_VALUE;
    protected long sessionStartTime = Long.MIN_VALUE;

    private String lastSubscriptionName;

    public SessionStatus status() {
        return status;
    }

    public SessionState status(SessionStatus status) {
        this.status = status;
        return this;
    }

    public long lastReceivedTime() {
        return lastReceivedTime;
    }

    public SessionState lastReceivedTime(long lastReceivedTime) {
        this.lastReceivedTime = lastReceivedTime;
        return this;
    }

    public long lastSentTime() {
        return lastSentTime;
    }

    public SessionState lastSentTime(long lastSentTime) {
        this.lastSentTime = lastSentTime;
        return this;
    }

    public long sessionStartTime() {
        return sessionStartTime;
    }

    public SessionState sessionStartTime(long sessionStartTime) {
        this.sessionStartTime = sessionStartTime;
        return this;
    }

    public String lastSubscriptionName() {
        return lastSubscriptionName;
    }

    public SessionState lastSubscriptionName(String lastSubscriptionName) {
        this.lastSubscriptionName = lastSubscriptionName;
        return this;
    }

    @Override
    public void flush() {
    }

    @Override
    public void open() {
    }

    @Override
    public void close() {
    }

    @Override
    public void appendTo(AppendableEntry entry) {
        entry.append("SessionState - status: ").append(status)
                .append(", lastSentTime: ").appendTimestamp(lastSentTime)
                .append(", lastReceivedTime: ").appendTimestamp(lastReceivedTime)
                .append(", lastSubscriptionName").append(lastSubscriptionName);
    }
}
