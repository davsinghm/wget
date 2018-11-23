package com.davsinghm.wget.core;

import android.content.Context;
import android.net.Uri;

import com.davsinghm.wget.core.info.DownloadInfo;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Direct {

    public static final int BUF_SIZE = 4 * 1024; // size of read buffer

    private Context context;
    private Uri target;
    private DownloadInfo info;

    public Direct(Context context, DownloadInfo info, Uri target) {
        this.context = context;
        this.target = target;
        this.info = info;
    }

    protected synchronized DownloadInfo getInfo() {
        return info;
    }

    protected synchronized Uri getTarget() {
        return target;
    }

    protected synchronized Context getContext() {
        return context;
    }

    public abstract void download(AtomicBoolean stop, Runnable notify);

}
