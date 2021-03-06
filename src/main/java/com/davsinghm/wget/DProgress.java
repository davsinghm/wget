package com.davsinghm.wget;

import android.os.Parcel;
import android.os.Parcelable;

public class DProgress implements Parcelable {

    /*public int getProgressPer() {
        return length != 0 ? (int) ((float) count * 100 / length) : -1;
    }*/

    private int downloadCode;
    private String downloadId;
    private String title;

    private DState dState;
    private DState dLastState = DState.QUEUED;
    private boolean showAudio; // true if downloading Audio of Dash video;
    private long count;
    private long length;
    private int avgSpeed; //Bytes per second;
    private int currentSpeed; //Bytes per second;
    private String threadCount;
    private int time; //time, mux stats
    private int totalTime; //total time, mux stats

    DProgress(DBundle dBundle) {
        this.downloadCode = dBundle.getDownloadCode();
        this.downloadId = dBundle.getDownloadId();
        this.title = dBundle.getTitle();
    }

    public String getDownloadId() {
        return downloadId;
    }

    public DState getDState() {
        return dState;
    }

    public void setDState(DState dState) {
        this.dState = dState;
    }

    public void setLastDState(DState dState) {
        this.dLastState = dState;
    }

    public DState getLastDState() {
        return dLastState;
    }

    public boolean isStateChanged() {
        return dLastState != dState;
    }

    public String getTitle() {
        return title;
    }

    public boolean showAudio() {
        return showAudio;
    }

    public void setShowAudio(boolean bool) {
        this.showAudio = bool;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public int getDownloadCode() {
        return downloadCode;
    }

    public int getAvgSpeed() {
        return avgSpeed;
    }

    public void setAvgSpeed(int avgSpeed) {
        this.avgSpeed = avgSpeed;
    }

    public int getCurrentSpeed() {
        return currentSpeed;
    }

    public void setCurrentSpeed(int currentSpeed) {
        this.currentSpeed = currentSpeed;
    }

    public String getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(String threadCount) {
        this.threadCount = threadCount;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(int totalTime) {
        this.totalTime = totalTime;
    }

    protected DProgress(Parcel in) {
        downloadCode = in.readInt();
        downloadId = in.readString();
        title = in.readString();
        dState = (DState) in.readValue(DState.class.getClassLoader());
        dLastState = (DState) in.readValue(DState.class.getClassLoader());
        showAudio = in.readByte() != 0x00;
        count = in.readLong();
        length = in.readLong();
        avgSpeed = in.readInt();
        currentSpeed = in.readInt();
        threadCount = in.readString();
        time = in.readInt();
        totalTime = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(downloadCode);
        dest.writeString(downloadId);
        dest.writeString(title);
        dest.writeValue(dState);
        dest.writeValue(dLastState);
        dest.writeByte((byte) (showAudio ? 0x01 : 0x00));
        dest.writeLong(count);
        dest.writeLong(length);
        dest.writeInt(avgSpeed);
        dest.writeInt(currentSpeed);
        dest.writeString(threadCount);
        dest.writeInt(time);
        dest.writeInt(totalTime);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<DProgress> CREATOR = new Parcelable.Creator<DProgress>() {
        @Override
        public DProgress createFromParcel(Parcel in) {
            return new DProgress(in);
        }

        @Override
        public DProgress[] newArray(int size) {
            return new DProgress[size];
        }
    };
}