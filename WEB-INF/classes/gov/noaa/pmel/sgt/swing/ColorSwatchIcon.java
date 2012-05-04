/*
 * $Id: ColorSwatchIcon.java,v 1.6 2003/08/22 23:02:39 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package gov.noaa.pmel.sgt.swing;

import gov.noaa.pmel.sgt.IndexedColor;
import gov.noaa.pmel.sgt.ColorMap;
import gov.noaa.pmel.util.Debug;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Color;
/**
 * <code>ColorSwatchIcon</code> implements <code>Icon</code> to create a
 * icon that displays a small square of
 * color. <code>ColorSwatchIcon</code> is used with property dialogs
 * to display/edit colors from an <code>IndexedColor</code> map.
 *
 * @author Donald Denbo
 * @version $Revision: 1.6 $, $Date: 2003/08/22 23:02:39 $
 * @since 2.0
 * @see Icon
 */
public class ColorSwatchIcon implements Icon, PropertyChangeListener {
  private int width_;
  private int height_;
//  private int size_;
  private IndexedColor cmap_;
  private int index_;
  private Color color_ = null;
  /**
   * Construct a <code>ColorSwatchIcon</code>.
   *
   * @param cmap indexed color map
   * @param index color index
   * @param size swatch size in pixels
   */
  public ColorSwatchIcon(IndexedColor cmap, int index, int size) {
    setSize(size);
    cmap_ = cmap;
    index_ = index;
    ((ColorMap)cmap_).addPropertyChangeListener(this);
    color_ = cmap_.getColorByIndex(index_);
  }

  /**
   * @since version 1.3
   * @param color
   * @param size
   */
  public ColorSwatchIcon(Color color, int width, int height) {
    index_ = -1;
    cmap_ = null;
    color_ = color;
    width_ = width;
    height_ = height;
  }
  /**
   * Get color index.
   */
  public int getIndex() {
    return index_;
  }
  /**
   * Get icon color.
   */
  public Color getColor() {
    return color_;
  }
  /**
   * Change the size of the swatch.
   */
  public void setSize(int size) {
    width_ = size;
    height_ = size;
  }
  /**
   * Get the size of the icon.
   */
//  public int getSize() {
//    return size_;
//  }
  /**
   * Paint the icon at the specified location
   */
  public void paintIcon(Component c, Graphics g, int x, int y) {
    g.setColor(color_);
    g.fillRect(x, y, width_, height_);
  }
  /**
   * Get the icon width.
   */
  public int getIconWidth() {
    return width_;
  }
  /**
   * Get the icon heigth.
   */
  public int getIconHeight() {
    return height_;
  }

  public String toString() {
    return "ColorSwatchIcon: ";
  }
  /**
   * <code>ColorSwatchIcon</code> listens for changes to the
   * <code>IndexedColor</code> color map. If changes occur the swatch
   * is updated.
   */
  public void propertyChange(PropertyChangeEvent event) {
    if(Debug.EVENT) {
      System.out.println("ColorSwatchIcon: " + event);
      System.out.println("                 " + event.getPropertyName());
    }
    if(event.getPropertyName().equals("color")) {
      //
      // color has changed
      //
      Color ncolor = cmap_.getColorByIndex(index_);
      if(!ncolor.equals(color_)) {
        color_ = ncolor;
        // notify here?
      }
    }
  }
}



