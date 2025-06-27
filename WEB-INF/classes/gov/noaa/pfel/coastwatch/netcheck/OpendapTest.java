/*
 * OpendapTest Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.netcheck;

import com.cohort.util.Calendar2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import dods.dap.*;
import gov.noaa.pfel.coastwatch.griddata.Opendap;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import java.util.GregorianCalendar;

/**
 * This deals with one type of netCheck test: the ability to get das and dds information and actual
 * data from Opendap.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-08-16
 */
public class OpendapTest extends NetCheckTest {

  // required
  private String url;
  private String variableName;
  private String missingValue;
  private GregorianCalendar offsetDate;
  private double[] minMaxXY;

  // optional
  private String dasMustContain;
  private String ddsMustContain;
  private int gridNLonValues = -1; // -1 = not set
  private int gridNLatValues = -1;

  /**
   * This constructor loads the information for a test with information from the xmlReader. The
   * xmlReader.getNextTag() should have just returned with the initial reference to this class. The
   * method will read information until xmlReader.getNextTag() returns the close tag for the
   * reference to this class.
   *
   * @param xmlReader
   * @throws Exception if trouble
   */
  public OpendapTest(SimpleXMLReader xmlReader) throws Exception {
    String errorIn = String2.ERROR + " in OpendapTest constructor: ";

    // ensure the xmlReader is just starting with this class
    Test.ensureEqual(
        xmlReader.allTags(), "<netCheck><opendapTest>", errorIn + "incorrect initial tags.");

    // read the xml properties file
    xmlReader.nextTag();
    String tags = xmlReader.allTags();
    int iteration = 0;
    while (!tags.equals("<netCheck></opendapTest>") && iteration++ < 1000000) {
      // process the tags
      if (verbose) String2.log(tags + xmlReader.content());
      switch (tags) {
        case "<netCheck><opendapTest><title>",
            "<netCheck><opendapTest><emailChangeHeadlinesTo>",
            "<netCheck><opendapTest><emailChangesTo>",
            "<netCheck><opendapTest><emailStatusHeadlinesTo>",
            "<netCheck><opendapTest><emailStatusTo>",
            "<netCheck><opendapTest><mustRespondWithinSeconds>",
            "<netCheck><opendapTest><gridNLatValues>",
            "<netCheck><opendapTest><gridNLonValues>",
            "<netCheck><opendapTest><ddsMustContain>",
            "<netCheck><opendapTest><dasMustContain>",
            "<netCheck><opendapTest><minMaxXY>",
            "<netCheck><opendapTest><offsetDate>",
            "<netCheck><opendapTest><missingValue>",
            "<netCheck><opendapTest><variableName>",
            "<netCheck><opendapTest><url>" -> {}
        case "<netCheck><opendapTest></title>" -> title = xmlReader.content();
        case "<netCheck><opendapTest></url>" -> url = xmlReader.content();
        case "<netCheck><opendapTest></variableName>" -> variableName = xmlReader.content();
        case "<netCheck><opendapTest></missingValue>" -> missingValue = xmlReader.content();
        case "<netCheck><opendapTest></offsetDate>" ->
            offsetDate =
                Calendar2.parseISODateTimeZulu(xmlReader.content()); // throws Exception if trouble
        case "<netCheck><opendapTest></minMaxXY>" ->
            minMaxXY = String2.csvToDoubleArray(xmlReader.content());
        case "<netCheck><opendapTest></dasMustContain>" -> dasMustContain = xmlReader.content();
        case "<netCheck><opendapTest></ddsMustContain>" -> ddsMustContain = xmlReader.content();
        case "<netCheck><opendapTest></gridNLonValues>" ->
            gridNLonValues = String2.parseInt(xmlReader.content());
        case "<netCheck><opendapTest></gridNLatValues>" ->
            gridNLatValues = String2.parseInt(xmlReader.content());
        case "<netCheck><opendapTest></mustRespondWithinSeconds>" ->
            mustRespondWithinSeconds = String2.parseDouble(xmlReader.content());
        case "<netCheck><opendapTest></emailStatusTo>" -> emailStatusTo.add(xmlReader.content());
        case "<netCheck><opendapTest></emailStatusHeadlinesTo>" ->
            emailStatusHeadlinesTo.add(xmlReader.content());
        case "<netCheck><opendapTest></emailChangesTo>" -> emailChangesTo.add(xmlReader.content());
        case "<netCheck><opendapTest></emailChangeHeadlinesTo>" ->
            emailChangeHeadlinesTo.add(xmlReader.content());
        default -> throw new RuntimeException(errorIn + "unrecognized tags: " + tags);
      }

      // get the next tags
      xmlReader.nextTag();
      tags = xmlReader.allTags();
    }

    // ensure that the required values are set
    ensureValid();
  }

  /**
   * A constructor for setting up the test. You can set other values with their setXxx or addXxx
   * methods of the superclass NetCheckTest.
   *
   * @param title e.g., THREDDS Opendap GAssta
   * @param url e.g.,
   * @param variableName e.g., ssta
   * @param missingValue e.g., -1e32
   * @param isoOffsetDate e.g., 1970-01-01
   * @param minMaxXY {minX, maxX, minY, maxY} e.g., {0.0, 360.0, -75.0, 75.0}
   * @param dasMustContain a String that the das must contain (use null to not do this test)
   * @param ddsMustContain a String that the das must contain (use null to not do this test)
   * @param gridNLonValues the expected number of Lon values (use -1 to not test this)
   * @param gridNLatValues the expected number of Lat values (use -1 to not test this)
   * @throws Exception if trouble
   */
  public OpendapTest(
      String title,
      String url,
      String variableName,
      String missingValue,
      String isoOffsetDate,
      double minMaxXY[],
      String dasMustContain,
      String ddsMustContain,
      int gridNLonValues,
      int gridNLatValues)
      throws Exception {

    // required
    this.title = title;
    this.url = url;
    this.variableName = variableName;
    this.missingValue = missingValue;
    offsetDate = Calendar2.parseISODateTimeZulu(isoOffsetDate); // throws Exception if trouble
    this.minMaxXY = minMaxXY;

    // optional
    this.dasMustContain = dasMustContain;
    this.ddsMustContain = ddsMustContain;
    this.gridNLonValues = gridNLonValues;
    this.gridNLatValues = gridNLatValues;

    // ensure that the required values are set
    ensureValid();
  }

  /**
   * This is used by the constructors to ensure that all the required values were set.
   *
   * @throws Exception if trouble
   */
  public void ensureValid() throws Exception {
    String errorIn = String2.ERROR + " in OpendapTest.ensureValid: ";

    // ensure that required items were set
    Test.ensureTrue(
        title != null && title.length() > 0,
        errorIn + "<netCheck><opendapTest><title> was not specified.\n");
    Test.ensureTrue(
        url != null && url.length() > 0,
        errorIn + "<netCheck><opendapTest><url> was not specified.\n");
    Test.ensureTrue(
        variableName != null && variableName.length() > 0,
        errorIn + "<netCheck><opendapTest><variableName> was not specified.\n");
    Test.ensureTrue(
        missingValue != null && missingValue.length() > 0,
        errorIn + "<netCheck><opendapTest><missingValue> was not specified.\n");
    Test.ensureTrue(
        offsetDate != null, errorIn + "<netCheck><opendapTest><offsetDate> was not specified.\n");
    Test.ensureTrue(
        minMaxXY != null && minMaxXY.length == 4,
        errorIn
            + "<netCheck><opendapTest><minMaxXY> with 4 comma-separated doubles was not specified.\n");
  }

  /**
   * This does the test and returns an error string ("" if no error). This does not send out emails.
   * This won't throw an Exception.
   *
   * @return an error string ("" if no error). If there is an error, this will end with '\n'. If the
   *     error has a short section followed by a longer section, NetCheckTest.END_SHORT_SECTION will
   *     separate the two sections.
   */
  @Override
  public String test() {
    try {
      long time = System.currentTimeMillis();

      // open the dataSet; getTimeOptions, makeGrid
      Opendap opendap = new Opendap(url, true, null); // acceptDeflate, resetFlagDir
      DConnect dConnect = new DConnect(opendap.url, opendap.acceptDeflate, 1, 1);
      DAS das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
      DDS dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);
      opendap.getGridInfo(das, dds, variableName, missingValue);
      opendap.getTimeOptions(
          false, // false = format as date time
          opendap.gridTimeFactorToGetSeconds,
          opendap.gridTimeBaseSeconds,
          0); // 0 not accurate here, but doesn't matter for opendapTest

      // makeGrid throws exception if trouble
      opendap.makeGrid(
          opendap.timeOptions[0], minMaxXY[0], minMaxXY[1], minMaxXY[2], minMaxXY[3], 200, 200);
      time = System.currentTimeMillis() - time;

      // check mustRespondWithinSeconds
      // String2.log("opendap time=" + time);
      StringBuilder errorSB = new StringBuilder();
      if (Double.isFinite(mustRespondWithinSeconds) && time > mustRespondWithinSeconds * 1000) {
        errorSB.append(
            "  "
                + String2.ERROR
                + ": response time ("
                + (time / 1000.0)
                + " s) was too slow (mustRespondWithinSeconds = "
                + mustRespondWithinSeconds
                + ").\n");
      }

      // check dasMustContain
      if (dasMustContain != null && dasMustContain.length() > 0) {
        String dasString = OpendapHelper.getDasString(das);
        if (dasString.indexOf(dasMustContain) < 0) {
          errorSB.append(
              "  "
                  + String2.ERROR
                  + ": dasMustContain ("
                  + dasMustContain
                  + ") wasn't found in das:\n"
                  + dasString);
        }
      }

      // check ddsMustContain
      if (ddsMustContain != null && ddsMustContain.length() > 0) {
        String ddsString = OpendapHelper.getDdsString(dds);
        if (ddsString.indexOf(ddsMustContain) < 0) {
          errorSB.append(
              "  "
                  + String2.ERROR
                  + ": ddsMustContain ("
                  + ddsMustContain
                  + ") wasn't found in dds:\n"
                  + ddsString);
        }
      }

      // check gridNLonValues
      if (gridNLonValues >= 0 && opendap.gridNLonValues != gridNLonValues) {
        errorSB.append(
            "  "
                + String2.ERROR
                + ": opendap.gridNLonValues ("
                + opendap.gridNLonValues
                + ") wasn't the expected value ("
                + gridNLonValues
                + ").\n");
      }

      // check gridNLatValues
      if (gridNLatValues >= 0 && opendap.gridNLatValues != gridNLatValues) {
        errorSB.append(
            "  "
                + String2.ERROR
                + ": opendap.gridNLatValues ("
                + opendap.gridNLatValues
                + ") wasn't the expected value ("
                + gridNLatValues
                + ").\n");
      }

      // if there was trouble, include info (at the start) of the error message
      if (errorSB.length() > 0) {
        errorSB.insert(0, getDescription());
      }

      return errorSB.toString();
    } catch (Exception e) {

      return MustBe.throwable("opendapTest.test\n" + getDescription(), e);
    }
  }

  /**
   * This returns a description of this test (suitable for putting at the top of an error message),
   * with " " at the start and \n at the end.
   *
   * @return a description
   */
  @Override
  public String getDescription() {
    return "  url: "
        + url
        + "\n"
        + "  variableName: "
        + variableName
        + "\n"
        + "  missingValue: "
        + missingValue
        + "\n"
        + "  offsetDate: "
        + Calendar2.formatAsISODate(offsetDate)
        + "\n"
        + "  minMaxXY: "
        + String2.toCSSVString(minMaxXY)
        + "\n";
  }

  /**
   * A unit test of this class.
   *
   * @throws Exception if trouble
   */
  public static void unitTest() throws Exception {

    verbose = true;
    Opendap.verbose = verbose;
    long time;
    OpendapTest opendapTest;
    String error;

    // test of THREDDS opendap  AGssta 3day
    String2.log("\n*** netcheck.OpendapTest THREDDS AGssta 3day");
    time = System.currentTimeMillis();
    opendapTest =
        new OpendapTest(
            "THREDDS OPeNDAP AGssta", // </title>
            "https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/AG/ssta/3day", // </url>
            "AGssta", // </variableName>
            "-1.0e32", // </missingValue>
            "1970-01-01", // </offsetDate>
            new double[] {0, 360, -75, 75}, // </minMaxXY>
            "EARTH SCIENCE > Oceans > Ocean Temperature > Sea Surface Temperature", // </dasMustContain>
            "Float32 AGssta", // </ddsMustContain>
            3601, // </gridNLonValues>
            1501); // </gridNLatValues>
    if (verbose) String2.log(opendapTest.getDescription());
    error = opendapTest.test();
    Test.ensureEqual(error, "", String2.ERROR + " in OpendapTest.unitTest:\n" + error);
    String2.log(
        "netcheck.OpendapTest THREDDS AGssta 3day finished successfully   time="
            + (System.currentTimeMillis() - time)
            + "ms");

    // test of THREDDS opendap  CMusfc hday
    String2.log("\n*** netcheck.OpendapTest THREDDS CMusfc hday");
    time = System.currentTimeMillis();
    opendapTest =
        new OpendapTest(
            "THREDDS OPeNDAP CMusfc", // </title>
            "https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/CM/usfc/hday", // </url>
            "CMusfc", // </variableName>
            "-1.0e32", // </missingValue>
            "1970-01-01", // </offsetDate>
            new double[] {237.6455, 238.2335, 36.5449, 37.0399}, // </minMaxXY>
            "EARTH SCIENCE > Oceans > Ocean Circulation > Ocean Currents", // </dasMustContain>
            "Float32 CMusfc", // </ddsMustContain>
            44, // </gridNLonValues>
            26); // </gridNLatValues>
    if (verbose) String2.log(opendapTest.getDescription());
    error = opendapTest.test();
    Test.ensureEqual(error, "", String2.ERROR + " in OpendapTest.unitTest:\n" + error);
    String2.log(
        "netcheck.OpendapTest THREDDS CMusfc hday finished successfully   time="
            + (System.currentTimeMillis() - time)
            + "ms");

    // test of THREDDS opendap  GAssta hday
    String2.log("\n*** netcheck.OpendapTest THREDDS GAssta hday");
    time = System.currentTimeMillis();
    opendapTest =
        new OpendapTest(
            "THREDDS OPeNDAP GAssta", // </title>
            "https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/GA/ssta/hday", // </url>
            "GAssta", // </variableName>
            "-1.0e32", // </missingValue>
            "1970-01-01", // </offsetDate>
            new double[] {215.0, 255.0, 22.0, 51.0}, // </minMaxXY>
            "EARTH SCIENCE > Oceans > Ocean Temperature > Sea Surface Temperature", // </dasMustContain>
            "Float32 GAssta", // </ddsMustContain>
            3000, // </gridNLonValues>
            2100); // </gridNLatValues>
    if (verbose) String2.log(opendapTest.getDescription());
    error = opendapTest.test();
    Test.ensureEqual(error, "", String2.ERROR + " in OpendapTest.unitTest:\n" + error);
    String2.log(
        "netcheck.OpendapTest THREDDS GAssta hday finished successfully   time="
            + (System.currentTimeMillis() - time)
            + "ms");

    // test of THREDDS opendap MBchla 1day
    String2.log("\n*** netcheck.OpendapTest THREDDS MBchla 1day");
    time = System.currentTimeMillis();
    opendapTest =
        new OpendapTest(
            "THREDDS OPeNDAP MBchla", // </title>
            "https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MB/chla/1day", // </url>
            "MBchla", // </variableName>
            "-1.0e32", // </missingValue>
            "1970-01-01", // </offsetDate>
            new double[] {120.0, 320.0, -45.0, 65.0}, // </minMaxXY>
            "EARTH SCIENCE > Oceans > Ocean Chemistry > Chlorophyll", // </dasMustContain>
            "Float32 MBchla", // </ddsMustContain>
            8001, // </gridNLonValues>
            4401); // </gridNLatValues>
    if (verbose) String2.log(opendapTest.getDescription());
    error = opendapTest.test();
    Test.ensureEqual(error, "", String2.ERROR + " in OpendapTest.unitTest:\n" + error);
    String2.log(
        "netcheck.OpendapTest THREDDS MBchla 1day finished successfully   time="
            + (System.currentTimeMillis() - time)
            + "ms");

    // test of THREDDS opendap QScurl 8day
    String2.log("\n*** netcheck.OpendapTest THREDDS QScurl 8day");
    time = System.currentTimeMillis();
    opendapTest =
        new OpendapTest(
            "THREDDS OPeNDAP QScurl", // </title>
            "https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/curl/8day", // </url>
            "QScurl", // </variableName>
            "-1.0e32", // </missingValue>
            "1970-01-01", // </offsetDate>
            new double[] {0.0, 360.0, -75.0, 75.0}, // </minMaxXY>
            "EARTH SCIENCE > Oceans > Ocean Winds > Wind Stress", // </dasMustContain>
            "Float32 QScurl", // </ddsMustContain>
            2881, // </gridNLonValues>
            1201); // </gridNLatValues>
    if (verbose) String2.log(opendapTest.getDescription());
    error = opendapTest.test();
    Test.ensureEqual(error, "", String2.ERROR + " in OpendapTest.unitTest:\n" + error);
    String2.log(
        "netcheck.OpendapTest THREDDS QScurl 8day finished successfully   time="
            + (System.currentTimeMillis() - time)
            + "ms");
    /* */

  }
}
