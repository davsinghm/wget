package com.davsinghm.wget.core.info.ex;

public class DownloadIOException extends DownloadException {

    public DownloadIOException() {
    }

    public DownloadIOException(String message) {
        super(message);
    }

    public DownloadIOException(String message, Throwable cause) {
        super(message, cause);
    }

    public DownloadIOException(Throwable cause) {
        super(cause);
    }
}
