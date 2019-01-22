package com.davsinghm.wget.core.io;

import java.io.Closeable;
import java.io.IOException;

public interface RandomAccessUri extends Closeable {

    void write(byte[] b, int off, int len) throws IOException;

    void seek(long pos) throws IOException;

    /**
     * Returns the length of this file.
     *
     * @return the length of this file, measured in bytes.
     * @throws IOException if an I/O error occurs.
     */
    long length() throws IOException;

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
    @Override
    void close() throws IOException;
}
