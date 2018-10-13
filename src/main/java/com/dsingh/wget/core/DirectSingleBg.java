package com.dsingh.wget.core;

import com.dsingh.wget.Constants;
import com.dsingh.wget.core.info.ex.DownloadInterruptedError;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public class DirectSingleBg extends Direct {

    private URL url;

    public DirectSingleBg(String url, File target) throws MalformedURLException {
        super(null, target);
        this.url = new URL(url);
    }

    public void downloadPart(AtomicBoolean stop) throws IOException {
        RandomAccessFile randomAccessFile = null;
        BufferedInputStream bufferedInputStream = null;

        try {

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setConnectTimeout(Constants.WGET_CONNECT_TIMEOUT);
            urlConnection.setReadTimeout(Constants.WGET_READ_TIMEOUT);

            urlConnection.setRequestProperty("User-Agent", Constants.USER_AGENT); //NON-NLS

            RetryWrap.checkResponse(urlConnection);

            bufferedInputStream = new BufferedInputStream(urlConnection.getInputStream());

            getTarget().createNewFile();

            randomAccessFile = new RandomAccessFile(getTarget(), "rw");

            byte[] bytes = new byte[BUF_SIZE];
            int read;
            while ((read = bufferedInputStream.read(bytes)) > 0) {
                randomAccessFile.write(bytes, 0, read);

                if (stop.get())
                    throw new DownloadInterruptedError("Stopped");
                if (Thread.interrupted())
                    throw new DownloadInterruptedError("Interrupted");
            }

        } finally {
            if (randomAccessFile != null)
                randomAccessFile.close();
            if (bufferedInputStream != null)
                bufferedInputStream.close();
        }
    }

    @Override
    public void download(AtomicBoolean stop, Runnable notify) {

    }
}
