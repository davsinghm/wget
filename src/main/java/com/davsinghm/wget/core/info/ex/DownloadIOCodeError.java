package com.davsinghm.wget.core.info.ex;

import android.os.Build;

import androidx.annotation.RequiresApi;

public class DownloadIOCodeError extends DownloadError {

    private int code;

    public DownloadIOCodeError() {
    }

    public DownloadIOCodeError(String message) {
        super(message);
    }

    public DownloadIOCodeError(String message, Throwable cause) {
        super(message, cause);
    }

    public DownloadIOCodeError(Throwable cause) {
        super(cause);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public DownloadIOCodeError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public DownloadIOCodeError(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

}
