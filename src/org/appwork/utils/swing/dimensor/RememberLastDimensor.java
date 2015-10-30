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
package org.appwork.utils.swing.dimensor;

import java.awt.Dimension;
import java.awt.Window;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.Hash;
import org.appwork.utils.swing.dialog.LocationStorage;

/**
 * @author Thomas
 * 
 */
public class RememberLastDimensor extends AbstractDimensor {

    private String id;

    /**
     * @param id
     */
    public RememberLastDimensor(final String id) {
        this.id = id;
    }

    /**
     * @param dialog
     * @return
     */
    public Dimension getDimension(final Window dialog) {
        final LocationStorage cfg = createConfig(dialog);
        if (cfg.isValid()) {

        return validate(new Dimension(cfg.getX(), cfg.getY()), dialog); }
        return null;
    }

    /**
     * @param dialog
     * @return
     */
    private LocationStorage createConfig(final Window dialog) {
        String storageID = RememberLastDimensor.class.getSimpleName() + "-" + getID(dialog);
        if (storageID.length() > 128) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < storageID.length(); i++) {
                char c = storageID.charAt(i);
                switch (c) {
                case 'U':
                case 'E':
                case 'I':
                case 'O':
                case 'A':
                case 'J':
                case 'u':
                case 'e':
                case 'i':
                case 'o':
                case 'a':
                case 'j':
                    continue;
                default:
                    sb.append(c);

                }
            }
            storageID = sb.toString();

        }
        if (storageID.length() > 128) {
            storageID = RememberLastDimensor.class.getSimpleName() + "-" + Hash.getMD5(storageID);
        }
        return JsonConfig.create(Application.getResource("cfg/" + storageID), LocationStorage.class);
    }

    protected String getID(final Window dialog) {
        if (id == null) { return dialog.toString(); }
        return id;
    }

    /**
     * @param dialog
     */
    public void onClose(final Window frame) {
        if (frame.isShowing()) {
            final LocationStorage cfg = createConfig(frame);
            cfg.setValid(true);
            cfg.setX(frame.getWidth());
            cfg.setY(frame.getHeight());
            cfg._getStorageHandler().write();
        }

    }

}
