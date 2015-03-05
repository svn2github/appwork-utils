package org.appwork.uio;

import javax.swing.Icon;

public interface UserIOHandlerInterface {

    <T extends UserIODefinition> T show(Class<T> class1, T impl);

    boolean showConfirmDialog(int flag, String title, String message);

    boolean showConfirmDialog(int flags, String title, String message, Icon icon, String ok, String cancel);

    void showErrorMessage(String message);

    void showMessageDialog(String message);

    /**
     * @param message
     * @param e
     */
    void showException(String message, Throwable e);

}
