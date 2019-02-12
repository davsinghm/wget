package com.davsinghm.wget;

import android.content.Context;
import android.net.Uri;

import com.davsinghm.wget.core.DirectMultipart;
import com.davsinghm.wget.core.DirectRange;
import com.davsinghm.wget.core.DirectSingle;
import com.davsinghm.wget.core.DirectSingleBg;
import com.davsinghm.wget.core.info.DownloadInfo;
import com.davsinghm.wget.core.info.Part;
import com.davsinghm.wget.core.info.State;
import com.davsinghm.wget.core.info.ex.DownloadException;
import com.davsinghm.wget.core.info.ex.DownloadInterruptedException;
import com.davsinghm.wget.core.info.ex.DownloadMultipartException;
import com.davsinghm.wget.core.info.ex.MuxException;

import java.io.InterruptedIOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DRunnable implements Runnable {

    private Context context;
    private DBundle dBundle;
    private DManager dManager;

    private AtomicBoolean stop = new AtomicBoolean(false);
    private AtomicBoolean finished = new AtomicBoolean(false);
    private DProgress dProgress;
    private DownloadInfo dInfo;

    private BlockingQueue<Job> jobQueue;

    @Nullable
    private Job currentJob;
    @NonNull
    private DState lastDState = DState.QUEUED; // used to display in activity;

    private long lastTimestamp;

    @Nullable
    private Thread currentThread;

    DRunnable(DManager dManager, DBundle dBundle) {
        this.dManager = dManager;
        this.dBundle = dBundle;
        this.context = dManager.getAppContext();
    }

    Thread getCurrentThread() {
        return currentThread;
    }

    private void setCurrentThread(Thread currentThread) {
        this.currentThread = currentThread;
    }

    DBundle getDBundle() {
        return dBundle;
    }

    String getDownloadUid() {
        return dBundle.getDownloadId();
    }

    private void progressUpdate(int what, DProgress progress) {
        dBundle.setProgress(progress);
        dManager.progressUpdate(what, progress);
    }

    void stopDownload() {
        stop.set(true);
        interruptMuxer();
        if (currentThread != null)
            currentThread.interrupt();
    }

    @NonNull
    DState getState() {
        return lastDState;
    }

    @Override
    public void run() {

        if (stop.get())
            return;

        setCurrentThread(Thread.currentThread());
        //Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        try {

            dProgress = new DProgress(dBundle);

            jobQueue = dBundle.getJobQueue();

            doNextJob();

        } catch (Exception e) {
            stop.set(true);
        }
    }

    private void doNextJob() throws Exception {

        while ((currentJob = jobQueue.poll()) != null) {

            Logger.d("DRunnable", "doNextJob(): " + currentJob);

            switch (currentJob) {
                case PARSE:
                    updateYtUrls();
                    break;
                case VIDEO:
                    download(dBundle.getVideoUrl(), dBundle.getDirectory(), dBundle.getVideoFilename());
                    break;
                case AUDIO:
                    download(dBundle.getAudioUrl(), dBundle.getDirectory(), dBundle.getAudioFilename());
                    break;
                case SUBTITLE:
                    downloadSubtitles();
                    break;
                case ENCODE:
                    encodeAudio();
                    break;
                case MUX:
                    muxAudioVideo();
                    break;
            }
        }

        Logger.d("DRunnable", "doNextJob(): out of loop");

        updateProgress(DState.COMPLETE);
    }

    private String getTableName() {
        if (currentJob == null)
            return dBundle.isAudioOnly() ? DInfoHelper.TABLE_AUDIO : DInfoHelper.TABLE_VIDEO;

        switch (currentJob) {
            case PARSE:
                return dBundle.isAudioOnly() ? DInfoHelper.TABLE_AUDIO : DInfoHelper.TABLE_VIDEO;
            case VIDEO:
            case SUBTITLE:
            case MUX:
                return DInfoHelper.TABLE_VIDEO;
            case ENCODE:
                return dBundle.isAudioOnly() ? DInfoHelper.TABLE_AUDIO : DInfoHelper.TABLE_VIDEO;
            case AUDIO:
                return DInfoHelper.TABLE_AUDIO;
        }
        return null;
    }

    @NonNull
    private String getDInfoID() {
        return dBundle.getDownloadId() + " " + "[" + (currentJob != null ? currentJob.toString().substring(0, 1) : "N") + (dBundle.isTwoPartDownload() ? "|2" : "") + "]"; //NON-NLS
    }

    private void downloadSubtitles() {

        try {
            updateProgress(DState.EXTRACTING);

            new DirectSingleBg(context, dBundle.getSubtitleUrl(), dBundle.getSubtitleUri()).downloadPart(stop);

        } catch (Exception e) { //TODO make fatal
            Logger.wtf("DRunnable: Non-Fatal, Critical: downloadSubtitles(): Failed to load subtitles.", e);
        }
    }

    private void download(String url, Uri directory, String filename) throws Exception {

       /* while (!stop.get()) {
            updateProgress(DState.RETRYING);
        }

//        Thread.currentThread().sleep(2000);
        updateProgress(DState.STOPPED);
        if (true) return;
          //  throw new DownloadInterruptedError();
*/
        Runnable notify = new Runnable() {
            @Override
            public void run() {
                switch (dInfo.getState()) {
                    case QUEUED: //TODO take care
                    case EXTRACTING:
                    case EXTRACTING_DONE:
                        updateProgress(DState.EXTRACTING);
                        break;
                    case DOWNLOADING:
                        updateProgress(DState.DOWNLOADING);
                        break;
                    case RETRYING:
                        updateProgress(DState.RETRYING);
                        break;
                    case STOP:
                        updateProgress(DState.STOPPED);
                        break;
                    case ERROR:
                        updateProgress(DState.ERROR);
                        break;
                    case DONE:
                        DInfoHelper.getInstance(context).addInfo(getTableName(), dBundle.getDownloadId(), dInfo.toString(), "DONE");
                        break;
                }
            }
        };

        try {
            DSettings dSettings = dBundle.getDSettings();

            dInfo = new DownloadInfo(new URL(url));
            dInfo.setDInfoID(getDInfoID());
            dInfo.setDSettings(dSettings);
            dInfo.extract(context, stop, notify);
            dInfo.fromString(notify, DInfoHelper.getInstance(context).getInfoString(getTableName(), dBundle.getDownloadId()));

            if (dInfo.isMultipart()) {
                Logger.d("WGet", "createDirect(): MultiPart");
                new DirectMultipart(context, dInfo, directory, filename).download(stop, notify);
            } else if (dInfo.hasRange()) {
                Logger.d("WGet", "createDirect(): Range");
                new DirectRange(context, dInfo, directory, filename).download(stop, notify);
            } else {
                Logger.d("WGet", "createDirect(): Single");
                new DirectSingle(context, dInfo, directory, filename).download(stop, notify);
            }

        } catch (DownloadMultipartException e) {
            Logger.wtf("DRunnable: " + getDInfoID(), e);

            throw e;
        } catch (DownloadInterruptedException e) {
            updateProgress(DState.STOPPED);
            //Logger.w("DRunnable: " + getDInfoID(), e);

            throw e;
        } catch (Exception e) {
            updateProgress(DState.ERROR);
            stop.set(true);

            Logger.wtf("DRunnable: " + getDInfoID(), new Exception("BundleId: " + dBundle.getDownloadId(), e));

            throw e;
        }

        if (dInfo.getState() != State.DONE) {
            updateProgress(DState.ERROR);
            throw new DownloadException("Test: thrown if State != DONE to exit job loop");
        }
    }

    private synchronized void updateProgress(@NonNull DState dState) {

        boolean bool = System.currentTimeMillis() - lastTimestamp > Constants.DRUNNABLE_PROGRESS_UPDATE_INTERVAL || lastDState != dState;
        if (!finished.get() && bool) {
            int onGoing = 0;
            dProgress.setDState(dState);
            dProgress.setShowAudio(dBundle.isTwoPartDownload() && currentJob == Job.AUDIO);

            switch (dState) {
                case QUEUED:
                case EXTRACTING:
                case PARSING:
                case RETRYING:
                    onGoing = 1;
                    DInfoHelper.getInstance(context).addInfoState(getTableName(), dBundle.getDownloadId(), "ONGOING");
                    break;
                case MUXING:
                case ENCODING:
                    onGoing = 1;
                    DInfoHelper.getInstance(context).addInfoState(getTableName(), dBundle.getDownloadId(), "ONGOING");
                    break;
                case DOWNLOADING:
                    onGoing = 1;
                    dProgress.setCount(dInfo.getCount());
                    dProgress.setLength(dInfo.getLength() != null ? dInfo.getLength() : 0);
                    dProgress.setAvgSpeed(dInfo.getSpeedInfo().getAverageSpeed());
                    dProgress.setCurrentSpeed(dInfo.getSpeedInfo().getCurrentSpeed());
                    dProgress.setThreadCount(activeThreadCount());
                    DInfoHelper.getInstance(context).addInfo(getTableName(), dBundle.getDownloadId(), dInfo.toString(), "ONGOING");
                    break;
                case ERROR:
                case STOPPED:
                case MUX_ERROR:
                case ENCODE_ERROR:
                    //FIXME remove this hack. on error the updated sizes are not sent because of update delay (interval condition)
                    if (dBundle.isTwoPartDownload()) {
                        //read from helper, and update dprogress
                        String videoStr = DInfoHelper.getInstance(context).getInfoString(DInfoHelper.TABLE_VIDEO, dBundle.getDownloadId());
                        String audioStr = DInfoHelper.getInstance(context).getInfoString(DInfoHelper.TABLE_AUDIO, dBundle.getDownloadId());
                        long length = 0;
                        long count = 0;
                        length += DInfoHelper.getLengthFromInfoString(videoStr);
                        length += DInfoHelper.getLengthFromInfoString(audioStr);
                        count += DInfoHelper.getCountFromInfoString(videoStr);
                        count += DInfoHelper.getCountFromInfoString(audioStr);
                        dProgress.setLength(length);
                        dProgress.setCount(count);
                    }
                case COMPLETE:
                    if (dState == DState.COMPLETE)
                        dBundle.onDownloadComplete();

                    DInfoHelper.getInstance(context).addInfoState(getTableName(), dBundle.getDownloadId(), dState.toString());
                    finished.set(true);
                    break;
            }

            if (finished.get()) {

                if (lastDState != DState.COMPLETE && lastDState != DState.ERROR && lastDState != DState.STOPPED && lastDState != DState.MUX_ERROR && lastDState != DState.ENCODE_ERROR) {
                    setCurrentThread(null);
                    Thread.interrupted();
                    progressUpdate(onGoing == 1 ? DService.MESSAGE_PROGRESS_ONGOING : DService.MESSAGE_PROGRESS_ENDED, dProgress);

                    dManager.removeFromQueue(this);
                }

            } else
                progressUpdate(onGoing, dProgress);

            lastDState = dState;
            lastTimestamp = System.currentTimeMillis();
        }
    }

    private String activeThreadCount() {
        if (dInfo.isMultipart()) {
            List<Part> arrayList = dInfo.getPartList();
            int running = 0;
            for (Part part : arrayList) {
                if (part.getState().equals(State.DOWNLOADING) || part.getState().equals(State.RETRYING))
                    running++;
                Logger.w("DRunnable: " + getDInfoID() + ": Part " + (part.getNumber() + 1) + "/" + arrayList.size() + ", PartState", part.getState().toString() + ", Count: " + (part.getCount() / 1024) + "kb, Length: " + (part.getLength() / 1024) + "kb");
            }
            if (running == 0)
                return null;
            return "[" + running + "/" + dInfo.getDSettings().getThreadCount() + "]";
        }
        return null;
    }

    private void interruptMuxer() {
        if (currentJob == Job.MUX || currentJob == Job.ENCODE)
            dBundle.cancelMux();
    }

    private void muxAudioVideo() throws MuxException {

        updateProgress(DState.MUXING);

        try {

            MuxStatCallback statisticsCallback = new MuxStatCallback() {
                @Override
                public void onStatisticsUpdated(int time, int totalTime) {
                    dProgress.setTime(time);
                    dProgress.setTotalTime(totalTime);
                    updateProgress(DState.MUXING);
                }

                @Override
                public void onMuxFinished(long fileSize) {
                    if (fileSize > 0) { //FIXME this overrides the size of stream, use different tables to save final data
                        String audioStr = DInfoHelper.getInstance(context).getInfoString(getTableName(), dBundle.getDownloadId());
                        DInfoHelper.getInstance(context).updateCountAndLengthWithInfoString(getTableName(), dBundle.getDownloadId(), audioStr, fileSize, fileSize);
                        dProgress.setCount(fileSize);
                        dProgress.setLength(fileSize);
                    }
                }
            };

            dBundle.muxAllFiles(statisticsCallback);

        } catch (InterruptedException | InterruptedIOException e) {
            updateProgress(DState.STOPPED);
            throw new MuxException(e);
        } catch (Throwable e) { //also catch system errors: unsatisfied link error etc.
            Logger.wtf("DRunnable", e);

            updateProgress(DState.MUX_ERROR);
            throw new MuxException(e);
        }
    }

    private void encodeAudio() throws MuxException {

        updateProgress(DState.ENCODING);

        try {

            MuxStatCallback statisticsCallback = new MuxStatCallback() {
                @Override
                public void onStatisticsUpdated(int time, int totalTime) {
                    dProgress.setTime(time);
                    dProgress.setTotalTime(totalTime);
                    updateProgress(DState.ENCODING);
                }

                @Override
                public void onMuxFinished(long fileSize) {
                    if (fileSize > 0) { //FIXME this overrides the size of stream, use different tables to save final data
                        String audioStr = DInfoHelper.getInstance(context).getInfoString(getTableName(), dBundle.getDownloadId());
                        DInfoHelper.getInstance(context).updateCountAndLengthWithInfoString(getTableName(), dBundle.getDownloadId(), audioStr, fileSize, fileSize);
                        dProgress.setCount(fileSize);
                        dProgress.setLength(fileSize);
                    }
                    //updateProgress(DState.ENCODING); the complete will send it
                }
            };

            dBundle.encodeAudio(statisticsCallback);

        } catch (InterruptedException | InterruptedIOException e) {
            updateProgress(DState.STOPPED);
            throw new MuxException(e);
        } catch (Throwable e) { //also catch system errors: unsatisfied link error etc.
            Logger.wtf("DRunnable", e);

            updateProgress(DState.ENCODE_ERROR);
            throw new MuxException(e);
        }
    }

    private void updateYtUrls() throws Exception {

        updateProgress(DState.PARSING);

        try {
            dBundle.updateBundle();

        } catch (InterruptedException | InterruptedIOException e) {
            updateProgress(DState.STOPPED);
            throw e;
        } catch (Exception e) {
            Logger.wtf("DRunnable", e);

            updateProgress(DState.ERROR);
            throw e;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        String a = getDownloadUid();
        String b = ((DRunnable) o).getDownloadUid();
        return a != null && a.equals(b);
    }
}
