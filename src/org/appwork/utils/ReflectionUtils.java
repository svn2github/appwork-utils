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
package org.appwork.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.simplejson.mapper.ClassCache;
import org.appwork.utils.reflection.Clazz;

public class ReflectionUtils {
    // TODO: make weak

    /**
     * @param <T>
     * @param name
     * @param object
     * @param class1
     * @return
     */
    public static <T> List<Class<? extends T>> getClassesInPackage(final ClassLoader cl, final String name, final Pattern pattern, final Class<T> class1) {
        Enumeration<URL> found;
        final List<T> ret = new ArrayList<T>();
        try {
            final String finalName = name.replace(".", "/");
            found = cl.getResources(finalName);

            while (found.hasMoreElements()) {

                final URL url = found.nextElement();

                if (url.getProtocol().equalsIgnoreCase("jar")) {
                    final String path = url.getPath();
                    final File jarFile = new File(new URL(path.substring(0, path.lastIndexOf('!'))).toURI());
                    JarInputStream jis = null;
                    try {
                        jis = new JarInputStream(new FileInputStream(jarFile));
                        JarEntry e;

                        while ((e = jis.getNextJarEntry()) != null) {

                            if (!e.getName().endsWith(".class")) {
                                continue;
                            }
                            if (!e.getName().startsWith(finalName)) {
                                continue;
                            }
                            // try {
                            if (pattern != null) {

                                final Matcher matcher = pattern.matcher(e.getName());
                                if (!matcher.matches()) {
                                    continue;
                                }

                            }

                            String classPath = e.getName().replace("/", ".");
                            classPath = classPath.substring(0, classPath.length() - 6);
                            try {
                                final Class<?> clazz = cl.loadClass(classPath);
                                if (class1 == clazz) {
                                    continue;
                                }
                                if (class1 == null || class1.isAssignableFrom(clazz)) {
                                    ret.add((T) clazz);
                                }
                            } catch (final Throwable ee) {

                            }

                        }
                    } finally {
                        try {
                            jis.close();
                        } catch (final Throwable e) {
                        }
                    }
                } else {
                    final File path = new File(url.toURI());
                    final int i = path.getAbsolutePath().replace("\\", "/").indexOf(finalName);
                    final File root = new File(path.getAbsolutePath().substring(0, i));
                    final List<File> files = Files.getFiles(new FileFilter() {

                        @Override
                        public boolean accept(final File pathname) {
                            if (!pathname.getName().endsWith(".class")) {
                                return false;
                            }
                            final String rel = Files.getRelativePath(root, pathname);
                            if (pattern != null) {
                                final Matcher matcher = pattern.matcher(rel);
                                if (!matcher.matches()) {
                                    return false;
                                }
                            }

                            return true;
                        }
                    }, new File(url.toURI()));

                    for (final File classFile : files) {
                        String classPath = Files.getRelativePath(root, classFile).replace("/", ".").replace("\\", ".");
                        classPath = classPath.substring(0, classPath.length() - 6);
                        try {
                            final Class<?> clazz = cl.loadClass(classPath);
                            if (class1 == clazz) {
                                continue;
                            }
                            if (class1 == null || class1.isAssignableFrom(clazz)) {
                                ret.add((T) clazz);
                            }
                        } catch (final Throwable ee) {

                        }

                    }
                    //
                }
            }
        } catch (final Exception e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
        return (List<Class<? extends T>>) ret;
    }

    private static final HashMap<Class<?>, Collection<GetterSetter>> GETTER_SETTER_CACHE = new HashMap<Class<?>, Collection<GetterSetter>>();

    /**
     * @return
     */
    public static Collection<GetterSetter> getGettersSetteres(Class<?> clazz) {
        Collection<GetterSetter> ret = GETTER_SETTER_CACHE.get(clazz);
        if (ret != null) {
            return ret;
        }
        final Class<?> org = clazz;
        synchronized (GETTER_SETTER_CACHE) {
            ret = GETTER_SETTER_CACHE.get(clazz);
            if (ret != null) {
                return ret;
            }
            final HashMap<String, GetterSetter> map = new HashMap<String, GetterSetter>();
            while (clazz != null) {
                for (final Method m : clazz.getDeclaredMethods()) {
                    String key = null;
                    boolean getter = false;
                    if (m.getName().startsWith("is") && Clazz.isBoolean(m.getReturnType()) && m.getParameterTypes().length == 0) {
                        key = (m.getName().substring(2));
                        getter = true;

                    } else if (m.getName().startsWith("get") && m.getParameterTypes().length == 0) {
                        key = (m.getName().substring(3));
                        getter = true;

                    } else if (m.getName().startsWith("set") && m.getParameterTypes().length == 1) {
                        key = (m.getName().substring(3));
                        getter = false;

                    }

                    if (StringUtils.isNotEmpty(key)) {
                        final String unmodifiedKey = key;
                        key = ClassCache.createKey(key);
                        GetterSetter v = map.get(key);
                        if (v == null) {
                            v = new GetterSetter(key);
                            map.put(key, v);
                        }
                        if (getter) {
                            v.setGetter(m);
                        } else {
                            v.setSetter(m);
                        }
                        Field field;
                        try {
                            field = clazz.getField(unmodifiedKey.substring(0, 1).toLowerCase(Locale.ENGLISH) + unmodifiedKey.substring(1));
                            v.setField(field);
                        } catch (final NoSuchFieldException e) {
                        }

                    }
                }
                clazz = clazz.getSuperclass();
            }
            GETTER_SETTER_CACHE.put(org, map.values());
            return GETTER_SETTER_CACHE.get(org);
        }

    }

    /**
     * @param type
     * @return
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public static Object[] getEnumValues(final Class<? extends Enum> type) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {

        return (Object[]) type.getMethod("values", new Class[] {}).invoke(null, new Object[] {});

    }

    /**
     * @param type
     * @param value
     * @return
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public static Object getEnumValueOf(final Class<? extends Enum> type, final String value) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        return type.getMethod("valueOf", new Class[] { String.class }).invoke(null, new Object[] { value });

    }

    /**
     * @param value
     * @param clazz
     * @return
     */
    public static Number castNumber(Number value, Class<?> clazz) {

        if (Clazz.isByte(clazz)) {
            return value.byteValue();
        } else if (Clazz.isInteger(clazz)) {
            return value.intValue();
        } else if (Clazz.isLong(clazz)) {
            return value.longValue();
        } else if (Clazz.isDouble(clazz)) {
            return value.doubleValue();
        } else if (Clazz.isFloat(clazz)) {
            return value.floatValue();
        }
        throw new WTFException("Unsupported type: " + clazz);

    }
}
