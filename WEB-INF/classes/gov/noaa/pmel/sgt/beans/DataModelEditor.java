/*
 * $Id: DataModelEditor.java,v 1.2 2003/08/22 23:02:34 dwd Exp $
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

import javax.swing.JComponent;

/**
 * Used at run-time to modify the <code>DataModel</code>. Not implemented at this
 * time.
 *
 * @author Donald Denbo
 * @version $Revision: 1.2 $, $Date: 2003/08/22 23:02:34 $
 * @since 3.0
 **/
class DataModelEditor extends JComponent {
  /**
   * @supplierCardinality 1
   */
  private DataModel dataModel_;

  public DataModelEditor() {
    super();
    dataModel_ = null;
  }

  public DataModelEditor(DataModel model) {
    super();
    setDataModel(model);
  }

  public DataModel getDataModel(){
    return dataModel_;
  }

  public void setDataModel(DataModel dataModel){
    dataModel_ = dataModel;
  }
}
