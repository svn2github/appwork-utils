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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.HashSet;

import org.appwork.utils.logging2.extmanager.LoggerFactory;

/**
 * @author $Author: unknown$
 *
 */
public class Exceptions {
    /**
     *
     * returns true of e instanceof class1 or any causes of e instanceof class1
     *
     * @param e
     * @param class1
     * @return
     */
    public static boolean containsInstanceOf(final Throwable e, final Class<? extends Throwable>... classes) {
        if (classes != null) {
            for (final Class<? extends Throwable> class1 : classes) {
                if (Exceptions.getInstanceof(e, class1) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param <T>
     * @param e
     * @param class1
     * @return
     */
    public static <T extends Throwable> T getInstanceof(Throwable e, final Class<T> class1) {
        if (e == null || class1 == null) {
            return null;
        }
        Throwable cause;
        final HashSet<Throwable> dupe = new HashSet<Throwable>();
        while (true) {
            if (class1.isAssignableFrom(e.getClass())) {
                return (T) e;
            }
            cause = e.getCause();
            if (cause == null || !dupe.add(cause)) {
                return null;
            }
            e = cause;
        }
    }

    /**
     * prints the StrackTrace into given StringBuffer
     *
     */
    public static String getStackTrace(final StringBuilder sb, final Throwable thrown) {
        final Writer sw = new Writer() {
            final int startPos;
            {
                this.lock = sb;
                this.startPos = sb.length();
            }

            @Override
            public Writer append(final char c) throws IOException {
                this.write(c);
                return this;
            }

            @Override
            public Writer append(final CharSequence csq) throws IOException {
                if (csq == null) {
                    this.write("null");
                } else {
                    this.write(csq.toString());
                }
                return this;
            }

            @Override
            public Writer append(final CharSequence csq, final int start, final int end) throws IOException {
                final CharSequence cs = csq == null ? "null" : csq;
                this.write(cs.subSequence(start, end).toString());
                return this;
            }

            @Override
            public void close() throws IOException {
            }

            @Override
            public void flush() throws IOException {
            }

            @Override
            public String toString() {
                return sb.substring(this.startPos);
            }

            @Override
            public void write(final char[] cbuf) throws IOException {
                sb.append(cbuf);
            }

            @Override
            public void write(final char[] cbuf, final int off, final int len) throws IOException {
                if (off < 0 || off > cbuf.length || len < 0 || off + len > cbuf.length || off + len < 0) {
                    throw new IndexOutOfBoundsException();
                } else if (len == 0) {
                    return;
                }
                sb.append(cbuf, off, len);
            }

            @Override
            public void write(final int c) throws IOException {
                sb.append(c);
            }

            @Override
            public void write(final String str) throws IOException {
                sb.append(str);
            }

            @Override
            public void write(final String str, final int off, final int len) {
                sb.append(str.substring(off, off + len));
            }
        };
        final PrintWriter pw = new PrintWriter(sw);
        thrown.printStackTrace(pw);
        pw.close();
        return "";
    }

    /**
     * returns the Exceptions Stacktrace as String
     *
     * @param thrown
     * @return
     */
    public static String getStackTrace(final Throwable thrown) {
        if (thrown == null) {
            return "";
        }
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        thrown.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    private static Method addSuppressedMethod = null;
    static {
        try {
            addSuppressedMethod = Throwable.class.getMethod("addSuppressed", Throwable.class);
        } catch (NoSuchMethodException ex) {
            // This is the case for JDK < 7
        } catch (SecurityException ex) {
            LoggerFactory.getDefaultLogger().log(ex);
        }
    }

    /**
     * @param err
     * @param e
     */
    public static void addSuppressed(Throwable throwing, Throwable additional) {
        if (addSuppressedMethod == null) {
            // unsupported JVM - ignore
            return;
        }
        try {
            addSuppressedMethod.invoke(throwing, additional);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}
