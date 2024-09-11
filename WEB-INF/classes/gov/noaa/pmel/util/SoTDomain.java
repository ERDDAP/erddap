/*
 * $Id: SoTDomain.java,v 1.4 2003/08/22 23:02:40 dwd Exp $
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
 * <code>SoTDomain</code> contains the X and Y ranges of a domain in user units. These ranges are
 * <code>SoTRange</code> objects which can be either Space or Time.
 *
 * @author Donald Denbo
 * @version $Revision: 1.4 $, $Date: 2003/08/22 23:02:40 $
 * @since sgt 2.0
 */
public class SoTDomain implements java.io.Serializable {
  SoTRange xRange_ = null;
  SoTRange yRange_ = null;
  boolean xReversed_ = false;
  boolean yReversed_ = false;

  /** Default constructor. */
  public SoTDomain() {}

  /** Construct a <code>SoTDomain</code> from a <code>Domain</code>. */
  public SoTDomain(Domain domain) {
    if (domain.isXTime()) {
      //      xRange_ = new SoTRange.GeoDate(domain.getTimeRange());
      xRange_ = new SoTRange.Time(domain.getTimeRange());
    } else {
      xRange_ = new SoTRange.Double(domain.getXRange());
    }
    if (domain.isYTime()) {
      //      yRange_ = new SoTRange.GeoDate(domain.getTimeRange());
      yRange_ = new SoTRange.Time(domain.getTimeRange());
    } else {
      yRange_ = new SoTRange.Double(domain.getYRange());
    }
    xReversed_ = domain.isXReversed();
    yReversed_ = domain.isYReversed();
  }

  /** Constract a <code>SoTDomain</code> from a <code>SoTDomain</code> */
  public SoTDomain(SoTDomain domain) {
    xRange_ = domain.getXRange();
    yRange_ = domain.getYRange();
    xReversed_ = domain.isXReversed();
    yReversed_ = domain.isYReversed();
  }

  /** Construct a <code>SoTDomain</code> from <code>SoTRange</code>s. */
  public SoTDomain(SoTRange xRange, SoTRange yRange) {
    xRange_ = xRange;
    yRange_ = yRange;
    xReversed_ = false;
    yReversed_ = false;
  }

  /**
   * @since sgt 3.0
   */
  public SoTDomain(SoTRange xRange, SoTRange yRange, boolean xRev, boolean yRev) {
    xRange_ = xRange;
    yRange_ = yRange;
    xReversed_ = xRev;
    yReversed_ = yRev;
  }

  /** Set the x range */
  public void setXRange(SoTRange xRange) {
    xRange_ = xRange;
  }

  /** Get the x range */
  public SoTRange getXRange() {
    return xRange_;
  }

  /** Set the y range */
  public void setYRange(SoTRange yRange) {
    yRange_ = yRange;
  }

  /** Get the y range */
  public SoTRange getYRange() {
    return yRange_;
  }

  /** Test if the x range is temporal. */
  public boolean isXTime() {
    return xRange_.isTime();
  }

  /** Test if the y range is temporal */
  public boolean isYTime() {
    return yRange_.isTime();
  }

  /**
   * Get the center of the domain.
   *
   * @since sgt 3.0
   */
  public SoTPoint getCenter() {
    SoTValue xVal = null;
    SoTValue yVal = null;
    if (isXTime()) {
      xVal =
          new SoTValue.Time(
              (xRange_.getStart().getLongTime() + xRange_.getEnd().getLongTime()) / 2);
    } else {
      xVal =
          new SoTValue.Double(
              (((Number) xRange_.getStart().getObjectValue()).doubleValue()
                      + ((Number) xRange_.getEnd().getObjectValue()).doubleValue())
                  / 2.0);
    }
    if (isYTime()) {
      yVal =
          new SoTValue.Time(
              (yRange_.getStart().getLongTime() + yRange_.getEnd().getLongTime()) / 2);
    } else {
      yVal =
          new SoTValue.Double(
              (((Number) yRange_.getStart().getObjectValue()).doubleValue()
                      + ((Number) yRange_.getEnd().getObjectValue()).doubleValue())
                  / 2.0);
    }

    return new SoTPoint(xVal, yVal);
  }

  /** Test for equality. Both ranges must be equal for equality. */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SoTDomain)) {
      return false;
    }
    SoTDomain d = (SoTDomain) o;
    if (!xRange_.equals(d.getXRange())) return false;
    if (!yRange_.equals(d.getYRange())) return false;
    if (xReversed_ != d.isXReversed()) return false;
    if (yReversed_ != d.isYReversed()) return false;
    return true;
  }

  @Override
  public int hashCode() {
    int hash = 31 * 7 + xRange_.hashCode();
    hash = 31 * hash + yRange_.hashCode();
    hash = 31 * hash + (xReversed_ ? 1 : 0) + (yReversed_ ? 2 : 0);
    return hash;
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer(100);
    buf.append("x=");
    buf.append(xRange_).append(",y=");
    buf.append(yRange_);
    buf.append(", xRev=").append(xReversed_);
    buf.append(", yRev=").append(yReversed_);
    return buf.toString();
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
}
