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

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.swing.KeyStroke;

import org.appwork.exceptions.WTFException;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.uio.InputDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Hash;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.locale._AWU;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.appwork.utils.os.mime.Mime;
import org.appwork.utils.os.mime.MimeFactory;
import org.appwork.utils.processes.ProcessBuilderFactory;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.InputDialog;

/**
 * This class provides a few native features.
 *
 * @author $Author: unknown$
 */
public class CrossSystem {
    public static enum OperatingSystem {
        NETBSD(OSFamily.BSD),
        OPENBSD(OSFamily.BSD),
        KFREEBSD(OSFamily.BSD),
        FREEBSD(OSFamily.BSD),
        DRAGONFLYBSD(OSFamily.BSD),
        BSD(OSFamily.BSD),
        LINUX(OSFamily.LINUX),
        /*
         * List must be sorted by release Date!!
         */
        MAC(OSFamily.MAC),
        MAC_CHEETAH(OSFamily.MAC),
        MAC_PUMA(OSFamily.MAC),
        MAC_JAGUAR(OSFamily.MAC),
        MAC_PANTHER(OSFamily.MAC),
        MAC_TIGER(OSFamily.MAC),
        MAC_LEOPOARD(OSFamily.MAC),
        MAC_SNOW_LEOPOARD(OSFamily.MAC),
        MAC_LION(OSFamily.MAC),
        MAC_MOUNTAIN_LION(OSFamily.MAC),
        MAC_MAVERICKS(OSFamily.MAC),
        MAC_YOSEMITE(OSFamily.MAC),
        MAC_EL_CAPITAN(OSFamily.MAC),
        MAC_SIERRA(OSFamily.MAC),
        MAC_HIGH_SIERRA(OSFamily.MAC),
        OS2(OSFamily.OS2),
        /*
         * List must be sorted by release Date!!
         */
        WINDOWS_OTHERS(OSFamily.WINDOWS),
        WINDOWS_NT(OSFamily.WINDOWS),
        WINDOWS_2000(OSFamily.WINDOWS),
        WINDOWS_XP(OSFamily.WINDOWS),
        WINDOWS_2003(OSFamily.WINDOWS),
        WINDOWS_VISTA(OSFamily.WINDOWS),
        WINDOWS_SERVER_2003(OSFamily.WINDOWS),
        WINDOWS_SERVER_2008(OSFamily.WINDOWS),
        WINDOWS_7(OSFamily.WINDOWS),
        WINDOWS_8(OSFamily.WINDOWS),
        WINDOWS_SERVER_2012(OSFamily.WINDOWS),
        WINDOWS_SERVER_2016(OSFamily.WINDOWS),
        WINDOWS_10(OSFamily.WINDOWS);
        private final OSFamily family;

        private OperatingSystem(final OSFamily family) {
            this.family = family;
        }

        public final OSFamily getFamily() {
            return this.family;
        }

        public final boolean isMaximum(final OperatingSystem os) {
            if (os.getFamily().equals(getFamily())) {
                final int maximum = os.ordinal();
                return ordinal() <= maximum;
            }
            return false;
        }

        public final boolean isMinimum(final OperatingSystem os) {
            if (os.getFamily().equals(getFamily())) {
                final int minimum = os.ordinal();
                return ordinal() >= minimum;
            }
            return false;
        }
    }

    public static enum OSFamily {
        BSD,
        LINUX,
        MAC,
        OS2,
        OTHERS,
        WINDOWS
    }

    public static enum ARCHFamily {
        NA,
        X86,
        ARM,
        PPC,
        SPARC,
        IA64
    }

    private static Boolean ISRASPBERRYPI = null;

    public static boolean isUnix() {
        return CrossSystem.isBSD() || CrossSystem.isLinux();
    }

    // http://www.raspberrypi-spy.co.uk/2012/09/checking-your-raspberry-pi-board-version/
    // http://elinux.org/RPi_HardwareHistory#Board_Revision_History
    public static boolean isRaspberryPi() {
        if (CrossSystem.ISRASPBERRYPI != null) {
            return CrossSystem.ISRASPBERRYPI;
        }
        boolean isRaspberryPi = false;
        if (isUnix() && ARCHFamily.ARM.equals(CrossSystem.getARCHFamily())) {
            FileInputStream fis = null;
            try {
                boolean armVx = false;
                String revision = null;
                fis = new FileInputStream("/proc/cpuinfo");
                final BufferedReader is = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
                String line = null;
                while ((line = is.readLine()) != null) {
                    if (StringUtils.containsIgnoreCase(line, "ARMv")) {
                        armVx = true;
                    } else if (StringUtils.startsWithCaseInsensitive(line, "Revision")) {
                        revision = new Regex(line, "(?i)^Revision\\s*:\\s*(.+)").getMatch(0);
                    }
                    if (armVx && revision != null) {
                        is.close();
                        break;
                    }
                }
                if (armVx && revision != null) {
                    if (revision.startsWith("1000") && revision.length() > 4) {
                        // if you see a "1000" at the front of the Revision, e.g. 10000002 then it indicates[1] that your Raspberry Pi has
                        // been over-volted, and your board revision is simply the last 4 digits (i.e. 0002 in this example).
                        revision = revision.substring(4);
                    }
                    if (StringUtils.equalsIgnoreCase(revision, "0002")) {
                        // Model B Rev 1
                        isRaspberryPi = true;
                    } else if (StringUtils.equalsIgnoreCase(revision, "0003")) {
                        // Model B Rev 1, ECHN0001
                        isRaspberryPi = true;
                    } else if (StringUtils.equalsIgnoreCase(revision, "0004") || StringUtils.equalsIgnoreCase(revision, "0005") || StringUtils.equalsIgnoreCase(revision, "0006")) {
                        // Model B Rev 2
                        isRaspberryPi = true;
                    } else if (StringUtils.equalsIgnoreCase(revision, "0007") || StringUtils.equalsIgnoreCase(revision, "0008") || StringUtils.equalsIgnoreCase(revision, "0009")) {
                        // Model A
                        isRaspberryPi = true;
                    } else if (StringUtils.equalsIgnoreCase(revision, "000d") || StringUtils.equalsIgnoreCase(revision, "000e") || StringUtils.equalsIgnoreCase(revision, "00e") || StringUtils.equalsIgnoreCase(revision, "000f")) {
                        // Model B Rev 2
                        isRaspberryPi = true;
                    } else if (StringUtils.equalsIgnoreCase(revision, "0010") || StringUtils.equalsIgnoreCase(revision, "0013") || StringUtils.equalsIgnoreCase(revision, "900032")) {
                        // Model B+
                        isRaspberryPi = true;
                    } else if (StringUtils.equalsIgnoreCase(revision, "0011") || StringUtils.equalsIgnoreCase(revision, "0014")) {
                        // Compute Module
                        isRaspberryPi = true;
                    } else if (StringUtils.equalsIgnoreCase(revision, "0012") || StringUtils.equalsIgnoreCase(revision, "0015")) {
                        // Model A+
                        isRaspberryPi = true;
                    } else if (StringUtils.equalsIgnoreCase(revision, "a01040") || StringUtils.equalsIgnoreCase(revision, "a01041") || StringUtils.equalsIgnoreCase(revision, "a21041")) {
                        // Pi 2 Model B v1.0,v1.1,v1.2
                        isRaspberryPi = true;
                    } else if (StringUtils.equalsIgnoreCase(revision, "a22042")) {
                        // Pi 2 Model B v1.2
                        isRaspberryPi = true;
                    } else if (StringUtils.equalsIgnoreCase(revision, "900021")) {
                        // Pi 2 Model A+ V1.1
                        isRaspberryPi = true;
                    } else if (StringUtils.equalsIgnoreCase(revision, "900032")) {
                        // Pi 2 Model B+ V1.2
                        isRaspberryPi = true;
                    } else if (StringUtils.equalsIgnoreCase(revision, "900092")) {
                        // Pi Zero v1.2
                        isRaspberryPi = true;
                    } else if (StringUtils.equalsIgnoreCase(revision, "900093") || StringUtils.equalsIgnoreCase(revision, "920093")) {
                        // Pi Zero v1.3
                        isRaspberryPi = true;
                    } else if (StringUtils.equalsIgnoreCase(revision, "9000C1") || StringUtils.equalsIgnoreCase(revision, "0x9000C1")) {
                        // Pi Zero W
                        isRaspberryPi = true;
                    } else if (StringUtils.equalsIgnoreCase(revision, "a020a0 ")) {
                        // Compute Module 3 (and CM3 Lite)
                        isRaspberryPi = true;
                    } else if (StringUtils.equalsIgnoreCase(revision, "a02082") || StringUtils.equalsIgnoreCase(revision, "a22082") || StringUtils.equalsIgnoreCase(revision, "a32082")) {
                        // Pi 3 Model B
                        isRaspberryPi = true;
                    }
                }
                is.close();
            } catch (final Throwable e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (final Throwable e) {
                }
            }
        }
        CrossSystem.ISRASPBERRYPI = isRaspberryPi;
        return isRaspberryPi;
    }

    private static final boolean        __HEADLESS                = Application.isHeadless();
    private static String[]             BROWSER_COMMANDLINE       = null;
    private static DesktopSupport       DESKTOP_SUPPORT           = null;
    private static String[]             FILE_COMMANDLINE          = null;
    private static String               JAVAINT                   = null;
    /**
     *
     */
    private static final KeyStroke      KEY_STROKE_BACKSPACE_CTRL = CrossSystem.__HEADLESS ? null : KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    /**
     *
     */
    private static final KeyStroke      KEY_STROKE_COPY           = CrossSystem.__HEADLESS ? null : KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    /**
     *
     */
    private static final KeyStroke      KEY_STROKE_CUT            = CrossSystem.__HEADLESS ? null : KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    /**
     *
     */
    private static final KeyStroke      KEY_STROKE_DELETE         = CrossSystem.__HEADLESS ? null : KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
    /**
     *
     */
    private static final KeyStroke      KEY_STROKE_DOWN           = CrossSystem.__HEADLESS ? null : KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);
    /**
     *
     */
    private static final KeyStroke      KEY_STROKE_ESCAPE         = CrossSystem.__HEADLESS ? null : KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    /**
     *
     */
    private static final KeyStroke      KEY_STROKE_FORCE_DELETE   = CrossSystem.__HEADLESS ? null : KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, ActionEvent.SHIFT_MASK);
    /**
     *
     */
    private static final KeyStroke      KEY_STROKE_PASTE          = CrossSystem.__HEADLESS ? null : KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    /**
     *
     */
    private static final KeyStroke      KEY_STROKE_SEARCH         = CrossSystem.__HEADLESS ? null : KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    /**
     *
     */
    private static final KeyStroke      KEY_STROKE_SELECT_ALL     = CrossSystem.__HEADLESS ? null : KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    /**
     *
     */
    private static final KeyStroke      KEY_STROKE_UP             = CrossSystem.__HEADLESS ? null : KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0);
    /**
     * Cache to store the Mime Class in
     */
    private static final Mime           MIME;
    public static final OperatingSystem OS;
    public static final ARCHFamily      ARCH;
    /**
     * Cache to store the OS string in
     */
    private final static String         OS_STRING;
    private final static String         ARCH_STRING;
    private static Boolean              OS64BIT                   = null;
    public final static String          WMIC_PATH;
    static {
        /* Init OS_ID */
        OS_STRING = System.getProperty("os.name");
        ARCH_STRING = System.getProperty("os.arch");
        OS = CrossSystem.getOSByString(CrossSystem.OS_STRING);
        ARCH = CrossSystem.getARCHByString(CrossSystem.ARCH_STRING);
        /* Init MIME */
        if (CrossSystem.isWindows()) {
            CrossSystem.DESKTOP_SUPPORT = new DesktopSupportWindows();
        } else if (CrossSystem.isLinux()) {
            CrossSystem.DESKTOP_SUPPORT = new DesktopSupportLinux();
        } else if (CrossSystem.isMac()) {
            CrossSystem.DESKTOP_SUPPORT = new DesktopSupportMac();
        } else {
            CrossSystem.DESKTOP_SUPPORT = new DesktopSupportJavaDesktop();
        }
        MIME = MimeFactory.getInstance();
        if (CrossSystem.isWindows()) {
            final String wmic = System.getenv("SYSTEMROOT") + "\\System32\\Wbem\\wmic.exe";
            if (new File(wmic).exists()) {
                WMIC_PATH = wmic;
            } else {
                WMIC_PATH = "wmic";
            }
        } else {
            WMIC_PATH = null;
        }
    }

    public static String getDefaultDownloadDirectory() {
        try {
            final String defaultDownloadDirectory = CrossSystem.DESKTOP_SUPPORT.getDefaultDownloadDirectory();
            if (StringUtils.isNotEmpty(defaultDownloadDirectory)) {
                //
                return defaultDownloadDirectory;
            }
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        final String userHome = System.getProperty("user.home");
        if (userHome != null && new File(userHome).exists() && new File(userHome).isDirectory()) {
            return new File(userHome, "Downloads").getAbsolutePath();
        } else {
            return Application.getResource("Downloads").getAbsolutePath();
        }
    }

    /**
     * internal function to open a file/folder
     *
     * @param file
     * @throws IOException
     */
    private static void _openFILE(final File file) throws IOException {
        try {
            if (CrossSystem.openCustom(CrossSystem.FILE_COMMANDLINE, file.getAbsolutePath())) {
                return;
            } else if (CrossSystem.isOpenFileSupported()) {
                CrossSystem.DESKTOP_SUPPORT.openFile(file);
            }
        } catch (IOException e) {
            if (CrossSystem.isOpenFileSupported()) {
                CrossSystem.DESKTOP_SUPPORT.openFile(file);
            } else {
                throw e;
            }
        }
    }

    /**
     * internal function to open an URL in a browser
     *
     * @param _url
     * @throws IOException
     * @throws URISyntaxException
     */
    public static void openUrlOrThrowException(final String _url) throws IOException, URISyntaxException {
        try {
            if (CrossSystem.openCustom(CrossSystem.BROWSER_COMMANDLINE, _url)) {
                return;
            } else if (CrossSystem.isOpenBrowserSupported()) {
                CrossSystem.DESKTOP_SUPPORT.browseURL(new URL(_url));
            } else {
                throw new IOException("Unsupported OpenBrowser:" + _url);
            }
        } catch (final IOException e) {
            if (CrossSystem.isOpenBrowserSupported()) {
                CrossSystem.DESKTOP_SUPPORT.browseURL(new URL(_url));
            } else {
                throw e;
            }
        }
    }

    /**
     * use this method to make pathPart safe to use in a full absoluePath.
     *
     * it will remove driveletters/path separators and all known chars that are forbidden in a path
     *
     * @param pathPart
     * @return
     */
    public static String alleviatePathParts(String pathPart) {
        return alleviatePathParts(pathPart, true);
    }

    /**
     * use this method to make pathPart safe to use in a full absoluePath.
     *
     * it will remove driveletters/path separators and all known chars that are forbidden in a path
     *
     * @param pathPart
     *            {@link String}
     * @param removeLeadingHidingDot
     *            {@link Boolean} remove leading/hiding(unix) dot
     * @return
     */
    public static String alleviatePathParts(String pathPart, boolean removeLeadingHidingDot) {
        if (StringUtils.isEmpty(pathPart)) {
            if (pathPart != null) {
                return pathPart;
            }
            return null;
        }
        pathPart = pathPart.trim();
        /* remove invalid chars */
        /**
         * Integer value zero, sometimes referred to as the ASCII NUL character.
         *
         * Characters whose integer representations are in the range from 1 through 31->\\x00-\\x1f
         *
         * < (less than), * > (greater than), : (colon), " (double quote), / (forward slash), \ (backslash), | (vertical bar or pipe), ?
         * (question* mark) (asterisk)
         *
         * Volume designators (drive letters) are similarly case-insensitive. For example, "D:\" and "d:\" refer to the same volume.
         */
        pathPart = pathPart.replaceAll("([\\\\|<|>|\\||\r|\n|\t|\"|:|\\*|\\?|/|\\x00-\\x1f])+", "_");
        if (CrossSystem.isWindows() || CrossSystem.isOS2()) {
            /**
             * http://msdn.microsoft.com/en-us/library/windows/desktop/aa365247%28v=vs.85%29.aspx
             */
            if (CrossSystem.isForbiddenFilename(pathPart)) {
                pathPart = "_" + pathPart;
            }
        }
        /*
         * replace starting dots by single dot (prevents directory traversal)
         */
        if (removeLeadingHidingDot) {
            pathPart = pathPart.replaceFirst("^\\.+", "");
        } else {
            pathPart = pathPart.replaceFirst("^\\.+", ".");
        }
        /*
         * remove ending dots, not allowed under windows and others os maybe too
         * 
         * Do not end a file or directory name with a space or a period.
         */
        pathPart = pathPart.replaceFirst("\\.+$", "");
        pathPart = pathPart.trim();
        if (StringUtils.isEmpty(pathPart)) {
            return "_";
        } else {
            return pathPart;
        }
    }

    public static boolean isForbiddenFilename(String name) {
        if (CrossSystem.isWindows() || CrossSystem.isOS2()) {
            /**
             * http://msdn.microsoft.com/en-us/library/windows/desktop/aa365247%28v=vs.85%29.aspx
             *
             * Do not use the following reserved names for the name of a file:
             *
             * CON, PRN, AUX, NUL, COM1, COM2, COM3, COM4, COM5, COM6, COM7, COM8, COM9, LPT1, LPT2, LPT3, LPT4, LPT5, LPT6, LPT7, LPT8, and
             * LPT9.
             */
            return new Regex(name, "^(CON|PRN|AUX|NUL|COM\\d+|LPT\\d+|CLOCK)\\s*?(\\.|$)").matches();
        }
        return false;
    }

    public static String fixPathSeparators(String path) {
        if (StringUtils.isEmpty(path)) {
            if (path != null) {
                return path;
            }
            return null;
        }
        if (CrossSystem.isWindows()) {
            /* windows uses \ as path separator */
            final boolean network = path.startsWith("\\\\");
            path = path.replaceAll("(/+)", "\\\\");
            path = path.replaceAll("(\\\\+)", "\\\\");
            if (network) {
                path = "\\" + path;
            }
        } else {
            /* mac/linux uses / as path separator */
            path = path.replaceAll("(\\\\+)", "/");
            path = path.replaceAll("(/+)", "/");
        }
        return path;
    }

    public static String[] getBrowserCommandLine() {
        return CrossSystem.BROWSER_COMMANDLINE;
    }

    /**
     * @return
     */
    public static KeyStroke getDeleteShortcut() {
        if (CrossSystem.isMac()) {
            return CrossSystem.KEY_STROKE_BACKSPACE_CTRL;
        }
        return CrossSystem.KEY_STROKE_DELETE;
    }

    public static String[] getFileCommandLine() {
        return CrossSystem.FILE_COMMANDLINE;
    }

    public static String getJavaBinary() {
        if (CrossSystem.JAVAINT != null) {
            return CrossSystem.JAVAINT;
        }
        String javaBinary = "java";
        if (CrossSystem.isWindows() || CrossSystem.isOS2()) {
            javaBinary = "javaw.exe";
        }
        final String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            /* get path from system property */
            final File java = new File(new File(javaHome), "/bin/" + javaBinary);
            if (java.exists() && java.isFile()) {
                CrossSystem.JAVAINT = java.getAbsolutePath();
            }
        } else {
            CrossSystem.JAVAINT = javaBinary;
        }
        return CrossSystem.JAVAINT;
    }

    public static boolean caseSensitiveFileExists(File file) {
        if (file != null) {
            if (CrossSystem.isWindows()) {
                if (Application.getJavaVersion() >= Application.JAVA17) {
                    try {
                        /**
                         * this is very fast
                         */
                        return CrossSystem17.caseSensitiveFileExists(file);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
                if (file.exists()) {
                    /** this can be slow **/
                    File current = file;
                    String currentName = current.getName();
                    loop: while ((current = current.getParentFile()) != null) {
                        final String[] list = current.list();
                        if (list != null) {
                            for (String listItem : list) {
                                if (currentName.equals(listItem)) {
                                    currentName = current.getName();
                                    continue loop;
                                }
                            }
                        }
                        return false;
                    }
                    return true;
                }
                return false;
            } else {
                return file.exists();
            }
        }
        return false;
    }

    private static long parseMacOSVersion(final String osVersionProperty) {
        if (osVersionProperty != null) {
            try {
                long ret = 0;
                long faktor = 1000000;
                for (final String s : osVersionProperty.split("\\.")) {
                    ret += Integer.parseInt(s) * faktor;
                    faktor /= 1000;
                }
                return ret;
            } catch (final Throwable ignore) {
            }
        }
        return -1;
    }

    /**
     * Returns the Mime Class for the current OS
     *
     * @return
     * @see Mime
     */
    public static Mime getMime() {
        return CrossSystem.MIME;
    }

    /**
     * @return
     */
    public static OperatingSystem getOS() {
        return CrossSystem.OS;
    }

    private static OperatingSystem getWindowsRelease(final String osName) {
        if (osName != null) {
            final String os = osName.toLowerCase(Locale.ENGLISH);
            if (os.contains("windows 10")) {
                return OperatingSystem.WINDOWS_10;
            } else if (os.contains("windows 8")) {
                return OperatingSystem.WINDOWS_8;
            } else if (os.contains("windows 7")) {
                return OperatingSystem.WINDOWS_7;
            } else if (os.contains("windows xp")) {
                return OperatingSystem.WINDOWS_XP;
            } else if (os.contains("windows vista")) {
                return OperatingSystem.WINDOWS_VISTA;
            } else if (os.contains("windows 2000")) {
                return OperatingSystem.WINDOWS_2000;
            } else if (os.contains("windows 2003")) {
                return OperatingSystem.WINDOWS_2003;
            } else if (os.contains("windows server 2003")) {
                return OperatingSystem.WINDOWS_SERVER_2003;
            } else if (os.contains("windows server 2008")) {
                return OperatingSystem.WINDOWS_SERVER_2008;
            } else if (os.contains("windows server 2012")) {
                return OperatingSystem.WINDOWS_SERVER_2012;
            } else if (os.contains("windows server 2016")) {
                return OperatingSystem.WINDOWS_SERVER_2016;
            } else if (os.contains("nt")) {
                return OperatingSystem.WINDOWS_NT;
            } else if (os.contains("windows")) {
                return OperatingSystem.WINDOWS_OTHERS;
            }
        }
        return null;
    }

    private static OperatingSystem getMacOSRelease(final String osName, final String osVersion) {
        if (osName != null) {
            final String os = osName.toLowerCase(Locale.ENGLISH);
            if (os.contains("mac") || os.contains("darwin")) {
                final long version = parseMacOSVersion(osVersion);
                if (version >= 10013000) {
                    return OperatingSystem.MAC_HIGH_SIERRA;
                } else if (version >= 10012000) {
                    return OperatingSystem.MAC_SIERRA;
                } else if (version >= 10011000) {
                    return OperatingSystem.MAC_EL_CAPITAN;
                } else if (version >= 10010000) {
                    return OperatingSystem.MAC_YOSEMITE;
                } else if (version >= 10009000) {
                    return OperatingSystem.MAC_MAVERICKS;
                } else if (version >= 10008000) {
                    return OperatingSystem.MAC_MOUNTAIN_LION;
                } else if (version >= 10007000) {
                    return OperatingSystem.MAC_LION;
                } else if (version >= 10006000) {
                    return OperatingSystem.MAC_SNOW_LEOPOARD;
                } else if (version >= 10005000) {
                    return OperatingSystem.MAC_LEOPOARD;
                } else if (version >= 10004000) {
                    return OperatingSystem.MAC_TIGER;
                } else if (version >= 10003000) {
                    return OperatingSystem.MAC_PANTHER;
                } else if (version >= 10002000) {
                    return OperatingSystem.MAC_JAGUAR;
                } else if (version >= 10001000) {
                    return OperatingSystem.MAC_PUMA;
                } else if (version >= 10000000) {
                    return OperatingSystem.MAC_CHEETAH;
                } else {
                    return OperatingSystem.MAC;
                }
            }
        }
        return null;
    }

    private static OperatingSystem getBSDRelease(final String osName) {
        if (osName != null) {
            final String os = osName.toLowerCase(Locale.ENGLISH);
            if (os.contains("bsd")) {
                if (os.contains("kfreebsd")) {
                    return OperatingSystem.KFREEBSD;
                } else if (os.contains("freebsd")) {
                    return OperatingSystem.FREEBSD;
                } else if (os.contains("netbsd")) {
                    return OperatingSystem.NETBSD;
                } else if (os.contains("openbsd")) {
                    return OperatingSystem.OPENBSD;
                } else if (os.contains("dragonflybsd")) {
                    return OperatingSystem.DRAGONFLYBSD;
                } else {
                    return OperatingSystem.BSD;
                }
            }
        }
        return null;
    }

    /**
     * @param osString
     * @return
     */
    public static OperatingSystem getOSByString(final String osString) {
        if (osString != null) {
            final String os = osString.toLowerCase(Locale.ENGLISH);
            OperatingSystem ret = null;
            if (ret == null && (os.contains("windows") || os.contains("nt"))) {
                ret = getWindowsRelease(os);
            }
            if (ret == null && (os.contains("mac") || os.contains("darwin"))) {
                ret = getMacOSRelease(os, System.getProperty("os.version"));
            }
            if (ret == null && os.contains("bsd")) {
                ret = getBSDRelease(os);
            }
            if (ret != null) {
                return ret;
            } else if (os.contains("os/2")) {
                return OperatingSystem.OS2;
            } else {
                return OperatingSystem.LINUX;
            }
        }
        return OperatingSystem.WINDOWS_8;
    }

    private static ARCHFamily getARCHByString(final String archString) {
        if (archString != null) {
            final String arch = archString.toLowerCase(Locale.ENGLISH);
            if (arch.contains("amd64")) {
                return ARCHFamily.X86;
            } else if (arch.contains("i386") || arch.contains("i686") || arch.contains("i586")) {
                return ARCHFamily.X86;
            } else if (arch.contains("x86")) {
                return ARCHFamily.X86;
            } else if (arch.contains("ppc") || arch.contains("powerpc")) {
                return ARCHFamily.PPC;
            } else if (arch.contains("sparc")) {
                return ARCHFamily.SPARC;
            } else if (arch.contains("arm") || arch.contains("aarch")) {
                return ARCHFamily.ARM;
            } else if (arch.contains("ia64")) {
                return ARCHFamily.IA64;
            }
        }
        return ARCHFamily.NA;
    }

    /**
     * Returns true if the OS is a linux system
     *
     * @return
     */
    public static OSFamily getOSFamily() {
        return CrossSystem.OS.getFamily();
    }

    public static ARCHFamily getARCHFamily() {
        return CrossSystem.ARCH;
    }

    public static String getARCHString() {
        return CrossSystem.ARCH_STRING;
    }

    public static String getOSString() {
        return CrossSystem.OS_STRING;
    }

    public static String[] getPathComponents(File path) throws IOException {
        final LinkedList<String> ret = new LinkedList<String>();
        if (path != null) {
            /*
             * getCanonicalFile once, so we are sure all .././symlinks are evaluated
             */
            try {
                if (!CrossSystem.isForbiddenFilename(path.getName())) {
                    path = path.getCanonicalFile();
                }
            } catch (final IOException e) {
                /**
                 * can happen when drive is not mounted, no cd in drive...
                 */
                e.printStackTrace();
            }
            final String separator = File.separatorChar + "";
            while (path != null) {
                if (path.getPath().endsWith(separator)) {
                    // for example c:\ file.getName() would be "" in this case.
                    ret.add(0, path.getPath());
                    break;
                } else {
                    ret.add(0, path.getName());
                }
                path = path.getParentFile();
            }
        }
        return ret.toArray(new String[] {});
    }

    public static double getSystemCPUUsage() {
        try {
            final java.lang.management.OperatingSystemMXBean operatingSystemMXBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            double sysload = operatingSystemMXBean.getSystemLoadAverage();
            if (sysload < 0) {
                final java.lang.reflect.Method method = operatingSystemMXBean.getClass().getDeclaredMethod("getSystemCpuLoad", new Class[] {});
                method.setAccessible(true);
                sysload = (Double) method.invoke(operatingSystemMXBean, new Object[] {});
            }
            return sysload;
        } catch (final Throwable e) {
            return -1;
        }
    }

    public static long getPID() {
        final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        try {
            final String jvmName = runtimeMXBean.getName();
            final int index = jvmName.indexOf('@');
            /**
             * http://www.golesny.de/p/code/javagetpid
             *
             * @return
             */
            if (index >= 1) {
                return Long.parseLong(jvmName.substring(0, index));
            }
        } catch (Throwable e) {
        }
        try {
            /**
             * http://blog.philippheckel.com/2014/06/14/getting-the-java-process -pid-and-managing-pid-files-linux-windows/
             */
            final Field jvmField = runtimeMXBean.getClass().getDeclaredField("jvm");
            jvmField.setAccessible(true);
            final Object vmManagement = jvmField.get(runtimeMXBean);
            final Method getProcessIdMethod = vmManagement.getClass().getDeclaredMethod("getProcessId");
            getProcessIdMethod.setAccessible(true);
            return (Integer) getProcessIdMethod.invoke(vmManagement);
        } catch (final Throwable e) {
        }
        return -1;
    }

    public static String NEWLINE = null;

    public static String getNewLine() {
        if (NEWLINE == null) {
            String newLine = null;
            try {
                if (Application.getJavaVersion() >= Application.JAVA17) {
                    newLine = System.lineSeparator();
                }
            } catch (final Throwable e) {
            }
            if (StringUtils.isEmpty(newLine)) {
                newLine = System.getProperty("line.separator");
            }
            if (StringUtils.isEmpty(newLine)) {
                switch (CrossSystem.getOSFamily()) {
                case WINDOWS:
                    newLine = "\r\n";
                    break;
                default:
                    newLine = "\n";
                    break;
                }
            }
            NEWLINE = newLine;
            return newLine;
        }
        return NEWLINE;
    }

    public static boolean is64BitArch() {
        final String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");
        if (wow64Arch != null) {
            // cpu architecture
            // null = 32Bit
            // AMD64 = 64Bit
            return wow64Arch.trim().endsWith("64");
        }
        final String arch = System.getenv("PROCESSOR_ARCHITECTURE");
        if (arch != null) {
            // process architecture
            // x86 = 32Bit
            // AMD64 = 64Bit
            return arch.trim().endsWith("64");
        }
        final String osArch = System.getProperty("os.arch");
        final boolean is64bit;
        if (osArch != null) {
            if ("i386".equals(osArch)) {
                is64bit = false;
            } else if ("x86".equals(osArch)) {
                is64bit = false;
            } else if ("sparc".equals(osArch)) {
                is64bit = false;
            } else if ("amd64".equals(osArch)) {
                is64bit = true;
            } else if ("ppc64".equals(osArch)) {
                is64bit = true;
            } else if ("ia64".equals(osArch)) {
                is64bit = true;
            } else if ("amd_64".equals(osArch)) {
                is64bit = true;
            } else if ("x86_64".equals(osArch)) {
                is64bit = true;
            } else if ("sparcv9".equals(osArch)) {
                is64bit = true;
            } else if ("aarch64".equals(osArch)) {
                is64bit = true;
            } else if ("arm64".equals(osArch)) {
                is64bit = true;
            } else {
                is64bit = false;
            }
        } else {
            // no os.arch?!
            is64bit = false;
        }
        return is64bit;
    }

    public static boolean is64BitOperatingSystem() {
        if (CrossSystem.OS64BIT != null) {
            return CrossSystem.OS64BIT;
        }
        if (org.appwork.utils.Application.is64BitJvm()) {
            /*
             * we are running a 64bit jvm, so the underlying os must be 64bit too
             */
            CrossSystem.OS64BIT = true;
            return true;
        } else {
            switch (CrossSystem.getOSFamily()) {
            case BSD:
            case LINUX:
                if (CrossSystem.is64BitArch()) {
                    CrossSystem.OS64BIT = true;
                    return true;
                }
                final String hostType = System.getenv("HOSTTYPE");
                if (hostType != null && hostType.contains("x86_64")) {
                    CrossSystem.OS64BIT = true;
                    return true;
                }
                Process p = null;
                try {
                    final Runtime r = Runtime.getRuntime();
                    p = r.exec("uname -m");
                    p.waitFor();
                    final BufferedReader b = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    final String arch = b.readLine();
                    if (arch != null && arch.contains("x86_64")) {
                        CrossSystem.OS64BIT = true;
                        return true;
                    }
                } catch (final Throwable e) {
                } finally {
                    try {
                        if (p != null) {
                            p.destroy();
                        }
                    } catch (final Throwable e2) {
                    }
                }
                break;
            case WINDOWS:
                if (System.getenv("ProgramFiles(x86)") != null || System.getenv("ProgramW6432") != null) {
                    /* those folders also exist on newer 32bit os */
                    if (CrossSystem.is64BitArch()) {
                        CrossSystem.OS64BIT = true;
                        return true;
                    }
                }
                break;
            default:
                if (CrossSystem.is64BitArch()) {
                    CrossSystem.OS64BIT = true;
                    return true;
                }
                break;
            }
        }
        CrossSystem.OS64BIT = false;
        return false;
    }

    /**
     * checks if given path is absolute or relative
     *
     * @param path
     * @return
     */
    public static boolean isAbsolutePath(final String path) {
        if (StringUtils.isEmpty(path)) {
            return false;
        }
        if ((CrossSystem.isWindows() || CrossSystem.isOS2()) && path.matches("\\\\\\\\.+\\\\.+")) {
            return true;
        }
        if ((CrossSystem.isWindows() || CrossSystem.isOS2()) && path.matches("[a-zA-Z]:/.*")) {
            return true;
        }
        if ((CrossSystem.isWindows() || CrossSystem.isOS2()) && path.matches("[a-zA-Z]:\\\\.*")) {
            return true;
        }
        if (!CrossSystem.isWindows() && !CrossSystem.isOS2() && path.startsWith("/")) {
            return true;
        }
        return false;
    }

    /**
     * @param ks
     * @return
     */
    public static boolean isClearSelectionTrigger(final KeyStroke ks) {
        return ks == CrossSystem.KEY_STROKE_ESCAPE;
    }

    /**
     *
     * /**
     *
     * @param e
     * @return
     */
    public static boolean isContextMenuTrigger(final MouseEvent e) {
        if (CrossSystem.isMac()) {
            if (e.getButton() == MouseEvent.BUTTON1 && e.isControlDown()) {
                return true;
            }
        }
        return e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3;
    }

    /**
     * @param ks
     * @return
     */
    public static boolean isCopySelectionTrigger(final KeyStroke ks) {
        return ks == CrossSystem.KEY_STROKE_COPY;
    }

    /**
     * @param ks
     * @return
     */
    public static boolean isCutSelectionTrigger(final KeyStroke ks) {
        // TODO Auto-generated method stub
        return ks == CrossSystem.KEY_STROKE_CUT;
    }

    /**
     * @param ks
     * @return
     */
    public static boolean isDeleteFinalSelectionTrigger(final KeyStroke ks) {
        if (CrossSystem.isMac()) {
            if (ks == KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | ActionEvent.SHIFT_MASK)) {
                return true;
            }
        }
        return ks == CrossSystem.KEY_STROKE_FORCE_DELETE;
    }

    /**
     * @param e
     * @return
     */
    public static boolean isDeleteSelectionTrigger(final KeyEvent e) {
        return CrossSystem.isDeleteSelectionTrigger(KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiers()));
    }

    /**
     * @param ks
     * @return
     */
    public static boolean isDeleteSelectionTrigger(final KeyStroke ks) {
        if (CrossSystem.isMac()) {
            if (ks == CrossSystem.KEY_STROKE_BACKSPACE_CTRL) {
                return true;
            }
        }
        return ks == CrossSystem.KEY_STROKE_DELETE;
    }

    public static boolean isLinux() {
        return CrossSystem.OS.getFamily() == OSFamily.LINUX;
    }

    public static boolean isBSD() {
        return CrossSystem.OS.getFamily() == OSFamily.BSD;
    }

    /**
     * Returns true if the OS is a MAC System
     *
     * @return
     */
    public static boolean isMac() {
        return CrossSystem.OS.getFamily() == OSFamily.MAC;
    }

    /**
     * returns true in case of "open an URL in a browser" is supported
     *
     * @return
     */
    public static boolean isOpenBrowserSupported() {
        return CrossSystem.DESKTOP_SUPPORT.isBrowseURLSupported() || (CrossSystem.getBrowserCommandLine() != null && CrossSystem.getBrowserCommandLine().length > 0);
    }

    public static DesktopSupport getDesktopSupport() {
        return DESKTOP_SUPPORT;
    }

    /**
     * returns true in case of "open a File" is supported
     *
     * @return
     */
    public static boolean isOpenFileSupported() {
        return CrossSystem.DESKTOP_SUPPORT.isOpenFileSupported();
    }

    public static boolean isOS2() {
        return CrossSystem.OS.getFamily() == OSFamily.OS2;
    }

    /**
     * @param ks
     * @return
     */
    public static boolean isPasteSelectionTrigger(final KeyStroke ks) {
        // TODO Auto-generated method stub
        return ks == CrossSystem.KEY_STROKE_PASTE;
    }

    /**
     * @param ks
     * @return
     */
    public static boolean isSearchTrigger(final KeyStroke ks) {
        // TODO Auto-generated method stub
        return ks == CrossSystem.KEY_STROKE_SEARCH;
    }

    /**
     * @param ks
     * @return
     */
    public static boolean isSelectionAllTrigger(final KeyStroke ks) {
        // TODO Auto-generated method stub
        return ks == CrossSystem.KEY_STROKE_SELECT_ALL;
    }

    /**
     * @param ks
     * @return
     */
    public static boolean isSelectionDownTrigger(final KeyStroke ks) {
        // TODO Auto-generated method stub
        return ks == CrossSystem.KEY_STROKE_DOWN;
    }

    /**
     * @param ks
     * @return
     */
    public static boolean isSelectionUpTrigger(final KeyStroke ks) {
        // TODO Auto-generated method stub
        return ks == CrossSystem.KEY_STROKE_UP;
    }

    /**
     * Returns true if the OS is a Windows System
     *
     * @return
     */
    public static boolean isWindows() {
        return CrossSystem.OS.getFamily() == OSFamily.WINDOWS;
    }

    protected static boolean openCustom(final String[] custom, final String what) throws IOException {
        if (custom == null || custom.length < 1 || what == null) {
            return false;
        }
        boolean added = false;
        final List<String> commands = new ArrayList<String>();
        for (final String s : custom) {
            if (s != null) {
                final String add = s.replace("%s", what);
                if (!add.equals(s)) {
                    added = true;
                }
                commands.add(add);
            }
        }
        if (commands.size() > 0) {
            if (added == false) {
                commands.add(what);
            }
            Runtime.getRuntime().exec(commands.toArray(new String[] {}));
            return true;
        } else {
            return false;
        }
    }

    /**
     * Opens a file or directory
     *
     * @see java.awt.Desktop#open(File)
     * @param file
     * @throws IOException
     */
    public static void openFile(final File file) {
        // I noticed a bug: desktop.open freezes under win7 java 1.7u25 in some
        // cases... we should at least avoid a gui freeze in such cases..
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    CrossSystem._openFILE(file);
                } catch (final IOException e) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                }
            }
        };
        if (CrossSystem.isWindows()) {
            new Thread(runnable, "Open Folder").start();
        } else {
            runnable.run();
        }
    }

    /**
     * Open an url in the systems default browser
     *
     * @param url
     */
    public static void openURL(final String url) {
        try {
            CrossSystem.openUrlOrThrowException(url);
        } catch (final Throwable e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
        }
    }

    public static void openURL(final URL url) {
        CrossSystem.openURL(url.toString());
    }

    /**
     * @param update_dialog_news_button_url
     */
    public static void openURLOrShowMessage(final String urlString) {
        try {
            CrossSystem.openUrlOrThrowException(urlString);
        } catch (final Throwable e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
            try {
                final String question = _AWU.T.crossSystem_open_url_failed_msg();
                final InputDialog dialog = new InputDialog(UIOManager.LOGIC_COUNTDOWN | UIOManager.BUTTONS_HIDE_CANCEL, _AWU.T.DIALOG_INPUT_TITLE(), question, urlString, Dialog.getIconByText(question), null, null);
                dialog.setTimeout(61 * 1000);
                UIOManager.I().show(InputDialogInterface.class, dialog);
            } catch (final Throwable donothing) {
            }
        }
    }

    /**
     * @param class1
     */
    public static void restartApplication(final File jar, final String... parameters) {
        try {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info("restartApplication " + jar + " " + parameters.length);
            final java.util.List<String> nativeParameters = new ArrayList<String>();
            File runin = null;
            if (CrossSystem.isMac()) {
                // find .app
                File rootpath = jar;
                final HashSet<File> loopMap = new HashSet<File>();
                while (rootpath != null && loopMap.add(rootpath)) {
                    if (rootpath.getName().endsWith(".app")) {
                        break;
                    }
                    rootpath = rootpath.getParentFile();
                }
                if (rootpath.getName().endsWith(".app")) {
                    // found app.- restart it.
                    nativeParameters.add("open");
                    nativeParameters.add("-n");
                    nativeParameters.add(rootpath.getAbsolutePath());
                    runin = rootpath.getParentFile();
                }
            }
            if (nativeParameters.isEmpty()) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info("Find Jarfile");
                final File jarFile = jar;
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info("Find Jarfile " + jarFile);
                runin = jarFile.getParentFile();
                if (CrossSystem.isWindows() || CrossSystem.isOS2()) {
                    final File exeFile = new File(jarFile.getParentFile(), jarFile.getName().substring(0, jarFile.getName().length() - 4) + ".exe");
                    if (exeFile.exists()) {
                        nativeParameters.add(exeFile.getAbsolutePath());
                    } else {
                        nativeParameters.add(CrossSystem.getJavaBinary());
                        nativeParameters.add("-jar");
                        nativeParameters.add(jarFile.getAbsolutePath());
                    }
                } else {
                    nativeParameters.add(CrossSystem.getJavaBinary());
                    nativeParameters.add("-jar");
                    nativeParameters.add(jarFile.getAbsolutePath());
                }
            }
            if (parameters != null) {
                for (final String s : parameters) {
                    nativeParameters.add(s);
                }
            }
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info("Start " + nativeParameters);
            final ProcessBuilder pb = ProcessBuilderFactory.create(nativeParameters.toArray(new String[] {}));
            /*
             * needed because the root is different for jre/class version
             */
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info("Root: " + runin);
            if (runin != null) {
                pb.directory(runin);
            }
            ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {
                {
                    this.setHookPriority(Integer.MIN_VALUE);
                }

                @Override
                public void onShutdown(final ShutdownRequest shutdownRequest) {
                    try {
                        pb.start();
                    } catch (final IOException e) {
                        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                    }
                }
            });
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info("Start " + ShutdownController.getInstance().requestShutdown(true));
        } catch (final Throwable e) {
            throw new WTFException(e);
        }
    }

    /**
     * Set commandline to open the browser use %s as wildcard for the url
     *
     * @param commands
     */
    public static void setBrowserCommandLine(final String[] commands) {
        CrossSystem.BROWSER_COMMANDLINE = commands;
    }

    public static void setFileCommandLine(final String[] fILE_COMMANDLINE) {
        CrossSystem.FILE_COMMANDLINE = fILE_COMMANDLINE;
    }

    /**
     * @param saveTo
     */
    public static void showInExplorer(final File saveTo) {
        if (saveTo.exists()) {
            if (CrossSystem.isWindows()) {
                try {
                    // we need to go this cmd /c way, because explorer.exe seems to
                    // do some strange parameter parsing.
                    new ProcessBuilder("cmd", "/c", "explorer /select,\"" + saveTo.getAbsolutePath() + "\"").start();
                    return;
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            } else if (CrossSystem.isMac()) {
                try {
                    ProcessBuilderFactory.create("open", "-R", saveTo.getAbsolutePath()).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (saveTo.isDirectory()) {
            CrossSystem.openFile(saveTo);
        } else {
            CrossSystem.openFile(saveTo.getParentFile());
        }
    }

    /**
     * splits filename into name,extension
     *
     * @param filename
     * @return
     */
    public static String[] splitFileName(final String filename) {
        final String extension = new Regex(filename, "\\.+([^\\.]*$)").getMatch(0);
        final String name = new Regex(filename, "(.*?)(\\.+[^\\.]*$|$)").getMatch(0);
        return new String[] { name, extension };
    }

    public static void standbySystem() {
        CrossSystem.DESKTOP_SUPPORT.standby();
    }

    public static void hibernateSystem() {
        CrossSystem.DESKTOP_SUPPORT.hibernate();
    }

    public static void shutdownSystem(final boolean force) {
        CrossSystem.DESKTOP_SUPPORT.shutdown(force);
    }

    /**
     * @return
     * @throws SecuritySoftwareException
     */
    public static SecuritySoftwareResponse getAntiVirusSoftwareInfo() throws UnsupportedOperationException, SecuritySoftwareException {
        String response = null;
        try {
            if (!CrossSystem.isWindows()) {
                throw new UnsupportedOperationException("getAntiVirusSoftwareInfo: Not Supported for your OS");
            }
            switch (CrossSystem.getOS()) {
            case WINDOWS_XP:
                response = ProcessBuilderFactory.runCommand(WMIC_PATH, "/NAMESPACE:\\\\root\\SecurityCenter", "path", "AntiVirusProduct", "get", "companyName,displayName,pathToEnableOnAccessUI,pathToUpdateUI,productUptoDate", "/format:value").getStdOutString();
                break;
            default:
                response = ProcessBuilderFactory.runCommand(WMIC_PATH, "/NAMESPACE:\\\\root\\SecurityCenter2", "path", "AntiVirusProduct", "get", "displayName,pathToSignedProductExe,pathToSignedReportingExe,productState", "/format:value").getStdOutString();
                break;
            }
            return parseWindowWMIResponse(response, null);
        } catch (UnsupportedOperationException e) {
            throw e;
        } catch (Throwable e) {
            throw new SecuritySoftwareException(e, response);
        }
    }

    public static SecuritySoftwareResponse parseWindowWMIResponse(final String response, OperatingSystem os) {
        if (os == null) {
            os = CrossSystem.getOS();
        }
        SecuritySoftwareResponse list = new SecuritySoftwareResponse();
        list.setResponse(response);
        if (StringUtils.isNotEmpty(response)) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info(response);
            String[] lines = response.split("[\r\n]{1,2}");
            String firstKey = null;
            SecuritySoftwareInfo ret = new SecuritySoftwareInfo();
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (StringUtils.isNotEmpty(line)) {
                    int index = line.indexOf("=");
                    if (index > 0) {
                        String key = line.substring(0, index);
                        String value = line.substring(index + 1);
                        if (firstKey != null && firstKey.equals(key)) {
                            list.add(ret);
                            ret = new SecuritySoftwareInfo();
                        }
                        if (firstKey == null) {
                            firstKey = key;
                        }
                        ret.put(key, value);
                    }
                }
            }
            if (ret.size() > 0) {
                list.add(ret);
            }
            return list;
        }
        throw new WTFException("WMIC returned no response");
    }

    /**
     * @return
     * @throws SecuritySoftwareException
     *
     */
    public static SecuritySoftwareResponse getFirewallSoftwareInfo() throws UnsupportedOperationException, SecuritySoftwareException {
        String response = null;
        try {
            if (!CrossSystem.isWindows()) {
                throw new UnsupportedOperationException("getFirewallSoftwareInfo: Not Supported for your OS");
            }
            switch (CrossSystem.getOS()) {
            case WINDOWS_XP:
                response = ProcessBuilderFactory.runCommand(WMIC_PATH, "/NAMESPACE:\\\\root\\SecurityCenter", "path", "FirewallProduct", "get", "companyName,displayName,enabled,pathToEnableUI", "/format:value").getStdOutString();
                break;
            default:
                response = ProcessBuilderFactory.runCommand(WMIC_PATH, "/NAMESPACE:\\\\root\\SecurityCenter2", "path", "FirewallProduct", "get", "displayName,pathToSignedProductExe,pathToSignedProductExe,pathToSignedReportingExe,productState", "/format:value").getStdOutString();
                break;
            }
            return parseWindowWMIResponse(response, null);
        } catch (UnsupportedOperationException e) {
            throw e;
        } catch (Throwable e) {
            throw new SecuritySoftwareException(e, response);
        }
    }

    /**
     * @return
     * @throws SecuritySoftwareException
     *
     */
    public static SecuritySoftwareResponse getAntiSpySoftwareInfo() throws UnsupportedOperationException, SecuritySoftwareException {
        String response = null;
        try {
            if (!CrossSystem.isWindows()) {
                throw new UnsupportedOperationException("getAntiSpySoftwareInfo: Not Supported for your OS");
            }
            switch (CrossSystem.getOS()) {
            case WINDOWS_XP:
                throw new UnsupportedOperationException("getAntiSpySoftwareInfo: Not Supported for your OS");
            default:
                response = ProcessBuilderFactory.runCommand(WMIC_PATH, "/NAMESPACE:\\\\root\\SecurityCenter2", "path", "AntiSpywareProduct", "get", "displayName,pathToSignedProductExe,pathToSignedProductExe,pathToSignedReportingExe,productState", "/format:value").getStdOutString();
                break;
            }
            return parseWindowWMIResponse(response, null);
        } catch (UnsupportedOperationException e) {
            throw e;
        } catch (Throwable e) {
            throw new SecuritySoftwareException(e, response);
        }
    }

    public static boolean isProcessRunning(String path) throws UnexpectedResponseException {
        String response = null;
        try {
            if (!CrossSystem.isWindows()) {
                throw new UnsupportedOperationException("isProcessRunning: Not Supported for your OS");
            }
            switch (CrossSystem.getOS()) {
            default:
                response = ProcessBuilderFactory.runCommand(WMIC_PATH, "process", "where", "executablepath='" + path.replaceAll("[\\/\\\\]+", "\\\\\\\\") + "'", "get", "processID", "/format:value").getStdOutString();
                break;
            }
            if (StringUtils.isNotEmpty(response) && response.contains("ProcessId=")) {
                return true;
            } else if (StringUtils.isEmpty(response)) {
                return false;
            }
        } catch (UnsupportedOperationException e) {
            throw e;
        } catch (Throwable e) {
            throw new WTFException(e);
        }
        throw new UnexpectedResponseException("Unexpected Response: " + response);
    }

    /**
     *
     */
    public static void playErrorSound() {
        if (getOSFamily() == OSFamily.WINDOWS) {
            final Runnable runnable = (Runnable) Toolkit.getDefaultToolkit().getDesktopProperty("win.sound.exclamation");
            if (runnable != null) {
                runnable.run();
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    /**
     * tries to generate an hardware ID from the available network Interfaces. may return NULL
     *
     * @return
     * @throws SocketException
     */
    public static String generateNetworkIdentifier() {
        try {
            Enumeration<NetworkInterface> netifs;
            String netID = null;
            // LoggerFactory.getDefaultLogger().info("Scan MACs");
            netifs = NetworkInterface.getNetworkInterfaces();
            // final StringBuilder macs = new StringBuilder();
            HashSet<String> dupes = new HashSet<String>();
            ArrayList<String> list = new ArrayList<String>();
            while (netifs.hasMoreElements()) {
                final NetworkInterface netif = netifs.nextElement();
                // LoggerFactory.getDefaultLogger().info("* " + netif);
                if (netif.isLoopback()) {
                    // LoggerFactory.getDefaultLogger().info("- Loopback");
                    continue;
                }
                if (netif.isPointToPoint()) {
                    // LoggerFactory.getDefaultLogger().info("- P2P");
                    continue;
                }
                if (netif.isVirtual()) {
                    // LoggerFactory.getDefaultLogger().info("- VIRT");
                    continue;
                }
                String macString;
                final byte[] macBytes = netif.getHardwareAddress();
                if (macBytes != null) {
                    macString = HexFormatter.byteArrayToHex(macBytes).toLowerCase(Locale.ENGLISH);
                    if (dupes.add(macString)) {
                        // LoggerFactory.getDefaultLogger().info("+ " + macString);
                        list.add(macString.toUpperCase(Locale.ENGLISH));
                        continue;
                    } else {
                        // LoggerFactory.getDefaultLogger().info("- MACDUPE");
                    }
                } else {
                    // LoggerFactory.getDefaultLogger().info("- NOMAC");
                }
            }
            Collections.sort(list);
            StringBuilder sb = new StringBuilder();
            for (String s : list) {
                sb.append(s);
                sb.append("\r\n");
            }
            netID = Base64.encodeToString(HexFormatter.hexToByteArray(Hash.getMD5(sb.toString())), false);
            return netID;
        } catch (Throwable e) {
            LoggerFactory.getDefaultLogger().log(e);
        }
        return null;
    }

    /**
     * @return
     */
    public static String generateHardwareID() {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA256");
            md.update(Base64.decodeFast(generateNetworkIdentifier()));
            md.update((byte) Runtime.getRuntime().availableProcessors());
            return Base64.encodeToString(md.digest(), false);
        } catch (Throwable e) {
            throw new WTFException(e);
        }
    }
}