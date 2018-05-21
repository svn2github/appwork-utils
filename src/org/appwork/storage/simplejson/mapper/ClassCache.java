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
package org.appwork.storage.simplejson.mapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.appwork.storage.simplejson.Ignore;
import org.appwork.storage.simplejson.Ignores;

/**
 * @author thomas
 *
 */
public class ClassCache {
    private static final HashMap<Class<?>, ClassCache> CACHE        = new HashMap<Class<?>, ClassCache>();
    private static final Object[]                      EMPTY_OBJECT = new Object[] {};
    private static final Class<?>[]                    EMPTY_TYPES  = new Class[] {};

    /**
     * @param clazz
     * @return
     * @throws NoSuchMethodException
     * @throws SecurityException
     */
    protected static ClassCache create(final Class<? extends Object> clazz) throws SecurityException, NoSuchMethodException {
        final ClassCache cc = new ClassCache(clazz);
        Getter g;
        Setter s;
        Class<? extends Object> cls = clazz;
        final HashSet<String> ignores = new HashSet<String>();
        do {
            final Ignores ig = cls.getAnnotation(Ignores.class);
            if (ig != null) {
                for (final String i : ig.value()) {
                    ignores.add(i);
                }
            }
            for (final Method m : cls.getDeclaredMethods()) {
                if (m.getAnnotation(Ignore.class) != null || ignores.contains(m.toString())) {
                    continue;
                }
                if (m.getName().startsWith("get") && m.getParameterTypes().length == 0 && m.getReturnType() != void.class) {
                    cc.getter.add(g = new Getter(createKey(m.getName().substring(3)), m));
                    cc.getterMap.put(g.getKey(), g);
                    // org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finer(m.toString());
                } else if (m.getName().startsWith("is") && m.getParameterTypes().length == 0 && m.getReturnType() != void.class) {
                    cc.getter.add(g = new Getter(createKey(m.getName().substring(2)), m));
                    cc.getterMap.put(g.getKey(), g);
                    // org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finer(m.toString());
                } else if (m.getName().startsWith("set") && m.getParameterTypes().length == 1) {
                    cc.setter.add(s = new Setter(createKey(m.getName().substring(3)), m));
                    cc.setterMap.put(s.getKey(), s);
                    // org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finer(m.toString());
                }
            }
        } while ((cls = cls.getSuperclass()) != null && cls != Object.class);
        // we do not want to serialize object's getter
        for (final Constructor<?> c : clazz.getDeclaredConstructors()) {
            if (c.getParameterTypes().length == 0) {
                try {
                    c.setAccessible(true);
                    cc.constructor = c;
                } catch (final java.lang.SecurityException e) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                }
                break;
            }
        }
        if (cc.constructor == null) {
            //
            final int lastIndex = clazz.getName().lastIndexOf(".");
            final String pkg = lastIndex > 0 ? clazz.getName().substring(0, lastIndex) : "";
            if (pkg.startsWith("java") || pkg.startsWith("sun.")) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().warning("No Null Constructor in " + clazz + " found. De-Json-serial will fail");
            } else {
                throw new NoSuchMethodException(" Class " + clazz + " requires a null constructor. please add private " + clazz.getSimpleName() + "(){}");
            }
        }
        return cc;
    }

    /**
     *
     * Jackson maps methodnames to keys like this. setID becomes key "id" , setMethodName becomes "methodName". To keep compatibility
     * between jackson and simplemapper, we should do it the same way
     *
     * @param substring
     * @return
     */
    public static String createKey(final String key) {
        final StringBuilder sb = new StringBuilder();
        final char[] ca = key.toCharArray();
        boolean starter = true;
        for (int i = 0; i < ca.length; i++) {
            if (starter && Character.isUpperCase(ca[i])) {
                sb.append(Character.toLowerCase(ca[i]));
            } else {
                starter = false;
                sb.append(ca[i]);
            }
        }
        return sb.toString();
    }

    /**
     * @param clazz
     * @return
     * @throws NoSuchMethodException
     * @throws SecurityException
     */
    public static ClassCache getClassCache(final Class<? extends Object> clazz) throws SecurityException, NoSuchMethodException {
        ClassCache cc = ClassCache.CACHE.get(clazz);
        if (cc == null) {
            System.out.println("ClassCache: " + clazz);
            cc = ClassCache.create(clazz);
            ClassCache.CACHE.put(clazz, cc);
        }
        return cc;
    }

    protected Constructor<? extends Object> constructor;
    protected final Class<? extends Object> clazz;
    protected final java.util.List<Getter>  getter;
    protected final java.util.List<Setter>  setter;
    protected final HashMap<String, Getter> getterMap;
    protected final HashMap<String, Setter> setterMap;

    /**
     * @param clazz
     */
    protected ClassCache(final Class<? extends Object> clazz) {
        this.clazz = clazz;
        getter = new ArrayList<Getter>();
        setter = new ArrayList<Setter>();
        getterMap = new HashMap<String, Getter>();
        setterMap = new HashMap<String, Setter>();
    }

    public java.util.List<Getter> getGetter() {
        return getter;
    }

    public Getter getGetter(final String key) {
        return getterMap.get(key);
    }

    /**
     * @return
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws IllegalArgumentException
     */
    public Object getInstance() throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        return constructor.newInstance(ClassCache.EMPTY_OBJECT);
    }

    public java.util.List<Setter> getSetter() {
        return setter;
    }

    public Setter getSetter(final String key) {
        return setterMap.get(key);
    }

    /**
     * @param class1
     * @param stackTraceElementClassCache
     */
    public static void put(final Class<?> class1, final ClassCache stackTraceElementClassCache) {
        CACHE.put(class1, stackTraceElementClassCache);
    }
}
