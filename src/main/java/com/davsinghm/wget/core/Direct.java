package com.davsinghm.wget.core;

import android.content.Context;
import android.net.Uri;

import com.davsinghm.wget.DocFile;
import com.davsinghm.wget.Logger;
import com.davsinghm.wget.core.info.DownloadInfo;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class Direct {

    public static final int BUF_SIZE = 4 * 1024; // size of read buffer

    private Context context;
    private Uri directory;
    private String filename;
    private DownloadInfo info;
    private DocFile targetFile;
    private Uri targetUri;

    public Direct(Context context, DownloadInfo info, Uri directory, String filename) {
        this.context = context;
        this.info = info;
        this.directory = directory;
        this.filename = filename;
    }

    public Direct(Context context, DownloadInfo info, Uri targetUri) {
        this.context = context;
        this.info = info;
        this.targetUri = targetUri;
    }

    protected synchronized DownloadInfo getInfo() {
        return info;
    }

    protected synchronized Context getContext() {
        return context;
    }

    public abstract void download(AtomicBoolean stop, Runnable notify);

    @Nullable
    private String getExtension(@NonNull String filename) {
        int index = filename.lastIndexOf(".");
        if (index == -1)
            return null;
        else
            return filename.substring(index + 1);
    }

    private String getBaseName(@NonNull String filename) {
        int index = filename.lastIndexOf(".");
        if (index == -1)
            return filename;
        else
            return filename.substring(0, index);
    }

    @Nullable
    synchronized DocFile getTargetFile() throws IOException {

        Logger.d("Direct", "Creating File: " + filename + "\nin Directory URI: " + directory.toString());

        if (targetFile == null) {
            DocFile dirFile = DocFile.fromTreeUri(context, directory);
            if (dirFile != null && (targetFile = dirFile.findFile(filename)) == null)
                if ((targetFile = dirFile.createFileWithExt(getBaseName(filename), getExtension(filename))) == null)
                    throw new IOException("Unable to create new file");
        }

        return targetFile;
    }

    synchronized Uri getTargetUri() {
        return targetUri;
    }
}
