package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
import com.cohort.array.PAOne;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Image2Tests;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDVGridAxis;
import gov.noaa.pfel.erddap.variable.EDVTimeGridAxis;
import gov.noaa.pfel.erddap.variable.EDVTimeStampGridAxis;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.io.Writer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tags.TagExternalERDDAP;
import tags.TagExternalOther;
import tags.TagImageComparison;
import tags.TagIncompleteTest;
import tags.TagMissingDataset;
import tags.TagSlowTests;
import tags.TagThredds;
import testDataset.EDDTestDataset;
import testDataset.Initialization;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;

class EDDGridFromDapTests {
  @BeforeAll
  static void init() throws Exception {
    Initialization.edStatic();
    // Several tests rely on the development/test/datasets.xml file, so make sure it is generated.
    EDDTestDataset.generateDatasetsXml();
  }

  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testForCarleton() throws Throwable {
    // test for Charles Carleton .nc request failed; others ok
    // testVerboseOn();
    int language = 0;
    EDDGrid gridDataset = (EDDGridFromDap) EDDTestDataset.getNCOM_Region7_2D();
    String tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "surf_el[(2008-06-12T00:00:00):1:(2008-06-12T00:00:00)][(10.0):100:(65.0)][(-150.0):100:(-100.0)]",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Carleton",
            ".nc");
    String results = NcHelper.ncdump(EDStatic.fullTestCacheDirectory + tName, "");
    String expected =
        "time =\n"
            + "  {1.2132288E9}\n"
            + "latitude =\n"
            + "  {10.0, 22.5, 35.0, 47.5, 60.0}\n"
            + "longitude =\n"
            + "  {-150.0, -137.5, -125.0, -112.5, -100.0}\n"
            + "surf_el =\n"
            + "  {\n"
            + "    {\n"
            + "      {0.314, 0.203, 0.505, 0.495, 0.317},\n"
            + "      {0.646, 0.6, 0.621, 0.547, -30.0},\n"
            + "      {0.326, 0.487, 0.589, -30.0, -30.0},\n"
            + "      {-0.34400000000000003, -0.044, 0.318, -30.0, -30.0},\n"
            + "      {-30.0, -30.0, -30.0, -30.0, -30.0}\n"
            + "    }\n"
            + "  }\n"
            + "}\n";
    Test.ensureTrue(results.endsWith(expected), "RESULTS=\n" + results);
  }

  @org.junit.jupiter.api.Test
  @TagMissingDataset
  void testForDave() throws Throwable {
    // testVerboseOn();
    int language = 0;

    // tests for Dave works, but datasets not always active
    // the cwAM datasets are gone; see active noaa_coastwatch_... datasets
    String tid = "cwAMchlaD1";
    // String tid = "cwAMchlaD61";
    // String tid = "cwAMchlaG";
    // String tid = "cwAMchlaAnG";
    EDDGrid gridDataset = (EDDGridFromDap) EDDGridFromDap.oneFromDatasetsXml(null, tid);

    String2.log("\n\n***** DDS");
    String tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_" + tid,
            ".dds");
    String2.log(File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName));

    String2.log("\n\n***** DAS ");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_" + tid,
            ".das");
    String2.log(File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName));

    String2.log("\n\n***** NCDUMP ");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "chlor_a[(2008-03-29T12:00:00):1:(2008-03-29T12:00:00)][(0.0):1:(0.0)][(16.995124378128825):100:(31.00905181853181)][(-99.01235553141787):100:(-78.99636386525192)]",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_" + tid,
            ".nc");
    String2.log(NcHelper.ncdump(EDStatic.fullTestCacheDirectory + tName, ""));

    String2.log("\n\n***** PNG ");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "chlor_a[0][][][]&.colorBar=Rainbow|C|Log|.04|10|",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_" + tid + "_Map",
            ".png");
    Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
  }

  @org.junit.jupiter.api.Test
  @TagThredds
  void testBasic1() throws Throwable {
    String2.log("\n*** EDDGridFromDap.testBasic1\n");
    // testVerboseOn();
    int language = 0;
    EDDGridFromDap gridDataset;
    String name, tName, axisDapQuery, results, expected, error;
    int tPo;
    String userDapQuery = "chlorophyll[(2007-02-06)][][(29):10:(50)][(225):10:(247)]";
    String graphDapQuery = "chlorophyll[0:10:200][][(29)][(225)]";
    String mapDapQuery = "chlorophyll[200][][(29):(50)][(225):(247)]"; // stride irrelevant
    StringArray destinationNames = new StringArray();
    IntArray constraints = new IntArray();

    gridDataset = (EDDGridFromDap) EDDTestDataset.geterdMHchla8day();

    /* */
    // just comment out to work on some other test

    // test regex for integer >=0
    Test.ensureTrue("0".matches("[0-9]+"), "");
    Test.ensureTrue("123".matches("[0-9]+"), "");
    Test.ensureTrue(!"-1".matches("[0-9]+"), "");
    Test.ensureTrue(!"2.3".matches("[0-9]+"), "");

    // test that bad metadata was removed
    // String2.log(
    // "\n\naddAtt=" + gridDataset.addGlobalAttributes() +
    // "\n\ncombinedAtt=" + gridDataset.combinedGlobalAttributes());
    Test.ensureEqual(gridDataset.combinedGlobalAttributes().getString("et_affine"), null, "");
    Test.ensureEqual(
        gridDataset.dataVariables()[0].combinedAttributes().getString("percentCoverage"), null, "");

    // *** test parseQuery with invalid queries
    error = "";
    try {
      gridDataset.parseDataDapQuery(0, "zztop", destinationNames, constraints, false);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(
        String2.split(error, '\n')[0],
        "SimpleException: Error: destinationVariableName=zztop wasn't found in datasetID=erdMHchla8day.",
        "error=" + error);

    error = "";
    try {
      gridDataset.parseDataDapQuery(0, "chlorophyll[][][]", destinationNames, constraints, false);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(
        String2.split(error, '\n')[0],
        "SimpleException: Query error: For variable=chlorophyll axis#3=longitude: "
            + "\"[\" was expected at or after position=17, not [end of query].",
        "error=" + error);

    error = "";
    try {
      gridDataset.parseDataDapQuery(
          0,
          "chlorophyll[(2007-02-06)[][(29):10:(50)][(225):10:(247)]",
          destinationNames,
          constraints,
          false);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureLinesMatch(
        String2.split(error, '\n')[0], // last # changes frequently. Was 500.
        "SimpleException: Query error: For variable=chlorophyll axis#0=time "
            + "Constraint=\"\\[\\(2007-02-06\\)\\[\\]\": Stop=\"\" is invalid\\.  "
            + "It must be an integer between 0 and \\d{3}\\.",
        "error=" + error);

    // invalid date format 2014-10-02
    // but 2007-2-06 is allowed as of 2018-05-17.
    error = "";
    try {
      gridDataset.parseDataDapQuery(
          0,
          "chlorophyll[(2007-002-06):(2007-02-06)][][(29):10:(50)][(225):10:(247)]",
          destinationNames,
          constraints,
          false);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(
        String2.split(error, '\n')[0],
        "SimpleException: Query error: For variable=chlorophyll axis#0=time "
            + "Constraint=\"[(2007-002-06):(2007-02-06)]\": "
            + "Start=NaN (invalid format?) isn't allowed.",
        "error=" + error);

    // invalid date format 2014-10-02
    error = "";
    try {
      gridDataset.parseDataDapQuery(
          0,
          "chlorophyll[(2007-02-06):(2007-002-06)][][(29):10:(50)][(225):10:(247)]",
          destinationNames,
          constraints,
          false);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(
        String2.split(error, '\n')[0],
        "SimpleException: Query error: For variable=chlorophyll axis#0=time "
            + "Constraint=\"[(2007-02-06):(2007-002-06)]\": "
            + "Stop=NaN (invalid format?) isn't allowed.",
        "error=" + error);

    // extra dimension 2014-10-02
    error = "";
    try {
      gridDataset.parseDataDapQuery(
          0,
          "chlorophyll[(2007-02-06)][][(29):10:(50)][(225):10:(247)][somethingElse]",
          destinationNames,
          constraints,
          false);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(
        String2.split(error, '\n')[0],
        "SimpleException: Query error: \",\" or \"[end of query]\" was expected "
            + "at or after position=57, not \"[\".",
        "error=" + error);

    error = "";
    try {
      gridDataset.parseAxisDapQuery(0, "zztop", destinationNames, constraints, false);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(
        String2.split(error, '\n')[0],
        "SimpleException: Error: variableName=zztop wasn't found in datasetID=erdMHchla8day.",
        "error=" + error);

    error = "";
    try {
      gridDataset.parseAxisDapQuery(
          0, "latitude,chlorophyll", destinationNames, constraints, false);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(
        String2.split(error, '\n')[0],
        "SimpleException: Query error: A griddap axis variable query can't "
            + "include a data variable (chlorophyll).",
        "error=" + error);

    error = "";
    try {
      gridDataset.parseDataDapQuery(
          0, "chlorophyll,latitude", destinationNames, constraints, false);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(
        String2.split(error, '\n')[0],
        "SimpleException: Query error: A griddap data variable query can't "
            + "include an axis variable (latitude).",
        "error=" + error);

    error = "";
    try {
      gridDataset.parseAxisDapQuery(0, "latitude[", destinationNames, constraints, false);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(
        String2.split(error, '\n')[0],
        "SimpleException: Query error: For variable=latitude axis#2=latitude: "
            + "\"]\" was not found after position=8.",
        "error=" + error);

    // test error message from dataset that doesn't load
    // EDDGrid tGrid = (EDDGrid)oneFromDatasetsXml(null, "erdAGtanm3day"); //should
    // fail
    // String2.log("tGrid==null = " + (tGrid == null));
    // if (true) System.exit(0);

    // *** test valid parseDataDapQuery
    String iso = Calendar2.epochSecondsToIsoStringTZ(1.0260864E9);
    Test.ensureTrue(!gridDataset.isAxisDapQuery(userDapQuery), "");
    gridDataset.parseDataDapQuery(0, userDapQuery, destinationNames, constraints, false);
    Test.ensureEqual(destinationNames.toString(), "chlorophyll", "");
    // pre 2009-09-09 was different. Based on other changes: Dave must have
    // regridded the dataset
    Test.ensureEqual(
        constraints.toString(), "206, 1, 206, 0, 1, 0, 2855, 10, 3359, 5399, 10, 5927", "");

    String tDapQuery = "chlorophyll[(2007-02-06)][][(29):10:(50)][last:1:last]"; // test last
    gridDataset.parseDataDapQuery(0, tDapQuery, destinationNames, constraints, false);
    Test.ensureEqual(destinationNames.toString(), "chlorophyll", "");
    Test.ensureEqual(
        constraints.toString(), "206, 1, 206, 0, 1, 0, 2855, 10, 3359, 8639, 1, 8639", "");

    tDapQuery = "chlorophyll[(2007-02-06T12:00:00)][][(29):10:(50)][last]"; // test colons
    gridDataset.parseDataDapQuery(0, tDapQuery, destinationNames, constraints, false);
    Test.ensureEqual(destinationNames.toString(), "chlorophyll", "");
    Test.ensureEqual(
        constraints.toString(), "206, 1, 206, 0, 1, 0, 2855, 10, 3359, 8639, 1, 8639", "");

    Test.ensureTrue(gridDataset.isAxisDapQuery("time"), "");
    gridDataset.parseAxisDapQuery(0, "time", destinationNames, constraints, false);
    Test.ensureEqual(destinationNames.toString(), "time", "");
    // Test.ensureEqual(constraints.toString(), "0, 1, 331", ""); //this will
    // increase once in a while

    Test.ensureTrue(gridDataset.isAxisDapQuery("time["), "");
    gridDataset.parseAxisDapQuery(0, "time[(2007-02-06)]", destinationNames, constraints, false);
    Test.ensureEqual(destinationNames.toString(), "time", "");
    Test.ensureEqual(constraints.toString(), "206, 1, 206", "");

    gridDataset.parseAxisDapQuery(
        0, "longitude[ last : 1 : last ]", destinationNames, constraints, false);
    Test.ensureEqual(destinationNames.toString(), "longitude", "");
    Test.ensureEqual(constraints.toString(), "8639, 1, 8639", "");

    gridDataset.parseAxisDapQuery(0, "longitude[ last ]", destinationNames, constraints, false);
    Test.ensureEqual(destinationNames.toString(), "longitude", "");
    Test.ensureEqual(constraints.toString(), "8639, 1, 8639", "");

    gridDataset.parseAxisDapQuery(0, "longitude[ last - 20]", destinationNames, constraints, false);
    Test.ensureEqual(constraints.toString(), "8619, 1, 8619", "");

    gridDataset.parseAxisDapQuery(0, "longitude[last+-20]", destinationNames, constraints, false);
    Test.ensureEqual(constraints.toString(), "8619, 1, 8619", "");

    gridDataset.parseAxisDapQuery(
        0,
        "time[20:10:(2007-02-06)],altitude,longitude[last]",
        destinationNames,
        constraints,
        false);
    Test.ensureEqual(destinationNames.toNewlineString(), "time\naltitude\nlongitude\n", "");
    Test.ensureEqual(constraints.toString(), "20, 10, 206, 0, 1, 0, 8639, 1, 8639", "");

    // lon: incr=0.04166667 n=8640
    gridDataset.parseAxisDapQuery(
        0, "longitude[(last-0.4166)]", destinationNames, constraints, false);
    Test.ensureEqual(constraints.toString(), "8629, 1, 8629", "");

    gridDataset.parseAxisDapQuery(
        0, "longitude[(last+-0.4166)]", destinationNames, constraints, false);
    Test.ensureEqual(constraints.toString(), "8629, 1, 8629", "");

    // time: incr=8days n=272 16days=16*86400=1382400
    gridDataset.parseAxisDapQuery(0, "time[(last-1382400)]", destinationNames, constraints, false);
    // Test.ensureEqual(constraints.toString(), "329, 1, 329", ""); //changes
    // sometimes

    gridDataset.parseAxisDapQuery(0, "time[(last+-1382400)]", destinationNames, constraints, false);
    // Test.ensureEqual(constraints.toString(), "329, 1, 329", ""); //changes
    // sometimes

    error = "";
    try {
      gridDataset.parseAxisDapQuery(0, "latitude[last-2.0]", destinationNames, constraints, false);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureTrue(
        error.indexOf(
                "SimpleException: Query error: The +/- index "
                    + "value in Start=last-2.0 isn't an integer.")
            >= 0,
        "error=" + error);

    error = "";
    try {
      gridDataset.parseAxisDapQuery(
          0, "latitude[(last-2.0a)]", destinationNames, constraints, false);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureTrue(
        error.indexOf(
                "SimpleException: Query error: The +/- "
                    + "value in Start=(last-2.0a) isn't valid.")
            >= 0,
        "error=" + error);

    error = "";
    try {
      gridDataset.parseAxisDapQuery(0, "latitude[(last*2)]", destinationNames, constraints, false);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureTrue(
        error.indexOf(
                "SimpleException: Query error: Unexpected "
                    + "character after \"last\" in Start=(last*2).")
            >= 0,
        "error=" + error);

    // ***test some edvga things
    EDVGridAxis edvga = gridDataset.axisVariables()[0];
    Test.ensureEqual(edvga.isEvenlySpaced(), false, "");
    // Test.ensureEqual(edvga.averageSpacing(), 700858.0060422961, ""); //changes
    // sometimes
    // Test.ensureEqual(edvga.spacingDescription(0), "8 days 02:46:30 (uneven)",
    // ""); //changes sometimes

    edvga = gridDataset.axisVariables()[1];
    Test.ensureEqual(edvga.isEvenlySpaced(), true, "");
    Test.ensureEqual(edvga.averageSpacing(), Double.NaN, "");
    Test.ensureEqual(edvga.spacingDescription(0), "(just one value)", "");

    edvga = gridDataset.axisVariables()[2];
    Test.ensureEqual(edvga.isEvenlySpaced(), true, "");
    Test.ensureEqual(edvga.averageSpacing(), 0.041676313961565174, ""); // not ideal, but true
    Test.ensureEqual(edvga.spacingDescription(0), "0.04167631 (even)", "");

    // *** test dapInstructions
    // StringWriter sw = new StringWriter();
    // writeGeneralDapHtmlDocument(language, EDStatic.erddapUrl, sw); //for testing,
    // always non-https url
    // results = sw.toString();
    // String2.log(results);
    // expected = "Requests for Gridded Data in ";
    // Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    // expected = "In ERDDAP, time variables always have the name \"" +
    // EDV.TIME_NAME + "\"";
    // Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);

    // *** test getting das for entire dataset
    String2.log("\n****************** EDDGridFromDap test entire dataset\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Entire",
            ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        // "Attributes {\n" +
        // " time {\n" +
        // " Float64 actual_range 1.1886912e+9, 1.1886912e+9;\n"; //this will change
        // sometimes
        "    String axis \"T\";\n"
            + "    Int32 fraction_digits 0;\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Centered Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  altitude {\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);

    expected = // test that _FillValue and missing_value are as in sourceAtts
        // but complicated, because that's the value my Grid class uses.
        "  chlorophyll {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float64 colorBarMaximum 30.0;\n"
            + "    Float64 colorBarMinimum 0.03;\n"
            + "    String colorBarScale \"Log\";\n"
            + "    String coordsys \"geographic\";\n"
            + "    Int32 fraction_digits 2;\n"
            + "    String ioos_category \"Ocean Color\";\n"
            + "    String long_name \"Concentration Of Chlorophyll In Sea Water\";\n"
            + "    Float32 missing_value -9999999.0;\n"
            + "    String standard_name \"concentration_of_chlorophyll_in_sea_water\";\n"
            + "    String units \"mg m-3\";\n"
            + "  }\n"
            + "  NC_GLOBAL {\n"
            + "    String acknowledgement \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n"
            + "    String cdm_data_type \"Grid\";\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);

    // *** test getting dds for entire dataset
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Entire",
            ".dds");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "Dataset \\{\n"
            + "  Float64 time\\[time = \\d{3}\\];\n"
            + // \\d was 500. It changes sometimes.
            "  Float64 altitude\\[altitude = 1\\];\n"
            + "  Float64 latitude\\[latitude = 4320\\];\n"
            + "  Float64 longitude\\[longitude = 8640\\];\n"
            + "  GRID \\{\n"
            + "    ARRAY:\n"
            + "      Float32 chlorophyll\\[time = \\d{3}\\]\\[altitude = 1\\]\\[latitude = 4320\\]\\[longitude = 8640\\];\n"
            + "    MAPS:\n"
            + "      Float64 time\\[time = \\d{3}\\];\n"
            + "      Float64 altitude\\[altitude = 1\\];\n"
            + "      Float64 latitude\\[latitude = 4320\\];\n"
            + "      Float64 longitude\\[longitude = 8640\\];\n"
            + "  \\} chlorophyll;\n"
            + "\\} erdMHchla8day;\n";
    Test.ensureLinesMatch(results, expected, "\nresults=\n" + results);

    // *** test DAP data access form
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Entire",
            ".html");
    // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

    // *** test getting das for 1 variable das isn't affected by userDapQuery
    String2.log("\n****************** EDDGridFromDap test 1 variable\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "chlorophyll",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_1Variable",
            ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        // "Attributes {\n" +
        // " time {\n" +
        // " Float64 actual_range 1.17072e+9, 1.17072e+9;\n" + //changes sometimes
        "    String axis \"T\";\n"
            + "    Int32 fraction_digits 0;\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Centered Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  altitude {\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    expected =
        "  NC_GLOBAL {\n"
            + "    String acknowledgement \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n"
            + "    String cdm_data_type \"Grid\";\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);

    // *** test getting dds for 1 variable
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "chlorophyll",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_1Variable",
            ".dds");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "Dataset \\{\n"
            + "  GRID \\{\n"
            + "    ARRAY:\n"
            + // \\d{3} was 500. It changes sometimes.
            "      Float32 chlorophyll\\[time = \\d{3}\\]\\[altitude = 1\\]\\[latitude = 4320\\]\\[longitude = 8640\\];\n"
            + "    MAPS:\n"
            + "      Float64 time\\[time = \\d{3}\\];\n"
            + "      Float64 altitude\\[altitude = 1\\];\n"
            + "      Float64 latitude\\[latitude = 4320\\];\n"
            + "      Float64 longitude\\[longitude = 8640\\];\n"
            + "  } chlorophyll;\n"
            + "} erdMHchla8day;\n";
    Test.ensureLinesMatch(results, expected, "\nresults=\n" + results);
  }

  @org.junit.jupiter.api.Test
  @TagThredds
  void testBasic2() throws Throwable {
    String2.log("\n*** EDDGridFromDap.testBasic2\n");
    // testVerboseOn();
    EDDGridFromDap gridDataset;
    String name, tName, axisDapQuery, results, expected, error;
    int tPo;
    String userDapQuery = "chlorophyll[(2007-02-06)][][(29):10:(50)][(225):10:(247)]";
    String graphDapQuery = "chlorophyll[0:10:200][][(29)][(225)]";
    String mapDapQuery = "chlorophyll[200][][(29):(50)][(225):(247)]"; // stride irrelevant
    StringArray destinationNames = new StringArray();
    IntArray constraints = new IntArray();
    int language = 0;

    gridDataset = (EDDGridFromDap) EDDTestDataset.geterdMHchla8day();
    // just comment out to work on some other test

    // ********************************************** test getting axis data

    // .asc
    String2.log("\n*** EDDGridFromDap test get .ASC axis data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "time[0:100:200],longitude[last]",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Axis",
            ".asc");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + "  Float64 time[time = 3];\n"
            + "  Float64 longitude[longitude = 1];\n"
            + "} erdMHchla8day;\n"
            + "---------------------------------------------\n"
            + "Data:\n"
            + "time[3]\n"
            + "1.0260864E9, 1.0960704E9, 1.1661408E9\n"
            + "longitude[1]\n"
            + "360.0\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    // .csv
    String2.log("\n*** EDDGridFromDap test get .csv axis data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "time[0:100:200],longitude[last]",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Axis",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    expected =
        "time,longitude\n"
            + "UTC,degrees_east\n"
            + "2002-07-08T00:00:00Z,360.0\n"
            + "2004-09-25T00:00:00Z,NaN\n"
            + "2006-12-15T00:00:00Z,NaN\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    // .csvp
    String2.log("\n*** EDDGridFromDap test get .csvp axis data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "time[0:100:200],longitude[last]",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Axis",
            ".csvp");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    expected =
        "time (UTC),longitude (degrees_east)\n"
            + "2002-07-08T00:00:00Z,360.0\n"
            + "2004-09-25T00:00:00Z,NaN\n"
            + "2006-12-15T00:00:00Z,NaN\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    // .csv test of gridName.axisName notation
    String2.log("\n*** EDDGridFromDap test get .CSV axis data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "chlorophyll.time[0:100:200],chlorophyll.longitude[last]",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_AxisG.A",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    expected =
        "time,longitude\n"
            + "UTC,degrees_east\n"
            + "2002-07-08T00:00:00Z,360.0\n"
            + "2004-09-25T00:00:00Z,NaN\n"
            + "2006-12-15T00:00:00Z,NaN\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    // .das which disregards userDapQuery
    String2.log("\n*** EDDGridFromDap test get .DAS axis data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "time[0:100:200],longitude[last]",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Axis",
            ".das");
    results =
        String2.annotatedString(
            new String(ByteArray.fromFile(EDStatic.fullTestCacheDirectory + tName).toArray()));
    expected = // see OpendapHelper.EOL definition for comments
        // "Attributes {[10]\n" +
        // " time {[10]\n" +
        // " Float64 actual_range 1.17072e+9, 1.17072e+9;[10]\n" + //this will change
        // sometimes
        "    String axis \"T\";[10]\n"
            + "    Int32 fraction_digits 0;[10]\n"
            + "    String ioos_category \"Time\";[10]\n"
            + "    String long_name \"Centered Time\";[10]\n"
            + "    String standard_name \"time\";[10]\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";[10]\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";[10]\n"
            + "  }[10]\n"
            + "  altitude {[10]\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    expected =
        "  NC_GLOBAL {[10]\n"
            + "    String acknowledgement \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";[10]\n"
            + "    String cdm_data_type \"Grid\";[10]\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);

    // .dds
    String2.log("\n*** EDDGridFromDap test get .DDS axis data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "time[0:100:200],longitude[last]",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Axis",
            ".dds");
    results =
        String2.annotatedString(
            File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName));
    // String2.log(results);
    expected =
        "Dataset {[10]\n"
            + "  Float64 time[time = 3];[10]\n"
            + "  Float64 longitude[longitude = 1];[10]\n"
            + "} erdMHchla8day;[10]\n"
            + "[end]";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    // .dods
    String2.log("\n*** EDDGridFromDap test get .DODS axis data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "time[0:100:200],longitude[last]",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Axis",
            ".dods");
    results =
        String2.annotatedString(
            File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName));
    // String2.log(results);
    expected =
        "Dataset {[10]\n"
            + "  Float64 time[time = 3];[10]\n"
            + "  Float64 longitude[longitude = 1];[10]\n"
            + "} erdMHchla8day;[10]\n"
            + "[10]\n"
            + "Data:[10]\n"
            + "[0][0][0][3][0][0][0][3]A[206][148]k[0][0][0][0]A[208]U-@[0][0][0]A[209]`y`[0][0][0][0][0][0][1][0][0][0][1]@v[128][0][0][0][0][0][end]";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    // .json
    String2.log("\n*** EDDGridFromDap test get .JSON axis data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "time[0:100:200],longitude[last]",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Axis",
            ".json");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    expected =
        "{\n"
            + "  \"table\": {\n"
            + "    \"columnNames\": [\"time\", \"longitude\"],\n"
            + "    \"columnTypes\": [\"String\", \"double\"],\n"
            + "    \"columnUnits\": [\"UTC\", \"degrees_east\"],\n"
            + "    \"rows\": [\n"
            + "      [\"2002-07-08T00:00:00Z\", 360],\n"
            + // pre 2018-03-17 was 360.0
            "      [\"2004-09-25T00:00:00Z\", null],\n"
            + "      [\"2006-12-15T00:00:00Z\", null]\n"
            + "    ]\n"
            + "  }\n"
            + "}\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // .json with jsonp
    String2.log("\n*** EDDGridFromDap test get .JSON axis data (with jsonp)\n");
    String jsonp = "myFunctionName";
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "time[0:100:200],longitude[last]" + "&.jsonp=" + SSR.percentEncode(jsonp),
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Axis",
            ".json");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    expected =
        jsonp
            + "("
            + "{\n"
            + "  \"table\": {\n"
            + "    \"columnNames\": [\"time\", \"longitude\"],\n"
            + "    \"columnTypes\": [\"String\", \"double\"],\n"
            + "    \"columnUnits\": [\"UTC\", \"degrees_east\"],\n"
            + "    \"rows\": [\n"
            + "      [\"2002-07-08T00:00:00Z\", 360],\n"
            + // pre 2018-03-17 was 360.0
            "      [\"2004-09-25T00:00:00Z\", null],\n"
            + "      [\"2006-12-15T00:00:00Z\", null]\n"
            + "    ]\n"
            + "  }\n"
            + "}\n"
            + ")";
    Test.ensureEqual(results, expected, "results=\n" + results);
    /* */
    // .mat
    // octave> load('c:/temp/griddap/EDDGridFromDap_Axis.mat');
    // octave> erdMHchla8day
    String matlabAxisQuery = "time[0:100:200],longitude[last]";
    String2.log("\n*** EDDGridFromDap test get .MAT axis data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            matlabAxisQuery,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Axis",
            ".mat");
    String2.log(".mat test file is " + EDStatic.fullTestCacheDirectory + tName);
    results = File2.hexDump(EDStatic.fullTestCacheDirectory + tName, 1000000);
    String2.log(results);
    expected =
        "4d 41 54 4c 41 42 20 35   2e 30 20 4d 41 54 2d 66   MATLAB 5.0 MAT-f |\n"
            + "69 6c 65 2c 20 43 72 65   61 74 65 64 20 62 79 3a   ile, Created by: |\n"
            + "20 67 6f 76 2e 6e 6f 61   61 2e 70 66 65 6c 2e 63    gov.noaa.pfel.c |\n"
            + "6f 61 73 74 77 61 74 63   68 2e 4d 61 74 6c 61 62   oastwatch.Matlab |\n"
            +
            // "2c 20 43 72 65 61 74 65 64 20 6f 6e 3a 20 54 75 , Created on: Tu |\n" +
            // "65 20 4f 63 74 20 31 34 20 30 38 3a 35 36 3a 35 e Oct 14 08:56:5 |\n" +
            // "34 20 32 30 30 38 20 20 20 20 20 20 20 20 20 20 4 2008 |\n" +
            "20 20 20 20 00 00 00 00   00 00 00 00 01 00 4d 49                 MI |\n"
            + "00 00 00 0e 00 00 01 18   00 00 00 06 00 00 00 08                    |\n"
            + "00 00 00 02 00 00 00 00   00 00 00 05 00 00 00 08                    |\n"
            + "00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 0d                    |\n"
            + "65 72 64 4d 48 63 68 6c   61 38 64 61 79 00 00 00   erdMHchla8day    |\n"
            + "00 04 00 05 00 00 00 20   00 00 00 01 00 00 00 40                  @ |\n"
            + "74 69 6d 65 00 00 00 00   00 00 00 00 00 00 00 00   time             |\n"
            + "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            + "6c 6f 6e 67 69 74 75 64   65 00 00 00 00 00 00 00   longitude        |\n"
            + "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            + "00 00 00 0e 00 00 00 48   00 00 00 06 00 00 00 08          H         |\n"
            + "00 00 00 06 00 00 00 00   00 00 00 05 00 00 00 08                    |\n"
            + "00 00 00 03 00 00 00 01   00 00 00 01 00 00 00 00                    |\n"
            + "00 00 00 09 00 00 00 18   41 ce 94 6b 00 00 00 00           A  k     |\n"
            + "41 d0 55 2d 40 00 00 00   41 d1 60 79 60 00 00 00   A U-@   A `y`    |\n"
            + "00 00 00 0e 00 00 00 38   00 00 00 06 00 00 00 08          8         |\n"
            + "00 00 00 06 00 00 00 00   00 00 00 05 00 00 00 08                    |\n"
            + "00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 00                    |\n"
            + "00 00 00 09 00 00 00 08   40 76 80 00 00 00 00 00           @v       |\n";
    Test.ensureEqual(
        results.substring(0, 71 * 4) + results.substring(71 * 7), // remove the creation
        // dateTime
        expected,
        "RESULTS(" + EDStatic.fullTestCacheDirectory + tName + ")=\n" + results);
    /* */
    // .nc
    String2.log("\n*** EDDGridFromDap test get .nc axis data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "time[0:100:200],longitude[last]",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Axis",
            ".nc");
    results = NcHelper.ncdump(EDStatic.fullTestCacheDirectory + tName, "");
    expected =
        "netcdf EDDGridFromDap_Axis.nc {\n"
            + "  dimensions:\n"
            + "    time = 3;\n"
            + // (has coord.var)\n" + //changed when switched to netcdf-java 4.0,
            // 2009-02-23
            "    longitude = 1;\n"
            + // (has coord.var)\n" +
            "  variables:\n"
            + "    double time(time=3);\n"
            + "      :_CoordinateAxisType = \"Time\";\n"
            + "      :actual_range = 1.0260864E9, 1.1661408E9; // double\n"
            + // up-to-date
            "      :axis = \"T\";\n"
            + "      :fraction_digits = 0; // int\n"
            + "      :ioos_category = \"Time\";\n"
            + "      :long_name = \"Centered Time\";\n"
            + "      :standard_name = \"time\";\n"
            + "      :time_origin = \"01-JAN-1970 00:00:00\";\n"
            + "      :units = \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "\n"
            + "    double longitude(longitude=1);\n"
            + "      :_CoordinateAxisType = \"Lon\";\n"
            + "      :actual_range = 360.0, 360.0; // double\n"
            + "      :axis = \"X\";\n"
            + "      :coordsys = \"geographic\";\n"
            + "      :fraction_digits = 4; // int\n"
            + "      :ioos_category = \"Location\";\n"
            + "      :long_name = \"Longitude\";\n"
            + "      :point_spacing = \"even\";\n"
            + "      :standard_name = \"longitude\";\n"
            + "      :units = \"degrees_east\";\n"
            + "\n"
            + "  // global attributes:\n"
            + "  :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n"
            + "  :cdm_data_type = \"Grid\";\n"
            + "  :composite = \"true\";\n"
            + "  :contributor_name = \"NASA GSFC (OBPG)\";\n"
            + "  :contributor_role = \"Source of level 2 data.\";\n"
            + "  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "  :creator_email = \"erd.data@noaa.gov\";\n"
            + "  :creator_name = \"NOAA NMFS SWFSC ERD\";\n"
            + "  :creator_type = \"institution\";\n"
            + "  :creator_url = \"https://www.pfeg.noaa.gov\";\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);
    expected =
        "  :infoUrl = \"https://coastwatch.pfeg.noaa.gov/infog/MH_chla_las.html\";\n"
            + "  :institution = \"NOAA NMFS SWFSC ERD\";\n"
            + "  :keywords = \"8-day, aqua, chemistry, chlorophyll, chlorophyll-a, coastwatch, color, concentration, concentration_of_chlorophyll_in_sea_water, day, degrees, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, global, modis, noaa, npp, ocean, ocean color, oceans, quality, science, science quality, sea, seawater, water, wcn\";\n"
            + "  :keywords_vocabulary = \"GCMD Science Keywords\";\n"
            + "  :license = \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "  :naming_authority = \"gov.noaa.pfeg.coastwatch\";\n"
            + "  :origin = \"NASA GSFC (OBPG)\";\n"
            + "  :processing_level = \"3\";\n"
            + "  :project = \"CoastWatch (https://coastwatch.noaa.gov/)\";\n"
            + "  :projection = \"geographic\";\n"
            + "  :projection_type = \"mapped\";\n"
            + "  :publisher_email = \"erd.data@noaa.gov\";\n"
            + "  :publisher_name = \"NOAA NMFS SWFSC ERD\";\n"
            + "  :publisher_type = \"institution\";\n"
            + "  :publisher_url = \"https://www.pfeg.noaa.gov\";\n"
            + "  :references = \"Aqua/MODIS information: https://oceancolor.gsfc.nasa.gov/ . MODIS information: https://coastwatch.noaa.gov/modis_ocolor_overview.html .\";\n"
            + "  :satellite = \"Aqua\";\n"
            + "  :sensor = \"MODIS\";\n"
            + "  :source = \"satellite observation: Aqua, MODIS\";\n"
            + "  :sourceUrl = \"https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day\";\n"
            + "  :standard_name_vocabulary = \"CF Standard Name Table v70\";\n"
            + "  :summary = \"NOAA CoastWatch distributes chlorophyll-a concentration data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.   This is Science Quality data.\";\n"
            + "  :time_coverage_end = \"2006-12-15T00:00:00Z\";\n"
            + "  :time_coverage_start = \"2002-07-08T00:00:00Z\";\n"
            + "  :title = \"Chlorophyll-a, Aqua MODIS, NPP, 2002-2013, DEPRECATED OLDER VERSION (8 Day Composite)\";\n"
            + "  :Westernmost_Easting = 360.0; // double\n"
            + "\n"
            + "  data:\n"
            + "    time = \n"
            + "      {1.0260864E9, 1.0960704E9, 1.1661408E9}\n"
            + "    longitude = \n"
            + "      {360.0}\n"
            + "}\n";
    tPo = results.indexOf("  :infoUrl");
    Test.ensureEqual(results.substring(tPo), expected, "RESULTS=\n" + results);

    // .ncHeader
    String2.log("\n*** EDDGridFromDap test get .NCHEADER axis data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "time[0:100:200],longitude[last]",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Axis",
            ".ncHeader");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "netcdf EDDGridFromDap_Axis.nc {\n"
            + "  dimensions:\n"
            + "    time = 3;\n"
            + // (has coord.var)\n" + //changed when switched to netcdf-java 4.0,
            // 2009-02-23
            "    longitude = 1;\n"
            + // (has coord.var)\n" +
            "  variables:\n"
            + "    double time(time=3);\n"
            + "      :_CoordinateAxisType = \"Time\";\n"
            + "      :actual_range = 1.0260864E9, 1.1661408E9; // double\n"
            + // up-to-date
            "      :axis = \"T\";\n"
            + "      :fraction_digits = 0; // int\n"
            + "      :ioos_category = \"Time\";\n"
            + "      :long_name = \"Centered Time\";\n"
            + "      :standard_name = \"time\";\n"
            + "      :time_origin = \"01-JAN-1970 00:00:00\";\n"
            + "      :units = \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "\n"
            + "    double longitude(longitude=1);\n"
            + "      :_CoordinateAxisType = \"Lon\";\n"
            + "      :actual_range = 360.0, 360.0; // double\n"
            + "      :axis = \"X\";\n"
            + "      :coordsys = \"geographic\";\n"
            + "      :fraction_digits = 4; // int\n"
            + "      :ioos_category = \"Location\";\n"
            + "      :long_name = \"Longitude\";\n"
            + "      :point_spacing = \"even\";\n"
            + "      :standard_name = \"longitude\";\n"
            + "      :units = \"degrees_east\";\n"
            + "\n"
            + "  // global attributes:\n"
            + "  :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);
    expected =
        "  :title = \"Chlorophyll-a, Aqua MODIS, NPP, 2002-2013, DEPRECATED OLDER VERSION (8 Day Composite)\";\n"
            + "  :Westernmost_Easting = 360.0; // double\n"
            + "}\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "RESULTS=\n" + results);

    // .ncoJson
    String2.log("\n*** EDDGridFromDap test get .ncoJson axis data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "time[0:100:200],longitude[last]",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Axis",
            ".ncoJson");
    // 2017-08-03 I tested the resulting file for validity at https://jsonlint.com/
    String2.log(">> NCO JSON " + EDStatic.fullTestCacheDirectory + tName);
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    expected =
        "{\n"
            + "  \"attributes\": {\n"
            + "    \"acknowledgement\": {\"type\": \"char\", \"data\": \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\"},\n"
            + "    \"cdm_data_type\": {\"type\": \"char\", \"data\": \"Grid\"},\n"
            + "    \"composite\": {\"type\": \"char\", \"data\": \"true\"},\n"
            + "    \"contributor_name\": {\"type\": \"char\", \"data\": \"NASA GSFC (OBPG)\"},\n"
            + "    \"contributor_role\": {\"type\": \"char\", \"data\": \"Source of level 2 data.\"},\n"
            + "    \"Conventions\": {\"type\": \"char\", \"data\": \"COARDS, CF-1.6, ACDD-1.3\"},\n"
            + "    \"creator_email\": {\"type\": \"char\", \"data\": \"erd.data@noaa.gov\"},\n"
            + "    \"creator_name\": {\"type\": \"char\", \"data\": \"NOAA NMFS SWFSC ERD\"},\n"
            + "    \"creator_type\": {\"type\": \"char\", \"data\": \"institution\"},\n"
            + "    \"creator_url\": {\"type\": \"char\", \"data\": \"https://www.pfeg.noaa.gov\"},\n"
            + "    \"date_created\": {\"type\": \"char\", \"data\": \"2013-11-01\"},\n"
            + "    \"date_issued\": {\"type\": \"char\", \"data\": \"2013-11-01\"},\n"
            + "    \"Easternmost_Easting\": {\"type\": \"double\", \"data\": 360.0},\n"
            + "    \"geospatial_lon_max\": {\"type\": \"double\", \"data\": 360.0},\n"
            + "    \"geospatial_lon_min\": {\"type\": \"double\", \"data\": 360.0},\n"
            + "    \"geospatial_lon_units\": {\"type\": \"char\", \"data\": \"degrees_east\"},\n"
            + "    \"history\": {\"type\": \"char\", \"data\": \"NASA GSFC (OBPG)\\n2013-11-01T20:42:40Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD\\n";
    // 2017-07-31T20:10:46Z
    // https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day\\n2017-07-31T20:10:46Z
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    expected =
        "http://127.0.0.1:8080/cwexperimental/griddap/erdMHchla8day.ncoJson?time[0:100:200],longitude[last]\"},\n"
            + "    \"infoUrl\": {\"type\": \"char\", \"data\": \"https://coastwatch.pfeg.noaa.gov/infog/MH_chla_las.html\"},\n"
            + "    \"institution\": {\"type\": \"char\", \"data\": \"NOAA NMFS SWFSC ERD\"},\n"
            + "    \"keywords\": {\"type\": \"char\", \"data\": \"8-day, aqua, chemistry, chlorophyll, chlorophyll-a, coastwatch, color, concentration, concentration_of_chlorophyll_in_sea_water, day, degrees, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, global, modis, noaa, npp, ocean, ocean color, oceans, quality, science, science quality, sea, seawater, water, wcn\"},\n"
            + "    \"keywords_vocabulary\": {\"type\": \"char\", \"data\": \"GCMD Science Keywords\"},\n"
            + "    \"license\": {\"type\": \"char\", \"data\": \"The data may be used and redistributed for free but is not intended\\nfor legal use, since it may contain inaccuracies. Neither the data\\nContributor, ERD, NOAA, nor the United States Government, nor any\\nof their employees or contractors, makes any warranty, express or\\nimplied, including warranties of merchantability and fitness for a\\nparticular purpose, or assumes any legal liability for the accuracy,\\ncompleteness, or usefulness, of this information.\"},\n"
            + "    \"naming_authority\": {\"type\": \"char\", \"data\": \"gov.noaa.pfeg.coastwatch\"},\n"
            + "    \"origin\": {\"type\": \"char\", \"data\": \"NASA GSFC (OBPG)\"},\n"
            + "    \"processing_level\": {\"type\": \"char\", \"data\": \"3\"},\n"
            + "    \"project\": {\"type\": \"char\", \"data\": \"CoastWatch (https://coastwatch.noaa.gov/)\"},\n"
            + "    \"projection\": {\"type\": \"char\", \"data\": \"geographic\"},\n"
            + "    \"projection_type\": {\"type\": \"char\", \"data\": \"mapped\"},\n"
            + "    \"publisher_email\": {\"type\": \"char\", \"data\": \"erd.data@noaa.gov\"},\n"
            + "    \"publisher_name\": {\"type\": \"char\", \"data\": \"NOAA NMFS SWFSC ERD\"},\n"
            + "    \"publisher_type\": {\"type\": \"char\", \"data\": \"institution\"},\n"
            + "    \"publisher_url\": {\"type\": \"char\", \"data\": \"https://www.pfeg.noaa.gov\"},\n"
            + "    \"references\": {\"type\": \"char\", \"data\": \"Aqua/MODIS information: https://oceancolor.gsfc.nasa.gov/ . MODIS information: https://coastwatch.noaa.gov/modis_ocolor_overview.html .\"},\n"
            + "    \"satellite\": {\"type\": \"char\", \"data\": \"Aqua\"},\n"
            + "    \"sensor\": {\"type\": \"char\", \"data\": \"MODIS\"},\n"
            + "    \"source\": {\"type\": \"char\", \"data\": \"satellite observation: Aqua, MODIS\"},\n"
            + "    \"sourceUrl\": {\"type\": \"char\", \"data\": \"https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day\"},\n"
            + "    \"standard_name_vocabulary\": {\"type\": \"char\", \"data\": \"CF Standard Name Table v70\"},\n"
            + "    \"summary\": {\"type\": \"char\", \"data\": \"NOAA CoastWatch distributes chlorophyll-a concentration data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.   This is Science Quality data.\"},\n"
            + "    \"time_coverage_end\": {\"type\": \"char\", \"data\": \"2006-12-15T00:00:00Z\"},\n"
            + "    \"time_coverage_start\": {\"type\": \"char\", \"data\": \"2002-07-08T00:00:00Z\"},\n"
            + "    \"title\": {\"type\": \"char\", \"data\": \"Chlorophyll-a, Aqua MODIS, NPP, 2002-2013, DEPRECATED OLDER VERSION (8 Day Composite)\"},\n"
            + "    \"Westernmost_Easting\": {\"type\": \"double\", \"data\": 360.0}\n"
            + "  },\n"
            + "  \"dimensions\": {\n"
            + "    \"time\": 3,\n"
            + "    \"longitude\": 1\n"
            + "  },\n"
            + "  \"variables\": {\n"
            + "    \"time\": {\n"
            + "      \"shape\": [\"time\"],\n"
            + "      \"type\": \"double\",\n"
            + "      \"attributes\": {\n"
            + "        \"_CoordinateAxisType\": {\"type\": \"char\", \"data\": \"Time\"},\n"
            + "        \"actual_range\": {\"type\": \"double\", \"data\": [1.0260864E9, 1.1661408E9]},\n"
            + "        \"axis\": {\"type\": \"char\", \"data\": \"T\"},\n"
            + "        \"fraction_digits\": {\"type\": \"int\", \"data\": 0},\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Time\"},\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Centered Time\"},\n"
            + "        \"standard_name\": {\"type\": \"char\", \"data\": \"time\"},\n"
            + "        \"time_origin\": {\"type\": \"char\", \"data\": \"01-JAN-1970 00:00:00\"},\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"seconds since 1970-01-01T00:00:00Z\"}\n"
            + "      },\n"
            + "      \"data\": [1.0260864E9, 1.0960704E9, 1.1661408E9]\n"
            + "    },\n"
            + "    \"longitude\": {\n"
            + "      \"shape\": [\"longitude\"],\n"
            + "      \"type\": \"double\",\n"
            + "      \"attributes\": {\n"
            + "        \"_CoordinateAxisType\": {\"type\": \"char\", \"data\": \"Lon\"},\n"
            + "        \"actual_range\": {\"type\": \"double\", \"data\": [360.0, 360.0]},\n"
            + "        \"axis\": {\"type\": \"char\", \"data\": \"X\"},\n"
            + "        \"coordsys\": {\"type\": \"char\", \"data\": \"geographic\"},\n"
            + "        \"fraction_digits\": {\"type\": \"int\", \"data\": 4},\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Location\"},\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Longitude\"},\n"
            + "        \"point_spacing\": {\"type\": \"char\", \"data\": \"even\"},\n"
            + "        \"standard_name\": {\"type\": \"char\", \"data\": \"longitude\"},\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"degrees_east\"}\n"
            + "      },\n"
            + "      \"data\": [360.0]\n"
            + "    }\n"
            + "  }\n"
            + "}\n";
    tPo = results.indexOf(expected.substring(0, 40));
    if (tPo < 0) String2.log("results=\n" + results + "\ntPo=" + tPo);
    Test.ensureEqual(results.substring(tPo), expected, "results=\n" + results + "\ntPo=" + tPo);

    // .ncoJson with jsonp
    String2.log("\n*** EDDGridFromDap test get .ncoJson axis data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "time[0:100:200],longitude[last]&.jsonp=myFunctionName",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Axisjp",
            ".ncoJson");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    expected =
        "myFunctionName("
            + "{\n"
            + "  \"attributes\": {\n"
            + "    \"acknowledgement\": {\"type\": \"char\", \"data\": \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\"},\n"
            + "    \"cdm_data_type\": {\"type\": \"char\", \"data\": \"Grid\"},\n"
            + "    \"composite\": {\"type\": \"char\", \"data\": \"true\"},\n"
            + "    \"contributor_name\": {\"type\": \"char\", \"data\": \"NASA GSFC (OBPG)\"},\n"
            + "    \"contributor_role\": {\"type\": \"char\", \"data\": \"Source of level 2 data.\"},\n"
            + "    \"Conventions\": {\"type\": \"char\", \"data\": \"COARDS, CF-1.6, ACDD-1.3\"},\n"
            + "    \"creator_email\": {\"type\": \"char\", \"data\": \"erd.data@noaa.gov\"},\n"
            + "    \"creator_name\": {\"type\": \"char\", \"data\": \"NOAA NMFS SWFSC ERD\"},\n"
            + "    \"creator_type\": {\"type\": \"char\", \"data\": \"institution\"},\n"
            + "    \"creator_url\": {\"type\": \"char\", \"data\": \"https://www.pfeg.noaa.gov\"},\n"
            + "    \"date_created\": {\"type\": \"char\", \"data\": \"2013-11-01\"},\n"
            + "    \"date_issued\": {\"type\": \"char\", \"data\": \"2013-11-01\"},\n"
            + "    \"Easternmost_Easting\": {\"type\": \"double\", \"data\": 360.0},\n"
            + "    \"geospatial_lon_max\": {\"type\": \"double\", \"data\": 360.0},\n"
            + "    \"geospatial_lon_min\": {\"type\": \"double\", \"data\": 360.0},\n"
            + "    \"geospatial_lon_units\": {\"type\": \"char\", \"data\": \"degrees_east\"},\n"
            + "    \"history\": {\"type\": \"char\", \"data\": \"NASA GSFC (OBPG)\\n2013-11-01T20:42:40Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD\\n";
    // 2017-07-31T20:10:46Z
    // https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day\\n2017-07-31T20:10:46Z
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    expected =
        "http://127.0.0.1:8080/cwexperimental/griddap/erdMHchla8day.ncoJson?time[0:100:200],longitude[last]&.jsonp=myFunctionName\"},\n"
            + "    \"infoUrl\": {\"type\": \"char\", \"data\": \"https://coastwatch.pfeg.noaa.gov/infog/MH_chla_las.html\"},\n"
            + "    \"institution\": {\"type\": \"char\", \"data\": \"NOAA NMFS SWFSC ERD\"},\n"
            + "    \"keywords\": {\"type\": \"char\", \"data\": \"8-day, aqua, chemistry, chlorophyll, chlorophyll-a, coastwatch, color, concentration, concentration_of_chlorophyll_in_sea_water, day, degrees, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, global, modis, noaa, npp, ocean, ocean color, oceans, quality, science, science quality, sea, seawater, water, wcn\"},\n"
            + "    \"keywords_vocabulary\": {\"type\": \"char\", \"data\": \"GCMD Science Keywords\"},\n"
            + "    \"license\": {\"type\": \"char\", \"data\": \"The data may be used and redistributed for free but is not intended\\nfor legal use, since it may contain inaccuracies. Neither the data\\nContributor, ERD, NOAA, nor the United States Government, nor any\\nof their employees or contractors, makes any warranty, express or\\nimplied, including warranties of merchantability and fitness for a\\nparticular purpose, or assumes any legal liability for the accuracy,\\ncompleteness, or usefulness, of this information.\"},\n"
            + "    \"naming_authority\": {\"type\": \"char\", \"data\": \"gov.noaa.pfeg.coastwatch\"},\n"
            + "    \"origin\": {\"type\": \"char\", \"data\": \"NASA GSFC (OBPG)\"},\n"
            + "    \"processing_level\": {\"type\": \"char\", \"data\": \"3\"},\n"
            + "    \"project\": {\"type\": \"char\", \"data\": \"CoastWatch (https://coastwatch.noaa.gov/)\"},\n"
            + "    \"projection\": {\"type\": \"char\", \"data\": \"geographic\"},\n"
            + "    \"projection_type\": {\"type\": \"char\", \"data\": \"mapped\"},\n"
            + "    \"publisher_email\": {\"type\": \"char\", \"data\": \"erd.data@noaa.gov\"},\n"
            + "    \"publisher_name\": {\"type\": \"char\", \"data\": \"NOAA NMFS SWFSC ERD\"},\n"
            + "    \"publisher_type\": {\"type\": \"char\", \"data\": \"institution\"},\n"
            + "    \"publisher_url\": {\"type\": \"char\", \"data\": \"https://www.pfeg.noaa.gov\"},\n"
            + "    \"references\": {\"type\": \"char\", \"data\": \"Aqua/MODIS information: https://oceancolor.gsfc.nasa.gov/ . MODIS information: https://coastwatch.noaa.gov/modis_ocolor_overview.html .\"},\n"
            + "    \"satellite\": {\"type\": \"char\", \"data\": \"Aqua\"},\n"
            + "    \"sensor\": {\"type\": \"char\", \"data\": \"MODIS\"},\n"
            + "    \"source\": {\"type\": \"char\", \"data\": \"satellite observation: Aqua, MODIS\"},\n"
            + "    \"sourceUrl\": {\"type\": \"char\", \"data\": \"https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day\"},\n"
            + "    \"standard_name_vocabulary\": {\"type\": \"char\", \"data\": \"CF Standard Name Table v70\"},\n"
            + "    \"summary\": {\"type\": \"char\", \"data\": \"NOAA CoastWatch distributes chlorophyll-a concentration data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.   This is Science Quality data.\"},\n"
            + "    \"time_coverage_end\": {\"type\": \"char\", \"data\": \"2006-12-15T00:00:00Z\"},\n"
            + "    \"time_coverage_start\": {\"type\": \"char\", \"data\": \"2002-07-08T00:00:00Z\"},\n"
            + "    \"title\": {\"type\": \"char\", \"data\": \"Chlorophyll-a, Aqua MODIS, NPP, 2002-2013, DEPRECATED OLDER VERSION (8 Day Composite)\"},\n"
            + "    \"Westernmost_Easting\": {\"type\": \"double\", \"data\": 360.0}\n"
            + "  },\n"
            + "  \"dimensions\": {\n"
            + "    \"time\": 3,\n"
            + "    \"longitude\": 1\n"
            + "  },\n"
            + "  \"variables\": {\n"
            + "    \"time\": {\n"
            + "      \"shape\": [\"time\"],\n"
            + "      \"type\": \"double\",\n"
            + "      \"attributes\": {\n"
            + "        \"_CoordinateAxisType\": {\"type\": \"char\", \"data\": \"Time\"},\n"
            + "        \"actual_range\": {\"type\": \"double\", \"data\": [1.0260864E9, 1.1661408E9]},\n"
            + "        \"axis\": {\"type\": \"char\", \"data\": \"T\"},\n"
            + "        \"fraction_digits\": {\"type\": \"int\", \"data\": 0},\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Time\"},\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Centered Time\"},\n"
            + "        \"standard_name\": {\"type\": \"char\", \"data\": \"time\"},\n"
            + "        \"time_origin\": {\"type\": \"char\", \"data\": \"01-JAN-1970 00:00:00\"},\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"seconds since 1970-01-01T00:00:00Z\"}\n"
            + "      },\n"
            + "      \"data\": [1.0260864E9, 1.0960704E9, 1.1661408E9]\n"
            + "    },\n"
            + "    \"longitude\": {\n"
            + "      \"shape\": [\"longitude\"],\n"
            + "      \"type\": \"double\",\n"
            + "      \"attributes\": {\n"
            + "        \"_CoordinateAxisType\": {\"type\": \"char\", \"data\": \"Lon\"},\n"
            + "        \"actual_range\": {\"type\": \"double\", \"data\": [360.0, 360.0]},\n"
            + "        \"axis\": {\"type\": \"char\", \"data\": \"X\"},\n"
            + "        \"coordsys\": {\"type\": \"char\", \"data\": \"geographic\"},\n"
            + "        \"fraction_digits\": {\"type\": \"int\", \"data\": 4},\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Location\"},\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Longitude\"},\n"
            + "        \"point_spacing\": {\"type\": \"char\", \"data\": \"even\"},\n"
            + "        \"standard_name\": {\"type\": \"char\", \"data\": \"longitude\"},\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"degrees_east\"}\n"
            + "      },\n"
            + "      \"data\": [360.0]\n"
            + "    }\n"
            + "  }\n"
            + "}\n"
            + ")";
    tPo = results.indexOf(expected.substring(0, 40));
    Test.ensureEqual(results.substring(tPo), expected, "results=\n" + results);

    // .timeGaps
    String2.log("\n*** EDDGridFromDap test get .timeGaps\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "time[0:100:200],longitude[last]",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_tg",
            ".timeGaps");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "Time gaps greater than the median (8 days):\n"
            + "[21]=2002-12-23T00:00:00Z -> [22]=2003-01-05T00:00:00Z, gap=13 days\n"
            + "[66]=2003-12-23T00:00:00Z -> [67]=2004-01-05T00:00:00Z, gap=13 days\n"
            + "[111]=2004-12-22T00:00:00Z -> [112]=2005-01-05T00:00:00Z, gap=14 days\n"
            + "[156]=2005-12-23T00:00:00Z -> [157]=2006-01-05T00:00:00Z, gap=13 days\n"
            + "[201]=2006-12-23T00:00:00Z -> [202]=2007-01-05T00:00:00Z, gap=13 days\n"
            + "[246]=2007-12-23T00:00:00Z -> [247]=2008-01-05T00:00:00Z, gap=13 days\n"
            + "[291]=2008-12-22T00:00:00Z -> [292]=2009-01-05T00:00:00Z, gap=14 days\n"
            + "[336]=2009-12-23T00:00:00Z -> [337]=2010-01-05T00:00:00Z, gap=13 days\n"
            + "[381]=2010-12-23T00:00:00Z -> [382]=2011-01-05T00:00:00Z, gap=13 days\n"
            + "[426]=2011-12-23T00:00:00Z -> [427]=2012-01-05T00:00:00Z, gap=13 days\n"
            + "[471]=2012-12-22T00:00:00Z -> [472]=2013-01-05T00:00:00Z, gap=14 days\n"
            + "nGaps=11\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // .tsv
    String2.log("\n*** EDDGridFromDap test get .TSV axis data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "time[0:100:200],longitude[last]",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Axis",
            ".tsv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    expected =
        "time\tlongitude\n"
            + "UTC\tdegrees_east\n"
            + "2002-07-08T00:00:00Z\t360.0\n"
            + "2004-09-25T00:00:00Z\tNaN\n"
            + "2006-12-15T00:00:00Z\tNaN\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    // .tsvp
    String2.log("\n*** EDDGridFromDap test get .tsv axis data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "time[0:100:200],longitude[last]",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Axis",
            ".tsvp");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    expected =
        "time (UTC)\tlongitude (degrees_east)\n"
            + "2002-07-08T00:00:00Z\t360.0\n"
            + "2004-09-25T00:00:00Z\tNaN\n"
            + "2006-12-15T00:00:00Z\tNaN\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    // .xhtml latitude
    String2.log("\n*** EDDGridFromDap test get .XHTML axis data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "latitude[0:10:40],longitude[last]",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_LatAxis",
            ".xhtml");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    expected =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n"
            + "  \"https://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n"
            + "<html xmlns=\"https://www.w3.org/1999/xhtml\">\n"
            + "<head>\n"
            + "  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n"
            + "  <title>EDDGridFromDap_LatAxis</title>\n"
            + "  <link rel=\"stylesheet\" type=\"text/css\" href=\"http://127.0.0.1:8080/cwexperimental/images/erddap2.css\" />\n"
            + "</head>\n"
            + "<body>\n"
            + "\n"
            + "&nbsp;\n"
            + "<table class=\"erd commonBGColor nowrap\">\n"
            + "<tr>\n"
            + "<th>latitude</th>\n"
            + "<th>longitude</th>\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<th>degrees_north</th>\n"
            + "<th>degrees_east</th>\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td class=\"R\">-90.0</td>\n"
            + "<td class=\"R\">360.0</td>\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td class=\"R\">-89.58323686038435</td>\n"
            + "<td></td>\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td class=\"R\">-89.16647372076869</td>\n"
            + "<td></td>\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td class=\"R\">-88.74971058115304</td>\n"
            + "<td></td>\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td class=\"R\">-88.3329474415374</td>\n"
            + "<td></td>\n"
            + "</tr>\n"
            + "</table>\n"
            + "</body>\n"
            + "</html>\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    // .htmlTable latitude
    String2.log("\n*** EDDGridFromDap test get .htmlTable axis data\n");
    String tQuery = "latitude[0:10:40],longitude[last]";
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            tQuery,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_LatAxis",
            ".htmlTable");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    expected =
        EDStatic.startHeadHtml(
                language, EDStatic.erddapUrl((String) null, language), "EDDGridFromDap_LatAxis")
            + "\n"
            + "</head>\n"
            + EDStatic.startBodyHtml(language, null, "griddap/erdMHchla8day.html", tQuery)
            + // 2022-11-22
            // .html
            // because
            // language
            // selector
            // system
            // here
            // changes
            // .htmlTable
            // to
            // .html
            // to
            // avoid
            "&nbsp;<br>\n"
            + "&nbsp;\n"
            + "<table class=\"erd commonBGColor nowrap\">\n"
            + "<tr>\n"
            + "<th>latitude\n"
            + "<th>longitude\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<th>degrees_north\n"
            + "<th>degrees_east\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td class=\"R\">-90.0\n"
            + "<td class=\"R\">360.0\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td class=\"R\">-89.58323686038435\n"
            + "<td>\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td class=\"R\">-89.16647372076869\n"
            + "<td>\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td class=\"R\">-88.74971058115304\n"
            + "<td>\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td class=\"R\">-88.3329474415374\n"
            + "<td>\n"
            + "</tr>\n"
            + "</table>\n"
            + EDStatic.endBodyHtml(
                language, EDStatic.erddapUrl((String) null, language), (String) null)
            + "\n"
            + "</html>\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    // .xhtml time
    String2.log("\n*** EDDGridFromDap test get .XHTML axis data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "time[0:100:200],longitude[last]",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Axis",
            ".xhtml");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    expected =
        "<tr>\n"
            + "<th>time</th>\n"
            + "<th>longitude</th>\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<th>UTC</th>\n"
            + "<th>degrees_east</th>\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td>2002-07-08T00:00:00Z</td>\n"
            + "<td class=\"R\">360.0</td>\n"
            + "</tr>\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "RESULTS=\n" + results);
    expected =
        "<tr>\n"
            + "<td>2006-12-15T00:00:00Z</td>\n"
            + "<td></td>\n"
            + "</tr>\n"
            + "</table>\n"
            + "</body>\n"
            + "</html>\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "RESULTS=\n" + results);
  }

  @org.junit.jupiter.api.Test
  @TagThredds
  void testBasic3() throws Throwable {
    String2.log("\n*** EDDGridFromDap.testBasic3\n");
    // testVerboseOn();
    EDDGridFromDap gridDataset;
    String name, tName, axisDapQuery, results, expected, error;
    int tPo;
    String userDapQuery = "chlorophyll[(2007-02-06)][][(29):10:(50)][(225):10:(247)]";
    String graphDapQuery = "chlorophyll[0:10:200][][(29)][(225)]";
    String mapDapQuery = "chlorophyll[200][][(29):(50)][(225):(247)]"; // stride irrelevant
    StringArray destinationNames = new StringArray();
    IntArray constraints = new IntArray();
    int language = 0;

    gridDataset = (EDDGridFromDap) EDDTestDataset.geterdMHchla8day();
    // just comment out to work on some other test

    // ******************************************** test GridDataRandomAccessor
    // set up GridDataRandomAccessor
    GridDataAccessor gda =
        new GridDataAccessor(language, gridDataset, "", userDapQuery, true, true); // rowMajor
    // toNaN
    GridDataRandomAccessor gdra = new GridDataRandomAccessor(gda);
    // maka a new rowMajor gda and test if same data
    gda =
        new GridDataAccessor(language, gridDataset, "", userDapQuery, true, true); // rowMajor toNaN
    PAOne gdaPAOne = new PAOne(gda.dataVariables()[0].sourceDataPAType());
    PAOne gdraPAOne = new PAOne(gdra.dataPAType(0));
    int current[] = gda.totalIndex().getCurrent(); // the internal object that changes
    int count = 0;
    while (gda.increment()) {
      // String2.log(String2.toCSSVString(current)); //to prove that access is
      // rowMajor
      Test.ensureEqual(
          gda.getDataValueAsPAOne(0, gdaPAOne),
          gdra.getDataValueAsPAOne(current, 0, gdraPAOne),
          "count=" + count);
      count++;
    }
    String2.log("Test of GridDataRandomAccess rowMajor succeeded. count=" + count);
    // maka a new columnMajor gda and test if same data
    gda =
        new GridDataAccessor(
            language, gridDataset, "", userDapQuery, false, true); // rowMajor toNaN
    gdaPAOne = new PAOne(gda.dataVariables()[0].sourceDataPAType());
    gdraPAOne = new PAOne(gdra.dataPAType(0));
    current = gda.totalIndex().getCurrent(); // the internal object that changes
    count = 0;
    while (gda.increment()) {
      // String2.log(String2.toCSSVString(current)); //to prove that access is
      // columnMajor
      Test.ensureEqual(
          gda.getDataValueAsPAOne(0, gdaPAOne),
          gdra.getDataValueAsPAOne(current, 0, gdraPAOne),
          "count=" + count);
      count++;
    }
    String2.log("Test of GridDataRandomAccess columnMajor succeeded. count=" + count);
    gdra.releaseResources();
    gda.releaseResources();

    // ********************************************** test getting grid data
    // .asc
    String2.log("\n*** EDDGridFromDap test get .ASC data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Data",
            ".asc");
    results =
        String2.annotatedString(
            File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName));
    // String2.log("\n.asc results=\n" + results);
    expected =
        "Dataset {[10]\n"
            + "  GRID {[10]\n"
            + "    ARRAY:[10]\n"
            + "      Float32 chlorophyll[time = 1][altitude = 1][latitude = 51][longitude = 53];[10]\n"
            + "    MAPS:[10]\n"
            + "      Float64 time[time = 1];[10]\n"
            + "      Float64 altitude[altitude = 1];[10]\n"
            + "      Float64 latitude[latitude = 51];[10]\n"
            + "      Float64 longitude[longitude = 53];[10]\n"
            + "  } chlorophyll;[10]\n"
            + "} erdMHchla8day;[10]\n"
            + "---------------------------------------------[10]\n"
            + "chlorophyll.chlorophyll[1][1][51][53][10]\n"
            +
            // missing values are sourceMissingValue
            // "[0][0][0], -9999999.0, -9999999.0, 0.099, 0.118, -9999999.0, 0.091,
            // -9999999.0,"; //pre 2010-10-26
            // "[0][0][0], -9999999.0, -9999999.0, 0.10655, 0.12478, -9999999.0, 0.09398,
            // -9999999.0, 0.08919, 0.09892,"; //pre 2012-08-17
            "[0][0][0], -9999999.0, -9999999.0, 0.11093, 0.12439, -9999999.0, 0.09554, -9999999.0, 0.09044, 0.10009,";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);
    expected = "[0][0][42], -9999999.0,";
    Test.ensureTrue(results.indexOf(expected) > 0, "RESULTS=\n" + results);
    expected =
        "chlorophyll.time[1][10]\n"
            + "1.17072E9[10]\n"
            + "[10]\n"
            + "chlorophyll.altitude[1][10]\n"
            + "0.0[10]\n"
            + "[10]\n"
            + "chlorophyll.latitude[51][10]\n"
            + "28.985876360268577, 29.40263949988423, 29.81940263949987, 30.236165779115524, 30.652928918731178, 31.06969205834683, 31.486455197962485, 31.90321833757814, 32.31998147719379, 32.73674461680943, 33.153507756425086, 33.57027089604074, 33.98703403565639, 34.40379717527205, 34.8205603148877, 35.237323454503354, 35.65408659411899, 36.07084973373465, 36.4876128733503, 36.904376012965955, 37.32113915258161, 37.73790229219726, 38.1546654318129, 38.571428571428555, 38.98819171104421, 39.40495485065986, 39.821717990275516, 40.23848112989117, 40.655244269506824, 41.07200740912248, 41.48877054873813, 41.905533688353785, 42.32229682796944, 42.73905996758509, 43.155823107200746, 43.57258624681637, 43.989349386432025, 44.40611252604768, 44.82287566566333, 45.239638805278986, 45.65640194489464, 46.07316508451029, 46.48992822412595, 46.9066913637416, 47.323454503357254, 47.74021764297291, 48.15698078258856, 48.573743922204216, 48.99050706181987, 49.407270201435495, 49.82403334105115[10]\n"
            + "[10]\n"
            + "chlorophyll.longitude[53][10]\n"
            + "224.98437319134158, 225.40108808889917, 225.81780298645677, 226.23451788401434, 226.65123278157193, 227.06794767912953, 227.4846625766871, 227.9013774742447, 228.3180923718023, 228.73480726935986, 229.15152216691746, 229.56823706447506, 229.98495196203262, 230.40166685959022, 230.81838175714782, 231.2350966547054, 231.65181155226298, 232.06852644982058, 232.48524134737815, 232.90195624493575, 233.31867114249334, 233.7353860400509, 234.1521009376085, 234.5688158351661, 234.98553073272367, 235.40224563028127, 235.81896052783887, 236.23567542539644, 236.65239032295403, 237.06910522051163, 237.48582011806923, 237.9025350156268, 238.3192499131844, 238.735964810742, 239.15267970829956, 239.56939460585716, 239.98610950341475, 240.40282440097232, 240.81953929852992, 241.23625419608751, 241.65296909364508, 242.06968399120268, 242.48639888876028, 242.90311378631785, 243.31982868387544, 243.73654358143304, 244.1532584789906, 244.5699733765482, 244.9866882741058, 245.40340317166337, 245.82011806922097, 246.23683296677856, 246.65354786433613[10]\n[end]";
    int po7 = results.indexOf("chlorophyll.time");
    Test.ensureEqual(results.substring(po7), expected, "RESULTS=\n" + results);

    // .csv
    String2.log("\n*** EDDGridFromDap test get .CSV data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Data",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected = // missing values are "NaN"
        /*
         * pre 2010-10-26 was:
         * "time, altitude, latitude, longitude, chlorophyll\n" +
         * "UTC, m, degrees_north, degrees_east, mg m-3\n" +
         * "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 224.98437319134158, NaN\n" +
         * "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 225.40108808889917, NaN\n" +
         * "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 225.81780298645677, 0.099\n"
         * +
         * "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 226.23451788401434, 0.118\n"
         * +
         * "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 226.65123278157193, NaN\n" +
         * "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 227.06794767912953, 0.091\n";
         * pre 2012-08-17 was
         * "time,altitude,latitude,longitude,chlorophyll\n" +
         * "UTC,m,degrees_north,degrees_east,mg m-3\n" +
         * "2007-02-06T00:00:00Z,0.0,28.985876360268577,224.98437319134158,NaN\n" +
         * "2007-02-06T00:00:00Z,0.0,28.985876360268577,225.40108808889917,NaN\n" +
         * "2007-02-06T00:00:00Z,0.0,28.985876360268577,225.81780298645677,0.10655\n" +
         * "2007-02-06T00:00:00Z,0.0,28.985876360268577,226.23451788401434,0.12478\n" +
         * "2007-02-06T00:00:00Z,0.0,28.985876360268577,226.65123278157193,NaN\n" +
         * "2007-02-06T00:00:00Z,0.0,28.985876360268577,227.06794767912953,0.09398\n";
         */
        "time,altitude,latitude,longitude,chlorophyll\n"
            + "UTC,m,degrees_north,degrees_east,mg m-3\n"
            + "2007-02-06T00:00:00Z,0.0,28.985876360268577,224.98437319134158,NaN\n"
            + "2007-02-06T00:00:00Z,0.0,28.985876360268577,225.40108808889917,NaN\n"
            + "2007-02-06T00:00:00Z,0.0,28.985876360268577,225.81780298645677,0.11093\n"
            + "2007-02-06T00:00:00Z,0.0,28.985876360268577,226.23451788401434,0.12439\n"
            + "2007-02-06T00:00:00Z,0.0,28.985876360268577,226.65123278157193,NaN\n"
            + "2007-02-06T00:00:00Z,0.0,28.985876360268577,227.06794767912953,0.09554\n";

    Test.ensureTrue(results.indexOf(expected) == 0, "RESULTS=\n" + results);
    expected = // "2007-02-06T00:00:00Z,0.0,49.407270201435495,232.06852644982058,0.37\n";
        // //pre 2010-10-26
        // "2007-02-06T00:00:00Z,0.0,49.407270201435495,232.06852644982058,0.58877\n";
        // //pre 2012-08-17
        "2007-02-06T00:00:00Z,0.0,49.407270201435495,232.06852644982058,0.56545\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "RESULTS=\n" + results);

    // .csv test gridName.gridName notation
    String2.log("\n*** EDDGridFromDap test get .CSV data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "chlorophyll." + userDapQuery,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_DotNotation",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        /*
         * pre 2010-10-26 was:
         * "time, altitude, latitude, longitude, chlorophyll\n" +
         * "UTC, m, degrees_north, degrees_east, mg m-3\n" +
         * "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 224.98437319134158, NaN\n" +
         * "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 225.40108808889917, NaN\n" +
         * "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 225.81780298645677, 0.099\n"
         * +
         * "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 226.23451788401434, 0.118\n"
         * +
         * "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 226.65123278157193, NaN\n" +
         * "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 227.06794767912953, 0.091\n";
         * //pre 2012-08-17 was
         * "time,altitude,latitude,longitude,chlorophyll\n" +
         * "UTC,m,degrees_north,degrees_east,mg m-3\n" +
         * "2007-02-06T00:00:00Z,0.0,28.985876360268577,224.98437319134158,NaN\n" +
         * "2007-02-06T00:00:00Z,0.0,28.985876360268577,225.40108808889917,NaN\n" +
         * "2007-02-06T00:00:00Z,0.0,28.985876360268577,225.81780298645677,0.10655\n" +
         * "2007-02-06T00:00:00Z,0.0,28.985876360268577,226.23451788401434,0.12478\n" +
         * "2007-02-06T00:00:00Z,0.0,28.985876360268577,226.65123278157193,NaN\n" +
         * "2007-02-06T00:00:00Z,0.0,28.985876360268577,227.06794767912953,0.09398\n";
         */
        "time,altitude,latitude,longitude,chlorophyll\n"
            + "UTC,m,degrees_north,degrees_east,mg m-3\n"
            + "2007-02-06T00:00:00Z,0.0,28.985876360268577,224.98437319134158,NaN\n"
            + "2007-02-06T00:00:00Z,0.0,28.985876360268577,225.40108808889917,NaN\n"
            + "2007-02-06T00:00:00Z,0.0,28.985876360268577,225.81780298645677,0.11093\n"
            + "2007-02-06T00:00:00Z,0.0,28.985876360268577,226.23451788401434,0.12439\n"
            + "2007-02-06T00:00:00Z,0.0,28.985876360268577,226.65123278157193,NaN\n"
            + "2007-02-06T00:00:00Z,0.0,28.985876360268577,227.06794767912953,0.09554\n";
    Test.ensureTrue(results.indexOf(expected) == 0, "RESULTS=\n" + results);
    expected = // "2007-02-06T00:00:00Z,0.0,49.407270201435495,232.06852644982058,0.37\n";
        // //pre 2010-10-26
        // "2007-02-06T00:00:00Z,0.0,49.407270201435495,232.06852644982058,0.58877\n";
        // //pre 2012-08-17
        "2007-02-06T00:00:00Z,0.0,49.407270201435495,232.06852644982058,0.56545\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "RESULTS=\n" + results);

    // .das
    String2.log("\n*** EDDGridFromDap test get .DAS data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Data",
            ".das");
    results =
        String2.annotatedString(
            File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName));
    expected =
        // "Attributes {[10]\n" +
        // " time {[10]\n" +
        // " Float64 actual_range 1.17072e+9, 1.17072e+9;[10]\n" + //changes sometimes
        "    String axis \"T\";[10]\n"
            + "    Int32 fraction_digits 0;[10]\n"
            + "    String ioos_category \"Time\";[10]\n"
            + "    String long_name \"Centered Time\";[10]\n"
            + "    String standard_name \"time\";[10]\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";[10]\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";[10]\n"
            + "  }[10]\n"
            + "  altitude {[10]\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    expected =
        "  NC_GLOBAL {[10]\n"
            + "    String acknowledgement \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";[10]\n"
            + "    String cdm_data_type \"Grid\";[10]\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);

    // .dds
    String2.log("\n*** EDDGridFromDap test get .DDS data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Data",
            ".dds");
    results =
        String2.annotatedString(
            File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName));
    // String2.log(results);
    expected =
        "Dataset {[10]\n"
            + "  GRID {[10]\n"
            + "    ARRAY:[10]\n"
            + "      Float32 chlorophyll[time = 1][altitude = 1][latitude = 51][longitude = 53];[10]\n"
            + "    MAPS:[10]\n"
            + "      Float64 time[time = 1];[10]\n"
            + "      Float64 altitude[altitude = 1];[10]\n"
            + "      Float64 latitude[latitude = 51];[10]\n"
            + "      Float64 longitude[longitude = 53];[10]\n"
            + "  } chlorophyll;[10]\n"
            + "} erdMHchla8day;[10]\n"
            + "[end]";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    // .dods
    String2.log("\n*** EDDGridFromDap test get .DODS data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Data",
            ".dods");
    results =
        String2.annotatedString(
            File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName));
    // String2.log(results);
    expected =
        "Dataset {[10]\n"
            + "  GRID {[10]\n"
            + "    ARRAY:[10]\n"
            + "      Float32 chlorophyll[time = 1][altitude = 1][latitude = 51][longitude = 53];[10]\n"
            + "    MAPS:[10]\n"
            + "      Float64 time[time = 1];[10]\n"
            + "      Float64 altitude[altitude = 1];[10]\n"
            + "      Float64 latitude[latitude = 51];[10]\n"
            + "      Float64 longitude[longitude = 53];[10]\n"
            + "  } chlorophyll;[10]\n"
            + "} erdMHchla8day;[10]\n"
            + "[10]\n"
            + "Data:[10]\n";
    Test.ensureTrue(results.indexOf(expected) == 0, "RESULTS=\n" + results);
    results = String2.annotatedString(results);
    expected =
        "N[226][25][169]@nj<[9]]2+@nw[145][195][216]J[173]@n[132][231]~Sc.@n[146]=8[206]{[176]@n[159][146][243]I[148]2@n[172][232][173][196][172][179]@n[186]>h?[197]5@n[199][148]\"[186][221][183]@n[212][233][221]5[246]8[end][end]";
    Test.ensureTrue(results.indexOf(expected) > 0, "RESULTS=\n" + results);

    // .esriAscii
    String2.log("\n*** EDDGridFromDap test get .esriAscii data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Map", // must be Map
            // because .asc
            // already
            // used
            ".esriAscii");
    // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected = // note that lon values have been shifted from 225 to -135
        "ncols 53\n"
            + "nrows 51\n"
            + "xllcenter -135.01562680865842\n"
            + "yllcenter 28.985876360268577\n"
            + "cellsize 0.4167631396156514\n"
            + "nodata_value -9999999\n"
            + "-9999999 -9999999 -9999999 -9999999 -9999999 -9999999 -9999999 -9999999 -9999999";
    Test.ensureTrue(results.indexOf(expected) == 0, "RESULTS=\n" + results);
    expected = // last row
        // "0.204 0.239 0.26 0.252 0.274 0.289 0.367 0.37 0.65 0.531 -9999999 -9999999
        // 1.141\n"; //pre 2010-10-26
        // "0.27878 0.31141 0.32663 0.41135 0.40628 0.65426 0.4827 -9999999 -9999999
        // 1.16268\n"; //pre 2010-08-17
        "0.28787 0.31865 0.33447 0.43293 0.43297 0.68101 0.48409 -9999999 -9999999 1.20716\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "RESULTS=\n" + results);

    // .json
    String2.log("\n*** EDDGridFromDap test get .JSON data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Data",
            ".json");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    expected = // missing values are "null"
        "{\n"
            + "  \"table\": {\n"
            + "    \"columnNames\": [\"time\", \"altitude\", \"latitude\", \"longitude\", \"chlorophyll\"],\n"
            + "    \"columnTypes\": [\"String\", \"double\", \"double\", \"double\", \"float\"],\n"
            + "    \"columnUnits\": [\"UTC\", \"m\", \"degrees_north\", \"degrees_east\", \"mg m-3\"],\n"
            + "    \"rows\": [\n"
            + "      [\"2007-02-06T00:00:00Z\", 0, 28.985876360268577, 224.98437319134158, null],\n"
            + // pre
            // 2018-03-17
            // was
            // 0.0
            "      [\"2007-02-06T00:00:00Z\", 0, 28.985876360268577, 225.40108808889917, null],\n"
            +
            // pre 2010-10-26, was
            // " [\"2007-02-06T00:00:00Z\", 0.0, 28.985876360268577,
            // 225.81780298645677,
            // 0.099],\n" +
            // " [\"2007-02-06T00:00:00Z\", 0.0, 28.985876360268577,
            // 226.23451788401434,
            // 0.118],\n";
            // pre 2012-08-17 was:
            // " [\"2007-02-06T00:00:00Z\", 0.0, 28.985876360268577,
            // 225.81780298645677,
            // 0.10655],\n" +
            // " [\"2007-02-06T00:00:00Z\", 0.0, 28.985876360268577,
            // 226.23451788401434,
            // 0.12478],\n"; */
            "      [\"2007-02-06T00:00:00Z\", 0, 28.985876360268577, 225.81780298645677, 0.11093],\n"
            + "      [\"2007-02-06T00:00:00Z\", 0, 28.985876360268577, 226.23451788401434, 0.12439],\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);
    expected =
        "      [\"2007-02-06T00:00:00Z\", 0, 49.82403334105115, 246.23683296677856, null],\n"
            + "      [\"2007-02-06T00:00:00Z\", 0, 49.82403334105115, 246.65354786433613, null]\n"
            + "    ]\n"
            + "  }\n"
            + "}\n";
    Test.ensureEqual(
        results.substring(results.length() - expected.length()), expected, "RESULTS=\n" + results);
    /* */
    // .mat
    // octave> load('c:/temp/griddap/EDDGridFromDap_Data.mat');
    // octave> erdMHchla8day
    String2.log("\n*** EDDGridFromDap test get .MAT data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Data",
            ".mat");
    results = File2.hexDump(EDStatic.fullTestCacheDirectory + tName, 1000000);

    // String2.log(results);
    expected =
        "4d 41 54 4c 41 42 20 35   2e 30 20 4d 41 54 2d 66   MATLAB 5.0 MAT-f |\n"
            + "69 6c 65 2c 20 43 72 65   61 74 65 64 20 62 79 3a   ile, Created by: |\n"
            + "20 67 6f 76 2e 6e 6f 61   61 2e 70 66 65 6c 2e 63    gov.noaa.pfel.c |\n"
            + "6f 61 73 74 77 61 74 63   68 2e 4d 61 74 6c 61 62   oastwatch.Matlab |\n"
            + "20 20 20 20 00 00 00 00   00 00 00 00 01 00 4d 49                 MI |\n"
            + "00 00 00 0e 00 00 2f 98   00 00 00 06 00 00 00 08         /          |\n"
            + "00 00 00 02 00 00 00 00   00 00 00 05 00 00 00 08                    |\n"
            + "00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 0d                    |\n"
            + "65 72 64 4d 48 63 68 6c   61 38 64 61 79 00 00 00   erdMHchla8day    |\n"
            + "00 04 00 05 00 00 00 20   00 00 00 01 00 00 00 a0                    |\n"
            + "74 69 6d 65 00 00 00 00   00 00 00 00 00 00 00 00   time             |\n"
            + "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            + "61 6c 74 69 74 75 64 65   00 00 00 00 00 00 00 00   altitude         |\n"
            + "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            + "6c 61 74 69 74 75 64 65   00 00 00 00 00 00 00 00   latitude         |\n"
            + "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            + "6c 6f 6e 67 69 74 75 64   65 00 00 00 00 00 00 00   longitude        |\n"
            + "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            + "63 68 6c 6f 72 6f 70 68   79 6c 6c 00 00 00 00 00   chlorophyll      |\n"
            + "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            + "00 00 00 0e 00 00 00 38   00 00 00 06 00 00 00 08          8         |\n"
            + "00 00 00 06 00 00 00 00   00 00 00 05 00 00 00 08                    |\n"
            + "00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 00                    |\n"
            + "00 00 00 09 00 00 00 08   41 d1 71 f1 40 00 00 00           A q @    |\n"
            + "00 00 00 0e 00 00 00 38   00 00 00 06 00 00 00 08          8         |\n"
            + "00 00 00 06 00 00 00 00   00 00 00 05 00 00 00 08                    |\n"
            + "00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 00                    |\n"
            + "00 00 00 09 00 00 00 08   00 00 00 00 00 00 00 00                    |\n"
            + "00 00 00 0e 00 00 01 c8   00 00 00 06 00 00 00 08                    |\n"
            + "00 00 00 06 00 00 00 00   00 00 00 05 00 00 00 08                    |\n"
            + "00 00 00 33 00 00 00 01   00 00 00 01 00 00 00 00      3             |\n"
            + "00 00 00 09 00 00 01 98   40 3c fc 62 64 a5 40 c8           @< bd @  |\n"
            + "40 3d 67 13 61 dc 14 a0   40 3d d1 c4 5f 12 e8 74   @=g a   @=  _  t |\n"
            + "40 3e 3c 75 5c 49 bc 4c   40 3e a7 26 59 80 90 24   @><u\\I L@> &Y  $ |\n"
            + "40 3f 11 d7 56 b7 63 fc   40 3f 7c 88 53 ee 37 d4   @?  V c @?| S 7  |\n"
            + "40 3f e7 39 51 25 0b ac   40 40 28 f5 27 2d ef c2   @? 9Q%  @@( '-   |\n"
            + "40 40 5e 4d a5 c9 59 ac   40 40 93 a6 24 64 c3 98   @@^M  Y @@  $d   |\n"
            + "40 40 c8 fe a3 00 2d 84   40 40 fe 57 21 9b 97 70   @@    - @@ W!  p |";
    results =
        results.substring(0, 71 * 4) + results.substring(71 * 7); // remove the creation dateTime
    results =
        results.substring(0, Math.min(results.length(), expected.length())); // remove the creation
    // dateTime
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);
    /* */
    // .nc
    String2.log("\n*** EDDGridFromDap test get .NC data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Data",
            ".nc");
    results = NcHelper.ncdump(EDStatic.fullTestCacheDirectory + tName, "");
    expected = // changed a little ("// (has coord.var)") when switched to netcdf-java 4.0,
        // 2009-02-23
        "netcdf EDDGridFromDap_Data.nc {\n"
            + "  dimensions:\n"
            + "    time = 1;\n"
            + "    altitude = 1;\n"
            + "    latitude = 51;\n"
            + "    longitude = 53;\n"
            + "  variables:\n"
            + "    double time(time=1);\n"
            + "      :_CoordinateAxisType = \"Time\";\n"
            + "      :actual_range = 1.17072E9, 1.17072E9; // double\n"
            + "      :axis = \"T\";\n"
            + "      :fraction_digits = 0; // int\n"
            + "      :ioos_category = \"Time\";\n"
            + "      :long_name = \"Centered Time\";\n"
            + "      :standard_name = \"time\";\n"
            + "      :time_origin = \"01-JAN-1970 00:00:00\";\n"
            + "      :units = \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "\n"
            + "    double altitude(altitude=1);\n"
            + "      :_CoordinateAxisType = \"Height\";\n"
            + "      :_CoordinateZisPositive = \"up\";\n"
            + "      :actual_range = 0.0, 0.0; // double\n"
            + "      :axis = \"Z\";\n"
            + "      :fraction_digits = 0; // int\n"
            + "      :ioos_category = \"Location\";\n"
            + "      :long_name = \"Altitude\";\n"
            + "      :positive = \"up\";\n"
            + "      :standard_name = \"altitude\";\n"
            + "      :units = \"m\";\n"
            + "\n"
            + "    double latitude(latitude=51);\n"
            + "      :_CoordinateAxisType = \"Lat\";\n"
            + "      :actual_range = 28.985876360268577, 49.82403334105115; // double\n"
            + "      :axis = \"Y\";\n"
            + "      :coordsys = \"geographic\";\n"
            + "      :fraction_digits = 4; // int\n"
            + "      :ioos_category = \"Location\";\n"
            + "      :long_name = \"Latitude\";\n"
            + "      :point_spacing = \"even\";\n"
            + "      :standard_name = \"latitude\";\n"
            + "      :units = \"degrees_north\";\n"
            + "\n"
            + "    double longitude(longitude=53);\n"
            + "      :_CoordinateAxisType = \"Lon\";\n"
            + "      :actual_range = 224.98437319134158, 246.65354786433613; // double\n"
            + "      :axis = \"X\";\n"
            + "      :coordsys = \"geographic\";\n"
            + "      :fraction_digits = 4; // int\n"
            + "      :ioos_category = \"Location\";\n"
            + "      :long_name = \"Longitude\";\n"
            + "      :point_spacing = \"even\";\n"
            + "      :standard_name = \"longitude\";\n"
            + "      :units = \"degrees_east\";\n"
            + "\n"
            + "    float chlorophyll(time=1, altitude=1, latitude=51, longitude=53);\n"
            + "      :_FillValue = -9999999.0f; // float\n"
            + "      :colorBarMaximum = 30.0; // double\n"
            + "      :colorBarMinimum = 0.03; // double\n"
            + "      :colorBarScale = \"Log\";\n"
            + "      :coordsys = \"geographic\";\n"
            + "      :fraction_digits = 2; // int\n"
            + "      :ioos_category = \"Ocean Color\";\n"
            + "      :long_name = \"Concentration Of Chlorophyll In Sea Water\";\n"
            + "      :missing_value = -9999999.0f; // float\n"
            + "      :standard_name = \"concentration_of_chlorophyll_in_sea_water\";\n"
            + "      :units = \"mg m-3\";\n"
            + "\n"
            + "  // global attributes:\n"
            + "  :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n"
            + "  :cdm_data_type = \"Grid\";\n"
            + "  :composite = \"true\";\n"
            + "  :contributor_name = \"NASA GSFC (OBPG)\";\n"
            + "  :contributor_role = \"Source of level 2 data.\";\n"
            + "  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "  :creator_email = \"erd.data@noaa.gov\";\n"
            + "  :creator_name = \"NOAA NMFS SWFSC ERD\";\n"
            + "  :creator_type = \"institution\";\n"
            + "  :creator_url = \"https://www.pfeg.noaa.gov\";\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);
    expected = // note original missing values
        "  :infoUrl = \"https://coastwatch.pfeg.noaa.gov/infog/MH_chla_las.html\";\n"
            + "  :institution = \"NOAA NMFS SWFSC ERD\";\n"
            + "  :keywords = \"8-day, aqua, chemistry, chlorophyll, chlorophyll-a, coastwatch, color, concentration, concentration_of_chlorophyll_in_sea_water, day, degrees, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, global, modis, noaa, npp, ocean, ocean color, oceans, quality, science, science quality, sea, seawater, water, wcn\";\n"
            + "  :keywords_vocabulary = \"GCMD Science Keywords\";\n"
            + "  :license = \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "  :naming_authority = \"gov.noaa.pfeg.coastwatch\";\n"
            + "  :Northernmost_Northing = 49.82403334105115; // double\n"
            + "  :origin = \"NASA GSFC (OBPG)\";\n"
            + "  :processing_level = \"3\";\n"
            + "  :project = \"CoastWatch (https://coastwatch.noaa.gov/)\";\n"
            + "  :projection = \"geographic\";\n"
            + "  :projection_type = \"mapped\";\n"
            + "  :publisher_email = \"erd.data@noaa.gov\";\n"
            + "  :publisher_name = \"NOAA NMFS SWFSC ERD\";\n"
            + "  :publisher_type = \"institution\";\n"
            + "  :publisher_url = \"https://www.pfeg.noaa.gov\";\n"
            + "  :references = \"Aqua/MODIS information: https://oceancolor.gsfc.nasa.gov/ . MODIS information: https://coastwatch.noaa.gov/modis_ocolor_overview.html .\";\n"
            + "  :satellite = \"Aqua\";\n"
            + "  :sensor = \"MODIS\";\n"
            + "  :source = \"satellite observation: Aqua, MODIS\";\n"
            + "  :sourceUrl = \"https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day\";\n"
            + "  :Southernmost_Northing = 28.985876360268577; // double\n"
            + "  :standard_name_vocabulary = \"CF Standard Name Table v70\";\n"
            + "  :summary = \"NOAA CoastWatch distributes chlorophyll-a concentration data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.   This is Science Quality data.\";\n"
            + "  :time_coverage_end = \"2007-02-06T00:00:00Z\";\n"
            + "  :time_coverage_start = \"2007-02-06T00:00:00Z\";\n"
            + "  :title = \"Chlorophyll-a, Aqua MODIS, NPP, 2002-2013, DEPRECATED OLDER VERSION (8 Day Composite)\";\n"
            + "  :Westernmost_Easting = 224.98437319134158; // double\n"
            + "\n"
            + "  data:\n"
            + "    time = \n"
            + "      {1.17072E9}\n"
            + "    altitude = \n"
            + "      {0.0}\n"
            + "    latitude = \n"
            + "      {28.985876360268577, 29.40263949988423, 29.81940263949987, 30.236165779115524, 30.652928918731178, 31.06969205834683, 31.486455197962485, 31.90321833757814, 32.31998147719379, 32.73674461680943, 33.153507756425086, 33.57027089604074, 33.98703403565639, 34.40379717527205, 34.8205603148877, 35.237323454503354, 35.65408659411899, 36.07084973373465, 36.4876128733503, 36.904376012965955, 37.32113915258161, 37.73790229219726, 38.1546654318129, 38.571428571428555, 38.98819171104421, 39.40495485065986, 39.821717990275516, 40.23848112989117, 40.655244269506824, 41.07200740912248, 41.48877054873813, 41.905533688353785, 42.32229682796944, 42.73905996758509, 43.155823107200746, 43.57258624681637, 43.989349386432025, 44.40611252604768, 44.82287566566333, 45.239638805278986, 45.65640194489464, 46.07316508451029, 46.48992822412595, 46.9066913637416, 47.323454503357254, 47.74021764297291, 48.15698078258856, 48.573743922204216, 48.99050706181987, 49.407270201435495, 49.82403334105115}\n"
            + "    longitude = \n"
            + "      {224.98437319134158, 225.40108808889917, 225.81780298645677, 226.23451788401434, 226.65123278157193, 227.06794767912953, 227.4846625766871, 227.9013774742447, 228.3180923718023, 228.73480726935986, 229.15152216691746, 229.56823706447506, 229.98495196203262, 230.40166685959022, 230.81838175714782, 231.2350966547054, 231.65181155226298, 232.06852644982058, 232.48524134737815, 232.90195624493575, 233.31867114249334, 233.7353860400509, 234.1521009376085, 234.5688158351661, 234.98553073272367, 235.40224563028127, 235.81896052783887, 236.23567542539644, 236.65239032295403, 237.06910522051163, 237.48582011806923, 237.9025350156268, 238.3192499131844, 238.735964810742, 239.15267970829956, 239.56939460585716, 239.98610950341475, 240.40282440097232, 240.81953929852992, 241.23625419608751, 241.65296909364508, 242.06968399120268, 242.48639888876028, 242.90311378631785, 243.31982868387544, 243.73654358143304, 244.1532584789906, 244.5699733765482, 244.9866882741058, 245.40340317166337, 245.82011806922097, 246.23683296677856, 246.65354786433613}\n"
            + "    chlorophyll = \n"
            + "      {\n"
            + "        {\n"
            + "          {\n"
            +
            // pre 2010-10-26 was
            // " {-9999999.0, -9999999.0, 0.099, 0.118, -9999999.0, 0.091,
            // -9999999.0,
            // 0.088, 0.085, 0.088, -9999999.0, 0.098, -9999999.0, 0.076,
            // -9999999.0, 0.07,
            // 0.071, -9999999.0, -9999999.0, -9999999.0, 0.078, -9999999.0, 0.09,
            // 0.084,
            // -9999999.0, -9999999.0, 0.098, -9999999.0, 0.079, 0.076, 0.085,
            // -9999999.0,
            // 0.086, 0.127, 0.199, 0.167, 0.191, 0.133, 0.14, 0.173, 0.204, 0.239,
            // 0.26,
            // 0.252, 0.274, 0.289, 0.367, 0.37, 0.65, 0.531, -9999999.0,
            // -9999999.0,
            // 1.141},\n";
            // pre 2012-08-17 was
            // " {-9999999.0, -9999999.0, 0.10655, 0.12478, -9999999.0, 0.09398,
            // -9999999.0,
            // 0.08919, 0.09892, 0.10007, -9999999.0, 0.09986, -9999999.0, 0.07119,
            // -9999999.0, 0.08288, 0.08163, -9999999.0, -9999999.0, -9999999.0,
            // 0.08319,
            // -9999999.0, 0.09706, 0.08309, -9999999.0, -9999999.0, 0.0996,
            // -9999999.0,
            // 0.08962, 0.08329, 0.09101, -9999999.0, 0.08679, 0.13689, 0.21315,
            // 0.18729,
            // 0.21642, 0.15069, 0.15123, 0.18849, 0.22975, 0.27075, 0.29062,
            // 0.27878,
            // 0.31141, 0.32663, 0.41135, 0.40628, 0.65426, 0.4827, -9999999.0,
            // -9999999.0,
            // 1.16268},\n";
            "            {-9999999.0, -9999999.0, 0.11093, 0.12439, -9999999.0, 0.09554, -9999999.0, 0.09044, 0.10009, 0.10116, -9999999.0, 0.10095, -9999999.0, 0.07243, -9999999.0, 0.08363, 0.08291, -9999999.0, -9999999.0, -9999999.0, 0.08885, -9999999.0, 0.09632, 0.0909, -9999999.0, -9999999.0, 0.09725, -9999999.0, 0.09978, 0.09462, 0.09905, -9999999.0, 0.09937, 0.12816, 0.20255, 0.17595, 0.20562, 0.14333, 0.15073, 0.18803, 0.22673, 0.27252, 0.29005, 0.28787, 0.31865, 0.33447, 0.43293, 0.43297, 0.68101, 0.48409, -9999999.0, -9999999.0, 1.20716},\n";
    tPo = results.indexOf("  :infoUrl");
    Test.ensureEqual(
        results.substring(tPo, tPo + expected.length()), expected, "RESULTS=\n" + results);

    // .ncHeader
    String2.log("\n*** EDDGridFromDap test get .NCHEADER data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Data",
            ".ncHeader");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    // if (true) System.exit(1);
    expected =
        "netcdf EDDGridFromDap_Data.nc {\n"
            + "  dimensions:\n"
            + "    time = 1;\n"
            + // (has coord.var)\n" + //changed when switched to netcdf-java 4.0,
            // 2009-02-23
            "    altitude = 1;\n"
            + // (has coord.var)\n" +
            "    latitude = 51;\n"
            + // (has coord.var)\n" +
            "    longitude = 53;\n"
            + // (has coord.var)\n" +
            "  variables:\n"
            + "    double time(time=1);\n";
    Test.ensureTrue(results.indexOf(expected) == 0, "RESULTS=\n" + results);

    expected = // test that sourceMissingValue is intact
        // (but complicated because that is the mv I use in Grid)
        // test that actual_range has been removed
        "    float chlorophyll(time=1, altitude=1, latitude=51, longitude=53);\n"
            + "      :_FillValue = -9999999.0f; // float\n"
            + "      :colorBarMaximum = 30.0; // double\n"
            + "      :colorBarMinimum = 0.03; // double\n"
            + "      :colorBarScale = \"Log\";\n"
            + "      :coordsys = \"geographic\";\n"
            + "      :fraction_digits = 2; // int\n"
            + "      :ioos_category = \"Ocean Color\";\n"
            + "      :long_name = \"Concentration Of Chlorophyll In Sea Water\";\n"
            + "      :missing_value = -9999999.0f; // float\n"
            + "      :standard_name = \"concentration_of_chlorophyll_in_sea_water\";\n"
            + "      :units = \"mg m-3\";\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "RESULTS=\n" + results);

    expected =
        "  :title = \"Chlorophyll-a, Aqua MODIS, NPP, 2002-2013, DEPRECATED OLDER VERSION (8 Day Composite)\";\n"
            + "  :Westernmost_Easting = 224.98437319134158; // double\n"
            + // note updated value
            "}\n";
    int po10 = results.indexOf("  :title");
    Test.ensureEqual(results.substring(po10), expected, "RESULTS=\n" + results);

    // .ncoJson
    String2.log("\n*** EDDGridFromDap test get .ncoJson data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "chlorophyll[(2002-07-08):(2002-07-16)][][(29):100:(50)][(225):100:(247)]",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Data",
            ".ncoJson");
    // 2017-08-03 I tested the resulting file for validity at https://jsonlint.com/
    String2.log(">> NCO JSON " + EDStatic.fullTestCacheDirectory + tName);
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "{\n"
            + "  \"attributes\": {\n"
            + "    \"acknowledgement\": {\"type\": \"char\", \"data\": \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\"},\n"
            + "    \"cdm_data_type\": {\"type\": \"char\", \"data\": \"Grid\"},\n"
            + "    \"composite\": {\"type\": \"char\", \"data\": \"true\"},\n"
            + "    \"contributor_name\": {\"type\": \"char\", \"data\": \"NASA GSFC (OBPG)\"},\n"
            + "    \"contributor_role\": {\"type\": \"char\", \"data\": \"Source of level 2 data.\"},\n"
            + "    \"Conventions\": {\"type\": \"char\", \"data\": \"COARDS, CF-1.6, ACDD-1.3\"},\n"
            + "    \"creator_email\": {\"type\": \"char\", \"data\": \"erd.data@noaa.gov\"},\n"
            + "    \"creator_name\": {\"type\": \"char\", \"data\": \"NOAA NMFS SWFSC ERD\"},\n"
            + "    \"creator_type\": {\"type\": \"char\", \"data\": \"institution\"},\n"
            + "    \"creator_url\": {\"type\": \"char\", \"data\": \"https://www.pfeg.noaa.gov\"},\n"
            + "    \"date_created\": {\"type\": \"char\", \"data\": \"2013-11-01\"},\n"
            + "    \"date_issued\": {\"type\": \"char\", \"data\": \"2013-11-01\"},\n"
            + "    \"Easternmost_Easting\": {\"type\": \"double\", \"data\": 245.82011806922097},\n"
            + "    \"geospatial_lat_max\": {\"type\": \"double\", \"data\": 49.82403334105115},\n"
            + "    \"geospatial_lat_min\": {\"type\": \"double\", \"data\": 28.985876360268577},\n"
            + "    \"geospatial_lat_resolution\": {\"type\": \"double\", \"data\": 0.041676313961565174},\n"
            + "    \"geospatial_lat_units\": {\"type\": \"char\", \"data\": \"degrees_north\"},\n"
            + "    \"geospatial_lon_max\": {\"type\": \"double\", \"data\": 245.82011806922097},\n"
            + "    \"geospatial_lon_min\": {\"type\": \"double\", \"data\": 224.98437319134158},\n"
            + "    \"geospatial_lon_resolution\": {\"type\": \"double\", \"data\": 0.04167148975575877},\n"
            + "    \"geospatial_lon_units\": {\"type\": \"char\", \"data\": \"degrees_east\"},\n"
            + "    \"geospatial_vertical_max\": {\"type\": \"double\", \"data\": 0.0},\n"
            + "    \"geospatial_vertical_min\": {\"type\": \"double\", \"data\": 0.0},\n"
            + "    \"geospatial_vertical_positive\": {\"type\": \"char\", \"data\": \"up\"},\n"
            + "    \"geospatial_vertical_units\": {\"type\": \"char\", \"data\": \"m\"},\n"
            + "    \"history\": {\"type\": \"char\", \"data\": \"NASA GSFC (OBPG)\\n2013-11-01T20:42:40Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD\\n";
    // 2017-07-31T20:37:46Z
    // https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day\\n2017-07-31T20:37:46Z
    Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);

    expected =
        "http://127.0.0.1:8080/cwexperimental/griddap/erdMHchla8day.ncoJson?chlorophyll[(2002-07-08):(2002-07-16)][][(29):100:(50)][(225):100:(247)]\"},\n"
            + "    \"infoUrl\": {\"type\": \"char\", \"data\": \"https://coastwatch.pfeg.noaa.gov/infog/MH_chla_las.html\"},\n"
            + "    \"institution\": {\"type\": \"char\", \"data\": \"NOAA NMFS SWFSC ERD\"},\n"
            + "    \"keywords\": {\"type\": \"char\", \"data\": \"8-day, aqua, chemistry, chlorophyll, chlorophyll-a, coastwatch, color, concentration, concentration_of_chlorophyll_in_sea_water, day, degrees, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, global, modis, noaa, npp, ocean, ocean color, oceans, quality, science, science quality, sea, seawater, water, wcn\"},\n"
            + "    \"keywords_vocabulary\": {\"type\": \"char\", \"data\": \"GCMD Science Keywords\"},\n"
            + "    \"license\": {\"type\": \"char\", \"data\": \"The data may be used and redistributed for free but is not intended\\nfor legal use, since it may contain inaccuracies. Neither the data\\nContributor, ERD, NOAA, nor the United States Government, nor any\\nof their employees or contractors, makes any warranty, express or\\nimplied, including warranties of merchantability and fitness for a\\nparticular purpose, or assumes any legal liability for the accuracy,\\ncompleteness, or usefulness, of this information.\"},\n"
            + "    \"naming_authority\": {\"type\": \"char\", \"data\": \"gov.noaa.pfeg.coastwatch\"},\n"
            + "    \"Northernmost_Northing\": {\"type\": \"double\", \"data\": 49.82403334105115},\n"
            + "    \"origin\": {\"type\": \"char\", \"data\": \"NASA GSFC (OBPG)\"},\n"
            + "    \"processing_level\": {\"type\": \"char\", \"data\": \"3\"},\n"
            + "    \"project\": {\"type\": \"char\", \"data\": \"CoastWatch (https://coastwatch.noaa.gov/)\"},\n"
            + "    \"projection\": {\"type\": \"char\", \"data\": \"geographic\"},\n"
            + "    \"projection_type\": {\"type\": \"char\", \"data\": \"mapped\"},\n"
            + "    \"publisher_email\": {\"type\": \"char\", \"data\": \"erd.data@noaa.gov\"},\n"
            + "    \"publisher_name\": {\"type\": \"char\", \"data\": \"NOAA NMFS SWFSC ERD\"},\n"
            + "    \"publisher_type\": {\"type\": \"char\", \"data\": \"institution\"},\n"
            + "    \"publisher_url\": {\"type\": \"char\", \"data\": \"https://www.pfeg.noaa.gov\"},\n"
            + "    \"references\": {\"type\": \"char\", \"data\": \"Aqua/MODIS information: https://oceancolor.gsfc.nasa.gov/ . MODIS information: https://coastwatch.noaa.gov/modis_ocolor_overview.html .\"},\n"
            + "    \"satellite\": {\"type\": \"char\", \"data\": \"Aqua\"},\n"
            + "    \"sensor\": {\"type\": \"char\", \"data\": \"MODIS\"},\n"
            + "    \"source\": {\"type\": \"char\", \"data\": \"satellite observation: Aqua, MODIS\"},\n"
            + "    \"sourceUrl\": {\"type\": \"char\", \"data\": \"https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day\"},\n"
            + "    \"Southernmost_Northing\": {\"type\": \"double\", \"data\": 28.985876360268577},\n"
            + "    \"standard_name_vocabulary\": {\"type\": \"char\", \"data\": \"CF Standard Name Table v70\"},\n"
            + "    \"summary\": {\"type\": \"char\", \"data\": \"NOAA CoastWatch distributes chlorophyll-a concentration data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.   This is Science Quality data.\"},\n"
            + "    \"time_coverage_end\": {\"type\": \"char\", \"data\": \"2002-07-16T00:00:00Z\"},\n"
            + "    \"time_coverage_start\": {\"type\": \"char\", \"data\": \"2002-07-08T00:00:00Z\"},\n"
            + "    \"title\": {\"type\": \"char\", \"data\": \"Chlorophyll-a, Aqua MODIS, NPP, 2002-2013, DEPRECATED OLDER VERSION (8 Day Composite)\"},\n"
            + "    \"Westernmost_Easting\": {\"type\": \"double\", \"data\": 224.98437319134158}\n"
            + "  },\n"
            + "  \"dimensions\": {\n"
            + "    \"time\": 2,\n"
            + "    \"altitude\": 1,\n"
            + "    \"latitude\": 6,\n"
            + "    \"longitude\": 6\n"
            + "  },\n"
            + "  \"variables\": {\n"
            + "    \"time\": {\n"
            + "      \"shape\": [\"time\"],\n"
            + "      \"type\": \"double\",\n"
            + "      \"attributes\": {\n"
            + "        \"_CoordinateAxisType\": {\"type\": \"char\", \"data\": \"Time\"},\n"
            + "        \"actual_range\": {\"type\": \"double\", \"data\": [1.0260864E9, 1.0267776E9]},\n"
            + "        \"axis\": {\"type\": \"char\", \"data\": \"T\"},\n"
            + "        \"fraction_digits\": {\"type\": \"int\", \"data\": 0},\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Time\"},\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Centered Time\"},\n"
            + "        \"standard_name\": {\"type\": \"char\", \"data\": \"time\"},\n"
            + "        \"time_origin\": {\"type\": \"char\", \"data\": \"01-JAN-1970 00:00:00\"},\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"seconds since 1970-01-01T00:00:00Z\"}\n"
            + "      },\n"
            + "      \"data\": [1.0260864E9, 1.0267776E9]\n"
            + "    },\n"
            + "    \"altitude\": {\n"
            + "      \"shape\": [\"altitude\"],\n"
            + "      \"type\": \"double\",\n"
            + "      \"attributes\": {\n"
            + "        \"_CoordinateAxisType\": {\"type\": \"char\", \"data\": \"Height\"},\n"
            + "        \"_CoordinateZisPositive\": {\"type\": \"char\", \"data\": \"up\"},\n"
            + "        \"actual_range\": {\"type\": \"double\", \"data\": [0.0, 0.0]},\n"
            + "        \"axis\": {\"type\": \"char\", \"data\": \"Z\"},\n"
            + "        \"fraction_digits\": {\"type\": \"int\", \"data\": 0},\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Location\"},\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Altitude\"},\n"
            + "        \"positive\": {\"type\": \"char\", \"data\": \"up\"},\n"
            + "        \"standard_name\": {\"type\": \"char\", \"data\": \"altitude\"},\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"m\"}\n"
            + "      },\n"
            + "      \"data\": [0.0]\n"
            + "    },\n"
            + "    \"latitude\": {\n"
            + "      \"shape\": [\"latitude\"],\n"
            + "      \"type\": \"double\",\n"
            + "      \"attributes\": {\n"
            + "        \"_CoordinateAxisType\": {\"type\": \"char\", \"data\": \"Lat\"},\n"
            + "        \"actual_range\": {\"type\": \"double\", \"data\": [28.985876360268577, 49.82403334105115]},\n"
            + "        \"axis\": {\"type\": \"char\", \"data\": \"Y\"},\n"
            + "        \"coordsys\": {\"type\": \"char\", \"data\": \"geographic\"},\n"
            + "        \"fraction_digits\": {\"type\": \"int\", \"data\": 4},\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Location\"},\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Latitude\"},\n"
            + "        \"point_spacing\": {\"type\": \"char\", \"data\": \"even\"},\n"
            + "        \"standard_name\": {\"type\": \"char\", \"data\": \"latitude\"},\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"degrees_north\"}\n"
            + "      },\n"
            + "      \"data\": [28.985876360268577, 33.153507756425086, 37.32113915258161, 41.48877054873813, 45.65640194489464, 49.82403334105115]\n"
            + "    },\n"
            + "    \"longitude\": {\n"
            + "      \"shape\": [\"longitude\"],\n"
            + "      \"type\": \"double\",\n"
            + "      \"attributes\": {\n"
            + "        \"_CoordinateAxisType\": {\"type\": \"char\", \"data\": \"Lon\"},\n"
            + "        \"actual_range\": {\"type\": \"double\", \"data\": [224.98437319134158, 245.82011806922097]},\n"
            + "        \"axis\": {\"type\": \"char\", \"data\": \"X\"},\n"
            + "        \"coordsys\": {\"type\": \"char\", \"data\": \"geographic\"},\n"
            + "        \"fraction_digits\": {\"type\": \"int\", \"data\": 4},\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Location\"},\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Longitude\"},\n"
            + "        \"point_spacing\": {\"type\": \"char\", \"data\": \"even\"},\n"
            + "        \"standard_name\": {\"type\": \"char\", \"data\": \"longitude\"},\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"degrees_east\"}\n"
            + "      },\n"
            + "      \"data\": [224.98437319134158, 229.15152216691746, 233.31867114249334, 237.48582011806923, 241.65296909364508, 245.82011806922097]\n"
            + "    },\n"
            + "    \"chlorophyll\": {\n"
            + "      \"shape\": [\"time\", \"altitude\", \"latitude\", \"longitude\"],\n"
            + "      \"type\": \"float\",\n"
            + "      \"attributes\": {\n"
            + "        \"_FillValue\": {\"type\": \"float\", \"data\": -9999999.0},\n"
            + "        \"colorBarMaximum\": {\"type\": \"double\", \"data\": 30.0},\n"
            + "        \"colorBarMinimum\": {\"type\": \"double\", \"data\": 0.03},\n"
            + "        \"colorBarScale\": {\"type\": \"char\", \"data\": \"Log\"},\n"
            + "        \"coordsys\": {\"type\": \"char\", \"data\": \"geographic\"},\n"
            + "        \"fraction_digits\": {\"type\": \"int\", \"data\": 2},\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Ocean Color\"},\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Concentration Of Chlorophyll In Sea Water\"},\n"
            + "        \"missing_value\": {\"type\": \"float\", \"data\": -9999999.0},\n"
            + "        \"standard_name\": {\"type\": \"char\", \"data\": \"concentration_of_chlorophyll_in_sea_water\"},\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"mg m-3\"}\n"
            + "      },\n"
            + "      \"data\":\n"
            + "[ [ [ [ -9999999.0, -9999999.0, 0.05499, -9999999.0, -9999999.0, -9999999.0 ],\n"
            + "[ 0.08882, 0.08416, -9999999.0, 0.34349, -9999999.0, -9999999.0 ],\n"
            + "[ 0.07901, 0.07788, 0.20406, -9999999.0, -9999999.0, -9999999.0 ],\n"
            + "[ 0.10195, 0.13437, 0.14067, -9999999.0, -9999999.0, -9999999.0 ],\n"
            + "[ -9999999.0, 0.38725, 0.14722, -9999999.0, -9999999.0, -9999999.0 ],\n"
            + "[ 0.45281, 0.1994, -9999999.0, -9999999.0, -9999999.0, -9999999.0 ] ] ],\n"
            + "[ [ [ -9999999.0, -9999999.0, -9999999.0, -9999999.0, 0.07097, -9999999.0 ],\n"
            + "[ 0.08856, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0 ],\n"
            + "[ 0.08815, 0.08337, -9999999.0, -9999999.0, -9999999.0, -9999999.0 ],\n"
            + "[ -9999999.0, -9999999.0, 0.15922, -9999999.0, -9999999.0, -9999999.0 ],\n"
            + "[ -9999999.0, 0.32735, 0.11346, -9999999.0, -9999999.0, -9999999.0 ],\n"
            + "[ -9999999.0, 0.15682, -9999999.0, -9999999.0, -9999999.0, -9999999.0 ] ] ] ]\n"
            + "    }\n"
            + "  }\n"
            + "}\n";
    // 2017-08-01 I verified these number by hand in ERDDAP with
    // https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdMHchla8day.htmlTable?chlorophyll[(2002-07-08):1:(2002-07-16)][(0.0):1:(0.0)][(29):100:(50)][(225):100:(247)]
    po10 = results.indexOf(expected.substring(0, 40));
    if (po10 < 0) String2.log("results=\n" + results + "\npo10=" + po10);
    Test.ensureEqual(results.substring(po10), expected, "RESULTS=\n" + results);

    // .ncoJson with jsonp
    String2.log("\n*** EDDGridFromDap test get .ncoJson data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "chlorophyll[(2002-07-08):(2002-07-16)][][(29):100:(50)][(225):100:(247)]&.jsonp=myFunctionName",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Datajp",
            ".ncoJson");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "myFunctionName("
            + "{\n"
            + "  \"attributes\": {\n"
            + "    \"acknowledgement\": {\"type\": \"char\", \"data\": \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\"},\n"
            + "    \"cdm_data_type\": {\"type\": \"char\", \"data\": \"Grid\"},\n"
            + "    \"composite\": {\"type\": \"char\", \"data\": \"true\"},\n"
            + "    \"contributor_name\": {\"type\": \"char\", \"data\": \"NASA GSFC (OBPG)\"},\n"
            + "    \"contributor_role\": {\"type\": \"char\", \"data\": \"Source of level 2 data.\"},\n"
            + "    \"Conventions\": {\"type\": \"char\", \"data\": \"COARDS, CF-1.6, ACDD-1.3\"},\n"
            + "    \"creator_email\": {\"type\": \"char\", \"data\": \"erd.data@noaa.gov\"},\n"
            + "    \"creator_name\": {\"type\": \"char\", \"data\": \"NOAA NMFS SWFSC ERD\"},\n"
            + "    \"creator_type\": {\"type\": \"char\", \"data\": \"institution\"},\n"
            + "    \"creator_url\": {\"type\": \"char\", \"data\": \"https://www.pfeg.noaa.gov\"},\n"
            + "    \"date_created\": {\"type\": \"char\", \"data\": \"2013-11-01\"},\n"
            + "    \"date_issued\": {\"type\": \"char\", \"data\": \"2013-11-01\"},\n"
            + "    \"Easternmost_Easting\": {\"type\": \"double\", \"data\": 245.82011806922097},\n"
            + "    \"geospatial_lat_max\": {\"type\": \"double\", \"data\": 49.82403334105115},\n"
            + "    \"geospatial_lat_min\": {\"type\": \"double\", \"data\": 28.985876360268577},\n"
            + "    \"geospatial_lat_resolution\": {\"type\": \"double\", \"data\": 0.041676313961565174},\n"
            + "    \"geospatial_lat_units\": {\"type\": \"char\", \"data\": \"degrees_north\"},\n"
            + "    \"geospatial_lon_max\": {\"type\": \"double\", \"data\": 245.82011806922097},\n"
            + "    \"geospatial_lon_min\": {\"type\": \"double\", \"data\": 224.98437319134158},\n"
            + "    \"geospatial_lon_resolution\": {\"type\": \"double\", \"data\": 0.04167148975575877},\n"
            + "    \"geospatial_lon_units\": {\"type\": \"char\", \"data\": \"degrees_east\"},\n"
            + "    \"geospatial_vertical_max\": {\"type\": \"double\", \"data\": 0.0},\n"
            + "    \"geospatial_vertical_min\": {\"type\": \"double\", \"data\": 0.0},\n"
            + "    \"geospatial_vertical_positive\": {\"type\": \"char\", \"data\": \"up\"},\n"
            + "    \"geospatial_vertical_units\": {\"type\": \"char\", \"data\": \"m\"},\n"
            + "    \"history\": {\"type\": \"char\", \"data\": \"NASA GSFC (OBPG)\\n2013-11-01T20:42:40Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD\\n";
    // 2017-07-31T20:37:46Z
    // https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day\\n2017-07-31T20:37:46Z
    Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);

    expected =
        "http://127.0.0.1:8080/cwexperimental/griddap/erdMHchla8day.ncoJson?chlorophyll[(2002-07-08):(2002-07-16)][][(29):100:(50)][(225):100:(247)]&.jsonp=myFunctionName\"},\n"
            + "    \"infoUrl\": {\"type\": \"char\", \"data\": \"https://coastwatch.pfeg.noaa.gov/infog/MH_chla_las.html\"},\n"
            + "    \"institution\": {\"type\": \"char\", \"data\": \"NOAA NMFS SWFSC ERD\"},\n"
            + "    \"keywords\": {\"type\": \"char\", \"data\": \"8-day, aqua, chemistry, chlorophyll, chlorophyll-a, coastwatch, color, concentration, concentration_of_chlorophyll_in_sea_water, day, degrees, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, global, modis, noaa, npp, ocean, ocean color, oceans, quality, science, science quality, sea, seawater, water, wcn\"},\n"
            + "    \"keywords_vocabulary\": {\"type\": \"char\", \"data\": \"GCMD Science Keywords\"},\n"
            + "    \"license\": {\"type\": \"char\", \"data\": \"The data may be used and redistributed for free but is not intended\\nfor legal use, since it may contain inaccuracies. Neither the data\\nContributor, ERD, NOAA, nor the United States Government, nor any\\nof their employees or contractors, makes any warranty, express or\\nimplied, including warranties of merchantability and fitness for a\\nparticular purpose, or assumes any legal liability for the accuracy,\\ncompleteness, or usefulness, of this information.\"},\n"
            + "    \"naming_authority\": {\"type\": \"char\", \"data\": \"gov.noaa.pfeg.coastwatch\"},\n"
            + "    \"Northernmost_Northing\": {\"type\": \"double\", \"data\": 49.82403334105115},\n"
            + "    \"origin\": {\"type\": \"char\", \"data\": \"NASA GSFC (OBPG)\"},\n"
            + "    \"processing_level\": {\"type\": \"char\", \"data\": \"3\"},\n"
            + "    \"project\": {\"type\": \"char\", \"data\": \"CoastWatch (https://coastwatch.noaa.gov/)\"},\n"
            + "    \"projection\": {\"type\": \"char\", \"data\": \"geographic\"},\n"
            + "    \"projection_type\": {\"type\": \"char\", \"data\": \"mapped\"},\n"
            + "    \"publisher_email\": {\"type\": \"char\", \"data\": \"erd.data@noaa.gov\"},\n"
            + "    \"publisher_name\": {\"type\": \"char\", \"data\": \"NOAA NMFS SWFSC ERD\"},\n"
            + "    \"publisher_type\": {\"type\": \"char\", \"data\": \"institution\"},\n"
            + "    \"publisher_url\": {\"type\": \"char\", \"data\": \"https://www.pfeg.noaa.gov\"},\n"
            + "    \"references\": {\"type\": \"char\", \"data\": \"Aqua/MODIS information: https://oceancolor.gsfc.nasa.gov/ . MODIS information: https://coastwatch.noaa.gov/modis_ocolor_overview.html .\"},\n"
            + "    \"satellite\": {\"type\": \"char\", \"data\": \"Aqua\"},\n"
            + "    \"sensor\": {\"type\": \"char\", \"data\": \"MODIS\"},\n"
            + "    \"source\": {\"type\": \"char\", \"data\": \"satellite observation: Aqua, MODIS\"},\n"
            + "    \"sourceUrl\": {\"type\": \"char\", \"data\": \"https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day\"},\n"
            + "    \"Southernmost_Northing\": {\"type\": \"double\", \"data\": 28.985876360268577},\n"
            + "    \"standard_name_vocabulary\": {\"type\": \"char\", \"data\": \"CF Standard Name Table v70\"},\n"
            + "    \"summary\": {\"type\": \"char\", \"data\": \"NOAA CoastWatch distributes chlorophyll-a concentration data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.   This is Science Quality data.\"},\n"
            + "    \"time_coverage_end\": {\"type\": \"char\", \"data\": \"2002-07-16T00:00:00Z\"},\n"
            + "    \"time_coverage_start\": {\"type\": \"char\", \"data\": \"2002-07-08T00:00:00Z\"},\n"
            + "    \"title\": {\"type\": \"char\", \"data\": \"Chlorophyll-a, Aqua MODIS, NPP, 2002-2013, DEPRECATED OLDER VERSION (8 Day Composite)\"},\n"
            + "    \"Westernmost_Easting\": {\"type\": \"double\", \"data\": 224.98437319134158}\n"
            + "  },\n"
            + "  \"dimensions\": {\n"
            + "    \"time\": 2,\n"
            + "    \"altitude\": 1,\n"
            + "    \"latitude\": 6,\n"
            + "    \"longitude\": 6\n"
            + "  },\n"
            + "  \"variables\": {\n"
            + "    \"time\": {\n"
            + "      \"shape\": [\"time\"],\n"
            + "      \"type\": \"double\",\n"
            + "      \"attributes\": {\n"
            + "        \"_CoordinateAxisType\": {\"type\": \"char\", \"data\": \"Time\"},\n"
            + "        \"actual_range\": {\"type\": \"double\", \"data\": [1.0260864E9, 1.0267776E9]},\n"
            + "        \"axis\": {\"type\": \"char\", \"data\": \"T\"},\n"
            + "        \"fraction_digits\": {\"type\": \"int\", \"data\": 0},\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Time\"},\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Centered Time\"},\n"
            + "        \"standard_name\": {\"type\": \"char\", \"data\": \"time\"},\n"
            + "        \"time_origin\": {\"type\": \"char\", \"data\": \"01-JAN-1970 00:00:00\"},\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"seconds since 1970-01-01T00:00:00Z\"}\n"
            + "      },\n"
            + "      \"data\": [1.0260864E9, 1.0267776E9]\n"
            + "    },\n"
            + "    \"altitude\": {\n"
            + "      \"shape\": [\"altitude\"],\n"
            + "      \"type\": \"double\",\n"
            + "      \"attributes\": {\n"
            + "        \"_CoordinateAxisType\": {\"type\": \"char\", \"data\": \"Height\"},\n"
            + "        \"_CoordinateZisPositive\": {\"type\": \"char\", \"data\": \"up\"},\n"
            + "        \"actual_range\": {\"type\": \"double\", \"data\": [0.0, 0.0]},\n"
            + "        \"axis\": {\"type\": \"char\", \"data\": \"Z\"},\n"
            + "        \"fraction_digits\": {\"type\": \"int\", \"data\": 0},\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Location\"},\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Altitude\"},\n"
            + "        \"positive\": {\"type\": \"char\", \"data\": \"up\"},\n"
            + "        \"standard_name\": {\"type\": \"char\", \"data\": \"altitude\"},\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"m\"}\n"
            + "      },\n"
            + "      \"data\": [0.0]\n"
            + "    },\n"
            + "    \"latitude\": {\n"
            + "      \"shape\": [\"latitude\"],\n"
            + "      \"type\": \"double\",\n"
            + "      \"attributes\": {\n"
            + "        \"_CoordinateAxisType\": {\"type\": \"char\", \"data\": \"Lat\"},\n"
            + "        \"actual_range\": {\"type\": \"double\", \"data\": [28.985876360268577, 49.82403334105115]},\n"
            + "        \"axis\": {\"type\": \"char\", \"data\": \"Y\"},\n"
            + "        \"coordsys\": {\"type\": \"char\", \"data\": \"geographic\"},\n"
            + "        \"fraction_digits\": {\"type\": \"int\", \"data\": 4},\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Location\"},\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Latitude\"},\n"
            + "        \"point_spacing\": {\"type\": \"char\", \"data\": \"even\"},\n"
            + "        \"standard_name\": {\"type\": \"char\", \"data\": \"latitude\"},\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"degrees_north\"}\n"
            + "      },\n"
            + "      \"data\": [28.985876360268577, 33.153507756425086, 37.32113915258161, 41.48877054873813, 45.65640194489464, 49.82403334105115]\n"
            + "    },\n"
            + "    \"longitude\": {\n"
            + "      \"shape\": [\"longitude\"],\n"
            + "      \"type\": \"double\",\n"
            + "      \"attributes\": {\n"
            + "        \"_CoordinateAxisType\": {\"type\": \"char\", \"data\": \"Lon\"},\n"
            + "        \"actual_range\": {\"type\": \"double\", \"data\": [224.98437319134158, 245.82011806922097]},\n"
            + "        \"axis\": {\"type\": \"char\", \"data\": \"X\"},\n"
            + "        \"coordsys\": {\"type\": \"char\", \"data\": \"geographic\"},\n"
            + "        \"fraction_digits\": {\"type\": \"int\", \"data\": 4},\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Location\"},\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Longitude\"},\n"
            + "        \"point_spacing\": {\"type\": \"char\", \"data\": \"even\"},\n"
            + "        \"standard_name\": {\"type\": \"char\", \"data\": \"longitude\"},\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"degrees_east\"}\n"
            + "      },\n"
            + "      \"data\": [224.98437319134158, 229.15152216691746, 233.31867114249334, 237.48582011806923, 241.65296909364508, 245.82011806922097]\n"
            + "    },\n"
            + "    \"chlorophyll\": {\n"
            + "      \"shape\": [\"time\", \"altitude\", \"latitude\", \"longitude\"],\n"
            + "      \"type\": \"float\",\n"
            + "      \"attributes\": {\n"
            + "        \"_FillValue\": {\"type\": \"float\", \"data\": -9999999.0},\n"
            + "        \"colorBarMaximum\": {\"type\": \"double\", \"data\": 30.0},\n"
            + "        \"colorBarMinimum\": {\"type\": \"double\", \"data\": 0.03},\n"
            + "        \"colorBarScale\": {\"type\": \"char\", \"data\": \"Log\"},\n"
            + "        \"coordsys\": {\"type\": \"char\", \"data\": \"geographic\"},\n"
            + "        \"fraction_digits\": {\"type\": \"int\", \"data\": 2},\n"
            + "        \"ioos_category\": {\"type\": \"char\", \"data\": \"Ocean Color\"},\n"
            + "        \"long_name\": {\"type\": \"char\", \"data\": \"Concentration Of Chlorophyll In Sea Water\"},\n"
            + "        \"missing_value\": {\"type\": \"float\", \"data\": -9999999.0},\n"
            + "        \"standard_name\": {\"type\": \"char\", \"data\": \"concentration_of_chlorophyll_in_sea_water\"},\n"
            + "        \"units\": {\"type\": \"char\", \"data\": \"mg m-3\"}\n"
            + "      },\n"
            + "      \"data\":\n"
            + "[ [ [ [ -9999999.0, -9999999.0, 0.05499, -9999999.0, -9999999.0, -9999999.0 ],\n"
            + "[ 0.08882, 0.08416, -9999999.0, 0.34349, -9999999.0, -9999999.0 ],\n"
            + "[ 0.07901, 0.07788, 0.20406, -9999999.0, -9999999.0, -9999999.0 ],\n"
            + "[ 0.10195, 0.13437, 0.14067, -9999999.0, -9999999.0, -9999999.0 ],\n"
            + "[ -9999999.0, 0.38725, 0.14722, -9999999.0, -9999999.0, -9999999.0 ],\n"
            + "[ 0.45281, 0.1994, -9999999.0, -9999999.0, -9999999.0, -9999999.0 ] ] ],\n"
            + "[ [ [ -9999999.0, -9999999.0, -9999999.0, -9999999.0, 0.07097, -9999999.0 ],\n"
            + "[ 0.08856, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0 ],\n"
            + "[ 0.08815, 0.08337, -9999999.0, -9999999.0, -9999999.0, -9999999.0 ],\n"
            + "[ -9999999.0, -9999999.0, 0.15922, -9999999.0, -9999999.0, -9999999.0 ],\n"
            + "[ -9999999.0, 0.32735, 0.11346, -9999999.0, -9999999.0, -9999999.0 ],\n"
            + "[ -9999999.0, 0.15682, -9999999.0, -9999999.0, -9999999.0, -9999999.0 ] ] ] ]\n"
            + "    }\n"
            + "  }\n"
            + "}\n"
            + ")";
    // 2017-08-01 I verified these number by hand in ERDDAP with
    // https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdMHchla8day.htmlTable?chlorophyll[(2002-07-08):1:(2002-07-16)][(0.0):1:(0.0)][(29):100:(50)][(225):100:(247)]
    po10 = results.indexOf(expected.substring(0, 40));
    if (po10 < 0) String2.log("results=\n" + results + "\npo10=" + po10);
    Test.ensureEqual(results.substring(po10), expected, "RESULTS=\n" + results);

    // .tsv
    String2.log("\n*** EDDGridFromDap test get .TSV data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Data",
            ".tsv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "time\taltitude\tlatitude\tlongitude\tchlorophyll\n"
            + "UTC\tm\tdegrees_north\tdegrees_east\tmg m-3\n"
            + "2007-02-06T00:00:00Z\t0.0\t28.985876360268577\t224.98437319134158\tNaN\n"
            + "2007-02-06T00:00:00Z\t0.0\t28.985876360268577\t225.40108808889917\tNaN\n"
            +
            // pre 2010-10-26 was
            // "2007-02-06T00:00:00Z\t0.0\t28.985876360268577\t225.81780298645677\t0.099\n"
            // +
            // "2007-02-06T00:00:00Z\t0.0\t28.985876360268577\t226.23451788401434\t0.118\n";
            // pre 2012-08-17 was
            // "2007-02-06T00:00:00Z\t0.0\t28.985876360268577\t225.81780298645677\t0.10655\n"
            // +
            // "2007-02-06T00:00:00Z\t0.0\t28.985876360268577\t226.23451788401434\t0.12478\n";
            "2007-02-06T00:00:00Z\t0.0\t28.985876360268577\t225.81780298645677\t0.11093\n"
            + "2007-02-06T00:00:00Z\t0.0\t28.985876360268577\t226.23451788401434\t0.12439\n";
    Test.ensureTrue(results.indexOf(expected) == 0, "RESULTS=\n" + results);
    expected =
        // pre 2010-10-26 was
        // "2007-02-06T00:00:00Z\t0.0\t49.407270201435495\t232.06852644982058\t0.37\n";
        // pre 2012-08-17 was
        // "2007-02-06T00:00:00Z\t0.0\t49.407270201435495\t232.06852644982058\t0.58877\n";
        "2007-02-06T00:00:00Z\t0.0\t49.407270201435495\t232.06852644982058\t0.56545\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "RESULTS=\n" + results);

    // .xhtml
    String2.log("\n*** EDDGridFromDap test get .XHTMLTABLE data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Data",
            ".xhtml");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n"
            + "  \"https://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n"
            + "<html xmlns=\"https://www.w3.org/1999/xhtml\">\n"
            + "<head>\n";
    Test.ensureTrue(results.indexOf(expected) == 0, "RESULTS=\n" + results);
    expected =
        "<th>time</th>\n"
            + "<th>altitude</th>\n"
            + "<th>latitude</th>\n"
            + "<th>longitude</th>\n"
            + "<th>chlorophyll</th>\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<th>UTC</th>\n"
            + "<th>m</th>\n"
            + "<th>degrees_north</th>\n"
            + "<th>degrees_east</th>\n"
            + "<th>mg m-3</th>\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td>2007-02-06T00:00:00Z</td>\n"
            + "<td class=\"R\">0.0</td>\n"
            + "<td class=\"R\">28.985876360268577</td>\n"
            + "<td class=\"R\">224.98437319134158</td>\n"
            + "<td></td>\n"
            + // missing value is ""
            "</tr>";
    int po11 = results.indexOf("<th>time</th>");
    Test.ensureEqual(
        results.substring(po11, po11 + expected.length()),
        expected,
        "RESULTS=\n" + results.substring(0, 1000));
    expected =
        "<tr>\n"
            + "<td>2007-02-06T00:00:00Z</td>\n"
            + "<td class=\"R\">0.0</td>\n"
            + "<td class=\"R\">49.82403334105115</td>\n"
            + "<td class=\"R\">246.65354786433613</td>\n"
            + "<td></td>\n"
            + "</tr>\n"
            + "</table>\n"
            + "</body>\n"
            + "</html>\n";
    Test.ensureEqual(
        results.substring(results.length() - expected.length()),
        expected,
        "RESULTS=\n" + results.substring(results.length() - expected.length()));

    // test " in attributes
    EDDGrid tedg = (EDDGridFromDap) EDDGridFromDap.oneFromDatasetsXml(null, "erdSGchla8day");
    String2.log("\n***raw references=" + tedg.addGlobalAttributes.getString("references"));
    tName =
        tedg.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            tedg.className() + "Quotes",
            ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    int po9 = results.indexOf("The 4th Pacific");
    String2.log("*** in results: " + results.substring(po9 - 10, po9 + 15));
    expected = " Proceedings of \"\"The 4th Pacific";
    Test.ensureTrue(results.indexOf(expected) < 0, "\nresults=\n" + results);
    expected = " Proceedings of \\\"The 4th Pacific";
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);

    // test loading other datasets
    EDDGridFromDap.oneFromDatasetsXml(null, "erdAGssta1day");
    // oneFromDatasetsXml(null, "erdAGssta8day");
    // oneFromDatasetsXml(null, "erdAGsstamday");
    // */

  }

  @org.junit.jupiter.api.Test
  void testGenerateDatasetsXml() throws Throwable {
    // testVerboseOn();
    // don't test local dataset because of dns/numericIP problems
    // this dataset is good test because it has 2 dimension combos
    String url = "http://apdrc.soest.hawaii.edu/dods/public_data/SODA/soda_pop2.2.4";
    // String2.log("\n*** EDDGridFromDap.testGenerateDatasetsXml");
    int language = 0;
    String suggDatasetID = EDDGridFromDap.suggestDatasetID(url + "?[time][lev][lat][lon]");
    String suggDatasetID2 = EDDGridFromDap.suggestDatasetID(url + "?[time][lat][lon]");
    String expected1 =
        "<dataset type=\"EDDGridFromDap\" datasetID=\""
            + suggDatasetID
            + "\" active=\"true\">\n"
            + "    <sourceUrl>http://apdrc.soest.hawaii.edu/dods/public_data/SODA/soda_pop2.2.4</sourceUrl>\n"
            + "    <reloadEveryNMinutes>43200</reloadEveryNMinutes>\n"
            + "    <!-- sourceAttributes>\n"
            + "        <att name=\"Conventions\">COARDS</att>\n"
            + "        <att name=\"dataType\">Grid</att>\n"
            + "        <att name=\"documentation\">http://apdrc.soest.hawaii.edu/datadoc/soda_2.2.4.php</att>\n"
            + "        <att name=\"history\">DDD MMM dd hh:mm:ss HST yyyy : imported by GrADS Data Server 2.0</att>\n"
            + "        <att name=\"title\">SODA v2.2.4 monthly means</att>\n"
            + "    </sourceAttributes -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cdm_data_type\">Grid</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
            + "        <att name=\"creator_email\">chepurin@umd.edu</att>\n"
            + "        <att name=\"creator_name\">HAWAII SOEST</att>\n"
            + "        <att name=\"creator_type\">institution</att>\n"
            + "        <att name=\"creator_url\">https://www.atmos.umd.edu/~ocean/</att>\n"
            + "        <att name=\"infoUrl\">http://apdrc.soest.hawaii.edu/datadoc/soda_2.2.4.php</att>\n"
            + "        <att name=\"institution\">HAWAII SOEST</att>\n"
            + "        <att name=\"keywords\">assimilation, currents, data, degc, density, depth, earth, Earth Science &gt; Oceans &gt; Salinity/Density &gt; Salinity, hawaii, latitude, longitude, means, meridional, month, monthly, ocean, oceans, pop2.2.4, practical, psu, salinity, salt, school, science, sea, sea_water_practical_salinity, seawater, simple, soda, soest, technology, temperature, time, u, unit, v, v2.2.4, velocity, vertical, w, water, zonal</att>\n"
            + "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n"
            + "        <att name=\"license\">[standard]</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"summary\">Simple Ocean Data Assimilation (SODA) v2.2.4 monthly means (soda pop2.2.4)</att>\n"
            + "        <att name=\"title\">SODA v2.2.4 monthly means (soda pop2.2.4) [time][lev][lat][lon], 0.5°, 1871-2010</att>\n"
            + "    </addAttributes>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>time</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"grads_dim\">t</att>\n"
            + "            <att name=\"grads_mapping\">linear</att>\n"
            + "            <att name=\"grads_min\">00z15jan1871</att>\n"
            + "            <att name=\"grads_size\">1680</att>\n"
            + "            <att name=\"grads_step\">1mo</att>\n"
            + "            <att name=\"long_name\">time</att>\n"
            + "            <att name=\"maximum\">00z15dec2010</att>\n"
            + "            <att name=\"minimum\">00z15jan1871</att>\n"
            + "            <att name=\"resolution\" type=\"float\">30.43657</att>\n"
            + "            <att name=\"units\">days since 1-1-1 00:00:0.0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"axis\">T</att>\n"
            + "            <att name=\"data_max\">00z15dec2010</att>\n"
            + "            <att name=\"data_min\">00z15jan1871</att>\n"
            + "            <att name=\"grads_dim\">null</att>\n"
            + "            <att name=\"grads_mapping\">null</att>\n"
            + "            <att name=\"grads_min\">null</att>\n"
            + "            <att name=\"grads_size\">null</att>\n"
            + "            <att name=\"grads_step\">null</att>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"maximum\">null</att>\n"
            + "            <att name=\"minimum\">null</att>\n"
            + "            <att name=\"resolution\">null</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"units\">days since 0001-01-01T00:00:00.000Z</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>lev</sourceName>\n"
            + "        <destinationName>depth</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"grads_dim\">z</att>\n"
            + "            <att name=\"grads_mapping\">levels</att>\n"
            + "            <att name=\"long_name\">altitude</att>\n"
            + "            <att name=\"maximum\" type=\"double\">5375.0</att>\n"
            + "            <att name=\"minimum\" type=\"double\">5.01</att>\n"
            + "            <att name=\"name\">Depth</att>\n"
            + "            <att name=\"positive\">down</att>\n"
            + "            <att name=\"resolution\" type=\"float\">137.69205</att>\n"
            + "            <att name=\"units\">meters</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"axis\">Z</att>\n"
            + "            <att name=\"data_max\" type=\"double\">5375.0</att>\n"
            + "            <att name=\"data_min\" type=\"double\">5.01</att>\n"
            + "            <att name=\"grads_dim\">null</att>\n"
            + "            <att name=\"grads_mapping\">null</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"maximum\">null</att>\n"
            + "            <att name=\"minimum\">null</att>\n"
            + "            <att name=\"resolution\">null</att>\n"
            + "            <att name=\"source_name\">lev</att>\n"
            + "            <att name=\"standard_name\">depth</att>\n"
            + "            <att name=\"units\">m</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>lat</sourceName>\n"
            + "        <destinationName>latitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"grads_dim\">y</att>\n"
            + "            <att name=\"grads_mapping\">linear</att>\n"
            + "            <att name=\"grads_size\">330</att>\n"
            + "            <att name=\"long_name\">latitude</att>\n"
            + "            <att name=\"maximum\" type=\"double\">89.25</att>\n"
            + "            <att name=\"minimum\" type=\"double\">-75.25</att>\n"
            + "            <att name=\"resolution\" type=\"float\">0.5</att>\n"
            + "            <att name=\"units\">degrees_north</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"axis\">Y</att>\n"
            + "            <att name=\"data_max\" type=\"double\">89.25</att>\n"
            + "            <att name=\"data_min\" type=\"double\">-75.25</att>\n"
            + "            <att name=\"grads_dim\">null</att>\n"
            + "            <att name=\"grads_mapping\">null</att>\n"
            + "            <att name=\"grads_size\">null</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Latitude</att>\n"
            + "            <att name=\"maximum\">null</att>\n"
            + "            <att name=\"minimum\">null</att>\n"
            + "            <att name=\"resolution\">null</att>\n"
            + "            <att name=\"standard_name\">latitude</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>lon</sourceName>\n"
            + "        <destinationName>longitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"grads_dim\">x</att>\n"
            + "            <att name=\"grads_mapping\">linear</att>\n"
            + "            <att name=\"grads_size\">720</att>\n"
            + "            <att name=\"long_name\">longitude</att>\n"
            + "            <att name=\"maximum\" type=\"double\">359.75</att>\n"
            + "            <att name=\"minimum\" type=\"double\">0.25</att>\n"
            + "            <att name=\"resolution\" type=\"float\">0.5</att>\n"
            + "            <att name=\"units\">degrees_east</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"axis\">X</att>\n"
            + "            <att name=\"data_max\" type=\"double\">359.75</att>\n"
            + "            <att name=\"data_min\" type=\"double\">0.25</att>\n"
            + "            <att name=\"grads_dim\">null</att>\n"
            + "            <att name=\"grads_mapping\">null</att>\n"
            + "            <att name=\"grads_size\">null</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Longitude</att>\n"
            + "            <att name=\"maximum\">null</att>\n"
            + "            <att name=\"minimum\">null</att>\n"
            + "            <att name=\"resolution\">null</att>\n"
            + "            <att name=\"standard_name\">longitude</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>temp</sourceName>\n"
            + "        <destinationName>temp</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">-9.99E33</att>\n"
            + "            <att name=\"long_name\">temperature [degc]</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-9.99E33</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Temperature</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>salt</sourceName>\n"
            + "        <destinationName>salt</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">-9.99E33</att>\n"
            + "            <att name=\"long_name\">salinity [psu]</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-9.99E33</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">37.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">32.0</att>\n"
            + "            <att name=\"ioos_category\">Salinity</att>\n"
            + "            <att name=\"standard_name\">sea_water_practical_salinity</att>\n"
            + "            <att name=\"units\">PSU</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>u</sourceName>\n"
            + "        <destinationName>u</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">-9.99E33</att>\n"
            + "            <att name=\"long_name\">zonal velocity [m/s]</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-9.99E33</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Currents</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>v</sourceName>\n"
            + "        <destinationName>v</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">-9.99E33</att>\n"
            + "            <att name=\"long_name\">meridional velocity [m/s]</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-9.99E33</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Currents</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>w</sourceName>\n"
            + "        <destinationName>w</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">-9.99E33</att>\n"
            + "            <att name=\"long_name\">vertical velocity [m/s]</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-9.99E33</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Currents</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n"
            + "<dataset type=\"EDDGridFromDap\" datasetID=\""
            + suggDatasetID2
            + "\" active=\"true\">\n";

    String results = EDDGridFromDap.generateDatasetsXml(url, null, null, null, -1, null);
    results = results.replaceAll("... ... .. ..:..:.. HST ....", "DDD MMM dd hh:mm:ss HST yyyy");
    Test.ensureEqual(results.substring(0, expected1.length()), expected1, "results=\n" + results);

    // int po = results.indexOf(expected2.substring(0, 40));
    // Test.ensureEqual(results.substring(po), expected2, "results=\n" + results);

    // GenerateDatasetsXml
    String gdxResults =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {
                  "-verbose", "-i#testGenerateDatasetsXml", "EDDGridFromDap", url, "-1"
                }, // defaultReloadEvery,
                false); // doIt loop?
    gdxResults =
        gdxResults.replaceAll("... ... .. ..:..:.. HST ....", "DDD MMM dd hh:mm:ss HST yyyy");
    Test.ensureEqual(
        gdxResults.substring(0, expected1.length()),
        expected1,
        "Unexpected results from GenerateDatasetsXml.doIt.");

    // ensure it is ready-to-use by making a dataset from it
    String tDatasetID = suggDatasetID;
    EDD.deleteCachedDatasetInfo(tDatasetID);
    EDD edd =
        EDDGridFromDap.oneFromXmlFragment(
            null, results); // only returns the first dataset defined in
    // results
    Test.ensureEqual(edd.datasetID(), tDatasetID, "");
    Test.ensureEqual(
        edd.title(),
        "SODA v2.2.4 monthly means (soda pop2.2.4) [time][lev][lat][lon], 0.5°, 1871-2010",
        "");
    Test.ensureEqual(
        String2.toCSSVString(edd.dataVariableDestinationNames()), "temp, salt, u, v, w", "");

    // test of doNotSetStandardNames
    try {
      EDDGridFromDap.doNotAddStandardNames = true;

      expected1 =
          "<dataset type=\"EDDGridFromDap\" datasetID=\""
              + suggDatasetID
              + "\" active=\"true\">\n"
              + "    <sourceUrl>http://apdrc.soest.hawaii.edu/dods/public_data/SODA/soda_pop2.2.4</sourceUrl>\n"
              + "    <reloadEveryNMinutes>43200</reloadEveryNMinutes>\n"
              + "    <!-- sourceAttributes>\n"
              + "        <att name=\"Conventions\">COARDS</att>\n"
              + "        <att name=\"dataType\">Grid</att>\n"
              + "        <att name=\"documentation\">http://apdrc.soest.hawaii.edu/datadoc/soda_2.2.4.php</att>\n"
              + "        <att name=\"history\">DDD MMM DD hh:mm:ss HST YYYY : imported by GrADS Data Server 2.0</att>\n"
              + "        <att name=\"title\">SODA v2.2.4 monthly means</att>\n"
              + "    </sourceAttributes -->\n"
              + "    <addAttributes>\n"
              + "        <att name=\"cdm_data_type\">Grid</att>\n"
              + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
              + "        <att name=\"creator_email\">chepurin@umd.edu</att>\n"
              + "        <att name=\"creator_name\">HAWAII SOEST</att>\n"
              + "        <att name=\"creator_type\">institution</att>\n"
              + "        <att name=\"creator_url\">https://www.atmos.umd.edu/~ocean/</att>\n"
              + "        <att name=\"infoUrl\">http://apdrc.soest.hawaii.edu/datadoc/soda_2.2.4.php</att>\n"
              + "        <att name=\"institution\">HAWAII SOEST</att>\n"
              + // some changes to
              // keywords in this
              // test
              // vs above
              // test
              "        <att name=\"keywords\">assimilation, currents, data, degc, depth, earth, hawaii, latitude, longitude, means, meridional, month, monthly, ocean, pop2.2.4, practical, psu, salinity, salt, school, science, simple, soda, soest, technology, temperature, time, u, unit, v, v2.2.4, velocity, vertical, w, zonal</att>\n"
              +
              // " <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
              // //removed in this test
              "        <att name=\"license\">[standard]</att>\n"
              + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
              + "        <att name=\"summary\">Simple Ocean Data Assimilation (SODA) v2.2.4 monthly means (soda pop2.2.4)</att>\n"
              + "        <att name=\"title\">SODA v2.2.4 monthly means (soda pop2.2.4) [time][lev][lat][lon], 0.5°, 1871-2010</att>\n"
              + "    </addAttributes>\n"
              + "    <axisVariable>\n"
              + "        <sourceName>time</sourceName>\n"
              + "        <destinationName>time</destinationName>\n"
              + "        <!-- sourceAttributes>\n"
              + "            <att name=\"grads_dim\">t</att>\n"
              + "            <att name=\"grads_mapping\">linear</att>\n"
              + "            <att name=\"grads_min\">00z15jan1871</att>\n"
              + "            <att name=\"grads_size\">1680</att>\n"
              + "            <att name=\"grads_step\">1mo</att>\n"
              + "            <att name=\"long_name\">time</att>\n"
              + "            <att name=\"maximum\">00z15dec2010</att>\n"
              + "            <att name=\"minimum\">00z15jan1871</att>\n"
              + "            <att name=\"resolution\" type=\"float\">30.43657</att>\n"
              + "            <att name=\"units\">days since 1-1-1 00:00:0.0</att>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"axis\">T</att>\n"
              + "            <att name=\"data_max\">00z15dec2010</att>\n"
              + "            <att name=\"data_min\">00z15jan1871</att>\n"
              + "            <att name=\"grads_dim\">null</att>\n"
              + "            <att name=\"grads_mapping\">null</att>\n"
              + "            <att name=\"grads_min\">null</att>\n"
              + "            <att name=\"grads_size\">null</att>\n"
              + "            <att name=\"grads_step\">null</att>\n"
              + "            <att name=\"ioos_category\">Time</att>\n"
              + "            <att name=\"maximum\">null</att>\n"
              + "            <att name=\"minimum\">null</att>\n"
              + "            <att name=\"resolution\">null</att>\n"
              + "            <att name=\"standard_name\">time</att>\n"
              + // LLAT still set
              "            <att name=\"units\">days since 0001-01-01T00:00:00.000Z</att>\n"
              + "        </addAttributes>\n"
              + "    </axisVariable>\n"
              + "    <axisVariable>\n"
              + "        <sourceName>lev</sourceName>\n"
              + "        <destinationName>depth</destinationName>\n"
              + "        <!-- sourceAttributes>\n"
              + "            <att name=\"grads_dim\">z</att>\n"
              + "            <att name=\"grads_mapping\">levels</att>\n"
              + "            <att name=\"long_name\">altitude</att>\n"
              + "            <att name=\"maximum\" type=\"double\">5375.0</att>\n"
              + "            <att name=\"minimum\" type=\"double\">5.01</att>\n"
              + "            <att name=\"name\">Depth</att>\n"
              + "            <att name=\"positive\">down</att>\n"
              + "            <att name=\"resolution\" type=\"float\">137.69205</att>\n"
              + "            <att name=\"units\">meters</att>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"axis\">Z</att>\n"
              + "            <att name=\"data_max\" type=\"double\">5375.0</att>\n"
              + "            <att name=\"data_min\" type=\"double\">5.01</att>\n"
              + "            <att name=\"grads_dim\">null</att>\n"
              + "            <att name=\"grads_mapping\">null</att>\n"
              + "            <att name=\"ioos_category\">Location</att>\n"
              + "            <att name=\"maximum\">null</att>\n"
              + "            <att name=\"minimum\">null</att>\n"
              + "            <att name=\"resolution\">null</att>\n"
              + "            <att name=\"source_name\">lev</att>\n"
              + "            <att name=\"standard_name\">depth</att>\n"
              + // LLAT still set
              "            <att name=\"units\">m</att>\n"
              + "        </addAttributes>\n"
              + "    </axisVariable>\n"
              + "    <axisVariable>\n"
              + "        <sourceName>lat</sourceName>\n"
              + "        <destinationName>latitude</destinationName>\n"
              + "        <!-- sourceAttributes>\n"
              + "            <att name=\"grads_dim\">y</att>\n"
              + "            <att name=\"grads_mapping\">linear</att>\n"
              + "            <att name=\"grads_size\">330</att>\n"
              + "            <att name=\"long_name\">latitude</att>\n"
              + "            <att name=\"maximum\" type=\"double\">89.25</att>\n"
              + "            <att name=\"minimum\" type=\"double\">-75.25</att>\n"
              + "            <att name=\"resolution\" type=\"float\">0.5</att>\n"
              + "            <att name=\"units\">degrees_north</att>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"axis\">Y</att>\n"
              + "            <att name=\"data_max\" type=\"double\">89.25</att>\n"
              + "            <att name=\"data_min\" type=\"double\">-75.25</att>\n"
              + "            <att name=\"grads_dim\">null</att>\n"
              + "            <att name=\"grads_mapping\">null</att>\n"
              + "            <att name=\"grads_size\">null</att>\n"
              + "            <att name=\"ioos_category\">Location</att>\n"
              + "            <att name=\"long_name\">Latitude</att>\n"
              + "            <att name=\"maximum\">null</att>\n"
              + "            <att name=\"minimum\">null</att>\n"
              + "            <att name=\"resolution\">null</att>\n"
              + "            <att name=\"standard_name\">latitude</att>\n"
              + // LLAT still set
              "        </addAttributes>\n"
              + "    </axisVariable>\n"
              + "    <axisVariable>\n"
              + "        <sourceName>lon</sourceName>\n"
              + "        <destinationName>longitude</destinationName>\n"
              + "        <!-- sourceAttributes>\n"
              + "            <att name=\"grads_dim\">x</att>\n"
              + "            <att name=\"grads_mapping\">linear</att>\n"
              + "            <att name=\"grads_size\">720</att>\n"
              + "            <att name=\"long_name\">longitude</att>\n"
              + "            <att name=\"maximum\" type=\"double\">359.75</att>\n"
              + "            <att name=\"minimum\" type=\"double\">0.25</att>\n"
              + "            <att name=\"resolution\" type=\"float\">0.5</att>\n"
              + "            <att name=\"units\">degrees_east</att>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"axis\">X</att>\n"
              + "            <att name=\"data_max\" type=\"double\">359.75</att>\n"
              + "            <att name=\"data_min\" type=\"double\">0.25</att>\n"
              + "            <att name=\"grads_dim\">null</att>\n"
              + "            <att name=\"grads_mapping\">null</att>\n"
              + "            <att name=\"grads_size\">null</att>\n"
              + "            <att name=\"ioos_category\">Location</att>\n"
              + "            <att name=\"long_name\">Longitude</att>\n"
              + "            <att name=\"maximum\">null</att>\n"
              + "            <att name=\"minimum\">null</att>\n"
              + "            <att name=\"resolution\">null</att>\n"
              + "            <att name=\"standard_name\">longitude</att>\n"
              + // LLAT still set
              "        </addAttributes>\n"
              + "    </axisVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>temp</sourceName>\n"
              + "        <destinationName>temp</destinationName>\n"
              + "        <!-- sourceAttributes>\n"
              + "            <att name=\"_FillValue\" type=\"float\">-9.99E33</att>\n"
              + "            <att name=\"long_name\">temperature [degc]</att>\n"
              + "            <att name=\"missing_value\" type=\"float\">-9.99E33</att>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Temperature</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>salt</sourceName>\n"
              + "        <destinationName>salt</destinationName>\n"
              + "        <!-- sourceAttributes>\n"
              + "            <att name=\"_FillValue\" type=\"float\">-9.99E33</att>\n"
              + "            <att name=\"long_name\">salinity [psu]</att>\n"
              + "            <att name=\"missing_value\" type=\"float\">-9.99E33</att>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              +
              // " <att name=\"colorBarMaximum\" type=\"double\">37.0</att>\n" +
              // " <att name=\"colorBarMinimum\" type=\"double\">32.0</att>\n" +
              "            <att name=\"ioos_category\">Salinity</att>\n"
              +
              // " <att name=\"standard_name\">sea_water_practical_salinity</att>\n" +
              // //should not be set
              // " <att name=\"units\">PSU</att>\n" + //should not be set -- unfortunate
              "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>u</sourceName>\n"
              + "        <destinationName>u</destinationName>\n"
              + "        <!-- sourceAttributes>\n"
              + "            <att name=\"_FillValue\" type=\"float\">-9.99E33</att>\n"
              + "            <att name=\"long_name\">zonal velocity [m/s]</att>\n"
              + "            <att name=\"missing_value\" type=\"float\">-9.99E33</att>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Currents</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>v</sourceName>\n"
              + "        <destinationName>v</destinationName>\n"
              + "        <!-- sourceAttributes>\n"
              + "            <att name=\"_FillValue\" type=\"float\">-9.99E33</att>\n"
              + "            <att name=\"long_name\">meridional velocity [m/s]</att>\n"
              + "            <att name=\"missing_value\" type=\"float\">-9.99E33</att>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Currents</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>w</sourceName>\n"
              + "        <destinationName>w</destinationName>\n"
              + "        <!-- sourceAttributes>\n"
              + "            <att name=\"_FillValue\" type=\"float\">-9.99E33</att>\n"
              + "            <att name=\"long_name\">vertical velocity [m/s]</att>\n"
              + "            <att name=\"missing_value\" type=\"float\">-9.99E33</att>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Currents</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "</dataset>\n"
              + "\n"
              + "<dataset type=\"EDDGridFromDap\" datasetID=\""
              + suggDatasetID2
              + "\" active=\"true\">\n";

      results = EDDGridFromDap.generateDatasetsXml(url, null, null, null, -1, null);

      results =
          results.replaceAll(
              "[a-zA-Z]+ [a-zA-Z]+ [0-9]+ [0-9]+:[0-9]+:[0-9]+ HST [0-9]+",
              "DDD MMM DD hh:mm:ss HST YYYY");
      Test.ensureEqual(results.substring(0, expected1.length()), expected1, "results=\n" + results);

    } finally {
      // always undo this, so if there is a failure, subsequent tests won't be screwed
      // up
      EDDGridFromDap.doNotAddStandardNames = false;
    }
  }

  /**
   * This test generateDatasetsXml, especially suggestReloadEveryNMinutes and adding resolution to
   * title. Another good/better test of suggestReloadEveryNMinutes is
   * EDDGridFromDap.testUAFSubThreddsCatalog(1); //test one sub catalog
   * https://data.pmel.noaa.gov/uaf/thredds/catalog/CleanCatalogs/cwcgom.aoml.noaa.gov/thredds/catalog.xml
   * because it has a good mix of NRT and delayed datasets.
   */
  @org.junit.jupiter.api.Test
  @TagExternalERDDAP
  void testGenerateDatasetsXml2() throws Throwable {
    // testVerboseOn();
    String url = "https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdGAsstahday";
    String2.log("\n*** EDDGridFromDap.testGenerateDatasetsXml2");
    int language = 0;

    String expected1 =
        "<dataset type=\"EDDGridFromDap\" datasetID=\"noaa_pfeg_cada_f1d6_7111\" active=\"true\">\n"
            + "    <sourceUrl>https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdGAsstahday</sourceUrl>\n"
            + "    <reloadEveryNMinutes>[VARIES]</reloadEveryNMinutes>\n"
            + // 60, 180, or ???, test
            // of
            // suggestReloadEveryNMinutes,
            // but varies
            // with
            // time since sample file.
            "    <!-- sourceAttributes>\n"
            + "        <att name=\"acknowledgement\">NOAA NESDIS COASTWATCH, NOAA SWFSC ERD</att>\n"
            + "        <att name=\"cdm_data_type\">Grid</att>\n"
            + "        <att name=\"composite\">false</att>\n"
            + "        <att name=\"contributor_name\">NOAA NESDIS</att>\n"
            + "        <att name=\"contributor_role\">Source of level 2 data.</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n"
            + "        <att name=\"creator_email\">erd.data@noaa.gov</att>\n"
            + "        <att name=\"creator_name\">NOAA NMFS SWFSC ERD</att>\n"
            + "        <att name=\"creator_type\">institution</att>\n"
            + "        <att name=\"creator_url\">https://www.pfeg.noaa.gov</att>\n";
    /*
     * "        <att name=\"date_created\">2013-01-30Z</att>\n" +
     * "        <att name=\"date_issued\">2013-01-30Z</att>\n" +
     * "        <att name=\"Easternmost_Easting\" type=\"double\">329.975</att>\n" +
     * "        <att name=\"geospatial_lat_max\" type=\"double\">59.975</att>\n" +
     * "        <att name=\"geospatial_lat_min\" type=\"double\">-44.975</att>\n" +
     * "        <att name=\"geospatial_lat_resolution\" type=\"double\">0.05</att>\n"
     * +
     * "        <att name=\"geospatial_lat_units\">degrees_north</att>\n" +
     * "        <att name=\"geospatial_lon_max\" type=\"double\">329.975</att>\n" +
     * "        <att name=\"geospatial_lon_min\" type=\"double\">180.025</att>\n" +
     * "        <att name=\"geospatial_lon_resolution\" type=\"double\">0.05</att>\n"
     * +
     * "        <att name=\"geospatial_lon_units\">degrees_east</att>\n" +
     * "        <att name=\"geospatial_vertical_max\" type=\"double\">0.0</att>\n" +
     * "        <att name=\"geospatial_vertical_min\" type=\"double\">0.0</att>\n" +
     * "        <att name=\"geospatial_vertical_positive\">up</att>\n" +
     * "        <att name=\"geospatial_vertical_units\">m</att>\n" +
     * "        <att name=\"history\">NOAA NESDIS\n" +
     * "2013-01-30T16:45:58Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD\n"
     * +
     * "2013-01-30T18:11:15Z https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/GA/ssta/hday\n"
     * +
     * "2013-01-30T18:11:15Z https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdGAsstahday.das</att>\n"
     * +
     * "        <att name=\"infoUrl\">https://coastwatch.pfeg.noaa.gov/infog/GA_ssta_las.html</att>\n"
     * +
     * "        <att name=\"institution\">NOAA CoastWatch, West Coast Node</att>\n"
     * +
     * "        <att name=\"keywords\">Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature,\n"
     * +
     * "coastwatch, day, degrees, goes, hemisphere, imager, night, noaa, ocean, oceans, scan, sea, sea_surface_temperature, single, sst, surface, temperature, wcn, western</att>\n"
     * +
     * "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
     * "        <att name=\"license\">The data may be used and redistributed for free but is not intended\n"
     * +
     * "for legal use, since it may contain inaccuracies. Neither the data\n" +
     * "Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
     * "of their employees or contractors, makes any warranty, express or\n" +
     * "implied, including warranties of merchantability and fitness for a\n" +
     * "particular purpose, or assumes any legal liability for the accuracy,\n" +
     * "completeness, or usefulness, of this information.</att>\n" +
     * "        <att name=\"naming_authority\">gov.noaa.pfel.coastwatch</att>\n" +
     * "        <att name=\"Northernmost_Northing\" type=\"double\">59.975</att>\n"
     * +
     * "        <att name=\"origin\">NOAA NESDIS</att>\n" +
     * "        <att name=\"processing_level\">3</att>\n" +
     * "        <att name=\"project\">CoastWatch (http://coastwatch.noaa.gov/)</att>\n"
     * +
     * "        <att name=\"projection\">geographic</att>\n" +
     * "        <att name=\"projection_type\">mapped</att>\n" +
     * "        <att name=\"references\">NOAA GOES satellites: http://coastwatch.noaa.gov/goes_sst_overview.html . NOAA GOES satellites: http://www.oso.noaa.gov/goes/index.htm . Processing reference: Wu, X., W. P. Menzel, and G. S. Wade (1999). Estimation of sea surface temperatures using GOES-8/9 radiance measurements. Bull. Amer. Meteor. Soc., 80, 1127-1138. Processing reference: Maturi, E., C. Merchant, A. Harris, X. Li, and B. Potash.  Geostationary Sea Surface Temperature Product Validation and Methodology.  Poster Presentation at the American Meteorological Society&#39;s 13th Conference on Satellite Meteorology and Oceanography (P5.16).  Norfolk, VA; 19-23 Sept., 2004. http://ams.confex.com/ams/pdfpapers/79202.pdf .</att>\n"
     * +
     * "        <att name=\"satellite\">GOES</att>\n" +
     * "        <att name=\"sensor\">Imager</att>\n" +
     * "        <att name=\"source\">satellite observation: GOES, Imager</att>\n" +
     * "        <att name=\"sourceUrl\">https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/GA/ssta/hday</att>\n"
     * +
     * "        <att name=\"Southernmost_Northing\" type=\"double\">-44.975</att>\n"
     * +
     * "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v55</att>\n"
     * +
     * "        <att name=\"summary\">NOAA CoastWatch provides SST data from the NOAA Geostationary Operational Environmental Satellites (GOES).  Measurements are gathered by the GOES Imager, a multi-channel radiometer carried aboard the satellite.  SST is available for hourly Imager measurements, or in composite images of various durations.</att>\n"
     * +
     * "        <att name=\"time_coverage_end\">2013-01-30T15:00:00Z</att>\n" +
     * "        <att name=\"time_coverage_start\">2008-06-02T00:00:00Z</att>\n" +
     * "        <att name=\"title\">SST, GOES Imager, Day and Night, Western Hemisphere (Hourly)</att>\n"
     * +
     * "        <att name=\"Westernmost_Easting\" type=\"double\">180.025</att>\n" +
     * "    </sourceAttributes -->\n" +
     * "    <addAttributes>\n" +
     * "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n" +
     * "        <att name=\"institution\">NOAA CoastWatch WCN</att>\n" +
     * "        <att name=\"keywords\">coastwatch, day, degrees, goes, hemisphere, hourly, imager, night, noaa, ocean, oceans,\n"
     * +
     * "Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature,\n" +
     * "scan, sea, sea_surface_temperature, single, sst, surface, temperature, wcn, western</att>\n"
     * +
     * "    </addAttributes>\n" +
     * "    <axisVariable>\n" +
     * "        <sourceName>time</sourceName>\n" +
     * "        <destinationName>time</destinationName>\n" +
     * "        <!-- sourceAttributes>\n" +
     * "            <att name=\"_CoordinateAxisType\">Time</att>\n" +
     * "            <att name=\"actual_range\" type=\"doubleList\">1.2123648E9 1.359558E9</att>\n"
     * +
     */

    String expected2 =
        "<att name=\"axis\">T</att>\n"
            + "            <att name=\"fraction_digits\" type=\"int\">0</att>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"long_name\">Centered Time</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"time_origin\">01-JAN-1970 00:00:00</att>\n"
            + "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>altitude</sourceName>\n"
            + "        <destinationName>altitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Height</att>\n"
            + "            <att name=\"_CoordinateZisPositive\">up</att>\n"
            + "            <att name=\"actual_range\" type=\"doubleList\">0.0 0.0</att>\n"
            + "            <att name=\"axis\">Z</att>\n"
            + "            <att name=\"fraction_digits\" type=\"int\">0</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Altitude</att>\n"
            + "            <att name=\"positive\">up</att>\n"
            + "            <att name=\"standard_name\">altitude</att>\n"
            + "            <att name=\"units\">m</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>latitude</sourceName>\n"
            + "        <destinationName>latitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Lat</att>\n"
            + "            <att name=\"actual_range\" type=\"doubleList\">-44.975 59.975</att>\n"
            + "            <att name=\"axis\">Y</att>\n"
            + "            <att name=\"coordsys\">geographic</att>\n"
            + "            <att name=\"fraction_digits\" type=\"int\">3</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Latitude</att>\n"
            + "            <att name=\"point_spacing\">even</att>\n"
            + "            <att name=\"standard_name\">latitude</att>\n"
            + "            <att name=\"units\">degrees_north</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>longitude</sourceName>\n"
            + "        <destinationName>longitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Lon</att>\n"
            + "            <att name=\"actual_range\" type=\"doubleList\">180.025 329.975</att>\n"
            + "            <att name=\"axis\">X</att>\n"
            + "            <att name=\"coordsys\">geographic</att>\n"
            + "            <att name=\"fraction_digits\" type=\"int\">3</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Longitude</att>\n"
            + "            <att name=\"point_spacing\">even</att>\n"
            + "            <att name=\"standard_name\">longitude</att>\n"
            + "            <att name=\"units\">degrees_east</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>sst</sourceName>\n"
            + "        <destinationName>sst</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"coordsys\">geographic</att>\n"
            + "            <att name=\"fraction_digits\" type=\"int\">1</att>\n"
            + "            <att name=\"ioos_category\">Temperature</att>\n"
            + "            <att name=\"long_name\">Sea Surface Temperature</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n"
            + "            <att name=\"standard_name\">sea_surface_temperature</att>\n"
            + "            <att name=\"units\">degree_C</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n";

    String results = EDDGridFromDap.generateDatasetsXml(url, null, null, null, -1, null);
    results =
        results.replaceAll(
            "<reloadEveryNMinutes>\\d*</reloadEveryNMinutes>",
            "<reloadEveryNMinutes>[VARIES]</reloadEveryNMinutes>");

    Test.ensureEqual(results.substring(0, expected1.length()), expected1, "results=\n" + results);

    int po = results.indexOf(expected2.substring(0, 20));
    Test.ensureEqual(results.substring(po), expected2, "results=\n" + results);

    po = results.lastIndexOf("<att name=\"title\">");
    int po2 = results.indexOf("</att>", po);
    String tResults = results.substring(po, po2 + 6);
    String tExpected =
        "<att name=\"title\">SST, GOES Imager, Day and Night, "
            + "Western Hemisphere, 2006-2020 (Hourly) (erdGAsstahday), "
            + "0.05°</att>"; // important test of catching resolution
    Test.ensureEqual(tResults, tExpected, "results=\n" + results);
  }

  /** This tests that generateDatasetsXml tests that the axes are sorted. */
  @org.junit.jupiter.api.Test
  @TagThredds
  void testGenerateDatasetsXml3() throws Throwable {
    String2.log("\n*** EDDGridFromDap.testGenerateDatasetsXml3()");
    // from uaf. Not sorted and never will be.
    String url =
        "http://oos.soest.hawaii.edu/thredds/dodsC/pacioos/ncom/global/NCOM_Global_Ocean_Model_fmrc.ncd";
    int language = 0;

    try {
      String results =
          EDDGridFromDap.generateDatasetsXml(
              url, null, null, new String[] {"run", "time"}, -1, null);
      throw new RuntimeException("Shouldn't get here.");

    } catch (Throwable t) {
      String msg = MustBe.throwableToString(t);
      if (msg.indexOf("ERROR: unsorted axis: ") >= 0) String2.log("\nCommon error:\n" + msg);
      else {
        Test.knownProblem("STARTING 2013-05-21 DATA SOURCE IS GONE.   FIX IT?", msg);
      }
    }
  }

  /** Test scale_factor and add_offset. */
  @org.junit.jupiter.api.Test
  @TagThredds
  void testScaleAddOffset() throws Throwable {
    // tests of scale_factor/scaleFactor and add_offset/addOffset
    // and tests of _FillValue with no missing_value
    // testVerboseOn();
    int tPo;
    int language = 0;
    String baseName;

    EDDGrid eddGrid = (EDDGridFromDap) EDDTestDataset.getjplNesdisG17v271();
    String tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            eddGrid.className() + "_scale",
            ".das");
    String results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    String expected =
        "sses_standard_deviation {\n"
            + "    Float32 _FillValue -0.28;\n"
            + // scaled and add_offset
            "    Float64 colorBarMaximum 5.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String comment \"Standard deviation of sea_surface_temperature from SST measured by drifting buoys. Further information at (Petrenko et al., JTECH, 2016; doi:10.1175/JTECH-D-15-0166.1)\";\n"
            + "    String ioos_category \"Statistics\";\n"
            + "    String long_name \"SSES standard deviation\";\n"
            + "    String units \"degree_C\";\n"
            + "    Float32 valid_max 2.27;\n"
            + // scaled and add_offset
            "    Float32 valid_min -0.27;\n"
            + // scaled and add_offset
            "  }";
    tPo = results.indexOf("sses_standard_deviation {");
    int po2 = results.indexOf("}", tPo);
    String tResults = results.substring(tPo, po2 + 1);
    Test.ensureEqual(tResults, expected, "tresults=\n" + tResults);

    // test mv as -9.99 (adjusted by scaleFactor)
    String query = "sses_standard_deviation[0][2400:100:2600][2200:100:2400]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            query,
            EDStatic.fullTestCacheDirectory,
            eddGrid.className() + "eddGrid",
            ".asc");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 sses_standard_deviation[time = 1][latitude = 3][longitude = 3];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 1];\n"
            + "      Float32 latitude[latitude = 3];\n"
            + "      Float32 longitude[longitude = 3];\n"
            + "  } sses_standard_deviation;\n"
            + "} jplNesdisG17v271;\n"
            + "---------------------------------------------\n"
            + "sses_standard_deviation.sses_standard_deviation[1][3][3]\n"
            + "[0][0], -0.28, 0.21, 0.19\n"
            + "[0][1], 0.21, 0.19, -0.28\n"
            + "[0][2], 0.19, -0.28, -0.28\n"
            + "\n"
            + "sses_standard_deviation.time[1]\n"
            + "1.571184E9\n"
            + "\n"
            + "sses_standard_deviation.latitude[3]\n"
            + "41.99, 39.99, 37.99\n"
            + "\n"
            + "sses_standard_deviation.longitude[3]\n"
            + "-135.99, -133.99, -131.99\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test mv as NaN (adjusted to -9.99 by scaleFactor, then converted to NaN (then
    // null))
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            query,
            EDStatic.fullTestCacheDirectory,
            eddGrid.className() + "eddGrid",
            ".json");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "{\n"
            + "  \"table\": {\n"
            + "    \"columnNames\": [\"time\", \"latitude\", \"longitude\", \"sses_standard_deviation\"],\n"
            + "    \"columnTypes\": [\"String\", \"float\", \"float\", \"float\"],\n"
            + "    \"columnUnits\": [\"UTC\", \"degrees_north\", \"degrees_east\", \"degree_C\"],\n"
            + "    \"rows\": [\n"
            + "      [\"2019-10-16T00:00:00Z\", 41.99, -135.99, null],\n"
            + "      [\"2019-10-16T00:00:00Z\", 41.99, -133.99, 0.21],\n"
            + "      [\"2019-10-16T00:00:00Z\", 41.99, -131.99, 0.19],\n"
            + "      [\"2019-10-16T00:00:00Z\", 39.99, -135.99, 0.21],\n"
            + "      [\"2019-10-16T00:00:00Z\", 39.99, -133.99, 0.19],\n"
            + "      [\"2019-10-16T00:00:00Z\", 39.99, -131.99, null],\n"
            + "      [\"2019-10-16T00:00:00Z\", 37.99, -135.99, 0.19],\n"
            + "      [\"2019-10-16T00:00:00Z\", 37.99, -133.99, null],\n"
            + "      [\"2019-10-16T00:00:00Z\", 37.99, -131.99, null]\n"
            + "    ]\n"
            + "  }\n"
            + "}\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  @org.junit.jupiter.api.Test
  @TagMissingDataset
  void testOneTime() throws Throwable {
    int language = 0;

    // gridDataset = (EDDGridFromDap)oneFromDatasetsXml(null, "pmelOscar");
    // if (true) System.exit(0);

    // one time stuff
    // gridDataset = (EDDGridFromDap)oneFromDatasetsXml(null, "ncdcOisstAmsrAgg");
    // tName = gridDataset.makeNewFileForDapQuery(language, null, null, "",
    // EDStatic.fullTestCacheDirectory,
    // gridDataset.className() + "ncdc", ".das");
    // String results =
    // File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);

    // soda
    EDDGrid gridDataset =
        (EDDGridFromDap) EDDGridFromDap.oneFromDatasetsXml(null, "erdSoda202d"); // should
    // work
    String tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_soda202d",
            ".das");
    String results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    String2.log(results);

    String sodaq = "time[(2001-12-15T00:00:00):100:(2001-12-15T00:00:00)]";
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            sodaq,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_soda202dqt",
            ".json");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    String2.log(results);

    sodaq =
        "temp[(2001-12-15T00:00:00):100:(2001-12-15T00:00:00)][(-5.01):1:(-5.01)][(-75.25):100:(89.25)][(0.25):100:(359.75)]"
    // +
    // ",salt[(2001-12-15T00:00:00):100:(2001-12-15T00:00:00)][(-5.01):1:(-5.01)][(-75.25):100:(89.25)][(0.25):100:(359.75)]"
    // +
    // ",u[(2001-12-15T00:00:00):100:(2001-12-15T00:00:00)][(-5.01):1:(-5.01)][(-75.25):100:(89.25)][(0.25):100:(359.75)]"
    // +
    // ",v[(2001-12-15T00:00:00):100:(2001-12-15T00:00:00)][(-5.01):1:(-5.01)][(-75.25):100:(89.25)][(0.25):100:(359.75)]"
    // +
    // ",w[(2001-12-15T00:00:00):100:(2001-12-15T00:00:00)][(-5.01):1:(-5.01)][(-75.25):100:(89.25)][(0.25):100:(359.75)]"
    // +
    // ",utrans[(2001-12-15T00:00:00):100:(2001-12-15T00:00:00)][(-5.01):1:(-5.01)][(-75.25):100:(89.25)][(0.25):100:(359.75)]"
    // +
    // ",vtrans[(2001-12-15T00:00:00):100:(2001-12-15T00:00:00)][(-5.01):1:(-5.01)][(-75.25):100:(89.25)][(0.25):100:(359.75)]"
    // +
    // ",CFC11[(2001-12-15T00:00:00):100:(2001-12-15T00:00:00)][(-5.01):1:(-5.01)][(-75.25):100:(89.25)][(0.25):100:(359.75)]"
    ;
    // The error: GridDataAccessor.increment: partialResults[0] was not as expected.
    // The other primitiveArray has a different value #0 (1008374400 != 623)
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            sodaq,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_soda202dq",
            ".json");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    String2.log(results);

    gridDataset =
        (EDDGridFromDap) EDDGridFromDap.oneFromDatasetsXml(null, "erdSoda202s"); // should work
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_soda202s",
            ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    String2.log(results);

    gridDataset =
        (EDDGridFromDap) EDDGridFromDap.oneFromDatasetsXml(null, "erdSoda203d"); // should work
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_soda203d",
            ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    String2.log(results);

    gridDataset =
        (EDDGridFromDap) EDDGridFromDap.oneFromDatasetsXml(null, "erdSoda203s"); // should work
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_soda203s",
            ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    String2.log(results);
  }

  /**
   * THIS TEST IS NOT ACTIVE BECAUSE DAPPER IS NO LONGER ACTIVE. This does important tests with
   * pmelOscar (which has descending lat axis values AND wierd rage: longitude is 20 .. 419!!!).
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagMissingDataset
  void testPmelOscar() throws Throwable {
    boolean doGraphicsTests = false;
    // String2.log("\n*** EDDGridFromDap.test for pmelOscar");
    // testVerboseOn();
    String name, tName, results, tResults, expected, userDapQuery;
    String today =
        Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); // 14 is enough to check
    // hour. Hard
    // to
    // check min:sec.
    EDDGrid eddGrid = (EDDGrid) EDDGridFromDap.oneFromDatasetsXml(null, "pmelOscar");
    EDVGridAxis edvga;
    int language = 0;

    // ***test some edvga things
    // time commented out: it changes too often
    // EDVGridAxis edvga = eddGrid.axisVariables()[0];
    // Test.ensureEqual(edvga.isEvenlySpaced(), false, "");
    // Test.ensureEqual(edvga.averageSpacing(), 438329.1311754685, ""); //changes
    // sometimes
    // Test.ensureEqual(edvga.spacingDescription(0), "5 days 01:45:29 (uneven)",
    // "");

    edvga = eddGrid.axisVariables()[1];
    Test.ensureEqual(edvga.isEvenlySpaced(), true, "");
    Test.ensureEqual(edvga.averageSpacing(), Double.NaN, "");
    Test.ensureEqual(edvga.spacingDescription(0), "(just one value)", "");

    edvga = eddGrid.axisVariables()[2];
    Test.ensureEqual(edvga.isEvenlySpaced(), true, "");
    Test.ensureEqual(edvga.averageSpacing(), -1, "");
    Test.ensureEqual(edvga.spacingDescription(0), "-1.0 (even)", "");

    // time
    results = eddGrid.axisVariables[0].sliderCsvValues();
    expected = "\"1992-10-21\", \"1992-11-21\", \"1992-12-21\", \"1993-01-21\",";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
    // last values and sliderCsvValues changes frequently

    // alt
    results = eddGrid.axisVariables[1].sliderCsvValues();
    expected = "-15";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // lat
    results = eddGrid.axisVariables[2].sliderCsvValues();
    expected =
        "69.5, 68.5, 67.5, 66.5, 65.5, 64.5, 63.5, 62.5, 61.5, 60.5, 59.5, 58.5, 57.5, 56.5, "
            + "55.5, 54.5, 53.5, 52.5, 51.5, 50.5, 49.5, 48.5, 47.5, 46.5, 45.5, 44.5, 43.5, 42.5, "
            + "41.5, 40.5, 39.5, 38.5, 37.5, 36.5, 35.5, 34.5, 33.5, 32.5, 31.5, 30.5, 29.5, 28.5, "
            + "27.5, 26.5, 25.5, 24.5, 23.5, 22.5, 21.5, 20.5, 19.5, 18.5, 17.5, 16.5, 15.5, 14.5, "
            + "13.5, 12.5, 11.5, 10.5, 9.5, 8.5, 7.5, 6.5, 5.5, 4.5, 3.5, 2.5, 1.5, 0.5, -0.5, -1.5, "
            + "-2.5, -3.5, -4.5, -5.5, -6.5, -7.5, -8.5, -9.5, -10.5, -11.5, -12.5, -13.5, -14.5, -15.5, "
            + "-16.5, -17.5, -18.5, -19.5, -20.5, -21.5, -22.5, -23.5, -24.5, -25.5, -26.5, -27.5, -28.5, "
            + "-29.5, -30.5, -31.5, -32.5, -33.5, -34.5, -35.5, -36.5, -37.5, -38.5, -39.5, -40.5, -41.5, "
            + "-42.5, -43.5, -44.5, -45.5, -46.5, -47.5, -48.5, -49.5, -50.5, -51.5, -52.5, -53.5, -54.5, "
            + "-55.5, -56.5, -57.5, -58.5, -59.5, -60.5, -61.5, -62.5, -63.5, -64.5, -65.5, -66.5, -67.5, -68.5, -69.5";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // lon
    results = eddGrid.axisVariables[3].sliderCsvValues();
    expected = "20.5, 22.5, 24.5, 26.5,";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
    expected = "414.5, 416.5, 418.5, 419.5";
    Test.ensureEqual(
        results.substring(results.length() - expected.length()), expected, "results=\n" + results);

    // .das das isn't affected by userDapQuery
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, "", EDStatic.fullTestCacheDirectory, eddGrid.className(), ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    String lastTime = "" + eddGrid.axisVariables[eddGrid.timeIndex].lastDestinationValue();
    String lastTimeString =
        ((EDVTimeGridAxis) eddGrid.axisVariables[eddGrid.timeIndex]).destinationMaxString();
    lastTime = String2.replaceAll(lastTime, "E", "e+");
    expected =
        "Attributes {\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range 7.196256e+8, "
            + lastTime
            + ";\n"
            + // stop time changes
            // periodically
            "    String axis \"T\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  altitude {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    String _CoordinateZisPositive \"up\";\n"
            + "    Float32 actual_range -15.0, -15.0;\n"
            + "    String axis \"Z\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Altitude\";\n"
            + "    String positive \"up\";\n"
            + "    String standard_name \"altitude\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float32 actual_range 69.5, -69.5;\n"
            + "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float32 actual_range 20.5, 419.5;\n"
            + "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  u {\n"
            + "    Float64 colorBarMaximum 0.5;\n"
            + "    Float64 colorBarMinimum -0.5;\n"
            + "    String generic_name \"u\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Ocean Surface Zonal Currents\";\n"
            + "    Float32 missing_value NaN;\n"
            + "    String name \"u\";\n"
            + "    String standard_name \"eastward_sea_water_velocity\";\n"
            + "    String units \"m s-1\";\n"
            + "  }\n";
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

    expected =
        "    String DATASUBTYPE \"unfiltered\";\n"
            + "    String DATATYPE \"5-Day Interval\";\n"
            + "    String date \"27-Apr-2007\";\n"
            + "    String description \"Sea Surface Velocity\";\n"
            + "    Float64 Easternmost_Easting 419.5;\n"
            + "    Float64 geospatial_lat_max 69.5;\n"
            + "    Float64 geospatial_lat_min -69.5;\n"
            + "    Float64 geospatial_lat_resolution 1.0;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max 419.5;\n"
            + "    Float64 geospatial_lon_min 20.5;\n"
            + "    Float64 geospatial_lon_resolution 1.0;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    Float64 geospatial_vertical_max -15.0;\n"
            + "    Float64 geospatial_vertical_min -15.0;\n"
            + "    String geospatial_vertical_positive \"up\";\n"
            + "    String geospatial_vertical_units \"m\";\n"
            + "    String history \""
            + today;
    int tpo = results.indexOf(expected.substring(0, 17));
    Test.ensureTrue(tpo >= 0, "tpo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tpo, Math.min(results.length(), tpo + expected.length())),
        expected,
        "results=\n" + results);

    // + " http://dapper.pmel.noaa.gov/dapper/oscar/world-unfilter.nc\n" +
    // today + " " + EDStatic.erddapUrl + //in tests, always non-https url
    // "/griddap/pmelOscar.das\";\n" +
    expected =
        "String infoUrl \"http://www.oscar.noaa.gov/\";\n"
            + "    String institution \"NOAA PMEL\";\n"
            + "    String keywords \"Oceans > Ocean Circulation > Ocean Currents,\n"
            + "analyses, anomaly, circulation, current, currents, eastward, eastward_sea_water_velocity, meridional, noaa, northward, northward_sea_water_velocity, ocean, oceans, oscar, pmel, real, real time, sea, seawater, surface, time, velocity, water, zonal\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    Float64 Northernmost_Northing 69.5;\n"
            + "    String reference1 \"Bonjean F. and G.S.E. Lagerloef, 2002, \\\"Diagnostic model and analysis of the surface currents in the tropical Pacific ocean\\\", J. Phys. Oceanogr., 32, 2,938-2,954\";\n"
            + "    String source \"Gary Lagerloef, ESR (lager@esr.org) and Fabrice Bonjean (bonjean@esr.org)\";\n"
            + "    String sourceUrl \"http://dapper.pmel.noaa.gov/dapper/oscar/world-unfilter.nc\";\n"
            + "    Float64 Southernmost_Northing -69.5;\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v55\";\n"
            + "    String summary \"This project is developing a processing system and data center to provide operational ocean surface velocity fields from satellite altimeter and vector wind data. The method to derive surface currents with satellite altimeter and scatterometer data is the outcome of several years NASA sponsored research. The project will transition that capability to operational oceanographic applications. The end product is velocity maps updated daily, with a goal for eventual 2-day maximum delay from time of satellite measurement. Grid resolution is 100 km for the basin scale, and finer resolution in the vicinity of the Pacific Islands.\";\n"
            + "    String time_coverage_end \""
            + lastTimeString
            + "\";\n"
            + "    String time_coverage_start \"1992-10-21T00:00:00Z\";\n"
            + "    String title \"OSCAR - Ocean Surface Current Analyses, Real-Time\";\n"
            + "    String VARIABLE \"Ocean Surface Currents\";\n"
            + "    Float32 version 2006.0;\n"
            + "    Float64 Westernmost_Easting 20.5;\n"
            + "  }\n"
            + "}\n";
    tpo = results.indexOf(expected.substring(0, 17));
    Test.ensureTrue(tpo >= 0, "tpo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tpo, Math.min(results.length(), tpo + expected.length())),
        expected,
        "results=\n" + results);

    // .dds dds isn't affected by userDapQuery
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, "", EDStatic.fullTestCacheDirectory, eddGrid.className(), ".dds");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    int tnTime = eddGrid.axisVariables[eddGrid.timeIndex].sourceValues().size();
    expected =
        "Dataset {\n"
            + "  Float64 time[time = "
            + tnTime
            + "];\n"
            + // changes periodically
            "  Float32 altitude[altitude = 1];\n"
            + "  Float32 latitude[latitude = 140];\n"
            + "  Float32 longitude[longitude = 400];\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 u[time = "
            + tnTime
            + "][altitude = 1][latitude = 140][longitude = 400];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = "
            + tnTime
            + "];\n"
            + "      Float32 altitude[altitude = 1];\n"
            + "      Float32 latitude[latitude = 140];\n"
            + "      Float32 longitude[longitude = 400];\n"
            + "  } u;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 v[time = "
            + tnTime
            + "][altitude = 1][latitude = 140][longitude = 400];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = "
            + tnTime
            + "];\n"
            + "      Float32 altitude[altitude = 1];\n"
            + "      Float32 latitude[latitude = 140];\n"
            + "      Float32 longitude[longitude = 400];\n"
            + "  } v;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 u_anom[time = "
            + tnTime
            + "][altitude = 1][latitude = 140][longitude = 400];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = "
            + tnTime
            + "];\n"
            + "      Float32 altitude[altitude = 1];\n"
            + "      Float32 latitude[latitude = 140];\n"
            + "      Float32 longitude[longitude = 400];\n"
            + "  } u_anom;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 v_anom[time = "
            + tnTime
            + "][altitude = 1][latitude = 140][longitude = 400];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = "
            + tnTime
            + "];\n"
            + "      Float32 altitude[altitude = 1];\n"
            + "      Float32 latitude[latitude = 140];\n"
            + "      Float32 longitude[longitude = 400];\n"
            + "  } v_anom;\n"
            + "} pmelOscar;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .nc lat values subset (subset correct? actual_range correct?)
    userDapQuery = "latitude[(69.5):10:(-69.5)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            eddGrid.className() + "Lat",
            ".nc");
    results = NcHelper.ncdump(EDStatic.fullTestCacheDirectory + tName, "");
    String2.log(results);
    expected = // note actual range is low to high
        "netcdf EDDGridFromDapLat.nc {\n"
            + " dimensions:\n"
            + "   latitude = 14;\n"
            + // (has coord.var)\n" + //changed when switched
            // to netcdf-java 4.0,
            // 2009-02-23
            " variables:\n"
            + "   float latitude(latitude=14);\n"
            + "     :_CoordinateAxisType = \"Lat\";\n"
            + "     :actual_range = -60.5f, 69.5f; // float\n"
            + "     :axis = \"Y\";\n"
            + "     :ioos_category = \"Location\";\n"
            + "     :long_name = \"Latitude\";\n"
            + "     :standard_name = \"latitude\";\n"
            + "     :units = \"degrees_north\";\n"
            + "\n"
            + " :ANOM_MEAN_PERIOD = \"1993-01-01 to 2006-12-31\";\n"
            + " :cdm_data_type = \"Grid\";\n"
            + " :company = \"Earth & Space Research, Seattle, WA\";\n"
            + " :contact = \"Fabrice Bonjean (bonjean@esr.org) or John T. Gunn (gunn@esr.org)\";\n"
            + " :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n";

    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "results=\n" + results);
    expected = // note geospatial_lat_min max; note that internal " are not slashed, but that
        // is ncdump's problem
        " :DATASUBTYPE = \"unfiltered\";\n"
            + " :DATATYPE = \"5-Day Interval\";\n"
            + " :date = \"27-Apr-2007\";\n"
            + " :description = \"Sea Surface Velocity\";\n"
            + " :geospatial_lat_max = 69.5f; // float\n"
            + " :geospatial_lat_min = -60.5f; // float\n"
            + " :geospatial_lat_units = \"degrees_north\";\n"
            + " :history = \"";
    tpo = results.indexOf(expected.substring(0, 17));
    Test.ensureTrue(tpo >= 0, "tpo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tpo, Math.min(results.length(), tpo + expected.length())),
        expected,
        "results=\n" + results);

    // note geospatial_lat_min max; note that internal " are not slashed, but that
    // is ncdump's problem
    // today + " http://dapper.pmel.noaa.gov/dapper/oscar/world-unfilter.nc\n" +
    // today + " " + EDStatic.erddapUrl + //in tests, always non-https url

    expected =
        "/griddap/pmelOscar.nc?latitude[(69.5):10:(-69.5)]\";\n"
            + " :infoUrl = \"http://www.oscar.noaa.gov/\";\n"
            + " :institution = \"NOAA PMEL\";\n"
            + " :keywords = \"Oceans > Ocean Circulation > Ocean Currents,\n"
            + "analyses, anomaly, circulation, current, currents, eastward, eastward_sea_water_velocity, meridional, noaa, northward, northward_sea_water_velocity, ocean, oceans, oscar, pmel, real, real time, sea, seawater, surface, time, velocity, water, zonal\";\n"
            + " :keywords_vocabulary = \"GCMD Science Keywords\";\n"
            + " :license = \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + " :Northernmost_Northing = 69.5f; // float\n"
            + " :reference1 = \"Bonjean F. and G.S.E. Lagerloef, 2002, \\\"Diagnostic model and analysis of the surface currents in the tropical Pacific ocean\\\", J. Phys. Oceanogr., 32, 2,938-2,954\";\n"
            + " :source = \"Gary Lagerloef, ESR (lager@esr.org) and Fabrice Bonjean (bonjean@esr.org)\";\n"
            + " :sourceUrl = \"http://dapper.pmel.noaa.gov/dapper/oscar/world-unfilter.nc\";\n"
            + " :Southernmost_Northing = -60.5f; // float\n"
            + " :standard_name_vocabulary = \"CF Standard Name Table v55\";\n"
            + " :summary = \"This project is developing a processing system and data center to provide operational ocean surface velocity fields from satellite altimeter and vector wind data. The method to derive surface currents with satellite altimeter and scatterometer data is the outcome of several years NASA sponsored research. The project will transition that capability to operational oceanographic applications. The end product is velocity maps updated daily, with a goal for eventual 2-day maximum delay from time of satellite measurement. Grid resolution is 100 km for the basin scale, and finer resolution in the vicinity of the Pacific Islands.\";\n"
            + " :title = \"OSCAR - Ocean Surface Current Analyses, Real-Time\";\n"
            + " :VARIABLE = \"Ocean Surface Currents\";\n"
            + " :version = 2006.0f; // float\n"
            + " data:\n"
            + "latitude =\n"
            + "  {69.5, 59.5, 49.5, 39.5, 29.5, 19.5, 9.5, -0.5, -10.5, -20.5, -30.5, -40.5, -50.5, -60.5}\n"
            + "}\n";
    tpo = results.indexOf(expected.substring(0, 17));
    Test.ensureTrue(tpo >= 0, "tpo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tpo, Math.min(results.length(), tpo + expected.length())),
        expected,
        "results=\n" + results);

    // .csv data
    userDapQuery = "u[0][0][(69.5):10:(-69.5)][0]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            eddGrid.className(),
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    String2.log(results);
    expected =
        "time,altitude,latitude,longitude,u\n"
            + "UTC,m,degrees_north,degrees_east,m s-1\n"
            + "1992-10-21T00:00:00Z,-15.0,69.5,20.5,NaN\n"
            + "1992-10-21T00:00:00Z,-15.0,59.5,20.5,NaN\n"
            + "1992-10-21T00:00:00Z,-15.0,49.5,20.5,NaN\n"
            + "1992-10-21T00:00:00Z,-15.0,39.5,20.5,NaN\n"
            + "1992-10-21T00:00:00Z,-15.0,29.5,20.5,NaN\n"
            + "1992-10-21T00:00:00Z,-15.0,19.5,20.5,NaN\n"
            + "1992-10-21T00:00:00Z,-15.0,9.5,20.5,NaN\n"
            + "1992-10-21T00:00:00Z,-15.0,-0.5,20.5,NaN\n"
            + "1992-10-21T00:00:00Z,-15.0,-10.5,20.5,NaN\n"
            + "1992-10-21T00:00:00Z,-15.0,-20.5,20.5,NaN\n"
            + "1992-10-21T00:00:00Z,-15.0,-30.5,20.5,NaN\n"
            + "1992-10-21T00:00:00Z,-15.0,-40.5,20.5,0.08152205\n"
            + "1992-10-21T00:00:00Z,-15.0,-50.5,20.5,0.17953366\n"
            + "1992-10-21T00:00:00Z,-15.0,-60.5,20.5,NaN\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    String mapDapQuery =
        "u[(2008-08-06T00:00:00Z)][][][]"
            + "&.draw=surface&.vars=longitude|latitude|u&.colorBar=|C|Linear|||&.land=under";

    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            mapDapQuery,
            EDStatic.fullTestCacheDirectory,
            eddGrid.className() + "_Map",
            ".png");
    if (doGraphicsTests) Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

    String transparentQuery =
        "u[(2008-08-06T00:00:00Z)][][][]"
            + "&.draw=surface&.vars=longitude|latitude|u&.colorBar=|C|Linear|||&.land=under";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            transparentQuery,
            EDStatic.fullTestCacheDirectory,
            eddGrid.className() + "_Transparent",
            ".transparentPng");
    if (doGraphicsTests) Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

    String query180 =
        "u[(2008-08-06T00:00:00Z)][][][0:(179)]"
            + "&.draw=surface&.vars=longitude|latitude|u&.colorBar=|C|Linear|||&.land=under";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            query180,
            EDStatic.fullTestCacheDirectory,
            eddGrid.className() + "_Map",
            ".kml");
    if (doGraphicsTests) Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

    // currently doesn't work, either ERDDAP needs to rearrange lat and lon values
    // or GeotiffWritter needs to accept descending axis values.
    // tName = eddGrid.makeNewFileForDapQuery(language, null, null, query180,
    // EDStatic.fullTestCacheDirectory,
    // eddGrid.className() + "_Map", ".geotif");
    // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            "u[(2008-08-06T00:00:00Z)][][0:50:(last)][0:50:(179)]",
            EDStatic.fullTestCacheDirectory,
            eddGrid.className() + "_Map",
            ".esriAscii");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    String2.log(results);
    // It seems to toggle frequently between these similar results ...
    // changed slightly 2008-09-07! and 2008-09-24 2008-10-09 2008-10-13 2008-11-11

    expected =
        "ncols 4\n"
            + "nrows 3\n"
            + "xllcenter 20.5\n"
            + "yllcenter -30.5\n"
            + "cellsize 50.0\n"
            + "nodata_value -9999999\n"
            + "-9999999 -9999999 -9999999 -9999999\n";
    // "-9999999 0.085097924 0.032853972 0.16006929\n" +
    // "-9999999 -0.009836693 -9999999 -0.047798216\n";

    // and infinite variants of last 2 lines:
    // "-9999999 0.08509792 0.032853972 0.16006929\n" +
    // "-9999999 -0.009836693 -9999999 -0.047798216\n";

    // "-9999999 0.085097924 0.03285397 0.16006929\n" +
    // "-9999999 -0.009836693 -9999999 -0.047798216\n";

    // "-9999999 0.085097924 0.032853976 0.1600693\n" +
    // "-9999999 -0.009836693 -9999999 -0.047798216\n";

    // "-9999999 0.08509793 0.032853976 0.1600693\n" +
    // "-9999999 -0.009836693 -9999999 -0.047798216\n";

    // "-9999999 0.08509792 0.032853976 0.1600693\n" +
    // "-9999999 -0.009836693 -9999999 -0.047798216\n";

    // but on 2009-02-04 it changed to
    // -9999999 0.07428373 -9999999 0.18457247
    // -9999999 0.0044357306 -9999999 -0.05290417

    // I gave up testing: it changes too often.
    // Test.ensureEqual(results.substring(0, expected.length()), expected,
    // "\nresults=\n" + results);
    // Test.ensureTrue(results.indexOf(" 0.085097") > 0, "\nresults=\n" + results);
    // Test.ensureTrue(results.indexOf(" 0.032853") > 0, "\nresults=\n" + results);
    // Test.ensureTrue(results.indexOf(" 0.160069") > 0, "\nresults=\n" + results);
    // Test.ensureTrue(results.indexOf("-0.00983669") > 0, "\nresults=\n" +
    // results);
    // Test.ensureTrue(results.indexOf("-0.0477982") > 0, "\nresults=\n" + results);

  }

  /**
   * This does important tests with usgsCeCrm10 (which has descending lat axis values).
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagThredds
  @TagImageComparison
  void testDescendingLat() throws Throwable {
    boolean doGraphicsTests = true;
    // String2.log("\n*** EDDGridFromDap.testDescendinglat");
    // testVerboseOn();
    int tSize;
    String name, tName, results, tResults, expected, userDapQuery;
    EDDGrid eddGrid = (EDDGrid) EDDTestDataset.getusgsCeCrm10();
    // if need a different test dataset in the future:
    EDVGridAxis edvga;
    int tpo;
    String query180 =
        SSR.fixPercentEncodedUrl("topo[][]&.draw=surface&.vars=longitude|latitude|topo");
    String query180stride =
        SSR.fixPercentEncodedUrl(
            "topo[0:20:last][0:20:last]&.draw=surface&.vars=longitude|latitude|topo");
    int language = 0;
    /* */
    // ***test some edvga things with descending lat axis
    edvga = eddGrid.axisVariables()[0];
    Test.ensureEqual(edvga.isEvenlySpaced(), true, "");
    Test.ensureEqual(edvga.averageSpacing(), -8.333333333333334E-4, "");
    Test.ensureEqual(edvga.spacingDescription(0), "-8.333333E-4 (even)", "");

    tSize = edvga.sourceValues().size();
    String2.log("tSize-1=" + (tSize - 1)); // 6000
    Test.ensureEqual(edvga.destinationDouble(0), 23, "");
    Test.ensureEqual(edvga.destinationDouble(tSize - 1), 18, "");

    Test.ensureEqual(edvga.destinationToDoubleIndex(25), 0, "");
    Test.ensureEqual(edvga.destinationToDoubleIndex(23.0001), 0, "");
    Test.ensureEqual(edvga.destinationToDoubleIndex(23), 0, "");
    Test.ensureEqual(edvga.destinationToDoubleIndex(22.9999), 0.11999999999982947, "");

    Test.ensureEqual(edvga.destinationToDoubleIndex(20.0001), 3599.88, "");
    Test.ensureEqual(edvga.destinationToDoubleIndex(20), 3600, "");
    Test.ensureEqual(edvga.destinationToDoubleIndex(19.9999), 3600.12, "");

    Test.ensureEqual(edvga.destinationToDoubleIndex(18.0001), 5999.88, "");
    Test.ensureEqual(edvga.destinationToDoubleIndex(18), 6000, "");
    Test.ensureEqual(edvga.destinationToDoubleIndex(17.9999), 6000, "");
    Test.ensureEqual(edvga.destinationToDoubleIndex(16), 6000, "");

    // ***test some edvga things with ascending lon axis
    edvga = eddGrid.axisVariables()[1];
    Test.ensureEqual(edvga.isEvenlySpaced(), true, "");
    Test.ensureEqual(edvga.averageSpacing(), 8.333333333333334E-4, "");
    Test.ensureEqual(edvga.spacingDescription(0), "8.333333E-4 (even)", "");

    tSize = edvga.sourceValues().size();
    String2.log("tSize-1=" + (tSize - 1)); // 9600
    Test.ensureEqual(edvga.destinationDouble(0), -161, "");
    Test.ensureEqual(edvga.destinationDouble(tSize - 1), -153, "");

    Test.ensureEqual(edvga.destinationToDoubleIndex(-162), 0, "");
    Test.ensureEqual(edvga.destinationToDoubleIndex(-161.0001), 0, "");
    Test.ensureEqual(edvga.destinationToDoubleIndex(-161), 0, "");
    Test.ensureEqual(edvga.destinationToDoubleIndex(-160.9999), 0.11999999999982947, "");

    Test.ensureEqual(edvga.destinationToDoubleIndex(-160.0001), 1199.879999999996, "");
    Test.ensureEqual(edvga.destinationToDoubleIndex(-160), 1200, "");
    Test.ensureEqual(edvga.destinationToDoubleIndex(-159.9999), 1200.12, "");

    Test.ensureEqual(edvga.destinationToDoubleIndex(-153.0001), 9599.88, "");
    Test.ensureEqual(edvga.destinationToDoubleIndex(-153), 9600, "");
    Test.ensureEqual(edvga.destinationToDoubleIndex(-152.9999), 9600, "");
    Test.ensureEqual(edvga.destinationToDoubleIndex(-151), 9600, "");

    // lat
    results = eddGrid.axisVariables[0].sliderCsvValues();
    expected = "23, 22.98, 22.96, 22.94, 22.92, 22.9, 22.88, 22.86, 22.84";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
    expected = "18.14, 18.12, 18.1, 18.08, 18.060000000000002, 18.04, 18.02, 18";
    Test.ensureEqual(
        results.substring(results.length() - expected.length()), expected, "results=\n" + results);

    // lon
    results = eddGrid.axisVariables[1].sliderCsvValues();
    expected = "-161, -160.95, -160.9, -160.85, -160.8, -160.75, -160.7, -160.65, -160.6, -160.55,";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
    expected = "-153.45, -153.4, -153.35, -153.3, -153.25, -153.2, -153.15, -153.1, -153.05, -153";
    Test.ensureEqual(
        results.substring(results.length() - expected.length()), expected, "results=\n" + results);

    // .das das isn't affected by userDapQuery
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, "", EDStatic.fullTestCacheDirectory, eddGrid.className(), ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "Attributes {\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float64 actual_range 18.0, 23.0;\n"
            + // important test
            "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range -161.0, -153.0;\n"
            + "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  topo {\n"
            + "    Float32 _FillValue -9999.0;\n"
            + "    Float64 colorBarMaximum 8000.0;\n"
            + "    Float64 colorBarMinimum -8000.0;\n"
            + "    String colorBarPalette \"Topography\";\n"
            + "    String grid_mapping \"GDAL_Geographics\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Topography\";\n"
            + "    String standard_name \"altitude\";\n"
            + "    String units \"meters\";\n"
            + "  }\n"
            + "  NC_GLOBAL {\n"
            + "    String AREA_OR_POINT \"Area\";\n"
            + "    String cdm_data_type \"Grid\";\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "    String creator_email \"Barry.Eakins@noaa.gov\";\n"
            + "    Float64 Easternmost_Easting -153.0;\n"
            + "    Float64 geospatial_lat_max 23.0;\n"
            + "    Float64 geospatial_lat_min 18.0;\n"
            + "    Float64 geospatial_lat_resolution 8.333333333333334e-4;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max -153.0;\n"
            + "    Float64 geospatial_lon_min -161.0;\n"
            + "    Float64 geospatial_lon_resolution 8.333333333333334e-4;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    String history \"Mon Apr 28 13:22:51 2008: ncrename crm_vol10.nc -d x,lon -d y,lat -v Band1,topo\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

    // "2013-08-29T17:33:53Z
    // https://geoport.whoi.edu/thredds/dodsC/bathy/crm_vol10.nc\n" +
    // "2013-08-29T17:33:53Z
    expected =
        "http://127.0.0.1:8080/cwexperimental/griddap/usgsCeCrm10.das\";\n"
            + "    String infoUrl \"https://www.ngdc.noaa.gov/mgg/coastal/coastal.html\";\n"
            + "    String institution \"NOAA NGDC\";\n"
            + "    String keywords \"altitude, arc, atmosphere, bathymetry, coastal, earth science, Earth Science > Oceans > Bathymetry/Seafloor Topography > Bathymetry, hawaii, height, model, ngdc, noaa, oceans, relief, second, station, topography, vol.\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    String naming_authority \"gov.noaa.pfeg.coastwatch\";\n"
            + "    Float64 Northernmost_Northing 23.0;\n"
            + "    String references \"Divins, D.L., and D. Metzger, NGDC Coastal Relief Model, https://www.ngdc.noaa.gov/mgg/coastal/coastal.html\";\n"
            + "    String sourceUrl \"https://geoport.whoi.edu/thredds/dodsC/bathy/crm_vol10.nc\";\n"
            + "    Float64 Southernmost_Northing 18.0;\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String summary \"This Coastal Relief Gridded database provides the first comprehensive view of the US Coastal Zone; one that extends from the coastal state boundaries to as far offshore as the NOS hydrographic data will support a continuous view of the seafloor. In many cases, this seaward limit reaches out to, and in places even beyond the continental slope. The gridded database contains data for the entire coastal zone of the conterminous US, including Hawaii and Puerto Rico.\";\n"
            + "    String title \"Topography, NOAA Coastal Relief Model, 3 arc second, Vol. 10 (Hawaii)\";\n"
            + "    Float64 Westernmost_Easting -161.0;\n"
            + "  }\n"
            + "}\n";
    tpo = results.indexOf(expected.substring(0, 30));
    Test.ensureTrue(tpo >= 0, "\nresults=\n" + results);
    Test.ensureEqual(results.substring(tpo), expected, "\nresults=\n" + results);

    // .dds dds isn't affected by userDapQuery
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, "", EDStatic.fullTestCacheDirectory, eddGrid.className(), ".dds");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "Dataset {\n"
            + "  Float64 latitude[latitude = 6001];\n"
            + "  Float64 longitude[longitude = 9601];\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 topo[latitude = 6001][longitude = 9601];\n"
            + "    MAPS:\n"
            + "      Float64 latitude[latitude = 6001];\n"
            + "      Float64 longitude[longitude = 9601];\n"
            + "  } topo;\n"
            + "} usgsCeCrm10;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .nc lat values subset (subset correct? actual_range correct?)
    userDapQuery = "latitude[(22):10:(21)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            eddGrid.className() + "Lat",
            ".nc");
    results = NcHelper.ncdump(EDStatic.fullTestCacheDirectory + tName, "");
    results = String2.replaceAll(results, "\r", "");
    expected = // note actual range is low to high
        "netcdf EDDGridFromDapLat.nc {\n"
            + "  dimensions:\n"
            + "    latitude = 121;\n"
            + "  variables:\n"
            + "    double latitude(latitude=121);\n"
            + "      :_CoordinateAxisType = \"Lat\";\n"
            + "      :actual_range = 21.0, 22.0; // double\n"
            + "      :axis = \"Y\";\n"
            + "      :ioos_category = \"Location\";\n"
            + "      :long_name = \"Latitude\";\n"
            + "      :standard_name = \"latitude\";\n"
            + "      :units = \"degrees_north\";\n"
            + "\n"
            + "  // global attributes:\n"
            + "  :AREA_OR_POINT = \"Area\";\n"
            + "  :cdm_data_type = \"Grid\";\n"
            + "  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "  :creator_email = \"Barry.Eakins@noaa.gov\";\n"
            + "  :geospatial_lat_max = 22.0; // double\n"
            + "  :geospatial_lat_min = 21.0; // double\n"
            + "  :geospatial_lat_units = \"degrees_north\";\n"
            + "  :history = \"Mon Apr 28 13:22:51 2008: ncrename crm_vol10.nc -d x,lon -d y,lat -v Band1,topo\n";
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "results=\n" + results);

    // 2013-08-29T17:41:13Z
    // https://geoport.whoi.edu/thredds/dodsC/bathy/crm_vol10.nc
    // 2013-08-29T17:41:13Z
    expected =
        "http://127.0.0.1:8080/cwexperimental/griddap/usgsCeCrm10.nc?latitude[(22):10:(21)]\";\n"
            + "  :infoUrl = \"https://www.ngdc.noaa.gov/mgg/coastal/coastal.html\";\n"
            + "  :institution = \"NOAA NGDC\";\n"
            + "  :keywords = \"altitude, arc, atmosphere, bathymetry, coastal, earth science, Earth Science > Oceans > Bathymetry/Seafloor Topography > Bathymetry, hawaii, height, model, ngdc, noaa, oceans, relief, second, station, topography, vol.\";\n"
            + "  :keywords_vocabulary = \"GCMD Science Keywords\";\n"
            + "  :license = \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "  :naming_authority = \"gov.noaa.pfeg.coastwatch\";\n"
            + "  :Northernmost_Northing = 22.0; // double\n"
            + "  :references = \"Divins, D.L., and D. Metzger, NGDC Coastal Relief Model, https://www.ngdc.noaa.gov/mgg/coastal/coastal.html\";\n"
            + "  :sourceUrl = \"https://geoport.whoi.edu/thredds/dodsC/bathy/crm_vol10.nc\";\n"
            + "  :Southernmost_Northing = 21.0; // double\n"
            + "  :standard_name_vocabulary = \"CF Standard Name Table v70\";\n"
            + "  :summary = \"This Coastal Relief Gridded database provides the first comprehensive view of the US Coastal Zone; one that extends from the coastal state boundaries to as far offshore as the NOS hydrographic data will support a continuous view of the seafloor. In many cases, this seaward limit reaches out to, and in places even beyond the continental slope. The gridded database contains data for the entire coastal zone of the conterminous US, including Hawaii and Puerto Rico.\";\n"
            + "  :title = \"Topography, NOAA Coastal Relief Model, 3 arc second, Vol. 10 (Hawaii)\";\n"
            + "\n"
            + "  data:\n"
            + "    latitude = \n"
            + "      {22.0, 21.991666666666667, 21.983333333333334, 21.975, 21.96666666666667, 21.958333333333332, 21.95, 21.941666666666666, 21.933333333333334, 21.925, 21.916666666666668, 21.908333333333335, 21.9, 21.891666666666666, 21.883333333333333, 21.875, 21.866666666666667, 21.858333333333334, 21.85, 21.84166666666667, 21.833333333333332, 21.825, 21.816666666666666, 21.808333333333334, 21.8, 21.791666666666668, 21.783333333333335, 21.775, 21.766666666666666, 21.758333333333333, 21.75, 21.741666666666667, 21.733333333333334, 21.725, 21.71666666666667, 21.708333333333332, 21.7, 21.691666666666666, 21.683333333333334, 21.675, 21.666666666666668, 21.658333333333335, 21.65, 21.641666666666666, 21.633333333333333, 21.625, 21.616666666666667, 21.608333333333334, 21.6, 21.59166666666667, 21.583333333333332, 21.575, 21.566666666666666, 21.558333333333334, 21.55, 21.541666666666668, 21.533333333333335, 21.525, 21.516666666666666, 21.508333333333333, 21.5, 21.491666666666667, 21.483333333333334, 21.475, 21.46666666666667, 21.458333333333332, 21.45, 21.441666666666666, 21.433333333333334, 21.425, 21.416666666666668, 21.408333333333335, 21.4, 21.391666666666666, 21.383333333333333, 21.375, 21.366666666666667, 21.358333333333334, 21.35, 21.34166666666667, 21.333333333333332, 21.325, 21.316666666666666, 21.308333333333334, 21.3, 21.291666666666668, 21.283333333333335, 21.275, 21.266666666666666, 21.258333333333333, 21.25, 21.241666666666667, 21.233333333333334, 21.225, 21.21666666666667, 21.208333333333332, 21.2, 21.191666666666666, 21.183333333333334, 21.175, 21.166666666666668, 21.158333333333335, 21.15, 21.141666666666666, 21.133333333333333, 21.125, 21.116666666666667, 21.108333333333334, 21.1, 21.09166666666667, 21.083333333333332, 21.075, 21.066666666666666, 21.058333333333334, 21.05, 21.041666666666668, 21.033333333333335, 21.025, 21.016666666666666, 21.008333333333333, 21.0}\n"
            + "}\n";
    tpo = results.indexOf(expected.substring(0, 30));
    Test.ensureTrue(tpo >= 0, "tpo=-1 results=\n" + results);
    Test.ensureEqual(results.substring(tpo), expected, "results=\n" + results);

    // .csv data
    userDapQuery = "topo[(20.005):(20.003)][(-156.002):(-156)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            eddGrid.className(),
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    String2.log(results);
    expected =
        "latitude,longitude,topo\n"
            + "degrees_north,degrees_east,meters\n"
            + "20.005,-156.00166666666667,-423.5\n"
            + "20.005,-156.00083333333333,-423.0\n"
            + "20.005,-156.0,-423.1\n"
            + "20.004166666666666,-156.00166666666667,-424.0\n"
            + "20.004166666666666,-156.00083333333333,-423.5\n"
            + "20.004166666666666,-156.0,-423.1\n"
            + "20.003333333333334,-156.00166666666667,-424.0\n"
            + "20.003333333333334,-156.00083333333333,-424.0\n"
            + "20.003333333333334,-156.0,-423.5\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    String mapDapQuery =
        "topo[(23):(18)][(-161):(-153)]" + "&.draw=surface&.vars=longitude|latitude|topo";

    String obsDir = Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR);

    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, mapDapQuery, obsDir, eddGrid.className() + "_Map", ".png");
    if (doGraphicsTests) {
      // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
      Image2Tests.testImagesIdentical(
          tName,
          "EDDGridFromDapTestDescendingLat" + ".png",
          "EDDGridFromDapTestDescendingLat" + "_diff.png");
    }

    String transparentQuery =
        "topo[(23):20:(18)][(-161):20:(-153)]" + "&.draw=surface&.vars=longitude|latitude|topo";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            transparentQuery,
            obsDir,
            eddGrid.className() + "_Transparent",
            ".transparentPng");
    if (doGraphicsTests) {
      // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
      Image2Tests.testImagesIdentical(
          tName,
          "EDDGridFromDapTestDescendingLatTP" + ".png",
          "EDDGridFromDapTestDescendingLatTP" + "_diff.png");
    }

    // this test requires usgsCeCrm10 to be available from localhost erddap
    // (The .kml is produced locally. But it refers to a .png on the localhost
    // erddap.)
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            query180,
            EDStatic.fullTestCacheDirectory,
            eddGrid.className() + "_Map",
            ".kml");
    // if (doGraphicsTests) {
    // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    // }

    // 2013-10-21 this works with new GeotiffWriter (which rearranges lat values)
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            query180stride,
            EDStatic.fullTestCacheDirectory,
            eddGrid.className() + "_Map",
            ".geotif");
    // if (doGraphicsTests) {
    // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    // }

    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            "topo[0:1000:last][0:1000:last]",
            EDStatic.fullTestCacheDirectory,
            eddGrid.className() + "_Map",
            ".esriAscii");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    String2.log(results);
    expected =
        "ncols 10\n"
            + "nrows 7\n"
            + "xllcenter -161.0\n"
            + "yllcenter 18.0\n"
            + "cellsize 0.8333333333333334\n"
            + "nodata_value -9999\n"
            + "-4232.9 -9999 -4865.5 -4753.1 -9999 -9999 -9999 -9999 -9999 -9999\n"
            + "-9999 -1773.1 171.0 -4647.8 -4740.0 -9999 -9999 -9999 -9999 -9999\n"
            + "-9999 -9999 -3645.5 -2465.4 -14.5 -1828.4 -5134.5 -9999 -9999 -9999\n"
            + "-9999 -9999 -9999 -9999 -9999 -397.4 -1850.7 -2912.0 -5551.7 -9999\n"
            + "-9999 -9999 -9999 -9999 -4289.5 -4729.6 79.0 538.0 -4666.6 -9999\n"
            + "-9999 -9999 -9999 -9999 -4391.0 -4526.5 -3830.6 -4417.2 -9999 -9999\n"
            + "-9999 -9999 -9999 -9999 -9999 -9999 -9999 -9999 -9999 -9999\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
  }

  /**
   * This does some tests for Ellyn (not usually run).
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagThredds
  void testForEllyn() throws Throwable {
    String2.log("\n*** EDDGridFromDap.testForEllyn");
    // testVerboseOn();
    String name, tName, results, tResults, expected, userDapQuery;
    String today = Calendar2.getCurrentISODateTimeStringZulu() + "Z";
    int language = 0;

    EDDGrid eddGrid = (EDDGrid) EDDTestDataset.getmb7201adc();

    // .das das isn't affected by userDapQuery
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, "", EDStatic.fullTestCacheDirectory, eddGrid.className(), ".das");
    results = File2.readFromFile88591(EDStatic.fullTestCacheDirectory + tName)[1];
    expected =
        "Attributes {\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range 1.0673667e+9, 1.0747107e+9;\n"
            + "    String axis \"T\";\n"
            + "    Int32 epic_code 624;\n"
            + "    String FORTRAN_format \"F10.2\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String type \"EVEN\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  altitude {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    String _CoordinateZisPositive \"up\";\n"
            + "    Float32 actual_range -9.452215, -0.9522152;\n"
            + "    String axis \"Z\";\n"
            + "    Float64 bin_size 0.5;\n"
            + "    Float64 blanking_distance 0.4399999976158142;\n"
            + "    Int32 epic_code 3;\n"
            + "    String FORTRAN_format \"F10.2\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Altitude\";\n"
            + "    String NOTE \"Depth values were calculated using Surface.exe output\";\n"
            + "    String positive \"up\";\n"
            + "    String standard_name \"altitude\";\n"
            + "    String type \"EVEN\";\n"
            + "    String units \"m\";\n"
            + "    Float64 xducer_offset_from_bottom 1.2599999904632568;\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float32 actual_range 33.6496, 33.6496;\n"
            + "    String axis \"Y\";\n"
            + "    Int32 epic_code 500;\n"
            + "    String FORTRAN_format \"F10.2\";\n"
            + "    String generic_name \"lat\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String name \"LAT\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String type \"EVEN\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float32 actual_range -78.7894, -78.7894;\n"
            + "    String axis \"X\";\n"
            + "    Int32 epic_code 502;\n"
            + "    String FORTRAN_format \"f10.4\";\n"
            + "    String generic_name \"lon\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String name \"LON\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String type \"EVEN\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  u_1205 {\n"
            + "    Float32 _FillValue 1.0E35;\n"
            + "    Float64 bins_questioned 17.0, 18.0, 19.0;\n"
            + "    Float64 bins_std_dev_threshold_for_fill 2.0, 0.5, 0.0;\n"
            + "    Int32 epic_code 1205;\n"
            + "    String generic_name \"u\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Eastward Velocity (cm/s)\";\n"
            + "    Float32 maximum 15.260121, 15.359408, 17.115356, 20.168367, 22.383686, 25.54994, 28.047798, 29.958483, 31.26962, 32.359756, 33.601105, 34.743168, 37.12874, 37.92423, 38.83359, 40.78402, 38.88334, 35.099434;\n"
            + "    Float32 minimum -20.984797, -21.533491, -22.015848, -21.91846, -24.096342, -23.670574, -23.147821, -23.790667, -24.070864, -23.311993, -23.055223, -23.374365, -22.824059, -22.077686, -22.545044, -23.29182, -23.85412, -22.895117;\n"
            + "    String name \"u\";\n"
            + "    String NOTE \"Questionable data in bins likely contaminated by side-lobe surface reflection set to FillValue_\";\n"
            + "    Float64 sensor_depth 10.452199935913086;\n"
            + "    String sensor_type \"RD Instruments ADCP\";\n"
            + "    Float64 serial_number 159.0;\n"
            + "    String standard_name \"eastward_current_velocity\";\n"
            + "    String units \"cm/s\";\n"
            + "    Float64 valid_range -1000.0, 1000.0;\n"
            + "  }\n";
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
    expected =
        "  PGd_1203 {\n"
            + "    Float32 _FillValue 1.0E35;\n"
            + "    Int32 epic_code 1203;\n"
            + "    String generic_name \"PGd\";\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Percent Good Pings\";\n"
            + "    Float32 maximum 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0;\n"
            + "    Float32 minimum 94.25, 50.0, 50.0, 50.0, 50.0, 50.0, 39.5, 34.5, 32.75, 32.5, 32.25, 32.5, 32.0, 31.75, 31.5, 31.75, 27.0, 41.25;\n"
            + "    String name \"PGd\";\n"
            + "    Float64 sensor_depth 10.452199935913086;\n"
            + "    String sensor_type \"RD Instruments ADCP\";\n"
            + "    Float64 serial_number 159.0;\n"
            + "    String standard_name \"none\";\n"
            + "    String units \"counts\";\n"
            + "    Float64 valid_range 0.0, 100.0;\n"
            + "  }\n"
            + "  NC_GLOBAL {\n"
            + "    Int32 ADCP_serial_number 159;\n"
            + "    Int32 beam_angle 20;\n"
            + "    String beam_pattern \"convex\";\n"
            + "    String cdm_data_type \"Grid\";\n"
            + "    Int32 COMPOSITE 0;\n"
            + "    String Conventions \"CF-1.6\";\n"
            + "    String COORD_SYSTEM \"GEOGRAPHIC\";\n"
            + "    String CREATION_DATE \"30-Aug-2006 11:56:52\";\n"
            + "    String DATA_CMNT \"additional information\";\n"
            + "    String DATA_ORIGIN \"USGS WHFS Sed Trans Group\";\n"
            + "    String DATA_SUBTYPE \"MOORED\";\n"
            + "    String DATA_TYPE \"ADCP\";\n"
            + "    String DELTA_T \"900\";\n"
            + "    String Deployment_date \"28-Oct-2003\";\n"
            + "    Int32 DEPTH_CONST 0;\n"
            + "    String DESCRIPT \"Site 1 ADCP\";\n"
            + "    Int32 DRIFTER 0;\n"
            + "    Int32 ending_water_layer 5;\n"
            + "    Int32 error_velocity_threshold 2000;\n"
            + "    String EXPERIMENT \"Myrtle Beach\";\n"
            + "    Int32 false_target_reject_values 255, 255;\n"
            + "    Float64 FILL_FLAG 1.0;\n"
            + "    Float32 firmware_version 16.21;\n"
            + "    Int32 frequency 1200;\n"
            + "    Float64 geospatial_vertical_max 9.452215;\n"
            + "    Float64 geospatial_vertical_min 0.9522152;\n"
            + "    String geospatial_vertical_positive \"down\";\n"
            + "    String geospatial_vertical_units \"m\";\n"
            + "    String history \"histories combined, fill_vals removed from coordinate vars.:WATER_DEPTH related attributes corrected.:Depth subsampled by dolly.m.:Min and max attributes on u, v, and z recalculated using recalculate_adcp_minmax_vals.m on 24-Mar-2006 10:27:10 by C. Sullivan, WHSC; Questionable velocity data in bins likely contaminated by side-lobe surface reflection set to FillValue_ by clean_adcp_bins_std.m V 1.1 on 02-Dec-2005 10:12:05 by C. Sullivan, USGS WHSC; Extra variables deleted by dolly.m.:Written to an EPIC standard data file by adcp2ep.m (version 1.1):Transformed to earth coordinates by runbm2g.m:Bins were trimmed by trimBins.m using 94% of the RDI surface output.:Transformed to earth coordinates by runbm2g.m:Bins were trimmed by trimBins.m using 94% of the RDI surface output.:Ensembles recorded pre and post deployment were trimmed by goodends.m.:The data were filtered using rdi quality control factors in runmask.m.:Converted to netCDF via MATLAB by rdi2cdf.m 3.0 10-Jan-2003\n"
            + today
            + " http://coast-enviro.er.usgs.gov/thredds/dodsC/DATAFILES/MYRTLEBEACH/7201adc-a.nc\n"
            + today
            + " "
            + EDStatic.erddapUrl
            + // in tests, always non-https url
            "/griddap/mb-7201adc.das\";\n"
            + "    String infoUrl \"http://stellwagen.er.usgs.gov/myrtlebeach.html\";\n"
            + "    Float32 inst_depth 10.4522;\n"
            + "    String inst_depth_note \"inst_depth = (water_depth - inst_height); nominal depth below the surface\";\n"
            + "    Float32 inst_height 1.26;\n"
            + "    String inst_height_note \"height in meters above bottom: accurate for tripod mounted intstruments\";\n"
            + "    String INST_TYPE \"RD Instruments ADCP\";\n"
            + "    String institution \"USGS/CMGP\";\n"
            + "    String janus \"4 Beam\";\n"
            + "    Float64 latitude 33.6496;\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    Float64 longitude -78.78939819335938;\n"
            + "    Float64 magnetic_variation -8.22;\n"
            + "    Int32 minmax_percent_good 0, 100;\n"
            + "    String MOORING \"7201\";\n"
            + "    String orientation \"UP\";\n"
            + "    Int32 pings_per_ensemble 300;\n"
            + "    Int32 POS_CONST 0;\n"
            + "    Float32 pred_accuracy 0.4;\n"
            + "    String PROJECT \"WHFC\";\n"
            + "    String Recovery_date \"21-Jan-2004\";\n"
            + "    String sourceUrl \"http://coast-enviro.er.usgs.gov/thredds/dodsC/DATAFILES/MYRTLEBEACH/7201adc-a.nc\";\n"
            + "    String standard_name_vocabulary \"EPIC, CF-1.0\";\n"
            + "    String start_time \"28-Oct-2003 18:45:00\";\n"
            + "    Int32 starting_water_layer 1;\n"
            + "    String stop_time \"21-Jan-2004 18:45:00\";\n"
            + "    String summary \"velocity data from the ADCP on mooring 720\";\n"
            + "    Float32 time_between_ping_groups 1.0;\n"
            + "    String title \"South Carolina Coastal Erosion Study -adcp7201\";\n"
            + "    String transform \"EARTH\";\n"
            + "    Int32 transmit_pulse_length_cm 49;\n"
            + "    Int32 valid_correlation_range 64, 255;\n"
            + "    String VAR_DESC \"u:v:w:Werr:AGC:PGd:hght:Tx\";\n"
            + "    Float64 VAR_FILL 1.0000000409184788e+35;\n"
            + "    Float32 WATER_DEPTH 11.7122;\n"
            + "    String WATER_DEPTH_NOTE \"from ADCP: (m) \";\n"
            + "    String WATER_MASS \"?\";\n"
            + "  }\n"
            + "}\n";
    int tpo = results.indexOf(expected.substring(0, 17));
    Test.ensureTrue(tpo >= 0, "tpo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tpo, Math.min(results.length(), tpo + expected.length())),
        expected,
        "results=\n" + results);

    // .dds dds isn't affected by userDapQuery
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, "", EDStatic.fullTestCacheDirectory, eddGrid.className(), ".dds");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "Dataset {\n"
            + "  Float64 time[time = 8161];\n"
            + "  Float32 altitude[altitude = 18];\n"
            + "  Float32 latitude[latitude = 1];\n"
            + "  Float32 longitude[longitude = 1];\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 u_1205[time = 8161][altitude = 18][latitude = 1][longitude = 1];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 8161];\n"
            + "      Float32 altitude[altitude = 18];\n"
            + "      Float32 latitude[latitude = 1];\n"
            + "      Float32 longitude[longitude = 1];\n"
            + "  } u_1205;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 v_1206[time = 8161][altitude = 18][latitude = 1][longitude = 1];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 8161];\n"
            + "      Float32 altitude[altitude = 18];\n"
            + "      Float32 latitude[latitude = 1];\n"
            + "      Float32 longitude[longitude = 1];\n"
            + "  } v_1206;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 w_1204[time = 8161][altitude = 18][latitude = 1][longitude = 1];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 8161];\n"
            + "      Float32 altitude[altitude = 18];\n"
            + "      Float32 latitude[latitude = 1];\n"
            + "      Float32 longitude[longitude = 1];\n"
            + "  } w_1204;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 Werr_1201[time = 8161][altitude = 18][latitude = 1][longitude = 1];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 8161];\n"
            + "      Float32 altitude[altitude = 18];\n"
            + "      Float32 latitude[latitude = 1];\n"
            + "      Float32 longitude[longitude = 1];\n"
            + "  } Werr_1201;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 AGC_1202[time = 8161][altitude = 18][latitude = 1][longitude = 1];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 8161];\n"
            + "      Float32 altitude[altitude = 18];\n"
            + "      Float32 latitude[latitude = 1];\n"
            + "      Float32 longitude[longitude = 1];\n"
            + "  } AGC_1202;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 PGd_1203[time = 8161][altitude = 18][latitude = 1][longitude = 1];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 8161];\n"
            + "      Float32 altitude[altitude = 18];\n"
            + "      Float32 latitude[latitude = 1];\n"
            + "      Float32 longitude[longitude = 1];\n"
            + "  } PGd_1203;\n"
            + "} mb-7201adc;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .csv altitude values
    userDapQuery = "altitude";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            eddGrid.className() + "Alt",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    String2.log(results);
    expected =
        "altitude\n"
            + "m\n"
            + "-9.452215\n"
            + "-8.952215\n"
            + "-8.452215\n"
            + "-7.952215\n"
            + "-7.452215\n"
            + "-6.952215\n"
            + "-6.452215\n"
            + "-5.952215\n"
            + "-5.452215\n"
            + "-4.952215\n"
            + "-4.452215\n"
            + "-3.9522152\n"
            + "-3.4522152\n"
            + "-2.9522152\n"
            + "-2.4522152\n"
            + "-1.9522152\n"
            + "-1.4522152\n"
            + "-0.9522152\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .csv data values
    userDapQuery = "u_1205[0][0:17][0][0]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            eddGrid.className(),
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    String2.log(results);
    expected =
        "time, altitude, latitude, longitude, u_1205\n"
            + "UTC, m, degrees_north, degrees_east, cm/s\n"
            + "2003-10-28T18:45:00Z, -9.452215, 33.6496, -78.7894, 4.3488593\n"
            + "2003-10-28T18:45:00Z, -8.952215, 33.6496, -78.7894, 5.9566345\n"
            + "2003-10-28T18:45:00Z, -8.452215, 33.6496, -78.7894, 6.794666\n"
            + "2003-10-28T18:45:00Z, -7.952215, 33.6496, -78.7894, 8.219185\n"
            + "2003-10-28T18:45:00Z, -7.452215, 33.6496, -78.7894, 8.064997\n"
            + "2003-10-28T18:45:00Z, -6.952215, 33.6496, -78.7894, 8.445462\n"
            + "2003-10-28T18:45:00Z, -6.452215, 33.6496, -78.7894, 9.1305\n"
            + "2003-10-28T18:45:00Z, -5.952215, 33.6496, -78.7894, 8.146257\n"
            + "2003-10-28T18:45:00Z, -5.452215, 33.6496, -78.7894, 10.3772335\n"
            + "2003-10-28T18:45:00Z, -4.952215, 33.6496, -78.7894, 9.835191\n"
            + "2003-10-28T18:45:00Z, -4.452215, 33.6496, -78.7894, 8.704758\n"
            + "2003-10-28T18:45:00Z, -3.9522152, 33.6496, -78.7894, 6.0494637\n"
            + "2003-10-28T18:45:00Z, -3.4522152, 33.6496, -78.7894, 4.7303987\n"
            + "2003-10-28T18:45:00Z, -2.9522152, 33.6496, -78.7894, 3.260833\n"
            + "2003-10-28T18:45:00Z, -2.4522152, 33.6496, -78.7894, 1.9206865\n"
            + "2003-10-28T18:45:00Z, -1.9522152, 33.6496, -78.7894, 1.6506728\n"
            + "2003-10-28T18:45:00Z, -1.4522152, 33.6496, -78.7894, 1.1185195\n"
            + "2003-10-28T18:45:00Z, -0.9522152, 33.6496, -78.7894, NaN\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
  }

  /**
   * This tests that the query parser is properly dealing with altitude scale_factor = -1 (which
   * makes ascending values into descending). Originally, Dave found a specific problem related to
   * .mat files that had already been fixed but not released.
   */
  @org.junit.jupiter.api.Test
  @TagSlowTests
  void testScaleFactor() throws Throwable {
    // testVerboseOn();
    int language = 0;
    // soda 2.2.4
    // EDDGrid gridDataset = (EDDGridFromDap)
    // EDDGridFromDap.oneFromDatasetsXml(null, "hawaii_d90f_20ee_c4cb");
    EDDGrid gridDataset = (EDDGridFromDap) EDDTestDataset.gethawaii_d90f_20ee_c4cb();
    String query =
        // Dave had (-500):(-5.01) which succeeded for .htmlTable but failed for .mat
        // but I had already fixed/cleaned up erddap's handling of descending axis vars
        // (including altitude axes with negative scale_factor
        // so reformed request with (-500):(-5.01) passes .htmlTable and .mat
        // 2013-01-13 This test changed a lot with dataset change from altitude to depth
        "temp[(2001-12-15T00:00:00)][(5.01):(500)][(23.1)][(185.2)],"
            + "salt[(2001-12-15T00:00:00)][(5.01):(500)][(23.1)][(185.2)],"
            + "u[(2001-12-15T00:00:00)][(5.01):(500)][(23.1)][(185.2)],"
            + "v[(2001-12-15T00:00:00)][(5.01):(500)][(23.1)][(185.2)],"
            + "w[(2001-12-15T00:00:00)][(5.01):(500)][(23.1)][(185.2)]";

    String tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            query,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_soda224",
            ".htmlTable"); // was ok
    String results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    String expected =
        EDStatic.startHeadHtml(
                language, EDStatic.erddapUrl((String) null, language), "EDDGridFromDap_soda224")
            + "\n"
            + "</head>\n"
            + EDStatic.startBodyHtml(language, null, "griddap/hawaii_d90f_20ee_c4cb.html", query)
            + // 2022-11-22
            // .htmlTable
            // converted
            // to
            // .html
            // to
            // avoid
            // user
            // requesting
            // all
            // data
            // in
            // a
            // dataset
            // if
            // they
            // change
            // language
            "&nbsp;<br>\n"
            +
            // HtmlWidgets.BACK_BUTTON +
            "&nbsp;\n"
            + "<table class=\"erd commonBGColor nowrap\">\n"
            + "<tr>\n"
            + "<th>time\n"
            + "<th>depth\n"
            + "<th>latitude\n"
            + "<th>longitude\n"
            + "<th>temp\n"
            + "<th>salt\n"
            + "<th>u\n"
            + "<th>v\n"
            + "<th>w\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<th>UTC\n"
            + "<th>m\n"
            + "<th>degrees_north\n"
            + "<th>degrees_east\n"
            + "<th>degree_C\n"
            + "<th>PSU\n"
            + "<th>m s-1\n"
            + "<th>m s-1\n"
            + "<th>m s-1\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td>2001-12-15T00:00:00Z\n"
            + "<td class=\"R\">5.01\n"
            + // 2014-01-17 was 5.0
            "<td class=\"R\">23.25\n"
            + "<td class=\"R\">185.25\n"
            + "<td class=\"R\">26.7815\n"
            + "<td class=\"R\">35.205196\n"
            + "<td class=\"R\">-0.16983111\n"
            + "<td class=\"R\">0.11358413\n"
            + "<td class=\"R\">2.099171E-10\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td>2001-12-15T00:00:00Z\n"
            + "<td class=\"R\">15.07\n"
            + // 2014-01-17 was 15.0
            "<td class=\"R\">23.25\n"
            + "<td class=\"R\">185.25\n"
            + "<td class=\"R\">26.77543\n"
            + "<td class=\"R\">35.205135\n"
            + "<td class=\"R\">-0.15841055\n"
            + "<td class=\"R\">0.11168823\n"
            + "<td class=\"R\">-6.394319E-7\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td>2001-12-15T00:00:00Z\n"
            + "<td class=\"R\">25.28\n"
            + // 2014-01-17 was 25.0
            "<td class=\"R\">23.25\n"
            + "<td class=\"R\">185.25\n"
            + "<td class=\"R\">26.774588\n"
            + "<td class=\"R\">35.205017\n"
            + "<td class=\"R\">-0.15311892\n"
            + "<td class=\"R\">0.10998611\n"
            + "<td class=\"R\">-1.3381572E-6\n"
            + "</tr>\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            query,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_soda224",
            ".mat"); // threw an exception
    results = File2.hexDump(EDStatic.fullTestCacheDirectory + tName, 1000000);
    expected =
        "4d 41 54 4c 41 42 20 35   2e 30 20 4d 41 54 2d 66   MATLAB 5.0 MAT-f |\n"
            + "69 6c 65 2c 20 43 72 65   61 74 65 64 20 62 79 3a   ile, Created by: |\n"
            + "20 67 6f 76 2e 6e 6f 61   61 2e 70 66 65 6c 2e 63    gov.noaa.pfel.c |\n"
            + "6f 61 73 74 77 61 74 63   68 2e 4d 61 74 6c 61 62   oastwatch.Matlab |\n"
            +
            // "2c 20 43 72 65 61 74 65 64 20 6f 6e 3a 20 46 72 , Created on: Fr |\n" +
            // "69 20 4a 61 6e 20 31 37 20 31 35 3a 30 38 3a 32 i Jan 17 15:08:2 |\n" +
            // "38 20 32 30 31 34 20 20 20 20 20 20 20 20 20 20 8 2014 |\n" +
            "20 20 20 20 00 00 00 00   00 00 00 00 01 00 4d 49                 MI |\n"
            + "00 00 00 0e 00 00 05 d0   00 00 00 06 00 00 00 08                    |\n"
            + "00 00 00 02 00 00 00 00   00 00 00 05 00 00 00 08                    |\n"
            + "00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 15                    |\n"
            + "68 61 77 61 69 69 5f 64   39 30 66 5f 32 30 65 65   hawaii_d90f_20ee |\n"
            + "5f 63 34 63 62 00 00 00   00 04 00 05 00 00 00 20   _c4cb            |\n"
            + "00 00 00 01 00 00 01 20   74 69 6d 65 00 00 00 00           time     |\n"
            + "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            + "00 00 00 00 00 00 00 00   64 65 70 74 68 00 00 00           depth    |\n"
            + "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            + "00 00 00 00 00 00 00 00   6c 61 74 69 74 75 64 65           latitude |\n"
            + "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            + "00 00 00 00 00 00 00 00   6c 6f 6e 67 69 74 75 64           longitud |\n"
            + "65 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00   e                |\n"
            + "00 00 00 00 00 00 00 00   74 65 6d 70 00 00 00 00           temp     |\n"
            + "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            + "00 00 00 00 00 00 00 00   73 61 6c 74 00 00 00 00           salt     |\n"
            + "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            + "00 00 00 00 00 00 00 00   75 00 00 00 00 00 00 00           u        |\n"
            + "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            + "00 00 00 00 00 00 00 00   76 00 00 00 00 00 00 00           v        |\n"
            + "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            + "00 00 00 00 00 00 00 00   77 00 00 00 00 00 00 00           w        |\n"
            + "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            + "00 00 00 00 00 00 00 00   00 00 00 0e 00 00 00 38                  8 |\n"
            + "00 00 00 06 00 00 00 08   00 00 00 06 00 00 00 00                    |\n"
            + "00 00 00 05 00 00 00 08   00 00 00 01 00 00 00 01                    |\n"
            + "00 00 00 01 00 00 00 00   00 00 00 09 00 00 00 08                    |\n"
            + "41 ce 0d 49 40 00 00 00   00 00 00 0e 00 00 00 c8   A  I@            |\n"
            + "00 00 00 06 00 00 00 08   00 00 00 06 00 00 00 00                    |\n"
            + "00 00 00 05 00 00 00 08   00 00 00 13 00 00 00 01                    |\n"
            + "00 00 00 01 00 00 00 00   00 00 00 09 00 00 00 98                    |\n"
            + "40 14 0a 3d 70 a3 d7 0a   40 2e 23 d7 0a 3d 70 a4   @  =p   @.#  =p  |\n"
            + "40 39 47 ae 14 7a e1 48   40 41 e1 47 ae 14 7a e1   @9G  z H@A G  z  |\n"
            + "40 47 4e 14 7a e1 47 ae   40 4c fd 70 a3 d7 0a 3d   @GN z G @L p   = |\n"
            + "40 51 81 47 ae 14 7a e1   40 54 ba e1 47 ae 14 7b   @Q G  z @T  G  { |\n"
            + "40 58 3a e1 47 ae 14 7b   40 5c 14 7a e1 47 ae 14   @X: G  {@\\ z G   |\n"
            + "40 60 2f ae 14 7a e1 48   40 62 9e b8 51 eb 85 1f   @`/  z H@b  Q    |\n"
            + "40 65 6c cc cc cc cc cd   40 68 b9 47 ae 14 7a e1   @el     @h G  z  |\n"
            + "40 6c af 5c 28 f5 c2 8f   40 70 c7 5c 28 f5 c2 8f   @l \\(   @p \\(    |\n"
            + "40 73 da 66 66 66 66 66   40 77 d6 3d 70 a3 d7 0a   @s fffff@w =p    |\n"
            + "40 7d 1e 8f 5c 28 f5 c3   00 00 00 0e 00 00 00 38   @}  \\(         8 |\n"
            + "00 00 00 06 00 00 00 08   00 00 00 06 00 00 00 00                    |\n"
            + "00 00 00 05 00 00 00 08   00 00 00 01 00 00 00 01                    |\n"
            + "00 00 00 01 00 00 00 00   00 00 00 09 00 00 00 08                    |\n"
            + "40 37 40 00 00 00 00 00   00 00 00 0e 00 00 00 38   @7@            8 |\n"
            + "00 00 00 06 00 00 00 08   00 00 00 06 00 00 00 00                    |\n"
            + "00 00 00 05 00 00 00 08   00 00 00 01 00 00 00 01                    |\n"
            + "00 00 00 01 00 00 00 00   00 00 00 09 00 00 00 08                    |\n"
            + "40 67 28 00 00 00 00 00   00 00 00 0e 00 00 00 88   @g(              |\n"
            + "00 00 00 06 00 00 00 08   00 00 00 07 00 00 00 00                    |\n"
            + "00 00 00 05 00 00 00 10   00 00 00 01 00 00 00 13                    |\n"
            + "00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 00                    |\n"
            + "00 00 00 07 00 00 00 4c   41 d6 40 83 41 d6 34 15          LA @ A 4  |\n"
            + "41 d6 32 5b 41 d6 30 7f   41 d6 1d d9 41 d5 ff 8f   A 2[A 0 A   A    |\n"
            + "41 d2 f0 89 41 c9 67 76   41 bd fa 78 41 b3 48 12   A   A gvA  xA H  |\n"
            + "41 ab 5f 71 41 a3 67 76   41 9b 4a e6 41 91 de 8e   A _qA gvA J A    |\n"
            + "41 86 eb e9 41 71 78 64   41 53 c4 80 41 32 55 8b   A   AqxdAS  A2U  |\n"
            + "41 10 98 26 00 00 00 00   00 00 00 0e 00 00 00 88   A  &             |\n"
            + "00 00 00 06 00 00 00 08   00 00 00 07 00 00 00 00                    |\n"
            + "00 00 00 05 00 00 00 10   00 00 00 01 00 00 00 13                    |\n"
            + "00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 00                    |\n"
            + "00 00 00 07 00 00 00 4c   42 0c d2 1f 42 0c d2 0f          LB   B    |\n"
            + "42 0c d1 f0 42 0c d1 d0   42 0c d1 6e 42 0c d0 70   B   B   B  nB  p |\n"
            + "42 0c b3 32 42 0c 5d 0d   42 0c 0b 17 42 0b d7 38   B  2B ] B   B  8 |\n"
            + "42 0b cb 34 42 0b db ba   42 0b fc 25 42 0b fe 8b   B  4B   B  %B    |\n"
            + "42 0b c7 32 42 0b 41 83   42 0a 8c 40 42 09 bf e2   B  2B A B  @B    |\n"
            + "42 09 47 9b 00 00 00 00   00 00 00 0e 00 00 00 88   B G              |\n"
            + "00 00 00 06 00 00 00 08   00 00 00 07 00 00 00 00                    |\n"
            + "00 00 00 05 00 00 00 10   00 00 00 01 00 00 00 13                    |\n"
            + "00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 00                    |\n"
            + "00 00 00 07 00 00 00 4c   be 2d e8 35 be 22 36 60          L - 5 \"6` |\n"
            + "be 1c cb 35 be 19 83 74   be 18 45 db be 19 84 17      5   t  E      |\n"
            + "be 1e 2a a2 be 21 e7 7e   be 23 8d 81 be 20 8d 8e     *  ! ~ #       |\n"
            + "be 19 8b 40 be 10 ab 11   be 06 d6 cd bd f7 80 30      @           0 |\n"
            + "bd de e1 53 bd c5 00 a8   bd ab 5c 7e bd 92 cf 27      S      \\~   ' |\n"
            + "bd 71 15 b9 00 00 00 00   00 00 00 0e 00 00 00 88    q               |\n"
            + "00 00 00 06 00 00 00 08   00 00 00 07 00 00 00 00                    |\n"
            + "00 00 00 05 00 00 00 10   00 00 00 01 00 00 00 13                    |\n"
            + "00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 00                    |\n"
            + "00 00 00 07 00 00 00 4c   3d e8 9e cc 3d e4 bc cc          L=   =    |\n"
            + "3d e1 40 66 3d dd 4d ff   3d d5 1e e8 3d ce 17 d4   = @f= M =   =    |\n"
            + "3d cd ad 86 3d d5 41 f0   3d d8 e2 e0 3d d0 e4 d3   =   = A =   =    |\n"
            + "3d bc e2 86 3d a2 20 e2   3d 84 f9 40 3d 4c 1f fa   =   =   =  @=L   |\n"
            + "3d 0c fe b9 3c ab 92 e4   3c 35 b6 0f 3b a7 b9 78   =   <   <5  ;  x |\n"
            + "3b 40 c9 df 00 00 00 00   00 00 00 0e 00 00 00 88   ;@               |\n"
            + "00 00 00 06 00 00 00 08   00 00 00 07 00 00 00 00                    |\n"
            + "00 00 00 05 00 00 00 10   00 00 00 01 00 00 00 13                    |\n"
            + "00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 00                    |\n"
            + "00 00 00 07 00 00 00 4c   2f 66 ce 69 b5 2b a5 6d          L/f i + m |\n"
            + "b5 b3 9a bb b6 09 19 de   b6 38 c0 1c b6 77 29 9a            8   w)  |\n"
            + "b6 96 a8 25 b6 b0 67 d7   b6 c8 ed 82 b6 e3 fd 89      %  g          |\n"
            + "b7 00 9b a8 b7 0f a6 fd   b7 1e ac 0e b7 2d 56 2e                -V. |\n"
            + "b7 3a e2 f3 b7 45 ef e6   b7 4d 39 fb b7 50 88 d0    :   E   M9  P   |\n"
            + "b7 51 15 7f 00 00 00 00    Q                                         |\n";
    Test.ensureEqual(
        results.substring(0, 71 * 4) + results.substring(71 * 7), // remove the creation
        // dateTime
        expected,
        "results=\n" + results);

    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            query,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_soda224",
            ".asc");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "Dataset {\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 temp[time = 1][depth = 19][latitude = 1][longitude = 1];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 1];\n"
            + "      Float64 depth[depth = 19];\n"
            + "      Float64 latitude[latitude = 1];\n"
            + "      Float64 longitude[longitude = 1];\n"
            + "  } temp;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 salt[time = 1][depth = 19][latitude = 1][longitude = 1];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 1];\n"
            + "      Float64 depth[depth = 19];\n"
            + "      Float64 latitude[latitude = 1];\n"
            + "      Float64 longitude[longitude = 1];\n"
            + "  } salt;";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // ***** 2022-02-07 now this succeeds because of change to ERDDAP: now accept
    // [high:low]
    query = // same as above, but depth values are reversed
        "temp[(2001-12-15T00:00:00)][(500):(5.01)][(23.1)][(185.2)],"
            + "salt[(2001-12-15T00:00:00)][(500):(5.01)][(23.1)][(185.2)],"
            + "u[(2001-12-15T00:00:00)][(500):(5.01)][(23.1)][(185.2)],"
            + "v[(2001-12-15T00:00:00)][(500):(5.01)][(23.1)][(185.2)],"
            + "w[(2001-12-15T00:00:00)][(500):(5.01)][(23.1)][(185.2)]";
    expected = "No error.";
    // was "SimpleException: Query error: For variable=temp axis#1=depth
    // Constraint=\"[(500):(5.01)]\": StartIndex=18 is greater than StopIndex=0.";
    for (int i = 0; i < EDDGridFromDap.dataFileTypeNames.length; i++) {
      String fileType = EDDGridFromDap.dataFileTypeNames[i];

      // skip the fileTypes that don't look at the query (or don't object to errors in
      // it)
      if (String2.indexOf(
              new String[] {
                ".das",
                ".dds",
                ".esriAscii", // fails because request should be for 1 var only
                ".fgdc",
                ".graph",
                ".help",
                ".html",
                ".iso19115",
                ".ncml",
                ".nccsvMetadata",
                ".timeGaps",
                ".wav" // fails because float and double values can't be written to .wav
              },
              fileType)
          >= 0) continue;

      try {
        results = "No error.";
        tName =
            gridDataset.makeNewFileForDapQuery(
                language,
                null,
                null,
                query,
                EDStatic.fullTestCacheDirectory,
                gridDataset.className() + "_soda224",
                fileType);
      } catch (Throwable t) {
        results = MustBe.throwableToString(t);
      }
      Test.ensureEqual(
          results.substring(0, Math.min(results.length(), expected.length())),
          expected,
          "fileType=" + fileType + " results=\n" + results);
    }
  }

  /** This tests sliderCsvValues. */
  @org.junit.jupiter.api.Test
  @TagThredds
  void testSliderCsv() throws Throwable {
    // testVerboseOn();
    String name, tName, results, expected;
    EDDGridFromDap gridDataset;
    int language = 0;

    // test erdBAssta5day
    gridDataset = (EDDGridFromDap) EDDTestDataset.geterdBAssta5day();

    // time
    results = gridDataset.axisVariables[0].sliderCsvValues();
    expected = "\"2002-07-06T12:00:00Z\", \"2002-08-01T12:00:00Z\", \"2002-09-01T12:00:00Z\",";
    Test.ensureEqual(
        results.substring(0, expected.length()),
        expected,
        "results=\n" + results + "\n\nThese expected values should rarely (never?) change.\n");
    expected =
        "\"2013-12-29T12:00:00Z\", \"2014-02-01T12:00:00Z\", \"2014-03-02T12:00:00Z\", \"2014-03-05T12:00:00Z\"";
    Test.ensureTrue(
        results.endsWith(expected),
        "results=\n"
            + results
            + "\n"
            + "\nexpected="
            + expected
            + "\nThis changes often.\n"
            + "Normally, the penultimate 3 are YYYY-MM-01 and last is most recent date.\n"
            + "But sometimes there is no data for -01 so nearby dates are used instead.");

    // alt
    results = gridDataset.axisVariables[1].sliderCsvValues();
    expected = "0";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // lat
    results = gridDataset.axisVariables[2].sliderCsvValues();
    expected = "-75, -74, -73, -72, -71, -70, -69, -68, -67, -66,";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
    expected = "64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75";
    Test.ensureEqual(
        results.substring(results.length() - expected.length()), expected, "results=\n" + results);

    // lon
    results = gridDataset.axisVariables[3].sliderCsvValues();
    expected = "0, 2, 4, 6, 8, 10, 12, 14, 16, 18,";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
    expected = "350, 352, 354, 356, 358, 360";
    Test.ensureEqual(
        results.substring(results.length() - expected.length()), expected, "results=\n" + results);

    // *** SEE also the oscar tests

    // *** test of small axis
    String2.log("\ntest of int axis with 100 values");

    long time = System.currentTimeMillis();
    IntArray vals = new IntArray();
    for (int i = 0; i < 100; i++) vals.add(12 + i);
    String2.log("  make IntArray time=" + (System.currentTimeMillis() - time) + "ms");

    EDVGridAxis edvga =
        new EDVGridAxis("testDatasetID", "x", "x", new Attributes(), new Attributes(), vals);
    time = System.currentTimeMillis();
    results = edvga.sliderCsvValues();
    expected = "12, 13, 14, 15, 16, 17, 18, 19,";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
    expected = "107, 108, 109, 110, 111";
    Test.ensureEqual(
        results.substring(results.length() - expected.length()), expected, "results=\n" + results);
    String2.log("  sliderCsvValues time=" + (System.currentTimeMillis() - time) + "ms");

    // *** test of huge axis
    String2.log("\ntest of int axis with 10^6 values");

    time = System.currentTimeMillis();
    vals = new IntArray();
    for (int i = 0; i < 10000000; i++) vals.add(123456 + i);
    String2.log("  make IntArray time=" + (System.currentTimeMillis() - time) + "ms");

    edvga = new EDVGridAxis("testDatasetID", "x", "x", new Attributes(), new Attributes(), vals);
    time = System.currentTimeMillis();
    results = edvga.sliderCsvValues();
    expected = "123456, 150000, 200000, 250000, 300000, 350000, 400000, 450000,";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
    expected = "9750000, 9800000, 9850000, 9900000, 9950000, 10000000, 10050000, 10123455";
    Test.ensureEqual(
        results.substring(results.length() - expected.length()), expected, "results=\n" + results);
    String2.log("  sliderCsvValues time=" + (System.currentTimeMillis() - time) + "ms");

    // *** test of huge time axis
    String2.log("\ntest of time axis with 10^6 values");

    time = System.currentTimeMillis();
    DoubleArray seconds = new DoubleArray();
    for (int i = 0; i < 10000000; i++) seconds.add(123456 + i);
    String2.log("  make DoubleArray time=" + (System.currentTimeMillis() - time) + "ms");

    // test EDVTimeStampGridAxis
    EDVTimeStampGridAxis edvtsga =
        new EDVTimeStampGridAxis(
            "testDatasetID",
            "mytime",
            null,
            new Attributes().add("units", Calendar2.SECONDS_SINCE_1970),
            new Attributes(),
            seconds);
    time = System.currentTimeMillis();
    results = edvtsga.sliderCsvValues();
    expected =
        "\"1970-01-02T10:17:36Z\", \"1970-01-02T12:00:00Z\", \"1970-01-03\", \"1970-01-03T12:00:00Z\", \"1970-01-04\", \"1970-01-04T12:00:00Z\",";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
    expected = "\"1970-04-27\", \"1970-04-27T12:00:00Z\", \"1970-04-28\", \"1970-04-28T04:04:15Z\"";
    Test.ensureEqual(
        results.substring(results.length() - expected.length()), expected, "results=\n" + results);
    String2.log("  TimeStamp sliderCsvValues time=" + (System.currentTimeMillis() - time) + "ms");

    // EDVTimeGridAxis
    EDVTimeGridAxis edvtga =
        new EDVTimeGridAxis(
            "testDatasetID",
            "time",
            new Attributes().add("units", Calendar2.SECONDS_SINCE_1970),
            new Attributes(),
            seconds);
    time = System.currentTimeMillis();
    results = edvtga.sliderCsvValues();
    expected =
        "\"1970-01-02T10:17:36Z\", \"1970-01-02T12:00:00Z\", \"1970-01-03\", \"1970-01-03T12:00:00Z\", \"1970-01-04\", \"1970-01-04T12:00:00Z\",";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
    expected = "\"1970-04-27\", \"1970-04-27T12:00:00Z\", \"1970-04-28\", \"1970-04-28T04:04:15Z\"";
    Test.ensureEqual(
        results.substring(results.length() - expected.length()), expected, "results=\n" + results);
    String2.log("  Time sliderCsvValues time=" + (System.currentTimeMillis() - time) + "ms");
  }

  /** This tests a depth axis variable. */
  @org.junit.jupiter.api.Test
  void testGridWithDepth2() throws Throwable {
    String2.log("\n*** EDDGridFromDap.testGridWithDepth2");
    String results, expected, tName;
    int po;
    int language = 0;

    // test generateDatasetsXml -- It should catch z variable and convert to
    // altitude.
    // !!! I don't have a test dataset with real altitude data that isn't already
    // called altitude!
    /*
     * String url =
     * "http://www.marine.csiro.au/dods/nph-dods/dods-data/bl/BRAN2.1/bodas/19921014.bodas_ts.nc";
     * results = generateDatasetsXml(true, url,
     * null, null, null, 10080, null);
     * po = results.indexOf("<sourceName>z</sourceName>");
     * Test.ensureTrue(po >= 0, "results=\n" + results);
     * expected =
     * "<sourceName>z</sourceName>\n" +
     * "        <destinationName>depth</destinationName>\n" +
     * "        <!-- sourceAttributes>\n" +
     * "            <att name=\"cartesian_axis\">Z</att>\n" +
     * "            <att name=\"long_name\">Depth</att>\n" +
     * "            <att name=\"positive\">down</att>\n" +
     * "            <att name=\"units\">m</att>\n" +
     * "        </sourceAttributes -->\n" +
     * "        <addAttributes>\n" +
     * "            <att name=\"ioos_category\">Location</att>\n" +
     * "            <att name=\"standard_name\">depth</att>\n" +
     * "        </addAttributes>\n" +
     * "    </axisVariable>";
     * Test.ensureEqual(results.substring(po, po + expected.length()), expected,
     * "results=\n" + results);
     */

    // Test that constructor of EDVDepthGridAxis added proper metadata for depth
    // variable.
    // EDDGridFromDap gridDataset = (EDDGridFromDap)
    // EDDGridFromDap.oneFromDatasetsXml(null, "hawaii_d90f_20ee_c4cb");
    EDDGridFromDap gridDataset = (EDDGridFromDap) EDDTestDataset.gethawaii_d90f_20ee_c4cb();
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "testGridWithDepth2",
            ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    po = results.indexOf("depth {");
    Test.ensureTrue(po >= 0, "results=\n" + results);
    expected =
        "depth {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + //
            "    String _CoordinateZisPositive \"down\";\n"
            + //
            "    Float64 actual_range 5.01, 5375.0;\n"
            + //
            "    String axis \"Z\";\n"
            + //
            (EDStatic.useSaxParser ? "    String grads_dim \"z\";\n" : "")
            + (EDStatic.useSaxParser ? "    String grads_mapping \"levels\";\n" : "")
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Depth\";\n"
            + (EDStatic.useSaxParser ? "    Float64 maximum 5375.0;\n" : "")
            + (EDStatic.useSaxParser ? "    Float64 minimum 5.01;\n" : "")
            + (EDStatic.useSaxParser ? "    String name \"Depth\";\n" : "")
            + "    String positive \"down\";\n"
            + (EDStatic.useSaxParser ? "    Float32 resolution 137.69205;\n" : "")
            + "    String standard_name \"depth\";\n"
            + "    String units \"m\";\n"
            + "  }";
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    // FGDC should deal with depth correctly
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "testGridWithDepth2",
            ".fgdc");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    po = results.indexOf("<vertdef>");
    Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
    expected =
        "<vertdef>\n"
            + "      <depthsys>\n"
            + "        <depthdn>Unknown</depthdn>\n"
            + "        <depthres>Unknown</depthres>\n"
            + "        <depthdu>meters</depthdu>\n"
            + "        <depthem>Explicit depth coordinate included with horizontal coordinates</depthem>\n"
            + "      </depthsys>\n"
            + "    </vertdef>\n"
            + "  </spref>";
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    // ISO 19115 should deal with depth correctly
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "testGridWithDepth2",
            ".iso19115");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);

    po = results.indexOf("codeListValue=\"vertical\">");
    Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
    expected =
        "codeListValue=\"vertical\">vertical</gmd:MD_DimensionNameTypeCode>\n"
            + "          </gmd:dimensionName>\n"
            + "          <gmd:dimensionSize>\n"
            + "            <gco:Integer>40</gco:Integer>\n"
            + "          </gmd:dimensionSize>\n"
            + "          <gmd:resolution>\n"
            + "            <gco:Measure uom=\"m\">137.69205128205127</gco:Measure>\n"
            + // 2014-01-17
            // was
            // 137.66666666666666
            "          </gmd:resolution>\n"
            + "        </gmd:MD_Dimension>\n"
            + "      </gmd:axisDimensionProperties>\n";
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    po = results.indexOf("<gmd:EX_VerticalExtent>");
    Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
    expected =
        "<gmd:EX_VerticalExtent>\n"
            + "              <gmd:minimumValue><gco:Real>-5375.0</gco:Real></gmd:minimumValue>\n"
            + "              <gmd:maximumValue><gco:Real>-5.01</gco:Real></gmd:maximumValue>\n"
            + "              <gmd:verticalCRS gco:nilReason=\"missing\"/>\n"
            + "            </gmd:EX_VerticalExtent>";
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);
  }

  /**
   * 2013-10-24 INACTIVE. THE TEST DATASET IS NO LONGER AVAILABLE. This tests a depth axis variable.
   * This requires testGridWithDepth dataset in localhost ERDDAP.
   */
  /*
   * void testGridWithDepth() throws Throwable {
   * String2.log("\n*** EDDGridFromDap.testGridWithDepth");
   * String results, expected, tName;
   * int po;
   * int language = 0;
   *
   * //test generateDatasetsXml -- It should catch z variable and convert to
   * depth.
   * String url =
   * "http://www.marine.csiro.au/dods/nph-dods/dods-data/bl/BRAN2.1/bodas/19921014.bodas_ts.nc";
   * results = generateDatasetsXml(true, url,
   * null, null, null, DEFAULT_RELOAD_EVERY_N_MINUTES, null);
   * po = results.indexOf("<sourceName>z</sourceName>");
   * Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
   * expected =
   * "<sourceName>z</sourceName>\n" +
   * "        <destinationName>depth</destinationName>\n" +
   * "        <!-- sourceAttributes>\n" +
   * "            <att name=\"cartesian_axis\">Z</att>\n" +
   * "            <att name=\"long_name\">Depth</att>\n" +
   * "            <att name=\"positive\">down</att>\n" +
   * "            <att name=\"units\">m</att>\n" +
   * "        </sourceAttributes -->\n" +
   * "        <addAttributes>\n" +
   * "            <att name=\"ioos_category\">Location</att>\n" +
   * "            <att name=\"standard_name\">depth</att>\n" +
   * "        </addAttributes>\n" +
   * "    </axisVariable>";
   * Test.ensureEqual(results.substring(po, po + expected.length()), expected,
   * "results=\n" + results);
   *
   * //Test that constructor of EDVDepthGridAxis added proper metadata for depth
   * variable.
   * EDDGridFromDap gridDataset = (EDDGridFromDap)oneFromDatasetsXml(null,
   * "testGridWithDepth");
   * tName = gridDataset.makeNewFileForDapQuery(language, null, null, "",
   * EDStatic.fullTestCacheDirectory, "EDDGridLonPM180_testGridWithDepth",
   * ".das");
   * results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory +
   * tName);
   * po = results.indexOf("depth {");
   * Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
   * expected =
   * "depth {\n" +
   * "    String _CoordinateAxisType \"Height\";\n" +
   * "    String _CoordinateZisPositive \"down\";\n" +
   * "    Float32 actual_range 5.0, 4499.0;\n" +
   * "    String axis \"Z\";\n" +
   * "    String cartesian_axis \"Z\";\n" +
   * "    String ioos_category \"Location\";\n" +
   * "    String long_name \"Depth\";\n" +
   * "    String positive \"down\";\n" +
   * "    String standard_name \"depth\";\n" +
   * "    String units \"m\";\n" +
   * "  }";
   * Test.ensureEqual(results.substring(po, po + expected.length()), expected,
   * "results=\n" + results);
   *
   * //FGDC should deal with depth correctly
   * tName = gridDataset.makeNewFileForDapQuery(language, null, null, "",
   * EDStatic.fullTestCacheDirectory, "EDDGridLonPM180_testGridWithDepth",
   * ".fgdc");
   * results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory +
   * tName);
   * po = results.indexOf("<vertdef>");
   * Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
   * expected =
   * "<vertdef>\n" +
   * "      <depthsys>\n" +
   * "        <depthdn>Unknown</depthdn>\n" +
   * "        <depthres>Unknown</depthres>\n" +
   * "        <depthdu>meters</depthdu>\n" +
   * "        <depthem>Explicit depth coordinate included with horizontal coordinates</depthem>\n"
   * +
   * "      </depthsys>\n" +
   * "    </vertdef>\n" +
   * "  </spref>";
   * Test.ensureEqual(results.substring(po, po + expected.length()), expected,
   * "results=\n" + results);
   *
   * //ISO 19115 should deal with depth correctly
   * tName = gridDataset.makeNewFileForDapQuery(language, null, null, "",
   * EDStatic.fullTestCacheDirectory, "EDDGridLonPM180_testGridWithDepth",
   * ".iso19115");
   * results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory +
   * tName);
   *
   * po = results.indexOf(
   * "codeListValue=\"vertical\">");
   * Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
   * expected =
   * "codeListValue=\"vertical\">vertical</gmd:MD_DimensionNameTypeCode>\n" +
   * "          </gmd:dimensionName>\n" +
   * "          <gmd:dimensionSize>\n" +
   * "            <gco:Integer>47</gco:Integer>\n" +
   * "          </gmd:dimensionSize>\n" +
   * "          <gmd:resolution>\n" +
   * "            <gco:Measure uom=\"m\">97.69565217391305</gco:Measure>\n" +
   * "          </gmd:resolution>\n" +
   * "        </gmd:MD_Dimension>\n" +
   * "      </gmd:axisDimensionProperties>\n";
   * Test.ensureEqual(results.substring(po, po + expected.length()), expected,
   * "results=\n" + results);
   *
   * po = results.indexOf(
   * "<gmd:EX_VerticalExtent>");
   * Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
   * expected =
   * "<gmd:EX_VerticalExtent>\n" +
   * "              <gmd:minimumValue><gco:Real>-4499.0</gco:Real></gmd:minimumValue>\n"
   * +
   * "              <gmd:maximumValue><gco:Real>-5.0</gco:Real></gmd:maximumValue>\n"
   * +
   * "              <gmd:verticalCRS gco:nilReason=\"missing\"/>\n" +
   * "            </gmd:EX_VerticalExtent>";
   * Test.ensureEqual(results.substring(po, po + expected.length()), expected,
   * "results=\n" + results);
   *
   * //test WMS 1.1.0 service getCapabilities from localhost erddap
   * String2.log("\nTest WMS 1.1.0 getCapabilities\n" +
   * "!!! This test requires testGridWithDepth dataset in localhost ERDDAP!!!");
   * results = SSR.getUrlResponseStringUnchanged(
   * "http://localhost:8080/cwexperimental/wms/testGridWithDepth/request?" +
   * "service=WMS&request=GetCapabilities&version=1.1.0");
   * po = results.indexOf("</Layer>");
   * Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
   * expected =
   * "</Layer>\n" +
   * "      <Layer>\n" +
   * "        <Title>testGridWithDepth</Title>\n" +
   * "        <SRS>EPSG:4326</SRS>\n" +
   * "        <LatLonBoundingBox minx=\"1.0\" miny=\"-74.95\" maxx=\"359.002\" maxy=\"73.99947\" />\n"
   * +
   * "        <BoundingBox SRS=\"EPSG:4326\" minx=\"1.0\" miny=\"-74.95\" maxx=\"359.002\" maxy=\"73.99947\" />\n"
   * +
   * "        <Dimension name=\"time\" units=\"ISO8601\" />\n" +
   * "        <Dimension name=\"elevation\" units=\"EPSG:5030\" />\n" +
   * "        <Extent name=\"time\" default=\"1992-10-14T00:00:00Z\" >1992-10-14T00:00:00Z</Extent>\n"
   * +
   * "        <Extent name=\"elevation\" default=\"-4499.0\" >-5.0,-15.0,-25.0,-35.0,-45.0,-55.0,-65.0,-75.0,-85.0,-95.0,-105.0,-115.0,-125.0,-135.0,-145.0,-155.0,-165.0,-175.0,-185.0,-195.0,-205.0,-216.50769,-232.35658,-254.85658,-285.51538,-324.8566,-372.3566,-426.50772,-485.00003,-545.0,-609.0192,-684.0192,-774.0192,-879.0192,-995.0,-1115.0,-1237.4758,-1366.8885,-1506.3256,-1656.8885,-1817.4758,-1985.0,-2155.0,-2404.449,-2861.898,-3576.449,-4499.0</Extent>"
   * ;
   * Test.ensureEqual(results.substring(po, po + expected.length()), expected,
   * "results=\n" + results);
   *
   * //test WMS 1.1.0 elevation=-5
   * tName = EDStatic.fullTestCacheDirectory + gridDataset.className() +
   * "testGridWithDepth110e5.png";
   * SSR.downloadFile(
   * "http://localhost:8080/cwexperimental/wms/testGridWithDepth/request?" +
   * "EXCEPTIONS=INIMAGE&VERSION=1.1.0&SRS=EPSG%3A4326&LAYERS=testGridWithDepth%3Atemp_inc"
   * +
   * "&TIME=1992-10-14T00%3A00%3A00Z&ELEVATION=-5.0&TRANSPARENT=true&BGCOLOR=0x808080"
   * +
   * "&FORMAT=image%2Fpng&SERVICE=WMS&REQUEST=GetMap&STYLES=" +
   * "&BBOX=153.6,-90,307.2,63.6&WIDTH=256&HEIGHT=256",
   * tName, false);
   * Test.displayInBrowser("file://" + tName);
   *
   * //test WMS 1.1.0 elevation=default
   * tName = EDStatic.fullTestCacheDirectory + gridDataset.className() +
   * "testGridWithDepth110edef.png";
   * SSR.downloadFile(
   * "http://localhost:8080/cwexperimental/wms/testGridWithDepth/request?" +
   * "EXCEPTIONS=INIMAGE&VERSION=1.1.0&SRS=EPSG%3A4326&LAYERS=testGridWithDepth%3Atemp_inc"
   * +
   * "&TIME=1992-10-14T00%3A00%3A00Z&TRANSPARENT=true&BGCOLOR=0x808080" +
   * "&FORMAT=image%2Fpng&SERVICE=WMS&REQUEST=GetMap&STYLES=" +
   * "&BBOX=153.6,-90,307.2,63.6&WIDTH=256&HEIGHT=256",
   * tName, false);
   * Test.displayInBrowser("file://" + tName);
   *
   *
   * //test WMS 1.3.0 service getCapabilities from localhost erddap
   * String2.log("\nTest WMS 1.3.0 getCapabilities\n" +
   * "!!! This test requires testGridWithDepth dataset in localhost ERDDAP!!!");
   * results = SSR.getUrlResponseStringUnchanged(
   * "http://localhost:8080/cwexperimental/wms/testGridWithDepth/request?" +
   * "service=WMS&request=GetCapabilities&version=1.3.0");
   * po = results.indexOf("</Layer>");
   * Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
   * expected =
   * "</Layer>\n" +
   * "      <Layer>\n" +
   * "        <Title>testGridWithDepth</Title>\n" +
   * "        <CRS>CRS:84</CRS>\n" +
   * "        <CRS>EPSG:4326</CRS>\n" +
   * "        <EX_GeographicBoundingBox>\n" +
   * "          <westBoundLongitude>1.0</westBoundLongitude>\n" +
   * "          <eastBoundLongitude>359.002</eastBoundLongitude>\n" +
   * "          <southBoundLatitude>-74.95</southBoundLatitude>\n" +
   * "          <northBoundLatitude>73.99947</northBoundLatitude>\n" +
   * "        </EX_GeographicBoundingBox>\n" +
   * "        <BoundingBox CRS=\"EPSG:4326\" minx=\"1.0\" miny=\"-74.95\" maxx=\"359.002\" maxy=\"73.99947\" />\n"
   * +
   * "        <Dimension name=\"time\" units=\"ISO8601\" multipleValues=\"0\" nearestValue=\"1\" default=\"1992-10-14T00:00:00Z\" >1992-10-14T00:00:00Z</Dimension>\n"
   * +
   * "        <Dimension name=\"elevation\" units=\"CRS:88\" unitSymbol=\"m\" multipleValues=\"0\" nearestValue=\"1\" default=\"-4499.0\" >-5.0,-15.0,-25.0,-35.0,-45.0,-55.0,-65.0,-75.0,-85.0,-95.0,-105.0,-115.0,-125.0,-135.0,-145.0,-155.0,-165.0,-175.0,-185.0,-195.0,-205.0,-216.50769,-232.35658,-254.85658,-285.51538,-324.8566,-372.3566,-426.50772,-485.00003,-545.0,-609.0192,-684.0192,-774.0192,-879.0192,-995.0,-1115.0,-1237.4758,-1366.8885,-1506.3256,-1656.8885,-1817.4758,-1985.0,-2155.0,-2404.449,-2861.898,-3576.449,-4499.0</Dimension>"
   * ;
   * Test.ensureEqual(results.substring(po, po + expected.length()), expected,
   * "results=\n" + results);
   *
   * //test WMS 1.3.0 elevation=-5
   * tName = EDStatic.fullTestCacheDirectory + gridDataset.className() +
   * "testGridWithDepth130e5.png";
   * SSR.downloadFile(
   * "http://localhost:8080/cwexperimental/wms/testGridWithDepth/request?" +
   * "EXCEPTIONS=INIMAGE&VERSION=1.3.0&SRS=EPSG%3A4326&LAYERS=testGridWithDepth%3Atemp_inc"
   * +
   * "&TIME=1992-10-14T00%3A00%3A00Z&ELEVATION=-5.0&TRANSPARENT=true&BGCOLOR=0x808080"
   * +
   * "&FORMAT=image%2Fpng&SERVICE=WMS&REQUEST=GetMap&STYLES=" +
   * "&BBOX=153.6,-90,307.2,63.6&WIDTH=256&HEIGHT=256",
   * tName, false);
   * Test.displayInBrowser("file://" + tName);
   *
   * //test WMS 1.3.0 elevation=default
   * tName = EDStatic.fullTestCacheDirectory + gridDataset.className() +
   * "testGridWithDepth130edef.png";
   * SSR.downloadFile(
   * "http://localhost:8080/cwexperimental/wms/testGridWithDepth/request?" +
   * "EXCEPTIONS=INIMAGE&VERSION=1.3.0&SRS=EPSG%3A4326&LAYERS=testGridWithDepth%3Atemp_inc"
   * +
   * "&TIME=1992-10-14T00%3A00%3A00Z&TRANSPARENT=true&BGCOLOR=0x808080" +
   * "&FORMAT=image%2Fpng&SERVICE=WMS&REQUEST=GetMap&STYLES=" +
   * "&BBOX=153.6,-90,307.2,63.6&WIDTH=256&HEIGHT=256",
   * tName, false);
   * Test.displayInBrowser("file://" + tName);
   *
   * }
   */

  /** This tests non-nc-"Grid" data variable (dimensions don't have axis/coordinate variable). */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest
  void testNoAxisVariable() throws Throwable {

    // testVerboseOn();
    String name, tName, results, tResults, expected, userDapQuery;
    String today = Calendar2.getCurrentISODateTimeStringZulu() + "Z";
    int language = 0;

    try {
      EDDGrid eddGrid = (EDDGrid) EDDGridFromDap.oneFromDatasetsXml(null, "testNoAxisVariable");

      // .das das isn't affected by userDapQuery
      tName =
          eddGrid.makeNewFileForDapQuery(
              language,
              null,
              null,
              "",
              EDStatic.fullTestCacheDirectory,
              eddGrid.className(),
              ".das");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      String2.log(results);
      expected =
          "Attributes {\n"
              + "  time {\n"
              + "    String _CoordinateAxisType \"Time\";\n"
              + "    Float64 actual_range 1.075464e+9, 1.075464e+9;\n"
              + "    String axis \"T\";\n"
              + "    String calendar \"standard\";\n"
              + "    String field \"time, scalar, series\";\n"
              + "    String ioos_category \"Time\";\n"
              + "    String long_name \"averaged time since initialization\";\n"
              + "    String standard_name \"time\";\n"
              + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
              + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
              + "  }\n"
              + "  eta_rho {\n"
              + "    Int16 actual_range 0, 641;\n"
              + "    String ioos_category \"Location\";\n"
              + "    String units \"count\";\n"
              + "  }\n"
              + "  xi_rho {\n"
              + "    Int16 actual_range 0, 225;\n"
              + "    String ioos_category \"Location\";\n"
              + "    String units \"count\";\n"
              + "  }\n"
              + "  zeta {\n"
              + "    String coordinates \"lat_rho lon_rho\";\n"
              + "    String field \"free-surface, scalar, series\";\n"
              + "    String ioos_category \"Other\";\n"
              + "    String long_name \"averaged free-surface\";\n"
              + "    String time \"ocean_time\";\n"
              + "    String units \"meter\";\n"
              + "  }\n"
              + "  NC_GLOBAL {\n"
              + "    String avg_base \"nep4_avg\";\n"
              + "    String bry_file \"/wrkdir/kate/NEP4_djd/NEP4_bry_CCSM_2000-2004.nc\";\n";
      tResults = results.substring(0, Math.min(results.length(), expected.length()));
      Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

      expected =
          "String tiling \"003x016\";\n"
              + "    String time_coverage_end \"2004-01-30T12:00:00Z\";\n"
              + "    String time_coverage_start \"2004-01-30T12:00:00Z\";\n"
              + "    String title \"ROMS/TOMS 3.0 - Northeast Pacific 10km Grid (NEP4)\";\n"
              + "    String type \"ROMS/TOMS averages file\";\n"
              + "    String var_info \"External/varinfo.dat\";\n"
              + "  }\n"
              + "}\n";
      int tpo = results.indexOf("String tiling ");
      Test.ensureTrue(tpo >= 0, "tpo=-1 results=\n" + results);
      Test.ensureEqual(results.substring(tpo), expected, "results=\n" + results);

      // .dds dds isn't affected by userDapQuery
      tName =
          eddGrid.makeNewFileForDapQuery(
              language,
              null,
              null,
              "",
              EDStatic.fullTestCacheDirectory,
              eddGrid.className(),
              ".dds");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      expected =
          "Dataset {\n"
              + "  Float64 time[time = 1];\n"
              + "  Int16 eta_rho[eta_rho = 642];\n"
              + "  Int16 xi_rho[xi_rho = 226];\n"
              + "  GRID {\n"
              + "    ARRAY:\n"
              + "      Float32 zeta[time = 1][eta_rho = 642][xi_rho = 226];\n"
              + "    MAPS:\n"
              + "      Float64 time[time = 1];\n"
              + "      Int16 eta_rho[eta_rho = 642];\n"
              + "      Int16 xi_rho[xi_rho = 226];\n"
              + "  } zeta;\n"
              + "} testNoAxisVariable;\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);

      // .csv an index values
      userDapQuery = "xi_rho[0:2:5]";
      tName =
          eddGrid.makeNewFileForDapQuery(
              language,
              null,
              null,
              userDapQuery,
              EDStatic.fullTestCacheDirectory,
              eddGrid.className() + "XiRho",
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      expected = "xi_rho\n" + "count\n" + "0\n" + "2\n" + "4\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);

      // .csv data values
      userDapQuery = "zeta[0][0:2][0:2]";
      tName =
          eddGrid.makeNewFileForDapQuery(
              language,
              null,
              null,
              userDapQuery,
              EDStatic.fullTestCacheDirectory,
              eddGrid.className() + "_NAV",
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      expected =
          // from source
          // http://edac-dap2.northerngulfinstitute.org/thredds/dodsC/roms/al_roms/nep4_004.nc.ascii?zeta[0:1:0][0:1:2][0:1:2]
          // Dataset {
          // Float32 zeta[ocean_time = 1][eta_rho = 3][xi_rho = 3];
          // } roms/al_roms/nep4_004.nc;
          // ---------------------------------------------
          // zeta[1][3][3]
          // [0][0], 0.6276099, 0.6276099, 0.6215845
          // [0][1], 0.6276099, 0.6276089, 0.6215834
          // [0][2], 0.6242121, 0.62421095, 0.6181293

          "time, eta_rho, xi_rho, zeta\n"
              + "UTC, count, count, meter\n"
              + "2004-01-30T12:00:00Z, 0, 0, 0.6276099\n"
              + "2004-01-30T12:00:00Z, 0, 1, 0.6276099\n"
              + "2004-01-30T12:00:00Z, 0, 2, 0.6215845\n"
              + "2004-01-30T12:00:00Z, 1, 0, 0.6276099\n"
              + "2004-01-30T12:00:00Z, 1, 1, 0.6276089\n"
              + "2004-01-30T12:00:00Z, 1, 2, 0.6215834\n"
              + "2004-01-30T12:00:00Z, 2, 0, 0.6242121\n"
              + "2004-01-30T12:00:00Z, 2, 1, 0.62421095\n"
              + "2004-01-30T12:00:00Z, 2, 2, 0.6181293\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);

      // analogous query
      userDapQuery = "zeta[0][(0):(2)][(0):(2)]";
      tName =
          eddGrid.makeNewFileForDapQuery(
              language,
              null,
              null,
              userDapQuery,
              EDStatic.fullTestCacheDirectory,
              eddGrid.className() + "_NAV2",
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      Test.ensureEqual(results, expected, "\nresults=\n" + results);

    } catch (Throwable t) {
      Test.knownProblem("!!!!!!  This test dataset has disappeared.", t);
    }
  }

  /** This tests a climatology time problem. */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest
  void testClimatologyTime() throws Throwable {
    // String2.log("\n*** EDDGridFromDap.testClimatologyTime");
    // testVerboseOn();
    String name, tName, results, tResults, expected, userDapQuery;
    int language = 0;

    try {
      EDDGrid eddGrid =
          (EDDGrid) EDDGridFromDap.oneFromDatasetsXml(null, "ncdcOwClm9505"); // 2020-08-20
      // this
      // now
      // often
      // fails
      // with
      // Http 502 Bad Gateway error after a delay
      userDapQuery = "u[(0000-12-28)][][(22)][(225)]";
      tName =
          eddGrid.makeNewFileForDapQuery(
              language,
              null,
              null,
              userDapQuery,
              EDStatic.fullTestCacheDirectory,
              eddGrid.className() + "_clim",
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      expected =
          "time,altitude,latitude,longitude,u\n"
              + "UTC,m,degrees_north,degrees_east,m s-1\n"
              +
              // 2020-05-28 data is all NaNs. I emailed Charles Carleton.
              "0000-12-13T00:00:00Z,10.0,22.0,225.0,-6.749089\n"; // 2018-05-17 was 12-15,
      // 2018-01-25 was 12-13?!
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
    } catch (Exception e) {
      Test.knownProblem(
          "2020-08-20 this now often fails with Http 502 Bad Gateway error after a delay.", e);
    }
  }

  /** This tests accessibleTo. */
  @org.junit.jupiter.api.Test
  @TagThredds
  void testAccessibleTo() throws Throwable {
    // testVerboseOn();
    EDDGrid eddGrid = (EDDGrid) EDDTestDataset.geterdBAssta5day();
    String roleNull[] = null;
    String roleNone[] = new String[0];
    String roleBob[] = new String[] {"bob"};
    String roleBMT[] = new String[] {"bob", "mike", "tom"};
    int language = 0;

    // test accessible = null
    eddGrid.setAccessibleTo(null);
    Test.ensureTrue(eddGrid.isAccessibleTo(roleNull), "");
    Test.ensureTrue(eddGrid.isAccessibleTo(roleNone), "");
    Test.ensureTrue(eddGrid.isAccessibleTo(roleBob), "");
    Test.ensureTrue(eddGrid.isAccessibleTo(roleBMT), "");

    // test accessible = ""
    eddGrid.setAccessibleTo("");
    Test.ensureTrue(!eddGrid.isAccessibleTo(roleNull), "");
    Test.ensureTrue(!eddGrid.isAccessibleTo(roleNone), "");
    Test.ensureTrue(!eddGrid.isAccessibleTo(roleBob), "");
    Test.ensureTrue(!eddGrid.isAccessibleTo(roleBMT), "");

    // test accessible = "john, tom"
    eddGrid.setAccessibleTo("john, tom");
    Test.ensureTrue(!eddGrid.isAccessibleTo(roleNull), "");
    Test.ensureTrue(!eddGrid.isAccessibleTo(roleNone), "");
    Test.ensureTrue(!eddGrid.isAccessibleTo(roleBob), "");
    Test.ensureTrue(eddGrid.isAccessibleTo(roleBMT), "");

    // test accessible = "albert, stan"
    eddGrid.setAccessibleTo("albert, stan");
    Test.ensureTrue(!eddGrid.isAccessibleTo(roleNull), "");
    Test.ensureTrue(!eddGrid.isAccessibleTo(roleNone), "");
    Test.ensureTrue(!eddGrid.isAccessibleTo(roleBob), "");
    Test.ensureTrue(!eddGrid.isAccessibleTo(roleBMT), "");
  }

  /**
   * The request size will be ~22MB/timePoint. If partialRequestMaxBytes=100000000 (10^8), <br>
   * Small request (e.g., 1,2,3,4) will be handled with one source query. <br>
   * Large request (e.g., 6+, depending on setup.xml partialRequestMaxBytes) <br>
   * will be handled with multiple source queries (one per timePoint).
   *
   * <p>Note that THREDDS has a default limit of 500MB for opendap responses.
   * https://www.unidata.ucar.edu/software/thredds/current/tds/reference/ThreddsConfigXMLFile.html#opendap
   * partialRequestMaxBytes=10^8 stays well under that.
   */
  @org.junit.jupiter.api.Test
  @ParameterizedTest
  @ValueSource(ints = {2, 4, 6, 96})
  @TagThredds
  void testBigRequest(int nTimePoints) throws Throwable {
    // testVerboseOn();
    String2.log(
        "\n*** EDDGridFromDap.testbigRequest  partialRequestMaxBytes="
            + EDStatic.partialRequestMaxBytes
            + "\n nTimePoints="
            + nTimePoints
            + " estimated nPartialRequests="
            + Math2.hiDiv(nTimePoints * 22000000, EDStatic.partialRequestMaxBytes));
    int language = 0;
    EDDGrid eddGrid = (EDDGrid) EDDTestDataset.geterdBAssta5day();
    String dir = EDStatic.fullTestCacheDirectory;
    String tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            "sst[0:" + (nTimePoints - 1) + "][][][]",
            dir,
            eddGrid.className() + "_testBigRequest",
            ".nc");
    String2.log("done. size=" + File2.length(dir + tName));

    // for each time point, test that values are same from erddap tiny request or
    // .nc file
    NetcdfFile ncFile = NcHelper.openFile(dir + tName);
    try {
      Variable ncVariable = ncFile.findVariable("sst");
      if (ncVariable == null) throw new RuntimeException("sst not found in " + dir + tName);
      int xIndex = 600, yIndex = 650, zIndex = 0;

      for (int tp = 0; tp < nTimePoints; tp++) {
        String2.log("\ntp=" + tp);

        // from new nc file:
        PrimitiveArray pa =
            NcHelper.get4DValues(ncVariable, xIndex, yIndex, zIndex, tp, tp); // x,y,z,t1,t2
        String2.log("\nfrom ncFile, sst=" + pa.getFloat(0));

        // from opendap:
        String testName =
            eddGrid.makeNewFileForDapQuery(
                language,
                null,
                null,
                "sst[" + tp + ":" + tp + "][" + zIndex + "][" + yIndex + "][" + xIndex + "]",
                dir,
                eddGrid.className() + "_testBigRequestDap",
                ".csv");
        String dapResult[] = File2.readFromFile88591(dir + testName);
        Test.ensureEqual(dapResult[0], "", "Error reading " + dir + testName);
        String2.log("\nfrom opendap:\n" + dapResult[1]);

        // compare
        int cpo = dapResult[1].lastIndexOf(",");
        String ncTest = pa.getFloat(0) == -9999999 ? "NaN" : "" + pa.getFloat(0);
        String dapTest = dapResult[1].substring(cpo + 1, dapResult[1].length() - 1);
        String msg = "tp=" + tp + "\n   ncTest=" + ncTest + "\n  dapTest=" + dapTest;
        String2.log(msg);
        Test.ensureEqual(ncTest, dapTest, "sst values don't match!\n" + msg);
      }
    } finally {
      try {
        if (ncFile != null) ncFile.close();
      } catch (Exception e9) {
      }
    }
  }

  /**
   * The request size will be ~22MB/timePoint. If partialRequestMaxBytes=100000000 (10^8), <br>
   * Small request (e.g., 1,2,3,4) will be handled with one source query. <br>
   * Large request (e.g., 6+, depending on setup.xml partialRequestMaxBytes) <br>
   * will be handled with multiple source queries (one per timePoint).
   *
   * <p>Note that THREDDS has a default limit of 500MB for opendap responses.
   * https://www.unidata.ucar.edu/software/thredds/current/tds/reference/ThreddsConfigXMLFile.html#opendap
   * partialRequestMaxBytes=10^8 stays well under that.
   */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest
  void testBigRequestSpeed(int nTimePoints, String fileType, int expectedMs) throws Throwable {
    // testVerboseOn();
    int language = 0;
    String msg =
        "\n*** EDDGridFromDap.testbigRequest  partialRequestMaxBytes="
            + EDStatic.partialRequestMaxBytes
            + "\n nTimePoints="
            + nTimePoints
            + " estimated nPartialRequests="
            + Math2.hiDiv(nTimePoints * 22000000, EDStatic.partialRequestMaxBytes);
    String2.log(msg);
    EDDGrid eddGrid = (EDDGrid) EDDGridFromDap.oneFromDatasetsXml(null, "nceiPH53sstd1day");
    String dir = EDStatic.fullTestCacheDirectory;
    String tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            "sst[0:" + (nTimePoints - 1) + "][][][]",
            dir,
            eddGrid.className() + "_testBigRequest",
            fileType);

    // time the second request
    long time = System.currentTimeMillis();
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            "sst[0:" + (nTimePoints - 1) + "][][][]",
            dir,
            eddGrid.className() + "_testBigRequest2",
            fileType);
    msg +=
        "\n*** Not necessarily a problem, just logging the results:\n"
            + "fileType="
            + fileType
            + " finished. size="
            + File2.length(dir + tName)
            + " time="
            + (System.currentTimeMillis() - time)
            + "ms expected="
            + expectedMs
            + "ms";
    throw new RuntimeException(msg);
  }

  /**
   * Test speed of Data Access Form. Sometimes: use this with profiler:
   * -agentlib:hprof=cpu=samples,depth=20,file=/JavaHeap.txt
   */
  @org.junit.jupiter.api.Test
  @TagThredds
  void testSpeedDAF() throws Throwable {
    // setup and warmup
    // EDD.testVerbose(false);
    int language = 0;
    EDDGridFromDap gridDataset = (EDDGridFromDap) EDDTestDataset.geterdMHchla8day();
    String fileName = EDStatic.fullTestCacheDirectory + "gridTestSpeedDAF.txt";
    Writer writer = File2.getBufferedFileWriterUtf8(fileName);
    gridDataset.writeDapHtmlForm(language, null, "", writer);

    // time it DAF
    String2.log("start timing");
    long time = System.currentTimeMillis();
    for (int i = 0; i < 1000; i++) // 1000 so it dominates program run time
    gridDataset.writeDapHtmlForm(language, null, "", writer);
    String2.log(
        "EDDGridFromDap.testSpeedDAF time per .html = "
            + ((System.currentTimeMillis() - time) / 1000.0)
            + "ms (avg=6.14ms)\n"
            + "  outputFileName="
            + fileName);
    // EDD.testVerbose(true);
  }

  /**
   * Test speed of Make A Graph Form. Sometimes: use this with profiler:
   * -agentlib:hprof=cpu=samples,depth=20,file=/JavaHeap.txt
   */
  @org.junit.jupiter.api.Test
  @TagThredds
  void testSpeedMAG() throws Throwable {
    // setup and warmup
    // EDD.testVerbose(false);
    EDDGridFromDap gridDataset = (EDDGridFromDap) EDDTestDataset.geterdMHchla8day();
    String dir = EDStatic.fullTestCacheDirectory;
    String baseFileName = "gridTestSpeedMAG";
    int language = 0;

    // time it
    String2.log("start timing");
    long time2 = System.currentTimeMillis();
    int n = 1000; // 1000 so it dominates program run time
    for (int i = 0; i < n; i++) {
      String fileName = baseFileName + i + ".html";
      OutputStreamSource oss =
          new OutputStreamSourceSimple(
              new BufferedOutputStream(new FileOutputStream(dir + fileName)));
      gridDataset.respondToGraphQuery(
          0, null, null, "", "", "", oss, EDStatic.fullTestCacheDirectory, fileName, ".graph");
      Test.ensureTrue(File2.delete(dir + fileName), "");
    }
    time2 = System.currentTimeMillis() - time2;
    Test.ensureTrue(
        time2 > 27,
        "Slow! EDDGridFromDap.testSpeedMAG time per .graph = "
            + (time2 / (double) n)
            + "ms (avg=18ms)");

    // EDD.testVerbose(true);
  }

  /** Test quick restart */
  @org.junit.jupiter.api.Test
  @TagThredds
  void testQuickRestart() throws Throwable {
    // String2.log("\nEDDGridFromDap.testQuickRestart");
    String tDatasetID = "erdBAssta5day";
    int language = 0;

    // regular load dataset
    File2.delete(EDDGridFromDap.quickRestartFullFileName(tDatasetID)); // force regular load
    long time1 = System.currentTimeMillis();
    EDD edd1 = EDDTestDataset.geterdBAssta5day();
    String searchString1 = String2.utf8BytesToString(edd1.searchBytes());
    time1 = System.currentTimeMillis() - time1;

    // try to load from quickRestartFile
    EDStatic.majorLoadDatasetsTimeSeriesSB.setLength(
        0); // so EDStatic.initialLoadDatasets() will be true
    Test.ensureTrue(EDStatic.initialLoadDatasets(), "");
    long time2 = System.currentTimeMillis();
    EDD edd2 = EDDTestDataset.geterdBAssta5day();
    String searchString2 = String2.utf8BytesToString(edd2.searchBytes());
    time2 = System.currentTimeMillis() - time2;

    String2.log(
        "  regular load dataset       time="
            + time1
            + "ms\n"
            + "  quick restart load dataset time="
            + time2
            + "ms");
    Test.ensureEqual(searchString1, searchString2, "");

    // but add something to it so again EDStatic.initialLoadDatasets() will be false
    EDStatic.majorLoadDatasetsTimeSeriesSB.append("\n");
  }

  /**
   * Test getting a geotiff from a dataset with descending lat values. Sometimes: use this with
   * profiler: -agentlib:hprof=cpu=samples,depth=20,file=/JavaHeap.txt
   */
  @org.junit.jupiter.api.Test
  @TagThredds
  // was an interactive test, likely needs additional changes
  void testDescendingAxisGeotif() throws Throwable {
    // String2.log("\n*** EDDGridFromDap.testDescendingAxisGeotif");
    // EDD.testVerbose(false);
    EDDGrid gridDataset;
    int language = 0;
    String tName;
    /* */

    // descending Lat axis
    gridDataset = (EDDGrid) EDDTestDataset.getusgsCeCrm10();
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "topo[0:20:last][0:20:last]&.draw=surface&.vars=longitude|latitude|topo",
            EDStatic.fullTestCacheDirectory,
            "descendingAxisGeotif",
            ".geotif");
    Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

    // NOT FINISHED ADDING FEATURE
    // descending Lat axis AND &.size=width|height
    // gridDataset = (EDDGridFromDap)oneFromDatasetsXml(null, "usgsCeCrm10");
    // tName = gridDataset.makeNewFileForDapQuery(language, null, null,
    // "topo[(23):(19)][(-161):(-155)]&.draw=surface&.vars=longitude|latitude|topo&.size=200|300",
    // EDStatic.fullTestCacheDirectory, "descendingAxisGeotifSize", ".geotif");
    // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

    // Mercator Lat axis
    // 2013-10-21 this still fails. Bizarre error is
    // Exception in thread "main" java.lang.IllegalArgumentException: Must have 1D x
    // and y axes for heatFlux
    // at ucar.nc2.geotiff.GeotiffWriter.writeGrid(GeotiffWriter.java:80)
    // at gov.noaa.pfel.erddap.dataset.EDDGrid.saveAsGeotiff(EDDGrid.java:4497)
    // at gov.noaa.pfel.erddap.dataset.EDDGrid.respondToDapQuery(EDDGrid.java:2242)
    // at gov.noaa.pfel.erddap.dataset.EDD.lowMakeFileForDapQuery(EDD.java:2511)
    // at gov.noaa.pfel.erddap.dataset.EDD.makeNewFileForDapQuery(EDD.java:2430)
    // at
    // gov.noaa.pfel.erddap.dataset.EDDGridFromDap.testDescendingAxisGeotif(EDDGridFromDap.java:7846)
    // at gov.noaa.pfel.coastwatch.TestAll.main(TestAll.java:180)

    // test lon can't be below and above 180
    String error = "shoudn't happen";
    try {
      gridDataset = (EDDGrid) EDDGridFromDap.oneFromDatasetsXml(null, "jplAmsreSstMon");
      tName =
          gridDataset.makeNewFileForDapQuery(
              language,
              null,
              null,
              "tos[(2010-12-16T12)][(-89.5):(89.5)][(0.5):(359.5)]"
                  + "&.draw=surface&.vars=longitude|latitude|tos",
              EDStatic.fullTestCacheDirectory,
              "LonBelowAbove180",
              ".geotif");
      Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    } catch (Throwable t) {
      error = t.toString();
    }

    try {
      String expected =
          "com.cohort.util.SimpleException: "
              + "Query error: For .geotif requests, the longitude values can't be "
              + "below and above 180.";
      Test.ensureEqual(error, expected, "Unexpected error:\n" + error);
    } catch (Throwable t) {
      String2.pressEnterToContinue(MustBe.throwableToString(t));
    }

    // test unevenly spaced lat not allowed (wierd! regular 29.5 to 81.5 but
    // irregular (mercator?) in middle)
    // (2014-08-07 I verified it is still a requirement by removing check in
    // EDDGrid.saveAsGeotiff,
    // although error is odd: java.lang.IllegalArgumentException: Must have 1D x and
    // y axes for tos)
    error = "shouldn't happen";
    try {
      gridDataset = (EDDGrid) EDDGridFromDap.oneFromDatasetsXml(null, "gfdlCM2120C3M5OS");
      tName =
          gridDataset.makeNewFileForDapQuery(
              language,
              null,
              null,
              "tos[(2000-11-12T12)][(-81.5):(89.5)][(0.5):(179.5)]"
                  + "&.draw=surface&.vars=longitude|latitude|tos",
              EDStatic.fullTestCacheDirectory,
              "MercatorAxisGeotif",
              ".geotif");
      Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    } catch (Throwable t) {
      error = t.toString();
    }

    try {
      String expected =
          "com.cohort.util.SimpleException: "
              + ".geotif is never available for this dataset because the dataset's "
              + "longitude and/or latitude values aren't evenly spaced.";
      Test.ensureEqual(error, expected, "Unexpected error:\n" + error);
    } catch (Throwable t) {
      String2.pressEnterToContinue(
          MustBe.throwableToString(t) + "And now, source dataset is gone.");
    }
  }

  /** This tests saveAsNcml. */
  @org.junit.jupiter.api.Test
  @TagThredds
  void testNcml() throws Throwable {
    // testVerboseOn();
    int language = 0;

    EDDGridFromDap gridDataset = (EDDGridFromDap) EDDTestDataset.geterdBAssta5day();
    String name, tName, results, expected;

    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_testNcml",
            ".ncml");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>\n"
            + "<netcdf xmlns=\"https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2\" location=\"http://127.0.0.1:8080/griddap/erdBAssta5day\">\n"
            + "  <attribute name=\"acknowledgement\" value=\"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\" />\n"
            + "  <attribute name=\"cdm_data_type\" value=\"Grid\" />\n"
            + "  <attribute name=\"composite\" value=\"true\" />\n"
            + "  <attribute name=\"contributor_name\" value=\"Remote Sensing Systems Inc, JAXA, NASA, OSDPD, CoastWatch\" />\n"
            + "  <attribute name=\"contributor_role\" value=\"Source of level 2 data.\" />\n"
            + "  <attribute name=\"Conventions\" value=\"COARDS, CF-1.6, ACDD-1.3\" />\n"
            + "  <attribute name=\"creator_email\" value=\"erd.data@noaa.gov\" />\n"
            + "  <attribute name=\"creator_name\" value=\"NOAA NMFS SWFSC ERD\" />\n"
            + "  <attribute name=\"creator_type\" value=\"institution\" />\n"
            + "  <attribute name=\"creator_url\" value=\"https://www.pfeg.noaa.gov\" />\n"
            + "  <attribute name=\"date_created\" value=\"2014-03-08\" />\n"
            + "  <attribute name=\"date_issued\" value=\"2014-03-08\" />\n"
            + "  <attribute name=\"Easternmost_Easting\" type=\"double\" value=\"360.0\" />\n"
            + "  <attribute name=\"geospatial_lat_max\" type=\"double\" value=\"75.0\" />\n"
            + "  <attribute name=\"geospatial_lat_min\" type=\"double\" value=\"-75.0\" />\n"
            + "  <attribute name=\"geospatial_lat_resolution\" type=\"double\" value=\"0.1\" />\n"
            + "  <attribute name=\"geospatial_lat_units\" value=\"degrees_north\" />\n"
            + "  <attribute name=\"geospatial_lon_max\" type=\"double\" value=\"360.0\" />\n"
            + "  <attribute name=\"geospatial_lon_min\" type=\"double\" value=\"0.0\" />\n"
            + "  <attribute name=\"geospatial_lon_resolution\" type=\"double\" value=\"0.1\" />\n"
            + "  <attribute name=\"geospatial_lon_units\" value=\"degrees_east\" />\n"
            + "  <attribute name=\"geospatial_vertical_max\" type=\"double\" value=\"0.0\" />\n"
            + "  <attribute name=\"geospatial_vertical_min\" type=\"double\" value=\"0.0\" />\n"
            + "  <attribute name=\"geospatial_vertical_positive\" value=\"up\" />\n"
            + "  <attribute name=\"geospatial_vertical_units\" value=\"m\" />\n"
            + "  <attribute name=\"history\" value=\"Remote Sensing Systems Inc, JAXA, NASA, OSDPD, CoastWatch\n"
            + "2014-03-08T23:30:04Z NOAA CoastWatch \\(West Coast Node\\) and NOAA SFSC ERD\" />\n"
            + "  <attribute name=\"infoUrl\" value=\"https://coastwatch.pfeg.noaa.gov/infog/BA_ssta_las.html\" />\n"
            + "  <attribute name=\"institution\" value=\"NOAA NMFS SWFSC ERD\" />\n"
            + "  <attribute name=\"keywords\" value=\"5-day, blended, coastwatch, day, degrees, Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature, experimental, global, noaa, ocean, oceans, sea, sea_surface_temperature, sst, surface, temperature, wcn\" />\n"
            + "  <attribute name=\"keywords_vocabulary\" value=\"GCMD Science Keywords\" />\n"
            + "  <attribute name=\"license\" value=\"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\" />\n"
            + "  <attribute name=\"naming_authority\" value=\"gov.noaa.pfeg.coastwatch\" />\n"
            + "  <attribute name=\"Northernmost_Northing\" type=\"double\" value=\"75.0\" />\n"
            + "  <attribute name=\"origin\" value=\"Remote Sensing Systems Inc, JAXA, NASA, OSDPD, CoastWatch\" />\n"
            + "  <attribute name=\"processing_level\" value=\"3\" />\n"
            + "  <attribute name=\"project\" value=\"CoastWatch \\(https://coastwatch.noaa.gov/\\)\" />\n"
            + "  <attribute name=\"projection\" value=\"geographic\" />\n"
            + "  <attribute name=\"projection_type\" value=\"mapped\" />\n"
            + "  <attribute name=\"publisher_email\" value=\"erd.data@noaa.gov\" />\n"
            + "  <attribute name=\"publisher_name\" value=\"NOAA NMFS SWFSC ERD\" />\n"
            + "  <attribute name=\"publisher_type\" value=\"institution\" />\n"
            + "  <attribute name=\"publisher_url\" value=\"https://www.pfeg.noaa.gov\" />\n"
            + "  <attribute name=\"references\" value=\"Blended SST from satellites information: This is an experimental product which blends satellite-"
            + "derived SST data from multiple platforms using a weighted mean.  Weights are based on the inverse square of the nominal accuracy of ea"
            + "ch satellite. AMSR_E Processing information: https://www.eorc.jaxa.jp/en/distribution/standard_dataset/pdf/amsr-e_handbook_e.pdf . "
            + "AMSR-E Processing reference: Wentz, "
            + "F.J., C. Gentemann, and P. Ashcroft. 2005. ON-ORBIT CALIBRATION OF AMSR-E AND THE RETRIEVAL OF OCEAN PRODUCTS. Remote Sensing Systems "
            + "Internal Report. AVHRR Processing Information: http://www.osdpd.noaa.gov/PSB/EPS/CW/coastwatch.html .  AVHRR Processing Reference: Wa"
            + "lton C. C., W. G. Pichel, J. F. Sapper, D. A. May. The development and operational application of nonlinear algorithms for the measure"
            + "ment of sea surface temperatures with the NOAA polar-orbiting environmental satellites. J.G.R., 103: \\(C12\\) 27999-28012, 1998. Cloudmas"
            + "k reference: Stowe, L. L., P. A. Davis, and E. P. McClain.  Scientific basis and initial evaluation of the CLAVR-1 global clear/cloud "
            + "classification algorithm for the advanced very high resolution radiometer. J. Atmos. Oceanic Technol., 16, 656-681. 1999. Calibration "
            + "and Validation: Li, X., W. Pichel, E. Maturi, P. Clemente-Colon, and J. Sapper. Deriving the operational nonlinear multi-channel sea s"
            + "urface temperature algorithm coefficients for NOAA-15 AVHRR/3. International Journal of Remote Sensing, Volume 22, No. 4, 699 - 704, M"
            + "arch 2001a. Calibration and Validation: Li, X, W. Pichel, P. Clemente-Colon, V. Krasnopolsky, and J. Sapper. Validation of coastal sea "
            + "and lake surface temperature measurements derived from NOAA/AVHRR Data. International Journal of Remote Sensing, Vol. 22, No. 7, 1285"
            + "-1303, 2001b. GOES Imager Processing Information: https://coastwatch.noaa.gov/goes_sst_overview.html .  GOES Imager Processing Referenc"
            + "e: Wu, X., W. P. Menzel, and G. S. Wade, 1999. Estimation of sea surface temperatures using GOES-8/9 radiance measurements, Bull. Amer"
            + ". Meteor. Soc., 80, 1127-1138.  MODIS Aqua Processing Information: https://oceancolor.gsfc.nasa.gov/DOCS/modis_sst/ . MODIS Aqua Proces"
            + "sing reference: Not Available.\" />\n"
            + "  <attribute name=\"satellite\" value=\"Aqua, GOES, POES\" />\n"
            + "  <attribute name=\"sensor\" value=\"AMSR-E, MODIS, Imager, AVHRR\" />\n"
            + "  <attribute name=\"source\" value=\"satellite observation: Aqua, GOES, POES, AMSR-E, MODIS, Imager, AVHRR\" />\n"
            + "  <attribute name=\"sourceUrl\" value=\"https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/BA/ssta/5day\" />\n"
            + "  <attribute name=\"Southernmost_Northing\" type=\"double\" value=\"-75.0\" />\n"
            + "  <attribute name=\"standard_name_vocabulary\" value=\"CF Standard Name Table v70\" />\n"
            + "  <attribute name=\"summary\" value=\"NOAA OceanWatch provides a blended sea surface temperature \\(SST\\) products derived from both microwa"
            + "ve and infrared sensors carried on multiple platforms.  The microwave instruments can measure ocean temperatures even in the presence "
            + "of clouds, though the resolution is a bit coarse when considering features typical of the coastal environment.  These are complemented "
            + "by the relatively fine measurements of infrared sensors.  The blended data are provided at moderate spatial resolution \\(0.1 degrees\\) "
            + "for the Global Ocean.  Measurements are gathered by Japan&#39;s Advanced Microwave Scanning Radiometer \\(AMSR-E\\) instrument, a passive "
            + "radiance sensor carried aboard NASA&#39;s Aqua spacecraft, NOAA&#39;s Advanced Very High Resolution Radiometer, NOAA GOES Imager, and "
            + "NASA&#39;s Moderate Resolution Imaging Spectrometer \\(MODIS\\). THIS IS AN EXPERIMENTAL PRODUCT: intended strictly for scientific evaluat"
            + "ion by professional marine scientists.\" />\n"
            + "  <attribute name=\"time_coverage_end\" value=\"20.{8}T12:00:00Z\" />\n"
            + "  <attribute name=\"time_coverage_start\" value=\"2002-07-06T12:00:00Z\" />\n"
            + "  <attribute name=\"title\" value=\"SST, Blended, Global, 2002-2014, EXPERIMENTAL \\(5 Day Composite\\)\" />\n"
            + "  <attribute name=\"Westernmost_Easting\" type=\"double\" value=\"0.0\" />\n"
            + "  <dimension name=\"time\" length=\"3880\" />\n"
            + "  <dimension name=\"altitude\" length=\"1\" />\n"
            + "  <dimension name=\"latitude\" length=\"1501\" />\n"
            + "  <dimension name=\"longitude\" length=\"3601\" />\n"
            + "  <variable name=\"time\" shape=\"time\" type=\"double\">\n"
            + "    <attribute name=\"_CoordinateAxisType\" value=\"Time\" />\n"
            + "    <attribute name=\"actual_range\" type=\"double\" value=\"1.0259568E9 1.{5,11}\" />\n"
            + "    <attribute name=\"axis\" value=\"T\" />\n"
            + "    <attribute name=\"fraction_digits\" type=\"int\" value=\"0\" />\n"
            + "    <attribute name=\"ioos_category\" value=\"Time\" />\n"
            + "    <attribute name=\"long_name\" value=\"Centered Time\" />\n"
            + "    <attribute name=\"standard_name\" value=\"time\" />\n"
            + "    <attribute name=\"time_origin\" value=\"01-JAN-1970 00:00:00\" />\n"
            + "    <attribute name=\"units\" value=\"seconds since 1970-01-01T00:00:00Z\" />\n"
            + "  </variable>\n"
            + "  <variable name=\"altitude\" shape=\"altitude\" type=\"double\">\n"
            + "    <attribute name=\"_CoordinateAxisType\" value=\"Height\" />\n"
            + "    <attribute name=\"_CoordinateZisPositive\" value=\"up\" />\n"
            + "    <attribute name=\"actual_range\" type=\"double\" value=\"0.0 0.0\" />\n"
            + "    <attribute name=\"axis\" value=\"Z\" />\n"
            + "    <attribute name=\"fraction_digits\" type=\"int\" value=\"0\" />\n"
            + "    <attribute name=\"ioos_category\" value=\"Location\" />\n"
            + "    <attribute name=\"long_name\" value=\"Altitude\" />\n"
            + "    <attribute name=\"positive\" value=\"up\" />\n"
            + "    <attribute name=\"standard_name\" value=\"altitude\" />\n"
            + "    <attribute name=\"units\" value=\"m\" />\n"
            + "  </variable>\n"
            + "  <variable name=\"latitude\" shape=\"latitude\" type=\"double\">\n"
            + "    <attribute name=\"_CoordinateAxisType\" value=\"Lat\" />\n"
            + "    <attribute name=\"actual_range\" type=\"double\" value=\"-75.0 75.0\" />\n"
            + "    <attribute name=\"axis\" value=\"Y\" />\n"
            + "    <attribute name=\"coordsys\" value=\"geographic\" />\n"
            + "    <attribute name=\"fraction_digits\" type=\"int\" value=\"1\" />\n"
            + "    <attribute name=\"ioos_category\" value=\"Location\" />\n"
            + "    <attribute name=\"long_name\" value=\"Latitude\" />\n"
            + "    <attribute name=\"point_spacing\" value=\"even\" />\n"
            + "    <attribute name=\"standard_name\" value=\"latitude\" />\n"
            + "    <attribute name=\"units\" value=\"degrees_north\" />\n"
            + "  </variable>\n"
            + "  <variable name=\"longitude\" shape=\"longitude\" type=\"double\">\n"
            + "    <attribute name=\"_CoordinateAxisType\" value=\"Lon\" />\n"
            + "    <attribute name=\"actual_range\" type=\"double\" value=\"0.0 360.0\" />\n"
            + "    <attribute name=\"axis\" value=\"X\" />\n"
            + "    <attribute name=\"coordsys\" value=\"geographic\" />\n"
            + "    <attribute name=\"fraction_digits\" type=\"int\" value=\"1\" />\n"
            + "    <attribute name=\"ioos_category\" value=\"Location\" />\n"
            + "    <attribute name=\"long_name\" value=\"Longitude\" />\n"
            + "    <attribute name=\"point_spacing\" value=\"even\" />\n"
            + "    <attribute name=\"standard_name\" value=\"longitude\" />\n"
            + "    <attribute name=\"units\" value=\"degrees_east\" />\n"
            + "  </variable>\n"
            + "  <variable name=\"sst\" shape=\"time altitude latitude longitude\" type=\"float\">\n"
            + "    <attribute name=\"_FillValue\" type=\"float\" value=\"-9999999.0\" />\n"
            + "    <attribute name=\"colorBarMaximum\" type=\"double\" value=\"32.0\" />\n"
            + "    <attribute name=\"colorBarMinimum\" type=\"double\" value=\"0.0\" />\n"
            + "    <attribute name=\"coordsys\" value=\"geographic\" />\n"
            + "    <attribute name=\"fraction_digits\" type=\"int\" value=\"1\" />\n"
            + "    <attribute name=\"ioos_category\" value=\"Temperature\" />\n"
            + "    <attribute name=\"long_name\" value=\"Sea Surface Temperature\" />\n"
            + "    <attribute name=\"missing_value\" type=\"float\" value=\"-9999999.0\" />\n"
            + "    <attribute name=\"standard_name\" value=\"sea_surface_temperature\" />\n"
            + "    <attribute name=\"units\" value=\"degree_C\" />\n"
            + "  </variable>\n"
            + "</netcdf>\n";
    Test.ensureLinesMatch(results, expected, "RESULTS=\n" + results);
  }

  @org.junit.jupiter.api.Test
  @TagExternalERDDAP
  void testNetcdfJava() throws Throwable {
    // open as a NetcdfDataset, not a NetcdfFile as above
    String url = "https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdMHchla8day";
    // String url = EDStatic.erddapUrl + "/griddap/erdMHchla8day"; //in tests,
    // always non-https url
    int language = 0;

    NetcdfDataset nc = NetcdfDatasets.openDataset(url); // 2021: 's' is the new API
    String results, expected;
    Attributes attributes = new Attributes();
    try {
      results = nc.toString();
      results = NcHelper.decodeNcDump(results); // added with switch to netcdf-java 4.0
      String tUrl = String2.replaceAll(EDStatic.erddapUrl, "http:", "dods:"); // in tests, always
      // non-https url
      expected =
          "netcdf "
              + File2.getNameNoExtension(url)
              + " \\{\n"
              + "  dimensions:\n"
              + "    time = \\d{3};\n"
              + // was 500 It changes (here and below...)
              "    altitude = 1;\n"
              + "    latitude = 4320;\n"
              + "    longitude = 8640;\n"
              + "  variables:\n"
              + "    float chlorophyll\\(time=\\d{3}, altitude=1, latitude=4320, longitude=8640\\);\n"
              + "      :_CoordinateAxes = \"time altitude latitude longitude \";\n"
              + "      :_FillValue = -9999999.0f; // float\n"
              + "      :colorBarMaximum = 30.0; // double\n"
              + "      :colorBarMinimum = 0.03; // double\n"
              + "      :colorBarScale = \"Log\";\n"
              + "      :coordsys = \"geographic\";\n"
              + "      :fraction_digits = 2; // int\n"
              + "      :ioos_category = \"Ocean Color\";\n"
              + "      :long_name = \"Concentration Of Chlorophyll In Sea Water\";\n"
              + "      :missing_value = -9999999.0f; // float\n"
              + "      :standard_name = \"concentration_of_chlorophyll_in_sea_water\";\n"
              + "      :units = \"mg m-3\";\n"
              + "\n"
              + "    double time\\(time=\\d{3}\\);\n"
              + "      :_CoordinateAxisType = \"Time\";\n"
              + "      :actual_range = 1.0260864E9, .{5,15}; // double\n"
              + // 2nd number
              // changes
              "      :axis = \"T\";\n"
              + "      :fraction_digits = 0; // int\n"
              + "      :ioos_category = \"Time\";\n"
              + "      :long_name = \"Centered Time\";\n"
              + "      :standard_name = \"time\";\n"
              + "      :time_origin = \"01-JAN-1970 00:00:00\";\n"
              + "      :units = \"seconds since 1970-01-01T00:00:00Z\";\n"
              + "      :calendar = \"gregorian\";\n"
              + "\n"
              + "    double altitude\\(altitude=1\\);\n"
              + "      :_CoordinateAxisType = \"Height\";\n"
              + "      :_CoordinateZisPositive = \"up\";\n"
              + "      :actual_range = 0.0, 0.0; // double\n"
              + "      :axis = \"Z\";\n"
              + "      :fraction_digits = 0; // int\n"
              + "      :ioos_category = \"Location\";\n"
              + "      :long_name = \"Altitude\";\n"
              + "      :positive = \"up\";\n"
              + "      :standard_name = \"altitude\";\n"
              + "      :units = \"m\";\n"
              + "\n"
              + "    double latitude\\(latitude=4320\\);\n"
              + "      :_CoordinateAxisType = \"Lat\";\n"
              + "      :actual_range = -90.0, 90.0; // double\n"
              + "      :axis = \"Y\";\n"
              + "      :coordsys = \"geographic\";\n"
              + "      :fraction_digits = 4; // int\n"
              + "      :ioos_category = \"Location\";\n"
              + "      :long_name = \"Latitude\";\n"
              + "      :point_spacing = \"even\";\n"
              + "      :standard_name = \"latitude\";\n"
              + "      :units = \"degrees_north\";\n"
              + "\n"
              + "    double longitude\\(longitude=8640\\);\n"
              + "      :_CoordinateAxisType = \"Lon\";\n"
              + "      :actual_range = 0.0, 360.0; // double\n"
              + "      :axis = \"X\";\n"
              + "      :coordsys = \"geographic\";\n"
              + "      :fraction_digits = 4; // int\n"
              + "      :ioos_category = \"Location\";\n"
              + "      :long_name = \"Longitude\";\n"
              + "      :point_spacing = \"even\";\n"
              + "      :standard_name = \"longitude\";\n"
              + "      :units = \"degrees_east\";\n"
              + "\n"
              + "  // global attributes:\n"
              + "  :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n"
              + "  :cdm_data_type = \"Grid\";\n"
              + "  :composite = \"true\";\n"
              + "  :contributor_name = \"NASA GSFC \\(OBPG\\)\";\n"
              + "  :contributor_role = \"Source of level 2 data.\";\n"
              + "  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n"
              + "  :creator_email = \"erd.data@noaa.gov\";\n"
              + "  :creator_name = \"NOAA NMFS SWFSC ERD\";\n"
              + "  :creator_type = \"institution\";\n"
              + "  :creator_url = \"https://www.pfeg.noaa.gov\";\n"
              + "  :date_created = \"20.{8}\";\n"
              + // changes
              "  :date_issued = \"20.{8}\";\n"
              + // changes
              "  :Easternmost_Easting = 360.0; // double\n"
              + "  :geospatial_lat_max = 90.0; // double\n"
              + "  :geospatial_lat_min = -90.0; // double\n"
              + "  :geospatial_lat_resolution = 0.041676313961565174; // double\n"
              + "  :geospatial_lat_units = \"degrees_north\";\n"
              + "  :geospatial_lon_max = 360.0; // double\n"
              + "  :geospatial_lon_min = 0.0; // double\n"
              + "  :geospatial_lon_resolution = 0.04167148975575877; // double\n"
              + "  :geospatial_lon_units = \"degrees_east\";\n"
              + "  :geospatial_vertical_max = 0.0; // double\n"
              + "  :geospatial_vertical_min = 0.0; // double\n"
              + "  :geospatial_vertical_positive = \"up\";\n"
              + "  :geospatial_vertical_units = \"m\";\n"
              + "  :history = \"NASA GSFC \\(OBPG\\)";
      int po = results.indexOf(":history = \"NASA GSFC (OBPG)");
      Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
      Test.ensureLinesMatch(results.substring(0, po + 28), expected, "RESULTS=\n" + results);

      expected =
          "  :satellite = \"Aqua\";\n"
              + "  :sensor = \"MODIS\";\n"
              + "  :source = \"satellite observation: Aqua, MODIS\";\n"
              + "  :sourceUrl = \"https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day\";\n"
              + "  :Southernmost_Northing = -90.0; // double\n"
              + "  :standard_name_vocabulary = \"CF Standard Name Table v70\";\n"
              + "  :summary = \"NOAA CoastWatch distributes chlorophyll-a concentration data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer \\(MODIS\\) carried aboard the spacecraft.   This is Science Quality data.\";\n"
              + "  :time_coverage_end = \"20.{8}T00:00:00Z\";\n"
              + // changes
              "  :time_coverage_start = \"20.{8}T00:00:00Z\";\n"
              + "  :title = \"Chlorophyll-a, Aqua MODIS, NPP, 2002-2013, DEPRECATED OLDER VERSION \\(8 Day Composite\\)\";\n"
              + "  :Westernmost_Easting = 0.0; // double\n"
              + "  :_CoordSysBuilder = \"ucar.nc2.internal.dataset.conv.CF1Convention\";\n"
              +
              // " :_CoordSysBuilder = \"ucar.nc2.dataset.conv.CF1Convention\";\n" +
              // //2021-01-07 this is pre netcdf v5.4.1
              "\\}\n";
      Test.ensureLinesMatch(
          results.substring(results.indexOf("  :satellite =")), expected, "RESULTS=\n" + results);

      attributes.clear();
      NcHelper.getGroupAttributes(nc.getRootGroup(), attributes);
      Test.ensureEqual(attributes.getString("contributor_name"), "NASA GSFC (OBPG)", "");
      Test.ensureEqual(
          attributes.getString("keywords"),
          "8-day, aqua, chemistry, chlorophyll, chlorophyll-a, coastwatch, color, concentration, concentration_of_chlorophyll_in_sea_water, day, degrees, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, global, modis, noaa, npp, ocean, ocean color, oceans, quality, science, science quality, sea, seawater, water, wcn",
          "found=" + attributes.getString("keywords"));

      // get attributes for a dimension
      Variable ncLat = nc.findVariable("latitude");
      attributes.clear();
      NcHelper.getVariableAttributes(ncLat, attributes);
      Test.ensureEqual(attributes.getString("coordsys"), "geographic", "");
      Test.ensureEqual(attributes.get("fraction_digits"), new IntArray(new int[] {4}), ""); // test
      // if
      // stored
      // in
      // correct
      // form

      // get attributes for grid variable
      Variable ncChl = nc.findVariable("chlorophyll");
      attributes.clear();
      NcHelper.getVariableAttributes(ncChl, attributes);
      Test.ensureEqual(
          attributes.getString("standard_name"), "concentration_of_chlorophyll_in_sea_water", "");
      Test.ensureEqual(attributes.getString("units"), "mg m-3", "");

      // test get dimension data - all
      PrimitiveArray pa = NcHelper.getPrimitiveArray(ncLat);
      Test.ensureEqual(pa.elementType(), PAType.DOUBLE, "");
      Test.ensureEqual(pa.size(), 4320, "");
      Test.ensureEqual(pa.getDouble(0), -90, "");
      Test.ensureEqual(pa.getDouble(4319), 90, "");

      // test get dimension data - part
      pa = NcHelper.getPrimitiveArray(ncLat, 10, 20);
      Test.ensureEqual(pa.elementType(), PAType.DOUBLE, "");
      Test.ensureEqual(pa.size(), 11, "");
      Test.ensureEqual(pa.getDouble(0), -89.58323686038435, "");
      Test.ensureEqual(pa.getDouble(10), -89.16647372076869, "");

      // get grid data
      pa = NcHelper.get4DValues(ncChl, 4500, 2080, 0, 170, 190); // x,y,z,t1,t2
      Test.ensureEqual(pa.elementType(), PAType.FLOAT, "");
      String2.log("pa=" + pa);
      Test.ensureEqual(pa.size(), 21, "");
      // pre 2010-10-26 was 0.113f
      // pre 2012-08-17 was 0.12906f
      Test.ensureEqual(pa.getFloat(0), 0.13295f, "");
      Test.ensureEqual(pa.getFloat(1), Float.NaN, ""); // !!! NetcdfFile returns -9999999.0f

    } finally {
      nc.close();
    }

    /*
     * //open as a NetcdfDataset, not a NetcdfFile as above
     * nc = NetcdfDatasets.openDataset(
     * "http://beach.mbari.org:8180/erddap/griddap/erdRyanSST"); //in tests, always
     * non-https url
     * String results, expected;
     * Attributes attributes = new Attributes();
     * try {
     * results = nc.toString();
     * results = NcHelper.decodeNcDump(results); //added with switch to netcdf-java
     * 4.0
     * String tUrl = String2.replaceAll(EDStatic.erddapUrl, "http:", "dods:"); //in
     * tests, always non-https url
     * expected =
     * "zztop\n";
     * Test.ensureEqual(results.substring(results.indexOf(" :satellite =")),
     * expected, "RESULTS=\n" + results);
     *
     *
     * } finally {
     * nc.close();
     * }
     *
     */
  }

  /** This tests making maps where lon is 74 to 434 */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  // was an interactive test, likely needs additional changes
  void testMap74to434() throws Throwable {
    // String2.log("\n*** EDDGridFromDap.testMap74to434\n");
    // testVerboseOn();
    int language = 0;
    try {
      String mapDapQuery = "temperature[last][0][0:last][0:last]&.land=under"; // stride irrelevant
      EDDGrid gridDataset =
          (EDDGrid) EDDGridFromDap.oneFromDatasetsXml(null, "ncepRtofsG3DNowDaily"); // should
      // work
      String baseName = gridDataset.className() + "_434_Map";
      String tName =
          gridDataset.makeNewFileForDapQuery(
              language, null, null, mapDapQuery, EDStatic.fullTestCacheDirectory, baseName, ".png");
      Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
      // I can't use testImagesIdentical because the source data is changes every day
      // and old data isn't available.
      // Image2.testImagesIdentical(
      // EDStatic.fullTestCacheDirectory + tName,
      // String2.unitTestImagesDir() + baseName + ".png",
      // File2.getSystemTempDirectory() + baseName + "_diff.png");
      // String2.pressEnterToContinue();
    } catch (Throwable t) {
      String2.pressEnterToContinue(
          MustBe.throwableToString(t)
              + "\nFREQUENT ERROR. "
              + "\nOften, the dataset doesn't load because its URL hasn't be updated recently."
              + "\nOr a data request fails because GRADS is slow and times out or fails.");
    }
  }

  /**
   * This tests a bug in which land and national boundaries were draw twice and offset horizontally
   * if drawLandMask=under.
   *
   * <p>ANTIALIASING PROBLEM SOLVED ITSELF 2018-06-20 (not by me). It was a problem with
   * antialiasing, but turning antialiasing off or changing other renderingHints had no effict
   * (e.g., in SgtMap, see g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, )). I spent hours
   * trying to characterize then fix this, but found no solution.
   */
  @org.junit.jupiter.api.Test
  @TagImageComparison
  void testMapAntialiasing() throws Throwable {
    // String2.log("\n*** EDDGridFromDap.testMapAntialiasing\n");
    // testVerboseOn();
    int language = 0;
    EDDGrid gridDataset = (EDDGrid) EDDGridFromDap.oneFromDatasetsXml(null, "etopo360");

    String obsDir = Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR);
    // most color backgrounds don't have a problem
    String mapDapQuery =
        "altitude"
            + "[(68):(71)][(217):(220)]"
            + // stride irrelevant
            "&.draw=surface&.vars=longitude|latitude|altitude";
    String baseName = gridDataset.className() + "_mapAntialiasingOKAY";
    String tName =
        gridDataset.makeNewFileForDapQuery(
            language, null, null, mapDapQuery, obsDir, baseName, ".png");
    // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // some color backgrounds have a problem
    // as if it is bad interpolation based on pixel to left and right.
    // So vertical lines look worst and pure horizontal lines are okay.
    mapDapQuery += "&.colorBar=BlackBlueWhite|||||";
    baseName = gridDataset.className() + "_mapAntialiasingAlsoOkay";
    tName =
        gridDataset.makeNewFileForDapQuery(
            language, null, null, mapDapQuery, obsDir, baseName, ".png");
    // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    String2.log("\nANTIALIASING PROBLEM SOLVED ITSELF 2018-06-20");
  }

  /** This tests fixing an unhelpful error message. */
  @org.junit.jupiter.api.Test
  @TagThredds
  void testTimeErrorMessage() throws Throwable {
    // String2.log("\n*** EDDGridFromDap.testTimeErrorMessage\n");
    // testVerboseOn();
    int language = 0;
    EDDGrid gridDataset = (EDDGrid) EDDTestDataset.geterdBAssta5day();
    String dir = EDStatic.fullTestCacheDirectory;
    String results = "shouldn't be this", expected;
    int po;

    // start <
    try {
      String dapQuery = "sst[(2002-07-05)][(0.0)][(30):(31)][(225):(226)]";
      String tName =
          gridDataset.makeNewFileForDapQuery(
              language, null, null, dapQuery, dir, gridDataset.className() + "_timeError", ".nc");
      results = "shouldn't get here";
    } catch (Throwable t) {
      results = MustBe.throwableToString(t);
    }
    expected =
        "SimpleException: Your query produced no matching results. Query error: "
            + "For variable=sst axis#0=time Constraint=\"[(2002-07-05)]\": Start=\"2002-07-05\" "
            + "is less than the axis minimum=2002-07-06T12:00:00Z (and even 2002-07-05T22:49:16Z).";
    po = results.indexOf("SimpleException");
    String2.log("intentional error=\n" + results);
    Test.ensureEqual(results.substring(po, po + expected.length()), expected, "");

    // start >
    results = "shouldn't be this";
    try {
      String dapQuery = "sst[(2014-03-07)][(0.0)][(30):(31)][(225):(226)]";
      String tName =
          gridDataset.makeNewFileForDapQuery(
              language, null, null, dapQuery, dir, gridDataset.className() + "_timeError", ".nc");
      results = "shouldn't get here";
    } catch (Throwable t) {
      results = MustBe.throwableToString(t);
    }
    expected =
        "SimpleException: Your query produced no matching results. Query error: "
            + "For variable=sst axis#0=time Constraint=\"[(2014-03-07)]\": Start=\"2014-03-07\" "
            + "is greater than the axis maximum=2014-03-05T12:00:00Z (and even 2014-03-06T01:10:43Z).";
    po = results.indexOf("SimpleException");
    String2.log("intentional error=\n" + results);
    Test.ensureEqual(results.substring(po, po + expected.length()), expected, "");

    // end <
    results = "should be this";
    try {
      String dapQuery = "sst[(2010-03-01):(2014-02-01)][(0.0)][(30):(31)][(225):(226)]";
      String tName =
          gridDataset.makeNewFileForDapQuery(
              language, null, null, dapQuery, dir, gridDataset.className() + "_timeError", ".nc");
      results = "should be this";
    } catch (Throwable t) {
      results = MustBe.throwableToString(t);
    }
    expected = "should be this"; // 2022-02-07 because now [high:low] is allowed
    // "SimpleException: Query error: For variable=sst axis#0=time " +
    // "Constraint=\"[(2014-03-01):(2014-02-01)]\": StartIndex=3877 is greater than
    // StopIndex=3850.";
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    // end >
    results = "shouldn't be this";
    try {
      String dapQuery = "sst[(2010-03-06):(2014-03-07)][(0.0)][(30):(31)][(225):(226)]";
      String tName =
          gridDataset.makeNewFileForDapQuery(
              language, null, null, dapQuery, dir, gridDataset.className() + "_timeError", ".nc");
      results = "shouldn't get here";
    } catch (Throwable t) {
      results = MustBe.throwableToString(t);
    }
    expected =
        "SimpleException: Your query produced no matching results. Query error: "
            + "For variable=sst axis#0=time Constraint=\"[(2010-03-06):(2014-03-07)]\": "
            + "Stop=\"2014-03-07\" is greater than the axis maximum=2014-03-05T12:00:00Z "
            + "(and even 2014-03-06T01:10:43Z).";
    po = results.indexOf("SimpleException");
    String2.log("intentional error=\n" + results);
    Test.ensureEqual(results.substring(po, po + expected.length()), expected, "");
  }

  /** This tests creation of surface graphs (e.g., x,y axes, not lon,lat axes). */
  @org.junit.jupiter.api.Test
  @TagThredds
  @TagImageComparison
  void testSurfaceGraph() throws Throwable {
    // testVerboseOn();
    int language = 0;
    String baseName;

    EDDGrid gridDataset = (EDDGridFromDap) EDDTestDataset.gethawaii_d90f_20ee_c4cb();
    String tName, result, dir = Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR);
    // depth on Y axis
    baseName = gridDataset.className() + "_surfaceGraphB0";
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "temp[(2010-12-15)][][(-30.75)][]&.draw=surface&.vars=longitude|depth|temp",
            dir,
            baseName,
            ".png");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // Hovmoeller Diagram x=time, y=depth
    baseName = gridDataset.className() + "_surfaceGraphB1";
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "temp[(2009-12-15):(2010-12-15)][][(-30.75)][(225)]&.draw=surface&.vars=time|depth|temp",
            dir,
            baseName,
            ".png");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // qtot is weird: first 1/3 time complex red blue, 2nd half: big blog red, rest
    // blue
    gridDataset = (EDDGridFromDap) EDDTestDataset.gethycom_GLBa008_tyx();

    // minimal request
    baseName = gridDataset.className() + "_surfaceGraphA0";
    tName =
        gridDataset.makeNewFileForDapQuery(
            language, null, null, "qtot[1500][][]", dir, baseName, ".png");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // .draw specified
    baseName = gridDataset.className() + "_surfaceGraphA1";
    tName =
        gridDataset.makeNewFileForDapQuery(
            language, null, null, "qtot[1500][][]&.draw=surface", dir, baseName, ".png");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // .vars specified
    baseName = gridDataset.className() + "_surfaceGraphA2";
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "qtot[1500][][]&.vars=Y|X|qtot", // intentionally flipped x/y
            dir,
            baseName,
            ".png");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // .draw specified
    baseName = gridDataset.className() + "_surfaceGraphA3";
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null, // min|max|nDiv
            "qtot[1500][][]&.draw=surface&.vars=X|Y|qtot&.colorBar=LightRainbow|||||",
            dir,
            baseName,
            ".png");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // time on x axis
    baseName = gridDataset.className() + "_surfaceGraphA4";
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "qtot[1500:1510][1200][]&.draw=surface&.vars=time|X|qtot", // 1200 is through Australia
            dir,
            baseName,
            ".png");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // time on y axis
    baseName = gridDataset.className() + "_surfaceGraphA5";
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "qtot[1500:1510][1200][]&.draw=surface&.vars=X|time|qtot", // 1200 is through Australia
            dir,
            baseName,
            ".png");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // 2 x&y axis values
    baseName = gridDataset.className() + "_surfaceGraph2Values";
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "qtot[1500:1501][1200][313:314]&.draw=surface&.vars=time|X|qtot",
            dir,
            baseName,
            ".png");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // 1 x&y axis values
    // fails in EDDGrid.saveAsImage at test: if (nAAv < 1 || nAAv > 2)
    // because all axes have just 1 value.
    // This could probably be made to work because .vars is specified (so activeAxis
    // are known)
    // and x,y axis range could be made +/- avgSpacing/2.
    // tName = gridDataset.makeNewFileForDapQuery(language, null, null,
    // "qtot[1500][1200][313]&.draw=surface&.vars=time|X|qtot",
    // dir, gridDataset.className() + "_surfaceGraph1Value", ".png");
    // Test.displayInBrowser("file://" + dir + tName);
  }

  /** Ensure that packed source atts valid_min, valid_max are unpacked when dataset is loaded. */
  @org.junit.jupiter.api.Test
  void testValidMinMax() throws Throwable {
    // String2.log("\n\n*** EDDGridFromDap.testValidMinMax");
    String results, expected, tName, userDapQuery;
    String tDir = EDStatic.fullTestCacheDirectory;
    int language = 0;

    // ncdump of source file
    String fiName =
        EDDGridFromDapTests.class
            .getResource(
                "/largeSatellite/PH2/sstd/1day/20121231152528-NODC-L3C_GHRSST-SSTskin-AVHRR_Pathfinder-PFV5.2_NOAA19_G_2012366_day-v02.0-fv01.0.nc")
            .getPath();
    String2.log("ncdump of " + fiName + ":");
    String2.log(NcHelper.ncdump(fiName, "-h"));

    // look at metadata
    EDDGrid edd = (EDDGrid) EDDTestDataset.getnodcPH2sstd1day(); // should work
    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            edd.className() + "_vmm",
            ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "  sea_surface_temperature {\n"
            + "    Float32 _FillValue -327.68;\n"
            + // 2020-01-21 appeared with netcdf-java 5.2, so
            // what did this test
            // actually test before?
            "    Float64 colorBarMaximum 32.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String comment \"Skin temperature of the ocean\";\n"
            + "    String grid_mapping \"Equidistant Cylindrical\";\n"
            + "    String ioos_category \"Temperature\";\n"
            + "    String long_name \"NOAA Climate Data Record of Sea Surface Skin Temperature\";\n"
            + "    String standard_name \"sea_surface_skin_temperature\";\n"
            + "    String units \"degree_C\";\n"
            + "    Float32 valid_max 45.0;\n"
            + // 2020-01-21 vmin/max appeared with netcdf-java 5.2,
            // so what did this
            // test
            // actually test before?
            "    Float32 valid_min -1.8;\n"
            + "  }\n";
    int po = results.indexOf("  sea_surface_temperature {");
    Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "RESULTS=\n" + results);

    // get some data
    userDapQuery = "sea_surface_temperature[(2001-01-01T12)][(10):100:(0)][(-140):100:(-130)]";
    tName =
        edd.makeNewFileForDapQuery(
            language, null, null, userDapQuery, tDir, edd.className() + "_tvmm", ".csv");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected =
        "time,latitude,longitude,sea_surface_temperature\n"
            + "UTC,degrees_north,degrees_east,degree_C\n"
            + "2001-01-01T12:00:00Z,9.979172,-139.97917,NaN\n"
            + "2001-01-01T12:00:00Z,9.979172,-135.8125,2.991\n"
            + "2001-01-01T12:00:00Z,9.979172,-131.64584,2.9799\n"
            + "2001-01-01T12:00:00Z,5.8125076,-139.97917,2.8749\n"
            + "2001-01-01T12:00:00Z,5.8125076,-135.8125,NaN\n"
            + "2001-01-01T12:00:00Z,5.8125076,-131.64584,NaN\n"
            + "2001-01-01T12:00:00Z,1.6458359,-139.97917,2.9751\n"
            + "2001-01-01T12:00:00Z,1.6458359,-135.8125,2.9631\n"
            + "2001-01-01T12:00:00Z,1.6458359,-131.64584,2.9845\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
  }

  /**
   * This tests many things in generateDatasetsXml, notably, conversion of units from kelvin to
   * degree_C.
   */
  @org.junit.jupiter.api.Test
  @TagThredds
  void testGenerateDatasetsXml4() throws Throwable {
    String url =
        "https://thredds.jpl.nasa.gov/thredds/dodsC/OceanTemperature/AVHRR_SST_METOP_A_GLB-OSISAF-L3C-v1.0.nc";
    // String2.log("*** EDDGridFromDap.testGenerateDatasetsXml4\n" +
    // "url=" + url +
    // "\n.dds:\n");
    // String2.log(SSR.getUrlResponseStringNewline(url + ".dds"));
    // String2.log("\n.das:\n");
    // String2.log(SSR.getUrlResponseStringNewline(url + ".das"));
    int language = 0;

    String results = EDDGridFromDap.generateDatasetsXml(url, null, null, null, 10080, null);

    // KNOWN PROBLEM knownProblem -- these times bounce around -- I don't know why
    String change[] =
        new String[] {
          "date_created",
          "start_time",
          "stop_time",
          "time_coverage_start",
          "time_coverage_end",
          "uuid"
        };
    for (int i = 0; i < change.length; i++) {
      results =
          results.replaceAll(
              "<att name=\"" + change[i] + "\">.*</att>",
              "<att name=\"" + change[i] + "\">[VARIES]</att>");
    }
    results =
        results.replaceAll(
            "<att name=\"time_offset\" type=\"double\">.*</att>\n", // changes 12 or 24 (or 0 in
            // another place)
            "<att name=\"time_offset\" type=\"double\">[TIME_OFFSET]</att>\n");

    String tDatasetID = "nasa_jpl_aa77_f42f_c5ff";
    String expected =
        "<dataset type=\"EDDGridFromDap\" datasetID=\""
            + tDatasetID
            + "\" active=\"true\">\n"
            + "    <sourceUrl>https://thredds.jpl.nasa.gov/thredds/dodsC/OceanTemperature/AVHRR_SST_METOP_A_GLB-OSISAF-L3C-v1.0.nc</sourceUrl>\n"
            + "    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n"
            + "    <!-- sourceAttributes>\n"
            + "        <att name=\"acknowledgment\">In case SAF data (pre-operational or operational) has been used for the study described in a paper the following sentence would be an appropriate reference to the funding coming from EUMETSAT: The data from the EUMETSAT Satellite Application Facility on Ocean &amp; Sea Ice  used in this study are accessible through the SAF&#39;s homepage http://www.osi-saf.org</att>\n"
            + "        <att name=\"cdm_data_type\">grid</att>\n"
            + "        <att name=\"Conventions\">CF-1.4</att>\n"
            + "        <att name=\"creator_email\">helpdesk@osi-saf.org</att>\n"
            + "        <att name=\"creator_name\">O&amp;SI SAF</att>\n"
            + "        <att name=\"creator_url\">http://www.osi-saf.org</att>\n"
            + "        <att name=\"date_created\">[VARIES]</att>\n"
            + "        <att name=\"easternmost_longitude\" type=\"float\">180.0</att>\n"
            + "        <att name=\"file_quality_level\" type=\"int\">3</att>\n"
            + "        <att name=\"gds_version_id\">2.0</att>\n"
            + "        <att name=\"geospatial_lat_resolution\" type=\"double\">0.05</att>\n"
            + "        <att name=\"geospatial_lat_units\">degrees_north</att>\n"
            + "        <att name=\"geospatial_lon_resolution\" type=\"double\">0.05</att>\n"
            + "        <att name=\"geospatial_lon_units\">degrees_east</att>\n"
            + "        <att name=\"history\">METEO-FRANCE/CMS SAFOA processor</att>\n"
            + "        <att name=\"id\">AVHRR_SST_METOP_A_GLB-OSISAF-L3C-v1.0</att>\n"
            + "        <att name=\"institution\">OSISAF</att>\n"
            + "        <att name=\"keywords\">Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature</att>\n"
            + "        <att name=\"keywords_vocabulary\">NASA Global Change Master Directory (GCMD) Science Keywords</att>\n"
            + "        <att name=\"license\">All intellectual property rights of the Ocean &amp; Sea Ice SAF products belong to EUMETSAT. The use of these products is granted to every user, free of charge. If users wish to use these products, EUMETSAT&#39;s copyright credit must be shown by displaying the words &#39;Copyright EUMETSAT&#39; under each of the products shown. EUMETSAT offers no warranty and accepts no liability in respect of the Ocean &amp; Sea Ice SAF products. EUMETSAT neither commits to nor guarantees the continuity, availability, or quality or suitability for any purpose of, the Ocean &amp; Sea Ice SAF products.</att>\n"
            + "        <att name=\"Metadata_Conventions\">Unidata Dataset Discovery v1.0</att>\n"
            + "        <att name=\"metadata_link\">N/A</att>\n"
            + "        <att name=\"naming_authority\">org.ghrsst</att>\n"
            + "        <att name=\"netcdf_version_id\">4.2.1.1</att>\n"
            + "        <att name=\"northernmost_latitude\" type=\"float\">90.0</att>\n"
            + "        <att name=\"platform\">METOP_A</att>\n"
            + "        <att name=\"processing_level\">L3C</att>\n"
            + "        <att name=\"product_version\">1.0</att>\n"
            + "        <att name=\"project\">Group for High Resolution Sea Surface Temperature</att>\n"
            + "        <att name=\"publisher_email\">ghrsst-po@nceo.ac.uk</att>\n"
            + "        <att name=\"publisher_name\">The GHRSST Project Office</att>\n"
            + "        <att name=\"publisher_url\">http://www.ghrsst.org</att>\n"
            + "        <att name=\"references\">Low Earth Orbiter Sea Surface Temperature Product User Manual, http://www.osi-saf.org</att>\n"
            + "        <att name=\"sensor\">AVHRR</att>\n"
            + "        <att name=\"source\">AVHRR</att>\n"
            + "        <att name=\"southernmost_latitude\" type=\"float\">-90.0</att>\n"
            + "        <att name=\"spatial_resolution\">0.05 degree</att>\n"
            + "        <att name=\"standard_name_vocabulary\">NetCDF Climate and Forecast (CF) Metadata Convention</att>\n"
            + "        <att name=\"start_time\">[VARIES]</att>\n"
            + // changes. test with regex?
            "        <att name=\"stop_time\">[VARIES]</att>\n"
            + // changes. test with regex?
            "        <att name=\"summary\">The GLB L3C product derived from METOP A AVHRR brightness temperatures.</att>\n"
            + "        <att name=\"time_coverage_end\">[VARIES]</att>\n"
            + // changes
            "        <att name=\"time_coverage_start\">[VARIES]</att>\n"
            + // changes
            "        <att name=\"title\">Sea Surface Temperature</att>\n"
            + "        <att name=\"uuid\">[VARIES]</att>\n"
            + // changes
            "        <att name=\"westernmost_longitude\" type=\"float\">-180.0</att>\n"
            + "    </sourceAttributes -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"acknowledgement\">In case SAF data (pre-operational or operational) has been used for the study described in a paper the following sentence would be an appropriate reference to the funding coming from EUMETSAT: The data from the EUMETSAT Satellite Application Facility on Ocean &amp; Sea Ice  used in this study are accessible through the SAF&#39;s homepage http://www.osi-saf.org</att>\n"
            + "        <att name=\"acknowledgment\">null</att>\n"
            + "        <att name=\"cdm_data_type\">Grid</att>\n"
            + "        <att name=\"Conventions\">CF-1.10, COARDS, ACDD-1.3</att>\n"
            + "        <att name=\"creator_type\">institution</att>\n"
            + "        <att name=\"date_created\">[VARIES]</att>\n"
            + "        <att name=\"easternmost_longitude\">null</att>\n"
            + "        <att name=\"file_quality_level\">null</att>\n"
            + "        <att name=\"infoUrl\">https://podaac.jpl.nasa.gov/dataset/AVHRR_SST_METOP_A_GLB-OSISAF-L3C-v1.0</att>\n"
            + "        <att name=\"keywords\">10m, adi, adi_dtime_from_sst, advanced, aerosol, aerosol_dynamic_indicator, analysis, angle, application, area, atmosphere, atmospheric, avhrr, bias, climatology, cryosphere, data, deviation, difference, distribution, dt_analysis, dynamic, earth, Earth Science &gt; Atmosphere &gt; Atmospheric Radiation &gt; Incoming Solar Radiation, Earth Science &gt; Atmosphere &gt; Atmospheric Radiation &gt; Solar Irradiance, Earth Science &gt; Atmosphere &gt; Atmospheric Radiation &gt; Solar Radiation, Earth Science &gt; Atmosphere &gt; Atmospheric Winds &gt; Surface Winds, Earth Science &gt; Cryosphere &gt; Sea Ice &gt; Ice Extent, Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature, Earth Science &gt; Oceans &gt; Sea Ice &gt; Ice Extent, error, estimate, extent, facility, flags, fraction, glb, glb-osisaf-l3c-v1.0, high, ice, ice distribution, incoming, indicator, irradiance, l2p, l2p_flags, l3c, latitude, level, longitude, measurement, metop, ocean, oceans, optical, optical properties, osisaf, pixel, properties, quality, quality_level, radiation, radiometer, reference, resolution, satellite, satellite_zenith_angle, science, sea, sea_ice_area_fraction, sea_ice_fraction, sea_surface_subskin_temperature, sea_surface_temperature, sensor, single, solar, solar_zenith_angle, sources, sources_of_adi, speed, sses, sses_bias, sses_standard_deviation, sst, sst_dtime, standard, statistics, subskin, surface, temperature, time, v1.0, very, vhrr, wind, wind_speed, winds, zenith</att>\n"
            + "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n"
            + "        <att name=\"Metadata_Conventions\">null</att>\n"
            + "        <att name=\"metadata_link\">null</att>\n"
            + "        <att name=\"netcdf_version_id\">null</att>\n"
            + "        <att name=\"northernmost_latitude\">null</att>\n"
            + "        <att name=\"publisher_type\">group</att>\n"
            + "        <att name=\"publisher_url\">https://www.ghrsst.org</att>\n"
            + "        <att name=\"southernmost_latitude\">null</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"start_time\">[VARIES]</att>\n"
            + // null
            "        <att name=\"stop_time\">[VARIES]</att>\n"
            + // null
            "        <att name=\"summary\">Sea Surface Temperature. The GLB L3C product derived from METOP A Advanced Very High Resolution Radiometer (AVHRR) brightness temperatures.</att>\n"
            + "        <att name=\"time_coverage_end\">[VARIES]</att>\n"
            + // null
            "        <att name=\"time_coverage_start\">[VARIES]</att>\n"
            + // null
            "        <att name=\"title\">SST (AVHRR SST METOP A GLB-OSISAF-L3C-v1.0), 2013-2016</att>\n"
            + "        <att name=\"uuid\">[VARIES]</att>\n"
            + // null
            "        <att name=\"westernmost_longitude\">null</att>\n"
            + "    </addAttributes>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>time</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_ChunkSizes\" type=\"int\">1</att>\n"
            + "            <att name=\"axis\">T</att>\n"
            + "            <att name=\"comment\">includes leap seconds since 1981</att>\n"
            + "            <att name=\"long_name\">reference time of sst file</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"units\">seconds since 1981-01-01 00:00:00</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_ChunkSizes\">null</att>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"units\">seconds since 1981-01-01T00:00:00Z</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>lat</sourceName>\n"
            + "        <destinationName>latitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_ChunkSizes\" type=\"int\">3600</att>\n"
            + "            <att name=\"axis\">Y</att>\n"
            + "            <att name=\"comment\">geographical coordinates, WGS84 projection</att>\n"
            + "            <att name=\"long_name\">latitude</att>\n"
            + "            <att name=\"standard_name\">latitude</att>\n"
            + "            <att name=\"units\">degrees_north</att>\n"
            + "            <att name=\"valid_max\" type=\"float\">90.0</att>\n"
            + "            <att name=\"valid_min\" type=\"float\">-90.0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_ChunkSizes\">null</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Latitude</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>lon</sourceName>\n"
            + "        <destinationName>longitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_ChunkSizes\" type=\"int\">7200</att>\n"
            + "            <att name=\"axis\">X</att>\n"
            + "            <att name=\"comment\">geographical coordinates, WGS84 projection</att>\n"
            + "            <att name=\"long_name\">longitude</att>\n"
            + "            <att name=\"standard_name\">longitude</att>\n"
            + "            <att name=\"units\">degrees_east</att>\n"
            + "            <att name=\"valid_max\" type=\"float\">180.0</att>\n"
            + "            <att name=\"valid_min\" type=\"float\">-180.0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_ChunkSizes\">null</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Longitude</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>sea_surface_temperature</sourceName>\n"
            + "        <destinationName>sea_surface_temperature</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_ChunkSizes\" type=\"intList\">1 1200 2400</att>\n"
            + "            <att name=\"_FillValue\" type=\"short\">-32768</att>\n"
            + "            <att name=\"add_offset\" type=\"double\">273.15</att>\n"
            + "            <att name=\"comment\">Temperature of the subskin of the ocean</att>\n"
            + "            <att name=\"depth\">1 millimeter</att>\n"
            + "            <att name=\"long_name\">sea surface subskin temperature</att>\n"
            + "            <att name=\"scale_factor\" type=\"double\">0.01</att>\n"
            + "            <att name=\"source\">AVHRR_METOP_A</att>\n"
            + "            <att name=\"standard_name\">sea_surface_subskin_temperature</att>\n"
            + "            <att name=\"units\">kelvin</att>\n"
            + "            <att name=\"valid_max\" type=\"short\">4500</att>\n"
            + "            <att name=\"valid_min\" type=\"short\">-300</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_ChunkSizes\">null</att>\n"
            + "            <att name=\"add_offset\" type=\"double\">0.0</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Temperature</att>\n"
            + "            <att name=\"units\">degree_C</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>sst_dtime</sourceName>\n"
            + "        <destinationName>sst_dtime</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_ChunkSizes\" type=\"intList\">1 1200 2400</att>\n"
            + "            <att name=\"_FillValue\" type=\"int\">-2147483648</att>\n"
            + "            <att name=\"add_offset\" type=\"double\">0.0</att>\n"
            + "            <att name=\"comment\">time plus sst_dtime gives seconds after 00:00:00 UTC January 1, 1981</att>\n"
            + "            <att name=\"long_name\">time difference from reference time</att>\n"
            + "            <att name=\"scale_factor\" type=\"double\">1.0</att>\n"
            + "            <att name=\"units\">seconds</att>\n"
            + "            <att name=\"valid_max\" type=\"int\">2147483647</att>\n"
            + "            <att name=\"valid_min\" type=\"int\">-2147483647</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_ChunkSizes\">null</att>\n"
            + "            <att name=\"add_offset\">null</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-10.0</att>\n"
            + "            <att name=\"ioos_category\">Temperature</att>\n"
            + "            <att name=\"scale_factor\">null</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>sses_bias</sourceName>\n"
            + "        <destinationName>sses_bias</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_ChunkSizes\" type=\"intList\">1 1800 3600</att>\n"
            + "            <att name=\"_FillValue\" type=\"short\">-128</att>\n"
            + "            <att name=\"_Unsigned\">false</att>\n"
            + "            <att name=\"add_offset\" type=\"double\">0.0</att>\n"
            + "            <att name=\"comment\">Bias estimate derived using the techniques described at http://www.ghrsst.org/SSES-Description-of-schemes.html</att>\n"
            + "            <att name=\"long_name\">SSES bias estimate</att>\n"
            + "            <att name=\"scale_factor\" type=\"double\">0.01</att>\n"
            + "            <att name=\"units\">kelvin</att>\n"
            + "            <att name=\"valid_max\" type=\"byte\">127</att>\n"
            + "            <att name=\"valid_min\" type=\"short\">-127</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_ChunkSizes\">null</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">5.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"comment\">Bias estimate derived using the techniques described at https://www.ghrsst.org/SSES-Description-of-schemes.html</att>\n"
            + "            <att name=\"ioos_category\">Statistics</att>\n"
            + "            <att name=\"units\">degree_C</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>sses_standard_deviation</sourceName>\n"
            + // source var is byte
            "        <destinationName>sses_standard_deviation</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_ChunkSizes\" type=\"intList\">1 1800 3600</att>\n"
            + "            <att name=\"_FillValue\" type=\"short\">-128</att>\n"
            + // source is short!
            // to deal with
            // dap byte
            // being 0..255
            "            <att name=\"_Unsigned\">false</att>\n"
            + "            <att name=\"add_offset\" type=\"double\">1.0</att>\n"
            + "            <att name=\"comment\">Standard deviation estimate derived using the techniques described at http://www.ghrsst.org/SSES-Description-of-schemes.html</att>\n"
            + "            <att name=\"long_name\">SSES standard deviation</att>\n"
            + "            <att name=\"scale_factor\" type=\"double\">0.01</att>\n"
            + "            <att name=\"units\">kelvin</att>\n"
            + "            <att name=\"valid_max\" type=\"byte\">127</att>\n"
            + "            <att name=\"valid_min\" type=\"short\">-127</att>\n"
            + // source is short!
            // to deal with dap
            // byte
            // being 0..255
            "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_ChunkSizes\">null</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">5.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"comment\">Standard deviation estimate derived using the techniques described at https://www.ghrsst.org/SSES-Description-of-schemes.html</att>\n"
            + "            <att name=\"ioos_category\">Statistics</att>\n"
            + "            <att name=\"units\">degree_C</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>dt_analysis</sourceName>\n"
            + "        <destinationName>dt_analysis</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_ChunkSizes\" type=\"intList\">1 1800 3600</att>\n"
            + "            <att name=\"_FillValue\" type=\"short\">-128</att>\n"
            + "            <att name=\"_Unsigned\">false</att>\n"
            + "            <att name=\"add_offset\" type=\"double\">0.0</att>\n"
            + "            <att name=\"comment\">The difference between this SST and the previous day&#39;s SST analysis</att>\n"
            + "            <att name=\"long_name\">deviation from SST analysis or reference climatology</att>\n"
            + "            <att name=\"reference\">OSTIA</att>\n"
            + "            <att name=\"scale_factor\" type=\"double\">0.1</att>\n"
            + "            <att name=\"units\">kelvin</att>\n"
            + "            <att name=\"valid_max\" type=\"byte\">127</att>\n"
            + "            <att name=\"valid_min\" type=\"short\">-127</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_ChunkSizes\">null</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">5.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-5.0</att>\n"
            + "            <att name=\"ioos_category\">Statistics</att>\n"
            + "            <att name=\"reference\">null</att>\n"
            + "            <att name=\"references\">OSTIA</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>wind_speed</sourceName>\n"
            + "        <destinationName>wind_speed</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_ChunkSizes\" type=\"intList\">1 1800 3600</att>\n"
            + "            <att name=\"_FillValue\" type=\"short\">-128</att>\n"
            + "            <att name=\"_Unsigned\">false</att>\n"
            + "            <att name=\"add_offset\" type=\"double\">0.0</att>\n"
            + "            <att name=\"comment\">These wind speeds were created by the ECMWF and represent winds at 10 metres above the sea surface</att>\n"
            + "            <att name=\"height\">10 m</att>\n"
            + "            <att name=\"long_name\">10m wind speed</att>\n"
            + "            <att name=\"scale_factor\" type=\"double\">1.0</att>\n"
            + "            <att name=\"source\">WSP-ECMWF-Forecast</att>\n"
            + "            <att name=\"standard_name\">wind_speed</att>\n"
            + "            <att name=\"time_offset\" type=\"double\">[TIME_OFFSET]</att>\n"
            + "            <att name=\"units\">m s-1</att>\n"
            + "            <att name=\"valid_max\" type=\"byte\">127</att>\n"
            + "            <att name=\"valid_min\" type=\"byte\">0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_ChunkSizes\">null</att>\n"
            + "            <att name=\"add_offset\">null</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Wind</att>\n"
            + "            <att name=\"scale_factor\">null</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>sea_ice_fraction</sourceName>\n"
            + "        <destinationName>sea_ice_fraction</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_ChunkSizes\" type=\"intList\">1 1800 3600</att>\n"
            + "            <att name=\"_FillValue\" type=\"short\">-128</att>\n"
            + "            <att name=\"_Unsigned\">false</att>\n"
            + "            <att name=\"add_offset\" type=\"double\">0.0</att>\n"
            + "            <att name=\"comment\">Fractional sea ice cover from OSISAF ice concentration product</att>\n"
            + "            <att name=\"long_name\">sea ice fraction</att>\n"
            + "            <att name=\"scale_factor\" type=\"double\">0.01</att>\n"
            + "            <att name=\"source\">ICE-OSISAF</att>\n"
            + "            <att name=\"standard_name\">sea_ice_area_fraction</att>\n"
            + "            <att name=\"time_offset\" type=\"double\">[TIME_OFFSET]</att>\n"
            + // changes
            // 12 or
            // 24
            "            <att name=\"valid_max\" type=\"byte\">100</att>\n"
            + "            <att name=\"valid_min\" type=\"byte\">0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_ChunkSizes\">null</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">1.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Ice Distribution</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>aerosol_dynamic_indicator</sourceName>\n"
            + "        <destinationName>aerosol_dynamic_indicator</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_ChunkSizes\" type=\"intList\">1 1800 3600</att>\n"
            + "            <att name=\"_FillValue\" type=\"short\">-128</att>\n"
            + "            <att name=\"_Unsigned\">false</att>\n"
            + "            <att name=\"add_offset\" type=\"double\">0.0</att>\n"
            + "            <att name=\"long_name\">aerosol dynamic indicator</att>\n"
            + "            <att name=\"scale_factor\" type=\"double\">0.1</att>\n"
            + "            <att name=\"source\">sources_of_adi</att>\n"
            + "            <att name=\"valid_max\" type=\"byte\">127</att>\n"
            + "            <att name=\"valid_min\" type=\"byte\">0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_ChunkSizes\">null</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Optical Properties</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>adi_dtime_from_sst</sourceName>\n"
            + "        <destinationName>adi_dtime_from_sst</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_ChunkSizes\" type=\"intList\">1 1800 3600</att>\n"
            + "            <att name=\"_FillValue\" type=\"short\">-128</att>\n"
            + "            <att name=\"_Unsigned\">false</att>\n"
            + "            <att name=\"add_offset\" type=\"double\">0.0</att>\n"
            + "            <att name=\"comment\">Difference in hours between the ADI and SST data</att>\n"
            + "            <att name=\"long_name\">time difference of ADI data from sst measurement</att>\n"
            + "            <att name=\"scale_factor\" type=\"double\">0.1</att>\n"
            + "            <att name=\"units\">hour</att>\n"
            + "            <att name=\"valid_max\" type=\"byte\">127</att>\n"
            + "            <att name=\"valid_min\" type=\"short\">-127</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_ChunkSizes\">null</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-10.0</att>\n"
            + "            <att name=\"ioos_category\">Temperature</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>sources_of_adi</sourceName>\n"
            + "        <destinationName>sources_of_adi</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_ChunkSizes\" type=\"intList\">1 1800 3600</att>\n"
            + "            <att name=\"_FillValue\" type=\"short\">-128</att>\n"
            + "            <att name=\"_Unsigned\">false</att>\n"
            + "            <att name=\"comment\">This variable provides a pixel by pixel description of where aerosol optical depth were derived from.</att>\n"
            + "            <att name=\"flag_meanings\">no_data AOD-NAAPS-ADI SDI-OSISAF-ADI</att>\n"
            + "            <att name=\"flag_values\" type=\"byteList\">0 1 2</att>\n"
            + "            <att name=\"long_name\">sources of aerosol dynamic indicator</att>\n"
            + "            <att name=\"valid_max\" type=\"byte\">2</att>\n"
            + "            <att name=\"valid_min\" type=\"byte\">0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_ChunkSizes\">null</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">2.5</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Optical Properties</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>l2p_flags</sourceName>\n"
            + "        <destinationName>l2p_flags</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_ChunkSizes\" type=\"intList\">1 1200 2400</att>\n"
            + "            <att name=\"comment\">These flags are important to properly use the data.</att>\n"
            + "            <att name=\"flag_masks\" type=\"shortList\">1 2 4 8</att>\n"
            + "            <att name=\"flag_meanings\">microwave land ice lake</att>\n"
            + "            <att name=\"long_name\">L2P flags</att>\n"
            + "            <att name=\"valid_max\" type=\"short\">15</att>\n"
            + "            <att name=\"valid_min\" type=\"short\">0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_ChunkSizes\">null</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>quality_level</sourceName>\n"
            + "        <destinationName>quality_level</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_ChunkSizes\" type=\"intList\">1 1800 3600</att>\n"
            + "            <att name=\"_FillValue\" type=\"short\">-128</att>\n"
            + "            <att name=\"_Unsigned\">false</att>\n"
            + "            <att name=\"comment\">These are the overall quality indicators and are used for all GHRSST SSTs</att>\n"
            + "            <att name=\"flag_meanings\">no_data bad_data worst_quality low_quality acceptable_quality best_quality</att>\n"
            + "            <att name=\"flag_values\" type=\"byteList\">0 1 2 3 4 5</att>\n"
            + "            <att name=\"long_name\">quality level of SST pixel</att>\n"
            + "            <att name=\"valid_max\" type=\"byte\">5</att>\n"
            + "            <att name=\"valid_min\" type=\"byte\">0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_ChunkSizes\">null</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">6.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>satellite_zenith_angle</sourceName>\n"
            + "        <destinationName>satellite_zenith_angle</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_ChunkSizes\" type=\"intList\">1 1800 3600</att>\n"
            + "            <att name=\"_FillValue\" type=\"short\">-128</att>\n"
            + "            <att name=\"_Unsigned\">false</att>\n"
            + "            <att name=\"add_offset\" type=\"double\">0.0</att>\n"
            + "            <att name=\"comment\">The satellite zenith angle at the time of the SST observations.</att>\n"
            + "            <att name=\"long_name\">satellite zenith angle</att>\n"
            + "            <att name=\"scale_factor\" type=\"double\">1.0</att>\n"
            + "            <att name=\"units\">angular_degree</att>\n"
            + "            <att name=\"valid_max\" type=\"byte\">90</att>\n"
            + "            <att name=\"valid_min\" type=\"short\">-90</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_ChunkSizes\">null</att>\n"
            + "            <att name=\"add_offset\">null</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"scale_factor\">null</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>solar_zenith_angle</sourceName>\n"
            + "        <destinationName>solar_zenith_angle</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_ChunkSizes\" type=\"intList\">1 1800 3600</att>\n"
            + "            <att name=\"_FillValue\" type=\"short\">-128</att>\n"
            + "            <att name=\"_Unsigned\">false</att>\n"
            + "            <att name=\"add_offset\" type=\"double\">90.0</att>\n"
            + "            <att name=\"comment\">The solar zenith angle at the time of the SST observations.</att>\n"
            + "            <att name=\"long_name\">solar zenith angle</att>\n"
            + "            <att name=\"scale_factor\" type=\"double\">1.0</att>\n"
            + "            <att name=\"units\">angular_degree</att>\n"
            + "            <att name=\"valid_max\" type=\"byte\">90</att>\n"
            + "            <att name=\"valid_min\" type=\"short\">-90</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_ChunkSizes\">null</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"standard_name\">solar_zenith_angle</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    EDD.deleteCachedDatasetInfo(tDatasetID);
    EDD edd = EDDGridFromDap.oneFromXmlFragment(null, results);
    String tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            edd.className() + "_gdx4",
            ".dds");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "Dataset {\n"
            + "  Float64 time[time = 1975];\n"
            + "  Float32 latitude[latitude = 3600];\n"
            + "  Float32 longitude[longitude = 7200];\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float64 sea_surface_temperature[time = 1975][latitude = 3600][longitude = 7200];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 1975];\n"
            + "      Float32 latitude[latitude = 3600];\n"
            + "      Float32 longitude[longitude = 7200];\n"
            + "  } sea_surface_temperature;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Int32 sst_dtime[time = 1975][latitude = 3600][longitude = 7200];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 1975];\n"
            + "      Float32 latitude[latitude = 3600];\n"
            + "      Float32 longitude[longitude = 7200];\n"
            + "  } sst_dtime;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float64 sses_bias[time = 1975][latitude = 3600][longitude = 7200];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 1975];\n"
            + "      Float32 latitude[latitude = 3600];\n"
            + "      Float32 longitude[longitude = 7200];\n"
            + "  } sses_bias;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float64 sses_standard_deviation[time = 1975][latitude = 3600][longitude = 7200];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 1975];\n"
            + "      Float32 latitude[latitude = 3600];\n"
            + "      Float32 longitude[longitude = 7200];\n"
            + "  } sses_standard_deviation;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float64 dt_analysis[time = 1975][latitude = 3600][longitude = 7200];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 1975];\n"
            + "      Float32 latitude[latitude = 3600];\n"
            + "      Float32 longitude[longitude = 7200];\n"
            + "  } dt_analysis;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Byte wind_speed[time = 1975][latitude = 3600][longitude = 7200];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 1975];\n"
            + "      Float32 latitude[latitude = 3600];\n"
            + "      Float32 longitude[longitude = 7200];\n"
            + "  } wind_speed;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float64 sea_ice_fraction[time = 1975][latitude = 3600][longitude = 7200];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 1975];\n"
            + "      Float32 latitude[latitude = 3600];\n"
            + "      Float32 longitude[longitude = 7200];\n"
            + "  } sea_ice_fraction;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float64 aerosol_dynamic_indicator[time = 1975][latitude = 3600][longitude = 7200];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 1975];\n"
            + "      Float32 latitude[latitude = 3600];\n"
            + "      Float32 longitude[longitude = 7200];\n"
            + "  } aerosol_dynamic_indicator;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float64 adi_dtime_from_sst[time = 1975][latitude = 3600][longitude = 7200];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 1975];\n"
            + "      Float32 latitude[latitude = 3600];\n"
            + "      Float32 longitude[longitude = 7200];\n"
            + "  } adi_dtime_from_sst;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Byte sources_of_adi[time = 1975][latitude = 3600][longitude = 7200];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 1975];\n"
            + "      Float32 latitude[latitude = 3600];\n"
            + "      Float32 longitude[longitude = 7200];\n"
            + "  } sources_of_adi;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Int16 l2p_flags[time = 1975][latitude = 3600][longitude = 7200];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 1975];\n"
            + "      Float32 latitude[latitude = 3600];\n"
            + "      Float32 longitude[longitude = 7200];\n"
            + "  } l2p_flags;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Byte quality_level[time = 1975][latitude = 3600][longitude = 7200];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 1975];\n"
            + "      Float32 latitude[latitude = 3600];\n"
            + "      Float32 longitude[longitude = 7200];\n"
            + "  } quality_level;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Byte satellite_zenith_angle[time = 1975][latitude = 3600][longitude = 7200];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 1975];\n"
            + "      Float32 latitude[latitude = 3600];\n"
            + "      Float32 longitude[longitude = 7200];\n"
            + "  } satellite_zenith_angle;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float64 solar_zenith_angle[time = 1975][latitude = 3600][longitude = 7200];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 1975];\n"
            + "      Float32 latitude[latitude = 3600];\n"
            + "      Float32 longitude[longitude = 7200];\n"
            + "  } solar_zenith_angle;\n"
            + "} nasa_jpl_aa77_f42f_c5ff;\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            edd.className() + "_gdx4",
            ".nccsvMetadata");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    results =
        results.replaceAll("20\\d{2}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z http", "[DATE_TIME] http");
    results =
        results.replaceAll(
            "sea_ice_fraction,time_offset,.*d", "sea_ice_fraction,time_offset,[TIME_OFFSET]");
    expected =
        "*GLOBAL*,Conventions,\"CF-1.10, COARDS, ACDD-1.3, NCCSV-1.2\"\n"
            + "*GLOBAL*,acknowledgement,In case SAF data (pre-operational or operational) has been used for the study described in a paper the following sentence would be an appropriate reference to the funding coming from EUMETSAT: The data from the EUMETSAT Satellite Application Facility on Ocean & Sea Ice  used in this study are accessible through the SAF's homepage http://www.osi-saf.org\n"
            + "*GLOBAL*,cdm_data_type,Grid\n"
            + "*GLOBAL*,creator_email,helpdesk@osi-saf.org\n"
            + "*GLOBAL*,creator_name,O&SI SAF\n"
            + "*GLOBAL*,creator_type,institution\n"
            + "*GLOBAL*,creator_url,http://www.osi-saf.org\n"
            + "*GLOBAL*,date_created,[VARIES]\n"
            + "*GLOBAL*,Easternmost_Easting,179.9746d\n"
            + "*GLOBAL*,gds_version_id,\"2.0\"\n"
            + "*GLOBAL*,geospatial_lat_max,89.97461d\n"
            + "*GLOBAL*,geospatial_lat_min,-89.97461d\n"
            + "*GLOBAL*,geospatial_lat_units,degrees_north\n"
            + "*GLOBAL*,geospatial_lon_max,179.9746d\n"
            + "*GLOBAL*,geospatial_lon_min,-179.9746d\n"
            + "*GLOBAL*,geospatial_lon_units,degrees_east\n"
            + "*GLOBAL*,history,METEO-FRANCE/CMS SAFOA processor\n"
            + "*GLOBAL*,id,AVHRR_SST_METOP_A_GLB-OSISAF-L3C-v1.0\n"
            + "*GLOBAL*,infoUrl,https://podaac.jpl.nasa.gov/dataset/AVHRR_SST_METOP_A_GLB-OSISAF-L3C-v1.0\n"
            + "*GLOBAL*,institution,OSISAF\n"
            + "*GLOBAL*,keywords,\"10m, adi, adi_dtime_from_sst, advanced, aerosol, aerosol_dynamic_indicator, analysis, angle, application, area, atmosphere, atmospheric, avhrr, bias, climatology, cryosphere, data, deviation, difference, distribution, dt_analysis, dynamic, earth, Earth Science > Atmosphere > Atmospheric Radiation > Incoming Solar Radiation, Earth Science > Atmosphere > Atmospheric Radiation > Solar Irradiance, Earth Science > Atmosphere > Atmospheric Radiation > Solar Radiation, Earth Science > Atmosphere > Atmospheric Winds > Surface Winds, Earth Science > Cryosphere > Sea Ice > Ice Extent, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, Earth Science > Oceans > Sea Ice > Ice Extent, error, estimate, extent, facility, flags, fraction, glb, glb-osisaf-l3c-v1.0, high, ice, ice distribution, incoming, indicator, irradiance, l2p, l2p_flags, l3c, latitude, level, longitude, measurement, metop, ocean, oceans, optical, optical properties, osisaf, pixel, properties, quality, quality_level, radiation, radiometer, reference, resolution, satellite, satellite_zenith_angle, science, sea, sea_ice_area_fraction, sea_ice_fraction, sea_surface_subskin_temperature, sea_surface_temperature, sensor, single, solar, solar_zenith_angle, sources, sources_of_adi, speed, sses, sses_bias, sses_standard_deviation, sst, sst_dtime, standard, statistics, subskin, surface, temperature, time, v1.0, very, vhrr, wind, wind_speed, winds, zenith\"\n"
            + "*GLOBAL*,keywords_vocabulary,GCMD Science Keywords\n"
            + "*GLOBAL*,license,\"All intellectual property rights of the Ocean & Sea Ice SAF products belong to EUMETSAT. The use of these products is granted to every user, free of charge. If users wish to use these products, EUMETSAT's copyright credit must be shown by displaying the words 'Copyright EUMETSAT' under each of the products shown. EUMETSAT offers no warranty and accepts no liability in respect of the Ocean & Sea Ice SAF products. EUMETSAT neither commits to nor guarantees the continuity, availability, or quality or suitability for any purpose of, the Ocean & Sea Ice SAF products.\"\n"
            + "*GLOBAL*,naming_authority,org.ghrsst\n"
            + "*GLOBAL*,Northernmost_Northing,89.97461d\n"
            + "*GLOBAL*,platform,METOP_A\n"
            + "*GLOBAL*,processing_level,L3C\n"
            + "*GLOBAL*,product_version,\"1.0\"\n"
            + "*GLOBAL*,project,Group for High Resolution Sea Surface Temperature\n"
            + "*GLOBAL*,publisher_email,ghrsst-po@nceo.ac.uk\n"
            + "*GLOBAL*,publisher_name,The GHRSST Project Office\n"
            + "*GLOBAL*,publisher_type,group\n"
            + "*GLOBAL*,publisher_url,https://www.ghrsst.org\n"
            + "*GLOBAL*,references,\"Low Earth Orbiter Sea Surface Temperature Product User Manual, http://www.osi-saf.org\"\n"
            + "*GLOBAL*,sensor,AVHRR\n"
            + "*GLOBAL*,source,AVHRR\n"
            + "*GLOBAL*,sourceUrl,https://thredds.jpl.nasa.gov/thredds/dodsC/OceanTemperature/AVHRR_SST_METOP_A_GLB-OSISAF-L3C-v1.0.nc\n"
            + "*GLOBAL*,Southernmost_Northing,-89.97461d\n"
            + "*GLOBAL*,spatial_resolution,0.05 degree\n"
            + "*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v70\n"
            + "*GLOBAL*,start_time,[VARIES]\n"
            + "*GLOBAL*,stop_time,[VARIES]\n"
            + "*GLOBAL*,summary,Sea Surface Temperature. The GLB L3C product derived from METOP A Advanced Very High Resolution Radiometer (AVHRR) brightness temperatures.\n"
            + "*GLOBAL*,time_coverage_end,2016-02-23T00:00:00Z\n"
            + "*GLOBAL*,time_coverage_start,2013-06-04T12:00:00Z\n"
            + "*GLOBAL*,title,\"SST (AVHRR SST METOP A GLB-OSISAF-L3C-v1.0), 2013-2016\"\n"
            + "*GLOBAL*,uuid,[VARIES]\n"
            + "*GLOBAL*,Westernmost_Easting,-179.9746d\n"
            + "time,*DATA_TYPE*,String\n"
            + "time,_CoordinateAxisType,Time\n"
            + "time,actual_range,2013-06-04T12:00:00Z\\n2016-02-23T00:00:00Z\n"
            + "time,axis,T\n"
            + "time,comment,includes leap seconds since 1981\n"
            + "time,ioos_category,Time\n"
            + "time,long_name,reference time of sst file\n"
            + "time,standard_name,time\n"
            + "time,time_origin,01-JAN-1970 00:00:00\n"
            + "time,units,yyyy-MM-dd'T'HH:mm:ssZ\n"
            + "latitude,*DATA_TYPE*,float\n"
            + "latitude,_CoordinateAxisType,Lat\n"
            + "latitude,actual_range,-89.97461f,89.97461f\n"
            + "latitude,axis,Y\n"
            + "latitude,comment,\"geographical coordinates, WGS84 projection\"\n"
            + "latitude,ioos_category,Location\n"
            + "latitude,long_name,Latitude\n"
            + "latitude,standard_name,latitude\n"
            + "latitude,units,degrees_north\n"
            + "latitude,valid_max,90.0f\n"
            + "latitude,valid_min,-90.0f\n"
            + "longitude,*DATA_TYPE*,float\n"
            + "longitude,_CoordinateAxisType,Lon\n"
            + "longitude,actual_range,-179.9746f,179.9746f\n"
            + "longitude,axis,X\n"
            + "longitude,comment,\"geographical coordinates, WGS84 projection\"\n"
            + "longitude,ioos_category,Location\n"
            + "longitude,long_name,Longitude\n"
            + "longitude,standard_name,longitude\n"
            + "longitude,units,degrees_east\n"
            + "longitude,valid_max,180.0f\n"
            + "longitude,valid_min,-180.0f\n"
            + "sea_surface_temperature,*DATA_TYPE*,double\n"
            + "sea_surface_temperature,_FillValue,-327.68d\n"
            + "sea_surface_temperature,colorBarMaximum,32.0d\n"
            + "sea_surface_temperature,colorBarMinimum,0.0d\n"
            + "sea_surface_temperature,comment,Temperature of the subskin of the ocean\n"
            + "sea_surface_temperature,depth,1 millimeter\n"
            + "sea_surface_temperature,ioos_category,Temperature\n"
            + "sea_surface_temperature,long_name,sea surface subskin temperature\n"
            + "sea_surface_temperature,source,AVHRR_METOP_A\n"
            + "sea_surface_temperature,standard_name,sea_surface_subskin_temperature\n"
            + "sea_surface_temperature,units,degree_C\n"
            + "sea_surface_temperature,valid_max,45.0d\n"
            + "sea_surface_temperature,valid_min,-3.0d\n"
            + "sst_dtime,*DATA_TYPE*,int\n"
            + "sst_dtime,_FillValue,-2147483648i\n"
            + "sst_dtime,colorBarMaximum,10.0d\n"
            + "sst_dtime,colorBarMinimum,-10.0d\n"
            + "sst_dtime,comment,\"time plus sst_dtime gives seconds after 00:00:00 UTC January 1, 1981\"\n"
            + "sst_dtime,ioos_category,Temperature\n"
            + "sst_dtime,long_name,time difference from reference time\n"
            + "sst_dtime,units,seconds\n"
            + "sst_dtime,valid_max,2147483647i\n"
            + "sst_dtime,valid_min,-2147483647i\n"
            + "sses_bias,*DATA_TYPE*,double\n"
            + "sses_bias,_FillValue,-1.28d\n"
            + "sses_bias,colorBarMaximum,5.0d\n"
            + "sses_bias,colorBarMinimum,0.0d\n"
            + "sses_bias,comment,Bias estimate derived using the techniques described at https://www.ghrsst.org/SSES-Description-of-schemes.html\n"
            + "sses_bias,ioos_category,Statistics\n"
            + "sses_bias,long_name,SSES bias estimate\n"
            + "sses_bias,units,degree_C\n"
            + "sses_bias,valid_max,1.27d\n"
            + "sses_bias,valid_min,-1.27d\n"
            + "sses_standard_deviation,*DATA_TYPE*,double\n"
            + "sses_standard_deviation,_FillValue,-0.28d\n"
            + "sses_standard_deviation,colorBarMaximum,5.0d\n"
            + "sses_standard_deviation,colorBarMinimum,0.0d\n"
            + "sses_standard_deviation,comment,Standard deviation estimate derived using the techniques described at https://www.ghrsst.org/SSES-Description-of-schemes.html\n"
            + "sses_standard_deviation,ioos_category,Statistics\n"
            + "sses_standard_deviation,long_name,SSES standard deviation\n"
            + "sses_standard_deviation,units,degree_C\n"
            + "sses_standard_deviation,valid_max,2.27d\n"
            + "sses_standard_deviation,valid_min,-0.27d\n"
            + "dt_analysis,*DATA_TYPE*,double\n"
            + "dt_analysis,_FillValue,-12.8d\n"
            + "dt_analysis,colorBarMaximum,5.0d\n"
            + "dt_analysis,colorBarMinimum,-5.0d\n"
            + "dt_analysis,comment,The difference between this SST and the previous day's SST analysis\n"
            + "dt_analysis,ioos_category,Statistics\n"
            + "dt_analysis,long_name,deviation from SST analysis or reference climatology\n"
            + "dt_analysis,references,OSTIA\n"
            + "dt_analysis,units,kelvin\n"
            + "dt_analysis,valid_max,12.700000000000001d\n"
            + "dt_analysis,valid_min,-12.700000000000001d\n"
            + "wind_speed,*DATA_TYPE*,byte\n"
            + "wind_speed,_FillValue,-128b\n"
            + "wind_speed,colorBarMaximum,15.0d\n"
            + "wind_speed,colorBarMinimum,0.0d\n"
            + "wind_speed,comment,These wind speeds were created by the ECMWF and represent winds at 10 metres above the sea surface\n"
            + "wind_speed,height,10 m\n"
            + "wind_speed,ioos_category,Wind\n"
            + "wind_speed,long_name,10m wind speed\n"
            + "wind_speed,source,WSP-ECMWF-Forecast\n"
            + "wind_speed,standard_name,wind_speed\n"
            + "wind_speed,time_offset,0.0d\n"
            + "wind_speed,units,m s-1\n"
            + "wind_speed,valid_max,127b\n"
            + "wind_speed,valid_min,0b\n"
            + "sea_ice_fraction,*DATA_TYPE*,double\n"
            + "sea_ice_fraction,_FillValue,-1.28d\n"
            + "sea_ice_fraction,colorBarMaximum,1.0d\n"
            + "sea_ice_fraction,colorBarMinimum,0.0d\n"
            + "sea_ice_fraction,comment,Fractional sea ice cover from OSISAF ice concentration product\n"
            + "sea_ice_fraction,ioos_category,Ice Distribution\n"
            + "sea_ice_fraction,long_name,sea ice fraction\n"
            + "sea_ice_fraction,source,ICE-OSISAF\n"
            + "sea_ice_fraction,standard_name,sea_ice_area_fraction\n"
            + "sea_ice_fraction,time_offset,[TIME_OFFSET]\n"
            + // varies
            "sea_ice_fraction,valid_max,1.0d\n"
            + "sea_ice_fraction,valid_min,0.0d\n"
            + "aerosol_dynamic_indicator,*DATA_TYPE*,double\n"
            + "aerosol_dynamic_indicator,_FillValue,-12.8d\n"
            + "aerosol_dynamic_indicator,colorBarMaximum,15.0d\n"
            + "aerosol_dynamic_indicator,colorBarMinimum,0.0d\n"
            + "aerosol_dynamic_indicator,ioos_category,Optical Properties\n"
            + "aerosol_dynamic_indicator,long_name,aerosol dynamic indicator\n"
            + "aerosol_dynamic_indicator,source,sources_of_adi\n"
            + "aerosol_dynamic_indicator,valid_max,12.700000000000001d\n"
            + "aerosol_dynamic_indicator,valid_min,0.0d\n"
            + "adi_dtime_from_sst,*DATA_TYPE*,double\n"
            + "adi_dtime_from_sst,_FillValue,-12.8d\n"
            + "adi_dtime_from_sst,colorBarMaximum,10.0d\n"
            + "adi_dtime_from_sst,colorBarMinimum,-10.0d\n"
            + "adi_dtime_from_sst,comment,Difference in hours between the ADI and SST data\n"
            + "adi_dtime_from_sst,ioos_category,Temperature\n"
            + "adi_dtime_from_sst,long_name,time difference of ADI data from sst measurement\n"
            + "adi_dtime_from_sst,units,hour\n"
            + "adi_dtime_from_sst,valid_max,12.700000000000001d\n"
            + "adi_dtime_from_sst,valid_min,-12.700000000000001d\n"
            + "sources_of_adi,*DATA_TYPE*,byte\n"
            + "sources_of_adi,_FillValue,-128b\n"
            + // note that _Unsigned doesn't appear in
            // .nccsvMetadata
            "sources_of_adi,colorBarMaximum,2.5d\n"
            + "sources_of_adi,colorBarMinimum,0.0d\n"
            + "sources_of_adi,comment,This variable provides a pixel by pixel description of where aerosol optical depth were derived from.\n"
            + "sources_of_adi,flag_meanings,no_data AOD-NAAPS-ADI SDI-OSISAF-ADI\n"
            + "sources_of_adi,flag_values,0b,1b,2b\n"
            + "sources_of_adi,ioos_category,Optical Properties\n"
            + "sources_of_adi,long_name,sources of aerosol dynamic indicator\n"
            + "sources_of_adi,valid_max,2b\n"
            + "sources_of_adi,valid_min,0b\n"
            + "l2p_flags,*DATA_TYPE*,short\n"
            + "l2p_flags,colorBarMaximum,10.0d\n"
            + "l2p_flags,colorBarMinimum,0.0d\n"
            + "l2p_flags,comment,These flags are important to properly use the data.\n"
            + "l2p_flags,flag_masks,1s,2s,4s,8s\n"
            + "l2p_flags,flag_meanings,microwave land ice lake\n"
            + "l2p_flags,ioos_category,Quality\n"
            + "l2p_flags,long_name,L2P flags\n"
            + "l2p_flags,valid_max,15s\n"
            + "l2p_flags,valid_min,0s\n"
            + "quality_level,*DATA_TYPE*,byte\n"
            + "quality_level,_FillValue,-128b\n"
            + "quality_level,colorBarMaximum,6.0d\n"
            + "quality_level,colorBarMinimum,0.0d\n"
            + "quality_level,comment,These are the overall quality indicators and are used for all GHRSST SSTs\n"
            + "quality_level,flag_meanings,no_data bad_data worst_quality low_quality acceptable_quality best_quality\n"
            + "quality_level,flag_values,0b,1b,2b,3b,4b,5b\n"
            + "quality_level,ioos_category,Quality\n"
            + "quality_level,long_name,quality level of SST pixel\n"
            + "quality_level,valid_max,5b\n"
            + "quality_level,valid_min,0b\n"
            + "satellite_zenith_angle,*DATA_TYPE*,byte\n"
            + "satellite_zenith_angle,_FillValue,-128b\n"
            + "satellite_zenith_angle,colorBarMaximum,90.0d\n"
            + "satellite_zenith_angle,colorBarMinimum,-90.0d\n"
            + "satellite_zenith_angle,comment,The satellite zenith angle at the time of the SST observations.\n"
            + "satellite_zenith_angle,ioos_category,Unknown\n"
            + "satellite_zenith_angle,long_name,satellite zenith angle\n"
            + "satellite_zenith_angle,units,angular_degree\n"
            + "satellite_zenith_angle,valid_max,90b\n"
            + "satellite_zenith_angle,valid_min,-90b\n"
            + "solar_zenith_angle,*DATA_TYPE*,double\n"
            + "solar_zenith_angle,_FillValue,-38.0d\n"
            + "solar_zenith_angle,colorBarMaximum,90.0d\n"
            + "solar_zenith_angle,colorBarMinimum,-90.0d\n"
            + "solar_zenith_angle,comment,The solar zenith angle at the time of the SST observations.\n"
            + "solar_zenith_angle,ioos_category,Unknown\n"
            + "solar_zenith_angle,long_name,solar zenith angle\n"
            + "solar_zenith_angle,standard_name,solar_zenith_angle\n"
            + "solar_zenith_angle,units,angular_degree\n"
            + "solar_zenith_angle,valid_max,180.0d\n"
            + "solar_zenith_angle,valid_min,0.0d\n"
            + "\n"
            + "*END_METADATA*\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /**
   * This tests many things in generateDatasetsXml, notably, reloadEveryNMinutes and testOutOfDate.
   */
  @org.junit.jupiter.api.Test
  @TagThredds
  void testGenerateDatasetsXml5() throws Throwable {
    String2.log("*** EDDGridFromDap.testGenerateDatasetsXml5");
    int language = 0;
    String results =
        EDDGridFromDap.generateDatasetsXml(
            "https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MUR41/ssta/1day",
            null,
            null,
            null,
            -1,
            null);
    String2.log(results);
    int po;
    String expected =
        "<dataset type=\"EDDGridFromDap\" datasetID=\"noaa_pfeg_f8f0_f003_fc21\" active=\"true\">\n"
            + "    <sourceUrl>https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MUR41/ssta/1day</sourceUrl>\n"
            + "    <reloadEveryNMinutes>180</reloadEveryNMinutes>\n"
            + "    <!-- sourceAttributes>\n"
            + "        <att name=\"acknowledgment\">Please acknowledge the use of these data with the following statement:  These data were provided by JPL under support by NASA MEaSUREs program.</att>\n"
            + "        <att name=\"cdm_data_type\">grid</att>\n"
            + "        <att name=\"comment\">Interim-MUR(nrt) will be replaced by MUR-Final in about 3 days; MUR = &quot;Multi-scale Ultra-high Resolution&quot;</att>\n"
            + "        <att name=\"Conventions\">CF-1.7</att>\n"
            + "        <att name=\"creator_email\">ghrsst@podaac.jpl.nasa.gov</att>\n"
            + "        <att name=\"creator_name\">JPL MUR SST project</att>\n"
            + "        <att name=\"creator_url\">http://mur.jpl.nasa.gov</att>\n";
    // " <att name=\"date_created\">20171220T023739Z</att>\n" +
    Test.ensureEqual(results.substring(0, expected.length()), expected, "");

    boolean withGCOM = true; // This changes periodically
    expected =
        "<att name=\"easternmost_longitude\" type=\"float\">180.0</att>\n"
            + "        <att name=\"file_quality_level\" type=\"int\">3</att>\n"
            + // this may change
            // depending on
            // source file
            // (eg, 1 or 3)
            "        <att name=\"gds_version_id\">2.0</att>\n"
            + "        <att name=\"geospatial_lat_resolution\" type=\"float\">0.01</att>\n"
            + "        <att name=\"geospatial_lat_units\">degrees north</att>\n"
            + "        <att name=\"geospatial_lon_resolution\" type=\"float\">0.01</att>\n"
            + "        <att name=\"geospatial_lon_units\">degrees east</att>\n"
            + "        <att name=\"history\">near real time (nrt) version created at nominal 1-day latency.</att>\n"
            + "        <att name=\"id\">MUR-JPL-L4-GLOB-v04.1</att>\n"
            + "        <att name=\"institution\">Jet Propulsion Laboratory</att>\n"
            + "        <att name=\"keywords\">Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature</att>\n"
            + "        <att name=\"keywords_vocabulary\">NASA Global Change Master Directory (GCMD) Science Keywords</att>\n"
            + "        <att name=\"license\">These data are available free of charge under data policy of JPL PO.DAAC.</att>\n"
            + "        <att name=\"Metadata_Conventions\">Unidata Observation Dataset v1.0</att>\n"
            + "        <att name=\"metadata_link\">http://podaac.jpl.nasa.gov/ws/metadata/dataset/?format=iso&amp;shortName=MUR-JPL-L4-GLOB-v04.1</att>\n"
            + "        <att name=\"naming_authority\">org.ghrsst</att>\n"
            + "        <att name=\"netcdf_version_id\">4.1</att>\n"
            + "        <att name=\"northernmost_latitude\" type=\"float\">90.0</att>\n"
            + "        <att name=\"platform\">Terra, Aqua, "
            + (withGCOM ? "GCOM-W, " : "")
            + "MetOp-B, Buoys/Ships</att>\n"
            + // changes
            // periodically
            "        <att name=\"processing_level\">L4</att>\n"
            + "        <att name=\"product_version\">04.1nrt</att>\n"
            + "        <att name=\"project\">NASA Making Earth Science Data Records for Use in Research Environments (MEaSUREs) Program</att>\n"
            + "        <att name=\"publisher_email\">ghrsst-po@nceo.ac.uk</att>\n"
            + "        <att name=\"publisher_name\">GHRSST Project Office</att>\n"
            + "        <att name=\"publisher_url\">http://www.ghrsst.org</att>\n"
            + "        <att name=\"references\">http://podaac.jpl.nasa.gov/Multi-scale_Ultra-high_Resolution_MUR-SST</att>\n"
            + "        <att name=\"sensor\">MODIS, "
            + (withGCOM ? "AMSR2, " : "")
            + "AVHRR, in-situ</att>\n"
            + // changes
            // periodically
            "        <att name=\"source\">MODIS_T-JPL, MODIS_A-JPL, "
            + (withGCOM ? "AMSR2-REMSS, " : "")
            + "AVHRRMTB_G-NAVO, iQUAM-NOAA/NESDIS, Ice_Conc-OSISAF</att>\n"
            + // changes
            // periodically
            "        <att name=\"southernmost_latitude\" type=\"float\">-90.0</att>\n"
            + "        <att name=\"spatial_resolution\">0.01 degrees</att>\n"
            + "        <att name=\"standard_name_vocabulary\">NetCDF Climate and Forecast (CF) Metadata Convention</att>\n";
    po = results.indexOf(expected.substring(0, 80));
    Test.ensureEqual(results.substring(po, po + expected.length()), expected, "");

    // " <att name=\"start_time\">20171218T090000Z</att>\n" +
    // " <att name=\"stop_time\">20171218T090000Z</att>\n" +
    // " <att name=\"summary\">A merged, multi-sensor L4 Foundation SST analysis
    // product from JPL.</att>\n" +
    // " <att name=\"time_coverage_end\">20171218T210000Z</att>\n" +
    // " <att name=\"time_coverage_start\">20171217T210000Z</att>\n" +
    expected =
        "<att name=\"title\">Daily MUR SST, Interim near-real-time (nrt) product</att>\n"
            + "        <att name=\"uuid\">27665bc0-d5fc-11e1-9b23-0800200c9a66</att>\n"
            + "        <att name=\"westernmost_longitude\" type=\"float\">-180.0</att>\n"
            + "    </sourceAttributes -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"acknowledgement\">Please acknowledge the use of these data with the following statement:  These data were provided by JPL under support by NASA MEaSUREs program.</att>\n"
            + "        <att name=\"acknowledgment\">null</att>\n"
            + "        <att name=\"cdm_data_type\">Grid</att>\n"
            + "        <att name=\"Conventions\">CF-1.10, COARDS, ACDD-1.3</att>\n"
            + "        <att name=\"creator_type\">group</att>\n"
            + "        <att name=\"creator_url\">https://podaac.jpl.nasa.gov/dataset/MUR-JPL-L4-GLOB-v4.1</att>\n";
    po = results.indexOf(expected.substring(0, 80));
    Test.ensureEqual(results.substring(po, po + expected.length()), expected, "");

    // " <att name=\"date_created\">2017-12-20T02:37:39Z</att>\n" +
    expected =
        "<att name=\"easternmost_longitude\">null</att>\n"
            + "        <att name=\"file_quality_level\">null</att>\n"
            + "        <att name=\"infoUrl\">https://podaac.jpl.nasa.gov/ws/metadata/dataset/?format=iso&amp;shortName=MUR-JPL-L4-GLOB-v04.1</att>\n"
            + "        <att name=\"keywords\">1day, 1km, 2003-2014, analysed, analysed_sst, analysis_error, anomaly, area, based, binary, climatology, composite, cryosphere, daily, data, day, deviation, distribution, dt_1km_data, earth, Earth Science &gt; Cryosphere &gt; Sea Ice &gt; Ice Extent, Earth Science &gt; Land Surface &gt; Land Temperature &gt; Land Surface Temperature, Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature, Earth Science &gt; Oceans &gt; Sea Ice &gt; Ice Extent, error, estimated, extent, field, foundation, fraction, high, ice, ice distribution, identifier, interim, jet, jpl, laboratory, land, land_binary_mask, latitude, longitude, mask, most, multi, multi-scale, mur, near, near real time, near-real-time, nrt, ocean, oceans, over, period, product, propulsion, real, recent, resolution, scale, science, sea, sea/land, sea_ice_area_fraction, sea_ice_fraction, sea_surface_foundation_temperature, seasonal, sst, sst_anomaly, standard, statistics, surface, surface_temperature_anomaly, temperature, time, ultra, ultra-high</att>\n"
            + "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n"
            + "        <att name=\"Metadata_Conventions\">null</att>\n"
            + "        <att name=\"metadata_link\">https://podaac.jpl.nasa.gov/ws/metadata/dataset/?format=iso&amp;shortName=MUR-JPL-L4-GLOB-v04.1</att>\n"
            + "        <att name=\"netcdf_version_id\">null</att>\n"
            + "        <att name=\"northernmost_latitude\">null</att>\n"
            + "        <att name=\"publisher_type\">group</att>\n"
            + "        <att name=\"publisher_url\">https://www.ghrsst.org</att>\n"
            + "        <att name=\"references\">https://podaac.jpl.nasa.gov/dataset/MUR-JPL-L4-GLOB-v4.1</att>\n"
            + "        <att name=\"southernmost_latitude\">null</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"start_time\">null</att>\n"
            + "        <att name=\"stop_time\">null</att>\n"
            + "        <att name=\"summary\">A merged, multi-sensor L4 Foundation Sea Surface Temperature (SST) analysis product from Jet Propulsion Laboratory (JPL).</att>\n";
    po = results.indexOf(expected.substring(0, 80));
    Test.ensureEqual(results.substring(po, po + expected.length()), expected, "");

    // ignore
    /*
     * try {
     * expected = "<att name=\"testOutOfDate\">now-4days</att>\n";
     * po = results.indexOf(expected.substring(0, 25));
     * Test.ensureEqual(results.substring(po, po + expected.length()), expected,
     * "");
     * } catch (Throwable t3) {
     * String2.pressEnterToContinue(MustBe.throwableToString(t3) +
     * "nDays varies slightly.");
     * }
     */

    // " <att name=\"time_coverage_end\">2017-12-18T21:00:00Z</att>\n" +
    // " <att name=\"time_coverage_start\">2017-12-17T21:00:00Z</att>\n" +
    expected =
        "<att name=\"title\">Daily MUR SST, Interim near-real-time (nrt) product (1day), 0.01°, 2002-present</att>\n"
            + "        <att name=\"uuid\">null</att>\n"
            + "        <att name=\"westernmost_longitude\">null</att>\n"
            + "    </addAttributes>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>time</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"axis\">T</att>\n"
            + "            <att name=\"comment\">Nominal time of analyzed fields</att>\n"
            + "            <att name=\"long_name\">reference time of sst field</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"units\">seconds since 1981-01-01 00:00:00 UTC</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"units\">seconds since 1981-01-01T00:00:00Z</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>lat</sourceName>\n"
            + "        <destinationName>latitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_ChunkSizes\" type=\"uint\">17999</att>\n"
            + "            <att name=\"axis\">Y</att>\n"
            + "            <att name=\"comment\">geolocations inherited from the input data without correction</att>\n"
            + "            <att name=\"long_name\">latitude</att>\n"
            + "            <att name=\"standard_name\">latitude</att>\n"
            + "            <att name=\"units\">degrees_north</att>\n"
            + "            <att name=\"valid_max\" type=\"float\">90.0</att>\n"
            + "            <att name=\"valid_min\" type=\"float\">-90.0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_ChunkSizes\">null</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Latitude</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>lon</sourceName>\n"
            + "        <destinationName>longitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_ChunkSizes\" type=\"uint\">36000</att>\n"
            + "            <att name=\"axis\">X</att>\n"
            + "            <att name=\"comment\">geolocations inherited from the input data without correction</att>\n"
            + "            <att name=\"long_name\">longitude</att>\n"
            + "            <att name=\"standard_name\">longitude</att>\n"
            + "            <att name=\"units\">degrees_east</att>\n"
            + "            <att name=\"valid_max\" type=\"float\">180.0</att>\n"
            + "            <att name=\"valid_min\" type=\"float\">-180.0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_ChunkSizes\">null</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Longitude</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>analysed_sst</sourceName>\n"
            + "        <destinationName>analysed_sst</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_ChunkSizes\" type=\"uintList\">1 1023 2047</att>\n"
            + "            <att name=\"_FillValue\" type=\"short\">-32768</att>\n"
            + "            <att name=\"add_offset\" type=\"double\">298.15</att>\n"
            + "            <att name=\"comment\">Interim near-real-time (nrt) version using Multi-Resolution Variational Analysis (MRVA) method for interpolation; to be replaced by Final version</att>\n"
            + "            <att name=\"coordinates\">lon lat</att>\n"
            + "            <att name=\"long_name\">analysed sea surface temperature</att>\n"
            + "            <att name=\"scale_factor\" type=\"double\">0.001</att>\n"
            + "            <att name=\"source\">MODIS_T-JPL, MODIS_A-JPL, "
            + (withGCOM ? "AMSR2-REMSS, " : "")
            + "AVHRRMTB_G-NAVO, iQUAM-NOAA/NESDIS, Ice_Conc-OSISAF</att>\n"
            + "            <att name=\"standard_name\">sea_surface_foundation_temperature</att>\n"
            + "            <att name=\"units\">kelvin</att>\n"
            + "            <att name=\"valid_max\" type=\"short\">32767</att>\n"
            + "            <att name=\"valid_min\" type=\"short\">-32767</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_ChunkSizes\">null</att>\n"
            + "            <att name=\"add_offset\" type=\"double\">25.0</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"coordinates\">null</att>\n"
            + "            <att name=\"ioos_category\">Temperature</att>\n"
            + "            <att name=\"units\">degree_C</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>analysis_error</sourceName>\n"
            + "        <destinationName>analysis_error</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_ChunkSizes\" type=\"uintList\">1 1023 2047</att>\n"
            + "            <att name=\"_FillValue\" type=\"short\">-32768</att>\n"
            + "            <att name=\"add_offset\" type=\"double\">0.0</att>\n"
            + "            <att name=\"comment\">uncertainty in &quot;analysed_sst&quot;</att>\n"
            + "            <att name=\"coordinates\">lon lat</att>\n"
            + "            <att name=\"long_name\">estimated error standard deviation of analysed_sst</att>\n"
            + "            <att name=\"scale_factor\" type=\"double\">0.01</att>\n"
            + "            <att name=\"units\">kelvin</att>\n"
            + "            <att name=\"valid_max\" type=\"short\">32767</att>\n"
            + "            <att name=\"valid_min\" type=\"short\">0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_ChunkSizes\">null</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">5.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"coordinates\">null</att>\n"
            + "            <att name=\"ioos_category\">Statistics</att>\n"
            + "            <att name=\"units\">degree_C</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>mask</sourceName>\n"
            + "        <destinationName>mask</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_ChunkSizes\" type=\"uintList\">1 1447 2895</att>\n"
            + "            <att name=\"_FillValue\" type=\"short\">-128</att>\n"
            + "            <att name=\"_Unsigned\">false</att>\n"
            + "            <att name=\"comment\">mask can be used to further filter the data.</att>\n"
            + "            <att name=\"coordinates\">lon lat</att>\n"
            + "            <att name=\"flag_masks\" type=\"byteList\">1 2 4 8 16</att>\n"
            + "            <att name=\"flag_meanings\">open_sea land open_lake open_sea_with_ice_in_the_grid open_lake_with_ice_in_the_grid</att>\n"
            + "            <att name=\"long_name\">sea/land field composite mask</att>\n"
            + "            <att name=\"source\">GMT &quot;grdlandmask&quot;, ice flag from sea_ice_fraction data</att>\n"
            + "            <att name=\"valid_max\" type=\"byte\">31</att>\n"
            + "            <att name=\"valid_min\" type=\"byte\">1</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_ChunkSizes\">null</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">20.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"coordinates\">null</att>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "            <att name=\"standard_name\">land_binary_mask</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>sea_ice_fraction</sourceName>\n"
            + "        <destinationName>sea_ice_fraction</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_ChunkSizes\" type=\"uintList\">1 1447 2895</att>\n"
            + "            <att name=\"_FillValue\" type=\"short\">-128</att>\n"
            + "            <att name=\"_Unsigned\">false</att>\n"
            + "            <att name=\"add_offset\" type=\"double\">0.0</att>\n"
            + "            <att name=\"comment\">ice fraction is a dimensionless quantity between 0 and 1; it has been interpolated by a nearest neighbor approach.</att>\n"
            + "            <att name=\"coordinates\">lon lat</att>\n"
            + "            <att name=\"long_name\">sea ice area fraction</att>\n"
            + "            <att name=\"scale_factor\" type=\"double\">0.01</att>\n"
            + "            <att name=\"source\">EUMETSAT OSI-SAF, copyright EUMETSAT</att>\n"
            + "            <att name=\"standard_name\">sea_ice_area_fraction</att>\n"
            + "            <att name=\"valid_max\" type=\"byte\">100</att>\n"
            + "            <att name=\"valid_min\" type=\"byte\">0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_ChunkSizes\">null</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">1.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"coordinates\">null</att>\n"
            + "            <att name=\"ioos_category\">Ice Distribution</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>dt_1km_data</sourceName>\n"
            + "        <destinationName>dt_1km_data</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_ChunkSizes\" type=\"uintList\">1 1447 2895</att>\n"
            + "            <att name=\"_FillValue\" type=\"short\">-128</att>\n"
            + "            <att name=\"_Unsigned\">false</att>\n"
            + "            <att name=\"comment\">The grid value is hours between the analysis time and the most recent MODIS or VIIRS 1km L2P datum within 0.01 degrees from the grid point.  &quot;Fill value&quot; indicates absence of such 1km data at the grid point.</att>\n"
            + "            <att name=\"coordinates\">lon lat</att>\n"
            + "            <att name=\"long_name\">time to most recent 1km data</att>\n"
            + "            <att name=\"source\">MODIS and VIIRS pixels ingested by MUR</att>\n"
            + "            <att name=\"units\">hours</att>\n"
            + "            <att name=\"valid_max\" type=\"byte\">127</att>\n"
            + "            <att name=\"valid_min\" type=\"short\">-127</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_ChunkSizes\">null</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">200.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-200.0</att>\n"
            + "            <att name=\"coordinates\">null</att>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>sst_anomaly</sourceName>\n"
            + "        <destinationName>sst_anomaly</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_ChunkSizes\" type=\"uintList\">1 1023 2047</att>\n"
            + "            <att name=\"_FillValue\" type=\"short\">-32768</att>\n"
            + "            <att name=\"add_offset\" type=\"double\">0.0</att>\n"
            + "            <att name=\"comment\">anomaly reference to the day-of-year average between 2003 and 2014</att>\n"
            + "            <att name=\"coordinates\">lon lat</att>\n"
            + "            <att name=\"long_name\">SST anomaly from a seasonal SST climatology based on the MUR data over 2003-2014 period</att>\n"
            + "            <att name=\"scale_factor\" type=\"double\">0.001</att>\n"
            + "            <att name=\"units\">kelvin</att>\n"
            + "            <att name=\"valid_max\" type=\"short\">32767</att>\n"
            + "            <att name=\"valid_min\" type=\"short\">-32767</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_ChunkSizes\">null</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">3.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-3.0</att>\n"
            + "            <att name=\"coordinates\">null</att>\n"
            + "            <att name=\"ioos_category\">Temperature</att>\n"
            + "            <att name=\"standard_name\">surface_temperature_anomaly</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n";
    po = results.indexOf(expected.substring(0, 100));
    if (po < 0) String2.log("results=" + results + "\npo=" + po);
    Test.ensureEqual(results.substring(po), expected, "");
  }

  /** This tests a UInt16 variable in generateDatasetsXml. */
  @org.junit.jupiter.api.Test
  @TagThredds
  void testGenerateDatasetsXmlUInt16() throws Throwable {
    // String2.log("*** EDDGridFromDap.testGenerateDatasetsXmlUInt16");
    int language = 0;
    String results =
        EDDGridFromDap.generateDatasetsXml(
            "https://thredds.jpl.nasa.gov/thredds/dodsC/ncml_aggregation/OceanTemperature/modis/aqua/4um/4km/aggregate__MODIS_AQUA_L3_SST_MID_IR_8DAY_4KM_NIGHTTIME_v2019.0.ncml",
            null,
            null,
            new String[] {"time", "lat", "lon"},
            -1,
            null);
    // frequent small changes. I don't know why.
    results =
        results.replaceAll(
            "(2019\\-12\\-17|2022\\-03\\-23)", // flips between 2019-12-17 and
            // 2022-03-23
            "[DATE]");
    results = results.replaceAll("T\\d{2}:\\d{2}:\\d{2}.000Z", "T[TIME].000Z");
    results =
        results.replaceAll(
            "<att name=\"data_bins\" type=\"int\">\\d*</att>",
            "<att name=\"data_bins\" type=\"int\">[DATA_BINS]</att>");
    results =
        results.replaceAll(
            "<att name=\"data_maximum\" type=\"float\">.*</att>",
            "<att name=\"data_maximum\" type=\"float\">[DATA_MAXIMUM]</att>");
    results =
        results.replaceAll(
            "<att name=\"data_minimum\" type=\"float\">.*</att>",
            "<att name=\"data_minimum\" type=\"float\">[DATA_MINIMUM]</att>");
    results =
        results.replaceAll(
            "<att name=\"end_orbit_number\" type=\"int\">\\d*</att>",
            "<att name=\"end_orbit_number\" type=\"int\">[END_ORBIT_NUMBER]</att>");
    results =
        results.replaceAll(
            "<att name=\"start_orbit_number\" type=\"int\">\\d*</att>",
            "<att name=\"start_orbit_number\" type=\"int\">[START_ORBIT_NUMBER]</att>");
    results = results.replaceAll("20\\d{6}", "[DATE]");
    results =
        results.replaceAll(
            "<reloadEveryNMinutes>\\d*</reloadEveryNMinutes>",
            "<reloadEveryNMinutes>[RELOAD]</reloadEveryNMinutes>");
    results =
        results.replaceAll(
            "<att name=\"time_coverage_end\">.*</att>",
            "<att name=\"time_coverage_end\">[TCE]</att>");
    results =
        results.replaceAll(
            "<att name=\"time_coverage_start\">.*</att>",
            "<att name=\"time_coverage_start\">[TCS]</att>");
    results =
        results.replaceAll(
            "<att name=\"testOutOfDate\">now-\\d*days</att>",
            "<att name=\"testOutOfDate\">now-[N_DAYS]days</att>");

    String tDatasetID = "nasa_jpl_0d1b_74cd_7593";
    boolean newStuff = false; // appeared once then left
    String expected =
        "<dataset type=\"EDDGridFromDap\" datasetID=\""
            + tDatasetID
            + "\" active=\"true\">\n"
            + "    <sourceUrl>https://thredds.jpl.nasa.gov/thredds/dodsC/ncml_aggregation/OceanTemperature/modis/aqua/4um/4km/aggregate__MODIS_AQUA_L3_SST_MID_IR_8DAY_4KM_NIGHTTIME_v2019.0.ncml</sourceUrl>\n"
            + "    <reloadEveryNMinutes>[RELOAD]</reloadEveryNMinutes>\n"
            + // changes
            "    <!-- sourceAttributes>\n"
            + "        <att name=\"_lastModified\">[DATE]T[TIME].000Z</att>\n"
            + // 2020-08-20 this
            // change by 5
            // seconds. why?
            // few seconds
            // changes often!
            // 2021-11-16 was
            // 2020-09-21! We're
            // going backwards!
            // //2022-08-31 now
            // 2020-01-06, then
            // 2019-12-17
            "        <att name=\"cdm_data_type\">grid</att>\n"
            + "        <att name=\"Conventions\">CF-1.6 ACDD-1.3</att>\n"
            + "        <att name=\"creator_email\">data@oceancolor.gsfc.nasa.gov</att>\n"
            + "        <att name=\"creator_name\">NASA/GSFC/OBPG</att>\n"
            + "        <att name=\"creator_url\">https://oceandata.sci.gsfc.nasa.gov</att>\n"
            + "        <att name=\"data_bins\" type=\"int\">[DATA_BINS]</att>\n"
            + // changes (often?)
            "        <att name=\"data_maximum\" type=\"float\">[DATA_MAXIMUM]</att>\n"
            + // changes
            // (often?)
            "        <att name=\"data_minimum\" type=\"float\">[DATA_MINIMUM]</att>\n"
            + // changes
            // (often?)
            "        <att name=\"date_created\">[DATE]T[TIME].000Z</att>\n"
            + // few seconds changes
            // (often?)
            // 2021-11-16 was
            // 2020-09-21! We're
            // going backwards!
            "        <att name=\"easternmost_longitude\" type=\"float\">180.0</att>\n"
            + "        <att name=\"end_orbit_number\" type=\"int\">[END_ORBIT_NUMBER]</att>\n"
            + // changes
            // often?
            "        <att name=\"geospatial_lat_max\" type=\"float\">90.0</att>\n"
            + "        <att name=\"geospatial_lat_min\" type=\"float\">-90.0</att>\n"
            + "        <att name=\"geospatial_lat_resolution\" type=\"float\">0.041666668</att>\n"
            + "        <att name=\"geospatial_lat_units\">degrees_north</att>\n"
            + "        <att name=\"geospatial_lon_max\" type=\"float\">180.0</att>\n"
            + "        <att name=\"geospatial_lon_min\" type=\"float\">-180.0</att>\n"
            + "        <att name=\"geospatial_lon_resolution\" type=\"float\">0.041666668</att>\n"
            + "        <att name=\"geospatial_lon_units\">degrees_east</att>\n"
            + "        <att name=\"history\">l3mapgen par=AQUA_MODIS.[DATE]_[DATE].L3m.8D.SST4.sst4.4km.nc.param</att>\n"
            + "        <att name=\"id\">AQUA_MODIS.[DATE]_[DATE].L3b.8D.SST4.nc/L3/AQUA_MODIS.[DATE]_[DATE].L3b.8D.SST4.nc</att>\n"
            + "        <att name=\"institution\">NASA Goddard Space Flight Center, Ocean Ecology Laboratory, Ocean Biology Processing Group</att>\n"
            + "        <att name=\"instrument\">MODIS</att>\n"
            + "        <att name=\"l2_flag_names\">LAND,~HISOLZEN</att>\n"
            + "        <att name=\"latitude_step\" type=\"float\">0.041666668</att>\n"
            + "        <att name=\"latitude_units\">degrees_north</att>\n"
            + "        <att name=\"license\">https://science.nasa.gov/earth-science/earth-science-data/data-information-policy/</att>\n"
            + "        <att name=\"longitude_step\" type=\"float\">0.041666668</att>\n"
            + "        <att name=\"longitude_units\">degrees_east</att>\n"
            + "        <att name=\"map_projection\">Equidistant Cylindrical</att>\n"
            + "        <att name=\"measure\">Mean</att>\n"
            + "        <att name=\"naming_authority\">gov.nasa.gsfc.sci.oceandata</att>\n"
            + "        <att name=\"northernmost_latitude\" type=\"float\">90.0</att>\n"
            + "        <att name=\"number_of_columns\" type=\"int\">8640</att>\n"
            + "        <att name=\"number_of_lines\" type=\"int\">4320</att>\n"
            + "        <att name=\"platform\">Aqua</att>\n"
            + "        <att name=\"processing_control_input_parameters_apply_pal\">1</att>\n"
            + "        <att name=\"processing_control_input_parameters_central_meridian\">-999</att>\n"
            + "        <att name=\"processing_control_input_parameters_deflate\">4</att>\n"
            + "        <att name=\"processing_control_input_parameters_east\">180.000</att>\n"
            + "        <att name=\"processing_control_input_parameters_fudge\">1.0</att>\n"
            + "        <att name=\"processing_control_input_parameters_full_latlon\">yes</att>\n"
            + "        <att name=\"processing_control_input_parameters_ifile\">AQUA_MODIS.[DATE]_[DATE].L3b.8D.SST4.nc</att>\n"
            + "        <att name=\"processing_control_input_parameters_interp\">area</att>\n"
            + "        <att name=\"processing_control_input_parameters_land\">$OCDATAROOT/common/landmask_GMT15ARC.nc</att>\n"
            + "        <att name=\"processing_control_input_parameters_mask_land\">no</att>\n"
            + "        <att name=\"processing_control_input_parameters_north\">90.000</att>\n"
            + "        <att name=\"processing_control_input_parameters_num_cache\">500</att>\n"
            + "        <att name=\"processing_control_input_parameters_ofile\">AQUA_MODIS.[DATE]_[DATE].L3m.8D.SST4.sst4.4km.nc</att>\n"
            + "        <att name=\"processing_control_input_parameters_ofile_product_tag\">PRODUCT</att>\n"
            + "        <att name=\"processing_control_input_parameters_oformat\">2</att>\n"
            + "        <att name=\"processing_control_input_parameters_oformat2\">png</att>\n"
            + "        <att name=\"processing_control_input_parameters_par\">AQUA_MODIS.[DATE]_[DATE].L3m.8D.SST4.sst4.4km.nc.param</att>\n"
            + "        <att name=\"processing_control_input_parameters_product\">sst4</att>\n"
            + "        <att name=\"processing_control_input_parameters_product_rgb\">rhos_645,rhos_555,rhos_469</att>\n"
            + "        <att name=\"processing_control_input_parameters_projection\">smi</att>\n"
            + "        <att name=\"processing_control_input_parameters_pversion\">R2019.0</att>\n"
            + "        <att name=\"processing_control_input_parameters_quiet\">false</att>\n"
            + "        <att name=\"processing_control_input_parameters_resolution\">4km</att>\n"
            + (newStuff
                ? "        <att name=\"processing_control_input_parameters_rgb_land\">160,82,45</att>\n"
                : "")
            + "        <att name=\"processing_control_input_parameters_south\">-90.000</att>\n"
            + "        <att name=\"processing_control_input_parameters_suite\">SST4</att>\n"
            + "        <att name=\"processing_control_input_parameters_threshold\">0</att>\n"
            + "        <att name=\"processing_control_input_parameters_trimNSEW\">yes</att>\n"
            + "        <att name=\"processing_control_input_parameters_use_quality\">yes</att>\n"
            + "        <att name=\"processing_control_input_parameters_use_rgb\">no</att>\n"
            + "        <att name=\"processing_control_input_parameters_use_transparency\">no</att>\n"
            + "        <att name=\"processing_control_input_parameters_west\">-180.000</att>\n"
            + (newStuff
                ? "        <att name=\"processing_control_input_parameters_write_projtext\">no</att>\n"
                : "")
            + "        <att name=\"processing_control_l2_flag_names\">LAND,~HISOLZEN</att>\n"
            + "        <att name=\"processing_control_software_name\">l3mapgen</att>\n"
            + "        <att name=\"processing_control_software_version\">"
            + (newStuff ? "2.3.0-V2021.1" : "2.2.0-V2019.4")
            + "</att>\n"
            + // changes periodically
            "        <att name=\"processing_control_source\">AQUA_MODIS.[DATE]_[DATE].L3b.8D.SST4.nc</att>\n"
            + "        <att name=\"processing_level\">L3 Mapped</att>\n"
            + "        <att name=\"processing_version\">R2019.0</att>\n"
            + "        <att name=\"product_name\">AQUA_MODIS.[DATE]_[DATE].L3m.8D.SST4.sst4.4km.nc</att>\n"
            + "        <att name=\"project\">Ocean Biology Processing Group (NASA/GSFC/OBPG)</att>\n"
            + "        <att name=\"publisher_email\">data@oceancolor.gsfc.nasa.gov</att>\n"
            + "        <att name=\"publisher_name\">NASA/GSFC/OBPG</att>\n"
            + "        <att name=\"publisher_url\">https://oceandata.sci.gsfc.nasa.gov</att>\n"
            + "        <att name=\"southernmost_latitude\" type=\"float\">-90.0</att>\n"
            + "        <att name=\"spatialResolution\">4.64 km</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v36</att>\n"
            + "        <att name=\"start_date\">2002-07-04 UTC</att>\n"
            + "        <att name=\"start_orbit_number\" type=\"int\">[START_ORBIT_NUMBER]</att>\n"
            + "        <att name=\"start_time\">00:00:00 UTC</att>\n"
            + "        <att name=\"stop_date\">2022-03-30 UTC</att>\n"
            + // changes
            "        <att name=\"stop_time\">23:59:59 UTC</att>\n"
            + "        <att name=\"suggested_image_scaling_applied\">No</att>\n"
            + "        <att name=\"suggested_image_scaling_maximum\" type=\"float\">45.0</att>\n"
            + "        <att name=\"suggested_image_scaling_minimum\" type=\"float\">-2.0</att>\n"
            + "        <att name=\"suggested_image_scaling_type\">LINEAR</att>\n"
            + "        <att name=\"sw_point_latitude\" type=\"float\">-89.979164</att>\n"
            + "        <att name=\"sw_point_longitude\" type=\"float\">-179.97917</att>\n"
            + "        <att name=\"temporal_range\">8-day</att>\n"
            + // 2020-09-21 6-day?! was and
            // should be 8-day. I
            // reported
            // it to podaac
            "        <att name=\"time_coverage_end\">[TCE]</att>\n"
            + "        <att name=\"time_coverage_start\">[TCS]</att>\n"
            + "        <att name=\"title\">MODISA Level-3 Standard Mapped Image</att>\n"
            + "        <att name=\"westernmost_longitude\" type=\"float\">-180.0</att>\n"
            + "    </sourceAttributes -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cdm_data_type\">Grid</att>\n"
            + "        <att name=\"Conventions\">CF-1.10, ACDD-1.3, COARDS</att>\n"
            + "        <att name=\"creator_type\">group</att>\n"
            + "        <att name=\"data_bins\">null</att>\n"
            + "        <att name=\"data_maximum\">null</att>\n"
            + "        <att name=\"data_minimum\">null</att>\n"
            + "        <att name=\"easternmost_longitude\">null</att>\n"
            + "        <att name=\"end_orbit_number\">null</att>\n"
            + "        <att name=\"infoUrl\">https://podaac.jpl.nasa.gov/dataset/MODIS_AQUA_L3_SST_MID_IR_8DAY_4KM_NIGHTTIME_v2019.0</att>\n"
            + "        <att name=\"institution\">NASA/GSFC OBPG</att>\n"
            + "        <att name=\"keywords\">4km, 4um, 8day, aqua, biology, center, color, data, earth, Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature, flag, flag_values, flight, goddard, group, gsfc, image, imaging, infrared, L3, latitude, level, level-3, levels, longitude, mapped, mid, moderate, modis, modisa, nasa, night, nighttime, obpg, ocean, oceans, processing, qual_sst4, quality, resolution, science, sea, sea_surface_temperature, smi, space, spectroradiometer, sst, sst4, standard, surface, temperature, time, v2019.0, values</att>\n"
            + "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n"
            + "        <att name=\"latitude_step\">null</att>\n"
            + "        <att name=\"latitude_units\">null</att>\n"
            + "        <att name=\"longitude_step\">null</att>\n"
            + "        <att name=\"longitude_units\">null</att>\n"
            + "        <att name=\"northernmost_latitude\">null</att>\n"
            + "        <att name=\"number_of_columns\">null</att>\n"
            + "        <att name=\"number_of_lines\">null</att>\n"
            + "        <att name=\"publisher_type\">group</att>\n"
            + "        <att name=\"southernmost_latitude\">null</att>\n"
            + "        <att name=\"start_date\">null</att>\n"
            + "        <att name=\"start_orbit_number\">null</att>\n"
            + "        <att name=\"start_time\">null</att>\n"
            + "        <att name=\"stop_date\">null</att>\n"
            + "        <att name=\"stop_time\">null</att>\n"
            + "        <att name=\"suggested_image_scaling_applied\">null</att>\n"
            + "        <att name=\"suggested_image_scaling_maximum\">null</att>\n"
            + "        <att name=\"suggested_image_scaling_minimum\">null</att>\n"
            + "        <att name=\"suggested_image_scaling_type\">null</att>\n"
            + "        <att name=\"summary\">Moderate Resolution Imaging Spectroradiometer on Aqua (MODISA) Level-3 Standard Mapped Image (MODIS AQUA L3 Sea Surface Temperature (SST) MID InfraRed (IR) 8DAY 4KM NIGHTTIME v2019.0)</att>\n"
            + "        <att name=\"sw_point_latitude\">null</att>\n"
            + "        <att name=\"sw_point_longitude\">null</att>\n"
            +
            // " <att name=\"testOutOfDate\">now-[N_DAYS]days</att>\n" + //2020-10-21 comes
            // and goes
            "        <att name=\"title\">MODISA L3 SMI, MODIS AQUA L3 SST MID IR 8DAY 4KM NIGHTTIME v2019.0 [time][lat][lon], 0.041666668°, 2002-2022</att>\n"
            + // 2021-05-03 changes: -present or current year depending on when they last
            // updated
            "        <att name=\"westernmost_longitude\">null</att>\n"
            + "    </addAttributes>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>time</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"axis\">timeAxis</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"units\">days since 2002-01-01</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"long_name\">Time</att>\n"
            + "            <att name=\"units\">days since 2002-01-01T00:00:00Z</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>lat</sourceName>\n"
            + "        <destinationName>latitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">-999.0</att>\n"
            + "            <att name=\"axis\">Y</att>\n"
            + "            <att name=\"long_name\">Latitude</att>\n"
            + "            <att name=\"standard_name\">latitude</att>\n"
            + "            <att name=\"units\">degrees_north</att>\n"
            + "            <att name=\"valid_max\" type=\"float\">90.0</att>\n"
            + "            <att name=\"valid_min\" type=\"float\">-90.0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\">null</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>lon</sourceName>\n"
            + "        <destinationName>longitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">-999.0</att>\n"
            + "            <att name=\"axis\">X</att>\n"
            + "            <att name=\"long_name\">Longitude</att>\n"
            + "            <att name=\"standard_name\">longitude</att>\n"
            + "            <att name=\"units\">degrees_east</att>\n"
            + "            <att name=\"valid_max\" type=\"float\">180.0</att>\n"
            + "            <att name=\"valid_min\" type=\"float\">-180.0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\">null</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>sst4</sourceName>\n"
            + "        <destinationName>sst4</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_ChunkSizes\" type=\"intList\">44 87</att>\n"
            + "            <att name=\"_FillValue\" type=\"short\">-32767</att>\n"
            + "            <att name=\"add_offset\" type=\"float\">0.0</att>\n"
            + "            <att name=\"display_max\" type=\"float\">45.0</att>\n"
            + "            <att name=\"display_min\" type=\"float\">-2.0</att>\n"
            + "            <att name=\"display_scale\">linear</att>\n"
            + "            <att name=\"long_name\">4um Sea Surface Temperature</att>\n"
            + "            <att name=\"scale_factor\" type=\"float\">0.005</att>\n"
            + "            <att name=\"standard_name\">sea_surface_temperature</att>\n"
            + "            <att name=\"units\">degree_C</att>\n"
            + "            <att name=\"valid_max\" type=\"short\">10000</att>\n"
            + "            <att name=\"valid_min\" type=\"short\">-1000</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_ChunkSizes\">null</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"display_max\">null</att>\n"
            + "            <att name=\"display_min\">null</att>\n"
            + "            <att name=\"display_scale\">null</att>\n"
            + "            <att name=\"ioos_category\">Temperature</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>qual_sst4</sourceName>\n"
            + "        <destinationName>qual_sst4</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_ChunkSizes\" type=\"intList\">44 87</att>\n"
            + "            <att name=\"_FillValue\" type=\"ushort\">65535</att>\n"
            + // important
            // test.
            // originally
            // short = -1
            "            <att name=\"_Unsigned\">true</att>\n"
            + // important test, needs to be left
            // in so EDV
            // paType is
            // correct
            "            <att name=\"long_name\">Quality Levels, Sea Surface Temperature</att>\n"
            + "            <att name=\"standard_name\">flag_values</att>\n"
            + "            <att name=\"valid_max\">5</att>\n"
            + // 2020-09-28 had type=ubyte (good),
            // now nothing (bad)
            "            <att name=\"valid_min\">0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_ChunkSizes\">null</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // ensure it is ready-to-use by making a dataset from it
    EDD.deleteCachedDatasetInfo(tDatasetID);
    EDD edd = EDDGridFromDap.oneFromXmlFragment(null, results);

    String tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            edd.className() + "UInt16",
            ".dds");
    results = File2.readFromFile88591(EDStatic.fullTestCacheDirectory + tName)[1];
    results = results.replaceAll("time = \\d{3,4}", "time = ###");
    expected =
        "Dataset {\n"
            + "  Float64 time[time = ###];\n"
            + "  Float64 latitude[latitude = 4320];\n"
            + "  Float64 longitude[longitude = 8640];\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 sst4[time = ###][latitude = 4320][longitude = 8640];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = ###];\n"
            + "      Float64 latitude[latitude = 4320];\n"
            + "      Float64 longitude[longitude = 8640];\n"
            + "  } sst4;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      UInt16 qual_sst4[time = ###][latitude = 4320][longitude = 8640];\n"
            + // UInt16
            // is
            // important
            // test
            "    MAPS:\n"
            + "      Float64 time[time = ###];\n"
            + "      Float64 latitude[latitude = 4320];\n"
            + "      Float64 longitude[longitude = 8640];\n"
            + "  } qual_sst4;\n"
            + "} nasa_jpl_0d1b_74cd_7593;\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            edd.className() + "UInt16",
            ".das");
    results = File2.readFromFile88591(EDStatic.fullTestCacheDirectory + tName)[1];
    results = results.replaceAll("\\d{8}_\\d{8}", "[DATE_DATE]");
    results =
        results.replaceAll(
            "(2019\\-12\\-17|2022\\-03\\-23)", // flips between 2019-12-17 and
            // 2022-03-23
            "[DATE]");
    results =
        results.replaceAll("\\[DATE\\]T\\d{2}:\\d{2}:\\d{2}.000Z", "[DATE]T[TIME_VARIES].000Z");
    results =
        results.replaceAll("20\\d{2}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z http", "[DATE_TIME] http");
    expected =
        "Attributes {\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range 1.0257408e+9, 1.6485984e+9;\n"
            + // changes
            "    String axis \"T\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float64 actual_range -89.97916810599999, 89.979166667;\n"
            + "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "    Float64 valid_max 90.0;\n"
            + "    Float64 valid_min -90.0;\n"
            + "  }\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range -179.979166667, 179.979169546;\n"
            + "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "    Float64 valid_max 180.0;\n"
            + "    Float64 valid_min -180.0;\n"
            + "  }\n"
            + "  sst4 {\n"
            + "    Float32 _FillValue -163.835;\n"
            + "    Float64 colorBarMaximum 32.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Temperature\";\n"
            + "    String long_name \"4um Sea Surface Temperature\";\n"
            + "    String standard_name \"sea_surface_temperature\";\n"
            + "    String units \"degree_C\";\n"
            + "    Float32 valid_max 50.0;\n"
            + "    Float32 valid_min -5.0;\n"
            + "  }\n"
            + "  qual_sst4 {\n"
            + // important test that _Unsigned was removed from combinedAtta
            "    UInt16 _FillValue 65535;\n"
            + // UInt16 is important test
            "    Float64 colorBarMaximum 150.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Quality Levels, Sea Surface Temperature\";\n"
            + "    String standard_name \"flag_values\";\n"
            + "    UInt16 valid_max 5;\n"
            + "    UInt16 valid_min 0;\n"
            + "  }\n"
            + "  NC_GLOBAL {\n"
            + "    String _lastModified \"[DATE]T[TIME_VARIES].000Z\";\n"
            + // 2019-12-17T13:42:11.000Z
            // value from
            // source
            // drifts a little each
            // time I run this.
            // 2021-11-16 time jumped
            // backwards from
            // 2020-09-21!
            "    String cdm_data_type \"Grid\";\n"
            + "    String Conventions \"CF-1.10, ACDD-1.3, COARDS\";\n"
            + "    String creator_email \"data@oceancolor.gsfc.nasa.gov\";\n"
            + "    String creator_name \"NASA/GSFC/OBPG\";\n"
            + "    String creator_type \"group\";\n"
            + "    String creator_url \"https://oceandata.sci.gsfc.nasa.gov\";\n"
            + "    String date_created \"[DATE]T[TIME_VARIES].000Z\";\n"
            + // changes sometimes
            "    Float64 Easternmost_Easting 179.979169546;\n"
            + "    Float64 geospatial_lat_max 89.979166667;\n"
            + "    Float64 geospatial_lat_min -89.97916810599999;\n"
            + "    Float64 geospatial_lat_resolution 0.041666667;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max 179.979169546;\n"
            + "    Float64 geospatial_lon_min -179.979166667;\n"
            + "    Float64 geospatial_lon_resolution 0.041666667;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    String history \"l3mapgen par=AQUA_MODIS.[DATE_DATE].L3m.8D.SST4.sst4.4km.nc.param\n"
            + "[DATE_TIME] https://thredds.jpl.nasa.gov/thredds/dodsC/ncml_aggregation/OceanTemperature/modis/aqua/4um/4km/aggregate__MODIS_AQUA_L3_SST_MID_IR_8DAY_4KM_NIGHTTIME_v2019.0.ncml\n"
            + "[DATE_TIME] http://127.0.0.1:8080/cwexperimental/griddap/nasa_jpl_0d1b_74cd_7593.das\";\n"
            + "    String id \"AQUA_MODIS.[DATE_DATE].L3b.8D.SST4.nc/L3/AQUA_MODIS.[DATE_DATE].L3b.8D.SST4.nc\";\n"
            + "    String infoUrl \"https://podaac.jpl.nasa.gov/dataset/MODIS_AQUA_L3_SST_MID_IR_8DAY_4KM_NIGHTTIME_v2019.0\";\n"
            + "    String institution \"NASA/GSFC OBPG\";\n"
            + "    String instrument \"MODIS\";\n"
            + "    String keywords \"4km, 4um, 8day, aqua, biology, center, color, data, earth, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, flag, flag_values, flight, goddard, group, gsfc, image, imaging, infrared, L3, latitude, level, level-3, levels, longitude, mapped, mid, moderate, modis, modisa, nasa, night, nighttime, obpg, ocean, oceans, processing, qual_sst4, quality, resolution, science, sea, sea_surface_temperature, smi, space, spectroradiometer, sst, sst4, standard, surface, temperature, time, v2019.0, values\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String l2_flag_names \"LAND,~HISOLZEN\";\n"
            + "    String license \"https://science.nasa.gov/earth-science/earth-science-data/data-information-policy/\";\n"
            + "    String map_projection \"Equidistant Cylindrical\";\n"
            + "    String measure \"Mean\";\n"
            + "    String naming_authority \"gov.nasa.gsfc.sci.oceandata\";\n"
            + "    Float64 Northernmost_Northing 89.979166667;\n"
            + "    String platform \"Aqua\";\n"
            + "    String processing_control_input_parameters_apply_pal \"1\";\n"
            + "    String processing_control_input_parameters_central_meridian \"-999\";\n"
            + "    String processing_control_input_parameters_deflate \"4\";\n"
            + "    String processing_control_input_parameters_east \"180.000\";\n"
            + "    String processing_control_input_parameters_fudge \"1.0\";\n"
            + "    String processing_control_input_parameters_full_latlon \"yes\";\n"
            + "    String processing_control_input_parameters_ifile \"AQUA_MODIS.[DATE_DATE].L3b.8D.SST4.nc\";\n"
            + "    String processing_control_input_parameters_interp \"area\";\n"
            + "    String processing_control_input_parameters_land \"$OCDATAROOT/common/landmask_GMT15ARC.nc\";\n"
            + "    String processing_control_input_parameters_mask_land \"no\";\n"
            + "    String processing_control_input_parameters_north \"90.000\";\n"
            + "    String processing_control_input_parameters_num_cache \"500\";\n"
            + "    String processing_control_input_parameters_ofile \"AQUA_MODIS.[DATE_DATE].L3m.8D.SST4.sst4.4km.nc\";\n"
            + "    String processing_control_input_parameters_ofile_product_tag \"PRODUCT\";\n"
            + "    String processing_control_input_parameters_oformat \"2\";\n"
            + "    String processing_control_input_parameters_oformat2 \"png\";\n"
            + "    String processing_control_input_parameters_par \"AQUA_MODIS.[DATE_DATE].L3m.8D.SST4.sst4.4km.nc.param\";\n"
            + "    String processing_control_input_parameters_product \"sst4\";\n"
            + "    String processing_control_input_parameters_product_rgb \"rhos_645,rhos_555,rhos_469\";\n"
            + "    String processing_control_input_parameters_projection \"smi\";\n"
            + "    String processing_control_input_parameters_pversion \"R2019.0\";\n"
            + "    String processing_control_input_parameters_quiet \"false\";\n"
            + "    String processing_control_input_parameters_resolution \"4km\";\n"
            + (newStuff
                ? "    String processing_control_input_parameters_rgb_land \"160,82,45\";\n"
                : "")
            + "    String processing_control_input_parameters_south \"-90.000\";\n"
            + "    String processing_control_input_parameters_suite \"SST4\";\n"
            + "    String processing_control_input_parameters_threshold \"0\";\n"
            + "    String processing_control_input_parameters_trimNSEW \"yes\";\n"
            + "    String processing_control_input_parameters_use_quality \"yes\";\n"
            + "    String processing_control_input_parameters_use_rgb \"no\";\n"
            + "    String processing_control_input_parameters_use_transparency \"no\";\n"
            + "    String processing_control_input_parameters_west \"-180.000\";\n"
            + (newStuff
                ? "    String processing_control_input_parameters_write_projtext \"no\";\n"
                : "")
            + "    String processing_control_l2_flag_names \"LAND,~HISOLZEN\";\n"
            + "    String processing_control_software_name \"l3mapgen\";\n"
            + "    String processing_control_software_version \""
            + (newStuff ? "2.3.0-V2021.1" : "2.2.0-V2019.4")
            + "\";\n"
            + // changes sometimes
            "    String processing_control_source \"AQUA_MODIS.[DATE_DATE].L3b.8D.SST4.nc\";\n"
            + "    String processing_level \"L3 Mapped\";\n"
            + "    String processing_version \"R2019.0\";\n"
            + "    String product_name \"AQUA_MODIS.[DATE_DATE].L3m.8D.SST4.sst4.4km.nc\";\n"
            + "    String project \"Ocean Biology Processing Group (NASA/GSFC/OBPG)\";\n"
            + "    String publisher_email \"data@oceancolor.gsfc.nasa.gov\";\n"
            + "    String publisher_name \"NASA/GSFC/OBPG\";\n"
            + "    String publisher_type \"group\";\n"
            + "    String publisher_url \"https://oceandata.sci.gsfc.nasa.gov\";\n"
            + "    String sourceUrl \"https://thredds.jpl.nasa.gov/thredds/dodsC/ncml_aggregation/OceanTemperature/modis/aqua/4um/4km/aggregate__MODIS_AQUA_L3_SST_MID_IR_8DAY_4KM_NIGHTTIME_v2019.0.ncml\";\n"
            + "    Float64 Southernmost_Northing -89.97916810599999;\n"
            + "    String spatialResolution \"4.64 km\";\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v36\";\n"
            + "    String summary \"Moderate Resolution Imaging Spectroradiometer on Aqua (MODISA) Level-3 Standard Mapped Image (MODIS AQUA L3 Sea Surface Temperature (SST) MID InfraRed (IR) 8DAY 4KM NIGHTTIME v2019.0)\";\n"
            + "    String temporal_range \"8-day\";\n"
            + // 2020-09-21 6-day?! was and should be
            // 8-day. I reported it
            // to
            // podaac
            // " String testOutOfDate \"now-[N_DAYS]days\";\n" + //2020-10-21 comes and goes
            "    String time_coverage_end \"2022-03-30T00:00:00Z\";\n"
            + // 2020-10-02 varies
            // 2022-02-18 was wrong:
            // I
            // reported to podaac@...
            // Subject="Incorrect time
            // values and _FillValue"
            "    String time_coverage_start \"2002-07-04T00:00:00Z\";\n"
            + "    String title \"MODISA L3 SMI, MODIS AQUA L3 SST MID IR 8DAY 4KM NIGHTTIME v2019.0 [time][lat][lon], 0.041666668°, 2002-2022\";\n"
            + // 2021-05-03 changes between -present and current year
            "    Float64 Westernmost_Easting -179.979166667;\n"
            + "  }\n"
            + "}\n";
    try {
      Test.ensureEqual(results, expected, "results=\n" + results);
    } catch (Exception e) {
      Test.knownProblem(
          "time_coverage_end calculated from data source is WRONG!\n"
              + // I don't know
              // what it
              // should be
              "2020-08-25 I emailed podaac@... Subject=\"Incorrect time values and _FillValue\"",
          e);
    }
  }

  /**
   * This test UInt16 data.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagThredds
  void testUInt16Dap() throws Throwable {
    // String2.log("\n*** EDDGridFromDap.testUInt16");
    int language = 0;
    // testVerboseOn();
    String name, tName, results, tResults, expected, userDapQuery, url;
    String baseUrl =
        "https://thredds.jpl.nasa.gov/thredds/dodsC/ncml_aggregation/OceanTemperature/modis/aqua/4um/4km/aggregate__MODIS_AQUA_L3_SST_MID_IR_8DAY_4KM_NIGHTTIME_v2019.0.ncml";
    try {

      // .dds from source
      url = baseUrl + ".dds";
      results = SSR.getUrlResponseStringUnchanged(url);
      results = results.replaceAll("\\[time = \\d*\\]", "[time = [N_TIME]]");
      expected =
          "Dataset {\n"
              + "    Float64 lon[lon = 8640];\n"
              + "    Byte palette[rgb = 3][eightbitcolor = 256];\n"
              + "    Float64 lat[lat = 4320];\n"
              + "    Int32 time[time = [N_TIME]];\n"
              + // changes
              "    Grid {\n"
              + "     ARRAY:\n"
              + "        Int16 sst4[time = [N_TIME]][lat = 4320][lon = 8640];\n"
              + "     MAPS:\n"
              + "        Int32 time[time = [N_TIME]];\n"
              + "        Float64 lat[lat = 4320];\n"
              + "        Float64 lon[lon = 8640];\n"
              + "    } sst4;\n"
              + "    Grid {\n"
              + "     ARRAY:\n"
              + "        UInt16 qual_sst4[time = [N_TIME]][lat = 4320][lon = 8640];\n"
              + "     MAPS:\n"
              + "        Int32 time[time = [N_TIME]];\n"
              + "        Float64 lat[lat = 4320];\n"
              + "        Float64 lon[lon = 8640];\n"
              + "    } qual_sst4;\n"
              + "} ncml_aggregation/OceanTemperature/modis/aqua/4um/4km/aggregate__MODIS_AQUA_L3_SST_MID_IR_8DAY_4KM_NIGHTTIME_v2019.0.ncml;\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // .das from source
      url = baseUrl + ".das";
      results = SSR.getUrlResponseStringUnchanged(url);
      results =
          results.replaceAll("String date_created \".*\";", "String date_created \"[TIME]\";");
      results =
          results.replaceAll(
              "String time_coverage_start \".*\";", "String time_coverage_start \"[TIME]\";");
      results =
          results.replaceAll(
              "String time_coverage_end \".*\";", "String time_coverage_end \"[TIME]\";");
      results =
          results.replaceAll("Int32 start_orbit_number \\d*;", "Int32 start_orbit_number [SON];");
      results = results.replaceAll("Int32 end_orbit_number \\d*;", "Int32 end_orbit_number [EON];");
      results = results.replaceAll("Int32 data_bins \\d*;", "Int32 data_bins [DB];");
      results = results.replaceAll("Float32 data_minimum .*;", "Float32 data_minimum [DM];");
      results = results.replaceAll("Float32 data_maximum .*;", "Float32 data_maximum [DM];");
      results = results.replaceAll("20\\d{6}", "[DATE]");
      results =
          results.replaceAll("String _lastModified \".*\";", "String _lastModified \"[TIME]\";");
      results =
          results.replaceAll(
              "String stop_date \".* UTC\";", "String stop_date \"[STOP_DATE] UTC\";");

      expected =
          "Attributes {\n"
              + "    lon {\n"
              + "        String long_name \"Longitude\";\n"
              + "        String units \"degrees_east\";\n"
              + "        String standard_name \"longitude\";\n"
              + "        Float32 _FillValue -999.0;\n"
              + "        Float32 valid_min -180.0;\n"
              + "        Float32 valid_max 180.0;\n"
              + "        String axis \"X\";\n"
              + "    }\n"
              + "    palette {\n"
              + "        String _Unsigned \"true\";\n"
              + "        Int16 _FillValue -1;\n"
              + "    }\n"
              + "    lat {\n"
              + "        String long_name \"Latitude\";\n"
              + "        String units \"degrees_north\";\n"
              + "        String standard_name \"latitude\";\n"
              + "        Float32 _FillValue -999.0;\n"
              + "        Float32 valid_min -90.0;\n"
              + "        Float32 valid_max 90.0;\n"
              + "        String axis \"Y\";\n"
              + "    }\n"
              + "    time {\n"
              + "        String standard_name \"time\";\n"
              + "        String axis \"timeAxis\";\n"
              + "        String units \"days since 2002-01-01\";\n"
              + "    }\n"
              + "    sst4 {\n"
              + "        String long_name \"4um Sea Surface Temperature\";\n"
              + "        Float32 scale_factor 0.005;\n"
              + "        Float32 add_offset 0.0;\n"
              + "        String units \"degree_C\";\n"
              + "        String standard_name \"sea_surface_temperature\";\n"
              + "        Int16 _FillValue -32767;\n"
              + "        Int16 valid_min -1000;\n"
              + "        Int16 valid_max 10000;\n"
              + "        String display_scale \"linear\";\n"
              + "        Float32 display_min -2.0;\n"
              + "        Float32 display_max 45.0;\n"
              + "        Int32 _ChunkSizes 44, 87;\n"
              + "    }\n"
              + "    qual_sst4 {\n"
              + "        String long_name \"Quality Levels, Sea Surface Temperature\";\n"
              + "        Int16 _FillValue -1;\n"
              + "        String valid_min \"0\";\n"
              + // source problem: should be Byte
              "        String valid_max \"5\";\n"
              + // source problem: should be Byte
              "        String _Unsigned \"true\";\n"
              + "        Int32 _ChunkSizes 44, 87;\n"
              + "        String standard_name \"flag_values\";\n"
              + "    }\n"
              + "    NC_GLOBAL {\n"
              + "        String product_name \"AQUA_MODIS.[DATE]_[DATE].L3m.8D.SST4.sst4.4km.nc\";\n"
              + "        String instrument \"MODIS\";\n"
              + "        String title \"MODISA Level-3 Standard Mapped Image\";\n"
              + "        String project \"Ocean Biology Processing Group (NASA/GSFC/OBPG)\";\n"
              + "        String platform \"Aqua\";\n"
              + "        String temporal_range \"8-day\";\n"
              + "        String processing_version \"R2019.0\";\n"
              + "        String date_created \"[TIME]\";\n"
              + // 2020-08-20 changed by 4
              // seconds, and other dates
              "        String history \"l3mapgen par=AQUA_MODIS.[DATE]_[DATE].L3m.8D.SST4.sst4.4km.nc.param \";\n"
              + "        String l2_flag_names \"LAND,~HISOLZEN\";\n"
              + "        String time_coverage_start \"[TIME]\";\n"
              + "        String time_coverage_end \"[TIME]\";\n"
              + "        Int32 start_orbit_number [SON];\n"
              + "        Int32 end_orbit_number [EON];\n"
              + "        String map_projection \"Equidistant Cylindrical\";\n"
              + "        String latitude_units \"degrees_north\";\n"
              + "        String longitude_units \"degrees_east\";\n"
              + "        Float32 northernmost_latitude 90.0;\n"
              + "        Float32 southernmost_latitude -90.0;\n"
              + "        Float32 westernmost_longitude -180.0;\n"
              + "        Float32 easternmost_longitude 180.0;\n"
              + "        Float32 geospatial_lat_max 90.0;\n"
              + "        Float32 geospatial_lat_min -90.0;\n"
              + "        Float32 geospatial_lon_max 180.0;\n"
              + "        Float32 geospatial_lon_min -180.0;\n"
              + "        Float32 latitude_step 0.041666668;\n"
              + "        Float32 longitude_step 0.041666668;\n"
              + "        Float32 sw_point_latitude -89.979164;\n"
              + "        Float32 sw_point_longitude -179.97917;\n"
              + "        String spatialResolution \"4.64 km\";\n"
              + "        Float32 geospatial_lon_resolution 0.041666668;\n"
              + "        Float32 geospatial_lat_resolution 0.041666668;\n"
              + "        String geospatial_lat_units \"degrees_north\";\n"
              + "        String geospatial_lon_units \"degrees_east\";\n"
              + "        Int32 number_of_lines 4320;\n"
              + "        Int32 number_of_columns 8640;\n"
              + "        String measure \"Mean\";\n"
              + "        Float32 suggested_image_scaling_minimum -2.0;\n"
              + "        Float32 suggested_image_scaling_maximum 45.0;\n"
              + "        String suggested_image_scaling_type \"LINEAR\";\n"
              + "        String suggested_image_scaling_applied \"No\";\n"
              + "        String _lastModified \"[TIME]\";\n"
              + "        String Conventions \"CF-1.6 ACDD-1.3\";\n"
              + "        String institution \"NASA Goddard Space Flight Center, Ocean Ecology Laboratory, Ocean Biology Processing Group\";\n"
              + "        String standard_name_vocabulary \"CF Standard Name Table v36\";\n"
              + "        String naming_authority \"gov.nasa.gsfc.sci.oceandata\";\n"
              + "        String id \"AQUA_MODIS.[DATE]_[DATE].L3b.8D.SST4.nc/L3/AQUA_MODIS.[DATE]_[DATE].L3b.8D.SST4.nc\";\n"
              + "        String license \"https://science.nasa.gov/earth-science/earth-science-data/data-information-policy/\";\n"
              + "        String creator_name \"NASA/GSFC/OBPG\";\n"
              + "        String publisher_name \"NASA/GSFC/OBPG\";\n"
              + "        String creator_email \"data@oceancolor.gsfc.nasa.gov\";\n"
              + "        String publisher_email \"data@oceancolor.gsfc.nasa.gov\";\n"
              + "        String creator_url \"https://oceandata.sci.gsfc.nasa.gov\";\n"
              + "        String publisher_url \"https://oceandata.sci.gsfc.nasa.gov\";\n"
              + "        String processing_level \"L3 Mapped\";\n"
              + "        String cdm_data_type \"grid\";\n"
              + "        Int32 data_bins [DB];\n"
              + "        Float32 data_minimum [DM];\n"
              + "        Float32 data_maximum [DM];\n"
              + "        String start_date \"2002-07-04 UTC\";\n"
              + "        String start_time \"00:00:00 UTC\";\n"
              + "        String stop_date \"[STOP_DATE] UTC\";\n"
              + // varies
              "        String stop_time \"23:59:59 UTC\";\n"
              + "        String processing_control_software_name \"l3mapgen\";\n"
              + "        String processing_control_software_version \"2.3.0-V2021.1\";\n"
              + // changes
              // sometimes
              "        String processing_control_source \"AQUA_MODIS.[DATE]_[DATE].L3b.8D.SST4.nc\";\n"
              + "        String processing_control_l2_flag_names \"LAND,~HISOLZEN\";\n"
              + "        String processing_control_input_parameters_par \"AQUA_MODIS.[DATE]_[DATE].L3m.8D.SST4.sst4.4km.nc.param\";\n"
              + "        String processing_control_input_parameters_suite \"SST4\";\n"
              + "        String processing_control_input_parameters_ifile \"AQUA_MODIS.[DATE]_[DATE].L3b.8D.SST4.nc\";\n"
              + "        String processing_control_input_parameters_ofile \"AQUA_MODIS.[DATE]_[DATE].L3m.8D.SST4.sst4.4km.nc\";\n"
              + "        String processing_control_input_parameters_oformat \"2\";\n"
              + "        String processing_control_input_parameters_ofile_product_tag \"PRODUCT\";\n"
              + "        String processing_control_input_parameters_ofile2 \"\";\n"
              + "        String processing_control_input_parameters_oformat2 \"png\";\n"
              + "        String processing_control_input_parameters_deflate \"4\";\n"
              + "        String processing_control_input_parameters_product \"sst4\";\n"
              + "        String processing_control_input_parameters_resolution \"4km\";\n"
              + "        String processing_control_input_parameters_width \"\";\n"
              + "        String processing_control_input_parameters_projection \"smi\";\n"
              + "        String processing_control_input_parameters_write_projtext \"no\";\n"
              + "        String processing_control_input_parameters_central_meridian \"-999\";\n"
              + "        String processing_control_input_parameters_lat_ts \"\";\n"
              + "        String processing_control_input_parameters_lat_0 \"\";\n"
              + "        String processing_control_input_parameters_lat_1 \"\";\n"
              + "        String processing_control_input_parameters_lat_2 \"\";\n"
              + "        String processing_control_input_parameters_azimuth \"\";\n"
              + "        String processing_control_input_parameters_utm_zone \"\";\n"
              + "        String processing_control_input_parameters_north \"90.000\";\n"
              + "        String processing_control_input_parameters_south \"-90.000\";\n"
              + "        String processing_control_input_parameters_east \"180.000\";\n"
              + "        String processing_control_input_parameters_west \"-180.000\";\n"
              + "        String processing_control_input_parameters_trimNSEW \"yes\";\n"
              + "        String processing_control_input_parameters_interp \"area\";\n"
              + "        String processing_control_input_parameters_apply_pal \"1\";\n"
              + "        String processing_control_input_parameters_palfile \"\";\n"
              + "        String processing_control_input_parameters_use_transparency \"no\";\n"
              + "        String processing_control_input_parameters_datamin \"\";\n"
              + "        String processing_control_input_parameters_datamax \"\";\n"
              + "        String processing_control_input_parameters_scale_type \"\";\n"
              + "        String processing_control_input_parameters_quiet \"false\";\n"
              + "        String processing_control_input_parameters_pversion \"R2019.0\";\n"
              + "        String processing_control_input_parameters_use_quality \"yes\";\n"
              + "        String processing_control_input_parameters_quality_product \"\";\n"
              + "        String processing_control_input_parameters_use_rgb \"no\";\n"
              + "        String processing_control_input_parameters_product_rgb \"rhos_645,rhos_555,rhos_469\";\n"
              + "        String processing_control_input_parameters_fudge \"1.0\";\n"
              + "        String processing_control_input_parameters_threshold \"0\";\n"
              + "        String processing_control_input_parameters_num_cache \"500\";\n"
              + "        String processing_control_input_parameters_mask_land \"no\";\n"
              + "        String processing_control_input_parameters_rgb_land \"160,82,45\";\n"
              + "        String processing_control_input_parameters_land \"$OCDATAROOT/common/landmask_GMT15ARC.nc\";\n"
              + "        String processing_control_input_parameters_full_latlon \"yes\";\n"
              + "    }\n"
              + "}\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // ensure files are reread
      File2.deleteAllFiles(EDDGridFromDap.datasetDir("testUInt16Dap"));
      EDDGrid eddGrid = (EDDGrid) EDDGridFromDap.oneFromDatasetsXml(null, "testUInt16Dap");
      // in uaf erddap, this is nasa_jpl_c688_be2f_cf9d

      // .nccsvMetadata
      tName =
          eddGrid.makeNewFileForDapQuery(
              language,
              null,
              null,
              "",
              EDStatic.fullTestCacheDirectory,
              eddGrid.className() + "uint16",
              ".nccsvMetadata");
      results = File2.readFromFile88591(EDStatic.fullTestCacheDirectory + tName)[1];
      results =
          results.replaceAll(
              "(2019\\-12\\-17|2022\\-03\\-23)", // flips between 2019-12-17 and
              // 2022-03-23
              "[DATE]");
      results =
          results.replaceAll(
              "\\[DATE\\]T.*.000Z", // date changes sometimes //2021-11-17 was
              // 2020-09-21,
              // now
              // 2019!
              "[DATE]T[TIME].000Z");
      results =
          results.replaceAll("AQUA_MODIS\\.\\d{8}_\\d{8}\\.L3", "AQUA_MODIS.[DATE]_[DATE].L3");
      results =
          results.replaceAll(
              "\\*GLOBAL\\*,time_coverage_end,.*Z", "*GLOBAL*,time_coverage_end,[TIME]Z");

      expected =
          "*GLOBAL*,Conventions,\"CF-1.6 ACDD-1.3, COARDS, NCCSV-1.2\"\n"
              + "*GLOBAL*,_lastModified,[DATE]T[TIME].000Z\n"
              + // changes by a few seconds
              // periodically!
              "*GLOBAL*,cdm_data_type,Grid\n"
              + "*GLOBAL*,creator_email,data@oceancolor.gsfc.nasa.gov\n"
              + "*GLOBAL*,creator_name,NASA/GSFC/OBPG\n"
              + "*GLOBAL*,creator_type,group\n"
              + "*GLOBAL*,creator_url,https://oceandata.sci.gsfc.nasa.gov\n"
              + "*GLOBAL*,date_created,[DATE]T[TIME].000Z\n"
              + "*GLOBAL*,Easternmost_Easting,179.979169546d\n"
              + "*GLOBAL*,geospatial_lat_max,89.979166667d\n"
              + "*GLOBAL*,geospatial_lat_min,-89.97916810599999d\n"
              + "*GLOBAL*,geospatial_lat_resolution,0.041666667d\n"
              + "*GLOBAL*,geospatial_lat_units,degrees_north\n"
              + "*GLOBAL*,geospatial_lon_max,179.979169546d\n"
              + "*GLOBAL*,geospatial_lon_min,-179.979166667d\n"
              + "*GLOBAL*,geospatial_lon_resolution,0.041666667d\n"
              + "*GLOBAL*,geospatial_lon_units,degrees_east\n"
              + "*GLOBAL*,history,l3mapgen par=AQUA_MODIS.[DATE]_[DATE].L3m.8D.SST4.sst4.4km.nc.param\n"
              + "*GLOBAL*,id,AQUA_MODIS.[DATE]_[DATE].L3b.8D.SST4.nc/L3/AQUA_MODIS.[DATE]_[DATE].L3b.8D.SST4.nc\n"
              + "*GLOBAL*,infoUrl,https://podaac.jpl.nasa.gov/dataset/MODIS_AQUA_L3_SST_MID_IR_8DAY_4KM_NIGHTTIME_v2019.0\n"
              + "*GLOBAL*,institution,NASA/GSFC OBPG\n"
              + "*GLOBAL*,instrument,MODIS\n"
              + "*GLOBAL*,keywords,\"4km, 4um, 8day, aqua, biology, center, color, data, earth, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, flag, flag_values, flight, goddard, group, gsfc, image, imaging, infrared, L3, latitude, level, level-3, levels, longitude, mapped, mid, moderate, modis, modisa, nasa, night, nighttime, obpg, ocean, oceans, processing, qual_sst4, quality, resolution, science, sea, sea_surface_temperature, smi, space, spectroradiometer, sst, sst4, standard, surface, temperature, time, v2019.0, values\"\n"
              + "*GLOBAL*,keywords_vocabulary,GCMD Science Keywords\n"
              + "*GLOBAL*,l2_flag_names,\"LAND,~HISOLZEN\"\n"
              + "*GLOBAL*,license,https://science.nasa.gov/earth-science/earth-science-data/data-information-policy/\n"
              + "*GLOBAL*,map_projection,Equidistant Cylindrical\n"
              + "*GLOBAL*,measure,Mean\n"
              + "*GLOBAL*,naming_authority,gov.nasa.gsfc.sci.oceandata\n"
              + "*GLOBAL*,Northernmost_Northing,89.979166667d\n"
              + "*GLOBAL*,platform,Aqua\n"
              + "*GLOBAL*,processing_control_input_parameters_apply_pal,\"1\"\n"
              + "*GLOBAL*,processing_control_input_parameters_central_meridian,\"-999\"\n"
              + "*GLOBAL*,processing_control_input_parameters_deflate,\"4\"\n"
              + "*GLOBAL*,processing_control_input_parameters_east,\"180.000\"\n"
              + "*GLOBAL*,processing_control_input_parameters_fudge,\"1.0\"\n"
              + "*GLOBAL*,processing_control_input_parameters_full_latlon,yes\n"
              + "*GLOBAL*,processing_control_input_parameters_ifile,AQUA_MODIS.[DATE]_[DATE].L3b.8D.SST4.nc\n"
              + "*GLOBAL*,processing_control_input_parameters_interp,area\n"
              + "*GLOBAL*,processing_control_input_parameters_land,$OCDATAROOT/common/landmask_GMT15ARC.nc\n"
              + "*GLOBAL*,processing_control_input_parameters_mask_land,no\n"
              + "*GLOBAL*,processing_control_input_parameters_north,\"90.000\"\n"
              + "*GLOBAL*,processing_control_input_parameters_num_cache,\"500\"\n"
              + "*GLOBAL*,processing_control_input_parameters_ofile,AQUA_MODIS.[DATE]_[DATE].L3m.8D.SST4.sst4.4km.nc\n"
              + "*GLOBAL*,processing_control_input_parameters_ofile_product_tag,PRODUCT\n"
              + "*GLOBAL*,processing_control_input_parameters_oformat,\"2\"\n"
              + "*GLOBAL*,processing_control_input_parameters_oformat2,png\n"
              + "*GLOBAL*,processing_control_input_parameters_par,AQUA_MODIS.[DATE]_[DATE].L3m.8D.SST4.sst4.4km.nc.param\n"
              + "*GLOBAL*,processing_control_input_parameters_product,sst4\n"
              + "*GLOBAL*,processing_control_input_parameters_product_rgb,\"rhos_645,rhos_555,rhos_469\"\n"
              + "*GLOBAL*,processing_control_input_parameters_projection,smi\n"
              + "*GLOBAL*,processing_control_input_parameters_pversion,R2019.0\n"
              + "*GLOBAL*,processing_control_input_parameters_quiet,false\n"
              + "*GLOBAL*,processing_control_input_parameters_resolution,4km\n"
              + "*GLOBAL*,processing_control_input_parameters_rgb_land,\"160,82,45\"\n"
              + "*GLOBAL*,processing_control_input_parameters_south,\"-90.000\"\n"
              + "*GLOBAL*,processing_control_input_parameters_suite,SST4\n"
              + "*GLOBAL*,processing_control_input_parameters_threshold,\"0\"\n"
              + "*GLOBAL*,processing_control_input_parameters_trimNSEW,yes\n"
              + "*GLOBAL*,processing_control_input_parameters_use_quality,yes\n"
              + "*GLOBAL*,processing_control_input_parameters_use_rgb,no\n"
              + "*GLOBAL*,processing_control_input_parameters_use_transparency,no\n"
              + "*GLOBAL*,processing_control_input_parameters_west,\"-180.000\"\n"
              + "*GLOBAL*,processing_control_input_parameters_write_projtext,no\n"
              + "*GLOBAL*,processing_control_l2_flag_names,\"LAND,~HISOLZEN\"\n"
              + "*GLOBAL*,processing_control_software_name,l3mapgen\n"
              + "*GLOBAL*,processing_control_software_version,2.3.0-V2021.1\n"
              + // changes
              // sometimes
              "*GLOBAL*,processing_control_source,AQUA_MODIS.[DATE]_[DATE].L3b.8D.SST4.nc\n"
              + "*GLOBAL*,processing_level,L3 Mapped\n"
              + "*GLOBAL*,processing_version,R2019.0\n"
              + "*GLOBAL*,product_name,AQUA_MODIS.[DATE]_[DATE].L3m.8D.SST4.sst4.4km.nc\n"
              + "*GLOBAL*,project,Ocean Biology Processing Group (NASA/GSFC/OBPG)\n"
              + "*GLOBAL*,publisher_email,data@oceancolor.gsfc.nasa.gov\n"
              + "*GLOBAL*,publisher_name,NASA/GSFC/OBPG\n"
              + "*GLOBAL*,publisher_type,group\n"
              + "*GLOBAL*,publisher_url,https://oceandata.sci.gsfc.nasa.gov\n"
              + "*GLOBAL*,sourceUrl,https://thredds.jpl.nasa.gov/thredds/dodsC/ncml_aggregation/OceanTemperature/modis/aqua/4um/4km/aggregate__MODIS_AQUA_L3_SST_MID_IR_8DAY_4KM_NIGHTTIME_v2019.0.ncml\n"
              + "*GLOBAL*,Southernmost_Northing,-89.97916810599999d\n"
              + "*GLOBAL*,spatialResolution,4.64 km\n"
              + "*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v36\n"
              + "*GLOBAL*,summary,Moderate Resolution Imaging Spectroradiometer on Aqua (MODISA) Level-3 Standard Mapped Image (MODIS AQUA L3 Sea Surface Temperature (SST) MID InfraRed (IR) 8DAY 4KM NIGHTTIME v2019.0)\n"
              + "*GLOBAL*,temporal_range,8-day\n"
              + "*GLOBAL*,time_coverage_end,[TIME]Z\n"
              + "*GLOBAL*,time_coverage_start,2002-07-04T00:00:00Z\n"
              + "*GLOBAL*,title,\"MODISA L3 SMI, MODIS AQUA L3 SST MID IR 8DAY 4KM NIGHTTIME v2019.0 [time][lat][lon], 0.041666668\u00b0, 2002-present\"\n"
              + "*GLOBAL*,Westernmost_Easting,-179.979166667d\n"
              + "time,*DATA_TYPE*,String\n"
              + "time,_CoordinateAxisType,Time\n"
              + "time,actual_range,2002-07-04T00:00:00Z\\n2022-03-30T00:00:00Z\n"
              + // changes,
              // but can't
              // use
              // replaceAll
              // above
              // to avoid
              "time,axis,T\n"
              + "time,ioos_category,Time\n"
              + "time,long_name,Time\n"
              + "time,standard_name,time\n"
              + "time,time_origin,01-JAN-1970 00:00:00\n"
              + "time,units,yyyy-MM-dd'T'HH:mm:ssZ\n"
              + "latitude,*DATA_TYPE*,double\n"
              + "latitude,_CoordinateAxisType,Lat\n"
              + "latitude,actual_range,-89.97916810599999d,89.979166667d\n"
              + "latitude,axis,Y\n"
              + "latitude,ioos_category,Location\n"
              + "latitude,long_name,Latitude\n"
              + "latitude,standard_name,latitude\n"
              + "latitude,units,degrees_north\n"
              + "latitude,valid_max,90.0d\n"
              + "latitude,valid_min,-90.0d\n"
              + "longitude,*DATA_TYPE*,double\n"
              + "longitude,_CoordinateAxisType,Lon\n"
              + "longitude,actual_range,-179.979166667d,179.979169546d\n"
              + "longitude,axis,X\n"
              + "longitude,ioos_category,Location\n"
              + "longitude,long_name,Longitude\n"
              + "longitude,standard_name,longitude\n"
              + "longitude,units,degrees_east\n"
              + "longitude,valid_max,180.0d\n"
              + "longitude,valid_min,-180.0d\n"
              + "sst4,*DATA_TYPE*,float\n"
              + "sst4,_FillValue,-163.835f\n"
              + "sst4,colorBarMaximum,32.0d\n"
              + "sst4,colorBarMinimum,0.0d\n"
              + "sst4,ioos_category,Temperature\n"
              + "sst4,long_name,4um Sea Surface Temperature\n"
              + "sst4,standard_name,sea_surface_temperature\n"
              + "sst4,units,degree_C\n"
              + "sst4,valid_max,50.0f\n"
              + "sst4,valid_min,-5.0f\n"
              + "qual_sst4,*DATA_TYPE*,ushort\n"
              + "qual_sst4,_FillValue,65535us\n"
              + // important test
              "qual_sst4,colorBarMaximum,150.0d\n"
              + "qual_sst4,colorBarMinimum,0.0d\n"
              + "qual_sst4,ioos_category,Quality\n"
              + "qual_sst4,long_name,\"Quality Levels, Sea Surface Temperature\"\n"
              + "qual_sst4,standard_name,flag_values\n"
              + "qual_sst4,units,degrees_north\n"
              + "qual_sst4,valid_max,5us\n"
              + "qual_sst4,valid_min,0us\n"
              + "\n"
              + "*END_METADATA*\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // .das from erddap dataset das isn't affected by userDapQuery
      tName =
          eddGrid.makeNewFileForDapQuery(
              language,
              null,
              null,
              "",
              EDStatic.fullTestCacheDirectory,
              eddGrid.className() + "uint16",
              ".das");
      results = File2.readFromFile88591(EDStatic.fullTestCacheDirectory + tName)[1];
      results =
          results.replaceAll("2\\d{3}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z http", "[TODAY] http");
      results = results.replaceAll("20\\d{6}", "[DATE]");
      results = results.replaceAll("String date_created \".*\";", "String date_created \"[DC]\";");
      results =
          results.replaceAll("String _lastModified \".*\";", "String _lastModified \"[LM]\";");
      results =
          results.replaceAll(
              "Float64 actual_range 1.0257408e\\+9, .*;",
              "Float64 actual_range 1.0257408e+9, [TIME];");
      results =
          results.replaceAll(
              "String time_coverage_end \".*\";", "String time_coverage_end \"[TCE]\";");

      expected =
          "Attributes {\n"
              + "  time {\n"
              + "    String _CoordinateAxisType \"Time\";\n"
              + "    Float64 actual_range 1.0257408e+9, [TIME];\n"
              + "    String axis \"T\";\n"
              + "    String ioos_category \"Time\";\n"
              + "    String long_name \"Time\";\n"
              + "    String standard_name \"time\";\n"
              + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
              + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
              + "  }\n"
              + "  latitude {\n"
              + "    String _CoordinateAxisType \"Lat\";\n"
              + "    Float64 actual_range -89.97916810599999, 89.979166667;\n"
              + "    String axis \"Y\";\n"
              + "    String ioos_category \"Location\";\n"
              + "    String long_name \"Latitude\";\n"
              + "    String standard_name \"latitude\";\n"
              + "    String units \"degrees_north\";\n"
              + "    Float64 valid_max 90.0;\n"
              + "    Float64 valid_min -90.0;\n"
              + "  }\n"
              + "  longitude {\n"
              + "    String _CoordinateAxisType \"Lon\";\n"
              + "    Float64 actual_range -179.979166667, 179.979169546;\n"
              + "    String axis \"X\";\n"
              + "    String ioos_category \"Location\";\n"
              + "    String long_name \"Longitude\";\n"
              + "    String standard_name \"longitude\";\n"
              + "    String units \"degrees_east\";\n"
              + "    Float64 valid_max 180.0;\n"
              + "    Float64 valid_min -180.0;\n"
              + "  }\n"
              + "  sst4 {\n"
              + "    Float32 _FillValue -163.835;\n"
              + "    Float64 colorBarMaximum 32.0;\n"
              + "    Float64 colorBarMinimum 0.0;\n"
              + "    String ioos_category \"Temperature\";\n"
              + "    String long_name \"4um Sea Surface Temperature\";\n"
              + "    String standard_name \"sea_surface_temperature\";\n"
              + "    String units \"degree_C\";\n"
              + "    Float32 valid_max 50.0;\n"
              + "    Float32 valid_min -5.0;\n"
              + "  }\n"
              + "  qual_sst4 {\n"
              + "    UInt16 _FillValue 65535;\n"
              + "    Float64 colorBarMaximum 150.0;\n"
              + "    Float64 colorBarMinimum 0.0;\n"
              + "    String ioos_category \"Quality\";\n"
              + "    String long_name \"Quality Levels, Sea Surface Temperature\";\n"
              + "    String standard_name \"flag_values\";\n"
              + "    String units \"degrees_north\";\n"
              + "    UInt16 valid_max 5;\n"
              + "    UInt16 valid_min 0;\n"
              + "  }\n"
              + "  NC_GLOBAL {\n"
              + "    String _lastModified \"[LM]\";\n"
              + // 2020-08-20 changed by 4 seconds
              "    String cdm_data_type \"Grid\";\n"
              + "    String Conventions \"CF-1.6 ACDD-1.3, COARDS\";\n"
              + "    String creator_email \"data@oceancolor.gsfc.nasa.gov\";\n"
              + "    String creator_name \"NASA/GSFC/OBPG\";\n"
              + "    String creator_type \"group\";\n"
              + "    String creator_url \"https://oceandata.sci.gsfc.nasa.gov\";\n"
              + "    String date_created \"[DC]\";\n"
              + "    Float64 Easternmost_Easting 179.979169546;\n"
              + "    Float64 geospatial_lat_max 89.979166667;\n"
              + "    Float64 geospatial_lat_min -89.97916810599999;\n"
              + "    Float64 geospatial_lat_resolution 0.041666667;\n"
              + "    String geospatial_lat_units \"degrees_north\";\n"
              + "    Float64 geospatial_lon_max 179.979169546;\n"
              + "    Float64 geospatial_lon_min -179.979166667;\n"
              + "    Float64 geospatial_lon_resolution 0.041666667;\n"
              + "    String geospatial_lon_units \"degrees_east\";\n"
              + "    String history \"l3mapgen par=AQUA_MODIS.[DATE]_[DATE].L3m.8D.SST4.sst4.4km.nc.param\n"
              + "[TODAY] https://thredds.jpl.nasa.gov/thredds/dodsC/ncml_aggregation/OceanTemperature/modis/aqua/4um/4km/aggregate__MODIS_AQUA_L3_SST_MID_IR_8DAY_4KM_NIGHTTIME_v2019.0.ncml\n"
              + "[TODAY] http://127.0.0.1:8080/cwexperimental/griddap/testUInt16Dap.das\";\n"
              + "    String id \"AQUA_MODIS.[DATE]_[DATE].L3b.8D.SST4.nc/L3/AQUA_MODIS.[DATE]_[DATE].L3b.8D.SST4.nc\";\n"
              + "    String infoUrl \"https://podaac.jpl.nasa.gov/dataset/MODIS_AQUA_L3_SST_MID_IR_8DAY_4KM_NIGHTTIME_v2019.0\";\n"
              + "    String institution \"NASA/GSFC OBPG\";\n"
              + "    String instrument \"MODIS\";\n"
              + "    String keywords \"4km, 4um, 8day, aqua, biology, center, color, data, earth, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, flag, flag_values, flight, goddard, group, gsfc, image, imaging, infrared, L3, latitude, level, level-3, levels, longitude, mapped, mid, moderate, modis, modisa, nasa, night, nighttime, obpg, ocean, oceans, processing, qual_sst4, quality, resolution, science, sea, sea_surface_temperature, smi, space, spectroradiometer, sst, sst4, standard, surface, temperature, time, v2019.0, values\";\n"
              + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
              + "    String l2_flag_names \"LAND,~HISOLZEN\";\n"
              + "    String license \"https://science.nasa.gov/earth-science/earth-science-data/data-information-policy/\";\n"
              + "    String map_projection \"Equidistant Cylindrical\";\n"
              + "    String measure \"Mean\";\n"
              + "    String naming_authority \"gov.nasa.gsfc.sci.oceandata\";\n"
              + "    Float64 Northernmost_Northing 89.979166667;\n"
              + "    String platform \"Aqua\";\n"
              + "    String processing_control_input_parameters_apply_pal \"1\";\n"
              + "    String processing_control_input_parameters_central_meridian \"-999\";\n"
              + "    String processing_control_input_parameters_deflate \"4\";\n"
              + "    String processing_control_input_parameters_east \"180.000\";\n"
              + "    String processing_control_input_parameters_fudge \"1.0\";\n"
              + "    String processing_control_input_parameters_full_latlon \"yes\";\n"
              + "    String processing_control_input_parameters_ifile \"AQUA_MODIS.[DATE]_[DATE].L3b.8D.SST4.nc\";\n"
              + "    String processing_control_input_parameters_interp \"area\";\n"
              + "    String processing_control_input_parameters_land \"$OCDATAROOT/common/landmask_GMT15ARC.nc\";\n"
              + "    String processing_control_input_parameters_mask_land \"no\";\n"
              + "    String processing_control_input_parameters_north \"90.000\";\n"
              + "    String processing_control_input_parameters_num_cache \"500\";\n"
              + "    String processing_control_input_parameters_ofile \"AQUA_MODIS.[DATE]_[DATE].L3m.8D.SST4.sst4.4km.nc\";\n"
              + "    String processing_control_input_parameters_ofile_product_tag \"PRODUCT\";\n"
              + "    String processing_control_input_parameters_oformat \"2\";\n"
              + "    String processing_control_input_parameters_oformat2 \"png\";\n"
              + "    String processing_control_input_parameters_par \"AQUA_MODIS.[DATE]_[DATE].L3m.8D.SST4.sst4.4km.nc.param\";\n"
              + "    String processing_control_input_parameters_product \"sst4\";\n"
              + "    String processing_control_input_parameters_product_rgb \"rhos_645,rhos_555,rhos_469\";\n"
              + "    String processing_control_input_parameters_projection \"smi\";\n"
              + "    String processing_control_input_parameters_pversion \"R2019.0\";\n"
              + "    String processing_control_input_parameters_quiet \"false\";\n"
              + "    String processing_control_input_parameters_resolution \"4km\";\n"
              + "    String processing_control_input_parameters_rgb_land \"160,82,45\";\n"
              + "    String processing_control_input_parameters_south \"-90.000\";\n"
              + "    String processing_control_input_parameters_suite \"SST4\";\n"
              + "    String processing_control_input_parameters_threshold \"0\";\n"
              + "    String processing_control_input_parameters_trimNSEW \"yes\";\n"
              + "    String processing_control_input_parameters_use_quality \"yes\";\n"
              + "    String processing_control_input_parameters_use_rgb \"no\";\n"
              + "    String processing_control_input_parameters_use_transparency \"no\";\n"
              + "    String processing_control_input_parameters_west \"-180.000\";\n"
              + "    String processing_control_input_parameters_write_projtext \"no\";\n"
              + "    String processing_control_l2_flag_names \"LAND,~HISOLZEN\";\n"
              + "    String processing_control_software_name \"l3mapgen\";\n"
              + "    String processing_control_software_version \"2.3.0-V2021.1\";\n"
              + // changes
              "    String processing_control_source \"AQUA_MODIS.[DATE]_[DATE].L3b.8D.SST4.nc\";\n"
              + "    String processing_level \"L3 Mapped\";\n"
              + "    String processing_version \"R2019.0\";\n"
              + "    String product_name \"AQUA_MODIS.[DATE]_[DATE].L3m.8D.SST4.sst4.4km.nc\";\n"
              + "    String project \"Ocean Biology Processing Group (NASA/GSFC/OBPG)\";\n"
              + "    String publisher_email \"data@oceancolor.gsfc.nasa.gov\";\n"
              + "    String publisher_name \"NASA/GSFC/OBPG\";\n"
              + "    String publisher_type \"group\";\n"
              + "    String publisher_url \"https://oceandata.sci.gsfc.nasa.gov\";\n"
              + "    String sourceUrl \"https://thredds.jpl.nasa.gov/thredds/dodsC/ncml_aggregation/OceanTemperature/modis/aqua/4um/4km/aggregate__MODIS_AQUA_L3_SST_MID_IR_8DAY_4KM_NIGHTTIME_v2019.0.ncml\";\n"
              + "    Float64 Southernmost_Northing -89.97916810599999;\n"
              + "    String spatialResolution \"4.64 km\";\n"
              + "    String standard_name_vocabulary \"CF Standard Name Table v36\";\n"
              + "    String summary \"Moderate Resolution Imaging Spectroradiometer on Aqua (MODISA) Level-3 Standard Mapped Image (MODIS AQUA L3 Sea Surface Temperature (SST) MID InfraRed (IR) 8DAY 4KM NIGHTTIME v2019.0)\";\n"
              + "    String temporal_range \"8-day\";\n"
              + "    String time_coverage_end \"[TCE]\";\n"
              + "    String time_coverage_start \"2002-07-04T00:00:00Z\";\n"
              + "    String title \"MODISA L3 SMI, MODIS AQUA L3 SST MID IR 8DAY 4KM NIGHTTIME v2019.0 [time][lat][lon], 0.041666668°, 2002-present\";\n"
              + "    Float64 Westernmost_Easting -179.979166667;\n"
              + "  }\n"
              + "}\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);

      // .dds dds isn't affected by userDapQuery
      tName =
          eddGrid.makeNewFileForDapQuery(
              language,
              null,
              null,
              "",
              EDStatic.fullTestCacheDirectory,
              eddGrid.className() + "uint16",
              ".dds");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      results = results.replaceAll("\\[time = \\d*\\]", "[time = [N_TIME]]");
      expected = // difference from testUInt16File: lat lon are double here, not float
          "Dataset {\n"
              + "  Float64 time[time = [N_TIME]];\n"
              + "  Float64 latitude[latitude = 4320];\n"
              + "  Float64 longitude[longitude = 8640];\n"
              + "  GRID {\n"
              + "    ARRAY:\n"
              + "      Float32 sst4[time = [N_TIME]][latitude = 4320][longitude = 8640];\n"
              + "    MAPS:\n"
              + "      Float64 time[time = [N_TIME]];\n"
              + "      Float64 latitude[latitude = 4320];\n"
              + "      Float64 longitude[longitude = 8640];\n"
              + "  } sst4;\n"
              + "  GRID {\n"
              + "    ARRAY:\n"
              + "      UInt16 qual_sst4[time = [N_TIME]][latitude = 4320][longitude = 8640];\n"
              + "    MAPS:\n"
              + "      Float64 time[time = [N_TIME]];\n"
              + "      Float64 latitude[latitude = 4320];\n"
              + "      Float64 longitude[longitude = 8640];\n"
              + "  } qual_sst4;\n"
              + "} testUInt16Dap;\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);

      // .csv data values
      // same query to source also shows 255's
      // https://thredds.jpl.nasa.gov/thredds/dodsC/ncml_aggregation/OceanTemperature/modis/aqua/4um/4km/aggregate__MODIS_AQUA_L3_SST_MID_IR_8DAY_4KM_NIGHTTIME_v2019.0.ncml.ascii?qual_sst4%5B0:1:0%5D%5B400:200:800%5D%5B400:200:800%5D
      userDapQuery = "qual_sst4[0][400:200:800][400:200:800]";
      tName =
          eddGrid.makeNewFileForDapQuery(
              language,
              null,
              null,
              userDapQuery,
              EDStatic.fullTestCacheDirectory,
              eddGrid.className() + "uint16",
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      String2.log(results);
      expected =
          // ugly lat and lon values (clearly float -> double)
          "time,latitude,longitude,qual_sst4\n"
              + "UTC,degrees_north,degrees_east,degrees_north\n"
              + "2002-07-04T00:00:00Z,73.312499867,-163.31249986699999,65535\n"
              + // currently appears as
              // 255 (from
              // source) but that's
              // wrong, should be
              // 65535, or _FillValue
              // should be
              // different
              "2002-07-04T00:00:00Z,73.312499867,-154.979166467,65535\n"
              + // and
              // same
              // on
              // following
              // lines
              "2002-07-04T00:00:00Z,73.312499867,-146.64583306699998,65535\n"
              + "2002-07-04T00:00:00Z,64.979166467,-163.31249986699999,65535\n"
              + "2002-07-04T00:00:00Z,64.979166467,-154.979166467,65535\n"
              + "2002-07-04T00:00:00Z,64.979166467,-146.64583306699998,65535\n"
              + "2002-07-04T00:00:00Z,56.645833067000005,-163.31249986699999,0\n"
              + "2002-07-04T00:00:00Z,56.645833067000005,-154.979166467,1\n"
              + "2002-07-04T00:00:00Z,56.645833067000005,-146.64583306699998,65535\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);

      // display the image
      String2.log("\n\n* PNG ");
      tName =
          eddGrid.makeNewFileForDapQuery(
              language,
              null,
              null,
              "qual_sst4[0][][]",
              EDStatic.fullTestCacheDirectory,
              eddGrid.className() + "uint16",
              ".png");
      Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

    } catch (Throwable t) {
      Test.knownProblem(
          "This dataset has problems with time values in future and incorrect qual_sst4 _FillValue (255 vs 65535) and units (degree_north! -- now fixed). \n"
              + "2020-08-14 I notified podaac, so hopefully this will change.",
          t);
    }
  }

  /**
   * This scale_factor=1 add_offset=0 with a different data type. Thanks to Roy Mendelssohn.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagThredds
  void testScale1Offset0() throws Throwable {
    // String2.log("\n*** EDDGridFromDap.testScale1Offset0");
    int language = 0;
    // testVerboseOn();
    String name, tName, results, tResults, expected, userDapQuery;

    EDDGrid eddGrid = null;

    try {
      eddGrid =
          (EDDGrid) EDDGridFromDap.oneFromDatasetsXml(null, "testDAPnodcPH2sstd1day"); // 2022-05-23
      // new
      // THREDDS
      // fail:
      // https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/PH2/sstd/1day
      // has unsorted time dimension:
      // AxisVariable=time isn't sorted.
      // IntArray isn't sorted in ascending
      // order: [30]=376056000 > [31]=375969600.
      // IntArray isn't sorted in descending
      // order: [0]=373377600 < [1]=373464000.
    } catch (Exception e) {
      Test.knownProblem(
          "https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/PH2/sstd/1day time axis isn't in sorted order. "
              + "I've reported to Roy repeatedly. Problem solved then reoccurs.",
          e);
    }

    // .dds -- error only occurs on .nc
    userDapQuery = "";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            eddGrid.className() + "scale1offset0",
            ".dds");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    String2.log(results);
    expected = // source is byte, with double scale_factor=1 and add_offset=0
        "Dataset {\n"
            + "  Float64 time[time = 11236];\n"
            + "  Float32 latitude[latitude = 4320];\n"
            + "  Float32 longitude[longitude = 8640];\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float64 wind_speed[time = 11236][latitude = 4320][longitude = 8640];\n"
            + // double!
            "    MAPS:\n"
            + "      Float64 time[time = 11236];\n"
            + "      Float32 latitude[latitude = 4320];\n"
            + "      Float32 longitude[longitude = 8640];\n"
            + "  } wind_speed;\n"
            + "} testDAPnodcPH2sstd1day;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .nc -- error only occurs on .nc
    userDapQuery =
        "wind_speed[(2012-12-31T12:00:00Z)][(89.97918):1000:(-89.97916)][(-179.9792):1000:(179.9792)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            eddGrid.className() + "scale1offset0",
            ".nc");
    results = NcHelper.ncdump(EDStatic.fullTestCacheDirectory + tName, "");
    expected = // source is byte, with double scale_factor=1 and add_offset=0
        "  data:\n"
            + "    time = \n"
            + "      {1.3569552E9}\n"
            + "    latitude = \n"
            + "      {89.97918, 48.312508, 6.645836, -35.02083, -76.6875}\n"
            + "    longitude = \n"
            + "      {-179.97917, -138.3125, -96.645836, -54.97917, -13.3125, 28.354172, 70.02083, 111.687515, 153.35417}\n"
            + "    wind_speed = \n"
            + "      {\n"
            + "        {\n"
            + "          {14.0, 10.0, 6.0, 5.0, 5.0, 7.0, 10.0, 14.0, 15.0},\n"
            + "          {9.0, 14.0, 3.0, 9.0, 10.0, 4.0, 4.0, 7.0, 25.0},\n"
            + "          {10.0, 9.0, 3.0, 5.0, 4.0, 3.0, 5.0, 9.0, 10.0},\n"
            + "          {8.0, 10.0, 4.0, 3.0, 9.0, 6.0, 10.0, 7.0, 12.0},\n"
            + "          {1.0, 3.0, 3.0, 3.0, 5.0, 4.0, 1.0, 10.0, 12.0}\n"
            + "        }\n"
            + "      }\n"
            + "}\n";
    int po = results.indexOf(expected.substring(0, 40));
    Test.ensureEqual(results.substring(po), expected, "\nresults=\n" + results);
  }

  /**
   * The tos.comment attribute is json-like encoded. This makes sure opendapHelper.getAttributes
   * uses attributes.fromNccsv().
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testFromNccsv() throws Throwable {

    // testVerboseOn();
    int language = 0;
    // don't test local dataset because of dns/numericIP problems
    // this dataset is good test because it has several dimension combos
    String url =
        "https://opendap.jpl.nasa.gov/opendap/allData/amsre/L3/sst_1deg_1mo/tos_AMSRE_L3_v7_200206-201012.nc";
    String2.log("\n*** EDDGridFromDap.testFromJson");

    String expected =
        "<dataset type=\"EDDGridFromDap\" datasetID=\"nasa_jpl_6f13_e4f4_fe70\" active=\"true\">\n"
            + "    <sourceUrl>https://opendap.jpl.nasa.gov/opendap/allData/amsre/L3/sst_1deg_1mo/tos_AMSRE_L3_v7_200206-201012.nc</sourceUrl>\n"
            + "    <reloadEveryNMinutes>43200</reloadEveryNMinutes>\n"
            + "    <!-- sourceAttributes>\n"
            + "        <att name=\"cmor_version\">2.5.3</att>\n"
            + "        <att name=\"contact\">support@remss.com</att>\n"
            + "        <att name=\"Conventions\">CF-1.4</att>\n"
            + "        <att name=\"creation_date\">2011-10-25T18:51:27Z</att>\n"
            + "        <att name=\"data_structure\">grid</att>\n"
            + "        <att name=\"experiment_id\">obs</att>\n"
            + "        <att name=\"frequency\">mon</att>\n"
            + "        <att name=\"history\">2011-10-25T18:51:27Z CMOR rewrote data to comply with CF standards and obs4MIPs requirements.</att>\n"
            + "        <att name=\"institute_id\">REMSS</att>\n"
            + "        <att name=\"institution\">Remote Sensing Systems</att>\n"
            + "        <att name=\"instrument\">AMSRE</att>\n"
            + "        <att name=\"mip_specs\">CMIP5</att>\n"
            + "        <att name=\"model_id\">Obs-AMSRE</att>\n"
            + "        <att name=\"modeling_realm\">ocean</att>\n"
            + "        <att name=\"obs_project\">AMSRE</att>\n"
            + "        <att name=\"processing_level\">L3</att>\n"
            + "        <att name=\"processing_version\">v7</att>\n"
            + "        <att name=\"product\">observations</att>\n"
            + "        <att name=\"project_id\">obs4MIPs</att>\n"
            + "        <att name=\"realm\">ocean</att>\n"
            + "        <att name=\"source\">Sea Surface Temperature from AMSR-E onboard AQUA.</att>\n"
            + "        <att name=\"source_id\">AMSRE</att>\n"
            + "        <att name=\"source_type\">satellite_retrieval</att>\n"
            + "        <att name=\"table_id\">Table Omon_obs (31 January 2011) 3852a2b3aff8bddd40fa764d07bb2bdb</att>\n"
            + "        <att name=\"title\">Obs-AMSRE model output prepared for obs4MIPs NASA-JPL observation</att>\n"
            + "        <att name=\"tracking_id\">2e41e817-3a79-485a-8336-7993544aee62</att>\n"
            + "    </sourceAttributes -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cdm_data_type\">Grid</att>\n"
            + "        <att name=\"Conventions\">CF-1.10, COARDS, ACDD-1.3</att>\n"
            + "        <att name=\"creator_email\">support@remss.com</att>\n"
            + "        <att name=\"creator_name\">Remote Sensing Systems</att>\n"
            + "        <att name=\"creator_type\">institution</att>\n"
            + "        <att name=\"creator_url\">https://www.jpl.nasa.gov/</att>\n"
            + "        <att name=\"infoUrl\">https://opendap.jpl.nasa.gov/opendap/allData/amsre/L3/sst_1deg_1mo/contents.html</att>\n"
            + "        <att name=\"keywords\">advanced, amsr, amsr-e, amsre, data, earth, Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature, eos, intercomparisons, jet, jpl, laboratory, latitude, longitude, microwave, model, msr, nasa, nasa-jpl, obs, obs-amsre, obs4mips, observation, observations, ocean, oceans, output, prepared, propulsion, radiometer, remote, scanning, science, sea, sea_surface_temperature, sensing, surface, systems, temperature, time, tos</att>\n"
            + "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n"
            + "        <att name=\"license\">[standard]</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"summary\">Obs-Advanced Microwave Scanning Radiometer on EOS (AMSRE) model output prepared for Observations for Model Intercomparisons (obs4MIPs) NASA-Jet Propulsion Laboratory (JPL) observation (tos AMSRE L3 v7 200206-201012)</att>\n"
            + "        <att name=\"title\">Obs-AMSRE model output prepared for obs4MIPs NASA-JPL observation, 1.0°, 2002-2010</att>\n"
            + // test of resolution and timeRange
            "    </addAttributes>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>time</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"axis\">T</att>\n"
            + "            <att name=\"bounds\">time_bnds</att>\n"
            + "            <att name=\"calendar\">standard</att>\n"
            + "            <att name=\"long_name\">time</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"units\">days since 2002-01-01</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"bounds\">null</att>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"units\">days since 2002-01-01T00:00:00Z</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>lat</sourceName>\n"
            + "        <destinationName>latitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"axis\">Y</att>\n"
            + "            <att name=\"bounds\">lat_bnds</att>\n"
            + "            <att name=\"long_name\">latitude</att>\n"
            + "            <att name=\"standard_name\">latitude</att>\n"
            + "            <att name=\"units\">degrees_north</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"bounds\">null</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Latitude</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>lon</sourceName>\n"
            + "        <destinationName>longitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"axis\">X</att>\n"
            + "            <att name=\"bounds\">lon_bnds</att>\n"
            + "            <att name=\"long_name\">longitude</att>\n"
            + "            <att name=\"standard_name\">longitude</att>\n"
            + "            <att name=\"units\">degrees_east</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"bounds\">null</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Longitude</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>tos</sourceName>\n"
            + "        <destinationName>tos</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">1.0E20</att>\n"
            + "            <att name=\"associated_files\">baseURL: http://cmip-pcmdi.llnl.gov/CMIP5/dataLocation gridspecFile: gridspec_ocean_fx_Obs-AMSRE_obs_r0i0p0.nc areacello: areacello_fx_Obs-AMSRE_obs_r0i0p0.nc</att>\n"
            + "            <att name=\"cell_measures\">area: areacello</att>\n"
            + "            <att name=\"cell_methods\">time: mean</att>\n"
            +
            // original " <att name=\"comment\">&quot;this may differ from
            // &quot;&quot;surface temperature&quot;&quot; in regions of sea
            // ice.&quot;</att>\n" +
            "            <att name=\"comment\">this may differ from &quot;surface temperature&quot; in regions of sea ice.</att>\n"
            + "            <att name=\"history\">2011-10-25T18:51:27Z altered by CMOR: Converted type from &#39;d&#39; to &#39;f&#39;.</att>\n"
            + "            <att name=\"long_name\">Sea Surface Temperature</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">1.0E20</att>\n"
            + "            <att name=\"original_name\">sea_surface_temperature</att>\n"
            + "            <att name=\"standard_name\">sea_surface_temperature</att>\n"
            + "            <att name=\"units\">K</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">305.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">273.0</att>\n"
            + "            <att name=\"ioos_category\">Temperature</att>\n"
            + "            <att name=\"original_name\">null</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n";

    String results = EDDGridFromDap.generateDatasetsXml(url, null, null, null, -1, null);

    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** This tests crawlThreddsCatalog. */
  @org.junit.jupiter.api.Test
  @TagThredds
  void testCrawlThreddsCatalog() throws Throwable {
    String2.log("\n*** testCrawlThreddsCatalog()");
    int language = 0;
    StringWriter writer;
    String results, expected;

    // test pathRegex -- 2017-11-30 now hard to use and hard to test
    // writer = new StringWriter();
    // results = EDDGridFromDap.crawlThreddsCatalog(
    // "https://oceanwatch.pfeg.noaa.gov/thredds/Satellite/aggregsatBA/ssta/catalog.html",
    // null, ".*(8day).*", null, writer).toNewlineString();
    // expected =
    // "https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/BA/ssta/8day\n";
    // Test.ensureEqual(results, expected, "results=\n" + results);

    // test find several datasets
    try {
      // 2022-07-07 This used to succeed but now fails with netcdf-java 5.5.2 and
      // 5.5.3 because
      // catalogBuilder returns null for url with "remoteCatalogService?catalog=",
      // The solution was to change the URL in crawlThreddsCatalog to be just the
      // remote URL.
      writer = new StringWriter();
      results =
          EDDGridFromDap.crawlThreddsCatalog(
                  "https://oceanwatch.pfeg.noaa.gov/thredds/remoteCatalogService?catalog=https://thredds1.pfeg.noaa.gov/thredds/catalog/Satellite/MPOC/catalog.xml",
                  // was
                  // "https://oceanwatch.pfeg.noaa.gov/thredds/catalog/Satellite/MPOC/catalog.html",
                  null,
                  null,
                  null,
                  writer)
              .toNewlineString();
      expected =
          "https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MPOC/1day\n"
              + "https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MPOC/8day\n"
              + "https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MPOC/mday\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

    } catch (Exception e) {
      Test.knownProblem(
          "2022-07-07 This used to work but now fails with netcdf-java 5.5.2 and 5.5.3 "
              + "because catalogBuilder returns null for remotCatalogService urls. "
              + "I just reported it to netcdf-java developers.",
          e);
    }

    // test negativePathRegex
    writer = new StringWriter();
    results =
        EDDGridFromDap.crawlThreddsCatalog(
                "https://oceanwatch.pfeg.noaa.gov/thredds/catalog/Satellite/aggregsatBA/ssta/catalog.html",
                null,
                null,
                ".*(8day).*",
                writer)
            .toNewlineString();
    expected =
        "https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/BA/ssta/5day\n"
            + "https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/BA/ssta/mday\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test generateDatasetsXml
    writer = new StringWriter();
    results =
        EDDGridFromDap.crawlThreddsCatalog(
                "https://oceanwatch.pfeg.noaa.gov/thredds/catalog/Satellite/aggregsatBA/ssta/catalog.html",
                null,
                null,
                null,
                writer)
            .toNewlineString();
    expected =
        "https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/BA/ssta/5day\n"
            + "https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/BA/ssta/8day\n"
            + "https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/BA/ssta/mday\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // note that it converts to https and uses https for getting info
    results = writer.toString();
    expected =
        "<dataset type=\"EDDGridFromDap\" datasetID=\"noaa_pfeg_de5e_562e_b0ad\" active=\"true\">\n"
            + "    <sourceUrl>https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/BA/ssta/5day</sourceUrl>\n"
            + "    <reloadEveryNMinutes>43200</reloadEveryNMinutes>\n"
            + "    <!-- sourceAttributes>\n"
            + "        <att name=\"acknowledgement\">NOAA NESDIS COASTWATCH, NOAA SWFSC ERD</att>\n"
            + "        <att name=\"cdm_data_type\">Grid</att>\n"
            + "        <att name=\"cols\" type=\"int\">3601</att>\n"
            + "        <att name=\"composite\">true</att>\n"
            + "        <att name=\"contributor_name\">Remote Sensing Systems Inc, JAXA, NASA, OSDPD, CoastWatch</att>\n"
            + "        <att name=\"contributor_role\">Source of level 2 data.</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.0, Unidata Dataset Discovery v1.0, CWHDF</att>\n"
            + "        <att name=\"creator_email\">dave.foley@noaa.gov</att>\n"
            + "        <att name=\"creator_name\">NOAA CoastWatch, West Coast Node</att>\n"
            + "        <att name=\"creator_url\">http://coastwatch.pfel.noaa.gov</att>\n"
            + "        <att name=\"cwhdf_version\">3.4</att>\n"
            + "        <att name=\"date_created\">2014-03-08Z</att>\n"
            + "        <att name=\"date_issued\">2014-03-08Z</att>\n"
            + "        <att name=\"Easternmost_Easting\" type=\"double\">360.0</att>\n"
            + "        <att name=\"et_affine\" type=\"doubleList\">0.0 0.1 0.1 0.0 0.0 -75.0</att>\n"
            + "        <att name=\"gctp_datum\" type=\"int\">12</att>\n"
            + "        <att name=\"gctp_parm\" type=\"doubleList\">0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0</att>\n"
            + "        <att name=\"gctp_sys\" type=\"int\">0</att>\n"
            + "        <att name=\"gctp_zone\" type=\"int\">0</att>\n"
            + "        <att name=\"geospatial_lat_max\" type=\"double\">75.0</att>\n"
            + "        <att name=\"geospatial_lat_min\" type=\"double\">-75.0</att>\n"
            + "        <att name=\"geospatial_lat_resolution\" type=\"double\">0.1</att>\n"
            + "        <att name=\"geospatial_lat_units\">degrees_north</att>\n"
            + "        <att name=\"geospatial_lon_max\" type=\"double\">360.0</att>\n"
            + "        <att name=\"geospatial_lon_min\" type=\"double\">0.0</att>\n"
            + "        <att name=\"geospatial_lon_resolution\" type=\"double\">0.1</att>\n"
            + "        <att name=\"geospatial_lon_units\">degrees_east</att>\n"
            + "        <att name=\"geospatial_vertical_max\" type=\"double\">0.0</att>\n"
            + "        <att name=\"geospatial_vertical_min\" type=\"double\">0.0</att>\n"
            + "        <att name=\"geospatial_vertical_positive\">up</att>\n"
            + "        <att name=\"geospatial_vertical_units\">m</att>\n"
            + "        <att name=\"history\">Remote Sensing Systems Inc, JAXA, NASA, OSDPD, CoastWatch\n"
            + "2014-03-08T23:30:04Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD</att>\n"
            + "        <att name=\"id\">LBAsstaS5day_20140304120000</att>\n"
            + "        <att name=\"institution\">NOAA CoastWatch, West Coast Node</att>\n"
            + "        <att name=\"keywords\">EARTH SCIENCE &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature</att>\n"
            + "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n"
            + "        <att name=\"license\">The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.</att>\n"
            + "        <att name=\"naming_authority\">gov.noaa.pfel.coastwatch</att>\n"
            + "        <att name=\"Northernmost_Northing\" type=\"double\">75.0</att>\n"
            + "        <att name=\"origin\">Remote Sensing Systems Inc, JAXA, NASA, OSDPD, CoastWatch</att>\n"
            + "        <att name=\"pass_date\" type=\"intList\">16131 16132 16133 16134 16135</att>\n"
            + "        <att name=\"polygon_latitude\" type=\"doubleList\">-75.0 75.0 75.0 -75.0 -75.0</att>\n"
            + "        <att name=\"polygon_longitude\" type=\"doubleList\">0.0 0.0 360.0 360.0 0.0</att>\n"
            + "        <att name=\"processing_level\">3</att>\n"
            + "        <att name=\"project\">CoastWatch (http://coastwatch.noaa.gov/)</att>\n"
            + "        <att name=\"projection\">geographic</att>\n"
            + "        <att name=\"projection_type\">mapped</att>\n"
            + "        <att name=\"references\">Blended SST from satellites information: This is an experimental product which blends satellite-derived SST data from multiple platforms using a weighted mean.  Weights are based on the inverse square of the nominal accuracy of each satellite. AMSR_E Processing information: http://www.ssmi.com/amsr/docs/AMSRE_V05_Updates.pdf . AMSR-E Processing reference: Wentz, F.J., C. Gentemann, and P. Ashcroft. 2005. ON-ORBIT CALIBRATION OF AMSR-E AND THE RETRIEVAL OF OCEAN PRODUCTS. Remote Sensing Systems Internal Report. AVHRR Processing Information: http://www.osdpd.noaa.gov/PSB/EPS/CW/coastwatch.html .  AVHRR Processing Reference: Walton C. C., W. G. Pichel, J. F. Sapper, D. A. May. The development and operational application of nonlinear algorithms for the measurement of sea surface temperatures with the NOAA polar-orbiting environmental satellites. J.G.R., 103: (C12) 27999-28012, 1998. Cloudmask reference: Stowe, L. L., P. A. Davis, and E. P. McClain.  Scientific basis and initial evaluation of the CLAVR-1 global clear/cloud classification algorithm for the advanced very high resolution radiometer. J. Atmos. Oceanic Technol., 16, 656-681. 1999. Calibration and Validation: Li, X., W. Pichel, E. Maturi, P. Clemente-Colon, and J. Sapper. Deriving the operational nonlinear multi-channel sea surface temperature algorithm coefficients for NOAA-15 AVHRR/3. International Journal of Remote Sensing, Volume 22, No. 4, 699 - 704, March 2001a. Calibration and Validation: Li, X, W. Pichel, P. Clemente-Colon, V. Krasnopolsky, and J. Sapper. Validation of coastal sea and lake surface temperature measurements derived from NOAA/AVHRR Data. International Journal of Remote Sensing, Vol. 22, No. 7, 1285-1303, 2001b. GOES Imager Processing Information: http://coastwatch.noaa.gov/goes_sst_overview.html .  GOES Imager Processing Reference: Wu, X., W. P. Menzel, and G. S. Wade, 1999. Estimation of sea surface temperatures using GOES-8/9 radiance measurements, Bull. Amer. Meteor. Soc., 80, 1127-1138.  MODIS Aqua Processing Information: http://oceancolor.gsfc.nasa.gov/DOCS/modis_sst/ . MODIS Aqua Processing reference: Not Available.</att>\n"
            + "        <att name=\"rows\" type=\"int\">1501</att>\n"
            + "        <att name=\"satellite\">Aqua, GOES, POES</att>\n"
            + "        <att name=\"sensor\">AMSR-E, MODIS, Imager, AVHRR</att>\n"
            + "        <att name=\"source\">satellite observation: Aqua, GOES, POES, AMSR-E, MODIS, Imager, AVHRR</att>\n"
            + "        <att name=\"Southernmost_Northing\" type=\"double\">-75.0</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF-1.0</att>\n"
            + "        <att name=\"start_time\" type=\"doubleList\">0.0 0.0 0.0 0.0 0.0</att>\n"
            + "        <att name=\"summary\">NOAA OceanWatch provides a blended sea surface temperature (SST) products derived from both microwave and infrared sensors carried on multiple platforms.  The microwave instruments can measure ocean temperatures even in the presence of clouds, though the resolution is a bit coarse when considering features typical of the coastal environment.  These are complemented by the relatively fine measurements of infrared sensors.  The blended data are provided at moderate spatial resolution (0.1 degrees) for the Global Ocean.  Measurements are gathered by Japan&#39;s Advanced Microwave Scanning Radiometer (AMSR-E) instrument, a passive radiance sensor carried aboard NASA&#39;s Aqua spacecraft, NOAA&#39;s Advanced Very High Resolution Radiometer, NOAA GOES Imager, and NASA&#39;s Moderate Resolution Imaging Spectrometer (MODIS). THIS IS AN EXPERIMENTAL PRODUCT: intended strictly for scientific evaluation by professional marine scientists.</att>\n"
            + "        <att name=\"time_coverage_end\">2014-03-07T00:00:00Z</att>\n"
            + "        <att name=\"time_coverage_start\">2014-03-02T00:00:00Z</att>\n"
            + "        <att name=\"title\">SST, Blended, 0.1 degrees, Global, EXPERIMENTAL</att>\n"
            + "        <att name=\"Westernmost_Easting\" type=\"double\">0.0</att>\n"
            + "    </sourceAttributes -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cols\">null</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n"
            + "        <att name=\"creator_email\">erd.data@noaa.gov</att>\n"
            + "        <att name=\"creator_name\">Remote Sensing Systems Inc, JAXA, NASA, OSDPD, CoastWatch</att>\n"
            + "        <att name=\"creator_type\">institution</att>\n"
            + "        <att name=\"creator_url\">https://coastwatch.pfeg.noaa.gov</att>\n"
            + "        <att name=\"cwhdf_version\">null</att>\n"
            + "        <att name=\"date_created\">2014-03-08</att>\n"
            + "        <att name=\"date_issued\">2014-03-08</att>\n"
            + "        <att name=\"et_affine\">null</att>\n"
            + "        <att name=\"gctp_datum\">null</att>\n"
            + "        <att name=\"gctp_parm\">null</att>\n"
            + "        <att name=\"gctp_sys\">null</att>\n"
            + "        <att name=\"gctp_zone\">null</att>\n"
            + "        <att name=\"id\">satellite/BA/ssta/5day</att>\n"
            + "        <att name=\"infoUrl\">https://coastwatch.pfeg.noaa.gov/infog/BA_ssta_las.html</att>\n"
            + "        <att name=\"institution\">NOAA CoastWatch WCN</att>\n"
            + "        <att name=\"keywords\">5-day, advanced, altitude, amsr, amsr-e, avhrr, BAssta, blended, coast, coastwatch, data, day, degrees, earth, Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature, environmental, eos, experimental, geostationary, global, goes, high, imaging, infrared, latitude, longitude, microwave, moderate, modis, msr, noaa, node, ocean, oceans, operational, radiometer, resolution, satellite, scanning, science, sea, sea_surface_temperature, spectroradiometer, sst, surface, temperature, time, very, vhrr, wcn, west</att>\n"
            + "        <att name=\"naming_authority\">gov.noaa.pfeg.coastwatch</att>\n"
            + "        <att name=\"pass_date\">null</att>\n"
            + "        <att name=\"polygon_latitude\">null</att>\n"
            + "        <att name=\"polygon_longitude\">null</att>\n"
            + "        <att name=\"project\">CoastWatch (https://coastwatch.noaa.gov/)</att>\n"
            + "        <att name=\"publisher_email\">erd.data@noaa.gov</att>\n"
            + "        <att name=\"publisher_name\">NOAA NMFS SWFSC ERD</att>\n"
            + "        <att name=\"publisher_type\">institution</att>\n"
            + "        <att name=\"publisher_url\">https://coastwatch.pfeg.noaa.gov</att>\n"
            + "        <att name=\"references\">Blended SST from satellites information: This is an experimental product which blends satellite-derived SST data from multiple platforms using a weighted mean.  Weights are based on the inverse square of the nominal accuracy of each satellite. AMSR_E Processing information: https://www.eorc.jaxa.jp/en/distribution/standard_dataset/pdf/amsr-e_handbook_e.pdf . AMSR-E Processing reference: Wentz, F.J., C. Gentemann, and P. Ashcroft. 2005. ON-ORBIT CALIBRATION OF AMSR-E AND THE RETRIEVAL OF OCEAN PRODUCTS. Remote Sensing Systems Internal Report. AVHRR Processing Information: http://www.osdpd.noaa.gov/PSB/EPS/CW/coastwatch.html .  AVHRR Processing Reference: Walton C. C., W. G. Pichel, J. F. Sapper, D. A. May. The development and operational application of nonlinear algorithms for the measurement of sea surface temperatures with the NOAA polar-orbiting environmental satellites. J.G.R., 103: (C12) 27999-28012, 1998. Cloudmask reference: Stowe, L. L., P. A. Davis, and E. P. McClain.  Scientific basis and initial evaluation of the CLAVR-1 global clear/cloud classification algorithm for the advanced very high resolution radiometer. J. Atmos. Oceanic Technol., 16, 656-681. 1999. Calibration and Validation: Li, X., W. Pichel, E. Maturi, P. Clemente-Colon, and J. Sapper. Deriving the operational nonlinear multi-channel sea surface temperature algorithm coefficients for NOAA-15 AVHRR/3. International Journal of Remote Sensing, Volume 22, No. 4, 699 - 704, March 2001a. Calibration and Validation: Li, X, W. Pichel, P. Clemente-Colon, V. Krasnopolsky, and J. Sapper. Validation of coastal sea and lake surface temperature measurements derived from NOAA/AVHRR Data. International Journal of Remote Sensing, Vol. 22, No. 7, 1285-1303, 2001b. GOES Imager Processing Information: https://coastwatch.noaa.gov/goes_sst_overview.html .  GOES Imager Processing Reference: Wu, X., W. P. Menzel, and G. S. Wade, 1999. Estimation of sea surface temperatures using GOES-8/9 radiance measurements, Bull. Amer. Meteor. Soc., 80, 1127-1138.  MODIS Aqua Processing Information: https://oceancolor.gsfc.nasa.gov/DOCS/modis_sst/ . MODIS Aqua Processing reference: Not Available.</att>\n"
            + "        <att name=\"rows\">null</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"start_time\">null</att>\n"
            + "        <att name=\"summary\">SST, Blended, 0.1 degrees, Global, EXPERIMENTAL. NOAA OceanWatch provides a blended sea surface temperature (SST) products derived from both microwave and infrared sensors carried on multiple platforms. The microwave instruments can measure ocean temperatures even in the presence of clouds, though the resolution is a bit coarse when considering features typical of the coastal environment. These are complemented by the relatively fine measurements of infrared sensors. The blended data are provided at moderate spatial resolution (0.1 degrees) for the Global Ocean. Measurements are gathered by Japan&#39;s Advanced Microwave Scanning Radiometer (Advanced Microwave Scanning Radiometer on EOS (AMSR-E)) instrument, a passive radiance sensor carried aboard NASA&#39;s Aqua spacecraft, NOAA&#39;s Advanced Very High Resolution Radiometer, NOAA Geostationary Operational Environmental Satellite (GOES) Imager, and NASA&#39;s Moderate Resolution Imaging Spectrometer (Moderate Resolution Imaging Spectroradiometer (MODIS)). THIS IS AN EXPERIMENTAL PRODUCT: intended strictly for scientific evaluation by professional marine scientists.</att>\n"
            + "        <att name=\"title\">SST, Blended, 0.1 degrees, Global, EXPERIMENTAL (SST, Blended, Global, EXPERIMENTAL, 5-day), 2002-2014</att>\n"
            + // important test of adding timeRange
            "    </addAttributes>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>time</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Time</att>\n"
            + "            <att name=\"actual_range\" type=\"doubleList\">1.3939344E9 1.3939344E9</att>\n"
            + "            <att name=\"axis\">T</att>\n"
            + "            <att name=\"fraction_digits\" type=\"int\">0</att>\n"
            + "            <att name=\"long_name\">Centered Time</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>altitude</sourceName>\n"
            + "        <destinationName>altitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Height</att>\n"
            + "            <att name=\"_CoordinateZisPositive\">up</att>\n"
            + "            <att name=\"actual_range\" type=\"doubleList\">0.0 0.0</att>\n"
            + "            <att name=\"axis\">Z</att>\n"
            + "            <att name=\"fraction_digits\" type=\"int\">0</att>\n"
            + "            <att name=\"long_name\">Altitude</att>\n"
            + "            <att name=\"positive\">up</att>\n"
            + "            <att name=\"standard_name\">altitude</att>\n"
            + "            <att name=\"units\">m</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>lat</sourceName>\n"
            + "        <destinationName>latitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Lat</att>\n"
            + "            <att name=\"actual_range\" type=\"doubleList\">-75.0 75.0</att>\n"
            + "            <att name=\"axis\">Y</att>\n"
            + "            <att name=\"coordsys\">geographic</att>\n"
            + "            <att name=\"fraction_digits\" type=\"int\">1</att>\n"
            + "            <att name=\"long_name\">Latitude</att>\n"
            + "            <att name=\"point_spacing\">even</att>\n"
            + "            <att name=\"standard_name\">latitude</att>\n"
            + "            <att name=\"units\">degrees_north</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>lon</sourceName>\n"
            + "        <destinationName>longitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Lon</att>\n"
            + "            <att name=\"actual_range\" type=\"doubleList\">0.0 360.0</att>\n"
            + "            <att name=\"axis\">X</att>\n"
            + "            <att name=\"coordsys\">geographic</att>\n"
            + "            <att name=\"fraction_digits\" type=\"int\">1</att>\n"
            + "            <att name=\"long_name\">Longitude</att>\n"
            + "            <att name=\"point_spacing\">even</att>\n"
            + "            <att name=\"standard_name\">longitude</att>\n"
            + "            <att name=\"units\">degrees_east</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>BAssta</sourceName>\n"
            + "        <destinationName>BAssta</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n"
            + "            <att name=\"actual_range\" type=\"floatList\">-1.995 34.005</att>\n"
            + "            <att name=\"coordsys\">geographic</att>\n"
            + "            <att name=\"fraction_digits\" type=\"int\">1</att>\n"
            + "            <att name=\"long_name\">SST, Blended, 0.1 degrees, Global, EXPERIMENTAL</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n"
            + "            <att name=\"numberOfObservations\" type=\"int\">2059696</att>\n"
            + "            <att name=\"percentCoverage\" type=\"double\">0.38106521968784673</att>\n"
            + "            <att name=\"standard_name\">sea_surface_temperature</att>\n"
            + "            <att name=\"units\">degree_C</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Temperature</att>\n"
            + "            <att name=\"numberOfObservations\">null</att>\n"
            + "            <att name=\"percentCoverage\">null</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n"
            + "<dataset type=\"EDDGridFromDap\" datasetID=\"noaa_pfeg_86d1_17e1_363f\" active=\"true\">\n"
            + "    <sourceUrl>https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/BA/ssta/8day</sourceUrl>\n"
            + "    <reloadEveryNMinutes>43200</reloadEveryNMinutes>\n"
            + "    <!-- sourceAttributes>\n"
            + "        <att name=\"acknowledgement\">NOAA NESDIS COASTWATCH, NOAA SWFSC ERD</att>\n"
            + "        <att name=\"cdm_data_type\">Grid</att>\n"
            + "        <att name=\"cols\" type=\"int\">3601</att>\n"
            + "        <att name=\"composite\">true</att>\n"
            + "        <att name=\"contributor_name\">Remote Sensing Systems Inc, JAXA, NASA, OSDPD, CoastWatch</att>\n"
            + "        <att name=\"contributor_role\">Source of level 2 data.</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.0, Unidata Dataset Discovery v1.0, CWHDF</att>\n"
            + "        <att name=\"creator_email\">dave.foley@noaa.gov</att>\n"
            + "        <att name=\"creator_name\">NOAA CoastWatch, West Coast Node</att>\n"
            + "        <att name=\"creator_url\">http://coastwatch.pfel.noaa.gov</att>\n"
            + "        <att name=\"cwhdf_version\">3.4</att>\n"
            + "        <att name=\"date_created\">2014-03-08Z</att>\n"
            + "        <att name=\"date_issued\">2014-03-08Z</att>\n"
            + "        <att name=\"Easternmost_Easting\" type=\"double\">360.0</att>\n"
            + "        <att name=\"et_affine\" type=\"doubleList\">0.0 0.1 0.1 0.0 0.0 -75.0</att>\n"
            + "        <att name=\"gctp_datum\" type=\"int\">12</att>\n"
            + "        <att name=\"gctp_parm\" type=\"doubleList\">0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0</att>\n"
            + "        <att name=\"gctp_sys\" type=\"int\">0</att>\n"
            + "        <att name=\"gctp_zone\" type=\"int\">0</att>\n"
            + "        <att name=\"geospatial_lat_max\" type=\"double\">75.0</att>\n"
            + "        <att name=\"geospatial_lat_min\" type=\"double\">-75.0</att>\n"
            + "        <att name=\"geospatial_lat_resolution\" type=\"double\">0.1</att>\n"
            + "        <att name=\"geospatial_lat_units\">degrees_north</att>\n"
            + "        <att name=\"geospatial_lon_max\" type=\"double\">360.0</att>\n"
            + "        <att name=\"geospatial_lon_min\" type=\"double\">0.0</att>\n"
            + "        <att name=\"geospatial_lon_resolution\" type=\"double\">0.1</att>\n"
            + "        <att name=\"geospatial_lon_units\">degrees_east</att>\n"
            + "        <att name=\"geospatial_vertical_max\" type=\"double\">0.0</att>\n"
            + "        <att name=\"geospatial_vertical_min\" type=\"double\">0.0</att>\n"
            + "        <att name=\"geospatial_vertical_positive\">up</att>\n"
            + "        <att name=\"geospatial_vertical_units\">m</att>\n"
            + "        <att name=\"history\">Remote Sensing Systems Inc, JAXA, NASA, OSDPD, CoastWatch\n"
            + "2014-03-08T23:35:43Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD</att>\n"
            + "        <att name=\"id\">LBAsstaS8day_20140303000000</att>\n"
            + "        <att name=\"institution\">NOAA CoastWatch, West Coast Node</att>\n"
            + "        <att name=\"keywords\">EARTH SCIENCE &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature</att>\n"
            + "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n"
            + "        <att name=\"license\">The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.</att>\n"
            + "        <att name=\"naming_authority\">gov.noaa.pfel.coastwatch</att>\n"
            + "        <att name=\"Northernmost_Northing\" type=\"double\">75.0</att>\n"
            + "        <att name=\"origin\">Remote Sensing Systems Inc, JAXA, NASA, OSDPD, CoastWatch</att>\n"
            + "        <att name=\"pass_date\" type=\"intList\">16128 16129 16130 16131 16132 16133 16134 16135</att>\n"
            + "        <att name=\"polygon_latitude\" type=\"doubleList\">-75.0 75.0 75.0 -75.0 -75.0</att>\n"
            + "        <att name=\"polygon_longitude\" type=\"doubleList\">0.0 0.0 360.0 360.0 0.0</att>\n"
            + "        <att name=\"processing_level\">3</att>\n"
            + "        <att name=\"project\">CoastWatch (http://coastwatch.noaa.gov/)</att>\n"
            + "        <att name=\"projection\">geographic</att>\n"
            + "        <att name=\"projection_type\">mapped</att>\n"
            + "        <att name=\"references\">Blended SST from satellites information: This is an experimental product which blends satellite-derived SST data from multiple platforms using a weighted mean.  Weights are based on the inverse square of the nominal accuracy of each satellite. AMSR_E Processing information: http://www.ssmi.com/amsr/docs/AMSRE_V05_Updates.pdf . AMSR-E Processing reference: Wentz, F.J., C. Gentemann, and P. Ashcroft. 2005. ON-ORBIT CALIBRATION OF AMSR-E AND THE RETRIEVAL OF OCEAN PRODUCTS. Remote Sensing Systems Internal Report. AVHRR Processing Information: http://www.osdpd.noaa.gov/PSB/EPS/CW/coastwatch.html .  AVHRR Processing Reference: Walton C. C., W. G. Pichel, J. F. Sapper, D. A. May. The development and operational application of nonlinear algorithms for the measurement of sea surface temperatures with the NOAA polar-orbiting environmental satellites. J.G.R., 103: (C12) 27999-28012, 1998. Cloudmask reference: Stowe, L. L., P. A. Davis, and E. P. McClain.  Scientific basis and initial evaluation of the CLAVR-1 global clear/cloud classification algorithm for the advanced very high resolution radiometer. J. Atmos. Oceanic Technol., 16, 656-681. 1999. Calibration and Validation: Li, X., W. Pichel, E. Maturi, P. Clemente-Colon, and J. Sapper. Deriving the operational nonlinear multi-channel sea surface temperature algorithm coefficients for NOAA-15 AVHRR/3. International Journal of Remote Sensing, Volume 22, No. 4, 699 - 704, March 2001a. Calibration and Validation: Li, X, W. Pichel, P. Clemente-Colon, V. Krasnopolsky, and J. Sapper. Validation of coastal sea and lake surface temperature measurements derived from NOAA/AVHRR Data. International Journal of Remote Sensing, Vol. 22, No. 7, 1285-1303, 2001b. GOES Imager Processing Information: http://coastwatch.noaa.gov/goes_sst_overview.html .  GOES Imager Processing Reference: Wu, X., W. P. Menzel, and G. S. Wade, 1999. Estimation of sea surface temperatures using GOES-8/9 radiance measurements, Bull. Amer. Meteor. Soc., 80, 1127-1138.  MODIS Aqua Processing Information: http://oceancolor.gsfc.nasa.gov/DOCS/modis_sst/ . MODIS Aqua Processing reference: Not Available.</att>\n"
            + "        <att name=\"rows\" type=\"int\">1501</att>\n"
            + "        <att name=\"satellite\">Aqua, GOES, POES</att>\n"
            + "        <att name=\"sensor\">AMSR-E, MODIS, Imager, AVHRR</att>\n"
            + "        <att name=\"source\">satellite observation: Aqua, GOES, POES, AMSR-E, MODIS, Imager, AVHRR</att>\n"
            + "        <att name=\"Southernmost_Northing\" type=\"double\">-75.0</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF-1.0</att>\n"
            + "        <att name=\"start_time\" type=\"doubleList\">0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0</att>\n"
            + "        <att name=\"summary\">NOAA OceanWatch provides a blended sea surface temperature (SST) products derived from both microwave and infrared sensors carried on multiple platforms.  The microwave instruments can measure ocean temperatures even in the presence of clouds, though the resolution is a bit coarse when considering features typical of the coastal environment.  These are complemented by the relatively fine measurements of infrared sensors.  The blended data are provided at moderate spatial resolution (0.1 degrees) for the Global Ocean.  Measurements are gathered by Japan&#39;s Advanced Microwave Scanning Radiometer (AMSR-E) instrument, a passive radiance sensor carried aboard NASA&#39;s Aqua spacecraft, NOAA&#39;s Advanced Very High Resolution Radiometer, NOAA GOES Imager, and NASA&#39;s Moderate Resolution Imaging Spectrometer (MODIS). THIS IS AN EXPERIMENTAL PRODUCT: intended strictly for scientific evaluation by professional marine scientists.</att>\n"
            + "        <att name=\"time_coverage_end\">2014-03-07T00:00:00Z</att>\n"
            + "        <att name=\"time_coverage_start\">2014-02-27T00:00:00Z</att>\n"
            + "        <att name=\"title\">SST, Blended, 0.1 degrees, Global, EXPERIMENTAL</att>\n"
            + "        <att name=\"Westernmost_Easting\" type=\"double\">0.0</att>\n"
            + "    </sourceAttributes -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cols\">null</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n"
            + "        <att name=\"creator_email\">erd.data@noaa.gov</att>\n"
            + "        <att name=\"creator_name\">Remote Sensing Systems Inc, JAXA, NASA, OSDPD, CoastWatch</att>\n"
            + "        <att name=\"creator_type\">institution</att>\n"
            + "        <att name=\"creator_url\">https://coastwatch.pfeg.noaa.gov</att>\n"
            + "        <att name=\"cwhdf_version\">null</att>\n"
            + "        <att name=\"date_created\">2014-03-08</att>\n"
            + "        <att name=\"date_issued\">2014-03-08</att>\n"
            + "        <att name=\"et_affine\">null</att>\n"
            + "        <att name=\"gctp_datum\">null</att>\n"
            + "        <att name=\"gctp_parm\">null</att>\n"
            + "        <att name=\"gctp_sys\">null</att>\n"
            + "        <att name=\"gctp_zone\">null</att>\n"
            + "        <att name=\"id\">satellite/BA/ssta/8day</att>\n"
            + "        <att name=\"infoUrl\">https://coastwatch.pfeg.noaa.gov/infog/BA_ssta_las.html</att>\n"
            + "        <att name=\"institution\">NOAA CoastWatch WCN</att>\n"
            + "        <att name=\"keywords\">8-day, advanced, altitude, amsr, amsr-e, avhrr, BAssta, blended, coast, coastwatch, data, day, degrees, earth, Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature, environmental, eos, experimental, geostationary, global, goes, high, imaging, infrared, latitude, longitude, microwave, moderate, modis, msr, noaa, node, ocean, oceans, operational, radiometer, resolution, satellite, scanning, science, sea, sea_surface_temperature, spectroradiometer, sst, surface, temperature, time, very, vhrr, wcn, west</att>\n"
            + "        <att name=\"naming_authority\">gov.noaa.pfeg.coastwatch</att>\n"
            + "        <att name=\"pass_date\">null</att>\n"
            + "        <att name=\"polygon_latitude\">null</att>\n"
            + "        <att name=\"polygon_longitude\">null</att>\n"
            + "        <att name=\"project\">CoastWatch (https://coastwatch.noaa.gov/)</att>\n"
            + "        <att name=\"publisher_email\">erd.data@noaa.gov</att>\n"
            + "        <att name=\"publisher_name\">NOAA NMFS SWFSC ERD</att>\n"
            + "        <att name=\"publisher_type\">institution</att>\n"
            + "        <att name=\"publisher_url\">https://coastwatch.pfeg.noaa.gov</att>\n"
            + "        <att name=\"references\">Blended SST from satellites information: This is an experimental product which blends satellite-derived SST data from multiple platforms using a weighted mean.  Weights are based on the inverse square of the nominal accuracy of each satellite. AMSR_E Processing information: https://www.eorc.jaxa.jp/en/distribution/standard_dataset/pdf/amsr-e_handbook_e.pdf . AMSR-E Processing reference: Wentz, F.J., C. Gentemann, and P. Ashcroft. 2005. ON-ORBIT CALIBRATION OF AMSR-E AND THE RETRIEVAL OF OCEAN PRODUCTS. Remote Sensing Systems Internal Report. AVHRR Processing Information: http://www.osdpd.noaa.gov/PSB/EPS/CW/coastwatch.html .  AVHRR Processing Reference: Walton C. C., W. G. Pichel, J. F. Sapper, D. A. May. The development and operational application of nonlinear algorithms for the measurement of sea surface temperatures with the NOAA polar-orbiting environmental satellites. J.G.R., 103: (C12) 27999-28012, 1998. Cloudmask reference: Stowe, L. L., P. A. Davis, and E. P. McClain.  Scientific basis and initial evaluation of the CLAVR-1 global clear/cloud classification algorithm for the advanced very high resolution radiometer. J. Atmos. Oceanic Technol., 16, 656-681. 1999. Calibration and Validation: Li, X., W. Pichel, E. Maturi, P. Clemente-Colon, and J. Sapper. Deriving the operational nonlinear multi-channel sea surface temperature algorithm coefficients for NOAA-15 AVHRR/3. International Journal of Remote Sensing, Volume 22, No. 4, 699 - 704, March 2001a. Calibration and Validation: Li, X, W. Pichel, P. Clemente-Colon, V. Krasnopolsky, and J. Sapper. Validation of coastal sea and lake surface temperature measurements derived from NOAA/AVHRR Data. International Journal of Remote Sensing, Vol. 22, No. 7, 1285-1303, 2001b. GOES Imager Processing Information: https://coastwatch.noaa.gov/goes_sst_overview.html .  GOES Imager Processing Reference: Wu, X., W. P. Menzel, and G. S. Wade, 1999. Estimation of sea surface temperatures using GOES-8/9 radiance measurements, Bull. Amer. Meteor. Soc., 80, 1127-1138.  MODIS Aqua Processing Information: https://oceancolor.gsfc.nasa.gov/DOCS/modis_sst/ . MODIS Aqua Processing reference: Not Available.</att>\n"
            + "        <att name=\"rows\">null</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"start_time\">null</att>\n"
            + "        <att name=\"summary\">SST, Blended, 0.1 degrees, Global, EXPERIMENTAL. NOAA OceanWatch provides a blended sea surface temperature (SST) products derived from both microwave and infrared sensors carried on multiple platforms. The microwave instruments can measure ocean temperatures even in the presence of clouds, though the resolution is a bit coarse when considering features typical of the coastal environment. These are complemented by the relatively fine measurements of infrared sensors. The blended data are provided at moderate spatial resolution (0.1 degrees) for the Global Ocean. Measurements are gathered by Japan&#39;s Advanced Microwave Scanning Radiometer (Advanced Microwave Scanning Radiometer on EOS (AMSR-E)) instrument, a passive radiance sensor carried aboard NASA&#39;s Aqua spacecraft, NOAA&#39;s Advanced Very High Resolution Radiometer, NOAA Geostationary Operational Environmental Satellite (GOES) Imager, and NASA&#39;s Moderate Resolution Imaging Spectrometer (Moderate Resolution Imaging Spectroradiometer (MODIS)). THIS IS AN EXPERIMENTAL PRODUCT: intended strictly for scientific evaluation by professional marine scientists.</att>\n"
            + "        <att name=\"title\">SST, Blended, 0.1 degrees, Global, EXPERIMENTAL (SST, Blended, Global, EXPERIMENTAL, 8-day), 2006-2014</att>\n"
            + "    </addAttributes>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>time</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Time</att>\n"
            + "            <att name=\"actual_range\" type=\"doubleList\">1.3938048E9 1.3938048E9</att>\n"
            + "            <att name=\"axis\">T</att>\n"
            + "            <att name=\"fraction_digits\" type=\"int\">0</att>\n"
            + "            <att name=\"long_name\">Centered Time</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>altitude</sourceName>\n"
            + "        <destinationName>altitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Height</att>\n"
            + "            <att name=\"_CoordinateZisPositive\">up</att>\n"
            + "            <att name=\"actual_range\" type=\"doubleList\">0.0 0.0</att>\n"
            + "            <att name=\"axis\">Z</att>\n"
            + "            <att name=\"fraction_digits\" type=\"int\">0</att>\n"
            + "            <att name=\"long_name\">Altitude</att>\n"
            + "            <att name=\"positive\">up</att>\n"
            + "            <att name=\"standard_name\">altitude</att>\n"
            + "            <att name=\"units\">m</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>lat</sourceName>\n"
            + "        <destinationName>latitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Lat</att>\n"
            + "            <att name=\"actual_range\" type=\"doubleList\">-75.0 75.0</att>\n"
            + "            <att name=\"axis\">Y</att>\n"
            + "            <att name=\"coordsys\">geographic</att>\n"
            + "            <att name=\"fraction_digits\" type=\"int\">1</att>\n"
            + "            <att name=\"long_name\">Latitude</att>\n"
            + "            <att name=\"point_spacing\">even</att>\n"
            + "            <att name=\"standard_name\">latitude</att>\n"
            + "            <att name=\"units\">degrees_north</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>lon</sourceName>\n"
            + "        <destinationName>longitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Lon</att>\n"
            + "            <att name=\"actual_range\" type=\"doubleList\">0.0 360.0</att>\n"
            + "            <att name=\"axis\">X</att>\n"
            + "            <att name=\"coordsys\">geographic</att>\n"
            + "            <att name=\"fraction_digits\" type=\"int\">1</att>\n"
            + "            <att name=\"long_name\">Longitude</att>\n"
            + "            <att name=\"point_spacing\">even</att>\n"
            + "            <att name=\"standard_name\">longitude</att>\n"
            + "            <att name=\"units\">degrees_east</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>BAssta</sourceName>\n"
            + "        <destinationName>BAssta</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n"
            + "            <att name=\"actual_range\" type=\"floatList\">-1.995 34.005</att>\n"
            + "            <att name=\"coordsys\">geographic</att>\n"
            + "            <att name=\"fraction_digits\" type=\"int\">1</att>\n"
            + "            <att name=\"long_name\">SST, Blended, 0.1 degrees, Global, EXPERIMENTAL</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n"
            + "            <att name=\"numberOfObservations\" type=\"int\">2362817</att>\n"
            + "            <att name=\"percentCoverage\" type=\"double\">0.43714576286363566</att>\n"
            + "            <att name=\"standard_name\">sea_surface_temperature</att>\n"
            + "            <att name=\"units\">degree_C</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Temperature</att>\n"
            + "            <att name=\"numberOfObservations\">null</att>\n"
            + "            <att name=\"percentCoverage\">null</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n"
            + "<dataset type=\"EDDGridFromDap\" datasetID=\"noaa_pfeg_0e99_0cbd_9400\" active=\"true\">\n"
            + "    <sourceUrl>https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/BA/ssta/mday</sourceUrl>\n"
            + "    <reloadEveryNMinutes>43200</reloadEveryNMinutes>\n"
            + "    <!-- sourceAttributes>\n"
            + "        <att name=\"acknowledgement\">NOAA NESDIS COASTWATCH, NOAA SWFSC ERD</att>\n"
            + "        <att name=\"cdm_data_type\">Grid</att>\n"
            + "        <att name=\"cols\" type=\"int\">3601</att>\n"
            + "        <att name=\"composite\">true</att>\n"
            + "        <att name=\"contributor_name\">Remote Sensing Systems Inc, JAXA, NASA, OSDPD, CoastWatch</att>\n"
            + "        <att name=\"contributor_role\">Source of level 2 data.</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.0, Unidata Dataset Discovery v1.0, CWHDF</att>\n"
            + "        <att name=\"creator_email\">dave.foley@noaa.gov</att>\n"
            + "        <att name=\"creator_name\">NOAA CoastWatch, West Coast Node</att>\n"
            + "        <att name=\"creator_url\">http://coastwatch.pfel.noaa.gov</att>\n"
            + "        <att name=\"cwhdf_version\">3.4</att>\n"
            + "        <att name=\"date_created\">2013-12-01Z</att>\n"
            + "        <att name=\"date_issued\">2013-12-01Z</att>\n"
            + "        <att name=\"Easternmost_Easting\" type=\"double\">360.0</att>\n"
            + "        <att name=\"et_affine\" type=\"doubleList\">0.0 0.1 0.1 0.0 0.0 -75.0</att>\n"
            + "        <att name=\"gctp_datum\" type=\"int\">12</att>\n"
            + "        <att name=\"gctp_parm\" type=\"doubleList\">0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0</att>\n"
            + "        <att name=\"gctp_sys\" type=\"int\">0</att>\n"
            + "        <att name=\"gctp_zone\" type=\"int\">0</att>\n"
            + "        <att name=\"geospatial_lat_max\" type=\"double\">75.0</att>\n"
            + "        <att name=\"geospatial_lat_min\" type=\"double\">-75.0</att>\n"
            + "        <att name=\"geospatial_lat_resolution\" type=\"double\">0.1</att>\n"
            + "        <att name=\"geospatial_lat_units\">degrees_north</att>\n"
            + "        <att name=\"geospatial_lon_max\" type=\"double\">360.0</att>\n"
            + "        <att name=\"geospatial_lon_min\" type=\"double\">0.0</att>\n"
            + "        <att name=\"geospatial_lon_resolution\" type=\"double\">0.1</att>\n"
            + "        <att name=\"geospatial_lon_units\">degrees_east</att>\n"
            + "        <att name=\"geospatial_vertical_max\" type=\"double\">0.0</att>\n"
            + "        <att name=\"geospatial_vertical_min\" type=\"double\">0.0</att>\n"
            + "        <att name=\"geospatial_vertical_positive\">up</att>\n"
            + "        <att name=\"geospatial_vertical_units\">m</att>\n"
            + "        <att name=\"history\">Remote Sensing Systems Inc, JAXA, NASA, OSDPD, CoastWatch\n"
            + "2013-12-01T19:15:23Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD</att>\n"
            + "        <att name=\"id\">LBAsstaSmday_20131116000000</att>\n"
            + "        <att name=\"institution\">NOAA CoastWatch, West Coast Node</att>\n"
            + "        <att name=\"keywords\">EARTH SCIENCE &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature</att>\n"
            + "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n"
            + "        <att name=\"license\">The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.</att>\n"
            + "        <att name=\"naming_authority\">gov.noaa.pfel.coastwatch</att>\n"
            + "        <att name=\"Northernmost_Northing\" type=\"double\">75.0</att>\n"
            + "        <att name=\"origin\">Remote Sensing Systems Inc, JAXA, NASA, OSDPD, CoastWatch</att>\n"
            + "        <att name=\"pass_date\" type=\"intList\">16010 16011 16012 16013 16014 16015 16016 16017 16018 16019 16020 16021 16022 16023 16024 16025 16026 16027 16028 16029 16030 16031 16032 16033 16034 16035 16036 16037 16038 16039</att>\n"
            + "        <att name=\"polygon_latitude\" type=\"doubleList\">-75.0 75.0 75.0 -75.0 -75.0</att>\n"
            + "        <att name=\"polygon_longitude\" type=\"doubleList\">0.0 0.0 360.0 360.0 0.0</att>\n"
            + "        <att name=\"processing_level\">3</att>\n"
            + "        <att name=\"project\">CoastWatch (http://coastwatch.noaa.gov/)</att>\n"
            + "        <att name=\"projection\">geographic</att>\n"
            + "        <att name=\"projection_type\">mapped</att>\n"
            + "        <att name=\"references\">Blended SST from satellites information: This is an experimental product which blends satellite-derived SST data from multiple platforms using a weighted mean.  Weights are based on the inverse square of the nominal accuracy of each satellite. AMSR_E Processing information: http://www.ssmi.com/amsr/docs/AMSRE_V05_Updates.pdf . AMSR-E Processing reference: Wentz, F.J., C. Gentemann, and P. Ashcroft. 2005. ON-ORBIT CALIBRATION OF AMSR-E AND THE RETRIEVAL OF OCEAN PRODUCTS. Remote Sensing Systems Internal Report. AVHRR Processing Information: http://www.osdpd.noaa.gov/PSB/EPS/CW/coastwatch.html .  AVHRR Processing Reference: Walton C. C., W. G. Pichel, J. F. Sapper, D. A. May. The development and operational application of nonlinear algorithms for the measurement of sea surface temperatures with the NOAA polar-orbiting environmental satellites. J.G.R., 103: (C12) 27999-28012, 1998. Cloudmask reference: Stowe, L. L., P. A. Davis, and E. P. McClain.  Scientific basis and initial evaluation of the CLAVR-1 global clear/cloud classification algorithm for the advanced very high resolution radiometer. J. Atmos. Oceanic Technol., 16, 656-681. 1999. Calibration and Validation: Li, X., W. Pichel, E. Maturi, P. Clemente-Colon, and J. Sapper. Deriving the operational nonlinear multi-channel sea surface temperature algorithm coefficients for NOAA-15 AVHRR/3. International Journal of Remote Sensing, Volume 22, No. 4, 699 - 704, March 2001a. Calibration and Validation: Li, X, W. Pichel, P. Clemente-Colon, V. Krasnopolsky, and J. Sapper. Validation of coastal sea and lake surface temperature measurements derived from NOAA/AVHRR Data. International Journal of Remote Sensing, Vol. 22, No. 7, 1285-1303, 2001b. GOES Imager Processing Information: http://coastwatch.noaa.gov/goes_sst_overview.html .  GOES Imager Processing Reference: Wu, X., W. P. Menzel, and G. S. Wade, 1999. Estimation of sea surface temperatures using GOES-8/9 radiance measurements, Bull. Amer. Meteor. Soc., 80, 1127-1138.  MODIS Aqua Processing Information: http://oceancolor.gsfc.nasa.gov/DOCS/modis_sst/ . MODIS Aqua Processing reference: Not Available.</att>\n"
            + "        <att name=\"rows\" type=\"int\">1501</att>\n"
            + "        <att name=\"satellite\">Aqua, GOES, POES</att>\n"
            + "        <att name=\"sensor\">AMSR-E, MODIS, Imager, AVHRR</att>\n"
            + "        <att name=\"source\">satellite observation: Aqua, GOES, POES, AMSR-E, MODIS, Imager, AVHRR</att>\n"
            + "        <att name=\"Southernmost_Northing\" type=\"double\">-75.0</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF-1.0</att>\n"
            + "        <att name=\"start_time\" type=\"doubleList\">0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0</att>\n"
            + "        <att name=\"summary\">NOAA OceanWatch provides a blended sea surface temperature (SST) products derived from both microwave and infrared sensors carried on multiple platforms.  The microwave instruments can measure ocean temperatures even in the presence of clouds, though the resolution is a bit coarse when considering features typical of the coastal environment.  These are complemented by the relatively fine measurements of infrared sensors.  The blended data are provided at moderate spatial resolution (0.1 degrees) for the Global Ocean.  Measurements are gathered by Japan&#39;s Advanced Microwave Scanning Radiometer (AMSR-E) instrument, a passive radiance sensor carried aboard NASA&#39;s Aqua spacecraft, NOAA&#39;s Advanced Very High Resolution Radiometer, NOAA GOES Imager, and NASA&#39;s Moderate Resolution Imaging Spectrometer (MODIS). THIS IS AN EXPERIMENTAL PRODUCT: intended strictly for scientific evaluation by professional marine scientists.</att>\n"
            + "        <att name=\"time_coverage_end\">2013-12-01T00:00:00Z</att>\n"
            + "        <att name=\"time_coverage_start\">2013-11-01T00:00:00Z</att>\n"
            + "        <att name=\"title\">SST, Blended, 0.1 degrees, Global, EXPERIMENTAL</att>\n"
            + "        <att name=\"Westernmost_Easting\" type=\"double\">0.0</att>\n"
            + "    </sourceAttributes -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cols\">null</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n"
            + "        <att name=\"creator_email\">erd.data@noaa.gov</att>\n"
            + "        <att name=\"creator_name\">Remote Sensing Systems Inc, JAXA, NASA, OSDPD, CoastWatch</att>\n"
            + "        <att name=\"creator_type\">institution</att>\n"
            + "        <att name=\"creator_url\">https://coastwatch.pfeg.noaa.gov</att>\n"
            + "        <att name=\"cwhdf_version\">null</att>\n"
            + "        <att name=\"date_created\">2013-12-01</att>\n"
            + "        <att name=\"date_issued\">2013-12-01</att>\n"
            + "        <att name=\"et_affine\">null</att>\n"
            + "        <att name=\"gctp_datum\">null</att>\n"
            + "        <att name=\"gctp_parm\">null</att>\n"
            + "        <att name=\"gctp_sys\">null</att>\n"
            + "        <att name=\"gctp_zone\">null</att>\n"
            + "        <att name=\"id\">satellite/BA/ssta/mday</att>\n"
            + "        <att name=\"infoUrl\">https://coastwatch.pfeg.noaa.gov/infog/BA_ssta_las.html</att>\n"
            + "        <att name=\"institution\">NOAA CoastWatch WCN</att>\n"
            + "        <att name=\"keywords\">advanced, altitude, amsr, amsr-e, avhrr, BAssta, blended, coast, coastwatch, data, degrees, earth, Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature, environmental, eos, experimental, geostationary, global, goes, high, imaging, infrared, latitude, longitude, microwave, moderate, modis, month, monthly, msr, noaa, node, ocean, oceans, operational, radiometer, resolution, satellite, scanning, science, sea, sea_surface_temperature, spectroradiometer, sst, surface, temperature, time, very, vhrr, wcn, west</att>\n"
            + "        <att name=\"naming_authority\">gov.noaa.pfeg.coastwatch</att>\n"
            + "        <att name=\"pass_date\">null</att>\n"
            + "        <att name=\"polygon_latitude\">null</att>\n"
            + "        <att name=\"polygon_longitude\">null</att>\n"
            + "        <att name=\"project\">CoastWatch (https://coastwatch.noaa.gov/)</att>\n"
            + "        <att name=\"publisher_email\">erd.data@noaa.gov</att>\n"
            + "        <att name=\"publisher_name\">NOAA NMFS SWFSC ERD</att>\n"
            + "        <att name=\"publisher_type\">institution</att>\n"
            + "        <att name=\"publisher_url\">https://coastwatch.pfeg.noaa.gov</att>\n"
            + "        <att name=\"references\">Blended SST from satellites information: This is an experimental product which blends satellite-derived SST data from multiple platforms using a weighted mean.  Weights are based on the inverse square of the nominal accuracy of each satellite. AMSR_E Processing information: https://www.eorc.jaxa.jp/en/distribution/standard_dataset/pdf/amsr-e_handbook_e.pdf . AMSR-E Processing reference: Wentz, F.J., C. Gentemann, and P. Ashcroft. 2005. ON-ORBIT CALIBRATION OF AMSR-E AND THE RETRIEVAL OF OCEAN PRODUCTS. Remote Sensing Systems Internal Report. AVHRR Processing Information: http://www.osdpd.noaa.gov/PSB/EPS/CW/coastwatch.html .  AVHRR Processing Reference: Walton C. C., W. G. Pichel, J. F. Sapper, D. A. May. The development and operational application of nonlinear algorithms for the measurement of sea surface temperatures with the NOAA polar-orbiting environmental satellites. J.G.R., 103: (C12) 27999-28012, 1998. Cloudmask reference: Stowe, L. L., P. A. Davis, and E. P. McClain.  Scientific basis and initial evaluation of the CLAVR-1 global clear/cloud classification algorithm for the advanced very high resolution radiometer. J. Atmos. Oceanic Technol., 16, 656-681. 1999. Calibration and Validation: Li, X., W. Pichel, E. Maturi, P. Clemente-Colon, and J. Sapper. Deriving the operational nonlinear multi-channel sea surface temperature algorithm coefficients for NOAA-15 AVHRR/3. International Journal of Remote Sensing, Volume 22, No. 4, 699 - 704, March 2001a. Calibration and Validation: Li, X, W. Pichel, P. Clemente-Colon, V. Krasnopolsky, and J. Sapper. Validation of coastal sea and lake surface temperature measurements derived from NOAA/AVHRR Data. International Journal of Remote Sensing, Vol. 22, No. 7, 1285-1303, 2001b. GOES Imager Processing Information: https://coastwatch.noaa.gov/goes_sst_overview.html .  GOES Imager Processing Reference: Wu, X., W. P. Menzel, and G. S. Wade, 1999. Estimation of sea surface temperatures using GOES-8/9 radiance measurements, Bull. Amer. Meteor. Soc., 80, 1127-1138.  MODIS Aqua Processing Information: https://oceancolor.gsfc.nasa.gov/DOCS/modis_sst/ . MODIS Aqua Processing reference: Not Available.</att>\n"
            + "        <att name=\"rows\">null</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"start_time\">null</att>\n"
            + "        <att name=\"summary\">SST, Blended, 0.1 degrees, Global, EXPERIMENTAL. NOAA OceanWatch provides a blended sea surface temperature (SST) products derived from both microwave and infrared sensors carried on multiple platforms. The microwave instruments can measure ocean temperatures even in the presence of clouds, though the resolution is a bit coarse when considering features typical of the coastal environment. These are complemented by the relatively fine measurements of infrared sensors. The blended data are provided at moderate spatial resolution (0.1 degrees) for the Global Ocean. Measurements are gathered by Japan&#39;s Advanced Microwave Scanning Radiometer (Advanced Microwave Scanning Radiometer on EOS (AMSR-E)) instrument, a passive radiance sensor carried aboard NASA&#39;s Aqua spacecraft, NOAA&#39;s Advanced Very High Resolution Radiometer, NOAA Geostationary Operational Environmental Satellite (GOES) Imager, and NASA&#39;s Moderate Resolution Imaging Spectrometer (Moderate Resolution Imaging Spectroradiometer (MODIS)). THIS IS AN EXPERIMENTAL PRODUCT: intended strictly for scientific evaluation by professional marine scientists.</att>\n"
            + "        <att name=\"title\">SST, Blended, 0.1 degrees, Global, EXPERIMENTAL (SST, Blended, Global, EXPERIMENTAL, Monthly), 2002-2013</att>\n"
            + "    </addAttributes>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>time</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Time</att>\n"
            + "            <att name=\"actual_range\" type=\"doubleList\">1.38456E9 1.38456E9</att>\n"
            + "            <att name=\"axis\">T</att>\n"
            + "            <att name=\"fraction_digits\" type=\"int\">0</att>\n"
            + "            <att name=\"long_name\">Centered Time</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>altitude</sourceName>\n"
            + "        <destinationName>altitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Height</att>\n"
            + "            <att name=\"_CoordinateZisPositive\">up</att>\n"
            + "            <att name=\"actual_range\" type=\"doubleList\">0.0 0.0</att>\n"
            + "            <att name=\"axis\">Z</att>\n"
            + "            <att name=\"fraction_digits\" type=\"int\">0</att>\n"
            + "            <att name=\"long_name\">Altitude</att>\n"
            + "            <att name=\"positive\">up</att>\n"
            + "            <att name=\"standard_name\">altitude</att>\n"
            + "            <att name=\"units\">m</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>lat</sourceName>\n"
            + "        <destinationName>latitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Lat</att>\n"
            + "            <att name=\"actual_range\" type=\"doubleList\">-75.0 75.0</att>\n"
            + "            <att name=\"axis\">Y</att>\n"
            + "            <att name=\"coordsys\">geographic</att>\n"
            + "            <att name=\"fraction_digits\" type=\"int\">1</att>\n"
            + "            <att name=\"long_name\">Latitude</att>\n"
            + "            <att name=\"point_spacing\">even</att>\n"
            + "            <att name=\"standard_name\">latitude</att>\n"
            + "            <att name=\"units\">degrees_north</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>lon</sourceName>\n"
            + "        <destinationName>longitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Lon</att>\n"
            + "            <att name=\"actual_range\" type=\"doubleList\">0.0 360.0</att>\n"
            + "            <att name=\"axis\">X</att>\n"
            + "            <att name=\"coordsys\">geographic</att>\n"
            + "            <att name=\"fraction_digits\" type=\"int\">1</att>\n"
            + "            <att name=\"long_name\">Longitude</att>\n"
            + "            <att name=\"point_spacing\">even</att>\n"
            + "            <att name=\"standard_name\">longitude</att>\n"
            + "            <att name=\"units\">degrees_east</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>BAssta</sourceName>\n"
            + "        <destinationName>BAssta</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n"
            + "            <att name=\"actual_range\" type=\"floatList\">-1.99 33.045</att>\n"
            + "            <att name=\"coordsys\">geographic</att>\n"
            + "            <att name=\"fraction_digits\" type=\"int\">1</att>\n"
            + "            <att name=\"long_name\">SST, Blended, 0.1 degrees, Global, EXPERIMENTAL</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n"
            + "            <att name=\"numberOfObservations\" type=\"int\">2885693</att>\n"
            + "            <att name=\"percentCoverage\" type=\"double\">0.5338832706363859</att>\n"
            + "            <att name=\"standard_name\">sea_surface_temperature</att>\n"
            + "            <att name=\"units\">degree_C</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Temperature</att>\n"
            + "            <att name=\"numberOfObservations\">null</att>\n"
            + "            <att name=\"percentCoverage\">null</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n";
    // String2.setClipboardString(results);
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /**
   * This tests allowing actual_range source att with different dataType than var (if no
   * scale_factor).
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagThredds // external server is failing to respond, so disable the test for now
  void testActualRange() throws Throwable {
    // String2.log("\n*** EDDGridFromDap.testActualRange");
    int language = 0;
    // testVerboseOn();
    String tDir = EDStatic.fullTestCacheDirectory;
    String name, tName, results, tResults, expected, userDapQuery;
    String today = Calendar2.getCurrentISODateTimeStringZulu() + "Z";

    EDDGrid edd = (EDDGrid) EDDTestDataset.gettestActualRange();

    tName =
        edd.makeNewFileForDapQuery(
            language, null, null, "", tDir, edd.className() + "_actual_range", ".dds");
    int nTime = edd.axisVariables[0].sourceValues().size();
    results = File2.directReadFrom88591File(tDir + tName);
    expected =
        "Dataset {\n"
            + "  Float64 time[time = "
            + nTime
            + "];\n"
            + // time=# changes here and below
            "  Float64 latitude[latitude = 62];\n"
            + "  Float64 longitude[longitude = 122];\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 SST[time = "
            + nTime
            + "][latitude = 62][longitude = 122];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = "
            + nTime
            + "];\n"
            + "      Float64 latitude[latitude = 62];\n"
            + "      Float64 longitude[longitude = 122];\n"
            + "  } SST;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 SSS[time = "
            + nTime
            + "][latitude = 62][longitude = 122];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = "
            + nTime
            + "];\n"
            + "      Float64 latitude[latitude = 62];\n"
            + "      Float64 longitude[longitude = 122];\n"
            + "  } SSS;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 pCO2sw[time = "
            + nTime
            + "][latitude = 62][longitude = 122];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = "
            + nTime
            + "];\n"
            + "      Float64 latitude[latitude = 62];\n"
            + "      Float64 longitude[longitude = 122];\n"
            + "  } pCO2sw;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 TA[time = "
            + nTime
            + "][latitude = 62][longitude = 122];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = "
            + nTime
            + "];\n"
            + "      Float64 latitude[latitude = 62];\n"
            + "      Float64 longitude[longitude = 122];\n"
            + "  } TA;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 TC[time = "
            + nTime
            + "][latitude = 62][longitude = 122];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = "
            + nTime
            + "];\n"
            + "      Float64 latitude[latitude = 62];\n"
            + "      Float64 longitude[longitude = 122];\n"
            + "  } TC;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 pH[time = "
            + nTime
            + "][latitude = 62][longitude = 122];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = "
            + nTime
            + "];\n"
            + "      Float64 latitude[latitude = 62];\n"
            + "      Float64 longitude[longitude = 122];\n"
            + "  } pH;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 SSA[time = "
            + nTime
            + "][latitude = 62][longitude = 122];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = "
            + nTime
            + "];\n"
            + "      Float64 latitude[latitude = 62];\n"
            + "      Float64 longitude[longitude = 122];\n"
            + "  } SSA;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 HCO3[time = "
            + nTime
            + "][latitude = 62][longitude = 122];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = "
            + nTime
            + "];\n"
            + "      Float64 latitude[latitude = 62];\n"
            + "      Float64 longitude[longitude = 122];\n"
            + "  } HCO3;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 CO3[time = "
            + nTime
            + "][latitude = 62][longitude = 122];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = "
            + nTime
            + "];\n"
            + "      Float64 latitude[latitude = 62];\n"
            + "      Float64 longitude[longitude = 122];\n"
            + "  } CO3;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 surface_flag[time = "
            + nTime
            + "][latitude = 62][longitude = 122];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = "
            + nTime
            + "];\n"
            + "      Float64 latitude[latitude = 62];\n"
            + "      Float64 longitude[longitude = 122];\n"
            + "  } surface_flag;\n"
            + "} testActualRange;\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    tName =
        edd.makeNewFileForDapQuery(
            language, null, null, "", tDir, edd.className() + "_actual_range", ".das");
    results = File2.directReadFrom88591File(tDir + tName);
    expected =
        // "Attributes {
        // time {
        // String _CoordinateAxisType \"Time\";
        // Float64 actual_range 1.4516064e+9, 1.516752e+9;
        "String axis \"T\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float64 actual_range 14.875, 30.125;\n"
            + "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String source_name \"y\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range -90.125, -59.875;\n"
            + "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String source_name \"x\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  SST {\n"
            + "    Float32 _FillValue NaN;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + // source actual_range is
            // Float64 values from 1
            // file and so
            // changes often! in real
            // life -> null
            "    Float64 colorBarMaximum 32.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String coordsys \"geographic\";\n"
            + "    String ioos_category \"Temperature\";\n"
            + "    String long_name \"NOAA Coral Reef Watch Experimental Ocean Acidification Product Suite - NOAA OI AVHRR-AMSRE SST 25km\";\n"
            + "    String standard_name \"sea_surface_temperature\";\n"
            + "    String units \"degree_C\";\n"
            + "    String variable_info \"The values in this variable array and in the valid_range attribute are in the units of x100 degrees Celsius. The data values should be multiplied by the value (=0.01) contained in thescale_factor attribute to obtain the actual values in the units of degrees Celsius.\";\n"
            + "  }\n"
            + "  SSS {\n"
            + "    Float32 _FillValue NaN;\n"
            + "    Float64 colorBarMaximum 37.0;\n"
            + "    Float64 colorBarMinimum 32.0;\n"
            + "    String coordsys \"geographic\";\n"
            + "    String ioos_category \"Salinity\";\n"
            + "    String long_name \"NOAA Coral Reef Watch Experimental Ocean Acidification Product Suite - IASNFS SSS interpolated to 25km\";\n"
            + "    String standard_name \"sea_water_practical_salinity\";\n"
            + "    String units \"PSU\";\n"
            + "    String variable_info \"The values in this variable array and in the valid_range attribute are in the units of x100 psu . The data values should be multiplied by the value (=0.01) contained in thescale_factor attribute to obtain the actual values in the units of .psu\";\n"
            + "  }\n"
            + "  pCO2sw {\n"
            + "    Float32 _FillValue NaN;\n"
            + "    Float64 colorBarMaximum 420.0;\n"
            + "    Float64 colorBarMinimum 340.0;\n"
            + "    String coordsys \"geographic\";\n"
            + "    String ioos_category \"CO2\";\n"
            + "    String long_name \"NOAA Coral Reef Watch Experimental Ocean Acidification Product Suite - pCO2sw 25km\";\n"
            + "    String units \"uatm\";\n"
            + "    String variable_info \"The values in this variable array and in the valid_range attribute are in the units of x10 uatm . The data values should be multiplied by the value (=0.1) contained in thescale_factor attribute to obtain the actual values in the units of uatm .  The values are modeled according to Gledhill et al., 2008\";\n"
            + "  }\n"
            + "  TA {\n"
            + "    Float32 _FillValue NaN;\n"
            + "    Float64 colorBarMaximum 2450.0;\n"
            + "    Float64 colorBarMinimum 2200.0;\n"
            + "    String coordsys \"geographic\";\n"
            + "    String ioos_category \"CO2\";\n"
            + "    String long_name \"NOAA Coral Reef Watch Experimental Ocean Acidification Product Suite - Total Alkalinity (TA) 25km\";\n"
            + "    String units \"µmole/kg\";\n"
            + "    String variable_info \"The values in this variable array and in the valid_range attribute are in the units of x10umol/kg . The data values should be multiplied by the value (=0.1) contained in thescale_factor attribute to obtain the actual values in the units of .umol/kg .  The values are modeled according to Gledhill et al., 2008 and depend on the tropical/subtropical algorthim offered by Lee et al. 2006\";\n"
            + "  }\n"
            + "  TC {\n"
            + "    Float32 _FillValue NaN;\n"
            + "    Float64 colorBarMaximum 2150.0;\n"
            + "    Float64 colorBarMinimum 1950.0;\n"
            + "    String coordsys \"geographic\";\n"
            + "    String ioos_category \"Ocean Color\";\n"
            + "    String long_name \"NOAA Coral Reef Watch Experimental Ocean Acidification Product Suite - Total Inorganic Carbon (TC) 25km\";\n"
            + "    String units \"µmole/kg\";\n"
            + "    String variable_info \"The values in this variable array and in the valid_range attribute are in the units of x10umol/kg . The data values should be multiplied by the value (=0.1) contained in thescale_factor attribute to obtain the actual values in the units of .umol/kg .  The values are modeled according to Gledhill et al., 2008.  Fields of TA & fCO2sw were  were coupled to solve for the carbonic acid system using the CO2SYS program (Lewis & Wallace, 1998).  Constants: K1,K2 from Mehrbach et al, 1973 refit by Dickson & Millero, 1987;  fCO2 (versus pCO2); KSO4 from Dickson; pH = total scale\";\n"
            + "  }\n"
            + "  pH {\n"
            + "    Float32 _FillValue NaN;\n"
            + "    Float64 colorBarMaximum 9.0;\n"
            + "    Float64 colorBarMinimum 7.0;\n"
            + "    String coordsys \"geographic\";\n"
            + "    String ioos_category \"Salinity\";\n"
            + "    String long_name \"NOAA Coral Reef Watch Experimental Ocean Acidification Product Suite - pH 25km\";\n"
            + "    String standard_name \"sea_water_ph_reported_on_total_scale\";\n"
            + "    String units \"Total Scale\";\n"
            + "    String variable_info \"The values in this variable array and in the valid_range attribute are in the units of x100Total Scale. The data values should be multiplied by the value (=0.01) contained in thescale_factor attribute to obtain the actual values in the units of .Total Scale.  The values are modeled according to Gledhill et al., 2008.  Fields of TA & fCO2sw were  were coupled to solve for the carbonic acid system using the CO2SYS program (Lewis & Wallace, 1998).  Constants: K1,K2 from Mehrbach et al, 1973 refit by Dickson & Millero, 1987;  fCO2 (versus pCO2); KSO4 from Dickson; pH = total scale\";\n"
            + "  }\n"
            + "  SSA {\n"
            + "    Float32 _FillValue NaN;\n"
            + "    Float64 colorBarMaximum 4.0;\n"
            + "    Float64 colorBarMinimum 2.0;\n"
            + "    String coordsys \"geographic\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"NOAA Coral Reef Watch Experimental Ocean Acidification Product Suite - Saturation State (argonite) 25km\";\n"
            + "    String units \"Omega\";\n"
            + "    String variable_info \"The values in this variable array and in the valid_range attribute are in the units of x100Omega . The data values should be multiplied by the value (=0.01) contained in thescale_factor attribute to obtain the actual values in the units of .Omega .  The values are modeled according to Gledhill et al., 2008.  Fields of TA & fCO2sw were  were coupled to solve for the carbonic acid system using the CO2SYS program (Lewis & Wallace, 1998).  Constants: K1,K2 from Mehrbach et al, 1973 refit by Dickson & Millero, 1987;  fCO2 (versus pCO2); KSO4 from Dickson; pH = total scale\";\n"
            + "  }\n"
            + "  HCO3 {\n"
            + "    Float32 _FillValue NaN;\n"
            + "    Float64 colorBarMaximum 1950.0;\n"
            + "    Float64 colorBarMinimum 1750.0;\n"
            + "    String coordsys \"geographic\";\n"
            + "    String ioos_category \"CO2\";\n"
            + "    String long_name \"NOAA Coral Reef Watch Experimental Ocean Acidification Product Suite - Bicarbonate Ion Concentration (HCO3-) 25km\";\n"
            + "    String units \"µmole/kg\";\n"
            + "    String variable_info \"The values in this variable array and in the valid_range attribute are in the units of x10umol/kg . The data values should be multiplied by the value (=0.1) contained in thescale_factor attribute to obtain the actual values in the units of .umol/kg .  The values are modeled according to Gledhill et al., 2008.  Fields of TA & fCO2sw were  were coupled to solve for the carbonic acid system using the CO2SYS program (Lewis & Wallace, 1998).  Constants: K1,K2 from Mehrbach et al, 1973 refit by Dickson & Millero, 1987;  fCO2 (versus pCO2); KSO4 from Dickson; pH = total scale\";\n"
            + "  }\n"
            + "  CO3 {\n"
            + "    Float32 _FillValue NaN;\n"
            + "    Float64 colorBarMaximum 260.0;\n"
            + "    Float64 colorBarMinimum 140.0;\n"
            + "    String coordsys \"geographic\";\n"
            + "    String ioos_category \"CO2\";\n"
            + "    String long_name \"NOAA Coral Reef Watch Experimental Ocean Acidification Product Suite - Carbonate Ion Concentration (CO3--) 25km\";\n"
            + "    String units \"µmole/kg\";\n"
            + "    String variable_info \"The values in this variable array and in the valid_range attribute are in the units of x10umol/kg . The data values should be multiplied by the value (=0.1) contained in thescale_factor attribute to obtain the actual values in the units of .umol/kg .  The values are modeled according to Gledhill et al., 2008.  Fields of TA & fCO2sw were  were coupled to solve for the carbonic acid system using the CO2SYS program (Lewis & Wallace, 1998).  Constants: K1,K2 from Mehrbach et al, 1973 refit by Dickson & Millero, 1987;  fCO2 (versus pCO2); KSO4 from Dickson; pH = total scale\";\n"
            + "  }\n"
            + "  surface_flag {\n"
            + "    Float32 _FillValue NaN;\n"
            + "    Float64 colorBarMaximum 1.5;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String description \"An array in the same dimension as the data array(s) classifies valid, non-valid (includes land and areas of no model outpt) and missing pixels that are all flaged by the same missing_value in the da array(s).\";\n"
            + "    String flag_meanings \"valid, non-valid (includes land and areas of no model output)\";\n"
            + "    Byte flag_values 0, 1;\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Pixel characteristics flag array\";\n"
            + "  }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_data_type \"Grid\";\n"
            + "    String comment \"This is the Coral Reef Watch Ocean Acidification Product Suite  produced monthly in conjunction with NOAA CoastWatch. CoastWatch Utilities, including CoastWatch Data Analysis  Tool (CDAT), v3.2.1 or higher (https://coastwatch.noaa.gov/cw_software.html) can be used for viewing, analyzing, and plotting the data.\";\n"
            + "    String composite \"false\";\n"
            + "    String contact \"NOAA Coral Reef Watch at coralreefwatch@noaa.gov\";\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "    String creator_email \"coralreefwatch@noaa.gov\";\n"
            + "    String creator_name \"CORALREEFWATCH\";\n"
            + "    String creator_type \"institution\";\n"
            + "    String creator_url \"https://www.noaa.gov/\";\n"
            + "    String data_source \"NOAA OI AVHRR-AMSRE SST, NCEP Interp SLP, Forecast pCO2air Model, IASNFS SSS\";\n"
            + "    Float64 Easternmost_Easting -59.875;\n"
            + "    Float64 geospatial_lat_max 30.125;\n"
            + "    Float64 geospatial_lat_min 14.875;\n"
            + "    Float64 geospatial_lat_resolution 0.25;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max -59.875;\n"
            + "    Float64 geospatial_lon_min -90.125;\n"
            + "    Float64 geospatial_lon_resolution 0.25;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n";
    // try {
    results =
        results.replaceAll(
            "Float32 actual_range -?[0-9]+.[0-9]+, -?[0-9]+.[0-9]+;",
            "Float32 actual_range MIN, MAX;");
    int po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);
    // } catch (Exception e) {
    //   Test.knownProblem(
    //       "2018-06-20 I think actual_ranges change with every new timepoint.",
    //       "Remove those atts in generateDatasetsXml esp from THREDDS or HYRAX?", e);
    // }
  }

  /**
   * This tests allowing actual_range source att with different dataType than var (if add_offset=0
   * and scale_factor=1).
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  void testActualRange2() throws Throwable {
    // String2.log("\n*** EDDGridFromDap.testActualRange2");
    int language = 0;
    // testVerboseOn();
    String tDir = EDStatic.fullTestCacheDirectory;
    String name, tName, results, tResults, expected, userDapQuery;
    String today = Calendar2.getCurrentISODateTimeStringZulu() + "Z";

    EDDGrid edd = (EDDGrid) EDDTestDataset.gettestActualRange2();

    tName =
        edd.makeNewFileForDapQuery(
            language, null, null, "", tDir, edd.className() + "_actual_range2", ".dds");
    results = File2.directReadFrom88591File(tDir + tName);
    expected =
        "Dataset {\n"
            + "  Float64 time[time = 180];\n"
            + "  Float32 latitude[latitude = 51];\n"
            + "  Float32 longitude[longitude = 360];\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Int16 hrc[time = 180][latitude = 51][longitude = 360];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 180];\n"
            + "      Float32 latitude[latitude = 51];\n"
            + "      Float32 longitude[longitude = 360];\n"
            + "  } hrc;\n"
            + "} testActualRange2;\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    tName =
        edd.makeNewFileForDapQuery(
            language, null, null, "", tDir, edd.className() + "_actual_range2", ".das");
    results = File2.directReadFrom88591File(tDir + tName);
    expected =
        // Attributes {
        // time {
        // String _CoordinateAxisType "Time";
        // Float64 actual_range 3.1536e+7, 5.022432e+8;
        "String avg_period \"0000-01-00 00:00:00\";\n"
            + "    String axis \"T\";\n"
            + "    String delta_t \"0000-01-00 00:00:00\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float32 actual_range -25.0, 25.0;\n"
            + "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float32 actual_range 1.0, 360.0;\n"
            + "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  hrc {\n"
            + "    Int16 actual_range 0, 21;\n"
            + // source actual_range is Float32
            "    Float64 colorBarMaximum 25.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Meteorology\";\n"
            + "    String level_desc \"Entire Atmosphere Considered As a Single Layer\";\n"
            + "    String long_name \"Highly Reflective Clouds Monthly Missing Days\";\n"
            + "    Int16 missing_value 32766;\n"
            + "    String parent_stat \"Individual Obs\";\n"
            + "    Int16 precision 0;\n"
            + "    String statistic \"Number of Missing Days\";\n"
            + "    Float32 valid_range 0.0, 31.0;\n"
            + "    String var_desc \"Highly Reflective Clouds\";\n"
            + "  }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_data_type \"Grid\";\n"
            + "    String Conventions \"CF-1.6, COARDS, ACDD-1.3\";\n"
            + "    String creator_email \"esrl.psd.data@noaa.gov\";\n"
            + "    String creator_name \"NOAA ESRL PSD\";\n"
            + "    String creator_type \"institution\";\n"
            + "    String creator_url \"https://www.esrl.noaa.gov/psd/\";\n"
            + "    String dataset_title \"NOAA Highly Reflective Clouds\";\n"
            + "    Float64 Easternmost_Easting 360.0;\n"
            + "    Float64 geospatial_lat_max 25.0;\n"
            + "    Float64 geospatial_lat_min -25.0;\n"
            + "    Float64 geospatial_lat_resolution 1.0;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max 360.0;\n"
            + "    Float64 geospatial_lon_min 1.0;\n"
            + "    Float64 geospatial_lon_resolution 1.0;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    String history \"Created 1998/08/27 by Don Hooper from NCAR data";

    int po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);
  }

  /** This tests generateDatasetsXmlFromThreddsCatalog. */
  @org.junit.jupiter.api.Test
  @TagThredds
  void testGenerateDatasetsXmlFromThreddsCatalog() throws Throwable {
    String2.log("\n*** EDDGridFromDap.testGenerateDatasetsXmlFromThreddsCatalog()");

    EDDGridFromDap.runGenerateDatasetsXmlFromThreddsCatalog(null, null, ".*", null, null, -1);
  }

  /** This tests generateDatasetsXmlFromThreddsCatalog. */
  @org.junit.jupiter.api.Test
  @TagThredds
  void testGenerateDatasetsXmlFromThreddsCatalog2() throws Throwable {
    String2.log("\n*** EDDGridFromDap.testGenerateDatasetsXmlFromThreddsCatalog()");

    EDDGridFromDap.runGenerateDatasetsXmlFromThreddsCatalog(
        null, // resultsFileName
        "http://opendap.oceanbrowser.net/thredds/catalog/data/emodnet-domains/Coastal%20areas/Northeast%20Atlantic%20Ocean%20-%20Loire%20River/catalog.xml",
        ".*",
        null,
        null,
        -1);

    /*
     * thredds.client.catalog.Catalog catalog = (new
     * thredds.client.catalog.builder.CatalogBuilder()).buildFromURI(new
     * java.net.URI(
     * "http://opendap.oceanbrowser.net/thredds/catalog/data/emodnet-domains/Coastal%20areas/Northeast%20Atlantic%20Ocean%20-%20Loire%20River/catalog.xml"
     * ));
     * List<thredds.client.catalog.Dataset> datasets = catalog.getDatasets();
     * //getDatasetsLogical();
     * if (datasets != null) {
     * for (int i = 0; i < datasets.size(); i++) { //usually just 1
     * String catUrl = datasets.get(i).getCatalogUrl();
     * if (catUrl != null) {
     * //has opendap service?
     * thredds.client.catalog.Access access =
     * datasets.get(i).getAccess(thredds.client.catalog.ServiceType.OPENDAP);
     * if (access != null) {
     * String baseUrl = access.getStandardUrlName(); //Error parsing
     * URL=/thredds/dodsC/data/emodnet-domains/Coastal areas/Northeast Atlantic
     * Ocean - Loire River/Water_body_silicate.nc
     * }
     * }
     * }
     * }
     */

  }

  /** This tests getUrlsFromThreddsCatalog. */
  @org.junit.jupiter.api.Test
  @TagThredds
  void testGetUrlsFromThreddsCatalog() throws Throwable {
    String2.log("\n* testGetUrlsFromThreddsCatalog()");
    String results, expected;

    // hard to test positive pathRegex

    // test negativePathRegex
    results =
        EDDGridFromDap.getUrlsFromThreddsCatalog(
                "https://thredds1.pfeg.noaa.gov/thredds/catalog/Satellite/aggregsatMH/chla/catalog.xml",
                null,
                null,
                ".*(8day).*")
            .toNewlineString();
    expected =
        "https://thredds1.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/1day\n"
            +
            // 8day blocked by negativePathRegex
            "https://thredds1.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/mday\n";
    Test.ensureEqual(results, expected, "results1=" + results);

    // test pathRegex
    results =
        EDDGridFromDap.getUrlsFromThreddsCatalog(
                "https://thredds1.pfeg.noaa.gov/thredds/catalog/Satellite/aggregsatMH/chla/catalog.xml",
                "8.*",
                null,
                "")
            .toNewlineString();
    expected = "https://thredds1.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day\n";
    Test.ensureEqual(results, expected, "results2=" + results);
  }

  /**
   * Currently unused helper test method.
   *
   * <p>Bob uses this for testing in individual GEO IDE UAF Thredds catalog. See original catalog
   * https://data.pmel.noaa.gov/thredds/geoideCatalog.html See the clean catalog at
   * https://data.pmel.noaa.gov/uaf/thredds/geoIDECleanCatalog.html
   */
  void testUAFSubThreddsCatalog(int which) throws Throwable {

    String partName = which + "_" + Calendar2.getCompactCurrentISODateTimeStringLocal();

    EDDGridFromDap.generateDatasetsXmlFromThreddsCatalog(
        EDStatic.fullLogsDirectory + "UAFdatasets" + partName + ".xml",
        EDDGridFromDap.UAFSubThreddsCatalogs[which],
        ".*",
        ".*", // pathRegex
        ".*(oceanwatch\\.pfeg\\.noaa\\.gov|coastwatch/viirs-ocr/).*", // negativePathRegex
        -1); // -1 uses suggestReloadEveryNMinutes
  }
}
