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
package org.appwork.utils.reflection;

import java.lang.reflect.Type;

/**
 * @author thomas
 *
 */
public class Clazz {
    /**
     * @param class1
     * @return
     */
    public static String getPackage(final Class<?> clazz) {
        return clazz.getPackage().getName();
    }

    /**
     * returns true if type is a boolean. No Matter if primitive or it's object wrapper
     *
     * @param type
     * @return
     */
    public static boolean isBoolean(final Type type) {
        return type == Boolean.class || type == boolean.class;
    }

    /**
     * returns true if type is a byte. No Matter if primitive or it's object wrapper
     *
     * @param type
     * @return
     */
    public static boolean isByte(final Type type) {
        return type == Byte.class || type == byte.class;
    }

    /**
     * returns true if type is a char. No Matter if primitive or it's object wrapper
     *
     * @param type
     * @return
     */
    public static boolean isCharacter(final Type type) {
        return type == Character.class || type == char.class;
    }

    /**
     * returns true if type is a double. No Matter if primitive or it's object wrapper
     *
     * @param type
     * @return
     */
    public static boolean isDouble(final Type type) {
        return type == Double.class || type == double.class;
    }

    /**
     * returns true if type is a float. No Matter if primitive or it's object wrapper
     *
     * @param type
     * @return
     */
    public static boolean isFloat(final Type type) {
        return type == Float.class || type == float.class;
    }

    /**
     * returns true if type is a int. No Matter if primitive or it's object wrapper
     *
     * @param type
     * @return
     */
    public static boolean isInteger(final Type type) {
        return type == Integer.class || type == int.class;
    }

    /**
     * returns true if type is a long. No Matter if primitive or it's object wrapper
     *
     * @param type
     * @return
     */
    public static boolean isLong(final Type type) {
        return type == Long.class || type == long.class;
    }

    /**
     * returns true if type is a primitive or a priomitive object wrapper
     *
     * @param type
     * @return
     */
    public static boolean isPrimitive(final Type type) {
        if (type instanceof Class) {
            return ((Class<?>) type).isPrimitive() || Clazz.isPrimitiveWrapper(type);
        }
        return false;
    }

    /**
     * returns true if type os a primitive object wrapper
     *
     * @param type
     * @return
     */
    public static boolean isPrimitiveWrapper(final Type type) {
        return type == Boolean.class || type == Integer.class || type == Long.class || type == Byte.class || type == Short.class || type == Float.class || type == Double.class || type == Character.class || type == Void.class;
    }

    /**
     * returns true if type is a short. No Matter if primitive or it's object wrapper
     *
     * @param type
     * @return
     */
    public static boolean isShort(final Type type) {
        return type == Short.class || type == short.class;
    }

    /**
     * returns true if type is a void. No Matter if primitive or it's object wrapper
     *
     * @param type
     * @return
     */
    public static boolean isVoid(final Type type) {
        return type == Void.class || type == void.class;
    }

    /**
     * @param type
     * @return
     */
    public static boolean isString(final Type type) {
        return type == String.class;
    }

    /**
     * @param type
     * @return
     */
    public static boolean isEnum(final Type type) {
        return type instanceof Class && ((Class<?>) type).isEnum();
    }

    /**
     * @param genericReturnType
     * @return
     */
    public static boolean isByteArray(final Type genericReturnType) {
        // TODO Auto-generated method stub
        return genericReturnType == byte[].class;
    }

    /**
     * is a instanceof b
     * 
     * @param c
     * @param class1
     * @return
     */
    public static boolean isInstanceof(final Class<?> a, final Class<?> b) {
        final boolean ret = b.isAssignableFrom(a);
        return ret;
    }

    /**
     * @param enumClass
     * @return
     */
    public static boolean isArray(Type type) {
        return type instanceof Class && ((Class) type).isArray();
    }
}
