/*
 * $Id: TableLookupTransform.java,v 1.4 2001/12/11 21:31:42 dwd Exp $
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

import gov.noaa.pmel.util.Range2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Description of Class TableLookupTransform
 *
 * @author Donald Denbo
 * @version $Revision: 1.4 $, $Date: 2001/12/11 21:31:42 $
 * @since 2.x
 */
public class TableLookupTransform implements Cloneable, Transform {
  private PropertyChangeSupport changes_ = new PropertyChangeSupport(this);
  private double[] pvals_;
  private double[] uvals_;

  public TableLookupTransform() {}

  public TableLookupTransform(double p1, double p2, double u1, double u2) {}

  public TableLookupTransform(Range2D pr, Range2D ur) {}

  public TableLookupTransform(double[] p, double[] u) throws DataNotSameShapeException {}

  @Override
  public void setRangeP(Range2D pr) {
    throw new MethodNotImplementedError();
  }

  @Override
  public void setRangeP(double pmin, double pmax) {
    throw new MethodNotImplementedError();
  }

  @Override
  public Range2D getRangeP() {
    throw new MethodNotImplementedError();
  }

  @Override
  public void setRangeU(Range2D ur) {
    throw new MethodNotImplementedError();
  }

  @Override
  public void setRangeU(double umin, double umax) {
    throw new MethodNotImplementedError();
  }

  @Override
  public Range2D getRangeU() {
    throw new MethodNotImplementedError();
  }

  public void setDoInteger(boolean si) {
    throw new MethodNotImplementedError();
  }

  public boolean getDoInteger() {
    throw new MethodNotImplementedError();
  }

  @Override
  public double getTransP(double u) {
    throw new MethodNotImplementedError();
  }

  @Override
  public double getTransU(double p) {
    throw new MethodNotImplementedError();
  }

  void computeTransform() {
    throw new MethodNotImplementedError();
  }

  /** Add listener to changes in <code>TableLookupTransform</code> properties. */
  @Override
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    changes_.addPropertyChangeListener(listener);
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener listener) {
    changes_.removePropertyChangeListener(listener);
  }

  @Override
  public void releaseResources() throws Exception {
    changes_ = null;
    pvals_ = null;
    uvals_ = null;
  }
}
