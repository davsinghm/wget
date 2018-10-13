package com.dsingh.wget.core.info;

import com.dsingh.wget.Constants;
import com.dsingh.wget.DSettings;
import com.dsingh.wget.Logs;
import com.dsingh.wget.core.SpeedInfo;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * DownloadInfo class. Keep part information. We need to serialize this class
 * between application restart. Thread safe.
 */
public class DownloadInfo extends URLInfo {

    private DSettings dSettings;
    private String dinfoId;
    private SpeedInfo speedInfo = new SpeedInfo();
    private List<Part> partList;
    private long count; // total bytes downloaded. for chunk download progress info. for one thread count - also local file size;

    public DownloadInfo(URL source) {
        super(source);
    }

    @Override
    public void extract(final AtomicBoolean stop, final Runnable notify) {
        Logs.d("WGet: DownloadInfo: " + getDInfoID(), "extract(): invoked");
        super.extract(stop, notify);
    }

    synchronized public DSettings getDSettings() {
        return dSettings;
    }

    public void setDSettings(DSettings dSettings) {
        this.dSettings = dSettings;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    synchronized public String getDInfoID() {
        return dinfoId;
    }

    synchronized public void setDInfoID(String id) {
        this.dinfoId = id;
    }

    synchronized public SpeedInfo getSpeedInfo() {
        return speedInfo;
    }

    synchronized public void updateSpeed() {
        getSpeedInfo().step(getCount());
    }

    synchronized public List<Part> getPartList() {
        return partList;
    }

    synchronized public List<Part> getSortedPartList() {
        synchronized (getPartList()) {
            if (getPartList() != null)
                Collections.sort(getPartList(), Part.PartComparator);
            return getPartList();
        }
    }

    @Override
    synchronized public State getState() {
        if (isMultipart()) {
            boolean atLeastOneRetrying = false;
            for (Part part : getPartList()) {
                if (part.getState().equals(State.DOWNLOADING))
                    return State.DOWNLOADING;
                if (part.getState().equals(State.RETRYING))
                    atLeastOneRetrying = true;
            }
            if (atLeastOneRetrying)
                return State.RETRYING;
        }
        return super.getState();
    }

    synchronized public boolean isMultipart() {
        return hasRange() && partList != null;
    }

    synchronized public void updateMultipartCount() {
        setCount(0);

        for (Part p : getPartList())
            setCount(getCount() + p.getCount());

        updateSpeed();
    }

    public void enableMultipart(Runnable notify) {

        if (isEmpty()) {
            Logs.d("WGet: DownloadInfo: " + getDInfoID(), "enableMultipart(): isEmpty: true, throwing RuntimeException");
            setState(State.ERROR);
            notify.run();

            throw new RuntimeException("Empty Download info, can't set multipart");
        }

        if (!hasRange()) {
            Logs.d("WGet: DownloadInfo: " + getDInfoID(), "enableMultipart(): hasRange: false, reporting non-fatal ex & skipping enable.");
            //TODO Crash.lytics().logException(new DownloadMultipartError("Server doesn't support Range. Download as Single."));
            return;
        }

        switch (dSettings.getMtStyle()) {
            case Constants.MT_STYLE_CLASSIC:
            case Constants.MT_STYLE_SMART:
                enableClassicMP();
                break;
            case Constants.MT_STYLE_AXET:
                enableAxetMP();
                break;
        }
    }

    public void enableAxetMP() {
        Logs.d("WGet: DownloadInfo: " + getDInfoID(), "enableAxetMP(): invoked");

        long partLength = dSettings.getMaxPartLength();
        long noOfParts = getLength() / partLength + 1;

        if (noOfParts > 2) {
            partList = new ArrayList<>();

            long start = 0;
            for (int i = 0; i < noOfParts; i++) {

                Part part = new Part();
                part.setNumber(i);

                part.setStart(start);
                part.setEnd(i == noOfParts - 1 ? (getLength() - 1) : (start + partLength - 1));
                part.setState(State.QUEUED);

                //TODO Part.setCount from DB.
                if (part.getEnd() - part.getStart() + 1 != 0)
                    partList.add(part);

                start += partLength;
            }

            updateMultipartCount();
            getSpeedInfo().start(getCount());
        }

    }

    public void enableClassicMP() {
        Logs.d("WGet: DownloadInfo: " + getDInfoID(), "enableClassicMP(): invoked");

        partList = new ArrayList<>();

        int noOfParts = dSettings.getThreadCount();
        long partLength = getLength() / noOfParts;

        if (partLength < dSettings.getMinPartLength()) {

            for (int i = noOfParts; i >= 1; i--) {
                noOfParts = i;
                partLength = getLength() / noOfParts;
                if (partLength > dSettings.getMinPartLength())
                    break;
            }
        }

        if (noOfParts == 1) {
            partList = null;
            return;
        }

        long start = 0;
        for (int i = 0; i < noOfParts; i++) {
            Part part = new Part();
            part.setNumber(i);
            part.setStart(start);
            part.setEnd(i == noOfParts - 1 ? (getLength() - 1) : (start + partLength - 1));
            part.setState(State.QUEUED);
            partList.add(part);

            start += partLength;
        }

        updateMultipartCount();
        getSpeedInfo().start(getCount());
    }

    public void useDefaultSettings(Runnable notify) {

        Logs.d("WGet: DownloadInfo: " + getDInfoID(), "Using default settings!");

        setCount(0);
        partList = null;

        if (dSettings.useMultipart())
            enableMultipart(notify);
    }

    @SuppressWarnings("HardCodedStringLiteral")
    public void fromString(Runnable notify, String string) { //TODO remove notify from arg

        Logs.d("WGet: DownloadInfo: " + getDInfoID(), "fromString(): invoked | Settings: useMultipart: " + dSettings.useMultipart() + ", threadCount: " + dSettings.getThreadCount() + ", mtStyle: " + dSettings.getMtStyle() + ", minPartLength: " + dSettings.getMinPartLength() + ", maxPartLength: " + dSettings.getMaxPartLength());

        if (string == null || !hasRange()) {
            Logs.d("WGet: DownloadInfo: " + getDInfoID(), "fromString(): String is null || !hasRange(), useDefaultSettings()");
            useDefaultSettings(notify);
            return;
        }

        try {
            String[] ss1 = string.split("<l>");
            long count = Long.valueOf(ss1[0]);
            setCount(count);
            long length = Long.valueOf(ss1[1]);
            if (getLength() != length)
                throw new IllegalStateException("Non-Critical: Stored length: " + length + " doesn't matches with getLength(): " + getLength()
                + ". Using default settings, setting count to 0.");

            DSettings dSettings = new DSettings();
            dSettings.setMultipart(Boolean.valueOf(ss1[2]));
            dSettings.setThreadCount(Integer.valueOf(ss1[3]));
            dSettings.setMtStyle(Integer.valueOf(ss1[4]));
            dSettings.setMinPartLength(Long.valueOf(ss1[5]));
            dSettings.setMaxPartLength(Long.valueOf(ss1[6]));

            String pS = ss1[7];

            if (!pS.equals("none")) {
                partList = new ArrayList<>();
                String[] parts = pS.split("<p>");
                for (String p : parts) {
                    String[] pSs = p.split("<i>");
                    Part part = new Part();
                    part.setNumber(Integer.valueOf(pSs[0]));
                    part.setStart(Long.valueOf(pSs[1]));
                    part.setEnd(Long.valueOf(pSs[2]));
                    part.setCount(Long.valueOf(pSs[3]));
                    part.setState(State.QUEUED);
                    partList.add(part);

                }
                updateMultipartCount();
                getSpeedInfo().start(getCount());
            }

            this.dSettings = dSettings;

            this.dSettings.answers();

        } catch (Exception e) {
            useDefaultSettings(notify);
            Logs.wtf("WGet: DownloadInfo: " + getDInfoID(), "Load settings failed: " + string, e);
        }

    }

    @SuppressWarnings("HardCodedStringLiteral")
    public String toString() {
        if (getLength() == 0) // if !hasRange() => length = 0, don't save anything
            return null;

        StringBuilder sb = new StringBuilder();
        sb.append(getCount()).append("<l>"); // 0
        sb.append(getLength()).append("<l>"); // 1
        sb.append(dSettings.useMultipart()).append("<l>"); //2
        sb.append(dSettings.getThreadCount()).append("<l>"); // 3
        sb.append(dSettings.getMtStyle()).append("<l>"); // 4
        sb.append(dSettings.getMinPartLength()).append("<l>"); //5
        sb.append(dSettings.getMaxPartLength()).append("<l>"); //6...//7

        int partListSize;
        if (getPartList() != null && ((partListSize = getPartList().size()) > 0)) {
            String parts = "";
            for (int i = 0; i < partListSize; i++) {
                Part part = getPartList().get(i);
                parts += part.getNumber() + "<i>" + part.getStart() + "<i>" + part.getEnd() + "<i>" + part.getCount();
                if (i != partListSize - 1)
                    parts += "<p>";
            }
            sb.append(parts);
        } else
            sb.append("none");

        return sb.toString();
    }

    synchronized public void reset() {
        setCount(0);
        getSpeedInfo().start(0); //TODO verify test

        if (partList != null) {
            for (Part p : partList) {
                p.setCount(0);
                p.setState(State.QUEUED);
            }
        }
    }

    /**
     * Check if we can continue download a file from new source. Check if new
     * souce has the same file length, title. and supports for range
     *
     * @param newSource new source
     * @return true - possible to resume from new location
     */
    synchronized public boolean resume(DownloadInfo newSource) {
        if (!newSource.hasRange())
            return false;

        if (newSource.getContentFilename() != null && this.getContentFilename() != null) {
            if (!newSource.getContentFilename().equals(this.getContentFilename()))
                // one source has different name
                return false;
        } else if (newSource.getContentFilename() != null || this.getContentFilename() != null) {
            // one source has a have old is not
            return false;
        }

        if (newSource.getLength() != null && this.getLength() != null) {
            if (!newSource.getLength().equals(this.getLength()))
                // one source has different length
                return false;
        } else if (newSource.getLength() != null || this.getLength() != null) {
            // one source has length, other is not
            return false;
        }

        if (newSource.getContentType() != null && this.getContentType() != null) {
            if (!newSource.getContentType().equals(this.getContentType()))
                // one source has different getContentType
                return false;
        } else if (newSource.getContentType() != null || this.getContentType() != null) {
            // one source has a have old is not
            return false;
        }

        return true;
    }

    /**
     * copy resume data from oldSource;
     */
    synchronized public void copy(DownloadInfo oldSource) {
        setCount(oldSource.getCount());
        partList = oldSource.partList;
    }
}
