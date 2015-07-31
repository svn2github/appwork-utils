/**
 * Copyright (c) 2009 - 2010 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 *
 * This file is part of org.appwork.utils
 *
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils;

import java.awt.GraphicsEnvironment;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;

import javax.swing.UIManager;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.logging.Log;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.os.CrossSystem;

/**
 * Application utils provide statis helper functions concerning the applications System integration
 *
 * @author $Author: unknown$
 *
 */
public class Application {

    private static Boolean              IS_JARED      = null;

    static {
        Application.redirectOutputStreams();
    }
    private static String               APP_FOLDER    = ".appwork";
    private static String               ROOT;
    private static long                 javaVersion   = 0;
    public final static long            JAVA15        = 15000000;
    public final static long            JAVA16        = 16000000;
    public final static long            JAVA17        = 17000000;
    public final static long            JAVA18        = 18000000;
    private static Boolean              IS_SYNTHETICA = null;
    private static Boolean              JVM64BIT      = null;

    private static boolean              REDIRECTED    = false;
    public static PauseableOutputStream STD_OUT;
    public static PauseableOutputStream ERR_OUT;

    public static void addStreamCopy(File file, org.appwork.utils.Application.PauseableOutputStream stream) {
        int i = 0;
        File orgFile = file;
        while (true) {

            try {
                if (file.exists()) {
                    throw new FileNotFoundException("Exists");
                }
                stream.addBranch(new BufferedOutputStream(new FileOutputStream(file)));
                break;
            } catch (FileNotFoundException e1) {
                i++;
                e1.printStackTrace();
                String extension = org.appwork.utils.Files.getExtension(orgFile.getName());
                if (extension != null) {
                    file = new File(orgFile.getParentFile(), orgFile.getName().substring(0, orgFile.getName().length() - extension.length() - 1) + "." + i + "." + extension);
                } else {
                    file = new File(orgFile.getParentFile(), orgFile.getName() + "." + i);
                }
            }
        }
    }

    /**
     * Adds a folder to the System classloader classpath this might fail if there is a security manager
     *
     * @param file
     * @throws IOException
     */
    public static void addFolderToClassPath(final File file) throws IOException {
        try {
            // hack to add an url to the system classpath
            final Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
            method.setAccessible(true);
            method.invoke(Application.class.getClassLoader(), new Object[] { file.toURI().toURL() });

        } catch (final Throwable t) {
            Log.exception(t);
            throw new IOException("Error, could not add URL to system classloader");
        }

    }

    /**
     * Adds a url to the classloader classpath this might fail if there is a security manager
     *
     * @param file
     * @throws IOException
     */
    public static void addUrlToClassPath(final URL url, final ClassLoader cl) throws IOException {
        try {
            // hack to add an url to the system classpath
            final Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
            method.setAccessible(true);
            method.invoke(cl, new Object[] { url });

        } catch (final Throwable t) {
            Log.exception(t);
            throw new IOException("Error, could not add URL to system classloader");
        }
    }

    public static String getApplication() {
        return Application.APP_FOLDER;
    }

    /**
     * @return
     */
    public static File getApplicationRoot() {
        return Application.getRootByClass(Application.class, null);
    }

    /**
     * Returns the Path of appworkutils.jar
     *
     * @return
     */
    public static String getHome() {
        return Application.getRoot(Application.class);
    }

    /**
     * @return
     */
    public static URL getHomeURL() {

        try {
            return new File(Application.getHome()).toURI().toURL();
        } catch (final MalformedURLException e) {
            throw new WTFException(e);
        }
    }

    // returns the jar filename of clazz
    public static File getJarFile(final Class<?> clazz) {
        final String name = clazz.getName().replaceAll("\\.", "/") + ".class";
        final URL url = Application.getRessourceURL(name);
        final String prot = url.getProtocol();
        final String path = url.getPath();
        Log.L.info(url + "");
        if (!"jar".equals(prot)) {
            throw new WTFException("Works in Jared mode only");
        }
        final int index = path.indexOf(".jar!");
        if (index < 0) {
            throw new WTFException("Works in Jared mode only");
        }
        try {
            return new File(new URL(path.substring(0, index + 4)).toURI());
        } catch (final MalformedURLException e) {
            Log.exception(Level.WARNING, e);

        } catch (final URISyntaxException e) {
            Log.exception(Level.WARNING, e);

        }
        return null;
    }

    /**
     * @param object
     * @return
     */
    public static String getJarName(Class<?> clazz) {
        if (clazz == null) {
            clazz = Application.class;
        }
        final String name = clazz.getName().replaceAll("\\.", "/") + ".class";
        final String url = Application.getRessourceURL(name).toString();

        final int index = url.indexOf(".jar!");
        if (index < 0) {
            throw new IllegalStateException("No JarName Found");
        }
        try {
            return new File(new URL(url.substring(4, index + 4)).toURI()).getName();
        } catch (final Exception e) {

        }
        throw new IllegalStateException("No JarName Found");
    }

    public static long getJavaVersion() {
        if (Application.javaVersion > 0) {
            return Application.javaVersion;
        }
        try {
            /* this version info contains more information */
            String version = System.getProperty("java.runtime.version");
            if (version == null || version.trim().length() == 0) {
                version = System.getProperty("java.version");
            }
            String v = new Regex(version, "^(\\d+\\.\\d+\\.\\d+)").getMatch(0);
            final String u = new Regex(version, "^.*?_(\\d+)").getMatch(0);
            final String b = new Regex(version, "^.*?(_|-)b(\\d+)$").getMatch(1);
            v = v.replaceAll("\\.", "");
            /* 170uubbb */
            /* eg 1.6 = 16000000 */
            long ret = Long.parseLong(v) * 100000;
            if (u != null) {
                /* append update number */
                ret = ret + Long.parseLong(u) * 1000;
            }
            if (b != null) {
                /* append build number */
                ret = ret + Long.parseLong(b);
            }
            Application.javaVersion = ret;
            return ret;
        } catch (final Exception e) {
            Log.exception(e);
            return -1;
        }
    }

    /**
     * @param class1
     * @return
     */
    public static String getPackagePath(final Class<?> class1) {
        // TODO Auto-generated method stub
        return class1.getPackage().getName().replace('.', '/') + "/";
    }

    /**
     * Returns a ressourcefile relative to the instaldirectory
     *
     * @param relative
     * @return
     */
    public static File getResource(final String relative) {
        return new File(Application.getHome(), relative);
    }

    /**
     * returns the url for the resource. if The resource can be found in classpath, it will be returned. otherwise the function will return
     * the fileurl to current wprkingdirectory
     *
     * @param string
     * @return
     */
    public static URL getRessourceURL(final String relative) {
        return Application.getRessourceURL(relative, true);
    }

    /**
     * Returns the Resource url for relative.
     *
     * NOTE:this function only returns URL's that really exists!
     *
     * if preferClassPath is true:
     *
     * we first check if there is a ressource available inside current classpath, for example inside the jar itself. if no such URL exists
     * we check for file in local filesystem
     *
     * if preferClassPath if false:
     *
     * first check local filesystem, then inside classpath
     *
     *
     *
     * @param string
     * @param b
     */
    public static URL getRessourceURL(final String relative, final boolean preferClasspath) {
        try {

            if (relative == null) {
                return null;
            }
            if (relative.startsWith("/") || relative.startsWith("\\")) {
                throw new WTFException("getRessourceURL only works with relative paths.");
            }
            if (preferClasspath) {

                final URL res = Application.class.getClassLoader().getResource(relative);
                if (res != null) {
                    return res;
                }
                final File file = new File(Application.getHome(), relative);
                if (!file.exists()) {
                    return null;
                }
                return file.toURI().toURL();

            } else {
                final File file = new File(Application.getHome(), relative);
                if (file.exists()) {
                    return file.toURI().toURL();
                }

                final URL res = Application.class.getClassLoader().getResource(relative);
                if (res != null) {
                    return res;
                }

            }
        } catch (final MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Detects the applications home directory. it is either the pass of the appworkutils.jar or HOME/
     */
    public static String getRoot(final Class<?> rootOfClazz) {

        if (Application.ROOT != null) {
            return Application.ROOT;
        }
        final String key = "awuhome" + Application.APP_FOLDER;
        final String sysProp = System.getProperty(key);
        if (sysProp != null) {

            Application.ROOT = sysProp;
            return Application.ROOT;
        }
        if (Application.isJared(rootOfClazz)) {
            // this is the jar file
            URL loc;

            loc = rootOfClazz.getProtectionDomain().getCodeSource().getLocation();

            java.io.File appRoot = null;
            try {
                String path = loc.getPath();
                // loc may be a
                try {
                    appRoot = new File(java.net.URLDecoder.decode(path, "UTF-8"));
                    if (!appRoot.exists()) {
                        appRoot = null;
                    }
                } catch (java.io.UnsupportedEncodingException e) {
                    Log.exception(e);
                }
                if (appRoot == null) {
                    appRoot = new File(path);
                    if (!appRoot.exists()) {
                        appRoot = null;
                    }
                }
                if (appRoot == null) {
                    appRoot = new File(loc.toURI());
                    if (!appRoot.exists()) {
                        appRoot = null;
                    }
                }
                if (appRoot == null) {
                    throw new java.net.URISyntaxException(loc + "", "Bad URI");
                }
                if (appRoot.isFile()) {
                    appRoot = appRoot.getParentFile();
                }
                Application.ROOT = appRoot.getAbsolutePath();
                System.out.println("Application Root: " + Application.ROOT + " (jared) " + rootOfClazz);
            } catch (final URISyntaxException e) {
                Log.exception(e);
                Application.ROOT = System.getProperty("user.home") + System.getProperty("file.separator") + Application.APP_FOLDER + System.getProperty("file.separator");
                System.out.println("Application Root: " + Application.ROOT + " (jared but error) " + rootOfClazz);
            }
        } else {
            Application.ROOT = System.getProperty("user.home") + System.getProperty("file.separator") + Application.APP_FOLDER;
            System.out.println("Application Root: " + Application.ROOT + " (DEV) " + rootOfClazz);
        }
        // do not use Log.L here. this might be null
        return Application.ROOT;
    }

    /**
     * @param class1
     * @param subPaths
     * @return
     */
    public static File getRootByClass(final Class<?> class1, final String subPaths) {
        // this is the jar file
        URL loc;

        loc = class1.getProtectionDomain().getCodeSource().getLocation();

        File appRoot;
        try {
            appRoot = new File(loc.toURI());

            if (appRoot.isFile()) {
                appRoot = appRoot.getParentFile();
            }
            if (subPaths != null) {
                return new File(appRoot, subPaths);
            }
            return appRoot;
        } catch (final URISyntaxException e) {
            Log.exception(e);
            return null;
        }
    }

    /**
     * @param class1
     * @param subPaths
     *            TODO
     * @return
     */
    public static URL getRootUrlByClass(final Class<?> class1, final String subPaths) {
        // TODO Auto-generated method stub
        try {
            return Application.getRootByClass(class1, subPaths).toURI().toURL();
        } catch (final MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @return
     */
    public static File getTemp() {
        final File ret = Application.getResource("tmp");
        if (!ret.exists()) {
            ret.mkdirs();
        }
        return ret;
    }

    /**
     * @param cache
     * @return
     */
    public static File getTempResource(final String cache) {
        return new File(Application.getTemp(), cache);
    }

    public static boolean is64BitJvm() {
        if (Application.JVM64BIT != null) {
            return Application.JVM64BIT;
        }
        final String archDataModel = System.getProperty("sun.arch.data.model");
        try {
            if (archDataModel != null) {
                if (Integer.parseInt(archDataModel) == 64) {
                    Application.JVM64BIT = true;
                    return true;
                } else {
                    Application.JVM64BIT = false;
                    return false;
                }
            }
        } catch (final Throwable e) {
        }
        final boolean is64BitJVM = CrossSystem.is64BitArch();
        Application.JVM64BIT = is64BitJVM;
        return is64BitJVM;
    }

    /**
     * Detects if the Application runs out of a jar or not.
     *
     * @param rootOfClazz
     *
     * @return
     */
    public static boolean isJared(Class<?> rootOfClazz) {
        if (Application.IS_JARED != null) {

            return Application.IS_JARED == Boolean.TRUE;

        }
        if (rootOfClazz == null) {
            rootOfClazz = Application.class;
        }
        final String name = rootOfClazz.getName().replaceAll("\\.", "/") + ".class";
        final ClassLoader cll = Application.class.getClassLoader();
        if (cll == null) {
            Log.L.severe("getContextClassLoader() is null");
            Application.IS_JARED = Boolean.TRUE;
            return true;
        }
        // System.out.println(name);
        final URL caller = cll.getResource(name);

        // System.out.println(caller);
        /*
         * caller is null in case the ressource is not found or not enough rights, in that case we assume its not jared
         */
        if (caller == null) {
            Application.IS_JARED = false;
            return false;
        }
        boolean ret = caller.toString().matches("jar\\:.*\\.(jar|exe)\\!.*");
        Application.IS_JARED = ret;
        return ret;
    }

    /**
     * checks current java version for known issues/bugs or unsupported ones
     *
     * @param support15
     * @return
     */
    public static boolean isOutdatedJavaVersion(final boolean supportJAVA15) {
        final long java = Application.getJavaVersion();
        if (java < Application.JAVA16 && !CrossSystem.isMac()) {
            Log.L.warning("Java 1.6 should be available on your System, please upgrade!");
            /* this is no mac os, so please use java>=1.6 */
            return true;
        }
        if (java < Application.JAVA16 && !supportJAVA15) {
            Log.L.warning("Java 1.5 no longer supported!");
            /* we no longer support java 1.5 */
            return true;
        }
        if (java >= 16018000l && java < 16019000l) {
            Log.L.warning("Java 1.6 Update 18 has a serious bug in garbage collector!");
            /*
             * java 1.6 update 18 has a bug in garbage collector, causes java crashes
             *
             * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6847956
             */
            return true;
        }
        if (java >= 16010000l && java < 16011000l) {
            Log.L.warning("Java 1.6 Update 10 has a swing bug!");
            /*
             * 16010.26 http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6657923
             */
            return true;
        }
        if (CrossSystem.isMac() && java >= Application.JAVA17 && java < 17006000l) {
            Log.L.warning("leaking semaphores bug");
            /*
             * leaking semaphores http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7166379
             */
            return true;
        }
        if (CrossSystem.isMac() && java >= 17250000l && java < 17550000l) {
            Log.L.warning("freezing AppKit thread bug");
            /*
             * http://bugs.java.com/view_bug.do?bug_id=8025588
             *
             * Frozen AppKit thread
             */
            return true;
        }
        return false;
    }

    /**
     * @param logger
     */
    public static void printSystemProperties(final LogInterface logger) {
        final Properties p = System.getProperties();
        final Enumeration keys = p.keys();
        final StringBuilder sb = new StringBuilder();
        String key;
        while (keys.hasMoreElements()) {
            key = (String) keys.nextElement();
            sb.append("SysProp: ").append(key).append(": ").append((String) p.get(key));

            logger.info(sb.toString());
            sb.setLength(0);
        }

        for (final Entry<String, String> e : System.getenv().entrySet()) {

            sb.append("SysEnv: ").append(e.getKey()).append(": ").append(e.getValue());
            logger.info(sb.toString());
            sb.setLength(0);
        }

        URL url = Application.getRessourceURL("version.nfo");

        if (url != null) {
            try {
                logger.info(url + ":\r\n" + IO.readURLToString(url));
            } catch (final IOException e1) {
                logger.log(e1);

            }
        }
        url = Application.getRessourceURL("build.json");

        if (url != null) {
            try {
                logger.info(url + ":\r\n" + IO.readURLToString(url));
            } catch (final IOException e1) {
                logger.log(e1);

            }
        }

    }

    public static class PauseableOutputStream extends OutputStream {

        private PrintStream           _out;
        private ByteArrayOutputStream buffer;

        /**
         * @param out
         */
        public PauseableOutputStream(PrintStream out) {
            this._out = out;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.io.OutputStream#write(int)
         */
        @Override
        public void write(int paramInt) throws IOException {
            if (this.branches != null) {
                for (OutputStream os : this.branches) {
                    try {
                        os.write(paramInt);
                    } catch (Throwable e) {

                    }
                }
            }
            if (this.buffer != null) {
                this.buffer.write(paramInt);
                return;
            }
            this._out.write(paramInt);

        }

        /*
         * (non-Javadoc)
         *
         * @see java.io.OutputStream#write(byte[])
         */
        @Override
        public void write(byte[] b) throws IOException {
            if (this.branches != null) {
                for (OutputStream os : this.branches) {
                    try {
                        os.write(b);
                    } catch (Throwable e) {

                    }
                }
            }
            if (this.buffer != null) {
                this.buffer.write(b);
                return;
            }
            this._out.write(b);
        }

        /*
         * (non-Javadoc)
         *
         * @see java.io.OutputStream#write(byte[], int, int)
         */
        @Override
        public void write(byte[] buff, int off, int len) throws IOException {

            if (this.branches != null) {
                for (OutputStream os : this.branches) {
                    try {
                        os.write(buff, off, len);
                    } catch (Throwable e) {

                    }
                }
            }
            if (this.buffer != null) {
                this.buffer.write(buff, off, len);
                return;
            }
            this._out.write(buff, off, len);
        }

        /*
         * (non-Javadoc)
         *
         * @see java.io.OutputStream#flush()
         */
        @Override
        public void flush() throws IOException {
            if (this.buffer != null) {
                this.buffer.flush();
                return;
            }
            if (this.branches != null) {
                for (OutputStream os : this.branches) {
                    try {
                        os.flush();
                    } catch (Throwable e) {

                    }
                }
            }
            this._out.flush();
        }

        /*
         * (non-Javadoc)
         *
         * @see java.io.OutputStream#close()
         */
        @Override
        public void close() throws IOException {
            if (this.buffer != null) {
                this.buffer.close();
                this.setBufferEnabled(false);
            }
            if (this.branches != null) {
                for (OutputStream os : this.branches) {
                    try {
                        os.close();
                    } catch (Throwable e) {

                    }
                }
            }
            this._out.close();
        }

        /**
         * @param b
         * @throws IOException
         */
        public boolean setBufferEnabled(boolean b) throws IOException {
            synchronized (this) {
                if (b) {

                    if (this.buffer != null) {
                        return true;
                    }
                    this.buffer = new ByteArrayOutputStream();
                    return false;
                } else {
                    if (this.buffer != null) {
                        this.buffer.writeTo(this._out);
                        this.buffer = null;
                        return true;
                    } else {
                        return false;
                    }

                }
            }
        }

        private List<OutputStream> branches = null;

        /**
         * @param bufferedOutputStream
         */
        public void addBranch(OutputStream os) {
            if (this.branches == null) {
                this.branches = new ArrayList<OutputStream>();
            }
            this.branches.add(os);

        }

    }

    /**
     *
     */
    public static void redirectOutputStreams() {
        if (Application.REDIRECTED) {
            return;
        }
        if (Charset.defaultCharset() == Charset.forName("cp1252")) {
            Application.REDIRECTED = true;
            // workaround.
            // even 1252 is default codepage, windows console expects cp850
            // codepage input
            try {
                System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, "CP850"));
            } catch (final UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, "CP850"));
            } catch (final UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (Application.STD_OUT == null) {
            Application.STD_OUT = new PauseableOutputStream(System.out);

            Application.ERR_OUT = new PauseableOutputStream(System.err);

            PrintStream o;
            System.setOut(o = new PrintStream(Application.STD_OUT));
            PrintStream e;
            System.setErr(e = new PrintStream(Application.ERR_OUT));
            System.out.println("SetOut " + o);
            System.out.println("SetErr " + e);
        }
    }

    /**
     * sets current Application Folder and Jar ID. MUST BE SET at startup! Can only be set once!
     *
     * @param newAppFolder
     * @param newJar
     */
    public synchronized static void setApplication(final String newAppFolder) {
        Application.ROOT = null;
        Application.APP_FOLDER = newAppFolder;
    }

    /**
     * @return
     */
    public static boolean isHeadless() {

        return GraphicsEnvironment.isHeadless();

    }

    /**
     * returns a file that does not exists. thus it ads a counter to the path until the resulting file does not exist
     *
     * @param string
     * @return
     */
    public static File generateNumberedTempResource(String string) {
        return Application.generateNumbered(Application.getTempResource(string));
    }

    /**
     * returns a file that does not exists. thus it ads a counter to the path until the resulting file does not exist
     *
     * @param string
     * @return
     */
    public static File generateNumberedResource(String string) {
        return Application.generateNumbered(Application.getResource(string));

    }

    /**
     * @param resource
     */
    private static File generateNumbered(File orgFile) {
        int i = 0;

        String extension = Files.getExtension(orgFile.getName());
        File file = null;
        while (file == null || file.exists()) {
            i++;

            if (extension != null) {
                file = new File(orgFile.getParentFile(), orgFile.getName().substring(0, orgFile.getName().length() - extension.length() - 1) + "." + i + "." + extension);
            } else {
                file = new File(orgFile.getParentFile(), orgFile.getName() + "." + i);
            }

        }
        return file;

    }

    /**
     * check if the synthetica look and feel is used. make sure not to call this before you set the final look and feel! Else all calls will
     * return the wrong results.
     *
     * @return
     */
    public static boolean isSyntheticaLookAndFeel() {

        if (IS_SYNTHETICA != null) {
            return IS_SYNTHETICA;
        }
        Class<?> cls;
        try {
            cls = Class.forName("de.javasoft.plaf.synthetica.SyntheticaLookAndFeel");

            IS_SYNTHETICA = cls.isAssignableFrom(UIManager.getLookAndFeel().getClass());
        } catch (Throwable e) {
            IS_SYNTHETICA = false;
        }
        return IS_SYNTHETICA;
    }
}
