package com.dsingh.wget.core;

import com.dsingh.wget.DManager;
import com.dsingh.wget.Logs;
import com.dsingh.wget.NetworkUtils;
import com.dsingh.wget.core.info.ex.DownloadError;
import com.dsingh.wget.core.info.ex.DownloadIOCodeError;
import com.dsingh.wget.core.info.ex.DownloadIOError;
import com.dsingh.wget.core.info.ex.DownloadInterruptedError;
import com.dsingh.wget.core.info.ex.DownloadMoved;
import com.dsingh.wget.core.info.ex.DownloadRetry;


import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.HttpRetryException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLException;

public class RetryWrap {

    public static final int RETRY_DELAY = 10;

    public interface Wrap<T> {
        void retry(int delay, Throwable e);

        void moved(URL url);

        T download() throws IOException;
    }

    static <T> void moved(AtomicBoolean stop, Wrap<T> wrap, DownloadMoved e) {
        if (stop.get())
            throw new DownloadInterruptedError("Stopped");

        if (Thread.currentThread().isInterrupted())
            throw new DownloadInterruptedError("Interrupted");

        wrap.moved(e.getMoved());
    }

    static <T> void retry(AtomicBoolean stop, Wrap<T> wrap, RuntimeException e) {
        for (int i = RETRY_DELAY; i >= 0; i--) {
            wrap.retry(i, e);

            if (stop.get())
                throw new DownloadInterruptedError("Stopped");

            if (Thread.currentThread().isInterrupted())
                throw new DownloadInterruptedError("Interrupted");

            Logs.w("RetryWrap: run()", "DManager.getInstance().getAppContext()1", e);
            if (!NetworkUtils.isNetworkAvailable(DManager.getInstance().getAppContext()))
                i = RETRY_DELAY;

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                throw new DownloadInterruptedError(e1); //TODO remove e1, "Interrupted" may be good
            }
        }
    }

    public static <T> T run(AtomicBoolean stop, Wrap<T> wrap) {
        while (true) {
            if (stop.get())
                throw new DownloadInterruptedError("Stopped");
            if (Thread.currentThread().isInterrupted())
                throw new DownloadInterruptedError("Interrupted");

            try {
                try {
                    return wrap.download();
                } catch (SocketException | UnknownHostException | InterruptedIOException | HttpRetryException | ProtocolException | EOFException | SSLException e) {
                    Logs.w("RetryWrap: run()", "SUIHPES, throw DownloadRetry(e)", e);
                    throw new DownloadRetry(e);
                } catch (FileNotFoundException e) {
                    throw new DownloadError(e);
                } catch (RuntimeException e) {
                    Logs.w("RetryWrap: run()", "DManager.getInstance().getAppContext()", e);

                    if (!NetworkUtils.isNetworkAvailable(DManager.getInstance().getAppContext())) {
                        Logs.w("RetryWrap: run()", "throw DownloadRetry(e)", e);
                        throw new DownloadRetry(e);
                    }
                    throw e;
                } catch (IOException e) {
                    if (e.getMessage().startsWith("unexpected end of stream")) { //NON-NLS
                        Logs.w("RetryWrap: run()", "Unexpected EOF, throw DownloadRetry(e)", e);
                        throw new DownloadRetry(e);
                    }
                    throw new DownloadIOError(e);
                }
            } catch (DownloadMoved e) {
                moved(stop, wrap, e);
            } catch (DownloadRetry e) {
                retry(stop, wrap, e);
            }
        }
    }

    public static void checkResponse(HttpURLConnection c) throws IOException {
        int code = c.getResponseCode();
        switch (code) {
            case HttpURLConnection.HTTP_OK:
            case HttpURLConnection.HTTP_PARTIAL:
                return;
            case HttpURLConnection.HTTP_MOVED_TEMP:
            case HttpURLConnection.HTTP_MOVED_PERM:
                // the user agent MUST NOT automatically redirect the request unless it can be confirmed by the user
                throw new DownloadMoved(c);
            case HttpURLConnection.HTTP_FORBIDDEN:
                throw new DownloadIOCodeError(HttpURLConnection.HTTP_FORBIDDEN);
            case 416:
                // HTTP Error 416 - Requested Range Not Satisfiable
                throw new DownloadIOCodeError(416);
        }
    }
}
