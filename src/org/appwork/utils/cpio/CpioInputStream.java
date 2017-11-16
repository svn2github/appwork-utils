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
package org.appwork.utils.cpio;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Arrays;

import org.appwork.utils.net.CountingInputStream;
import org.appwork.utils.net.LimitedInputStream;

/**
 * @author daniel
 *
 *         https://www.cs.helsinki.fi/linux/linux-kernel/2002-01/0940.html
 *
 *         http://pubs.opengroup.org/onlinepubs/007908799/xsh/cpio.h.html
 *
 *         https://www.ibm.com/support/knowledgecenter/en/SSLTBW_2.2.0/com.ibm.zos.v2r2.bpxa500/fmtcpio.htm
 *
 *         https://www.mkssoftware.com/docs/man4/cpio.4.asp
 *
 *         https://people.freebsd.org/~kientzle/libarchive/man/cpio.5.txt
 * @date 10.11.2017
 *
 */
public class CpioInputStream extends InputStream {
    public static enum MODE {
        BLOCK(0060000),
        CHARACTER(0020000),
        DIRECTORY(0040000),
        PIPE(0010000),
        FILE(0100000),
        NETWORK(0110000),
        LINK(0120000),
        SOCKET(0140000);
        private final int mask;

        private MODE(int mask) {
            this.mask = mask;
        }

        public static MODE get(int mask) {
            mask = mask & 0170000;
            for (MODE mode : values()) {
                if (mode.mask == mask) {
                    return mode;
                }
            }
            return null;
        }
    }

    private final static int pad(long value, int padding) {
        final int pad = (int) value % padding;
        return pad > 0 ? padding - pad : 0;
    }

    protected class CpioEntryInputStream extends LimitedInputStream {
        protected final CpioEntry entry;

        protected CpioEntryInputStream(CpioInputStream cpioInputStream, CpioEntry entry) {
            super(cpioInputStream.is, entry.getSize());
            this.entry = entry;
        }

        @Override
        public int available() throws IOException {
            final long ret = this.getLimit() - this.transferedBytes();
            if (ret > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            return (int) ret;
        }

        protected CpioEntry getEntry() {
            return entry;
        }
    }

    protected CpioEntry readEntry_ASCII_NEW(final DataInputStream is, final MAGIC magic) throws IOException {
        final byte[] entryHeader = new byte[104];
        is.readFully(entryHeader);
        final int nameSize = (int) readHexASCII(entryHeader, 88);
        final byte[] name = new byte[nameSize - 1];
        is.readFully(name);
        is.readByte();// NUL of name
        final int padding = CpioInputStream.pad(110 + nameSize, 4);// multiple of 4
        if (padding > 0) {
            is.readFully(new byte[4], 0, padding);
        }
        return new CpioEntry() {
            @Override
            public MAGIC getMagic() {
                return magic;
            }

            @Override
            public long getSize() {
                return readHexASCII(entryHeader, 48);
            }

            @Override
            public MODE getMode() {
                return MODE.get((int) readHexASCII(entryHeader, 8));
            }

            @Override
            public byte[] getName() {
                return name;
            }

            @Override
            public long getInode() {
                return readHexASCII(entryHeader, 0);
            }

            @Override
            public long getUid() {
                return readHexASCII(entryHeader, 16);
            }

            @Override
            public long getGid() {
                return readHexASCII(entryHeader, 24);
            }

            @Override
            public long getMtime() {
                return readHexASCII(entryHeader, 40);
            }
        };
    }

    protected CpioEntry readEntry_BINARY_OLD(final DataInputStream is, final MAGIC magic) throws IOException {
        final byte[] entryHeader = new byte[24];
        is.readFully(entryHeader);
        final boolean swap = MAGIC.ASCII_OLD_BINARY_SWAP.equals(magic);
        final int nameSize = (int) readBinary(entryHeader, 18, 2, swap);
        final byte[] name = new byte[nameSize - 1];
        is.readFully(name);
        is.readByte();// NUL of name
        final int padding = CpioInputStream.pad(26 + nameSize, 2);// multiple of two
        if (padding > 0) {
            is.readFully(new byte[4], 0, padding);
        }
        return new CpioEntry() {
            @Override
            public MAGIC getMagic() {
                return magic;
            }

            @Override
            public long getSize() {
                return readBinary(entryHeader, 20, 4, swap);
            }

            @Override
            public MODE getMode() {
                return MODE.get((int) readBinary(entryHeader, 4, 2, swap));
            }

            @Override
            public byte[] getName() {
                return name;
            }

            @Override
            public long getInode() {
                return readBinary(entryHeader, 2, 2, swap);
            }

            @Override
            public long getUid() {
                return readBinary(entryHeader, 6, 2, swap);
            }

            @Override
            public long getGid() {
                return readBinary(entryHeader, 8, 2, swap);
            }

            @Override
            public long getMtime() {
                return readBinary(entryHeader, 14, 4, swap);
            }
        };
    }

    protected CpioEntry readEntry_ASCII_OLD(final DataInputStream is, final MAGIC magic) throws IOException {
        final byte[] entryHeader = new byte[70];
        is.readFully(entryHeader);
        final int nameSize = (int) readOctalASCII(entryHeader, 53, 6);
        final byte[] name = new byte[nameSize - 1];
        is.readFully(name);
        is.readByte();// NUL of name
        return new CpioEntry() {
            @Override
            public MAGIC getMagic() {
                return magic;
            }

            @Override
            public long getSize() {
                return readOctalASCII(entryHeader, 59, 11);
            }

            @Override
            public MODE getMode() {
                return MODE.get((int) readOctalASCII(entryHeader, 12, 6));
            }

            @Override
            public byte[] getName() {
                return name;
            }

            @Override
            public long getInode() {
                return readOctalASCII(entryHeader, 6, 6);
            }

            @Override
            public long getUid() {
                return readOctalASCII(entryHeader, 18, 6);
            }

            @Override
            public long getGid() {
                return readOctalASCII(entryHeader, 24, 6);
            }

            @Override
            public long getMtime() {
                return readOctalASCII(entryHeader, 42, 11);
            }
        };
    }

    protected long readHexASCII(byte[] array, int index) {
        long ret = 0;
        ret += (((Character.digit(array[index++], 16) << 4) + Character.digit(array[index++], 16)) & 255) << 24;
        ret += (((Character.digit(array[index++], 16) << 4) + Character.digit(array[index++], 16)) & 255) << 16;
        ret += (((Character.digit(array[index++], 16) << 4) + Character.digit(array[index++], 16)) & 255) << 8;
        ret += (((Character.digit(array[index++], 16) << 4) + Character.digit(array[index++], 16)) & 255) << 0;
        return ret;
    }

    protected long readBinary(byte[] array, int index, int length, boolean swapHalfWords) {
        switch (length) {
        case 2:
            if (!swapHalfWords) {
                return ((array[index + 1] & 0xff) << 8) + ((array[index] & 0xff));
            } else {
                return ((array[index] & 0xff) << 8) + ((array[index + 1] & 0xff));
            }
        case 4:
            if (!swapHalfWords) {
                long ret = (array[index + 1] & 0xff) << 24;
                ret += (array[index] & 0xff) << 16;
                ret += (array[index + 3] & 0xff) << 8;
                ret += (array[index + 2] & 0xff);
                return ret;
            } else {
                long ret = (array[index] & 0xff) << 24;
                ret += (array[index + 1] & 0xff) << 16;
                ret += (array[index + 2] & 0xff) << 8;
                ret += array[index + 3] & 0xff;
                return ret;
            }
        default:
            throw new UnsupportedOperationException("length:" + length + " is not supported");
        }
    }

    protected long readOctalASCII(byte[] array, int index, int length) {
        long ret = 0;
        for (int i = 0; i < length; i++) {
            ret += Character.digit(array[i + index], 8) * Math.pow(8, length - 1 - i);
        }
        return ret;
    }

    public static enum MAGIC {
        ASCII_NEW(new byte[] { 0x30, 0x37, 0x30, 0x37, 0x30, 0x31 }),
        ASCII_NEW_CRC(new byte[] { 0x30, 0x37, 0x30, 0x37, 0x30, 0x32 }),
        ASCII_OLD(new byte[] { 0x30, 0x37, 0x30, 0x37, 0x30, 0x37 }),
        ASCII_OLD_BINARY(new byte[] { (byte) 0xC7, 0x71 }),
        ASCII_OLD_BINARY_SWAP(new byte[] { 0x71, (byte) 0xC7 });// untested
        private final byte[] magic;

        private MAGIC(byte[] magic) {
            this.magic = magic;
        }

        public final byte[] getMagic() {
            return magic;
        }

        public static MAGIC get(final byte[] array) {
            if (array != null) {
                for (final MAGIC magic : MAGIC.values()) {
                    if (Arrays.equals(array, magic.getMagic())) {
                        return magic;
                    }
                }
                for (final MAGIC magic : MAGIC.values()) {
                    if (array.length > 2 && magic.magic[0] == array[0] && magic.magic[1] == array[1]) {
                        return magic;
                    }
                }
            }
            return null;
        }
    }

    protected final DataInputStream     is;
    protected CpioEntryInputStream      lis               = null;
    protected final CountingInputStream cis;
    protected final int                 blockSize;
    protected final static byte[]       TRAILER           = "TRAILER!!!".getBytes();
    public final static int             DEFAULT_BLOCKSIZE = 512;

    public CpioInputStream(final InputStream is) {
        this(is, DEFAULT_BLOCKSIZE);
    }

    public CpioInputStream(final InputStream is, final int blockSize) {
        this.is = new DataInputStream(cis = new CountingInputStream(is));
        this.blockSize = blockSize;
    }

    @Override
    public int available() throws IOException {
        return this.getCurrentInputStream().available();
    }

    @Override
    public void close() throws IOException {
        this.getCurrentInputStream().close();
    }

    protected InputStream getInputStream() throws IOException {
        return this.is;
    }

    private synchronized InputStream getCurrentInputStream() throws IOException {
        if (this.lis != null) {
            return this.lis;
        } else {
            return this.is;
        }
    }

    protected synchronized void skipEntry(CpioEntry entry) throws IOException {
        if (this.lis.getEntry() != entry) {
            throw new IllegalStateException("wrong cpio entry!?");
        }
        while (this.lis.available() > 0) {
            this.lis.skip(this.lis.available());
        }
        int padding = 0;
        switch (entry.getMagic()) {
        case ASCII_NEW:
        case ASCII_NEW_CRC:
            padding = CpioInputStream.pad(entry.getSize(), 4);
            break;
        case ASCII_OLD:
            break;
        case ASCII_OLD_BINARY:
            padding = CpioInputStream.pad(entry.getSize(), 2);
            break;
        default:
            break;
        }
        if (padding > 0) {
            is.readFully(new byte[4], 0, padding);
        }
    }

    protected boolean isTrailer(CpioEntry entry) throws IOException {
        return entry != null && entry.getSize() == 0 && Arrays.equals(TRAILER, entry.getName());
    }

    public int getBlockSize() {
        return blockSize;
    }

    protected void skipTrailer() throws IOException {
        final int skipTrailer = pad(cis.transferedBytes(), getBlockSize());
        if (skipTrailer > 0) {
            skip(skipTrailer);
        }
    }

    public synchronized CpioEntry getNextEntry() throws IOException {
        if (lis != null) {
            try {
                skipEntry(lis.getEntry());
            } finally {
                this.lis = null;
            }
        }
        final CpioEntry entry = this.readEntry();
        if (entry == null) {
            return null;
        } else if (isTrailer(entry)) {
            skipTrailer();
            return null;
        } else {
            this.lis = new CpioEntryInputStream(this, entry);
            return entry;
        }
    }

    @Override
    public synchronized void mark(final int readlimit) {
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int read() throws IOException {
        return this.getCurrentInputStream().read();
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return this.getCurrentInputStream().read(b);
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return this.getCurrentInputStream().read(b, off, len);
    }

    private CpioEntry readEntry() throws IOException {
        final int eof = is.read();
        if (eof == -1) {
            return null;
        }
        final byte[] magicMarker = new byte[6];
        magicMarker[0] = (byte) (eof & 0xff);
        is.readFully(magicMarker, 1, 5);
        final MAGIC magic = MAGIC.get(magicMarker);
        if (magic != null) {
            switch (magic) {
            case ASCII_NEW:
            case ASCII_NEW_CRC:
                return readEntry_ASCII_NEW(is, magic);
            case ASCII_OLD:
                return readEntry_ASCII_OLD(is, magic);
            case ASCII_OLD_BINARY:
                final PushbackInputStream pbis = new PushbackInputStream(is, 4);
                pbis.unread(magicMarker, 2, 4);
                return readEntry_BINARY_OLD(new DataInputStream(pbis), magic);
            default:
                throw new IOException("Unsupported magic:" + magic);
            }
        }
        throw new IOException("Unsupported magic:" + Arrays.toString(magicMarker));
    }

    @Override
    public synchronized void reset() throws IOException {
    }

    @Override
    public long skip(final long n) throws IOException {
        return this.getCurrentInputStream().skip(n);
    }
}
