package com.davsinghm.wget;

import android.content.Context;
import android.os.Handler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;

public class DManager {

    @Nullable
    private BlockingQueue<Runnable> workQueue;
    @Nullable
    private BlockingQueue<DRunnable> runnableQueue;
    @Nullable
    private ThreadPoolExecutor threadPool;

    private Handler handler;
    private DService dService;
    private Context context;

    public Context getAppContext() {
        return context;
    }

    DManager(Context context, int poolSize) {
        this.context = context.getApplicationContext();

        initialize(poolSize);
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void setService(DService dService) {
        this.dService = dService;
    }

    private void initialize(int poolSize) {
        Logger.d("DManager: initPool()", "invoked");

        runnableQueue = new LinkedBlockingQueue<>();
        workQueue = new LinkedBlockingQueue<>();
        threadPool = new ThreadPoolExecutor(poolSize, poolSize, 0, TimeUnit.SECONDS, workQueue);
    }

    void shutdown() {

        Logger.w("DManager", "shutdown(): invoked");

        handler = null;

        cancelAll();

        threadPool.shutdown();
    }

    boolean queueDownload(DBundle dBundle) {

        DRunnable dRunnable = new DRunnable(this, dBundle);

        if (runnableQueue.contains(dRunnable))
            return false;

        runnableQueue.add(dRunnable);
        threadPool.execute(dRunnable);

        return true;
    }

    /*public static int getDownloadCount() {

        synchronized (sInstance) {
            int i = sInstance.mTaskWorkQueue.size();
            Logger.d("DManager", "getDownloadCount(): invoked, return " + i);
            return i;
        }
    }*/

    void cancelDownload(String downloadUid) {

        Logger.d("DManager", "cancelDownload(): invoked, UID: " + downloadUid);

        for (DRunnable dRunnable : runnableQueue) {
            if (dRunnable.getDownloadUid().equals(downloadUid)) {

                dRunnable.stopDownload();
                if (dRunnable.getCurrentThread() == null) {
                    threadPool.remove(dRunnable);
                    runnableQueue.remove(dRunnable);

                    if (dService != null)
                        dService.onBundleRemoved(dRunnable.getDBundle());
                }
            }
        }
    }

    void cancelAll() {

        Logger.d("DManager", "cancelAll(): invoked");

        for (DRunnable dRunnable : runnableQueue) {
            dRunnable.stopDownload();
            if (dRunnable.getCurrentThread() == null) {
                threadPool.remove(dRunnable);
                runnableQueue.remove(dRunnable);

                //if thread is null, it means DRunnable was not started, if so, we need to set the state to STOPPED.
                //if it got started there could be a problem.
                //FIXME what if we do this after calling mThreadPool shutdown Now? read docs.
                if (dService != null)
                    dService.onBundleRemoved(dRunnable.getDBundle());
            }
        }
    }

    @Nullable
    DState getDState(String downloadUid) {

        for (DRunnable dRunnable : runnableQueue) {
            if (dRunnable.getDownloadUid().equals(downloadUid))
                return dRunnable.getState();
        }

        return null;
    }

    boolean removeFromQueue(DRunnable dRunnable) {

        boolean taskQ = runnableQueue.remove(dRunnable);
        Logger.d("DManager: removeDTaskFromQueue()", "invoked. Param - DTask:UID: " + dRunnable.getDownloadUid() + " ? removed: " + taskQ);

        if (taskQ)
            if (runnableQueue.size() == 0) {
                Logger.d("DManager", "stopService(): DManager.getDownloadCount() == 0");

                if (handler != null)
                    handler.obtainMessage(DService.MESSAGE_SHUTDOWN).sendToTarget();
            }

        return taskQ;
    }

    void progressUpdate(int what, DProgress dProgress) {

        if (handler != null)
            handler.obtainMessage(what, dProgress).sendToTarget();
    }
}
