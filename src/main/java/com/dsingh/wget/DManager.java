package com.dsingh.wget;

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
        Logs.d("DManager: initPool()", "invoked");

        runnableQueue = new LinkedBlockingQueue<>();
        workQueue = new LinkedBlockingQueue<>();
        threadPool = new ThreadPoolExecutor(poolSize, poolSize, 0, TimeUnit.SECONDS, workQueue);
    }

    void shutdown() {

        Logs.w("DManager", "shutdown(): invoked");

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
            Logs.d("DManager", "getDownloadCount(): invoked, return " + i);
            return i;
        }
    }*/

    void cancelDownload(String downloadUid) {

        Logs.d("DManager", "cancelDownload(): invoked, UID: " + downloadUid);

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

        Logs.d("DManager", "cancelAll(): invoked");

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

    boolean removeFromQueue(DRunnable dRunnable) {

        boolean taskQ = runnableQueue.remove(dRunnable);
        Logs.d("DManager: removeDTaskFromQueue()", "invoked. Param - DTask:UID: " + dRunnable.getDownloadUid() + " ? removed: " + taskQ);

        if (taskQ)
            if (runnableQueue.size() == 0) {
                Logs.d("DManager", "stopService(): DManager.getDownloadCount() == 0");

                if (handler != null)
                    handler.obtainMessage(DService.MESSAGE_SHUTDOWN).sendToTarget();
            }

        return taskQ;
    }

    void progressUpdate(int what, DProgress dProgress) {

        if (handler != null)
            handler.obtainMessage(what, dProgress).sendToTarget();
    }

    public static DState getDownloadState(Context context, DBundle dBundle) {

        DState dState = getActiveDownloadState(dBundle);
        if (dState != null)
            return dState;

        String table = dBundle.isAudioOnly() ? DInfoHelper.TABLE_AUDIO : DInfoHelper.TABLE_VIDEO;
        String state = DInfoHelper.getInstance(context).getInfoState(table, dBundle.getDownloadUid());

        return DInfoHelper.getInactiveDStateFromString(state);
    }

    @Nullable
    public static DState getActiveDownloadState(DBundle dBundle) {

        //TODO
        /*
        synchronized (sInstance) {
            if (isAlive.get()) {
                DTask[] taskArray = new DTask[sInstance.mTaskWorkQueue.size()];
                sInstance.mTaskWorkQueue.toArray(taskArray);

                for (DTask task : taskArray)
                    if (task.getDownloadUid().equals(dBundle.getDownloadUid()))
                        return task.getDRunnable().getState();
            }
        }*/

        return null;
    }
}
