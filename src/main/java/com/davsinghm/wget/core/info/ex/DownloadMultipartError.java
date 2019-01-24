package com.davsinghm.wget.core.info.ex;

import com.davsinghm.wget.core.info.DownloadInfo;

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

    public DownloadMultipartError(String message, Throwable cause, DownloadInfo info) {
        super(message, cause);
        this.info = info;
    }

    public DownloadInfo getInfo() {
        return info;
    }

}
