/**
 * 
 * ====================================================================================================================================================
 * "AppWork Utilities" License
 * ====================================================================================================================================================
 * Copyright (c) 2009-2015, AppWork GmbH <e-mail@appwork.org>
 * Schwabacher Straße 117
 * 90763 Fürth
 * Germany
 * 
 * === Preamble ===
 * This license establishes the terms under which the AppWork Utilities Source Code & Binary files may be used, copied, modified, distributed, and/or redistributed.
 * The intent is that the AppWork GmbH is able to provide  their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 * These terms apply to all files that have the "AppWork Utilities" License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 * 
 * === 3rd Party Licences ===
 * Some parts of the AppWork Utilities use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 * to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header.
 * 
 * === Definition: Commercial Usage ===
 * If anybody or any organization is generating income (directly or indirectly) by using "AppWork Utilities" or if there's as much as a
 * sniff of commercial interest or aspect in what you are doing, we consider this as a commercial usage. If you are unsure whether your use-case is commercial or not, consider it as commercial.
 * === Dual Licensing ===
 * === Commercial Usage ===
 * If you want to use AppWork Utilities in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 * Contact AppWork for further details: e-mail@appwork.org
 * === Non-Commercial Usage ===
 * If there is no commercial usage (see definition above), you may use AppWork Utilities under the terms of the
 * "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 * 
 * If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.storage;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;

import org.appwork.storage.simplejson.mapper.CompiledTypeRef;

/**
 * @author thomas
 *
 */
public abstract class TypeRef<T> {

    public static final TypeRef<String>                   STRING          = new TypeRef<String>() {
                                                                              public Type getType() {
                                                                                  return String.class;
                                                                              };
                                                                          };
    public static final TypeRef<byte[]>                   BYTE_ARRAY      = new TypeRef<byte[]>() {
                                                                              public Type getType() {
                                                                                  return byte[].class;
                                                                              };
                                                                          };
    public static final TypeRef<HashMap<String, Object>>  HASHMAP         = new TypeRef<HashMap<String, Object>>() {
                                                                          };
    public static final TypeRef<ArrayList<Object>>        LIST            = new TypeRef<ArrayList<Object>>() {
                                                                          };
    public static final TypeRef<HashMap<String, String>>  HASHMAP_STRING  = new TypeRef<HashMap<String, String>>() {
                                                                          };

    public static final TypeRef<HashMap<String, Integer>> HASHMAP_INTEGER = new TypeRef<HashMap<String, Integer>>() {
                                                                          };
    public static final TypeRef<Boolean>                  BOOLEAN         = new TypeRef<Boolean>() {
                                                                          };
    public static final TypeRef<String[]>                 STRING_ARRAY    = new TypeRef<String[]>() {
                                                                              public Type getType() {
                                                                                  return String[].class;
                                                                              };
                                                                          };
    public static final TypeRef<HashMap<String, Double>>  HASHMAP_DOUBLE  = new TypeRef<HashMap<String, Double>>() {
                                                                          };
    private final Type                                    type;

    public TypeRef() {
        final Type superClass = this.getClass().getGenericSuperclass();

        if (superClass instanceof Class) {
            throw new IllegalArgumentException("Wrong TypeRef Construct");
        }
        this.type = ((ParameterizedType) superClass).getActualTypeArguments()[0];

    }

    public TypeRef(final Type t) {

        this.type = t;

    }

    public Type getType() {
        return this.type;
    }

    /**
     * @return
     */
    public CompiledTypeRef compile() {
        final CompiledTypeRef ret = new CompiledTypeRef(getType());
        return ret;
    }

}
