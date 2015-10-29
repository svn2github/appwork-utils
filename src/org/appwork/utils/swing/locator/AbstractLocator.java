/**
 * 
 * ====================================================================================================================================================
 * 	    "MyJDownloader Client" License
 * 	    The "MyJDownloader Client" will be called [The Product] from now on.
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
package org.appwork.utils.swing.locator;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

/**
 * @author Thomas
 * 
 */
public abstract class AbstractLocator implements Locator {

    public static Point correct(final Point point, final Window d) {
        final Dimension prefSize = d.getSize();

        return correct(point, prefSize);

    }

    public static Point correct(final Point point, final Dimension prefSize) {
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice[] screens = ge.getScreenDevices();

        final Rectangle preferedRect = new Rectangle(point.x, point.y, prefSize.width, prefSize.height);
        GraphicsDevice biggestInteresctionScreem = null;
        int biggestIntersection = -1;

        for (final GraphicsDevice screen : screens) {
            final Rectangle bounds = screen.getDefaultConfiguration().getBounds();
            final Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(screen.getDefaultConfiguration());
            bounds.x += insets.left;
            bounds.y += insets.top;
            bounds.width -= insets.left + insets.right;
            bounds.height -= insets.top + insets.bottom;
            final Rectangle interSec = bounds.intersection(preferedRect);
            if (Math.max(interSec.width, 0) * Math.max(interSec.height, 0) > biggestIntersection || biggestInteresctionScreem == null) {
                biggestIntersection = Math.max(interSec.width, 0) * Math.max(interSec.height, 0);
                biggestInteresctionScreem = screen;
                if (interSec.equals(preferedRect)) {
                    break;
                }
            }
        }
        final Rectangle bounds = biggestInteresctionScreem.getDefaultConfiguration().getBounds();
        final Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(biggestInteresctionScreem.getDefaultConfiguration());
        bounds.x += insets.left;
        bounds.y += insets.top;
        bounds.width -= insets.left + insets.right;
        bounds.height -= insets.top + insets.bottom;
        if (preferedRect.x + preferedRect.width > bounds.x + bounds.width) {
            preferedRect.x = bounds.x + bounds.width - preferedRect.width;
        }
        if (preferedRect.y + preferedRect.height > bounds.y + bounds.height) {
            preferedRect.y = bounds.y + bounds.height - preferedRect.height;
        }
        if (preferedRect.x < bounds.x) {
            preferedRect.x = bounds.x;
        }

        if (preferedRect.y < bounds.y) {
            preferedRect.y = bounds.y;
        }

        return preferedRect.getLocation();
    }

    /**
     * @param point
     * @param dialog
     * @return
     */
    public static Point validate(Point point, final Window dialog) {
        point = AbstractLocator.correct(point, dialog);
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice[] screens = ge.getScreenDevices();

        // for (final GraphicsDevice screen : screens) {
        for (final GraphicsDevice screen : screens) {
            final Rectangle bounds = screen.getDefaultConfiguration().getBounds();
            if (bounds.contains(point)) { return point;
            // if (point.x >= bounds.x && point.x < bounds.x + bounds.width) {
            // if (point.y >= bounds.y && point.y < bounds.y + bounds.height) {
            // // found point on screen
            // if (point.x + dimension.width <= bounds.x + bounds.width) {
            //
            // if (point.y + dimension.height <= bounds.y + bounds.height) {
            // // dialog is completly visible on this screen
            // return point;
            // }
            // }
            //
            // }
            // }
            }
        }

        return new CenterOfScreenLocator().getLocationOnScreen(dialog);

    }
}
