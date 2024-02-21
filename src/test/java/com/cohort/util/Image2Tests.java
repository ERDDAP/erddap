package com.cohort.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.io.TempDir;

class Image2Tests {

  @TempDir
  private static Path TEMP_DIR;

  @org.junit.jupiter.api.Test
  /**
   * This intentionally throws an Exception to test testImagesIdentical().
   */
  void testTestImagesIdentical() throws Exception {
    String2.log("\n*** Image2.testTestImagesIdentical");

    // test images which are identical
    String testDir = Image2Tests.class.getResource("/images/").getPath();
    String tempDir = TEMP_DIR.toAbsolutePath().toString();
    Image2.testImagesIdentical(
        testDir + "/testImagesIdentical_1.png",
        testDir + "/testImagesIdentical_1.png",
        tempDir + "/testImagesIdentical_diff.png");

    // test images which aren't identical
    try {

      // one time: createImage with transparent background
      // BufferedImage image2 = Image2.getImage(testDir + "testImagesIdentical_1.png",
      // 2000, false);
      // image2 = Image2.makeImageBackgroundTransparent(image2, Color.white, 10000);
      // Image2.saveAsPng(image2, testDir + "testImagesIdentical_2.png");

      // test images which aren't identical
      Image2.testImagesIdentical(
          testDir + "/testImagesIdentical_1.png",
          testDir + "/testImagesIdentical_2.png",
          tempDir + "/testImagesIdentical_diff.png");
    } catch (Exception e) {
      return;
    }
    throw new RuntimeException("shouldn't get here");
  }

  @org.junit.jupiter.api.Test
  /**
   * Test the methods in Image2.
   */
  void basicTest() throws Exception {
    String2.log("\n*** Image2.basicTest");

    String imageDir = Image2Tests.class.getResource("/images/").getPath();

    // test ImageIO
    BufferedImage bi = ImageIO.read(new File(imageDir + "testmap.gif"));
    Graphics g = bi.getGraphics();
    ImageIO.write(bi, "png", new File(imageDir + "temp.png"));
    Image2.saveAsGif(bi, imageDir + "temp.gif");

    long localTime = System.currentTimeMillis();
    String2.log("test() here 1");
    Color gray = new Color(128, 128, 128);
    String2.log("test() here 2=" + (System.currentTimeMillis() - localTime));
    Image image = Image2.getImage(imageDir + "temp.gif",
        10000, false); // javaShouldCache
    String2.log("test() here 3=" + (System.currentTimeMillis() - localTime));

    // convert 128,128,128 to transparent
    image = Image2.makeImageBackgroundTransparent(image, gray, 10000);
    String2.log("test() here 4=" + (System.currentTimeMillis() - localTime));

    // save as gif again
    Image2.saveAsGif(image, imageDir + "temp2.gif");
    String2.log("test() here 5=" + (System.currentTimeMillis() - localTime));

    File2.delete(imageDir + "temp.png");
    File2.delete(imageDir + "temp.gif");
    File2.delete(imageDir + "temp2.gif");

  }
}
