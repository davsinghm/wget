package com.dsingh.wget;

public interface TaskRunnable {

    void setDownloadThread(Thread currentThread);

    void progressUpdate(int isOnGoing, DProgress progress);

    boolean removeFromQueue();

}