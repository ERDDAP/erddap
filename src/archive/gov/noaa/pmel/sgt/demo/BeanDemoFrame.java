/*
 * $Id: BeanDemoFrame.java,v 1.7 2003/09/16 19:05:41 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */
package gov.noaa.pmel.sgt.demo;

import java.awt.*;
import java.awt.print.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

import gov.noaa.pmel.sgt.beans.*;
import gov.noaa.pmel.sgt.dm.*;
import gov.noaa.pmel.sgt.AbstractPane;
import gov.noaa.pmel.sgt.LineAttribute;
import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.ColorMap;
import gov.noaa.pmel.sgt.IndexedColorMap;
import gov.noaa.pmel.sgt.LinearTransform;
import gov.noaa.pmel.sgt.GridAttribute;
import gov.noaa.pmel.sgt.Attribute;
import gov.noaa.pmel.sgt.swing.JClassTree;
import gov.noaa.pmel.util.*;
import gov.noaa.pmel.swing.JSystemPropertiesDialog;

/**
 *
 * @author Donald Denbo
 * @version $Revision: 1.7 $, $Date: 2003/09/16 19:05:41 $
 * @since 3.0
 **/
class BeanDemoFrame extends JFrame {
  private Page page = new Page();
  private PanelModel panelModel = new PanelModel();
  private DataModel dataModel = new DataModel();
  private SGTData timeSeries;
  private Attribute timeSeriesAttr;
  private SGTData grid;
  private Attribute gridAttr;
  private SGTData line;
  private Attribute lineAttr;

  private JPanel contentPane;
  private JMenuBar jMenuBar1 = new JMenuBar();
  private JMenu jMenuFile = new JMenu();
  private JMenuItem jMenuFileExit = new JMenuItem();
  private JMenu jMenuHelp = new JMenu();
  private JMenuItem jMenuHelpAbout = new JMenuItem();
  private BorderLayout borderLayout1 = new BorderLayout();
  private JMenu jMenuView = new JMenu();
  private JMenuItem jMenuViewReset = new JMenuItem();
  private JMenuItem jMenuViewTree = new JMenuItem();
  private JPanel pagePanel = new JPanel();
  private BorderLayout borderLayout2 = new BorderLayout();
  private JMenuItem jMenuHelpProps = new JMenuItem();
  private JMenu jMenuEdit = new JMenu();
  private JMenuItem jMenuEditData = new JMenuItem();
  private JMenuItem jMenuEditPM = new JMenuItem();
  private JMenuItem jMenuFilePrint = new JMenuItem();
  private JMenuItem jMenuFilePage = new JMenuItem();

  private PageFormat pageFormat = PrinterJob.getPrinterJob().defaultPage();

  //Construct the frame
  public BeanDemoFrame() {
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    page.getJPane().setBackground(Color.lightGray);
    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    createGraphic();
    pack();
  }
  //Component initialization
  private void jbInit() throws Exception  {
    //setIconImage(Toolkit.getDefaultToolkit().createImage(BeanDemoFrame.class.getResource("[Your Icon]")));
    contentPane = (JPanel) this.getContentPane();
    contentPane.setLayout(borderLayout1);
//    this.setSize(new Dimension(501, 450));
    this.setTitle("SGT Bean Demo");
    jMenuFile.setText("File");
    jMenuFileExit.setText("Exit");
    jMenuFileExit.addActionListener(new ActionListener()  {
      public void actionPerformed(ActionEvent e) {
        jMenuFileExit_actionPerformed(e);
      }
    });
    jMenuHelp.setText("Help");
    jMenuHelpAbout.setText("About");
    jMenuHelpAbout.addActionListener(new ActionListener()  {
      public void actionPerformed(ActionEvent e) {
        jMenuHelpAbout_actionPerformed(e);
      }
    });
    jMenuView.setText("View");
    jMenuViewReset.setText("Reset All Zoom");
    jMenuViewReset.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        jMenuViewReset_actionPerformed(e);
      }
    });
    jMenuViewTree.setText("Class Tree...");
    jMenuViewTree.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        jMenuViewTree_actionPerformed(e);
      }
    });
    pagePanel.setLayout(borderLayout2);
    jMenuHelpProps.setText("System Properties...");
    jMenuHelpProps.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        jMenuHelpProps_actionPerformed(e);
      }
    });
    jMenuEdit.setText("Edit");
    jMenuEditData.setText("Add Data...");
    jMenuEditData.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        jMenuEditData_actionPerformed(e);
      }
    });
    jMenuEditPM.setText("PanelModel");
    jMenuEditPM.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        jMenuEditPM_actionPerformed(e);
      }
    });
    jMenuFilePrint.setText("Print...");
    jMenuFilePrint.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        jMenuFilePrint_actionPerformed(e);
      }
    });
    jMenuFilePage.setText("Page Layout...");
    jMenuFilePage.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        jMenuFilePage_actionPerformed(e);
      }
    });
    jMenuFile.add(jMenuFilePage);
    jMenuFile.add(jMenuFilePrint);
    jMenuFile.addSeparator();
    jMenuFile.add(jMenuFileExit);
    jMenuHelp.add(jMenuHelpProps);
    jMenuHelp.add(jMenuHelpAbout);
    jMenuBar1.add(jMenuFile);
    jMenuBar1.add(jMenuEdit);
    jMenuBar1.add(jMenuView);
    jMenuBar1.add(jMenuHelp);
    jMenuView.add(jMenuViewReset);
    jMenuView.add(jMenuViewTree);
    contentPane.add(pagePanel, BorderLayout.CENTER);
    jMenuEdit.add(jMenuEditData);
    jMenuEdit.add(jMenuEditPM);
    this.setJMenuBar(jMenuBar1);
  }
  //File | Exit action performed
  private void jMenuFileExit_actionPerformed(ActionEvent e) {
    System.exit(0);
  }
  //Help | About action performed
  private void jMenuHelpAbout_actionPerformed(ActionEvent e) {
    JOptionPane.showMessageDialog(this, "BeanDemo: SGT version 3.0",
                                  "About BeanDemo",
                                  JOptionPane.INFORMATION_MESSAGE);
  }
  //Overridden so we can exit when window is closed
  protected void processWindowEvent(WindowEvent e) {
    super.processWindowEvent(e);
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      jMenuFileExit_actionPerformed(null);
    }
  }

  private void createGraphic() {
    pagePanel.add(page, BorderLayout.CENTER);
    page.setDataModel(dataModel);
    try {
      panelModel = PanelModel.loadFromXML(getClass().getResource("BeanDemoPanelModel.xml").openStream());
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    page.setPanelModel(panelModel);
    PanelHolder ul = panelModel.findPanelHolder("UpperLeft");
    DataGroup uldg = ul.findDataGroup("Grid");
    Legend ullg = ul.findLegend("ColorKey");
    createGrid();
    dataModel.addData(grid, gridAttr, ul, uldg, ullg);

    PanelHolder ur = panelModel.findPanelHolder("UpperRight");
    DataGroup urdg = ur.findDataGroup("Random");
    createLine();
    dataModel.addData(line, lineAttr, ur, urdg, null);

    PanelHolder bt = panelModel.findPanelHolder("Bottom");
    DataGroup btdg = bt.findDataGroup("TimeSeries");
    createTimeSeries();
    dataModel.addData(timeSeries, timeSeriesAttr, bt, btdg, null);
    // get datasets

  }

  private void createTimeSeries() {
    int dir = TestData.TIME_SERIES;
    int type = TestData.RANDOM;

    GeoDate start = null;
    GeoDate end = null;
    //2011-12-15 Bob Simons changed spaces to 'T's
    String format = "yyyy-MM-dd'T'HH:mm";
    String min1 = "2001-02-12'T'00:00";
    String max1 = "2001-06-10'T'00:00";
    try {
      start = new GeoDate(min1, format);
      end = new GeoDate(max1, format);
    } catch (IllegalTimeValue itf) {
      String message = "Illegal time string " + "'" + min1 + "' or '" + max1 + "'" +
                       "\nshould be of the form " + format;
      JOptionPane.showMessageDialog(this, message,
                                    "Error in Time Value", JOptionPane.ERROR_MESSAGE);
      return;
    }

    float delta = 2.0f;

    float amp = 1.0f;
    float off = 0.0f;
    float per = 5.0f;
    TestData td = new TestData(dir, new TimeRange(start, end), delta,
                               type, amp, off, per);
    timeSeries = td.getSGTData();
    timeSeriesAttr = new LineAttribute(LineAttribute.SOLID, Color.blue.brighter());
  }

  void createLine() {
    int dir = TestData.X_SERIES;
    int type = TestData.RANDOM;
    double min = 0.0;
    double max = 10.0;
    double delta = 0.5;
    Range2D range = new Range2D(min, max, delta);

    float amp = 1.0f;
    float off = 0.0f;
    float per = 5.0f;

    TestData td = new TestData(dir, range, type, amp, off, per);
    line = td.getSGTData();
    lineAttr = new LineAttribute(LineAttribute.SOLID, Color.blue);
  }

  void createGrid() {
    ColorMap cmap;
    int[] red =
    {  0,  0,  0,  0,  0,  0,  0,  0,
       0,  0,  0,  0,  0,  0,  0,  0,
       0,  0,  0,  0,  0,  0,  0,  0,
       0,  7, 23, 39, 55, 71, 87,103,
       119,135,151,167,183,199,215,231,
       247,255,255,255,255,255,255,255,
       255,255,255,255,255,255,255,255,
       255,246,228,211,193,175,158,140};
    int[] green =
    {  0,  0,  0,  0,  0,  0,  0,  0,
       0, 11, 27, 43, 59, 75, 91,107,
       123,139,155,171,187,203,219,235,
       251,255,255,255,255,255,255,255,
       255,255,255,255,255,255,255,255,
       255,247,231,215,199,183,167,151,
       135,119,103, 87, 71, 55, 39, 23,
       7,  0,  0,  0,  0,  0,  0,  0};
    int[] blue =
    {  0,143,159,175,191,207,223,239,
       255,255,255,255,255,255,255,255,
       255,255,255,255,255,255,255,255,
       255,247,231,215,199,183,167,151,
       135,119,103, 87, 71, 55, 39, 23,
       7,  0,  0,  0,  0,  0,  0,  0,
       0,  0,  0,  0,  0,  0,  0,  0,
       0,  0,  0,  0,  0,  0,  0,  0};

    int dir = TestData.XY_GRID;
    int type = TestData.SINE;
    double min1 = 0.0;
    double max1 = 1.0;
    double delta1 = 0.02;
    Range2D range1 = new Range2D(min1, max1, delta1);

    double min2 = 0.0;
    double max2 = 1.0;
    double delta2 = 0.02;
    Range2D range2 = new Range2D(min2, max2, delta2);

    float amp = 0.5f;
    float off = 0.5f;
    float per = 0.2f;

    TestData td = new TestData(dir, range1, range2, type, amp, off, per);
    grid = td.getSGTData();
    cmap = new IndexedColorMap(red, green, blue);
    LinearTransform ctrans =
      new LinearTransform(0.0, (double)red.length, 0.0, 1.0);
    ((IndexedColorMap)cmap).setTransform(ctrans);
    gridAttr = new GridAttribute(GridAttribute.RASTER, cmap);
  }

  void jMenuViewReset_actionPerformed(ActionEvent e) {
    page.resetZoom();
  }

  void jMenuViewTree_actionPerformed(ActionEvent e) {
    JClassTree ct = new JClassTree();
    ct.setModal(false);
    ct.setJPane(page.getJPane());
    ct.show();
  }

  void jMenuEditData_actionPerformed(ActionEvent e) {
    AddDataFrame adf = new AddDataFrame(page);
    adf.setVisible(true);
  }

  void jMenuEditPM_actionPerformed(ActionEvent e) {
    PanelModelEditor pme = new PanelModelEditor(panelModel);
    pme.setVisible(true);
  }

  void jMenuHelpProps_actionPerformed(ActionEvent e) {
    JSystemPropertiesDialog sysProps =
      new JSystemPropertiesDialog(this, "System Properties", false);
    sysProps.show();
  }

  void jMenuFilePrint_actionPerformed(ActionEvent e) {
    Color saveColor;
    JPane pane = page.getJPane();

    PrinterJob printJob = PrinterJob.getPrinterJob();
    printJob.setPrintable(page, pageFormat);
    printJob.setJobName("BeanDemo");
    if(printJob.printDialog()) {
      try {
        RepaintManager currentManager = RepaintManager.currentManager(pane);
        currentManager.setDoubleBufferingEnabled(false);
        printJob.print();
        currentManager.setDoubleBufferingEnabled(true);
      } catch (PrinterException pe) {
        System.out.println("Error printing: " + pe);
      }
    }
  }

  void jMenuFilePage_actionPerformed(ActionEvent e) {
    PrinterJob pj = PrinterJob.getPrinterJob();
    pageFormat = pj.pageDialog(pageFormat);
  }
}
