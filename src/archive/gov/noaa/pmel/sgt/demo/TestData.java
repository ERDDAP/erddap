/*
 * $Id: TestData.java,v 1.8 2003/08/22 23:02:38 dwd Exp $
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

import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.TimeRange;
import gov.noaa.pmel.util.GeoDateArray;

import gov.noaa.pmel.sgt.dm.SGTData;
import gov.noaa.pmel.sgt.dm.SGTPoint;
import gov.noaa.pmel.sgt.dm.SGTLine;
import gov.noaa.pmel.sgt.dm.SGTGrid;
import gov.noaa.pmel.sgt.dm.SGTMetaData;
import gov.noaa.pmel.sgt.dm.SimplePoint;
import gov.noaa.pmel.sgt.dm.SimpleLine;
import gov.noaa.pmel.sgt.dm.SimpleGrid;
import gov.noaa.pmel.sgt.dm.Collection;
import gov.noaa.pmel.sgt.SGLabel;

/**
 * Create <code>SGTData</code> objects containing test data.
 * These objects can be used for
 * graphics and analysis testing purposes.
 *
 * @author Donald Denbo
 * @version $Revision: 1.8 $, $Date: 2003/08/22 23:02:38 $
 * @since 1.0
 */
public class TestData {
  /**
   * create profile data
   **/
  public final static int PROFILE = 1;
  /**
   * create longitude track data
   **/
  public final static int X_SERIES = 2;
  /**
   * create latitude track data
   **/
  public final static int Y_SERIES = 3;
  /**
   * create time series data
   **/
  public final static int TIME_SERIES = 4;
  /**
   * create X-Y grid
   */
  public final static int XY_GRID = 5;
  /**
   * create X-Y grid
   */
  public final static int XZ_GRID = 6;
  /**
   * create X-Y grid
   */
  public final static int YZ_GRID = 7;
  /**
   * create Z-T grid
   */
  public final static int ZT_GRID = 10;
  /**
   * create log-log line
   */
  public final static int LOG_LOG = 11;
  /**
   * create sine wave
   **/
  public final static int SINE = 1;
  /**
   * create random data
   **/
  public final static int RANDOM = 2;
  /**
   * create sine wave with a ramp
   */
  public final static int SINE_RAMP = 3;
  //
  private int dataType_;
  private SGTData data_;
  private Collection coll_;
  /**
   * Create default test data. Will create profile (z) data with sine shape.
   **/
  public TestData() {
    this(PROFILE, new Range2D(0.0f, 100.0f, 10.0f), SINE, 1.0f, 0.0f, 25.0f);
  }
  /**
   * Create one-dimensional spatial test data.
   *
   * @param dir direction of test data, TestData.X_SERIES,
   * TestData.Y_SERIES, TestData.PROFILE, TestData.LOG_LOG
   * @param range minimum and maximum overwhich to create
   * @param type type of series, TestData.SINE or TestData.RANDOM
   * @param amp amplitude
   * @param off offset
   * @param per period
   * @see #PROFILE
   * @see #X_SERIES
   * @see #Y_SERIES
   * @see #LOG_LOG
   * @see #SINE
   * @see #RANDOM
   **/
  public TestData(int dir, Range2D range,
                  int type, float amp, float off, float per) {
    SimpleLine sl;
    SGTMetaData xMeta;
    SGTMetaData yMeta;

    double[] axis;
    double[] values;
    double[] zero;
    GeoDate tzero[] = new GeoDate[1];
    int count;

    zero = new double[1];
    tzero[0] = new GeoDate();
    zero[0] = 0;
    tzero[0].now();
    if(dir != LOG_LOG) {
      int num = (int)((range.end - range.start)/range.delta) + 1;
      axis = new double[num];

      for(count=0; count < num; count++) {
        axis[count] = range.start + count*range.delta;
      }

      values = getValues(axis, num, type, amp, off, per);
    } else {
      double log10 = Math.log(10.0);
      double end10 = Math.log10(range.end);
      double start10 = Math.log10(range.start);
      double delta10 = Math.log10(range.delta);
      float amp10 = (float)Math.log10(amp);
      float off10 = (float)Math.log10(off);
      float per10 = (float)Math.log10(per);
      int num = (int)((end10 - start10)/delta10) + 1;
      axis = new double[num];

      for(count=0; count < num; count++) {
        axis[count] = start10 + count*delta10;
      }

      // create values in log space
      values = getValues(axis, num, type, amp10, off10, per10);

      // convert back ...
      for(count=0; count < num; count++) {
        axis[count] = Math.pow(10.0, axis[count]);
        values[count] = Math.pow(10.0, values[count]);
      }
    }
    SGLabel keyLabel = new SGLabel("Key Label", "", new Point2D.Double(0.0, 0.0));
    keyLabel.setHeightP(0.16);
    //
    // create SimpleLine object
    //
    switch(dir) {
    case X_SERIES:
      keyLabel.setText("X series ts data");
      xMeta = new SGTMetaData("lon", "degE");
      yMeta = new SGTMetaData("ts", "m s-1");
      sl = new SimpleLine(axis, values, "Test Series");
      break;
    case Y_SERIES:
      keyLabel.setText("Y series ts data");
      xMeta = new SGTMetaData("lat", "deg");
      yMeta = new SGTMetaData("ts", "m s-1");
      sl = new SimpleLine(axis, values, "Test Series");
      break;
    default:
    case PROFILE:
      keyLabel.setText("Profile ts data");
      xMeta = new SGTMetaData("ts", "m s-1");
      yMeta = new SGTMetaData("depth", "m");
      sl = new SimpleLine(values, axis, "Test Series");
      break;
    case LOG_LOG:
      keyLabel.setText("Log/Log diameters");
      xMeta = new SGTMetaData("diameter", "m");
      yMeta = new SGTMetaData("count", "");
      sl = new SimpleLine(axis, values, "Test Log-Log Series");
    }
    sl.setXMetaData(xMeta);
    sl.setYMetaData(yMeta);
    sl.setKeyTitle(keyLabel);
    data_ = sl;
  }
  /**
   * Create two-dimensional spatial test data.
   *
   * @param dir direction of test data, TestData.XY_GRID, TestData.XZ_GRID, TestData.YZ_GRID
   * @param range1 minimum and maximum overwhich to create for first axis
   * @param range2 minimum and maximum overwhich to create for second axis
   * @param type type of series, TestData.SINE or TestData.RANDOM
   * @param amp amplitude
   * @param off offset
   * @param per period
   * @see #PROFILE
   * @see #X_SERIES
   * @see #Y_SERIES
   * @see #SINE
   * @see #RANDOM
   **/
  public TestData(int dir, Range2D range1, Range2D range2,
                  int type, float amp, float off, float per) {
    SimpleGrid sg;
    SGTMetaData xMeta;
    SGTMetaData yMeta;
    SGTMetaData zMeta;

    double[] axis1, axis2;
    double[] values;
    double[] zero;
    GeoDate tzero[] = new GeoDate[1];
    int count;

    zero = new double[1];
    tzero[0] = new GeoDate();
    zero[0] = 0;
    tzero[0].now();
    int num1 = (int)((range1.end - range1.start)/range1.delta) + 1;
    axis1 = new double[num1];
    int num2 = (int)((range2.end - range2.start)/range2.delta) + 1;
    axis2 = new double[num2];

    for(count=0; count < num1; count++) {
      axis1[count] = range1.start + count*range1.delta;
    }
    for(count=0; count < num2; count++) {
      axis2[count] = range2.start + count*range2.delta;
    }

    values = getValues(axis1, num1, axis2, num2, type, amp, off, per);

    SGLabel keyLabel = new SGLabel("Key Label", "", new Point2D.Double(0.0, 0.0));
    keyLabel.setHeightP(0.16);
    //
    // create SimpleGrid
    //
    zMeta = new SGTMetaData("ts", "m s-1");
    switch(dir) {
    case XY_GRID:
      keyLabel.setText("XY test grid");
      xMeta = new SGTMetaData("lon", "degE");
      yMeta = new SGTMetaData("lat", "deg");
      sg = new SimpleGrid(values, axis1, axis2, "Test Series");
      break;
    case XZ_GRID:
      keyLabel.setText("XZ test grid");
      xMeta = new SGTMetaData("lon", "degE");
      yMeta = new SGTMetaData("depth", "m");
      sg = new SimpleGrid(values, axis1, axis2, "Test Series");
      break;
    default:
    case YZ_GRID:
      keyLabel.setText("YZ test grid");
      xMeta = new SGTMetaData("lat", "deg");
      yMeta = new SGTMetaData("depth", "m");
      sg = new SimpleGrid(values, axis1, axis2, "Test Series");
    }
    sg.setXMetaData(xMeta);
    sg.setYMetaData(yMeta);
    sg.setZMetaData(zMeta);
    sg.setKeyTitle(keyLabel);
    data_ = sg;
  }
  /**
   * Create time series test data.
   *
   * @param dir direction of test data, TestData.TIME_SERIES
   * @param range minimum and maximum overwhich to create
   * @param delta space between points in days
   * @param type type of series, TestData.SINE or TestData.RANDOM
   * @param amp amplitude
   * @param off offset
   * @param per period
   * @see #PROFILE
   * @see #X_SERIES
   * @see #Y_SERIES
   * @see #SINE
   * @see #RANDOM
   **/
  public TestData(int dir, TimeRange range, float delta,
                  int type, float amp, float off, float per) {
    SimpleLine sl;
    SGTMetaData xMeta;
    SGTMetaData yMeta;

    GeoDateArray taxis;
    long[] tvalues;
    double[] values;
    double[] zero;
    double totalTime;
    int count, num;

    zero = new double[1];
    zero[0] = 0;

    totalTime = range.end.offset(range.start);
    num = (int)(totalTime/(double)delta) + 1;

    tvalues = new long[num];
    long ref = range.start.getTime();
    long ldelta = ((long)delta)*86400000;
//    taxis = new GeoDate[num];
    for(count=0; count < num; count++) {
      tvalues[count] = ref + count*ldelta;
//      taxis[count] = new GeoDate(range.start);
//      taxis[count].increment(delta*count, GeoDate.DAYS);
    }
    taxis = new GeoDateArray(tvalues);
    values = getValues(taxis, num, type, amp, off, per);

    SGLabel keyLabel = new SGLabel("Key Label", "", new Point2D.Double(0.0, 0.0));
    keyLabel.setHeightP(0.16);
    //
    // create SimpleLine
    //
    keyLabel.setText("Time series ts");
    xMeta = new SGTMetaData("time", "");
    yMeta = new SGTMetaData("ts", "m s-1");
    sl = new SimpleLine(taxis, values, "Test Series");
    sl.setXMetaData(xMeta);
    sl.setYMetaData(yMeta);
    sl.setKeyTitle(keyLabel);
    data_ = sl;
  }
  /**
   * Create time series test grid data.
   *
   * @param dir direction of test data, TestData.ZT_SERIES
   * @param range minimum and maximum for space axis
   * @param trange minimum and maximum overwhich to create
   * @param delta space between points in days
   * @param type type of series, TestData.SINE or TestData.RANDOM
   * @param amp amplitude
   * @param off offset
   * @param per period
   * @see #PROFILE
   * @see #X_SERIES
   * @see #Y_SERIES
   * @see #SINE
   * @see #RANDOM
   **/
  public TestData(int dir, Range2D range,
                  TimeRange trange, float delta,
                  int type, float amp, float off, float per) {
    SimpleGrid sg;
    SGTMetaData xMeta;
    SGTMetaData yMeta;
    SGTMetaData zMeta;

    GeoDate[] taxis;
    double[] values, axis;
    double[] zero;
    double totalTime;
    int count, tnum, snum;

    zero = new double[1];
    zero[0] = 0;

    totalTime = trange.end.offset(trange.start);
    tnum = (int)(totalTime/(double)delta) + 1;

    taxis = new GeoDate[tnum];
    for(count=0; count < tnum; count++) {
      taxis[count] = new GeoDate(trange.start);
      taxis[count].increment(delta*count, GeoDate.DAYS);
    }

    snum = (int)((range.end - range.start)/range.delta) + 1;
    axis = new double[snum];

    for(count=0; count < snum; count++) {
      axis[count] = range.start + count*range.delta;
    }

    values = getValues(axis, snum, taxis, tnum, type, amp, off, per);
    SGLabel keyLabel = new SGLabel("Key Label", "", new Point2D.Double(0.0, 0.0));
    keyLabel.setHeightP(0.16);
    //
    // create SimpleGrid
    //
    keyLabel.setText("time-depth grid");
    xMeta = new SGTMetaData("time", "");
    yMeta = new SGTMetaData("depth", "m");
    zMeta = new SGTMetaData("ts", "m s-1");
    sg = new SimpleGrid(values, taxis, axis, "Test Series");
    sg.setXMetaData(xMeta);
    sg.setYMetaData(yMeta);
    sg.setZMetaData(zMeta);
    sg.setKeyTitle(keyLabel);
    data_ = sg;
  }
  /**
   * Create a Collection of points.
   *
   * @param xrnge range of values in x
   * @param yrnge range of values in y
   * @param num number of values to create
   */
  public TestData(Range2D xrnge, Range2D yrnge, int num) {
    SimplePoint sp;
    double xamp, yamp, xoff, yoff;
    double xp, yp;
    String title;
    coll_ = new Collection("Test Points", num);
    xamp = xrnge.end - xrnge.start;
    xoff = xrnge.start;
    yamp = yrnge.end - yrnge.start;
    yoff = yrnge.start;
    for(int i=0; i < num; i++) {
      xp = xamp*Math.random() + xoff;
      yp = yamp*Math.random() + yoff;
      title = "point"+i;
      sp = new SimplePoint(xp, yp, title);
      coll_.addElement((Object)sp);
    }
  }

  /**
   * Get the Collection of points created with the point constructor.
   *
   * @return Collection of points
   */
  public Collection getCollection() {
    return coll_;
  }

  /**
   * Get a SGTGrid or SGTLine object depending on type of constructor
   * used.
   *
   * @return SGTData object.
   */
  public SGTData getSGTData() {
    return data_;
  }
  private double[] getValues(double[] axis, int num, int type,
                             float amp, float off, float per) {
    double[] values;
    int count;
    values = new double[num];
    switch(type) {
    default:
    case TestData.RANDOM:
      for(count=0; count < num; count++) {
        values[count] = amp*Math.random()+off;
      }
      break;
    case TestData.SINE:
      for(count=0; count < num; count++) {
        values[count] = amp*Math.sin(axis[count]/per) + off;
      }
    }
    return values;
  }

  private double[] getValues(double[] axis1, int num1, double[] axis2, int num2,
                             int type, float amp, float off, float per) {
    double[] values;
    int count1, count2, count;
    values = new double[num1*num2];
    switch(type) {
    default:
    case TestData.RANDOM:
      for(count=0; count < num1*num2; count++) {
        values[count] = amp*Math.random()+off;
      }
      break;
    case TestData.SINE:
      count=0;
      for(count1=0; count1 < num1; count1++) {
        for(count2=0; count2 < num2; count2++) {
          values[count] = amp*Math.sin(axis1[count1]/per)*
            Math.sin(axis2[count2]/per) + off;
          count++;
        }
      }
      break;
    case TestData.SINE_RAMP:
      count=0;
      double ax1factr = 0.08*Math.abs(axis1[0]-axis1[num1-1])/
  Math.max(Math.abs(axis1[0]),Math.abs(axis1[num1-1]));
      double ax2factr = 0.08*Math.abs(axis2[0]-axis2[num2-1])/
  Math.max(Math.abs(axis2[0]),Math.abs(axis2[num2-1]));
      for(count1=0; count1 < num1; count1++) {
        for(count2=0; count2 < num2; count2++) {
          values[count] = amp*Math.sin(axis1[count1]/per)*
            Math.sin(axis2[count2]/per) + off +
      amp*(ax1factr*count1 - ax2factr*count2);
          count++;
        }
      }

    }
    return values;
  }

  private double[] getValues(GeoDate[] axis, int num, int type,
                             float amp, float off, float per) {
    double[] values;
    double theta;
    int count;
    values = new double[num];
    switch(type) {
    default:
    case TestData.RANDOM:
      for(count=0; count < num; count++) {
        values[count] = amp*Math.random() + off;
      }
      break;
    case TestData.SINE:
      for(count=0; count < num; count++) {
        theta = axis[count].offset(axis[0])/per;
        values[count] = amp*Math.sin(theta) + off;
      }
    }
    return values;
  }

  private double[] getValues(GeoDateArray axis, int num, int type,
                             float amp, float off, float per) {
    double[] values;
    double theta;
    float lper = per*86400000;
    long[] tvalues = axis.getTime();
    int count;
    values = new double[num];
    switch(type) {
    default:
    case TestData.RANDOM:
      for(count=0; count < num; count++) {
        values[count] = amp*Math.random() + off;
      }
      break;
    case TestData.SINE:
      for(count=0; count < num; count++) {
        theta = (tvalues[count] - tvalues[0])/lper;
 //       theta = axis[count].offset(axis[0])/per;
        values[count] = amp*Math.sin(theta) + off;
      }
    }
    return values;
  }

  private double[] getValues(double[] axis, int snum, GeoDate[] taxis, int tnum,
                             int type, float amp, float off, float per) {
    double[] values;
    double theta;
    int tcount, scount, count;
    values = new double[tnum*snum];
    switch(type) {
    default:
    case TestData.RANDOM:
      for(count=0; count < tnum*snum; count++) {
        values[count] = amp*Math.random() + off;
      }
      break;
    case TestData.SINE:
      count=0;
      for(tcount=0; tcount < tnum; tcount++) {
        theta = taxis[tcount].offset(taxis[0])/per;
        for(scount=0; scount < snum; scount++) {
          values[count] = amp*Math.sin(theta)*Math.sin(axis[scount]/per) + off;
          count++;
        }
      }
    }
    return values;
  }
}
