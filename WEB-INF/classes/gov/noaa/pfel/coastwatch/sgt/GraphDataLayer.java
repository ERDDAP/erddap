/*
 * GraphDataLayer Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.sgt;

import gov.noaa.pfel.coastwatch.griddata.Grid;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pmel.sgt.ColorMap;
import java.awt.Color;

/**
 * This class holds the information for a data layer on a graph (not SGT's use of "layer" which what
 * a graph is drawn on).
 */
public class GraphDataLayer {

  /**
   * For drawing markers (v1=x, v2=y). If colorMap (CompoundColorMap) is provided, v3 is used for
   * data for varying the marker's fill color. Available for SgtMap and SgtGraph. (!!!!not yet: done
   * everywhere)
   */
  public static final int DRAW_MARKERS = 0;

  /**
   * For drawing markers and a polyline based on (v1=x, v2=y). Currently, only available for
   * SgtGraph.
   */
  public static final int DRAW_LINES = 1;

  /**
   * For drawing markers and a polyline based on (v1=x, v2=y). Uses lineColor. Currently, only
   * available for SgtGraph.
   */
  public static final int DRAW_MARKERS_AND_LINES = 2;

  /**
   * For drawing vector time series (v1=x, v2=u, v3=v) Uses lineColor. Only available for SgtGraph.
   */
  public static final int DRAW_STICKS = 3; // vector-like sticks

  /**
   * For drawing vectors based on 2 grid files. v1..v4 are ignored. Uses ncFileName for the u data
   * and ncFileName2 for the v data. Use colorMap (CompoundColorMap), or set colorMap to null and
   * use lineColor. Uses vectorStandard for the length which should generate a standard vector
   * (e.g., .1 for .1 m/s) Only available for SgtMap.
   */
  public static final int DRAW_GRID_VECTORS = 4;

  /**
   * For drawing vectors based on a table with v1=x,v2=y,v3=u,v4=v columns. Use colorMap
   * (CompoundColorMap), or set colorMap to null and use lineColor. Uses markerSize for size which
   * should generate a standard vector (e.g., .1 for .1 m/s) Only available for SgtMap.
   */
  public static final int DRAW_POINT_VECTORS = 5;

  /**
   * For drawing a colored surface based on the data in a Grid (where grid.lon is treated as x, and
   * grid.lat is treated as y). Use colorMap (CompoundColorMap). Originally only available for
   * SgtMap. 2015-03-31 SgtGraph, too.
   */
  public static final int DRAW_COLORED_SURFACE = 6;

  /**
   * For drawing a contour lines based on the data in a Grid (where grid.lon is treated as x, and
   * grid.lat is treated as y). Use colorMap (CompoundColorMap) to specify drawLinesAt and lineColor
   * to specify the line color. Only available for SgtMap.
   */
  public static final int DRAW_CONTOUR_LINES = 7;

  /**
   * For drawing a colored surface based on the data in a Grid (where grid.lon is treated as x, and
   * grid.lat is treated as y). Use colorMap (CompoundColorMap) for the colored surface and
   * lineColor for the lines. Only available for SgtMap.
   */
  public static final int DRAW_COLORED_SURFACE_AND_CONTOUR_LINES = 8;

  /** The names of the DRAW options (currently just for diagnostic purposes). */
  public static final String DRAW_NAMES[] = {
    "Markers",
    "Lines",
    "Markers and Lines",
    "Sticks",
    "Grid Vectors",
    "Point Vectors",
    "Colored Surface",
    "Contour Lines",
    "Colored Surface and Contour Lines"
  };

  public static final int MARKER_TYPE_NONE = 0;
  public static final int MARKER_TYPE_PLUS = 1;
  public static final int MARKER_TYPE_X = 2;
  public static final int MARKER_TYPE_DOT = 3;
  public static final int MARKER_TYPE_SQUARE = 4;
  public static final int MARKER_TYPE_FILLED_SQUARE = 5;
  public static final int MARKER_TYPE_CIRCLE = 6;
  public static final int MARKER_TYPE_FILLED_CIRCLE = 7;
  public static final int MARKER_TYPE_UP_TRIANGLE = 8;
  public static final int MARKER_TYPE_FILLED_UP_TRIANGLE = 9;
  public static final int MARKER_TYPE_BORDERLESS_FILLED_SQUARE = 10;
  public static final int MARKER_TYPE_BORDERLESS_FILLED_CIRCLE = 11;
  public static final int MARKER_TYPE_BORDERLESS_FILLED_UP_TRIANGLE = 12;

  /**
   * These exactly parallel the MARKER_TYPEs. Some code elsewhere relies on filled options having
   * s.toLowerCase().indexOf("filled") >= 0.
   */
  public static final String MARKER_TYPES[] = {
    "None",
    "Plus",
    "X",
    "Dot",
    "Square",
    "Filled Square",
    "Circle",
    "Filled Circle",
    "Up Triangle",
    "Filled Up Triangle",
    "Borderless Filled Square",
    "Borderless Filled Circle",
    "Borderless Filled Up Triangle"
  };

  /**
   * These don't parallel the MARKER_TYPEs, but the names are the same as in MARKER_TYPES, so you
   * can look them up.
   */
  public static final String FILLED_MARKER_TYPES[] = {
    "Filled Square", "Filled Circle", "Filled Up Triangle",
    "Borderless Filled Square", "Borderless Filled Circle", "Borderless Filled Up Triangle"
  };

  public static final int REGRESS_NONE = -1;
  public static final int REGRESS_MEAN = 0;

  /** The v1 column number. */
  // was x
  public int v1;

  /** The v2 column number. */
  // was y
  public int v2;

  /** The v3 column number. Set to -1 for safety if not needed. */
  // was data
  public int v3;

  /** The v4 column number. Set to -1 for safety if not needed. */
  // was id
  public int v4;

  /** The v5 column number. Set to -1 for safety if not needed. */
  // was id
  public int v5;

  /** How the data should be drawn. See the DRAW_xxx options, e.e., DRAW_FILLED_MARKER. */
  public int draw;

  /**
   * Indicates if the x axis is a date/time axis (and the x data values are seconds since
   * 1970-01-01).
   */
  public boolean xIsTimeAxis;

  /**
   * Indicates if the y axis is a date/time axis (and the y data values are seconds since
   * 1970-01-01).
   */
  public boolean yIsTimeAxis;

  /**
   * The sourceID identifies the source of this (0..n-1, or -1 for pointVectorScreen, or -2 for
   * vectorScreen). This is just used for tables of returned information (e.g., where points were
   * plotted).
   */
  public int sourceID;

  /**
   * The x axis title (ignored if null or if x axis isTimeAxis). This is only used by SgtGraph and
   * only if the graph's x title is generated automatically (from the first GraphDataLayer.
   */
  public String xAxisTitle;

  /** For y axis title. Ignored if null. */
  public String yAxisTitle;

  /** For the legend. If null, nothing in legend for this GraphDataLayer. */
  public String boldTitle;

  /** For the legend. Ignored if null. */
  public String title2;

  /** For the legend. Ignored if null. */
  public String title3;

  /** For the legend. Ignored if null. */
  public String title4;

  /** The table of data to be plotted (or null). */
  public Table table;

  /** The grid data to be plotted (or null). */
  public Grid grid1;

  /** The other grid data to be plotted (e.g., the yVector grid) (or null). */
  public Grid grid2;

  /**
   * An optional ColorMap (used for DRAW_MARKERS, DRAW_POINT_VECTORS, DRAW_GRID_VECTORS). Set to
   * null if not needed. See CompoundColorMap and MonoColorMap.
   */
  public ColorMap colorMap = null;

  /** lineColor for the lines between points and/or the color of the edges of the markers. */
  public Color lineColor;

  /** markerType: e.g., MARKER_TYPE_SQUARE */
  public int markerType = MARKER_TYPE_SQUARE;

  /**
   * markerSize in pixels (Graph2D units), but will be modified by fontScale (or whatever the
   * variable name that modifies the basic font size). Ignored if not relevant.
   */
  public int markerSize;

  public static final int MARKER_SIZE_XSMALL = 3;
  public static final int MARKER_SIZE_SMALL = 5;
  public static final int MARKER_SIZE_MEDIUM = 7;
  public static final int MARKER_SIZE_LARGE = 9;
  public static final int MARKER_SIZE_XLARGE = 11;

  /**
   * VectorStandard is the length which should generate a standard vector (e.g., .1 for .1 m/s).
   * Ignored if not relevant, but use 0 or NaN to be safe.
   */
  public double vectorStandard;

  /**
   * regressionType indicates if a regression line should be calculated and displayed in addition to
   * the data. See the REGRESS_xxx options.
   */
  public int regressionType;

  /** The constructor. */
  public GraphDataLayer(
      int sourceID,
      int v1,
      int v2,
      int v3,
      int v4,
      int v5,
      int draw,
      boolean xIsTimeAxis,
      boolean yIsTimeAxis,
      String xAxisTitle,
      String yAxisTitle,
      String boldTitle,
      String title2,
      String title3,
      String title4,
      Table table,
      Grid grid1,
      Grid grid2,
      ColorMap colorMap,
      Color lineColor,
      int markerType,
      int markerSize,
      double vectorStandard,
      int regressionType) {

    this.sourceID = sourceID;
    this.v1 = v1;
    this.v2 = v2;
    this.v3 = v3;
    this.v4 = v4;
    this.v5 = v5;
    this.draw = draw;
    this.xIsTimeAxis = xIsTimeAxis;
    this.yIsTimeAxis = yIsTimeAxis;
    this.xAxisTitle = xAxisTitle;
    this.yAxisTitle = yAxisTitle;
    this.boldTitle = boldTitle;
    this.title2 = title2;
    this.title3 = title3;
    this.title4 = title4;
    this.table = table;
    this.grid1 = grid1;
    this.grid2 = grid2;
    this.colorMap = colorMap;
    this.lineColor = lineColor;
    this.markerType = markerType;
    this.markerSize = markerSize;
    this.vectorStandard = vectorStandard;
    this.regressionType = regressionType;
  }

  /**
   * Generates a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return "GraphDataLayer "
        + " sourceID="
        + sourceID
        + "\n    v1="
        + v1
        + " v2="
        + v2
        + " v3="
        + v3
        + " v4="
        + v4
        + " draw="
        + (draw < 0 || draw >= DRAW_NAMES.length ? "" + draw : DRAW_NAMES[draw])
        + " xIsTimeAxis="
        + xIsTimeAxis
        + " yIsTimeAxis="
        + yIsTimeAxis
        + "\n    xAxisTitle="
        + xAxisTitle
        + " yAxisTitle="
        + yAxisTitle
        + "\n    boldTitle="
        + boldTitle
        + "\n    title2="
        + title2
        + "\n    title3="
        + title3
        + "\n    title4="
        + title4
        + "\n    cColorMap="
        + colorMap
        + " lineColor="
        + (lineColor == null ? "null" : "0x" + Integer.toHexString(lineColor.getRGB()))
        + " markerType="
        + markerType
        + "\n    markerSize="
        + markerSize
        + " vectorStandard="
        + vectorStandard
        + " regressionType="
        + regressionType;
  }

  /**
   * This returns the number of legend line equivalents which are needed for this GraphDataLayer in
   * the legend (symbol, text, and gap).
   *
   * @param maxCharsPerLine
   * @return the number of legend line equivalents which are needed for this GraphDataLayer in the
   *     legend.
   */
  public int legendLineCount(int maxCharsPerLine) {

    // no entry in legend
    if (boldTitle == null) return 0;

    int count = 1; // gap at end
    count +=
        SgtUtil.makeShortLines(SgtUtil.maxBoldCharsPerLine(maxCharsPerLine), boldTitle, null, null)
            .size();
    count += SgtUtil.makeShortLines(maxCharsPerLine, title2, title3, title4).size();

    // needs a color bar
    if (colorMap != null
        && (draw == DRAW_MARKERS
            || draw == DRAW_MARKERS_AND_LINES
            || draw == DRAW_POINT_VECTORS
            || draw == DRAW_GRID_VECTORS
            || draw == DRAW_COLORED_SURFACE)) count += 4;

    // String2.log("GraphDataLayer.legendLineCount=" + count);
    return count;
  }
}
