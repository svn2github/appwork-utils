package org.appwork.utils.processes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.appwork.loggingv3.LogV3;

public class ProcessOutputLinereadBuffer extends OutputStream {
    /**
     * @author Thomas
     * @date 17.10.2018
     *
     */
    private static final int MIN_STEP       = 32 * 1024;
    private int              writeIndex;
    private int              readIndex;
    private byte[]           bb;
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
        this.readIndex = 0;
        this.writeIndex = 0;
        try {
            this.consoleCharset = Charset.forName(ProcessBuilderFactory.getConsoleCodepage());
            // this.convert = this.consoleCharset != null && !UTF8.equals(this.consoleCharset);
        } catch (final Throwable e) {
            LogV3.logger(ProcessOutputLinereadBuffer.class).exception("Charset Issue", e);
        }
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

    private int size() {
        return writeIndex - readIndex;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        LogV3.info(">>>" + new String(b, off, len) + "<<<");
        this.resizeIfRequired(len);
        System.arraycopy(b, off, this.bb, this.writeIndex, len);
        this.writeIndex += len;
        this.forwardLinesToSink();
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
        this.forwardLinesToSink();
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[] { (byte) b }, 0, 1);
    }

    private byte last;

    private int forwardLinesToSink() {
        int lines = 0;
        ByteArrayOutputStream l = new ByteArrayOutputStream();
        while (true) {
            if (size() == 0) {
                break;
            }
            byte c;
            for (int i = readIndex; i < writeIndex; i++) {
                c = bb[i];
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
                        l.write(c);
                        continue;
                    }
                } finally {
                    last = c;
                }
                this.sink.handleLine(new String(l.toByteArray(), consoleCharset), this);
                lines++;
                readIndex = i;
                l.reset();
            }
        }
        if (closed && l.size() > 0) {
            this.sink.handleLine(new String(l.toByteArray(), consoleCharset), this);
            readIndex = writeIndex;
            lines++;
            l.reset();
        }
        if (lines > 0) {
            resizeIfRequired(0);
        }
        return lines;
    }
}