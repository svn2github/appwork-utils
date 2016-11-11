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
package org.appwork.utils.processes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.appwork.utils.os.CrossSystem;

public class ProcessBuilderFactory {
    private static String CONSOLE_CODEPAGE = null;

    public static ProcessOutput runCommand(final java.util.List<String> commands) throws IOException, InterruptedException {
        return ProcessBuilderFactory.runCommand(ProcessBuilderFactory.create(commands));
    }

    public static ProcessOutput runCommand(String... commands) throws IOException, InterruptedException {
        return ProcessBuilderFactory.runCommand(ProcessBuilderFactory.create(commands));
    }

    public static void readStreamToOutputStream(final InputStream input, final OutputStream baos) throws IOException, Error {
        try {
            final byte[] buffer = new byte[1024];
            int len = 0;
            boolean wait = false;
            while (true) {
                synchronized (input) {
                    len = input.read(buffer);
                }
                if (len == -1) {
                    break;
                } else if (len > 0) {
                    wait = false;
                    baos.write(buffer, 0, len);
                    // System.out.println("> " + new String(buffer, 0, len,
                    // "UTF-8"));
                } else {
                    try {
                        if (wait == false) {
                            wait = true;
                            // System.out.println("Reader Wait");
                        }
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        throw new IOException(e);
                    }
                }
            }
        } catch (final IOException e) {
            throw e;
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Error e) {
            throw e;
        } finally {
            try {
                input.close();
            } catch (final Exception e) {
            }
            try {
                baos.close();
            } catch (final Throwable e) {
            }
        }
    }

    public static ProcessOutput runCommand(ProcessBuilder pb) throws IOException, InterruptedException {
        final ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        final ByteArrayOutputStream sdtStream = new ByteArrayOutputStream();
        int exitCode = runCommand(pb, errorStream, sdtStream);
        return new ProcessOutput(exitCode, sdtStream.toByteArray(), errorStream.toByteArray());
    }

    public static int runCommand(ProcessBuilder pb, final OutputStream errorStream, final OutputStream sdtStream) throws IOException, InterruptedException {
        return runCommand(pb, errorStream, sdtStream, null);
    }

    /**
     * s
     *
     * @param create
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static int runCommand(ProcessBuilder pb, final OutputStream errorStream, final OutputStream sdtStream, final ProcessHandler osHandler) throws IOException, InterruptedException {
        // System.out.println("Start Process " + pb.command());
        //
        final Process process = pb.start();
        final AtomicReference<IOException> exception = new AtomicReference<IOException>();
        if (osHandler == null || !osHandler.setProcess(process)) {
            process.getOutputStream().close();
        }
        final Thread reader1 = new Thread("Process-Reader-Std") {
            @Override
            public void run() {
                try {
                    // System.out.println("Start Process-Reader-Std");
                    ProcessBuilderFactory.readStreamToOutputStream(process.getInputStream(), sdtStream);
                } catch (IOException e) {
                    exception.compareAndSet(null, e);
                    e.printStackTrace();
                    try {
                        process.exitValue();
                    } catch (IllegalThreadStateException e2) {
                        // System.out.println("Process still running. Killing it");
                        process.destroy();
                    }
                } finally {
                    // System.out.println("Stop Process-Reader-Std");
                }
            }
        };
        // TODO check if pb.redirectErrorStream()
        final Thread reader2 = new Thread("Process-Reader-Error") {
            @Override
            public void run() {
                try {
                    // System.out.println("Start Process-Reader-Error");
                    ProcessBuilderFactory.readStreamToOutputStream(process.getErrorStream(), errorStream);
                } catch (IOException e) {
                    exception.compareAndSet(null, e);
                    e.printStackTrace();
                    try {
                        process.exitValue();
                    } catch (IllegalThreadStateException e2) {
                        // System.out.println("Process still running. Killing it");
                        process.destroy();
                    }
                } finally {
                    // System.out.println("Stop Process-Reader-Error");
                }
            }
        };
        if (CrossSystem.isWindows()) {
            reader1.setPriority(Thread.NORM_PRIORITY + 1);
            reader2.setPriority(Thread.NORM_PRIORITY + 1);
        }
        reader1.setDaemon(true);
        reader2.setDaemon(true);
        reader1.start();
        reader2.start();
        // System.out.println("Wait for Process");
        final int returnCode = process.waitFor();
        // System.out.println("Process returned: " + returnCode);
        if (reader1.isAlive()) {
            // System.out.println("Wait for Process-Reader-Std");
            reader1.join(5000);
            if (reader1.isAlive()) {
                // System.out.println("Process-Reader-Std still alive!");
                reader1.interrupt();
            }
        }
        if (reader2.isAlive()) {
            // System.out.println("Wait fo Process-Reader-Error");
            reader2.join(5000);
            if (reader2.isAlive()) {
                // System.out.println("Process-Reader-Error still alive!");
                reader2.interrupt();
            }
        }
        return returnCode;
    }

    public static ProcessBuilder create(final java.util.List<String> splitCommandString) {
        return ProcessBuilderFactory.create(splitCommandString.toArray(new String[] {}));
    }

    public static ProcessBuilder create(final String... tiny) {
        return new ProcessBuilder(ProcessBuilderFactory.escape(tiny));
    }

    private static String[] escape(final String[] tiny) {
        if (CrossSystem.isWindows() || CrossSystem.isOS2()) {
            // The windows processbuilder throws exceptions if a arguments
            // starts
            // with ", but does not end with " or vice versa
            final String[] ret = new String[tiny.length];
            //
            for (int i = 0; i < ret.length; i++) {
                if (tiny[i].startsWith("\"") && !tiny[i].endsWith("\"")) {
                    ret[i] = "\"" + tiny[i].replace("\"", "\\\"") + "\"";
                } else if (!tiny[i].startsWith("\"") && tiny[i].endsWith("\"")) {
                    ret[i] = "\"" + tiny[i].replace("\"", "\\\"") + "\"";
                } else {
                    ret[i] = tiny[i];
                }
            }
            return ret;
        } else {
            return tiny;
        }
    }

    /**
     * @return
     */
    public static String getConsoleCodepage() {
        if (StringUtils.isEmpty(CONSOLE_CODEPAGE)) {
            switch (CrossSystem.getOSFamily()) {
            case WINDOWS:
                try {
                    String result = runCommand("cmd", "/c", "chcp").getStdOutString("ASCII");
                    result = new Regex(result, ":\\s*(\\d+)").getMatch(0);
                    if (StringUtils.isNotEmpty(result)) {
                        final String cp = "cp" + result.trim();
                        // https://msdn.microsoft.com/en-us/library/dd317756%28VS.85%29.aspx
                        if ("CP65001".equalsIgnoreCase(cp)) {
                            CONSOLE_CODEPAGE = "UTF-8";
                        } else {
                            CONSOLE_CODEPAGE = cp;
                        }
                    }
                } catch (Throwable e) {
                    LoggerFactory.getDefaultLogger().log(e);
                }
                break;
            default:
                break;
            }
            if (StringUtils.isEmpty(CONSOLE_CODEPAGE)) {
                CONSOLE_CODEPAGE = Charset.defaultCharset().displayName();
            }
        }
        return CONSOLE_CODEPAGE;
    }
}
