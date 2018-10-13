package com.dsingh.wget;
import android.os.Handler;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DManager {

    @NonNull
    private static final DManager sInstance;
    @Nullable
    private BlockingQueue<Runnable> mDownloadWorkQueue;
    @Nullable
    private Queue<DTask> mTaskWorkQueue;
    @Nullable
    private ThreadPoolExecutor mThreadPool;
    @NonNull
    private static AtomicBoolean isAlive = new AtomicBoolean(false);

    private Handler mHandler;

    static {
        sInstance = new DManager();
    }

    public static DManager getInstance() {
        return sInstance;
    }

    public void initPool() {
        Logs.d("DManager: initPool()", "invoked");

        int poolSize = Integer.valueOf(PreferenceHelper.getString(PreferenceHelper.DM_DOWNLOAD_COUNT, PreferenceHelper.DM_DOWNLOAD_COUNT_VALUE));
        mTaskWorkQueue = new LinkedBlockingQueue<>();
        mDownloadWorkQueue = new LinkedBlockingQueue<>();
        mThreadPool = new ThreadPoolExecutor(poolSize, poolSize, 0, TimeUnit.SECONDS, mDownloadWorkQueue);
    }

    public void reboot() {

        synchronized (sInstance) {
            Logs.i("DManager", "reboot(): invoked");

            initPool();
            isAlive.set(true);
        }
    }

    public static boolean isAlive() { //TODO remove!!
        synchronized (sInstance) {
            return isAlive.get();
        }
    }

    public void shutdown() {

        synchronized (sInstance) {

            Logs.w("DManager", "shutdown(): invoked");

            cancelAll();

            isAlive.set(false);

            mThreadPool.shutdown();

            mThreadPool = null;
            mDownloadWorkQueue = null;
            mTaskWorkQueue = null;
            mHandler = null;
        }
    }

    public boolean queueDownload(DBundle dBundle) {

        synchronized (sInstance) {

            if (!isAlive.get()) {
                Logs.wtf("DManager", "WTF! NotAlive: startDownload() is called. DBundle:UID: " + dBundle.getDownloadUid());
                throw new IllegalStateException("DManager: WTF! NotAlive: startDownload() is called. DBundle:UID: " + dBundle.getDownloadUid());
            }

            DTask dTask = new DTask(this, dBundle);

            if (mTaskWorkQueue.contains(dTask))
                return false;

            mTaskWorkQueue.add(dTask);
            mThreadPool.execute(dTask.getDRunnable());

            Broadcaster.onBundleQueued(dBundle.getDownloadUid());

            return true;
        }
    }

    public static int getDownloadCount() {

        synchronized (sInstance) {
            if (!isAlive.get()) {
                Logs.e("DManager", "NotAlive: getDownloadCount() is called. return 0.");
                return 0;
            }

            int i = sInstance.mTaskWorkQueue.size();
            Logs.d("DManager", "getDownloadCount(): invoked, return " + i);
            return i;
        }
    }

    public static void cancelDownload(String downloadUID) {
        synchronized (sInstance) {

            if (!isAlive.get()) {
                Logs.e("DManager", "NotAlive: cancelDownload() is called. Param - UID: " + downloadUID);
                return;
            }

            Logs.d("DManager", "cancelDownload(): invoked, Param - UID: " + downloadUID);

            DTask[] taskArray = new DTask[sInstance.mTaskWorkQueue.size()];
            sInstance.mTaskWorkQueue.toArray(taskArray);

            if (taskArray.length == 0) {
                Logs.d("DManager", "cancelDownload(): Queue is empty, skip");
                return;
            }

            for (DTask dTask : taskArray)
                if (dTask.getDownloadUid().equals(downloadUID)) {
                    if (dTask.getCurrentThread() == null) {
                        sInstance.mThreadPool.remove(dTask.getDRunnable());
                        sInstance.mTaskWorkQueue.remove(dTask);
                        Broadcaster.onBundleRemoved(downloadUID);
                    }
                    dTask.stopDownload();
                }
        }

    }

    public static void cancelAll() {
        synchronized (sInstance) {

            if (!isAlive.get()) {
                Logs.e("DManager", "NotAlive: cancelAll() is called.");
                return;
            }

            Logs.d("DManager", "cancelAll(): invoked");

            DTask[] taskArray = new DTask[sInstance.mTaskWorkQueue.size()];
            sInstance.mTaskWorkQueue.toArray(taskArray);

            if (taskArray.length == 0) {
                Logs.e("DManager", "cancelAll(): Queue is empty, skip");
                return;
            }

            for (DTask dTask : taskArray) {
                dTask.stopDownload();
                if (dTask.getCurrentThread() == null) {
                    boolean pool = sInstance.mThreadPool.remove(dTask.getDRunnable());
                    Logs.d("DManager", "cancelAll(): UID: " + dTask.getDownloadUid() + ": ? removed Runnable from the Thread Pool: " + pool);
                    boolean taskQ = sInstance.mTaskWorkQueue.remove(dTask);
                    Logs.d("DManager", "cancelAll(): UID: " + dTask.getDownloadUid() + ": ? removed Task from the Queue: " + taskQ);

                    //if thread is null, it means DRunnable was not started, if so, we need to set the state to STOPPED.
                    //if it got started there could be a problem.
                    //FIXME what if we do this after calling mThreadPool shutdown Now? read docs.
                    Broadcaster.onBundleRemoved(dTask.getDownloadUid());
                }
            }
            //Broadcaster.onAllDownloadsCancelled();
        }
    }


    public boolean removeDTaskFromQueue(DTask dTask) {

        synchronized (sInstance) {

            if (!isAlive.get()) {
                Logs.e("DManager", "NotAlive: removeDTaskFromQueue() is called. Param - DTask:UID: " + dTask.getDownloadUid());
                return false;
            }

            boolean taskQ = mTaskWorkQueue.remove(dTask);
            Logs.d("DManager: removeDTaskFromQueue()", "invoked. Param - DTask:UID: " + dTask.getDownloadUid() + " ? removed: " + taskQ);
            return taskQ;
        }
    }

    public void progressUpdate(int onGoing, DProgress dProgress) {

        synchronized (sInstance) {

            if (!isAlive.get()) {
                Logs.e("DManager", "NotAlive: progressUpdate() is called. Param - onGoing: " + onGoing + ", DProgress:UID: " + dProgress.getDownloadUID());
                return;
            }

            mHandler.obtainMessage(onGoing, dProgress).sendToTarget();
        }

    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    public static DState getDownloadState(DBundle dBundle) {

        DState dState = getActiveDownloadState(dBundle);
        if (dState != null)
            return dState;

        String table = dBundle.isAudioOnly() ? DInfoHelper.TABLE_AUDIO : DInfoHelper.TABLE_VIDEO;
        String state = DInfoHelper.getInstance().getInfoState(table, dBundle.getDownloadUid());

        return DInfoHelper.getInactiveDStateFromString(state);
    }

    @Nullable
    public static DState getActiveDownloadState(DBundle dBundle) {

        synchronized (sInstance) {
            if (isAlive.get()) {
                DTask[] taskArray = new DTask[sInstance.mTaskWorkQueue.size()];
                sInstance.mTaskWorkQueue.toArray(taskArray);

                for (DTask task : taskArray)
                    if (task.getDownloadUid().equals(dBundle.getDownloadUid()))
                        return task.getDRunnable().getState();
            }
        }

        return null;
    }

}
