// $Id: Debug.java,v 1.1 2001/10/24 22:51:42 ndp Exp $
/*
 * Copyright 1997-2000 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package dods.util;

import java.util.TreeMap;

/** A minimal implementation of a globally-accessible set of Debug flags. */
public class Debug {
  private static TreeMap<String, Boolean> map = new TreeMap<>();
  private static boolean debug = false, changed = true;

  public static boolean isSet(String flagName) {
    Object val;
    if (null == (val = map.get(flagName))) {
      if (debug) System.out.println("Debug.isSet new " + flagName);
      map.put(flagName, Boolean.FALSE);
      changed = true;
      return false;
    }

    return ((Boolean) val).booleanValue();
  }

  public static void set(String flagName, boolean value) {
    Object val;
    if (null == (val = map.get(flagName))) {
      changed = true;
    }
    map.put(flagName, Boolean.valueOf(value));
    if (debug) System.out.println("  Debug.set " + flagName + " " + value);
  }

  public static void clear() {
    map = new TreeMap<>();
  }
}

/**
 * $Log: Debug.java,v $ Revision 1.1 2001/10/24 22:51:42 ndp *** empty log message ***
 *
 * <p>Revision 1.1.1.1 2001/09/26 15:36:47 caron checkin beta1
 */
