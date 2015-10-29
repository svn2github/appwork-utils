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
package org.appwork.swing.components.tooltips;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import org.appwork.utils.swing.SwingUtils;

public class IconLabelToolTip extends ExtTooltip {

    /**
     * 
     */
    private static final long serialVersionUID = 1437567673004968332L;

    private String    name;

    private Icon icon;

    protected JLabel    label;

    /**
     * @param host
     * @param hosterIcon
     */
    public IconLabelToolTip(final String name, final Icon icon2) {
        this.name = name;
        icon = icon2;
        label.setText(name);
        label.setIcon(icon2);
    }

    @Override
    public TooltipPanel createContent() {
        final TooltipPanel ret = new TooltipPanel("ins 0", "[grow,fill]", "[]");
        label = new JLabel();
        SwingUtils.setOpaque(label, false);
        label.setForeground(new Color(getConfig().getForegroundColor()));
        ret.add(label);
        return ret;
    }

    public Icon getIcon() {
        return icon;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setIcon(final ImageIcon icon) {
        this.icon = icon;

        label.setIcon(icon);
        this.repaint();
    }

    @Override
    public void setName(final String name) {
        this.name = name;
        label.setText(name);
        this.repaint();
    }

    /* (non-Javadoc)
     * @see org.appwork.swing.components.tooltips.ExtTooltip#toText()
     */
    @Override
    public String toText() {
        // TODO Auto-generated method stub
        return label.getText();
    }
}
