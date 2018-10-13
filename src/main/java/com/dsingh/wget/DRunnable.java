package com.dsingh.wget;

import android.content.Context;
import android.os.Build;

import com.dsingh.wget.core.DirectMultipart;
import com.dsingh.wget.core.DirectRange;
import com.dsingh.wget.core.DirectSingle;
import com.dsingh.wget.core.DirectSingleBg;
import com.dsingh.wget.core.info.DownloadInfo;
import com.dsingh.wget.core.info.Part;
import com.dsingh.wget.core.info.State;
import com.dsingh.wget.core.info.ex.DownloadError;
import com.dsingh.wget.core.info.ex.DownloadInterruptedError;
import com.dsingh.wget.core.info.ex.DownloadMultipartError;
import com.dsingh.wget.core.info.ex.MuxException;

import java.io.File;
import java.io.InterruptedIOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;

public class DRunnable implements Runnable {

    private Context context;
    private DBundle mDBundle;
    private TaskRunnable mDTaskInterface;
    private AtomicBoolean mStop;
    private AtomicBoolean mFinished = new AtomicBoolean(false);
    private DProgress mDProgress;
    private DownloadInfo mDInfo;

    private BlockingQueue<Job> mJobQueue;

    private Job mCurrentJob;
    private DState mLastDState = DState.QUEUED; // used to display in activity;

    private long mLastTimestamp;

    public DRunnable(Context context, TaskRunnable dTask, DBundle dBundle, AtomicBoolean stop) {
        this.context = context;
        mDBundle = dBundle;
        mDTaskInterface = dTask;
        mStop = stop;
    }

    public DState getState() {
        return mLastDState;
    }

    @Override
    public void run() {

        mDTaskInterface.setDownloadThread(Thread.currentThread());
        //Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        if (mStop.get())
            return;

        try {

            mDProgress = new DProgress(mDBundle);

            mJobQueue = mDBundle.getJobQueue();

            doNextJob();

        } catch (DownloadMultipartError e) {  //TODO improve, backport add suppressed ? + do all wget exception logging
            Logs.wtf("DRunnable", "DownloadMultipartError");

            if (e.getInfo() != null && e.getInfo().getPartList() != null)
                for (Part p : e.getInfo().getPartList()) {
                    String partID = "Part " + (p.getNumber() + 1) + "/" + e.getInfo().getPartList().size() + ": "; //NON-NLS
                    Logs.e("DRunnable: " + getDInfoID(), partID + "ERROR | start: " + p.getStart() + ", end: " + p.getEnd() + ", count: " + p.getCount() + ", length: " + p.getLength());
                    Throwable ee = p.getException();
                    if (ee != null) {
                        Logs.e("DRunnable: " + getDInfoID() + ": Part: " + (p.getNumber() + 1) + " : PartException", ee.toString());
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                            e.addSuppressed(ee);
                        StackTraceElement[] steS = ee.getStackTrace();
                        for (StackTraceElement ste : steS)
                            Logs.e("DRunnable: " + getDInfoID() + ": PartException", ste.toString());
                        Logs.e("DRunnable: " + getDInfoID() + ": PartException", "End of StackTraceElement (Part: " + (p.getNumber() + 1) + ")\n");
                    }
                }
            Logs.wtf("DRunnable: " + getDInfoID(), e);

        } catch (DownloadInterruptedError e) {
            Logs.w("DRunnable: " + getDInfoID(), e);
        } catch (MuxException e) {
            Logs.wtf("DRunnable: " + getDInfoID(), e);
        } catch (Exception e) {
            Logs.wtf("DRunnable: " + getDInfoID(), e);
            mStop.set(true);
        }
    }

    private void doNextJob() throws Exception {

        while ((mCurrentJob = mJobQueue.poll()) != null) {

            Logs.d("DRunnable", "doNextJob(): " + mCurrentJob);

            switch (mCurrentJob) {
                case PARSE:
                    updateYtUrls();
                    break;
                case VIDEO:
                    download(mDBundle.getVideoUrl(), mDBundle.getVideoFile());
                    break;
                case AUDIO:
                    download(mDBundle.getAudioUrl(), mDBundle.getAudioFile());
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

        Logs.d("DRunnable", "doNextJob(): out of loop");

        updateProgress(DState.COMPLETE);
    }

    private String getTableName() {
        if (mCurrentJob == null)
            return mDBundle.isAudioOnly() ? DInfoHelper.TABLE_AUDIO : DInfoHelper.TABLE_VIDEO;

        switch (mCurrentJob) {
            case PARSE:
                return mDBundle.isAudioOnly() ? DInfoHelper.TABLE_AUDIO : DInfoHelper.TABLE_VIDEO;
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
        return mDBundle.getDownloadUid() + " " + "[" + (mCurrentJob != null ? mCurrentJob.toString().substring(0, 1) : "N") + (mDBundle.isTwoPartDownload() ? "|2" : "") + "]"; //NON-NLS
    }

    private void downloadSubtitles() {
        try {

            updateProgress(DState.EXTRACTING);

            File file = mDBundle.getSubtitleFile();

            new DirectSingleBg(mDBundle.getSubtitleUrl(), file).downloadPart(mStop);

        } catch (Exception e) { //TODO make fatal
            Logs.wtf("DRunnable: Non-Fatal, Critical: downloadSubtitles(): Failed to load subtitles.", e);
        }
    }

    private void download(String url, File target) throws Exception {

        /*while (!mStop.get()) {
            updateProgress(DState.RETRYING);
        }

//        Thread.currentThread().sleep(2000);
        updateProgress(DState.STOPPED);
        if (true)
            throw new DownloadInterruptedError();*/

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
                        DInfoHelper.getInstance(context).addInfo(getTableName(), mDBundle.getDownloadUid(), mDInfo.toString(), "DONE");
                        break;
                }
            }
        };

        DSettings dSettings = mDBundle.getDSettings();

        mDInfo = new DownloadInfo(new URL(url));
        mDInfo.setDInfoID(getDInfoID());
        mDInfo.setDSettings(dSettings);
        mDInfo.extract(mStop, notify);
        mDInfo.fromString(notify, DInfoHelper.getInstance(context).getInfoString(getTableName(), mDBundle.getDownloadUid()));

        if (mDInfo.isMultipart()) {
            Logs.d("WGet", "createDirect(): MultiPart");
            new DirectMultipart(mDInfo, target).download(mStop, notify);
        } else if (mDInfo.hasRange()) {
            Logs.d("WGet", "createDirect(): Range");
            new DirectRange(mDInfo, target).download(mStop, notify);
        } else {
            Logs.d("WGet", "createDirect(): Single");
            new DirectSingle(mDInfo, target).download(mStop, notify);
        }

        if (mDInfo.getState() != State.DONE)
            throw new DownloadError("Test: thrown if State != DONE to exit job loop");
    }

    synchronized private void updateProgress(DState dState) {

        boolean bool = System.currentTimeMillis() - mLastTimestamp > Constants.DRUNNABLE_PROGRESS_UPDATE_INTERVAL || mLastDState != dState;
        if (!mFinished.get() && bool) {
            int onGoing = 0;
            mDProgress.setDState(dState);
            mDProgress.setShowAudio(mDBundle.isTwoPartDownload() && mCurrentJob == Job.AUDIO);

            switch (dState) {
                case QUEUED:
                case EXTRACTING:
                case PARSING:
                case RETRYING:
                    onGoing = 1;
                    DInfoHelper.getInstance(context).addInfoState(getTableName(), mDBundle.getDownloadUid(), "ONGOING");
                    break;
                case MUXING:
                case ENCODING:
                    onGoing = 1;
                    DInfoHelper.getInstance(context).addInfoState(getTableName(), mDBundle.getDownloadUid(), "ONGOING");
                    break;
                case DOWNLOADING:
                    onGoing = 1;
                    mDProgress.setCount(mDInfo.getCount());
                    mDProgress.setLength(mDInfo.getLength() != null ? mDInfo.getLength() : 0);
                    mDProgress.setAvgSpeed(mDInfo.getSpeedInfo().getAverageSpeed());
                    mDProgress.setCurrentSpeed(mDInfo.getSpeedInfo().getCurrentSpeed());
                    mDProgress.setThreadCount(activeThreadCount());
                    DInfoHelper.getInstance(context).addInfo(getTableName(), mDBundle.getDownloadUid(), mDInfo.toString(), "ONGOING");
                    break;
                case COMPLETE:
                    //if two part, update total count and size/length. this part is added quite later
                    if (mDBundle.isTwoPartDownload()) {
                        //need to load dinfos again, as only one is avail.
                        String videoStr = DInfoHelper.getInstance(context).getInfoString(DInfoHelper.TABLE_VIDEO, mDBundle.getDownloadUid());
                        String audioStr = DInfoHelper.getInstance(context).getInfoString(DInfoHelper.TABLE_AUDIO, mDBundle.getDownloadUid());
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

                    mDBundle.onDownloadComplete();

                case ERROR:
                case STOPPED:
                case MUX_ERROR:
                case ENCODE_ERROR:
                    DInfoHelper.getInstance(context).addInfoState(getTableName(), mDBundle.getDownloadUid(), dState.toString());
                    mFinished.set(true);
                    break;
            }

            mLastDState = dState;

            if (mFinished.get()) {
                mDTaskInterface.removeFromQueue();
                mDTaskInterface.setDownloadThread(null);
                Thread.interrupted();
                mDTaskInterface.progressUpdate(onGoing, mDProgress);

                //Broadcaster.downloadFinished();

            } else
                mDTaskInterface.progressUpdate(onGoing, mDProgress);

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
                Logs.w("DRunnable: " + getDInfoID() + ": Part " + (part.getNumber() + 1) + "/" + arrayList.size() + ", PartState", part.getState().toString() + ", Count: " + (part.getCount() / 1024) + "kb, Length: " + (part.getLength() / 1024) + "kb");
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

            mDBundle.muxAllFiles();

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

            mDBundle.updateYtUrls();

        } catch (InterruptedException | InterruptedIOException e) {
            updateProgress(DState.STOPPED);
            throw e;
        } catch (Exception e) {
            updateProgress(DState.ERROR);
            throw e;
        }
    }
}
