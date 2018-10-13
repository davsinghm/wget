package com.dsingh.wget.core.info.ex;

import android.os.Build;

import androidx.annotation.RequiresApi;

public class DownloadInterruptedError extends DownloadError {

    public DownloadInterruptedError() {
    }

    public DownloadInterruptedError(String message) {
        super(message);
    }

    public DownloadInterruptedError(String message, Throwable cause) {
        super(message, cause);
    }

    public DownloadInterruptedError(Throwable cause) {
        super(cause);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public DownloadInterruptedError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
