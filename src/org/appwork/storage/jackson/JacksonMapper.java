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
package org.appwork.storage.jackson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;

import org.appwork.storage.JSONMapper;
import org.appwork.storage.JSonMapperException;
import org.appwork.storage.JsonSerializer;
import org.appwork.storage.TypeRef;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @author thomas
 *
 */
public class JacksonMapper implements JSONMapper {
    private final ObjectMapper mapper;

    public JacksonMapper() {
        mapper = new ObjectMapper(new ExtJsonFactory());
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);// needed as MyJDownloader Clients may use regex and fail because of
        // changed ident
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * @param <T>
     * @param class1
     * @param jsonSerializer
     */
    public <T> void addSerializer(final Class<T> clazz, final JsonSerializer<T> jsonSerializer) {
        final SimpleModule mod = new SimpleModule("MyModule", new Version(1, 0, 0, null));
        mod.addSerializer(clazz, new com.fasterxml.jackson.databind.JsonSerializer<T>() {
            @Override
            public void serialize(final T arg0, final JsonGenerator jgen, final SerializerProvider arg2) throws IOException, JsonProcessingException {
                jgen.writeRawValue(jsonSerializer.toJSonString(arg0));
            }
        });
        mapper.registerModule(mod);
    }

    @Override
    public String objectToString(final Object value) throws JSonMapperException {
        try {
            return mapper.writeValueAsString(value);
        } catch (final JsonProcessingException e) {
            throw new JSonMapperException(e);
        }
    }

    @Override
    public <T> T stringToObject(final String jsonString, final Class<T> clazz) throws JSonMapperException {
        try {
            return mapper.readValue(jsonString, clazz);
        } catch (final JsonProcessingException e) {
            throw new JSonMapperException(e);
        } catch (final IOException e) {
            throw new JSonMapperException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T stringToObject(final String jsonString, final TypeRef<T> type) throws JSonMapperException {
        try {
            final TypeReference<T> tr = new TypeReference<T>() {
                @Override
                public Type getType() {
                    return type.getType();
                }
            };
            // this (T) is required because of java bug
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6302954
            // (compiles in eclipse, but not with javac)
            return (T) mapper.readValue(jsonString, tr);
        } catch (final JsonProcessingException e) {
            throw new JSonMapperException(e);
        } catch (final IOException e) {
            throw new JSonMapperException(e);
        }
    }

    @Override
    public <T> T convert(Object jsonString, final TypeRef<T> type) throws JSonMapperException {
        final TypeReference<T> tr = new TypeReference<T>() {
            @Override
            public Type getType() {
                return type.getType();
            }
        };
        return mapper.convertValue(jsonString, tr);
    }

    @Override
    public byte[] objectToByteArray(final Object value) throws JSonMapperException {
        try {
            return mapper.writeValueAsBytes(value);
        } catch (final JsonProcessingException e) {
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
    @SuppressWarnings("unchecked")
    @Override
    public <T> T inputStreamToObject(final InputStream inputStream, final TypeRef<T> type) throws JSonMapperException {
        try {
            try {
                final TypeReference<T> tr = new TypeReference<T>() {
                    @Override
                    public Type getType() {
                        return type.getType();
                    }
                };
                // this (T) is required because of java bug
                // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6302954
                // (compiles in eclipse, but not with javac)
                return (T) mapper.readValue(inputStream, tr);
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        } catch (final JsonProcessingException e) {
            throw new JSonMapperException(e);
        } catch (final IOException e) {
            throw new JSonMapperException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T byteArrayToObject(final byte[] byteArray, final TypeRef<T> type) throws JSonMapperException {
        try {
            final TypeReference<T> tr = new TypeReference<T>() {
                @Override
                public Type getType() {
                    return type.getType();
                }
            };
            // this (T) is required because of java bug
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6302954
            // (compiles in eclipse, but not with javac)
            return (T) mapper.readValue(byteArray, tr);
        } catch (final JsonProcessingException e) {
            throw new JSonMapperException(e);
        } catch (final IOException e) {
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
                mapper.writeValue(outputStream, value);
            } finally {
                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (final JsonProcessingException e) {
            throw new JSonMapperException(e);
        } catch (final IOException e) {
            throw new JSonMapperException(e);
        }
    }

    @Override
    public <T> T byteArrayToObject(byte[] byteArray, Class<T> clazz) throws JSonMapperException {
        try {
            return mapper.readValue(byteArray, clazz);
        } catch (final JsonProcessingException e) {
            throw new JSonMapperException(e);
        } catch (final IOException e) {
            throw new JSonMapperException(e);
        }
    }

    /**
     * closes inputStream
     */
    @Override
    public <T> T inputStreamToObject(InputStream inputStream, Class<T> clazz) throws JSonMapperException {
        try {
            try {
                return mapper.readValue(inputStream, clazz);
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        } catch (final JsonProcessingException e) {
            throw new JSonMapperException(e);
        } catch (final IOException e) {
            throw new JSonMapperException(e);
        }
    }
}
