/* This file is Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.util;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.BufferedInputStream;
import java.io.File;
import javax.imageio.ImageIO;

// import net.sourceforge.jiu.color.dithering.ErrorDiffusionDithering;
// import net.sourceforge.jiu.color.quantization.MedianCutQuantizer;
// import net.sourceforge.jiu.color.quantization.OctreeColorQuantizer;
// import net.sourceforge.jiu.data.MemoryRGB24Image;
// import net.sourceforge.jiu.data.RGB24Image;
// import net.sourceforge.jiu.gui.awt.ImageCreator;
// import net.sourceforge.jiu.data.PixelImage;
/** Image2 has useful static methods for working with images. */
public class Image2 {

  /**
   * Java font drawing isn't consistent in minor ways. Change this to change the sensitivity of
   * Image2.compareImages().
   */
  public static final int DEFAULT_ALLOW_N_PIXELS_DIFFERENT = 250;

  public static final boolean DEFAULT_AUTO_CREATE_IF_MISSING = true;
  public static final boolean DEFAULT_DISPLAY_IMAGES = false;

  /**
   * This tries to load the specified image (directory + file name) (gif/jpg/png).
   *
   * @param fullFileName is the full file name (e.g., c:\mydir\image.gif)
   * @param waitMS the timeout time in milliseconds (2000 is recommended for small images)
   * @param javaShouldCache true if you want Java to cache this image (for fast access in future,
   *     but at the expense of memory)
   * @return the image. If unsuccessful, it returns null.
   */
  public static BufferedImage getImage(String fullFileName, int waitMS, boolean javaShouldCache) {
    try {
      if (!File2.isFile(fullFileName)) {
        System.err.println("Image2.getImage: File not found: " + fullFileName);
        return null;
      }

      return ImageIO.read(new File(fullFileName));

      /*
      Toolkit toolkit = Toolkit.getDefaultToolkit();
      Image image= javaShouldCache? toolkit.getImage(fullFileName) :
          toolkit.createImage(fullFileName);
      waitForImage(image, waitMS);
      return image;
      */

    } catch (Exception e) {
      String2.log(MustBe.throwable("Image2.getImage(" + fullFileName + ")\n", e));
      return null;
    }
  }

  /**
   * This is a variant of getImage which gets a file from default dir or .jar
   *
   * @param resourceName is the file name (e.g., /com/cohort/enof/image.gif)
   * @param waitMS the timeout time in milliseconds (2000 is recommended for small images)
   * @param javaShouldCache true if you want Java to cache this image (for fast access in future,
   *     but at the expense of memory)
   * @return the image. If unsuccessful, it returns null.
   */
  public static BufferedImage getImageFromResource(
      String resourceName, int waitMS, boolean javaShouldCache) {
    try {
      BufferedInputStream bis =
          new BufferedInputStream(
              ClassLoader.getSystemClassLoader().getResourceAsStream(resourceName));
      return ImageIO.read(bis);

      /*
      URL url = Image2.class.getResource(resourceName);
      if (url == null)
          return null;
      Toolkit toolkit = Toolkit.getDefaultToolkit();
      Image image = javaShouldCache? toolkit.getImage(url) :
          toolkit.createImage(url);
      waitForImage(image, waitMS);
      return image;
      */

    } catch (Exception e) {
      String2.log(MustBe.throwable("Image2.getImageFromResource(" + resourceName + ")\n", e));
      return null;
    }
  }

  /**
   * Makes an array of int (one int per pixel) representing the image.
   *
   * @param img the image
   * @param width the width of the image (image.getWidth(null))
   * @param height the height of the image (image.getHeight(null))
   * @param millis 10000 milliseconds is a good timeout
   * @return an array of integers representing the image
   * @throws Exception
   */
  public static int[] makeArrayFromImage(Image img, int width, int height, int millis)
      throws Exception {
    int pixels[] = new int[width * height];
    // see Nutshell 2nd Ed, pg 369
    PixelGrabber grabber =
        new PixelGrabber(img, 0, 0, width, height, pixels, 0, width); // 0=offset, width=scansize
    grabber.grabPixels(millis);
    return pixels;
  }

  /**
   * Given an array of ints (one int per pixel), this makes an image.
   *
   * @param ar is the array of ints
   * @param width the width of the image
   * @param height the height of the image
   * @param millis 10000 milliseconds is a good timeout
   * @return a BufferedImage object
   * @throws Exception
   */
  public static BufferedImage makeImageFromArray(int ar[], int width, int height, int millis)
      throws Exception {

    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    image.setRGB(0, 0, width, height, ar, 0, width);
    return image;

    /* 2022-02-28 was:
    ImageProducer ip = new MemoryImageSource(width, height, ar, 0, width);
    Toolkit toolkit = Toolkit.getDefaultToolkit();
    Image image = toolkit.createImage(ip);
    int seconds10 = 0;
    int timeout = millis/100;
    while (seconds10++ < timeout
            && !toolkit.prepareImage(image, width, height, null)) {
        Math2.sleep(100);
        seconds10++;
    }
    if (seconds10 == timeout)
        throw new Exception("Image2.makeImageFromArray: Timeout while making image: " +
            width + "x" + height);
    return image;
    */
  }

  /**
   * This returns an image with the specified background color made transparent.
   *
   * @param image is the image
   * @param background the color to be made transparent
   * @param millis 10000 milliseconds is a good timeout
   * @return a BufferedImage object
   * @throws Exception
   */
  public static BufferedImage makeImageBackgroundTransparent(
      Image image, Color background, int millis) throws Exception {

    int opaqueColor = 0xFF000000 | background.getRGB();
    int imageWidth = image.getWidth(null);
    int imageHeight = image.getHeight(null);
    int pixels[] = makeArrayFromImage(image, imageWidth, imageHeight, millis);
    int widthHeight = imageWidth * imageHeight;
    for (int i = 0; i < widthHeight; i++)
      if (pixels[i] == opaqueColor) pixels[i] = 0xFFFFFF; // transparent
    return makeImageFromArray(pixels, imageWidth, imageHeight, millis);
  }

  /** Save an image as a .gif file */
  /*    public static void saveAsGif(Image image, OutputStream os) {
          //from J.M.G. Elliott tep@jmge.net  0=no palette restriction
          Gif89Encoder ge = new Gif89Encoder(image, 0);
          ge.getFrameAt(0).setInterlaced(false);
          ge.encode(os);
      }
  */
}
