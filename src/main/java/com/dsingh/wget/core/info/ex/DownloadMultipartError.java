package com.dsingh.wget.core.info.ex;

import android.os.Build;

import com.dsingh.wget.core.info.DownloadInfo;

import androidx.annotation.RequiresApi;

public class DownloadMultipartError extends DownloadError {

    private DownloadInfo info;

    public DownloadMultipartError() {
    }

    public DownloadMultipartError(String message) {
        super(message);
    }

    public DownloadMultipartError(String message, Throwable cause) {
        super(message, cause);
    }

    public DownloadMultipartError(Throwable cause) {
        super(cause);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public DownloadMultipartError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public DownloadMultipartError(DownloadInfo info) {
        super();
        this.info = info;
    }

    public DownloadInfo getInfo() {
        return info;
    }

}
