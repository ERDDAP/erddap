/*
 * $Id: PropertyPanel.java,v 1.4 2003/09/17 22:30:02 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package gov.noaa.pmel.sgt.beans;

import java.awt.*;
import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;
import java.util.EventListener;
import java.util.Vector;
import java.text.DecimalFormat;
import java.util.StringTokenizer;

import gov.noaa.pmel.sgt.SGLabel;
import gov.noaa.pmel.util.Rectangle2D;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.SoTRange;
import gov.noaa.pmel.util.SoTPoint;
import gov.noaa.pmel.util.IllegalTimeValue;
import gov.noaa.pmel.util.GeoDate;
import gov.noaa.pmel.sgt.swing.ColorSwatchIcon;

/**
 * @author Donald Denbo
 * @version $Revision: 1.4 $, $Date: 2003/09/17 22:30:02 $
 * @since 3.0
 **/
abstract class PropertyPanel extends JComponent implements DesignListener {
  private PanelHolder pHolder_ = null;
  private JLabel jLabel1 = new JLabel();
  private JLabel jLabel3 = new JLabel();
  private GridBagLayout gridBagLayout1 = new GridBagLayout();
  private Font hdrFont_ = new Font("Dialog", 1, 9);
  private Font textFont_ = new Font("Dialog", 0, 9);
  private Insets lInset = new Insets(2, 1, 1, 3);
  private Insets rInset = new Insets(2, 3, 1, 1);
  private DecimalFormat numberFormat_ = new DecimalFormat("#.##"); //2015-09-02 was static
  //2011-12-15 Bob Simons changed space to 'T' and hh to HH (24 hour)
  private String dateFormat_ = "yyyy-MM-dd'T'HH:mm";

  public PropertyPanel() {
    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }

  private void jbInit() throws Exception {
    jLabel1.setText("Property");
    jLabel1.setFont(hdrFont_);
    jLabel1.setForeground(Color.black);
    jLabel1.setHorizontalAlignment(SwingConstants.RIGHT);
    this.setLayout(gridBagLayout1);
    jLabel3.setText("Value");
    jLabel3.setFont(hdrFont_);
    jLabel3.setForeground(Color.black);
    this.add(jLabel1,          new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, lInset, 5, 5));
    this.add(jLabel3,       new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, rInset, 10, 5));
  }

  abstract void resetFields();

  public void reset() {
    this.removeAll();
    resetFields();
    try {
      jbInit();
    } catch (Exception e) {
      e.printStackTrace();
    }
    create();
    revalidate();
  }

  protected void addProperty(int row, String propertyName, JComponent component, boolean last) {
    double weighty;
    int left, right;
    JLabel label = new JLabel(propertyName);
    label.setHorizontalAlignment(SwingConstants.RIGHT);
    label.setFont(textFont_);
    component.setFont(textFont_);
    if(last) {
      weighty = 1.0;
      left = GridBagConstraints.NORTHEAST;
      right = GridBagConstraints.NORTHWEST;
    } else {
      weighty = 0.0;
      left = GridBagConstraints.EAST;
      right = GridBagConstraints.WEST;
    }
    this.add(label, new GridBagConstraints(0, row, 1, 1, 0.0, weighty,
        left, GridBagConstraints.BOTH, lInset, 5, 5));
    this.add(component, new GridBagConstraints(1, row, 1, 1, 1.0, weighty,
        right, GridBagConstraints.HORIZONTAL, rInset, 5, 5));
  }

  abstract void update();
  abstract void create();

  protected JTextField createTextField(String text,
                                       String action, EventListener listen,
                                       boolean edit) {
    JTextField tf = new JTextField(text);
    tf.setActionCommand(action);
    tf.setName(action);
    tf.setEditable(edit);
    if(listen != null) {
      if(listen instanceof ActionListener) tf.addActionListener((ActionListener)listen);
      if(listen instanceof FocusListener) tf.addFocusListener((FocusListener)listen);
    }
    return tf;
  }

  protected JLabel createLabel(String text) {
    JLabel jl = new JLabel(text);
    return jl;
  }

  protected JLabel createLabel(int value) {
    return new JLabel(Integer.toString(value));
  }

  protected JCheckBox createCheckBox(boolean value,
                                     String action, ActionListener listen) {
    JCheckBox cb = new JCheckBox("", value);
    cb.setHorizontalTextPosition(SwingConstants.LEFT);
    cb.setActionCommand(action);
    if(listen != null) cb.addActionListener(listen);
    return cb;
  }

  protected JComboBox createComboBox(Vector list, int item,
                                     String action, ActionListener listen,
                                     boolean edit) {
    return createComboBox(list.toArray(), item, action, listen, edit);
  }

  protected JComboBox createComboBox(Object[] list, int item,
                                     String action, ActionListener listen,
                                     boolean edit) {
    JComboBox cb = new JComboBox(list);
    cb.setSelectedIndex(item);
    cb.setActionCommand(action);
    cb.setEditable(edit);
    cb.setEnabled(edit);
    if(listen != null) cb.addActionListener(listen);
    return cb;
  }

  protected JButton createSGLabel(SGLabel value,
                                String action, ActionListener listen) {
    if(value == null) return new JButton("null");
    JButton jb = new JButton(value.getText());
    jb.setFont(value.getFont());
    jb.setForeground(value.getColor());
//  jb.setHorizontalTextPosition(SwingConstants.LEFT);
    jb.setActionCommand(action);
    if(listen != null) jb.addActionListener(listen);
    return jb;
  }

  protected Point2D.Double parsePoint2D(String value) {
    StringTokenizer tok = new StringTokenizer(value, ",\t\n\r\f");
    if(tok.countTokens() != 2) {
      JOptionPane.showMessageDialog(this, "Four values required", "Illegal Response", JOptionPane.ERROR_MESSAGE);
      return null;
    }
    double x = Double.parseDouble(tok.nextToken().trim());
    double y = Double.parseDouble(tok.nextToken().trim());
    return new Point2D.Double(x, y);
  }

  protected Point parsePoint(String value) {
    StringTokenizer tok = new StringTokenizer(value, ",\t\n\r\f");
    if(tok.countTokens() != 2) {
      JOptionPane.showMessageDialog(this, "Two values required", "Illegal Response", JOptionPane.ERROR_MESSAGE);
      return null;
    }
    int x = Integer.parseInt(tok.nextToken().trim());
    int y = Integer.parseInt(tok.nextToken().trim());
    return new Point(x, y);
  }

  protected Dimension parseDimension(String value) {
    StringTokenizer tok = new StringTokenizer(value, ",\t\n\r\f");
    if(tok.countTokens() != 2) {
      JOptionPane.showMessageDialog(this, "Two values required", "Illegal Response", JOptionPane.ERROR_MESSAGE);
      return null;
    }
    int width = Integer.parseInt(tok.nextToken().trim());
    int height = Integer.parseInt(tok.nextToken().trim());
    return new Dimension(width, height);
  }

  protected Rectangle2D parseBounds(String value) {
    StringTokenizer tok = new StringTokenizer(value, ",\t\n\r\f");
    if(tok.countTokens() != 4) {
      JOptionPane.showMessageDialog(this, "Four values required", "Illegal Response", JOptionPane.ERROR_MESSAGE);
      return null;
    }
    double x = Double.parseDouble(tok.nextToken().trim());
    double y = Double.parseDouble(tok.nextToken().trim());
    double width = Double.parseDouble(tok.nextToken().trim());
    double height = Double.parseDouble(tok.nextToken().trim());
    return new Rectangle2D.Double(x, y, width, height);
  }

  protected SoTRange parseRange(String value, boolean isTime) {
    StringTokenizer tok = new StringTokenizer(value, ",\t\n\r\f");
    if(tok.countTokens() != 3) {
      JOptionPane.showMessageDialog(this, "Three values required", "Illegal Response", JOptionPane.ERROR_MESSAGE);
      return null;
    }
    SoTRange range = null;
    if(isTime) {
//2011-12-15 Bob Simons changed space to 'T' and hh to HH (24 hour)
//      String format = "yyyy-MM-dd'T'HH:mm";
      try {
        GeoDate start = new GeoDate(tok.nextToken().trim(), dateFormat_);
        GeoDate end = new GeoDate(tok.nextToken().trim(), dateFormat_);
        long dlta = Long.parseLong(tok.nextToken().trim())*86400000;
        GeoDate delta = new GeoDate(dlta);
        range = new SoTRange.Time(start, end, delta);
      } catch (IllegalTimeValue itv) {
        JOptionPane.showMessageDialog(this, "Illegal Time Value", "Illegal Response", JOptionPane.ERROR_MESSAGE);
        return null;
      }
    } else {
      double start = Double.parseDouble(tok.nextToken().trim());
      double end = Double.parseDouble(tok.nextToken().trim());
      double delta  = Double.parseDouble(tok.nextToken().trim());
      range = new SoTRange.Double(start, end, delta);
    }
    return range;
  }

  protected String colorString(Color value) {
    StringBuffer sbuf = new StringBuffer("[");
    sbuf.append(value.getRed()).append(", ");
    sbuf.append(value.getGreen()).append(", ");
    sbuf.append(value.getRed());
    if(value.getAlpha() == 255) {
      sbuf.append("]");
    } else {
      sbuf.append(", ").append(value.getAlpha()).append("]");
    }
    return sbuf.toString();
  }

  protected JButton createColor(Color value,
                                String action, ActionListener listen) {
    Icon icon = new ColorSwatchIcon(value, 32, 20);
    JButton jb = new JButton(colorString(value), icon);
    jb.setActionCommand(action);
    if(listen != null) jb.addActionListener(listen);
    return jb;
  }

  protected void updateColor(JButton comp, Color value) {
    comp.setText(colorString(value));
    Icon icon = new ColorSwatchIcon(value, 32, 20);
    comp.setIcon(icon);
  }

  protected JButton createFont(Font value,
                                String action, ActionListener listen) {
    JButton jb = new JButton(value.getName());
    jb.setFont(value);
//  jb.setHorizontalTextPosition(SwingConstants.LEFT);
    jb.setActionCommand(action);
    if(listen != null) jb.addActionListener(listen);
    return jb;
  }

  protected void updateFont(JButton comp, Font value) {
    comp.setText(value.getName());
    comp.setFont(value);
  }
  protected void updateSGLabel(JButton comp, SGLabel value) {
    comp.setFont(value.getFont());
    comp.setText(value.getText());
    comp.setForeground(value.getColor());
  }

  protected String format(Point val, boolean brackets) {
    StringBuffer buf = new StringBuffer();
    if(brackets) buf.append("[");
    buf.append(format(val.x)).append(", ");
    buf.append(format(val.y));
    if(brackets) buf.append("]");
    return buf.toString();
  }

  protected String format(Dimension val, boolean brackets) {
    StringBuffer buf = new StringBuffer();
    if(brackets) buf.append("[");
    buf.append(format(val.width)).append(", ");
    buf.append(format(val.height));
    if(brackets) buf.append("]");
    return buf.toString();
  }

  protected String format(Rectangle2D.Double val, boolean brackets) {
    StringBuffer buf = new StringBuffer();
    if(brackets) buf.append("[");
    buf.append(format(val.x)).append(", ");
    buf.append(format(val.y)).append(", ");
    buf.append(format(val.width)).append(", ");
    buf.append(format(val.height));
    if(brackets) buf.append("]");
    return buf.toString();
  }

  protected String format(Point2D.Double val, boolean brackets) {
    StringBuffer buf = new StringBuffer();
    if(brackets) buf.append("[");
    buf.append(format(val.x)).append(", ");
    buf.append(format(val.y));
    if(brackets) buf.append("]");
    return buf.toString();
  }

  protected String format(Range2D val, boolean brackets) {
    StringBuffer buf = new StringBuffer();
    if(brackets) buf.append("[");
    buf.append(format(val.start)).append(", ");
    buf.append(format(val.end)).append(", ");
    buf.append(format(val.delta));
    if(brackets) buf.append("]");
    return buf.toString();
  }

  protected String format(SoTRange.Double val, boolean brackets) {
    StringBuffer buf = new StringBuffer();
    if(brackets) buf.append("[");
    buf.append(format(val.start)).append(", ");
    buf.append(format(val.end)).append(", ");
    buf.append(format(val.delta));
    if(brackets) buf.append("]");
    return buf.toString();
  }

  protected String format(SoTRange.Time val, boolean brackets) {
    StringBuffer buf = new StringBuffer();
    if(brackets) buf.append("[");
    buf.append(val.getStart().toString()).append(", ");
    buf.append(val.getEnd().toString()).append(", ");
    buf.append(Long.toString(val.getDelta().getLongTime()/86400000)); // days
    if(brackets) buf.append("]");
    return buf.toString();
  }

  protected String format(SoTRange val, boolean brackets) {
    if(val instanceof SoTRange.Float) {
      return format((SoTRange.Float)val, brackets);
    } else if(val instanceof SoTRange.Double) {
      return format((SoTRange.Double)val, brackets);
    } else if(val instanceof SoTRange.Time) {
      return format((SoTRange.Time)val, brackets);
    }
    return "";
  }

  protected String format(SoTPoint val, boolean brackets) {
    if(val == null) return "null";
    return val.toString();
  }

  protected String format(float value) {
    return numberFormat_.format(value);
  }

  protected String format(float value, DecimalFormat format) {
    return format.format(value);
  }

  protected String format(double value, DecimalFormat format) {
    return format.format(value);
  }

  protected String format(double value) {
    DecimalFormat format = new DecimalFormat("#.##");
    return format.format(value);
  }

  protected void paintBorder(Graphics g) {
    int x0=0, x1=0, x2=0;
    int y;
    int y1=0;
    int y0=0;

    Component[] comps = getComponents();

    for(int i=0;  i < comps.length;i += 2) {
      if(i == 0) {
        x0 = comps[0].getBounds().x - lInset.left;
        x1 = comps[0].getBounds().x + comps[0].getBounds().width + lInset.right;
        x2 = comps[1].getBounds().x + comps[1].getBounds().width;
        y0 = comps[0].getBounds().y - lInset.top;
      }
      y = comps[i].getBounds().y - lInset.top;
      if(i >= comps.length - 2) {
        y1 = y;
      }
      g.drawLine(0, y, x2, y);
    }
    g.drawLine(x0, y0, x0, y1);
    g.drawLine(x1, y0, x1, y1);
    g.drawLine(x2, y0, x2, y1);
  }

  public String toString() {
    return getClass().getName() + '@' + Integer.toHexString(hashCode());
  }

  Frame getFrame() {
    Window fr = javax.swing.SwingUtilities.getWindowAncestor(this);
    if(fr != null && fr instanceof Frame) {
      return (Frame)fr;
    }
    return null;
  }

  abstract public void setExpert(boolean expert);
  abstract public boolean isExpert();
}