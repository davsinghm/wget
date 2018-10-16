package com.dsingh.wget.core.info;

import android.content.Context;

import com.dsingh.wget.Constants;
import com.dsingh.wget.Logs;
import com.dsingh.wget.core.RetryWrap;
import com.dsingh.wget.core.info.ex.DownloadInterruptedError;
import com.dsingh.wget.core.info.ex.DownloadMoved;
import com.dsingh.wget.core.info.ex.DownloadRetry;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * URLInfo - keep all information about source in one place. Thread safe.
 */
public class URLInfo extends BrowserInfo {

    private URL source; // source url
    private boolean isEmpty = true; // have been extracted?
    private Long length; // null if size is unknown, which means we unable to restore downloads or do multi thread downlaods
    private boolean hasRange; // does server support for the range param?
    private String contentType; // null if here is no such file or other error
    private String contentFilename; // come from Content-Disposition: attachment; filename="fname.ext"
    private State state; // download state
    private Throwable exception; // downloading error / retry error
    private int delay; // retrying delay

    URLInfo(URL source) {
        this.source = source;
        this.state = State.QUEUED;
    }

    void extract(final Context context, AtomicBoolean stop, final Runnable notify) {
        Logs.d("WGet: URLInfo", "extract(): invoked");
        try {
            HttpURLConnection urlConnection = RetryWrap.run(stop, new RetryWrap.Wrap<HttpURLConnection>() {
                URL url = source;

                @Override
                public Context getContext() {
                    return context;
                }

                @Override
                public HttpURLConnection download() throws IOException {

                    setState(State.EXTRACTING);
                    notify.run();

                    try {
                        return extractRange(url);
                    } catch (DownloadRetry | DownloadMoved e) {
                        Logs.e("WGet: URLInfo", "extract(): " + e.toString());
                        throw e;
                    } catch (RuntimeException e) {
                        Logs.e("WGet: URLInfo", "extract(): " + e.toString());
                        return extractNormal(url);
                    }
                }

                @Override
                public void retry(int d, Throwable ee) {
                    setDelay(d, ee);
                    notify.run();
                }

                @Override
                public void moved(URL u) {
                    setReferrer(url);
                    url = u;
                    setState(State.RETRYING);
                    notify.run();
                }
            });

            setContentType(urlConnection.getContentType());

            String contentDisposition = urlConnection.getHeaderField("Content-Disposition");
            if (contentDisposition != null) {
                // i support for two forms with and without quotes:
                //
                // 1) contentDisposition="attachment;filename="ap61.ram"";
                // 2) contentDisposition="attachment;filename=ap61.ram";

                Pattern cp = Pattern.compile("filename=[\"]*([^\"]*)[\"]*");
                Matcher cm = cp.matcher(contentDisposition);
                if (cm.find())
                    setContentFilename(cm.group(1));
            }

            setEmpty(false);

            setState(State.EXTRACTING_DONE);
            notify.run();

        } catch (DownloadInterruptedError e) {
            Logs.e("WGet: URLInfo", "extract(): " + e.toString());
            setState(State.STOP, e);
            notify.run();

            throw e;

        } catch (RuntimeException e) {
            Logs.e("WGet: URLInfo", "extract(): " + e.toString());
            setState(State.ERROR, e);
            notify.run();

            throw e;
        }
    }

    synchronized public boolean isEmpty() {
        return isEmpty;
    }

    synchronized public void setEmpty(boolean isEmpty) {
        Logs.d("WGet: URLInfo", "setEmpty(): " + isEmpty);
        this.isEmpty = isEmpty;
    }

    // if range failed - do plain download with no retrys's
    private HttpURLConnection extractRange(URL source) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) source.openConnection();

        urlConnection.setConnectTimeout(Constants.WGET_CONNECT_TIMEOUT);
        urlConnection.setReadTimeout(Constants.WGET_READ_TIMEOUT);

        urlConnection.setRequestProperty("User-Agent", getUserAgent());
        if (getReferrer() != null)
            urlConnection.setRequestProperty("Referer", getReferrer().toExternalForm());

        // may raise an exception if not supported by server
        urlConnection.setRequestProperty("Range", "bytes=" + 0 + "-" + 0);

        RetryWrap.checkResponse(urlConnection);

        String range = urlConnection.getHeaderField("Content-Range");
        if (range == null)
            throw new RuntimeException("Range not supported");

        Pattern p = Pattern.compile("bytes \\d+-\\d+/(\\d+)");
        Matcher m = p.matcher(range);
        if (m.find())
            setLength(Long.valueOf(m.group(1)));
        else
            throw new RuntimeException("Range not supported");

        setHasRange(true);

        return urlConnection;
    }

    // if range failed - do plain download with no retrys's
    private HttpURLConnection extractNormal(URL source) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) source.openConnection();

        urlConnection.setConnectTimeout(Constants.WGET_CONNECT_TIMEOUT);
        urlConnection.setReadTimeout(Constants.WGET_READ_TIMEOUT);

        urlConnection.setRequestProperty("User-Agent", getUserAgent());
        if (getReferrer() != null)
            urlConnection.setRequestProperty("Referer", getReferrer().toExternalForm());

        setHasRange(false);

        RetryWrap.checkResponse(urlConnection);

        long len = urlConnection.getContentLength();
        if (len >= 0)
            setLength(len);

        return urlConnection;
    }

    synchronized public String getContentType() {
        return contentType;
    }

    synchronized public void setContentType(String ct) {
        contentType = ct;
    }

    synchronized public Long getLength() {
        return length;
    }

    synchronized public void setLength(Long l) {
        length = l;
    }

    synchronized public URL getSource() {
        return source;
    }

    synchronized public String getContentFilename() {
        return contentFilename;
    }

    synchronized public void setContentFilename(String f) {
        contentFilename = f;
    }

    synchronized public State getState() {
        return state;
    }

    synchronized public void setState(State state) {
        this.state = state;
        this.exception = null;
        this.delay = 0;
    }

    synchronized public void setState(State state, Throwable e) {
        this.state = state;
        this.exception = e;
        this.delay = 0;
    }

    synchronized public Throwable getException() {
        return exception;
    }

    synchronized protected void setException(Throwable exception) {
        this.exception = exception;
    }

    synchronized public int getDelay() {
        return delay;
    }

    synchronized public void setDelay(int delay, Throwable e) {
        this.delay = delay;
        this.exception = e;
        this.state = State.RETRYING;
    }

    synchronized public boolean hasRange() {
        return hasRange;
    }

    private synchronized void setHasRange(boolean hasRange) {
        Logs.d("WGet: URLInfo", "setHasRange(): " + hasRange);
        this.hasRange = hasRange;
    }

}
