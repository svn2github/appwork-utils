/**
 * Copyright (c) 2009 - 2011 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils.net.httpserver
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.net.httpserver.requests;

import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.net.HeaderCollection;
import org.appwork.utils.net.httpserver.HttpConnection;

/**
 * @author daniel
 * 
 */
public abstract class HttpRequest implements HttpRequestInterface {

    protected String           requestedURL   = null;

    protected HeaderCollection requestHeaders = null;

    protected String           requestedPath  = null;
    protected String           serverName     = null;

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getServerProtocol() {
        return serverProtocol;
    }

    public void setServerProtocol(String serverProtocol) {
        this.serverProtocol = serverProtocol;
    }

    public boolean isHttps() {
        return https;
    }

    public void setHttps(boolean https) {
        this.https = https;
    }

    protected int                  serverPort             = -1;
    protected String               serverProtocol         = null;
    protected boolean              https                  = false;
    protected List<KeyValuePair>   requestedURLParameters = null;

    private List<String>           remoteAddress          = new ArrayList<String>();

    protected final HttpConnection connection;

    public HttpConnection getConnection() {
        return connection;
    }

    public HttpRequest(final HttpConnection connection) {
        this.connection = connection;
    }

    /**
     * @see http://en.wikipedia.org/wiki/X-Forwarded-For There may be several Remote Addresses if the connection is piped through several
     *      proxies.<br>
     *      [0] is always the direct address.<br>
     *      if remoteAdresses.size>1 then<br>
     *      [1] is the actuall clients ip.<br>
     *      [2] is the proxy next to him..<br>
     *      [3] is the proxy next to [2]<br>
     *      ..<br>
     *      [size-1] should be the address next to [0]<br>
     * @param inetAddress
     */
    public List<String> getRemoteAddress() {
        return remoteAddress;
    }

    public String getRequestedPath() {
        return requestedPath;
    }

    public String getRequestedURL() {
        return requestedURL;
    }

    /**
     * @return the requestedURLParameters
     */
    public List<KeyValuePair> getRequestedURLParameters() {
        return requestedURLParameters;
    }

    public HeaderCollection getRequestHeaders() {
        return requestHeaders;
    }

    /**
     * @see http://en.wikipedia.org/wiki/X-Forwarded-For There may be several Remote Addresses if the connection is piped through several
     *      proxies.<br>
     *      [0] is always the direct address.<br>
     *      if remoteAdresses.size>1 then<br>
     *      [1] is the actuall clients ip.<br>
     *      [2] is the proxy next to him..<br>
     *      [3] is the proxy next to [2]<br>
     *      ..<br>
     *      [size-1] should be the address next to [0]<br>
     * @param inetAddress
     */
    public void setRemoteAddress(final List<String> remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    /**
     * @param requestedPath
     *            the requestedPath to set
     */
    public void setRequestedPath(final String requestedPath) {
        this.requestedPath = requestedPath;
    }

    /**
     * @param requestedURL
     *            the requestedURL to set
     */
    public void setRequestedURL(final String requestedURL) {
        this.requestedURL = requestedURL;
    }

    /**
     * @param requestedURLParameters
     *            the requestedURLParameters to set
     */
    public void setRequestedURLParameters(final List<KeyValuePair> requestedURLParameters) {
        this.requestedURLParameters = requestedURLParameters;
    }

    public void setRequestHeaders(final HeaderCollection requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

}
