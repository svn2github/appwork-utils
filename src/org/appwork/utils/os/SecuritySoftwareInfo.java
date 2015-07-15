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

    public String getState() {
        return get("productState");
    }

    /**
     * http://neophob.com/2010/03/wmi-query-windows-securitycenter2/
     *
     * @return
     */
    public boolean isEnabled() {
        if ("TRUE".equals(get("enabled"))) {

            // XP Firewall only
            return true;
        }
        if (get("productState") == null) {
            // XP
            return true;
        }
        String state = getState();
        if (state == null) {
            return false;
        }
        int i = Integer.parseInt(state);
        return isEnabledByState(i);
    }

    public static boolean isEnabledByState(int i) {
        // String hex = StringUtils.fillPre(Integer.toHexString(i), "0", 6);
        // int upToDate = 0xFF & i;
        int enabledFlag = 0xFF & i >> 8;
        // int type = 0xFF & i >> 16;
        // System.out.println(Integer.toHexString(enabled) + " " + Integer.toHexString(enabledFlag) + " " + Integer.toHexString(c));
        // if ("AVG AntiVirus Free Edition 2015".equals(getName())) {
        // System.out.println(getState() + "\t-> " + hex + "\t" + (enabledFlag >= 16) + " " + Regex.getLines(get("response")).length);
        // }
        return enabledFlag >= 16;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.AbstractMap#toString()
     */
    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return getName() + " Enabled: " + isEnabled() + " Up2Date: " + isUp2Date();
    }

    public boolean isUp2Date() {
        String up2DateXp = get("productUptoDate");
        if ("TRUE".equalsIgnoreCase(up2DateXp)) {
            return true;
        }
        String state = getState();
        if (state == null) {
            return false;
        }
        int i = Integer.parseInt(state);
        // String hex = StringUtils.fillPre(Integer.toHexString(i), "0", 6);
        int upToDate = 0xFF & i;
        // int enabledFlag = 0xFF & i >> 8;
        // int type = 0xFF & i >> 16;
        // System.out.println(Integer.toHexString(enabled) + " " + Integer.toHexString(enabledFlag) + " " + Integer.toHexString(c));
        return upToDate == 0;
    }
}
