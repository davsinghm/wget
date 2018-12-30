package com.davsinghm.wget;

import android.net.Uri;
import android.os.Parcelable;

import java.util.concurrent.BlockingQueue;

import androidx.annotation.NonNull;

public abstract class DBundle implements Parcelable {

    public abstract BlockingQueue<Job> getJobQueue();

    public abstract String getDownloadId();

    public abstract int getDownloadCode();

    public abstract String getTitle();

    public abstract boolean isTwoPartDownload();

    public abstract boolean isAudioOnly();

    public abstract void muxAllFiles() throws Exception;

    public abstract void updateBundle() throws Exception;

    public abstract void onDownloadComplete();

    public abstract DSettings getDSettings(); //TODO move

    public abstract String getVideoUrl();

    public abstract String getAudioUrl();

    public abstract String getSubtitleUrl();

    @NonNull
    public abstract Uri getDirectory();

    @NonNull
    public abstract String getVideoFilename();

    @NonNull
    public abstract String getAudioFilename();

    @NonNull
    public abstract Uri getSubtitleUri();
}