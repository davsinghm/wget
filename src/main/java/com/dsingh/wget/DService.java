package com.dsingh.wget;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

public abstract class DService extends Service {

    private Handler handler = new IncomingHandler(Looper.myLooper());

    private static final String EXTRA_DBUNDLE = "DBundle";

    public static final String ACTION_DMANAGER_CANCEL_ALL = DService.class.getSimpleName() + ".ACTION.CANCEL_ALL";

    public static void startDownload(Context context, DBundle dBundle) {
        Intent intent = new Intent(context, DService.class);
        intent.putExtra(EXTRA_DBUNDLE, dBundle);
        context.startService(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (!DManager.isAlive())
            DManager.getInstance().reboot();

        DManager.getInstance().setHandler(handler);

        startForeground(Constants.DSERVICE_FG_NOTI_ID, getForegroundNotification());
    }

    public abstract Notification getForegroundNotification();

    public abstract void onRepeatedBundleAdded();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (ACTION_DMANAGER_CANCEL_ALL.equals(intent.getAction())) {
            DManager.cancelAll();

            return START_NOT_STICKY;
        }

        DBundle bundle = intent.getParcelableExtra(EXTRA_DBUNDLE);
        if (bundle != null) {
            Logs.d("DService: onStartCommand()", "Got DBundle: " + bundle.getDownloadUid());
            boolean isAdded = DManager.getInstance().queueDownload(bundle);
            if (!isAdded)
                onRepeatedBundleAdded();
        } else
            Logs.wtf("DService: onStartCommand()", "DBundle: Intent.ParcelableExtra is null", new IllegalArgumentException("Non-Fatal: DService: onStartCommand(): DBundle: Intent.ParcelableExtra is null"));

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Logs.w("DService", "onDestroy(): invoked");

        DManager.getInstance().shutdown();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    public abstract void onProgressUpdated(DProgress dProgress, boolean isOnGoing);

    private class IncomingHandler extends Handler {

        IncomingHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            onProgressUpdated((DProgress) msg.obj, msg.what == 1);
        }
    }

}
