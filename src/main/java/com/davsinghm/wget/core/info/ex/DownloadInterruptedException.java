package com.davsinghm.wget.core.info.ex;

public class DownloadInterruptedException extends DownloadException {

    public DownloadInterruptedException() {
    }

    public DownloadInterruptedException(String message) {
        super(message);
    }

    public DownloadInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }

    public DownloadInterruptedException(Throwable cause) {
        super(cause);
    }
}
