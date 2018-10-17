package org.appwork.utils.processes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.appwork.exceptions.WTFException;
import org.appwork.loggingv3.LogV3;

public class ProcessOutputLinereadBuffer {
    private static final int MIN_STEP       = 32 * 1024;
    private int              writeIndex;
    private int              readIndex;
    private int              totalReadIndex;
    private int              totalWriteIndex;
    private byte[]           bb;
    private BufferedReader   reader;
    private boolean          closed;
    public static Charset    UTF8           = Charset.forName("UTF-8");
    private Charset          consoleCharset = UTF8;
    private LineHandler      sink;

    /**
     * can be used as outputstream for #ProcessBuilderFactory runCommand methods. the class will read the output, buffer it, and once a
     * lineend is detected, push it to the Linehandler. <br>
     * IMPORTANT: call {@link #close()} once the process finished to flush the buffer and handle the last line
     * 
     * @param sink
     */
    public ProcessOutputLinereadBuffer(LineHandler sink) {
        this.bb = new byte[MIN_STEP];
        this.sink = sink;
        this.totalReadIndex = 0;
        this.totalWriteIndex = 0;
        this.readIndex = 0;
        this.writeIndex = 0;
        try {
            this.consoleCharset = Charset.forName(ProcessBuilderFactory.getConsoleCodepage());
            // this.convert = this.consoleCharset != null && !UTF8.equals(this.consoleCharset);
        } catch (final Throwable e) {
            LogV3.logger(ProcessOutputLinereadBuffer.class).exception("Charset Issue", e);
        }
        this.reader = new BufferedReader(new InputStreamReader(this.getInputStream(), this.consoleCharset));
    }

    private InputStream getInputStream() {
        return new InputStream() {
            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                while (this.available() == 0) {
                    synchronized (ProcessOutputLinereadBuffer.this) {
                        if (ProcessOutputLinereadBuffer.this.closed) {
                            return -1;
                        }
                        try {
                            this.wait(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IOException("Closed by Interrupt");
                        }
                    }
                }
                synchronized (ProcessOutputLinereadBuffer.this) {
                    int toRead = Math.min(this.available(), len);
                    System.arraycopy(ProcessOutputLinereadBuffer.this.bb, ProcessOutputLinereadBuffer.this.readIndex, b, off, toRead);
                    ProcessOutputLinereadBuffer.this.totalReadIndex += toRead;
                    ProcessOutputLinereadBuffer.this.readIndex = toRead;
                    ProcessOutputLinereadBuffer.this.resizeIfRequired(0);
                    return toRead;
                }
            }

            @Override
            public void close() throws IOException {
                // super.close();
            }

            @Override
            public int read() throws IOException {
                synchronized (ProcessOutputLinereadBuffer.this) {
                    if (this.available() == 0) {
                        // nothing to read;
                        return -1;
                    }
                    int ret = ProcessOutputLinereadBuffer.this.bb[ProcessOutputLinereadBuffer.this.readIndex];
                    ProcessOutputLinereadBuffer.this.readIndex++;
                    ProcessOutputLinereadBuffer.this.totalReadIndex++;
                    ProcessOutputLinereadBuffer.this.resizeIfRequired(0);
                    return ret;
                }
            }

            @Override
            public int available() throws IOException {
                return ProcessOutputLinereadBuffer.this.writeIndex - ProcessOutputLinereadBuffer.this.readIndex;
            }

            @Override
            public int read(byte[] b) throws IOException {
                return this.read(b, 0, b.length);
            }
        };
    }

    public void write(byte[] b, int off, int len) {
        synchronized (this) {
            this.resizeIfRequired(len);
            System.arraycopy(b, off, this.bb, this.writeIndex, len);
            this.writeIndex += len;
            this.totalWriteIndex += len;
            this.notifyAll();
        }
        this.handleLines();
    }

    private void resizeIfRequired(int len) {
        int unreadBytesInBuffer = this.writeIndex - this.readIndex;
        int remainingToWrite = this.bb.length - this.writeIndex;
        if (remainingToWrite < len || this.readIndex > MIN_STEP) {
            byte[] newBB = new byte[unreadBytesInBuffer + Math.max(len, MIN_STEP)];
            System.arraycopy(this.bb, this.readIndex, newBB, 0, unreadBytesInBuffer);
            this.writeIndex -= this.readIndex;
            this.readIndex = 0;
            this.bb = newBB;
        }
    }

    // if (this.convert) {
    // final ByteBuffer inputBuffer = ByteBuffer.wrap(b, off, len);
    // final CharBuffer data = this.consoleCharset.decode(inputBuffer);
    // final ByteBuffer outputBuffer = this.UTF8.encode(data);
    // final byte write[];
    // if (outputBuffer.limit() != outputBuffer.array().length) {
    // write = new byte[outputBuffer.limit()];
    // outputBuffer.get(write);
    // } else {
    // write = outputBuffer.array();
    // }
    // this.job.write(write);
    // this.output.write(write);
    // } else {
    // this.job.write(b, off, len);
    // this.output.write(b, off, len);
    // }
    public String[] getLines() {
        try {
            ArrayList<String> lines = new ArrayList<String>();
            char[] cbuf = new char[1024];
            StringBuilder l = new StringBuilder();
            boolean newline = false;
            while (true) {
                int read;
                if (this.closed) {
                    read = this.reader.read(cbuf);
                    if (read < 0) {
                        if (l.length() > 0) {
                            lines.add(l.toString());
                        }
                        return lines.toArray(new String[] {});
                    }
                } else {
                    if (!this.reader.ready()) {
                        return lines.toArray(new String[] {});
                    }
                    read = this.reader.read(cbuf);
                    if (read < 0) {
                        return lines.toArray(new String[] {});
                    }
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
                        lines.add(l.toString());
                        l.setLength(0);
                        newline = false;
                    }
                    l.append(c);
                    System.out.println(l);
                }
                if (newline) {
                    lines.add(l.toString());
                    l.setLength(0);
                    newline = false;
                }
            }
        } catch (Exception e) {
            throw new WTFException(e);
        }
    }

    public void close() throws IOException {
        this.closed = true;
        this.handleLines();
    }

    private void handleLines() {
        for (String line : this.getLines()) {
            this.sink.handleLine(line, this);
        }
    }

    public OutputStream getOutputStream() {
        return new OutputStream() {
            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                LogV3.info(">>>" + new String(b, off, len) + "<<<");
                ProcessOutputLinereadBuffer.this.write(b, off, len);
            }

            @Override
            public void write(byte[] b) throws IOException {
                this.write(b, 0, b.length);
            }

            @Override
            public void close() throws IOException {
                //
            }

            @Override
            public void write(int b) throws IOException {
                ProcessOutputLinereadBuffer.this.write(new byte[] { (byte) b }, 0, 1);
            }
        };
    }
}