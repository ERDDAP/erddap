package com.cohort.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.awt.image.RenderedImage;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import javax.imageio.ImageIO;
import tags.TagImageComparison;

public class Image2Tests {

  public static URL TEST_DIR = Image2Tests.class.getResource("/data/images/");

  public static URL DIFF_DIR = Image2Tests.class.getResource("/data/images/diff/");

  public static URL OBS_DIR = Image2Tests.class.getResource("/data/images/obs/");

  public static String urlToAbsolutePath(URL url) throws URISyntaxException {
    return Path.of(url.toURI()).toString() + "/";
  }

  /** This intentionally throws an Exception to test testImagesIdentical(). */
  @org.junit.jupiter.api.Test
  @TagImageComparison
  void testTestImagesIdentical() throws Exception {
    // String2.log("\n*** Image2.testTestImagesIdentical");

    // test images which are identical
    Image2Tests.testImagesIdentical(
        urlToAbsolutePath(TEST_DIR) + "testImagesIdentical_1.png",
        "testImagesIdentical_1.png",
        "testImagesIdentical_diff.png");

    // test images which aren't identical
    try {

      // one time: createImage with transparent background
      // BufferedImage image2 = Image2.getImage(testDir + "testImagesIdentical_1.png",
      // 2000, false);
      // image2 = Image2.makeImageBackgroundTransparent(image2, Color.white, 10000);
      // Image2.saveAsPng(image2, testDir + "testImagesIdentical_2.png");

      // test images which aren't identical
      Image2Tests.testImagesIdentical(
          urlToAbsolutePath(TEST_DIR) + "testImagesIdentical_1.png",
          "testImagesIdentical_2.png",
          "testImagesIdentical_diff.png");
    } catch (Exception e) {
      return;
    }
    throw new RuntimeException("shouldn't get here");
  }

  @org.junit.jupiter.api.Test
  /** Test the methods in Image2. */
  void basicTest() throws Exception {
    String2.log("\n*** Image2.basicTest");

    String testDir = urlToAbsolutePath(TEST_DIR);

    // test ImageIO
    BufferedImage bi = ImageIO.read(new File(testDir + "testmap.gif"));
    Graphics g = bi.getGraphics();
    ImageIO.write(bi, "png", new File(testDir + "temp.png"));
    Image2.saveAsGif(bi, testDir + "temp.gif");

    long localTime = System.currentTimeMillis();
    String2.log("test() here 1");
    Color gray = new Color(128, 128, 128);
    String2.log("test() here 2=" + (System.currentTimeMillis() - localTime));
    Image image = Image2.getImage(testDir + "temp.gif", 10000, false); // javaShouldCache
    String2.log("test() here 3=" + (System.currentTimeMillis() - localTime));

    // convert 128,128,128 to transparent
    image = Image2.makeImageBackgroundTransparent(image, gray, 10000);
    String2.log("test() here 4=" + (System.currentTimeMillis() - localTime));

    // save as gif again
    Image2.saveAsGif(image, testDir + "temp2.gif");
    String2.log("test() here 5=" + (System.currentTimeMillis() - localTime));

    File2.delete(testDir + "temp.png");
    File2.delete(testDir + "temp.gif");
    File2.delete(testDir + "temp2.gif");
  }

  /**
   * This is like the other testImagesIdentical, but uses DEFAULT_ALLOW_N_PIXELS_DIFFERENT.
   *
   * @param observed the full name of the image file to be tested (.gif, .jpg, or .png)
   * @param expected the full name of the expected/standard/correct image file (.gif, .jpg, or .png)
   * @param diffName the full name of a .png image file that will be created highlighting the
   *     differences between observed and expected.
   * @throws Exception if the images are different or there is trouble
   */
  public static void testImagesIdentical(String observed, String expected, String diffName)
      throws Exception {
    testImagesIdentical(
        observed,
        expected,
        diffName,
        Image2.DEFAULT_ALLOW_N_PIXELS_DIFFERENT,
        Image2.DEFAULT_AUTO_CREATE_IF_MISSING,
        Image2.DEFAULT_DISPLAY_IMAGES);
  }

  /**
   * This tests if image1 is the same as image2. If different, the differences are saved in
   * diffName.
   *
   * @param image1 an image
   * @param image2 an image
   * @param diffName the name of a .gif, .jpg, or .png image file that is sometimes created if there
   *     are differences
   * @param allowNPixelsDifferent doesn't throw an Exception if nPixelsDifferent
   *     &lt;=allowNPixelsDifferent. Use 0 to test for true equality.
   * @return a string describing the differences ("" if no differences or nPixelsDifferent &lt;=
   *     COMPARE_ALLOW_N_PIXELS_DIFFERENT).
   * @throws Exception if serious trouble, e.g., images are null
   */
  public static String compareImages(
      Image image1, Image image2, String diffName, int allowNPixelsDifferent) throws Exception {
    String cmd = "Image2.compareImages: ";

    // are they the same size?
    int width1 = image1.getWidth(null);
    int width2 = image2.getWidth(null);
    int height1 = image1.getHeight(null);
    int height2 = image2.getHeight(null);
    int widthHeight = width1 * height1;
    if (width1 != width2 || height1 != height2)
      return cmd
          + "the sizes are not the same: "
          + width1
          + "x"
          + height1
          + " "
          + width2
          + "x"
          + height2;

    // grab the pixels
    int[] pixels1 = new int[widthHeight];
    PixelGrabber grabber =
        new PixelGrabber(
            image1, // see Nutshell pg 369
            0, 0, width1, height1, pixels1, // array to put pixels in
            0, // offset
            width1); // scansize
    grabber.grabPixels(10000); // try for up to 10 sec (huge files)

    int[] pixels2 = new int[widthHeight];
    grabber =
        new PixelGrabber(
            image2, // see Nutshell pg 369
            0, 0, width2, height2, pixels2, // array to put pixels in
            0, // offset
            width2); // scansize
    grabber.grabPixels(10000); // try for up to 10 sec (for huge files)

    // recover some memory
    image1 = null;
    image2 = null;

    // if different, make array of differences
    int[] pixels3 = new int[widthHeight];
    Arrays.fill(pixels3, 0xFF909090); // opaque gray
    int nDifferent = 0;
    int diff, lastDiff = 0, tMaxDiff, maxDiff = 0;
    String error = "";
    if (!Arrays.equals(pixels1, pixels2)) {
      for (int i = 0; i < widthHeight; i++) {
        diff = pixels1[i] - pixels2[i];

        if (diff != 0) {
          if (diff != lastDiff) { // diagnostic
            int aDiff = Math.abs((pixels1[i] >>> 24) - (pixels2[i] >>> 24));
            int rDiff = Math.abs(((pixels1[i] >> 16) & 255) - ((pixels2[i] >> 16) & 255));
            int gDiff = Math.abs(((pixels1[i] >> 8) & 255) - ((pixels2[i] >> 8) & 255));
            int bDiff = Math.abs((pixels1[i] & 255) - (pixels2[i] & 255));
            lastDiff = diff;
            tMaxDiff = rDiff + gDiff + aDiff + bDiff;
            if (tMaxDiff > maxDiff) {
              error =
                  cmd
                      + "maxDiff: "
                      + aDiff
                      + " "
                      + rDiff
                      + " "
                      + gDiff
                      + " "
                      + bDiff
                      + " "
                      + Integer.toHexString(pixels1[i])
                      + " "
                      + Integer.toHexString(pixels2[i]);
              maxDiff = tMaxDiff;
            }
          }
          nDifferent++;
          pixels3[i] = 0xFF000000 | Math.abs(diff); // make opaque
        }
      }
    }
    if (nDifferent == 0) return "";

    // if different, save differences as an image file
    RenderedImage image3 = Image2.makeImageFromArray(pixels3, width1, height1, 5000);
    Image2.saveAsPng(image3, diffName);
    String msg =
        cmd
            + "There were "
            + nDifferent
            + " different pixels in the images.\n"
            + error
            + "\n"
            + "See the differences in "
            + diffName
            + " .";
    if (nDifferent <= allowNPixelsDifferent) {
      String2.log("WARNING: " + msg);
      return "";
    } else {
      return msg;
    }
  }

  /**
   * This tests if fileName1 generates the same image as fileName2. If different, the differences
   * are saved in diffName.
   *
   * @param observed the full name of the image file to be tested (.gif, .jpg, or .png)
   * @param expected the full name of the expected/standard/correct image file (.gif, .jpg, or .png)
   * @param diffName the full name of a .png image file that will be created highlighting the
   *     differences between observed and expected.
   * @param allowNPixelsDifferent doesn't throw an Exception if nPixelsDifferent
   *     &lt;=allowNPixelsDifferent. Use 0 to test for true equality.
   * @throws Exception if the images are different or there is trouble
   */
  public static void testImagesIdentical(
      String observed,
      String expected,
      String diffName,
      int allowNPixelsDifferent,
      boolean autoCreateMissing,
      boolean displayImages)
      throws Exception {
    if (!Paths.get(observed).isAbsolute()) {
      observed = Paths.get(urlToAbsolutePath(OBS_DIR), observed).toString();
    }

    if (!Paths.get(expected).isAbsolute()) {
      expected = Paths.get(urlToAbsolutePath(TEST_DIR), expected).toString();
    }

    if (!Paths.get(diffName).isAbsolute()) {
      diffName = Paths.get(urlToAbsolutePath(DIFF_DIR), diffName).toString();
    }

    // if expected doesn't exist, save observed as expected?
    if (!File2.isFile(expected)) {
      if (displayImages) {
        Test.displayInBrowser("file://" + observed);
      }
      File2.appendFile("missingImage.txt", expected + "\n", File2.UTF_8);
      if (autoCreateMissing) {
        System.out.println("Expected image file doesn't exist, creating it from observed.");
        File2.copy(observed, expected);
      } else {
        if (String2.getStringFromSystemIn(
                "Error at\n"
                    + MustBe.getStackTrace()
                    + "testImagesIdentical: expected image file doesn't exist. Create it from observed (y/n)? ")
            .equals("y")) {
          File2.copy(observed, expected);
          return;
        }
        throw new RuntimeException("expectedFile=" + expected + " doesn't exist.");
      }
      return;
    }

    // if diffName not .png, throw exception
    if (!diffName.endsWith(".png"))
      throw new RuntimeException("diffName=" + diffName + " MUST end in .png .");

    // get the images
    Image obsImg = Image2.getImage(observed, 10000, false);
    Image expImg = Image2.getImage(expected, 10000, false);
    String error =
        Image2Tests.compareImages(
            obsImg, expImg, diffName, allowNPixelsDifferent); // error.length>0 if >
    // allowNPixelsDifferent
    if (error.length() == 0) return;
    if (displayImages) {
      Test.displayInBrowser("file://" + observed);
      Test.displayInBrowser("file://" + expected);
      if (File2.isFile(diffName)) Test.displayInBrowser("file://" + diffName);
    }
    throw new RuntimeException(
        "testImagesIdentical found differences:\n"
            + error
            + "\n"
            + "observed="
            + observed
            + "\n"
            + "expected="
            + expected);
  }
}
