package org.appwork.utils.processes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;

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
    private File                       file;
    private volatile boolean           closed = false;
    private Thread                     thread;
    protected InputStream              is;
    private LineHandler                sink;
    private Charset                    charset;
    private ArrayList<NewlineSequence> nlSequences;
    private byte                       first;

    protected void setSink(LineHandler sink) {
        this.sink = sink;
    }

    protected class NewlineSequence {
        private final byte[] bytes;
        private byte         index;
        private boolean      valid;

        /**
         * @param string
         * @param charset
         */
        public NewlineSequence(String string, Charset charset) {
            bytes = string.getBytes(charset);
            index = 0;
            valid = true;
        }

        /**
         * @param c
         * @return
         */
        public boolean add(byte c) {
            if (!valid) {
                return false;
            }
            if (index == bytes.length) {
                return valid = false;
            }
            valid &= bytes[index++] == c;
            return valid && index == bytes.length;
        }

        /**
         *
         */
        public void reset() {
            valid = true;
            index = 0;
        }
    }

    public ContinuesFileLineReader(LineHandler sink, String path, Charset charset) {
        this.file = new File(path);
        this.sink = sink;
        if (charset == null) {
            charset = Charset.forName("UTF-8");
        }
        this.charset = charset;
        nlSequences = new ArrayList<NewlineSequence>();
        nlSequences.add(new NewlineSequence("\r", charset));
        nlSequences.add(new NewlineSequence("\n", charset));
        nlSequences.add(new NewlineSequence("\r\n", charset));
        first = (byte) 0;
        for (NewlineSequence nl : nlSequences) {
            first |= nl.bytes[0];
        }
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
            private int lines;
            private int waiting;

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
                lines = 0;
                try {
                    byte[] cbuf = new byte[128 * 1024];
                    long t = System.currentTimeMillis();
                    ContinuesFileLineReader.this.is = getInputStream();
                    final ByteArrayOutputStream bao = new ByteArrayOutputStream(16 * 1024) {
                        /*
                         * (non-Javadoc)
                         *
                         * @see java.io.ByteArrayOutputStream#toByteArray()
                         */
                        @Override
                        public synchronized byte[] toByteArray() {
                            // TODO Auto-generated method stub
                            return buf;
                        }
                    };
                    NewlineSequence triggerSequence = null;
                    while (true) {
                        int length = is.read(cbuf);
                        if (length == -1) {
                            if (t > 0) {
                                System.out.println(System.currentTimeMillis() - t);
                                System.out.println("Lines: " + lines);
                                t = 0;
                            }
                            if (closed) {
                                if (bao.size() > 0) {
                                    // no empty newline at the end
                                    lines++;
                                    toSink(bao, null);
                                    bao.reset();
                                }
                                return;
                            } else {
                                waiting++;
                                Thread.sleep(20);
                            }
                        } else {
                            byte c;
                            for (int i = 0; i < length; i++) {
                                c = cbuf[i];
                                NewlineSequence match = null;
                                byte valids = 0;
                                if ((c & first) == c) {
                                    for (NewlineSequence nl : nlSequences) {
                                        if (nl.add(c)) {
                                            match = nl;
                                        }
                                        if (nl.valid) {
                                            valids++;
                                        }
                                    }
                                }
                                if (valids == 0 && triggerSequence != null) {
                                    bao.reset();
                                    resetSequences();
                                    valids = 0;
                                    if ((c & first) == c) {
                                        for (NewlineSequence nl : nlSequences) {
                                            if (nl.add(c)) {
                                                match = nl;
                                            }
                                            if (nl.valid) {
                                                valids++;
                                            }
                                        }
                                    }
                                    triggerSequence = null;
                                }
                                if (valids == 0) {
                                    resetSequences();
                                }
                                bao.write(c);
                                if (match != null && triggerSequence == null) {
                                    lines++;
                                    if (waiting > 0) {
                                        System.out.println(lines);
                                    }
                                    toSink(bao, match);
                                    triggerSequence = match;
                                    bao.reset();
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    exceptionIOException = e;
                } catch (InterruptedException e) {
                    // we leave the thread, no reason to reset the interrupt flag
                    return;
                } finally {
                    if (ContinuesFileLineReader.this.is != null) {
                        try {
                            ContinuesFileLineReader.this.is.close();
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

    /**
     *
     */
    protected void resetSequences() {
        for (NewlineSequence nl : nlSequences) {
            nl.reset();
        }
    }

    public void closeAndFlush() throws InterruptedException, IOException {
        try {
            if (closed) {
                return;
            }
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

    protected InputStream getInputStream() throws FileNotFoundException {
        return new FileInputStream(ContinuesFileLineReader.this.file);
    }

    protected void toSink(ByteArrayOutputStream bao, NewlineSequence match) {
        sink.handleLine(new String(bao.toByteArray(), 0, bao.size() - (match == null ? 0 : match.bytes.length), charset), this);
    }
}
