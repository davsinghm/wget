package com.davsinghm.wget;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DownloadManager {

    static DService dService;

    @NonNull
    public static DState getDownloadState(Context context, DBundle dBundle) {

        DState dState = getActiveDownloadState(dBundle.getDownloadUid());
        if (dState != null)
            return dState;

        String table = dBundle.isAudioOnly() ? DInfoHelper.TABLE_AUDIO : DInfoHelper.TABLE_VIDEO;
        String state = DInfoHelper.getInstance(context).getInfoState(table, dBundle.getDownloadUid());

        return DInfoHelper.getInactiveDStateFromString(state);
    }

    @Nullable
    public static DState getActiveDownloadState(String downloadUid) {

        if (dService != null) {
            return dService.getDState(downloadUid);
        }

        return null;
    }
}
