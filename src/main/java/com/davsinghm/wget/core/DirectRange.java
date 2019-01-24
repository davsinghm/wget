package com.davsinghm.wget.core;

import android.content.Context;
import android.net.Uri;

import com.davsinghm.wget.Constants;
import com.davsinghm.wget.core.info.DownloadInfo;
import com.davsinghm.wget.core.info.State;
import com.davsinghm.wget.core.info.ex.DownloadInterruptedError;
import com.davsinghm.wget.core.io.RandomAccessUri;
import com.davsinghm.wget.core.io.Utils;
import com.davsinghm.wget.core.util.HttpUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public class DirectRange extends Direct {

    public DirectRange(Context context, DownloadInfo info, Uri directory, String filename) {
        super(context, info, directory, filename);
    }

    public void downloadPart(DownloadInfo info, AtomicBoolean stop, Runnable notify) throws IOException {

        RandomAccessUri randomAccessUri = null;
        BufferedInputStream bufferedInputStream = null;

        try {

            randomAccessUri = Utils.openUriFile(getContext(), getTargetFile().getUri(), "rw");
            /*
                TODO fix bug, if DefaultSetting is Multipart: off, if download is resuming
                previously multipart on, if exception occurred (if file length from sever is different than last time [stored in db]) in  fromString() download will resumed with checking file size,
                and half of the file will left empty
                or if file lengths are different they will be appended than restart
                solution: force restart, use flag
            */
            info.setCount(randomAccessUri.length());

            info.getSpeedInfo().start(info.getCount());

            if (info.getCount() >= info.getLength()) {
                notify.run();
                return;
            }

            randomAccessUri.seek(info.getCount());

            HttpURLConnection urlConnection = HttpUtils.openConnection(info, info.getCount());
            HttpUtils.checkResponse(urlConnection);
            bufferedInputStream = new BufferedInputStream(urlConnection.getInputStream());

            byte[] bytes = new byte[Constants.BUF_SIZE];
            int read;
            while ((read = bufferedInputStream.read(bytes)) > 0) {
                randomAccessUri.write(bytes, 0, read);

                info.setCount(info.getCount() + read);
                notify.run();
                info.updateSpeed();

                if (stop.get())
                    throw new DownloadInterruptedError("Stopped");
                if (Thread.interrupted())
                    throw new DownloadInterruptedError("Interrupted");
            }

        } catch (InterruptedIOException e) {
            throw new DownloadInterruptedError("Interrupted", e);
        } finally {
            if (randomAccessUri != null)
                randomAccessUri.close();
            if (bufferedInputStream != null)
                bufferedInputStream.close();
        }
    }

    @Override
    public void download(final AtomicBoolean stop, final Runnable notify) {

        try {
            RetryWrap.run(stop, new RetryWrap.Wrap() {

                @Override
                public Context getContext() {
                    return DirectRange.this.getContext();
                }

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
     * check existing file for download resume. for range download it will check
     * file size and inside state. they sould be equal.
     *
     * @param info       DownloadInfo
     * @param targetFile location of File to resume
     * @return return true - if all ok, false - if download can not be restored.
     */
    public static boolean canResume(DownloadInfo info, File targetFile) {
        if (targetFile.exists()) {
            if (info.getCount() != targetFile.length())
                return false;
        } else {
            if (info.getCount() > 0)
                return false;
        }
        return true;
    }
}
