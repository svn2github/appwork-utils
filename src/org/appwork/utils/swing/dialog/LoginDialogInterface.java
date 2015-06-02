package org.appwork.utils.swing.dialog;

import org.appwork.uio.In;
import org.appwork.uio.Out;

public interface LoginDialogInterface extends OKCancelCloseUserIODefinition {

    @Out
    public String getMessage();

    // input
    @Out
    public String getDefaultUsername();

    @Out
    public String getDefaultPassword();

    @Out
    public boolean isDefaultRememberSelected();

    @Out
    public boolean isRememberOptionVisible();

    // input
    @In
    public String getUsername();

    @In
    public String getPassword();

    @In
    public boolean isRememberSelected();
}