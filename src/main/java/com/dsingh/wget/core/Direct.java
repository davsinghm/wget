package com.dsingh.wget.core;

import com.dsingh.wget.core.info.DownloadInfo;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Direct {

    private File target;
    private DownloadInfo info;
    static public final int BUF_SIZE = 4 * 1024; // size of read buffer

    public Direct(DownloadInfo info, File target) {
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
