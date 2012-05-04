/*
 * $Id: BorderDialog.java,v 1.3 2003/08/25 23:42:59 dwd Exp $
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
import javax.swing.border.*;
import java.awt.event.*;

import gov.noaa.pmel.swing.ThreeDotsButton;
import gov.noaa.pmel.sgt.swing.prop.ColorEntryPanel;
import gov.noaa.pmel.sgt.swing.prop.FontDialog;

/**
 * Edit/create a <code>Border</code> object to be used with <code>PanelHolder</code>.
 * @author Donald Denbo
 * @version $Revision: 1.3 $, $Date: 2003/08/25 23:42:59 $
 * @since 3.0
 **/
public class BorderDialog extends JDialog {
  private JPanel panel1 = new JPanel();
  private BorderLayout borderLayout1 = new BorderLayout();
  private JPanel cardPanel = new JPanel();
  private JPanel bevelPanel = new JPanel();
  private JPanel jPanel3 = new JPanel();
  private JPanel etchedPanel = new JPanel();
  private JLabel jLabel1 = new JLabel();
  private JPanel borderPanel = new JPanel();
  private BorderLayout borderLayout2 = new BorderLayout();
  private CardLayout cardLayout1 = new CardLayout();
  private JPanel linePanel = new JPanel();
  private GridBagLayout gridBagLayout8 = new GridBagLayout();
  private GridBagLayout gridBagLayout5 = new GridBagLayout();
  private GridBagLayout gridBagLayout3 = new GridBagLayout();
  private JPanel buttonPanel = new JPanel();
  private JButton okButton = new JButton();
  private JButton cancelButton = new JButton();
  private JPanel emptyPanel = new JPanel();
  private String[] args = {"None", "Beveled",
      "Etched", "Line"};
  private String[] typeargs = {"Raised", "Lowered"};
  private JComboBox borderTypeCB = new JComboBox(args);
  private JComboBox bevelStyleCB = new JComboBox(typeargs);
  private JComboBox etchedStyleCB = new JComboBox(typeargs);

  private Border border_ = null;
  private Border working_ = null;
  private JLabel jLabel2 = new JLabel();
  private JLabel jLabel3 = new JLabel();
  private JLabel jLabel4 = new JLabel();
  private JLabel jLabel5 = new JLabel();
  private JLabel jLabel6 = new JLabel();
  private ColorEntryPanel bevelHOColorPanel = new ColorEntryPanel();
  private ColorEntryPanel bevelHIColorPanel = new ColorEntryPanel();
  private ColorEntryPanel bevelSOColorPanel = new ColorEntryPanel();
  private ColorEntryPanel bevelSIColorPanel = new ColorEntryPanel();
  private JLabel jLabel8 = new JLabel();
  private JLabel jLabel10 = new JLabel();
  private JLabel jLabel11 = new JLabel();
  private ColorEntryPanel etchedSColorPanel = new ColorEntryPanel();
  private ColorEntryPanel etchedHColorPanel = new ColorEntryPanel();
  private JLabel jLabel12 = new JLabel();
  private JLabel jLabel13 = new JLabel();
  private JLabel jLabel9 = new JLabel();
  private ColorEntryPanel lineColorPanel = new ColorEntryPanel();
  private JTextField lineTF = new JTextField();
  private JCheckBox lineCB = new JCheckBox();
  private JPanel titledPanel = new JPanel();
  private JCheckBox titledCB = new JCheckBox();
  private GridBagLayout gridBagLayout1 = new GridBagLayout();
  private JTextField titleTF = new JTextField();
  private JLabel jLabel7 = new JLabel();
  private JLabel jLabel14 = new JLabel();
  private JPanel jPanel1 = new JPanel();
  String[] justargs = {"Left", "Center", "Right"};
  private JComboBox justCB = new JComboBox(justargs);
  private JLabel jLabel15 = new JLabel();
  String[] posargs = {"Above Top", "Top", "Below Top",
                      "Above Bottom", "Bottom", "Below Bottom"};
  private JComboBox positionCB = new JComboBox(posargs);
  private GridBagLayout gridBagLayout2 = new GridBagLayout();
  private JLabel jLabel16 = new JLabel();
  private JLabel jLabel17 = new JLabel();
  private ColorEntryPanel titleColorPanel = new ColorEntryPanel();
  private ThreeDotsButton fontEditor = new ThreeDotsButton();
  private FlowLayout flowLayout1 = new FlowLayout();
  private JPanel fontPanel = new javax.swing.JPanel();
  private JLabel fontLabel = new javax.swing.JLabel();
  private String[] styleNames_ = {"plain", "bold", "italic", "bold-italic"};
  private Font labelFont_ = getFont();
  private boolean createNewBorder_ = true;

  /**
   * Dialog constructor.
   * @param frame parent frame
   * @param title dialog title
   * @param modal set true if modal dialog
   */
  public BorderDialog(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    init(frame);
  }

  /**
   * Default constructor.
   */
  public BorderDialog() {
    this(null, "", false);
  }

  private void init(Window win) {
    try {
      jbInit();
      pack();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
    this.setSize(520, 520);
    ((CardLayout)cardPanel.getLayout()).show(cardPanel, "none");
    if(win != null) {
      Rectangle fBounds = win.getBounds();
      Point fLoc = win.getLocationOnScreen();
      Rectangle bounds = getBounds();
      int x = fLoc.x + fBounds.width/2 - bounds.width/2;
      int y = fLoc.y + fBounds.height/2 - bounds.height/2;
      setLocation(x, y);
    } else {
      setLocation(200, 200);
    }
  }

  /**
   * Set the border.
   * @param border initialize dialog to this
   */
  public void setBorder(Border border) {
    border_ = border;
    if(border_ instanceof TitledBorder) {
      titledCB.setSelected(true);
      working_ = ((TitledBorder)border_).getBorder();
    } else {
      titledCB.setSelected(false);
      working_ = border_;
    }
    init();
  }

  /**
   * Get the new <code>Border</code>.
   * @return border
   */
  public Border getBorder() {
    return border_;
  }

  private void jbInit() throws Exception {
    panel1.setLayout(borderLayout1);
    cardPanel.setLayout(cardLayout1);
    borderTypeCB.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        borderTypeCB_actionPerformed(e);
      }
    });
    bevelPanel.setLayout(gridBagLayout5);
    etchedPanel.setLayout(gridBagLayout3);
    jLabel1.setText("Border Type");
    borderPanel.setMinimumSize(new Dimension(163, 300));
    borderPanel.setPreferredSize(new Dimension(167, 300));
    borderPanel.setLayout(borderLayout2);
    linePanel.setLayout(gridBagLayout8);
    okButton.setText("OK");
    okButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        okButton_actionPerformed(e);
      }
    });
    cancelButton.setText("Cancel");
    cancelButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        cancelButton_actionPerformed(e);
      }
    });
    jLabel2.setText("Style:");
    jLabel3.setText("Highlight Outer:");
    jLabel4.setText("Highlight Inner:");
    jLabel5.setText("Shadow Outer:");
    jLabel6.setText("Shadow Inner:");
    jLabel8.setText("Shadow:");
    jLabel10.setText("Highlight:");
    jLabel11.setText("Style:");
    jLabel12.setText("Color:");
    jLabel13.setText("Thickness:");
    jLabel9.setText("Rounded Corners:");
    lineTF.setText("2");
    lineTF.setColumns(5);
    lineColorPanel.setTitle("Set Line Color");
    bevelHOColorPanel.setTitle("Set Highlight Outer Color");
    bevelHIColorPanel.setTitle("Set Highlight Inner Color");
    bevelSOColorPanel.setTitle("Set Shadow Outer Color");
    bevelSIColorPanel.setTitle("Set Shadow Inner Color");
    etchedHColorPanel.setTitle("Set Highlight Color");
    etchedSColorPanel.setTitle("Set Shadow Color");
    buttonPanel.setBorder(BorderFactory.createEtchedBorder());
    titledPanel.setBorder(BorderFactory.createEtchedBorder());
    titledPanel.setLayout(gridBagLayout1);
    titledCB.setText("Titled");
    titledCB.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        titledCB_actionPerformed(e);
      }
    });
    jLabel7.setText("Title:");
    jLabel14.setText("Position:");
    jLabel15.setText("Justification:");
    jPanel1.setLayout(gridBagLayout2);
    jLabel16.setText("Color:");
    jLabel17.setText("Font:");
    titleColorPanel.setEnabled(false);
    titleColorPanel.setTitle("Set Title Color");
    fontEditor.setEnabled(false);
    fontEditor.setAlignmentY((float) 0.0);
    fontEditor.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        fontEditor_actionPerformed(e);
      }
    });
    fontEditor.setToolTipText("Edit font.");
    fontEditor.setActionCommand("...");
    fontPanel.setLayout(flowLayout1);
    fontLabel.setAlignmentY((float) 0.0);
    fontLabel.setText("Dialog, 12, Bold");
    fontLabel.setForeground(java.awt.Color.black);
    titleTF.setEnabled(false);
    positionCB.setEnabled(false);
    justCB.setEnabled(false);
    cardPanel.setMinimumSize(new Dimension(451, 225));
    cardPanel.setPreferredSize(new Dimension(451, 225));
    cardPanel.setToolTipText("");
    panel1.setMinimumSize(new Dimension(163, 360));
    panel1.setPreferredSize(new Dimension(187, 360));
    getContentPane().add(panel1);
    panel1.add(borderPanel,  BorderLayout.CENTER);
    jPanel3.add(jLabel1, null);
    jPanel3.add(borderTypeCB, null);
    borderPanel.add(titledPanel, BorderLayout.SOUTH);
    borderPanel.add(cardPanel, BorderLayout.CENTER);
    borderPanel.add(jPanel3, BorderLayout.NORTH);
    cardPanel.add(bevelPanel,  "bevel");
    bevelPanel.add(jLabel2,                   new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    bevelPanel.add(bevelStyleCB,                    new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    bevelPanel.add(jLabel3,                    new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    bevelPanel.add(jLabel4,                    new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    bevelPanel.add(jLabel5,                     new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    bevelPanel.add(jLabel6,                     new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    bevelPanel.add(bevelHOColorPanel,                     new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    bevelPanel.add(bevelHIColorPanel,                 new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    bevelPanel.add(bevelSOColorPanel,             new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    bevelPanel.add(bevelSIColorPanel,        new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    cardPanel.add(etchedPanel,  "etched");
    etchedPanel.add(jLabel8,                  new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    etchedPanel.add(jLabel10,                 new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    etchedPanel.add(jLabel11,              new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    etchedPanel.add(etchedSColorPanel,              new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    etchedPanel.add(etchedStyleCB,            new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    etchedPanel.add(etchedHColorPanel,              new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    cardPanel.add(linePanel,  "line");
    linePanel.add(jLabel9,           new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    linePanel.add(lineColorPanel,             new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    linePanel.add(jLabel12,       new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    linePanel.add(jLabel13,       new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    panel1.add(buttonPanel,  BorderLayout.SOUTH);
    buttonPanel.add(okButton, null);
    buttonPanel.add(cancelButton, null);
    cardPanel.add(emptyPanel,  "none");
    linePanel.add(lineTF,   new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    linePanel.add(lineCB,    new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    titledPanel.add(titledCB,               new GridBagConstraints(0, 0, 1, 1, 0.7, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 15, 0));
    titledPanel.add(titleTF,              new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 5, 15), 0, 0));
    titledPanel.add(jLabel7,           new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    titledPanel.add(jLabel14,            new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    titledPanel.add(jPanel1,           new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    jPanel1.add(positionCB,     new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    jPanel1.add(jLabel15,     new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    jPanel1.add(justCB,    new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    titledPanel.add(jLabel16,        new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    titledPanel.add(titleColorPanel,      new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    titledPanel.add(jLabel17,         new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 10, 5), 0, 0));
    titledPanel.add(fontPanel,     new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 10, 5), 0, 0));
    fontPanel.add(fontLabel, null);
    fontPanel.add(fontEditor, null);
    positionCB.setSelectedIndex(1);
  }

  void init() {
//    working_ = null;
    createNewBorder_ = false;
    String type = "none";
    int index = 0;
    if(working_ != null) {
      if(working_ instanceof BevelBorder) {
        BevelBorder bb = (BevelBorder)working_;

/*        working_ = new BevelBorder(bb.getBevelType(),
                                   bb.getHighlightOuterColor(),
                                   bb.getHighlightInnerColor(),
                                   bb.getShadowOuterColor(),
                                   bb.getShadowInnerColor()); */

        bevelHIColorPanel.setColor(bb.getHighlightInnerColor(this));
        bevelHOColorPanel.setColor(bb.getHighlightOuterColor(this));
        bevelSIColorPanel.setColor(bb.getShadowInnerColor(this));
        bevelSOColorPanel.setColor(bb.getShadowOuterColor(this));

        int style = bb.getBevelType();
        if(style == BevelBorder.RAISED) {
          bevelStyleCB.setSelectedItem("Raised");
        } else if(style == BevelBorder.LOWERED) {
          bevelStyleCB.setSelectedItem("Lowered");
        }

        type = "bevel";
        index = 1;
      } else if(working_ instanceof EtchedBorder) {
        EtchedBorder eb = (EtchedBorder)working_;

/*        working_ = new EtchedBorder(eb.getEtchType(),
                                    eb.getHighlightColor(),
                                    eb.getShadowColor()); */

        etchedHColorPanel.setColor(eb.getHighlightColor(this));
        etchedSColorPanel.setColor(eb.getShadowColor(this));

        int style = eb.getEtchType();
        if(style == EtchedBorder.RAISED) {
          etchedStyleCB.setSelectedItem("Raised");
        } else if(style == EtchedBorder.LOWERED) {
          etchedStyleCB.setSelectedItem("Lowered");
        }

        type = "etched";
        index = 2;
      } else if(working_ instanceof LineBorder) {
        LineBorder lb = (LineBorder)working_;

/*        working_ = new LineBorder(lb.getLineColor(),
                                  lb.getThickness(),
                                  lb.getRoundedCorners()); */

        lineColorPanel.setColor(lb.getLineColor());

        int thick = lb.getThickness();
        lineTF.setText(Integer.toString(thick));
        boolean round = lb.getRoundedCorners();
        lineCB.setSelected(round);

        type = "line";
        index = 3;
      }
      if(titledCB.isSelected()) {
        TitledBorder tb = (TitledBorder)border_;
        titleTF.setText(tb.getTitle());
        titleColorPanel.setColor(tb.getTitleColor());
        int tindex = -1;
        switch(tb.getTitlePosition()) {
          case TitledBorder.ABOVE_TOP:
            tindex = 0;
            break;
          default:
          case TitledBorder.TOP:
            tindex = 1;
            break;
          case TitledBorder.BELOW_TOP:
            tindex = 2;
            break;
          case TitledBorder.ABOVE_BOTTOM:
            tindex = 3;
            break;
          case TitledBorder.BOTTOM:
            tindex = 4;
            break;
          case TitledBorder.BELOW_BOTTOM:
            tindex = 5;
            break;
        }
        positionCB.setSelectedIndex(tindex);
        tindex = -1;
        switch(tb.getTitleJustification()) {
          default:
          case TitledBorder.LEFT:
            tindex = 0;
            break;
          case TitledBorder.CENTER:
            tindex = 1;
            break;
          case TitledBorder.RIGHT:
            tindex = 2;
            break;
        }
        justCB.setSelectedIndex(tindex);
        labelFont_ = tb.getTitleFont();
        fontLabel.setText(fontString(labelFont_));
      }
    }
    setTitledEnabled(titledCB.isEnabled());
    borderTypeCB.setSelectedIndex(index);
    ((CardLayout)cardPanel.getLayout()).show(cardPanel, type);
    createNewBorder_ = true;
  }

  void borderTypeCB_actionPerformed(ActionEvent e) {
    if(Page.DEBUG) System.out.println("ActionPerformed: new value = " + borderTypeCB.getSelectedItem());
    int index = borderTypeCB.getSelectedIndex();
    switch(index) {
      case 0:
        ((CardLayout)cardPanel.getLayout()).show(cardPanel, "none");
        break;
      case 1:
        if(createNewBorder_) working_ = new BevelBorder(BevelBorder.LOWERED);
        init();
        ((CardLayout)cardPanel.getLayout()).show(cardPanel, "bevel");
        break;
      case 2:
        if(createNewBorder_) working_ = new EtchedBorder(EtchedBorder.LOWERED);
        init();
        ((CardLayout)cardPanel.getLayout()).show(cardPanel, "etched");
        break;
      case 3:
        if(createNewBorder_) working_ = new LineBorder(Color.gray);
        init();
        ((CardLayout)cardPanel.getLayout()).show(cardPanel, "line");
        break;
      default:
        JOptionPane.showMessageDialog(this, "Selection Not Yet Implemented",
                                      "Not Implemented", JOptionPane.ERROR_MESSAGE);
        borderTypeCB.setSelectedIndex(0);
        ((CardLayout)cardPanel.getLayout()).show(cardPanel, "none");
    }

  }

  void cancelButton_actionPerformed(ActionEvent e) {
    setVisible(false);
    dispose();
  }

  void okButton_actionPerformed(ActionEvent e) {
    String borderType = (String)borderTypeCB.getSelectedItem();
    if(borderType.equals("None")) {
      working_ = null;
      titledCB.setSelected(false);
    } else if(borderType.equals("Beveled")) {
      Color highlighto = bevelHOColorPanel.getColor();
      Color highlighti = bevelHIColorPanel.getColor();
      Color shadowo = bevelSOColorPanel.getColor();
      Color shadowi = bevelSIColorPanel.getColor();
      int style = BevelBorder.RAISED;
      if(etchedStyleCB.getSelectedIndex() == 1) {
        style = BevelBorder.LOWERED;
      }
      working_ = new BevelBorder(style, highlighto, highlighti,
                                shadowo, shadowi);
    } else if(borderType.equals("Etched")) {
      Color highlight = etchedHColorPanel.getColor();
      Color shadow = etchedSColorPanel.getColor();
      int style = EtchedBorder.RAISED;
      if(etchedStyleCB.getSelectedIndex() == 1) {
        style = EtchedBorder.LOWERED;
      }
      working_ = new EtchedBorder(style, highlight, shadow);
    } else if(borderType.equals("Line")) {
      Color color = lineColorPanel.getColor();
      int thick = Integer.parseInt(lineTF.getText());
      boolean round = lineCB.isSelected();

      working_ = new LineBorder(color, thick, round);
    }
    if(titledCB.isSelected()) {
      int just = 0;
      int pos = 0;
      String posStr = (String)positionCB.getSelectedItem();
      if(posStr.equals("Above Top")) {
        pos = TitledBorder.ABOVE_TOP;
      } else if (posStr.equals("Below Top")) {
        pos = TitledBorder.BELOW_TOP;
      } else if (posStr.equals("Above Bottom")) {
        pos = TitledBorder.ABOVE_BOTTOM;
      } else if (posStr.equals("Bottom")) {
        pos = TitledBorder.BOTTOM;
      } else if (posStr.equals("Below Bottom")) {
        pos = TitledBorder.BELOW_BOTTOM;
      } else {
        pos = TitledBorder.TOP;
      }
      String justStr = (String)justCB.getSelectedItem();
      if(justStr.equals("Rigt")) {
        just = TitledBorder.RIGHT;
      } else if(justStr.equals("Center")) {
        just = TitledBorder.CENTER;
      } else {
        just = TitledBorder.LEFT;
      }
      border_ = new TitledBorder(working_,
                                 titleTF.getText(),
                                 just,
                                 pos,
                                 labelFont_,
                                 titleColorPanel.getColor());
    } else {
      border_ = working_;
    }
    setVisible(false);
    dispose();
  }

  void titledCB_actionPerformed(ActionEvent e) {
    if(Page.DEBUG) System.out.println("ActionPerformed: new value = " + titledCB.isSelected());
    setTitledEnabled(titledCB.isSelected());
    if(titledCB.isSelected()) {
      if(border_ instanceof TitledBorder) {
        ((TitledBorder)border_).setBorder(working_);
      } else {
        border_ = new TitledBorder(working_,
                                   titleTF.getText());
        init();
      }
    }
  }

  void setTitledEnabled(boolean en) {
    titleTF.setEnabled(en);
    positionCB.setEnabled(en);
    justCB.setEnabled(en);
    titleColorPanel.setEnabled(en);
    fontEditor.setEnabled(en);
  }

  void fontEditor_actionPerformed(ActionEvent e) {
    FontDialog fd = new FontDialog();
    int result = fd.showDialog(labelFont_);
    if(result == FontDialog.OK_RESPONSE) {
      labelFont_ = fd.getFont();
      fontLabel.setText(fontString(labelFont_));
      fontLabel.setFont(labelFont_);
    }
  }

  String fontString(Font font) {
    int style = (font.isBold()?1:0) + (font.isItalic()?2:0);
    return font.getName() + " " + styleNames_[style];
  }
}