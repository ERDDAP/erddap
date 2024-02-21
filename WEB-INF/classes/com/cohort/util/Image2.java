/* This file is Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.util;

import java.awt.Color;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.PixelGrabber;
import java.awt.image.RenderedImage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import java.net.URL;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import net.jmge.gif.Gif89Encoder;

//import net.sourceforge.jiu.color.dithering.ErrorDiffusionDithering;
//import net.sourceforge.jiu.color.quantization.MedianCutQuantizer;
//import net.sourceforge.jiu.color.quantization.OctreeColorQuantizer;
//import net.sourceforge.jiu.data.MemoryRGB24Image;
//import net.sourceforge.jiu.data.RGB24Image;
//import net.sourceforge.jiu.gui.awt.ImageCreator;
//import net.sourceforge.jiu.data.PixelImage;
/**
 * Image2 has useful static methods for working with images.
 */
public class Image2 {

    /** Java font drawing isn't consistent in minor ways.
     * Change this to change the sensitivity of Image2.compareImages().
     */
    public static final int DEFAULT_ALLOW_N_PIXELS_DIFFERENT = 250;
    public static final boolean DEFAULT_AUTO_CREATE_IF_MISSING = true;
    public static final boolean DEFAULT_DISPLAY_IMAGES = false;

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
            String2.log(MustBe.throwable("Image2.getImageFromUrl("
                + urlString + ")\n", e));
            return null;
        }
    }

    /**
     * This tries to load the specified image (directory + file name)
     *   (gif/jpg/png).
     *
     * @param fullFileName is the full file name (e.g., c:\mydir\image.gif)
     * @param waitMS the timeout time in milliseconds
     *   (2000 is recommended for small images)
     * @param javaShouldCache true if you want Java to cache this image (for
     *   fast access in future, but at the expense of memory)
     * @return the image. If unsuccessful, it returns null.
     */
    public static BufferedImage getImage(String fullFileName, int waitMS, 
            boolean javaShouldCache) {
        try {
            if (!File2.isFile(fullFileName)) {
                System.err.println("Image2.getImage: File not found: "
                    + fullFileName);
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
            String2.log(MustBe.throwable("Image2.getImage("
                    + fullFileName + ")\n", e));
            return null;
        }
    }

    /**
     * This is a variant of getImage which gets a file from default
     *    dir or .jar
     *
     * @param resourceName is the file name (e.g., /com/cohort/enof/image.gif)
     * @param waitMS the timeout time in milliseconds
     *   (2000 is recommended for small images)
     * @param javaShouldCache true if you want Java to cache this image (for
     *   fast access in future, but at the expense of memory)
     * @return the image. If unsuccessful, it returns null.
     */
    public static BufferedImage getImageFromResource(String resourceName, int waitMS, 
            boolean javaShouldCache) {     
        try {
            BufferedInputStream bis = new BufferedInputStream(
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
            String2.log(MustBe.throwable(
                "Image2.getImageFromResource(" + resourceName + ")\n", e));
            return null;
        }
    }

    /**
     * This waits for the image to load.
     *
     * @param image
     * @param waitMS the timeout time in milliseconds
     *   (2000 is recommended for small images)
     * @throws Exception if trouble (e.g., doesn't load in time)
     */
    public static void waitForImage(Image image, int waitMS) throws Exception {

        //wait until the image is completely loaded (or waitMS)
        int seconds10 = 0;
        int timeOut10 = waitMS / 100;
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        toolkit.prepareImage(image, -1, -1, null); //start loading
        while (seconds10 < timeOut10) {
            int flags = toolkit.checkImage(image, -1, -1, null);
            if ((flags & (ImageObserver.ERROR | ImageObserver.ABORT)) != 0)
                throw new Exception("Unable to load image (bad URL? invalid file?).");
            if ((flags & ImageObserver.ALLBITS) != 0)
                return;
            Thread.sleep(100);
            seconds10++;
        }
        if (seconds10 >= timeOut10) 
            throw new Exception("Timeout.");
    }

    /**
     * If icon is null, this returns the specified resource image as an icon.
     *
     * @param icon the initial value of the icon
     * @param resourceName for example, packagePath + "Error.gif"
     * @return icon (if it isn't null), else the specified resource image
     *      as an icon, else null if it isn't found
     */
    public static Icon getIcon(Icon icon, String resourceName) {
        if (icon != null) 
            return icon;
        
        Image image = getImageFromResource(resourceName, 3000, false);     
        if (image == null)
            return null;
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
    public static void saveAsJpeg(BufferedImage originalImage, 
            String fullFileName, float compressionQuality) throws Exception {

        OutputStream out = new BufferedOutputStream(
            new FileOutputStream(fullFileName));
        try {

            // Retrieve jpg image to be compressed
            //BufferedImage originalImage = ImageIO.read(infile);

            // Find a jpeg writer
            ImageWriter writer = null;
            try {
                Iterator iter = ImageIO.getImageWritersByFormatName("jpg");
                if (iter.hasNext()) {
                    writer = (ImageWriter)iter.next();
                }

                // Prepare output file
                ImageOutputStream ios = ImageIO.createImageOutputStream(out);
                try {
                    writer.setOutput(ios);

                    // Set the compression quality
                    ImageWriteParam iwparam = new JPEGImageWriteParam(Locale.getDefault());
                    iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT) ;
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
        } finally {
            out.close();
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

  // Returns the format name of the image in the object 'o'.
    // 'o' can be either a File or InputStream object.
    // Returns null if the format is not known.
    private static String getFormatName(Object o) {
        ImageInputStream iis = null;
        try {
            // Create an image input stream on the image
            iis = ImageIO.createImageInputStream(o);
    
            // Find all image readers that recognize the image format
            Iterator iter = ImageIO.getImageReaders(iis);
            if (!iter.hasNext()) {
                // No readers found
                iis.close();
                iis = null;
                return null;
            }
    
            // Use the first reader
            ImageReader reader = (ImageReader)iter.next();
    
            // Close stream
            iis.close();
            iis = null;
    
            // Return the format name
            return reader.getFormatName();
        } catch (Exception e) {
            if (iis != null)
                try {iis.close();} catch (Exception e2) {}
        }
        // The image could not be read
        return null;
    }

    
    // Returns true if the specified format name (e.g., "png") can be read
    public static boolean canReadFormat(String formatName) {
        Iterator iter = ImageIO.getImageReadersByFormatName(formatName);
        return iter.hasNext();
    }

    /**
     * Makes an array of int (one int per pixel) representing the image.
     *
     * @param img the image
     * @param width  the width of the image  (image.getWidth(null))
     * @param height the height of the image  (image.getHeight(null))
     * @param millis 10000 milliseconds is a good timeout
     * @return an array of integers representing the image
     * @throws Exception
     */
    public static int[] makeArrayFromImage(Image img, int width, int height, 
            int millis) throws Exception {
        int pixels[] = new int[width*height];
        //see Nutshell 2nd Ed, pg 369
        PixelGrabber grabber = new PixelGrabber(img,
            0, 0, width, height, pixels, 0, width); //0=offset, width=scansize
        grabber.grabPixels(millis); 
        return pixels;
    }

    /**
     * Given an array of ints (one int per pixel), this makes an image.
     *
     * @param ar is the array of ints
     * @param width  the width of the image  
     * @param height the height of the image 
     * @param millis 10000 milliseconds is a good timeout
     * @return a BufferedImage object
     * @throws Exception
     */
    public static BufferedImage makeImageFromArray(int ar[], int width, int height, 
            int millis) throws Exception {

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
     * This returns an image with the specified background color made 
     *   transparent.
     * 
     * @param image is the image
     * @param background the color to be made transparent
     * @param millis 10000 milliseconds is a good timeout
     * @return a BufferedImage object
     * @throws Exception
     */
    public static BufferedImage makeImageBackgroundTransparent(Image image, 
            Color background, int millis) throws Exception {

        int opaqueColor = 0xFF000000 | background.getRGB(); 
        int imageWidth  = image.getWidth(null);
        int imageHeight = image.getHeight(null);        
        int pixels[] = 
            makeArrayFromImage(image, imageWidth, imageHeight, millis);
        int widthHeight = imageWidth*imageHeight;
        for (int i = 0; i < widthHeight; i++)
            if (pixels[i] == opaqueColor) 
                pixels[i] =  0xFFFFFF; //transparent
        return makeImageFromArray(pixels, imageWidth, imageHeight, millis);
    }

    // TODO: Move this to test file after all uses of this are moved to test files.
    /**
     * This is like the other testImagesIdentical, but uses DEFAULT_ALLOW_N_PIXELS_DIFFERENT.
     *
     * @param observed the full name of the image file to be tested (.gif, .jpg, or .png)
     * @param expected the full name of the expected/standard/correct image file (.gif, .jpg, or .png)
     * @param diffName the full name of a .png image file that will be created
     *    highlighting the differences between observed and expected.
     * @throws Exception if the images are different or there is trouble
     */
    public static void testImagesIdentical(String observed, String expected, 
            String diffName) throws Exception {
        testImagesIdentical(observed, expected, diffName, DEFAULT_ALLOW_N_PIXELS_DIFFERENT, DEFAULT_AUTO_CREATE_IF_MISSING, DEFAULT_DISPLAY_IMAGES);
    }

    /**
     * This tests if fileName1 generates the same image as fileName2.
     * If different, the differences are saved in diffName.
     *
     * @param observed the full name of the image file to be tested (.gif, .jpg, or .png)
     * @param expected the full name of the expected/standard/correct image file (.gif, .jpg, or .png)
     * @param diffName the full name of a .png image file that will be created
     *    highlighting the differences between observed and expected.
     * @param allowNPixelsDifferent doesn't throw an Exception if nPixelsDifferent
     *    &lt;=allowNPixelsDifferent.  Use 0 to test for true equality.
     * @throws Exception if the images are different or there is trouble
     */
    public static void testImagesIdentical(String observed, String expected, 
            String diffName, int allowNPixelsDifferent, boolean autoCreateMissing, boolean displayImages) throws Exception {

        //if expected doesn't exist, save observed as expected?
        if (!File2.isFile(expected)) {
            Test.displayInBrowser("file://" + observed);
            if (autoCreateMissing) {
                System.out.println("Expected image file doesn't exist, creating it from observed.");
                File2.copy(observed, expected);
            }else {
                if (String2.getStringFromSystemIn("Error at\n" + MustBe.getStackTrace() + 
                    "testImagesIdentical: expected image file doesn't exist. Create it from observed (y/n)? ").equals("y")) {
                    File2.copy(observed, expected);
                    return;
                } 
                throw new RuntimeException("expectedFile=" + expected + " doesn't exist.");
            }
            return;
        }

        //if diffName not .png, throw exception
        if (!diffName.endsWith(".png"))
            throw new RuntimeException("diffName=" + diffName + " MUST end in .png .");
            
        //get the images
        Image obsImg = getImage(observed, 10000, false);
        Image expImg = getImage(expected, 10000, false);
        String error = compareImages(obsImg, expImg, diffName, allowNPixelsDifferent);  //error.length>0 if > allowNPixelsDifferent
        if (error.length() == 0)
            return;
        if (displayImages) {
            Test.displayInBrowser("file://" + observed);
            Test.displayInBrowser("file://" + expected);
            if (File2.isFile(diffName))
                Test.displayInBrowser("file://" + diffName);
        }
        throw new RuntimeException(
            "testImagesIdentical found differences:\n" +
            error + "\n" +
            "observed=" + observed + "\n" + 
            "expected=" + expected);
    }

    /**
     * This tests if image1 is the same as image2.
     * If different, the differences are saved in diffName.
     *
     * @param image1 an image
     * @param image2 an image
     * @param diffName the name of a .gif, .jpg, or .png image file
     *   that is sometimes created if there are differences
     * @param allowNPixelsDifferent doesn't throw an Exception if nPixelsDifferent
     *    &lt;=allowNPixelsDifferent.  Use 0 to test for true equality.
     * @return a string describing the differences ("" if no differences or
        nPixelsDifferent &lt;= COMPARE_ALLOW_N_PIXELS_DIFFERENT).
     * @throws Exception if serious trouble, e.g., images are null
     */
    public static String compareImages(Image image1, Image image2, 
            String diffName, int allowNPixelsDifferent) throws Exception {
        String cmd = "Image2.compareImages: ";

        //are they the same size?
        int width1  = image1.getWidth(null);
        int width2  = image2.getWidth(null);
        int height1 = image1.getHeight(null);
        int height2 = image2.getHeight(null);
        int widthHeight = width1 * height1;
        if (width1 != width2 || height1 != height2) 
            return cmd + "the sizes are not the same: " + 
                width1 + "x" + height1 + " " + width2 + "x" + height2;

        //grab the pixels
        int[] pixels1 = new int[widthHeight];
        PixelGrabber grabber =
            new PixelGrabber(image1, //see Nutshell pg 369
                0, 0, width1, height1, pixels1, //array to put pixels in
                0, //offset
                width1); //scansize
        grabber.grabPixels(10000); //try for up to 10 sec (huge files)

        int[] pixels2 = new int[widthHeight];
        grabber =
            new PixelGrabber(image2, //see Nutshell pg 369
                0, 0, width2, height2, pixels2, //array to put pixels in
                0, //offset
                width2); //scansize
        grabber.grabPixels(10000); //try for up to 10 sec (for huge files)

        //recover some memory
        image1 = null;
        image2 = null;

        //if different, make array of differences
        int[] pixels3 = new int[widthHeight];
        Arrays.fill(pixels3, 0xFF909090); //opaque gray
        int nDifferent = 0;
        int diff, lastDiff = 0, tMaxDiff, maxDiff = 0;
        String error = "";
        if (!Arrays.equals(pixels1, pixels2)) {
            for (int i = 0; i < widthHeight; i++) {
                diff = pixels1[i] - pixels2[i];

                if (diff != 0) {
                    if (diff != lastDiff) { //diagnostic
                        int aDiff = Math.abs((pixels1[i] >>> 24) - (pixels2[i] >>> 24));
                        int rDiff = Math.abs(((pixels1[i] >> 16) & 255) - ((pixels2[i] >> 16) & 255));
                        int gDiff = Math.abs(((pixels1[i] >> 8)  & 255) - ((pixels2[i] >> 8)  & 255));
                        int bDiff = Math.abs(((pixels1[i])       & 255) - ((pixels2[i])       & 255)); 
                        lastDiff = diff;
                        tMaxDiff = rDiff + gDiff + aDiff + bDiff;
                        if (tMaxDiff > maxDiff) {
                            error = cmd + "maxDiff: " +
                                aDiff + " " + rDiff + " " + gDiff + " " + bDiff +
                                " " + Integer.toHexString(pixels1[i]) +
                                " " + Integer.toHexString(pixels2[i]); 
                            maxDiff = tMaxDiff;
                        }
                    }
                    nDifferent++;
                    pixels3[i] = 0xFF000000 | Math.abs(diff); //make opaque
                }
            } 
        }
        if (nDifferent == 0)
            return "";

        //if different, save differences as an image file 
        RenderedImage image3 = makeImageFromArray(pixels3, width1, height1, 5000);
        saveAsPng(image3, diffName);
        String msg = cmd + "There were " + nDifferent + " different pixels in the images.\n" +
            error + "\n" +
            "See the differences in " + diffName + " .";
        if (nDifferent <= allowNPixelsDifferent) {
            String2.log("WARNING: " + msg);
            return "";
        } else {
            return msg;
        }
    }

    /**
     * Save an image as a .gif file
     */
/*    public static void saveAsGif(Image image, OutputStream os) {
        //from J.M.G. Elliott tep@jmge.net  0=no palette restriction
        Gif89Encoder ge = new Gif89Encoder(image, 0);
        ge.getFrameAt(0).setInterlaced(false);
        ge.encode(os);
    }
*/
    /**
     * Save an image as a .gif file. Handles transparent colors.
     * Throws Exception if &gt;256 colors.
     *
     * @param image
     * @param fullFileName (e.g., c:\myDir\myFile.gif)
     */
    public static void saveAsGif(Image image, String fullFileName) 
            throws Exception {
        //Gif89Encoder is free from http://jmge.net/java/gifenc/ 
        OutputStream out = new BufferedOutputStream(
            new FileOutputStream(fullFileName));
        try {
            (new Gif89Encoder(image)).encode(out);
        } finally {
            out.close();
        }
    }

    /**
     * Save an image as a .gif file constrained to standard web 216
     * color palette.
     *
     * @param image
     * @param fullFileName (e.g., c:\myDir\myFile.gif)
     * @param useDithering if false, it forces to nearest of 216 colors
     * @throws Exception if trouble
     */
    public static void saveAsGif216(Image image, String fullFileName, boolean useDithering) 
            throws Exception {
        //Gif89Encoder is free from http://jmge.net/java/gifenc/ 
        OutputStream out = new BufferedOutputStream(
            new FileOutputStream(fullFileName));
        try {
            (new Gif89Encoder(image, 
                useDithering? Gif89Encoder.DITHERED_WEB216_PALETTE: Gif89Encoder.WEB216_PALETTE)
                ).encode(out);
        } finally {
            out.close();
        }
    }

    /**
     * Save an image (with any number of colors) as a .png file.
     *
     * @param image may be a BufferedImage, too
     * @param fullFileName (e.g., c:\myDir\myFile.png)
     * @throws Exception if trouble
     */
    public static void saveAsPng(RenderedImage image, String fullFileName) 
            throws Exception {
        OutputStream out = new BufferedOutputStream(
            new FileOutputStream(fullFileName));
        try {
            ImageIO.write(image, "png", out);
        } finally {
            out.close();
        }
    }

    /**
     * Convert any rgb to a color in the standard 216 color web palette
     * (without dithering).
     * 
     * @param rgb  (the opacity value is ignored)
     * @return a color in the standard 216 color web palette 
     *    (6 levels each of r, g, b).
     */
    public static int rgbToWeb216Color(int rgb) {
        //0x19 (~1/2 of 0x33) rounds the result
        return (((rgb & 0xFF0000) + 0x190000) / 0x330000 * 0x330000) |
               (((rgb & 0xFF00  ) + 0x1900  ) / 0x3300   * 0x3300  ) |
               (((rgb & 0xFF    ) + 0x19    ) / 0x33     * 0x33    );
    }

    /**
     * Reduce the image to 216 colors.
     * This requires that the optional Java Advanced Imaging classes be installed
     * in the current JRE or JDK.
     *
     * @param bi a bufferedImage
     * @return a bufferedImage with 216 or fewer colors
     */ 
    //public static Image reduceTo216Colors(BufferedImage bi) throws Exception {
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

    //}
}    

