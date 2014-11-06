/**
 * Copyright (c) 2009 - 2014 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.sunwrapper
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.sunwrapper;

/**
 * @author Thomas
 * 
 */
public class WrapperNotAvailableException extends Exception {

    /**
     * @param paramThrowable
     */
    public WrapperNotAvailableException(Error paramThrowable) {
        super(paramThrowable);

    }

}
