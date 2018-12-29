package com.davsinghm.wget;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

public class DocFile {

    @NonNull
    private DocumentFile documentFile;
    @NonNull
    private Context context;

    private DocFile(@NonNull Context context, @NonNull DocumentFile documentFile) {
        this.documentFile = documentFile;
        this.context = context;
    }

    @Nullable
    public static DocFile fromTreeUri(Context context, @Nullable Uri uri) {
        if (uri != null) {
            if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                return new DocFile(context, DocumentFile.fromFile(new File(uri.getPath())));
            } else {
                DocumentFile documentFile = DocumentFile.fromTreeUri(context, uri);
                if (documentFile != null)
                    return new DocFile(context, documentFile);
            }
        }

        return null;
    }

    /**
     * @return new file or null if failed (either all files exists or io error)
     */
    @Nullable
    public DocFile createAutoRenamedFileWithExt(@NonNull String filename, @NonNull String extension) {

        String newDisplayName = getAutoRenamedDisplayName(filename, extension);
        if (newDisplayName != null) {
            return createFileWithExt(newDisplayName, extension);
        }

        return null;
    }

    /**
     * @return null if all file names are used up.
     */
    @Nullable
    public String getAutoRenamedDisplayName(@NonNull String displayName, @NonNull String extension) {

        //TODO optimize. this is apparently not so helpful, the getName actually calls the internal api, it'll be great if array is of names instead
        DocumentFile[] files = documentFile.listFiles();

        int i;
        for (i = 0; i < 11; i++) {
            String suffix = i == 0 ? "" : " (" + i + ")";
            String filename = displayName + suffix + extension;

            boolean found = false;
            for (DocumentFile file : files)
                if (filename.equalsIgnoreCase(file.getName())) {
                    found = true;
                    break;
                }

            if (!found) {
                displayName = displayName + suffix;
                break;
            }
        }

        if (i < 11)
            return displayName;

        return null;
    }

    /**
     * this tries to create file with give extension. problem is MimeTypeMap is not one to one map
     * <p>
     * Create a new document as a direct child of this directory.
     *
     * @param displayName name of new document, without any file extension appended
     * @param extension   extension of the file
     * @return file representing newly created document, or null if failed
     * @see android.provider.DocumentsContract#createDocument(ContentResolver, Uri, String, String)
     */
    @Nullable
    public DocFile createFileWithExt(@NonNull String displayName, @Nullable String extension) {

        String mimeType = null;
        String extFromMap = null;
        String mimeTypeFromMap;

        //checks if map returns same extension from mime type
        if ((mimeTypeFromMap = extension != null ? MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) : null) != null)
            if (extension.equals(extFromMap = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeTypeFromMap)))
                mimeType = mimeTypeFromMap;

        if (mimeType == null) {
            mimeType = "application/octet-stream";

            if (extension != null)
                displayName = displayName + "." + extension;
        }

        DocFile newFile = createFileWithType(mimeType, displayName);
        //check if extension is same
        if (newFile != null) {
            String name = newFile.getName();
            if (name == null || (!name.endsWith("." + extension) || name.endsWith("." + extension + "." + extension))) {
                newFile.delete();
                throw new AssertionError("The extension appended by the system doesn't match provided extension. DisplayName: " + displayName + "\ngetName(): " + name + "\nExt: " + extension + ", MimeType: " + mimeType + ", ExtFromMap: " + extFromMap + ", MimeFromMap: " + mimeTypeFromMap);
            }
        }

        return newFile;
    }


    @Nullable
    public DocFile createFileWithType(@NonNull String mimeType, @NonNull String displayName) {
        DocumentFile newFile = documentFile.createFile(mimeType, displayName);
        if (newFile != null)
            return new DocFile(context, newFile);

        return null;
    }

    @Nullable
    public DocumentFile createDirectory(@NonNull String displayName) {
        return documentFile.createDirectory(displayName);
    }

    @NonNull
    public Uri getUri() {
        return documentFile.getUri();
    }

    @Nullable
    public String getName() {
        return documentFile.getName();
    }

    @Nullable
    public String getType() {
        return documentFile.getType();
    }

    @Nullable
    public DocFile getParentFile() {
        DocumentFile parent = documentFile.getParentFile();
        if (parent != null)
            return new DocFile(context, parent);

        return null;
    }

    public boolean isDirectory() {
        return documentFile.isDirectory();
    }

    public boolean isFile() {
        return documentFile.isFile();
    }

    public boolean isVirtual() {
        return documentFile.isVirtual();
    }

    public long lastModified() {
        return documentFile.lastModified();
    }

    public long length() {
        return documentFile.length();
    }

    public boolean canRead() {
        return documentFile.canRead();
    }

    public boolean canWrite() {
        return documentFile.canWrite();
    }

    public boolean delete() {
        return documentFile.delete();
    }

    public boolean exists() {
        return documentFile.exists();
    }

    @NonNull
    public DocFile[] listFiles() {
        final ArrayList<DocFile> results = new ArrayList<>();
        final DocumentFile[] files = documentFile.listFiles();
        for (DocumentFile file : files) {
            results.add(new DocFile(context, file));
        }
        return results.toArray(new DocFile[results.size()]);
    }

    @Nullable
    public DocFile findFile(@NonNull String displayName) {
        for (DocumentFile doc : documentFile.listFiles())
            if (displayName.equals(doc.getName()))
                return new DocFile(context, doc);

        return null;
    }

    @Nullable
    public DocFile findFileIgnoreCase(@NonNull String displayName) {
        for (DocumentFile doc : documentFile.listFiles())
            if (displayName.equalsIgnoreCase(doc.getName()))
                return new DocFile(context, doc);

        return null;
    }

    public boolean renameTo(@NonNull String displayName) {
        return documentFile.renameTo(displayName);
    }
}
