/*
 * $Id: ColorDialog.java,v 1.2 2003/08/22 23:02:39 dwd Exp $
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

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.border.*;

/**
 * @author Donald Denbo
 * @version $Revision: 1.2 $, $Date: 2003/08/22 23:02:39 $
 * @since 3.0
 **/
public class ColorDialog extends JDialog {
  private JPanel panel1 = new JPanel();
  private JColorChooser colorChooserPanel = new JColorChooser();
  private JPanel alphaPanel = new JPanel();
  private JPanel buttonPanel = new JPanel();
  private JButton cancelButton = new JButton();
  private JButton okButton = new JButton();
  private TitledBorder titledBorder1;
  private JLabel jLabel1 = new JLabel();
  private Border border1;
  private GridBagLayout gridBagLayout1 = new GridBagLayout();
  private JTextField alphaTF = new JTextField();
  private GridBagLayout gridBagLayout2 = new GridBagLayout();

  private Color color_ = null;

  public ColorDialog(Dialog dialog,  String title, boolean modal) {
    super(dialog, title, modal);
    init(dialog);
  }

  public ColorDialog(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    init(frame);
  }

  private void init(Window win) {
    try {
      jbInit();
      pack();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
    if(win != null) {
      Rectangle fBounds = win.getBounds();
      Point fLoc = win.getLocationOnScreen();
      Rectangle bounds = getBounds();
      int x = fLoc.x + fBounds.width/2 - bounds.width/2;
      int y = fLoc.y + fBounds.height/2 - bounds.height/2;
      setLocation(x, y);
    }
  }

  public ColorDialog() {
    this((Frame)null, "", false);
  }

  private void jbInit() throws Exception {
    titledBorder1 = new TitledBorder("");
    border1 = BorderFactory.createLineBorder(Color.gray,1);
    panel1.setLayout(gridBagLayout1);
    buttonPanel.setBorder(BorderFactory.createEtchedBorder());
    cancelButton.setText("Cancel");
    cancelButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        cancelButton_actionPerformed(e);
      }
    });
    okButton.setText("OK");
    okButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        okButton_actionPerformed(e);
      }
    });
    alphaPanel.setBorder(titledBorder1);
    alphaPanel.setLayout(gridBagLayout2);
    titledBorder1.setTitle("Alpha Channel");
    titledBorder1.setBorder(border1);
    jLabel1.setText("Alpha");
    alphaTF.setText("0");
    alphaTF.setColumns(5);
    getContentPane().add(panel1);
    panel1.add(colorChooserPanel,    new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
    panel1.add(alphaPanel,    new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 5, 5, 5), 0, 0));
    alphaPanel.add(jLabel1,   new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 10, 5, 5), 0, 0));
    alphaPanel.add(alphaTF,    new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 15), 0, 0));
    panel1.add(buttonPanel,     new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 5, 5, 5), 0, 0));
    buttonPanel.add(okButton, null);
    buttonPanel.add(cancelButton, null);
  }

  void cancelButton_actionPerformed(ActionEvent e) {
    setVisible(false);
  }

  void okButton_actionPerformed(ActionEvent e) {
    Color temp = colorChooserPanel.getColor();
    int alpha = Integer.parseInt(alphaTF.getText());
    color_ = new Color(temp.getRed(), temp.getGreen(), temp.getBlue(), alpha);
    setVisible(false);
  }

  public void setColor(Color color) {
    color_ = color;
    colorChooserPanel.setColor(color_);
    alphaTF.setText(Integer.toString(color_.getAlpha()));
  }

  public Color getColor() {
    return color_;
  }
}