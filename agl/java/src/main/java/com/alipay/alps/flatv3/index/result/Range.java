package com.alipay.alps.flatv3.index.result;


import lombok.Getter;
import lombok.Setter;

public class Range {
    @Getter
    @Setter
    private int low;
    @Getter
    @Setter
    private int high;

    public Range(Range r) {
        this.low = r.low;
        this.high = r.low;
        if (this.low > this.high) {
            this.low = -1;
            this.high = -1;
        }
    }

    public Range(int low, int high) {
        this.low = low;
        this.high = high;
    }

    public boolean contains(int number) {
        return (number >= low && number <= high);
    }

    public void join(Range range) {
        this.low = Math.max(this.low, range.low);
        this.high = Math.min(this.high, range.high);
        if (this.low > this.high) {
            this.low = -1;
            this.high = -1;
        }
    }

    public int getSize() {
        if (this.low == -1 && this.high == -1) {
            return 0;
        }
        return high - low + 1;
    }

    public void setLow(int low) {
        this.low = low;
    }

    public void setHigh(int high) {
        this.high = high;
    }

    public int getLow() {
        return low;
    }

    public int getHigh() {
        return high;
    }

    @Override
    public String toString() {
        return "Range{" +
                "low=" + low +
                ", high=" + high +
                '}';
    }
}
