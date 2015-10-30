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
package org.appwork.utils.logging2;

import java.text.DateFormat;
import java.util.Date;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import org.appwork.utils.Exceptions;

public class LogSourceFormatter extends SimpleFormatter {

    private final Date       dat                    = new Date();
    private final DateFormat longTimestamp          = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);

    private int              lastThreadID;

    protected StringBuilder  formatterStringBuilder = null;

    @Override
    public synchronized String format(final LogRecord record) {
        StringBuilder sb = this.formatterStringBuilder;
        if (sb == null) {
            /*
             * create new local StringBuilder in case we don't have once set
             * externally
             */
            sb = new StringBuilder();
        }
        // Minimize memory allocations here.
        this.dat.setTime(record.getMillis());
        final String message = this.formatMessage(record);
        final int th = record.getThreadID();
        if (th != this.lastThreadID) {
            sb.append("------------------------Thread: ");
            sb.append(th);
            sb.append(":" + record.getLoggerName());
            sb.append("-----------------------\r\n");
        }
        this.lastThreadID = th;
        /* we have this line for easier logfile purifier :) */
        sb.append("--ID:" + th + "TS:" + record.getMillis() + "-");
        sb.append(this.longTimestamp.format(this.dat));
        sb.append(" - ");
        sb.append(" [");
        String tmp = null;
        if ((tmp = record.getSourceClassName()) != null) {
            sb.append(tmp);
        }
        if ((tmp = record.getSourceMethodName()) != null) {
            sb.append('(');
            sb.append(tmp);
            sb.append(')');
        }
        sb.append("] ");
        sb.append("-> ");
        sb.append(message);
        sb.append("\r\n");
        if (record.getThrown() != null) {
            Exceptions.getStackTrace(sb, record.getThrown());
            sb.append("\r\n");
        }
        if (this.formatterStringBuilder == sb) { return ""; }
        return sb.toString();
    }

    public StringBuilder getFormatterStringBuilder() {
        return this.formatterStringBuilder;
    }

    public void setFormatterStringBuilder(final StringBuilder formatterStringBuilder) {
        this.formatterStringBuilder = formatterStringBuilder;
    }
}