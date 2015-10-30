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
package org.appwork.storage;

/* infoScoop OpenSource
 * Copyright (C) 2010 Beacon IT Inc.
 * from https://infoscoop.googlecode.com/svn-history/r629/branches/3.0/src/main/java/org/infoscoop/util/Xml2Json.java
 * changes: updated and adapted to appwork simplemapper
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 3
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 */

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.appwork.storage.simplejson.JSonArray;
import org.appwork.storage.simplejson.JSonNode;
import org.appwork.storage.simplejson.JSonObject;
import org.appwork.storage.simplejson.JSonValue;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * from https://infoscoop.googlecode.com/svn-history/r629/branches/3.0/src/main/
 * java/org/infoscoop/util/Xml2Json.java repeatable & keypath : 'tagname' : {
 * 'key' : {...}, 'key' : {...}, ...} repeatable : 'tagname' : [ {...}, {...},
 * ... ] keypath : 'key' : { ... } arrayPath : [[{...}], [{...}]]
 * 
 * @author a-kimura
 * 
 */
public class Xml2Json {
    public static String XML2JSON(String xml) throws Exception {
        try {
            String ret = new Xml2Json().xml2json(xml);
            return ret;
        } catch (Exception e) {
            throw e;
        }

    }

    public static void main(String[] args) {
        String xml = "<breakfast_menu aa=\"1\" ab=\"0.9\" ac=\"bla\">\n" + "<food>\n" + "<name>Belgian Waffles</name>\n" + "<price>$5.95</price>\n" + "<description>\n" + "Two of our famous Belgian Waffles with plenty of real maple syrup\n" + "</description>\n" + "<calories>650</calories>\n" + "</food>\n" + "<food>\n" + "<name>Strawberry Belgian Waffles</name>\n" + "<price>$7.95</price>\n" + "<description>\n" + "Light Belgian waffles covered with strawberries and whipped cream\n" + "</description>\n" + "<calories>900</calories>\n" + "</food>\n" + "</breakfast_menu>";
        ;

        try {
            Xml2Json xmler = new Xml2Json();
            // xmler.addArrayPath("/breakfast_menu/food");
            System.out.println(xmler.xml2json(xml));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private List   keyPaths           = new ArrayList();

    private Map    pathMaps           = new HashMap();

    private List   repeatables        = new ArrayList();

    private List   singles            = new ArrayList();

    private List   skips              = new ArrayList();

    private List   arrays             = new ArrayList();

    private Map    namespaceResolvers = new HashMap();

    private String basePath;

    public void addPathRule(String xpath, String keyAttrName, boolean isRepeatable, boolean isSingle) {
        if (keyAttrName != null) {
            keyPaths.add(xpath);
            pathMaps.put(xpath, keyAttrName);
        }
        if (isRepeatable) {
            repeatables.add(xpath);
        }
        if (isSingle) {
            singles.add(xpath);
        }
    }

    public void addSkipRule(String xpath) {
        skips.add(xpath);
    }

    public void addArrayPath(String xpath) {
        arrays.add(xpath);
    }

    public void addNamespaceResolver(String prefix, String uri) {
        namespaceResolvers.put(uri, prefix);
    }

    public JSonObject xml2jsonObj(NodeList nodes) throws Exception {
        this.basePath = null;
        if (nodes == null || nodes.getLength() == 0) { return null; }
        Node baseNode = nodes.item(0).getParentNode();
        if (baseNode == null) { return null; }
        this.basePath = getXPath((Element) baseNode);
        JSonObject json = new JSonObject();
        nodelist2json(json, nodes);
        return json;
    }

    public JSonObject xml2jsonObj(Element element) throws Exception {
        this.basePath = null;
        Node baseNode = element.getParentNode();
        if (baseNode != null && baseNode.getNodeType() == Node.ELEMENT_NODE) {
            this.basePath = getXPath((Element) baseNode);
        }
        JSonObject obj = (JSonObject) node2json(element);

        JSonObject ret = new JSonObject();
        ret.put(element.getNodeName(), obj);
        return ret;
    }

    public String xml2json(NodeList nodes) throws Exception {
        JSonObject obj = xml2jsonObj(nodes);
        if (obj == null) { return ""; }
        return obj.toString();
    }

    public String xml2json(Element element) throws Exception {
        JSonObject obj = xml2jsonObj(element);

        return obj.toString();
    }

    public String xml2json(String xml) throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        builder.setEntityResolver(new EntityResolver() {

            @Override
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                // TODO Auto-generated method stub
                return new InputSource(new StringReader(""));
            }
        });
        builder.setErrorHandler(new ErrorHandler() {

            @Override
            public void warning(SAXParseException exception) throws SAXException {
                // TODO Auto-generated method stub
                exception.printStackTrace();

            }

            @Override
            public void fatalError(SAXParseException exception) throws SAXException {
                // TODO Auto-generated method stub
                exception.printStackTrace();

            }

            @Override
            public void error(SAXParseException exception) throws SAXException {
                // TODO Auto-generated method stub
                exception.printStackTrace();
            }
        });
        Document doc = builder.parse(new InputSource(new StringReader(xml)));
        Element root = doc.getDocumentElement();
        return xml2json(root);
    }

    private JSonNode node2json(Element element) throws Exception {
        JSonObject json = new JSonObject();
        String xpath = getXPath(element);
        if (singles.contains(xpath)) {
            if (element.getFirstChild() != null) {
                return (text(element.getFirstChild().getNodeValue()));
            } else {
                return new JSonValue("");
            }
        }
        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            String name = attr.getNodeName();
            String value = attr.getNodeValue();
            json.put(name, new JSonValue(value));
        }
        NodeList childs = element.getChildNodes();
        nodelist2json(json, childs);
        if (json.size() == 1) { return (JSonNode) json.entrySet().iterator().next().getValue(); }
        return json;
    }

    /**
     * @param nodeValue
     * @return
     */
    private JSonValue text(String nodeValue) {
        try {
            return new JSonValue(Long.parseLong(nodeValue));
        } catch (Throwable e) {

        }

        try {
            return new JSonValue(Double.parseDouble(nodeValue));
        } catch (Throwable e) {

        }
        if ("true".equalsIgnoreCase(nodeValue.trim())) {
            return new JSonValue(true);
        } else if ("false".equalsIgnoreCase(nodeValue.trim())) { return new JSonValue(true); }

        return new JSonValue(nodeValue);
    }

    private void nodelist2json(JSonObject map, NodeList nodes) throws Exception {
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            switch (node.getNodeType()) {
            case Node.TEXT_NODE:
            case Node.CDATA_SECTION_NODE:
                String text = node.getNodeValue().trim();
                if (text.length() > 0) {
                    map.put("content", (text(node.getNodeValue())));
                }
                break;
            case Node.ELEMENT_NODE:
                Element childElm = (Element) node;
                String childXPath = getXPath(childElm);

                if (skips.contains(childXPath)) {
                    nodelist2json(map, childElm.getChildNodes());
                } else if (arrays.contains(childXPath)) {
                    JSonArray obj = (JSonArray) map.get(childElm.getNodeName());
                    if (obj == null) {
                        obj = new JSonArray();
                        map.put(childElm.getNodeName(), obj);
                    }
                    JSonArray array = new JSonArray();
                    NodeList childNodes = childElm.getChildNodes();
                    for (int j = 0; j < childNodes.getLength(); j++) {
                        Node child = childNodes.item(j);
                        // TODO need to support the other node types.
                        if (child.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        array.add(node2json((Element) child));
                    }
                    obj.add(array);
                } else {
                    String childName = childElm.getNodeName();
                    boolean isRepeatable = repeatables.contains(childXPath);
                    boolean hasKey = keyPaths.contains(childXPath);
                    if (isRepeatable && hasKey) {
                        JSonObject obj = (JSonObject) map.get(childName);
                        if (obj == null) {
                            obj = new JSonObject();
                            map.put(childName, obj);
                        }
                        String attrName = (String) pathMaps.get(childXPath);
                        String attrValue = childElm.getAttribute(attrName);
                        obj.put(attrValue, node2json(childElm));
                    } else if (isRepeatable && !hasKey) {
                        JSonArray obj = (JSonArray) map.get(childName);
                        if (obj == null) {
                            obj = new JSonArray();
                            map.put(childName, obj);
                        }
                        obj.add(node2json(childElm));
                    } else if (hasKey) {
                        String attrName = (String) pathMaps.get(childXPath);
                        String attrValue = childElm.getAttribute(attrName);
                        map.put(attrValue, node2json(childElm));
                    } else {
                        map.put(childName, node2json(childElm));
                    }
                }
                break;
            default:
                break;
            }
        }
    }

    private String getXPath(Element element) {
        if (element == null) { return null; }
        StringBuffer xpath = new StringBuffer();
        xpath.append("/");
        String uri = element.getNamespaceURI();
        String prefix = (String) namespaceResolvers.get(uri);
        if (prefix != null) {
            xpath.append(prefix).append(":");
        }
        xpath.append(getTagName(element));
        Element parent = element;
        try {
            while (true) {
                parent = (Element) parent.getParentNode();
                if (parent == null) {
                    break;
                }
                xpath.insert(0, getTagName(parent));
                uri = parent.getNamespaceURI();
                prefix = (String) namespaceResolvers.get(uri);
                if (prefix != null) {
                    xpath.insert(0, prefix + ":");
                }
                xpath.insert(0, "/");
            }
        } catch (ClassCastException e) {

        }
        String xpathStr = xpath.toString();
        if (this.basePath != null) {
            xpathStr = xpathStr.replaceFirst("^" + this.basePath, "");
        }
        return xpathStr;
    }

    private String getTagName(Element elem) {
        String name = elem.getLocalName();
        if (name == null) {
            name = elem.getNodeName();
        }
        return name;
    }

}