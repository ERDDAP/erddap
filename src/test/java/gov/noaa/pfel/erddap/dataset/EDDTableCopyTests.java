package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tags.TagMissingDataset;
import tags.TagSlowTests;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

class EDDTableCopyTests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /** The basic tests of this class. */
  @org.junit.jupiter.api.Test
  @TagSlowTests // If the dataset needs to be copied, this is slow.
  void testTableCopyBasic() throws Throwable {
    // testVerboseOn();
    int language = 0;

    String name, tName, results, tResults, expected, expected2, expected3, userDapQuery, tQuery;
    String tDir = EDStatic.fullTestCacheDirectory;
    String error = "";
    int epo, tPo;
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
    String mapDapQuery = "longitude,latitude,NO3,time&latitude>0&time>=2002-08-03";
    userDapQuery = "longitude,NO3,time,ship&latitude%3E0&time%3E=2002-08-03";

    EDDTable edd = (EDDTableCopy) EDDTestDataset.gettestTableCopy();

    // *** test getting das for entire dataset
    String2.log("\n****************** EDDTableCopy.test das dds for entire dataset\n");
    tName =
        edd.makeNewFileForDapQuery(
            language, null, null, "", tDir, edd.className() + "_Entire", ".das");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected = // see OpendapHelper.EOL for comments
        "Attributes {\n"
            + " s {\n"
            + "  cruise_id {\n"
            + "    String cf_role \"trajectory_id\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Cruise ID\";\n"
            + "  }\n"
            + "  ship {\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Ship\";\n"
            + "  }\n"
            + "  cast {\n"
            + "    Int16 _FillValue 32767;\n"
            + "    Int16 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 140.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Cast Number\";\n"
            + "    Int16 missing_value 32767;\n"
            + "  }\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float32 _FillValue 327.67;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    Float32 missing_value 327.67;\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float32 _FillValue 327.67;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    Float32 missing_value 327.67;\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range MIN, MAX;\n"
            + "    String axis \"T\";\n"
            + "    String cf_role \"profile_id\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  bottle_posn {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    Byte _FillValue 127;\n"
            + "    String _Unsigned \"false\";\n"
            + "    Int16 actual_range MIN, MAX;\n"
            + "    String axis \"Z\";\n"
            + "    Float64 colorBarMaximum 12.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Bottle Number\";\n"
            + "    Byte missing_value -128;\n"
            + "  }\n"
            + "  chl_a_total {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 30.0;\n"
            + "    Float64 colorBarMinimum 0.03;\n"
            + "    String colorBarScale \"Log\";\n"
            + "    String ioos_category \"Ocean Color\";\n"
            + "    String long_name \"Chlorophyll-a\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String standard_name \"concentration_of_chlorophyll_in_sea_water\";\n"
            + "    String units \"ug L-1\";\n"
            + "  }\n"
            + "  chl_a_10um {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 30.0;\n"
            + "    Float64 colorBarMinimum 0.03;\n"
            + "    String colorBarScale \"Log\";\n"
            + "    String ioos_category \"Ocean Color\";\n"
            + "    String long_name \"Chlorophyll-a after passing 10um screen\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String standard_name \"concentration_of_chlorophyll_in_sea_water\";\n"
            + "    String units \"ug L-1\";\n"
            + "  }\n"
            + "  phaeo_total {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 30.0;\n"
            + "    Float64 colorBarMinimum 0.03;\n"
            + "    String colorBarScale \"Log\";\n"
            + "    String ioos_category \"Ocean Color\";\n"
            + "    String long_name \"Total Phaeopigments\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String units \"ug L-1\";\n"
            + "  }\n"
            + "  phaeo_10um {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 30.0;\n"
            + "    Float64 colorBarMinimum 0.03;\n"
            + "    String colorBarScale \"Log\";\n"
            + "    String ioos_category \"Ocean Color\";\n"
            + "    String long_name \"Phaeopigments 10um\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String units \"ug L-1\";\n"
            + "  }\n"
            + "  sal00 {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 37.0;\n"
            + "    Float64 colorBarMinimum 32.0;\n"
            + "    String ioos_category \"Salinity\";\n"
            + "    String long_name \"Practical Salinity from T0 and C0 Sensors\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String standard_name \"sea_water_practical_salinity\";\n"
            + "    String units \"PSU\";\n"
            + "  }\n"
            + "  sal11 {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 37.0;\n"
            + "    Float64 colorBarMinimum 32.0;\n"
            + "    String ioos_category \"Salinity\";\n"
            + "    String long_name \"Practical Salinity from T1 and C1 Sensors\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String standard_name \"sea_water_practical_salinity\";\n"
            + "    String units \"PSU\";\n"
            + "  }\n"
            + "  temperature0 {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 32.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Temperature\";\n"
            + "    String long_name \"Sea Water Temperature from T0 Sensor\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String standard_name \"sea_water_temperature\";\n"
            + "    String units \"degree_C\";\n"
            + "  }\n"
            + "  temperature1 {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 32.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Temperature\";\n"
            + "    String long_name \"Sea Water Temperature from T1 Sensor\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String standard_name \"sea_water_temperature\";\n"
            + "    String units \"degree_C\";\n"
            + "  }\n"
            + "  fluor_v {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 5.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Ocean Color\";\n"
            + "    String long_name \"Fluorescence Voltage\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String units \"volts\";\n"
            + "  }\n"
            + "  xmiss_v {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 5.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Optical Properties\";\n"
            + "    String long_name \"Transmissivity Voltage\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String units \"volts\";\n"
            + "  }\n"
            + "  PO4 {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 4.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Dissolved Nutrients\";\n"
            + "    String long_name \"Phosphate\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String standard_name \"mole_concentration_of_phosphate_in_sea_water\";\n"
            + "    String units \"micromoles L-1\";\n"
            + "  }\n"
            + "  N_N {\n"
            + "    Float32 _FillValue -99.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 50.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Dissolved Nutrients\";\n"
            + "    String long_name \"Nitrate plus Nitrite\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String standard_name \"moles_of_nitrate_and_nitrite_per_unit_mass_in_sea_water\";\n"
            + "    String units \"micromoles L-1\";\n"
            + "  }\n"
            + "  NO3 {\n"
            + "    Float32 _FillValue -99.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 50.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Dissolved Nutrients\";\n"
            + "    String long_name \"Nitrate\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String standard_name \"mole_concentration_of_nitrate_in_sea_water\";\n"
            + "    String units \"micromoles L-1\";\n"
            + "  }\n"
            + "  Si {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 50.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Dissolved Nutrients\";\n"
            + "    String long_name \"Silicate\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String standard_name \"mole_concentration_of_silicate_in_sea_water\";\n"
            + "    String units \"micromoles L-1\";\n"
            + "  }\n"
            + "  NO2 {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 1.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Dissolved Nutrients\";\n"
            + "    String long_name \"Nitrite\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String standard_name \"mole_concentration_of_nitrite_in_sea_water\";\n"
            + "    String units \"micromoles L-1\";\n"
            + "  }\n"
            + "  NH4 {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 5.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Dissolved Nutrients\";\n"
            + "    String long_name \"Ammonium\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String standard_name \"mole_concentration_of_ammonium_in_sea_water\";\n"
            + "    String units \"micromoles L-1\";\n"
            + "  }\n"
            + "  oxygen {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 10.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Dissolved O2\";\n"
            + "    String long_name \"Oxygen\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String standard_name \"volume_fraction_of_oxygen_in_sea_water\";\n"
            + "    String units \"mL L-1\";\n"
            + "  }\n"
            + "  par {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 3.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Ocean Color\";\n"
            + "    String long_name \"Photosynthetically Active Radiation\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String units \"volts\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_altitude_proxy \"bottle_posn\";\n"
            + "    String cdm_data_type \"TrajectoryProfile\";\n"
            + "    String cdm_profile_variables \"cast, longitude, latitude, time\";\n"
            + "    String cdm_trajectory_variables \"cruise_id, ship\";\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "    Float64 Easternmost_Easting -124.1;\n"
            + "    String featureType \"TrajectoryProfile\";\n"
            + "    Float64 geospatial_lat_max 44.65;\n"
            + "    Float64 geospatial_lat_min 41.9;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max -124.1;\n"
            + "    Float64 geospatial_lon_min -126.2;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n";
    // " String history \"" + today + " 2012-07-29T19:11:09Z (local files; contact
    // erd.data@noaa.gov)\n"; //date is from last created file, so varies sometimes
    // today + "
    // http://localhost:" + PORT + "/erddap/tabledap/erdGlobecBottle.das"; //\n"
    // +
    // today + " https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle\n" +
    // today + "
    // http://localhost:" + PORT + "/erddap/tabledap/rGlobecBottle.das\";\n" +
    expected2 =
        "    String infoUrl \"https://en.wikipedia.org/wiki/Global_Ocean_Ecosystem_Dynamics\";\n"
            + "    String institution \"GLOBEC\";\n"
            + "    String keywords \"10um, active, after, ammonia, ammonium, attenuation, biosphere, bottle, cast, chemistry, chlorophyll, chlorophyll-a, color, concentration, concentration_of_chlorophyll_in_sea_water, cruise, data, density, dissolved, dissolved nutrients, dissolved o2, Earth Science > Biosphere > Vegetation > Photosynthetically Active Radiation, Earth Science > Oceans > Ocean Chemistry > Ammonia, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, Earth Science > Oceans > Ocean Chemistry > Nitrate, Earth Science > Oceans > Ocean Chemistry > Nitrite, Earth Science > Oceans > Ocean Chemistry > Nitrogen, Earth Science > Oceans > Ocean Chemistry > Oxygen, Earth Science > Oceans > Ocean Chemistry > Phosphate, Earth Science > Oceans > Ocean Chemistry > Pigments, Earth Science > Oceans > Ocean Chemistry > Silicate, Earth Science > Oceans > Ocean Optics > Attenuation/Transmission, Earth Science > Oceans > Ocean Temperature > Water Temperature, Earth Science > Oceans > Salinity/Density > Salinity, fluorescence, fraction, from, globec, identifier, mass, mole, mole_concentration_of_ammonium_in_sea_water, mole_concentration_of_nitrate_in_sea_water, mole_concentration_of_nitrite_in_sea_water, mole_concentration_of_phosphate_in_sea_water, mole_concentration_of_silicate_in_sea_water, moles, moles_of_nitrate_and_nitrite_per_unit_mass_in_sea_water, n02, nep, nh4, nitrate, nitrite, nitrogen, no3, number, nutrients, o2, ocean, ocean color, oceans, optical, optical properties, optics, oxygen, passing, per, phaeopigments, phosphate, photosynthetically, pigments, plus, po4, properties, radiation, rosette, salinity, screen, sea, sea_water_practical_salinity, sea_water_temperature, seawater, sensor, sensors, ship, silicate, temperature, time, total, transmission, transmissivity, unit, vegetation, voltage, volume, volume_fraction_of_oxygen_in_sea_water, water\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    Float64 Northernmost_Northing 44.65;\n"
            + "    String sourceUrl \"(local files; contact erd.data@noaa.gov)\";\n"
            + "    Float64 Southernmost_Northing 41.9;\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String subsetVariables \"cruise_id, ship, cast, longitude, latitude, time\";\n"
            + "    String summary \"GLOBEC (GLOBal Ocean ECosystems Dynamics) NEP (Northeast Pacific)\n"
            + "Rosette Bottle Data from New Horizon Cruise (NH0207: 1-19 August 2002).\n"
            + "Notes:\n"
            + "Physical data processed by Jane Fleischbein (OSU).\n"
            + "Chlorophyll readings done by Leah Feinberg (OSU).\n"
            + "Nutrient analysis done by Burke Hales (OSU).\n"
            + "Sal00 - salinity calculated from primary sensors (C0,T0).\n"
            + "Sal11 - salinity calculated from secondary sensors (C1,T1).\n"
            + "secondary sensor pair was used in final processing of CTD data for\n"
            + "most stations because the primary had more noise and spikes. The\n"
            + "primary pair were used for cast #9, 24, 48, 111 and 150 due to\n"
            + "multiple spikes or offsets in the secondary pair.\n"
            + "Nutrient samples were collected from most bottles; all nutrient data\n"
            + "developed from samples frozen during the cruise and analyzed ashore;\n"
            + "data developed by Burke Hales (OSU).\n"
            + "Operation Detection Limits for Nutrient Concentrations\n"
            + "Nutrient  Range         Mean    Variable         Units\n"
            + "PO4       0.003-0.004   0.004   Phosphate        micromoles per liter\n"
            + "N+N       0.04-0.08     0.06    Nitrate+Nitrite  micromoles per liter\n"
            + "Si        0.13-0.24     0.16    Silicate         micromoles per liter\n"
            + "NO2       0.003-0.004   0.003   Nitrite          micromoles per liter\n"
            + "Dates and Times are UTC.\n"
            + "\n"
            + "For more information, see https://www.bco-dmo.org/dataset/2452\n"
            + "\n"
            + "Inquiries about how to access this data should be directed to\n"
            + "Dr. Hal Batchelder (hbatchelder@coas.oregonstate.edu).\";\n"
            + "    String time_coverage_end \"yyyy-MM-ddThh:mm:ssZ\";\n"
            + "    String time_coverage_start \"2002-05-30T03:21:00Z\";\n"
            + "    String title \"GLOBEC NEP Rosette Bottle Data (2002)\";\n"
            + "    Float64 Westernmost_Easting -126.2;\n"
            + "  }\n"
            + "}\n";

    results =
        results.replaceAll("Byte actual_range [0-9]+, [0-9]+;", "Int16 actual_range MIN, MAX;");
    results =
        results.replaceAll("Int16 actual_range [0-9]+, [0-9]+;", "Int16 actual_range MIN, MAX;");
    results =
        results.replaceAll(
            "Float32 actual_range -?[0-9]+.[0-9]+, -?[0-9]+.[0-9]+;",
            "Float32 actual_range MIN, MAX;");
    results =
        results.replaceAll(
            "Float64 actual_range -?[0-9].[0-9]+e[+][0-9], -?[0-9].[0-9]+e[+][0-9];",
            "Float64 actual_range MIN, MAX;");
    results =
        results.replaceAll(
            "String time_coverage_end \\\"....-..-..T..:..:..Z",
            "String time_coverage_end \"yyyy-MM-ddThh:mm:ssZ");
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    tPo = results.indexOf("    String infoUrl ");
    Test.ensureEqual(results.substring(tPo), expected2, "\nresults=\n" + results);

    // *** test getting dds for entire dataset
    tName =
        edd.makeNewFileForDapQuery(
            language, null, null, "", tDir, edd.className() + "_Entire", ".dds");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    String cruise_id;\n"
            + "    String ship;\n"
            + "    Int16 cast;\n"
            + "    Float32 longitude;\n"
            + "    Float32 latitude;\n"
            + "    Float64 time;\n"
            + "    Byte bottle_posn;\n"
            + "    Float32 chl_a_total;\n"
            + "    Float32 chl_a_10um;\n"
            + "    Float32 phaeo_total;\n"
            + "    Float32 phaeo_10um;\n"
            + "    Float32 sal00;\n"
            + "    Float32 sal11;\n"
            + "    Float32 temperature0;\n"
            + "    Float32 temperature1;\n"
            + "    Float32 fluor_v;\n"
            + "    Float32 xmiss_v;\n"
            + "    Float32 PO4;\n"
            + "    Float32 N_N;\n"
            + "    Float32 NO3;\n"
            + "    Float32 Si;\n"
            + "    Float32 NO2;\n"
            + "    Float32 NH4;\n"
            + "    Float32 oxygen;\n"
            + "    Float32 par;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // *** test DAP data access form
    tName =
        edd.makeNewFileForDapQuery(
            language, null, null, "", tDir, edd.className() + "_Entire", ".html");
    results = File2.directReadFromUtf8File(tDir + tName);
    expected = "<option>.png - View a standard, medium-sized .png image file with a graph or map.";
    expected2 = "    String _CoordinateAxisType &quot;Lon&quot;;";
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    Test.ensureTrue(results.indexOf(expected2) > 0, "\nresults=\n" + results);
    // Test.displayInBrowser("file://" + tDir + tName);

    // *** test make data files
    String2.log("\n****************** EDDTableCopy.test make DATA FILES\n");

    // .asc
    tName =
        edd.makeNewFileForDapQuery(
            language, null, null, userDapQuery, tDir, edd.className() + "_Data", ".asc");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    Float32 longitude;\n"
            + "    Float32 NO3;\n"
            + "    Float64 time;\n"
            + "    String ship;\n"
            + "  } s;\n"
            + "} s;\n"
            + "---------------------------------------------\n"
            + "s.longitude, s.NO3, s.time, s.ship\n"
            + "-124.4, 35.7, 1.02833814E9, \"New_Horizon\"\n";
    expected2 =
        "-124.8, -9999.0, 1.02835902E9, \"New_Horizon\"\n"; // row with missing value has source
    // missing
    // value
    expected3 = "-124.57, 19.31, 1.02939792E9, \"New_Horizon\"\n"; // last row
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    Test.ensureTrue(results.indexOf(expected2) > 0, "\nresults=\n" + results);
    Test.ensureTrue(
        results.indexOf(expected3) > 0,
        "\nresults=\n" + results); // last row in erdGlobedBottle, not
    // last
    // here

    // .csv
    tName =
        edd.makeNewFileForDapQuery(
            language, null, null, userDapQuery, tDir, edd.className() + "_Data", ".csv");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected =
        "longitude,NO3,time,ship\n"
            + "degrees_east,micromoles L-1,UTC,\n"
            + "-124.4,35.7,2002-08-03T01:29:00Z,New_Horizon\n"
            + "-124.4,35.48,2002-08-03T01:29:00Z,New_Horizon\n"
            + "-124.4,31.61,2002-08-03T01:29:00Z,New_Horizon\n";
    expected2 =
        "-124.8,NaN,2002-08-03T07:17:00Z,New_Horizon\n"; // row with missing value has source
    // missing value
    expected3 = "-124.57,19.31,2002-08-15T07:52:00Z,New_Horizon\n"; // last row
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    Test.ensureTrue(results.indexOf(expected2) > 0, "\nresults=\n" + results);
    Test.ensureTrue(
        results.indexOf(expected3) > 0,
        "\nresults=\n" + results); // last row in erdGlobedBottle, not
    // last
    // here

    // .dds
    tName =
        edd.makeNewFileForDapQuery(
            language, null, null, userDapQuery, tDir, edd.className() + "_Data", ".dds");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    Float32 longitude;\n"
            + "    Float32 NO3;\n"
            + "    Float64 time;\n"
            + "    String ship;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
  }

  /** The tests testRepPostDet. */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @TagMissingDataset
  void testRepPostDet(boolean tCheckSourceData) throws Throwable {
    String2.log(
        "\n****************** EDDTableCopy.testRepPostDet(tCheckSourceData="
            + tCheckSourceData
            + ") *****************\n");
    // testVerboseOn();
    int language = 0;
    // defaultCheckSourceData = tCheckSourceData;
    String name, tName, results, tResults, expected, expected2, expected3, userDapQuery, tQuery;
    String error = "";
    int epo, tPo;
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
    long eTime;
    EDDTable edd = null;

    // maxChunks = 400;

    try {
      edd = (EDDTableCopy) EDDTestDataset.getrepPostDet();
    } catch (Throwable t2) {
      // it will fail if no files have been copied
      String2.log(MustBe.throwableToString(t2));
    }
    if (tCheckSourceData && EDStatic.nUnfinishedTasks() > 0) {
      while (EDStatic.nUnfinishedTasks() > 0) {
        String2.log("nUnfinishedTasks=" + EDStatic.nUnfinishedTasks());
        Math2.sleep(10000);
      }
      // recreate edd to see new copied data files
      edd = (EDDTableCopy) EDDTestDataset.getrepPostDet();
    }
    // reallyVerbose=false;

    // .dds
    eTime = System.currentTimeMillis();
    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            edd.className() + "_postDet",
            ".dds");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    Float64 longitude;\n"
            + "    Float64 latitude;\n"
            + "    Float64 time;\n"
            + "    String common_name;\n"
            + "    String pi;\n"
            + "    String project;\n"
            + "    Int32 surgery_id;\n"
            + "    String tag_id_code;\n"
            + "    String tag_sn;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .das
    eTime = System.currentTimeMillis();
    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            edd.className() + "_postDet",
            ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected =
        "    String infoUrl \"http://www.postprogram.org/\";\n"
            + "    String institution \"POST\";\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    expected = "  surgery_id {\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);

    // 1var
    eTime = System.currentTimeMillis();
    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "pi&distinct()",
            EDStatic.fullTestCacheDirectory,
            edd.className() + "_postDet1Var",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    String2.log(results);
    expected = "pi\n" + "\n" + "BARRY BEREJIKIAN\n" + "CEDAR CHITTENDEN\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    String2.log(
        "*** 1var elapsed time="
            + (System.currentTimeMillis() - eTime)
            + "ms (vs 148,000 or 286,000 ms for POST).");

    // 2var
    eTime = System.currentTimeMillis();
    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "pi,common_name&distinct()",
            EDStatic.fullTestCacheDirectory,
            edd.className() + "_postDet2var",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected = // this will change
        "pi,common_name\n"
            + ",\n"
            + "BARRY BEREJIKIAN,STEELHEAD\n"
            + "CEDAR CHITTENDEN,COHO\n"
            + "CHRIS WOOD,\"SOCKEYE,KOKANEE\"\n"
            + "CHUCK BOGGS,COHO\n"
            + "DAVID WELCH,CHINOOK\n"
            + "DAVID WELCH,COHO\n"
            + "DAVID WELCH,DOLLY VARDEN\n"
            + "DAVID WELCH,\"SOCKEYE,KOKANEE\"\n"
            + "DAVID WELCH,STEELHEAD\n"
            + "FRED GOETZ,CHINOOK\n"
            + "FRED GOETZ,CUTTHROAT\n"
            + "JACK TIPPING,STEELHEAD\n"
            + "JEFF MARLIAVE,BLACK ROCKFISH\n"
            + "JOHN PAYNE,SQUID\n"
            + "LYSE GODBOUT,\"SOCKEYE,KOKANEE\"\n"
            + "MIKE MELNYCHUK,COHO\n"
            + "MIKE MELNYCHUK,\"SOCKEYE,KOKANEE\"\n"
            + "MIKE MELNYCHUK,STEELHEAD\n"
            + "ROBERT BISON,STEELHEAD\n"
            + "SCOTT STELTZNER,CHINOOK\n"
            + "SCOTT STELTZNER,COHO\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
    String2.log(results);
    String2.log(
        "*** 2var elapsed time="
            + (System.currentTimeMillis() - eTime)
            + "ms (vs 192,000 ms for POST).");

    // 3var
    eTime = System.currentTimeMillis();
    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "pi,common_name,surgery_id&distinct()",
            EDStatic.fullTestCacheDirectory,
            edd.className() + "_postDet3var",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected = "pi,common_name,surgery_id\n" + ",,\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected = "BARRY BEREJIKIAN,STEELHEAD,2846\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    String lines[] = String2.split(results, '\n');
    Test.ensureEqual(lines.length, 4317 + 3, "\nresults=\n" + results);
    lines = null;
    String2.log(
        "*** 3var elapsed time="
            + (System.currentTimeMillis() - eTime)
            + "ms (vs 152,000ms for POST).");

    // 1tag
    eTime = System.currentTimeMillis();
    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&pi=\"BARRY BEREJIKIAN\"&common_name=\"STEELHEAD\"&surgery_id=2846",
            EDStatic.fullTestCacheDirectory,
            edd.className() + "_postDet1tag",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "longitude,latitude,time,common_name,pi,project,surgery_id,tag_id_code,tag_sn\n"
            + "degrees_east,degrees_north,UTC,,,,,,\n"
            + "-127.34393,50.67973,2004-05-30T06:08:40Z,STEELHEAD,BARRY BEREJIKIAN,NOAA|NOAA FISHERIES,2846,3985,1031916\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    String2.log(
        "*** 1tag elapsed time="
            + (System.currentTimeMillis() - eTime)
            + "ms (vs 5,700ms for POST).");

    // constraint
    eTime = System.currentTimeMillis();
    tQuery =
        "&pi=\"DAVID WELCH\"&common_name=\"CHINOOK\"&latitude>50"
            + "&surgery_id>=1201&surgery_id<1202&time>=2007-05-01T08&time<2007-05-01T09";
    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            tQuery,
            EDStatic.fullTestCacheDirectory,
            edd.className() + "_peb_constrained",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "longitude,latitude,time,common_name,pi,project,surgery_id,tag_id_code,tag_sn\n"
            + "degrees_east,degrees_north,UTC,,,,,,\n"
            + "-127.48843,50.78142,2007-05-01T08:43:33Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n"
            + "-127.48843,50.78142,2007-05-01T08:48:23Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n"
            + "-127.48843,50.78142,2007-05-01T08:51:14Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n"
            + "-127.48843,50.78142,2007-05-01T08:53:18Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n"
            + "-127.48843,50.78142,2007-05-01T08:56:23Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n"
            + "-127.48843,50.78142,2007-05-01T08:59:27Z,CHINOOK,DAVID WELCH,KINTAMA RESEARCH,1201,1054,1030254\n";
    Test.ensureEqual(
        results.substring(0, Math.min(results.length(), expected.length())),
        expected,
        "\nresults=\n" + results);
    String2.log(
        "*** constraint elapsed time=" + (System.currentTimeMillis() - eTime) + "ms (usually 31).");

    // done
    // String2.pressEnterToContinue("EDDTableCopy.testRepPostDet done.");

    // defaultCheckSourceData = true;

  } // end of testRepPostDet
}
