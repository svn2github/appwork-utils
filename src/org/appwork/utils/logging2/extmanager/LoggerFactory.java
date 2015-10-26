/**
 * Copyright (c) 2009 - 2013 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 *
 * This file is part of org.appwork.utils.logging2.extmanager
 *
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.logging2.extmanager;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.appwork.utils.logging2.ConsoleLogImpl;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.logging2.LogSourceProvider;

/**
 * @author Thomas
 *
 */
public class LoggerFactory extends LogSourceProvider {

    private static LoggerFactory INSTANCE;
    static {

        try {
            // the logmanager should not be initialized here. so setting the
            // property should tell the logmanager to init a ExtLogManager
            // instance.
            System.setProperty("java.util.logging.manager", ExtLogManager.class.getName());
            LogManager man = java.util.logging.LogManager.getLogManager();
            // throws an exception if man is not instanceof ExtLogManager
            ((ExtLogManager) man).getClass();

            // The init order is important
            INSTANCE = new LoggerFactory();
            ((ExtLogManager) man).setLoggerFactory(INSTANCE);
        } catch (final Throwable e) {
            e.printStackTrace();
            final java.util.logging.LogManager lm = java.util.logging.LogManager.getLogManager();
            System.err.println("Logmanager: " + lm);
            try {
                if (lm != null) {
                    // seems like the logmanager has already been set, and is
                    // not of type ExtLogManager. try to fix this here
                    // we experiences this bug once on a mac system. may be
                    // caused by mac jvm, or the mac install4j launcher

                    // 12.11:
                    // a winxp user had this problem with install4j (exe4j) as
                    // well.
                    // seems like 4xeej sets a logger before our main is
                    // reached.
                    final Field field = java.util.logging.LogManager.class.getDeclaredField("manager");
                    field.setAccessible(true);
                    final ExtLogManager manager = new ExtLogManager();
                    INSTANCE = new LoggerFactory();
                    manager.setLoggerFactory(INSTANCE);
                    Field modifiersField = Field.class.getDeclaredField("modifiers");
                    modifiersField.setAccessible(true);
                    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

                    field.set(null, manager);

                    final Field rootLogger = java.util.logging.LogManager.class.getDeclaredField("rootLogger");
                    rootLogger.setAccessible(true);

                    modifiersField.setAccessible(true);
                    modifiersField.setInt(rootLogger, rootLogger.getModifiers() & ~Modifier.FINAL);
                    final Logger rootLoggerInstance = (Logger) rootLogger.get(lm);
                    rootLogger.set(manager, rootLoggerInstance);
                    manager.addLogger(rootLoggerInstance);

                    // Adding the global Logger. Doing so in the Logger.<clinit>
                    // would deadlock with the LogManager.<clinit>.

                    final Method setLogManager = Logger.class.getDeclaredMethod("setLogManager", new Class[] { java.util.logging.LogManager.class });
                    setLogManager.setAccessible(true);
                    setLogManager.invoke(Logger.global, manager);

                    final Enumeration<String> names = lm.getLoggerNames();
                    while (names.hasMoreElements()) {
                        manager.addLogger(lm.getLogger(names.nextElement()));

                    }
                }
            } catch (final Throwable e1) {
                e1.printStackTrace();
            }
            // catch (final IllegalAccessException e1) {
            // e1.printStackTrace();
            // }
        }

    }

    public static LoggerFactory I() {
        return INSTANCE;
    }

    private LogInterface      defaultLogger = new ConsoleLogImpl();
    private LogSourceProvider delegate;

    public LoggerFactory() {
        super(System.currentTimeMillis());
        // try {
        // Log.closeLogfile();
        // } catch (final Throwable e) {
        // }
        // try {
        // for (final Handler handler : org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().getHandlers()) {
        // org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().removeHandler(handler);
        // }
        // } catch (final Throwable e) {
        // }
        // org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().setUseParentHandlers(true);
        // org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().setLevel(Level.ALL);
        defaultLogger = getLogger("Log.L");
        // org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().addHandler(new Handler() {
        //
        // @Override
        // public void close() throws SecurityException {
        // }
        //
        // @Override
        // public void flush() {
        // }
        //
        // @Override
        // public void publish(final LogRecord record) {
        // final LogSource logger = defaultLogger;
        // logger.log(record);
        // }
        // });
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(final Thread t, final Throwable e) {
                final LogSource logger = getLogger("UncaughtExceptionHandler");
                logger.severe("Uncaught Exception in: " + t.getId() + "=" + t.getName());
                logger.log(e);
                logger.close();
            }
        });
    }

    /**
     * @return
     */
    public static LogInterface getDefaultLogger() {
        if (INSTANCE == null) {
            return new ConsoleLogImpl();
        }
        return INSTANCE.defaultLogger;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.utils.logging2.LogSourceProvider#getLogger(java.lang.String)
     */
    @Override
    public LogSource getLogger(String name) {
        if (delegate != null) {
            return delegate.getLogger(name);
        }
        return super.getLogger(name);
    }

    /**
     * @param logController
     */
    public void setDelegate(LogSourceProvider newLogController) {
        this.delegate = newLogController;

    }

    /**
     * @return
     */
    public static LoggerFactory getInstance() {

        return INSTANCE;
    }

    /**
     * @param name
     * @return
     */
    public static LogInterface get(String name) {
        if (INSTANCE == null) {
            return getDefaultLogger();
        }
        if (INSTANCE.delegate != null) {
            return INSTANCE.delegate.getLogger(name);
        }
        return INSTANCE.getLogger(name);
    }

    /**
     * @param logger
     * @param e
     */
    public static void log(LogSource logger, Throwable e) {
        if (logger == null) {
            return;
        }
        logger.log(e);

    }

}
