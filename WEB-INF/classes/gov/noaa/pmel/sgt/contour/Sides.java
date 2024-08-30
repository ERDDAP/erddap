/*
 * $Id: Sides.java,v 1.5 2001/02/02 20:27:37 dwd Exp $
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

import gov.noaa.pmel.util.Debug;
import java.util.BitSet;

/**
 * Used to keep track of which sides have been used during the contour line generation process.
 *
 * @author D. W. Denbo
 * @version $Revision: 1.5 $, $Date: 2001/02/02 20:27:37 $
 * @since 2.0
 */
class Sides {
  BitSet sides_;
  int nx_;
  int ny_;
  int ny2_;

  public Sides(int nx, int ny) {
    nx_ = nx;
    ny_ = ny;
    ny2_ = ny_ * 2;
    sides_ = new BitSet(ny2_ * nx_);
  }

  public boolean isSideUsed(int i, int j, int side) {
    int ind = index(i, j, side);
    if (ind < 0 || ind > ny2_ * nx_ - 1) {
      if (Debug.CONTOUR) {
        System.out.println("Sides.isSideUsed(): (i,j,side) = " + i + ", " + j + ", " + side);
      }
      return false;
    }
    return sides_.get(ind);
  }

  public void setSideUsed(int i, int j, int side, boolean set) {
    int ind = index(i, j, side);
    if (Debug.CONTOUR) {
      if (ind < 0 || ind > ny2_ * nx_ - 1) {
        System.out.println("Sides.setSideUsed(): (i,j,side) = " + i + ", " + j + ", " + side);
      }
    }
    if (set) {
      sides_.set(ind);
    } else {
      sides_.clear(ind);
    }
  }

  public int getSide(int i, int j, int side) {
    int ind = index(i, j, side);
    if (ind < 0 || ind > ny2_ * nx_ - 1) {
      if (Debug.CONTOUR) {
        System.out.println("Sides.getSide(): (i,j,side) = " + i + ", " + j + ", " + side);
      }
      return 0;
    }
    return sides_.get(ind) ? 1 : 0;
  }

  int index(int i, int j, int side) {
    int index = -10;
    if (side == 1) {
      /* i+1,j  right */
      index = 1 + j * 2 + (i + 1) * ny2_;
    } else if (side == 2) {
      /* i,j+1  top */
      index = (j + 1) * 2 + i * ny2_;
    } else if (side == 0) {
      /* i,j  bottom */
      index = j * 2 + i * ny2_;
    } else if (side == 3) {
      /* i,j  left */
      index = 1 + j * 2 + i * ny2_;
    }
    return index;
  }

  public void clear() {
    sides_ = new BitSet(ny2_ * nx_);
  }
}
