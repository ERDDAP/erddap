/*
 * $Id: PointCollection.java,v 1.6 2001/02/06 00:47:24 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package gov.noaa.pmel.sgt.dm;

import gov.noaa.pmel.util.SoTRange;
import java.util.Enumeration;

/**
 * <code>PointCollection</code> is an extension to <code>Vector</code> designed to hold <code>
 * SGTPoint</code> objects.
 *
 * @author Donald Denbo
 * @version $Revision: 1.6 $, $Date: 2001/02/06 00:47:24 $
 * @since 2.0
 * @see SGTData
 * @see SGTPoint
 * @see SGTLine
 * @see SGTGrid
 * @see SGTVector
 */
public class PointCollection extends Collection {
  private SoTRange xRange_ = null;
  private SoTRange yRange_ = null;
  private int colLen_ = 0;

  /**
   * @link aggregation
   * @clientRole x
   */
  private SGTMetaData xMetaData_;

  /**
   * @link aggregation
   * @clientRole y
   */
  private SGTMetaData yMetaData_;

  /** Default consturctor */
  public PointCollection() {
    this("");
  }

  public PointCollection(String title) {
    super(title);
  }

  public PointCollection(String title, int initialCapacity) {
    super(title, initialCapacity);
  }

  public PointCollection(String title, int initialCapacity, int increment) {
    super(title, initialCapacity, increment);
  }

  /**
   * Create a copy.
   *
   * @see SGTData
   */
  @Override
  public SGTData copy() {
    PointCollection newCollection;
    newCollection = (PointCollection) clone();
    return (SGTData) newCollection;
  }

  @Override
  public SoTRange getXRange() {
    computeRange();
    return xRange_.copy();
  }

  @Override
  public SoTRange getYRange() {
    computeRange();
    return yRange_.copy();
  }

  private void computeRange() {
    if (colLen_ == size()) return;
    colLen_ = size();
    double xmin = Double.POSITIVE_INFINITY;
    double xmax = Double.NEGATIVE_INFINITY;
    double ymin = xmin;
    double ymax = xmax;
    double ptx, pty;

    int count = 0;
    Enumeration e = elements();
    while (e.hasMoreElements()) {
      Object obj = e.nextElement();
      if (obj instanceof SGTPoint) {
        SGTPoint pt = (SGTPoint) obj;
        ptx = pt.getX();
        pty = pt.getY();
        if (!(Double.isNaN(ptx) || Double.isNaN(pty))) {
          xmin = Math.min(xmin, ptx);
          xmax = Math.max(xmax, ptx);
          ymin = Math.min(ymin, pty);
          ymax = Math.max(ymax, pty);
          count++;
        }
      }
    }
    if (count == 0) {
      xRange_ = new SoTRange.Double(Double.NaN, Double.NaN);
      yRange_ = new SoTRange.Double(Double.NaN, Double.NaN);
    } else {
      xRange_ = new SoTRange.Double(xmin, xmax);
      yRange_ = new SoTRange.Double(ymin, ymax);
    }
  }

  @Override
  public SGTMetaData getXMetaData() {
    return xMetaData_;
  }

  /** Set the <code>SGTMetaData</code> associated with the x axis. */
  public void setXMetaData(SGTMetaData xMetaData) {
    xMetaData_ = xMetaData;
  }

  @Override
  public SGTMetaData getYMetaData() {
    return yMetaData_;
  }

  /** Set the <code>SGTMetaData</code> associated with the y axis. */
  public void setYMetaData(SGTMetaData yMetaData) {
    yMetaData_ = yMetaData;
  }
}
