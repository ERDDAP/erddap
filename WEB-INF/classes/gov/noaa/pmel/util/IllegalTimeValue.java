/*
 * $Id: IllegalTimeValue.java,v 1.2 2003/08/22 23:02:40 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package gov.noaa.pmel.util;

/**
 * The specified time was unreadable or illegal.
 *
 * @author Don Denbo
 * @version $Revision: 1.2 $, $Date: 2003/08/22 23:02:40 $
 */
public class IllegalTimeValue extends Exception {
  public IllegalTimeValue() {
    super();
  }

  public IllegalTimeValue(String s) {
    super(s);
  }
}
