public class Image2 {

    /**
   * This tries to load the specified image (gif/jpg/png).
   *
   * @param urlString is the url string
   * @return the image. If unsuccessful, it returns null.
   */
  public static BufferedImage getImageFromURL(String urlString) {
    try {
      return ImageIO.read(new URL(urlString));

      /*
      Toolkit toolkit = Toolkit.getDefaultToolkit();
      URL url = new URL(urlString);
      Image image = javaShouldCache? toolkit.getImage(url) :
          toolkit.createImage(url);
      waitForImage(image, waitMS);
      return image;
      */

    } catch (Exception e) {
      String2.log(MustBe.throwable("Image2.getImageFromUrl(" + urlString + ")\n", e));
      return null;
    }
  }

    /**
       * Saves the image in a .jpg file.
       * compressionQuality ranges between 0 (lowest) and 1 (highest).
       * Originally from Java Almanac 1.4, but modified to be more flexible and
       * since JPEGImageWriteParam bug is fixed in Java 1.5.0.
       * See http://javaalmanac.com/egs/javax.imageio/JpegWrite.html .
       *
       * @param originalImage is the original image
       * @param fullFileName is the full name of the destination file
       * @param compressionQuality is 0 (lowest quality) to 1 (highest quality).
       * @throws Exception if trouble
       */
  public static void saveAsJpeg(
    BufferedImage originalImage, String fullFileName, float compressionQuality) throws Exception {

  try (OutputStream out = new BufferedOutputStream(new FileOutputStream(fullFileName))) {

    // Retrieve jpg image to be compressed
    // BufferedImage originalImage = ImageIO.read(infile);

    // Find a jpeg writer
    ImageWriter writer = null;
    try {
      Iterator iter = ImageIO.getImageWritersByFormatName("jpg");
      if (iter.hasNext()) {
        writer = (ImageWriter) iter.next();
      }

      // Prepare output file
      ImageOutputStream ios = ImageIO.createImageOutputStream(out);
      try {
        writer.setOutput(ios);

        // Set the compression quality
        ImageWriteParam iwparam = new JPEGImageWriteParam(Locale.getDefault());
        iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        iwparam.setCompressionQuality(compressionQuality);

        // Write the image
        writer.write(null, new IIOImage(originalImage, null, null), iwparam);

      } finally {
        // Cleanup
        ios.flush();
        ios.close();
      }
    } finally {
      writer.dispose();
    }
  }
}

/**
   * This waits for the image to load.
   *
   * @param image
   * @param waitMS the timeout time in milliseconds (2000 is recommended for small images)
   * @throws Exception if trouble (e.g., doesn't load in time)
   */
  public static void waitForImage(Image image, int waitMS) throws Exception {

    // wait until the image is completely loaded (or waitMS)
    int seconds10 = 0;
    int timeOut10 = waitMS / 100;
    Toolkit toolkit = Toolkit.getDefaultToolkit();
    toolkit.prepareImage(image, -1, -1, null); // start loading
    while (seconds10 < timeOut10) {
      int flags = toolkit.checkImage(image, -1, -1, null);
      if ((flags & (ImageObserver.ERROR | ImageObserver.ABORT)) != 0)
        throw new Exception("Unable to load image (bad URL? invalid file?).");
      if ((flags & ImageObserver.ALLBITS) != 0) return;
      Thread.sleep(100);
      seconds10++;
    }
    if (seconds10 >= timeOut10) throw new Exception("Timeout.");
  }

  /**
   * If icon is null, this returns the specified resource image as an icon.
   *
   * @param icon the initial value of the icon
   * @param resourceName for example, packagePath + "Error.gif"
   * @return icon (if it isn't null), else the specified resource image as an icon, else null if it
   *     isn't found
   */
  public static Icon getIcon(Icon icon, String resourceName) {
    if (icon != null) return icon;

    Image image = getImageFromResource(resourceName, 3000, false);
    if (image == null) return null;
    else return new ImageIcon(image);
  }

   /* *
   * This tries to load the specified image (gif/jpg/png).
   *
   * @param inputStream is an inputStream
   * @return the image. If unsuccessful, it returns null.
   */
  /*
  known Java bugs: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5098176 and 4881314
       public static BufferedImage getImageFromStream(InputStream inputStream) {
          try {
              return ImageIO.read(inputStream);
          } catch (Exception e) {
              System.err.println("Image2.getImage from inputStream");
              String2.log.fine(MustBe.throwableToString(e));
              return null;
          }
      }

      /* *
       * This tries to load the specified file (gif/jpg/png).
       *
       * @param fileName
       * @return the image. If unsuccessful, it returns null.
       */
  /*
  known Java bugs: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5098176 and 4881314
       public static BufferedImage getImageFromFile(String fileName) {
          try {
              return ImageIO.read(File2.getDecompressedBufferedInputStream(fileName));
          } catch (Exception e) {
              System.err.println("Image2.getImage from inputStream");
              String2.log.fine(MustBe.throwableToString(e));
              return null;
          }
      }

      /* *
       * This is a variant of getImage which gets a file from the default
       *    dir or .jar, or from a url
       *
       * @param resourceName is the resource name (e.g., /com/cohort/enof/image.gif)
       *     or url (e.g., "http://hostname.com/image.gif")
       * @return the image. If unsuccessful, it returns null.
       */
  /*
  known Java bugs: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5098176 (now fixed) and 4881314 (fixed)
       public static BufferedImage getImageFromResource(String resourceName) {
          try {
              BufferedInputStream bis = new BufferedInputStream(
                  ClassLoader.getSystemClassLoader().getResourceAsStream(resourceName));
              System.err.println("Image2.getImageFromResource isNull? " + (bis == null) +
                  " formatName=" + getFormatName(bis) + " can read png? " + canReadFormat("PNG"));
              return ImageIO.read(bis);
          } catch (Exception e) {
              System.err.println("Image2.getImageFromResource(" + resourceName + ")");
              String2.log.fine(MustBe.throwableToString(e));
              return null;
          }
      }

  /*
  // This class overwrites the setCompressionQuality() method to workaround
  // a problem in compressing JPEG images using the javax.imageio package.
  public class MyImageWriteParam extends JPEGImageWriteParam {
      public MyImageWriteParam() {
          super(Locale.getDefault());
      }

      // This method accepts quality levels between 0 (lowest) and 1 (highest) and simply converts
      // it to a range between 0 and 256; this is not a correct conversion algorithm.
      // However, a proper alternative is a lot more complicated.
      // This should do until the bug is fixed.
      public void setCompressionQuality(float quality) {
          if (quality < 0.0F || quality > 1.0F) {
              throw new IllegalArgumentException("Quality out-of-bounds!");
          }
          this.compressionQuality = 256 - (quality * 256);
      }
  }
  */

  // Returns true if the specified format name (e.g., "png") can be read
  public static boolean canReadFormat(String formatName) {
    Iterator iter = ImageIO.getImageReadersByFormatName(formatName);
    return iter.hasNext();
  }

  /**
   * Convert any rgb to a color in the standard 216 color web palette (without dithering).
   *
   * @param rgb (the opacity value is ignored)
   * @return a color in the standard 216 color web palette (6 levels each of r, g, b).
   */
  public static int rgbToWeb216Color(int rgb) {
    // 0x19 (~1/2 of 0x33) rounds the result
    return (((rgb & 0xFF0000) + 0x190000) / 0x330000 * 0x330000)
        | (((rgb & 0xFF00) + 0x1900) / 0x3300 * 0x3300)
        | (((rgb & 0xFF) + 0x19) / 0x33 * 0x33);
  }

  /**
   * Reduce the image to 216 colors. This requires that the optional Java Advanced Imaging classes
   * be installed in the current JRE or JDK.
   *
   * @param bi a bufferedImage
   * @return a bufferedImage with 216 or fewer colors
   */
  // public static Image reduceTo216Colors(BufferedImage bi) throws Exception {
  /* //the JAI way
  // Create a color map with the 4-9-6 color cube and the
  // Floyd-Steinberg error kernel.
  java.awt.image.renderable.ParameterBlock pb = new java.awt.image.renderable.ParameterBlock();
  pb.addSource(bi);
  pb.add(javax.media.jai.ColorCube.BYTE_496);
  pb.add(javax.media.jai.KernelJAI.ERROR_FILTER_FLOYD_STEINBERG);

  // Perform the error diffusion operation.
  return ((javax.media.jai.PlanarImage)javax.media.jai.JAI.create("errordiffusion", pb, null)).getAsBufferedImage();
  */

  /*
  //a JIU way
  RGB24Image image = ImageCreator.convertImageToRGB24Image(bi);
  OctreeColorQuantizer quantizer = new OctreeColorQuantizer();
  quantizer.setInputImage(image);
  quantizer.setPaletteSize(255);
  quantizer.init();
  ErrorDiffusionDithering edd = new ErrorDiffusionDithering();
  edd.setTemplateType(ErrorDiffusionDithering.TYPE_STUCKI);
  edd.setQuantizer(quantizer);
  edd.setInputImage(image);
  edd.process();
  PixelImage quantizedImage = edd.getOutputImage();
  return ImageCreator.convertToAwtImage(quantizedImage, ImageCreator.DEFAULT_ALPHA);
  // */

  /*
  //a JIU way
  //median cut supposed to be better, but leads to grays!
  RGB24Image image = ImageCreator.convertImageToRGB24Image(bi);
  MedianCutQuantizer quantizer = new MedianCutQuantizer();
  quantizer.setInputImage(image);
  quantizer.setPaletteSize(256);
  quantizer.process();
  ErrorDiffusionDithering edd = new ErrorDiffusionDithering();
  edd.setTemplateType(ErrorDiffusionDithering.TYPE_STUCKI);
  edd.setQuantizer(quantizer);
  edd.setInputImage(image);
  edd.process();
  PixelImage quantizedImage = edd.getOutputImage();
  return ImageCreator.convertToAwtImage(quantizedImage, ImageCreator.DEFAULT_ALPHA);
  // */

  /*
  //a JIU way
  //median cut supposed to be better, but leads to grays!
  RGB24Image image = ImageCreator.convertImageToRGB24Image(bi);
  MedianCutQuantizer quantizer = new MedianCutQuantizer();
  quantizer.setInputImage(image);
  quantizer.setPaletteSize(256);
  quantizer.process();
  PixelImage quantizedImage = quantizer.getOutputImage();
  return ImageCreator.convertToAwtImage(quantizedImage, ImageCreator.DEFAULT_ALPHA);
  // */

  // }
    
}
