/*
 * $Id: ColorEntryPanel.java,v 1.2 2003/08/22 23:02:39 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */

package gov.noaa.pmel.sgt.swing.prop;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

import gov.noaa.pmel.swing.ThreeDotsButton;


/**
 * @author Donald Denbo
 * @version $Revision: 1.2 $, $Date: 2003/08/22 23:02:39 $
 * @since 3.0
 **/
public class ColorEntryPanel extends JComponent {
//  private ThreeDotsIcon dotsIcon_ = new ThreeDotsIcon(Color.black);
  private JLabel redLabel = new JLabel();
  private JLabel greenLabel = new JLabel();
  private JLabel blueLabel = new JLabel();
  private JLabel alphaLabel = new JLabel();
  private JTextField redTF = new JTextField();
  private JTextField greenTF = new JTextField();
  private JTextField blueTF = new JTextField();
  private JTextField alphaTF = new JTextField();
  private ThreeDotsButton button = new ThreeDotsButton();
  private FlowLayout fLayout = new FlowLayout();

  private Color color_ = Color.black;
  private String title_ = "Set Color";

  public ColorEntryPanel(String title, Color color) {
    super();
    setColor(color);
    setTitle(title);
  }

  public ColorEntryPanel() {
    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }

  private void jbInit() throws Exception {
    setLayout(fLayout);
    redLabel.setText("red");
    greenLabel.setText("green");
    blueLabel.setText("blue");
    alphaLabel.setText("alpha");
    redTF.setText("0");
    redTF.setColumns(3);
    greenTF.setText("0");
    greenTF.setColumns(3);
    blueTF.setText("0");
    blueTF.setColumns(3);
    alphaTF.setText("0");
    alphaTF.setColumns(3);
    button.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        button_actionPerformed(e);
      }
    });
    button.setToolTipText("Edit color.");
    button.setActionCommand("...");
    add(redLabel);
    add(redTF);
    add(greenLabel);
    add(greenTF);
    add(blueLabel);
    add(blueTF);
    add(alphaLabel);
    add(alphaTF);
    add(button);
    boolean enabled = isEnabled();
    redTF.setEnabled(enabled);
    greenTF.setEnabled(enabled);
    blueTF.setEnabled(enabled);
    alphaTF.setEnabled(enabled);
    button.setEnabled(enabled);
  }

  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    redTF.setEnabled(enabled);
    greenTF.setEnabled(enabled);
    blueTF.setEnabled(enabled);
    alphaTF.setEnabled(enabled);
    button.setEnabled(enabled);
  }

  void button_actionPerformed(ActionEvent e) {
    Window win = javax.swing.SwingUtilities.getWindowAncestor(this);
    ColorDialog cd = null;
    if(win instanceof Frame) {
      cd = new ColorDialog((Frame)win, title_, true);
    } else if(win instanceof Dialog) {
      cd = new ColorDialog((Dialog)win, title_, true);
    } else {
      cd = new ColorDialog((Frame)null, title_, true);
    }
    cd.setColor(getColorFromTF());
    cd.setVisible(true);
    setColor(cd.getColor());
  }

  public void setColor(Color color) {
    color_ = color;
    redTF.setText(Integer.toString(color_.getRed()));
    greenTF.setText(Integer.toString(color_.getGreen()));
    blueTF.setText(Integer.toString(color_.getBlue()));
    alphaTF.setText(Integer.toString(color_.getAlpha()));
  }

  public Color getColor() {
    return getColorFromTF();
  }

  private Color getColorFromTF() {
    int red = Integer.parseInt(redTF.getText());
    int green = Integer.parseInt(greenTF.getText());
    int blue = Integer.parseInt(blueTF.getText());
    int alpha = Integer.parseInt(alphaTF.getText());
    return new Color(red, green, blue, alpha);
  }

  public void setTitle(String title) {
    title_ = title;
  }
  public String getTitle() {
    return title_;
  }
}