/**
 * 
 * ====================================================================================================================================================
 * "AppWork Utilities" License
 * ====================================================================================================================================================
 * Copyright (c) 2009-2015, AppWork GmbH <e-mail@appwork.org>
 * Schwabacher Straße 117
 * 90763 Fürth
 * Germany
 * 
 * === Preamble ===
 * This license establishes the terms under which the AppWork Utilities Source Code & Binary files may be used, copied, modified, distributed, and/or redistributed.
 * The intent is that the AppWork GmbH is able to provide  their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 * These terms apply to all files that have the "AppWork Utilities" License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 * 
 * === 3rd Party Licences ===
 * Some parts of the AppWork Utilities use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 * to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header.
 * 
 * === Definition: Commercial Usage ===
 * If anybody or any organization is generating income (directly or indirectly) by using "AppWork Utilities" or if there's as much as a
 * sniff of commercial interest or aspect in what you are doing, we consider this as a commercial usage. If you are unsure whether your use-case is commercial or not, consider it as commercial.
 * === Dual Licensing ===
 * === Commercial Usage ===
 * If you want to use AppWork Utilities in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 * Contact AppWork for further details: e-mail@appwork.org
 * === Non-Commercial Usage ===
 * If there is no commercial usage (see definition above), you may use AppWork Utilities under the terms of the
 * "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 * 
 * If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
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
