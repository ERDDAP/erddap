/*
 * $Id: PanelModelEditor.java,v 1.2 2003/08/22 23:02:35 dwd Exp $
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
import java.io.InvalidObjectException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import javax.swing.*;
import javax.swing.filechooser.*;
import java.awt.event.*;

import gov.noaa.pmel.util.SimpleFileFilter;

/**
 * Run-time and stand-alone editor to modify a <code>PanelModel</code>.
 *
 * @author Donald Denbo
 * @version $Revision: 1.2 $, $Date: 2003/08/22 23:02:35 $
 * @since 3.0
 * @see PanelModelCustomizer
 **/
public class PanelModelEditor extends JFrame {
  private PanelModel panelModel_ = null;
  private static boolean isApp_ = false;
  private boolean throwClosing = true;
  private String openPath_ = null;
  private static String title_ = "Panel Model Editor";
  //
  private JFileChooser fileChooser_ = null;
  private JPanel pmcPanel = new JPanel();
  private JPanel buttonPanel = new JPanel();
  private JMenuBar menuBar = new JMenuBar();
  private JButton okButton = new JButton();
  private PanelModelCustomizer pmc = new PanelModelCustomizer();
  private BorderLayout borderLayout1 = new BorderLayout();
  private JMenu fileMenu = new JMenu();
  private JMenuItem openFileMI = new JMenuItem();
  private JMenuItem saveFileMI = new JMenuItem();
  private JMenuItem exitFileMI = new JMenuItem();
  private JMenuItem saveAsFileMI = new JMenuItem();
  private JMenuItem newFileMI = new JMenuItem();

  /**
   * <code>PanelModelEditor</code> constructor.
   * @param pModel PanelModel
   */
  public PanelModelEditor(PanelModel pModel) {
    this();
    setPanelModel(pModel);
  }

  /**
   * Default constructor.
   */
  public PanelModelEditor() {
    try {
      jbInit();
      pack();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Main method for stand-alone application to create/modify <code>PanelModel</code>s
   * that have been serialized using <code>XMLEncoder</code>.
   * @param args Arguement String
   * @see java.beans.XMLEncoder
   */
  public static void main(String[] args) {
    isApp_ = true;
    PanelModelEditor pme = new PanelModelEditor(new PanelModel());
    pme.setTitle(title_);
    pme.setVisible(true);
  }

  private void jbInit() throws Exception {
    okButton.setText("OK");
    okButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        okButton_actionPerformed(e);
      }
    });
    pmcPanel.setLayout(borderLayout1);
    fileMenu.setText("File");
    openFileMI.setActionCommand("Open");
    openFileMI.setText("Open...");
    openFileMI.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        openFileMI_actionPerformed(e);
      }
    });
    saveFileMI.setActionCommand("Save");
    saveFileMI.setText("Save");
    saveFileMI.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        saveFileMI_actionPerformed(e);
      }
    });
    exitFileMI.setText("Exit");
    exitFileMI.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        exitFileMI_actionPerformed(e);
      }
    });
    saveAsFileMI.setActionCommand("SaveAs");
    saveAsFileMI.setText("Save As...");
    saveAsFileMI.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        saveAsFileMI_actionPerformed(e);
      }
    });
    if(isApp_)this.setJMenuBar(menuBar);
    newFileMI.setText("New");
    newFileMI.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        newFileMI_actionPerformed(e);
      }
    });
    this.getContentPane().add(pmcPanel, BorderLayout.CENTER);
    if(!isApp_) this.getContentPane().add(buttonPanel,  BorderLayout.SOUTH);
    buttonPanel.add(okButton, null);
    pmcPanel.add(pmc,  BorderLayout.CENTER);
    menuBar.add(fileMenu);
    fileMenu.add(newFileMI);
    fileMenu.addSeparator();
    fileMenu.add(openFileMI);
    fileMenu.add(saveFileMI);
    fileMenu.add(saveAsFileMI);
    fileMenu.addSeparator();
    fileMenu.add(exitFileMI);

    addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        if(throwClosing) pmf_windowClosing(e);
      }
    });
    Dimension dim = pmc.getPreferredSize();
    setSize(new Dimension(473, 354));
    //Center the window
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension frameSize = getSize();
    if (frameSize.height > screenSize.height) {
      frameSize.height = screenSize.height;
    }
    if (frameSize.width > screenSize.width) {
      frameSize.width = screenSize.width;
    }
    setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
  }

  void pmf_windowClosing(WindowEvent e) {
    if(Page.DEBUG) System.out.println("windowClosing");
    throwClosing = false;
    setVisible(false);
    if(panelModel_ != null) panelModel_.setBatch(false);
    dispose();
    if(isApp_) System.exit(0);
  }

  void okButton_actionPerformed(ActionEvent e) {
    throwClosing = false;
    pmf_windowClosing(null);
  }

  /**
   * Get <code>PanelModel</code>.
   * @return PanelModel
   */
  public PanelModel getPanelModel() {
    return panelModel_;
  }

  /**
   * Set <code>PanelModel</code>.
   * @param panelModel PanelModel
   */
  public void setPanelModel(PanelModel panelModel) {
    panelModel_ = panelModel;
    if(panelModel_ != null) {
      panelModel_.setBatch(true);
      pmc.setObject(panelModel_);
    }
  }

  void openFileMI_actionPerformed(ActionEvent e) {
    PanelModel pModel = null;
    File file;
    String ext;
    String[] xmlFile = new String[]{".xml"};
    SimpleFileFilter xmlFilt = new SimpleFileFilter(xmlFile, "XML Serialized Bean");

    if(fileChooser_ == null) {
      fileChooser_ = new JFileChooser();
    }
    fileChooser_.setFileFilter(fileChooser_.getAcceptAllFileFilter());
    fileChooser_.resetChoosableFileFilters();
    fileChooser_.addChoosableFileFilter(xmlFilt);
    fileChooser_.setFileFilter(xmlFilt);

    int result = fileChooser_.showOpenDialog(this);
    if(result == JFileChooser.APPROVE_OPTION) {
      file = fileChooser_.getSelectedFile();
      openPath_ = file.getPath();
      setTitle(title_ + ": " + openPath_);
      try {
        pModel = PanelModel.loadFromXML(new BufferedInputStream(
                           new FileInputStream(openPath_)));
        setPanelModel(pModel);
       } catch (FileNotFoundException fnfe) {
        JOptionPane.showMessageDialog(this, "Error openning file",
                                      "File Open Error", JOptionPane.ERROR_MESSAGE);
      } catch (InvalidObjectException ioe) {
        JOptionPane.showMessageDialog(this, "File does not contain a PanelModel",
                                       "PanelModel Not Found",
                                       JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  void saveAsFileMI_actionPerformed(ActionEvent e) {
    File file;
    FileFilter filt;
    String ext;
    String outpath;
    String[] xmlFile = new String[]{".xml"};
    SimpleFileFilter xmlFilt = new SimpleFileFilter(xmlFile, "XML Serialized Bean");

    if(fileChooser_ == null) {
      fileChooser_ = new JFileChooser();
    }
    fileChooser_.setFileFilter(fileChooser_.getAcceptAllFileFilter());
    fileChooser_.resetChoosableFileFilters();
    fileChooser_.addChoosableFileFilter(xmlFilt);
    fileChooser_.setFileFilter(xmlFilt);

    int result = fileChooser_.showSaveDialog(this);
    if(result == JFileChooser.APPROVE_OPTION) {
      file = fileChooser_.getSelectedFile();
      filt = fileChooser_.getFileFilter();
      outpath = file.getPath();
      if(filt instanceof SimpleFileFilter) {
        ext = ((SimpleFileFilter)filt).getExtension();
        String name = file.getName().toLowerCase();
        if(!name.endsWith(ext)) {
          outpath += ext;
        }
      }
      if(new File(outpath).exists()) {
        JOptionPane.showMessageDialog(this, "File already exists, rename and try again",
                                      "File Save Error", JOptionPane.ERROR_MESSAGE);
        return;
      }
      try {
        panelModel_.saveToXML(new BufferedOutputStream(
            new FileOutputStream(outpath, false)));
      } catch (FileNotFoundException fnfe) {
        JOptionPane.showMessageDialog(this, "Error creating file, rename and try again",
                                      "File Save Error", JOptionPane.ERROR_MESSAGE);
        return;
      }
    }
  }

  void saveFileMI_actionPerformed(ActionEvent e) {
    if(openPath_ == null) return;
    try {
      panelModel_.saveToXML(new BufferedOutputStream(
          new FileOutputStream(openPath_, false)));
    } catch (FileNotFoundException fnfe) {
      JOptionPane.showMessageDialog(this, "Error creating file, rename and try again",
                                    "File Save Error", JOptionPane.ERROR_MESSAGE);
      return;
    }
  }

  void exitFileMI_actionPerformed(ActionEvent e) {
    throwClosing = false;
    pmf_windowClosing(null);
  }

  void newFileMI_actionPerformed(ActionEvent e) {
    setPanelModel(new PanelModel());
  }
}
