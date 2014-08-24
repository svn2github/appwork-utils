package org.appwork.utils.swing;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.appwork.swing.MigPanel;
import org.appwork.uio.MessageDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;

public class IconDialog extends AbstractDialog<Integer> implements MessageDialogInterface {

    private Icon bigIcon;
    private String    message;
    private JLabel    lbl;

    public IconDialog(final int flag, final String title, final String msg, final Icon icon, final String okText) {
        super(flag | Dialog.STYLE_HIDE_ICON | UIOManager.BUTTONS_HIDE_CANCEL, title, icon, okText, null);
        bigIcon = icon;
        message = msg;

    }

    public String getMessage() {
        return message;
    }

    @Override
    protected Integer createReturnValue() {
        // TODO Auto-generated method stub
        return getReturnmask();
    }

    public void setIcon(Icon icon) {
        lbl.setIcon(icon);
    }

    @Override
    public JComponent layoutDialogContent() {
        final MigPanel p = new MigPanel("wrap 1", "[grow,fill]", "[][]");

        p.add(lbl = new JLabel());
        if(bigIcon!=null)lbl.setIcon(bigIcon);
        p.add(new JLabel(getMessage(), SwingConstants.CENTER));
        return p;
    }

    /**
     * 
     */
    public MessageDialogInterface show() {
        return UIOManager.I().show(MessageDialogInterface.class, this);
    }

}
