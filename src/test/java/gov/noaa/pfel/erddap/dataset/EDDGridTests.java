package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.File2;
import com.cohort.util.Image2Tests;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.text.MessageFormat;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeAll;
import tags.TagImageComparison;
import tags.TagIncompleteTest;
import tags.TagThredds;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

class EDDGridTests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /**
   * Test saveAsImage, specifically to make sure a transparent png that's partially outside of the
   * range of the dataset still returns the image for the part that is within range.
   */
  @org.junit.jupiter.api.Test
  @TagImageComparison
  void testSaveAsImage() throws Throwable {
    // String2.log("\n*** EDDGrid.testSaveAsImage()");
    EDDGrid eddGrid = (EDDGrid) EDDTestDataset.geterdMH1chlamday();
    // EDDGrid eddGrid = (EDDGrid) EDDGrid.oneFromDatasetsXml(null,
    // "erdMHchla8day");
    int language = 0;
    String dir = Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR);
    // String requestUrl = "/erddap/griddap/erdMHchla8day.transparentPng";
    String userDapQueryTemplate =
        "MWchla%5B(2022-01-16T12:00:00Z):1:(2022-01-16T12:00:00Z)%5D%5B(0.0):1:(0.0)%5D%5B({0,number,#.##########}):1:({1,number,#.##########})%5D%5B({2,number,#.##########}):1:({3,number,#.##########})%5D";
    String baseName, tName;

    // Make fully valid image
    baseName = "EDDGrid_testSaveAsImage_fullyValid";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            MessageFormat.format(userDapQueryTemplate, 30, 40, 210, 220), // #'s are minLat, maxLat,
            // minLon, maxLon
            dir,
            baseName,
            ".transparentPng");
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // Invalid min y.
    baseName = "EDDGrid_testSaveAsImage_invalidMinY";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            MessageFormat.format(userDapQueryTemplate, -100, 40, 210, 220), // #'s are minLat,
            // maxLat, minLon,
            // maxLon
            dir,
            baseName,
            ".transparentPng");
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // 2020-08-03 For tests below, some generated images have data, some don't,
    // but results seem inconsistent.
    // The images in erddapTest/images are old and I'm not sure appropriate.
    // I'm not sure what they should be. Leave this for Chris John.

    // Invalid max y.
    baseName = "EDDGrid_testSaveAsImage_invalidMaxY";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            MessageFormat.format(userDapQueryTemplate, 30, 100, 210, 220), // #'s are minLat,
            // maxLat, minLon, maxLon
            dir,
            baseName,
            ".transparentPng");
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // All invalid.
    baseName = "EDDGrid_testSaveAsImage_allInvalid";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            MessageFormat.format(userDapQueryTemplate, -100, 100, -200, 370), // #'s are minLat,
            // maxLat, minLon,
            // maxLon
            dir,
            baseName,
            ".transparentPng");
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // Out of range min x.
    baseName = "EDDGrid_testSaveAsImage_OORMinX";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            MessageFormat.format(userDapQueryTemplate, 30, 40, 200, 210), // #'s are minLat, maxLat,
            // minLon, maxLon
            dir,
            baseName,
            ".transparentPng");
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // Out of range max x.
    baseName = "EDDGrid_testSaveAsImage_OORMaxX";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            MessageFormat.format(userDapQueryTemplate, 30, 40, 250, 260), // #'s are minLat, maxLat,
            // minLon, maxLon
            dir,
            baseName,
            ".transparentPng");
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // Out of range min y.
    baseName = "EDDGrid_testSaveAsImage_OORMinY";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            MessageFormat.format(userDapQueryTemplate, 20, 30, 210, 220), // #'s are minLat, maxLat,
            // minLon, maxLon
            dir,
            baseName,
            ".transparentPng");
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // Out of range max y.
    baseName = "EDDGrid_testSaveAsImage_OORMaxY";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            MessageFormat.format(userDapQueryTemplate, 50, 60, 210, 220), // #'s are minLat, maxLat,
            // minLon, maxLon
            dir,
            baseName,
            ".transparentPng");
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // Fully out of range min x.
    baseName = "EDDGrid_testSaveAsImage_FOORMinX";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            MessageFormat.format(userDapQueryTemplate, 30, 40, 190, 200), // #'s are minLat, maxLat,
            // minLon, maxLon
            dir,
            baseName,
            ".transparentPng");
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // Fully out of range max x.
    baseName = "EDDGrid_testSaveAsImage_FOORMaxX";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            MessageFormat.format(userDapQueryTemplate, 30, 40, 260, 270), // #'s are minLat, maxLat,
            // minLon, maxLon
            dir,
            baseName,
            ".transparentPng");
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // Fully out of range min y.
    baseName = "EDDGrid_testSaveAsImage_FOORMinY";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            MessageFormat.format(userDapQueryTemplate, 10, 20, 210, 220), // #'s are minLat, maxLat,
            // minLon, maxLon
            dir,
            baseName,
            ".transparentPng");
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // Fully out of range max y.
    baseName = "EDDGrid_testSaveAsImage_FOORMaxY";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            MessageFormat.format(userDapQueryTemplate, 60, 70, 210, 220), // #'s are minLat, maxLat,
            // minLon, maxLon
            dir,
            baseName,
            ".transparentPng");
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");
  }

  /** Test getting geotiffs. */
  @org.junit.jupiter.api.Test
  @TagImageComparison
  void testGeotif() throws Throwable {
    // String2.log("\n*** EDDGridFromDap.testGeotif");
    // EDD.testVerbose(false);
    int language = 0;
    EDDGrid gridDataset = (EDDGrid) EDDTestDataset.getetopo180();
    String mapDapQuery = "altitude%5B(-90.0):(-88.0)%5D%5B(-180.0):(-178.0)%5D";
    String tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            mapDapQuery,
            Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR),
            "testGeotif",
            ".geotif");

    // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    Image2Tests.testImagesIdentical(
        tName, "EDDGrid_testGeotif" + ".tif", "EDDGrid_testGeotif" + "_diff.png");
  }

  @org.junit.jupiter.api.Test
  @TagImageComparison
  @TagThredds
  void testGeotif2() throws Throwable {
    String tDir = Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR);
    EDDGrid gridDataset = (EDDGrid) EDDTestDataset.geterdMHchla8day();
    String mapDapQuery =
        "chlorophyll%5B(2013-10-12T00:00:00Z)%5D%5B(0.0)%5D%5B(-90.0):(90.0)%5D%5B(0.0):(175.0)%5D&.draw=surface&.vars=longitude%7Clatitude%7Cchlorophyll&.colorBar=%7C%7C%7C%7C%7C&.bgColor=0xffccccff";

    String tName;
    int language = 0;

    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            mapDapQuery,
            tDir,
            gridDataset.className() + "_Map_mhcla",
            ".geotif");
    Image2Tests.testImagesIdentical(
        tName, "EDDGrid_testGeotif2_mhchla" + ".tif", "EDDGrid_testGeotif2_mhchla" + "_diff.png");

    gridDataset = (EDDGrid) EDDTestDataset.gettestSuperPreciseTimeUnits();
    mapDapQuery =
        "wind_speed%5B(2020-01-01T23:30:00Z)%5D%5B(-39.9):(39.9)%5D%5B(0.1):(175.0)%5D&.draw=surface&.vars=longitude%7Clatitude%7Cwind_speed&.colorBar=%7C%7C%7C%7C%7C&.bgColor=0xffccccff";

    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            mapDapQuery,
            tDir,
            gridDataset.className() + "_Map_windspeed",
            ".geotif");
    Image2Tests.testImagesIdentical(
        tName,
        "EDDGrid_testGeotif2_windSpeed" + ".tif",
        "EDDGrid_testGeotif2_windSpeed" + "_diff.png");
  }

  @org.junit.jupiter.api.Test
  @TagImageComparison
  void testGraphics() throws Throwable {
    // String2.log("*** EDDGrid.testGraphics(" + testAll + ")\n" +
    // "!!! This requires erdMHchla8day in localhost erddap to display in Google
    // Earth.");
    // testVerboseOn();
    boolean testAll = true;
    String tDir = Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR);
    EDDGrid gridDataset = (EDDGrid) EDDTestDataset.geterdMH1chla8day();
    String graphDapQuery = SSR.percentEncode("chlorophyll[0:.1:1][][(29)][(225)]");
    String mapDapQuery = SSR.percentEncode("chlorophyll[1][][(29):(45)][(225):(247)]"); // stride
    // irrelevant
    String kmlQuery = SSR.percentEncode("chlorophyll[1][][(29):(45)]");
    String tName, results;
    int language = 0;

    // *** test getting graphs
    String2.log("\n****************** EDDGrid test get graphs\n");

    // test graph .png
    if (testAll || false) {
      tName =
          gridDataset.makeNewFileForDapQuery(
              language,
              null,
              null,
              graphDapQuery + "&.size=128|240&.font=0.75",
              tDir,
              gridDataset.className() + "_GraphTiny",
              ".largePng"); // to show it's irrelevant
      // Test.displayInBrowser("file://" + tDir + tName);
      Image2Tests.testImagesIdentical(
          tName,
          "EDDGridTestGraphics_GraphTiny" + ".png",
          "EDDGridTestGraphics_GraphTiny" + "_diff.png");

      tName =
          gridDataset.makeNewFileForDapQuery(
              language,
              null,
              null,
              graphDapQuery,
              tDir,
              gridDataset.className() + "_GraphS",
              ".smallPng");
      // Test.displayInBrowser("file://" + tDir + tName);
      Image2Tests.testImagesIdentical(
          tName,
          "EDDGridTestGraphics_GraphSmall" + ".png",
          "EDDGridTestGraphics_GraphSmall" + "_diff.png");

      tName =
          gridDataset.makeNewFileForDapQuery(
              language,
              null,
              null,
              graphDapQuery,
              tDir,
              gridDataset.className() + "_Graph",
              ".png");
      // Test.displayInBrowser("file://" + tDir + tName);
      Image2Tests.testImagesIdentical(
          tName,
          "EDDGridTestGraphics_GraphPng" + ".png",
          "EDDGridTestGraphics_GraphPng" + "_diff.png");

      tName =
          gridDataset.makeNewFileForDapQuery(
              language,
              null,
              null,
              graphDapQuery,
              tDir,
              gridDataset.className() + "_GraphL",
              ".largePng");
      // Test.displayInBrowser("file://" + tDir + tName);
      Image2Tests.testImagesIdentical(
          tName, "EDDGridTestGraphics_GraphL" + ".png", "EDDGridTestGraphics_GraphL" + "_diff.png");

      tName =
          gridDataset.makeNewFileForDapQuery(
              language,
              null,
              null,
              graphDapQuery + "&.size=1700|1800&.font=3",
              tDir,
              gridDataset.className() + "_GraphHuge",
              ".smallPng"); // to show it's irrelevant
      // Test.displayInBrowser("file://" + tDir + tName);
      Image2Tests.testImagesIdentical(
          tName,
          "EDDGridTestGraphics_GraphHuge" + ".png",
          "EDDGridTestGraphics_GraphHuge" + "_diff.png");
    }

    // test graph .pdf
    if (testAll || false) {
      tName =
          gridDataset.makeNewFileForDapQuery(
              language,
              null,
              null,
              graphDapQuery,
              tDir,
              gridDataset.className() + "_GraphS",
              ".smallPdf");
      // Test.displayInBrowser("file://" + tDir + tName);

      tName =
          gridDataset.makeNewFileForDapQuery(
              language,
              null,
              null,
              graphDapQuery,
              tDir,
              gridDataset.className() + "_Graph",
              ".pdf");
      // Test.displayInBrowser("file://" + tDir + tName);

      tName =
          gridDataset.makeNewFileForDapQuery(
              language,
              null,
              null,
              graphDapQuery,
              tDir,
              gridDataset.className() + "_GraphL",
              ".largePdf");
      // Test.displayInBrowser("file://" + tDir + tName);
    }

    // test legend= options
    if (testAll || false) {
      tName =
          gridDataset.makeNewFileForDapQuery(
              language,
              null,
              null,
              graphDapQuery + "&.legend=Off&.trim=10",
              tDir,
              gridDataset.className() + "_GraphLegendOff",
              ".png");
      // Test.displayInBrowser("file://" + tDir + tName);
      Image2Tests.testImagesIdentical(
          tName,
          "EDDGridTestGraphics_LegendOff" + ".png",
          "EDDGridTestGraphics_LegendOff" + "_diff.png");

      tName =
          gridDataset.makeNewFileForDapQuery(
              language,
              null,
              null,
              graphDapQuery + "&.legend=Only",
              tDir,
              gridDataset.className() + "_GraphLegendOnlySmall",
              ".smallPng");
      // Test.displayInBrowser("file://" + tDir + tName);
      Image2Tests.testImagesIdentical(
          tName,
          "EDDGridTestGraphics_LegendOnly" + ".png",
          "EDDGridTestGraphics_LegendOnly" + "_diff.png");

      tName =
          gridDataset.makeNewFileForDapQuery(
              language,
              null,
              null,
              graphDapQuery + "&.legend=Only",
              tDir,
              gridDataset.className() + "_GraphLegendOnlyMed",
              ".png");
      // Test.displayInBrowser("file://" + tDir + tName);
      Image2Tests.testImagesIdentical(
          tName,
          "EDDGridTestGraphics_LegendOnlyMed" + ".png",
          "EDDGridTestGraphics_LegendOnlyMed" + "_diff.png");

      tName =
          gridDataset.makeNewFileForDapQuery(
              language,
              null,
              null,
              graphDapQuery + "&.legend=Only",
              tDir,
              gridDataset.className() + "_GraphLegendOnlyLarge",
              ".largePng");
      // Test.displayInBrowser("file://" + tDir + tName);
      Image2Tests.testImagesIdentical(
          tName,
          "EDDGridTestGraphics_LegendOnlyLarge" + ".png",
          "EDDGridTestGraphics_LegendOnlyLarge" + "_diff.png");

      tName =
          gridDataset.makeNewFileForDapQuery(
              language,
              null,
              null,
              graphDapQuery + "&.legend=Only",
              tDir,
              gridDataset.className() + "_GraphTransparentLegendOnly",
              ".transparentPng");
      // Test.displayInBrowser("file://" + tDir + tName);
      Image2Tests.testImagesIdentical(
          tName,
          "EDDGridTestGraphics_LegendOnlyTransparent" + ".png",
          "EDDGridTestGraphics_LegendOnlyTransparent" + "_diff.png");
    }

    // *** test getting colored surface graph
    // String2.log("\n****************** EDDGrid test get colored surface
    // graph\n");
    // not working yet time axis is hard to work with
    // String tempDapQuery = "chlorophyll[0:10:20][][(29):1:(50)][(225):1:(225)]";
    // tName = gridDataset.makeNewFileForDapQuery(language, null, null,
    // tempDapQuery,
    // tDir, gridDataset.className() + "_CSGraph", ".png");
    // Test.displayInBrowser("file://" + tDir + tName);

    // *** test getting map .png
    if (testAll || false) {
      // String2.log("\n****************** EDDGrid test get maps\n");

      tName =
          gridDataset.makeNewFileForDapQuery(
              language,
              null,
              null,
              mapDapQuery + "&.size=120|280&.font=0.75",
              tDir,
              gridDataset.className() + "_MapTiny",
              ".largePng"); // to show it's irrelevant
      // Test.displayInBrowser("file://" + tDir + tName);
      Image2Tests.testImagesIdentical(
          tName,
          "EDDGridTestGraphics_MapTiny" + ".png",
          "EDDGridTestGraphics_MapTiny" + "_diff.png");

      tName =
          gridDataset.makeNewFileForDapQuery(
              language,
              null,
              null,
              mapDapQuery,
              tDir,
              gridDataset.className() + "_MapS",
              ".smallPng");
      // Test.displayInBrowser("file://" + tDir + tName);
      Image2Tests.testImagesIdentical(
          tName,
          "EDDGridTestGraphics_MapSmall" + ".png",
          "EDDGridTestGraphics_MapSmall" + "_diff.png");

      tName =
          gridDataset.makeNewFileForDapQuery(
              language, null, null, mapDapQuery, tDir, gridDataset.className() + "_Map", ".png");
      // Test.displayInBrowser("file://" + tDir + tName);
      Image2Tests.testImagesIdentical(
          tName, "EDDGridTestGraphics_Map" + ".png", "EDDGridTestGraphics_Map" + "_diff.png");

      tName =
          gridDataset.makeNewFileForDapQuery(
              language,
              null,
              null,
              mapDapQuery,
              tDir,
              gridDataset.className() + "_MapL",
              ".largePng");
      // Test.displayInBrowser("file://" + tDir + tName);
      Image2Tests.testImagesIdentical(
          tName,
          "EDDGridTestGraphics_MapLarge" + ".png",
          "EDDGridTestGraphics_MapLarge" + "_diff.png");

      tName =
          gridDataset.makeNewFileForDapQuery(
              language,
              null,
              null,
              mapDapQuery + "&.size=1700|1800&.font=3",
              tDir,
              gridDataset.className() + "_MapHuge",
              ".smallPng"); // to show it's irrelevant
      // Test.displayInBrowser("file://" + tDir + tName);
      Image2Tests.testImagesIdentical(
          tName,
          "EDDGridTestGraphics_MapHuge" + ".png",
          "EDDGridTestGraphics_MapHuge" + "_diff.png");
    }

    // test getting map transparentPng
    if (testAll || false) {

      // yes, stretched. that's what query is asking for
      tName =
          gridDataset.makeNewFileForDapQuery(
              language,
              null,
              null,
              mapDapQuery + "&.size=300|400",
              tDir,
              gridDataset.className() + "_MapTPSmall",
              ".transparentPng");
      // Test.displayInBrowser("file://" + tDir + tName);
      Image2Tests.testImagesIdentical(
          tName,
          "EDDGridTestGraphics_MapTPSmall" + ".png",
          "EDDGridTestGraphics_Map" + "_diff.png");

      tName =
          gridDataset.makeNewFileForDapQuery(
              language,
              null,
              null,
              mapDapQuery,
              tDir,
              gridDataset.className() + "_MapTP",
              ".transparentPng");
      // Test.displayInBrowser("file://" + tDir + tName);
      Image2Tests.testImagesIdentical(
          tName, "EDDGridTestGraphics_MapTP" + ".png", "EDDGridTestGraphics_MapTP" + "_diff.png");
    }

    // test map pdf
    if (testAll || false) {
      tName =
          gridDataset.makeNewFileForDapQuery(
              language,
              null,
              null,
              mapDapQuery,
              tDir,
              gridDataset.className() + "_MapS",
              ".smallPdf");
      // Test.displayInBrowser("file://" + tDir + tName);

      tName =
          gridDataset.makeNewFileForDapQuery(
              language, null, null, mapDapQuery, tDir, gridDataset.className() + "_Map", ".pdf");
      // Test.displayInBrowser("file://" + tDir + tName);

      tName =
          gridDataset.makeNewFileForDapQuery(
              language,
              null,
              null,
              mapDapQuery,
              tDir,
              gridDataset.className() + "_MapL",
              ".largePdf");
      // Test.displayInBrowser("file://" + tDir + tName);
    }

    // test map kml
    // !!! This requires erdMHchla8day in localhost erddap to display in Google
    // Earth
    if (testAll || false) {
      tName =
          gridDataset.makeNewFileForDapQuery(
              language, null, null, kmlQuery, tDir, gridDataset.className() + "_Map", ".kml");
      results = File2.directReadFromUtf8File(tDir + tName);
      // String2.log("results=\n" + results);
      // Test.displayInBrowser("file://" + tDir + tName);
    }
  }

  /**
   * Tests input for saveAsImage against the provided output. Specifically the output is provided as
   * a hash (sha-256) of the output bytes.
   *
   * @param eddGrid EDDGrid that saveAsImage is called on.
   * @param dir Directory used for temporary/cache files.
   * @param requestUrl The part of the user's request, after EDStatic.baseUrl, before '?'.
   * @param userDapQuery An OPeNDAP DAP-style query string, still percentEncoded (shouldn't be
   *     null). e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
   * @param fileTypeName File type being requested (eg: .transparentPng)
   * @param expected The expected hash of the output of the saveAsImage call.
   * @throws Throwable
   */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest
  void testSaveAsImageVsExpected(
      EDDGrid eddGrid,
      String dir,
      String requestUrl,
      String userDapQuery,
      String fileTypeName,
      String expected)
      throws Throwable {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    OutputStreamSourceSimple osss = new OutputStreamSourceSimple(baos);
    String filename = dir + Math2.random(Integer.MAX_VALUE) + ".png";

    eddGrid.saveAsImage(
        0 /* language */,
        null /* loggedInAs */,
        requestUrl,
        userDapQuery,
        dir,
        filename,
        osss /* outputStreamSource */,
        fileTypeName);

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(baos.toByteArray());
      StringBuilder hexString = new StringBuilder();

      for (int i = 0; i < hash.length; i++) {
        String hex = Integer.toHexString(0xff & hash[i]);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
      }

      String results = hexString.toString();

      // String2.log(results);
      Test.ensureEqual(
          results.substring(0, expected.length()),
          expected,
          "\nresults=\n" + results.substring(0, Math.min(256, results.length())));
    } catch (Exception ex) {
      FileOutputStream fos = new FileOutputStream(filename);
      fos.write(baos.toByteArray());
      fos.flush();
      fos.close();
      // Test.displayInBrowser("file://" + filename);
      throw new RuntimeException(ex);
    }
  }

  /** Test the WCS server using erdBAssta5day. */
  @org.junit.jupiter.api.Test
  @TagThredds
  void testWcsBAssta() throws Throwable {
    String2.log("\n*** EDDGridFromNcFiles.testWcsBAssta()");
    EDDGrid eddGrid = (EDDGrid) EDDTestDataset.geterdBAssta5day();
    String wcsQuery, fileName, results, expected;
    java.io.StringWriter writer;
    ByteArrayOutputStream baos;
    OutputStreamSourceSimple osss;
    String loggedInAs = null;

    // 1.0.0 capabilities
    // try to validate with https://xmlvalidation.com/ (just an error in a schema)
    String2.log("\n+++ GetCapabilities 1.0.0");
    writer = new java.io.StringWriter();
    eddGrid.wcsGetCapabilities(0, loggedInAs, "1.0.0", writer);
    results = writer.toString();
    String2.log(results);

    // 1.0.0 DescribeCoverage
    // try to validate with https://xmlvalidation.com/
    String2.log("\n+++ DescribeCoverage 1.0.0");
    writer = new java.io.StringWriter();
    eddGrid.wcsDescribeCoverage(0, loggedInAs, "1.0.0", "sst", writer);
    results = writer.toString();
    String2.log(results);

    // test wcsQueryToDapQuery()
    String wcsQuery1 =
        "service=WCS&version=1.0.0&request=GetCoverage" + "&coverage=sst" + "&format=NetCDF3";
    String wcsQuery2 =
        "service=WCS&version=1.0.0&request=GetCoverage"
            + "&coverage=sst"
            + "&format=netcdf3"
            + "&time=2008-08-01T00:00:00Z"
            + "&bbox=220,20,250,50&width=10&height=10";
    String wcsQuery3 =
        "service=WCS&version=1.0.0&request=GetCoverage"
            + "&coverage=sst"
            + "&format=png"
            + "&time=2008-08-01T00:00:00Z"
            + "&bbox=220,20,250,50";
    HashMap<String, String> wcsQueryMap1 = EDD.userQueryHashMap(wcsQuery1, true);
    HashMap<String, String> wcsQueryMap2 = EDD.userQueryHashMap(wcsQuery2, true);
    HashMap<String, String> wcsQueryMap3 = EDD.userQueryHashMap(wcsQuery3, true);

    String dapQuery1[] = eddGrid.wcsQueryToDapQuery(0, wcsQueryMap1);
    String2.log("\nwcsQuery1=" + wcsQuery1 + "\n\ndapQuery1=" + dapQuery1[0]);
    Test.ensureEqual(
        dapQuery1[0], "sst[(last)][(0.0):1:(0.0)][(-75.0):1:(75.0)][(0.0):1:(360.0)]", "");

    String dapQuery2[] = eddGrid.wcsQueryToDapQuery(0, wcsQueryMap2);
    String2.log("\nwcsQuery2=" + wcsQuery2 + "\n\ndapQuery2=" + dapQuery2[0]);
    Test.ensureEqual(
        dapQuery2[0],
        "sst[(2008-08-01T00:00:00Z)][(0.0):1:(0.0)][(20):33:(50)][(220):33:(250)]",
        "");
    Test.ensureEqual(dapQuery2[1], ".nc", "");

    String dapQuery3[] = eddGrid.wcsQueryToDapQuery(0, wcsQueryMap3);
    String2.log("\nwcsQuery3=" + wcsQuery3 + "\n\ndapQuery3=" + dapQuery3[0]);
    Test.ensureEqual(
        dapQuery3[0], "sst[(2008-08-01T00:00:00Z)][(0.0):1:(0.0)][(20):1:(50)][(220):1:(250)]", "");
    Test.ensureEqual(dapQuery3[1], ".transparentPng", "");

    // ???write tests of invalid queries?

    // *** check netcdf response
    String2.log("\n+++ GetCoverage\n" + wcsQuery1);
    fileName = EDStatic.fullTestCacheDirectory + "testWcsBA_2.nc";
    String endOfRequest = "myEndOfRequest";
    eddGrid.wcsGetCoverage(
        0,
        "someIPAddress",
        loggedInAs,
        endOfRequest,
        wcsQuery2,
        new OutputStreamSourceSimple(new BufferedOutputStream(new FileOutputStream(fileName))));
    results = NcHelper.ncdump(fileName, "");
    String2.log(results);
    // expected = "zztop";
    // Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // *** check png response
    String2.log("\n+++ GetCoverage\n" + wcsQuery1);
    fileName = EDStatic.fullTestCacheDirectory + "testWcsBA_3.png";
    eddGrid.wcsGetCoverage(
        0,
        "someIPAddress",
        loggedInAs,
        endOfRequest,
        wcsQuery3,
        new OutputStreamSourceSimple(new BufferedOutputStream(new FileOutputStream(fileName))));
    // Test.displayInBrowser("file://" + fileName);

    /*
     * //*** observations for all stations and with BBOX (but just same 1 station)
     * //featureOfInterest=BBOX:<min_lon>,<min_lat>,<max_lon>,<max_lat>
     * String2.log("\n+++ GetObservations with BBOX (1 station)");
     * writer = new java.io.StringWriter();
     * String2.log("query: " + wcsQuery2);
     * baos = new ByteArrayOutputStream();
     * osss = new OutputStreamSourceSimple(baos);
     * eddGrid.respondToWcsQuery(wcsQuery2, null, osss);
     * results = baos.toString(File2.UTF_8);
     * String2.log(results);
     *
     * //*** observations for all stations and with BBOX (multiple stations)
     * //featureOfInterest=BBOX:<min_lon>,<min_lat>,<max_lon>,<max_lat>
     * String2.log("\n+++ GetObservations with BBOX (multiple stations)");
     * writer = new java.io.StringWriter();
     * String2.log("query: " + wcsQuery3);
     * baos = new ByteArrayOutputStream();
     * osss = new OutputStreamSourceSimple(baos);
     * eddGrid.respondToWcsQuery(wcsQuery3, null, osss);
     * results = baos.toString(File2.UTF_8);
     * String2.log(results);
     */
  }

  @org.junit.jupiter.api.Test
  void testEsriAscii() throws Throwable {
    EDDGrid gridDataset = (EDDGrid) EDDTestDataset.getetopo180();
    String tName;
    int language = 0;
    String tDir = EDStatic.fullTestCacheDirectory;

    // *** test getting graphs
    String2.log("\n****************** EDDGrid test get graphs\n");

    // test graph .png
    String query = "altitude%5B(-90.0):(-89.0)%5D%5B(-180.0):(-179.0)%5D";
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            query,
            tDir,
            gridDataset.className() + "_esriAscii",
            ".esriAscii");
    String results = File2.directReadFrom88591File(tDir + tName);
    String expected =
        "ncols 61\n"
            + //
            "nrows 61\n"
            + //
            "xllcenter -180.0\n"
            + //
            "yllcenter -90.0\n"
            + //
            "cellsize 0.016666666666666666\n"
            + //
            "nodata_value -9999999\n"
            + //
            "2990 2990 2990 2990 2990 2990 2990 2990 2990 2990 2990 2990 2990 2990 2990 2990 2990 2990 2990 2990 2989 2989 2989 2989 2989 2989 2989 2989 2989 2988 2988 2988 2988 2988 2988 2988 2988 2988 2988 2988 2988 2988 2988 2988 2988 2988 2988 2987 2987 2987 2987 2987 2987 2987 2987 2987 2987 2987 2987 2987 2987\n"
            + //
            "2987 2986 2986 2986 2986 2986 2986 2986 2986 2986 2986 2986 2986 2986 2986 2986 2986 2986 2986 2986 2986 2986 2986 2986 2986 2986 2986 2986 2986 2985 2985 2985 2985 2985 2985 2985 2984 2984 2984 2984 2984 2984 2984 2984 2984 2984 2984 2984 2984 2984 2984 2984 2984 2983 2983 2983 2983 2983 2983 2983 2983\n"
            + //
            "2984 2984 2984 2984 2983 2983 2983 2983 2983 2983 2983 2983 2983 2983 2983 2983 2983 2983 2983 2983 2982 2982 2982 2982 2982 2982 2982 2982 2982 2982 2982 2982 2981 2981 2981 2981 2981 2981 2981 2981 2981 2981 2981 2981 2981 2981 2981 2981 2980 2980 2980 2980 2980 2980 2980 2980 2980 2980 2980 2980 2980\n"
            + //
            "2980 2980 2980 2980 2980 2980 2980 2980 2979 2979 2979 2979 2979 2979 2979 2979 2979 2979 2979 2979 2978 2978 2978 2978 2978 2978 2978 2978 2978 2978 2978 2978 2978 2978 2978 2978 2978 2978 2977 2977 2977 2977 2977 2977 2977 2977 2977 2977 2977 2977 2977 2977 2977 2977 2976 2976 2976 2976 2976 2976 2976\n"
            + //
            "2976 2976 2976 2976 2976 2976 2976 2976 2975 2975 2975 2975 2975 2975 2975 2975 2975 2975 2975 2975 2975 2975 2975 2975 2974 2974 2974 2974 2974 2974 2974 2974 2974 2974 2974 2974 2974 2974 2974 2974 2973 2973 2973 2973 2973 2973 2973 2973 2973 2973 2973 2973 2973 2973 2973 2973 2973 2973 2973 2973 2973\n"
            + //
            "2972 2972 2972 2972 2972 2972 2972 2972 2972 2972 2972 2972 2972 2972 2972 2972 2971 2971 2971 2971 2971 2971 2971 2971 2971 2971 2971 2971 2971 2971 2971 2971 2970 2970 2970 2970 2970 2970 2970 2970 2970 2970 2969 2969 2969 2969 2969 2969 2969 2969 2969 2969 2969 2969 2969 2969 2969 2969 2968 2968 2968\n"
            + //
            "2968 2968 2968 2968 2968 2968 2968 2968 2968 2968 2968 2968 2968 2968 2968 2968 2967 2967 2967 2967 2967 2967 2967 2967 2967 2967 2967 2967 2967 2967 2967 2966 2966 2966 2966 2966 2966 2966 2966 2966 2966 2966 2966 2966 2966 2966 2966 2966 2965 2965 2965 2965 2965 2965 2965 2965 2965 2965 2965 2965 2965\n"
            + //
            "2964 2964 2964 2964 2964 2964 2964 2964 2964 2964 2964 2964 2963 2963 2963 2963 2963 2963 2963 2963 2963 2963 2963 2963 2963 2963 2963 2963 2962 2962 2962 2962 2962 2962 2962 2962 2962 2962 2962 2962 2961 2961 2961 2961 2961 2961 2961 2961 2961 2961 2961 2961 2961 2961 2961 2961 2960 2960 2960 2960 2960\n"
            + //
            "2960 2960 2960 2960 2960 2960 2960 2959 2959 2959 2959 2959 2959 2959 2959 2959 2959 2959 2959 2959 2959 2959 2959 2958 2958 2958 2958 2958 2958 2958 2958 2958 2958 2958 2958 2958 2957 2957 2957 2957 2957 2957 2957 2957 2957 2957 2957 2957 2957 2957 2957 2957 2956 2956 2956 2956 2956 2956 2956 2956 2956\n"
            + //
            "2955 2955 2955 2955 2955 2955 2955 2955 2955 2955 2955 2955 2954 2954 2954 2954 2954 2954 2954 2954 2954 2954 2954 2954 2954 2954 2954 2954 2954 2954 2954 2954 2953 2953 2953 2953 2953 2953 2953 2953 2953 2952 2952 2952 2952 2952 2952 2952 2952 2952 2952 2952 2952 2952 2952 2952 2952 2952 2952 2952 2951\n"
            + //
            "2951 2951 2951 2950 2950 2950 2950 2950 2950 2950 2950 2950 2950 2950 2950 2950 2950 2950 2950 2950 2949 2949 2949 2949 2949 2949 2949 2949 2949 2949 2949 2949 2949 2949 2948 2948 2948 2948 2948 2948 2948 2948 2948 2948 2948 2948 2948 2948 2948 2948 2947 2947 2947 2947 2947 2947 2947 2947 2947 2947 2947\n"
            + //
            "2946 2946 2946 2946 2946 2946 2946 2946 2946 2946 2946 2946 2946 2946 2946 2945 2945 2945 2945 2945 2945 2945 2945 2945 2945 2945 2945 2945 2945 2944 2944 2944 2944 2944 2944 2944 2944 2944 2944 2944 2944 2944 2943 2943 2943 2943 2943 2943 2943 2943 2943 2943 2943 2943 2943 2943 2943 2943 2942 2942 2942\n"
            + //
            "2941 2941 2941 2941 2941 2941 2940 2940 2940 2940 2940 2940 2940 2940 2940 2940 2940 2940 2940 2940 2940 2940 2940 2939 2939 2939 2939 2939 2939 2939 2939 2939 2939 2939 2939 2938 2938 2938 2938 2938 2938 2938 2938 2938 2938 2938 2938 2938 2938 2938 2938 2937 2937 2937 2937 2937 2937 2937 2937 2937 2937\n"
            + //
            "2935 2935 2935 2935 2935 2935 2935 2935 2935 2935 2935 2935 2935 2935 2934 2934 2934 2934 2934 2934 2934 2934 2934 2934 2934 2934 2934 2934 2934 2934 2933 2933 2933 2933 2933 2933 2933 2933 2933 2933 2933 2933 2933 2932 2932 2932 2932 2932 2932 2932 2932 2932 2932 2932 2932 2932 2932 2932 2932 2932 2931\n"
            + //
            "2931 2931 2931 2931 2931 2931 2931 2930 2930 2930 2930 2930 2930 2930 2930 2930 2930 2930 2930 2930 2929 2929 2929 2929 2929 2929 2929 2929 2929 2929 2929 2929 2929 2929 2929 2929 2928 2928 2928 2928 2928 2928 2928 2928 2928 2928 2928 2928 2928 2928 2928 2928 2927 2927 2927 2927 2927 2927 2927 2927 2927\n"
            + //
            "2925 2925 2925 2925 2925 2925 2925 2925 2925 2925 2925 2924 2924 2924 2924 2924 2924 2924 2924 2924 2924 2924 2924 2924 2924 2924 2924 2924 2924 2924 2924 2923 2923 2923 2923 2923 2923 2923 2923 2923 2923 2923 2923 2923 2923 2922 2922 2922 2922 2922 2922 2922 2922 2922 2922 2922 2922 2922 2922 2922 2922\n"
            + //
            "2921 2921 2921 2920 2920 2920 2920 2920 2920 2920 2920 2920 2920 2920 2920 2920 2920 2920 2920 2919 2919 2919 2919 2919 2919 2919 2919 2919 2919 2919 2919 2919 2919 2919 2919 2919 2919 2918 2918 2918 2918 2918 2918 2918 2918 2918 2918 2918 2918 2918 2918 2918 2918 2917 2917 2917 2917 2917 2917 2917 2917\n"
            + //
            "2916 2916 2916 2916 2916 2916 2916 2916 2916 2916 2916 2916 2916 2916 2915 2915 2915 2915 2915 2915 2915 2915 2915 2915 2915 2915 2915 2915 2915 2915 2915 2914 2914 2914 2914 2914 2914 2914 2914 2914 2914 2914 2914 2914 2914 2914 2914 2914 2914 2913 2913 2913 2913 2913 2913 2913 2913 2913 2913 2912 2912\n"
            + //
            "2912 2912 2912 2912 2912 2912 2911 2911 2911 2911 2911 2911 2911 2911 2911 2911 2911 2911 2911 2910 2910 2910 2910 2910 2910 2910 2910 2910 2910 2910 2910 2910 2910 2910 2910 2910 2910 2910 2910 2910 2910 2909 2909 2909 2909 2909 2909 2909 2909 2909 2909 2909 2909 2909 2908 2908 2908 2908 2908 2908 2908\n"
            + //
            "2907 2907 2907 2907 2907 2907 2907 2907 2907 2907 2907 2906 2906 2906 2906 2906 2906 2906 2906 2906 2906 2906 2906 2906 2906 2906 2906 2905 2905 2905 2905 2905 2905 2905 2905 2905 2905 2905 2905 2905 2905 2905 2905 2905 2905 2905 2905 2904 2904 2904 2904 2904 2904 2904 2904 2904 2904 2904 2904 2904 2904\n"
            + //
            "2904 2904 2904 2904 2903 2903 2903 2903 2903 2903 2903 2903 2903 2903 2903 2903 2903 2903 2902 2902 2902 2902 2902 2902 2902 2902 2902 2901 2901 2901 2901 2901 2901 2901 2901 2901 2901 2901 2901 2901 2901 2901 2901 2900 2900 2900 2900 2900 2900 2900 2900 2900 2900 2900 2900 2900 2900 2900 2900 2899 2899\n"
            + //
            "2900 2900 2900 2900 2900 2900 2899 2899 2899 2899 2899 2899 2899 2899 2899 2899 2899 2899 2898 2898 2898 2898 2898 2898 2898 2898 2898 2898 2898 2898 2898 2898 2898 2898 2898 2898 2898 2898 2898 2898 2897 2897 2897 2897 2897 2897 2897 2897 2897 2897 2897 2896 2896 2896 2896 2896 2896 2896 2896 2896 2896\n"
            + //
            "2896 2896 2896 2896 2896 2896 2895 2895 2895 2895 2895 2895 2895 2895 2895 2895 2895 2895 2895 2895 2895 2895 2895 2894 2894 2894 2894 2894 2894 2894 2894 2894 2894 2894 2894 2894 2894 2894 2894 2894 2894 2894 2893 2893 2893 2893 2893 2893 2893 2893 2893 2893 2893 2893 2893 2893 2893 2892 2892 2892 2892\n"
            + //
            "2893 2893 2893 2893 2893 2893 2893 2893 2893 2893 2892 2892 2892 2892 2892 2892 2892 2892 2892 2892 2892 2892 2892 2892 2892 2892 2891 2891 2891 2891 2891 2891 2891 2891 2891 2891 2891 2891 2891 2891 2891 2891 2891 2891 2891 2891 2891 2890 2890 2890 2890 2890 2890 2890 2890 2890 2890 2890 2890 2889 2889\n"
            + //
            "2890 2890 2890 2890 2890 2890 2890 2890 2890 2890 2889 2889 2889 2889 2889 2889 2889 2889 2889 2889 2889 2888 2888 2888 2888 2888 2888 2888 2888 2888 2888 2888 2888 2888 2888 2888 2888 2888 2888 2888 2888 2888 2888 2888 2887 2887 2887 2887 2887 2887 2887 2887 2887 2887 2886 2886 2886 2886 2886 2886 2886\n"
            + //
            "2887 2887 2887 2887 2887 2886 2886 2886 2886 2886 2886 2886 2886 2886 2886 2886 2886 2886 2886 2886 2886 2885 2885 2885 2885 2885 2885 2885 2885 2885 2885 2885 2885 2885 2885 2885 2885 2885 2885 2885 2884 2884 2884 2884 2884 2884 2884 2884 2884 2884 2884 2884 2884 2884 2883 2883 2883 2883 2883 2883 2883\n"
            + //
            "2885 2885 2885 2885 2885 2884 2884 2884 2884 2884 2884 2884 2884 2884 2884 2884 2884 2884 2884 2884 2884 2883 2883 2883 2883 2883 2883 2883 2883 2883 2883 2883 2883 2883 2883 2883 2883 2883 2883 2883 2883 2883 2883 2883 2883 2883 2882 2882 2882 2882 2882 2882 2882 2882 2882 2882 2882 2882 2882 2882 2882\n"
            + //
            "2882 2882 2882 2882 2882 2882 2882 2882 2882 2882 2882 2882 2882 2882 2882 2882 2882 2882 2882 2882 2882 2881 2881 2881 2881 2881 2881 2881 2881 2881 2880 2880 2881 2881 2881 2881 2881 2881 2881 2880 2880 2880 2880 2880 2880 2880 2880 2880 2880 2880 2880 2880 2880 2880 2880 2880 2880 2880 2880 2880 2880\n"
            + //
            "2879 2879 2879 2879 2879 2879 2879 2879 2879 2879 2879 2879 2879 2879 2879 2879 2879 2879 2879 2879 2879 2879 2878 2878 2878 2878 2878 2878 2878 2878 2878 2878 2878 2878 2878 2878 2878 2878 2878 2878 2878 2878 2878 2878 2878 2878 2878 2878 2877 2877 2877 2877 2877 2877 2877 2877 2877 2877 2877 2877 2877\n"
            + //
            "2876 2876 2876 2876 2876 2876 2876 2876 2876 2876 2876 2876 2876 2876 2876 2876 2876 2876 2876 2876 2876 2876 2876 2876 2876 2876 2876 2876 2876 2876 2876 2876 2875 2875 2875 2875 2875 2875 2875 2875 2875 2875 2875 2875 2875 2875 2875 2875 2875 2875 2875 2875 2875 2875 2875 2875 2875 2875 2875 2875 2874\n"
            + //
            "2873 2873 2873 2873 2873 2873 2873 2873 2873 2873 2873 2873 2873 2873 2873 2873 2873 2873 2873 2872 2872 2872 2872 2872 2872 2872 2872 2872 2872 2872 2872 2872 2872 2872 2872 2872 2872 2872 2872 2872 2872 2872 2872 2872 2872 2872 2872 2872 2872 2871 2871 2871 2871 2871 2871 2871 2871 2871 2871 2871 2871\n"
            + //
            "2870 2869 2869 2869 2869 2869 2869 2869 2869 2869 2869 2869 2869 2869 2869 2869 2869 2869 2869 2869 2869 2869 2869 2869 2869 2869 2869 2869 2869 2869 2869 2869 2868 2868 2868 2868 2868 2868 2868 2868 2868 2868 2868 2868 2868 2868 2868 2868 2868 2868 2868 2868 2868 2868 2868 2868 2868 2868 2868 2868 2868\n"
            + //
            "2866 2866 2866 2866 2866 2866 2866 2866 2866 2866 2866 2866 2866 2866 2866 2866 2866 2866 2866 2866 2866 2866 2866 2866 2866 2866 2866 2866 2866 2866 2865 2865 2865 2865 2865 2865 2865 2865 2865 2865 2865 2865 2865 2865 2865 2865 2865 2865 2865 2865 2865 2865 2865 2864 2864 2864 2864 2864 2864 2864 2864\n"
            + //
            "2862 2862 2862 2862 2862 2862 2862 2862 2862 2862 2862 2862 2862 2862 2862 2862 2862 2862 2862 2862 2862 2862 2862 2862 2862 2862 2862 2862 2862 2862 2861 2861 2861 2861 2861 2861 2861 2861 2861 2861 2861 2861 2861 2861 2861 2861 2861 2861 2861 2861 2861 2861 2861 2861 2861 2861 2861 2861 2861 2861 2861\n"
            + //
            "2858 2858 2858 2858 2858 2858 2858 2858 2858 2858 2858 2858 2858 2858 2858 2858 2858 2858 2857 2857 2857 2857 2857 2857 2857 2857 2857 2857 2857 2857 2857 2857 2857 2857 2857 2857 2857 2857 2857 2857 2857 2857 2857 2857 2857 2857 2856 2856 2856 2856 2856 2856 2856 2856 2856 2856 2856 2856 2856 2856 2856\n"
            + //
            "2853 2853 2853 2853 2853 2853 2853 2853 2853 2853 2853 2853 2853 2853 2853 2853 2853 2853 2853 2853 2853 2853 2853 2853 2853 2853 2853 2853 2853 2853 2853 2853 2852 2852 2852 2852 2852 2852 2852 2852 2852 2852 2852 2852 2852 2852 2852 2852 2852 2852 2852 2852 2852 2852 2852 2852 2852 2852 2852 2852 2852\n"
            + //
            "2848 2848 2848 2848 2848 2848 2848 2848 2848 2848 2848 2848 2848 2848 2848 2848 2848 2848 2847 2847 2847 2847 2847 2847 2847 2847 2847 2847 2847 2847 2847 2847 2847 2847 2847 2847 2847 2847 2847 2847 2847 2847 2847 2847 2847 2847 2847 2847 2847 2846 2846 2846 2846 2846 2846 2846 2846 2846 2846 2846 2846\n"
            + //
            "2842 2842 2842 2842 2842 2842 2842 2842 2842 2842 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2841 2840 2840 2840 2840 2840 2840 2840 2840 2840 2840 2840\n"
            + //
            "2836 2836 2836 2836 2836 2836 2836 2836 2836 2836 2836 2836 2836 2836 2836 2836 2836 2835 2835 2835 2835 2835 2835 2835 2835 2835 2835 2835 2835 2835 2835 2835 2835 2835 2835 2835 2835 2835 2835 2835 2835 2835 2835 2835 2835 2835 2835 2835 2835 2835 2835 2835 2834 2834 2834 2834 2834 2834 2834 2834 2834\n"
            + //
            "2829 2829 2829 2829 2829 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2828 2827 2827 2827 2827 2827 2827 2827 2827\n"
            + //
            "2821 2821 2821 2821 2821 2821 2821 2821 2821 2821 2821 2821 2821 2821 2821 2821 2821 2821 2821 2821 2821 2821 2821 2821 2821 2821 2821 2821 2820 2820 2820 2820 2820 2820 2820 2820 2820 2820 2820 2820 2820 2820 2820 2820 2820 2820 2820 2820 2820 2820 2820 2820 2820 2820 2820 2820 2820 2820 2820 2820 2820\n"
            + //
            "2814 2814 2814 2814 2814 2814 2814 2814 2814 2814 2814 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813 2813\n"
            + //
            "2806 2806 2806 2806 2806 2806 2806 2806 2806 2806 2806 2806 2806 2806 2806 2806 2806 2806 2806 2806 2806 2806 2806 2806 2806 2806 2806 2806 2806 2806 2806 2806 2805 2805 2805 2805 2805 2805 2805 2805 2805 2805 2805 2805 2805 2805 2805 2805 2805 2805 2805 2805 2805 2805 2805 2805 2805 2805 2805 2805 2805\n"
            + //
            "2798 2798 2798 2798 2798 2798 2798 2798 2798 2798 2798 2798 2798 2798 2798 2798 2798 2798 2798 2798 2798 2798 2798 2798 2798 2798 2798 2798 2798 2798 2798 2797 2797 2797 2797 2797 2797 2797 2797 2797 2797 2797 2797 2797 2797 2797 2797 2797 2797 2797 2797 2797 2797 2797 2797 2797 2797 2797 2797 2797 2797\n"
            + //
            "2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791 2791\n"
            + //
            "2784 2784 2784 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783 2783\n"
            + //
            "2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2776 2775 2775 2775 2775 2775 2775 2775\n"
            + //
            "2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2770 2769 2769 2769 2769 2769 2769\n"
            + //
            "2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2764 2763 2763 2763 2763 2763 2763 2763 2763 2763 2763\n"
            + //
            "2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758 2758\n"
            + //
            "2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754 2754\n"
            + //
            "2750 2751 2751 2751 2751 2751 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750 2750\n"
            + //
            "2748 2748 2748 2748 2748 2748 2748 2748 2748 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747 2747\n"
            + //
            "2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746 2746\n"
            + //
            "2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744\n"
            + //
            "2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744\n"
            + //
            "2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2743 2743 2743 2743 2743 2743 2743 2743 2743 2743 2743 2743 2743 2743 2743 2743 2743 2743 2743 2743 2744 2744 2744 2743 2743 2743 2743 2743 2743 2743 2743 2743 2743 2743 2743 2743 2743 2743 2743 2743 2743 2743 2743 2743 2743 2743 2743 2743\n"
            + //
            "2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744\n"
            + //
            "2744 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2745 2745 2745 2745 2745 2745 2744 2745 2745 2745 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744\n"
            + //
            "2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744 2744\n"
            + //
            "2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745 2745\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }
}
