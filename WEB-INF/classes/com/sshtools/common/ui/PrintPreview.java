/*
 *  SSHTools - Java SSH2 API
 *
 *  Copyright (C) 2002-2003 Lee David Painter and Contributors.
 *
 *  Contributions made by:
 *
 *  Brett Smith
 *  Richard Pernavas
 *  Erwin Bolwidt
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Library General Public License
 *  as published by the Free Software Foundation; either version 2 of
 *  the License, or (at your option) any later version.
 *
 *  You may also distribute it and/or modify it under the terms of the
 *  Apache style J2SSH Software License. A copy of which should have
 *  been provided with the distribution.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  License document supplied with your distribution for more details.
 *
 */
package com.sshtools.common.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.print.*;

import javax.swing.*;
import javax.swing.border.*;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.13 $
 */
public class PrintPreview extends JPanel implements ActionListener, Runnable {
    /**  */
    protected PrinterJob job;

    /**  */
    protected JButton setup;

    /**  */
    protected JComboBox scale;

    /**  */
    protected int pageWidth;

    /**  */
    protected Printable printable;

    /**  */
    protected PreviewContainer previewer;

    /**  */
    protected PageFormat pageFormat;

    /**  */
    protected int pageHeight;

    /**  */
    protected JScrollPane scrollpane;

    /**
* Creates a new PrintPreview object.
*
* @param printable
* @param pageFormat
*/
    public PrintPreview(Printable printable, PageFormat pageFormat) {
        super(new BorderLayout());
        this.printable = printable;
        job = PrinterJob.getPrinterJob();
        this.pageFormat = pageFormat;

        JPanel jpanel = new JPanel(new FlowLayout(0, 2, 2));
        setup = new JButton("Setup");
        setup.addActionListener(this);
        setup.setMnemonic('p');
        jpanel.add(setup);

        String[] as = { "10 %", "25 %", "50 %", "75 %", "100 %", "200 %" };
        scale = new JComboBox(as);
        scale.setSelectedItem(as[2]);
        scale.addActionListener(this);
        scale.setEditable(true);
        jpanel.add(scale);
        add(jpanel, BorderLayout.NORTH);
        previewer = new PreviewContainer();

        if ((pageFormat.getHeight() == 0.0D) ||
                (pageFormat.getWidth() == 0.0D)) {
            System.err.println("Unable to determine default page size");

            return;
        }

        pageWidth = (int) pageFormat.getWidth();
        pageHeight = (int) pageFormat.getHeight();

        byte byte0 = 50;
        int i = (pageWidth * byte0) / 100;
        int j = (pageHeight * byte0) / 100;
        int k = 0;

        try {
            do {
                BufferedImage bufferedimage = new BufferedImage(pageWidth,
                        pageHeight, 1);
                Graphics g = bufferedimage.getGraphics();
                g.setColor(Color.white);
                g.fillRect(0, 0, pageWidth, pageHeight);

                if (printable.print(g, pageFormat, k) != 0) {
                    break;
                }

                PagePreview pagepreview = new PagePreview(i, j, bufferedimage);
                previewer.add(pagepreview);
                k++;
            } while (true);
        } catch (PrinterException printerexception) {
            printerexception.printStackTrace();
            System.err.println("Printing error: " +
                printerexception.toString());
        }

        scrollpane = new JScrollPane(previewer);
        add(scrollpane, BorderLayout.CENTER);
    }

    /**
*
*
* @return
*/
    public PageFormat getPageFormat() {
        return pageFormat;
    }

    private void setup() {
        pageFormat = job.pageDialog(pageFormat);
        pageWidth = (int) pageFormat.getWidth();
        pageHeight = (int) pageFormat.getHeight();

        Thread thread = new Thread(this);
        thread.start();
    }

    /**
*
*/
    public void run() {
        String s = scale.getSelectedItem().toString();
        scrollpane.setViewportView(new JLabel("Resizing"));

        if (s.endsWith("%")) {
            s = s.substring(0, s.length() - 1);
        }

        s = s.trim();

        int i = 0;

        try {
            i = Integer.parseInt(s);
        } catch (NumberFormatException numberformatexception) {
            i = 100;
        }

        int j = (pageWidth * i) / 100;
        int k = (pageHeight * i) / 100;
        Component[] acomponent = previewer.getComponents();

        for (int l = 0; l < acomponent.length; l++) {
            if (acomponent[l] instanceof PagePreview) {
                PagePreview pagepreview = (PagePreview) acomponent[l];
                pagepreview.setScaledSize(j, k);
            }
        }

        scrollpane.setViewportView(previewer);
    }

    /**
*
*
* @param actionevent
*/
    public void actionPerformed(ActionEvent actionevent) {
        if (actionevent.getSource() == setup) {
            setup();
        } else if (actionevent.getSource() == scale) {
            Thread thread = new Thread(this);
            thread.start();
        }
    }

    class PagePreview extends JPanel {
        protected int m_h;
        protected int m_w;
        protected Image m_img;
        protected Image m_source;

        public PagePreview(int i, int j, Image image) {
            m_w = i;
            m_h = j;
            m_source = image;
            m_img = m_source.getScaledInstance(m_w, m_h, 4);
            m_img.flush();
            setBackground(Color.white);
            setBorder(new MatteBorder(1, 1, 2, 2, Color.black));
        }

        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        public Dimension getPreferredSize() {
            Insets insets = getInsets();

            return new Dimension(m_w + insets.left + insets.right,
                m_h + insets.top + insets.bottom);
        }

        public void paint(Graphics g) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            g.drawImage(m_img, 0, 0, this);
            paintBorder(g);
        }

        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        public void setScaledSize(int i, int j) {
            m_w = i;
            m_h = j;
            m_img = m_source.getScaledInstance(m_w, m_h, 4);
            repaint();
        }
    }

    class PreviewContainer extends JPanel {
        protected int V_GAP;
        protected int H_GAP;

        PreviewContainer() {
            V_GAP = 10;
            H_GAP = 16;
        }

        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        public void doLayout() {
            Insets insets = getInsets();
            int i = insets.left + H_GAP;
            int j = insets.top + V_GAP;
            int k = getComponentCount();

            if (k == 0) {
                return;
            }

            Component component = getComponent(0);
            Dimension dimension = component.getPreferredSize();
            int l = dimension.width;
            int i1 = dimension.height;
            Dimension dimension1 = getParent().getSize();
            int j1 = Math.max((dimension1.width - H_GAP) / (l + H_GAP), 1);
            int k1 = k / j1;

            if ((k1 * j1) < k) {
                k1++;
            }

            int l1 = 0;

            for (int i2 = 0; i2 < k1; i2++) {
                for (int j2 = 0; j2 < j1; j2++) {
                    if (l1 >= k) {
                        return;
                    }

                    Component component1 = getComponent(l1++);
                    component1.setBounds(i, j, l, i1);
                    i += (l + H_GAP);
                }

                j += (i1 + V_GAP);
                i = insets.left + H_GAP;
            }
        }

        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        public Dimension getPreferredSize() {
            int i = getComponentCount();

            if (i == 0) {
                return new Dimension(H_GAP, V_GAP);
            }

            Component component = getComponent(0);
            Dimension dimension = component.getPreferredSize();
            int j = dimension.width;
            int k = dimension.height;
            Dimension dimension1 = getParent().getSize();
            int l = Math.max((dimension1.width - H_GAP) / (j + H_GAP), 1);
            int i1 = i / l;

            if ((i1 * l) < i) {
                i1++;
            }

            int j1 = (l * (j + H_GAP)) + H_GAP;
            int k1 = (i1 * (k + V_GAP)) + V_GAP;
            Insets insets = getInsets();

            return new Dimension(j1 + insets.left + insets.right,
                k1 + insets.top + insets.bottom);
        }
    }
}
