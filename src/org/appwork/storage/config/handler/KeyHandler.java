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
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.InterfaceParseException;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.AbstractCustomValueGetter;
import org.appwork.storage.config.annotations.AbstractValidator;
import org.appwork.storage.config.annotations.AllowStorage;
import org.appwork.storage.config.annotations.ConfigEntryKeywords;
import org.appwork.storage.config.annotations.CryptedStorage;
import org.appwork.storage.config.annotations.CustomValueGetter;
import org.appwork.storage.config.annotations.DefaultFactory;
import org.appwork.storage.config.annotations.DefaultJsonObject;
import org.appwork.storage.config.annotations.DefaultOnNull;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.DevConfig;
import org.appwork.storage.config.annotations.HexColorString;
import org.appwork.storage.config.annotations.LookUpKeys;
import org.appwork.storage.config.annotations.NoHeadless;
import org.appwork.storage.config.annotations.PlainStorage;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.annotations.ValidatorFactory;
import org.appwork.storage.config.events.ConfigEvent;
import org.appwork.storage.config.events.ConfigEvent.Types;
import org.appwork.storage.config.events.ConfigEventSender;
import org.appwork.utils.reflection.Clazz;

/**
 * @author thomas
 *
 */
public abstract class KeyHandler<RawClass> {
    private static final String                   ANNOTATION_PACKAGE_NAME = CryptedStorage.class.getPackage().getName();
    private static final String                   PACKAGE_NAME            = PlainStorage.class.getPackage().getName();
    private final String                          key;
    protected Method                              getMethod               = null;
    protected Method                              setMethod               = null;                                        ;
    protected final StorageHandler<?>             storageHandler;
    private boolean                               primitive;
    protected RawClass                            defaultValue;
    private ConfigEventSender<RawClass>           eventSender;
    private AbstractValidator<RawClass>           validatorFactory;
    protected AbstractCustomValueGetter<RawClass> customValueGetter;
    protected String[]                            backwardsCompatibilityLookupKeys;
    private boolean                               defaultOnNull           = false;

    /**
     * @param storageHandler
     * @param key2
     */
    protected KeyHandler(final StorageHandler<?> storageHandler, final String key) {
        this.storageHandler = storageHandler;
        this.key = key;
    }

    protected boolean isDefaultOnNull() {
        final boolean isPrimitive = getRawClass().isPrimitive();
        return defaultOnNull || isPrimitive;
    }

    public File getPath() {
        return null;
    }

    protected void checkBadAnnotations(final Class<? extends Annotation>... class1) {
        int checker = 0;
        if (this.getAnnotation(this.getDefaultAnnotation()) != null) {
            checker++;
        }
        if (this.getAnnotation(DefaultJsonObject.class) != null) {
            checker++;
        }
        if (this.getAnnotation(DefaultFactory.class) != null) {
            checker++;
        }
        if (checker > 1) {
            throw new InterfaceParseException("Make sure that you use only one  of getDefaultAnnotation,DefaultObjectValue or DefaultValue ");
        }
        this.checkBadAnnotations(getMethod, class1);
        if (setMethod != null) {
            this.checkBadAnnotations(setMethod, class1);
        }
    }

    public byte[] getCryptKey() {
        return getStorageHandler().getPrimitiveStorage().getCryptKey();
    }

    /**
     * @param m
     * @param class1
     */
    private void checkBadAnnotations(final Method m, final Class<? extends Annotation>... classes) {
        final Class<?>[] okForAll = new Class<?>[] { DefaultOnNull.class, HexColorString.class, CustomValueGetter.class, ValidatorFactory.class, DefaultJsonObject.class, DefaultFactory.class, AboutConfig.class, NoHeadless.class, DevConfig.class, RequiresRestart.class, AllowStorage.class, DescriptionForConfigEntry.class, ConfigEntryKeywords.class, CryptedStorage.class, PlainStorage.class };
        final Class<?>[] clazzes = new Class<?>[classes.length + okForAll.length];
        System.arraycopy(classes, 0, clazzes, 0, classes.length);
        System.arraycopy(okForAll, 0, clazzes, classes.length, okForAll.length);
        /**
         * This main mark is important!!
         */
        main: for (final Annotation a : m.getAnnotations()) {
            // all other Annotations are ok anyway
            if (a == null) {
                continue;
            }
            final String aName = a.annotationType().getName();
            if (!aName.startsWith(KeyHandler.PACKAGE_NAME)) {
                continue;
            }
            if (this.getDefaultAnnotation() != null && this.getDefaultAnnotation().isAssignableFrom(a.getClass())) {
                continue;
            }
            for (final Class<?> ok : clazzes) {
                if (ok.isAssignableFrom(a.getClass())) {
                    continue main;
                }
            }
            throw new InterfaceParseException("Bad Annotation: " + a + " for " + m);
        }
    }

    /**
     * @param valueUpdated
     * @param keyHandler
     * @param object
     */
    protected void fireEvent(final Types type, final KeyHandler<?> keyHandler, final Object parameter) {
        this.storageHandler.fireEvent(type, keyHandler, parameter);
        if (this.hasEventListener()) {
            this.getEventSender().fireEvent(new ConfigEvent(type, this, parameter));
        }
    }

    @SuppressWarnings("unchecked")
    protected Class<? extends Annotation>[] getAllowedAnnotations() {
        return (Class<? extends Annotation>[]) new Class<?>[] { LookUpKeys.class };
    }

    /**
     * @param <T>
     * @param class1
     * @return
     */
    public <T extends Annotation> T getAnnotation(final Class<T> class1) {
        if (class1 == null) {
            return null;
        }
        T ret = getMethod.getAnnotation(class1);
        if (ret == null && setMethod != null) {
            ret = setMethod.getAnnotation(class1);
        } else if (setMethod != null && setMethod.getAnnotation(class1) != null) {
            if (KeyHandler.ANNOTATION_PACKAGE_NAME.equals(class1.getPackage().getName())) {
                //
                throw new InterfaceParseException("Dupe Annotation in  " + this + " (" + class1 + ") " + setMethod);
            }
        }
        return ret;
    }

    /**
     * @return
     */
    public Class<?> getDeclaringClass() {
        if (this.getMethod != null) {
            return this.getMethod.getDeclaringClass();
        } else {
            return setMethod.getDeclaringClass();
        }
    }

    protected Class<? extends Annotation> getDefaultAnnotation() {
        return null;
    }

    public boolean hasDefaultValue() {
        try {
            final DefaultJsonObject defaultJson = this.getAnnotation(DefaultJsonObject.class);
            if (defaultJson != null && defaultJson.value() != null) {
                return true;
            }
            final Annotation ann = this.getAnnotation(this.getDefaultAnnotation());
            if (ann != null) {
                return true;
            }
            final DefaultFactory df = this.getAnnotation(DefaultFactory.class);
            if (df != null) {
                return true;
            }
            return false;
        } catch (final Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public RawClass getDefaultValue() {
        try {
            final DefaultFactory df = this.getAnnotation(DefaultFactory.class);
            if (df != null) {
                return (RawClass) storageHandler.runDefaultValueFactory(this, df.value().newInstance().getDefaultValue());
            }
            final DefaultJsonObject defaultJson = this.getAnnotation(DefaultJsonObject.class);
            if (defaultJson != null) {
                return (RawClass) storageHandler.runDefaultValueFactory(this, JSonStorage.restoreFromString(defaultJson.value(), new TypeRef<Object>(this.getRawType()) {
                }, null));
            }
            final Annotation ann = this.getAnnotation(this.getDefaultAnnotation());
            if (ann != null) {
                return (RawClass) storageHandler.runDefaultValueFactory(this, ann.annotationType().getMethod("value", new Class[] {}).invoke(ann, new Object[] {}));
            }
            return (RawClass) storageHandler.runDefaultValueFactory(this, this.defaultValue);
        } catch (final Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Lazy initialiser of the eventsender. we do not wnat to create an eventsender if nowbody uses it
     *
     * @return
     */
    public synchronized ConfigEventSender<RawClass> getEventSender() {
        if (this.eventSender == null) {
            this.eventSender = new ConfigEventSender<RawClass>();
        }
        return this.eventSender;
    }

    public Method getGetMethod() {
        return getMethod;
    }

    public String getKey() {
        return this.key;
    }

    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    public Class<RawClass> getRawClass() {
        return (Class<RawClass>) getMethod.getReturnType();
    }

    /**
     * @return
     */
    public Type getRawType() {
        if (this.getMethod != null) {
            return getMethod.getGenericReturnType();
        }
        return setMethod.getGenericParameterTypes()[0];
    }

    public Method getSetMethod() {
        return setMethod;
    }

    public StorageHandler<?> getStorageHandler() {
        return this.storageHandler;
    }

    public static enum AbstractTypeDefinition {
        BOOLEAN,
        INT,
        LONG,
        STRING,
        OBJECT,
        OBJECT_LIST,
        STRING_LIST,
        ENUM,
        BYTE,
        CHAR,
        DOUBLE,
        FLOAT,
        SHORT,
        BOOLEAN_LIST,
        BYTE_LIST,
        SHORT_LIST,
        LONG_LIST,
        INT_LIST,
        FLOAT_LIST,
        ENUM_LIST,
        DOUBLE_LIST,
        CHAR_LIST,
        UNKNOWN,
        HEX_COLOR,
        HEX_COLOR_LIST;
    }

    public AbstractTypeDefinition getAbstractType() {
        final Type ret = getRawType();
        if (ret instanceof Class) {
            final Class<?> clazz = (Class<?>) ret;
            if (Clazz.isBoolean(ret)) {
                return AbstractTypeDefinition.BOOLEAN;
            }
            if (Clazz.isByte(ret)) {
                return AbstractTypeDefinition.BYTE;
            }
            if (Clazz.isCharacter(ret)) {
                return AbstractTypeDefinition.CHAR;
            }
            if (Clazz.isDouble(ret)) {
                return AbstractTypeDefinition.DOUBLE;
            }
            if (Clazz.isEnum(ret)) {
                return AbstractTypeDefinition.ENUM;
            }
            if (Clazz.isFloat(ret)) {
                return AbstractTypeDefinition.FLOAT;
            }
            if (Clazz.isInteger(ret)) {
                return AbstractTypeDefinition.INT;
            }
            if (Clazz.isLong(ret)) {
                return AbstractTypeDefinition.LONG;
            }
            if (Clazz.isShort(ret)) {
                return AbstractTypeDefinition.SHORT;
            }
            if (Clazz.isString(ret)) {
                if (this.getAnnotation(HexColorString.class) != null) {
                    return AbstractTypeDefinition.HEX_COLOR;
                }
                return AbstractTypeDefinition.STRING;
            }
            if (clazz.isArray()) {
                final Class aType = ((Class) ret).getComponentType();
                if (Clazz.isBoolean(aType)) {
                    return AbstractTypeDefinition.BOOLEAN_LIST;
                }
                if (Clazz.isByte(aType)) {
                    return AbstractTypeDefinition.BYTE_LIST;
                }
                if (Clazz.isCharacter(aType)) {
                    return AbstractTypeDefinition.CHAR_LIST;
                }
                if (Clazz.isDouble(aType)) {
                    return AbstractTypeDefinition.DOUBLE_LIST;
                }
                if (Clazz.isEnum(aType)) {
                    return AbstractTypeDefinition.ENUM_LIST;
                }
                if (Clazz.isFloat(aType)) {
                    return AbstractTypeDefinition.FLOAT_LIST;
                }
                if (Clazz.isInteger(aType)) {
                    return AbstractTypeDefinition.INT_LIST;
                }
                if (Clazz.isLong(aType)) {
                    return AbstractTypeDefinition.LONG_LIST;
                }
                if (Clazz.isShort(aType)) {
                    return AbstractTypeDefinition.SHORT_LIST;
                }
                if (Clazz.isString(aType)) {
                    if (this.getAnnotation(HexColorString.class) != null) {
                        return AbstractTypeDefinition.HEX_COLOR_LIST;
                    }
                    return AbstractTypeDefinition.STRING_LIST;
                }
                return AbstractTypeDefinition.OBJECT_LIST;
            }
            // if(ret instanceof List){
            // return AbstractType.OBJECT_LIST;
            // }
        } else {
            if (ret instanceof ParameterizedType) {
                final Type raw = ((ParameterizedType) ret).getRawType();
                final Type[] acutal = ((ParameterizedType) ret).getActualTypeArguments();
                if (raw instanceof Class) {
                    final Class<?> rawClazz = (Class<?>) raw;
                    if (List.class.isAssignableFrom(rawClazz)) {
                        if (Clazz.isBoolean(acutal[0])) {
                            return AbstractTypeDefinition.BOOLEAN_LIST;
                        }
                        if (Clazz.isByte(acutal[0])) {
                            return AbstractTypeDefinition.BYTE_LIST;
                        }
                        if (Clazz.isCharacter(acutal[0])) {
                            return AbstractTypeDefinition.CHAR_LIST;
                        }
                        if (Clazz.isDouble(acutal[0])) {
                            return AbstractTypeDefinition.DOUBLE_LIST;
                        }
                        if (Clazz.isEnum(acutal[0])) {
                            return AbstractTypeDefinition.ENUM_LIST;
                        }
                        if (Clazz.isFloat(acutal[0])) {
                            return AbstractTypeDefinition.FLOAT_LIST;
                        }
                        if (Clazz.isInteger(acutal[0])) {
                            return AbstractTypeDefinition.INT_LIST;
                        }
                        if (Clazz.isLong(acutal[0])) {
                            return AbstractTypeDefinition.LONG_LIST;
                        }
                        if (Clazz.isShort(acutal[0])) {
                            return AbstractTypeDefinition.SHORT_LIST;
                        }
                        if (Clazz.isString(acutal[0])) {
                            if (this.getAnnotation(HexColorString.class) != null) {
                                return AbstractTypeDefinition.HEX_COLOR_LIST;
                            }
                            return AbstractTypeDefinition.STRING_LIST;
                        }
                        return AbstractTypeDefinition.OBJECT_LIST;
                    }
                } else {
                    return AbstractTypeDefinition.UNKNOWN;
                }
            } else {
                return AbstractTypeDefinition.UNKNOWN;
            }
        }
        return AbstractTypeDefinition.OBJECT;
    }

    public String getTypeString() {
        final Type ret = getRawType();
        if (ret instanceof Class) {
            return ((Class<?>) ret).getName();
        } else {
            return ret.toString();
        }
    }

    public RawClass getValue() {
        synchronized (this) {
            RawClass value = this.getValueStorage();
            if (this.customValueGetter != null) {
                value = this.customValueGetter.getValue(this, value);
            }
            if (value == null && isDefaultOnNull()) {
                value = getDefaultValue();
            }
            return value;
        }
    }

    public RawClass getValueStorage() {
        final Storage storage = this.getStorageHandler().getPrimitiveStorage();
        if (storage.hasProperty(this.getKey())) {
            // primitiveSTorage contains a value. we do not need to calculate
            // the defaultvalue.
            return storage.get(this.getKey(), this.defaultValue);
        }
        // we have no value yet. call the getDefaultMethod to calculate the
        // default value
        if (this.backwardsCompatibilityLookupKeys != null) {
            for (final String key : this.backwardsCompatibilityLookupKeys) {
                if (storage.hasProperty(key)) {
                    final boolean apv = storage.isAutoPutValues();
                    try {
                        if (!apv) {
                            storage.setAutoPutValues(true);
                        }
                        return storage.get(this.getKey(), storage.get(key, this.defaultValue));
                    } finally {
                        if (!apv) {
                            storage.setAutoPutValues(apv);
                        }
                    }
                }
            }
        }
        return storage.get(this.getKey(), this.getDefaultValue(), isAllowWriteDefaultObjects());
    }

    private boolean allowWriteDefaultObjects = true;

    public boolean isAllowWriteDefaultObjects() {
        return allowWriteDefaultObjects;
    }

    public void setAllowWriteDefaultObjects(boolean allowWriteDefaultObjects) {
        this.allowWriteDefaultObjects = allowWriteDefaultObjects;
    }

    public synchronized boolean hasEventListener() {
        return this.eventSender != null && this.eventSender.hasListener();
    }

    /**
     * @throws Throwable
     *
     */
    @SuppressWarnings("unchecked")
    protected void init() throws Throwable {
        if (getMethod == null) {
            throw new InterfaceParseException("Getter Method is Missing for " + setMethod);
        }
        // read local cryptinfos
        this.primitive = JSonStorage.canStorePrimitive(getMethod.getReturnType());
        final CryptedStorage cryptedStorage = this.getAnnotation(CryptedStorage.class);
        if (cryptedStorage != null) {
            if (this.storageHandler.getPrimitiveStorage().getCryptKey() != null) {
                //
                throw new InterfaceParseException("No reason to mark " + this + " as @CryptedStorage. Parent is already CryptedStorage");
            } else if (!(this instanceof ListHandler)) {
                //
                throw new InterfaceParseException(this + " Cannot set @CryptedStorage on primitive fields. Use an object, or an extra plain config interface");
            }
            this.validateEncryptionKey(cryptedStorage.key());
        }
        final PlainStorage plainStorage = this.getAnnotation(PlainStorage.class);
        if (plainStorage != null) {
            if (cryptedStorage != null) {
                //
                throw new InterfaceParseException("Cannot Set CryptStorage and PlainStorage Annotation in " + this);
            }
            if (this.storageHandler.getPrimitiveStorage().getCryptKey() == null) {
                //
                throw new InterfaceParseException("No reason to mark " + this + " as @PlainStorage. Parent is already Plain");
            } else if (!(this instanceof ListHandler)) {
                //
                throw new InterfaceParseException(this + " Cannot set @PlainStorage on primitive fields. Use an object, or an extra plain config interface");
                // primitive storage. cannot set single plain values in a en
                // crypted
                // primitive storage
            }
            // parent crypted, but plain for this single entry
        }
        try {
            final ValidatorFactory anno = this.getAnnotation(ValidatorFactory.class);
            if (anno != null) {
                this.validatorFactory = (AbstractValidator<RawClass>) anno.value().newInstance();
            }
        } catch (final Throwable e) {
        }
        try {
            final CustomValueGetter anno = this.getAnnotation(CustomValueGetter.class);
            if (anno != null) {
                this.customValueGetter = (AbstractCustomValueGetter<RawClass>) anno.value().newInstance();
            }
        } catch (final Throwable e) {
        }
        try {
            this.defaultOnNull = this.getAnnotation(DefaultOnNull.class) != null;
        } catch (final Throwable e) {
        }
        this.checkBadAnnotations(this.getAllowedAnnotations());
        this.initDefaults();
        this.initHandler();
        final String kk = "CFG:" + this.storageHandler.getConfigInterface().getName() + "." + this.key;
        final String sys = System.getProperty(kk);
        if (sys != null) {
            // Set configvalud because of JVM Parameter
            System.out.println(kk + "=" + sys);
            this.setValue((RawClass) JSonStorage.restoreFromString(sys, new TypeRef<Object>(this.getRawClass()) {
            }, null));
        }
        final LookUpKeys lookups = this.getAnnotation(LookUpKeys.class);
        if (lookups != null) {
            this.backwardsCompatibilityLookupKeys = lookups.value();
        }
    }

    protected boolean isDelayedWriteAllowed() {
        return true;
    }

    protected void initDefaults() throws Throwable {
        this.defaultValue = null;
    }

    /**
     * @throws Throwable
     *
     */
    protected abstract void initHandler() throws Throwable;

    /**
     * returns true of this keyhandler belongs to ConfigInterface
     *
     * @param settings
     * @return
     */
    public boolean isChildOf(final ConfigInterface settings) {
        return settings._getStorageHandler() == this.getStorageHandler();
    }

    /**
     * @param m
     * @return
     */
    protected boolean isGetter(final Method m) {
        return m != null && m.equals(getMethod);
    }

    protected boolean isPrimitive() {
        return this.primitive;
    }

    /**
     * @param object
     */
    protected abstract void putValue(RawClass object);

    public void setDefaultValue(final RawClass c) {
        this.defaultValue = c;
    }

    /**
     * @param h
     */
    protected void setGetMethod(final Method method) {
        this.getMethod = method;
    }

    /**
     * @param h
     */
    protected void setSetMethod(final Method method) {
        this.setMethod = method;
    }

    /**
     * this method compare newValue and getValue and returns false if newValue is different from getValue
     *
     * @param newValue
     * @return
     */
    protected boolean setValueEqualsGetValue(final RawClass newValue) {
        final RawClass oldValue = this.getValue();
        return equals(newValue, oldValue);
    }

    /**
     * @param newValue
     */
    public void setValue(final RawClass newValue) throws ValidationException {
        try {
            synchronized (this) {
                if (setValueEqualsGetValue(newValue)) {
                    return;
                } else {
                    if (this.validatorFactory != null) {
                        this.validatorFactory.validate(newValue);
                    }
                    this.validateValue(newValue);
                    this.putValue(newValue);
                    getStorageHandler().requestSave();
                }
            }
            this.fireEvent(ConfigEvent.Types.VALUE_UPDATED, this, newValue);
        } catch (final ValidationException e) {
            e.setValue(newValue);
            this.fireEvent(ConfigEvent.Types.VALIDATOR_ERROR, this, e);
            throw e;
        } catch (final Throwable t) {
            final ValidationException e = new ValidationException(t);
            e.setValue(newValue);
            this.fireEvent(ConfigEvent.Types.VALIDATOR_ERROR, this, e);
            throw e;
        }
    }

    private static final Set<Class<?>> WRAPPER_TYPES = getWrapperTypes();

    public static boolean isWrapperType(final Class<?> clazz) {
        return WRAPPER_TYPES.contains(clazz);
    }

    private static Set<Class<?>> getWrapperTypes() {
        final Set<Class<?>> ret = new HashSet<Class<?>>();
        ret.add(Boolean.class);
        ret.add(Character.class);
        ret.add(Byte.class);
        ret.add(Short.class);
        ret.add(Integer.class);
        ret.add(Long.class);
        ret.add(Float.class);
        ret.add(Double.class);
        ret.add(Void.class);
        ret.add(String.class);
        return ret;
    }

    protected boolean equals(Object x, Object y) {
        try {
            if (x == null && y == null) {
                return true;
            } else if (x != null && y != null) {
                final Class<?> xC = x.getClass();
                final Class<?> yC = y.getClass();
                if (xC.isPrimitive() && yC.isPrimitive()) {
                    // primitives are safe to x.equals(y)
                    return x.equals(y);
                }
                if (isWrapperType(xC) && isWrapperType(yC)) {
                    // wrappers are safe to x.equals(y)
                    return x.equals(y);
                }
                if (xC.isEnum() && yC.isEnum()) {
                    // enums are safe to x.equals(y)
                    return x.equals(y);
                }
                final boolean xCList = List.class.isAssignableFrom(xC);
                final boolean yCList = List.class.isAssignableFrom(yC);
                if (xCList && yCList) {
                    final List<?> xL = (List) x;
                    final List<?> yL = (List) y;
                    final int xLL = xL.size();
                    final int yLL = yL.size();
                    if (xLL == yLL) {
                        for (int index = 0; index < xLL; index++) {
                            final Object xE = xL.get(index);
                            final Object yE = yL.get(index);
                            if (equals(xE, yE) == false) {
                                return false;
                            }
                        }
                        return true;
                    } else {
                        return false;
                    }
                }
                final boolean xCArray = xC.isArray();
                final boolean yCArray = yC.isArray();
                if (xCArray && yCArray) {
                    final int xL = Array.getLength(x);
                    final int yL = Array.getLength(y);
                    if (xL == yL) {
                        for (int index = 0; index < xL; index++) {
                            final Object xE = Array.get(x, index);
                            final Object yE = Array.get(y, index);
                            if (equals(xE, yE) == false) {
                                return false;
                            }
                        }
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public String toString() {
        final RawClass ret = this.getValue();
        return ret == null ? null : ret.toString();
    }

    public void validateEncryptionKey(final byte[] key2) {
        if (key2 == null) {
            throw new InterfaceParseException("Key missing in " + this);
        }
        if (key2.length != JSonStorage.KEY.length) {
            throw new InterfaceParseException("Crypt key for " + this + " is invalid. required length: " + JSonStorage.KEY.length);
        }
    }

    /**
     * @param object
     */
    protected abstract void validateValue(RawClass object) throws Throwable;

    /**
     * @return
     */
    public String getReadableName() {
        String getterName = getGetMethod().getName();
        if (getterName.startsWith("is")) {
            getterName = getterName.substring(2);
        } else if (getterName.startsWith("get")) {
            getterName = getterName.substring(3);
        }
        getterName = getterName.replaceAll("([a-z])([A-Z])", "$1 $2");
        getterName = getterName.replaceAll("(\\D)(\\d+)", "$1 $2");
        getterName = getterName.replaceAll("(\\d+)(\\D)", "$1 $2");
        getterName = getterName.replaceAll("(\\S)([A-Z][a-z])", "$1 $2");
        getterName = getterName.replaceAll("(1080|720|480|540|360|240) ?(p|P)", "$1$2");
        getterName = getterName.replaceAll("(^| )(2|4|8) ?(k|K)($| )", "$1$2$3$4");
        if (getterName.endsWith(" Enabled")) {
            getterName = getterName.substring(0, getterName.length() - 8);
        }
        return getterName;
    }
}
