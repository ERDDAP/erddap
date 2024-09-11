/*
 * $Id: LabelDrawer.java,v 1.5 2003/08/22 23:02:32 dwd Exp $
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

import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.Rectangle2D;
import java.awt.*;
import java.io.Serializable;

/**
 * Defines the methods that implement label drawing in sgt. This interface is necessary since sgt
 * v2.0 will use Java2D functionality to draw labels if it is available.
 *
 * @author Donald Denbo
 * @version $Revision: 1.5 $, $Date: 2003/08/22 23:02:32 $
 * @since 2.0
 */
public interface LabelDrawer extends Serializable {
  /** Used internally by sgt. */
  public void draw(Graphics g) throws LayerNotFoundException;

  /**
   * Set the label text.
   *
   * @param lbl the label text
   */
  public void setText(String lbl);

  /**
   * Get the label text.
   *
   * @return the label text
   */
  public String getText();

  /**
   * Get the color.
   *
   * @return The current color of the label.
   */
  public Color getColor();

  /**
   * Set the color.
   *
   * @param color The color of the label.
   * @see java.awt.Color
   */
  public void setColor(Color clr);

  /**
   * Set the font.
   *
   * @param fnt The Font to use to draw the label.
   * @see java.awt.Font
   */
  public void setFont(Font font);

  /**
   * Get the font.
   *
   * @return The current font for the label.
   */
  public Font getFont();

  /** Used internally by sgt. */
  public void setLayer(Layer layer);

  /**
   * Get the layer.
   *
   * @return the layer object.
   */
  public Layer getLayer();

  /**
   * Set the orientation. The orientation can be HORIZONTAL or VERTICAL.
   *
   * @param orient The orientation.
   */
  public void setOrientation(int orient);

  /**
   * Get the origentation.
   *
   * @return the orientation
   */
  public int getOrientation();

  /**
   * Set the horizontal alignment. The alignment can be LEFT, CENTER, or RIGHT.
   *
   * @param horz The horizontal alignment.
   */
  public void setHAlign(int halign);

  /**
   * Get the horizontal alignment.
   *
   * @return the horizontal alignment.
   */
  public int getHAlign();

  /**
   * Set the vertical alignment. The alignment can be TOP, MIDDLE, or BOTTOM.
   *
   * @param vert The vertical alignment.
   */
  public void setVAlign(int valign);

  /**
   * Get the vertical alignment.
   *
   * @return the vertical alignment.
   */
  public int getVAlign();

  /**
   * Set the label position in device coordinates.
   *
   * @param loc label position
   */
  public void setLocation(Point loc);

  /**
   * Get the label position in device coordinates.
   *
   * @return the label position
   */
  public Point getLocation();

  /**
   * Set the label bounds in device units.
   *
   * @param x x location of label
   * @param y y location of label
   * @param width label width
   * @param height label height
   */
  public void setBounds(int x, int y, int width, int height);

  /**
   * Get the label bounds in device units.
   *
   * @return the label bounds
   */
  public Rectangle getBounds();

  /**
   * Set the label reference location in physcial coordinates.
   *
   * @param loc physical location of label
   */
  public void setLocationP(Point2D.Double loc);

  /**
   * Get the label reference location in physcial coordinates.
   *
   * @return the labels position.
   */
  public Point2D.Double getLocationP();

  /**
   * Get the label reference location in physcial coordinates.
   *
   * @return the labels position.
   */
  public Rectangle2D.Double getBoundsP();

  /**
   * Draw label at arbitrary rotation. Warning: Rotated labels are not drawn very well when using
   * JDK1.1. For best results use JDK1.2 or newer.
   */
  public void setAngle(double angle);

  /** Get label drawing angle. */
  public double getAngle();

  /**
   * Set the height of the label in physical coordinates.
   *
   * @param hgt The label height.
   */
  public void setHeightP(double hgt);

  /**
   * Get the label height in physical coordinates.
   *
   * @return The label height.
   */
  public double getHeightP();

  /** Set visibility of label. */
  public void setVisible(boolean vis);

  /** Is label visible? */
  public boolean isVisible();

  /** Get the string width in device units. */
  public float getStringWidth(Graphics g);

  /** Get the string height in device units. */
  public float getStringHeight(Graphics g);
}
