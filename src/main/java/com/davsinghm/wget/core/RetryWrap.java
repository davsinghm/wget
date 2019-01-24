package com.davsinghm.wget.core;

import android.content.Context;

import com.davsinghm.wget.Logger;
import com.davsinghm.wget.NetworkUtils;
import com.davsinghm.wget.core.info.ex.DownloadException;
import com.davsinghm.wget.core.info.ex.DownloadIOException;
import com.davsinghm.wget.core.info.ex.DownloadInterruptedException;
import com.davsinghm.wget.core.info.ex.DownloadMoved;
import com.davsinghm.wget.core.info.ex.DownloadRetry;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.HttpRetryException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLException;

public class RetryWrap {

    private static final int RETRY_DELAY = 10;

    public interface Wrap<T> {

        Context getContext();

        void retry(int delay, Throwable e);

        void moved(URL url);

        T download() throws IOException;
    }

    private static <T> void moved(AtomicBoolean stop, Wrap<T> wrap, DownloadMoved e) {
        if (stop.get())
            throw new DownloadInterruptedException("Stopped");

        if (Thread.currentThread().isInterrupted())
            throw new DownloadInterruptedException("Interrupted");

        wrap.moved(e.getMoved());
    }

    private static <T> void retry(AtomicBoolean stop, Wrap<T> wrap, RuntimeException e) {
        for (int i = RETRY_DELAY; i >= 0; i--) {
            wrap.retry(i, e);

            if (stop.get())
                throw new DownloadInterruptedException("Stopped");

            if (Thread.currentThread().isInterrupted())
                throw new DownloadInterruptedException("Interrupted");

            if (!NetworkUtils.isNetworkAvailable(wrap.getContext()))
                i = RETRY_DELAY;

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                throw new DownloadInterruptedException(e1); //TODO remove e1, "Interrupted" may be good
            }
        }
    }

    public static <T> T run(AtomicBoolean stop, Wrap<T> wrap) {
        while (true) {
            if (stop.get())
                throw new DownloadInterruptedException("Stopped");
            if (Thread.currentThread().isInterrupted())
                throw new DownloadInterruptedException("Interrupted");

            try {
                try {
                    return wrap.download();
                } catch (SocketException | UnknownHostException | InterruptedIOException | HttpRetryException | ProtocolException | EOFException | SSLException e) {
                    Logger.wtf("RetryWrap: run()", new DownloadRetry("Retry: SUIHPES, throw DownloadRetry(e)", e));
                    throw new DownloadRetry(e);
                } catch (FileNotFoundException e) {
                    throw new DownloadException(e);
                } catch (RuntimeException e) {

                    if (!NetworkUtils.isNetworkAvailable(wrap.getContext())) {
                        Logger.wtf("RetryWrap: run()", new DownloadRetry("Retry: throw DownloadRetry(e)", e));
                        throw new DownloadRetry(e);
                    }

                    throw e;
                } catch (IOException e) {
                    String message = e.getMessage();
                    if (message != null && message.startsWith("unexpected end of stream")) { //NON-NLS
                        Logger.wtf("RetryWrap: run()", new DownloadRetry("Retry: Unexpected EOF, throw DownloadRetry(e)", e));
                        throw new DownloadRetry(e);
                    }
                    throw new DownloadIOException(e);
                }
            } catch (DownloadMoved e) {
                moved(stop, wrap, e);
            } catch (DownloadRetry e) {
                retry(stop, wrap, e);
            }
        }
    }

}
