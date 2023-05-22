package deltix.connector.fix;

import deltix.anvil.util.annotation.Duration;
import deltix.anvil.util.annotation.Optional;
import deltix.anvil.util.annotation.Required;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@SuppressWarnings("unused")
public class FixTradeConnectorSettings {

    protected @Required String host;
    protected @Required int port;

    protected @Required String attributeKey;

    protected @Required String senderCompId;
    protected @Optional String senderSubId;
    protected @Required String targetCompId;
    protected @Optional String targetSubId;

    protected @Required Path workDir;
    protected @Optional Path stateFile;
    protected @Optional Path storeFile;
    protected @Optional Path logFile;

    protected @Optional int logBufferSize = 1 << 16;
    protected @Optional boolean resetSeqNums;
    protected @Optional boolean logHeartbeat;

    @Duration @Optional protected int logonTimeout = 2000;

    @Duration @Optional protected int logoutTimeout = 2000;

    @Duration @Optional protected int sendTimeout = 100;

    protected @Optional Schedule schedule;

    public String getHost() {
        return host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public String getAttributeKey() {
        return attributeKey;
    }

    public void setAttributeKey(String attributeKey) {
        this.attributeKey = attributeKey;
    }

    public String getSenderCompId() {
        return senderCompId;
    }

    public void setSenderCompId(final String senderCompId) {
        this.senderCompId = senderCompId;
    }

    public String getSenderSubId() {
        return senderSubId;
    }

    public void setSenderSubId(final String senderSubId) {
        this.senderSubId = senderSubId;
    }

    public String getTargetCompId() {
        return targetCompId;
    }

    public void setTargetCompId(final String targetCompId) {
        this.targetCompId = targetCompId;
    }

    public String getTargetSubId() {
        return targetSubId;
    }

    public void setTargetSubId(final String targetSubId) {
        this.targetSubId = targetSubId;
    }

    public Path getWorkDir() {
        return workDir;
    }

    public void setWorkDir(final Path workDir) {
        this.workDir = workDir;
    }

    public Path getStateFile() {
        return stateFile;
    }

    public void setStateFile(final Path stateFile) {
        this.stateFile = stateFile;
    }

    public Path getStoreFile() {
        return storeFile;
    }

    public void setStoreFile(final Path storeFile) {
        this.storeFile = storeFile;
    }

    public Path getLogFile() {
        return logFile;
    }

    public void setLogFile(final Path logFile) {
        this.logFile = logFile;
    }

    public int getLogBufferSize() {
        return logBufferSize;
    }

    public void setLogBufferSize(final int logBufferSize) {
        this.logBufferSize = logBufferSize;
    }

    public boolean isResetSeqNums() {
        return resetSeqNums;
    }

    public void setResetSeqNums(final boolean resetSeqNums) {
        this.resetSeqNums = resetSeqNums;
    }

    public boolean isLogHeartbeat() {
        return logHeartbeat;
    }

    public void setLogHeartbeat(final boolean logHeartbeat) {
        this.logHeartbeat = logHeartbeat;
    }

    public int getLogonTimeout() {
        return logonTimeout;
    }

    public void setLogonTimeout(final int logonTimeout) {
        this.logonTimeout = logonTimeout;
    }

    public int getLogoutTimeout() {
        return logoutTimeout;
    }

    public void setLogoutTimeout(final int logoutTimeout) {
        this.logoutTimeout = logoutTimeout;
    }

    public int getSendTimeout() {
        return sendTimeout;
    }

    public void setSendTimeout(final int sendTimeout) {
        this.sendTimeout = sendTimeout;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public void setSchedule(final Schedule schedule) {
        this.schedule = schedule;
    }

    public static class Schedule {

        protected @Required ZoneId zoneId;
        protected @Required List<Interval> intervals;

        public ZoneId getZoneId() {
            return zoneId;
        }

        public void setZoneId(final ZoneId zoneId) {
            this.zoneId = zoneId;
        }

        public List<Interval> getIntervals() {
            return intervals;
        }

        public void setIntervals(final List<Interval> intervals) {
            this.intervals = intervals;
        }

        public static class Interval {

            protected @Required LocalTime startTime;
            protected @Required LocalTime endTime;
            protected @Required DayOfWeek startDay;
            protected @Required DayOfWeek endDay;

            public LocalTime getStartTime() {
                return startTime;
            }

            public void setStartTime(final LocalTime startTime) {
                this.startTime = startTime;
            }

            public LocalTime getEndTime() {
                return endTime;
            }

            public void setEndTime(final LocalTime endTime) {
                this.endTime = endTime;
            }

            public DayOfWeek getStartDay() {
                return startDay;
            }

            public void setStartDay(final DayOfWeek startDay) {
                this.startDay = startDay;
            }

            public DayOfWeek getEndDay() {
                return endDay;
            }

            public void setEndDay(final DayOfWeek endDay) {
                this.endDay = endDay;
            }

        }

    }

}
