package com.davsinghm.wget.core;

import android.content.Context;
import android.net.Uri;
import android.os.Process;

import com.davsinghm.wget.Constants;
import com.davsinghm.wget.Logger;
import com.davsinghm.wget.core.info.DownloadInfo;
import com.davsinghm.wget.core.info.Part;
import com.davsinghm.wget.core.info.State;
import com.davsinghm.wget.core.info.ex.DownloadInterruptedError;
import com.davsinghm.wget.core.info.ex.DownloadMultipartError;
import com.davsinghm.wget.core.info.ex.DownloadRetry;
import com.davsinghm.wget.core.threads.LimitThreadPool;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public class DirectMultipart extends Direct {

    private LimitThreadPool limitThreadPool;
    private boolean isFatal;
    private final Object lock = new Object();

    public DirectMultipart(Context context, DownloadInfo info, Uri target) {
        super(context, info, target);
        this.limitThreadPool = new LimitThreadPool(info.getDSettings().getThreadCount());
    }

    /**
     * download part.
     * <p/>
     * if returns normally - part is fully downloaded. other wise - it throws
     * RuntimeException or DownloadRetry or DownloadError
     *
     * @param part Part to download
     */
    private void downloadPart(Part part, AtomicBoolean stop, Runnable notify) throws IOException {

        RandomAccessUri randomAccessUri = null;
        BufferedInputStream bufferedInputStream = null;

        try {
            URL url = getInfo().getSource();

            long start = part.getStart() + part.getCount();
            long end = part.getEnd();

            String partID = "Part " + (part.getNumber() + 1) + "/" + getInfo().getPartList().size() + ": "; //NON-NLS
            Logger.d("WGet: " + getInfo().getDInfoID() + ": IDM", partID + "start: " + part.getStart() + ", end: " + part.getEnd() + ", " + part.getCount() + " of " + part.getLength() + " already downloaded");

            // fully downloaded already?
            if (end - start + 1 == 0) {
                Logger.d("WGet: " + getInfo().getDInfoID() + ": IDM", partID + "Fully downloaded already");
                return;
            }

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setConnectTimeout(Constants.WGET_CONNECT_TIMEOUT);
            urlConnection.setReadTimeout(Constants.WGET_READ_TIMEOUT);

            urlConnection.setRequestProperty("User-Agent", getInfo().getUserAgent()); //NON-NLS
            if (getInfo().getReferer() != null)
                urlConnection.setRequestProperty("Referer", getInfo().getReferer().toExternalForm());

            synchronized (getTarget()) {
                randomAccessUri = new RandomAccessUri(getContext(), getTarget(), "w");
                randomAccessUri.seek(start);
            }

            urlConnection.setRequestProperty("Range", "bytes=" + start + "-" + end); //NON-NLS

            HttpUtil.checkResponse(urlConnection);

            bufferedInputStream = new BufferedInputStream(urlConnection.getInputStream());

            byte[] bytes = new byte[BUF_SIZE];
            int read;
            boolean localStop = false;
            while ((read = bufferedInputStream.read(bytes)) > 0) {
                // ensure we do not download more then part size.
                // if so cut bytes and stop download

                long partEnd = part.getLength() - part.getCount();
                if (read > partEnd) {
                    Logger.d("WGet: " + getInfo().getDInfoID() + ": IDM", partID + "LocalStop = true | read: " + read + ", partEnd: " + partEnd);
                    read = (int) partEnd;
                    localStop = true;
                }

                randomAccessUri.write(bytes, 0, read);
                part.setCount(part.getCount() + read);
                getInfo().updateMultipartCount();
                notify.run();

                if (stop.get())
                    throw new DownloadInterruptedError("Stopped");
                if (Thread.interrupted())
                    throw new DownloadInterruptedError("Interrupted");
                if (fatal())
                    throw new DownloadInterruptedError("Fatal");

                // do not throw exception here. we normally done downloading.
                // just took a little bit more
                if (localStop)
                    break;
            }

            Logger.d("WGet: " + getInfo().getDInfoID() + ": IDM", partID + "COMPLETE | count: " + part.getCount() + ", length: " + part.getLength() + " from " + part.getStart() + "-" + part.getEnd());

            if (part.getCount() != part.getLength())
                throw new DownloadRetry("EOF before end of part");

        } finally {
            if (bufferedInputStream != null)
                bufferedInputStream.close();
            if (randomAccessUri != null)
                randomAccessUri.close();
        }

    }

    private boolean fatal() {
        synchronized (lock) {
            return isFatal;
        }
    }

    private void fatal(boolean bool) {
        synchronized (lock) {
            isFatal = bool;
        }
    }

    private void downloadWorker(final Part part, final AtomicBoolean stop, final Runnable notify) throws InterruptedException {
        limitThreadPool.blockExecute(new Runnable() {
            @Override
            public void run() {

                {
                    Thread thread = Thread.currentThread();
                    thread.setName(getInfo().getDInfoID() + " - Part: " + (part.getNumber() + 1)); //NON-NLS
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                }

                try {
                    RetryWrap.run(stop, new RetryWrap.Wrap() {

                        @Override
                        public Context getContext() {
                            return DirectMultipart.this.getContext();
                        }

                        @Override
                        public Object download() throws IOException {
                            part.setState(State.DOWNLOADING);
                            notify.run();

                            downloadPart(part, stop, notify);
                            return null;
                        }

                        @Override
                        public void retry(int delay, Throwable e) {
                            part.setDelay(delay, e);
                            notify.run();
                        }

                        @Override
                        public void moved(URL url) {
                            part.setState(State.RETRYING);
                            notify.run();
                        }

                    });

                    part.setState(State.DONE);
                    notify.run();

                } catch (DownloadInterruptedError e) {
                    part.setState(State.STOP, e);
                    notify.run();

                    fatal(true);
                } catch (RuntimeException e) {
                    part.setState(State.ERROR, e);
                    notify.run();

                    fatal(true);
                }
            }
        });

        part.setState(State.WAITING);
    }

    private void collaborate() {

        long minPartLength = getInfo().getDSettings().getMinPartLength();
        int threadCount = getInfo().getDSettings().getThreadCount();
        int c = runningPartCount();
        Logger.d("WGet: " + getInfo().getDInfoID() + ": IDM", "collaborate(): runningPartCount: " + c + ", threadCount: " + threadCount + ", entryingLoop: " + (c < threadCount && c > 0));
        if (c < threadCount && c > 0)
            for (Part part : getInfo().getSortedPartList()) {
                int listSize = getInfo().getPartList().size();
                State state = part.getState();
                if (state.equals(State.DOWNLOADING) || state.equals(State.RETRYING)) {

                    Logger.d("WGet: " + getInfo().getDInfoID() + ": IDM", "collaborate(): -- Trying to help Part: " + (part.getNumber() + 1) + " (Currently: " + state + ")");

                    Part part1 = new Part();
                    part1.setState(State.QUEUED);
                    part1.setNumber(listSize);

                    long left = (part.getLength() - part.getCount()) / 2;
                    long start = part.getStart() + part.getCount() + left;
                    long end = part.getEnd();
                    long minSecs = getInfo().getSpeedInfo().getAverageSpeed() * Constants.MT_MIN_SEC_SPEED_MULTIPLE_FOR_IDM;
                    if (left < minSecs || left <= 0 || left < minPartLength) {
                        Logger.d("WGet: " + getInfo().getDInfoID() + ": IDM", "collaborate(): ---- Skipping help Part: " + (part.getNumber() + 1) + " left: " + left + ", min: " + minSecs + " [left: " + (left / 1024) + "kb, min: " + (minSecs / 1024) + "kb, minPart: " + (minPartLength / 1024) + "kb]");
                        continue;
                    }
                    part.setEnd(start - 1);
                    part1.setStart(start);
                    part1.setEnd(end);

                    getInfo().getPartList().add(part1);

                    Logger.d("WGet: " + getInfo().getDInfoID() + ": IDM", "collaborate(): ---- Helping Part: " + (part.getNumber() + 1) + " left: " + left + ", min: " + minSecs + " [left: " + (left / 1024) + "kb, min: " + (minSecs / 1024) + "kb, minPart: " + (minPartLength / 1024) + "kb]");
                    Logger.d("WGet: " + getInfo().getDInfoID() + ": IDM", "collaborate(): ---- NEW PART - start: " + part1.getStart() + " [" + (part1.getStart() / 1024) + "kb], end: " + part1.getEnd() + " [" + (part1.getEnd() / 1024) + "kb], length: " + part1.getLength() + " [" + (part1.getLength() / 1024) + "kb]");

                    break;
                }
            }

    }

    private int runningPartCount() {
        int running = 0;
        for (Part part : getInfo().getPartList()) {
            if (part.getState().equals(State.WAITING) || part.getState().equals(State.QUEUED) || part.getState().equals(State.DOWNLOADING) || part.getState().equals(State.RETRYING))
                running++;
        }
        return running;
    }

    /**
     * return next part to download. ensure this part is not done() and not
     * currently downloading
     *
     * @return next Part to download
     */
    private Part getPart() {
        for (Part part : getInfo().getPartList()) {
            if (!part.getState().equals(State.QUEUED))
                continue;
            return part;
        }

        return null;
    }

    /**
     * return true, when thread pool empty, and here is no unfinished parts to
     * download
     *
     * @return true - done. false - not done yet
     */
    private boolean done(AtomicBoolean stop) {
        if (stop.get())
            throw new DownloadInterruptedError("Stopped");
        if (Thread.interrupted())
            throw new DownloadInterruptedError("Interupted");
        return !limitThreadPool.active() && getPart() == null;
    }

    @Override
    public void download(AtomicBoolean stop, Runnable notify) {
        for (Part part : getInfo().getPartList()) {
            if (part.getState().equals(State.DONE))
                continue;
            part.setState(State.QUEUED);
        }

        getInfo().setState(State.DOWNLOADING);
        notify.run();

        Logger.d("WGet: " + getInfo().getDInfoID() + ": download()", "DownloadInfo length: " + getInfo().getLength());

        try {
            while (!done(stop)) {
                Part part = getPart();
                if (part != null) {
                    Logger.d("WGet: " + getInfo().getDInfoID() + ": download()", "Adding new part: " + (part.getNumber() + 1));
                    downloadWorker(part, stop, notify);
                } else // we have no parts left. wait until task ends and check again if we have to retry. we have to check if last part back to queue in case of RETRY state
                    limitThreadPool.waitUntilNextTaskEnds();

                if (getInfo().getDSettings().getMtStyle() == Constants.MT_STYLE_SMART)
                    collaborate();

                // if we start to receive errors. stop adding new tasks and wait until all active tasks be emptied
                if (fatal()) {
                    Logger.e("WGet: " + getInfo().getDInfoID() + ": DirectMP", "Fatal is true");
                    limitThreadPool.waitUntilTermination();

                    // check if all parts finished with interrupted, throw one interruption
                    {
                        boolean interrupted = true;
                        for (Part pp : getInfo().getPartList()) {
                            Throwable e = pp.getException();
                            if (e == null)
                                continue;
                            if (e instanceof DownloadInterruptedError)
                                continue;
                            interrupted = false;
                        }
                        if (interrupted) {
                            Logger.e("WGet: " + getInfo().getDInfoID() + ": DirectMP", "Fatal! Any of Part is Interrupted, throw DownloadInterruptedError()");
                            throw new DownloadInterruptedError("Multipart: All interrupted");
                        }
                    }

                    // ok all thread stopped. now throw the exception and let app deal with the errors
                    Logger.e("WGet: " + getInfo().getDInfoID() + ": DirectMP", "Fatal! None of Part is Interrupted though, throw DownloadMultipartError()");

                    throw new DownloadMultipartError(getInfo());
                }

            }

            getInfo().setState(State.DONE);
            notify.run();
        } catch (InterruptedException e) {
            getInfo().setState(State.STOP);
            notify.run();

            throw new DownloadInterruptedError(e);
        } catch (DownloadInterruptedError e) {
            getInfo().setState(State.STOP);
            notify.run();

            throw e;
        } catch (RuntimeException e) {
            getInfo().setState(State.ERROR);
            notify.run();

            throw e;
        } finally {
            limitThreadPool.shutdown();
        }
    }

}
