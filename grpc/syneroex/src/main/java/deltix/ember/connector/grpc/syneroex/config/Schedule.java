package deltix.ember.connector.grpc.syneroex.config;

import deltix.anvil.util.annotation.Required;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

public class Schedule {

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
