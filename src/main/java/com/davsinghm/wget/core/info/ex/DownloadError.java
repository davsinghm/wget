package com.davsinghm.wget.core.info.ex;

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
}
