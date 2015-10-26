/**
 * Copyright (c) 2009 - 2011 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 *
 * This file is part of org.appwork.storage.config
 *
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.storage.config.handler;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URL;

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
    public static final int              MIN_LIFETIME   = 10000;
    private MinTimeWeakReference<Object> cache;
    private final TypeRef<Object>        typeRef;
    private final static Object          NULL           = new Object();

    private File                         path;
    private URL                          url;
    private boolean                      useObjectCache = false;
    private byte[]                       cryptKey       = null;

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
        if (this.useObjectCache && this.getStorageHandler().isObjectCacheEnabled()) {
            final MinTimeWeakReference<Object> lCache = this.cache;
            if (lCache != null) {
                return lCache.get();
            }
        }
        return null;
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
                return null;
            } else {
                return (T) value;
            }
        }
    }

    @Override
    protected void initDefaults() throws Throwable {
    }

    @Override
    protected void initHandler() throws Throwable {
        this.path = new File(this.storageHandler.getPath() + "." + this.getKey() + "." + (this.cryptKey != null ? "ejs" : "json"));
        if (this.storageHandler.getRelativCPPath() != null && !this.path.exists()) {
            // Remember: Application.getResourceUrl returns an url to the classpath (bin/jar) or to a file on the harddisk (cfg folder)
            // we do only want urls to the classpath here
            this.url = Application.class.getClassLoader().getResource(this.storageHandler.getRelativCPPath() + "." + this.getKey() + "." + (this.cryptKey != null ? "ejs" : "json"));
        }

        this.useObjectCache = this.getAnnotation(DisableObjectCache.class) == null;
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
    }

    protected void putCachedValue(Object value) {
        if (this.useObjectCache && this.getStorageHandler().isObjectCacheEnabled()) {
            if (value == null) {
                value = ListHandler.NULL;
            }
            this.cache = new MinTimeWeakReference<Object>(value, ListHandler.MIN_LIFETIME, "Storage " + this.getKey());
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

    /**
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    protected Object read() throws InstantiationException, IllegalAccessException, IOException {
        boolean exists = false;
        try {
            final Object dummy = new Object();
            Object ret = null;
            // prefer local file. like primitive storage does as well.
            if (path.exists()) {
                      org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finer("Read Config: " + this.path.getAbsolutePath());
                exists = path.exists();
                ret = JSonStorage.restoreFrom(this.path, this.cryptKey == null, this.cryptKey, this.typeRef, dummy);

            }
            if (ret == dummy || (!path.exists())) {
                if (this.url != null) {
                          org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finer("Read Config: " + this.url);
                    ret = JSonStorage.restoreFromString(IO.readURL(this.url), this.cryptKey == null, this.cryptKey, this.typeRef, dummy);
                }
            }

            if (ret == dummy) {
                final T def = this.getDefaultValue();
                if (def != null) {
                    return def;
                }
                Annotation ann;
                final DefaultJsonObject defaultJson = this.getAnnotation(DefaultJsonObject.class);
                final DefaultFactory df = this.getAnnotation(DefaultFactory.class);
                if (defaultJson != null) {
                    this.setDefaultValue((T) JSonStorage.restoreFromString(defaultJson.value(), this.typeRef, null));
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
            return ret;
        } finally {
            if (!exists && this.url == null) {
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
        JSonStorage.saveTo(this.path, this.cryptKey == null, this.cryptKey, JSonStorage.serializeToJson(object));
        this.url = null;
    }

}
