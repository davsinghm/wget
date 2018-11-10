package com.davsinghm.wget;

public class Constants {

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.3; WOW64; rv:38.0) Gecko/20100101 Firefox/38.0";

    public static final int DRUNNABLE_PROGRESS_UPDATE_INTERVAL = 1000;

    public static final int MT_MIN_SEC_SPEED_MULTIPLE_FOR_IDM = 5;
    public static final int MT_STYLE_CLASSIC = 0;
    public static final int MT_STYLE_SMART = 1;
    public static final int MT_STYLE_AXET = 2;

    public static final int WGET_CONNECT_TIMEOUT = 30 * 1000; // connect socket timeout, in milliseconds
    public static final int WGET_READ_TIMEOUT = 30 * 1000; // read socket timeout, in milliseconds

}
