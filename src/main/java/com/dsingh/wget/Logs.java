package com.dsingh.wget;

import android.util.Log;

public class Logs {

    //TODO reporter

    public static final int ASSERT = Log.ERROR;
    public static final int DEBUG = Log.ERROR;
    public static final int ERROR = Log.ERROR;
    public static final int INFO = Log.ERROR;
    public static final int VERBOSE = Log.ERROR;
    public static final int WARN = Log.ERROR;

    synchronized private static void log(int priority, String tag, String msg) {
        if (BuildConfig.DEBUG)
            Log.println(priority, tag, msg);
    }

    synchronized public static void wtf(String tag, Throwable t) {
        wtf(tag, t.toString(), t);
    }

    synchronized public static void wtf(String tag, String msg) {
        log(ASSERT, tag, msg);
    }

    synchronized public static void wtf(String tag, String msg, Throwable t) {
        log(ASSERT, tag, msg);
        handleException(t);
    }

    synchronized public static void d(String tag, String msg) {
        log(DEBUG, tag, msg);
    }

    synchronized public static void e(String tag, String msg) {
        log(ERROR, tag, msg);
    }

    synchronized public static void i(String tag, String msg) {
        log(INFO, tag, msg);
    }

    synchronized public static void w(String tag, String msg) {
        log(WARN, tag, msg);
    }

    synchronized public static void w(String tag, Throwable t) {
        w(tag, t.toString(), t);
    }

    synchronized public static void w(String tag, String msg, Throwable t) {
        w(tag, msg + '\n' + t.toString(), t);
    }

    synchronized private static void handleException(Throwable t) {
        if (BuildConfig.DEBUG)
            Log.wtf("WGet", t);
    }
}