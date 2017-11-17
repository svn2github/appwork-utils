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
 *     The intent is that the AppWork GmbH is able to provide  their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 *     These terms apply to all files that have the [The Product] License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 *
 * === 3rd Party Licences ===
 *     Some parts of the [The Product] use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 *     to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header.
 *
 * === Definition: Commercial Usage ===
 *     If anybody or any organization is generating income (directly or indirectly) by using [The Product] or if there's any commercial interest or aspect in what you are doing, we consider this as a commercial usage.
 *     If your use-case is neither strictly private nor strictly educational, it is commercial. If you are unsure whether your use-case is commercial or not, consider it as commercial or contact as.
 * === Dual Licensing ===
 * === Commercial Usage ===
 *     If you want to use [The Product] in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 *     Contact AppWork for further details: e-mail@appwork.org
 * === Non-Commercial Usage ===
 *     If there is no commercial usage (see definition above), you may use [The Product] under the terms of the
 *     "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 *
 *     If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.utils.crypto;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

/**
 * @author Thomas
 * @date 13.11.2017
 *
 */
public class UUIDSignVerificator {
    /**
     * Verify a signature. Contains the PUBLIC KEY
     */
    public static boolean verify(String uuid, String signature) {
        try {
            // from hex format
            final int len = signature.length();
            final byte[] signatureBytes = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                signatureBytes[i / 2] = (byte) ((Character.digit(signature.charAt(i), 16) << 4) + Character.digit(signature.charAt(i + 1), 16));
            }
            byte[] bytes = uuid.getBytes("UTF-8");
            PublicKey pub = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(new byte[] { (byte) 48, (byte) -126, (byte) 1, (byte) 34, (byte) 48, (byte) 13, (byte) 6, (byte) 9, (byte) 42, (byte) -122, (byte) 72, (byte) -122, (byte) -9, (byte) 13, (byte) 1, (byte) 1, (byte) 1, (byte) 5, (byte) 0, (byte) 3, (byte) -126, (byte) 1, (byte) 15, (byte) 0, (byte) 48, (byte) -126, (byte) 1, (byte) 10, (byte) 2, (byte) -126, (byte) 1, (byte) 1, (byte) 0, (byte) -122, (byte) 50, (byte) 111, (byte) -24, (byte) 51, (byte) -32, (byte) -22, (byte) 48, (byte) 94, (byte) -28, (byte) 78, (byte) 26, (byte) -67, (byte) -35, (byte) 6, (byte) -118, (byte) -87, (byte) 37, (byte) -36, (byte) 81, (byte) -36, (byte) -100, (byte) -86, (byte) -122, (byte) -28, (byte) -98, (byte) 48, (byte) 42, (byte) 18, (byte) 75, (byte) 119, (byte) -76, (byte) -82, (byte) 81, (byte) -41, (byte) 94, (byte) 32, (byte) 48, (byte) 94, (byte) 94,
                            (byte) 121, (byte) -87, (byte) -79, (byte) -70, (byte) -105, (byte) -12, (byte) -39, (byte) 53, (byte) 21, (byte) 75, (byte) -29, (byte) -119, (byte) -16, (byte) 126, (byte) -109, (byte) 112, (byte) -117, (byte) 46, (byte) -88, (byte) -87, (byte) 124, (byte) -19, (byte) -54, (byte) 46, (byte) 69, (byte) -126, (byte) 114, (byte) 73, (byte) 49, (byte) 11, (byte) 63, (byte) 118, (byte) -27, (byte) 3, (byte) 105, (byte) -62, (byte) 84, (byte) 113, (byte) 110, (byte) 88, (byte) -92, (byte) 64, (byte) -95, (byte) 23, (byte) -55, (byte) 101, (byte) 110, (byte) -37, (byte) 18, (byte) 101, (byte) 102, (byte) -104, (byte) 18, (byte) -116, (byte) 62, (byte) 105, (byte) -97, (byte) -66, (byte) -38, (byte) 22, (byte) -21, (byte) 106, (byte) 31, (byte) -105, (byte) -49, (byte) -94, (byte) -23, (byte) -120, (byte) 73, (byte) -40, (byte) 59, (byte) 24, (byte) -120,
                            (byte) -38, (byte) -127, (byte) -101, (byte) -71, (byte) -117, (byte) -44, (byte) 15, (byte) 42, (byte) -85, (byte) -116, (byte) 25, (byte) -17, (byte) -91, (byte) 97, (byte) 61, (byte) -22, (byte) 100, (byte) 9, (byte) 22, (byte) 26, (byte) -52, (byte) -73, (byte) 104, (byte) 10, (byte) -33, (byte) 54, (byte) 13, (byte) -96, (byte) 84, (byte) 93, (byte) -26, (byte) 29, (byte) -106, (byte) 67, (byte) -36, (byte) 51, (byte) -19, (byte) -118, (byte) -34, (byte) 27, (byte) -98, (byte) 53, (byte) -125, (byte) 70, (byte) -114, (byte) 124, (byte) -108, (byte) 2, (byte) 110, (byte) 70, (byte) -92, (byte) 66, (byte) -86, (byte) 88, (byte) -26, (byte) -72, (byte) -69, (byte) -112, (byte) 106, (byte) -17, (byte) 21, (byte) 100, (byte) 97, (byte) -126, (byte) -21, (byte) -80, (byte) -26, (byte) -80, (byte) -32, (byte) 127, (byte) 32, (byte) -33, (byte) -35, (byte) -99,
                            (byte) -82, (byte) 15, (byte) -69, (byte) 81, (byte) -8, (byte) -113, (byte) -67, (byte) -90, (byte) 110, (byte) 38, (byte) -17, (byte) -114, (byte) 100, (byte) -102, (byte) -46, (byte) 51, (byte) -39, (byte) 60, (byte) -62, (byte) -66, (byte) 103, (byte) 89, (byte) 37, (byte) 24, (byte) 57, (byte) 89, (byte) -110, (byte) 112, (byte) 38, (byte) -99, (byte) 2, (byte) -107, (byte) 75, (byte) -128, (byte) -45, (byte) 65, (byte) -5, (byte) 2, (byte) 2, (byte) -69, (byte) -28, (byte) -33, (byte) 73, (byte) 71, (byte) 30, (byte) -71, (byte) -43, (byte) 24, (byte) -4, (byte) 50, (byte) 84, (byte) -67, (byte) 25, (byte) 127, (byte) -26, (byte) -102, (byte) -98, (byte) -109, (byte) -105, (byte) 110, (byte) 9, (byte) 90, (byte) 27, (byte) -41, (byte) -41, (byte) -47, (byte) 124, (byte) -54, (byte) 95, (byte) 2, (byte) 3, (byte) 1, (byte) 0, (byte) 1 }));
            final Signature sig = Signature.getInstance("Sha256WithRSA");
            sig.initVerify(pub);
            sig.update(bytes);
            return sig.verify(signatureBytes);
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }
}
