/*
 * FilledMarkerRenderer Copyright 2006, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.DoubleObject;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import gov.noaa.pmel.sgt.Attribute;
import gov.noaa.pmel.sgt.CartesianGraph;
import gov.noaa.pmel.sgt.CartesianRenderer;
import gov.noaa.pmel.sgt.ColorMap;
import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.LogTransform;
import gov.noaa.pmel.sgt.dm.SGTData;
import gov.noaa.pmel.util.Range2D;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;

/** This class lets you draw filled markers on a Cartesian graph. */
public class FilledMarkerRenderer extends CartesianRenderer {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  public static boolean reallyVerbose = false;

  public static final int TYPE_SQUARE = 0;

  // things set by constructor
  private CartesianGraph graph;
  private boolean xIsLogAxis, yIsLogAxis;
  private PrimitiveArray xPA, yPA, dataPA, idPA;
  private int markerType;
  private ColorMap colorMap;
  private Color lineColor;
  private int markerSize;
  private boolean drawLine;

  /** resultXxx set by draw() to be used for constructing user maps on the .gif */
  public int sourceID;

  public IntArray resultMinX, resultMaxX, resultMinY, resultMaxY, resultRowNumber;

  /** Bob Simons added this to avoid memory leak problems. */
  @Override
  public void releaseResources() throws Exception {
    try {
      graph = null;
      xPA = null;
      yPA = null;
      dataPA = null;
      idPA = null;
      colorMap = null;
      resultMinX = null;
      resultMaxX = null;
      resultMinY = null;
      resultMaxY = null;
      resultRowNumber = null;
      if (JPane.debug) String2.log("sgt.FilledMarkerRenderer.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  /**
   * The constructor.
   *
   * @param graph
   * @param sourceID indicates which, e.g., pointScreen led to this (not used, but held)
   * @param xPA the longitude values
   * @param yPA the latitude values
   * @param dataPA the data values
   * @param idPA the id's used for making user maps on the .gif (usually Strings)
   * @param colorMap used to translate the data values into the fill colors. If null, the line color
   *     is used.
   * @param lineColor The line color for the edge of the marker
   * @param markerType see GraphDataLayer MARKER_TYPE
   * @param markerSize The size of the marker in pixels (Graphics2D units).
   * @param drawLine true if you want a line between adjacent valid points
   */
  public FilledMarkerRenderer(
      CartesianGraph graph,
      int sourceID,
      PrimitiveArray xPA,
      PrimitiveArray yPA,
      PrimitiveArray dataPA,
      PrimitiveArray idPA,
      ColorMap colorMap,
      Color lineColor,
      int markerType,
      int markerSize,
      boolean drawLine) {

    this.graph = graph;
    xIsLogAxis = graph.getXTransform() instanceof LogTransform;
    yIsLogAxis = graph.getYTransform() instanceof LogTransform;
    this.sourceID = sourceID;
    this.xPA = xPA;
    this.yPA = yPA;
    this.dataPA = dataPA;
    this.idPA = idPA;
    this.colorMap = colorMap;
    this.lineColor = lineColor;
    this.markerType = markerType;
    this.markerSize = markerSize;
    this.drawLine = drawLine;
  }

  /**
   * This draws the filled area on the map. Required to extend CartesianRenderer.
   *
   * @param g
   */
  @Override
  public void draw(java.awt.Graphics g) {
    long time = System.currentTimeMillis();
    Graphics2D g2 = (Graphics2D) g;
    resultMinX = new IntArray();
    resultMaxX = new IntArray();
    resultMinY = new IntArray();
    resultMaxY = new IntArray();
    resultRowNumber = new IntArray();

    // store a copy of the transform
    AffineTransform originalTransform = g2.getTransform();
    Shape originalClip = g2.getClip();

    // modify the transform  so degrees is converted to device coordinates
    gov.noaa.pmel.sgt.AxisTransform xt = (gov.noaa.pmel.sgt.AxisTransform) graph.getXTransform();
    gov.noaa.pmel.sgt.AxisTransform yt = (gov.noaa.pmel.sgt.AxisTransform) graph.getYTransform();
    Range2D xUserRange = xt.getRangeU();
    Range2D yUserRange = yt.getRangeU();
    int leftX = graph.getXUtoD(xUserRange.start);
    int rightX = graph.getXUtoD(xUserRange.end);
    int lowerY = graph.getYUtoD(yUserRange.start);
    int upperY = graph.getYUtoD(yUserRange.end);
    double xDegPerPixel = (xUserRange.end - xUserRange.start) / (rightX - leftX);
    double yDegPerPixel = (yUserRange.end - yUserRange.start) / (upperY - lowerY);
    // String2.log("xDegPerPixel=" + xDegPerPixel + " yDegPerPixel=" + yDegPerPixel +
    //    " xUserRange.start=" + xUserRange.start + " xUserRange.end=" + xUserRange.end);

    CartesianProjection cartesianProjection =
        new CartesianProjection(
            xUserRange.start,
            xUserRange.end,
            yUserRange.start,
            yUserRange.end,
            leftX,
            rightX,
            lowerY,
            upperY,
            xIsLogAxis,
            yIsLogAxis);

    // change clip before changing transform
    g2.clipRect(leftX, upperY, rightX - leftX, lowerY - upperY);

    int markerSize2 = (markerSize + 1) / 2;
    int nRows = xPA.size();
    if (verbose)
      String2.log(
          "  FilledMarkerRenderer nRows="
              + nRows
              + "\n  colorMap="
              + colorMap
              + " lineColor=0x"
              + Integer.toHexString(lineColor.getRGB()));
    int oPixelX, oPixelY, pixelX = Integer.MAX_VALUE, pixelY = Integer.MAX_VALUE;
    DoubleObject dox = new DoubleObject(0);
    DoubleObject doy = new DoubleObject(0);
    IntArray markerXs = new IntArray();
    IntArray markerYs = new IntArray();
    ArrayList<Color> markerInteriorColors = new ArrayList();
    g2.setColor(lineColor); // important in case never needs to be set below
    for (int row = 0; row < nRows; row++) {
      double x = xPA.getDouble(row);
      if (Double.isNaN(x) || x < xUserRange.start || x > xUserRange.end) {
        oPixelX = Integer.MAX_VALUE;
        continue;
      }
      double y = yPA.getDouble(row);
      if (Double.isNaN(y) || y < yUserRange.start || y > yUserRange.end) {
        oPixelX = Integer.MAX_VALUE; // yes, x,  it signals trouble
        continue;
      }

      double data = dataPA.getDouble(row); // may be NaN
      // if (verbose) String2.log("FilledMarkerRenderer row=" + row +
      //    " x=" + xPA.getDouble(row) + " y=" + yPA.getDouble(row) +
      //    " data=" + dataPA.getDouble(row) + " fillColor=0x" +
      //    Integer.toHexString(fillColor.getRGB()));

      oPixelX = pixelX;
      oPixelY = pixelY;

      // pixelX = graph.getXUtoD(x);
      // pixelY = graph.getYUtoD(y);
      cartesianProjection.graphToDevice(x, y, dox, doy);
      pixelX = (int) dox.d; // safe since x checked above
      pixelY = (int) doy.d; // safe since y checked above

      // String2.log("FilledMarkerRenderer x=" + pixelX + " y=" + pixelY + " size=" + markerSize);

      // draw the connecting line?  color is already lineColor
      if (drawLine
          && oPixelX != Integer.MAX_VALUE
          && // no check for oPixelY; x signals trouble
          (oPixelX != pixelX || oPixelY != pixelY)) g2.drawLine(oPixelX, oPixelY, pixelX, pixelY);

      markerXs.add(pixelX);
      markerYs.add(pixelY);
      markerInteriorColors.add(colorMap == null ? lineColor : colorMap.getColor(data));

      // store location
      resultMinX.add(pixelX - markerSize2);
      resultMinY.add(pixelY - markerSize2);
      resultMaxX.add(pixelX + markerSize2);
      resultMaxY.add(pixelY + markerSize2);
      resultRowNumber.add(row);
    }

    // draw markers on top of lines
    int nMarkerXs = markerXs.size();
    if (nMarkerXs > 0) {
      long markerTime = System.currentTimeMillis();
      int nMarkerXs1 = nMarkerXs - 1;
      int xarray[] = markerXs.array;
      int yarray[] = markerYs.array;
      // int count = 1;
      for (int i = 0; i < nMarkerXs1; i++) {
        // simple optimization:
        // if next marker at same x,y, skip this one
        // (since other params constant or irrelevant)
        if (xarray[i] != xarray[i + 1] || yarray[i] != yarray[i + 1]) {
          // count++;
          SgtGraph.drawMarker(
              g2,
              markerType,
              markerSize,
              xarray[i],
              yarray[i],
              markerInteriorColors.get(i),
              lineColor);
        }
      }
      // always draw last marker
      SgtGraph.drawMarker(
          g2,
          markerType,
          markerSize,
          xarray[nMarkerXs1],
          yarray[nMarkerXs1],
          markerInteriorColors.get(nMarkerXs1),
          lineColor);
      if (reallyVerbose)
        String2.log("  draw markers time=" + (System.currentTimeMillis() - markerTime) + "ms");
    }

    // restore the original transform
    //        g2.setTransform(originalTransform);
    g2.setClip(originalClip);

    String2.log(
        "  FilledMarkerRenderer.draw done. time=" + (System.currentTimeMillis() - time) + "ms");
  }

  /**
   * NOT IMPLEMENTED. Get the Attribute associated with the renderer. Required to extend
   * CartesianRenderer.
   */
  @Override
  public Attribute getAttribute() {
    return null;
  }

  /** Get the CartesianGraph associated with the renderer. Required to extend CartesianRenderer. */
  @Override
  public CartesianGraph getCartesianGraph() {
    return graph;
  }

  // Find data object.
  // SGTData getDataAt(int x, int y)

  /** NOT IMPLEMENTED. Find data object. Required to extend CartesianRenderer. */
  @Override
  public SGTData getDataAt(java.awt.Point pt) {
    return null;
  }

  // Get parent pane.
  // AbstractPane getPane()

  // Factory method to create a new Renderer instance given the SGTData object and Attribute.
  // static CartesianRenderer	getRenderer(CartesianGraph cg, SGTData dmo, Attribute attr)

  // For internal sgt use.
  // void modified(java.lang.String mess)

  /* as implemented by GridCartesianRenderer */
  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    //      if(Debug.EVENT) {
    //        String2.log("FilledMarkerRenderer: " + evt);
    //        String2.log("                       " + evt.getPropertyName());
    //      }
    modified(
        "FilledMarkerRenderer: propertyChange("
            + evt.getSource().toString()
            + "["
            + evt.getPropertyName()
            + "]"
            + ")");
  }
}
