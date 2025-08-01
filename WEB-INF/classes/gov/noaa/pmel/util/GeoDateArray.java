/*
 * $Id: GeoDateArray.java,v 1.5 2003/08/22 23:02:40 dwd Exp $
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

import java.io.Serializable;

/**
 * <code>GeoDateArray</code> creates an efficient storage of <code>GeoDate</code> objects. This is
 * accomplished by using an internal storage of <code>long</code> for the number of milliseconds
 * since January 1, 1970, 00:00:00 GMT.
 *
 * @author Donald Denbo
 * @version $Revision: 1.5 $, $Date: 2003/08/22 23:02:40 $
 * @since sgt 3.0
 */
public class GeoDateArray implements Serializable {
  private final long[] date_;

  /**
   * Construct a new <code>GeoDateArray</code> from an array of <code>GeoDate</code>s.
   *
   * @param dates an array of <code>GeoDate</code>s.
   */
  public GeoDateArray(GeoDate[] dates) {
    date_ = new long[dates.length];
    for (int i = 0; i < dates.length; i++) {
      if (!(dates[i] == null || dates[i].isMissing())) {
        date_[i] = dates[i].getTime();
      } else {
        date_[i] = Long.MAX_VALUE;
      }
    }
  }

  /**
   * Construct a new <code>GeoDateArray</code> from an array of <code>long</code>s that represent
   * the number of milliseconds since January 1, 1970, 00:00:00 GMT. Missing value for date is
   * <code>Long.MAX_VALUE</code>.
   *
   * @param dates an array of <code>long</code>s.
   */
  public GeoDateArray(long[] dates) {
    date_ = dates;
  }

  public long[] getTime() {
    return date_;
  }

  public GeoDate[] getGeoDate() {
    GeoDate[] gd = new GeoDate[date_.length];
    for (int i = 0; i < date_.length; i++) {
      gd[i] = new GeoDate(date_[i]);
    }
    return gd;
  }

  /** Get length of array. */
  public int getLength() {
    return date_.length;
  }
}
