package com.davsinghm.wget.core.io;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileNotFoundException;

import androidx.annotation.NonNull;

public class Utils {

    public static RandomAccessUri openUriFile(@NonNull Context context, @NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            return new RandomAccessRawFile(new File(uri.getPath()), mode);
        } else {
            if (!ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()))
                throw new AssertionError("Uri.getScheme() is unknown: Uri: " + uri);
            return new RandomAccessSafFile(context, uri, mode);
        }
    }
}
