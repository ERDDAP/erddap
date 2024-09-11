/*
 * CompoundColorMapLayerChild Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
// This class is loosely based on gov.noaa.pmel.util.ColorMap.
package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.array.IntArray;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import gov.noaa.pmel.sgt.AbstractPane;
import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.Layer;
import gov.noaa.pmel.sgt.LayerChild;
import gov.noaa.pmel.sgt.LayerNotFoundException;
// import gov.noaa.pmel.sgt.Pane;
import gov.noaa.pmel.sgt.SGLabel;
import gov.noaa.pmel.util.Point2D;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * This is a LayerChild than can draw a ColorKey (a color bar for a legend) for a CompoundColorMap.
 * Currently: this doen't support selectable (something related to SGT interactive use). Currently:
 * this doen't support attribute flags: L, U, B (they are ignored). See drawEveryNthLabel instead.
 */
public class CompoundColorMapLayerChild implements LayerChild {

  protected CompoundColorMap ccm;
  protected Layer layer;
  protected String id = "";
  protected boolean visible = true;
  protected boolean selectable = false;
  protected boolean selected = false;

  protected double leftX, lowerY, width, height;
  protected int drawEveryNthLabel = 0;
  protected Font labelFont;
  protected double labelHeight;
  protected double ticLength;

  /** Bob Simons added this to avoid memory leak problems. */
  @Override
  public void releaseResources() throws Exception {
    try {
      ccm = null;
      layer = null;
      if (JPane.debug) String2.log("sgt.CompoundColorMapLayerChild.releaseResources() finished");
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      if (JPane.debug) String2.pressEnterToContinue();
    }
  }

  /**
   * The constructor.
   *
   * @param id an identifying string
   * @param compoundColorMap
   */
  public CompoundColorMapLayerChild(String id, CompoundColorMap compoundColorMap) {
    this.id = id;
    ccm = compoundColorMap;
  }

  /**
   * Set the Rectangle defining where the actual colorbar will be. For a horizontal colorbar, make
   * width &gt; height. For a vertical colorbar, make height &gt; width.
   *
   * @param lowerX in pixels
   * @param leftY
   * @param width
   * @param height
   */
  public void setRectangle(double leftX, double lowerY, double width, double height) {
    this.leftX = leftX;
    this.lowerY = lowerY;
    this.width = width;
    this.height = height;
  }

  /**
   * Set the label font.
   *
   * @param font
   */
  public void setLabelFont(Font font) {
    this.labelFont = font;
  }

  /**
   * Set the label font.
   *
   * @param font
   */
  public void setLabelHeightP(double labelHeight) {
    this.labelHeight = labelHeight;
  }

  /**
   * Set the tic length.
   *
   * @param tic length
   */
  public void setTicLength(double ticLength) {
    this.ticLength = ticLength;
  }

  /**
   * Draw the <code>LayerChild</code>.
   *
   * @param g Graphics context
   * @exception LayerNotFoundException No layer is associated with the <code>LayerChild</code>.
   */
  @Override
  public void draw(Graphics g) throws LayerNotFoundException {

    // AbstractPane pane = getPane(); //removed 2008-11-18

    int n = ccm.rangeLow.length;
    if (width > height) {
      // draw horizontal colorbar
      int left;
      int right = layer.getXPtoD(leftX);

      // set drawEveryNthlabel
      // find max label length
      IntArray lengths = new IntArray();
      boolean hasBottomLabel = false;
      for (int piece = 0; piece <= n; piece++) { // yes, =n is last label
        String tLabel = piece == n ? ccm.lastLabel : ccm.leftLabel[piece];
        int brPo = tLabel.indexOf("<br>");
        if (brPo < 0) brPo = tLabel.length();
        else hasBottomLabel = true;
        lengths.add(brPo);
      }
      double tLowerY = lowerY;
      if (hasBottomLabel) tLowerY += 3 * ticLength; // move it up by 3* ticLength
      int yLabel = layer.getYPtoD(tLowerY - ticLength);
      int shortYLabel = layer.getYPtoD(tLowerY - ticLength / 2);
      int y1 =
          layer.getYPtoD(
              tLowerY + height + (hasBottomLabel ? -2 * ticLength : 0)); // narrower by 2 ticLengths
      int y2 = layer.getYPtoD(tLowerY);
      // .55: assume each char's width is .55*labelHeight  (crude, empirical)
      lengths.sort();
      int max = lengths.get(Math.max(0, lengths.size() - 2)); // get next-to-longest length
      max += 1;
      // String2.log("CompoundColorMapLayerChild drawEvery max+1=" + max + " d=" + (n * 0.55 * max *
      // labelHeight / width));
      drawEveryNthLabel =
          Math.max(1, Math2.roundToInt(Math.ceil(n * 0.55 * max * labelHeight / width)));

      // draw the colorbar
      SGLabel label;
      for (int piece = 0; piece < n; piece++) {
        left = right;
        right = layer.getXPtoD(leftX + (piece + 1) * width / n);
        // String2.log("ccmLayerChild.draw piece=" + piece +
        //    " y1=" + y1 + " y2=" + y2 + " left=" + left + " right=" + right);

        // draw vertical lines to fill the color bar
        int tRight = piece == n - 1 ? right : right - 1;
        for (int x = left; x <= tRight; x++) {
          g.setColor(
              ccm.getColor(
                  ccm.rangeLow[piece]
                      + (x - left)
                          * (ccm.rangeHigh[piece] - ccm.rangeLow[piece])
                          / (right - left + 1)));
          // g.drawLine(x, y1, x, y2 - 1); didn't look good on pdf, so fill it and overlap
          g.fillRect(x, y1, 1, y2 - y1);
        }

        // draw the left label
        g.setColor(Color.black);
        if ((piece % drawEveryNthLabel) == 0) {
          g.drawLine(left, y2, left, yLabel);
          drawBottomLabel(
              g,
              ccm.leftLabel[piece],
              leftX + piece * width / n - labelHeight / 8,
              tLowerY - ticLength - labelHeight / 2);
        } else {
          g.drawLine(left, y2, left, shortYLabel);
        }

        // draw the right label of the last piece
        if (piece == n - 1) {
          if (((piece + 1) % drawEveryNthLabel) == 0) {
            g.drawLine(right, y2, right, yLabel);
            drawBottomLabel(
                g,
                ccm.lastLabel,
                leftX + (piece + 1) * width / n - labelHeight / 8,
                tLowerY - ticLength - labelHeight / 2);
          } else {
            g.drawLine(right, y2, right, shortYLabel);
          }
        }
      }
    } else {
      // draw vertical colorbar
      int bottom;
      int top = layer.getYPtoD(lowerY);
      int x1 = layer.getXPtoD(leftX);
      int x2 = layer.getXPtoD(leftX + width);
      int xLabel = layer.getXPtoD(leftX + width + ticLength);
      int shortXLabel = layer.getXPtoD(leftX + width + ticLength / 3);
      // calculate drawEveryNthLabel
      drawEveryNthLabel = Math.max(1, Math2.roundToInt(Math.ceil(n * 0.75 * labelHeight / height)));
      SGLabel label;
      for (int piece = 0; piece < n; piece++) {
        bottom = top;
        top = layer.getYPtoD(lowerY + (piece + 1) * height / n);
        // String2.log("ccmLayerChild.draw piece=" + piece +
        //    " x1=" + x1 + " x2=" + x2 + " bottom=" + bottom + " top=" + top);

        // draw horizontal lines to fill the color bar
        int tTop = piece == n - 1 ? top - 1 : top; // -1 because y increases downwards
        for (int y = bottom; y > tTop; y--) {
          g.setColor(
              ccm.getColor(
                  ccm.rangeLow[piece]
                      + (y - bottom)
                          * (ccm.rangeHigh[piece] - ccm.rangeLow[piece])
                          / (top - bottom - 1))); // -1 since y increases downwards
          // g.drawLine(x1, y, x2 - 1, y);  didn't look good on pdf, so fill it and overlap
          g.fillRect(x1, y, x2 - x1, 1);
        }

        // draw the bottom label
        g.setColor(Color.black);
        if ((piece % drawEveryNthLabel) == 0) {
          g.drawLine(x2, bottom, xLabel, bottom);
          label =
              new SGLabel(
                  "",
                  ccm.leftLabel[piece],
                  labelHeight,
                  new Point2D.Double(
                      leftX + width + ticLength + labelHeight / 3, lowerY + piece * height / n),
                  SGLabel.CENTER,
                  SGLabel.LEFT);
          label.setOrientation(SGLabel.HORIZONTAL);
          label.setFont(labelFont);
          label.setLayer(layer);
          label.draw(g);
        } else {
          g.drawLine(x2, bottom, shortXLabel, bottom);
        }

        // draw the top label of the last piece
        if (piece == n - 1) {
          if (((piece + 1) % drawEveryNthLabel) == 0) {
            g.drawLine(x2, top, xLabel, top);
            label =
                new SGLabel(
                    "",
                    ccm.lastLabel,
                    labelHeight,
                    new Point2D.Double(
                        leftX + width + ticLength + labelHeight / 3,
                        lowerY + (piece + 1) * height / n),
                    SGLabel.CENTER,
                    SGLabel.LEFT);
            label.setOrientation(SGLabel.HORIZONTAL);
            label.setFont(labelFont);
            label.setLayer(layer);
            label.draw(g);
          } else {
            g.drawLine(x2, top, shortXLabel, top);
          }
        }
      }
    }
  }

  // this manually deals with <br> in a label
  // I can't use attrubutedString2 because only have g, not g2
  private void drawBottomLabel(Graphics g, String wholeLabel, double tx, double ty)
      throws LayerNotFoundException {
    int brPo = wholeLabel.indexOf("<br>");
    String tLabel = brPo < 0 ? wholeLabel : wholeLabel.substring(0, brPo);
    SGLabel label =
        new SGLabel(
            "", tLabel, labelHeight, new Point2D.Double(tx, ty), SGLabel.CENTER, SGLabel.CENTER);
    label.setOrientation(SGLabel.ANGLE);
    label.setAngle(0);
    label.setFont(labelFont);
    label.setLayer(layer);
    label.draw(g);

    if (brPo >= 0) {
      tLabel = wholeLabel.substring(brPo + 4);
      label =
          new SGLabel(
              "",
              tLabel,
              labelHeight,
              new Point2D.Double(tx, ty - .8 * labelHeight),
              SGLabel.CENTER,
              SGLabel.CENTER);
      label.setOrientation(SGLabel.ANGLE);
      label.setAngle(0);
      label.setFont(labelFont);
      label.setLayer(layer);
      label.draw(g);
    }
  }

  /**
   * Get the associated <code>Layer</code>.
   *
   * @return Associated layer
   */
  @Override
  public Layer getLayer() {
    return layer;
  }

  /**
   * Associate a <code>Layer</code> with the <code>LayerChild</code>.
   *
   * @param l Parent layer.
   */
  @Override
  public void setLayer(Layer l) {
    layer = l;
  }

  /**
   * Get the identifier.
   *
   * @return <code>LayerChild</code> identification.
   */
  @Override
  public String getId() {
    return id;
  }

  /**
   * Set the identifier.
   *
   * @param id <code>LayerChild</code> identification.
   */
  @Override
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Create a copy of the <code>LayerChild</code>. Currently, the implementation is pathetic.
   *
   * @return A copy of the <code>LayerChild</code>.
   */
  @Override
  public LayerChild copy() {
    return this; // is this good enough?
  }

  /**
   * Return a string that represents the <code>LayerChild</code>. Currently, the implementation is
   * pathetic.
   *
   * @return Stringified <code>LayerChild</code> representation.
   */
  @Override
  public String toString() {
    return "[CompoundColorMap]";
  }

  /**
   * Check if <code>LayerChild</code> is visible.
   *
   * @since 2.0
   * @return true if visible
   */
  @Override
  public boolean isVisible() {
    return visible;
  }

  /**
   * Set visibility for a <code>LayerChild</code>.
   *
   * @since 2.0
   * @param visible visible if true
   */
  @Override
  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  /**
   * Get <code>AbstractPane</code> of the <code>LayerChild</code>.
   *
   * @since 2.0
   */
  @Override
  public AbstractPane getPane() {
    return layer.getPane();
  }

  /**
   * Used by sgt internally.
   *
   * @since 2.0
   */
  @Override
  public void modified(String mess) {}

  // ********* implement selectable

  /**
   * Sets the selected property.
   *
   * @param sel true if selected, false if not.
   */
  @Override
  public void setSelected(boolean sel) {
    selected = sel;
  }

  /**
   * Returns true if the object's selected property is set.
   *
   * @return true if selected, false if not.
   */
  @Override
  public boolean isSelected() {
    return selected;
  }

  /**
   * Gets the bounding rectangle in device coordinates.
   *
   * @return bounding rectangle
   */
  @Override
  public Rectangle getBounds() {
    return new Rectangle(0, 0, 0, 0);
  }

  /**
   * Returns true if the current state is selectable.
   *
   * @return true if selectable
   */
  @Override
  public boolean isSelectable() {
    return selectable;
  }

  /**
   * Set the Selectable property. Currently, this is not supported.
   *
   * @param select if true object is selectable
   */
  @Override
  public void setSelectable(boolean select) {
    selectable = select;
  }
}
