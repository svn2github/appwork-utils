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
package org.appwork.utils.swing.locator;

import java.awt.Point;
import java.awt.Window;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.Hash;
import org.appwork.utils.swing.dialog.LocationStorage;

public class RememberAbsoluteLocator extends AbstractLocator {

    private final String id;

    /**
     * @param string
     */
    public RememberAbsoluteLocator(final String id) {
        this.id = cleanId(id);
        if (id == null) { throw new IllegalArgumentException("id ==null"); }
    }

    /**
     * @param id2
     * @return
     */
    private String cleanId(String id) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            switch (c) {
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'g':
            case 'h':
            case 'i':
            case 'j':
            case 'k':
            case 'l':
            case 'm':
            case 'n':
            case 'o':
            case 'p':
            case 'q':
            case 'r':
            case 's':
            case 't':
            case 'u':
            case 'v':
            case 'w':
            case 'x':
            case 'y':
            case 'z':
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
            case 'G':
            case 'H':
            case 'J':
            case 'K':
            case 'L':
            case 'M':
            case 'N':
            case 'O':
            case 'P':
            case 'Q':
            case 'R':
            case 'S':
            case 'T':
            case 'U':
            case 'V':
            case 'W':
            case 'X':
            case 'Y':
            case 'Z':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case '-':
            case '_':
            case '.':
                sb.append(c);

            default:
                continue;

            }
        }
        return sb.toString();
    }

    /**
     * @param dialog
     * @return
     */
    private LocationStorage createConfig(final Window dialog) {
        String storageID = RememberAbsoluteLocator.class.getSimpleName() + "-" + this.getID(dialog);
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
            storageID = RememberAbsoluteLocator.class.getSimpleName() + "-" + Hash.getMD5(storageID);
        }
        return JsonConfig.create(Application.getResource("cfg/" + storageID), LocationStorage.class);
    }

    /**
     * @param dialog
     * @return
     */
    protected String getID(final Window dialog) {
        return this.id;
    }

    @Override
    public Point getLocationOnScreen(final Window dialog) {
        try {
            final LocationStorage cfg = this.createConfig(dialog);
            if (cfg.isValid()) {

                // Do a "is on screen check" here

                return AbstractLocator.validate(new Point(cfg.getX(), cfg.getY()), dialog);
            }
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        return new CenterOfScreenLocator().getLocationOnScreen(dialog);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.appwork.utils.swing.dialog.Locator#onClose(org.appwork.utils.swing
     * .dialog.frame)
     */
    @Override
    public void onClose(final Window frame) {
        try {
            if (frame.isShowing()) {
                final Point loc = frame.getLocationOnScreen();
                final LocationStorage cfg = this.createConfig(frame);
                cfg.setValid(true);
                cfg.setX(loc.x);
                cfg.setY(loc.y);
                cfg._getStorageHandler().write();
            }
        } catch (final Throwable e) {
            e.printStackTrace();
            // nothing.... frame.getParent or parent might be invisible
        }
    }

}
