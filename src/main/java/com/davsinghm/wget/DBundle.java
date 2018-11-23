package com.davsinghm.wget;

import android.net.Uri;
import android.os.Parcelable;

import java.util.concurrent.BlockingQueue;

public abstract class DBundle implements Parcelable {

    public abstract BlockingQueue<Job> getJobQueue();

    public abstract String getDownloadUid();

    public abstract int getDownloadCode();

    public abstract String getTitle();

    public abstract boolean isTwoPartDownload();

    public abstract boolean isAudioOnly();

    public abstract Uri getTargetUri();

    public abstract String getSubtitleUrl();

    public abstract Uri getSubtitleUri();

    public abstract String getVideoUrl();

    public abstract Uri getVideoUri();

    public abstract String getAudioUrl();

    public abstract Uri getAudioUri();

    public abstract void muxAllFiles() throws Exception;

    public abstract void updateYtUrls() throws Exception;

    public abstract void onDownloadComplete();

    public abstract DSettings getDSettings(); //TODO move
}