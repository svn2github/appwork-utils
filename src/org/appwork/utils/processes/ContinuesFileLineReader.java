package org.appwork.utils.processes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.appwork.utils.logging2.extmanager.LoggerFactory;

/**
 * Read a File line by line and post it to the sink after a line is fully read. If EOF is reached, the reader waits for new content until
 * {@link #closeAndFlush()} is called
 *
 * @author Thomas
 * @date 17.10.2018
 *
 */
public class ContinuesFileLineReader {
    private File              file;
    private volatile boolean  closed = false;
    private Thread            thread;
    protected FileInputStream br;
    private LineHandler       sink;

    protected void setSink(LineHandler sink) {
        this.sink = sink;
    }

    public ContinuesFileLineReader(LineHandler sink, String path) {
        this.file = new File(path);
        this.sink = sink;
    }

    public ContinuesFileLineReader(String path) {
        this.file = new File(path);
    }

    private volatile IOException exceptionIOException;

    public synchronized ContinuesFileLineReader run() {
        if (sink == null) {
            throw new IllegalStateException("Sink missing");
        }
        this.thread = new Thread("Read " + this.file) {
            private Charset charSet = Charset.forName("UTF-8");

            @Override
            public void run() {
                // Wait until the logfile is available
                while (!ContinuesFileLineReader.this.file.exists()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                try {
                    byte[] cbuf = new byte[1024];
                    ContinuesFileLineReader.this.br = new FileInputStream(ContinuesFileLineReader.this.file);
                    ByteArrayOutputStream bao = new ByteArrayOutputStream();
                    byte last = 0;
                    while (true) {
                        int length = br.read(cbuf);
                        if (length == -1) {
                            if (closed) {
                                if (bao.size() > 0) {
                                    // no empty newline at the end
                                    sink.handleLine(new String(bao.toByteArray(), charSet), this);
                                    bao.reset();
                                }
                                return;
                            } else {
                                Thread.sleep(50);
                            }
                        } else {
                            byte c;
                            for (int i = 0; i < length; i++) {
                                c = cbuf[i];
                                try {
                                    if (c == '\r') {
                                        // \r newline
                                    } else if (c == '\n') {
                                        if (last == '\r') {
                                            // \r\n we already sent the new line after the \r
                                            continue;
                                        } else {
                                            // \r new line
                                        }
                                    } else {
                                        bao.write(c);
                                        continue;
                                    }
                                } finally {
                                    last = c;
                                }
                                sink.handleLine(new String(bao.toByteArray(), charSet), this);
                                bao.reset();
                                last = c;
                            }
                        }
                    }
                } catch (IOException e) {
                    exceptionIOException = e;
                } catch (InterruptedException e) {
                    // we leave the thread, no reason to reset the interrupt flag
                    return;
                } finally {
                    if (ContinuesFileLineReader.this.br != null) {
                        try {
                            ContinuesFileLineReader.this.br.close();
                        } catch (IOException e) {
                            LoggerFactory.getDefaultLogger().log(e);
                        }
                    }
                }
            }
        };
        this.thread.start();
        return this;
    }

    public void closeAndFlush() throws InterruptedException, IOException {
        try {
            this.closed = true;
            this.thread.join();
            if (exceptionIOException != null) {
                throw exceptionIOException;
            }
        } catch (InterruptedException e) {
            this.thread.interrupt();
            throw e;
        }
    }
}
