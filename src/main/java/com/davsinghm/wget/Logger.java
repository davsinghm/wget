package com.davsinghm.wget;

public class Logger {

    static Callback callback;

    public static void wtf(String tag, Throwable t) {
        if (callback != null)
            callback.wtf(tag, t);
    }

    public static void wtf(String tag, String msg) {
        if (callback != null)
            callback.wtf(tag, msg);
    }

    public static void wtf(String tag, String msg, Throwable t) {
        if (callback != null)
            callback.wtf(tag, msg, t);
    }

    public static void d(String tag, String msg) {
        if (callback != null)
            callback.d(tag, msg);
    }

    public static void e(String tag, String msg) {
        if (callback != null)
            callback.e(tag, msg);
    }

    public static void i(String tag, String msg) {
        if (callback != null)
            callback.i(tag, msg);
    }

    public static void w(String tag, String msg) {
        if (callback != null)
            callback.w(tag, msg);
    }

    public static void w(String tag, Throwable t) {
        if (callback != null)
            callback.w(tag, t);
    }

    public static void w(String tag, String msg, Throwable t) {
        if (callback != null)
            callback.w(tag, msg, t);
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