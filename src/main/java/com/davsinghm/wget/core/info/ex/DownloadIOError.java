package com.davsinghm.wget.core.info.ex;

import android.os.Build;

import androidx.annotation.RequiresApi;

public class DownloadIOError extends DownloadError {

    public DownloadIOError() {
    }

    public DownloadIOError(String message) {
        super(message);
    }

    public DownloadIOError(String message, Throwable cause) {
        super(message, cause);
    }

    public DownloadIOError(Throwable cause) {
        super(cause);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public DownloadIOError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
