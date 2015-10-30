/**
 * 
 * ====================================================================================================================================================
 * 	    "AppWork Utilities" License
 * 	    The "AppWork Utilities" will be called [The Product] from now on.
 * ====================================================================================================================================================
 * 	    Copyright (c) 2009-2015, AppWork GmbH <e-mail@appwork.org>
 * 	    Schwabacher Straße 117
 * 	    90763 Fürth
 * 	    Germany   
 * === Preamble ===
 * 	This license establishes the terms under which the [The Product] Source Code & Binary files may be used, copied, modified, distributed, and/or redistributed.
 * 	The intent is that the AppWork GmbH is able to provide  their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 * 	These terms apply to all files that have the [The Product] License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 * 	
 * === 3rd Party Licences ===
 * 	Some parts of the [The Product] use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 * 	to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header. 	
 * 	
 * === Definition: Commercial Usage ===
 * 	If anybody or any organization is generating income (directly or indirectly) by using [The Product] or if there's as much as a 
 * 	sniff of commercial interest or aspect in what you are doing, we consider this as a commercial usage. If you are unsure whether your use-case is commercial or not, consider it as commercial.
 * === Dual Licensing ===
 * === Commercial Usage ===
 * 	If you want to use [The Product] in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 * 	Contact AppWork for further details: e-mail@appwork.org
 * === Non-Commercial Usage ===
 * 	If there is no commercial usage (see definition above), you may use [The Product] under the terms of the 
 * 	"GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 * 	
 * 	If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.storage.jackson;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.appwork.storage.JSONMapper;
import org.appwork.storage.JSonMapperException;
import org.appwork.storage.JsonSerializer;
import org.appwork.storage.TypeRef;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.module.SimpleModule;
import org.codehaus.jackson.type.TypeReference;

/**
 * @author thomas
 * 
 */
public class JacksonMapper implements JSONMapper {

    private final ObjectMapper mapper;

    public JacksonMapper() {

        mapper = new ObjectMapper(new ExtJsonFactory());

        mapper.getDeserializationConfig().set(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    }

    private List<JsonSerializer> serializer = new ArrayList<JsonSerializer>();

    /**
     * @param <T>
     * @param class1
     * @param jsonSerializer
     */
    public <T> void addSerializer(final Class<T> clazz, final JsonSerializer<T> jsonSerializer) {
        final SimpleModule mod = new SimpleModule("MyModule", new Version(1, 0, 0, null));
        mod.addSerializer(clazz, new org.codehaus.jackson.map.JsonSerializer<T>() {

            @Override
            public void serialize(final T arg0, final JsonGenerator jgen, final SerializerProvider arg2) throws IOException, JsonProcessingException {
                jgen.writeRawValue(jsonSerializer.toJSonString(arg0));
            }
        }

        );

        mapper.registerModule(mod);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.storage.JSONMapper#objectToString(java.lang.Object)
     */
    @Override
    public String objectToString(final Object o) throws JSonMapperException {
        try {

            return mapper.writeValueAsString(o);
        } catch (final JsonGenerationException e) {
            throw new JSonMapperException(e);
        } catch (final JsonMappingException e) {
            throw new JSonMapperException(e);
        } catch (final IOException e) {
            throw new JSonMapperException(e);
        }
    }

    @Override
    public <T> T stringToObject(final String jsonString, final Class<T> clazz) throws JSonMapperException {
        try {
            return mapper.readValue(jsonString, clazz);
        } catch (final JsonParseException e) {
            throw new JSonMapperException(e);
        } catch (final JsonMappingException e) {
            throw new JSonMapperException(e);
        } catch (final IOException e) {
            throw new JSonMapperException(e);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.storage.JSONMapper#stringToObject(java.lang.String,
     * org.appwork.storage.TypeRef)
     */
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
        } catch (final JsonParseException e) {
            throw new JSonMapperException(e);
        } catch (final JsonMappingException e) {
            throw new JSonMapperException(e);
        } catch (final IOException e) {
            throw new JSonMapperException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.storage.JSONMapper#convert(java.lang.Object,
     * org.appwork.storage.TypeRef)
     */
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

}
