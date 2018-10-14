package com.dsingh.wget;

public enum DState {
    QUEUED, PARSING, EXTRACTING, DOWNLOADING, RETRYING, MUXING, ENCODING,
    COMPLETE,
    STOPPED,
    ERROR, MUX_ERROR, ENCODE_ERROR//, DONE //(DONE is used only for stored State, while checking if Download is COMPLETE | As first part shows DONE other shows FINAL State)
    //ON_GOING, seems to be for storing in DInfo table. recall.
}