package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.junit.jupiter.api.BeforeAll;
import tags.TagLocalERDDAP;
import tags.TagMissingDataset;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

class EDDGridFromEDDTableTests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /** testGenerateDatasetsXml */
  @org.junit.jupiter.api.Test
  void testGenerateDatasetsXml() throws Throwable {
    // testVerboseOn();
    // String2.log("\n*** EDDGridFromEDDTable.testGenerateDatasetsXml");

    // This test relies on the development/test/datasets.xml file existing, so make sure it's
    // created.
    EDDTestDataset.generateDatasetsXml();
    String tid = "erdNph";

    String expected =
        "<dataset type=\"EDDGridFromEDDTable\" datasetID=\"erdNph_c2e8_7f71_e246\" active=\"true\">\n"
            + "    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n"
            + "    <gapThreshold>1000</gapThreshold>\n"
            + "    <!-- sourceAttributes>\n"
            + "        <att name=\"cdm_data_type\">Point</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n"
            + "        <att name=\"defaultDataQuery\">time,year,month,longitude,latitude,area,maxSLP</att>\n"
            + "        <att name=\"defaultGraphQuery\">longitude,latitude,month&amp;.draw=markers</att>\n"
            + "        <att name=\"Easternmost_Easting\" type=\"double\">233.5</att>\n"
            + "        <att name=\"featureType\">Point</att>\n"
            + "        <att name=\"geospatial_lat_max\" type=\"double\">39.3</att>\n"
            + "        <att name=\"geospatial_lat_min\" type=\"double\">23.3</att>\n"
            + "        <att name=\"geospatial_lat_units\">degrees_north</att>\n"
            + "        <att name=\"geospatial_lon_max\" type=\"double\">233.5</att>\n"
            + "        <att name=\"geospatial_lon_min\" type=\"double\">201.3</att>\n"
            + "        <att name=\"geospatial_lon_units\">degrees_east</att>\n"
            + "        <att name=\"id\">erdNph</att>\n"
            + "        <att name=\"infoUrl\">https://onlinelibrary.wiley.com/doi/10.1002/grl.50100/abstract</att>\n"
            + "        <att name=\"institution\">NOAA ERD</att>\n"
            + "        <att name=\"keywords\">area, areal, california, ccs, center, centered, contour, current, data, extent, high, hpa, level, maximum, month, north, nph, pacific, pressure, sea, system, time, year</att>\n"
            + "        <att name=\"license\">The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.</att>\n"
            + "        <att name=\"Northernmost_Northing\" type=\"double\">39.3</att>\n"
            + "        <att name=\"references\">Schroeder, Isaac D., Bryan A. Black, William J. Sydeman, Steven J. Bograd, Elliott L. Hazen, Jarrod A. Santora, and Brian K. Wells. &quot;The North Pacific High and wintertime pre-conditioning of California current productivity&quot;, Geophys. Res. Letters, VOL. 40, 541-546, doi:10.1002/grl.50100, 2013</att>\n"
            + "        <att name=\"sourceUrl\">(local files)</att>\n"
            + "        <att name=\"Southernmost_Northing\" type=\"double\">23.3</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"subsetVariables\">time, year, month</att>\n"
            + "        <att name=\"summary\">Variations in large-scale atmospheric forcing influence upwelling dynamics and ecosystem productivity in the California Current System (CCS). In this paper, we characterize interannual variability of the North Pacific High over 40 years and investigate how variation in its amplitude and position affect upwelling and biology. We develop a winter upwelling &quot;pre-conditioning&quot; index and demonstrate its utility to understanding biological processes. Variation in the winter NPH can be well described by its areal extent and maximum pressure, which in turn is predictive of winter upwelling. Our winter pre-conditioning index explained 64&#37; of the variation in biological responses (fish and seabirds). Understanding characteristics of the NPH in winter is therefore critical to predicting biological responses in the CCS.</att>\n"
            + "        <att name=\"time_coverage_end\">2014-01-16</att>\n"
            + "        <att name=\"time_coverage_start\">1967-01-16</att>\n"
            + "        <att name=\"title\">North Pacific High, 1967 - 2014</att>\n"
            + "        <att name=\"Westernmost_Easting\" type=\"double\">201.3</att>\n"
            + "    </sourceAttributes -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cdm_data_type\">Grid</att>\n"
            + "        <att name=\"featureType\">null</att>\n"
            + "    </addAttributes>\n"
            + "\n"
            + "    <!-- *** If appropriate:\n"
            + "      * Change some of the <dataVariables> to be <axisVariables>.\n"
            + "      * Insert them here in the correct order.\n"
            + "      * For each one, add to its <addAttributes> one of:\n"
            + "        <att name=\"axisValues\" type=\"doubleList\">a CSV list of values</att>\n"
            + "        <att name=\"axisValuesStartStrideStop\" type=\"doubleList\">startValue, strideValue, stopValue</att>\n"
            + "      * For each one, if defaults aren't suitable, add\n"
            + "        <att name=\"precision\" type=\"int\">totalNumberOfDigitsToMatch</att>\n"
            + "    -->\n"
            + "\n"
            + "    <axisVariable>\n"
            + "        <sourceName>time</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Time</att>\n"
            + "            <att name=\"actual_range\" type=\"doubleList\">-9.33984E7 1.3898304E9</att>\n"
            + "            <att name=\"axis\">T</att>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"long_name\">Centered Time</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"time_origin\">01-JAN-1970 00:00:00</att>\n"
            + "            <att name=\"time_precision\">1970-01-01</att>\n"
            + "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "\n"
            + "    <dataVariable>\n"
            + "        <sourceName>year</sourceName>\n"
            + "        <destinationName>year</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"short\">32767</att>\n"
            + "            <att name=\"actual_range\" type=\"shortList\">1967 2014</att>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"long_name\">Year</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>month</sourceName>\n"
            + "        <destinationName>month</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"byte\">127</att>\n"
            + "            <att name=\"actual_range\" type=\"byteList\">1 12</att>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"long_name\">Month (1 - 12)</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>longitude</sourceName>\n"
            + "        <destinationName>longitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Lon</att>\n"
            + "            <att name=\"actual_range\" type=\"floatList\">201.3 233.5</att>\n"
            + "            <att name=\"axis\">X</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Longitude of the Center of the NPH</att>\n"
            + "            <att name=\"standard_name\">longitude</att>\n"
            + "            <att name=\"units\">degrees_east</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>latitude</sourceName>\n"
            + "        <destinationName>latitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Lat</att>\n"
            + "            <att name=\"actual_range\" type=\"floatList\">23.3 39.3</att>\n"
            + "            <att name=\"axis\">Y</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Latitude of the Center of the NPH</att>\n"
            + "            <att name=\"standard_name\">latitude</att>\n"
            + "            <att name=\"units\">degrees_north</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>area</sourceName>\n"
            + "        <destinationName>area</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"actual_range\" type=\"floatList\">0.0 7810500.0</att>\n"
            + "            <att name=\"ioos_category\">Pressure</att>\n"
            + "            <att name=\"long_name\">Areal Extent of the 1020 hPa Contour</att>\n"
            + "            <att name=\"units\">km2</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>maxSLP</sourceName>\n"
            + "        <destinationName>maxSLP</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"actual_range\" type=\"floatList\">1016.7 1033.3</att>\n"
            + "            <att name=\"ioos_category\">Pressure</att>\n"
            + "            <att name=\"long_name\">Maximum Sea Level Pressure</att>\n"
            + "            <att name=\"units\">hPa</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "\n"
            + "    <!-- *** Insert the entire <dataset> chunk for erdNph here.\n"
            + "       If the original dataset will be accessible to users, change the\n"
            + "       datasetID here so they aren't the same. -->\n"
            + "    <dataset ... > ... </dataset>\n"
            + "\n"
            + "</dataset>\n"
            + "\n\n";

    String results = EDDGridFromEDDTable.generateDatasetsXml(tid, -1, null) + "\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // int po = results.indexOf(expected2.substring(0, 40));
    // Test.ensureEqual(results.substring(po), expected2, "results=\n" + results);

    // GenerateDatasetsXml
    String gdxResults =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {"-verbose", "EDDGridFromEDDTable", tid, "-1"}, // default reloadEvery,
                false); // doIt loop?
    Test.ensureEqual(gdxResults, expected, "Unexpected results from GenerateDatasetsXml.doIt.");
  }

  /** testBasic */
  @org.junit.jupiter.api.Test
  @TagMissingDataset // missing nmspWcosAdcpD/BAY
  void testBasic() throws Throwable {
    // String2.log("\n*** EDDGridFromEDDTable.testBasic\n");
    // testVerboseOn();
    String name, tName, query, results, expected, error;
    String testDir = EDStatic.fullTestCacheDirectory;
    int po;
    int language = 0;
    EDDGrid edd = (EDDGrid) EDDTestDataset.gettestGridFromTable();

    // get dds
    String2.log("\nget .dds\n");
    tName =
        edd.makeNewFileForDapQuery(
            language, null, null, "", testDir, edd.className() + "_Entire", ".dds");
    results = File2.directReadFrom88591File(testDir + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + "  Float64 time[time = 66201];\n"
            + "  Float64 latitude[latitude = 1];\n"
            + "  Float64 longitude[longitude = 1];\n"
            + "  Float64 depth[depth = 19];\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Int32 DataQuality[time = 66201][latitude = 1][longitude = 1][depth = 19];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 66201];\n"
            + "      Float64 latitude[latitude = 1];\n"
            + "      Float64 longitude[longitude = 1];\n"
            + "      Float64 depth[depth = 19];\n"
            + "  } DataQuality;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Byte DataQuality_flag[time = 66201][latitude = 1][longitude = 1][depth = 19];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 66201];\n"
            + "      Float64 latitude[latitude = 1];\n"
            + "      Float64 longitude[longitude = 1];\n"
            + "      Float64 depth[depth = 19];\n"
            + "  } DataQuality_flag;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float64 Eastward[time = 66201][latitude = 1][longitude = 1][depth = 19];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 66201];\n"
            + "      Float64 latitude[latitude = 1];\n"
            + "      Float64 longitude[longitude = 1];\n"
            + "      Float64 depth[depth = 19];\n"
            + "  } Eastward;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Byte Eastward_flag[time = 66201][latitude = 1][longitude = 1][depth = 19];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 66201];\n"
            + "      Float64 latitude[latitude = 1];\n"
            + "      Float64 longitude[longitude = 1];\n"
            + "      Float64 depth[depth = 19];\n"
            + "  } Eastward_flag;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float64 ErrorVelocity[time = 66201][latitude = 1][longitude = 1][depth = 19];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 66201];\n"
            + "      Float64 latitude[latitude = 1];\n"
            + "      Float64 longitude[longitude = 1];\n"
            + "      Float64 depth[depth = 19];\n"
            + "  } ErrorVelocity;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Byte ErrorVelocity_flag[time = 66201][latitude = 1][longitude = 1][depth = 19];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 66201];\n"
            + "      Float64 latitude[latitude = 1];\n"
            + "      Float64 longitude[longitude = 1];\n"
            + "      Float64 depth[depth = 19];\n"
            + "  } ErrorVelocity_flag;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Int32 Intensity[time = 66201][latitude = 1][longitude = 1][depth = 19];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 66201];\n"
            + "      Float64 latitude[latitude = 1];\n"
            + "      Float64 longitude[longitude = 1];\n"
            + "      Float64 depth[depth = 19];\n"
            + "  } Intensity;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Byte Intensity_flag[time = 66201][latitude = 1][longitude = 1][depth = 19];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 66201];\n"
            + "      Float64 latitude[latitude = 1];\n"
            + "      Float64 longitude[longitude = 1];\n"
            + "      Float64 depth[depth = 19];\n"
            + "  } Intensity_flag;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float64 Northward[time = 66201][latitude = 1][longitude = 1][depth = 19];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 66201];\n"
            + "      Float64 latitude[latitude = 1];\n"
            + "      Float64 longitude[longitude = 1];\n"
            + "      Float64 depth[depth = 19];\n"
            + "  } Northward;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Byte Northward_flag[time = 66201][latitude = 1][longitude = 1][depth = 19];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 66201];\n"
            + "      Float64 latitude[latitude = 1];\n"
            + "      Float64 longitude[longitude = 1];\n"
            + "      Float64 depth[depth = 19];\n"
            + "  } Northward_flag;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float64 Upwards[time = 66201][latitude = 1][longitude = 1][depth = 19];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 66201];\n"
            + "      Float64 latitude[latitude = 1];\n"
            + "      Float64 longitude[longitude = 1];\n"
            + "      Float64 depth[depth = 19];\n"
            + "  } Upwards;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Byte Upwards_flag[time = 66201][latitude = 1][longitude = 1][depth = 19];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 66201];\n"
            + "      Float64 latitude[latitude = 1];\n"
            + "      Float64 longitude[longitude = 1];\n"
            + "      Float64 depth[depth = 19];\n"
            + "  } Upwards_flag;\n"
            + "} testGridFromTable;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // get das
    String2.log("\nget .das\n");
    tName =
        edd.makeNewFileForDapQuery(
            language, null, null, "", testDir, edd.className() + "_Entire", ".das");
    results = File2.directReadFrom88591File(testDir + tName);
    // String2.log(results);
    expected =
        "Attributes {\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range 1.10184436e+9, 1.10978836e+9;\n"
            + "    String axis \"T\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float64 actual_range 34.04017, 34.04017;\n"
            + "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range -120.31121, -120.31121;\n"
            + "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  depth {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    String _CoordinateZisPositive \"down\";\n"
            +
            // " Float64 _FillValue 9999.0;\n" + //2020-08-10 I removed this via
            // addAttributes because axisVariables can't have missing values
            "    Float64 actual_range -1.8, 16.2;\n"
            + "    String axis \"Z\";\n"
            + "    String description \"Relative to Mean Sea Level (MSL)\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Depth\";\n"
            + "    String positive \"down\";\n"
            + "    String standard_name \"depth\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  DataQuality {\n"
            + "    Int32 _FillValue 9999;\n"
            + "    Int32 actual_range 0, 100;\n"
            + "    String description \"A post-processing quantitative data quality indicator.  Specifically, RDI percent-good #4, in earth coordinates (percentage of successful 4-beam transformations), expressed as a percentage, 50, 80, 95, etc.\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Data Quality\";\n"
            + "    String units \"%\";\n"
            + "  }\n"
            + "  DataQuality_flag {\n"
            + "    String _Unsigned \"false\";\n"
            + // ERDDAP adds
            "    Byte actual_range 0, 9;\n"
            + "    String description \"flag for data column, 0: no problems, 1: bad data due to malfunction or fouling, 2: suspicious data, 9: missing data\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Data Quality Flag\";\n"
            + "  }\n"
            + "  Eastward {\n"
            + "    Float64 _FillValue 9999.0;\n"
            + "    Float64 actual_range -0.966, 1.051;\n"
            + "    Float64 colorBarMaximum 0.5;\n"
            + "    Float64 colorBarMinimum -0.5;\n"
            + "    String description \"True eastward velocity measurements. Negative values represent westward velocities.\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Eastward Current\";\n"
            + "    String standard_name \"eastward_sea_water_velocity\";\n"
            + "    String units \"m s-1\";\n"
            + "  }\n"
            + "  Eastward_flag {\n"
            + "    String _Unsigned \"false\";\n"
            + // ERDDAP adds
            "    Byte actual_range 0, 9;\n"
            + "    String description \"flag for data column, 0: no problems, 1: bad data due to malfunction or fouling, 2: suspicious data, 9: missing data\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Eastward Flag\";\n"
            + "    String standard_name \"eastward_sea_water_velocity status_flag\";\n"
            + "  }\n"
            + "  ErrorVelocity {\n"
            + "    Float64 _FillValue 9999.0;\n"
            + "    Float64 actual_range -1.073, 1.122;\n"
            + "    String description \"The difference of two vertical velocities, each measured by an opposing pair of ADCP beams.\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Error Velocity\";\n"
            + "    String units \"m s-1\";\n"
            + "  }\n"
            + "  ErrorVelocity_flag {\n"
            + "    String _Unsigned \"false\";\n"
            + // ERDDAP adds
            "    Byte actual_range 0, 9;\n"
            + "    String description \"flag for data column, 0: no problems, 1: bad data due to malfunction or fouling, 2: suspicious data, 9: missing data\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Error Velocity Flag\";\n"
            + "  }\n"
            + "  Intensity {\n"
            + "    Int32 _FillValue 9999;\n"
            + "    Int32 actual_range 53, 228;\n"
            + "    String description \"ADCP echo intensity (or backscatter), in RDI counts.  This value represents the average of all 4 beams, rounded to the nearest whole number.\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Intensity\";\n"
            + "    String units \"count\";\n"
            + "  }\n"
            + "  Intensity_flag {\n"
            + "    String _Unsigned \"false\";\n"
            + // ERDDAP adds
            "    Byte actual_range 0, 9;\n"
            + "    String description \"flag for data column, 0: no problems, 1: bad data due to malfunction or fouling, 2: suspicious data, 9: missing data\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Intensity Flag\";\n"
            + "  }\n"
            + "  Northward {\n"
            + "    Float64 _FillValue 9999.0;\n"
            + "    Float64 actual_range -1.072, 1.588;\n"
            + "    Float64 colorBarMaximum 0.5;\n"
            + "    Float64 colorBarMinimum -0.5;\n"
            + "    String description \"True northward velocity measurements. Negative values represent southward velocities.\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Northward Current\";\n"
            + "    String standard_name \"northward_sea_water_velocity\";\n"
            + "    String units \"m s-1\";\n"
            + "  }\n"
            + "  Northward_flag {\n"
            + "    String _Unsigned \"false\";\n"
            + // ERDDAP adds
            "    Byte actual_range 0, 9;\n"
            + "    String description \"flag for data column, 0: no problems, 1: bad data due to malfunction or fouling, 2: suspicious data, 9: missing data\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Northward Flag\";\n"
            + "    String standard_name \"northward_sea_water_velocity status_flag\";\n"
            + "  }\n"
            + "  Upwards {\n"
            + "    Float64 _FillValue 9999.0;\n"
            + "    Float64 actual_range -0.405, 0.406;\n"
            + "    String description \"True upwards velocity measurements.  Negative values represent downward velocities.\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Upward Current\";\n"
            + "    String standard_name \"upward_sea_water_velocity\";\n"
            + "    String units \"m s-1\";\n"
            + "  }\n"
            + "  Upwards_flag {\n"
            + "    String _Unsigned \"false\";\n"
            + // ERDDAP adds
            "    Byte actual_range 0, 9;\n"
            + "    String description \"flag for data column, 0: no problems, 1: bad data due to malfunction or fouling, 2: suspicious data, 9: missing data\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Upwards Flag\";\n"
            + "    String standard_name \"upward_sea_water_velocity\";\n"
            + "  }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_data_type \"Point\";\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "    Float64 Easternmost_Easting -120.31121;\n"
            + "    String featureType \"Point\";\n"
            + "    Float64 geospatial_lat_max 34.04017;\n"
            + "    Float64 geospatial_lat_min 34.04017;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max -120.31121;\n"
            + "    Float64 geospatial_lon_min -120.31121;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    String history \"Created by the NCDDC PISCO ADCP Profile to converter on 2009/00/11 15:00 CST.\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

    // "2015-01-30T17:06:49Z
    // https://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/catalog.xml\n" +
    // "2015-01-30T17:06:49Z
    // http://localhost:8080/cwexperimental/griddap/testGridFromTable.das\";\n" +
    expected =
        "String infoUrl \"ftp://ftp.nodc.noaa.gov/nodc/archive/arc0006/0002039/1.1/about/WCOS_project_document_phaseI_20060317.pdf\";\n"
            + "    String institution \"NOAA NMSP\";\n"
            + "    String keywords \"adcp, atmosphere, circulation, coast, current, currents, data, depth, Earth Science > Oceans > Ocean Circulation > Ocean Currents, eastward, eastward_sea_water_velocity, eastward_sea_water_velocity status_flag, error, flag, height, identifier, intensity, nmsp, noaa, northward, northward_sea_water_velocity, northward_sea_water_velocity status_flag, observing, ocean, oceans, quality, sea, seawater, station, status, system, time, upward, upward_sea_water_velocity, upwards, velocity, water, wcos, west, west coast\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    Float64 Northernmost_Northing 34.04017;\n"
            + "    String sourceUrl \"https://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/catalog.xml\";\n"
            + "    Float64 Southernmost_Northing 34.04017;\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String subsetVariables \"longitude, latitude\";\n"
            + "    String summary \"The West Coast Observing System (WCOS) project provides access to temperature and currents data collected at four of the five National Marine Sanctuary sites, including Olympic Coast, Gulf of the Farallones, Monterey Bay, and Channel Islands. A semi-automated end-to-end data management system transports and transforms the data from source to archive, making the data acessible for discovery, access and analysis from multiple Internet points of entry.\n"
            + "\n"
            + "The stations (and their code names) are San Miguel North (BAY), Santa Rosa North (BEA), Cuyler Harbor (CUY), Pelican/Prisoners Area (PEL), San Miguel South (SMS), Santa Rosa South (SRS), Valley Anch (VAL).\";\n"
            + "    String time_coverage_end \"2005-03-02T18:32:40Z\";\n"
            + "    String time_coverage_start \"2004-11-30T19:52:40Z\";\n"
            + "    String title \"West Coast Observing System (WCOS) ADCP Currents Data\";\n"
            + "    String Version \"2\";\n"
            + "    Float64 Westernmost_Easting -120.31121;\n"
            + "  }\n"
            + "}\n";
    po = results.indexOf("String infoUrl");
    Test.ensureEqual(results.substring(po), expected, "\nresults=\n" + results);

    // get data subset -- big gap in axis#0
    // look in logs to see
    // * nOuterAxes=1 of 4 nOuterRequests=2
    // axis sizes: result=2, 1, 1, 19 dataset=66201, 1, 1, 19
    // strides=10, 1, 1, 1 gapAvValues=19, 19, 19, 1
    // gap=171=((stride=10)-1) * gapAvValue=19 >= gapThreshold=15
    String2.log("\nget .csv\n");
    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "Eastward[10:10:20][][][],Eastward_flag[10:10:20][][][],"
                + "Northward[10:10:20][][][],Northward_flag[10:10:20][][][]",
            testDir,
            edd.className() + "_gap0",
            ".csv");
    results = File2.directReadFrom88591File(testDir + tName);
    /*
     * from source eddTable
     * station,longitude,latitude,time,depth,Eastward,Eastward_flag,Northward,
     * Northward_flag
     * ,degrees_east,degrees_north,UTC,m,m s-1,,m s-1,
     * BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,16.2,NaN,9,NaN,9
     * BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,15.2,0.001,0,0.06,0
     * BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,14.2,-0.002,0,0.043,0
     * BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,13.2,-0.037,0,0.023,0
     * BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,12.2,-0.029,0,0.055,0
     * BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,11.2,-0.014,0,0.052,0
     * BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,10.2,-0.023,0,0.068,0
     * BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,9.2,-0.039,0,0.046,0
     * BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,8.2,-0.043,0,0.063,0
     * BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,7.2,-0.03,0,0.062,0
     * BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,6.2,-0.032,0,0.075,0
     * BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,5.2,-0.038,0,0.061,0
     * BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,4.2,-0.059,0,0.075,0
     * BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,3.2,-0.062,0,0.102,0
     * BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,2.2,-0.059,0,0.101,0
     * BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,1.2,-0.069,0,0.079,0
     * BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,0.2,NaN,9,NaN,9
     * BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,-0.8,NaN,9,NaN,9
     * BAYXXX,-120.31121,34.04017,2004-11-30T20:12:40Z,-1.8,NaN,9,NaN,9
     */
    expected =
        "time,latitude,longitude,depth,Eastward,Eastward_flag,Northward,Northward_flag\n"
            + "UTC,degrees_north,degrees_east,m,m s-1,,m s-1,\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,-1.8,NaN,9,NaN,9\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,-0.8,NaN,9,NaN,9\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,0.19999999999999996,NaN,9,NaN,9\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,1.2,-0.069,0,0.079,0\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,2.2,-0.059,0,0.101,0\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,3.2,-0.062,0,0.102,0\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,4.2,-0.059,0,0.075,0\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,5.2,-0.038,0,0.061,0\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,6.2,-0.032,0,0.075,0\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,7.2,-0.03,0,0.062,0\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,8.2,-0.043,0,0.063,0\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,9.2,-0.039,0,0.046,0\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,10.2,-0.023,0,0.068,0\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,11.2,-0.014,0,0.052,0\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,12.2,-0.029,0,0.055,0\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,13.2,-0.037,0,0.023,0\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,14.2,-0.002,0,0.043,0\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,15.2,0.001,0,0.06,0\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,16.2,NaN,9,NaN,9\n"
            +
            /*
             * from source eddTable
             * station,longitude,latitude,time,depth,Eastward,Eastward_flag,Northward,
             * Northward_flag
             * ,degrees_east,degrees_north,UTC,m,m s-1,,m s-1,
             * BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,16.2,NaN,9,NaN,9
             * BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,15.2,0.034,0,0.047,0
             * BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,14.2,0.033,0,0.037,0
             * BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,13.2,0.007,0,0.051,0
             * BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,12.2,-0.003,0,0.037,0
             * BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,11.2,-0.016,0,0.042,0
             * BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,10.2,-0.016,0,0.055,0
             * BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,9.2,-0.028,0,0.041,0
             * BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,8.2,-0.028,0,0.093,0
             * BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,7.2,-0.057,0,0.061,0
             * BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,6.2,-0.06,0,0.064,0
             * BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,5.2,-0.077,0,0.081,0
             * BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,4.2,-0.139,0,0.086,0
             * BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,3.2,-0.105,0,0.102,0
             * BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,2.2,-0.122,0,0.1,0
             * BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,1.2,-0.02,0,-0.065,0
             * BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,0.2,NaN,9,NaN,9
             * BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,-0.8,NaN,9,NaN,9
             * BAYXXX,-120.31121,34.04017,2004-11-30T20:32:40Z,-1.8,NaN,9,NaN,9
             */
            "2004-11-30T20:32:40Z,34.04017,-120.31121,-1.8,NaN,9,NaN,9\n"
            + "2004-11-30T20:32:40Z,34.04017,-120.31121,-0.8,NaN,9,NaN,9\n"
            + "2004-11-30T20:32:40Z,34.04017,-120.31121,0.19999999999999996,NaN,9,NaN,9\n"
            + "2004-11-30T20:32:40Z,34.04017,-120.31121,1.2,-0.02,0,-0.065,0\n"
            + "2004-11-30T20:32:40Z,34.04017,-120.31121,2.2,-0.122,0,0.1,0\n"
            + "2004-11-30T20:32:40Z,34.04017,-120.31121,3.2,-0.105,0,0.102,0\n"
            + "2004-11-30T20:32:40Z,34.04017,-120.31121,4.2,-0.139,0,0.086,0\n"
            + "2004-11-30T20:32:40Z,34.04017,-120.31121,5.2,-0.077,0,0.081,0\n"
            + "2004-11-30T20:32:40Z,34.04017,-120.31121,6.2,-0.06,0,0.064,0\n"
            + "2004-11-30T20:32:40Z,34.04017,-120.31121,7.2,-0.057,0,0.061,0\n"
            + "2004-11-30T20:32:40Z,34.04017,-120.31121,8.2,-0.028,0,0.093,0\n"
            + "2004-11-30T20:32:40Z,34.04017,-120.31121,9.2,-0.028,0,0.041,0\n"
            + "2004-11-30T20:32:40Z,34.04017,-120.31121,10.2,-0.016,0,0.055,0\n"
            + "2004-11-30T20:32:40Z,34.04017,-120.31121,11.2,-0.016,0,0.042,0\n"
            + "2004-11-30T20:32:40Z,34.04017,-120.31121,12.2,-0.003,0,0.037,0\n"
            + "2004-11-30T20:32:40Z,34.04017,-120.31121,13.2,0.007,0,0.051,0\n"
            + "2004-11-30T20:32:40Z,34.04017,-120.31121,14.2,0.033,0,0.037,0\n"
            + "2004-11-30T20:32:40Z,34.04017,-120.31121,15.2,0.034,0,0.047,0\n"
            + "2004-11-30T20:32:40Z,34.04017,-120.31121,16.2,NaN,9,NaN,9\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
    String2.log("\n*** EDDGridFromEDDTable.testBasic finished successfully\n");

    // get data subset -- big gap in axis#3
    // look in log to see:
    // * nOuterAxes=4 of 4 nOuterRequests=4
    // axis sizes: result=2, 1, 1, 2 dataset=66201, 1, 1, 19
    // strides=10, 1, 1, 17 gapAvValues=18, 18, 18, 1
    // gap=16=((stride=17)-1) * gapAvValue=1 >= gapThreshold=15
    String2.log("\nget .csv\n");
    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "Eastward[10:10:20][][][0:17:17],Eastward_flag[10:10:20][][][0:17:17],"
                + "Northward[10:10:20][][][0:17:17],Northward_flag[10:10:20][][][0:17:17]",
            testDir,
            edd.className() + "_gap3",
            ".csv");
    results = File2.directReadFrom88591File(testDir + tName);
    expected =
        "time,latitude,longitude,depth,Eastward,Eastward_flag,Northward,Northward_flag\n"
            + "UTC,degrees_north,degrees_east,m,m s-1,,m s-1,\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,-1.8,NaN,9,NaN,9\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,15.2,0.001,0,0.06,0\n"
            + "2004-11-30T20:32:40Z,34.04017,-120.31121,-1.8,NaN,9,NaN,9\n"
            + "2004-11-30T20:32:40Z,34.04017,-120.31121,15.2,0.034,0,0.047,0\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
    String2.log("\n*** EDDGridFromEDDTable.testBasic finished successfully\n");

    // get data subset -- no gap
    // look in log to see:
    // * nOuterAxes=0 of 4 nOuterRequests=1
    // axis sizes: result=1, 1, 1, 8 dataset=66201, 1, 1, 19
    // strides=1, 1, 1, 1 gapAvValues=8, 8, 8, 1
    String2.log("\nget .csv\n");
    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "Eastward[10][][][2:9],Eastward_flag[10][][][2:9],"
                + "Northward[10][][][2:9],Northward_flag[10][][][2:9]",
            testDir,
            edd.className() + "_nogap",
            ".csv");
    results = File2.directReadFrom88591File(testDir + tName);
    expected =
        "time,latitude,longitude,depth,Eastward,Eastward_flag,Northward,Northward_flag\n"
            + "UTC,degrees_north,degrees_east,m,m s-1,,m s-1,\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,0.19999999999999996,NaN,9,NaN,9\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,1.2,-0.069,0,0.079,0\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,2.2,-0.059,0,0.101,0\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,3.2,-0.062,0,0.102,0\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,4.2,-0.059,0,0.075,0\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,5.2,-0.038,0,0.061,0\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,6.2,-0.032,0,0.075,0\n"
            + "2004-11-30T20:12:40Z,34.04017,-120.31121,7.2,-0.03,0,0.062,0\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
    String2.log("\n*** EDDGridFromEDDTable.testBasic finished successfully\n");
  }

  /**
   * This tests the /files/ "files" system. This requires testGridFromTable in the localhost ERDDAP.
   */
  @org.junit.jupiter.api.Test
  @TagLocalERDDAP
  void testFiles() throws Throwable {

    // String2.log("\n*** EDDGridFromEDDTable.testFiles()\n");
    String tDir = EDStatic.fullTestCacheDirectory;
    String dapQuery, tName, start, query, results, expected;
    int po;

    try {
      // get /files/datasetID/.csv
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:8080/cwexperimental/files/testGridFromTable/.csv");
      expected =
          "Name,Last modified,Size,Description\n"
              + "2004/,NaN,NaN,\n"
              + "2005/,NaN,NaN,\n"
              + "2006/,NaN,NaN,\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // get /files/datasetID/
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:8080/cwexperimental/files/testGridFromTable/");
      Test.ensureTrue(results.indexOf("2004&#x2f;") > 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("2004/") > 0, "results=\n" + results);

      // get /files/datasetID/subdir/.csv
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:8080/cwexperimental/files/testGridFromTable/2005/.csv");
      expected =
          "Name,Last modified,Size,Description\n"
              + "BAYXXX_015ADCP015R00_20050302.nc,1337283378000,55283216,\n"
              + "BAYXXX_015ADCP015R00_20050613.nc,1337477650000,65127016,\n"
              + "BAYXXX_015ADCP015R00_20050929.nc,1337416274000,49365084,\n"
              + "BAYXXX_015ADCP015R00_20051215.nc,1337422794000,64554832,\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // download a file in root

      // download a file in subdir
      results =
          String2.annotatedString(
              SSR.getUrlResponseStringNewline(
                      "http://localhost:8080/cwexperimental/files/testGridFromTable/2005/BAYXXX_015ADCP015R00_20050613.nc")
                  .substring(0, 50));
      expected =
          "CDF[1][0][0][0][0][0][0][0][10]\n"
              + "[0][0][0][4][0][0][0][4]Time[0][1][22]N[0][0][0][5]Depth[0][0][0][0][0][0][19][0][0][0][8]La[end]";
      Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

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
                "http://localhost:8080/cwexperimental/files/testGridFromTable/gibberish/");
      } catch (Exception e) {
        results = e.toString();
      }
      expected =
          "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/testGridFromTable/gibberish/\n"
              + "(Error {\n"
              + "    code=404;\n"
              + "    message=\"Not Found: Resource not found: directory=gibberish/\";\n"
              + "})";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // try to download a non-existent file
      try {
        results =
            SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/testGridFromTable/gibberish.csv");
      } catch (Exception e) {
        results = e.toString();
      }
      expected =
          "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/testGridFromTable/gibberish.csv\n"
              + "(Error {\n"
              + "    code=404;\n"
              + "    message=\"Not Found: File not found: gibberish.csv .\";\n"
              + "})";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // try to download a non-existent file in existant subdir
      try {
        results =
            SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/testGridFromTable/2005/gibberish.csv");
      } catch (Exception e) {
        results = e.toString();
      }
      expected =
          "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/testGridFromTable/2005/gibberish.csv\n"
              + "(Error {\n"
              + "    code=404;\n"
              + "    message=\"Not Found: File not found: gibberish.csv .\";\n"
              + "})";
      Test.ensureEqual(results, expected, "results=\n" + results);

    } catch (Throwable t) {
      throw new RuntimeException(
          "Unexpected error. This test requires testGridFromTable in the localhost ERDDAP.", t);
    }
  }
}
