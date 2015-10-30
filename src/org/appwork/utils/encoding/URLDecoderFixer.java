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
package org.appwork.utils.encoding;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;



/**
 * @author daniel
 * 
 */
public class URLDecoderFixer extends URLDecoder {
    public static String decode(final String s, final String enc) throws UnsupportedEncodingException {

        boolean needToChange = false;
        final int numChars = s.length();
        final StringBuffer sb = new StringBuffer(numChars > 500 ? numChars / 2 : numChars);
        int i = 0;

        if (enc.length() == 0) { throw new UnsupportedEncodingException("URLDecoderFixer: empty string enc parameter"); }
        boolean exceptionFixed = false;
        char c;
        byte[] bytes = null;
        while (i < numChars) {
            c = s.charAt(i);
            switch (c) {
            case '+':
                sb.append(' ');
                i++;
                needToChange = true;
                break;
            case '%':
                /*
                 * Starting with this instance of %, process all consecutive
                 * substrings of the form %xy. Each substring %xy will yield a
                 * byte. Convert all consecutive bytes obtained this way to
                 * whatever character(s) they represent in the provided
                 * encoding.
                 */
                final int iBackup = i;
                try {
                    try {

                        // (numChars-i)/3 is an upper bound for the number
                        // of remaining bytes
                        if (bytes == null) {
                            bytes = new byte[(numChars - i) / 3];
                        }
                        int pos = 0;

                        while (i + 2 < numChars && c == '%') {
                            final int v = Integer.parseInt(s.substring(i + 1, i + 3), 16);
                            if (v < 0) { throw new IllegalArgumentException("URLDecoderFixer: Illegal hex characters in escape (%) pattern - negative value"); }
                            bytes[pos++] = (byte) v;
                            i += 3;
                            if (i < numChars) {
                                c = s.charAt(i);
                            }
                        }

                        // A trailing, incomplete byte encoding such as
                        // "%x" will cause an exception to be thrown

                        if (i < numChars && c == '%') { throw new IllegalArgumentException("URLDecoderFixer: Incomplete trailing escape (%) pattern"); }

                        sb.append(new String(bytes, 0, pos, enc));
                    } catch (final NumberFormatException e) {
                        throw new IllegalArgumentException("URLDecoderFixer: Illegal hex characters in escape (%) pattern - " + e.getMessage());
                    }
                } catch (final IllegalArgumentException e) {
                    exceptionFixed = true;
                    i = iBackup;
                    sb.append(c);
                    i++;
                }
                needToChange = true;
                break;
            default:
                sb.append(c);
                i++;
                break;
            }
        }
        if (exceptionFixed) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(new Exception("URLDecoderFixer: had to fix " + s));
        }
        return needToChange ? sb.toString() : s;
    }
}
