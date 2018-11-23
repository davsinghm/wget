package com.davsinghm.wget;

import android.net.Uri;
import android.os.Parcelable;

import java.io.File;
import java.util.concurrent.BlockingQueue;

public interface DBundle extends Parcelable {

    BlockingQueue<Job> getJobQueue();

    String getDownloadUid();

    int getDownloadCode();

    String getTitle();

    boolean isTwoPartDownload();

    boolean isAudioOnly();

    File getTargetFile();

    String getSubtitleUrl();

    File getSubtitleFile();

    String getVideoUrl();

    Uri getVideoUri();

    String getAudioUrl();

    Uri getAudioUri();

    void muxAllFiles() throws Exception;

    void updateYtUrls() throws Exception;

    void onDownloadComplete();

    DSettings getDSettings();
}