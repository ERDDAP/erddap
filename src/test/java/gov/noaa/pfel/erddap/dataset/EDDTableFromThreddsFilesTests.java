package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.LongArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tags.TagIncompleteTest;
import tags.TagMissingDataset;
import tags.TagThredds;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

class EDDTableFromThreddsFilesTests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /**
   * This tests the methods in this class.
   *
   * @throws Throwable if trouble
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @TagIncompleteTest
  void testShipWTEP(boolean deleteCachedInfo) throws Throwable {
    // String2.log("\n****************** EDDTableFromThreddsFiles.testShipWTEP()
    // *****************\n");
    // testVerboseOn();
    int language = 0;
    String name, tName, results, tResults, expected, userDapQuery, tQuery;
    String error = "";
    int po;
    EDV edv;

    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 11); // [10]='T'

    String id = "fsuNoaaShipWTEP";
    if (deleteCachedInfo) EDDTableFromThreddsFiles.deleteCachedDatasetInfo(id);

    EDDTable eddTable = (EDDTable) EDDTestDataset.getfsuNoaaShipWTEP();

    // *** test getting das for entire dataset
    try {
      String2.log(
          "\n****************** EDDTableFromThreddsFiles testShipWTEP das and dds for entire dataset\n");
      tName =
          eddTable.makeNewFileForDapQuery(
              language,
              null,
              null,
              "",
              EDStatic.fullTestCacheDirectory,
              eddTable.className() + "_ShipEntire",
              ".das");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      // String2.log(results);
      boolean with = false; // 2014-01-09 several lines disappeared, 2016-09-16 returned, ...
      // disappeared,
      // 2019-05-20 returned, ...
      expected = // 2019-11-22 ~2 dozen small changes to centerline, precision, instrument,
          // qcindex, ...
          "Attributes \\{\n"
              + " s \\{\n"
              + "  ID \\{\n"
              + "    String cf_role \"trajectory_id\";\n"
              + "    String ioos_category \"Identifier\";\n"
              + "    String long_name \"Call Sign\";\n"
              + "  \\}\n"
              + "  site \\{\n"
              + "    String ioos_category \"Identifier\";\n"
              + "    String long_name \"Ship Name\";\n"
              + "  \\}\n"
              + "  IMO \\{\n"
              + "    String ioos_category \"Identifier\";\n"
              + "  \\}\n"
              + "  cruise_id \\{\n"
              + "    String ioos_category \"Identifier\";\n"
              + "  \\}\n"
              + "  expocode \\{\n"
              + "    String ioos_category \"Identifier\";\n"
              + "  \\}\n"
              + "  facility \\{\n"
              + "    String ioos_category \"Identifier\";\n"
              + "  \\}\n"
              + "  platform \\{\n"
              + "    String ioos_category \"Identifier\";\n"
              + "  \\}\n"
              + "  platform_version \\{\n"
              + "    String ioos_category \"Identifier\";\n"
              + "  \\}\n"
              + "  time \\{\n"
              + "    String _CoordinateAxisType \"Time\";\n"
              + "    Float64 actual_range 1.1886048e\\+9, .{8,14};\n"
              + // 2nd
              // number
              // changes
              "    String axis \"T\";\n"
              + "    Int32 data_interval 60;\n"
              + "    String ioos_category \"Time\";\n"
              + "    String long_name \"Time\";\n"
              + "    String observation_type \"calculated\";\n"
              + "    Int32 qcindex 1;\n"
              + "    String standard_name \"time\";\n"
              + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
              + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
              + "  \\}\n"
              + "  latitude \\{\n"
              + "    String _CoordinateAxisType \"Lat\";\n"
              + "    Float32 actual_range [0-9.-]+, [0-9.]+;\n"
              + // changed a
              // little
              "    String average_center \"time at end of period\";\n"
              + "    Int16 average_length 60;\n"
              + "    String average_method \"average\";\n"
              + "    String axis \"Y\";\n"
              + "    Float32 data_precision -9999.0;\n"
              + "    String instrument \"unknown\";\n"
              + "    String ioos_category \"Location\";\n"
              + "    String long_name \"Latitude\";\n"
              + "    String observation_type \"measured\";\n"
              + "    String original_units \"degrees \\(\\+N\\)\";\n"
              + "    Int32 qcindex 2;\n"
              + "    Float32 sampling_rate [0-9.]+;\n"
              +
              // " String serial_number \"8928\";\n" +
              "    String standard_name \"latitude\";\n"
              + "    String units \"degrees_north\";\n"
              + "  \\}\n"
              + "  longitude \\{\n"
              + "    String _CoordinateAxisType \"Lon\";\n"
              + "    Float32 actual_range [0-9.-]+, [0-9.]+;\n"
              + "    String average_center \"time at end of period\";\n"
              + "    Int16 average_length 60;\n"
              + "    String average_method \"average\";\n"
              + "    String axis \"X\";\n"
              + "    Float32 data_precision -9999.0;\n"
              + "    String instrument \"unknown\";\n"
              + "    String ioos_category \"Location\";\n"
              + "    String long_name \"Longitude\";\n"
              + "    String observation_type \"measured\";\n"
              + "    String original_units \"degrees \\(-W/\\+E\\)\";\n"
              + "    Int32 qcindex 3;\n"
              + "    Float32 sampling_rate [0-9.]+;\n"
              +
              // " String serial_number \"8928\";\n" +
              "    String standard_name \"longitude\";\n"
              + "    String units \"degrees_east\";\n"
              + "  \\}\n"
              + "  airPressure \\{\n"
              + "    Float32 _FillValue -8888.0;\n"
              + "    Float32 actual_range [0-9.]+, [0-9.]+;\n"
              + "    String average_center \"time at end of period\";\n"
              + "    Int16 average_length 60;\n"
              + "    String average_method \"average\";\n"
              + "    Float32 centerline_offset -9999.0;\n"
              + "    Float64 colorBarMaximum 1050.0;\n"
              + "    Float64 colorBarMinimum 950.0;\n"
              + "    Float32 data_precision -9999.0;\n"
              + "    Float32 distance_from_bow -9999.0;\n"
              + "    Float32 height -9999.0;\n"
              + "    String instrument \"unknown\";\n"
              + "    String ioos_category \"Pressure\";\n"
              + "    String long_name \"Atmospheric Pressure\";\n"
              + "    Float32 missing_value -9999.0;\n"
              + "    String mslp_indicator \"at sensor height\";\n"
              + "    String observation_type \"measured\";\n"
              + "    String original_units \"hectopascal\";\n"
              + "    Int32 qcindex [0-9]+;\n"
              + // 2021-02-26 was 19 2018-09-15
              // was 18
              "    Float32 sampling_rate [0-9.]+;\n"
              +
              // " String serial_number \"BPA13658\";\n" +
              "    Float32 special_value -8888.0;\n"
              + "    String standard_name \"air_pressure\";\n"
              + "    String units \"millibar\";\n"
              + "  \\}\n"
              + "  airTemperature \\{\n"
              + "    Float32 _FillValue -8888.0;\n"
              + "    Float32 actual_range [0-9.-]+, [0-9.]+;\n"
              + // before
              // 2018-09-15
              // was 48.07,
              // before
              // 2013-08-28
              // was 18.97
              "    String average_center \"time at end of period\";\n"
              + "    Int16 average_length 60;\n"
              + "    String average_method \"average\";\n"
              + "    Float32 centerline_offset [0-9.-]+;\n"
              + "    Float64 colorBarMaximum 40.0;\n"
              + "    Float64 colorBarMinimum -10.0;\n"
              + "    Float32 data_precision -9999.0;\n"
              + "    Float32 distance_from_bow [0-9.-]+;\n"
              + "    Float32 height [0-9.-]+;\n"
              + "    String instrument \"unknown\";\n"
              + "    String ioos_category \"Temperature\";\n"
              + "    String long_name \"Air Temperature\";\n"
              + "    Float32 missing_value -9999.0;\n"
              + "    String observation_type \"measured\";\n"
              + "    Int32 qcindex [0-9]+;\n"
              + // 2021-02-26 was 21, 2018-09-15
              // was 12
              "    Float32 sampling_rate [0-9.]+;\n"
              +
              // " String serial_number \"15365\";\n" +
              "    Float32 special_value -8888.0;\n"
              + "    String standard_name \"air_temperature\";\n"
              + "    String units \"degree_C\";\n"
              + "  \\}\n"
              + "  conductivity \\{\n"
              + "    Float32 _FillValue -8888.0;\n"
              + "    Float32 actual_range [0-9.]+, [0-9.e+]+;\n"
              + // 2013-03-26
              // new
              // value
              // is
              // nonsense.
              // was
              "    Float64 colorBarMaximum 4.0;\n"
              + "    Float64 colorBarMinimum 0.0;\n"
              + "    String ioos_category \"Salinity\";\n"
              + "    String long_name \"Conductivity\";\n"
              + "    Float32 missing_value -9999.0;\n"
              + "    String observation_type \"measured\";\n"
              + "    Int32 qcindex 16;\n"
              + (with
                  ?
                  // 4.78" String average_center \"time at end of period\";\n" +
                  "    Int16 average_length 60;\n"
                      + "    String average_method \"average\";\n"
                      + "    Float32 centerline_offset 0.0;\n"
                      + "    Float32 data_precision 0.01;\n"
                      + "    Float32 distance_from_bow 32.3;\n"
                      + "    Float32 height 3.7;\n"
                      + "    String instrument \"Sea-Bird SBE 45\";\n"
                      + "    Float32 intake_centerline_offset -9999.0;\n"
                      + "    String original_units \"siemens meter-1\";\n"
                      + "    Float32 sampling_rate [0-9.]+;\n"
                      + "    String serial_number \"157\";\n"
                      + "    Float32 special_value -8888.0;\n"
                  : "")
              + "    String standard_name \"sea_water_electrical_conductivity\";\n"
              + "    String units \"siemens meter-1\";\n"
              + "  \\}\n"
              + "  relativeHumidity \\{\n"
              + "    Float32 _FillValue -8888.0;\n"
              + "    Float32 actual_range [0-9.]+, [0-9.]+;\n"
              + // changes
              "    String average_center \"time at end of period\";\n"
              + "    Int16 average_length 60;\n"
              + "    String average_method \"average\";\n"
              + "    Float32 centerline_offset [0-9.-]+;\n"
              + "    Float64 colorBarMaximum 100.0;\n"
              + "    Float64 colorBarMinimum 0.0;\n"
              + "    Float32 data_precision [0-9.-]+;\n"
              + "    Float32 distance_from_bow [0-9.-]+;\n"
              + "    Float32 height [0-9.-]+;\n"
              + "    String instrument \"unknown\";\n"
              + "    String ioos_category \"Meteorology\";\n"
              + "    String long_name \"Relative Humidity\";\n"
              + "    Float32 missing_value -9999.0;\n"
              + "    String observation_type \"measured\";\n"
              + "    String original_units \"percent\";\n"
              + "    Int32 qcindex [0-9]+;\n"
              + // 2021-02-26 was 22, 2018-09-15
              // was 13
              "    Float32 sampling_rate [0-9.]+;\n"
              +
              // " String serial_number \"15365\";\n" +
              "    Float32 special_value -8888.0;\n"
              + "    String standard_name \"relative_humidity\";\n"
              + "    String units \"percent\";\n"
              + "  \\}\n"
              + "  salinity \\{\n"
              + "    Float32 _FillValue -8888.0;\n"
              + "    Float32 actual_range [0-9.]+, [0-9.]+;\n"
              + // 2013-03-26
              // nonsense!
              // was 9672.92
              "    Float64 colorBarMaximum 37.0;\n"
              + "    Float64 colorBarMinimum 32.0;\n"
              + (with
                  ? "    String average_center \"time at end of period\";\n"
                      + "    Int16 average_length 60;\n"
                      + "    String average_method \"average\";\n"
                      + "    Float32 centerline_offset 0.0;\n"
                      +
                      // " Int32 data_interval 60;\n" +
                      "    Float32 data_precision 0.01;\n"
                      + "    Float32 distance_from_bow 32.3;\n"
                      + "    Float32 height 3.7;\n"
                      + "    String instrument \"Sea-Bird SBE 45\";\n"
                  : "")
              + "    String ioos_category \"Salinity\";\n"
              + "    String long_name \"Sea Water Practical Salinity\";\n"
              + "    Float32 missing_value -9999.0;\n"
              + "    String observation_type \"calculated\";\n"
              + (with ? "    String original_units \"PSU\";\n" : "")
              + "    Int32 qcindex 15;\n"
              + (with
                  ? "    Float32 sampling_rate [0-9.]+;\n"
                      + "    String serial_number \"157\";\n"
                      + "    Float32 special_value -8888.0;\n"
                  : "")
              + "    String standard_name \"sea_water_practical_salinity\";\n"
              + (with ? "    String units \"PSU\";\n" : "")
              + "  \\}\n"
              + "  seaTemperature \\{\n"
              + "    Float32 _FillValue -8888.0;\n"
              + "    Float32 actual_range [0-9.-]+, [0-9.]+;\n"
              + // nonsense!
              "    String average_center \"time at end of period\";\n"
              + "    Int16 average_length 60;\n"
              + "    String average_method \"average\";\n"
              + "    Float32 centerline_offset [0-9.-]+;\n"
              + "    Float64 colorBarMaximum 40.0;\n"
              + "    Float64 colorBarMinimum -10.0;\n"
              + "    Float32 data_precision [0-9.-]+;\n"
              + "    Float32 distance_from_bow [0-9.-]+;\n"
              + "    Float32 height [0-9.-]+;\n"
              + "    String instrument \"unknown\";\n"
              + "    String ioos_category \"Temperature\";\n"
              + "    String long_name \"Sea Water Temperature\";\n"
              + "    Float32 missing_value -9999.0;\n"
              + "    String observation_type \"measured\";\n"
              + "    String original_units \"celsius\";\n"
              + "    Int32 qcindex 14;\n"
              + "    Float32 sampling_rate [0-9.]+;\n"
              +
              // " String serial_number \"157\";\n" +
              "    Float32 special_value -8888.0;\n"
              + "    String standard_name \"sea_water_temperature\";\n"
              + "    Int16 ts_sensor_category 12;\n"
              + "    String units \"degree_C\";\n"
              + "  \\}\n"
              + "  windDirection \\{\n"
              + "    Float32 _FillValue -8888.0;\n"
              + "    Float32 actual_range 0.0, 360.0;\n"
              + "    String average_center \"time at end of period\";\n"
              + "    Int16 average_length 60;\n"
              + "    String average_method \"average\";\n"
              + "    Float32 centerline_offset [0-9.-]+;\n"
              + "    Float64 colorBarMaximum 360.0;\n"
              + "    Float64 colorBarMinimum 0.0;\n"
              +
              // " Int32 data_interval -9999;\n" + //disappeared 2017-02-15
              "    Float32 data_precision [0-9.-]+;\n"
              + "    Float32 distance_from_bow [0-9.-]+;\n"
              + "    Float32 height [0-9.-]+;\n"
              + "    String instrument \"unknown\";\n"
              + "    String ioos_category \"Wind\";\n"
              + "    String long_name \"Earth Relative Wind Direction\";\n"
              + "    Float32 missing_value -9999.0;\n"
              + "    String observation_type \"calculated\";\n"
              + "    String original_units \"degrees \\(clockwise from true north\\)\";\n"
              + "    Int32 qcindex 6;\n"
              + "    Float32 sampling_rate [0-9.]+;\n"
              +
              // " String serial_number \"WM86188\";\n" +
              "    Float32 special_value -8888.0;\n"
              + "    String standard_name \"wind_from_direction\";\n"
              + "    String units \"degrees \\(clockwise from true north\\)\";\n"
              + "  \\}\n"
              + "  windSpeed \\{\n"
              + "    Float32 _FillValue -8888.0;\n"
              + "    Float32 actual_range [0-9.]+, [0-9.]+;\n"
              + // really?
              // 2020-05-05
              // to .2, was
              // 2850253.0.
              // 2020-10-05
              // is .0 again
              "    String average_center \"time at end of period\";\n"
              + "    Int16 average_length 60;\n"
              + "    String average_method \"average\";\n"
              + "    Float32 centerline_offset [0-9.-]+;\n"
              + "    Float64 colorBarMaximum 15.0;\n"
              + "    Float64 colorBarMinimum 0.0;\n"
              +
              // " Int32 data_interval -9999;\n" + //disappeared 2017-02-15
              "    Float32 data_precision [0-9.-]+;\n"
              + "    Float32 distance_from_bow [0-9.-]+;\n"
              + "    Float32 height [0-9.-]+;\n"
              + "    String instrument \"unknown\";\n"
              + "    String ioos_category \"Wind\";\n"
              + "    String long_name \"Earth Relative Wind Speed\";\n"
              + "    Float32 missing_value -9999.0;\n"
              + "    String observation_type \"calculated\";\n"
              + "    String original_units \"knot\";\n"
              + "    Int32 qcindex [0-9]+;\n"
              + // 2021-02-26 was 13, 2018-09-15
              // was 9
              "    Float32 sampling_rate [0-9.]+;\n"
              +
              // " String serial_number \"WM86188\";\n" +
              "    Float32 special_value -8888.0;\n"
              + "    String standard_name \"wind_speed\";\n"
              + "    String units \"meter second-1\";\n"
              + "  \\}\n"
              + "  platformCourse \\{\n"
              + "    Float32 _FillValue -8888.0;\n"
              + "    Float32 actual_range 0.0, 360.0;\n"
              + "    String average_center \"time at end of period\";\n"
              + "    Int16 average_length 60;\n"
              + "    String average_method \"average\";\n"
              + "    Float64 colorBarMaximum 360.0;\n"
              + "    Float64 colorBarMinimum 0.0;\n"
              + "    Float32 data_precision [0-9.-]+;\n"
              + "    String instrument \"unknown\";\n"
              + "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Platform Course\";\n"
              + "    Float32 missing_value -9999.0;\n"
              + "    String observation_type \"calculated\";\n"
              + // 2018-09-15
              // was
              // calculated
              "    String original_units \"degrees \\(clockwise towards true north\\)\";\n"
              + "    Int32 qcindex 5;\n"
              + "    Float32 sampling_rate [0-9.]+;\n"
              +
              // " String serial_number \"8928\";\n" +
              "    Float32 special_value -8888.0;\n"
              + "    String units \"degrees_true\";\n"
              + "  \\}\n"
              + "  platformHeading \\{\n"
              + "    Float32 _FillValue -8888.0;\n"
              + "    Float32 actual_range 0.0, 360.0;\n"
              + "    String average_center \"time at end of period\";\n"
              + "    Int16 average_length 60;\n"
              + "    String average_method \"average\";\n"
              + "    Float64 colorBarMaximum 360.0;\n"
              + "    Float64 colorBarMinimum 0.0;\n"
              + "    Float32 data_precision [0-9.-]+;\n"
              + "    String instrument \"unknown\";\n"
              + // 2018-09-15
              // was
              // Applanix
              // POSMV
              // V4
              "    String ioos_category \"Unknown\";\n"
              + "    String long_name \"Platform Heading\";\n"
              + "    Float32 missing_value -9999.0;\n"
              + "    String observation_type \"calculated\";\n"
              + // 2018-09-15
              // was
              // calculated
              "    String original_units \"degrees \\(clockwise towards true north\\)\";\n"
              + "    Int32 qcindex 4;\n"
              + "    Float32 sampling_rate [0-9.]+;\n"
              +
              // " String serial_number \"QA0799, QA0701\";\n" +
              "    Float32 special_value -8888.0;\n"
              + "    String units \"degrees_true\";\n"
              + "  \\}\n"
              + "  platformSpeed \\{\n"
              + "    Float32 _FillValue -8888.0;\n"
              + "    Float32 actual_range 0.0, 2850255.0;\n"
              + // really?
              "    String average_center \"time at end of period\";\n"
              + "    Int16 average_length 60;\n"
              + "    String average_method \"average\";\n"
              + "    Float64 colorBarMaximum 10.0;\n"
              + "    Float64 colorBarMinimum 0.0;\n"
              + "    Float32 data_precision [0-9.-]+;\n"
              + "    String instrument \"unknown\";\n"
              + "    String ioos_category \"Other\";\n"
              + "    String long_name \"Platform Speed Over Ground\";\n"
              + "    Float32 missing_value -9999.0;\n"
              + "    String observation_type \"calculated\";\n"
              + // 2018-09-15
              // was
              // calculated
              "    String original_units \"knot\";\n"
              + "    Int32 qcindex [0-9]+;\n"
              + // 2018-09-15 was 8
              "    Float32 sampling_rate [0-9.]+;\n"
              + // 2018-09-15 was 0.5
              // " String serial_number \"8928\";\n" +
              "    Float32 special_value -8888.0;\n"
              + "    String units \"meter second-1\";\n"
              + "  \\}\n"
              + "  platformWindDirection \\{\n"
              + "    Float32 _FillValue -8888.0;\n"
              + "    Float32 actual_range 0.0, 360.0;\n"
              + "    String average_center \"time at end of period\";\n"
              + "    Int16 average_length 60;\n"
              + "    String average_method \"average\";\n"
              + "    Float32 centerline_offset [0-9.-]+;\n"
              + "    Float64 colorBarMaximum 360.0;\n"
              + "    Float64 colorBarMinimum 0.0;\n"
              +
              // " Int32 data_interval -9999;\n" + //disappeared 2017-02-15
              "    Float32 data_precision [0-9.-]+;\n"
              + "    Float32 distance_from_bow [0-9.-]+;\n"
              + "    Float32 height [0-9.-]+;\n"
              + "    String instrument \"unknown\";\n"
              + "    String ioos_category \"Wind\";\n"
              + "    String long_name \"Platform Relative Wind Direction\";\n"
              + "    Float32 missing_value -9999.0;\n"
              + "    String observation_type \"measured\";\n"
              + "    String original_units \"degrees \\(clockwise from bow\\)\";\n"
              + "    Int32 qcindex [0-9]+;\n"
              + // 2018-09-15 was 7
              "    Float32 sampling_rate [0-9.]+;\n"
              +
              // " String serial_number \"WM86188\";\n" +
              "    Float32 special_value -8888.0;\n"
              + "    String standard_name \"wind_from_direction\";\n"
              + "    String units \"degrees \\(clockwise from bow\\)\";\n"
              + "    Float32 zero_line_reference [0-9.-]+;\n"
              + // 2018-09-15 was
              // -9999.0
              "  \\}\n"
              + "  platformWindSpeed \\{\n"
              + "    Float32 _FillValue -8888.0;\n"
              + "    Float32 actual_range [0-9.]+, [0-9.]+;\n"
              + // before
              // 2013-08-28
              // was 36.09545
              "    String average_center \"time at end of period\";\n"
              + "    Int16 average_length 60;\n"
              + "    String average_method \"average\";\n"
              + "    Float32 centerline_offset [0-9.-]+;\n"
              + "    Float64 colorBarMaximum 15.0;\n"
              + "    Float64 colorBarMinimum 0.0;\n"
              +
              // " Int32 data_interval -9999;\n" + //disappeared 2017-02-15
              "    Float32 data_precision [0-9.-]+;\n"
              + "    Float32 distance_from_bow [0-9.-]+;\n"
              + "    Float32 height [0-9.-]+;\n"
              + "    String instrument \"unknown\";\n"
              + "    String ioos_category \"Wind\";\n"
              + "    String long_name \"Platform Relative Wind Speed\";\n"
              + "    Float32 missing_value -9999.0;\n"
              + "    String observation_type \"measured\";\n"
              + "    String original_units \"knot\";\n"
              + "    Int32 qcindex [0-9]+;\n"
              + // 2021-02-26 was 16, 2018-09-15
              // was 10
              "    Float32 sampling_rate [0-9.]+;\n"
              +
              // " String serial_number \"WM86188\";\n" +
              "    Float32 special_value -8888.0;\n"
              + "    String standard_name \"wind_speed\";\n"
              + "    String units \"meter second-1\";\n"
              + "  \\}\n"
              + "  flag \\{\n"
              + "    String A \"Units added\";\n"
              + "    String B \"Data out of range\";\n"
              + "    String C \"Non-sequential time\";\n"
              + "    String D \"Failed T>=Tw>=Td\";\n"
              + "    String DODS_dimName \"f_string\";\n"
              + "    Int32 DODS_strlen \\d\\d;\n"
              + // changes: 13, 16
              "    String E \"True wind error\";\n"
              + "    String F \"Velocity unrealistic\";\n"
              + "    String G \"Value > 4 s. d. from climatology\";\n"
              + "    String H \"Discontinuity\";\n"
              + "    String I \"Interesting feature\";\n"
              + "    String ioos_category \"Quality\";\n"
              + "    String J \"Erroneous\";\n"
              + "    String K \"Suspect - visual\";\n"
              + "    String L \"Ocean platform over land\";\n"
              + "    String long_name \"Quality Control Flags\";\n"
              + "    String M \"Instrument malfunction\";\n"
              +
              // " String N \"In Port\";\n" +
              "    String O \"Multiple original units\";\n"
              + "    String P \"Movement uncertain\";\n"
              + "    String Q \"Pre-flagged as suspect\";\n"
              + "    String R \"Interpolated data\";\n"
              + "    String S \"Spike - visual\";\n"
              + "    String T \"Time duplicate\";\n"
              + "    String U \"Suspect - statistial\";\n"
              + "    String V \"Spike - statistical\";\n"
              + "    String X \"Step - statistical\";\n"
              + "    String Y \"Suspect between X-flags\";\n"
              + "    String Z \"Good data\";\n"
              + "  \\}\n"
              + " \\}\n"
              + "  NC_GLOBAL \\{\n"
              + "    String cdm_data_type \"Trajectory\";\n"
              + "    String cdm_trajectory_variables \"ID, site\";\n"
              +
              // " String commit_hash
              // \"7524017926524418e6907515721436930f2eb50b\";\n" +
              // //disappeared 2020-09-17
              "    String contact_email \"samos@coaps.fsu.edu\";\n"
              + "    String contact_info \"Center for Ocean-Atmospheric Prediction Studies, The Florida State University, Tallahassee, FL, 32306-2840, USA\";\n"
              + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
              + "    String creator_email \"samos@coaps.fsu.edu\";\n"
              + "    String creator_name \"Shipboard Automated Meteorological and Oceanographic System \\(SAMOS\\)\";\n"
              + "    String creator_type \"group\";\n"
              + "    String creator_url \"https://samos.coaps.fsu.edu/html/\";\n"
              + "    String Data_modification_date \".{19} E.T\";\n"
              + // changes
              "    String data_provider \"[a-zA-Z ]+\";\n"
              + "    String defaultDataQuery \"&time>=max\\(time\\)-7days&time<=max\\(time\\)&flag=~\\\\\"ZZZ\\.\\*\\\\\"\";\n"
              + "    String defaultGraphQuery \"&time>=max\\(time\\)-7days&time<=max\\(time\\)&flag=~\\\\\"ZZZ\\.\\*\\\\\"&\\.marker=10\\|5\";\n"
              + "    Float64 Easternmost_Easting [0-9.]+;\n"
              + "    Int16 elev 0;\n"
              + "    String featureType \"Trajectory\";\n"
              +
              // " String files_merged \"\\[WTEP_202.....v10001.nc(|,
              // WTEP_202.....v10002.nc(|, WTEP_202.....v10003.nc))\\]\";\n" +
              // changes, so neutered
              "    String fsu_version \"...\";\n"
              + // changes 300 to 301 to
              // 300
              "    Float64 geospatial_lat_max [0-9.]+;\n"
              + "    Float64 geospatial_lat_min [0-9.-]+;\n"
              + "    String geospatial_lat_units \"degrees_north\";\n"
              + "    Float64 geospatial_lon_max [0-9.]+;\n"
              + "    Float64 geospatial_lon_min [0-9.]+;\n"
              + "    String geospatial_lon_units \"degrees_east\";\n"
              + "    String history \""
              + today;
      String seek = "String history \"" + today;
      int tPo = results.indexOf(seek);
      Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
      Test.ensureLinesMatch(
          results.substring(0, tPo + seek.length()), expected, "\nresults=\n" + results);

      // + "
      // https://tds.coaps.fsu.edu/thredds/catalog/samos/data/research/WTEP/catalog.xml\n"
      // +
      // today + " http://localhost:8080/cwexperimental/tabledap/
      expected =
          "fsuNoaaShipWTEP.das\";\n"
              + "    String infoUrl \"https://samos.coaps.fsu.edu/html/\";\n"
              + "    String institution \"FSU\";\n"
              + "    String keywords \"air, air_pressure, air_temperature, atmosphere, atmospheric, calender, conductivity, control, course, data, date, day, density, direction, dyson, earth, Earth Science > Atmosphere > Atmospheric Pressure > Atmospheric Pressure Measurements, Earth Science > Atmosphere > Atmospheric Pressure > Sea Level Pressure, Earth Science > Atmosphere > Atmospheric Pressure > Static Pressure, Earth Science > Atmosphere > Atmospheric Temperature > Air Temperature, Earth Science > Atmosphere > Atmospheric Temperature > Surface Air Temperature, Earth Science > Atmosphere > Atmospheric Water Vapor > Humidity, Earth Science > Atmosphere > Atmospheric Winds > Surface Winds, Earth Science > Oceans > Salinity/Density > Conductivity, Earth Science > Oceans > Salinity/Density > Salinity, electrical, file, flags, from, fsu, ground, heading, history, humidity, information, level, measurements, meteorological, meteorology, oceans, oscar, over, platform, pressure, quality, relative, relative_humidity, salinity, sea, sea_water_electrical_conductivity, sea_water_practical_salinity, seawater, speed, static, surface, temperature, time, vapor, water, wind, wind_from_direction, wind_speed, winds\";\n"
              + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
              + "    String license \"The data may be used and redistributed for free but is not intended\n"
              + "for legal use, since it may contain inaccuracies. Neither the data\n"
              + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
              + "of their employees or contractors, makes any warranty, express or\n"
              + "implied, including warranties of merchantability and fitness for a\n"
              + "particular purpose, or assumes any legal liability for the accuracy,\n"
              + "completeness, or usefulness, of this information.\";\n"
              +
              // " String merger_version \"v001\";\n" +
              "    String Metadata_modification_date \".{19} E.T\";\n"
              + // changes
              // " String metadata_retrieved_from \"WTEP_202.....v1000.\\.nc\";\n" + //
              // changes
              "    String naming_authority \"gov.noaa.pfeg.coastwatch\";\n"
              + "    Float64 Northernmost_Northing [0-9.]+;\n"
              + // changes
              "    String receipt_order \"01\";\n"
              + "    String sourceUrl \"https://tds.coaps.fsu.edu/thredds/catalog/samos/data/research/WTEP/catalog.xml\";\n"
              + "    Float64 Southernmost_Northing -46.45;\n"
              + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
              + "    String subsetVariables \"ID, site, IMO, cruise_id, expocode, facility, platform, platform_version\";\n"
              + "    String summary \"NOAA Ship Oscar Dyson Underway Meteorological Data "
              + "\\(delayed ~10 days for quality control\\) are from the Shipboard "
              + "Automated Meteorological and Oceanographic System \\(SAMOS\\) program.\n"
              + "\n"
              + "IMPORTANT: ALWAYS USE THE QUALITY FLAG DATA! Each data variable's metadata "
              + "includes a qcindex attribute which indicates a character number in the "
              + "flag data.  ALWAYS check the flag data for each row of data to see which "
              + "data is good \\(flag='Z'\\) and which data isn't.  For example, to extract "
              + "just data where time \\(qcindex=1\\), latitude \\(qcindex=2\\), longitude "
              + "\\(qcindex=3\\), and airTemperature \\(qcindex=12\\) are 'good' data, "
              + "include this constraint in your ERDDAP query:\n"
              + "  flag=~\\\\\"ZZZ........Z.*\\\\\"\n"
              + "in your query.\n"
              + "'=~' indicates this is a regular expression constraint.\n"
              + "The 'Z's are literal characters.  In this dataset, 'Z' indicates 'good' data.\n"
              + "The '\\.'s say to match any character.\n"
              + "The '\\*' says to match the previous character 0 or more times.\n"
              + "\\(Don't include backslashes in your query.\\)\n"
              + "See the tutorial for regular expressions at\n"
              + "https://www.vogella.com/tutorials/JavaRegularExpressions/article.html\";\n"
              + "    String time_coverage_end \"20.{8}T.{8}Z\";\n"
              + // changes
              "    String time_coverage_start \"2007-09-01T00:00:00Z\";\n"
              + "    String title \"NOAA Ship Oscar Dyson Underway Meteorological Data, Quality Controlled\";\n"
              + "    Float64 Westernmost_Easting [0-9.]+;\n"
              + "  \\}\n"
              + "\\}\n";
      tPo = results.indexOf(expected.substring(0, 17));
      Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
      Test.ensureLinesMatch(results.substring(tPo), expected, "results=\n" + results);
    } catch (Throwable t) {
      Test.knownProblem("This often has small metadata changes.", t);
    }

    // *** test getting dds for entire dataset
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_ShipEntire",
            ".dds");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    String ID;\n"
            + "    String site;\n"
            + "    String IMO;\n"
            + "    String cruise_id;\n"
            + "    String expocode;\n"
            + "    String facility;\n"
            + "    String platform;\n"
            + "    String platform_version;\n"
            + "    Float64 time;\n"
            + "    Float32 latitude;\n"
            + "    Float32 longitude;\n"
            + "    Float32 airPressure;\n"
            + "    Float32 airTemperature;\n"
            + "    Float32 conductivity;\n"
            + "    Float32 relativeHumidity;\n"
            + "    Float32 salinity;\n"
            + "    Float32 seaTemperature;\n"
            + "    Float32 windDirection;\n"
            + "    Float32 windSpeed;\n"
            + "    Float32 platformCourse;\n"
            + "    Float32 platformHeading;\n"
            + "    Float32 platformSpeed;\n"
            + "    Float32 platformWindDirection;\n"
            + "    Float32 platformWindSpeed;\n"
            + "    String flag;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // *** test make data files
    String2.log("\n****************** EDDTableFromThreddsFiles.testShipWTEP make DATA FILES\n");

    // .csv for one lat,lon,time
    userDapQuery = "cruise_id&distinct()";
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_ShipCruiseList",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected = "cruise_id\n" + "\n" + "Cruise_id undefined for now\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .csv
    userDapQuery =
        "time,latitude,longitude,airPressure,airTemperature,flag&time%3E=2012-01-29T19:30:00Z&time%3C=2012-01-29T19:34:00Z";
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_Ship1StationGTLT",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "time,latitude,longitude,airPressure,airTemperature,flag\n"
            + "UTC,degrees_north,degrees_east,millibar,degree_C,\n"
            + "2012-01-29T19:30:00Z,48.47,235.11,1009.1,9.01,ZZZZZZZZZZZZBZZZ\n"
            + "2012-01-29T19:31:00Z,48.47,235.11,1009.05,8.94,ZZZZZZZZZZZZBZZZ\n"
            + "2012-01-29T19:32:00Z,48.47,235.12,1009.03,8.9,ZZZZZZZZZZZZBZZZ\n"
            + "2012-01-29T19:33:00Z,48.47,235.12,1009.03,8.93,ZZZZZZZZZZZZBZZZ\n"
            + "2012-01-29T19:34:00Z,48.47,235.13,1009.02,8.96,ZZZZZZZZZZZZBZZZ\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
  }

  /**
   * This tests getThreddsFileInfo.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagThredds
  void testGetThreddsFileInfo() throws Throwable {
    String2.log("\n*** EDDTableFromThredds.testGetThreddsFileInfo");
    String results, expected;

    StringArray fileDir = new StringArray();
    StringArray fileName = new StringArray();
    LongArray fileLastMod = new LongArray();

    // *** test
    // https://tds.coaps.fsu.edu/thredds/catalog/samos/data/quick/WTEP/2011/catalog.html
    if (true) {
      EDDTableFromThreddsFiles.getThreddsFileInfo(
          "https://tds.coaps.fsu.edu/thredds/catalog/samos/data/quick/WTEP/catalog.xml",
          "WTEP_2011082.*\\.nc",
          true,
          "", // recursive, pathRegex
          fileDir,
          fileName,
          fileLastMod);

      // 2016-05-11 sort order was different
      // I think it doesn't matter for this class, but does for testing,
      // so sort results.
      Table table = new Table();
      table.addColumn("dir", fileDir);
      table.addColumn("name", fileName);
      table.addColumn("lastMod", fileLastMod);
      table.leftToRightSort(3);

      results = fileDir.toNewlineString();
      expected =
          "https://tds.coaps.fsu.edu/thredds/dodsC/samos/data/quick/WTEP/2011/\n"
              + "https://tds.coaps.fsu.edu/thredds/dodsC/samos/data/quick/WTEP/2011/\n"
              + "https://tds.coaps.fsu.edu/thredds/dodsC/samos/data/quick/WTEP/2011/\n"
              + "https://tds.coaps.fsu.edu/thredds/dodsC/samos/data/quick/WTEP/2011/\n"
              + "https://tds.coaps.fsu.edu/thredds/dodsC/samos/data/quick/WTEP/2011/\n"
              + "https://tds.coaps.fsu.edu/thredds/dodsC/samos/data/quick/WTEP/2011/\n"
              + "https://tds.coaps.fsu.edu/thredds/dodsC/samos/data/quick/WTEP/2011/\n"
              + "https://tds.coaps.fsu.edu/thredds/dodsC/samos/data/quick/WTEP/2011/\n"
              + "https://tds.coaps.fsu.edu/thredds/dodsC/samos/data/quick/WTEP/2011/\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      results = fileName.toNewlineString();
      expected =
          "WTEP_20110821v10001.nc\n"
              + "WTEP_20110822v10001.nc\n"
              + "WTEP_20110823v10001.nc\n"
              + "WTEP_20110824v10001.nc\n"
              + "WTEP_20110825v10001.nc\n"
              + "WTEP_20110826v10001.nc\n"
              + "WTEP_20110827v10001.nc\n"
              + "WTEP_20110828v10001.nc\n"
              + "WTEP_20110829v10001.nc\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < fileLastMod.size(); i++)
        sb.append(Calendar2.millisToIsoStringTZ(fileLastMod.get(i)) + "\n");
      results = sb.toString();
      expected =
          "2011-08-22T00:02:00Z\n"
              + "2011-08-23T00:03:13Z\n"
              + "2011-08-24T00:04:06Z\n"
              + "2011-08-25T00:02:02Z\n"
              + "2011-08-26T00:03:13Z\n"
              + "2011-08-27T00:04:46Z\n"
              + "2011-08-28T00:05:25Z\n"
              + "2011-08-29T00:03:35Z\n"
              + "2011-08-30T00:05:39Z\n";
      Test.ensureEqual(results, expected, "results=\n" + results);
    }

    // *** test https://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/SMS/catalog.xml
    if (true) {
      fileDir.clear();
      fileName.clear();
      fileLastMod.clear();
      EDDTableFromThreddsFiles.getThreddsFileInfo(
          "https://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/SMS/catalog.xml",
          "SMS.*_2004.*\\.nc",
          true,
          "", // recursive, pathRegex
          fileDir,
          fileName,
          fileLastMod);

      results = fileDir.toNewlineString();
      expected =
          "https://data.nodc.noaa.gov/thredds/dodsC/nmsp/wcos/SMS/2004/\n"
              + "https://data.nodc.noaa.gov/thredds/dodsC/nmsp/wcos/SMS/2004/\n"
              + "https://data.nodc.noaa.gov/thredds/dodsC/nmsp/wcos/SMS/2004/\n"
              + "https://data.nodc.noaa.gov/thredds/dodsC/nmsp/wcos/SMS/2004/\n"
              + "https://data.nodc.noaa.gov/thredds/dodsC/nmsp/wcos/SMS/2004/\n"
              + "https://data.nodc.noaa.gov/thredds/dodsC/nmsp/wcos/SMS/2004/\n"
              + "https://data.nodc.noaa.gov/thredds/dodsC/nmsp/wcos/SMS/2004/\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      results = fileName.toNewlineString();
      expected =
          "SMSXXX_015ADCP015R00_20041130.nc\n"
              + "SMSXXX_015MTBD003R00_20041011.nc\n"
              + "SMSXXX_015MTBD003R00_20041122.nc\n"
              + "SMSXXX_015MTBD009R00_20041011.nc\n"
              + "SMSXXX_015MTBD009R00_20041122.nc\n"
              + "SMSXXX_015MTBD014R00_20041011.nc\n"
              + "SMSXXX_015MTBD014R00_20041122.nc\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < fileLastMod.size(); i++)
        sb.append(Calendar2.millisToIsoStringTZ(fileLastMod.get(i)) + "\n");
      results = sb.toString();
      expected =
          // "2012-05-18T05:42:56Z\n" +
          // "2012-05-11T01:03:50Z\n" +
          // "2012-05-12T14:37:14Z\n" +
          // "2012-05-14T12:55:06Z\n" +
          // "2012-05-10T07:25:54Z\n" +
          // "2012-05-11T17:41:56Z\n" +
          // "2012-05-14T20:46:04Z\n";
          // ! changed in odd way 2017-12-15 ! now modified time is older! See
          // https://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/SMS/2004/catalog.xml
          "2009-09-28T06:45:08Z\n"
              + "2010-01-05T09:16:32Z\n"
              + "2010-01-07T21:55:36Z\n"
              + "2010-01-09T22:13:44Z\n"
              + "2010-01-10T21:16:10Z\n"
              + "2010-01-10T21:35:00Z\n"
              + "2009-10-02T05:01:10Z\n";
      Test.ensureEqual(results, expected, "results=\n" + results);
    }

    String2.log("\n*** EDDTableFromThredds.testGetThreddsFileInfo finished.");
  }

  /**
   * testGenerateDatasetsXml. This doesn't test suggestTestOutOfDate, except that for old data it
   * doesn't suggest anything.
   */
  @org.junit.jupiter.api.Test
  @TagThredds
  void testGenerateDatasetsXml() throws Throwable {
    // testVerboseOn();
    String results =
        EDDTableFromThreddsFiles.generateDatasetsXml(
                // I could do wcos/catalog.xml but very slow because lots of files
                "https://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/WES001/2008/catalog.xml",
                ".*MTBD.*\\.nc", // ADCP files have different vars and diff metadata, e.g.,
                // _FillValue
                "https://data.nodc.noaa.gov/thredds/dodsC/nmsp/wcos/WES001/2008/WES001_030MTBD029R00_20080613.nc",
                1440,
                "",
                "_.*$",
                ".*",
                "stationID",
                "Time",
                "stationID Time",
                -1, // defaultStandardizeWhat
                null)
            + "\n"; // externalAddGlobalAttributes

    // GenerateDatasetsXml
    String gdxResults =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {
                  "-verbose",
                  "EDDTableFromThreddsFiles",
                  "https://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/WES001/2008/catalog.xml",
                  ".*MTBD.*\\.nc",
                  "https://data.nodc.noaa.gov/thredds/dodsC/nmsp/wcos/WES001/2008/WES001_030MTBD029R00_20080613.nc",
                  "1440",
                  "",
                  "_.*$",
                  ".*",
                  "stationID",
                  "Time",
                  "stationID Time",
                  "-1"
                }, // defaultStandardizeWhat
                false); // doIt loop?
    Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

    String tDatasetID =
        EDDTableFromNcFiles.suggestDatasetID(
            "https://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/WES001/2008/catalog.xml"
                + ".*MTBD.*\\.nc");
    String expected =
        "<dataset type=\"EDDTableFromThreddsFiles\" datasetID=\""
            + tDatasetID
            + "\" active=\"true\">\n"
            + "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n"
            + "    <updateEveryNMillis>0</updateEveryNMillis>\n"
            + "    <fileDir></fileDir>  <!-- automatically set to [bigParentDirectory]/copy/noaa_nodc_d91a_eb5f_b55f/ -->\n"
            + "    <fileNameRegex>.*MTBD.*\\.nc</fileNameRegex>\n"
            + "    <recursive>true</recursive>\n"
            + "    <pathRegex>.*</pathRegex>\n"
            + "    <metadataFrom>last</metadataFrom>\n"
            + "    <standardizeWhat>0</standardizeWhat>\n"
            + "    <preExtractRegex></preExtractRegex>\n"
            + "    <postExtractRegex>_.*$</postExtractRegex>\n"
            + "    <extractRegex>.*</extractRegex>\n"
            + "    <columnNameForExtract>stationID</columnNameForExtract>\n"
            + "    <sortedColumnSourceName>Time</sortedColumnSourceName>\n"
            + "    <sortFilesBySourceNames>stationID Time</sortFilesBySourceNames>\n"
            + "    <fileTableInMemory>false</fileTableInMemory>\n"
            + "    <!-- sourceAttributes>\n"
            + "        <att name=\"Conventions\">CF-1.4</att>\n"
            + // dates below change
            // !2017-12-15 older date! was 2012/31/11 20:31 CST, now 2010/00/10 03:00 CST
            "        <att name=\"History\">created by the NCDDC PISCO Temperature Profile to NetCDF converter on 2010/00/10 03:00 CST. Original dataset URL:</att>\n"
            + "        <att name=\"Mooring_ID\">WES001</att>\n"
            + "        <att name=\"Version\">2</att>\n"
            + "    </sourceAttributes -->\n"
            + "    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n"
            + "        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n"
            + "        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n"
            + "    -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cdm_data_type\">Point</att>\n"
            + "        <att name=\"Conventions\">CF-1.10, COARDS, ACDD-1.3</att>\n"
            + "        <att name=\"creator_email\">NODC.Webmaster@noaa.gov</att>\n"
            + "        <att name=\"creator_name\">NOAA NODC</att>\n"
            + "        <att name=\"creator_type\">institution</att>\n"
            + "        <att name=\"creator_url\">https://www.nodc.noaa.gov/</att>\n"
            + "        <att name=\"History\">null</att>\n"
            + // date below changes
            "        <att name=\"history\">created by the NCDDC PISCO Temperature Profile to NetCDF converter on 2010/00/10 03:00 CST. Original dataset URL:</att>\n"
            + "        <att name=\"infoUrl\">https://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/WES001/2008/catalog.html</att>\n"
            + "        <att name=\"institution\">NOAA NODC</att>\n"
            + "        <att name=\"keywords\">center, data, data.nodc.noaa.gov, day, depth, earth, Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Water Temperature, flag, identifier, latitude, longitude, national, ncei, nesdis, nmsp, noaa, nodc, ocean, oceanographic, oceans, quality, science, sea, sea_water_temperature, sea_water_temperature status_flag, seawater, station, stationID, status, temperature, Temperature_flag, time, water, wcos, wes001, year, yearday, yearday_flag</att>\n"
            + "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n"
            + "        <att name=\"license\">[standard]</att>\n"
            + "        <att name=\"sourceUrl\">https://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/WES001/2008/catalog.xml</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"summary\">NOAA National Oceanographic Data Center (NODC) data from https://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/WES001/2008/catalog.html</att>\n"
            + "        <att name=\"title\">NOAA NODC data from https://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/WES001/2008/catalog.html</att>\n"
            + "        <att name=\"Version\">null</att>\n"
            + "        <att name=\"version\">2</att>\n"
            + "    </addAttributes>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>stationID</sourceName>\n"
            + "        <destinationName>stationID</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "            <att name=\"long_name\">Station ID</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Time</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"int\">-9999</att>\n"
            + "            <att name=\"description\">Greenwich Mean Time of each temperature measurement record,in seconds since 1970-01-01 00:00:00.000 0:00</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"units\">seconds since 1970-01-01 00:00:00.000 0:00</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"double\">-9999.0</att>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"long_name\">Time</att>\n"
            + "            <att name=\"units\">seconds since 1970-01-01T00:00:00.000Z</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Depth</sourceName>\n"
            + "        <destinationName>depth</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"int\">-9999</att>\n"
            + "            <att name=\"description\">Data logger measurement depth (expressed as negative altitudes), referenced to Mean Sea Level (MSL)</att>\n"
            + "            <att name=\"long_name\">depth expressed as negative altitudes</att>\n"
            + "            <att name=\"standard_name\">depth</att>\n"
            + "            <att name=\"units\">meter</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"double\">-9999.0</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">8000.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-8000.0</att>\n"
            + "            <att name=\"colorBarPalette\">TopographyDepth</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"units\">m</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Latitude</sourceName>\n"
            + "        <destinationName>latitude</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">latitude</att>\n"
            + "            <att name=\"standard_name\">latitude</att>\n"
            + "            <att name=\"units\">degree_north</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Latitude</att>\n"
            + "            <att name=\"units\">degrees_north</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Longitude</sourceName>\n"
            + "        <destinationName>longitude</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">longitude</att>\n"
            + "            <att name=\"standard_name\">longitude</att>\n"
            + "            <att name=\"units\">degree_east</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Longitude</att>\n"
            + "            <att name=\"units\">degrees_east</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>yearday</sourceName>\n"
            + "        <destinationName>yearday</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">Day of the year</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Temperature_flag</sourceName>\n"
            + "        <destinationName>Temperature_flag</destinationName>\n"
            + "        <dataType>byte</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"description\">flag for data column, 0: no problems, 1: bad data due to malfunction or fouling, 2: suspicious data, 9: missing data</att>\n"
            + "            <att name=\"long_name\">Temperature flag</att>\n"
            + "            <att name=\"standard_name\">sea_water_temperature status_flag</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"byte\">127</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Temperature</sourceName>\n"
            + "        <destinationName>Temperature</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"int\">9999</att>\n"
            + "            <att name=\"description\">Seawater temperature</att>\n"
            + "            <att name=\"long_name\">Sea Water Temperature</att>\n"
            + "            <att name=\"quantity\">Temperature</att>\n"
            + "            <att name=\"standard_name\">sea_water_temperature</att>\n"
            + "            <att name=\"units\">Celsius</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"double\">9999.0</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Temperature</att>\n"
            + "            <att name=\"units\">degree_C</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>yearday_flag</sourceName>\n"
            + "        <destinationName>yearday_flag</destinationName>\n"
            + "        <dataType>byte</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"description\">flag for data column, 0: no problems, 1: bad data due to malfunction or fouling, 2: suspicious data, 9: missing data</att>\n"
            + "            <att name=\"long_name\">Yearday flag</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"byte\">127</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    // Test.ensureEqual(results.substring(0, Math.min(results.length(),
    // expected.length())),
    // expected, "");

    /*
     * This won't work because sample file is in testCacheDir (not regular cache
     * dir)
     * //ensure it is ready-to-use by making a dataset from it
     * String tDatasetID = "noaa_nodc_8fcf_be37_cbe4";
     * EDD.deleteCachedDatasetInfo(tDatasetID);
     * EDD edd = oneFromXmlFragment(null, results);
     * Test.ensureEqual(edd.datasetID(), tDatasetID, "");
     * Test.ensureEqual(edd.title(), "WES001 2008", "");
     * Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()),
     * "stationID, yearday, latitude, time, Depth, longitude, Temperature_flag, " +
     * "Temperature, yearday_flag", "");
     */

  }

  /**
   * This tests the methods in this class. 2020-10-21 I stopped running this test because thredds
   * randomly stalls when returning catalog.xml pages.
   *
   * @throws Throwable if trouble
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @TagMissingDataset
  void testWcosTemp(boolean deleteCachedInfo) throws Throwable {
    // String2.log("\n****************** EDDTableFromThreddsFiles.testWcosTemp()
    // *****************\n");
    // testVerboseOn();
    int language = 0;
    String name, tName, results, tResults, expected, userDapQuery, tQuery;
    String error = "";
    int po;
    EDV edv;

    String today =
        Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); // 14 is enough to check
    // hour. Hard
    // to check min:sec.

    String id = "nmspWcosTemp";
    if (deleteCachedInfo) EDDTableFromThreddsFiles.deleteCachedDatasetInfo(id);

    EDDTable eddTable = (EDDTable) EDDTableFromThreddsFiles.oneFromDatasetsXml(null, id);

    // *** test getting das for entire dataset
    String2.log(
        "\n****************** EDDTableFromThreddsFiles testWcosTemp das and dds for entire dataset\n");
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
        "Attributes {\n"
            + " s {\n"
            + "  station {\n"
            + "    String cf_role \"timeseries_id\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Station\";\n"
            + "  }\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range -124.932, -119.66934;\n"
            + "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float64 actual_range 33.89511, 48.325001;\n"
            + "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range 1.0971834e+9, 1.29749592e+9;\n"
            + // changes sometimes see
            // time_coverage_end
            // below
            "    String axis \"T\";\n"
            + "    String cf_role \"profile_id\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  depth {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    String _CoordinateZisPositive \"down\";\n"
            + "    Float64 _FillValue -9999.0;\n"
            + "    Float64 actual_range 0.0, 99.0;\n"
            + "    String axis \"Z\";\n"
            + "    String description \"Relative to Mean Sea Level (MSL)\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Depth\";\n"
            + "    String positive \"down\";\n"
            + "    String standard_name \"depth\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  Temperature {\n"
            + "    Float64 _FillValue 9999.0;\n"
            + "    Float64 actual_range 6.68, 38.25;\n"
            + "    Float64 colorBarMaximum 32.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Temperature\";\n"
            + "    String long_name \"Sea Water Temperature\";\n"
            + "    String quantity \"Temperature\";\n"
            + "    String standard_name \"sea_water_temperature\";\n"
            + "    String units \"degree_C\";\n"
            + "  }\n"
            + "  Temperature_flag {\n"
            + "    Byte _FillValue 127;\n"
            + "    String _Unsigned \"false\";\n"
            + // ERDDAP adds
            "    Byte actual_range 0, 9;\n"
            + "    String description \"flag for data column, 0: no problems, 1: bad data due to malfunction or fouling, 2: suspicious data, 9: missing data\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Temperature Flag\";\n"
            + "    String standard_name \"sea_water_temperature status_flag\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_data_type \"TimeSeriesProfile\";\n"
            + "    String cdm_profile_variables \"time\";\n"
            + "    String cdm_timeseries_variables \"station, longitude, latitude\";\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "    Float64 Easternmost_Easting -119.66934;\n"
            + "    String featureType \"TimeSeriesProfile\";\n"
            + "    Float64 geospatial_lat_max 48.325001;\n"
            + "    Float64 geospatial_lat_min 33.89511;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max -119.66934;\n"
            + "    Float64 geospatial_lon_min -124.932;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    Float64 geospatial_vertical_max 99.0;\n"
            + "    Float64 geospatial_vertical_min 0.0;\n"
            + "    String geospatial_vertical_positive \"down\";\n"
            + "    String geospatial_vertical_units \"m\";\n"
            + "    String history \"Created by the NCDDC PISCO Temperature Profile to NetCDF converter.\n"
            + today;
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

    // + " https://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/catalog.xml\n" +
    // today + " http://localhost:8080/cwexperimental/tabledap/
    expected =
        "nmspWcosTemp.das\";\n"
            + "    String infoUrl \"ftp://ftp.nodc.noaa.gov/nodc/archive/arc0006/0002039/1.1/about/WCOS_project_document_phaseI_20060317.pdf\";\n"
            + "    String institution \"NOAA NMSP\";\n"
            + "    String keywords \"Earth Science > Oceans > Ocean Temperature > Water Temperature\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    Float64 Northernmost_Northing 48.325001;\n"
            + "    String sourceUrl \"https://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/catalog.xml\";\n"
            + "    Float64 Southernmost_Northing 33.89511;\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String subsetVariables \"station, longitude, latitude\";\n"
            + "    String summary \"The West Coast Observing System (WCOS) project provides access to temperature and currents data collected at four of the five National Marine Sanctuary sites, including Olympic Coast, Gulf of the Farallones, Monterey Bay, and Channel Islands. A semi-automated end-to-end data management system transports and transforms the data from source to archive, making the data acessible for discovery, access and analysis from multiple Internet points of entry.\n"
            + "\n"
            + "The stations (and their code names) are Ano Nuevo (ANO001), San Miguel North (BAY), Santa Rosa North (BEA), Big Creek (BIG001), Bodega Head (BOD001), Cape Alava 15M (CA015), Cape Alava 42M (CA042), Cape Alava 65M (CA065), Cape Alava 100M (CA100), Cannery Row (CAN001), Cape Elizabeth 15M (CE015), Cape Elizabeth 42M (CE042), Cape Elizabeth 65M (CE065), Cape Elizabeth 100M (CE100), Cuyler Harbor (CUY), Esalen (ESA001), Point Joe (JOE001), Kalaloch 15M (KL015), Kalaloch 27M (KL027), La Cruz Rock (LAC001), Lopez Rock (LOP001), Makah Bay 15M (MB015), Makah Bay 42M (MB042), Pelican/Prisoners Area (PEL), Pigeon Point (PIG001), Plaskett Rock (PLA001), Southeast Farallon Island (SEF001), San Miguel South (SMS), Santa Rosa South (SRS), Sunset Point (SUN001), Teawhit Head 15M (TH015), Teawhit Head 31M (TH031), Teawhit Head 42M (TH042), Terrace Point 7 (TPT007), Terrace Point 8 (TPT008), Valley Anch (VAL), Weston Beach (WES001).\";\n"
            + "    String time_coverage_end \"2011-02-12T07:32:00Z\";\n"
            + // changes
            "    String time_coverage_start \"2004-10-07T21:10:00Z\";\n"
            + "    String title \"West Coast Observing System (WCOS) Temperature Data, 2004-2011\";\n"
            + "    String Version \"2\";\n"
            + "    Float64 Westernmost_Easting -124.932;\n"
            + "  }\n"
            + "}\n";
    int tPo = results.indexOf(expected.substring(0, 17));
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
        expected,
        "results=\n" + results);

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
            + "    String station;\n"
            + "    Float64 longitude;\n"
            + "    Float64 latitude;\n"
            + "    Float64 time;\n"
            + "    Float64 depth;\n"
            + "    Float64 Temperature;\n"
            + "    Byte Temperature_flag;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // *** test make data files
    String2.log("\n****************** EDDTableFromThreddsFiles.testWcosTemp make DATA FILES\n");

    // .csv for one lat,lon,time
    userDapQuery = "station&distinct()";
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_stationList",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "station\n"
            + "\n"
            + "ANO001\n"
            + "BAYXXX\n"
            + "BEAXXX\n"
            + "BIG001\n"
            + "BOD001\n"
            + "CA015X\n"
            + "CA042X\n"
            + "CA065X\n"
            + "CA100X\n"
            + "CE015X\n"
            + "CE042X\n"
            + "CE065X\n"
            + "CE100X\n"
            + "ESA001\n"
            + "JOE001\n"
            + "KL015X\n"
            + "KL027X\n"
            + "LAC001\n"
            + "LOP001\n"
            + "MB015X\n"
            + "MB042X\n"
            + "PELXXX\n"
            + "PIG001\n"
            + "SEF001\n"
            + "SMSXXX\n"
            + "SRSXXX\n"
            + "SUN001\n"
            + "TH015X\n"
            + "TH031X\n"
            + "TH042X\n"
            + "TPT007\n"
            + "TPT008\n"
            + "VALXXX\n"
            + "WES001\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .csv for one lat,lon,time, many depths (from different files) via lon > <
    userDapQuery = "&station=\"ANO001\"&time=1122592440";
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_1StationGTLT",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    // one depth, by hand via DAP form: (note that depth is already negative!)
    // Lat, 37.13015 Time=1122592440 depth=-12 Lon=-122.361253
    // yearday 208.968, temp_flag 0, temp 10.66, yeardayflag 0
    expected =
        "station,longitude,latitude,time,depth,Temperature,Temperature_flag\n"
            + ",degrees_east,degrees_north,UTC,m,degree_C,\n"
            + "ANO001,-122.361253,37.13015,2005-07-28T23:14:00Z,4.0,12.04,0\n"
            + "ANO001,-122.361253,37.13015,2005-07-28T23:14:00Z,12.0,10.66,0\n"
            + "ANO001,-122.361253,37.13015,2005-07-28T23:14:00Z,20.0,10.51,0\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    /* */
  }
}
