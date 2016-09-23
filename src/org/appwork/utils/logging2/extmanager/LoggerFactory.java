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
        org.appwork.utils.Application.warnInit();
        try {
            // the logmanager should not be initialized here. so setting the
            // property should tell the logmanager to init a ExtLogManager
            // instance.
            // TODO: Java 1.9, this will initialize ExtLogManager via AppClassLoader!
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
            if (lm != null) {
                System.err.println("Logmanager: " + lm + "|" + lm.getClass().getClassLoader());
            } else {
                System.err.println("Logmanager: null");
            }
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

    private LogInterface defaultLogInterface = null;

    public LogInterface getDefaultLogInterface() {
        return defaultLogInterface;
    }

    public void setDefaultLogInterface(LogInterface defaultLogInterface) {
        this.defaultLogInterface = defaultLogInterface;
    }

    private LogSourceProvider delegate;

    public LoggerFactory() {
        super(System.currentTimeMillis());
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
            if ("true".equalsIgnoreCase(System.getProperty(LOG_NO_CONSOLE))) {
                return new DevNullLogger();
            }
            return new ConsoleLogImpl();
        }
        synchronized (INSTANCE) {
            if (INSTANCE.defaultLogInterface == null) {
                INSTANCE.defaultLogInterface = INSTANCE.getLogger("Log.L");
            }
        }
        return INSTANCE.defaultLogInterface;
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
