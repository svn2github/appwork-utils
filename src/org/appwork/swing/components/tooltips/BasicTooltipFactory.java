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

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;

import org.appwork.utils.swing.SwingUtils;

public class BasicTooltipFactory implements TooltipFactory {

    /**
     * @param name
     * @param editScriptAction_EditScriptAction_tt
     * @param icon2
     */
    public BasicTooltipFactory(String name, String tooltip, Icon icon2) {
        this.header = name;
        this.text = tooltip;
        this.icon = icon2;

    }

    @Override
    public ExtTooltip createTooltip() {
        TooltipPanel p = new TooltipPanel("ins 3", "[][grow,fill]", "[][grow,fill]");
        Color fg = new Color(ExtTooltip.createConfig(ExtTooltip.DEFAULT).getForegroundColor());
        JLabel headerLbl = SwingUtils.toBold(new JLabel(getHeader()));
        headerLbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, fg));
        headerLbl.setForeground(fg);
        JLabel iconLbl = new JLabel(getIcon());

        JLabel txt = new JLabel();
        txt.setForeground(fg);
        txt.setText("<html>" + getText().replaceAll("[\r\n]+", "<br>") + "</html>");
        p.add(headerLbl, "hidemode 2,spanx,pushx,growx");
        p.add(iconLbl, "hidemode 2");
        p.add(txt);
        return new PanelToolTip(p);
    }

    private String header;

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    private String text;
    private Icon   icon;
}
