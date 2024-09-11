/*
 * $Id: Page.java,v 1.12 2003/09/18 21:01:14 dwd Exp $
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

import gov.noaa.pmel.sgt.JPane;
// import gov.noaa.pmel.util.SoTDomain;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.beans.*;
import java.io.*;
import java.util.Iterator;
import javax.swing.JComponent;
import javax.swing.border.Border;

/**
 * Main SGT JavaBean in conjunction with a <code>DataModel</code> and <code>PanelModel</code> will
 * create a graphic.
 *
 * <p>Some classes have been omitted for display purposes.
 *
 * <p style="text-align:center;"><img src="images/RunTimeSimple.png" style="vertical-align:bottom;
 * border:0;">
 *
 * @author Donald Denbo
 * @version $Revision: 1.12 $, $Date: 2003/09/18 21:01:14 $
 * @since 3.0
 * @stereotype bean
 */
public class Page extends JComponent implements PropertyChangeListener, Serializable, Printable {
  public static final boolean DEBUG = false;

  /**
   * @link aggregation
   * @supplierCardinality 1
   * @undirected
   * @label dataModel
   */
  private DataModel dataModel;

  /**
   * @link aggregation
   * @supplierCardinality 1
   * @undirected
   * @label panelModel
   */
  private PanelModel panelModel;

  /**
   * @link aggregation
   * @associates <{Panel}>
   * @supplierCardinality 1..*
   * @clientCardinality 1
   * @undirected
   */
  //  transient private Vector panels_;

  /**
   * @link aggregationByValue
   * @clientCardinality 1
   * @supplierCardinality 1
   * @undirected
   * @label pane
   */
  private JPane pane_;

  private String name;
  private transient boolean isDesignTime_ = Beans.isDesignTime();

  /** <code>Page</code> constructor. */
  public Page() {
    pane_ = new JPane("SGT Bean Pane", new Dimension(200, 200));
    pane_.addPropertyChangeListener(this);
    pane_.setBackground(Color.white);
    pane_.setOpaque(true);
    this.setOpaque(true);
    this.setLayout(new BorderLayout());
    this.setBackground(Color.white);
    this.add(pane_, BorderLayout.CENTER);
    pane_.addMouseListener(new PageMouse());
  }

  /**
   * Get <code>JPane</code> associated with <code>Page</code>
   *
   * @return JPane
   */
  public JPane getJPane() {
    return pane_;
  }

  /**
   * Get <code>JPane</code> size.
   *
   * @return JPane size
   */
  public Dimension getJPaneSize() {
    return pane_.getSize();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
  }

  /**
   * Get <code>DataModel</code>.
   *
   * @return DataModel
   * @see DataModel
   */
  public DataModel getDataModel() {
    return dataModel;
  }

  /**
   * Set <code>DataModel</code>. <code>DataModel</code> is used to define the relationship between
   * <code>SGTData</code> and the graphical representation.
   *
   * @param dataModel DataModel
   * @see DataModel
   */
  public void setDataModel(DataModel dataModel) {
    if (DEBUG) System.out.println("Page.setDataModel()");

    DataModel saved = this.dataModel;
    if (this.dataModel != null) this.dataModel.removePropertyChangeListener(this);
    if (dataModel != null) dataModel.addPropertyChangeListener(this);

    this.dataModel = dataModel;
    this.dataModel.setPage(this);
    if (isDesignTime_) repaint();
    firePropertyChange("dataModel", saved, this.dataModel);
  }

  /**
   * Get <code>PanelModel</code>.
   *
   * @return PanelModel
   * @see PanelModel
   */
  public PanelModel getPanelModel() {
    return panelModel;
  }

  /**
   * Set <code>PanelModel</code>. The <code>PanelModel</code> contains the information that
   * indicates placement of <code>Panel</code>s and <code>DataGroupLayer</code>s.
   *
   * @param panelModel PanelModel
   * @see PanelModel
   */
  public void setPanelModel(PanelModel panelModel) {
    if (DEBUG) System.out.println("Page.setPanelModel()");
    PanelModel saved = this.panelModel;
    setSize(panelModel.getPageSize());
    setBackground(panelModel.getPageBackgroundColor());
    setPreferredSize(panelModel.getPageSize());
    this.panelModel = panelModel;
    this.panelModel.setPage(this);
    updatePanels();
    if (isDesignTime_) repaint();
    firePropertyChange("panelModel", saved, this.panelModel);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setBackground(Color color) {
    super.setBackground(color);
    pane_.setBackground(color);
  }

  @Override
  public void setName(String name) {
    firePropertyChange("name", this.name, name);
    this.name = name;
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    Object source = evt.getSource();
    String property = evt.getPropertyName();
    if (source instanceof PanelModel) {
      if (DEBUG) System.out.println("Page.propertyChange(PanelModel): " + property);
      updatePanels();
    } else if (source instanceof DataModel) {
      Object obj = evt.getNewValue();
      if (obj instanceof DataHolder) {
        try {
          pane_.setBatch(true);
          ((DataHolder) obj).notifyPanel();
          pane_.setModified(true, "Page");
          pane_.setBatch(false);
        } catch (DataTargetMismatchException dtme) {
          dtme.printStackTrace();
        }
      }
      if (DEBUG) System.out.println("Page.propertyChange(DataModel): " + property);
    } else if (source == pane_) {
      if (property.equals("objectSelected")) {

      } else if (property.equals("zoomRectangle")) {

      }
      if (DEBUG) System.out.println("Page.propertyChange(JPane): " + property);
    }
  }

  private void updatePanels() {
    // check for deleted PanelHolders
    setSize(panelModel.getPageSize());
    setBackground(panelModel.getPageBackgroundColor());
    setPrintHAlign(panelModel.getPrintHAlign());
    setPrintVAlign(panelModel.getPrintVAlign());
    setPrintOrigin(panelModel.getPrintOrigin());
    setPrintScaleMode(panelModel.getPrintScaleMode());
    pane_.setBatch(true);
    Component[] comps = pane_.getComponents();
    for (int i = 0; i < comps.length; i++) {
      if (comps[i] instanceof Panel) {
        Panel pnl = (Panel) comps[i];
        if (!panelModel.hasPanelHolder(pnl.getName())) {
          pane_.remove(pnl);
        }
      }
    }
    // check for new PanelHolders then create otherwise update
    Panel panel = null;
    Iterator phIter = panelModel.panelIterator();
    while (phIter.hasNext()) {
      PanelHolder ph = (PanelHolder) phIter.next();
      panel = findPanel(ph);
      if (panel == null) {
        panel = new Panel(ph);
        ph.setInstantiated(true);
        //        panel.setBorder(new EtchedBorder());
        pane_.add(panel);
      } else {
        panel.update();
      }
    }
    validate();
    pane_.setModified(true, "Page");
    pane_.setBatch(false);
  }

  /**
   * Find the <code>Panel</code> associated with <code>PanelHolder</code>, a <code>PanelModel</code>
   * component.
   *
   * @param pHolder PanelHolder
   * @return Panel
   * @see PanelModel
   * @see PanelHolder
   * @see Panel
   */
  public Panel findPanel(PanelHolder pHolder) {
    Component[] comps = pane_.getComponents();
    for (int i = 0; i < comps.length; i++) {
      if (comps[i] instanceof Panel) {
        if (((Panel) comps[i]).getName().equals(pHolder.getId())) return (Panel) comps[i];
      }
    }
    return null;
  }

  private void pageMousePressed(MouseEvent event) {
    if (!event.isControlDown()) return;

    pane_.setBatch(true);
    Component[] comps = pane_.getComponents();
    for (int i = 0; i < comps.length; i++) {
      if (comps[i] instanceof Panel) {
        Panel pnl = (Panel) comps[i];
        pnl.resetZoom(event.getX(), event.getY());
      }
    }
    pane_.setBatch(false);
  }

  /** Reset the zoom for all <code>Panel</code>s and <code>DataGroupLayer</code>s. */
  public void resetZoom() {
    pane_.setBatch(true);
    Component[] comps = pane_.getComponents();
    for (int i = 0; i < comps.length; i++) {
      if (comps[i] instanceof Panel) {
        Panel pnl = (Panel) comps[i];
        pnl.resetZoom();
      }
    }
    pane_.setBatch(false);
  }

  private void pageMouseClicked(MouseEvent event) {
    if (event.isControlDown()) return; // ignore zoom resets
    if (event.isPopupTrigger()) System.out.println("Page.pageMouseClicked(): isPopupTrigger()");
    Object obj = pane_.getSelectedObject();
    if ((event.getModifiers() & InputEvent.BUTTON3_MASK) != 0)
      System.out.println("Page.pageMouseClicked(): Button3!");
  }

  private void pageMouseReleased(MouseEvent event) {
    //
    // continue only if button1 is pressed
    //
    if ((event.getModifiers() & InputEvent.BUTTON1_MASK) == 0) return;
    Rectangle zm = pane_.getZoomBounds();
    Point zmStart = pane_.getZoomStart();
    if (zm.width <= 1 || zm.height <= 1) return;

    pane_.setBatch(true);
    Component[] comps = pane_.getComponents();
    for (int i = 0; i < comps.length; i++) {
      if (comps[i] instanceof Panel) {
        Panel pnl = (Panel) comps[i];
        pnl.zoomTo(zmStart, zm);
      }
    }
    pane_.setBatch(false);
  }

  class PageMouse extends MouseAdapter {
    @Override
    public void mousePressed(MouseEvent event) {
      if (!pane_.isMouseEventsEnabled()) return;
      pageMousePressed(event);
    }

    @Override
    public void mouseClicked(MouseEvent event) {
      if (!pane_.isMouseEventsEnabled()) return;
      pageMouseClicked(event);
    }

    @Override
    public void mouseReleased(MouseEvent event) {
      if (!pane_.isMouseEventsEnabled()) return;
      pageMouseReleased(event);
    }
  }

  @Override
  public int print(Graphics g, PageFormat pf, int pageIndex) throws PrinterException {
    int result = NO_SUCH_PAGE;
    Color saveColor = getBackground();
    if (panelModel.isPrintWhitePage()) {
      setBackground(Color.white);
    }
    result = pane_.print(g, pf, pageIndex);
    if (result == PAGE_EXISTS && panelModel.isPrintBorders()) {
      Component[] comps = pane_.getComponents();
      for (int i = 0; i < comps.length; i++) {
        if (comps[i] instanceof Panel) {
          Panel pnl = (Panel) comps[i];
          Rectangle r = pnl.getBounds();
          Border bdr = pnl.getBorder();
          bdr.paintBorder(pnl, g, r.x, r.y, r.width, r.height);
        }
      }
    }
    setBackground(saveColor);
    return result;
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
    pane_.setPageScaleMode(mode);
  }

  /**
   * Get printing scale mode.
   *
   * @return scale mode
   */
  public int getPrintScaleMode() {
    return pane_.getPageScaleMode();
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
  public void setPrintVAlign(int vert) {
    pane_.setPageVAlign(vert);
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
  public void setPrintHAlign(int horz) {
    pane_.setPageHAlign(horz);
  }

  /**
   * Get vertical alignment for printing.
   *
   * @return vertical alignment.
   */
  public int getPrintVAlign() {
    return pane_.getPageVAlign();
  }

  /**
   * Get horizontal alignment for printing.
   *
   * @return horizontal alignment
   */
  public int getPrintHAlign() {
    return pane_.getPageHAlign();
  }

  /**
   * Set page origin for printing. Will be used if the horizontal or vertical alignment is
   * AbstractPane.SPECIFIED_LOCATION. Default = (0,0).
   *
   * @param pageOrigin page origin
   * @see gov.noaa.pmel.sgt.AbstractPane AbstractPane
   */
  public void setPrintOrigin(Point pt) {
    pane_.setPageOrigin(pt);
  }

  /**
   * Get page origin for printing.
   *
   * @return page origin.
   */
  public Point getPrintOrigin() {
    return pane_.getPageOrigin();
  }
}
