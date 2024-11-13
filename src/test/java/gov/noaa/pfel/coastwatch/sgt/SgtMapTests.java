package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.Image2Tests;
import com.cohort.util.Math2;
import com.cohort.util.ResourceBundle2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.google.common.io.Resources;
import gov.noaa.pfel.coastwatch.griddata.Grid;
import gov.noaa.pmel.sgt.Axis;
import gov.noaa.pmel.sgt.CartesianGraph;
import gov.noaa.pmel.sgt.GridAttribute;
import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.Layer;
import gov.noaa.pmel.sgt.LineAttribute;
import gov.noaa.pmel.sgt.StackedLayout;
import gov.noaa.pmel.sgt.dm.SimpleGrid;
import gov.noaa.pmel.util.Dimension2D;
import gov.noaa.pmel.util.Point2D;
import gov.noaa.pmel.util.Range2D;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import tags.TagImageComparison;
import testDataset.Initialization;

class SgtMapTests {
  private static final String testImageExtension = ".png";

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /** This tests SgtMap making bathymetry maps. 0, 11 */
  @org.junit.jupiter.api.Test
  @TagImageComparison
  void testBathymetry() throws Exception {
    // verbose = true;
    // reallyVerbose = true;
    // Grid.verbose = true;
    // String2.log("*** test SgtMap.testBathymetry");

    // 0, 12 9 is imperfect but unreasonable request
    int first = 0;
    int last = 12;

    for (int region = first; region <= last; region++) {
      BufferedImage bufferedImage = SgtUtil.getBufferedImage(480, 640);
      testBathymetryMap(
          true, (Graphics2D) bufferedImage.getGraphics(), 0, 0, 480, 480, region, 0, 1);
      String fileName = "SgtMapTestBathymetry" + region;
      String tName =
          Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR) + fileName + testImageExtension;
      File2.delete(tName); // old version? delete it
      SgtUtil.saveImage(bufferedImage, tName);
      // Test.displayInBrowser("file://" + tName);
      Image2Tests.testImagesIdentical(tName, fileName + ".png", fileName + "_diff.png");
    }
  }

  /** This tests SgtMap making topography maps. (0, 11) */
  @org.junit.jupiter.api.Test
  @TagImageComparison
  void testTopography() throws Exception {
    // verbose = true;
    // reallyVerbose = true;
    // Grid.verbose = true;
    // String2.log("*** test SgtMap.testTopography");

    // 0, 12 6 fails because of Known Problem with island
    int first = 0;
    int last = 12;

    for (int region = first; region <= last; region++) {
      BufferedImage bufferedImage = SgtUtil.getBufferedImage(480, 640);
      testBathymetryMap(
          false, (Graphics2D) bufferedImage.getGraphics(), 0, 0, 480, 480, region, 0, 1);
      String baseName = "SgtMapTestTopography" + region;
      String tName =
          Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR) + baseName + testImageExtension;
      File2.delete(tName); // old version? delete it
      SgtUtil.saveImage(bufferedImage, tName);
      // String2.log("displaying " + tName);
      // Test.displayInBrowser("file://" + tName);
      Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");
    }
  }

  /**
   * Make an image (currently a .png, usually in /images dir) with a map of the entire region and
   * labeled boxes for each of the regions. The resulting image will be no larger than maxGifWidth,
   * maxGifHeight.
   *
   * @param maxGifWidth (in pixels)
   * @param maxGifHeight
   * @param regionInfo info about the regions in the form String[region#][info], where info is:
   *     RectangleColor = 0, MinX degrees = 1, MaxX degrees = 2, MinY degrees = 3, MaxY degrees = 4,
   *     LeftLabelX degrees = 5, LowerLabelY degrees = 6, Label text = 7.
   * @param resultDir the directory for the files (e.g., <context>/images/ with a slash at the end)
   * @param resultFileName the output file name with the extension (e.g., "USMexicoRegion.png").
   *     Extension can be ".gif" or ".png". If the file exists, it will be overwritten.
   * @return int[], 0=imageWidth, 1=imageHeight, 2=graphULX, 3=graphRightX, 4=graphUpperY,
   *     5=graphLowerY.
   * @throws Exception if trouble
   */
  private static int[] makeRegionsMap(
      int maxGifWidth,
      int maxGifHeight,
      String regionInfo[][],
      String resultDir,
      String resultFileName)
      throws Exception {
    if (SgtMap.verbose)
      String2.log("\nSSR.makeRegionsMap(" + resultDir + ", " + resultFileName + ")");
    int randomInt = Math2.random(Integer.MAX_VALUE);
    String fullTmpPngFile = resultDir + randomInt + ".png";
    String fullTmpGifFile = resultDir + randomInt + ".gif";
    String fullImageFileName = resultDir + resultFileName;

    File2.delete(fullImageFileName); // delete any old version of the result file

    int imageWidth = maxGifWidth; // it may be revised below
    int imageHeight = maxGifHeight;

    // get region info
    // fields in regionInfo[][x]
    int COLOR = 0;
    int MINX = 1;
    int MAXX = 2;
    int MINY = 3;
    int MAXY = 4;
    int LLLABELX = 5;
    int LLLABELY = 6;
    int LABEL = 7;

    // the first region must be the biggest
    double minX = String2.parseDouble(regionInfo[0][MINX]);
    double maxX = String2.parseDouble(regionInfo[0][MAXX]);
    double minY = String2.parseDouble(regionInfo[0][MINY]);
    double maxY = String2.parseDouble(regionInfo[0][MAXY]);
    double xRange = maxX - minX;
    double yRange = maxY - minY;

    // determine appropriate axis lengths
    // int coordinates are in pixels (theoretically 1/100th inch)
    // note  graphHeight/yRange = graphWidth/xRange
    int graphULX = 20; // in pixels
    int graphBottomY = 20;
    int graphULY = 15;
    int graphRightBorder = 15;
    int graphWidth = imageWidth - graphULX - graphRightBorder;
    int graphHeight = imageHeight - graphBottomY - graphULY + 1;
    double tempXScale = graphWidth / xRange;
    double tempYScale = graphHeight / yRange;
    double graphScale = Math.min(tempXScale, tempYScale);
    if (tempXScale < tempYScale) {
      graphHeight = Math2.roundToInt(graphScale * yRange);
      imageHeight = graphBottomY + graphHeight + graphULY;
    } else {
      graphWidth = Math2.roundToInt(graphScale * xRange);
      imageWidth = graphULX + graphWidth + graphRightBorder;
    }

    // make the image
    BufferedImage bi =
        new BufferedImage(
            imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB); // I need opacity "A"
    Graphics g = bi.getGraphics();
    g.setColor(Color.white); // I'm not sure why necessary, but it is
    g.fillRect(0, 0, imageWidth, imageHeight); // I'm not sure why necessary, but it is

    double maxRange = Math.max(xRange, yRange);
    double fontScale = 1;
    int boundaryResAdjust = 0;
    double majorIncrement = SgtMap.suggestMajorIncrement(maxRange, imageWidth - 30, fontScale);
    double minorIncrement = SgtMap.suggestMinorIncrement(maxRange, imageWidth - 30, fontScale);
    int boundaryResolution =
        SgtMap.suggestBoundaryResolution(maxRange, imageWidth - 30, boundaryResAdjust);
    if (SgtMap.reallyVerbose) String2.log("  boundaryResolution=" + boundaryResolution);

    // create the pane
    JPane jPane = new JPane("", new java.awt.Dimension(imageWidth, imageHeight));
    jPane.setLayout(new StackedLayout());
    StringArray layerNames = new StringArray();
    CartesianGraph graph;
    Layer layer;

    // create the graph parts
    // graph's physical location (inches) (start, end, delta); delta is ignored
    Range2D xPhysRange = new Range2D(graphULX / 100.0, (imageWidth - graphRightBorder) / 100.0, 1);
    Range2D yPhysRange = new Range2D(graphBottomY / 100.0, (imageHeight - graphULY) / 100.0, 1);
    // graph's axis ranges in degrees
    Range2D xUserRange = new Range2D(minX, maxX, majorIncrement);
    Range2D yUserRange = new Range2D(minY, maxY, majorIncrement);
    gov.noaa.pmel.sgt.LinearTransform xt =
        new gov.noaa.pmel.sgt.LinearTransform(xPhysRange, xUserRange);
    gov.noaa.pmel.sgt.LinearTransform yt =
        new gov.noaa.pmel.sgt.LinearTransform(yPhysRange, yUserRange);
    Point2D.Double origin = new Point2D.Double(xUserRange.start, yUserRange.start);
    Dimension2D layerDimension2D = new Dimension2D(imageWidth / 100.0, imageHeight / 100.0);

    // draw bathymetry colors
    boolean plotBathymetryColors = false;
    if (plotBathymetryColors) {
      Grid bathymetryGrid =
          SgtMap.createTopographyGrid(
              SgtMap.fullPrivateDirectory, minX, maxX, minY, maxY, graphWidth, graphHeight);
      URL resourceFile =
          Resources.getResource("gov/noaa/pfel/coastwatch/sgt/" + SgtMap.bathymetryCpt);
      CompoundColorMap oceanColorMap = new CompoundColorMap(resourceFile);
      graph = new CartesianGraph("", xt, yt);
      layer = new Layer("bathymetryColors", layerDimension2D);
      layerNames.add(layer.getId());
      jPane.add(layer); // calls layer.setPane(this);
      layer.setGraph(graph); // calls graph.setLayer(this);
      graph.setClip(
          xUserRange.start, xUserRange.end,
          yUserRange.start, yUserRange.end);
      graph.setClipping(true);

      // get the Grid
      SimpleGrid simpleGrid =
          new SimpleGrid(bathymetryGrid.data, bathymetryGrid.lon, bathymetryGrid.lat, ""); // title

      // assign the data
      graph.setData(simpleGrid, new GridAttribute(GridAttribute.RASTER, oceanColorMap));
    }

    // draw the landMask
    graph = new CartesianGraph("", xt, yt);
    layer = new Layer("landmask", layerDimension2D);
    layerNames.add(layer.getId());
    jPane.add(layer); // calls layer.setPane(this);
    layer.setGraph(graph); // calls graph.setLayer(this);
    // assign the data   (PathCartesionRenderer always clips by itself)
    graph.setRenderer(
        new PathCartesianRenderer(
            graph,
            GSHHS.getGeneralPath(
                GSHHS.RESOLUTIONS.charAt(boundaryResolution),
                1, // just get land info
                minX,
                maxX,
                minY,
                maxY,
                true),
            1e-6,
            SgtMap.landColor,
            plotBathymetryColors
                ? SgtMap.landMaskStrokeColor
                : SgtMap.landColor)); // strokeColor  2009-10-29 landColor was null

    // draw lakes
    /* It works, but don't change to this unless needed.
    if (boundaryResolution != CRUDE_RESOLUTION) {
        graph = new CartesianGraph("", xt, yt);
        layer = new Layer("lakes", layerDimension2D);
        layerNames.add(layer.getId());
        jPane.add(layer);      //calls layer.setPane(this);
        layer.setGraph(graph); //calls graph.setLayer(this);
        //assign the data   (PathCartesionRenderer always clips by itself)
        graph.setRenderer(new PathCartesianRenderer(graph,
            GSHHS.getGeneralPath(
                GSHHS.RESOLUTIONS.charAt(boundaryResolution),
                2, //just get lakes info
                minX, maxX, minY, maxY, true),
            1e-6, lakesColor,  //fillColor
            lakesColor)); //strokeColor
    }
    */

    // draw the state boundary
    LineAttribute lineAttribute;
    if (SgtMap.drawPoliticalBoundaries && boundaryResolution != SgtMap.CRUDE_RESOLUTION) {
      graph = new CartesianGraph("", xt, yt);
      layer = new Layer("state", layerDimension2D);
      layerNames.add(layer.getId());
      jPane.add(layer); // calls layer.setPane(this);
      layer.setGraph(graph); // calls graph.setLayer(this);
      graph.setClip(
          xUserRange.start, xUserRange.end,
          yUserRange.start, yUserRange.end);
      graph.setClipping(true);
      lineAttribute = new LineAttribute();
      lineAttribute.setColor(SgtMap.statesColor);
      graph.setData(
          SgtMap.stateBoundaries.getSgtLine(boundaryResolution, minX, maxX, minY, maxY),
          lineAttribute);
    }

    // draw the national boundary
    graph = new CartesianGraph("", xt, yt);
    graph.setClip(
        xUserRange.start, xUserRange.end,
        yUserRange.start, yUserRange.end);
    graph.setClipping(true);
    if (SgtMap.drawPoliticalBoundaries) {
      layer = new Layer("national", layerDimension2D);
      layerNames.add(layer.getId());
      jPane.add(layer); // calls layer.setPane(this);
      layer.setGraph(graph); // calls graph.setLayer(this);
      lineAttribute = new LineAttribute();
      lineAttribute.setColor(SgtMap.nationsColor);
      graph.setData(
          SgtMap.nationalBoundaries.getSgtLine(boundaryResolution, minX, maxX, minY, maxY),
          lineAttribute);
    }

    // create the x axes
    Font myLabelFont = new Font(SgtMap.fontFamily, Font.PLAIN, 9);
    PlainAxis2 xAxis = new PlainAxis2(new GenEFormatter());
    xAxis.setRangeU(xUserRange);
    xAxis.setLocationU(origin);
    int nSmallTics = Math2.roundToInt(majorIncrement / minorIncrement) - 1;
    xAxis.setNumberSmallTics(nSmallTics);
    xAxis.setLabelInterval(1);
    xAxis.setLabelFont(myLabelFont);
    xAxis.setLabelFormat("%g°");
    xAxis.setLabelHeightP(.12);
    xAxis.setSmallTicHeightP(.02);
    xAxis.setLargeTicHeightP(.05);

    PlainAxis2 topXAxis = new PlainAxis2(new GenEFormatter());
    topXAxis.setRangeU(xUserRange);
    topXAxis.setLocationU(new Point2D.Double(xUserRange.start, yUserRange.end));
    topXAxis.setSmallTicHeightP(0);
    topXAxis.setLargeTicHeightP(0);
    topXAxis.setLabelPosition(Axis.NO_LABEL);

    // create the y axes
    PlainAxis2 yAxis = new PlainAxis2(new GenEFormatter());
    yAxis.setRangeU(yUserRange);
    yAxis.setLocationU(origin);
    yAxis.setNumberSmallTics(nSmallTics);
    yAxis.setLabelInterval(1);
    yAxis.setLabelFont(myLabelFont);
    yAxis.setLabelFormat("%g°");
    yAxis.setLabelHeightP(.12);
    yAxis.setSmallTicHeightP(.02);
    yAxis.setLargeTicHeightP(.05);

    PlainAxis2 rightYAxis = new PlainAxis2(new GenEFormatter());
    rightYAxis.setRangeU(yUserRange);
    rightYAxis.setLocationU(new Point2D.Double(xUserRange.end, yUserRange.start));
    rightYAxis.setSmallTicHeightP(0);
    rightYAxis.setLargeTicHeightP(0);
    rightYAxis.setLabelPosition(Axis.NO_LABEL);

    graph.addXAxis(xAxis);
    graph.addXAxis(topXAxis);
    graph.addYAxis(yAxis);
    graph.addYAxis(rightYAxis);

    // draw the graph background color right before drawing the graph
    int x1 = graph.getXUtoD(xUserRange.start);
    int y1 = graph.getYUtoD(yUserRange.end);
    int x2, y2;
    g.setColor(SgtMap.oceanColor);
    g.fillRect(x1, y1, graphWidth, graphHeight);
    g.setColor(Color.black);

    // actually draw the graph
    jPane.draw(g);

    // draw the region rectangles
    for (int region = 0; region < regionInfo.length; region++) {
      g.setColor(new Color(String2.parseInt(regionInfo[region][COLOR]), true));
      x1 = graph.getXUtoD(String2.parseDouble(regionInfo[region][MINX]));
      x2 = graph.getXUtoD(String2.parseDouble(regionInfo[region][MAXX]));
      y1 = graph.getYUtoD(String2.parseDouble(regionInfo[region][MINY]));
      y2 = graph.getYUtoD(String2.parseDouble(regionInfo[region][MAXY]));
      g.fillRect(x1, y2, x2 - x1, y1 - y2); // remember image y's are flipped
      for (int i = 0; i < 5; i++) // draw edge a few times so highlighted
      g.drawRect(x1, y2, x2 - x1, y1 - y2);
    }

    // draw the region text
    g.setColor(Color.black);
    g.setFont(myLabelFont);
    for (int region = 0; region < regionInfo.length; region++) {
      g.drawString(
          regionInfo[region][LABEL],
          graph.getXUtoD(String2.parseDouble(regionInfo[region][LLLABELX])),
          graph.getYUtoD(String2.parseDouble(regionInfo[region][LLLABELY])));
    }

    // deconstruct jPane
    SgtMap.deconstructJPane("SgtMap.makeRegionsMap", jPane, layerNames);

    // save as .png
    // use png (not bmp) because png supports ARGB images

    // "convert" to .gif   (or save as fullImageFileName .png)
    if (fullImageFileName.endsWith(".gif")) {
      // SSR.dosOrCShell("convert " + fullTmpPngFile + " " + fullTmpGifFile, 20);
      ImageIO.write(bi, "gif", new File(fullTmpGifFile));
      /* //attempts to make work on Windows:
      PipeToStringArray outCatcher = new PipeToStringArray();
      PipeToStringArray errCatcher = new PipeToStringArray();
      String2.log("SSR.shell: 1 string " + Calendar2.getCurrentISODateTimeString());
      int exitValue = SSR.shell(new String[]{"convert " +
                  String2.replaceAll(fullTmpPngFile, "/", "\\").substring(2) + " " +
                  "GIF:" + String2.replaceAll(fullTmpGifFile, "/", "\\").substring(2)},
                  outCatcher, errCatcher);
      String2.log("out: " + outCatcher.getString());
      String2.log("err: " + errCatcher.getString());
      */
      // File2.delete(fullTmpPngFile); // delete .png file
      File2.rename(fullTmpGifFile, fullImageFileName); // final step
    } else if (fullImageFileName.endsWith(".png")) {
      ImageIO.write(bi, "png", new File(fullTmpPngFile));
      File2.rename(fullTmpPngFile, fullImageFileName); // final step
    } else
      Test.error(
          String2.ERROR
              + " in SgtMap.makeRegionsMap: "
              + "Unexpected extension: "
              + fullImageFileName);

    // generate the results array
    // (literally the min and max of the x,y values for the bounds of the graph)
    // remember 0,0 is upper left of image
    // String2.log("results: 0=imageWidth, 1=imageHeight, 2=graphMinX, 3=graphMaxX, 4=graphMinY,
    // 5=graphMaxY");
    int results[] =
        new int[] {
          imageWidth,
          imageHeight,
          graphULX,
          imageWidth - graphRightBorder,
          graphULY,
          imageHeight - graphBottomY
        };
    // String2.log(String2.toCSSVString(results));
    return results;
  }

  /** This tests SgtMap. */
  @org.junit.jupiter.api.Test
  @TagImageComparison
  void basicTest() throws Exception {
    // verbose = true;
    // reallyVerbose = true;
    // String2.log("*** test SgtMap.basicTest");

    boolean allSizes = true;
    boolean showInBrowser = false;
    BufferedImage bufferedImage;
    String baseName, tName;

    // String2.getContextDirectory() + "WEB-INF/ref/landmask_pt0125deg_usmex.grd");

    if (true) {
      // make a regionsMap
      ResourceBundle2 classRB2 =
          new ResourceBundle2(
              "gov.noaa.pfel.coastwatch.TimePeriods", "gov.noaa.pfel.coastwatch.BrowserDefault");
      String tRegionInfo[] = String2.split(classRB2.getString("regionInfo", null), '\f');
      String regionInfo[][] = new String[tRegionInfo.length][];
      for (int region = 0; region < tRegionInfo.length; region++) {
        regionInfo[region] = String2.split(tRegionInfo[region], ',');
        Test.ensureEqual(
            regionInfo[region].length,
            8,
            String2.ERROR + " in SgtMap.makePlainRegionsMap, region=" + region);
      }
      baseName = "SgtMapBasicTestRegionsMap";

      SgtMapTests.makeRegionsMap(
          classRB2.getInt("regionMapMaxWidth", 228),
          classRB2.getInt("regionMapMaxHeight", 200),
          regionInfo,
          Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR),
          baseName + testImageExtension);
      // Test.displayInBrowser("file://" + Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR) +
      // "tempRegionsMap" + testImageExtension);
      Image2Tests.testImagesIdentical(
          baseName + testImageExtension, baseName + ".png", baseName + "_diff.png");

      // these match the sizes in CWBrowser.properties
      int imageWidths[] = new int[] {240, 480, 720}; // these were updated 9/1/06
      int imageHeights[] = new int[] {500, 640, 960};
      int pdfWidths[] = new int[] {480, 660, 960}; // 6.5*144
      int pdfHeights[] = new int[] {780, 980, 1280}; // 9*144
      String regionNames[] =
          new String[] {
            "_C2_", "_C_", "_USMexico_",
          };

      // make Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR) + temp + testImageExtension
      baseName = "SgtMapBasicTestA";
      tName = Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR) + baseName + testImageExtension;
      File2.delete(tName); // old version? delete it
      bufferedImage = SgtUtil.getBufferedImage(imageWidths[1], imageHeights[1]);
      testMakeMap(
          (Graphics2D) bufferedImage.getGraphics(),
          "over",
          0,
          0,
          imageWidths[1],
          imageHeights[1],
          2,
          0,
          1); // region=2
      SgtUtil.saveImage(bufferedImage, tName);
      // Test.displayInBrowser("file://" + SSR.getTempDirectory() + "temp" +
      // testImageExtension);
      Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

      // test of properly created maps and test for memory leak (testForMemoryLeak)
      // make a series of images and .pdf's:
      // SSR.getTempDirectory() + temp* + testImageExtension and temp*.pdf
      // for memory leak tests: comment out jPane.draw in makeMap
      // and load crude data in sgt constructor above
      long baseMemory = 0;
      int nReps = 1;
      for (int rep = 0; rep < nReps; rep++) { // do more reps for harder memory test
        for (int region = 0; region < 3; region++) { // for all regions, start at 0
          for (int size = (allSizes ? 0 : 1); size < (allSizes ? 3 : 2); size++) {

            baseName = "SgtMapBasicTest" + regionNames[region] + size;
            tName =
                Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR) + baseName + testImageExtension;
            File2.delete(tName); // old version? delete it
            bufferedImage = SgtUtil.getBufferedImage(imageWidths[size], imageHeights[size]);
            testMakeMap(
                (Graphics2D) bufferedImage.getGraphics(),
                "over",
                0,
                0,
                imageWidths[size],
                imageHeights[size],
                region,
                0,
                1);
            SgtUtil.saveImage(bufferedImage, tName);

            // view it in browser?
            if (showInBrowser && rep == 0 && region == 2 && size == 1) {
              String2.log("displaying " + tName);
              // Test.displayInBrowser("file://" + tName);
              Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");
            }

            // make a pdf file
            // make biggest image that fits on page (document has .5" margin all around)
            // make y smaller, looks goofy to have huge legend, but not small enough to
            // reduce width
            String2.log("\n*** Test SgtUtil.createPdf");
            tName =
                Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR)
                    + "temp"
                    + regionNames[region]
                    + size
                    + ".pdf";
            File2.delete(tName); // old version? delete it
            Object oar[] =
                SgtUtil.createPdf(SgtUtil.PDF_PORTRAIT, pdfWidths[size], pdfHeights[size], tName);
            testMakeMap(
                (Graphics2D) oar[0],
                "over",
                0,
                0,
                pdfWidths[size],
                pdfHeights[size],
                region,
                0,
                SgtMap.PDF_FONTSCALE); // 0=boundaryResAdjust,
            SgtUtil.closePdf(oar);

            // view it in browser?
            // if (showInBrowser && rep == 0 && region == 2) {
            // String2.log("displaying " + tName);
            // Test.displayInBrowser("file://" + tName);
            // }
          }
        }
      }

      // look for memory leak
      Math2.gcAndWait("SgtGraph (between tests)");
      Math2.gcAndWait("SgtGraph (between tests)"); // in a test. Ensure all garbage collected.
      long using = Math2.getMemoryInUse();
      if (baseMemory == 0) baseMemory = using;
      long lpr = (using - baseMemory) / nReps;
      String2.log(
          "\n**** SgtMap test for memory leak: nReps="
              + nReps
              + " memoryUsing="
              + using
              + " leak/rep="
              + lpr
              + "\n-> See all the "
              + Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR)
              + "tempXXX"
              + testImageExtension
              + " and .pdf files."
              + "\nPress CtrlBreak in console window to generate hprof heap info.");
      if (lpr > 0) String2.pressEnterToContinue();
    }
    // String2.pressEnterToContinue();

    // make a transparent version
    bufferedImage = SgtUtil.getBufferedImage(600, 600);
    testMakeMap((Graphics2D) bufferedImage.getGraphics(), "over", 0, 0, 600, 600, 2, 0, 1);
    baseName = "SgtMapBasicTestTransparent";
    tName = Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR) + baseName;
    File2.delete(tName);
    SgtUtil.saveAsTransparentPng(bufferedImage, SgtMap.oceanColor, tName);
    // String2.log(" transparentName = " + tName);
    // Test.displayInBrowser("file://" + tranName + ".png");
    Image2Tests.testImagesIdentical(tName + ".png", baseName + ".png", baseName + "_diff.png");
  }

  /** This tests makeCleanMap. (0, 5) */
  @org.junit.jupiter.api.Test
  @TagImageComparison
  void testMakeCleanMap() throws Exception {
    // verbose = true;
    // reallyVerbose = true;
    // String2.log("*** testMakeCleanMap");
    int first = 0;
    int last = 5; // 5=all

    int size = 400;
    int px[] = {0, size, 2 * size, 0, size, 2 * size};
    int py[] = {0, 0, 0, size, size, size};
    double cx[] = {-90, -108, -122, -122, -122, -122.15};
    double cy[] = {0, 38, 38, 38, 38, 38.1};
    double inc[] = {90, 22.5, 5, 2, 1, 0.19};
    // !!!EEK!!! no Sacramento River Delta! gshhs is based on 2 datasets:
    // World Vector Shorelines (WVS) and CIA World Data Bank II (WDBII).
    // Perhaps each thinks the other is responsible for it.

    for (int im = first; im <= last; im++) {
      BufferedImage bufferedImage = SgtUtil.getBufferedImage(size * 3, size * 2);
      Graphics g = bufferedImage.getGraphics();
      Graphics2D g2 = (Graphics2D) g;
      g.setColor(SgtMap.oceanColor);
      g.fillRect(0, 0, size * 3, size * 2);

      for (int res = 0; res < 6; res++) {
        SgtMap.makeCleanMap(
            cx[res] - inc[res],
            cx[res] + inc[res],
            cy[res] - inc[res],
            cy[res] + inc[res],
            im == 0, // boolean drawLandUnder,
            null,
            1,
            1,
            0,
            null,
            im >= 1, // boolean drawLandOver,
            im == 2 || im == 6, // boolean drawCoastline,
            im == 3 || im == 6 ? SgtMap.FILL_LAKES_AND_RIVERS : SgtMap.NO_LAKES_AND_RIVERS,
            im >= 4, // boolean drawNationalBoundaries,
            im == 5 || im == 6, // boolean drawStateBoundaries,
            g2,
            size * 3,
            size * 2,
            px[res],
            py[res],
            size,
            size);
      }
      // draw dividing lines
      g.setColor(Color.BLACK);
      g.drawLine(0, size, size * 3, size);
      g.drawLine(size, 0, size, size * 2);
      g.drawLine(size * 2, 0, size * 2, size * 2);
      String baseName = "SgtMapTestMakeCleanMap" + im;
      String tName = Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR) + baseName;
      File2.delete(tName + ".png");
      SgtUtil.saveAsTransparentPng(bufferedImage, null, tName); // oceanColor?
      // Test.displayInBrowser("file://" + tName + ".png");
      Image2Tests.testImagesIdentical(tName + ".png", baseName + ".png", baseName + "_diff.png");
    }
  }

  @org.junit.jupiter.api.Test
  @TagImageComparison
  void testX0To360Regions() throws Exception {
    testRegionsMap(0, 360, -90, 90);
  }

  @org.junit.jupiter.api.Test
  @TagImageComparison
  void testX180To180Regions() throws Exception {
    testRegionsMap(-180, 180, -90, 90);
  }

  /** This makes a regionsMap for the specified lon lat range and displays it in the browser. */
  void testRegionsMap(double minX, double maxX, double minY, double maxY) throws Exception {
    // verbose = true;
    // reallyVerbose = true;
    // Boundaries.verbose = true;
    // Boundaries.reallyVerbose = true;

    int offset = minX >= 0 ? 360 : 0;
    String regionInfo[][] = {
      {
        "0x00FFFFFF",
        "" + minX,
        "" + maxX,
        "" + minY,
        "" + maxY,
        "" + (-150 + offset),
        "10",
        "World"
      },
      {
        "0x30FF00FF",
        "" + (-135 + offset),
        "" + (-105 + offset),
        "22",
        "50",
        "" + (-131 + offset),
        "25",
        "US+Mexico"
      }
    };
    String dir = Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR);
    String baseName = "SgtMapTestRegionsMapW" + minX + "E" + maxX + "S" + minY + "N" + maxY;
    String name = baseName + testImageExtension;

    SgtMapTests.makeRegionsMap(
        300,
        200, // size
        regionInfo,
        dir,
        name);

    // view it
    // ImageViewer.display("SgtMap", image);
    // Test.displayInBrowser("file://" + dir + name);
    Image2Tests.testImagesIdentical(name, baseName + ".png", baseName + "_diff.png");
  }

  /** This tests the creation of a bathymetry grid. */
  @org.junit.jupiter.api.Test
  void testCreateTopographyGrid() throws Exception {
    // String2.log("\ntestCreateTopographyGrid");
    // Grid.verbose = true;

    // lon -180 160; a simple test within the native range of the data
    // This section has circular tests; used as basis for other tests.
    // Bath in cwbrowser180 looks right for this range.
    Grid grid = SgtMap.createTopographyGrid(null, -180, 160, -80, 80, 35, 17);
    Test.ensureEqual(grid.lon.length, 35, "");
    Test.ensureEqual(grid.lon[0], -180, "");
    Test.ensureEqual(grid.lon[34], 160, "");
    Test.ensureEqual(grid.lat.length, 17, "");
    Test.ensureEqual(grid.lat[0], -80, "");
    Test.ensureEqual(grid.lat[16], 80, "");
    Test.ensureEqual(grid.lonSpacing, 10, "");
    Test.ensureEqual(grid.latSpacing, 10, ""); // was etopo2v2 etopo1
    Test.ensureEqual(grid.getData(0, 0), -100, ""); // -180 was 2 -1
    Test.ensureEqual(grid.getData(2, 0), 22, ""); // -160 was 2 -1
    Test.ensureEqual(grid.getData(17, 0), 1838, ""); // -10 was 1932 1925
    Test.ensureEqual(grid.getData(18, 0), 2334, ""); // 0 was 2352 2351
    Test.ensureEqual(grid.getData(19, 0), 2798, ""); // 10 was 2768 2761
    Test.ensureEqual(grid.getData(34, 0), 604, ""); // 160 was 654 723

    Test.ensureEqual(grid.getData(0, 16), -1530, ""); // -180 was -1521 -1519
    Test.ensureEqual(grid.getData(2, 16), -3258, ""); // -160 was -3201 -3192
    Test.ensureEqual(grid.getData(17, 16), -113, ""); // -10 was -105 -104
    Test.ensureEqual(grid.getData(18, 16), -2603, ""); // 0 was -2593 -2591
    Test.ensureEqual(grid.getData(19, 16), -480, ""); // 10 was -481 -481
    Test.ensureEqual(grid.getData(34, 16), -2199, ""); // 160 was -2211 -2211

    Test.ensureEqual(grid.nValidPoints, 35 * 17, "");
    Test.ensureEqual(grid.minData, -6154, ""); // was -6297 -6119
    Test.ensureEqual(grid.maxData, 5530, "");

    // -180 180 //hard part: catch 180
    grid = SgtMap.createTopographyGrid(null, -180, 180, -80, 80, 37, 17);
    Test.ensureEqual(grid.lon.length, 37, "");
    Test.ensureEqual(grid.lon[0], -180, "");
    Test.ensureEqual(grid.lon[36], 180, "");
    Test.ensureEqual(grid.lat.length, 17, "");
    Test.ensureEqual(grid.lat[0], -80, "");
    Test.ensureEqual(grid.lat[16], 80, ""); // was etopo2v2 etopo2
    Test.ensureEqual(grid.getData(0, 0), -100, ""); // -180 was 2 -1
    Test.ensureEqual(grid.getData(2, 0), 22, ""); // -160 was 2 -1
    Test.ensureEqual(grid.getData(17, 0), 1838, ""); // -10 was 1932 1925
    Test.ensureEqual(grid.getData(18, 0), 2334, ""); // 0 was 2352 2351
    Test.ensureEqual(grid.getData(19, 0), 2798, ""); // 10 was 2768 2761
    Test.ensureEqual(grid.getData(34, 0), 604, ""); // 160 was 654 723
    Test.ensureEqual(grid.getData(36, 0), -100, ""); // 180 was 2 -1

    Test.ensureEqual(grid.getData(0, 16), -1530, ""); // -180 was -1521 -1519
    Test.ensureEqual(grid.getData(2, 16), -3258, ""); // -160 was -3201 -3192
    Test.ensureEqual(grid.getData(17, 16), -113, ""); // -10 was -105 -104
    Test.ensureEqual(grid.getData(18, 16), -2603, ""); // 0 was -2593 -2591
    Test.ensureEqual(grid.getData(19, 16), -480, ""); // 10 was -481 -481
    Test.ensureEqual(grid.getData(34, 16), -2199, ""); // 160 was -2211 -2211
    Test.ensureEqual(grid.getData(36, 16), -1530, ""); // 180 was -1521 -1519

    // -180 180 //a test that failed
    grid = SgtMap.createTopographyGrid(null, -180, 180, -90, 90, 480, 540);
    Test.ensureEqual(grid.lon.length, 481, ""); // was 491
    Test.ensureEqual((float) grid.lon[0], -180.0, ""); // was -179.6666666f
    Test.ensureEqual((float) grid.lon[480], 180, ""); // was 179.666666f
    Test.ensureEqual(grid.lat.length, 541, "");
    Test.ensureEqual((float) grid.lat[0], -90f, "");
    Test.ensureEqual((float) grid.lat[540], 90f, "");
    Test.ensureEqual(grid.getData(0, 0), 2745, ""); // -180 was 2774
    Test.ensureEqual(grid.getData(480, 540), -4228, ""); // 180 was -4229 -4117

    // lon 0 360 //hard parts, 180 to 359, 360
    grid = SgtMap.createTopographyGrid(null, 0, 360, -80, 80, 37, 17);
    Test.ensureEqual(grid.lon.length, 37, "");
    Test.ensureEqual(grid.lon[0], -0.0, "");
    Test.ensureEqual(grid.lon[36], 360, "");
    Test.ensureEqual(grid.lat.length, 17, "");
    Test.ensureEqual(grid.lat[0], -80, "");
    Test.ensureEqual(grid.lat[16], 80, ""); // was etopo2v2 etopo2
    Test.ensureEqual(grid.getData(0, 0), 2334, ""); // 0 was 2352 2351
    Test.ensureEqual(grid.getData(1, 0), 2798, ""); // 10 was 2768 2761
    Test.ensureEqual(grid.getData(16, 0), 604, ""); // 160 was 654 723
    Test.ensureEqual(grid.getData(18, 0), -100, ""); // 180 was 2 -1
    Test.ensureEqual(grid.getData(20, 0), 22, ""); // -160 20 was 2 -1
    Test.ensureEqual(grid.getData(35, 0), 1838, ""); // -10 350 was 1932 1925
    Test.ensureEqual(grid.getData(36, 0), 2334, ""); // 0 360 was 2352 2351

    Test.ensureEqual(grid.getData(0, 16), -2603, ""); // 0 was -2593 -2591
    Test.ensureEqual(grid.getData(1, 16), -480, ""); // 10 was -481 -481
    Test.ensureEqual(grid.getData(16, 16), -2199, ""); // 160 was -2211 -2211
    Test.ensureEqual(grid.getData(18, 16), -1530, ""); // 180 was -1521 -1519
    Test.ensureEqual(grid.getData(20, 16), -3258, ""); // -160 20 was -3201 -3192
    Test.ensureEqual(grid.getData(35, 16), -113, ""); // -10 350 was -105 -104
    Test.ensureEqual(grid.getData(36, 16), -2603.0, ""); // -10 360 was-2593 -2591

    // 1 point
    grid =
        SgtMap.createTopographyGrid(null, -10, -10, -80, -80, Integer.MAX_VALUE, Integer.MAX_VALUE);
    Test.ensureEqual(grid.lon.length, 1, "");
    Test.ensureEqual(grid.lon[0], -10, "");
    Test.ensureEqual(grid.lat.length, 1, "");
    Test.ensureEqual(grid.lat[0], -80, "");
    Test.ensureEqual(grid.getData(0, 0), 1838, ""); // -10 was 1932

    // 1 point, but approx lon and lat
    grid =
        SgtMap.createTopographyGrid(
            null, -10.002, -10.001, -80.002, -80.001, Integer.MAX_VALUE, Integer.MAX_VALUE);
    Test.ensureEqual(grid.lon.length, 1, "");
    Test.ensureEqual(grid.lon[0], -10, "");
    Test.ensureEqual(grid.lat.length, 1, "");
    Test.ensureEqual(grid.lat[0], -80, "");
    Test.ensureEqual(grid.getData(0, 0), 1838, ""); // -10 was 1932

    // 2x2 points, approx lon and lat
    grid =
        SgtMap.createTopographyGrid(
            null, -10.001, -9.97, -80.001, -79.97, Integer.MAX_VALUE, Integer.MAX_VALUE);
    Test.ensureEqual((float) grid.lonSpacing, 0.016666668f, ""); // was 0.033333333f
    Test.ensureEqual((float) grid.latSpacing, 0.016666668f, ""); // was 0.033333333f
    Test.ensureEqual(grid.lon.length, 3, ""); // was 2
    Test.ensureEqual((float) grid.lon[0], -10f, ""); // was -10f
    Test.ensureEqual((float) grid.lon[1], -9.983334f, ""); // was -9.9666666666f
    Test.ensureEqual(grid.lat.length, 3, ""); // was 2
    Test.ensureEqual((float) grid.lat[0], -80f, "");
    Test.ensureEqual((float) grid.lat[1], -79.98334f, ""); // was -79.966666666f
    Test.ensureEqual(grid.getData(0, 0), 1838, ""); // same as above 1932
    Test.ensureEqual(grid.getData(0, 1), 1835, ""); // was 1942
    Test.ensureEqual(grid.getData(1, 0), 1839, ""); // was 1938
    Test.ensureEqual(grid.getData(1, 1), 1837, ""); // was 1950

    // 2x2 points
    grid = SgtMap.createTopographyGrid(null, -10, -9, -80, -79, 2, 2);
    Test.ensureEqual(grid.lon.length, 2, "");
    Test.ensureEqual((float) grid.lon[0], -10f, "");
    Test.ensureEqual((float) grid.lon[1], -9f, "");
    Test.ensureEqual(grid.lat.length, 2, "");
    Test.ensureEqual((float) grid.lat[0], -80f, "");
    Test.ensureEqual((float) grid.lat[1], -79f, "");
    Test.ensureEqual(grid.getData(0, 0), 1838, ""); // was 1932
    Test.ensureEqual(grid.getData(0, 1), 1873, ""); // was 1898
    Test.ensureEqual(grid.getData(1, 0), 1919, ""); // was 2016
    Test.ensureEqual(grid.getData(1, 1), 1960, ""); // was 2018

    // 2x2 points, approx lon and lat
    grid =
        SgtMap.createTopographyGrid(
            null, -10.001, -9.001, -80.001, -79.001, Integer.MAX_VALUE, Integer.MAX_VALUE);
    Test.ensureEqual(grid.lon.length, 61, "");
    Test.ensureEqual((float) grid.lon[0], -10f, "");
    Test.ensureEqual((float) grid.lon[60], -9f, "");
    Test.ensureEqual(grid.lat.length, 61, "");
    Test.ensureEqual((float) grid.lat[0], -80f, "");
    Test.ensureEqual((float) grid.lat[60], -79f, "");
    Test.ensureEqual(grid.getData(0, 0), 1838, ""); // same as above was 1932
    Test.ensureEqual(grid.getData(0, 60), 1873, ""); // was 1898
    Test.ensureEqual(grid.getData(60, 0), 1919, ""); // was 2016
    Test.ensureEqual(grid.getData(60, 60), 1960, ""); // was 2018
  }

  /** This tests bathymetry and the ocean palette in an area that was trouble. */
  @org.junit.jupiter.api.Test
  @TagImageComparison
  void testOceanPalette() throws Exception {
    int first = 0;
    int last = 7;

    double cx[] = {270, 90, -30, 235, 240, -123, -122.6, -122};
    double cy[] = {0, 0, 40, 40, 40, 37.8, 37.8, 36.8};
    double inc[] = {90, 90, 30, 15, 7, 1.5, 0.5, 0.25};

    for (int i = first; i <= last; i++) { // 0..7
      BufferedImage bufferedImage = SgtUtil.getBufferedImage(480, 500);
      Grid bath =
          SgtMap.createTopographyGrid(
              Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR),
              cx[i] - inc[i],
              cx[i] + inc[i],
              cy[i] - inc[i],
              cy[i] + inc[i],
              460,
              460);
      String bathymetryCptFullPath =
          File2.accessResourceFile(SgtMap.bathymetryCptFullName.toString());
      SgtMap.makeMap(
          false,
          SgtUtil.LEGEND_BELOW,
          null,
          null,
          null,
          null,
          cx[i] - inc[i],
          cx[i] + inc[i],
          cy[i] - inc[i],
          cy[i] + inc[i],
          "over", // drawLandMask,
          true, // plotGridData (bathymetry)
          bath,
          1,
          1,
          0, // double gridScaleFactor, gridAltScaleFactor, gridAltOffset,
          bathymetryCptFullPath,
          null, // SgtMap.BATHYMETRY_BOLD_TITLE + " (" + SgtMap.BATHYMETRY_UNITS + ")",
          "",
          "",
          "", // "Data courtesy of " + SgtMap.BATHYMETRY_COURTESY,
          SgtMap.NO_LAKES_AND_RIVERS,
          false,
          null,
          1,
          1,
          1,
          "",
          null,
          "",
          "",
          "",
          "",
          "", // plot contour
          new ArrayList(), // graphDataLayers
          (Graphics2D) bufferedImage.getGraphics(),
          0,
          0,
          480,
          500,
          0, // no boundaryResAdjust,
          1);
      String fileName = "SgtMapOceanPalette" + i;
      String tName = Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR) + fileName + ".png";
      File2.delete(tName); // old version? delete it
      SgtUtil.saveImage(bufferedImage, tName);
      String2.log("displaying " + tName);
      // Test.displayInBrowser("file://" + tName);
      Image2Tests.testImagesIdentical(tName, fileName + ".png", fileName + "_diff.png");
    }
  }

  /**
   * For test purposes, this draws a map on the specified g2 object.
   *
   * @param g2
   * @param drawLandMask One of the non-"" SgtMap.drawLandMask_OPTIONS: "under", "over", "outline",
   *     or "off"
   * @param baseULXPixel
   * @param baseULYPixel
   * @param imageWidth
   * @param imageHeight
   * @param region 0=C2 1=C 2=US+Mexico
   * @param boundaryResAdjust
   * @param fontScale
   * @throws Exception if trouble
   */
  void testMakeMap(
      Graphics2D g2,
      String drawLandMask,
      int baseULXPixel,
      int baseULYPixel,
      int imageWidth,
      int imageHeight,
      int region,
      int boundaryResAdjust,
      double fontScale)
      throws Exception {

    // describe grid vectors
    ArrayList<GraphDataLayer> pointDataList = new ArrayList<>();
    String griddataDir = SgtMapTests.class.getResource("/data/gridTests/").getPath();
    /*
     * String fullResultCpt = griddataDir + "TestMakeMap.cpt";
     * File2.delete(fullResultCpt);
     * CompoundColorMap.makeCPT(File2.webInfParentDirectory() + //with / separator
     * and / at the end
     * "WEB-INF/cptfiles/Rainbow.cpt",
     * "Linear", 0, 10, true, fullResultCpt);
     */
    String vectorCpt =
        CompoundColorMap.makeCPT(
            File2.getWebInfParentDirectory()
                + // with / separator and / at the end
                "WEB-INF/cptfiles/",
            "Rainbow",
            "Linear",
            0,
            10,
            5,
            false,
            griddataDir);

    String gridCpt =
        CompoundColorMap.makeCPT(
            File2.getWebInfParentDirectory()
                + // with / separator and / at the end
                "WEB-INF/cptfiles/",
            "BlueWhiteRed", // "LightBlueWhite"
            "Linear",
            -10,
            10,
            8,
            true,
            griddataDir);

    Grid xGrid = new Grid();
    Grid yGrid = new Grid();
    xGrid.readGrd(griddataDir + "windx.grd");
    yGrid.readGrd(griddataDir + "windy.grd");

    pointDataList.add(
        new GraphDataLayer(
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            GraphDataLayer.DRAW_GRID_VECTORS,
            false,
            false,
            "gvX axis title",
            "gvY axis title",
            "Grid Vector Bold Title",
            "This is gv title2.",
            "This is gv title3.",
            "This is gv title4.",
            null,
            xGrid,
            yGrid,
            new CompoundColorMap(vectorCpt),
            null,
            GraphDataLayer.MARKER_TYPE_NONE,
            0,
            10, // standardVector=10m/s
            -1));

    // region? C2 : C : US+Mexico minX, maxX, minY, maxY,
    double minX = region == 0 ? -125 : region == 1 ? -129.5 : -135;
    double maxX = region == 0 ? -121 : region == 1 ? -120.5 : -105;
    double minY = region == 0 ? 35 : region == 1 ? 33.5 : 22;
    double maxY = region == 0 ? 39 : region == 1 ? 42.5 : 50;
    Grid gridGrid = new Grid();
    gridGrid.readGrd(
        griddataDir + "OQNux10S1day_20050712_x-135_X-105_y22_Y50.grd",
        minX,
        maxX,
        minY,
        maxY,
        imageWidth,
        imageHeight);
    Grid contourGrid = new Grid();
    contourGrid.readGrd(
        griddataDir + "OQNux10S1day_20050712_x-135_X-105_y22_Y50.grd",
        minX,
        maxX,
        minY,
        maxY,
        imageWidth,
        imageHeight);
    SgtMap.makeMap(
        false,
        SgtUtil.LEGEND_BELOW,
        "NOAA",
        "CoastWatch",
        File2.getWebInfParentDirectory()
            + // with / separator and / at the end
            "images/", // imageDir
        "noaa20.gif", // logoImageFile
        minX,
        maxX,
        minY,
        maxY,
        drawLandMask,
        true, // plotGridData,
        gridGrid,
        1,
        1,
        0, // double gridScaleFactor, gridAltScaleFactor, gridAltOffset,
        gridCpt,
        "Bold Title",
        SgtUtil.getNewTitle2("dUnits", "2004-01-05 to 2004-01-07", "Title 2"),
        "Data courtesy of John Someone, UC Davis",
        "title4 text blah blah blah",
        SgtMap.NO_LAKES_AND_RIVERS,
        true, // plotContourData,
        contourGrid,
        1,
        1,
        0, // double contourScaleFactor, contourAltScaleFactor, contourAltOffset,
        "5", // contourDrawLinesAt,
        new Color(0x990099), // contourColor
        "Contour Bold Title with lots of text",
        "cUnits",
        "",
        "2004-01-05 to 2004-01-07", // contourDateTime,
        "Data courtesy of related text", // Contour data
        pointDataList,
        g2,
        baseULXPixel,
        baseULYPixel,
        imageWidth,
        imageHeight,
        boundaryResAdjust,
        fontScale);

    // delete the temp cpt file
    File2.delete(vectorCpt);
    File2.delete(gridCpt);
  }

  /**
   * For test purposes, this draws a map on the specified g2 object.
   *
   * @param bathCpt true=Ocean.cpt false=Topography.cpt
   * @param g2
   * @param baseULXPixel
   * @param baseULYPixel
   * @param imageWidth
   * @param imageHeight
   * @param region 0=C2 1=C 2=US+Mexico 3=world 4=himalayas 5=peru, 6=PacIsland offset, 7=-400 to
   *     -40, 8=40 to 400, 9=-540 to 540, 10=C2-360, 11=C2+360
   * @param boundaryResAdjust
   * @param fontScale
   * @throws Exception if trouble
   */
  void testBathymetryMap(
      boolean bathCpt,
      Graphics2D g2,
      int baseULXPixel,
      int baseULYPixel,
      int imageWidth,
      int imageHeight,
      int region,
      int boundaryResAdjust,
      double fontScale)
      throws Exception {

    // region? 0=SF 1=C 2=US+Mexico 3=world minX, maxX, minY, maxY,
    double minX[] = {-122.47, -129.5, -135, -180, 75, -85, -176.5, -400, 40, -540, -495, 225, 585};
    double maxX[] = {-122.3, -120.5, -105, 180, 105, -55, -176.44, -40, 400, 540, -465, 255, 615};
    double minY[] = {37.8, 33.5, 22, -90, 15, -35, .18, -90, -90, -90, 22, 22, 22};
    double maxY[] = {37.95, 42.5, 50, 90, 45, -5, .24, 90, 90, 90, 50, 50, 50};
    int predicted[] =
        SgtMap.predictGraphSize(
            1, imageWidth, imageHeight, minX[region], maxX[region], minY[region], maxY[region]);

    URL bathyResourceFile = bathCpt ? SgtMap.bathymetryCptFullName : SgtMap.topographyCptFullName;
    String bathymetryCptFullPath = File2.accessResourceFile(bathyResourceFile.toString());
    SgtMap.makeMap(
        false,
        SgtUtil.LEGEND_BELOW,
        "NOAA",
        "CoastWatch",
        File2.getWebInfParentDirectory()
            + // with / separator and / at the end
            "images/", // imageDir
        "noaa20.gif", // logoImageFile
        minX[region],
        maxX[region],
        minY[region],
        maxY[region],
        bathCpt ? "over" : "under",
        true, // plotGridData,
        SgtMap.createTopographyGrid(
            SgtMap.fullPrivateDirectory,
            minX[region],
            maxX[region],
            minY[region],
            maxY[region],
            predicted[0],
            predicted[1]),
        1,
        1,
        0, // double gridScaleFactor, gridAltScaleFactor, gridAltOffset,
        bathymetryCptFullPath,
        bathCpt
            ? SgtMap.BATHYMETRY_BOLD_TITLE + " (" + SgtMap.BATHYMETRY_UNITS + ")"
            : SgtMap.TOPOGRAPHY_BOLD_TITLE + " (" + SgtMap.TOPOGRAPHY_UNITS + ")",
        "",
        "",
        "Data courtesy of " + SgtMap.BATHYMETRY_COURTESY,
        SgtMap.FILL_LAKES_AND_RIVERS,
        false, // plotContourData,
        null,
        1,
        1,
        0, // double contourScaleFactor, contourAltScaleFactor, contourAltOffset,
        "10, 14", // contourDrawLinesAt,
        new Color(0x990099), // contourColor
        "",
        "",
        "",
        "", // contourDateTime,
        "", // Contour data
        new ArrayList(), // pointDataList
        g2,
        baseULXPixel,
        baseULYPixel,
        imageWidth,
        imageHeight,
        boundaryResAdjust,
        fontScale);
  }
}
