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
package org.appwork.txtresource;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.appwork.storage.config.annotations.IntegerInterface;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.storage.config.annotations.TooltipInterface;
import org.appwork.txtresource.TranslationUtils.Translated;
import org.appwork.txtresource.TranslationUtils.TranslationProviderInterface;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;

/**
 * @author thomas
 *
 */
public class TranslationHandler implements InvocationHandler {
    private final Class<? extends TranslateInterface> tInterface;
    private java.util.List<TranslateResource>         lookup;
    private HashMap<Method, String>                   cache;
    private final Method[]                            methods;
    private HashMap<String, TranslateResource>        resourceCache;
    private boolean                                   tryCustom;
    public static final String                        DEFAULT = "en";

    /**
     * @param class1
     * @param lookup
     * @throws IOException
     */
    public TranslationHandler(final Class<? extends TranslateInterface> class1, final String... lookup) {
        this.tInterface = class1;
        tryCustom = Application.getResource("translations/custom").exists();
        this.methods = this.tInterface.getDeclaredMethods();
        this.cache = new HashMap<Method, String>();
        this.resourceCache = new HashMap<String, TranslateResource>();
        this.lookup = this.fillLookup(lookup);
    }

    /**
     * @param m
     * @param types
     * @return
     */
    private boolean checkTypes(final Method m, final Class<?>[] types) {
        final Class<?>[] parameters = m.getParameterTypes();
        if (parameters.length != types.length) {
            return false;
        }
        if (types.length == 0) {
            return true;
        }
        for (int i = 0; i < types.length; i++) {
            if (types[i] != parameters[i]) {
                if (Number.class.isAssignableFrom(types[i])) {
                    if (parameters[i] == int.class || parameters[i] == long.class || parameters[i] == double.class || parameters[i] == float.class || parameters[i] == byte.class || parameters[i] == char.class) {
                        continue;
                    } else {
                        return false;
                    }
                } else if (types[i] == Boolean.class && parameters[i] == boolean.class) {
                    return true;
                }
                return false;
            }
        }
        return true;
    }

    /**
     * @param string
     * @param prov
     * @param addComments
     * @return
     */
    public String createFile(final String string, TranslationProviderInterface prov, final boolean addComments) {
        final TranslateData map = new TranslateData();
        this.cache.clear();
        this.lookup = this.fillLookup(string);
        HashMap<Method, Translated> comments = new HashMap<Method, Translated>();
        for (final Method m : this.tInterface.getDeclaredMethods()) {
            try {
                if (prov == null) {
                    map.put(m.getName(), this.invoke(null, m, null).toString());
                } else {
                    Translated trans = prov.get(m, string, this.invoke(null, m, null).toString());
                    comments.put(m, trans);
                    map.put(m.getName(), trans.value);
                }
            } catch (final Throwable e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        String ret = TranslationUtils.serialize(map);
        if (addComments) {
            for (final Method m : this.tInterface.getDeclaredMethods()) {
                final Default def = m.getAnnotation(Default.class);
                final DescriptionForTranslationEntry desc = m.getAnnotation(DescriptionForTranslationEntry.class);
                String comment = "";
                Translated translated = comments.get(m);
                if (translated != null) {
                    String c = translated.comment;
                    if (StringUtils.isNotEmpty(c)) {
                        comment += "\r\n//   " + c;
                    }
                }
                if (desc != null) {
                    final String d = desc.value().replaceAll("[\\\r\\\n]+", "\r\n//    ");
                    comment += "\r\n// Description:\r\n//    " + d;
                }
                //
                if (comment.length() > 0) {
                    ret = ret.replace(m.getName() + "=", comment + "\r\n" + m.getName() + "=");
                }
            }
        }
        return ret;
    }

    /**
     * @param string
     * @return
     * @throws IOException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private TranslateResource createTranslationResource(final String string) throws IOException, InstantiationException, IllegalAccessException {
        TranslateResource ret = this.resourceCache.get(string);
        if (ret != null) {
            return ret;
        }
        final DynamicResourcePath rPath = this.tInterface.getAnnotation(DynamicResourcePath.class);
        String path = null;
        URL url = null;
        // check custom path
        if (tryCustom) {
            path = rPath != null ? "translations/custom/" + rPath.value().newInstance().getPath() + "." + string + ".lng" : "translations/custom/" + this.tInterface.getName().replace(".", "/") + "." + string + ".lng";
            url = Application.getRessourceURL(path, false);
            if (url != null) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finer("Load Custom Translation " + url);
            }
        }
        if (url == null) {
            path = rPath != null ? "translations/" + rPath.value().newInstance().getPath() + "." + string + ".lng" : "translations/" + this.tInterface.getName().replace(".", "/") + "." + string + ".lng";
            url = Application.getRessourceURL(path, false);
            if (url != null) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finer("Load Translation " + url);
            }
        }
        if (url == null) {
            // translations files may either be located in the same path as the
            // interface is located, or in a translations/namespace
            path = rPath != null ? this.tInterface.getPackage().getName().replace(".", "/") + "/" + rPath.value().newInstance().getPath() + "." + string + ".lng" : this.tInterface.getName().replace(".", "/") + "." + string + ".lng";
            url = Application.getRessourceURL(path, false);
            if (url != null) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finer("Load Neighbour Translation " + url);
            }
        }
        if (url == null && rPath != null) {
            path = rPath.value().newInstance().getPath();
            url = Application.getRessourceURL(path, false);
            if (url != null) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finer("Load DynamicResourcePath Translation " + url);
            }
        }
        miss: if (url == null) {
            final Defaults ann = this.tInterface.getAnnotation(Defaults.class);
            if (ann != null) {
                for (final String d : ann.lngs()) {
                    if (d.equals(string)) {
                        // defaults
                        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().info("Translation file missing:" + path + "Use Annotation Dev fallback");
                        break miss;
                    }
                }
            }
            // throw new NullPointerException("Missing Translation: " + path);
        }
        ret = new TranslateResource(url, string);
        this.resourceCache.put(string, ret);
        return ret;
    }

    /**
     * @param lookup2
     * @return
     */
    private java.util.List<TranslateResource> fillLookup(final String... lookup) {
        final java.util.List<TranslateResource> ret = new ArrayList<TranslateResource>();
        TranslateResource res;
        boolean containsDefault = false;
        for (final String oo : lookup) {
            for (String o : TranslationFactory.getVariantsOf(oo)) {
                try {
                    if (TranslationHandler.DEFAULT.equals(o)) {
                        containsDefault = true;
                    }
                    res = this.createTranslationResource(o);
                    ret.add(res);
                } catch (final NullPointerException e) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().warning(e.getMessage());
                } catch (final Throwable e) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                }
            }
        }
        if (!containsDefault) {
            try {
                res = this.createTranslationResource(TranslationHandler.DEFAULT);
                ret.add(res);
            } catch (final Throwable e) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
            }
        }
        return ret;
    }

    /**
     * @param ret
     * @param args
     * @return
     */
    private String format(String ret, final Object[] args) {
        if (args != null) {
            int i = 0;
            for (final Object o : args) {
                i++;
                if (o != null) {
                    if (o instanceof LabelInterface) {
                        ret = ret.replace("%s" + i, ((LabelInterface) o).getLabel());
                    } else if (o instanceof IntegerInterface) {
                        ret = ret.replace("%s" + i, String.valueOf(((IntegerInterface) o).getInt()));
                    } else if (o instanceof TooltipInterface) {
                        ret = ret.replace("%s" + i, ((TooltipInterface) o).getTooltip());
                    } else {
                        ret = ret.replace("%s" + i, o.toString());
                    }
                } else {
                    ret = ret.replace("%s" + i, "null");
                }
            }
        }
        return ret;
    }

    /**
     * @param method
     * @return
     */
    public String getDefault(final Method method) {
        final TranslateResource res = this.resourceCache.get(TranslationHandler.DEFAULT);
        return res.readDefaults(method);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
     */
    /**
     * @return
     */
    public Method[] getMethods() {
        return this.methods;
    }

    /**
     * @param method
     * @return
     */
    public String getTranslation(final Method method, final Object... params) {
        return this.getValue(method, this.lookup);
    }

    /**
     * @param string
     * @param string2
     * @return
     */
    public String getTranslation(final String languageKey, final String methodname, final Object... params) {
        final Class<?>[] types = new Class<?>[params.length];
        for (int i = 0; i < params.length; i++) {
            types[i] = params[i].getClass();
        }
        for (final Method m : this.methods) {
            if (m.getName().equals(methodname)) {
                if (this.checkTypes(m, types)) {
                    final String ret = this.getValue(m, this.fillLookup(languageKey));
                    return this.format(ret, params);
                }
            }
        }
        return null;
    }

    public String getValue(final Method method, final java.util.List<TranslateResource> lookup) {
        String ret = null;
        TranslateResource res;
        for (final Iterator<TranslateResource> it = lookup.iterator(); it.hasNext();) {
            res = it.next();
            try {
                ret = res.getEntry(method);
                if (ret != null) {
                    return ret;
                }
            } catch (final Throwable e) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().warning("Exception in translation: " + this.tInterface.getName() + "." + res.getName());
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
            }
        }
        if (ret == null) {
            ret = this.tInterface.getSimpleName() + "." + method.getName();
        }
        return ret;
    }

    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        final java.util.List<TranslateResource> lookup = this.lookup;
        // for speed reasons let all controller methods (@see
        // TRanslationINterface.java) start with _
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }
        // else if (method.getName().startsWith("_")) {
        if (method.getName().equals("_getHandler")) {
            return this;
        }
        // if (method.getName().equals("_getSupportedLanguages")) {
        //
        // return TranslationFactory.listAvailableTranslations(this.tInterface);
        // }
        // if (method.getName().equals("_setLanguage")) {
        //
        // if (method.getName().equals("_getTranslation")) {
        HashMap<Method, String> lcache = cache;
        String ret = null;
        synchronized (lcache) {
            ret = lcache.get(method);
            if (ret == null) {
                ret = this.getValue(method, lookup);
                if (ret != null) {
                    lcache.put(method, ret);
                }
            }
        }
        return this.format(ret, args);
    }

    /**
     * Tells the TranslationHandler to use this language from now on.clears
     *
     * cache.
     *
     * for speed reasons, cache access is not synchronized
     *
     * @param loc
     */
    public void setLanguage(final String loc) {
        this.cache = new HashMap<Method, String>();
        this.resourceCache = new HashMap<String, TranslateResource>();
        this.lookup = this.fillLookup(loc);
    }

    /**
     * @param method
     * @return
     */
    public TranslationSource getSource(Method method) {
        TranslationSource ret = null;
        TranslateResource res;
        for (final Iterator<TranslateResource> it = lookup.iterator(); it.hasNext();) {
            res = it.next();
            try {
                ret = res.getSource(method);
                if (ret != null) {
                    return ret;
                }
            } catch (final Throwable e) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().warning("Exception in translation: " + this.tInterface.getName() + "." + res.getName());
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
            }
        }
        return ret;
    }

    /**
     * @return
     */
    public String getID() {
        return lookup.get(0).getName();
    }

    /**
     * @param id
     * @return
     */
    public TranslateResource getResource(String id) {
        for (TranslateResource tr : lookup) {
            if (tr.getName().equals(id)) {
                return tr;
            }
        }
        return null;
    }

    /**
     * @return
     */
    public Class<? extends TranslateInterface> getInterfaceClass() {
        return tInterface;
    }
}
