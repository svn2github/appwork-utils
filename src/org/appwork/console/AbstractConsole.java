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

import org.appwork.utils.Application;

/**
 * @author Thomas
 * 
 */
public abstract class AbstractConsole {
    public static final Object LOCK = new Object();

    public static AbstractConsole newInstance() {

        if (Application.isJared(null)) {
            Console c = System.console();
            if (c != null) { return new ConsoleWrapper(c); }
        } else {
            return new IDEConsole();
        }
        return null;
    }

    /**
     * @param string
     */
    abstract public void println(String string);

    /**
     * @return
     */
    abstract public String readLine();

    /**
     * @return
     */
    abstract public String readPassword();

    /**
     * @param returnStrPos
     */
    abstract public void print(String string);

}
