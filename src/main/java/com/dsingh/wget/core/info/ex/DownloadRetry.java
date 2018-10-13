package com.dsingh.wget.core.info.ex;

import android.os.Build;

import androidx.annotation.RequiresApi;

public class DownloadRetry extends RuntimeException {

    public DownloadRetry() {
    }

    public DownloadRetry(String message) {
        super(message);
    }

    public DownloadRetry(String message, Throwable cause) {
        super(message, cause);
    }

    public DownloadRetry(Throwable cause) {
        super(cause);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public DownloadRetry(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
