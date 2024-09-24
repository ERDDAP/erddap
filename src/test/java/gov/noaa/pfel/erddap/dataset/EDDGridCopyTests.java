package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tags.TagIncompleteTest;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

class EDDGridCopyTests {

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /** The basic tests of this class (erdGlobecBottle). */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testBasic(boolean checkSourceData) throws Throwable {
    // String2.log("\n****************** EDDGridCopy.testBasic(checkSourceData=" +
    // checkSourceData +
    // ") *****************\n");
    // testVerboseOn();
    EDDGridCopy.defaultCheckSourceData = checkSourceData;
    String name, tName, results, tResults, expected, userDapQuery, tQuery;
    String error = "";
    EDV edv;
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard to
    // check min:sec.
    EDDGrid eddGrid = null;
    int language = 0;

    try {
      eddGrid = (EDDGridCopy) EDDTestDataset.gettestGridCopy();
    } catch (Throwable t2) {
      // it will fail if no files have been copied
      String2.log(MustBe.throwableToString(t2));
    }
    if (checkSourceData) {
      while (EDStatic.nUnfinishedTasks() > 0) {
        String2.log("nUnfinishedTasks=" + EDStatic.nUnfinishedTasks());
        Math2.sleep(10000);
      }
      // recreate edd to see new copied data files
      eddGrid = (EDDGridCopy) EDDTestDataset.gettestGridCopy();
    }

    // *** test getting das for entire dataset
    String2.log("\n*** .nc test das dds for entire dataset\n");
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            eddGrid.className() + "_Entire",
            ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "Attributes {\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range MIN, MAX;\n"
            + "    String axis \"T\";\n"
            + "    Int32 fraction_digits 0;\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Centered Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  altitude {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    String _CoordinateZisPositive \"up\";\n"
            + "    Float64 actual_range MIN, MAX;\n"
            + "    String axis \"Z\";\n"
            + "    Int32 fraction_digits 0;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Altitude\";\n"
            + "    String positive \"up\";\n"
            + "    String standard_name \"altitude\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float64 actual_range MIN, MAX;\n"
            + "    String axis \"Y\";\n"
            + "    String coordsys \"geographic\";\n"
            + "    Int32 fraction_digits 2;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String point_spacing \"even\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range MIN, MAX;\n"
            + "    String axis \"X\";\n"
            + "    String coordsys \"geographic\";\n"
            + "    Int32 fraction_digits 2;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String point_spacing \"even\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  x_wind {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float64 colorBarMaximum 15.0;\n"
            + "    Float64 colorBarMinimum -15.0;\n"
            + "    String coordsys \"geographic\";\n"
            + "    Int32 fraction_digits 1;\n"
            + "    String ioos_category \"Wind\";\n"
            + "    String long_name \"Zonal Wind\";\n"
            + "    Float32 missing_value -9999999.0;\n"
            + "    String standard_name \"x_wind\";\n"
            + "    String units \"m s-1\";\n"
            + "  }\n"
            + "  y_wind {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float64 colorBarMaximum 15.0;\n"
            + "    Float64 colorBarMinimum -15.0;\n"
            + "    String coordsys \"geographic\";\n"
            + "    Int32 fraction_digits 1;\n"
            + "    String ioos_category \"Wind\";\n"
            + "    String long_name \"Meridional Wind\";\n"
            + "    Float32 missing_value -9999999.0;\n"
            + "    String standard_name \"y_wind\";\n"
            + "    String units \"m s-1\";\n"
            + "  }\n"
            + "  mod {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float64 colorBarMaximum 18.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String colorBarPalette \"WhiteRedBlack\";\n"
            + "    String coordsys \"geographic\";\n"
            + "    Int32 fraction_digits 1;\n"
            + "    String ioos_category \"Wind\";\n"
            + "    String long_name \"Modulus of Wind\";\n"
            + "    Float32 missing_value -9999999.0;\n"
            + "    String units \"m s-1\";\n"
            + "  }\n"
            + "  NC_GLOBAL {\n"
            + "    String acknowledgement \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n"
            + "    String cdm_data_type \"Grid\";\n"
            + "    String composite \"true\";\n"
            + "    String contributor_name \"Remote Sensing Systems, Inc\";\n"
            + "    String contributor_role \"Source of level 2 data.\";\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "    String creator_email \"erd.data@noaa.gov\";\n"
            + "    String creator_name \"NOAA CoastWatch, West Coast Node\";\n"
            + "    String creator_url \"https://coastwatch.pfeg.noaa.gov\";\n"
            + "    String date_created \"2008-08-29\";\n"
            + "    String date_issued \"2008-08-29\";\n"
            + "    Float64 Easternmost_Easting 359.875;\n"
            + "    Float64 geospatial_lat_max 89.875;\n"
            + "    Float64 geospatial_lat_min -89.875;\n"
            + "    Float64 geospatial_lat_resolution 0.25;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max 359.875;\n"
            + "    Float64 geospatial_lon_min 0.125;\n"
            + "    Float64 geospatial_lon_resolution 0.25;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    Float64 geospatial_vertical_max 0.0;\n"
            + "    Float64 geospatial_vertical_min 0.0;\n"
            + "    String geospatial_vertical_positive \"up\";\n"
            + "    String geospatial_vertical_units \"m\";\n"
            + "    String history \"Remote Sensing Systems, Inc\n"
            + "2008-08-29T00:31:43Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD\n"
            + today;
    results =
        results.replaceAll(
            "Float64 actual_range -?[0-9]+.[0-9]+e.[0-9]+, -?[0-9]+.[0-9]+e.[0-9]+",
            "Float64 actual_range MIN, MAX");
    results =
        results.replaceAll(
            "Float64 actual_range -?[0-9]+.[0-9]+, -?[0-9]+.[0-9]+",
            "Float64 actual_range MIN, MAX");
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

    // + " https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/ux10/1day\n"
    // +
    // today +

    expected =
        "/erddap/griddap/testGridCopy.das\";\n"
            + // different
            "    String infoUrl \"https://coastwatch.pfeg.noaa.gov/infog/QS_ux10_las.html\";\n"
            + "    String institution \"NOAA CoastWatch, West Coast Node\";\n"
            + "    String keywords \"Earth Science > Oceans > Ocean Winds > Surface Winds\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.\";\n"
            + "    String naming_authority \"gov.noaa.pfeg.coastwatch\";\n"
            + "    Float64 Northernmost_Northing 89.875;\n"
            + "    String origin \"Remote Sensing Systems, Inc\";\n"
            + "    String processing_level \"3\";\n"
            + "    String project \"CoastWatch (https://coastwatch.noaa.gov/)\";\n"
            + "    String projection \"geographic\";\n"
            + "    String projection_type \"mapped\";\n"
            + "    String references \"RSS Inc. Winds: http://www.remss.com/ .\";\n"
            + "    String satellite \"QuikSCAT\";\n"
            + "    String sensor \"SeaWinds\";\n"
            + "    String source \"satellite observation: QuikSCAT, SeaWinds\";\n"
            +
            // it's still a numeric ip because the source file was created long ago
            "    String sourceUrl \"http://192.168.31.18/thredds/dodsC/satellite/QS/ux10/1day\";\n"
            + "    Float64 Southernmost_Northing -89.875;\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String summary \"Remote Sensing Inc. distributes science quality wind velocity data from the SeaWinds instrument onboard NASA's QuikSCAT satellite.  SeaWinds is a microwave scatterometer designed to measure surface winds over the global ocean.  Wind velocity fields are provided in zonal, meriodonal, and modulus sets. The reference height for all wind velocities is 10 meters.\";\n"
            + "    String time_coverage_end \"2008-01-10T12:00:00Z\";\n"
            + "    String time_coverage_start \"YYYY-MM-DDTHH:00:00Z\";\n"
            + "    String title \"Wind, QuikSCAT, Global, Science Quality (1 Day Composite)\";\n"
            + "    Float64 Westernmost_Easting 0.125;\n"
            + "  }\n"
            + "}\n";

    results =
        results.replaceAll(
            "String time_coverage_start \\\"....-..-..T..:00:00Z\\\"",
            "String time_coverage_start \"YYYY-MM-DDTHH:00:00Z\"");
    int tpo = results.indexOf(expected.substring(0, 17));
    Test.ensureTrue(tpo > 0, "tpo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tpo, Math.min(results.length(), tpo + expected.length())),
        expected,
        "results=\n" + results);

    // *** test getting dds for entire dataset
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            eddGrid.className() + "_Entire",
            ".dds");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + "  Float64 time[time = NUM];\n"
            + "  Float64 altitude[altitude = 1];\n"
            + "  Float64 latitude[latitude = 720];\n"
            + "  Float64 longitude[longitude = 1440];\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 x_wind[time = NUM][altitude = 1][latitude = 720][longitude = 1440];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = NUM];\n"
            + "      Float64 altitude[altitude = 1];\n"
            + "      Float64 latitude[latitude = 720];\n"
            + "      Float64 longitude[longitude = 1440];\n"
            + "  } x_wind;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 y_wind[time = NUM][altitude = 1][latitude = 720][longitude = 1440];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = NUM];\n"
            + "      Float64 altitude[altitude = 1];\n"
            + "      Float64 latitude[latitude = 720];\n"
            + "      Float64 longitude[longitude = 1440];\n"
            + "  } y_wind;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 mod[time = NUM][altitude = 1][latitude = 720][longitude = 1440];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = NUM];\n"
            + "      Float64 altitude[altitude = 1];\n"
            + "      Float64 latitude[latitude = 720];\n"
            + "      Float64 longitude[longitude = 1440];\n"
            + "  } mod;\n"
            + "} testGridCopy;\n"; // different
    results = results.replaceAll("time = [0-9]+", "time = NUM");
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .csv with data from one file
    String2.log("\n*** .nc test read from one file\n");
    userDapQuery = "y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            eddGrid.className() + "_Data1",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        // verified with
        // https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.csv?y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]
        "time,altitude,latitude,longitude,y_wind\n"
            + "UTC,m,degrees_north,degrees_east,m s-1\n"
            + "2008-01-10T12:00:00Z,0.0,36.625,230.125,3.555585\n"
            + "2008-01-10T12:00:00Z,0.0,36.625,230.875,2.82175\n"
            + "2008-01-10T12:00:00Z,0.0,36.625,231.625,4.539375\n"
            + "2008-01-10T12:00:00Z,0.0,36.625,232.375,4.975015\n"
            + "2008-01-10T12:00:00Z,0.0,36.625,233.125,5.643055\n"
            + "2008-01-10T12:00:00Z,0.0,36.625,233.875,2.72394\n"
            + "2008-01-10T12:00:00Z,0.0,36.625,234.625,1.39762\n"
            + "2008-01-10T12:00:00Z,0.0,36.625,235.375,2.10711\n"
            + "2008-01-10T12:00:00Z,0.0,36.625,236.125,3.019165\n"
            + "2008-01-10T12:00:00Z,0.0,36.625,236.875,3.551915\n"
            + "2008-01-10T12:00:00Z,0.0,36.625,237.625,NaN\n"; // test of NaN
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .csv with data from several files
    String2.log("\n*** .nc test read from several files\n");
    // :3:(1.1999664e9)
    userDapQuery = "y_wind[(1.1999664e9)][0][(36.5)][(230)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            eddGrid.className() + "_Data1",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        // verified with
        // https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.csv?y_wind[(1.1991888e9):3:(1.1999664e9)][0][(36.5)][(230)]
        "time,altitude,latitude,longitude,y_wind\n"
            + "UTC,m,degrees_north,degrees_east,m s-1\n"
            +
            // "2008-01-01T12:00:00Z,0.0,36.625,230.125,7.6282454\n" +
            // "2008-01-04T12:00:00Z,0.0,36.625,230.125,-12.3\n" +
            // "2008-01-07T12:00:00Z,0.0,36.625,230.125,-5.974585\n" +
            "2008-01-10T12:00:00Z,0.0,36.625,230.125,3.555585\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
    // */
    // defaultCheckSourceData = true;
  } // end of testBasic

  /** The tests onlySince. */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest // need to do an assert instead of the manual verification requested at the
  // enter to contiue
  void testOnlySince() throws Throwable {
    // String2.log("\n******* EDDGridCopy.testOnlySince *******\n");
    // testVerboseOn();
    String name, tName, results, tResults, expected, userDapQuery, tQuery;
    String error = "";
    EDV edv;
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard to
    // check min:sec.
    EDDGrid eddGrid = null;
    String tDatasetID = "testOnlySince";
    String copyDatasetDir = EDStatic.fullCopyDirectory + tDatasetID + "/";
    int language = 0;

    try {
      // delete all existing files
      File2.deleteAllFiles(copyDatasetDir);
      eddGrid = (EDDGridCopy) EDDTestDataset.gettestOnlySince();
      String2.pressEnterToContinue("shouldn't get here");
    } catch (Throwable t2) {
      // construction will fail because no files have been copied
    }
    String2.log("Errors above are fine and to be expected.\n" + "Downloading data...");
    while (EDStatic.nUnfinishedTasks() > 0) {
      String2.log("nUnfinishedTasks=" + EDStatic.nUnfinishedTasks());
      Math2.sleep(5000);
    }
    // recreate edd to see new copied data files
    eddGrid = (EDDGridCopy) EDDTestDataset.gettestOnlySince();

    // *** look at the time values
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            "time",
            EDStatic.fullTestCacheDirectory,
            eddGrid.className() + "_time",
            ".csv");
    String2.log("\n" + File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName));
    String2.pressEnterToContinue(
        "The time values shown should only include times since "
            + Calendar2.epochSecondsToIsoStringTZ(Calendar2.nowStringToEpochSeconds("now-3days"))
            + " (with rounding)");
  }
}
