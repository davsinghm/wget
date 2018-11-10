package com.davsinghm.wget.core.info;

import java.util.Comparator;

public class Part implements Comparable<Part> {

    private long start; // start offset [start, end]
    private long end; //end offset [start, end]
    private int number; // part number
    private long count; //number of bytes we are downloaded
    private State state; // download state
    private Throwable exception; // downloading error / retry error
    private int delay; // retrying delay;

    public synchronized long getStart() {
        return start;
    }

    public synchronized void setStart(long start) {
        this.start = start;
    }

    public synchronized long getEnd() {
        return end;
    }

    public synchronized void setEnd(long end) {
        this.end = end;
    }

    public synchronized int getNumber() {
        return number;
    }

    public synchronized void setNumber(int number) {
        this.number = number;
    }

    public synchronized long getLength() {
        return end - start + 1;
    }

    public synchronized long getCount() {
        return count;
    }

    public synchronized void setCount(long count) {
        this.count = count;
    }

    public synchronized State getState() {
        return state;
    }

    public synchronized void setState(State state) {
        this.state = state;
        this.exception = null;
    }

    public synchronized void setState(State state, Throwable e) {
        this.state = state;
        this.exception = e;
    }

    public synchronized Throwable getException() {
        return exception;
    }

    public synchronized void setException(Throwable exception) {
        this.exception = exception;
    }

    public synchronized int getDelay() {
        return delay;
    }

    public synchronized void setDelay(int delay, Throwable e) {
        this.state = State.RETRYING;
        this.delay = delay;
        this.exception = e;
    }

    @Override
    public int compareTo(Part another) {
        long l1 = getLength() - getCount();
        long l2 = another.getLength() - another.getCount();
        return (int) (l2 - l1);
    }

    public static Comparator<Part> PartComparator = new Comparator<Part>() {

        @Override
        public int compare(Part lhs, Part rhs) {
            return lhs.compareTo(rhs);
        }
    };
}