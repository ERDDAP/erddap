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
package com.sshtools.common.util;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.8 $
 */
public class Search {
    /**
*
*
* @param str
* @param query
*
* @return
*
* @throws IllegalArgumentException
*/
    public static boolean matchesWildcardQuery(String str, String query)
        throws IllegalArgumentException {
        int idx = query.indexOf("*");

        if (idx > -1) {
            // We have a wildcard search
            if ((idx > 0) && (idx < (query.length() - 1))) {
                throw new IllegalArgumentException(
                    "Wildcards not supported in middle of query string; use either 'searchtext*' or '*searchtext'");
            }

            if (idx == (query.length() - 1)) {
                return str.startsWith(query.substring(0, idx));
            } else {
                return str.endsWith(query.substring(idx + 1));
            }
        } else {
            if (str.equalsIgnoreCase(query)) {
                return true;
            }
        }

        return false;
    }
}
