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

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.IO;
import org.appwork.utils.os.CrossSystem.OperatingSystem;
import org.appwork.utils.processes.ProcessBuilderFactory;
import org.appwork.utils.processes.ProcessOutput;

/**
 * @author daniel
 *
 */
public class DesktopSupportWindows extends DesktopSupportJavaDesktop {
    @Override
    public void browseURL(final URL url) throws IOException, URISyntaxException {
        try {
            // it seems that unicode filenames cannot be opened with
            // "rundll32.exe", "url.dll,FileProtocolHandler". let's try
            // Desktop.open first
            Desktop.getDesktop().browse(url.toURI());
        } catch (final Throwable e) {
            ProcessBuilderFactory.create("rundll32.exe", "url.dll,FileProtocolHandler", url.toExternalForm()).start();
        }
    }

    @Override
    public boolean isBrowseURLSupported() {
        return true;
    }

    @Override
    public boolean isOpenFileSupported() {
        return true;
    }

    @Override
    public void openFile(final File file) throws IOException {
        // workaround for windows
        // see http://bugs.sun.com/view_bug.do?bug_id=6599987
        if (!file.exists()) {
            throw new IOException("File does not exist " + file.getAbsolutePath());
        }
        try {
            // it seems that unicode filenames cannot be opened with
            // "rundll32.exe", "url.dll,FileProtocolHandler". let's try
            // this call works for unicode paths as well.
            // the " " parameter is a dummy parameter to represent the window
            // name. without it, paths with space will fail
            ProcessBuilderFactory.create("cmd", "/c", "start", "/B", " ", file.getCanonicalPath()).start();
            // desktop.open might freeze in WDesktopPeer.open....bla on win7
            // java 1.7u25
            // Desktop.getDesktop().open(file);
        } catch (final Exception e) {
            ProcessBuilderFactory.create("rundll32.exe", "url.dll,FileProtocolHandler", file.getCanonicalPath()).start();
        }
    }

    @Override
    public boolean shutdown(boolean force) {
        switch (CrossSystem.OS) {
        case WINDOWS_2003:
        case WINDOWS_VISTA:
        case WINDOWS_XP:
        case WINDOWS_7:
        case WINDOWS_8:
        case WINDOWS_SERVER_2012:
            /* modern windows versions */
        case WINDOWS_2000:
        case WINDOWS_NT:
        case WINDOWS_SERVER_2008:
            /* not so modern windows versions */
            if (force) {
                /* force shutdown */
                try {
                    ProcessBuilderFactory.runCommand(new String[] { "shutdown.exe", "-s", "-f", "-t", "01" });
                } catch (Exception e) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                }
                try {
                    ProcessBuilderFactory.runCommand(new String[] { "%windir%\\system32\\shutdown.exe", "-s", "-f", "-t", "01" });
                } catch (Exception e) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                }
            } else {
                /* normal shutdown */
                try {
                    ProcessBuilderFactory.runCommand(new String[] { "shutdown.exe", "-s", "-t", "01" });
                } catch (Exception e) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                }
                try {
                    ProcessBuilderFactory.runCommand(new String[] { "%windir%\\system32\\shutdown.exe", "-s", "-t", "01" });
                } catch (Exception e) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                }
            }
            if (CrossSystem.OS == OperatingSystem.WINDOWS_2000 || CrossSystem.OS == OperatingSystem.WINDOWS_NT) {
                /* also try extra methods for windows2000 and nt */
                try {
                    File f = Application.getTempResource("shutdown.vbs");
                    f.deleteOnExit();
                    IO.writeStringToFile(f, "set WshShell = CreateObject(\"WScript.Shell\")\r\nWshShell.SendKeys \"^{ESC}^{ESC}^{ESC}{UP}{ENTER}{ENTER}\"\r\n");
                    try {
                        ProcessBuilderFactory.runCommand(new String[] { "cmd", "/c", "start", "/min", "cscript", f.getAbsolutePath() });
                    } finally {
                        Files.deleteRecursiv(f);
                    }
                } catch (Exception e) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                }
            }
            break;
        case WINDOWS_OTHERS:
            /* older windows versions */
            try {
                ProcessBuilderFactory.runCommand(new String[] { "RUNDLL32.EXE", "user,ExitWindows" });
            } catch (Exception e) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
            }
            try {
                ProcessBuilderFactory.runCommand(new String[] { "RUNDLL32.EXE", "Shell32,SHExitWindowsEx", "1" });
            } catch (Exception e) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
            }
            break;
        }
        return true;
    }

    @Override
    public boolean standby() {
        switch (CrossSystem.OS) {
        case WINDOWS_2003:
        case WINDOWS_VISTA:
        case WINDOWS_XP:
        case WINDOWS_7:
        case WINDOWS_8:
            /* modern windows versions */
        case WINDOWS_2000:
        case WINDOWS_NT:
            /* not so modern windows versions */
            if (isHibernateActivated()) {
                // hibernate activated? -> disable it
                String path = CrossSystem.is64BitOperatingSystem() ? Application.getResource("tools\\Windows\\elevate\\Elevate64.exe").getAbsolutePath() : Application.getResource("tools\\Windows\\elevate\\Elevate32.exe").getAbsolutePath();
                try {
                    ProcessBuilderFactory.runCommand(new String[] { path, "powercfg", "-hibernate", "off" });
                } catch (Throwable e) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                }
            }
            try {
                ProcessBuilderFactory.runCommand(new String[] { "powercfg.exe", "hibernate off" });
            } catch (Exception e) {
                try {
                    ProcessBuilderFactory.runCommand(new String[] { "%windir%\\system32\\powercfg.exe", "hibernate off" });
                } catch (Exception ex) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(ex);
                }
            }
            try {
                ProcessBuilderFactory.runCommand(new String[] { "RUNDLL32.EXE", "powrprof.dll,SetSuspendState" });
            } catch (Exception e) {
                try {
                    ProcessBuilderFactory.runCommand(new String[] { "%windir%\\system32\\RUNDLL32.EXE", "powrprof.dll,SetSuspendState" });
                } catch (Exception e1) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                }
            }
            break;
        case WINDOWS_OTHERS:
            /* older windows versions */
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info("no standby support, use shutdown");
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.utils.os.DesktopSupport#hibernate()
     */
    @Override
    public boolean hibernate() {
        switch (CrossSystem.OS) {
        case WINDOWS_2003:
        case WINDOWS_VISTA:
        case WINDOWS_XP:
        case WINDOWS_7:
        case WINDOWS_8:
            /* modern windows versions */
        case WINDOWS_2000:
        case WINDOWS_NT:
            /* not so modern windows versions */
            if (!DesktopSupportWindows.isHibernateActivated()) {
                // enable hibernate
                String path = CrossSystem.is64BitOperatingSystem() ? Application.getResource("tools\\Windows\\elevate\\Elevate64.exe").getAbsolutePath() : Application.getResource("tools\\Windows\\elevate\\Elevate32.exe").getAbsolutePath();
                try {
                    ProcessBuilderFactory.runCommand(new String[] { path, "powercfg", "-hibernate", "on" });
                } catch (Throwable e) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                }
            }
            try {
                ProcessBuilderFactory.runCommand(new String[] { "powercfg.exe", "hibernate on" });
            } catch (Exception e) {
                try {
                    ProcessBuilderFactory.runCommand(new String[] { "%windir%\\system32\\powercfg.exe", "hibernate on" });
                } catch (Exception ex) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(ex);
                }
            }
            try {
                ProcessBuilderFactory.runCommand(new String[] { "RUNDLL32.EXE", "powrprof.dll,SetSuspendState" });
            } catch (Exception e) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                try {
                    ProcessBuilderFactory.runCommand(new String[] { "%windir%\\system32\\RUNDLL32.EXE", "powrprof.dll,SetSuspendState" });
                } catch (Exception ex) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(ex);
                }
            }
            break;
        case WINDOWS_OTHERS:
            /* older windows versions */
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info("no hibernate support, use shutdown");
            return false;
        }
        return true;
    }

    private static boolean isHibernateActivated() {
        ProcessOutput status;
        try {
            status = ProcessBuilderFactory.runCommand(new String[] { "powercfg", "-a" });
            // we should add the return for other languages
            if (status.getStdOutString() != null) {
                if (status.getStdOutString().contains("Ruhezustand wurde nicht aktiviert")) {
                    return false;
                }
                if (status.getStdOutString().contains("Hibernation has not been enabled")) {
                    return false;
                }
                if (status.getStdOutString().contains("Hibernation")) {
                    return false;
                }
            }
        } catch (Exception e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
        }
        return true;
    }
}
