/**
 * Copyright (c) 2009 - 2014 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 *
 * This file is part of org.appwork.utils.os
 *
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.os;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.appwork.utils.logging.Log;
import org.appwork.utils.processes.ProcessBuilderFactory;

/**
 * @author Thomas
 *
 */
public class DesktopSupportMac extends DesktopSupportJavaDesktop {

    @Override
    public boolean isBrowseURLSupported() {
        /* either Desktop or FallBack method */
        return true;
    }

    @Override
    public void browseURL(URL url) throws IOException, URISyntaxException {
        try {
            super.browseURL(url);
        } catch (Throwable e) {
            try {
                Class.forName("com.apple.eio.FileManager").getDeclaredMethod("openURL", new Class[] { String.class }).invoke(null, new Object[] { url.toExternalForm() });
            } catch (final Throwable ignore) {
                if (e instanceof IOException) {
                    throw (IOException) e;
                } else {
                    throw new IOException(e);
                }
            }
        }
    }

    @Override
    public boolean shutdown(boolean force) {
        if (force) {
            /* force shutdown */
            try {
                ProcessBuilderFactory.runCommand(new String[] { "sudo", "shutdown", "-p", "now" });
            } catch (Exception e) {
                Log.exception(e);
            }
            try {
                ProcessBuilderFactory.runCommand(new String[] { "sudo", "shutdown", "-h", "now" });
            } catch (Exception e) {
                Log.exception(e);
            }
            return true;
        } else {
            /* normal shutdown */
            try {
                ProcessBuilderFactory.runCommand(new String[] { "/usr/bin/osascript", "-e", "tell application \"Finder\" to shut down" });
                return true;
            } catch (Exception e) {
                Log.exception(e);
            }
        }
        return false;
    }

    @Override
    public boolean standby() {
        try {
            ProcessBuilderFactory.runCommand(new String[] { "/usr/bin/osascript", "-e", "tell application \"Finder\" to sleep" });
            return true;
        } catch (Exception e) {
            Log.exception(e);
        }
        Log.L.info("no standby support, use shutdown");
        return false;
    }

    @Override
    public boolean hibernate() {
        Log.L.info("no hibernate support, use shutdown");
        return false;
    }
}
