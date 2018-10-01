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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.appwork.loggingv3.simple.Formatter;
import org.appwork.loggingv3.simple.LogRecord2;
import org.appwork.utils.StringUtils;

/**
 * @author Thomas
 * @date 21.09.2018
 *
 */
public class SimpleFormatter implements Formatter {
    private final SimpleDateFormat longTimestamp = new SimpleDateFormat("dd.MM.yy HH:mm:ss.SSS");

    class IntByReference {
        /**
         *
         */
        public IntByReference(int i) {
            this.i = i;
        }

        private int i;
    }

    private IntByReference threadID              = new IntByReference(3);
    private IntByReference threadName            = new IntByReference(5);
    private IntByReference timestamp             = new IntByReference(5);
    private IntByReference thrownAt              = new IntByReference(5);
    private int            maxThreadNameLength   = 30;
    private int            maxSourceStringLength = 90;

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.loggingv3.simple.Formatter#format(org.appwork.loggingv3.simple.LogRecord2)
     */
    @Override
    public String format(LogRecord2 record) {
        String message = record.message == null ? "" : record.message;
        StackTraceElement source = record.getThrownAt();
        String sourceString = source.getClassName();
        if (StringUtils.isNotEmpty(source.getFileName()) && source.getLineNumber() >= 0) {
            int li = sourceString.lastIndexOf(".");
            if (li > 0) {
                sourceString = sourceString.substring(0, li) + " (" + source.getFileName() + ":" + source.getLineNumber() + ")";
            } else {
                sourceString = "DEBUG" + sourceString;
            }
        }
        sourceString += "." + source.getMethodName();
        String pre = "--" + fillPre(String.valueOf(record.thread.getId()), " ", threadID) + fillPost("(" + abbr(record.thread.getName(), maxThreadNameLength) + ")", " ", threadName) + " " + fillPre(longTimestamp.format(new Date(record.timestamp)), " ", timestamp) + " - " + fillPost("" + abbr(String.valueOf(sourceString) + "", maxSourceStringLength), " ", thrownAt) + " > ";
        int line = 0;
        int preLength = pre.length();
        String[] lines = message.split("[\r\n]+");
        for (String s : lines) {
            if (s.trim().length() > 0) {
                if (line == 0) {
                    pre += s;
                } else {
                    pre += "\r\n" + fillPre("", " ", preLength) + s;
                }
                line++;
            }
        }
        return pre;
    }

    /**
     * @param name
     * @param i
     * @return
     */
    private String abbr(String name, int i) {
        if (name.length() <= i + 3) {
            return name;
        }
        return "..." + name.substring(name.length() - i);
    }

    public static String fillPre(String string, final String filler, IntByReference max) {
        string = string == null ? "null" : string;
        max.i = Math.max(max.i, string.length());
        return fillPre(string, filler, max.i);
    }

    /**
     * @param string
     * @param filler
     * @param i
     * @return
     */
    private static String fillPre(String string, String filler, int i) {
        string = string == null ? "null" : string;
        if (string.length() == i) {
            return string;
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(string);
        while (sb.length() < i) {
            sb.insert(0, filler);
        }
        return sb.toString();
    }

    public static String fillPost(String string, final String filler, IntByReference max) {
        string = string == null ? "null" : string;
        max.i = Math.max(max.i, string.length());
        if (string.length() == max.i) {
            return string;
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(string);
        while (sb.length() < max.i) {
            sb.append(filler);
        }
        return sb.toString();
    }
}
