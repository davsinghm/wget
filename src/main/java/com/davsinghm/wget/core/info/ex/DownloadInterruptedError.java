package com.davsinghm.wget.core.info.ex;

public class DownloadInterruptedError extends DownloadError {

    public DownloadInterruptedError() {
    }

    public DownloadInterruptedError(String message) {
        super(message);
    }

    public DownloadInterruptedError(String message, Throwable cause) {
        super(message, cause);
    }

    public DownloadInterruptedError(Throwable cause) {
        super(cause);
    }
}
