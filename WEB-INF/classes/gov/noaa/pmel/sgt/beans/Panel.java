/*
 * $Id: Panel.java,v 1.8 2003/09/17 23:16:45 dwd Exp $
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

import gov.noaa.pmel.sgt.AbstractPane;
import gov.noaa.pmel.sgt.Attribute;
import gov.noaa.pmel.sgt.ChildNotFoundException;
import gov.noaa.pmel.sgt.ColorKey;
import gov.noaa.pmel.sgt.DataKey;
import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.Layer;
import gov.noaa.pmel.sgt.LayerChild;
import gov.noaa.pmel.sgt.LayerControl;
import gov.noaa.pmel.sgt.LineKey;
import gov.noaa.pmel.sgt.PaneNotFoundException;
import gov.noaa.pmel.sgt.PointCollectionKey;
import gov.noaa.pmel.sgt.Ruler;
import gov.noaa.pmel.sgt.SGLabel;
import gov.noaa.pmel.sgt.StackedLayout;
import gov.noaa.pmel.sgt.VectorKey;
import gov.noaa.pmel.sgt.dm.SGTData;
import gov.noaa.pmel.util.Dimension2D;
import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.Rectangle2D;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import javax.swing.JComponent;

/**
 * A <code>Layer</code> container that provides a <code>StackedLayout</code> of the <code>Layer
 * </code>s.
 *
 * @author Donald Denbo
 * @version $Revision: 1.8 $, $Date: 2003/09/17 23:16:45 $
 * @since 3.0
 * @stereotype container
 */
public class Panel extends JComponent implements LayerControl {
  /**
   * @label pane
   */
  private JPane pane_ = null;

  /**
   * @directed
   * @label pHolder
   */
  private PanelHolder pHolder_ = null;

  /**
   * @label labelLayer
   */
  private Layer labelLayer_ = null;

  /**
   * @label legendLayer
   */
  private Layer legendLayer_ = null;

  /**
   * @link aggregation
   * @supplierCardinality *
   */
  /*#DataGroupLayer lnkDataGroupLayer; */
  private Map dataGroupLayerList_ = new HashMap(2);

  /**
   * <code>Panel</code> constructor. <code>PanelHolder</code> will need to be set.
   *
   * @param name Panel name
   */
  public Panel(String name) {
    super();
    setName(name);
    setLayout(new StackedLayout());
  }

  /**
   * <code>Panel</code> constructor with <code>PanelHolder</code> argument. <code>Panel</code> name
   * set from <code>PanelHolder</code> identifier.
   *
   * @param ph PanelHolder
   */
  public Panel(PanelHolder ph) {
    this(ph.getId());
    pHolder_ = ph;
    update();
  }

  /**
   * Set <code>PanelHolder</code>. <code>PanelHolder</code> contains the layout information and
   * <code>DataGroup</code>s associated with the <code>Panel</code>.
   *
   * @param ph PanelHolder
   */
  public void setPanelHolder(PanelHolder ph) {
    pHolder_ = ph;
    setName(pHolder_.getId());
    update();
  }

  /**
   * Traverses the <code>PanelHolder</code> and adds any new objects and updates exisiting objects.
   */
  public void update() {
    float dpi = pHolder_.getPanelModel().getDpi();
    setBounds(pHolder_.getBounds());
    double width = getBounds().width / dpi;
    double height = getBounds().height / dpi;
    Dimension2D psize = new Dimension2D(width, height);
    if (pane_ != null) {
      boolean batch = pane_.isBatch();
      pane_.setBatch(true);
      if (labelLayer_ == null) {
        labelLayer_ = new Layer("Label Layer", psize);
        add(labelLayer_);
        labelLayer_.setPane(pane_);
      } else {
        labelLayer_.setSizeP(psize);
      }
      if (legendLayer_ == null) {
        legendLayer_ = new Layer("Legend Layer", psize);
        add(legendLayer_);
        legendLayer_.setPane(pane_);
      } else {
        legendLayer_.setSizeP(psize);
      }

      this.setBorder(pHolder_.getBorder());

      invalidate();
      try {
        updateLabels();
        updateLegends();
        updateDataGroups();
      } catch (ChildNotFoundException e) {
        e.printStackTrace();
      }
      pane_.setBatch(batch);
    }
  }

  private void updateLabels() throws ChildNotFoundException {
    Iterator lbIter = pHolder_.labelIterator();
    // remove deleted labels from layer
    // note cant remove from an Iterator!
    LayerChild[] children = labelLayer_.getChildren();
    for (int i = 0; i < children.length; i++) {
      LayerChild child = children[i];
      if (!pHolder_.hasLabel(child.getId())) {
        labelLayer_.removeChild(child);
      }
    }
    // add or update labels to layer
    while (lbIter.hasNext()) {
      SGLabel sgl = null;
      Label label = (Label) lbIter.next();
      LayerChild child = labelLayer_.findChild(label.getId());

      Point2D.Double loc = label.getLocationP();
      Rectangle2D.Double bnds = label.getBoundsP();
      double hgt;
      if (label.getOrientation() == SGLabel.HORIZONTAL) {
        hgt = label.getHeightP();
        switch (label.getJustification()) {
          case SGLabel.LEFT:
            break;
          case SGLabel.CENTER:
            loc.x = loc.x + bnds.getWidth() * 0.5;
            break;
          case SGLabel.RIGHT:
            loc.x = loc.x + bnds.getWidth();
            break;
        }
      } else {
        hgt = label.getWidthP();
        loc.x += bnds.getWidth();
        switch (label.getJustification()) {
          case SGLabel.LEFT:
            break;
          case SGLabel.CENTER:
            loc.y = loc.y + bnds.getHeight() * 0.5;
            break;
          case SGLabel.RIGHT:
            loc.y = loc.y + bnds.getHeight();
            break;
        }
      }
      if (child == null) {
        sgl =
            new SGLabel(
                label.getId(), label.getText(), hgt, loc, SGLabel.BOTTOM, label.getJustification());
        sgl.setVisible(label.isVisible());
        sgl.setSelectable(label.isSelectable());
        sgl.setOrientation(label.getOrientation());
        sgl.setColor(label.getColor());
        sgl.setFont(label.getFont());
        labelLayer_.addChild(sgl);
        label.setInstantiated(true);
      } else {
        sgl = (SGLabel) child;
        sgl.setText(label.getText());
        sgl.setHeightP(hgt);
        sgl.setLocationP(loc);
        sgl.setHAlign(label.getJustification());
        sgl.setVisible(label.isVisible());
        sgl.setSelectable(label.isSelectable());
        sgl.setOrientation(label.getOrientation());
        sgl.setColor(label.getColor());
        sgl.setFont(label.getFont());
      }
    }
  }

  private void updateLegends() {
    Iterator lgIter = pHolder_.legendIterator();
    while (lgIter.hasNext()) {
      DataKey key = null;
      Legend legend = (Legend) lgIter.next();
      LayerChild child = legendLayer_.findChild(legend.getId());
      Rectangle2D.Double boundsP = legend.getBoundsP();
      if (child == null) {
        switch (legend.getType()) {
          case Legend.LINE:
            key = new LineKey(legend.getLocationP(), LineKey.TOP, LineKey.LEFT);
            break;
          case Legend.COLOR:
            key =
                new ColorKey(
                    legend.getLocationP(),
                    new Dimension2D(boundsP.width, boundsP.height),
                    ColorKey.TOP,
                    ColorKey.LEFT);
            break;
          case Legend.VECTOR:
            key = new VectorKey(legend.getLocationP(), VectorKey.TOP, VectorKey.LEFT);
            break;
          case Legend.POINT:
            key =
                new PointCollectionKey(
                    legend.getLocationP(), PointCollectionKey.TOP, PointCollectionKey.LEFT);
            break;
        }
        key.setId(legend.getId());
        legendLayer_.addChild(key);
        legend.setInstantiated(true);
      } else {
        key = (DataKey) child;
        key.setLocationP(legend.getLocationP());
        //        key.setBoundsP(legend.getBoundsP());
      }
      if (legend.isColor()) {
        ColorKey ckey = (ColorKey) key;
        ckey.setSizeP(new Dimension2D(boundsP.width, boundsP.height));
        Ruler scale = ckey.getRuler();
        scale.setLabelColor(legend.getScaleColor());
        scale.setLineColor(legend.getScaleColor());
        scale.setLabelFont(legend.getScaleLabelFont());
        scale.setLabelHeightP(legend.getScaleLabelHeightP());
        scale.setLabelInterval(legend.getScaleLabelInterval());
        scale.setNumberSmallTics(legend.getScaleNumberSmallTics());
        scale.setLargeTicHeightP(legend.getScaleLargeTicHeightP());
        scale.setSmallTicHeightP(legend.getScaleSmallTicHeightP());
        scale.setSignificantDigits(legend.getScaleSignificantDigits());
        scale.setLabelFormat(legend.getScaleLabelFormat());
        scale.setVisible(legend.isScaleVisible());
        if (boundsP.width >= boundsP.height) {
          ckey.setOrientation(ColorKey.HORIZONTAL);
        } else {
          ckey.setOrientation(ColorKey.VERTICAL);
        }
      }
      key.setBorderStyle(legend.getBorderStyle());
      key.setColumns(legend.getColumns());
      key.setLineLengthP(legend.getLineLength());
      key.setVisible(legend.isVisible());
    }
  }

  private void updateDataGroups() {
    float dpi = pHolder_.getPanelModel().getDpi();
    setBounds(pHolder_.getBounds());
    double width = getBounds().width / dpi;
    double height = getBounds().height / dpi;
    Dimension2D psize = new Dimension2D(width, height);

    Iterator dgIter = pHolder_.dataGroupIterator();
    // remove delete DataGroups from panel/pane
    Component[] comps = getComponents();
    for (int i = 0; i < comps.length; i++) {
      if (comps[i] instanceof Layer) {
        Layer ly = (Layer) comps[i];
        if (ly != labelLayer_ && ly != legendLayer_) {
          if (!pHolder_.hasDataGroup(ly.getId())) {
            this.remove(ly);
          }
        }
      }
    }
    // create/update DataGroup layers
    while (dgIter.hasNext()) {
      DataGroup ag = (DataGroup) dgIter.next();
      // find DataGroupLayer
      DataGroupLayer dgl = findDataGroupLayer(ag.getId());
      if (dgl == null) {
        dgl = new DataGroupLayer(this, ag);
        add(dgl);
        dgl.setSizeP(psize);
        ag.setInstantiated(true);
        dataGroupLayerList_.put(dgl.getId(), dgl);
      } else {
        dgl.update();
      }
    }
  }

  /**
   * Find object associated with a MOUSE_DOWN event. The getObjectAt method scans through all the
   * objects associated with the Panel to find one whose bounding box contains the mouse location.
   *
   * <p>This method should not be called by a user.
   *
   * @param x mouse coordinate
   * @param y mouse coordinate
   * @param check if true requires that object isSelectable
   * @return object at location
   */
  public Object getObjectAt(int x, int y, boolean check) {
    Object obj = null;

    obj = labelLayer_.getObjectAt(x, y, check);
    if (obj != null) return obj;
    obj = legendLayer_.getObjectAt(x, y, check);
    if (obj != null) return obj;
    Iterator iter = dataGroupLayerList_.values().iterator();
    while (iter.hasNext()) {
      obj = ((DataGroupLayer) iter.next()).getObjectAt(x, y, check);
      if (obj != null) return obj;
    }
    return obj;
  }

  /**
   * Zoom <code>DataGroupLayer</code>s in <code>Panel</code>. Zoom to <code>Rectangle</code> if zoom
   * operation started within bounds.
   *
   * @param start start point
   * @param rect zoom rectangle
   */
  void zoomTo(Point start, Rectangle rect) {
    Iterator iter = dataGroupLayerList_.values().iterator();
    while (iter.hasNext()) {
      ((DataGroupLayer) iter.next()).zoomTo(start, rect);
    }
  }

  /**
   * Reset the zoom for <code>DataGroupLayer</code>s in this <code>Panel</code> if it contains the
   * point.
   *
   * @param x x device coordinate
   * @param y y device coordinate
   */
  void resetZoom(int x, int y) {
    Iterator iter = dataGroupLayerList_.values().iterator();
    while (iter.hasNext()) {
      ((DataGroupLayer) iter.next()).resetZoom(x, y);
    }
  }

  /** Reset the zoom for all <code>DataGroupLayer</code>s in this <code>Panel</code>. */
  public void resetZoom() {
    Iterator iter = dataGroupLayerList_.values().iterator();
    while (iter.hasNext()) {
      ((DataGroupLayer) iter.next()).resetZoom();
    }
  }

  /**
   * Set clipping for <code>DataGroupLayer</code>s in this <code>Panel</code>.
   *
   * @param clip if true clip data to bounds.
   */
  public void setClipping(boolean clip) {
    Iterator iter = dataGroupLayerList_.values().iterator();
    while (iter.hasNext()) {
      ((DataGroupLayer) iter.next()).setClipping(clip);
    }
  }

  /**
   * Does this <code>Panel</code> contain this Layer?
   *
   * @param id layer identification
   * @return true, if Layer is in Panel
   */
  public boolean hasLayer(String id) {
    if (id.equals(labelLayer_.getId())) return true;
    if (id.equals(legendLayer_.getId())) return true;
    Iterator iter = dataGroupLayerList_.values().iterator();
    while (iter.hasNext()) {
      if (id.equals(((DataGroupLayer) iter.next()).getId())) return true;
    }
    return false;
  }

  /**
   * Get <code>Layer</code> from this <code>Panel</code>. Returns <code>null</code> if Layer is not
   * found.
   *
   * @param id Layer identifier
   * @return Layer
   */
  public Layer getLayer(String id) {
    if (id.equals(labelLayer_.getId())) return labelLayer_;
    if (id.equals(legendLayer_.getId())) return legendLayer_;
    Iterator iter = dataGroupLayerList_.values().iterator();
    while (iter.hasNext()) {
      DataGroupLayer dgl = (DataGroupLayer) iter.next();
      if (id.equals(dgl.getId())) return dgl;
    }
    return null;
  }

  /**
   * Checks to see if a data id matches that data attached to the <code>Graph</code>.
   *
   * @param id data identifier
   * @return true if data is in Panel
   */
  public boolean isDataInPanel(String id) {
    Iterator iter = dataGroupLayerList_.values().iterator();
    while (iter.hasNext()) {
      DataGroupLayer dgl = (DataGroupLayer) iter.next();
      if (dgl.isDataInLayer(id)) return true;
    }
    return false;
  }

  /**
   * Get <code>Layer</code> from this <code>Panel</code>. Returns <code>null</code> if Layer is not
   * found.
   *
   * @param id SGTData identifier
   * @return Layer
   */
  public Layer getLayerFromDataId(String id) {
    Iterator iter = dataGroupLayerList_.values().iterator();
    while (iter.hasNext()) {
      DataGroupLayer dgl = (DataGroupLayer) iter.next();
      if (dgl.isDataInLayer(id)) return dgl;
    }
    return null;
  }

  /**
   * Find objects associated with a MOUSE_DOWN event. The getObjecstAt method scans through all the
   * objects associated with the Panel to find those whose bounding box contains the mouse location.
   *
   * <p>This method should not be called by a user.
   *
   * @param x mouse coordinate
   * @param y mouse coordinate
   * @param check if selectable
   * @return object array
   */
  public Object[] getObjectsAt(int x, int y, boolean check) {
    Object[] obj = null;
    Vector obList = new Vector();

    obj = labelLayer_.getObjectsAt(x, y, check);
    if (obj != null) {
      for (int i = 0; i < obj.length; i++) {
        obList.addElement(obj[i]);
      }
    }
    obj = legendLayer_.getObjectsAt(x, y, check);
    if (obj != null) {
      for (int i = 0; i < obj.length; i++) {
        obList.addElement(obj[i]);
      }
    }
    Iterator iter = dataGroupLayerList_.values().iterator();
    while (iter.hasNext()) {
      obj = ((DataGroupLayer) iter.next()).getObjectsAt(x, y, check);
      if (obj != null) {
        for (int i = 0; i < obj.length; i++) {
          obList.addElement(obj[i]);
        }
      }
    }

    return obList.toArray();
  }

  /**
   * Find <code>DataGroupLayer</code> in <code>Panel</code>.
   *
   * @param id DataGroupLayer identifier
   * @return DataGroupLayer
   */
  public DataGroupLayer findDataGroupLayer(String id) {
    return (DataGroupLayer) dataGroupLayerList_.get(id);
  }

  /**
   * Get the <code>JPane</code> the <code>Panel</code> is associated with.
   *
   * @return Refence to the <code>Pane</code>
   */
  public JPane getPane() {
    return pane_;
  }

  /**
   * Set the <code>Pane</code> the <code>Panel</code> is associated with. This method is called by
   * <code>Pane</code> when the <code>Pane.add</code> method is exectued.
   *
   * @param pane The <code>Pane</code>
   */
  @Override
  public void setPane(AbstractPane pane) {
    pane_ = (JPane) pane;
    update();
  }

  @Override
  public void draw(Graphics g) throws PaneNotFoundException {
    if (!pHolder_.isUsePageBackground()) {
      Color saved = g.getColor();
      Rectangle r = pHolder_.getBounds();
      g.setColor(pHolder_.getBackground());
      g.fillRect(r.x, r.y, r.width, r.height);
      g.setColor(saved);
    }
    Component[] comps = this.getComponents();
    for (int i = 0; i < comps.length; i++) {
      if (comps[i] instanceof LayerControl) {
        ((LayerControl) comps[i]).draw(g);
      }
    }
  }

  @Override
  public void drawDraggableItems(Graphics g) throws PaneNotFoundException {
    Component[] comps = this.getComponents();
    for (int i = 0; i < comps.length; i++) {
      if (comps[i] instanceof LayerControl) {
        ((LayerControl) comps[i]).drawDraggableItems(g);
      }
    }
  }

  /**
   * Add data to this <code>Panel</code> and <code>DataGroup</code>. legend can be <code>null</code>
   * .
   *
   * @param data SGTData to be added
   * @param attr Attribute
   * @param dataGroup DataGroup to add data to
   * @param legend Lenged for data
   * @throws DataTargetMismatchException
   */
  public void addData(SGTData data, Attribute attr, DataGroup dataGroup, Legend legend)
      throws DataTargetMismatchException {
    DataGroupLayer agLayer = findDataGroupLayer(dataGroup.getId());
    DataKey key = null;
    if (legend != null) key = (DataKey) legendLayer_.findChild(legend.getId());

    agLayer.addData(data, attr, key);
  }

  /**
   * Get the identifier for the <code>Panel</code>
   *
   * @return identifier/name
   */
  @Override
  public String getId() {
    return getName();
  }

  /**
   * Get a <code>String</code> representation of the <code>Layer</code>.
   *
   * @return <code>String</code> representation
   */
  @Override
  public String toString() {
    String name = getClass().getName();
    return name.substring(name.lastIndexOf(".") + 1) + ": " + pHolder_.getId();
  }
}
