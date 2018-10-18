package org.appwork.utils.processes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;

import org.appwork.utils.net.LineParsingInputStream;
import org.appwork.utils.net.LineParsingOutputStream.NEWLINE;

/**
 * Read a File line by line and post it to the sink after a line is fully read. If EOF is reached, the reader waits for new content until
 * {@link #closeAndFlush()} is called
 *
 * @author Thomas
 * @date 17.10.2018
 *
 */
public class ContinuesFileLineReader {
    protected final File          file;
    protected final AtomicBoolean closedFlag = new AtomicBoolean(false);
    protected Thread              thread     = null;
    protected final LineHandler   sink;
    protected final Charset       charset;

    public ContinuesFileLineReader(LineHandler sink, String path, Charset charset) {
        if (sink == null) {
            throw new IllegalArgumentException("Sink missing");
        }
        this.file = new File(path);
        this.sink = sink;
        if (charset == null) {
            this.charset = Charset.forName("UTF-8");
        } else {
            this.charset = charset;
        }
    }

    private volatile IOException exceptionIOException;

    public synchronized ContinuesFileLineReader run() {
        if (thread != null && thread.isAlive()) {
            return this;
        }
        closedFlag.compareAndSet(true, false);
        exceptionIOException = null;
        this.thread = new Thread("Read " + this.file) {
            @Override
            public void run() {
                // Wait until the logfile is available
                while (!ContinuesFileLineReader.this.file.exists() && !isClosed()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        // we leave the thread, no reason to reset the interrupt flag
                        return;
                    }
                }
                if (!isClosed()) {
                    try {
                        final FileInputStream fis = new FileInputStream(file);
                        try {
                            final LineParsingInputStream is = new LineParsingInputStream(fis, charset) {
                                @Override
                                protected void onNextLine(NEWLINE newLine, long line, StringBuilder sb, int startIndex, int endIndex) {
                                    ContinuesFileLineReader.this.onNextLine(newLine, line, sb, startIndex, endIndex);
                                };
                            };
                            try {
                                final byte[] buf = new byte[32767];
                                while (!isClosed()) {
                                    final int read = is.read(buf);
                                    if (read <= 0) {
                                        if (isClosed()) {
                                            break;
                                        } else {
                                            onWait(read, is.getLines());
                                        }
                                    }
                                }
                            } finally {
                                is.close();
                            }
                        } finally {
                            fis.close();
                        }
                    } catch (IOException e) {
                        exceptionIOException = e;
                    } catch (InterruptedException e) {
                        // we leave the thread, no reason to reset the interrupt flag
                        return;
                    }
                }
            }
        };
        this.thread.start();
        return this;
    }

    public boolean isClosed() {
        return closedFlag.get();
    }

    public void closeAndFlush() throws InterruptedException, IOException {
        try {
            if (closedFlag.compareAndSet(false, true)) {
                this.thread.join();
                if (exceptionIOException != null) {
                    throw exceptionIOException;
                }
            }
        } catch (InterruptedException e) {
            this.thread.interrupt();
            throw e;
        }
    }

    protected void onWait(int read, long lines) throws InterruptedException {
        Thread.sleep(50);
    }

    protected void onNextLine(NEWLINE newLine, long line, StringBuilder sb, int startIndex, int endIndex) {
        sink.handleLine(sb.substring(startIndex, endIndex), this);
    }
}
