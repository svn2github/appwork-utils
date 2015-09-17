/**
 * Copyright (c) 2009 - 2015 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 *
 * This file is part of org.appwork.stats
 *
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.stats;

/**
 * @author thomas
 *
 */
public class Info {
    private String key;

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    private String value;

    /**
     *
     */
    public Info(String key, Object value) {
        this.key = key;
        if (value == null) {
            value = null;
        } else {
            this.value = value.toString();
        }
    }
}
