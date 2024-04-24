package gov.noaa.pfel.erddap.dataset;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.text.MessageFormat;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeAll;

import com.cohort.util.Image2Tests;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.erddap.util.EDStatic;
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
   * Test saveAsImage, specifically to make sure a transparent png that's
   * partially outside of the range of the dataset still returns the image for
   * the part that is within range.
   */
  @org.junit.jupiter.api.Test
  @TagThredds
  @TagImageComparison
  void testSaveAsImage() throws Throwable {
    // String2.log("\n*** EDDGrid.testSaveAsImage()");
    EDDGrid eddGrid = (EDDGrid) EDDTestDataset.geterdMHchla8day();
    // EDDGrid eddGrid = (EDDGrid) EDDGrid.oneFromDatasetsXml(null,
    // "erdMHchla8day");
    int language = 0;
    String dir = Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR);
    // String requestUrl = "/erddap/griddap/erdMHchla8day.transparentPng";
    String userDapQueryTemplate = "MWchla%5B(2022-01-16T12:00:00Z):1:(2022-01-16T12:00:00Z)%5D%5B(0.0):1:(0.0)%5D%5B({0,number,#.##########}):1:({1,number,#.##########})%5D%5B({2,number,#.##########}):1:({3,number,#.##########})%5D";
    String baseName, tName;

    String expectedHashForInvalidInput = "9b750d93bf5cc5f356e7b159facec812dc09c20050d38d6362280def580bc62e";

    // Make fully valid image
    baseName = "EDDGrid_testSaveAsImage_fullyValid";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null,
        MessageFormat.format(userDapQueryTemplate, 30, 40, 210, 220), // #'s are minLat, maxLat,
                                                                      // minLon, maxLon
        dir, baseName, ".transparentPng");
    Image2Tests.testImagesIdentical(
        tName,
        baseName + ".png",
        baseName + "_diff.png");

    // Invalid min y.
    baseName = "EDDGrid_testSaveAsImage_invalidMinY";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null,
        MessageFormat.format(userDapQueryTemplate, -100, 40, 210, 220), // #'s are minLat,
                                                                        // maxLat, minLon,
                                                                        // maxLon
        dir, baseName, ".transparentPng");
    Image2Tests.testImagesIdentical(
        tName,
        baseName + ".png",
        baseName + "_diff.png");

    // 2020-08-03 For tests below, some generated images have data, some don't,
    // but results seem inconsistent.
    // The images in erddapTest/images are old and I'm not sure appropriate.
    // I'm not sure what they should be. Leave this for Chris John.

    // Invalid max y.
    baseName = "EDDGrid_testSaveAsImage_invalidMaxY";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null,
        MessageFormat.format(userDapQueryTemplate, 30, 100, 210, 220), // #'s are minLat,
                                                                       // maxLat, minLon, maxLon
        dir, baseName, ".transparentPng");
    Image2Tests.testImagesIdentical(
        tName,
        baseName + ".png",
        baseName + "_diff.png");

    // All invalid.
    baseName = "EDDGrid_testSaveAsImage_allInvalid";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null,
        MessageFormat.format(userDapQueryTemplate, -100, 100, -200, 370), // #'s are minLat,
                                                                          // maxLat, minLon,
                                                                          // maxLon
        dir, baseName, ".transparentPng");
    Image2Tests.testImagesIdentical(
        tName,
        baseName + ".png",
        baseName + "_diff.png");

    // Out of range min x.
    baseName = "EDDGrid_testSaveAsImage_OORMinX";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null,
        MessageFormat.format(userDapQueryTemplate, 30, 40, 200, 210), // #'s are minLat, maxLat,
                                                                      // minLon, maxLon
        dir, baseName, ".transparentPng");
    Image2Tests.testImagesIdentical(
        tName,
        baseName + ".png",
        baseName + "_diff.png");

    // Out of range max x.
    baseName = "EDDGrid_testSaveAsImage_OORMaxX";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null,
        MessageFormat.format(userDapQueryTemplate, 30, 40, 250, 260), // #'s are minLat, maxLat,
                                                                      // minLon, maxLon
        dir, baseName, ".transparentPng");
    Image2Tests.testImagesIdentical(
        tName,
        baseName + ".png",
        baseName + "_diff.png");

    // Out of range min y.
    baseName = "EDDGrid_testSaveAsImage_OORMinY";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null,
        MessageFormat.format(userDapQueryTemplate, 20, 30, 210, 220), // #'s are minLat, maxLat,
                                                                      // minLon, maxLon
        dir, baseName, ".transparentPng");
    Image2Tests.testImagesIdentical(
        tName,
        baseName + ".png",
        baseName + "_diff.png");

    // Out of range max y.
    baseName = "EDDGrid_testSaveAsImage_OORMaxY";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null,
        MessageFormat.format(userDapQueryTemplate, 50, 60, 210, 220), // #'s are minLat, maxLat,
                                                                      // minLon, maxLon
        dir, baseName, ".transparentPng");
    Image2Tests.testImagesIdentical(
        tName,
        baseName + ".png",
        baseName + "_diff.png");

    // Fully out of range min x.
    baseName = "EDDGrid_testSaveAsImage_FOORMinX";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null,
        MessageFormat.format(userDapQueryTemplate, 30, 40, 190, 200), // #'s are minLat, maxLat,
                                                                      // minLon, maxLon
        dir, baseName, ".transparentPng");
    Image2Tests.testImagesIdentical(
        tName,
        baseName + ".png",
        baseName + "_diff.png");

    // Fully out of range max x.
    baseName = "EDDGrid_testSaveAsImage_FOORMaxX";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null,
        MessageFormat.format(userDapQueryTemplate, 30, 40, 260, 270), // #'s are minLat, maxLat,
                                                                      // minLon, maxLon
        dir, baseName, ".transparentPng");
    Image2Tests.testImagesIdentical(
        tName,
        baseName + ".png",
        baseName + "_diff.png");

    // Fully out of range min y.
    baseName = "EDDGrid_testSaveAsImage_FOORMinY";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null,
        MessageFormat.format(userDapQueryTemplate, 10, 20, 210, 220), // #'s are minLat, maxLat,
                                                                      // minLon, maxLon
        dir, baseName, ".transparentPng");
    Image2Tests.testImagesIdentical(
        tName,
        baseName + ".png",
        baseName + "_diff.png");

    // Fully out of range max y.
    baseName = "EDDGrid_testSaveAsImage_FOORMaxY";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null,
        MessageFormat.format(userDapQueryTemplate, 60, 70, 210, 220), // #'s are minLat, maxLat,
                                                                      // minLon, maxLon
        dir, baseName, ".transparentPng");
    Image2Tests.testImagesIdentical(
        tName,
        baseName + ".png",
        baseName + "_diff.png");

  }

  /**
   * Tests input for saveAsImage against the provided output. Specifically the
   * output is provided as a hash (sha-256) of the output bytes.
   * 
   * @param eddGrid      EDDGrid that saveAsImage is called on.
   * @param dir          Directory used for temporary/cache files.
   * @param requestUrl   The part of the user's request, after
   *                     EDStatic.baseUrl, before '?'.
   * @param userDapQuery An OPeNDAP DAP-style query string, still
   *                     percentEncoded (shouldn't be null). e.g.,
   *                     ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
   * @param fileTypeName File type being requested (eg: .transparentPng)
   * @param expected     The expected hash of the output of the saveAsImage
   *                     call.
   * @throws Throwable
   */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest
  void testSaveAsImageVsExpected(EDDGrid eddGrid, String dir,
      String requestUrl, String userDapQuery, String fileTypeName,
      String expected) throws Throwable {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    OutputStreamSourceSimple osss = new OutputStreamSourceSimple(baos);
    String filename = dir + Math2.random(Integer.MAX_VALUE) + ".png";

    eddGrid.saveAsImage(0 /* language */, null /* loggedInAs */, requestUrl,
        userDapQuery, dir, filename,
        osss /* outputStreamSource */, fileTypeName);

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(baos.toByteArray());
      StringBuilder hexString = new StringBuilder();

      for (int i = 0; i < hash.length; i++) {
        String hex = Integer.toHexString(0xff & hash[i]);
        if (hex.length() == 1)
          hexString.append('0');
        hexString.append(hex);
      }

      String results = hexString.toString();

      // String2.log(results);
      Test.ensureEqual(results.substring(0, expected.length()), expected,
          "\nresults=\n" + results.substring(0,
              Math.min(256, results.length())));
    } catch (Exception ex) {
      FileOutputStream fos = new FileOutputStream(filename);
      fos.write(baos.toByteArray());
      fos.flush();
      fos.close();
      Test.displayInBrowser("file://" + filename);
      throw new RuntimeException(ex);
    }
  }

  /**
   * Test the WCS server using erdBAssta5day.
   */
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
    String wcsQuery1 = "service=WCS&version=1.0.0&request=GetCoverage" +
        "&coverage=sst" +
        "&format=NetCDF3";
    String wcsQuery2 = "service=WCS&version=1.0.0&request=GetCoverage" +
        "&coverage=sst" +
        "&format=netcdf3" +
        "&time=2008-08-01T00:00:00Z" +
        "&bbox=220,20,250,50&width=10&height=10";
    String wcsQuery3 = "service=WCS&version=1.0.0&request=GetCoverage" +
        "&coverage=sst" +
        "&format=png" +
        "&time=2008-08-01T00:00:00Z" +
        "&bbox=220,20,250,50";
    HashMap<String, String> wcsQueryMap1 = EDD.userQueryHashMap(wcsQuery1, true);
    HashMap<String, String> wcsQueryMap2 = EDD.userQueryHashMap(wcsQuery2, true);
    HashMap<String, String> wcsQueryMap3 = EDD.userQueryHashMap(wcsQuery3, true);

    String dapQuery1[] = eddGrid.wcsQueryToDapQuery(0, wcsQueryMap1);
    String2.log("\nwcsQuery1=" + wcsQuery1 + "\n\ndapQuery1=" + dapQuery1[0]);
    Test.ensureEqual(dapQuery1[0],
        "sst[(last)][(0.0):1:(0.0)][(-75.0):1:(75.0)][(0.0):1:(360.0)]", "");

    String dapQuery2[] = eddGrid.wcsQueryToDapQuery(0, wcsQueryMap2);
    String2.log("\nwcsQuery2=" + wcsQuery2 + "\n\ndapQuery2=" + dapQuery2[0]);
    Test.ensureEqual(dapQuery2[0],
        "sst[(2008-08-01T00:00:00Z)][(0.0):1:(0.0)][(20):33:(50)][(220):33:(250)]",
        "");
    Test.ensureEqual(dapQuery2[1], ".nc", "");

    String dapQuery3[] = eddGrid.wcsQueryToDapQuery(0, wcsQueryMap3);
    String2.log("\nwcsQuery3=" + wcsQuery3 + "\n\ndapQuery3=" + dapQuery3[0]);
    Test.ensureEqual(dapQuery3[0],
        "sst[(2008-08-01T00:00:00Z)][(0.0):1:(0.0)][(20):1:(50)][(220):1:(250)]",
        "");
    Test.ensureEqual(dapQuery3[1], ".transparentPng", "");

    // ???write tests of invalid queries?

    // *** check netcdf response
    String2.log("\n+++ GetCoverage\n" + wcsQuery1);
    fileName = EDStatic.fullTestCacheDirectory + "testWcsBA_2.nc";
    String endOfRequest = "myEndOfRequest";
    eddGrid.wcsGetCoverage(0, "someIPAddress", loggedInAs, endOfRequest, wcsQuery2,
        new OutputStreamSourceSimple(new BufferedOutputStream(new FileOutputStream(fileName))));
    results = NcHelper.ncdump(fileName, "");
    String2.log(results);
    // expected = "zztop";
    // Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // *** check png response
    String2.log("\n+++ GetCoverage\n" + wcsQuery1);
    fileName = EDStatic.fullTestCacheDirectory + "testWcsBA_3.png";
    eddGrid.wcsGetCoverage(0, "someIPAddress", loggedInAs, endOfRequest, wcsQuery3,
        new OutputStreamSourceSimple(new BufferedOutputStream(new FileOutputStream(fileName))));
    Test.displayInBrowser("file://" + fileName);

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

}
