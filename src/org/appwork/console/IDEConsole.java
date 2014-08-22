/**
 * Copyright (c) 2009 - 2014 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils.swing.dialog
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.console;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * @author Thomas
 * 
 */
public class IDEConsole extends AbstractConsole {
    class LineReader extends Reader {
        private Reader in;
        private char[] cb;
        private int    nChars, nextChar;
        boolean        leftoverLF;

        LineReader(Reader in) {
            this.in = in;
            cb = new char[1024];
            nextChar = nChars = 0;
            leftoverLF = false;
        }

        public void close() {
        }

        public boolean ready() throws IOException {
            // in.ready synchronizes on readLock already
            return in.ready();
        }

        public int read(char cbuf[], int offset, int length) throws IOException {
            int off = offset;
            int end = offset + length;
            if (offset < 0 || offset > cbuf.length || length < 0 || end < 0 || end > cbuf.length) { throw new IndexOutOfBoundsException(); }
            synchronized (readLock) {
                boolean eof = false;
                char c = 0;
                for (;;) {
                    if (nextChar >= nChars) { // fill
                        int n = 0;
                        do {
                            n = in.read(cb, 0, cb.length);
                        } while (n == 0);
                        if (n > 0) {
                            nChars = n;
                            nextChar = 0;
                            if (n < cb.length && cb[n - 1] != '\n' && cb[n - 1] != '\r') {
                                /*
                                 * we're in canonical mode so each "fill" should
                                 * come back with an eol. if there no lf or nl
                                 * at the end of returned bytes we reached an
                                 * eof.
                                 */
                                eof = true;
                            }
                        } else { /* EOF */
                            if (off - offset == 0) { return -1; }
                            return off - offset;
                        }
                    }
                    if (leftoverLF && cbuf == rcb && cb[nextChar] == '\n') {
                        /*
                         * if invoked by our readline, skip the leftover,
                         * otherwise return the LF.
                         */
                        nextChar++;
                    }
                    leftoverLF = false;
                    while (nextChar < nChars) {
                        c = cbuf[off++] = cb[nextChar];
                        cb[nextChar++] = 0;
                        if (c == '\n') {
                            return off - offset;
                        } else if (c == '\r') {
                            if (off == end) {
                                /*
                                 * no space left even the next is LF, so return
                                 * whatever we have if the invoker is not our
                                 * readLine()
                                 */
                                if (cbuf == rcb) {
                                    cbuf = grow();
                                    end = cbuf.length;
                                } else {
                                    leftoverLF = true;
                                    return off - offset;
                                }
                            }
                            if (nextChar == nChars && in.ready()) {
                                /*
                                 * we have a CR and we reached the end of the
                                 * read in buffer, fill to make sure we don't
                                 * miss a LF, if there is one, it's possible
                                 * that it got cut off during last round reading
                                 * simply because the read in buffer was full.
                                 */
                                nChars = in.read(cb, 0, cb.length);
                                nextChar = 0;
                            }
                            if (nextChar < nChars && cb[nextChar] == '\n') {
                                cbuf[off++] = '\n';
                                nextChar++;
                            }
                            return off - offset;
                        } else if (off == end) {
                            if (cbuf == rcb) {
                                cbuf = grow();
                                end = cbuf.length;
                            } else {
                                return off - offset;
                            }
                        }
                    }
                    if (eof) { return off - offset; }
                }
            }
        }
    }

    private char[] grow() {

        char[] t = new char[rcb.length * 2];
        System.arraycopy(rcb, 0, t, 0, rcb.length);
        rcb = t;
        return rcb;
    }

    private OutputStreamWriter out;
    private InputStreamReader  in;
    private PrintWriter        writer;
    private LineReader         reader;
    private Object             readLock;
    private Object             writeLock;
    private char[]             rcb;

    public IDEConsole() {

        Charset cs = Charset.defaultCharset();
        out = new OutputStreamWriter(new FileOutputStream(FileDescriptor.out), cs);
        readLock = out;

        writer = new PrintWriter(out, true) {
            public void close() {
            }
        };

        in = new InputStreamReader(new FileInputStream(FileDescriptor.in), cs);
        writeLock = in;
        reader = new LineReader(in);
        rcb = new char[1024];
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.appwork.utils.swing.dialog.AbstractConsole#write(java.lang.String)
     */
    @Override
    public void println(String string) {

        writer.println(string);

    }

    public String readLine(String fmt, Object... args) {
        String line = null;
        synchronized (writeLock) {
            synchronized (readLock) {
                if (fmt.length() != 0) {
                    writer.format(fmt, args);
                }
                try {
                    char[] ca = readline(false);
                    if (ca != null) {
                        line = new String(ca);
                    }
                } catch (IOException x) {
                    throw new IOError(x);
                }
            }
        }
        return line;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.utils.swing.dialog.AbstractConsole#readLine()
     */
    @Override
    public String readLine() {
        return readLine("");
    }

    private char[] readline(boolean zeroOut) throws IOException {
        int len = reader.read(rcb, 0, rcb.length);
        if (len < 0) { return null; // EOL
        }
        if (rcb[len - 1] == '\r') {
            len--; // remove CR at end;
        } else if (rcb[len - 1] == '\n') {
            len--; // remove LF at end;
            if (len > 0 && rcb[len - 1] == '\r') {
                len--; // remove the CR, if
                       // there is one
            }
        }
        char[] b = new char[len];
        if (len > 0) {
            System.arraycopy(rcb, 0, b, 0, len);
            if (zeroOut) {
                Arrays.fill(rcb, 0, len, ' ');
            }
        }
        return b;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.appwork.utils.swing.dialog.console.AbstractConsole#print(java.lang
     * .String)
     */
    @Override
    public void print(String string) {
        writer.print(string);
        try {
            out.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
