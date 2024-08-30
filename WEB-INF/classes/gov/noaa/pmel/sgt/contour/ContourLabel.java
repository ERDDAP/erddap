/*
 * $Id: ContourLabel.java,v 1.3 2001/02/02 20:27:37 dwd Exp $
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

import gov.noaa.pmel.sgt.SGLabel;

/**
 * Container for information about where labels should be inserted on a ContourLine.
 *
 * @author D. W. Denbo
 * @version $Revision: 1.3 $, $Date: 2001/02/02 20:27:37 $
 * @since 2.0
 */
class ContourLabel {
  int index;

  /**
   * @label label
   * @link aggregationByValue
   * @supplierCardinality 1
   */
  SGLabel label;

  double width;
  double height;

  public ContourLabel(int indx, SGLabel lab, double hgt, double wid) {
    index = indx;
    label = lab;
    width = wid;
    height = hgt;
  }

  public SGLabel getLabel() {
    return label;
  }

  public int getIndex() {
    return index;
  }

  public double getWidth() {
    return width;
  }

  public double getHeight() {
    return height;
  }
}
