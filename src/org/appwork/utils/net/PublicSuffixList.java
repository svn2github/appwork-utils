/**
 * Copyright (c) 2009 - 2014 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils.net
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.utils.Application;
import org.appwork.utils.Regex;

/**
 * @author daniel
 * 
 */
public class PublicSuffixList {

    private static PublicSuffixList INSTANCE;
    static {
        try {
            PublicSuffixList.INSTANCE = new PublicSuffixList();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static PublicSuffixList getInstance() {
        return PublicSuffixList.INSTANCE;
    }

    /**
     * Implementation for https://publicsuffix.org/list/
     */

    private final Map<String, List<String>> map;

    public static void main(String[] args) throws Throwable {
        PublicSuffixList test = new PublicSuffixList();
        System.out.println(test.getTopLevelDomain("test.teledata.mz"));
        System.out.println(test.getDomain("test.teledata.mz"));
        System.out.println(test.getTopLevelDomain("jdownloader.org"));
        System.out.println(test.getDomain("jdownloader.org"));
    }

    public PublicSuffixList(URL publicSuffixList) throws IOException {
        this.map = this.parse(publicSuffixList);
    }

    private PublicSuffixList() throws IOException {
        this(Application.getRessourceURL("org/appwork/utils/net/effective_tld_names.dat"));
    }

    public String getDomain(String fullDomain) {
        final String topLeveLDomain = this.getTopLevelDomain(fullDomain);
        if (topLeveLDomain != null) {
            final String pattern = "([^\\.]+\\." + Pattern.quote(topLeveLDomain) + ")";
            final String domain = new Regex(fullDomain, pattern).getMatch(0);
            return domain;
        }
        return null;
    }

    public String getTopLevelDomain(String fullDomain) {
        if (fullDomain != null) {
            final int tldIndex = fullDomain.lastIndexOf('.');
            if (tldIndex > 0 && tldIndex + 1 < fullDomain.length()) {
                final String tld = fullDomain.substring(tldIndex + 1);
                final List<String> list = this.map.get(tld);
                if (list != null) {
                    if (list.size() == 0) { return tld; }
                    final List<String> hits = new ArrayList<String>();
                    hits.add(tld);
                    for (String item : list) {
                        if (item.startsWith("*")) {
                            final String pattern = "([^\\.]+" + Pattern.quote(item.substring(1)) + ")";
                            final String domain = new Regex(fullDomain, pattern).getMatch(0);
                            if (domain != null) {
                                hits.add(domain);
                            }
                        } else if (item.startsWith("!") && fullDomain.contains(item.substring(1))) {
                            return item.substring(item.indexOf('.') + 1);
                        } else if (fullDomain.contains(item)) {
                            hits.add(item);
                        }
                    }
                    Collections.sort(hits, new Comparator<String>() {

                        public int compare(int x, int y) {
                            return x < y ? 1 : x == y ? 0 : -1;
                        }

                        @Override
                        public int compare(String o1, String o2) {
                            return this.compare(o1.length(), o2.length());
                        }

                    });
                    return hits.get(0);

                }
            }
        }
        return null;
    }

    protected Map<String, List<String>> parse(URL publicSuffixList) throws IOException {
        final HashMap<String, List<String>> map = new HashMap<String, List<String>>();
        if (publicSuffixList == null) { return map; }
        final InputStream is = publicSuffixList.openStream();
        try {

            final BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            final List<String> emptyList = new ArrayList<String>(0);
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("//") && line.length() > 0) {
                    final String tld;
                    final int tldIndex = line.lastIndexOf('.');
                    if (tldIndex == -1) {
                        tld = line;
                    } else {
                        tld = line.substring(tldIndex + 1);
                    }
                    List<String> list = map.get(tld);
                    if (list == null) {
                        list = emptyList;
                        map.put(tld, list);
                    }
                    if (tldIndex > 0) {
                        if (list == emptyList) {
                            list = new ArrayList<String>();
                            map.put(tld, list);
                        }
                        list.add(line);
                    }
                }
            }
            for (List<String> list : map.values()) {
                if (list != emptyList) {
                    ((ArrayList) list).trimToSize();
                }
            }
            return map;
        } finally {
            is.close();
        }
    }
}
