package org.appwork.utils.processes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.utils.os.CrossSystem;

public class ProcessBuilderFactory {

    public static ProcessOutput runCommand(final java.util.List<String> commands) throws IOException, InterruptedException {

        return ProcessBuilderFactory.runCommand(ProcessBuilderFactory.create(commands));
    }

    public static ProcessOutput runCommand(String... commands) throws IOException, InterruptedException {

        return ProcessBuilderFactory.runCommand(ProcessBuilderFactory.create(commands));
    }

    public static void readStreamToOutputStream(final InputStream input, final OutputStream baos) throws IOException, Error {
        try {
            final byte[] buffer = new byte[32767];
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

    /**
     * s
     * 
     * @param create
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static int runCommand(ProcessBuilder pb, final OutputStream errorStream, final OutputStream sdtStream) throws IOException, InterruptedException {
        // System.out.println("Start Process " + pb.command());

        //
        final Process process = pb.start();
        process.getOutputStream().close();

        final AtomicReference<IOException> exception = new AtomicReference<IOException>();
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
}
