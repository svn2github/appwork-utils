/**
 *
 * ====================================================================================================================================================
 *         "AppWork Utilities" License
 *         The "AppWork Utilities" will be called [The Product] from now on.
 * ====================================================================================================================================================
 *         Copyright (c) 2009-2015, AppWork GmbH <e-mail@appwork.org>
 *         Schwabacher Straße 117
 *         90763 Fürth
 *         Germany
 * === Preamble ===
 *     This license establishes the terms under which the [The Product] Source Code & Binary files may be used, copied, modified, distributed, and/or redistributed.
 *     The intent is that the AppWork GmbH is able to provide  their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 *     These terms apply to all files that have the [The Product] License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 *
 * === 3rd Party Licences ===
 *     Some parts of the [The Product] use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 *     to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header.
 *
 * === Definition: Commercial Usage ===
 *     If anybody or any organization is generating income (directly or indirectly) by using [The Product] or if there's any commercial interest or aspect in what you are doing, we consider this as a commercial usage.
 *     If your use-case is neither strictly private nor strictly educational, it is commercial. If you are unsure whether your use-case is commercial or not, consider it as commercial or contact as.
 * === Dual Licensing ===
 * === Commercial Usage ===
 *     If you want to use [The Product] in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 *     Contact AppWork for further details: e-mail@appwork.org
 * === Non-Commercial Usage ===
 *     If there is no commercial usage (see definition above), you may use [The Product] under the terms of the
 *     "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 *
 *     If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.utils.net;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.extmanager.LoggerFactory;

/**
 * @author daniel
 * @date 02.05.2016
 *
 */
public class URLHelper {
    public static String createURL(final String protocol, final String userInfo, final String host, final int port, final String path, final String query, final String ref) throws MalformedURLException {
        final StringBuilder sb = new StringBuilder();
        sb.append(protocol);
        sb.append("://");
        if (userInfo != null && userInfo.length() > 0) {
            sb.append(userInfo);
            sb.append("@");
        }
        sb.append(host);
        if (port != -1) {
            sb.append(":");
            sb.append(port);
        }
        if (path != null && path.length() > 0) {
            final String encodedURLPath;
            try {
                encodedURLPath = encodeURLPathSegment(path);
            } catch (UnsupportedEncodingException shouldNeverHappen) {
                throw new MalformedURLException(shouldNeverHappen.getMessage());
            }
            if (encodedURLPath.startsWith("/")) {
                sb.append(encodedURLPath);
            } else {
                sb.append("/");
                sb.append(encodedURLPath);
            }
        } else {
            sb.append("/");
        }
        if (query != null && query.length() > 0) {
            sb.append("?");
            /**
             * TODO: implement encodeURLQuerySegment
             */
            sb.append(query);
        }
        if (ref != null && ref.length() > 0) {
            sb.append("#");
            sb.append(ref);
        }
        return sb.toString();
    }

    /**
     * this method takes care of correct encoding of path segment of an URL
     *
     * @param urlPath
     * @return
     * @throws UnsupportedEncodingException
     */
    private static String encodeURLPathSegment(final String urlPath) throws UnsupportedEncodingException {
        if (urlPath != null) {
            boolean encodeRequired = false;
            for (int index = 0; index < urlPath.length(); index++) {
                final char c = urlPath.charAt(index);
                if (c == '"' || c == ' ' || c == '[' || c == ']' || c == '{' || c == '}') {
                    encodeRequired = true;
                    break;
                } else if (c > 127) {
                    encodeRequired = true;
                    break;
                }
            }
            if (encodeRequired) {
                final StringBuilder encodedURLPath = new StringBuilder();
                for (int index = 0; index < urlPath.length(); index++) {
                    final char c = urlPath.charAt(index);
                    if (c == ' ') {
                        encodedURLPath.append("%20");
                    } else if (c == '"') {
                        encodedURLPath.append("%22");
                    } else if (c == '[') {
                        encodedURLPath.append("%5B");
                    } else if (c == ']') {
                        encodedURLPath.append("%5D");
                    } else if (c == '{') {
                        encodedURLPath.append("%7B");
                    } else if (c == '}') {
                        encodedURLPath.append("%7D");
                    } else if (c <= 127) {
                        encodedURLPath.append(c);
                    } else {
                        encodedURLPath.append(URLEncoder.encode(String.valueOf(c), "UTF-8"));
                    }
                }
                return encodedURLPath.toString();
            }
        }
        return urlPath;
    }

    public static URL createURL(final String url) throws MalformedURLException {
        final URL tmp = new URL(url.trim().replaceAll(" ", "%20"));
        if (tmp.getPath() != null && tmp.getQuery() == null && tmp.getPath().matches(".*(\\&(?!amp;)).*")) {
            final Pattern search = Pattern.compile(".*?\\&(?!amp;)");
            final Matcher matcher = search.matcher(tmp.getPath());
            if (matcher.find()) {
                final int index = matcher.end();
                final String newPath = tmp.getPath().substring(0, index - 1);
                final String newQuery = tmp.getPath().substring(index);
                if (newQuery.matches("^[a-zA-Z0-9%]+=.*?")) {
                    final String newURL = createURL(tmp.getProtocol(), tmp.getUserInfo(), tmp.getHost(), tmp.getPort(), newPath, newQuery, tmp.getRef());
                    LoggerFactory.getDefaultLogger().info("Apply auto fix '&->?'|" + url + "|" + newURL);
                    return new URL(newURL);
                }
            }
        }
        final String newURL = createURL(tmp.getProtocol(), tmp.getUserInfo(), tmp.getHost(), tmp.getPort(), tmp.getPath(), tmp.getQuery(), tmp.getRef());
        return new URL(newURL);
    }

    public static String parseLocation(final URL url, final String loc) {
        final String location = loc.trim().replaceAll(" ", "%20");
        try {
            if (location.matches("^https?://.+")) {
                final URL dummyURL = createURL(location);
                return fixPathTraversal(dummyURL).toString();
            } else if (location.matches("^:\\d+/.*")) {
                // scheme + host + loc
                final URL dummyURL = createURL(url.getProtocol() + "://" + url.getHost() + location);
                return fixPathTraversal(dummyURL).toString();
            } else if (location.startsWith("//")) {
                // scheme + loc
                final URL dummyURL = createURL(url.getProtocol() + ":" + location);
                return fixPathTraversal(dummyURL).toString();
            } else if (location.startsWith("/")) {
                final StringBuilder sb = new StringBuilder();
                sb.append(url.getProtocol()).append("://");
                sb.append(url.getHost());
                if (url.getPort() != -1) {
                    sb.append(":").append(url.getPort());
                }
                sb.append(location);
                final URL dummyURL = createURL(sb.toString());
                return fixPathTraversal(dummyURL).toString();
            } else if (location.startsWith("?")) {
                URL dummyURL = getURL(url, false, false, false);
                final String query = location.substring(1);
                if (StringUtils.isEmpty(query)) {
                    return createURL(dummyURL.toString()).toString();
                } else {
                    final StringBuilder sb = new StringBuilder();
                    sb.append(dummyURL.toString());
                    if (StringUtils.isEmpty(dummyURL.getPath())) {
                        sb.append("/");
                    }
                    sb.append(location);
                    dummyURL = createURL(sb.toString());
                    return dummyURL.toString();
                }
            } else if (location.startsWith("&")) {
                final String query = location.substring(1);
                if (StringUtils.isEmpty(query)) {
                    final URL dummyURL = getURL(url, true, false, false);
                    return createURL(dummyURL.toString()).toString();
                } else {
                    URL dummyURL = getURL(url, false, false, false);
                    final StringBuilder sb = new StringBuilder();
                    sb.append(dummyURL.toString());
                    if (StringUtils.isEmpty(dummyURL.getPath())) {
                        sb.append("/");
                    }
                    sb.append("?");
                    if (StringUtils.isNotEmpty(url.getQuery())) {
                        sb.append(url.getQuery());
                        if (!url.getQuery().endsWith("&")) {
                            sb.append("&");
                        }
                    }
                    sb.append(query);
                    dummyURL = createURL(sb.toString());
                    return dummyURL.toString();
                }
            } else if (location.startsWith("#") || StringUtils.isEmpty(location)) {
                // ignore empty location or anchor
                return url.toString();
            } else {
                final URL dummyURL = createURL(getBaseURL(url) + location);
                return fixPathTraversal(dummyURL).toString();
            }
        } catch (MalformedURLException e) {
            throw new WTFException("FIXME:location=" + location, e);
        }
    }

    public static URL fixPathTraversal(final URL url) throws MalformedURLException {
        if (url != null && (StringUtils.contains(url.getPath(), "../") || StringUtils.contains(url.getPath(), "./"))) {
            final String pathParts[] = url.getPath().split("/");
            for (int i = 0; i < pathParts.length; i++) {
                if (".".equals(pathParts[i])) {
                    pathParts[i] = null;
                } else if ("..".equals(pathParts[i])) {
                    if (i > 0) {
                        int j = i - 1;
                        while (true && j > 0) {
                            if (pathParts[j] != null) {
                                pathParts[j] = null;
                                break;
                            }
                            j--;
                        }
                    }
                    pathParts[i] = null;
                } else if (i > 0 && pathParts[i].length() == 0) {
                    pathParts[i] = "/";
                }
            }
            final StringBuilder sb = new StringBuilder();
            sb.append(url.getProtocol());
            sb.append("://");
            if (url.getUserInfo() != null) {
                sb.append(url.getUserInfo());
                sb.append("@");
            }
            sb.append(url.getHost());
            if (url.getPort() != -1) {
                sb.append(":");
                sb.append(url.getPort());
            }
            sb.append("/");
            for (int i = 0; i < pathParts.length; i++) {
                final String pathPart = pathParts[i];
                if (pathPart != null) {
                    if (pathPart.length() > 0 && !"/".equals(pathPart)) {
                        sb.append(pathPart);
                        if (i != pathParts.length - 1 && '/' != sb.charAt(sb.length() - 1)) {
                            sb.append("/");
                        }
                    }
                }
            }
            if (url.getQuery() != null) {
                sb.append("?");
                sb.append(url.getQuery());
            }
            if (url.getRef() != null) {
                sb.append("#");
                sb.append(url.getRef());
            }
            return createURL(sb.toString());
        }
        return url;
    }

    public static String getBaseURL(final URL url) throws MalformedURLException {
        final URL baseURI = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getPath());
        final String base;
        if (baseURI.getPath() != null) {
            base = new Regex(baseURI.toString(), "(https?://.+)/").getMatch(0);
        } else {
            base = baseURI.toString();
        }
        if (base.endsWith("/")) {
            return base;
        } else {
            return base + "/";
        }
    }

    public static URL getURL(final URL url, final boolean includeQuery, final boolean includeUserInfo, final boolean includeRef) throws MalformedURLException {
        final boolean modifyQuery = includeQuery == false && url.getQuery() != null;
        final boolean modifyUserInfo = includeUserInfo == false && url.getUserInfo() != null;
        final boolean modifyRef = includeRef == false && url.getRef() != null;
        if (!modifyQuery && !modifyUserInfo && !modifyRef) {
            return url;
        } else {
            final StringBuilder sb = new StringBuilder();
            sb.append(url.getProtocol());
            sb.append("://");
            if (includeUserInfo && url.getUserInfo() != null) {
                sb.append(url.getUserInfo());
                sb.append("@");
            }
            sb.append(url.getHost());
            if (url.getPort() != -1) {
                sb.append(":");
                sb.append(url.getPort());
            }
            if (!StringUtils.isEmpty(url.getPath())) {
                sb.append(url.getPath());
            } else {
                sb.append("/");
            }
            if (includeQuery && url.getQuery() != null) {
                sb.append("?");
                sb.append(url.getQuery());
            }
            if (includeRef && url.getRef() != null) {
                sb.append("#");
                sb.append(url.getRef());
            }
            return createURL(sb.toString());
        }
    }
}
