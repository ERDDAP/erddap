/*
 * $Id: DataGroupLayer.java,v 1.4 2003/09/17 20:32:10 dwd Exp $
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
import gov.noaa.pmel.sgt.Axis;
import gov.noaa.pmel.sgt.AxisNotFoundException;
import gov.noaa.pmel.sgt.AxisTransform;
import gov.noaa.pmel.sgt.CartesianGraph;
import gov.noaa.pmel.sgt.CartesianRenderer;
import gov.noaa.pmel.sgt.ColorMap;
import gov.noaa.pmel.sgt.ContourLevels;
import gov.noaa.pmel.sgt.ContourLevelsAccess;
import gov.noaa.pmel.sgt.DataKey;
import gov.noaa.pmel.sgt.Graph;
import gov.noaa.pmel.sgt.GridAttribute;
import gov.noaa.pmel.sgt.Layer;
import gov.noaa.pmel.sgt.LinearTransform;
import gov.noaa.pmel.sgt.LogAxis;
import gov.noaa.pmel.sgt.LogTransform;
import gov.noaa.pmel.sgt.PaneNotFoundException;
import gov.noaa.pmel.sgt.PlainAxis;
import gov.noaa.pmel.sgt.SGLabel;
import gov.noaa.pmel.sgt.TimeAxis;
import gov.noaa.pmel.sgt.TransformAccess;
import gov.noaa.pmel.sgt.dm.SGTData;
import gov.noaa.pmel.sgt.dm.SGTGrid;
import gov.noaa.pmel.util.Dimension2D;
import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.SoTDomain;
import gov.noaa.pmel.util.SoTPoint;
import gov.noaa.pmel.util.SoTRange;
import gov.noaa.pmel.util.SoTValue;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.swing.JOptionPane;

/**
 * A holder for <code>DataGroup</code> and <code>Layer</code> associated with axes and <code>Graph
 * </code>. Multiple sets of data can be added to a DataGroupLayer, but each will share the same
 * <code>DataGroup</code>. I.e., the same axes and transforms.
 *
 * @author Donald Denbo
 * @version $Revision: 1.4 $, $Date: 2003/09/17 20:32:10 $
 * @since 3.0
 * @see Page Page for UML diagram
 * @stereotype container
 */
public class DataGroupLayer extends Layer {
  private List dataLayers_ = new Vector();

  /**
   * @label dg
   */
  private DataGroup dg_ = null;

  /**
   * @label pHolder
   */
  private PanelHolder pHolder_ = null;

  private Panel panel_ = null;
  private SoTRange xRange_ = null;
  private SoTRange yRange_ = null;
  private boolean clipping_ = false;
  private SoTDomain clipDomain_ = null;
  //
  private SoTRange xZoomRange_ = null;
  private SoTRange yZoomRange_ = null;
  private boolean inZoom_ = false;
  //
  private SGLabel xLabel_ = null;
  private SGLabel yLabel_ = null;

  /** Default constructor. <code>Panel</code> and <code>DataGroup</code> must be specified. */
  public DataGroupLayer() {
    this(null, null);
  }

  /**
   * Construct <code>DataGroupLayer</code>.
   *
   * @param panel parent
   * @param dg datagroup
   */
  public DataGroupLayer(Panel panel, DataGroup dg) {
    super();
    panel_ = panel;
    setPane(panel.getPane());
    pHolder_ = dg.getPanelHolder();
    setGraph(new CartesianGraph("Graph"));
    dg_ = dg;
    setId(dg_.getId());
    update();
  }

  /**
   * Get the <code>Panel</code> parent.
   *
   * @return panel
   */
  public Panel getPanel() {
    return panel_;
  }

  /**
   * Get <code>DataGroup</code>
   *
   * @return the datagroup
   */
  public DataGroup getDataGroup() {
    return dg_;
  }

  /**
   * Get <code>Layer</code> <code>Iterator</code>.
   *
   * @return iterator of Layers
   */
  public Iterator getLayerIterator() {
    return dataLayers_.iterator();
  }

  /**
   * Get a <code>List</code> of the <code>Layer</code>s.
   *
   * @return layer list
   */
  public List getLayerList() {
    return dataLayers_;
  }

  private String getNextLayerId() {
    int count = dataLayers_.size() + 1;
    return getId() + "_" + count;
  }

  /**
   * Add data to the <code>DataGroupLayer</code>.
   *
   * @param data SGTData
   * @param attr Attribute associated with data
   * @param key DataKey
   * @throws DataTargetMismatchException
   */
  public void addData(SGTData data, Attribute attr, DataKey key)
      throws DataTargetMismatchException {
    if (Page.DEBUG) {
      System.out.println("DataGroupLayer.addData: data = " + data.toString());
      System.out.println(
          "DataGroupLayer.addData: xTime = " + data.isXTime() + ", yTime = " + data.isYTime());
      System.out.println("DataGroupLayer.addData: attr = " + attr.toString());
      System.out.println("DataGroupLayer.addData: key = " + key);
      System.out.println("DataGroupLayer.addData: xRange = " + data.getXRange());
      System.out.println("DataGroupLayer.addData: yRange = " + data.getYRange());
      if (data instanceof SGTGrid)
        System.out.println("DataGroupLayer.addData: zRange = " + ((SGTGrid) data).getZRange());
      else System.out.println("DataGroupLayer.addData: zRange = <no zRange>");
    }
    /**
     * @todo add data check
     */
    CartesianGraph graph = (CartesianGraph) getGraph();
    if ((graph.getXTransform().isTime() != data.isXTime())
        || graph.getYTransform().isTime() != data.isYTime()) {
      JOptionPane.showMessageDialog(
          this,
          "Added data does not have the\n"
              + "same axis types as the DataGroup\n\n"
              + "Time or Space axis does not match.",
          "DataGroup Error",
          JOptionPane.ERROR_MESSAGE);
      throw new DataTargetMismatchException("Data - Axis Mismatch");
    }

    CartesianRenderer rend = graph.getRenderer();
    if (rend == null) { // first data on DataGroupLayer
      xRange_ = data.getXRange();
      yRange_ = data.getYRange();
      graph.setData(data, attr);
      dataLayers_.add(this);

      StringBuffer label = new StringBuffer(data.getXMetaData().getName());
      String units = data.getXMetaData().getUnits();
      if (units != null && units.length() > 0) {
        label.append(" (").append(units).append(")");
      }
      xLabel_ = new SGLabel("X Axis Label", label.toString(), new Point2D.Double(0.0, 0.0));

      label = new StringBuffer(data.getYMetaData().getName());
      units = data.getYMetaData().getUnits();
      if (units != null && units.length() > 0) {
        label.append(" (").append(units).append(")");
      }
      yLabel_ = new SGLabel("Y Axis Label", label.toString(), new Point2D.Double(0.0, 0.0));
    } else { // adding data to DataGroupLayer
      xRange_.add(data.getXRange());
      yRange_.add(data.getYRange());
      // more data, create new layer
      Layer ly = new Layer(getNextLayerId());
      CartesianGraph cg =
          new CartesianGraph("Graph_" + ly.getId(), graph.getXTransform(), graph.getYTransform());
      ly.setPane(getPane());
      ly.setSizeP(getSizeP());
      ly.setBounds(getBounds());
      ly.setGraph(cg);
      cg.setData(data, attr);
      dataLayers_.add(ly);
      if (clipping_) cg.setClip(clipDomain_.getXRange(), clipDomain_.getYRange());
      graph = cg;
    }

    if (dg_.isZAutoScale() && attr != null && attr instanceof GridAttribute) {
      Range2D zRange = ((SGTGrid) data).getZRange();
      ColorMap cmap = ((GridAttribute) attr).getColorMap();
      if (cmap instanceof TransformAccess) {
        ((TransformAccess) cmap).setRange(zRange);
      } else if (cmap instanceof ContourLevelsAccess) {
        ContourLevels cl = ((ContourLevelsAccess) cmap).getContourLevels();
        int levels = dg_.getNumberAutoContourLevels();
        Range2D newRange = Graph.computeRange(zRange, levels);
        ((ContourLevelsAccess) cmap).setContourLevels(ContourLevels.getDefault(newRange));
      }
    }

    if (key != null) {
      rend = graph.getRenderer();
      SGLabel label = getLabel(data, key);
      key.addGraph(rend, label);
    }
    update();
  }

  private SGLabel getLabel(SGTData data, DataKey key) {
    SGLabel lineTitle = data.getKeyTitle();
    if (lineTitle == null) {
      lineTitle = xLabel_;
    }
    Legend lg = pHolder_.findLegend(key.getId());
    lineTitle.setHeightP(lg.getKeyLabelHeightP());
    return lineTitle;
  }

  /** Update <code>DataGroupLayer</code>. Used internally. */
  public void update() {
    Rectangle bnds = pHolder_.getBounds();
    double dpi = pHolder_.getPanelModel().getDpi();
    double width = bnds.width / dpi;
    double height = bnds.height / dpi;
    SoTRange xRange = null;
    SoTRange yRange = null;
    SoTPoint xOrig = null;
    SoTPoint yOrig = null;

    boolean batch = getPane().isBatch();
    getPane().setBatch(true);

    AxisHolder xAxHolder = dg_.getXAxisHolder();
    AxisHolder yAxHolder = dg_.getYAxisHolder();
    CartesianGraph gr = (CartesianGraph) getGraph();

    this.setSizeP(new Dimension2D(width, height));
    // determine correct range
    if (inZoom_) {
      xRange = xZoomRange_;
      yRange = yZoomRange_;
    } else {
      if (!xAxHolder.isAutoRange() || xRange_ == null) {
        xRange = xAxHolder.getUserRange();
      } else {
        xRange = xRange_;
      }
      if (!yAxHolder.isAutoRange() || yRange_ == null) {
        yRange = yAxHolder.getUserRange();
      } else {
        yRange = yRange_;
      }
    }
    // transform setup
    updateTransform(DataGroup.X_DIR, xRange);
    updateTransform(DataGroup.Y_DIR, yRange);
    // determine axes origins
    // xaxis
    Margin marg = dg_.getMargin();
    if (xAxHolder.getAxisPosition() == DataGroup.MANUAL) {
      xOrig =
          new SoTPoint(
              gr.getXPtoSoT(xAxHolder.getAxisOriginP().x),
              gr.getYPtoSoT(xAxHolder.getAxisOriginP().y));
    } else {
      SoTValue yloc = null;
      SoTRange range = null;
      if (yAxHolder.getAxisPosition() == DataGroup.MANUAL) {
        if (gr.getYTransform().isTime()) {
          range = new SoTRange.Time(gr.getYPtoLongTime(marg.bottom), gr.getYPtoLongTime(marg.top));
        } else {
          range = new SoTRange.Double(gr.getYPtoU(marg.bottom), gr.getYPtoU(marg.top));
        }
      } else {
        range = yRange;
      }
      switch (xAxHolder.getAxisPosition()) {
        case DataGroup.BOTTOM:
          yloc = range.getStart();
          break;
        case DataGroup.TOP:
          yloc = range.getEnd();
          break;
      }
      xOrig = new SoTPoint(xRange.getStart(), yloc);
    }
    // yaxis
    if (yAxHolder.getAxisPosition() == DataGroup.MANUAL) {
      yOrig =
          new SoTPoint(
              gr.getXPtoSoT(yAxHolder.getAxisOriginP().x),
              gr.getYPtoSoT(yAxHolder.getAxisOriginP().y));
    } else {
      SoTValue xloc = null;
      SoTRange range = null;
      if (xAxHolder.getAxisPosition() == DataGroup.MANUAL) {
        if (gr.getXTransform().isTime()) {
          range = new SoTRange.Time(gr.getXPtoLongTime(marg.left), gr.getXPtoLongTime(marg.right));
        } else {
          range = new SoTRange.Double(gr.getXPtoU(marg.left), gr.getXPtoU(marg.right));
        }
      } else {
        range = xRange;
      }
      switch (yAxHolder.getAxisPosition()) {
        case DataGroup.LEFT:
          xloc = range.getStart();
          break;
        case DataGroup.RIGHT:
          xloc = range.getEnd();
          break;
      }
      yOrig = new SoTPoint(xloc, yRange.getStart());
    }

    updateAxis(DataGroup.X_DIR, xRange, xOrig, xLabel_);
    updateAxis(DataGroup.Y_DIR, yRange, yOrig, yLabel_);

    getPane().setBatch(batch);

    if (Page.DEBUG) {
      System.out.println("layer: " + getSize() + ", " + getSizeP());
      System.out.println(
          "xTrans: " + gr.getXTransform().getRangeP() + ", " + gr.getXTransform().getSoTRangeU());
      System.out.println(
          "yTrans: " + gr.getYTransform().getRangeP() + ", " + gr.getYTransform().getSoTRangeU());
    }
  }

  private void updateTransform(int dir, SoTRange range) {
    AxisTransform at;
    AxisHolder ah;
    CartesianGraph gr = (CartesianGraph) getGraph();

    if (dir == DataGroup.X_DIR) {
      ah = dg_.getXAxisHolder();
      at = gr.getXTransform();
    } else {
      ah = dg_.getYAxisHolder();
      at = gr.getYTransform();
    }

    switch (ah.getTransformType()) {
      case DataGroup.LINEAR:
        if (at instanceof LinearTransform) {
          at.setRangeU(range);
          at.setRangeP(ah.getRangeP());
        } else {
          at = new LinearTransform(ah.getRangeP(), range);
          gr.setXTransform(at);
        }
        break;
      case DataGroup.LOG:
        if (at instanceof LogTransform) {
          at.setRangeU(range);
          at.setRangeP(ah.getRangeP());
        } else {
          at = new LogTransform(ah.getRangeP(), range);
          gr.setXTransform(at);
        }
        break;
      case DataGroup.REFERENCE:
        at = getReferenceTransform(DataGroup.X_DIR, ah.getTransformGroup());
        gr.setXTransform(at);
    }
  }

  private void updateAxis(int dir, SoTRange range, SoTPoint origin, SGLabel title) {
    String axis;
    Axis ax = null;
    AxisTransform at;
    AxisHolder ah;
    boolean newAxis = true;
    CartesianGraph gr = (CartesianGraph) getGraph();

    if (dir == DataGroup.X_DIR) {
      axis = "X Axis";
      ah = dg_.getXAxisHolder();
      at = gr.getXTransform();
    } else {
      axis = "Y Axis";
      ah = dg_.getYAxisHolder();
      at = gr.getYTransform();
    }

    try {
      ax = gr.getXAxis(axis);
    } catch (AxisNotFoundException anfe) {
      ax = null;
    }
    newAxis = false;
    switch (ah.getAxisType()) {
      case DataGroup.PLAIN:
        PlainAxis pax = null;
        if (ax != null) {
          if (ax instanceof PlainAxis) {
            pax = (PlainAxis) ax;
          } else {
            pax = new PlainAxis(axis);
            newAxis = true;
          }
        } else {
          pax = new PlainAxis(axis);
          newAxis = true;
        }
        ax = pax;
        pax.setRangeP(ah.getRangeP());
        pax.setRangeU(range);
        pax.setLabelFormat(ah.getLabelFormat());
        pax.setLabelInterval(ah.getLabelInterval());
        pax.setSignificantDigits(ah.getLabelSignificantDigits());
        break;
      case DataGroup.TIME:
        TimeAxis tax = null;
        if (ax != null) {
          if (ax instanceof TimeAxis) {
            tax = (TimeAxis) ax;
          } else {
            tax = new TimeAxis(axis, ah.getTimeAxisStyle());
            newAxis = true;
          }
        } else {
          tax = new TimeAxis(axis, ah.getTimeAxisStyle());
          newAxis = true;
        }
        ax = tax;
        tax.setRangeP(ah.getRangeP());
        tax.setRangeU(range);
        if (ah.getTimeAxisStyle() != TimeAxis.AUTO) {
          tax.setLabelFormat(ah.getMinorFormat(), ah.getMajorFormat());
          tax.setLabelInterval(ah.getMinorInterval(), ah.getMajorInterval());
        }
        break;
      case DataGroup.LOG:
        LogAxis lax = null;
        if (ax != null) {
          if (ax instanceof LogAxis) {
            lax = (LogAxis) ax;
          } else {
            lax = new LogAxis(axis);
            newAxis = true;
          }
        } else {
          lax = new LogAxis(axis);
          newAxis = true;
        }
        ax = lax;
        lax.setRangeP(ah.getRangeP());
        lax.setRangeU(range);
        lax.setLabelFormat(ah.getLabelFormat());
        lax.setLabelInterval(ah.getLabelInterval());
        lax.setSignificantDigits(ah.getLabelSignificantDigits());
    }
    if (dir == DataGroup.X_DIR) {
      ax.setOrientation(Axis.HORIZONTAL);
    } else {
      ax.setOrientation(Axis.VERTICAL);
    }
    ax.setLocationU(origin);
    ax.setLineColor(ah.getAxisColor());
    ax.setTicPosition(ah.getTicPosition());
    ax.setLabelColor(ah.getLabelColor());
    ax.setLabelFont(ah.getLabelFont());
    ax.setLabelHeightP(ah.getLabelHeightP());
    ax.setLargeTicHeightP(ah.getLargeTicHeightP());
    ax.setNumberSmallTics(ah.getNumSmallTics());
    ax.setSmallTicHeightP(ah.getSmallTicHeightP());
    ax.setThickTicWidthP(ah.getThickTicWidth());
    ax.setLabelPosition(ah.getLabelPosition());
    ax.setVisible(ah.isVisible());
    if (title != null && ah.isTitleAuto()) {
      SGLabel def = ah.getTitle();
      title.setColor(def.getColor());
      title.setFont(def.getFont());
      title.setHeightP(def.getHeightP());
      title.setVisible(def.isVisible());
      ax.setTitle(title);
    } else {
      ax.setTitle(ah.getTitle());
    }
    ax.register(at);
    if (dir == DataGroup.X_DIR) {
      if (gr.getNumberXAxis() > 0 && newAxis) {
        gr.removeAllXAxes();
      }
      if (newAxis) gr.addXAxis(ax);
    } else {
      if (gr.getNumberYAxis() > 0 && newAxis) {
        gr.removeAllYAxes();
      }
      if (newAxis) gr.addYAxis(ax);
    }
  }

  //
  // replicate Layer methods and pass on to
  // all children Layer's
  //
  @Override
  public void draw(Graphics g) throws PaneNotFoundException {
    super.draw(g);
    for (int i = 1; i < dataLayers_.size(); i++) {
      ((Layer) dataLayers_.get(i)).draw(g);
    }
  }

  @Override
  public void drawDraggableItems(Graphics g) throws PaneNotFoundException {
    super.drawDraggableItems(g);
    for (int i = 1; i < dataLayers_.size(); i++) {
      ((Layer) dataLayers_.get(i)).drawDraggableItems(g);
    }
  }

  @Override
  public void setBounds(int x, int y, int w, int h) {
    super.setBounds(x, y, w, h);
    for (int i = 1; i < dataLayers_.size(); i++) {
      ((Layer) dataLayers_.get(i)).setBounds(x, y, w, h);
    }
  }

  @Override
  public void setBounds(Rectangle rect) {
    super.setBounds(rect);
    for (int i = 1; i < dataLayers_.size(); i++) {
      ((Layer) dataLayers_.get(i)).setBounds(rect);
    }
  }

  @Override
  public void setLocation(int x, int y) {
    super.setLocation(x, y);
    for (int i = 1; i < dataLayers_.size(); i++) {
      ((Layer) dataLayers_.get(i)).setLocation(x, y);
    }
  }

  @Override
  public void setLocation(Point pt) {
    super.setLocation(pt);
    for (int i = 1; i < dataLayers_.size(); i++) {
      ((Layer) dataLayers_.get(i)).setLocation(pt);
    }
  }

  @Override
  public void setSize(Dimension size) {
    super.setSize(size);
    for (int i = 1; i < dataLayers_.size(); i++) {
      ((Layer) dataLayers_.get(i)).setSize(size);
    }
  }

  @Override
  public void setSize(int w, int h) {
    super.setSize(w, h);
    for (int i = 1; i < dataLayers_.size(); i++) {
      ((Layer) dataLayers_.get(i)).setSize(w, h);
    }
  }

  @Override
  public void setSizeP(Dimension2D size) {
    super.setSizeP(size);
    for (int i = 1; i < dataLayers_.size(); i++) {
      ((Layer) dataLayers_.get(i)).setSizeP(size);
    }
  }

  private AxisTransform getReferenceTransform(int dir, String datagroup) {
    //    PanelHolder ph = dg_.getPanelHolder();
    String dgLast = datagroup;
    boolean stillLooking = true;
    int tType = -1;
    String newDG = null;
    while (stillLooking) {
      DataGroup dg = pHolder_.findDataGroup(dgLast);
      if (dir == DataGroup.X_DIR) {
        tType = dg.getXAxisHolder().getTransformType();
      } else {
        tType = dg.getYAxisHolder().getTransformType();
      }
      if (tType != DataGroup.REFERENCE) {
        CartesianGraph graph = null;
        Layer dgl = null;
        /**
         * @todo resolve instatiation order issue
         */
        if (dir == DataGroup.X_DIR) {
          dgl = panel_.findDataGroupLayer(dg.getXAxisHolder().getTransformGroup());
          graph = (CartesianGraph) dgl.getGraph();
          if (graph == null) return null; // graph and transform not yet created
          return graph.getXTransform();
        } else {
          dgl = panel_.findDataGroupLayer(dg.getYAxisHolder().getTransformGroup());
          graph = (CartesianGraph) dgl.getGraph();
          if (graph == null) return null;
          return graph.getYTransform();
        }
      }
      if (dir == DataGroup.X_DIR) {
        newDG = dg.getXAxisHolder().getTransformGroup();
      } else {
        newDG = dg.getYAxisHolder().getTransformGroup();
      }
      dgLast = newDG;
    }
    return null;
  }

  /**
   * Set clipping for <code>DataGroupLayer</code>
   *
   * @param clip if true clip data to bounds.
   */
  public void setClipping(boolean clip) {
    if (!dg_.isZoomable()) return;
    if (Page.DEBUG) System.out.println("DataGroupLayer: " + getId() + ": clip = " + clip);
    clipping_ = clip;
    setAllClipping(clipping_);
  }

  /**
   * Zoom <code>DataGroupLayer</code>. Zoom to <code>Rectangle</code> if zoom operation started
   * within bounds.
   *
   * @param start start point
   * @param rect zoom rectangle
   */
  void zoomTo(Point start, Rectangle rect) {
    if (!dg_.isZoomable()) return;
    CartesianGraph graph = (CartesianGraph) getGraph();
    Rectangle gbnds = getGraphBounds();
    Rectangle bnds = getPanelBounds();
    if (!gbnds.contains(start)) return;
    setClipping(true);
    double xStartP = getXDtoP(rect.x);
    double yStartP = getYDtoP(rect.y + rect.height);
    double xEndP = getXDtoP(rect.x + rect.width);
    double yEndP = getYDtoP(rect.y);
    SoTRange xRangeU = null;
    SoTRange yRangeU = null;
    if (graph.getXTransform().isTime()) {
      xRangeU = new SoTRange.Time(graph.getXPtoLongTime(xStartP), graph.getXPtoLongTime(xEndP));
    } else {
      xRangeU = new SoTRange.Double(graph.getXPtoU(xStartP), graph.getXPtoU(xEndP));
    }
    if (graph.getYTransform().isTime()) {
      yRangeU = new SoTRange.Time(graph.getYPtoLongTime(yStartP), graph.getYPtoLongTime(yEndP));
    } else {
      yRangeU = new SoTRange.Double(graph.getYPtoU(yStartP), graph.getYPtoU(yEndP));
    }
    inZoom_ = true;
    setDomain(new SoTDomain(xRangeU, yRangeU));
  }

  /**
   * Reset the zoom for this <code>DataGroupLayer</code> if it contains the point.
   *
   * @param x x device coordinate
   * @param y y device coordinate
   */
  void resetZoom(int x, int y) {
    if (!dg_.isZoomable()) return;
    Rectangle bnds = getGraphBounds();
    if (!bnds.contains(x, y)) return;
    if (Page.DEBUG) System.out.println("DataGroupLayer: " + getId() + ": " + x + ", " + y);
    inZoom_ = false;
    xZoomRange_ = null;
    yZoomRange_ = null;
    setClipping(false);
    update();
  }

  /** Reset the zoom for this <code>DataGroupLayer</code>. */
  public void resetZoom() {
    inZoom_ = false;
    xZoomRange_ = null;
    yZoomRange_ = null;
    setClipping(false);
    update();
  }

  /**
   * Set <code>DataGroupLayer</code> domain.
   *
   * @param domain domain
   */
  public void setDomain(SoTDomain domain) {
    setXRange(domain.getXRange(), domain.isXReversed());
    setYRange(domain.getYRange(), domain.isYReversed());
    if (clipping_) {
      clipDomain_ = domain;
      setAllClip(domain);
    } else {
      clipDomain_ = null;
      setAllClipping(false);
    }
    update();
    if (Page.DEBUG) {
      System.out.println("DataGroupLayer().setDomain: " + getId());
      System.out.println("        domain.XRange = " + domain.getXRange());
      System.out.println("        domain.YRange = " + domain.getYRange());
      System.out.println("           isClipping = " + clipping_);
    }
  }

  private void setXRange(SoTRange range, boolean reversed) {
    xZoomRange_ = range.copy();
  }

  private void setYRange(SoTRange range, boolean reversed) {
    yZoomRange_ = range.copy();
  }

  private void setAllClip(SoTDomain domain) {
    Iterator iter = dataLayers_.iterator();
    while (iter.hasNext()) {
      Object obj = iter.next();
      if (obj instanceof Layer) {
        Layer ly = (Layer) obj;
        ((CartesianGraph) ly.getGraph()).setClip(domain.getXRange(), domain.getYRange());
      }
    }
  }

  private void setAllClipping(boolean clip) {
    Iterator iter = dataLayers_.iterator();
    while (iter.hasNext()) {
      Object obj = iter.next();
      if (obj instanceof Layer) {
        Layer ly = (Layer) obj;
        ((CartesianGraph) ly.getGraph()).setClipping(clip);
      }
    }
  }

  private Rectangle getPanelBounds() {
    return pHolder_.getBounds();
  }

  private Rectangle getGraphBounds() {
    CartesianGraph graph = (CartesianGraph) getGraph();
    AxisTransform xTrans = graph.getXTransform();
    AxisTransform yTrans = graph.getYTransform();
    Range2D xRange = xTrans.getRangeP();
    Range2D yRange = yTrans.getRangeP();
    int x = getXPtoD(xRange.start);
    int y = getYPtoD(yRange.end);
    int width = getXPtoD(xRange.end) - x;
    int height = getYPtoD(yRange.start) - y;
    return new Rectangle(x, y, width, height);
  }
}
