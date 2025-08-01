/*
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */
// RCS $Id: LogTransform.java,v 1.8 2003/08/22 23:02:32 dwd Exp $
package gov.noaa.pmel.sgt;

import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.SoTRange;

/**
 * Transform class for creation of "log" axes. An {@link gov.noaa.pmel.sgt.demo.JLogLogDemo example}
 * is available demonstrating <code>LogTransform</code> use.
 *
 * <p>--------------------------------------------------------------------------<br>
 * NAME : LogTransform.java<br>
 * FUNCTION : Performs a logarithm transform on a cartesian axis.<br>
 * ORIGIN : GFI INFORMATIQUE<br>
 * PROJECT : SONC DPS<br>
 * -------------------------------------------------------------------------<br>
 * HISTORY<br>
 * VERSION : 03/07/2002 LBE<br>
 * old version had no fonctionality. It was just written<br>
 * for future evolutions. This new version complete the class<br>
 * END-HISTORY<br>
 * ------------------------------------------------------------------------<br>
 *
 * @author Donald Denbo
 * @version $Revision: 1.8 $, $Date: 2003/08/22 23:02:32 $
 * @since 3.0
 */
public class LogTransform extends AxisTransform implements Cloneable {
  double at_;
  double bt_;
  double a_;
  double b_;
  int min_ = -50;

  public LogTransform() {
    super();
  }

  public LogTransform(Range2D pr, SoTRange str) {
    super(pr, str);
  }

  @Override
  AxisTransform copy() {
    LogTransform newTransform;
    try {
      newTransform = (LogTransform) clone();
    } catch (CloneNotSupportedException e) {
      newTransform = new LogTransform();
    }
    return newTransform;
  }

  @Override
  public double getTransP(double u) {
    try {
      if (u1_ <= 0 || u2_ <= 0) {
        // System.out.println("ERROR Negative LOG");
        throw new NegativeLogException("Can't Log negative values");
      }
    } catch (NegativeLogException e) {
      e.printStackTrace();
    }
    return a_ * Math.log10(u) + b_;
  }

  @Override
  public double getTransP(GeoDate t) {
    throw new UnsupportedOperationException();
  }

  @Override
  public double getTransP(long t) {
    throw new UnsupportedOperationException();
  }

  @Override
  void computeTransform() {

    try {
      if (u1_ <= 0 || u2_ <= 0) {
        // System.out.println("ERROR Negative LOG: "+u1_+"/"+u2_);
        throw new NegativeLogException("Can't Log negative values");
      }
    } catch (NegativeLogException e) {
      e.printStackTrace();
    }

    double denom;
    denom = Math.log10(u1_) - Math.log10(u2_);
    if (denom == 0) {
      a_ = 1.0f;
      b_ = 0.0f;
    } else {
      a_ = (p1_ - p2_) / denom;
      b_ = p1_ - a_ * Math.log10(u1_);
    }
  }
}
