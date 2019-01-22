package com.davsinghm.wget.core.io;

import android.content.ContentProvider;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.davsinghm.wget.core.Direct;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;

import androidx.annotation.NonNull;

public class RandomAccessSafFile implements RandomAccessUri {

    private ParcelFileDescriptor parcelFileDescriptor;
    private FileDescriptor fileDescriptor;
    private FileChannel fileChannel;
    private ByteBuffer byteBuffer;

    /**
     * @param context Context
     * @param uri     The desired URI to open.
     * @param mode    The file mode to use, as per {@link ContentProvider#openFile
     *                ContentProvider.openFile}.
     * @throws FileNotFoundException Throws FileNotFoundException if no
     *                               file exists under the URI or the mode is invalid.
     */
    RandomAccessSafFile(Context context, @NonNull Uri uri, String mode) throws FileNotFoundException {
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
     * @throws InterruptedIOException (ClosedByInterruptException) When thread is interrupted while
     *                                blocked in an I/O operation upon a channel. Before this
     *                                exception is thrown the channel will have been closed and the
     *                                interrupt status of the previously-blocked thread will have
     *                                been set.
     */
    public void write(byte b[], int off, int len) throws IOException {
        try {
            byteBuffer.clear();
            byteBuffer.put(b, off, len);
            byteBuffer.flip();
            while (byteBuffer.hasRemaining())
                fileChannel.write(byteBuffer);
        } catch (ClosedByInterruptException e) {
            throw new InterruptedIOException(e.getMessage());
        }
    }

    /**
     * Returns the current size of this channel's file.
     *
     * @return The current size of this channel's file, measured in bytes
     * @throws ClosedChannelException If this channel is closed
     * @throws IOException            If some other I/O error occurs
     */
    public long length() throws IOException {
        return fileChannel.size();
    }

    /**
     * Closes this random access file stream and releases any system resources associated with the
     * stream. A closed random access file cannot perform input or output operations and cannot be
     * reopened.
     *
     * <p> If this file has an associated channel then the channel is closed as well.
     *
     * @throws IOException if an I/O error occurs.
     */
    public void close() throws IOException {
        if (fileChannel.isOpen())
            fileChannel.close();
        parcelFileDescriptor.close();
    }
}
