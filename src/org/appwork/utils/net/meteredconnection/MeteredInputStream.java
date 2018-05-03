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
package org.appwork.utils.net.meteredconnection;

import java.io.IOException;
import java.io.InputStream;

import org.appwork.utils.speedmeter.SpeedMeterInterface;

/**
 * @author daniel
 * 
 */
public class MeteredInputStream extends InputStream implements SpeedMeterInterface {
    private InputStream         in;
    private SpeedMeterInterface speedmeter = null;
    private long                transfered = 0;

    public long getTransfered() {
        return transfered;
    }

    private long            transfered2      = 0;
    private long            time             = 0;
    private int             readTmp1;
    private long            speed            = 0;
    private int             offset;
    private int             checkStep        = 1024;
    // private final static int HIGHStep = 524288;
    public final static int LOWStep          = 1024;
    private int             todo;
    private int             lastRead;
    private int             rest;
    private int             lastRead2;
    private long            lastTime;
    private long            lastTrans;
    private long            timeForCheckStep = 0;
    private int             timeCheck        = 0;

    /**
     * constructor for MeterdInputStream
     * 
     * @param in
     */
    public MeteredInputStream(InputStream in) {
        this.in = in;
    }

    /**
     * constructor for MeteredInputStream with custom SpeedMeter
     * 
     * @param in
     * @param speedmeter
     */
    public MeteredInputStream(InputStream in, SpeedMeterInterface speedmeter) {
        this.in = in;
        this.speedmeter = speedmeter;
    }

    @Override
    public int read() throws IOException {
        readTmp1 = in.read();
        if (readTmp1 != -1) {
            transfered++;
        }
        return readTmp1;
    }

    public int getCheckStepSize() {
        return checkStep;
    }

    public void setCheckStepSize(int step) {
        checkStep = Math.min(LOWStep, checkStep);
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        offset = off;
        rest = len;
        lastRead2 = 0;
        while (rest != 0) {
            todo = rest;
            if (todo > checkStep) {
                todo = checkStep;
            }
            timeForCheckStep = System.currentTimeMillis();
            lastRead = in.read(b, offset, todo);
            timeCheck = (int) (System.currentTimeMillis() - timeForCheckStep);
            if (lastRead == -1) {
                break;
            }
            if (timeCheck > 1000) {
                /* we want 5 update per second */
                checkStep = Math.max(LOWStep, (todo / timeCheck) * 500);
            } else if (timeCheck == 0) {
                /* we increase in little steps */
                checkStep += 1024;
                // checkStep = Math.min(HIGHStep, checkStep + 1024);
            }
            lastRead2 += lastRead;
            transfered += lastRead;
            rest -= lastRead;
            offset += lastRead;
        }
        if (lastRead == -1 && lastRead2 == 0) {
            return -1;
        } else {
            return lastRead2;
        }
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public synchronized void mark(int readlimit) {
        in.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        in.reset();
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }

    @Override
    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.utils.SpeedMeterInterface#getSpeedMeter()
     */
    public synchronized long getValue(final long scalingFactor) {
        if (time == 0) {
            time = System.currentTimeMillis();
            transfered2 = transfered;
            return 0;
        }
        if (System.currentTimeMillis() - time < 1000) {
            if (speedmeter != null) {
                return speedmeter.getValue(scalingFactor);
            }
            return speed;
        }
        lastTime = System.currentTimeMillis() - time;
        time = System.currentTimeMillis();
        lastTrans = transfered - transfered2;
        transfered2 = transfered;
        if (speedmeter != null) {
            speedmeter.putSpeedMeter(lastTrans, lastTime);
            speed = speedmeter.getValue(scalingFactor);
            return speed;
        } else {
            speed = (lastTrans / lastTime) * 1000;
            return speed;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.utils.SpeedMeterInterface#putSpeedMeter(long, long)
     */
    public void putSpeedMeter(long bytes, long time) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.utils.SpeedMeterInterface#resetSpeedMeter()
     */
    public synchronized void resetSpeedMeter() {
        if (speedmeter != null) {
            speedmeter.resetSpeedMeter();
        }
        speed = 0;
        transfered2 = transfered;
        time = System.currentTimeMillis();
    }
}
