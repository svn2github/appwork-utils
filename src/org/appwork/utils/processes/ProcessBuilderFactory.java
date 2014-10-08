package org.appwork.utils.processes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.utils.IO;
import org.appwork.utils.os.CrossSystem;

public class ProcessBuilderFactory {

    public static ProcessOutput runCommand(final java.util.List<String> commands) throws IOException, InterruptedException {

        return runCommand(create(commands));
    }

    public static ProcessOutput runCommand(String... commands) throws IOException, InterruptedException {

        return runCommand(create(commands));
    }

    /**
     * @param create
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    private static ProcessOutput runCommand(ProcessBuilder pb) throws IOException, InterruptedException {
        final Process process = pb.start();
        final ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        final ByteArrayOutputStream sdtStream = new ByteArrayOutputStream();
        final AtomicReference<IOException> exception = new AtomicReference<IOException>();

        final Thread reader1 = new Thread("Process-Reader-Std") {
            public void run() {
                try {
                    IO.readStreamToOutputStream(-1, process.getInputStream(), sdtStream, false);
                } catch (IOException e) {
                    exception.compareAndSet(null, e);
                }
            }
        };

        final Thread reader2 = new Thread("Process-Reader-Error") {
            public void run() {
                try {
                    IO.readStreamToOutputStream(-1, process.getErrorStream(), errorStream, false);
                } catch (IOException e) {
                    exception.compareAndSet(null, e);
                }
            }
        };
        if (CrossSystem.isWindows()) {
            reader1.setPriority(Thread.NORM_PRIORITY + 1);
            reader2.setPriority(Thread.NORM_PRIORITY + 1);
        }
        reader1.start();
        reader2.start();
        reader1.join();
        reader2.join();
        return new ProcessOutput(process.waitFor(), sdtStream.toByteArray(), sdtStream.toByteArray());
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
