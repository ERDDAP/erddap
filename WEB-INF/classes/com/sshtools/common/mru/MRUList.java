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
package com.sshtools.common.mru;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.15 $
 */
public class MRUList extends java.util.Vector {
    private static Log log = LogFactory.getLog(MRUList.class);
    private static final String MRU_LIST_ELEMENT = "MRUList";
    private static final String FILE_ELEMENT = "File";
    private String currentElement = null;

    /**
* Creates a new MRUList object.
*/
    public MRUList() {
        super();
    }

    /**
* Creates a new MRUList object.
*
* @param in
*
* @throws SAXException
* @throws ParserConfigurationException
* @throws IOException
*/
    public MRUList(InputStream in)
        throws SAXException, ParserConfigurationException, IOException {
        this();
        reload(in);
    }

    /**
*
*
* @param in
*
* @throws SAXException
* @throws ParserConfigurationException
* @throws IOException
*/
    public void reload(InputStream in)
        throws SAXException, ParserConfigurationException, IOException {
        SAXParserFactory saxFactory = SAXParserFactory.newInstance();
        SAXParser saxParser = saxFactory.newSAXParser();
        saxParser.parse(in, new MRUSAXHandler());
    }

    /**
*
*
* @return
*/
    public String toString() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
        xml += ("<!-- Most recently used -->\n<" + MRU_LIST_ELEMENT + ">\n");

        Iterator it = iterator();
        File file = null;

        while (it.hasNext()) {
            file = (File) it.next();
            xml += ("   " + "<" + FILE_ELEMENT + ">" + file.getAbsolutePath() +
            "</" + FILE_ELEMENT + ">\n");
        }

        xml += ("</" + MRU_LIST_ELEMENT + ">");

        return xml;
    }

    private class MRUSAXHandler extends DefaultHandler {
        private String MRU_LIST_ELEMENT = "MRUList";
        private String FILE_ELEMENT = "File";
        private File currentFile = null;
        private Stack tags = new Stack();

        public void startElement(String uri, String localName, String qname,
            Attributes attrs) throws SAXException {
            ElementWrapper currentElement = (tags.size() == 0) ? null
                                                               : (ElementWrapper) tags.peek();

            if (currentElement == null) {
                if (!qname.equals(MRU_LIST_ELEMENT)) {
                    throw new SAXException("Unexpected root element <" + qname +
                        ">");
                }
            } else {
                if (currentElement.element.equals(MRU_LIST_ELEMENT)) {
                    if (qname.equals(FILE_ELEMENT)) {
                    } else {
                        throw new SAXException("Unexpected element <" + qname +
                            ">");
                    }
                } else {
                    throw new SAXException("Unexpected element <" + qname +
                        ">");
                }
            }

            ElementWrapper w = new ElementWrapper(qname);
            tags.push(w);
        }

        public void characters(char[] ch, int start, int len)
            throws SAXException {
            ElementWrapper currentElement = (tags.size() == 0) ? null
                                                               : (ElementWrapper) tags.peek();

            if (currentElement != null) {
                currentElement.text.append(new String(ch, start, len));
            } else {
                throw new SAXException("Unexpected text at " + start + " for " +
                    len);
            }
        }

        public void endElement(String uri, String localName, String qname)
            throws SAXException {
            ElementWrapper currentElement = (tags.size() == 0) ? null
                                                               : (ElementWrapper) tags.peek();

            if (currentElement != null) {
                if (!currentElement.element.equals(qname)) {
                    throw new SAXException("Unexpected end element found <" +
                        qname + ">");
                }

                if (currentElement.element.equals(FILE_ELEMENT)) {
                    MRUList.this.add(new File(currentElement.text.toString()));
                }

                tags.pop();
            }
        }
    }

    public class ElementWrapper {
        String element;
        StringBuffer text;

        ElementWrapper(String element) {
            this.element = element;
            text = new StringBuffer();
        }
    }
}
