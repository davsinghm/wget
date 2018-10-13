package com.dsingh.wget.core;

import com.dsingh.wget.Constants;
import com.dsingh.wget.core.info.DownloadInfo;
import com.dsingh.wget.core.info.State;
import com.dsingh.wget.core.info.ex.DownloadInterruptedError;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public class DirectSingle extends Direct {

    /**
     * @param info   download file information
     * @param target target file
     */
    public DirectSingle(DownloadInfo info, File target) {
        super(info, target);
    }

    void downloadPart(DownloadInfo info, AtomicBoolean stop, Runnable notify) throws IOException {
        RandomAccessFile randomAccessFile = null;
        BufferedInputStream bufferedInputStream = null;

        try {
            URL url = info.getSource();

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setConnectTimeout(Constants.WGET_CONNECT_TIMEOUT);
            urlConnection.setReadTimeout(Constants.WGET_READ_TIMEOUT);

            urlConnection.setRequestProperty("User-Agent", info.getUserAgent()); //NON-NLS
            if (info.getReferrer() != null)
                urlConnection.setRequestProperty("Referer", info.getReferrer().toExternalForm());

            getTarget().createNewFile();
            info.setCount(0);
            info.getSpeedInfo().start(0);

            randomAccessFile = new RandomAccessFile(getTarget(), "rw");

            RetryWrap.checkResponse(urlConnection);

            bufferedInputStream = new BufferedInputStream(urlConnection.getInputStream());

            byte[] bytes = new byte[BUF_SIZE];
            int read;
            while ((read = bufferedInputStream.read(bytes)) > 0) {
                randomAccessFile.write(bytes, 0, read);

                info.setCount(info.getCount() + read);
                notify.run();
                info.updateSpeed();

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
    public void download(final AtomicBoolean stop, final Runnable notify) {

        try {
            RetryWrap.run(stop, new RetryWrap.Wrap() {

                @Override
                public Object download() throws IOException {
                    getInfo().setState(State.DOWNLOADING);
                    notify.run();

                    downloadPart(getInfo(), stop, notify);
                    return null;
                }

                @Override
                public void retry(int delay, Throwable e) {
                    getInfo().setDelay(delay, e);
                    notify.run();
                }

                @Override
                public void moved(URL url) {
                    getInfo().setState(State.RETRYING);
                    notify.run();
                }
            });

            getInfo().setState(State.DONE);
            notify.run();
        } catch (DownloadInterruptedError e) {
            getInfo().setState(State.STOP);
            notify.run();

            throw e;
        } catch (RuntimeException e) {
            getInfo().setState(State.ERROR);
            notify.run();

            throw e;
        }
    }

    /**
     * check existing file for download resume. for single download it will
     * check file dose not exist or zero size. so we can resume download.
     *
     * @param info
     * @param targetFile
     * @return return true - if all ok, false - if download can not be restored.
     */
    public static boolean canResume(DownloadInfo info, File targetFile) {
        if (info.getCount() != 0)
            return false;

        if (targetFile.exists()) {
            if (targetFile.length() != 0)
                return false;
        }

        return true;
    }
}
