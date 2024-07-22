/*
 *  $Id: Annote.java,v 1.8 2003/08/22 23:02:38 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */
package gov.noaa.pmel.sgt.dm;

import com.cohort.util.MustBe;
import com.cohort.util.String2;
import gov.noaa.pmel.sgt.CartesianGraph;
import gov.noaa.pmel.sgt.Graph;
import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.LineAttribute;
import gov.noaa.pmel.sgt.PointAttribute;
import gov.noaa.pmel.sgt.SGLabel;
import gov.noaa.pmel.util.GeoDateArray;
import gov.noaa.pmel.util.Rectangle2D;
import gov.noaa.pmel.util.SoTDomain;
import gov.noaa.pmel.util.SoTPoint;
import gov.noaa.pmel.util.SoTRange;
import java.awt.Color;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;

/**
 * Abstract class for annotations.
 *
 * @author Donald Denbo
 * @version $Revision: 1.8 $
 * @since 3.0
 */
public abstract class Annote implements SGTData, Serializable, PropertyChangeListener {
  protected transient PropertyChangeSupport changes_;
  // serial version ref 1.7.2.5
  private static final long serialVersionUID = 7305616377566581275L;
  protected String id_ = null;

  /**
   * Class for line annotations.
   *
   * @author Donald Denbo
   * @version $Revision: 1.8 $
   * @since 3.0
   */
  public static class Line extends Annote {
    SGTLine line;
    LineAttribute attr;

    /** Bob Simons added this to avoid memory leak problems. */
    @Override
    public void releaseResources() throws Exception {
      try {
        line = null;
        attr = null;
        if (JPane.debug) String2.log("sgt.dm.Annote.Line.releaseResources() finished");
      } catch (Throwable t) {
        String2.log(MustBe.throwableToString(t));
        if (JPane.debug) String2.pressEnterToContinue();
      }
    }

    public Line(String id, SGTLine line, LineAttribute attr) {
      id_ = id;
      this.line = line;
      this.attr = attr;
      init();
    }

    @Override
    public void init() {
      if (attr != null) attr.addPropertyChangeListener(this);
      changes_ = new PropertyChangeSupport(this);
    }

    public SGTLine getLine() {
      return line;
    }

    public LineAttribute getAttribute() {
      return attr;
    }

    @Override
    public SoTDomain getDomain() {
      return new SoTDomain(line.getXRange(), line.getYRange());
    }

    @Override
    public Rectangle getBounds(Graph graph) {
      return computeBounds(graph, this.getDomain());
    }

    @Override
    public void moveBy(SoTPoint point) {
      double[] xu = null;
      double[] yu = null;
      long[] tu = null;
      double delta;
      long tdelta;
      if (!(line instanceof SimpleLine)) return;
      if (line.isXTime()) {
        tu = line.getGeoDateArray().getTime();
        tdelta = point.getX().getLongTime();
        for (int i = 0; i < tu.length; i++) {
          tu[i] += tdelta;
        }
        ((SimpleLine) line).setTimeArray(new GeoDateArray(tu));
      } else {
        xu = line.getXArray();
        delta = ((Number) point.getX().getObjectValue()).doubleValue();
        for (int i = 0; i < xu.length; i++) {
          xu[i] += delta;
        }
        ((SimpleLine) line).setXArray(xu);
      }
      if (line.isYTime()) {
        tu = line.getGeoDateArray().getTime();
        tdelta = point.getY().getLongTime();
        for (int i = 0; i < tu.length; i++) {
          tu[i] += tdelta;
        }
        ((SimpleLine) line).setTimeArray(new GeoDateArray(tu));
      } else {
        yu = line.getYArray();
        delta = ((Number) point.getY().getObjectValue()).doubleValue();
        for (int i = 0; i < yu.length; i++) {
          yu[i] += delta;
        }
        ((SimpleLine) line).setYArray(yu);
      }
      changes_.firePropertyChange("lineMoved", true, false);
    }

    public void setXArray(double[] xa) {}

    public void setYArray(double[] ya) {}

    public void setTimeArray(GeoDateArray gda) {}

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      changes_.firePropertyChange(evt);
    }

    /* SGTData required methods */
    @Override
    public SGTData copy() {
      SGTData copy = null;
      try {
        copy = (SGTData) this.clone();
      } catch (CloneNotSupportedException ex) {
        ex.printStackTrace();
      }
      return copy;
    }

    @Override
    public String getId() {
      return line.getId();
    }

    @Override
    public SGLabel getKeyTitle() {
      return line.getKeyTitle();
    }

    @Override
    public String getTitle() {
      return line.getTitle();
    }

    @Override
    public SGTMetaData getXMetaData() {
      return line.getXMetaData();
    }

    @Override
    public SoTRange getXRange() {
      return line.getXRange();
    }

    @Override
    public SGTMetaData getYMetaData() {
      return line.getYMetaData();
    }

    @Override
    public SoTRange getYRange() {
      return line.getYRange();
    }

    @Override
    public boolean isXTime() {
      return line.isXTime();
    }

    @Override
    public boolean isYTime() {
      return line.isYTime();
    }
  }

  /**
   * Class for point annotations.
   *
   * @author Donald Denbo
   * @version $Revision: 1.8 $
   * @since 3.0
   */
  public static class Point extends Annote {
    SGTPoint point;
    PointAttribute attr;

    /** Bob Simons added this to avoid memory leak problems. */
    @Override
    public void releaseResources() throws Exception {
      try {
        point = null;
        attr = null;
        if (JPane.debug) String2.log("sgt.dm.Annote.Point.releaseResources() finished");
      } catch (Throwable t) {
        String2.log(MustBe.throwableToString(t));
        if (JPane.debug) String2.pressEnterToContinue();
      }
    }

    public Point(String id, SGTPoint point, PointAttribute attr) {
      id_ = id;
      this.point = point;
      this.attr = attr;
      init();
    }

    @Override
    public void init() {
      if (attr != null) attr.addPropertyChangeListener(this);
      changes_ = new PropertyChangeSupport(this);
    }

    public SGTPoint getPoint() {
      return point;
    }

    public PointAttribute getAttribute() {
      return attr;
    }

    @Override
    public SoTDomain getDomain() {
      return new SoTDomain(point.getXRange(), point.getYRange());
    }

    @Override
    public Rectangle getBounds(Graph graph) {
      /**
       * @todo add text bounds
       */
      Rectangle rect = computeBounds(graph, this.getDomain());
      double hgt = attr.getMarkHeightP();
      int ihgt = graph.getLayer().getXPtoD(hgt);
      rect.setBounds(rect.x - ihgt / 2, rect.y - ihgt / 2, ihgt, ihgt);
      return rect;
    }

    @Override
    public void moveBy(SoTPoint pnt) {
      double xu = 0.0;
      double yu = 0.0;
      long tu = 0;
      double delta;
      long tdelta;
      if (!(point instanceof SimplePoint)) return;
      if (point.isXTime()) {
        tu = point.getLongTime();
        tdelta = pnt.getX().getLongTime();
        tu += tdelta;
        ((SimplePoint) point).setTime(tu);
      } else {
        xu = point.getX();
        delta = ((Number) pnt.getX().getObjectValue()).doubleValue();
        xu += delta;
        ((SimplePoint) point).setX(xu);
      }
      if (point.isYTime()) {
        tu = point.getLongTime();
        tdelta = pnt.getY().getLongTime();
        tu += tdelta;
        ((SimplePoint) point).setTime(tu);
      } else {
        yu = point.getY();
        delta = ((Number) pnt.getY().getObjectValue()).doubleValue();
        yu += delta;
        ((SimplePoint) point).setY(yu);
      }
      changes_.firePropertyChange("pointMoved", true, false);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      changes_.firePropertyChange(evt);
    }

    /* SGTData required methods */
    @Override
    public SGTData copy() {
      SGTData copy = null;
      try {
        copy = (SGTData) this.clone();
      } catch (CloneNotSupportedException ex) {
        ex.printStackTrace();
      }
      return copy;
    }

    @Override
    public String getId() {
      return point.getId();
    }

    @Override
    public SGLabel getKeyTitle() {
      return point.getKeyTitle();
    }

    @Override
    public String getTitle() {
      return point.getTitle();
    }

    @Override
    public SGTMetaData getXMetaData() {
      return point.getXMetaData();
    }

    @Override
    public SoTRange getXRange() {
      return point.getXRange();
    }

    @Override
    public SGTMetaData getYMetaData() {
      return point.getYMetaData();
    }

    @Override
    public SoTRange getYRange() {
      return point.getYRange();
    }

    @Override
    public boolean isXTime() {
      return point.isXTime();
    }

    @Override
    public boolean isYTime() {
      return point.isYTime();
    }
  }

  /**
   * Class for text annotations.
   *
   * @author Donald Denbo
   * @version $Revision: 1.8 $
   * @since 3.0
   */
  public static class Text extends Annote {
    SoTPoint location;
    SGLabel text;

    /** Bob Simons added this to avoid memory leak problems. */
    @Override
    public void releaseResources() throws Exception {
      try {
        location = null;
        text = null;
        if (JPane.debug) String2.log("sgt.dm.Annote.Text.releaseResources() finished");
      } catch (Throwable t) {
        String2.log(MustBe.throwableToString(t));
        if (JPane.debug) String2.pressEnterToContinue();
      }
    }

    public Text(String id, SoTPoint location, SGLabel text) {
      id_ = id;
      this.location = location;
      this.text = text;
      init();
    }

    @Override
    public void init() {
      if (text != null) text.addPropertyChangeListener(this);
      changes_ = new PropertyChangeSupport(this);
    }

    public SoTPoint getLocation() {
      return location;
    }

    public SGLabel getText() {
      return text;
    }

    @Override
    public SoTDomain getDomain() {

      return null;
    }

    private SoTDomain getDomain(Graph graph) {
      CartesianGraph cg = (CartesianGraph) graph;
      SoTRange xRange = null;
      SoTRange yRange = null;
      Rectangle2D.Double bnds = text.getBoundsP();
      /**
       * @todo rewrite to go directly to device units
       */
      double xloc = cg.getXUtoP(location.getX());
      double yloc = cg.getYUtoP(location.getY());
      double width = bnds.width;
      double height = bnds.height;
      if (location.isXTime()) {
        long min = cg.getXPtoLongTime(xloc);
        long max = cg.getXPtoLongTime(xloc + width);
        xRange = new SoTRange.Time(min, max);
      } else {
        double min = cg.getXPtoU(xloc);
        double max = cg.getXPtoU(xloc + width);
        xRange = new SoTRange.Double(min, max);
      }
      if (location.isYTime()) {
        long min = cg.getYPtoLongTime(yloc);
        long max = cg.getYPtoLongTime(yloc + height);
        yRange = new SoTRange.Time(min, max);
      } else {
        double min = cg.getYPtoU(yloc);
        double max = cg.getYPtoU(yloc + height);
        yRange = new SoTRange.Double(min, max);
      }
      return new SoTDomain(xRange, yRange);
    }

    @Override
    public Rectangle getBounds(Graph graph) {
      return computeBounds(graph, this.getDomain(graph));
    }

    @Override
    public void moveBy(SoTPoint point) {
      location.add(point);
      changes_.firePropertyChange("textMoved", true, false);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      changes_.firePropertyChange(evt);
    }

    /* SGTData required methods */
    @Override
    public SGTData copy() {
      SGTData copy = null;
      try {
        copy = (SGTData) this.clone();
      } catch (CloneNotSupportedException ex) {
        ex.printStackTrace();
      }
      return copy;
    }

    @Override
    public String getId() {
      return text.getId();
    }

    @Override
    public SGLabel getKeyTitle() {
      return null;
    }

    @Override
    public String getTitle() {
      return null;
    }

    @Override
    public SGTMetaData getXMetaData() {
      return null;
    }

    @Override
    public SoTRange getXRange() {
      return null;
    }

    @Override
    public SGTMetaData getYMetaData() {
      return null;
    }

    @Override
    public SoTRange getYRange() {
      return null;
    }

    @Override
    public boolean isXTime() {
      return location.isXTime();
    }

    @Override
    public boolean isYTime() {
      return location.isYTime();
    }
  }

  /**
   * Class for oval annotations.
   *
   * @author Donald Denbo
   * @version $Revision: 1.8 $
   * @since 3.0
   */
  public static class Oval extends Annote {
    SoTPoint upperLeft;
    SoTPoint lowerRight;
    LineAttribute attr;
    Color color;
    SoTRange xRange_ = null;
    SoTRange yRange_ = null;

    /** Bob Simons added this to avoid memory leak problems. */
    @Override
    public void releaseResources() throws Exception {
      try {
        attr = null;
        if (JPane.debug) String2.log("sgt.dm.Annote.Oval.releaseResources() finished");
      } catch (Throwable t) {
        String2.log(MustBe.throwableToString(t));
        if (JPane.debug) String2.pressEnterToContinue();
      }
    }

    public Oval(
        String id, SoTPoint upperLeft, SoTPoint lowerRight, LineAttribute attr, Color color) {
      id_ = id;
      this.upperLeft = upperLeft;
      this.lowerRight = lowerRight;
      this.attr = attr;
      this.color = color;
      init();
      computeRange();
    }

    @Override
    public void init() {
      if (attr != null) attr.addPropertyChangeListener(this);
      changes_ = new PropertyChangeSupport(this);
    }

    public SoTPoint getUpperLeft() {
      return upperLeft;
    }

    public void setUpperLeft(SoTPoint ul) {
      upperLeft = ul;
      computeRange();
      changes_.firePropertyChange("ovalMoved", true, false);
    }

    public SoTPoint getLowerRight() {
      return lowerRight;
    }

    public void setLowerRight(SoTPoint lr) {
      lowerRight = lr;
      computeRange();
      changes_.firePropertyChange("ovalMoved", true, false);
    }

    public void setLocation(SoTPoint ul, SoTPoint lr) {
      upperLeft = ul;
      lowerRight = lr;
      computeRange();
      changes_.firePropertyChange("ovalMoved", true, false);
    }

    public LineAttribute getLineAttribute() {
      return attr;
    }

    public Color getFillColor() {
      return color;
    }

    public void setFillColor(Color color) {
      this.color = color;
      changes_.firePropertyChange("colorChanged", true, false);
    }

    @Override
    public SoTDomain getDomain() {
      return new SoTDomain(xRange_, yRange_);
    }

    @Override
    public Rectangle getBounds(Graph graph) {
      return computeBounds(graph, this.getDomain());
    }

    @Override
    public void moveBy(SoTPoint point) {
      upperLeft.add(point);
      lowerRight.add(point);
      computeRange();
      changes_.firePropertyChange("ovalMoved", true, false);
    }

    private void computeRange() {
      if (upperLeft.getX().isTime()) {
        xRange_ =
            new SoTRange.Time(upperLeft.getX().getLongTime(), lowerRight.getX().getLongTime());
      } else {
        double xmin = ((Number) upperLeft.getX().getObjectValue()).doubleValue();
        double xmax = ((Number) lowerRight.getX().getObjectValue()).doubleValue();
        xRange_ = new SoTRange.Double(xmin, xmax);
      }
      if (upperLeft.getY().isTime()) {
        yRange_ =
            new SoTRange.Time(lowerRight.getY().getLongTime(), upperLeft.getY().getLongTime());
      } else {
        double ymin = ((Number) lowerRight.getY().getObjectValue()).doubleValue();
        double ymax = ((Number) upperLeft.getY().getObjectValue()).doubleValue();
        yRange_ = new SoTRange.Double(ymin, ymax);
      }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      changes_.firePropertyChange(evt);
    }

    /* SGTData required methods */
    @Override
    public SGTData copy() {
      SGTData copy = null;
      try {
        copy = (SGTData) this.clone();
      } catch (CloneNotSupportedException ex) {
        ex.printStackTrace();
      }
      return copy;
    }

    @Override
    public String getId() {
      return null;
    }

    @Override
    public SGLabel getKeyTitle() {
      return null;
    }

    @Override
    public String getTitle() {
      return null;
    }

    @Override
    public SGTMetaData getXMetaData() {
      return null;
    }

    @Override
    public SoTRange getXRange() {
      return xRange_;
    }

    @Override
    public SGTMetaData getYMetaData() {
      return null;
    }

    @Override
    public SoTRange getYRange() {
      return yRange_;
    }

    @Override
    public boolean isXTime() {
      return upperLeft.isXTime();
    }

    @Override
    public boolean isYTime() {
      return upperLeft.isYTime();
    }
  }

  /**
   * Class for rectangle annotations.
   *
   * @author Donald Denbo
   * @version $Revision: 1.8 $
   * @since 3.0
   */
  public static class Rect extends Annote {
    SoTPoint upperLeft;
    SoTPoint lowerRight;
    LineAttribute attr;
    Color color;
    SoTRange xRange_ = null;
    SoTRange yRange_ = null;

    /** Bob Simons added this to avoid memory leak problems. */
    @Override
    public void releaseResources() throws Exception {
      try {
        attr = null;
        if (JPane.debug) String2.log("sgt.dm.Annote.Line.releaseResources() finished");
      } catch (Throwable t) {
        String2.log(MustBe.throwableToString(t));
        if (JPane.debug) String2.pressEnterToContinue();
      }
    }

    public Rect(
        String id, SoTPoint upperLeft, SoTPoint lowerRight, LineAttribute attr, Color color) {
      id_ = id;
      this.upperLeft = upperLeft;
      this.lowerRight = lowerRight;
      this.attr = attr;
      this.color = color;
      init();
      computeRange();
    }

    @Override
    public void init() {
      if (attr != null) attr.addPropertyChangeListener(this);
      changes_ = new PropertyChangeSupport(this);
    }

    public SoTPoint getUpperLeft() {
      return upperLeft;
    }

    public void setUpperLeft(SoTPoint ul) {
      upperLeft = ul;
      computeRange();
      changes_.firePropertyChange("rectMoved", true, false);
    }

    public SoTPoint getLowerRight() {
      return lowerRight;
    }

    public void setLowerRight(SoTPoint lr) {
      lowerRight = lr;
      computeRange();
      changes_.firePropertyChange("rectMoved", true, false);
    }

    public void setLocation(SoTPoint ul, SoTPoint lr) {
      upperLeft = ul;
      lowerRight = lr;
      computeRange();
      changes_.firePropertyChange("rectMoved", true, false);
    }

    public LineAttribute getLineAttribute() {
      return attr;
    }

    public Color getFillColor() {
      return color;
    }

    public void setFillColor(Color color) {
      this.color = color;
      changes_.firePropertyChange("colorChanged", true, false);
    }

    @Override
    public SoTDomain getDomain() {
      return new SoTDomain(xRange_, yRange_);
    }

    @Override
    public Rectangle getBounds(Graph graph) {
      return computeBounds(graph, this.getDomain());
    }

    @Override
    public void moveBy(SoTPoint point) {
      upperLeft.add(point);
      lowerRight.add(point);
      computeRange();
      changes_.firePropertyChange("rectMoved", true, false);
    }

    private void computeRange() {
      if (upperLeft.getX().isTime()) {
        xRange_ =
            new SoTRange.Time(upperLeft.getX().getLongTime(), lowerRight.getX().getLongTime());
      } else {
        double xmin = ((Number) upperLeft.getX().getObjectValue()).doubleValue();
        double xmax = ((Number) lowerRight.getX().getObjectValue()).doubleValue();
        xRange_ = new SoTRange.Double(xmin, xmax);
      }
      if (upperLeft.getY().isTime()) {
        yRange_ =
            new SoTRange.Time(lowerRight.getY().getLongTime(), upperLeft.getY().getLongTime());
      } else {
        double ymin = ((Number) lowerRight.getY().getObjectValue()).doubleValue();
        double ymax = ((Number) upperLeft.getY().getObjectValue()).doubleValue();
        yRange_ = new SoTRange.Double(ymin, ymax);
      }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      changes_.firePropertyChange(evt);
    }

    /* SGTData required methods */
    @Override
    public SGTData copy() {
      SGTData copy = null;
      try {
        copy = (SGTData) this.clone();
      } catch (CloneNotSupportedException ex) {
        ex.printStackTrace();
      }
      return copy;
    }

    @Override
    public String getId() {
      return null;
    }

    @Override
    public SGLabel getKeyTitle() {
      return null;
    }

    @Override
    public String getTitle() {
      return null;
    }

    @Override
    public SGTMetaData getXMetaData() {
      return null;
    }

    @Override
    public SoTRange getXRange() {
      return xRange_;
    }

    @Override
    public SGTMetaData getYMetaData() {
      return null;
    }

    @Override
    public SoTRange getYRange() {
      return yRange_;
    }

    @Override
    public boolean isXTime() {
      return upperLeft.isXTime();
    }

    @Override
    public boolean isYTime() {
      return upperLeft.isYTime();
    }
  }

  protected Annote() {}

  protected Rectangle computeBounds(Graph graph, SoTDomain domain) {
    Rectangle rect = null;
    if (domain == null || !(graph instanceof CartesianGraph)) return rect;
    CartesianGraph cg = (CartesianGraph) graph;
    int xd, yd;
    int width, height;

    int xd1 = cg.getXUtoD(domain.getXRange().getStart());
    int yd1 = cg.getYUtoD(domain.getYRange().getStart());
    int xd2 = cg.getXUtoD(domain.getXRange().getEnd());
    int yd2 = cg.getYUtoD(domain.getYRange().getEnd());
    if (xd1 < xd2) {
      xd = xd1;
      width = xd2 - xd1;
    } else {
      xd = xd2;
      width = xd1 - xd2;
    }
    if (yd1 < yd2) {
      yd = yd1;
      height = yd2 - yd1;
    } else {
      yd = yd2;
      height = yd1 - yd2;
    }
    rect = new Rectangle(xd, yd, width, height);
    return rect;
  }

  public abstract Rectangle getBounds(Graph graph);

  public abstract SoTDomain getDomain();

  public abstract void moveBy(SoTPoint point);

  @Override
  public abstract void propertyChange(PropertyChangeEvent evt);

  public String getAnnoteId() {
    return id_;
  }

  /** Init method used to setup serialized object. */
  public abstract void init();

  /* SGTData required methods */
  @Override
  public void addPropertyChangeListener(PropertyChangeListener l) {
    if (changes_ == null) changes_ = new PropertyChangeSupport(this);
    changes_.addPropertyChangeListener(l);
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener l) {
    if (changes_ == null) changes_ = new PropertyChangeSupport(this);
    changes_.removePropertyChangeListener(l);
  }
}
