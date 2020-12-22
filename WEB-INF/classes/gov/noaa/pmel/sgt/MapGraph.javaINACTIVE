/*
 * $Id: MapGraph.java,v 1.5 2003/08/22 23:02:32 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package  gov.noaa.pmel.sgt;

import gov.noaa.pmel.sgt.dm.SGTData;

import java.awt.Graphics;
import java.awt.Point;
import java.beans.PropertyChangeEvent;

/**
 * Base class for all Map based Graphs and projections.
 *
 * @author Donald Denbo
 * @version $Revision: 1.5 $, $Date: 2003/08/22 23:02:32 $
 * @since 2.x
 */
public class MapGraph extends Graph implements Cloneable {
  /**@shapeType AggregationLink*/
  private MapProj mproj_;
  public MapGraph() {
  }
  public Graph copy() {
    MapGraph newGraph;
    try {
      newGraph = (MapGraph)clone();
    } catch (CloneNotSupportedException e) {
      newGraph = new MapGraph();
    }
    return (Graph)newGraph;
  }
  void draw(Graphics g) {
    throw new MethodNotImplementedError();
  }
  public void attachProj(MapProj proj) {
    throw new MethodNotImplementedError();
  }
  public Object getObjectAt(Point pt) {
    throw new MethodNotImplementedError();
  }

  public void propertyChange(PropertyChangeEvent evt) {
  }
  /**
   * @since 3.0
   */
  public SGTData getDataAt(Point pt) {
    throw new MethodNotImplementedError();
  }
}
