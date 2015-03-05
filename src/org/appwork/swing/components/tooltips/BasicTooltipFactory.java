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
