package org.appwork.uio;

import javax.swing.Icon;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.locale._AWU;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;

public class BasicDialogHandler implements UserIOHandlerInterface {
    private static final Dialog D = Dialog.I();

    public boolean showConfirmDialog(final int flags, final String title, final String message, final Icon icon, final String ok, final String cancel) {
        try {
            D.showConfirmDialog(flags, title, message, icon, ok, cancel);
            return true;
        } catch (final DialogClosedException e) {
            // // TODO Auto-generated catch block
            // e.printStackTrace();
        } catch (final DialogCanceledException e) {
            // // TODO Auto-generated catch block
            // e.printStackTrace();
        }
        return false;
    }

    public boolean showConfirmDialog(final int flags, final String title, final String message) {
        return showConfirmDialog(flags, title, message, null, null, null);
    }

    public void showMessageDialog(final String message) {
        D.showMessageDialog(message);
    }

    // @SuppressWarnings("unchecked")
    public <T extends UserIODefinition> T show(final Class<T> class1, final T impl) {
        try {
            if (impl instanceof AbstractDialog) {
                D.showDialog((AbstractDialog<?>) impl);
            } else {
                throw new WTFException("Not Supported Dialog Type!: " + impl);
            }
        } catch (final DialogClosedException e) {

            // no Reason to log here
        } catch (final DialogCanceledException e) {
            // no Reason to log here
        }
        return impl;
    }

    public void showErrorMessage(final String message) {
        D.showErrorDialog(message);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.uio.UserIOHandlerInterface#showException(java.lang.String, java.lang.Throwable)
     */
    @Override
    public void showException(String message, Throwable e) {

        D.showExceptionDialog(_AWU.T.DIALOG_ERROR_TITLE(), message, e);
    }

}
