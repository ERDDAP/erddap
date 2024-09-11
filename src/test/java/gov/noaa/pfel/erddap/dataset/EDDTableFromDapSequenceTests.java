package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import dods.dap.BaseType;
import dods.dap.DAS;
import dods.dap.DBoolean;
import dods.dap.DByte;
import dods.dap.DConnect;
import dods.dap.DDS;
import dods.dap.DFloat32;
import dods.dap.DFloat64;
import dods.dap.DInt16;
import dods.dap.DInt32;
import dods.dap.DSequence;
import dods.dap.DString;
import dods.dap.DUInt16;
import dods.dap.DUInt32;
import dods.dap.DataDDS;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import org.junit.jupiter.api.BeforeAll;
import tags.TagExternalERDDAP;
import tags.TagLocalERDDAP;
import tags.TagMissingDataset;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

class EDDTableFromDapSequenceTests {

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /** testGenerateDatasetsXml */
  @org.junit.jupiter.api.Test
  @TagMissingDataset // connection can't be opened
  void testGenerateDatasetsXml() throws Throwable {
    // String2.log("\n*** EDDTableFromDapSequence.testGenerateDatasetsXml\n" +
    // "This requires testNccsvScalar in localhost ERDDAP.\n");
    // testVerboseOn();

    String tUrl = "http://cimt.dyndns.org:8080/dods/drds/vCTD";

    String expected =
        "<dataset type=\"EDDTableFromDapSequence\" datasetID=\"dyndns_cimt_8cad_5f3b_717e\" active=\"true\">\n"
            + "    <sourceUrl>http://cimt.dyndns.org:8080/dods/drds/vCTD</sourceUrl>\n"
            + "    <outerSequenceName>vCTD</outerSequenceName>\n"
            + "    <skipDapperSpacerRows>false</skipDapperSpacerRows>\n"
            + "    <sourceCanConstrainStringEQNE>true</sourceCanConstrainStringEQNE>\n"
            + "    <sourceCanConstrainStringGTLT>true</sourceCanConstrainStringGTLT>\n"
            + "    <sourceCanConstrainStringRegex></sourceCanConstrainStringRegex>\n"
            + "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n"
            + "    <!-- sourceAttributes>\n"
            + "    </sourceAttributes -->\n"
            + "    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n"
            + "        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n"
            + "        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n"
            + "    -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cdm_data_type\">Other</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
            + "        <att name=\"creator_name\">DYNDNS CIMT</att>\n"
            + "        <att name=\"creator_type\">institution</att>\n"
            + "        <att name=\"creator_url\">http://cimt.dyndns.org:8080/dods/drds/vCTD</att>\n"
            + "        <att name=\"infoUrl\">http://cimt.dyndns.org:8080/dods/drds/vCTD</att>\n"
            + "        <att name=\"institution\">DYNDNS CIMT</att>\n"
            + "        <att name=\"keywords\">acceleration, anomaly, average, avg_sound_velocity, center, cimt, cimt.dyndns.org, currents, data, density, depth, dods, drds, dyndns, earth, Earth Science &gt; Oceans &gt; Salinity/Density &gt; Salinity, fluorescence, geopotential, geopotential_anomaly, identifier, integrated, latitude, longitude, marine, ocean, oceans, optical, optical properties, practical, properties, salinity, science, sea, sea_water_practical_salinity, seawater, sigma, sigma_t, sound, station, technology, temperature, time, time2, vctd, vctd.das, velocity, water</att>\n"
            + "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n"
            + "        <att name=\"license\">[standard]</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"subsetVariables\">time2, latitude, longitude, station, depth, temperature, salinity, fluorescence, avg_sound_velocity, sigma_t, acceleration, geopotential_anomaly</att>\n"
            + "        <att name=\"summary\">vCTD. DYNDNS Center for Integrated Marine Technology (CIMT) data from http://cimt.dyndns.org:8080/dods/drds/vCTD.das .</att>\n"
            + "        <att name=\"title\">vCTD. DYNDNS CIMT data from http://cimt.dyndns.org:8080/dods/drds/vCTD.das .</att>\n"
            + "    </addAttributes>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>time</sourceName>\n"
            + "        <destinationName>time2</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"Description\">A date-time string</att>\n"
            + "            <att name=\"Timezone\">GMT</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"long_name\">Time</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>latitude</sourceName>\n"
            + "        <destinationName>latitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"Description\">Latitude as recorded by GPS</att>\n"
            + "            <att name=\"units\">degrees_north</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Latitude</att>\n"
            + "            <att name=\"standard_name\">latitude</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>longitude</sourceName>\n"
            + "        <destinationName>longitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"Description\">Longitude as recorded by GPS</att>\n"
            + "            <att name=\"units\">degrees_east</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Longitude</att>\n"
            + "            <att name=\"standard_name\">longitude</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>station</sourceName>\n"
            + "        <destinationName>station</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"Description\">CIMT Station ID</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "            <att name=\"long_name\">Station</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>depth</sourceName>\n"
            + "        <destinationName>depth</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"Constants\">salt water, lat=36.9</att>\n"
            + "            <att name=\"Description\">Binned depth from the CTD</att>\n"
            + "            <att name=\"units\">meters</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">8000.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-8000.0</att>\n"
            + "            <att name=\"colorBarPalette\">TopographyDepth</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Depth</att>\n"
            + "            <att name=\"standard_name\">depth</att>\n"
            + "            <att name=\"units\">m</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>temperature</sourceName>\n"
            + "        <destinationName>temperature</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"Description\">Temperature at depth, ITS-90</att>\n"
            + "            <att name=\"units\">degrees_Celsius</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">40.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-10.0</att>\n"
            + "            <att name=\"ioos_category\">Temperature</att>\n"
            + "            <att name=\"long_name\">Temperature</att>\n"
            + "            <att name=\"units\">degree_C</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>salinity</sourceName>\n"
            + "        <destinationName>salinity</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"Description\">Salinity at depth derived from conductivity</att>\n"
            + "            <att name=\"units\">Presumed Salinity Units</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">37.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">32.0</att>\n"
            + "            <att name=\"ioos_category\">Salinity</att>\n"
            + "            <att name=\"long_name\">Sea Water Practical Salinity</att>\n"
            + "            <att name=\"standard_name\">sea_water_practical_salinity</att>\n"
            + "            <att name=\"units\">PSU</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>fluorescence</sourceName>\n"
            + "        <destinationName>fluorescence</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"Description\">Fluorescence at depth from the WETStar</att>\n"
            + "            <att name=\"units\">nominal mg Chl m/^-3</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Optical Properties</att>\n"
            + "            <att name=\"long_name\">Fluorescence</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>avg_sound_velocity</sourceName>\n"
            + "        <destinationName>avg_sound_velocity</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"Constants\">Chen-Millero, minP=20, minS=20, pWS=20, tWS=60</att>\n"
            + "            <att name=\"Description\">Average sound velocity at depth derived from temperature and pressure</att>\n"
            + "            <att name=\"units\">m/s</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Currents</att>\n"
            + "            <att name=\"long_name\">Avg Sound Velocity</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>sigma_t</sourceName>\n"
            + "        <destinationName>sigma_t</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"Description\">Density (sigma-theta)</att>\n"
            + "            <att name=\"units\">Kg/m^-3</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Sigma T</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>acceleration</sourceName>\n"
            + "        <destinationName>acceleration</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"Constants\">WS=2</att>\n"
            + "            <att name=\"Description\">Acceleration</att>\n"
            + "            <att name=\"units\">m/s^2</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Acceleration</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>geopotential_anomaly</sourceName>\n"
            + "        <destinationName>geopotential_anomaly</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"Description\">Geopotential Anomaly</att>\n"
            + "            <att name=\"units\">J/Kg</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-10.0</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Geopotential Anomaly</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n\n";
    String results = EDDTableFromDapSequence.generateDatasetsXml(tUrl, 1440, null) + "\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // GenerateDatasetsXml
    String gdxResults =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {
                  "-verbose", "EDDTableFromDapSequence", tUrl, "1440", "-1"
                }, // defaultStandardizeWhat
                false); // doIt loop?
    Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

    // EDDGridFromDap should fail and try EDDTableFromDapSequence and generate same
    // result
    results = EDDGridFromDap.generateDatasetsXml(tUrl, null, null, null, 1440, null) + "\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // ensure it is ready-to-use by making a dataset from it
    /*
     * This fails because time variable has no units.
     * String tDatasetID = "dyndns_cimt_8cad_5f3b_717e";
     * EDD.deleteCachedDatasetInfo(tDatasetID);
     * EDD edd = oneFromXmlFragment(null, results);
     * Test.ensureEqual(edd.datasetID(), tDatasetID, "");
     * Test.ensureEqual(edd.title(), "zztop", "");
     * Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()),
     * "zztop",
     * "");
     */
  }

  /** testGenerateDatasetsXml */
  @org.junit.jupiter.api.Test
  @TagLocalERDDAP
  void testGenerateDatasetsXml2() throws Throwable {
    // String2.log("\n*** EDDTableFromDapSequence.testGenerateDatasetsXml2\n" +
    // "This requires testNccsvScalar11 in localhost ERDDAP.\n");
    // testVerboseOn();

    try {

      String tUrl =
          "http://localhost:8080/cwexperimental/tabledap/testNccsvScalar11.html"; // test that it
      // removes
      // .html
      String results = EDDTableFromDapSequence.generateDatasetsXml(tUrl, 1440, null) + "\n";

      String expected =
          "<dataset type=\"EDDTableFromDapSequence\" datasetID=\"localhost_6f85_6209_a3de\" active=\"true\">\n"
              + "    <sourceUrl>http://localhost:8080/cwexperimental/tabledap/testNccsvScalar11</sourceUrl>\n"
              + "    <outerSequenceName>s</outerSequenceName>\n"
              + "    <skipDapperSpacerRows>false</skipDapperSpacerRows>\n"
              + "    <sourceCanConstrainStringEQNE>true</sourceCanConstrainStringEQNE>\n"
              + "    <sourceCanConstrainStringGTLT>true</sourceCanConstrainStringGTLT>\n"
              + "    <sourceCanConstrainStringRegex></sourceCanConstrainStringRegex>\n"
              + "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n"
              + "    <!-- sourceAttributes>\n"
              + "        <att name=\"cdm_data_type\">Trajectory</att>\n"
              + "        <att name=\"cdm_trajectory_variables\">ship</att>\n"
              + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
              + "        <att name=\"creator_email\">bob.simons@noaa.gov</att>\n"
              + "        <att name=\"creator_name\">Bob Simons</att>\n"
              + "        <att name=\"creator_type\">person</att>\n"
              + "        <att name=\"creator_url\">https://www.pfeg.noaa.gov</att>\n"
              + "        <att name=\"Easternmost_Easting\" type=\"double\">-130.2576</att>\n"
              + "        <att name=\"featureType\">Trajectory</att>\n"
              + "        <att name=\"geospatial_lat_max\" type=\"double\">28.0003</att>\n"
              + "        <att name=\"geospatial_lat_min\" type=\"double\">27.9998</att>\n"
              + "        <att name=\"geospatial_lat_units\">degrees_north</att>\n"
              + "        <att name=\"geospatial_lon_max\" type=\"double\">-130.2576</att>\n"
              + "        <att name=\"geospatial_lon_min\" type=\"double\">-132.1591</att>\n"
              + "        <att name=\"geospatial_lon_units\">degrees_east</att>\n"
              + "        <att name=\"history\">";
      Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

      // 2017-05-05T16:27:08Z (local files)
      // 2017-05-05T16:27:08Z
      // "http://localhost:8080/cwexperimental/tabledap/testNccsvScalar.das</att>\n" +
      expected =
          "        <att name=\"infoUrl\">https://erddap.github.io/NCCSV.html</att>\n"
              + "        <att name=\"institution\">NOAA NMFS SWFSC ERD, NOAA PMEL</att>\n"
              + "        <att name=\"keywords\">center, data, demonstration, Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature, environmental, erd, fisheries, identifier, laboratory, latitude, long, longitude, marine, national, nccsv, nmfs, noaa, ocean, oceans, pacific, pmel, science, sea, sea_surface_temperature, service, ship, southwest, sst, status, surface, swfsc, temperature, test, testLong, time, trajectory</att>\n"
              + "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n"
              + "        <att name=\"license\">&quot;NCCSV Demonstration&quot; by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .</att>\n"
              + "        <att name=\"Northernmost_Northing\" type=\"double\">28.0003</att>\n"
              + "        <att name=\"sourceUrl\">(local files)</att>\n"
              + "        <att name=\"Southernmost_Northing\" type=\"double\">27.9998</att>\n"
              + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v55</att>\n"
              + "        <att name=\"subsetVariables\">ship, status, testLong</att>\n"
              + "        <att name=\"summary\">This is a paragraph or two describing the dataset.</att>\n"
              + "        <att name=\"time_coverage_end\">2017-03-23T23:45:00Z</att>\n"
              + "        <att name=\"time_coverage_start\">2017-03-23T00:45:00Z</att>\n"
              + "        <att name=\"title\">NCCSV Demonstration</att>\n"
              + "        <att name=\"Westernmost_Easting\" type=\"double\">-132.1591</att>\n"
              + "    </sourceAttributes -->\n"
              + "    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n"
              + "        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n"
              + "        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n"
              + "    -->\n"
              + "    <addAttributes>\n"
              + "        <att name=\"keywords\">byte, center, data, demonstration, earth, Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature, environmental, erd, fisheries, identifier, laboratory, latitude, long, longitude, longs, marine, national, nccsv, nmfs, noaa, ocean, oceans, pacific, pmel, science, sea, sea_surface_temperature, service, ship, southwest, sst, status, surface, swfsc, temperature, test, testByte, testlong, testnccsvscalar11, testUByte, testULong, time, trajectory, ubyte, ulong</att>\n"
              + "        <att name=\"subsetVariables\">ship, time, latitude, longitude, status, testByte, testUByte, testLong, testULong, sst</att>\n"
              + "        <att name=\"title\">NCCSV Demonstration (testNccsvScalar11)</att>\n"
              + "    </addAttributes>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>ship</sourceName>\n"
              + "        <destinationName>ship</destinationName>\n"
              + "        <!-- sourceAttributes>\n"
              + "            <att name=\"cf_role\">trajectory_id</att>\n"
              + "            <att name=\"ioos_category\">Identifier</att>\n"
              + "            <att name=\"long_name\">Ship</att>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>time</sourceName>\n"
              + "        <destinationName>time</destinationName>\n"
              + "        <!-- sourceAttributes>\n"
              + "            <att name=\"_CoordinateAxisType\">Time</att>\n"
              + "            <att name=\"actual_range\" type=\"doubleList\">1.4902299E9 1.4903127E9</att>\n"
              + "            <att name=\"axis\">T</att>\n"
              + "            <att name=\"ioos_category\">Time</att>\n"
              + "            <att name=\"long_name\">Time</att>\n"
              + "            <att name=\"standard_name\">time</att>\n"
              + "            <att name=\"time_origin\">01-JAN-1970 00:00:00</att>\n"
              + "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"colorBarMaximum\" type=\"double\">1.49032E9</att>\n"
              + "            <att name=\"colorBarMinimum\" type=\"double\">1.49022E9</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>latitude</sourceName>\n"
              + "        <destinationName>latitude</destinationName>\n"
              + "        <!-- sourceAttributes>\n"
              + "            <att name=\"_CoordinateAxisType\">Lat</att>\n"
              + "            <att name=\"actual_range\" type=\"doubleList\">27.9998 28.0003</att>\n"
              + "            <att name=\"axis\">Y</att>\n"
              + "            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n"
              + "            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n"
              + "            <att name=\"ioos_category\">Location</att>\n"
              + "            <att name=\"long_name\">Latitude</att>\n"
              + "            <att name=\"standard_name\">latitude</att>\n"
              + "            <att name=\"units\">degrees_north</att>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>longitude</sourceName>\n"
              + "        <destinationName>longitude</destinationName>\n"
              + "        <!-- sourceAttributes>\n"
              + "            <att name=\"_CoordinateAxisType\">Lon</att>\n"
              + "            <att name=\"actual_range\" type=\"doubleList\">-132.1591 -130.2576</att>\n"
              + "            <att name=\"axis\">X</att>\n"
              + "            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n"
              + "            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n"
              + "            <att name=\"ioos_category\">Location</att>\n"
              + "            <att name=\"long_name\">Longitude</att>\n"
              + "            <att name=\"standard_name\">longitude</att>\n"
              + "            <att name=\"units\">degrees_east</att>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>status</sourceName>\n"
              + "        <destinationName>status</destinationName>\n"
              + "        <!-- sourceAttributes>\n"
              + "            <att name=\"actual_range\">?</att>\n"
              + // info not transmitted correctly by DAP
              "            <att name=\"comment\">From http://some.url.gov/someProjectDocument , Table C</att>\n"
              + "            <att name=\"ioos_category\">Unknown</att>\n"
              + "            <att name=\"long_name\">Status</att>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"actual_range\">null</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>testByte</sourceName>\n"
              + "        <destinationName>testByte</destinationName>\n"
              + "        <!-- sourceAttributes>\n"
              + "            <att name=\"_FillValue\" type=\"byte\">127</att>\n"
              + "            <att name=\"_Unsigned\">false</att>\n"
              + // erddap puts this to say "actually it is signed"
              "            <att name=\"actual_range\" type=\"byteList\">-128 126</att>\n"
              + "            <att name=\"ioos_category\">Unknown</att>\n"
              + "            <att name=\"units\">1</att>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"colorBarMaximum\" type=\"double\">200.0</att>\n"
              + "            <att name=\"colorBarMinimum\" type=\"double\">-200.0</att>\n"
              + "            <att name=\"long_name\">Test Byte</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>testUByte</sourceName>\n"
              + "        <destinationName>testUByte</destinationName>\n"
              + "        <!-- sourceAttributes>\n"
              +
              // " <att name=\"_FillValue\" type=\"byte\">-1</att>\n" + //source is (erd)dap,
              // which treats bytes as signed
              // " <att name=\"actual_range\" type=\"byteList\">0 -2</att>\n" +
              // but ERDDAP converts source when read
              "            <att name=\"_FillValue\" type=\"ubyte\">255</att>\n"
              + "            <att name=\"_Unsigned\">true</att>\n"
              + // erddap puts this to say "no, actually it is unsigned"
              "            <att name=\"actual_range\" type=\"ubyteList\">0 254</att>\n"
              + "            <att name=\"ioos_category\">Unknown</att>\n"
              + "            <att name=\"units\">1</att>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"colorBarMaximum\" type=\"double\">300.0</att>\n"
              + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
              + "            <att name=\"long_name\">Test UByte</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>testLong</sourceName>\n"
              + "        <destinationName>testLong</destinationName>\n"
              + "        <!-- sourceAttributes>\n"
              + "            <att name=\"_FillValue\" type=\"double\">9.223372036854776E18</att>\n"
              + // long vars appear as
              // double vars in DAP
              // these are the largest longs, converted to doubles
              "            <att name=\"actual_range\" type=\"doubleList\">-9.223372036854776E18 9.223372036854776E18</att>\n"
              + // trouble
              "            <att name=\"ioos_category\">Unknown</att>\n"
              + "            <att name=\"long_name\">Test of Longs</att>\n"
              + "            <att name=\"units\">1</att>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"colorBarMaximum\" type=\"double\">1.0E19</att>\n"
              + "            <att name=\"colorBarMinimum\" type=\"double\">-1.0E19</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>testULong</sourceName>\n"
              + "        <destinationName>testULong</destinationName>\n"
              + "        <!-- sourceAttributes>\n"
              + "            <att name=\"_FillValue\" type=\"double\">1.8446744073709552E19</att>\n"
              + "            <att name=\"actual_range\" type=\"doubleList\">0.0 1.8446744073709552E19</att>\n"
              + "            <att name=\"ioos_category\">Unknown</att>\n"
              + "            <att name=\"long_name\">Test ULong</att>\n"
              + "            <att name=\"units\">1</att>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"colorBarMaximum\" type=\"double\">2.0E19</att>\n"
              + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>sst</sourceName>\n"
              + "        <destinationName>sst</destinationName>\n"
              + "        <!-- sourceAttributes>\n"
              + "            <att name=\"actual_range\" type=\"floatList\">10.0 10.9</att>\n"
              + "            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n"
              + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
              + "            <att name=\"ioos_category\">Temperature</att>\n"
              + "            <att name=\"long_name\">Sea Surface Temperature</att>\n"
              + "            <att name=\"missing_value\" type=\"float\">99.0</att>\n"
              + "            <att name=\"standard_name\">sea_surface_temperature</att>\n"
              + "            <att name=\"testBytes\" type=\"byteList\">-128 0 127</att>\n"
              + "            <att name=\"testChars\">,\n"
              + "&quot;\n"
              + "?</att>\n"
              + // test of \\u20ac
              "            <att name=\"testDoubles\" type=\"doubleList\">-1.7976931348623157E308 0.0 1.7976931348623157E308</att>\n"
              +
              // ??? !!! Unlike Java parseFloat, JDAP reads+/-3.40282345E38 as NaN. !!!
              // Hence NaNs here. This is an unfixed bug (hopefully won't ever affect anyone).
              "            <att name=\"testFloats\" type=\"floatList\">NaN 0.0 NaN</att>\n"
              + "            <att name=\"testInts\" type=\"intList\">-2147483648 0 2147483647</att>\n"
              + "            <att name=\"testLongs\" type=\"doubleList\">-9.223372036854776E18 -9.007199254740992E15 9.007199254740992E15 9.223372036854776E18 9.223372036854776E18</att>\n"
              + "            <att name=\"testShorts\" type=\"shortList\">-32768 0 32767</att>\n"
              + "            <att name=\"testStrings\">a&#9;~\u00fc,\n"
              + "&#39;z&quot;?</att>\n"
              + "            <att name=\"testUBytes\" type=\"byteList\">0 127 -1</att>\n"
              + // var is _Unsigned=true and isn't
              // common un/signed att name, so
              // no way to know it should be
              // ubytes
              "            <att name=\"testUInts\" type=\"uintList\">0 2147483647 4294967295</att>\n"
              + "            <att name=\"testULongs\" type=\"doubleList\">0.0 9.223372036854776E18 1.8446744073709552E19</att>\n"
              + // long atts appear as double atts
              "            <att name=\"testUShorts\" type=\"ushortList\">0 32767 65535</att>\n"
              + "            <att name=\"units\">degree_C</att>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "</dataset>\n"
              + "\n\n";
      int po = results.indexOf(expected.substring(0, 60));
      Test.ensureEqual(results.substring(po), expected, "results=\n" + results);

      // ensure it is ready-to-use by making a dataset from it
      String tDatasetID = "localhost_6f85_6209_a3de";
      EDD.deleteCachedDatasetInfo(tDatasetID);
      EDD edd = EDDTableFromDapSequence.oneFromXmlFragment(null, results);
      Test.ensureEqual(edd.datasetID(), tDatasetID, "");
      Test.ensureEqual(edd.title(), "NCCSV Demonstration (testNccsvScalar11)", "");
      Test.ensureEqual(
          String2.toCSSVString(edd.dataVariableDestinationNames()),
          "ship, time, latitude, longitude, status, testByte, testUByte, testLong, testULong, sst",
          "");
    } catch (Throwable t) {
      throw new RuntimeException(
          "This test requires datasetID=testNccsvScalar in localhost ERDDAP.", t);
    }
  }

  @org.junit.jupiter.api.Test
  @TagMissingDataset // don't have a definition for pmelArgoAll
  void testOneTime() throws Throwable {
    // testVerboseOn();
    String tName;
    int language = 0;

    if (true) {
      // get empiricalMinMax
      EDDTable tedd = (EDDTable) EDDTableFromDapSequence.oneFromDatasetsXml(null, "pmelArgoAll");
      tedd.getEmpiricalMinMax(language, null, "2007-08-01", "2007-08-10", false, true);
      String tq =
          "longitude,latitude,id&time>=2008-06-17T16:04:12Z&time<=2008-06-24T16:04:12Z"
              + "&.draw=markers&.marker=5|5&.color=0x000000&.colorBar=|C|Linear|||";
      tName =
          tedd.makeNewFileForDapQuery(
              language,
              null,
              null,
              tq,
              EDStatic.fullTestCacheDirectory,
              tedd.className() + "_GraphArgo",
              ".png");
      // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    }

    if (false) {
      // get summary string
      EDDTable tedd =
          (EDDTable) EDDTableFromDapSequence.oneFromDatasetsXml(null, "nwioosGroundfish");
      String2.log(String2.annotatedString(tedd.combinedGlobalAttributes().getString("summary")));
    }

    if (false) {
      // graph colorbar range
      EDDTable tedd = (EDDTable) EDDTableFromDapSequence.oneFromDatasetsXml(null, "pmelArgoAll");
      String tq =
          "longitude,latitude,temp_adjusted&time>=2008-06-27T00:00:00Z"
              + "&.draw=markers&.marker=5|5&.color=0x000000&.colorBar=|C|Linear|0|30|30";
      tName =
          tedd.makeNewFileForDapQuery(
              language,
              null,
              null,
              tq,
              EDStatic.fullTestCacheDirectory,
              tedd.className() + "_GraphArgo30",
              ".png");
      Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    }
  }

  /** Try to isolate trouble with Argo. */
  @org.junit.jupiter.api.Test
  @TagMissingDataset // don't have a definition for pmelArgoAll
  void testArgo() throws Throwable {
    // testVerboseOn();
    int language = 0;
    String sourceUrl = "http://dapper.pmel.noaa.gov/dapper/argo/argo_all.cdp"; // no longer running
    String2.log("\n*** EDDTableFromDapSequence.testArgo " + sourceUrl);
    DConnect dConnect = new DConnect(sourceUrl, EDDTableFromDapSequence.acceptDeflate, 1, 1);
    String2.log("getDAS");
    DAS das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
    String2.log("getDDS");
    DDS dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);

    EDDTable tedd = (EDDTable) EDDTableFromDapSequence.oneFromDatasetsXml(null, "pmelArgoAll");
    String tq =
        "longitude,latitude,id&id<=1000000&.draw=markers&.marker=4|5&.color=0x000000&.colorBar=|C|Linear|||";
    String tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            tq,
            EDStatic.fullTestCacheDirectory,
            tedd.className() + "_Argo",
            ".png");
    Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            tq,
            EDStatic.fullTestCacheDirectory,
            tedd.className() + "_Argo",
            ".csv");
    String results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    // String expected =
    // "";
    // Test.ensureEqual(results, expected, "results=\n" + results);

  }

  /**
   * Try to isolate trouble with psdac for Peter Piatko. Trouble is with source time that has
   * internal spaces -- erddap needs to percentEncode the request.
   */
  @org.junit.jupiter.api.Test
  @TagMissingDataset // connection timeout
  void testPsdac() throws Throwable {
    // testVerboseOn();
    int language = 0;
    String results, query, tName;
    String baseQuery =
        "time,longitude,latitude,depth,station,waterTemperature,salinity" + "&latitude=36.692";
    EDDTable tedd = (EDDTable) EDDTestDataset.getcimtPsdac();
    String expected =
        "time,longitude,latitude,depth,station,waterTemperature,salinity\n"
            + "UTC,degrees_east,degrees_north,m,,degree_C,Presumed Salinity Units\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,1.0,T402,12.8887,33.8966\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,2.0,T402,12.8272,33.8937\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,3.0,T402,12.8125,33.8898\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,4.0,T402,12.7125,33.8487\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,5.0,T402,12.4326,33.8241\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,6.0,T402,12.1666,33.8349\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,7.0,T402,11.9364,33.8159\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,8.0,T402,11.7206,33.8039\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,9.0,T402,11.511,33.8271\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,10.0,T402,11.4064,33.853\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,11.0,T402,11.3552,33.8502\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,12.0,T402,11.2519,33.8607\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,13.0,T402,11.1777,33.8655\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,14.0,T402,11.1381,33.8785\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,15.0,T402,11.0643,33.8768\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,16.0,T402,10.9416,33.8537\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,17.0,T402,10.809,33.8379\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,18.0,T402,10.7034,33.8593\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,19.0,T402,10.6502,33.8476\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,20.0,T402,10.5257,33.8174\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,21.0,T402,10.2857,33.831\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,22.0,T402,10.0717,33.8511\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,23.0,T402,9.9577,33.8557\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,24.0,T402,9.8876,33.8614\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,25.0,T402,9.842,33.8757\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,26.0,T402,9.7788,33.8904\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,27.0,T402,9.7224,33.8982\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,28.0,T402,9.695,33.9038\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,29.0,T402,9.6751,33.9013\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,30.0,T402,9.6462,33.9061\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,31.0,T402,9.6088,33.9069\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,32.0,T402,9.5447,33.9145\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,33.0,T402,9.4887,33.9263\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,34.0,T402,9.4514,33.9333\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,35.0,T402,9.4253,33.9358\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,36.0,T402,9.397,33.9387\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,37.0,T402,9.3795,33.9479\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,38.0,T402,9.3437,33.9475\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,39.0,T402,9.2946,33.9494\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,40.0,T402,9.2339,33.9458\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,41.0,T402,9.1812,33.9468\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,42.0,T402,9.153,33.9548\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,43.0,T402,9.1294,33.9615\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,44.0,T402,9.1048,33.9652\n"
            + "2002-06-25T14:55:00Z,-121.845,36.692,45.0,T402,9.0566,33.9762\n";

    // the basicQuery
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            baseQuery,
            EDStatic.fullTestCacheDirectory,
            tedd.className() + "_psdac",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    Test.ensureEqual(results, expected, "results=\n" + results);

    // basicQuery + String= constraint that shouldn't change the results
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            baseQuery + "&station=\"T402\"",
            EDStatic.fullTestCacheDirectory,
            tedd.className() + "_psdacNonTime",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    Test.ensureEqual(results, expected, "results=\n" + results);

    // basicQuery + String> String< constraints that shouldn't change the results
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            baseQuery + "&station>\"T3\"&station<\"T5\"",
            EDStatic.fullTestCacheDirectory,
            tedd.className() + "_psdacGTLT",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    Test.ensureEqual(results, expected, "results=\n" + results);

    // REGEX: If dataset is setup with sourceCanConstraintStringRegex ~=, THIS WORKS
    // SO SOURCE REGEX PARTLY WORKS
    // basicQuery + String regex constraint (ERDDAP handles it) that shouldn't
    // change the results
    // This succeeds with source not handling regex, so leave test active.
    // always =~ (regardless of what source needs) because this is an erddap request
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            baseQuery + "&station=~\"T40.\"",
            EDStatic.fullTestCacheDirectory,
            tedd.className() + "_psdacRegex",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    Test.ensureEqual(results, expected, "results=\n" + results);

    // REGEX: If dataset is setup with sourceCanConstraintStringRegex ~=, THIS
    // DOESN'T WORK.
    // SO SOURCE REGEX SUPPORT IS LIMITED, SO DON'T RELY ON SOURCE HANDLING REGEX
    // basicQuery + String regex constraint (ERDDAP handles it) that shouldn't
    // change the results
    // This succeeds with source not handling regex, so leave test active.
    // always =~ (regardless of what source needs) because this is an erddap request
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            baseQuery + "&station=~\"(T402|t403)\"",
            EDStatic.fullTestCacheDirectory,
            tedd.className() + "_psdacRegex",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    Test.ensureEqual(results, expected, "results=\n" + results);

    // basicQuery + time= (a string= test) constraint that shouldn't change the
    // results
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            baseQuery + "&time=2002-06-25T14:55:00Z",
            EDStatic.fullTestCacheDirectory,
            tedd.className() + "_psdacTime",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** */
  @org.junit.jupiter.api.Test
  @TagMissingDataset // no definition for erdlasNewportCtd
  void testErdlasNewportCtd() throws Throwable {
    // testVerboseOn();
    int language = 0;
    String results, query, tName, expected;
    String baseQuery = "&time>=2006-08-07T00&time<2006-08-08";
    EDDTable tedd = (EDDTable) EDDTableFromDapSequence.oneFromDatasetsXml(null, "erdlasNewportCtd");

    // the basicQuery
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            baseQuery,
            EDStatic.fullTestCacheDirectory,
            tedd.className() + "_newport",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected = "\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** NOT FINISHED. */
  @org.junit.jupiter.api.Test
  @TagMissingDataset // Connection can't be opened
  void testDapErdlasNewportCtd() throws Throwable {
    // testVerboseOn();
    int language = 0;

    // the basicQuery
    for (int test = 1; test < 2; test++) {
      String url =
          test == 0
              ? "https://oceanwatch.pfeg.noaa.gov:8080/dods/GLOBEC/GLOBEC_birds?birds.year,birds.species,birds.head_c,birds.month_local,birds.day_local&birds.year=2000&birds.month_local=8&birds.day_local=7"
              : "http://las.pfeg.noaa.gov/cgi-bin/ERDserver/northwest.sql?northwest.temperature,northwest.ctd_station_code,northwest.datetime,northwest.station,northwest.longitude,northwest.latitude&northwest.datetime%3E13821";
      System.out.println("\ntesting url=" + url);
      DConnect dConnect = new DConnect(url, true);
      DataDDS dataDds = dConnect.getData(null); // null = no statusUI

      // *** read the data (row-by-row, as it wants)
      DSequence outerSequence = (DSequence) dataDds.getVariables().nextElement();
      int nOuterRows = outerSequence.getRowCount();
      System.out.println("nRows=" + nOuterRows);
      for (int outerRow = 0; outerRow < Math.min(5, nOuterRows); outerRow++) {
        java.util.Vector outerVector = outerSequence.getRow(outerRow);
        StringBuilder sb = new StringBuilder();

        // process the other outerCol
        for (int outerCol = 0; outerCol < outerVector.size(); outerCol++) {
          if (outerCol > 0) sb.append(", ");
          BaseType obt = (BaseType) outerVector.get(outerCol);
          if (obt instanceof DByte t) sb.append(t.getValue());
          else if (obt instanceof DFloat32 t) sb.append(t.getValue());
          else if (obt instanceof DFloat64 t) sb.append(t.getValue());
          else if (obt instanceof DInt16 t) sb.append(t.getValue());
          else if (obt instanceof DUInt16 t) sb.append(t.getValue());
          else if (obt instanceof DInt32 t) sb.append(t.getValue());
          else if (obt instanceof DUInt32 t) sb.append(t.getValue());
          else if (obt instanceof DBoolean t) sb.append(t.getValue());
          else if (obt instanceof DString t) sb.append(t.getValue());
          else if (obt instanceof DSequence t) sb.append("DSequence)");
          else sb.append(obt.getTypeName());
        }
        System.out.println(sb.toString());
      }
    }
  }

  /** */
  @org.junit.jupiter.api.Test
  @TagMissingDataset // no defintion for erdlasCalCatch
  void testErdlasCalCatch() throws Throwable {
    // testVerboseOn();
    int language = 0;
    String results, query, tName, expected;
    String baseQuery = "&time>=2006-01-01";
    EDDTable tedd = (EDDTable) EDDTableFromDapSequence.oneFromDatasetsXml(null, "erdlasCalCatch");

    // the basicQuery
    // http://las.pfeg.noaa.gov/cgi-bin/ERDserver/calcatch.sql?time,area,block,Comments,Description,imported,mark_cat,NominalSpecies,pounds,region,RegionName,SpeciesGroup&time>="2006-01-01%2000:00:00"
    // my test in browser (with calcatch. added)
    // http://las.pfeg.noaa.gov/cgi-bin/ERDserver/calcatch.sql.ascii?calcatch.time,calcatch.area,calcatch.block,calcatch.Comments,calcatch.Description,calcatch.imported,calcatch.mark_cat,calcatch.NominalSpecies,calcatch.pounds,calcatch.region,calcatch.RegionName,calcatch.SpeciesGroup&calcatch.time%3E=1978-01-01
    // returns goofy results with or without " around date constraint
    // lynn test (she has more variables, and in same order as form):
    // http://las.pfeg.noaa.gov/cgi-bin/ERDserver/calcatch.sql.ascii?calcatch.mark_cat,calcatch.pounds,calcatch.imported,calcatch.Description,calcatch.area,calcatch.region,calcatch.year,calcatch.SpeciesGroup,calcatch.month,calcatch.NominalSpecies,calcatch.Comments,calcatch.time,calcatch.RegionName,calcatch.block&calcatch.time%3E1978-01-01
    // try next: change pydap config: calcatch.time -> String, and all String vars
    // missing_value=""
    /*
     * Dataset {
     * Sequence {
     * String time;
     * String area;
     * Int32 block;
     * String Comments;
     * String Description;
     * String imported;
     * Int32 mark_cat;
     * String NominalSpecies;
     * String pounds;
     * Int32 region;
     * String RegionName;
     * String SpeciesGroup;
     * } calcatch;
     * } calcatch%2Esql;
     * ---------------------------------------------
     * calcatch.time, calcatch.area, calcatch.block, calcatch.Comments,
     * calcatch.Description, calcatch.imported, calcatch.mark_cat,
     * calcatch.NominalSpecies, calcatch.pounds, calcatch.region,
     * calcatch.RegionName, calcatch.SpeciesGroup
     * "1972-02-01", -9999, 682, -9999, "Rockfish, yelloweye", "N", 265, "YEYE",
     * "264", -9999, -9999, "ROCK"
     * "1973-08-01", -9999, 200, -9999, "Smelts, true", "N", 180, "SMLT", "375",
     * -9999, -9999, "PEL"
     * ...
     * -9999, -9999, -9999, -9999, "Surfperch, unspecified", "N", 550, "PRCH",
     * "40020", -9999, -9999, "OTH"
     * ...
     * "1973-07-01", -9999, 701, -9999, "Bonito, Pacific", "N", 3, "BONI", "149",
     * -9999, -9999, "GAME"
     * "1974-06-01", "Northern California", 203, "also called pointed nose sole",
     * "Sole, English", "N", 206, "EGLS", "638", 2, "Eureka", "FLAT"
     * "1977-07-01", "Southern California", 652, -9999, "Shark, thresher", "N", 155,
     * "SHRK", "22", 6, "Santa Barbara - Morro Bay", "SHRK"
     * "1971-02-01", "Southern California", 665, "also called southern halibut",
     * "Halibut, California", "N", 222, "CHLB", "2383", 6,
     * "Santa Barbara - Morro Bay", "FLAT"
     * "1976-11-01", "Central California", 623, -9999, "Sole, unspecified", "N",
     * 200, "UFLT", "302", 6, "Santa Barbara - Morro Bay", "FLAT"
     * "1976-08-01", "Central California", 600, -9999, "Turbot", "N", 240, "UFLT",
     * "40", 6, "Santa Barbara - Morro Bay", "FLAT"
     */
    tName =
        tedd.makeNewFileForDapQuery(
            language,
            null,
            null,
            baseQuery,
            EDStatic.fullTestCacheDirectory,
            tedd.className() + "_CalCaltch",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected = "\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** This tests sourceNeedsExpandedFP_EQ. 2016-01-16 SOURCE IS GONE. */
  @org.junit.jupiter.api.Test
  @TagMissingDataset // unknown host
  void testSourceNeedsExpandedFP_EQ() throws Throwable {
    // String2.log("\n*** EDDTableFromDapSequence.testSourceNeedsExpandedFP_EQ\n");
    // testVerboseOn();
    int language = 0;
    String results, query, tName, expected;
    EDDTable edd = (EDDTable) EDDTestDataset.getnwioosGroundfish();

    // the basicQuery
    // [was: test a TableWriter that doesn't convert time to iso format
    // now year is converted to time.]
    query =
        "longitude,latitude,time,common_name&longitude=-124.348098754882&latitude=44.690254211425";

    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            query,
            EDStatic.fullTestCacheDirectory,
            edd.className() + "_FP_EQ",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected = // pre 2015-12-28 was sorted lexically, now case insensitive. pre 2013-05-28
        // wasn't sorted
        "longitude,latitude,time,common_name\n"
            + "degrees_east,degrees_north,UTC,\n"
            + "-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,arrowtooth flounder\n"
            + "-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,bocaccio\n"
            + "-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,canary rockfish\n"
            + "-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,chilipepper\n"
            + "-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,cowcod\n"
            + "-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,darkblotched rockfish\n"
            + "-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,Dover sole\n"
            + "-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,English sole\n"
            + "-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,longspine thornyhead\n"
            + "-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,Pacific ocean perch\n"
            + "-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,petrale sole\n"
            + "-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,sablefish\n"
            + "-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,shortspine thornyhead\n"
            + "-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,widow rockfish\n"
            + "-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,yelloweye rockfish\n"
            + "-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,yellowtail rockfish\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** NOT FINISHED. This tests nosCoopsRWL. */
  @org.junit.jupiter.api.Test
  @TagMissingDataset // source 404
  void testNosCoopsRWL() throws Throwable {
    // String2.log("\n*** EDDTableFromDapSequence.testNosCoopsRWL\n");
    // testVerboseOn();
    int language = 0;
    String results, query, tName, expected;
    String today = Calendar2.epochSecondsToIsoStringTZ(Calendar2.backNDays(1, Double.NaN));
    String yesterday = Calendar2.epochSecondsToIsoStringTZ(Calendar2.backNDays(2, Double.NaN));

    EDDTable edd = (EDDTable) EDDTestDataset.getnosCoopsRWL();

    // *** test a TableWriter that doesn't convert time to iso format
    query = "&station=\"1612340\"&datum=\"MLLW\"&beginTime=" + yesterday + "&endTime=" + today;
    // https://opendap.co-ops.nos.noaa.gov/dods/IOOS/SixMin_Verified_Water_Level.ascii?
    // &WATERLEVEL_6MIN_VFD_PX._STATION_ID="1612340"&WATERLEVEL_6MIN_VFD_PX._DATUM="MLLW"
    // &WATERLEVEL_6MIN_VFD_PX._BEGIN_DATE="20100825"&WATERLEVEL_6MIN_VFD_PX._END_DATE="20100826"
    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            query,
            EDStatic.fullTestCacheDirectory,
            edd.className() + "_RWL",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // trouble: java.time (was Joda) doesn't like space-padded hour values

    expected = "zztop\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** Test reading .das */
  @org.junit.jupiter.api.Test
  @TagExternalERDDAP
  void testReadDas() throws Exception {
    String2.log("\n*** EDDTableFromDapSequence.testReadDas\n");
    String url = "https://coastwatch.pfeg.noaa.gov/erddap/tabledap/erdGtsppBest";
    int language = 0;
    DConnect dConnect = new DConnect(url, true, 1, 1);
    DAS das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
    DDS dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);
  }

  /** Test graph made from subsetVariables data */
  @org.junit.jupiter.api.Test
  @TagMissingDataset // unknown host
  void testSubsetVariablesGraph() throws Throwable {
    String2.log("\n*** EDDTableFromDapSequence.testSubsetVariablesGraph\n");
    int language = 0;
    EDDTable edd = (EDDTable) EDDTestDataset.getnwioosCoral();

    String tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "longitude,latitude,time&time=%221992-01-01T00:00:00Z%22"
                + "&longitude>=-132.0&longitude<=-112.0&latitude>=30.0&latitude<=50.0"
                + "&distinct()&.draw=markers&.colorBar=|D||||",
            EDStatic.fullTestCacheDirectory,
            edd.className() + "_SVGraph",
            ".png");
    Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
  }

  /** Test that info from subsetVariables gets back to variable's ranges */
  @org.junit.jupiter.api.Test
  @TagMissingDataset // unknown host
  void testSubsetVariablesRange() throws Throwable {
    String2.log("\n*** EDDTableFromDapSequence.testSubsetVariablesRange\n");
    int language = 0;
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard to
    // check min:sec.

    // before I fixed this, time had destinationMin/Max = NaN
    EDDTable edd = (EDDTable) EDDTestDataset.getnwioosCoral();
    EDV edvTime = edd.dataVariables()[edd.timeIndex];
    Test.ensureEqual(edvTime.destinationMinDouble(), 3.155328E8, "");
    Test.ensureEqual(edvTime.destinationMaxDouble(), 1.1045376E9, "");

    String tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            edd.className() + "_Entire",
            ".das");
    String results =
        String2.annotatedString(
            File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName));
    String tResults;
    String expected =
        "Attributes {[10]\n"
            + " s {[10]\n"
            + "  longitude {[10]\n"
            + "    String _CoordinateAxisType \"Lon\";[10]\n"
            + "    Float64 actual_range -125.98999786376953, -117.27667236328125;[10]\n"
            + "    String axis \"X\";[10]\n"
            + "    String ioos_category \"Location\";[10]\n"
            + "    String long_name \"Longitude\";[10]\n"
            + "    String standard_name \"longitude\";[10]\n"
            + "    String units \"degrees_east\";[10]\n"
            + "  }[10]\n"
            + "  latitude {[10]\n"
            + "    String _CoordinateAxisType \"Lat\";[10]\n"
            + "    Float64 actual_range 32.570838928222656, 48.969085693359375;[10]\n"
            + "    String axis \"Y\";[10]\n"
            + "    String ioos_category \"Location\";[10]\n"
            + "    String long_name \"Latitude\";[10]\n"
            + "    String standard_name \"latitude\";[10]\n"
            + "    String units \"degrees_north\";[10]\n"
            + "  }[10]\n"
            + "  depth {[10]\n"
            + "    String _CoordinateAxisType \"Height\";[10]\n"
            + "    String _CoordinateZisPositive \"down\";[10]\n"
            + "    Float64 actual_range 11.0, 1543.0;[10]\n"
            + "    String axis \"Z\";[10]\n"
            + "    Float64 colorBarMaximum 1500.0;[10]\n"
            + "    Float64 colorBarMinimum 0.0;[10]\n"
            + "    String ioos_category \"Location\";[10]\n"
            + "    String long_name \"Depth\";[10]\n"
            + "    String positive \"down\";[10]\n"
            + "    String standard_name \"depth\";[10]\n"
            + "    String units \"m\";[10]\n"
            + "  }[10]\n"
            + "  time {[10]\n"
            + "    String _CoordinateAxisType \"Time\";[10]\n"
            + "    Float64 actual_range 3.155328e+8, 1.1045376e+9;[10]\n"
            + "    String axis \"T\";[10]\n"
            + "    String Description \"Year of Survey.\";[10]\n"
            + "    String ioos_category \"Time\";[10]\n"
            + "    String long_name \"Time (Beginning of Survey Year)\";[10]\n"
            + "    String standard_name \"time\";[10]\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";[10]\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";[10]\n"
            + "  }[10]\n"
            + "  institution {[10]\n"
            + "    String Description \"Institution is either: Northwest Fisheries Science Center (FRAM Division) or Alaska Fisheries Science Center (RACE Division)\";[10]\n"
            + "    String ioos_category \"Identifier\";[10]\n"
            + "    String long_name \"Institution\";[10]\n"
            + "  }[10]\n"
            + "  institution_id {[10]\n"
            + "    Float64 actual_range 38807.0, 2.00503017472e+11;[10]\n"
            + "    String Description \"Unique ID from Institution.\";[10]\n"
            + "    String ioos_category \"Identifier\";[10]\n"
            + "    String long_name \"Institution ID\";[10]\n"
            + "  }[10]\n"
            + "  species_code {[10]\n"
            + "    Float64 actual_range 41000.0, 144115.0;[10]\n"
            + "    String Description \"Unique identifier for species.\";[10]\n"
            + "    String ioos_category \"Taxonomy\";[10]\n"
            + "    String long_name \"Species Code\";[10]\n"
            + "  }[10]\n"
            + "  taxa_scientific {[10]\n"
            + "    String Description \"Scientific name of taxa\";[10]\n"
            + "    String ioos_category \"Taxonomy\";[10]\n"
            + "    String long_name \"Taxa Scientific\";[10]\n"
            + "  }[10]\n"
            + "  taxonomic_order {[10]\n"
            + "    String ioos_category \"Taxonomy\";[10]\n"
            + "    String long_name \"Taxonomic Order\";[10]\n"
            + "  }[10]\n"
            + "  order_abbreviation {[10]\n"
            + "    String ioos_category \"Taxonomy\";[10]\n"
            + "    String long_name \"Order Abbreviation\";[10]\n"
            + "  }[10]\n"
            + "  taxonomic_family {[10]\n"
            + "    String ioos_category \"Taxonomy\";[10]\n"
            + "    String long_name \"Taxonomic Family\";[10]\n"
            + "  }[10]\n"
            + "  family_abbreviation {[10]\n"
            + "    String ioos_category \"Taxonomy\";[10]\n"
            + "    String long_name \"Family Abbreviation\";[10]\n"
            + "  }[10]\n"
            + "  taxonomic_genus {[10]\n"
            + "    String Description \"Taxonomic Genus.\";[10]\n"
            + "    String ioos_category \"Taxonomy\";[10]\n"
            + "    String long_name \"Taxonomic Genus\";[10]\n"
            + "  }[10]\n"
            + " }[10]\n"
            + "  NC_GLOBAL {[10]\n"
            + "    String cdm_data_type \"Point\";[10]\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";[10]\n"
            + "    Float64 Easternmost_Easting -117.27667236328125;[10]\n"
            + "    String featureType \"Point\";[10]\n"
            + "    Float64 geospatial_lat_max 48.969085693359375;[10]\n"
            + "    Float64 geospatial_lat_min 32.570838928222656;[10]\n"
            + "    String geospatial_lat_units \"degrees_north\";[10]\n"
            + "    Float64 geospatial_lon_max -117.27667236328125;[10]\n"
            + "    Float64 geospatial_lon_min -125.98999786376953;[10]\n"
            + "    String geospatial_lon_units \"degrees_east\";[10]\n"
            + "    Float64 geospatial_vertical_max 1543.0;[10]\n"
            + "    Float64 geospatial_vertical_min 11.0;[10]\n"
            + "    String geospatial_vertical_positive \"down\";[10]\n"
            + "    String geospatial_vertical_units \"m\";[10]\n"
            + "    String history \""
            + today;
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

    // + "
    // http://nwioos.coas.oregonstate.edu:8080/dods/drds/Coral%201980-2005[10]\n" +
    // today + " http://localhost:8080/cwexperimental/
    expected =
        "tabledap/nwioosCoral.das\";[10]\n"
            + "    String infoUrl \"http://nwioos.coas.oregonstate.edu:8080/dods/drds/Coral%201980-2005.info\";[10]\n"
            + "    String institution \"NOAA NWFSC\";[10]\n"
            + "    String keywords \"1980-2005, abbreviation, atmosphere, beginning, coast, code, collected, coral, data, depth, Earth Science > Biological Classification > Animals/Invertebrates > Cnidarians > Anthozoans/Hexacorals > Hard Or Stony Corals, Earth Science > Biosphere > Aquatic Ecosystems > Coastal Habitat, Earth Science > Biosphere > Aquatic Ecosystems > Marine Habitat, family, genus, height, identifier, institution, noaa, nwfsc, off, order, scientific, species, station, survey, taxa, taxonomic, taxonomy, time, west, west coast, year\";[10]\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";[10]\n"
            + "    String license \"The data may be used and redistributed for free but is not intended[10]\n"
            + "for legal use, since it may contain inaccuracies. Neither the data[10]\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any[10]\n"
            + "of their employees or contractors, makes any warranty, express or[10]\n"
            + "implied, including warranties of merchantability and fitness for a[10]\n"
            + "particular purpose, or assumes any legal liability for the accuracy,[10]\n"
            + "completeness, or usefulness, of this information.\";[10]\n"
            + "    Float64 Northernmost_Northing 48.969085693359375;[10]\n"
            + "    String sourceUrl \"http://nwioos.coas.oregonstate.edu:8080/dods/drds/Coral%201980-2005\";[10]\n"
            + "    Float64 Southernmost_Northing 32.570838928222656;[10]\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v55\";[10]\n"
            + "    String subsetVariables \"longitude, latitude, depth, time, institution, institution_id, species_code, taxa_scientific, taxonomic_order, order_abbreviation, taxonomic_family, family_abbreviation, taxonomic_genus\";[10]\n"
            + "    String summary \"This data contains the locations of some observations of[10]\n"
            + "cold-water/deep-sea corals off the west coast of the United States.[10]\n"
            + "Records of coral catch originate from bottom trawl surveys conducted[10]\n"
            + "from 1980 to 2001 by the Alaska Fisheries Science Center (AFSC) and[10]\n"
            + "2001 to 2005 by the Northwest Fisheries Science Center (NWFSC).[10]\n"
            + "Locational information represent the vessel mid positions (for AFSC[10]\n"
            + "survey trawls) or \\\"best position\\\" (i.e., priority order: 1) gear[10]\n"
            + "midpoint 2) vessel midpoint, 3) vessel start point, 4) vessel end[10]\n"
            + "point, 5) station coordinates for NWFSC survey trawls) conducted as[10]\n"
            + "part of regular surveys of groundfish off the coasts of Washington,[10]\n"
            + "Oregon and California by NOAA Fisheries. Only records where corals[10]\n"
            + "were identified in the total catch are included. Each catch sample[10]\n"
            + "of coral was identified down to the most specific taxonomic level[10]\n"
            + "possible by the biologists onboard, therefore identification was[10]\n"
            + "dependent on their expertise. When positive identification was not[10]\n"
            + "possible, samples were sometimes archived for future identification[10]\n"
            + "by systematist experts. Data were compiled by the NWFSC, Fishery[10]\n"
            + "Resource Analysis & Monitoring Division[10]\n"
            + "[10]\n"
            + "Purpose - Examination of the spatial and temporal distributions of[10]\n"
            + "observations of cold-water/deep-sea corals off the west coast of the[10]\n"
            + "United States, including waters off the states of Washington, Oregon,[10]\n"
            + "and California. It is important to note that these records represent[10]\n"
            + "only presence of corals in the area swept by the trawl gear. Since[10]\n"
            + "bottom trawls used during these surveys are not designed to sample[10]\n"
            + "epibenthic invertebrates, absence of corals in the catch does not[10]\n"
            + "necessary mean they do not occupy the area swept by the trawl gear.[10]\n"
            + "[10]\n"
            + "Data Credits - NOAA Fisheries, Alaska Fisheries Science Center,[10]\n"
            + "Resource Assessment & Conservation Engineering Division (RACE) NOAA[10]\n"
            + "Fisheries, Northwest Fisheries Science Center, Fishery Resource[10]\n"
            + "Analysis & Monitoring Division (FRAM)[10]\n"
            + "[10]\n"
            + "Contact: Curt Whitmire, NOAA NWFSC, Curt.Whitmire@noaa.gov\";[10]\n"
            + "    String time_coverage_end \"2005-01-01T00:00:00Z\";[10]\n"
            + "    String time_coverage_start \"1980-01-01T00:00:00Z\";[10]\n"
            + "    String title \"NWFSC Coral Data Collected off West Coast of US (1980-2005)\";[10]\n"
            + "    Float64 Westernmost_Easting -125.98999786376953;[10]\n"
            + "  }[10]\n"
            + "}[10]\n"
            + "[end]";
    int tPo = results.indexOf(expected.substring(0, 17));
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
        expected,
        "results=\n" + results);
  }
}
