/*
 * $Id: JClassTree.java,v 1.17 2003/08/22 23:02:39 dwd Exp $
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

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.InputEvent;
import java.util.Enumeration;
import java.util.List;

import gov.noaa.pmel.util.Debug;

import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.Layer;
import gov.noaa.pmel.sgt.LayerChild;
import gov.noaa.pmel.sgt.SGLabel;
import gov.noaa.pmel.sgt.Graph;
import gov.noaa.pmel.sgt.CartesianGraph;
import gov.noaa.pmel.sgt.CartesianRenderer;
import gov.noaa.pmel.sgt.PointCartesianRenderer;
import gov.noaa.pmel.sgt.LineCartesianRenderer;
import gov.noaa.pmel.sgt.GridCartesianRenderer;
import gov.noaa.pmel.sgt.VectorCartesianRenderer;
import gov.noaa.pmel.sgt.Axis;
import gov.noaa.pmel.sgt.SpaceAxis;
import gov.noaa.pmel.sgt.PlainAxis;
import gov.noaa.pmel.sgt.TimeAxis;
import gov.noaa.pmel.sgt.Transform;
import gov.noaa.pmel.sgt.PointAttribute;
import gov.noaa.pmel.sgt.LineAttribute;
import gov.noaa.pmel.sgt.GridAttribute;
import gov.noaa.pmel.sgt.VectorAttribute;
import gov.noaa.pmel.sgt.Logo;

import gov.noaa.pmel.sgt.beans.Panel;
import gov.noaa.pmel.sgt.beans.DataGroupLayer;

import gov.noaa.pmel.sgt.swing.prop.LineAttributeDialog;
import gov.noaa.pmel.sgt.swing.prop.GridAttributeDialog;
import gov.noaa.pmel.sgt.swing.prop.SGLabelDialog;
import gov.noaa.pmel.sgt.swing.prop.TimeAxisDialog;
import gov.noaa.pmel.sgt.swing.prop.SpaceAxisDialog;
import gov.noaa.pmel.sgt.swing.prop.PointAttributeDialog;
import gov.noaa.pmel.sgt.swing.prop.LogoDialog;
import gov.noaa.pmel.sgt.swing.prop.VectorAttributeDialog;

/**
 * <code>JClassTree</code> displays the <code>sgt</code> object tree
 * in a <code>JDialog</code> using a <code>JTree</code>.  Many
 * <code>sgt</code> classes can be selected from the
 * <code>JTree</code> to edit their properties.  They include:
 * {@link gov.noaa.pmel.sgt.SGLabel SGLabel},
 * {@link gov.noaa.pmel.sgt.LineAttribute LineAttribute},
 * {@link gov.noaa.pmel.sgt.GridAttribute GridAttribute},
 * {@link gov.noaa.pmel.sgt.CartesianGraph CartesianGraph},
 * {@link gov.noaa.pmel.sgt.TimeAxis TimeAxis},
 * {@link gov.noaa.pmel.sgt.SpaceAxis SpaceAxis},
 * {@link gov.noaa.pmel.sgt.PointAttribute PointAttribute}, and
 * {@link gov.noaa.pmel.sgt.Logo Logo}.
 *
 * @author Donald Denbo
 * @version $Revision: 1.17 $, $Date: 2003/08/22 23:02:39 $
 * @since 2.0
 * @see SGLabelDialog
 * @see LineAttributeDialog
 * @see GridAttributeDialog
 * @see TimeAxisDialog
 * @see SpaceAxisDialog
 * @see PointAttributeDialog
 * @see LogoDialog
 */
public class JClassTree extends javax.swing.JDialog {
  private JPane pane_;

  private static SGLabelDialog sg_;
  private static LineAttributeDialog la_;
  private static GridAttributeDialog ga_;
  private static VectorAttributeDialog va_;
  private static TimeAxisDialog ta_;
  private static SpaceAxisDialog pa_;
  private static PointAttributeDialog pta_;
  private static LogoDialog lg_;
  /**
   * Default constructor.
   */
  public JClassTree() {
    getContentPane().setLayout(new BorderLayout(0,0));
    setVisible(false);
    setSize(405,362);
    jScrollPane1 = new javax.swing.JScrollPane();
    jScrollPane1.setOpaque(true);
    getContentPane().add(BorderLayout.CENTER, jScrollPane1);
    treeView_ = new javax.swing.JTree();
    treeView_.setBounds(0,0,402,324);
    treeView_.setFont(new Font("Dialog", Font.PLAIN, 12));
    treeView_.setBackground(java.awt.Color.white);
    jScrollPane1.getViewport().add(treeView_);
    treeControlsPanel = new javax.swing.JPanel();
    treeControlsPanel.setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
    treeControlsPanel.setFont(new Font("Dialog", Font.PLAIN, 12));
    treeControlsPanel.setForeground(java.awt.Color.black);
    treeControlsPanel.setBackground(new java.awt.Color(204,204,204));
    getContentPane().add(BorderLayout.SOUTH, treeControlsPanel);
    expandButton = new javax.swing.JButton();
    expandButton.setText("Expand All");
    expandButton.setActionCommand("Expand All");
    expandButton.setFont(new Font("Dialog", Font.BOLD, 12));
    expandButton.setBackground(new java.awt.Color(204,204,204));
    treeControlsPanel.add(expandButton);
    collapseButton = new javax.swing.JButton();
    collapseButton.setText("Collapse All");
    collapseButton.setActionCommand("Collapse All");
    collapseButton.setFont(new Font("Dialog", Font.BOLD, 12));
    collapseButton.setBackground(new java.awt.Color(204,204,204));
    treeControlsPanel.add(collapseButton);
    editButton = new javax.swing.JButton();
    editButton.setText("Edit Selected");
    editButton.setActionCommand("Edit Selected");
    editButton.setFont(new Font("Dialog", Font.BOLD, 12));
    editButton.setBackground(new java.awt.Color(204,204,204));
    treeControlsPanel.add(editButton);
    cancelButton = new javax.swing.JButton();
    cancelButton.setText("Close");
    cancelButton.setActionCommand("Close");
    cancelButton.setFont(new Font("Dialog", Font.BOLD, 12));
    cancelButton.setBackground(new java.awt.Color(204,204,204));
    treeControlsPanel.add(cancelButton);
    setTitle("Class View");

    SymWindow aSymWindow = new SymWindow();
    this.addWindowListener(aSymWindow);
    SymAction lSymAction = new SymAction();
    expandButton.addActionListener(lSymAction);
    collapseButton.addActionListener(lSymAction);
    editButton.addActionListener(lSymAction);
    cancelButton.addActionListener(lSymAction);

    MouseListener ml = new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
          int selRow = treeView_.getRowForLocation(e.getX(),
                                                   e.getY());
          TreePath selPath = treeView_.getPathForLocation(e.getX(),
                                                          e.getY());
          if(selRow != -1) {
            if((e.getClickCount() == 2) ||
               ((e.getModifiers()&InputEvent.BUTTON3_MASK) != 0)) {
              doubleClick(selRow, selPath);
            }
          }
        }
      };
    treeView_.addMouseListener(ml);
    //

    if(System.getProperty("mrj.version") == null ||
       !UIManager.getSystemLookAndFeelClassName().equals(UIManager.getLookAndFeel().getClass().getName())) {
      Insets inset = new Insets(0,5,0,5);
      expandButton.setMargin(inset);
      collapseButton.setMargin(inset);
      editButton.setMargin(inset);
      cancelButton.setMargin(inset);
    }
  }

  void doubleClick(int selRow, TreePath selPath) {
    if(Debug.DEBUG) System.out.println("row " + selRow + " selected");
    Object[] objs = selPath.getPath();
    Object thing = ((DefaultMutableTreeNode)objs[objs.length-1]).getUserObject();
    showProperties(thing);
  }

  public JClassTree(String sTitle) {
    this();
    setTitle(sTitle);
  }
  /**
   * Make the dialog visible.
   */
  public void setVisible(boolean b) {
    if (b)
      setLocation(50, 50);
    super.setVisible(b);
  }

  static public void main(String[] args) {
    (new JClassTree()).setVisible(true);
  }

  public void addNotify() {
    // Record the size of the window prior to calling parents addNotify.
    Dimension d = getSize();
    Insets in = getInsets();

    super.addNotify();

    if (fComponentsAdjusted)
      return;
    // Adjust components according to the insets
    setSize(in.left + in.right + d.width, in.top + in.bottom + d.height);
    Component components[] = getContentPane().getComponents();
    for (int i = 0; i < components.length; i++)
      {
        Point p = components[i].getLocation();
        p.translate(in.left, in.top);
        components[i].setLocation(p);
      }
    fComponentsAdjusted = true;
  }

  // Used for addNotify check.
  private boolean fComponentsAdjusted = false;

  javax.swing.JScrollPane jScrollPane1 = new javax.swing.JScrollPane();
  javax.swing.JTree treeView_ = new javax.swing.JTree();
  javax.swing.JPanel treeControlsPanel = new javax.swing.JPanel();
  javax.swing.JButton expandButton = new javax.swing.JButton();
  javax.swing.JButton collapseButton = new javax.swing.JButton();
  javax.swing.JButton editButton = new javax.swing.JButton();
  javax.swing.JButton cancelButton = new javax.swing.JButton();

  class SymWindow extends java.awt.event.WindowAdapter {
    public void windowClosing(java.awt.event.WindowEvent event) {
      Object object = event.getSource();
      if (object == JClassTree.this)
        JClassTree_WindowClosing(event);
    }
  }

  void JClassTree_WindowClosing(java.awt.event.WindowEvent event) {
    setVisible(false); // hide the Frame
    //          dispose();           // free the system resources
    //          System.exit(0);    // close the application
  }

  class SymAction implements java.awt.event.ActionListener {
    public void actionPerformed(java.awt.event.ActionEvent event) {
      Object object = event.getSource();
      if (object == expandButton)
        expandButton_actionPerformed(event);
      else if (object == collapseButton)
        collapseButton_actionPerformed(event);
      else if (object == editButton)
        editButton_actionPerformed(event);
      else if (object == cancelButton)
        cancelButton_actionPerformed(event);
    }
  }

  void expandButton_actionPerformed(java.awt.event.ActionEvent event) {
    expandTree();
  }
  /**
   * Expand the tree to show all nodes.
   */
  public void expandTree() {
    int row=0;
    while(row < treeView_.getRowCount()) {
      if(treeView_.isCollapsed(row)) {
        treeView_.expandRow(row);
      }
      row++;
    }
  }

  void collapseButton_actionPerformed(java.awt.event.ActionEvent event) {
    treeView_.collapseRow(0);
  }

  void editButton_actionPerformed(java.awt.event.ActionEvent event) {
    Object[] objs = treeView_.getSelectionPath().getPath();
    showProperties(((DefaultMutableTreeNode)objs[objs.length-1]).getUserObject());
  }

  void cancelButton_actionPerformed(java.awt.event.ActionEvent event) {
    setVisible(false);
  }
  /**
   * Set the <code>JPane</code> for which the <code>JTree</code> is to
   * be built.
   */
  public void setJPane(JPane pane) {
    Layer ly;
    //
    DefaultTreeModel treeModel;
    DefaultMutableTreeNode paneNode;
    DefaultMutableTreeNode panelNode;
    DefaultMutableTreeNode layerNode;
    DefaultMutableTreeNode dataGroupNode;

    pane_ = pane;
    paneNode = new DefaultMutableTreeNode(pane_);
    treeModel = new DefaultTreeModel(paneNode);
    treeView_.setModel(treeModel);
    //
    Component[] comps = pane_.getComponents();
    for(int il=0; il < comps.length; il++) {
      if(comps[il] instanceof DataGroupLayer) {
        ly = (Layer)comps[il];
        dataGroupNode = new DefaultMutableTreeNode(ly);
        treeModel.insertNodeInto(dataGroupNode,
                                 paneNode,
                                 treeModel.getChildCount(paneNode));
        List lyList = ((DataGroupLayer)ly).getLayerList();
        for(int i=1; i < lyList.size(); i++) {
          ly = (Layer)lyList.get(i);
          layerNode = new DefaultMutableTreeNode(ly);
          treeModel.insertNodeInto(layerNode,
                                   dataGroupNode,
                                   treeModel.getChildCount(dataGroupNode));
          expandLayer(ly, dataGroupNode, treeModel);
        }
      } else if(comps[il] instanceof Layer) {
        ly = (Layer)comps[il];
        layerNode = new DefaultMutableTreeNode(ly);
        treeModel.insertNodeInto(layerNode,
                                 paneNode,
                                 treeModel.getChildCount(paneNode));
        expandLayer(ly, layerNode, treeModel);
      } else if(comps[il] instanceof Panel) {
        panelNode = new DefaultMutableTreeNode(comps[il]);
        treeModel.insertNodeInto(panelNode,
                                 paneNode,
                                 treeModel.getChildCount(paneNode));
        Component[] pcomps = ((Panel)comps[il]).getComponents();
        for(int pl=0; pl < pcomps.length; pl++) {
          if(pcomps[pl] instanceof DataGroupLayer) {
            ly = (Layer)pcomps[pl];
            dataGroupNode = new DefaultMutableTreeNode(ly);
            treeModel.insertNodeInto(dataGroupNode,
                                     paneNode,
                                     treeModel.getChildCount(paneNode));
            expandLayer(ly, dataGroupNode, treeModel);
            List lyList = ((DataGroupLayer)ly).getLayerList();
            for(int i=1; i < lyList.size(); i++) {
              ly = (Layer)lyList.get(i);
              layerNode = new DefaultMutableTreeNode(ly);
              treeModel.insertNodeInto(layerNode,
                                       dataGroupNode,
                                       treeModel.getChildCount(dataGroupNode));
              expandLayer(ly, layerNode, treeModel);
            }
          } else if(pcomps[pl] instanceof Layer) {
            ly = (Layer)pcomps[pl];
            layerNode = new DefaultMutableTreeNode(ly);
            treeModel.insertNodeInto(layerNode,
                                     panelNode,
                                     treeModel.getChildCount(panelNode));
            expandLayer(ly, layerNode, treeModel);
          }
        }
       } else {
        continue;
      }

      int row=0;
      while(row < treeView_.getRowCount()) {
        if(treeView_.isCollapsed(row)) {
          treeView_.expandRow(row);
        }
        row++;
      }
    }
  }

  private void expandLayer(Layer ly,
                           DefaultMutableTreeNode layerNode,
                           DefaultTreeModel treeModel) {
    LayerChild child;
    Graph graph;
    Axis axis;
    CartesianRenderer rend;
    LineAttribute attr;
    GridAttribute gattr;
    PointAttribute pattr;
    VectorAttribute vattr;
    //
    DefaultMutableTreeNode node;
    DefaultMutableTreeNode paneNode;
    DefaultMutableTreeNode childNode;
    DefaultMutableTreeNode graphNode;
    DefaultMutableTreeNode attrNode;
    DefaultMutableTreeNode gattrNode;
    DefaultMutableTreeNode pattrNode;
    DefaultMutableTreeNode axisNode;
    DefaultMutableTreeNode titleNode;

      String name, className;
      for(Enumeration childs = ly.childElements(); childs.hasMoreElements();) {
        child = (LayerChild)childs.nextElement();
        className = child.getClass().getName();
        name = className.substring(className.lastIndexOf(".")+1);
        childNode = new DefaultMutableTreeNode(child);
        treeModel.insertNodeInto(childNode,
                                 layerNode,
                                 treeModel.getChildCount(layerNode));
      }
      graph = ly.getGraph();
      if(graph == null) return;
      className = graph.getClass().getName();
      name = className.substring(className.lastIndexOf(".")+1);
      if(graph instanceof CartesianGraph) {
        graphNode = new DefaultMutableTreeNode(graph);
        treeModel.insertNodeInto(graphNode,
                                 layerNode,
                                 treeModel.getChildCount(layerNode));
        rend = ((CartesianGraph)graph).getRenderer();
        if(rend instanceof LineCartesianRenderer) {
          attr = (LineAttribute)((LineCartesianRenderer)rend).getAttribute();
          if(attr != null) {
            className = attr.getClass().getName();
            name = className.substring(className.lastIndexOf(".")+1);
            attrNode = new DefaultMutableTreeNode(attr);
            treeModel.insertNodeInto(attrNode,
                                     graphNode,
                                     treeModel.getChildCount(graphNode));
          }
        } else if(rend instanceof VectorCartesianRenderer) {
          vattr = (VectorAttribute)((VectorCartesianRenderer)rend).getAttribute();
          if(vattr != null) {
            className = vattr.getClass().getName();
            name = className.substring(className.lastIndexOf(".")+1);
            attrNode = new DefaultMutableTreeNode(vattr);
            treeModel.insertNodeInto(attrNode,
                                     graphNode,
                                     treeModel.getChildCount(graphNode));
          }
        } else if(rend instanceof GridCartesianRenderer) {
          gattr = (GridAttribute)((GridCartesianRenderer)rend).getAttribute();
          if(gattr != null) {
            className = gattr.getClass().getName();
            name = className.substring(className.lastIndexOf(".")+1);
            gattrNode = new DefaultMutableTreeNode(gattr);
            treeModel.insertNodeInto(gattrNode,
                                     graphNode,
                                     treeModel.getChildCount(graphNode));
          }
        } else if(rend instanceof PointCartesianRenderer) {
          pattr = (PointAttribute)((PointCartesianRenderer)rend).getAttribute();
          if(pattr != null) {
            className = pattr.getClass().getName();
            name = className.substring(className.lastIndexOf(".")+1);
            pattrNode = new DefaultMutableTreeNode(pattr);
            treeModel.insertNodeInto(pattrNode,
                                     graphNode,
                                     treeModel.getChildCount(graphNode));
          }
        }
        for(Enumeration axes = ((CartesianGraph)graph).xAxisElements();
            axes.hasMoreElements();) {
          axis = (Axis)axes.nextElement();
          className = axis.getClass().getName();
          name = className.substring(className.lastIndexOf(".")+1);
          if(axis instanceof SpaceAxis) {
            axisNode = new DefaultMutableTreeNode(axis);
            treeModel.insertNodeInto(axisNode,
                                     graphNode,
                                     treeModel.getChildCount(graphNode));
            SGLabel title = axis.getTitle();
            if(title != null) {
              titleNode = new DefaultMutableTreeNode(title);
              treeModel.insertNodeInto(titleNode,
                                       axisNode,
                                       treeModel.getChildCount(axisNode));
            }
          } else { // not a SpaceAxis
            axisNode = new DefaultMutableTreeNode(axis);
            treeModel.insertNodeInto(axisNode,
                                     graphNode,
                                     treeModel.getChildCount(graphNode));
          }
        }
        for(Enumeration axes = ((CartesianGraph)graph).yAxisElements();
            axes.hasMoreElements();) {
          axis = (Axis)axes.nextElement();
          className = axis.getClass().getName();
          name = className.substring(className.lastIndexOf(".")+1);
          if(axis instanceof SpaceAxis) {
            axisNode = new DefaultMutableTreeNode(axis);
            treeModel.insertNodeInto(axisNode,
                                     graphNode,
                                     treeModel.getChildCount(graphNode));
            SGLabel title = axis.getTitle();
            if(title != null) {
              titleNode = new DefaultMutableTreeNode(title);
              treeModel.insertNodeInto(titleNode,
                                       axisNode,
                                       treeModel.getChildCount(axisNode));
            }
          } else { // not a SpaceAxis
            axisNode = new DefaultMutableTreeNode(axis);
            treeModel.insertNodeInto(axisNode,
                                     graphNode,
                                     treeModel.getChildCount(graphNode));
          }
        }
      } else {  // not a CartesianGraph
        graphNode = new DefaultMutableTreeNode(graph);
        treeModel.insertNodeInto(graphNode,
                                 layerNode,
                                 treeModel.getChildCount(layerNode));
      }
    }

  void showProperties(Object obj) {
    if(obj instanceof SGLabel) {
      if(sg_ == (SGLabelDialog) null) {
        //
        // create the SGLabel  dialog
        //
        sg_ = new SGLabelDialog();
      }
      sg_.setSGLabel((SGLabel) obj, pane_);
      if(!sg_.isShowing())
        sg_.setVisible(true);
    } else if(obj instanceof Logo) {
      if(lg_ == null) {
        //
        // create logo dialog
        //
        lg_ = new LogoDialog();
      }
      lg_.setLogo((Logo) obj, pane_);
      if(!lg_.isShowing())
        lg_.setVisible(true);
    } else if(obj instanceof PlainAxis) {
      if(pa_ == (SpaceAxisDialog) null) {
        //
        // create the PlainAxis  dialog
        //
        pa_ = new SpaceAxisDialog();
      }
      pa_.setSpaceAxis((PlainAxis) obj, pane_);
      if(!pa_.isShowing())
        pa_.show();
    } else if(obj instanceof TimeAxis) {
      if(ta_ == (TimeAxisDialog) null) {
        //
        // create the TimeAxis  dialog
        //
        ta_ = new TimeAxisDialog();
      }
      ta_.setTimeAxis((TimeAxis) obj, pane_);
      if(!ta_.isShowing())
        ta_.show();
    } else if(obj instanceof LineAttribute) {
      if(la_ == (LineAttributeDialog) null) {
        //
        // create the LineAttr  dialog
        //
        la_ = new LineAttributeDialog();
        la_.setJPane(pane_);
      }
      la_.setLineAttribute((LineAttribute)obj);
      if(!la_.isShowing())
        la_.setVisible(true);
    } else if(obj instanceof VectorAttribute) {
      if(va_ == (VectorAttributeDialog) null) {
        //
        // create the LineAttr  dialog
        //
        va_ = new VectorAttributeDialog();
        va_.setJPane(pane_);
      }
      va_.setVectorAttribute((VectorAttribute)obj);
      if(!va_.isShowing())
        va_.setVisible(true);
    } else if (obj instanceof GridAttribute) {
      if(ga_ == null) {
        //
        // create a new dialog
        //
        ga_ = new GridAttributeDialog();
      }
      ga_.setGridAttribute((GridAttribute)obj);
      if(!ga_.isShowing())
        ga_.setVisible(true);
    } else if (obj instanceof CartesianGraph) {
      CartesianRenderer rend = ((CartesianGraph)obj).getRenderer();
      if( rend instanceof GridCartesianRenderer) {
        if(ga_ == null) {
          ga_ = new GridAttributeDialog();
        }
        ga_.setGridCartesianRenderer((GridCartesianRenderer)rend);
        if(!ga_.isShowing())
          ga_.setVisible(true);
      }
    } else if (obj instanceof PointAttribute) {
      if(pta_ == (PointAttributeDialog) null) {
        pta_ = new PointAttributeDialog();
      }
      pta_.setPointAttribute((PointAttribute) obj, pane_);
      if(!pta_.isShowing())
        pta_.show();
    }
  }
  private Frame getFrame() {
    Container theFrame = this;
    do {
      theFrame = theFrame.getParent();
    } while ((theFrame != null) && !(theFrame instanceof Frame));
    if (theFrame == null)
      theFrame = new Frame();
    return (Frame)theFrame;
  }
}
