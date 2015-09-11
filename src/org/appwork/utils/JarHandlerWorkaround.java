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

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author daniel
 *
 */
public class JarHandlerWorkaround {

    private final static AtomicBoolean INIT = new AtomicBoolean(false);

    // add custom jarHandler for http://bugs.java.com/view_bug.do?bug_id=6390779
    public static void init() {
        if (INIT.compareAndSet(false, true)) {
            try {
                final Class<?> sunJarHandler = Class.forName("sun.net.www.protocol.jar.Handler");
                final Class<?> sunJarURLConnection = Class.forName("sun.net.www.protocol.jar.JarURLConnection");
                final Class<?> sunParseUtil = Class.forName("sun.net.www.ParseUtil");
                if (sunJarHandler != null && sunJarURLConnection != null && sunParseUtil != null) {
                    final Class<?> oracleWorkaround = Class.forName("org.appwork.utils.JarHandlerWorkaroundOracle");
                    final Method init = oracleWorkaround.getMethod("init", new Class[0]);
                    init.invoke(null, new Object[0]);
                }
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
    }

}
