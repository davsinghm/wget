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

    synchronized public long getStart() {
        return start;
    }

    synchronized public void setStart(long start) {
        this.start = start;
    }

    synchronized public long getEnd() {
        return end;
    }

    synchronized public void setEnd(long end) {
        this.end = end;
    }

    synchronized public int getNumber() {
        return number;
    }

    synchronized public void setNumber(int number) {
        this.number = number;
    }

    synchronized public long getLength() {
        return end - start + 1;
    }

    synchronized public long getCount() {
        return count;
    }

    synchronized public void setCount(long count) {
        this.count = count;
    }

    synchronized public State getState() {
        return state;
    }

    synchronized public void setState(State state) {
        this.state = state;
        this.exception = null;
    }

    synchronized public void setState(State state, Throwable e) {
        this.state = state;
        this.exception = e;
    }

    synchronized public Throwable getException() {
        return exception;
    }

    synchronized public void setException(Throwable exception) {
        this.exception = exception;
    }

    synchronized public int getDelay() {
        return delay;
    }

    synchronized public void setDelay(int delay, Throwable e) {
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