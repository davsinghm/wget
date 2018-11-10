package com.davsinghm.wget;

import android.content.Context;
import android.os.Build;

import com.davsinghm.wget.core.DirectMultipart;
import com.davsinghm.wget.core.DirectRange;
import com.davsinghm.wget.core.DirectSingle;
import com.davsinghm.wget.core.DirectSingleBg;
import com.davsinghm.wget.core.info.DownloadInfo;
import com.davsinghm.wget.core.info.Part;
import com.davsinghm.wget.core.info.State;
import com.davsinghm.wget.core.info.ex.DownloadError;
import com.davsinghm.wget.core.info.ex.DownloadInterruptedError;
import com.davsinghm.wget.core.info.ex.DownloadMultipartError;
import com.davsinghm.wget.core.info.ex.MuxException;

import java.io.File;
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

    private AtomicBoolean mStop = new AtomicBoolean(false);
    private AtomicBoolean mFinished = new AtomicBoolean(false);
    private DProgress mDProgress;
    private DownloadInfo mDInfo;

    private BlockingQueue<Job> mJobQueue;

    private Job mCurrentJob;
    private DState mLastDState = DState.QUEUED; // used to display in activity;

    private long mLastTimestamp;

    @Nullable
    private Thread currentThread;

    private void setCurrentThread(Thread currentThread) {
        this.currentThread = currentThread;
    }

    Thread getCurrentThread() {
        return currentThread;
    }

    DRunnable(DManager dManager, DBundle dBundle) {
        this.dManager = dManager;
        this.dBundle = dBundle;
        this.context = dManager.getAppContext();
    }

    DBundle getDBundle() {
        return dBundle;
    }

    String getDownloadUid() {
        return dBundle.getDownloadUid();
    }

    private void progressUpdate(int what, DProgress progress) {
        dManager.progressUpdate(what, progress);
    }

    void stopDownload() {
        mStop.set(true);
        if (currentThread != null)
            currentThread.interrupt();
    }

    DState getState() {
        return mLastDState;
    }

    @Override
    public void run() {

        if (mStop.get())
            return;

        setCurrentThread(Thread.currentThread());
        //Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        try {

            mDProgress = new DProgress(dBundle);

            mJobQueue = dBundle.getJobQueue();

            doNextJob();

        } catch (Exception e) {
            mStop.set(true);
        }
    }

    private void doNextJob() throws Exception {

        while ((mCurrentJob = mJobQueue.poll()) != null) {

            Logger.d("DRunnable", "doNextJob(): " + mCurrentJob);

            switch (mCurrentJob) {
                case PARSE:
                    updateYtUrls();
                    break;
                case VIDEO:
                    download(dBundle.getVideoUrl(), dBundle.getVideoFile());
                    break;
                case AUDIO:
                    download(dBundle.getAudioUrl(), dBundle.getAudioFile());
                    break;
                case SUBTITLE:
                    downloadSubtitles();
                    break;
                case ENCODE:
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
        if (mCurrentJob == null)
            return dBundle.isAudioOnly() ? DInfoHelper.TABLE_AUDIO : DInfoHelper.TABLE_VIDEO;

        switch (mCurrentJob) {
            case PARSE:
                return dBundle.isAudioOnly() ? DInfoHelper.TABLE_AUDIO : DInfoHelper.TABLE_VIDEO;
            case VIDEO:
            case SUBTITLE:
            case MUX:
                return DInfoHelper.TABLE_VIDEO;
            case AUDIO:
            case ENCODE:
                return DInfoHelper.TABLE_AUDIO;
        }
        return null;
    }

    @NonNull
    private String getDInfoID() {
        return dBundle.getDownloadUid() + " " + "[" + (mCurrentJob != null ? mCurrentJob.toString().substring(0, 1) : "N") + (dBundle.isTwoPartDownload() ? "|2" : "") + "]"; //NON-NLS
    }

    private void downloadSubtitles() {

        try {
            updateProgress(DState.EXTRACTING);

            File file = dBundle.getSubtitleFile();

            new DirectSingleBg(context, dBundle.getSubtitleUrl(), file).downloadPart(mStop);

        } catch (Exception e) { //TODO make fatal
            Logger.wtf("DRunnable: Non-Fatal, Critical: downloadSubtitles(): Failed to load subtitles.", e);
        }
    }

    private void download(String url, File target) throws Exception {

       /* while (!mStop.get()) {
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
                switch (mDInfo.getState()) {
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
                        DInfoHelper.getInstance(context).addInfo(getTableName(), dBundle.getDownloadUid(), mDInfo.toString(), "DONE");
                        break;
                }
            }
        };

        try {
            DSettings dSettings = dBundle.getDSettings();

            mDInfo = new DownloadInfo(new URL(url));
            mDInfo.setDInfoID(getDInfoID());
            mDInfo.setDSettings(dSettings);
            mDInfo.extract(context, mStop, notify);
            mDInfo.fromString(notify, DInfoHelper.getInstance(context).getInfoString(getTableName(), dBundle.getDownloadUid()));

            if (mDInfo.isMultipart()) {
                Logger.d("WGet", "createDirect(): MultiPart");
                new DirectMultipart(context, mDInfo, target).download(mStop, notify);
            } else if (mDInfo.hasRange()) {
                Logger.d("WGet", "createDirect(): Range");
                new DirectRange(context, mDInfo, target).download(mStop, notify);
            } else {
                Logger.d("WGet", "createDirect(): Single");
                new DirectSingle(context, mDInfo, target).download(mStop, notify);
            }

        } catch (DownloadMultipartError e) {  //TODO improve, backport add suppressed ? + do all wget exception logging

            if (e.getInfo() != null && e.getInfo().getPartList() != null)
                for (Part p : e.getInfo().getPartList()) {
                    String partID = "Part " + (p.getNumber() + 1) + "/" + e.getInfo().getPartList().size() + ": "; //NON-NLS
                    Logger.e("DRunnable: " + getDInfoID(), partID + "ERROR | start: " + p.getStart() + ", end: " + p.getEnd() + ", count: " + p.getCount() + ", length: " + p.getLength());
                    Throwable ee = p.getException();
                    if (ee != null) {
                        Logger.e("DRunnable: " + getDInfoID() + ": Part: " + (p.getNumber() + 1) + " : PartException", ee.toString());
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                            e.addSuppressed(ee);
                        StackTraceElement[] steS = ee.getStackTrace();
                        for (StackTraceElement ste : steS)
                            Logger.e("DRunnable: " + getDInfoID() + ": PartException", ste.toString());
                        Logger.e("DRunnable: " + getDInfoID() + ": PartException", "End of StackTraceElement (Part: " + (p.getNumber() + 1) + ")\n");
                    }
                }
            Logger.wtf("DRunnable: " + getDInfoID(), e);

            throw e;

        } catch (DownloadInterruptedError e) {
            updateProgress(DState.STOPPED);
            Logger.w("DRunnable: " + getDInfoID(), e);

            throw e;
        } catch (Exception e) {
            updateProgress(DState.ERROR);
            mStop.set(true);

            Logger.wtf("DRunnable: " + getDInfoID(), e);

            throw e;
        }

        if (mDInfo.getState() != State.DONE) {
            updateProgress(DState.ERROR);
            throw new DownloadError("Test: thrown if State != DONE to exit job loop");
        }
    }

    synchronized private void updateProgress(DState dState) {

        boolean bool = System.currentTimeMillis() - mLastTimestamp > Constants.DRUNNABLE_PROGRESS_UPDATE_INTERVAL || mLastDState != dState;
        if (!mFinished.get() && bool) {
            int onGoing = 0;
            mDProgress.setDState(dState);
            mDProgress.setShowAudio(dBundle.isTwoPartDownload() && mCurrentJob == Job.AUDIO);

            switch (dState) {
                case QUEUED:
                case EXTRACTING:
                case PARSING:
                case RETRYING:
                    onGoing = 1;
                    DInfoHelper.getInstance(context).addInfoState(getTableName(), dBundle.getDownloadUid(), "ONGOING");
                    break;
                case MUXING:
                case ENCODING:
                    onGoing = 1;
                    DInfoHelper.getInstance(context).addInfoState(getTableName(), dBundle.getDownloadUid(), "ONGOING");
                    break;
                case DOWNLOADING:
                    onGoing = 1;
                    mDProgress.setCount(mDInfo.getCount());
                    mDProgress.setLength(mDInfo.getLength() != null ? mDInfo.getLength() : 0);
                    mDProgress.setAvgSpeed(mDInfo.getSpeedInfo().getAverageSpeed());
                    mDProgress.setCurrentSpeed(mDInfo.getSpeedInfo().getCurrentSpeed());
                    mDProgress.setThreadCount(activeThreadCount());
                    DInfoHelper.getInstance(context).addInfo(getTableName(), dBundle.getDownloadUid(), mDInfo.toString(), "ONGOING");
                    break;
                case COMPLETE:
                    //if two part, update total count and size/length. this part is added quite later
                    if (dBundle.isTwoPartDownload()) {
                        //need to load dinfos again, as only one is avail.
                        String videoStr = DInfoHelper.getInstance(context).getInfoString(DInfoHelper.TABLE_VIDEO, dBundle.getDownloadUid());
                        String audioStr = DInfoHelper.getInstance(context).getInfoString(DInfoHelper.TABLE_AUDIO, dBundle.getDownloadUid());
                        //also add subtitles?
                        long length = 0;
                        long count = 0;
                        length += DInfoHelper.getLengthFromInfoString(videoStr);
                        length += DInfoHelper.getLengthFromInfoString(audioStr);
                        count += DInfoHelper.getCountFromInfoString(videoStr);
                        count += DInfoHelper.getCountFromInfoString(audioStr);

                        mDProgress.setLength(length);
                        mDProgress.setCount(count);
                        //TODO instead of sum of two files, should we just update content length in dbundle table with filesize? that way encoding one also be supported.
                    } //if encoding is supported. can update content length of newly encoded file.

                    dBundle.onDownloadComplete();

                case ERROR:
                case STOPPED:
                case MUX_ERROR:
                case ENCODE_ERROR:
                    DInfoHelper.getInstance(context).addInfoState(getTableName(), dBundle.getDownloadUid(), dState.toString());
                    mFinished.set(true);
                    break;
            }

            if (mFinished.get()) {

                if (mLastDState != DState.COMPLETE && mLastDState != DState.ERROR && mLastDState != DState.STOPPED && mLastDState != DState.MUX_ERROR && mLastDState != DState.ENCODE_ERROR) {
                    setCurrentThread(null);
                    Thread.interrupted();
                    progressUpdate(onGoing == 1 ? DService.MESSAGE_PROGRESS_ONGOING : DService.MESSAGE_PROGRESS_ENDED, mDProgress);

                    dManager.removeFromQueue(this);
                }

            } else
                progressUpdate(onGoing, mDProgress);

            mLastDState = dState;
            mLastTimestamp = System.currentTimeMillis();
        }
    }

    private String activeThreadCount() {
        if (mDInfo.isMultipart()) {
            List<Part> arrayList = mDInfo.getPartList();
            int running = 0;
            for (Part part : arrayList) {
                if (part.getState().equals(State.DOWNLOADING) || part.getState().equals(State.RETRYING))
                    running++;
                Logger.w("DRunnable: " + getDInfoID() + ": Part " + (part.getNumber() + 1) + "/" + arrayList.size() + ", PartState", part.getState().toString() + ", Count: " + (part.getCount() / 1024) + "kb, Length: " + (part.getLength() / 1024) + "kb");
            }
            if (running == 0)
                return null;
            return "[" + running + "/" + mDInfo.getDSettings().getThreadCount() + "]";
        }
        return null;
    }

    private void muxAudioVideo() throws MuxException {

        updateProgress(DState.MUXING);

        try {
            dBundle.muxAllFiles();

        } catch (InterruptedException | InterruptedIOException e) {
            updateProgress(DState.STOPPED);
            throw new MuxException(e);
        } catch (Exception e) {
            updateProgress(DState.MUX_ERROR);
            throw new MuxException(e);
        }
    }

    private void updateYtUrls() throws Exception {

        updateProgress(DState.PARSING);

        try {
            dBundle.updateYtUrls();

        } catch (InterruptedException | InterruptedIOException e) {
            updateProgress(DState.STOPPED);
            throw e;
        } catch (Exception e) {
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
