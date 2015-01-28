/**
 * Copyright (c) 2009 - 2012 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.storage.simplejson.test
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.storage.simplejson.test;

import java.util.HashMap;

import org.appwork.storage.SimpleMapper;
import org.appwork.storage.TypeRef;
import org.appwork.storage.jackson.JacksonMapper;
import org.appwork.storage.simplejson.JSonFactory;
import org.appwork.storage.simplejson.JSonValue;
import org.appwork.storage.simplejson.ParserException;

/**
 * @author Thomas
 * 
 */
public class EscapeTest {
    public static void main(String[] args) {
        try {

            String str;
            JSonValue value = new JSonValue(str = "{\"target\":\"http:\\/\\/uploaded.net\\/register\",\"banner\":\"http:\\/\\/uploaded.net\\/img\\/e\\/jdownloader\\/en\\/jd-banner-2.png\"}");
            HashMap<String, Object> map = new JacksonMapper().stringToObject(str, TypeRef.HASHMAP);
            map = new SimpleMapper().stringToObject(str, TypeRef.HASHMAP);
            String toString = value.toString();
            JSonValue paresed;
            paresed = (JSonValue) new JSonFactory(toString).parse();
            System.out.println("Test OK: " + paresed.getValue().equals(value.getValue()));
        } catch (ParserException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
