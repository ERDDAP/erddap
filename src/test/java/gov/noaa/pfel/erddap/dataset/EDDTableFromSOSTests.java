package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeAll;
import tags.TagExternalOther;
import tags.TagLocalERDDAP;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

/**
 * Comments from the old test file: 'Treat all tests as slow tests. These aren't actually slow
 * tests, but they fail/change so often that it usually isn't worth the time to test. Stated the
 * other way: the problems usually aren't ERDDAP problems.'
 *
 * <p>So until these are significantly updated they are likely to be flaky.
 */
class EDDTableFromSOSTests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /**
   * This should work, but server is in flux so it often breaks. Send questions about NOS SOS to
   * Andrea.Hardy@noaa.gov.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testNosSosATemp() throws Throwable {
    // testVerboseOn();

    int language = 0;
    EDDTable eddTable = (EDDTable) EDDTestDataset.getnosSosATemp();

    String name, tName, results, expected, userDapQuery;
    // it was hard to find data. station advertises 1999+ for several composites.
    // but searching January's for each year found no data until 2006
    String2.log("\n*** EDDTableFromSOS nos AirTemperature test get one station .CSV data\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:8419317\"&time>=2006-01-01T00&time<=2006-01-01T01",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_nosSosATemp",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "longitude,latitude,station_id,altitude,time,sensor_id,air_temperature,quality_flags\n"
            + "degrees_east,degrees_north,,m,UTC,,degree_C,\n"
            + "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-3.9,0;0;0\n"
            + "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-3.9,0;0;0\n"
            + "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-3.9,0;0;0\n"
            + "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-3.9,0;0;0\n"
            + "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-3.9,0;0;0\n"
            + "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-3.9,0;0;0\n"
            + "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-3.9,0;0;0\n"
            + "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-4.0,0;0;0\n"
            + "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-4.0,0;0;0\n"
            + "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-4.1,0;0;0\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    // String2.log("\n*** EDDTableFromSOS nos AirTemperature test get one station
    // .CSV data\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null, // 1612340, NaN, 2008-10-26
            "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:1612340\"&time>=2008-10-26T00&time<2008-10-26T01",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_nosSosATemp",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "longitude,latitude,station_id,altitude,time,sensor_id,air_temperature,quality_flags\n"
            + "degrees_east,degrees_north,,m,UTC,,degree_C,\n"
            + "-157.8645,21.3033,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.9,0;0;0\n"
            + "-157.8645,21.3033,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8,0;0;0\n"
            + "-157.8645,21.3033,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.7,0;0;0\n"
            + "-157.8645,21.3033,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8,0;0;0\n"
            + "-157.8645,21.3033,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.7,0;0;0\n"
            + "-157.8645,21.3033,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8,0;0;0\n"
            + "-157.8645,21.3033,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.7,0;0;0\n"
            + "-157.8645,21.3033,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8,0;0;0\n"
            + "-157.8645,21.3033,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8,0;0;0\n"
            + "-157.8645,21.3033,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.7,0;0;0\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    // String2.log("\n*** EDDTableFromSOS nos AirTemperature .das\n");
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard
    // to check min:sec.
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_nosSosATemp",
            ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range -177.36, 166.6176;\n"
            + "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float64 actual_range -14.2767, 70.4114;\n"
            + "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  station_id {\n"
            + "    String cf_role \"timeseries_id\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Station ID\";\n"
            + "  }\n"
            + "  altitude {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    String _CoordinateZisPositive \"up\";\n"
            + "    String axis \"Z\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Altitude\";\n"
            + "    String positive \"up\";\n"
            + "    String standard_name \"altitude\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range -3.6757152e+9, NaN;\n"
            + "    String axis \"T\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  sensor_id {\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Sensor ID\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/air_temperature\";\n"
            + "  }\n"
            + "  air_temperature {\n"
            + "    Float64 colorBarMaximum 40.0;\n"
            + "    Float64 colorBarMinimum -10.0;\n"
            + "    String ioos_category \"Temperature\";\n"
            + "    String long_name \"Air Temperature\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/air_temperature\";\n"
            + "    String standard_name \"air_temperature\";\n"
            + "    String units \"degree_C\";\n"
            + "  }\n"
            + "  quality_flags {\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Quality Flags\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/air_temperature\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_data_type \"TimeSeries\";\n"
            + "    String cdm_timeseries_variables \"station_id, longitude, latitude, sensor_id\";\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "    Float64 Easternmost_Easting 166.6176;\n"
            + "    String featureType \"TimeSeries\";\n"
            + "    Float64 geospatial_lat_max 70.4114;\n"
            + "    Float64 geospatial_lat_min -14.2767;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max 166.6176;\n"
            + "    Float64 geospatial_lon_min -177.36;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    String geospatial_vertical_positive \"up\";\n"
            + "    String geospatial_vertical_units \"m\";\n"
            + "    String history \""
            + today;
    // + " https://opendap.co-ops.nos.noaa.gov/ioos-dif-sos"; //"-test"
    Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);

    expected =
        "    Float64 Southernmost_Northing -14.2767;\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String subsetVariables \"station_id, longitude, latitude\";\n"
            + "    String summary \"The NOAA NOS SOS server is part of the IOOS DIF SOS Project.  The stations in this dataset have air temperature data.  ****These services are for testing and evaluation use only****\n"
            + "\n"
            + "Because of the nature of SOS requests, requests for data MUST include constraints for the longitude, latitude, time, and/or station_id variables.\";\n"
            + "    String time_coverage_start \"1853-07-10T00:00:00Z\";\n"
            + "    String title \"NOAA NOS SOS, EXPERIMENTAL, 1853-present, Air Temperature\";\n"
            + "    Float64 Westernmost_Easting -177.36;\n"
            + "  }\n"
            + "}\n";
    int po = Math.max(0, results.indexOf(expected.substring(0, 30)));
    Test.ensureEqual(results.substring(po), expected, "RESULTS=\n" + results);
  }

  /**
   * This should work, but server is in flux so it often breaks.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testNosSosATempAllStations() throws Throwable {
    // testVerboseOn();

    int language = 0;
    EDDTable eddTable;
    String name, tName, results, expected, userDapQuery;

    try {
      eddTable = (EDDTable) EDDTestDataset.getnosSosATemp();

      // it was hard to find data. station advertises 1999+ for several composites.
      // but searching January's for each year found no data until 2006
      String2.log("\n*** EDDTableFromSOS nos AirTemperature test get all stations .CSV data\n");
      tName =
          eddTable.makeNewFileForDapQuery(
              language,
              null,
              null,
              "&time>=2008-10-26T00&time<=2008-10-26T01&orderBy(\"station_id,time\")",
              EDStatic.fullTestCacheDirectory,
              eddTable.className() + "_nosSosATempAllStations",
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      expected =
          "longitude,latitude,station_id,altitude,time,sensor_id,air_temperature,quality_flags\n"
              + "degrees_east,degrees_north,,m,UTC,,degree_C,\n"
              + "-157.8645,21.3033,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.9,0;0;0\n"
              + "-157.8645,21.3033,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8,0;0;0\n"
              + "-157.8645,21.3033,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.7,0;0;0\n"
              + "-157.8645,21.3033,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8,0;0;0\n"
              + "-157.8645,21.3033,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.7,0;0;0\n"
              + "-157.8645,21.3033,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8,0;0;0\n"
              + "-157.8645,21.3033,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.7,0;0;0\n"
              + "-157.8645,21.3033,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8,0;0;0\n"
              + "-157.8645,21.3033,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8,0;0;0\n"
              + "-157.8645,21.3033,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.7,0;0;0\n"
              + "-157.8645,21.3033,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8,0;0;0\n"
              + "-157.79,21.4331,urn:ioos:station:NOAA.NOS.CO-OPS:1612480,NaN,2008-10-26T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612480:D1,22.9,0;0;0\n"
              + "-157.79,21.4331,urn:ioos:station:NOAA.NOS.CO-OPS:1612480,NaN,2008-10-26T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612480:D1,22.8,0;0;0\n"
              + "-157.79,21.4331,urn:ioos:station:NOAA.NOS.CO-OPS:1612480,NaN,2008-10-26T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612480:D1,23.0,0;0;0\n"
              + "-157.79,21.4331,urn:ioos:station:NOAA.NOS.CO-OPS:1612480,NaN,2008-10-26T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612480:D1,22.9,0;0;0\n"
              + "-157.79,21.4331,urn:ioos:station:NOAA.NOS.CO-OPS:1612480,NaN,2008-10-26T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612480:D1,23.0,0;0;0\n";
      Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);

    } catch (Throwable t) {
      throw new RuntimeException("Unexpected error nosSosATemp. NOS SOS Server is in flux.", t);
    }
  }

  /**
   * This should work, but server is in flux so it often breaks.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testNosSosATempStationList() throws Throwable {
    // testVerboseOn();

    int language = 0;
    EDDTable eddTable;
    String name, tName, results, expected, userDapQuery;

    eddTable = (EDDTable) EDDTestDataset.getnosSosATemp();

    String2.log("\n*** EDDTableFromSOS nos AirTemperature test get all stations .CSV data\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "longitude,latitude,station_id&orderBy(\"station_id\")",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_nosSosATempStationList",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "longitude,latitude,station_id\n"
            + "degrees_east,degrees_north,\n"
            +
            // "122.6003,37.7501,urn:ioos:station:NOAA.NOS.CO-OPS:1600012\n" + //disappeared
            // 2012-08-17
            "-159.3561,21.9544,urn:ioos:station:NOAA.NOS.CO-OPS:1611400\n"
            + // gone 2014-08-12, returned 2015-02-19
            "-157.8645,21.3033,urn:ioos:station:NOAA.NOS.CO-OPS:1612340\n"
            + "-157.9639,21.3675,urn:ioos:station:NOAA.NOS.CO-OPS:1612401\n"
            + "-157.79,21.4331,urn:ioos:station:NOAA.NOS.CO-OPS:1612480\n"
            + "-156.4692,20.895,urn:ioos:station:NOAA.NOS.CO-OPS:1615680\n"
            + "-155.8294,20.0366,urn:ioos:station:NOAA.NOS.CO-OPS:1617433\n";
    // ...
    Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);
  }

  /**
   * This should work, but server is in flux so it often breaks.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testNosSosBPres() throws Throwable {
    // testVerboseOn();

    int language = 0;
    EDDTable eddTable = (EDDTable) EDDTestDataset.getnosSosBPres();
    String name, tName, results, expected, userDapQuery;

    // it was hard to find data. station advertises 1999+ for several composites.
    // but searching January's for each year found no data until 2006
    String2.log("\n*** EDDTableFromSOS nos Pressure test get one station .CSV data\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:8447386\""
                + "&time>=2006-01-01T00&time<=2006-01-01T01",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_nosSosPressure",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "longitude,latitude,station_id,altitude,time,sensor_id,air_pressure,quality_flags\n"
            + "degrees_east,degrees_north,,m,UTC,,millibars,\n"
            + "-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2006-01-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:F1,1010.8,0;0;0\n"
            + //
            "-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2006-01-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:F1,1010.7,0;0;0\n"
            + //
            "-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2006-01-01T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:F1,1010.8,0;0;0\n"
            + //
            "-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2006-01-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:F1,1010.8,0;0;0\n"
            + //
            "-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2006-01-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:F1,1010.8,0;0;0\n"
            + //
            "-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2006-01-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:F1,1010.7,0;0;0\n"
            + //
            "-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2006-01-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:F1,1010.5,0;0;0\n"
            + //
            "-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2006-01-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:F1,1010.4,0;0;0\n"
            + //
            "-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2006-01-01T00:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:F1,1010.3,0;0;0\n"
            + //
            "-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2006-01-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:F1,1010.2,0;0;0\n"
            + //
            "-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2006-01-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:F1,1010.1,0;0;0\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    String2.log("\n*** EDDTableFromSOS nos Pressure test get one station .CSV data\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:9491094\"&time>=2008-09-01T00&time<=2008-09-01T01",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_nosSosPressure",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "longitude,latitude,station_id,altitude,time,sensor_id,air_pressure,quality_flags\n"
            + "degrees_east,degrees_north,,m,UTC,,millibars,\n"
            + "-164.0644,67.5758,urn:ioos:station:NOAA.NOS.CO-OPS:9491094,NaN,2008-09-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9491094:F1,1011.3,0;0;0\n"
            + "-164.0644,67.5758,urn:ioos:station:NOAA.NOS.CO-OPS:9491094,NaN,2008-09-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9491094:F1,1011.2,0;0;0\n"
            + "-164.0644,67.5758,urn:ioos:station:NOAA.NOS.CO-OPS:9491094,NaN,2008-09-01T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9491094:F1,1011.2,0;0;0\n"
            + "-164.0644,67.5758,urn:ioos:station:NOAA.NOS.CO-OPS:9491094,NaN,2008-09-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9491094:F1,1011.1,0;0;0\n"
            + "-164.0644,67.5758,urn:ioos:station:NOAA.NOS.CO-OPS:9491094,NaN,2008-09-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9491094:F1,1011.1,0;0;0\n"
            + "-164.0644,67.5758,urn:ioos:station:NOAA.NOS.CO-OPS:9491094,NaN,2008-09-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9491094:F1,1011.1,0;0;0\n"
            + "-164.0644,67.5758,urn:ioos:station:NOAA.NOS.CO-OPS:9491094,NaN,2008-09-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9491094:F1,1011.0,0;0;0\n"
            + "-164.0644,67.5758,urn:ioos:station:NOAA.NOS.CO-OPS:9491094,NaN,2008-09-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9491094:F1,1010.9,0;0;0\n"
            + "-164.0644,67.5758,urn:ioos:station:NOAA.NOS.CO-OPS:9491094,NaN,2008-09-01T00:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9491094:F1,1010.9,0;0;0\n"
            + "-164.0644,67.5758,urn:ioos:station:NOAA.NOS.CO-OPS:9491094,NaN,2008-09-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9491094:F1,1010.9,0;0;0\n"
            + "-164.0644,67.5758,urn:ioos:station:NOAA.NOS.CO-OPS:9491094,NaN,2008-09-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9491094:F1,1010.8,0;0;0\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    String2.log("\n*** EDDTableFromSOS nos Pressure .das\n");
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard
    // to check min:sec.
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_nosSosPressure",
            ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range MIN, MAX;\n"
            + "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float64 actual_range MIN, MAX;\n"
            + "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  station_id {\n"
            + "    String cf_role \"timeseries_id\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Station ID\";\n"
            + "  }\n"
            + "  altitude {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    String _CoordinateZisPositive \"up\";\n"
            + "    String axis \"Z\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Altitude\";\n"
            + "    String positive \"up\";\n"
            + "    String standard_name \"altitude\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range MIN, MAX;\n"
            + "    String axis \"T\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  sensor_id {\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Sensor ID\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/air_pressure\";\n"
            + "  }\n"
            + "  air_pressure {\n"
            + "    Float64 colorBarMaximum 1050.0;\n"
            + "    Float64 colorBarMinimum 950.0;\n"
            + "    String ioos_category \"Pressure\";\n"
            + "    String long_name \"Barometric Pressure\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/air_pressure\";\n"
            + "    String standard_name \"air_pressure\";\n"
            + "    String units \"millibars\";\n"
            + "  }\n"
            + "  quality_flags {\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Quality Flags\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/air_pressure\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_data_type \"TimeSeries\";\n"
            + "    String cdm_timeseries_variables \"station_id, longitude, latitude, sensor_id\";\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "    Float64 Easternmost_Easting 167.7361;\n"
            + "    String featureType \"TimeSeries\";\n"
            + "    Float64 geospatial_lat_max 70.4114;\n"
            + "    Float64 geospatial_lat_min MIN;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max 167.7361;\n"
            + "    Float64 geospatial_lon_min -177.36;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    String geospatial_vertical_positive \"up\";\n"
            + "    String geospatial_vertical_units \"m\";\n"
            + "    String history \""
            + today;
    // + " https://opendap.co-ops.nos.noaa.gov/ioos-dif-sos"; //-test
    results =
        results.replaceAll(
            "Float64 actual_range -?[0-9]+.[0-9]+, -?[0-9]+.[0-9]+;",
            "Float64 actual_range MIN, MAX;");
    results =
        results.replaceAll(
            "Float64 actual_range -?[0-9]+.[0-9]+e?.[0-9]?, NaN;",
            "Float64 actual_range MIN, MAX;");
    results =
        results.replaceAll(
            "Float64 geospatial_lat_min -?[0-9]+.[0-9]+;", "Float64 geospatial_lat_min MIN;");
    Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);
  }

  /**
   * This should work, but server is in flux so it often breaks.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testNosSosCond() throws Throwable {
    // testVerboseOn();

    int language = 0;
    EDDTable eddTable = (EDDTable) EDDTestDataset.getnosSosCond();
    String name, tName, results, expected, userDapQuery;

    // it was hard to find data. station advertises 1999+ for several composites.
    // but searching January's for each year found no data until 2006
    // 2012-03-16 I used station=...8419317 for years, now station=...8419317 not
    // found.

    // Use this to find a valid station and time range
    // testFindValidStation(eddTable, testNosStations,
    // "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:",
    // "\"&time>=2013-09-01T00:03&time<=2013-09-01T01");

    String2.log("\n*** EDDTableFromSOS nos Conductivity test get one station .CSV data\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:8452660\"&time>=2013-09-01T00&time<=2013-09-01T01",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_nosSosCond",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "longitude,latitude,station_id,altitude,time,sensor_id,conductivity,quality_flags\n"
            + "degrees_east,degrees_north,,m,UTC,,mS cm-1,\n"
            + "-71.3261,41.5043,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,42.6,0;0;0\n"
            + "-71.3261,41.5043,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,42.5,0;0;0\n"
            + "-71.3261,41.5043,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,42.6,0;0;0\n"
            + "-71.3261,41.5043,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,42.6,0;0;0\n"
            + "-71.3261,41.5043,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,42.6,0;0;0\n"
            + "-71.3261,41.5043,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,42.6,0;0;0\n"
            + "-71.3261,41.5043,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,42.6,0;0;0\n"
            + "-71.3261,41.5043,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,42.6,0;0;0\n"
            + "-71.3261,41.5043,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,42.6,0;0;0\n"
            + "-71.3261,41.5043,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,42.7,0;0;0\n"
            + "-71.3261,41.5043,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,42.6,0;0;0\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    String2.log("\n*** EDDTableFromSOS nos Conductivity .das\n");
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard
    // to check min:sec.
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_nosSosCond",
            ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range -146.362, -70.741;\n"
            + // changes sometimes
            "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float64 actual_range 27.8461, 61.125;\n"
            + // changes sometimes
            "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  station_id {\n"
            + "    String cf_role \"timeseries_id\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Station ID\";\n"
            + "  }\n"
            + "  altitude {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    String _CoordinateZisPositive \"up\";\n"
            + "    String axis \"Z\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Altitude\";\n"
            + "    String positive \"up\";\n"
            + "    String standard_name \"altitude\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range -1.608336e+9, NaN;\n"
            + "    String axis \"T\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  sensor_id {\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Sensor ID\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/sea_water_electrical_conductivity\";\n"
            + "  }\n"
            + "  conductivity {\n"
            + "    String ioos_category \"Salinity\";\n"
            + "    String long_name \"Electrical Conductivity\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/sea_water_electrical_conductivity\";\n"
            + "    String standard_name \"sea_water_electrical_conductivity\";\n"
            + "    String units \"mS cm-1\";\n"
            + "  }\n"
            + "  quality_flags {\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Quality Flags\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/sea_water_electrical_conductivity\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_data_type \"TimeSeries\";\n"
            + "    String cdm_timeseries_variables \"station_id, longitude, latitude, sensor_id\";\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "    Float64 Easternmost_Easting -70.741;\n"
            + // changes sometimes
            "    String featureType \"TimeSeries\";\n"
            + "    Float64 geospatial_lat_max 61.125;\n"
            + // changes sometimes
            "    Float64 geospatial_lat_min 27.8461;\n"
            + // 2013-05-11 was 29.48 then 29.6817, then...
            "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max -70.741;\n"
            + // changes sometimes
            "    Float64 geospatial_lon_min -146.362;\n"
            + // changes sometimes
            "    String geospatial_lon_units \"degrees_east\";\n"
            + "    String geospatial_vertical_positive \"up\";\n"
            + "    String geospatial_vertical_units \"m\";\n"
            + "    String history \""
            + today;
    // + " https://opendap.co-ops.nos.noaa.gov/ioos-dif-sos"; //-test
    Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);
  }

  /** Bob uses this to find a valid station and time period for unit tests. */
  static String testNosStations[] = {
    "g06010", "gl0101", "gl0201", "gl0301", "hb0101", "hb0201", "hb0301", "hb0401", "lc0101",
    "lc0201", "lc0301", "lm0101", "mb0101", "mb0301", "mb0401", "n05010", "nb0201", "nb0301",
    "nl0101", "ps0201", "ps0301", "8447386", "8452660", "8454049", "8537121", "8574680", "8635750",
    "8637689", "8638610", "8638863", "8639348", "8737048", "8770613", "8771013", "9454050"
  };

  private static void testFindValidStation(
      EDDTable eddTable, String stations[], String preQuery, String postQuery) throws Exception {
    // String2.log("\n*** EDDTableFromSOS.testFindValidStation");

    int language = 0;
    int i = -1;
    while (++i < stations.length) {
      try {
        String tName =
            eddTable.makeNewFileForDapQuery(
                language,
                null,
                null,
                preQuery + stations[i] + postQuery,
                EDStatic.fullTestCacheDirectory,
                eddTable.className() + "_testFindValidStation",
                ".csv");
        String2.pressEnterToContinue("SUCCESS with station=" + stations[i]);
        break;
      } catch (Throwable t) {
        String2.log(MustBe.throwableToString(t));
      }
    }
    if (i >= stations.length) String2.pressEnterToContinue("FAILED to find a valid station.");
  }

  /**
   * This should work, but server is in flux so it often breaks.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testNosSosCurrents() throws Throwable {
    // testVerboseOn();

    int language = 0;
    EDDTable eddTable = (EDDTable) EDDTestDataset.getnosSosCurrents();
    String name, tName = "", results, expected, userDapQuery;

    // worked until Feb 2013: (then: no matching station)
    // That means a request for historical data stopped working!
    // "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:g07010\"" +
    // "&time>=2012-02-01T00:03&time<=2012-02-01T00:03", //It was hard to find a
    // request that had data

    // Use this to find a valid station and time range
    testFindValidStation(
        eddTable,
        testNosStations,
        "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:",
        "\"&time>=2013-09-01T00:03&time<=2013-09-01T00:03");

    String2.log("\n*** EDDTableFromSOS.testNosSosCurrents test get one station .CSV data\n");

    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:lc0301\""
                + "&time>=2013-09-01T00:03&time<=2013-09-01T00:03", // It was hard to find a request
            // that had data
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_nosSosCurrents",
            ".csv");

    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "longitude,latitude,station_id,altitude,time,sensor_id,sensor_depth,direction_of_sea_water_velocity,sea_water_speed,platform_orientation,platform_pitch_angle,platform_roll_angle,sea_water_temperature,orientation,sampling_rate,reporting_interval,processing_level,bin_size,first_bin_center,number_of_bins,bin\n"
            + "degrees_east,degrees_north,,m,UTC,,m,degrees_true,cm s-1,degrees_true,degrees,degrees,degree_C,,Hz,s,,m,m,,count\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-5.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,283.0,7.6,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,1\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-9.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,288.0,13.0,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,2\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-13.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,301.0,10.0,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,3\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-17.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,283.0,12.4,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,4\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-21.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,291.0,13.9,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,5\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-25.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,284.0,14.8,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,6\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-29.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,293.0,16.2,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,7\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-33.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,276.0,11.5,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,8\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-37.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,282.0,9.0,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,9\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-41.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,285.0,7.6,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,10\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-45.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,293.0,10.4,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,11\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-49.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,299.0,10.8,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,12\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-53.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,296.0,12.5,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,13\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-57.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,287.0,11.6,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,14\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-61.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,298.0,8.7,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,15\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-65.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,296.0,14.9,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,16\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-69.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,302.0,8.4,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,17\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-73.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,289.0,14.3,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,18\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-77.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,290.0,11.5,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,19\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-81.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,293.0,9.6,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,20\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-85.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,300.0,8.6,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,21\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-89.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,298.0,9.4,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,22\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-93.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,300.0,10.4,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,23\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-97.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,292.0,11.1,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,24\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-101.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,282.0,10.4,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,25\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-105.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,282.0,9.5,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,26\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-109.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,290.0,10.5,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,27\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-113.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,289.0,8.5,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,28\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-117.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,289.0,6.4,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,29\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-121.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,297.0,8.4,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,30\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-125.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,295.0,10.8,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,31\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-129.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,282.0,8.9,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,32\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-133.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,290.0,7.4,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,33\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-137.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,288.0,11.8,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,34\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-141.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,302.0,6.7,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,35\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-145.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,336.0,3.6,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,36\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-149.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,290.0,5.7,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,37\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-153.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,311.0,3.7,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,38\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-157.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,171.0,1.7,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,39\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-161.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,133.0,1.7,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,40\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-165.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,269.0,2.8,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,41\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-169.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,64.0,3.1,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,42\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-173.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,277.0,1.2,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,43\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-177.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,310.0,1.5,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,44\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-181.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,313.0,0.6,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,45\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-185.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,102.0,1.8,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,46\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-189.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,302.0,3.7,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,47\n"
            + "-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-193.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,80.0,3.9,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,48\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    // String2.log("\n*** EDDTableFromSOS.testNosSosCurrents .das\n");
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard
    // to check min:sec.
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_nosSosCurrents",
            ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range -167.9007, -66.9956;\n"
            + "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float64 actual_range 19.6351, 61.2782;\n"
            + "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  station_id {\n"
            + "    String cf_role \"timeseries_id\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Station ID\";\n"
            + "  }\n"
            + "  altitude {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    String _CoordinateZisPositive \"up\";\n"
            + "    String axis \"Z\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Altitude\";\n"
            + "    String positive \"up\";\n"
            + "    String standard_name \"altitude\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range 8.63001e+8, NaN;\n"
            + "    String axis \"T\";\n"
            + "    String cf_role \"profile_id\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  sensor_id {\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Sensor ID\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "  }\n"
            + "  sensor_depth {\n"
            + "    Float64 colorBarMaximum 100.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  direction_of_sea_water_velocity {\n"
            + "    Float64 colorBarMaximum 360.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String standard_name \"direction_of_sea_water_velocity\";\n"
            + "    String units \"degrees_true\";\n"
            + "  }\n"
            + "  sea_water_speed {\n"
            + "    Float64 colorBarMaximum 50.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String standard_name \"sea_water_speed\";\n"
            + "    String units \"cm s-1\";\n"
            + "  }\n"
            + "  platform_orientation {\n"
            + "    Float64 colorBarMaximum 360.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String units \"degrees_true\";\n"
            + "  }\n"
            + "  platform_pitch_angle {\n"
            + "    Float64 colorBarMaximum 50.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String units \"degrees\";\n"
            + "  }\n"
            + "  platform_roll_angle {\n"
            + "    Float64 colorBarMaximum 50.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String units \"degrees\";\n"
            + "  }\n"
            + "  sea_water_temperature {\n"
            + "    Float64 colorBarMaximum 32.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String standard_name \"sea_water_temperature\";\n"
            + "    String units \"degree_C\";\n"
            + "  }\n"
            + "  orientation {\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "  }\n"
            + "  sampling_rate {\n"
            + "    Float64 colorBarMaximum 10000.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String units \"Hz\";\n"
            + "  }\n"
            + "  reporting_interval {\n"
            + "    Float64 colorBarMaximum 3600.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String units \"s\";\n"
            + "  }\n"
            + "  processing_level {\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "  }\n"
            + "  bin_size {\n"
            + "    Float64 colorBarMaximum 50.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  first_bin_center {\n"
            + "    Float64 colorBarMaximum 50.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  number_of_bins {\n"
            + "    Float64 colorBarMaximum 100.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "  }\n"
            + "  bin {\n"
            + "    Float64 colorBarMaximum 100.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String units \"count\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_data_type \"TimeSeriesProfile\";\n"
            + "    String cdm_profile_variables \"time\";\n"
            + "    String cdm_timeseries_variables \"station_id, longitude, latitude, sensor_id\";\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "    Float64 Easternmost_Easting -66.9956;\n"
            + "    String featureType \"TimeSeriesProfile\";\n"
            + "    Float64 geospatial_lat_max 61.2782;\n"
            + "    Float64 geospatial_lat_min 19.6351;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max -66.9956;\n"
            + "    Float64 geospatial_lon_min -167.9007;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    String geospatial_vertical_positive \"up\";\n"
            + "    String geospatial_vertical_units \"m\";\n"
            + "    String history \""
            + today;
    // + " https://opendap.co-ops.nos.noaa.gov/ioos-dif-sos"; //-test
    Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);

    /*
     * https://opendap.co-ops.nos.noaa.gov/ioos-dif-sos/SOS?service=SOS&request=
     * GetCapabilities
     * <sos:ObservationOffering gml:id="network_currentsactive">
     * <gml:description>
     * All CurrentsActive stations on NOAA.NOS.CO-OPS SOS server
     * </gml:description>
     * <gml:name>urn:ioos:network:noaa.nos.co-ops:currentsactive</gml:name>
     * <gml:boundedBy>
     * <gml:Envelope srsName="http://www.opengis.net/def/crs/EPSG/0/4326">
     * <gml:lowerCorner>27.6250 -124.2498</gml:lowerCorner>
     * <gml:upperCorner>48.8628 -71.1784</gml:upperCorner>
     * </gml:Envelope>
     * </gml:boundedBy>
     * <sos:time>
     * <gml:TimePeriod>
     * <gml:beginPosition>2011-03-30T01:00:00Z</gml:beginPosition>
     * <gml:endPosition indeterminatePosition="now"/>
     * </gml:TimePeriod>
     * </sos:time>
     * <sos:procedure xlink:href="urn:ioos:network:NOAA.NOS.CO-OPS:CurrentsActive"/>
     * <sos:observedProperty xlink:href=
     * "http://mmisw.org/ont/cf/parameter/direction_of_sea_water_velocity"/>
     * <sos:observedProperty
     * xlink:href="http://mmisw.org/ont/cf/parameter/sea_water_speed"/>
     * <sos:featureOfInterest xlink:href="urn:cgi:feature:CGI:EarthOcean"/>
     * <sos:responseFormat>text/csv</sos:responseFormat>
     * <sos:responseFormat>text/tab-separated-values</sos:responseFormat>
     * <sos:responseFormat>application/vnd.google-earth.kml+xml</sos:responseFormat>
     * <sos:responseFormat>text/xml;subtype="om/1.0.0/profiles/ioos_sos/1.0"</sos:
     * responseFormat>
     * <sos:resultModel>om:ObservationCollection</sos:resultModel>
     * <sos:responseMode>inline</sos:responseMode>
     * </sos:ObservationOffering>
     *
     * <sos:ObservationOffering gml:id="network_currentssurvey">
     * <gml:description>
     * All CurrentsSurvey stations on NOAA.NOS.CO-OPS SOS server
     * </gml:description>
     * <gml:name>urn:ioos:network:noaa.nos.co-ops:currentssurvey</gml:name>
     * <gml:boundedBy>
     * <gml:Envelope srsName="http://www.opengis.net/def/crs/EPSG/0/4326">
     * <gml:lowerCorner>19.6351 -167.9007</gml:lowerCorner>
     * <gml:upperCorner>61.2782 -66.9956</gml:upperCorner>
     * </gml:Envelope>
     * </gml:boundedBy>
     * <sos:time>
     * <gml:TimePeriod>
     * <gml:beginPosition>1997-05-07T10:30:00Z</gml:beginPosition>
     * <gml:endPosition>2015-09-13T20:56:00Z</gml:endPosition>
     * </gml:TimePeriod>
     * </sos:time>
     * <sos:procedure xlink:href="urn:ioos:network:NOAA.NOS.CO-OPS:CurrentsSurvey"/>
     * <sos:observedProperty xlink:href=
     * "http://mmisw.org/ont/cf/parameter/direction_of_sea_water_velocity"/>
     * <sos:observedProperty
     * xlink:href="http://mmisw.org/ont/cf/parameter/sea_water_speed"/>
     * <sos:featureOfInterest xlink:href="urn:cgi:feature:CGI:EarthOcean"/>
     * <sos:responseFormat>text/csv</sos:responseFormat>
     * <sos:responseFormat>text/tab-separated-values</sos:responseFormat>
     * <sos:responseFormat>application/vnd.google-earth.kml+xml</sos:responseFormat>
     * <sos:responseFormat>text/xml;subtype="om/1.0.0/profiles/ioos_sos/1.0"</sos:
     * responseFormat>
     * <sos:resultModel>om:ObservationCollection</sos:resultModel>
     * <sos:responseMode>inline</sos:responseMode>
     * </sos:ObservationOffering>
     *
     * ERROR while processing response from
     * requestUrl=https://opendap.co-ops.nos.noaa.gov/ioos-dif-sos/SOS?service=S
     * OS&version=1.0.0&request=GetObservation&offering=urn:ioos:station:NOAA.NOS.CO
     * -OPS:lc0301&observedProperty=http:
     * //mmisw.org/ont/cf/parameter/direction_of_sea_water_velocity&responseFormat=
     * text%2Fcsv&eventTime=2013-09-01T00:
     * 03:00Z
     * java.lang.RuntimeException: Source Exception="InvalidParameterValue:
     * observedProperty: Valid observedProperty v
     * alues: air_temperature, air_pressure, sea_water_electrical_conductivity,
     * currents, sea_water_salinity, water_su
     * rface_height_above_reference_datum,
     * sea_surface_height_amplitude_due_to_equilibrium_ocean_tide, sea_water_tempe
     * rature, winds, harmonic_constituents, datums, relative_humidity, rain_fall,
     * visibility".
     */
    // throw new RuntimeException(
    // "This started failing ~Feb 2013. Then okay.\n" +
    // "2015-12-10 failing: Was observedProperty=currents\n" +
    // " now GetCapabilities says direction_of_sea_water_velocity,
    // sea_water_speed\n" +
    // " but using those in datasets.xml says it must be one of ... currents\n" +
    // " Ah! I see there are 2 group procedures (not separate stations)\n" +
    // " but ERDDAP doesn't support such groups right now.",
    // t);
    // FUTURE: add support for this new approach. Changes are endless!!!!
  }

  /**
   * This should work, but server is in flux so it often breaks.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testNosSosSalinity() throws Throwable {
    // testVerboseOn();

    int language = 0;
    EDDTable eddTable = (EDDTable) EDDTestDataset.getnosSosSalinity();
    String name, tName, results, expected, userDapQuery;

    String2.log("\n*** EDDTableFromSOS.testNosSosSalinity .das\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_nosSosSalinity",
            ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range -146.362, -70.741;\n"
            + // ..., pre 2013-11-04 was -94.985, changed 2012-03-16
            "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float64 actual_range 27.8461, 61.125;\n"
            + // ..., pre 2013-11-04 was 41.7043, pre 2013-06-28 was
            // 29.6817 pre 2013-05-22 was 29.48 ;pre 2012-03-16 was 43.32
            "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  station_id {\n"
            + "    String cf_role \"timeseries_id\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Station ID\";\n"
            + "  }\n"
            + "  altitude {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    String _CoordinateZisPositive \"up\";\n"
            + "    String axis \"Z\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Altitude\";\n"
            + "    String positive \"up\";\n"
            + "    String standard_name \"altitude\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n";
    // " Float64 actual_range -2.2184928e+9, NaN;\n" + //-2.1302784e+9
    Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);

    // it was hard to find data. station advertises 1999+ for several composites.
    // but searching January's for each year found no data until 2006
    // ??? Does the error message need to be treated as simple no data?
    // <ExceptionReport><Exception>atts=exceptionCode="NoApplicableCode",
    // locator="8419317"
    // 2012-03-16 station=...8419317 stopped working
    // 2012-11-18 I switched to station 8447386
    // 2013-11-04 used
    // testFindValidStation(eddTable, testNosStations,
    // "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:",
    // "\"&time>=2013-09-01T00:03&time<=2013-09-01T01");

    String2.log("\n*** EDDTableFromSOS.testNosSosSalinity test get one station .CSV data\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:8452660\"&time>=2013-09-01T00&time<=2013-09-01T01",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_nosSosSalinity",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        // station 8419317: salinity is low because this is in estuary/river, not in
        // ocean
        // "longitude,latitude,station_id,altitude,time,sensor_id,sea_water_salinity\n"
        // +
        // "degrees_east,degrees_north,,m,UTC,,PSU\n" +
        // "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,2.283\n"
        // +
        // "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,2.286\n"
        // +
        // "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,2.295\n"
        // +
        // "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,2.286\n"
        // +
        // "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,2.286\n"
        // +
        // "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,2.286\n"
        // +
        // "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,2.283\n"
        // +
        // "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,2.267\n"
        // +
        // "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,2.448\n"
        // +
        // "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,2.486\n";
        // no it is because data was off by a factor of 10!!!
        /*
         * starting 2011-12-16:
         * "longitude,latitude,station_id,altitude,time,sensor_id,sea_water_salinity\n"
         * +
         * "degrees_east,degrees_north,,m,UTC,,PSU\n" +
         * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,27.075\n"
         * +
         * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,27.192\n"
         * +
         * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,27.226\n"
         * +
         * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,27.192\n"
         * +
         * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,27.192\n"
         * +
         * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,27.192\n"
         * +
         * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,27.075\n"
         * +
         * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,26.791\n"
         * +
         * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,28.319\n"
         * +
         * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,28.692\n";
         * //No! Wait! after lunch 2011-12-16 the results became:
         * "longitude,latitude,station_id,altitude,time,sensor_id,sea_water_salinity\n"
         * +
         * "degrees_east,degrees_north,,m,UTC,,PSU\n" +
         * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,27.158\n"
         * +
         * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,27.192\n"
         * +
         * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,27.31\n"
         * +
         * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,27.192\n"
         * +
         * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,27.192\n"
         * +
         * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,27.192\n"
         * +
         * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,27.158\n"
         * +
         * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,26.957\n"
         * +
         * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,29.2\n"
         * +
         * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,29.675\n";
         * retired 2013-11-04:
         * "longitude,latitude,station_id,altitude,time,sensor_id,sea_water_salinity\n"
         * +
         * "degrees_east,degrees_north,,m,UTC,,PSU\n" +
         * "-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2012-01-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:SALINITY,26.718\n"
         * +
         * "-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2012-01-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:SALINITY,26.611\n"
         * +
         * "-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2012-01-01T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:SALINITY,26.718\n"
         * +
         * "-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2012-01-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:SALINITY,26.718\n"
         * +
         * "-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2012-01-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:SALINITY,26.747\n"
         * +
         * "-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2012-01-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:SALINITY,26.825\n"
         * +
         * "-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2012-01-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:SALINITY,26.854\n"
         * +
         * "-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2012-01-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:SALINITY,26.854\n"
         * +
         * "-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2012-01-01T00:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:SALINITY,26.747\n"
         * +
         * "-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2012-01-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:SALINITY,26.854\n"
         * +
         * "-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2012-01-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:SALINITY,26.854\n";
         * 2013-11-04 forced to switch stations
         * "longitude,latitude,station_id,altitude,time,sensor_id,sea_water_salinity\n"
         * +
         * "degrees_east,degrees_north,,m,UTC,,PSU\n" +
         * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.692\n"
         * +
         * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.614\n"
         * +
         * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.692\n"
         * +
         * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.692\n"
         * +
         * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.692\n"
         * +
         * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.692\n"
         * +
         * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.692\n"
         * +
         * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.692\n"
         * +
         * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.692\n"
         * +
         * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.77\n"
         * +
         * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.692\n";
         * 2014-01-24 results modified a little
         * "longitude,latitude,station_id,altitude,time,sensor_id,sea_water_salinity\n"
         * +
         * "degrees_east,degrees_north,,m,UTC,,PSU\n" +
         * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.677\n"
         * +
         * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.608\n"
         * +
         * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.689\n"
         * +
         * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.682\n"
         * +
         * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.713\n"
         * +
         * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.695\n"
         * +
         * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.67\n"
         * +
         * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.704\n"
         * +
         * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.748\n"
         * +
         * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.68\n";
         * //2015-12-10: Historical data keeps changing. It's absurd! :12 is back,
         * precision changed.
         */
        "longitude,latitude,station_id,altitude,time,sensor_id,sea_water_salinity\n"
            + "degrees_east,degrees_north,,m,UTC,,PSU\n"
            + "-71.3261,41.5043,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.7\n"
            + "-71.3261,41.5043,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.62\n"
            + "-71.3261,41.5043,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.7\n"
            + "-71.3261,41.5043,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.7\n"
            + "-71.3261,41.5043,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.7\n"
            + "-71.3261,41.5043,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.7\n"
            + "-71.3261,41.5043,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.7\n"
            + "-71.3261,41.5043,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.7\n"
            + "-71.3261,41.5043,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.7\n"
            + "-71.3261,41.5043,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.77\n"
            + "-71.3261,41.5043,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.7\n";

    // 2015-12-28 sensor_id name changed to G1,
    // but 2016-01-19 it changed back
    /*
     * "longitude,latitude,station_id,altitude,time,sensor_id,sea_water_salinity\n"
     * +
     * "degrees_east,degrees_north,,m,UTC,,PSU\n" +
     * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,29.7\n"
     * +
     * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,29.62\n"
     * +
     * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,29.7\n"
     * +
     * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,29.7\n"
     * +
     * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,29.7\n"
     * +
     * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,29.7\n"
     * +
     * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,29.7\n"
     * +
     * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,29.7\n"
     * +
     * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,29.7\n"
     * +
     * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,29.77\n"
     * +
     * "-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,29.7\n";
     */
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);
  }

  /**
   * This should work, but server is in flux so it often breaks.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testNosSosWind() throws Throwable {
    // testVerboseOn();

    int language = 0;
    EDDTable eddTable = (EDDTable) EDDTestDataset.getnosSosWind();
    String name, tName, results, expected, userDapQuery;

    // stopped working ~12/15/2008
    // "&station_id=\"urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8419317\"&time>=2006-01-01T00&time<=2006-01-01T01",
    // stopped working ~2009-03-26, and no recent data
    // "&station_id=\"urn:x-noaa:def:station:NOAA.NOS.CO-OPS::9461380\"&time>=2008-09-01T00&time<=2008-09-01T01",

    String2.log("\n*** EDDTableFromSOS.testNosSosWind test get one station .CSV data\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:9468756\"&time>=2009-04-06T00&time<=2009-04-06T01",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_nosSosWind",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "longitude,latitude,station_id,altitude,time,sensor_id,wind_from_direction,wind_speed,wind_speed_of_gust\n"
            + "degrees_east,degrees_north,,m,UTC,,degrees_true,m s-1,m s-1\n"
            + "-165.43,64.5,urn:ioos:station:NOAA.NOS.CO-OPS:9468756,NaN,2009-04-06T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9468756:C1,79.0,7.8,9.5\n"
            + "-165.43,64.5,urn:ioos:station:NOAA.NOS.CO-OPS:9468756,NaN,2009-04-06T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9468756:C1,81.0,7.4,9.5\n"
            + "-165.43,64.5,urn:ioos:station:NOAA.NOS.CO-OPS:9468756,NaN,2009-04-06T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9468756:C1,80.0,7.8,9.5\n"
            + "-165.43,64.5,urn:ioos:station:NOAA.NOS.CO-OPS:9468756,NaN,2009-04-06T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9468756:C1,79.0,8.1,10.1\n"
            + "-165.43,64.5,urn:ioos:station:NOAA.NOS.CO-OPS:9468756,NaN,2009-04-06T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9468756:C1,79.0,7.3,9.0\n"
            + "-165.43,64.5,urn:ioos:station:NOAA.NOS.CO-OPS:9468756,NaN,2009-04-06T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9468756:C1,76.0,7.6,8.9\n"
            + "-165.43,64.5,urn:ioos:station:NOAA.NOS.CO-OPS:9468756,NaN,2009-04-06T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9468756:C1,82.0,7.2,8.5\n"
            + "-165.43,64.5,urn:ioos:station:NOAA.NOS.CO-OPS:9468756,NaN,2009-04-06T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9468756:C1,79.0,8.1,9.2\n"
            + "-165.43,64.5,urn:ioos:station:NOAA.NOS.CO-OPS:9468756,NaN,2009-04-06T00:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9468756:C1,80.0,7.3,9.4\n"
            + "-165.43,64.5,urn:ioos:station:NOAA.NOS.CO-OPS:9468756,NaN,2009-04-06T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9468756:C1,81.0,8.0,9.4\n"
            + "-165.43,64.5,urn:ioos:station:NOAA.NOS.CO-OPS:9468756,NaN,2009-04-06T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9468756:C1,80.0,7.9,10.3\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    String2.log("\n*** EDDTableFromSOS.testNosSosWind .das\n");
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard
    // to check min:sec.
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_nosSosWind",
            ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range -176.632, 166.618;\n"
            + "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float64 actual_range -14.28, 70.4;\n"
            + "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  station_id {\n"
            + "    String cf_role \"timeseries_id\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Station ID\";\n"
            + "  }\n"
            + "  altitude {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    String _CoordinateZisPositive \"up\";\n"
            + "    String axis \"Z\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Altitude\";\n"
            + "    String positive \"up\";\n"
            + "    String standard_name \"altitude\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range -3.6757152e+9, NaN;\n"
            + "    String axis \"T\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  sensor_id {\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Sensor ID\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/winds\";\n"
            + "  }\n"
            + "  wind_from_direction {\n"
            + "    Float64 colorBarMaximum 3600.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Wind\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/winds\";\n"
            + "    String standard_name \"wind_from_direction\";\n"
            + "    String units \"degrees_true\";\n"
            + "  }\n"
            + "  wind_speed {\n"
            + "    Float64 colorBarMaximum 1500.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Wind\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/winds\";\n"
            + "    String standard_name \"wind_speed\";\n"
            + "    String units \"m s-1\";\n"
            + "  }\n"
            + "  wind_speed_of_gust {\n"
            + "    Float64 colorBarMaximum 300.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Wind\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/winds\";\n"
            + "    String standard_name \"wind_speed_of_gust\";\n"
            + "    String units \"m s-1\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_data_type \"TimeSeries\";\n"
            + "    String cdm_timeseries_variables \"station_id, longitude, latitude, sensor_id\";\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "    Float64 Easternmost_Easting 166.618;\n"
            + "    String featureType \"TimeSeries\";\n"
            + "    Float64 geospatial_lat_max 70.4;\n"
            + "    Float64 geospatial_lat_min -14.28;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max 166.618;\n"
            + "    Float64 geospatial_lon_min -176.632;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    String geospatial_vertical_positive \"up\";\n"
            + "    String geospatial_vertical_units \"m\";\n"
            + "    String history \""
            + today;
    // + " https://opendap.co-ops.nos.noaa.gov/ioos-dif-sos"; //-test
    Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);
  }

  /**
   * This should work, but server is in flux so it often breaks.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testNosSosWLevel() throws Throwable {
    // testVerboseOn();

    int language = 0;
    EDDTable eddTable = (EDDTable) EDDTestDataset.getnosSosWLevel();
    String name, tName, results, expected, userDapQuery;

    // 2013-11-04 used to find a station with data:
    // testFindValidStation(eddTable, testNosStations,
    // "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:",
    // "\"&time>=2013-09-01T00:03&time<=2013-09-01T01");

    String2.log("\n*** EDDTableFromSOS.testNosSosWLevel .das\n");
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard
    // to check min:sec.
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_nosSosWLevel",
            ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range -177.36, 167.7361;\n"
            + // changes
            "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float64 actual_range -14.28, 70.4114;\n"
            + "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  station_id {\n"
            + "    String cf_role \"timeseries_id\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Station ID\";\n"
            + "  }\n"
            + "  altitude {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    String _CoordinateZisPositive \"up\";\n"
            + "    String axis \"Z\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Altitude\";\n"
            + "    String positive \"up\";\n"
            + "    String standard_name \"altitude\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range -3.675801598e+9, NaN;\n"
            + "    String axis \"T\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  sensor_id {\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Sensor ID\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/water_surface_height_above_reference_datum\";\n"
            + "  }\n"
            + "  water_level {\n"
            + "    Float64 colorBarMaximum 5.0;\n"
            + "    Float64 colorBarMinimum -5.0;\n"
            + "    String ioos_category \"Sea Level\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/water_surface_height_above_reference_datum\";\n"
            + "    String standard_name \"water_surface_height_above_reference_datum\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  datum_id {\n"
            + "    String ioos_category \"Sea Level\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/water_surface_height_above_reference_datum\";\n"
            + "  }\n"
            + "  vertical_position {\n"
            + "    String ioos_category \"Sea Level\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/water_surface_height_above_reference_datum\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  sigma {\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Sigma\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/water_surface_height_above_reference_datum\";\n"
            + "  }\n"
            + "  quality_flags {\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Quality Flags\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/water_surface_height_above_reference_datum\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_data_type \"TimeSeries\";\n"
            + "    String cdm_timeseries_variables \"station_id, longitude, latitude, sensor_id\";\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "    Float64 Easternmost_Easting 167.7361;\n"
            + "    String featureType \"TimeSeries\";\n"
            + "    Float64 geospatial_lat_max 70.4114;\n"
            + "    Float64 geospatial_lat_min -14.28;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max 167.7361;\n"
            + "    Float64 geospatial_lon_min -177.36;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    String geospatial_vertical_positive \"up\";\n"
            + "    String geospatial_vertical_units \"m\";\n"
            + "    String history \""
            + today;
    // + " https://opendap.co-ops.nos.noaa.gov/ioos-dif-sos"; //-test
    Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);

    String2.log("\n*** EDDTableFromSOS.testNosSosWLevel test get one station .CSV data\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:9454050\"&time>=2013-09-01T00&time<=2013-09-01T01",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_nosSosWLevel",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        /*
         * //pre 2009-11-15 and post 2009-12-13
         * "longitude,latitude,station_id,altitude,time,WaterLevel\n" +
         * "degrees_east,degrees_north,,m,UTC,m\n" +
         * "-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:00:00Z,75.014\n"
         * +
         * "-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:06:00Z,75.014\n"
         * +
         * "-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:12:00Z,75.018\n"
         * +
         * "-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:18:00Z,75.015\n"
         * +
         * "-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:24:00Z,75.012\n"
         * +
         * "-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:30:00Z,75.012\n"
         * +
         * "-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:36:00Z,75.016\n"
         * +
         * "-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:42:00Z,75.018\n"
         * +
         * "-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:48:00Z,75.02\n"
         * +
         * "-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:54:00Z,75.019\n"
         * +
         * "-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T15:00:00Z,75.017\n";
         */
        /*
         * "longitude,latitude,station_id,altitude,time,WaterLevel\n" +
         * "degrees_east,degrees_north,,m,UTC,m\n" +
         * "-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:00:00Z,75.021\n"
         * +
         * "-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:06:00Z,75.022\n"
         * +
         * "-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:12:00Z,75.025\n"
         * +
         * "-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:18:00Z,75.023\n"
         * +
         * "-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:24:00Z,75.02\n"
         * +
         * "-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:30:00Z,75.019\n"
         * +
         * "-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:36:00Z,75.023\n"
         * +
         * "-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:42:00Z,75.025\n"
         * +
         * "-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:48:00Z,75.027\n"
         * +
         * "-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:54:00Z,75.026\n"
         * +
         * "-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T15:00:00Z,75.024\n";
         *
         * "longitude,latitude,station_id,altitude,time,sensor_id,water_level,datum_id,vertical_position\n"
         * +
         * "degrees_east,degrees_north,,m,UTC,,m,,m\n" +
         * "-145.753,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.601,urn:ioos:def:datum:noaa::MLLW,1.916\n"
         * +
         * "-145.753,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.584,urn:ioos:def:datum:noaa::MLLW,1.916\n"
         * +
         * "-145.753,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.569,urn:ioos:def:datum:noaa::MLLW,1.916\n"
         * +
         * "-145.753,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.558,urn:ioos:def:datum:noaa::MLLW,1.916\n"
         * +
         * "-145.753,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.547,urn:ioos:def:datum:noaa::MLLW,1.916\n"
         * +
         * "-145.753,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.534,urn:ioos:def:datum:noaa::MLLW,1.916\n"
         * +
         * "-145.753,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.522,urn:ioos:def:datum:noaa::MLLW,1.916\n"
         * +
         * "-145.753,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.509,urn:ioos:def:datum:noaa::MLLW,1.916\n"
         * +
         * "-145.753,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.5,urn:ioos:def:datum:noaa::MLLW,1.916\n"
         * +
         * "-145.753,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.495,urn:ioos:def:datum:noaa::MLLW,1.916\n"
         * +
         * "-145.753,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.491,urn:ioos:def:datum:noaa::MLLW,1.916\n";
         * 2015-12-11:
         */
        "longitude,latitude,station_id,altitude,time,sensor_id,water_level,datum_id,vertical_position,sigma,quality_flags\n"
            + "degrees_east,degrees_north,,m,UTC,,m,,m,,\n"
            + "-145.7554,60.5575,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.601,urn:ioos:def:datum:noaa::MLLW,1.916,0.003,0;0;0;0\n"
            + "-145.7554,60.5575,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.584,urn:ioos:def:datum:noaa::MLLW,1.916,0.002,0;0;0;0\n"
            + "-145.7554,60.5575,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.569,urn:ioos:def:datum:noaa::MLLW,1.916,0.003,0;0;0;0\n"
            + "-145.7554,60.5575,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.558,urn:ioos:def:datum:noaa::MLLW,1.916,0.006,0;0;0;0\n"
            + "-145.7554,60.5575,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.547,urn:ioos:def:datum:noaa::MLLW,1.916,0.001,0;0;0;0\n"
            + "-145.7554,60.5575,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.534,urn:ioos:def:datum:noaa::MLLW,1.916,0.002,0;0;0;0\n"
            + "-145.7554,60.5575,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.522,urn:ioos:def:datum:noaa::MLLW,1.916,0.003,0;0;0;0\n"
            + "-145.7554,60.5575,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.509,urn:ioos:def:datum:noaa::MLLW,1.916,0.002,0;0;0;0\n"
            + "-145.7554,60.5575,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.5,urn:ioos:def:datum:noaa::MLLW,1.916,0.001,0;0;0;0\n"
            + "-145.7554,60.5575,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.495,urn:ioos:def:datum:noaa::MLLW,1.916,0.001,0;0;0;0\n"
            + "-145.7554,60.5575,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.491,urn:ioos:def:datum:noaa::MLLW,1.916,0.001,0;0;0;0\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);
  }

  /**
   * This should work, but server is in flux so it often breaks.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testNosSosWTemp() throws Throwable {
    // testVerboseOn();

    int language = 0;
    EDDTable eddTable = (EDDTable) EDDTestDataset.getnosSosWTemp();
    String name, tName, results, expected, userDapQuery;

    String2.log("\n*** EDDTableFromSOS.testNosSosWTemp .das\n");
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard
    // to check min:sec.
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_nosSosWTemp",
            ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range -177.36, 167.7361;\n"
            + // changes
            "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float64 actual_range -14.28, 70.4114;\n"
            + // changes
            "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  station_id {\n"
            + "    String cf_role \"timeseries_id\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Station ID\";\n"
            + "  }\n"
            + "  altitude {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    String _CoordinateZisPositive \"up\";\n"
            + "    String axis \"Z\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Altitude\";\n"
            + "    String positive \"up\";\n"
            + "    String standard_name \"altitude\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range -3.675801598e+9, NaN;\n"
            + "    String axis \"T\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  sensor_id {\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Sensor ID\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/sea_water_temperature\";\n"
            + "  }\n"
            + "  sea_water_temperature {\n"
            + "    Float64 colorBarMaximum 32.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Temperature\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/sea_water_temperature\";\n"
            + "    String standard_name \"sea_water_temperature\";\n"
            + "    String units \"degree_C\";\n"
            + "  }\n"
            + "  quality_flags {\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Quality Flags\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/sea_water_temperature\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_data_type \"TimeSeries\";\n"
            + "    String cdm_timeseries_variables \"station_id, longitude, latitude, sensor_id\";\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "    Float64 Easternmost_Easting 167.7361;\n"
            + "    String featureType \"TimeSeries\";\n"
            + "    Float64 geospatial_lat_max 70.4114;\n"
            + "    Float64 geospatial_lat_min -14.28;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max 167.7361;\n"
            + "    Float64 geospatial_lon_min -177.36;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    String geospatial_vertical_positive \"up\";\n"
            + "    String geospatial_vertical_units \"m\";\n"
            + "    String history \""
            + today;
    // + " https://opendap.co-ops.nos.noaa.gov/ioos-dif-sos"; //-test
    Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);

    // String2.log("\n*** EDDTableFromSOS.testNosSosWTemp test get one station .CSV
    // data\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:8311062\"&time>=2008-08-01T14:00&time<2008-08-01T15:00",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_nosSosWTemp",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        /*
         * "longitude,latitude,station_id,altitude,time,sensor_id,sea_water_temperature\n"
         * +
         * "degrees_east,degrees_north,,m,UTC,,degree_C\n" +
         * "-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.349\n"
         * +
         * "-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.4\n"
         * +
         * "-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.4\n"
         * +
         * "-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.4\n"
         * +
         * "-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.366\n"
         * +
         * "-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.4\n"
         * +
         * "-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.37\n"
         * +
         * "-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.379\n"
         * +
         * "-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.382\n"
         * +
         * "-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.389\n";
         * 2015-12-11 precision changed from 3 to 1 decimal digits:
         */
        "longitude,latitude,station_id,altitude,time,sensor_id,sea_water_temperature,quality_flags\n"
            + "degrees_east,degrees_north,,m,UTC,,degree_C,\n"
            + "-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.3,0;0;0\n"
            + "-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.4,0;0;0\n"
            + "-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.4,0;0;0\n"
            + "-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.4,0;0;0\n"
            + "-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.4,0;0;0\n"
            + "-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.4,0;0;0\n"
            + "-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.4,0;0;0\n"
            + "-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.4,0;0;0\n"
            + "-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.4,0;0;0\n"
            + "-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.4,0;0;0\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    // String2.log("\n*** EDDTableFromSOS.testNosSosWTemp test get one station .CSV
    // data\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:9432780\"&time>=2008-09-01T14:00&time<2008-09-01T15:00",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_nosSosWTemp",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "longitude,latitude,station_id,altitude,time,sensor_id,sea_water_temperature,quality_flags\n"
            + "degrees_east,degrees_north,,m,UTC,,degree_C,\n"
            + "-124.322,43.345,urn:ioos:station:NOAA.NOS.CO-OPS:9432780,NaN,2008-09-01T14:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1,11.6,0;0;0\n"
            + "-124.322,43.345,urn:ioos:station:NOAA.NOS.CO-OPS:9432780,NaN,2008-09-01T14:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1,11.6,0;0;0\n"
            + "-124.322,43.345,urn:ioos:station:NOAA.NOS.CO-OPS:9432780,NaN,2008-09-01T14:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1,11.6,0;0;0\n"
            + "-124.322,43.345,urn:ioos:station:NOAA.NOS.CO-OPS:9432780,NaN,2008-09-01T14:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1,11.7,0;0;0\n"
            + "-124.322,43.345,urn:ioos:station:NOAA.NOS.CO-OPS:9432780,NaN,2008-09-01T14:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1,11.7,0;0;0\n"
            + "-124.322,43.345,urn:ioos:station:NOAA.NOS.CO-OPS:9432780,NaN,2008-09-01T14:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1,11.6,0;0;0\n"
            + "-124.322,43.345,urn:ioos:station:NOAA.NOS.CO-OPS:9432780,NaN,2008-09-01T14:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1,11.6,0;0;0\n"
            + "-124.322,43.345,urn:ioos:station:NOAA.NOS.CO-OPS:9432780,NaN,2008-09-01T14:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1,11.6,0;0;0\n"
            + "-124.322,43.345,urn:ioos:station:NOAA.NOS.CO-OPS:9432780,NaN,2008-09-01T14:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1,11.6,0;0;0\n"
            + "-124.322,43.345,urn:ioos:station:NOAA.NOS.CO-OPS:9432780,NaN,2008-09-01T14:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1,11.6,0;0;0\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    // String2.log("\n*** EDDTableFromSOS.testNosSosWTemp test .geoJson lat, lon,
    // alt\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "altitude,longitude,latitude&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:9432780\"&time>=2008-09-01T14:00&time<2008-09-01T15:00",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_nosSosWTemp1",
            ".geoJson");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "{\n"
            + "  \"type\": \"MultiPoint\",\n"
            + "  \"coordinates\": [\n"
            + "[-124.322, 43.345],\n"
            + "[-124.322, 43.345],\n"
            + "[-124.322, 43.345],\n"
            + "[-124.322, 43.345],\n"
            + "[-124.322, 43.345],\n"
            + "[-124.322, 43.345],\n"
            + "[-124.322, 43.345],\n"
            + "[-124.322, 43.345],\n"
            + "[-124.322, 43.345],\n"
            + "[-124.322, 43.345]\n"
            + "  ],\n"
            + "  \"bbox\": [-124.322, 43.345, -124.322, 43.345]\n"
            + "}\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    // String2.log("\n*** EDDTableFromSOS.testNosSosWTemp test .geoJson all
    // vars\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:9432780\"&time>=2008-09-01T14:00&time<2008-09-01T15:00",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_nosSosWTemp2",
            ".geoJson");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "{\n"
            + "  \"type\": \"FeatureCollection\",\n"
            + "  \"propertyNames\": [\"station_id\", \"time\", \"sensor_id\", \"sea_water_temperature\", \"quality_flags\"],\n"
            + "  \"propertyUnits\": [null, \"UTC\", null, \"degree_C\", null],\n"
            + "  \"features\": [\n"
            + "{\"type\": \"Feature\",\n"
            + "  \"geometry\": {\n"
            + "    \"type\": \"Point\",\n"
            +
            // 2019-03-20 was " \"coordinates\": [-124.322, 43.345, null] },\n" +
            "    \"coordinates\": [-124.322, 43.345] },\n"
            + "  \"properties\": {\n"
            + "    \"station_id\": \"urn:ioos:station:NOAA.NOS.CO-OPS:9432780\",\n"
            + "    \"time\": \"2008-09-01T14:00:00Z\",\n"
            + "    \"sensor_id\": \"urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1\",\n"
            + "    \"sea_water_temperature\": 11.6,\n"
            + "    \"quality_flags\": \"0;0;0\" }\n"
            + "},\n"
            + "{\"type\": \"Feature\",\n"
            + "  \"geometry\": {\n"
            + "    \"type\": \"Point\",\n"
            + "    \"coordinates\": [-124.322, 43.345] },\n"
            + "  \"properties\": {\n"
            + "    \"station_id\": \"urn:ioos:station:NOAA.NOS.CO-OPS:9432780\",\n"
            + "    \"time\": \"2008-09-01T14:06:00Z\",\n"
            + "    \"sensor_id\": \"urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1\",\n"
            + "    \"sea_water_temperature\": 11.6,\n"
            + "    \"quality_flags\": \"0;0;0\" }\n"
            + "},\n"
            + "{\"type\": \"Feature\",\n"
            + "  \"geometry\": {\n"
            + "    \"type\": \"Point\",\n"
            + "    \"coordinates\": [-124.322, 43.345] },\n"
            + "  \"properties\": {\n"
            + "    \"station_id\": \"urn:ioos:station:NOAA.NOS.CO-OPS:9432780\",\n"
            + "    \"time\": \"2008-09-01T14:12:00Z\",\n"
            + "    \"sensor_id\": \"urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1\",\n"
            + "    \"sea_water_temperature\": 11.6,\n"
            + "    \"quality_flags\": \"0;0;0\" }\n"
            + "},\n"
            + "{\"type\": \"Feature\",\n"
            + "  \"geometry\": {\n"
            + "    \"type\": \"Point\",\n"
            + "    \"coordinates\": [-124.322, 43.345] },\n"
            + "  \"properties\": {\n"
            + "    \"station_id\": \"urn:ioos:station:NOAA.NOS.CO-OPS:9432780\",\n"
            + "    \"time\": \"2008-09-01T14:18:00Z\",\n"
            + "    \"sensor_id\": \"urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1\",\n"
            + "    \"sea_water_temperature\": 11.7,\n"
            + "    \"quality_flags\": \"0;0;0\" }\n"
            + "},\n"
            + "{\"type\": \"Feature\",\n"
            + "  \"geometry\": {\n"
            + "    \"type\": \"Point\",\n"
            + "    \"coordinates\": [-124.322, 43.345] },\n"
            + "  \"properties\": {\n"
            + "    \"station_id\": \"urn:ioos:station:NOAA.NOS.CO-OPS:9432780\",\n"
            + "    \"time\": \"2008-09-01T14:24:00Z\",\n"
            + "    \"sensor_id\": \"urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1\",\n"
            + "    \"sea_water_temperature\": 11.7,\n"
            + "    \"quality_flags\": \"0;0;0\" }\n"
            + "},\n"
            + "{\"type\": \"Feature\",\n"
            + "  \"geometry\": {\n"
            + "    \"type\": \"Point\",\n"
            + "    \"coordinates\": [-124.322, 43.345] },\n"
            + "  \"properties\": {\n"
            + "    \"station_id\": \"urn:ioos:station:NOAA.NOS.CO-OPS:9432780\",\n"
            + "    \"time\": \"2008-09-01T14:30:00Z\",\n"
            + "    \"sensor_id\": \"urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1\",\n"
            + "    \"sea_water_temperature\": 11.6,\n"
            + "    \"quality_flags\": \"0;0;0\" }\n"
            + "},\n"
            + "{\"type\": \"Feature\",\n"
            + "  \"geometry\": {\n"
            + "    \"type\": \"Point\",\n"
            + "    \"coordinates\": [-124.322, 43.345] },\n"
            + "  \"properties\": {\n"
            + "    \"station_id\": \"urn:ioos:station:NOAA.NOS.CO-OPS:9432780\",\n"
            + "    \"time\": \"2008-09-01T14:36:00Z\",\n"
            + "    \"sensor_id\": \"urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1\",\n"
            + "    \"sea_water_temperature\": 11.6,\n"
            + "    \"quality_flags\": \"0;0;0\" }\n"
            + "},\n"
            + "{\"type\": \"Feature\",\n"
            + "  \"geometry\": {\n"
            + "    \"type\": \"Point\",\n"
            + "    \"coordinates\": [-124.322, 43.345] },\n"
            + "  \"properties\": {\n"
            + "    \"station_id\": \"urn:ioos:station:NOAA.NOS.CO-OPS:9432780\",\n"
            + "    \"time\": \"2008-09-01T14:42:00Z\",\n"
            + "    \"sensor_id\": \"urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1\",\n"
            + "    \"sea_water_temperature\": 11.6,\n"
            + "    \"quality_flags\": \"0;0;0\" }\n"
            + "},\n"
            + "{\"type\": \"Feature\",\n"
            + "  \"geometry\": {\n"
            + "    \"type\": \"Point\",\n"
            + "    \"coordinates\": [-124.322, 43.345] },\n"
            + "  \"properties\": {\n"
            + "    \"station_id\": \"urn:ioos:station:NOAA.NOS.CO-OPS:9432780\",\n"
            + "    \"time\": \"2008-09-01T14:48:00Z\",\n"
            + "    \"sensor_id\": \"urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1\",\n"
            + "    \"sea_water_temperature\": 11.6,\n"
            + "    \"quality_flags\": \"0;0;0\" }\n"
            + "},\n"
            + "{\"type\": \"Feature\",\n"
            + "  \"geometry\": {\n"
            + "    \"type\": \"Point\",\n"
            + "    \"coordinates\": [-124.322, 43.345] },\n"
            + "  \"properties\": {\n"
            + "    \"station_id\": \"urn:ioos:station:NOAA.NOS.CO-OPS:9432780\",\n"
            + "    \"time\": \"2008-09-01T14:54:00Z\",\n"
            + "    \"sensor_id\": \"urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1\",\n"
            + "    \"sea_water_temperature\": 11.6,\n"
            + "    \"quality_flags\": \"0;0;0\" }\n"
            + "}\n"
            + "  ],\n"
            +
            // 2019-03-20 was " \"bbox\": [-124.322, 43.345, null, -124.322, 43.345,
            // null]\n" +
            "  \"bbox\": [-124.322, 43.345, -124.322, 43.345]\n"
            + "}\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);
  }

  /**
   * This should work, but server is in flux so it often breaks. Note: starting in 2009, there is a
   * test server at https://sdftest.ndbc.noaa.gov/sos/ .
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testNdbcSosCurrents() throws Throwable {
    // String2.log("\n*** EDDTableFromSOS.testNdbcSosCurrents");
    int language = 0;

    // testVerboseOn();
    // see 6/12/08 email from Roy
    // see web page: https://sdf.ndbc.noaa.gov/sos/
    // https://sdf.ndbc.noaa.gov/sos/server.php?request=GetObservation&service=SOS
    // &offering=NDBC:46088&observedproperty=currents&responseformat=text/xml;
    // schema=%22ioos/0.6.1%22&eventtime=2008-06-01T00:00Z/2008-06-02T00:00Z
    // request from -70.14 43.53 1193961600 NaN NDBC:44007
    EDDTable eddTable = (EDDTable) EDDTestDataset.getndbcSosCurrents();
    double tLon, tLat;
    String name, tName, results = null, expected, userDapQuery;
    Table table;
    String error = "";

    String2.log("\n*** EDDTableFromSOS.testNdbcSosCurrents .das\n");
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard
    // to check min:sec.
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_ndbc_test1",
            ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range -172.088, -66.588;\n"
            + "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float64 actual_range 17.043, 60.802;\n"
            + "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  station_id {\n"
            + "    String cf_role \"timeseries_id\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Station ID\";\n"
            + "  }\n"
            + "  altitude {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    String _CoordinateZisPositive \"up\";\n"
            + "    String axis \"Z\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Altitude\";\n"
            + "    String positive \"up\";\n"
            + "    String standard_name \"altitude\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range 1.187181e+9, NaN;\n"
            + "    String axis \"T\";\n"
            + "    String cf_role \"profile_id\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  sensor_id {\n"
            + "    String comment \"Always check the quality_flags before using this data.\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Sensor ID\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "  }\n"
            + "  bin {\n"
            + "    Byte _FillValue 127;\n"
            + "    String _Unsigned \"false\";\n"
            + "    String comment \"Always check the quality_flags before using this data.\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Bin\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String units \"count\";\n"
            + "  }\n"
            + "  direction_of_sea_water_velocity {\n"
            + "    Int16 _FillValue 32767;\n"
            + "    Float64 colorBarMaximum 0.5;\n"
            + "    Float64 colorBarMinimum -0.5;\n"
            + "    String comment \"Always check the quality_flags before using this data.\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Direction Of Sea Water Velocity\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String standard_name \"direction_of_sea_water_velocity\";\n"
            + "    String units \"degrees_true\";\n"
            + "  }\n"
            + "  sea_water_speed {\n"
            + "    Float64 colorBarMaximum 0.5;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String comment \"Always check the quality_flags before using this data.\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Sea Water Speed\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String standard_name \"sea_water_speed\";\n"
            + "    String units \"cm/s\";\n"
            + "  }\n"
            + "  upward_sea_water_velocity {\n"
            + "    Float64 colorBarMaximum 0.5;\n"
            + "    Float64 colorBarMinimum -0.5;\n"
            + "    String comment \"Always check the quality_flags before using this data.\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Upward Sea Water Velocity\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String standard_name \"upward_sea_water_velocity\";\n"
            + "    String units \"cm/s\";\n"
            + "  }\n"
            + "  error_velocity {\n"
            + "    String comment \"Always check the quality_flags before using this data.\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Error Velocity\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String units \"cm/s\";\n"
            + "  }\n"
            + "  platform_orientation {\n"
            + "    Int16 _FillValue 32767;\n"
            + "    String comment \"Always check the quality_flags before using this data.\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Platform Orientation\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String units \"degrees_true\";\n"
            + "  }\n"
            + "  platform_pitch_angle {\n"
            + "    Int16 _FillValue 32767;\n"
            + "    String comment \"Always check the quality_flags before using this data.\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Platform Pitch Angle\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String units \"degree\";\n"
            + "  }\n"
            + "  platform_roll_angle {\n"
            + "    Int16 _FillValue 32767;\n"
            + "    String comment \"Always check the quality_flags before using this data.\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Platform Roll Angle\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String units \"degree\";\n"
            + "  }\n"
            + "  sea_water_temperature {\n"
            + "    Float64 colorBarMaximum 32.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String comment \"Always check the quality_flags before using this data.\";\n"
            + "    String ioos_category \"Temperature\";\n"
            + "    String long_name \"Sea Water Temperature\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String standard_name \"sea_water_temperature\";\n"
            + "    String units \"Cel\";\n"
            + "  }\n"
            + "  pct_good_3_beam {\n"
            + "    Byte _FillValue 127;\n"
            + "    String _Unsigned \"false\";\n"
            + "    String comment \"Always check the quality_flags before using this data.\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Percent Good 3 Beam\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String units \"percent\";\n"
            + "  }\n"
            + "  pct_good_4_beam {\n"
            + "    Byte _FillValue 127;\n"
            + "    String _Unsigned \"false\";\n"
            + "    String comment \"Always check the quality_flags before using this data.\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Percent Good 4 Beam\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String units \"percent\";\n"
            + "  }\n"
            + "  pct_rejected {\n"
            + "    Byte _FillValue 127;\n"
            + "    String _Unsigned \"false\";\n"
            + "    String comment \"Always check the quality_flags before using this data.\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Percent Rejected\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String units \"percent\";\n"
            + "  }\n"
            + "  pct_bad {\n"
            + "    Byte _FillValue 127;\n"
            + "    String _Unsigned \"false\";\n"
            + "    String comment \"Always check the quality_flags before using this data.\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Percent Bad\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String units \"percent\";\n"
            + "  }\n"
            + "  echo_intensity_beam1 {\n"
            + "    Int16 _FillValue 32767;\n"
            + "    String comment \"Always check the quality_flags before using this data.\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Echo Intensity Beam 1\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String units \"count\";\n"
            + "  }\n"
            + "  echo_intensity_beam2 {\n"
            + "    Int16 _FillValue 32767;\n"
            + "    String comment \"Always check the quality_flags before using this data.\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Echo Intensity Beam #2\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String units \"count\";\n"
            + "  }\n"
            + "  echo_intensity_beam3 {\n"
            + "    Int16 _FillValue 32767;\n"
            + "    String comment \"Always check the quality_flags before using this data.\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Echo Intensity Beam 3\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String units \"count\";\n"
            + "  }\n"
            + "  echo_intensity_beam4 {\n"
            + "    Int16 _FillValue 32767;\n"
            + "    String comment \"Always check the quality_flags before using this data.\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Echo Intensity Beam 4\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String units \"count\";\n"
            + "  }\n"
            + "  correlation_magnitude_beam1 {\n"
            + "    Int16 _FillValue 32767;\n"
            + "    String comment \"Always check the quality_flags before using this data.\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Correlation Magnitude Beam 1\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String units \"count\";\n"
            + "  }\n"
            + "  correlation_magnitude_beam2 {\n"
            + "    Int16 _FillValue 32767;\n"
            + "    String comment \"Always check the quality_flags before using this data.\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Correlation Magnitude Beam #2\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String units \"count\";\n"
            + "  }\n"
            + "  correlation_magnitude_beam3 {\n"
            + "    Int16 _FillValue 32767;\n"
            + "    String comment \"Always check the quality_flags before using this data.\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Correlation Magnitude Beam 3\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String units \"count\";\n"
            + "  }\n"
            + "  correlation_magnitude_beam4 {\n"
            + "    Int16 _FillValue 32767;\n"
            + "    String comment \"Always check the quality_flags before using this data.\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Correlation Magnitude Beam 4\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "    String units \"count\";\n"
            + "  }\n"
            + "  quality_flags {\n"
            + "    String comment \"Always check the quality_flags before using this data.\";\n"
            + "    String description \"These nine, semi-colon-separated quality flags represent the results of the following quality tests based on their position (left to right) in the flags field:\n"
            + "Flag#  Meaning\n"
            + "-----  ------------------------------------------\n"
            + "  1    The overall bin status\n"
            + "  2    The ADCP Built-In Test (BIT) status\n"
            + "  3    The Error Velocity test status\n"
            + "  4    The Percent Good test status\n"
            + "  5    The Correlation Magnitude test status\n"
            + "  6    The Vertical Velocity test status\n"
            + "  7    The North Horizontal Velocity test status\n"
            + "  8    The East Horizontal Velocity test status\n"
            + "  9    The Echo Intensity test status\n"
            + "\n"
            + "Valid flag values are:\n"
            + "0 = quality not evaluated\n"
            + "1 = failed quality test\n"
            + "2 = questionable or suspect data\n"
            + "3 = good data/passed quality test\n"
            + "9 = missing data\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Quality Flags\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_data_type \"TimeSeriesProfile\";\n"
            + "    String cdm_profile_variables \"time\";\n"
            + "    String cdm_timeseries_variables \"station_id, longitude, latitude, sensor_id\";\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "    Float64 Easternmost_Easting -66.588;\n"
            + // 2019-03-19 small changes to bbox
            "    String featureType \"TimeSeriesProfile\";\n"
            + "    Float64 geospatial_lat_max 60.802;\n"
            + "    Float64 geospatial_lat_min 17.043;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max -66.588;\n"
            + "    Float64 geospatial_lon_min -172.088;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    String geospatial_vertical_positive \"up\";\n"
            + "    String geospatial_vertical_units \"m\";\n"
            + "    String history \""
            + today;
    String tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

    // + " https://sdf" + datasetIdPrefix + ".ndbc.noaa.gov/sos/server.php\n" +
    // today + " " + EDStatic.erddapUrl + //in tests, always use non-https url
    // "/tabledap/" +
    expected =
        "ndbcSosCurrents.das\";\n"
            + "    String infoUrl \"https://sdf.ndbc.noaa.gov/sos/\";\n"
            + "    String institution \"NOAA NDBC\";\n"
            + "    String keywords \"angle, atmosphere, bad, beam, bin, circulation, correlation, currents, depth, direction, direction_of_sea_water_velocity, Earth Science > Oceans > Ocean Circulation > Ocean Currents, Earth Science > Oceans > Ocean Temperature > Water Temperature, echo, error, flags, good, height, identifier, intensity, magnitude, ndbc, noaa, ocean, oceans, orientation, percent, pitch, platform, quality, rejected, roll, sea, sea_water_speed, sea_water_temperature, seawater, sensor, sos, speed, station, temperature, time, upward, upward_sea_water_velocity, velocity, water\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    Float64 Northernmost_Northing 60.802;\n"
            + "    String sourceUrl \"https://sdf.ndbc.noaa.gov/sos/server.php\";\n"
            + "    Float64 Southernmost_Northing 17.043;\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String subsetVariables \"station_id, longitude, latitude\";\n"
            + "    String summary \"The NOAA NDBC SOS server is part of the IOOS DIF SOS Project.  The stations in this dataset have currents data.\n"
            + "\n"
            + "Because of the nature of SOS requests, requests for data MUST include constraints for the longitude, latitude, time, and/or station_id variables.\n"
            + "\n"
            + "WARNING: Always check the quality_flags before using this data. A simple criterion is: only use a row of data if the first quality_flags value for the row (overall bin status) is 3 (good data/passed quality test). You can do this by appending &quality_flags=~\\\"3;.*\\\" to your request.\";\n"
            + "    String time_coverage_start \"2007-08-15T12:30:00Z\";\n"
            + "    String title \"NOAA NDBC SOS, 2007-present, currents\";\n"
            + "    Float64 Westernmost_Easting -172.088;\n"
            + "  }\n"
            + "}\n";
    int tPo = results.indexOf(expected.substring(0, 17));
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
        expected,
        "results=\n" + results);

    // test lon lat (numeric) = > <, and time > <
    // String2.log("\n*** EDDTableFromSOS.testNdbcSosCurrents test get one station
    // .CSV data\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            ""
                +
                // 2019-03-19 was -87.94,29.16 now -87.944,29.108
                "&longitude=-87.944&latitude>=29.1&latitude<29.2&time>=2008-06-01T14:00&time<=2008-06-01T14:30",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_ndbc_test1",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "longitude,latitude,station_id,altitude,time,sensor_id,bin,direction_of_sea_water_velocity,sea_water_speed,upward_sea_water_velocity,error_velocity,platform_orientation,platform_pitch_angle,platform_roll_angle,sea_water_temperature,pct_good_3_beam,pct_good_4_beam,pct_rejected,pct_bad,echo_intensity_beam1,echo_intensity_beam2,echo_intensity_beam3,echo_intensity_beam4,correlation_magnitude_beam1,correlation_magnitude_beam2,correlation_magnitude_beam3,correlation_magnitude_beam4,quality_flags\n"
            + "degrees_east,degrees_north,,m,UTC,,count,degrees_true,cm/s,cm/s,cm/s,degrees_true,degree,degree,Cel,percent,percent,percent,percent,count,count,count,count,count,count,count,count,\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-56.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,1,83,30.2,-2.6,-4.7,NaN,NaN,NaN,NaN,0,100,0,NaN,189,189,189,193,241,239,242,240,3;9;3;3;3;3;3;3;0\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-88.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,2,96,40.5,-2.5,-3.7,NaN,NaN,NaN,NaN,0,100,0,NaN,177,174,180,178,237,235,230,237,3;9;3;3;3;3;3;3;0\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-120.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,3,96,40.7,-1.3,-9.6,NaN,NaN,NaN,NaN,0,100,0,NaN,165,163,159,158,232,234,238,236,3;9;3;3;3;3;3;3;0\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-152.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,4,96,35.3,-2.0,-2.2,NaN,NaN,NaN,NaN,0,100,0,NaN,151,147,160,153,232,235,237,241,3;9;3;3;3;3;3;3;0\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);

    expected =
        "-87.944,29.108,urn:ioos:station:wmo:42376,-952.8,2008-06-01T14:23:00Z,urn:ioos:sensor:wmo:42376::adcp0,29,89,4.0,0.5,0.0,NaN,NaN,NaN,NaN,100,0,0,NaN,74,75,96,48,236,237,NaN,239,2;9;2;3;9;3;3;3;2\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-984.8,2008-06-01T14:23:00Z,urn:ioos:sensor:wmo:42376::adcp0,30,0,0.0,0.0,0.0,NaN,NaN,NaN,NaN,0,0,0,NaN,69,69,93,39,NaN,NaN,NaN,217,1;9;2;1;9;1;1;1;2\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-1016.8,2008-06-01T14:23:00Z,urn:ioos:sensor:wmo:42376::adcp0,31,0,0.0,0.0,0.0,NaN,NaN,NaN,NaN,0,0,0,NaN,62,65,89,33,NaN,NaN,NaN,245,1;9;2;1;9;1;1;1;2\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-1048.8,2008-06-01T14:23:00Z,urn:ioos:sensor:wmo:42376::adcp0,32,0,0.0,0.0,0.0,NaN,NaN,NaN,NaN,0,0,0,NaN,199,111,73,1,NaN,NaN,NaN,212,1;9;2;1;9;1;1;1;2\n";
    Test.ensureEqual(
        results.substring(results.length() - expected.length()), expected, "RESULTS=\n" + results);

    // test quality_flags regex (just GOOD data): &quality_flags=~"3;.*"
    String2.log("\n*** EDDTableFromSOS.testNcbcSosCurrents test get one station .CSV data\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            ""
                +
                // 2019-03-19 was -87.94 now -87.944
                "&longitude=-87.944&latitude>=29.1&latitude<29.2&time>=2008-06-01T14:00&time<=2008-06-01T14:30"
                + "&quality_flags=~\"3;.*\"",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_ndbc_test1",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "longitude,latitude,station_id,altitude,time,sensor_id,bin,direction_of_sea_water_velocity,sea_water_speed,upward_sea_water_velocity,error_velocity,platform_orientation,platform_pitch_angle,platform_roll_angle,sea_water_temperature,pct_good_3_beam,pct_good_4_beam,pct_rejected,pct_bad,echo_intensity_beam1,echo_intensity_beam2,echo_intensity_beam3,echo_intensity_beam4,correlation_magnitude_beam1,correlation_magnitude_beam2,correlation_magnitude_beam3,correlation_magnitude_beam4,quality_flags\n"
            + "degrees_east,degrees_north,,m,UTC,,count,degrees_true,cm/s,cm/s,cm/s,degrees_true,degree,degree,Cel,percent,percent,percent,percent,count,count,count,count,count,count,count,count,\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-56.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,1,83,30.2,-2.6,-4.7,NaN,NaN,NaN,NaN,0,100,0,NaN,189,189,189,193,241,239,242,240,3;9;3;3;3;3;3;3;0\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-88.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,2,96,40.5,-2.5,-3.7,NaN,NaN,NaN,NaN,0,100,0,NaN,177,174,180,178,237,235,230,237,3;9;3;3;3;3;3;3;0\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-120.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,3,96,40.7,-1.3,-9.6,NaN,NaN,NaN,NaN,0,100,0,NaN,165,163,159,158,232,234,238,236,3;9;3;3;3;3;3;3;0\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-152.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,4,96,35.3,-2.0,-2.2,NaN,NaN,NaN,NaN,0,100,0,NaN,151,147,160,153,232,235,237,241,3;9;3;3;3;3;3;3;0\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);

    expected = // this is different from previous test
        "-87.944,29.108,urn:ioos:station:wmo:42376,-600.8,2008-06-01T14:23:00Z,urn:ioos:sensor:wmo:42376::adcp0,18,160,4.6,0.1,-0.2,NaN,NaN,NaN,NaN,0,100,0,NaN,119,120,88,108,240,239,240,242,3;9;3;3;3;3;3;3;3\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-632.8,2008-06-01T14:23:00Z,urn:ioos:sensor:wmo:42376::adcp0,19,166,5.9,-0.6,-0.7,NaN,NaN,NaN,NaN,0,100,0,NaN,112,113,89,106,241,240,240,240,3;9;3;3;3;3;3;3;3\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-664.8,2008-06-01T14:23:00Z,urn:ioos:sensor:wmo:42376::adcp0,20,142,3.7,-1.7,-3.8,NaN,NaN,NaN,NaN,0,100,0,NaN,107,108,90,102,241,240,240,240,3;9;3;3;3;3;3;3;3\n";
    Test.ensureEqual(
        results.substring(results.length() - expected.length()), expected, "RESULTS=\n" + results);

    // test station regex
    String2.log(
        "\n*** EDDTableFromSOS.testNcbcSosCurrents test get 2 stations from regex, 1 time, .CSV data\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&station_id=~\"(urn:ioos:station:wmo:41035|urn:ioos:station:wmo:42376)\""
                + "&time>=2008-06-01T14:00&time<=2008-06-01T14:15",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_ndbc_test1b",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        // Before revamping 2008-10, test returned values below. NOW DIFFERENT!
        // "urn:ioos:station:wmo:41035,-77.28,34.48,-1.6,2008-06-01T14:00:00Z,223,3.3\n"
        // + now 74,15.2
        // "urn:ioos:station:wmo:42376,-87.94,29.16,-3.8,2008-06-01T14:00:00Z,206,19.0\n";
        "longitude,latitude,station_id,altitude,time,sensor_id,bin,direction_of_sea_water_velocity,sea_water_speed,upward_sea_water_velocity,error_velocity,platform_orientation,platform_pitch_angle,platform_roll_angle,sea_water_temperature,pct_good_3_beam,pct_good_4_beam,pct_rejected,pct_bad,echo_intensity_beam1,echo_intensity_beam2,echo_intensity_beam3,echo_intensity_beam4,correlation_magnitude_beam1,correlation_magnitude_beam2,correlation_magnitude_beam3,correlation_magnitude_beam4,quality_flags\n"
            + "degrees_east,degrees_north,,m,UTC,,count,degrees_true,cm/s,cm/s,cm/s,degrees_true,degree,degree,Cel,percent,percent,percent,percent,count,count,count,count,count,count,count,count,\n"
            + "-77.28,34.476,urn:ioos:station:wmo:41035,-1.6,2008-06-01T14:00:00Z,urn:ioos:sensor:wmo:41035::pscm0,1,74,15.2,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-56.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,1,83,30.2,-2.6,-4.7,NaN,NaN,NaN,NaN,0,100,0,NaN,189,189,189,193,241,239,242,240,3;9;3;3;3;3;3;3;0\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-88.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,2,96,40.5,-2.5,-3.7,NaN,NaN,NaN,NaN,0,100,0,NaN,177,174,180,178,237,235,230,237,3;9;3;3;3;3;3;3;0\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-120.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,3,96,40.7,-1.3,-9.6,NaN,NaN,NaN,NaN,0,100,0,NaN,165,163,159,158,232,234,238,236,3;9;3;3;3;3;3;3;0\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-152.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,4,96,35.3,-2.0,-2.2,NaN,NaN,NaN,NaN,0,100,0,NaN,151,147,160,153,232,235,237,241,3;9;3;3;3;3;3;3;0\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-184.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,5,89,31.9,-1.9,-1.1,NaN,NaN,NaN,NaN,0,100,0,NaN,145,144,151,151,239,241,237,241,3;9;3;3;3;3;3;3;0\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-216.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,6,90,25.2,-2.7,-3.8,NaN,NaN,NaN,NaN,0,100,0,NaN,144,145,141,148,240,240,239,239,3;9;3;3;3;3;3;3;0\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-248.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,7,74,22.5,-2.6,-4.0,NaN,NaN,NaN,NaN,0,100,0,NaN,142,144,133,131,240,238,241,241,3;9;3;3;3;3;3;3;0\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-280.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,8,65,21.5,-1.7,-0.8,NaN,NaN,NaN,NaN,0,100,0,NaN,132,133,133,119,237,237,241,234,3;9;3;3;3;3;3;3;0\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-312.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,9,71,16.4,-0.2,1.9,NaN,NaN,NaN,NaN,0,100,0,NaN,115,117,132,124,238,238,239,240,3;9;3;3;3;3;3;3;0\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-344.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,10,96,13.8,-1.2,0.6,NaN,NaN,NaN,NaN,0,100,0,NaN,101,102,135,133,242,241,239,240,3;9;3;3;3;3;3;3;0\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-376.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,11,110,14.2,-0.5,0.0,NaN,NaN,NaN,NaN,0,100,0,NaN,101,102,133,133,240,241,240,242,3;9;3;3;3;3;3;3;0\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-408.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,12,121,12.9,-1.6,-4.4,NaN,NaN,NaN,NaN,0,100,0,NaN,103,105,125,137,241,240,240,240,3;9;3;3;3;3;3;3;0\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-440.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,13,111,7.4,-1.9,-4.6,NaN,NaN,NaN,NaN,0,100,0,NaN,106,108,116,136,239,240,241,240,3;9;3;3;3;3;3;3;0\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-472.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,14,6,2.0,0.1,0.4,NaN,NaN,NaN,NaN,0,100,0,NaN,110,114,104,130,241,241,240,241,3;9;3;3;3;3;3;3;0\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-504.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,15,17,2.8,1.0,2.3,NaN,NaN,NaN,NaN,0,100,0,NaN,118,122,94,125,242,241,241,241,3;9;3;3;3;3;3;3;0\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-536.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,16,37,3.8,0.6,2.5,NaN,NaN,NaN,NaN,0,100,0,NaN,121,124,89,122,240,240,241,240,3;9;3;3;3;3;3;3;3\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-568.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,17,124,0.4,1.2,2.3,NaN,NaN,NaN,NaN,0,100,0,NaN,122,124,88,115,240,240,239,240,3;9;3;3;3;3;3;3;3\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-600.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,18,147,4.5,0.0,0.5,NaN,NaN,NaN,NaN,0,100,0,NaN,119,120,88,108,240,240,241,241,3;9;3;3;3;3;3;3;3\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-632.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,19,167,5.7,-0.1,-1.2,NaN,NaN,NaN,NaN,0,100,0,NaN,113,114,89,105,241,240,240,239,3;9;3;3;3;3;3;3;3\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-664.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,20,150,3.8,-0.8,-3.1,NaN,NaN,NaN,NaN,0,100,0,NaN,108,109,90,101,241,241,240,239,3;9;3;3;3;3;3;3;3\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-696.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,21,317,4.1,0.0,0.0,NaN,NaN,NaN,NaN,100,0,0,NaN,104,104,92,98,241,242,NaN,239,2;9;2;3;9;3;3;3;3\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-728.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,22,31,2.9,0.3,0.0,NaN,NaN,NaN,NaN,100,0,0,NaN,102,103,96,97,240,240,NaN,241,2;9;2;3;9;3;3;3;2\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-760.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,23,62,3.4,-0.6,0.0,NaN,NaN,NaN,NaN,100,0,0,NaN,98,100,99,87,239,240,NaN,240,2;9;2;3;9;3;3;3;2\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-792.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,24,24,4.4,0.2,0.0,NaN,NaN,NaN,NaN,100,0,0,NaN,92,95,102,88,241,241,NaN,241,2;9;2;3;9;3;3;3;2\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-824.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,25,56,4.8,-0.2,0.0,NaN,NaN,NaN,NaN,100,0,0,NaN,88,90,103,81,239,240,NaN,240,2;9;2;3;9;3;3;3;2\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-856.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,26,55,2.8,0.0,0.0,NaN,NaN,NaN,NaN,100,0,0,NaN,87,87,103,112,240,240,NaN,239,2;9;2;3;9;3;3;3;2\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-888.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,27,68,4.6,-0.3,0.0,NaN,NaN,NaN,NaN,100,0,0,NaN,86,87,104,166,240,239,NaN,238,2;9;2;3;9;3;3;3;2\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-920.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,28,68,5.2,-0.3,0.0,NaN,NaN,NaN,NaN,100,0,0,NaN,78,80,99,78,238,238,NaN,238,2;9;2;3;9;3;3;3;2\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-952.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,29,75,4.3,0.4,0.0,NaN,NaN,NaN,NaN,100,0,0,NaN,73,75,96,48,237,238,NaN,240,2;9;2;3;9;3;3;3;2\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-984.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,30,0,0.0,0.0,0.0,NaN,NaN,NaN,NaN,0,0,0,NaN,69,68,92,37,NaN,NaN,NaN,222,1;9;2;1;9;1;1;1;2\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-1016.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,31,0,0.0,0.0,0.0,NaN,NaN,NaN,NaN,0,0,0,NaN,63,64,89,31,NaN,NaN,NaN,244,1;9;2;1;9;1;1;1;2\n"
            + "-87.944,29.108,urn:ioos:station:wmo:42376,-1048.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,32,0,0.0,0.0,0.0,NaN,NaN,NaN,NaN,0,0,0,NaN,197,112,72,0,NaN,NaN,NaN,215,1;9;2;1;9;1;1;1;2\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);

    // test station =
    String2.log(
        "\n*** EDDTableFromSOS.testNdbcSosCurrents test get by station name, multi depths, .CSV data\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&station_id=\"urn:ioos:station:wmo:41012\"&time>=2008-06-01T00&time<=2008-06-01T01",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_ndbc_test2",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "longitude,latitude,station_id,altitude,time,sensor_id,bin,direction_of_sea_water_velocity,sea_water_speed,upward_sea_water_velocity,error_velocity,platform_orientation,platform_pitch_angle,platform_roll_angle,sea_water_temperature,pct_good_3_beam,pct_good_4_beam,pct_rejected,pct_bad,echo_intensity_beam1,echo_intensity_beam2,echo_intensity_beam3,echo_intensity_beam4,correlation_magnitude_beam1,correlation_magnitude_beam2,correlation_magnitude_beam3,correlation_magnitude_beam4,quality_flags\n"
            + "degrees_east,degrees_north,,m,UTC,,count,degrees_true,cm/s,cm/s,cm/s,degrees_true,degree,degree,Cel,percent,percent,percent,percent,count,count,count,count,count,count,count,count,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-5.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,1,56,6.3,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-7.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,2,59,14.8,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-9.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,3,57,20.6,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-11.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,4,56,22.2,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-13.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,5,59,25.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-15.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,6,63,27.6,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-17.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,7,70,31.8,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-19.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,8,73,33.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-21.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,9,74,33.8,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-23.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,10,75,33.5,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-25.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,11,76,32.8,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-27.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,12,75,31.5,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-29.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,13,77,28.9,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-31.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,14,78,25.4,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-33.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,15,80,23.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-5.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,1,52,14.9,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-7.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,2,63,23.8,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-9.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,3,68,28.9,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-11.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,4,71,31.5,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-13.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,5,74,32.5,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-15.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,6,81,31.9,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-17.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,7,83,31.3,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-19.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,8,85,31.3,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-21.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,9,84,32.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-23.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,10,85,30.7,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-25.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,11,86,29.6,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-27.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,12,85,28.2,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-29.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,13,86,26.9,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-31.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,14,86,25.2,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-33.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,15,85,22.6,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    // many stations (this was "long hard test", now with text/csv it is quick and
    // easy)
    String2.log("\n*** EDDTableFromSOS.testNdbcSosCurrents test get data from many stations\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&time>=2008-06-14T00&time<=2008-06-14T02&altitude=-25",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_ndbc_test3",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "longitude,latitude,station_id,altitude,time,sensor_id,bin,direction_of_sea_water_velocity,sea_water_speed,upward_sea_water_velocity,error_velocity,platform_orientation,platform_pitch_angle,platform_roll_angle,sea_water_temperature,pct_good_3_beam,pct_good_4_beam,pct_rejected,pct_bad,echo_intensity_beam1,echo_intensity_beam2,echo_intensity_beam3,echo_intensity_beam4,correlation_magnitude_beam1,correlation_magnitude_beam2,correlation_magnitude_beam3,correlation_magnitude_beam4,quality_flags\n"
            + "degrees_east,degrees_north,,m,UTC,,count,degrees_true,cm/s,cm/s,cm/s,degrees_true,degree,degree,Cel,percent,percent,percent,percent,count,count,count,count,count,count,count,count,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-25.0,2008-06-14T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,11,93,22.4,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-25.0,2008-06-14T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,11,96,19.7,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-25.0,2008-06-14T02:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,11,103,19.7,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-76.949,34.207,urn:ioos:station:wmo:41036,-25.0,2008-06-14T00:00:00Z,urn:ioos:sensor:wmo:41036::adcp0,13,170,11.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-76.949,34.207,urn:ioos:station:wmo:41036,-25.0,2008-06-14T01:00:00Z,urn:ioos:sensor:wmo:41036::adcp0,13,190,11.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n"
            + "-76.949,34.207,urn:ioos:station:wmo:41036,-25.0,2008-06-14T02:00:00Z,urn:ioos:sensor:wmo:41036::adcp0,13,220,9.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    String2.log("\n*** EDDTableFromSOS.testNdbcSosCurrents test display error in .png\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "station_id,longitude,latitude,altitude,time,zztop"
                + "&station_id=\"urn:ioos:network:noaa.nws.ndbc:all\"&time=2008-06-14T00",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_ndbc_testError",
            ".png");
    Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
  }

  /**
   * This should work, but server is in flux so it often breaks.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testNdbcSosSalinity() throws Throwable {
    // testVerboseOn();

    int language = 0;
    EDDTable eddTable = (EDDTable) EDDTestDataset.getndbcSosSalinity();
    String name, tName, results = null, expected, userDapQuery;

    String2.log("\n*** EDDTableFromSOS.testNdbcSosSalinity .das\n");
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard
    // to check min:sec.
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_ndbcSosSalinity",
            ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range -155.828, -39.52;\n"
            + // 2021-05-14 was -148.263, -60.521 //2014-08-11 was -65.927
            // //2012-10-10 was -151.719 //2016-09-21 was -64.763
            "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float64 actual_range -55.0, 60.802;\n"
            + // 2014-08-12 was 24.843, 2010-10-10 was 17.93, 2016-09-21 was
            // 17.86
            "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  station_id {\n"
            + "    String cf_role \"timeseries_id\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Station ID\";\n"
            + "  }\n"
            + "  altitude {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    String _CoordinateZisPositive \"up\";\n"
            + "    String axis \"Z\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Altitude\";\n"
            + "    String positive \"up\";\n"
            + "    String standard_name \"altitude\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range 1.1939616e+9, NaN;\n"
            + // changes
            "    String axis \"T\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  sensor_id {\n"
            + "    String comment \"Always check the quality_flags before using this data.\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Sensor ID\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/sea_water_salinity\";\n"
            + "  }\n"
            + "  sea_water_salinity {\n"
            + "    Float64 colorBarMaximum 37.0;\n"
            + "    Float64 colorBarMinimum 32.0;\n"
            + "    String ioos_category \"Salinity\";\n"
            + "    String long_name \"Sea Water Practical Salinity\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/sea_water_salinity\";\n"
            + "    String standard_name \"sea_water_practical_salinity\";\n"
            + "    String units \"PSU\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_data_type \"TimeSeries\";\n"
            + "    String cdm_timeseries_variables \"station_id, longitude, latitude, sensor_id\";\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "    Float64 Easternmost_Easting -39.52;\n"
            + "    String featureType \"TimeSeries\";\n"
            + "    Float64 geospatial_lat_max 60.802;\n"
            + "    Float64 geospatial_lat_min -55.0;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max -39.52;\n"
            + "    Float64 geospatial_lon_min -155.828;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    String geospatial_vertical_positive \"up\";\n"
            + "    String geospatial_vertical_units \"m\";\n"
            + "    String history \""
            + today;
    String tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

    // + " https://sdf" + datasetIdPrefix + ".ndbc.noaa.gov/sos/server.php\n" +
    // today + " " + EDStatic.erddapUrl + //in tests, always use non-https url
    // "/tabledap/" +
    expected =
        "ndbcSosSalinity.das\";\n"
            + "    String infoUrl \"https://sdf.ndbc.noaa.gov/sos/\";\n"
            + "    String institution \"NOAA NDBC\";\n"
            + "    String keywords \"altitude, atmosphere, density, Earth Science > Atmosphere > Altitude > Station Height, Earth Science > Oceans > Salinity/Density > Salinity, height, identifier, ndbc, noaa, oceans, salinity, sea, sea_water_practical_salinity, seawater, sensor, sos, station, time, water\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    Float64 Northernmost_Northing 60.802;\n"
            + "    String sourceUrl \"https://sdf.ndbc.noaa.gov/sos/server.php\";\n"
            + "    Float64 Southernmost_Northing -55.0;\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String subsetVariables \"station_id, longitude, latitude\";\n"
            + "    String summary \"The NOAA NDBC SOS server is part of the IOOS DIF SOS Project.  The stations in this dataset have sea_water_practical_salinity data.\n"
            + "\n"
            + "Because of the nature of SOS requests, requests for data MUST include constraints for the longitude, latitude, time, and/or station_id variables.\";\n"
            + "    String time_coverage_start \"2007-11-02T00:00:00Z\";\n"
            + // changes
            "    String title \"NOAA NDBC SOS, 2007-present, sea_water_practical_salinity\";\n"
            + "    Float64 Westernmost_Easting -155.828;\n"
            + "  }\n"
            + "}\n";
    int tPo = results.indexOf(expected.substring(0, 17));
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
        expected,
        "results=\n" + results);

    String2.log("\n*** EDDTableFromSOS.testNdbcSosSalinity test get one station .CSV data\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&station_id=\"urn:ioos:station:wmo:46013\"&time>=2008-08-01&time<2008-08-02",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_ndbcSosSalinity",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "longitude,latitude,station_id,altitude,time,sensor_id,sea_water_salinity\n"
            + "degrees_east,degrees_north,,m,UTC,,PSU\n"
            + "-123.303,38.253,urn:ioos:station:wmo:46013,-1.0,2008-08-01T20:50:00Z,urn:ioos:sensor:wmo:46013::ct1,33.89\n"
            + "-123.303,38.253,urn:ioos:station:wmo:46013,-1.0,2008-08-01T22:50:00Z,urn:ioos:sensor:wmo:46013::ct1,33.89\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    String2.log("\n*** EDDTableFromSOS.testNdbcSosSalinity test get all stations .CSV data\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&time>=2010-05-27T00:00:00Z&time<=2010-05-27T01:00:00Z",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_ndbcSosSalinityAll",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected = // 46081 appeared 2010-07-20, anrn6 and apqf1 disappeared 2010-10-10
        "longitude,latitude,station_id,altitude,time,sensor_id,sea_water_salinity\n"
            + "degrees_east,degrees_north,,m,UTC,,PSU\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-1.0,2010-05-27T00:50:00Z,urn:ioos:sensor:wmo:41012::ct1,35.58\n"
            + "-65.909,42.325,urn:ioos:station:wmo:44024,-1.0,2010-05-27T01:00:00Z,urn:ioos:sensor:wmo:44024::ct1,32.4\n"
            + // 2021-05-14 was -65.912,42.327
            "-70.566,42.523,urn:ioos:station:wmo:44029,-1.0,2010-05-27T00:00:00Z,urn:ioos:sensor:wmo:44029::ct1,30.2\n"
            + "-70.566,42.523,urn:ioos:station:wmo:44029,-1.0,2010-05-27T01:00:00Z,urn:ioos:sensor:wmo:44029::ct1,30.2\n"
            + "-70.426,43.179,urn:ioos:station:wmo:44030,-1.0,2010-05-27T01:00:00Z,urn:ioos:sensor:wmo:44030::ct1,30.5\n"
            + "-68.996,44.055,urn:ioos:station:wmo:44033,-1.0,2010-05-27T01:00:00Z,urn:ioos:sensor:wmo:44033::ct1,30.8\n"
            + "-68.112,44.103,urn:ioos:station:wmo:44034,-1.0,2010-05-27T00:00:00Z,urn:ioos:sensor:wmo:44034::ct1,31.6\n"
            + "-68.112,44.103,urn:ioos:station:wmo:44034,-1.0,2010-05-27T01:00:00Z,urn:ioos:sensor:wmo:44034::ct1,31.6\n"
            + "-148.283,60.802,urn:ioos:station:wmo:46081,-2.5,2010-05-27T00:50:00Z,urn:ioos:sensor:wmo:46081::ct1,25.24\n"; // 2021-05-14
    // was
    // -148.263,60.799
    // "-73.926,42.027,urn:ioos:station:wmo:anrn6,NaN,2010-05-27T00:45:00Z,urn:ioos:sensor:wmo:anrn6::ct1,0.1\n"
    // +
    // "-73.926,42.027,urn:ioos:station:wmo:anrn6,NaN,2010-05-27T01:00:00Z,urn:ioos:sensor:wmo:anrn6::ct1,0.1\n"
    // +
    // "-84.875,29.786,urn:ioos:station:wmo:apqf1,NaN,2010-05-27T00:15:00Z,urn:ioos:sensor:wmo:apqf1::ct1,2.2\n";
    Test.ensureEqual(
        results.substring(0, Math.min(results.length(), expected.length())),
        expected,
        "RESULTS=\n" + results);
  }

  /**
   * This is a test to determine longest allowed time request.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testNdbcSosLongTime() throws Throwable {
    // testVerboseOn();

    int language = 0;
    EDDTable eddTable = (EDDTable) EDDTestDataset.getndbcSosWTemp();
    String name, tName, results, expected, userDapQuery;

    String2.log("\n*** EDDTableFromSOS.testNdbcSosLongTime, one station .CSV data\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&station_id=\"urn:ioos:station:wmo:41012\"&time>=2002-06-01",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_LongTime",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);

    int nb = Math.min(results.length(), 500);
    String2.log(
        "\ncwwcNDBCMet indicates that 41012 has water temperature data from"
            + "\n2002-06-25T19:00:00Z to the present."
            + "\n\nstart of sos results=\n"
            + results.substring(0, nb)
            + "\n\nend of results=\n"
            + results.substring(results.length() - nb)
            + "\nnRows ~= "
            + (results.length() / 80));

    expected =
        "longitude,latitude,station_id,altitude,time,sensor_id,sea_water_temperature\n"
            + "degrees_east,degrees_north,,m,UTC,,degree_C\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-0.6,2008-07-21T20:50:00Z,urn:ioos:sensor:wmo:41012::watertemp1,28.0\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-0.6,2008-07-21T21:50:00Z,urn:ioos:sensor:wmo:41012::watertemp1,28.0\n"
            + "-80.534,30.042,urn:ioos:station:wmo:41012,-0.6,2008-07-21T22:50:00Z,urn:ioos:sensor:wmo:41012::watertemp1,28.0\n";
    Test.ensureEqual(
        results.substring(0, expected.length()), expected, "RESULTS=\n" + results.substring(0, nb));

    /*
     * 2012-04-25
     * Error from
     * https://sdf.ndbc.noaa.gov/sos/server.php?service=SOS&version=1.0.0&request=
     * GetObservation&offering=urn:ioos:station:wmo:41012&observedProperty=http://
     * mmisw.org/ont/cf/parameter/sea_water_temperature&responseFormat=text/csv&
     * eventTime=2006-07-27T21:10:00Z/2012-04-25T19:07:11Z
     * <ExceptionReport>
     * atts=xmlns="http://www.opengis.net/ows/1.1",
     * xmlns:xsi="https://www.w3.org/2001/XMLSchema-instance",
     * xsi:schemaLocation="http://www.opengis
     * .net/ows/1.1 owsExceptionReport.xsd", version="1.0.0", xml:lang="en"
     * <ExceptionReport><Exception>
     * atts=exceptionCode="InvalidParameterValue", locator="eventTime"
     * <ExceptionReport><Exception><ExceptionText>
     * <ExceptionReport><Exception></ExceptionText>
     * content=No more than thirty days of data can be requested.
     * ERROR is from
     * requestUrl=https://sdf.ndbc.noaa.gov/sos/server.php?service=SOS&version=1.0.0
     * &request=GetObservation&offering=urn:ioos:station:wmo:41012&observedProperty=
     * http://mmisw.org/ont/cf/parameter/sea_water_temperature&responseFormat=text/
     * csv&eventTime=2006-07-27T21:10:00Z/2012-04-25T19:07:11Z
     *
     * java.lang.RuntimeException: Source
     * Exception="InvalidParameterValue: eventTime: No more than thirty days of data can be requested."
     * .
     * at gov.noaa.pfel.erddap.dataset.EDDTableFromSOS.readFromIoosNdbcNos(
     * EDDTableFromSOS.java:1392)
     * at gov.noaa.pfel.erddap.dataset.EDDTableFromSOS.getDataForDapQuery(
     * EDDTableFromSOS.java:1231)
     * at
     * gov.noaa.pfel.erddap.dataset.EDDTable.respondToDapQuery(EDDTable.java:2266)
     * at gov.noaa.pfel.erddap.dataset.EDD.lowMakeFileForDapQuery(EDD.java:2354)
     * at gov.noaa.pfel.erddap.dataset.EDD.makeNewFileForDapQuery(EDD.java:2273)
     * at gov.noaa.pfel.erddap.dataset.EDDTableFromSOS.testNdbcSosLongTime(
     * EDDTableFromSOS.java:4028)
     * at
     * gov.noaa.pfel.erddap.dataset.EDDTableFromSOS.test(EDDTableFromSOS.java:7077)
     * at gov.noaa.pfel.coastwatch.TestAll.main(TestAll.java:1417)
     */
  }

  /**
   * This should work, but server is in flux so it often breaks.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testNdbcSosWLevel() throws Throwable {
    // testVerboseOn();

    int language = 0;
    EDDTable eddTable = (EDDTable) EDDTestDataset.getndbcSosWLevel();
    String name, tName, results = null, expected, userDapQuery;

    // String2.log("\n*** EDDTableFromSOS.testNdbcSosWLevel .das\n");
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard
    // to check min:sec.
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_ndbcSosWLevel",
            ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range -176.262, 178.219;\n"
            + "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float64 actual_range -46.83, 57.654;\n"
            + "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  station_id {\n"
            + "    String cf_role \"timeseries_id\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Station ID\";\n"
            + "  }\n"
            + "  altitude {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    String _CoordinateZisPositive \"up\";\n"
            + "    Float64 actual_range -8000.0, 0.0;\n"
            + "    String axis \"Z\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Altitude\";\n"
            + "    String positive \"up\";\n"
            + "    String standard_name \"altitude\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range 1.2095577e+9, NaN;\n"
            + // was a value, but many stations have "now", which is
            // propertly converted to NaN here
            "    String axis \"T\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  sensor_id {\n"
            + "    String comment \"Always check the quality_flags before using this data.\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Sensor ID\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/sea_floor_depth_below_sea_surface\";\n"
            + "  }\n"
            + "  averaging_interval {\n"
            + "    String ioos_category \"Sea Level\";\n"
            + "    String long_name \"Averaging Interval\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/sea_floor_depth_below_sea_surface\";\n"
            + "    String units \"s\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_data_type \"TimeSeries\";\n"
            + "    String cdm_timeseries_variables \"station_id, longitude, latitude, sensor_id\";\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "    Float64 Easternmost_Easting 178.219;\n"
            + "    String featureType \"TimeSeries\";\n"
            + "    Float64 geospatial_lat_max 57.654;\n"
            + "    Float64 geospatial_lat_min -46.83;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max 178.219;\n"
            + "    Float64 geospatial_lon_min -176.262;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    Float64 geospatial_vertical_max 0.0;\n"
            + "    Float64 geospatial_vertical_min -8000.0;\n"
            + "    String geospatial_vertical_positive \"up\";\n"
            + "    String geospatial_vertical_units \"m\";\n"
            + "    String history \""
            + today;
    String tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

    // + " https://sdf" + datasetIdPrefix + ".ndbc.noaa.gov/sos/server.php\n" +
    // today + " " + EDStatic.erddapUrl + //in tests, always use non-https url
    // "/tabledap/" +
    expected =
        "ndbcSosWLevel.das\";\n"
            + "    String infoUrl \"https://sdf.ndbc.noaa.gov/sos/\";\n"
            + "    String institution \"NOAA NDBC\";\n"
            + "    String keywords \"altitude, atmosphere, averaging, below, depth, Earth Science > Oceans > Bathymetry/Seafloor Topography > Bathymetry, floor, height, identifier, interval, level, ndbc, noaa, sea, sea level, sensor, sos, station, surface, time\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    Float64 Northernmost_Northing 57.654;\n"
            + "    String sourceUrl \"https://sdf.ndbc.noaa.gov/sos/server.php\";\n"
            + "    Float64 Southernmost_Northing -46.83;\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String subsetVariables \"station_id, longitude, latitude\";\n"
            + "    String summary \"The NOAA NDBC SOS server is part of the IOOS DIF SOS Project.  The stations in this dataset have sea_floor_depth_below_sea_surface data.\n"
            + "\n"
            + "Because of the nature of SOS requests, requests for data MUST include constraints for the longitude, latitude, time, and/or station_id variables.\";\n"
            + "    String time_coverage_start \"2008-04-30T12:15:00Z\";\n"
            + "    String title \"NOAA NDBC SOS, 2008-present, sea_floor_depth_below_sea_surface\";\n"
            + "    Float64 Westernmost_Easting -176.262;\n"
            + "  }\n"
            + "}\n";
    int tPo = results.indexOf(expected.substring(0, 17));
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
        expected,
        "results=\n" + results);

    // String2.log("\n*** EDDTableFromSOS.testNdbcSosWLevel test get one station
    // .CSV data\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&station_id=\"urn:ioos:station:wmo:55015\"&time>=2008-08-01T14:00&time<=2008-08-01T15:00",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_ndbcSosWLevel",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "longitude,latitude,station_id,altitude,time,sensor_id,averaging_interval\n"
            + "degrees_east,degrees_north,,m,UTC,,s\n"
            + "160.256,-46.83,urn:ioos:station:wmo:55015,-4944.303,2008-08-01T14:00:00Z,urn:ioos:sensor:wmo:55015::tsunameter0,900\n"
            + "160.256,-46.83,urn:ioos:station:wmo:55015,-4944.215,2008-08-01T14:15:00Z,urn:ioos:sensor:wmo:55015::tsunameter0,900\n"
            + "160.256,-46.83,urn:ioos:station:wmo:55015,-4944.121,2008-08-01T14:30:00Z,urn:ioos:sensor:wmo:55015::tsunameter0,900\n"
            + "160.256,-46.83,urn:ioos:station:wmo:55015,-4944.025,2008-08-01T14:45:00Z,urn:ioos:sensor:wmo:55015::tsunameter0,900\n"
            + "160.256,-46.83,urn:ioos:station:wmo:55015,-4943.93,2008-08-01T15:00:00Z,urn:ioos:sensor:wmo:55015::tsunameter0,900\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);
  }

  /**
   * This should work, but server is in flux so it often breaks.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testNdbcSosWTemp() throws Throwable {
    // testVerboseOn();

    int language = 0;
    EDDTable eddTable = (EDDTable) EDDTestDataset.getndbcSosWTemp();
    String name, tName, results = null, expected, userDapQuery;

    // String2.log("\n*** EDDTableFromSOS.testNdbcSosWTemp .das\n");
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard
    // to check min:sec.
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_ndbcSosWTemp",
            ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range -178.343, 180.0;\n"
            + "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float64 actual_range -55.0, 71.758;\n"
            + "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  station_id {\n"
            + "    String cf_role \"timeseries_id\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Station ID\";\n"
            + "  }\n"
            + "  altitude {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    String _CoordinateZisPositive \"up\";\n"
            + "    String axis \"Z\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Altitude\";\n"
            + "    String positive \"up\";\n"
            + "    String standard_name \"altitude\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range 1.1360742e+9, NaN;\n"
            + // pre 2013-11-01 was 1.1540346e+9
            "    String axis \"T\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  sensor_id {\n"
            + "    String comment \"Always check the quality_flags before using this data.\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Sensor ID\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/sea_water_temperature\";\n"
            + "  }\n"
            + "  sea_water_temperature {\n"
            + "    Float64 colorBarMaximum 32.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Temperature\";\n"
            + "    String long_name \"Sea Water Temperature\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/sea_water_temperature\";\n"
            + "    String standard_name \"sea_water_temperature\";\n"
            + "    String units \"degree_C\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_data_type \"TimeSeries\";\n"
            + "    String cdm_timeseries_variables \"station_id, longitude, latitude, sensor_id\";\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "    Float64 Easternmost_Easting 180.0;\n"
            + "    String featureType \"TimeSeries\";\n"
            + "    Float64 geospatial_lat_max 71.758;\n"
            + "    Float64 geospatial_lat_min -55.0;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max 180.0;\n"
            + "    Float64 geospatial_lon_min -178.343;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    String geospatial_vertical_positive \"up\";\n"
            + "    String geospatial_vertical_units \"m\";\n"
            + "    String history \""
            + today;
    String tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

    // + " https://sdf" + datasetIdPrefix + ".ndbc.noaa.gov/sos/server.php\n" +
    // today + " " + EDStatic.erddapUrl + //in tests, always use non-https url
    // "/tabledap/" +
    expected =
        "ndbcSosWTemp.das\";\n"
            + "    String infoUrl \"https://sdf.ndbc.noaa.gov/sos/\";\n"
            + "    String institution \"NOAA NDBC\";\n"
            + "    String keywords \"altitude, atmosphere, Earth Science > Atmosphere > Altitude > Station Height, Earth Science > Oceans > Ocean Temperature > Water Temperature, height, identifier, ndbc, noaa, ocean, oceans, sea, sea_water_temperature, seawater, sensor, sos, station, temperature, time, water\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    Float64 Northernmost_Northing 71.758;\n"
            + "    String sourceUrl \"https://sdf.ndbc.noaa.gov/sos/server.php\";\n"
            + "    Float64 Southernmost_Northing -55.0;\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String subsetVariables \"station_id, longitude, latitude\";\n"
            + "    String summary \"The NOAA NDBC SOS server is part of the IOOS DIF SOS Project.  The stations in this dataset have sea_water_temperature data.\n"
            + "\n"
            + "Because of the nature of SOS requests, requests for data MUST include constraints for the longitude, latitude, time, and/or station_id variables.\";\n"
            + "    String time_coverage_start \"2006-01-01T00:10:00Z\";\n"
            + // pre 2013-11-01 was 2006-07-27T21:10:00Z
            "    String title \"NOAA NDBC SOS, 2006-present, sea_water_temperature\";\n"
            + "    Float64 Westernmost_Easting -178.343;\n"
            + "  }\n"
            + "}\n";
    int tPo = results.indexOf(expected.substring(0, 17));
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
        expected,
        "results=\n" + results);

    String2.log("\n*** EDDTableFromSOS.testNdbcSosWTemp test get one station .CSV data\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&station_id=\"urn:ioos:station:wmo:46013\"&time>=2008-08-01T14:00&time<2008-08-01T20:00",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_ndbcSosWTemp",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected = // 2021-12-13 lon, lat, alt all changed slightly 2019-03-19 was -0.6, now -1.0
        "longitude,latitude,station_id,altitude,time,sensor_id,sea_water_temperature\n"
            + "degrees_east,degrees_north,,m,UTC,,degree_C\n"
            + "-123.303,38.253,urn:ioos:station:wmo:46013,-1.5,2008-08-01T14:50:00Z,urn:ioos:sensor:wmo:46013::watertemp1,10.9\n"
            + "-123.303,38.253,urn:ioos:station:wmo:46013,-1.5,2008-08-01T15:50:00Z,urn:ioos:sensor:wmo:46013::watertemp1,10.9\n"
            + "-123.303,38.253,urn:ioos:station:wmo:46013,-1.5,2008-08-01T16:50:00Z,urn:ioos:sensor:wmo:46013::watertemp1,10.9\n"
            + "-123.303,38.253,urn:ioos:station:wmo:46013,-1.5,2008-08-01T17:50:00Z,urn:ioos:sensor:wmo:46013::watertemp1,10.9\n"
            + "-123.303,38.253,urn:ioos:station:wmo:46013,-1.5,2008-08-01T18:50:00Z,urn:ioos:sensor:wmo:46013::watertemp1,11.0\n"
            + "-123.303,38.253,urn:ioos:station:wmo:46013,-1.5,2008-08-01T19:50:00Z,urn:ioos:sensor:wmo:46013::watertemp1,11.1\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);
  }

  /**
   * This should work, but server is in flux so it often breaks.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testNdbcSosBig() throws Throwable {
    // no, I want it to run fast: testVerboseOn();
    // reallyVerbose = false;

    int language = 0;
    EDDTable eddTable = (EDDTable) EDDTestDataset.getndbcSosWTemp();
    String name, tName, results, expected, query;

    // String2.log("\n*** EDDTableFromSOS.testNdbcSosBig test get one station .CSV
    // data\n");
    query = "&station_id=\"urn:ioos:station:wmo:46013\""; // for all time
    name = eddTable.className() + "_ndbcSosWTemp";

    EDDTableFromSOS.timeParts = true;
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, query, EDStatic.fullTestCacheDirectory, name, ".csv");

    EDDTableFromSOS.timeParts = false;
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, query, EDStatic.fullTestCacheDirectory, name, ".csv");

    EDDTableFromSOS.timeParts = true;
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, query, EDStatic.fullTestCacheDirectory, name, ".csv");

    EDDTableFromSOS.timeParts = false;
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, query, EDStatic.fullTestCacheDirectory, name, ".csv");

    EDDTableFromSOS.timeParts = false;
  }

  /**
   * This should work, but server is in flux so it often breaks.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testNdbcSosWaves() throws Throwable {

    // testVerboseOn();
    int language = 0;
    EDDTable eddTable = (EDDTable) EDDTestDataset.getndbcSosWaves();
    String name, tName, results = null, expected = null, userDapQuery;

    // String2.log("\n*** EDDTableFromSOS.testNdbcSosWaves .das\n");
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard
    // to check min:sec.
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_ndbcSosWaves",
            ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range -177.75, 179.0;\n"
            + "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float64 actual_range -19.713, 71.758;\n"
            + // 2010-10-10 was 60.8
            "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  station_id {\n"
            + "    String cf_role \"timeseries_id\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Station ID\";\n"
            + "  }\n"
            + "  altitude {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    String _CoordinateZisPositive \"up\";\n"
            + "    String axis \"Z\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Altitude\";\n"
            + "    String positive \"up\";\n"
            + "    String standard_name \"altitude\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range 1.1939616e+9, NaN;\n"
            + "    String axis \"T\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  sensor_id {\n"
            + "    String comment \"Always check the quality_flags before using this data.\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Sensor ID\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n"
            + "  }\n"
            + "  sea_surface_wave_significant_height {\n"
            + "    String ioos_category \"Surface Waves\";\n"
            + "    String long_name \"Sea Surface Wave Significant Height\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n"
            + "    String standard_name \"sea_surface_wave_significant_height\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  sea_surface_wave_peak_period {\n"
            + "    String ioos_category \"Surface Waves\";\n"
            + "    String long_name \"Sea Surface Wave Peak Period\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n"
            + "    String standard_name \"sea_surface_wave_peak_period\";\n"
            + "    String units \"s\";\n"
            + "  }\n"
            + "  sea_surface_wave_mean_period {\n"
            + "    String ioos_category \"Surface Waves\";\n"
            + "    String long_name \"Sea Surface Wave Mean Period\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n"
            + "    String standard_name \"sea_surface_wave_mean_period\";\n"
            + "    String units \"s\";\n"
            + "  }\n"
            + "  sea_surface_swell_wave_significant_height {\n"
            + "    Float64 colorBarMaximum 10.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Surface Waves\";\n"
            + "    String long_name \"Sea Surface Swell Wave Significant Height\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n"
            + "    String standard_name \"sea_surface_swell_wave_significant_height\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  sea_surface_swell_wave_period {\n"
            + "    Float64 colorBarMaximum 20.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Surface Waves\";\n"
            + "    String long_name \"Sea Surface Swell Wave Period\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n"
            + "    String standard_name \"sea_surface_swell_wave_period\";\n"
            + "    String units \"s\";\n"
            + "  }\n"
            + "  sea_surface_wind_wave_significant_height {\n"
            + "    String ioos_category \"Surface Waves\";\n"
            + "    String long_name \"Sea Surface Wind Wave Significant Height\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n"
            + "    String standard_name \"sea_surface_wind_wave_significant_height\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  sea_surface_wind_wave_period {\n"
            + "    String ioos_category \"Surface Waves\";\n"
            + "    String long_name \"Sea Surface Wind Wave Period\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n"
            + "    String standard_name \"sea_surface_wind_wave_period\";\n"
            + "    String units \"s\";\n"
            + "  }\n"
            + "  sea_water_temperature {\n"
            + "    Float64 colorBarMaximum 32.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Temperature\";\n"
            + "    String long_name \"Sea Water Temperature\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n"
            + "    String standard_name \"sea_water_temperature\";\n"
            + "    String units \"degree_C\";\n"
            + "  }\n"
            + "  sea_surface_wave_to_direction {\n"
            + "    Float64 colorBarMaximum 360.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Surface Waves\";\n"
            + "    String long_name \"Sea Surface Wave To Direction\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n"
            + "    String standard_name \"sea_surface_wave_to_direction\";\n"
            + "    String units \"degrees_true\";\n"
            + "  }\n"
            + "  sea_surface_swell_wave_to_direction {\n"
            + "    Float64 colorBarMaximum 360.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Surface Waves\";\n"
            + "    String long_name \"Sea Surface Swell Wave To Direction\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n"
            + "    String standard_name \"sea_surface_swell_wave_to_direction\";\n"
            + "    String units \"degrees_true\";\n"
            + "  }\n"
            + "  sea_surface_wind_wave_to_direction {\n"
            + "    Float64 colorBarMaximum 360.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Surface Waves\";\n"
            + "    String long_name \"Sea Surface Wind Wave To Direction\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n"
            + "    String standard_name \"sea_surface_wind_wave_to_direction\";\n"
            + "    String units \"degrees_true\";\n"
            + "  }\n"
            + "  number_of_frequencies {\n"
            + "    String ioos_category \"Surface Waves\";\n"
            + "    String long_name \"Number Of Frequencies\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n"
            + "    String units \"count\";\n"
            + "  }\n"
            + "  center_frequencies {\n"
            + "    String ioos_category \"Surface Waves\";\n"
            + "    String long_name \"Center Frequencies\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n"
            + "    String units \"Hz\";\n"
            + "  }\n"
            + "  bandwidths {\n"
            + "    String ioos_category \"Surface Waves\";\n"
            + "    String long_name \"Bandwidths\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n"
            + "    String units \"Hz\";\n"
            + "  }\n"
            + "  spectral_energy {\n"
            + "    String ioos_category \"Surface Waves\";\n"
            + "    String long_name \"Spectral Energy\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n"
            + "    String units \"m^2/Hz\";\n"
            + "  }\n"
            + "  mean_wave_direction {\n"
            + "    Float64 colorBarMaximum 360.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Surface Waves\";\n"
            + "    String long_name \"Mean Wave Direction\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n"
            + "    String standard_name \"mean_wave_direction\";\n"
            + "    String units \"degrees_true\";\n"
            + "  }\n"
            + "  principal_wave_direction {\n"
            + "    Float64 colorBarMaximum 360.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Surface Waves\";\n"
            + "    String long_name \"Principal Wave Direction\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n"
            + "    String units \"degrees_true\";\n"
            + "  }\n"
            + "  polar_coordinate_r1 {\n"
            + "    String ioos_category \"Surface Waves\";\n"
            + "    String long_name \"Polar Coordinate R1\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n"
            + "    String units \"1\";\n"
            + "  }\n"
            + "  polar_coordinate_r2 {\n"
            + "    String ioos_category \"Surface Waves\";\n"
            + "    String long_name \"Polar Coordinate R2\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n"
            + "    String units \"1\";\n"
            + "  }\n"
            + "  calculation_method {\n"
            + "    String ioos_category \"Surface Waves\";\n"
            + "    String long_name \"Calculation Method\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n"
            + "  }\n"
            + "  sampling_rate {\n"
            + "    String ioos_category \"Surface Waves\";\n"
            + "    String long_name \"Sampling Rate\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n"
            + "    String units \"Hz\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_data_type \"TimeSeries\";\n"
            + "    String cdm_timeseries_variables \"station_id, longitude, latitude, sensor_id\";\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "    Float64 Easternmost_Easting 179.0;\n"
            + "    String featureType \"TimeSeries\";\n"
            + "    Float64 geospatial_lat_max 71.758;\n"
            + "    Float64 geospatial_lat_min -19.713;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max 179.0;\n"
            + "    Float64 geospatial_lon_min -177.75;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    String geospatial_vertical_positive \"up\";\n"
            + "    String geospatial_vertical_units \"m\";\n"
            + "    String history \""
            + today;
    String tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

    // + " https://sdf" + datasetIdPrefix + ".ndbc.noaa.gov/sos/server.php\n" +
    // today + " " + EDStatic.erddapUrl + //in tests, always use non-https url
    // "/tabledap/" +
    expected =
        "ndbcSosWaves.das\";\n"
            + "    String infoUrl \"https://sdf.ndbc.noaa.gov/sos/\";\n"
            + "    String institution \"NOAA NDBC\";\n"
            + "    String keywords \"Atmosphere > Altitude > Station Height,\n"
            + "Oceans > Ocean Temperature > Water Temperature,\n"
            + "Oceans > Ocean Waves > Significant Wave Height,\n"
            + "Oceans > Ocean Waves > Swells,\n"
            + "Oceans > Ocean Waves > Wave Frequency,\n"
            + "Oceans > Ocean Waves > Wave Period,\n"
            + "Oceans > Ocean Waves > Wave Speed/Direction,\n"
            + "Oceans > Ocean Waves > Wind Waves,\n"
            + "altitude, atmosphere, bandwidths, calculation, center, coordinate, direction, energy, frequencies, height, identifier, mean, mean_wave_direction, method, ndbc, noaa, number, ocean, oceans, peak, period, polar, principal, rate, sampling, sea, sea_surface_swell_wave_period, sea_surface_swell_wave_significant_height, sea_surface_swell_wave_to_direction, sea_surface_wave_mean_period, sea_surface_wave_peak_period, sea_surface_wave_significant_height, sea_surface_wave_to_direction, sea_surface_wind_wave_period, sea_surface_wind_wave_significant_height, sea_surface_wind_wave_to_direction, sea_water_temperature, seawater, sensor, significant, sos, spectral, speed, station, surface, surface waves, swell, swells, temperature, time, water, wave, waves, wind\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    Float64 Northernmost_Northing 71.758;\n"
            + "    String sourceUrl \"https://sdf.ndbc.noaa.gov/sos/server.php\";\n"
            + "    Float64 Southernmost_Northing -19.713;\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String subsetVariables \"station_id, longitude, latitude\";\n"
            + "    String summary \"The NOAA NDBC SOS server is part of the IOOS DIF SOS Project.  The stations in this dataset have waves data.\n"
            + "\n"
            + "Because of the nature of SOS requests, requests for data MUST include constraints for the longitude, latitude, time, and/or station_id variables.\";\n"
            + "    String time_coverage_start \"2007-11-02T00:00:00Z\";\n"
            + "    String title \"NOAA NDBC SOS - waves\";\n"
            + "    Float64 Westernmost_Easting -177.75;\n"
            + "  }\n"
            + "}\n";
    int tPo = results.indexOf(expected.substring(0, 17));
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
        expected,
        "results=\n" + results);

    // test station= and superfluous string data > < constraints
    String2.log("\n*** EDDTableFromSOS.testNdbcSosWaves test get one station .CSV data\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&station_id=\"urn:ioos:station:wmo:46013\"&time>=2008-08-01T14&time<=2008-08-01T17"
                + "&calculation_method>=\"A\"&calculation_method<=\"Lonh\"",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_ndbcSosWaves",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        // was
        // "longitude,latitude,station_id,altitude,time,SignificantWaveHeight,DominantWavePeriod,AverageWavePeriod,SwellHeight,SwellPeriod,WindWaveHeight,WindWavePeriod,WaterTemperature,WaveDuration,CalculationMethod,SamplingRate,NumberOfFrequencies,CenterFrequencies,Bandwidths,SpectralEnergy,MeanWaveDirectionPeakPeriod,SwellWaveDirection,WindWaveDirection,MeanWaveDirection,PrincipalWaveDirection,PolarCoordinateR1,PolarCoordinateR2,DirectionalWaveParameter,FourierCoefficientA1,FourierCoefficientA2,FourierCoefficientB1,FourierCoefficientB2\n"
        // +
        // "degrees_east,degrees_north,,m,UTC,m,s,s,m,s,m,s,degree_C,s,,Hz,count,Hz,Hz,m2
        // Hz-1,degrees_true,degrees_true,degrees_true,degrees_true,degrees_true,degrees_true,degrees_true,degrees_true,m,m,m,m\n"
        // +
        //// before 2008-10-29 (last week?),I think the DirectionalWaveParameters were
        // different
        //// it changed again (~11am) vs earlier today (~9am) 2008-10-29
        //// see email to jeffDlB 2008-10-29
        // "-123.32,38.23,urn:ioos:station:wmo:46013,NaN,2008-08-01T14:50:00Z,1.62,14.81,5.21,1.12,14.8,1.17,4.3,NaN,NaN,Longuet-Higgins
        // (1964),NaN,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.779157;0.372876;2.02274;0.714328;0.675131;0.296029;0.138154;0.605274;1.96737;1.16217;0.884235;0.462599;0.57436;0.504724;0.218129;0.38115;0.504237;0.45285;0.708456;0.626689;0.747685;0.883292;0.632856;0.448383;0.331531;0.123811;0.265022;0.214203;0.208534;0.21145;0.223251;0.114582;0.10544;0.130131;0.118191;0.0652535;0.0604571;0.0167055;0.0158453;0.00866108;0.00483522,176.0,176.0,312.0,158.0;336.0;188.0;11.0;78.0;263.0;189.0;176.0;196.0;212.0;249.0;182.0;267.0;292.0;299.0;306.0;290.0;299.0;304.0;294.0;301.0;304.0;320.0;314.0;311.0;303.0;312.0;317.0;315.0;307.0;316.0;314.0;305.0;317.0;306.0;311.0;310.0;303.0;294.0;308.0;298.0;302.0;303.0;113.0;127.0;113.0,183.0;8.0;181.0;7.0;3.0;301.0;275.0;134.0;123.0;262.0;279.0;92.0;281.0;293.0;299.0;308.0;292.0;306.0;310.0;300.0;304.0;306.0;326.0;317.0;310.0;303.0;312.0;318.0;316.0;309.0;319.0;318.0;306.0;319.0;307.0;313.0;309.0;305.0;286.0;309.0;271.0;296.0;254.0;111.0;99.0;92.0,0.212213;0.112507;0.304966;0.35902;0.254397;0.488626;0.263791;0.515939;0.462758;0.430386;0.497566;0.12097;0.497566;0.653071;0.826652;0.841777;0.702193;0.768824;0.797214;0.741445;0.797214;0.797214;0.826652;0.841777;0.857178;0.841777;0.938514;0.921652;0.905092;0.8118;0.826652;0.826652;0.78289;0.872861;0.905092;0.857178;0.88883;0.841777;0.768824;0.857178;0.677187;0.826652;0.629814;0.797214;0.677187;0.715041,0.768824;0.78289;0.826652;0.497566;0.220049;0.263791;0.488626;0.372277;0.125437;0.386024;0.304966;0.278536;0.310546;0.365588;0.653071;0.66502;0.607386;0.488626;0.525379;0.430386;0.462758;0.446279;0.585756;0.544779;0.564896;0.534991;0.78289;0.768824;0.66502;0.43826;0.525379;0.462758;0.379088;0.715041;0.689577;0.585756;0.641337;0.544779;0.333904;0.554746;0.137339;0.564896;0.134872;0.372277;0.108501;0.340013,,,,,\n"
        // +
        ////
        // "-123.32,38.23,urn:ioos:station:wmo:46013,NaN,2008-08-01T14:50:00Z,1.62,14.81,5.21,1.12,14.8,1.17,4.3,NaN,NaN,Longuet-Higgins
        // (1964),NaN,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.779157;0.372876;2.02274;0.714328;0.675131;0.296029;0.138154;0.605274;1.96737;1.16217;0.884235;0.462599;0.57436;0.504724;0.218129;0.38115;0.504237;0.45285;0.708456;0.626689;0.747685;0.883292;0.632856;0.448383;0.331531;0.123811;0.265022;0.214203;0.208534;0.21145;0.223251;0.114582;0.10544;0.130131;0.118191;0.0652535;0.0604571;0.0167055;0.0158453;0.00866108;0.00483522,176.0,176.0,312.0,158.0;336.0;188.0;11.0;78.0;263.0;189.0;176.0;196.0;212.0;249.0;182.0;267.0;292.0;299.0;306.0;290.0;299.0;304.0;294.0;301.0;304.0;320.0;314.0;311.0;303.0;312.0;317.0;315.0;307.0;316.0;314.0;305.0;317.0;306.0;311.0;310.0;303.0;294.0;308.0;298.0;302.0;303.0;113.0;127.0;113.0,0.2;0.1;0.3;0.4;0.3;0.5;0.3;0.5;0.5;0.4;0.5;0.1;0.5;0.7;0.8;0.8;0.7;0.8;0.8;0.7;0.8;0.8;0.8;0.8;0.9;0.8;0.9;0.9;0.9;0.8;0.8;0.8;0.8;0.9;0.9;0.9;0.9;0.8;0.8;0.9;0.7;0.8;0.6;0.8;0.7;0.7,0.212213;0.112507;0.304966;0.35902;0.254397;0.488626;0.263791;0.515939;0.462758;0.430386;0.497566;0.12097;0.497566;0.653071;0.826652;0.841777;0.702193;0.768824;0.797214;0.741445;0.797214;0.797214;0.826652;0.841777;0.857178;0.841777;0.938514;0.921652;0.905092;0.8118;0.826652;0.826652;0.78289;0.872861;0.905092;0.857178;0.88883;0.841777;0.768824;0.857178;0.677187;0.826652;0.629814;0.797214;0.677187;0.715041,0.768824;0.78289;0.826652;0.497566;0.220049;0.263791;0.488626;0.372277;0.125437;0.386024;0.304966;0.278536;0.310546;0.365588;0.653071;0.66502;0.607386;0.488626;0.525379;0.430386;0.462758;0.446279;0.585756;0.544779;0.564896;0.534991;0.78289;0.768824;0.66502;0.43826;0.525379;0.462758;0.379088;0.715041;0.689577;0.585756;0.641337;0.544779;0.333904;0.554746;0.137339;0.564896;0.134872;0.372277;0.108501;0.340013,,,,,\n"
        // +
        ////
        // "-123.32,38.23,urn:ioos:station:wmo:46013,NaN,2008-08-01T14:50:00Z,1.62,14.81,5.21,1.12,14.8,1.17,4.3,NaN,NaN,Longuet-Higgins
        // (1964),NaN,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.779157;0.372876;2.02274;0.714328;0.675131;0.296029;0.138154;0.605274;1.96737;1.16217;0.884235;0.462599;0.57436;0.504724;0.218129;0.38115;0.504237;0.45285;0.708456;0.626689;0.747685;0.883292;0.632856;0.448383;0.331531;0.123811;0.265022;0.214203;0.208534;0.21145;0.223251;0.114582;0.10544;0.130131;0.118191;0.0652535;0.0604571;0.0167055;0.0158453;0.00866108;0.00483522,176.0,176.0,312.0,158.0;336.0;188.0;11.0;78.0;263.0;189.0;176.0;196.0;212.0;249.0;182.0;267.0;292.0;299.0;306.0;290.0;299.0;304.0;294.0;301.0;304.0;320.0;314.0;311.0;303.0;312.0;317.0;315.0;307.0;316.0;314.0;305.0;317.0;306.0;311.0;310.0;303.0;294.0;308.0;298.0;302.0;303.0;113.0;127.0;113.0,0.2;0.1;0.3;0.4;0.3;0.5;0.3;0.5;0.5;0.4;0.5;0.1;0.5;0.7;0.8;0.8;0.7;0.8;0.8;0.7;0.8;0.8;0.8;0.8;0.9;0.8;0.9;0.9;0.9;0.8;0.8;0.8;0.8;0.9;0.9;0.9;0.9;0.8;0.8;0.9;0.7;0.8;0.6;0.8;0.7;0.7,0.212213;0.112507;0.304966;0.35902;0.254397;0.488626;0.263791;0.515939;0.462758;0.430386;0.497566;0.12097;0.497566;0.653071;0.826652;0.841777;0.702193;0.768824;0.797214;0.741445;0.797214;0.797214;0.826652;0.841777;0.857178;0.841777;0.938514;0.921652;0.905092;0.8118;0.826652;0.826652;0.78289;0.872861;0.905092;0.857178;0.88883;0.841777;0.768824;0.857178;0.677187;0.826652;0.629814;0.797214;0.677187;0.715041,0.212213;0.112507;0.304966;0.35902;0.254397;0.488626;0.263791;0.515939;0.462758;0.430386;0.497566;0.12097;0.497566;0.653071;0.826652;0.841777;0.702193;0.768824;0.797214;0.741445;0.797214;0.797214;0.826652;0.841777;0.857178;0.841777;0.938514;0.921652;0.905092;0.8118;0.826652;0.826652;0.78289;0.872861;0.905092;0.857178;0.88883;0.841777;0.768824;0.857178;0.677187;0.826652;0.629814;0.797214;0.677187;0.715041,,,,,\n"
        // +
        // "-123.32,38.23,urn:ioos:station:wmo:46013,NaN,2008-08-01T15:50:00Z,1.52,9.09,4.98,1.0,9.1,1.15,5.6,NaN,NaN,Longuet-Higgins
        // (1964),NaN,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.244172;0.874391;0.950049;0.992051;0.292122;0.416385;0.116264;0.32567;1.0886;1.49577;0.707195;0.412901;0.383;0.336784;0.162325;0.507266;0.721374;0.521185;0.317616;0.580232;0.620904;0.720338;0.544952;0.400361;0.457406;0.340211;0.190368;0.295531;0.258054;0.13138;0.178793;0.207494;0.162191;0.0901461;0.101774;0.0468724;0.036226;0.0442694;0.0218615;0.0143249;0.00447678,291.0,291.0,297.0,245.0;180.0;16.0;1.0;157.0;253.0;192.0;221.0;234.0;193.0;171.0;182.0;331.0;297.0;291.0;287.0;304.0;296.0;295.0;307.0;306.0;297.0;309.0;309.0;310.0;301.0;321.0;300.0;296.0;315.0;295.0;305.0;311.0;311.0;312.0;312.0;311.0;309.0;305.0;310.0;317.0;304.0;303.0;125.0;132.0;122.0,179.0;190.0;1.0;1.0;176.0;292.0;110.0;311.0;321.0;118.0;143.0;131.0;335.0;305.0;296.0;290.0;313.0;310.0;298.0;321.0;306.0;298.0;308.0;308.0;310.0;301.0;323.0;300.0;294.0;320.0;295.0;308.0;312.0;313.0;315.0;311.0;311.0;310.0;305.0;311.0;316.0;309.0;298.0;121.0;132.0;109.0,0.153123;0.340013;0.288822;0.254397;0.372277;0.379088;0.400278;0.430386;0.299487;0.14767;0.228175;0.393087;0.534991;0.741445;0.78289;0.857178;0.702193;0.741445;0.797214;0.689577;0.857178;0.921652;0.905092;0.768824;0.905092;0.88883;0.921652;0.88883;0.872861;0.872861;0.872861;0.872861;0.921652;0.88883;0.857178;0.857178;0.88883;0.857178;0.78289;0.797214;0.728123;0.741445;0.826652;0.8118;0.872861;0.728123,0.768824;0.575231;0.564896;0.689577;0.575231;0.497566;0.150371;0.259051;0.259051;0.728123;0.400278;0.393087;0.728123;0.689577;0.66502;0.677187;0.607386;0.400278;0.575231;0.534991;0.677187;0.8118;0.75501;0.365588;0.741445;0.741445;0.8118;0.689577;0.653071;0.653071;0.629814;0.618498;0.768824;0.689577;0.585756;0.596473;0.66502;0.629814;0.454444;0.415059;0.283632;0.216095;0.488626;0.488626;0.618498;0.236601,,,,,\n"
        // +
        ////
        // "-123.32,38.23,urn:ioos:station:wmo:46013,NaN,2008-08-01T15:50:00Z,1.52,9.09,4.98,1.0,9.1,1.15,5.6,NaN,NaN,Longuet-Higgins
        // (1964),NaN,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.244172;0.874391;0.950049;0.992051;0.292122;0.416385;0.116264;0.32567;1.0886;1.49577;0.707195;0.412901;0.383;0.336784;0.162325;0.507266;0.721374;0.521185;0.317616;0.580232;0.620904;0.720338;0.544952;0.400361;0.457406;0.340211;0.190368;0.295531;0.258054;0.13138;0.178793;0.207494;0.162191;0.0901461;0.101774;0.0468724;0.036226;0.0442694;0.0218615;0.0143249;0.00447678,291.0,291.0,297.0,245.0;180.0;16.0;1.0;157.0;253.0;192.0;221.0;234.0;193.0;171.0;182.0;331.0;297.0;291.0;287.0;304.0;296.0;295.0;307.0;306.0;297.0;309.0;309.0;310.0;301.0;321.0;300.0;296.0;315.0;295.0;305.0;311.0;311.0;312.0;312.0;311.0;309.0;305.0;310.0;317.0;304.0;303.0;125.0;132.0;122.0,0.2;0.3;0.3;0.3;0.4;0.4;0.4;0.4;0.3;0.1;0.2;0.4;0.5;0.7;0.8;0.9;0.7;0.7;0.8;0.7;0.9;0.9;0.9;0.8;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.8;0.8;0.7;0.7;0.8;0.8;0.9;0.7,0.153123;0.340013;0.288822;0.254397;0.372277;0.379088;0.400278;0.430386;0.299487;0.14767;0.228175;0.393087;0.534991;0.741445;0.78289;0.857178;0.702193;0.741445;0.797214;0.689577;0.857178;0.921652;0.905092;0.768824;0.905092;0.88883;0.921652;0.88883;0.872861;0.872861;0.872861;0.872861;0.921652;0.88883;0.857178;0.857178;0.88883;0.857178;0.78289;0.797214;0.728123;0.741445;0.826652;0.8118;0.872861;0.728123,0.768824;0.575231;0.564896;0.689577;0.575231;0.497566;0.150371;0.259051;0.259051;0.728123;0.400278;0.393087;0.728123;0.689577;0.66502;0.677187;0.607386;0.400278;0.575231;0.534991;0.677187;0.8118;0.75501;0.365588;0.741445;0.741445;0.8118;0.689577;0.653071;0.653071;0.629814;0.618498;0.768824;0.689577;0.585756;0.596473;0.66502;0.629814;0.454444;0.415059;0.283632;0.216095;0.488626;0.488626;0.618498;0.236601,,,,,\n"
        // +
        ////
        // "-123.32,38.23,urn:ioos:station:wmo:46013,NaN,2008-08-01T15:50:00Z,1.52,9.09,4.98,1.0,9.1,1.15,5.6,NaN,NaN,Longuet-Higgins
        // (1964),NaN,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.244172;0.874391;0.950049;0.992051;0.292122;0.416385;0.116264;0.32567;1.0886;1.49577;0.707195;0.412901;0.383;0.336784;0.162325;0.507266;0.721374;0.521185;0.317616;0.580232;0.620904;0.720338;0.544952;0.400361;0.457406;0.340211;0.190368;0.295531;0.258054;0.13138;0.178793;0.207494;0.162191;0.0901461;0.101774;0.0468724;0.036226;0.0442694;0.0218615;0.0143249;0.00447678,291.0,291.0,297.0,245.0;180.0;16.0;1.0;157.0;253.0;192.0;221.0;234.0;193.0;171.0;182.0;331.0;297.0;291.0;287.0;304.0;296.0;295.0;307.0;306.0;297.0;309.0;309.0;310.0;301.0;321.0;300.0;296.0;315.0;295.0;305.0;311.0;311.0;312.0;312.0;311.0;309.0;305.0;310.0;317.0;304.0;303.0;125.0;132.0;122.0,0.2;0.3;0.3;0.3;0.4;0.4;0.4;0.4;0.3;0.1;0.2;0.4;0.5;0.7;0.8;0.9;0.7;0.7;0.8;0.7;0.9;0.9;0.9;0.8;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.8;0.8;0.7;0.7;0.8;0.8;0.9;0.7,0.153123;0.340013;0.288822;0.254397;0.372277;0.379088;0.400278;0.430386;0.299487;0.14767;0.228175;0.393087;0.534991;0.741445;0.78289;0.857178;0.702193;0.741445;0.797214;0.689577;0.857178;0.921652;0.905092;0.768824;0.905092;0.88883;0.921652;0.88883;0.872861;0.872861;0.872861;0.872861;0.921652;0.88883;0.857178;0.857178;0.88883;0.857178;0.78289;0.797214;0.728123;0.741445;0.826652;0.8118;0.872861;0.728123,0.153123;0.340013;0.288822;0.254397;0.372277;0.379088;0.400278;0.430386;0.299487;0.14767;0.228175;0.393087;0.534991;0.741445;0.78289;0.857178;0.702193;0.741445;0.797214;0.689577;0.857178;0.921652;0.905092;0.768824;0.905092;0.88883;0.921652;0.88883;0.872861;0.872861;0.872861;0.872861;0.921652;0.88883;0.857178;0.857178;0.88883;0.857178;0.78289;0.797214;0.728123;0.741445;0.826652;0.8118;0.872861;0.728123,,,,,\n"
        // +
        // "-123.32,38.23,urn:ioos:station:wmo:46013,NaN,2008-08-01T16:50:00Z,1.49,14.81,5.11,1.01,14.8,1.1,4.8,NaN,NaN,Longuet-Higgins
        // (1964),NaN,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.724702;0.909481;2.34661;0.698133;0.516662;0.499779;0.284884;0.407779;0.94326;1.08406;0.29313;0.464502;0.346171;0.393304;0.327266;0.531525;0.423195;0.328752;0.332852;0.702979;0.627516;0.379029;0.603016;0.337529;0.385623;0.308393;0.266641;0.207837;0.0681764;0.212742;0.18737;0.138199;0.122643;0.130927;0.0889706;0.0656523;0.0608267;0.0359928;0.0115031;0.0100742;0.00469153,175.0,175.0,309.0,287.0;208.0;76.0;353.0;123.0;193.0;205.0;175.0;198.0;155.0;196.0;246.0;285.0;304.0;297.0;324.0;298.0;296.0;299.0;303.0;299.0;298.0;304.0;306.0;309.0;304.0;311.0;299.0;317.0;301.0;308.0;314.0;314.0;325.0;315.0;301.0;312.0;322.0;306.0;305.0;324.0;302.0;326.0;119.0;139.0;137.0,350.0;193.0;12.0;13.0;171.0;135.0;151.0;161.0;162.0;158.0;143.0;301.0;313.0;303.0;304.0;321.0;320.0;303.0;302.0;306.0;299.0;300.0;307.0;305.0;311.0;302.0;316.0;299.0;317.0;299.0;308.0;317.0;320.0;346.0;313.0;304.0;312.0;327.0;305.0;306.0;331.0;299.0;333.0;115.0;139.0;143.0,0.177024;0.224075;0.333904;0.393087;0.273532;0.310546;0.299487;0.534991;0.506669;0.43826;0.249826;0.327905;0.340013;0.8118;0.78289;0.585756;0.653071;0.741445;0.826652;0.826652;0.857178;0.797214;0.841777;0.857178;0.905092;0.88883;0.872861;0.921652;0.905092;0.88883;0.872861;0.872861;0.8118;0.728123;0.872861;0.905092;0.857178;0.8118;0.872861;0.826652;0.841777;0.826652;0.857178;0.78289;0.797214;0.8118,0.596473;0.544779;0.488626;0.228175;0.316228;0.506669;0.479847;0.415059;0.372277;0.236601;0.228175;0.346234;0.479847;0.66502;0.534991;0.534991;0.288822;0.554746;0.728123;0.641337;0.728123;0.525379;0.653071;0.575231;0.768824;0.702193;0.618498;0.741445;0.741445;0.689577;0.629814;0.618498;0.430386;0.400278;0.629814;0.75501;0.629814;0.446279;0.641337;0.488626;0.585756;0.454444;0.618498;0.340013;0.454444;0.422653,,,,,\n";
        ////
        // "-123.32,38.23,urn:ioos:station:wmo:46013,NaN,2008-08-01T16:50:00Z,1.49,14.81,5.11,1.01,14.8,1.1,4.8,NaN,NaN,Longuet-Higgins
        // (1964),NaN,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.724702;0.909481;2.34661;0.698133;0.516662;0.499779;0.284884;0.407779;0.94326;1.08406;0.29313;0.464502;0.346171;0.393304;0.327266;0.531525;0.423195;0.328752;0.332852;0.702979;0.627516;0.379029;0.603016;0.337529;0.385623;0.308393;0.266641;0.207837;0.0681764;0.212742;0.18737;0.138199;0.122643;0.130927;0.0889706;0.0656523;0.0608267;0.0359928;0.0115031;0.0100742;0.00469153,175.0,175.0,309.0,287.0;208.0;76.0;353.0;123.0;193.0;205.0;175.0;198.0;155.0;196.0;246.0;285.0;304.0;297.0;324.0;298.0;296.0;299.0;303.0;299.0;298.0;304.0;306.0;309.0;304.0;311.0;299.0;317.0;301.0;308.0;314.0;314.0;325.0;315.0;301.0;312.0;322.0;306.0;305.0;324.0;302.0;326.0;119.0;139.0;137.0,0.2;0.2;0.3;0.4;0.3;0.3;0.3;0.5;0.5;0.4;0.2;0.3;0.3;0.8;0.8;0.6;0.7;0.7;0.8;0.8;0.9;0.8;0.8;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.8;0.7;0.9;0.9;0.9;0.8;0.9;0.8;0.8;0.8;0.9;0.8;0.8;0.8,0.177024;0.224075;0.333904;0.393087;0.273532;0.310546;0.299487;0.534991;0.506669;0.43826;0.249826;0.327905;0.340013;0.8118;0.78289;0.585756;0.653071;0.741445;0.826652;0.826652;0.857178;0.797214;0.841777;0.857178;0.905092;0.88883;0.872861;0.921652;0.905092;0.88883;0.872861;0.872861;0.8118;0.728123;0.872861;0.905092;0.857178;0.8118;0.872861;0.826652;0.841777;0.826652;0.857178;0.78289;0.797214;0.8118,0.596473;0.544779;0.488626;0.228175;0.316228;0.506669;0.479847;0.415059;0.372277;0.236601;0.228175;0.346234;0.479847;0.66502;0.534991;0.534991;0.288822;0.554746;0.728123;0.641337;0.728123;0.525379;0.653071;0.575231;0.768824;0.702193;0.618498;0.741445;0.741445;0.689577;0.629814;0.618498;0.430386;0.400278;0.629814;0.75501;0.629814;0.446279;0.641337;0.488626;0.585756;0.454444;0.618498;0.340013;0.454444;0.422653,,,,,\n";
        ////
        // "-123.32,38.23,urn:ioos:station:wmo:46013,NaN,2008-08-01T16:50:00Z,1.49,14.81,5.11,1.01,14.8,1.1,4.8,NaN,NaN,Longuet-Higgins
        // (1964),NaN,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.724702;0.909481;2.34661;0.698133;0.516662;0.499779;0.284884;0.407779;0.94326;1.08406;0.29313;0.464502;0.346171;0.393304;0.327266;0.531525;0.423195;0.328752;0.332852;0.702979;0.627516;0.379029;0.603016;0.337529;0.385623;0.308393;0.266641;0.207837;0.0681764;0.212742;0.18737;0.138199;0.122643;0.130927;0.0889706;0.0656523;0.0608267;0.0359928;0.0115031;0.0100742;0.00469153,175.0,175.0,309.0,287.0;208.0;76.0;353.0;123.0;193.0;205.0;175.0;198.0;155.0;196.0;246.0;285.0;304.0;297.0;324.0;298.0;296.0;299.0;303.0;299.0;298.0;304.0;306.0;309.0;304.0;311.0;299.0;317.0;301.0;308.0;314.0;314.0;325.0;315.0;301.0;312.0;322.0;306.0;305.0;324.0;302.0;326.0;119.0;139.0;137.0,0.2;0.2;0.3;0.4;0.3;0.3;0.3;0.5;0.5;0.4;0.2;0.3;0.3;0.8;0.8;0.6;0.7;0.7;0.8;0.8;0.9;0.8;0.8;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.8;0.7;0.9;0.9;0.9;0.8;0.9;0.8;0.8;0.8;0.9;0.8;0.8;0.8,0.177024;0.224075;0.333904;0.393087;0.273532;0.310546;0.299487;0.534991;0.506669;0.43826;0.249826;0.327905;0.340013;0.8118;0.78289;0.585756;0.653071;0.741445;0.826652;0.826652;0.857178;0.797214;0.841777;0.857178;0.905092;0.88883;0.872861;0.921652;0.905092;0.88883;0.872861;0.872861;0.8118;0.728123;0.872861;0.905092;0.857178;0.8118;0.872861;0.826652;0.841777;0.826652;0.857178;0.78289;0.797214;0.8118,0.177024;0.224075;0.333904;0.393087;0.273532;0.310546;0.299487;0.534991;0.506669;0.43826;0.249826;0.327905;0.340013;0.8118;0.78289;0.585756;0.653071;0.741445;0.826652;0.826652;0.857178;0.797214;0.841777;0.857178;0.905092;0.88883;0.872861;0.921652;0.905092;0.88883;0.872861;0.872861;0.8118;0.728123;0.872861;0.905092;0.857178;0.8118;0.872861;0.826652;0.841777;0.826652;0.857178;0.78289;0.797214;0.8118,,,,,\n";
        //

        // Their bug: (see constraint above) Starting with switch to text/csv
        // 2010-06-07, calculation_method became UNKNOWN!
        // Fixed 2010-06-22
        "longitude,latitude,station_id,altitude,time,sensor_id,sea_surface_wave_significant_height,sea_surface_wave_peak_period,sea_surface_wave_mean_period,sea_surface_swell_wave_significant_height,sea_surface_swell_wave_period,sea_surface_wind_wave_significant_height,sea_surface_wind_wave_period,sea_water_temperature,sea_surface_wave_to_direction,sea_surface_swell_wave_to_direction,sea_surface_wind_wave_to_direction,number_of_frequencies,center_frequencies,bandwidths,spectral_energy,mean_wave_direction,principal_wave_direction,polar_coordinate_r1,polar_coordinate_r2,calculation_method,sampling_rate\n"
            + "degrees_east,degrees_north,,m,UTC,,m,s,s,m,s,m,s,degree_C,degrees_true,degrees_true,degrees_true,count,Hz,Hz,m^2/Hz,degrees_true,degrees_true,1,1,,Hz\n"
            +
            // 2015-03-10 was
            // "-123.32,38.23,urn:ioos:station:wmo:46013,NaN,2008-08-01T14:50:00Z,urn:ioos:sensor:wmo:46013::wpm1,1.62,14.81,5.21,1.12,14.8,1.17,4.3,NaN,176.0,176.0,312.0,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.779157;0.372876;2.02274;0.714328;0.675131;0.296029;0.138154;0.605274;1.96737;1.16217;0.884235;0.462599;0.57436;0.504724;0.218129;0.38115;0.504237;0.45285;0.708456;0.626689;0.747685;0.883292;0.632856;0.448383;0.331531;0.123811;0.265022;0.214203;0.208534;0.21145;0.223251;0.114582;0.10544;0.130131;0.118191;0.0652535;0.0604571;0.0167055;0.0158453;0.00866108;0.00483522,158.0;336.0;188.0;11.0;78.0;263.0;189.0;176.0;196.0;212.0;249.0;182.0;267.0;292.0;299.0;306.0;290.0;299.0;304.0;294.0;301.0;304.0;320.0;314.0;311.0;303.0;312.0;317.0;315.0;307.0;316.0;314.0;305.0;317.0;306.0;311.0;310.0;303.0;294.0;308.0;298.0;302.0;303.0;113.0;127.0;113.0,183.0;8.0;181.0;7.0;3.0;301.0;275.0;134.0;123.0;262.0;279.0;92.0;281.0;293.0;299.0;308.0;292.0;306.0;310.0;300.0;304.0;306.0;326.0;317.0;310.0;303.0;312.0;318.0;316.0;309.0;319.0;318.0;306.0;319.0;307.0;313.0;309.0;305.0;286.0;309.0;271.0;296.0;254.0;111.0;99.0;92.0,0.212213;0.112507;0.304966;0.35902;0.254397;0.488626;0.263791;0.515939;0.462758;0.430386;0.497566;0.12097;0.497566;0.653071;0.826652;0.841777;0.702193;0.768824;0.797214;0.741445;0.797214;0.797214;0.826652;0.841777;0.857178;0.841777;0.938514;0.921652;0.905092;0.8118;0.826652;0.826652;0.78289;0.872861;0.905092;0.857178;0.88883;0.841777;0.768824;0.857178;0.677187;0.826652;0.629814;0.797214;0.677187;0.715041,0.768824;0.78289;0.826652;0.497566;0.220049;0.263791;0.488626;0.372277;0.125437;0.386024;0.304966;0.278536;0.310546;0.365588;0.653071;0.66502;0.607386;0.488626;0.525379;0.430386;0.462758;0.446279;0.585756;0.544779;0.564896;0.534991;0.78289;0.768824;0.66502;0.43826;0.525379;0.462758;0.379088;0.715041;0.689577;0.585756;0.641337;0.544779;0.333904;0.554746;0.137339;0.564896;0.134872;0.372277;0.108501;0.340013,Longuet-Higgins
            // (1964),NaN\n" +
            // "-123.32,38.23,urn:ioos:station:wmo:46013,NaN,2008-08-01T15:50:00Z,urn:ioos:sensor:wmo:46013::wpm1,1.52,9.09,4.98,1.0,9.1,1.15,5.6,NaN,291.0,291.0,297.0,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.244172;0.874391;0.950049;0.992051;0.292122;0.416385;0.116264;0.32567;1.0886;1.49577;0.707195;0.412901;0.383;0.336784;0.162325;0.507266;0.721374;0.521185;0.317616;0.580232;0.620904;0.720338;0.544952;0.400361;0.457406;0.340211;0.190368;0.295531;0.258054;0.13138;0.178793;0.207494;0.162191;0.0901461;0.101774;0.0468724;0.036226;0.0442694;0.0218615;0.0143249;0.00447678,245.0;180.0;16.0;1.0;157.0;253.0;192.0;221.0;234.0;193.0;171.0;182.0;331.0;297.0;291.0;287.0;304.0;296.0;295.0;307.0;306.0;297.0;309.0;309.0;310.0;301.0;321.0;300.0;296.0;315.0;295.0;305.0;311.0;311.0;312.0;312.0;311.0;309.0;305.0;310.0;317.0;304.0;303.0;125.0;132.0;122.0,179.0;190.0;1.0;1.0;176.0;292.0;110.0;311.0;321.0;118.0;143.0;131.0;335.0;305.0;296.0;290.0;313.0;310.0;298.0;321.0;306.0;298.0;308.0;308.0;310.0;301.0;323.0;300.0;294.0;320.0;295.0;308.0;312.0;313.0;315.0;311.0;311.0;310.0;305.0;311.0;316.0;309.0;298.0;121.0;132.0;109.0,0.153123;0.340013;0.288822;0.254397;0.372277;0.379088;0.400278;0.430386;0.299487;0.14767;0.228175;0.393087;0.534991;0.741445;0.78289;0.857178;0.702193;0.741445;0.797214;0.689577;0.857178;0.921652;0.905092;0.768824;0.905092;0.88883;0.921652;0.88883;0.872861;0.872861;0.872861;0.872861;0.921652;0.88883;0.857178;0.857178;0.88883;0.857178;0.78289;0.797214;0.728123;0.741445;0.826652;0.8118;0.872861;0.728123,0.768824;0.575231;0.564896;0.689577;0.575231;0.497566;0.150371;0.259051;0.259051;0.728123;0.400278;0.393087;0.728123;0.689577;0.66502;0.677187;0.607386;0.400278;0.575231;0.534991;0.677187;0.8118;0.75501;0.365588;0.741445;0.741445;0.8118;0.689577;0.653071;0.653071;0.629814;0.618498;0.768824;0.689577;0.585756;0.596473;0.66502;0.629814;0.454444;0.415059;0.283632;0.216095;0.488626;0.488626;0.618498;0.236601,Longuet-Higgins
            // (1964),NaN\n" +
            // "-123.32,38.23,urn:ioos:station:wmo:46013,NaN,2008-08-01T16:50:00Z,urn:ioos:sensor:wmo:46013::wpm1,1.49,14.81,5.11,1.01,14.8,1.1,4.8,NaN,175.0,175.0,309.0,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.724702;0.909481;2.34661;0.698133;0.516662;0.499779;0.284884;0.407779;0.94326;1.08406;0.29313;0.464502;0.346171;0.393304;0.327266;0.531525;0.423195;0.328752;0.332852;0.702979;0.627516;0.379029;0.603016;0.337529;0.385623;0.308393;0.266641;0.207837;0.0681764;0.212742;0.18737;0.138199;0.122643;0.130927;0.0889706;0.0656523;0.0608267;0.0359928;0.0115031;0.0100742;0.00469153,287.0;208.0;76.0;353.0;123.0;193.0;205.0;175.0;198.0;155.0;196.0;246.0;285.0;304.0;297.0;324.0;298.0;296.0;299.0;303.0;299.0;298.0;304.0;306.0;309.0;304.0;311.0;299.0;317.0;301.0;308.0;314.0;314.0;325.0;315.0;301.0;312.0;322.0;306.0;305.0;324.0;302.0;326.0;119.0;139.0;137.0,350.0;193.0;12.0;13.0;171.0;135.0;151.0;161.0;162.0;158.0;143.0;301.0;313.0;303.0;304.0;321.0;320.0;303.0;302.0;306.0;299.0;300.0;307.0;305.0;311.0;302.0;316.0;299.0;317.0;299.0;308.0;317.0;320.0;346.0;313.0;304.0;312.0;327.0;305.0;306.0;331.0;299.0;333.0;115.0;139.0;143.0,0.177024;0.224075;0.333904;0.393087;0.273532;0.310546;0.299487;0.534991;0.506669;0.43826;0.249826;0.327905;0.340013;0.8118;0.78289;0.585756;0.653071;0.741445;0.826652;0.826652;0.857178;0.797214;0.841777;0.857178;0.905092;0.88883;0.872861;0.921652;0.905092;0.88883;0.872861;0.872861;0.8118;0.728123;0.872861;0.905092;0.857178;0.8118;0.872861;0.826652;0.841777;0.826652;0.857178;0.78289;0.797214;0.8118,0.596473;0.544779;0.488626;0.228175;0.316228;0.506669;0.479847;0.415059;0.372277;0.236601;0.228175;0.346234;0.479847;0.66502;0.534991;0.534991;0.288822;0.554746;0.728123;0.641337;0.728123;0.525379;0.653071;0.575231;0.768824;0.702193;0.618498;0.741445;0.741445;0.689577;0.629814;0.618498;0.430386;0.400278;0.629814;0.75501;0.629814;0.446279;0.641337;0.488626;0.585756;0.454444;0.618498;0.340013;0.454444;0.422653,Longuet-Higgins
            // (1964),NaN\n";
            "-123.307,38.238,urn:ioos:station:wmo:46013,NaN,2008-08-01T14:50:00Z,urn:ioos:sensor:wmo:46013::wpm1,1.62,14.81,5.21,1.12,14.8,1.17,4.3,NaN,356.0,356.0,132.0,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.779157;0.372876;2.02274;0.714328;0.675131;0.296029;0.138154;0.605274;1.96737;1.16217;0.884235;0.462599;0.57436;0.504724;0.218129;0.38115;0.504237;0.45285;0.708456;0.626689;0.747685;0.883292;0.632856;0.448383;0.331531;0.123811;0.265022;0.214203;0.208534;0.21145;0.223251;0.114582;0.10544;0.130131;0.118191;0.0652535;0.0604571;0.0167055;0.0158453;0.00866108;0.00483522,158.0;336.0;188.0;11.0;78.0;263.0;189.0;176.0;196.0;212.0;249.0;182.0;267.0;292.0;299.0;306.0;290.0;299.0;304.0;294.0;301.0;304.0;320.0;314.0;311.0;303.0;312.0;317.0;315.0;307.0;316.0;314.0;305.0;317.0;306.0;311.0;310.0;303.0;294.0;308.0;298.0;302.0;303.0;113.0;127.0;113.0,183.0;8.0;181.0;7.0;3.0;301.0;275.0;134.0;123.0;262.0;279.0;92.0;281.0;293.0;299.0;308.0;292.0;306.0;310.0;300.0;304.0;306.0;326.0;317.0;310.0;303.0;312.0;318.0;316.0;309.0;319.0;318.0;306.0;319.0;307.0;313.0;309.0;305.0;286.0;309.0;271.0;296.0;254.0;111.0;99.0;92.0,0.212213;0.112507;0.304966;0.35902;0.254397;0.488626;0.263791;0.515939;0.462758;0.430386;0.497566;0.12097;0.497566;0.653071;0.826652;0.841777;0.702193;0.768824;0.797214;0.741445;0.797214;0.797214;0.826652;0.841777;0.857178;0.841777;0.938514;0.921652;0.905092;0.8118;0.826652;0.826652;0.78289;0.872861;0.905092;0.857178;0.88883;0.841777;0.768824;0.857178;0.677187;0.826652;0.629814;0.797214;0.677187;0.715041,0.768824;0.78289;0.826652;0.497566;0.220049;0.263791;0.488626;0.372277;0.125437;0.386024;0.304966;0.278536;0.310546;0.365588;0.653071;0.66502;0.607386;0.488626;0.525379;0.430386;0.462758;0.446279;0.585756;0.544779;0.564896;0.534991;0.78289;0.768824;0.66502;0.43826;0.525379;0.462758;0.379088;0.715041;0.689577;0.585756;0.641337;0.544779;0.333904;0.554746;0.137339;0.564896;0.134872;0.372277;0.108501;0.340013,Longuet-Higgins (1964),NaN\n"
            + "-123.307,38.238,urn:ioos:station:wmo:46013,NaN,2008-08-01T15:50:00Z,urn:ioos:sensor:wmo:46013::wpm1,1.52,9.09,4.98,1.0,9.1,1.15,5.6,NaN,111.0,111.0,117.0,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.244172;0.874391;0.950049;0.992051;0.292122;0.416385;0.116264;0.32567;1.0886;1.49577;0.707195;0.412901;0.383;0.336784;0.162325;0.507266;0.721374;0.521185;0.317616;0.580232;0.620904;0.720338;0.544952;0.400361;0.457406;0.340211;0.190368;0.295531;0.258054;0.13138;0.178793;0.207494;0.162191;0.0901461;0.101774;0.0468724;0.036226;0.0442694;0.0218615;0.0143249;0.00447678,245.0;180.0;16.0;1.0;157.0;253.0;192.0;221.0;234.0;193.0;171.0;182.0;331.0;297.0;291.0;287.0;304.0;296.0;295.0;307.0;306.0;297.0;309.0;309.0;310.0;301.0;321.0;300.0;296.0;315.0;295.0;305.0;311.0;311.0;312.0;312.0;311.0;309.0;305.0;310.0;317.0;304.0;303.0;125.0;132.0;122.0,179.0;190.0;1.0;1.0;176.0;292.0;110.0;311.0;321.0;118.0;143.0;131.0;335.0;305.0;296.0;290.0;313.0;310.0;298.0;321.0;306.0;298.0;308.0;308.0;310.0;301.0;323.0;300.0;294.0;320.0;295.0;308.0;312.0;313.0;315.0;311.0;311.0;310.0;305.0;311.0;316.0;309.0;298.0;121.0;132.0;109.0,0.153123;0.340013;0.288822;0.254397;0.372277;0.379088;0.400278;0.430386;0.299487;0.14767;0.228175;0.393087;0.534991;0.741445;0.78289;0.857178;0.702193;0.741445;0.797214;0.689577;0.857178;0.921652;0.905092;0.768824;0.905092;0.88883;0.921652;0.88883;0.872861;0.872861;0.872861;0.872861;0.921652;0.88883;0.857178;0.857178;0.88883;0.857178;0.78289;0.797214;0.728123;0.741445;0.826652;0.8118;0.872861;0.728123,0.768824;0.575231;0.564896;0.689577;0.575231;0.497566;0.150371;0.259051;0.259051;0.728123;0.400278;0.393087;0.728123;0.689577;0.66502;0.677187;0.607386;0.400278;0.575231;0.534991;0.677187;0.8118;0.75501;0.365588;0.741445;0.741445;0.8118;0.689577;0.653071;0.653071;0.629814;0.618498;0.768824;0.689577;0.585756;0.596473;0.66502;0.629814;0.454444;0.415059;0.283632;0.216095;0.488626;0.488626;0.618498;0.236601,Longuet-Higgins (1964),NaN\n"
            + "-123.307,38.238,urn:ioos:station:wmo:46013,NaN,2008-08-01T16:50:00Z,urn:ioos:sensor:wmo:46013::wpm1,1.49,14.81,5.11,1.01,14.8,1.1,4.8,NaN,355.0,355.0,129.0,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.724702;0.909481;2.34661;0.698133;0.516662;0.499779;0.284884;0.407779;0.94326;1.08406;0.29313;0.464502;0.346171;0.393304;0.327266;0.531525;0.423195;0.328752;0.332852;0.702979;0.627516;0.379029;0.603016;0.337529;0.385623;0.308393;0.266641;0.207837;0.0681764;0.212742;0.18737;0.138199;0.122643;0.130927;0.0889706;0.0656523;0.0608267;0.0359928;0.0115031;0.0100742;0.00469153,287.0;208.0;76.0;353.0;123.0;193.0;205.0;175.0;198.0;155.0;196.0;246.0;285.0;304.0;297.0;324.0;298.0;296.0;299.0;303.0;299.0;298.0;304.0;306.0;309.0;304.0;311.0;299.0;317.0;301.0;308.0;314.0;314.0;325.0;315.0;301.0;312.0;322.0;306.0;305.0;324.0;302.0;326.0;119.0;139.0;137.0,350.0;193.0;12.0;13.0;171.0;135.0;151.0;161.0;162.0;158.0;143.0;301.0;313.0;303.0;304.0;321.0;320.0;303.0;302.0;306.0;299.0;300.0;307.0;305.0;311.0;302.0;316.0;299.0;317.0;299.0;308.0;317.0;320.0;346.0;313.0;304.0;312.0;327.0;305.0;306.0;331.0;299.0;333.0;115.0;139.0;143.0,0.177024;0.224075;0.333904;0.393087;0.273532;0.310546;0.299487;0.534991;0.506669;0.43826;0.249826;0.327905;0.340013;0.8118;0.78289;0.585756;0.653071;0.741445;0.826652;0.826652;0.857178;0.797214;0.841777;0.857178;0.905092;0.88883;0.872861;0.921652;0.905092;0.88883;0.872861;0.872861;0.8118;0.728123;0.872861;0.905092;0.857178;0.8118;0.872861;0.826652;0.841777;0.826652;0.857178;0.78289;0.797214;0.8118,0.596473;0.544779;0.488626;0.228175;0.316228;0.506669;0.479847;0.415059;0.372277;0.236601;0.228175;0.346234;0.479847;0.66502;0.534991;0.534991;0.288822;0.554746;0.728123;0.641337;0.728123;0.525379;0.653071;0.575231;0.768824;0.702193;0.618498;0.741445;0.741445;0.689577;0.629814;0.618498;0.430386;0.400278;0.629814;0.75501;0.629814;0.446279;0.641337;0.488626;0.585756;0.454444;0.618498;0.340013;0.454444;0.422653,Longuet-Higgins (1964),NaN\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    // same test, but with superfluous string data regex test
    // String2.log("\n*** EDDTableFromSOS.testNdbcSosWaves test get one station .CSV
    // data\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&station_id=\"urn:ioos:station:wmo:46013\"&time>=2008-08-01T14&time<=2008-08-01T17"
                +
                // !!!!!! Starting with switch to text/csv 2010-06-07, calculation_method became
                // UNKNOWN!
                // but fixed 2010-06-22
                "&calculation_method=~\"(zztop|Long.*)\"",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_ndbcSosWaves",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    // test error
    try {
      // String2.log("\n*** EDDTableFromSOS.testNdbcSosWaves test >30 days\n");
      tName =
          eddTable.makeNewFileForDapQuery(
              language,
              null,
              null,
              "&station_id=\"urn:ioos:station:wmo:46013\"&time>=2008-08-01&time<=2008-09-05",
              EDStatic.fullTestCacheDirectory,
              eddTable.className() + "_ndbcSosWaves30",
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      expected = "shouldn't get here";
      Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    } catch (Throwable t) {
      String msg = MustBe.throwableToString(t);
      expected =
          "java.lang.RuntimeException: Source Exception=\""
              + "InvalidParameterValue: eventTime: No more than 31 days of "
              + "data can be requested.\".";
      if (msg.indexOf(expected) < 0) {
        throw new RuntimeException(
            "expected=" + expected + "\nUnexpected error ndbcSosWaves. NDBC SOS Server is in flux.",
            t);
      }
    }
  }

  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testNdbcSosWindQuickRestart() throws Throwable {

    String qrName =
        File2.forceExtension(EDDTableFromSOS.quickRestartFullFileName("ndbcSosWind"), ".xml");
    // String2.log("\n\n*** deleting quickRestartFile exists=" +
    // File2.isFile(qrName) +
    // " qrName=" + qrName);
    File2.delete(qrName);

    testNdbcSosWind();

    testNdbcSosWind();
  }

  /**
   * This should work, but server is in flux so it often breaks.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testNdbcSosWind() throws Throwable {

    // testVerboseOn();
    int language = 0;
    EDDTable eddTable = (EDDTable) EDDTestDataset.getndbcSosWind();
    String name, tName, results = null, expected, userDapQuery;

    String2.log("\n*** EDDTableFromSOS.testNdbcSosWind .das\n");
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard
    // to check min:sec.
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_ndbcSosWind",
            ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range -177.738, 180.0;\n"
            + "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float64 actual_range -55.0, 71.758;\n"
            + "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  station_id {\n"
            + "    String cf_role \"timeseries_id\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Station ID\";\n"
            + "  }\n"
            + "  altitude {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    String _CoordinateZisPositive \"up\";\n"
            + "    String axis \"Z\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Altitude\";\n"
            + "    String positive \"up\";\n"
            + "    String standard_name \"altitude\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range 1.1360742e+9, NaN;\n"
            + // pre 2013-11-01 was 1.1540346e+9
            "    String axis \"T\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  sensor_id {\n"
            + "    String comment \"Always check the quality_flags before using this data.\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Sensor ID\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/winds\";\n"
            + "  }\n"
            + "  wind_from_direction {\n"
            + "    Float64 colorBarMaximum 360.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Wind\";\n"
            + "    String long_name \"Wind From Direction\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/winds\";\n"
            + "    String standard_name \"wind_from_direction\";\n"
            + "    String units \"degrees_true\";\n"
            + "  }\n"
            + "  wind_speed {\n"
            + "    Float64 colorBarMaximum 15.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Wind\";\n"
            + "    String long_name \"Wind Speed\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/winds\";\n"
            + "    String standard_name \"wind_speed\";\n"
            + "    String units \"m s-1\";\n"
            + "  }\n"
            + "  wind_speed_of_gust {\n"
            + "    Float64 colorBarMaximum 30.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Wind\";\n"
            + "    String long_name \"Wind Speed of Gust\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/winds\";\n"
            + "    String standard_name \"wind_speed_of_gust\";\n"
            + "    String units \"m s-1\";\n"
            + "  }\n"
            + "  upward_air_velocity {\n"
            + "    Float64 colorBarMaximum 10.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Wind\";\n"
            + "    String long_name \"Upward Air Velocity\";\n"
            + "    String observedProperty \"http://mmisw.org/ont/cf/parameter/winds\";\n"
            + "    String units \"m s-1\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_data_type \"TimeSeries\";\n"
            + "    String cdm_timeseries_variables \"station_id, longitude, latitude, sensor_id\";\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "    Float64 Easternmost_Easting 180.0;\n"
            + "    String featureType \"TimeSeries\";\n"
            + "    Float64 geospatial_lat_max 71.758;\n"
            + "    Float64 geospatial_lat_min -55.0;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max 180.0;\n"
            + "    Float64 geospatial_lon_min -177.738;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    String geospatial_vertical_positive \"up\";\n"
            + "    String geospatial_vertical_units \"m\";\n"
            + "    String history \""
            + today;
    String tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

    // + " https://sdf" + datasetIdPrefix + ".ndbc.noaa.gov/sos/server.php\n" +
    // today + " " + EDStatic.erddapUrl + //in tests, always use non-https url
    // "/tabledap/" +
    expected =
        "ndbcSosWind.das\";\n"
            + "    String infoUrl \"https://sdf.ndbc.noaa.gov/sos/\";\n"
            + "    String institution \"NOAA NDBC\";\n"
            + "    String keywords \"air, altitude, atmosphere, atmospheric, direction, Earth Science > Atmosphere > Altitude > Station Height, Earth Science > Atmosphere > Atmospheric Winds > Surface Winds, Earth Science > Atmosphere > Atmospheric Winds > Vertical Wind Motion, from, gust, height, identifier, ndbc, noaa, sensor, sos, speed, station, surface, time, upward, velocity, wind, wind_from_direction, wind_speed, wind_speed_of_gust, winds\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    Float64 Northernmost_Northing 71.758;\n"
            + "    String sourceUrl \"https://sdf.ndbc.noaa.gov/sos/server.php\";\n"
            + "    Float64 Southernmost_Northing -55.0;\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String subsetVariables \"station_id, longitude, latitude\";\n"
            + "    String summary \"The NOAA NDBC SOS server is part of the IOOS DIF SOS Project.  The stations in this dataset have winds data.\n"
            + "\n"
            + "Because of the nature of SOS requests, requests for data MUST include constraints for the longitude, latitude, time, and/or station_id variables.\";\n"
            + "    String time_coverage_start \"2006-01-01T00:10:00Z\";\n"
            + // pre 2013-11-01 was 2006-07-27T21:10:00Z
            "    String title \"NOAA NDBC SOS, 2006-present, winds\";\n"
            + "    Float64 Westernmost_Easting -177.738;\n"
            + "  }\n"
            + "}\n";
    int tPo = results.indexOf(expected.substring(0, 17));
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
        expected,
        "results=\n" + results);

    String2.log("\n*** EDDTableFromSOS.testNdbcSosWind test get one station .CSV data\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&station_id=\"urn:ioos:station:wmo:41004\"&time>=2008-08-01T00:00&time<=2008-08-01T04",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_ndbcSosWind",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected = // 2019-03-19 was alt 5.0, now 4.0
        "longitude,latitude,station_id,altitude,time,sensor_id,wind_from_direction,wind_speed,wind_speed_of_gust,upward_air_velocity\n"
            + "degrees_east,degrees_north,,m,UTC,,degrees_true,m s-1,m s-1,m s-1\n"
            + "-79.099,32.501,urn:ioos:station:wmo:41004,4.0,2008-08-01T00:50:00Z,urn:ioos:sensor:wmo:41004::anemometer1,229.0,10.1,12.6,NaN\n"
            + "-79.099,32.501,urn:ioos:station:wmo:41004,4.0,2008-08-01T01:50:00Z,urn:ioos:sensor:wmo:41004::anemometer1,232.0,9.3,11.3,NaN\n"
            + "-79.099,32.501,urn:ioos:station:wmo:41004,4.0,2008-08-01T02:50:00Z,urn:ioos:sensor:wmo:41004::anemometer1,237.0,7.8,11.5,NaN\n"
            + "-79.099,32.501,urn:ioos:station:wmo:41004,4.0,2008-08-01T03:50:00Z,urn:ioos:sensor:wmo:41004::anemometer1,236.0,8.0,9.3,NaN\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);
  }

  /**
   * testOostethys This tests datasetID=gomoosBuoy.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testOostethys() throws Throwable {

    /*
     * I talked on phone with Eric Bridger (207 228-1662) of gomoos
     * (gomoos now gomri and neracoos)
     * He said
     * Gomoos sos server is the perl Oostethys server
     * and I basically can treat as an oostethys reference implementation
     * Luis Bermudez is person most responsible for oostethys,
     * which is now unfunded and a volunteer effort
     * and main web page is at google code
     * http://code.google.com/p/oostethys/ but it links back to
     * http://oostethys.org/ for all but code-related info)
     * Oostethys future: may get funding for Q2O project of QARTOD,
     * to add quality flags, provenance, etc to SOS
     * So I will make ERDDAP able to read
     * Goomoos sos server (strict! to comply with ogc tests)
     * and neracoos sos server (older version, less strict)
     * 2010-04-29
     * 2012-04-30 Server has been unavailable. Eric restarted it on new server:
     * was http://www.gomoos.org/cgi-bin/sos/V1.0/oostethys_sos.cgi
     * now http://oceandata.gmri.org/cgi-bin/sos/V1.0/oostethys_sos.cgi
     */
    int language = 0;
    boolean oSosActive = EDStatic.sosActive;
    EDStatic.sosActive = true;
    // boolean oDebugMode = debugMode;
    // debugMode = true;
    double tLon, tLat;
    String name, tName, results, expected, userDapQuery;
    String error = "";
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard to
    // check min:sec.
    EDDTable eddTable = (EDDTable) EDDTestDataset.getgomoosBuoy();

    // String2.log("\n*** EDDTableFromSOS.testOostethys");
    // testVerboseOn();

    // /*
    // getEmpiricalMinMax just do once
    // useful for SOS: get alt values
    // eddTable.getEmpiricalMinMax(language, "2007-02-01", "2007-02-01", false,
    // true);
    // if (true) System.exit(1);

    // test sos-server values
    String2.log("nOfferings=" + eddTable.sosOfferings.size());
    String2.log(
        eddTable.sosOfferings.getString(0)
            + "  lon="
            + eddTable.sosMinLon.getNiceDouble(0)
            + ", "
            + eddTable.sosMaxLon.getNiceDouble(0)
            + " lat="
            + eddTable.sosMinLat.getNiceDouble(0)
            + ", "
            + eddTable.sosMaxLat.getNiceDouble(0)
            + " time="
            + eddTable.sosMinTime.getNiceDouble(0)
            + ", "
            + eddTable.sosMaxTime.getNiceDouble(0));
    // String2.log(String2.toCSSVString(eddTable.sosObservedProperties()));

    Test.ensureTrue(
        eddTable.sosOfferings.size() >= 9, // was 15
        "nOfferings=" + eddTable.sosOfferings.size()); // changes sometimes
    int which = eddTable.sosOfferings.indexOf("A01");
    String2.log("which=" + which);
    Test.ensureEqual(eddTable.sosOfferings.getString(which), "A01", "");
    // 2008-07-25 was .5655, pre 2008-10-09 was .5658, 2009-03-23 changed,
    // pre 2010-07-08 was -70.5600967407227
    // pre 2013-06-28 was -70.5655
    // pre 2013-11-01 was -70.5645150078668
    // give up on =. Use almostEqual
    Test.ensureAlmostEqual(4, eddTable.sosMinLon.getNiceDouble(which), -70.565, "");
    Test.ensureAlmostEqual(4, eddTable.sosMaxLon.getNiceDouble(which), -70.565, "");
    // 2008-07-25 was 42.5232, pre 2008-10-09 was .5226, 2009-03-23 changed
    // pre 2010-07-08 was 42.5261497497559;
    // pre 2013-06-28 was 42.5232
    // pre 2013-11-01 was 42.5223609076606
    // ... It keeps changing.
    // give up on =. Use almostEqual
    Test.ensureAlmostEqual(4, eddTable.sosMinLat.getNiceDouble(which), 42.5243339538574, "");
    Test.ensureAlmostEqual(4, eddTable.sosMaxLat.getNiceDouble(which), 42.5243339538574, "");
    Test.ensureEqual(eddTable.sosMinTime.getNiceDouble(which), 9.94734E8, "");
    Test.ensureEqual(eddTable.sosMaxTime.getNiceDouble(which), Double.NaN, "");
    // Test.ensureEqual(String2.toCSSVString(eddTable.sosObservedProperties()),
    // "http://marinemetadata.org/cf#sea_water_salinity, " +
    // //http://marinemetadata.org is GONE!
    // "http://marinemetadata.org/cf#sea_water_temperature",
    // NOW http://mmisw.org/ont/cf/parameter/sea_water_salinity
    // "");

    userDapQuery =
        "longitude,latitude,altitude,time,station_id,"
            + "sea_water_temperature,sea_water_salinity"
            + "&longitude>=-70&longitude<=-69&latitude>=43&latitude<=44"
            + "&time>=2007-07-04T00:00&time<=2007-07-04T01:00";
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            eddTable.datasetID() + "_Data",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        // before 2010-02-11 was
        // "longitude,latitude,altitude,time,station_id,sea_water_temperature,sea_water_salinity\n"
        // +
        // "degrees_east,degrees_north,m,UTC,,degree_C,PSU\n" +
        // "-69.8891420948262,43.7813123586226,-1.0,2007-07-04T00:00:00Z,D01,13.5,29.3161296844482\n"
        // +
        // "-69.8891420948262,43.7813123586226,-20.0,2007-07-04T00:00:00Z,D01,9.28999996185303,31.24924659729\n"
        // +
        // "-69.8891420948262,43.7813123586226,-35.0,2007-07-04T00:00:00Z,D01,8.30000019073486,NaN\n"
        // +
        // "-69.8891420948262,43.7813123586226,-1.0,2007-07-04T01:00:00Z,D01,13.5100002288818,29.192590713501\n"
        // +
        // "-69.8891420948262,43.7813123586226,-20.0,2007-07-04T01:00:00Z,D01,9.06999969482422,31.2218112945557\n"
        // +
        // "-69.8891420948262,43.7813123586226,-35.0,2007-07-04T01:00:00Z,D01,8.51000022888184,NaN\n"
        // +
        // "-69.9878833333333,43.7617166666667,-1.0,2007-07-04T00:00:00Z,D02,13.460000038147,29.9242057800293\n"
        // +
        // "-69.9878833333333,43.7617166666667,-10.0,2007-07-04T00:00:00Z,D02,11.8699998855591,31.2601585388184\n"
        // +
        // "-69.9878833333333,43.7617166666667,-1.0,2007-07-04T00:30:00Z,D02,13.4200000762939,29.9195117950439\n"
        // +
        // "-69.9878833333333,43.7617166666667,-1.0,2007-07-04T01:00:00Z,D02,13.3699998855591,29.930046081543\n"
        // +
        // "-69.9878833333333,43.7617166666667,-10.0,2007-07-04T01:00:00Z,D02,11.6599998474121,31.2559909820557\n"
        // +
        // "-69.3549346923828,43.7136993408203,-1.0,2007-07-04T00:00:00Z,E01,13.7600002288818,31.1920852661133\n"
        // +
        // "-69.3549346923828,43.7136993408203,-2.0,2007-07-04T00:00:00Z,E01,13.7034998,NaN\n"
        // +
        // "-69.3549346923828,43.7136993408203,-20.0,2007-07-04T00:00:00Z,E01,7.65000009536743,31.8228702545166\n"
        // +
        // "-69.3549346923828,43.7136993408203,-50.0,2007-07-04T00:00:00Z,E01,5.84700012207031,32.1141357421875\n"
        // +
        // "-69.3549346923828,43.7136993408203,-1.0,2007-07-04T00:30:00Z,E01,13.8900003433228,31.1868896484375\n"
        // +
        // "-69.3549346923828,43.7136993408203,-1.0,2007-07-04T01:00:00Z,E01,13.8500003814697,31.1843872070312\n"
        // +
        // "-69.3549346923828,43.7136993408203,-2.0,2007-07-04T01:00:00Z,E01,13.8292704,NaN\n"
        // +
        // "-69.3549346923828,43.7136993408203,-20.0,2007-07-04T01:00:00Z,E01,7.57000017166138,31.833927154541\n"
        // +
        // "-69.3549346923828,43.7136993408203,-50.0,2007-07-04T01:00:00Z,E01,5.81699991226196,32.0988731384277\n";
        // before 12/17/07 was -69.8876
        // before 5/19/08 was -69.9879,43.7617,
        // changed again 2009-03-26:
        // "longitude,latitude,altitude,time,station_id,sea_water_temperature,sea_water_salinity\n"
        // +
        // "degrees_east,degrees_north,m,UTC,,degree_C,PSU\n" +
        // "-69.9878833333333,43.7617166666667,-1.0,2007-07-04T00:00:00Z,D02,13.460000038147,29.9242057800293\n"
        // +
        // "-69.9878833333333,43.7617166666667,-10.0,2007-07-04T00:00:00Z,D02,11.8699998855591,31.2601585388184\n"
        // +
        // "-69.9878833333333,43.7617166666667,-1.0,2007-07-04T00:30:00Z,D02,13.4200000762939,29.9195117950439\n"
        // +
        // "-69.9878833333333,43.7617166666667,-1.0,2007-07-04T01:00:00Z,D02,13.3699998855591,29.930046081543\n"
        // +
        // "-69.9878833333333,43.7617166666667,-10.0,2007-07-04T01:00:00Z,D02,11.6599998474121,31.2559909820557\n"
        // +
        // "-69.3549346923828,43.7136993408203,-1.0,2007-07-04T00:00:00Z,E01,13.7600002288818,31.1920852661133\n"
        // +
        // "-69.3549346923828,43.7136993408203,-2.0,2007-07-04T00:00:00Z,E01,13.7034998,NaN\n"
        // +
        // "-69.3549346923828,43.7136993408203,-20.0,2007-07-04T00:00:00Z,E01,7.65000009536743,31.8228702545166\n"
        // +
        // "-69.3549346923828,43.7136993408203,-50.0,2007-07-04T00:00:00Z,E01,5.84700012207031,32.1141357421875\n"
        // +
        // "-69.3549346923828,43.7136993408203,-1.0,2007-07-04T00:30:00Z,E01,13.8900003433228,31.1868896484375\n"
        // +
        // "-69.3549346923828,43.7136993408203,-1.0,2007-07-04T01:00:00Z,E01,13.8500003814697,31.1843872070312\n"
        // +
        // "-69.3549346923828,43.7136993408203,-2.0,2007-07-04T01:00:00Z,E01,13.8292704,NaN\n"
        // +
        // "-69.3549346923828,43.7136993408203,-20.0,2007-07-04T01:00:00Z,E01,7.57000017166138,31.833927154541\n"
        // +
        // "-69.3549346923828,43.7136993408203,-50.0,2007-07-04T01:00:00Z,E01,5.81699991226196,32.0988731384277\n";
        // 2010-07-08 When I switch to ALL_PLATFORMS BBOX request,
        // D01 (which is not currently in GetCapabilities) returned to the response
        "longitude,latitude,altitude,time,station_id,sea_water_temperature,sea_water_salinity\n"
            + "degrees_east,degrees_north,m,UTC,,degree_C,PSU\n"
            + "-69.8891420948262,43.7813123586226,-1.0,2007-07-04T00:00:00Z,D01,13.5,29.3161296844482\n"
            + "-69.8891420948262,43.7813123586226,-20.0,2007-07-04T00:00:00Z,D01,9.28999996185303,31.24924659729\n"
            + "-69.8891420948262,43.7813123586226,-35.0,2007-07-04T00:00:00Z,D01,8.30000019073486,NaN\n"
            + "-69.8891420948262,43.7813123586226,-1.0,2007-07-04T01:00:00Z,D01,13.5100002288818,29.192590713501\n"
            + "-69.8891420948262,43.7813123586226,-20.0,2007-07-04T01:00:00Z,D01,9.06999969482422,31.2218112945557\n"
            + "-69.8891420948262,43.7813123586226,-35.0,2007-07-04T01:00:00Z,D01,8.51000022888184,NaN\n"
            + "-69.9878833333333,43.7617166666667,-1.0,2007-07-04T00:00:00Z,D02,13.460000038147,29.9242057800293\n"
            + "-69.9878833333333,43.7617166666667,-10.0,2007-07-04T00:00:00Z,D02,11.8699998855591,31.2601585388184\n"
            + "-69.9878833333333,43.7617166666667,-1.0,2007-07-04T00:30:00Z,D02,13.4200000762939,29.9195117950439\n"
            + "-69.9878833333333,43.7617166666667,-1.0,2007-07-04T01:00:00Z,D02,13.3699998855591,29.930046081543\n"
            + "-69.9878833333333,43.7617166666667,-10.0,2007-07-04T01:00:00Z,D02,11.6599998474121,31.2559909820557\n"
            + "-69.3549346923828,43.7136993408203,-1.0,2007-07-04T00:00:00Z,E01,13.7600002288818,31.1920852661133\n"
            + "-69.3549346923828,43.7136993408203,-2.0,2007-07-04T00:00:00Z,E01,13.7034998,NaN\n"
            + "-69.3549346923828,43.7136993408203,-20.0,2007-07-04T00:00:00Z,E01,7.65000009536743,31.8228702545166\n"
            + "-69.3549346923828,43.7136993408203,-50.0,2007-07-04T00:00:00Z,E01,5.84700012207031,32.1141357421875\n"
            + "-69.3549346923828,43.7136993408203,-1.0,2007-07-04T00:30:00Z,E01,13.8900003433228,31.1868896484375\n"
            + "-69.3549346923828,43.7136993408203,-1.0,2007-07-04T01:00:00Z,E01,13.8500003814697,31.1843872070312\n"
            + "-69.3549346923828,43.7136993408203,-2.0,2007-07-04T01:00:00Z,E01,13.8292704,NaN\n"
            + "-69.3549346923828,43.7136993408203,-20.0,2007-07-04T01:00:00Z,E01,7.57000017166138,31.833927154541\n"
            + "-69.3549346923828,43.7136993408203,-50.0,2007-07-04T01:00:00Z,E01,5.81699991226196,32.0988731384277\n";
    Test.ensureEqual(results, expected, results);

    // data for mapExample (no time) just uses station table data
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "longitude,latitude&longitude>-70",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "MapNT",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected = // this changes a little periodically (NOT GOOD, stations have 1 point (which
        // applies to historic data and changes!)
        // before 2009-01-12 this was
        // "longitude,latitude\n" +
        // "degrees_east,degrees_north\n" +
        // "-69.8891,43.7813\n" +
        // "-69.9885,43.7612\n" +
        // "-69.3578,43.7148\n" +
        // "-68.998,44.0548\n" +
        // "-68.1087,44.1058\n" +
        // "-67.0123,44.8893\n" +
        // "-66.5541,43.6276\n" +
        // "-67.8798,43.4907\n" +
        // "-65.9118,42.3279\n";
        // before 2009-03-23 this was
        // "longitude,latitude\n" +
        // "degrees_east,degrees_north\n" +
        // "-69.8891,43.7813\n" +
        // "-69.9885,43.7612\n" +
        // "-69.3578,43.7148\n" +
        // "-68.998,44.0548\n" +
        // "-68.1085,44.1057\n" +
        // "-67.0123,44.8893\n" +
        // "-66.5541,43.6276\n" +
        // "-67.8798,43.4907\n" +
        // "-65.9118,42.3279\n";
        // before 2009-03-26 was
        // "longitude,latitude\n" +
        // "degrees_east,degrees_north\n" +
        // "-69.8891,43.7813\n" +
        // "-69.9885,43.7612\n" +
        // "-69.3578,43.7148\n" +
        // "-68.998,44.0548\n" +
        // "-68.108,44.1052\n" +
        // "-67.0123,44.8893\n" +
        // "-66.5541,43.6276\n" +
        // "-67.8798,43.4907\n" +
        // "-65.9118,42.3279\n";
        // "longitude,latitude\n" +
        // "degrees_east,degrees_north\n" +
        // "-69.8891,43.7813\n" +
        // "-69.9877,43.7628\n" +
        // "-69.3578,43.7148\n" +
        // "-68.9982,44.0555\n" +
        // "-68.108,44.1052\n" +
        // "-67.0123,44.8893\n" +
        // "-66.5541,43.6276\n" +
        // "-67.8798,43.4907\n" +
        // "-65.9118,42.3279\n";
        // "longitude,latitude\n" +
        // "degrees_east,degrees_north\n" +
        // "-69.8891,43.7813\n" +
        // "-69.9877,43.7628\n" +
        // "-69.3578,43.7148\n" +
        // "-68.9982,44.0555\n" +
        // "-68.108,44.1052\n" +
        // "-67.0123,44.8893\n" +
        // "-66.5541,43.6276\n" +
        // "-67.8798,43.4907\n" +
        // "-65.9096,42.3276\n";
        // "longitude,latitude\n" +
        // "degrees_east,degrees_north\n" +
        // "-69.8891,43.7813\n" +
        // "-69.9877,43.7628\n" +
        // "-69.3578,43.7148\n" +
        // "-68.9982,44.0555\n" +
        // "-68.1087,44.1058\n" +
        // "-67.0123,44.8893\n" +
        // "-66.5541,43.6276\n" +
        // "-67.8798,43.4907\n" +
        // "-65.9096,42.3276\n";
        // "longitude,latitude\n" + //starting 2009-09-28
        // "degrees_east,degrees_north\n" +
        // "-69.8891,43.7813\n" +
        // "-69.9877,43.7628\n" +
        // "-69.3578,43.7148\n" +
        // "-68.9982,44.0555\n" +
        // "-68.1087,44.1058\n" +
        // "-67.0122575759888,44.8892910480499\n" +
        // "-66.5541088046873,43.6276378712505\n" +
        // "-67.8798,43.4907\n" +
        // "-65.9066314697266,42.3259010314941\n";
        // "longitude,latitude\n" + //starting 2010-02-11
        // "degrees_east,degrees_north\n" +
        // "-69.9877,43.7628\n" +
        // "-69.3578,43.7148\n" +
        // "-68.9982,44.0555\n" +
        // "-68.1087,44.1058\n" +
        // "-67.8798,43.4907\n" +
        // "-65.9066314697266,42.3259010314941\n";
        // "longitude,latitude\n" + //starting 2010-07-08
        // "degrees_east,degrees_north\n" +
        // "-69.9877,43.7628\n" +
        // "-69.3578,43.7148\n" +
        // "-69.32,43.7065\n" + //added 2010-10-01
        // "-68.9982,44.0555\n" +
        // "-68.1087,44.1058\n" +
        // "-67.8716659545898,43.490140914917\n" + //small lat lon changes 2010-08-05
        // "-65.9081802368164,42.3263664245605\n";
        // "longitude,latitude\n" + //starting 2011-02-15
        // "degrees_east,degrees_north\n" +
        // "-69.9877,43.7628\n" +
        // "-69.3578,43.7148\n" +
        // "-69.319580078125,43.7063484191895\n" +
        // "-68.9977645874023,44.0548324584961\n" +
        // "-68.1087,44.1058\n" +
        // "-67.8716659545898,43.490140914917\n" +
        // "-65.9081802368164,42.3263664245605\n";
        // "longitude,latitude\n" + //starting 2011-07-24
        // "degrees_east,degrees_north\n" +
        // "-69.9877,43.7628\n" +
        // "-69.3552169799805,43.714298248291\n" +
        // "-69.319580078125,43.7063484191895\n" +
        // "-68.9977645874023,44.0548324584961\n" +
        // "-68.1087,44.1058\n" +
        // "-67.8798,43.4907\n" +
        // "-65.907,42.3303\n"; //pre 2011-09-05 was
        // "-65.9081802368164,42.3263664245605\n";
        // "longitude,latitude\n" + //starting 2012-03-16
        // "degrees_east,degrees_north\n" +
        // "-69.9877,43.7628\n" +
        // "-69.3578,43.7148\n" +
        // "-69.319580078125,43.7063484191895\n" +
        // "-68.9982,44.0555\n" +
        // "-68.996696472168,44.0559844970703\n" + //pre 2013-06-28 was
        // "-68.9982,44.0555\n" + //pre 2012-06-18 was -68.9981,44.055
        // "-68.109302520752,44.1061248779297\n" + //pre 2013-06-28 was
        // "-68.1090882815856,44.105949650925\n" + //pre 2012-11-02 was -68.1087,44.1058
        // "-67.8798,43.4907\n"
        "longitude,latitude\n"
            + // starting 2014-08-11
            "degrees_east,degrees_north\n"
            + "-63.4082,44.5001\n"
            + // 2021-05-14 added
            "-69.9877,43.7628\n"
            + "-69.3558807373047,43.7148017883301\n"
            + // 2020-10-05 was -69.3578,43.7148, 2015-07-31 was
            // -69.3550033569336,43.7163009643555\n" + //2014-09-22 was
            // -69.3578,43.7148\n" +
            "-69.319580078125,43.7063484191895\n"
            + "-68.997200012207,44.0555000305176\n"
            + "-68.8308,44.3878\n"
            + // 2020-10-05 was -68.82308,44.3878 2020-05-06 was
            // "-68.8249619164502,44.3871537467378\n" +
            "-68.1116700605913,44.1044013283469\n"
            + // 2020-10-05 was -68.1121,44.1028 2015-07-31 was
            // -68.1084442138672,44.1057103474935\n" +
            "-67.8798,43.4907\n"
            + // 2021-05-14 was "-67.8798,43.4907\n" +
            "-65.912899017334,42.3295593261719\n"
            + // 2021-05-14 was -65.907,42.3303 //2014-09-22 was
            // -65.9061666666667,42.3336666666667\n";
            "-54.688,46.9813\n"
            + "-54.1317,47.3255\n"
            + "-54.0488,47.7893\n";
    // "-65.9069544474284,42.3313840230306\n"; //pre 2013-06-28 was
    // "-65.907,42.3303\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // data for mapExample (with time
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "longitude,latitude&time=2007-12-11",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "MapWT",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        // changed a little on 2008-02-28, 2008-05-19, 2008-09-24, 2008-10-09 2009-01-12
        // -70.5658->-70.5655 42.5226->42.5232
        // change a little on 2009-03-26, 2009-09-18
        // "longitude,latitude\n" +
        // "degrees_east,degrees_north\n" +
        // "-70.5652267750436,42.5227725835475\n" +
        // "-70.4273380011109,43.18053298369\n" +
        // "-70.0584772405338,43.5671424043496\n" +
        // "-69.8876,43.7823\n" +
        // "-69.9878833333333,43.7617166666667\n" +
        // "-69.3563516790217,43.7138879949396\n" +
        // "-68.9979781363549,44.0556086654547\n" +
        // "-68.1087458631928,44.1053852961787\n" +
        // "-67.0122575759888,44.8892910480499\n";
        // starting 2010-02-11:
        // "longitude,latitude\n" +
        // "degrees_east,degrees_north\n" +
        // "-70.5652267750436,42.5227725835475\n" +
        // "-70.4273380011109,43.18053298369\n" +
        // "-69.9878833333333,43.7617166666667\n" +
        // "-69.3563516790217,43.7138879949396\n" +
        // "-68.9979781363549,44.0556086654547\n" +
        // "-68.1087458631928,44.1053852961787\n";
        // starting 2010-07-08 with new ALL_PLATFORMS BBOX request, D01 returns:
        "longitude,latitude\n"
            + "degrees_east,degrees_north\n"
            + "-70.5652267750436,42.5227725835475\n"
            + "-70.4273380011109,43.18053298369\n"
            + "-70.0584772405338,43.5671424043496\n"
            + "-69.8876,43.7823\n"
            + "-69.9878833333333,43.7617166666667\n"
            + "-69.3563516790217,43.7138879949396\n"
            + "-68.9979781363549,44.0556086654547\n"
            + "-68.1087458631928,44.1053852961787\n"
            + "-67.0122575759888,44.8892910480499\n"; // 2021-05-14 added
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // data for all variables
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&station_id=\"A01\"&time=2007-12-11",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "MapWTAV",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected = // changed a little on 2008-02-28, 2008-05-19, 2008-09-24, 2008-10-09 2009-01-12
        // -70.5658->-70.5655 42.5226->42.5232
        // change a little on 2009-03-26, 2009-09-18
        "longitude,latitude,station_id,altitude,time,air_temperature,chlorophyll,direction_of_sea_water_velocity,dominant_wave_period,sea_level_pressure,sea_water_density,sea_water_electrical_conductivity,sea_water_salinity,sea_water_speed,sea_water_temperature,wave_height,visibility_in_air,wind_from_direction,wind_gust,wind_speed\n"
            + "degrees_east,degrees_north,,m,UTC,degree_C,mg m-3,degrees_true,s,hPa,kg m-3,S m-1,PSU,cm s-1,degree_C,m,m,degrees_true,m s-1,m s-1\n"
            + "-70.5652267750436,42.5227725835475,A01,4.0,2007-12-11T00:00:00Z,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,332.100006103516,3.94600009918213,3.36700010299683\n"
            + "-70.5652267750436,42.5227725835475,A01,3.0,2007-12-11T00:00:00Z,0.899999976158142,NaN,NaN,NaN,1024.82849121094,NaN,NaN,NaN,NaN,NaN,NaN,2920.6796875,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,0.0,2007-12-11T00:00:00Z,NaN,NaN,NaN,5.33333349,NaN,NaN,NaN,NaN,NaN,NaN,0.644578338,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-1.0,2007-12-11T00:00:00Z,NaN,NaN,NaN,NaN,NaN,25.3888969421387,33.2410011291504,32.4736976623535,NaN,7.30000019073486,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-2.0,2007-12-11T00:00:00Z,NaN,NaN,174.5672,NaN,NaN,NaN,NaN,NaN,9.38560009,7.34996223,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-3.0,2007-12-11T00:00:00Z,NaN,0.965432941913605,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-10.0,2007-12-11T00:00:00Z,NaN,NaN,182.0,NaN,NaN,NaN,NaN,NaN,4.016217,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-14.0,2007-12-11T00:00:00Z,NaN,NaN,197.0,NaN,NaN,NaN,NaN,NaN,3.605551,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-18.0,2007-12-11T00:00:00Z,NaN,NaN,212.0,NaN,NaN,NaN,NaN,NaN,3.471311,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-20.0,2007-12-11T00:00:00Z,NaN,NaN,NaN,NaN,NaN,25.3971004486084,33.2999992370605,32.4910888671875,NaN,7.34000015258789,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-22.0,2007-12-11T00:00:00Z,NaN,NaN,193.0,NaN,NaN,NaN,NaN,NaN,3.671512,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-26.0,2007-12-11T00:00:00Z,NaN,NaN,192.0,NaN,NaN,NaN,NaN,NaN,2.505993,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-30.0,2007-12-11T00:00:00Z,NaN,NaN,207.0,NaN,NaN,NaN,NaN,NaN,2.475884,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-34.0,2007-12-11T00:00:00Z,NaN,NaN,189.0,NaN,NaN,NaN,NaN,NaN,3.534119,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-38.0,2007-12-11T00:00:00Z,NaN,NaN,173.0,NaN,NaN,NaN,NaN,NaN,4.356604,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-42.0,2007-12-11T00:00:00Z,NaN,NaN,185.0,NaN,NaN,NaN,NaN,NaN,4.846648,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-46.0,2007-12-11T00:00:00Z,NaN,NaN,157.0,NaN,NaN,NaN,NaN,NaN,4.527693,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-50.0,2007-12-11T00:00:00Z,NaN,NaN,174.0,NaN,NaN,25.400972366333,33.2970008850098,32.4925308227539,3.255764,7.32000017166138,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-54.0,2007-12-11T00:00:00Z,NaN,NaN,220.0,NaN,NaN,NaN,NaN,NaN,0.72111,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-58.0,2007-12-11T00:00:00Z,NaN,NaN,323.0,NaN,NaN,NaN,NaN,NaN,3.956008,NaN,NaN,NaN,NaN,NaN,NaN\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    EDStatic.sosActive = oSosActive;
    // debugMode = oDebugMode;
  }

  /**
   * testNeracoos should work.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testNeracoos() throws Throwable {

    // String2.log("\n*** EDDTableFromSOS.testNeracoos");
    // testVerboseOn();
    int language = 0;
    boolean oSosActive = EDStatic.sosActive;
    EDStatic.sosActive = true;
    // boolean oDebugMode = debugMode;
    // debugMode = true;
    double tLon, tLat;
    String name, tName, results, expected, userDapQuery;
    String error = "";
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard to
    // check min:sec.

    EDDTable eddTable = (EDDTable) EDDTestDataset.getneracoosSos();

    // getEmpiricalMinMax just do once
    // useful for SOS: get alt values
    // eddTable.getEmpiricalMinMax(language, "2007-02-01", "2007-02-01", false,
    // true);
    // if (true) System.exit(1);

    // test sos-server values
    String2.log("nOfferings=" + eddTable.sosOfferings.size());
    String2.log(
        eddTable.sosOfferings.getString(0)
            + "  lon="
            + eddTable.sosMinLon.getNiceDouble(0)
            + ", "
            + eddTable.sosMaxLon.getNiceDouble(0)
            + " lat="
            + eddTable.sosMinLat.getNiceDouble(0)
            + ", "
            + eddTable.sosMaxLat.getNiceDouble(0)
            + " time="
            + eddTable.sosMinTime.getNiceDouble(0)
            + ", "
            + eddTable.sosMaxTime.getNiceDouble(0));
    // String2.log(String2.toCSSVString(eddTable.sosObservedProperties()));

    Test.ensureTrue(
        eddTable.sosOfferings.size() >= 9, // was 15
        "nOfferings=" + eddTable.sosOfferings.size()); // changes sometimes
    int which = eddTable.sosOfferings.indexOf("A01");
    String2.log("which=" + which);
    Test.ensureEqual(eddTable.sosOfferings.getString(which), "A01", "");
    // pre 2010-06-22 was -70.5600967407227
    // pre 2013-06-28 was -70.5655, "");
    // pre 2013-11-01 was -70.5645150078668
    // give up on =. Use almostEqual
    Test.ensureAlmostEqual(4, eddTable.sosMinLon.getNiceDouble(which), -70.565, "");
    Test.ensureAlmostEqual(4, eddTable.sosMaxLon.getNiceDouble(which), -70.565, "");
    // pre 2010-06-10 was 42.5261497497559
    // pre 2013-06-28 was 42.5232, "");
    // pre 2013-11-01 was 42.5223609076606
    // give up on =. Use almostEqual
    Test.ensureAlmostEqual(4, eddTable.sosMinLat.getNiceDouble(which), 42.522, "");
    Test.ensureAlmostEqual(4, eddTable.sosMaxLat.getNiceDouble(which), 42.522, "");

    Test.ensureEqual(eddTable.sosMinTime.getNiceDouble(which), 9.94734E8, "");
    Test.ensureEqual(eddTable.sosMaxTime.getNiceDouble(which), Double.NaN, "");
    // Test.ensureEqual(String2.toCSSVString(eddTable.sosObservedProperties()),
    // "http://marinemetadata.org/cf#sea_water_salinity, " +
    // //http://marinemetadata.org is GONE!
    // "http://marinemetadata.org/cf#sea_water_temperature",
    // NOW http://mmisw.org/ont/cf/parameter/sea_water_salinity
    // "");

    userDapQuery =
        "longitude,latitude,altitude,time,station_id,"
            + "sea_water_temperature,sea_water_salinity"
            + "&longitude>=-70&longitude<=-69&latitude>=43&latitude<=44"
            + "&time>=2007-07-04T00:00&time<=2007-07-04T01:00";
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            eddTable.datasetID() + "_Data",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        // before 2010-07-08 when I started using ALL_PLATFORMS and BBOX,
        // there was no data for D01 in the response
        "longitude,latitude,altitude,time,station_id,sea_water_temperature,sea_water_salinity\n"
            + "degrees_east,degrees_north,m,UTC,,degree_C,PSU\n"
            + "-69.8891420948262,43.7813123586226,-1.0,2007-07-04T00:00:00Z,D01,13.5,29.3161296844482\n"
            + "-69.8891420948262,43.7813123586226,-20.0,2007-07-04T00:00:00Z,D01,9.28999996185303,31.24924659729\n"
            + "-69.8891420948262,43.7813123586226,-35.0,2007-07-04T00:00:00Z,D01,8.30000019073486,NaN\n"
            + "-69.8891420948262,43.7813123586226,-1.0,2007-07-04T01:00:00Z,D01,13.5100002288818,29.192590713501\n"
            + "-69.8891420948262,43.7813123586226,-20.0,2007-07-04T01:00:00Z,D01,9.06999969482422,31.2218112945557\n"
            + "-69.8891420948262,43.7813123586226,-35.0,2007-07-04T01:00:00Z,D01,8.51000022888184,NaN\n"
            + "-69.9878833333333,43.7617166666667,-1.0,2007-07-04T00:00:00Z,D02,13.460000038147,29.9242057800293\n"
            + "-69.9878833333333,43.7617166666667,-10.0,2007-07-04T00:00:00Z,D02,11.8699998855591,31.2601585388184\n"
            + "-69.9878833333333,43.7617166666667,-1.0,2007-07-04T00:30:00Z,D02,13.4200000762939,29.9195117950439\n"
            + "-69.9878833333333,43.7617166666667,-1.0,2007-07-04T01:00:00Z,D02,13.3699998855591,29.930046081543\n"
            + "-69.9878833333333,43.7617166666667,-10.0,2007-07-04T01:00:00Z,D02,11.6599998474121,31.2559909820557\n"
            + "-69.3549346923828,43.7136993408203,-1.0,2007-07-04T00:00:00Z,E01,13.7600002288818,31.1920852661133\n"
            + "-69.3549346923828,43.7136993408203,-2.0,2007-07-04T00:00:00Z,E01,13.7034998,NaN\n"
            + "-69.3549346923828,43.7136993408203,-20.0,2007-07-04T00:00:00Z,E01,7.65000009536743,31.8228702545166\n"
            + "-69.3549346923828,43.7136993408203,-50.0,2007-07-04T00:00:00Z,E01,5.84700012207031,32.1141357421875\n"
            + "-69.3549346923828,43.7136993408203,-1.0,2007-07-04T00:30:00Z,E01,13.8900003433228,31.1868896484375\n"
            + "-69.3549346923828,43.7136993408203,-1.0,2007-07-04T01:00:00Z,E01,13.8500003814697,31.1843872070312\n"
            + "-69.3549346923828,43.7136993408203,-2.0,2007-07-04T01:00:00Z,E01,13.8292704,NaN\n"
            + "-69.3549346923828,43.7136993408203,-20.0,2007-07-04T01:00:00Z,E01,7.57000017166138,31.833927154541\n"
            + "-69.3549346923828,43.7136993408203,-50.0,2007-07-04T01:00:00Z,E01,5.81699991226196,32.0988731384277\n";
    Test.ensureEqual(results, expected, results);

    // data for mapExample (no time) just uses station table data
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "longitude,latitude,station_id&longitude>-70",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "NeraNT",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        // "longitude,latitude,station_id\n" +
        // "degrees_east,degrees_north,\n" +
        // "-69.9877,43.7628,D02\n" +
        // "-69.3578,43.7148,E01\n" +
        // "-69.32,43.7065,E02\n" + //added 2010-10-01
        // "-68.9982,44.0555,F01\n" +
        // "-68.1087,44.1058,I01\n" +
        // "-67.8716659545898,43.490140914917,M01\n" + //pre 2010-06-22 was
        // "-67.8798,43.4907,M01\n" +
        // "-65.9081802368164,42.3263664245605,N01\n"; //small changes 2010-08-05
        // starting 2011-02-15
        // "longitude,latitude,station_id\n" +
        // "degrees_east,degrees_north,\n" +
        // "-69.9877,43.7628,D02\n" +
        // "-69.3578,43.7148,E01\n" +
        // "-69.319580078125,43.7063484191895,E02\n" +
        // "-68.9977645874023,44.0548324584961,F01\n" +
        // "-68.1087,44.1058,I01\n" +
        // "-67.8716659545898,43.490140914917,M01\n" +
        // "-65.9081802368164,42.3263664245605,N01\n";
        // "longitude,latitude,station_id\n" + //starting on 2011-07-24
        // "degrees_east,degrees_north,\n" +
        // "-69.9877,43.7628,D02\n" +
        // "-69.3552169799805,43.714298248291,E01\n" +
        // "-69.319580078125,43.7063484191895,E02\n" +
        // "-68.9977645874023,44.0548324584961,F01\n" +
        // "-68.1087,44.1058,I01\n" +
        // "-67.8798,43.4907,M01\n" +
        // "-65.907,42.3303,N01\n"; //pre 2011-09-05 was
        // "-65.9081802368164,42.3263664245605,N01\n";
        // "longitude,latitude,station_id\n" + //starting 2011-12-16
        // "degrees_east,degrees_north,\n" +
        // "-63.4082,44.5001,CDIP176\n" +
        // "-69.9877,43.7628,D02\n" +
        // "-69.3552169799805,43.714298248291,E01\n" +
        // "-69.319580078125,43.7063484191895,E02\n" +
        // "-68.9977645874023,44.0548324584961,F01\n" +
        // "-68.1087,44.1058,I01\n" +
        // "-67.8798,43.4907,M01\n" +
        // "-65.907,42.3303,N01\n" +
        // "-54.688,46.9813,SMB-MO-01\n" +
        // "-54.1317,47.3255,SMB-MO-04\n" +
        // "-54.0488,47.7893,SMB-MO-05\n";
        "longitude,latitude,station_id\n"
            + "degrees_east,degrees_north,\n"
            + "-63.\\d+,44.\\d+,CDIP176\n"
            + "-69.\\d+,43.\\d+,D02\n"
            + "-69.\\d+,43.\\d+,E01\n"
            + "-69.\\d+,43.\\d+,E02\n"
            + "-68.\\d+,44.\\d+,F01\n"
            + "-68.\\d+,44.\\d+,F02\n"
            + "-68.\\d+,44.\\d+,I01\n"
            + "-67.\\d+,43.\\d+,M01\n"
            + "-65.\\d+,42.\\d+,N01\n"
            + "-54.\\d+,46.\\d+,SMB-MO-01\n"
            + "-54.\\d+,47.\\d+,SMB-MO-04\n"
            + "-54.\\d+,47.\\d+,SMB-MO-05\n";
    Test.testLinesMatch(results, expected, "\nresults=\n" + results);

    // data for mapExample (with time
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "longitude,latitude&time=2007-12-11",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "NeraWT",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        // before 2010-07-08 when I started using ALL_PLATFORMS and BBOX,
        // there was no data for D01 (-69.8876, 43.7823) in the response
        "longitude,latitude\n"
            + "degrees_east,degrees_north\n"
            + "-70.5652267750436,42.5227725835475\n"
            + "-70.4273380011109,43.18053298369\n"
            + "-70.0584772405338,43.5671424043496\n"
            + "-69.8876,43.7823\n"
            + "-69.9878833333333,43.7617166666667\n"
            + "-69.3563516790217,43.7138879949396\n"
            + "-68.9979781363549,44.0556086654547\n"
            + "-68.1087458631928,44.1053852961787\n"
            + "-67.0122575759888,44.8892910480499\n"; // this line added 2011-12-16
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // data for all variables
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&station_id=\"A01\"&time=2007-12-11",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "neraAV",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "longitude,latitude,station_id,altitude,time,air_temperature,chlorophyll,direction_of_sea_water_velocity,dominant_wave_period,sea_level_pressure,sea_water_density,sea_water_electrical_conductivity,sea_water_salinity,sea_water_speed,sea_water_temperature,wave_height,visibility_in_air,wind_from_direction,wind_gust,wind_speed\n"
            + "degrees_east,degrees_north,,m,UTC,degree_C,mg m-3,degrees_true,s,hPa,kg m-3,S m-1,PSU,cm s-1,degree_C,m,m,degrees_true,m s-1,m s-1\n"
            + "-70.5652267750436,42.5227725835475,A01,4.0,2007-12-11T00:00:00Z,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,332.100006103516,3.94600009918213,3.36700010299683\n"
            + "-70.5652267750436,42.5227725835475,A01,3.0,2007-12-11T00:00:00Z,0.899999976158142,NaN,NaN,NaN,1024.82849121094,NaN,NaN,NaN,NaN,NaN,NaN,2920.6796875,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,0.0,2007-12-11T00:00:00Z,NaN,NaN,NaN,5.33333349,NaN,NaN,NaN,NaN,NaN,NaN,0.644578338,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-1.0,2007-12-11T00:00:00Z,NaN,NaN,NaN,NaN,NaN,25.3888969421387,33.2410011291504,32.4736976623535,NaN,7.30000019073486,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-2.0,2007-12-11T00:00:00Z,NaN,NaN,174.5672,NaN,NaN,NaN,NaN,NaN,9.38560009,7.34996223,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-3.0,2007-12-11T00:00:00Z,NaN,0.965432941913605,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-10.0,2007-12-11T00:00:00Z,NaN,NaN,182.0,NaN,NaN,NaN,NaN,NaN,4.016217,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-14.0,2007-12-11T00:00:00Z,NaN,NaN,197.0,NaN,NaN,NaN,NaN,NaN,3.605551,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-18.0,2007-12-11T00:00:00Z,NaN,NaN,212.0,NaN,NaN,NaN,NaN,NaN,3.471311,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-20.0,2007-12-11T00:00:00Z,NaN,NaN,NaN,NaN,NaN,25.3971004486084,33.2999992370605,32.4910888671875,NaN,7.34000015258789,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-22.0,2007-12-11T00:00:00Z,NaN,NaN,193.0,NaN,NaN,NaN,NaN,NaN,3.671512,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-26.0,2007-12-11T00:00:00Z,NaN,NaN,192.0,NaN,NaN,NaN,NaN,NaN,2.505993,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-30.0,2007-12-11T00:00:00Z,NaN,NaN,207.0,NaN,NaN,NaN,NaN,NaN,2.475884,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-34.0,2007-12-11T00:00:00Z,NaN,NaN,189.0,NaN,NaN,NaN,NaN,NaN,3.534119,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-38.0,2007-12-11T00:00:00Z,NaN,NaN,173.0,NaN,NaN,NaN,NaN,NaN,4.356604,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-42.0,2007-12-11T00:00:00Z,NaN,NaN,185.0,NaN,NaN,NaN,NaN,NaN,4.846648,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-46.0,2007-12-11T00:00:00Z,NaN,NaN,157.0,NaN,NaN,NaN,NaN,NaN,4.527693,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-50.0,2007-12-11T00:00:00Z,NaN,NaN,174.0,NaN,NaN,25.400972366333,33.2970008850098,32.4925308227539,3.255764,7.32000017166138,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-54.0,2007-12-11T00:00:00Z,NaN,NaN,220.0,NaN,NaN,NaN,NaN,NaN,0.72111,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "-70.5652267750436,42.5227725835475,A01,-58.0,2007-12-11T00:00:00Z,NaN,NaN,323.0,NaN,NaN,NaN,NaN,NaN,3.956008,NaN,NaN,NaN,NaN,NaN,NaN\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
    EDStatic.sosActive = oSosActive;
    // debugMode = oDebugMode;
  }

  /**
   * testTamu doesn't work yet (as of 2009-09-30). It doesn't support eventTime. It requires
   * RESPONSE_FORMAT instead of responseFormat. I sent email to tcook@nsstc.uah.edu .
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testTamu() throws Throwable {
    // String2.log("\n*** EDDTableFromSOS.testTamu");
    // testVerboseOn();

    int language = 0;
    boolean oSosActive = EDStatic.sosActive;
    EDStatic.sosActive = true;
    // boolean oDebugMode = debugMode;
    // debugMode = true;
    try {
      String name, tName, results, expected, userDapQuery;
      String error = "";
      String today =
          Calendar2.getCurrentISODateTimeStringZulu()
              .substring(0, 14); // 14 is enough to check hour. Hard
      // to check min:sec.

      EDDTable eddTable = (EDDTable) EDDTestDataset.gettamuSos();

      // data for all variables
      tName =
          eddTable.makeNewFileForDapQuery(
              language,
              null,
              null,
              "&time=2009-07-11", // &station_id=\"A01\"",
              EDStatic.fullTestCacheDirectory,
              eddTable.className() + "tamu",
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      // String2.log(results);
      expected = "zztop\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);

    } finally {
      EDStatic.sosActive = oSosActive;
      // debugMode = oDebugMode;
    }
  }

  /**
   * This tests that ensureValid throws exception if 2 dataVariables use the same sourceName. These
   * tests are in EDDTableFromSOS because EDDTableFromFiles has a separate test.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void test2DVSameSource() throws Throwable {

    // String2.log("\n*** EDDTableFromSOS.test2DVSameSource()\n");
    String error = "shouldn't happen";
    try {
      EDDGrid eddGrid = (EDDGrid) EDDTestDataset.gettabletest2DVSameSource();
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      error = String2.split(MustBe.throwableToString(t), '\n')[0];
      int errorPo = error.indexOf("ERROR"); // the line number in datasets.xml varies
      if (errorPo > 0) error = error.substring(errorPo);
    }

    Test.ensureEqual(
        error,
        "ERROR: Duplicate dataVariableSourceNames: [5] and [7] are both \"sensor_id\".",
        "Unexpected error message:\n" + error);
  }

  /**
   * This tests that ensureValid throws exception if 2 dataVariables use the same destinationName.
   * These tests are in EDDTableFromSOS because EDDTableFromFiles has a separate test.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void test2DVSameDestination() throws Throwable {
    // String2.log("\n*** EDDTableFromSOS.test2DVSameDestination()\n");

    String error = "shouldn't happen";
    try {
      EDDGrid eddGrid = (EDDGrid) EDDTestDataset.gettabletest2DVSameDestination();
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      error = String2.split(MustBe.throwableToString(t), '\n')[0];
      int errorPo = error.indexOf("ERROR"); // the line number in datasets.xml varies
      if (errorPo > 0) error = error.substring(errorPo);
    }

    Test.ensureEqual(
        error,
        "ERROR: Duplicate dataVariableDestinationNames: [5] and [7] are both \"sensor_id\".",
        "Unexpected error message:\n" + error);
  }

  /**
   * GCOOS SOS sample URLs: http://data.gcoos.org/52N_SOS.php
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testGcoos52N() throws Throwable {
    boolean useCachedInfo = true;

    // boolean oDebugMode = debugMode;
    // debugMode = false;
    // testVerboseOn();
    String dir = EDStatic.fullTestCacheDirectory;
    EDDTable eddTable;
    String name, tName, results, tResults, expected, userDapQuery;
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard to
    // check min:sec.

    // one time
    String tSourceUrl = "http://data.gcoos.org:8080/52nSOS/sos/kvp";
    // String2.log(generateDatasetsXml(useCachedInfo,
    // tSourceUrl, "1.0.0", "IOOS_52N"));
    // String2.log(generateDatasetsXmlFromIOOS(useCachedInfo,
    // tSourceUrl, "1.0.0", "IOOS_52N"));
    String oneIoosUrl =
        tSourceUrl
            + "?service=SOS&version=1.0.0&request=GetObservation"
            +
            // "&offering=urn:ioos:network:gcoos:all" + SSR times out after 10 minutes
            "&offering=urn:ioos:station:comps:42022"
            + // gml:id has '_', name has ':'
            "&observedProperty="
            + "http://mmisw.org/ont/cf/parameter/air_temperature,"
            + "http://mmisw.org/ont/cf/parameter/air_pressure,"
            + "http://mmisw.org/ont/cf/parameter/wind_speed,"
            + "http://mmisw.org/ont/cf/parameter/wind_speed_of_gust,"
            + "http://mmisw.org/ont/cf/parameter/wind_to_direction"
            + "&responseFormat=text/xml;+subtype=\"om/1.0.0/profiles/ioos_sos/1.0\""
            + "&eventTime=2015-02-25T00:00:00Z/2015-02-25T01:00:00Z";
    // csv part of response has interlaced info! e.g., for just 2 properties:
    // 2015-02-25T00:05:00.000Z,comps_42022_airpressure,1013.98
    // 2015-02-25T00:05:00.000Z,comps_42022_airtemperature,18.94
    // 2015-02-25T00:35:00.000Z,comps_42022_airpressure,1013.99
    // 2015-02-25T00:35:00.000Z,comps_42022_airtemperature,18.53

    String2.log(
        EDDTableFromSOS.generateDatasetsXmlFromOneIOOS(
            useCachedInfo,
            oneIoosUrl,
            "1.0.0",
            "IOOS_52N",
            "http://data.gcoos.org/",
            "GCOOS",
            "[standard]")); // infoUrl, institution, licence

    /*
     * Not finished because 52N response format is weird: interlaced lines.
     *
     * //test readIoos52NXmlDataTable()
     * Table table =
     * readIoos52NXmlDataTable(File2.getDecompressedBufferedInputStream(
     * "/programs/sos/ioos52NgcoosAirPressure.xml"));
     * results = table.toCSVString(5);
     * expected =
     * "{\n" +
     * "dimensions:\n" +
     * "\trow = 5312 ;\n" +
     * "\ttime_strlen = 24 ;\n" +
     * "\tstation_id_strlen = 14 ;\n" +
     * "variables:\n" +
     * "\tchar time(row, time_strlen) ;\n" +
     * "\tchar station_id(row, station_id_strlen) ;\n" +
     * "\tdouble air_pressure(row) ;\n" +
     * "\n" +
     * "// global attributes:\n" +
     * "}\n" +
     * "time,station_id,air_pressure\n" +
     * "2015-02-25T00:05:00.000Z,comps_42013,1013.93\n" +
     * "2015-02-25T00:05:00.000Z,comps_42022,1013.98\n" +
     * "2015-02-25T00:05:00.000Z,comps_42023,1014.55\n" +
     * "2015-02-25T00:06:00.000Z,comps_fhpf1,1016.0\n" +
     * "2015-02-25T00:06:00.000Z,comps_nfbf1,1016.56\n";
     * Test.ensureEqual(results, expected, "results=" + results);
     * Test.ensureEqual(table.nRows(), 5312, "results=" + results);
     * Test.ensureEqual(table.getStringData(0, 5311), "2015-02-26T00:00:00.000Z",
     * "data(0, 5311)");
     * Test.ensureEqual(table.getStringData(1, 5311), "nos_8771341",
     * "data(1, 5311)");
     * Test.ensureEqual(table.getDoubleData(2, 5311), 1009.10, "data(2, 5311)");
     *
     *
     * testQuickRestart = true;
     * eddTable = (EDDTable)oneFromDatasetsXml(null, "gcoosSosAirPressure");
     *
     * tName = eddTable.makeNewFileForDapQuery(language, null, null, "", dir,
     * "gcoosSosAirPressure_Entire", ".das");
     * results = File2.directReadFrom88591File(dir + tName);
     * //String2.log(results);
     * expected =
     * "Attributes {\n" +
     * " s {\n" +
     * "  longitude {\n" +
     * "    String _CoordinateAxisType \"Lon\";\n" +
     * "    Float64 actual_range -97.492, -80.033;\n" +
     * "    String axis \"X\";\n" +
     * "    String ioos_category \"Location\";\n" +
     * "    String long_name \"Longitude\";\n" +
     * "    String standard_name \"longitude\";\n" +
     * "    String units \"degrees_east\";\n" +
     * "  }\n" +
     * "  latitude {\n" +
     * "    String _CoordinateAxisType \"Lat\";\n" +
     * "    Float64 actual_range 16.834, 30.766;\n" +
     * "    String axis \"Y\";\n" +
     * "    String ioos_category \"Location\";\n" +
     * "    String long_name \"Latitude\";\n" +
     * "    String standard_name \"latitude\";\n" +
     * "    String units \"degrees_north\";\n" +
     * "  }\n" +
     * "  station_id {\n" +
     * "    String cf_role \"timeseries_id\";\n" +
     * "    String ioos_category \"Identifier\";\n" +
     * "    String long_name \"Station ID\";\n" +
     * "  }\n" +
     * "  altitude {\n" +
     * "    String _CoordinateAxisType \"Height\";\n" +
     * "    String _CoordinateZisPositive \"up\";\n" +
     * "    String axis \"Z\";\n" +
     * "    String ioos_category \"Location\";\n" +
     * "    String long_name \"Altitude\";\n" +
     * "    String positive \"up\";\n" +
     * "    String standard_name \"altitude\";\n" +
     * "    String units \"m\";\n" +
     * "  }\n" +
     * "  time {\n" +
     * "    String _CoordinateAxisType \"Time\";\n" +
     * "    Float64 actual_range 9.08541e+8, NaN;\n" +
     * "    String axis \"T\";\n" +
     * "    String ioos_category \"Time\";\n" +
     * "    String long_name \"Time\";\n" +
     * "    String standard_name \"time\";\n" +
     * "    String time_origin \"01-JAN-1970 00:00:00\";\n" +
     * "    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
     * "  }\n" +
     * "  air_pressure {\n" +
     * "    Float64 colorBarMaximum 1050.0;\n" +
     * "    Float64 colorBarMinimum 950.0;\n" +
     * "    String ioos_category \"Pressure\";\n" +
     * "    String long_name \"Air Pressure\";\n" +
     * "    String observedProperty \"http://mmisw.org/ont/cf/parameter/air_pressure\";\n"
     * +
     * "    String standard_name \"air_pressure\";\n" +
     * "    String units \"hPa\";\n" +
     * "  }\n" +
     * " }\n" +
     * "  NC_GLOBAL {\n" +
     * "    String cdm_data_type \"TimeSeries\";\n" +
     * "    String cdm_timeseries_variables \"station_id, longitude, latitude\";\n"
     * +
     * "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
     * "    Float64 Easternmost_Easting -80.033;\n" +
     * "    String featureType \"TimeSeries\";\n" +
     * "    Float64 geospatial_lat_max 30.766;\n" +
     * "    Float64 geospatial_lat_min 16.834;\n" +
     * "    String geospatial_lat_units \"degrees_north\";\n" +
     * "    Float64 geospatial_lon_max -80.033;\n" +
     * "    Float64 geospatial_lon_min -97.492;\n" +
     * "    String geospatial_lon_units \"degrees_east\";\n" +
     * "    String geospatial_vertical_positive \"up\";\n" +
     * "    String geospatial_vertical_units \"m\";\n" +
     * "    String history \"" + today;
     * //T15:47:07Z http://data.gcoos.org:8080/52nSOS/sos/kvp
     * //2015-03-09T15:47:07Z
     * http://localhost:8080/cwexperimental/tabledap/gcoosSosAirPressure.das
     * tResults = results.substring(0, Math.min(results.length(),
     * expected.length()));
     * Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
     *
     * expected =
     * "    String infoUrl \"http://data.gcoos.org/\";\n" +
     * "    String institution \"GCOOS\";\n" +
     * "    String license \"The data may be used and redistributed for free but is not intended\n"
     * +
     * "for legal use, since it may contain inaccuracies. Neither the data\n" +
     * "Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
     * "of their employees or contractors, makes any warranty, express or\n" +
     * "implied, including warranties of merchantability and fitness for a\n" +
     * "particular purpose, or assumes any legal liability for the accuracy,\n" +
     * "completeness, or usefulness, of this information.\";\n" +
     * "    Float64 Northernmost_Northing 30.766;\n" +
     * "    String sourceUrl \"http://data.gcoos.org:8080/52nSOS/sos/kvp\";\n" +
     * "    Float64 Southernmost_Northing 16.834;\n" +
     * "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
     * "    String subsetVariables \"station_id, longitude, latitude\";\n" +
     * "    String summary \"This SOS server is part of the IOOS DIF SOS Project.  The stations in this dataset have air_pressure data.\n"
     * +
     * "\n" +
     * "Because of the nature of SOS requests, requests for data MUST include constraints for the longitude, latitude, time, and/or station_id variables.\";\n"
     * +
     * "    String time_coverage_start \"1998-10-16T12:30:00Z\";\n" +
     * "    String title \"GCOOS SOS - air_pressure\";\n" +
     * "    Float64 Westernmost_Easting -97.492;\n" +
     * "  }\n" +
     * "}\n";
     * int tPo = results.indexOf(expected.substring(0, 20));
     * Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
     * Test.ensureEqual(
     * results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
     * expected, "results=\n" + results);
     *
     * //*** test getting dds for entire dataset
     * tName = eddTable.makeNewFileForDapQuery(language, null, null, "", dir,
     * "gcoosSosAirPressure_Entire", ".dds");
     * results = File2.directReadFrom88591File(dir + tName);
     * //String2.log(results);
     * expected =
     * "Dataset {\n" +
     * "  Sequence {\n" +
     * "    Float64 longitude;\n" +
     * "    Float64 latitude;\n" +
     * "    String station_id;\n" +
     * "    Float64 altitude;\n" +
     * "    Float64 time;\n" +
     * "    Float64 air_pressure;\n" +
     * "  } s;\n" +
     * "} s;\n";
     * Test.ensureEqual(results, expected, "\nresults=\n" + results);
     *
     * String2.
     * log("\n*** EDDTableFromSOS nos AirTemperature test get one station .CSV data\n"
     * );
     * tName = eddTable.makeNewFileForDapQuery(language, null, null,
     * "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:8419317\"&time>=2006-01-01T00&time<=2006-01-01T01",
     * dir, eddTable.className() + "_nosSosATemp", ".csv");
     * results = File2.directReadFrom88591File(dir + tName);
     * expected =
     * "longitude,latitude,station_id,altitude,time,sensor_id,air_temperature\n" +
     * "degrees_east,degrees_north,,m,UTC,,degree_C\n" +
     * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-3.9\n"
     * +
     * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-3.9\n"
     * +
     * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-3.9\n"
     * +
     * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-3.9\n"
     * +
     * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-3.9\n"
     * +
     * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-3.9\n"
     * +
     * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-3.9\n"
     * +
     * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-4.0\n"
     * +
     * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-4.0\n"
     * +
     * "-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-4.1\n";
     * Test.ensureEqual(results, expected, "RESULTS=\n" + results);
     *
     * /*
     *
     * String2.
     * log("\n*** EDDTableFromSOS nos AirTemperature test get one station .CSV data\n"
     * );
     * tName = eddTable.makeNewFileForDapQuery(language, null, null, //1612340, NaN,
     * 2008-10-26
     * "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:1612340\"&time>=2008-10-26T00&time<2008-10-26T01",
     * dir, eddTable.className() + "_nosSosATemp", ".csv");
     * results = File2.directReadFrom88591File(dir + tName);
     * expected =
     * "longitude,latitude,station_id,altitude,time,sensor_id,air_temperature\n" +
     * "degrees_east,degrees_north,,m,UTC,,degree_C\n" +
     * "-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.9\n"
     * +
     * "-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8\n"
     * +
     * "-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.7\n"
     * +
     * "-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8\n"
     * +
     * "-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.7\n"
     * +
     * "-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8\n"
     * +
     * "-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.7\n"
     * +
     * "-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8\n"
     * +
     * "-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8\n"
     * +
     * "-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.7\n";
     * Test.ensureEqual(results, expected, "RESULTS=\n" + results);
     *
     * String2.log("\n*** EDDTableFromSOS nos AirTemperature .das\n");
     * String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14);
     * //14 is enough to check hour. Hard to check min:sec.
     * tName = eddTable.makeNewFileForDapQuery(language, null, null, "", dir,
     * eddTable.className() + "_nosSosATemp", ".das");
     * results = File2.directReadFrom88591File(dir + tName);
     * expected =
     * "Attributes {\n" +
     * " s {\n" +
     * "  longitude {\n" +
     * "    String _CoordinateAxisType \"Lon\";\n" +
     * "    Float64 actual_range -177.36, 166.618;\n" +
     * "    String axis \"X\";\n" +
     * "    String ioos_category \"Location\";\n" +
     * "    String long_name \"Longitude\";\n" +
     * "    String standard_name \"longitude\";\n" +
     * "    String units \"degrees_east\";\n" +
     * "  }\n" +
     * "  latitude {\n" +
     * "    String _CoordinateAxisType \"Lat\";\n" +
     * "    Float64 actual_range -14.28, 70.4;\n" +
     * "    String axis \"Y\";\n" +
     * "    String ioos_category \"Location\";\n" +
     * "    String long_name \"Latitude\";\n" +
     * "    String standard_name \"latitude\";\n" +
     * "    String units \"degrees_north\";\n" +
     * "  }\n" +
     * "  station_id {\n" +
     * "    String cf_role \"timeseries_id\";\n" +
     * "    String ioos_category \"Identifier\";\n" +
     * "    String long_name \"Station ID\";\n" +
     * "  }\n" +
     * "  altitude {\n" +
     * "    String _CoordinateAxisType \"Height\";\n" +
     * "    String _CoordinateZisPositive \"up\";\n" +
     * "    String axis \"Z\";\n" +
     * "    String ioos_category \"Location\";\n" +
     * "    String long_name \"Altitude\";\n" +
     * "    String positive \"up\";\n" +
     * "    String standard_name \"altitude\";\n" +
     * "    String units \"m\";\n" +
     * "  }\n" +
     * "  time {\n" +
     * "    String _CoordinateAxisType \"Time\";\n" +
     * "    Float64 actual_range -3.6757152e+9, NaN;\n" +
     * "    String axis \"T\";\n" +
     * "    String ioos_category \"Time\";\n" +
     * "    String long_name \"Time\";\n" +
     * "    String standard_name \"time\";\n" +
     * "    String time_origin \"01-JAN-1970 00:00:00\";\n" +
     * "    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
     * "  }\n" +
     * "  sensor_id {\n" +
     * "    String ioos_category \"Identifier\";\n" +
     * "    String long_name \"Sensor ID\";\n" +
     * "    String observedProperty \"http://mmisw.org/ont/cf/parameter/air_temperature\";\n"
     * +
     * "  }\n" +
     * "  air_temperature {\n" +
     * "    Float64 colorBarMaximum 40.0;\n" +
     * "    Float64 colorBarMinimum -10.0;\n" +
     * "    String ioos_category \"Temperature\";\n" +
     * "    String long_name \"Air Temperature\";\n" +
     * "    String observedProperty \"http://mmisw.org/ont/cf/parameter/air_temperature\";\n"
     * +
     * "    String standard_name \"air_temperature\";\n" +
     * "    String units \"degree_C\";\n" +
     * "  }\n" +
     * " }\n" +
     * "  NC_GLOBAL {\n" +
     * "    String cdm_data_type \"TimeSeries\";\n" +
     * "    String cdm_timeseries_variables \"station_id, longitude, latitude, sensor_id\";\n"
     * +
     * "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
     * "    Float64 Easternmost_Easting 166.618;\n" +
     * "    String featureType \"TimeSeries\";\n" +
     * "    Float64 geospatial_lat_max 70.4;\n" +
     * "    Float64 geospatial_lat_min -14.28;\n" +
     * "    String geospatial_lat_units \"degrees_north\";\n" +
     * "    Float64 geospatial_lon_max 166.618;\n" +
     * "    Float64 geospatial_lon_min -177.36;\n" +
     * "    String geospatial_lon_units \"degrees_east\";\n" +
     * "    String geospatial_vertical_positive \"up\";\n" +
     * "    String geospatial_vertical_units \"m\";\n" +
     * "    String history \"" + today;
     * //+ " https://opendap.co-ops.nos.noaa.gov/ioos-dif-sos"; //"-test"
     * Test.ensureEqual(results.substring(0, expected.length()), expected,
     * "RESULTS=\n" + results);
     *
     * expected =
     * "    Float64 Southernmost_Northing -14.28;\n" +
     * "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
     * "    String subsetVariables \"station_id, longitude, latitude\";\n" +
     * "    String summary \"The NOAA NOS SOS server is part of the IOOS DIF SOS Project.  The stations in this dataset have air temperature data.  ****These services are for testing and evaluation use only****\n"
     * +
     * "\n" +
     * "Because of the nature of SOS requests, requests for data MUST include constraints for the longitude, latitude, time, and/or station_id variables.\";\n"
     * +
     * "    String time_coverage_start \"1853-07-10T00:00:00Z\";\n" +
     * "    String title \"NOAA NOS SOS, EXPERIMENTAL - Air Temperature\";\n" +
     * "    Float64 Westernmost_Easting -177.36;\n" +
     * "  }\n" +
     * "}\n";
     * int po = Math.max(0, results.indexOf(expected.substring(0, 30)));
     * Test.ensureEqual(results.substring(po), expected, "RESULTS=\n" + results);
     * /*
     */

    // debugMode = oDebugMode;
  }

  /** testGenerateDatasetsXml */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testGenerateDatasetsXml() throws Throwable {
    boolean useCachedInfo = true;

    // testVerboseOn();
    int language = 0;
    // debugMode = true;
    // String2.log("\n*** EDDTableFromSOS.testGenerateDatasetsXml");

    String results =
        EDDTableFromSOS.generateDatasetsXml(
                useCachedInfo, "https://sdf.ndbc.noaa.gov/sos/server.php", "1.0.0", "IOOS_NDBC")
            + "\n";

    String expected1 =
        "<!-- You have to choose which observedProperties will be used for this dataset.\n"
            + "\n"
            + "\n"
            + "   n  Station (shortened)                 Has ObservedProperty\n"
            + "----  ----------------------------------  ------------------------------------------------------------\n"
            + "   0  urn:ioos:network:noaa.nws.ndbc:all  ABCDEFGHI\n"
            + "   1  wmo:0y2w3                           AB    G I\n"
            + "   2  wmo:14040                           AB      I\n";

    Test.ensureEqual(results.substring(0, expected1.length()), expected1, "results=\n" + results);

    String expected2 =
        " id  ObservedProperty\n"
            + "---  --------------------------------------------------\n"
            + "  A  http://mmisw.org/ont/cf/parameter/air_temperature\n"
            + "  B  http://mmisw.org/ont/cf/parameter/air_pressure_at_sea_level\n"
            + "  C  http://mmisw.org/ont/cf/parameter/sea_water_electrical_conductivity\n"
            + "  D  http://mmisw.org/ont/cf/parameter/currents\n"
            + "  E  http://mmisw.org/ont/cf/parameter/sea_water_salinity\n"
            + "  F  http://mmisw.org/ont/cf/parameter/sea_floor_depth_below_sea_surface\n"
            + "  G  http://mmisw.org/ont/cf/parameter/sea_water_temperature\n"
            + "  H  http://mmisw.org/ont/cf/parameter/waves\n"
            + "  I  http://mmisw.org/ont/cf/parameter/winds\n"
            + "\n"
            + "NOTE! For SOS datasets, you must look at the observedProperty's\n"
            + "phenomenaDictionary URL (or an actual GetObservations response)\n"
            + "to see which dataVariables will be returned for a given phenomenon.\n"
            + "(longitude, latitude, altitude, and time are handled separately.)\n"
            + "-->\n"
            + "<dataset type=\"EDDTableFromSOS\" datasetID=\"noaa_ndbc_6a36_2db7_1aab\" active=\"true\">\n"
            + "    <sourceUrl>https://sdf.ndbc.noaa.gov/sos/server.php</sourceUrl>\n"
            + "    <sosVersion>1.0.0</sosVersion>\n"
            + "    <sosServerType>IOOS_NDBC</sosServerType>\n"
            + "    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n"
            + "    <observationOfferingIdRegex>.+</observationOfferingIdRegex>\n"
            + "    <requestObservedPropertiesSeparately>true</requestObservedPropertiesSeparately>\n"
            + "    <longitudeSourceName>longitude</longitudeSourceName>\n"
            + "    <latitudeSourceName>latitude</latitudeSourceName>\n"
            + "    <altitudeSourceName>???depth???ioos:VerticalPosition</altitudeSourceName>\n"
            + "    <altitudeMetersPerSourceUnit>-1</altitudeMetersPerSourceUnit>\n"
            + "    <timeSourceName>time</timeSourceName>\n"
            + "    <timeSourceFormat>yyyy-MM-dd'T'HH:mm:ssZ</timeSourceFormat>\n"
            + "    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below,\n"
            + "      notably, cdm_timeseries_variables, subsetVariables.\n"
            + "    -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cdm_data_type\">TimeSeries</att>\n"
            + "        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
            + "        <att name=\"creator_email\">webmaster.ndbc@noaa.gov</att>\n"
            + "        <att name=\"creator_name\">NOAA NDBC</att>\n"
            + "        <att name=\"creator_type\">institution</att>\n"
            + "        <att name=\"creator_url\">https://www.ndbc.noaa.gov/</att>\n"
            + "        <att name=\"infoUrl\">https://sdf.ndbc.noaa.gov/</att>\n"
            + "        <att name=\"institution\">NOAA NDBC</att>\n"
            + "        <att name=\"keywords\">buoy, center, data, national, ndbc, noaa, server.php, sos</att>\n"
            + "        <att name=\"license\">[standard]</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n"
            + "        <att name=\"summary\">National Data Buoy Center SOS. NOAA NDBC data from https://sdf.ndbc.noaa.gov/sos/server.php.das .</att>\n"
            + "        <att name=\"title\">National Data Buoy Center SOS (server.php)</att>\n"
            + "    </addAttributes>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>air_temperature</sourceName>\n"
            + "        <destinationName>air_temperature</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">40.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-10.0</att>\n"
            + "            <att name=\"ioos_category\">Temperature</att>\n"
            + "            <att name=\"long_name\">Air Temperature</att>\n"
            + "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/air_temperature</att>\n"
            + "            <att name=\"standard_name\">air_temperature</att>\n"
            + "            <att name=\"units\">???</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>air_pressure_at_sea_level</sourceName>\n"
            + "        <destinationName>air_pressure_at_sea_level</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">1050.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">950.0</att>\n"
            + "            <att name=\"ioos_category\">Pressure</att>\n"
            + "            <att name=\"long_name\">Air Pressure At Sea Level</att>\n"
            + "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/air_pressure_at_sea_level</att>\n"
            + "            <att name=\"standard_name\">air_pressure_at_sea_level</att>\n"
            + "            <att name=\"units\">???</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>sea_water_electrical_conductivity</sourceName>\n"
            + "        <destinationName>sea_water_electrical_conductivity</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">40.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">30.0</att>\n"
            + "            <att name=\"ioos_category\">Salinity</att>\n"
            + "            <att name=\"long_name\">Sea Water Electrical Conductivity</att>\n"
            + "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/sea_water_electrical_conductivity</att>\n"
            + "            <att name=\"standard_name\">sea_water_electrical_conductivity</att>\n"
            + "            <att name=\"units\">???</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>currents</sourceName>\n"
            + "        <destinationName>currents</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Currents</att>\n"
            + "            <att name=\"long_name\">Currents</att>\n"
            + "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/currents</att>\n"
            + "            <att name=\"standard_name\">currents</att>\n"
            + "            <att name=\"units\">???</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>sea_water_salinity</sourceName>\n"
            + "        <destinationName>sea_water_salinity</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">37.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">32.0</att>\n"
            + "            <att name=\"ioos_category\">Salinity</att>\n"
            + "            <att name=\"long_name\">Sea Water Practical Salinity</att>\n"
            + "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/sea_water_salinity</att>\n"
            + "            <att name=\"standard_name\">sea_water_salinity</att>\n"
            + "            <att name=\"units\">???</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>sea_floor_depth_below_sea_surface</sourceName>\n"
            + "        <destinationName>sea_floor_depth_below_sea_surface</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">8000.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-8000.0</att>\n"
            + "            <att name=\"colorBarPalette\">TopographyDepth</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Sea Floor Depth Below Sea Surface</att>\n"
            + "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/sea_floor_depth_below_sea_surface</att>\n"
            + "            <att name=\"standard_name\">sea_floor_depth_below_sea_surface</att>\n"
            + "            <att name=\"units\">???</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>sea_water_temperature</sourceName>\n"
            + "        <destinationName>sea_water_temperature</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Temperature</att>\n"
            + "            <att name=\"long_name\">Sea Water Temperature</att>\n"
            + "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/sea_water_temperature</att>\n"
            + "            <att name=\"standard_name\">sea_water_temperature</att>\n"
            + "            <att name=\"units\">???</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>waves</sourceName>\n"
            + "        <destinationName>waves</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Surface Waves</att>\n"
            + "            <att name=\"long_name\">Waves</att>\n"
            + "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/waves</att>\n"
            + "            <att name=\"standard_name\">waves</att>\n"
            + "            <att name=\"units\">???</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>winds</sourceName>\n"
            + "        <destinationName>winds</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Wind</att>\n"
            + "            <att name=\"long_name\">Winds</att>\n"
            + "            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/winds</att>\n"
            + "            <att name=\"standard_name\">winds</att>\n"
            + "            <att name=\"units\">???</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n";

    int po = results.indexOf(expected2.substring(0, 40));
    if (po < 0) String2.log(results);
    Test.ensureEqual(
        results.substring(po, Math.min(results.length(), po + expected2.length())),
        expected2,
        "results=\n" + results);

    // GenerateDatasetsXml
    String gdxResults =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {
                  "-verbose",
                  "EDDTableFromSOS",
                  "https://sdf.ndbc.noaa.gov/sos/server.php",
                  "1.0.0",
                  "IOOS_NDBC",
                  "-1"
                }, // defaultStandardizeWhat
                false); // doIt loop?
    Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");
  }

  /**
   * Test generateDatasetsXmlFromIOOS. Their example from https://sdf.ndbc.noaa.gov/sos/ :
   * https://sdf.ndbc.noaa.gov/sos/server.php?request=GetObservation&service=SOS
   * &offering=urn:ioos:network:noaa.nws.ndbc:all&featureofinterest=BBOX:-90,25,-85,30
   * &observedproperty=sea_water_temperature&responseformat=text/csv
   * &eventtime=2008-08-01T00:00Z/2008-08-02T00:00Z
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testGenerateDatasetsXmlFromOneIOOS() throws Throwable {
    boolean useCachedInfo = true;

    int language = 0;
    String results =
        EDDTableFromSOS.generateDatasetsXmlFromOneIOOS(
            useCachedInfo,
            "https://sdf.ndbc.noaa.gov/sos/server.php?service=SOS&version=1.0.0"
                + "&request=GetObservation"
                + "&observedProperty=sea_water_salinity"
                + "&offering=urn:ioos:network:noaa.nws.ndbc:all"
                + "&responseFormat=text/csv&eventTime=2010-05-27T00:00:00Z/2010-05-27T01:00:00Z"
                + "&featureofinterest=BBOX:-180,-90,180,90",
            "1.0.0",
            "IOOS_NDBC",
            "",
            "",
            "");

    String expected =
        expectedTestGenerateDatasetsXml(
            "https://sdf.ndbc.noaa.gov/sos/",
            "1.0.0",
            "IOOS_NDBC",
            "NOAA NDBC",
            "sea_water_salinity");
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /**
   * This generates ready-to-use datasets.xml (one dataset per observed property) for an IOOS SOS
   * server. <br>
   * The XML can then be edited by hand and added to the datasets.xml file. <br>
   * This tests https://sdf.ndbc.noaa.gov/sos/server.php
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testGenerateDatasetsXmlFromIOOS() throws Throwable {
    boolean useCachedInfo = true;

    int language = 0;
    String results =
        EDDTableFromSOS.generateDatasetsXmlFromIOOS(
            useCachedInfo, "https://sdf.ndbc.noaa.gov/sos/server.php", "1.0.0", "IOOS_NDBC");
    String expected =
        expectedTestGenerateDatasetsXml(
            "https://sdf.ndbc.noaa.gov/", "1.0.0", "IOOS_NDBC", "NOAA NDBC", "sea_water_salinity");
    String start = expected.substring(0, 80);
    int po = results.indexOf(start);
    if (po < 0) {
      String2.log("\nRESULTS=\n" + results);
      throw new SimpleException(
          "Start of 'expected' string not found in 'results' string:\n" + start);
    }
    String tResults = results.substring(po, Math.min(results.length(), po + expected.length()));
    Test.ensureEqual(tResults, expected, "\nresults=\n" + results + "\n\ntresults=\n" + tResults);
  }

  private static String expectedTestGenerateDatasetsXml(
      String tInfoUrl,
      String tSosVersion,
      String tSosServerType,
      String tInstitution,
      String whichObsProp) { // air_temperature or sea_water_salinity

    tSosServerType = tSosServerType == null ? "" : tSosServerType.trim();
    boolean isIoos52N =
        tSosServerType.toLowerCase().equals(EDDTableFromSOS.SosServerTypeIoos52N.toLowerCase());
    return "<dataset type=\"EDDTableFromSOS\" datasetID=\"noaa_ndbc_12f6_3b52_fb14\" active=\"true\">\n"
        + "    <sourceUrl>https://sdf.ndbc.noaa.gov/sos/server.php</sourceUrl>\n"
        + "    <sosVersion>"
        + (tSosVersion == null ? "" : tSosVersion.trim())
        + "</sosVersion>\n"
        + "    <sosServerType>"
        + (tSosServerType == null ? "" : tSosServerType.trim())
        + "</sosServerType>\n"
        + "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n"
        + "    <observationOfferingIdRegex>.+</observationOfferingIdRegex>\n"
        + "    <requestObservedPropertiesSeparately>true</requestObservedPropertiesSeparately>\n"
        + "    <bboxOffering>urn:ioos:network:noaa.nws.ndbc:all</bboxOffering>\n"
        + "    <bboxParameter>"
        + (isIoos52N ? "BBOX=" : "featureofinterest=BBOX:")
        + "</bboxParameter>\n"
        + "    <addAttributes>\n"
        + "        <att name=\"cdm_data_type\">TimeSeries</att>\n"
        + "        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n"
        + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
        + "        <att name=\"infoUrl\">"
        + tInfoUrl
        + "</att>\n"
        + "        <att name=\"institution\">"
        + tInstitution
        + "</att>\n"
        + "        <att name=\"license\">[standard]</att>\n"
        + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
        + "        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n"
        + "        <att name=\"summary\">This SOS server is part of the IOOS DIF SOS Project.  The stations in this dataset have "
        + whichObsProp
        + " data.\n"
        + "\n"
        + "Because of the nature of SOS requests, requests for data MUST include constraints for the longitude, latitude, time, and/or station_id variables.</att>\n"
        + "        <att name=\"title\">NOAA NDBC SOS - "
        + whichObsProp
        + "</att>\n"
        + "    </addAttributes>\n"
        + "    <longitudeSourceName>longitude (degree)</longitudeSourceName>\n"
        + "    <latitudeSourceName>latitude (degree)</latitudeSourceName>\n"
        + "    <altitudeSourceName>"
        + (isIoos52N ? "altitude" : "depth (m)")
        + "</altitudeSourceName>\n"
        + "    <altitudeMetersPerSourceUnit>"
        + (isIoos52N ? "1.0" : "-1.0")
        + "</altitudeMetersPerSourceUnit>\n"
        + "    <timeSourceName>date_time</timeSourceName>\n"
        + "    <timeSourceFormat>yyyy-MM-dd'T'HH:mm:ssZ</timeSourceFormat>\n"
        + "    <dataVariable>\n"
        + "        <sourceName>sensor_id</sourceName>\n"
        + "        <destinationName>sensor_id</destinationName>\n"
        + "        <dataType>String</dataType>\n"
        + "        <!-- sourceAttributes>\n"
        + "        </sourceAttributes -->\n"
        + "        <addAttributes>\n"
        + "            <att name=\"ioos_category\">Identifier</att>\n"
        + "            <att name=\"long_name\">Sensor Id</att>\n"
        + "            <att name=\"observedProperty\">"
        + whichObsProp
        + "</att>\n"
        + "            <att name=\"standard_name\">sensor_id</att>\n"
        + "        </addAttributes>\n"
        + "    </dataVariable>\n"
        + (whichObsProp.equals("air_temperature")
            ? "    <dataVariable>\n"
                + "        <sourceName>air_temperature (C)</sourceName>\n"
                + "        <destinationName>air_temperature</destinationName>\n"
                + "        <dataType>float</dataType>\n"
                + "        <!-- sourceAttributes>\n"
                + "        </sourceAttributes -->\n"
                + "        <addAttributes>\n"
                + "            <att name=\"colorBarMaximum\" type=\"double\">40.0</att>\n"
                + "            <att name=\"colorBarMinimum\" type=\"double\">-10.0</att>\n"
                + "            <att name=\"ioos_category\">Temperature</att>\n"
                + "            <att name=\"long_name\">Air Temperature</att>\n"
                + "            <att name=\"observedProperty\">air_temperature</att>\n"
                + "            <att name=\"standard_name\">air_temperature</att>\n"
                + "            <att name=\"units\">C</att>\n"
                + "        </addAttributes>\n"
                + "    </dataVariable>\n"
            : "    <dataVariable>\n"
                + "        <sourceName>sea_water_salinity (psu)</sourceName>\n"
                + "        <destinationName>sea_water_salinity</destinationName>\n"
                + "        <dataType>float</dataType>\n"
                + "        <!-- sourceAttributes>\n"
                + "        </sourceAttributes -->\n"
                + "        <addAttributes>\n"
                + "            <att name=\"colorBarMaximum\" type=\"double\">37.0</att>\n"
                + "            <att name=\"colorBarMinimum\" type=\"double\">32.0</att>\n"
                + "            <att name=\"ioos_category\">Salinity</att>\n"
                + "            <att name=\"long_name\">Sea Water Practical Salinity</att>\n"
                + "            <att name=\"observedProperty\">sea_water_salinity</att>\n"
                + "            <att name=\"standard_name\">sea_water_salinity</att>\n"
                + "            <att name=\"units\">psu</att>\n"
                + "        </addAttributes>\n"
                + "    </dataVariable>\n")
        + "</dataset>\n"
        + "\n";
  }

  /** NOT FINISHED. NOT ACTIVE. This tests getPhenomena. */
  @org.junit.jupiter.api.Test
  void testGetPhenomena() throws Throwable {
    // String2.log("testGetPhenomena");

    // testVerboseOn();
    int language = 0;
    HashMap hashMap = new HashMap();
    EDDTableFromSOS.getPhenomena(
        "https://ioos.github.io/sos-dif/gml/IOOS/0.6.1/dictionaries/phenomenaDictionary.xml",
        hashMap);
    String sar[] = String2.toStringArray(hashMap.keySet().toArray());
    for (int i = 0; i < sar.length; i++)
      String2.log(sar[i] + "\n" + ((StringArray) hashMap.get(sar[i])).toNewlineString() + "\n");
  }

  /**
   * This tests getting data from erddap's SOS server for cwwcNDBCMet.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagLocalERDDAP
  void testErddapSos() throws Throwable {
    // String2.log("\n*** EDDTableFromSOS.testErddapSos()\n");
    // testVerboseOn();

    int language = 0;
    String name, tName, results, tResults, expected, userDapQuery, tQuery;
    String error = "";
    EDV edv;
    int po;
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard to
    // check min:sec.
    EDDTable eddTable = (EDDTable) EDDTestDataset.gettestErddapSos();

    try {

      // *** test getting das for entire dataset
      String2.log("\n*** EDDTableFromSOS.testErddapSos() das dds for entire dataset\n");
      tName =
          eddTable.makeNewFileForDapQuery(
              language,
              null,
              null,
              "",
              EDStatic.fullTestCacheDirectory,
              eddTable.className() + "_Entire",
              ".das");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      // String2.log(results);
      expected =
          "  wspv {\n"
              + "    Float64 colorBarMaximum 15.0;\n"
              + "    Float64 colorBarMinimum -15.0;\n"
              + "    String ioos_category \"Wind\";\n"
              + "    String observedProperty \"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#cwwcNDBCMet\";\n"
              + "    String standard_name \"northward_wind\";\n"
              + "    String units \"m s-1\";\n"
              + "  }\n"
              + " }\n"
              + "  NC_GLOBAL {\n"
              + "    String cdm_data_type \"TimeSeries\";\n";
      po = results.indexOf(expected.substring(0, 9));
      if (po < 0) String2.log("\nresults=\n" + results);
      Test.ensureEqual(
          results.substring(po, po + expected.length()), expected, "\nresults=\n" + results);

      // *** test getting dds for entire dataset
      tName =
          eddTable.makeNewFileForDapQuery(
              language,
              null,
              null,
              "",
              EDStatic.fullTestCacheDirectory,
              eddTable.className() + "_Entire",
              ".dds");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      // String2.log(results);
      expected =
          "Dataset {\n"
              + "  Sequence {\n"
              + "    Float64 longitude;\n"
              + // minor differences from cwwcNDBCMet: LLiAT order and data type
              "    Float64 latitude;\n"
              + "    String station_id;\n"
              + "    Float64 altitude;\n"
              + "    Float64 time;\n"
              + "    Int16 wd;\n"
              + "    Float32 wspd;\n"
              + "    Float32 gst;\n"
              + "    Float32 wvht;\n"
              + "    Float32 dpd;\n"
              + "    Float32 apd;\n"
              + "    Int16 mwd;\n"
              + "    Float32 bar;\n"
              + "    Float32 atmp;\n"
              + "    Float32 wtmp;\n"
              + "    Float32 dewp;\n"
              + "    Float32 vis;\n"
              + "    Float32 ptdy;\n"
              + "    Float32 tide;\n"
              + "    Float32 wspu;\n"
              + "    Float32 wspv;\n"
              + "  } s;\n"
              + "} s;\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);

      // *** test make data files
      String2.log("\n*** EDDTableFromSOS.testErddapSos() make DATA FILES\n");

      // .csv
      // from NdbcMetStation.test31201
      // YYYY MM DD hh mm WD WSPD GST WVHT DPD APD MWD BARO ATMP WTMP DEWP VIS TIDE
      // 2005 04 19 00 00 999 99.0 99.0 1.40 9.00 99.00 999 9999.0 999.0 24.4 999.0
      // 99.0 99.00 first available
      // double seconds = Calendar2.isoStringToEpochSeconds("2005-04-19T00");
      // int row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
      // Test.ensureEqual(table.getStringData(sosOfferingIndex, row), "31201", "");
      // Test.ensureEqual(table.getFloatData(latIndex, row), -27.7f, "");
      // Test.ensureEqual(table.getFloatData(lonIndex, row), -48.13f, "");

      userDapQuery =
          "longitude,latitude,altitude,time,station_id,wvht,dpd,wtmp,dewp"
              + "&longitude=-48.13&latitude=-27.7&time=2005-04-19T00";
      tName =
          eddTable.makeNewFileForDapQuery(
              language,
              null,
              null,
              userDapQuery,
              EDStatic.fullTestCacheDirectory,
              eddTable.className() + "_Data1",
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      // String2.log(results);
      expected =
          "longitude, latitude, altitude, time, station_id, wvht, dpd, wtmp, dewp\n"
              + "degrees_east, degrees_north, m, UTC, , m, s, degree_C, degree_C\n"
              + "-48.13, -27.7, NaN, 2005-04-19T00:00:00Z, urn:ioos:def:Station:1.0.0.127.cwwcNDBCMet:31201, 1.4, 9.0, 24.4, NaN\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);

      // YYYY MM DD hh mm WD WSPD GST WVHT DPD APD MWD BARO ATMP WTMP DEWP VIS TIDE
      // 2005 04 25 18 00 999 99.0 99.0 3.90 8.00 99.00 999 9999.0 999.0 23.9 999.0
      // 99.0 99.00
      userDapQuery =
          "longitude,latitude,altitude,time,station_id,wvht,dpd,wtmp,dewp"
              + "&longitude=-48.13&latitude=-27.7&time>=2005-04-01&time<=2005-04-26";
      tName =
          eddTable.makeNewFileForDapQuery(
              language,
              null,
              null,
              userDapQuery,
              EDStatic.fullTestCacheDirectory,
              eddTable.className() + "_Data2",
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      // String2.log(results);
      expected = "longitude,latitude,altitude,time,station_id,wvht,dpd,wtmp,dewp\n";
      Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);
      expected = "degrees_east,degrees_north,m,UTC,,m,s,degree_C,degree_C\n";
      Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);
      expected =
          "-48.13,-27.7,NaN,2005-04-19T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:31201,1.4,9.0,24.4,NaN\n"; // time
      // above
      Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);
      expected =
          "-48.13,-27.7,NaN,2005-04-25T18:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:31201,3.9,8.0,23.9,NaN\n"; // this
      // time
      Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);

      // test requesting a lat lon area
      userDapQuery =
          "longitude,latitude,altitude,time,station_id,wvht,dpd,wtmp,dewp"
              + "&longitude>-125&longitude<-121&latitude>35&latitude<39&time=2005-04-01";
      long time = System.currentTimeMillis();
      tName =
          eddTable.makeNewFileForDapQuery(
              language,
              null,
              null,
              userDapQuery,
              EDStatic.fullTestCacheDirectory,
              eddTable.className() + "_Data3",
              ".csv");
      String2.log("queryTime=" + (System.currentTimeMillis() - time));
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      // String2.log(results);
      expected =
          "longitude,latitude,altitude,time,station_id,wvht,dpd,wtmp,dewp\n"
              + "degrees_east,degrees_north,m,UTC,,m,s,degree_C,degree_C\n"
              + "-122.88,37.36,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46012,2.55,12.5,13.7,NaN\n"
              + "-123.307,38.238,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46013,2.3,12.9,13.9,NaN\n"
              + "-122.82,37.75,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46026,1.96,12.12,14.0,NaN\n"
              + "-121.89,35.74,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46028,2.57,12.9,16.3,NaN\n"
              + "-122.42,36.75,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46042,2.21,17.39,14.5,NaN\n"
              + "-121.9,36.83,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46091,NaN,NaN,NaN,NaN\n"
              + "-122.02,36.75,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46092,NaN,NaN,NaN,NaN\n"
              + "-122.41,36.69,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46093,NaN,NaN,14.3,NaN\n"
              + "-123.28,37.57,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46214,2.5,9.0,12.8,NaN\n"
              + "-122.3,37.77,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:AAMC1,NaN,NaN,15.5,NaN\n"
              + "-123.71,38.91,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:ANVC1,NaN,NaN,NaN,NaN\n"
              + "-122.47,37.81,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:FTPC1,NaN,NaN,NaN,NaN\n"
              + "-121.89,36.61,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:MTYC1,NaN,NaN,15.1,NaN\n"
              + "-122.04,38.06,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:PCOC1,NaN,NaN,14.9,NaN\n"
              + "-123.74,38.96,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:PTAC1,NaN,NaN,NaN,NaN\n"
              + "-122.4,37.93,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:RCMC1,NaN,NaN,14.0,NaN\n"
              + "-122.21,37.51,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:RTYC1,NaN,NaN,14.2,NaN\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);

      // test that constraint vars are sent to low level data request
      userDapQuery =
          "longitude,latitude,altitude,station_id,wvht,dpd,wtmp,dewp"
              + // no "time" here
              "&longitude>-125&longitude<-121&latitude>35&latitude<39&time=2005-04-01"; // "time"
      // here
      tName =
          eddTable.makeNewFileForDapQuery(
              language,
              null,
              null,
              userDapQuery,
              EDStatic.fullTestCacheDirectory,
              eddTable.className() + "_Data4",
              ".csv");
      String2.log("queryTime=" + (System.currentTimeMillis() - time));
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      // String2.log(results);
      expected =
          "longitude,latitude,altitude,station_id,wvht,dpd,wtmp,dewp\n"
              + "degrees_east,degrees_north,m,,m,s,degree_C,degree_C\n"
              + "-122.88,37.36,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46012,2.55,12.5,13.7,NaN\n"
              + "-123.307,38.238,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46013,2.3,12.9,13.9,NaN\n"
              + "-122.82,37.75,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46026,1.96,12.12,14.0,NaN\n"
              + "-121.89,35.74,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46028,2.57,12.9,16.3,NaN\n"
              + "-122.42,36.75,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46042,2.21,17.39,14.5,NaN\n"
              + "-121.9,36.83,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46091,NaN,NaN,NaN,NaN\n"
              + "-122.02,36.75,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46092,NaN,NaN,NaN,NaN\n"
              + "-122.41,36.69,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46093,NaN,NaN,14.3,NaN\n"
              + "-123.28,37.57,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46214,2.5,9.0,12.8,NaN\n"
              + "-122.3,37.77,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:AAMC1,NaN,NaN,15.5,NaN\n"
              + "-123.71,38.91,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:ANVC1,NaN,NaN,NaN,NaN\n"
              + "-122.47,37.81,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:FTPC1,NaN,NaN,NaN,NaN\n"
              + "-121.89,36.61,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:MTYC1,NaN,NaN,15.1,NaN\n"
              + "-122.04,38.06,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:PCOC1,NaN,NaN,14.9,NaN\n"
              + "-123.74,38.96,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:PTAC1,NaN,NaN,NaN,NaN\n"
              + "-122.4,37.93,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:RCMC1,NaN,NaN,14.0,NaN\n"
              + "-122.21,37.51,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:RTYC1,NaN,NaN,14.2,NaN\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);

      // test that constraint vars are sent to low level data request
      // and that constraint causing 0rows for a station doesn't cause problems
      userDapQuery =
          "longitude,latitude,wtmp&time>=2008-03-14T18:00:00Z&time<=2008-03-14T18:00:00Z&wtmp>20";
      tName =
          eddTable.makeNewFileForDapQuery(
              language,
              null,
              null,
              userDapQuery,
              EDStatic.fullTestCacheDirectory,
              eddTable.className() + "_Data5",
              ".csv");
      String2.log("queryTime=" + (System.currentTimeMillis() - time));
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      // String2.log(results);
      expected =
          "longitude,latitude,wtmp\n"
              + "degrees_east,degrees_north,degree_C\n"
              + "-80.17,28.5,21.2\n"
              + "-78.47,28.95,23.7\n"
              + "-80.6,30.0,20.1\n"
              + "-46.0,14.53,25.3\n"
              + "-65.01,20.99,25.7\n"
              + "-71.49,27.47,23.8\n"
              + "-69.649,31.9784,22.0\n"
              + "-80.53,28.4,21.6\n"
              + "-80.22,27.55,21.7\n"
              + "-89.67,25.9,24.1\n"
              + "-94.42,25.17,23.4\n"
              + "-85.94,26.07,26.1\n"
              + "-94.05,22.01,24.4\n"
              + "-85.06,19.87,26.8\n"
              + "-67.5,15.01,26.4\n"
              + "-84.245,27.3403,20.2\n"
              + "-88.09,29.06,21.7\n"
              + "-157.79,17.14,24.3\n"
              + "-160.74,19.16,24.7\n"
              + "-152.48,17.52,24.0\n"
              + "-153.87,0.02,25.0\n"
              + "-158.12,21.67,24.3\n"
              + "-157.68,21.42,24.2\n"
              + "144.79,13.54,28.1\n"
              + "-90.42,29.78,20.4\n"
              + "-64.92,18.34,27.7\n"
              + "-81.87,26.65,22.2\n"
              + "-80.1,25.59,23.5\n"
              + "-156.47,20.9,25.0\n"
              + "167.74,8.74,27.6\n"
              + "-81.81,24.55,23.9\n"
              + "-80.86,24.84,23.8\n"
              + "-64.75,17.7,26.0\n"
              + "-67.05,17.97,27.1\n"
              + "-80.38,25.01,24.2\n"
              + "-81.81,26.13,23.7\n"
              + "-170.688,-14.28,29.6\n"
              + "-157.87,21.31,25.5\n"
              + "-96.4,28.45,20.1\n"
              + "-82.77,24.69,22.8\n"
              + "-97.22,26.06,20.1\n"
              + "-82.63,27.76,21.7\n"
              + "-66.12,18.46,28.3\n"
              + "-177.36,28.21,21.8\n"
              + "-80.59,28.42,22.7\n"
              + "166.62,19.29,27.9\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
    } catch (Throwable t) {
      throw new RuntimeException(
          "THIS TEST REQUIRES THAT SOS SERVICES BE TURNED ON IN LOCAL ERDDAP.", t);
    }
  }
}
