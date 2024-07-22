/*
 * $Id: TimeRange.java,v 1.5 2003/08/22 23:02:40 dwd Exp $
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

import java.util.Objects;

/**
 * Contains minimum and maximum Time values.
 *
 * @author Donald Denbo
 * @version $Revision: 1.5 $, $Date: 2003/08/22 23:02:40 $
 * @since sgt 1.0
 *     <p>deprecated As of sgt 3.0, use {@link gov.noaa.pmel.util.SoTRange.Time SoTRange.Time}
 */
public class TimeRange implements java.io.Serializable {
  /** The range's first time */
  public GeoDate start;

  /** The range's last time */
  public GeoDate end;

  /** The range's time increment */
  public GeoDate delta;

  /** the Default constructor */
  public TimeRange() {}

  /**
   * Constructor
   *
   * @param tstart first time
   * @param tend last time
   */
  public TimeRange(GeoDate tstart, GeoDate tend) {
    this(tstart, tend, null);
  }

  public TimeRange(long start, long end) {
    this(new GeoDate(start), new GeoDate(end));
  }

  /**
   * Constructor
   *
   * @param tstart first time
   * @param tend last time
   * @param delta time increment
   */
  public TimeRange(GeoDate tstart, GeoDate tend, GeoDate tdelta) {
    this.start = tstart;
    this.end = tend;
    this.delta = tdelta;
  }

  public TimeRange(long start, long end, long delta) {
    this(new GeoDate(start), new GeoDate(end), new GeoDate(delta));
  }

  /**
   * Adds the <code>TimeRange</code> object to this <code>TimeRange</code>. The resulting <code>
   * TimeRange</code> is the smallest <code>TimeRange</code> that contains both the origial <code>
   * TimeRange</code> and the specified <code>TimeRange</code>.
   */
  public void add(TimeRange trange) {
    if (trange.start.before(start)) start = trange.start;
    if (trange.end.after(end)) end = trange.end;
  }

  /** Test for equality. The start, end, and delta must all be equal for equality. */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof TimeRange)) {
      return false;
    }
    TimeRange tr = (TimeRange) o;
    if (start != null && tr.start != null) {
      if (!start.equals(tr.start)) return false;
    } else {
      return false;
    }
    if (end != null && tr.end != null) {
      if (!end.equals(tr.end)) return false;
    } else {
      return false;
    }
    if (delta != null && tr.delta != null) {
      if (!delta.equals(tr.delta)) return false;
    } else {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end, delta);
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer(50);
    buf.append("[").append(start).append(";").append(end);
    if (delta == null) {
      buf.append("]");
    } else {
      buf.append(";").append(delta).append("]");
    }
    return buf.toString();
  }
}
