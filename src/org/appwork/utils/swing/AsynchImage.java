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
package org.appwork.utils.swing;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import org.appwork.resources.AWUTheme;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.Hash;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.event.queue.QueueAction;

import org.appwork.utils.net.SimpleHTTP;

public class AsynchImage extends JLabel {

    public static class Updater extends QueueAction<Void, RuntimeException> {

        private final File        cache;
        private final int         x;
        private final int         y;
        private final URL         url;
        private final AsynchImage asynchImage;
        public final static long  EXPIRETIME = 7 * 24 * 60 * 60 * 1000l;

        /**
         * @param asynchImage
         * @param prefX
         * @param prefY
         * @param cache
         * @param url
         */
        public Updater(final AsynchImage asynchImage, final int prefX, final int prefY, final File cache, final URL url) {
            x = prefX;
            y = prefY;
            this.cache = cache;
            this.url = url;
            this.asynchImage = asynchImage;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Runnable#run()
         */
        @Override
        public Void run() {
            try {
                synchronized (AsynchImage.LOCK) {
                    // check again.
                    final long age = System.currentTimeMillis() - cache.lastModified();
                    if (cache.exists() && age < Updater.EXPIRETIME) {
                        // seems like another thread updated the image in the
                        // meantime
                        final BufferedImage image = ImageProvider.read(cache);
                        if (asynchImage != null) {
                            asynchImage.setDirectIcon(new ImageIcon(image));
                        }
                        return null;
                    }
                }
                BufferedImage image = null;
                synchronized (AsynchImage.LOCK2) {
                    synchronized (AsynchImage.LOCK) {
                        final long age = System.currentTimeMillis() - cache.lastModified();
                        if (cache.exists() && age < Updater.EXPIRETIME) {
                            // seems like another thread updated the image in
                            // the
                            // meantime
                            image = ImageProvider.read(cache);
                            if (asynchImage != null) {
                                asynchImage.setDirectIcon(new ImageIcon(image));
                            }
                            return null;
                        }
                    }
                          org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finest("Update image " + cache);
                    if (url == null) {
                              org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finest("no url given");
                        return null;
                    }
                    final SimpleHTTP simple = new SimpleHTTP();
                    HttpURLConnection ret = null;
                    try {
                              org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finest("Call " + url);
                        ret = simple.openGetConnection(url, 30 * 1000);
                              org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finest("DONE");
                        image = ImageIO.read(ret.getInputStream());
                    } finally {
                        try {
                            ret.disconnect();
                        } catch (final Throwable e) {
                        }
                        try {
                            simple.getConnection().disconnect();
                        } catch (final Throwable e) {
                        }
                    }
                          org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finest("Scale image " + cache);
                    image = ImageProvider.getScaledInstance(image, x, y, RenderingHints.VALUE_INTERPOLATION_BILINEAR, true);
                }
                synchronized (AsynchImage.LOCK) {
                    final long age = System.currentTimeMillis() - cache.lastModified();
                    if (cache.exists() && age < Updater.EXPIRETIME) {
                        // seems like another thread updated the image in
                        // the
                        // meantime
                        image = ImageProvider.read(cache);
                        if (asynchImage != null) {
                            asynchImage.setDirectIcon(new ImageIcon(image));
                        }
                        return null;
                    }
                          org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finest("Cachewrite image " + cache + " " + x + " - " + image.getWidth());
                    cache.getParentFile().mkdirs();
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(cache);
                        ImageIO.write(image, Files.getExtension(cache.getName()), fos);
                    } finally {
                        try {
                            fos.close();
                        } catch (final Throwable e) {
                        }
                    }
                    if (asynchImage != null) {
                        asynchImage.setDirectIcon(new ImageIcon(image));
                    }
                }
            } catch (final Throwable e) {
                      org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().severe("Error loading Url:"+url);
      
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
            
            }
            return null;
        }

        public void start() {
            AsynchImage.QUEUE.add(this);
        }
    }

    private static final Queue QUEUE               = new Queue(AsynchImage.class.getName() + "-Queue") {

                                                   };

    /**
     * 
     */
    private static final long  serialVersionUID    = 1L;
    private File               cache;

    private final int          prefX;
    private final int          prefY;
    private boolean            setIconAfterLoading = true;

    public static Object       LOCK                = new Object();
    private static Object      LOCK2               = new Object();

    /**
     * @param i
     * @param j
     */
    public AsynchImage(final int x, final int y) {
        prefX = x;
        prefY = y;
    }

    /**
     * @param thumbURL
     * @param i
     * @param j
     */
    public AsynchImage(final String thumbURL, final String extension, final int x, final int y) {
        super();
        setBorder(new ShadowBorder(2));
        prefX = x;
        prefY = y;
        this.setIcon(thumbURL, extension);

    }

    /**
     * @return the setIconAfterLoading
     */
    public boolean isSetIconAfterLoading() {
        return setIconAfterLoading;
    }

    /**
     * @param imageIcon
     */
    protected void setDirectIcon(final ImageIcon imageIcon) {
        if (setIconAfterLoading) {

            new EDTRunner() {
                @Override
                protected void runInEDT() {
                          org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finest("Set image " + cache);
                    AsynchImage.this.setIcon(imageIcon);
                    AsynchImage.this.repaint();
                }
            };
        }
    }

    /**
     * @param thumbURL
     * @param extension
     * @param x
     * @param y
     */
    public void setIcon(final String thumbURL, final String extension) {
        /* cacheFile for resized image */
        cache = Application.getTempResource("asynchimage/" + Hash.getMD5(thumbURL) + "_" + prefX + "x" + prefY + "." + extension);
        // if cache is older than 7 days. delete
        boolean refresh = true;
        try {
            synchronized (AsynchImage.LOCK) {
                final long age = System.currentTimeMillis() - cache.lastModified();
                if (cache.exists() && age < Updater.EXPIRETIME) {
                    refresh = false;
                    BufferedImage image;
                    image = ImageProvider.read(cache);
                    this.setIcon(new ImageIcon(image));
                    if (!isSetIconAfterLoading()) {
                        if (image.getWidth() > 32) {
                            //       org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finest(this.cache);
                        }
                    }
                    return;
                } else if (cache.exists()) {
                    BufferedImage image;
                    image = ImageProvider.read(cache);
                    this.setIcon(new ImageIcon(image));
                    return;
                }
            }
            this.setIcon(AWUTheme.getInstance().getIcon("imageLoader", prefX));
        } catch (final Throwable e) {
            this.setIcon(AWUTheme.getInstance().getIcon("imageLoader", prefX));
        } finally {
            if (refresh) {
                try {
                    new Updater(this, prefX, prefY, cache, new URL(thumbURL)).start();
                } catch (final Throwable e) {
                }
            }
        }
    }

    /**
     * @param setIconAfterLoading
     *            the setIconAfterLoading to set
     */
    public void setSetIconAfterLoading(final boolean setIconAfterLoading) {
        this.setIconAfterLoading = setIconAfterLoading;
    }

}
