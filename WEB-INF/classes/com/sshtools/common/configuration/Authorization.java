/*
 *  SSHTools - Java SSH2 API
 *
 *  Copyright (C) 2002-2003 Lee David Painter and Contributors.
 *
 *  Contributions made by:
 *
 *  Brett Smith
 *  Richard Pernavas
 *  Erwin Bolwidt
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Library General Public License
 *  as published by the Free Software Foundation; either version 2 of
 *  the License, or (at your option) any later version.
 *
 *  You may also distribute it and/or modify it under the terms of the
 *  Apache style J2SSH Software License. A copy of which should have
 *  been provided with the distribution.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  License document supplied with your distribution for more details.
 *
 */
package com.sshtools.common.configuration;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import java.io.*;

import java.util.*;

import javax.xml.parsers.*;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.12 $
 */
public class Authorization extends DefaultHandler {
    private static final String AUTHORIZEDKEYS_ELEMENT = "AuthorizedKeys";
    private static final String KEY_ELEMENT = "Key";
    private ArrayList authorizedKeys = new ArrayList();

    /**
* Creates a new Authorization object.
*
* @param in
*
* @throws SAXException
* @throws ParserConfigurationException
* @throws IOException
*/
    public Authorization(InputStream in)
        throws SAXException, ParserConfigurationException, IOException {
        SAXParserFactory saxFactory = SAXParserFactory.newInstance();
        SAXParser saxParser = saxFactory.newSAXParser();
        authorizedKeys.clear();
        saxParser.parse(in, new AuthorizedKeysSAXHandler());
    }

    /**
* Creates a new Authorization object.
*/
    public Authorization() {
        // Creates an empty authorization file
    }

    /**
*
*
* @return
*/
    public List getAuthorizedKeys() {
        return (List) authorizedKeys.clone();
    }

    /**
*
*
* @param keyfile
*/
    public void addKey(String keyfile) {
        authorizedKeys.add(keyfile);
    }

    /**
*
*
* @return
*/
    public String toString() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
        xml += "<!-- SSHTools Public Key Authorization File -->\n";
        xml += ("<" + AUTHORIZEDKEYS_ELEMENT + ">\n");
        xml += "<!-- Enter authorized public key elements here -->\n";

        Iterator it = authorizedKeys.iterator();

        while (it.hasNext()) {
            xml += ("   <" + KEY_ELEMENT + ">" + it.next().toString() + "</" +
            KEY_ELEMENT + ">\n");
        }

        xml += ("</" + AUTHORIZEDKEYS_ELEMENT + ">");

        return xml;
    }

    class AuthorizedKeysSAXHandler extends DefaultHandler {
        private String currentElement = null;

        public void startElement(String uri, String localName, String qname,
            Attributes attrs) throws SAXException {
            if (currentElement == null) {
                if (!qname.equals(AUTHORIZEDKEYS_ELEMENT)) {
                    throw new SAXException("Unexpected root element " + qname);
                }
            } else {
                if (currentElement.equals(AUTHORIZEDKEYS_ELEMENT)) {
                    if (!qname.equals(KEY_ELEMENT)) {
                        throw new SAXException("Unexpected element " + qname);
                    }
                } else {
                    throw new SAXException("Unexpected element " + qname);
                }
            }

            currentElement = qname;
        }

        public void characters(char[] ch, int start, int length)
            throws SAXException {
            if (currentElement != null) {
                if (currentElement.equals(KEY_ELEMENT)) {
                    String key = new String(ch, start, length);
                    authorizedKeys.add(key);
                }
            }
        }

        public void endElement(String uri, String localName, String qname)
            throws SAXException {
            if (currentElement != null) {
                if (!currentElement.equals(qname)) {
                    throw new SAXException("Unexpected end element found " +
                        qname);
                }

                if (currentElement.equals(KEY_ELEMENT)) {
                    currentElement = AUTHORIZEDKEYS_ELEMENT;
                } else if (currentElement.equals(AUTHORIZEDKEYS_ELEMENT)) {
                    currentElement = null;
                } else {
                    throw new SAXException("Unexpected end element " + qname);
                }
            }
        }
    }
}
