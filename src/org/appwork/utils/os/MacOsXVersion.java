/**
 * 
 * ====================================================================================================================================================
 *         "AppWork Utilities" License
 *         The "AppWork Utilities" will be called [The Product] from now on.
 * ====================================================================================================================================================
 *         Copyright (c) 2009-2015, AppWork GmbH <e-mail@appwork.org>
 *         Schwabacher Straße 117
 *         90763 Fürth
 *         Germany   
 * === Preamble ===
 *     This license establishes the terms under which the [The Product] Source Code & Binary files may be used, copied, modified, distributed, and/or redistributed.
 *     The intent is that the AppWork GmbH is able to provide their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 *     These terms apply to all files that have the [The Product] License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 * 	
 * === 3rd Party Licences ===
 *     Some parts of the [The Product] use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 *     to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header. 	
 * 	
 * === Definition: Commercial Usage ===
 *     If anybody or any organization is generating income (directly or indirectly) by using [The Product] or if there's any commercial interest or aspect in what you are doing, we consider this as a commercial usage.
 *     If your use-case is neither strictly private nor strictly educational, it is commercial. If you are unsure whether your use-case is commercial or not, consider it as commercial or contact us.
 * === Dual Licensing ===
 * === Commercial Usage ===
 *     If you want to use [The Product] in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 *     Contact AppWork for further details: <e-mail@appwork.org>
 * === Non-Commercial Usage ===
 *     If there is no commercial usage (see definition above), you may use [The Product] under the terms of the 
 *     "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 * 	
 *     If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.utils.os;

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
