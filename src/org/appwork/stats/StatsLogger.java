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
            getLogger().log(e);
        }
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
