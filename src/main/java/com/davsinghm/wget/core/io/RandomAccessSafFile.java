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
     * Sets this channel's file position.
     *
     * <p> Setting the position to a value that is greater than the file's
     * current size is legal but does not change the size of the file.  A later
     * attempt to read bytes at such a position will immediately return an
     * end-of-file indication.  A later attempt to write bytes at such a
     * position will cause the file to be grown to accommodate the new bytes;
     * the values of any bytes between the previous end-of-file and the
     * newly-written bytes are unspecified. </p>
     *
     * @param pos The new position, a non-negative integer counting
     *            the number of bytes from the beginning of the file
     * @throws ClosedChannelException   If this channel is closed
     * @throws IllegalArgumentException If the new position is negative
     * @throws IOException              If some other I/O error occurs
     */
    public void seek(long pos) throws IOException {
        fileChannel.position(pos);
    }

    /**
     * @throws InterruptedIOException When thread is interrupted while blocked in an I/O operation
     *                                upon a channel. Before this exception is thrown the channel
     *                                will have been closed and the interrupt status of the
     *                                previously-blocked thread will have been set.
     * @throws IOException            If some other I/O error occurs
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
     * Closes this channel.
     *
     * <p> If the channel has already been closed then this method returns immediately.
     * Otherwise it marks the channel as closed and then invokes the
     * {@link FileChannel#implCloseChannel implCloseChannel} method in order to complete the close
     * operation. </p>
     *
     * @throws IOException If an I/O error occurs
     */
    public void close() throws IOException {
        if (fileChannel.isOpen())
            fileChannel.close();
        parcelFileDescriptor.close();
    }
}
