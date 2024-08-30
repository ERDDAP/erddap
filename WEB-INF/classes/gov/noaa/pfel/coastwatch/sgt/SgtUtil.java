/*
 * SgtUtil Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.Image2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;
import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import gov.noaa.pfel.coastwatch.util.AttributedString2;
import gov.noaa.pfel.coastwatch.util.SSR;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.ImageCapabilities;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;

/**
 * This class has utilities for SgtMap and SgtGraph. A note about coordinates:
 *
 * <ul>
 *   <li>Graph - uses "user" coordinates (e.g., lat and lon).
 *   <li>Layer - uses "physical" coordinates (doubles, 0,0 at lower left).
 *   <li>JPane - uses "device" coordinates (ints, 0,0 at upper left).
 * </ul>
 */
public class SgtUtil {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  public static boolean reallyVerbose = false;

  /** For the legend position. */
  public static final int LEGEND_RIGHT = 0;

  public static final int LEGEND_BELOW = 1;

  public static final com.lowagie.text.Rectangle PDF_LANDSCAPE = PageSize.LETTER.rotate();
  public static final com.lowagie.text.Rectangle PDF_PORTRAIT = PageSize.LETTER;

  public static final double DEFAULT_AXIS_LABEL_HEIGHT = 0.12;
  public static final double DEFAULT_LABEL_HEIGHT =
      0.09; // in the legend    .08 causes problems with 'w' 'm'...

  public static final Color TRANSPARENT =
      new Color(0, 0, 0, 0); // 4th 0 is alpha value   //Hmmm, it may not be this simple

  public static double AVG_CHAR_WIDTH = 4.5;

  public static String isBufferedImageAccelerated;

  /**
   * This returns the maxCharsPerLine based on charsPerLine.
   *
   * @param legendTextWidth in pixels
   * @param fontScale
   */
  public static int maxCharsPerLine(int legendTextWidth, double fontScale) {
    // lessen the effect of small fonts (they stay wide to stay legible)
    if (fontScale < 1) fontScale = (1 + fontScale) / 2;
    int m = Math2.roundToInt(legendTextWidth / (SgtUtil.AVG_CHAR_WIDTH * fontScale));
    // String2.log("\n***maxCharsPerLine=" + m + " legendWidth=" + legendTextWidth + " fontScale=" +
    // fontScale);
    return m;
  }

  /** This returns the maxBoldCharsPerLine based on charsPerLine. */
  public static int maxBoldCharsPerLine(int maxCharsPerLine) {
    return maxCharsPerLine * 9 / 10;
  }

  /**
   * This creates a font and throws exception if font family not available
   *
   * @param fontFamily
   * @throws Exception if fontFamily not available
   */
  public static Font getFont(String fontFamily) {
    // minor or major failures return a default font ("Dialog"!)
    Font font = new Font(fontFamily, Font.PLAIN, 10); // Font.ITALIC
    if (!font.getFamily().equals(fontFamily))
      Test.error(
          String2.ERROR
              + " in SgtUtil.getFont: "
              + fontFamily
              + " not available.\n"
              + String2.javaInfo()
              + "\n"
              + "Fonts available: "
              + String2.noLongLinesAtSpace(
                  String2.toCSSVString(
                      GraphicsEnvironment.getLocalGraphicsEnvironment()
                          .getAvailableFontFamilyNames()),
                  80,
                  "  "));
    return font;
  }

  /**
   * This converts the titles into a StringArray of non-"", non-null, not-too-long lines.
   *
   * @return a StringArray of non-"", non-null, not-too-long lines.
   */
  public static StringArray makeShortLines(
      int maxCharsPerLine, String title2, String title3, String title4) {

    StringArray sa = new StringArray();
    splitLine(maxCharsPerLine, sa, title2);
    splitLine(maxCharsPerLine, sa, title3);
    splitLine(maxCharsPerLine, sa, title4);
    return sa;
  }

  /**
   * This draws the standard legend text for a BELOW legend.
   *
   * @param g2
   * @param legentTextX
   * @param legendTextY
   * @param fontFamily
   * @param labelHeightPixels
   * @param shortBoldLines must be valid (if null, nothing will be drawn, and return value will be
   *     legendTextY unchanged)
   * @param shortLines from makeShortLines
   * @return the new legendTextY (adjusted so there is a gap after the current text).
   */
  public static int belowLegendText(
      Graphics2D g2,
      int legendTextX,
      int legendTextY,
      String fontFamily,
      int labelHeightPixels,
      StringArray shortBoldLines,
      StringArray shortLines) {

    // String2.log("belowLegendText boldTitle=" + boldTitle);
    if (shortBoldLines == null) return legendTextY;

    // draw the boldShortLines
    int n = shortBoldLines.size();
    for (int i = 0; i < n; i++)
      legendTextY =
          drawHtmlText(
              g2,
              legendTextX,
              legendTextY,
              0,
              fontFamily,
              labelHeightPixels,
              false,
              "<strong>" + encodeAsHtml(shortBoldLines.get(i)) + "</strong>");

    // draw the shortLines
    n = shortLines.size();
    for (int i = 0; i < n; i++)
      legendTextY =
          drawHtmlText(
              g2,
              legendTextX,
              legendTextY,
              0,
              fontFamily,
              labelHeightPixels,
              i == n - 1,
              encodeAsHtml(shortLines.get(i)));

    return legendTextY;
  }

  /**
   * This is creates "(units) date, title2", and deals with nulls and ""'s.
   *
   * @param units e.g., m s^-1
   * @param date e.g., "1998-02-28 14:00:00"
   * @param title2 e.g., "Horizontal line is mean."
   */
  public static String getNewTitle2(String units, String date, String title2) {
    StringBuilder sb = new StringBuilder();
    if (units != null && units.length() > 0) sb.append("(" + units + ") ");
    if (date != null && date.length() > 0) sb.append(date + " ");
    if (title2 != null) sb.append(title2);
    return sb.toString();
  }

  /**
   * If the line is short, this adds the line to StringArray. If the line is long, this splits the
   * line in 2 and adds both to StringArray.
   *
   * @param limit is the maximum number of characters per line
   * @param stringArray to capture the parts of s
   * @param s the string to be split (if needed) (if s == null or "", stringArray is unchanged).
   */
  // Protected to be accessed in tests.
  protected static void splitLine(int limit, StringArray stringArray, String s) {

    int limit10 = limit * 10;
    while (true) {
      if (s == null || s.length() == 0) return;

      int sLength = s.length();
      if (sLength <= limit * 2 / 3) { // short line is okay even if all caps
        stringArray.add(s);
        return;
      }

      // count through chars   noting more width of cap letters and digits, than avg letter
      int lastSpace = -1;
      int lastNonDigitChar = -1;
      int po = 0;
      int sum10 = 0;
      while (po < sLength && sum10 < limit10) {
        char ch = s.charAt(po);
        if (String2.isDigit(ch)) {
          sum10 += 14;
        } else if (String2.isLetter(ch)) {
          sum10 +=
              ch == 'C' || ch == 'M' || ch == 'S' || ch == 'W'
                  ? 16
                  : ch == 'c'
                          || ch == 'm'
                          || ch == 's'
                          || ch == 'w'
                          || ch == Character.toUpperCase(ch)
                      ? 15
                      : 10;
        } else if (ch == ' ') {
          sum10 += 8;
          lastSpace = po;
          lastNonDigitChar = po;
        } else if ("<>=_".indexOf(ch) >= 0) {
          sum10 += 17;
        } else {
          sum10 += 10;
          lastNonDigitChar = po;
        }
        po++;
      }

      // po == sLength is success
      // if just a few chars more, let it go
      if (po + 4 >= sLength) {
        stringArray.add(s);
        return;
      }

      // break at last space (or nonDigitLetter) before limit
      po =
          lastSpace >= limit * 3 / 4
              ? lastSpace
              : // preferred
              lastNonDigitChar >= limit / 2
                  ? lastNonDigitChar
                  : // next best
                  po; // worst case

      // add the string
      stringArray.add(s.substring(0, po + 1));

      // revamp s
      s = s.substring(po + 1).trim(); // remove leading space, if any
    }
  }

  /**
   * drawHtmlText draws simple HTML text to g2d. Before 2019-02-08, drawHtmlText benefited greatly
   * from setting non-text antialising ON: <tt>g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
   * RenderingHints.VALUE_ANTIALIAS_ON); </tt> Now perhaps antialiasing on by default.
   *
   * @param g2d
   * @param x the base x for the text (in pixels)
   * @param y the base y for the text (in pixels)
   * @param hAlign one of SGLabel.LEFT|CENTER|RIGHT (0=left 1=center 2=right).
   * @param fontFamily
   * @param labelHeight (in pixels)
   * @param extraGapBelow adds an extra labelHeight to the returned yAdjusted (even if htmlText is
   *     null or "")
   * @param htmlText Unless modified by a <color=#ffffff> tag, the text will be black.
   * @return y adjusted to prepare for the text below this text
   */
  public static int drawHtmlText(
      Graphics2D g2d,
      int x,
      int y,
      int hAlign,
      String fontFamily,
      int labelHeight,
      boolean extraGapBelow,
      String htmlText) {

    if (htmlText == null || htmlText.length() == 0) return y + (extraGapBelow ? labelHeight : 0);

    // quick fix red affecting whole string?
    // htmlText= "<color=#000000> " + htmlText;
    // String2.log("drawHtmlText=" + htmlText);
    AttributedString2.drawHtmlText(
        g2d, htmlText, x, y, fontFamily, labelHeight, Color.black, hAlign);
    return y + labelHeight + (extraGapBelow ? labelHeight : 0);
  }

  /**
   * This is a special version of XML.encodeAsHTML that displays any occurence of "EXPERIMENTAL
   * PRODUCT" or "EXPERIMENTAL" in red.
   *
   * @param plainText
   * @return htmlText
   */
  public static String encodeAsHtml(String plainText) {
    if (plainText == null || plainText.length() == 0) return "";
    int po = plainText.indexOf("EXPERIMENTAL PRODUCT");
    if (po >= 0)
      return XML.encodeAsHTML(plainText.substring(0, po))
          + "<color=#ff0000>EXPERIMENTAL PRODUCT</color>"
          + XML.encodeAsHTML(plainText.substring(po + 20));

    po = plainText.indexOf("EXPERIMENTAL");
    if (po >= 0)
      return XML.encodeAsHTML(plainText.substring(0, po))
          + "<color=#ff0000>EXPERIMENTAL</color>"
          + XML.encodeAsHTML(plainText.substring(po + 12));

    return XML.encodeAsHTML(plainText);
  }

  /**
   * This makes a new bufferedImage suitable for SgtMap.makeMap or SgtGraph.makeGraph. The
   * background is white.
   *
   * @param gifWidth
   * @param gifHeight
   * @return a bufferedImage of the requested size
   * @throws Exception if trouble
   */
  public static BufferedImage getBufferedImage(int gifWidth, int gifHeight) {

    //  Work with BufferedImage requires the following line be added to
    //  beginning of startup.sh:
    //    export JAVA_OPTS=-Djava.awt.headless=true
    BufferedImage bi = new BufferedImage(gifWidth, gifHeight, BufferedImage.TYPE_INT_RGB);
    Graphics g = bi.getGraphics();
    Graphics2D g2 = (Graphics2D) g;
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setColor(Color.white); // I'm not sure why necessary, but it is
    g2.fillRect(0, 0, gifWidth, gifHeight); // I'm not sure why necessary, but it is

    return bi;
  }

  /**
   * This returns a message indicating if graphics operations on bufferedImages are hardware
   * accelerated.
   */
  public static String isBufferedImageAccelerated() {
    if (isBufferedImageAccelerated == null) {
      try {
        BufferedImage bi = getBufferedImage(10, 10);
        ImageCapabilities imCap = bi.getCapabilities(null);
        isBufferedImageAccelerated =
            "bufferedImage isAccelerated=" + (imCap == null ? "[unknown]" : imCap.isAccelerated());
      } catch (Throwable t) {
        String2.log(MustBe.throwableToString(t));
        isBufferedImageAccelerated = "bufferedImage isAccelerated=[unknown]";
      }
    }
    return isBufferedImageAccelerated;
  }

  /**
   * This reads a image using Java's ImageIO routines.
   *
   * @param fullName with directory and extension
   * @return a BufferedImage
   * @throws Exception if trouble
   */
  public static BufferedImage readImage(String fullName) throws Exception {
    return ImageIO.read(new File(fullName));
  }

  /**
   * Saves an image as a non-transparent .gif or .png based on the fullImageName's extension. This
   * will overwrite an existing file. Gif's are saved with ImageMagick's convert (which does great
   * color reduction).
   *
   * @param bi
   * @param fullName with directory and extension
   * @throws Exception if trouble
   */
  public static void saveImage(BufferedImage bi, String fullName) throws Exception {
    String shortName =
        fullName.substring(0, fullName.length() - 4); // currently, all extensions are 4 char
    if (fullName.endsWith(".gif")) saveAsGif(bi, shortName);
    else if (fullName.endsWith(".png")) saveAsPng(bi, shortName);
    // else if (fullName.endsWith(".jpg"))
    //    saveAsJpg(bi, shortName);
    else
      Test.error(
          String2.ERROR
              + " in SgtUtil.saveImage: "
              + "Unsupported image type for fileName="
              + fullName);
  }

  /**
   * Saves an image as a gif. Currently this uses ImageMagick's "convert" (Windows or Linux) because
   * it does the best job at color reduction (and is fast and is cross-platform). This will
   * overwrite an existing file.
   *
   * @param bi
   * @param fullGifName but without the .gif at the end
   * @throws Exception if trouble
   */
  public static void saveAsGif(BufferedImage bi, String fullGifName) throws Exception {

    // POLICY: because this procedure may be used in more than one thread,
    // do work on unique temp files names using randomInt, then rename to proper file name.
    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);

    // save as .bmp     (note: doesn't support transparent pixels)
    long time = System.currentTimeMillis();
    if (verbose) String2.log("SgtUtil.saveAsGif");
    ImageIO.write(bi, "bmp", new File(fullGifName + randomInt + ".bmp"));
    if (verbose)
      String2.log("  make .bmp done. time=" + (System.currentTimeMillis() - time) + "ms");

    // "convert" to .gif
    SSR.dosOrCShell(
        "convert " + fullGifName + randomInt + ".bmp" + " " + fullGifName + randomInt + ".gif", 30);
    File2.delete(fullGifName + randomInt + ".bmp");

    // try fancy color reduction algorithms
    // Image2.saveAsGif(Image2.reduceTo216Colors(bi), fullGifName + randomInt + ".gif");

    // try dithering
    // Image2.saveAsGif216(bi, fullGifName + randomInt + ".gif", true);

    // last step: rename to final gif name
    File2.rename(fullGifName + randomInt + ".gif", fullGifName + ".gif");

    if (verbose)
      String2.log(
          "SgtUtil.saveAsGif done. TOTAL TIME=" + (System.currentTimeMillis() - time) + "ms\n");
  }

  /**
   * Saves an image as a gif. Currently this uses ImageMagick's "convert" (Windows or Linux) because
   * it does the best job at color reduction (and is fast and is cross-platform). This will
   * overwrite an existing file.
   *
   * @param bi
   * @param transparent the color to be made transparent
   * @param fullGifName but without the .gif at the end
   * @throws Exception if trouble
   */
  public static void saveAsTransparentGif(BufferedImage bi, Color transparent, String fullGifName)
      throws Exception {

    // POLICY: because this procedure may be used in more than one thread,
    // do work on unique temp files names using randomInt, then rename to proper file name.
    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);

    // convert transparent color to be transparent
    long time = System.currentTimeMillis();
    Image image = Image2.makeImageBackgroundTransparent(bi, transparent, 10000);

    // convert image back to bufferedImage
    bi = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics g = bi.getGraphics();
    g.drawImage(image, 0, 0, bi.getWidth(), bi.getHeight(), null);
    image = null; // encourage garbage collection

    // save as png
    int random = Math2.random(Integer.MAX_VALUE);
    ImageIO.write(bi, "png", new File(fullGifName + randomInt + ".png"));

    // "convert" to .gif
    SSR.dosOrCShell(
        "convert " + fullGifName + randomInt + ".png" + " " + fullGifName + randomInt + ".gif", 30);
    File2.delete(fullGifName + randomInt + ".png");

    // try fancy color reduction algorithms
    // Image2.saveAsGif(Image2.reduceTo216Colors(bi), fullGifName + randomInt + ".gif");

    // try dithering
    // Image2.saveAsGif216(bi, fullGifName + randomInt + ".gif", true);

    // last step: rename to final gif name
    File2.rename(fullGifName + randomInt + ".gif", fullGifName + ".gif");

    if (verbose)
      String2.log(
          "SgtUtil.saveAsTransparentGif TIME=" + (System.currentTimeMillis() - time) + "ms\n");
  }

  /**
   * Saves an image as a png. This will overwrite an existing file.
   *
   * @param bi
   * @param fullPngName but without the .png at the end
   * @throws Exception if trouble
   */
  public static void saveAsPng(BufferedImage bi, String fullPngName) throws Exception {
    saveAsTransparentPng(bi, null, fullPngName);
  }

  /**
   * Saves an image as a png. This will overwrite an existing file.
   *
   * @param bi
   * @param transparent the color to be made transparent (or null if none)
   * @param fullPngName but without the .png at the end
   * @throws Exception if trouble
   */
  public static void saveAsTransparentPng(BufferedImage bi, Color transparent, String fullPngName)
      throws Exception {

    // POLICY: because this procedure may be used in more than one thread,
    // do work on unique temp files names using randomInt, then rename to proper file name.
    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);

    // create fileOutputStream
    BufferedOutputStream bos =
        new BufferedOutputStream(new FileOutputStream(fullPngName + randomInt + ".png"));
    try {
      // save the image
      saveAsTransparentPng(bi, transparent, bos);
    } finally {
      bos.close();
    }

    // last step: rename to final Png name
    File2.rename(fullPngName + randomInt + ".png", fullPngName + ".png");
  }

  /**
   * Saves an image as a png. This will overwrite an existing file.
   *
   * @param bi
   * @param outputStream
   * @throws Exception if trouble
   */
  public static void saveAsPng(BufferedImage bi, OutputStream outputStream) throws Exception {
    saveAsTransparentPng(bi, null, outputStream);
  }

  /**
   * Saves an image as a png. This will overwrite an existing file.
   *
   * @param bi
   * @param transparent the color to be made transparent (or null if none)
   * @param outputStream (it is flushed at the end)
   * @throws Exception if trouble
   */
  public static void saveAsTransparentPng(
      BufferedImage bi, Color transparent, OutputStream outputStream) throws Exception {

    // convert transparent color to be transparent
    long time = System.currentTimeMillis();
    if (transparent != null) {
      Image image = Image2.makeImageBackgroundTransparent(bi, transparent, 10000);

      // convert image back to bufferedImage
      bi = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
      Graphics g = bi.getGraphics();
      g.drawImage(image, 0, 0, bi.getWidth(), bi.getHeight(), null);
    }

    // save as png
    ImageIO.write(bi, "png", outputStream);
    outputStream.flush();

    if (verbose)
      String2.log("SgtUtil.saveAsPng TIME=" + (System.currentTimeMillis() - time) + "ms\n");
  }

  /**
   * This creates a file to capture the pdf output generated by calls to graphics2D (e.g., use
   * makeMap). This will overwrite an existing file.
   *
   * @param pageSize e.g, PageSize.LETTER or PageSize.LETTER.rotate() (or A4, or, ...)
   * @param width the bounding box width, in 1/144ths of an inch
   * @param height the bounding box height, in 1/144ths of an inch
   * @param fullFileName (with the extension .pdf)
   * @return an object[] with 0=g2D, 1=document, 2=pdfContentByte, 3=pdfTemplate
   * @throws Exception if trouble
   */
  public static Object[] createPdf(
      com.lowagie.text.Rectangle pageSize, int bbWidth, int bbHeight, String fullFileName)
      throws Exception {
    return createPdf(
        pageSize, bbWidth, bbHeight, new BufferedOutputStream(new FileOutputStream(fullFileName)));
  }

  /**
   * This creates a file to capture the pdf output generated by calls to graphics2D (e.g., use
   * makeMap). This will overwrite an existing file.
   *
   * @param pageSize e.g, PageSize.LETTER or PageSize.LETTER.rotate() (or A4, or, ...)
   * @param width the bounding box width, in 1/144ths of an inch
   * @param height the bounding box height, in 1/144ths of an inch
   * @param outputStream Best if buffered.
   * @return an object[] with 0=g2D, 1=document, 2=pdfContentByte, 3=pdfTemplate
   * @throws Exception if trouble
   */
  public static Object[] createPdf(
      com.lowagie.text.Rectangle pageSize, int bbWidth, int bbHeight, OutputStream outputStream)
      throws Exception {
    // currently, this uses itext
    // see the sample program:
    //
    // file://localhost/C:/programs/iText/examples/com/lowagie/examples/directcontent/graphics2D/G2D.java
    // Document.compress = false; //for test purposes only
    Document document = new Document(pageSize);
    document.addCreationDate();
    document.addCreator("gov.noaa.pfel.coastwatch.SgtUtil.createPdf");

    document.setPageSize(pageSize);
    PdfWriter writer = PdfWriter.getInstance(document, outputStream);
    document.open();

    // create contentByte and template and Graphics2D objects
    PdfContentByte pdfContentByte = writer.getDirectContent();
    PdfTemplate pdfTemplate = pdfContentByte.createTemplate(bbWidth, bbHeight);
    Graphics2D g2D = pdfTemplate.createGraphics(bbWidth, bbHeight);

    return new Object[] {g2D, document, pdfContentByte, pdfTemplate};
  }

  /**
   * This closes the pdf file created by createPDF, after you have written things to g2D.
   *
   * @param oar the object[] returned from createPdf
   * @throws Exception if trouble
   */
  public static void closePdf(Object oar[]) throws Exception {
    Graphics2D g2D = (Graphics2D) oar[0];
    Document document = (Document) oar[1];
    try {
      PdfContentByte pdfContentByte = (PdfContentByte) oar[2];
      PdfTemplate pdfTemplate = (PdfTemplate) oar[3];

      g2D.dispose();

      // center it
      if (verbose)
        String2.log(
            "SgtUtil.closePdf"
                + " left="
                + document.left()
                + " right="
                + document.right()
                + " bottom="
                + document.bottom()
                + " top="
                + document.top()
                + " template.width="
                + pdfTemplate.getWidth()
                + " template.height="
                + pdfTemplate.getHeight());
      // x device = ax user + by user + e
      // y device = cx user + dy user + f
      pdfContentByte.addTemplate(
          pdfTemplate, // a,b,c,d,e,f      //x,y location in points
          0.5f,
          0,
          0,
          0.5f,
          document.left() + (document.right() - document.left() - pdfTemplate.getWidth() / 2) / 2,
          document.bottom()
              + (document.top() - document.bottom() - pdfTemplate.getHeight() / 2) / 2);

      /*
      //if boundingBox is small, center it
      //if boundingBox is large, shrink and center it
      //document.left/right/top/bottom include 1/2" margins
      float xScale = (document.right() - document.left())   / pdfTemplate.getWidth();
      float yScale = (document.top()   - document.bottom()) / pdfTemplate.getHeight();
      float scale = Math.min(Math.min(xScale, yScale), 1);
      float xSize = pdfTemplate.getWidth()  / scale;
      float ySize = pdfTemplate.getHeight() / scale;
      //x device = ax user + by user + e
      //y device = cx user + dy user + f
      pdfContentByte.addTemplate(pdfTemplate, //a,b,c,d,e,f
          scale, 0, 0, scale,
          document.left()   + (document.right() - document.left()   - xSize) / 2,
          document.bottom() + (document.top()   - document.bottom() - ySize) / 2);
      */
    } finally {
      document.close();
    }
  }

  /**
   * This returns a whiter color than c.
   *
   * @param color
   * @return a whiter color than c
   */
  public static Color whiter(Color color) {
    int r = color.getRed();
    int g = color.getGreen();
    int b = color.getBlue();
    return new Color(
        r + (255 - r) / 4, // little changes close to 255 have big effect
        g + (255 - g) / 4,
        b + (255 - b) / 4);
  }

  /**
   * This returns a blacker color than c.
   *
   * @param color
   * @return a blacker color than c
   */
  public static Color blacker(Color color) {
    int r = color.getRed();
    int g = color.getGreen();
    int b = color.getBlue();
    return new Color(
        Math.max(0, r - (255 - r) / 4), // little changes close to 255 have big effect
        Math.max(0, g - (255 - g) / 4),
        Math.max(0, b - (255 - b) / 4));
  }

  /**
   * The default palette (aka color bar) range ([0]=min, [1]=max). The values are also suitable for
   * the axis range on a graph.
   *
   * @param dataMin the raw minimum value of the data
   * @param dataMax the raw maximum value of the data
   * @return the default palette (aka color bar) range ([0]=min, [1]=max).
   */
  public static double[] suggestPaletteRange(double dataMin, double dataMax) {

    double lowHigh[] = Math2.suggestLowHigh(dataMin, dataMax);

    // log axis?
    if (suggestPaletteScale(dataMin, dataMax)
        .equals("Log")) { // yes, use dataMin,dataMax,  not lowHigh
      lowHigh[0] =
          Math2.suggestLowHigh(dataMin, 2 * dataMin)[0]; // trick to get nice suggested min>0
      return lowHigh;
    }

    // axis is linear
    // suggest symmetric around 0 (symbolized by BlueWhiteRed)?
    if (suggestPalette(dataMin, dataMax)
        .equals("BlueWhiteRed")) { // yes, use dataMin,dataMax,  not lowHigh
      double rangeMax = Math.max(-lowHigh[0], lowHigh[1]);
      lowHigh[0] = -rangeMax;
      lowHigh[1] = rangeMax;
    }

    // standard Rainbow Linear
    return lowHigh;
  }

  /**
   * The name of the suggested palette (aka color bar), e.g., Rainbow or BlueWhiteRed. Must be one
   * of the palettes available to PointDataSets in the browser.
   *
   * @param min the raw minimum value of the data (preferred) or the refined minimum value for the
   *     palette
   * @param max the raw maximum value of the data (preferred) or the refined maximum value for the
   *     palette
   * @return the name of the suggested palette (aka color bar), e.g., Rainbow. "BlueWhiteRed" is
   *     suggested if the palette should be centered on 0.
   */
  public static String suggestPalette(double min, double max) {
    if (min < 0 && max > 0 && -min / max >= .5 && -min / max <= 2) return "BlueWhiteRed";
    if (min >= 0 && min < max / 5) return "WhiteRedBlack";
    return "Rainbow";
  }

  /**
   * The name of the suggested palette scale, e.g., Linear or Log.
   *
   * @param min the raw minimum value of the data (preferred) or the refined minimum value for the
   *     palette
   * @param max the raw maximum value of the data (preferred) or the refined maximum value for the
   *     palette
   * @return the name of the suggested palette (aka color bar) scale, e.g., Linear or Log.
   */
  public static String suggestPaletteScale(double min, double max) {
    if (min > 0 && min < 1 && max / min > 100) return "Log";
    return "Linear";
  }

  /**
   * This find the low and high pixels with the legend (assuming the legend is near the bottom and
   * is along the left edge, and spans the width of the image).
   *
   * @return the low (a smaller number) and high y (a bigger number) of the legend. They are the y's
   *     of the edges. If trouble, this returns {0, 0}.
   */
  public static int[] findLegendLH(BufferedImage bufferedImage) {
    int black = 0xFF000000;
    int height = bufferedImage.getHeight();
    int lh[] = new int[] {0, 0};

    // find bottom edge
    for (int y = height - 1; y >= 0; y--) {
      if (bufferedImage.getRGB(0, y) == black) {
        lh[1] = y;
        break;
      }
    }

    // find top edge
    for (int y = lh[1] - 1; y >= 0; y--) {
      if (bufferedImage.getRGB(0, y) != black) {
        lh[0] = y + 1;
        break;
      }
    }
    // String2.log("findLegendLH low=" + lh[0] + " high=" + lh[1]);
    return lh;
  }

  /**
   * Given a bufferedImage with a legend near the bottom (entire width of image), this replaces the
   * legend with white.
   *
   * @param bufferedImage
   * @return a bufferedImage without the legend. If trouble, this returns the original image.
   */
  public static BufferedImage removeLegend(BufferedImage bufferedImage) {
    try {
      int lh[] = findLegendLH(bufferedImage);
      if (lh[0] == 0 && lh[1] == 0) return bufferedImage;

      Graphics g = bufferedImage.getGraphics();
      g.setColor(Color.white); // white
      g.fillRect(0, lh[0], bufferedImage.getWidth(), lh[1] - lh[0] + 1);
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
    }
    return bufferedImage;
  }

  /**
   * Given a bufferedImage with a legend near the bottom (entire width of image), this returns an
   * image with just the legend.
   *
   * @param bufferedImage
   * @return a bufferedImage with just the legend.
   * @throws RuntimeException if trouble (e.g., no legend found)
   */
  public static BufferedImage extractLegend(BufferedImage bufferedImage) {
    int lh[] = findLegendLH(bufferedImage);
    if (lh[0] == 0 && lh[1] == 0) throw new RuntimeException("Legend not found.");

    int width = bufferedImage.getWidth();
    int height = lh[1] - lh[0] + 1;
    BufferedImage newBI = getBufferedImage(width, height);
    Graphics g = newBI.getGraphics();
    g.drawImage(
        bufferedImage,
        0,
        0,
        width,
        height, // dest     params are exclusive
        0,
        lh[0],
        width,
        lh[1] + 1, // source
        null); // documentation differs, but I think it blocks till finished if no observer
    return newBI;
  }

  /**
   * Given a bufferedImage, this removes any whitespace more than 10 lines at the bottom.
   *
   * @param bufferedImage
   * @param borderWidth in pixels
   * @return a trimmed bufferedImage. If trouble, this returns the original image.
   */
  public static BufferedImage trimBottom(BufferedImage bufferedImage, int borderWidth) {
    try {

      if (borderWidth < 0) borderWidth = 0;
      if (borderWidth > 1000) return bufferedImage;

      // find the first non-white pixel above bottom edge
      int width = bufferedImage.getWidth();
      int height = bufferedImage.getHeight();
      int y;
      Y_LOOP:
      for (y = height - 1; y >= 0; y--) {
        for (int x = 0; x < width; x++) {
          if (bufferedImage.getRGB(x, y) != 0xFFFFFFFF) break Y_LOOP;
        }
      }

      // if (verbose) String2.log("trimBottom y=" + y + " height=" + height);
      int newHeight = y + borderWidth + 1;
      if (y < 0 || newHeight >= height) return bufferedImage;

      BufferedImage newBI = getBufferedImage(width, newHeight);
      Graphics g = newBI.getGraphics();
      g.drawImage(
          bufferedImage,
          0,
          0,
          width,
          newHeight, // dest     params are exclusive
          0,
          0,
          width,
          newHeight, // source
          null); // documentation differs, but I think it blocks till finished if no observer
      return newBI;

    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
    }
    return bufferedImage;
  }

  /**
   * Given a bufferedImage with a rectangular graph/map at the top, this returns the left, right,
   * bottom, top of the graph (from human perspective). Since y=0 at top of image, the returned top
   * value will be a lower value than bottom.
   *
   * @param bufferedImage
   * @return int[4] with left, right, bottom, top of the graph. If trouble, this returns null.
   */
  public static int[] findGraph(BufferedImage bufferedImage) {
    // rely on this try/catch to catch errors
    try {

      int width = bufferedImage.getWidth();
      int height = bufferedImage.getHeight();
      int centerX = width / 2;

      // starting at top center, go down to first back pixel
      int top = 0;
      while (bufferedImage.getRGB(centerX, top) != 0xff000000
          || // look at main pixel
          bufferedImage.getRGB(centerX - 1, top) != 0xff000000
          || // and to left
          bufferedImage.getRGB(centerX + 1, top) != 0xff000000) { // and to right
        top++;
        // String2.log("top=" + top + " 0x" + Integer.toHexString(bufferedImage.getRGB(centerX,
        // top)));
      }

      // go left to left  (may be fooled by tic mark)
      int left = centerX - 1;
      while (bufferedImage.getRGB(left - 1, top) == 0xff000000) {
        left--;
        // String2.log("left=" + left + " 0x" + Integer.toHexString(bufferedImage.getRGB(left-1,
        // top)));
      }

      // backtrack left if it was tic mark
      while (bufferedImage.getRGB(left, top + 1) != 0xff000000) {
        left++;
        // String2.log("backtrack left=" + left + " 0x" +
        // Integer.toHexString(bufferedImage.getRGB(left, top-1)));
      }

      // go right to right
      int right = centerX + 1;
      while (bufferedImage.getRGB(right + 1, top) == 0xff000000) {
        right++;
        // String2.log("right=" + right + " 0x" + Integer.toHexString(bufferedImage.getRGB(right+1,
        // top)));
      }

      // go down to bottom (may be fooled by tick mark)
      int bottom = top;
      while (bufferedImage.getRGB(left, bottom + 1) == 0xff000000
          && bufferedImage.getRGB(right, bottom + 1) == 0xff000000) {
        bottom++;
        // String2.log("bottom=" + bottom + " 0x" + Integer.toHexString(bufferedImage.getRGB(left,
        // bottom-1)));
      }

      // backtrack bottom if it was tick mark
      while (bufferedImage.getRGB(left + 1, bottom) != 0xff000000
          || bufferedImage.getRGB(right - 1, bottom) != 0xff000000) {
        bottom--;
        // String2.log("backtrack bottom=" + bottom + " 0x" +
        // Integer.toHexString(bufferedImage.getRGB(left+1, bottom)));
      }

      // don't bother to check integrity of bottom edge
      // String2.log("success " + left + " " + right + " " + bottom + " " + top);
      return new int[] {left, right, bottom, top};

    } catch (Throwable t) {
      String2.log("SgtUtil.findGraph failed.\n" + MustBe.throwableToString(t));
      return null;
    }
  }

  /**
   * This is used to convert an image'x x,y location into lonLat values.
   *
   * @param x x pixel of user click on image
   * @param y y pixel of user click on image
   * @param intWESN is the WESN of the graph on the image. Note that S will be numerically greater
   *     than N.
   * @param doubleWESN is the lon lat WESN of the graph on the image
   * @param extentWESN is the maximum extent allowed (so center not shifted too far)
   * @return double[2] 0=lon 1=lat. or null if trouble (e.g., intWESN is null)
   */
  public static double[] xyToLonLat(
      int x, int y, int[] intWESN, double[] doubleWESN, double[] extentWESN) {
    if (intWESN == null
        || doubleWESN == null
        || intWESN[0] >= intWESN[1]
        || intWESN[2] <= intWESN[3]) return null;

    double xRange = doubleWESN[1] - doubleWESN[0];
    double yRange = doubleWESN[3] - doubleWESN[2];
    double newX, newY;

    if (x < intWESN[0] || x > intWESN[1]) {
      // a click outside of graph shifts center to (theoretical) adjacent panel
      newX = x < intWESN[0] ? doubleWESN[0] - xRange / 2 : doubleWESN[1] + xRange / 2;
      newY =
          y < intWESN[3]
              ? doubleWESN[3] + yRange / 2
              : // N
              y > intWESN[2]
                  ? doubleWESN[2] - yRange / 2
                  : // S
                  doubleWESN[2] + yRange / 2;

      // ensure not too far
      newX = Math.max(newX, extentWESN[0] + xRange / 2);
      newX = Math.min(newX, extentWESN[1] - xRange / 2);
      newY = Math.max(newY, extentWESN[2] + yRange / 2);
      newY = Math.min(newY, extentWESN[3] - yRange / 2);

    } else if (y < intWESN[3] || y > intWESN[2]) {
      // y is outside of graph, but x must be within graph
      newX = doubleWESN[0] + xRange / 2;
      newY = y < intWESN[3] ? doubleWESN[3] + yRange / 2 : doubleWESN[2] - yRange / 2;

      // ensure not too far
      newX = Math.max(newX, extentWESN[0] + xRange / 2);
      newX = Math.min(newX, extentWESN[1] - xRange / 2);
      newY = Math.max(newY, extentWESN[2] + yRange / 2);
      newY = Math.min(newY, extentWESN[3] - yRange / 2);

    } else {
      // click within the graph
      newX = doubleWESN[0] + (x - intWESN[0]) * xRange / (intWESN[1] - intWESN[0]);
      newY = doubleWESN[2] + (y - intWESN[2]) * yRange / (intWESN[3] - intWESN[2]);
    }

    return new double[] {newX, newY};
  }

  // *** Junk Yard *******
  // create the colorbar for the legend
  /*ColorKey colorKey = new ColorKey(new Point2D.Double(4.5, 3), //location
      new Dimension2D(0.25, 2.5), //size
      ColorKey.TOP, ColorKey.LEFT); //valign, halign
  colorKey.setOrientation(ColorKey.VERTICAL);
  colorKey.setBorderStyle(ColorKey.NO_BORDER);
  colorKey.setColorMap(colorMap);
  Ruler ruler = colorKey.getRuler();
  ruler.setLabelFont(labelFont);
  ruler.setLabelHeightP(0.15);
  ruler.setLabelInterval(2); //temp
  ruler.setLargeTicHeightP(0.04);
  ruler.setRangeU(colorMap.getRange());
  String2.log("colorMap start=" + colorMap.getRange().start + " end=" +
      colorMap.getRange().end + " delta=" + colorMap.getRange().delta);
  layer.addChild(colorKey);
  */
}
