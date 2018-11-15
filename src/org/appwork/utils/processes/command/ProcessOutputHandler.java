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
 *     The intent is that the AppWork GmbH is able to provide  their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 *     These terms apply to all files that have the [The Product] License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 *
 * === 3rd Party Licences ===
 *     Some parts of the [The Product] use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 *     to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header.
 *
 * === Definition: Commercial Usage ===
 *     If anybody or any organization is generating income (directly or indirectly) by using [The Product] or if there's any commercial interest or aspect in what you are doing, we consider this as a commercial usage.
 *     If your use-case is neither strictly private nor strictly educational, it is commercial. If you are unsure whether your use-case is commercial or not, consider it as commercial or contact as.
 * === Dual Licensing ===
 * === Commercial Usage ===
 *     If you want to use [The Product] in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 *     Contact AppWork for further details: e-mail@appwork.org
 * === Non-Commercial Usage ===
 *     If there is no commercial usage (see definition above), you may use [The Product] under the terms of the
 *     "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 *
 *     If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.utils.processes.command;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.appwork.loggingv3.LogV3;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.processes.ProcessOutput;

/**
 * @author Thomas
 * @date 08.11.2018
 *
 */
public class ProcessOutputHandler implements OutputHandler {
    private LogInterface          logger;
    private ByteArrayOutputStream baoErr;
    private ByteArrayOutputStream baoStd;
    private Charset               charset;
    private int                   exitCode;

    /**
     *
     */
    public ProcessOutputHandler() {
        logger = LogV3.defaultLogger();
    }

    public class ReaderThread extends Thread implements AsyncInputStreamHandler {
        private final InputStream     is;
        private volatile boolean      processIsDead;
        private ByteArrayOutputStream output;

        /**
         * @param charset
         * @param lh
         * @param inputStream
         * @param output
         * @throws UnsupportedEncodingException
         * @throws InterruptedException
         */
        public ReaderThread(final OutputHandler lineHandler, Charset charset, final InputStream inputStream, ByteArrayOutputStream output) throws UnsupportedEncodingException, InterruptedException {
            super(inputStream.getClass().getSimpleName());
            setDaemon(true);
            this.is = inputStream;
            this.output = output;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            try {
                final byte[] buffer = new byte[1024];
                int len = 0;
                boolean wait = false;
                while (true) {
                    if (processIsDead && is.available() == 0) {
                        return;
                    }
                    len = is.read(buffer);
                    if (processIsDead && len == 0) {
                        // according to
                        // https://stackoverflow.com/questions/2319395/what-0-returned-by-inputstream-read-means-how-to-handle-this, this
                        // method MAY return 0 if nothing is read.
                        // so this is a workaround for bad inputstream implementations that might result in endless blocking readers
                        return;
                    }
                    if (len == -1) {
                        break;
                    } else if (len > 0) {
                        wait = false;
                        output.write(buffer, 0, len);
                    } else {
                        if (wait == false) {
                            wait = true;
                        }
                        Thread.sleep(10);
                    }
                }
            } catch (InterruptedException e) {
                return;
            } catch (final IOException e) {
                if (!processIsDead) {
                    logger.log(e);
                }
            }
            final byte[] buf = new byte[8192];
            while (true) {
                try {
                    final int read = is.read(buf);
                    if (read <= 0) {
                        if (processIsDead) {
                            return;
                        } else {
                            Thread.sleep(50);
                        }
                    }
                } catch (IOException e) {
                    if (!processIsDead) {
                        logger.log(e);
                    }
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

        @Override
        public void interrupt() {
            try {
                is.close();
            } catch (IOException e) {
                logger.exception("Swallowed Exception closeing Command Reader", e);
            } finally {
                super.interrupt();
            }
        }

        /**
         * @throws InterruptedException
         * @throws IOException
         *
         */
        public void waitFor() throws InterruptedException {
            processIsDead = true;
            try {
                while (isAlive() && is.available() > 0) {
                    Thread.sleep(50);
                }
                interrupt();
            } catch (IOException e) {
                logger.exception("Swallowed Exception closeing Command Reader", e);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.utils.processes.command.OutputHandler#createAsyncStreamHandler(java.lang.String, java.io.InputStream)
     */
    @Override
    public AsyncInputStreamHandler createAsyncStreamHandler(CommandErrInputStream inputStream, Charset charset) throws UnsupportedEncodingException, InterruptedException {
        this.baoErr = new ByteArrayOutputStream();
        this.charset = charset;
        return new ReaderThread(this, charset, inputStream, baoErr);
    }

    @Override
    public AsyncInputStreamHandler createAsyncStreamHandler(CommandStdInputStream inputStream, Charset charset) throws UnsupportedEncodingException, InterruptedException {
        this.baoStd = new ByteArrayOutputStream();
        this.charset = charset;
        return new ReaderThread(this, charset, inputStream, baoStd);
    }

    /**
     * @return
     */
    public ProcessOutput getResult() {
        return new ProcessOutput(exitCode, baoStd.toByteArray(), baoErr.toByteArray(), charset.name());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.utils.processes.command.OutputHandler#onExitCode(int)
     */
    @Override
    public void onExitCode(int exitCode) {
        this.exitCode = exitCode;
    }
}
