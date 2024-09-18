package gov.noaa.pfel.coastwatch.util;

import com.cohort.array.StringArray;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.junit.jupiter.api.BeforeAll;
import tags.TagAWS;
import tags.TagExternalOther;
import testDataset.Initialization;

class FileVisitorSubdirTests {

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /** This tests a local file system. */
  @org.junit.jupiter.api.Test
  void testLocal() throws Throwable {
    // String2.log("\n*** FileVisitorSubdir.testLocal");
    // verbose = true;
    String contextDir =
        EDStatic.getWebInfParentDirectory().replace('\\', '/'); // with / separator and / at the end
    StringArray alps;
    long time;

    // test forward slashes
    alps =
        FileVisitorSubdir.oneStep(
            contextDir + "WEB-INF/classes/com/cohort", null); // without trailing slash
    alps.sort(); // sort is required before test comparison in Linux
    String results = alps.toNewlineString();
    String expected =
        contextDir
            + "WEB-INF/classes/com/cohort/\n"
            + contextDir
            + "WEB-INF/classes/com/cohort/array/\n"
            + contextDir
            + "WEB-INF/classes/com/cohort/util/\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test backslashes
    // alps = FileVisitorSubdir.oneStep(
    //     String2.replaceAll(contextDir + "WEB-INF/classes/com/cohort/", '/', '\\'), // with
    // trailing slash
    //     null);
    // results = alps.toNewlineString();
    // expected = String2.replaceAll(expected, '/', '\\');
    // Test.ensureEqual(results, expected, "results=\n" + results);

    String2.log("\n*** FileVisitorSubdir.testLocal finished.");
  }

  /**
   * This tests an Amazon AWS S3 file system. Your S3 credentials must be in <br>
   * ~/.aws/credentials on Linux, OS X, or Unix <br>
   * C:\Users\USERNAME\.aws\credentials on Windows See
   * https://docs.aws.amazon.com/sdk-for-java/?id=docs_gateway#aws-sdk-for-java,-version-1 .
   */
  @org.junit.jupiter.api.Test
  @TagAWS
  void testAWSS3() throws Throwable {
    // String2.log("\n*** FileVisitorSubdir.testAWSS3");

    // verbose = true;
    StringArray alps;
    long time;

    alps =
        FileVisitorSubdir.oneStep(
            "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/",
            "");
    String results = alps.toNewlineString();
    String expected =
        "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/\n"
            + "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    String2.log("\n*** FileVisitorSubdir.testAWSS3 finished.");
  }

  /** This tests a WAF and pathRegex. */
  @TagExternalOther
  @org.junit.jupiter.api.Test
  void testWAF() throws Throwable {
    // String2.log("\n*** FileVisitorSubdir.testWAF");

    // verbose = true;
    StringArray alps;
    long time;
    try {

      alps =
          FileVisitorSubdir.oneStep(
              "https://www.fisheries.noaa.gov/inportserve/waf/", // after 2020-08-03
              // "https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/", //pre 2020-08-03
              ".*/NMFS/(|SWFSC/|NWFSC/)(|inport-xml/)(|xml/)"); // tricky!
      String results = alps.toNewlineString();
      String expected =
          "https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NWFSC/\n"
              + "https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NWFSC/inport-xml/\n"
              + "https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/NWFSC/inport-xml/xml/\n"
              + "https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/SWFSC/\n"
              + "https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/SWFSC/inport-xml/\n"
              + "https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/SWFSC/inport-xml/xml/\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      String2.log("\n*** FileVisitorSubdir.testWAF finished.");
    } catch (Exception e) {
      // Test.knownProblem(
      // "2020-08-03 New 'directory' in new InPort system isn't a directory but a web
      // page with other info.", e);
    }
  }
}
