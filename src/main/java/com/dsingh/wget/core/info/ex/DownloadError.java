package com.dsingh.wget.core.info.ex;

import android.os.Build;

import androidx.annotation.RequiresApi;

public class DownloadError extends RuntimeException {

    public DownloadError() {
    }

    public DownloadError(String message) {
        super(message);
    }

    public DownloadError(String message, Throwable cause) {
        super(message, cause);
    }

    public DownloadError(Throwable cause) {
        super(cause);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public DownloadError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
