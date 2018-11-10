package com.davsinghm.wget.core.threads;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 
 * SynchronousQueue - hung while running. Seems like a bug in java (Max OSX Java
 * 1.7.0-25)
 * 
 * Unsafe.park(boolean, long) line: not available [native method] [local
 * variables unavailable]
 * 
 * LockSupport.park(Object) line: 186
 * 
 * SynchronousQueue$TransferStack.awaitFulfill(
 * SynchronousQueue$TransferStack$SNode, boolean, long) line: 458
 * 
 * 
 * @author axet
 * 
 */
public class LimitThreadPool extends ThreadPoolExecutor {
    private final Object lock = new Object();
    private int count = 0;

    protected static class BlockUntilFree implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            // Access to the task queue is intended primarily for
            // debugging and monitoring. This queue may be in active use.
            //
            // So we are a little bit off road here :) But since we have
            // full control over executor we are safe.
            try {
                executor.getQueue().put(r);
            } catch (InterruptedException e) {
                // since we could not rethrow interrupted exception. mark thread
                // as interrupted. and check thread status later in
                // blockExecute()
                Thread.currentThread().interrupt();
            }
        }
    }

    public LimitThreadPool(int maxThreadCount) {
        super(maxThreadCount, maxThreadCount, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1), new BlockUntilFree());
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);

        synchronized (lock) {
            count--;
            lock.notifyAll();
        }
    }

    /**
     * downloader working if here any getTasks() > 0
     */
    public boolean active() {
        synchronized (lock) {
            return (count) > 0;
        }
    }

    /**
     * Wait until current task ends. if here is no tasks exit immediately.
     * 
     * @throws InterruptedException
     * 
     */
    public void waitUntilNextTaskEnds() throws InterruptedException {
        synchronized (lock) {
            if (active()) {
                lock.wait();
            }
        }
    }

    /**
     * Wait until thread pool execute its last task. Waits forever until end.
     * 
     * @throws InterruptedException
     * 
     */
    public void waitUntilTermination() throws InterruptedException {
        synchronized (lock) {
            while (active())
                waitUntilNextTaskEnds();
        }
    }

    /**
     * You should not call this method on this Limited Version Thread Pool. Use
     * blockExecute() instead.
     *
    Override
    public void execute(Runnable command) {
        SafetyCheck s = (SafetyCheck) command;

        super.execute(s.getCause());
    }*/

    public void blockExecute(Runnable runnable) throws InterruptedException {
        synchronized (lock) {
            count++;
        }
        execute(runnable);

        if (Thread.interrupted())
            throw new InterruptedException();
    }
}