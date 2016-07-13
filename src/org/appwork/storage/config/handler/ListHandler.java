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
package org.appwork.storage.config.handler;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.MinTimeWeakReference;
import org.appwork.storage.config.annotations.CryptedStorage;
import org.appwork.storage.config.annotations.DefaultFactory;
import org.appwork.storage.config.annotations.DefaultJsonObject;
import org.appwork.storage.config.annotations.DisableObjectCache;
import org.appwork.storage.config.annotations.PlainStorage;
import org.appwork.utils.Application;
import org.appwork.utils.IO;

/**
 * @author Thomas
 *
 */
public abstract class ListHandler<T> extends KeyHandler<T> {

    private interface ListHandlerCache<T> {
        public T get();
    }

    public static final int                   MIN_LIFETIME = 10000;
    private volatile ListHandlerCache<Object> cache;
    private final TypeRef<Object>             typeRef;

    public TypeRef<Object> getTypeRef() {
        return typeRef;
    }

    private final static Object NULL           = new Object();

    private File                path;
    private URL                 url;
    private boolean             useObjectCache = false;
    private byte[]              cryptKey       = null;

    /**
     * @param storageHandler
     * @param key
     */
    public ListHandler(final StorageHandler<?> storageHandler, final String key, final Type type) {
        super(storageHandler, key);
        this.typeRef = new TypeRef<Object>(type) {
        };
    }

    @Override
    protected Class<? extends Annotation>[] getAllowedAnnotations() {
        return new Class[] { DisableObjectCache.class };
    }

    protected Object getCachedValue() {
        if (isCachingAllowed()) {
            final ListHandlerCache<Object> lCache = this.cache;
            if (lCache != null) {
                return lCache.get();
            }
        }
        return null;
    }

    protected boolean isCachingAllowed() {
        return this.useObjectCache && this.getStorageHandler().isObjectCacheEnabled();
    }

    @Override
    public T getValue() {
        synchronized (this) {
            Object value = this.getCachedValue();
            if (value == null) {
                try {
                    value = this.read();
                } catch (final Throwable e) {
                    throw new WTFException(e);
                }
                this.putCachedValue(value);
            }
            if (ListHandler.NULL == value) {
                value = null;
            }
            if (this.customValueGetter != null) {
                value = this.customValueGetter.getValue(this, (T) value);
            }
            if (value == null && isDefaultOnNull()) {
                value = getDefaultValue();
            }
            return (T) value;
        }
    }

    @Override
    protected void initDefaults() throws Throwable {
    }

    @Override
    protected void initHandler() throws Throwable {
        final CryptedStorage cryptedStorage = this.getAnnotation(CryptedStorage.class);
        if (cryptedStorage != null) {
            /* use key from CryptedStorage */
            this.cryptKey = cryptedStorage.key();
        } else {
            if (this.getAnnotation(PlainStorage.class) == null) {
                /* we use key from primitiveStorage */
                this.cryptKey = this.storageHandler.getPrimitiveStorage().getCryptKey();
            } else {
                /* we enforce no key! */
                this.cryptKey = null;
            }
        }
        final File storageHandlerPath = this.storageHandler.getPath();
        if (storageHandlerPath != null) {
            this.path = new File(storageHandlerPath + "." + this.getKey() + "." + (getCryptKey() != null ? "ejs" : "json"));
        } else {
            this.path = null;
        }
        final String relativeClassPath = this.storageHandler.getRelativCPPath();
        if (relativeClassPath != null && (path == null || !this.path.exists())) {
            // Remember: Application.getResourceUrl returns an url to the classpath (bin/jar) or to a file on the harddisk (cfg folder)
            // we do only want urls to the classpath here
            final String ressource = relativeClassPath + "." + this.getKey() + "." + (getCryptKey() != null ? "ejs" : "json");
            this.url = Application.class.getClassLoader().getResource(ressource);
        } else {
            this.url = null;
        }
        this.useObjectCache = this.getAnnotation(DisableObjectCache.class) == null;
    }

    @Override
    public File getPath() {
        return path;
    }

    public URL getURL() {
        return url;
    }

    @Override
    public byte[] getCryptKey() {
        return cryptKey;
    }

    protected void putCachedValue(final Object value) {
        final Object finalValue;
        if (value == null) {
            finalValue = ListHandler.NULL;
        } else {
            finalValue = value;
        }
        if (getStorageHandler().isDelayedWriteAllowed(this)) {
            this.cache = new ListHandlerCache<Object>() {

                @Override
                public Object get() {
                    return finalValue;
                }

            };
        } else if (isCachingAllowed()) {
            this.cache = new ListHandlerCache<Object>() {
                final MinTimeWeakReference<Object> minTimeWeakReference = new MinTimeWeakReference<Object>(finalValue, ListHandler.MIN_LIFETIME, "Storage " + getKey());

                @Override
                public Object get() {
                    return minTimeWeakReference.get();
                }

            };
        } else {
            this.cache = null;
        }
    }

    @Override
    protected void putValue(final T value) {
        synchronized (this) {
            this.putCachedValue(value);
            this.write(value);
        }
    }

    @Override
    protected boolean setValueEqualsGetValue(T newValue) {
        synchronized (this) {
            final Object value = this.getCachedValue();
            if (value != null) {
                if (value == newValue) {
                    /**
                     * newValue is the same object as our cached value. changes within the object no longer can be detected!!! so we write *
                     * enforce write to make sure changes land on disk
                     */
                    return false;
                }
                return super.setValueEqualsGetValue(newValue);
            }
        }
        /**
         * without cached value we enforce write and avoid additional read+equals
         */
        return false;
    }

    protected Object readObject(final Object dummyObject, AtomicBoolean readFlag) throws InstantiationException, IllegalAccessException, IOException {
        Object readObject = getStorageHandler().readObject(this, readFlag);
        if (!readFlag.get()) {
            final File jsonPath = getPath();
            final byte[] cryptKey = getCryptKey();
            if (jsonPath != null && jsonPath.exists()) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finer("Read Config(File): " + jsonPath.getAbsolutePath());
                readObject = JSonStorage.restoreFrom(jsonPath, cryptKey == null, cryptKey, getTypeRef(), dummyObject);
                readFlag.set(jsonPath.exists());
            }
            if (readObject == dummyObject || !readFlag.get()) {
                final URL jsonURL = getURL();
                if (jsonURL != null) {
                    org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finer("Read Config(URL): " + jsonURL);
                    readObject = JSonStorage.restoreFromByteArray(IO.readURL(jsonURL), cryptKey == null, cryptKey, getTypeRef(), dummyObject);
                    readFlag.set(true);
                }
            }
        }
        return readObject;
    }

    /**
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    protected Object read() throws InstantiationException, IllegalAccessException, IOException {
        final AtomicBoolean readFlag = new AtomicBoolean(false);
        try {
            final Object dummyObject = new Object();
            Object readObject = readObject(dummyObject, readFlag);
            if (readObject == dummyObject || !readFlag.get()) {
                final T def = this.getDefaultValue();
                if (def != null) {
                    return def;
                }
                Annotation ann;
                final DefaultJsonObject defaultJson = this.getAnnotation(DefaultJsonObject.class);
                final DefaultFactory df = this.getAnnotation(DefaultFactory.class);
                if (defaultJson != null) {
                    this.setDefaultValue((T) JSonStorage.restoreFromString(defaultJson.value(), getTypeRef(), null));
                    return this.getDefaultValue();
                } else if (df != null) {
                    this.setDefaultValue((T) df.value().newInstance().getDefaultValue());
                    return this.getDefaultValue();
                } else if ((ann = this.getAnnotation(this.getDefaultAnnotation())) != null) {
                    try {
                        this.setDefaultValue((T) ann.annotationType().getMethod("value", new Class[] {}).invoke(ann, new Object[] {}));
                    } catch (final Throwable e) {
                        e.printStackTrace();
                    }
                    return this.getDefaultValue();
                } else {
                    return null;
                }
            }
            return readObject;
        } finally {
            if (!readFlag.get() && getURL() == null && isAllowWriteDefaultObjects()) {
                this.write(this.getDefaultValue());
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.storage.config.KeyHandler#validateValue(java.lang.Object)
     */
    @Override
    protected void validateValue(final T object) throws Throwable {
    }

    /**
     * @param object
     */
    protected void write(final T object) {
        getStorageHandler().writeObject(this, object);
        this.url = null;
    }
}
