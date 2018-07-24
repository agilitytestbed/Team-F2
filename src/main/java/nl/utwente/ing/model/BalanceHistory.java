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

    public double getOpen() {
        return open / 100.0;
    }

    public double getClose() {
        return close / 100.0;
    }

    public double getHigh() {
        return high / 100.0;
    }

    public double getLow() {
        return low / 100.0;
    }

    public double getVolume() {
        return volume / 100.0;
    }

    public long getTimestamp() {
        return timestamp.getMillis() / 1000;
    }
}
