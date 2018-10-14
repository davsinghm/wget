package com.dsingh.wget;

import android.util.Log;

public class Logs {

    public static void wtf(String tag, Throwable t) {
        Log.e(tag, t.getMessage(), t);
    }

    public static void wtf(String tag, String msg) {
        Log.e(tag, msg);
    }

    public static void wtf(String tag, String msg, Throwable t) {
        Log.e(tag, msg, t);
    }

    public static void d(String tag, String msg) {
        Log.e(tag, msg);
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
    }

    public static void i(String tag, String msg) {
        Log.e(tag, msg);
    }

    public static void w(String tag, String msg) {
        Log.e(tag, msg);
    }

    public static void w(String tag, Throwable t) {
        Log.e(tag, t.getMessage(), t);
    }

    public static void w(String tag, String msg, Throwable t) {
        Log.e(tag, msg, t);
    }
}