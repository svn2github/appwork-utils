/**
 * Copyright (c) 2009 - 2014 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.swing.sunwrapper
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.sunwrapper.sun.swing;

import java.awt.Component;
import java.awt.FontMetrics;

import javax.swing.JComponent;

import org.appwork.utils.logging2.extmanager.LoggerFactory;

/**
 * @author Thomas
 * 
 */
public class SwingUtilities2Wrapper {

    private static boolean CLIP_STRING_IF_NECESSARY_OK = true;

    /**
     * @param rendererField
     * @param fontMetrics
     * @param str
     * @param i
     * @return
     */
    public static String clipStringIfNecessary(JComponent component, FontMetrics fontMetrics, String str, int availableWidth) {
        try {
            if (CLIP_STRING_IF_NECESSARY_OK) { return sun.swing.SwingUtilities2.clipStringIfNecessary(component, fontMetrics, str, availableWidth);

            }
        } catch (NoClassDefFoundError e) {
            CLIP_STRING_IF_NECESSARY_OK = false;
            LoggerFactory.I().getLogger(SwingUtilities2Wrapper.class.getName()).log(e);

        }
        System.err.println("sun.swing.SwingUtilities2.clipStringIfNecessary failed");
        return str;
    }

    /**
     * @param dispatchComponent
     * @param i
     */
    public static void setSkipClickCount(Component dispatchComponent, int i) {
        try {
            sun.swing.SwingUtilities2.setSkipClickCount(dispatchComponent, i);

        } catch (NoClassDefFoundError e) {

            LoggerFactory.I().getLogger(SwingUtilities2Wrapper.class.getName()).log(e);

        }
    }

}
