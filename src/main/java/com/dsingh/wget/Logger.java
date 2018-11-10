package com.dsingh.wget;

public class Logger {

    static Callback callback;

    public static void wtf(String tag, Throwable t) {
        if (callback != null)
            callback.e(tag, t);
    }

    public static void wtf(String tag, String msg) {
        if (callback != null)
            callback.e(tag, msg);
    }

    public static void wtf(String tag, String msg, Throwable t) {
        if (callback != null)
            callback.e(tag, msg, t);
    }

    public static void d(String tag, String msg) {
        if (callback != null)
            callback.e(tag, msg);
    }

    public static void e(String tag, String msg) {
        if (callback != null)
            callback.e(tag, msg);
    }

    public static void i(String tag, String msg) {
        if (callback != null)
            callback.e(tag, msg);
    }

    public static void w(String tag, String msg) {
        if (callback != null)
            callback.e(tag, msg);
    }

    public static void w(String tag, Throwable t) {
        if (callback != null)
            callback.e(tag, t);
    }

    public static void w(String tag, String msg, Throwable t) {
        if (callback != null)
            callback.e(tag, msg, t);
    }

    public interface Callback {

        void wtf(String tag, Throwable t);

        void wtf(String tag, String msg);

        void wtf(String tag, String msg, Throwable t);

        void d(String tag, String msg);

        void e(String tag, String msg);

        void e(String tag, Throwable t);

        void e(String tag, String msg, Throwable t);

        void i(String tag, String msg);

        void w(String tag, String msg);

        void w(String tag, Throwable t);

        void w(String tag, String msg, Throwable t);
    }
}