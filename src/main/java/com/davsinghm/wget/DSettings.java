package com.davsinghm.wget;

public class DSettings {

    private boolean useMultipart;
    private int threadCount;
    private int mtStyle;
    private long minPartLength;
    private long maxPartLength;

    public boolean useMultipart() {
        return useMultipart;
    }

    public void setMultipart(boolean useMultipart) {
        this.useMultipart = useMultipart;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public int getMtStyle() {
        return mtStyle;
    }

    public void setMtStyle(int mtStyle) {
        this.mtStyle = mtStyle;
    }

    public long getMinPartLength() {
        return minPartLength;
    }

    public void setMinPartLength(long minPartLength) {
        this.minPartLength = minPartLength;
    }

    public long getMaxPartLength() {
        return maxPartLength;
    }

    public void setMaxPartLength(long maxPartLength) {
        this.maxPartLength = maxPartLength;
    }

    @SuppressWarnings("HardCodedStringLiteral")
    public void answers() {
        //TODO send stats
        /*CustomEvent multipartEvent = new CustomEvent("Multi-Thread").putCustomAttribute("Enabled", String.valueOf(useMultipart));
        if (useMultipart) {
            switch (mtStyle) {
                case Constants.MT_STYLE_CLASSIC:
                    multipartEvent.putCustomAttribute("Min Thread Size", (minPartLength / 1024) + " KiB");
                    multipartEvent.putCustomAttribute("Method", "Classic");
                    multipartEvent.putCustomAttribute("Thread Count", threadCount);
                    break;
                case Constants.MT_STYLE_SMART:
                    multipartEvent.putCustomAttribute("Min Thread Size", (minPartLength / 1024) + " KiB");
                    multipartEvent.putCustomAttribute("Method", "Smart");
                    multipartEvent.putCustomAttribute("Thread Count", threadCount);
                    break;
                case Constants.MT_STYLE_AXET:
                    multipartEvent.putCustomAttribute("Piece Size", (maxPartLength / 1024 / 1024) + " MiB");
                    multipartEvent.putCustomAttribute("Method", "Pieces");
                    multipartEvent.putCustomAttribute("Thread Count", threadCount);
            }
        }

        Crash.answers().logCustom(multipartEvent);*/
    }
}
