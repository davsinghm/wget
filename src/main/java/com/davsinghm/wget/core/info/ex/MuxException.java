package com.davsinghm.wget.core.info.ex;

import android.os.Build;

import androidx.annotation.RequiresApi;

public class MuxException extends Exception {

    public MuxException() {
        super();
    }

    public MuxException(String message) {
        super(message);
    }

    public MuxException(String message, Throwable cause) {
        super(message, cause);
    }

    public MuxException(Throwable cause) {
        super(cause);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    protected MuxException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
