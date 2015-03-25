/**
 * Copyright (c) 2009 - 2014 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 *
 * This file is part of org.appwork.sunwrapper.sun.net.www.protocol.http
 *
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.sunwrapper.sun.net.www.protocol.http;

import org.appwork.utils.logging2.extmanager.LoggerFactory;

/**
 * @author Thomas
 *
 */
public class AuthCacheValueWrapper {

    /**
     *
     */
    public static void setAuthCacheImpl() {
        try {
            sun.net.www.protocol.http.AuthCacheValue.setAuthCache(new sun.net.www.protocol.http.AuthCacheImpl());
        } catch (final NoClassDefFoundError e) {
            /* sun/oracle java only? */
            LoggerFactory.I().getLogger(AuthCacheValueWrapper.class.getName()).log(e);
        }
    }

}
