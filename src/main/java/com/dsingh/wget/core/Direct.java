package com.dsingh.wget.core;

import android.content.Context;

import com.dsingh.wget.core.info.DownloadInfo;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Direct {

    protected Context context;
    private File target;
    private DownloadInfo info;
    static public final int BUF_SIZE = 4 * 1024; // size of read buffer

    public Direct(Context context, DownloadInfo info, File target) {
        this.context = context;
        this.target = target;
        this.info = info;
    }

    protected synchronized DownloadInfo getInfo() {
        return info;
    }

    protected synchronized File getTarget() {
        return target;
    }

    abstract public void download(AtomicBoolean stop, Runnable notify);

}
