package com.dsingh.wget;

public class Constants {

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.3; WOW64; rv:38.0) Gecko/20100101 Firefox/38.0";

    public static final int DRUNNABLE_PROGRESS_UPDATE_INTERVAL = 1000;
    public static final int DSERVICE_FG_NOTI_ID = 1;
    public static final int DSERVICE_FG_NOTI_UPDATE_DELAY = 1000;
    public static final int DSERVICE_FG_NOTI_SWAP_DELAY = 5000;

    public static final int DSERVICE_REPORT_LOGS_DELAY = 5000;

    public static final int MT_MIN_SEC_SPEED_MULTIPLE_FOR_IDM = 5;
    public static final int MT_STYLE_CLASSIC = 0;
    public static final int MT_STYLE_SMART = 1;
    public static final int MT_STYLE_AXET = 2;

    public static final int WGET_CONNECT_TIMEOUT = 30 * 1000; // connect socket timeout, in milliseconds
    public static final int WGET_READ_TIMEOUT = 30 * 1000; // read socket timeout, in milliseconds

    public static final String NOTI_GROUP_DOWNLOAD_PROGRESS = "download_progress_group";

    public static final String EXTRA_PI_NOTI_ONGOING = "DownloadOngoing";//pending intent extra flag opening activity;
    public static final String EXTRA_PI_NOTI_FINISHED = "DownloadFinishedUid";//pending intent extra flag opening activity;


    public static final String NOTI_CHANNEL_DOWNLOAD_PROGRESS = "download_progress";
    public static final String NOTI_CHANNEL_DOWNLOAD_COMPLETED = "download_completed";
    public static final String NOTI_CHANNEL_DOWNLOAD_STOPPED = "download_stopped";
    public static final String NOTI_CHANNEL_DOWNLOAD_ERROR = "download_error";

    public static final String NOTI_CHANNEL_DEFAULT = "default";

}
