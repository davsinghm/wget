package com.dsingh.wget.core.info.ex;

public class MuxException extends Exception {
    public MuxException() {
        super();
    }

    public MuxException(String detailMessage) {
        super(detailMessage);
    }

    public MuxException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public MuxException(Throwable throwable) {
        super(throwable);
    }
}
