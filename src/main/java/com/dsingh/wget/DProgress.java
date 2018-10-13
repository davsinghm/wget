package com.dsingh.wget;
import android.os.Parcel;
import android.os.Parcelable;

public class DProgress implements Parcelable {

    /*public int getProgressPer() {
        return length != 0 ? (int) ((float) count * 100 / length) : -1;
    }*/

    private int downloadCode;
    private String downloadUid;
    private String title;
    private final int notificationId;

    private DState dState;
    private DState dLastState = DState.QUEUED;
    private boolean showAudio; // true if downloading Audio of Dash video;
    private long count;
    private long length;
    private int avgSpeed; //Bytes per second;
    private int currentSpeed; //Bytes per second;
    private String threadCount;

    DProgress(DBundle dBundle) {
        this.downloadCode = dBundle.getDownloadCode();
        this.downloadUid = dBundle.getDownloadUid();
        this.title = dBundle.getTitle();
        this.notificationId = generateNotificationId(this.downloadUid);
    }

    public String getDownloadUID() {
        return downloadUid;
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

    protected DProgress(Parcel in) {
        downloadCode = in.readInt();
        downloadUid = in.readString();
        title = in.readString();
        notificationId = in.readInt();
        dState = (DState) in.readValue(DState.class.getClassLoader());
        dLastState = (DState) in.readValue(DState.class.getClassLoader());
        showAudio = in.readByte() != 0x00;
        count = in.readLong();
        length = in.readLong();
        avgSpeed = in.readInt();
        currentSpeed = in.readInt();
        threadCount = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(downloadCode);
        dest.writeString(downloadUid);
        dest.writeString(title);
        dest.writeInt(notificationId);
        dest.writeValue(dState);
        dest.writeValue(dLastState);
        dest.writeByte((byte) (showAudio ? 0x01 : 0x00));
        dest.writeLong(count);
        dest.writeLong(length);
        dest.writeInt(avgSpeed);
        dest.writeInt(currentSpeed);
        dest.writeString(threadCount);
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