package com.alipay.alps.flatv3.index.result;


import lombok.Getter;
import lombok.Setter;

/*
 * Range is a class that represents a range of integer values. It has the following properties:
 * 1. It has a low and a high integer value. The low and high value are inclusive.
 * 2. It has a size, which is the number of integers it contains.
 * 3. It can be used to test if a given integer is contained in the range.
 * 4. It can join another range, it will contain all the integers that are in both of the two ranges.
 *
 * The following are examples of valid Ranges:
 * 1. [0, 0] is a valid range that contains only the integer 0.
 * 2. [0, 2] is a valid range that contains the integers 0, 1, and 2.
 *
 * The following are examples of invalid Ranges:
 * 1. [0, -1] is invalid because the low value is greater than the high value.
 * 2. [-1, -1] is invalid because the low value and high value are less than 0.
 * 3. [1, 0] is invalid because the low value is greater than the high value.
 *
 * [2, 9] join [6, 10] is [6, 9]
 */
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
