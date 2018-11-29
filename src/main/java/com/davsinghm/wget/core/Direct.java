package com.davsinghm.wget.core;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.davsinghm.wget.Logger;
import com.davsinghm.wget.core.info.DownloadInfo;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

public abstract class Direct {

    public static final int BUF_SIZE = 4 * 1024; // size of read buffer

    private Context context;
    private Uri directory;
    private String filename;
    private DownloadInfo info;
    private DocumentFile targetFile;

    public Direct(Context context, DownloadInfo info, Uri directory, String filename) {
        this.context = context;
        this.info = info;
        this.directory = directory;
        this.filename = filename;
    }

    protected synchronized DownloadInfo getInfo() {
        return info;
    }

    protected synchronized String getFilename() {
        return filename;
    }

    protected synchronized Context getContext() {
        return context;
    }

    public abstract void download(AtomicBoolean stop, Runnable notify);

    @Nullable
    public static DocumentFile findFile(DocumentFile directoryFile, @NonNull String displayName) {
        for (DocumentFile doc : directoryFile.listFiles())
            if (displayName.equalsIgnoreCase(doc.getName()))
                return doc;

        return null;
    }

    @Nullable
    DocumentFile getTargetFile() throws IOException {
        Logger.d("Direct", "Creating File: " + filename + "\nin URI: " + directory.toString());
        if (targetFile == null)
            if (directory != null)
                if (ContentResolver.SCHEME_FILE.equals(directory.getScheme())) {
                    DocumentFile directoryFile = DocumentFile.fromFile(new File(directory.getPath()));
                    if ((targetFile = findFile(directoryFile, filename)) == null)
                        if ((targetFile = directoryFile.createFile("", filename)) == null)
                            throw new IOException("Unable to create new file");
                } else {
                    DocumentFile directoryFile = DocumentFile.fromTreeUri(context, directory);
                    if (directoryFile != null && (targetFile = findFile(directoryFile, filename)) == null)
                        if ((targetFile = directoryFile.createFile("", filename)) == null)
                            throw new IOException("Unable to create new file");
                }

        return targetFile;
    }
}
