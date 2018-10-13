package com.dsingh.wget;

import java.util.concurrent.atomic.AtomicBoolean;

public class DTask implements TaskRunnable {

    private DRunnable mDRunnable;
    private DBundle mDBundle;
    private final DManager sDManager;
    private Thread mCurrentThread;
    private String mDownloadUid;
    private AtomicBoolean mStop = new AtomicBoolean(false);

    DTask(DManager dManager, DBundle dBundle) {
        sDManager = dManager;
        mDBundle = dBundle;
        mDRunnable = new DRunnable(dManager.getAppContext(), this, dBundle, mStop);
        mDownloadUid = dBundle.getDownloadUid();
    }

    public DBundle getDBundle() {
        return mDBundle;
    }

    public void stopDownload() {
        mStop.set(true);
        if (mCurrentThread != null)
            mCurrentThread.interrupt();
    }

    public String getDownloadUid() {
        return mDownloadUid;
    }

    public DRunnable getDRunnable() {
        return mDRunnable;
    }

    public Thread getCurrentThread() {
        synchronized (sDManager) {
            return mCurrentThread;
        }
    }

    @Override
    public void setDownloadThread(Thread thread) {
        synchronized (sDManager) {
            mCurrentThread = thread;
        }
    }

    @Override
    public void progressUpdate(int isOnGoing, DProgress progress) {
        sDManager.progressUpdate(isOnGoing, progress);
    }

    @Override
    public boolean removeFromQueue() {
        return sDManager.removeDTaskFromQueue(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DTask dTask = (DTask) o;

        return !(mDownloadUid != null ? !mDownloadUid.equals(dTask.mDownloadUid) : dTask.mDownloadUid != null);

    }
}
