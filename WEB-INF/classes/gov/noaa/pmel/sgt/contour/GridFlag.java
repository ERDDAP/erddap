/*
 * $Id: GridFlag.java,v 1.7 2001/02/02 20:27:37 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package gov.noaa.pmel.sgt.contour;

import gov.noaa.pmel.sgt.dm.SGTGrid;
import gov.noaa.pmel.util.Debug;

/**
 * Used to set and test whether a grid value is less than, equal, or greater than a contour level.
 *
 * @author D. W. Denbo
 * @version $Revision: 1.7 $, $Date: 2001/02/02 20:27:37 $
 * @since 2.0
 */
class GridFlag {
  public static final int LESS_THAN_ZC = -1;
  public static final int MISSING = 10;
  public static final int GREATER_THAN_ZC = 1;

  private double[] array_;
  private double[] mask_ = null;
  private byte[] value_;
  private int nx_;
  private int ny_;

  public GridFlag(SGTGrid grid, double zc) {
    this(grid, null, zc);
  }

  public GridFlag(SGTGrid grid, SGTGrid mask, double zc) {
    if (grid.isXTime()) {
      nx_ = grid.getTSize();
    } else {
      nx_ = grid.getXSize();
    }
    if (grid.isYTime()) {
      ny_ = grid.getTSize();
    } else {
      ny_ = grid.getYSize();
    }
    value_ = new byte[nx_ * ny_];
    array_ = grid.getZArray();
    setLevel(zc);
    if (mask != null) setMask(mask);
  }

  public void setMask(SGTGrid mask) {
    mask_ = mask.getZArray();
    applyMask();
  }

  void applyMask() {
    if (mask_ == null) return;
    for (int i = 0; i < nx_ * ny_; i++) {
      if (Double.isNaN(mask_[i]) || mask_[i] != 0.0) {
        value_[i] = MISSING;
      }
    }
  }

  public void setLevel(double zc) {
    for (int i = 0; i < nx_ * ny_; i++) {
      if (!Double.isNaN(array_[i])) {
        if (array_[i] <= zc) {
          value_[i] = LESS_THAN_ZC;
        } else {
          value_[i] = GREATER_THAN_ZC;
        }
      } else {
        value_[i] = MISSING;
      }
    }
    applyMask();
  }

  int index(int i, int j) {
    return j + ny_ * i;
  }

  /** True if grid(i,j) is missing. */
  public boolean isMissing(int i, int j) {
    int ind = index(i, j);
    if (Debug.CONTOUR) {
      if (ind < 0 || ind >= nx_ * ny_) {
        System.out.println("GridFlag.isMissing(): (i,j) = (" + i + ", " + j + ")");
      }
    }
    return (value_[ind] == MISSING);
  }

  public boolean isGreater(int i, int j) {
    int ind = index(i, j);
    if (Debug.CONTOUR) {
      if (ind < 0 || ind >= nx_ * ny_) {
        System.out.println("GridFlag.isGreater(): (i,j) = (" + i + ", " + j + ")");
      }
    }
    return (value_[ind] == GREATER_THAN_ZC);
  }

  public int getValue(int i, int j) {
    int ind = index(i, j);
    if (Debug.CONTOUR) {
      if (ind < 0 || ind >= nx_ * ny_) {
        System.out.println("GridFlag.getValue(): (i,j) = (" + i + ", " + j + ")");
      }
    }
    return value_[ind];
  }
}
