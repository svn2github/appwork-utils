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
 *     The intent is that the AppWork GmbH is able to provide  their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 *     These terms apply to all files that have the [The Product] License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 *
 * === 3rd Party Licences ===
 *     Some parts of the [The Product] use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 *     to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header.
 *
 * === Definition: Commercial Usage ===
 *     If anybody or any organization is generating income (directly or indirectly) by using [The Product] or if there's any commercial interest or aspect in what you are doing, we consider this as a commercial usage.
 *     If your use-case is neither strictly private nor strictly educational, it is commercial. If you are unsure whether your use-case is commercial or not, consider it as commercial or contact as.
 * === Dual Licensing ===
 * === Commercial Usage ===
 *     If you want to use [The Product] in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 *     Contact AppWork for further details: e-mail@appwork.org
 * === Non-Commercial Usage ===
 *     If there is no commercial usage (see definition above), you may use [The Product] under the terms of the
 *     "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 *
 *     If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.utils;

/**
 * @author daniel
 * @date 21.04.2017
 *
 */
public class JVMVersion {
    public final static long  JAVA15  = 15000000;
    public final static long  JAVA16  = 16000000;
    public final static long  JAVA17  = 17000000;
    public final static long  JAVA18  = 18000000;
    public final static long  JAVA19  = 9000000000000l;
    public final static long  JAVA_10 = 10000000000000l;
    private static final long VERSION;
    static {
        long version = -1;
        try {
            final String versionString = getJVMVersion();
            version = parseJavaVersionString(versionString);
        } catch (final Throwable ignore) {
            ignore.printStackTrace();
            version = JAVA16;
        }
        VERSION = version;
    }

    // TODO: JDK9
    public static final boolean isJAVA19Test() {
        return isMinimum(JAVA19) || "true".equals(System.getProperty("jdk9test"));
    }

    public static final long get() {
        return VERSION;
    }

    public static final boolean isMinimum(final long version) {
        return VERSION >= version;
    }

    public static String getJVMVersion() {
        /* this version info contains more information */
        final String version = System.getProperty("java.runtime.version");
        if (version == null || version.trim().length() == 0) {
            return System.getProperty("java.version");
        } else {
            return version;
        }
    }

    public static long parseJavaVersionString(String version) {
        if (version != null) {
            // remove trailing LTS
            version = version.replaceFirst("\\s*-\\s*LTS$", "");
        }
        final String majorFeature = new Regex(version, "^(\\d+)").getMatch(0);
        final String minorInterim = new Regex(version, "^\\d+\\.(\\d+)").getMatch(0);
        final String securityUpdate = new Regex(version, "^\\d+\\.\\d+\\.(\\d+)").getMatch(0);
        // final String patch = new Regex(version, "^\\d+\\.\\d+\\.\\d+\\.(\\d+)").getMatch(0);
        long ret = 0;
        final String u;
        final String b;
        if ("1".equals(majorFeature) && minorInterim != null) {
            // java 1.5 - java 1.8
            ret = Long.parseLong(minorInterim) * 1000 * 1000 + 10000000;
            u = new Regex(version, "^.*?_(\\d+)").getMatch(0);
            b = new Regex(version, "^.*?(_|-)b(\\d+)$").getMatch(1);
            // ignore securityUpdate
        } else if (majorFeature != null) {
            // java 1.9, java 10, java 11...
            ret = Long.parseLong(majorFeature) * 1000 * 1000 * 1000 * 1000l;
            u = new Regex(version, "u(\\d+)").getMatch(0);
            b = new Regex(version, "\\+(\\d+)$").getMatch(0);
            if (minorInterim != null) {
                ret += Math.min(999, Long.parseLong(minorInterim)) * 1000 * 1000 * 1000l;
            }
            if (securityUpdate != null) {
                ret += Math.min(999, Long.parseLong(securityUpdate)) * 1000 * 1000;
            }
        } else {
            // fallback to Java 1.6
            return JAVA16;
        }
        if (u != null) {
            /* append update number */
            ret += Math.min(999, Long.parseLong(u)) * 1000;
        }
        if (b != null) {
            /* append build number */
            ret += Math.min(999, Long.parseLong(b));
        }
        return ret;
    }
}
