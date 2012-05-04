/*
 * $Id: PlotLayerManager.java,v 1.4 2003/08/22 23:02:39 dwd Exp $
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

import gov.noaa.pmel.sgt.AxisTransform;
import gov.noaa.pmel.sgt.LayerStack;
import gov.noaa.pmel.sgt.LayerNotFoundException;
/**
 * @since 2.x
 */
public class PlotLayerManager {
    public PlotLayerManager(JPlotPane plotPane) {
        plotPane_ = plotPane;
    }

  /**
   *
   */
  public void addLayer(PlotLayer layer) {
  }

  public void removeLayer(int index) throws LayerNotFoundException {
  }

  public void removeLayer(PlotLayer layer) throws LayerNotFoundException {
  }

  public void update() {
  }

  /** @link dependency */
  /*#LayerStack lnkLayerStack;*/

  /** @link dependency */
  /*#PlotLayer lnkPlotLayer;*/

  /** @link dependency */
  /*#AxisTransform lnkAxisTransform;*/

  /** @link dependency */
  /*#PlotLayerHints lnkPlotLayerHints;*/

  /**
   * @label plotPane 
   */
  private JPlotPane plotPane_;
}
