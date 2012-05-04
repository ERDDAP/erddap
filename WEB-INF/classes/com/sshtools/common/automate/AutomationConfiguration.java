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
package com.sshtools.common.automate;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;

import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.13 $
 */
public class AutomationConfiguration {
    private HashMap remoteIdentifications = new HashMap();

    /**
* Creates a new AutomationConfiguration object.
*
* @param in
*
* @throws IOException
* @throws SAXException
* @throws ParserConfigurationException
*/
    public AutomationConfiguration(InputStream in)
        throws IOException, SAXException, ParserConfigurationException {
        SAXParserFactory saxFactory = SAXParserFactory.newInstance();
        SAXParser saxParser = saxFactory.newSAXParser();
        saxParser.parse(in, new AutomationConfigurationSAXHandler());
    }

    /**
*
*
* @return
*/
    public Map getRemoteIdentifications() {
        return remoteIdentifications;
    }

    /**
*
*
* @param args
*/
    public static void main(String[] args) {
        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class AutomationConfigurationSAXHandler extends DefaultHandler {
        private String AUTOMATION_ELEMENT = "Automation";
        private String REMOTEID_ELEMENT = "RemoteIdentification";
        private String AUTHORIZEDKEYSFORMAT_ELEMENT = "AuthorizedKeysFormat";
        private String RULE_ELEMENT = "Rule";
        private String STARTSWITH_ATTRIBUTE = "startsWith";
        private String CONTAINS_ATTRIBUTE = "contains";
        private String DEFAULTNAME_ATTRIBUTE = "defaultName";
        private String NAME_ATTRIBUTE = "name";
        private String IMPLEMENTATION_ATTRIBUTE = "implementationClass";
        private String PRIORITY_ATTRIBUTE = "priority";
        private String DEFAULTPATH_ATTRIBUTE = "defaultPath";
        private String currentElement = null;
        private RemoteIdentification currentRID = null;

        public void startElement(String uri, String localName, String qname,
            Attributes attrs) throws SAXException {
            if (currentElement == null) {
                if (!qname.equals(AUTOMATION_ELEMENT)) {
                    throw new SAXException("Unexpected root element <" + qname +
                        ">");
                }
            } else {
                if (currentElement.equals(AUTOMATION_ELEMENT)) {
                    if (qname.equals(REMOTEID_ELEMENT)) {
                        String defaultName = attrs.getValue(DEFAULTNAME_ATTRIBUTE);

                        if (defaultName == null) {
                            throw new SAXException(DEFAULTNAME_ATTRIBUTE +
                                " attribute must be specified");
                        }

                        currentRID = new RemoteIdentification(defaultName);
                    } else {
                        throw new SAXException("Unexpected element <" + qname +
                            ">");
                    }
                } else if (currentElement.equals(REMOTEID_ELEMENT)) {
                    if (qname.equals(RULE_ELEMENT)) {
                        String startsWith = attrs.getValue(STARTSWITH_ATTRIBUTE);
                        String contains = attrs.getValue(CONTAINS_ATTRIBUTE);
                        String name = attrs.getValue(NAME_ATTRIBUTE);
                        String priority = attrs.getValue(PRIORITY_ATTRIBUTE);

                        try {
                            RemoteIdentificationRule rule = new RemoteIdentificationRule();

                            if (startsWith != null) {
                                rule.addExpression(STARTSWITH_ATTRIBUTE,
                                    startsWith);
                            }

                            if (contains != null) {
                                rule.addExpression(CONTAINS_ATTRIBUTE, contains);
                            }

                            if (name != null) {
                                rule.setName(name);
                            }

                            try {
                                if (priority != null) {
                                    rule.setPriority(Integer.parseInt(priority));
                                }
                            } catch (NumberFormatException ex1) {
                                throw new SAXException(
                                    "Failed to parse priority value! value=" +
                                    priority);
                            }

                            currentRID.addRule(rule);
                        } catch (UnsupportedRuleException ure) {
                            throw new SAXException(ure.getMessage());
                        }
                    } else if (qname.equals(AUTHORIZEDKEYSFORMAT_ELEMENT)) {
                        String implementationClass = attrs.getValue(IMPLEMENTATION_ATTRIBUTE);
                        String defaultPath = attrs.getValue(DEFAULTPATH_ATTRIBUTE);

                        if (implementationClass == null) {
                            throw new SAXException(IMPLEMENTATION_ATTRIBUTE +
                                " attribute is required");
                        }

                        try {
                            currentRID.setAuthorizedKeysFormat(Class.forName(
                                    implementationClass));
                            currentRID.setAuthorizedKeysDefaultPath(defaultPath);
                        } catch (ClassNotFoundException ex) {
                            throw new SAXException(ex.getMessage());
                        }
                    } else {
                        throw new SAXException("Unexpected element <" + qname +
                            ">");
                    }
                } else {
                    throw new SAXException("Unexpected element <" + qname +
                        ">");
                }
            }

            currentElement = qname;
        }

        public void endElement(String uri, String localName, String qname)
            throws SAXException {
            if (currentElement != null) {
                if (!currentElement.equals(qname)) {
                    throw new SAXException("Unexpected end element found <" +
                        qname + ">");
                }

                if (currentElement.equals(REMOTEID_ELEMENT)) {
                    if (currentRID.getRules().size() > 0) {
                        remoteIdentifications.put(currentRID.getDefaultName(),
                            currentRID);
                    } else {
                        throw new SAXException("<" + REMOTEID_ELEMENT + "> " +
                            " requires at least one child <" + RULE_ELEMENT +
                            "> element!");
                    }

                    currentElement = AUTOMATION_ELEMENT;
                } else if (currentElement.equals(RULE_ELEMENT)) {
                    currentElement = REMOTEID_ELEMENT;
                } else if (currentElement.equals(AUTHORIZEDKEYSFORMAT_ELEMENT)) {
                    currentElement = REMOTEID_ELEMENT;
                } else if (currentElement.equals(AUTOMATION_ELEMENT)) {
                    currentElement = null;
                } else {
                    throw new SAXException("Unexpected end element <" + qname +
                        ">");
                }
            }
        }
    }
}
