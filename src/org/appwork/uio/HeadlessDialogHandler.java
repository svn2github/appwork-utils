/**
 * Copyright (c) 2009 - 2014 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.uio
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.uio;

import javax.swing.Icon;

import org.appwork.console.AbstractConsole;
import org.appwork.console.ConsoleDialog;
import org.appwork.utils.BinaryLogic;
import org.appwork.utils.Exceptions;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;

/**
 * @author Thomas
 * 
 */
public class HeadlessDialogHandler implements UserIOHandlerInterface {

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.uio.UserIOHandlerInterface#show(java.lang.Class, org.appwork.uio.UserIODefinition)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends UserIODefinition> T show(Class<T> class1, T impl) {

        if (impl instanceof ConfirmDialog) {
            final CloseReason cr;
            if (BinaryLogic.containsAll(impl.getFlags(), UIOManager.BUTTONS_HIDE_CANCEL)) {
                showMessageDialog(((ConfirmDialog) impl).getMessage());

                cr = CloseReason.OK;
            } else {
                if (showConfirmDialog(impl.getFlags(), impl.getTitle(), ((ConfirmDialog) impl).getMessage(), null, ((ConfirmDialog) impl).getOKButtonText(), ((ConfirmDialog) impl).getCancelButtonText())) {
                    cr = CloseReason.OK;
                } else {
                    cr = CloseReason.CANCEL;
                }
            }

            return (T) new ConfirmDialogInterface() {

                @Override
                public void throwCloseExceptions() throws DialogClosedException, DialogCanceledException {

                    switch (getCloseReason()) {
                    case CANCEL:
                        throw new DialogCanceledException(Dialog.RETURN_CANCEL);
                    case CLOSE:
                        throw new DialogCanceledException(Dialog.RETURN_CLOSED);
                    case TIMEOUT:
                        throw new DialogCanceledException(Dialog.RETURN_CLOSED | Dialog.RETURN_TIMEOUT);
                    case INTERRUPT:
                        throw new DialogCanceledException(Dialog.RETURN_CLOSED | Dialog.RETURN_INTERRUPT);
                    default:
                        return;
                    }
                }

                @Override
                public void setCloseReason(CloseReason closeReason) {

                }

                @Override
                public boolean isRemoteAPIEnabled() {

                    return false;
                }

                @Override
                public boolean isDontShowAgainSelected() {

                    return false;
                }

                @Override
                public String getTitle() {

                    return null;
                }

                @Override
                public int getTimeout() {

                    return 0;
                }

                @Override
                public int getFlags() {

                    return 0;
                }

                @Override
                public CloseReason getCloseReason() {

                    return cr;
                }

                @Override
                public String getOKButtonText() {

                    return null;
                }

                @Override
                public String getMessage() {

                    return null;
                }

                @Override
                public String getCancelButtonText() {

                    return null;
                }
            };
        }
        return impl;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.uio.UserIOHandlerInterface#showConfirmDialog(int, java.lang.String, java.lang.String)
     */
    @Override
    public boolean showConfirmDialog(int flag, String title, String message) {
        synchronized (AbstractConsole.LOCK) {
            ConsoleDialog cd = new ConsoleDialog(title);
            cd.start();
            cd.printLines(message);
            try {
                cd.waitYesOrNo(flag, "ok", "cancel");
                return true;
            } catch (DialogCanceledException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (DialogClosedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                cd.end();
            }
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.uio.UserIOHandlerInterface#showConfirmDialog(int, java.lang.String, java.lang.String, javax.swing.Icon,
     * java.lang.String, java.lang.String)
     */
    @Override
    public boolean showConfirmDialog(int flags, String title, String message, Icon icon, String ok, String cancel) {
        synchronized (AbstractConsole.LOCK) {
            ConsoleDialog cd = new ConsoleDialog(title);
            cd.start();
            cd.printLines(message);
            try {
                cd.waitYesOrNo(flags, ok, cancel);
                return true;
            } catch (DialogCanceledException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (DialogClosedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                cd.end();
            }

        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.uio.UserIOHandlerInterface#showErrorMessage(java.lang.String)
     */
    @Override
    public void showErrorMessage(String message) {
        synchronized (AbstractConsole.LOCK) {
            ConsoleDialog cd = new ConsoleDialog("Error!");
            cd.printLines(message);
            cd.start();
            try {
                cd.waitYesOrNo(UIOManager.BUTTONS_HIDE_OK, null, null);

            } catch (DialogCanceledException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (DialogClosedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                cd.end();
            }
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.uio.UserIOHandlerInterface#showMessageDialog(java.lang.String )
     */
    @Override
    public void showMessageDialog(String message) {
        ConsoleDialog cd = new ConsoleDialog("Message");
        cd.printLines(message);
        try {
            cd.waitYesOrNo(UIOManager.BUTTONS_HIDE_OK, null, null);

        } catch (DialogCanceledException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (DialogClosedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.uio.UserIOHandlerInterface#showException(java.lang.String, java.lang.Throwable)
     */
    @Override
    public void showException(String message, Throwable e1) {
        synchronized (AbstractConsole.LOCK) {
            ConsoleDialog cd = new ConsoleDialog(message);
            cd.printLines(message);
            cd.printLines(Exceptions.getStackTrace(e1));
            cd.start();
            try {
                cd.waitYesOrNo(UIOManager.BUTTONS_HIDE_OK, null, null);

            } catch (DialogCanceledException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (DialogClosedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                cd.end();
            }
        }
    }

}
