package gov.noaa.pmel.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;

/**
 * Creates icon of three dots to be used with a <code>JButton</code> to open a dialog.
 *
 * @author Donald Denbo
 * @version $Revision: 1.2 $
 * @since 3.0
 */
public class ThreeDotsIcon implements Icon {
  private Color color_;
  private int size_;
  private int space_;
  private int dot_;
  private int y_;

  public ThreeDotsIcon() {
    this(Color.black, 14);
  }

  public ThreeDotsIcon(Color color) {
    this(color, 14);
  }

  public ThreeDotsIcon(Color color, int size) {
    color_ = color;
    if (size <= 14) {
      dot_ = 2;
      space_ = 2;
      size_ = 14;
    } else if (size > 14 && size <= 17) {
      dot_ = 3;
      space_ = 2;
      size_ = 17;
    } else if (size > 17 && size <= 20) {
      dot_ = 4;
      space_ = 2;
      size_ = 20;
    } else {
      size_ = size;
      dot_ = size_ / 5;
      space_ = dot_ / 2;
      if (space_ <= 1) space_ = 2;
      dot_ = (size_ - 4 * space_) / 3;
    }
    y_ = (size_ - dot_) / 2;
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    int xt, yt;
    Color save = g.getColor();
    g.setColor(color_);

    xt = x + space_;
    yt = y + y_;
    g.fillOval(xt, yt, dot_, dot_);
    xt = xt + space_ + dot_;
    g.fillOval(xt, yt, dot_, dot_);
    xt = xt + space_ + dot_;
    g.fillOval(xt, yt, dot_, dot_);

    g.setColor(save);
  }

  @Override
  public int getIconWidth() {
    return size_;
  }

  @Override
  public int getIconHeight() {
    return size_;
  }
}
