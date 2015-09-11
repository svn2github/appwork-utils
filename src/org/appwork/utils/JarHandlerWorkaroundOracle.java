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
import java.util.List;
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
                final Class<java.net.URLStreamHandler> oracleWorkaroundJarHandler = (Class<URLStreamHandler>) Class.forName("org.appwork.utils.OracleWorkaroundJarHandler");
                {
                    URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {

                        @Override
                        public URLStreamHandler createURLStreamHandler(String protocol) {
                            try {
                                if ("jar".equals(protocol)) {
                                    return oracleWorkaroundJarHandler.newInstance();
                                }
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                            return null;
                        }
                    });
                    System.out.println("JarHandlerWorkaroundOracle:setURLStreamHandlerFactory");
                }
                {
                    final Field field = sun.misc.Launcher.class.getDeclaredField("factory");
                    field.setAccessible(true);
                    final URLStreamHandlerFactory originalFactory = (URLStreamHandlerFactory) field.get(null);
                    final URLStreamHandlerFactory workAroundFactory = new URLStreamHandlerFactory() {

                        @Override
                        public URLStreamHandler createURLStreamHandler(final String protocol) {
                            try {
                                if ("jar".equals(protocol)) {
                                    return oracleWorkaroundJarHandler.newInstance();
                                }
                            } catch (final Throwable e) {
                                e.printStackTrace();
                            }
                            return originalFactory.createURLStreamHandler(protocol);
                        }
                    };
                    field.set(null, workAroundFactory);
                    System.out.println("JarHandlerWorkaroundOracle:replaceLauncherFactory");
                }
                {
                    sun.misc.URLClassPath urlClassPath = null;
                    try {
                        final ClassLoader cl = JarHandlerWorkaroundOracle.class.getClassLoader();
                        final Field ucp = cl.getClass().getDeclaredField("ucp");
                        ucp.setAccessible(true);
                        urlClassPath = (sun.misc.URLClassPath) ucp.get(cl);
                    } catch (final Throwable e) {
                        final Field bcp = sun.misc.Launcher.class.getDeclaredField("bcp");
                        bcp.setAccessible(true);
                        urlClassPath = (sun.misc.URLClassPath) bcp.get(null);
                    }
                    if (urlClassPath != null) {
                        System.out.println("JarHandlerWorkaroundOracle:replaceURLClassPath");
                        final Field jarHandler = urlClassPath.getClass().getDeclaredField("jarHandler");
                        jarHandler.setAccessible(true);
                        jarHandler.set(urlClassPath, oracleWorkaroundJarHandler.newInstance());
                        System.out.println("JarHandlerWorkaroundOracle:replacejarHandler");
                        final Field loadersField = urlClassPath.getClass().getDeclaredField("loaders");
                        loadersField.setAccessible(true);
                        final List<Object> loaders = (List<Object>) loadersField.get(urlClassPath);
                        System.out.println("JarHandlerWorkaroundOracle:replaceLoaders:" + loaders.size());
                        for (int index = 0; index < loaders.size(); index++) {
                            try {
                                final Object loader = loaders.get(index);
                                if (loader.getClass().getName().endsWith("JarLoader")) {
                                    Field handlerField = loader.getClass().getDeclaredField("handler");
                                    handlerField.setAccessible(true);
                                    handlerField.set(loader, oracleWorkaroundJarHandler.newInstance());
                                    System.out.println("JarHandlerWorkaroundOracle:replaceLoader:" + index + ":handler");
                                    final Field baseField = loader.getClass().getSuperclass().getDeclaredField("base");
                                    baseField.setAccessible(true);
                                    final URL base = (URL) baseField.get(loader);
                                    handlerField = base.getClass().getDeclaredField("handler");
                                    handlerField.setAccessible(true);
                                    handlerField.set(base, oracleWorkaroundJarHandler.newInstance());
                                    System.out.println("JarHandlerWorkaroundOracle:replaceLoader:" + index + ":handler:" + base);
                                }
                            } catch (final Throwable ignore) {
                                ignore.printStackTrace();
                            }
                        }
                    }
                }
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
    }
}
