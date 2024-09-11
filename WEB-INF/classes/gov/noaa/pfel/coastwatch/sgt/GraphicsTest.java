/*
 * SgtGraph Copyright 2006, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.sgt;

import gov.noaa.pfel.coastwatch.util.AttributedString2;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.TextAttribute;
import java.text.AttributedString;
import javax.swing.JFrame;
import javax.swing.JPanel;

/** This is a simple program to facilitate doing graphics tests on this computer. */
public class GraphicsTest extends JPanel {
  @Override
  public void paint(Graphics g) {
    Graphics2D g2 = (Graphics2D) g;

    // /* ***********************************************************
    // My test of AttributedString2
    AttributedString2.drawHtmlText(
        g2,
        "test <color=#ff0000>red</color> and <strong>bold</strong> tadah!",
        5,
        40,
        "Serif",
        20,
        Color.blue,
        0);
    // */

    // /*  **********************************************************
    // Working example of AttributedString from
    // http://www.java2s.com/ExampleCode/2D-Graphics-GUI/TextAttributecolorandfont.htm
    // 2019-02-08 Don't set Antialiasing. Go with current RenderingHints from the Graphics object.
    // g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
    //    RenderingHints.VALUE_ANTIALIAS_ON);
    String s = "Java Source and Support";
    Dimension d = getSize();

    Font serifFont = new Font("Serif", Font.PLAIN, 48);
    Font sansSerifFont = new Font("Monospaced", Font.PLAIN, 48);

    AttributedString as = new AttributedString(s);
    as.addAttribute(TextAttribute.FONT, serifFont);
    as.addAttribute(TextAttribute.FONT, sansSerifFont, 2, 5);
    as.addAttribute(TextAttribute.FOREGROUND, Color.red, 5, 6);
    as.addAttribute(TextAttribute.FOREGROUND, Color.red, 16, 17);

    g2.drawString(as.getIterator(), 40, 80);
    // */
  }

  public static void main(String[] args) {
    JFrame f = new JFrame();
    f.getContentPane().add(new GraphicsTest());
    f.setSize(850, 250);
    f.show();
  }
}
