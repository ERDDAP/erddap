/*
 * $Id: SineTransform.java,v 1.7 2003/08/22 23:02:32 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package gov.noaa.pmel.sgt;

import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.SoTRange;
import gov.noaa.pmel.util.SoTValue;
import gov.noaa.pmel.util.TimeRange;

/**
 * Performs a sine(x) transform on a cartesian axis.
 *
 * @author Donald Denbo
 * @version $Revision: 1.7 $, $Date: 2003/08/22 23:02:32 $
 * @since 2.x
 */
public class SineTransform extends AxisTransform implements Cloneable {
  public SineTransform() {
    super();
  }

  public SineTransform(double p1, double p2, double u1, double u2) {
    super(p1, p2, u1, u2);
  }

  public SineTransform(Range2D pr, Range2D ur) {
    super(pr, ur);
  }

  public SineTransform(double p1, double p2, GeoDate t1, GeoDate t2) {
    super(p1, p2, t1, t2);
  }

  public SineTransform(Range2D pr, TimeRange tr) {
    super(pr, tr);
  }

  public SineTransform(Range2D pr, SoTRange str) {
    super(pr, str);
  }

  @Override
  public AxisTransform copy() {
    SineTransform newTransform;
    try {
      newTransform = (SineTransform) clone();
    } catch (CloneNotSupportedException e) {
      newTransform = new SineTransform();
    }
    return (AxisTransform) newTransform;
  }

  @Override
  public double getTransP(double u) {
    throw new MethodNotImplementedError();
  }

  @Override
  public double getTransP(GeoDate t) {
    throw new MethodNotImplementedError();
  }

  @Override
  public double getTransP(long t) {
    throw new MethodNotImplementedError();
  }

  @Override
  public double getTransP(SoTValue v) {
    throw new MethodNotImplementedError();
  }

  @Override
  public double getTransU(double p) {
    throw new MethodNotImplementedError();
  }

  @Override
  public GeoDate getTimeTransU(double p) {
    throw new MethodNotImplementedError();
  }

  @Override
  public long getLongTimeTransU(double p) {
    throw new MethodNotImplementedError();
  }

  @Override
  public SoTValue getSoTTransU(double p) {
    throw new MethodNotImplementedError();
  }

  @Override
  void computeTransform() {
    throw new MethodNotImplementedError();
  }
}
