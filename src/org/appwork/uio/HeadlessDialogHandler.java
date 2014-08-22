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

import javax.swing.ImageIcon;

import org.appwork.console.AbstractConsole;
import org.appwork.console.ConsoleDialog;
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
     * @see org.appwork.uio.UserIOHandlerInterface#show(java.lang.Class,
     * org.appwork.uio.UserIODefinition)
     */
    @Override
    public <T extends UserIODefinition> T show(Class<T> class1, T impl) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.uio.UserIOHandlerInterface#showConfirmDialog(int,
     * java.lang.String, java.lang.String)
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
     * @see org.appwork.uio.UserIOHandlerInterface#showConfirmDialog(int,
     * java.lang.String, java.lang.String, javax.swing.ImageIcon,
     * java.lang.String, java.lang.String)
     */
    @Override
    public boolean showConfirmDialog(int flags, String title, String message, ImageIcon icon, String ok, String cancel) {
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
     * @see
     * org.appwork.uio.UserIOHandlerInterface#showErrorMessage(java.lang.String)
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
     * @see
     * org.appwork.uio.UserIOHandlerInterface#showMessageDialog(java.lang.String
     * )
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

}
