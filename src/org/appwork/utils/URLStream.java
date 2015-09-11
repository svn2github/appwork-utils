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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author daniel
 *
 */
public class URLStream {

    public static InputStream openStream(URL url) throws IOException {
        if (url != null) {
            return url.openStream();
        }
        return null;
    }
}
