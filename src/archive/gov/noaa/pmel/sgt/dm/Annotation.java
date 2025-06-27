/*
 * $Id: Annotation.java,v 1.8 2003/08/22 23:02:38 dwd Exp $
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
import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.LineAttribute;
import gov.noaa.pmel.sgt.PointAttribute;
import gov.noaa.pmel.sgt.SGException;
import gov.noaa.pmel.sgt.SGLabel;
import gov.noaa.pmel.util.SoTPoint;
import gov.noaa.pmel.util.SoTRange;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * A container to hold <code>Annote</code> objects.
 *
 * @author Donald Denbo
 * @version $Revision: 1.8 $
 * @since 3.0
 */
public class Annotation implements SGTData, PropertyChangeListener {
  private PropertyChangeSupport changes_ = new PropertyChangeSupport(this);
  private String title_ = null;
  private String id_ = null;

  /**
   * @label x
   * @link aggregation
   */
  private SGTMetaData xMeta_ = null;

  /**
   * @link aggregation
   * @label y
   */
  private SGTMetaData yMeta_ = null;

  private List text_ = new Vector();
  private List line_ = new Vector();
  private List point_ = new Vector();
  private List oval_ = new Vector();
  private List rect_ = new Vector();

  private SoTRange xRange_ = null;
  private SoTRange yRange_ = null;
  private boolean xTime_ = false;
  private boolean yTime_ = false;

  /** Bob Simons added this to avoid memory leak problems. */
  @Override
  public void releaseResources() throws Exception {
    try {
      changes_ = null;
      xMeta_ = null;
      yMeta_ = null;
      text_ = null;
      line_ = null;
      point_ = null;
      oval_ = null;
      rect_ = null;
      xRange_ = null;
      yRange_ = null;
      if (JPane.debug) String2.log("sgt.dm.Annotation.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  public Annotation() {
    this(null, false, false);
  }

  public Annotation(String title) {
    this(title, false, false);
  }

  public Annotation(String title, boolean xTime, boolean yTime) {
    title_ = title;
    xTime_ = xTime;
    yTime_ = yTime;
  }

  public boolean remove(String id) {
    Annote ann = findAnnote(id);
    if (ann == null) return false;
    return remove(ann);
  }

  public boolean remove(Annote ann) {
    if (ann instanceof Annote.Line) {
      return removeLine(ann);
    } else if (ann instanceof Annote.Oval) {
      return removeOval(ann);
    } else if (ann instanceof Annote.Point) {
      return removePoint(ann);
    } else if (ann instanceof Annote.Rect) {
      return removeRect(ann);
    } else if (ann instanceof Annote.Text) {
      return removeText(ann);
    }
    return false;
  }

  public void add(Annote ann) throws SGException {
    if (ann instanceof Annote.Line) {
      SGTLine line = ((Annote.Line) ann).getLine();
      if (xTime_ != line.isXTime() || yTime_ != line.isYTime())
        throw new SGException("Time axes do not match");
      ann.addPropertyChangeListener(this);
      line_.add(ann);
      changes_.firePropertyChange("lineAdded", true, false);
    } else if (ann instanceof Annote.Oval) {
      SoTPoint pt1 = ((Annote.Oval) ann).getUpperLeft();
      if (xTime_ != pt1.isXTime() || yTime_ != pt1.isYTime())
        throw new SGException("Time axes do not match");
      ann.addPropertyChangeListener(this);
      oval_.add(ann);
      changes_.firePropertyChange("ovalAdded", true, false);
    } else if (ann instanceof Annote.Point) {
      SGTPoint point = ((Annote.Point) ann).getPoint();
      if (xTime_ != point.isXTime() || yTime_ != point.isYTime())
        throw new SGException("Time axes do not match");
      ann.addPropertyChangeListener(this);
      point_.add(ann);
      changes_.firePropertyChange("pointAdded", true, false);
    } else if (ann instanceof Annote.Rect) {
      SoTPoint pt1 = ((Annote.Rect) ann).getUpperLeft();
      if (xTime_ != pt1.isXTime() || yTime_ != pt1.isYTime())
        throw new SGException("Time axes do not match");
      ann.addPropertyChangeListener(this);
      rect_.add(ann);
      changes_.firePropertyChange("rectAdded", true, false);
    } else if (ann instanceof Annote.Text) {
      SoTPoint loc = ((Annote.Text) ann).getLocation();
      if (xTime_ != loc.isXTime() || yTime_ != loc.isYTime())
        throw new SGException("Time axes do not match");
      ann.addPropertyChangeListener(this);
      text_.add(ann);
      changes_.firePropertyChange("textAdded", true, false);
    }
  }

  public Annote addLine(String id, SGTLine line, LineAttribute attr) throws SGException {
    if (xTime_ != line.isXTime() || yTime_ != line.isYTime())
      throw new SGException("Time axes do not match");
    Annote.Line aLine = new Annote.Line(id, line, attr);
    aLine.addPropertyChangeListener(this);
    line_.add(aLine);

    changes_.firePropertyChange("lineAdded", true, false);
    return aLine;
  }

  private boolean removeLine(Annote line) {
    boolean result = false;
    if (line instanceof Annote.Line) {
      line.removePropertyChangeListener(this);
      result = line_.remove(line);
      if (result) changes_.firePropertyChange("lineRemoved", true, false);
    }
    return result;
  }

  public Iterator getLineIterator() {
    return line_.iterator();
  }

  public boolean hasLine() {
    return !line_.isEmpty();
  }

  public Annote addPoint(String id, SGTPoint point, PointAttribute attr) throws SGException {
    if (xTime_ != point.isXTime() || yTime_ != point.isYTime())
      throw new SGException("Time axes do not match");
    Annote.Point aPoint = new Annote.Point(id, point, attr);
    aPoint.addPropertyChangeListener(this);
    point_.add(aPoint);

    changes_.firePropertyChange("pointAdded", true, false);
    return aPoint;
  }

  private boolean removePoint(Annote point) {
    boolean result = false;
    if (point instanceof Annote.Point) {
      point.removePropertyChangeListener(this);
      result = point_.remove(point);
      if (result) changes_.firePropertyChange("pointRemoved", true, false);
    }
    return result;
  }

  public Iterator getPointIterator() {
    return point_.iterator();
  }

  public boolean hasPoint() {
    return !point_.isEmpty();
  }

  public Annote addText(String id, SoTPoint loc, SGLabel text) throws SGException {
    if (xTime_ != loc.isXTime() || yTime_ != loc.isYTime())
      throw new SGException("Time axes do not match");
    Annote.Text aText = new Annote.Text(id, loc, text);
    aText.addPropertyChangeListener(this);
    text_.add(aText);

    changes_.firePropertyChange("textAdded", true, false);
    return aText;
  }

  private boolean removeText(Annote text) {
    boolean result = false;
    if (text instanceof Annote.Text) {
      text.removePropertyChangeListener(this);
      result = text_.remove(text);
      if (result) changes_.firePropertyChange("textRemoved", true, false);
    }
    return result;
  }

  public Iterator getTextIterator() {
    return text_.iterator();
  }

  public boolean hasText() {
    return !text_.isEmpty();
  }

  /**
   * Add an oval to the <code>Annotation</code>. If attr is non-null an oval outline will be drawn,
   * if color is non-null it will be filled.
   */
  public Annote addOval(String id, SoTPoint pt1, SoTPoint pt2, LineAttribute attr, Color color)
      throws SGException {
    if (xTime_ != pt1.isXTime() || yTime_ != pt1.isYTime())
      throw new SGException("Time axes do not match");
    Annote.Oval aOval = new Annote.Oval(id, pt1, pt2, attr, color);
    aOval.addPropertyChangeListener(this);
    oval_.add(aOval);

    changes_.firePropertyChange("ovalAdded", true, false);
    return aOval;
  }

  private boolean removeOval(Annote oval) {
    boolean result = false;
    if (oval instanceof Annote.Oval) {
      oval.removePropertyChangeListener(this);
      result = oval_.remove(oval);
      if (result) changes_.firePropertyChange("ovalRemoved", true, false);
    }
    return result;
  }

  public Iterator getOvalIterator() {
    return oval_.iterator();
  }

  public boolean hasOval() {
    return !oval_.isEmpty();
  }

  /**
   * Add an rectangle to the <code>Annotation</code>. If attr is non-null an rectangle outline will
   * be drawn, if color is non-null it will be filled.
   */
  public Annote addRect(String id, SoTPoint pt1, SoTPoint pt2, LineAttribute attr, Color color)
      throws SGException {
    if (xTime_ != pt1.isXTime() || yTime_ != pt1.isYTime())
      throw new SGException("Time axes do not match");
    Annote.Rect aRect = new Annote.Rect(id, pt1, pt2, attr, color);
    aRect.addPropertyChangeListener(this);
    rect_.add(aRect);

    changes_.firePropertyChange("rectAdded", true, false);
    return aRect;
  }

  private boolean removeRect(Annote rect) {
    boolean result = false;
    if (rect instanceof Annote.Rect) {
      rect.removePropertyChangeListener(this);
      result = rect_.remove(rect);
      if (result) changes_.firePropertyChange("rectRemoved", true, false);
    }
    return result;
  }

  public Iterator getRectIterator() {
    return rect_.iterator();
  }

  public boolean hasRect() {
    return !rect_.isEmpty();
  }

  public Annote findAnnote(String id) {
    Annote tmp = null;
    Iterator iter;
    if (!line_.isEmpty()) {
      iter = line_.iterator();
      while (iter.hasNext()) {
        tmp = (Annote) iter.next();
        if (tmp.getAnnoteId().equals(id)) return tmp;
      }
    }
    if (!point_.isEmpty()) {
      iter = point_.iterator();
      while (iter.hasNext()) {
        tmp = (Annote) iter.next();
        if (tmp.getAnnoteId().equals(id)) return tmp;
      }
    }
    if (!oval_.isEmpty()) {
      iter = oval_.iterator();
      while (iter.hasNext()) {
        tmp = (Annote) iter.next();
        if (tmp.getAnnoteId().equals(id)) return tmp;
      }
    }
    if (!rect_.isEmpty()) {
      iter = rect_.iterator();
      while (iter.hasNext()) {
        tmp = (Annote) iter.next();
        if (tmp.getAnnoteId().equals(id)) return tmp;
      }
    }
    if (!text_.isEmpty()) {
      iter = text_.iterator();
      while (iter.hasNext()) {
        tmp = (Annote) iter.next();
        if (tmp.getAnnoteId().equals(id)) return tmp;
      }
    }
    return null;
  }

  public void setTitle(String title) {
    title_ = title;
  }

  @Override
  public String getTitle() {
    return title_;
  }

  @Override
  public SGLabel getKeyTitle() {
    return null;
  }

  public void setId(String id) {
    id_ = id;
  }

  @Override
  public String getId() {
    return id_;
  }

  @Override
  public SGTData copy() {
    /**
     * @todo: Implement this gov.noaa.pmel.sgt.dm.SGTData method
     */
    throw new UnsupportedOperationException("Method copy() not yet implemented.");
  }

  @Override
  public boolean isXTime() {
    return xTime_;
  }

  @Override
  public boolean isYTime() {
    return yTime_;
  }

  public void setXMetaData(SGTMetaData meta) {
    xMeta_ = meta;
  }

  @Override
  public SGTMetaData getXMetaData() {
    return xMeta_;
  }

  public void setYMetaData(SGTMetaData meta) {
    yMeta_ = meta;
  }

  @Override
  public SGTMetaData getYMetaData() {
    return yMeta_;
  }

  @Override
  public SoTRange getXRange() {
    return xRange_;
  }

  @Override
  public SoTRange getYRange() {
    return yRange_;
  }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener l) {
    changes_.addPropertyChangeListener(l);
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener l) {
    changes_.removePropertyChangeListener(l);
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    changes_.firePropertyChange(evt);
  }
}
