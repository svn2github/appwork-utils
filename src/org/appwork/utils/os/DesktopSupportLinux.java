/**
 * Copyright (c) 2009 - 2012 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 *
 * This file is part of org.appwork.utils.os
 *
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.os;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

import org.appwork.utils.StringUtils;
import org.appwork.utils.logging.Log;
import org.appwork.utils.processes.ProcessBuilderFactory;
import org.appwork.utils.processes.ProcessOutput;

/**
 * @author daniel
 *
 */
public class DesktopSupportLinux implements DesktopSupport {

    public static enum WINDOW_MANAGER {
        XFCE,
        GNOME,
        MATE,
        UNITY,
        KDE,
        UNKNOWN
    }

    private final DesktopSupportJavaDesktop fallBack = new DesktopSupportJavaDesktop();
    private final String[]                  customFile;
    private final String[]                  customBrowse;
    private final WINDOW_MANAGER            windowManager;

    public DesktopSupportLinux() {
        /* java vm property */
        final String sunDesktop = System.getProperty("sun.desktop");
        /* see http://standards.freedesktop.org/menu-spec/latest/apb.html */
        final String XDG_CURRENT_DESKTOP = System.getenv("XDG_CURRENT_DESKTOP");
        final String GNOME_DESKTOP_SESSION_ID = System.getenv("GNOME_DESKTOP_SESSION_ID");
        /* returns true in case we have running KDE */
        final String KDE_FULL_SESSION = System.getenv("KDE_FULL_SESSION");
        /* gnome session */
        final String GDMSESSION = System.getenv("GDMSESSION");
        final String DESKTOP_SESSION = System.getenv("DESKTOP_SESSION");
        if ("Unity".equals(XDG_CURRENT_DESKTOP) || "ubuntu".equals(GDMSESSION) || "ubuntu-2d".equals(GDMSESSION)) {
            if ("ubuntu-2d".equals(GDMSESSION)) {
                System.out.println("Unity-2D Desktop detected");
            } else {
                System.out.println("Unity-3D Desktop detected");
            }
            this.windowManager = WINDOW_MANAGER.UNITY;
            this.customFile = new String[] { "gnome-open", "%s" };
            this.customBrowse = new String[] { "gnome-open", "%s" };
        } else if ("GNOME".equalsIgnoreCase(XDG_CURRENT_DESKTOP) || StringUtils.isNotEmpty(GNOME_DESKTOP_SESSION_ID) || "GNOME".equalsIgnoreCase(GDMSESSION) || "gnome-shell".equals(GDMSESSION) || "gnome-classic".equals(GDMSESSION) || "gnome-fallback".equals(GDMSESSION) || "cinnamon".equals(GDMSESSION)) {
            System.out.println("Gnome Desktop detected");
            this.windowManager = WINDOW_MANAGER.GNOME;
            this.customFile = new String[] { "gnome-open", "%s" };
            this.customBrowse = new String[] { "gnome-open", "%s" };
        } else if ("mate".equalsIgnoreCase(XDG_CURRENT_DESKTOP) || "mate".equalsIgnoreCase(DESKTOP_SESSION)) {
            System.out.println("Mate Desktop detected");
            this.windowManager = WINDOW_MANAGER.MATE;
            this.customFile = new String[] { "gnome-open", "%s" };
            this.customBrowse = new String[] { "gnome-open", "%s" };
        } else if ("true".equals(KDE_FULL_SESSION) || "kde-plasma".equals(DESKTOP_SESSION)) {
            System.out.println("KDE detected");
            this.windowManager = WINDOW_MANAGER.KDE;
            this.customFile = new String[] { "kde-open", "%s" };
            this.customBrowse = new String[] { "kde-open", "%s" };
        } else if ("XFCE".equals(XDG_CURRENT_DESKTOP)) {
            System.out.println("XFCE detected");
            this.windowManager = WINDOW_MANAGER.XFCE;
            this.customFile = new String[] { "xdg-open", "%s" };
            this.customBrowse = new String[] { "xdg-open", "%s" };
        } else {
            System.out.println("sun.Desktop: " + sunDesktop);
            System.out.println("XDG_CURRENT_DESKTOP: " + XDG_CURRENT_DESKTOP);
            System.out.println("GNOME_DESKTOP_SESSION_ID: " + GNOME_DESKTOP_SESSION_ID);
            System.out.println("KDE_FULL_SESSION: " + KDE_FULL_SESSION);
            System.out.println("DESKTOP_SESSION: " + DESKTOP_SESSION);
            this.windowManager = WINDOW_MANAGER.UNKNOWN;
            this.customFile = null;
            this.customBrowse = null;
        }
    }

    @Override
    public void browseURL(final URL url) throws IOException, URISyntaxException {
        if (this.openCustom(this.customBrowse, url.toExternalForm())) {
            return;
        }
        this.fallBack.browseURL(url);
    }

    public WINDOW_MANAGER getWindowManager() {
        return this.windowManager;
    }

    @Override
    public boolean isBrowseURLSupported() {
        if (this.customBrowse != null && this.customFile.length >= 2 || this.fallBack.isBrowseURLSupported()) {
            return true;
        }
        return false;
    }

    public boolean isGnomeDesktop() {
        switch (this.windowManager) {
        case GNOME:
        case MATE:
        case UNITY:
            return true;
        }
        return false;
    }

    public boolean isKDEDesktop() {
        switch (this.windowManager) {
        case KDE:
            return true;
        }
        return false;
    }

    @Override
    public boolean isOpenFileSupported() {
        if (this.customFile != null && this.customFile.length >= 2 || this.fallBack.isOpenFileSupported()) {
            return true;
        }
        return false;
    }

    public boolean isXFCEDesktop() {
        switch (this.windowManager) {
        case XFCE:
            return true;
        }
        return false;
    }

    private boolean openCustom(final String[] custom, final String what) throws IOException {
        if (custom == null || custom.length < 1) {
            return false;
        }
        boolean added = false;
        final java.util.List<String> commands = new ArrayList<String>();
        for (final String s : custom) {
            final String add = s.replace("%s", what);
            if (!add.equals(s)) {
                added = true;
            }
            commands.add(add);
        }
        if (added == false) {
            commands.add(what);
        }
        Runtime.getRuntime().exec(commands.toArray(new String[] {}));
        return true;
    }

    @Override
    public void openFile(final File file) throws IOException {
        if (this.openCustom(this.customFile, file.getAbsolutePath())) {
            return;
        }
        this.fallBack.openFile(file);
    }

    @Override
    public boolean shutdown(boolean force) {
        try {
            dbusPowerState("Shutdown");
        } catch (Exception e) {
            Log.exception(e);
        }
        try {
            ProcessBuilderFactory.runCommand(new String[] { "dcop", "--all-sessions", "--all-users", "ksmserver", "ksmserver", "logout", "0", "2", "0" });
        } catch (Exception e) {
            Log.exception(e);
        }
        try {
            ProcessBuilderFactory.runCommand("poweroff");
        } catch (Exception e) {
            Log.exception(e);
        }
        try {
            ProcessBuilderFactory.runCommand(new String[] { "sudo", "shutdown", "-P", "now" });
        } catch (Exception e) {
            Log.exception(e);
        }
        return true;
    }

    private void dbusPowerState(String command) {
        try {
            ProcessOutput output = ProcessBuilderFactory.runCommand(new String[] { "dbus-send", "--session", "--dest=org.freedesktop.PowerManagement", "--type=method_call", "--print-reply", "--reply-timeout=2000", "/org/freedesktop/PowerManagement", "org.freedesktop.PowerManagement." + command });
            if (output.getErrOutString("UTF-8").contains("org.freedesktop.DBus.Error.ServiceUnknown")) {
                // compatible to newer dbus versions
                ProcessBuilderFactory.runCommand("dbus-send", "--system", "--print-reply", "--dest=org.freedesktop.login1", "/org/freedesktop/login1", "org.freedesktop.login1.Manager." + command, "boolean:true");
            }
        } catch (Exception e) {
            Log.exception(e);
        }
    }

    @Override
    public boolean standby() {
        try {
            dbusPowerState("Suspend");
            return true;
        } catch (Exception e) {
            Log.exception(e);
            Log.L.info("no standby support, use shutdown");
            return false;
        }
    }

    @Override
    public boolean hibernate() {
        try {
            dbusPowerState("Hibernate");
            return true;
        } catch (Exception e) {
            Log.exception(e);
            Log.L.info("no hibernate support, use shutdown");
            return false;
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.utils.os.DesktopSupport#getDefaultDownloadDirectory()
     */
    @Override
    public String getDefaultDownloadDirectory() {
        /**
         * http://freedesktop.org/wiki/Software/xdg-user-dirs/
         */
        final String XDG_DOWNLOAD_DIR = System.getenv("XDG_DOWNLOAD_DIR");
        if (StringUtils.isNotEmpty(XDG_DOWNLOAD_DIR)) {
            if (XDG_DOWNLOAD_DIR.startsWith("/") && !XDG_DOWNLOAD_DIR.contains("$")) {
                return XDG_DOWNLOAD_DIR;
            } else if (XDG_DOWNLOAD_DIR.contains("$")) {
                final String HOME = System.getenv("HOME");
                String downloadDirectory = null;
                if (StringUtils.isNotEmpty(HOME) && XDG_DOWNLOAD_DIR.contains("$HOME")) {
                    downloadDirectory = XDG_DOWNLOAD_DIR.replaceFirst("\\$HOME", HOME);
                }
                if (downloadDirectory != null && !downloadDirectory.contains("$")) {
                    //
                    return downloadDirectory;
                }
            }
        }
        return null;
    }

}
