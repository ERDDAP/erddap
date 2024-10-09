package gov.noaa.pfel.coastwatch.util;

import com.cohort.array.DoubleArray;
import com.cohort.array.LongArray;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.file.Path;
import tags.TagAWS;
import tags.TagExternalOther;
import tags.TagLargeFiles;
import tags.TagMissingDataset;
import tags.TagMissingFile;
import tags.TagSlowTests;
import tags.TagThredds;

class FileVisitorDNLSTests {
  /** This tests THREDDS-related methods. */
  @org.junit.jupiter.api.Test
  @TagThredds
  void testThredds() throws Throwable {
    // String2.log("\n*** FileVisitorDNLS.testThredds");
    // boolean oReallyVerbose = reallyVerbose;
    // reallyVerbose = true;

    if (true)
      Test.knownProblem(
          "2020-10-22 FileVisitorDNLS.testThredds is not run now because the sourceUrl often stalls: https://data.nodc.noaa.gov/thredds");

    // String url =
    // "https://data.nodc.noaa.gov/thredds/catalog/aquarius/nodc_binned_V4.0/monthly/";
    // //catalog.html //was
    String url =
        "https://www.ncei.noaa.gov/thredds-ocean/catalog/aquarius/nodc_binned_V4.0/monthly/"; // catalog.html
    // //doesn't
    // exist.
    // Where
    // is this
    // dataset
    // now?
    String fileNameRegex = "sss_binned_L3_MON_SCI_V4.0_\\d{4}\\.nc";
    boolean recursive = true;
    String pathRegex = null;
    boolean dirsToo = true;
    StringArray childUrls = new StringArray();
    DoubleArray lastModified = new DoubleArray();
    LongArray fSize = new LongArray();

    // test TT_REGEX
    // note that String2.extractCaptureGroup fails if the string has line
    // terminators
    Test.ensureEqual(
        String2.extractRegex("q\r\na<tt>b</tt>c\r\nz", FileVisitorDNLS.TT_REGEX, 0),
        "<tt>b</tt>",
        "");

    // test error via addToThreddsUrlList
    // (yes, logged message includes directory name)
    String2.log("\nIntentional error:");
    String results =
        FileVisitorDNLS.addToThreddsUrlList(
            url + "testInvalidUrl",
            fileNameRegex,
            recursive,
            pathRegex,
            dirsToo,
            childUrls,
            lastModified,
            fSize);
    String expected = // fails very slowly!
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: https://www.ncei.noaa.gov/thredds-ocean/catalog/aquarius/nodc_binned_V4.0/monthly/testInvalidUrl/catalog.html\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // test addToThreddsUrlList
    childUrls = new StringArray();
    lastModified = new DoubleArray(); // epochSeconds
    fSize = new LongArray();
    FileVisitorDNLS.addToThreddsUrlList(
        url, fileNameRegex, recursive, pathRegex, dirsToo, childUrls, lastModified, fSize);

    results = childUrls.toNewlineString();
    expected =
        "https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V4.0/monthly/\n"
            + "https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V4.0/monthly/sss_binned_L3_MON_SCI_V4.0_2011.nc\n"
            + "https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V4.0/monthly/sss_binned_L3_MON_SCI_V4.0_2012.nc\n"
            + "https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V4.0/monthly/sss_binned_L3_MON_SCI_V4.0_2013.nc\n"
            + "https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V4.0/monthly/sss_binned_L3_MON_SCI_V4.0_2014.nc\n"
            + "https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V4.0/monthly/sss_binned_L3_MON_SCI_V4.0_2015.nc\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = lastModified.toString();
    expected = "NaN, 1.449908961E9, 1.449902223E9, 1.449887547E9, 1.449874773E9, 1.449861892E9";
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = fSize.toString();
    expected = "9223372036854775807, 2723152, 6528434, 6528434, 6528434, 3267363";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test via oneStep -- dirs
    Table table = FileVisitorDNLS.oneStep(url, fileNameRegex, recursive, pathRegex, true);
    results = table.dataToString();
    expected = // lastMod is longs, epochMilliseconds
        "directory,name,lastModified,size\n"
            + "https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V4.0/monthly/,,,\n"
            + "https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V4.0/monthly/,sss_binned_L3_MON_SCI_V4.0_2011.nc,1449908961000,2723152\n"
            + "https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V4.0/monthly/,sss_binned_L3_MON_SCI_V4.0_2012.nc,1449902223000,6528434\n"
            + "https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V4.0/monthly/,sss_binned_L3_MON_SCI_V4.0_2013.nc,1449887547000,6528434\n"
            + "https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V4.0/monthly/,sss_binned_L3_MON_SCI_V4.0_2014.nc,1449874773000,6528434\n"
            + "https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V4.0/monthly/,sss_binned_L3_MON_SCI_V4.0_2015.nc,1449861892000,3267363\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test via oneStep -- no dirs
    table = FileVisitorDNLS.oneStep(url, fileNameRegex, recursive, pathRegex, false);
    results = table.dataToString();
    expected =
        "directory,name,lastModified,size\n"
            + "https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V4.0/monthly/,sss_binned_L3_MON_SCI_V4.0_2011.nc,1449908961000,2723152\n"
            + "https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V4.0/monthly/,sss_binned_L3_MON_SCI_V4.0_2012.nc,1449902223000,6528434\n"
            + "https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V4.0/monthly/,sss_binned_L3_MON_SCI_V4.0_2013.nc,1449887547000,6528434\n"
            + "https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V4.0/monthly/,sss_binned_L3_MON_SCI_V4.0_2014.nc,1449874773000,6528434\n"
            + "https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V4.0/monthly/,sss_binned_L3_MON_SCI_V4.0_2015.nc,1449861892000,3267363\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // reallyVerbose = oReallyVerbose;
    // String2.log("\n*** FileVisitorDNLS.testThredds finished successfully");
  }

  /** This tests this class with the local file system. */
  @org.junit.jupiter.api.Test
  void testLocal() throws Throwable {
    // String2.log("\n*** FileVisitorDNLS.testLocal");
    // verbose = true;
    boolean doBigTest = true;
    String tPathRegex = null;
    Table table;
    long time;
    int n;
    String results, expected;
    String testDir =
        Path.of(FileVisitorDNLSTests.class.getResource("/data/fileNames").toURI())
            .toString()
            .replace('\\', '/');

    String expectedDir = testDir + "/";

    // recursive and dirToo and test \\ separator
    table = FileVisitorDNLS.oneStep(testDir, ".*\\.png", true, tPathRegex, true);
    table.removeColumn("lastModified");
    results = table.dataToString();
    expected =
        "directory,name,size\n"
            + expectedDir
            + ",jplMURSST20150103090000.png,46482\n"
            + expectedDir
            + ",jplMURSST20150104090000.png,46586\n"
            + expectedDir
            + "sub/,,0\n"
            + expectedDir
            + "sub/,jplMURSST20150105090000.png,46549\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // recursive and !dirToo and test // separator
    table = FileVisitorDNLS.oneStep(testDir, ".*\\.png", true, tPathRegex, false);
    table.removeColumn("lastModified");
    results = table.dataToString();
    expected =
        "directory,name,size\n"
            + expectedDir
            + ",jplMURSST20150103090000.png,46482\n"
            + expectedDir
            + ",jplMURSST20150104090000.png,46586\n"
            + expectedDir
            + "sub/,jplMURSST20150105090000.png,46549\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // !recursive and dirToo
    table = FileVisitorDNLS.oneStep(testDir, ".*\\.png", false, tPathRegex, true);
    table.removeColumn("lastModified");
    results = table.dataToString();
    expected =
        "directory,name,size\n"
            + expectedDir
            + ",jplMURSST20150103090000.png,46482\n"
            + expectedDir
            + ",jplMURSST20150104090000.png,46586\n"
            + expectedDir
            + "sub/,,0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // !recursive and !dirToo
    table = FileVisitorDNLS.oneStep(testDir, ".*\\.png", false, tPathRegex, false);
    table.removeColumn("lastModified");
    results = table.dataToString();
    expected =
        "directory,name,size\n"
            + expectedDir
            + ",jplMURSST20150103090000.png,46482\n"
            + expectedDir
            + ",jplMURSST20150104090000.png,46586\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // ***
    // oneStepDouble
    table = FileVisitorDNLS.oneStepDouble(testDir, ".*\\.png", true, tPathRegex, true);
    table.removeColumn("lastModified");
    results = table.toString();
    expected =
        "{\n"
            + "dimensions:\n"
            + "\trow = 4 ;\n"
            + "\tdirectory_strlen = "
            + (expectedDir.length() + 4)
            + " ;\n"
            + "\tname_strlen = 27 ;\n"
            + "variables:\n"
            + "\tchar directory(row, directory_strlen) ;\n"
            + "\t\tdirectory:ioos_category = \"Identifier\" ;\n"
            + "\t\tdirectory:long_name = \"Directory\" ;\n"
            + "\tchar name(row, name_strlen) ;\n"
            + "\t\tname:ioos_category = \"Identifier\" ;\n"
            + "\t\tname:long_name = \"File Name\" ;\n"
            + "\tdouble size(row) ;\n"
            + "\t\tsize:ioos_category = \"Other\" ;\n"
            + "\t\tsize:long_name = \"Size\" ;\n"
            + "\t\tsize:units = \"bytes\" ;\n"
            + "\n"
            + "// global attributes:\n"
            + "}\n"
            + "directory,name,size\n"
            + expectedDir
            + ",jplMURSST20150103090000.png,46482.0\n"
            + expectedDir
            + ",jplMURSST20150104090000.png,46586.0\n"
            + expectedDir
            + "sub/,,0.0\n"
            + expectedDir
            + "sub/,jplMURSST20150105090000.png,46549.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // ***
    // oneStepAccessibleViaFiles
    table =
        FileVisitorDNLS.oneStepDoubleWithUrlsNotDirs(
            testDir,
            ".*\\.png",
            true,
            tPathRegex,
            "http://localhost:8080/cwexperimental/files/testFileNames/");
    table.removeColumn("lastModified");
    results = table.toString();
    expected =
        "{\n"
            + "dimensions:\n"
            + "\trow = 3 ;\n"
            + "\turl_strlen = 88 ;\n"
            + "\tname_strlen = 27 ;\n"
            + "variables:\n"
            + "\tchar url(row, url_strlen) ;\n"
            + "\t\turl:ioos_category = \"Identifier\" ;\n"
            + "\t\turl:long_name = \"URL\" ;\n"
            + "\tchar name(row, name_strlen) ;\n"
            + "\t\tname:ioos_category = \"Identifier\" ;\n"
            + "\t\tname:long_name = \"File Name\" ;\n"
            +
            // "\tdouble lastModified(row) ;\n" +
            // "\t\tlastModified:ioos_category = \"Time\" ;\n" +
            // "\t\tlastModified:long_name = \"Last Modified\" ;\n" +
            // "\t\tlastModified:units = \"seconds since 1970-01-01T00:00:00Z\" ;\n" +
            "\tdouble size(row) ;\n"
            + "\t\tsize:ioos_category = \"Other\" ;\n"
            + "\t\tsize:long_name = \"Size\" ;\n"
            + "\t\tsize:units = \"bytes\" ;\n"
            + "\n"
            + "// global attributes:\n"
            + "}\n"
            + "url,name,size\n"
            + "http://localhost:8080/cwexperimental/files/testFileNames/jplMURSST20150103090000.png,jplMURSST20150103090000.png,46482.0\n"
            + "http://localhost:8080/cwexperimental/files/testFileNames/jplMURSST20150104090000.png,jplMURSST20150104090000.png,46586.0\n"
            + "http://localhost:8080/cwexperimental/files/testFileNames/sub/jplMURSST20150105090000.png,jplMURSST20150105090000.png,46549.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *** huge dir
    String unexpected =
        "\nUnexpected FileVisitorDNLS error (but /data/gtspp/temp dir has variable nFiles):\n";

    if (doBigTest) {
      for (int attempt = 0; attempt < 2; attempt++) {
        // try {
        // forward slash in huge directory
        time = System.currentTimeMillis();
        table = FileVisitorDNLS.oneStep("/data/gtspp/temp", ".*\\.nc", false, tPathRegex, false);
        time = System.currentTimeMillis() - time;
        // 2014-11-25 98436 files in 410ms
        StringArray directoryPA = (StringArray) table.getColumn(FileVisitorDNLS.DIRECTORY);
        String2.log("forward test: n=" + directoryPA.size() + " time=" + time + "ms");
        if (directoryPA.size() < 1000) {
          String2.log(directoryPA.size() + " files. Not a good test.");
        } else {
          Test.ensureBetween(
              time / (double) directoryPA.size(), 2e-3, 8e-3, "ms/file (4.1e-3 expected)");
          String dir0 = directoryPA.get(0);
          String2.log("forward slash test: dir0=" + dir0);
          Test.ensureTrue(dir0.indexOf('\\') < 0, "");
          Test.ensureTrue(dir0.endsWith("/"), "");
        }
        // } catch (Throwable t) {
        //   String2.pressEnterToContinue(unexpected +
        //       MustBe.throwableToString(t));
        // }
      }

      for (int attempt = 0; attempt < 2; attempt++) {
        // try {
        // backward slash in huge directory
        time = System.currentTimeMillis();
        table = FileVisitorDNLS.oneStep("\\data\\gtspp\\temp", ".*\\.nc", false, tPathRegex, false);
        time = System.currentTimeMillis() - time;
        // 2014-11-25 98436 files in 300ms
        StringArray directoryPA = (StringArray) table.getColumn(FileVisitorDNLS.DIRECTORY);
        String2.log("backward test: n=" + directoryPA.size() + " time=" + time + "ms");
        if (directoryPA.size() < 1000) {
          String2.log(directoryPA.size() + " files. Not a good test.");
        } else {
          Test.ensureBetween(
              time / (double) directoryPA.size(), 1e-3, 8e-3, "ms/file (3e-3 expected)");
          String dir0 = directoryPA.get(0);
          String2.log("backward slash test: dir0=" + dir0);
          Test.ensureTrue(dir0.indexOf('/') < 0, "");
          Test.ensureTrue(dir0.endsWith("\\"), "");
        }
        // } catch (Throwable t) {
        //   String2.pressEnterToContinue(unexpected +
        //       MustBe.throwableToString(t));
        // }
      }
    }
    String2.log("\n*** FileVisitorDNLS.testLocal finished.");
  }

  /**
   * This tests this class with Amazon AWS S3 file system. Your S3 credentials must be in <br>
   * ~/.aws/credentials on Linux, OS X, or Unix <br>
   * C:\Users\USERNAME\.aws\credentials on Windows See
   * https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingMetadata.html See
   * https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingBucket.html See
   * https://docs.aws.amazon.com/AmazonS3/latest/dev/ListingKeysHierarchy.html See
   * https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/setup.html#setup-credentials
   */
  @org.junit.jupiter.api.Test
  @TagAWS
  void testAWSS3() throws Throwable {
    // String2.log("\n*** FileVisitorDNLS.testAWSS3");

    // verbose = true;
    Table table;
    long time;
    int n;
    String results, expected;
    // this works in browser: http://nasanex.s3.us-west-2.amazonaws.com
    // the full parent here doesn't work in a browser.
    // But ERDDAP knows that "nasanex" is the bucket name and
    // "NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/" is the prefix.
    // See https://docs.aws.amazon.com/AmazonS3/latest/dev/ListingKeysHierarchy.html
    String parent =
        "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/";
    String child = "CONUS/";
    String pathRegex = null;

    {

      // !recursive and dirToo
      table =
          FileVisitorDNLS.oneStep(
              "https://nasanex.s3.us-west-2.amazonaws.com",
              ".*",
              false,
              ".*",
              true); // fileNameRegex, tRecursive, pathRegex, tDirectoriesToo
      results = table.dataToString();
      expected =
          "directory,name,lastModified,size\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/AVHRR/,,,\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/CMIP5/,,,\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/Landsat/,,,\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/LOCA/,,,\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/MAIAC/,,,\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/MODIS/,,,\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NAIP/,,,\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/,,,\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-GDDP/,,,\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // !recursive and dirToo
      table =
          FileVisitorDNLS.oneStep(
              "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/",
              ".*",
              false,
              ".*",
              true); // fileNameRegex, tRecursive, pathRegex, tDirectoriesToo
      results = table.dataToString();
      expected =
          "directory,name,lastModified,size\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/,,,\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/,doi.txt,1380418295000,35\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/,nex-dcp30-s3-files.json,1473288687000,2717227\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/,,,\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/CONTRIB/,,,\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/NEX-quartile/,,,\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // !recursive and !dirToo
      table =
          FileVisitorDNLS.oneStep(
              "https://nasanex.s3.us-west-2.amazonaws.com",
              ".*",
              false,
              ".*",
              false); // fileNameRegex, tRecursive, pathRegex, tDirectoriesToo
      results = table.dataToString();
      expected = "directory,name,lastModified,size\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // !recursive and !dirToo
      table =
          FileVisitorDNLS.oneStep(
              "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/",
              ".*",
              false,
              ".*",
              false); // fileNameRegex, tRecursive, pathRegex, tDirectoriesToo
      results = table.dataToString();
      expected =
          "directory,name,lastModified,size\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/,doi.txt,1380418295000,35\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/,nex-dcp30-s3-files.json,1473288687000,2717227\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // recursive and dirToo
      table = FileVisitorDNLS.oneStep(parent, ".*\\.nc", true, pathRegex, true);
      results = table.dataToString();
      expected =
          "directory,name,lastModified,size\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/,,,\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,,,\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_200601-201012.nc,1380652638000,1368229240\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_201101-201512.nc,1380649780000,1368487462\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_201601-202012.nc,1380651065000,1368894133\n";
      if (expected.length() > results.length()) String2.log("results=\n" + results);
      Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

      // recursive and !dirToo
      table = FileVisitorDNLS.oneStep(parent, ".*\\.nc", true, pathRegex, false);
      results = table.dataToString();
      expected =
          "directory,name,lastModified,size\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_200601-201012.nc,1380652638000,1368229240\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_201101-201512.nc,1380649780000,1368487462\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_201601-202012.nc,1380651065000,1368894133\n";
      if (expected.length() > results.length()) String2.log("results=\n" + results);
      Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

      // !recursive and dirToo
      table = FileVisitorDNLS.oneStep(parent + child, ".*\\.nc", false, pathRegex, true);
      results = table.dataToString();
      expected =
          "directory,name,lastModified,size\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,,,\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_200601-201012.nc,1380652638000,1368229240\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_201101-201512.nc,1380649780000,1368487462\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_201601-202012.nc,1380651065000,1368894133\n";
      if (expected.length() > results.length()) String2.log("results=\n" + results);
      Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

      // !recursive and !dirToo
      table = FileVisitorDNLS.oneStep(parent + child, ".*\\.nc", false, pathRegex, false);
      results = table.dataToString();
      expected =
          "directory,name,lastModified,size\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_200601-201012.nc,1380652638000,1368229240\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_201101-201512.nc,1380649780000,1368487462\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_201601-202012.nc,1380651065000,1368894133\n";
      if (expected.length() > results.length()) String2.log("results=\n" + results);
      Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
    } /* */

    // recursive and dirToo
    // reallyVerbose = true;
    // debugMode = true;
    parent = "https://noaa-goes17.s3.us-east-1.amazonaws.com/ABI-L1b-RadC/2018/338/";
    pathRegex = ".*";
    table = FileVisitorDNLS.oneStep(parent, ".*\\.nc", true, pathRegex, true);
    results = table.dataToString();
    expected =
        "directory,name,lastModified,size\n"
            + "https://noaa-goes17.s3.us-east-1.amazonaws.com/ABI-L1b-RadC/2018/338/,,,\n"
            + "https://noaa-goes17.s3.us-east-1.amazonaws.com/ABI-L1b-RadC/2018/338/00/,,,\n"
            + "https://noaa-goes17.s3.us-east-1.amazonaws.com/ABI-L1b-RadC/2018/338/00/,OR_ABI-L1b-RadC-M3C01_G17_s20183380002190_e20183380004563_c20183380004595.nc,1582123265000,12524368\n"
            + "https://noaa-goes17.s3.us-east-1.amazonaws.com/ABI-L1b-RadC/2018/338/00/,OR_ABI-L1b-RadC-M3C01_G17_s20183380007190_e20183380009563_c20183380009597.nc,1582123265000,12357541\n"
            + "https://noaa-goes17.s3.us-east-1.amazonaws.com/ABI-L1b-RadC/2018/338/00/,OR_ABI-L1b-RadC-M3C01_G17_s20183380012190_e20183380014503_c20183380014536.nc,1582123255000,12187253\n";
    // before 2020-03-03 these were the first returned rows:
    // "https://noaa-goes17.s3.us-east-1.amazonaws.com/ABI-L1b-RadC/2018/338/16/,,,\n"
    // +
    // "https://noaa-goes17.s3.us-east-1.amazonaws.com/ABI-L1b-RadC/2018/338/16/,OR_ABI-L1b-RadC-M3C01_G17_s20183381637189_e20183381639562_c20183381639596.nc,1543941659000,9269699\n"
    // +
    // "https://noaa-goes17.s3.us-east-1.amazonaws.com/ABI-L1b-RadC/2018/338/16/,OR_ABI-L1b-RadC-M3C01_G17_s20183381642189_e20183381644502_c20183381644536.nc,1543942123000,9585452\n"
    // +
    // "https://noaa-goes17.s3.us-east-1.amazonaws.com/ABI-L1b-RadC/2018/338/16/,OR_ABI-L1b-RadC-M3C01_G17_s20183381647189_e20183381649562_c20183381649596.nc,1543942279000,9894495\n"
    // +
    // "https://noaa-goes17.s3.us-east-1.amazonaws.com/ABI-L1b-RadC/2018/338/16/,OR_ABI-L1b-RadC-M3C01_G17_s20183381652189_e20183381654562_c20183381654595.nc,1543942520000,10195765\n";
    if (expected.length() > results.length()) String2.log("results=\n" + results);
    Test.ensureEqual(
        results.substring(0, expected.length()),
        expected,
        "results=\n" + results.substring(0, 1000));

    String2.log("\n*** FileVisitorDNLS.testAWSS3 finished.");
  }

  /**
   * This tests this class with Amazon AWS S3 file system and reading all from a big directory. Your
   * S3 credentials must be in <br>
   * ~/.aws/credentials on Linux, OS X, or Unix <br>
   * C:\Users\USERNAME\.aws\credentials on Windows See
   * https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingMetadata.html See
   * https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingBucket.html See
   * https://docs.aws.amazon.com/AmazonS3/latest/dev/ListingKeysHierarchy.html See
   * https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/setup.html#setup-credentials
   */
  @org.junit.jupiter.api.Test
  @TagAWS
  void testBigAWSS3() throws Throwable {
    // String2.log("\n*** FileVisitorDNLS.testBigAWSS3");

    // verbose = true;
    // debugMode = true;
    Table table;
    long time;
    int n;
    String results, expected;
    // this works in browser: http://nasanex.s3.us-west-2.amazonaws.com
    // the full parent here doesn't work in a browser.
    // But ERDDAP knows that "nasanex" is the bucket name and
    // "NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/" is the prefix.
    // See https://docs.aws.amazon.com/AmazonS3/latest/dev/ListingKeysHierarchy.html
    String parent =
        "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/";
    String child = "CONUS/";
    String pathRegex = null;
    String fullResults = null;

    // test that the results are identical regardless of page size
    int maxKeys[] = new int[] {10, 100, 1000}; // expectedCount is ~870.
    int oS3Chunk = FileVisitorDNLS.S3_CHUNK_TO_FILE;
    FileVisitorDNLS.S3_CHUNK_TO_FILE = 8; // small, to test chunking to temp file
    for (int mk = 0; mk < maxKeys.length; mk++) {
      FileVisitorDNLS.S3_MAX_KEYS = maxKeys[mk];

      // recursive and dirToo
      table =
          FileVisitorDNLS.oneStep(
              parent, ".*\\.nc", true, pathRegex, true); // there is a .nc.md5 for each
      // .nc, so
      // oneStep filters client-side
      results = table.dataToString();
      expected =
          "directory,name,lastModified,size\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/,,,\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,,,\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_200601-201012.nc,1380652638000,1368229240\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_201101-201512.nc,1380649780000,1368487462\n"
              + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_bcc-csm1-1_201601-202012.nc,1380651065000,1368894133\n";
      if (expected.length() > results.length()) String2.log("results=\n" + results);
      Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

      // test that results are the same each time
      if (fullResults == null) fullResults = results;
      Test.ensureEqual(
          results, fullResults, "results=\n" + results + "\nfullResults=\n" + fullResults);

      String2.log(
          "nLines="
              + String2.countAll(results, "\n")); // 2021-11-30 441 but it processes ~890 items
      // (half .nc,
      // half .nc.md5)

    }
    // debugMode = false;
    FileVisitorDNLS.S3_CHUNK_TO_FILE = oS3Chunk;
  }

  /**
   * This tests this class with Amazon AWS S3 file system. Your S3 credentials must be in <br>
   * ~/.aws/credentials on Linux, OS X, or Unix <br>
   * C:\Users\USERNAME\.aws\credentials on Windows See
   * https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingMetadata.html See
   * https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingBucket.html See
   * https://docs.aws.amazon.com/AmazonS3/latest/dev/ListingKeysHierarchy.html See
   * https://docs.aws.amazon.com/sdk-for-java/?id=docs_gateway#aws-sdk-for-java,-version-1 .
   * https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html#credentials-file-format
   */
  @org.junit.jupiter.api.Test
  @TagAWS
  void testPrivateAWSS3() throws Throwable {
    // String2.log("\n*** FileVisitorDNLS.testPrivateAWSS3");

    // verbose = true;
    Table table;
    long time;
    int n;
    String results, expected;
    // 2021-04-16 created a bucket:
    // log into AWS console as root (or IAM?)
    // Services : S3 : Create bucket: name=bobsimonsdata, region us-east-1, block
    // all public access, versioning=disabled, encryption=false,
    // upload files: Create folder, then Upload files

    String bucket = "https://bobsimonsdata.s3.us-east-1.amazonaws.com/";
    String dir = bucket + "erdQSwind1day/";
    String pathRegex = null;

    // !recursive and dirToo
    table =
        FileVisitorDNLS.oneStep(
            bucket, ".*", false, ".*", true); // fileNameRegex, tRecursive, pathRegex,
    // tDirectoriesToo
    results = table.dataToString();
    expected =
        "directory,name,lastModified,size\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/ascii/,,,\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/erdQSwind1day/,,,\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/testMediaFiles/,,,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // !recursive and dirToo
    table =
        FileVisitorDNLS.oneStep(
            dir, ".*", false, ".*", true); // fileNameRegex, tRecursive, pathRegex,
    // tDirectoriesToo
    results = table.dataToString();
    expected =
        "directory,name,lastModified,size\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/erdQSwind1day/,,,\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/erdQSwind1day/,bad.nc,1620243280000,39102\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/erdQSwind1day/,BadFileNoExtension,1620243280000,39102\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/erdQSwind1day/,erdQSwind1day_20080101_03.nc.gz,1620243280000,10478645\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/erdQSwind1day/,erdQSwind1day_20080104_07.nc,1620243281000,49790172\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/erdQSwind1day/subfolder/,,,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // !recursive and !dirToo
    table =
        FileVisitorDNLS.oneStep(
            bucket, ".*", false, ".*", false); // fileNameRegex, tRecursive, pathRegex,
    // tDirectoriesToo
    results = table.dataToString();
    expected = "directory,name,lastModified,size\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // !recursive and !dirToo
    table =
        FileVisitorDNLS.oneStep(
            dir, ".*", false, ".*", false); // fileNameRegex, tRecursive, pathRegex,
    // tDirectoriesToo
    results = table.dataToString();
    expected =
        "directory,name,lastModified,size\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/erdQSwind1day/,bad.nc,1620243280000,39102\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/erdQSwind1day/,BadFileNoExtension,1620243280000,39102\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/erdQSwind1day/,erdQSwind1day_20080101_03.nc.gz,1620243280000,10478645\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/erdQSwind1day/,erdQSwind1day_20080104_07.nc,1620243281000,49790172\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // recursive and dirToo
    table = FileVisitorDNLS.oneStep(bucket, ".*\\.nc", true, pathRegex, true);
    results = table.dataToString();
    expected =
        "directory,name,lastModified,size\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/,,,\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/ascii/,,,\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/erdQSwind1day/,,,\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/erdQSwind1day/,bad.nc,1620243280000,39102\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/erdQSwind1day/,erdQSwind1day_20080104_07.nc,1620243281000,49790172\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/erdQSwind1day/subfolder/,,,\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/erdQSwind1day/subfolder/,erdQSwind1day_20080108_10.nc,1620243280000,37348564\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/testMediaFiles/,,,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // recursive and !dirToo
    table = FileVisitorDNLS.oneStep(bucket, ".*\\.nc", true, pathRegex, false);
    results = table.dataToString();
    expected =
        "directory,name,lastModified,size\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/erdQSwind1day/,bad.nc,1620243280000,39102\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/erdQSwind1day/,erdQSwind1day_20080104_07.nc,1620243281000,49790172\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/erdQSwind1day/subfolder/,erdQSwind1day_20080108_10.nc,1620243280000,37348564\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // !recursive and dirToo
    table = FileVisitorDNLS.oneStep(dir, ".*\\.nc", false, pathRegex, true);
    results = table.dataToString();
    expected =
        "directory,name,lastModified,size\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/erdQSwind1day/,,,\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/erdQSwind1day/,bad.nc,1620243280000,39102\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/erdQSwind1day/,erdQSwind1day_20080104_07.nc,1620243281000,49790172\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/erdQSwind1day/subfolder/,,,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // !recursive and !dirToo
    table = FileVisitorDNLS.oneStep(dir, ".*\\.nc", false, pathRegex, false);
    results = table.dataToString();
    expected =
        "directory,name,lastModified,size\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/erdQSwind1day/,bad.nc,1620243280000,39102\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/erdQSwind1day/,erdQSwind1day_20080104_07.nc,1620243281000,49790172\n";
    if (expected.length() > results.length()) String2.log("results=\n" + results);
    Test.ensureEqual(results, expected, "results=\n" + results);

    // recursive and dirToo
    // reallyVerbose = true;
    // debugMode = true;
    pathRegex = ".*";
    table = FileVisitorDNLS.oneStep(dir + "subfolder/", ".*\\.nc", true, pathRegex, true);
    results = table.dataToString();
    expected =
        "directory,name,lastModified,size\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/erdQSwind1day/subfolder/,,,\n"
            + "https://bobsimonsdata.s3.us-east-1.amazonaws.com/erdQSwind1day/subfolder/,erdQSwind1day_20080108_10.nc,1620243280000,37348564\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // String2.log("\n*** FileVisitorDNLS.testPrivateAWSS3 finished.");

  }

  /** This tests Hyrax-related methods. */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testHyrax() throws Throwable {
    // String2.log("\n*** FileVisitorDNLS.testHyrax()\n");
    // reallyVerbose = true;
    // debugMode=true;

    // before 2018-08-17 podaac-opendap caused
    // "javax.net.ssl.SSLProtocolException: handshake alert: unrecognized_name"
    // error
    // String url =
    // "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/";
    // //contents.html
    // so I used domain name shown on digital certificate: opendap.jpl.nasa.gov for
    // tests below
    // But now podaac-opendap works.
    String url =
        "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/"; // contents.html
    String fileNameRegex = "month_198(8|9).*flk\\.nc\\.gz";
    boolean recursive = true;
    String pathRegex = null;
    boolean dirsToo = true;
    StringArray childUrls = new StringArray();
    DoubleArray lastModified = new DoubleArray();
    LongArray size = new LongArray();
    String results, expected;

    // test error via addToHyraxUrlList
    // (yes, logged message includes directory name)
    String2.log("\nIntentional error:");
    results =
        FileVisitorDNLS.addToHyraxUrlList(
            url + "testInvalidUrl",
            fileNameRegex,
            recursive,
            pathRegex,
            dirsToo,
            childUrls,
            lastModified,
            size);
    expected =
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/testInvalidUrl/contents.html\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // test addToHyraxUrlList
    childUrls = new StringArray();
    lastModified = new DoubleArray();
    size = new LongArray();
    results =
        FileVisitorDNLS.addToHyraxUrlList(
            url, fileNameRegex, recursive, pathRegex, dirsToo, childUrls, lastModified, size);
    Test.ensureEqual(results, "", "results=\n" + results);
    Table table = new Table();
    table.addColumn("URL", childUrls);
    table.addColumn("lastModified", lastModified);
    table.addColumn("size", size);
    results = table.dataToString();
    expected =
        "URL,lastModified,size\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/,,\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,,\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/,,\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880101_v11l35flk.nc.gz,1.336863115E9,4981045\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880201_v11l35flk.nc.gz,1.336723222E9,5024372\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880301_v11l35flk.nc.gz,1.336546575E9,5006043\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880401_v11l35flk.nc.gz,1.336860015E9,4948285\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880501_v11l35flk.nc.gz,1.336835143E9,4914250\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880601_v11l35flk.nc.gz,1.336484405E9,4841084\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880701_v11l35flk.nc.gz,1.336815079E9,4837417\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880801_v11l35flk.nc.gz,1.336799789E9,4834242\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880901_v11l35flk.nc.gz,1.336676042E9,4801865\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19881001_v11l35flk.nc.gz,1.336566352E9,4770289\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19881101_v11l35flk.nc.gz,1.336568382E9,4769160\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19881201_v11l35flk.nc.gz,1.336838712E9,4866335\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/,,\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890101_v11l35flk.nc.gz,1.336886548E9,5003981\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890201_v11l35flk.nc.gz,1.336268373E9,5054907\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890301_v11l35flk.nc.gz,1.336605483E9,4979393\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890401_v11l35flk.nc.gz,1.336350339E9,4960865\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890501_v11l35flk.nc.gz,1.336551575E9,4868541\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890601_v11l35flk.nc.gz,1.336177278E9,4790364\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890701_v11l35flk.nc.gz,1.336685187E9,4854943\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890801_v11l35flk.nc.gz,1.336534686E9,4859216\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890901_v11l35flk.nc.gz,1.33622953E9,4838390\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19891001_v11l35flk.nc.gz,1.336853599E9,4820645\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19891101_v11l35flk.nc.gz,1.336882933E9,4748166\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19891201_v11l35flk.nc.gz,1.336748115E9,4922858\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1990/,,\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1991/,,\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1992/,,\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1993/,,\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1994/,,\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1995/,,\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1996/,,\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1997/,,\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1998/,,\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1999/,,\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2000/,,\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2001/,,\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2002/,,\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2003/,,\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2004/,,\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2005/,,\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2006/,,\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2007/,,\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2008/,,\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2009/,,\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2010/,,\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/2011/,,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test getUrlsFromHyraxCatalog
    String resultsAr[] =
        FileVisitorDNLS.getUrlsFromHyraxCatalog(url, fileNameRegex, recursive, pathRegex);
    String expectedAr[] =
        new String[] {
          "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880101_v11l35flk.nc.gz",
          "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880201_v11l35flk.nc.gz",
          "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880301_v11l35flk.nc.gz",
          "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880401_v11l35flk.nc.gz",
          "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880501_v11l35flk.nc.gz",
          "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880601_v11l35flk.nc.gz",
          "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880701_v11l35flk.nc.gz",
          "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880801_v11l35flk.nc.gz",
          "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880901_v11l35flk.nc.gz",
          "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19881001_v11l35flk.nc.gz",
          "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19881101_v11l35flk.nc.gz",
          "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19881201_v11l35flk.nc.gz",
          "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890101_v11l35flk.nc.gz",
          "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890201_v11l35flk.nc.gz",
          "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890301_v11l35flk.nc.gz",
          "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890401_v11l35flk.nc.gz",
          "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890501_v11l35flk.nc.gz",
          "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890601_v11l35flk.nc.gz",
          "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890701_v11l35flk.nc.gz",
          "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890801_v11l35flk.nc.gz",
          "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19890901_v11l35flk.nc.gz",
          "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19891001_v11l35flk.nc.gz",
          "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19891101_v11l35flk.nc.gz",
          "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1989/month_19891201_v11l35flk.nc.gz"
        };
    Test.ensureEqual(resultsAr, expectedAr, "results=\n" + results);

    // different test of addToHyraxUrlList
    childUrls = new StringArray();
    lastModified = new DoubleArray();
    LongArray fSize = new LongArray(); // test that it will call setMaxIsMV(true)
    url =
        "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/"; // startUrl,
    fileNameRegex = "month_[0-9]{8}_v11l35flk\\.nc\\.gz"; // fileNameRegex,
    recursive = true;
    results =
        FileVisitorDNLS.addToHyraxUrlList(
            url, fileNameRegex, recursive, pathRegex, dirsToo, childUrls, lastModified, fSize);
    Test.ensureEqual(results, "", "results=\n" + results);

    results = childUrls.toNewlineString();
    expected =
        "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/month_19870701_v11l35flk.nc.gz\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/month_19870801_v11l35flk.nc.gz\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/month_19870901_v11l35flk.nc.gz\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/month_19871001_v11l35flk.nc.gz\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/month_19871101_v11l35flk.nc.gz\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/month_19871201_v11l35flk.nc.gz\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = lastModified.toString();
    expected =
        "NaN, 1.336609915E9, 1.336785444E9, 1.336673639E9, 1.336196561E9, 1.336881763E9, 1.336705731E9";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test via oneStep -- dirs
    table = FileVisitorDNLS.oneStep(url, fileNameRegex, recursive, pathRegex, true);
    results = table.dataToString();
    expected =
        "directory,name,lastModified,size\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,,,\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19870701_v11l35flk.nc.gz,1336609915000,4807310\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19870801_v11l35flk.nc.gz,1336785444000,4835774\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19870901_v11l35flk.nc.gz,1336673639000,4809582\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19871001_v11l35flk.nc.gz,1336196561000,4803285\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19871101_v11l35flk.nc.gz,1336881763000,4787239\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19871201_v11l35flk.nc.gz,1336705731000,4432696\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test via oneStep -- no dirs
    table = FileVisitorDNLS.oneStep(url, fileNameRegex, recursive, pathRegex, false);
    results = table.dataToString();
    expected =
        "directory,name,lastModified,size\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19870701_v11l35flk.nc.gz,1336609915000,4807310\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19870801_v11l35flk.nc.gz,1336785444000,4835774\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19870901_v11l35flk.nc.gz,1336673639000,4809582\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19871001_v11l35flk.nc.gz,1336196561000,4803285\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19871101_v11l35flk.nc.gz,1336881763000,4787239\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1987/,month_19871201_v11l35flk.nc.gz,1336705731000,4432696\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** This tests Hyrax-related methods with the JPL MUR dataset. */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testHyraxMUR() throws Throwable {
    // String2.log("\n*** FileVisitorDNLS.testHyraxMUR()\n");
    // reallyVerbose = true;
    // debugMode=true;

    String url =
        "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/"; // contents.html
    String fileNameRegex = "[0-9]{14}-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02\\.0-fv04\\.1\\.nc";
    boolean recursive = true;
    String pathRegex = ".*/v4\\.1/2018/(|01./)"; // for test, just get 01x dirs/files. Read regex:
    // "(|2018/(|/[0-9]{3}/))";
    boolean dirsToo = false;
    StringArray childUrls = new StringArray();

    // test via oneStep -- no dirs
    Table table = FileVisitorDNLS.oneStep(url, fileNameRegex, recursive, pathRegex, false);
    String results = table.dataToString();
    String expected =
        "directory,name,lastModified,size\n"
            + // lastMod is epochMillis
            "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/2018/010/,20180110090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc,1526396428000,400940089\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/2018/011/,20180111090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc,1526400024000,402342953\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/2018/012/,20180112090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc,1526403626000,407791965\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/2018/013/,20180113090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc,1526407232000,410202577\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/2018/014/,20180114090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc,1526410828000,412787416\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/2018/015/,20180115090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc,1526414431000,408049023\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/2018/016/,20180116090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc,1526418038000,398060630\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/2018/017/,20180117090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc,1526421637000,388221460\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/2018/018/,20180118090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc,1526425240000,381433871\n"
            + "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/2018/019/,20180119090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc,1526428852000,390892954\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // debugMode=false;
  }

  /** This tests GPCP. */
  @org.junit.jupiter.api.Test
  @TagSlowTests
  @TagMissingDataset // Data temporarily unavailable at NCEI due to Hurricane Helene impacts
  void testGpcp() throws Throwable {
    String2.log("\n*** FileVisitorDNLS.testGpcp()\n");

    // * Test ncei WAF
    Table tTable =
        FileVisitorDNLS.oneStep(
            "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/",
            "gpcp_v01r03_daily_d2000010.*\\.nc",
            true,
            ".*",
            true); // tDirsToo,
    // debugMode = false;
    String results = tTable.dataToString();
    String expected =
        "directory,name,lastModified,size\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/1996/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/1997/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/1998/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/1999/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2000/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2000/,gpcp_v01r03_daily_d20000101_c20170530.nc,1496163420000,289792\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2000/,gpcp_v01r03_daily_d20000102_c20170530.nc,1496163420000,289792\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2000/,gpcp_v01r03_daily_d20000103_c20170530.nc,1496163420000,289792\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2000/,gpcp_v01r03_daily_d20000104_c20170530.nc,1496163420000,289792\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2000/,gpcp_v01r03_daily_d20000105_c20170530.nc,1496163420000,289792\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2000/,gpcp_v01r03_daily_d20000106_c20170530.nc,1496163420000,289792\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2000/,gpcp_v01r03_daily_d20000107_c20170530.nc,1496163420000,289792\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2000/,gpcp_v01r03_daily_d20000108_c20170530.nc,1496163420000,289792\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2000/,gpcp_v01r03_daily_d20000109_c20170530.nc,1496163420000,289792\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2001/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2002/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2003/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2004/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2005/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2006/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2007/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2008/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2009/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2010/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2011/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2012/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2013/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2014/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2015/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2016/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2017/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2018/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2019/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2020/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2021/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2022/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2023/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/access/2024/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/doc/,,,\n"
            + "https://www.ncei.noaa.gov/data/global-precipitation-climatology-project-gpcp-daily/src/,,,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** This tests the ERSST directory. */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testErsst() throws Throwable {
    String2.log("\n*** FileVisitorDNLS.testErsst()\n");
    Table table = FileVisitorDNLS.makeEmptyTable();
    StringArray dirs = (StringArray) table.getColumn(0);
    StringArray names = (StringArray) table.getColumn(1);
    LongArray lastModifieds = (LongArray) table.getColumn(2);
    LongArray sizes = (LongArray) table.getColumn(3);

    String url = "https://www1.ncdc.noaa.gov/pub/data/cmb/ersst/v4/netcdf/";
    String tFileNameRegex = "ersst.v4.19660.*\\.nc";
    boolean tRecursive = false;
    String tPathRegex = ".*";
    boolean tDirsToo = true;
    String results =
        FileVisitorDNLS.addToWAFUrlList( // returns a list of errors or ""
            url,
            tFileNameRegex,
            tRecursive,
            tPathRegex,
            tDirsToo,
            dirs,
            names,
            lastModifieds,
            sizes);
    Test.ensureEqual(results, "", "results=\n" + results);
    results = table.dataToString();
    String expected =
        "directory,name,lastModified,size\n"
            + "https://www1.ncdc.noaa.gov/pub/data/cmb/ersst/v4/netcdf/,ersst.v4.196601.nc,1479909540000,135168\n"
            + "https://www1.ncdc.noaa.gov/pub/data/cmb/ersst/v4/netcdf/,ersst.v4.196602.nc,1479909540000,135168\n"
            + "https://www1.ncdc.noaa.gov/pub/data/cmb/ersst/v4/netcdf/,ersst.v4.196603.nc,1479909540000,135168\n"
            + "https://www1.ncdc.noaa.gov/pub/data/cmb/ersst/v4/netcdf/,ersst.v4.196604.nc,1479909540000,135168\n"
            + "https://www1.ncdc.noaa.gov/pub/data/cmb/ersst/v4/netcdf/,ersst.v4.196605.nc,1479909540000,135168\n"
            + "https://www1.ncdc.noaa.gov/pub/data/cmb/ersst/v4/netcdf/,ersst.v4.196606.nc,1479909540000,135168\n"
            + "https://www1.ncdc.noaa.gov/pub/data/cmb/ersst/v4/netcdf/,ersst.v4.196607.nc,1479909540000,135168\n"
            + "https://www1.ncdc.noaa.gov/pub/data/cmb/ersst/v4/netcdf/,ersst.v4.196608.nc,1479909540000,135168\n"
            + "https://www1.ncdc.noaa.gov/pub/data/cmb/ersst/v4/netcdf/,ersst.v4.196609.nc,1479909540000,135168\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** This tests oneStepToString(). */
  @org.junit.jupiter.api.Test
  void testOneStepToString() throws Throwable {
    // String2.log("\n*** FileVisitorDNLS.testOneStepToString()");

    String results =
        FileVisitorDNLS.oneStepToString(
            Path.of(
                    FileVisitorDNLSTests.class
                        .getResource("/data/CFPointConventions/timeSeries")
                        .toURI())
                .toString()
                .replace('\\', '/'),
            ".*",
            true,
            ".*",
            false /* addLastModified */);
    String expected =
        // this tests that all files are before all dirs
        "timeSeries-Incomplete-MultiDimensional-MultipleStations-H.2.2.rb         5173\n"
            + "timeSeries-Orthogonal-Multidimenstional-MultipleStations-H.2.1.rb         2979\n"
            + "timeSeries-Incomplete-MultiDimensional-MultipleStations-H.2.2/\n"
            + "  README                                                                   87\n"
            + "  timeSeries-Incomplete-MultiDimensional-MultipleStations-H.2.2.cdl         1793\n"
            + "  timeSeries-Incomplete-MultiDimensional-MultipleStations-H.2.2.nc         4796\n"
            + "  timeSeries-Incomplete-MultiDimensional-MultipleStations-H.2.2.ncml         3026\n"
            + "timeSeries-Orthogonal-Multidimenstional-MultipleStations-H.2.1/\n"
            + "  README                                                                  538\n"
            + "  timeSeries-Orthogonal-Multidimenstional-MultipleStations-H.2.1.cdl         1515\n"
            + "  timeSeries-Orthogonal-Multidimenstional-MultipleStations-H.2.1.nc        10436\n"
            + "  timeSeries-Orthogonal-Multidimenstional-MultipleStations-H.2.1.ncml         2625\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** This tests pathRegex(). */
  @org.junit.jupiter.api.Test
  void testPathRegex() throws Throwable {
    // String2.log("\n*** FileVisitorDNLS.testPathRegex()");
    String dirPath =
        File2.addSlash(
            Path.of(
                    FileVisitorDNLSTests.class
                        .getResource("/data/CFPointConventions/timeSeries")
                        .toURI())
                .toString()
                .replace('\\', '/'));

    Table table =
        FileVisitorDNLS.oneStep(
            dirPath,
            ".*",
            true, // all files
            ".*H\\.2\\.1.*",
            true /* directories */); // but only H.2.1 dirs
    table.removeColumn("lastModified");
    String results = table.toString();
    String expected =
        "{\n"
            + "dimensions:\n"
            + "\trow = 7 ;\n"
            + "\tdirectory_strlen = "
            + (dirPath.length() + 63)
            + " ;\n"
            + "\tname_strlen = 67 ;\n"
            + "variables:\n"
            + "\tchar directory(row, directory_strlen) ;\n"
            + "\tchar name(row, name_strlen) ;\n"
            + "\tlong size(row) ;\n"
            + "\n"
            + "// global attributes:\n"
            + "}\n"
            + "directory,name,size\n"
            + dirPath
            + ",timeSeries-Incomplete-MultiDimensional-MultipleStations-H.2.2.rb,5173\n"
            + dirPath
            + ",timeSeries-Orthogonal-Multidimenstional-MultipleStations-H.2.1.rb,2979\n"
            + dirPath
            + "timeSeries-Orthogonal-Multidimenstional-MultipleStations-H.2.1/,,0\n"
            + dirPath
            + "timeSeries-Orthogonal-Multidimenstional-MultipleStations-H.2.1/,README,538\n"
            + dirPath
            + "timeSeries-Orthogonal-Multidimenstional-MultipleStations-H.2.1/,timeSeries-Orthogonal-Multidimenstional-MultipleStations-H.2.1.cdl,1515\n"
            + dirPath
            + "timeSeries-Orthogonal-Multidimenstional-MultipleStations-H.2.1/,timeSeries-Orthogonal-Multidimenstional-MultipleStations-H.2.1.nc,10436\n"
            + dirPath
            + "timeSeries-Orthogonal-Multidimenstional-MultipleStations-H.2.1/,timeSeries-Orthogonal-Multidimenstional-MultipleStations-H.2.1.ncml,2625\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /**
   * This tests following symbolic links / soft links. THIS DOESN'T WORK on Windows, because Java
   * doesn't follow Windows .lnk's. Windows links are not easily parsed files. It would be hard to
   * add support for .lnk's to this class. This class now works as expected with symbolic links on
   * Linux. I tested manually on coastwatch Linux with FileVisitorDNLS.sh and main() below and
   * ./FileVisitorDNLS.sh /u00/satellite/MUR41/anom/1day/ ....0401.\*
   */
  @org.junit.jupiter.api.Test
  @TagLargeFiles
  void testSymbolicLinks() throws Throwable {
    // String2.log("\n*** FileVisitorDNLS.testSymbolicLinks()");
    // boolean oDebugMode = debugMode;
    // debugMode = true;

    String results =
        FileVisitorDNLS.oneStepToString(
            Path.of(
                    FileVisitorDNLSTests.class
                        .getResource("/veryLarge/satellite/MUR41/anom/1day/")
                        .toURI())
                .toString(),
            ".*",
            true,
            ".*",
            false /* addLastModified */);
    String expected =
        // 2002 are files. 2003 is a shortcut to files
        "2003.lnk                                                         1762\n"
            + //
            "2002\\\n"
            + //
            "  20020601090000-JPL-L4_GHRSST-SSTfndAnom-MUR-GLOB-v02.0-fv04.1.nc 913429216\n"
            + //
            "  20020602090000-JPL-L4_GHRSST-SSTfndAnom-MUR-GLOB-v02.0-fv04.1.nc 913067730\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    // debugMode = oDebugMode;
  }

  /** This tests reduceDnlsTableToOneDir(). */
  @org.junit.jupiter.api.Test
  void testReduceDnlsTableToOneDir() throws Exception {
    // String2.log("\n*** FileVisitorDNLS.testReduceDnlsTableToOneDir\n");
    String tableString =
        "directory, name, lastModified, size\n"
            + "/u00/, , , \n"
            + "/u00/, nothing, 60, 1\n"
            + "/u00/a/, , , \n"
            + "/u00/a/, AA, 60, 2\n"
            + "/u00/a/, A, 60, 3\n"
            + "/u00/a/q/,  ,   , \n"
            + "/u00/a/q/, D, 60, 4\n"
            + "/u00/a/b/, B, 60, 5\n"
            + "/u00/a/b/c/, C, 60, 6\n";
    Table table = new Table();
    table.readASCII(
        "testReduceDnlsTableToOneDir",
        new BufferedReader(new StringReader(tableString)),
        "",
        "",
        0,
        1,
        ",",
        null,
        null,
        null,
        null,
        true);
    String subDirs[] = FileVisitorDNLS.reduceDnlsTableToOneDir(table, "/u00/a/");

    String results = table.dataToString();
    String expected =
        "directory,name,lastModified,size\n"
            + "/u00/a/,A,60,3\n"
            + // files are sorted, dirs are removed
            "/u00/a/,AA,60,2\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = String2.toCSSVString(subDirs);
    expected = "b, q";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** This tests reduceDnlsTableToOneDir(). */
  @org.junit.jupiter.api.Test
  void testReduceDnlsTableToOneDirMisMatchedSeparator() throws Exception {
    // String2.log("\n*** FileVisitorDNLS.testReduceDnlsTableToOneDir\n");
    String tableString =
        "directory, name, lastModified, size\n"
            + "\\\\u00\\\\, , , \n"
            + "\\\\u00\\\\, nothing, 60, 1\n"
            + "\\\\u00\\\\a\\\\, , , \n"
            + "\\\\u00\\\\a\\\\, AA, 60, 2\n"
            + "\\\\u00\\\\a\\\\, A, 60, 3\n"
            + "\\\\u00\\\\a\\\\q\\\\,  ,   , \n"
            + "\\\\u00\\\\a\\\\q\\\\, D, 60, 4\n"
            + "\\\\u00\\\\a\\\\b\\\\, B, 60, 5\n"
            + "\\\\u00\\\\a\\\\b\\\\c\\\\, C, 60, 6\n";
    Table table = new Table();
    table.readASCII(
        "testReduceDnlsTableToOneDir",
        new BufferedReader(new StringReader(tableString)),
        "",
        "",
        0,
        1,
        ",",
        null,
        null,
        null,
        null,
        true);
    String subDirs[] = FileVisitorDNLS.reduceDnlsTableToOneDir(table, "/u00/a/");

    String results = table.dataToString();
    String expected =
        "directory,name,lastModified,size\n"
            + "\\\\u00\\\\a\\\\,A,60,3\n"
            + // files are sorted, dirs are removed
            "\\\\u00\\\\a\\\\,AA,60,2\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = String2.toCSSVString(subDirs);
    expected = "b, q";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** This tests a WAF-related (Web Accessible Folder) methods on an ERDDAP "files" directory. */
  @org.junit.jupiter.api.Test
  @TagMissingFile // DNS Failure 503 (might come back)
  void testInteractiveErddapFilesWAF() throws Throwable {
    String2.log("\n*** FileVisitorDNLS.testInteractiveErddapFilesWAF()\n");
    // test with trailing /
    // This also tests redirect to https!
    String url = "https://coastwatch.pfeg.noaa.gov/erddap/files/fedCalLandings/";
    String tFileNameRegex = "194\\d\\.nc";
    boolean tRecursive = true;
    String tPathRegex = ".*/(3|4)/.*";
    boolean tDirsToo = true;
    Table table = FileVisitorDNLS.makeEmptyTable();
    StringArray dirs = (StringArray) table.getColumn(0);
    StringArray names = (StringArray) table.getColumn(1);
    LongArray lastModifieds = (LongArray) table.getColumn(2);
    LongArray sizes = (LongArray) table.getColumn(3);
    String results, expected;

    // ** Test InPort WAF
    table.removeAllRows();
    // try {
    results =
        FileVisitorDNLS.addToWAFUrlList( // returns a list of errors or ""
            "https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/",
            "22...\\.xml",
            // pre 2016-03-04 I tested NWFSC/inport/xml, but it has been empty for a month!
            true,
            ".*/NMFS/(|NEFSC/(|inport-xml/(|xml/)))", // tricky!
            true, // tDirsToo,
            dirs,
            names,
            lastModifieds,
            sizes);
    Test.ensureEqual(results, "", "results=\n" + results);
    results = table.dataToString();
    expected =
        "directory,name,lastModified,size\n"
            + "https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/,,,\n"
            + "https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport-xml/,,,\n"
            + "https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport-xml/xml/,,,\n"
            + "https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport-xml/xml/,22560.xml,1580850460000,142029\n"
            + // added 2020-03-03
            "https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport-xml/xml/,22561.xml,1554803918000,214016\n"
            + "https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport-xml/xml/,22562.xml,1554803940000,211046\n"
            + "https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport-xml/xml/,22563.xml,1554803962000,213504\n"
            + "https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport-xml/xml/,22564.xml,1554803984000,213094\n"
            + "https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport-xml/xml/,22565.xml,1554804006000,214323\n"
            + "https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport-xml/xml/,22560.xml,1557568922000,213709\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // } catch (Throwable t) {
    // String2.pressEnterToContinue(MustBe.throwableToString(t) + "\n" +
    // "2020-10-22 This now fails because inport has web pages, not a directory
    // system as before.\n" +
    // "Was: This changes periodically. If reasonable, just continue.\n" +
    // "(there no more subtests in this test).");
    // }
  }

  /** This tests sync(). */
  @org.junit.jupiter.api.Test
  void testSync() throws Throwable {
    String2.log("\n*** FileVisitorDNLS.testSync");
    String rDir = "https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/";
    String lDir =
        Path.of(FileVisitorDNLSTests.class.getResource("/data/sync/NMFS").toURI()).toString();
    String fileRegex = "22...\\.xml";
    boolean recursive = true;
    // first part of pathRegex must be the last part of the rDir and the lDir, e.g.,
    // NMFS
    // pre 2016-03-04 I tested NWFSC/inport-xml/xml, but it has been empty for a
    // month!
    String pathRegex = ".*/NMFS/(|NEFSC/)(|inport-xml/)(|xml/)"; // tricky!
    boolean doIt = true;

    // get original times
    String name22560 = lDir + "/NEFSC/inport-xml/xml/22560.xml";
    String name22565 = lDir + "/NEFSC/inport-xml/xml/22565.xml";

    long time22560 = File2.getLastModified(name22560);
    long time22565 = File2.getLastModified(name22565);

    // try {
    // For testing purposes, I put one extra file in the dir: 22561.xml renamed as
    // 22ext.xml

    // do the sync
    FileVisitorDNLS.sync(rDir, lDir, fileRegex, recursive, pathRegex, true);

    // current date on all these files
    // Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(1455948120),
    // "2016-02-20T06:02:00Z", ""); //what the website shows for many

    // delete 1 local file
    File2.delete(lDir + "/NEFSC/inport-xml/xml/22563.xml");

    // make 1 local file older
    File2.setLastModified(name22560, 100);

    // make 1 local file newer
    File2.setLastModified(name22565, System.currentTimeMillis() + 100);
    Math2.sleep(500);

    // test the sync
    Table table = FileVisitorDNLS.sync(rDir, lDir, fileRegex, recursive, pathRegex, doIt);
    // String2.pressEnterToContinue(
    // "2020-10-22 This now fails because inport has web pages, not a directory
    // system as before.\n" +
    // "Was: Check above to ensure these numbers:\n" +
    // "\"found nAlready=3 nAvailDownload=2 nTooRecent=1 nExtraLocal=1\"\n");
    String results = table.dataToString();
    String expected = // the lastModified values change periodically
        // these are the files which were downloaded
        "remote,local,lastModified\n" + "";
    // "https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport-xml/xml/22560.xml,/erddapTest/sync/NMFS/NEFSC/inport-xml/xml/22560.xml,1580850460000\n"
    // +
    // "https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NEFSC/inport-xml/xml/22563.xml,/erddapTest/sync/NMFS/NEFSC/inport-xml/xml/22563.xml,1580850480000\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // no changes, do the sync again
    table = FileVisitorDNLS.sync(rDir, lDir, fileRegex, recursive, pathRegex, doIt);
    // String2.pressEnterToContinue(
    // "2020-10-22 This now fails because inport has web pages, not a directory
    // system as before.\n" +
    // "Check above to ensure these numbers:\n" +
    // "\"found nAlready=5 nToDownload=0 nTooRecent=1 nExtraLocal=1\"\n");
    results = table.dataToString();
    expected = "remote,local,lastModified\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // } catch (Exception e) {
    // String2.pressEnterToContinue(MustBe.throwableToString(e) +
    // "2020-10-22 This now fails because inport has web pages, not a directory
    // system as before.\n" +
    // "Was: Often, there are minor differences.");

    // } finally {
    File2.setLastModified(name22560, time22560);
    File2.setLastModified(name22565, time22565);
    // }

  }

  /** This tests makeTgz. */
  @org.junit.jupiter.api.Test
  void testMakeTgz() throws Exception {
    String2.log("\n*** FileVisitorDNLS.testMakeTgz");

    String dataDir =
        Path.of(FileVisitorDNLSTests.class.getResource("/data/fileNames").toURI()).toString();

    String tgzName =
        Path.of(FileVisitorDNLSTests.class.getResource("/data/").toURI()).toString()
            + "/testMakeTgz.tar.gz";
    // try {
    FileVisitorDNLS.makeTgz(dataDir, ".*", true, ".*", tgzName);
    // Test.displayInBrowser("file://" + tgzName); //works with .tar.gz, not .tgz
    // String2.pressEnterToContinue("Are the contents of the .tar.gz file okay?");
    // } catch (Throwable t) {
    // String2.pressEnterToContinue(MustBe.throwableToString(t));
    // }
    File2.delete(tgzName);
  }
}
