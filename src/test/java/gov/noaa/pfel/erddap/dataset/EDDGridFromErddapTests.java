package gov.noaa.pfel.erddap.dataset;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.cohort.array.IntArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import tags.TagExternalERDDAP;
import tags.TagLocalERDDAP;
import tags.TagThredds;
import testDataset.EDDTestDataset;

class EDDGridFromErddapTests {
  /**
   * testGenerateDatasetsXml
   */
  @org.junit.jupiter.api.Test
  @TagLocalERDDAP
  void testGenerateDatasetsXml() throws Throwable {
    // testVerboseOn();

    // test local generateDatasetsXml
    try {
      String results = EDDGridFromErddap.generateDatasetsXml(EDStatic.erddapUrl, false) + "\n"; // in tests, always
                                                                                                // non-https url
      String2.log("results=\n" + results);

      // GenerateDatasetsXml
      String gdxResults = (new GenerateDatasetsXml()).doIt(new String[] { "-verbose",
          "EDDGridFromErddap", EDStatic.erddapUrl, "false" },
          false); // doIt loop?
      Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

      String expected = "<dataset type=\"EDDGridFromErddap\" datasetID=\"1_0_3648_e4d8_5e3c\" active=\"true\">\n" +
          "    <!-- SST, Blended, Global, 2002-2014, EXPERIMENTAL (5 Day Composite) -->\n" +
          "    <sourceUrl>http://127.0.0.1:8080/cwexperimental/griddap/erdBAssta5day</sourceUrl>\n" +
          "</dataset>";
      int po = results.indexOf(expected.substring(0, 80));
      if (po < 0)
        String2.log("results=" + results);
      Test.ensureEqual(results.substring(po, po + expected.length()), expected, "results=\n" + results);

      expected = "<!-- Of the datasets above, the following datasets are EDDGridFromErddap's at the remote ERDDAP.\n";
      po = results.indexOf(expected.substring(0, 20));
      if (po < 0)
        String2.log("results=" + results);
      Test.ensureEqual(results.substring(po, po + expected.length()), expected, "results=\n" + results);
      Test.ensureTrue(results.indexOf("rMHchla8day", po) > 0,
          "results=\n" + results +
              "\nTHIS TEST REQUIRES rMHchla8day TO BE ACTIVE ON THE localhost ERDDAP.");

      // ensure it is ready-to-use by making a dataset from it
      String tDatasetID = "1_0_5615_5e97_1841";
      EDD.deleteCachedDatasetInfo(tDatasetID);
      EDD edd = EDDGridFromErddap.oneFromXmlFragment(null, results);
      String2.log(
          "\n!!! The first dataset will vary, depending on which are currently active!!!\n" +
              "title=" + edd.title() + "\n" +
              "datasetID=" + edd.datasetID() + "\n" +
              "vars=" + String2.toCSSVString(edd.dataVariableDestinationNames()));
      Test.ensureEqual(edd.title(),
          "Audio data from a local source.", "");
      Test.ensureEqual(edd.datasetID(), tDatasetID, "");
      Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()),
          "channel_1", "");

    } catch (Throwable t) {
      throw new RuntimeException(
          "Error using generateDatasetsXml on " + EDStatic.erddapUrl +
              "\nThis test requires |testGridWav|erdBAssta5day|rMHchla8day in the localhost ERDDAP.",
          t); // in tests, always non-https url
    }

  }

  /** This does some basic tests. */
  @org.junit.jupiter.api.Test
  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  @TagThredds
  void testBasic(boolean testLocalErddapToo) throws Throwable {
    // testVerboseOn();
    int language = 0;
    EDDGridFromErddap gridDataset;
    String name, tName, axisDapQuery, query, results, expected, expected2, error;
    int tPo;
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
    String userDapQuery = SSR.minimalPercentEncode("chlorophyll[(2007-02-06)][][(29):10:(50)][(225):10:(247)]");
    String graphDapQuery = SSR.minimalPercentEncode("chlorophyll[0:10:200][][(29)][(225)]");
    String mapDapQuery = SSR.minimalPercentEncode("chlorophyll[200][][(29):(50)][(225):(247)]"); // stride irrelevant
    StringArray destinationNames = new StringArray();
    IntArray constraints = new IntArray();
    String localUrl = EDStatic.erddapUrl + "/griddap/rMHchla8day"; // in tests, always non-https url

    try {

      gridDataset = (EDDGridFromErddap) EDDGridFromErddap.oneFromDatasetsXml(null, "rMHchla8day");

      // *** test getting das for entire dataset
      String2.log("\n****************** EDDGridFromErddap test entire dataset\n");
      tName = gridDataset.makeNewFileForDapQuery(language, null, null, "", EDStatic.fullTestCacheDirectory,
          gridDataset.className() + "_Entire", ".das");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      // String2.log(results);
      expected = "Attributes \\{\n" +
          "  time \\{\n" +
          "    String _CoordinateAxisType \"Time\";\n" +
          "    Float64 actual_range 1.0260864e\\+9, .{8,14};\n" + // changes
          "    String axis \"T\";\n" +
          "    Int32 fraction_digits 0;\n" +
          "    String ioos_category \"Time\";\n" +
          "    String long_name \"Centered Time\";\n" +
          "    String standard_name \"time\";\n" +
          "    String time_origin \"01-JAN-1970 00:00:00\";\n" +
          "    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
          "  \\}\n" +
          "  altitude \\{\n" +
          "    String _CoordinateAxisType \"Height\";\n" +
          "    String _CoordinateZisPositive \"up\";\n" +
          "    Float64 actual_range 0.0, 0.0;\n" +
          "    String axis \"Z\";\n" +
          "    Int32 fraction_digits 0;\n" +
          "    String ioos_category \"Location\";\n" +
          "    String long_name \"Altitude\";\n" +
          "    String positive \"up\";\n" +
          "    String standard_name \"altitude\";\n" +
          "    String units \"m\";\n" +
          "  \\}\n" +
          "  latitude \\{\n" +
          "    String _CoordinateAxisType \"Lat\";\n" +
          "    Float64 actual_range -90.0, 90.0;\n" +
          "    String axis \"Y\";\n" +
          "    String coordsys \"geographic\";\n" +
          "    Int32 fraction_digits 4;\n" +
          "    String ioos_category \"Location\";\n" +
          "    String long_name \"Latitude\";\n" +
          "    String point_spacing \"even\";\n" +
          "    String standard_name \"latitude\";\n" +
          "    String units \"degrees_north\";\n" +
          "  \\}\n" +
          "  longitude \\{\n" +
          "    String _CoordinateAxisType \"Lon\";\n" +
          "    Float64 actual_range 0.0, 360.0;\n" +
          "    String axis \"X\";\n" +
          "    String coordsys \"geographic\";\n" +
          "    Int32 fraction_digits 4;\n" +
          "    String ioos_category \"Location\";\n" +
          "    String long_name \"Longitude\";\n" +
          "    String point_spacing \"even\";\n" +
          "    String standard_name \"longitude\";\n" +
          "    String units \"degrees_east\";\n" +
          "  \\}\n" +
          "  chlorophyll \\{\n" +
          "    Float32 _FillValue -9999999.0;\n" +
          "    Float64 colorBarMaximum 30.0;\n" +
          "    Float64 colorBarMinimum 0.03;\n" +
          "    String colorBarScale \"Log\";\n" +
          "    String coordsys \"geographic\";\n" +
          "    Int32 fraction_digits 2;\n" +
          "    String ioos_category \"Ocean Color\";\n" +
          "    String long_name \"Concentration Of Chlorophyll In Sea Water\";\n" +
          "    Float32 missing_value -9999999.0;\n" +
          "    String standard_name \"concentration_of_chlorophyll_in_sea_water\";\n" +
          "    String units \"mg m-3\";\n" +
          "  \\}\n" +
          "  NC_GLOBAL \\{\n" +
          "    String acknowledgement \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
          "    String cdm_data_type \"Grid\";\n" +
          "    String composite \"true\";\n" +
          "    String contributor_name \"NASA GSFC \\(OBPG\\)\";\n" +
          "    String contributor_role \"Source of level 2 data.\";\n" +
          "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
          "    String creator_email \"erd.data@noaa.gov\";\n" +
          "    String creator_name \"NOAA NMFS SWFSC ERD\";\n" +
          "    String creator_type \"institution\";\n" +
          "    String creator_url \"https://www.pfeg.noaa.gov\";\n" +
          "    String date_created \"201.-..-..\";\n" + // changes
          "    String date_issued \"201.-..-..\";\n" + // changes
          "    Float64 Easternmost_Easting 360.0;\n" +
          "    Float64 geospatial_lat_max 90.0;\n" +
          "    Float64 geospatial_lat_min -90.0;\n" +
          "    Float64 geospatial_lat_resolution 0.041676313961565174;\n" +
          "    String geospatial_lat_units \"degrees_north\";\n" +
          "    Float64 geospatial_lon_max 360.0;\n" +
          "    Float64 geospatial_lon_min 0.0;\n" +
          "    Float64 geospatial_lon_resolution 0.04167148975575877;\n" +
          "    String geospatial_lon_units \"degrees_east\";\n" +
          "    Float64 geospatial_vertical_max 0.0;\n" +
          "    Float64 geospatial_vertical_min 0.0;\n" +
          "    String geospatial_vertical_positive \"up\";\n" +
          "    String geospatial_vertical_units \"m\";\n" +
          "    String history \"NASA GSFC \\(OBPG\\)";
      // "2010-02-05T02:14:47Z NOAA CoastWatch (West Coast Node) and NOAA SWFSC ERD\n"
      // + //changes sometimes
      // today + "
      // https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day\n";
      // today + " http://localhost:8080/cwexperimental/griddap/rMHchla8day.das\";\n"
      // + //This is it working locally.
      // or coastwatch ... //what I expected/wanted. This really appears as if remote
      // dataset.

      expected2 = "    String infoUrl \"https://coastwatch.pfeg.noaa.gov/infog/MH_chla_las.html\";\n" +
          "    String institution \"NOAA NMFS SWFSC ERD\";\n" +
          "    String keywords \"8-day, aqua, chemistry, chlorophyll, chlorophyll-a, coastwatch, color, concentration, concentration_of_chlorophyll_in_sea_water, day, degrees, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, global, modis, noaa, npp, ocean, ocean color, oceans, quality, science, science quality, sea, seawater, water, wcn\";\n"
          +
          "    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
          "    String license \"The data may be used and redistributed for free but is not intended\n" +
          "for legal use, since it may contain inaccuracies. Neither the data\n" +
          "Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
          "of their employees or contractors, makes any warranty, express or\n" +
          "implied, including warranties of merchantability and fitness for a\n" +
          "particular purpose, or assumes any legal liability for the accuracy,\n" +
          "completeness, or usefulness, of this information.\";\n" +
          "    String naming_authority \"gov.noaa.pfeg.coastwatch\";\n" +
          "    Float64 Northernmost_Northing 90.0;\n" +
          "    String origin \"NASA GSFC \\(OBPG\\)\";\n" +
          "    String processing_level \"3\";\n" +
          "    String project \"CoastWatch \\(https://coastwatch.noaa.gov/\\)\";\n" +
          "    String projection \"geographic\";\n" +
          "    String projection_type \"mapped\";\n" +
          "    String publisher_email \"erd.data@noaa.gov\";\n" +
          "    String publisher_name \"NOAA NMFS SWFSC ERD\";\n" +
          "    String publisher_type \"institution\";\n" +
          "    String publisher_url \"https://www.pfeg.noaa.gov\";\n" +
          "    String references \"Aqua/MODIS information: https://oceancolor.gsfc.nasa.gov/ . MODIS information: https://coastwatch.noaa.gov/modis_ocolor_overview.html .\";\n"
          +
          "    String satellite \"Aqua\";\n" +
          "    String sensor \"MODIS\";\n" +
          "    String source \"satellite observation: Aqua, MODIS\";\n" +
          "    String sourceUrl \"https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day\";\n" +
          "    Float64 Southernmost_Northing -90.0;\n" +
          "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
          "    String summary \"NOAA CoastWatch distributes chlorophyll-a concentration data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer \\(MODIS\\) carried aboard the spacecraft.   This is Science Quality data.\";\n"
          +
          "    String time_coverage_end \"20.{8}T00:00:00Z\";\n" + // changes
          "    String time_coverage_start \"2002-07-08T00:00:00Z\";\n" +
          "    String title \"Chlorophyll-a, Aqua MODIS, NPP, 2002-2013, DEPRECATED OLDER VERSION \\(8 Day Composite\\)\";\n"
          +
          "    Float64 Westernmost_Easting 0.0;\n" +
          "  }\n" +
          "}\n";
      tPo = results.indexOf("history \"NASA GSFC (OBPG)");
      Test.ensureTrue(tPo >= 0, "tPo=-1 results=" + results);
      Test.ensureLinesMatch(results.substring(0, tPo + 25), expected,
          "\nresults=\n" + results);

      tPo = results.indexOf("    String infoUrl ");
      Test.ensureTrue(tPo >= 0, "tPo=-1 results=" + results);
      Test.ensureLinesMatch(results.substring(tPo), expected2, "\nresults=\n" + results);

      if (testLocalErddapToo) {
        results = SSR.getUrlResponseStringUnchanged(localUrl + ".das");
        tPo = results.indexOf("history \"NASA GSFC (OBPG)");
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=" + results);
        Test.ensureLinesMatch(results.substring(0, tPo + 25), expected,
            "\nresults=\n" + results);

        tPo = results.indexOf("    String infoUrl ");
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=" + results);
        Test.ensureLinesMatch(results.substring(tPo), expected2,
            "\nresults=\n" + results);
      }

      // *** test getting dds for entire dataset
      tName = gridDataset.makeNewFileForDapQuery(language, null, null, "", EDStatic.fullTestCacheDirectory,
          gridDataset.className() + "_Entire", ".dds");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      // String2.log(results);
      expected = "Dataset \\{\n" +
          "  Float64 time\\[time = \\d{3}\\];\n" + // was 500, changes sometimes (and a few places below)
          "  Float64 altitude\\[altitude = 1\\];\n" +
          "  Float64 latitude\\[latitude = 4320\\];\n" +
          "  Float64 longitude\\[longitude = 8640\\];\n" +
          "  GRID \\{\n" +
          "    ARRAY:\n" +
          "      Float32 chlorophyll\\[time = \\d{3}\\]\\[altitude = 1\\]\\[latitude = 4320\\]\\[longitude = 8640\\];\n"
          +
          "    MAPS:\n" +
          "      Float64 time\\[time = \\d{3}\\];\n" +
          "      Float64 altitude\\[altitude = 1\\];\n" +
          "      Float64 latitude\\[latitude = 4320\\];\n" +
          "      Float64 longitude\\[longitude = 8640\\];\n" +
          "  \\} chlorophyll;\n" +
          "\\} rMHchla8day;\n";
      Test.ensureLinesMatch(results, expected, "\nresults=\n" + results);

      if (testLocalErddapToo) {
        results = SSR.getUrlResponseStringUnchanged(localUrl + ".dds");
        Test.ensureLinesMatch(results, expected, "\nresults=\n" + results);
      }

      // ********************************************** test getting axis data

      // .asc
      String2.log("\n*** EDDGridFromErddap test get .ASC axis data\n");
      query = "time%5B0:100:200%5D,longitude%5Blast%5D";
      tName = gridDataset.makeNewFileForDapQuery(language, null, null, query, EDStatic.fullTestCacheDirectory,
          gridDataset.className() + "_Axis", ".asc");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      // String2.log(results);
      expected = "Dataset {\n" +
          "  Float64 time[time = 3];\n" +
          "  Float64 longitude[longitude = 1];\n" +
          "} "; // r
      expected2 = "MHchla8day;\n" +
          "---------------------------------------------\n" +
          "Data:\n" +
          "time[3]\n" +
          "1.0260864E9, 1.0960704E9, 1.1661408E9\n" +
          "longitude[1]\n" +
          "360.0\n";
      Test.ensureEqual(results, expected + "r" + expected2, "RESULTS=\n" + results);

      if (testLocalErddapToo) {
        // 2018-08-08 this is an important test of SSR.getUrlResponseStringUnchanged
        // following a redirect from http to https,
        // which was previously not supported in SSR
        query = "time%5B0:100:200%5D,longitude%5Blast%5D";
        results = SSR.getUrlResponseStringUnchanged(localUrl + ".asc?" + query);
        Test.ensureEqual(results, expected + "erd" + expected2, "\nresults=\n" + results);
      }

      // .csv
      String2.log("\n*** EDDGridFromErddap test get .CSV axis data\n");
      query = SSR.minimalPercentEncode("time[0:100:200],longitude[last]");
      tName = gridDataset.makeNewFileForDapQuery(language, null, null, query, EDStatic.fullTestCacheDirectory,
          gridDataset.className() + "_Axis", ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      // String2.log(results);
      // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
      expected = "time,longitude\n" +
          "UTC,degrees_east\n" +
          "2002-07-08T00:00:00Z,360.0\n" +
          "2004-09-25T00:00:00Z,NaN\n" +
          "2006-12-15T00:00:00Z,NaN\n";
      Test.ensureEqual(results, expected, "RESULTS=\n" + results);

      if (testLocalErddapToo) {
        results = SSR.getUrlResponseStringUnchanged(localUrl + ".csv?" + query);
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
      }

      // .csv test of gridName.axisName notation
      String2.log("\n*** EDDGridFromErddap test get .CSV axis data\n");
      query = SSR.minimalPercentEncode("chlorophyll.time[0:100:200],chlorophyll.longitude[last]");
      tName = gridDataset.makeNewFileForDapQuery(language, null, null, query, EDStatic.fullTestCacheDirectory,
          gridDataset.className() + "_AxisG.A", ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      // String2.log(results);
      // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
      expected = "time,longitude\n" +
          "UTC,degrees_east\n" +
          "2002-07-08T00:00:00Z,360.0\n" +
          "2004-09-25T00:00:00Z,NaN\n" +
          "2006-12-15T00:00:00Z,NaN\n";
      Test.ensureEqual(results, expected, "RESULTS=\n" + results);

      if (testLocalErddapToo) {
        results = SSR.getUrlResponseStringUnchanged(localUrl + ".csv?" + query);
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
      }

      // .dods
      String2.log("\n*** EDDGridFromErddap test get .DODS axis data\n");
      query = SSR.minimalPercentEncode("time[0:100:200],longitude[last]");
      tName = gridDataset.makeNewFileForDapQuery(language, null, null, query, EDStatic.fullTestCacheDirectory,
          gridDataset.className() + "_Axis", ".dods");
      results = String2.annotatedString(File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName));
      // String2.log(results);
      expected = "Dataset {[10]\n" +
          "  Float64 time[time = 3];[10]\n" +
          "  Float64 longitude[longitude = 1];[10]\n" +
          "} "; // r
      expected2 = "MHchla8day;[10]\n" +
          "[10]\n" +
          "Data:[10]\n" +
          "[0][0][0][3][0][0][0][3]A[206][148]k[0][0][0][0]A[208]U-@[0][0][0]A[209]`y`[0][0][0][0][0][0][1][0][0][0][1]@v[128][0][0][0][0][0][end]";
      Test.ensureEqual(results, expected + "r" + expected2, "RESULTS=\n" + results);

      if (testLocalErddapToo) {
        results = String2.annotatedString(SSR.getUrlResponseStringUnchanged(localUrl + ".dods?" + query));
        Test.ensureEqual(results, expected + "erd" + expected2, "\nresults=\n" + results);
      }

      // .mat
      // octave> load('c:/temp/griddap/EDDGridFromErddap_Axis.mat');
      // octave> erdMHchla8day
      String matlabAxisQuery = SSR.minimalPercentEncode("time[0:100:200],longitude[last]");
      String2.log("\n*** EDDGridFromErddap test get .MAT axis data\n");
      tName = gridDataset.makeNewFileForDapQuery(language, null, null, matlabAxisQuery, EDStatic.fullTestCacheDirectory,
          gridDataset.className() + "_Axis", ".mat");
      String2.log(".mat test file is " + EDStatic.fullTestCacheDirectory + tName);
      results = File2.hexDump(EDStatic.fullTestCacheDirectory + tName, 1000000);
      String2.log(results);
      expected = "4d 41 54 4c 41 42 20 35   2e 30 20 4d 41 54 2d 66   MATLAB 5.0 MAT-f |\n" +
          "69 6c 65 2c 20 43 72 65   61 74 65 64 20 62 79 3a   ile, Created by: |\n" +
          "20 67 6f 76 2e 6e 6f 61   61 2e 70 66 65 6c 2e 63    gov.noaa.pfel.c |\n" +
          "6f 61 73 74 77 61 74 63   68 2e 4d 61 74 6c 61 62   oastwatch.Matlab |\n" +
          // "2c 20 43 72 65 61 74 65 64 20 6f 6e 3a 20 54 75 , Created on: Tu |\n" +
          // "65 20 4f 63 74 20 31 34 20 30 38 3a 35 36 3a 35 e Oct 14 08:56:5 |\n" +
          // "34 20 32 30 30 38 20 20 20 20 20 20 20 20 20 20 4 2008 |\n" +
          "20 20 20 20 00 00 00 00   00 00 00 00 01 00 4d 49                 MI |\n" +
          "00 00 00 0e 00 00 01 18   00 00 00 06 00 00 00 08                    |\n" +
          "00 00 00 02 00 00 00 00   00 00 00 05 00 00 00 08                    |\n" +
          "00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 0b                    |\n" +
          "72 4d 48 63 68 6c 61 38   64 61 79 00 00 00 00 00   rMHchla8day      |\n" +
          "00 04 00 05 00 00 00 20   00 00 00 01 00 00 00 40                  @ |\n" +
          "74 69 6d 65 00 00 00 00   00 00 00 00 00 00 00 00   time             |\n" +
          "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
          "6c 6f 6e 67 69 74 75 64   65 00 00 00 00 00 00 00   longitude        |\n" +
          "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
          "00 00 00 0e 00 00 00 48   00 00 00 06 00 00 00 08          H         |\n" +
          "00 00 00 06 00 00 00 00   00 00 00 05 00 00 00 08                    |\n" +
          "00 00 00 03 00 00 00 01   00 00 00 01 00 00 00 00                    |\n" +
          "00 00 00 09 00 00 00 18   41 ce 94 6b 00 00 00 00           A  k     |\n" +
          "41 d0 55 2d 40 00 00 00   41 d1 60 79 60 00 00 00   A U-@   A `y`    |\n" +
          "00 00 00 0e 00 00 00 38   00 00 00 06 00 00 00 08          8         |\n" +
          "00 00 00 06 00 00 00 00   00 00 00 05 00 00 00 08                    |\n" +
          "00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 00                    |\n" +
          "00 00 00 09 00 00 00 08   40 76 80 00 00 00 00 00           @v       |\n";
      Test.ensureEqual(
          results.substring(0, 71 * 4) + results.substring(71 * 7), // remove the creation dateTime
          expected, "RESULTS(" + EDStatic.fullTestCacheDirectory + tName + ")=\n" + results);

      // .ncHeader
      String2.log("\n*** EDDGridFromErddap test get .NCHEADER axis data\n");
      query = SSR.minimalPercentEncode("time[0:100:200],longitude[last]");
      tName = gridDataset.makeNewFileForDapQuery(language, null, null, query, EDStatic.fullTestCacheDirectory,
          gridDataset.className() + "_Axis", ".ncHeader");
      results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
      // String2.log(results);
      expected =
          // "netcdf EDDGridFromErddap_Axis.nc {\n" +
          // " dimensions:\n" +
          // " time = 3;\n" + // (has coord.var)\n" + //changed when switched to
          // netcdf-java 4.0, 2009-02-23
          // " longitude = 1;\n" + // (has coord.var)\n" + //but won't change for
          // testLocalErddapToo until next release
          "  variables:\n" +
              "    double time(time=3);\n" +
              "      :_CoordinateAxisType = \"Time\";\n" +
              "      :actual_range = 1.0260864E9, 1.1661408E9; // double\n" + // up-to-date
              "      :axis = \"T\";\n" +
              "      :fraction_digits = 0; // int\n" +
              "      :ioos_category = \"Time\";\n" +
              "      :long_name = \"Centered Time\";\n" +
              "      :standard_name = \"time\";\n" +
              "      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
              "      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
              "\n" +
              "    double longitude(longitude=1);\n" +
              "      :_CoordinateAxisType = \"Lon\";\n" +
              "      :actual_range = 360.0, 360.0; // double\n" +
              "      :axis = \"X\";\n" +
              "      :coordsys = \"geographic\";\n" +
              "      :fraction_digits = 4; // int\n" +
              "      :ioos_category = \"Location\";\n" +
              "      :long_name = \"Longitude\";\n" +
              "      :point_spacing = \"even\";\n" +
              "      :standard_name = \"longitude\";\n" +
              "      :units = \"degrees_east\";\n" +
              "\n" +
              "  // global attributes:\n" +
              "  :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n";
      tPo = results.indexOf("  variables:\n");
      Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
      Test.ensureEqual(results.substring(tPo, tPo + expected.length()), expected, "RESULTS=\n" + results);
      expected2 = "  :title = \"Chlorophyll-a, Aqua MODIS, NPP, 2002-2013, DEPRECATED OLDER VERSION (8 Day Composite)\";\n"
          +
          "  :Westernmost_Easting = 360.0; // double\n" +
          "}\n";
      Test.ensureTrue(results.indexOf(expected2) > 0, "RESULTS=\n" + results);

      if (testLocalErddapToo) {
        results = SSR.getUrlResponseStringUnchanged(localUrl + ".ncHeader?" + query);
        tPo = results.indexOf("  variables:\n");
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(results.substring(tPo, tPo + expected.length()), expected,
            "\nresults=\n" + results);
        Test.ensureTrue(results.indexOf(expected2) > 0, "RESULTS=\n" + results);
      }

      // ********************************************** test getting grid data
      // .csv
      String2.log("\n*** EDDGridFromErddap test get .CSV data\n");
      tName = gridDataset.makeNewFileForDapQuery(language, null, null, userDapQuery, EDStatic.fullTestCacheDirectory,
          gridDataset.className() + "_Data", ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      expected = // missing values are "NaN"
          // pre 2010-10-26 was
          // "time, altitude, latitude, longitude, chlorophyll\n" +
          // "UTC, m, degrees_north, degrees_east, mg m-3\n" +
          // "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 224.98437319134158, NaN\n" +
          // "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 225.40108808889917, NaN\n" +
          // "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 225.81780298645677, 0.099\n"
          // +
          // "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 226.23451788401434, 0.118\n"
          // +
          // "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 226.65123278157193, NaN\n" +
          // "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 227.06794767912953, 0.091\n";
          "time,altitude,latitude,longitude,chlorophyll\n" +
              "UTC,m,degrees_north,degrees_east,mg m-3\n" +
              "2007-02-06T00:00:00Z,0.0,28.985876360268577,224.98437319134158,NaN\n" +
              "2007-02-06T00:00:00Z,0.0,28.985876360268577,225.40108808889917,NaN\n" +
              // pre 2012-08-17 was
              // "2007-02-06T00:00:00Z,0.0,28.985876360268577,225.81780298645677,0.10655\n" +
              // "2007-02-06T00:00:00Z,0.0,28.985876360268577,226.23451788401434,0.12478\n";
              "2007-02-06T00:00:00Z,0.0,28.985876360268577,225.81780298645677,0.11093\n" +
              "2007-02-06T00:00:00Z,0.0,28.985876360268577,226.23451788401434,0.12439\n";
      Test.ensureTrue(results.indexOf(expected) == 0, "RESULTS=\n" + results);
      expected2 =
          // pre 2010-10-26 was
          // "2007-02-06T00:00:00Z, 0.0, 49.407270201435495, 232.06852644982058, 0.37\n";
          // pre 2012-08-17 was
          // "2007-02-06T00:00:00Z,0.0,49.407270201435495,232.06852644982058,0.58877\n";
          "2007-02-06T00:00:00Z,0.0,49.407270201435495,232.06852644982058,0.56545\n";
      Test.ensureTrue(results.indexOf(expected2) > 0, "RESULTS=\n" + results);

      if (testLocalErddapToo) {
        results = SSR.getUrlResponseStringUnchanged(localUrl + ".csv?" + userDapQuery);
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        Test.ensureTrue(results.indexOf(expected2) > 0, "RESULTS=\n" + results);
      }

      // .csv test gridName.gridName notation
      String2.log("\n*** EDDGridFromErddap test get .CSV data\n");
      tName = gridDataset.makeNewFileForDapQuery(language, null, null, "chlorophyll." + userDapQuery,
          EDStatic.fullTestCacheDirectory, gridDataset.className() + "_DotNotation", ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      expected =
          // pre 2010-10-26 was
          // "time, altitude, latitude, longitude, chlorophyll\n" +
          // "UTC, m, degrees_north, degrees_east, mg m-3\n" +
          // "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 224.98437319134158, NaN\n" +
          // "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 225.40108808889917, NaN\n" +
          // "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 225.81780298645677, 0.099\n"
          // +
          // "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 226.23451788401434, 0.118\n"
          // +
          // "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 226.65123278157193, NaN\n" +
          // "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 227.06794767912953, 0.091\n";
          "time,altitude,latitude,longitude,chlorophyll\n" +
              "UTC,m,degrees_north,degrees_east,mg m-3\n" +
              "2007-02-06T00:00:00Z,0.0,28.985876360268577,224.98437319134158,NaN\n" +
              "2007-02-06T00:00:00Z,0.0,28.985876360268577,225.40108808889917,NaN\n" +
              // pre 2012-08-17 was
              // "2007-02-06T00:00:00Z,0.0,28.985876360268577,225.81780298645677,0.10655\n" +
              // "2007-02-06T00:00:00Z,0.0,28.985876360268577,226.23451788401434,0.12478\n";
              "2007-02-06T00:00:00Z,0.0,28.985876360268577,225.81780298645677,0.11093\n" +
              "2007-02-06T00:00:00Z,0.0,28.985876360268577,226.23451788401434,0.12439\n";
      Test.ensureTrue(results.indexOf(expected) == 0, "RESULTS=\n" + results);
      expected2 =
          // pre 2010-10-26 was
          // "2007-02-06T00:00:00Z, 0.0, 49.407270201435495, 232.06852644982058, 0.37\n";
          // pre 2012-08-17 was
          // "2007-02-06T00:00:00Z,0.0,49.407270201435495,232.06852644982058,0.58877\n";
          "2007-02-06T00:00:00Z,0.0,49.407270201435495,232.06852644982058,0.56545\n";
      Test.ensureTrue(results.indexOf(expected2) > 0, "RESULTS=\n" + results);

      if (testLocalErddapToo) {
        results = SSR.getUrlResponseStringUnchanged(localUrl + ".csv" + "?chlorophyll." + userDapQuery);
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        Test.ensureTrue(results.indexOf(expected2) > 0, "RESULTS=\n" + results);
      }

      // .nc
      String2.log("\n*** EDDGridFromErddap test get .NC data\n");
      tName = gridDataset.makeNewFileForDapQuery(language, null, null, userDapQuery, EDStatic.fullTestCacheDirectory,
          gridDataset.className() + "_Data", ".nc");
      results = NcHelper.ncdump(EDStatic.fullTestCacheDirectory + tName, "");
      expected = "netcdf EDDGridFromErddap_Data.nc \\{\n" +
          "  dimensions:\n" +
          "    time = 1;\n" + // (has coord.var)\n" + //changed when switched to netcdf-java 4.0, 2009-02-23
          "    altitude = 1;\n" + // (has coord.var)\n" +
          "    latitude = 51;\n" + // (has coord.var)\n" +
          "    longitude = 53;\n" + // (has coord.var)\n" +
          "  variables:\n" +
          "    double time\\(time=1\\);\n" +
          "      :_CoordinateAxisType = \"Time\";\n" +
          "      :actual_range = 1.17072E9, 1.17072E9; // double\n" +
          "      :axis = \"T\";\n" +
          "      :fraction_digits = 0; // int\n" +
          "      :ioos_category = \"Time\";\n" +
          "      :long_name = \"Centered Time\";\n" +
          "      :standard_name = \"time\";\n" +
          "      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
          "      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
          "\n" +
          "    double altitude\\(altitude=1\\);\n" +
          "      :_CoordinateAxisType = \"Height\";\n" +
          "      :_CoordinateZisPositive = \"up\";\n" +
          "      :actual_range = 0.0, 0.0; // double\n" +
          "      :axis = \"Z\";\n" +
          "      :fraction_digits = 0; // int\n" +
          "      :ioos_category = \"Location\";\n" +
          "      :long_name = \"Altitude\";\n" +
          "      :positive = \"up\";\n" +
          "      :standard_name = \"altitude\";\n" +
          "      :units = \"m\";\n" +
          "\n" +
          "    double latitude\\(latitude=51\\);\n" +
          "      :_CoordinateAxisType = \"Lat\";\n" +
          "      :actual_range = 28.985876360268577, 49.82403334105115; // double\n" +
          "      :axis = \"Y\";\n" +
          "      :coordsys = \"geographic\";\n" +
          "      :fraction_digits = 4; // int\n" +
          "      :ioos_category = \"Location\";\n" +
          "      :long_name = \"Latitude\";\n" +
          "      :point_spacing = \"even\";\n" +
          "      :standard_name = \"latitude\";\n" +
          "      :units = \"degrees_north\";\n" +
          "\n" +
          "    double longitude\\(longitude=53\\);\n" +
          "      :_CoordinateAxisType = \"Lon\";\n" +
          "      :actual_range = 224.98437319134158, 246.65354786433613; // double\n" +
          "      :axis = \"X\";\n" +
          "      :coordsys = \"geographic\";\n" +
          "      :fraction_digits = 4; // int\n" +
          "      :ioos_category = \"Location\";\n" +
          "      :long_name = \"Longitude\";\n" +
          "      :point_spacing = \"even\";\n" +
          "      :standard_name = \"longitude\";\n" +
          "      :units = \"degrees_east\";\n" +
          "\n" +
          "    float chlorophyll\\(time=1, altitude=1, latitude=51, longitude=53\\);\n" +
          "      :_FillValue = -9999999.0f; // float\n" +
          "      :colorBarMaximum = 30.0; // double\n" +
          "      :colorBarMinimum = 0.03; // double\n" +
          "      :colorBarScale = \"Log\";\n" +
          "      :coordsys = \"geographic\";\n" +
          "      :fraction_digits = 2; // int\n" +
          "      :ioos_category = \"Ocean Color\";\n" +
          "      :long_name = \"Concentration Of Chlorophyll In Sea Water\";\n" +
          "      :missing_value = -9999999.0f; // float\n" +
          "      :standard_name = \"concentration_of_chlorophyll_in_sea_water\";\n" +
          "      :units = \"mg m-3\";\n" +
          "\n" +
          "  // global attributes:\n" +
          "  :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
          "  :cdm_data_type = \"Grid\";\n" +
          "  :composite = \"true\";\n" +
          "  :contributor_name = \"NASA GSFC \\(OBPG\\)\";\n" +
          "  :contributor_role = \"Source of level 2 data.\";\n" +
          "  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
          "  :creator_email = \"erd.data@noaa.gov\";\n" +
          "  :creator_name = \"NOAA NMFS SWFSC ERD\";\n" +
          "  :creator_type = \"institution\";\n" +
          "  :creator_url = \"https://www.pfeg.noaa.gov\";\n" +
          "  :date_created = \"201.-..-..\";\n" + // changes periodically
          "  :date_issued = \"201.-..-..\";\n" + // changes periodically
          "  :Easternmost_Easting = 246.65354786433613; // double\n" +
          "  :geospatial_lat_max = 49.82403334105115; // double\n" +
          "  :geospatial_lat_min = 28.985876360268577; // double\n" +
          "  :geospatial_lat_resolution = 0.041676313961565174; // double\n" +
          "  :geospatial_lat_units = \"degrees_north\";\n" +
          "  :geospatial_lon_max = 246.65354786433613; // double\n" +
          "  :geospatial_lon_min = 224.98437319134158; // double\n" +
          "  :geospatial_lon_resolution = 0.04167148975575877; // double\n" +
          "  :geospatial_lon_units = \"degrees_east\";\n" +
          "  :geospatial_vertical_max = 0.0; // double\n" +
          "  :geospatial_vertical_min = 0.0; // double\n" +
          "  :geospatial_vertical_positive = \"up\";\n" +
          "  :geospatial_vertical_units = \"m\";\n" +
          "  :history = \"NASA GSFC \\(OBPG\\)";
      tPo = results.indexOf(":history = \"NASA GSFC (OBPG)");
      Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
      Test.ensureLinesMatch(results.substring(0, tPo + 28), expected, "RESULTS=\n" + results);

      expected = // note original missing values
          "  :infoUrl = \"https://coastwatch.pfeg.noaa.gov/infog/MH_chla_las.html\";\n" +
              "  :institution = \"NOAA NMFS SWFSC ERD\";\n" +
              "  :keywords = \"8-day, aqua, chemistry, chlorophyll, chlorophyll-a, coastwatch, color, concentration, concentration_of_chlorophyll_in_sea_water, day, degrees, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, global, modis, noaa, npp, ocean, ocean color, oceans, quality, science, science quality, sea, seawater, water, wcn\";\n"
              +
              "  :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
              "  :license = \"The data may be used and redistributed for free but is not intended\n" +
              "for legal use, since it may contain inaccuracies. Neither the data\n" +
              "Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
              "of their employees or contractors, makes any warranty, express or\n" +
              "implied, including warranties of merchantability and fitness for a\n" +
              "particular purpose, or assumes any legal liability for the accuracy,\n" +
              "completeness, or usefulness, of this information.\";\n" +
              "  :naming_authority = \"gov.noaa.pfeg.coastwatch\";\n" +
              "  :Northernmost_Northing = 49.82403334105115; // double\n" +
              "  :origin = \"NASA GSFC \\(OBPG\\)\";\n" +
              "  :processing_level = \"3\";\n" +
              "  :project = \"CoastWatch \\(https://coastwatch.noaa.gov/\\)\";\n" +
              "  :projection = \"geographic\";\n" +
              "  :projection_type = \"mapped\";\n" +
              "  :publisher_email = \"erd.data@noaa.gov\";\n" +
              "  :publisher_name = \"NOAA NMFS SWFSC ERD\";\n" +
              "  :publisher_type = \"institution\";\n" +
              "  :publisher_url = \"https://www.pfeg.noaa.gov\";\n" +
              "  :references = \"Aqua/MODIS information: https://oceancolor.gsfc.nasa.gov/ . MODIS information: https://coastwatch.noaa.gov/modis_ocolor_overview.html .\";\n"
              +
              "  :satellite = \"Aqua\";\n" +
              "  :sensor = \"MODIS\";\n" +
              "  :source = \"satellite observation: Aqua, MODIS\";\n" +
              "  :sourceUrl = \"https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day\";\n" +
              "  :Southernmost_Northing = 28.985876360268577; // double\n" +
              "  :standard_name_vocabulary = \"CF Standard Name Table v70\";\n" +
              "  :summary = \"NOAA CoastWatch distributes chlorophyll-a concentration data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer \\(MODIS\\) carried aboard the spacecraft.   This is Science Quality data.\";\n"
              +
              "  :time_coverage_end = \"2007-02-06T00:00:00Z\";\n" +
              "  :time_coverage_start = \"2007-02-06T00:00:00Z\";\n" +
              "  :title = \"Chlorophyll-a, Aqua MODIS, NPP, 2002-2013, DEPRECATED OLDER VERSION \\(8 Day Composite\\)\";\n"
              +
              "  :Westernmost_Easting = 224.98437319134158; // double\n" +
              "\n" +
              "  data:\n" +
              "    time = \n" +
              "      \\{1.17072E9\\}\n" +
              "    altitude = \n" +
              "      \\{0.0\\}\n" +
              "    latitude = \n" +
              "      \\{28.985876360268577, 29.40263949988423, 29.81940263949987, 30.236165779115524, 30.652928918731178, 31.06969205834683, 31.486455197962485, 31.90321833757814, 32.31998147719379, 32.73674461680943, 33.153507756425086, 33.57027089604074, 33.98703403565639, 34.40379717527205, 34.8205603148877, 35.237323454503354, 35.65408659411899, 36.07084973373465, 36.4876128733503, 36.904376012965955, 37.32113915258161, 37.73790229219726, 38.1546654318129, 38.571428571428555, 38.98819171104421, 39.40495485065986, 39.821717990275516, 40.23848112989117, 40.655244269506824, 41.07200740912248, 41.48877054873813, 41.905533688353785, 42.32229682796944, 42.73905996758509, 43.155823107200746, 43.57258624681637, 43.989349386432025, 44.40611252604768, 44.82287566566333, 45.239638805278986, 45.65640194489464, 46.07316508451029, 46.48992822412595, 46.9066913637416, 47.323454503357254, 47.74021764297291, 48.15698078258856, 48.573743922204216, 48.99050706181987, 49.407270201435495, 49.82403334105115\\}\n"
              +
              "    longitude = \n" +
              "      \\{224.98437319134158, 225.40108808889917, 225.81780298645677, 226.23451788401434, 226.65123278157193, 227.06794767912953, 227.4846625766871, 227.9013774742447, 228.3180923718023, 228.73480726935986, 229.15152216691746, 229.56823706447506, 229.98495196203262, 230.40166685959022, 230.81838175714782, 231.2350966547054, 231.65181155226298, 232.06852644982058, 232.48524134737815, 232.90195624493575, 233.31867114249334, 233.7353860400509, 234.1521009376085, 234.5688158351661, 234.98553073272367, 235.40224563028127, 235.81896052783887, 236.23567542539644, 236.65239032295403, 237.06910522051163, 237.48582011806923, 237.9025350156268, 238.3192499131844, 238.735964810742, 239.15267970829956, 239.56939460585716, 239.98610950341475, 240.40282440097232, 240.81953929852992, 241.23625419608751, 241.65296909364508, 242.06968399120268, 242.48639888876028, 242.90311378631785, 243.31982868387544, 243.73654358143304, 244.1532584789906, 244.5699733765482, 244.9866882741058, 245.40340317166337, 245.82011806922097, 246.23683296677856, 246.65354786433613\\}\n"
              +
              "    chlorophyll = \n" +
              "      \\{\n" +
              "        \\{\n" +
              "          \\{\n" +
              // pre 2010-10-26 was
              // " {-9999999.0, -9999999.0, 0.099, 0.118, -9999999.0, 0.091, -9999999.0,
              // 0.088, 0.085, 0.088, -9999999.0, 0.098, -9999999.0, 0.076, -9999999.0, 0.07,
              // 0.071, -9999999.0, -9999999.0, -9999999.0, 0.078, -9999999.0, 0.09, 0.084,
              // -9999999.0, -9999999.0, 0.098, -9999999.0, 0.079, 0.076, 0.085, -9999999.0,
              // 0.086, 0.127, 0.199, 0.167, 0.191, 0.133, 0.14, 0.173, 0.204, 0.239, 0.26,
              // 0.252, 0.274, 0.289, 0.367, 0.37, 0.65, 0.531, -9999999.0, -9999999.0,
              // 1.141},";
              // pre 2012-08-17 was
              // " {-9999999.0, -9999999.0, 0.10655, 0.12478, -9999999.0, 0.09398, -9999999.0,
              // 0.08919, 0.09892, 0.10007, -9999999.0, 0.09986, -9999999.0, 0.07119,
              // -9999999.0, 0.08288, 0.08163, -9999999.0, -9999999.0, -9999999.0, 0.08319,
              // -9999999.0, 0.09706, 0.08309, -9999999.0, -9999999.0, 0.0996, -9999999.0,
              // 0.08962, 0.08329, 0.09101, -9999999.0, 0.08679, 0.13689, 0.21315, 0.18729,
              // 0.21642, 0.15069, 0.15123, 0.18849, 0.22975, 0.27075, 0.29062, 0.27878,
              // 0.31141, 0.32663, 0.41135, 0.40628, 0.65426, 0.4827, -9999999.0, -9999999.0,
              // 1.16268},";
              "            \\{-9999999.0, -9999999.0, 0.11093, 0.12439, -9999999.0, 0.09554, -9999999.0, 0.09044, 0.10009, 0.10116, -9999999.0, 0.10095, -9999999.0, 0.07243, -9999999.0, 0.08363, 0.08291, -9999999.0, -9999999.0, -9999999.0, 0.08885, -9999999.0, 0.09632, 0.0909, -9999999.0, -9999999.0, 0.09725, -9999999.0, 0.09978, 0.09462, 0.09905, -9999999.0, 0.09937, 0.12816, 0.20255, 0.17595, 0.20562, 0.14333, 0.15073, 0.18803, 0.22673, 0.27252, 0.29005, 0.28787, 0.31865, 0.33447, 0.43293, 0.43297, 0.68101, 0.48409, -9999999.0, -9999999.0, 1.20716\\},"; // no
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  // \n
      tPo = results.indexOf("  :infoUrl");
      int tPo2 = results.indexOf("1.20716},", tPo + 1);
      if (tPo < 0 || tPo2 < 0)
        String2.log("tPo=" + tPo + " tPo2=" + tPo2 + " results=\n" + results);

      Test.ensureLinesMatch(results.substring(tPo, tPo2 + 9), expected, "RESULTS=\n" + results);
      /* */

    } catch (Throwable t) {
      throw new RuntimeException(
          "*** This EDDGridFromErddap test requires erdMHchla8day on coastwatch's erddap" +
              (testLocalErddapToo ? "\n    AND rMHchla8day on localhost's erddap." : ""),
          t);
    }
  }

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
