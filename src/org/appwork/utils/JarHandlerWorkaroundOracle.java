/**
 * Copyright (c) 2009 - 2015 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 *
 * This file is part of org.appwork.utils
 *
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author daniel
 *
 */
public class JarHandlerWorkaroundOracle {

    private static final AtomicBoolean INIT = new AtomicBoolean(false);

    // add custom jarHandler for http://bugs.java.com/view_bug.do?bug_id=6390779
    public static void init() {
        if (INIT.compareAndSet(false, true)) {
            try {
                final Class<?> oracleWorkaroundJarHandler = Class.forName("org.appwork.utils.OracleWorkaroundJarHandler");
                URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {

                    @Override
                    public URLStreamHandler createURLStreamHandler(String protocol) {
                        try {
                            if ("jar".equals(protocol)) {
                                return (URLStreamHandler) oracleWorkaroundJarHandler.newInstance();
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                });
                System.out.println("JarHandlerWorkaroundOracle:setURLStreamHandlerFactory");
                final Field field = sun.misc.Launcher.class.getDeclaredField("factory");
                field.setAccessible(true);
                final URLStreamHandlerFactory originalFactory = (URLStreamHandlerFactory) field.get(null);
                final URLStreamHandlerFactory workAroundFactory = new URLStreamHandlerFactory() {

                    @Override
                    public URLStreamHandler createURLStreamHandler(final String protocol) {
                        try {
                            if ("jar".equals(protocol)) {
                                return (URLStreamHandler) oracleWorkaroundJarHandler.newInstance();
                            }
                        } catch (final Throwable e) {
                            e.printStackTrace();
                        }
                        return originalFactory.createURLStreamHandler(protocol);
                    }
                };
                field.set(null, workAroundFactory);
                System.out.println("JarHandlerWorkaroundOracle:replaceLauncherFactory");
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
    }
}
