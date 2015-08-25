/**
 * Copyright (c) 2009 - 2010 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 *
 * This file is part of org.appwork.utils.event.queue
 *
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.event.queue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.utils.logging.Log;

/**
 * @author daniel
 * @param <D>
 * @param <T>
 *
 */
public abstract class Queue {

    protected final Object queueLock = new Object();

    public static enum QueuePriority {
        HIGH,
        NORM,
        LOW;

    }

    protected boolean                                                   debugFlag           = false;

    protected final java.util.List<QueueAction<?, ? extends Throwable>> queueThreadHistory  = new ArrayList<QueueAction<?, ? extends Throwable>>(20);
    protected final AtomicReference<QueueThread>                        thread              = new AtomicReference<QueueThread>(null);
    private volatile QueueAction<?, ? extends Throwable>                sourceItem          = null;
    private volatile QueueAction<?, ?>                                  currentJob;

    protected final AtomicLong                                          addStats            = new AtomicLong(0);
    protected final AtomicLong                                          addWaitStats        = new AtomicLong(0);
    protected final AtomicLong                                          addRunStats         = new AtomicLong(0);

    protected static AtomicInteger                                      QUEUELOOPPREVENTION = new AtomicInteger(0);
    private final String                                                id;
    protected volatile long                                             timeout             = 10 * 1000l;

    private final ArrayDeque<?>[]                                       queues;

    public Queue(final String id) {
        this.id = id;
        Queue.QUEUELOOPPREVENTION.incrementAndGet();
        queues = new ArrayDeque<?>[QueuePriority.values().length];
        for (int i = 0; i < queues.length; i++) {
            queues[i] = new ArrayDeque<QueueAction<?, ? extends Throwable>>();
        }
    }

    /**
     * This method adds an action to the queue. if the caller is a queueaction itself, the action will be executed directly. In this case,
     * this method can throw Exceptions. If the caller is not the QUeuethread, this method is not able to throw exceptions, but the
     * exceptions are passed to the exeptionhandler method of the queueaction
     *
     * @param <T>
     * @param <E>
     * @param item
     * @throws T
     */
    public <E, T extends Throwable> void add(final QueueAction<?, T> action) throws T {
        /* set calling Thread to current item */
        action.reset();
        action.setCallerThread(this, Thread.currentThread());
        if (this.isQueueThread(action)) {
            /*
             * call comes from current running item, so lets start item
             */
            final QueueAction<?, ? extends Throwable> source = ((QueueThread) Thread.currentThread()).getSourceQueueAction();
            if (source != null) {
                /* forward source priority */
                action.setQueuePrio(source.getQueuePrio());
            }
            this.addRunStats.incrementAndGet();
            this.startItem(action, false);
        } else {
            this.addStats.incrementAndGet();
            /* call does not come from current running item, so lets queue it */
            this.internalAdd(action);
        }
    }

    /**
     * Only use this method if you can asure that the caller is NEVER the queue itself. if you are not sure use #add
     *
     * @param <E>
     * @param <T>
     * @param action
     * @throws T
     */
    public <E, T extends Throwable> void addAsynch(final QueueAction<?, T> action) {
        /* set calling Thread to current item */
        if (action.allowAsync() == false && this.isQueueThread(action)) {
            throw new RuntimeException("called addAsynch from the queue itself");
        } else {
            this.addStats.incrementAndGet();
            action.reset();
            action.setCallerThread(this, Thread.currentThread());
            this.internalAdd(action);
        }
    }

    @SuppressWarnings("unchecked")
    public <E, T extends Throwable> E addWait(final QueueAction<E, T> item) throws T {
        /* set calling Thread to current item */
        item.reset();
        item.setCallerThread(this, Thread.currentThread());
        if (this.isQueueThread(item)) {
            /*
             * call comes from current running item, so lets start item excaption handling is passed to top item. startItem throws an
             * exception in error case
             */
            final QueueAction<?, ? extends Throwable> source = ((QueueThread) Thread.currentThread()).getSourceQueueAction();
            if (source != null) {
                /* forward source priority */
                item.setQueuePrio(source.getQueuePrio());
            }
            this.addRunStats.incrementAndGet();
            this.startItem(item, false);
        } else {
            this.addWaitStats.incrementAndGet();
            /* call does not come from current running item, so lets queue it */
            this.internalAdd(item);
            /* wait till item is finished */
            try {
                while (!item.isFinished()) {
                    synchronized (item) {
                        if (!item.isFinished()) {
                            item.wait(1000);
                        }
                    }
                }
            } catch (final InterruptedException e) {
                item.handleException(e);
            }
            if (item.getExeption() != null) {
                // throw exception if item canot handle the exception itself
                if (!item.callExceptionHandler()) {
                    if (item.getExeption() instanceof RuntimeException) {
                        throw (RuntimeException) item.getExeption();
                    } else {
                        throw (T) item.getExeption();
                    }
                }
            }
            if (item.gotKilled() && !item.gotStarted()) {
                item.handleException(new InterruptedException("Queue got killed!"));
            }
        }
        return item.getResult();
    }

    public void enqueue(final QueueAction<?, ?> action) {
        /* set calling Thread to current item */
        action.reset();
        action.setCallerThread(this, Thread.currentThread());
        this.internalAdd(action);
    }

    protected QueueAction<?, ?> getCurrentJob() {
        return this.currentJob;
    }

    public List<QueueAction<?, ?>> getEntries() {
        final List<QueueAction<?, ?>> ret = new ArrayList<QueueAction<?, ?>>();
        synchronized (this.queueLock) {
            final QueueAction<?, ?> lcurrentJob = currentJob;
            if (lcurrentJob != null) {
                ret.add(currentJob);
            }
            for (int i = 0; i < queues.length; i++) {
                final ArrayDeque<?> queue = queues[i];
                ret.addAll((ArrayDeque<QueueAction<?, ?>>) queue);
            }
        }
        return ret;
    }

    public String getID() {
        return this.id;
    }

    protected QueueAction<?, ? extends Throwable> getLastHistoryItem() {
        synchronized (this.queueThreadHistory) {
            if (this.queueThreadHistory.size() == 0) {
                return null;
            }
            return this.queueThreadHistory.get(this.queueThreadHistory.size() - 1);
        }
    }

    public QueueThread getQueueThread() {
        return this.thread.get();
    }

    protected QueueAction<?, ? extends Throwable> getSourceQueueAction() {
        return this.sourceItem;
    }

    public long getTimeout() {
        return this.timeout;
    }

    /**
     * Overwrite this to hook before a action execution
     */
    protected void handlePreRun() {
        // TODO Auto-generated method stub

    }

    public void internalAdd(final QueueAction<?, ?> action) {
        if (action != null) {
            synchronized (this.queueLock) {
                try {
                    final QueuePriority prio = action.getQueuePrio();
                    if (prio != null) {
                        ((ArrayDeque<QueueAction<?, ? extends Throwable>>) queues[prio.ordinal()]).offer(action);
                    } else {
                        ((ArrayDeque<QueueAction<?, ? extends Throwable>>) queues[QueuePriority.NORM.ordinal()]).offer(action);
                    }
                } finally {
                    try {
                        final Thread currentThread = this.thread.get();
                        if (currentThread == null || !currentThread.isAlive()) {
                            final QueueThread newThread = new QueueThread(this);
                            this.thread.set(newThread);
                            newThread.start();
                        }
                    } finally {
                        queueLock.notifyAll();
                    }
                }
            }
        }
    }

    /**
     * returns true if this queue shows debug info
     *
     * @return
     */
    public boolean isDebug() {
        return this.debugFlag;
    }

    public boolean isEmpty() {
        synchronized (this.queueLock) {
            for (int i = 0; i < queues.length; i++) {
                final ArrayDeque<?> queue = queues[i];
                if (!queue.isEmpty()) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * this functions returns true if the current running Thread is our QueueThread OR the SourceQueueItem chain is rooted in current
     * running QueueItem
     */
    public boolean isQueueThread(final QueueAction<?, ? extends Throwable> item) {
        if (Thread.currentThread() == this.thread.get()) {
            return true;
        }
        QueueAction<?, ? extends Throwable> last = item;
        Thread t = null;
        /*
         * we walk through actionHistory to check if we are still in our QueueThread
         */
        int loopprevention = 0;
        while (last != null && (t = last.getCallerThread()) != null) {
            if (t != null && t instanceof QueueThread) {
                if (t == this.getQueueThread()) {
                    if (this.debugFlag) {
                        org.appwork.utils.logging.Log.L.warning("Multiple queues detected-> external synchronization may be required! " + item);
                    }
                    return true;
                }
                last = ((QueueThread) t).getLastHistoryItem();
                if (loopprevention > Queue.QUEUELOOPPREVENTION.get()) {
                    /*
                     * loop prevention: while can only loop max QUEUELOOPPREVENTION times, cause no more different queues exist
                     */
                    if (this.debugFlag) {
                        org.appwork.utils.logging.Log.L.warning("QueueLoopPrevention!");
                    }
                    break;
                }
                loopprevention++;
            } else {
                break;
            }
        }
        return false;
    }

    /**
     * Does NOT kill the currently running job
     *
     */
    public void killQueue() {
        final List<QueueAction<?, ? extends Throwable>> killList = new ArrayList<QueueAction<?, ? extends Throwable>>();
        synchronized (this.queueLock) {
            System.out.println("Kill: " + this);
            for (final ArrayDeque<?> queue : queues) {
                killList.addAll((ArrayDeque<QueueAction<?, ? extends Throwable>>) queue);
                queue.clear();
            }

        }
        for (final QueueAction<?, ? extends Throwable> item : killList) {
            item.kill();
        }
    }

    /**
     * @param item
     */
    protected void onItemHandled(final QueueAction<?, ? extends Throwable> item) {
        // TODO Auto-generated method stub

    }

    public boolean remove(final QueueAction<?, ?> action) {
        QueueAction<?, ?> kill = null;
        synchronized (this.queueLock) {
            final QueuePriority prio = action.getQueuePrio();
            if (prio != null && queues[prio.ordinal()].remove(action)) {
                kill = action;
            }
            if (kill == null) {
                for (int i = 0; i < queues.length; i++) {
                    final ArrayDeque<?> queue = queues[i];
                    if (queue.remove(action)) {
                        kill = action;
                        break;
                    }
                }
            }
        }
        if (kill != null) {
            kill.kill();
            return true;
        }
        return false;
    }

    private QueueAction<?, ? extends Throwable> poll() {
        for (int i = 0; i < queues.length; i++) {
            final ArrayDeque<?> queue = queues[i];
            final QueueAction<?, ? extends Throwable> ret = (QueueAction<?, ? extends Throwable>) queue.poll();
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    protected void runQueue() {
        try {
            QueueAction<?, ? extends Throwable> item = null;
            while (true) {
                try {
                    this.handlePreRun();
                    synchronized (this.queueLock) {
                        item = poll();
                        if (item == null) {
                            this.queueLock.wait(this.getTimeout());
                            item = poll();
                            if (item == null) {
                                final Thread thread = Thread.currentThread();
                                if (thread instanceof QueueThread) {
                                    this.thread.compareAndSet((QueueThread) thread, null);
                                }
                                return;
                            }
                        }
                    }
                    if (!handleItem(item)) {
                        continue;
                    }
                    try {
                        this.sourceItem = item;
                        this.startItem(item, true);
                    } catch (final Throwable e) {
                    } finally {
                        this.sourceItem = null;
                        this.onItemHandled(item);
                    }
                } catch (final Throwable e) {
                    Log.L.info("Queue rescued!");
                    Log.exception(e);
                }
            }
        } finally {
            synchronized (this.queueLock) {
                final Thread thread = Thread.currentThread();
                if (thread instanceof QueueThread) {
                    this.thread.compareAndSet((QueueThread) thread, null);
                }
            }
        }
    }

    /**
     * @param item
     * @return
     */
    protected boolean handleItem(QueueAction<?, ? extends Throwable> item) {
        return true;
    }

    /**
     * changes this queue's debugFlag
     *
     * @param b
     */
    public void setDebug(final boolean b) {
        this.debugFlag = b;
    }

    public void setTimeout(long timeout) {
        this.timeout = Math.max(0, timeout);
        synchronized (this.queueLock) {
            this.queueLock.notifyAll();
        }
    }

    public int size() {
        synchronized (this.queueLock) {
            int ret = 0;
            for (int i = 0; i < queues.length; i++) {
                final ArrayDeque<?> queue = queues[i];
                ret += queue.size();
            }
            return ret;
        }
    }

    /* if you override this, DON'T forget to notify item when its done! */
    @SuppressWarnings("unchecked")
    protected <T extends Throwable> void startItem(final QueueAction<?, T> item, final boolean callExceptionhandler) throws T {
        try {
            this.currentJob = item;
            if (this.getQueueThread() != item.getCallerThread()) {
                synchronized (this.queueThreadHistory) {
                    this.queueThreadHistory.add(item);
                }
            }
            item.start(this);
        } catch (final Throwable e) {
            if (!callExceptionhandler || !item.callExceptionHandler()) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw (T) e;
                }
            }
        } finally {
            if (this.getQueueThread() != item.getCallerThread()) {
                synchronized (this.queueThreadHistory) {
                    if (this.queueThreadHistory.size() != 0) {
                        this.queueThreadHistory.remove(this.queueThreadHistory.size() - 1);
                    }
                }
            }
            item.setFinished(true);
            this.currentJob = null;

        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Thread#toString()
     */
    @Override
    public String toString() {
        return this.id + ": add=" + this.addStats.get() + " addWait=" + this.addWaitStats.get() + " addRun=" + this.addRunStats.get();
    }
}
