package com.davsinghm.wget.core.info.ex;

import android.os.Build;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import androidx.annotation.RequiresApi;

public class DownloadMoved extends RuntimeException {

    private URLConnection c;

    public DownloadMoved() {
    }

    public DownloadMoved(String message) {
        super(message);
    }

    public DownloadMoved(String message, Throwable cause) {
        super(message, cause);
    }

    public DownloadMoved(Throwable cause) {
        super(cause);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public DownloadMoved(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public DownloadMoved(URLConnection c) {
        this.c = c;
    }

    public URL getMoved() {
        try {
            return new URL(c.getHeaderField("Location")); //NON-NLS
        } catch (MalformedURLException e) {
            throw new DownloadError(e);
        }
    }

}
