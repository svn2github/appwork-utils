/**
 * Copyright (c) 2009 - 2015 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 *
 * This file is part of org.appwork.txtresource
 *
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.svn;

import java.util.Locale;

public abstract class LocaleRunnable<T, E extends Exception> {
    private static final Object LOCK = new Object();

    protected abstract T run() throws E;

    public T runEnglish() throws E {
        synchronized (LOCK) {

            Locale bef = Locale.getDefault();
            Locale.setDefault(Locale.ENGLISH);
            try {
                return run();
            } finally {
                Locale.setDefault(bef);
            }
        }
    }

}
