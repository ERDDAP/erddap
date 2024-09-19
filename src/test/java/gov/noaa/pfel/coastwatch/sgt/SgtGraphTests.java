package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.array.DoubleArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.File2;
import com.cohort.util.Image2Tests;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.griddata.Grid;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import tags.TagImageComparison;
import testDataset.Initialization;

class SgtGraphTests {

  @TempDir private static Path TEMP_DIR;

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /**
   * Test for memory leak. Make a series of images and .pdf's: SSR.getTempDirectory() + temp* +
   * testImageExtension and temp*.pdf
   */
  @org.junit.jupiter.api.Test
  void testForMemoryLeak() throws Exception {
    File2.setWebInfParentDirectory(System.getProperty("user.dir") + "/");
    // verbose = true;
    // reallyVerbose = true;
    // PathCartesianRenderer.verbose = true;
    // PathCartesianRenderer.reallyVerbose = true;
    boolean xIsLogAxis = false;
    boolean yIsLogAxis = false;
    // AttributedString2.verbose = true;
    long time = System.currentTimeMillis();
    String tempDir = SSR.getTempDirectory();
    SgtGraph sgtGraph =
        new SgtGraph("SansSerif"); // "DejaVu Sans" "Bitstream Vera Sans"); //"SansSerif" is safe
    // choice
    String imageDir =
        EDStatic.getWebInfParentDirectory()
            + // with / separator and / at the end
            "images/";

    int width = 400;
    int height = 600;

    // graph 1: make a data file with data
    PrimitiveArray xCol1 =
        PrimitiveArray.factory(new double[] {1.500e9, 1.501e9, 1.502e9, 1.503e9});
    PrimitiveArray yCol1 = PrimitiveArray.factory(new double[] {1.1, 2.9, Double.NaN, 2.3});
    Table table1 = new Table();
    table1.addColumn("X", xCol1);
    table1.addColumn("Y", yCol1);

    // graph 1: make a graphDataLayer with data for a time series line
    GraphDataLayer graphDataLayer =
        new GraphDataLayer(
            -1, // which pointScreen
            0,
            1,
            -1,
            -1,
            -1, // x,y, others unused
            GraphDataLayer.DRAW_MARKERS_AND_LINES,
            true,
            false,
            "Time",
            "Y Axis Title", // x,yAxisTitle for now, always std units
            "This is the really, really, extra long and very nice bold title.",
            "This is a really, really, extra long and very nice title2.",
            "This is a really, really, extra long and very nice title3.",
            "This is title4.",
            table1,
            null,
            null,
            null,
            new java.awt.Color(0x0099FF),
            GraphDataLayer.MARKER_TYPE_PLUS,
            GraphDataLayer.MARKER_SIZE_SMALL,
            0, // vectorStandard
            GraphDataLayer.REGRESS_MEAN);
    ArrayList<GraphDataLayer> graphDataLayers1 = new ArrayList();
    graphDataLayers1.add(graphDataLayer);

    // graph 1: plus 100 random points of each marker type
    int lastMarker = 9;
    for (int i = 1; i <= lastMarker; i++) {
      // make a data file with data
      PrimitiveArray txCol = new DoubleArray();
      PrimitiveArray tyCol = new DoubleArray();
      for (int j = 0; j < 100; j++) {
        txCol.addDouble(1.500e9 + Math2.random() * .0025e9);
        tyCol.addDouble(1.5 + Math2.random());
      }
      Table ttable = new Table();
      ttable.addColumn("X", txCol);
      ttable.addColumn("Y", tyCol);

      graphDataLayer =
          new GraphDataLayer(
              -1, // which pointScreen
              0,
              1,
              -1,
              -1,
              -1, // x,y, others unused
              GraphDataLayer.DRAW_MARKERS,
              true,
              false, // x,yIsTimeAxis
              "Time",
              "Y Axis Title", // x,yAxisTitle for now, always std units
              null, // so not in legend
              "title2",
              "title3",
              "title4", // will be ignored
              ttable,
              null,
              null,
              null,
              new java.awt.Color(0x0099FF),
              i,
              GraphDataLayer.MARKER_SIZE_SMALL,
              0, // vectorStandard
              GraphDataLayer.REGRESS_NONE); // else mean line for each marker type
      graphDataLayers1.add(graphDataLayer);
    }

    // draw lots of graphs
    long baseMemory = 0;
    int nReps = 20;
    for (int rep = 0; rep < nReps; rep++) { // do more reps for harder memory test

      // draw the graph with data
      BufferedImage bufferedImage1 = SgtUtil.getBufferedImage(width, height);
      Graphics2D g21 = (Graphics2D) bufferedImage1.getGraphics();
      sgtGraph.makeGraph(
          false,
          "xAxisTitle",
          "yAxisTitle",
          SgtUtil.LEGEND_BELOW,
          "Graph 1,",
          "x is TimeAxis",
          imageDir,
          "noaa20.gif",
          Double.NaN,
          Double.NaN,
          true,
          true,
          xIsLogAxis, // isAscending, isTimeAxis, isLog
          Double.NaN,
          Double.NaN,
          true,
          false,
          yIsLogAxis,
          graphDataLayers1,
          g21,
          0,
          0, // upperLeft
          width,
          height,
          1, // graph width/height
          SgtGraph.DefaultBackgroundColor,
          1); // fontScale
      String fileName = tempDir + "SgtGraphMemoryTest" + rep + ".png";
      SgtUtil.saveImage(bufferedImage1, fileName);

      // view it in browser?
      // Graph of random points seems like its going to fail image diff eery time.
      // if (rep == 0) {
      //   // Test.displayInBrowser("file://" + fileName);
      //   Image2Tests.testImagesIdentical(
      //       fileName,
      //       String2.unitTestImagesDir() + "SgtGraphMemoryTest" + rep + ".png",
      //       File2.getSystemTempDirectory() + "SgtGraphMemoryTest" + rep + "_diff.png");
      // }
    }

    // check memory usage
    for (int rep = 0; rep < nReps; rep++)
      File2.delete(tempDir + "SgtGraphMemoryTest" + rep + ".png");
    Math2.gcAndWait("SgtGraph (between tests)");
    Math2.gcAndWait(
        "SgtGraph (between tests)"); // in a test, before getMemoryInUse(). Ensure all garbage
    // collected.
    long using = Math2.getMemoryInUse();
    if (baseMemory == 0) baseMemory = using;
    long lpr = (using - baseMemory) / nReps;
    String2.log(
        "\n*** SgtGraph test for memory leak: nReps="
            + nReps
            + " memoryUsing="
            + using
            + " leak/rep="
            + lpr
            + "\nPress CtrlBreak in console window to generate hprof heap info.");
    if (lpr > 0) {
      throw new Exception(
          "memory increase, suspected memory leak: ** SgtGraph test for memory leak: nReps="
              + nReps
              + " memoryUsing="
              + using
              + " leak/rep="
              + lpr);
    }
  }

  // if (test == 0) testDiverseGraphs(true, false, false); //testAllAndDisplay,
  // xIsLogAxis, yIsLogAxis
  // if (test == 1) testDiverseGraphs(true, false, true);
  // if (test == 2) testDiverseGraphs(true, true, true);

  /** This makes and displays lots of graph types. */
  @org.junit.jupiter.api.Test
  @TagImageComparison
  void testDiverseGraphs_tff() throws Exception {
    testDiverseGraphs(true, false, false);
  }

  @org.junit.jupiter.api.Test
  @TagImageComparison
  void testDiverseGraphs_tft() throws Exception {
    testDiverseGraphs(true, false, true);
  }

  @org.junit.jupiter.api.Test
  @TagImageComparison
  void testDiverseGraphs_ttt() throws Exception {
    testDiverseGraphs(true, true, true);
  }

  private static void testDiverseGraphs(
      boolean testAllAndDisplay, boolean xIsLogAxis, boolean yIsLogAxis) throws Exception {
    // verbose = true;
    // reallyVerbose = true;
    // PathCartesianRenderer.verbose = true;
    // PathCartesianRenderer.reallyVerbose = true;
    // AttributedString2.verbose = true;
    long time = System.currentTimeMillis();
    SgtGraph sgtGraph =
        new SgtGraph("SansSerif"); // "DejaVu Sans" "Bitstream Vera Sans"); //"SansSerif" is safe
    // choice
    String imageDir =
        EDStatic.getWebInfParentDirectory()
            + // with / separator and / at the end
            "images/";
    String baseImageName =
        "SgtGraph_testDiverseGraphs_" + (xIsLogAxis ? "X" : "") + (yIsLogAxis ? "Y" : "");

    int width = 800; // 2 graphs wide
    int height = 1200; // 3 graphs high

    // graph 1: make a data file with data
    PrimitiveArray xCol1 =
        PrimitiveArray.factory(new double[] {1.500e9, 1.501e9, 1.502e9, 1.503e9});
    PrimitiveArray yCol1 = PrimitiveArray.factory(new double[] {1, 29, Double.NaN, 23});
    Table table1 = new Table();
    table1.addColumn("X", xCol1);
    table1.addColumn("Y", yCol1);

    // graph 2: make a data file with data
    PrimitiveArray xCol2 =
        PrimitiveArray.factory(new double[] {0, 1, 2, 3}); // comment out for another test
    // PrimitiveArray xCol2 = PrimitiveArray.factory(new double[]{Double.NaN,
    // Double.NaN, Double.NaN, Double.NaN}); //comment out for another test
    PrimitiveArray yCol2 =
        PrimitiveArray.factory(
            new double[] {Double.NaN, Double.NaN, Double.NaN, Double.NaN}); // comment
    // out
    // for
    // another
    // test
    Table table2 = new Table();
    table2.addColumn("X", xCol2);
    table2.addColumn("Y", yCol2);

    // graph 3: make a data file with data
    PrimitiveArray xCol3 =
        PrimitiveArray.factory(
            new double[] {1.500e9, 1.501e9, 1.502e9, 1.503e9, 1.504e9, 1.505e9, 1.506e9});
    PrimitiveArray uCol3 = PrimitiveArray.factory(new double[] {1, 2, 3, 4, 5, 6, 7});
    PrimitiveArray vCol3 = PrimitiveArray.factory(new double[] {-3, -2, -1, 0, 1, 2, 3});
    Table table3 = new Table();
    table3.addColumn("X", xCol3);
    table3.addColumn("U", uCol3);
    table3.addColumn("V", vCol3);

    // graph 5: make a data file with two time columns
    PrimitiveArray xCol5 =
        PrimitiveArray.factory(
            new double[] {1.500e9, 1.501e9, 1.502e9, 1.503e9, 1.504e9, 1.505e9, 1.506e9});
    PrimitiveArray yCol5 =
        PrimitiveArray.factory(
            new double[] {1.500e9, 1.504e9, 1.501e9, 1.500e9, 1.503e9, 1.502e9, 1.506e9});
    Table table5 = new Table();
    table5.addColumn("X", xCol5);
    table5.addColumn("Y", yCol5);

    // graph 1: make a graphDataLayer with data for a time series line
    GraphDataLayer graphDataLayer =
        new GraphDataLayer(
            -1, // which pointScreen
            0,
            1,
            -1,
            -1,
            -1, // x,y, others unused
            GraphDataLayer.DRAW_MARKERS_AND_LINES,
            true,
            false,
            "Time",
            "Y Axis Title", // x,yAxisTitle for now, always std units
            "This is the really, really, extra long and very nice bold title.",
            "This is a really, really, extra long and very nice title2.",
            "This is a really, really, extra long and very nice title3.",
            "This is title4.",
            table1,
            null,
            null,
            null,
            new java.awt.Color(0x0099FF),
            GraphDataLayer.MARKER_TYPE_PLUS,
            GraphDataLayer.MARKER_SIZE_SMALL,
            0, // vectorStandard
            GraphDataLayer.REGRESS_MEAN);
    ArrayList<GraphDataLayer> graphDataLayers1 = new ArrayList();
    graphDataLayers1.add(graphDataLayer);

    // graph 1: plus 10 random points of each marker type
    int lastMarker = 9;
    Math2.setSeed(12345);
    for (int i = 1; i <= lastMarker; i++) {
      // make a data file with data
      PrimitiveArray txCol = new DoubleArray();
      PrimitiveArray tyCol = new DoubleArray();
      for (int j = 0; j < 10; j++) {
        txCol.addDouble(1.500e9 + Math2.random() * .0025e9);
        tyCol.addDouble(15 + 10 * Math2.random());
      }
      Table ttable = new Table();
      ttable.addColumn("X", txCol);
      ttable.addColumn("Y", tyCol);

      graphDataLayer =
          new GraphDataLayer(
              -1, // which pointScreen
              0,
              1,
              -1,
              -1,
              -1, // x,y, others unused
              GraphDataLayer.DRAW_MARKERS,
              true,
              false, // x,yIsTimeAxis
              "Time",
              "Y Axis Title", // x,yAxisTitle for now, always std units
              null, // so not in legend
              "title2",
              "title3",
              "title4", // will be ignored
              ttable,
              null,
              null,
              null,
              new java.awt.Color(0x0099FF),
              i,
              GraphDataLayer.MARKER_SIZE_SMALL,
              0, // vectorStandard
              GraphDataLayer.REGRESS_NONE); // else mean line for each marker type
      graphDataLayers1.add(graphDataLayer);
    }

    // graph 2: make a graphDataLayer with no data
    graphDataLayer =
        new GraphDataLayer(
            -1, // which pointScreen
            0,
            1,
            -1,
            -1,
            -1, // x,y, others unused
            GraphDataLayer.DRAW_MARKERS,
            false,
            false, // x,yIsTimeAxis
            "X Axis Title",
            "Y Axis Title", // for now, always std units
            "This is the really, really, extra long and very nice bold title.",
            "This is a really, really, extra long and very nice title2.",
            "This is a really, really, extra long and very nice title3.",
            "title4",
            table2,
            null,
            null,
            null,
            new java.awt.Color(0x0099FF),
            GraphDataLayer.MARKER_TYPE_FILLED_CIRCLE,
            GraphDataLayer.MARKER_SIZE_MEDIUM,
            0, // vectorStandard
            GraphDataLayer.REGRESS_MEAN);
    ArrayList<GraphDataLayer> graphDataLayers2 = new ArrayList();
    graphDataLayers2.add(graphDataLayer);

    // graph 3: make a graphDataLayer with data for a sticks graph
    graphDataLayer =
        new GraphDataLayer(
            -1, // which pointScreen
            0,
            1,
            2,
            -1,
            -1, // x,u,v
            GraphDataLayer.DRAW_STICKS,
            true,
            false, // x,yIsTimeAxis
            "Time",
            "Y Axis Title", // for now, always std units
            "This is the really, really, extra long and very nice bold title.",
            "This is a really, really, extra long and very nice title2.",
            "This is a really, really, extra long and very nice title3.",
            "title4",
            table3,
            null,
            null,
            null,
            new java.awt.Color(0xFF9900),
            GraphDataLayer.MARKER_TYPE_NONE,
            0,
            0, // vectorStandard
            GraphDataLayer.REGRESS_MEAN);
    ArrayList graphDataLayers3 = new ArrayList();
    graphDataLayers3.add(graphDataLayer);

    // graph 4: make a graphDataLayer with data for a time series line
    graphDataLayer =
        new GraphDataLayer(
            -1, // which pointScreen
            0,
            1,
            -1,
            -1,
            -1, // x,y, others unused
            GraphDataLayer.DRAW_MARKERS_AND_LINES,
            true,
            false, // x,yIsTimeAxis
            "Time",
            "Y Axis Title", // for now, always std units
            "This is the  extra long and very nice bold title (REGRESS=NONE).",
            "This is a really, really, extra long and very nice title2.",
            "This is a really, really, extra long and very nice title3.",
            "title4",
            table1,
            null,
            null,
            null,
            new java.awt.Color(0x0099FF),
            GraphDataLayer.MARKER_TYPE_PLUS,
            GraphDataLayer.MARKER_SIZE_MEDIUM,
            0, // vectorStandard
            GraphDataLayer.REGRESS_NONE);
    ArrayList graphDataLayers4 = new ArrayList();
    graphDataLayers4.add(graphDataLayer);

    // graph 4: plus 10 random points of each marker type
    for (int i = 1; i <= lastMarker; i++) {
      // make a data file with data
      PrimitiveArray txCol = new DoubleArray();
      PrimitiveArray tyCol = new DoubleArray();
      for (int j = 0; j < 10; j++) {
        txCol.addDouble(1.500e9 + Math2.random() * .0025e9);
        tyCol.addDouble(1.5 + Math2.random());
      }
      Table ttable = new Table();
      ttable.addColumn("X", txCol);
      ttable.addColumn("Y", tyCol);

      graphDataLayer =
          new GraphDataLayer(
              -1, // which pointScreen
              0,
              1,
              -1,
              -1,
              -1, // x,y, others unused
              GraphDataLayer.DRAW_MARKERS,
              true,
              false, // x,yIsTimeAxis
              "Time",
              "Y Axis Title", // for now, always std units
              null, // so no legend entry
              "title2",
              "title3",
              "title4", // will be ignored
              ttable,
              null,
              null,
              null,
              new java.awt.Color(0x0099FF),
              i,
              GraphDataLayer.MARKER_SIZE_MEDIUM,
              0, // vectorStandard
              GraphDataLayer.REGRESS_NONE); // if MEAN, you get a line for each marker type
      graphDataLayers4.add(graphDataLayer);
    }

    // graph 5: make a graphDataLayer with data for a time:time graph
    graphDataLayer =
        new GraphDataLayer(
            -1, // which pointScreen
            0,
            1,
            -1,
            -1,
            -1, // x,y
            GraphDataLayer.DRAW_MARKERS,
            true,
            false, // x,yIsTimeAxis
            "Time",
            "Y Axis Title", // for now, always std units
            "This is the long and very nice bold title.  REGRESS=NONE",
            "This is a really, really, extra long and very nice title2.",
            "This is a really, really, extra long and very nice title3.",
            "title4",
            table5,
            null,
            null,
            null,
            new java.awt.Color(0xFF0000),
            GraphDataLayer.MARKER_TYPE_FILLED_SQUARE,
            GraphDataLayer.MARKER_SIZE_SMALL,
            0, // vectorStandard
            GraphDataLayer.REGRESS_NONE);
    ArrayList<GraphDataLayer> graphDataLayers5 = new ArrayList();
    graphDataLayers5.add(graphDataLayer);

    // graph 6: make a graphDataLayer with data for a x=data, y=time line
    graphDataLayer =
        new GraphDataLayer(
            -1, // which pointScreen
            1,
            0,
            -1,
            -1,
            -1, // x,y, others unused //x and y swapped from graph 1
            GraphDataLayer.DRAW_MARKERS_AND_LINES,
            false,
            true,
            "Time",
            "Y Axis Title", // x,yAxisTitle for now, always std units
            "This is the really, really, extra long and very nice bold title.",
            "This is a really, really, extra long and very nice title2.",
            "This is a really, really, extra long and very nice title3.",
            "title4",
            table1,
            null,
            null,
            null,
            new java.awt.Color(0x0099FF),
            GraphDataLayer.MARKER_TYPE_PLUS,
            GraphDataLayer.MARKER_SIZE_SMALL,
            0, // vectorStandard
            GraphDataLayer.REGRESS_MEAN);
    ArrayList graphDataLayers6 = new ArrayList();
    graphDataLayers6.add(graphDataLayer);

    // draw the graph with data
    BufferedImage bufferedImage = SgtUtil.getBufferedImage(width + 20, height + 20);
    BufferedImage bufferedImage1 = SgtUtil.getBufferedImage(300, 300);
    BufferedImage bufferedImage2 = SgtUtil.getBufferedImage(300, 300);
    BufferedImage bufferedImage3 = SgtUtil.getBufferedImage(300, 300);
    BufferedImage bufferedImage4 = SgtUtil.getBufferedImage(300, 300);
    BufferedImage bufferedImage5 = SgtUtil.getBufferedImage(300, 300);
    BufferedImage bufferedImage6 = SgtUtil.getBufferedImage(300, 300);
    Graphics2D g2 = (Graphics2D) bufferedImage.getGraphics();
    Graphics2D g21 = (Graphics2D) bufferedImage1.getGraphics();
    Graphics2D g22 = (Graphics2D) bufferedImage2.getGraphics();
    Graphics2D g23 = (Graphics2D) bufferedImage3.getGraphics();
    Graphics2D g24 = (Graphics2D) bufferedImage4.getGraphics();
    Graphics2D g25 = (Graphics2D) bufferedImage5.getGraphics();
    Graphics2D g26 = (Graphics2D) bufferedImage6.getGraphics();

    // graph 1
    String2.log("Graph 1");
    sgtGraph.makeGraph(
        true,
        "xAxisTitle",
        "yAxisTitle",
        SgtUtil.LEGEND_BELOW,
        "Graph 1,",
        "x is TimeAxis",
        imageDir,
        "noaa20.gif",
        Double.NaN,
        Double.NaN,
        true,
        true,
        xIsLogAxis, // isAscending, isTimeAxis, isLog
        Double.NaN,
        Double.NaN,
        true,
        false,
        yIsLogAxis,
        graphDataLayers1,
        g21,
        0,
        0,
        300,
        300,
        2, // graph width/height
        SgtGraph.DefaultBackgroundColor,
        1); // fontScale
    sgtGraph.makeGraph(
        false,
        "xAxisTitle",
        "yAxisTitle",
        SgtUtil.LEGEND_BELOW,
        "Graph 1,",
        "x is TimeAxis",
        imageDir,
        "noaa20.gif",
        Double.NaN,
        Double.NaN,
        true,
        true,
        xIsLogAxis, // isAscending, isTimeAxis, isLog
        Double.NaN,
        Double.NaN,
        true,
        false,
        yIsLogAxis,
        graphDataLayers1,
        g2,
        0,
        0, // upperLeft
        width / 2,
        height / 3,
        2, // graph width/height
        SgtGraph.DefaultBackgroundColor,
        1); // fontScale

    // graph 2
    String2.log("Graph 2");
    sgtGraph.makeGraph(
        true,
        "xAxisTitle",
        "yAxisTitle",
        SgtUtil.LEGEND_BELOW,
        "Graph 2,",
        "no time, no data.",
        imageDir,
        "noaa20.gif",
        Double.NaN,
        Double.NaN,
        true,
        false,
        xIsLogAxis, // isAscending, isTimeAxis, isLog
        Double.NaN,
        Double.NaN,
        true,
        false,
        yIsLogAxis,
        graphDataLayers2,
        g22,
        0,
        0,
        300,
        300,
        2, // graph width/height
        SgtGraph.DefaultBackgroundColor,
        1.5); // fontScale
    sgtGraph.makeGraph(
        false,
        "xAxisTitle",
        "yAxisTitle",
        SgtUtil.LEGEND_BELOW,
        "Graph 2,",
        "no time, no data.",
        imageDir,
        "noaa20.gif",
        Double.NaN,
        Double.NaN,
        true,
        false,
        xIsLogAxis, // isAscending, isTimeAxis, isLog
        Double.NaN,
        Double.NaN,
        true,
        false,
        yIsLogAxis,
        graphDataLayers2,
        g2,
        width / 2 + 10,
        0, // upper Right
        width / 2,
        height / 3,
        2, // graph width/height
        SgtGraph.DefaultBackgroundColor,
        1.5); // fontScale

    // graph 3
    String2.log("Graph 3");
    sgtGraph.makeGraph(
        true,
        "xAxisTitle",
        "yAxisTitle",
        SgtUtil.LEGEND_BELOW,
        "Graph 3,",
        "stick graph",
        imageDir,
        "noaa20.gif",
        Double.NaN,
        Double.NaN,
        true,
        true,
        xIsLogAxis, // isAscending, isTimeAxis, isLog
        Double.NaN,
        Double.NaN,
        true,
        false,
        yIsLogAxis,
        graphDataLayers3,
        g23,
        0,
        0,
        300,
        300,
        2, // graph width/height
        SgtGraph.DefaultBackgroundColor,
        1); // fontScale
    sgtGraph.makeGraph(
        false,
        "xAxisTitle",
        "yAxisTitle",
        SgtUtil.LEGEND_BELOW,
        "Graph 3,",
        "stick graph",
        imageDir,
        "noaa20.gif",
        Double.NaN,
        Double.NaN,
        true,
        true,
        xIsLogAxis, // isAscending, isTimeAxis, isLog
        Double.NaN,
        Double.NaN,
        true,
        false,
        yIsLogAxis,
        graphDataLayers3,
        g2,
        0,
        height / 3, // mid Left
        width / 2,
        height / 3,
        2, // graph width/height
        SgtGraph.DefaultBackgroundColor,
        1); // fontScale

    // graph 4
    String2.log("Graph 4");
    sgtGraph.makeGraph(
        true,
        "xAxisTitle",
        "yAxisTitle",
        SgtUtil.LEGEND_BELOW,
        "Graph 4,",
        "x is TimeAxis",
        imageDir,
        "noaa20.gif",
        Double.NaN,
        Double.NaN,
        true,
        true,
        xIsLogAxis, // isAscending, isTimeAxis, isLog
        Double.NaN,
        Double.NaN,
        true,
        false,
        yIsLogAxis,
        graphDataLayers4,
        g24,
        0,
        0,
        300,
        300,
        2, // graph width/height
        SgtGraph.DefaultBackgroundColor,
        1); // fontScale
    sgtGraph.makeGraph(
        false,
        "xAxisTitle",
        "yAxisTitle",
        SgtUtil.LEGEND_BELOW,
        "Graph 4,",
        "x is TimeAxis",
        imageDir,
        "noaa20.gif",
        Double.NaN,
        Double.NaN,
        true,
        true,
        xIsLogAxis, // isAscending, isTimeAxis, isLog
        Double.NaN,
        Double.NaN,
        true,
        false,
        yIsLogAxis,
        graphDataLayers4,
        g2,
        width / 2 + 10,
        height / 3, // mid Right
        width / 2,
        height / 3,
        2, // graph width/height
        SgtGraph.DefaultBackgroundColor,
        1); // fontScale

    // graph 5
    String2.log("Graph 5");
    sgtGraph.makeGraph(
        true,
        "xAxisTitle",
        "yAxisTitle",
        SgtUtil.LEGEND_BELOW,
        "Graph 5,",
        "2 time axis! y->not",
        imageDir,
        "noaa20.gif",
        Double.NaN,
        Double.NaN,
        true,
        true,
        xIsLogAxis, // isAscending, isTimeAxis, isLog
        Double.NaN,
        Double.NaN,
        true,
        true,
        yIsLogAxis,
        graphDataLayers5,
        g25,
        0,
        0,
        300,
        300,
        2, // graph width/height
        SgtGraph.DefaultBackgroundColor,
        1); // fontScale
    sgtGraph.makeGraph(
        false,
        "xAxisTitle",
        "yAxisTitle",
        SgtUtil.LEGEND_BELOW,
        "Graph 5,",
        "2 time axis! y->not",
        imageDir,
        "noaa20.gif",
        Double.NaN,
        Double.NaN,
        true,
        true,
        xIsLogAxis, // isAscending, isTimeAxis, isLog
        Double.NaN,
        Double.NaN,
        true,
        true,
        yIsLogAxis,
        graphDataLayers5,
        g2,
        0,
        height * 2 / 3, // low left
        width / 2,
        height / 3,
        2, // graph width/height
        SgtGraph.DefaultBackgroundColor,
        1); // fontScale

    // graph 6
    String2.log("Graph 6");
    sgtGraph.makeGraph(
        true,
        "xAxisTitle",
        "yAxisTitle",
        SgtUtil.LEGEND_BELOW,
        "Graph 6,",
        "y is TimeAxis",
        imageDir,
        "noaa20.gif",
        Double.NaN,
        Double.NaN,
        true,
        false,
        xIsLogAxis, // isAscending, isTimeAxis, isLog
        Double.NaN,
        Double.NaN,
        true,
        true,
        yIsLogAxis,
        graphDataLayers6,
        g26,
        0,
        0,
        300,
        300,
        2, // graph width/height
        SgtGraph.DefaultBackgroundColor,
        1); // fontScale
    sgtGraph.makeGraph(
        false,
        "xAxisTitle",
        "yAxisTitle",
        SgtUtil.LEGEND_BELOW,
        "Graph 6,",
        "y is TimeAxis",
        imageDir,
        "noaa20.gif",
        Double.NaN,
        Double.NaN,
        true,
        false,
        xIsLogAxis, // isAscending, isTimeAxis, isLog
        Double.NaN,
        Double.NaN,
        true,
        true,
        yIsLogAxis,
        graphDataLayers6,
        g2,
        width / 2 + 10,
        height * 2 / 3, // low right
        width / 2,
        height / 3,
        2, // graph width/height
        SgtGraph.DefaultBackgroundColor,
        1); // fontScale

    // make sure old files are deleted
    String fileName = Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR) + baseImageName;
    File2.delete(fileName + ".png");
    File2.delete(fileName + "1.png");
    File2.delete(fileName + "2.png");
    File2.delete(fileName + "3.png");
    File2.delete(fileName + "4.png");
    File2.delete(fileName + "5.png");
    File2.delete(fileName + "6.png");

    // save image
    SgtUtil.saveImage(bufferedImage, fileName + ".png");
    if (testAllAndDisplay) {
      SgtUtil.saveImage(bufferedImage1, fileName + "1.png");
      SgtUtil.saveImage(bufferedImage2, fileName + "2.png");
      SgtUtil.saveImage(bufferedImage3, fileName + "3.png");
      SgtUtil.saveImage(bufferedImage4, fileName + "4.png");
      SgtUtil.saveImage(bufferedImage5, fileName + "5.png");
      SgtUtil.saveImage(bufferedImage6, fileName + "6.png");
    } else {
      String2.log("fileName=" + fileName + ".png");
    }

    // view it
    // Test.displayInBrowser("file://" + fileName + ".png");
    Image2Tests.testImagesIdentical(
        baseImageName + ".png", baseImageName + ".png", baseImageName + "_diff.png");
    Math2.sleep(2000);

    if (testAllAndDisplay) {
      for (int ti = 1; ti <= 6; ti++) {
        // Test.displayInBrowser("file://" + fileName + ti + ".png");
        Image2Tests.testImagesIdentical(
            baseImageName + ti + ".png",
            baseImageName + ti + ".png",
            baseImageName + ti + "_diff.png");
        Math2.sleep(400);
      }
    }

    // done
    Math2.gcAndWait("SgtGraph (between tests)");
    Math2.gcAndWait("SgtGraph (between tests)"); // in a test. Ensure all are garbage collected.
    String2.log("time=" + (System.currentTimeMillis() - time) + "ms\n" + Math2.memoryString());
  }

  // if (test == 4) testSurface(false, false); //xIsLogAxis, yIsLogAxis
  // if (test == 5) testSurface(false, true);
  // if (test == 6) testSurface(true, true);

  @org.junit.jupiter.api.Test
  @TagImageComparison
  void testSurface_ff() throws Exception {
    testSurface(false, false);
  }

  @org.junit.jupiter.api.Test
  @TagImageComparison
  void testSurface_ft() throws Exception {
    testSurface(false, true);
  }

  @org.junit.jupiter.api.Test
  @TagImageComparison
  void testSurface_tt() throws Exception {
    testSurface(true, true);
  }

  private static void testSurface(boolean xIsLogAxis, boolean yIsLogAxis) throws Exception {
    // verbose = true;
    // reallyVerbose = true;
    // PathCartesianRenderer.verbose = true;
    // PathCartesianRenderer.reallyVerbose = true;
    // AttributedString2.verbose = true;
    Math2.gcAndWait("SgtGraph (between tests)");
    Math2.gcAndWait(
        "SgtGraph (between tests)"); // in a test, before getMemoryInUse(). Ensure all garbage
    // collected.
    long time = System.currentTimeMillis();
    long memoryInUse = Math2.getMemoryInUse();

    SgtGraph sgtGraph =
        new SgtGraph("SansSerif"); // "DejaVu Sans" "Bitstream Vera Sans"); //"SansSerif" is safe
    // choice
    String imageDir =
        EDStatic.getWebInfParentDirectory()
            + // with / separator and / at the end
            "images/";

    int width = 400;
    int height = 300;

    // make a grid
    Grid grid = new Grid();
    grid.lon = new double[] {1, 2, 3, 4};
    grid.lat = new double[] {10, 20, 30, 40, 50};
    grid.data =
        new double[] { // mv's are NaNs
          1, 2, 3, 4, // leftmost column, going up
          5, 6, 7, 8,
          9, 10, 11, 12,
          13, 14, 15, 16,
          17, 18, 19, 20
        }; // rightmost column, going up
    grid.setLonLatSpacing();
    grid.calculateStats();

    CompoundColorMap cColorMap =
        new CompoundColorMap(
            // String baseDir, String palette, String scale, double minData,
            // double maxData, int nSections, boolean continuous, String resultDir)
            EDStatic.getWebInfParentDirectory() + "WEB-INF/cptfiles/",
            "Rainbow",
            "linear",
            0,
            20,
            5,
            true,
            SSR.getTempDirectory());

    // graph 1: make a graphDataLayer with data for a time series line
    GraphDataLayer graphDataLayer =
        new GraphDataLayer(
            -1, // which pointScreen
            0,
            1,
            -1,
            -1,
            -1, // x,y, others unused
            GraphDataLayer.DRAW_COLORED_SURFACE,
            true,
            false,
            "X Axis",
            "Y Axis Title", // x,yAxisTitle for now, always std units
            "This is the really, really, extra long and very nice bold title.",
            "This is a really, really, extra long and very nice title2.",
            "This is a really, really, extra long and very nice title3.",
            "This is title4.",
            null,
            grid,
            null,
            cColorMap,
            new java.awt.Color(0x0099FF),
            GraphDataLayer.MARKER_TYPE_PLUS,
            GraphDataLayer.MARKER_SIZE_SMALL,
            0, // vectorStandard
            GraphDataLayer.REGRESS_NONE);
    ArrayList<GraphDataLayer> graphDataLayers1 = new ArrayList();
    graphDataLayers1.add(graphDataLayer);

    // draw the graph with data
    BufferedImage bufferedImage = SgtUtil.getBufferedImage(width * 2, height * 2);
    Graphics2D g2 = (Graphics2D) bufferedImage.getGraphics();

    sgtGraph.makeGraph(
        false,
        "xAxisTitle",
        "yAxisTitle",
        SgtUtil.LEGEND_BELOW,
        "Standard,",
        "some text",
        imageDir,
        "noaa20.gif",
        Double.NaN,
        Double.NaN,
        true,
        false,
        xIsLogAxis, // isAscending, isTimeAxis, isLog
        Double.NaN,
        Double.NaN,
        true,
        false,
        yIsLogAxis,
        graphDataLayers1,
        g2,
        0,
        0, // upperLeft
        width,
        height,
        2, // graph width/height
        new Color(0x808080),
        1); // gray, fontScale

    sgtGraph.makeGraph(
        false,
        "xAxisTitle",
        "yAxisTitle",
        SgtUtil.LEGEND_BELOW,
        "Flip X,",
        "some text",
        imageDir,
        "noaa20.gif",
        Double.NaN,
        Double.NaN,
        false,
        false,
        xIsLogAxis, // isAscending, isTimeAxis, isLog
        Double.NaN,
        Double.NaN,
        true,
        false,
        yIsLogAxis,
        graphDataLayers1,
        g2,
        width,
        0, // upperLeft
        width,
        height,
        2, // graph width/height
        new Color(0x808080),
        1); // gray, fontScale

    sgtGraph.makeGraph(
        false,
        "xAxisTitle",
        "yAxisTitle",
        SgtUtil.LEGEND_BELOW,
        "Flip Y",
        "some text",
        imageDir,
        "noaa20.gif",
        Double.NaN,
        Double.NaN,
        true,
        false,
        xIsLogAxis, // isAscending, isTimeAxis, isLog
        Double.NaN,
        Double.NaN,
        false,
        false,
        yIsLogAxis,
        graphDataLayers1,
        g2,
        0,
        height, // upperLeft
        width,
        height,
        2, // graph width/height
        new Color(0x808080),
        1); // gray, fontScale

    sgtGraph.makeGraph(
        false,
        "xAxisTitle",
        "yAxisTitle",
        SgtUtil.LEGEND_BELOW,
        "Flip X & Y,",
        "some text",
        imageDir,
        "noaa20.gif",
        Double.NaN,
        Double.NaN,
        false,
        false,
        xIsLogAxis, // isAscending, isTimeAxis false,
        Double.NaN,
        Double.NaN,
        false,
        false,
        yIsLogAxis,
        graphDataLayers1,
        g2,
        width,
        height, // upperLeft
        width,
        height,
        2, // graph width/height
        new Color(0x808080),
        1); // gray, fontScale

    // save image
    String fileName = "SgtGraphTestSurface" + (xIsLogAxis ? "X" : "") + (yIsLogAxis ? "Y" : "");
    String obsDir = Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR);
    SgtUtil.saveImage(bufferedImage, obsDir + fileName + ".png");

    // view it
    // Test.displayInBrowser("file://" + fileName);
    Image2Tests.testImagesIdentical(fileName + ".png", fileName + ".png", fileName + "_diff.png");

    Math2.gc("SgtGraph.testSurface (between tests)", 2000);
    // String2.pressEnterToContinue();

    // delete files
    File2.delete(fileName);

    // done
    Math2.gcAndWait("SgtGraph (between tests)");
    Math2.gcAndWait(
        "SgtGraph (between tests)"); // in a test, before getMemoryInUse(). Ensure all garbage
    // collected.
    String2.log(
        "time="
            + (System.currentTimeMillis() - time)
            + "ms "
            + "changeInMemoryInUse="
            + (Math2.getMemoryInUse() - memoryInUse));
  }
}
