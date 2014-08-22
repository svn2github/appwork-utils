/**
 * Copyright (c) 2009 - 2014 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils.swing.dialog
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.console;

import java.io.Console;

/**
 * @author Thomas
 * 
 */
public class ConsoleWrapper extends AbstractConsole {

    private Console _console;

    /**
     * @param console
     */
    public ConsoleWrapper(Console console) {
        this._console = console;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.appwork.utils.swing.dialog.AbstractConsole#write(java.lang.String)
     */
    @Override
    public void println(String string) {
        _console.writer().println(string);

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.utils.swing.dialog.AbstractConsole#readLine()
     */
    @Override
    public String readLine() {

        return _console.readLine();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.appwork.utils.swing.dialog.console.AbstractConsole#print(java.lang
     * .String)
     */
    @Override
    public void print(String string) {
        _console.writer().println(string);

    }

}
