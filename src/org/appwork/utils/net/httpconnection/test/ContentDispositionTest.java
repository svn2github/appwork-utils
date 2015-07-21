/**
 * Copyright (c) 2009 - 2015 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 *
 * This file is part of org.appwork.utils.net.httpconnection.test
 *
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.net.httpconnection.test;

import java.util.ArrayList;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPConnectionUtils;

/**
 * @author daniel
 *
 */
public class ContentDispositionTest {

    /**
     * http://greenbytes.de/tech/tc2231/
     */
    public static void main(String[] args) {
        final ArrayList<String[]> tests = new ArrayList<String[]>();

        tests.add(new String[] { "filename=test.zip", "test.zip" });
        tests.add(new String[] { "filename ='test.zip'", "test.zip" });
        tests.add(new String[] { "filename= test.zip", "test.zip" });
        tests.add(new String[] { " filename=test.zip", "test.zip" });
        tests.add(new String[] { ";filename =test.zip", "test.zip" });
        tests.add(new String[] { "filename= test.zip", "test.zip" });

        tests.add(new String[] { "attachment;filename*=UTF-8''Test%20Test%20%282008%29.rar;filename=\"Test Test (2008).rar\"; ", "Test Test (2008).rar" });
        tests.add(new String[] { "attachment;filename*=UTF-8''TEST (2015).rar;filename=\"TEST (2015).rar\";", "TEST (2015).rar" });
        tests.add(new String[] { "attachment; filename==?UTF-8?B?dGVzdC56aXA=?=", "test.zip" });
        tests.add(new String[] { "attachment; filename=\"test.zip\"; creation-date=\"Thu, 27 Nov 2014 10:17:31 +0000\"; modification-date=\"Thu, 27 Nov 2014 10:17:31 +0000\"", "test.zip" });
        tests.add(new String[] { "attachment; filename=\"foo.html\"", "foo.html" });
        tests.add(new String[] { "attachment; filename=\"0000000000111111111122222\"", "0000000000111111111122222" });
        tests.add(new String[] { "attachment; filename=\"00000000001111111111222222222233333\"", "00000000001111111111222222222233333" });
        // tests.add(new String[] { "attachment; filename=\"f\\oo.html\"", "foo.html" });
        tests.add(new String[] { "attachment; filename=\"Here's a semicolon;.html\"", "Here's a semicolon;.html" });
        tests.add(new String[] { "attachment; foo=\"bar\"; filename=\"foo.html\"", "foo.html" });
        tests.add(new String[] { "attachment; foo=\"\\\"\\\\\";filename=\"foo.html\"", "foo.html" });
        tests.add(new String[] { "attachment; FILENAME=\"foo.html\"", "foo.html" });
        tests.add(new String[] { "attachment; filename=foo.html", "foo.html" });
        tests.add(new String[] { "attachment; file_name=foo.html", "foo.html" });
        tests.add(new String[] { "attachment; name=foo.html", "foo.html" });
        tests.add(new String[] { "attachment; filename='foo.bar'", "foo.bar" });
        tests.add(new String[] { "attachment; filename=\"foo-ä.html\"", "foo-ä.html" });
        tests.add(new String[] { "attachment; filename=\"foo-%41.html\"", "foo-%41.html" });
        tests.add(new String[] { "attachment; filename=\"50%.html\"", "50%.html" });
        tests.add(new String[] { "attachment; name=\"foo-%41.html\"", "foo-%41.html" });
        tests.add(new String[] { "attachment; filename=\"ä-%41.html\"", "ä-%41.html" });
        tests.add(new String[] { "attachment; filename=\"foo-%c3%a4-%e2%82%ac.html\"", "foo-%c3%a4-%e2%82%ac.html" });
        tests.add(new String[] { "attachment; filename =\"foo.html\"", "foo.html" });
        tests.add(new String[] { "attachment; xfilename=foo.html", null });
        tests.add(new String[] { "attachment; filename=\"/foo.html\"", "/foo.html" });
        tests.add(new String[] { "attachment; filename=\"\\\\foo.html\"", "_foo.html" });

        tests.add(new String[] { "attachment; filename*=UTF-8''foo-a%cc%88.html; creation-date=\"Thu, 27 Nov 2014 10:17:31 +0000\"; modification-date=\"Thu, 27 Nov 2014 10:17:31 +0000\"", "foo-ä.html" });

        tests.add(new String[] { "attachment; filename*=iso-8859-1''foo-%E4.html", "foo-ä.html" });
        tests.add(new String[] { "attachment; filename*=UTF-8''foo-%c3%a4-%e2%82%ac.html", "foo-ä-€.html" });
        tests.add(new String[] { "attachment; filename*=''foo-%c3%a4-%e2%82%ac.html", null });
        tests.add(new String[] { "attachment; filename*=UTF-8''foo-a%cc%88.html", "foo-ä.html" });

        tests.add(new String[] { "attachment; filename*= UTF-8''foo-%c3%a4.html", "foo-ä.html" });
        tests.add(new String[] { "attachment; filename* =UTF-8''foo-%c3%a4.html", "foo-ä.html" });

        tests.add(new String[] { "attachment; filename*=UTF-8''A-%2541.html", "A-%41.html" });
        tests.add(new String[] { "attachment; filename*=UTF-8''%5cfoo.html", "_foo.html" });

        tests.add(new String[] { "attachment; filename=\"foo-ae.html\"; filename*=UTF-8''foo-%c3%a4.html", "foo-ä.html" });
        tests.add(new String[] { "attachment; filename*=UTF-8''foo-%c3%a4.html; filename=\"foo-ae.html\"", "foo-ä.html" });

        for (String test[] : tests) {
            final String result = HTTPConnectionUtils.getFileNameFromDispositionHeader(test[0]);
            if (!StringUtils.equals(result, test[1])) {
                throw new WTFException("Broken: " + test[0] + " should return: " + test[1] + " but returns: " + result);
            }
        }
    }
}
