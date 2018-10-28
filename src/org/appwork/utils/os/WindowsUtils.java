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
package org.appwork.utils.os;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.prefs.Preferences;

import javax.security.auth.login.LoginException;

import org.appwork.utils.StringUtils;

import com.sun.security.auth.module.NTLoginModule;

/**
 * @author Thomas
 * @date 14.10.2018
 *
 */
public class WindowsUtils {
    public static final String SID_LOCAL_SYSTEM  = "S-1-5-18";
    public static final String SID_USER          = "S-1-5-32-545";
    public static final String SID_EVERYBODYY    = "S-1-1-0";
    public static final String SID_ADMINISTRATOR = "S-1-5-32-544";

    /**
     * Returns true, of the current user is part of the Administrators group. <br>
     * WARNING: This does NOT Mean that the process has admin priviledges. use {@link #hasCurrentProcessAdminPriviledges()} instead <br>
     * Also check {@link #isCurrentUserPartOfGroup(String)}
     *
     * @return
     */
    public static boolean isAdmin() {
        return isCurrentUserPartOfGroup(SID_ADMINISTRATOR);
    }

    /**
     * Tests if the current user is part of a SID group (e.g. {@link #SID_EVERYBODYY})
     *
     * @param sid
     * @return
     */
    public static boolean isCurrentUserPartOfGroup(String sid) {
        String groups[] = (new com.sun.security.auth.module.NTSystem()).getGroupIDs();
        for (String group : groups) {
            if (StringUtils.equals(sid, group)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) throws LoginException {
        NTLoginModule loginContext = new NTLoginModule();
        try {
            loginContext.login();
            System.out.println("You are real!");
            // Subject subject = loginContext.getSubject();
        } catch (LoginException e) {
            System.err.append("Authentication failed: ").println(e.getMessage());
        }
    }

    public static boolean hasCurrentProcessAdminPriviledges() {
        if (!isAdmin()) {
            return false;
        }
        PrintStream oldSTream = System.err;
        System.setErr(new PrintStream(new ByteArrayOutputStream()));
        // this might throw this error:
        // Sep 14, 2018 12:58:42 PM java.util.prefs.WindowsPreferences openKey
        // WARNING: Could not open windows registry node Software\JavaSoft\Prefs at root 0x80000002. Windows RegOpenKey(...) returned error
        // code 5.
        Preferences prefs = Preferences.systemRoot();
        try {
            prefs.put("foo", "bar"); // SecurityException on Windows
            prefs.remove("foo");
            prefs.flush(); // BackingStoreException on Linux
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            System.setErr(oldSTream);
        }
    }
}
