package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.util.EDStatic;
import tags.TagExternalERDDAP;
import tags.TagLocalERDDAP;
import testDataset.EDDTestDataset;

class EDDGridFromErddapTests {

  @org.junit.jupiter.api.Test
  @TagExternalERDDAP
  void testDataVarOrder() throws Throwable {
    String2.log("\n*** EDDGridFromErddap.testDataVarOrder()");
    EDDGrid eddGrid = (EDDGrid) EDDTestDataset.gettestDataVarOrder();
    String results = String2.toCSSVString(eddGrid.dataVariableDestinationNames());
    String expected = "SST, mask, analysis_error";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);
  }

  /**
   * This tests dealing with remote not having ioos_category, but local requiring
   * it.
   */
  @org.junit.jupiter.api.Test
  void testGridNoIoosCat() throws Throwable {
    String2.log("\n*** EDDGridFromErddap.testGridNoIoosCat");

    // not active because no test dataset
    // this failed because trajectory didn't have ioos_category
    // EDDGrid edd = (EDDGrid)oneFromDatasetsXml(null, "testGridNoIoosCat");

  }

  /**
   * This tests the /files/ "files" system.
   * This requires nceiPH53sstn1day and testGridFromErddap in the local ERDDAP.
   *
   * EDDGridFromNcFiles.testFiles() has more tests than any other testFiles().
   */
  @org.junit.jupiter.api.Test
  @TagLocalERDDAP
  void testFiles() throws Throwable {

    String2.log("\n*** EDDGridFromErddap.testFiles()\n");
    String tDir = EDStatic.fullTestCacheDirectory;
    String dapQuery, tName, start, query, results, expected;
    int po;

    try {
      // get /files/.csv
      results = String2.annotatedString(SSR.getUrlResponseStringNewline(
          "http://localhost:8080/cwexperimental/files/.csv"));
      Test.ensureTrue(results.indexOf("Name,Last modified,Size,Description[10]") == 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(
          "nceiPH53sstn1day/,NaN,NaN,\"AVHRR Pathfinder Version 5.3 L3-Collated (L3C) SST, Global, 0.0417\\u00b0, 1981-present, Nighttime (1 Day Composite)\"[10]") > 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf(
          "testGridFromErddap/,NaN,NaN,\"AVHRR Pathfinder Version 5.3 L3-Collated (L3C) SST, Global, 0.0417\\u00b0, 1981-present, Nighttime (1 Day Composite)\"[10]") > 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf("documentation.html,") > 0, "results=\n" + results);

      // get /files/datasetID/.csv
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:8080/cwexperimental/files/testGridFromErddap/.csv");
      expected = "Name,Last modified,Size,Description\n" +
          "1981/,NaN,NaN,\n" +
          "1994/,NaN,NaN,\n" +
          "2020/,NaN,NaN,\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // get /files/datasetID/
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:8080/cwexperimental/files/testGridFromErddap/");
      Test.ensureTrue(results.indexOf("1981&#x2f;") > 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("1981/") > 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("1994&#x2f;") > 0, "results=\n" + results);

      // get /files/datasetID //missing trailing slash will be redirected
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:8080/cwexperimental/files/testGridFromErddap");
      Test.ensureTrue(results.indexOf("1981&#x2f;") > 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("1981/") > 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("1994&#x2f;") > 0, "results=\n" + results);

      // get /files/datasetID/subdir/.csv
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:8080/cwexperimental/files/testGridFromErddap/1994/.csv");
      expected = "Name,Last modified,Size,Description\n" +
          "data/,NaN,NaN,\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // get /files/datasetID/subdir/subdir.csv
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:8080/cwexperimental/files/testGridFromErddap/1994/data/.csv");
      expected = "Name,Last modified,Size,Description\n" +
          "19940913000030-NCEI-L3C_GHRSST-SSTskin-AVHRR_Pathfinder-PFV5.3_NOAA09_G_1994256_night-v02.0-fv01.0.nc,1471330800000,12484412,\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // download a file in root -- none available

      // download a file in subdir
      results = String2.annotatedString(SSR.getUrlResponseStringNewline(
          "http://localhost:8080/cwexperimental/files/testGridFromErddap/1994/data/" +
              "19940913000030-NCEI-L3C_GHRSST-SSTskin-AVHRR_Pathfinder-PFV5.3_NOAA09_G_1994256_night-v02.0-fv01.0.nc")
          .substring(0, 50));
      expected = "[137]HDF[10]\n" +
          "[26][10]\n" +
          "[2][8][8][0][0][0][0][0][0][0][0][0][255][255][255][255][255][255][255][255]<[127][190][0][0][0][0][0]0[0][0][0][0][0][0][0][199](*yOHD[end]";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // query with // at start fails
      try {
        results = SSR.getUrlResponseStringNewline(
            "http://localhost:8080/cwexperimental/files//.csv");
      } catch (Exception e) {
        results = e.toString();
      }
      expected = "java.io.IOException: HTTP status code=400 for URL: http://localhost:8080/cwexperimental/files//.csv\n"
          +
          "(Error {\n" +
          "    code=400;\n" +
          "    message=\"Bad Request: Query error: // is not allowed!\";\n" +
          "})";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // query with // later fails
      try {
        results = SSR.getUrlResponseStringNewline(
            "http://localhost:8080/cwexperimental/files/testGridFromErddap//.csv");
      } catch (Exception e) {
        results = e.toString();
      }
      expected = "java.io.IOException: HTTP status code=400 for URL: http://localhost:8080/cwexperimental/files/testGridFromErddap//.csv\n"
          +
          "(Error {\n" +
          "    code=400;\n" +
          "    message=\"Bad Request: Query error: // is not allowed!\";\n" +
          "})";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // query with /../ fails
      try {
        results = SSR.getUrlResponseStringNewline(
            "http://localhost:8080/cwexperimental/files/testGridFromErddap/../");
      } catch (Exception e) {
        results = e.toString();
      }
      expected = "java.io.IOException: HTTP status code=400 for URL: http://localhost:8080/cwexperimental/files/testGridFromErddap/../\n"
          +
          "(Error {\n" +
          "    code=400;\n" +
          "    message=\"Bad Request: Query error: /../ is not allowed!\";\n" +
          "})";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // try to download a non-existent dataset
      try {
        results = SSR.getUrlResponseStringNewline(
            "http://localhost:8080/cwexperimental/files/gibberish/");
      } catch (Exception e) {
        results = e.toString();
      }
      expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/gibberish/\n"
          +
          "(Error {\n" +
          "    code=404;\n" +
          "    message=\"Not Found: Currently unknown datasetID=gibberish\";\n" +
          "})";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // try to download a non-existent datasetID
      try {
        results = SSR.getUrlResponseStringNewline(
            "http://localhost:8080/cwexperimental/files/gibberish/");
      } catch (Exception e) {
        results = e.toString();
      }
      expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/gibberish/\n"
          +
          "(Error {\n" +
          "    code=404;\n" +
          "    message=\"Not Found: Currently unknown datasetID=gibberish\";\n" +
          "})";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // try to download an existent subdirectory but without trailing slash
      try {
        results = SSR.getUrlResponseStringNewline(
            "http://localhost:8080/cwexperimental/files/testGridFromErddap/GLsubdir");
      } catch (Exception e) {
        results = e.toString();
      }
      expected = "java.io.IOException: ERROR from url=http://localhost:8080/cwexperimental/files/testGridFromErddap/GLsubdir : "
          +
          "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: " +
          "http://localhost:8080/cwexperimental/files/nceiPH53sstn1day/GLsubdir\n" +
          "(Error {\n" +
          "    code=404;\n" +
          "    message=\"Not Found: File not found: GLsubdir .\";\n" +
          "})";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // try to download a non-existent directory
      try {
        results = SSR.getUrlResponseStringNewline(
            "http://localhost:8080/cwexperimental/files/testGridFromErddap/gibberish/");
      } catch (Exception e) {
        results = e.toString();
      }
      expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: " +
          "http://localhost:8080/cwexperimental/files/testGridFromErddap/gibberish/\n" +
          "(Error {\n" +
          "    code=404;\n" +
          "    message=\"Not Found: Resource not found: directory=gibberish/\";\n" +
          "})";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // try to download a non-existent file in root
      try {
        results = SSR.getUrlResponseStringNewline(
            "http://localhost:8080/cwexperimental/files/testGridFromErddap/gibberish.csv");
      } catch (Exception e) {
        results = e.toString();
      }
      expected = "java.io.IOException: ERROR from url=http://localhost:8080/cwexperimental/files/testGridFromErddap/gibberish.csv : "
          +
          "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: " +
          "http://localhost:8080/cwexperimental/files/nceiPH53sstn1day/gibberish.csv\n" +
          "(Error {\n" +
          "    code=404;\n" +
          "    message=\"Not Found: File not found: gibberish.csv .\";\n" +
          "})";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // try to download a non-existent file in existent subdir
      try {
        results = SSR.getUrlResponseStringNewline(
            "http://localhost:8080/cwexperimental/files/testGridFromErddap/GLsubdir/gibberish.csv");
      } catch (Exception e) {
        results = e.toString();
      }
      expected = "java.io.IOException: ERROR from url=http://localhost:8080/cwexperimental/files/testGridFromErddap/GLsubdir/gibberish.csv : "
          +
          "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: " +
          "http://localhost:8080/cwexperimental/files/nceiPH53sstn1day/GLsubdir/gibberish.csv\n" +
          "(Error {\n" +
          "    code=404;\n" +
          "    message=\"Not Found: File not found: gibberish.csv .\";\n" +
          "})";
      Test.ensureEqual(results, expected, "results=\n" + results);

    } catch (Throwable t) {
      throw new RuntimeException(
          "Unexpected error. This test requires nceiPH53sstn1day and testGridFromErddap in the localhost ERDDAP.", t);
    }
  }
}
