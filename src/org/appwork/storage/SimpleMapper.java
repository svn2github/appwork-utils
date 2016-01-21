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
package org.appwork.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.appwork.storage.simplejson.JSonFactory;
import org.appwork.storage.simplejson.JSonNode;
import org.appwork.storage.simplejson.ParserException;
import org.appwork.storage.simplejson.mapper.JSonMapper;
import org.appwork.storage.simplejson.mapper.MapperException;
import org.appwork.utils.IO;

/**
 * @author thomas
 *
 */
public class SimpleMapper implements JSONMapper {
    protected final JSonMapper mapper;

    public SimpleMapper() {
        mapper = new JSonMapper() {

            @Override
            public JSonNode create(final Object obj) throws MapperException {
                for (final JsonSerializerEntry se : serializer) {
                    if (obj != null && se.clazz.isAssignableFrom(obj.getClass())) {
                        return new JSonNode() {

                            @Override
                            public String toString() {
                                return se.serializer.toJSonString(obj);
                            }
                        };
                    }
                }
                return super.create(obj);
            }
        };
    }

    public JSonMapper getMapper() {
        return mapper;
    }

    class JsonSerializerEntry {
        /**
         * @param <T>
         * @param clazz2
         * @param jsonSerializer
         */
        public <T> JsonSerializerEntry(final Class<T> clazz2, final JsonSerializer<T> jsonSerializer) {
            clazz = clazz2;
            serializer = jsonSerializer;
        }

        final protected JsonSerializer serializer;
        final protected Class<?>       clazz;
    }

    private final List<JsonSerializerEntry> serializer = new CopyOnWriteArrayList<JsonSerializerEntry>();

    /**
     * @param jsonSerializer
     */
    public <T> void addSerializer(final Class<T> clazz, final JsonSerializer<T> jsonSerializer) {
        serializer.add(new JsonSerializerEntry(clazz, jsonSerializer));
    }

    @Override
    public String objectToString(final Object value) throws JSonMapperException {
        try {
            return mapper.create(value).toString();
        } catch (final MapperException e) {
            throw new JSonMapperException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T stringToObject(final String jsonString, final Class<T> clazz) throws JSonMapperException {
        try {
            return (T) mapper.jsonToObject(new JSonFactory(jsonString).parse(), clazz);
        } catch (final ParserException e) {
            throw new JSonMapperException(e);
        } catch (final MapperException e) {
            throw new JSonMapperException(e);
        }
    }

    @Override
    public <T> T stringToObject(final String jsonString, final TypeRef<T> type) throws JSonMapperException {
        try {
            return mapper.jsonToObject(new JSonFactory(jsonString).parse(), type);
        } catch (final ParserException e) {
            throw new JSonMapperException(e);
        } catch (final MapperException e) {
            throw new JSonMapperException(e);
        }
    }

    @Override
    public <T> T convert(Object object, TypeRef<T> type) throws JSonMapperException {

        try {
            return mapper.jsonToObject(mapper.create(object), type);
        } catch (MapperException e) {
            throw new JSonMapperException(e);
        }
    }

    @Override
    public byte[] objectToByteArray(Object value) throws JSonMapperException {
        final String ret = objectToString(value);
        try {
            if (ret == null) {
                return "null".getBytes("UTF-8");
            } else {
                return ret.getBytes("UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            throw new JSonMapperException(e);
        }
    }

    /**
     * closes outputStream
     */
    @Override
    public void writeObject(OutputStream outputStream, Object value) throws JSonMapperException {
        try {
            try {
                outputStream.write(objectToByteArray(value));
            } finally {
                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            throw new JSonMapperException(e);
        }
    }

    /**
     * closes inputStream
     *
     * @param inputStream
     * @param type
     * @return
     * @throws JSonMapperException
     */
    @Override
    public <T> T inputStreamToObject(InputStream inputStream, TypeRef<T> type) throws JSonMapperException {
        try {
            try {
                return byteArrayToObject(IO.readStream(-1, inputStream), type);
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        } catch (IOException e) {
            throw new JSonMapperException(e);
        }
    }

    @Override
    public <T> T byteArrayToObject(byte[] byteArray, TypeRef<T> type) throws JSonMapperException {
        try {
            return stringToObject(new String(byteArray, "UTF-8"), type);
        } catch (UnsupportedEncodingException e) {
            throw new JSonMapperException(e);
        }
    }

    @Override
    public <T> T byteArrayToObject(byte[] byteArray, Class<T> clazz) throws JSonMapperException {
        try {
            return stringToObject(new String(byteArray, "UTF-8"), clazz);
        } catch (UnsupportedEncodingException e) {
            throw new JSonMapperException(e);
        }
    }

    /**
     * closes inputStream
     *
     * @param inputStream
     * @param clazz
     * @return
     * @throws JSonMapperException
     */
    @Override
    public <T> T inputStreamToObject(InputStream inputStream, Class<T> clazz) throws JSonMapperException {
        try {
            try {
                return byteArrayToObject(IO.readStream(-1, inputStream), clazz);
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        } catch (IOException e) {
            throw new JSonMapperException(e);
        }
    }

}
