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
package org.appwork.resources;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import javax.swing.Icon;
import javax.swing.JComponent;

import org.appwork.storage.config.MinTimeWeakReference;
import org.appwork.storage.config.MinTimeWeakReferenceCleanup;
import org.appwork.swing.components.CheckBoxIcon;
import org.appwork.utils.Application;
import org.appwork.utils.Exceptions;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.images.Interpolation;
import org.appwork.utils.locale._AWU;
import org.appwork.utils.swing.dialog.Dialog;

/**
 *
 * @author thomas
 *
 */
public class Theme implements MinTimeWeakReferenceCleanup {
    private String                                              path;
    // private final HashMap<String, MinTimeWeakReference<BufferedImage>>
    // imageCache = new HashMap<String, MinTimeWeakReference<BufferedImage>>();
    protected final HashMap<String, MinTimeWeakReference<Icon>> imageIconCache = new HashMap<String, MinTimeWeakReference<Icon>>();
    private long                                                cacheLifetime  = 20000l;
    private String                                              theme;
    private String                                              nameSpace;

    public Theme(final String namespace) {
        this.setNameSpace(namespace);
        this.setTheme("standard");
    }

    public void cache(final Icon ret, final String key) {
        synchronized (this.imageIconCache) {
            this.imageIconCache.put(key, new MinTimeWeakReference<Icon>(ret, this.getCacheLifetime(), key, this));
        }
    }

    private Theme      delegate;
    private String     defaultPath;
    public static File RESOURCE_HELPER_ROOT;

    /**
     * @param i
     */
    public void setDelegate(Theme i) {
        this.delegate = i;
    }

    /**
     *
     */
    public void clearCache() {
        synchronized (this.imageIconCache) {
            this.imageIconCache.clear();
        }
    }

    public Icon getCached(final String key) {
        synchronized (this.imageIconCache) {
            final MinTimeWeakReference<Icon> cache = this.imageIconCache.get(key);
            if (cache != null) {
                return cache.get();
            }
            return null;
        }
    }

    /**
     * @param relativePath
     * @param size
     * @return
     */
    protected String getCacheKey(final Object... objects) {
        if (objects.length == 1) {
            return objects[0].toString();
        }
        final StringBuilder sb = new StringBuilder();
        for (final Object o : objects) {
            if (sb.length() > 0) {
                sb.append("_");
            }
            sb.append(o.toString());
        }
        return sb.toString();
    }

    public long getCacheLifetime() {
        return this.cacheLifetime;
    }

    public Icon getDisabledIcon(final Icon _getIcon) {
        return getDisabledIcon(null, _getIcon);
    }

    public Icon getDisabledIcon(final JComponent component, final Icon _getIcon) {
        if (_getIcon != null) {
            if (this.delegate != null) {
                this.delegate.getDisabledIcon(_getIcon);
            }
            final String key = this.getCacheKey(_getIcon, "disabled");
            Icon ret = this.getCached(key);
            if (ret == null) {
                final Icon ico = ImageProvider.getDisabledIcon(component, _getIcon);
                ret = ico;
                ret = this.modify(ret, key);
                this.cache(ret, key);
            }
            return ret;
        }
        return null;
    }

    public Icon getIcon(final String relativePath, final int size) {
        return this.getIcon(relativePath, size, true);
    }

    /**
     * @param relativePath
     * @param size
     * @param b
     * @return
     */
    public Icon getIcon(final String relativePath, final int size, final boolean useCache) {
        if (this.delegate != null) {
            this.delegate.getIcon(relativePath, size, useCache);
        }
        Icon ret = null;
        String key = null;
        if (useCache) {
            key = this.getCacheKey(relativePath, size);
            ret = this.getCached(key);
        }
        if (ret == null) {
            if (StringUtils.equalsIgnoreCase(relativePath, "disabled") || StringUtils.equalsIgnoreCase(relativePath, "checkbox_false")) {
                ret = CheckBoxIcon.FALSE;
                if (ret != null) {
                    // may be null during calss loading static init of the CheckBoxIconClass
                    ret = IconIO.getScaledInstance(ret, size, size);
                }
            } else if (StringUtils.equalsIgnoreCase(relativePath, "enabled") || StringUtils.equalsIgnoreCase(relativePath, "checkbox_true")) {
                ret = CheckBoxIcon.TRUE;
                if (ret != null) {// may be null during calss loading static init of the CheckBoxIconClass
                    ret = IconIO.getScaledInstance(ret, size, size);
                }
            } else if (StringUtils.equalsIgnoreCase(relativePath, "checkbox_undefined")) {
                ret = CheckBoxIcon.UNDEFINED;
                if (ret != null) {// may be null during calss loading static init of the CheckBoxIconClass
                    ret = IconIO.getScaledInstance(ret, size, size);
                }
            }
            if (ret == null) {
                final URL url = lookupImageUrl(relativePath, size);
                ret = IconIO.getImageIcon(url, size);
                ret = this.modify(ret, relativePath);
                if (url == null) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(new Exception("Icon missing: " + this.getPath("images/", relativePath, ".png", false)));
                }
            }
            if (useCache && ret != null) {
                this.cache(ret, key);
            }
        }
        return ret;
    }

    /**
     * @param relativePath
     * @param size
     * @return
     */
    protected URL lookupImageUrl(String relativePath, int size) {
        URL url = this.getURL("images/", relativePath + "_" + size, ".png", false);
        if (url == null) {
            url = this.getURL("images/", relativePath, ".png", false);
        }
        if (url == null) {
            url = this.getURL("images/", relativePath, ".svg", false);
        }
        if (url == null) {
            url = this.getURL("images/", relativePath + "_" + size, ".png", true);
        }
        if (url == null) {
            url = this.getURL("images/", relativePath, ".png", true);
        }
        if (url == null) {
            url = this.getURL("images/", relativePath, ".svg", true);
        }
        return url;
    }

    /**
     * @param relativePath
     */
    private void resourcesHelper(String relativePath) {
        try {
            URL self = getClass().getResource("/");
            File file = new File(self.toURI());
            File res = new File(file.getParentFile().getParent(), "Resources");
            if (!res.exists()) {
                return;
            }
            File helperRoot = new File(file.getParentFile(), "themes/");
            if (RESOURCE_HELPER_ROOT != null) {
                helperRoot = RESOURCE_HELPER_ROOT;
            }
            File to = new File(helperRoot, relativePath);
            // String[] pathes = relativePath.split("[\\/\\\\]+");
            while (true) {
                File check = new File(res, relativePath);
                if (check.exists()) {
                    if (!to.exists()) {
                        Dialog.I().showConfirmDialog(0, "Found Missing Resource", "The Project " + file.getParentFile().getName() + " requires the resource " + relativePath + ".\r\nCopy " + check + " to " + to, null, _AWU.T.lit_yes(), null);
                        copy(check, to);
                        break;
                    }
                }
                int in = relativePath.indexOf("\\");
                int in2 = relativePath.indexOf("/");
                if (in < 0 || in2 < in) {
                    in = in2;
                }
                if (in < 0) {
                    break;
                }
                relativePath = relativePath.substring(in + 1);
            }
        } catch (Throwable e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().severe(Exceptions.getStackTrace(e));
        }
    }

    /**
     * @param check
     * @param to
     * @throws IOException
     */
    protected void copy(File check, File to) throws IOException {
        to.getParentFile().mkdirs();
        IO.copyFile(check, to);
        copy(check, to, ".txt");
        copy(check, to, ".license");
        copy(check, to, ".info");
        copy(check, to, ".nfo");
    }

    /**
     * @param check
     * @param to
     * @param ext
     * @throws IOException
     */
    protected void copy(File check, File to, String ext) throws IOException {
        File nfo = new File(check.getAbsolutePath() + ext);
        File t = new File(to.getAbsolutePath() + ext);
        if (nfo.exists() && !t.exists()) {
            t.getParentFile().mkdirs();
            IO.copyFile(nfo, t);
        }
    }

    /**
     * @param ret
     * @param relativePath
     */
    protected Icon modify(Icon ret, String relativePath) {
        return ret;
    }

    public Image getImage(final String relativePath, final int size) {
        return this.getImage(relativePath, size, false);
    }

    public Image getImage(final String key, final int size, final boolean useCache) {
        if (this.delegate != null) {
            this.delegate.getImage(key, size, useCache);
        }
        return IconIO.toBufferedImage(this.getIcon(key, size, useCache));
    }

    // public URL getImageUrl(final String relativePath) {
    // if (this.delegate != null) {
    // this.delegate.getImageUrl(relativePath);
    // }
    // return this.getURL("images/", relativePath, ".png");
    // }
    public String getNameSpace() {
        return this.nameSpace;
    }

    /**
     * @return
     */
    public String getPath() {
        return this.path;
    }

    private String getPath(final String pre, final String path, final String ext, boolean fallback) {
        final StringBuilder sb = new StringBuilder();
        sb.append(fallback ? defaultPath : this.path);
        sb.append(pre);
        sb.append(path);
        sb.append(ext);
        return sb.toString();
    }

    public Icon getScaledInstance(final Icon imageIcon, final int size) {
        if (this.delegate != null) {
            this.delegate.getScaledInstance(imageIcon, size);
        }
        final String key = this.getCacheKey(imageIcon, size);
        Icon ret = this.getCached(key);
        if (ret == null) {
            ret = IconIO.getScaledInstance(imageIcon, size, size, Interpolation.BILINEAR);
            this.cache(ret, key);
        }
        return ret;
    }

    public String getText(final String string) {
        if (this.delegate != null) {
            this.delegate.getText(string);
        }
        final URL url = this.getURL("", string, "");
        if (url == null) {
            return null;
        }
        try {
            return IO.readURLToString(url);
        } catch (final IOException e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
        }
        return null;
    }

    public String getTheme() {
        return this.theme;
    }

    /**
     * returns a valid resourceurl or null if no resource is available.
     *
     * @param pre
     *            subfolder. for exmaple "images/"
     * @param relativePath
     *            relative resourcepath
     * @param ext
     *            resource extension
     * @return
     */
    public URL getURL(final String pre, final String relativePath, final String ext) {
        URL ret = getURL(pre, relativePath, ext, false);
        if (ret != null) {
            return ret;
        }
        return getURL(pre, relativePath, ext, true);
    }

    private URL getURL(final String pre, final String relativePath, final String ext, boolean fallback) {
        if (this.delegate != null) {
            return this.delegate.getURL(pre, relativePath, ext, fallback);
        }
        final String path = this.getPath(pre, relativePath, ext, fallback);
        try {
            // first lookup in home dir. .jd_home or installdirectory
            final File file = Application.getResource(path);
            if (file.exists()) {
                return file.toURI().toURL();
            }
        } catch (final MalformedURLException e) {
            e.printStackTrace();
        }
        // afterwards, we lookup in classpath. jar or bin folders
        final URL url = Theme.class.getResource(path);
        return url;
    }

    public File getImagesDirectory() {
        if (this.delegate != null) {
            this.delegate.getImagesDirectory();
        }
        return Application.getResource(getPath("images/", "image", ".file", false)).getParentFile();
    }

    public boolean hasIcon(final String string) {
        if (this.delegate != null) {
            this.delegate.hasIcon(string);
        }
        return lookupImageUrl(string, -1) != null;
    }

    public URL getIconURL(final String string) {
        URL ret = null;
        if (this.delegate != null) {
            ret = delegate.getIconURL(string);
        }
        if (ret == null) {
            return lookupImageUrl(string, -1);
        } else {
            return ret;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.storage.config.MinTimeWeakReferenceCleanup# onMinTimeWeakReferenceCleanup
     * (org.appwork.storage.config.MinTimeWeakReference)
     */
    @Override
    public void onMinTimeWeakReferenceCleanup(final MinTimeWeakReference<?> minTimeWeakReference) {
        synchronized (this.imageIconCache) {
            this.imageIconCache.remove(minTimeWeakReference.getID());
        }
    }

    public void setCacheLifetime(final long cacheLifetime) {
        this.cacheLifetime = cacheLifetime;
    }

    public void setNameSpace(final String nameSpace) {
        if (!StringUtils.equals(getNameSpace(), nameSpace) && StringUtils.isNotEmpty(nameSpace)) {
            this.nameSpace = nameSpace;
            updatePath();
            this.clearCache();
        }
    }

    /**
     *
     */
    private void updatePath() {
        this.path = "/themes/" + this.getTheme() + "/" + this.getNameSpace();
        this.defaultPath = "/themes/" + "standard" + "/" + this.getNameSpace();
    }

    // public void setPath(final String path) {
    // this.path = path;
    // this.nameSpace = null;
    // this.theme = null;
    // this.clearCache();
    // }
    /**
     * @param theme
     */
    public void setTheme(final String theme) {
        if (!StringUtils.equals(getTheme(), theme) && StringUtils.isNotEmpty(theme)) {
            this.theme = theme;
            updatePath();
            this.clearCache();
        }
    }
}
