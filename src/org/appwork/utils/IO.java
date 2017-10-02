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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.Files.AbstractHandler;
import org.appwork.utils.os.CrossSystem;

public class IO {
    public static enum SYNC {
        /**
         * do not sync filesystem at all
         */
        NONE,
        /**
         * sync written data to filesystem
         */
        DATA,
        /**
         * sync written data and its meta-data (filesystem information)
         */
        META_AND_DATA
    }

    private static IOErrorHandler ERROR_HANDLER = null;

    public static void copyFile(final File in, final File out) throws IOException {
        IO.copyFile(in, out, null);
    }

    public static void copyFile(final File in, final File out, final SYNC sync) throws IOException {
        IO.copyFile(null, in, out, sync);
    }

    public static void copyFile(ProgressFeedback progress, final File in, final File out, final SYNC sync) throws IOException {
        try {
            FileInputStream fis = null;
            FileOutputStream fos = null;
            FileChannel inChannel = null;
            FileChannel outChannel = null;
            try {
                if (out.exists()) {
                    throw new IOException("Cannot overwrite " + out);
                }
                if (!in.exists()) {
                    throw new FileNotFoundException(in.getAbsolutePath());
                }
                fis = new FileInputStream(in);
                fos = new FileOutputStream(out);
                inChannel = fis.getChannel();
                outChannel = fos.getChannel();
                if (progress != null) {
                    progress.setBytesTotal(in.length());
                }
                if (CrossSystem.isWindows()) {
                    // magic number for Windows, 64Mb - 32Kb)
                    // On the Windows plateform, you can't copy a file bigger
                    // than
                    // 64Mb,
                    // an Exception in thread "main" java.io.IOException:
                    // Insufficient
                    // system resources exist to complete the requested service
                    // is
                    // thrown.
                    //
                    // For a discussion about this see :
                    // http://forum.java.sun.com/thread.jspa?threadID=439695&messageID=2917510
                    final int maxCount = 64 * 1024 * 1024 - 32 * 1024;
                    final long size = inChannel.size();
                    long position = 0;
                    while (position < size) {
                        position += inChannel.transferTo(position, maxCount, outChannel);
                        if (progress != null) {
                            progress.setBytesProcessed(position);
                        }
                    }
                } else {
                    /* we also loop here to make sure all data got transfered! */
                    final int maxCount = 64 * 1024 * 1024 - 32 * 1024;
                    final long size = inChannel.size();
                    long position = 0;
                    while (position < size) {
                        position += inChannel.transferTo(position, maxCount, outChannel);
                        if (progress != null) {
                            progress.setBytesProcessed(position);
                        }
                    }
                }
                if (sync != null) {
                    if (sync != null) {
                        switch (sync) {
                        case DATA:
                            outChannel.force(false);
                            break;
                        case META_AND_DATA:
                            outChannel.force(true);
                            break;
                        default:
                            break;
                        }
                    }
                }
            } catch (final IOException e) {
                throw e;
            } finally {
                try {
                    fos.close();
                } catch (final Throwable e) {
                }
                try {
                    fis.close();
                } catch (final Throwable e) {
                }
            }
        } catch (final IOException e) {
            if (IO.ERROR_HANDLER != null) {
                IO.ERROR_HANDLER.onCopyException(e, in, out);
            }
            throw e;
        } catch (final RuntimeException e) {
            if (IO.ERROR_HANDLER != null) {
                IO.ERROR_HANDLER.onCopyException(e, in, out);
            }
            throw e;
        } catch (final Error e) {
            if (IO.ERROR_HANDLER != null) {
                IO.ERROR_HANDLER.onCopyException(e, in, out);
            }
            throw e;
        }
    }

    public static void copyFolderRecursive(final File src, final File dest, final boolean overwriteFiles) throws IOException {
        IO.copyFolderRecursive(src, dest, overwriteFiles, SYNC.NONE);
    }

    public static void copyFolderRecursive(final File src, final File dest, final boolean overwriteFiles, final FileFilter filter, final SYNC sync) throws IOException {
        Files.walkThroughStructure(new AbstractHandler<IOException>() {
            @Override
            public void onFile(final File f) throws IOException {
                if (filter != null && !filter.accept(f)) {
                    return;
                }
                final String path = Files.getRelativePath(src, f);
                if (path == null) {
                    throw new IOException("No rel Path " + src + "-" + f);
                }
                if (f.isDirectory()) {
                    new File(dest, path).mkdirs();
                } else {
                    final File dst = new File(dest, path);
                    if (overwriteFiles && dst.exists()) {
                        if (!dst.delete()) {
                            //
                            throw new IOException("Cannot overwrite " + dst);
                        }
                    }
                    dst.getParentFile().mkdirs();
                    IO.copyFile(f, dst, sync);
                }
                return;
            }
        }, src);
    }

    /**
     * @param overwriteFiles
     *            TODO
     * @param dist
     * @param dist2
     * @throws IOException
     */
    public static void copyFolderRecursive(final File src, final File dest, final boolean overwriteFiles, final SYNC sync) throws IOException {
        IO.copyFolderRecursive(src, dest, overwriteFiles, null, sync);
    }

    public static IOErrorHandler getErrorHandler() {
        return IO.ERROR_HANDLER;
    }

    public static String importFileToString(final File file) throws IOException {
        return IO.importFileToString(file, -1);
    }

    public static enum BOM {
        UTF8(new byte[] { (byte) 239, (byte) 187, (byte) 191 }, "UTF-8"),
        UTF16BE(new byte[] { (byte) 254, (byte) 255 }, "UTF-16BE"),
        UTF16LE(new byte[] { (byte) 255, (byte) 254 }, "UTF-16LE"),
        UTF32BE(new byte[] { (byte) 0, (byte) 0, (byte) 254, (byte) 255 }, "UTF-32BE"),
        UTF32LE(new byte[] { (byte) 0, (byte) 0, (byte) 255, (byte) 254 }, "UTF-32LE");
        private final byte[] bomMarker;
        private final String charSet;

        public final String getCharSet() {
            return charSet;
        }

        private BOM(final byte[] bomMarker, final String charSet) {
            this.bomMarker = bomMarker;
            this.charSet = charSet;
        }

        public final int length() {
            return bomMarker.length;
        }

        public byte[] getBOM() {
            return bomMarker.clone();
        }

        public static BOM get(byte[] bytes) {
            for (final BOM bom : BOM.values()) {
                if (bom.startsWith(bytes)) {
                    return bom;
                }
            }
            return null;
        }

        public boolean startsWith(byte[] bytes) {
            if (bytes != null && bytes.length >= length()) {
                for (int index = 0; index < length(); index++) {
                    if (bytes[index] != bomMarker[index]) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        public static String read(byte[] bytes) throws IOException {
            for (final BOM bom : BOM.values()) {
                if (bom.startsWith(bytes)) {
                    return new String(bytes, bom.length(), bytes.length - bom.length(), bom.getCharSet());
                }
            }
            return null;
        }
    }

    public static String importFileToString(final File file, final int maxSize) throws IOException {
        final byte[] bytes = IO.readFile(file, maxSize);
        if (bytes == null) {
            return null;
        }
        final String ret = BOM.read(bytes);
        if (ret != null) {
            return ret;
        }
        return new String(bytes, "UTF-8");
    }

    public static void moveTo(final File source, final File dest, final FileFilter filter) throws IOException {
        final java.util.List<File> files = Files.getFiles(filter, source);
        // TODO Proper delete
        for (final File src : files) {
            final String rel = Files.getRelativePath(source, src);
            final File file = new File(dest, rel);
            if (src.isDirectory()) {
                file.mkdirs();
            } else {
                file.getParentFile().mkdirs();
                System.out.println(src + " -> " + file);
                if (!src.renameTo(file)) {
                    throw new IOException("Could not move file " + src + " to " + file);
                }
            }
        }
    }

    public static RandomAccessFile open(File file, String mode) throws IOException {
        if (CrossSystem.isWindows()) {
            int retry = 0;
            while (true) {
                try {
                    return new RandomAccessFile(file, mode);
                } catch (final FileNotFoundException e) {
                    /**
                     * too fast file opening/extraction (eg image gallery) can result in "access denied" exception
                     */
                    if (retry < 3) {
                        if (retry == 2 && CrossSystem.isWindows()) {
                            // http://stackoverflow.com/questions/10516472/file-createnewfile-randomly-fails
                            // http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6213298
                            System.gc();
                        }
                        try {
                            Thread.sleep(500 * retry++);
                        } catch (InterruptedException e1) {
                            throw e;
                        }
                    } else {
                        throw e;
                    }
                }
            }
        } else {
            return new RandomAccessFile(file, mode);
        }
    }

    public static byte[] readFile(final File ressource) throws IOException {
        int maxRead = -1;
        if (ressource.length() < Integer.MAX_VALUE) {
            maxRead = (int) ressource.length();
        }
        return IO.readFile(ressource, maxRead);
    }

    /*
     * this function reads a line from a bufferedinputstream up to a maxLength. in case the line is longer than maxLength the rest of the
     * line is read but not returned
     *
     * this function skips emtpy lines
     */
    public static byte[] readFile(final File ressource, final int maxSize) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(ressource);
            return IO.readStream(maxSize, fis);
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (final Throwable e) {
            }
        }
    }

    public static String readFileToString(final File file) throws IOException {
        return IO.importFileToString(file, -1);
    }

    public static String readFileToTrimmedString(final File file) throws IOException {
        final String ret = readFileToString(file);
        if (ret != null) {
            return ret.trim();
        }
        return null;
    }

    public static String readInputStreamToString(final InputStream fis) throws UnsupportedEncodingException, IOException {
        BufferedReader f = null;
        try {
            f = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            String line;
            final StringBuilder ret = new StringBuilder();
            final String sep = System.getProperty("line.separator");
            while ((line = f.readLine()) != null) {
                if (ret.length() > 0) {
                    ret.append(sep);
                } else if (line.startsWith("\uFEFF")) {
                    /*
                     * Workaround for this bug: http://bugs.sun.com/view_bug.do?bug_id=4508058
                     * http://bugs.sun.com/view_bug.do?bug_id=6378911
                     */
                    line = line.substring(1);
                }
                ret.append(line);
            }
            return ret.toString();
        } catch (final IOException e) {
            if (IO.ERROR_HANDLER != null) {
                IO.ERROR_HANDLER.onReadStreamException(e, fis);
            }
            throw e;
        } catch (final RuntimeException e) {
            if (IO.ERROR_HANDLER != null) {
                IO.ERROR_HANDLER.onReadStreamException(e, fis);
            }
            throw e;
        } catch (final Error e) {
            if (IO.ERROR_HANDLER != null) {
                IO.ERROR_HANDLER.onReadStreamException(e, fis);
            }
            throw e;
        } finally {
            try {
                f.close();
            } catch (final Throwable e) {
            }
        }
    }

    public static String readLine(final BufferedInputStream is, final byte[] array) throws IOException {
        Arrays.fill(array, 0, array.length, (byte) 0);
        int read = 0;
        int total = 0;
        int totalString = 0;
        boolean nextLineReached = false;
        while (true) {
            read = is.read();
            if (read == -1 && total == 0) {
                /* EOS */
                return null;
            }
            if (read == 13 || read == 10) {
                /* line break found, mark in inputstream */
                nextLineReached = true;
                is.mark(1024);
            } else if (nextLineReached) {
                /* new text found */
                is.reset();
                total--;
                break;
            } else if (total < array.length) {
                /* only write to outputstream if maxlength not reached yet */
                array[totalString++] = (byte) read;
            }
            total++;
        }
        return new String(array, 0, totalString, "UTF-8");
    }

    public static byte[] readStream(final int maxSize, final InputStream input) throws IOException {
        if (maxSize > 0) {
            return IO.readStream(maxSize, input, new ByteArrayOutputStream(maxSize));
        } else {
            return IO.readStream(maxSize, input, new ByteArrayOutputStream());
        }
    }

    public static byte[] readStream(final int maxSize, final InputStream input, final ByteArrayOutputStream baos) throws IOException {
        return IO.readStream(maxSize, input, baos, true);
    }

    public static byte[] readStream(final int maxSize, final InputStream input, final ByteArrayOutputStream baos, boolean closeInput) throws IOException {
        IO.readStreamToOutputStream(maxSize, input, baos, closeInput);
        return baos.toByteArray();
    }

    public static void readStreamToOutputStream(final int maxSize, final InputStream input, final OutputStream baos, boolean closeInput) throws IOException, Error {
        try {
            final byte[] buffer = new byte[32767];
            int len;
            if (maxSize > 0) {
                int done = 0;
                while (done < maxSize && (len = input.read(buffer, 0, Math.min(buffer.length, maxSize - done))) != -1) {
                    if (len > 0) {
                        baos.write(buffer, 0, len);
                        done += len;
                    }
                }
            } else {
                while ((len = input.read(buffer)) != -1) {
                    if (len > 0) {
                        baos.write(buffer, 0, len);
                    }
                }
            }
        } catch (final IOException e) {
            if (IO.ERROR_HANDLER != null) {
                IO.ERROR_HANDLER.onReadStreamException(e, input);
            }
            throw e;
        } catch (final RuntimeException e) {
            if (IO.ERROR_HANDLER != null) {
                IO.ERROR_HANDLER.onReadStreamException(e, input);
            }
            throw e;
        } catch (final Error e) {
            if (IO.ERROR_HANDLER != null) {
                IO.ERROR_HANDLER.onReadStreamException(e, input);
            }
            throw e;
        } finally {
            if (closeInput) {
                try {
                    input.close();
                } catch (final Exception e) {
                }
            }
        }
    }

    /**
     * @param f
     * @return
     * @throws IOException
     */
    public static byte[] readURL(final URL f) throws IOException {
        // TODO Auto-generated method stub
        return IO.readURL(f, -1);
    }

    /**
     * @param url
     * @param maxSize
     * @return
     * @throws IOException
     */
    public static byte[] readURL(final URL url, final int maxSize) throws IOException {
        InputStream input = null;
        try {
            input = URLStream.openStream(url);
            return IO.readStream(maxSize, input);
        } finally {
            try {
                input.close();
            } catch (final Throwable e) {
            }
        }
    }

    /**
     * @param ressourceURL
     * @return
     * @throws IOException
     * @throws UnsupportedEncodingException
     */
    public static String readURLToString(final URL ressourceURL) throws IOException {
        InputStream fis = null;
        try {
            fis = URLStream.openStream(ressourceURL);
            return IO.readInputStreamToString(fis);
        } finally {
            try {
                fis.close();
            } catch (final Throwable e) {
            }
        }
    }

    /**
     * @param file
     * @param bytes
     * @param none
     * @throws IOException
     */
    public static void secureWrite(final File file, final byte[] bytes) throws IOException {
        IO.secureWrite(file, bytes, SYNC.META_AND_DATA);
    }

    public static void secureWrite(final File file, final byte[] bytes, final SYNC sync) throws IOException {
        final File bac = new File(file.getAbsolutePath() + ".bac");
        if (file.getParentFile().exists() == false) {
            file.getParentFile().mkdirs();
        }
        if (bac.exists() && bac.delete() == false) {
            throw new IOException("could not remove " + bac);
        }
        boolean deleteFile = true;
        try {
            IO.writeToFile(bac, bytes, sync);
            if (file.exists() && file.delete() == false) {
                throw new IOException("could not remove " + file);
            }
            if (!bac.renameTo(file)) {
                throw new IOException("COuld not rename " + bac + " to " + file);
            }
            deleteFile = false;
        } finally {
            if (deleteFile) {
                bac.delete();
            }
        }
    }

    /**
     * @param latestTimestampFile
     * @param serializeToJson
     * @param none
     * @throws IOException
     * @throws UnsupportedEncodingException
     */
    public static void secureWrite(final File file, final String utf8String, final SYNC sync) throws UnsupportedEncodingException, IOException {
        IO.secureWrite(file, utf8String.getBytes("UTF-8"), sync);
    }

    /**
     * Want to get informed in case of any io problems, set this handler
     *
     * @param handler
     */
    public static void setErrorHandler(final IOErrorHandler handler) {
        IO.ERROR_HANDLER = handler;
    }

    public static void writeStringToFile(final File file, final String string) throws IOException {
        IO.writeStringToFile(file, string, false, SYNC.META_AND_DATA);
    }

    public static void writeStringToFile(final File file, final String string, final boolean append) throws IOException {
        IO.writeStringToFile(file, string, append, SYNC.META_AND_DATA);
    }

    public static void writeStringToFile(final File file, final String string, final boolean append, final SYNC sync) throws IOException {
        try {
            if (file == null) {
                throw new IllegalArgumentException("File is null.");
            }
            if (file.exists() && !append) {
                throw new IllegalArgumentException("File already exists: " + file);
            }
            file.createNewFile();
            if (!file.isFile()) {
                throw new IllegalArgumentException("Is not a file: " + file);
            }
            if (!file.canWrite()) {
                throw new IllegalArgumentException("Cannot write to file: " + file);
            }
            FileOutputStream fos = null;
            Writer output = null;
            boolean deleteFile = true;
            try {
                output = new BufferedWriter(new OutputStreamWriter(fos = new FileOutputStream(file, append), "UTF-8"));
                output.write(string);
                output.flush();
                if (sync != null) {
                    switch (sync) {
                    case DATA:
                        fos.getChannel().force(false);
                        break;
                    case META_AND_DATA:
                        fos.getChannel().force(true);
                        break;
                    default:
                        break;
                    }
                }
                deleteFile = false;
            } finally {
                try {
                    output.flush();
                } catch (final Throwable e) {
                }
                try {
                    output.close();
                } catch (final Throwable e) {
                }
                try {
                    fos.close();
                } catch (final Throwable e) {
                }
                if (deleteFile) {
                    file.delete();
                }
            }
        } catch (final IOException e) {
            if (IO.ERROR_HANDLER != null) {
                IO.ERROR_HANDLER.onWriteException(e, file, string.getBytes());
            }
            throw e;
        } catch (final RuntimeException e) {
            if (IO.ERROR_HANDLER != null) {
                IO.ERROR_HANDLER.onWriteException(e, file, string.getBytes());
            }
            throw e;
        } catch (final Error e) {
            if (IO.ERROR_HANDLER != null) {
                IO.ERROR_HANDLER.onWriteException(e, file, string.getBytes());
            }
            throw e;
        }
    }

    public static void writeToFile(final File file, final byte[] data) throws IOException {
        IO.writeToFile(file, data, SYNC.META_AND_DATA);
    }

    public static void writeToFile(final File file, final byte[] data, final SYNC sync) throws IOException {
        try {
            if (file == null) {
                throw new IllegalArgumentException("File is null.");
            }
            if (file.exists()) {
                throw new IllegalArgumentException("File already exists: " + file);
            }
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw e;
            }
            if (!file.isFile()) {
                throw new IllegalArgumentException("Is not a file: " + file);
            }
            if (!file.canWrite()) {
                throw new IllegalArgumentException("Cannot write to file: " + file);
            }
            FileOutputStream out = null;
            boolean deleteFile = true;
            try {
                if (false) {
                    new WTFException("Write to File:" + file).printStackTrace();
                }
                out = new FileOutputStream(file);
                out.write(data);
                out.flush();
                if (sync != null) {
                    switch (sync) {
                    case DATA:
                        out.getChannel().force(false);
                        break;
                    case META_AND_DATA:
                        out.getChannel().force(true);
                        break;
                    default:
                        break;
                    }
                }
                deleteFile = false;
            } finally {
                try {
                    out.close();
                } catch (final Throwable e) {
                }
                if (deleteFile) {
                    file.delete();
                }
            }
        } catch (final IOException e) {
            if (IO.ERROR_HANDLER != null) {
                IO.ERROR_HANDLER.onWriteException(e, file, data);
            }
            throw e;
        } catch (final RuntimeException e) {
            if (IO.ERROR_HANDLER != null) {
                IO.ERROR_HANDLER.onWriteException(e, file, data);
            }
            throw e;
        } catch (final Error e) {
            if (IO.ERROR_HANDLER != null) {
                IO.ERROR_HANDLER.onWriteException(e, file, data);
            }
            throw e;
        }
    }
}
