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
package org.appwork.storage.simplejson.test;

import org.appwork.storage.SimpleMapper;
import org.appwork.storage.Storable;
import org.appwork.storage.jackson.JacksonMapper;
import org.appwork.storage.simplejson.JSonFactory;
import org.appwork.storage.simplejson.JSonNode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * This tests compare Jackson parser with our own simpleparser code.
 *
 * @author thomas
 *
 */
public class Test {

    public static class TestObject implements Storable {
        public TestObject() {

        }

        private long UUID = -1;

        public long getUUID() {
            return UUID;
        }

        public void setUUID(final long uUID) {
            UUID = uUID;
        }
    }

    public static void main(final String[] args) throws Exception {
        final JacksonMapper m = new JacksonMapper();
        final SimpleMapper m2 = new SimpleMapper();
        final TestObject t = new TestObject();
        System.out.println(m.objectToString(t));
        System.out.println(m2.objectToString(t));
        // JSonFactory.DEBUG = true;
        Test.parseValid("{\"OBoolean\" :     false, \"pChar\" : 19, \"pDouble\" : 0.3, \"oByte\" : 68, \"pFloat\" : 0.4230000078678131, \"oFloat\" : 0.4122999906539917, \"oLong\" : 5435443543, \"pInt\" : 2435253, \"list\" : [1,2,3], \"pBoolean\" : false, \"oChar\" : 16, \"num\" : \"BLUMM\", \"oDouble\" : 0.52, \"oInt\" : 45343253, \"pLong\" : 4355543543, \"string\" : \"affe232\", \"intArray\" : [3,2,1], \"map\" : {\"3\" : {\"oBoolean\" : false, \"pChar\" : 19, \"pDouble\" : 0.3, \"oByte\" : 68, \"pFloat\" : 0.4230000078678131, \"oFloat\" : 0.4122999906539917, \"oLong\" : 5435443543, \"pInt\" : 2435253, \"list\" : [], \"pBoolean\" : false, \"oChar\" : 16, \"num\" : \"BLUMM\", \"oDouble\" : 0.52, \"oInt\" : 45343253, \"pLong\" : 4355543543, \"string\" : \"affe232\", \"intArray\" : [3,2,1], \"map\" : {}, \"objArray\" : [{\"oBoolean\" : true, \"pChar\" : 18, \"pDouble\" : 0.5, \"oByte\" : 36, \"pFloat\" : 0.4000000059604645, \"oFloat\" : 0.4000000059604645, \"oLong\" : 43543, \"pInt\" : 43253, \"list\" : [], \"pBoolean\" : true, \"oChar\" : 18, \"num\" : \"TEST\", \"oDouble\" : 0.5, \"oInt\" : 43253, \"pLong\" : 43543, \"string\" : \"affe\", \"intArray\" : [1,2], \"map\" : {}, \"objArray\" : null, \"pByte\" : 36, \"obj\" : null},{\"oBoolean\" : true, \"pChar\" : 18, \"pDouble\" : 0.5, \"oByte\" : 36, \"pFloat\" : 0.4000000059604645, \"oFloat\" : 0.4000000059604645, \"oLong\" : 43543, \"pInt\" : 43253, \"list\" : [], \"pBoolean\" : true, \"oChar\" : 18, \"num\" : \"TEST\", \"oDouble\" : 0.5, \"oInt\" : 43253, \"pLong\" : 43543, \"string\" : \"affe\", \"intArray\" : [1,2], \"map\" : {}, \"objArray\" : null, \"pByte\" : 36, \"obj\" : null},{\"oBoolean\" : true, \"pChar\" : 18, \"pDouble\" : 0.5, \"oByte\" : 36, \"pFloat\" : 0.4000000059604645, \"oFloat\" : 0.4000000059604645, \"oLong\" : 43543, \"pInt\" : 43253, \"list\" : [], \"pBoolean\" : true, \"oChar\" : 18, \"num\" : \"TEST\", \"oDouble\" : 0.5, \"oInt\" : 43253, \"pLong\" : 43543, \"string\" : \"affe\", \"intArray\" : [1,2], \"map\" : {}, \"objArray\" : null, \"pByte\" : 36, \"obj\" : null}], \"pByte\" : 20, \"obj\" : null}, \"2\" : {\"oBoolean\" : false, \"pChar\" : 19, \"pDouble\" : 0.3, \"oByte\" : 68, \"pFloat\" : 0.4230000078678131, \"oFloat\" : 0.4122999906539917, \"oLong\" : 5435443543, \"pInt\" : 2435253, \"list\" : [], \"pBoolean\" : false, \"oChar\" : 16, \"num\" : \"BLUMM\", \"oDouble\" : 0.52, \"oInt\" : 45343253, \"pLong\" : 4355543543, \"string\" : \"affe232\", \"intArray\" : [3,2,1], \"map\" : {}, \"objArray\" : [{\"oBoolean\" : true, \"pChar\" : 18, \"pDouble\" : 0.5, \"oByte\" : 36, \"pFloat\" : 0.4000000059604645, \"oFloat\" : 0.4000000059604645, \"oLong\" : 43543, \"pInt\" : 43253, \"list\" : [], \"pBoolean\" : true, \"oChar\" : 18, \"num\" : \"TEST\", \"oDouble\" : 0.5, \"oInt\" : 43253, \"pLong\" : 43543, \"string\" : \"affe\", \"intArray\" : [1,2], \"map\" : {}, \"objArray\" : null, \"pByte\" : 36, \"obj\" : null},{\"oBoolean\" : true, \"pChar\" : 18, \"pDouble\" : 0.5, \"oByte\" : 36, \"pFloat\" : 0.4000000059604645, \"oFloat\" : 0.4000000059604645, \"oLong\" : 43543, \"pInt\" : 43253, \"list\" : [], \"pBoolean\" : true, \"oChar\" : 18, \"num\" : \"TEST\", \"oDouble\" : 0.5, \"oInt\" : 43253, \"pLong\" : 43543, \"string\" : \"affe\", \"intArray\" : [1,2], \"map\" : {}, \"objArray\" : null, \"pByte\" : 36, \"obj\" : null},{\"oBoolean\" : true, \"pChar\" : 18, \"pDouble\" : 0.5, \"oByte\" : 36, \"pFloat\" : 0.4000000059604645, \"oFloat\" : 0.4000000059604645, \"oLong\" : 43543, \"pInt\" : 43253, \"list\" : [], \"pBoolean\" : true, \"oChar\" : 18, \"num\" : \"TEST\", \"oDouble\" : 0.5, \"oInt\" : 43253, \"pLong\" : 43543, \"string\" : \"affe\", \"intArray\" : [1,2], \"map\" : {}, \"objArray\" : null, \"pByte\" : 36, \"obj\" : null}], \"pByte\" : 20, \"obj\" : null}, \"5\" : {\"oBoolean\" : false, \"pChar\" : 19, \"pDouble\" : 0.3, \"oByte\" : 68, \"pFloat\" : 0.4230000078678131, \"oFloat\" : 0.4122999906539917, \"oLong\" : 5435443543, \"pInt\" : 2435253, \"list\" : [], \"pBoolean\" : false, \"oChar\" : 16, \"num\" : \"BLUMM\", \"oDouble\" : 0.52, \"oInt\" : 45343253, \"pLong\" : 4355543543, \"string\" : \"affe232\", \"intArray\" : [3,2,1], \"map\" : {}, \"objArray\" : [{\"oBoolean\" : true, \"pChar\" : 18, \"pDouble\" : 0.5, \"oByte\" : 36, \"pFloat\" : 0.4000000059604645, \"oFloat\" : 0.4000000059604645, \"oLong\" : 43543, \"pInt\" : 43253, \"list\" : [], \"pBoolean\" : true, \"oChar\" : 18, \"num\" : \"TEST\", \"oDouble\" : 0.5, \"oInt\" : 43253, \"pLong\" : 43543, \"string\" : \"affe\", \"intArray\" : [1,2], \"map\" : {}, \"objArray\" : null, \"pByte\" : 36, \"obj\" : null},{\"oBoolean\" : true, \"pChar\" : 18, \"pDouble\" : 0.5, \"oByte\" : 36, \"pFloat\" : 0.4000000059604645, \"oFloat\" : 0.4000000059604645, \"oLong\" : 43543, \"pInt\" : 43253, \"list\" : [], \"pBoolean\" : true, \"oChar\" : 18, \"num\" : \"TEST\", \"oDouble\" : 0.5, \"oInt\" : 43253, \"pLong\" : 43543, \"string\" : \"affe\", \"intArray\" : [1,2], \"map\" : {}, \"objArray\" : null, \"pByte\" : 36, \"obj\" : null},{\"oBoolean\" : true, \"pChar\" : 18, \"pDouble\" : 0.5, \"oByte\" : 36, \"pFloat\" : 0.4000000059604645, \"oFloat\" : 0.4000000059604645, \"oLong\" : 43543, \"pInt\" : 43253, \"list\" : [], \"pBoolean\" : true, \"oChar\" : 18, \"num\" : \"TEST\", \"oDouble\" : 0.5, \"oInt\" : 43253, \"pLong\" : 43543, \"string\" : \"affe\", \"intArray\" : [1,2], \"map\" : {}, \"objArray\" : null, \"pByte\" : 36, \"obj\" : null}], \"pByte\" : 20, \"obj\" : null}, \"4\" : {\"oBoolean\" : false, \"pChar\" : 19, \"pDouble\" : 0.3, \"oByte\" : 68, \"pFloat\" : 0.4230000078678131, \"oFloat\" : 0.4122999906539917, \"oLong\" : 5435443543, \"pInt\" : 2435253, \"list\" : [], \"pBoolean\" : false, \"oChar\" : 16, \"num\" : \"BLUMM\", \"oDouble\" : 0.52, \"oInt\" : 45343253, \"pLong\" : 4355543543, \"string\" : \"affe232\", \"intArray\" : [3,2,1], \"map\" : {}, \"objArray\" : [{\"oBoolean\" : true, \"pChar\" : 18, \"pDouble\" : 0.5, \"oByte\" : 36, \"pFloat\" : 0.4000000059604645, \"oFloat\" : 0.4000000059604645, \"oLong\" : 43543, \"pInt\" : 43253, \"list\" : [], \"pBoolean\" : true, \"oChar\" : 18, \"num\" : \"TEST\", \"oDouble\" : 0.5, \"oInt\" : 43253, \"pLong\" : 43543, \"string\" : \"affe\", \"intArray\" : [1,2], \"map\" : {}, \"objArray\" : null, \"pByte\" : 36, \"obj\" : null},{\"oBoolean\" : true, \"pChar\" : 18, \"pDouble\" : 0.5, \"oByte\" : 36, \"pFloat\" : 0.4000000059604645, \"oFloat\" : 0.4000000059604645, \"oLong\" : 43543, \"pInt\" : 43253, \"list\" : [], \"pBoolean\" : true, \"oChar\" : 18, \"num\" : \"TEST\", \"oDouble\" : 0.5, \"oInt\" : 43253, \"pLong\" : 43543, \"string\" : \"affe\", \"intArray\" : [1,2], \"map\" : {}, \"objArray\" : null, \"pByte\" : 36, \"obj\" : null},{\"oBoolean\" : true, \"pChar\" : 18, \"pDouble\" : 0.5, \"oByte\" : 36, \"pFloat\" : 0.4000000059604645, \"oFloat\" : 0.4000000059604645, \"oLong\" : 43543, \"pInt\" : 43253, \"list\" : [], \"pBoolean\" : true, \"oChar\" : 18, \"num\" : \"TEST\", \"oDouble\" : 0.5, \"oInt\" : 43253, \"pLong\" : 43543, \"string\" : \"affe\", \"intArray\" : [1,2], \"map\" : {}, \"objArray\" : null, \"pByte\" : 36, \"obj\" : null}], \"pByte\" : 20, \"obj\" : null}}, \"objArray\" : [{\"oBoolean\" : true, \"pChar\" : 18, \"pDouble\" : 0.5, \"oByte\" : 36, \"pFloat\" : 0.4000000059604645, \"oFloat\" : 0.4000000059604645, \"oLong\" : 43543, \"pInt\" : 43253, \"list\" : [], \"pBoolean\" : true, \"oChar\" : 18, \"num\" : \"TEST\", \"oDouble\" : 0.5, \"oInt\" : 43253, \"pLong\" : 43543, \"string\" : \"affe\", \"intArray\" : [1,2], \"map\" : {}, \"objArray\" : null, \"pByte\" : 36, \"obj\" : null},{\"oBoolean\" : true, \"pChar\" : 18, \"pDouble\" : 0.5, \"oByte\" : 36, \"pFloat\" : 0.4000000059604645, \"oFloat\" : 0.4000000059604645, \"oLong\" : 43543, \"pInt\" : 43253, \"list\" : [], \"pBoolean\" : true, \"oChar\" : 18, \"num\" : \"TEST\", \"oDouble\" : 0.5, \"oInt\" : 43253, \"pLong\" : 43543, \"string\" : \"affe\", \"intArray\" : [1,2], \"map\" : {}, \"objArray\" : null, \"pByte\" : 36, \"obj\" : null},{\"oBoolean\" : true, \"pChar\" : 18, \"pDouble\" : 0.5, \"oByte\" : 36, \"pFloat\" : 0.4000000059604645, \"oFloat\" : 0.4000000059604645, \"oLong\" : 43543, \"pInt\" : 43253, \"list\" : [], \"pBoolean\" : true, \"oChar\" : 18, \"num\" : \"TEST\", \"oDouble\" : 0.5, \"oInt\" : 43253, \"pLong\" : 43543, \"string\" : \"affe\", \"intArray\" : [1,2], \"map\" : {}, \"objArray\" : null, \"pByte\" : 36, \"obj\" : null}], \"pByte\" : 20, \"obj\" : null}");

        Test.parseValid("[1,2,\"23\",4,5,[true,false]]");
        Test.parseValid("\"Unicode pattern testblabla\\u003ebl\\r\\na\"");
        Test.parseValid("23.432e-4");
        Test.parseValid("[23.432e-4]");
        Test.parseValid("{\"defaultdownloadfolder\":\"C:\\\\Users\\\\thomas\\\\down\\rloads\"}");

        Test.parseValid("[\"Z:\\\\\"]");
        Test.parseValid("{}");
        Test.parseValid("{  }");
        // Test.parseValid("{\"oBoolean\" :     false,}");
        new JSonNode() {
        };

    }

    /**
     * @param string
     * @throws Exception
     */
    private static void parseValid(final String string) throws Exception {
        System.err.println("\r\n\r\n----------------------------------------------\r\nTEST: " + string);
        final ObjectMapper m = new ObjectMapper();
        // can either use mapper.readTree(JsonParser), or bind to JsonNode

        final JsonNode rootNode = m.readValue(string, JsonNode.class);
        final JSonNode json = new JSonFactory(string).parse();

        if (rootNode.toString().length() != json.toString().length()) {
            throw new Exception("Jackson mismatch");
        }
        System.err.println("SUCCESS " + json);

    }
}
