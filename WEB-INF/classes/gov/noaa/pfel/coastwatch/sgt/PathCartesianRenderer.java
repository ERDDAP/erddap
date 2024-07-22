/*
 * PathCartesianRenderer Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.util.MustBe;
import com.cohort.util.String2;
import gov.noaa.pmel.sgt.Attribute;
import gov.noaa.pmel.sgt.CartesianGraph;
import gov.noaa.pmel.sgt.CartesianRenderer;
import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.dm.SGTData;
import gov.noaa.pmel.util.*;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.beans.PropertyChangeEvent;

/** This class lets you draw a Java path on a Cartesian graph. */
public class PathCartesianRenderer extends CartesianRenderer {

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

  // things set by constructor
  private CartesianGraph graph;
  private GeneralPath path;
  private double scale;
  private Color fillColor;
  private Color strokeColor;

  /** Bob Simons added this to avoid memory leak problems. */
  @Override
  public void releaseResources() throws Exception {
    try {
      graph = null;
      path = null;
      if (JPane.debug) String2.log("sgt.SGLabel.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  /**
   * The constructor.
   *
   * @param graph
   * @param path the path with lon (0..360 or +/-180, or 0..360e6 or +/-180e6, already as needed for
   *     this map) and lat values
   * @param scale the number that the path values need to be multiplied by to convert to degrees
   *     (e.g., 1 if already degrees, or 1e-6 if stored as integers: millions of a degrees)
   * @param fillColor If fillColor == null or fillColor.getAlpha is 0, the path won't be filled.
   * @param strokeColor If strokecolor == null or strokeColor.getAlpha is 0, the path won't be
   *     stroked. For now, if stroked, it will be a 1 pixel wide line.
   */
  public PathCartesianRenderer(
      CartesianGraph graph, GeneralPath path, double scale, Color fillColor, Color strokeColor) {
    this.graph = graph;
    this.path = path;
    this.scale = scale;
    this.fillColor = fillColor;
    this.strokeColor = strokeColor;
  }

  /**
   * This draws the filled area on the map. Required to extend CartesianRenderer.
   *
   * @param g
   */
  @Override
  public void draw(java.awt.Graphics g) {
    Graphics2D g2 = (Graphics2D) g;
    long time = System.currentTimeMillis();

    // modify the transform  so degrees is converted to device coordinates
    gov.noaa.pmel.sgt.LinearTransform xt =
        (gov.noaa.pmel.sgt.LinearTransform) graph.getXTransform();
    gov.noaa.pmel.sgt.LinearTransform yt =
        (gov.noaa.pmel.sgt.LinearTransform) graph.getYTransform();
    Range2D xUserRange = xt.getRangeU();
    Range2D yUserRange = yt.getRangeU();
    int leftX = graph.getXUtoD(xUserRange.start);
    int rightX = graph.getXUtoD(xUserRange.end);
    int lowerY = graph.getYUtoD(yUserRange.start);
    int upperY = graph.getYUtoD(yUserRange.end);
    double xDegPerPixel = ((xUserRange.end - xUserRange.start) / scale) / (rightX - leftX + 1);
    double yDegPerPixel = ((yUserRange.end - yUserRange.start) / scale) / (upperY - lowerY - 1);
    if (reallyVerbose)
      String2.log(
          "  PathCartesianRenderer  scale="
              + scale
              + "\n    xDegPerPixel="
              + xDegPerPixel
              + " yDegPerPixel="
              + yDegPerPixel
              + "\n    xUserRange.start="
              + xUserRange.start
              + " xUserRange.end="
              + xUserRange.end
              + "  leftX="
              + leftX
              + " rightX="
              + rightX
              + "\n    yUserRange.start="
              + yUserRange.start
              + " yUserRange.end="
              + yUserRange.end
              + "  lowerY="
              + lowerY
              + " upperY="
              + upperY);

    // set clip
    Shape originalClip = g2.getClip();
    g2.clipRect(leftX, upperY, rightX - leftX, lowerY - upperY);

    // going from deg to pixels, do the opposite order of transforms
    // use affineTransform and createTransformedShape
    //   otherwise, setting line width impossible if x & y scaled differently
    //   I thought: might be slower, but it is really fast.
    AffineTransform transform = new AffineTransform();
    transform.translate(leftX, upperY); // push graphUL into place
    transform.scale(1 / xDegPerPixel, 1 / yDegPerPixel); // scale deg -> pixels   (y is flipped)
    transform.translate(
        -xUserRange.start / scale, -yUserRange.end / scale); // treat graphUL degrees as origin
    Shape shape = path.createTransformedShape(transform);

    // draw the path
    if (fillColor != null && fillColor.getAlpha() != 0) {
      g2.setColor(fillColor);
      g2.fill(shape);
    }

    if (strokeColor != null && strokeColor.getAlpha() != 0) {
      g2.setStroke(new BasicStroke());
      g2.setColor(strokeColor);
      g2.draw(shape);
    }

    // restore the original clip
    g2.setClip(originalClip);
    if (reallyVerbose)
      String2.log(
          "  PathCartesianRenderer time="
              + (System.currentTimeMillis() - time)
              + "ms"); // usually 1 - 6 ms
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
    //        String2.log("GridCartesianRenderer: " + evt);
    //        String2.log("                       " + evt.getPropertyName());
    //      }
    modified(
        "GridCartesianRenderer: propertyChange("
            + evt.getSource().toString()
            + "["
            + evt.getPropertyName()
            + "]"
            + ")");
  }
}
