/**
 * Copyright (c) 2009 - 2011 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils.net
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.net;

import org.appwork.net.protocol.http.HTTPConstants;

/**
 * @author daniel
 * 
 */

public class HTTPHeader {

    public static final byte[]     DELIMINATOR = ": ".getBytes();
    public static final HTTPHeader CONTENT_TYPE_TEXT_UTF8   = new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "text; charset=utf-8");
    private final String           key;
    private final String           value;
    private final boolean          allowOverwrite;

    public HTTPHeader(final String key, final String value) {
        this(key, value, true);
    }

    public HTTPHeader(final String key, final String value, final boolean overwriteAllowed) {
        this.key = key;
        this.value = value;
        this.allowOverwrite = overwriteAllowed;
    }

    public boolean contains(final String string) {
        if (this.value != null && this.value.contains(string)) {
            return true;
        }
        return false;
    }

    public String format() {
        final StringBuilder sb = new StringBuilder();
        sb.append(this.key);
        sb.append(": ");
        sb.append(this.value);
        return sb.toString();
    }

    public String getKey() {
        return this.key;
    }

    public String getValue() {
        return this.value;
    }

    public boolean isAllowOverwrite() {
        return this.allowOverwrite;
    }

    @Override
    public String toString() {
        return "HTTP Header: " + this.key + "= " + this.value;
    }

}
