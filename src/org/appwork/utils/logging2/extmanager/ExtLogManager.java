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
