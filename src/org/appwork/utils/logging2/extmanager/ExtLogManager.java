/**
 * 
 * ====================================================================================================================================================
 * 	    "MyJDownloader Client" License
 * 	    The "MyJDownloader Client" will be called [The Product] from now on.
 * ====================================================================================================================================================
 * 	    Copyright (c) 2009-2015, AppWork GmbH <e-mail@appwork.org>
 * 	    Schwabacher Straße 117
 * 	    90763 Fürth
 * 	    Germany   
 * === Preamble ===
 * 	This license establishes the terms under which the [The Product] Source Code & Binary files may be used, copied, modified, distributed, and/or redistributed.
 * 	The intent is that the AppWork GmbH is able to provide  their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 * 	These terms apply to all files that have the [The Product] License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 * 	
 * === 3rd Party Licences ===
 * 	Some parts of the [The Product] use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 * 	to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header. 	
 * 	
 * === Definition: Commercial Usage ===
 * 	If anybody or any organization is generating income (directly or indirectly) by using [The Product] or if there's as much as a 
 * 	sniff of commercial interest or aspect in what you are doing, we consider this as a commercial usage. If you are unsure whether your use-case is commercial or not, consider it as commercial.
 * === Dual Licensing ===
 * === Commercial Usage ===
 * 	If you want to use [The Product] in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 * 	Contact AppWork for further details: e-mail@appwork.org
 * === Non-Commercial Usage ===
 * 	If there is no commercial usage (see definition above), you may use [The Product] under the terms of the 
 * 	"GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 * 	
 * 	If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.utils.logging2.extmanager;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;

public class ExtLogManager extends LogManager {
    public static String[] WHITELIST     = new String[] { "org.fourthline", "com.mongo" };
    public static String[] BLACKLIST     = new String[] { "com.mongodb.driver.cluster", "org.fourthline", "org.fourthline.cling.registry.Registry", "org.fourthline.cling.model.message.header", "org.fourthline.cling.model.message.UpnpHeaders", "org.fourthline.cling.transport" };

    private LoggerFactory  loggerFactory = null;

    public LoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    public void setLoggerFactory(final LoggerFactory LoggerFactory) {
        this.loggerFactory = LoggerFactory;
    }

    public boolean addLogger(final Logger logger) {
        String name = logger.getName();

        if ("sun.util.logging.resources.logging".equals(logger.getResourceBundleName())) {
            if (loggerFactory != null) {

                for (final String w : WHITELIST) {
                    if (name.startsWith(w)) {
                        System.out.println("Redirect Logger (WL): " + name);
                        return false;

                    }
                }

            }

        }
        if (!(logger instanceof LogSource)) {

            // adds a handler to system loggers.
            // this handler delegates the output to our logging system
            logger.setLevel(Level.INFO);
            logger.addHandler(new Handler() {
                {
                    setLevel(Level.INFO);
                }
                private LogSource del;

                @Override
                public void publish(LogRecord record) {
                    ensureLogger(logger);
                    if (del != null) {
                        del.log(record);
                    } else {
                        System.out.println(record.getMessage());
                    }
                }

                /**
                 * @param logger
                 */
                protected void ensureLogger(final Logger logger) {
                    if (del == null && loggerFactory != null) {
                        String name = logger.getName();
                        if (StringUtils.isEmpty(name)) {
                            name = logger.toString();
                        }
                        del = loggerFactory.getLogger(name);
                    }
                }

                @Override
                public void flush() {
                    // TODO Auto-generated method stub

                }

                @Override
                public void close() throws SecurityException {

                }
            });
        }
        boolean ret = super.addLogger(logger);
        if (ret) {
            System.out.println("Created System Logger " + name + " " + logger);
        }
        return ret;
    }

    @Override
    public synchronized Logger getLogger(final String name) {

        if (loggerFactory != null) {
            for (final String b : BLACKLIST) {
                if (name.startsWith(b)) {
                    System.out.println("Ignored (BL): " + name);
                    return super.getLogger(name);
                }
            }

            for (final String w : WHITELIST) {
                if (name.startsWith(w)) {
                    System.out.println("Redirect Logger (WL): " + name);
                    return loggerFactory.getLogger(name);

                }
            }

        }
        System.out.println("Ignored: " + name);
        return super.getLogger(name);
    }

}
