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
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.appwork.exceptions.WTFException;
import org.appwork.scheduler.DelayedRunnable;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.InvalidTypeException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.JsonKeyValueStorage;
import org.appwork.storage.Storage;
import org.appwork.storage.StorageException;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.InterfaceParseException;
import org.appwork.storage.config.annotations.AllowStorage;
import org.appwork.storage.config.annotations.CryptedStorage;
import org.appwork.storage.config.annotations.DefaultBooleanArrayValue;
import org.appwork.storage.config.annotations.DefaultByteArrayValue;
import org.appwork.storage.config.annotations.DefaultDoubleArrayValue;
import org.appwork.storage.config.annotations.DefaultFloatArrayValue;
import org.appwork.storage.config.annotations.DefaultIntArrayValue;
import org.appwork.storage.config.annotations.DefaultLongArrayValue;
import org.appwork.storage.config.events.ConfigEvent;
import org.appwork.storage.config.events.ConfigEventSender;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.ReflectionUtils;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.appwork.utils.reflection.Clazz;
import org.appwork.utils.swing.dialog.Dialog;

/**
 * @author thomas
 * @param <T>
 *
 */
public class StorageHandler<T extends ConfigInterface> implements InvocationHandler {
    private final static LinkedHashMap<String, Runnable>    DELAYEDWRITES = new LinkedHashMap<String, Runnable>();
    protected static final DelayedRunnable                  SAVEDELAYER   = new DelayedRunnable(5000, 30000) {
                                                                              @Override
                                                                              public void delayedrun() {
                                                                                  StorageHandler.saveAll();
                                                                              }
                                                                          };
    private static final HashMap<StorageHandler<?>, String> STORAGEMAP;
    static {
        // important, because getDefaultLogger might initialize StorageHandler and access to STORAGEMAP must be ensured in constructor of
        // StorageHandler
        STORAGEMAP = new HashMap<StorageHandler<?>, String>();
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {
            @Override
            public long getMaxDuration() {
                return 0;
            }

            @Override
            public int getHookPriority() {
                return 0;
            }

            @Override
            public void onShutdown(final ShutdownRequest shutdownRequest) {
                flushWrites();
            }

            @Override
            public String toString() {
                synchronized (DELAYEDWRITES) {
                    return "ShutdownEvent: ProcessDelayedWrites num=" + DELAYEDWRITES.size();
                }
            }
        });
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {
            @Override
            public long getMaxDuration() {
                return 0;
            }

            @Override
            public int getHookPriority() {
                return 0;
            }

            @Override
            public void onShutdown(final ShutdownRequest shutdownRequest) {
                StorageHandler.saveAll();
            }

            @Override
            public String toString() {
                return "ShutdownEvent: SaveAllStorageHandler";
            }
        });
    }

    public static JsonKeyValueStorage createPrimitiveStorage(final File filePath, final String classPath, final Class<? extends ConfigInterface> configInterface) {
        final CryptedStorage crypted = configInterface.getAnnotation(CryptedStorage.class);
        final JsonKeyValueStorage ret;
        if (crypted != null) {
            byte[] key = JSonStorage.KEY;
            if (crypted.key() != null) {
                key = crypted.key();
            }
            final URL urlClassPath;
            if (classPath != null) {
                // Do not use Application.getResourceUrl here! it might return urls to local files instead of classpath urls
                urlClassPath = Application.class.getClassLoader().getResource(classPath + ".ejs");
            } else {
                urlClassPath = null;
            }
            ret = new JsonKeyValueStorage(new File(filePath.getAbsolutePath() + ".ejs"), urlClassPath, false, key);
        } else {
            final URL urlClassPath;
            if (classPath != null) {
                // Do not use Application.getResourceUrl here! it might return urls to local files instead of classpath urls
                urlClassPath = Application.class.getClassLoader().getResource(classPath + ".json");
            } else {
                urlClassPath = null;
            }
            ret = new JsonKeyValueStorage(new File(filePath.getAbsolutePath() + ".json"), urlClassPath, true, null);
        }
        return ret;
    }

    public static void flushWrites() {
        final LogInterface logger = org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger();
        while (true) {
            synchronized (DELAYEDWRITES) {
                final Iterator<Runnable> it = DELAYEDWRITES.values().iterator();
                if (it.hasNext()) {
                    final Runnable next = it.next();
                    try {
                        next.run();
                    } catch (final Throwable th) {
                        logger.log(th);
                    } finally {
                        it.remove();
                    }
                } else {
                    return;
                }
            }
        }
    }

    private static final AtomicBoolean DELAYED_WRITES = new AtomicBoolean(false);

    public static void setDelayedWritesEnabled(final boolean enabled) {
        if (DELAYED_WRITES.compareAndSet(!enabled, enabled)) {
            if (!enabled) {
                flushWrites();
            }
        }
    }

    public static boolean isDelayedWritesEnabled() {
        return DELAYED_WRITES.get();
    }

    public static void enqueueWrite(final Runnable run, final String ID, final boolean delayWrite) {
        final boolean write;
        synchronized (DELAYEDWRITES) {
            final boolean isShuttingDown = ShutdownController.getInstance().isShuttingDown();
            final boolean isDelayedWritesEnabled = isDelayedWritesEnabled();
            if (isShuttingDown || !delayWrite || !isDelayedWritesEnabled) {
                if (DELAYEDWRITES.size() > 0) {
                    DELAYEDWRITES.remove(ID);
                }
                write = true;
            } else {
                DELAYEDWRITES.put(ID, run);
                write = false;
            }
        }
        if (write) {
            run.run();
        }
    }

    /**
     * @param interfaceName
     * @param storage
     * @return
     */
    public static StorageHandler<?> getStorageHandler(final String interfaceName, final String storage) {
        synchronized (StorageHandler.STORAGEMAP) {
            final String ID = interfaceName + "." + storage;
            final Iterator<Entry<StorageHandler<?>, String>> it = StorageHandler.STORAGEMAP.entrySet().iterator();
            StorageHandler<?> ret = null;
            while (it.hasNext()) {
                final Entry<StorageHandler<?>, String> next = it.next();
                if (ID.equals(next.getValue()) && (ret = next.getKey()) != null) {
                    return ret;
                }
            }
        }
        return null;
    }

    public static void saveAll() {
        synchronized (StorageHandler.STORAGEMAP) {
            for (final StorageHandler<?> storageHandler : StorageHandler.STORAGEMAP.keySet()) {
                try {
                    if (storageHandler.isSaveInShutdownHookEnabled()) {
                        storageHandler.getPrimitiveStorage().save();
                    }
                } catch (final Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private final Class<T>                         configInterface;
    protected final HashMap<Method, KeyHandler<?>> method2KeyHandlerMap      = new HashMap<Method, KeyHandler<?>>();
    protected final HashMap<String, KeyHandler<?>> key2KeyHandlerMap         = new HashMap<String, KeyHandler<?>>();
    protected final Storage                        primitiveStorage;
    private final File                             path;
    private ConfigEventSender<Object>              eventSender               = null;
    private String                                 relativCPPath;
    // set externaly to start profiling
    public static HashMap<String, Long>            PROFILER_MAP              = null;
    public static HashMap<String, Long>            PROFILER_CALLNUM_MAP      = null;
    private volatile WriteStrategy                 writeStrategy             = null;
    private boolean                                objectCacheEnabled        = true;
    private final String                           storageID;
    boolean                                        saveInShutdownHookEnabled = true;
    private DefaultFactoryInterface                defaultFactory;

    public DefaultFactoryInterface getDefaultFactory() {
        return defaultFactory;
    }

    public void setDefaultFactory(DefaultFactoryInterface defaultFactory) {
        this.defaultFactory = defaultFactory;
    }

    /**
     * @param name
     * @param storage2
     * @param configInterface
     */
    public StorageHandler(final File filePath, final Class<T> configInterface) {
        this.configInterface = configInterface;
        this.path = filePath;
        preInit(path, configInterface);
        if (filePath.getName().endsWith(".json") || filePath.getName().endsWith(".ejs")) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().warning(filePath + " should not have an extension!!");
        }
        final File expected = Application.getResource("cfg/" + configInterface.getName());
        String storageID = null;
        if (!this.path.equals(expected)) {
            storageID = Files.getRelativePath(expected.getParentFile().getParentFile(), this.path);
            if (StringUtils.isEmpty(storageID)) {
                storageID = this.path.getAbsolutePath();
            }
        }
        this.storageID = storageID;
        String relativePath = null;
        try {
            // this way the config system reads default values from the classpath (bin folder or jar)
            relativePath = Files.getRelativePath(Application.getResource(""), filePath);
            this.relativCPPath = relativePath;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        this.primitiveStorage = StorageHandler.createPrimitiveStorage(this.path, relativePath, configInterface);
        final CryptedStorage cryptedStorage = configInterface.getAnnotation(CryptedStorage.class);
        if (cryptedStorage != null) {
            this.validateKeys(cryptedStorage);
        }
        try {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finer("Init StorageHandler for Interface:" + configInterface.getName() + "|Path:" + this.path);
            this.parseInterface();
        } catch (final InterfaceParseException e) {
            throw e;
        } catch (final Throwable e) {
            throw new InterfaceParseException(e);
        }
        this.addStorageHandler(this, configInterface.getName(), getStorageID());
    }

    /**
     * @param configInterfac
     * @param path
     *
     */
    protected void preInit(File path, Class<T> configInterfac) {
        // TODO Auto-generated method stub
    }

    public StorageHandler(final Storage storage, final Class<T> configInterface) {
        String storagePath = storage.getID();
        if (storagePath.endsWith(".json")) {
            storagePath = storagePath.replaceFirst("\\.json$", "");
        } else if (storagePath.endsWith(".ejs")) {
            storagePath = storagePath.replaceFirst("\\.ejs$", "");
        }
        this.primitiveStorage = storage;
        this.path = new File(storagePath);
        this.configInterface = configInterface;
        preInit(path, configInterface);
        final File expected = Application.getResource("cfg/" + configInterface.getName());
        String storageID = null;
        if (!this.path.equals(expected)) {
            storageID = Files.getRelativePath(expected.getParentFile().getParentFile(), this.path);
            if (StringUtils.isEmpty(storageID)) {
                storageID = this.path.getAbsolutePath();
            }
        }
        this.storageID = storageID;
        final CryptedStorage cryptedStorage = configInterface.getAnnotation(CryptedStorage.class);
        if (cryptedStorage != null) {
            this.validateKeys(cryptedStorage);
        }
        try {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finer("Init StorageHandler for Interface:" + configInterface.getName() + "|Path:" + this.path);
            this.parseInterface();
        } catch (final Throwable e) {
            throw new InterfaceParseException(e);
        }
        this.addStorageHandler(this, configInterface.getName(), getStorageID());
    }

    protected StorageHandler(final Class<T> configInterface) {
        this.primitiveStorage = null;
        this.path = null;
        this.configInterface = configInterface;
        this.storageID = null;
        final CryptedStorage cryptedStorage = configInterface.getAnnotation(CryptedStorage.class);
        if (cryptedStorage != null) {
            this.validateKeys(cryptedStorage);
        }
        try {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finer("Init StorageHandler for Interface:" + configInterface.getName() + "|Path:" + this.path);
            this.parseInterface();
        } catch (final Throwable e) {
            throw new InterfaceParseException(e);
        }
        this.addStorageHandler(this, configInterface.getName(), getStorageID());
    }

    protected void requestSave() {
        SAVEDELAYER.resetAndStart();
    }

    /**
     * @param path2
     * @param configInterface2
     * @throws URISyntaxException
     */
    public StorageHandler(final String classPath, final Class<T> configInterface) throws URISyntaxException {
        this.configInterface = configInterface;
        this.relativCPPath = classPath;
        if (classPath.endsWith(".json") || classPath.endsWith(".ejs")) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().warning(classPath + " should not have an extension!!");
        }
        this.path = Application.getResource(classPath);
        preInit(path, configInterface);
        final File expected = Application.getResource("cfg/" + configInterface.getName());
        String storageID = null;
        if (!this.path.equals(expected)) {
            storageID = Files.getRelativePath(expected.getParentFile().getParentFile(), this.path);
            if (StringUtils.isEmpty(storageID)) {
                storageID = this.path.getAbsolutePath();
            }
        }
        this.storageID = storageID;
        this.primitiveStorage = StorageHandler.createPrimitiveStorage(Application.getResource(classPath), classPath, configInterface);
        final CryptedStorage cryptedStorage = configInterface.getAnnotation(CryptedStorage.class);
        if (cryptedStorage != null) {
            this.validateKeys(cryptedStorage);
        }
        try {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().finer("Init StorageHandler for Interface:" + configInterface.getName() + "|Path:" + this.path);
            this.parseInterface();
        } catch (final Throwable e) {
            throw new InterfaceParseException(e);
        }
        this.addStorageHandler(this, configInterface.getName(), getStorageID());
    }

    protected void addStorageHandler(final StorageHandler<? extends ConfigInterface> storageHandler, final String interfaceName, final String storage) {
        synchronized (StorageHandler.STORAGEMAP) {
            final StorageHandler<?> existing = StorageHandler.getStorageHandler(interfaceName, storage);
            if (existing != null && existing != storageHandler) {
                throw new IllegalStateException("You cannot init the configinterface " + getConfigInterface() + " twice");
            }
            final String ID = interfaceName + "." + storage;
            StorageHandler.STORAGEMAP.put(storageHandler, ID);
        }
    }

    public Object runDefaultValueFactory(KeyHandler<?> handler, Object o) {
        DefaultFactoryInterface df = defaultFactory;
        if (df == null) {
            return o;
        }
        return df.getDefaultValue(handler, o);
    }

    /**
     * @param key2
     * @param genericReturnType
     * @return
     */
    protected KeyHandler<?> createKeyHandler(final String key, final Type type) {
        if (Clazz.isBoolean(type)) {
            return new BooleanKeyHandler(this, key);
        } else if (Clazz.isByte(type)) {
            return new ByteKeyHandler(this, key);
        } else if (Clazz.isDouble(type)) {
            return new DoubleKeyHandler(this, key);
        } else if (Clazz.isFloat(type)) {
            return new FloatKeyHandler(this, key);
        } else if (Clazz.isInteger(type)) {
            return new IntegerKeyHandler(this, key);
        } else if (type instanceof Class && ((Class<?>) type).isEnum()) {
            return new EnumKeyHandler(this, key);
        } else if (type == String.class) {
            return new StringKeyHandler(this, key);
        } else if (Clazz.isLong(type)) {
            return new LongKeyHandler(this, key);
        } else if (type instanceof Class && ((Class<?>) type).isArray()) {
            final Class<?> ct = ((Class<?>) type).getComponentType();
            final boolean p = ct.isPrimitive();
            if (Clazz.isBoolean(ct)) {
                if (p) {
                    return new ListHandler<boolean[]>(this, key, type) {
                        @Override
                        protected Class<? extends Annotation> getDefaultAnnotation() {
                            return DefaultBooleanArrayValue.class;
                        }
                    };
                } else {
                    return new ListHandler<Boolean[]>(this, key, type) {
                        @Override
                        protected Class<? extends Annotation> getDefaultAnnotation() {
                            return DefaultBooleanArrayValue.class;
                        }
                    };
                }
            } else if (Clazz.isLong(ct)) {
                if (p) {
                    return new ListHandler<long[]>(this, key, type) {
                        @Override
                        protected Class<? extends Annotation> getDefaultAnnotation() {
                            return DefaultLongArrayValue.class;
                        }
                    };
                } else {
                    return new ListHandler<Long[]>(this, key, type) {
                        @Override
                        protected Class<? extends Annotation> getDefaultAnnotation() {
                            return DefaultLongArrayValue.class;
                        }
                    };
                }
            } else if (Clazz.isInteger(ct)) {
                if (p) {
                    return new ListHandler<int[]>(this, key, type) {
                        @Override
                        protected Class<? extends Annotation> getDefaultAnnotation() {
                            return DefaultIntArrayValue.class;
                        }
                    };
                } else {
                    return new ListHandler<Integer[]>(this, key, type) {
                        @Override
                        protected Class<? extends Annotation> getDefaultAnnotation() {
                            return DefaultIntArrayValue.class;
                        }
                    };
                }
            } else if (Clazz.isByte(ct)) {
                if (p) {
                    return new ListHandler<byte[]>(this, key, type) {
                        @Override
                        protected Class<? extends Annotation> getDefaultAnnotation() {
                            return DefaultByteArrayValue.class;
                        }
                    };
                } else {
                    return new ListHandler<Byte[]>(this, key, type) {
                        @Override
                        protected Class<? extends Annotation> getDefaultAnnotation() {
                            return DefaultByteArrayValue.class;
                        }
                    };
                }
            } else if (Clazz.isFloat(ct)) {
                if (p) {
                    return new ListHandler<float[]>(this, key, type) {
                        @Override
                        protected Class<? extends Annotation> getDefaultAnnotation() {
                            return DefaultFloatArrayValue.class;
                        }
                    };
                } else {
                    return new ListHandler<Float[]>(this, key, type) {
                        @Override
                        protected Class<? extends Annotation> getDefaultAnnotation() {
                            return DefaultFloatArrayValue.class;
                        }
                    };
                }
            } else if (ct == String.class) {
                return new StringListHandler(this, key, type);
            } else if (ct.isEnum()) {
                return new EnumListHandler(this, key, type);
            } else if (Clazz.isDouble(ct)) {
                if (p) {
                    return new ListHandler<double[]>(this, key, type) {
                        @Override
                        protected Class<? extends Annotation> getDefaultAnnotation() {
                            return DefaultDoubleArrayValue.class;
                        }
                    };
                } else {
                    return new ListHandler<Double[]>(this, key, type) {
                        @Override
                        protected Class<? extends Annotation> getDefaultAnnotation() {
                            return DefaultDoubleArrayValue.class;
                        }
                    };
                }
            } else {
                return new ObjectKeyHandler(this, key, type);
            }
        } else {
            return new ObjectKeyHandler(this, key, type);
        }
    }

    /**
     * @param e
     */
    protected void error(final Throwable e) {
        new Thread("ERROR THROWER") {
            @Override
            public void run() {
                Dialog.getInstance().showExceptionDialog(e.getClass().getSimpleName(), e.getMessage(), e);
            }
        }.start();
        // we could throw the exception here, but this would kill the whole
        // interface. So we just show a a dialog for the developer and let the
        // rest of the interface work.
    }

    protected void fireEvent(final ConfigEvent.Types type, final KeyHandler<?> keyHandler, final Object parameter) {
        if (this.hasEventListener()) {
            this.getEventSender().fireEvent(new ConfigEvent(type, keyHandler, parameter));
        }
    }

    public Class<T> getConfigInterface() {
        return this.configInterface;
    }

    public synchronized ConfigEventSender<Object> getEventSender() {
        if (this.eventSender == null) {
            this.eventSender = new ConfigEventSender<Object>();
        }
        return this.eventSender;
    }

    /**
     * @param key2
     */
    @SuppressWarnings("unchecked")
    public KeyHandler<Object> getKeyHandler(final String key) {
        return this.getKeyHandler(key, KeyHandler.class);
    }

    /**
     * @param <RawClass>
     * @param string
     * @param class1
     * @return
     */
    @SuppressWarnings("unchecked")
    public <E extends KeyHandler<?>> E getKeyHandler(final String key, final Class<E> class1) {
        final String keyHandlerKey = key.toLowerCase(Locale.ENGLISH);
        final KeyHandler<?> ret = key2KeyHandlerMap.get(keyHandlerKey);
        if (ret != null) {
            return (E) ret;
        }
        throw new NullPointerException("No KeyHandler: " + key + " in " + getConfigInterface());
    }

    /**
     * @return
     */
    public File getPath() {
        return this.path;
    }

    /**
     * @param keyHandler
     * @return
     */
    public Object getPrimitive(final KeyHandler<?> keyHandler) {
        // only evaluate defaults of required
        if (getPrimitiveStorage().hasProperty(keyHandler.getKey())) {
            if (Clazz.isBoolean(keyHandler.getRawClass())) {
                return this.getPrimitive(keyHandler.getKey(), false);
            } else if (Clazz.isLong(keyHandler.getRawClass())) {
                return this.getPrimitive(keyHandler.getKey(), 0l);
            } else if (Clazz.isInteger(keyHandler.getRawClass())) {
                return this.getPrimitive(keyHandler.getKey(), 0);
            } else if (Clazz.isFloat(keyHandler.getRawClass())) {
                return this.getPrimitive(keyHandler.getKey(), 0.0f);
            } else if (Clazz.isByte(keyHandler.getRawClass())) {
                return this.getPrimitive(keyHandler.getKey(), (byte) 0);
            } else if (keyHandler.getRawClass() == String.class) {
                return this.getPrimitive(keyHandler.getKey(), (String) null);
                // } else if (getter.getRawClass() == String[].class) {
                // return this.get(getter.getKey(),
                // getter.getDefaultStringArray());
            } else if (keyHandler.getRawClass().isEnum()) {
                return this.getPrimitive(keyHandler.getKey(), keyHandler.getDefaultValue());
            } else if (keyHandler.getRawClass() == Double.class | keyHandler.getRawClass() == double.class) {
                return this.getPrimitive(keyHandler.getKey(), 0.0d);
            } else {
                throw new StorageException("Invalid datatype: " + keyHandler.getRawClass());
            }
        } else {
            if (Clazz.isBoolean(keyHandler.getRawClass())) {
                return this.getPrimitive(keyHandler.getKey(), keyHandler.getDefaultValue());
            } else if (Clazz.isLong(keyHandler.getRawClass())) {
                return this.getPrimitive(keyHandler.getKey(), keyHandler.getDefaultValue());
            } else if (Clazz.isInteger(keyHandler.getRawClass())) {
                return this.getPrimitive(keyHandler.getKey(), keyHandler.getDefaultValue());
            } else if (Clazz.isFloat(keyHandler.getRawClass())) {
                return this.getPrimitive(keyHandler.getKey(), keyHandler.getDefaultValue());
            } else if (Clazz.isByte(keyHandler.getRawClass())) {
                return this.getPrimitive(keyHandler.getKey(), keyHandler.getDefaultValue());
            } else if (keyHandler.getRawClass() == String.class) {
                return this.getPrimitive(keyHandler.getKey(), keyHandler.getDefaultValue());
                // } else if (getter.getRawClass() == String[].class) {
                // return this.get(getter.getKey(),
                // getter.getDefaultStringArray());
            } else if (keyHandler.getRawClass().isEnum()) {
                return this.getPrimitive(keyHandler.getKey(), keyHandler.getDefaultValue());
            } else if (Clazz.isDouble(keyHandler.getRawClass())) {
                return this.getPrimitive(keyHandler.getKey(), keyHandler.getDefaultValue());
            } else {
                throw new StorageException("Invalid datatype: " + keyHandler.getRawClass());
            }
        }
    }

    protected void writeObject(final ListHandler<?> keyHandler, final Object object) {
        final byte[] jsonBytes = JSonStorage.getMapper().objectToByteArray(object);
        final byte[] cryptKey = keyHandler.getCryptKey();
        final File path = keyHandler.getPath();
        final Runnable run = new Runnable() {
            @Override
            public void run() {
                JSonStorage.saveTo(path, cryptKey == null, cryptKey, jsonBytes);
            }
        };
        StorageHandler.enqueueWrite(run, path.getAbsolutePath(), isDelayedWriteAllowed(keyHandler));
    }

    protected Object readObject(final ListHandler<?> keyHandler, final AtomicBoolean readFlag) {
        return null;
    }

    protected boolean isDelayedWriteAllowed(final KeyHandler<?> keyHandler) {
        return keyHandler.isDelayedWriteAllowed();
    }

    /**
     * @param <E>
     * @param key2
     * @param defaultBoolean
     * @return
     */
    public <E> E getPrimitive(final String key, final E def) {
        return getPrimitiveStorage().get(key, def);
    }

    public Storage getPrimitiveStorage() {
        return this.primitiveStorage;
    }

    public String getRelativCPPath() {
        return this.relativCPPath;
    }

    public String getStorageID() {
        return this.storageID;
    }

    /**
     * @throws NullPointerException
     *             if there is no keyhandler for key
     * @param string
     * @return
     */
    public Object getValue(final String key) {
        return this.getKeyHandler(key).getValue();
    }

    public WriteStrategy getWriteStrategy() {
        return this.writeStrategy;
    }

    public synchronized boolean hasEventListener() {
        return this.eventSender != null && this.eventSender.hasListener();
    }

    @SuppressWarnings("unchecked")
    public Object invoke(final Object instance, final Method m, final Object[] parameter) throws Throwable {
        if (m != null) {
            final long t = StorageHandler.PROFILER_MAP == null ? 0 : System.nanoTime();
            try {
                final KeyHandler<?> handler = this.method2KeyHandlerMap.get(m);
                if (handler != null) {
                    if (handler.isGetter(m)) {
                        final Object ret = handler.getValue();
                        if (ret instanceof Number) {
                            return ReflectionUtils.castNumber((Number) ret, handler.getRawClass());
                        } else {
                            return ret;
                        }
                    } else {
                        ((KeyHandler<Object>) handler).setValue(parameter[0]);
                        if (this.writeStrategy != null) {
                            this.writeStrategy.write(this, handler);
                        }
                        return null;
                    }
                } else if (m.getName().equals("toString")) {
                    return this.toString();
                    // } else if (m.getName().equals("addListener")) {
                    // this.eventSender.addListener((ConfigEventListener)
                    // parameter[0]);
                    // return null;
                    // } else if (m.getName().equals("removeListener")) {
                    // this.eventSender.removeListener((ConfigEventListener)
                    // parameter[0]);
                    // return null;
                } else if (m.getName().equals("_getStorageHandler")) {
                    return this;
                } else if (m.getDeclaringClass() == Object.class) {
                    return m.invoke(this, parameter);
                } else {
                    throw new WTFException(m + " ??? no keyhandler. This is not possible!");
                }
            } finally {
                if (StorageHandler.PROFILER_MAP != null && m != null) {
                    final long dur = System.nanoTime() - t;
                    final String id = m.toString();
                    Long g = StorageHandler.PROFILER_MAP.get(id);
                    if (g == null) {
                        g = 0l;
                    }
                    StorageHandler.PROFILER_MAP.put(id, g + dur);
                }
                if (StorageHandler.PROFILER_CALLNUM_MAP != null && m != null) {
                    final String id = m.toString();
                    final Long g = StorageHandler.PROFILER_CALLNUM_MAP.get(id);
                    StorageHandler.PROFILER_CALLNUM_MAP.put(id, g == null ? 1 : g + 1);
                }
            }
        } else {
            // yes.... Method m may be null. this happens if we call a
            // method in the interface's own static init.
            return this;
        }
    }

    /**
     * @return
     */
    public boolean isObjectCacheEnabled() {
        return this.objectCacheEnabled;
    }

    public boolean isSaveInShutdownHookEnabled() {
        return this.saveInShutdownHookEnabled;
    }

    protected int getParameterCount(final Method method) {
        if (method != null) {
            return method.getParameterTypes().length;
        }
        return 0;
    }

    /**
     * @throws Throwable
     *
     */
    protected void parseInterface() throws Throwable {
        final HashMap<String, Method> keyGetterMap = new HashMap<String, Method>();
        final HashMap<String, Method> keySetterMap = new HashMap<String, Method>();
        String key;
        final HashMap<String, KeyHandler<?>> parseMap = new HashMap<String, KeyHandler<?>>();
        Class<?> clazz = getConfigInterface();
        while (clazz != null && clazz != ConfigInterface.class) {
            for (final Method m : clazz.getDeclaredMethods()) {
                final String methodName = m.getName().toLowerCase(Locale.ENGLISH);
                if (methodName.startsWith("get")) {
                    key = methodName.substring(3);
                    // we do not allow to setters/getters with the same name but
                    // different cases. this only confuses the user when editing
                    // the
                    // later config file
                    if (keyGetterMap.containsKey(key)) {
                        if (m.getName().equals(keyGetterMap.get(key).getName()) && getParameterCount(m) == getParameterCount(keyGetterMap.get(key))) {
                            // overridden method. that's ok
                            LoggerFactory.getDefaultLogger().info("Overridden Config Key found " + keyGetterMap.get(key) + "<-->" + m);
                            continue;
                        }
                        this.error(new InterfaceParseException("Key " + key + " Dupe found! " + keyGetterMap.get(key) + "<-->" + m));
                        continue;
                    }
                    keyGetterMap.put(key, m);
                    if (getParameterCount(m) > 0) {
                        this.error(new InterfaceParseException("Getter " + m + " has parameters."));
                        keyGetterMap.remove(key);
                        continue;
                    }
                    try {
                        final AllowStorage allow = m.getAnnotation(AllowStorage.class);
                        boolean found = false;
                        if (allow != null) {
                            for (final Class<?> c : allow.value()) {
                                if (m.getReturnType() == c) {
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found) {
                            JSonStorage.canStore(m.getGenericReturnType(), false);
                        }
                    } catch (final InvalidTypeException e) {
                        final AllowStorage allow = m.getAnnotation(AllowStorage.class);
                        boolean found = false;
                        if (allow != null) {
                            for (final Class<?> c : allow.value()) {
                                if (e.getType() == c) {
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found) {
                            this.error(new InterfaceParseException(e));
                            keyGetterMap.remove(key);
                            continue;
                        }
                    }
                    KeyHandler<?> kh = parseMap.get(key);
                    if (kh == null) {
                        kh = this.createKeyHandler(key, m.getGenericReturnType());
                        parseMap.put(key, kh);
                    }
                    kh.setGetMethod(m);
                    addKeyHandler(kh);
                } else if (methodName.startsWith("is")) {
                    key = methodName.substring(2);
                    // we do not allow to setters/getters with the same name but
                    // different cases. this only confuses the user when editing
                    // the
                    // later config file
                    if (keyGetterMap.containsKey(key)) {
                        if (m.getName().equals(keyGetterMap.get(key).getName()) && getParameterCount(m) == getParameterCount(keyGetterMap.get(key))) {
                            // overridden method. that's ok
                            LoggerFactory.getDefaultLogger().info("Overridden Config Key found " + keyGetterMap.get(key) + "<-->" + m);
                            continue;
                        }
                        this.error(new InterfaceParseException("Key " + key + " Dupe found! " + keyGetterMap.get(key) + "<-->" + m));
                        continue;
                    }
                    keyGetterMap.put(key, m);
                    if (getParameterCount(m) > 0) {
                        this.error(new InterfaceParseException("Getter " + m + " has parameters."));
                        keyGetterMap.remove(key);
                        continue;
                    }
                    try {
                        JSonStorage.canStore(m.getGenericReturnType(), false);
                    } catch (final InvalidTypeException e) {
                        this.error(new InterfaceParseException(e));
                        keyGetterMap.remove(key);
                        continue;
                    }
                    KeyHandler<?> kh = parseMap.get(key);
                    if (kh == null) {
                        kh = this.createKeyHandler(key, m.getGenericReturnType());
                        parseMap.put(key, kh);
                    }
                    kh.setGetMethod(m);
                    addKeyHandler(kh);
                } else if (methodName.startsWith("set")) {
                    key = methodName.substring(3);
                    if (keySetterMap.containsKey(key)) {
                        if (m.getName().equals(keyGetterMap.get(key).getName()) && getParameterCount(m) == getParameterCount(keyGetterMap.get(key))) {
                            // overridden method. that's ok
                            LoggerFactory.getDefaultLogger().info("Overridden Config Key found " + keyGetterMap.get(key) + "<-->" + m);
                            continue;
                        }
                        this.error(new InterfaceParseException("Key " + key + " Dupe found! " + keySetterMap.get(key) + "<-->" + m));
                        continue;
                    }
                    keySetterMap.put(key, m);
                    if (getParameterCount(m) != 1) {
                        this.error(new InterfaceParseException("Setter " + m + " has !=1 parameters."));
                        keySetterMap.remove(key);
                        continue;
                    }
                    if (m.getReturnType() != void.class) {
                        this.error(new InterfaceParseException("Setter " + m + " has a returntype != void"));
                        keySetterMap.remove(key);
                        continue;
                    }
                    try {
                        JSonStorage.canStore(m.getGenericParameterTypes()[0], false);
                    } catch (final InvalidTypeException e) {
                        final AllowStorage allow = m.getAnnotation(AllowStorage.class);
                        boolean found = false;
                        if (allow != null) {
                            for (final Class<?> c : allow.value()) {
                                if (e.getType() == c) {
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found) {
                            this.error(new InterfaceParseException(e));
                            keySetterMap.remove(key);
                            continue;
                        }
                    }
                    KeyHandler<?> kh = parseMap.get(key);
                    if (kh == null) {
                        kh = this.createKeyHandler(key, m.getGenericParameterTypes()[0]);
                        parseMap.put(key, kh);
                    }
                    kh.setSetMethod(m);
                    addKeyHandler(kh);
                } else {
                    this.error(new InterfaceParseException("Only getter and setter allowed:" + m));
                    continue;
                }
            }
            // run down the calss hirarchy to find all methods. getMethods does
            // not work, because it only finds public methods
            final Class<?>[] interfaces = clazz.getInterfaces();
            clazz = interfaces[0];
        }
        final ArrayList<KeyHandler<?>> keyHandlerToRemove = new ArrayList<KeyHandler<?>>();
        for (final KeyHandler<?> kh : key2KeyHandlerMap.values()) {
            try {
                kh.init();
            } catch (final Throwable e) {
                this.error(e);
                keyHandlerToRemove.add(kh);
            }
        }
        for (final KeyHandler<?> kh : keyHandlerToRemove) {
            removeKeyHandler(kh);
        }
    }

    public List<KeyHandler<?>> getKeyHandler() {
        return new ArrayList<KeyHandler<?>>(key2KeyHandlerMap.values());
    }

    private void removeKeyHandler(KeyHandler<?> keyHandler) {
        final Method getMethod = keyHandler.getGetMethod();
        if (getMethod != null) {
            this.method2KeyHandlerMap.remove(getMethod);
        }
        final Method setMethod = keyHandler.getSetMethod();
        if (setMethod != null) {
            this.method2KeyHandlerMap.remove(setMethod);
        }
        this.key2KeyHandlerMap.remove(keyHandler.getKey());
    }

    private void addKeyHandler(KeyHandler<?> keyHandler) {
        final Method getMethod = keyHandler.getGetMethod();
        if (getMethod != null) {
            this.method2KeyHandlerMap.put(getMethod, keyHandler);
        }
        final Method setMethod = keyHandler.getSetMethod();
        if (setMethod != null) {
            this.method2KeyHandlerMap.put(setMethod, keyHandler);
        }
        this.key2KeyHandlerMap.put(keyHandler.getKey(), keyHandler);
    }

    public void setObjectCacheEnabled(final boolean objectCacheEnabled) {
        this.objectCacheEnabled = objectCacheEnabled;
    }

    public void setSaveInShutdownHookEnabled(final boolean saveInShutdownHookEnabled) {
        this.saveInShutdownHookEnabled = saveInShutdownHookEnabled;
    }

    public void setWriteStrategy(final WriteStrategy writeStrategy) {
        this.writeStrategy = writeStrategy;
    }

    @Override
    public String toString() {
        final HashMap<String, Object> ret = new HashMap<String, Object>();
        for (final KeyHandler<?> h : key2KeyHandlerMap.values()) {
            try {
                ret.put(h.getKey(), this.invoke(null, h.getGetMethod(), new Object[] {}));
            } catch (final Throwable e) {
                e.printStackTrace();
                ret.put(h.getKey(), e.getMessage());
            }
        }
        return JSonStorage.toString(ret);
    }

    protected void validateKeys(final CryptedStorage crypted) {
    }

    public void write() {
        this.getPrimitiveStorage().save();
    }

    /**
     * @param key2
     */
    /**
     * @param b
     */
    public void setAllowWriteDefaultObjects(boolean b) {
        for (KeyHandler<?> kh : getKeyHandler()) {
            kh.setAllowWriteDefaultObjects(b);
        }
    }
}
