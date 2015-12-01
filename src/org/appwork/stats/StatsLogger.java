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
 *     The intent is that the AppWork GmbH is able to provide their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 *     These terms apply to all files that have the [The Product] License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 *
 * === 3rd Party Licences ===
 *     Some parts of the [The Product] use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 *     to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header.
 *
 * === Definition: Commercial Usage ===
 *     If anybody or any organization is generating income (directly or indirectly) by using [The Product] or if there's any commercial interest or aspect in what you are doing, we consider this as a commercial usage.
 *     If your use-case is neither strictly private nor strictly educational, it is commercial. If you are unsure whether your use-case is commercial or not, consider it as commercial or contact us.
 * === Dual Licensing ===
 * === Commercial Usage ===
 *     If you want to use [The Product] in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 *     Contact AppWork for further details: <e-mail@appwork.org>
 * === Non-Commercial Usage ===
 *     If there is no commercial usage (see definition above), you may use [The Product] under the terms of the
 *     "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 *
 *     If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.stats;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.appwork.exceptions.WTFException;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.storage.JSonStorage;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.appwork.utils.net.SimpleHTTP;

public class StatsLogger {
    private ThreadPoolExecutor pool;
    private LogSource          logger;

    /**
     *
     */
    public StatsLogger() {
        pool = new ThreadPoolExecutor(8, 8, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(10240));
        logger = LoggerFactory.I().getLogger("StatsLogger");
    }

    public LogSource getLogger() {
        return logger;
    }

    public void setLogger(LogSource logger) {
        this.logger = logger;
    }

    public void logAsynch(final String db, final String path, final String ip, Info... infos) {
        logAsynch(db, path, ip, createMap(infos));

    }

    public void logAsynch(final String db, final String path, final String ip, final HashMap<String, String> infos) {

        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                logSync(db, path, ip, infos);
            }
        };
        try {
            pool.submit(runnable);
        } catch (final Throwable e) {
            onException(e);
            getLogger().log(e);

        }
    }

    /**
     * @param e
     */
    protected void onException(Throwable e) {
        // TODO Auto-generated method stub

    }

    public void logSync(String db, final String path, final String ip, Info... infos) {

        logSync(db, path, ip, createMap(infos));
    }

    /**
     * @param infos
     * @return
     */
    private HashMap<String, String> createMap(Info... infos) {
        if (infos == null || infos.length == 0) {
            return null;
        }
        HashMap<String, String> map = new HashMap<String, String>();

        for (Info i : infos) {
            if (map.containsKey(i.getKey())) {
                throw new WTFException("Key Dupe: " + i.getKey());
            }
            map.put(i.getKey(), i.getValue());
        }

        return map;
    }

    /**
     * @param ip
     * @param infos
     * @param path
     */
    public void logSync(String db, final String path, final String ip, final HashMap<String, String> infos) {
        final SimpleHTTP simple = new SimpleHTTP();
        try {
            simple.setConnectTimeout(20000);
            simple.setReadTimeout(20000);

            HashMap<String, String> map = new HashMap<String, String>();
            if (infos != null) {
                map.putAll(infos);
            }

            simple.getPage(new java.net.URL("http://stats.appwork.org/jcgi/event/track?" + URLEncode.encodeRFC2396(path) + "&" + URLEncode.encodeRFC2396(JSonStorage.serializeToJson(map).replaceAll("[\r\n]+", "")) + "&" + ip + "&" + URLEncode.encodeRFC2396(db)));
        } catch (final Throwable e) {
            getLogger().log(e);
            onException(e);
        } finally {
            try {
                simple.getConnection().disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    public void logAsynch(String db, String path, RemoteAPIRequest request, Info... infos) {
        logAsynch(db, path, request, createMap(infos));
    }

    /**
     * @param db
     * @param path
     * @param request
     * @param infos
     */
    public void logAsynch(String db, String path, RemoteAPIRequest request, HashMap<String, String> infos) {

        final List<String> ips = request.getRemoteAddresses();

        String ip = ips.get(0);
        if (ips.size() > 1) {
            ip = ips.get(1);
        }
        logAsynch(db, path, ip, infos);

    }

}
