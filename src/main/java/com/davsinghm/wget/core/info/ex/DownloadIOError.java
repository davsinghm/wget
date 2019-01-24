package com.davsinghm.wget.core.info.ex;

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
}
