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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * @author Thomas
 * @date 13.11.2017
 *
 */
public class UUIDSignCreator___PRIVATE___ {
    public static void main(final String[] args) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IOException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeySpecException, SignatureException {
        createKeyPair();
        // WICHTIG
        // Die signUUID Funktion enthält den Private Key, und muss unter Verschluss gehalten werden. Sie darf NIEMALS den Weg ins Release
        // finden. Das darf nur die verifySignature
        String uuid = "00000000-0000-0000-0000-50E54933DD23";
        // signatur erstellen
        System.out.println("UUID: \t\t\t\t" + uuid);
        String signature = signUUID(uuid);
        System.out.println("Signature: \t\t\t" + signature);
        // prüfen ob die Signatur korrekt ist.
        System.out.println("Signature correct: \t\t" + UUIDSignVerificator.verify(uuid, signature));
        // Test ob eine ungültige UUID erkannt wird.
        System.out.println("Bad Signature incorrect: \t" + !UUIDSignVerificator.verify(uuid + "j", signature));
    }

    /**
     * Erstellt ein Neues keypair - zum Austausch der Zeilen in {@link #UUIDSignCreator___PRIVATE___()} und {@link #signUUID(String)}
     * 
     * @throws NoSuchAlgorithmException
     *
     */
    private static void createKeyPair() throws NoSuchAlgorithmException {
        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        final KeyPair keyPair = keyPairGenerator.genKeyPair();
        System.out.println("PublicKey pub = KeyFactory.getInstance(\"RSA\").generatePublic(new X509EncodedKeySpec(new byte[]" + Arrays.toString(keyPair.getPublic().getEncoded()).replace("[", "{(byte)").replaceAll("]", "}").replaceAll(", ", ", (byte)") + "));");
        System.out.println("PrivateKey priv = KeyFactory.getInstance(\"RSA\").generatePrivate(new PKCS8EncodedKeySpec(new byte[]" + Arrays.toString(keyPair.getPrivate().getEncoded()).replace("[", "{(byte)").replaceAll("]", "}").replaceAll(", ", ", (byte)") + "));");
    }

    /**
     * Create a Signature in Hex Format. CONTAINS THE PRIVATE KEY! KEEP SECRET! DO NOT RELEASE EVEN IN COMPILED CODE
     */
    public static String signUUID(String uuid) throws UnsupportedEncodingException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        byte[] bytes = uuid.getBytes("UTF-8");
        PrivateKey priv = KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(new byte[] { (byte) 48, (byte) -126, (byte) 4, (byte) -66, (byte) 2, (byte) 1, (byte) 0, (byte) 48, (byte) 13, (byte) 6, (byte) 9, (byte) 42, (byte) -122, (byte) 72, (byte) -122, (byte) -9, (byte) 13, (byte) 1, (byte) 1, (byte) 1, (byte) 5, (byte) 0, (byte) 4, (byte) -126, (byte) 4, (byte) -88, (byte) 48, (byte) -126, (byte) 4, (byte) -92, (byte) 2, (byte) 1, (byte) 0, (byte) 2, (byte) -126, (byte) 1, (byte) 1, (byte) 0, (byte) -122, (byte) 50, (byte) 111, (byte) -24, (byte) 51, (byte) -32, (byte) -22, (byte) 48, (byte) 94, (byte) -28, (byte) 78, (byte) 26, (byte) -67, (byte) -35, (byte) 6, (byte) -118, (byte) -87, (byte) 37, (byte) -36, (byte) 81, (byte) -36, (byte) -100, (byte) -86, (byte) -122, (byte) -28, (byte) -98, (byte) 48, (byte) 42, (byte) 18, (byte) 75, (byte) 119, (byte) -76, (byte) -82, (byte) 81, (byte) -41,
                        (byte) 94, (byte) 32, (byte) 48, (byte) 94, (byte) 94, (byte) 121, (byte) -87, (byte) -79, (byte) -70, (byte) -105, (byte) -12, (byte) -39, (byte) 53, (byte) 21, (byte) 75, (byte) -29, (byte) -119, (byte) -16, (byte) 126, (byte) -109, (byte) 112, (byte) -117, (byte) 46, (byte) -88, (byte) -87, (byte) 124, (byte) -19, (byte) -54, (byte) 46, (byte) 69, (byte) -126, (byte) 114, (byte) 73, (byte) 49, (byte) 11, (byte) 63, (byte) 118, (byte) -27, (byte) 3, (byte) 105, (byte) -62, (byte) 84, (byte) 113, (byte) 110, (byte) 88, (byte) -92, (byte) 64, (byte) -95, (byte) 23, (byte) -55, (byte) 101, (byte) 110, (byte) -37, (byte) 18, (byte) 101, (byte) 102, (byte) -104, (byte) 18, (byte) -116, (byte) 62, (byte) 105, (byte) -97, (byte) -66, (byte) -38, (byte) 22, (byte) -21, (byte) 106, (byte) 31, (byte) -105, (byte) -49, (byte) -94, (byte) -23, (byte) -120, (byte) 73,
                        (byte) -40, (byte) 59, (byte) 24, (byte) -120, (byte) -38, (byte) -127, (byte) -101, (byte) -71, (byte) -117, (byte) -44, (byte) 15, (byte) 42, (byte) -85, (byte) -116, (byte) 25, (byte) -17, (byte) -91, (byte) 97, (byte) 61, (byte) -22, (byte) 100, (byte) 9, (byte) 22, (byte) 26, (byte) -52, (byte) -73, (byte) 104, (byte) 10, (byte) -33, (byte) 54, (byte) 13, (byte) -96, (byte) 84, (byte) 93, (byte) -26, (byte) 29, (byte) -106, (byte) 67, (byte) -36, (byte) 51, (byte) -19, (byte) -118, (byte) -34, (byte) 27, (byte) -98, (byte) 53, (byte) -125, (byte) 70, (byte) -114, (byte) 124, (byte) -108, (byte) 2, (byte) 110, (byte) 70, (byte) -92, (byte) 66, (byte) -86, (byte) 88, (byte) -26, (byte) -72, (byte) -69, (byte) -112, (byte) 106, (byte) -17, (byte) 21, (byte) 100, (byte) 97, (byte) -126, (byte) -21, (byte) -80, (byte) -26, (byte) -80, (byte) -32, (byte) 127,
                        (byte) 32, (byte) -33, (byte) -35, (byte) -99, (byte) -82, (byte) 15, (byte) -69, (byte) 81, (byte) -8, (byte) -113, (byte) -67, (byte) -90, (byte) 110, (byte) 38, (byte) -17, (byte) -114, (byte) 100, (byte) -102, (byte) -46, (byte) 51, (byte) -39, (byte) 60, (byte) -62, (byte) -66, (byte) 103, (byte) 89, (byte) 37, (byte) 24, (byte) 57, (byte) 89, (byte) -110, (byte) 112, (byte) 38, (byte) -99, (byte) 2, (byte) -107, (byte) 75, (byte) -128, (byte) -45, (byte) 65, (byte) -5, (byte) 2, (byte) 2, (byte) -69, (byte) -28, (byte) -33, (byte) 73, (byte) 71, (byte) 30, (byte) -71, (byte) -43, (byte) 24, (byte) -4, (byte) 50, (byte) 84, (byte) -67, (byte) 25, (byte) 127, (byte) -26, (byte) -102, (byte) -98, (byte) -109, (byte) -105, (byte) 110, (byte) 9, (byte) 90, (byte) 27, (byte) -41, (byte) -41, (byte) -47, (byte) 124, (byte) -54, (byte) 95, (byte) 2, (byte) 3,
                        (byte) 1, (byte) 0, (byte) 1, (byte) 2, (byte) -126, (byte) 1, (byte) 0, (byte) 49, (byte) -96, (byte) 125, (byte) 89, (byte) -11, (byte) -75, (byte) 123, (byte) 101, (byte) -97, (byte) -15, (byte) -10, (byte) 32, (byte) 85, (byte) -114, (byte) 99, (byte) 88, (byte) -21, (byte) -20, (byte) -96, (byte) 105, (byte) 117, (byte) -104, (byte) 74, (byte) 71, (byte) -48, (byte) 75, (byte) -33, (byte) -85, (byte) -17, (byte) 80, (byte) 17, (byte) 88, (byte) -76, (byte) 14, (byte) 120, (byte) 2, (byte) -53, (byte) 12, (byte) -104, (byte) 25, (byte) -128, (byte) 12, (byte) -115, (byte) 57, (byte) 95, (byte) -16, (byte) -96, (byte) 82, (byte) -51, (byte) 41, (byte) 95, (byte) 96, (byte) 39, (byte) -101, (byte) -25, (byte) -110, (byte) 123, (byte) 38, (byte) 42, (byte) 33, (byte) 99, (byte) -99, (byte) -36, (byte) 98, (byte) -8, (byte) 57, (byte) -95, (byte) 98, (byte) 7,
                        (byte) -51, (byte) 13, (byte) 15, (byte) 62, (byte) -23, (byte) -32, (byte) 54, (byte) -119, (byte) 91, (byte) -39, (byte) 94, (byte) -42, (byte) 127, (byte) 64, (byte) 19, (byte) -73, (byte) -38, (byte) -41, (byte) 0, (byte) 20, (byte) -15, (byte) -126, (byte) -37, (byte) 66, (byte) 32, (byte) -77, (byte) 4, (byte) 4, (byte) -101, (byte) 99, (byte) -77, (byte) -114, (byte) 35, (byte) -108, (byte) -61, (byte) 99, (byte) 121, (byte) -17, (byte) -69, (byte) 2, (byte) -12, (byte) 0, (byte) 6, (byte) -74, (byte) -75, (byte) 119, (byte) -80, (byte) -53, (byte) 17, (byte) 54, (byte) -66, (byte) -43, (byte) 28, (byte) -45, (byte) -106, (byte) -45, (byte) -44, (byte) -24, (byte) -4, (byte) -98, (byte) -17, (byte) -2, (byte) -79, (byte) -68, (byte) -56, (byte) 45, (byte) 103, (byte) -111, (byte) -44, (byte) 114, (byte) -78, (byte) 55, (byte) -33, (byte) 47, (byte) 88,
                        (byte) 22, (byte) 120, (byte) -11, (byte) 17, (byte) 55, (byte) -109, (byte) -13, (byte) 18, (byte) 23, (byte) 37, (byte) 92, (byte) 67, (byte) -11, (byte) 2, (byte) -110, (byte) -31, (byte) -45, (byte) 113, (byte) 106, (byte) 8, (byte) -128, (byte) -94, (byte) -55, (byte) 117, (byte) -46, (byte) -120, (byte) 103, (byte) 126, (byte) -107, (byte) -123, (byte) -86, (byte) 20, (byte) -12, (byte) 18, (byte) -47, (byte) -25, (byte) -29, (byte) -42, (byte) 36, (byte) 60, (byte) -50, (byte) -85, (byte) 43, (byte) 105, (byte) -117, (byte) -17, (byte) 92, (byte) -32, (byte) 65, (byte) -100, (byte) 77, (byte) 107, (byte) -38, (byte) -40, (byte) 97, (byte) -110, (byte) -126, (byte) -42, (byte) -15, (byte) -40, (byte) 26, (byte) -74, (byte) -68, (byte) 96, (byte) -87, (byte) -126, (byte) 58, (byte) 47, (byte) 122, (byte) -83, (byte) 90, (byte) -89, (byte) -61, (byte) 31,
                        (byte) 107, (byte) -65, (byte) 85, (byte) 77, (byte) 6, (byte) 95, (byte) 41, (byte) 102, (byte) 16, (byte) 37, (byte) -53, (byte) 117, (byte) -79, (byte) 24, (byte) -92, (byte) -95, (byte) 40, (byte) 86, (byte) 27, (byte) -87, (byte) -48, (byte) -79, (byte) -74, (byte) -47, (byte) 72, (byte) 34, (byte) -98, (byte) 126, (byte) 15, (byte) -38, (byte) 113, (byte) 73, (byte) 15, (byte) 17, (byte) -97, (byte) 75, (byte) -100, (byte) 33, (byte) 2, (byte) -127, (byte) -127, (byte) 0, (byte) -52, (byte) 83, (byte) 25, (byte) 122, (byte) -90, (byte) 46, (byte) -125, (byte) -44, (byte) -52, (byte) -85, (byte) -33, (byte) -74, (byte) 44, (byte) 48, (byte) 48, (byte) -5, (byte) -121, (byte) 63, (byte) 49, (byte) 16, (byte) -73, (byte) 18, (byte) -53, (byte) 38, (byte) -104, (byte) -15, (byte) -53, (byte) -66, (byte) -84, (byte) -53, (byte) -1, (byte) 61, (byte) -86,
                        (byte) -126, (byte) -61, (byte) 122, (byte) 118, (byte) 24, (byte) -110, (byte) 39, (byte) 2, (byte) -2, (byte) -80, (byte) -6, (byte) -52, (byte) 70, (byte) -23, (byte) 108, (byte) -28, (byte) -43, (byte) 43, (byte) 76, (byte) 2, (byte) 3, (byte) -107, (byte) -1, (byte) -8, (byte) 17, (byte) -86, (byte) 13, (byte) -67, (byte) 0, (byte) -13, (byte) 63, (byte) -87, (byte) 124, (byte) 90, (byte) -107, (byte) 34, (byte) 98, (byte) -39, (byte) 121, (byte) -58, (byte) 78, (byte) 82, (byte) 107, (byte) 115, (byte) -55, (byte) -101, (byte) 22, (byte) -108, (byte) 2, (byte) 30, (byte) 33, (byte) -63, (byte) -115, (byte) 5, (byte) 125, (byte) 35, (byte) 36, (byte) 117, (byte) -73, (byte) -88, (byte) -17, (byte) 52, (byte) -99, (byte) 11, (byte) -79, (byte) 32, (byte) 88, (byte) -71, (byte) -61, (byte) -9, (byte) 40, (byte) 95, (byte) -112, (byte) 105, (byte) -28,
                        (byte) 37, (byte) -114, (byte) 28, (byte) -114, (byte) -48, (byte) -51, (byte) 75, (byte) 42, (byte) -121, (byte) -39, (byte) 75, (byte) -16, (byte) -112, (byte) -123, (byte) 100, (byte) -32, (byte) -45, (byte) -108, (byte) 117, (byte) 107, (byte) 2, (byte) -127, (byte) -127, (byte) 0, (byte) -88, (byte) 34, (byte) -11, (byte) -63, (byte) -13, (byte) -80, (byte) 121, (byte) 31, (byte) -111, (byte) 57, (byte) 84, (byte) -43, (byte) -84, (byte) 82, (byte) 17, (byte) -117, (byte) -106, (byte) 95, (byte) 16, (byte) 82, (byte) 67, (byte) 32, (byte) -123, (byte) 23, (byte) -57, (byte) 10, (byte) -82, (byte) -119, (byte) -4, (byte) 24, (byte) 1, (byte) -43, (byte) 51, (byte) 0, (byte) 29, (byte) -121, (byte) 102, (byte) 73, (byte) 107, (byte) -112, (byte) 121, (byte) 32, (byte) 68, (byte) -71, (byte) -114, (byte) -52, (byte) 73, (byte) -108, (byte) 23, (byte) -48,
                        (byte) -23, (byte) -53, (byte) -94, (byte) 64, (byte) 40, (byte) 110, (byte) -109, (byte) 32, (byte) 34, (byte) -27, (byte) -43, (byte) 81, (byte) 29, (byte) 28, (byte) 52, (byte) -86, (byte) -39, (byte) -103, (byte) 76, (byte) -42, (byte) -60, (byte) -59, (byte) -16, (byte) -60, (byte) 13, (byte) -116, (byte) -108, (byte) -115, (byte) 60, (byte) -19, (byte) 20, (byte) 94, (byte) -115, (byte) -23, (byte) 60, (byte) 51, (byte) 84, (byte) 98, (byte) 67, (byte) -107, (byte) 96, (byte) -12, (byte) 9, (byte) -55, (byte) 36, (byte) 67, (byte) -27, (byte) 71, (byte) 83, (byte) 0, (byte) -18, (byte) -38, (byte) 102, (byte) 127, (byte) -14, (byte) 79, (byte) -77, (byte) -119, (byte) -65, (byte) 116, (byte) -2, (byte) 120, (byte) -101, (byte) 10, (byte) 71, (byte) -82, (byte) 29, (byte) 14, (byte) -6, (byte) 42, (byte) 3, (byte) -113, (byte) -59, (byte) 7, (byte) -45,
                        (byte) 56, (byte) -121, (byte) -35, (byte) 2, (byte) -127, (byte) -127, (byte) 0, (byte) -111, (byte) -54, (byte) -3, (byte) -44, (byte) 57, (byte) 61, (byte) -39, (byte) -115, (byte) 127, (byte) 25, (byte) 104, (byte) -64, (byte) -119, (byte) 89, (byte) 61, (byte) -86, (byte) 76, (byte) 127, (byte) -9, (byte) -105, (byte) -80, (byte) -71, (byte) -11, (byte) 65, (byte) 46, (byte) 66, (byte) -30, (byte) 25, (byte) -59, (byte) 28, (byte) -82, (byte) -100, (byte) -90, (byte) -51, (byte) 53, (byte) -124, (byte) 109, (byte) 61, (byte) -19, (byte) 19, (byte) 111, (byte) 28, (byte) -94, (byte) -43, (byte) -31, (byte) -22, (byte) -5, (byte) 102, (byte) 91, (byte) 6, (byte) 12, (byte) 37, (byte) -23, (byte) 13, (byte) -5, (byte) -5, (byte) 48, (byte) 92, (byte) 18, (byte) 86, (byte) 73, (byte) 53, (byte) -93, (byte) 92, (byte) 26, (byte) -88, (byte) -125, (byte) -50,
                        (byte) 4, (byte) 66, (byte) 58, (byte) 97, (byte) -100, (byte) -15, (byte) -121, (byte) -9, (byte) -22, (byte) 31, (byte) -63, (byte) -52, (byte) -29, (byte) -127, (byte) 123, (byte) 84, (byte) -92, (byte) -47, (byte) -26, (byte) 71, (byte) -69, (byte) 74, (byte) -87, (byte) -64, (byte) -118, (byte) 7, (byte) 93, (byte) 40, (byte) 110, (byte) 14, (byte) 78, (byte) 65, (byte) 97, (byte) 20, (byte) 6, (byte) -120, (byte) -59, (byte) 95, (byte) -69, (byte) 95, (byte) 50, (byte) -72, (byte) -21, (byte) -127, (byte) -80, (byte) 85, (byte) -68, (byte) 59, (byte) -75, (byte) -63, (byte) -124, (byte) 97, (byte) 60, (byte) 14, (byte) 34, (byte) -60, (byte) 21, (byte) -44, (byte) -55, (byte) -49, (byte) 2, (byte) -127, (byte) -127, (byte) 0, (byte) -93, (byte) -119, (byte) 93, (byte) -128, (byte) 72, (byte) -122, (byte) 93, (byte) 73, (byte) 86, (byte) -9, (byte) 108,
                        (byte) -116, (byte) 104, (byte) 15, (byte) 107, (byte) 1, (byte) 90, (byte) 65, (byte) 28, (byte) -98, (byte) -26, (byte) -85, (byte) -70, (byte) -56, (byte) 101, (byte) -48, (byte) -3, (byte) -13, (byte) 56, (byte) 64, (byte) 33, (byte) -120, (byte) 61, (byte) 18, (byte) -33, (byte) -91, (byte) -46, (byte) -41, (byte) -106, (byte) -57, (byte) -17, (byte) 8, (byte) 115, (byte) -48, (byte) 8, (byte) 20, (byte) -85, (byte) 124, (byte) 95, (byte) -83, (byte) -45, (byte) -71, (byte) 61, (byte) 23, (byte) 42, (byte) 60, (byte) 98, (byte) -35, (byte) -33, (byte) 19, (byte) -83, (byte) 104, (byte) 55, (byte) -24, (byte) 55, (byte) 81, (byte) -10, (byte) 84, (byte) 37, (byte) 104, (byte) -65, (byte) -5, (byte) -57, (byte) 50, (byte) -23, (byte) -6, (byte) -46, (byte) -126, (byte) -115, (byte) -64, (byte) 11, (byte) 15, (byte) -20, (byte) -9, (byte) -18, (byte) -107,
                        (byte) -2, (byte) 125, (byte) -107, (byte) 62, (byte) -74, (byte) 14, (byte) -89, (byte) 117, (byte) -4, (byte) -48, (byte) 13, (byte) 50, (byte) 82, (byte) -119, (byte) -107, (byte) -56, (byte) -41, (byte) -23, (byte) -102, (byte) -59, (byte) -22, (byte) -37, (byte) 16, (byte) 93, (byte) -69, (byte) 37, (byte) -109, (byte) -89, (byte) 108, (byte) 16, (byte) -18, (byte) 50, (byte) -93, (byte) 32, (byte) 58, (byte) 86, (byte) -100, (byte) 78, (byte) 0, (byte) 35, (byte) -4, (byte) 1, (byte) 2, (byte) -127, (byte) -128, (byte) 115, (byte) 119, (byte) -18, (byte) 79, (byte) -52, (byte) -6, (byte) 83, (byte) -25, (byte) 65, (byte) -42, (byte) 83, (byte) -80, (byte) 49, (byte) -83, (byte) 10, (byte) -27, (byte) -50, (byte) 1, (byte) -102, (byte) 69, (byte) 83, (byte) -51, (byte) -69, (byte) -112, (byte) 109, (byte) -62, (byte) -110, (byte) -42, (byte) 3, (byte) 86,
                        (byte) 60, (byte) -70, (byte) 62, (byte) 38, (byte) 62, (byte) 59, (byte) -71, (byte) -69, (byte) 2, (byte) -81, (byte) 11, (byte) 83, (byte) 60, (byte) -43, (byte) 107, (byte) 114, (byte) -61, (byte) 104, (byte) 48, (byte) -117, (byte) 57, (byte) 65, (byte) -28, (byte) -121, (byte) -65, (byte) -71, (byte) 81, (byte) 113, (byte) -127, (byte) -70, (byte) -14, (byte) -115, (byte) 119, (byte) -33, (byte) 47, (byte) -69, (byte) -127, (byte) 65, (byte) -19, (byte) 96, (byte) 121, (byte) -14, (byte) -111, (byte) 119, (byte) 85, (byte) 57, (byte) -3, (byte) -111, (byte) -125, (byte) 93, (byte) 47, (byte) 46, (byte) -55, (byte) -7, (byte) 20, (byte) -83, (byte) -71, (byte) -125, (byte) 115, (byte) 115, (byte) -28, (byte) 5, (byte) -49, (byte) 28, (byte) 30, (byte) 32, (byte) -34, (byte) 54, (byte) 57, (byte) 101, (byte) -23, (byte) 125, (byte) -3, (byte) 65, (byte) -99,
                        (byte) 64, (byte) -44, (byte) -38, (byte) -97, (byte) 2, (byte) -125, (byte) 0, (byte) -102, (byte) -125, (byte) -73, (byte) -110, (byte) 114, (byte) 55, (byte) -23, (byte) 26, (byte) -89, (byte) -98, (byte) 9, (byte) 6, (byte) 20, (byte) 1, (byte) 41, (byte) 13 }));
        final Signature sig = Signature.getInstance("Sha256WithRSA");
        sig.initSign(priv);
        sig.update(bytes, 0, bytes.length);
        byte[] signatureBytes = sig.sign();
        // Format to Hex. Alternatively: Use B64 or any other encoding
        final StringBuilder ret = new StringBuilder(signatureBytes.length * 2);
        String tmp;
        for (final byte d : signatureBytes) {
            tmp = Integer.toHexString(d & 0xFF);
            if (tmp.length() < 2) {
                ret.append('0');
            }
            ret.append(tmp);
        }
        return ret.toString();
    }
}
