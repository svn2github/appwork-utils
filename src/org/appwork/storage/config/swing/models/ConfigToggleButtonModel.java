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
package org.appwork.storage.config.swing.models;

import java.awt.event.ItemEvent;

import javax.swing.JToggleButton.ToggleButtonModel;
import javax.swing.SwingUtilities;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.swing.EDTRunner;

public class ConfigToggleButtonModel extends ToggleButtonModel implements GenericConfigEventListener<Boolean> {

    /**
     *
     */
    private static final long       serialVersionUID = -3517910678740645735L;
    private final BooleanKeyHandler keyHandler;

    public ConfigToggleButtonModel(final BooleanKeyHandler keyHandler) {
        this.keyHandler = keyHandler;
        keyHandler.getEventSender().addListener(this, true);
        super.setSelected(keyHandler.isEnabled()); // we do not want to throw events, just change the gui!
    }

    public BooleanKeyHandler getKeyHandler() {
        return keyHandler;
    }

    public boolean isSelected() {
        return keyHandler.isEnabled();
    }

    public void setSelected(final boolean b) {
        // else this would hook into the button focus logic an might result in additional unwanted toggle calls
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                keyHandler.setValue(b);
            }
        });
    }

    private void fireItemStateChanged(final boolean selected) {
        fireItemStateChanged(new ItemEvent(ConfigToggleButtonModel.this, ItemEvent.ITEM_STATE_CHANGED, ConfigToggleButtonModel.this, selected ? ItemEvent.SELECTED : ItemEvent.DESELECTED));
    }

    public void onConfigValidatorError(final KeyHandler<Boolean> keyHandler, final Boolean invalidValue, final ValidationException validateException) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                fireStateChanged();
                fireItemStateChanged(getKeyHandler().isEnabled());
            }
        };
    }

    public void onConfigValueModified(final KeyHandler<Boolean> keyHandler, final Boolean newValue) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                fireStateChanged();
                fireItemStateChanged(Boolean.TRUE.equals(newValue));
            }
        };
    }

}
