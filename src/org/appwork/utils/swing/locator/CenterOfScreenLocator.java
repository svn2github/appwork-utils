/**
 * 
 * ====================================================================================================================================================
 * "AppWork Utilities" License
 * ====================================================================================================================================================
 * Copyright (c) 2009-2015, AppWork GmbH <e-mail@appwork.org>
 * Schwabacher Straße 117
 * 90763 Fürth
 * Germany
 * 
 * === Preamble ===
 * This license establishes the terms under which the AppWork Utilities Source Code & Binary files may be used, copied, modified, distributed, and/or redistributed.
 * The intent is that the AppWork GmbH is able to provide  their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 * These terms apply to all files that have the "AppWork Utilities" License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 * 
 * === 3rd Party Licences ===
 * Some parts of the AppWork Utilities use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 * to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header.
 * 
 * === Definition: Commercial Usage ===
 * If anybody or any organization is generating income (directly or indirectly) by using "AppWork Utilities" or if there's as much as a
 * sniff of commercial interest or aspect in what you are doing, we consider this as a commercial usage. If you are unsure whether your use-case is commercial or not, consider it as commercial.
 * === Dual Licensing ===
 * === Commercial Usage ===
 * If you want to use AppWork Utilities in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 * Contact AppWork for further details: e-mail@appwork.org
 * === Non-Commercial Usage ===
 * If there is no commercial usage (see definition above), you may use AppWork Utilities under the terms of the
 * "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 * 
 * If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.utils.swing.locator;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

import org.appwork.utils.swing.SwingUtils;

/**
 * @author Thomas
 *
 */
public class CenterOfScreenLocator extends AbstractLocator {

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.utils.swing.dialog.Locator#getLocationOnScreen(javax.swing .JDialog)
     */
    @Override
    public Point getLocationOnScreen(final Window dialog) {
        if (dialog.getParent() == null || !dialog.getParent().isDisplayable() || !dialog.getParent().isVisible()) {
            final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            return correct(new Point((int) (screenSize.getWidth() - dialog.getWidth()) / 2, (int) (screenSize.getHeight() - dialog.getHeight()) / 2), dialog);

        } else if (dialog.getParent() instanceof Frame && ((Frame) dialog.getParent()).getExtendedState() == Frame.ICONIFIED) {
            // dock dialog at bottom right if mainframe is not visible

            final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            final GraphicsDevice[] screens = ge.getScreenDevices();

            for (final GraphicsDevice screen : screens) {
                final Rectangle bounds = screen.getDefaultConfiguration().getBounds();
                screen.getDefaultConfiguration().getDevice();

                final Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(screen.getDefaultConfiguration());
                if (bounds.contains(MouseInfo.getPointerInfo().getLocation())) {

                    return correct(new Point((int) (bounds.x + bounds.getWidth() - dialog.getWidth() - 20 - insets.right), (int) (bounds.y + bounds.getHeight() - dialog.getHeight() - 20 - insets.bottom)), dialog);

                }

            }
            final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            return correct(new Point((int) (screenSize.getWidth() - dialog.getWidth() - 20), (int) (screenSize.getHeight() - dialog.getHeight() - 60)), dialog);
        } else {
            final Point ret = SwingUtils.getCenter(dialog.getParent(), dialog);

            return correct(ret, dialog);
        }

        // if (frame.getParent() == null || !frame.getParent().isDisplayable()
        // || !frame.getParent().isVisible()) {
        // final Dimension screenSize =
        // Toolkit.getDefaultToolkit().getScreenSize();
        //
        // return (new Point((int) (screenSize.getWidth() - frame.getWidth()) /
        // 2, (int) (screenSize.getHeight() - frame.getHeight()) / 2));
        //
        // } else if (frame.getParent() instanceof Frame && ((Frame)
        // frame.getParent()).getExtendedState() == Frame.ICONIFIED) {
        // // dock dialog at bottom right if mainframe is not visible
        //
        // final GraphicsEnvironment ge =
        // GraphicsEnvironment.getLocalGraphicsEnvironment();
        // final GraphicsDevice[] screens = ge.getScreenDevices();
        //
        // for (final GraphicsDevice screen : screens) {
        // final Rectangle bounds =
        // screen.getDefaultConfiguration().getBounds();
        // screen.getDefaultConfiguration().getDevice();
        //
        // Insets insets =
        // Toolkit.getDefaultToolkit().getScreenInsets(screen.getDefaultConfiguration());
        // if (bounds.contains(MouseInfo.getPointerInfo().getLocation())) {
        // return (new Point((int) (bounds.x + bounds.getWidth() -
        // frame.getWidth() - 20 - insets.right), (int) (bounds.y +
        // bounds.getHeight() - frame.getHeight() - 20 - insets.bottom))); }
        //
        // }
        // final Dimension screenSize =
        // Toolkit.getDefaultToolkit().getScreenSize();
        // return (new Point((int) (screenSize.getWidth() - frame.getWidth() -
        // 20), (int) (screenSize.getHeight() - frame.getHeight() - 60)));
        // } else {
        // return SwingUtils.getCenter(frame.getParent(), frame);
        // }

    }

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.utils.swing.dialog.Locator#onClose(org.appwork.utils.swing .dialog.AbstractDialog)
     */
    @Override
    public void onClose(final Window abstractDialog) {
        // TODO Auto-generated method stub

    }

}
