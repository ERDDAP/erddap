/*
 * $Id: ContourLevels.java,v 1.20 2002/02/25 19:01:11 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package gov.noaa.pmel.sgt;

import com.cohort.util.MustBe;
import com.cohort.util.String2;
import gov.noaa.pmel.util.Range2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Contains levels and line styles for contour graphics.
 *
 * @author Donald Denbo
 * @version $Revision: 1.20 $, $Date: 2002/02/25 19:01:11 $
 * @since 2.0
 */
public class ContourLevels implements Cloneable {
  /**
   * @label defaultAttr
   * @link aggregationByValue
   * @supplierCardinality 1
   */
  private DefaultContourLineAttribute defaultAttr_ = new DefaultContourLineAttribute();

  private Vector levels_ = new Vector();
  private Hashtable lineAttrMap_ = new Hashtable();
  private boolean sorted_ = false;

  /**
   * @label solid
   * @link aggregationByValue
   */
  private static ContourLineAttribute solid_ = new ContourLineAttribute(ContourLineAttribute.SOLID);

  /**
   * @label heavy
   * @link aggregationByValue
   */
  private static ContourLineAttribute heavy_ = new ContourLineAttribute(ContourLineAttribute.HEAVY);

  /**
   * @label dashed
   * @link aggregationByValue
   */
  private static ContourLineAttribute dashed_ =
      new ContourLineAttribute(ContourLineAttribute.DASHED);

  private PropertyChangeSupport changes_ = new PropertyChangeSupport(this);

  /**
   * @directed
   * @label lineAttrMap
   * @link aggregation
   * @supplierCardinality 1..*
   */
  private ContourLineAttribute lnkLineAttribute;

  /** Bob Simons added this to avoid memory leak problems. */
  public void releaseResources() throws Exception {
    try {
      if (levels_ != null) {
        levels_.clear();
        levels_ = null;
      }
      defaultAttr_ = null;
      if (lineAttrMap_ != null) {
        lineAttrMap_.clear();
        lineAttrMap_ = null;
      }
      solid_ = null;
      heavy_ = null;
      dashed_ = null;
      changes_ = null;
      lnkLineAttribute = null;
      if (JPane.debug) String2.log("sgt.ContourLevels.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  /** Construct a default <code>ContourLevels</code> object from a double[]. */
  public static ContourLevels getDefault(double[] array) {
    ContourLevels cl = new ContourLevels();
    double val = 0.0;
    for (int i = 0; i < array.length; i++) {
      cl.addLevel(array[i]);
    }
    return cl;
  }

  /** Construct a default <code>ContourLevels</code> object from a <code>Range2D</code>. */
  public static ContourLevels getDefault(Range2D range) {
    ContourLevels cl = new ContourLevels();
    double val = range.start;
    while (val <= range.end) {
      cl.addLevel(val);
      val = val + range.delta;
    }
    return cl;
  }

  /** Create a deep copy. */
  public ContourLevels copy() {
    ContourLevels newcls;
    try {
      newcls = (ContourLevels) clone();
      //      newcls.defaultAttr_ =
      //	(DefaultContourLineAttribute)this.defaultAttr_.copy();
      newcls.levels_ = (Vector) this.levels_.clone();
      newcls.lineAttrMap_ = (Hashtable) this.lineAttrMap_.clone();
    } catch (CloneNotSupportedException e) {
      newcls = null;
    }
    return newcls;
  }

  /** Get the contour level elements. */
  public Enumeration levelElements() {
    // try { //bob added
    // System.out.println("pre  ContourLevels.levelElements label(-4000)=" +
    // getContourLineAttribute(-4000).getLabelText());
    if (!sorted_) sort();
    // System.out.println("post ContourLevels.levelElements label(-4000)=" +
    // getContourLineAttribute(-4000).getLabelText());
    // } catch (Exception e) {}
    return levels_.elements();
  }

  /** Set a the <code>ContourLineAttribute</code> for a value. */
  public void setContourLineAttribute(double val, ContourLineAttribute l)
      throws ContourLevelNotFoundException {
    throw new MethodNotImplementedError();
  }

  /** Set a the <code>ContourLineAttribute</code> for an index. */
  public void setContourLineAttribute(int indx, ContourLineAttribute l)
      throws ContourLevelNotFoundException {
    throw new MethodNotImplementedError();
  }

  /** Get the <code>ContourLineAttribute</code> for a value. */
  public ContourLineAttribute getContourLineAttribute(double val)
      throws ContourLevelNotFoundException {
    ContourLineAttribute attr = (ContourLineAttribute) lineAttrMap_.get(Double.valueOf(val));
    // System.out.println("contourLevels.getContourLineAtt(" + val + ") label=" +
    // attr.getLabelText());
    if (attr == null) {
      throw new ContourLevelNotFoundException();
    }
    return attr;
  }

  /** Get the <code>ContourLineAttribute</code> for an index. */
  public ContourLineAttribute getContourLineAttribute(int indx)
      throws ContourLevelNotFoundException {
    if (!sorted_) sort();
    return getContourLineAttribute(getLevel(indx));
  }

  /** Get the <code>DefaultContourLineAtrribute</code> */
  public DefaultContourLineAttribute getDefaultContourLineAttribute() {
    return defaultAttr_;
  }

  /** Get the <code>DefaultContourLineAttribute</code> for index. */
  public DefaultContourLineAttribute getDefaultContourLineAttribute(int indx)
      throws ContourLevelNotFoundException {
    if (!sorted_) sort();
    return defaultAttr_.setContourLineAttribute(getContourLineAttribute(getLevel(indx)));
  }

  /** Get the <code>DefaultContourLineAttribute</code> for value. */
  public DefaultContourLineAttribute getDefaultContourLineAttribute(double val)
      throws ContourLevelNotFoundException {
    if (!sorted_) sort();
    return defaultAttr_.setContourLineAttribute(getContourLineAttribute(val));
  }

  /** Set the <code>DefaultContourLineAttribute</code> */
  public void setDefaultContourLineAttribute(DefaultContourLineAttribute attr) {
    defaultAttr_ = attr;
  }

  /** Add a contour level with default <code>ContourLineAttribute</code>. */
  public void addLevel(double val) {
    ContourLineAttribute attr = null;
    if (val < 0.0) {
      attr = (ContourLineAttribute) dashed_.copy();
    } else if (val > 0.0) {
      attr = (ContourLineAttribute) solid_.copy();
    } else {
      attr = (ContourLineAttribute) heavy_.copy();
    }
    attr.setStyleOverridden(true);
    attr.setLabelText("" + val); // bob added
    addLevel(val, attr);
  }

  /** Add a contour level with a specified <code>ContourLineAttribute</code>. */
  public void addLevel(double val, ContourLineAttribute l) {
    Double value = Double.valueOf(val);
    levels_.addElement(value);
    // System.out.println("contourLevels.addLevel(" + val + ") label=" + l.getLabelText() + "\n" +
    //    com.cohort.util.MustBe.getStackTrace());
    lineAttrMap_.put(value, l);
    sorted_ = false;
  }

  /** Get the value of level by index. */
  public double getLevel(int indx) throws ContourLevelNotFoundException {
    if (indx < 0 || indx >= levels_.size()) throw new ContourLevelNotFoundException();
    if (!sorted_) sort();
    Double value = (Double) levels_.elementAt(indx);
    return value.doubleValue();
  }

  /** Remove a level by value. */
  public void removeLevel(double val) throws ContourLevelNotFoundException {
    throw new MethodNotImplementedError();
  }

  /** Remove a level by index. */
  public void removeLevel(int indx) throws ContourLevelNotFoundException {
    throw new MethodNotImplementedError();
  }

  /** Get the index of a level by value */
  public int getIndex(Double dval) {
    if (!sorted_) sort();
    return levels_.indexOf(dval);
  }

  /** Get the index of a level by value */
  public int getIndex(double val) {
    if (!sorted_) sort();
    return getIndex(Double.valueOf(val));
  }

  /** Get the maximum level index. */
  public int getMaximumIndex() {
    return levels_.size() - 1;
  }

  /** Get the range of levels */
  public Range2D getRange() {
    double min = Double.MAX_VALUE;
    double max = -Double.MAX_VALUE; // bob changed this
    double value;
    for (int i = 0; i < levels_.size(); i++) {
      value = ((Double) levels_.get(i)).doubleValue();
      min = Math.min(min, value);
      max = Math.max(max, value);
    }
    return new Range2D(min, max);
  }

  /** Get the number of levels. */
  public int size() {
    return levels_.size();
  }

  private void sort() {
    //
    // use brain-dead bubble sort (there will be few lines)
    //
    int i, temp;
    int size = levels_.size();
    Double a, b;
    int[] index = new int[size];
    boolean flipped = true;
    for (i = 0; i < size; i++) {
      index[i] = i;
    }
    while (flipped) {
      flipped = false;
      for (i = 0; i < size - 1; i++) {
        a = (Double) levels_.elementAt(index[i]);
        b = (Double) levels_.elementAt(index[i + 1]);
        if (a.doubleValue() > b.doubleValue()) {
          //	  if(a.compareTo(b) > 0) { // jdk1.2
          temp = index[i];
          index[i] = index[i + 1];
          index[i + 1] = temp;
          flipped = true;
        }
      }
    }
    Vector oldValues = levels_;
    levels_ = new Vector(size);
    for (i = 0; i < size; i++) {
      levels_.addElement(oldValues.elementAt(index[i]));
    }
    sorted_ = true;
  }

  /** Add listener to changes in <code>ColorMap</code> properties. */
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    changes_.addPropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    changes_.removePropertyChangeListener(listener);
  }
}
