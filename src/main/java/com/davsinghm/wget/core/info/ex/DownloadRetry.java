package com.davsinghm.wget.core.info.ex;

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
}
