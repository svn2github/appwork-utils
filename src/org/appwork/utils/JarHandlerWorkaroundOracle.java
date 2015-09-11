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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;

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
                URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {

                    @Override
                    public URLStreamHandler createURLStreamHandler(String protocol) {
                        if ("jar".equals(protocol)) {
                            return new sun.net.www.protocol.jar.Handler() {
                                @Override
                                protected URLConnection openConnection(final URL url) throws IOException {
                                    try {
                                        System.out.println("openConnection:" + url);
                                        final String path = url.getFile();
                                        URL jarFileURL = null;
                                        int lastIndex = 0;
                                        if (StringUtils.startsWithCaseInsensitive(path, "file:")) {
                                            while (true) {
                                                final int indexOf = path.indexOf(".jar", lastIndex);
                                                if (indexOf > 0) {
                                                    final int index = indexOf + 4;
                                                    lastIndex = index;
                                                    final String jarFileName = path.substring(0, index);
                                                    if (jarFileName.contains("!/")) {
                                                        final File jarFile;
                                                        final URL jarURL;
                                                        try {
                                                            jarURL = new URL(jarFileName);
                                                            jarFile = new File(jarURL.toURI());
                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                            continue;
                                                        }
                                                        if (jarFile.exists() && jarFile.isFile()) {
                                                            jarFileURL = jarURL;
                                                            break;
                                                        }
                                                    }
                                                } else {
                                                    break;
                                                }
                                            }
                                        }
                                        if (jarFileURL != null) {
                                            lastIndex++;// skip !
                                            final URL finalJarFileURL = jarFileURL;
                                            final String entryName;
                                            if (++lastIndex != path.length()) {
                                                final String tempEntryName = path.substring(lastIndex, path.length());
                                                entryName = sun.net.www.ParseUtil.decode(tempEntryName);
                                            } else {
                                                entryName = null;
                                            }
                                            System.out.println("Workaround for openConnection:" + finalJarFileURL + " Entry:" + entryName);
                                            return new sun.net.www.protocol.jar.JarURLConnection(url, this) {
                                                @Override
                                                public URL getJarFileURL() {
                                                    return finalJarFileURL;
                                                }

                                                @Override
                                                public JarEntry getJarEntry() throws IOException {
                                                    return getJarFile().getJarEntry(getEntryName());
                                                }

                                                @Override
                                                public String getEntryName() {
                                                    return entryName;
                                                }
                                            };
                                        }
                                    } catch (final Throwable e) {
                                        e.printStackTrace();
                                    }
                                    return super.openConnection(url);

                                }
                            };
                        }
                        return null;
                    }
                });
                System.out.println("JarHandlerWorkaroundOracle");
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
    }
}
