/**
 * Copyright (c) 2009 - 2015 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 *
 * This file is part of org.appwork.utils.net.httpconnection
 *
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.net.httpconnection;

import java.io.IOException;

/**
 * @author daniel
 *
 */
public interface SSLSocketStreamFactory {

    public SocketStreamInterface create(SocketStreamInterface socketStream, final String host, final int port, final boolean autoclose, final boolean trustAll) throws IOException;
}
