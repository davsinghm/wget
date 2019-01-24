package com.davsinghm.wget.core.info.ex;

import com.davsinghm.wget.core.info.DownloadInfo;

public class DownloadMultipartException extends DownloadException {

    private DownloadInfo info;

    public DownloadMultipartException() {
    }

    public DownloadMultipartException(String message) {
        super(message);
    }

    public DownloadMultipartException(String message, Throwable cause) {
        super(message, cause);
    }

    public DownloadMultipartException(Throwable cause) {
        super(cause);
    }

    public DownloadMultipartException(String message, Throwable cause, DownloadInfo info) {
        super(message, cause);
        this.info = info;
    }

    public DownloadInfo getInfo() {
        return info;
    }

}
