package nl.utwente.ing.model;

import org.joda.money.Money;
import org.joda.time.DateTime;

import java.math.BigDecimal;

public class BalanceHistory {
    private Money open;
    private Money close;
    private Money high;
    private Money low;
    private Money volume;
    private DateTime timestamp;

    public BalanceHistory(Money open, Money close, Money high, Money low, Money volume, DateTime timestamp) {
        this.open = open;
        this.close = close;
        this.high = high;
        this.low = low;
        this.volume = volume;
        this.timestamp = timestamp;
    }

    public BigDecimal getOpen() {
        return open.getAmount();
    }

    public BigDecimal getClose() {
        return close.getAmount();
    }

    public BigDecimal getHigh() {
        return high.getAmount();
    }

    public BigDecimal getLow() {
        return low.getAmount();
    }

    public BigDecimal getVolume() {
        return volume.getAmount();
    }

    public long getTimestamp() {
        return timestamp.getMillis() / 1000;
    }
}
