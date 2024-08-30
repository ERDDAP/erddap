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
import java.util.Date;

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
  private long[] date_;

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
   * Construct a new <code>GeoDateArray</code> from an array of <code>Date</code>s.
   *
   * @param dates an array of <code>Date</code>s.
   */
  public GeoDateArray(Date[] dates) {
    date_ = new long[dates.length];
    for (int i = 0; i < dates.length; i++) {
      if (!(dates[i] == null)) {
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

  public long getTime(int index) {
    if (index < 0 || index >= date_.length) return Long.MAX_VALUE;
    return date_[index];
  }

  public GeoDate getGeoDate(int index) {
    if (index < 0 || index >= date_.length) return null;
    return new GeoDate(date_[index]);
  }

  public GeoDate[] getGeoDate() {
    GeoDate[] gd = new GeoDate[date_.length];
    for (int i = 0; i < date_.length; i++) {
      gd[i] = new GeoDate(date_[i]);
    }
    return gd;
  }

  /**
   * Time offset for reference <code>GeoDate</code>.
   *
   * @param ref reference <code>GeoDate</code>
   * @return offset in days
   */
  public double getOffset(int index, GeoDate ref) {
    if (index < 0 || index >= date_.length) return Double.NaN;
    return ((double) (date_[index] - ref.getTime())) / 86400000.0;
  }

  /**
   * Time offset for reference <code>GeoDate</code>.
   *
   * @param ref reference <code>GeoDate</code>
   * @return offset in days
   */
  public double[] getOffset(GeoDate ref) {
    long refgd = ref.getTime();
    double[] off = new double[date_.length];
    for (int i = 0; i < date_.length; i++) {
      off[i] = ((double) (date_[i] - refgd)) / 86400000.0;
    }
    return off;
  }

  /**
   * Time offset for reference <code>GeoDate</code>.
   *
   * @param ref reference <code>GeoDate</code>
   * @return offset in milliseconds
   */
  public long getOffsetTime(int index, GeoDate ref) {
    if (index < 0 || index >= date_.length) return Long.MAX_VALUE;
    return date_[index] - ref.getTime();
  }

  /**
   * Time offset for reference <code>GeoDate</code>.
   *
   * @param ref reference <code>GeoDate</code>
   * @return offset in milliseconds
   */
  public long[] getOffsetTime(GeoDate ref) {
    long refgd = ref.getTime();
    long[] off = new long[date_.length];
    for (int i = 0; i < date_.length; i++) {
      off[i] = date_[i] - refgd;
    }
    return off;
  }

  /** Add offset to all dates. */
  public void addOffset(long offset) {
    for (int i = 0; i < date_.length; i++) {
      date_[i] += offset;
    }
  }

  /** Add offset to single date. */
  public void addOffset(int index, long offset) {
    if (index < 0 || index >= date_.length) return;
    date_[index] += offset;
  }

  /** Get length of array. */
  public int getLength() {
    return date_.length;
  }
}
