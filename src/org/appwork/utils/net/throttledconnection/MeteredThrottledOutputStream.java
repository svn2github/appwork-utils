/**
 *
 * ====================================================================================================================================================
 *         "AppWork Utilities" License
 *         The "AppWork Utilities" will be called [The Product] from now on.
 * ====================================================================================================================================================
 *         Copyright (c) 2009-2015, AppWork GmbH <e-mail@appwork.org>
 *         Schwabacher Straße 117
 *         90763 Fürth
 *         Germany
 * === Preamble ===
 *     This license establishes the terms under which the [The Product] Source Code & Binary files may be used, copied, modified, distributed, and/or redistributed.
 *     The intent is that the AppWork GmbH is able to provide their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 *     These terms apply to all files that have the [The Product] License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 *
 * === 3rd Party Licences ===
 *     Some parts of the [The Product] use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 *     to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header.
 *
 * === Definition: Commercial Usage ===
 *     If anybody or any organization is generating income (directly or indirectly) by using [The Product] or if there's any commercial interest or aspect in what you are doing, we consider this as a commercial usage.
 *     If your use-case is neither strictly private nor strictly educational, it is commercial. If you are unsure whether your use-case is commercial or not, consider it as commercial or contact us.
 * === Dual Licensing ===
 * === Commercial Usage ===
 *     If you want to use [The Product] in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 *     Contact AppWork for further details: <e-mail@appwork.org>
 * === Non-Commercial Usage ===
 *     If there is no commercial usage (see definition above), you may use [The Product] under the terms of the
 *     "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 *
 *     If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.utils.net.throttledconnection;

import java.io.OutputStream;

import org.appwork.utils.speedmeter.SpeedMeterInterface;

/**
 * @author daniel
 *
 */
public class MeteredThrottledOutputStream extends ThrottledOutputStream implements SpeedMeterInterface {
    private SpeedMeterInterface speedmeter = null;
    private long                time       = 0;
    private long                speed      = 0;
    private long                lastTime;
    private long                lastTrans;
    private long                transferedCounter3;
    private final Object        LOCK       = new Object();

    /**
     * @param out
     */
    public MeteredThrottledOutputStream(OutputStream out) {
        super(out);
    }

    public MeteredThrottledOutputStream(OutputStream out, SpeedMeterInterface speedmeter) {
        super(out);
        this.speedmeter = speedmeter;
    }

    protected long getTime() {
        if (speedmeter != null) {
            return speedmeter.getResolution().getTime();
        }
        return getTime();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.utils.speedmeter.SpeedMeterInterface#getResolution()
     */
    @Override
    public Resolution getResolution() {
        if (speedmeter != null) {
            return speedmeter.getResolution();
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.utils.SpeedMeterInterface#getSpeedMeter()
     */
    public long getValue(Resolution resolution) {
        synchronized (LOCK) {
            if (time == 0) {
                time = getTime();
                transferedCounter3 = transferedCounter;
                return 0;
            }
            if (getTime() - time < 1000) {
                if (speedmeter != null) {
                    return speedmeter.getValue(resolution);
                }
                return speed;
            }
            lastTime = getTime() - time;
            time = getTime();
            lastTrans = transferedCounter - transferedCounter3;
            transferedCounter3 = transferedCounter;
            if (speedmeter != null) {
                speedmeter.putBytes(lastTrans, lastTime);
                speed = speedmeter.getValue(resolution);
                return speed;
            } else {
                speed = (lastTrans / lastTime) * 1000;
                return speed;
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.utils.SpeedMeterInterface#putSpeedMeter(long, long)
     */
    public void putBytes(long bytes, long time) {
    }

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.utils.SpeedMeterInterface#resetSpeedMeter()
     */
    public void resetSpeedmeter() {
        synchronized (LOCK) {
            if (speedmeter != null) {
                speedmeter.resetSpeedmeter();
            }
            time = getTime();
            speed = 0;
            transferedCounter3 = transferedCounter;
        }
    }
}
