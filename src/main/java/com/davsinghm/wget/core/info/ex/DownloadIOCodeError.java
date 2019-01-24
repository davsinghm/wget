package com.davsinghm.wget.core.info.ex;

public class DownloadIOCodeError extends DownloadError {

    private int code;

    public DownloadIOCodeError() {
    }

    public DownloadIOCodeError(String message) {
        super(message);
    }

    public DownloadIOCodeError(String message, Throwable cause) {
        super(message, cause);
    }

    public DownloadIOCodeError(Throwable cause) {
        super(cause);
    }

    public DownloadIOCodeError(int code) {
        this.code = code;
    }

    public DownloadIOCodeError(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
