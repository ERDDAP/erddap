/*
 * $Id: PseudoRealTimeData.java,v 1.9 2003/09/18 18:48:03 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */
package gov.noaa.pmel.sgt.demo;

import gov.noaa.pmel.sgt.SGLabel;
import gov.noaa.pmel.sgt.dm.SGTLine;
import gov.noaa.pmel.sgt.dm.SGTData;
import gov.noaa.pmel.sgt.dm.SGTMetaData;
import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.util.GeoDateArray;
import gov.noaa.pmel.util.SoTRange;
import gov.noaa.pmel.util.IllegalTimeValue;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

/**
 * Generates a real-time data stream using <code>SGTLine</code> and
 * <code>javax.swing.Timer</code>. <code>PseudoRealTimeData</code>
 * generates <code>PropertyCchangeEvent</code>s
 * whenever data is added "dataModified" or the data range changes
 * "rangeModified". The "dataModified" event is directly handled by
 * <code>sgt</code> and the "rangeModified" event needs to be handled
 * by the graphics application.
 *
 * <p> <code>PseudoRealTimeData</code> demonstrates how a class that
 * implements the <code>SGTLine</code> interface can use the
 * <code>getXRange()</code> and <code>getYRange()</code> methods to
 * produce "nice" plots.  This class updates the data each time step,
 * but updates the range only after a day has passed.
 *
 * @author Donald Denbo
 * @version $Revision: 1.9 $, $Date: 2003/09/18 18:48:03 $
 * @since 2.0
 */

public class PseudoRealTimeData implements SGTLine, ActionListener {
  private SGTMetaData xMeta_;
  private SGTMetaData yMeta_;
  private SoTRange.GeoDate xRange_;
  private SoTRange.Double yRange_;
  private GeoDate[] xData_;
  private double[] yData_;
  private GeoDate tend_;
  private int count_;
  private String title_;
  private SGLabel keyTitle_ = null;
  private String id_;
  private Timer timer_;
  private PropertyChangeSupport changes_ = new PropertyChangeSupport(this);
  private GeoDate ref_ = null;
  // bufsize should be integral multiple of majorIncrement plus 1
  private int bufsize_ = 241;
  private int offset_;
  // offsetIncrement should be same as majorIncrement
  private int offsetIncrement_ = 24;

  private double minorIncrement_ = 1.0;
  private double majorIncrement_ = 24.0;
  private int units_ = GeoDate.HOURS;

  private double A0_ = 1.0;
  private double A1_ = 0.375;
  private double A2_ = 0.2;
  private double omega0_ = 0.251327412;
  private double omega1_ = 0.3;
  /**
   * Constructor.
   */
  public PseudoRealTimeData(String id, String title) {
    xMeta_ = new SGTMetaData("Time", "");
    yMeta_ = new SGTMetaData("PseudoData", "Ps/day");
    title_ = title;
    id_ = id;
    timer_ = new Timer(250, this);
    resetData();
  }
  /**
   * Get x data array.  Always returns <code>null</code>.
   */
  public double[] getXArray() {
    return null;
  }
  /**
   * Get y data values. Creates a copy of the buffer array.
   */
  public double[] getYArray() {
    if(count_ > 0) {
      double[] temp = new double[count_+offset_];
      for(int i=0; i < count_+offset_; i++) {
        temp[i] = yData_[i];
      }
      return temp;
    } else {
      return null;
    }
  }
  public GeoDate[] getTimeArray() {
    if(count_ > 0) {
      GeoDate[] temp = new GeoDate[count_+offset_];
      for(int i=0; i < count_+offset_; i++) {
        temp[i] = xData_[i];
      }
      return temp;
    } else {
      return null;
    }
  }
  /**
   * @since 3.0
   */
  public GeoDateArray getGeoDateArray() {
    return new GeoDateArray(getTimeArray());
  }
  public SGTLine getAssociatedData() {
    return null;
  }
  public boolean hasAssociatedData() {
    return false;
  }
  public String getTitle() {
    return title_;
  }
  public SGLabel getKeyTitle() {
    return keyTitle_;
  }
  public String getId() {
    return id_;
  }
  public SGTData copy() {
    return null;
  }
  public boolean isXTime() {
    return true;
  }
  public boolean isYTime() {
    return false;
  }
  public SGTMetaData getXMetaData() {
    return xMeta_;
  }
  public SGTMetaData getYMetaData() {
    return yMeta_;
  }
  public SoTRange getXRange() {
    return xRange_.copy();
  }
  public SoTRange getYRange() {
    return yRange_.copy();
  }
  public void addPropertyChangeListener(PropertyChangeListener l) {
    changes_.addPropertyChangeListener(l);
  }
  public void removePropertyChangeListener(PropertyChangeListener l) {
    changes_.removePropertyChangeListener(l);
  }
  /**
   * Start the timer and begin/continue generating property change events.
   */
  public void startData() {
    timer_.start();
  }
  /**
   * Stop the timer.
   */
  public void stopData() {
    timer_.stop();
  }
  /**
   * Reset the demonstration to the begining.
   */
  public void resetData() {
    xData_ = new GeoDate[bufsize_];
    yData_ = new double[bufsize_];
    try {
      //2011-12-15 Bob Simons changed space to 'T'
      ref_ = new GeoDate("1999-01-01T00:00", "yyyy-MM-dd'T'HH:mm");
    } catch (IllegalTimeValue e) {
      e.printStackTrace();
    }
    tend_ = new GeoDate(ref_);
    // Add a little fudge to get last tic on the axis
    tend_.increment(10.0, GeoDate.SECONDS);
    yRange_ = new SoTRange.Double(-1.5, 1.5);
    xRange_ = new SoTRange.GeoDate(new GeoDate(ref_),
                                    tend_.increment(majorIncrement_, units_));
    xData_[0] = new GeoDate(ref_);
    yData_[0] = 0.0;
    count_ = 1;
    offset_ = 0;
  }

  /**
   * Handle timer ActionEvents
   * <BR><strong>Property Change:</strong> <code>rangeModified</code> and
   * <code>DataModified</code>
   */
  public void actionPerformed(ActionEvent e) {
    if((count_+offset_) >= bufsize_) {
      offset_ = offset_ - offsetIncrement_;
      for(int i=0; i < bufsize_-offsetIncrement_; i++) {
        xData_[i] = xData_[i+offsetIncrement_];
        yData_[i] = yData_[i+offsetIncrement_];
      }
      xRange_.start = xData_[0];
    }
    xData_[count_+offset_] = new GeoDate(ref_.increment(minorIncrement_, units_));
    yData_[count_+offset_] = tSeries(count_);
    if(xData_[count_+offset_].after(tend_)) {
      SoTRange.GeoDate oldRange = (SoTRange.GeoDate)xRange_.copy();
      /**
       * compute new range
       */
      tend_.increment(majorIncrement_, units_);
      xRange_.end = tend_;
      changes_.firePropertyChange("rangeModified", oldRange, xRange_);
    } else {
      changes_.firePropertyChange("dataModified",
                                  Integer.valueOf(count_),
                                  Integer.valueOf(count_+1));
    }
    count_++;
  }


    /** 
     * Bob Simons added this to avoid memory leak problems.
     */
    public void releaseResources() throws Exception {
        try {  
            //???
            //if (JPane.debug) String2.log("sgt.demo.PseudoRealTimeData.releaseResources() finished");
        } catch (Throwable t) {
            //String2.log(MustBe.throwableToString(t));
            //if (JPane.debug) String2.pressEnterToContinue(); 
        }
    }

  private double tSeries(int val) {
    return A0_*Math.sin(omega0_*val)+A1_*Math.sin(omega1_*val)+A2_*Math.random();
  }
}
