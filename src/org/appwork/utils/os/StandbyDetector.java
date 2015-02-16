/**
 * Copyright (c) 2009 - 2015 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 *
 * This file is part of org.appwork.utils.os
 *
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.os;

import java.util.EventListener;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.utils.event.DefaultEvent;
import org.appwork.utils.event.Eventsender;

/**
 * @author daniel
 *
 */
public class StandbyDetector {

    private class StandbyDetectedEvent extends DefaultEvent {
        private final long possibleStandByTime;

        public final long getPossibleStandByTime() {
            return this.possibleStandByTime;
        }

        public StandbyDetectedEvent(Object caller, long possibleStandByTime) {
            super(caller);
            this.possibleStandByTime = possibleStandByTime;
        }
    }

    public interface StandbyDetectorListener extends EventListener {
        public void onStandbyDetected(long possibleStandByTime);
    }

    private Eventsender<StandbyDetectorListener, StandbyDetectedEvent> eventSender = new Eventsender<StandbyDetector.StandbyDetectorListener, StandbyDetector.StandbyDetectedEvent>() {

                                                                                       @Override
                                                                                       protected void fireEvent(StandbyDetectorListener listener, StandbyDetectedEvent event) {
                                                                                           if (StandbyDetector.this.isStandByDetectorEnabled()) {
                                                                                               listener.onStandbyDetected(event.getPossibleStandByTime());
                                                                                           }
                                                                                       }
                                                                                   };

    public final Eventsender<StandbyDetectorListener, StandbyDetectedEvent> getEventSender() {
        return this.eventSender;
    }

    private final AtomicReference<Thread> standbyDetectorThread = new AtomicReference<Thread>(null);
    private final long                    standbyDetectionTimeout;

    public final long getStandbyDetectionTimeout() {
        return this.standbyDetectionTimeout;
    }

    public StandbyDetector(final long standbyDetectionTimeout) {
        this.standbyDetectionTimeout = Math.max(10 * 1000, standbyDetectionTimeout);
    }

    private final boolean isStandByDetectorEnabled() {
        return Thread.currentThread() == StandbyDetector.this.standbyDetectorThread.get();
    }

    public synchronized void start() {
        Thread thread = this.standbyDetectorThread.get();
        if (thread == null || !thread.isAlive()) {
            thread = new Thread("StandbyDetector") {

                @Override
                public void run() {
                    try {
                        while (StandbyDetector.this.isStandByDetectorEnabled()) {
                            final long lastTimeStamp = System.currentTimeMillis();
                            try {
                                Thread.sleep(StandbyDetector.this.getStandbyDetectionTimeout());
                            } catch (final InterruptedException e) {
                                break;
                            }
                            final long timeStampGap = System.currentTimeMillis() - lastTimeStamp;
                            if (timeStampGap > StandbyDetector.this.getStandbyDetectionTimeout()) {
                                try {
                                    StandbyDetector.this.eventSender.fireEvent(new StandbyDetectedEvent(StandbyDetector.this, timeStampGap));
                                } catch (final Throwable e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } finally {
                        StandbyDetector.this.standbyDetectorThread.compareAndSet(Thread.currentThread(), null);
                    }
                }
            };
            thread.setDaemon(true);
            this.standbyDetectorThread.set(thread);
            thread.start();
        }
    }

    public final synchronized boolean isRunning() {
        final Thread thread = this.standbyDetectorThread.get();
        return thread != null && thread.isAlive();
    }

    public final synchronized void stop() {
        this.standbyDetectorThread.getAndSet(null);
    }
}
