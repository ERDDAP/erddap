/*
 * $Id: PlotPaneMode.java,v 1.2 2001/12/11 21:31:43 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */
 
package gov.noaa.pmel.sgt.plot;

/**
 * @since 2.x
 */
final public class PlotPaneMode {
  /**
   * @supplierCardinality
   */
  private static PlotPaneMode[] values_ = new PlotPaneMode[4];
  private int value_;
    public final static int _ZOOM_DOMAIN = 0;
    public final static PlotPaneMode ZOOM_DOMAIN = new PlotPaneMode(_ZOOM_DOMAIN);
    public final static int _SELECT_OBJECT = 1;
    public final static PlotPaneMode SELECT_OBJECT = new PlotPaneMode(_SELECT_OBJECT);
    public final static int _SELECT_DATA = 2;
    public final static PlotPaneMode SELECT_DATA = new PlotPaneMode(_SELECT_DATA);
    public final static int _DRAG_AND_DROP = 3;
    public final static PlotPaneMode DRAG_AND_DROP = new PlotPaneMode(_DRAG_AND_DROP);

  protected PlotPaneMode(int value) {
    values_[value] = this;
    value_ = value;
  }
  public int getValue() {
    return value_;
  }
  public PlotPaneMode from_int(int value) {
    return values_[value];
  }
}
