/**
 * Copyright (c) 2009 - 2015 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils.os
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.os;

import java.util.HashMap;

/**
 * @author Thomas
 * 
 */
public class SecuritySoftwareInfo extends HashMap<String, String> {

    public String getName() {

        return get("displayName");
    }
}
