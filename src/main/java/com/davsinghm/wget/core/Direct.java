package com.davsinghm.wget.core;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

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

    protected synchronized Context getContext() {
        return context;
    }

    public abstract void download(AtomicBoolean stop, Runnable notify);

    /**
     * this is need as {@link androidx.documentfile.provider.RawDocumentFile} appends the extension
     * without checking if the file already has it, while internal document provider doesn't
     * <p>
     * Create a new document as a direct child of this directory.
     *
     * @param displayName name of new document, without any file extension appended
     * @param extension   extension of the file
     * @return file representing newly created document, or null if failed
     * @throws UnsupportedOperationException when working with a single document created
     *                                       from {@link androidx.documentfile.provider.DocumentFile#fromSingleUri(Context, Uri)}.
     * @see android.provider.DocumentsContract#createDocument(ContentResolver, Uri, String, String)
     */
    @Nullable
    public static DocumentFile createFile(@NonNull DocumentFile documentFile, @NonNull String displayName, @Nullable String extension) {

        String mimeType;
        String mimeFromMap = extension != null ? MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) : null;
        String extFromMap = null;
        boolean extMismatch = false;

        if ((mimeType = mimeFromMap) != null) {
            extFromMap = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (!extension.equals(extFromMap)) {
                extMismatch = true;
                mimeType = null;
            }
        }

        if (mimeType == null) {
            if (extension != null)
                displayName = displayName + "." + extension;

            mimeType = "application/octet-stream";
        }

        DocumentFile newFile = documentFile.createFile(mimeType, displayName);
        if (newFile != null) {
            String name = newFile.getName();
            if (name == null || (!name.endsWith("." + extension) || name.endsWith("." + extension + "." + extension))) {
                newFile.delete(); //delete the empty file
                throw new AssertionError("Fatal/Critical: The extension appended by the system doesn't match provided extension. DisplayName: " + displayName + "\ngetName(): " + name + "\nExt: " + extension + ", MimeType: " + mimeType + ", ExtMismatch: " + extMismatch + ", ExtFromMap: " + extFromMap + ", MimeFromMap: " + mimeFromMap);
            }
        }

        return newFile;
    }

    /**
     * this is needed as {@link androidx.documentfile.provider.DocumentFile#findFile(String)} is case sensitive
     */
    @Nullable
    public static DocumentFile findFile(DocumentFile directoryFile, @NonNull String displayName) {
        for (DocumentFile doc : directoryFile.listFiles())
            if (displayName.equalsIgnoreCase(doc.getName()))
                return doc;

        return null;
    }

    @Nullable
    public static String getExtension(@NonNull String filename) {
        int index = filename.lastIndexOf(".");
        if (index == -1)
            return null;
        else
            return filename.substring(index + 1);
    }

    public static String getBaseName(@NonNull String filename) {
        int index = filename.lastIndexOf(".");
        if (index == -1)
            return filename;
        else
            return filename.substring(0, index);
    }

    @Nullable
    DocumentFile getTargetFile() throws IOException {
        Logger.d("Direct", "Creating File: " + filename + "\nin Directory URI: " + directory.toString());
        if (targetFile == null)
            if (directory != null)
                if (ContentResolver.SCHEME_FILE.equals(directory.getScheme())) {
                    DocumentFile directoryFile = DocumentFile.fromFile(new File(directory.getPath()));
                    if ((targetFile = findFile(directoryFile, filename)) == null)
                        if ((targetFile = createFile(directoryFile, getBaseName(filename), getExtension(filename))) == null)
                            throw new IOException("Unable to create new file");
                } else {
                    DocumentFile directoryFile = DocumentFile.fromTreeUri(context, directory);
                    if (directoryFile != null && (targetFile = findFile(directoryFile, filename)) == null)
                        if ((targetFile = createFile(directoryFile, getBaseName(filename), getExtension(filename))) == null)
                            throw new IOException("Unable to create new file");
                }

        return targetFile;
    }
}
