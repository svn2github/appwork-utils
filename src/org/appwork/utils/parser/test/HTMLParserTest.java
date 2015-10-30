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
package org.appwork.utils.parser.test;

import org.appwork.utils.parser.HTMLParser;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author thomas
 * 
 */
public class HTMLParserTest {
    public static class TestEntry {

        private String[] finds = null;
        private final String string;
        private final int urlsCount;

        /**
         * @param i
         * @param string
         */
        public TestEntry(final int i, final String string) {
            this(i, string, (String) null);
        }

        /**
         * @param i
         * @param string2
         * @param b
         */
        public TestEntry(final int i, final String string, final String... finds) {
            this.urlsCount = i;
            this.string = string;
            this.finds = finds;
        }

        public TestEntry(final String string, final String... finds) {
            this(finds.length, string, finds);
        }

        /**
         * @return the finds
         */
        public String[] getFinds() {
            return this.finds;
        }

        /**
         * @return the string
         */
        public String getString() {
            return this.string;
        }

        /**
         * @return the urlsCount
         */
        public int getUrlsCount() {
            return this.urlsCount;
        }

    }

    @Test
    public void test() {
        final TestEntry[] testStrings = new TestEntry[] {
        /*
         * ends with space
         */
        new TestEntry("http://www.rapidshare.com/files/410828702/jetty-distribution-8.0.0.M0.zip ", "http://www.rapidshare.com/files/410828702/jetty-distribution-8.0.0.M0.zip"),
        /*
         * Starts with space
         */
        new TestEntry(" http://www.rapidshare.com/files/410828702/jetty-distribution-8.0.0.M0.zip ", "http://www.rapidshare.com/files/410828702/jetty-distribution-8.0.0.M0.zip"),
        /*
         * starts end ends with space
         */
        new TestEntry(" http://www.rapidshare.com/files/410828702/jetty-distribution-8.0.0.M0.zip ", "http://www.rapidshare.com/files/410828702/jetty-distribution-8.0.0.M0.zip"),
        /*
         * Space included
         */
        new TestEntry("http://www.rapidshare.com/files/410828702/jetty-dis tribution-8.0.0.M0.zip", "http://www.rapidshare.com/files/410828702/jetty-dis"),
        /*
         * 
         */
        new TestEntry("http://www.rapidshare.com/files/410828702/jetty-dis%20tribution-8.0.0.M0.zip", "http://www.rapidshare.com/files/410828702/jetty-dis%20tribution-8.0.0.M0.zip"),
        /*
         * 
         */
        new TestEntry("http://www.google.comkkkk&url1=www://yahoo.com:1182/s/Homelll", "http://www.google.comkkkk&url1=www://yahoo.com:1182/s/Homelll"),
        /*
         * multiple and ftp
         */
        new TestEntry(3, "http://www.google.de http://google.de ftp://bla www.google.de google.de"),
        /*
         * auth
         */
        new TestEntry("http://user@www.google.de http://user:pass@google.de ", "http://user@www.google.de", "http://user:pass@google.de"),

        /*
         * port
         */
        new TestEntry("http://www.google.de:999/dds.html", "http://www.google.de:999/dds.html"),

        /*
         * ankor and parameters
         */
        new TestEntry("http://www.google.de:999/dds.html#a \r\nhttp://www.google.de:999/dds.html?abc=3&jd=6#b", "http://www.google.de:999/dds.html#a", "http://www.google.de:999/dds.html?abc=3&jd=6#b"),

        };

        for (final TestEntry e : testStrings) {
            final java.util.List<String> found = HTMLParser.findUrls(e.getString());
            Assert.assertTrue(found.size() == e.getUrlsCount());
            if (e.getFinds() != null) {
                for (int i = 0; i < e.getFinds().length; i++) {
                    final boolean equals = found.get(i).equalsIgnoreCase(e.getFinds()[i]);
                    Assert.assertTrue(equals);
                }
            }
        }

    }
}
