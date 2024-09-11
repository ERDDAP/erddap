/*
 * $Id: PlotLayer.java,v 1.3 2003/08/22 23:02:39 dwd Exp $
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

import gov.noaa.pmel.sgt.Layer;
/**
 * @since 2.x
 */
public class PlotLayer extends Layer {
  public PlotLayerHints getPlotLayerHints(){
      return plotLayerHints_;
    }

  public void setPlotLayerHints(PlotLayerHints plotLayerHints){
      this.plotLayerHints_ = plotLayerHints;
    }

  /**
   * @link aggregation
   * @undirected
   * @label plotLayerHints 
   */
  private PlotLayerHints plotLayerHints_;
}
