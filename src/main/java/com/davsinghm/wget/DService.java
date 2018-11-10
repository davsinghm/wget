package com.davsinghm.wget;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.Nullable;

public abstract class DService extends Service {

    private Handler handler;
    private DManager dManager;

    protected static final String EXTRA_DBUNDLE = "DBundle";
    protected static final String EXTRA_DOWNLOAD_UID = "DownloadUid";
    public static final String ACTION_DOWNLOAD_SERVICE_CANCEL_DOWNLOAD = DService.class.getSimpleName() + ".action.CANCEL_DOWNLOAD";
    public static final String ACTION_DOWNLOAD_SERVICE_CANCEL_ALL = DService.class.getSimpleName() + ".action.CANCEL_ALL";

    static final int MESSAGE_PROGRESS_ENDED = 0;
    static final int MESSAGE_PROGRESS_ONGOING = 1;
    static final int MESSAGE_SHUTDOWN = 2;

    @Nullable
    DState getDState(String downloadUid) {
        if (dManager != null)
            return dManager.getDState(downloadUid);

        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new IncomingHandler(Looper.myLooper());

        dManager = new DManager(this, getPoolSize());
        dManager.setHandler(handler);
        dManager.setService(this);

        DownloadManager.dService = this;

        startForeground(getForegroundNotificationId(), getForegroundNotification());
    }

    public static void setLoggerCallback(Logger.Callback callback) {
        Logger.callback = callback;
    }

    public abstract void onRepeatedBundleAdded(DBundle dBundle);

    public abstract void onBundleQueued(DBundle dBundle);

    public abstract void onBundleRemoved(DBundle dBundle);

    public abstract int getPoolSize();

    public abstract Notification getForegroundNotification();

    public abstract int getForegroundNotificationId();

    public abstract void onDownloadManagerShutdown();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (ACTION_DOWNLOAD_SERVICE_CANCEL_DOWNLOAD.equals(intent.getAction())) {

            dManager.cancelDownload(intent.getStringExtra(EXTRA_DOWNLOAD_UID));

            return START_NOT_STICKY;

        } else if (ACTION_DOWNLOAD_SERVICE_CANCEL_ALL.equals(intent.getAction())) {

            dManager.cancelAll();

            return START_NOT_STICKY;
        }

        DBundle dBundle = intent.getParcelableExtra(EXTRA_DBUNDLE);

        if (dBundle != null) {
            Logger.d("DService: onStartCommand()", "Got DBundle: " + dBundle.getDownloadUid());

            if (dManager.queueDownload(dBundle))
                onBundleQueued(dBundle);
            else
                onRepeatedBundleAdded(dBundle);
        }

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Logger.w("DService", "onDestroy(): invoked");

        DownloadManager.dService = null;

        dManager.shutdown();

        onDownloadManagerShutdown();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public abstract void onProgressUpdated(DProgress dProgress, boolean isOnGoing);

    private class IncomingHandler extends Handler {

        private IncomingHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case MESSAGE_SHUTDOWN:
                    stopSelf();
                    break;
                case MESSAGE_PROGRESS_ONGOING:
                    onProgressUpdated((DProgress) msg.obj, true);
                    break;
                case MESSAGE_PROGRESS_ENDED:
                    onProgressUpdated((DProgress) msg.obj, false);
                    break;
            }
        }
    }

}
