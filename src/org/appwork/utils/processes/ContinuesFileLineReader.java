package org.appwork.utils.processes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.appwork.utils.logging2.extmanager.LoggerFactory;

/**
 * Read a File line by line and post it to the sink after a line is fully read. If EOF is reached, the reader waits for new content until
 * {@link #waitFor()} is called
 *
 * @author Thomas
 * @date 17.10.2018
 *
 */
public class ContinuesFileLineReader {
    private File             file;
    private volatile boolean closed = false;
    private Thread           thread;
    protected BufferedReader br;
    private LineHandler      sink;

    public ContinuesFileLineReader(LineHandler sink, String path) {
        this.file = new File(path);
        this.sink = sink;
    }

    private volatile IOException exceptionIOException;

    public synchronized ContinuesFileLineReader run() {
        this.thread = new Thread("Read " + this.file) {
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
                    char[] cbuf = new char[1024];
                    ContinuesFileLineReader.this.br = new BufferedReader(new InputStreamReader(new FileInputStream(ContinuesFileLineReader.this.file), "UTF-8"));
                    StringBuilder l = new StringBuilder();
                    boolean newline = false;
                    while (true) {
                        int read;
                        if (ContinuesFileLineReader.this.closed) {
                            read = ContinuesFileLineReader.this.br.read(cbuf);
                            if (read < 0) {
                                if (l.length() > 0) {
                                    ContinuesFileLineReader.this.sink.handleLine(l.toString(), ContinuesFileLineReader.this);
                                }
                                return;
                            }
                        } else {
                            read = ContinuesFileLineReader.this.br.read(cbuf);
                            if (read < 0) {
                                // end of file reached. wait and retry.
                                Thread.sleep(50);
                                continue;
                            }
                        }
                        if (Thread.currentThread().isInterrupted()) {
                            return;
                        }
                        for (int i = 0; i < Math.min(read, cbuf.length); i++) {
                            char c = cbuf[i];
                            if (c == '\r') {
                                newline = true;
                                continue;
                            }
                            if (c == '\n') {
                                newline = true;
                                continue;
                            }
                            if (newline) {
                                ContinuesFileLineReader.this.sink.handleLine(l.toString(), ContinuesFileLineReader.this);
                                l.setLength(0);
                                newline = false;
                            }
                            l.append(c);
                        }
                        if (newline) {
                            ContinuesFileLineReader.this.sink.handleLine(l.toString(), ContinuesFileLineReader.this);
                            l.setLength(0);
                            newline = false;
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

    // protected void handleLine(StringBuilder l) {
    // protected void handleLine(StringBuilder l) {
    // if (l.length() > 0) {
    // String line = l.toString();
    // if (StringUtils.isNotEmpty(line)) {
    // AbstractLogFileReader.this.success.compareAndSet(false, AbstractLogFileReader.this.successPattern.matcher(line).find());
    // AbstractLogFileReader.this.client.getLogger().info(AbstractLogFileReader.this.file.getName() + "> " + line);
    // AbstractLogFileReader.this.client.parseExternalProcessResponseLine(this, line, AbstractLogFileReader.this.data);
    // }
    // l.setLength(0);
    // }
    // }
    // }
    public void waitFor() throws InterruptedException, IOException {
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
