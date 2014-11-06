/**
 * Copyright (c) 2009 - 2014 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.sunwrapper.sun.swing
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.sunwrapper.sun.swing;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;

import org.appwork.utils.logging2.extmanager.LoggerFactory;

/**
 * @author Thomas
 * 
 */
public class DefaultLookupWrapper {

    /**
     * @param extTableHeaderRenderer
     * @param ui
     * @param string
     * @return
     */
    public static Color getColor(JComponent comp, ComponentUI ui, String key) {
        try {
            return sun.swing.DefaultLookup.getColor(comp, ui, key);
        } catch (final NoClassDefFoundError e) {
            LoggerFactory.I().getLogger(DefaultLookupWrapper.class.getName()).log(e);
            // DefaultLookupWrapper is sun.swing, any may not be
            // available
            // e.gh. in 1.6.0_01-b06
            return (Color) UIManager.get("TableHeader.focusCellForeground", comp.getLocale());

        }

    }

    /**
     * @param extTableHeaderRenderer
     * @param ui
     * @param string
     * @return
     */
    public static Border getBorder(JComponent comp, ComponentUI ui, String key) {
        try {
            return sun.swing.DefaultLookup.getBorder(comp, ui, key);

        } catch (final NoClassDefFoundError e) {
            // DefaultLookupWrapper is sun.swing, any may not be available
            // e.gh. in 1.6.0_01-b06

            return (Border) UIManager.get("TableHeader.focusCellBorder", comp.getLocale());

        }
    }

}
