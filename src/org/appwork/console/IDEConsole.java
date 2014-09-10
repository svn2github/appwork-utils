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
        private final Reader in;
        private char[]       cb;
        private int          nChars, nextChar;
        boolean              leftoverLF;

        LineReader(Reader in) {
            this.in = in;
            this.cb = new char[1024];
            this.nextChar = this.nChars = 0;
            this.leftoverLF = false;
        }

        @Override
        public void close() {
        }

        @Override
        public boolean ready() throws IOException {
            // in.ready synchronizes on readLock already
            return this.in.ready();
        }

        @Override
        public int read(char cbuf[], int offset, int length) throws IOException {
            int off = offset;
            int end = offset + length;
            if (offset < 0 || offset > cbuf.length || length < 0 || end < 0 || end > cbuf.length) { throw new IndexOutOfBoundsException(); }
            synchronized (IDEConsole.this.readLock) {
                boolean eof = false;
                char c = 0;
                for (;;) {
                    if (this.nextChar >= this.nChars) { // fill
                        int n = 0;
                        do {
                            n = this.in.read(this.cb, 0, this.cb.length);
                        } while (n == 0);
                        if (n > 0) {
                            this.nChars = n;
                            this.nextChar = 0;
                            if (n < this.cb.length && this.cb[n - 1] != '\n' && this.cb[n - 1] != '\r') {
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
                    if (this.leftoverLF && cbuf == IDEConsole.this.rcb && this.cb[this.nextChar] == '\n') {
                        /*
                         * if invoked by our readline, skip the leftover,
                         * otherwise return the LF.
                         */
                        this.nextChar++;
                    }
                    this.leftoverLF = false;
                    while (this.nextChar < this.nChars) {
                        c = cbuf[off++] = this.cb[this.nextChar];
                        this.cb[this.nextChar++] = 0;
                        if (c == '\n') {
                            return off - offset;
                        } else if (c == '\r') {
                            if (off == end) {
                                /*
                                 * no space left even the next is LF, so return
                                 * whatever we have if the invoker is not our
                                 * readLine()
                                 */
                                if (cbuf == IDEConsole.this.rcb) {
                                    cbuf = IDEConsole.this.grow();
                                    end = cbuf.length;
                                } else {
                                    this.leftoverLF = true;
                                    return off - offset;
                                }
                            }
                            if (this.nextChar == this.nChars && this.in.ready()) {
                                /*
                                 * we have a CR and we reached the end of the
                                 * read in buffer, fill to make sure we don't
                                 * miss a LF, if there is one, it's possible
                                 * that it got cut off during last round reading
                                 * simply because the read in buffer was full.
                                 */
                                this.nChars = this.in.read(this.cb, 0, this.cb.length);
                                this.nextChar = 0;
                            }
                            if (this.nextChar < this.nChars && this.cb[this.nextChar] == '\n') {
                                cbuf[off++] = '\n';
                                this.nextChar++;
                            }
                            return off - offset;
                        } else if (off == end) {
                            if (cbuf == IDEConsole.this.rcb) {
                                cbuf = IDEConsole.this.grow();
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
        char[] t = new char[this.rcb.length * 2];
        System.arraycopy(this.rcb, 0, t, 0, this.rcb.length);
        this.rcb = t;
        return this.rcb;
    }

    private final OutputStreamWriter out;
    private final InputStreamReader  in;
    private final PrintWriter        writer;
    private final LineReader         reader;
    private final Object             readLock;
    private final Object             writeLock;
    private char[]                   rcb;

    public IDEConsole() {

        Charset cs = Charset.defaultCharset();
        this.out = new OutputStreamWriter(new FileOutputStream(FileDescriptor.out), cs);
        this.readLock = this.out;

        this.writer = new PrintWriter(this.out, true) {
            @Override
            public void close() {
            }
        };

        this.in = new InputStreamReader(new FileInputStream(FileDescriptor.in), cs);
        this.writeLock = this.in;
        this.reader = new LineReader(this.in);
        this.rcb = new char[1024];
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.appwork.utils.swing.dialog.AbstractConsole#write(java.lang.String)
     */
    @Override
    public void println(String string) {

        this.writer.println(string);

    }

    public String readLine(String fmt, Object... args) {
        String line = null;
        synchronized (this.writeLock) {
            synchronized (this.readLock) {
                if (fmt.length() != 0) {
                    this.writer.format(fmt, args);
                }
                try {
                    char[] ca = this.readline(false);
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
        return this.readLine("");
    }

    private char[] readline(boolean zeroOut) throws IOException {
        int len = this.reader.read(this.rcb, 0, this.rcb.length);
        if (len < 0) { return null; // EOL
        }
        if (this.rcb[len - 1] == '\r') {
            len--; // remove CR at end;
        } else if (this.rcb[len - 1] == '\n') {
            len--; // remove LF at end;
            if (len > 0 && this.rcb[len - 1] == '\r') {
                len--; // remove the CR, if
                       // there is one
            }
        }
        char[] b = new char[len];
        if (len > 0) {
            System.arraycopy(this.rcb, 0, b, 0, len);
            if (zeroOut) {
                Arrays.fill(this.rcb, 0, len, ' ');
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
        this.writer.print(string);
        try {
            this.out.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.console.AbstractConsole#readPassword()
     */
    @Override
    public String readPassword() {
        return this.readLine("");
    }

}
