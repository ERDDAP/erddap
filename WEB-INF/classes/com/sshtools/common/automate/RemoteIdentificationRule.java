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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.13 $
 */
public class RemoteIdentificationRule {
    private static HashSet allowedOperations = new HashSet();

    static {
        allowedOperations.add("startsWith");
        allowedOperations.add("contains");
    }

    private HashMap expressions = new HashMap();
    private int priority = 10;
    private String name;

    /**
*
*
* @param identification
*
* @return
*/
    public boolean testRule(String identification) {
        // Get the software version portion of the id string
        String svc = identification.substring(identification.lastIndexOf("-") +
                1);
        Iterator it = expressions.entrySet().iterator();
        Map.Entry entry;
        boolean pass = false;
        String operation;

        while (it.hasNext()) {
            entry = (Map.Entry) it.next();
            operation = (String) entry.getKey();

            if (operation.equals("startsWith")) {
                pass = svc.startsWith((String) entry.getValue());
            }

            if (operation.equals("contains")) {
                pass = (svc.indexOf((String) entry.getValue()) >= 0);
            }
        }

        return pass;
    }

    /**
*
*
* @param priority
*/
    protected void setPriority(int priority) {
        this.priority = priority;
    }

    /**
*
*
* @param operation
* @param value
*
* @throws UnsupportedRuleException
*/
    protected void addExpression(String operation, String value)
        throws UnsupportedRuleException {
        if (allowedOperations.contains(operation)) {
            expressions.put(operation, value);
        } else {
            throw new UnsupportedRuleException("The rule '" + operation +
                "' is not supported");
        }
    }

    /**
*
*
* @param name
*/
    protected void setName(String name) {
        this.name = name;
    }

    /**
*
*
* @return
*/
    public String getName() {
        return name;
    }

    /**
*
*
* @return
*/
    public int getPriority() {
        return priority;
    }
}
