package com.davsinghm.wget.core.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class RandomAccessRawFile extends RandomAccessFile implements RandomAccessUri {

    RandomAccessRawFile(String name, String mode) throws FileNotFoundException {
        super(name, mode);
    }

    RandomAccessRawFile(File file, String mode) throws FileNotFoundException {
        super(file, mode);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);
    }

    @Override
    public void seek(long pos) throws IOException {
        super.seek(pos);
    }

    @Override
    public long length() throws IOException {
        return super.length();
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}
