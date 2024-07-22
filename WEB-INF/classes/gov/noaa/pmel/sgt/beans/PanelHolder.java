/*
 * $Id: PanelHolder.java,v 1.4 2003/08/27 23:29:40 dwd Exp $
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

import java.awt.Color;
import java.awt.Rectangle;
import java.beans.*;
import java.io.*;
import java.util.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.*;

/**
 * Object to hold all the objects necessary to describe a <code>Panel</code>. Associated objects
 * include <code>Label</code>, <code>DataGroup</code>, and <code>Legend</code>.
 *
 * @author Donald Denbo
 * @version $Revision: 1.4 $, $Date: 2003/08/27 23:29:40 $
 * @since 3.0
 * @see Label
 * @see Panel
 * @see PanelModel PanelModel for UML diagram
 * @see DataGroup
 * @see Legend
 */
public class PanelHolder implements ChangeListener, Serializable {
  private String id = "";
  private Rectangle bounds = new Rectangle(0, 0, 100, 50);

  /**
   * @link aggregation
   * @supplierCardinality *
   * @label labels
   */
  /*#Label lnkLabel; */
  private Map labels_ = new HashMap(5);

  /**
   * @link aggregation
   * @supplierCardinality 1..*
   * @label dataGroups
   */
  /*#DataGroup lnkDataGroup; */
  private Map dataGroups_ = new HashMap(2);

  /**
   * @link aggregation
   * @supplierCardinality *
   * @label legends
   */
  /*#Legend lnkLegend; */
  private Map legends_ = new HashMap(1);

  /**
   * @label pModel
   */
  private transient PanelModel pModel_ = null;

  private transient ChangeEvent changeEvent_ = new ChangeEvent(this);
  private transient Vector changeListeners;
  private boolean visible = true;
  private transient boolean instantiated = false;
  private Border border = new LineBorder(Color.gray, 2);
  private Color background = Color.white;
  private boolean usePageBackground = true;

  static {
    try {
      BeanInfo info = Introspector.getBeanInfo(PanelHolder.class);
      PropertyDescriptor[] descriptors = info.getPropertyDescriptors();
      for (int i = 0; i < descriptors.length; i++) {
        PropertyDescriptor pd = descriptors[i];
        if (pd.getName().equals("instantiated")) {
          pd.setValue("transient", Boolean.TRUE);
        } else if (pd.getName().equals("panelModel")) {
          pd.setValue("transient", Boolean.TRUE);
        }
      }
    } catch (IntrospectionException ie) {
      ie.printStackTrace();
    }
  }

  /**
   * Default constructor. Sets <code>PanelHolder</code> identifier and <code>PanelModel</code>
   * parent to <code>null</code>.
   */
  public PanelHolder() {
    this(null, null);
  }

  /**
   * <code>PanelHolder</code> constructor.
   *
   * @param id PanelHolder identifier
   * @param pModel PanelModel parent
   */
  public PanelHolder(String id, PanelModel pModel) {
    this.id = id;
    pModel_ = pModel;
  }

  /**
   * <code>PanelHolder</code> constructor. This constructor has arguments to pass <code>Map</code>s
   * of <code>Label</code>, <code>DataGroup</code>, and <code>Legend</code> objects. The <code>Map
   * </code> key is the objects identifier.
   *
   * @param id PanelHolder identifier
   * @param pModel PanelModel parent
   * @param bounds bounds
   * @param labels Map containing Labels
   * @param dataGroups Map containing DataGroups
   * @param legends Map contiaining Legends
   */
  public PanelHolder(
      String id, PanelModel pModel, Rectangle bounds, Map labels, Map dataGroups, Map legends) {
    this.id = id;
    pModel_ = pModel;
    this.bounds = bounds;
    if (labels != null) {
      labels_ = new HashMap(labels);
    }
    if (dataGroups != null) {
      dataGroups_ = new HashMap(dataGroups);
    }
    if (legends != null) {
      legends_ = new HashMap(legends);
    }
  }

  /**
   * Set the <code>PanelModel</code> parent.
   *
   * @param pModel PanelModel
   */
  public void setPanelModel(PanelModel pModel) {
    if (pModel_ != null) removeChangeListener(pModel_);
    pModel_ = pModel;
    addChangeListener(pModel_);
  }

  /**
   * Get the <code>PanelModel</code>.
   *
   * @return PanelModel
   */
  public PanelModel getPanelModel() {
    return pModel_;
  }

  /**
   * Add <code>Label</code> to the <code>PanelHolder</code>.
   *
   * @param label Label
   */
  public void addLabel(Label label) {
    label.addChangeListener(this);
    label.setPanelHolder(this);
    labels_.put(label.getId(), label);
    fireStateChanged();
  }

  /**
   * Remove <code>Label</code> from the <code>PanelHolder</code>.
   *
   * @param label Label
   */
  public void removeLabel(Label label) {
    label.removeAllChangeListeners();
    labels_.remove(label.getId());
    fireStateChanged();
  }

  /**
   * Add <code>DataGroup</code> to the <code>PanelHolder</code>.
   *
   * @param dataGroup DataGroup
   */
  public void addDataGroup(DataGroup dataGroup) {
    dataGroup.addChangeListener(this);
    dataGroups_.put(dataGroup.getId(), dataGroup);
    fireStateChanged();
  }

  /**
   * Remove <code>DataGroup</code> from the <code>PanelHolder</code>.
   *
   * @param dataGroup DataGroup
   */
  public void removeDataGroup(DataGroup dataGroup) {
    dataGroup.removeAllChangeListeners();
    dataGroup.getXAxisHolder().removeAllChangeListeners();
    dataGroup.getYAxisHolder().removeAllChangeListeners();
    dataGroups_.remove(dataGroup.getId());
    fireStateChanged();
  }

  /**
   * Add <code>Legend</code> to the <code>PanelHolder</code>.
   *
   * @param legend Legend
   */
  public void addLegend(Legend legend) {
    legend.addChangeListener(this);
    legend.setPanelHolder(this);
    legends_.put(legend.getId(), legend);
    fireStateChanged();
  }

  /**
   * Remove <code>Legend</code> from the <code>PanelHolder</code>.
   *
   * @param legend Legend
   */
  public void removeLegend(Legend legend) {
    legend.removeAllChangeListeners();
    legends_.remove(legend.getId());
    fireStateChanged();
  }

  /**
   * Set <code>PanelHolder</code> identifier.
   *
   * @param id identifier
   */
  public void setId(String id) {
    String old = this.id;
    this.id = id;
    if (old == null || !old.equals(this.id)) fireStateChanged();
  }

  /**
   * Get <code>PanelHolder</code> identifier
   *
   * @return identifier
   */
  public String getId() {
    return id;
  }

  /**
   * Get number of <code>Label</code> objects in <code>Map</code>.
   *
   * @return number of Labels
   */
  public int getLabelSize() {
    return labels_.size();
  }

  /**
   * Get number of <code>DataGroup</code> objects in <code>Map</code>.
   *
   * @return number of DataGroups
   */
  public int getDataGroupSize() {
    return dataGroups_.size();
  }

  /**
   * Get number of <code>Legend</code> objects in <code>Map</code>.
   *
   * @return number of Legends
   */
  public int getLegendSize() {
    return legends_.size();
  }

  /**
   * Get an <code>Iterator</code> of <code>Label</code> objects.
   *
   * @return Iterator
   */
  public Iterator labelIterator() {
    return labels_.values().iterator();
  }

  /**
   * Get an <code>Iterator</code> of <code>DataGroup</code> objects.
   *
   * @return Iterator
   */
  public Iterator dataGroupIterator() {
    return dataGroups_.values().iterator();
  }

  /**
   * Get an <code>Iterator</code> of <code>Legend</code> objects.
   *
   * @return Iterator
   */
  public Iterator legendIterator() {
    return legends_.values().iterator();
  }

  /**
   * Set bounds of <code>Panel</code>.
   *
   * @param bounds bounds
   */
  public void setBounds(Rectangle bounds) {
    Rectangle old = this.bounds;
    this.bounds = bounds;
    fireStateChanged();
  }

  /**
   * Get bounds of <code>Panel</code>.
   *
   * @return bounds
   */
  public Rectangle getBounds() {
    return bounds;
    /**
     * @todo bounds isn't cloned should be see note
     */
    //    return (Rectangle)bounds.clone();  // the clone didn't work with XMLEncoder for some
    // reason
  }

  /** Remove all <code>ChangeListener</code>s. */
  public void removeAllChangeListeners() {
    changeListeners = null;
  }

  /**
   * <code>ChangeListner</code> callback.
   *
   * @param e ChangeEvent
   */
  @Override
  public void stateChanged(ChangeEvent e) {
    if (changeListeners != null) {
      Vector listeners = changeListeners;
      int count = listeners.size();
      for (int i = 0; i < count; i++) {
        ((ChangeListener) listeners.elementAt(i)).stateChanged(e);
      }
    }
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
   * Set <code>Panel</code> visible.
   *
   * @param visible true if visible
   */
  public void setVisible(boolean visible) {
    boolean saved = this.visible;
    this.visible = visible;
    if (saved != this.visible) fireStateChanged();
  }

  /**
   * Is <code>Panel</code> visible?
   *
   * @return true, if Panel is visible
   */
  public boolean isVisible() {
    return visible;
  }

  /**
   * Set instantiated. Once associated <code>Panel</code> object has been created this property is
   * set true. Used internally.
   *
   * @param instantiated true if instantiated
   */
  public void setInstantiated(boolean instantiated) {
    this.instantiated = instantiated;
  }

  /**
   * Is panel instantiated?
   *
   * @return true, if Panel instantiated
   */
  public boolean isInstantiated() {
    return instantiated;
  }

  /**
   * Find a <code>DataGroup</code> by identifier.
   *
   * @param id identifier
   * @return DataGroup
   */
  public DataGroup findDataGroup(String id) {
    return (DataGroup) dataGroups_.get(id);
  }

  /**
   * Find <code>Label</code> by identifier.
   *
   * @param id identifier
   * @return Label
   */
  public Label findLabel(String id) {
    return (Label) labels_.get(id);
  }

  /**
   * Find <code>Legend</code> by identifier.
   *
   * @param id identifier
   * @return Legend
   */
  public Legend findLegend(String id) {
    return (Legend) legends_.get(id);
  }

  /**
   * Does <code>PanelHolder</code> contain <code>DataGroup</code>?
   *
   * @param id DataGroup identifier
   * @return true, if DataGroup exists
   */
  public boolean hasDataGroup(String id) {
    return dataGroups_.containsKey(id);
  }

  /**
   * Does <code>PanelHolder</code> contain <code>Label</code>?
   *
   * @param id Label identifier
   * @return Label
   */
  public boolean hasLabel(String id) {
    return labels_.containsKey(id);
  }

  /**
   * Does <code>PanelHolder</code> contain <code>Legend</code>?
   *
   * @param id Legend identifier
   * @return Legend
   */
  public boolean hasLegend(String id) {
    return legends_.containsKey(id);
  }

  /**
   * Return <code>Border</code> for <code>Panel</code>.
   *
   * @return Border
   */
  public Border getBorder() {
    return border;
  }

  /**
   * Set <code>Border</code> for <code>Panel</code>. Default = LineBorder(gray, 2)
   *
   * @param border Border
   */
  public void setBorder(Border border) {
    Border saved = this.border;
    this.border = border;
    if (saved == null || !saved.equals(this.border)) fireStateChanged();
  }

  /**
   * Get <code>Map</code> of </code>Label</code>s.
   *
   * @return Map
   */
  public Map getLabels() {
    return labels_;
  }

  /**
   * Set <code>Map</code> of </code>Label</code>s. Key in <code>Map</code> contains <code>Label
   * </code> identifier.
   *
   * @param labels Label Map
   */
  public void setLabels(Map labels) {
    labels_ = labels;
    Iterator iter = labelIterator();
    while (iter.hasNext()) {
      ((Label) iter.next()).addChangeListener(this);
    }
    fireStateChanged();
  }

  /**
   * Get <code>Map</code> containing <code>Legend</code>s.
   *
   * @return Map
   */
  public Map getLegends() {
    return legends_;
  }

  /**
   * Set <code>Map</code> of </code>Legend</code>s. Key in <code>Map</code> contains <code>Legend
   * </code> identifier.
   *
   * @param legends Legend Map
   */
  public void setLegends(Map legends) {
    legends_ = legends;
    Iterator iter = legendIterator();
    while (iter.hasNext()) {
      ((Legend) iter.next()).addChangeListener(this);
    }
    fireStateChanged();
  }

  /**
   * Get <code>Map</code> containing <code>DataGroup</code>s.
   *
   * @return Map
   */
  public Map getDataGroups() {
    return dataGroups_;
  }

  /**
   * Set <code>Map</code> of </code>DataGroup</code>s. Key in <code>Map</code> contains <code>
   * DataGroup</code> identifier.
   *
   * @param dataGroups DataGroup Map
   */
  public void setDataGroups(Map dataGroups) {
    dataGroups_ = dataGroups;
    Iterator iter = dataGroupIterator();
    while (iter.hasNext()) {
      ((DataGroup) iter.next()).addChangeListener(this);
    }
    fireStateChanged();
  }

  /**
   * Get background color. Will use the page background if usePageBackground property is true.
   *
   * @return color
   */
  public Color getBackground() {
    return background;
  }

  /**
   * Set the <code>Panel</code> background color.
   *
   * @param background color
   */
  public void setBackground(Color background) {
    Color saved = this.background;
    this.background = background;
    if (!saved.equals(this.background)) fireStateChanged();
  }

  /**
   * Use the page background color?
   *
   * @return true if using page background color
   */
  public boolean isUsePageBackground() {
    return usePageBackground;
  }

  /**
   * Set the <code>Panel</code> to use the <code>Page</code> background color.
   *
   * @param pageBackground true to use page background color
   */
  public void setUsePageBackground(boolean pageBackground) {
    boolean saved = this.usePageBackground;
    this.usePageBackground = pageBackground;
    if (saved != this.usePageBackground) fireStateChanged();
  }
}
