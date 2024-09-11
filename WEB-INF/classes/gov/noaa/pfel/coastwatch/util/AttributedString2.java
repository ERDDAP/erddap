/*
 * AttributedString2 Copyright 2006, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.util;

import com.cohort.array.IntArray;
import com.cohort.util.String2;
import com.cohort.util.XML;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;

/**
 * This class facilitates building an AttributedString in chunks. To use it: you
 *
 * <ol>
 *   <li>do first: construct an AttributedString2 object
 *   <li>in middle: call addBaseAttributes
 *   <li>in middle: repeatedly addText and addAttributesForLastAddText
 *   <li>in middle: call addAttributes
 *   <li>do last: call draw
 * </ol>
 */
public class AttributedString2 {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  protected StringBuilder cumulative = new StringBuilder();
  protected int lastStart = 0;

  protected ArrayList baseAttributes = new ArrayList();
  protected ArrayList baseObjects = new ArrayList();

  protected ArrayList attributes = new ArrayList();
  protected ArrayList objects = new ArrayList();
  protected IntArray start = new IntArray();
  protected IntArray end = new IntArray();

  /**
   * A constructor.
   *
   * @param family e.g., "DejaVu Sans", "Bitstream Vera Sans", "Serif" or "SansSerif" (standard
   *     logical fonts: Serif, Sans-serif, Monospaced, Dialog, and DialogInput?) or a family
   *     available on the computer this is running on
   * @param size in points
   * @param color the initial color for the text
   */
  public AttributedString2(String family, double size, Color color) {
    addBaseAttribute(TextAttribute.FAMILY, family);
    addBaseAttribute(TextAttribute.SIZE, Float.valueOf((float) size));
    addBaseAttribute(TextAttribute.FOREGROUND, color);
  }

  /**
   * This adds attributes for the cumulative text. They are done first, before any range-specific
   * attributes.
   *
   * @param attribute e.g., TextAttribute.BACKGROUND
   * @param object e.g., color
   */
  public void addBaseAttribute(AttributedCharacterIterator.Attribute attribute, Object object) {
    baseAttributes.add(attribute);
    baseObjects.add(object);
  }

  /**
   * This adds a chunk of text and resets lastStart.
   *
   * @param text
   */
  public void addText(String text) {
    lastStart = cumulative.length();
    cumulative.append(text);
  }

  /**
   * This adds a character but doesn't reset lastStart.
   *
   * @param ch
   */
  public void addChar(char ch) {
    cumulative.append(ch);
  }

  /**
   * This adds the characters but doesn't reset lastStart.
   *
   * @param s
   */
  public void addChars(String s) {
    cumulative.append(s);
  }

  /**
   * This adds attributes for the last addText text to the cumulative String. Note that italic is
   * done with TextAttribute.POSTURE and Float.valueOf(0.2).
   *
   * @param attribute e.g., TextAttribute.BACKGROUND
   * @param object e.g., color
   */
  public void addAttributesForLastAddText(
      AttributedCharacterIterator.Attribute attribute, Object object) {
    attributes.add(attribute);
    objects.add(object);
    start.add(lastStart);
    end.add(cumulative.length());
  }

  /**
   * This adds an attribute for the last addText text.
   *
   * @param attribute e.g., TextAttribute.BACKGROUND
   * @param object e.g., color
   * @param tStart the starting position in the cumulative String
   * @param tEnd the ending position (exclusive) in the cumulative String
   */
  public void addAttribute(
      AttributedCharacterIterator.Attribute attribute, Object object, int tStart, int tEnd) {
    attributes.add(attribute);
    objects.add(object);
    start.add(tStart);
    end.add(tEnd);
  }

  /**
   * This returns the current number of characters in the cumulative String.
   *
   * @return the current number of characters in the cumulative String.
   */
  public int size() {
    return cumulative.length();
  }

  /**
   * This actually draws the attributed text.
   *
   * @param g2d
   * @param x
   * @param y
   * @param hAlign position of x,y relative to the text: 0=left 1=center 2=right
   */
  public void draw(Graphics2D g2d, float x, float y, int hAlign) {

    // apply styles to text
    AttributedString as = new AttributedString(cumulative.toString());
    int nChar = size();

    // apply the base attributes
    int n = baseAttributes.size();
    for (int i = 0; i < n; i++)
      as.addAttribute(
          (AttributedCharacterIterator.Attribute) baseAttributes.get(i),
          baseObjects.get(i)); // , 0, nChar);

    // apply the subsection attributes
    n = start.size();
    for (int i = 0; i < n; i++) {
      // String2.log("attributedString attributes " + attributes.get(i).toString() +
      //    " start=" + start.get(i) + " end=" + end.get(i));
      as.addAttribute(
          (AttributedCharacterIterator.Attribute) attributes.get(i),
          objects.get(i),
          start.get(i),
          end.get(i));
    }

    // draw the text
    TextLayout tl = new TextLayout(as.getIterator(), g2d.getFontRenderContext());
    if (hAlign == 1 || hAlign == 2) {
      float advance = tl.getVisibleAdvance();
      if (hAlign == 1) x -= advance / 2;
      else x -= advance;
    }
    // g2d.drawString(as.getIterator(), x, y);
    tl.draw(g2d, x, y);
  }

  /**
   * This is a convenience method which draws HTML-like text. If there are errors (e.g.,
   * unrecognized tags), this it does its best and (if verbose) prints a String2.log message.
   * Currently supported tags: b, /b, color=#FFFFFF, /color, i, /i, u, /u. Currently supported
   * character entities: amp, gt, lt, quot.
   *
   * @param g2d
   * @param htmlText basically plain text but with support for some html tags. The characters &lt;
   *     &gt; &amp; (and perhaps ") should be represented by their corresponding character entities.
   * @param x in the current coordinate system (perhaps just pixels)
   * @param y in the current coordinate system (perhaps just pixels)
   * @param family "DejaVu Sans", "Bitstream Vera Sans", "Serif" or "SansSerif" (standard logical
   *     fonts: Serif, Sans-serif, Monospaced, Dialog, and DialogInput?) or a family available on
   *     the computer this is running on
   * @param fontSize font height (ascender + descender + leading) in the current coordinate system
   * @param initialColor the initial color for the text
   * @param hAlign position of x,y relative to the text: 0=left 1=center 2=right
   */
  public static void drawHtmlText(
      Graphics2D g2d,
      String htmlText,
      double x,
      double y,
      String family,
      double fontSize,
      Color initialColor,
      int hAlign) {

    String errorInMethod =
        String2.ERROR + " in AttributedString2.drawHtmlText(" + htmlText + "):\n";
    if (verbose)
      String2.log(
          "  AttributedString2.drawHtmlText x=" + x + " y=" + y + "\n    htmlText=" + htmlText);

    int boldStart = -1;
    int colorStart = -1; // 0;
    Color color = null; // initialColor;
    int italicStart = -1;
    int underlineStart = -1;
    float weight = 1;
    int weightStart = -1;

    // go through the htmlText
    AttributedString2 as2 = new AttributedString2(family, (float) fontSize, initialColor);
    int po = 0; // next character to be read
    int htmlTextLength = htmlText.length();
    while (po < htmlTextLength) {
      char ch = htmlText.charAt(po++);

      // deal with tags
      if (ch == '<') {
        // find the '>'
        int po2 = htmlText.indexOf('>', po);
        if (po2 < 0)
          // closing '>' not found; treat '<' as literal
          as2.addChar(ch);
        else {
          String tag = htmlText.substring(po, po2).toLowerCase();
          if (verbose) String2.log("    tag=" + tag);

          // bold
          if (tag.equals("b") || tag.equals("strong")) {
            if (boldStart < 0) boldStart = as2.size();
          } else if (tag.equals("/b") || tag.equals("/strong")) {
            if (boldStart == as2.size()) {
              // do nothing
            } else if (boldStart >= 0) {
              as2.addAttribute(TextAttribute.WEIGHT, Float.valueOf(2), boldStart, as2.size());
              boldStart = -1;
            } else String2.log(errorInMethod + "unexpected /b or /strong at position " + po);

            // color
          } else if (tag.startsWith("color=#")) {
            if (colorStart >= 0)
              as2.addAttribute(TextAttribute.FOREGROUND, color, colorStart, as2.size());
            colorStart = as2.size();
            int i = String2.parseInt("0x" + tag.substring(7));
            color = new Color(i == Integer.MAX_VALUE ? 0xFFFFFF : (i & 0xFFFFFF));
          } else if (tag.equals("/color")) {
            if (colorStart == as2.size()) {
              // do nothing
            } else if (colorStart >= 0) {
              as2.addAttribute(TextAttribute.FOREGROUND, color, colorStart, as2.size());
              colorStart = -1;
            } else String2.log(errorInMethod + "unexpected /color at position " + po);

            // italic
          } else if (tag.equals("i") || tag.equals("em")) {
            if (italicStart < 0) italicStart = as2.size();
          } else if (tag.equals("/i") || tag.equals("/em")) {
            if (italicStart == as2.size()) {
              // do nothing
            } else if (italicStart >= 0) {
              as2.addAttribute(TextAttribute.POSTURE, Float.valueOf(0.2f), italicStart, as2.size());
              italicStart = -1;
            } else String2.log(errorInMethod + "unexpected /i or /em at position " + po);

            // underline
          } else if (tag.equals("u")) {
            if (underlineStart < 0) italicStart = as2.size();
          } else if (tag.equals("/u")) {
            if (underlineStart == as2.size()) {
              // do nothing
            } else if (underlineStart >= 0) {
              as2.addAttribute(
                  TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON, underlineStart, as2.size());
              underlineStart = -1;
            } else String2.log(errorInMethod + "unexpected /u at position " + po);

            // unrecognized -- ignore it
          } else {
            String2.log(errorInMethod + "unrecognized tag '" + tag + "'.");
          }

          // advance po
          po = po2 + 1;
        }

        // deal with special characters
      } else if (ch == '&') {
        // find the ';'
        int po2 = htmlText.indexOf(';', po);
        if (po2 < 0) as2.addChar(ch);
        else {
          as2.addChars(XML.decodeEntities(htmlText.substring(po - 1, po2 + 1)));
          po = po2 + 1;
        }

        // deal with plain characters
      } else as2.addChar(ch);
    }

    // close out unclosed attributes
    if (boldStart >= 0 && boldStart < as2.size())
      as2.addAttribute(TextAttribute.WEIGHT, Float.valueOf(2), boldStart, as2.size());
    if (colorStart >= 0 && colorStart < as2.size())
      as2.addAttribute(TextAttribute.FOREGROUND, color, colorStart, as2.size());
    if (italicStart >= 0 && italicStart < as2.size())
      as2.addAttribute(TextAttribute.POSTURE, Float.valueOf(0.2f), italicStart, as2.size());
    if (underlineStart >= 0 && underlineStart < as2.size())
      as2.addAttribute(
          TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON, underlineStart, as2.size());

    // draw the htmlText
    as2.draw(g2d, (float) x, (float) y, hAlign);
  }
}
