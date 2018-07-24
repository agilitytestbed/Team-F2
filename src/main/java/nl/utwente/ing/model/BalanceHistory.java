package nl.utwente.ing.model;

import org.joda.time.DateTime;

import java.util.Date;

public class BalanceHistory {
    private long open;
    private long close;
    private long high;
    private long low;
    private long volume;
    private DateTime timestamp;

    public BalanceHistory(long open, long close, long high, long low, long volume, DateTime timestamp) {
        this.open = open;
        this.close = close;
        this.high = high;
        this.low = low;
        this.volume = volume;
        this.timestamp = timestamp;
    }

    public long getOpen() {
        return open;
    }

    public long getClose() {
        return close;
    }

    public long getHigh() {
        return high;
    }

    public long getLow() {
        return low;
    }

    public long getVolume() {
        return volume;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }
}
