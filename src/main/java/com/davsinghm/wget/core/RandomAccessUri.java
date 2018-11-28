package com.davsinghm.wget.core;

import android.content.ContentProvider;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import androidx.annotation.NonNull;

public class RandomAccessUri {

    private ParcelFileDescriptor parcelFileDescriptor;
    private FileDescriptor fileDescriptor;
    private FileChannel fileChannel;
    private ByteBuffer byteBuffer;

    /**
     * @param context
     * @param uri     The desired URI to open.
     * @param mode    The file mode to use, as per {@link ContentProvider#openFile
     *                ContentProvider.openFile}.
     * @throws FileNotFoundException Throws FileNotFoundException if no
     *                               file exists under the URI or the mode is invalid.
     */
    public RandomAccessUri(Context context, @NonNull Uri uri, String mode) throws FileNotFoundException {
        parcelFileDescriptor = context.getContentResolver().openFileDescriptor(uri, mode);

        if (parcelFileDescriptor == null)
            throw new NullPointerException("ParcelFileDescriptor == null");

        fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        fileChannel = new FileOutputStream(fileDescriptor).getChannel(); //TODO

        byteBuffer = ByteBuffer.allocate(Direct.BUF_SIZE); //TODO
    }

    /**
     * Sets the file-pointer offset, measured from the beginning of this
     * file, at which the next read or write occurs.  The offset may be
     * set beyond the end of the file. Setting the offset beyond the end
     * of the file does not change the file length.  The file length will
     * change only by writing after the offset has been set beyond the end
     * of the file.
     *
     * @param pos the offset position, measured in bytes from the
     *            beginning of the file, at which to set the file
     *            pointer.
     * @throws IOException if {@code pos} is less than
     *                     {@code 0} or if an I/O error occurs.
     */
    public void seek(long pos) throws IOException {
        fileChannel.position(pos);
    }

    /**
     * Writes {@code len} bytes from the specified byte array
     * starting at offset {@code off} to this file.
     *
     * @param b   the data.
     * @param off the start offset in the data.
     * @param len the number of bytes to write.
     * @throws IOException if an I/O error occurs.
     */
    public void write(byte b[], int off, int len) throws IOException {
        byteBuffer.clear();
        byteBuffer.put(b, off, len);
        byteBuffer.flip();
        while (byteBuffer.hasRemaining())
            fileChannel.write(byteBuffer);
    }

    /**
     * Returns the length of this file.
     *
     * @return the length of this file, measured in bytes.
     * @throws IOException if an I/O error occurs.
     */
    public long length() throws IOException {
        return fileChannel.size();
    }

    /**
     * Closes this random access file stream and releases any system
     * resources associated with the stream. A closed random access
     * file cannot perform input or output operations and cannot be
     * reopened.
     *
     * <p> If this file has an associated channel then the channel is closed
     * as well.
     *
     * @throws IOException if an I/O error occurs.
     */
    public void close() throws IOException {
        //fileDescriptor.sync(); TODO
        //fileChannel.force(false);
        fileChannel.close();
        parcelFileDescriptor.close();
    }
}
