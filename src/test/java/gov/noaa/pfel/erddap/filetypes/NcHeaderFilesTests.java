package gov.noaa.pfel.erddap.filetypes;

import com.cohort.util.File2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testDataset.EDDTestDataset;
import testDataset.Initialization;
import testSupport.ExternalTestUrls;
import testSupport.WireMockLifecycle;

/**
 * This tests making an nc header file. This is supporting ncHeaderMakeFile true and false. There
 * are a number of lines from the output that vary slightly depending on the setting, to allow tests
 * to pass either way, we don't test those lines. The way \' is put into output between the two
 * settings varies, so we avoid checking those lines. In some cases the old approach does not have
 * range information geospatial_lat/lon_min/max, etc... In those tests we remove those lines as
 * well. Once we fully migrate to ncHeaderMakeFile false we can simplify these tests.
 */
public class NcHeaderFilesTests extends WireMockLifecycle {

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /**
   * @throws Throwable if trouble
   */
  @Test
  void testJsonlCSV() throws Throwable {
    int language = 0;
    String tName, results, expected;

    EDD edd = EDDTestDataset.gettestJsonlCSV();
    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.config.fullTestCacheDirectory,
            edd.className(),
            ".ncHeader");
    results = File2.directReadFrom88591File(EDStatic.config.fullTestCacheDirectory + tName);
    results =
        results.replaceAll("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z", "YYYY-MM-DDTHH:mm:ssZ");
    results =
        results.replaceFirst("^netcdf\\s+(.+?)\\s*\\{", "netcdf EDDTableFromJsonlCSVFiles.nc {");
    expected =
"""
netcdf EDDTableFromJsonlCSVFiles.nc {
  dimensions:
    row = 6;
    ship_strlen = 15;
  variables:
    char ship(row=6, ship_strlen=15);
      :_Encoding = "ISO-8859-1";
      :cf_role = "trajectory_id";
      :ioos_category = "Identifier";
      :long_name = "Ship";

    double time(row=6);
      :_CoordinateAxisType = "Time";
      :actual_range = 1.4902299E9, 1.4903055E9; // double
      :axis = "T";
      :ioos_category = "Time";
      :long_name = "Time";
      :standard_name = "time";
      :time_origin = "01-JAN-1970 00:00:00";
      :units = "seconds since YYYY-MM-DDTHH:mm:ssZ";

    float latitude(row=6);
      :_CoordinateAxisType = "Lat";
      :actual_range = 27.9998f, 28.0003f; // float
      :axis = "Y";
      :colorBarMaximum = 90.0; // double
      :colorBarMinimum = -90.0; // double
      :ioos_category = "Location";
      :long_name = "Latitude";
      :standard_name = "latitude";
      :units = "degrees_north";

    float longitude(row=6);
      :_CoordinateAxisType = "Lon";
      :actual_range = -132.0014f, -130.2576f; // float
      :axis = "X";
      :colorBarMaximum = 180.0; // double
      :colorBarMinimum = -180.0; // double
      :ioos_category = "Location";
      :long_name = "Longitude";
      :standard_name = "longitude";
      :units = "degrees_east";

    char status(row=6);
      :actual_range = "\\t", "?";
      :ioos_category = "Other";
      :long_name = "Status";

    double testLong(row=6);
      :_FillValue = 9.223372036854776E18; // double
      :actual_range = -9.223372036854776E18, 9.223372036854776E18; // double
      :ioos_category = "Other";
      :long_name = "Test Long";

    float sst(row=6);
      :actual_range = 10.0f, 99.0f; // float
      :ioos_category = "Temperature";
      :long_name = "Sea Surface Temperature";
      :units = "degree_C";

  // global attributes:
  :cdm_data_type = "Trajectory";
  :cdm_trajectory_variables = "ship";
  :Conventions = "COARDS, CF-1.6, ACDD-1.3";
  :Easternmost_Easting = -130.2576f; // float
  :featureType = "Trajectory";
  :geospatial_lat_max = 28.0003f; // float
  :geospatial_lat_min = 27.9998f; // float
  :geospatial_lat_units = "degrees_north";
  :geospatial_lon_max = -130.2576f; // float
  :geospatial_lon_min = -132.0014f; // float
  :geospatial_lon_units = "degrees_east";
  :history = "YYYY-MM-DDTHH:mm:ssZ (local files)
YYYY-MM-DDTHH:mm:ssZ http://localhost:8080/erddap/tabledap/testJsonlCSV.ncHeader";
  :id = "testJsonlCSV";
  :infoUrl = "https://jsonlines.org/examples/";
  :institution = "jsonlines.org";
  :keywords = "data, latitude, local, long, longitude, sea, ship, source, sst, status, surface, temperature, test, testLong, time";
  :license = "The data may be used and redistributed for free but is not intended
for legal use, since it may contain inaccuracies. Neither the data
Contributor, ERD, NOAA, nor the United States Government, nor any
of their employees or contractors, makes any warranty, express or
implied, including warranties of merchantability and fitness for a
particular purpose, or assumes any legal liability for the accuracy,
completeness, or usefulness, of this information.";
  :Northernmost_Northing = 28.0003f; // float
  :sourceUrl = "(local files)";
  :Southernmost_Northing = 27.9998f; // float
  :standard_name_vocabulary = "CF Standard Name Table v70";
  :subsetVariables = "ship";
  :summary = "This is the sample summary.";
  :time_coverage_end = "YYYY-MM-DDTHH:mm:ssZ";
  :time_coverage_start = "YYYY-MM-DDTHH:mm:ssZ";
  :title = "Test of JSON Lines CSV";
  :Westernmost_Easting = -132.0014f; // float
}
            """;
    expected = expected.replaceAll("\r\n", "\n");
    results = results.replaceAll("\r\n", "\n");
    expected =
        expected.substring(0, expected.indexOf("  :history = "))
            + expected.substring(expected.indexOf("  :infoUrl = "));
    results =
        results.substring(0, results.indexOf("  :history = "))
            + results.substring(results.indexOf("  :infoUrl = "));
    expected =
        expected.substring(0, expected.indexOf("  :license = "))
            + expected.substring(expected.indexOf("  :sourceUrl = "));
    results =
        results.substring(0, results.indexOf("  :license = "))
            + results.substring(results.indexOf("  :sourceUrl = "));
    expected =
        expected.substring(0, expected.indexOf("    char status(row=6);"))
            + expected.substring(expected.indexOf("    double testLong(row=6);"));
    results =
        results.substring(0, results.indexOf("    char status(row=6);"))
            + results.substring(results.indexOf("    double testLong(row=6);"));
    expected = removeLineStartsWithIfExist("  :Easternmost_Easting = ", expected);
    expected = removeLineStartsWithIfExist("  :geospatial_lat_max = ", expected);
    expected = removeLineStartsWithIfExist("  :geospatial_lat_min = ", expected);
    expected = removeLineStartsWithIfExist("  :geospatial_lon_max = ", expected);
    expected = removeLineStartsWithIfExist("  :geospatial_lon_min = ", expected);
    expected = removeLineStartsWithIfExist("  :Northernmost_Northing = ", expected);
    expected = removeLineStartsWithIfExist("  :Southernmost_Northing = ", expected);
    expected = removeLineStartsWithIfExist("  :Westernmost_Easting = ", expected);
    results = removeLineStartsWithIfExist("  :Easternmost_Easting = ", results);
    results = removeLineStartsWithIfExist("  :geospatial_lat_max = ", results);
    results = removeLineStartsWithIfExist("  :geospatial_lat_min = ", results);
    results = removeLineStartsWithIfExist("  :geospatial_lon_max = ", results);
    results = removeLineStartsWithIfExist("  :geospatial_lon_min = ", results);
    results = removeLineStartsWithIfExist("  :Northernmost_Northing = ", results);
    results = removeLineStartsWithIfExist("  :Southernmost_Northing = ", results);
    results = removeLineStartsWithIfExist("  :Westernmost_Easting = ", results);
    com.cohort.util.Test.ensureEqual(expected, results, "results=\n" + results);

    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "time,ship,sst&time=2017-03-23T02:45",
            EDStatic.config.fullTestCacheDirectory,
            edd.className() + "_1time",
            ".ncHeader");
    results = File2.directReadFrom88591File(EDStatic.config.fullTestCacheDirectory + tName);
    results =
        results.replaceAll("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z", "YYYY-MM-DDTHH:mm:ssZ");
    results =
        results.replaceFirst(
            "^netcdf\\s+(.+?)\\s*\\{", "netcdf EDDTableFromJsonlCSVFiles_1time.nc {");
    expected =
"""
netcdf EDDTableFromJsonlCSVFiles_1time.nc {
  dimensions:
    row = 1;
    ship_strlen = 15;
  variables:
    double time(row=1);
      :_CoordinateAxisType = "Time";
      :actual_range = 1.4902371E9, 1.4902371E9; // double
      :axis = "T";
      :ioos_category = "Time";
      :long_name = "Time";
      :standard_name = "time";
      :time_origin = "01-JAN-1970 00:00:00";
      :units = "seconds since YYYY-MM-DDTHH:mm:ssZ";

    char ship(row=1, ship_strlen=15);
      :_Encoding = "ISO-8859-1";
      :cf_role = "trajectory_id";
      :ioos_category = "Identifier";
      :long_name = "Ship";

    float sst(row=1);
      :actual_range = 10.7f, 10.7f; // float
      :ioos_category = "Temperature";
      :long_name = "Sea Surface Temperature";
      :units = "degree_C";

  // global attributes:
  :cdm_data_type = "Trajectory";
  :cdm_trajectory_variables = "ship";
  :Conventions = "COARDS, CF-1.6, ACDD-1.3";
  :Easternmost_Easting = -130.2576; // double
  :featureType = "Trajectory";
  :geospatial_lat_max = 28.0003; // double
  :geospatial_lat_min = 27.9998; // double
  :geospatial_lat_units = "degrees_north";
  :geospatial_lon_max = -130.2576; // double
  :geospatial_lon_min = -132.0014; // double
  :geospatial_lon_units = "degrees_east";
  :history = "YYYY-MM-DDTHH:mm:ssZ (local files)
YYYY-MM-DDTHH:mm:ssZ http://localhost:8080/erddap/tabledap/testJsonlCSV.ncHeader?time,ship,sst&time=2017-03-23T02:45";
  :id = "testJsonlCSV";
  :infoUrl = "https://jsonlines.org/examples/";
  :institution = "jsonlines.org";
  :keywords = "data, latitude, local, long, longitude, sea, ship, source, sst, status, surface, temperature, test, testLong, time";
  :license = "The data may be used and redistributed for free but is not intended
for legal use, since it may contain inaccuracies. Neither the data
Contributor, ERD, NOAA, nor the United States Government, nor any
of their employees or contractors, makes any warranty, express or
implied, including warranties of merchantability and fitness for a
particular purpose, or assumes any legal liability for the accuracy,
completeness, or usefulness, of this information.";
  :sourceUrl = "(local files)";
  :Southernmost_Northing = 27.9998; // double
  :standard_name_vocabulary = "CF Standard Name Table v70";
  :subsetVariables = "ship";
  :summary = "This is the sample summary.";
  :time_coverage_end = "YYYY-MM-DDTHH:mm:ssZ";
  :time_coverage_start = "YYYY-MM-DDTHH:mm:ssZ";
  :title = "Test of JSON Lines CSV";
  :Westernmost_Easting = -132.0014; // double
}
            """;
    expected = expected.replaceAll("\r\n", "\n");
    results = results.replaceAll("\r\n", "\n");
    expected =
        expected.substring(0, expected.indexOf("  :history = "))
            + expected.substring(expected.indexOf("  :infoUrl = "));
    results =
        results.substring(0, results.indexOf("  :history = "))
            + results.substring(results.indexOf("  :infoUrl = "));
    expected =
        expected.substring(0, expected.indexOf("  :license = "))
            + expected.substring(expected.indexOf("  :sourceUrl = "));
    results =
        results.substring(0, results.indexOf("  :license = "))
            + results.substring(results.indexOf("  :sourceUrl = "));
    expected = removeLineStartsWithIfExist("  :Easternmost_Easting = ", expected);
    expected = removeLineStartsWithIfExist("  :geospatial_lat_max = ", expected);
    expected = removeLineStartsWithIfExist("  :geospatial_lat_min = ", expected);
    expected = removeLineStartsWithIfExist("  :geospatial_lon_max = ", expected);
    expected = removeLineStartsWithIfExist("  :geospatial_lon_min = ", expected);
    expected = removeLineStartsWithIfExist("  :Northernmost_Northing = ", expected);
    expected = removeLineStartsWithIfExist("  :Southernmost_Northing = ", expected);
    expected = removeLineStartsWithIfExist("  :Westernmost_Easting = ", expected);
    results = removeLineStartsWithIfExist("  :Easternmost_Easting = ", results);
    results = removeLineStartsWithIfExist("  :geospatial_lat_max = ", results);
    results = removeLineStartsWithIfExist("  :geospatial_lat_min = ", results);
    results = removeLineStartsWithIfExist("  :geospatial_lon_max = ", results);
    results = removeLineStartsWithIfExist("  :geospatial_lon_min = ", results);
    results = removeLineStartsWithIfExist("  :Northernmost_Northing = ", results);
    results = removeLineStartsWithIfExist("  :Southernmost_Northing = ", results);
    results = removeLineStartsWithIfExist("  :Westernmost_Easting = ", results);
    com.cohort.util.Test.ensureEqual(expected, results, "results=\n" + results);
  }

  private static String removeLineStartsWithIfExist(String toFind, String longString) {
    int po = longString.indexOf(toFind);
    if (po >= 0) {
      int endLine = longString.indexOf('\n', po);
      if (endLine >= 0) {
        longString = longString.substring(0, po) + longString.substring(endLine + 1);
      } else {
        longString = longString.substring(0, po);
      }
    }
    return longString;
  }

  /**
   * @throws Throwable if trouble
   */
  @Test
  void testGridFromDap() throws Throwable {
    int language = 0;
    String tName, results, expected;

    String base = ExternalTestUrls.apdrcHawaiiBase();

    EDD edd = EDDTestDataset.gethawaii_d90f_20ee_c4cb();
    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "temp[(2001-12-15T00:00:00)][(5.01):(500)][(23.1)][(185.2)],"
                + "salt[(2001-12-15T00:00:00)][(5.01):(500)][(23.1)][(185.2)],"
                + "u[(2001-12-15T00:00:00)][(5.01):(500)][(23.1)][(185.2)],"
                + "v[(2001-12-15T00:00:00)][(5.01):(500)][(23.1)][(185.2)],"
                + "w[(2001-12-15T00:00:00)][(5.01):(500)][(23.1)][(185.2)]",
            EDStatic.config.fullTestCacheDirectory,
            edd.className(),
            ".ncHeader");
    results = File2.directReadFrom88591File(EDStatic.config.fullTestCacheDirectory + tName);
    results =
        results.replaceAll("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z", "YYYY-MM-DDTHH:mm:ssZ");
    results = results.replaceFirst("^netcdf\\s+(.+?)\\s*\\{", "netcdf EDDGridFromDap.nc {");
    expected =
"""
netcdf EDDGridFromDap.nc {
  dimensions:
    time = 1;
    depth = 19;
    latitude = 1;
    longitude = 1;
  variables:
    double time(time=1);
      :_CoordinateAxisType = "Time";
      :actual_range = 1.0083744E9, 1.0083744E9; // double
      :axis = "T";
      :ioos_category = "Time";
      :legacy_time_adjust = "true";
      :long_name = "Centered Time";
      :standard_name = "time";
      :time_origin = "01-JAN-1970 00:00:00";
      :units = "seconds since YYYY-MM-DDTHH:mm:ssZ";

    double depth(depth=19);
      :_CoordinateAxisType = "Height";
      :_CoordinateZisPositive = "down";
      :actual_range = 5.01, 465.91; // double
      :axis = "Z";
      :ioos_category = "Location";
      :long_name = "Depth";
      :positive = "down";
      :standard_name = "depth";
      :units = "m";

    double latitude(latitude=1);
      :_CoordinateAxisType = "Lat";
      :actual_range = 23.25, 23.25; // double
      :axis = "Y";
      :ioos_category = "Location";
      :long_name = "Latitude";
      :standard_name = "latitude";
      :units = "degrees_north";

    double longitude(longitude=1);
      :_CoordinateAxisType = "Lon";
      :actual_range = 185.25, 185.25; // double
      :axis = "X";
      :ioos_category = "Location";
      :long_name = "Longitude";
      :standard_name = "longitude";
      :units = "degrees_east";

    float temp(time=1, depth=19, latitude=1, longitude=1);
      :_FillValue = -9.99E33f; // float
      :colorBarMaximum = 32.0; // double
      :colorBarMinimum = 0.0; // double
      :ioos_category = "Temperature";
      :long_name = "Sea Water Temperature";
      :missing_value = -9.99E33f; // float
      :standard_name = "sea_water_temperature";
      :units = "degree_C";

    float salt(time=1, depth=19, latitude=1, longitude=1);
      :_FillValue = -9.99E33f; // float
      :colorBarMaximum = 37.0; // double
      :colorBarMinimum = 32.0; // double
      :ioos_category = "Salinity";
      :long_name = "Sea Water Practical Salinity";
      :missing_value = -9.99E33f; // float
      :standard_name = "sea_water_practical_salinity";
      :units = "PSU";

    float u(time=1, depth=19, latitude=1, longitude=1);
      :_FillValue = -9.99E33f; // float
      :colorBarMaximum = 0.5; // double
      :colorBarMinimum = -0.5; // double
      :ioos_category = "Currents";
      :long_name = "Eastward Sea Water Velocity";
      :missing_value = -9.99E33f; // float
      :standard_name = "eastward_sea_water_velocity";
      :units = "m s-1";

    float v(time=1, depth=19, latitude=1, longitude=1);
      :_FillValue = -9.99E33f; // float
      :colorBarMaximum = 0.5; // double
      :colorBarMinimum = -0.5; // double
      :ioos_category = "Currents";
      :long_name = "Northward Sea Water Velocity";
      :missing_value = -9.99E33f; // float
      :standard_name = "northward_sea_water_velocity";
      :units = "m s-1";

    float w(time=1, depth=19, latitude=1, longitude=1);
      :_FillValue = -9.99E33f; // float
      :colorBarMaximum = 1.0E-5; // double
      :colorBarMinimum = -1.0E-5; // double
      :comment = "WARNING: Please use this variable\\'s data with caution.";
      :ioos_category = "Currents";
      :long_name = "Upward Sea Water Velocity";
      :missing_value = -9.99E33f; // float
      :standard_name = "upward_sea_water_velocity";
      :units = "m s-1";

  // global attributes:
  :cdm_data_type = "Grid";
  :Conventions = "COARDS, CF-1.6, ACDD-1.3";
  :dataType = "Grid";
  :defaultDataQuery = "temp[last][0][0:last][0:last],salt[last][0][0:last][0:last],u[last][0][0:last][0:last],v[last][0][0:last][0:last],w[last][0][0:last][0:last]";
  :defaultGraphQuery = "temp[last][0][0:last][0:last]&.draw=surface&.vars=longitude|latitude|temp";
  :documentation = "%s/datadoc/soda_2.2.4.php";
  :Easternmost_Easting = 185.25; // double
  :geospatial_lat_max = 23.25; // double
  :geospatial_lat_min = 23.25; // double
  :geospatial_lat_resolution = 0.5; // double
  :geospatial_lat_units = "degrees_north";
  :geospatial_lon_max = 185.25; // double
  :geospatial_lon_min = 185.25; // double
  :geospatial_lon_resolution = 0.5; // double
  :geospatial_lon_units = "degrees_east";
  :geospatial_vertical_max = 465.91; // double
  :geospatial_vertical_min = 5.01; // double
  :geospatial_vertical_positive = "down";
  :geospatial_vertical_units = "m";
  :history = "Fri Nov 07 05:28:17 HST 2025 : imported by GrADS Data Server 2.0
YYYY-MM-DDTHH:mm:ssZ %s/dods/public_data/SODA/soda_pop2.2.4
YYYY-MM-DDTHH:mm:ssZ http://localhost:8080/erddap/griddap/hawaii_d90f_20ee_c4cb.ncHeader?temp[(2001-12-15T00:00:00)][(5.01):(500)][(23.1)][(185.2)],salt[(2001-12-15T00:00:00)][(5.01):(500)][(23.1)][(185.2)],u[(2001-12-15T00:00:00)][(5.01):(500)][(23.1)][(185.2)],v[(2001-12-15T00:00:00)][(5.01):(500)][(23.1)][(185.2)],w[(2001-12-15T00:00:00)][(5.01):(500)][(23.1)][(185.2)]";
  :infoUrl = "https://www.atmos.umd.edu/~ocean/";
  :institution = "TAMU/UMD";
  :keywords = "circulation, currents, density, depths, Earth Science > Oceans > Ocean Circulation > Ocean Currents, Earth Science > Oceans > Ocean Temperature > Water Temperature, Earth Science > Oceans > Salinity/Density > Salinity, eastward, eastward_sea_water_velocity, means, monthly, northward, northward_sea_water_velocity, ocean, oceans, pop, salinity, sea, sea_water_practical_salinity, sea_water_temperature, seawater, soda, tamu, temperature, umd, upward, upward_sea_water_velocity, velocity, water";
  :keywords_vocabulary = "GCMD Science Keywords";
  :license = "The data may be used and redistributed for free but is not intended
for legal use, since it may contain inaccuracies. Neither the data
Contributor, ERD, NOAA, nor the United States Government, nor any
of their employees or contractors, makes any warranty, express or
implied, including warranties of merchantability and fitness for a
particular purpose, or assumes any legal liability for the accuracy,
completeness, or usefulness, of this information.";
  :Northernmost_Northing = 23.25; // double
  :sourceUrl = "%s/dods/public_data/SODA/soda_pop2.2.4";
  :Southernmost_Northing = 23.25; // double
  :standard_name_vocabulary = "CF Standard Name Table v70";
  :summary = "Simple Ocean Data Assimilation (SODA) version 2.2.4 - A reanalysis of ocean\\s
climate. SODA uses the GFDL modular ocean model version 2.2. The model is\\s
forced by observed surface wind stresses from the COADS data set (from 1958\\s
to 1992) and from NCEP (after 1992). Note that the wind stresses were\\s
detrended before use due to inconsistencies with observed sea level pressure\\s
trends. The model is also constrained by constant assimilation of observed\\s
temperatures, salinities, and altimetry using an optimal data assimilation\\s
technique. The observed data comes from: 1) The World Ocean Atlas 1994 which\\s
contains ocean temperatures and salinities from mechanical\\s
bathythermographs, expendable bathythermographs and conductivity-temperature-
depth probes. 2) The expendable bathythermograph archive 3) The TOGA-TAO\\s
thermistor array 4) The Soviet SECTIONS tropical program 5) Satellite\\s
altimetry from Geosat, ERS/1 and TOPEX/Poseidon.\\s
We are now exploring an eddy-permitting reanalysis based on the Parallel\\s
Ocean Program POP-1.4 model with 40 levels in the vertical and a 0.4x0.25\\s
degree displaced pole grid (25 km resolution in the western North\\s
Atlantic).  The first version of this we will release is SODA1.2, a\\s
reanalysis driven by ERA-40 winds covering the period 1958-2001 (extended to\\s
the current year using available altimetry).";
  :time_coverage_end = "YYYY-MM-DDTHH:mm:ssZ";
  :time_coverage_start = "YYYY-MM-DDTHH:mm:ssZ";
  :title = "SODA - POP 2.2.4 Monthly Means, 1871-2010 (At Depths)";
  :Westernmost_Easting = 185.25; // double
}
"""
            .formatted(base, base, base);
    expected = expected.replaceAll("\r\n", "\n");
    results = results.replaceAll("\r\n", "\n");
    expected =
        expected.substring(0, expected.indexOf("  :history = "))
            + expected.substring(expected.indexOf("  :infoUrl = "));
    results =
        results.substring(0, results.indexOf("  :history = "))
            + results.substring(results.indexOf("  :infoUrl = "));
    expected =
        expected.substring(0, expected.indexOf("  :license = "))
            + expected.substring(expected.indexOf("  :Northernmost_Northing = "));
    results =
        results.substring(0, results.indexOf("  :license = "))
            + results.substring(results.indexOf("  :Northernmost_Northing = "));
    expected =
        expected.substring(0, expected.indexOf("  :summary = "))
            + expected.substring(expected.indexOf("  :time_coverage_end = "));
    results =
        results.substring(0, results.indexOf("  :summary = "))
            + results.substring(results.indexOf("  :time_coverage_end = "));
    int commentStart = expected.indexOf("      :comment = ");
    expected =
        expected.substring(0, commentStart)
            + expected.substring(expected.indexOf("      :ioos_category = ", commentStart));
    commentStart = results.indexOf("      :comment = ");
    results =
        results.substring(0, commentStart)
            + results.substring(results.indexOf("      :ioos_category = ", commentStart));
    results = results.replaceAll("\'", "'");
    expected = expected.replaceAll("\'", "'");
    com.cohort.util.Test.ensureEqual(expected, results, "results=\n" + results);
  }

  /**
   * @throws Throwable if trouble
   */
  @Test
  void testgridCopy() throws Throwable {
    int language = 0;
    String tName, results, expected;

    EDD edd = EDDTestDataset.gettestGridCopy();
    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.config.fullTestCacheDirectory,
            edd.className(),
            ".ncHeader");
    results = File2.directReadFrom88591File(EDStatic.config.fullTestCacheDirectory + tName);
    results =
        results.replaceAll("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z", "YYYY-MM-DDTHH:mm:ssZ");
    results = results.replaceFirst("^netcdf\\s+(.+?)\\s*\\{", "netcdf EDDGridCopy.nc {");
    expected =
"""
netcdf EDDGridCopy.nc {
  dimensions:
    time = 10;
    altitude = 1;
    latitude = 720;
    longitude = 1440;
  variables:
    double time(time=10);
      :_CoordinateAxisType = "Time";
      :actual_range = 1.1991888E9, 1.1999664E9; // double
      :axis = "T";
      :fraction_digits = 0; // int
      :ioos_category = "Time";
      :long_name = "Centered Time";
      :standard_name = "time";
      :time_origin = "01-JAN-1970 00:00:00";
      :units = "seconds since YYYY-MM-DDTHH:mm:ssZ";

    double altitude(altitude=1);
      :_CoordinateAxisType = "Height";
      :_CoordinateZisPositive = "up";
      :actual_range = 0.0, 0.0; // double
      :axis = "Z";
      :fraction_digits = 0; // int
      :ioos_category = "Location";
      :long_name = "Altitude";
      :positive = "up";
      :standard_name = "altitude";
      :units = "m";

    double latitude(latitude=720);
      :_CoordinateAxisType = "Lat";
      :actual_range = -89.875, 89.875; // double
      :axis = "Y";
      :coordsys = "geographic";
      :fraction_digits = 2; // int
      :ioos_category = "Location";
      :long_name = "Latitude";
      :point_spacing = "even";
      :standard_name = "latitude";
      :units = "degrees_north";

    double longitude(longitude=1440);
      :_CoordinateAxisType = "Lon";
      :actual_range = 0.125, 359.875; // double
      :axis = "X";
      :coordsys = "geographic";
      :fraction_digits = 2; // int
      :ioos_category = "Location";
      :long_name = "Longitude";
      :point_spacing = "even";
      :standard_name = "longitude";
      :units = "degrees_east";

    float x_wind(time=10, altitude=1, latitude=720, longitude=1440);
      :_FillValue = -9999999.0f; // float
      :colorBarMaximum = 15.0; // double
      :colorBarMinimum = -15.0; // double
      :coordsys = "geographic";
      :fraction_digits = 1; // int
      :ioos_category = "Wind";
      :long_name = "Zonal Wind";
      :missing_value = -9999999.0f; // float
      :standard_name = "x_wind";
      :units = "m s-1";

    float y_wind(time=10, altitude=1, latitude=720, longitude=1440);
      :_FillValue = -9999999.0f; // float
      :colorBarMaximum = 15.0; // double
      :colorBarMinimum = -15.0; // double
      :coordsys = "geographic";
      :fraction_digits = 1; // int
      :ioos_category = "Wind";
      :long_name = "Meridional Wind";
      :missing_value = -9999999.0f; // float
      :standard_name = "y_wind";
      :units = "m s-1";

    float mod(time=10, altitude=1, latitude=720, longitude=1440);
      :_FillValue = -9999999.0f; // float
      :colorBarMaximum = 18.0; // double
      :colorBarMinimum = 0.0; // double
      :colorBarPalette = "WhiteRedBlack";
      :coordsys = "geographic";
      :fraction_digits = 1; // int
      :ioos_category = "Wind";
      :long_name = "Modulus of Wind";
      :missing_value = -9999999.0f; // float
      :units = "m s-1";

  // global attributes:
  :acknowledgement = "NOAA NESDIS COASTWATCH, NOAA SWFSC ERD";
  :cdm_data_type = "Grid";
  :composite = "true";
  :contributor_name = "Remote Sensing Systems, Inc";
  :contributor_role = "Source of level 2 data.";
  :Conventions = "COARDS, CF-1.6, ACDD-1.3";
  :creator_email = "erd.data@noaa.gov";
  :creator_name = "NOAA CoastWatch, West Coast Node";
  :creator_url = "https://coastwatch.pfeg.noaa.gov";
  :date_created = "2008-08-29";
  :date_issued = "2008-08-29";
  :Easternmost_Easting = 359.875; // double
  :geospatial_lat_max = 89.875; // double
  :geospatial_lat_min = -89.875; // double
  :geospatial_lat_resolution = 0.25; // double
  :geospatial_lat_units = "degrees_north";
  :geospatial_lon_max = 359.875; // double
  :geospatial_lon_min = 0.125; // double
  :geospatial_lon_resolution = 0.25; // double
  :geospatial_lon_units = "degrees_east";
  :geospatial_vertical_max = 0.0; // double
  :geospatial_vertical_min = 0.0; // double
  :geospatial_vertical_positive = "up";
  :geospatial_vertical_units = "m";
  :history = "Remote Sensing Systems, Inc
YYYY-MM-DDTHH:mm:ssZ NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD
YYYY-MM-DDTHH:mm:ssZ http://192.168.31.18/thredds/dodsC/satellite/QS/ux10/1day
YYYY-MM-DDTHH:mm:ssZ http://localhost:8080/erddap/griddap/testGridCopy.ncHeader";
  :infoUrl = "https://coastwatch.pfeg.noaa.gov/infog/QS_ux10_las.html";
  :institution = "NOAA CoastWatch, West Coast Node";
  :keywords = "Earth Science > Oceans > Ocean Winds > Surface Winds";
  :keywords_vocabulary = "GCMD Science Keywords";
  :license = "The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.";
  :naming_authority = "gov.noaa.pfeg.coastwatch";
  :Northernmost_Northing = 89.875; // double
  :origin = "Remote Sensing Systems, Inc";
  :processing_level = "3";
  :project = "CoastWatch (https://coastwatch.noaa.gov/)";
  :projection = "geographic";
  :projection_type = "mapped";
  :references = "RSS Inc. Winds: http://www.remss.com/ .";
  :satellite = "QuikSCAT";
  :sensor = "SeaWinds";
  :source = "satellite observation: QuikSCAT, SeaWinds";
  :sourceUrl = "http://192.168.31.18/thredds/dodsC/satellite/QS/ux10/1day";
  :Southernmost_Northing = -89.875; // double
  :standard_name_vocabulary = "CF Standard Name Table v70";
  :summary = "Remote Sensing Inc. distributes science quality wind velocity data from the SeaWinds instrument onboard NASA\\'s QuikSCAT satellite.  SeaWinds is a microwave scatterometer designed to measure surface winds over the global ocean.  Wind velocity fields are provided in zonal, meriodonal, and modulus sets. The reference height for all wind velocities is 10 meters.";
  :time_coverage_end = "YYYY-MM-DDTHH:mm:ssZ";
  :time_coverage_start = "YYYY-MM-DDTHH:mm:ssZ";
  :title = "Wind, QuikSCAT, Global, Science Quality (1 Day Composite)";
  :Westernmost_Easting = 0.125; // double
}
            """;
    expected = expected.replaceAll("\r\n", "\n");
    results = results.replaceAll("\r\n", "\n");
    expected =
        expected.substring(0, expected.indexOf("  :history = "))
            + expected.substring(expected.indexOf("  :infoUrl = "));
    results =
        results.substring(0, results.indexOf("  :history = "))
            + results.substring(results.indexOf("  :infoUrl = "));
    expected =
        expected.substring(0, expected.indexOf("  :summary = "))
            + expected.substring(expected.indexOf("  :time_coverage_end = "));
    results =
        results.substring(0, results.indexOf("  :summary = "))
            + results.substring(results.indexOf("  :time_coverage_end = "));

    results = results.replaceAll("\'", "'");
    expected = expected.replaceAll("\'", "'");
    com.cohort.util.Test.ensureEqual(expected, results, "results=\n" + results);

    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]",
            EDStatic.config.fullTestCacheDirectory,
            edd.className() + "_ywind",
            ".ncHeader");
    results = File2.directReadFrom88591File(EDStatic.config.fullTestCacheDirectory + tName);
    results =
        results.replaceAll("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z", "YYYY-MM-DDTHH:mm:ssZ");
    results = results.replaceFirst("^netcdf\\s+(.+?)\\s*\\{", "netcdf EDDGridCopy_ywind.nc {");
    expected =
"""
netcdf EDDGridCopy_ywind.nc {
  dimensions:
    time = 1;
    altitude = 1;
    latitude = 1;
    longitude = 11;
  variables:
    double time(time=1);
      :_CoordinateAxisType = "Time";
      :actual_range = 1.1999664E9, 1.1999664E9; // double
      :axis = "T";
      :fraction_digits = 0; // int
      :ioos_category = "Time";
      :long_name = "Centered Time";
      :standard_name = "time";
      :time_origin = "01-JAN-1970 00:00:00";
      :units = "seconds since YYYY-MM-DDTHH:mm:ssZ";

    double altitude(altitude=1);
      :_CoordinateAxisType = "Height";
      :_CoordinateZisPositive = "up";
      :actual_range = 0.0, 0.0; // double
      :axis = "Z";
      :fraction_digits = 0; // int
      :ioos_category = "Location";
      :long_name = "Altitude";
      :positive = "up";
      :standard_name = "altitude";
      :units = "m";

    double latitude(latitude=1);
      :_CoordinateAxisType = "Lat";
      :actual_range = 36.625, 36.625; // double
      :axis = "Y";
      :coordsys = "geographic";
      :fraction_digits = 2; // int
      :ioos_category = "Location";
      :long_name = "Latitude";
      :point_spacing = "even";
      :standard_name = "latitude";
      :units = "degrees_north";

    double longitude(longitude=11);
      :_CoordinateAxisType = "Lon";
      :actual_range = 230.125, 237.625; // double
      :axis = "X";
      :coordsys = "geographic";
      :fraction_digits = 2; // int
      :ioos_category = "Location";
      :long_name = "Longitude";
      :point_spacing = "even";
      :standard_name = "longitude";
      :units = "degrees_east";

    float y_wind(time=1, altitude=1, latitude=1, longitude=11);
      :_FillValue = -9999999.0f; // float
      :colorBarMaximum = 15.0; // double
      :colorBarMinimum = -15.0; // double
      :coordsys = "geographic";
      :fraction_digits = 1; // int
      :ioos_category = "Wind";
      :long_name = "Meridional Wind";
      :missing_value = -9999999.0f; // float
      :standard_name = "y_wind";
      :units = "m s-1";

  // global attributes:
  :acknowledgement = "NOAA NESDIS COASTWATCH, NOAA SWFSC ERD";
  :cdm_data_type = "Grid";
  :composite = "true";
  :contributor_name = "Remote Sensing Systems, Inc";
  :contributor_role = "Source of level 2 data.";
  :Conventions = "COARDS, CF-1.6, ACDD-1.3";
  :creator_email = "erd.data@noaa.gov";
  :creator_name = "NOAA CoastWatch, West Coast Node";
  :creator_url = "https://coastwatch.pfeg.noaa.gov";
  :date_created = "2008-08-29";
  :date_issued = "2008-08-29";
  :Easternmost_Easting = 237.625; // double
  :geospatial_lat_max = 36.625; // double
  :geospatial_lat_min = 36.625; // double
  :geospatial_lat_resolution = 0.25; // double
  :geospatial_lat_units = "degrees_north";
  :geospatial_lon_max = 237.625; // double
  :geospatial_lon_min = 230.125; // double
  :geospatial_lon_resolution = 0.25; // double
  :geospatial_lon_units = "degrees_east";
  :geospatial_vertical_max = 0.0; // double
  :geospatial_vertical_min = 0.0; // double
  :geospatial_vertical_positive = "up";
  :geospatial_vertical_units = "m";
  :history = "Remote Sensing Systems, Inc
YYYY-MM-DDTHH:mm:ssZ NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD
YYYY-MM-DDTHH:mm:ssZ http://192.168.31.18/thredds/dodsC/satellite/QS/ux10/1day
YYYY-MM-DDTHH:mm:ssZ http://localhost:8080/erddap/griddap/testGridCopy.ncHeader?y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]";
  :infoUrl = "https://coastwatch.pfeg.noaa.gov/infog/QS_ux10_las.html";
  :institution = "NOAA CoastWatch, West Coast Node";
  :keywords = "Earth Science > Oceans > Ocean Winds > Surface Winds";
  :keywords_vocabulary = "GCMD Science Keywords";
  :license = "The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.";
  :naming_authority = "gov.noaa.pfeg.coastwatch";
  :Northernmost_Northing = 36.625; // double
  :origin = "Remote Sensing Systems, Inc";
  :processing_level = "3";
  :project = "CoastWatch (https://coastwatch.noaa.gov/)";
  :projection = "geographic";
  :projection_type = "mapped";
  :references = "RSS Inc. Winds: http://www.remss.com/ .";
  :satellite = "QuikSCAT";
  :sensor = "SeaWinds";
  :source = "satellite observation: QuikSCAT, SeaWinds";
  :sourceUrl = "http://192.168.31.18/thredds/dodsC/satellite/QS/ux10/1day";
  :Southernmost_Northing = 36.625; // double
  :standard_name_vocabulary = "CF Standard Name Table v70";
  :summary = "Remote Sensing Inc. distributes science quality wind velocity data from the SeaWinds instrument onboard NASA\\'s QuikSCAT satellite.  SeaWinds is a microwave scatterometer designed to measure surface winds over the global ocean.  Wind velocity fields are provided in zonal, meriodonal, and modulus sets. The reference height for all wind velocities is 10 meters.";
  :time_coverage_end = "YYYY-MM-DDTHH:mm:ssZ";
  :time_coverage_start = "YYYY-MM-DDTHH:mm:ssZ";
  :title = "Wind, QuikSCAT, Global, Science Quality (1 Day Composite)";
  :Westernmost_Easting = 230.125; // double
}
            """;
    expected = expected.replaceAll("\r\n", "\n");
    results = results.replaceAll("\r\n", "\n");
    expected =
        expected.substring(0, expected.indexOf("  :history = "))
            + expected.substring(expected.indexOf("  :infoUrl = "));
    results =
        results.substring(0, results.indexOf("  :history = "))
            + results.substring(results.indexOf("  :infoUrl = "));
    expected =
        expected.substring(0, expected.indexOf("  :summary = "))
            + expected.substring(expected.indexOf("  :time_coverage_end = "));
    results =
        results.substring(0, results.indexOf("  :summary = "))
            + results.substring(results.indexOf("  :time_coverage_end = "));
    results = results.replaceAll("\'", "'");
    expected = expected.replaceAll("\'", "'");
    com.cohort.util.Test.ensureEqual(expected, results, "results=\n" + results);
  }

  /**
   * @throws Throwable if trouble
   */
  @Test
  void testGridNcFiles() throws Throwable {
    int language = 0;
    String tName, results, expected;

    EDD edd = EDDTestDataset.gettestGriddedNcFiles();
    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.config.fullTestCacheDirectory,
            edd.className(),
            ".ncHeader");
    results = File2.directReadFrom88591File(EDStatic.config.fullTestCacheDirectory + tName);
    results = results.replaceFirst("^netcdf\\s+(.+?)\\s*\\{", "netcdf EDDGridFromNcFiles.nc {");
    results =
        results.replaceAll("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z", "YYYY-MM-DDTHH:mm:ssZ");
    expected =
"""
netcdf EDDGridFromNcFiles.nc {
  dimensions:
    time = 10;
    altitude = 1;
    latitude = 720;
    longitude = 1440;
  variables:
    double time(time=10);
      :_CoordinateAxisType = "Time";
      :actual_range = 1.1991888E9, 1.1999664E9; // double
      :axis = "T";
      :fraction_digits = 0; // int
      :ioos_category = "Time";
      :long_name = "Centered Time";
      :standard_name = "time";
      :time_origin = "01-JAN-1970 00:00:00";
      :units = "seconds since YYYY-MM-DDTHH:mm:ssZ";

    double altitude(altitude=1);
      :_CoordinateAxisType = "Height";
      :_CoordinateZisPositive = "up";
      :actual_range = 0.0, 0.0; // double
      :axis = "Z";
      :fraction_digits = 0; // int
      :ioos_category = "Location";
      :long_name = "Altitude";
      :positive = "up";
      :standard_name = "altitude";
      :units = "m";

    double latitude(latitude=720);
      :_CoordinateAxisType = "Lat";
      :actual_range = -89.875, 89.875; // double
      :axis = "Y";
      :coordsys = "geographic";
      :fraction_digits = 2; // int
      :ioos_category = "Location";
      :long_name = "Latitude";
      :point_spacing = "even";
      :standard_name = "latitude";
      :units = "degrees_north";

    double longitude(longitude=1440);
      :_CoordinateAxisType = "Lon";
      :actual_range = 0.125, 359.875; // double
      :axis = "X";
      :coordsys = "geographic";
      :fraction_digits = 2; // int
      :ioos_category = "Location";
      :long_name = "Longitude";
      :point_spacing = "even";
      :standard_name = "longitude";
      :units = "degrees_east";

    float x_wind(time=10, altitude=1, latitude=720, longitude=1440);
      :_FillValue = -9999999.0f; // float
      :colorBarMaximum = 15.0; // double
      :colorBarMinimum = -15.0; // double
      :coordsys = "geographic";
      :fraction_digits = 1; // int
      :ioos_category = "Wind";
      :long_name = "Zonal Wind";
      :missing_value = -9999999.0f; // float
      :standard_name = "x_wind";
      :units = "m s-1";

    float y_wind(time=10, altitude=1, latitude=720, longitude=1440);
      :_FillValue = -9999999.0f; // float
      :colorBarMaximum = 15.0; // double
      :colorBarMinimum = -15.0; // double
      :coordsys = "geographic";
      :fraction_digits = 1; // int
      :ioos_category = "Wind";
      :long_name = "Meridional Wind";
      :missing_value = -9999999.0f; // float
      :standard_name = "y_wind";
      :units = "m s-1";

    float mod(time=10, altitude=1, latitude=720, longitude=1440);
      :_FillValue = -9999999.0f; // float
      :colorBarMaximum = 18.0; // double
      :colorBarMinimum = 0.0; // double
      :colorBarPalette = "WhiteRedBlack";
      :coordsys = "geographic";
      :fraction_digits = 1; // int
      :ioos_category = "Wind";
      :long_name = "Modulus of Wind";
      :missing_value = -9999999.0f; // float
      :units = "m s-1";

  // global attributes:
  :acknowledgement = "NOAA NESDIS COASTWATCH, NOAA SWFSC ERD";
  :cdm_data_type = "Grid";
  :composite = "true";
  :contributor_name = "Remote Sensing Systems, Inc";
  :contributor_role = "Source of level 2 data.";
  :Conventions = "COARDS, CF-1.6, ACDD-1.3";
  :creator_email = "erd.data@noaa.gov";
  :creator_name = "NOAA CoastWatch, West Coast Node";
  :creator_url = "https://coastwatch.pfeg.noaa.gov";
  :date_created = "2008-08-29";
  :date_issued = "2008-08-29";
  :Easternmost_Easting = 359.875; // double
  :geospatial_lat_max = 89.875; // double
  :geospatial_lat_min = -89.875; // double
  :geospatial_lat_resolution = 0.25; // double
  :geospatial_lat_units = "degrees_north";
  :geospatial_lon_max = 359.875; // double
  :geospatial_lon_min = 0.125; // double
  :geospatial_lon_resolution = 0.25; // double
  :geospatial_lon_units = "degrees_east";
  :geospatial_vertical_max = 0.0; // double
  :geospatial_vertical_min = 0.0; // double
  :geospatial_vertical_positive = "up";
  :geospatial_vertical_units = "m";
  :history = "Remote Sensing Systems, Inc
YYYY-MM-DDTHH:mm:ssZ NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD
YYYY-MM-DDTHH:mm:ssZ http://192.168.31.18/thredds/dodsC/satellite/QS/ux10/1day
YYYY-MM-DDTHH:mm:ssZ http://localhost:8080/erddap/griddap/testGriddedNcFiles.ncHeader";
  :infoUrl = "https://coastwatch.pfeg.noaa.gov/infog/QS_ux10_las.html";
  :institution = "NOAA CoastWatch, West Coast Node";
  :keywords = "Earth Science > Oceans > Ocean Winds > Surface Winds";
  :keywords_vocabulary = "GCMD Science Keywords";
  :license = "The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.";
  :naming_authority = "gov.noaa.pfeg.coastwatch";
  :Northernmost_Northing = 89.875; // double
  :origin = "Remote Sensing Systems, Inc";
  :processing_level = "3";
  :project = "CoastWatch (https://coastwatch.noaa.gov/)";
  :projection = "geographic";
  :projection_type = "mapped";
  :references = "RSS Inc. Winds: http://www.remss.com/ .";
  :satellite = "QuikSCAT";
  :sensor = "SeaWinds";
  :source = "satellite observation: QuikSCAT, SeaWinds";
  :sourceUrl = "http://192.168.31.18/thredds/dodsC/satellite/QS/ux10/1day";
  :Southernmost_Northing = -89.875; // double
  :standard_name_vocabulary = "CF Standard Name Table v70";
  :summary = "Remote Sensing Inc. distributes science quality wind velocity data from the SeaWinds instrument onboard NASA\\'s QuikSCAT satellite.  SeaWinds is a microwave scatterometer designed to measure surface winds over the global ocean.  Wind velocity fields are provided in zonal, meriodonal, and modulus sets. The reference height for all wind velocities is 10 meters.";
  :time_coverage_end = "YYYY-MM-DDTHH:mm:ssZ";
  :time_coverage_start = "YYYY-MM-DDTHH:mm:ssZ";
  :title = "Wind, QuikSCAT, Global, Science Quality (1 Day Composite)";
  :Westernmost_Easting = 0.125; // double
}
            """;
    expected = expected.replaceAll("\r\n", "\n");
    results = results.replaceAll("\r\n", "\n");
    expected =
        expected.substring(0, expected.indexOf("  :history = "))
            + expected.substring(expected.indexOf("  :infoUrl = "));
    results =
        results.substring(0, results.indexOf("  :history = "))
            + results.substring(results.indexOf("  :infoUrl = "));
    expected =
        expected.substring(0, expected.indexOf("  :summary = "))
            + expected.substring(expected.indexOf("  :time_coverage_end = "));
    results =
        results.substring(0, results.indexOf("  :summary = "))
            + results.substring(results.indexOf("  :time_coverage_end = "));
    results = results.replaceAll("\\'", "'");
    expected = expected.replaceAll("\\'", "'");
    com.cohort.util.Test.ensureEqual(expected, results, "results=\n" + results);

    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]",
            EDStatic.config.fullTestCacheDirectory,
            edd.className() + "_ywind",
            ".ncHeader");
    results = File2.directReadFrom88591File(EDStatic.config.fullTestCacheDirectory + tName);
    results =
        results.replaceFirst("^netcdf\\s+(.+?)\\s*\\{", "netcdf EDDGridFromNcFiles_ywind.nc {");
    results =
        results.replaceAll("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z", "YYYY-MM-DDTHH:mm:ssZ");
    expected =
"""
netcdf EDDGridFromNcFiles_ywind.nc {
  dimensions:
    time = 1;
    altitude = 1;
    latitude = 1;
    longitude = 11;
  variables:
    double time(time=1);
      :_CoordinateAxisType = "Time";
      :actual_range = 1.1999664E9, 1.1999664E9; // double
      :axis = "T";
      :fraction_digits = 0; // int
      :ioos_category = "Time";
      :long_name = "Centered Time";
      :standard_name = "time";
      :time_origin = "01-JAN-1970 00:00:00";
      :units = "seconds since YYYY-MM-DDTHH:mm:ssZ";

    double altitude(altitude=1);
      :_CoordinateAxisType = "Height";
      :_CoordinateZisPositive = "up";
      :actual_range = 0.0, 0.0; // double
      :axis = "Z";
      :fraction_digits = 0; // int
      :ioos_category = "Location";
      :long_name = "Altitude";
      :positive = "up";
      :standard_name = "altitude";
      :units = "m";

    double latitude(latitude=1);
      :_CoordinateAxisType = "Lat";
      :actual_range = 36.625, 36.625; // double
      :axis = "Y";
      :coordsys = "geographic";
      :fraction_digits = 2; // int
      :ioos_category = "Location";
      :long_name = "Latitude";
      :point_spacing = "even";
      :standard_name = "latitude";
      :units = "degrees_north";

    double longitude(longitude=11);
      :_CoordinateAxisType = "Lon";
      :actual_range = 230.125, 237.625; // double
      :axis = "X";
      :coordsys = "geographic";
      :fraction_digits = 2; // int
      :ioos_category = "Location";
      :long_name = "Longitude";
      :point_spacing = "even";
      :standard_name = "longitude";
      :units = "degrees_east";

    float y_wind(time=1, altitude=1, latitude=1, longitude=11);
      :_FillValue = -9999999.0f; // float
      :colorBarMaximum = 15.0; // double
      :colorBarMinimum = -15.0; // double
      :coordsys = "geographic";
      :fraction_digits = 1; // int
      :ioos_category = "Wind";
      :long_name = "Meridional Wind";
      :missing_value = -9999999.0f; // float
      :standard_name = "y_wind";
      :units = "m s-1";

  // global attributes:
  :acknowledgement = "NOAA NESDIS COASTWATCH, NOAA SWFSC ERD";
  :cdm_data_type = "Grid";
  :composite = "true";
  :contributor_name = "Remote Sensing Systems, Inc";
  :contributor_role = "Source of level 2 data.";
  :Conventions = "COARDS, CF-1.6, ACDD-1.3";
  :creator_email = "erd.data@noaa.gov";
  :creator_name = "NOAA CoastWatch, West Coast Node";
  :creator_url = "https://coastwatch.pfeg.noaa.gov";
  :date_created = "2008-08-29";
  :date_issued = "2008-08-29";
  :Easternmost_Easting = 237.625; // double
  :geospatial_lat_max = 36.625; // double
  :geospatial_lat_min = 36.625; // double
  :geospatial_lat_resolution = 0.25; // double
  :geospatial_lat_units = "degrees_north";
  :geospatial_lon_max = 237.625; // double
  :geospatial_lon_min = 230.125; // double
  :geospatial_lon_resolution = 0.25; // double
  :geospatial_lon_units = "degrees_east";
  :geospatial_vertical_max = 0.0; // double
  :geospatial_vertical_min = 0.0; // double
  :geospatial_vertical_positive = "up";
  :geospatial_vertical_units = "m";
  :history = "Remote Sensing Systems, Inc
YYYY-MM-DDTHH:mm:ssZ NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD
YYYY-MM-DDTHH:mm:ssZ http://192.168.31.18/thredds/dodsC/satellite/QS/ux10/1day
YYYY-MM-DDTHH:mm:ssZ http://localhost:8080/erddap/griddap/testGriddedNcFiles.ncHeader?y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]";
  :infoUrl = "https://coastwatch.pfeg.noaa.gov/infog/QS_ux10_las.html";
  :institution = "NOAA CoastWatch, West Coast Node";
  :keywords = "Earth Science > Oceans > Ocean Winds > Surface Winds";
  :keywords_vocabulary = "GCMD Science Keywords";
  :license = "The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.";
  :naming_authority = "gov.noaa.pfeg.coastwatch";
  :Northernmost_Northing = 36.625; // double
  :origin = "Remote Sensing Systems, Inc";
  :processing_level = "3";
  :project = "CoastWatch (https://coastwatch.noaa.gov/)";
  :projection = "geographic";
  :projection_type = "mapped";
  :references = "RSS Inc. Winds: http://www.remss.com/ .";
  :satellite = "QuikSCAT";
  :sensor = "SeaWinds";
  :source = "satellite observation: QuikSCAT, SeaWinds";
  :sourceUrl = "http://192.168.31.18/thredds/dodsC/satellite/QS/ux10/1day";
  :Southernmost_Northing = 36.625; // double
  :standard_name_vocabulary = "CF Standard Name Table v70";
  :summary = "Remote Sensing Inc. distributes science quality wind velocity data from the SeaWinds instrument onboard NASA\\'s QuikSCAT satellite.  SeaWinds is a microwave scatterometer designed to measure surface winds over the global ocean.  Wind velocity fields are provided in zonal, meriodonal, and modulus sets. The reference height for all wind velocities is 10 meters.";
  :time_coverage_end = "YYYY-MM-DDTHH:mm:ssZ";
  :time_coverage_start = "YYYY-MM-DDTHH:mm:ssZ";
  :title = "Wind, QuikSCAT, Global, Science Quality (1 Day Composite)";
  :Westernmost_Easting = 230.125; // double
}
            """;
    expected = expected.replaceAll("\r\n", "\n");
    results = results.replaceAll("\r\n", "\n");
    expected =
        expected.substring(0, expected.indexOf("  :history = "))
            + expected.substring(expected.indexOf("  :infoUrl = "));
    results =
        results.substring(0, results.indexOf("  :history = "))
            + results.substring(results.indexOf("  :infoUrl = "));
    expected =
        expected.substring(0, expected.indexOf("  :summary = "))
            + expected.substring(expected.indexOf("  :time_coverage_end = "));
    results =
        results.substring(0, results.indexOf("  :summary = "))
            + results.substring(results.indexOf("  :time_coverage_end = "));
    results = results.replaceAll("\'", "'");
    expected = expected.replaceAll("\'", "'");
    com.cohort.util.Test.ensureEqual(expected, results, "results=\n" + results);
  }

  /**
   * @throws Throwable if trouble
   */
  @Test
  void testTableNcFiles() throws Throwable {
    int language = 0;
    String tName, results, expected;

    EDD edd = EDDTestDataset.geterdCinpKfmSFNH();
    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.config.fullTestCacheDirectory,
            edd.className(),
            ".ncHeader");
    results = File2.directReadFrom88591File(EDStatic.config.fullTestCacheDirectory + tName);
    results =
        results.replaceAll("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z", "YYYY-MM-DDTHH:mm:ssZ");
    results = results.replaceFirst("^netcdf\\s+(.+?)\\s*\\{", "netcdf EDDTableFromNcFiles.nc {");
    expected =
"""
netcdf EDDTableFromNcFiles.nc {
  dimensions:
    row = 263242;
    id_strlen = 35;
    common_name_strlen = 22;
    species_name_strlen = 31;
  variables:
    char id(row=263242, id_strlen=35);
      :_Encoding = "ISO-8859-1";
      :cf_role = "timeseries_id";
      :ioos_category = "Identifier";
      :long_name = "Station Identifier";

    double longitude(row=263242);
      :_CoordinateAxisType = "Lon";
      :actual_range = -120.4, -118.4; // double
      :axis = "X";
      :colorBarMaximum = -118.4; // double
      :colorBarMinimum = -120.4; // double
      :ioos_category = "Location";
      :long_name = "Longitude";
      :standard_name = "longitude";
      :units = "degrees_east";

    double latitude(row=263242);
      :_CoordinateAxisType = "Lat";
      :actual_range = 32.8, 34.05; // double
      :axis = "Y";
      :colorBarMaximum = 34.5; // double
      :colorBarMinimum = 32.5; // double
      :ioos_category = "Location";
      :long_name = "Latitude";
      :standard_name = "latitude";
      :units = "degrees_north";

    double depth(row=263242);
      :_CoordinateAxisType = "Height";
      :_CoordinateZisPositive = "down";
      :actual_range = 5.0, 17.0; // double
      :axis = "Z";
      :colorBarMaximum = 20.0; // double
      :colorBarMinimum = 0.0; // double
      :ioos_category = "Location";
      :long_name = "Depth";
      :positive = "down";
      :standard_name = "depth";
      :units = "m";

    double time(row=263242);
      :_CoordinateAxisType = "Time";
      :actual_range = 4.89024E8, 1.183248E9; // double
      :axis = "T";
      :colorBarMaximum = 1.183248E9; // double
      :colorBarMinimum = 4.89024E8; // double
      :ioos_category = "Time";
      :long_name = "Time";
      :standard_name = "time";
      :time_origin = "01-JAN-1970 00:00:00";
      :units = "seconds since YYYY-MM-DDTHH:mm:ssZ";

    char common_name(row=263242, common_name_strlen=22);
      :_Encoding = "ISO-8859-1";
      :ioos_category = "Taxonomy";
      :long_name = "Common Name";

    char species_name(row=263242, species_name_strlen=31);
      :_Encoding = "ISO-8859-1";
      :ioos_category = "Taxonomy";
      :long_name = "Species Name";

    short size(row=263242);
      :_FillValue = 32767S; // short
      :actual_range = 1S, 385S; // short
      :ioos_category = "Biology";
      :long_name = "Size";
      :units = "mm";

  // global attributes:
  :acknowledgement = "NOAA NESDIS COASTWATCH, NOAA SWFSC ERD, Channel Islands National Park, National Park Service";
  :cdm_data_type = "TimeSeries";
  :cdm_timeseries_variables = "id, longitude, latitude";
  :contributor_email = "David_Kushner@nps.gov";
  :contributor_name = "Channel Islands National Park, National Park Service";
  :contributor_role = "Source of data.";
  :Conventions = "COARDS, CF-1.6, ACDD-1.3";
  :creator_email = "erd.data@noaa.gov";
  :creator_name = "NOAA NMFS SWFSC ERD";
  :creator_type = "institution";
  :creator_url = "https://www.pfeg.noaa.gov";
  :date_created = "YYYY-MM-DDTHH:mm:ssZ";
  :date_issued = "YYYY-MM-DDTHH:mm:ssZ";
  :Easternmost_Easting = -118.4; // double
  :featureType = "TimeSeries";
  :geospatial_lat_max = 34.05; // double
  :geospatial_lat_min = 32.8; // double
  :geospatial_lat_units = "degrees_north";
  :geospatial_lon_max = -118.4; // double
  :geospatial_lon_min = -120.4; // double
  :geospatial_lon_units = "degrees_east";
  :geospatial_vertical_max = 17.0; // double
  :geospatial_vertical_min = 5.0; // double
  :geospatial_vertical_positive = "down";
  :geospatial_vertical_units = "m";
  :history = "Channel Islands National Park, National Park Service
YYYY-MM-DDTHH:mm:ssZ NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD
YYYY-MM-DDTHH:mm:ssZ (local files)
YYYY-MM-DDTHH:mm:ssZ http://localhost:8080/erddap/tabledap/erdCinpKfmSFNH.ncHeader";
  :id = "erdCinpKfmSFNH";
  :infoUrl = "https://www.nps.gov/chis/naturescience/index.htm";
  :institution = "CINP";
  :keywords = "aquatic, atmosphere, biology, biosphere, channel, cinp, coastal, common, depth, Earth Science > Biosphere > Aquatic Ecosystems > Coastal Habitat, Earth Science > Biosphere > Aquatic Ecosystems > Marine Habitat, ecosystems, forest, frequency, habitat, height, identifier, islands, kelp, marine, monitoring, name, natural, size, species, station, taxonomy, time";
  :keywords_vocabulary = "GCMD Science Keywords";
  :license = "The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.  National Park Service Disclaimer: The National Park Service shall not be held liable for improper or incorrect use of the data described and/or contained herein. These data and related graphics are not legal documents and are not intended to be used as such. The information contained in these data is dynamic and may change over time. The data are not better than the original sources from which they were derived. It is the responsibility of the data user to use the data appropriately and consistent within the limitation of geospatial data in general and these data in particular. The related graphics are intended to aid the data user in acquiring relevant data; it is not appropriate to use the related graphics as data. The National Park Service gives no warranty, expressed or implied, as to the accuracy, reliability, or completeness of these data. It is strongly recommended that these data are directly acquired from an NPS server and not indirectly through other sources which may have changed the data in some way. Although these data have been processed successfully on computer systems at the National Park Service, no warranty expressed or implied is made regarding the utility of the data on other systems for general or scientific purposes, nor shall the act of distribution constitute any such warranty. This disclaimer applies both to individual use of the data and aggregate use with other data.";
  :naming_authority = "gov.noaa.pfeg.coastwatch";
  :Northernmost_Northing = 34.05; // double
  :observationDimension = "row";
  :project = "NOAA NMFS SWFSC ERD (https://www.pfeg.noaa.gov/)";
  :references = "Channel Islands National Parks Inventory and Monitoring information: http://nature.nps.gov/im/units/medn . Kelp Forest Monitoring Protocols: http://www.nature.nps.gov/im/units/chis/Reports_PDF/Marine/KFM-HandbookVol1.pdf .";
  :sourceUrl = "(local files)";
  :Southernmost_Northing = 32.8; // double
  :standard_name_vocabulary = "CF Standard Name Table v70";
  :subsetVariables = "id, longitude, latitude, common_name, species_name";
  :summary = "This dataset has measurements of the size of selected animal species at selected locations in the Channel Islands National Park. Sampling is conducted annually between the months of May-October, so the Time data in this file is July 1 of each year (a nominal value). The size frequency measurements were taken within 10 meters of the transect line at each site.  Depths at the site vary some, but we describe the depth of the site along the transect line where that station\\'s temperature logger is located, a typical depth for the site.";
  :time_coverage_end = "YYYY-MM-DDTHH:mm:ssZ";
  :time_coverage_start = "YYYY-MM-DDTHH:mm:ssZ";
  :title = "Channel Islands, Kelp Forest Monitoring, Size and Frequency, Natural Habitat, 1985-2007";
  :Westernmost_Easting = -120.4; // double
}
            """;
    expected = expected.replaceAll("\r\n", "\n");
    results = results.replaceAll("\r\n", "\n");
    expected =
        expected.substring(0, expected.indexOf("  :history = "))
            + expected.substring(expected.indexOf("  :infoUrl = "));
    results =
        results.substring(0, results.indexOf("  :history = "))
            + results.substring(results.indexOf("  :infoUrl = "));
    expected =
        expected.substring(0, expected.indexOf("  :summary = "))
            + expected.substring(expected.indexOf("  :time_coverage_end = "));
    results =
        results.substring(0, results.indexOf("  :summary = "))
            + results.substring(results.indexOf("  :time_coverage_end = "));
    results = results.replaceAll("\'", "'");
    expected = expected.replaceAll("\'", "'");
    com.cohort.util.Test.ensureEqual(expected, results, "results=\n" + results);

    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&longitude=-119.05&latitude=33.46666666666&time=2005-07-01T00:00:00",
            EDStatic.config.fullTestCacheDirectory,
            edd.className() + "_1time",
            ".ncHeader");
    results = File2.directReadFrom88591File(EDStatic.config.fullTestCacheDirectory + tName);
    results =
        results.replaceAll("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z", "YYYY-MM-DDTHH:mm:ssZ");
    results =
        results.replaceFirst("^netcdf\\s+(.+?)\\s*\\{", "netcdf EDDTableFromNcFiles_1time.nc {");
    expected =
"""
netcdf EDDTableFromNcFiles_1time.nc {
  dimensions:
    row = 681;
    id_strlen = 30;
    common_name_strlen = 21;
    species_name_strlen = 31;
  variables:
    char id(row=681, id_strlen=30);
      :_Encoding = "ISO-8859-1";
      :cf_role = "timeseries_id";
      :ioos_category = "Identifier";
      :long_name = "Station Identifier";

    double longitude(row=681);
      :_CoordinateAxisType = "Lon";
      :actual_range = -119.05, -119.05; // double
      :axis = "X";
      :colorBarMaximum = -118.4; // double
      :colorBarMinimum = -120.4; // double
      :ioos_category = "Location";
      :long_name = "Longitude";
      :standard_name = "longitude";
      :units = "degrees_east";

    double latitude(row=681);
      :_CoordinateAxisType = "Lat";
      :actual_range = 33.4666666666667, 33.4666666666667; // double
      :axis = "Y";
      :colorBarMaximum = 34.5; // double
      :colorBarMinimum = 32.5; // double
      :ioos_category = "Location";
      :long_name = "Latitude";
      :standard_name = "latitude";
      :units = "degrees_north";

    double depth(row=681);
      :_CoordinateAxisType = "Height";
      :_CoordinateZisPositive = "down";
      :actual_range = 14.0, 14.0; // double
      :axis = "Z";
      :colorBarMaximum = 20.0; // double
      :colorBarMinimum = 0.0; // double
      :ioos_category = "Location";
      :long_name = "Depth";
      :positive = "down";
      :standard_name = "depth";
      :units = "m";

    double time(row=681);
      :_CoordinateAxisType = "Time";
      :actual_range = 1.120176E9, 1.120176E9; // double
      :axis = "T";
      :colorBarMaximum = 1.183248E9; // double
      :colorBarMinimum = 4.89024E8; // double
      :ioos_category = "Time";
      :long_name = "Time";
      :standard_name = "time";
      :time_origin = "01-JAN-1970 00:00:00";
      :units = "seconds since YYYY-MM-DDTHH:mm:ssZ";

    char common_name(row=681, common_name_strlen=21);
      :_Encoding = "ISO-8859-1";
      :ioos_category = "Taxonomy";
      :long_name = "Common Name";

    char species_name(row=681, species_name_strlen=31);
      :_Encoding = "ISO-8859-1";
      :ioos_category = "Taxonomy";
      :long_name = "Species Name";

    short size(row=681);
      :_FillValue = 32767S; // short
      :actual_range = 5S, 171S; // short
      :ioos_category = "Biology";
      :long_name = "Size";
      :units = "mm";

  // global attributes:
  :acknowledgement = "NOAA NESDIS COASTWATCH, NOAA SWFSC ERD, Channel Islands National Park, National Park Service";
  :cdm_data_type = "TimeSeries";
  :cdm_timeseries_variables = "id, longitude, latitude";
  :contributor_email = "David_Kushner@nps.gov";
  :contributor_name = "Channel Islands National Park, National Park Service";
  :contributor_role = "Source of data.";
  :Conventions = "COARDS, CF-1.6, ACDD-1.3";
  :creator_email = "erd.data@noaa.gov";
  :creator_name = "NOAA NMFS SWFSC ERD";
  :creator_type = "institution";
  :creator_url = "https://www.pfeg.noaa.gov";
  :date_created = "YYYY-MM-DDTHH:mm:ssZ";
  :date_issued = "YYYY-MM-DDTHH:mm:ssZ";
  :Easternmost_Easting = -119.05; // double
  :featureType = "TimeSeries";
  :geospatial_lat_max = 33.4666666666667; // double
  :geospatial_lat_min = 33.4666666666667; // double
  :geospatial_lat_units = "degrees_north";
  :geospatial_lon_max = -119.05; // double
  :geospatial_lon_min = -119.05; // double
  :geospatial_lon_units = "degrees_east";
  :geospatial_vertical_max = 14.0; // double
  :geospatial_vertical_min = 14.0; // double
  :geospatial_vertical_positive = "down";
  :geospatial_vertical_units = "m";
  :history = "Channel Islands National Park, National Park Service
YYYY-MM-DDTHH:mm:ssZ NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD
YYYY-MM-DDTHH:mm:ssZ (local files)
YYYY-MM-DDTHH:mm:ssZ http://localhost:8080/erddap/tabledap/erdCinpKfmSFNH.ncHeader?&longitude=-119.05&latitude=33.46666666666&time=2005-07-01T00:00:00";
  :id = "erdCinpKfmSFNH";
  :infoUrl = "https://www.nps.gov/chis/naturescience/index.htm";
  :institution = "CINP";
  :keywords = "aquatic, atmosphere, biology, biosphere, channel, cinp, coastal, common, depth, Earth Science > Biosphere > Aquatic Ecosystems > Coastal Habitat, Earth Science > Biosphere > Aquatic Ecosystems > Marine Habitat, ecosystems, forest, frequency, habitat, height, identifier, islands, kelp, marine, monitoring, name, natural, size, species, station, taxonomy, time";
  :keywords_vocabulary = "GCMD Science Keywords";
  :license = "The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.  National Park Service Disclaimer: The National Park Service shall not be held liable for improper or incorrect use of the data described and/or contained herein. These data and related graphics are not legal documents and are not intended to be used as such. The information contained in these data is dynamic and may change over time. The data are not better than the original sources from which they were derived. It is the responsibility of the data user to use the data appropriately and consistent within the limitation of geospatial data in general and these data in particular. The related graphics are intended to aid the data user in acquiring relevant data; it is not appropriate to use the related graphics as data. The National Park Service gives no warranty, expressed or implied, as to the accuracy, reliability, or completeness of these data. It is strongly recommended that these data are directly acquired from an NPS server and not indirectly through other sources which may have changed the data in some way. Although these data have been processed successfully on computer systems at the National Park Service, no warranty expressed or implied is made regarding the utility of the data on other systems for general or scientific purposes, nor shall the act of distribution constitute any such warranty. This disclaimer applies both to individual use of the data and aggregate use with other data.";
  :naming_authority = "gov.noaa.pfeg.coastwatch";
  :Northernmost_Northing = 33.4666666666667; // double
  :observationDimension = "row";
  :project = "NOAA NMFS SWFSC ERD (https://www.pfeg.noaa.gov/)";
  :references = "Channel Islands National Parks Inventory and Monitoring information: http://nature.nps.gov/im/units/medn . Kelp Forest Monitoring Protocols: http://www.nature.nps.gov/im/units/chis/Reports_PDF/Marine/KFM-HandbookVol1.pdf .";
  :sourceUrl = "(local files)";
  :Southernmost_Northing = 33.4666666666667; // double
  :standard_name_vocabulary = "CF Standard Name Table v70";
  :subsetVariables = "id, longitude, latitude, common_name, species_name";
  :summary = "This dataset has measurements of the size of selected animal species at selected locations in the Channel Islands National Park. Sampling is conducted annually between the months of May-October, so the Time data in this file is July 1 of each year (a nominal value). The size frequency measurements were taken within 10 meters of the transect line at each site.  Depths at the site vary some, but we describe the depth of the site along the transect line where that station\\'s temperature logger is located, a typical depth for the site.";
  :time_coverage_end = "YYYY-MM-DDTHH:mm:ssZ";
  :time_coverage_start = "YYYY-MM-DDTHH:mm:ssZ";
  :title = "Channel Islands, Kelp Forest Monitoring, Size and Frequency, Natural Habitat, 1985-2007";
  :Westernmost_Easting = -119.05; // double
}
            """;
    expected = expected.replaceAll("\r\n", "\n");
    results = results.replaceAll("\r\n", "\n");
    expected =
        expected.substring(0, expected.indexOf("  :history = "))
            + expected.substring(expected.indexOf("  :infoUrl = "));
    results =
        results.substring(0, results.indexOf("  :history = "))
            + results.substring(results.indexOf("  :infoUrl = "));
    expected =
        expected.substring(0, expected.indexOf("  :summary = "))
            + expected.substring(expected.indexOf("  :time_coverage_end = "));
    results =
        results.substring(0, results.indexOf("  :summary = "))
            + results.substring(results.indexOf("  :time_coverage_end = "));
    results = results.replaceAll("\'", "'");
    expected = expected.replaceAll("\'", "'");
    com.cohort.util.Test.ensureEqual(expected, results, "results=\n" + results);
  }
}
