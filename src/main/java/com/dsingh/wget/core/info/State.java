package com.dsingh.wget.core.info;

public enum State {
    QUEUED, WAITING, EXTRACTING, EXTRACTING_DONE, DOWNLOADING, RETRYING, STOP, ERROR, DONE
}
