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
package org.appwork.utils.os.mime;

import java.awt.Image;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileSystemView;

import org.appwork.sunwrapper.sun.awt.shell.ShellFolderWrapper;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.IO.SYNC;
import org.appwork.utils.StringUtils;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.logging2.extmanager.LoggerFactory;

public class MimeWindows extends MimeDefault {

    @Override
    public Icon getFileIcon(final String extension, final int width, final int height) throws IOException {
        final String iconKey = "osFileIcon_" + super.getIconKey(extension, width, height);
        Icon ret = super.getCacheIcon(iconKey);
        if (ret != null) {
            return ret;
        }
        final boolean registryContainsFileIcon = registryContainsFileIcon(extension);
        if (!registryContainsFileIcon) {
            ret = super.getFileIcon(extension, width, height);
        } else {
            final File path = Application.getTempResource("images/" + extension + ".png");
            if (path.getParentFile().isDirectory()) {
                // woraround a bug we had until 24.06.2013.. created folders
                // instead of files
                path.getParentFile().delete();
            }
            if (!path.getParentFile().exists()) {
                path.getParentFile().mkdirs();
            }
            try {
                if (path.exists() && path.isFile()) {
                    ret = new ImageIcon(ImageProvider.read(path));
                } else {
                    File tempFile = null;
                    FileOutputStream fos = null;
                    try {
                        tempFile = Application.getTempResource("icon" + System.nanoTime() + "." + extension);
                        if (!tempFile.exists()) {
                            IO.writeToFile(tempFile, new byte[0], SYNC.NONE);
                        }
                        final Image image = ShellFolderWrapper.getIcon(tempFile);
                        if (image != null) {
                            ret = new ImageIcon(image);
                            fos = new FileOutputStream(path);
                            ImageIO.write((RenderedImage) image, "png", fos);
                        } else {
                            ret = ImageProvider.toImageIcon(FileSystemView.getFileSystemView().getSystemIcon(tempFile));
                        }
                    } catch (final Throwable e) {
                        final LogSource logger = LoggerFactory.I().getCurrentClassLogger();
                        logger.log(e);
                        logger.severe("TempFile:" + tempFile);
                        // http://www.oracle.com/technetwork/java/faq-sun-packages-142232.html
                        ret = ImageProvider.toImageIcon(FileSystemView.getFileSystemView().getSystemIcon(tempFile));
                    } finally {
                        try {
                            if (fos != null) {
                                fos.close();
                            }
                        } catch (final Throwable e) {
                        }
                        if (tempFile != null) {
                            if (!tempFile.delete()) {
                                tempFile.deleteOnExit();
                            }
                        }
                    }
                }
            } catch (final Throwable e) {
                LoggerFactory.I().getCurrentClassLogger().log(e);
            }
        }
        if (ret == null || ret.getIconWidth() < width || ret.getIconHeight() < height) {
            ret = super.getFileIcon(extension, width, height);
        }
        ret = IconIO.getScaledInstance(ret, width, height);
        super.saveIconCache(iconKey, ret);
        return ret;
    }

    private static boolean registryContainsFileIcon(final String extension) {
        return registryContainsFileIcon(extension, null);
    }

    private static boolean registryContainsFileIcon(final String extension, final Preferences rootNode) {
        if (StringUtils.isNotEmpty(extension)) {
            try {
                final Preferences root;
                if (rootNode == null) {
                    root = Preferences.userRoot();
                } else {
                    root = rootNode;
                }
                final Class<?> clz = root.getClass();
                final Method windowsRegOpenKey = clz.getDeclaredMethod("WindowsRegOpenKey", int.class, byte[].class, int.class);
                windowsRegOpenKey.setAccessible(true);
                final Method windowsRegCloseKey = clz.getDeclaredMethod("WindowsRegCloseKey", int.class);
                windowsRegCloseKey.setAccessible(true);
                final Method rootNativeHandle = clz.getDeclaredMethod("rootNativeHandle", new Class[0]);
                rootNativeHandle.setAccessible(true);
                final Integer rootNativeHdl = (Integer) rootNativeHandle.invoke(root);
                final String key = "Software\\Classes\\." + extension.toLowerCase(Locale.ENGLISH);
                final int KEY_READ = 0x20019;
                final int ERROR_SUCCESS = 0;
                final int ERROR_FILE_NOT_FOUND = 2;
                final int ERROR_ACCESS_DENIED = 5;
                final int NATIVE_HANDLE = 0;
                final int ERROR_CODE = 1;
                final int[] result = (int[]) windowsRegOpenKey.invoke(root, rootNativeHdl, toCstr(key), KEY_READ);
                if (result[ERROR_CODE] == ERROR_SUCCESS) {
                    windowsRegCloseKey.invoke(root, result[NATIVE_HANDLE]);
                    return true;
                } else if (result[ERROR_CODE] == ERROR_FILE_NOT_FOUND) {
                    final Preferences nextRoot = Preferences.systemRoot();
                    if (nextRoot != null && rootNode == null) {
                        return registryContainsFileIcon(extension, nextRoot);
                    } else {
                        // avoid endless recursion
                        return false;
                    }
                } else if (result[ERROR_CODE] == ERROR_ACCESS_DENIED) {
                    return false;
                } else {
                    return false;
                }
            } catch (final Throwable e) {
                LoggerFactory.I().getCurrentClassLogger().log(e);
            }
        }
        return false;
    }

    private static byte[] toCstr(final String str) {
        final byte[] result = new byte[str.length() + 1];
        for (int i = 0; i < str.length(); i++) {
            result[i] = (byte) str.charAt(i);
        }
        result[str.length()] = 0;
        return result;
    }
}