/*
 * $Id: FontDialog.java,v 1.7 2003/08/22 23:02:39 dwd Exp $
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
import java.awt.*;

/**
 * Edits a <code>Font</code> object.  This dialog does not
 * make a copy of the object so changes "Applied" will cause
 * <code>sgt</code> to redraw the plot using the new <code>Font</code>
 * properties unless
 * {@link gov.noaa.pmel.sgt.JPane#setBatch(boolean) batching}
 * has been turned on.
 *
 * <p> Example of <code>FontDialog</code> use:
 * <pre>
 * public void editFont(Font font) {
 *   FontDialog fd = new FontDialog();
 *   fd.showDialog(font);
 * }
 * </pre>
 *
 * @author Donald Denbo
 * @version $Revision: 1.7 $, $Date: 2003/08/22 23:02:39 $
 * @since 2.0
 */
public class FontDialog extends JDialog {
  private int result_;
  private Font font_;
  private String[] fontNames_;
  private int[] styles_ = {Font.PLAIN, Font.BOLD, Font.ITALIC, Font.BOLD|Font.ITALIC};
  private String[] styleNames_ = {"plain", "bold", "italic", "bold-italic"};
  /** OK button was selected */
  public static int OK_RESPONSE = 1;
  /** Cancel button was selected */
  public static int CANCEL_RESPONSE = 2;
  /**
   * Constructor.
   */
  public FontDialog(Frame parent) {
    super(parent);
    try {
      jbInit();
    } catch(Exception ex) {
      ex.printStackTrace();
    }
  }
  void jbInit() throws Exception {
    //{{INIT_CONTROLS
    getContentPane().setLayout(new BorderLayout(0,0));
    setSize(450,141);
    setVisible(false);
    buttonPanel.setBorder(etchedBorder1);
    buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
    italicButton.setMnemonic('0');
    okButton.setText("OK");
    okButton.setActionCommand("OK");
    cancelButton.setText("Cancel");
    cancelButton.setActionCommand("Cancel");
    buttonPanel.add(okButton);
    buttonPanel.add(cancelButton);
    //$$ etchedBorder1.move(0,228);
    mainPanel.setLayout(new GridBagLayout());
    getContentPane().add(mainPanel, "Center");
    mainPanel.add(fontComboBox, new GridBagConstraints(0,1,1,1,0.0,0.0,
    GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(5,5,5,1),0,0));
    boldButton.setText("Bold");
    mainPanel.add(boldButton,  new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 1, 5, 2), 0, 0));
    italicButton.setText("Italic");
    mainPanel.add(italicButton,  new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 2, 5, 5), 0, 0));
    italicButton.setFont(new Font("Dialog", Font.BOLD|Font.ITALIC, 12));
    fontLabel.setText("Font Name in font.");
    mainPanel.add(fontLabel, new GridBagConstraints(0,0,3,1,0.0,0.0,
    GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(5,5,5,5),0,0));
    this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    setTitle("Select a Mark");
    //}}

    //{{REGISTER_LISTENERS
    SymWindow aSymWindow = new SymWindow();
    this.addWindowListener(aSymWindow);
    SymAction lSymAction = new SymAction();
    cancelButton.addActionListener(lSymAction);
    okButton.addActionListener(lSymAction);
    fontComboBox.addActionListener(lSymAction);
    boldButton.addActionListener(lSymAction);
    italicButton.addActionListener(lSymAction);
    //}}
  }
  /** Used internally */
  public void addNotify() {
    // Record the size of the window prior to calling parents addNotify.
    Dimension d = getSize();

    super.addNotify();

    if (fComponentsAdjusted)
      return;

    // Adjust components according to the insets
    Insets ins = getInsets();
    setSize(ins.left + ins.right + d.width, ins.top + ins.bottom + d.height);
    Component components[] = getContentPane().getComponents();
    for (int i = 0; i < components.length; i++) {
      Point p = components[i].getLocation();
      p.translate(ins.left, ins.top);
      components[i].setLocation(p);
    }
    fComponentsAdjusted = true;
  }

  // Used for addNotify check.
  boolean fComponentsAdjusted = false;
  /**
   * Constructor.
   */
  public FontDialog(String title) {
    this();
    setTitle(title);
  }
  /**
   * Default constructor.
   */
  public FontDialog() {
    this((Frame)null);
  }
  /**
   * Make the dialog visible
   */
  public void setVisible(boolean b) {
    if(b) {
      setLocation(50, 50);
    }
    super.setVisible(b);
  }

  class SymWindow extends java.awt.event.WindowAdapter {
    public void windowClosing(java.awt.event.WindowEvent event) {
      Object object = event.getSource();
      if (object == FontDialog.this)
        FontDialog_WindowClosing(event);
    }
  }

  void FontDialog_WindowClosing(java.awt.event.WindowEvent event) {
    dispose();
  }

  //{{DECLARE_CONTROLS
  javax.swing.JPanel buttonPanel = new javax.swing.JPanel();
  javax.swing.JButton okButton = new javax.swing.JButton();
  javax.swing.JButton cancelButton = new javax.swing.JButton();
  javax.swing.border.EtchedBorder etchedBorder1 = new javax.swing.border.EtchedBorder();
  javax.swing.JPanel mainPanel = new javax.swing.JPanel();
  javax.swing.JComboBox fontComboBox = new javax.swing.JComboBox();
  javax.swing.JToggleButton boldButton = new javax.swing.JToggleButton();
  javax.swing.JToggleButton italicButton = new javax.swing.JToggleButton();
  javax.swing.JLabel fontLabel = new javax.swing.JLabel();
  //}}


  class SymAction implements java.awt.event.ActionListener {
    public void actionPerformed(java.awt.event.ActionEvent event) {
      Object object = event.getSource();
      if (object == cancelButton)
        cancelButton_actionPerformed(event);
      else if (object == okButton)
        okButton_actionPerformed(event);
      else if (object == fontComboBox)
        fontComboBox_actionPerformed(event);
      else if (object == boldButton)
        boldButton_actionPerformed(event);
      else if (object == italicButton)
        italicButton_actionPerformed(event);
    }
  }

  void cancelButton_actionPerformed(java.awt.event.ActionEvent event) {
    this.setVisible(false);
    result_ = CANCEL_RESPONSE;
  }

  void okButton_actionPerformed(java.awt.event.ActionEvent event) {
    result_ = OK_RESPONSE;
    this.setVisible(false);
  }
  /**
   * Test entry point
   */
  public static void main(String[] args) {
    FontDialog la = new FontDialog();
    la.setFont(null);
    la.setTitle("Test Font Dialog");
    la.setVisible(true);
  }
  /**
   * Show the dialog and wait for a response
   *
   * @param font the font
   * @return result, either CANCEL_RESPONSE or OK_RESPONSE
   */
  public int showDialog(Font font) {
//    fontNames_ = Toolkit.getDefaultToolkit().getFontList();
    fontNames_ = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
    for(int i=0; i < fontNames_.length; i++) {
      fontComboBox.addItem(fontNames_[i]);
    }
    setDefaultFont(font);
    result_ = CANCEL_RESPONSE;
    setModal(true);
    super.setVisible(true);
    return result_;
  }
  /**
   * Set the font to be edited
   */
  public void setDefaultFont(Font font) {
    int index=0;
    font_ = font;
    if(font_ == null) {
      font_ = super.getFont();
    }
    //
    // setup font combo box
    //
    for(int i=0; i < fontNames_.length; i++) {
      if(font_.getName().equals(fontNames_[i])) {
        index = i;
        break;
      }
    }
    boldButton.setSelected(font_.isBold());
    italicButton.setSelected(font_.isItalic());
    fontComboBox.setSelectedIndex(index);
    fontLabel.setText(fontString());
    fontLabel.setFont(font_);
  }
  /**
   * Create a string representation of the <code>Font</code>
   */
  public String fontString() {
    int style = (boldButton.isSelected()?1:0) + (italicButton.isSelected()?2:0);
    return font_.getName() + " " + styleNames_[style];
  }
  /**
   * Get the edited font.
   */
  public Font getFont() {
    return font_;
  }

  void fontComboBox_actionPerformed(java.awt.event.ActionEvent event) {
    updateFont();
  }

  void boldButton_actionPerformed(java.awt.event.ActionEvent event) {
    updateFont();
  }

  void italicButton_actionPerformed(java.awt.event.ActionEvent event) {
    updateFont();
  }

  void updateFont() {
    int style = (boldButton.isSelected()?1:0) + (italicButton.isSelected()?2:0);
    Font font = new Font(fontNames_[fontComboBox.getSelectedIndex()], styles_[style], 12);
    font_ = font;
    fontLabel.setText(fontString());
    fontLabel.setFont(font_);
  }
}
