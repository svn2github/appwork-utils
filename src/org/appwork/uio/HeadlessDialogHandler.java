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
        // TODO Auto-generated method stub
        return false;
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
        // TODO Auto-generated method stub
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
        System.err.println("Dialog ->Error: " + message);

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
        System.err.println("Dialog ->Message: " + message);

    }

}
