package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.erddap.util.EDStatic;
import testDataset.EDDTestDataset;

class EDDTableAggregateRowsTests {
  /**
   */
  @org.junit.jupiter.api.Test
  void testBasic() throws Throwable {
    File2.setWebInfParentDirectory();
    System.setProperty("erddapContentDirectory", System.getProperty("user.dir") + "\\content\\erddap");
    System.setProperty("doSetupValidation", String.valueOf(false));
    // String2.log("\nEDDTableAggregateRows.testBasic()");
    // testVerboseOn();
    // boolean oDebugMode = debugMode;
    // debugMode = false; //normally false. Set it to true if need help.
    String results, query, tName, expected;
    // DasDds.main(new String[]{"miniNdbc410", "-verbose"});
    String id = "miniNdbc410";
    EDDTable tedd = (EDDTable) EDDTestDataset.getminiNdbc410();
    String dir = EDStatic.fullTestCacheDirectory;
    int tPo;
    int language = 0;

    // das
    tName = tedd.makeNewFileForDapQuery(language, null, null, "", dir,
        tedd.className() + "1", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "Attributes {\n" +
        " s {\n" +
        "  station {\n" +
        "    String cf_role \"timeseries_id\";\n" +
        "    String ioos_category \"Identifier\";\n" +
        "    String long_name \"Station Name\";\n" +
        "  }\n" +
        "  prefix {\n" +
        "    Float32 actual_range 4102.0, 4103.0;\n" + // important test that it's from all children
        "    String comment \"fixed value\";\n" +
        "    String ioos_category \"Other\";\n" +
        "    String units \"m\";\n" +
        "  }\n" +
        "  longitude {\n" +
        "    String _CoordinateAxisType \"Lon\";\n" +
        "    Float32 actual_range -80.41, -75.402;\n" + // important test that it's from all children
        "    String axis \"X\";\n" +
        "    String comment \"The longitude of the station.\";\n" +
        "    String ioos_category \"Location\";\n" +
        "    String long_name \"Longitude\";\n" +
        "    String standard_name \"longitude\";\n" +
        "    String units \"degrees_east\";\n" +
        "  }\n" +
        "  latitude {\n" +
        "    String _CoordinateAxisType \"Lat\";\n" +
        "    Float32 actual_range 32.28, 35.006;\n" +
        "    String axis \"Y\";\n" +
        "    String comment \"The latitude of the station.\";\n" +
        "    String ioos_category \"Location\";\n" +
        "    String long_name \"Latitude\";\n" +
        "    String standard_name \"latitude\";\n" +
        "    String units \"degrees_north\";\n" +
        "  }\n" +
        "  time {\n" +
        "    String _CoordinateAxisType \"Time\";\n" +
        "    Float64 actual_range 1.048878e+9, 1.4220504e+9;\n" +
        "    String axis \"T\";\n" +
        "    String comment \"Time in seconds since 1970-01-01T00:00:00Z. The original times are rounded to the nearest hour.\";\n"
        +
        "    String ioos_category \"Time\";\n" +
        "    String long_name \"Time\";\n" +
        "    String standard_name \"time\";\n" +
        "    String time_origin \"01-JAN-1970 00:00:00\";\n" +
        "    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
        "  }\n" +
        "  wd {\n" +
        "    Int16 _FillValue 32767;\n" +
        "    Int16 actual_range 0, 359;\n" +
        "    Float64 colorBarMaximum 360.0;\n" +
        "    Float64 colorBarMinimum 0.0;\n" +
        "    String comment \"Wind direction (the direction the wind is coming from in degrees clockwise from true N) during the same period used for WSPD. See Wind Averaging Methods.\";\n"
        +
        "    String ioos_category \"Wind\";\n" +
        "    String long_name \"Wind Direction\";\n" +
        "    Int16 missing_value 32767;\n" +
        "    String standard_name \"wind_from_direction\";\n" +
        "    String units \"degrees_true\";\n" +
        "  }\n" +
        "  wspd {\n" +
        "    Float32 _FillValue -9999999.0;\n" +
        "    Float32 actual_range 0.0, 96.0;\n" +
        "    Float64 colorBarMaximum 15.0;\n" +
        "    Float64 colorBarMinimum 0.0;\n" +
        "    String comment \"Wind speed (m/s) averaged over an eight-minute period for buoys and a two-minute period for land stations. Reported Hourly. See Wind Averaging Methods.\";\n"
        +
        "    String ioos_category \"Wind\";\n" +
        "    String long_name \"Wind Speed\";\n" +
        "    Float32 missing_value -9999999.0;\n" +
        "    String standard_name \"wind_speed\";\n" +
        "    String units \"m s-1\";\n" +
        "  }\n" +
        "  atmp {\n" +
        "    Float32 _FillValue -9999999.0;\n" +
        "    Float32 actual_range -5.9, 35.9;\n" +
        "    Float64 colorBarMaximum 40.0;\n" +
        "    Float64 colorBarMinimum 0.0;\n" +
        "    String comment \"Air temperature (Celsius). For sensor heights on buoys, see Hull Descriptions. For sensor heights at C-MAN stations, see C-MAN Sensor Locations.\";\n"
        +
        "    String ioos_category \"Temperature\";\n" +
        "    String long_name \"Air Temperature\";\n" +
        "    Float32 missing_value -9999999.0;\n" +
        "    String standard_name \"air_temperature\";\n" +
        "    String units \"degree_C\";\n" +
        "  }\n" +
        "  wtmp {\n" +
        "    Float32 _FillValue -9999999.0;\n" +
        "    Float32 actual_range 4.3, 32.6;\n" +
        "    Float64 colorBarMaximum 32.0;\n" +
        "    Float64 colorBarMinimum 0.0;\n" +
        "    String comment \"Sea surface temperature (Celsius). For sensor depth, see Hull Description.\";\n" +
        "    String ioos_category \"Temperature\";\n" +
        "    String long_name \"SST\";\n" +
        "    Float32 missing_value -9999999.0;\n" +
        "    String standard_name \"sea_surface_temperature\";\n" +
        "    String units \"degree_C\";\n" +
        "  }\n" +
        " }\n" +
        "  NC_GLOBAL {\n" +
        "    String acknowledgement \"NOAA NDBC and NOAA CoastWatch (West Coast Node)\";\n" +
        "    String cdm_data_type \"TimeSeries\";\n" +
        "    String cdm_timeseries_variables \"station, prefix, longitude, latitude\";\n" +
        "    String contributor_name \"NOAA NDBC and NOAA CoastWatch (West Coast Node)\";\n" +
        "    String contributor_role \"Source of data.\";\n" +
        "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
        "    String creator_email \"erd.data@noaa.gov\";\n" +
        "    String creator_name \"NOAA CoastWatch, West Coast Node\";\n" +
        "    String creator_url \"https://coastwatch.pfeg.noaa.gov\";\n" +
        "    Float64 Easternmost_Easting -75.402;\n" +
        "    String featureType \"TimeSeries\";\n" +
        "    Float64 geospatial_lat_max 35.006;\n" +
        "    Float64 geospatial_lat_min 32.28;\n" +
        "    String geospatial_lat_units \"degrees_north\";\n" +
        "    Float64 geospatial_lon_max -75.402;\n" +
        "    Float64 geospatial_lon_min -80.41;\n" +
        "    String geospatial_lon_units \"degrees_east\";\n" +
        "    String geospatial_vertical_positive \"down\";\n" +
        "    String geospatial_vertical_units \"m\";\n" +
        "    String history \"NOAA NDBC\n";
    // 2016-02-24T16:48:35Z https://www.ndbc.noaa.gov/
    // 2016-02-24T16:48:35Z
    // http://localhost:8080/cwexperimental/tabledap/miniNdbc410.das";";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    expected = "String infoUrl \"https://www.ndbc.noaa.gov/\";\n" +
        "    String institution \"NOAA NDBC, CoastWatch WCN\";\n" +
        "    String keywords \"keyword1, keyword2\";\n" +
        "    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
        "    String license \"The data may be used and redistributed for free but is not intended\n" +
        "for legal use, since it may contain inaccuracies. Neither the data\n" +
        "Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
        "of their employees or contractors, makes any warranty, express or\n" +
        "implied, including warranties of merchantability and fitness for a\n" +
        "particular purpose, or assumes any legal liability for the accuracy,\n" +
        "completeness, or usefulness, of this information.\";\n" +
        "    String naming_authority \"gov.noaa.pfeg.coastwatch\";\n" +
        "    String NDBCMeasurementDescriptionUrl \"https://www.ndbc.noaa.gov/measdes.shtml\";\n" +
        "    Float64 Northernmost_Northing 35.006;\n" +
        "    String project \"NOAA NDBC and NOAA CoastWatch (West Coast Node)\";\n" +
        "    String quality \"Automated QC checks with periodic manual QC\";\n" +
        "    String source \"station observation\";\n" +
        "    String sourceUrl \"https://www.ndbc.noaa.gov/\";\n" +
        "    Float64 Southernmost_Northing 32.28;\n" +
        "    String standard_name_vocabulary \"CF-12\";\n" +
        "    String subsetVariables \"station, prefix, longitude, latitude\";\n" +
        "    String summary \"miniNdbc summary\";\n" +
        "    String time_coverage_end \"2015-01-23T22:00:00Z\";\n" +
        "    String time_coverage_resolution \"P1H\";\n" +
        "    String time_coverage_start \"2003-03-28T19:00:00Z\";\n" +
        "    String title \"NDBC Standard Meteorological Buoy Data\";\n" +
        "    Float64 Westernmost_Easting -80.41;\n" +
        "  }\n" +
        "}\n";
    tPo = results.indexOf("String infoUrl ");
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(results.substring(tPo), expected, "results=\n" + results);

    // das
    tName = tedd.makeNewFileForDapQuery(language, null, null, "", dir,
        tedd.className() + "2", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "Dataset {\n" +
        "  Sequence {\n" +
        "    String station;\n" +
        "    Float32 prefix;\n" +
        "    Float32 longitude;\n" +
        "    Float32 latitude;\n" +
        "    Float64 time;\n" +
        "    Int16 wd;\n" +
        "    Float32 wspd;\n" +
        "    Float32 atmp;\n" +
        "    Float32 wtmp;\n" +
        "  } s;\n" +
        "} s;\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query station info
    query = "prefix,station,latitude,longitude&distinct()";
    tName = tedd.makeNewFileForDapQuery(language, null, null, query, dir,
        tedd.className() + "stationInfo", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "prefix,station,latitude,longitude\n" +
        "m,,degrees_north,degrees_east\n" +
        "4102.0,41024,33.848,-78.489\n" +
        "4102.0,41025,35.006,-75.402\n" +
        "4102.0,41029,32.81,-79.63\n" +
        "4103.0,41033,32.28,-80.41\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query station info, lon<-80
    query = "prefix,station,latitude,longitude&distinct()&longitude<-80";
    tName = tedd.makeNewFileForDapQuery(language, null, null, query, dir,
        tedd.className() + "stationInfoLT", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "prefix,station,latitude,longitude\n" +
        "m,,degrees_north,degrees_east\n" +
        "4103.0,41033,32.28,-80.41\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query station info, lon>-80
    query = "prefix,station,latitude,longitude&distinct()&longitude>-80";
    tName = tedd.makeNewFileForDapQuery(language, null, null, query, dir,
        tedd.className() + "stationInfoGT", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "prefix,station,latitude,longitude\n" +
        "m,,degrees_north,degrees_east\n" +
        "4102.0,41024,33.848,-78.489\n" +
        "4102.0,41025,35.006,-75.402\n" +
        "4102.0,41029,32.81,-79.63\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query data
    query = "&time=2014-01-01";
    tName = tedd.makeNewFileForDapQuery(language, null, null, query, dir,
        tedd.className() + "data", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "station,prefix,longitude,latitude,time,wd,wspd,atmp,wtmp\n" +
        ",m,degrees_east,degrees_north,UTC,degrees_true,m s-1,degree_C,degree_C\n" +
        "41024,4102.0,-78.489,33.848,2014-01-01T00:00:00Z,320,2.0,10.3,NaN\n" +
        "41025,4102.0,-75.402,35.006,2014-01-01T00:00:00Z,305,7.0,11.8,23.3\n" +
        "41029,4102.0,-79.63,32.81,2014-01-01T00:00:00Z,340,3.0,17.4,NaN\n" +
        "41033,4103.0,-80.41,32.28,2014-01-01T00:00:00Z,350,3.0,12.2,NaN\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query data, lon<-80
    query = "&time=2014-01-01&longitude<-80";
    tName = tedd.makeNewFileForDapQuery(language, null, null, query, dir,
        tedd.className() + "dataLT", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "station,prefix,longitude,latitude,time,wd,wspd,atmp,wtmp\n" +
        ",m,degrees_east,degrees_north,UTC,degrees_true,m s-1,degree_C,degree_C\n" +
        "41033,4103.0,-80.41,32.28,2014-01-01T00:00:00Z,350,3.0,12.2,NaN\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query data, lon >-80
    query = "&time=2014-01-01&longitude>-80";
    tName = tedd.makeNewFileForDapQuery(language, null, null, query, dir,
        tedd.className() + "dataGT", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "station,prefix,longitude,latitude,time,wd,wspd,atmp,wtmp\n" +
        ",m,degrees_east,degrees_north,UTC,degrees_true,m s-1,degree_C,degree_C\n" +
        "41024,4102.0,-78.489,33.848,2014-01-01T00:00:00Z,320,2.0,10.3,NaN\n" +
        "41025,4102.0,-75.402,35.006,2014-01-01T00:00:00Z,305,7.0,11.8,23.3\n" +
        "41029,4102.0,-79.63,32.81,2014-01-01T00:00:00Z,340,3.0,17.4,NaN\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query data, prefix=4103
    query = "&time=2014-01-01&prefix=4103";
    tName = tedd.makeNewFileForDapQuery(language, null, null, query, dir,
        tedd.className() + "data4103", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "station,prefix,longitude,latitude,time,wd,wspd,atmp,wtmp\n" +
        ",m,degrees_east,degrees_north,UTC,degrees_true,m s-1,degree_C,degree_C\n" +
        "41033,4103.0,-80.41,32.28,2014-01-01T00:00:00Z,350,3.0,12.2,NaN\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query data, prefix=4102
    query = "&time=2014-01-01&prefix=4102";
    tName = tedd.makeNewFileForDapQuery(language, null, null, query, dir,
        tedd.className() + "data4102", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "station,prefix,longitude,latitude,time,wd,wspd,atmp,wtmp\n" +
        ",m,degrees_east,degrees_north,UTC,degrees_true,m s-1,degree_C,degree_C\n" +
        "41024,4102.0,-78.489,33.848,2014-01-01T00:00:00Z,320,2.0,10.3,NaN\n" +
        "41025,4102.0,-75.402,35.006,2014-01-01T00:00:00Z,305,7.0,11.8,23.3\n" +
        "41029,4102.0,-79.63,32.81,2014-01-01T00:00:00Z,340,3.0,17.4,NaN\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test metadata is from child0: query data, prefix=4103
    query = "&time=2014-01-01&prefix=4103";
    tName = tedd.makeNewFileForDapQuery(language, null, null, query, dir,
        tedd.className() + "data4103", ".nc");
    results = NcHelper.ncdump(dir + tName, "");
    expected = "short wd(row=1);\n" + // 1 row
        "      :_FillValue = 32767S; // short\n" +
        "      :actual_range = 350S, 350S; // short\n" + // up-to-date
        "      :colorBarMaximum = 360.0; // double\n" +
        "      :colorBarMinimum = 0.0; // double\n" +
        "      :comment = \"Wind direction (the direction the wind is coming from in degrees clockwise from true N) during the same period used for WSPD. See Wind Averaging Methods.\";\n"
        +
        "      :ioos_category = \"Wind\";\n" +
        "      :long_name = \"Wind Direction\";\n" +
        "      :missing_value = 32767S; // short\n" +
        "      :standard_name = \"wind_from_direction\";\n" +
        "      :units = \"degrees_true\";\n";
    tPo = results.indexOf("short wd(");
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(results.substring(tPo, tPo + expected.length()), expected, "results=\n" + results);

    // test metadata is from child0: query data, prefix=4102
    query = "&time=2014-01-01&prefix=4102";
    tName = tedd.makeNewFileForDapQuery(language, null, null, query, dir,
        tedd.className() + "data4102", ".nc");
    results = NcHelper.ncdump(dir + tName, "");
    expected = "short wd(row=3);\n" + // 3 rows
        "      :_FillValue = 32767S; // short\n" +
        "      :actual_range = 305S, 340S; // short\n" + // up-to-date
        "      :colorBarMaximum = 360.0; // double\n" +
        "      :colorBarMinimum = 0.0; // double\n" +
        "      :comment = \"Wind direction (the direction the wind is coming from in degrees clockwise from true N) during the same period used for WSPD. See Wind Averaging Methods.\";\n"
        +
        "      :ioos_category = \"Wind\";\n" +
        "      :long_name = \"Wind Direction\";\n" +
        "      :missing_value = 32767S; // short\n" +
        "      :standard_name = \"wind_from_direction\";\n" +
        "      :units = \"degrees_true\";\n";
    tPo = results.indexOf("short wd(");
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(results.substring(tPo, tPo + expected.length()), expected, "results=\n" + results);

    // query error
    results = "";
    try {
      query = "&longitude<-90"; // quick reject
      tName = tedd.makeNewFileForDapQuery(language, null, null, query, dir,
          tedd.className() + "errorLT90", ".csv");
      results = "shouldn't get here";
    } catch (Throwable t) {
      results = t.toString();
      String2.log("Expected error:\n" + MustBe.throwableToString(t));
    }
    expected = "com.cohort.util.SimpleException: Your query produced no matching results. " +
        "(longitude<-90 is outside of the variable's actual_range: -80.41 to -75.402)";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query error
    results = "";
    try {
      query = "&wtmp=23.12345"; // look through every row of data: no matching data
      tName = tedd.makeNewFileForDapQuery(language, null, null, query, dir,
          tedd.className() + "errorWtmp", ".csv");
      results = "shouldn't get here";
    } catch (Throwable t) {
      results = t.toString();
      String2.log("Expected error:\n" + MustBe.throwableToString(t));
    }
    expected = "com.cohort.util.SimpleException: Your query produced no matching results. (nRows = 0)";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // debugMode = oDebugMode;
    // String2.log("\n*** EDDTableAggregateRows.testBasic finished successfully.");
    /* */
  }

}
