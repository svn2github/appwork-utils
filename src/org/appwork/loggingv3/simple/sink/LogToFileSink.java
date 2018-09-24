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
package org.appwork.loggingv3.simple.sink;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.appwork.loggingv3.simple.LogRecord2;

/**
 * @author Thomas
 * @date 19.09.2018
 *
 */
public class LogToFileSink extends AbstractSink {
    private String filepattern;
    private File   logRoot;
    private File   logFolder;
    private String timeTag;

    /**
     * @param string
     */
    public LogToFileSink(File root, String filepattern, int zipLevel, CompressionMode compressMode) {
        logRoot = root;
        logRoot.mkdirs();
        timeTag = createTimeTag();
        if (!filepattern.contains("\\d")) {
            filepattern += ".\\d";
        }
        this.filepattern = filepattern;
        this.zipLevel = zipLevel;
        this.compressMode = compressMode;
        Runtime.getRuntime().addShutdownHook(new Thread("ShutdownHook: Logger") {
            /*
             * (non-Javadoc)
             *
             * @see java.lang.Thread#run()
             */
            @Override
            public void run() {
                onShutdown();
            }
        });
    }

    public File getLogRoot() {
        return logRoot;
    }

    public File getLogFolder() {
        return logFolder;
    }

    /**
     *
     */
    protected void onShutdown() {
        synchronized (this) {
            try {
                if (zipOut != null) {
                    fos.flush();
                    zipOut.closeEntry();
                    zipOut.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (Throwable e) {
            }
        }
    }

    protected String createTimeTag() {
        return new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(new Date());
    }

    private File currentFile = null;

    public File getCurrentFile() {
        return currentFile;
    }

    private BufferedWriter  fos;
    private int             zipLevel     = 3;
    private CompressionMode compressMode = CompressionMode.ZIP_FOLDER;

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.loggingv3.simple.sink.Sink#publish(java.lang.String)
     */
    @Override
    public void publish(LogRecord2 record) {
        synchronized (this) {
            if (counter == null || counter.written >= getMaxFileSize()) {
                nextFile();
            }
            try {
                fos.write(format(record) + "\r\n");
                fos.flush();
            } catch (IOException e) {
                // TODO: WAS JETZT?
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    int                          files = 0;
    private ZipOutputStream      zipOut;
    private CountingOutputStream counter;

    /**
     *
     */
    private void nextFile() {
        try {
            if (logFolder == null) {
                int i = 1;
                while (logFolder == null || logFolder.exists()) {
                    logFolder = new File(logRoot, "logs_" + timeTag + "_" + (i++));
                }
            }
            files++;
            File file = null;
            switch (compressMode) {
            case ZIP_FOLDER:
                if (zipOut == null) {
                    logFolder.getParentFile().mkdirs();
                    zipOut = new ZipOutputStream(new FileOutputStream(currentFile = new File(logFolder.getAbsolutePath() + ".zip")));
                    fos = new BufferedWriter(new OutputStreamWriter(counter = new CountingOutputStream(zipOut), "UTF-8"));
                    zipOut.setLevel(zipLevel);
                } else {
                    fos.flush();
                    zipOut.closeEntry();
                    counter.written = 0;
                }
                zipOut.putNextEntry(new ZipEntry(filepattern.replace("\\d", createFileIndexTag(files))));
                break;
            case ZIP_FILES:
                try {
                    if (zipOut != null) {
                        fos.flush();
                        zipOut.closeEntry();
                        zipOut.close();
                        fos.close();
                        zipOut = null;
                        fos = null;
                    }
                } catch (IOException e1) { // TODO: WAS JETZT?
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                while (file == null || file.exists()) {
                    file = new File(logFolder, filepattern.replace("\\d", createFileIndexTag(files)));
                    if (file.exists()) {
                        files++;
                    }
                }
                file.getParentFile().mkdirs();
                fos = new BufferedWriter(new OutputStreamWriter(counter = new CountingOutputStream(zipOut = new ZipOutputStream(new FileOutputStream(file.getAbsolutePath() + ".zip"))), "UTF-8"));
                zipOut.putNextEntry(new ZipEntry(file.getName()));
                this.currentFile = file;
                break;
            case NONE:
                if (fos != null) {
                    fos.close();
                    fos = null;
                }
                while (file == null || file.exists()) {
                    file = new File(logFolder, filepattern.replace("\\d", createFileIndexTag(files)));
                    if (file.exists()) {
                        files++;
                    }
                }
                file.getParentFile().mkdirs();
                fos = new BufferedWriter(new OutputStreamWriter(counter = new CountingOutputStream(new FileOutputStream(file)), "UTF-8"));
                this.currentFile = file;
                break;
            }
        } catch (IOException e) {
            // TODO: WAS JETZT?
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    class CountingOutputStream extends OutputStream {
        private final OutputStream os;
        private volatile long      written = 0;

        public CountingOutputStream(final OutputStream os) {
            this.os = os;
        }

        @Override
        public void close() throws IOException {
            this.os.close();
        }

        @Override
        public void flush() throws IOException {
            this.os.flush();
        }

        @Override
        public void write(final byte b[]) throws IOException {
            this.os.write(b);
            this.written += b.length;
        }

        public void setTransferedBytes(long written) {
            if (written >= 0) {
                this.written = written;
            }
        }

        @Override
        public void write(final byte b[], final int off, final int len) throws IOException {
            this.os.write(b, off, len);
            this.written += len;
        }

        @Override
        public void write(final int b) throws IOException {
            this.os.write(b);
            this.written++;
        }
    }

    /**
     * @param files2
     * @return
     */
    protected String createFileIndexTag(int files2) {
        String ret = String.valueOf(files2);
        while (ret.length() < 3) {
            ret = "0" + ret;
        }
        return ret;
    }

    private int maxFileSize = 200 * 1024;

    public int getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(int maxFileSize) {
        this.maxFileSize = maxFileSize;
    }
}
