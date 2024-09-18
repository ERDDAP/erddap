/*
 * CompoundColorMap Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
// This class is loosely based on gov.noaa.pmel.util.ColorMap.
package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.google.common.io.Resources;
import gov.noaa.pmel.sgt.ColorMap;
import gov.noaa.pmel.util.Range2D;
import java.awt.Color;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class mimics the behavior of a GMT Color Palette Table (.cpt) file. (was
 * https://www.soest.hawaii.edu/gmt/html/GMT_Docs.html#x1-720004.15 ) Note that log ranges can be
 * simulated by a series of ranges (each of which is actually linearly interpolated).
 */
public class CompoundColorMap extends ColorMap {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  /**
   * Set this to true (by calling reallyVerbose=true in your program, not by changing the code here)
   * if you want lots and lots of diagnostic messages sent to String2.log.
   */
  public static boolean reallyVerbose = false;

  /** Set in the constructor. */
  public double rangeLow[]; // stores the low ends of a piece

  public double rangeHigh[]; // stores the high ends of a piece
  protected int rLow[]; // stores the red   value for the low  end of a piece
  protected int rHigh[]; // stores the red   value for the high end of a piece
  protected int gLow[]; // stores the green value for the low  end of a piece
  protected int gHigh[]; // stores the green value for the high end of a piece
  protected int bLow[]; // stores the blue  value for the low  end of a piece
  protected int bHigh[]; // stores the blue  value for the high end of a piece
  public String leftLabel[]; // stores the label for the low end of the piece
  public String lastLabel; // stores the label for the high end of the final piece
  protected Color backgroundColor = Color.BLACK;
  protected Color foregroundColor = Color.WHITE;
  protected Color NaNColor = Color.GRAY;
  protected StringBuilder annotationFlags = new StringBuilder();

  /** Set in the constructor. */
  protected boolean continuous; // true if one or more ranges have a non-0 r g or b range

  protected double range1024[]; // stores the (rangeHigh-rangeLow)/1024 for a piece
  protected int rRange[]; // stores the rHigh-rLow
  protected int gRange[]; // stores the gHigh-gLow
  protected int bRange[]; // stores the bHigh-bLow
  protected double rangeMin;
  protected double rangeMax;
  protected int halfI;
  protected double halfStart;
  protected Color color[]; // used only if !continuous

  /**
   * The cumulative variables are used to collect statistics. See resetStats() and getStats(). It
   * takes significant time to collect statistics, so usually turned off (see TESTING ON/OFF and
   * resetStats() below).
   */
  public long cumulativeLookupTime = -1; // -1 indicate not used

  public long cumulativeTotalTime = -1; // -1 indicate not used
  public long cumulativeCount = 0; // 0 indicates not used

  /** This returns a shallow copy of this colormap. */
  @Override
  public ColorMap copy() {
    CompoundColorMap ccm = new CompoundColorMap();
    ccm.rangeLow = rangeLow;
    ccm.rangeHigh = rangeHigh;
    ccm.range1024 = range1024;
    ccm.rLow = rLow;
    ccm.rHigh = rHigh;
    ccm.rRange = rRange;
    ccm.gLow = gLow;
    ccm.gHigh = gHigh;
    ccm.gRange = gRange;
    ccm.bLow = bLow;
    ccm.bHigh = bHigh;
    ccm.bRange = bRange;
    ccm.rangeMin = rangeMin;
    ccm.rangeMax = rangeMax;
    ccm.annotationFlags = annotationFlags;
    ccm.backgroundColor = backgroundColor;
    ccm.foregroundColor = foregroundColor;
    ccm.NaNColor = NaNColor;
    ccm.halfI = halfI;
    ccm.halfStart = halfStart;
    ccm.continuous = continuous;
    ccm.color = color;
    return (ColorMap) ccm;
  }

  /** This consructs an empty CompoundColorMap. This used by copy(). */
  public CompoundColorMap() {}

  /**
   * This is essentially <tt>this(makeCPT())</tt> -- it makes the file and then loads it. See the
   * makeCpt parameters.
   */
  public CompoundColorMap(
      String baseDir,
      String palette,
      String scale,
      double minData,
      double maxData,
      int nSections,
      boolean continuous,
      String resultDir)
      throws Exception {

    this(makeCPT(baseDir, palette, scale, minData, maxData, nSections, continuous, resultDir));
  }

  /**
   * This constructs a CompoundColorMap based on the info in a GMT-style .cpt file. Compared to GMT:
   *
   * <ul>
   *   <li>Named colors, single-value gray, "-", CMYK, and HSV colors are currently not supported.
   *       Only RGB is supported.
   * </ul>
   *
   * @param cptFileName the complete name of the .cpt file
   */
  public CompoundColorMap(String cptFileName) throws Exception {
    List<String> lines = File2.readLinesFromFile(cptFileName, File2.ISO_8859_1, 3);
    populate(this, cptFileName, lines);
  }

  public CompoundColorMap(URL cptFileName) throws Exception {
    List<String> lines = Resources.readLines(cptFileName, StandardCharsets.ISO_8859_1);
    populate(this, cptFileName.getFile(), lines);
  }

  /**
   * This constructs a CompoundColorMap based on a .cpt file. It is used by some of the
   * constructors.
   */
  protected static void populate(CompoundColorMap ccm, String cptFileName, List<String> lines)
      throws Exception {
    // set up a colorMap based on info in the .cpt file
    // set up temporary PrimitiveArrays
    DoubleArray rangeLowAr = new DoubleArray(); // stores the low ends of a piece
    DoubleArray rangeHighAr = new DoubleArray(); // stores the high ends of a piece
    IntArray rLowAr = new IntArray(); // stores the red   value for the low  end of a piece
    IntArray rHighAr = new IntArray(); // stores the red   value for the high end of a piece
    IntArray gLowAr = new IntArray(); // stores the green value for the low  end of a piece
    IntArray gHighAr = new IntArray(); // stores the green value for the high end of a piece
    IntArray bLowAr = new IntArray(); // stores the blue  value for the low  end of a piece
    IntArray bHighAr = new IntArray(); // stores the blue  value for the high end of a piece
    StringArray leftLabelAr = new StringArray();
    ccm.annotationFlags.setLength(0);

    // go through the lines of the file
    int nLines = lines.size();
    for (int i = 0; i < nLines; i++) {
      // go through the lines
      String tLine = lines.get(i);
      if (tLine.startsWith("#")) continue; // a comment
      String[] items = String2.split(tLine, '\t');
      if (items.length >= 4) {
        double low = String2.parseDouble(items[0]);
        if (!Double.isNaN(low) && items.length >= 8) {
          // it is a color range
          // String2.log(String2.toCSSVString(items));
          double high = String2.parseDouble(items[4]);
          rangeLowAr.add(low);
          rangeHighAr.add(high);
          leftLabelAr.add(String2.genX10Format6(low));
          rLowAr.add(String2.parseInt(items[1]));
          rHighAr.add(String2.parseInt(items[5]));
          gLowAr.add(String2.parseInt(items[2]));
          gHighAr.add(String2.parseInt(items[6]));
          bLowAr.add(String2.parseInt(items[3]));
          bHighAr.add(String2.parseInt(items[7]));
          ccm.annotationFlags.append(items.length >= 9 ? items[8].charAt(0) : 'B');
        } else if (items[0].equals("B")) {
          ccm.setBackgroundColor(
              new Color(
                  String2.parseInt(items[1]),
                  String2.parseInt(items[2]),
                  String2.parseInt(items[3])));
        } else if (items[0].equals("F")) {
          ccm.setForegroundColor(
              new Color(
                  String2.parseInt(items[1]),
                  String2.parseInt(items[2]),
                  String2.parseInt(items[3])));
        } else if (items[0].equals("N")) {
          ccm.setNaNColor(
              new Color(
                  String2.parseInt(items[1]),
                  String2.parseInt(items[2]),
                  String2.parseInt(items[3])));
        } else {
          String2.log(
              String2.ERROR
                  + ": CompoundColorMap unexpected line in "
                  + cptFileName
                  + "\n"
                  + lines.get(i));
        }
      } else if (tLine.trim().length() > 0) {
        String2.log(
            String2.ERROR
                + ": CompoundColorMap unexpected line in "
                + cptFileName
                + "\n"
                + lines.get(i));
      }
    }

    // convert to arrays
    ccm.rangeLow = rangeLowAr.toArray();
    ccm.rangeHigh = rangeHighAr.toArray();
    ccm.rLow = rLowAr.toArray();
    ccm.rHigh = rHighAr.toArray();
    ccm.gLow = gLowAr.toArray();
    ccm.gHigh = gHighAr.toArray();
    ccm.bLow = bLowAr.toArray();
    ccm.bHigh = bHighAr.toArray();
    ccm.leftLabel = leftLabelAr.toArray();
    ccm.lastLabel = String2.genX10Format6(ccm.rangeHigh[ccm.rangeHigh.length - 1]);
    // String2.log("rLow size=" + ccm.rLow.length + " rangeLow size=" + ccm.rangeLow.length);

    ccm.finishUpConstruction();
  }

  /**
   * This makes a ccm for a date range. See the makeCpt parameters.
   *
   * @param dataIsMillis true if data is epoch millis, else data is epochSeconds.
   * @param atLeastNPieces e.g., 5; in general the resulting nPieces will be atLeastNPieces to
   *     2*atLeastNPieces. Use -1 to get the default.
   */
  public CompoundColorMap(
      String baseDir,
      String palette,
      boolean dataIsMillis,
      double minData,
      double maxData,
      int atLeastNPieces,
      boolean continuous,
      String resultDir)
      throws Exception {

    String errorInMethod =
        String2.ERROR
            + " in CompoundColorMap(dates, minData="
            + minData
            + " maxData="
            + maxData
            + "): ";

    // regardless of dataIsMillis, internal calculations are done in seconds
    // figure out increment's nth duration (e.g, 5th day)
    if (atLeastNPieces < 1) atLeastNPieces = 5;
    if (minData > maxData) {
      double d = minData;
      minData = maxData;
      maxData = d;
    }
    double minSeconds = minData / (dataIsMillis ? 1000 : 1);
    double maxSeconds = maxData / (dataIsMillis ? 1000 : 1);
    if (reallyVerbose)
      String2.log(
          "CompoundColorMap("
              + Calendar2.epochSecondsToIsoStringTZ(minSeconds)
              + " to "
              + Calendar2.epochSecondsToIsoStringTZ(maxSeconds)
              + ")");
    double secondsRange = maxSeconds - minSeconds;
    if (secondsRange < 10) {
      minSeconds -= 5;
      maxSeconds += 5;
      secondsRange = maxSeconds - minSeconds;
    }
    int duration;
    int durationNSec; // sometimes approximate, err on high side
    int minorDisplayBegin = 0; // position in 2006-01-03 12:03:05
    int minorDisplayEnd = 4; // 0,4 -> year
    int majorDisplayBegin = 5; // position in 2006-01-03 12:03:05
    int majorDisplayEnd = 7; // 5,7 -> month
    int majorTrigger; // if this field (e.g., YEAR) changes, display a major label
    int nthOptions[];
    int nthBase = 0; // eg. DATES are counted 1..; months (in GC) and seconds are 0..
    if (secondsRange
        > (atLeastNPieces - 2)
            * 365
            * Calendar2.SECONDS_PER_DAY) { // i.e., if nth=1, we would have atLeastNPieces
      // years
      duration = Calendar2.YEAR;
      durationNSec = 365 * Calendar2.SECONDS_PER_DAY;
      minorDisplayBegin = 0;
      minorDisplayEnd = 4;
      majorDisplayBegin = 0;
      majorDisplayEnd = 0;
      majorTrigger = Calendar2.SECOND; // goofy, but value won't ever change
      nthOptions = new int[] {1, 2, 5, 10, 20, 50, 100, 200, 500, 1000};
    } else if (secondsRange > (atLeastNPieces - 2) * 31 * Calendar2.SECONDS_PER_DAY) {
      // months
      duration = Calendar2.MONTH;
      durationNSec = 31 * Calendar2.SECONDS_PER_DAY;
      minorDisplayBegin = 5;
      minorDisplayEnd = 7;
      majorDisplayBegin = 0;
      majorDisplayEnd = 4;
      majorTrigger = Calendar2.YEAR;
      nthOptions = new int[] {1, 2, 3, 6};
    } else if (secondsRange > (atLeastNPieces - 2) * Calendar2.SECONDS_PER_DAY) {
      // days
      duration = Calendar2.DATE;
      durationNSec = Calendar2.SECONDS_PER_DAY;
      minorDisplayBegin = 8;
      minorDisplayEnd = 10;
      majorDisplayBegin = 0;
      majorDisplayEnd = 7;
      majorTrigger = Calendar2.MONTH;
      nthOptions = new int[] {1, 2, 4, 5, 10, 16};
      nthBase = 1;
    } else if (secondsRange > (atLeastNPieces - 2) * Calendar2.SECONDS_PER_HOUR) {
      // hours
      duration = Calendar2.HOUR_OF_DAY;
      durationNSec = Calendar2.SECONDS_PER_HOUR;
      minorDisplayBegin = 11;
      minorDisplayEnd = 13;
      majorDisplayBegin = 0;
      majorDisplayEnd = 10;
      majorTrigger = Calendar2.DATE;
      nthOptions = new int[] {1, 2, 3, 4, 6, 12};
    } else if (secondsRange > (atLeastNPieces - 2) * Calendar2.SECONDS_PER_MINUTE) {
      // minutes
      duration = Calendar2.MINUTE;
      durationNSec = Calendar2.SECONDS_PER_MINUTE;
      minorDisplayBegin = 14;
      minorDisplayEnd = 16;
      majorDisplayBegin = 0;
      majorDisplayEnd = 13;
      majorTrigger = Calendar2.HOUR_OF_DAY;
      nthOptions = new int[] {1, 2, 3, 4, 5, 10, 15, 30};
    } else {
      // seconds
      duration = Calendar2.SECOND;
      durationNSec = 1;
      minorDisplayBegin = 17;
      minorDisplayEnd = 19;
      majorDisplayBegin = 0;
      majorDisplayEnd = 16;
      majorTrigger = Calendar2.MINUTE;
      nthOptions = new int[] {1, 2, 3, 4, 5, 10, 15, 30};
    }

    // find biggest divisor to generate atLeastNPieces
    double dnDurations =
        secondsRange / durationNSec; // # of durations e.g., 5.1 ... 100(may be big if year)
    int nthOptionIndex = nthOptions.length - 1;
    while (nthOptionIndex > 0 && dnDurations / nthOptions[nthOptionIndex] < (atLeastNPieces - 1))
      nthOptionIndex--;
    int nth = nthOptions[nthOptionIndex];
    if (reallyVerbose)
      String2.log("  durationNSec=" + durationNSec + " dnDurations=" + dnDurations + " nth=" + nth);

    // generate starting gc
    GregorianCalendar gc = Calendar2.epochSecondsToGc(minSeconds);
    Calendar2.clearSmallerFields(gc, duration);
    int ti = gc.get(duration); // current Date if duration is date
    ti = ((ti - nthBase) / nth) * nth + nthBase; // integer division truncates
    gc.set(duration, ti);

    // generate the range and labels for the palette
    DoubleArray tRangeLow = new DoubleArray();
    DoubleArray tRangeHigh = new DoubleArray();
    StringArray tLeftLabel = new StringArray(); // tLastLabel set below
    boolean triggered =
        duration
            != Calendar2.YEAR; // YEAR never has major label; all other start with triggered=true
    int nPieces = 0;
    String dt;
    // String2.log("gc=" + Calendar2.gcToEpochSeconds(gc) + " maxSeconds=" + maxSeconds);
    while (Calendar2.gcToEpochSeconds(gc) < maxSeconds) {
      dt = Calendar2.formatAsISODateTimeT(gc); // 2011-12-15 Bob Simons changed Space to T.
      String tLabel =
          duration == Calendar2.MONTH
              ? Calendar2.getMonthName3(gc.get(Calendar2.MONTH) + 1)
              : // since java is 0..
              dt.substring(minorDisplayBegin, minorDisplayEnd);
      if (triggered) tLabel += "<br>" + dt.substring(majorDisplayBegin, majorDisplayEnd);
      // String2.log("  piece=" + String2.right(""+nPieces,2) + " dt=" + dt + " tLabel=" + tLabel);

      tLeftLabel.add(tLabel);
      tRangeLow.add(Calendar2.gcToEpochSeconds(gc) * (dataIsMillis ? 1000 : 1));

      // step forward nth durations
      int oTriggerValue = gc.get(majorTrigger);
      gc.add(duration, nth);
      int triggerValue = gc.get(majorTrigger);
      triggered = oTriggerValue != triggerValue;
      // special trigger test for duration=DATE since months have diff # days
      // and, e.g., if nth is 10, you want 31st to trigger to next month
      if (!triggered && duration == Calendar2.DATE) {
        GregorianCalendar tgc = (GregorianCalendar) gc.clone();
        tgc.add(majorTrigger, 1); // next month
        Calendar2.clearSmallerFields(tgc, majorTrigger); // beginning of next month
        // is time to next trigger relatively small?  then triggered=true
        if (Calendar2.gcToEpochSeconds(tgc) - Calendar2.gcToEpochSeconds(gc)
            <= nth * Calendar2.SECONDS_PER_DAY / 2) {
          triggered = true;
          gc.add(majorTrigger, 1); // clearSmallerFields done below
        }
      }
      if (triggered) {
        // duration=YEAR has intentionally goofy majorTrigger, so never triggered
        // clear smaller fields is important for duration=DATE; irrelevant for others
        Calendar2.clearSmallerFields(gc, majorTrigger); // sets day=1
      }
      tRangeHigh.add(Calendar2.gcToEpochSeconds(gc) * (dataIsMillis ? 1000 : 1));
      nPieces++;

      // safety valve
      if (nPieces > atLeastNPieces * 5) {
        String2.log(errorInMethod + "nPieces=" + nPieces + " is too big.");
        break;
      }
    }
    dt = Calendar2.formatAsISODateTimeT(gc); // 2011-12-15 Bob Simons changed Space to T.
    String tLastLabel =
        duration == Calendar2.MONTH
            ? Calendar2.getMonthName3(gc.get(Calendar2.MONTH) + 1)
            : // since java is 0..
            dt.substring(minorDisplayBegin, minorDisplayEnd);
    if (triggered) tLastLabel += "<br>" + dt.substring(majorDisplayBegin, majorDisplayEnd);
    if (reallyVerbose) String2.log("  lastPieceDt=" + dt + " tLastLabel=" + tLastLabel);

    Test.ensureTrue(
        Calendar2.gcToEpochSeconds(gc) >= maxSeconds,
        errorInMethod
            + "final gc="
            + Calendar2.formatAsISODateTimeT(gc)
            + " is less than maxSeconds="
            + Calendar2.epochSecondsToIsoStringTZ(maxSeconds)
            + ".");

    // make an integer palette (0..nPieces)
    //   public static String makeCPT(String baseDir, String palette, String scale, double minData,
    //       double maxData, int nSections, boolean continuous, String resultDir) throws Exception {
    if (reallyVerbose) String2.log("nPieces=" + nPieces);
    String cptFileName =
        makeCPT(baseDir, palette, "Linear", 0, nPieces, nPieces, continuous, resultDir);
    List<String> lines = File2.readLinesFromFile(cptFileName, File2.ISO_8859_1, 3);
    populate(this, cptFileName, lines);

    // put tRangeLow, tRangeHigh, tLeftLabel, tLastLabel into place
    rangeLow = tRangeLow.toArray();
    rangeHigh = tRangeHigh.toArray();
    leftLabel = tLeftLabel.toArray();
    lastLabel = tLastLabel;

    // finish up  (sets rangeMin, rangeMax, range1024)
    // this does lots of validity checking
    finishUpConstruction();
  }

  /** This finishes up the construction process by calculating derived values. */
  protected void finishUpConstruction() {
    int n = rangeLow.length;
    Test.ensureEqual(rangeHigh.length, n, "rangeHigh.length != rangeLow.length");
    Test.ensureEqual(rLow.length, n, "rLow.length != rangeLow.length");
    Test.ensureEqual(rHigh.length, n, "rHigh.length != rangeLow.length");
    Test.ensureEqual(gLow.length, n, "gLow.length != rangeLow.length");
    Test.ensureEqual(gHigh.length, n, "gHigh.length != rangeLow.length");
    Test.ensureEqual(bLow.length, n, "bLow.length != rangeLow.length");
    Test.ensureEqual(bHigh.length, n, "bHigh.length != rangeLow.length");
    Test.ensureEqual(leftLabel.length, n, "leftLabel.length != rangeLow.length");
    Test.ensureNotNull(lastLabel, "lastLabel is null");
    range1024 = new double[n];
    rRange = new int[n];
    gRange = new int[n];
    bRange = new int[n];
    color = new Color[n];
    continuous = false; // assume all r g b ranges are 0
    // use MAX_VALUE and -MAX_VALUE so any finite value will reset it
    rangeMin = Double.MAX_VALUE;
    rangeMax = -Double.MAX_VALUE; // not Double.MIN_VALUE which ~= 0

    for (int i = 0; i < n; i++) {
      rangeMin = Math.min(rangeMin, rangeLow[i]);
      rangeMax = Math.max(rangeMax, rangeHigh[i]);
      range1024[i] = (rangeHigh[i] - rangeLow[i]) / 1024;
      rRange[i] = rHigh[i] - rLow[i];
      gRange[i] = gHigh[i] - gLow[i];
      bRange[i] = bHigh[i] - bLow[i];
      // String2.log("" + i +
      //    " r:" + String2.right(""+rLow[i], 4) + String2.right(""+rHigh[i], 4) +
      // String2.right(""+rRange[i], 4) +
      //    " g:" + String2.right(""+gLow[i], 4) + String2.right(""+gHigh[i], 4) +
      // String2.right(""+gRange[i], 4) +
      //    " b:" + String2.right(""+bLow[i], 4) + String2.right(""+bHigh[i], 4) +
      // String2.right(""+bRange[i], 4));
      if (rRange[i] != 0 || gRange[i] != 0 || bRange[i] != 0) continuous = true;
      color[i] = new Color(rLow[i], gLow[i], bLow[i]); // used if !continuous
    }
    halfI = n / 2;
    halfStart = rangeLow[halfI];
  }

  /** This crudely implements equals. returns false */
  @Override
  public boolean equals(Object o) {
    return false;
  }

  /** This crudely implements hashCode. returns 1 */
  @Override
  public int hashCode() {
    return 1;
  }

  /**
   * This returns the range of values covered by this colorMap.
   *
   * @return the range
   */
  @Override
  public Range2D getRange() {
    return new Range2D(rangeMin, rangeMax);
  }

  /**
   * This finds the transform which encompasses inVal and determines the appropriate color.
   *
   * @param inVal the incoming value
   * @return the appropriate color. If range.start <= inVal < range.end, for one of the ranges, or
   *     range.start <= inVal <= range.end for the last range, that transform is used to determine
   *     the appropriate color. [Disabled: If inVal is NaN, this returns NaNColor. (BUT THIS IS
   *     NEVER USED. GridCartesianRenderer doesn't draw pixel if Double.isNaN. And since I don't do
   *     proper land mask, drawing NaN's would draw over the land. It is simpler and faster to just
   *     draw the backgound color of the graph and don't draw NaN data pixels.) ] If inVal <
   *     rangeMin, this returns backgroundColor. If inVal > rangeMax, this returns foregroundColor.
   *     If rangeMin <= inVal <= rangeMax, but inVal isn't covered by any piece, this returns
   *     NaNColor.
   */
  @Override
  public Color getColor(double inVal) {

    // deal with special cases quickly
    if (Double.isNaN(inVal)) return NaNColor; // SSR.makeCPT2 uses this
    if (inVal < rangeMin)
      return backgroundColor; // note that end points don't get back/foreground color
    if (inVal > rangeMax) return foregroundColor;

    // find the appropriate transform by testing what Range2D it's in
    // TESTING ON/OFF: don't delete cumulative system, since I sometimes uncomment for test()
    // long time = System.currentTimeMillis();
    int n = rangeLow.length;
    int lastPiece = n - 1;
    int foundPiece = -1;
    for (int i = (inVal >= halfStart ? halfI : 0); i < lastPiece; i++) {
      if (inVal >= rangeLow[i] && inVal < rangeHigh[i]) { // note < for all except last range
        foundPiece = i;
        break;
      }
    }
    // check last range with <= (different than above) at high end
    if (foundPiece == -1) {
      if (inVal >= rangeLow[lastPiece] && inVal <= rangeHigh[lastPiece]) {
        foundPiece = lastPiece;
      } else return NaNColor;
    }
    // TESTING ON/OFF: don't delete cumulative system, since I sometimes uncomment for test()
    // cumulativeLookupTime += System.currentTimeMillis() - time;

    /*
    //I tried binary search, but not faster
    int foundPiece = Arrays.binarySearch(rangeLow, inVal);
    if (foundPiece < 0) {
        foundPiece = -foundPiece - 2;
        //if it isn't in that range, return NaNColor
        if (inVal < rangeLow[foundPiece] || inVal > rangeHigh[foundPiece]) {
            Test.error("foundPiece not right");
            return NaNColor;
        }

    } //>=0 means exact match, no need to check rangeLow and High
    */

    if (continuous) {
      // the value is in range #piece
      // convert the value to be 0 - 1024/1024 of the range
      int val1024 =
          (int)
              Math.round(
                  (inVal - rangeLow[foundPiece])
                      / range1024[foundPiece]); // safe since rangeMin/Max checked above

      // generate the color
      Color tColor =
          new Color(
              rLow[foundPiece]
                  + ((val1024 * rRange[foundPiece]) >> 10), // >>10 same as /1024 since 1024 is 2^10
              gLow[foundPiece] + ((val1024 * gRange[foundPiece]) >> 10),
              bLow[foundPiece] + ((val1024 * bRange[foundPiece]) >> 10));
      // TESTING ON/OFF: don't delete cumulative system, since I sometimes uncomment for test()
      // cumulativeTotalTime += System.currentTimeMillis() - time;
      // cumulativeCount++;

      return tColor;
    } else {
      // !continuous,   use pre-made colors
      return color[foundPiece];
    }
  }

  /**
   * This specifies the color that will be returned by getColor(aValueLessThanAnyRange).
   *
   * @param color the color that will be returned by getColor(aValueLessThanAnyRange). The default
   *     is Color.BLACK.
   */
  public void setBackgroundColor(Color color) {
    backgroundColor = color;
  }

  /**
   * This specifies the color that will be returned by getColor(aValueGreaterThanAnyRange).
   *
   * @param color the color that will be returned by getColor(aValueGreaterThanAnyRange). The
   *     default is Color.WHITE.
   */
  public void setForegroundColor(Color color) {
    foregroundColor = color;
  }

  /**
   * This specifies the color that will be returned by getColor(Double.NaN).
   *
   * @param color the color that will be returned by getColor(Double.NaN). The default is
   *     Color.GRAY.
   */
  public void setNaNColor(Color color) {
    NaNColor = color;
  }

  /**
   * This returns the number of pieces currently held.
   *
   * @return nPieces
   */
  public int getNPieces() {
    return rangeLow.length;
  }

  /**
   * This makes a GMT-style .cpt Color Palette Table file (if it doesn't already exist). If it does
   * exist, the file is File2.touch()'ed.
   *
   * @param baseDir is the name of the base .cpt palette directory
   * @param palette the name of the base palette (e.g., Rainbow). So basePalette is baseDir +
   *     palette + .cpt.
   * @param scale is one of "Linear", "Log". If the scale value is not valid, Linear is used. The
   *     number of divisions for either scale is handled automatically. If the scale is Log and
   *     minData <= 0, minData is changed to be maxData/1000.
   * @param minData the data value corresponding to the low end of the palette
   * @param maxData the Data value corresponding to the high end of the palette. If minData=maxData,
   *     this creates a suitable new range, roughly centered on minData. If maxData is less than
   *     minData, they will be swapped. (I would allow it but GMT doesn't.)
   * @param nSections the number of major sections that the colorbar is divided into; use -1 to get
   *     the default. For log axes, the resulting nSections may not be exactly what you request.
   * @param continuous If true, the scale is a continuum of colors. If false, the scale has discrete
   *     colors.
   * @return complete name of the resulting .cpt palette file
   * @throws Exception if trouble (but it tries to deal with problems and keep going)
   */
  public static String makeCPT(
      String baseDir,
      String palette,
      String scale,
      double minData,
      double maxData,
      int nSections,
      boolean continuous,
      String resultDir)
      throws Exception {

    // validate minData and maxData
    if (!Double.isFinite(minData) || !Double.isFinite(maxData))
      throw new RuntimeException(
          String2.ERROR
              + " in CompoundColorMap.makeCPT: minData ("
              + minData
              + ") and/or maxData ("
              + maxData
              + ") is invalid.");
    if (maxData < minData) {
      double d = minData;
      minData = maxData;
      maxData = d;
    }
    if (Math2.almostEqual(9, minData, maxData)) {
      double dar[] = Math2.suggestLowHigh(minData, maxData);
      minData = dar[0];
      maxData = dar[1];
    }
    if (minData <= 0 && scale.equals("Log")) {
      if (maxData <= 0) scale = "Linear";
      else minData = maxData / 1000; // 3 decades
    }
    if (nSections <= 0 || nSections > 100) nSections = -1; // standardize it

    // create the file names
    String fullBaseCpt = baseDir + palette + ".cpt";
    String paletteID =
        palette
            + "_"
            + scale
            + "_"
            + String2.genEFormat6(minData)
            + "_"
            + String2.genEFormat6(maxData)
            + "_"
            + nSections
            + "_"
            + continuous;
    String fullResultCpt = String2.canonical(resultDir + paletteID + ".cpt");

    // thread-safe creation of the file
    // (If there are almost simultaneous requests for the same one, only one thread will make it.)
    ReentrantLock lock = String2.canonicalLock(fullResultCpt);
    if (!lock.tryLock(String2.longTimeoutSeconds, TimeUnit.SECONDS))
      throw new TimeoutException("Timeout waiting for lock on CompoundColorMap fullResultCpt.");
    try {

      // result file already exists?
      if (File2.touch(fullResultCpt)) {
        if (reallyVerbose) String2.log("CompoundColorMap.makeCPT is reusing\n  " + fullResultCpt);
        return fullResultCpt;
      }
      if (reallyVerbose)
        String2.log("CompoundColorMap.makeCPT is making a new\n  " + fullResultCpt);

      // make a list of desired levels; add the first level
      double range = maxData - minData;
      DoubleArray levels = new DoubleArray(16, false);
      levels.add(minData);

      // scale=Log
      if (scale.equals("Log")) {
        int minExponent = Math2.intExponent(minData);
        int maxExponent = Math2.intExponent(maxData) + 1;
        double toAdd[] = {
          1, 4, 2, 7, 1.4, 3, 5, 2.4, 6, 8, 1.2, 1.7, 9, 3.4, 2.2, 2.7, 1.1, 1.3, 1.5, 1.6, 4.4,
          1.8, 1.9, 3.2, 3.7, 2.1, 2.3, 2.5, 2.6, 4.2, 4.7, 5.4, 2.8, 2.9
        };
        if (nSections == -1) nSections = 8;

        // add toAdd*EveryDecade
        for (int which = 0; which < toAdd.length; which++) {
          // String2.log("makeCPT which=" + which + " levels.size()=" + levels.size() + " " +
          // levels.toString());
          if (levels.size() < nSections) { // if fewer than 8 sections, poor sampling of colors
            for (int i = minExponent; i <= maxExponent; i++) {
              double d = toAdd[which] * Math2.ten(i);
              if (d > minData
                  && d < maxData
                  && !Math2.almostEqual(9, d, minData)
                  && !Math2.almostEqual(9, d, maxData)) {
                levels.add(d);
              }
            }
          }
        }
        if (reallyVerbose) String2.log("CompoundColorMap.makeCPT levels=" + levels.toString());

        // sort the values
        levels.sort();

        // default: scale="Linear"
      } else {
        if (nSections == -1) {
          // figure out how many sections are best;  need at least 6 to get good sampling of colors
          // e.g. 0..10 in 10 sections is 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
          // range=18, better 9 sections than 6)
          // look for 11 and 13 (primes), too
          if (Math.abs(range) == 360) {
            nSections = 8; // 0, 45, 90, ...
          } else {
            nSections = 10; // default
            int sectionOptions[] = {9, 8, 7, 12, 13, 11, 10, 6};
            for (int i = 0; i < sectionOptions.length; i++) {
              // is the interval just one digit?
              // String2.log("nSectionOptions=" + nSectionOptions + " range=" + range + " mantissa="
              // + Math2.mantissa(range / nSectionOptions));
              double interval =
                  Math2.mantissa(
                      Math.abs(range) / sectionOptions[i]); // e.g., 7.0000001 or 6.99999999
              if (Math2.almostEqual(9, interval, Math.round(interval))) {
                nSections = sectionOptions[i];
                break;
              }
            }
          }
        }

        // generate the levels
        for (int i = 1; i < nSections; i++) levels.add(minData + i * range / nSections);
      }

      /*//NO LONGER AN OPTION: scale=JumpAtEnd  0 1 2 3 4 5 10
      } else if (scale.equals("JumpAtEnd")) {
          for (int i = 1; i <= 5; i++)
              levels.add(minData + i * range / 10);

      //NO LONGER AN OPTION: scale=JumpAtBothEnds 0 3 4 5 6 7 10
      } else if (scale.equals("JumpAtBothEnds")) {
          for (int i = 3; i <= 7; i++)
              levels.add(minData +  i * range / 10);
      */

      // add the last level
      levels.add(maxData);
      int nLevels = levels.size();

      // make the cpt file
      // each line: low r g b high r g b
      // the basecpt's are special: the low end of the pieces are 0, 1, 2, ..., nPieces-1
      //  so range covered is 0..nPieces
      CompoundColorMap colorMap = new CompoundColorMap(fullBaseCpt);
      double nPieces = colorMap.getNPieces(); // double so double arithmetic used below
      StringBuilder results = new StringBuilder();
      Color backColor = null;
      Color foreColor = null;
      for (int i = 1; i < nLevels; i++) {
        Color lowColor, highColor;
        if (continuous) {
          // 0.99999 gets color right before, in case colors sections are discontinuous (e.g.,
          // Topography.cpt)
          lowColor = colorMap.getColor((i - 1) * nPieces / (nLevels - 1));
          highColor = colorMap.getColor(0.99999 * i * nPieces / (nLevels - 1));
        } else {
          // get color from center of its range
          lowColor = colorMap.getColor((i - 0.5) * nPieces / (nLevels - 1));
          highColor = lowColor;
        }
        results.append(
            levels.get(i - 1)
                + "\t"
                + lowColor.getRed()
                + "\t"
                + lowColor.getGreen()
                + "\t"
                + lowColor.getBlue()
                + "\t"
                + levels.get(i)
                + "\t"
                + highColor.getRed()
                + "\t"
                + highColor.getGreen()
                + "\t"
                + highColor.getBlue()
                + "\n");
        if (i == 1) backColor = lowColor;
        if (i == nLevels - 1) foreColor = highColor;
      }

      // background color
      results.append(
          "B\t"
              + backColor.getRed()
              + "\t"
              + backColor.getGreen()
              + "\t"
              + backColor.getBlue()
              + "\n");

      // foreground color
      results.append(
          "F\t"
              + foreColor.getRed()
              + "\t"
              + foreColor.getGreen()
              + "\t"
              + foreColor.getBlue()
              + "\n");

      // NaN color
      // note that SGT doesn't use colorMap for NaN's, so this usually isn't used
      Color NaNColor = colorMap.getColor(Double.NaN);
      results.append(
          "N\t"
              + NaNColor.getRed()
              + "\t"
              + NaNColor.getGreen()
              + "\t"
              + NaNColor.getBlue()
              + "\n");

      // write the file
      if (reallyVerbose)
        String2.log(
            "colorMap  "
                + palette
                + " "
                + scale
                + " "
                + minData
                + " "
                + maxData
                + " continuous="
                + continuous
                + "\n"
                + results);
      int randomInt = Math2.random(Integer.MAX_VALUE);
      String error = File2.writeToFile88591(fullResultCpt + randomInt, results.toString());
      if (error.length() > 0)
        throw new RuntimeException(String2.ERROR + " in CompoundColorMap.makeCPT:\n" + error);

      File2.renameIfNewDoesntExist(fullResultCpt + randomInt, fullResultCpt);

      return fullResultCpt;
    } finally {
      lock.unlock();
    }
  }

  /** This resets the stats system. */
  public void resetStats() {
    cumulativeLookupTime = -1;
    cumulativeTotalTime = -1;
    cumulativeCount = 0;
  }

  /**
   * This returns a string with informataion about the speed of the getColor calls. This doesn't
   * call resetStats().
   *
   * @return a string with informataion about the speed of the getColor calls.
   */
  public String getStats() {
    if (cumulativeLookupTime == -1 && cumulativeTotalTime == -1 && cumulativeCount == 0)
      return "  compundColorMap statistics not gathered (often significant time)";
    else
      return "  compoundColorMap.cumulativeLookupTime="
          + cumulativeLookupTime
          + " (often significant, -1=not measured)\n"
          + "  compoundColorMap.cumulativeTotalTime="
          + cumulativeTotalTime
          + " (often significant, -1=not measured)\n"
          + "  sqrt(compoundColorMap.cumulativeCount)="
          + Math2.roundToInt(Math.sqrt(cumulativeCount))
          + " (0=not measured)";
  }

  @Override
  public String toString() {
    return "CompoundColorMap min=" + rangeMin + " max=" + rangeMax;
  }

  /**
   * THIS ISN'T USED BECAUSE THE ESTIMATED TIMES WEREN'T ACCURATE. This returns the estimated time
   * needed to call getColor n times.
   *
   * @param n the number of times getColor will be called
   * @return an estimate fo the time needed to call getColor n times.
   */
  //    public static String getTimeEstimate(int n) {
  //        return "CompoundColorMap estimated cumLookupTime=" +
  //            (170 * n / 1000000) +  //170 from test() above
  //            " ms, cumTotalTime=" + (474 * n / 1000000) + " ms"; //474 from test() above
  //    }
}
