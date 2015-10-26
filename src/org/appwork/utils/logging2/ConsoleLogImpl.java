/**
 * Copyright (c) 2009 - 2015 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 *
 * This file is part of org.appwork.utils.logging2
 *
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.logging2;

import org.appwork.utils.Exceptions;

/**
 * @author thomas
 *
 */
public class ConsoleLogImpl implements LogInterface {

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.utils.logging2.LogInterface#info(java.lang.String)
     */
    @Override
    public void info(String msg) {
        System.out.println("INFO >> " + msg);

    }

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.utils.logging2.LogInterface#log(java.lang.Throwable)
     */
    @Override
    public void log(Throwable e) {
        severe("Exception");
        System.err.println(Exceptions.getStackTrace(e));
    }

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.utils.logging2.LogInterface#fine(java.lang.String)
     */
    @Override
    public void fine(String msg) {
        System.out.println("FINE >> " + msg);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.utils.logging2.LogInterface#finest(java.lang.String)
     */
    @Override
    public void finest(String msg) {
        System.out.println("FINEST >> " + msg);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.utils.logging2.LogInterface#severe(java.lang.String)
     */
    @Override
    public void severe(String msg) {
        System.out.println("SEVERE >> " + msg);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.utils.logging2.LogInterface#finer(java.lang.String)
     */
    @Override
    public void finer(String msg) {
        System.out.println("FINER >> " + msg);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.utils.logging2.LogInterface#warning(java.lang.String)
     */
    @Override
    public void warning(String msg) {
        System.out.println("WARNING >> " + msg);
    }

}
