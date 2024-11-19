/*
 * $Id: DataHolder.java,v 1.2 2003/08/22 23:02:34 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package gov.noaa.pmel.sgt.beans;

import gov.noaa.pmel.sgt.Attribute;
import gov.noaa.pmel.sgt.dm.SGTData;

/**
 * Object to associate <code>SGTData</code>, <code>Attribute</code>, <code>Panel</code>, and <code>
 * DataGroup</code>.
 *
 * @author Donald Denbo
 * @version $Revision: 1.2 $, $Date: 2003/08/22 23:02:34 $
 * @since 3.0
 */
class DataHolder {
  /**
   * @undirected
   * @supplierCardinality 1
   * @link aggregation
   * @label data
   */
  private SGTData data_;

  /**
   * @link aggregation
   * @undirected
   * @supplierCardinality 1
   * @label attr
   */
  private Attribute attr_;

  /**
   * @supplierCardinality 1
   * @label pHolder
   */
  private PanelHolder pHolder_;

  /**
   * @supplierCardinality 1
   * @label dataGroup
   */
  private DataGroup dataGroup_;

  /**
   * @label legend
   */
  private Legend legend_;

  /**
   * @label dModel
   */
  private DataModel dModel_;

  public DataHolder(
      DataModel model,
      SGTData data,
      Attribute attr,
      PanelHolder pHolder,
      DataGroup dataGroup,
      Legend legend) {
    dModel_ = model;
    data_ = data;
    attr_ = attr;
    pHolder_ = pHolder;
    dataGroup_ = dataGroup;
    legend_ = legend;
  }

  SGTData getData() {
    return data_;
  }

  Attribute getAttribute() {
    return attr_;
  }

  PanelHolder getPanelHolder() {
    return pHolder_;
  }

  DataGroup getDataGroup() {
    return dataGroup_;
  }

  Legend getLegend() {
    return legend_;
  }

  public void notifyPanel() throws DataTargetMismatchException {
    dModel_.getPage().findPanel(pHolder_).addData(data_, attr_, dataGroup_, legend_);
  }
}
