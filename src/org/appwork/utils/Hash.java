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
 *     The intent is that the AppWork GmbH is able to provide their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 *     These terms apply to all files that have the [The Product] License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 *
 * === 3rd Party Licences ===
 *     Some parts of the [The Product] use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 *     to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header.
 *
 * === Definition: Commercial Usage ===
 *     If anybody or any organization is generating income (directly or indirectly) by using [The Product] or if there's any commercial interest or aspect in what you are doing, we consider this as a commercial usage.
 *     If your use-case is neither strictly private nor strictly educational, it is commercial. If you are unsure whether your use-case is commercial or not, consider it as commercial or contact us.
 * === Dual Licensing ===
 * === Commercial Usage ===
 *     If you want to use [The Product] in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 *     Contact AppWork for further details: <e-mail@appwork.org>
 * === Non-Commercial Usage ===
 *     If there is no commercial usage (see definition above), you may use [The Product] under the terms of the
 *     "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 *
 *     If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;

import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.net.NullOutputStream;

public class Hash {
    public static final String HASH_TYPE_SHA256 = "SHA-256";
    public static final String HASH_TYPE_SHA512 = "SHA-512";
    public static String       HASH_TYPE_MD5    = "md5";
    public static String       HASH_TYPE_SHA1   = "SHA-1";

    /**
     * @param download
     * @param hashType
     * @return
     */
    public static String getBytesHash(final byte[] download, final String type) {
        try {
            final MessageDigest md = MessageDigest.getInstance(type);
            final byte[] digest = md.digest(download);
            return HexFormatter.byteArrayToHex(digest);
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    public static long getCRC32(final byte[] data) throws IOException {
        final CheckedOutputStream cos = new CheckedOutputStream(new NullOutputStream(), new CRC32());
        try {
            cos.write(data);
            return cos.getChecksum().getValue();
        } finally {
            cos.close();
        }
    }

    public static long getCRC32(final File arg) throws IOException {
        FileInputStream fis = null;
        CheckedInputStream cis = null;
        try {
            fis = new FileInputStream(arg);
            cis = new CheckedInputStream(fis, new CRC32());
            final byte readBuffer[] = new byte[32767];
            while (cis.read(readBuffer) >= 0) {
            }
            return cis.getChecksum().getValue();
        } finally {
            try {
                cis.close();
            } catch (final Throwable e) {
            }
            try {
                fis.close();
            } catch (final Throwable e) {
            }
        }
    }

    public static byte[] getFileHashBytes(final File arg, final String type) {
        if (arg == null || !arg.exists() || arg.isDirectory()) {
            return null;
        }
        FileInputStream fis = null;
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(type);
            // if (true) { throw new IOException("Any IOEXCeption"); }
            final byte[] b = new byte[32767];
            fis = new FileInputStream(arg);
            int n = 0;
            while ((n = fis.read(b)) >= 0) {
                if (n > 0) {
                    md.update(b, 0, n);
                }
            }
        } catch (final Throwable e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                fis.close();
            } catch (final Throwable e) {
            }
        }
        return md.digest();
    }

    public static String getFileHash(final File arg, final String type) {
        byte[] bytes = Hash.getFileHashBytes(arg, type);
        if (bytes == null) {
            return null;
        }
        return HexFormatter.byteArrayToHex(bytes);
    }

    public static String getFileHash(final File arg, final String type, final long maxHash) {
        if (arg == null || !arg.exists() || arg.isDirectory()) {
            return null;
        }
        FileInputStream fis = null;
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(type);
            // if (true) { throw new IOException("Any IOEXCeption"); }
            int bufferSize = 32767;
            if (maxHash < bufferSize) {
                bufferSize = (int) maxHash;
            }
            final byte[] b = new byte[bufferSize];
            fis = new FileInputStream(arg);
            int n = 0;
            long todo = maxHash;
            while ((n = fis.read(b, 0, bufferSize)) >= 0) {
                if (n > 0) {
                    md.update(b, 0, n);
                }
                if (maxHash > 0 && n > 0) {
                    todo -= n;
                    if (todo == 0) {
                        break;
                    }
                    if (todo < bufferSize) {
                        bufferSize = (int) todo;
                    }
                }
            }
        } catch (final Throwable e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                fis.close();
            } catch (final Throwable e) {
            }
        }
        final byte[] digest = md.digest();
        return HexFormatter.byteArrayToHex(digest);
    }

    /**
     * @param download
     * @return
     */
    public static String getMD5(final byte[] download) {
        return Hash.getBytesHash(download, Hash.HASH_TYPE_MD5);
    }

    public static String getMD5(final File arg) {
        return Hash.getFileHash(arg, Hash.HASH_TYPE_MD5);
    }

    public static String getMD5(final String arg) {
        return Hash.getStringHash(arg, Hash.HASH_TYPE_MD5);
    }

    public static String getSHA1(final File arg) {
        return Hash.getFileHash(arg, Hash.HASH_TYPE_SHA1);
    }

    public static String getSHA1(final String arg) {
        return Hash.getStringHash(arg, Hash.HASH_TYPE_SHA1);
    }

    /**
     * @param download
     * @return
     */
    public static String getSHA256(final byte[] download) {
        return Hash.getBytesHash(download, Hash.HASH_TYPE_SHA256);
    }

    /**
     * @param f
     * @return
     */
    public static String getSHA256(final File f) {
        return Hash.getFileHash(f, Hash.HASH_TYPE_SHA256);
    }

    /**
     * @param createPostData
     * @return
     */
    public static String getSHA256(final String createPostData) {
        return Hash.getStringHash(createPostData, Hash.HASH_TYPE_SHA256);
    }

    public static String getStringHash(final String arg, final String type) {
        try {
            final MessageDigest md = MessageDigest.getInstance(type);
            final byte[] digest = md.digest(arg.getBytes("UTF-8"));
            return HexFormatter.byteArrayToHex(digest);
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        return null;
    }
}
