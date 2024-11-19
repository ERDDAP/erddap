/*
 * $Id: PanelModel.java,v 1.8 2003/09/18 21:01:14 dwd Exp $
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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.beans.*;
import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A model that supports the <code>Panel</code> structure of a <code>Plot</code>. Information
 * includes the <code>DataGroup</code>s and <code>LayerChild</code>ren that are included in a <code>
 * Panel</code>. A <code>Panel</code> consist of several <code>Layer</code>s, at least one for each
 * <code>DataGroup</code>.
 *
 * <p>The layout of the <code>Panel</code>s will eventually be accomplished using the <code>
 * SpringLayout</code>. This will provide a flexible and easy method to position the <code>Panel
 * </code>s in arbitrary positions. Currently the <code>Panel</code>s are positioned in absolution
 * location.
 *
 * <p>Some classes have been omitted for display purposes.
 *
 * <p style="text-align:center;"><img src="images/PanelModelSimple.png"
 * style="vertical-align:bottom; border:0;">
 *
 * @author Donald Denbo
 * @version $Revision: 1.8 $, $Date: 2003/09/18 21:01:14 $
 * @since 3.0
 * @stereotype bean
 */
public class PanelModel
    implements Serializable, ChangeListener, ComponentListener, PropertyChangeListener {
  private transient PropertyChangeSupport support_ = new PropertyChangeSupport(this);
  private Map panelList = new HashMap(4);
  private float dpi = 72.0f; // pixel to user scale (dots per inch);
  private Dimension pageSize = new Dimension(400, 300);
  private Color pageBackgroundColor = Color.white;
  private int printHAlign = AbstractPane.CENTER;
  private int printVAlign = AbstractPane.TOP;
  private int printMode = AbstractPane.DEFAULT_SCALE;
  private Point printOrigin = new Point(0, 0);
  private boolean printBorders = false;
  private boolean printWhitePage = true;
  private transient boolean batch = false;
  private transient boolean modified = false;
  //
  private transient ChangeEvent changeEvent_ = new ChangeEvent(this);
  private transient Vector changeListeners;

  /**
   * @label page
   */
  private transient Page page;

  /**
   * @link aggregation
   * @supplierCardinality 1..*
   * @label panelList
   */
  /*#PanelHolder lnkPanelHolder;*/

  /** <code>PanelModel</code> constructor. */
  public PanelModel() {}

  /**
   * Create a new <code>PanelModel</code> from an object serialized using <code>XMLEncoder</code>.
   * For example,
   *
   * <pre>
   *   PanelModel pModel;
   *   Page page = new Page();
   *       try {
   *    pModel = PanelModel.loadFromXML(new BufferedInputStream(
   *                       new FileInputStream(outpath)));
   *    page.setPanelModel(pModel);
   *  } catch (FileNotFoundException fnfe) {
   *    JOptionPane.showMessageDialog(this, "Error openning file",
   *                                  "File Open Error", JOptionPane.ERROR_MESSAGE);
   *  } catch (InvalidObjectException ioe) {
   *    JOptionPane.showMessageDialog(this, "File does not contain a PanelModel",
   *                                  "PanelModel Not Found",
   *                                  JOptionPane.ERROR_MESSAGE);
   *  }
   *
   * </pre>
   *
   * @param is InputStream
   * @return PanelModel object
   * @throws InvalidObjectException
   * @see java.beans.XMLEncoder
   */
  public static PanelModel loadFromXML(InputStream is) throws InvalidObjectException {
    PanelModel pModel = null;
    XMLDecoder xd = new XMLDecoder(is);
    Object obj = xd.readObject();
    xd.close();
    if (obj instanceof PanelModel) {
      pModel = (PanelModel) obj;
      pModel.repair();
    } else {
      throw new InvalidObjectException("XML file does not contain a PanelModel");
    }
    return pModel;
  }

  /**
   * Save <code>PanelModel</code> and its supporting classes as a XML stream using the <code>
   * XMLEncoder</code>.
   *
   * @param os OutputStream
   */
  public void saveToXML(OutputStream os) {
    XMLEncoder xe = new XMLEncoder(os);
    xe.writeObject(this);
    xe.close();
  }

  /**
   * Create a new <code>Panel</code> and add to <code>PanelModel</code>.
   *
   * @param id Panel identifier
   * @param bounds bounds of Panel
   * @param labels Label Map
   * @param dataGroups DataGroup Map
   * @param legends Legend Map
   */
  public void addPanel(String id, Rectangle bounds, Map labels, Map dataGroups, Map legends) {
    PanelHolder ph = new PanelHolder(id, this, bounds, labels, dataGroups, legends);
    addPanel(ph);
  }

  /**
   * Add a <code>PanelHolder</code> to <code>PanelModel</code>.
   *
   * @param ph PanelHolder
   */
  public void addPanel(PanelHolder ph) {
    int sze = panelList.size();
    ph.addChangeListener(this);
    panelList.put(ph.getId(), ph);
    firePropertyChange("panelList", sze, panelList.size());
  }

  /**
   * Remove Panel from <code>PanelModel</code>.
   *
   * @param ph PanelHolder
   */
  public void removePanel(PanelHolder ph) {
    int sze = panelList.size();
    ph.removeChangeListener(this);
    panelList.remove(ph.getId());
    firePropertyChange("panelList", sze, panelList.size());
  }

  /**
   * Get <code>Iterator</code> of <code>PanelHolder</code> objects.
   *
   * @return Iterator
   */
  public Iterator panelIterator() {
    return panelList.values().iterator();
  }

  /**
   * Get number of <code>PanelHolder</code> objects in <code>Map</code>.
   *
   * @return count
   */
  public int getPanelCount() {
    return panelList.size();
  }

  /**
   * Does the <code>PanelModel</code> contain this <code>PanelHolder</code>?
   *
   * @param id PanelHolder identifier
   * @return true, if PanelHolder exists.
   */
  public boolean hasPanelHolder(String id) {
    return findPanelHolder(id) != null;
  }

  /**
   * Find the <code>PanelHolder</code> from its identifier.
   *
   * @param id PanelHolder identifier
   * @return PanelHolder
   */
  public PanelHolder findPanelHolder(String id) {
    return (PanelHolder) panelList.get(id);
  }

  /**
   * Add property change listener.
   *
   * @param l property change listener
   */
  public void addPropertyChangeListener(PropertyChangeListener l) {
    support_.addPropertyChangeListener(l);
  }

  /**
   * Listen for specific property change.
   *
   * @param name property name
   * @param l property change listner
   */
  public void addPropertyChangeListener(String name, PropertyChangeListener l) {
    support_.addPropertyChangeListener(name, l);
  }

  /**
   * Remove property change listener.
   *
   * @param l property change listener
   */
  public void removePropertyChangeListener(PropertyChangeListener l) {
    support_.removePropertyChangeListener(l);
  }

  /**
   * Remove specific property change listener
   *
   * @param name property name
   * @param l property change listener
   */
  public void removePropertyChangeListener(String name, PropertyChangeListener l) {
    support_.removePropertyChangeListener(name, l);
  }

  /**
   * Get dots per inch value.
   *
   * @return dots per inch
   */
  public float getDpi() {
    return dpi;
  }

  /**
   * Set dots per inch. Fires property "dpi" when changed.
   *
   * @param dpi dots per inch
   */
  public void setDpi(float dpi) {
    float saved = this.dpi;
    this.dpi = dpi;
    if (saved != this.dpi) firePropertyChange("dpi", Float.valueOf(saved), Float.valueOf(this.dpi));
  }

  /**
   * <code>ChangeListner</code> callback.
   *
   * @param e ChangeEvent
   */
  @Override
  public void stateChanged(ChangeEvent e) {
    Object source = e.getSource();
    firePropertyChange("panel", null, source);
  }

  private void firePropertyChange(String name, Object oldValue, Object newValue) {
    if (batch) {
      modified = true;
    } else {
      support_.firePropertyChange(name, oldValue, newValue);
    }
  }

  private void firePropertyChange(String name, int oldValue, int newValue) {
    if (batch) {
      modified = true;
    } else {
      support_.firePropertyChange(name, oldValue, newValue);
    }
  }

  /**
   * Set batching for changes to <code>PanelModel</code>. Fires property "batch" when <code>
   * PanelModel</code> has changed and batch is set to false.
   *
   * @param batch batch value
   */
  public void setBatch(boolean batch) {
    this.batch = batch;
    if (!this.batch && modified) {
      modified = false;
      support_.firePropertyChange("batch", null, null);
    }
  }

  /**
   * Registers ChangeListeners and sets back references in <code>PanelModel</code> and its children.
   */
  private void repair() {
    // traverse PanelModel and create all event links
    Iterator iter = panelIterator();
    while (iter.hasNext()) {
      PanelHolder ph = (PanelHolder) iter.next();
      ph.removeAllChangeListeners();
      ph.addChangeListener(this);
      ph.setPanelModel(this);
      Iterator dgIter = ph.dataGroupIterator();
      while (dgIter.hasNext()) {
        DataGroup dg = (DataGroup) dgIter.next();
        dg.removeAllChangeListeners();
        dg.addChangeListener(ph);
        dg.setPanelHolder(ph);
        dg.getXAxisHolder().removeAllChangeListeners();
        dg.getXAxisHolder().addChangeListener(dg);
        dg.getXAxisHolder().setDataGroup(dg);
        dg.getYAxisHolder().removeAllChangeListeners();
        dg.getYAxisHolder().addChangeListener(dg);
        dg.getYAxisHolder().setDataGroup(dg);
      }
      Iterator lbIter = ph.labelIterator();
      while (lbIter.hasNext()) {
        Label lb = (Label) lbIter.next();
        lb.removeAllChangeListeners();
        lb.addChangeListener(ph);
        lb.setPanelHolder(ph);
      }
      Iterator lgIter = ph.legendIterator();
      while (lgIter.hasNext()) {
        Legend lg = (Legend) lgIter.next();
        lg.removeAllChangeListeners();
        lg.addChangeListener(ph);
        lg.setPanelHolder(ph);
      }
    }
  }

  /**
   * Is batching turned on?
   *
   * @return true, if batch is on
   */
  public boolean isBatch() {
    return batch;
  }

  /**
   * Set <code>Page</code> parent. Called from <code>Page</code> when added to <code>Page</code>
   * object.
   *
   * @param page Page
   */
  public void setPage(Page page) {
    if (this.page != null) removePropertyChangeListener(this.page);
    this.page = page;
    addPropertyChangeListener(this.page);
    page.addComponentListener(this);
    page.addPropertyChangeListener(this);
    pageSize = page.getSize();
  }

  /**
   * Get Page parent
   *
   * @return Page
   */
  public Page getPage() {
    return page;
  }

  /**
   * Get <code>Map</code> containing <code>PanelHolder</code> objects.
   *
   * @return Map
   */
  public Map getPanelList() {
    return panelList;
  }

  /**
   * Set <code>PanelHolder</code> <code>Map</code>.
   *
   * @param panelList Map
   */
  public void setPanelList(Map panelList) {
    if (Page.DEBUG) System.out.println("PanelModel.setPanelList(): size = " + panelList.size());
    this.panelList = panelList;
    Iterator iter = panelIterator();
    while (iter.hasNext()) {
      PanelHolder ph = (PanelHolder) iter.next();
      ph.addChangeListener(this);
    }
    firePropertyChange("panelList", 0, panelList.size()); // should this be fired?
  }

  /**
   * Get <code>Page</code> size.
   *
   * @return Page dimensions
   */
  public Dimension getPageSize() {
    return pageSize;
  }

  /**
   * Set <code>Page</code> size.
   *
   * @param pageSize Page dimension
   */
  public void setPageSize(Dimension pageSize) {
    Dimension saved = this.pageSize;
    this.pageSize = pageSize;
    if (!saved.equals(this.pageSize)) {
      firePropertyChange("pageSize", saved, pageSize);
      fireStateChanged();
    }
  }

  /**
   * Listener to update <code>Page</code> size if it changes.
   *
   * @param e ComponentEvent
   */
  @Override
  public void componentResized(ComponentEvent e) {
    pageSize = page.getSize();
  }

  /** Unused. */
  @Override
  public void componentMoved(ComponentEvent e) {}

  /** Unused. */
  @Override
  public void componentShown(ComponentEvent e) {}

  /** Unused. */
  @Override
  public void componentHidden(ComponentEvent e) {}

  /**
   * Get the background color for <code>Page</code>.
   *
   * @return color
   */
  public Color getPageBackgroundColor() {
    return pageBackgroundColor;
  }

  /**
   * Set <code>Page</code> background color.
   *
   * @param pageBackgroundColor background color
   */
  public void setPageBackgroundColor(Color pageBackgroundColor) {
    Color saved = this.pageBackgroundColor;
    this.pageBackgroundColor = pageBackgroundColor;
    if (!saved.equals(this.pageBackgroundColor)) {
      firePropertyChange("pageBackgroundColor", saved, this.pageBackgroundColor);
      fireStateChanged();
    }
  }

  /**
   * Print borders?
   *
   * @return true, if borders will be printed
   */
  public boolean isPrintBorders() {
    return printBorders;
  }

  /**
   * Print with white background?
   *
   * @return true, print on white background
   */
  public boolean isPrintWhitePage() {
    return printWhitePage;
  }

  /**
   * Set the print borders property. Default = false.
   *
   * @param printBorders true to print borders
   */
  public void setPrintBorders(boolean printBorders) {
    boolean saved = this.printBorders;
    this.printBorders = printBorders;
    if (saved != this.printBorders)
      firePropertyChange(
          "printBorders", Boolean.valueOf(saved), Boolean.valueOf(this.printBorders));
  }

  /**
   * Set the print on white background property. Default = true.
   *
   * @param printWhitePage true to use white for background color
   */
  public void setPrintWhitePage(boolean printWhitePage) {
    boolean saved = this.printWhitePage;
    this.printWhitePage = printWhitePage;
    if (saved != this.printWhitePage)
      firePropertyChange(
          "printWhitePage", Boolean.valueOf(saved), Boolean.valueOf(this.printWhitePage));
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    if (Page.DEBUG) System.out.println("PanelModel.propertyChange(" + evt.getPropertyName() + ")");
  }

  /**
   * Remove changelistener.
   *
   * @param l changelistener
   */
  public synchronized void removeChangeListener(ChangeListener l) {
    if (changeListeners != null && changeListeners.contains(l)) {
      Vector v = (Vector) changeListeners.clone();
      v.removeElement(l);
      changeListeners = v;
    }
  }

  /**
   * Remove all <code>ChangeListener</code>s that implement the <code>DesignListener</code>
   * interface.
   *
   * @see DesignListener
   */
  public synchronized void removeDesignChangeListeners() {
    if (changeListeners != null) {
      Vector v = (Vector) changeListeners.clone();
      Iterator iter = v.iterator();
      while (iter.hasNext()) {
        Object obj = iter.next();
        if (obj instanceof DesignListener) changeListeners.removeElement(obj);
      }
    }
  }

  /**
   * Add changelistener
   *
   * @param l changelistener
   */
  public synchronized void addChangeListener(ChangeListener l) {
    Vector v = changeListeners == null ? new Vector(2) : (Vector) changeListeners.clone();
    if (!v.contains(l)) {
      v.addElement(l);
      changeListeners = v;
    }
  }

  protected void fireStateChanged() {
    if (changeListeners != null) {
      Vector listeners = changeListeners;
      int count = listeners.size();
      for (int i = 0; i < count; i++) {
        ((ChangeListener) listeners.elementAt(i)).stateChanged(changeEvent_);
      }
    }
  }

  /**
   * Get horizontal alignment for printing.
   *
   * @return horizontal alignment
   */
  public int getPrintHAlign() {
    return printHAlign;
  }

  /**
   * Set horizontal alignment for printing. Legal values are AbstractPane.LEFT, AbstractPane.CENTER,
   * AbstractPane.RIGHT, and AbstractPane.SPECIFIED_LOCATION. Default = AbstractPane.CENTER.
   *
   * @param pageHAlign horizontal alignment
   * @see gov.noaa.pmel.sgt.AbstractPane#LEFT AbstractPane.LEFT
   * @see gov.noaa.pmel.sgt.AbstractPane#CENTER AbstractPane.CENTER
   * @see gov.noaa.pmel.sgt.AbstractPane#RIGHT AbstractPane.RIGHT
   * @see gov.noaa.pmel.sgt.AbstractPane#SPECIFIED_LOCATION AbstractPane.SPECIFIED_LOCATION
   */
  public void setPrintHAlign(int printHAlign) {
    int saved = this.printHAlign;
    this.printHAlign = printHAlign;
    if (saved != this.printHAlign) firePropertyChange("printHAlign", saved, this.printHAlign);
  }

  /**
   * Set page origin for printing. Will be used if the horizontal or vertical alignment is
   * AbstractPane.SPECIFIED_LOCATION. Default = (0,0).
   *
   * @param pageOrigin page origin
   * @see gov.noaa.pmel.sgt.AbstractPane AbstractPane
   */
  public void setPrintOrigin(Point printOrigin) {
    Point saved = this.printOrigin;
    this.printOrigin = printOrigin;
    if (!saved.equals(this.printOrigin)) firePropertyChange("printOrigin", saved, this.printOrigin);
  }

  /**
   * Get page origin for printing.
   *
   * @return page origin.
   */
  public Point getPrintOrigin() {
    return printOrigin;
  }

  /**
   * Get vertical alignment for printing.
   *
   * @return vertical alignment.
   */
  public int getPrintVAlign() {
    return printVAlign;
  }

  /**
   * Set vertical alignment for printing. Legal values are AbstractPane.TOP, AbstractPane.MIDDLE,
   * AbstractPane.BOTTOM, and AbstractPane.SPECIFIED_LOCATION. Default = AbstractPane.TOP;
   *
   * @param pageVAlign vertical alignment
   * @see gov.noaa.pmel.sgt.AbstractPane#TOP AbstractPane.TOP
   * @see gov.noaa.pmel.sgt.AbstractPane#MIDDLE AbstractPane.MIDDLE
   * @see gov.noaa.pmel.sgt.AbstractPane#BOTTOM AbstractPane.BOTTOM
   * @see gov.noaa.pmel.sgt.AbstractPane#SPECIFIED_LOCATION AbstractPane.SPECIFIED_LOCATION
   */
  public void setPrintVAlign(int printVAlign) {
    int saved = this.printVAlign;
    this.printVAlign = printVAlign;
    if (saved != this.printVAlign) firePropertyChange("printVAlign", saved, this.printVAlign);
  }

  /**
   * Get printing scale mode.
   *
   * @return scale mode
   */
  public int getPrintScaleMode() {
    return printMode;
  }

  /**
   * Set printing scale mode. Legal values are AbstractPane.DEFAULT_SCALE, AbstractPane.TO_FIT, and
   * AbstractPane.SHRINK_TO_FIT. Default = AbstractPane.DEFAULT_SCALE.
   *
   * @param mode scale mode
   * @see gov.noaa.pmel.sgt.AbstractPane#DEFAULT_SCALE AbstractPane.DEFAULT_SCALE
   * @see gov.noaa.pmel.sgt.AbstractPane#TO_FIT AbstractPane.TO_FIT
   * @see gov.noaa.pmel.sgt.AbstractPane#SHRINK_TO_FIT AbstractPane.SHRINK_TO_FIT
   */
  public void setPrintScaleMode(int mode) {
    int saved = this.printMode;
    this.printMode = mode;
    if (saved != this.printMode) firePropertyChange("printScaleMode", saved, this.printMode);
  }
}
