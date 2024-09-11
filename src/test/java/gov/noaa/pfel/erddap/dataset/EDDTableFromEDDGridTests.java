package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.junit.jupiter.api.BeforeAll;
import tags.TagLocalERDDAP;
import testDataset.Initialization;

class EDDTableFromEDDGridTests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /** */
  @org.junit.jupiter.api.Test
  @TagLocalERDDAP // "erdMBsstdmday_AsATable" needs local erddap
  void testBasic() throws Throwable {
    // String2.log("\nEDDTableFromEDDGrid.testBasic()");
    // testVerboseOn();
    // boolean oDebugMode = debugMode;
    // debugMode = false; //normally false. Set it to true if need help.
    String results, query, tName, expected, expected2;
    String id = "erdMBsstdmday_AsATable";
    int language = 0;
    EDDTable tedd = (EDDTable) EDDTableFromEDDGrid.oneFromDatasetsXml(null, id);
    String dir = EDStatic.fullTestCacheDirectory;
    /* */
    // das
    tName =
        tedd.makeNewFileForDapQuery(language, null, null, "", dir, tedd.className() + "1", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range 120.0, 320.0;\n"
            + "    String axis \"X\";\n"
            + "    String coordsys \"geographic\";\n"
            + "    Int32 fraction_digits 3;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String point_spacing \"even\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float64 actual_range -45.0, 65.0;\n"
            + "    String axis \"Y\";\n"
            + "    String coordsys \"geographic\";\n"
            + "    Int32 fraction_digits 3;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String point_spacing \"even\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  altitude {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    String _CoordinateZisPositive \"up\";\n"
            + "    Float64 actual_range 0.0, 0.0;\n"
            + "    String axis \"Z\";\n"
            + "    Int32 fraction_digits 0;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Altitude\";\n"
            + "    String positive \"up\";\n"
            + "    String standard_name \"altitude\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range 1.2030768e+9, 1.2056688e+9;\n"
            + "    String axis \"T\";\n"
            + "    Int32 fraction_digits 0;\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Centered Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  sst {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float64 colorBarMaximum 32.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String coordsys \"geographic\";\n"
            + "    Int32 fraction_digits 1;\n"
            + "    String ioos_category \"Temperature\";\n"
            + "    String long_name \"Sea Surface Temperature\";\n"
            + "    Float32 missing_value -9999999.0;\n"
            + "    String standard_name \"sea_surface_temperature\";\n"
            + "    String units \"degree_C\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String acknowledgement \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n"
            + "    String cdm_data_type \"Point\";\n"
            + "    String composite \"true\";\n"
            + "    String contributor_name \"NASA GSFC (G. Feldman)\";\n"
            + "    String contributor_role \"Source of level 2 data.\";\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "    String creator_email \"erd.data@noaa.gov\";\n"
            + "    String creator_name \"NOAA NMFS SWFSC ERD\";\n"
            + "    String creator_type \"institution\";\n"
            + "    String creator_url \"https://www.pfeg.noaa.gov\";\n"
            + "    String date_created \"2008-04-04\";\n"
            + "    String date_issued \"2008-04-04\";\n"
            + "    Float64 Easternmost_Easting 320.0;\n"
            + "    String featureType \"Point\";\n"
            + "    Float64 geospatial_lat_max 65.0;\n"
            + "    Float64 geospatial_lat_min -45.0;\n"
            + "    Float64 geospatial_lat_resolution 0.025;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max 320.0;\n"
            + "    Float64 geospatial_lon_min 120.0;\n"
            + "    Float64 geospatial_lon_resolution 0.025;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    Float64 geospatial_vertical_max 0.0;\n"
            + "    Float64 geospatial_vertical_min 0.0;\n"
            + "    String geospatial_vertical_positive \"up\";\n"
            + "    String geospatial_vertical_units \"m\";\n"
            + "    String history \"NASA GSFC (G. Feldman)";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    expected2 =
        "String infoUrl \"https://coastwatch.pfeg.noaa.gov/infog/MB_sstd_las.html\";\n"
            + "    String institution \"NOAA NMFS SWFSC ERD\";\n"
            + "    String keywords \"altitude, aqua, coast, coastwatch, data, day, daytime, degrees, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, imaging, MBsstd, moderate, modis, national, noaa, node, npp, ocean, oceans, orbiting, pacific, partnership, polar, polar-orbiting, resolution, sea, sea_surface_temperature, spectroradiometer, sst, surface, temperature, time, wcn, west\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    Int32 maxAxis0 1;\n"
            + "    String naming_authority \"gov.noaa.pfeg.coastwatch\";\n"
            + "    Float64 Northernmost_Northing 65.0;\n"
            + "    String origin \"NASA GSFC (G. Feldman)\";\n"
            + "    String processing_level \"3\";\n"
            + "    String project \"CoastWatch (https://coastwatch.noaa.gov/)\";\n"
            + "    String projection \"geographic\";\n"
            + "    String projection_type \"mapped\";\n"
            + "    String publisher_email \"erd.data@noaa.gov\";\n"
            + "    String publisher_name \"NOAA NMFS SWFSC ERD\";\n"
            + "    String publisher_type \"institution\";\n"
            + "    String publisher_url \"https://www.pfeg.noaa.gov\";\n"
            + "    String references \"Aqua/MODIS information: https://oceancolor.gsfc.nasa.gov/ . MODIS information: https://coastwatch.noaa.gov/cw_html/OceanColor_NRT_MODIS_Aqua.html .\";\n"
            + "    String satellite \"Aqua\";\n"
            + "    String sensor \"MODIS\";\n"
            + "    String source \"satellite observation: Aqua, MODIS\";\n"
            + "    String sourceUrl \"(local files)\";\n"
            + "    Float64 Southernmost_Northing -45.0;\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String summary \"NOTE: This dataset is the tabular version of a gridded dataset which is also\n"
            + "available in this ERDDAP (see datasetID=erdMBsstdmday). Most people, most\n"
            + "of the time, will prefer to use the original gridded version of this dataset.\n"
            + "This dataset presents all of the axisVariables and dataVariables from the\n"
            + "gridded dataset as columns in a huge database-like table of data.\n"
            + "You can query this dataset as you would any other tabular dataset in ERDDAP;\n"
            + "however, if you query any of the original dataVariables, you must constrain\n"
            + "what was axisVariable[0] (usually \\\"time\\\") so that data for no more than 10\n"
            + "axisVariable[0] values are searched.\n"
            + "\n"
            + "---\n"
            + "NOAA CoastWatch provides SST data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.  Currently, only daytime imagery is supported.\";\n"
            + "    String testOutOfDate \"now-60days\";\n"
            + "    String time_coverage_end \"2008-03-16T12:00:00Z\";\n"
            + "    String time_coverage_start \"2008-02-15T12:00:00Z\";\n"
            + "    String title \"SST, Aqua MODIS, NPP, 0.025 degrees, Pacific Ocean, Daytime, 2006-present (Monthly Composite), (As A Table)\";\n"
            + "    Float64 Westernmost_Easting 120.0;\n"
            + "  }\n"
            + "}\n";
    int tPo = results.indexOf("String infoUrl ");
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(results.substring(tPo), expected2, "results=\n" + results);

    // das
    tName =
        tedd.makeNewFileForDapQuery(language, null, null, "", dir, tedd.className() + "2", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    Float64 longitude;\n"
            + "    Float64 latitude;\n"
            + "    Float64 altitude;\n"
            + "    Float64 time;\n"
            + "    Float32 sst;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query 1 axis
    query =
        "latitude&latitude>20&latitude<=20.1"
            + "&longitude=200"; // longitude constraint is ignored (since it's valid)
    tName =
        tedd.makeNewFileForDapQuery(
            language, null, null, query, dir, tedd.className() + "1axis", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "latitude\n" + "degrees_north\n" + "20.025\n" + "20.05\n" + "20.075\n" + "20.1\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query 1 axis =min lon is 120 - 320
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "longitude&longitude=120",
            dir,
            tedd.className() + "1axisMin",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "longitude\n" + "degrees_east\n" + "120.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query 1 axis <=min
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "longitude&longitude<=120",
            dir,
            tedd.className() + "1axisLEMin",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "longitude\n" + "degrees_east\n" + "120.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query 1 axis <min fails but not immediately
    try {
      tName =
          tedd.makeNewFileForDapQuery(
              language,
              null,
              null,
              "longitude&longitude<120",
              dir,
              tedd.className() + "1axisLMin",
              ".csv");
      results = "shouldn't get here";
    } catch (Throwable t) {
      results = "Caught: " + t.toString();
    }
    Test.ensureEqual(
        results,
        "Caught: com.cohort.util.SimpleException: Your query produced no matching results. (nRows = 0)",
        "results=\n" + results);

    // query 1 axis <min fails immediately
    try {
      tName =
          tedd.makeNewFileForDapQuery(
              language,
              null,
              null,
              "longitude&longitude<-119.99",
              dir,
              tedd.className() + "1axisLMin2",
              ".csv");
      results = "shouldn't get here";
    } catch (Throwable t) {
      results = "Caught: " + t.toString();
    }
    Test.ensureEqual(
        results,
        "Caught: com.cohort.util.SimpleException: Your query produced no matching results. "
            + "(longitude<-119.99 is outside of the variable's actual_range: 120.0 to 320.0)",
        "results=\n" + results);

    // query 1 axis =max
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "longitude&longitude=320",
            dir,
            tedd.className() + "1axisMax",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "longitude\n" + "degrees_east\n" + "320.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query 1 axis >=max
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "longitude&longitude>=320",
            dir,
            tedd.className() + "1axisGEMax",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "longitude\n" + "degrees_east\n" + "320.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query 1 axis >max fails but not immediately
    try {
      tName =
          tedd.makeNewFileForDapQuery(
              language,
              null,
              null,
              "longitude&longitude>320",
              dir,
              tedd.className() + "1axisGMax",
              ".csv");
      results = "shouldn't get here";
    } catch (Throwable t) {
      results = "Caught: " + t.toString();
    }
    Test.ensureEqual(
        results,
        "Caught: com.cohort.util.SimpleException: Your query produced no matching results. (nRows = 0)",
        "results=\n" + results);

    // query 1 axis >max fails immediately
    try {
      tName =
          tedd.makeNewFileForDapQuery(
              language,
              null,
              null,
              "longitude&longitude>320.1",
              dir,
              tedd.className() + "1axisGMin2",
              ".csv");
      results = "shouldn't get here";
    } catch (Throwable t) {
      results = "Caught: " + t.toString();
    }
    Test.ensureEqual(
        results,
        "Caught: com.cohort.util.SimpleException: Your query produced no matching results. "
            + "(longitude>320.1 is outside of the variable's actual_range: 120.0 to 320.0)",
        "results=\n" + results);

    // query time axis =min
    String timeMin = "2008-02-15T12:00:00Z";
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "time&time=" + timeMin,
            dir,
            tedd.className() + "timeAxisMin",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "time\n" + "UTC\n" + "2008-02-15T12:00:00Z\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query time axis <=min
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "time&time<=" + timeMin,
            dir,
            tedd.className() + "timeAxisLEMin",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "time\n" + "UTC\n" + "2008-02-15T12:00:00Z\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query time axis <min fails but not immediately (< is tested as <=)
    try {
      tName =
          tedd.makeNewFileForDapQuery(
              language,
              null,
              null,
              "time&time<2008-02-15T12:00:00Z",
              dir,
              tedd.className() + "timeAxisLMin",
              ".csv");
      results = "shouldn't get here";
    } catch (Throwable t) {
      results = "Caught: " + t.toString();
    }
    Test.ensureEqual(
        results,
        "Caught: com.cohort.util.SimpleException: Your query produced no matching results. (nRows = 0)",
        "results=\n" + results);

    // query time axis <=min-1day fails immediately
    try {
      tName =
          tedd.makeNewFileForDapQuery(
              language,
              null,
              null,
              "time&time<=2008-02-14T12",
              dir,
              tedd.className() + "timeAxisLMin2",
              ".csv");
      results = "shouldn't get here";
    } catch (Throwable t) {
      results = "Caught: " + t.toString();
    }
    Test.ensureEqual(
        results,
        "Caught: com.cohort.util.SimpleException: Your query produced no matching results. "
            + "(time<=2008-02-14T12:00:00Z is outside of the variable's actual_range: 2008-02-15T12:00:00Z to 2008-03-16T12:00:00Z)",
        "results=\n" + results);

    // query time axis >max fails
    try {
      tName =
          tedd.makeNewFileForDapQuery(
              language,
              null,
              null,
              "time&time>2008-03-16T12",
              dir,
              tedd.className() + "timeAxisGMax",
              ".csv");
      results = "shouldn't get here";
    } catch (Throwable t) {
      results = "Caught: " + t.toString();
    }
    Test.ensureEqual(
        results,
        "Caught: com.cohort.util.SimpleException: Your query produced no matching results. (nRows = 0)",
        "results=\n" + results);

    // query error
    results = "";
    try {
      query =
          "latitude&latitude>20&latitude<=20.5"
              + "&longitude=201.04"; // invalid: in middle of range but no match
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "1axisInvalid", ".csv");
      results = "shouldn't get here";
    } catch (Throwable t) {
      results = "Caught: " + t.toString();
    }
    expected =
        "Caught: com.cohort.util.SimpleException: Your query produced no matching results. (longitude=201.04 will never be true.)";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query error
    results = "";
    try {
      query =
          "latitude&latitude>20&latitude<=20.5" + "&longitude=119"; // invalid: out of actual_range
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "1axisInvalid", ".csv");
      results = "shouldn't get here";
    } catch (Throwable t) {
      results = "Caught: " + t.toString();
    }
    expected =
        "Caught: com.cohort.util.SimpleException: Your query produced no matching results. "
            + "(longitude=119 is outside of the variable's actual_range: 120.0 to 320.0)";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query 2 axes
    query =
        "longitude,latitude&latitude>20&latitude<=20.1"
            + "&longitude>=215&longitude<215.1&time=\"2008-02-15T12\""; // time constraint is
    // ignored (since it's
    // valid)
    tName =
        tedd.makeNewFileForDapQuery(
            language, null, null, query, dir, tedd.className() + "2axes", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "longitude,latitude\n"
            + "degrees_east,degrees_north\n"
            + "215.0,20.025\n"
            + "215.0,20.05\n"
            + "215.0,20.075\n"
            + "215.0,20.1\n"
            + "215.025,20.025\n"
            + "215.025,20.05\n"
            + "215.025,20.075\n"
            + "215.025,20.1\n"
            + "215.05,20.025\n"
            + "215.05,20.05\n"
            + "215.05,20.075\n"
            + "215.05,20.1\n"
            + "215.07500000000002,20.025\n"
            + "215.07500000000002,20.05\n"
            + "215.07500000000002,20.075\n"
            + "215.07500000000002,20.1\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query all axes (different order)
    query =
        "latitude,longitude,altitude,time&latitude>20&latitude<=20.1"
            + "&longitude>=215&longitude<215.1&time=\"2008-02-15T12\"";
    tName =
        tedd.makeNewFileForDapQuery(
            language, null, null, query, dir, tedd.className() + "allaxes", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "latitude,longitude,altitude,time\n"
            + "degrees_north,degrees_east,m,UTC\n"
            + "20.025,215.0,0.0,2008-02-15T12:00:00Z\n"
            + "20.025,215.025,0.0,2008-02-15T12:00:00Z\n"
            + "20.025,215.05,0.0,2008-02-15T12:00:00Z\n"
            + "20.025,215.07500000000002,0.0,2008-02-15T12:00:00Z\n"
            + "20.05,215.0,0.0,2008-02-15T12:00:00Z\n"
            + "20.05,215.025,0.0,2008-02-15T12:00:00Z\n"
            + "20.05,215.05,0.0,2008-02-15T12:00:00Z\n"
            + "20.05,215.07500000000002,0.0,2008-02-15T12:00:00Z\n"
            + "20.075,215.0,0.0,2008-02-15T12:00:00Z\n"
            + "20.075,215.025,0.0,2008-02-15T12:00:00Z\n"
            + "20.075,215.05,0.0,2008-02-15T12:00:00Z\n"
            + "20.075,215.07500000000002,0.0,2008-02-15T12:00:00Z\n"
            + "20.1,215.0,0.0,2008-02-15T12:00:00Z\n"
            + "20.1,215.025,0.0,2008-02-15T12:00:00Z\n"
            + "20.1,215.05,0.0,2008-02-15T12:00:00Z\n"
            + "20.1,215.07500000000002,0.0,2008-02-15T12:00:00Z\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query error
    results = "";
    String2.log("Here Now!");
    try {
      query =
          "latitude,longitude,altitude,time,chlorophyll&latitude>20&latitude<=20.1"
              + "&longitude>=215&longitude<215.1&time=\"2008-02-15T12\"";
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "1axisInvalid", ".csv");
      results = "shouldn't get here";
    } catch (Throwable t) {
      results = "Caught: " + t.toString();
    }
    expected =
        "Caught: com.cohort.util.SimpleException: Query error: Unrecognized variable=\"chlorophyll\".";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query av and dv, with time constraint
    query =
        "latitude,longitude,altitude,time,sst&latitude>20&latitude<=20.1"
            + "&longitude>=215&longitude<215.1&time=\"2008-02-15T12\"";
    tName =
        tedd.makeNewFileForDapQuery(
            language, null, null, query, dir, tedd.className() + "allaxes", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "latitude,longitude,altitude,time,sst\n"
            + "degrees_north,degrees_east,m,UTC,degree_C\n"
            + "20.025,215.0,0.0,2008-02-15T12:00:00Z,22.4018\n"
            + "20.025,215.025,0.0,2008-02-15T12:00:00Z,22.6585\n"
            + "20.025,215.05,0.0,2008-02-15T12:00:00Z,22.5025\n"
            + "20.025,215.07500000000002,0.0,2008-02-15T12:00:00Z,22.542\n"
            + "20.05,215.0,0.0,2008-02-15T12:00:00Z,22.005\n"
            + "20.05,215.025,0.0,2008-02-15T12:00:00Z,22.8965\n"
            + "20.05,215.05,0.0,2008-02-15T12:00:00Z,22.555\n"
            + "20.05,215.07500000000002,0.0,2008-02-15T12:00:00Z,22.4825\n"
            + "20.075,215.0,0.0,2008-02-15T12:00:00Z,21.7341\n"
            + "20.075,215.025,0.0,2008-02-15T12:00:00Z,22.7635\n"
            + "20.075,215.05,0.0,2008-02-15T12:00:00Z,22.389\n"
            + "20.075,215.07500000000002,0.0,2008-02-15T12:00:00Z,22.4805\n"
            + "20.1,215.0,0.0,2008-02-15T12:00:00Z,22.5244\n"
            + "20.1,215.025,0.0,2008-02-15T12:00:00Z,22.1972\n"
            + "20.1,215.05,0.0,2008-02-15T12:00:00Z,22.81\n"
            + "20.1,215.07500000000002,0.0,2008-02-15T12:00:00Z,22.6944\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // av+dv query with dv and av constraints
    query = // "latitude,longitude,altitude,time,sst&sst>37&time=\"2008-02-15T12\"");
        "latitude,longitude,altitude,time,sst&sst%3E37&time=%222008-02-15T12%22";
    tName =
        tedd.makeNewFileForDapQuery(
            language, null, null, query, dir, tedd.className() + "test17", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "latitude,longitude,altitude,time,sst\n"
            + "degrees_north,degrees_east,m,UTC,degree_C\n"
            + "-15.175,125.05,0.0,2008-02-15T12:00:00Z,37.17\n"
            + "-14.95,124.0,0.0,2008-02-15T12:00:00Z,37.875\n"
            + "-14.475,136.825,0.0,2008-02-15T12:00:00Z,38.79\n"
            + "-14.45,136.8,0.0,2008-02-15T12:00:00Z,39.055\n"
            + "-14.35,123.325,0.0,2008-02-15T12:00:00Z,37.33\n"
            + "-14.325,123.325,0.0,2008-02-15T12:00:00Z,37.33\n"
            + "-14.3,123.375,0.0,2008-02-15T12:00:00Z,37.73\n"
            + "-8.55,164.425,0.0,2008-02-15T12:00:00Z,37.325\n"
            + "-7.1,131.075,0.0,2008-02-15T12:00:00Z,39.805\n"
            + "-7.1,131.1,0.0,2008-02-15T12:00:00Z,40.775\n"
            + "-7.1,131.125,0.0,2008-02-15T12:00:00Z,40.775\n"
            + "-7.075,131.025,0.0,2008-02-15T12:00:00Z,37.16\n"
            + "-7.075,131.05,0.0,2008-02-15T12:00:00Z,37.935\n"
            + "-0.425,314.875,0.0,2008-02-15T12:00:00Z,37.575\n"
            + "-0.425,314.90000000000003,0.0,2008-02-15T12:00:00Z,37.575\n"
            + "0.175,137.375,0.0,2008-02-15T12:00:00Z,37.18\n"
            + "0.225,137.375,0.0,2008-02-15T12:00:00Z,37.665\n"
            + "5.35,129.75,0.0,2008-02-15T12:00:00Z,38.46\n"
            + "5.35,129.775,0.0,2008-02-15T12:00:00Z,37.54\n";

    // av query with dv and av constraints
    query = // "latitude,longitude,altitude,time&latitude>0&sst>37&time=\"2008-02-15T12\"");
        "latitude,longitude,altitude,time&latitude%3E0&sst%3E37&time=%222008-02-15T12%22";
    tName =
        tedd.makeNewFileForDapQuery(
            language, null, null, query, dir, tedd.className() + "test18", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "latitude,longitude,altitude,time\n"
            + "degrees_north,degrees_east,m,UTC\n"
            + "0.175,137.375,0.0,2008-02-15T12:00:00Z\n"
            + "0.225,137.375,0.0,2008-02-15T12:00:00Z\n"
            + "5.35,129.75,0.0,2008-02-15T12:00:00Z\n"
            + "5.35,129.775,0.0,2008-02-15T12:00:00Z\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // dv query with dv and av constraint
    query = // "sst&sst>40&time=\"2008-02-15T12\"");
        "sst&sst%3E40&time=%222008-02-15T12%22";
    tName =
        tedd.makeNewFileForDapQuery(
            language, null, null, query, dir, tedd.className() + "test19", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "sst\n" + "degree_C\n" + "40.775\n" + "40.775\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    /* */
    // query error
    results = "";
    try {
      query = // "latitude,longitude,altitude,time&latitude>0&sst>37");
          "latitude,longitude,altitude,time&latitude%3E0&sst%3E37";
      tName =
          tedd.makeNewFileForDapQuery(
              language, null, null, query, dir, tedd.className() + "maxAxis0Error", ".csv");
      results = "shouldn't get here";
    } catch (Throwable t) {
      results = "Caught: " + t.toString();
    }
    expected =
        "Caught: com.cohort.util.SimpleException: Your query produced too much data.  Try to request less data. [memory]: "
            + "Your request for data from 2 axis[0] (time) values exceeds the maximum allowed for this dataset (1). "
            + "Please add tighter constraints on the time variable.";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // debugMode = oDebugMode;
  }

  /** */
  @org.junit.jupiter.api.Test
  @TagLocalERDDAP
  void testInErddap() throws Throwable {
    // String2.log("\nEDDTableFromEDDGrid.testInErddap()");
    // testVerboseOn();
    // boolean oDebugMode = debugMode;
    // debugMode = false; //normally false. Set it to true if need help.
    String results, query, expected, expected2;
    String id = "erdMBsstdmday_AsATable";
    // this needs to test the dataset in ERDDAP, so child is local
    String baseQuery = "http://localhost:8080/cwexperimental/tabledap/erdMBsstdmday_AsATable";
    String dir = EDStatic.fullTestCacheDirectory;

    // das
    results = SSR.getUrlResponseStringNewline(baseQuery + ".das");
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range 120.0, 320.0;\n"
            + "    String axis \"X\";\n"
            + "    String coordsys \"geographic\";\n"
            + "    Int32 fraction_digits 3;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String point_spacing \"even\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float64 actual_range -45.0, 65.0;\n"
            + "    String axis \"Y\";\n"
            + "    String coordsys \"geographic\";\n"
            + "    Int32 fraction_digits 3;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String point_spacing \"even\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  altitude {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    String _CoordinateZisPositive \"up\";\n"
            + "    Float64 actual_range 0.0, 0.0;\n"
            + "    String axis \"Z\";\n"
            + "    Int32 fraction_digits 0;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Altitude\";\n"
            + "    String positive \"up\";\n"
            + "    String standard_name \"altitude\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range 1.2030768e+9, 1.2056688e+9;\n"
            + "    String axis \"T\";\n"
            + "    Int32 fraction_digits 0;\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Centered Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  sst {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float64 colorBarMaximum 32.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String coordsys \"geographic\";\n"
            + "    Int32 fraction_digits 1;\n"
            + "    String ioos_category \"Temperature\";\n"
            + "    String long_name \"Sea Surface Temperature\";\n"
            + "    Float32 missing_value -9999999.0;\n"
            + "    String standard_name \"sea_surface_temperature\";\n"
            + "    String units \"degree_C\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String acknowledgement \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n"
            + "    String cdm_data_type \"Point\";\n"
            + "    String composite \"true\";\n"
            + "    String contributor_name \"NASA GSFC (G. Feldman)\";\n"
            + "    String contributor_role \"Source of level 2 data.\";\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "    String creator_email \"erd.data@noaa.gov\";\n"
            + "    String creator_name \"NOAA NMFS SWFSC ERD\";\n"
            + "    String creator_type \"institution\";\n"
            + "    String creator_url \"https://www.pfeg.noaa.gov\";\n"
            + "    String date_created \"2008-04-04\";\n"
            + "    String date_issued \"2008-04-04\";\n"
            + "    Float64 Easternmost_Easting 320.0;\n"
            + "    String featureType \"Point\";\n"
            + "    Float64 geospatial_lat_max 65.0;\n"
            + "    Float64 geospatial_lat_min -45.0;\n"
            + "    Float64 geospatial_lat_resolution 0.025;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max 320.0;\n"
            + "    Float64 geospatial_lon_min 120.0;\n"
            + "    Float64 geospatial_lon_resolution 0.025;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    Float64 geospatial_vertical_max 0.0;\n"
            + "    Float64 geospatial_vertical_min 0.0;\n"
            + "    String geospatial_vertical_positive \"up\";\n"
            + "    String geospatial_vertical_units \"m\";\n"
            + "    String history \"NASA GSFC (G. Feldman)";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    expected2 =
        "String infoUrl \"https://coastwatch.pfeg.noaa.gov/infog/MB_sstd_las.html\";\n"
            + "    String institution \"NOAA NMFS SWFSC ERD\";\n"
            + "    String keywords \"altitude, aqua, coast, coastwatch, data, day, daytime, degrees, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, imaging, MBsstd, moderate, modis, national, noaa, node, npp, ocean, oceans, orbiting, pacific, partnership, polar, polar-orbiting, resolution, sea, sea_surface_temperature, spectroradiometer, sst, surface, temperature, time, wcn, west\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    Int32 maxAxis0 1;\n"
            + "    String naming_authority \"gov.noaa.pfeg.coastwatch\";\n"
            + "    Float64 Northernmost_Northing 65.0;\n"
            + "    String origin \"NASA GSFC (G. Feldman)\";\n"
            + "    String processing_level \"3\";\n"
            + "    String project \"CoastWatch (https://coastwatch.noaa.gov/)\";\n"
            + "    String projection \"geographic\";\n"
            + "    String projection_type \"mapped\";\n"
            + "    String publisher_email \"erd.data@noaa.gov\";\n"
            + "    String publisher_name \"NOAA NMFS SWFSC ERD\";\n"
            + "    String publisher_type \"institution\";\n"
            + "    String publisher_url \"https://www.pfeg.noaa.gov\";\n"
            + "    String references \"Aqua/MODIS information: https://oceancolor.gsfc.nasa.gov/ . MODIS information: https://coastwatch.noaa.gov/cw_html/OceanColor_NRT_MODIS_Aqua.html .\";\n"
            + "    String satellite \"Aqua\";\n"
            + "    String sensor \"MODIS\";\n"
            + "    String source \"satellite observation: Aqua, MODIS\";\n"
            + "    String sourceUrl \"(local files)\";\n"
            + "    Float64 Southernmost_Northing -45.0;\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String summary \"NOTE: This dataset is the tabular version of a gridded dataset which is also\n"
            + "available in this ERDDAP (see datasetID=erdMBsstdmday). Most people, most\n"
            + "of the time, will prefer to use the original gridded version of this dataset.\n"
            + "This dataset presents all of the axisVariables and dataVariables from the\n"
            + "gridded dataset as columns in a huge database-like table of data.\n"
            + "You can query this dataset as you would any other tabular dataset in ERDDAP;\n"
            + "however, if you query any of the original dataVariables, you must constrain\n"
            + "what was axisVariable[0] (usually \\\"time\\\") so that data for no more than 10\n"
            + "axisVariable[0] values are searched.\n"
            + "\n"
            + "---\n"
            + "NOAA CoastWatch provides SST data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.  Currently, only daytime imagery is supported.\";\n"
            + "    String testOutOfDate \"now-60days\";\n"
            + "    String time_coverage_end \"2008-03-16T12:00:00Z\";\n"
            + "    String time_coverage_start \"2008-02-15T12:00:00Z\";\n"
            + "    String title \"SST, Aqua MODIS, NPP, 0.025 degrees, Pacific Ocean, Daytime, 2006-present (Monthly Composite), (As A Table)\";\n"
            + "    Float64 Westernmost_Easting 120.0;\n"
            + "  }\n"
            + "}\n";
    int tPo = results.indexOf("String infoUrl ");
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(results.substring(tPo), expected2, "results=\n" + results);

    // das
    results = SSR.getUrlResponseStringNewline(baseQuery + ".dds");
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    Float64 longitude;\n"
            + "    Float64 latitude;\n"
            + "    Float64 altitude;\n"
            + "    Float64 time;\n"
            + "    Float32 sst;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query 1 axis
    results =
        SSR.getUrlResponseStringNewline(
            baseQuery
                + ".csv?"
                +
                // "latitude&latitude>20&latitude<=20.1" +
                "latitude&latitude%3E20&latitude%3C=20.1"
                + "&longitude=200"); // longitude constraint is ignored (since it's valid)
    expected = "latitude\n" + "degrees_north\n" + "20.025\n" + "20.05\n" + "20.075\n" + "20.1\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query 1 axis =min lon is 120 - 320
    results = SSR.getUrlResponseStringNewline(baseQuery + ".csv?" + "longitude&longitude=120");
    expected = "longitude\n" + "degrees_east\n" + "120.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query 1 axis <=min
    results =
        SSR.getUrlResponseStringNewline(
            baseQuery
                + ".csv?"
                +
                // "longitude&longitude<=120");
                "longitude&longitude%3C=120");
    expected = "longitude\n" + "degrees_east\n" + "120.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    String2.log("\nLots of intentional errors are coming...");
    Math2.sleep(1000);

    // query 1 axis <min fails immediately
    try {
      results = "";
      results =
          SSR.getUrlResponseStringNewline(
              baseQuery
                  + ".csv?"
                  +
                  // "longitude&longitude<120");
                  "longitude&longitude%3C120");
      results = "shouldn't get here";
    } catch (Throwable t) {
      results = "Caught: " + t.toString(); // MustBe.throwableToString(t);
    }
    Test.ensureEqual(
        results,
        // wanted: "com.cohort.util.SimpleException: Your query produced no matching
        // results. (nRows = 0)",
        "Caught: java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: "
            + "http://localhost:8080/cwexperimental/tabledap/erdMBsstdmday_AsATable.csv?longitude&longitude%3C120\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: Your query produced no matching results. (nRows = 0)\";\n"
            + "})",
        "results=\n" + results);

    // query 1 axis <min fails immediately and differently
    try {
      results = "";
      results =
          SSR.getUrlResponseStringNewline(
              baseQuery
                  + ".csv?"
                  +
                  // "longitude&longitude<-119.99");
                  "longitude&longitude%3C-119.99");
      results = "shouldn't get here";
    } catch (Throwable t) {
      results = "Caught: " + t.toString();
    }
    Test.ensureEqual(
        results,
        // "com.cohort.util.SimpleException: Your query produced no matching results. "
        // +
        // "(longitude<-119.99 is outside of the variable's actual_range: 120.0 to
        // 320.0)",
        "Caught: java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: "
            + "http://localhost:8080/cwexperimental/tabledap/erdMBsstdmday_AsATable.csv?longitude&longitude%3C-119.99\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: Your query produced no matching results. (longitude<-119.99 is outside of the variable's actual_range: 120.0 to 320.0)\";\n"
            + "})",
        "results=\n" + results);

    // query 1 axis =max
    results = SSR.getUrlResponseStringNewline(baseQuery + ".csv?" + "longitude&longitude=320");
    expected = "longitude\n" + "degrees_east\n" + "320.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query 1 axis >=max
    results =
        SSR.getUrlResponseStringNewline(
            baseQuery
                + ".csv?"
                +
                // "longitude&longitude>=320");
                "longitude&longitude%3E=320");
    expected = "longitude\n" + "degrees_east\n" + "320.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query 1 axis >max fails but not immediately
    try {
      results = "";
      results =
          SSR.getUrlResponseStringNewline(
              baseQuery
                  + ".csv?"
                  +
                  // "longitude&longitude>320");
                  "longitude&longitude%3E320");
      results = "shouldn't get here";
    } catch (Throwable t) {
      results = "Caught: " + t.toString();
    }
    Test.ensureEqual(
        results,
        // "Caught: com.cohort.util.SimpleException: Your query produced no matching
        // results. (nRows = 0)",
        "Caught: java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: "
            + "http://localhost:8080/cwexperimental/tabledap/erdMBsstdmday_AsATable.csv?longitude&longitude%3E320\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: Your query produced no matching results. (nRows = 0)\";\n"
            + "})",
        "results=\n" + results);

    // query 1 axis >max fails immediately
    try {
      results = "";
      results =
          SSR.getUrlResponseStringNewline(
              baseQuery
                  + ".csv?"
                  +
                  // "longitude&longitude>320.1");
                  "longitude&longitude%3E320.1");
      results = "shouldn't get here";
    } catch (Throwable t) {
      results = "Caught: " + t.toString();
    }
    Test.ensureEqual(
        results,
        // "Caught: com.cohort.util.SimpleException: Your query produced no matching
        // results. " +
        // "(longitude>320.1 is outside of the variable's actual_range: 120.0 to
        // 320.0)",
        "Caught: java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: "
            + "http://localhost:8080/cwexperimental/tabledap/erdMBsstdmday_AsATable.csv?longitude&longitude%3E320.1\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: Your query produced no matching results. (longitude>320.1 is outside of the variable's actual_range: 120.0 to 320.0)\";\n"
            + "})",
        "results=\n" + results);

    // query time axis =min
    String timeMin = "2008-02-15T12:00:00Z";
    results = SSR.getUrlResponseStringNewline(baseQuery + ".csv?" + "time&time=" + timeMin);
    expected = "time\n" + "UTC\n" + "2008-02-15T12:00:00Z\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query time axis <=min
    results =
        SSR.getUrlResponseStringNewline(
            baseQuery
                + ".csv?"
                +
                // "time&time<=" + timeMin);
                "time&time%3C="
                + timeMin);
    expected = "time\n" + "UTC\n" + "2008-02-15T12:00:00Z\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query time axis <min fails but not immediately (< is tested as <=)
    try {
      results = "";
      results =
          SSR.getUrlResponseStringNewline(
              baseQuery
                  + ".csv?"
                  +
                  // "time&time<2008-02-15T12:00:00Z");
                  "time&time%3C2008-02-15T12:00:00Z");
      results = "shouldn't get here";
    } catch (Throwable t) {
      results = "Caught: " + t.toString();
    }
    Test.ensureEqual(
        results,
        // "Caught: com.cohort.util.SimpleException: Your query produced no matching
        // results. (nRows = 0)",
        "Caught: java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: "
            + "http://localhost:8080/cwexperimental/tabledap/erdMBsstdmday_AsATable.csv?time&time%3C2008-02-15T12:00:00Z\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: Your query produced no matching results. (nRows = 0)\";\n"
            + "})",
        "results=\n" + results);

    // query time axis <=min-1day fails immediately
    try {
      results = "";
      results =
          SSR.getUrlResponseStringNewline(
              baseQuery
                  + ".csv?"
                  +
                  // "time&time<=2008-02-14T12");
                  "time&time%3C=2008-02-14T12");
      results = "shouldn't get here";
    } catch (Throwable t) {
      results = "Caught: " + t.toString();
    }
    Test.ensureEqual(
        results,
        // "Caught: com.cohort.util.SimpleException: Your query produced no matching
        // results. " +
        // "(time<=2008-02-14T12:00:00Z is outside of the variable's actual_range:
        // 2008-02-15T12:00:00Z to 2008-03-16T12:00:00Z)",
        "Caught: java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: "
            + "http://localhost:8080/cwexperimental/tabledap/erdMBsstdmday_AsATable.csv?time&time%3C=2008-02-14T12\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: Your query produced no matching results. (time<=2008-02-14T12:00:00Z is outside of the variable's actual_range: 2008-02-15T12:00:00Z to 2008-03-16T12:00:00Z)\";\n"
            + "})",
        "results=\n" + results);

    // query time axis >max fails
    try {
      results = "";
      results =
          SSR.getUrlResponseStringNewline(
              baseQuery
                  + ".csv?"
                  +
                  // "time&time>2008-03-16T12");
                  "time&time%3E2008-03-16T12");
      results = "shouldn't get here";
    } catch (Throwable t) {
      results = "Caught: " + t.toString();
    }
    Test.ensureEqual(
        results,
        // "Caught: com.cohort.util.SimpleException: Your query produced no matching
        // results. (nRows = 0)",
        "Caught: java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: "
            + "http://localhost:8080/cwexperimental/tabledap/erdMBsstdmday_AsATable.csv?time&time%3E2008-03-16T12\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: Your query produced no matching results. (nRows = 0)\";\n"
            + "})",
        "results=\n" + results);

    // query error
    results = "";
    try {
      results = "";
      results =
          SSR.getUrlResponseStringNewline(
              baseQuery
                  + ".csv?"
                  +
                  // "latitude&latitude>20&latitude<=20.5" +
                  "latitude&latitude%3E20&latitude%3C=20.5"
                  + "&longitude=201.04"); // invalid: in middle of range but no match
      results = "shouldn't get here";
    } catch (Throwable t) {
      results = "Caught: " + t.toString();
    }
    expected = // "Caught: com.cohort.util.SimpleException: Your query produced no matching
        // results. (longitude=201.04 will never be true.)";
        "Caught: java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: "
            + "http://localhost:8080/cwexperimental/tabledap/erdMBsstdmday_AsATable.csv?latitude&latitude%3E20&latitude%3C=20.5&longitude=201.04\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: Your query produced no matching results. (longitude=201.04 will never be true.)\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query error
    results = "";
    try {
      results = "";
      results =
          SSR.getUrlResponseStringNewline(
              baseQuery
                  + ".csv?"
                  +
                  // "latitude&latitude>20&latitude<=20.5" +
                  "latitude&latitude%3E20&latitude%3C=20.5"
                  + "&longitude=119"); // invalid: out of actual_range
      results = "shouldn't get here";
    } catch (Throwable t) {
      results = "Caught: " + t.toString();
    }
    expected =
        // "Caught: com.cohort.util.SimpleException: Your query produced no matching
        // results. " +
        // "(longitude=119 is outside of the variable's actual_range: 120.0 to 320.0)";
        "Caught: java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: "
            + "http://localhost:8080/cwexperimental/tabledap/erdMBsstdmday_AsATable.csv?latitude&latitude%3E20&latitude%3C=20.5&longitude=119\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: Your query produced no matching results. (longitude=119 is outside of the variable's actual_range: 120.0 to 320.0)\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query 2 axes
    results =
        SSR.getUrlResponseStringNewline(
            baseQuery
                + ".csv?"
                +
                // "longitude,latitude&latitude>20&latitude<=20.1" +
                "longitude,latitude&latitude%3E20&latitude%3C=20.1"
                +
                // "&longitude>=215&longitude<215.1&time=\"2008-02-15T12\""); //time constraint
                // is ignored (since it's valid)
                "&longitude%3E=215&longitude%3C215.1&time=%222008-02-15T12%22"); // time constraint
    // is ignored
    // (since it's
    // valid)
    expected =
        "longitude,latitude\n"
            + "degrees_east,degrees_north\n"
            + "215.0,20.025\n"
            + "215.0,20.05\n"
            + "215.0,20.075\n"
            + "215.0,20.1\n"
            + "215.025,20.025\n"
            + "215.025,20.05\n"
            + "215.025,20.075\n"
            + "215.025,20.1\n"
            + "215.05,20.025\n"
            + "215.05,20.05\n"
            + "215.05,20.075\n"
            + "215.05,20.1\n"
            + "215.07500000000002,20.025\n"
            + "215.07500000000002,20.05\n"
            + "215.07500000000002,20.075\n"
            + "215.07500000000002,20.1\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query all axes (different order)
    results =
        SSR.getUrlResponseStringNewline(
            baseQuery
                + ".csv?"
                +
                // "latitude,longitude,altitude,time&latitude>20&latitude<=20.1" +
                "latitude,longitude,altitude,time&latitude%3E20&latitude%3C=20.1"
                +
                // "&longitude>=215&longitude<215.1&time=\"2008-02-15T12\"");
                "&longitude%3E=215&longitude%3C215.1&time=%222008-02-15T12%22");
    expected =
        "latitude,longitude,altitude,time\n"
            + "degrees_north,degrees_east,m,UTC\n"
            + "20.025,215.0,0.0,2008-02-15T12:00:00Z\n"
            + "20.025,215.025,0.0,2008-02-15T12:00:00Z\n"
            + "20.025,215.05,0.0,2008-02-15T12:00:00Z\n"
            + "20.025,215.07500000000002,0.0,2008-02-15T12:00:00Z\n"
            + "20.05,215.0,0.0,2008-02-15T12:00:00Z\n"
            + "20.05,215.025,0.0,2008-02-15T12:00:00Z\n"
            + "20.05,215.05,0.0,2008-02-15T12:00:00Z\n"
            + "20.05,215.07500000000002,0.0,2008-02-15T12:00:00Z\n"
            + "20.075,215.0,0.0,2008-02-15T12:00:00Z\n"
            + "20.075,215.025,0.0,2008-02-15T12:00:00Z\n"
            + "20.075,215.05,0.0,2008-02-15T12:00:00Z\n"
            + "20.075,215.07500000000002,0.0,2008-02-15T12:00:00Z\n"
            + "20.1,215.0,0.0,2008-02-15T12:00:00Z\n"
            + "20.1,215.025,0.0,2008-02-15T12:00:00Z\n"
            + "20.1,215.05,0.0,2008-02-15T12:00:00Z\n"
            + "20.1,215.07500000000002,0.0,2008-02-15T12:00:00Z\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query error
    results = "";
    String2.log("Pre getUrlResponse");
    try {
      results = "";
      results =
          SSR.getUrlResponseStringNewline(
              baseQuery
                  + ".csv?"
                  +
                  // "latitude,longitude,altitude,time,zztop&latitude>20&latitude<=20.1" +
                  "latitude,longitude,altitude,time,zztop&latitude%3E20&latitude%3C=20.1"
                  +
                  // "&longitude>=215&longitude<215.1&time=\"2008-02-15T12\"");
                  "&longitude%3E=215&longitude%3C215.1&time=%222008-02-15T12%22");
      results = "shouldn't get here";
    } catch (Throwable t) {
      results = "Caught: " + t.toString();
    }
    expected =
        // "Caught: com.cohort.util.SimpleException: Query error: Unrecognized
        // variable=\"zztop\"";
        "Caught: java.io.IOException: HTTP status code=400 for URL: "
            + "http://localhost:8080/cwexperimental/tabledap/erdMBsstdmday_AsATable.csv?latitude,longitude,altitude,time,zztop&latitude%3E20&latitude%3C=20.1&longitude%3E=215&longitude%3C215.1&time=%222008-02-15T12%22\n"
            + "(Error {\n"
            + "    code=400;\n"
            + "    message=\"Bad Request: Query error: Unrecognized variable=\\\"zztop\\\".\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query av and dv, with time constraint
    results =
        SSR.getUrlResponseStringNewline(
            baseQuery
                + ".csv?"
                +
                // "latitude,longitude,altitude,time,sst&latitude>20&latitude<=20.1" +
                "latitude,longitude,altitude,time,sst&latitude%3E20&latitude%3C=20.1"
                +
                // "&longitude>=215&longitude<215.1&time=\"2008-02-15T12\"");
                "&longitude%3E=215&longitude%3C215.1&time=%222008-02-15T12%22");
    expected =
        "latitude,longitude,altitude,time,sst\n"
            + "degrees_north,degrees_east,m,UTC,degree_C\n"
            + "20.025,215.0,0.0,2008-02-15T12:00:00Z,22.4018\n"
            + "20.025,215.025,0.0,2008-02-15T12:00:00Z,22.6585\n"
            + "20.025,215.05,0.0,2008-02-15T12:00:00Z,22.5025\n"
            + "20.025,215.07500000000002,0.0,2008-02-15T12:00:00Z,22.542\n"
            + "20.05,215.0,0.0,2008-02-15T12:00:00Z,22.005\n"
            + "20.05,215.025,0.0,2008-02-15T12:00:00Z,22.8965\n"
            + "20.05,215.05,0.0,2008-02-15T12:00:00Z,22.555\n"
            + "20.05,215.07500000000002,0.0,2008-02-15T12:00:00Z,22.4825\n"
            + "20.075,215.0,0.0,2008-02-15T12:00:00Z,21.7341\n"
            + "20.075,215.025,0.0,2008-02-15T12:00:00Z,22.7635\n"
            + "20.075,215.05,0.0,2008-02-15T12:00:00Z,22.389\n"
            + "20.075,215.07500000000002,0.0,2008-02-15T12:00:00Z,22.4805\n"
            + "20.1,215.0,0.0,2008-02-15T12:00:00Z,22.5244\n"
            + "20.1,215.025,0.0,2008-02-15T12:00:00Z,22.1972\n"
            + "20.1,215.05,0.0,2008-02-15T12:00:00Z,22.81\n"
            + "20.1,215.07500000000002,0.0,2008-02-15T12:00:00Z,22.6944\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // av+dv query with dv and av constraints
    results =
        SSR.getUrlResponseStringNewline(
            baseQuery
                + ".csv?"
                +
                // "latitude,longitude,altitude,time,sst&sst>37&time=\"2008-02-15T12\"");
                "latitude,longitude,altitude,time,sst&sst%3E37&time=%222008-02-15T12%22");
    expected =
        "latitude,longitude,altitude,time,sst\n"
            + "degrees_north,degrees_east,m,UTC,degree_C\n"
            + "-15.175,125.05,0.0,2008-02-15T12:00:00Z,37.17\n"
            + "-14.95,124.0,0.0,2008-02-15T12:00:00Z,37.875\n"
            + "-14.475,136.825,0.0,2008-02-15T12:00:00Z,38.79\n"
            + "-14.45,136.8,0.0,2008-02-15T12:00:00Z,39.055\n"
            + "-14.35,123.325,0.0,2008-02-15T12:00:00Z,37.33\n"
            + "-14.325,123.325,0.0,2008-02-15T12:00:00Z,37.33\n"
            + "-14.3,123.375,0.0,2008-02-15T12:00:00Z,37.73\n"
            + "-8.55,164.425,0.0,2008-02-15T12:00:00Z,37.325\n"
            + "-7.1,131.075,0.0,2008-02-15T12:00:00Z,39.805\n"
            + "-7.1,131.1,0.0,2008-02-15T12:00:00Z,40.775\n"
            + "-7.1,131.125,0.0,2008-02-15T12:00:00Z,40.775\n"
            + "-7.075,131.025,0.0,2008-02-15T12:00:00Z,37.16\n"
            + "-7.075,131.05,0.0,2008-02-15T12:00:00Z,37.935\n"
            + "-0.425,314.875,0.0,2008-02-15T12:00:00Z,37.575\n"
            + "-0.425,314.90000000000003,0.0,2008-02-15T12:00:00Z,37.575\n"
            + "0.175,137.375,0.0,2008-02-15T12:00:00Z,37.18\n"
            + "0.225,137.375,0.0,2008-02-15T12:00:00Z,37.665\n"
            + "5.35,129.75,0.0,2008-02-15T12:00:00Z,38.46\n"
            + "5.35,129.775,0.0,2008-02-15T12:00:00Z,37.54\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // av query with dv and av constraints
    results =
        SSR.getUrlResponseStringNewline(
            baseQuery
                + ".csv?"
                +
                // "latitude,longitude,altitude,time&latitude>0&sst>37&time=\"2008-02-15T12\"");
                "latitude,longitude,altitude,time&latitude%3E0&sst%3E37&time=%222008-02-15T12%22");
    expected =
        "latitude,longitude,altitude,time\n"
            + "degrees_north,degrees_east,m,UTC\n"
            + "0.175,137.375,0.0,2008-02-15T12:00:00Z\n"
            + "0.225,137.375,0.0,2008-02-15T12:00:00Z\n"
            + "5.35,129.75,0.0,2008-02-15T12:00:00Z\n"
            + "5.35,129.775,0.0,2008-02-15T12:00:00Z\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // dv query with dv and av constraint
    results =
        SSR.getUrlResponseStringNewline(
            baseQuery
                + ".csv?"
                +
                // "sst&sst>40&time=\"2008-02-15T12\"");
                "sst&sst%3E40&time=%222008-02-15T12%22");
    expected = "sst\n" + "degree_C\n" + "40.775\n" + "40.775\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query error
    results = "";
    try {
      results =
          SSR.getUrlResponseStringNewline(
              baseQuery
                  + ".csv?"
                  +
                  // "latitude,longitude,altitude,time&latitude>0&sst>37");
                  "latitude,longitude,altitude,time&latitude%3E0&sst%3E37");
      results = "shouldn't get here";
    } catch (Throwable t) {
      results = "Caught: " + t.toString();
    }
    expected =
        "Caught: java.io.IOException: HTTP status code=413 for URL: "
            + "http://localhost:8080/cwexperimental/tabledap/erdMBsstdmday_AsATable.csv?latitude,longitude,altitude,time&latitude%3E0&sst%3E37\n"
            + "(Error {\n"
            + "    code=413;\n"
            + "    message=\"Payload Too Large: Your query produced too much data.  Try to request less data. [memory]: "
            + "Your request for data from 2 axis[0] (time) values exceeds the maximum allowed for this dataset (1). Please add tighter constraints on the time variable.\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // debugMode = oDebugMode;
  }

  /**
   * This tests generateDatasetsXmlFromErddapCatalog.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagLocalERDDAP
  void testGenerateDatasetsXml() throws Throwable {

    // String2.log("\n*** EDDTableFromEDDGrid.testGenerateDatasetsXml() ***\n");
    // testVerboseOn();
    int language = 0;
    String url = "http://localhost:8080/cwexperimental/";
    // others are good, different test cases
    String regex = "(erdMBsstdmday|jplMURSST41|zztop)";

    String results =
        EDDTableFromEDDGrid.generateDatasetsXml(language, url, regex, Integer.MAX_VALUE) + "\n";

    String expected =
        "<dataset type=\"EDDTableFromEDDGrid\" datasetID=\"erdMBsstdmday_AsATable\" active=\"true\">\n"
            + "    <addAttributes>\n"
            + "        <att name=\"maxAxis0\" type=\"int\">10</att>\n"
            + "        <att name=\"title\">SST, Aqua MODIS, NPP, 0.025 degrees, Pacific Ocean, Daytime, 2006-present (Monthly Composite), (As A Table)</att>\n"
            + "        <att name=\"summary\">NOTE: This dataset is the tabular version of a gridded dataset which is also\n"
            + "available in this ERDDAP (see datasetID=erdMBsstdmday). Most people, most\n"
            + "of the time, will prefer to use the original gridded version of this dataset.\n"
            + "This dataset presents all of the axisVariables and dataVariables from the\n"
            + "gridded dataset as columns in a huge database-like table of data.\n"
            + "You can query this dataset as you would any other tabular dataset in ERDDAP;\n"
            + "however, if you query any of the original dataVariables, you must constrain\n"
            + "what was axisVariable[0] (usually &quot;time&quot;) so that data for no more than 10\n"
            + "axisVariable[0] values are searched.\n"
            + "\n"
            + "---\n"
            + "NOAA CoastWatch provides SST data from NASA&#39;s Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.  Currently, only daytime imagery is supported.</att>\n"
            + "    </addAttributes>\n"
            + "    <dataset type=\"EDDGridFromErddap\" datasetID=\"erdMBsstdmday_AsATableChild\">\n"
            + "        <sourceUrl>http://127.0.0.1:8080/cwexperimental/griddap/erdMBsstdmday</sourceUrl>\n"
            + "    </dataset>\n"
            + "</dataset>\n"
            + "\n"
            + "<dataset type=\"EDDTableFromEDDGrid\" datasetID=\"jplMURSST41_AsATable\" active=\"true\">\n"
            + "    <addAttributes>\n"
            + "        <att name=\"maxAxis0\" type=\"int\">10</att>\n"
            + "        <att name=\"title\">Multi-scale Ultra-high Resolution (MUR) SST Analysis fv04.1, Global, 0.01°, 2002-present, Daily, (As A Table)</att>\n"
            + "        <att name=\"summary\">NOTE: This dataset is the tabular version of a gridded dataset which is also\n"
            + "available in this ERDDAP (see datasetID=jplMURSST41). Most people, most\n"
            + "of the time, will prefer to use the original gridded version of this dataset.\n"
            + "This dataset presents all of the axisVariables and dataVariables from the\n"
            + "gridded dataset as columns in a huge database-like table of data.\n"
            + "You can query this dataset as you would any other tabular dataset in ERDDAP;\n"
            + "however, if you query any of the original dataVariables, you must constrain\n"
            + "what was axisVariable[0] (usually &quot;time&quot;) so that data for no more than 10\n"
            + "axisVariable[0] values are searched.\n"
            + "\n"
            + "---\n"
            + "This is a merged, multi-sensor L4 Foundation Sea Surface Temperature (SST) analysis product from Jet Propulsion Laboratory (JPL). This daily, global, Multi-scale, Ultra-high Resolution (MUR) Sea Surface Temperature (SST) 1-km data set, Version 4.1, is produced at JPL under the NASA MEaSUREs program. For details, see https://podaac.jpl.nasa.gov/dataset/MUR-JPL-L4-GLOB-v4.1 . This dataset is part of the Group for High-Resolution Sea Surface Temperature (GHRSST) project. The data for the most recent 7 days is usually revised everyday.  The data for other days is sometimes revised.</att>\n"
            + "    </addAttributes>\n"
            + "    <dataset type=\"EDDGridFromErddap\" datasetID=\"jplMURSST41_AsATableChild\">\n"
            + "        <sourceUrl>http://127.0.0.1:8080/cwexperimental/griddap/jplMURSST41</sourceUrl>\n"
            + "    </dataset>\n"
            + "</dataset>\n"
            + "\n\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // GenerateDatasetsXml
    String gdxResults =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {"-verbose", "EDDTableFromEDDGrid", url, regex, ""},
                false); // doIt loop?
    Test.ensureEqual(
        gdxResults,
        results,
        "Unexpected results from GenerateDatasetsXml.doIt. results=\n" + results);
  }

  /**
   * This tests the /files/ "files" system. This requires erdMBsstdmday and erdMBsstdmday_AsATable
   * in the localhost ERDDAP.
   */
  @org.junit.jupiter.api.Test
  @TagLocalERDDAP
  void testFiles() throws Throwable {

    // String2.log("\n*** EDDTableFromEDDGrid.testFiles()\n");
    String tDir = EDStatic.fullTestCacheDirectory;
    String dapQuery, tName, start, query, results, expected;
    int po;

    try {
      // get /files/datasetID/.csv
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:8080/cwexperimental/files/erdMBsstdmday_AsATable/.csv");
      expected =
          "Name,Last modified,Size,Description\n"
              + "MB2008032_2008060_sstd.nc,1204467796000,140954640,\n"
              + "MB2008061_2008091_sstd.nc,1207339104000,140954664,\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // get /files/datasetID/
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:8080/cwexperimental/files/erdMBsstdmday_AsATable/");
      Test.ensureTrue(
          results.indexOf("MB2008032&#x5f;2008060&#x5f;sstd&#x2e;nc") > 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">140954640<") > 0, "results=\n" + results);

      // get /files/datasetID/subdir/.csv

      // download a file in root
      results =
          String2.annotatedString(
              SSR.getUrlResponseStringNewline(
                      "http://localhost:8080/cwexperimental/files/erdMBsstdmday_AsATable/MB2008032_2008060_sstd.nc")
                  .substring(0, 50));
      expected =
          "CDF[1][0][0][0][0][0][0][0][10]\n"
              + "[0][0][0][4][0][0][0][4]time[0][0][0][1][0][0][0][8]altitude[0][0][0][1][0][0][0][3]la[end]";
      Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

      // download a file in subdir

      // try to download a non-existent dataset
      try {
        results =
            SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/gibberish/");
      } catch (Exception e) {
        results = e.toString();
      }
      expected =
          "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/gibberish/\n"
              + "(Error {\n"
              + "    code=404;\n"
              + "    message=\"Not Found: Currently unknown datasetID=gibberish\";\n"
              + "})";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // try to download a non-existent directory
      try {
        results =
            SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/erdMBsstdmday_AsATable/gibberish/");
      } catch (Exception e) {
        results = e.toString();
      }
      expected =
          "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/erdMBsstdmday_AsATable/gibberish/\n"
              + "(Error {\n"
              + "    code=404;\n"
              + "    message=\"Not Found: Resource not found: directory=gibberish/\";\n"
              + "})";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // try to download a non-existent file
      try {
        results =
            SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/erdMBsstdmday_AsATable/gibberish.csv");
      } catch (Exception e) {
        results = e.toString();
      }
      expected =
          "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/erdMBsstdmday_AsATable/gibberish.csv\n"
              + "(Error {\n"
              + "    code=404;\n"
              + "    message=\"Not Found: File not found: gibberish.csv .\";\n"
              + "})";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // try to download a non-existent file in existant subdir

    } catch (Throwable t) {
      throw new RuntimeException(
          "This test requires erdMBsstdmday_AsATable in the localhost ERDDAP.\n"
              + "Unexpected error.",
          t);
    }
  }
}
