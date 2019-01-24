package com.davsinghm.wget.core.info.ex;

public class DownloadIOCodeException extends DownloadException {

    private int code;

    public DownloadIOCodeException() {
    }

    public DownloadIOCodeException(String message) {
        super(message);
    }

    public DownloadIOCodeException(String message, Throwable cause) {
        super(message, cause);
    }

    public DownloadIOCodeException(Throwable cause) {
        super(cause);
    }

    public DownloadIOCodeException(int code) {
        this.code = code;
    }

    public DownloadIOCodeException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
