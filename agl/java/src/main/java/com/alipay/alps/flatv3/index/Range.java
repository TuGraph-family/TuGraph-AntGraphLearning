package com.alipay.alps.flatv3.index;

public class Range
{
    private int low;
    private int high;

    public Range(Range r){
        this.low = r.getLow();
        this.high = r.getHigh();
    }

    public Range(int low, int high){
        this.low = low;
        this.high = high;
    }

    public boolean contains(int number){
        return (number >= low && number <= high);
    }

    public int getLow() {
        return low;
    }

    public int getHigh() {
        return high;
    }

    public void setLow(int low) {
        this.low = low;
    }

    public void setHigh(int high) {
        this.high = high;
    }

    public void join(Range range) {
        this.low = Math.max(this.low, range.low);
        this.high = Math.min(this.high, range.high);
    }

    public int getSize() {
        return high - low + 1;
    }

    @Override
    public String toString() {
        return "Range{" +
                "low=" + low +
                ", high=" + high +
                '}';
    }
}
