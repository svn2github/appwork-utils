/**
 * Copyright (c) 2009 - 2011 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.storage.simplejson.mapper
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
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
