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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MappingJsonFactory;

/**
 * @author thomas
 *
 */
public class ExtJsonFactory extends MappingJsonFactory {

    /**
     * Method for constructing json generator for writing json content to specified file, overwriting contents it might have (or creating it
     * if such file does not yet exist). Encoding to use must be specified, and needs to be one of available types (as per JSON
     * specification).
     * <p>
     * Underlying stream <b>is owned</b> by the generator constructed, i.e. generator will handle closing of file when
     * {@link JsonGenerator#close} is called.
     *
     * @param f
     *            File to write contents to
     * @param enc
     *            Character encoding to use
     */
    public JsonGenerator createJsonGenerator(final File f, final JsonEncoding enc) throws IOException {
        final FileOutputStream fos = new FileOutputStream(f);
        final JsonGenerator ret = super.createGenerator(fos, enc);
        ret.useDefaultPrettyPrinter();
        return ret;
    }

    /**
     * Method for constructing JSON generator for writing JSON content using specified output stream. Encoding to use must be specified, and
     * needs to be one of available types (as per JSON specification).
     * <p>
     * Underlying stream <b>is NOT owned</b> by the generator constructed, so that generator will NOT close the output stream when
     * {@link JsonGenerator#close} is called (unless auto-closing feature,
     * {@link org.codehaus.jackson.JsonGenerator.Feature#AUTO_CLOSE_TARGET} is enabled). Using application needs to close it explicitly if
     * this is the case.
     *
     * @param out
     *            OutputStream to use for writing JSON content
     * @param enc
     *            Character encoding to use
     */
    @Override
    public JsonGenerator createJsonGenerator(final OutputStream out, final JsonEncoding enc) throws IOException {
        final JsonGenerator ret = super.createGenerator(out, enc);
        ret.useDefaultPrettyPrinter();
        return ret;
    }

    /**
     * Method for constructing JSON generator for writing JSON content using specified Writer.
     * <p>
     * Underlying stream <b>is NOT owned</b> by the generator constructed, so that generator will NOT close the Reader when
     * {@link JsonGenerator#close} is called (unless auto-closing feature,
     * {@link org.codehaus.jackson.JsonGenerator.Feature#AUTO_CLOSE_TARGET} is enabled). Using application needs to close it explicitly.
     *
     * @param out
     *            Writer to use for writing JSON content
     */
    @Override
    public JsonGenerator createJsonGenerator(final Writer out) throws IOException {
        final JsonGenerator ret = super.createJsonGenerator(out);
        ret.useDefaultPrettyPrinter();
        return ret;
    }
}
