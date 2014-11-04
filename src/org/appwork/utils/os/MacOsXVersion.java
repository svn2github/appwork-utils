package org.appwork.utils.os;

/**
 * Copyright (c) 2009 - 2014 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils.os
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */

/**
 * @author Thomas
 * 
 */
public enum MacOsXVersion {

    /**
     * 24. March 2001
     */
    MAC_OSX_10p0_CHEETAH("Cheetah", "10.0", 10000000),
    /**
     * 25. September 2001
     */
    MAC_OSX_10p1_PUMA("Puma", "10.1", 10001000),
    /**
     * 13. August 2002
     */
    MAC_OSX_10p2_JAGUAR("Jaguar", "10.2", 10002000),
    /**
     * 24. Oktober 2003
     */
    MAC_OSX_10p3_PANTHER("Panther", "10.3", 10003000),
    /**
     * 29. April 2005
     */
    MAC_OSX_10p4_TIGER("Tiger", "10.4", 10004000),
    /**
     * 
     * 26. Oktober 2007
     */
    MAC_OSX_10p5_LEOPARD("Leopard", "10.5", 10005000),
    /**
     * 28. August 2009
     */
    MAC_OSX_10p6_SNOW_LEOPARD("Snow Leopard", "10.6", 10006000),
    /**
     * 20. Juli 2011
     */
    MAC_OSX_10p7_LION("Lion", "10.7", 10007000),
    /**
     * 16. Februar 2012
     */
    MAC_OSX_10p8_MOUNTAIN_LION("Mountain Lion", "10.8", 10008000),
    /**
     * 10. Juni 2013
     */
    MAC_OSX_10p9_MAVERICKS("Mavericks", "10.9", 10009000),
    /**
     * 16. Oktober 2014
     */
    MAC_OSX_10p10_YOSEMITE("Yosemite", "10.10", 10010000);

    final private String name;

    public String getName() {
        return name;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public long getVersionID() {
        return versionID;
    }

    final private String osVersion;
    final private long   versionID;

    private MacOsXVersion(String name, String osVersion, long versionID) {
        this.name = name;
        this.osVersion = osVersion;
        this.versionID = versionID;
    }
}
