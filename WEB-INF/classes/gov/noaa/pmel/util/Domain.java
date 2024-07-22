/*
 * $Id: Domain.java,v 1.4 2003/08/22 23:02:40 dwd Exp $
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
 * <code>GraphDomain</code> contains the X and Y ranges in user units.
 *
 * @author Donald Denbo
 * @version $Revision: 1.4 $, $Date: 2003/08/22 23:02:40 $
 * @since sgt 2.0
 */
public class Domain implements java.io.Serializable {
  Range2D xRange_ = null;
  Range2D yRange_ = null;
  TimeRange tRange_ = null;
  boolean xTime_ = false;
  boolean yTime_ = false;
  boolean xReversed_ = false;
  boolean yReversed_ = false;

  /** Default constructor. */
  public Domain() {}

  /**
   * Create a copy of a <code>Domain</code>. The references for the ranges are copied, not the
   * objects.
   */
  public Domain(Domain domain) {
    if (domain.isXTime()) {
      tRange_ = domain.getTimeRange();
    } else {
      xRange_ = domain.getXRange();
    }
    if (domain.isYTime()) {
      tRange_ = domain.getTimeRange();
    } else {
      yRange_ = domain.getYRange();
    }
    xReversed_ = domain.isXReversed();
    yReversed_ = domain.isYReversed();
  }

  public Domain(Range2D xRange, Range2D yRange) {
    xRange_ = xRange;
    yRange_ = yRange;
  }

  public Domain(TimeRange tRange, Range2D yRange) {
    tRange_ = tRange;
    yRange_ = yRange;
    xTime_ = true;
  }

  public Domain(Range2D xRange, TimeRange tRange) {
    xRange_ = xRange;
    tRange_ = tRange;
    yTime_ = true;
  }

  /**
   * @since sgt 3.0
   */
  public Domain(Range2D xRange, Range2D yRange, boolean xRev, boolean yRev) {
    xRange_ = xRange;
    yRange_ = yRange;
    xReversed_ = xRev;
    yReversed_ = yRev;
  }

  /**
   * @since sgt 3.0
   */
  public Domain(TimeRange tRange, Range2D yRange, boolean xRev, boolean yRev) {
    tRange_ = tRange;
    yRange_ = yRange;
    xTime_ = true;
    xReversed_ = xRev;
    yReversed_ = yRev;
  }

  /**
   * @since sgt 3.0
   */
  public Domain(Range2D xRange, TimeRange tRange, boolean xRev, boolean yRev) {
    xRange_ = xRange;
    tRange_ = tRange;
    yTime_ = true;
    xReversed_ = xRev;
    yReversed_ = yRev;
  }

  /** Set the x range. */
  public void setXRange(Range2D xRange) {
    xTime_ = false;
    xRange_ = xRange;
  }

  /** Set the x range as time. */
  public void setXRange(TimeRange tRange) {
    xTime_ = true;
    tRange_ = tRange;
  }

  /** Get the x range. */
  public Range2D getXRange() {
    return xRange_;
  }

  /** Set the y range */
  public void setYRange(Range2D yRange) {
    yTime_ = false;
    yRange_ = yRange;
  }

  /** Set the yrange as time. */
  public void setYRange(TimeRange tRange) {
    yTime_ = true;
    tRange_ = tRange;
  }

  /** Get the y range. */
  public Range2D getYRange() {
    return yRange_;
  }

  /** Get the time range */
  public TimeRange getTimeRange() {
    return tRange_;
  }

  /** Test if x range is time. */
  public boolean isXTime() {
    return xTime_;
  }

  /** Test if y range is time. */
  public boolean isYTime() {
    return yTime_;
  }

  /**
   * @since sgt 3.0
   */
  public void setYReversed(boolean rev) {
    yReversed_ = rev;
  }

  /**
   * @since sgt 3.0
   */
  public boolean isYReversed() {
    return yReversed_;
  }

  /**
   * @since sgt 3.0
   */
  public void setXReversed(boolean rev) {
    xReversed_ = rev;
  }

  /**
   * @since sgt 3.0
   */
  public boolean isXReversed() {
    return xReversed_;
  }

  /** Tests for equality of <code>Domain</code>s. Both ranges must be equal. */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Domain)) {
      return false;
    }
    Domain d = (Domain) o;
    if (xTime_) {
      if (!d.isXTime()) return false;
      if (!tRange_.equals(d.getTimeRange())) return false;
    } else {
      if (d.isXTime()) return false;
      if (!xRange_.equals(d.getXRange())) return false;
    }
    if (yTime_) {
      if (!d.isYTime()) return false;
      if (!tRange_.equals(d.getTimeRange())) return false;
    } else {
      if (d.isYTime()) return false;
      if (!yRange_.equals(d.getYRange())) return false;
    }
    if (xReversed_ != d.isXReversed()) return false;
    if (yReversed_ != d.isYReversed()) return false;
    return true;
  }

  @Override
  public int hashCode() {
    int hash = 31 * 7;
    if (xTime_) {
      hash = 31 * hash + tRange_.hashCode();
    } else {
      hash = 31 * hash + xRange_.hashCode();
    }
    if (yTime_) {
      hash = 31 * hash + tRange_.hashCode();
    } else {
      hash = 31 * hash + yRange_.hashCode();
    }
    hash = 31 * hash + (xReversed_ ? 1 : 0) + (yReversed_ ? 2 : 0);
    return hash;
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer(100);
    buf.append("x=");
    if (xTime_) {
      buf.append(tRange_).append(",y=");
    } else {
      buf.append(xRange_).append(",y=");
    }
    if (yTime_) {
      buf.append(tRange_);
    } else {
      buf.append(yRange_);
    }
    buf.append(", xRev=").append(xReversed_);
    buf.append(", yRev=").append(yReversed_);
    return buf.toString();
  }
}
