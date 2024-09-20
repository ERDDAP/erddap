package gov.noaa.pfel.erddap.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tags.TagIncompleteTest;
import tags.TagMissingDataset;
import tags.TagSlowTests;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

class EDDTableFromMultidimNcFilesTests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /**
   * testGenerateDatasetsXml. This doesn't test suggestTestOutOfDate, except that for old data it
   * doesn't suggest anything.
   */
  @org.junit.jupiter.api.Test
  void testGenerateDatasetsXml() throws Throwable {
    // testVerboseOn();
    String dataDir =
        File2.addSlash(
            Path.of(EDDTableFromMultidimNcFilesTests.class.getResource("/data/nc/").toURI())
                .toString());
    String fileNameRegex = ".*_prof\\.nc";
    String useDimensionsCSV = "N_PROF, N_LEVELS";
    String results =
        EDDTableFromMultidimNcFiles.generateDatasetsXml(
                dataDir,
                fileNameRegex,
                "",
                useDimensionsCSV,
                1440,
                "^",
                "_prof.nc$",
                ".*",
                "fileNumber", // just for test purposes
                true, // removeMVRows
                "FLOAT_SERIAL_NO JULD", // sort files by
                "",
                "",
                "",
                "",
                -1, // defaultStandardizeWhat
                "", // treatDimensionsAs
                null, // cacheFromUrl
                null)
            + "\n";

    String tDatasetID =
        EDDTableFromMultidimNcFiles.suggestDatasetID(dataDir + fileNameRegex + useDimensionsCSV);
    String expected =
        "<dataset type=\"EDDTableFromMultidimNcFiles\" datasetID=\""
            + tDatasetID
            + "\" active=\"true\">\n"
            + "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n"
            + "    <updateEveryNMillis>10000</updateEveryNMillis>\n"
            + "    <fileDir>"
            + dataDir
            + "</fileDir>\n"
            + "    <fileNameRegex>"
            + fileNameRegex
            + "</fileNameRegex>\n"
            + "    <recursive>true</recursive>\n"
            + "    <pathRegex>.*</pathRegex>\n"
            + "    <metadataFrom>last</metadataFrom>\n"
            + "    <standardizeWhat>0</standardizeWhat>\n"
            + "    <preExtractRegex>^</preExtractRegex>\n"
            + "    <postExtractRegex>_prof.nc$</postExtractRegex>\n"
            + "    <extractRegex>.*</extractRegex>\n"
            + "    <columnNameForExtract>fileNumber</columnNameForExtract>\n"
            + "    <removeMVRows>true</removeMVRows>\n"
            + "    <sortFilesBySourceNames>FLOAT_SERIAL_NO JULD</sortFilesBySourceNames>\n"
            + "    <fileTableInMemory>false</fileTableInMemory>\n"
            + "    <!-- sourceAttributes>\n"
            + "        <att name=\"Conventions\">Argo-3.1 CF-1.6</att>\n"
            + "        <att name=\"featureType\">trajectoryProfile</att>\n"
            + "        <att name=\"history\">2016-04-15T20:47:22Z creation</att>\n"
            + "        <att name=\"institution\">Coriolis GDAC</att>\n"
            + "        <att name=\"references\">http://www.argodatamgt.org/Documentation</att>\n"
            + "        <att name=\"source\">Argo float</att>\n"
            + "        <att name=\"title\">Argo float vertical profile</att>\n"
            + "        <att name=\"user_manual_version\">3.1</att>\n"
            + "    </sourceAttributes -->\n"
            + "    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n"
            + "        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n"
            + "        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n"
            + "    -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cdm_data_type\">TrajectoryProfile</att>\n"
            + "        <att name=\"cdm_profile_variables\">profile_id, ???</att>\n"
            + "        <att name=\"cdm_trajectory_variables\">trajectory_id, ???</att>\n"
            + "        <att name=\"Conventions\">Argo-3.1, CF-1.10, COARDS, ACDD-1.3</att>\n"
            + "        <att name=\"creator_name\">Coriolis GDAC</att>\n"
            + "        <att name=\"creator_type\">institution</att>\n"
            + "        <att name=\"creator_url\">http://www.argodatamgt.org/Documentation</att>\n"
            + "        <att name=\"infoUrl\">http://www.argodatamgt.org/Documentation</att>\n"
            + "        <att name=\"keywords\">adjusted, argo, array, assembly, centre, centres, charge, coded, CONFIG_MISSION_NUMBER, contains, coriolis, creation, currents, cycle, CYCLE_NUMBER, data, DATA_CENTRE, DATA_MODE, DATA_STATE_INDICATOR, DATA_TYPE, date, DATE_CREATION, DATE_UPDATE, day, days, DC_REFERENCE, degree, delayed, denoting, density, determined, direction, earth, Earth Science &gt; Oceans &gt; Ocean Pressure &gt; Water Pressure, Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Water Temperature, Earth Science &gt; Oceans &gt; Salinity/Density &gt; Salinity, equals, error, file, firmware, FIRMWARE_VERSION, flag, float, FLOAT_SERIAL_NO, format, FORMAT_VERSION, gdac, geostrophic, global, handbook, HANDBOOK_VERSION, identifier, in-situ, instrument, investigator, its, its-90, JULD_LOCATION, JULD_QC, julian, latitude, level, longitude, missions, mode, name, number, ocean, oceanography, oceans, passed, performed, PI_NAME, PLATFORM_NUMBER, PLATFORM_TYPE, position, POSITION_QC, positioning, POSITIONING_SYSTEM, practical, pres, PRES_ADJUSTED, PRES_ADJUSTED_ERROR, PRES_ADJUSTED_QC, PRES_QC, pressure, principal, process, processing, profile, PROFILE_PRES_QC, PROFILE_PSAL_QC, PROFILE_TEMP_QC, profiles, project, PROJECT_NAME, psal, PSAL_ADJUSTED, PSAL_ADJUSTED_ERROR, PSAL_ADJUSTED_QC, PSAL_QC, quality, real, real time, real-time, realtime, reference, REFERENCE_DATE_TIME, relative, salinity, sampling, scale, scheme, science, sea, sea level, sea-level, sea_water_practical_salinity, sea_water_pressure, sea_water_temperature, seawater, serial, situ, station, statistics, system, TEMP, TEMP_ADJUSTED, TEMP_ADJUSTED_ERROR, TEMP_ADJUSTED_QC, TEMP_QC, temperature, through, time, type, unique, update, values, version, vertical, VERTICAL_SAMPLING_SCHEME, water, WMO_INST_TYPE</att>\n"
            + "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n"
            + "        <att name=\"license\">[standard]</att>\n"
            + "        <att name=\"sourceUrl\">(local files)</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"subsetVariables\">DATA_TYPE, FORMAT_VERSION, HANDBOOK_VERSION, REFERENCE_DATE_TIME, DATE_CREATION, DATE_UPDATE, PLATFORM_NUMBER, PROJECT_NAME, PI_NAME, DIRECTION, DATA_CENTRE, WMO_INST_TYPE, JULD_QC, POSITION_QC, POSITIONING_SYSTEM, CONFIG_MISSION_NUMBER</att>\n"
            + "        <att name=\"summary\">Argo float vertical profile. Coriolis Global Data Assembly Centres (GDAC) data from a local source.</att>\n"
            + "    </addAttributes>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>fileNumber</sourceName>\n"
            + "        <destinationName>fileNumber</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "            <att name=\"long_name\">File Number</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>DATA_TYPE</sourceName>\n"
            + "        <destinationName>DATA_TYPE</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"conventions\">Argo reference table 1</att>\n"
            + "            <att name=\"long_name\">Data type</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>FORMAT_VERSION</sourceName>\n"
            + "        <destinationName>FORMAT_VERSION</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">File format version</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>HANDBOOK_VERSION</sourceName>\n"
            + "        <destinationName>HANDBOOK_VERSION</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">Data handbook version</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>REFERENCE_DATE_TIME</sourceName>\n"
            + "        <destinationName>REFERENCE_DATE_TIME</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"conventions\">YYYYMMDDHHMISS</att>\n"
            + "            <att name=\"long_name\">Date of reference for Julian days</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>DATE_CREATION</sourceName>\n"
            + "        <destinationName>DATE_CREATION</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"conventions\">YYYYMMDDHHMISS</att>\n"
            + "            <att name=\"long_name\">Date of file creation</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>DATE_UPDATE</sourceName>\n"
            + "        <destinationName>DATE_UPDATE</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"conventions\">YYYYMMDDHHMISS</att>\n"
            + "            <att name=\"long_name\">Date of update of this file</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>PLATFORM_NUMBER</sourceName>\n"
            + "        <destinationName>PLATFORM_NUMBER</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"conventions\">WMO float identifier : A9IIIII</att>\n"
            + "            <att name=\"long_name\">Float unique identifier</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>PROJECT_NAME</sourceName>\n"
            + "        <destinationName>PROJECT_NAME</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">Name of the project</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>PI_NAME</sourceName>\n"
            + "        <destinationName>PI_NAME</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">Name of the principal investigator</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>CYCLE_NUMBER</sourceName>\n"
            + "        <destinationName>CYCLE_NUMBER</destinationName>\n"
            + "        <dataType>int</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"int\">99999</att>\n"
            + "            <att name=\"conventions\">0...N, 0 : launch cycle (if exists), 1 : first complete cycle</att>\n"
            + "            <att name=\"long_name\">Float cycle number</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Statistics</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>DIRECTION</sourceName>\n"
            + "        <destinationName>DIRECTION</destinationName>\n"
            + "        <dataType>char</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"conventions\">A: ascending profiles, D: descending profiles</att>\n"
            + "            <att name=\"long_name\">Direction of the station profiles</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Currents</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>DATA_CENTRE</sourceName>\n"
            + "        <destinationName>DATA_CENTRE</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"conventions\">Argo reference table 4</att>\n"
            + "            <att name=\"long_name\">Data centre in charge of float data processing</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>DC_REFERENCE</sourceName>\n"
            + "        <destinationName>DC_REFERENCE</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"conventions\">Data centre convention</att>\n"
            + "            <att name=\"long_name\">Station unique identifier in data centre</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>DATA_STATE_INDICATOR</sourceName>\n"
            + "        <destinationName>DATA_STATE_INDICATOR</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"conventions\">Argo reference table 6</att>\n"
            + "            <att name=\"long_name\">Degree of processing the data have passed through</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>DATA_MODE</sourceName>\n"
            + "        <destinationName>DATA_MODE</destinationName>\n"
            + "        <dataType>char</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"conventions\">R : real time; D : delayed mode; A : real time with adjustment</att>\n"
            + "            <att name=\"long_name\">Delayed mode or real time data</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>PLATFORM_TYPE</sourceName>\n"
            + "        <destinationName>PLATFORM_TYPE</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"conventions\">Argo reference table 23</att>\n"
            + "            <att name=\"long_name\">Type of float</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>FLOAT_SERIAL_NO</sourceName>\n"
            + "        <destinationName>FLOAT_SERIAL_NO</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">Serial number of the float</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Statistics</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>FIRMWARE_VERSION</sourceName>\n"
            + "        <destinationName>FIRMWARE_VERSION</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">Instrument firmware version</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>WMO_INST_TYPE</sourceName>\n"
            + "        <destinationName>WMO_INST_TYPE</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"conventions\">Argo reference table 8</att>\n"
            + "            <att name=\"long_name\">Coded instrument type</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>JULD</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"double\">999999.0</att>\n"
            + "            <att name=\"axis\">T</att>\n"
            + "            <att name=\"conventions\">Relative julian days with decimal part (as parts of day)</att>\n"
            + "            <att name=\"long_name\">Julian day (UTC) of the station relative to REFERENCE_DATE_TIME</att>\n"
            + "            <att name=\"resolution\" type=\"double\">0.0</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"units\">days since 1950-01-01 00:00:00 UTC</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"resolution\">null</att>\n"
            + "            <att name=\"source_name\">JULD</att>\n"
            + "            <att name=\"units\">days since 1950-01-01T00:00:00Z</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>JULD_QC</sourceName>\n"
            + "        <destinationName>JULD_QC</destinationName>\n"
            + "        <dataType>char</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"conventions\">Argo reference table 2</att>\n"
            + "            <att name=\"long_name\">Quality on date and time</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>JULD_LOCATION</sourceName>\n"
            + "        <destinationName>JULD_LOCATION</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"double\">999999.0</att>\n"
            + "            <att name=\"conventions\">Relative julian days with decimal part (as parts of day)</att>\n"
            + "            <att name=\"long_name\">Julian day (UTC) of the location relative to REFERENCE_DATE_TIME</att>\n"
            + "            <att name=\"resolution\" type=\"double\">0.0</att>\n"
            + "            <att name=\"units\">days since 1950-01-01 00:00:00 UTC</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"resolution\">null</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"units\">days since 1950-01-01T00:00:00Z</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>LATITUDE</sourceName>\n"
            + "        <destinationName>latitude</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"double\">99999.0</att>\n"
            + "            <att name=\"axis\">Y</att>\n"
            + "            <att name=\"long_name\">Latitude of the station, best estimate</att>\n"
            + "            <att name=\"standard_name\">latitude</att>\n"
            + "            <att name=\"units\">degree_north</att>\n"
            + "            <att name=\"valid_max\" type=\"double\">90.0</att>\n"
            + "            <att name=\"valid_min\" type=\"double\">-90.0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"units\">degrees_north</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>LONGITUDE</sourceName>\n"
            + "        <destinationName>longitude</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"double\">99999.0</att>\n"
            + "            <att name=\"axis\">X</att>\n"
            + "            <att name=\"long_name\">Longitude of the station, best estimate</att>\n"
            + "            <att name=\"standard_name\">longitude</att>\n"
            + "            <att name=\"units\">degree_east</att>\n"
            + "            <att name=\"valid_max\" type=\"double\">180.0</att>\n"
            + "            <att name=\"valid_min\" type=\"double\">-180.0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"units\">degrees_east</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>POSITION_QC</sourceName>\n"
            + "        <destinationName>POSITION_QC</destinationName>\n"
            + "        <dataType>char</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"conventions\">Argo reference table 2</att>\n"
            + "            <att name=\"long_name\">Quality on position (latitude and longitude)</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>POSITIONING_SYSTEM</sourceName>\n"
            + "        <destinationName>POSITIONING_SYSTEM</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">Positioning system</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>PROFILE_PRES_QC</sourceName>\n"
            + "        <destinationName>PROFILE_PRES_QC</destinationName>\n"
            + "        <dataType>char</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"conventions\">Argo reference table 2a</att>\n"
            + "            <att name=\"long_name\">Global quality flag of PRES profile</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>PROFILE_TEMP_QC</sourceName>\n"
            + "        <destinationName>PROFILE_TEMP_QC</destinationName>\n"
            + "        <dataType>char</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"conventions\">Argo reference table 2a</att>\n"
            + "            <att name=\"long_name\">Global quality flag of TEMP profile</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>PROFILE_PSAL_QC</sourceName>\n"
            + "        <destinationName>PROFILE_PSAL_QC</destinationName>\n"
            + "        <dataType>char</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"conventions\">Argo reference table 2a</att>\n"
            + "            <att name=\"long_name\">Global quality flag of PSAL profile</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>VERTICAL_SAMPLING_SCHEME</sourceName>\n"
            + "        <destinationName>VERTICAL_SAMPLING_SCHEME</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"conventions\">Argo reference table 16</att>\n"
            + "            <att name=\"long_name\">Vertical sampling scheme</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>CONFIG_MISSION_NUMBER</sourceName>\n"
            + "        <destinationName>CONFIG_MISSION_NUMBER</destinationName>\n"
            + "        <dataType>int</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"int\">99999</att>\n"
            + "            <att name=\"conventions\">1...N, 1 : first complete mission</att>\n"
            + "            <att name=\"long_name\">Unique number denoting the missions performed by the float</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Statistics</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>PRES</sourceName>\n"
            + "        <destinationName>PRES</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">99999.0</att>\n"
            + "            <att name=\"axis\">Z</att>\n"
            + "            <att name=\"C_format\">&#37;7.1f</att>\n"
            + "            <att name=\"FORTRAN_format\">F7.1</att>\n"
            + "            <att name=\"long_name\">Sea water pressure, equals 0 at sea-level</att>\n"
            + "            <att name=\"resolution\" type=\"float\">1.0</att>\n"
            + "            <att name=\"standard_name\">sea_water_pressure</att>\n"
            + "            <att name=\"units\">decibar</att>\n"
            + "            <att name=\"valid_max\" type=\"float\">12000.0</att>\n"
            + "            <att name=\"valid_min\" type=\"float\">0.0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">5000.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Sea Level</att>\n"
            + "            <att name=\"resolution\">null</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>PRES_QC</sourceName>\n"
            + "        <destinationName>PRES_QC</destinationName>\n"
            + "        <dataType>char</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"conventions\">Argo reference table 2</att>\n"
            + "            <att name=\"long_name\">quality flag</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>PRES_ADJUSTED</sourceName>\n"
            + "        <destinationName>PRES_ADJUSTED</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">99999.0</att>\n"
            + "            <att name=\"axis\">Z</att>\n"
            + "            <att name=\"C_format\">&#37;7.1f</att>\n"
            + "            <att name=\"FORTRAN_format\">F7.1</att>\n"
            + "            <att name=\"long_name\">Sea water pressure, equals 0 at sea-level</att>\n"
            + "            <att name=\"resolution\" type=\"float\">1.0</att>\n"
            + "            <att name=\"standard_name\">sea_water_pressure</att>\n"
            + "            <att name=\"units\">decibar</att>\n"
            + "            <att name=\"valid_max\" type=\"float\">12000.0</att>\n"
            + "            <att name=\"valid_min\" type=\"float\">0.0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">5000.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Sea Level</att>\n"
            + "            <att name=\"resolution\">null</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>PRES_ADJUSTED_QC</sourceName>\n"
            + "        <destinationName>PRES_ADJUSTED_QC</destinationName>\n"
            + "        <dataType>char</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"conventions\">Argo reference table 2</att>\n"
            + "            <att name=\"long_name\">quality flag</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>PRES_ADJUSTED_ERROR</sourceName>\n"
            + "        <destinationName>PRES_ADJUSTED_ERROR</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">99999.0</att>\n"
            + "            <att name=\"C_format\">&#37;7.1f</att>\n"
            + "            <att name=\"FORTRAN_format\">F7.1</att>\n"
            + "            <att name=\"long_name\">Contains the error on the adjusted values as determined by the delayed mode QC process</att>\n"
            + "            <att name=\"resolution\" type=\"float\">1.0</att>\n"
            + "            <att name=\"units\">decibar</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">50.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Statistics</att>\n"
            + "            <att name=\"resolution\">null</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>TEMP</sourceName>\n"
            + "        <destinationName>TEMP</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">99999.0</att>\n"
            + "            <att name=\"C_format\">&#37;9.3f</att>\n"
            + "            <att name=\"FORTRAN_format\">F9.3</att>\n"
            + "            <att name=\"long_name\">Sea temperature in-situ ITS-90 scale</att>\n"
            + "            <att name=\"resolution\" type=\"float\">0.001</att>\n"
            + "            <att name=\"standard_name\">sea_water_temperature</att>\n"
            + "            <att name=\"units\">degree_Celsius</att>\n"
            + "            <att name=\"valid_max\" type=\"float\">40.0</att>\n"
            + "            <att name=\"valid_min\" type=\"float\">-2.5</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Temperature</att>\n"
            + "            <att name=\"resolution\">null</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>TEMP_QC</sourceName>\n"
            + "        <destinationName>TEMP_QC</destinationName>\n"
            + "        <dataType>char</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"conventions\">Argo reference table 2</att>\n"
            + "            <att name=\"long_name\">quality flag</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>TEMP_ADJUSTED</sourceName>\n"
            + "        <destinationName>TEMP_ADJUSTED</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">99999.0</att>\n"
            + "            <att name=\"C_format\">&#37;9.3f</att>\n"
            + "            <att name=\"FORTRAN_format\">F9.3</att>\n"
            + "            <att name=\"long_name\">Sea temperature in-situ ITS-90 scale</att>\n"
            + "            <att name=\"resolution\" type=\"float\">0.001</att>\n"
            + "            <att name=\"standard_name\">sea_water_temperature</att>\n"
            + "            <att name=\"units\">degree_Celsius</att>\n"
            + "            <att name=\"valid_max\" type=\"float\">40.0</att>\n"
            + "            <att name=\"valid_min\" type=\"float\">-2.5</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Temperature</att>\n"
            + "            <att name=\"resolution\">null</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>TEMP_ADJUSTED_QC</sourceName>\n"
            + "        <destinationName>TEMP_ADJUSTED_QC</destinationName>\n"
            + "        <dataType>char</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"conventions\">Argo reference table 2</att>\n"
            + "            <att name=\"long_name\">quality flag</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>TEMP_ADJUSTED_ERROR</sourceName>\n"
            + "        <destinationName>TEMP_ADJUSTED_ERROR</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">99999.0</att>\n"
            + "            <att name=\"C_format\">&#37;9.3f</att>\n"
            + "            <att name=\"FORTRAN_format\">F9.3</att>\n"
            + "            <att name=\"long_name\">Contains the error on the adjusted values as determined by the delayed mode QC process</att>\n"
            + "            <att name=\"resolution\" type=\"float\">0.001</att>\n"
            + "            <att name=\"units\">degree_Celsius</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">5.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Statistics</att>\n"
            + "            <att name=\"resolution\">null</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>PSAL</sourceName>\n"
            + "        <destinationName>PSAL</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">99999.0</att>\n"
            + "            <att name=\"C_format\">&#37;9.3f</att>\n"
            + "            <att name=\"FORTRAN_format\">F9.3</att>\n"
            + "            <att name=\"long_name\">Practical salinity</att>\n"
            + "            <att name=\"resolution\" type=\"float\">0.001</att>\n"
            + "            <att name=\"standard_name\">sea_water_salinity</att>\n"
            + "            <att name=\"units\">psu</att>\n"
            + "            <att name=\"valid_max\" type=\"float\">41.0</att>\n"
            + "            <att name=\"valid_min\" type=\"float\">2.0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">37.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">32.0</att>\n"
            + "            <att name=\"ioos_category\">Salinity</att>\n"
            + "            <att name=\"resolution\">null</att>\n"
            + "            <att name=\"standard_name\">sea_water_practical_salinity</att>\n"
            + "            <att name=\"units\">PSU</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>PSAL_QC</sourceName>\n"
            + "        <destinationName>PSAL_QC</destinationName>\n"
            + "        <dataType>char</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"conventions\">Argo reference table 2</att>\n"
            + "            <att name=\"long_name\">quality flag</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>PSAL_ADJUSTED</sourceName>\n"
            + "        <destinationName>PSAL_ADJUSTED</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">99999.0</att>\n"
            + "            <att name=\"C_format\">&#37;9.3f</att>\n"
            + "            <att name=\"FORTRAN_format\">F9.3</att>\n"
            + "            <att name=\"long_name\">Practical salinity</att>\n"
            + "            <att name=\"resolution\" type=\"float\">0.001</att>\n"
            + "            <att name=\"standard_name\">sea_water_salinity</att>\n"
            + "            <att name=\"units\">psu</att>\n"
            + "            <att name=\"valid_max\" type=\"float\">41.0</att>\n"
            + "            <att name=\"valid_min\" type=\"float\">2.0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">37.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">32.0</att>\n"
            + "            <att name=\"ioos_category\">Salinity</att>\n"
            + "            <att name=\"resolution\">null</att>\n"
            + "            <att name=\"standard_name\">sea_water_practical_salinity</att>\n"
            + "            <att name=\"units\">PSU</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>PSAL_ADJUSTED_QC</sourceName>\n"
            + "        <destinationName>PSAL_ADJUSTED_QC</destinationName>\n"
            + "        <dataType>char</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"conventions\">Argo reference table 2</att>\n"
            + "            <att name=\"long_name\">quality flag</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>PSAL_ADJUSTED_ERROR</sourceName>\n"
            + "        <destinationName>PSAL_ADJUSTED_ERROR</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">99999.0</att>\n"
            + "            <att name=\"C_format\">&#37;9.3f</att>\n"
            + "            <att name=\"FORTRAN_format\">F9.3</att>\n"
            + "            <att name=\"long_name\">Contains the error on the adjusted values as determined by the delayed mode QC process</att>\n"
            + "            <att name=\"resolution\" type=\"float\">0.001</att>\n"
            + "            <att name=\"units\">psu</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">1.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Statistics</att>\n"
            + "            <att name=\"resolution\">null</att>\n"
            + "            <att name=\"units\">PSU</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // GenerateDatasetsXml
    results =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {
                  "-verbose",
                  "EDDTableFromMultidimNcFiles",
                  dataDir,
                  ".*_prof\\.nc",
                  "",
                  "N_PROF, N_LEVELS",
                  "1440",
                  "^",
                  "_prof.nc$",
                  ".*",
                  "fileNumber", // just for test purposes
                  "true", // removeMVRows
                  "FLOAT_SERIAL_NO JULD", // sort files by
                  "",
                  "",
                  "",
                  "",
                  "-1", // defaultStandardizeWhat
                  "", // treatDimensionsAs
                  ""
                }, // cacheFromUrl
                false); // doIt loop?
    Test.ensureEqual(results, expected, "Unexpected results from GenerateDatasetsXml.doIt.");

    // Test.ensureEqual(results.substring(0, Math.min(results.length(),
    // expected.length())),
    // expected, "");

    // ensure it is ready-to-use by making a dataset from it
    // with one small change to addAttributes:

    String tr =
        "        <att name=\"cdm_data_type\">TrajectoryProfile</att>\n"
            + "        <att name=\"cdm_profile_variables\">profile_id, ???</att>\n"
            + "        <att name=\"cdm_trajectory_variables\">trajectory_id, ???</att>\n";
    int po = results.indexOf(tr);
    Test.ensureTrue(po > 0, "pre replaceAll:\n" + results);
    results = String2.replaceAll(results, tr, "        <att name=\"cdm_data_type\">Point</att>\n");
    String2.log("post replaceAll:\n" + results);

    // String tDatasetID = "_prof_8d3d_e39b_5d82";
    EDD.deleteCachedDatasetInfo(tDatasetID);
    EDD edd = EDDTableFromMultidimNcFiles.oneFromXmlFragment(null, results);
    Test.ensureEqual(edd.datasetID(), tDatasetID, "");
    Test.ensureEqual(edd.title(), "Argo float vertical profile", "");
    Test.ensureEqual(
        String2.toCSSVString(edd.dataVariableDestinationNames()),
        "fileNumber, DATA_TYPE, FORMAT_VERSION, HANDBOOK_VERSION, REFERENCE_DATE_TIME, DATE_CREATION, DATE_UPDATE, PLATFORM_NUMBER, PROJECT_NAME, PI_NAME, CYCLE_NUMBER, DIRECTION, DATA_CENTRE, DC_REFERENCE, DATA_STATE_INDICATOR, DATA_MODE, PLATFORM_TYPE, FLOAT_SERIAL_NO, FIRMWARE_VERSION, WMO_INST_TYPE, time, JULD_QC, JULD_LOCATION, latitude, longitude, POSITION_QC, POSITIONING_SYSTEM, PROFILE_PRES_QC, PROFILE_TEMP_QC, PROFILE_PSAL_QC, VERTICAL_SAMPLING_SCHEME, CONFIG_MISSION_NUMBER, PRES, PRES_QC, PRES_ADJUSTED, PRES_ADJUSTED_QC, PRES_ADJUSTED_ERROR, TEMP, TEMP_QC, TEMP_ADJUSTED, TEMP_ADJUSTED_QC, TEMP_ADJUSTED_ERROR, PSAL, PSAL_QC, PSAL_ADJUSTED, PSAL_ADJUSTED_QC, PSAL_ADJUSTED_ERROR",
        "");
  }

  /**
   * This tests the methods in this class.
   *
   * @throws Throwable if trouble
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testBasic(boolean deleteCachedInfo) throws Throwable {
    // String2.log("\n****************** EDDTableFromMultidimNcFiles.testBasic()
    // *****************\n");
    // testVerboseOn();
    int language = 0;
    String name, tName, results, tResults, expected, userDapQuery, tQuery;
    String error = "";
    EDV edv;
    String dir = EDStatic.fullTestCacheDirectory;
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard
    // to check min:sec.

    String id = "argoFloats";
    if (deleteCachedInfo) EDD.deleteCachedDatasetInfo(id);
    EDDTable eddTable = (EDDTable) EDDTestDataset.getArgoFloats();

    // *** test getting das for entire dataset
    String2.log(
        "\n****************** EDDTableFromMultidimNcFiles test das and dds for entire dataset\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_Entire", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  fileNumber {\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"File Number\";\n"
            + "  }\n"
            + "  data_type {\n"
            + "    String conventions \"Argo reference table 1\";\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Data type\";\n"
            + "  }\n"
            + "  format_version {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"File format version\";\n"
            + "  }\n"
            + "  handbook_version {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Data handbook version\";\n"
            + "  }\n"
            + "  reference_date_time {\n"
            + "    Float64 actual_range -6.31152e+8, -6.31152e+8;\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Date of reference for Julian days\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  date_creation {\n"
            + "    Float64 actual_range 1.397639727e+9, 1.467319628e+9;\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Date of file creation\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  date_update {\n"
            + "    Float64 actual_range 1.49100283e+9, 1.491241644e+9;\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Date of update of this file\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  platform_number {\n"
            + "    String cf_role \"trajectory_id\";\n"
            + "    String conventions \"WMO float identifier : A9IIIII\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Float unique identifier\";\n"
            + "  }\n"
            + "  project_name {\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Name of the project\";\n"
            + "  }\n"
            + "  pi_name {\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Name of the principal investigator\";\n"
            + "  }\n"
            + "  cycle_number {\n"
            + "    Int32 _FillValue 99999;\n"
            + "    Int32 actual_range 1, 110;\n"
            + "    String cf_role \"profile_id\";\n"
            + "    Float64 colorBarMaximum 200.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"0...N, 0 : launch cycle (if exists), 1 : first complete cycle\";\n"
            + "    String ioos_category \"Statistics\";\n"
            + "    String long_name \"Float cycle number\";\n"
            + "  }\n"
            + "  direction {\n"
            +
            // " String actual_range \"A\nA\";\n" +
            "    Float64 colorBarMaximum 360.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"A: ascending profiles, D: descending profiles\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Direction of the station profiles\";\n"
            + "  }\n"
            + "  data_center {\n"
            + "    String conventions \"Argo reference table 4\";\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Data centre in charge of float data processing\";\n"
            + "  }\n"
            + "  dc_reference {\n"
            + "    String conventions \"Data centre convention\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Station unique identifier in data centre\";\n"
            + "  }\n"
            + "  data_state_indicator {\n"
            + "    String conventions \"Argo reference table 6\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Degree of processing the data have passed through\";\n"
            + "  }\n"
            + "  data_mode {\n"
            +
            // " String actual_range \"A\nD\";\n" +
            "    String conventions \"R : real time; D : delayed mode; A : real time with adjustment\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Delayed mode or real time data\";\n"
            + "  }\n"
            + "  platform_type {\n"
            + "    String conventions \"Argo reference table 23\";\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Type of float\";\n"
            + "  }\n"
            + "  float_serial_no {\n"
            + "    Float64 colorBarMaximum 100.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Statistics\";\n"
            + "    String long_name \"Serial number of the float\";\n"
            + "  }\n"
            + "  firmware_version {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Instrument firmware version\";\n"
            + "  }\n"
            + "  wmo_inst_type {\n"
            + "    String conventions \"Argo reference table 8\";\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Coded instrument type\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range 1.397033909e+9, 1.491210572e+9;\n"
            + "    String axis \"T\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Julian day (UTC) of the station relative to REFERENCE_DATE_TIME\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  time_qc {\n"
            +
            // " String actual_range \"1\n1\";\n" +
            "    Float64 colorBarMaximum 150.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"Argo reference table 2\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Quality on date and time\";\n"
            + "  }\n"
            + "  time_location {\n"
            + "    Float64 actual_range 1.397033909e+9, 1.491213835e+9;\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Julian day (UTC) of the location relative to REFERENCE_DATE_TIME\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float64 _FillValue 99999.0;\n"
            + "    Float64 actual_range 3.035, 42.68257;\n"
            + "    String axis \"Y\";\n"
            + "    Float64 colorBarMaximum 90.0;\n"
            + "    Float64 colorBarMinimum -90.0;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude of the station, best estimate\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "    Float64 valid_max 90.0;\n"
            + "    Float64 valid_min -90.0;\n"
            + "  }\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 _FillValue 99999.0;\n"
            + "    Float64 actual_range -27.099, 8.141141666666666;\n"
            + "    String axis \"X\";\n"
            + "    Float64 colorBarMaximum 180.0;\n"
            + "    Float64 colorBarMinimum -180.0;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude of the station, best estimate\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "    Float64 valid_max 180.0;\n"
            + "    Float64 valid_min -180.0;\n"
            + "  }\n"
            + "  position_qc {\n"
            +
            // " String actual_range \"1\n1\";\n" +
            "    Float64 colorBarMaximum 150.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"Argo reference table 2\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Quality on position (latitude and longitude)\";\n"
            + "  }\n"
            + "  positioning_system {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Positioning system\";\n"
            + "  }\n"
            + "  profile_pres_qc {\n"
            +
            // " String actual_range \"A\nF\";\n" +
            "    Float64 colorBarMaximum 150.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"Argo reference table 2a\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Global quality flag of PRES profile\";\n"
            + "  }\n"
            + "  profile_temp_qc {\n"
            +
            // " String actual_range \"A\nF\";\n" +
            "    Float64 colorBarMaximum 150.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"Argo reference table 2a\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Global quality flag of TEMP profile\";\n"
            + "  }\n"
            + "  profile_psal_qc {\n"
            +
            // " String actual_range \"A\nF\";\n" +
            "    Float64 colorBarMaximum 150.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"Argo reference table 2a\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Global quality flag of PSAL profile\";\n"
            + "  }\n"
            + "  vertical_sampling_scheme {\n"
            + "    String conventions \"Argo reference table 16\";\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Vertical sampling scheme\";\n"
            + "  }\n"
            + "  config_mission_number {\n"
            + "    Int32 _FillValue 99999;\n"
            + "    Int32 actual_range 1, 16;\n"
            + "    Float64 colorBarMaximum 100.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"1...N, 1 : first complete mission\";\n"
            + "    String ioos_category \"Statistics\";\n"
            + "    String long_name \"Unique number denoting the missions performed by the float\";\n"
            + "  }\n"
            + "  pres {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    Float32 _FillValue 99999.0;\n"
            + "    Float32 actual_range 2.1, 2000.9;\n"
            + "    String axis \"Z\";\n"
            + "    String C_format \"%7.1f\";\n"
            + "    Float64 colorBarMaximum 5000.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String FORTRAN_format \"F7.1\";\n"
            + "    String ioos_category \"Sea Level\";\n"
            + "    String long_name \"Sea water pressure, equals 0 at sea-level\";\n"
            + "    String standard_name \"sea_water_pressure\";\n"
            + "    String units \"decibar\";\n"
            + "    Float32 valid_max 12000.0;\n"
            + "    Float32 valid_min 0.0;\n"
            + "  }\n"
            + "  pres_qc {\n"
            +
            // " String actual_range \"1\n4\";\n" +
            "    Float64 colorBarMaximum 150.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"Argo reference table 2\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"quality flag\";\n"
            + "  }\n"
            + "  pres_adjusted {\n"
            + "    Float32 _FillValue 99999.0;\n"
            + "    Float32 actual_range 3.6, 2000.8;\n"
            + "    String axis \"Z\";\n"
            + "    String C_format \"%7.1f\";\n"
            + "    Float64 colorBarMaximum 5000.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String FORTRAN_format \"F7.1\";\n"
            + "    String ioos_category \"Sea Level\";\n"
            + "    String long_name \"Sea water pressure, equals 0 at sea-level\";\n"
            + "    String standard_name \"sea_water_pressure\";\n"
            + "    String units \"decibar\";\n"
            + "    Float32 valid_max 12000.0;\n"
            + "    Float32 valid_min 0.0;\n"
            + "  }\n"
            + "  pres_adjusted_qc {\n"
            +
            // " String actual_range \" \n4\";\n" +
            "    Float64 colorBarMaximum 150.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"Argo reference table 2\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"quality flag\";\n"
            + "  }\n"
            + "  pres_adjusted_error {\n"
            + "    Float32 _FillValue 99999.0;\n"
            + "    Float32 actual_range 2.4, 2.4;\n"
            + "    String C_format \"%7.1f\";\n"
            + "    Float64 colorBarMaximum 50.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String FORTRAN_format \"F7.1\";\n"
            + "    String ioos_category \"Statistics\";\n"
            + "    String long_name \"Contains the error on the adjusted values as determined by the delayed mode QC process\";\n"
            + "    String units \"decibar\";\n"
            + "  }\n"
            + "  temp {\n"
            + "    Float32 _FillValue 99999.0;\n"
            + "    Float32 actual_range 3.466, 29.233;\n"
            + "    String C_format \"%9.3f\";\n"
            + "    Float64 colorBarMaximum 32.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String FORTRAN_format \"F9.3\";\n"
            + "    String ioos_category \"Temperature\";\n"
            + "    String long_name \"Sea temperature in-situ ITS-90 scale\";\n"
            + "    String standard_name \"sea_water_temperature\";\n"
            + "    String units \"degree_Celsius\";\n"
            + "    Float32 valid_max 40.0;\n"
            + "    Float32 valid_min -2.5;\n"
            + "  }\n"
            + "  temp_qc {\n"
            +
            // " String actual_range \"1\n4\";\n" +
            "    Float64 colorBarMaximum 150.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"Argo reference table 2\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"quality flag\";\n"
            + "  }\n"
            + "  temp_adjusted {\n"
            + "    Float32 _FillValue 99999.0;\n"
            + "    Float32 actual_range 3.466, 29.233;\n"
            + "    String C_format \"%9.3f\";\n"
            + "    Float64 colorBarMaximum 32.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String FORTRAN_format \"F9.3\";\n"
            + "    String ioos_category \"Temperature\";\n"
            + "    String long_name \"Sea temperature in-situ ITS-90 scale\";\n"
            + "    String standard_name \"sea_water_temperature\";\n"
            + "    String units \"degree_Celsius\";\n"
            + "    Float32 valid_max 40.0;\n"
            + "    Float32 valid_min -2.5;\n"
            + "  }\n"
            + "  temp_adjusted_qc {\n"
            +
            // " String actual_range \" \n4\";\n" +
            "    Float64 colorBarMaximum 150.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"Argo reference table 2\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"quality flag\";\n"
            + "  }\n"
            + "  temp_adjusted_error {\n"
            + "    Float32 _FillValue 99999.0;\n"
            + "    Float32 actual_range 0.002, 0.002;\n"
            + "    String C_format \"%9.3f\";\n"
            + "    Float64 colorBarMaximum 1.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String FORTRAN_format \"F9.3\";\n"
            + "    String ioos_category \"Statistics\";\n"
            + "    String long_name \"Contains the error on the adjusted values as determined by the delayed mode QC process\";\n"
            + "    String units \"degree_Celsius\";\n"
            + "  }\n"
            + "  psal {\n"
            + "    Float32 _FillValue 99999.0;\n"
            + "    Float32 actual_range 34.161, 38.693;\n"
            + "    String C_format \"%9.3f\";\n"
            + "    Float64 colorBarMaximum 37.0;\n"
            + "    Float64 colorBarMinimum 32.0;\n"
            + "    String FORTRAN_format \"F9.3\";\n"
            + "    String ioos_category \"Salinity\";\n"
            + "    String long_name \"Practical salinity\";\n"
            + "    String standard_name \"sea_water_practical_salinity\";\n"
            + "    String units \"PSU\";\n"
            + "    Float32 valid_max 41.0;\n"
            + "    Float32 valid_min 2.0;\n"
            + "  }\n"
            + "  psal_qc {\n"
            +
            // " String actual_range \"1\n4\";\n" +
            "    Float64 colorBarMaximum 150.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"Argo reference table 2\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"quality flag\";\n"
            + "  }\n"
            + "  psal_adjusted {\n"
            + "    Float32 _FillValue 99999.0;\n"
            + "    Float32 actual_range 34.1611, 36.4901;\n"
            + "    String C_format \"%9.3f\";\n"
            + "    Float64 colorBarMaximum 37.0;\n"
            + "    Float64 colorBarMinimum 32.0;\n"
            + "    String FORTRAN_format \"F9.3\";\n"
            + "    String ioos_category \"Salinity\";\n"
            + "    String long_name \"Practical salinity\";\n"
            + "    String standard_name \"sea_water_practical_salinity\";\n"
            + "    String units \"PSU\";\n"
            + "    Float32 valid_max 41.0;\n"
            + "    Float32 valid_min 2.0;\n"
            + "  }\n"
            + "  psal_adjusted_qc {\n"
            +
            // " String actual_range \" \n4\";\n" +
            "    Float64 colorBarMaximum 150.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"Argo reference table 2\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"quality flag\";\n"
            + "  }\n"
            + "  psal_adjusted_error {\n"
            + "    Float32 _FillValue 99999.0;\n"
            + "    Float32 actual_range 0.01, 0.01;\n"
            + "    String C_format \"%9.3f\";\n"
            + "    Float64 colorBarMaximum 1.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String FORTRAN_format \"F9.3\";\n"
            + "    String ioos_category \"Statistics\";\n"
            + "    String long_name \"Contains the error on the adjusted values as determined by the delayed mode QC process\";\n"
            + "    String units \"psu\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_altitude_proxy \"pres\";\n"
            + "    String cdm_data_type \"TrajectoryProfile\";\n"
            + "    String cdm_profile_variables \"cycle_number, data_type, format_version, handbook_version, reference_date_time, date_creation, date_update, direction, data_center, dc_reference, data_state_indicator, data_mode, firmware_version, wmo_inst_type, time, time_qc, time_location, latitude, longitude, position_qc, positioning_system, profile_pres_qc, profile_temp_qc, profile_psal_qc, vertical_sampling_scheme\";\n"
            + "    String cdm_trajectory_variables \"platform_number, project_name, pi_name, platform_type, float_serial_no\";\n"
            + "    String Conventions \"Argo-3.1, CF-1.6, COARDS, ACDD-1.3\";\n"
            + "    String creator_email \"support@argo.net\";\n"
            + "    String creator_name \"Argo\";\n"
            + "    String creator_url \"http://www.argo.net/\";\n"
            + "    Float64 Easternmost_Easting 8.141141666666666;\n"
            + "    String featureType \"TrajectoryProfile\";\n"
            + "    Float64 geospatial_lat_max 42.68257;\n"
            + "    Float64 geospatial_lat_min 3.035;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max 8.141141666666666;\n"
            + "    Float64 geospatial_lon_min -27.099;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    String history \"";
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

    // 2016-05-09T15:34:11Z (local files)
    // 2016-05-09T15:34:11Z
    // http://localhost:8080/cwexperimental/tabledap/testMultidimNc.das\";
    expected =
        "String infoUrl \"http://www.argo.net/\";\n"
            + "    String institution \"Argo\";\n"
            + "    String keywords \"adjusted, argo, array, assembly, best, centre, centres, charge, coded, CONFIG_MISSION_NUMBER, contains, coriolis, creation, currents, cycle, CYCLE_NUMBER, data, DATA_CENTRE, DATA_MODE, DATA_STATE_INDICATOR, DATA_TYPE, date, DATE_CREATION, DATE_UPDATE, day, days, DC_REFERENCE, degree, delayed, denoting, density, determined, direction, Earth Science > Oceans > Ocean Pressure > Water Pressure, Earth Science > Oceans > Ocean Temperature > Water Temperature, Earth Science > Oceans > Salinity/Density > Salinity, equals, error, estimate, file, firmware, FIRMWARE_VERSION, flag, float, FLOAT_SERIAL_NO, format, FORMAT_VERSION, gdac, geostrophic, global, handbook, HANDBOOK_VERSION, have, identifier, in-situ, instrument, investigator, its, its-90, JULD, JULD_LOCATION, JULD_QC, julian, latitude, level, longitude, missions, mode, name, number, ocean, oceanography, oceans, passed, performed, PI_NAME, PLATFORM_NUMBER, PLATFORM_TYPE, position, POSITION_QC, positioning, POSITIONING_SYSTEM, practical, pres, PRES_ADJUSTED, PRES_ADJUSTED_ERROR, PRES_ADJUSTED_QC, PRES_QC, pressure, principal, process, processing, profile, PROFILE_PRES_QC, PROFILE_PSAL_QC, PROFILE_TEMP_QC, profiles, project, PROJECT_NAME, psal, PSAL_ADJUSTED, PSAL_ADJUSTED_ERROR, PSAL_ADJUSTED_QC, PSAL_QC, quality, rdac, real, real time, real-time, realtime, reference, REFERENCE_DATE_TIME, regional, relative, salinity, sampling, scale, scheme, sea, sea level, sea-level, sea_water_practical_salinity, sea_water_pressure, sea_water_temperature, seawater, serial, situ, station, statistics, system, TEMP, TEMP_ADJUSTED, TEMP_ADJUSTED_ERROR, TEMP_ADJUSTED_QC, TEMP_QC, temperature, through, time, type, unique, update, values, version, vertical, VERTICAL_SAMPLING_SCHEME, water, WMO_INST_TYPE\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    Float64 Northernmost_Northing 42.68257;\n"
            + "    String references \"http://www.argodatamgt.org/Documentation\";\n"
            + "    String source \"Argo float\";\n"
            + "    String sourceUrl \"(local files)\";\n"
            + "    Float64 Southernmost_Northing 3.035;\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            +
            // " String subsetVariables \"platform_number, project_name, pi_name,
            // platform_type, float_serial_no, cycle_number, data_type, format_version,
            // handbook_version, reference_date_time, date_creation, date_update, direction,
            // data_center, dc_reference, data_state_indicator, data_mode, firmware_version,
            // wmo_inst_type, time, time_qc, time_location, latitude, longitude,
            // position_qc, positioning_system, profile_pres_qc, profile_temp_qc,
            // profile_psal_qc, vertical_sampling_scheme\";\n" +
            "    String summary \"Argo float vertical profiles from Coriolis Global Data Assembly Centres\n"
            + "(GDAC). Argo is an international collaboration that collects high-quality\n"
            + "temperature and salinity profiles from the upper 2000m of the ice-free\n"
            + "global ocean and currents from intermediate depths. The data come from\n"
            + "battery-powered autonomous floats that spend most of their life drifting\n"
            + "at depth where they are stabilised by being neutrally buoyant at the\n"
            + "\\\"parking depth\\\" pressure by having a density equal to the ambient pressure\n"
            + "and a compressibility that is less than that of sea water. At present there\n"
            + "are several models of profiling float used in Argo. All work in a similar\n"
            + "fashion but differ somewhat in their design characteristics. At typically\n"
            + "10-day intervals, the floats pump fluid into an external bladder and rise\n"
            + "to the surface over about 6 hours while measuring temperature and salinity.\n"
            + "Satellites or GPS determine the position of the floats when they surface,\n"
            + "and the floats transmit their data to the satellites. The bladder then\n"
            + "deflates and the float returns to its original density and sinks to drift\n"
            + "until the cycle is repeated. Floats are designed to make about 150 such\n"
            + "cycles.\n"
            + "Data Management URL: http://www.argodatamgt.org/Documentation\";\n"
            + "    String time_coverage_end \"2017-04-03T09:09:32Z\";\n"
            + "    String time_coverage_start \"2014-04-09T08:58:29Z\";\n"
            + "    String title \"Argo Float Vertical Profiles\";\n"
            + "    String user_manual_version \"3.1\";\n"
            + "    Float64 Westernmost_Easting -27.099;\n"
            + "  }\n"
            + "}\n";
    int tPo = results.indexOf(expected.substring(0, 40));
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
        expected,
        "results=\n" + results);

    // *** test getting dds for entire dataset
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_Entire", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    String fileNumber;\n"
            + "    String data_type;\n"
            + "    String format_version;\n"
            + "    String handbook_version;\n"
            + "    Float64 reference_date_time;\n"
            + "    Float64 date_creation;\n"
            + "    Float64 date_update;\n"
            + "    String platform_number;\n"
            + "    String project_name;\n"
            + "    String pi_name;\n"
            + "    Int32 cycle_number;\n"
            + "    String direction;\n"
            + "    String data_center;\n"
            + "    String dc_reference;\n"
            + "    String data_state_indicator;\n"
            + "    String data_mode;\n"
            + "    String platform_type;\n"
            + "    String float_serial_no;\n"
            + "    String firmware_version;\n"
            + "    String wmo_inst_type;\n"
            + "    Float64 time;\n"
            + "    String time_qc;\n"
            + "    Float64 time_location;\n"
            + "    Float64 latitude;\n"
            + "    Float64 longitude;\n"
            + "    String position_qc;\n"
            + "    String positioning_system;\n"
            + "    String profile_pres_qc;\n"
            + "    String profile_temp_qc;\n"
            + "    String profile_psal_qc;\n"
            + "    String vertical_sampling_scheme;\n"
            + "    Int32 config_mission_number;\n"
            + "    Float32 pres;\n"
            + "    String pres_qc;\n"
            + "    Float32 pres_adjusted;\n"
            + "    String pres_adjusted_qc;\n"
            + "    Float32 pres_adjusted_error;\n"
            + "    Float32 temp;\n"
            + "    String temp_qc;\n"
            + "    Float32 temp_adjusted;\n"
            + "    String temp_adjusted_qc;\n"
            + "    Float32 temp_adjusted_error;\n"
            + "    Float32 psal;\n"
            + "    String psal_qc;\n"
            + "    Float32 psal_adjusted;\n"
            + "    String psal_adjusted_qc;\n"
            + "    Float32 psal_adjusted_error;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // *** test make data files
    String2.log("\n****************** EDDTableFromMultidimNcFiles.test make DATA FILES\n");

    // .csv for one lat,lon 26.587,154.853
    userDapQuery =
        ""
            +
            // "&longitude=154.853&latitude=26.587";
            // "&latitude=42.50334333333333&longitude=7.837398333333333";
            "&pres=804.9&cycle_number=53";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_1profile", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "fileNumber,data_type,format_version,handbook_version,reference_date_time,date_creation,date_update,platform_number,project_name,pi_name,cycle_number,direction,data_center,dc_reference,data_state_indicator,data_mode,platform_type,float_serial_no,firmware_version,wmo_inst_type,time,time_qc,time_location,latitude,longitude,position_qc,positioning_system,profile_pres_qc,profile_temp_qc,profile_psal_qc,vertical_sampling_scheme,config_mission_number,pres,pres_qc,pres_adjusted,pres_adjusted_qc,pres_adjusted_error,temp,temp_qc,temp_adjusted,temp_adjusted_qc,temp_adjusted_error,psal,psal_qc,psal_adjusted,psal_adjusted_qc,psal_adjusted_error\n"
            + ",,,,UTC,UTC,UTC,,,,,,,,,,,,,,UTC,,UTC,degrees_north,degrees_east,,,,,,,,decibar,,decibar,,decibar,degree_Celsius,,degree_Celsius,,degree_Celsius,PSU,,PSU,,psu\n"
            + "6902733,Argo profile,3.1,1.2,1950-01-01T00:00:00Z,2016-06-30T20:47:08Z,2017-03-31T23:27:10Z,6902733,NAOS,Fabrizio D'ortenzio,53,A,IF,,2B,R,PROVOR_III,OIN-12-RA-S31-001,1.07,836,2017-03-31T15:35:00Z,1,2017-03-31T16:04:13Z,42.50334333333333,7.837398333333333,1,GPS,A,A,A,\"Primary sampling: averaged [2s sampling, 10dbar average from 1000dbar to 250dbar;2s samp., 1dbar avg from 250dbar to 10dbar;2s samp., 1dbar avg from 10dbar to 2.2dbar]\",12,804.9,1,NaN,\" \",NaN,13.348,1,NaN,\" \",NaN,38.556,1,NaN,\" \",NaN\n";
    // "2901175,Argo
    // profile,3.1,1.2,1950-01-01T00:00:00Z,2009-04-22T12:19:13Z,2016-04-15T20:47:22Z,2901175,CHINA
    // ARGO PROJECT,JIANPING
    // XU,256,A,HZ,0066_80617_256,2B,A,APEX,4136,013108,846,2016-04-14T10:43:08Z,1,2016-04-14T10:43:08Z,26.587,154.853,1,ARGOS,A,A,A,Primary
    // sampling:
    // discrete,1,4.6,1,4.0,1,NaN,23.123,1,23.123,1,NaN,35.288,1,35.288,1,NaN\n" +
    // "2901175,Argo
    // profile,3.1,1.2,1950-01-01T00:00:00Z,2009-04-22T12:19:13Z,2016-04-15T20:47:22Z,2901175,CHINA
    // ARGO PROJECT,JIANPING
    // XU,256,A,HZ,0066_80617_256,2B,A,APEX,4136,013108,846,2016-04-14T10:43:08Z,1,2016-04-14T10:43:08Z,26.587,154.853,1,ARGOS,A,A,A,Primary
    // sampling:
    // discrete,1,9.7,1,9.1,1,NaN,23.131,1,23.131,1,NaN,35.289,1,35.289,1,NaN\n" +
    // "2901175,Argo
    // profile,3.1,1.2,1950-01-01T00:00:00Z,2009-04-22T12:19:13Z,2016-04-15T20:47:22Z,2901175,CHINA
    // ARGO PROJECT,JIANPING
    // XU,256,A,HZ,0066_80617_256,2B,A,APEX,4136,013108,846,2016-04-14T10:43:08Z,1,2016-04-14T10:43:08Z,26.587,154.853,1,ARGOS,A,A,A,Primary
    // sampling:
    // discrete,1,20.5,1,19.9,1,NaN,23.009,1,23.009,1,NaN,35.276,1,35.276,1,NaN\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    // expected = //last 3 lines
    // "2901175,Argo
    // profile,3.1,1.2,1950-01-01T00:00:00Z,2009-04-22T12:19:13Z,2016-04-15T20:47:22Z,2901175,CHINA
    // ARGO PROJECT,JIANPING
    // XU,256,A,HZ,0066_80617_256,2B,A,APEX,4136,013108,846,2016-04-14T10:43:08Z,1,2016-04-14T10:43:08Z,26.587,154.853,1,ARGOS,A,A,A,Primary
    // sampling:
    // discrete,1,1850.0,1,1849.4,1,NaN,2.106,1,2.106,1,NaN,34.604,1,34.604,1,NaN\n"
    // +
    // "2901175,Argo
    // profile,3.1,1.2,1950-01-01T00:00:00Z,2009-04-22T12:19:13Z,2016-04-15T20:47:22Z,2901175,CHINA
    // ARGO PROJECT,JIANPING
    // XU,256,A,HZ,0066_80617_256,2B,A,APEX,4136,013108,846,2016-04-14T10:43:08Z,1,2016-04-14T10:43:08Z,26.587,154.853,1,ARGOS,A,A,A,Primary
    // sampling:
    // discrete,1,1899.9,1,1899.3,1,NaN,2.055,1,2.055,1,NaN,34.612,1,34.612,1,NaN\n"
    // +
    // "2901175,Argo
    // profile,3.1,1.2,1950-01-01T00:00:00Z,2009-04-22T12:19:13Z,2016-04-15T20:47:22Z,2901175,CHINA
    // ARGO PROJECT,JIANPING
    // XU,256,A,HZ,0066_80617_256,2B,A,APEX,4136,013108,846,2016-04-14T10:43:08Z,1,2016-04-14T10:43:08Z,26.587,154.853,1,ARGOS,A,A,A,Primary
    // sampling:
    // discrete,1,1950.0,1,1949.4,1,NaN,2.014,1,2.014,1,NaN,34.617,1,34.617,1,NaN\n";
    // Test.ensureEqual(results.substring(results.length() - expected.length()),
    // expected, "\nresults=\n" + results);

    // .csv for one lat,lon via lon > <
    userDapQuery = "" + "&longitude>7&longitude<=154.854&cycle_number=53&pres>970";
    // "&longitude>154.852&longitude<=154.854";
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            dir,
            eddTable.className() + "_1StationGTLT",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "fileNumber,data_type,format_version,handbook_version,reference_date_time,date_creation,date_update,platform_number,project_name,pi_name,cycle_number,direction,data_center,dc_reference,data_state_indicator,data_mode,platform_type,float_serial_no,firmware_version,wmo_inst_type,time,time_qc,time_location,latitude,longitude,position_qc,positioning_system,profile_pres_qc,profile_temp_qc,profile_psal_qc,vertical_sampling_scheme,config_mission_number,pres,pres_qc,pres_adjusted,pres_adjusted_qc,pres_adjusted_error,temp,temp_qc,temp_adjusted,temp_adjusted_qc,temp_adjusted_error,psal,psal_qc,psal_adjusted,psal_adjusted_qc,psal_adjusted_error\n"
            + ",,,,UTC,UTC,UTC,,,,,,,,,,,,,,UTC,,UTC,degrees_north,degrees_east,,,,,,,,decibar,,decibar,,decibar,degree_Celsius,,degree_Celsius,,degree_Celsius,PSU,,PSU,,psu\n"
            + "6902733,Argo profile,3.1,1.2,1950-01-01T00:00:00Z,2016-06-30T20:47:08Z,2017-03-31T23:27:10Z,6902733,NAOS,Fabrizio D'ortenzio,53,A,IF,,2B,R,PROVOR_III,OIN-12-RA-S31-001,1.07,836,2017-03-31T15:35:00Z,1,2017-03-31T16:04:13Z,42.50334333333333,7.837398333333333,1,GPS,A,A,A,\"Primary sampling: averaged [2s sampling, 10dbar average from 1000dbar to 250dbar;2s samp., 1dbar avg from 250dbar to 10dbar;2s samp., 1dbar avg from 10dbar to 2.2dbar]\",12,974.9,1,NaN,\" \",NaN,13.227,1,NaN,\" \",NaN,38.522,1,NaN,\" \",NaN\n"
            + //
            "6902733,Argo profile,3.1,1.2,1950-01-01T00:00:00Z,2016-06-30T20:47:08Z,2017-03-31T23:27:10Z,6902733,NAOS,Fabrizio D'ortenzio,53,A,IF,,2B,R,PROVOR_III,OIN-12-RA-S31-001,1.07,836,2017-03-31T15:35:00Z,1,2017-03-31T16:04:13Z,42.50334333333333,7.837398333333333,1,GPS,A,A,A,\"Primary sampling: averaged [2s sampling, 10dbar average from 1000dbar to 250dbar;2s samp., 1dbar avg from 250dbar to 10dbar;2s samp., 1dbar avg from 10dbar to 2.2dbar]\",12,985.1,1,NaN,\" \",NaN,13.226,1,NaN,\" \",NaN,38.521,1,NaN,\" \",NaN\n"
            + //
            "6902733,Argo profile,3.1,1.2,1950-01-01T00:00:00Z,2016-06-30T20:47:08Z,2017-03-31T23:27:10Z,6902733,NAOS,Fabrizio D'ortenzio,53,A,IF,,2B,R,PROVOR_III,OIN-12-RA-S31-001,1.07,836,2017-03-31T15:35:00Z,1,2017-03-31T16:04:13Z,42.50334333333333,7.837398333333333,1,GPS,A,A,A,\"Primary sampling: averaged [2s sampling, 10dbar average from 1000dbar to 250dbar;2s samp., 1dbar avg from 250dbar to 10dbar;2s samp., 1dbar avg from 10dbar to 2.2dbar]\",12,990.1,1,NaN,\" \",NaN,13.226,1,NaN,\" \",NaN,38.522,1,NaN,\" \",NaN\n";
    // "2901175,Argo
    // profile,3.1,1.2,1950-01-01T00:00:00Z,2009-04-22T12:19:13Z,2016-04-15T20:47:22Z,2901175,CHINA
    // ARGO PROJECT,JIANPING
    // XU,256,A,HZ,0066_80617_256,2B,A,APEX,4136,013108,846,2016-04-14T10:43:08Z,1,2016-04-14T10:43:08Z,26.587,154.853,1,ARGOS,A,A,A,Primary
    // sampling:
    // discrete,1,4.6,1,4.0,1,NaN,23.123,1,23.123,1,NaN,35.288,1,35.288,1,NaN\n" +
    // "2901175,Argo
    // profile,3.1,1.2,1950-01-01T00:00:00Z,2009-04-22T12:19:13Z,2016-04-15T20:47:22Z,2901175,CHINA
    // ARGO PROJECT,JIANPING
    // XU,256,A,HZ,0066_80617_256,2B,A,APEX,4136,013108,846,2016-04-14T10:43:08Z,1,2016-04-14T10:43:08Z,26.587,154.853,1,ARGOS,A,A,A,Primary
    // sampling:
    // discrete,1,9.7,1,9.1,1,NaN,23.131,1,23.131,1,NaN,35.289,1,35.289,1,NaN\n" +
    // "2901175,Argo
    // profile,3.1,1.2,1950-01-01T00:00:00Z,2009-04-22T12:19:13Z,2016-04-15T20:47:22Z,2901175,CHINA
    // ARGO PROJECT,JIANPING
    // XU,256,A,HZ,0066_80617_256,2B,A,APEX,4136,013108,846,2016-04-14T10:43:08Z,1,2016-04-14T10:43:08Z,26.587,154.853,1,ARGOS,A,A,A,Primary
    // sampling:
    // discrete,1,20.5,1,19.9,1,NaN,23.009,1,23.009,1,NaN,35.276,1,35.276,1,NaN\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    // expected = //last 3 lines
    // "2901175,Argo
    // profile,3.1,1.2,1950-01-01T00:00:00Z,2009-04-22T12:19:13Z,2016-04-15T20:47:22Z,2901175,CHINA
    // ARGO PROJECT,JIANPING
    // XU,256,A,HZ,0066_80617_256,2B,A,APEX,4136,013108,846,2016-04-14T10:43:08Z,1,2016-04-14T10:43:08Z,26.587,154.853,1,ARGOS,A,A,A,Primary
    // sampling:
    // discrete,1,1850.0,1,1849.4,1,NaN,2.106,1,2.106,1,NaN,34.604,1,34.604,1,NaN\n"
    // +
    // "2901175,Argo
    // profile,3.1,1.2,1950-01-01T00:00:00Z,2009-04-22T12:19:13Z,2016-04-15T20:47:22Z,2901175,CHINA
    // ARGO PROJECT,JIANPING
    // XU,256,A,HZ,0066_80617_256,2B,A,APEX,4136,013108,846,2016-04-14T10:43:08Z,1,2016-04-14T10:43:08Z,26.587,154.853,1,ARGOS,A,A,A,Primary
    // sampling:
    // discrete,1,1899.9,1,1899.3,1,NaN,2.055,1,2.055,1,NaN,34.612,1,34.612,1,NaN\n"
    // +
    // "2901175,Argo
    // profile,3.1,1.2,1950-01-01T00:00:00Z,2009-04-22T12:19:13Z,2016-04-15T20:47:22Z,2901175,CHINA
    // ARGO PROJECT,JIANPING
    // XU,256,A,HZ,0066_80617_256,2B,A,APEX,4136,013108,846,2016-04-14T10:43:08Z,1,2016-04-14T10:43:08Z,26.587,154.853,1,ARGOS,A,A,A,Primary
    // sampling:
    // discrete,1,1950.0,1,1949.4,1,NaN,2.014,1,2.014,1,NaN,34.617,1,34.617,1,NaN\n";
    // Test.ensureEqual(results.substring(results.length() - expected.length()),
    // expected, "\nresults=\n" + results);

    // .csv for test requesting scalar var
    userDapQuery = "data_type&data_type=~\".*go.*\"";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_scalar", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected = "data_type\n" + "\n" + "Argo profile\n" + "Argo profile\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .csv for test requesting distinct
    userDapQuery = "pres&pres>10&pres<10.5&distinct()";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_scalar", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected = "pres\n" + "decibar\n" + "10.1\n" + "10.2\n" + "10.3\n" + "10.4\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
  }

  /**
   * testGenerateDatasetsXml with a SeaDataNet file, specifically the generation of sdn_P02_urn from
   * sdn_parameter_urn attributes.
   */
  @org.junit.jupiter.api.Test
  void testGenerateDatasetsXmlSeaDataNet() throws Throwable {
    // testVerboseOn();
    // debugMode = true;
    String dataDir =
        File2.addSlash(
            Path.of(EDDTableFromMultidimNcFilesTests.class.getResource("/data/sdn/").toURI())
                .toString());
    String fileNameRegex = "netCDF_timeseries_tidegauge\\.nc";
    String useDimensionsCSV = "INSTANCE, MAXT";
    String results =
        EDDTableFromMultidimNcFiles.generateDatasetsXml(
            dataDir,
            fileNameRegex,
            "",
            useDimensionsCSV, // dimensions
            1440,
            "",
            "",
            "",
            "", // just for test purposes; station is already a column in the file
            true, // removeMVRows
            "", // sortFilesBy
            "",
            "",
            "",
            "",
            -1, // defaultStandardizeWhat
            "", // treatDimensionsAs
            null, // cacheFromUrl
            null);
    String2.setClipboardString(results);

    String tDatasetID =
        EDDTableFromMultidimNcFiles.suggestDatasetID(dataDir + fileNameRegex + useDimensionsCSV);
    String expected =
        "<dataset type=\"EDDTableFromMultidimNcFiles\" datasetID=\""
            + tDatasetID
            + "\" active=\"true\">\n"
            + "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n"
            + "    <updateEveryNMillis>10000</updateEveryNMillis>\n"
            + "    <fileDir>"
            + dataDir
            + "</fileDir>\n"
            + "    <fileNameRegex>"
            + fileNameRegex
            + "</fileNameRegex>\n"
            + "    <recursive>true</recursive>\n"
            + "    <pathRegex>.*</pathRegex>\n"
            + "    <metadataFrom>last</metadataFrom>\n"
            + "    <standardizeWhat>0</standardizeWhat>\n"
            + "    <removeMVRows>true</removeMVRows>\n"
            + "    <sortFilesBySourceNames></sortFilesBySourceNames>\n"
            + "    <fileTableInMemory>false</fileTableInMemory>\n"
            + "    <!-- sourceAttributes>\n"
            + "        <att name=\"Conventions\">SeaDataNet_1.0 CF 1.6</att>\n"
            + "        <att name=\"date_update\">2015-05-13T18:28+0200</att>\n"
            + "        <att name=\"featureType\">timeSeries</att>\n"
            + "        <att name=\"title\">NetCDF TIMESERIES - Generated by NEMO, version 1.6.0</att>\n"
            + "    </sourceAttributes -->\n"
            + "    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n"
            + "        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n"
            + "        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n"
            + "    -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cdm_data_type\">TimeSeries</att>\n"
            + "        <att name=\"cdm_timeseries_variables\">station_id, latitude, longitude, ???</att>\n"
            + "        <att name=\"Conventions\">SeaDataNet_1.0, CF-1.10, COARDS, ACDD-1.3</att>\n"
            + "        <att name=\"creator_name\">SeaDataNet</att>\n"
            + "        <att name=\"creator_url\">https://www.seadatanet.org/</att>\n"
            + "        <att name=\"infoUrl\">https://www.seadatanet.org/</att>\n"
            + "        <att name=\"institution\">SeaDataNet</att>\n"
            + "        <att name=\"keywords\">above, ASLVZZ01, ASLVZZ01_SEADATANET_QC, bathymetric, bathymetry, below, cdi, code, common, crs, data, depth, DEPTH_SEADATANET_QC, directory, earth, Earth Science &gt; Oceans &gt; Bathymetry/Seafloor Topography &gt; Bathymetry, Earth Science &gt; Oceans &gt; Ocean Pressure &gt; Water Pressure, Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Water Temperature, Earth Science &gt; Oceans &gt; Sea Surface Topography &gt; Sea Surface Height, Earth Science &gt; Solid Earth &gt; Geodetics/Gravity &gt; Geoid Properties, european, flag, floor, format, generated, geodetics, geoid, gravity, height, identifier, latitude, level, list, longitude, marine, measurement, nemo, network, numbers, ocean, oceans, organisations, POSITION_SEADATANET_QC, PRESPR01, PRESPR01_SEADATANET_QC, pressure, properties, quality, science, SDN_BOT_DEPTH, SDN_CRUISE, SDN_EDMO_CODE, SDN_LOCAL_CDI_ID, SDN_STATION, sea, sea level, sea_floor_depth_below_sea_surface, sea_surface_height_above_geoid, sea_water_pressure, sea_water_temperature, seadatanet, seafloor, seawater, site, solid, station, statistics, supplier, surface, suva, temperature, TEMPPR01, TEMPPR01_SEADATANET_QC, time, TIME_SEADATANET_QC, timeseries, topography, version, water</att>\n"
            + "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n"
            + "        <att name=\"license\">[standard]</att>\n"
            + "        <att name=\"sourceUrl\">(local files)</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"subsetVariables\">SDN_EDMO_CODE, SDN_CRUISE, SDN_STATION, SDN_LOCAL_CDI_ID, SDN_BOT_DEPTH, longitude, latitude, POSITION_SEADATANET_QC, crs, TIME_SEADATANET_QC, depth, DEPTH_SEADATANET_QC, ASLVZZ01_SEADATANET_QC, TEMPPR01_SEADATANET_QC, PRESPR01_SEADATANET_QC</att>\n"
            + "        <att name=\"summary\">Network Common Data Format (NetCDF) TIMESERIES - Generated by NEMO, version 1.6.0</att>\n"
            + "        <att name=\"title\">NetCDF TIMESERIES, Generated by NEMO, version 1.6.0</att>\n"
            + "    </addAttributes>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>SDN_EDMO_CODE</sourceName>\n"
            + "        <destinationName>SDN_EDMO_CODE</destinationName>\n"
            + "        <dataType>int</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">European Directory of Marine Organisations code for the CDI supplier</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"int\">2147483647</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>SDN_CRUISE</sourceName>\n"
            + "        <destinationName>SDN_CRUISE</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"availability\">publicly</att>\n"
            + "            <att name=\"chiefScientist\">FICHEZ Renaud</att>\n"
            + "            <att name=\"country\">35</att>\n"
            + "            <att name=\"dataCentre\">FI</att>\n"
            + "            <att name=\"endDate\">1998-07-31T00:00:00.000</att>\n"
            + "            <att name=\"laboratories\">ORSTOM NOUMEA, University of the South Pacific (USP)</att>\n"
            + "            <att name=\"long_name\">SUVA 1</att>\n"
            + "            <att name=\"regionName\">Southwest Pacific Ocean (140W)</att>\n"
            + "            <att name=\"shipCode\">35AY</att>\n"
            + "            <att name=\"shipName\">Alis</att>\n"
            + "            <att name=\"startDate\">1998-07-20T00:00:00.000</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>SDN_STATION</sourceName>\n"
            + "        <destinationName>SDN_STATION</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">List of station numbers</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Statistics</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>SDN_LOCAL_CDI_ID</sourceName>\n"
            + "        <destinationName>SDN_LOCAL_CDI_ID</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"cf_role\">timeseries_id</att>\n"
            + "            <att name=\"long_name\">SeaDataNet CDI identifier</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>SDN_BOT_DEPTH</sourceName>\n"
            + "        <destinationName>SDN_BOT_DEPTH</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">-999.0</att>\n"
            + "            <att name=\"long_name\">Bathymetric depth at  measurement site</att>\n"
            + "            <att name=\"sdn_parameter_name\">Sea-floor depth (below instantaneous sea level) {bathymetric depth} in the water body</att>\n"
            + "            <att name=\"sdn_parameter_urn\">SDN:P01::MBANZZZZ</att>\n"
            + "            <att name=\"sdn_uom_name\">Metres</att>\n"
            + "            <att name=\"sdn_uom_urn\">SDN:P06::ULAA</att>\n"
            + "            <att name=\"standard_name\">sea_floor_depth_below_sea_surface</att>\n"
            + "            <att name=\"units\">meters</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">8000.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-8000.0</att>\n"
            + "            <att name=\"colorBarPalette\">TopographyDepth</att>\n"
            + "            <att name=\"ioos_category\">Bathymetry</att>\n"
            + "            <att name=\"sdn_P02_urn\">SDN:P02::MBAN</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>LONGITUDE</sourceName>\n"
            + "        <destinationName>longitude</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"double\">-99999.0</att>\n"
            + "            <att name=\"ancillary_variables\">POSITION_SEADATANET_QC</att>\n"
            + "            <att name=\"axis\">X</att>\n"
            + "            <att name=\"grid_mapping\">crs</att>\n"
            + "            <att name=\"long_name\">Longitude</att>\n"
            + "            <att name=\"sdn_parameter_name\">Longitude east</att>\n"
            + "            <att name=\"sdn_parameter_urn\">SDN:P01::ALONZZ01</att>\n"
            + "            <att name=\"sdn_uom_name\">Degrees east</att>\n"
            + "            <att name=\"sdn_uom_urn\">SDN:P06::DEGE</att>\n"
            + "            <att name=\"standard_name\">longitude</att>\n"
            + "            <att name=\"units\">degrees_east</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n"
            + "            <att name=\"grid_mapping\">null</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"sdn_P02_urn\">SDN:P02::ALAT</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>LATITUDE</sourceName>\n"
            + "        <destinationName>latitude</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"double\">-99999.0</att>\n"
            + "            <att name=\"ancillary_variables\">POSITION_SEADATANET_QC</att>\n"
            + "            <att name=\"axis\">Y</att>\n"
            + "            <att name=\"grid_mapping\">crs</att>\n"
            + "            <att name=\"long_name\">Latitude</att>\n"
            + "            <att name=\"sdn_parameter_name\">Latitude north</att>\n"
            + "            <att name=\"sdn_parameter_urn\">SDN:P01::ALATZZ01</att>\n"
            + "            <att name=\"sdn_uom_name\">Degrees north</att>\n"
            + "            <att name=\"sdn_uom_urn\">SDN:P06::DEGN</att>\n"
            + "            <att name=\"standard_name\">latitude</att>\n"
            + "            <att name=\"units\">degrees_north</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n"
            + "            <att name=\"grid_mapping\">null</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"sdn_P02_urn\">SDN:P02::ALAT</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>POSITION_SEADATANET_QC</sourceName>\n"
            + "        <destinationName>POSITION_SEADATANET_QC</destinationName>\n"
            + "        <dataType>byte</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"byte\">57</att>\n"
            + "            <att name=\"Conventions\">SeaDataNet measurand qualifier flags</att>\n"
            + "            <att name=\"flag_meanings\">no_quality_control good_value probably_good_value probably_bad_value bad_value changed_value value_below_detection value_in_excess interpolated_value missing_value value_phenomenon_uncertain</att>\n"
            + "            <att name=\"flag_values\" type=\"byteList\">48 49 50 51 52 53 54 55 56 57 65</att>\n"
            + "            <att name=\"long_name\">SeaDataNet quality flag</att>\n"
            + "            <att name=\"sdn_conventions_urn\">SDN:L20::</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">80.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>crs</sourceName>\n"
            + "        <destinationName>crs</destinationName>\n"
            + "        <dataType>int</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"epsg_code\">EPSG:4326</att>\n"
            + "            <att name=\"grid_mapping_name\">latitude_longitude</att>\n"
            + "            <att name=\"inverse_flattening\" type=\"double\">298.257223563</att>\n"
            + "            <att name=\"semi_major_axis\" type=\"double\">6378137.0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"int\">2147483647</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">CRS</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>TIME</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"double\">-99999.0</att>\n"
            + "            <att name=\"ancillary_variables\">TIME_SEADATANET_QC</att>\n"
            + "            <att name=\"axis\">T</att>\n"
            + "            <att name=\"calendar\">julian</att>\n"
            + "            <att name=\"long_name\">Chronological Julian Date</att>\n"
            + "            <att name=\"sdn_parameter_name\">Julian Date (chronological)</att>\n"
            + "            <att name=\"sdn_parameter_urn\">SDN:P01::CJDY1101</att>\n"
            + "            <att name=\"sdn_uom_name\">Days</att>\n"
            + "            <att name=\"sdn_uom_urn\">SDN:P06::UTAA</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"units\">days since -4713-01-01T00:00:00Z</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"sdn_P02_urn\">SDN:P02::AYMD</att>\n"
            + "            <att name=\"units\">days since -4712-01-01T00:00:00Z</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>TIME_SEADATANET_QC</sourceName>\n"
            + "        <destinationName>TIME_SEADATANET_QC</destinationName>\n"
            + "        <dataType>byte</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"byte\">57</att>\n"
            + "            <att name=\"Conventions\">SeaDataNet measurand qualifier flags</att>\n"
            + "            <att name=\"flag_meanings\">no_quality_control good_value probably_good_value probably_bad_value bad_value changed_value value_below_detection value_in_excess interpolated_value missing_value value_phenomenon_uncertain</att>\n"
            + "            <att name=\"flag_values\" type=\"byteList\">48 49 50 51 52 53 54 55 56 57 65</att>\n"
            + "            <att name=\"long_name\">SeaDataNet quality flag</att>\n"
            + "            <att name=\"sdn_conventions_urn\">SDN:L20::</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">80.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>DEPTH</sourceName>\n"
            + "        <destinationName>depth</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"double\">-99999.0</att>\n"
            + "            <att name=\"ancillary_variables\">DEPTH_SEADATANET_QC</att>\n"
            + "            <att name=\"axis\">Z</att>\n"
            + "            <att name=\"long_name\">Depth</att>\n"
            + "            <att name=\"positive\">down</att>\n"
            + "            <att name=\"sdn_parameter_name\">Depth below surface of the water body</att>\n"
            + "            <att name=\"sdn_parameter_urn\">SDN:P01::ADEPZZ01</att>\n"
            + "            <att name=\"sdn_uom_name\">Metres</att>\n"
            + "            <att name=\"sdn_uom_urn\">SDN:P06::ULAA</att>\n"
            + "            <att name=\"standard_name\">depth</att>\n"
            + "            <att name=\"units\">meters</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">8000.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-8000.0</att>\n"
            + "            <att name=\"colorBarPalette\">TopographyDepth</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"sdn_P02_urn\">SDN:P02::AHGT</att>\n"
            + "            <att name=\"units\">m</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>DEPTH_SEADATANET_QC</sourceName>\n"
            + "        <destinationName>DEPTH_SEADATANET_QC</destinationName>\n"
            + "        <dataType>byte</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"byte\">57</att>\n"
            + "            <att name=\"Conventions\">SeaDataNet measurand qualifier flags</att>\n"
            + "            <att name=\"flag_meanings\">no_quality_control good_value probably_good_value probably_bad_value bad_value changed_value value_below_detection value_in_excess interpolated_value missing_value value_phenomenon_uncertain</att>\n"
            + "            <att name=\"flag_values\" type=\"byteList\">48 49 50 51 52 53 54 55 56 57 65</att>\n"
            + "            <att name=\"long_name\">SeaDataNet quality flag</att>\n"
            + "            <att name=\"sdn_conventions_urn\">SDN:L20::</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">80.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "            <att name=\"standard_name\">depth</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>ASLVZZ01</sourceName>\n"
            + "        <destinationName>ASLVZZ01</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"double\">-99999.0</att>\n"
            + "            <att name=\"ancillary_variables\">ASLVZZ01_SEADATANET_QC</att>\n"
            + "            <att name=\"coordinates\">LONGITUDE LATITUDE TIME DEPTH</att>\n"
            + "            <att name=\"long_name\">Sea level</att>\n"
            + "            <att name=\"sdn_parameter_name\">Surface elevation (unspecified datum) of the water body</att>\n"
            + "            <att name=\"sdn_parameter_urn\">SDN:P01::ASLVZZ01</att>\n"
            + "            <att name=\"sdn_uom_name\">meter</att>\n"
            + "            <att name=\"sdn_uom_urn\">SDN:P06::ULAA</att>\n"
            + "            <att name=\"standard_name\">sea_surface_height_above_geoid</att>\n"
            + "            <att name=\"units\">meter</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">2.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-2.0</att>\n"
            + "            <att name=\"coordinates\">null</att>\n"
            + "            <att name=\"ioos_category\">Sea Level</att>\n"
            + "            <att name=\"sdn_P02_urn\">SDN:P02::ASLV</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>ASLVZZ01_SEADATANET_QC</sourceName>\n"
            + "        <destinationName>ASLVZZ01_SEADATANET_QC</destinationName>\n"
            + "        <dataType>byte</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"byte\">57</att>\n"
            + "            <att name=\"Conventions\">SeaDataNet measurand qualifier flags</att>\n"
            + "            <att name=\"flag_meanings\">no_quality_control good_value probably_good_value probably_bad_value bad_value changed_value value_below_detection value_in_excess interpolated_value missing_value value_phenomenon_uncertain</att>\n"
            + "            <att name=\"flag_values\" type=\"byteList\">48 49 50 51 52 53 54 55 56 57 65</att>\n"
            + "            <att name=\"long_name\">SeaDataNet quality flag</att>\n"
            + "            <att name=\"sdn_conventions_urn\">SDN:L20::</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">80.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>TEMPPR01</sourceName>\n"
            + "        <destinationName>TEMPPR01</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"double\">-99999.0</att>\n"
            + "            <att name=\"ancillary_variables\">TEMPPR01_SEADATANET_QC</att>\n"
            + "            <att name=\"coordinates\">LONGITUDE LATITUDE TIME DEPTH</att>\n"
            + "            <att name=\"long_name\">Temperature</att>\n"
            + "            <att name=\"sdn_parameter_name\">Temperature of the water body</att>\n"
            + "            <att name=\"sdn_parameter_urn\">SDN:P01::TEMPPR01</att>\n"
            + "            <att name=\"sdn_uom_name\">Celsius degree</att>\n"
            + "            <att name=\"sdn_uom_urn\">SDN:P06::UPAA</att>\n"
            + "            <att name=\"standard_name\">sea_water_temperature</att>\n"
            + "            <att name=\"units\">celsius degree</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"coordinates\">null</att>\n"
            + "            <att name=\"ioos_category\">Temperature</att>\n"
            + "            <att name=\"sdn_P02_urn\">SDN:P02::TEMP</att>\n"
            + "            <att name=\"units\">degree_C</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>TEMPPR01_SEADATANET_QC</sourceName>\n"
            + "        <destinationName>TEMPPR01_SEADATANET_QC</destinationName>\n"
            + "        <dataType>byte</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"byte\">57</att>\n"
            + "            <att name=\"Conventions\">SeaDataNet measurand qualifier flags</att>\n"
            + "            <att name=\"flag_meanings\">no_quality_control good_value probably_good_value probably_bad_value bad_value changed_value value_below_detection value_in_excess interpolated_value missing_value value_phenomenon_uncertain</att>\n"
            + "            <att name=\"flag_values\" type=\"byteList\">48 49 50 51 52 53 54 55 56 57 65</att>\n"
            + "            <att name=\"long_name\">SeaDataNet quality flag</att>\n"
            + "            <att name=\"sdn_conventions_urn\">SDN:L20::</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">80.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>PRESPR01</sourceName>\n"
            + "        <destinationName>PRESPR01</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"double\">-99999.0</att>\n"
            + "            <att name=\"ancillary_variables\">PRESPR01_SEADATANET_QC</att>\n"
            + "            <att name=\"coordinates\">LONGITUDE LATITUDE TIME DEPTH</att>\n"
            + "            <att name=\"long_name\">Pressure</att>\n"
            + "            <att name=\"sdn_parameter_name\">Pressure (spatial co-ordinate) exerted by the water body by profiling pressure sensor and corrected to read zero at sea level</att>\n"
            + "            <att name=\"sdn_parameter_urn\">SDN:P01::PRESPR01</att>\n"
            + "            <att name=\"sdn_uom_name\">decibar=10000 pascals</att>\n"
            + "            <att name=\"sdn_uom_urn\">SDN:P06::UPDB</att>\n"
            + "            <att name=\"standard_name\">sea_water_pressure</att>\n"
            + "            <att name=\"units\">decibar=10000 pascals</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">5000.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"coordinates\">null</att>\n"
            + "            <att name=\"ioos_category\">Pressure</att>\n"
            + "            <att name=\"sdn_P02_urn\">SDN:P02::AHGT</att>\n"
            + "            <att name=\"units\">decibar</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>PRESPR01_SEADATANET_QC</sourceName>\n"
            + "        <destinationName>PRESPR01_SEADATANET_QC</destinationName>\n"
            + "        <dataType>byte</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"byte\">57</att>\n"
            + "            <att name=\"Conventions\">SeaDataNet measurand qualifier flags</att>\n"
            + "            <att name=\"flag_meanings\">no_quality_control good_value probably_good_value probably_bad_value bad_value changed_value value_below_detection value_in_excess interpolated_value missing_value value_phenomenon_uncertain</att>\n"
            + "            <att name=\"flag_values\" type=\"byteList\">48 49 50 51 52 53 54 55 56 57 65</att>\n"
            + "            <att name=\"long_name\">SeaDataNet quality flag</att>\n"
            + "            <att name=\"sdn_conventions_urn\">SDN:L20::</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">80.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    // Test.ensureEqual(results.substring(0, Math.min(results.length(),
    // expected.length())),
    // expected, "");

  }

  /**
   * testGenerateDatasetsXml with treatDimensionsAs and standadizeWhat. This doesn't test
   * suggestTestOutOfDate, except that for old data it doesn't suggest anything.
   */
  @org.junit.jupiter.api.Test
  void testGenerateDatasetsXmlDimensions() throws Throwable {
    // testVerboseOn();
    String dataDir =
        File2.addSlash(
            Path.of(EDDTableFromMultidimNcFilesTests.class.getResource("/data/nc/").toURI())
                .toString());
    String fileNameRegex = "GL_.*44761\\.nc";
    String useDimensionsCSV = "TIME, DEPTH";

    String results =
        EDDTableFromMultidimNcFiles.generateDatasetsXml(
                dataDir,
                fileNameRegex,
                "",
                useDimensionsCSV,
                1440,
                "",
                "",
                "",
                "", // just for test purposes
                false, // removeMVRows
                "TIME", // sort files by
                "",
                "",
                "",
                "",
                4355, // standardizeWhat 1+2(numericTime)+256(catch numeric mv)+4096(units)
                "LATITUDE, LONGITUDE, TIME", // treatDimensionsAs
                null, // cacheFromUrl
                null)
            + "\n";

    String tDatasetID =
        EDDTableFromMultidimNcFiles.suggestDatasetID(dataDir + fileNameRegex + useDimensionsCSV);
    String expected =
        "<dataset type=\"EDDTableFromMultidimNcFiles\" datasetID=\""
            + tDatasetID
            + "\" active=\"true\">\n"
            + "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n"
            + "    <updateEveryNMillis>10000</updateEveryNMillis>\n"
            + "    <fileDir>"
            + dataDir
            + "</fileDir>\n"
            + "    <fileNameRegex>"
            + fileNameRegex
            + "</fileNameRegex>\n"
            + "    <recursive>true</recursive>\n"
            + "    <pathRegex>.*</pathRegex>\n"
            + "    <metadataFrom>last</metadataFrom>\n"
            + "    <standardizeWhat>4355</standardizeWhat>\n"
            + "    <removeMVRows>false</removeMVRows>\n"
            + "    <sortFilesBySourceNames>TIME</sortFilesBySourceNames>\n"
            + "    <fileTableInMemory>false</fileTableInMemory>\n"
            + "    <!-- sourceAttributes>\n"
            + "        <att name=\"area\">Global Ocean</att>\n"
            + "        <att name=\"author\">Coriolis and MyOcean data provider</att>\n"
            + "        <att name=\"cdm_data_type\">Time-series</att>\n"
            + "        <att name=\"citation\">These data were collected and made freely available by the MyOcean project and the programs that contribute to it</att>\n"
            + "        <att name=\"contact\">codac@ifremer.fr</att>\n"
            + "        <att name=\"conventions\">OceanSITES Manual 1.1, CF-1.1</att>\n"
            + "        <att name=\"data_assembly_center\">Coriolis</att>\n"
            + "        <att name=\"data_mode\">R</att>\n"
            + "        <att name=\"data_type\">OceanSITES time-series data</att>\n"
            + "        <att name=\"date_update\">2012-08-06T22:07:01Z</att>\n"
            + "        <att name=\"distribution_statement\">These data follow MyOcean standards; they are public and free of charge. User assumes all risk for use of data. User must display citation in any publication or product using data. User must contact PI prior to any commercial use of data. More on: http://www.myocean.eu/data_policy</att>\n"
            + "        <att name=\"format_version\">1.1</att>\n"
            + "        <att name=\"geospatial_lat_max\">51.078</att>\n"
            + "        <att name=\"geospatial_lat_min\">47.763</att>\n"
            + "        <att name=\"geospatial_lon_max\">-39.878</att>\n"
            + "        <att name=\"geospatial_lon_min\">-44.112</att>\n"
            + "        <att name=\"history\">2012-08-06T22:07:01Z : Creation</att>\n"
            + "        <att name=\"id\">GL_201207_TS_DB_44761</att>\n"
            + "        <att name=\"institution\">Unknown institution</att>\n"
            + "        <att name=\"institution_references\">http://www.coriolis.eu.org</att>\n"
            + "        <att name=\"naming_authority\">OceanSITES</att>\n"
            + "        <att name=\"netcdf_version\">3.5</att>\n"
            + "        <att name=\"platform_code\">44761</att>\n"
            + "        <att name=\"qc_manual\">OceanSITES User&#39;s Manual v1.1</att>\n"
            + "        <att name=\"quality_control_indicator\">6</att>\n"
            + "        <att name=\"quality_index\">A</att>\n"
            + "        <att name=\"references\">http://www.myocean.eu.org,http://www.coriolis.eu.org</att>\n"
            + "        <att name=\"source\">BUOY/MOORING: SURFACE, DRIFTING : observation</att>\n"
            + "        <att name=\"time_coverage_end\">2012-07-31T23:00:00Z</att>\n"
            + "        <att name=\"time_coverage_start\">2012-07-11T13:00:00Z</att>\n"
            + "        <att name=\"update_interval\">daily</att>\n"
            + "        <att name=\"wmo_platform_code\">44761</att>\n"
            + "    </sourceAttributes -->\n"
            + "    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n"
            + "        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n"
            + "        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n"
            + "    -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cdm_data_type\">TimeSeries</att>\n"
            + "        <att name=\"cdm_timeseries_variables\">station_id, latitude, longitude, ???</att>\n"
            + "        <att name=\"Conventions\">OceanSITES Manual 1.1, CF-1.10, COARDS, ACDD-1.3</att>\n"
            + "        <att name=\"conventions\">null</att>\n"
            + "        <att name=\"creator_email\">codac@ifremer.fr</att>\n"
            + "        <att name=\"creator_name\">CODAC</att>\n"
            + "        <att name=\"creator_type\">institution</att>\n"
            + "        <att name=\"creator_url\">https://wwz.ifremer.fr/</att>\n"
            + "        <att name=\"infoUrl\">http://www.myocean.eu</att>\n"
            + "        <att name=\"keywords\">air, air_pressure_at_sea_level, atmosphere, atmospheric, ATMS, ATMS_DM, ATMS_QC, ATPT, ATPT_DM, ATPT_QC, data, depth, DEPTH_QC, earth, Earth Science &gt; Atmosphere &gt; Atmospheric Pressure &gt; Atmospheric Pressure Measurements, Earth Science &gt; Atmosphere &gt; Atmospheric Pressure &gt; Pressure Tendency, Earth Science &gt; Atmosphere &gt; Atmospheric Pressure &gt; Sea Level Pressure, Earth Science &gt; Atmosphere &gt; Atmospheric Pressure &gt; Static Pressure, Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Water Temperature, flag, hour, hourly, institution, latitude, level, local, longitude, measurements, method, ocean, oceans, pressure, processing, quality, science, sea, sea_water_temperature, seawater, source, static, TEMP, TEMP_DM, TEMP_QC, temperature, tendency, tendency_of_air_pressure, time, TIME_QC, water</att>\n"
            + "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n"
            + "        <att name=\"license\">These data follow MyOcean standards; they are public and free of charge. User assumes all risk for use of data. User must display citation in any publication or product using data. User must contact PI prior to any commercial use of data. More on: http://www.myocean.eu/data_policy</att>\n"
            + "        <att name=\"references\">http://www.myocean.eu,http://www.coriolis.eu.org</att>\n"
            + "        <att name=\"sourceUrl\">(local files)</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"subsetVariables\">TIME_QC, depth, DEPTH_QC, TEMP_QC, TEMP_DM, ATPT_QC, ATPT_DM, ATMS_QC, ATMS_DM</att>\n"
            + "        <att name=\"summary\">Unknown institution data from a local source.</att>\n"
            + "        <att name=\"title\">Unknown institution data from a local source.</att>\n"
            + "        <att name=\"treatDimensionsAs\">LATITUDE, LONGITUDE, TIME</att>\n"
            + "    </addAttributes>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>TIME</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"double\">NaN</att>\n"
            + "            <att name=\"axis\">T</att>\n"
            + "            <att name=\"long_name\">time</att>\n"
            + "            <att name=\"QC_indicator\" type=\"int\">1</att>\n"
            + "            <att name=\"QC_procedure\" type=\"int\">1</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
            + "            <att name=\"valid_max\" type=\"double\">7.144848E9</att>\n"
            + "            <att name=\"valid_min\" type=\"double\">-6.31152E8</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">8.0E9</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-2.0E9</att>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>TIME_QC</sourceName>\n"
            + "        <destinationName>TIME_QC</destinationName>\n"
            + "        <dataType>byte</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"byte\">127</att>\n"
            + "            <att name=\"conventions\">OceanSites reference table 2</att>\n"
            + "            <att name=\"flag_meanings\">no_qc_performed good_data probably_good_data bad_data_that_are_potentially_correctable bad_data value_changed not_used nominal_value interpolated_value missing_value</att>\n"
            + "            <att name=\"flag_values\" type=\"byteList\">0 1 2 3 4 5 6 7 8 9</att>\n"
            + "            <att name=\"long_name\">quality flag</att>\n"
            + "            <att name=\"valid_max\" type=\"byte\">9</att>\n"
            + "            <att name=\"valid_min\" type=\"byte\">0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>DEPTH</sourceName>\n"
            + "        <destinationName>depth</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">NaN</att>\n"
            + "            <att name=\"axis\">Z</att>\n"
            + "            <att name=\"coordinate_reference_frame\">urn:ogc:crs:EPSG::5113</att>\n"
            + "            <att name=\"long_name\">Depth of each measurement</att>\n"
            + "            <att name=\"positive\">down</att>\n"
            + "            <att name=\"QC_indicator\" type=\"int\">1</att>\n"
            + "            <att name=\"QC_procedure\" type=\"int\">1</att>\n"
            + "            <att name=\"reference\">sea_level</att>\n"
            + "            <att name=\"standard_name\">depth</att>\n"
            + "            <att name=\"units\">m</att>\n"
            + "            <att name=\"valid_max\" type=\"double\">12000.0</att>\n"
            + "            <att name=\"valid_min\" type=\"double\">0.0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">8000.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-8000.0</att>\n"
            + "            <att name=\"colorBarPalette\">TopographyDepth</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">NaN</att>\n"
            + "            <att name=\"reference\">null</att>\n"
            + "            <att name=\"references\">sea_level</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>DEPTH_QC</sourceName>\n"
            + "        <destinationName>DEPTH_QC</destinationName>\n"
            + "        <dataType>byte</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"byte\">127</att>\n"
            + "            <att name=\"conventions\">OceanSites reference table 2</att>\n"
            + "            <att name=\"flag_meanings\">no_qc_performed good_data probably_good_data bad_data_that_are_potentially_correctable bad_data value_changed not_used nominal_value interpolated_value missing_value</att>\n"
            + "            <att name=\"flag_values\" type=\"byteList\">0 1 2 3 4 5 6 7 8 9</att>\n"
            + "            <att name=\"long_name\">quality flag</att>\n"
            + "            <att name=\"valid_max\" type=\"byte\">9</att>\n"
            + "            <att name=\"valid_min\" type=\"byte\">0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "            <att name=\"standard_name\">depth</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>LATITUDE</sourceName>\n"
            + "        <destinationName>latitude</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">NaN</att>\n"
            + "            <att name=\"axis\">Y</att>\n"
            + "            <att name=\"long_name\">Latitude of each location</att>\n"
            + "            <att name=\"QC_indicator\" type=\"int\">1</att>\n"
            + "            <att name=\"QC_procedure\" type=\"int\">1</att>\n"
            + "            <att name=\"standard_name\">latitude</att>\n"
            + "            <att name=\"units\">degrees_north</att>\n"
            + "            <att name=\"valid_max\" type=\"double\">90.0</att>\n"
            + "            <att name=\"valid_min\" type=\"double\">-90.0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>LONGITUDE</sourceName>\n"
            + "        <destinationName>longitude</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">NaN</att>\n"
            + "            <att name=\"axis\">X</att>\n"
            + "            <att name=\"long_name\">Longitude of each location</att>\n"
            + "            <att name=\"QC_indicator\" type=\"int\">1</att>\n"
            + "            <att name=\"QC_procedure\" type=\"int\">1</att>\n"
            + "            <att name=\"standard_name\">longitude</att>\n"
            + "            <att name=\"units\">degrees_east</att>\n"
            + "            <att name=\"valid_max\" type=\"double\">180.0</att>\n"
            + "            <att name=\"valid_min\" type=\"double\">-180.0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>TEMP</sourceName>\n"
            + "        <destinationName>TEMP</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">NaN</att>\n"
            + "            <att name=\"long_name\">Sea temperature</att>\n"
            + "            <att name=\"standard_name\">sea_water_temperature</att>\n"
            + "            <att name=\"units\">degree_C</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Temperature</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>TEMP_QC</sourceName>\n"
            + "        <destinationName>TEMP_QC</destinationName>\n"
            + "        <dataType>byte</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"byte\">127</att>\n"
            + "            <att name=\"conventions\">OceanSites reference table 2</att>\n"
            + "            <att name=\"flag_meanings\">no_qc_performed good_data probably_good_data bad_data_that_are_potentially_correctable bad_data value_changed not_used nominal_value interpolated_value missing_value</att>\n"
            + "            <att name=\"flag_values\" type=\"byteList\">0 1 2 3 4 5 6 7 8 9</att>\n"
            + "            <att name=\"long_name\">quality flag</att>\n"
            + "            <att name=\"valid_max\" type=\"byte\">9</att>\n"
            + "            <att name=\"valid_min\" type=\"byte\">0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>TEMP_DM</sourceName>\n"
            + "        <destinationName>TEMP_DM</destinationName>\n"
            + "        <dataType>char</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"conventions\">OceanSITES reference table 5</att>\n"
            + "            <att name=\"flag_meanings\">realtime post-recovery delayed-mode mixed</att>\n"
            + "            <att name=\"flag_values\">R, P, D, M</att>\n"
            + "            <att name=\"long_name\">method of data processing</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">20.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>ATPT</sourceName>\n"
            + "        <destinationName>ATPT</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">NaN</att>\n"
            + "            <att name=\"long_name\">Atmospheric pressure hourly tendency</att>\n"
            + "            <att name=\"standard_name\">tendency_of_air_pressure</att>\n"
            + "            <att name=\"units\">hPa hour-1</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">3.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-3.0</att>\n"
            + "            <att name=\"ioos_category\">Pressure</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>ATPT_QC</sourceName>\n"
            + "        <destinationName>ATPT_QC</destinationName>\n"
            + "        <dataType>byte</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"byte\">127</att>\n"
            + "            <att name=\"conventions\">OceanSites reference table 2</att>\n"
            + "            <att name=\"flag_meanings\">no_qc_performed good_data probably_good_data bad_data_that_are_potentially_correctable bad_data value_changed not_used nominal_value interpolated_value missing_value</att>\n"
            + "            <att name=\"flag_values\" type=\"byteList\">0 1 2 3 4 5 6 7 8 9</att>\n"
            + "            <att name=\"long_name\">quality flag</att>\n"
            + "            <att name=\"valid_max\" type=\"byte\">9</att>\n"
            + "            <att name=\"valid_min\" type=\"byte\">0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>ATPT_DM</sourceName>\n"
            + "        <destinationName>ATPT_DM</destinationName>\n"
            + "        <dataType>char</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"conventions\">OceanSITES reference table 5</att>\n"
            + "            <att name=\"flag_meanings\">realtime post-recovery delayed-mode mixed</att>\n"
            + "            <att name=\"flag_values\">R, P, D, M</att>\n"
            + "            <att name=\"long_name\">method of data processing</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">20.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>ATMS</sourceName>\n"
            + "        <destinationName>ATMS</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">NaN</att>\n"
            + "            <att name=\"long_name\">Atmospheric pressure at sea level</att>\n"
            + "            <att name=\"standard_name\">air_pressure_at_sea_level</att>\n"
            + "            <att name=\"units\">hPa</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">1050.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">950.0</att>\n"
            + "            <att name=\"ioos_category\">Pressure</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>ATMS_QC</sourceName>\n"
            + "        <destinationName>ATMS_QC</destinationName>\n"
            + "        <dataType>byte</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"byte\">127</att>\n"
            + "            <att name=\"conventions\">OceanSites reference table 2</att>\n"
            + "            <att name=\"flag_meanings\">no_qc_performed good_data probably_good_data bad_data_that_are_potentially_correctable bad_data value_changed not_used nominal_value interpolated_value missing_value</att>\n"
            + "            <att name=\"flag_values\" type=\"byteList\">0 1 2 3 4 5 6 7 8 9</att>\n"
            + "            <att name=\"long_name\">quality flag</att>\n"
            + "            <att name=\"valid_max\" type=\"byte\">9</att>\n"
            + "            <att name=\"valid_min\" type=\"byte\">0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>ATMS_DM</sourceName>\n"
            + "        <destinationName>ATMS_DM</destinationName>\n"
            + "        <dataType>char</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"conventions\">OceanSITES reference table 5</att>\n"
            + "            <att name=\"flag_meanings\">realtime post-recovery delayed-mode mixed</att>\n"
            + "            <att name=\"flag_values\">R, P, D, M</att>\n"
            + "            <att name=\"long_name\">method of data processing</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">20.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // GenerateDatasetsXml
    results =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {
                  "-verbose",
                  "EDDTableFromMultidimNcFiles",
                  dataDir,
                  "GL_.*44761\\.nc",
                  "",
                  "TIME, DEPTH",
                  "1440",
                  "",
                  "",
                  "",
                  "", // just for test purposes
                  "false", // removeMVRows
                  "TIME", // sort files by
                  "",
                  "",
                  "",
                  "",
                  "4355", // standardizeWhat 1+2(numericTime)+256(catch numeric mv)+4096(units)
                  "LATITUDE, LONGITUDE, TIME", // treatDimensionsAs
                  ""
                }, // cacheFromUrl
                false); // doIt loop?
    Test.ensureEqual(results, expected, "Unexpected results from GenerateDatasetsXml.doIt.");

    // ensure it is ready-to-use by making a dataset from it
    // with one small change to addAttributes:
    results =
        String2.replaceAll(
            results,
            "        <att name=\"cdm_data_type\">TimeSeries</att>\n",
            "        <att name=\"cdm_data_type\">Point</att>\n");
    results =
        String2.replaceAll(
            results,
            "        <att name=\"cdm_timeseries_variables\">station_id, latitude, longitude, ???</att>\n",
            "");
    // it could be made into valid TimeSeries by adding a few more atts
    String2.log(results);

    // String tDatasetID = "GL___44761_0171_3b6f_1e9c";
    EDD.deleteCachedDatasetInfo(tDatasetID);
    EDDTableFromMultidimNcFiles edd =
        (EDDTableFromMultidimNcFiles) EDDTableFromMultidimNcFiles.oneFromXmlFragment(null, results);
    Test.ensureEqual(edd.datasetID(), tDatasetID, "");
    Test.ensureEqual(edd.title(), "Unknown institution data from a local source.", "");
    Test.ensureEqual(
        String2.toCSSVString(edd.dataVariableDestinationNames()),
        "time, TIME_QC, depth, DEPTH_QC, latitude, longitude, TEMP, TEMP_QC, TEMP_DM, ATPT, ATPT_QC, ATPT_DM, ATMS, ATMS_QC, ATMS_DM",
        "");
    Test.ensureEqual(
        edd.treatDimensionsAs.length, 1, EDDTableFromMultidimNcFiles.TREAT_DIMENSIONS_AS);
    Test.ensureEqual(
        String2.toCSSVString(edd.treatDimensionsAs[0]),
        "LATITUDE, LONGITUDE, TIME",
        EDDTableFromMultidimNcFiles.TREAT_DIMENSIONS_AS);
  }

  /**
   * This tests treatDimensionsAs and standardizeWhat.
   *
   * @throws Throwable if trouble
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testTreatDimensionsAs(boolean deleteCachedInfo) throws Throwable {
    // String2.log("\n******************
    // EDDTableFromMultidimNcFiles.testTreatDimensionsAs() *****************\n");
    // testVerboseOn();
    int language = 0;
    String name, tName, results, tResults, expected, userDapQuery, tQuery;
    String error = "";
    EDV edv;
    String dir = EDStatic.fullTestCacheDirectory;
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard
    // to check min:sec.

    String id = "testTreatDimensionsAs";
    if (deleteCachedInfo) EDD.deleteCachedDatasetInfo(id);
    EDDTable eddTable = (EDDTable) EDDTestDataset.gettestTreatDimensionsAs();

    // .das
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_treatDimensionsAs", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range 1.3201056e+9, 1.3437756e+9;\n"
            + "    String axis \"T\";\n"
            + "    Float64 colorBarMaximum 8.0e+9;\n"
            + "    Float64 colorBarMinimum -2.0e+9;\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    Int32 QC_indicator 1;\n"
            + "    Int32 QC_procedure 1;\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "    Float64 valid_max 7.144848e+9;\n"
            + "    Float64 valid_min -6.31152e+8;\n"
            + "  }\n"
            + "  TIME_QC {\n"
            + "    Byte _FillValue 127;\n"
            + "    String _Unsigned \"false\";\n"
            + // ERDDAP adds
            "    Byte actual_range 1, 1;\n"
            + "    Float64 colorBarMaximum 10.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"OceanSites reference table 2\";\n"
            + "    String flag_meanings \"no_qc_performed good_data probably_good_data bad_data_that_are_potentially_correctable bad_data value_changed not_used nominal_value interpolated_value missing_value\";\n"
            + "    Byte flag_values 0, 1, 2, 3, 4, 5, 6, 7, 8, 9;\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"quality flag\";\n"
            + "    Byte valid_max 9;\n"
            + "    Byte valid_min 0;\n"
            + "  }\n"
            + "  depth {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    String _CoordinateZisPositive \"down\";\n"
            + "    Float32 _FillValue NaN;\n"
            + "    String axis \"Z\";\n"
            + "    Float64 colorBarMaximum 8000.0;\n"
            + "    Float64 colorBarMinimum -8000.0;\n"
            + "    String colorBarPalette \"TopographyDepth\";\n"
            + (results.indexOf("String coordinate_reference_frame \"urn:ogc:crs:EPSG::5113\"") > -1
                ? "    String coordinate_reference_frame \"urn:ogc:crs:EPSG::5113\";\n"
                : "")
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Depth of each measurement\";\n"
            + "    String positive \"down\";\n"
            + "    Int32 QC_indicator 1;\n"
            + "    Int32 QC_procedure 1;\n"
            + "    String references \"sea_level\";\n"
            + "    String standard_name \"depth\";\n"
            + "    String units \"m\";\n"
            + "    Float32 valid_max 12000.0;\n"
            + "    Float32 valid_min 0.0;\n"
            + "  }\n"
            + "  DEPTH_QC {\n"
            + "    Byte _FillValue 127;\n"
            + "    String _Unsigned \"false\";\n"
            + // ERDDAP adds
            (results.indexOf("Byte actual_range 127, 127") > -1
                ? "    Byte actual_range 127, 127;\n"
                : "")
            + "    Float64 colorBarMaximum 10.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"OceanSites reference table 2\";\n"
            + "    String flag_meanings \"no_qc_performed good_data probably_good_data bad_data_that_are_potentially_correctable bad_data value_changed not_used nominal_value interpolated_value missing_value\";\n"
            + "    Byte flag_values 0, 1, 2, 3, 4, 5, 6, 7, 8, 9;\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"quality flag\";\n"
            + "    String standard_name \"depth\";\n"
            + "    Byte valid_max 9;\n"
            + "    Byte valid_min 0;\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float32 _FillValue NaN;\n"
            + "    Float32 actual_range 47.763, 52.936;\n"
            + "    String axis \"Y\";\n"
            + "    Float64 colorBarMaximum 90.0;\n"
            + "    Float64 colorBarMinimum -90.0;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude of each location\";\n"
            + "    Int32 QC_indicator 1;\n"
            + "    Int32 QC_procedure 1;\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "    Float32 valid_max 90.0;\n"
            + "    Float32 valid_min -90.0;\n"
            + "  }\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float32 _FillValue NaN;\n"
            + "    Float32 actual_range -44.112, -30.196;\n"
            + "    String axis \"X\";\n"
            + "    Float64 colorBarMaximum 180.0;\n"
            + "    Float64 colorBarMinimum -180.0;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude of each location\";\n"
            + "    Int32 QC_indicator 1;\n"
            + "    Int32 QC_procedure 1;\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "    Float32 valid_max 180.0;\n"
            + "    Float32 valid_min -180.0;\n"
            + "  }\n"
            + "  TEMP {\n"
            + "    Float32 _FillValue NaN;\n"
            + (results.indexOf("Float32 accuracy 0.0") > -1 ? "    Float32 accuracy 0.0;\n" : "")
            + "    Float32 actual_range 6.7, 16.7;\n"
            + (results.indexOf("String ancillary_variables \"temp_qc\";") > -1
                ? "    String ancillary_variables \"temp_qc\";\n"
                : "")
            + "    Float64 colorBarMaximum 32.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + (results.indexOf("String DM_indicator \"R\"") > -1
                ? "    String DM_indicator \"R\";\n"
                : "")
            + "    String ioos_category \"Temperature\";\n"
            + "    String long_name \"Sea temperature\";\n"
            + "    String standard_name \"sea_water_temperature\";\n"
            + "    String units \"degree_C\";\n"
            + "  }\n"
            + "  TEMP_QC {\n"
            + "    Byte _FillValue 127;\n"
            + "    String _Unsigned \"false\";\n"
            + // ERDDAP adds
            "    Byte actual_range 1, 1;\n"
            + "    Float64 colorBarMaximum 10.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"OceanSites reference table 2\";\n"
            + "    String flag_meanings \"no_qc_performed good_data probably_good_data bad_data_that_are_potentially_correctable bad_data value_changed not_used nominal_value interpolated_value missing_value\";\n"
            + "    Byte flag_values 0, 1, 2, 3, 4, 5, 6, 7, 8, 9;\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"quality flag\";\n"
            + "    Byte valid_max 9;\n"
            + "    Byte valid_min 0;\n"
            + "  }\n"
            + "  TEMP_DM {\n"
            + "    String actual_range \"R\n"
            + "R\";\n"
            + "    Float64 colorBarMaximum 20.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"OceanSITES reference table 5\";\n"
            + "    String flag_meanings \"realtime post-recovery delayed-mode mixed\";\n"
            + "    String flag_values \"R, P, D, M\";\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"method of data processing\";\n"
            + "  }\n"
            + "  ATPT {\n"
            + "    Float32 _FillValue NaN;\n"
            + "    Float32 actual_range -8.5, 4.166667;\n"
            + "    Float64 colorBarMaximum 3.0;\n"
            + "    Float64 colorBarMinimum -3.0;\n"
            + (results.indexOf("String DM_indicator \"R\"") > -1
                ? "    String DM_indicator \"R\";\n"
                : "")
            + "    String ioos_category \"Pressure\";\n"
            + "    String long_name \"Atmospheric pressure hourly tendency\";\n"
            + "    String standard_name \"tendency_of_air_pressure\";\n"
            + "    String units \"hPa hour-1\";\n"
            + "  }\n"
            + "  ATPT_QC {\n"
            + "    Byte _FillValue 127;\n"
            + "    String _Unsigned \"false\";\n"
            + // ERDDAP adds
            "    Byte actual_range 0, 0;\n"
            + "    Float64 colorBarMaximum 10.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"OceanSites reference table 2\";\n"
            + "    String flag_meanings \"no_qc_performed good_data probably_good_data bad_data_that_are_potentially_correctable bad_data value_changed not_used nominal_value interpolated_value missing_value\";\n"
            + "    Byte flag_values 0, 1, 2, 3, 4, 5, 6, 7, 8, 9;\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"quality flag\";\n"
            + "    Byte valid_max 9;\n"
            + "    Byte valid_min 0;\n"
            + "  }\n"
            + "  ATPT_DM {\n"
            + "    String actual_range \"R\n"
            + "R\";\n"
            + "    Float64 colorBarMaximum 20.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"OceanSITES reference table 5\";\n"
            + "    String flag_meanings \"realtime post-recovery delayed-mode mixed\";\n"
            + "    String flag_values \"R, P, D, M\";\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"method of data processing\";\n"
            + "  }\n"
            + "  ATMS {\n"
            + "    Float32 _FillValue NaN;\n"
            + "    Float32 actual_range 974.7, 1026.6;\n"
            + "    Float64 colorBarMaximum 1050.0;\n"
            + "    Float64 colorBarMinimum 950.0;\n"
            + (results.indexOf("String DM_indicator \"R\"") > -1
                ? "    String DM_indicator \"R\";\n"
                : "")
            + "    String ioos_category \"Pressure\";\n"
            + "    String long_name \"Atmospheric pressure at sea level\";\n"
            + "    String standard_name \"air_pressure_at_sea_level\";\n"
            + "    String units \"hPa\";\n"
            + "  }\n"
            + "  ATMS_QC {\n"
            + "    Byte _FillValue 127;\n"
            + "    String _Unsigned \"false\";\n"
            + // ERDDAP adds
            "    Byte actual_range 0, 0;\n"
            + "    Float64 colorBarMaximum 10.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"OceanSites reference table 2\";\n"
            + "    String flag_meanings \"no_qc_performed good_data probably_good_data bad_data_that_are_potentially_correctable bad_data value_changed not_used nominal_value interpolated_value missing_value\";\n"
            + "    Byte flag_values 0, 1, 2, 3, 4, 5, 6, 7, 8, 9;\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"quality flag\";\n"
            + "    Byte valid_max 9;\n"
            + "    Byte valid_min 0;\n"
            + "  }\n"
            + "  ATMS_DM {\n"
            + "    String actual_range \"R\n"
            + "R\";\n"
            + "    Float64 colorBarMaximum 20.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"OceanSITES reference table 5\";\n"
            + "    String flag_meanings \"realtime post-recovery delayed-mode mixed\";\n"
            + "    String flag_values \"R, P, D, M\";\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"method of data processing\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String area \"Global Ocean\";\n"
            + "    String author \"Coriolis and MyOcean data provider\";\n"
            + "    String cdm_data_type \"Point\";\n"
            + "    String citation \"These data were collected and made freely available by the MyOcean project and the programs that contribute to it\";\n"
            + "    String contact \"codac@ifremer.fr\";\n"
            + "    String Conventions \"OceanSITES Manual 1.1, CF-1.6, COARDS, ACDD-1.3\";\n"
            + "    String creator_email \"codac@ifremer.fr\";\n"
            + "    String creator_name \"CODAC\";\n"
            + "    String creator_type \"institution\";\n"
            + "    String creator_url \"https://wwz.ifremer.fr/\";\n"
            + "    String data_assembly_center \"Coriolis\";\n"
            + "    String data_mode \"R\";\n"
            + "    String data_type \"OceanSITES time-series data\";\n"
            + "    String date_update \"2012-08-06T22:07:01Z\";\n"
            + "    String distribution_statement \"These data follow MyOcean standards; they are public and free of charge. User assumes all risk for use of data. User must display citation in any publication or product using data. User must contact PI prior to any commercial use of data. More on: http://www.myocean.eu/data_policy\";\n"
            + "    Float64 Easternmost_Easting -30.196;\n"
            + "    String featureType \"Point\";\n"
            + "    String format_version \"1.1\";\n"
            + "    Float64 geospatial_lat_max 52.936;\n"
            + "    Float64 geospatial_lat_min 47.763;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max -30.196;\n"
            + "    Float64 geospatial_lon_min -44.112;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    String geospatial_vertical_positive \"down\";\n"
            + "    String geospatial_vertical_units \"m\";\n"
            + "    String history \"2012-08-06T22:07:01Z : Creation\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // 2018-06-14T18:48:29Z (local files)
    // 2018-06-14T18:48:29Z
    // "http://localhost:8080/cwexperimental/tabledap/testTreatDimensionsAs.das";
    expected =
        "String id \"GL_201207_TS_DB_44761\";\n"
            + "    String infoUrl \"http://www.myocean.eu\";\n"
            + "    String institution \"Unknown institution\";\n"
            + "    String institution_references \"http://www.coriolis.eu.org\";\n"
            + "    String keywords \"air, air_pressure_at_sea_level, atmosphere, atmospheric, ATMS, ATMS_DM, ATMS_QC, ATPT, ATPT_DM, ATPT_QC, data, depth, DEPTH_QC, earth, Earth Science > Atmosphere > Atmospheric Pressure > Atmospheric Pressure Measurements, Earth Science > Atmosphere > Atmospheric Pressure > Pressure Tendency, Earth Science > Atmosphere > Atmospheric Pressure > Sea Level Pressure, Earth Science > Atmosphere > Atmospheric Pressure > Static Pressure, Earth Science > Oceans > Ocean Temperature > Water Temperature, flag, hour, hourly, institution, latitude, level, local, longitude, measurements, method, ocean, oceans, pressure, processing, quality, science, sea, sea_water_temperature, seawater, source, static, TEMP, TEMP_DM, TEMP_QC, temperature, tendency, tendency_of_air_pressure, time, TIME_QC, water\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String license \"These data follow MyOcean standards; they are public and free of charge. User assumes all risk for use of data. User must display citation in any publication or product using data. User must contact PI prior to any commercial use of data. More on: http://www.myocean.eu/data_policy\";\n"
            + "    String naming_authority \"OceanSITES\";\n"
            + "    String netcdf_version \"3.5\";\n"
            + "    Float64 Northernmost_Northing 52.936;\n"
            + "    String platform_code \"44761\";\n"
            + "    String qc_manual \"OceanSITES User's Manual v1.1\";\n"
            + "    String quality_control_indicator \"6\";\n"
            + "    String quality_index \"A\";\n"
            + "    String references \"http://www.myocean.eu,http://www.coriolis.eu.org\";\n"
            + "    String source \"BUOY/MOORING: SURFACE, DRIFTING : observation\";\n"
            + "    String sourceUrl \"(local files)\";\n"
            + "    Float64 Southernmost_Northing 47.763;\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String subsetVariables \"TIME_QC, depth, DEPTH_QC, TEMP_QC, TEMP_DM, ATPT_QC, ATPT_DM, ATMS_QC, ATMS_DM\";\n"
            + "    String summary \"Unknown institution data from a local source.\";\n"
            + "    String time_coverage_end \"2012-07-31T23:00:00Z\";\n"
            + "    String time_coverage_start \"2011-11-01T00:00:00Z\";\n"
            + "    String title \"The Title for testTreatDimensionsAs\";\n"
            + "    String update_interval \"daily\";\n"
            + "    Float64 Westernmost_Easting -44.112;\n"
            + "    String wmo_platform_code \"44761\";\n"
            + "  }\n"
            + "}\n";
    int po = results.indexOf(expected.substring(0, 40));
    Test.ensureEqual(results.substring(po), expected, "results=\n" + results);

    // .dds
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_treatDimensionsAs", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    Float64 time;\n"
            + "    Byte TIME_QC;\n"
            + "    Float32 depth;\n"
            + "    Byte DEPTH_QC;\n"
            + "    Float32 latitude;\n"
            + "    Float32 longitude;\n"
            + "    Float32 TEMP;\n"
            + "    Byte TEMP_QC;\n"
            + "    String TEMP_DM;\n"
            + "    Float32 ATPT;\n"
            + "    Byte ATPT_QC;\n"
            + "    String ATPT_DM;\n"
            + "    Float32 ATMS;\n"
            + "    Byte ATMS_QC;\n"
            + "    String ATMS_DM;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // .csv
    // " Float64 actual_range 1.3201056e+9, 1.3437756e+9;\n" +
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&time=1.3201056e9",
            dir,
            eddTable.className() + "_treatDimensionsAs1",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "time,TIME_QC,depth,DEPTH_QC,latitude,longitude,TEMP,TEMP_QC,TEMP_DM,ATPT,ATPT_QC,ATPT_DM,ATMS,ATMS_QC,ATMS_DM\n"
            + "UTC,,m,,degrees_north,degrees_east,degree_C,,,hPa hour-1,,,hPa,,\n"
            + "2011-11-01T00:00:00Z,1,NaN,NaN,52.33,-35.219,9.2,1,R,-1.6,0,R,985.6,0,R\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&time=1.3437756e9",
            dir,
            eddTable.className() + "_treatDimensionsAs2",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "time,TIME_QC,depth,DEPTH_QC,latitude,longitude,TEMP,TEMP_QC,TEMP_DM,ATPT,ATPT_QC,ATPT_DM,ATMS,ATMS_QC,ATMS_DM\n"
            + "UTC,,m,,degrees_north,degrees_east,degree_C,,,hPa hour-1,,,hPa,,\n"
            + "2012-07-31T23:00:00Z,1,NaN,NaN,50.969,-40.416,16.5,1,R,0.13333334,0,R,1022.0,0,R\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /**
   * This tests treatDimensionsAs and standardizeWhat.
   *
   * @throws Throwable if trouble
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testTreatDimensionsAs2(boolean deleteCachedInfo) throws Throwable {
    // String2.log("\n******************
    // EDDTableFromMultidimNcFiles.testTreatDimensionsAs2() *****************\n");
    // testVerboseOn();
    int language = 0;
    String name, tName, results, tResults, expected, userDapQuery, tQuery;
    String error = "";
    EDV edv;
    String dir = EDStatic.fullTestCacheDirectory;
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard
    // to check min:sec.

    String id = "testTreatDimensionsAs2";
    if (deleteCachedInfo) EDD.deleteCachedDatasetInfo(id);
    EDDTable eddTable = (EDDTable) EDDTestDataset.gettestTreatDimensionsAs2();

    // .das
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_treatDimensionsAs2", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected = // long flag masks appear a float64
        "Attributes {\n"
            + " s {\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range 1.506816e+9, 1.5119982e+9;\n"
            + // before Calendar2 rounded to nearest
            // second for "days since ", this was
            // 1.5119981999999967e+9;\n" +
            "    String axis \"T\";\n"
            + "    Float64 colorBarMaximum 8.0e+9;\n"
            + "    Float64 colorBarMinimum -2.0e+9;\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    Int32 QC_indicator 1;\n"
            + "    Int32 QC_procedure 1;\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "    Float64 valid_max 7.144848e+9;\n"
            + "    Float64 valid_min -6.31152e+8;\n"
            + "  }\n"
            + "  TIME_QC {\n"
            + "    Byte _FillValue 127;\n"
            + "    String _Unsigned \"false\";\n"
            + // ERDDAP adds
            "    Byte actual_range 1, 1;\n"
            + "    Float64 colorBarMaximum 10.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"OceanSITES reference table 2\";\n"
            + "    String flag_meanings \"no_qc_performed good_data probably_good_data bad_data_that_are_potentially_correctable bad_data value_changed not_used nominal_value interpolated_value missing_value\";\n"
            + "    Byte flag_values 0, 1, 2, 3, 4, 5, 6, 7, 8, 9;\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"quality flag\";\n"
            + "    Byte valid_max 9;\n"
            + "    Byte valid_min 0;\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float32 _FillValue NaN;\n"
            + "    Float32 actual_range 42.5, 42.5;\n"
            + "    String axis \"Y\";\n"
            + "    Float64 colorBarMaximum 90.0;\n"
            + "    Float64 colorBarMinimum -90.0;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude of each location\";\n"
            + "    Int32 QC_indicator 1;\n"
            + "    Int32 QC_procedure 1;\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "    Float32 valid_max 90.0;\n"
            + "    Float32 valid_min MIN;\n"
            + "  }\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float32 _FillValue NaN;\n"
            + "    Float32 actual_range 27.4833, 27.4833;\n"
            + "    String axis \"X\";\n"
            + "    Float64 colorBarMaximum 180.0;\n"
            + "    Float64 colorBarMinimum -180.0;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude of each location\";\n"
            + "    Int32 QC_indicator 1;\n"
            + "    Int32 QC_procedure 1;\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "    Float32 valid_max 180.0;\n"
            + "    Float32 valid_min MIN;\n"
            + "  }\n"
            + "  depth {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    String _CoordinateZisPositive \"down\";\n"
            + "    Float32 _FillValue NaN;\n"
            + "    Float32 actual_range -2.0, 0.0;\n"
            + "    String axis \"Z\";\n"
            + "    Float64 colorBarMaximum 8000.0;\n"
            + "    Float64 colorBarMinimum -8000.0;\n"
            + "    String colorBarPalette \"TopographyDepth\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Depth\";\n"
            + "    String positive \"down\";\n"
            + "    String source_name \"DEPH\";\n"
            + "    String standard_name \"depth\";\n"
            + "    String units \"m\";\n"
            + "    Float32 valid_max 12000.0;\n"
            + "    Float32 valid_min MIN;\n"
            + "  }\n"
            + "  DEPH_QC {\n"
            + "    Byte _FillValue 127;\n"
            + "    String _Unsigned \"false\";\n"
            + // ERDDAP adds
            "    Byte actual_range 7, 7;\n"
            + "    Float64 colorBarMaximum 10.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"OceanSITES reference table 2\";\n"
            + "    String flag_meanings \"no_qc_performed good_data probably_good_data bad_data_that_are_potentially_correctable bad_data value_changed not_used nominal_value interpolated_value missing_value\";\n"
            + "    Byte flag_values 0, 1, 2, 3, 4, 5, 6, 7, 8, 9;\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"quality flag\";\n"
            + "    Byte valid_max 9;\n"
            + "    Byte valid_min 0;\n"
            + "  }\n"
            + "  DEPH_DM {\n"
            + "    String actual_range \"R\n"
            + "R\";\n"
            + "    Float64 colorBarMaximum 20.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"OceanSITES reference table 5\";\n"
            + "    String flag_meanings \"real-time provisional delayed-mode mixed\";\n"
            + "    String flag_values \"R, P, D, M\";\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"method of data processing\";\n"
            + "  }\n"
            + "  RELH {\n"
            + "    Float32 _FillValue NaN;\n"
            + "    Float32 actual_range 27.91, 100.0;\n"
            + "    Float64 colorBarMaximum 100.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Meteorology\";\n"
            + "    String long_name \"Relative humidity\";\n"
            + "    String standard_name \"relative_humidity\";\n"
            + "    String units \"percent\";\n"
            + "  }\n"
            + "  RELH_QC {\n"
            + "    Byte _FillValue 127;\n"
            + "    String _Unsigned \"false\";\n"
            + // ERDDAP adds
            "    Byte actual_range 0, 0;\n"
            + "    Float64 colorBarMaximum 10.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"OceanSITES reference table 2\";\n"
            + "    String flag_meanings \"no_qc_performed good_data probably_good_data bad_data_that_are_potentially_correctable bad_data value_changed not_used nominal_value interpolated_value missing_value\";\n"
            + "    Byte flag_values 0, 1, 2, 3, 4, 5, 6, 7, 8, 9;\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"quality flag\";\n"
            + "    Byte valid_max 9;\n"
            + "    Byte valid_min 0;\n"
            + "  }\n"
            + "  RELH_DM {\n"
            + "    String actual_range \"R\n"
            + "R\";\n"
            + "    Float64 colorBarMaximum 20.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"OceanSITES reference table 5\";\n"
            + "    String flag_meanings \"real-time provisional delayed-mode mixed\";\n"
            + "    String flag_values \"R, P, D, M\";\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"method of data processing\";\n"
            + "  }\n"
            + "  ATMS {\n"
            + "    Float32 _FillValue NaN;\n"
            + "    Float64 colorBarMaximum 1050.0;\n"
            + "    Float64 colorBarMinimum 950.0;\n"
            + "    String ioos_category \"Pressure\";\n"
            + "    String long_name \"Atmospheric pressure at sea level\";\n"
            + "    String standard_name \"air_pressure_at_sea_level\";\n"
            + "    String units \"hPa\";\n"
            + "  }\n"
            + "  ATMS_QC {\n"
            + "    Byte _FillValue 127;\n"
            + "    String _Unsigned \"false\";\n"
            + // ERDDAP adds
            "    Byte actual_range 9, 9;\n"
            + "    Float64 colorBarMaximum 10.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"OceanSITES reference table 2\";\n"
            + "    String flag_meanings \"no_qc_performed good_data probably_good_data bad_data_that_are_potentially_correctable bad_data value_changed not_used nominal_value interpolated_value missing_value\";\n"
            + "    Byte flag_values 0, 1, 2, 3, 4, 5, 6, 7, 8, 9;\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"quality flag\";\n"
            + "    Byte valid_max 9;\n"
            + "    Byte valid_min 0;\n"
            + "  }\n"
            + "  ATMS_DM {\n"
            + "    String actual_range \" \n"
            + "R\";\n"
            + "    Float64 colorBarMaximum 20.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"OceanSITES reference table 5\";\n"
            + "    String flag_meanings \"real-time provisional delayed-mode mixed\";\n"
            + "    String flag_values \"R, P, D, M\";\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"method of data processing\";\n"
            + "  }\n"
            + "  DRYT {\n"
            + "    Float32 _FillValue NaN;\n"
            + "    Float32 actual_range 0.0, 27.0;\n"
            + "    Float64 colorBarMaximum 40.0;\n"
            + "    Float64 colorBarMinimum -10.0;\n"
            + "    String ioos_category \"Temperature\";\n"
            + "    String long_name \"Air temperature in dry bulb\";\n"
            + "    String standard_name \"air_temperature\";\n"
            + "    String units \"degree_C\";\n"
            + "  }\n"
            + "  DRYT_QC {\n"
            + "    Byte _FillValue 127;\n"
            + "    String _Unsigned \"false\";\n"
            + // ERDDAP adds
            "    Byte actual_range 0, 0;\n"
            + "    Float64 colorBarMaximum 10.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"OceanSITES reference table 2\";\n"
            + "    String flag_meanings \"no_qc_performed good_data probably_good_data bad_data_that_are_potentially_correctable bad_data value_changed not_used nominal_value interpolated_value missing_value\";\n"
            + "    Byte flag_values 0, 1, 2, 3, 4, 5, 6, 7, 8, 9;\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"quality flag\";\n"
            + "    Byte valid_max 9;\n"
            + "    Byte valid_min 0;\n"
            + "  }\n"
            + "  DRYT_DM {\n"
            + "    String actual_range \"R\n"
            + "R\";\n"
            + "    Float64 colorBarMaximum 20.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"OceanSITES reference table 5\";\n"
            + "    String flag_meanings \"real-time provisional delayed-mode mixed\";\n"
            + "    String flag_values \"R, P, D, M\";\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"method of data processing\";\n"
            + "  }\n"
            + "  DEWT {\n"
            + "    Float32 _FillValue NaN;\n"
            + "    Float32 actual_range -6.0, 17.0;\n"
            + "    Float64 colorBarMaximum 40.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Temperature\";\n"
            + "    String long_name \"Dew point temperature\";\n"
            + "    String standard_name \"dew_point_temperature\";\n"
            + "    String units \"degree_C\";\n"
            + "  }\n"
            + "  DEWT_QC {\n"
            + "    Byte _FillValue 127;\n"
            + "    String _Unsigned \"false\";\n"
            + // ERDDAP adds
            "    Byte actual_range 0, 0;\n"
            + "    Float64 colorBarMaximum 10.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"OceanSITES reference table 2\";\n"
            + "    String flag_meanings \"no_qc_performed good_data probably_good_data bad_data_that_are_potentially_correctable bad_data value_changed not_used nominal_value interpolated_value missing_value\";\n"
            + "    Byte flag_values 0, 1, 2, 3, 4, 5, 6, 7, 8, 9;\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"quality flag\";\n"
            + "    Byte valid_max 9;\n"
            + "    Byte valid_min 0;\n"
            + "  }\n"
            + "  DEWT_DM {\n"
            + "    String actual_range \"R\n"
            + "R\";\n"
            + "    Float64 colorBarMaximum 20.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"OceanSITES reference table 5\";\n"
            + "    String flag_meanings \"real-time provisional delayed-mode mixed\";\n"
            + "    String flag_values \"R, P, D, M\";\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"method of data processing\";\n"
            + "  }\n"
            + "  WSPD {\n"
            + "    Float32 _FillValue NaN;\n"
            + "    Float32 actual_range 0.0, 14.91889;\n"
            + "    Float64 colorBarMaximum 15.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Wind\";\n"
            + "    String long_name \"Horizontal wind speed\";\n"
            + "    String standard_name \"wind_speed\";\n"
            + "    String units \"m s-1\";\n"
            + "  }\n"
            + "  WSPD_QC {\n"
            + "    Byte _FillValue 127;\n"
            + "    String _Unsigned \"false\";\n"
            + // ERDDAP adds
            "    Byte actual_range 0, 0;\n"
            + "    Float64 colorBarMaximum 10.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"OceanSITES reference table 2\";\n"
            + "    String flag_meanings \"no_qc_performed good_data probably_good_data bad_data_that_are_potentially_correctable bad_data value_changed not_used nominal_value interpolated_value missing_value\";\n"
            + "    Byte flag_values 0, 1, 2, 3, 4, 5, 6, 7, 8, 9;\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"quality flag\";\n"
            + "    Byte valid_max 9;\n"
            + "    Byte valid_min 0;\n"
            + "  }\n"
            + "  WSPD_DM {\n"
            + "    String actual_range \"R\n"
            + "R\";\n"
            + "    Float64 colorBarMaximum 20.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"OceanSITES reference table 5\";\n"
            + "    String flag_meanings \"real-time provisional delayed-mode mixed\";\n"
            + "    String flag_values \"R, P, D, M\";\n"
            + "    String ioos_category \"Wind\";\n"
            + "    String long_name \"method of data processing\";\n"
            + "    String standard_name \"wind_speed\";\n"
            + "  }\n"
            + "  WDIR {\n"
            + "    Float32 _FillValue NaN;\n"
            + "    Float32 actual_range 0.0, 360.0;\n"
            + "    Float64 colorBarMaximum 360.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Wind\";\n"
            + "    String long_name \"Wind from direction relative true north\";\n"
            + "    String standard_name \"wind_from_direction\";\n"
            + "    String units \"degree\";\n"
            + "  }\n"
            + "  WDIR_QC {\n"
            + "    Byte _FillValue 127;\n"
            + "    String _Unsigned \"false\";\n"
            + // ERDDAP adds
            "    Byte actual_range 0, 9;\n"
            + "    Float64 colorBarMaximum 10.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"OceanSITES reference table 2\";\n"
            + "    String flag_meanings \"no_qc_performed good_data probably_good_data bad_data_that_are_potentially_correctable bad_data value_changed not_used nominal_value interpolated_value missing_value\";\n"
            + "    Byte flag_values 0, 1, 2, 3, 4, 5, 6, 7, 8, 9;\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"quality flag\";\n"
            + "    Byte valid_max 9;\n"
            + "    Byte valid_min 0;\n"
            + "  }\n"
            + "  WDIR_DM {\n"
            + "    String actual_range \" \n"
            + "R\";\n"
            + "    Float64 colorBarMaximum 20.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"OceanSITES reference table 5\";\n"
            + "    String flag_meanings \"real-time provisional delayed-mode mixed\";\n"
            + "    String flag_values \"R, P, D, M\";\n"
            + "    String ioos_category \"Wind\";\n"
            + "    String long_name \"method of data processing\";\n"
            + "  }\n"
            + "  GSPD {\n"
            + "    Float32 _FillValue NaN;\n"
            + "    Float32 actual_range 6.173333, 20.06333;\n"
            + "    Float64 colorBarMaximum 30.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Wind\";\n"
            + "    String long_name \"Gust wind speed\";\n"
            + "    String standard_name \"wind_speed_of_gust\";\n"
            + "    String units \"m s-1\";\n"
            + "  }\n"
            + "  GSPD_QC {\n"
            + "    Byte _FillValue 127;\n"
            + "    String _Unsigned \"false\";\n"
            + // ERDDAP adds
            "    Byte actual_range 0, 9;\n"
            + "    Float64 colorBarMaximum 10.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"OceanSITES reference table 2\";\n"
            + "    String flag_meanings \"no_qc_performed good_data probably_good_data bad_data_that_are_potentially_correctable bad_data value_changed not_used nominal_value interpolated_value missing_value\";\n"
            + "    Byte flag_values 0, 1, 2, 3, 4, 5, 6, 7, 8, 9;\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"quality flag\";\n"
            + "    Byte valid_max 9;\n"
            + "    Byte valid_min 0;\n"
            + "  }\n"
            + "  GSPD_DM {\n"
            + "    String actual_range \" \n"
            + "R\";\n"
            + "    Float64 colorBarMaximum 20.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"OceanSITES reference table 5\";\n"
            + "    String flag_meanings \"real-time provisional delayed-mode mixed\";\n"
            + "    String flag_values \"R, P, D, M\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"method of data processing\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String area \"Black Sea\";\n"
            + "    String cdm_data_type \"Point\";\n"
            + "    String citation \"These data were collected and made freely available by the Copernicus project and the programs that contribute to it\";\n"
            + "    String contact \"cmems-service@io-bas.bg\";\n"
            + "    String Conventions \"CF-1.6 OceanSITES-Manual-1.2 Copernicus-InSituTAC-SRD-1.3 Copernicus-InSituTAC-ParametersList-3.0.0, COARDS, ACDD-1.3\";\n"
            + "    String creator_email \"cmems-service@io-bas.bg\";\n"
            + "    String creator_name \"Unknown institution\";\n"
            + "    String creator_url \"http://www.oceansites.org\";\n"
            + "    String data_assembly_center \"IOBAS\";\n"
            + "    String data_mode \"R\";\n"
            + "    String data_type \"OceanSITES time-series data\";\n"
            + "    String date_update \"yyyy-MM-ddThh:mm:ssZ\";\n"
            + "    String distribution_statement \"These data follow Copernicus standards; they are public and free of charge. User assumes all risk for use of data. User must display citation in any publication or product using data. User must contact PI prior to any commercial use of data.\";\n"
            + "    Float64 Easternmost_Easting 27.4833;\n"
            + "    String featureType \"Point\";\n"
            + "    String format_version \"1.2\";\n"
            + "    Float64 geospatial_lat_max 42.5;\n"
            + "    Float64 geospatial_lat_min 42.5;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max 27.4833;\n"
            + "    Float64 geospatial_lon_min 27.4833;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    Float64 geospatial_vertical_max 0.0;\n"
            + "    Float64 geospatial_vertical_min -2.0;\n"
            + "    String geospatial_vertical_positive \"down\";\n"
            + "    String geospatial_vertical_units \"m\";\n";
    // "    String history \"2018-01-11T02:59:08Z : Creation\n";
    results =
        results.replaceAll(
            "String date_update \\\"....-..-..T..:..:..Z",
            "String date_update \"yyyy-MM-ddThh:mm:ssZ");
    results = results.replaceAll("Float32 valid_min -?[0-9]+.[0-9]+;", "Float32 valid_min MIN;");
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // 2018-06-14T18:48:29Z (local files)
    // 2018-06-14T18:48:29Z
    // "http://localhost:8080/cwexperimental/tabledap/testTreatDimensionsAs.das";
    expected = // "String id \"BS_201711_TS_MO_LBBG\";\n" +
        "    String infoUrl \"http://www.oceansites.org\";\n"
            + "    String institution \"Unknown institution\";\n"
            + "    String institution_references \"http://www.io-bas.bg/\";\n"
            + "    String keywords \"air, air_pressure_at_sea_level, air_temperature, atmosphere, atmospheric, ATMS, ATMS_DM, ATMS_QC, bulb, currents, data, DEPH_DM, DEPH_QC, depth, dew, dew point, dew_point_temperature, DEWT, DEWT_DM, DEWT_QC, direction, dry, DRYT, DRYT_DM, DRYT_QC, earth, Earth Science > Atmosphere > Atmospheric Pressure > Atmospheric Pressure Measurements, Earth Science > Atmosphere > Atmospheric Pressure > Sea Level Pressure, Earth Science > Atmosphere > Atmospheric Pressure > Static Pressure, Earth Science > Atmosphere > Atmospheric Temperature > Air Temperature, Earth Science > Atmosphere > Atmospheric Temperature > Dew Point Temperature, Earth Science > Atmosphere > Atmospheric Temperature > Surface Air Temperature, Earth Science > Atmosphere > Atmospheric Water Vapor > Dew Point Temperature, Earth Science > Atmosphere > Atmospheric Water Vapor > Humidity, Earth Science > Atmosphere > Atmospheric Winds > Surface Winds, flag, GSPD, GSPD_DM, GSPD_QC, gust, horizontal, humidity, institution, latitude, level, local, longitude, measurements, meteorology, method, north, point, pressure, processing, quality, relative, relative_humidity, RELH, RELH_DM, RELH_QC, science, sea, seawater, source, speed, static, surface, temperature, time, TIME_QC, true, vapor, water, WDIR, WDIR_DM, WDIR_QC, wind, wind_from_direction, wind_speed, wind_speed_of_gust, winds, WSPD, WSPD_DM, WSPD_QC\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String last_date_observation \"YYYY-MM-DDThh:mm:ssZ\";\n"
            + "    String last_latitude_observation \"42.5\";\n"
            + "    String last_longitude_observation \"27.4833\";\n"
            + "    String license \"These data follow Copernicus standards; they are public and free of charge. User assumes all risk for use of data. User must display citation in any publication or product using data. User must contact PI prior to any commercial use of data.\";\n"
            + "    String naming_authority \"OceanSITES\";\n"
            + "    String netcdf_version \"3.5\";\n"
            + "    Float64 Northernmost_Northing 42.5;\n"
            + "    String platform_code \"LBBG\";\n"
            + "    String qc_manual \"OceanSITES User\\\\'s Manual v1.1\";\n"
            + // it is incorrect in source
            "    String quality_control_indicator \"6\";\n"
            + "    String quality_index \"A\";\n"
            + "    String references \"http://www.oceansites.org, http://marine.copernicus.eu\";\n"
            + "    String source \"land/onshore structure\";\n"
            + "    String sourceUrl \"(local files)\";\n"
            + "    Float64 Southernmost_Northing 42.5;\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String subsetVariables \"latitude, longitude, depth\";\n"
            + "    String summary \"Unknown institution data from a local source.\";\n"
            + "    String time_coverage_end \"YYYY-MM-DDThh:mm:ssZ\";\n"
            + "    String time_coverage_start \"YYYY-MM-DDThh:mm:ssZ\";\n"
            + "    String title \"Unknown institution data from a local source.\";\n"
            + "    String update_interval \"daily\";\n"
            + "    Float64 Westernmost_Easting 27.4833;\n"
            + "  }\n"
            + "}\n";
    results = results.replaceAll("....-..-..T..:..:..Z", "YYYY-MM-DDThh:mm:ssZ");
    int po = results.indexOf(expected.substring(0, 40));
    Test.ensureEqual(results.substring(Math.max(0, po)), expected, "results=\n" + results);

    // .dds
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_treatDimensionsAs2", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    Float64 time;\n"
            + "    Byte TIME_QC;\n"
            + "    Float32 latitude;\n"
            + "    Float32 longitude;\n"
            + "    Float32 depth;\n"
            + "    Byte DEPH_QC;\n"
            + "    String DEPH_DM;\n"
            + "    Float32 RELH;\n"
            + "    Byte RELH_QC;\n"
            + "    String RELH_DM;\n"
            + "    Float32 ATMS;\n"
            + "    Byte ATMS_QC;\n"
            + "    String ATMS_DM;\n"
            + "    Float32 DRYT;\n"
            + "    Byte DRYT_QC;\n"
            + "    String DRYT_DM;\n"
            + "    Float32 DEWT;\n"
            + "    Byte DEWT_QC;\n"
            + "    String DEWT_DM;\n"
            + "    Float32 WSPD;\n"
            + "    Byte WSPD_QC;\n"
            + "    String WSPD_DM;\n"
            + "    Float32 WDIR;\n"
            + "    Byte WDIR_QC;\n"
            + "    String WDIR_DM;\n"
            + "    Float32 GSPD;\n"
            + "    Byte GSPD_QC;\n"
            + "    String GSPD_DM;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // .csv
    // " Float64 actual_range 1.3201056e+9, 1.3437756e+9;\n" +
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&time=\"2017-10-01T00:00:00Z\"", // in quotes
            dir,
            eddTable.className() + "_treatDimensionsAs21",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "time,TIME_QC,latitude,longitude,depth,DEPH_QC,DEPH_DM,RELH,RELH_QC,RELH_DM,ATMS,ATMS_QC,ATMS_DM,DRYT,DRYT_QC,DRYT_DM,DEWT,DEWT_QC,DEWT_DM,WSPD,WSPD_QC,WSPD_DM,WDIR,WDIR_QC,WDIR_DM,GSPD,GSPD_QC,GSPD_DM\n"
            + "UTC,,degrees_north,degrees_east,m,,,percent,,,hPa,,,degree_C,,,degree_C,,,m s-1,,,degree,,,m s-1,,\n"
            + "2017-10-01T00:00:00Z,1,42.5,27.4833,-2.0,7,R,71.45,0,R,NaN,NaN,R,12.0,0,R,7.0,0,R,4.115556,0,R,10.0,0,R,NaN,9,R\n"
            + "2017-10-01T00:00:00Z,1,42.5,27.4833,0.0,7,R,NaN,NaN,R,NaN,9,R,NaN,NaN,R,NaN,NaN,R,NaN,NaN,R,NaN,NaN,R,NaN,NaN,R\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // all data
    // tName = eddTable.makeNewFileForDapQuery(language, null, null, "",
    // dir, eddTable.className() + "_treatDimensionsAs22", ".csv");
    // results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);

    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            // !!! INTERESTING TEST. Originally, specific max iso time fails
            // because it is to nearest second and actual max is off by tiny amount
            // I added a little fudge to the test to make it to the nearest second.
            // See EDDTable line 2302 and above
            "&time=2017-11-29T23:30:00Z",
            dir,
            eddTable.className() + "_treatDimensionsAs23",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "time,TIME_QC,latitude,longitude,depth,DEPH_QC,DEPH_DM,RELH,RELH_QC,RELH_DM,ATMS,ATMS_QC,ATMS_DM,DRYT,DRYT_QC,DRYT_DM,DEWT,DEWT_QC,DEWT_DM,WSPD,WSPD_QC,WSPD_DM,WDIR,WDIR_QC,WDIR_DM,GSPD,GSPD_QC,GSPD_DM\n"
            + "UTC,,degrees_north,degrees_east,m,,,percent,,,hPa,,,degree_C,,,degree_C,,,m s-1,,,degree,,,m s-1,,\n"
            + "2017-11-29T23:30:00Z,1,42.5,27.4833,0.0,7,R,93.14,0,R,NaN,9,\" \",3.0,0,R,2.0,0,R,0.0,0,R,0.0,0,R,NaN,9,\" \"\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /**
   * This tests long variables in this class.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest // nc4 support not complete
  void testLongAndNetcdf4() throws Throwable {
    // String2.log("\n******************
    // EDDTableFromMultidimNcFiles.testLongAndNetcdf4() *****************\n");
    // testVerboseOn();
    int language = 0;
    String name, tName, results, tResults, expected, userDapQuery, tQuery;
    String error = "";
    EDV edv;
    String dir = EDStatic.fullTestCacheDirectory;
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard
    // to check min:sec.

    String id = "testLong";

    EDDTable eddTable = (EDDTable) EDDTestDataset.gettestLong();

    /*
     *
     * //*** test getting das for entire dataset
     * String2.
     * log("\n*** EDDTableFromMultidimNcFiles test das and dds for entire dataset\n"
     * );
     * tName = eddTable.makeNewFileForDapQuery(language, null, null, "", dir,
     * eddTable.className() + "_LongEntire", ".das");
     * results = File2.directReadFrom88591File(dir + tName);
     * //String2.log(results);
     * expected = //long flag masks appear a float64
     * "Attributes {\n" +
     * " s {\n" +
     * "  feature_type_instance {\n" +
     * "    String cf_role \"timeseries_id\";\n" +
     * "    String ioos_category \"Identifier\";\n" +
     * "    String long_name \"Identifier for each feature type instance\";\n" +
     * "  }\n" +
     * "  latitude {\n" +
     * "    String _CoordinateAxisType \"Lat\";\n" +
     * "    Float64 actual_range 44.63893, 44.63893;\n" +
     * "    String axis \"Y\";\n" +
     * "    Float64 colorBarMaximum 90.0;\n" +
     * "    Float64 colorBarMinimum -90.0;\n" +
     * "    String ioos_category \"Location\";\n" +
     * "    String long_name \"sensor latitude\";\n" +
     * "    String standard_name \"latitude\";\n" +
     * "    String units \"degrees_north\";\n" +
     * "    Float64 valid_max 44.63893;\n" +
     * "    Float64 valid_min 44.63893;\n" +
     * "  }\n" +
     * "  longitude {\n" +
     * "    String _CoordinateAxisType \"Lon\";\n" +
     * "    Float64 actual_range -124.30379, -124.30379;\n" +
     * "    String axis \"X\";\n" +
     * "    Float64 colorBarMaximum 180.0;\n" +
     * "    Float64 colorBarMinimum -180.0;\n" +
     * "    String ioos_category \"Location\";\n" +
     * "    String long_name \"sensor longitude\";\n" +
     * "    String standard_name \"longitude\";\n" +
     * "    String units \"degrees_east\";\n" +
     * "    Float64 valid_max -124.30379;\n" +
     * "    Float64 valid_min -124.30379;\n" +
     * "  }\n" +
     * "  crs {\n" +
     * "    Int32 actual_range -2147483647, -2147483647;\n" +
     * "    String epsg_code \"EPSG:4326\";\n" +
     * "    String grid_mapping_name \"latitude_longitude\";\n" +
     * "    Float64 inverse_flattening 298.257223563;\n" +
     * "    String ioos_category \"Unknown\";\n" +
     * "    String long_name \"http://www.opengis.net/def/crs/EPSG/0/4326\";\n" +
     * "    Float64 semi_major_axis 6378137.0;\n" +
     * "  }\n" +
     * "  platform {\n" +
     * "    Int32 actual_range -2147483647, -2147483647;\n" +
     * "    String definition \"http://mmisw.org/ont/ioos/definition/stationID\";\n"
     * +
     * "    String ioos_category \"Wind\";\n" +
     * "    String ioos_code \"ce02shsm\";\n" +
     * "    String long_name \"Measures the status of the mooring power system controller, encompassing the batteries, recharging sources (wind and solar), and outputs.\";\n"
     * +
     * "    String short_name \"Mooring Power System Controller (PSC) Status Data\";\n"
     * +
     * "  }\n" +
     * "  time {\n" +
     * "    String _CoordinateAxisType \"Time\";\n" +
     * "    Float64 actual_range 1.475020819849e+9, 1.475107159296e+9;\n" +
     * "    String axis \"T\";\n" +
     * "    String calendar \"gregorian\";\n" +
     * "    String ioos_category \"Time\";\n" +
     * "    String long_name \"time of measurement\";\n" +
     * "    String standard_name \"time\";\n" +
     * "    String time_origin \"01-JAN-1970 00:00:00\";\n" +
     * "    String time_precision \"1970-01-01T00:00:00.000Z\";\n" +
     * "    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
     * "  }\n" +
     * "  depth {\n" +
     * "    String _CoordinateAxisType \"Height\";\n" +
     * "    String _CoordinateZisPositive \"down\";\n" +
     * "    Float64 _FillValue -9999.9;\n" +
     * "    Float64 actual_range 0.0, 0.0;\n" +
     * "    String axis \"Z\";\n" +
     * "    Float64 colorBarMaximum 8000.0;\n" +
     * "    Float64 colorBarMinimum -8000.0;\n" +
     * "    String colorBarPalette \"TopographyDepth\";\n" +
     * "    String grid_mapping \"crs\";\n" +
     * "    String ioos_category \"Location\";\n" +
     * "    String long_name \"z of the sensor relative to the water surface\";\n" +
     * "    String positive \"down\";\n" +
     * "    String standard_name \"depth\";\n" +
     * "    String units \"m\";\n" +
     * "    Float64 valid_max 0.0;\n" +
     * "    Float64 valid_min 0.0;\n" +
     * "  }\n" +
     * "  battery_bank1_current {\n" +
     * "    Float64 _FillValue -9.99999999e+8;\n" +
     * "    Float64 actual_range -2093.0, 996.0;\n" +
     * "    String ancillary_variables \"platform\";\n" +
     * "    String coverage_content_type \"physicalMeasurement\";\n" +
     * "    String grid_mapping \"crs\";\n" +
     * "    String ioos_category \"Unknown\";\n" +
     * "    String long_name \"Battery Bank 1 Current\";\n" +
     * "    Float64 missing_value -9.99999999e+8;\n" +
     * "    String platform \"platform\";\n" +
     * "    String standard_name \"battery_bank_1_current\";\n" +
     * "    String units \"mA\";\n" +
     * "  }\n" +
     * "  battery_bank1_temperature {\n" +
     * "    Float64 _FillValue -9.99999999e+8;\n" +
     * "    Float64 actual_range 13.47, 14.94;\n" +
     * "    String ancillary_variables \"platform\";\n" +
     * "    String coverage_content_type \"physicalMeasurement\";\n" +
     * "    String grid_mapping \"crs\";\n" +
     * "    String ioos_category \"Temperature\";\n" +
     * "    String long_name \"Battery Bank 1 Temperature\";\n" +
     * "    Float64 missing_value -9.99999999e+8;\n" +
     * "    String platform \"platform\";\n" +
     * "    String standard_name \"battery_bank_1_temperature\";\n" +
     * "    String units \"degree_Celsius\";\n" +
     * "  }\n" +
     * "  dcl_date_time_string {\n" +
     * "    String ancillary_variables \"platform\";\n" +
     * "    String coverage_content_type \"physicalMeasurement\";\n" +
     * "    String grid_mapping \"crs\";\n" +
     * "    String ioos_category \"Time\";\n" +
     * "    String long_name \"DCL Date and Time Stamp\";\n" +
     * "    String platform \"platform\";\n" +
     * "    String standard_name \"dcl_date_time_string\";\n" +
     * "    String time_origin \"01-JAN-1970 00:00:00\";\n" +
     * "    String time_precision \"1970-01-01T00:00:00.000Z\";\n" +
     * "    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
     * "  }\n" +
     * "  error_flag1 {\n" +
     * "    Float64 actual_range 0, 0;\n" +
     * "    Float64 colorBarMaximum 2.5e+9;\n" +
     * "    Float64 colorBarMinimum 0.0;\n" +
     * "    Float64 flag_masks 0, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216, 33554432, 67108864, 134217728, 268435456, 536870912, 1073741824, 2147483648;\n"
     * +
     * "    String flag_meanings \"no_error battery1_of_string1_overtemp battery2_of_string1_overtemp battery1_of_string2_overtemp battery2_of_string2_overtemp battery1_of_string3_overtemp battery2_of_string3_overtemp battery1_of_string4_overtemp battery2_of_string4_overtemp battery_string_1_fuse_blown battery_string_2_fuse_blown battery_string_3_fuse_blown battery_string_4_fuse_blown battery_string_1_charging_sensor_fault battery_string_1_discharging_sensor_fault battery_string_2_charging_sensor_fault battery_string_2_discharging_sensor_fault battery_string_3_charging_sensor_fault battery_string_3_discharging_sensor_fault battery_string_4_charging_sensor_fault battery_string_4_discharging_sensor_fault pv1_sensor_fault pv2_sensor_fault pv3_sensor_fault pv4_sensor_fault wt1_sensor_fault wt2_sensor_fault eeprom_access_fault rtclk_access_fault external_power_sensor_fault psc_hotel_power_sensor_fault psc_internal_overtemp_fault 24v_300v_dc_dc_converter_fuse_blown\";\n"
     * +
     * "    String ioos_category \"Statistics\";\n" +
     * "    String long_name \"Error Flag 1\";\n" +
     * "    String standard_name \"error_flag_1\";\n" +
     * "    String units \"1\";\n" +
     * "  }\n" +
     * "  error_flag2 {\n" +
     * "    Float64 actual_range 4202496, 12591104;\n" +
     * "    Float64 colorBarMaximum 5000000.0;\n" +
     * "    Float64 colorBarMinimum 0.0;\n" +
     * "    Float64 flag_masks 0, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288, 1048576, 2097152, 4194304;\n"
     * +
     * "    String flag_meanings \"no_error 24v_buoy_power_sensor_fault 24v_buoy_power_over_voltage_fault 24v_buoy_power_under_voltage_fault 5v_fuse_blown_non_critical wt1_control_relay_fault wt2_control_relay_fault pv1_control_relay_fault pv2_control_relay_fault pv3_control_relay_fault pv4_control_relay_fault fc1_control_relay_fault fc2_control_relay_fault cvt_swg_fault cvt_general_fault psc_hard_reset_flag psc_power_on_reset_flag wt1_fuse_blown wt2_fuse_blown pv1_fuse_blown pv2_fuse_blown pv3_fuse_blown pv4_fuse_blown cvt_shut_down_due_to_low_input_voltage\";\n"
     * +
     * "    String ioos_category \"Statistics\";\n" +
     * "    String long_name \"Error Flag 2\";\n" +
     * "    String standard_name \"error_flag_2\";\n" +
     * "    String units \"1\";\n" +
     * "  }\n" +
     * "  error_flag3 {\n" +
     * "    Float64 actual_range 253755392, 253755392;\n" +
     * "    Float64 colorBarMaximum 2.5e+9;\n" +
     * "    Float64 colorBarMinimum 0.0;\n" +
     * "    Float64 flag_masks 0, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216, 33554432, 67108864, 134217728, 268435456, 536870912, 1073741824, 2147483648;\n"
     * +
     * "    String flag_meanings \"no_error cvt_board_temp_over_100C interlock_output_supply_fuse_blown interlock_status_1_supply_fuse_blown interlock_status_2_supply_fuse_blown input_1_fuse_blown input_2_fuse_blown input_3_fuse_blown input_4_fuse_blown 5v_over_voltage 5v_under_voltage output_sensor_circuit_power_over_voltage output_sensor_circuit_power_under_voltage p_swgf_sensor_circuit_power_over_voltage p_swgf_sensor_circuit_power_under_voltage n_swgf_sensor_circuit_power_over_voltage n_swgf_sensor_circuit_power_under_voltage raw_24v_input_power_sensor_fault cvt_24v_hotel_power_sensor_fault interlock_supply_output_sensor_fault interlock_status_1_sensor_fault interlock_status_2_sensor_fault interlock_input_sensor_fault p_swgf_occured n_swgf_occured input_1_sensor_fault input_2_sensor_fault input_3_sensor_fault input_4_sensor_fault high_voltage_output_current_sensor_fault high_voltage_output_voltage_sensor_fault p_swgf_sensor_fault n_swgf_sensor_fault\";\n"
     * +
     * "    String ioos_category \"Statistics\";\n" +
     * "    String long_name \"Error Flag 3\";\n" +
     * "    String standard_name \"error_flag_3\";\n" +
     * "    String units \"1\";\n" +
     * "  }\n" +
     * "  deploy_id {\n" +
     * "    String ancillary_variables \"platform\";\n" +
     * "    String coverage_content_type \"physicalMeasurement\";\n" +
     * "    String grid_mapping \"crs\";\n" +
     * "    String ioos_category \"Identifier\";\n" +
     * "    String long_name \"Deployment ID\";\n" +
     * "    String platform \"platform\";\n" +
     * "    String standard_name \"deployment_id\";\n" +
     * "    String units \"1\";\n" +
     * "  }\n" +
     * " }\n" +
     * "  NC_GLOBAL {\n" +
     * "    String _NCProperties \"version=1|netcdflibversion=4.4.1.1|hdf5libversion=1.8.17\";\n"
     * +
     * "    String acknowledgement \"National Science Foundation\";\n" +
     * "    String cdm_data_type \"TimeSeries\";\n" +
     * "    String cdm_timeseries_variables \"feature_type_instance, latitude, longitude, crs, platform, depth, deploy_id\";\n"
     * +
     * "    String comment \"Mooring ID: CE02SHSM-00004\";\n" +
     * "    String Conventions \"CF-1.6,ACDD-1.3, COARDS\";\n" +
     * "    String creator_email \"cwingard@coas.oregonstate.edu\";\n" +
     * "    String creator_name \"Christopher Wingard\";\n" +
     * "    String creator_type \"person\";\n" +
     * "    String creator_url \"http://oceanobservatories.org\";\n" +
     * "    String date_created \"2017-03-08T19:21:00Z\";\n" +
     * "    String date_issued \"2017-03-08T19:21:00Z\";\n" +
     * "    String date_metadata_modified \"2017-03-08T19:21:00Z\";\n" +
     * "    String date_modified \"2017-03-08T19:21:00Z\";\n" +
     * "    Float64 Easternmost_Easting -124.30379;\n" +
     * "    String featureType \"TimeSeries\";\n" +
     * "    String geospatial_bounds \"POINT(-124.30379 44.63893)\";\n" +
     * "    String geospatial_bounds_crs \"4326\";\n" +
     * "    Float64 geospatial_lat_max 44.63893;\n" +
     * "    Float64 geospatial_lat_min 44.63893;\n" +
     * "    Float64 geospatial_lat_resolution 0;\n" +
     * "    String geospatial_lat_units \"degrees_north\";\n" +
     * "    Float64 geospatial_lon_max -124.30379;\n" +
     * "    Float64 geospatial_lon_min -124.30379;\n" +
     * "    Float64 geospatial_lon_resolution 0;\n" +
     * "    String geospatial_lon_units \"degrees_east\";\n" +
     * "    Float64 geospatial_vertical_max 0.0;\n" +
     * "    Float64 geospatial_vertical_min 0.0;\n" +
     * "    String geospatial_vertical_positive \"down\";\n" +
     * "    String geospatial_vertical_resolution \"0\";\n" +
     * "    String geospatial_vertical_units \"m\";\n" +
     * "    String history \"";
     * tResults = results.substring(0, Math.min(results.length(),
     * expected.length()));
     * Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
     *
     * expected=
     * "    String infoUrl \"http://oceanobservatories.org\";\n" +
     * "    String institution \"CGSN\";\n" +
     * "    String keywords \"bank, batteries, battery\";\n" +
     * "    String license \"The data may be used and redistributed for free but is not intended\n"
     * +
     * "for legal use, since it may contain inaccuracies. Neither the data\n" +
     * "Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
     * "of their employees or contractors, makes any warranty, express or\n" +
     * "implied, including warranties of merchantability and fitness for a\n" +
     * "particular purpose, or assumes any legal liability for the accuracy,\n" +
     * "completeness, or usefulness, of this information.\";\n" +
     * "    String ncei_template_version \"NCEI_NetCDF_TimeSeries_Orthogonal_Template_v2.0\";\n"
     * +
     * "    Float64 Northernmost_Northing 44.63893;\n" +
     * "    String project \"Ocean Observatories Initiative\";\n" +
     * "    String references \"http://oceanobservatories.org\";\n" +
     * "    String sourceUrl \"(local files)\";\n" +
     * "    Float64 Southernmost_Northing 44.63893;\n" +
     * "    String standard_name_vocabulary \"CF Standard Name Table v55\";\n" +
     * "    String subsetVariables \"feature_type_instance, latitude, longitude, crs, platform, depth, deploy_id\";\n"
     * +
     * "    String summary \"Measures the status of the mooring power system controller, encompassing the batteries, recharging sources (wind and solar), and outputs.\";\n"
     * +
     * "    String time_coverage_duration \"PT86339S\";\n" +
     * "    String time_coverage_end \"2016-09-28T23:59:19.296Z\";\n" +
     * "    String time_coverage_resolution \"PT60S\";\n" +
     * "    String time_coverage_start \"2016-09-28T00:00:19.849Z\";\n" +
     * "    String title \"Mooring Power System Controller (PSC) Status Data\";\n" +
     * "    Float64 Westernmost_Easting -124.30379;\n" +
     * "  }\n" +
     * "}\n";
     * int tPo = results.indexOf(expected.substring(0, 40));
     * Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
     * Test.ensureEqual(
     * results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
     * expected, "results=\n" + results);
     *
     * //*** test getting dds for entire dataset
     * tName = eddTable.makeNewFileForDapQuery(language, null, null, "", dir,
     * eddTable.className() + "_LongEntire", ".dds");
     * results = File2.directReadFrom88591File(dir + tName);
     * //String2.log(results);
     * expected =
     * "Dataset {\n" +
     * "  Sequence {\n" +
     * "    String feature_type_instance;\n" +
     * "    Float64 latitude;\n" +
     * "    Float64 longitude;\n" +
     * "    Int32 crs;\n" +
     * "    Int32 platform;\n" +
     * "    Float64 time;\n" +
     * "    Float64 depth;\n" +
     * "    Float64 battery_bank1_current;\n" +
     * "    Float64 battery_bank1_temperature;\n" +
     * "    Float64 dcl_date_time_string;\n" +
     * "    Float64 error_flag1;\n" + //long flags appear as float64
     * "    Float64 error_flag2;\n" +
     * "    Float64 error_flag3;\n" +
     * "    String deploy_id;\n" +
     * "  } s;\n" +
     * "} s;\n";
     * Test.ensureEqual(results, expected, "\nresults=\n" + results);
     *
     *
     * //*** test make data files
     *
     * //.csv
     * userDapQuery = "&time<=2016-09-28T00:03";
     * tName = eddTable.makeNewFileForDapQuery(language, null, null, userDapQuery,
     * dir,
     * eddTable.className() + "_Long1", ".csv");
     * results = File2.directReadFrom88591File(dir + tName);
     * //String2.log(results);
     * expected =
     * "feature_type_instance,latitude,longitude,crs,platform,time,depth,battery_bank1_current,battery_bank1_temperature,dcl_date_time_string,error_flag1,error_flag2,error_flag3,deploy_id\n"
     * +
     * ",degrees_north,degrees_east,,,UTC,m,mA,degree_Celsius,UTC,1,1,1,1\n" +
     * "ce02shsm,44.63893,-124.30379,-2147483647,-2147483647,2016-09-28T00:00:19.849Z,0.0,-1479.0,14.94,2016-09-28T00:00:19.849Z,0,4202496,253755392,D00004\n"
     * +
     * "ce02shsm,44.63893,-124.30379,-2147483647,-2147483647,2016-09-28T00:01:19.849Z,0.0,-1464.0,14.94,2016-09-28T00:01:19.849Z,0,4202496,253755392,D00004\n"
     * +
     * "ce02shsm,44.63893,-124.30379,-2147483647,-2147483647,2016-09-28T00:02:19.852Z,0.0,-1356.0,14.94,2016-09-28T00:02:19.852Z,0,4202496,253755392,D00004\n";
     * Test.ensureEqual(results, expected, "\nresults=\n" + results);
     *
     * //.csv constrain long
     * userDapQuery = "&error_flag2!=4202496";
     * tName = eddTable.makeNewFileForDapQuery(language, null, null, userDapQuery,
     * dir,
     * eddTable.className() + "_Long2", ".csv");
     * results = File2.directReadFrom88591File(dir + tName);
     * //String2.log(results);
     * expected =
     * "feature_type_instance,latitude,longitude,crs,platform,time,depth,battery_bank1_current,battery_bank1_temperature,dcl_date_time_string,error_flag1,error_flag2,error_flag3,deploy_id\n"
     * +
     * ",degrees_north,degrees_east,,,UTC,m,mA,degree_Celsius,UTC,1,1,1,1\n" +
     * "ce02shsm,44.63893,-124.30379,-2147483647,-2147483647,2016-09-28T00:20:19.819Z,0.0,450.0,14.94,2016-09-28T00:20:19.819Z,0,12591104,253755392,D00004\n"
     * +
     * "ce02shsm,44.63893,-124.30379,-2147483647,-2147483647,2016-09-28T00:21:19.825Z,0.0,519.0,14.94,2016-09-28T00:21:19.825Z,0,12591104,253755392,D00004\n";
     * Test.ensureEqual(results, expected, "\nresults=\n" + results);
     *
     *
     * //.csv for test requesting distinct
     * userDapQuery = "error_flag2&distinct()";
     * tName = eddTable.makeNewFileForDapQuery(language, null, null, userDapQuery,
     * dir,
     * eddTable.className() + "_Long1distinct", ".csv");
     * results = File2.directReadFrom88591File(dir + tName);
     * //String2.log(results);
     * expected =
     * "error_flag2\n" +
     * "1\n" +
     * "4202496\n" +
     * "12591104\n";
     * Test.ensureEqual(results, expected, "\nresults=\n" + results);
     *
     * //make nc3
     * userDapQuery =
     * "feature_type_instance,latitude,longitude,error_flag3&time<=2016-09-28T00:03";
     * tName = eddTable.makeNewFileForDapQuery(language, null, null, userDapQuery,
     * dir,
     * eddTable.className() + "_Longnc3", ".nc");
     * results = NcHelper.ncdump(dir + tName, true);
     * //String2.log(results);
     * expected =
     * "netcdf EDDTableFromMultidimNcFiles_Longnc3.nc {\n" +
     * "  dimensions:\n" +
     * "    row = 3;\n" +
     * "    feature_type_instance_strlen = 8;\n" +
     * "  variables:\n" +
     * "    char feature_type_instance(row=3, feature_type_instance_strlen=8);\n" +
     * "      :cf_role = \"timeseries_id\";\n" +
     * "      :ioos_category = \"Identifier\";\n" +
     * "      :long_name = \"Identifier for each feature type instance\";\n" +
     * "\n" +
     * "    double latitude(row=3);\n" +
     * "      :_CoordinateAxisType = \"Lat\";\n" +
     * "      :actual_range = 44.63893, 44.63893; // double\n" +
     * "      :axis = \"Y\";\n" +
     * "      :colorBarMaximum = 90.0; // double\n" +
     * "      :colorBarMinimum = -90.0; // double\n" +
     * "      :ioos_category = \"Location\";\n" +
     * "      :long_name = \"sensor latitude\";\n" +
     * "      :standard_name = \"latitude\";\n" +
     * "      :units = \"degrees_north\";\n" +
     * "      :valid_max = 44.63893; // double\n" +
     * "      :valid_min = 44.63893; // double\n" +
     * "\n" +
     * "    double longitude(row=3);\n" +
     * "      :_CoordinateAxisType = \"Lon\";\n" +
     * "      :actual_range = -124.30379, -124.30379; // double\n" +
     * "      :axis = \"X\";\n" +
     * "      :colorBarMaximum = 180.0; // double\n" +
     * "      :colorBarMinimum = -180.0; // double\n" +
     * "      :ioos_category = \"Location\";\n" +
     * "      :long_name = \"sensor longitude\";\n" +
     * "      :standard_name = \"longitude\";\n" +
     * "      :units = \"degrees_east\";\n" +
     * "      :valid_max = -124.30379; // double\n" +
     * "      :valid_min = -124.30379; // double\n" +
     * "\n" +
     * "    double error_flag3(row=3);\n" +
     * "      :actual_range = \"253755392253755392\";\n" +
     * "      :colorBarMaximum = 2.5E9; // double\n" +
     * "      :colorBarMinimum = 0.0; // double\n" +
     * "      :flag_masks = \"012481632641282565121024204840968192163843276865536131072262144524288104857620971524194304838860816777216335544326710886413421772826843545653687091210737418242147483648\";\n"
     * +
     * "      :flag_meanings = \"no_error cvt_board_temp_over_100C interlock_output_supply_fuse_blown interlock_status_1_supply_fuse_blown interlock_status_2_supply_fuse_blown input_1_fuse_blown input_2_fuse_blown input_3_fuse_blown input_4_fuse_blown 5v_over_voltage 5v_under_voltage output_sensor_circuit_power_over_voltage output_sensor_circuit_power_under_voltage p_swgf_sensor_circuit_power_over_voltage p_swgf_sensor_circuit_power_under_voltage n_swgf_sensor_circuit_power_over_voltage n_swgf_sensor_circuit_power_under_voltage raw_24v_input_power_sensor_fault cvt_24v_hotel_power_sensor_fault interlock_supply_output_sensor_fault interlock_status_1_sensor_fault interlock_status_2_sensor_fault interlock_input_sensor_fault p_swgf_occured n_swgf_occured input_1_sensor_fault input_2_sensor_fault input_3_sensor_fault input_4_sensor_fault high_voltage_output_current_sensor_fault high_voltage_output_voltage_sensor_fault p_swgf_sensor_fault n_swgf_sensor_fault\";\n"
     * +
     * "      :ioos_category = \"Statistics\";\n" +
     * "      :long_name = \"Error Flag 3\";\n" +
     * "      :standard_name = \"error_flag_3\";\n" +
     * "      :units = \"1\";\n" +
     * "\n" +
     * "  // global attributes:\n" +
     * "  :_NCProperties = \"version=1|netcdflibversion=4.4.1.1|hdf5libversion=1.8.17\";\n"
     * +
     * "  :acknowledgement = \"National Science Foundation\";\n" +
     * "  :cdm_data_type = \"TimeSeries\";\n" +
     * "  :cdm_timeseries_variables = \"feature_type_instance, latitude, longitude, crs, platform, depth, deploy_id\";\n"
     * +
     * "  :comment = \"Mooring ID: CE02SHSM-00004\";\n" +
     * "  :Conventions = \"CF-1.6,ACDD-1.3, COARDS\";\n" +
     * "  :creator_email = \"cwingard@coas.oregonstate.edu\";\n" +
     * "  :creator_name = \"Christopher Wingard\";\n" +
     * "  :creator_type = \"person\";\n" +
     * "  :creator_url = \"http://oceanobservatories.org\";\n" +
     * "  :date_created = \"2017-03-08T19:21:00Z\";\n" +
     * "  :date_issued = \"2017-03-08T19:21:00Z\";\n" +
     * "  :date_metadata_modified = \"2017-03-08T19:21:00Z\";\n" +
     * "  :date_modified = \"2017-03-08T19:21:00Z\";\n" +
     * "  :Easternmost_Easting = -124.30379; // double\n" +
     * "  :featureType = \"TimeSeries\";\n" +
     * "  :geospatial_bounds = \"POINT(-124.30379 44.63893)\";\n" +
     * "  :geospatial_bounds_crs = \"4326\";\n" +
     * "  :geospatial_lat_max = 44.63893; // double\n" +
     * "  :geospatial_lat_min = 44.63893; // double\n" +
     * "  :geospatial_lat_resolution = \"0\";\n" +
     * "  :geospatial_lat_units = \"degrees_north\";\n" +
     * "  :geospatial_lon_max = -124.30379; // double\n" +
     * "  :geospatial_lon_min = -124.30379; // double\n" +
     * "  :geospatial_lon_resolution = \"0\";\n" +
     * "  :geospatial_lon_units = \"degrees_east\";\n" +
     * "  :geospatial_vertical_positive = \"down\";\n" +
     * "  :geospatial_vertical_resolution = \"0\";\n" +
     * "  :geospatial_vertical_units = \"m\";\n" +
     * "  :history = \"2017-03-08T19:21:00Z - pyaxiom - File created using pyaxiom\n"
     * ;
     * Test.ensureEqual(results.substring(0, Math.min(results.length(),
     * expected.length())), expected, "\nresults=\n" + results);
     *
     * //"2017-03-28T22:21:20Z (local files)\n" +
     * //"2017-03-28T22:21:20Z http://localhost:8080/cwexperimental/tabledap/testLong.nc?feature_type_instance,latitude,longitude,error_flag3&time<=2016-09-28T00:03\";\n"
     * +
     * expected =
     * "  :id = \"EDDTableFromMultidimNcFiles_Longnc3\";\n" +
     * "  :infoUrl = \"http://oceanobservatories.org\";\n" +
     * "  :institution = \"CGSN\";\n" +
     * "  :keywords = \"bank, batteries, battery\";\n" +
     * "  :license = \"The data may be used and redistributed for free but is not intended\n"
     * +
     * "for legal use, since it may contain inaccuracies. Neither the data\n" +
     * "Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
     * "of their employees or contractors, makes any warranty, express or\n" +
     * "implied, including warranties of merchantability and fitness for a\n" +
     * "particular purpose, or assumes any legal liability for the accuracy,\n" +
     * "completeness, or usefulness, of this information.\";\n" +
     * "  :ncei_template_version = \"NCEI_NetCDF_TimeSeries_Orthogonal_Template_v2.0\";\n"
     * +
     * "  :Northernmost_Northing = 44.63893; // double\n" +
     * "  :project = \"Ocean Observatories Initiative\";\n" +
     * "  :references = \"http://oceanobservatories.org\";\n" +
     * "  :sourceUrl = \"(local files)\";\n" +
     * "  :Southernmost_Northing = 44.63893; // double\n" +
     * "  :standard_name_vocabulary = \"CF Standard Name Table v55\";\n" +
     * "  :subsetVariables = \"feature_type_instance, latitude, longitude, crs, platform, depth, deploy_id\";\n"
     * +
     * "  :summary = \"Measures the status of the mooring power system controller, encompassing the batteries, recharging sources (wind and solar), and outputs.\";\n"
     * +
     * "  :time_coverage_duration = \"PT86339S\";\n" +
     * "  :time_coverage_resolution = \"PT60S\";\n" +
     * "  :title = \"Mooring Power System Controller (PSC) Status Data\";\n" +
     * "  :Westernmost_Easting = -124.30379; // double\n" +
     * " data:\n" +
     * "feature_type_instance =\"ce02shsm\", \"ce02shsm\", \"ce02shsm\"\n" +
     * "latitude =\n" +
     * "  {44.63893, 44.63893, 44.63893}\n" +
     * "longitude =\n" +
     * "  {-124.30379, -124.30379, -124.30379}\n" +
     * "error_flag3 =\n" +
     * "  {2.53755392E8, 2.53755392E8, 2.53755392E8}\n" + //appears as double values
     * "}\n";
     * int po = results.indexOf(expected.substring(0, 30));
     * Test.ensureEqual(results.substring(po), expected, "\nresults=\n" + results);
     *
     */
    // make nc4
    Test.ensureTrue(
        String2.indexOf(EDDTableFromMultidimNcFiles.dataFileTypeNames, ".nc4") >= 0,
        "Enable .nc4?");

    userDapQuery = "feature_type_instance,latitude,longitude,error_flag3&time<=2016-09-28T00:03";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_Longnc4", ".nc4");
    results = NcHelper.ncdump(dir + tName, "");
    // String2.log(results);
    expected = "zztop\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
  }

  /**
   * This tests the methods in this class.
   *
   * @throws Throwable if trouble
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @TagMissingDataset // Errors loading dataset files, nothing valid?
  void testW1M3A(boolean deleteCachedInfo) throws Throwable {
    // String2.log("\n****************** EDDTableFromMultidimNcFiles.testW1M3A()
    // *****************\n");
    // testVerboseOn();
    int language = 0;
    String name, tName, results, tResults, expected, userDapQuery, tQuery;
    String error = "";
    EDV edv;
    int po, po2;
    String dir = EDStatic.fullTestCacheDirectory;
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard
    // to check min:sec.

    // test the floats work as expected
    float f = String2.parseFloat("-3.4E38");
    String2.log(
        ">> parse -3.4E38 => "
            + f
            + " isFinite="
            + Float.isFinite(f)
            + " equal5? "
            + Math2.almostEqual(5, -3.4e38, f));
    Test.ensureTrue(Float.isFinite(f), "");
    Test.ensureTrue(Math2.almostEqual(5, -3.4e38, f), "");

    // dump the temp attributes form all files file
    // float TEMP(TIME=620, DEPTH=7);
    // :long_name = "Sea temperature";
    // :standard_name = "sea_water_temperature";
    // :units = "degree_Celsius";
    // :_FillValue = 9.96921E36f; // float
    //
    // byte TEMP_QC(TIME=620, DEPTH=7);
    // for (int year = 2004; year <= 2016; year++) {
    // try {
    // String s = NcHelper.ncdump("/data/briand/W1M3A/OS_W1M3A_" + year + "_R.nc",
    // "-h");
    // if (year == 2004)
    // String2.log(s);

    // String2.log("" + year);
    // // po = s.indexOf(" variables:");
    // // String2.log(s.substring(0, po));

    // po = s.indexOf("float TEMP");
    // po2 = s.indexOf("byte TEMP_QC");
    // String2.log(s.substring(po, po2));

    // if (year == 2016)
    // String2.log(s);

    // } catch (Throwable t) {
    // }
    // }

    // make the dataset
    String id = "W1M3A";
    if (deleteCachedInfo) EDD.deleteCachedDatasetInfo(id);
    EDDTable eddTable = (EDDTable) EDDTestDataset.getW1M3A();

    // reported problem
    userDapQuery = "time,depth,TEMP&time>=2011-01-03T00&time<=2011-01-03T03";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_1profile", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "time,depth,TEMP\n"
            + "UTC,m,\n"
            + "2011-01-03T00:00:00Z,0.0,\n"
            + // was -3.4E38
            "2011-01-03T00:00:00Z,1.0,13.151\n"
            + "2011-01-03T00:00:00Z,6.0,13.168\n"
            + "2011-01-03T00:00:00Z,12.0,13.165\n"
            + "2011-01-03T00:00:00Z,20.0,13.166\n"
            + "2011-01-03T00:00:00Z,36.0,13.395\n"
            + "2011-01-03T03:00:00Z,0.0,\n"
            + // was -3.4E38
            "2011-01-03T03:00:00Z,1.0,13.194\n"
            + "2011-01-03T03:00:00Z,6.0,13.241\n"
            + "2011-01-03T03:00:00Z,12.0,13.186\n"
            + "2011-01-03T03:00:00Z,20.0,13.514\n"
            + "2011-01-03T03:00:00Z,36.0,13.927\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
  }

  /**
   * This tests treating char data as a String variable.
   *
   * @throws Throwable if trouble
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testCharAsString(boolean deleteCachedInfo) throws Throwable {
    // String2.log("\n******************
    // EDDTableFromMultidimNcFiles.testCharAsString() *****************\n");
    // testVerboseOn();
    int language = 0;
    String name, tName, results, tResults, expected, userDapQuery, tQuery;
    String error = "";
    EDV edv;
    String dir = EDStatic.fullTestCacheDirectory;
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard
    // to check min:sec.

    // print dumpString of one of the data files
    // String2.log(NcHelper.ncdump(EDStatic.unitTestDataDir +
    // "nccf/testCharAsString/7900364_prof.nc", "-h"));

    String id = "testCharAsString";
    if (deleteCachedInfo) EDD.deleteCachedDatasetInfo(id);
    EDDTable eddTable = (EDDTable) EDDTestDataset.gettestCharAsString();

    // *** test getting das for entire dataset
    String2.log("\n****************** test das and dds for entire dataset\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_Entire", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  fileNumber {\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"File Number\";\n"
            + "  }\n"
            + "  data_type {\n"
            + "    String conventions \"Argo reference table 1\";\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Data type\";\n"
            + "  }\n"
            + "  format_version {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"File format version\";\n"
            + "  }\n"
            + "  handbook_version {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Data handbook version\";\n"
            + "  }\n"
            + "  reference_date_time {\n"
            + "    Float64 actual_range -6.31152e+8, -6.31152e+8;\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Date of reference for Julian days\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  date_creation {\n"
            + "    Float64 actual_range 1.369414924e+9, 1.446162171e+9;\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Date of file creation\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  date_update {\n"
            + "    Float64 actual_range 1.499448499e+9, 1.542981342e+9;\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Date of update of this file\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  platform_number {\n"
            + "    String cf_role \"trajectory_id\";\n"
            + "    String conventions \"WMO float identifier : A9IIIII\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Float unique identifier\";\n"
            + "  }\n"
            + "  project_name {\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Name of the project\";\n"
            + "  }\n"
            + "  pi_name {\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Name of the principal investigator\";\n"
            + "  }\n"
            + "  cycle_number {\n"
            + "    Int32 _FillValue 99999;\n"
            + "    Int32 actual_range 1, 142;\n"
            + "    String cf_role \"profile_id\";\n"
            + "    Float64 colorBarMaximum 200.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"0...N, 0 : launch cycle (if exists), 1 : first complete cycle\";\n"
            + "    String ioos_category \"Statistics\";\n"
            + "    String long_name \"Float cycle number\";\n"
            + "  }\n"
            + "  direction {\n"
            + "    Float64 colorBarMaximum 360.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"A: ascending profiles, D: descending profiles\";\n"
            + "    String ioos_category \"Currents\";\n"
            + "    String long_name \"Direction of the station profiles\";\n"
            + "  }\n"
            + "  data_center {\n"
            + "    String conventions \"Argo reference table 4\";\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Data centre in charge of float data processing\";\n"
            + "  }\n"
            + "  dc_reference {\n"
            + "    String conventions \"Data centre convention\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Station unique identifier in data centre\";\n"
            + "  }\n"
            + "  data_state_indicator {\n"
            + "    String conventions \"Argo reference table 6\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Degree of processing the data have passed through\";\n"
            + "  }\n"
            + "  data_mode {\n"
            + "    String conventions \"R : real time; D : delayed mode; A : real time with adjustment\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Delayed mode or real time data\";\n"
            + "  }\n"
            + "  platform_type {\n"
            + "    String conventions \"Argo reference table 23\";\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Type of float\";\n"
            + "  }\n"
            + "  float_serial_no {\n"
            + "    Float64 colorBarMaximum 100.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Statistics\";\n"
            + "    String long_name \"Serial number of the float\";\n"
            + "  }\n"
            + "  firmware_version {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Instrument firmware version\";\n"
            + "  }\n"
            + "  wmo_inst_type {\n"
            + "    String conventions \"Argo reference table 8\";\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Coded instrument type\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range 1.356599997e+9, 1.4963616e+9;\n"
            + "    String axis \"T\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Julian day (UTC) of the station relative to REFERENCE_DATE_TIME\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  time_qc {\n"
            + "    Float64 colorBarMaximum 150.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"Argo reference table 2\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Quality on date and time\";\n"
            + "  }\n"
            + "  time_location {\n"
            + "    Float64 actual_range 1.356599997e+9, 1.496362576e+9;\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Julian day (UTC) of the location relative to REFERENCE_DATE_TIME\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float64 _FillValue 99999.0;\n"
            + "    Float64 actual_range -66.6667, 43.81645;\n"
            + "    String axis \"Y\";\n"
            + "    Float64 colorBarMaximum 90.0;\n"
            + "    Float64 colorBarMinimum -90.0;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude of the station, best estimate\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "    Float64 valid_max 90.0;\n"
            + "    Float64 valid_min -90.0;\n"
            + "  }\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 _FillValue 99999.0;\n"
            + "    Float64 actual_range -26.250239999999998, 36.42373;\n"
            + "    String axis \"X\";\n"
            + "    Float64 colorBarMaximum 180.0;\n"
            + "    Float64 colorBarMinimum -180.0;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude of the station, best estimate\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "    Float64 valid_max 180.0;\n"
            + "    Float64 valid_min -180.0;\n"
            + "  }\n"
            + "  position_qc {\n"
            + "    Float64 colorBarMaximum 150.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"Argo reference table 2\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Quality on position (latitude and longitude)\";\n"
            + "  }\n"
            + "  positioning_system {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Positioning system\";\n"
            + "  }\n"
            + "  profile_pres_qc {\n"
            + "    Float64 colorBarMaximum 150.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"Argo reference table 2a\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Global quality flag of PRES profile\";\n"
            + "  }\n"
            + "  profile_temp_qc {\n"
            + "    Float64 colorBarMaximum 150.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"Argo reference table 2a\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Global quality flag of TEMP profile\";\n"
            + "  }\n"
            + "  profile_psal_qc {\n"
            + "    Float64 colorBarMaximum 150.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"Argo reference table 2a\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Global quality flag of PSAL profile\";\n"
            + "  }\n"
            + "  vertical_sampling_scheme {\n"
            + "    String conventions \"Argo reference table 16\";\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Vertical sampling scheme\";\n"
            + "  }\n"
            + "  config_mission_number {\n"
            + "    Int32 _FillValue 99999;\n"
            + "    Int32 actual_range 1, 2;\n"
            + "    Float64 colorBarMaximum 100.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"1...N, 1 : first complete mission\";\n"
            + "    String ioos_category \"Statistics\";\n"
            + "    String long_name \"Unique number denoting the missions performed by the float\";\n"
            + "  }\n"
            + "  pres {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    Float32 _FillValue 99999.0;\n"
            + "    Float32 actual_range -0.2, 1999.9;\n"
            + "    String axis \"Z\";\n"
            + "    String C_format \"%7.1f\";\n"
            + "    Float64 colorBarMaximum 5000.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String FORTRAN_format \"F7.1\";\n"
            + "    String ioos_category \"Sea Level\";\n"
            + "    String long_name \"Sea water pressure, equals 0 at sea-level\";\n"
            + "    String standard_name \"sea_water_pressure\";\n"
            + "    String units \"decibar\";\n"
            + "    Float32 valid_max 12000.0;\n"
            + "    Float32 valid_min 0.0;\n"
            + "  }\n"
            + "  pres_qc {\n"
            + "    Float64 colorBarMaximum 150.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"Argo reference table 2\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"quality flag\";\n"
            + "  }\n"
            + "  pres_adjusted {\n"
            + "    Float32 _FillValue 99999.0;\n"
            + "    String axis \"Z\";\n"
            + "    String C_format \"%7.1f\";\n"
            + "    Float64 colorBarMaximum 5000.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String FORTRAN_format \"F7.1\";\n"
            + "    String ioos_category \"Sea Level\";\n"
            + "    String long_name \"Sea water pressure, equals 0 at sea-level\";\n"
            + "    String standard_name \"sea_water_pressure\";\n"
            + "    String units \"decibar\";\n"
            + "    Float32 valid_max 12000.0;\n"
            + "    Float32 valid_min 0.0;\n"
            + "  }\n"
            + "  pres_adjusted_qc {\n"
            + "    Float64 colorBarMaximum 150.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"Argo reference table 2\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"quality flag\";\n"
            + "  }\n"
            + "  pres_aqdjusted_error {\n"
            + "    Float32 _FillValue 99999.0;\n"
            + "    String C_format \"%7.1f\";\n"
            + "    Float64 colorBarMaximum 50.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String FORTRAN_format \"F7.1\";\n"
            + "    String ioos_category \"Statistics\";\n"
            + "    String long_name \"Contains the error on the adjusted values as determined by the delayed mode QC process\";\n"
            + "    String units \"decibar\";\n"
            + "  }\n"
            + "  temp {\n"
            + "    Float32 _FillValue 99999.0;\n"
            + "    Float32 actual_range -1.855, 27.185;\n"
            + "    String C_format \"%9.3f\";\n"
            + "    Float64 colorBarMaximum 32.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String FORTRAN_format \"F9.3\";\n"
            + "    String ioos_category \"Temperature\";\n"
            + "    String long_name \"Sea temperature in-situ ITS-90 scale\";\n"
            + "    String standard_name \"sea_water_temperature\";\n"
            + "    String units \"degree_Celsius\";\n"
            + "    Float32 valid_max 40.0;\n"
            + "    Float32 valid_min -2.5;\n"
            + "  }\n"
            + "  temp_qc {\n"
            + "    Float64 colorBarMaximum 150.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"Argo reference table 2\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"quality flag\";\n"
            + "  }\n"
            + "  temp_adjusted {\n"
            + "    Float32 _FillValue 99999.0;\n"
            + "    String C_format \"%9.3f\";\n"
            + "    Float64 colorBarMaximum 32.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String FORTRAN_format \"F9.3\";\n"
            + "    String ioos_category \"Temperature\";\n"
            + "    String long_name \"Sea temperature in-situ ITS-90 scale\";\n"
            + "    String standard_name \"sea_water_temperature\";\n"
            + "    String units \"degree_Celsius\";\n"
            + "    Float32 valid_max 40.0;\n"
            + "    Float32 valid_min -2.5;\n"
            + "  }\n"
            + "  temp_adjusted_qc {\n"
            + "    Float64 colorBarMaximum 150.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"Argo reference table 2\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"quality flag\";\n"
            + "  }\n"
            + "  temp_adjusted_error {\n"
            + "    Float32 _FillValue 99999.0;\n"
            + "    String C_format \"%9.3f\";\n"
            + "    Float64 colorBarMaximum 1.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String FORTRAN_format \"F9.3\";\n"
            + "    String ioos_category \"Statistics\";\n"
            + "    String long_name \"Contains the error on the adjusted values as determined by the delayed mode QC process\";\n"
            + "    String units \"degree_Celsius\";\n"
            + "  }\n"
            + "  psal {\n"
            + "    Float32 _FillValue 99999.0;\n"
            + "    Float32 actual_range 15.829, 34.691;\n"
            + "    String C_format \"%9.3f\";\n"
            + "    Float64 colorBarMaximum 37.0;\n"
            + "    Float64 colorBarMinimum 32.0;\n"
            + "    String FORTRAN_format \"F9.3\";\n"
            + "    String ioos_category \"Salinity\";\n"
            + "    String long_name \"Practical salinity\";\n"
            + "    String standard_name \"sea_water_practical_salinity\";\n"
            + "    String units \"PSU\";\n"
            + "    Float32 valid_max 41.0;\n"
            + "    Float32 valid_min 2.0;\n"
            + "  }\n"
            + "  psal_qc {\n"
            + "    Float64 colorBarMaximum 150.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"Argo reference table 2\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"quality flag\";\n"
            + "  }\n"
            + "  psal_adjusted {\n"
            + "    Float32 _FillValue 99999.0;\n"
            + "    String C_format \"%9.3f\";\n"
            + "    Float64 colorBarMaximum 37.0;\n"
            + "    Float64 colorBarMinimum 32.0;\n"
            + "    String FORTRAN_format \"F9.3\";\n"
            + "    String ioos_category \"Salinity\";\n"
            + "    String long_name \"Practical salinity\";\n"
            + "    String standard_name \"sea_water_practical_salinity\";\n"
            + "    String units \"PSU\";\n"
            + "    Float32 valid_max 41.0;\n"
            + "    Float32 valid_min 2.0;\n"
            + "  }\n"
            + "  psal_adjusted_qc {\n"
            + "    Float64 colorBarMaximum 150.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String conventions \"Argo reference table 2\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"quality flag\";\n"
            + "  }\n"
            + "  psal_adjusted_error {\n"
            + "    Float32 _FillValue 99999.0;\n"
            + "    String C_format \"%9.3f\";\n"
            + "    Float64 colorBarMaximum 1.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String FORTRAN_format \"F9.3\";\n"
            + "    String ioos_category \"Statistics\";\n"
            + "    String long_name \"Contains the error on the adjusted values as determined by the delayed mode QC process\";\n"
            + "    String units \"psu\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_altitude_proxy \"pres\";\n"
            + "    String cdm_data_type \"TrajectoryProfile\";\n"
            + "    String cdm_profile_variables \"cycle_number, data_type, format_version, handbook_version, reference_date_time, date_creation, date_update, direction, data_center, dc_reference, data_state_indicator, data_mode, firmware_version, wmo_inst_type, time, time_qc, time_location, latitude, longitude, position_qc, positioning_system, profile_pres_qc, profile_temp_qc, profile_psal_qc, vertical_sampling_scheme\";\n"
            + "    String cdm_trajectory_variables \"platform_number, project_name, pi_name, platform_type, float_serial_no\";\n"
            + "    String Conventions \"Argo-3.1, CF-1.6, COARDS, ACDD-1.3\";\n"
            + "    String creator_email \"support@argo.net\";\n"
            + "    String creator_name \"Argo\";\n"
            + "    String creator_url \"http://www.argo.net/\";\n"
            + "    Float64 Easternmost_Easting 36.42373;\n"
            + "    String featureType \"TrajectoryProfile\";\n"
            + "    Float64 geospatial_lat_max 43.81645;\n"
            + "    Float64 geospatial_lat_min -66.6667;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max 36.42373;\n"
            + "    Float64 geospatial_lon_min -26.250239999999998;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    String history \"";
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

    // 2016-05-09T15:34:11Z (local files)
    // 2016-05-09T15:34:11Z
    // http://localhost:8080/cwexperimental/tabledap/testMultidimNc.das\";
    expected =
        "String infoUrl \"http://www.argo.net/\";\n"
            + "    String institution \"Argo\";\n"
            + "    String keywords \"adjusted, argo, array, assembly, best, centre, centres, charge, coded, CONFIG_MISSION_NUMBER, contains, coriolis, creation, currents, cycle, CYCLE_NUMBER, data, DATA_CENTRE, DATA_MODE, DATA_STATE_INDICATOR, DATA_TYPE, date, DATE_CREATION, DATE_UPDATE, day, days, DC_REFERENCE, degree, delayed, denoting, density, determined, direction, Earth Science > Oceans > Ocean Pressure > Water Pressure, Earth Science > Oceans > Ocean Temperature > Water Temperature, Earth Science > Oceans > Salinity/Density > Salinity, equals, error, estimate, file, firmware, FIRMWARE_VERSION, flag, float, FLOAT_SERIAL_NO, format, FORMAT_VERSION, gdac, geostrophic, global, handbook, HANDBOOK_VERSION, have, identifier, in-situ, instrument, investigator, its, its-90, JULD, JULD_LOCATION, JULD_QC, julian, latitude, level, longitude, missions, mode, name, number, ocean, oceanography, oceans, passed, performed, PI_NAME, PLATFORM_NUMBER, PLATFORM_TYPE, position, POSITION_QC, positioning, POSITIONING_SYSTEM, practical, pres, PRES_ADJUSTED, PRES_ADJUSTED_ERROR, PRES_ADJUSTED_QC, PRES_QC, pressure, principal, process, processing, profile, PROFILE_PRES_QC, PROFILE_PSAL_QC, PROFILE_TEMP_QC, profiles, project, PROJECT_NAME, psal, PSAL_ADJUSTED, PSAL_ADJUSTED_ERROR, PSAL_ADJUSTED_QC, PSAL_QC, quality, rdac, real, real time, real-time, realtime, reference, REFERENCE_DATE_TIME, regional, relative, salinity, sampling, scale, scheme, sea, sea level, sea-level, sea_water_practical_salinity, sea_water_pressure, sea_water_temperature, seawater, serial, situ, station, statistics, system, TEMP, TEMP_ADJUSTED, TEMP_ADJUSTED_ERROR, TEMP_ADJUSTED_QC, TEMP_QC, temperature, through, time, type, unique, update, values, version, vertical, VERTICAL_SAMPLING_SCHEME, water, WMO_INST_TYPE\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    Float64 Northernmost_Northing 43.81645;\n"
            + "    String references \"http://www.argodatamgt.org/Documentation\";\n"
            + "    String source \"Argo float\";\n"
            + "    String sourceUrl \"(local files)\";\n"
            + "    Float64 Southernmost_Northing -66.6667;\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v29\";\n"
            + "    String summary \"Argo float vertical profiles from Coriolis Global Data Assembly Centres\n"
            + "(GDAC). Argo is an international collaboration that collects high-quality\n"
            + "temperature and salinity profiles from the upper 2000m of the ice-free\n"
            + "global ocean and currents from intermediate depths. The data come from\n"
            + "battery-powered autonomous floats that spend most of their life drifting\n"
            + "at depth where they are stabilised by being neutrally buoyant at the\n"
            + "\\\"parking depth\\\" pressure by having a density equal to the ambient pressure\n"
            + "and a compressibility that is less than that of sea water. At present there\n"
            + "are several models of profiling float used in Argo. All work in a similar\n"
            + "fashion but differ somewhat in their design characteristics. At typically\n"
            + "10-day intervals, the floats pump fluid into an external bladder and rise\n"
            + "to the surface over about 6 hours while measuring temperature and salinity.\n"
            + "Satellites or GPS determine the position of the floats when they surface,\n"
            + "and the floats transmit their data to the satellites. The bladder then\n"
            + "deflates and the float returns to its original density and sinks to drift\n"
            + "until the cycle is repeated. Floats are designed to make about 150 such\n"
            + "cycles.\n"
            + "Data Management URL: http://www.argodatamgt.org/Documentation\";\n"
            + "    String time_coverage_end \"2017-06-02T00:00:00Z\";\n"
            + "    String time_coverage_start \"2012-12-27T09:19:57Z\";\n"
            + "    String title \"Argo Float Vertical Profiles\";\n"
            + "    String user_manual_version \"3.1\";\n"
            + "    Float64 Westernmost_Easting -26.250239999999998;\n"
            + "  }\n"
            + "}\n";
    int tPo = results.indexOf(expected.substring(0, 15));
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
        expected,
        "results=\n" + results);

    // char vars that are now strings include these destinationNames:
    // direction (A|D), data_mode (A|D),
    // and all .*_QC (_FillValue=' ') e.g., time_qc, position_qc, profile_pres_qc,
    // pres_qc.

    // dds
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_1", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    String fileNumber;\n"
            + "    String data_type;\n"
            + "    String format_version;\n"
            + "    String handbook_version;\n"
            + "    Float64 reference_date_time;\n"
            + "    Float64 date_creation;\n"
            + "    Float64 date_update;\n"
            + "    String platform_number;\n"
            + "    String project_name;\n"
            + "    String pi_name;\n"
            + "    Int32 cycle_number;\n"
            + "    String direction;\n"
            + "    String data_center;\n"
            + "    String dc_reference;\n"
            + "    String data_state_indicator;\n"
            + "    String data_mode;\n"
            + "    String platform_type;\n"
            + "    String float_serial_no;\n"
            + "    String firmware_version;\n"
            + "    String wmo_inst_type;\n"
            + "    Float64 time;\n"
            + "    String time_qc;\n"
            + "    Float64 time_location;\n"
            + "    Float64 latitude;\n"
            + "    Float64 longitude;\n"
            + "    String position_qc;\n"
            + "    String positioning_system;\n"
            + "    String profile_pres_qc;\n"
            + "    String profile_temp_qc;\n"
            + "    String profile_psal_qc;\n"
            + "    String vertical_sampling_scheme;\n"
            + "    Int32 config_mission_number;\n"
            + "    Float32 pres;\n"
            + "    String pres_qc;\n"
            + "    Float32 pres_adjusted;\n"
            + "    String pres_adjusted_qc;\n"
            + "    Float32 pres_aqdjusted_error;\n"
            + "    Float32 temp;\n"
            + "    String temp_qc;\n"
            + "    Float32 temp_adjusted;\n"
            + "    String temp_adjusted_qc;\n"
            + "    Float32 temp_adjusted_error;\n"
            + "    Float32 psal;\n"
            + "    String psal_qc;\n"
            + "    Float32 psal_adjusted;\n"
            + "    String psal_adjusted_qc;\n"
            + "    Float32 psal_adjusted_error;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // view some data
    userDapQuery =
        "fileNumber,data_type,format_version,handbook_version,reference_date_time,date_creation,date_update,platform_number,project_name,pi_name,cycle_number,direction,data_center,dc_reference,data_state_indicator,data_mode,platform_type,float_serial_no,firmware_version,wmo_inst_type,time,time_qc,time_location,latitude,longitude,position_qc"
            + "&cycle_number<3";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_2", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "fileNumber,data_type,format_version,handbook_version,reference_date_time,date_creation,date_update,platform_number,project_name,pi_name,cycle_number,direction,data_center,dc_reference,data_state_indicator,data_mode,platform_type,float_serial_no,firmware_version,wmo_inst_type,time,time_qc,time_location,latitude,longitude,position_qc\n"
            + ",,,,UTC,UTC,UTC,,,,,,,,,,,,,,UTC,,UTC,degrees_north,degrees_east,\n"
            + "7900364,Argo profile,3.1,1.2,1950-01-01T00:00:00Z,2013-05-24T17:02:04Z,2017-07-07T17:28:19Z,7900364,AWI,Gerd ROHARDT,1,A,IF,29532210,2B,R,NEMO,185,,860,2012-12-27T09:19:57Z,1,2012-12-27T09:19:57Z,-66.3326,-11.662600000000001,1\n"
            + "7900364,Argo profile,3.1,1.2,1950-01-01T00:00:00Z,2013-05-24T17:02:04Z,2017-07-07T17:28:19Z,7900364,AWI,Gerd ROHARDT,2,A,IF,29532211,2B,R,NEMO,185,,860,2012-12-30T05:57:58Z,1,2012-12-30T05:57:58Z,-66.3135,-11.6555,1\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // adding &direction="A" should yield the same results
    userDapQuery =
        "fileNumber,data_type,format_version,handbook_version,reference_date_time,date_creation,date_update,platform_number,project_name,pi_name,cycle_number,direction,data_center,dc_reference,data_state_indicator,data_mode,platform_type,float_serial_no,firmware_version,wmo_inst_type,time,time_qc,time_location,latitude,longitude,position_qc"
            + "&cycle_number<3&direction=\"A\"";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_3", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // test distinct for a char var
    userDapQuery = "position_qc" + "&distinct()";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_4", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "position_qc\n" + "\n" + "1\n" + "2\n" + "4\n" + "8\n";
    // String2.log(results);
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // test > char var
    userDapQuery = "platform_number,cycle_number,position_qc&position_qc>\"3\"&position_qc<\"8\"";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_5", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "platform_number,cycle_number,position_qc\n"
            + ",,\n"
            + "7900364,17,4\n"
            + "7900364,18,4\n"
            + "7900364,21,4\n"
            + "7900364,23,4\n"
            + "7900364,25,4\n"
            + "7900364,27,4\n"
            + "7900364,28,4\n"
            + "7900364,29,4\n";
    // String2.log(results);
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // test = for a char var
    userDapQuery = "platform_number,cycle_number,position_qc&position_qc=\"2\"";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_6", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "platform_number,cycle_number,position_qc\n"
            + ",,\n"
            + "7900594,96,2\n"
            + "7900594,97,2\n"
            + "7900594,101,2\n"
            + "7900594,102,2\n";
    // String2.log(results);
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // **************************** testCharAsChar
    id = "testCharAsChar";
    if (deleteCachedInfo) EDD.deleteCachedDatasetInfo(id);
    eddTable = (EDDTable) EDDTestDataset.gettestCharAsChar();

    // char vars that are now strings include these destinationNames:
    // direction (A|D), data_mode (A|D),
    // and all .*_QC (_FillValue=' ') e.g., time_qc, position_qc, profile_pres_qc,
    // pres_qc.

    // dds
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", dir, eddTable.className() + "_1b", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    String fileNumber;\n"
            + "    String data_type;\n"
            + "    String format_version;\n"
            + "    String handbook_version;\n"
            + "    Float64 reference_date_time;\n"
            + "    Float64 date_creation;\n"
            + "    Float64 date_update;\n"
            + "    String platform_number;\n"
            + "    String project_name;\n"
            + "    String pi_name;\n"
            + "    Int32 cycle_number;\n"
            + "    String direction;\n"
            + // char vars show up as String, I think because .dds doesn't support char
            "    String data_center;\n"
            + "    String dc_reference;\n"
            + "    String data_state_indicator;\n"
            + "    String data_mode;\n"
            + "    String platform_type;\n"
            + "    String float_serial_no;\n"
            + "    String firmware_version;\n"
            + "    String wmo_inst_type;\n"
            + "    Float64 time;\n"
            + "    String time_qc;\n"
            + "    Float64 time_location;\n"
            + "    Float64 latitude;\n"
            + "    Float64 longitude;\n"
            + "    String position_qc;\n"
            + "    String positioning_system;\n"
            + "    String profile_pres_qc;\n"
            + "    String profile_temp_qc;\n"
            + "    String profile_psal_qc;\n"
            + "    String vertical_sampling_scheme;\n"
            + "    Int32 config_mission_number;\n"
            + "    Float32 pres;\n"
            + "    String pres_qc;\n"
            + "    Float32 pres_adjusted;\n"
            + "    String pres_adjusted_qc;\n"
            + "    Float32 pres_aqdjusted_error;\n"
            + "    Float32 temp;\n"
            + "    String temp_qc;\n"
            + "    Float32 temp_adjusted;\n"
            + "    String temp_adjusted_qc;\n"
            + "    Float32 temp_adjusted_error;\n"
            + "    Float32 psal;\n"
            + "    String psal_qc;\n"
            + "    Float32 psal_adjusted;\n"
            + "    String psal_adjusted_qc;\n"
            + "    Float32 psal_adjusted_error;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // view some data
    userDapQuery =
        "fileNumber,data_type,format_version,handbook_version,reference_date_time,date_creation,date_update,platform_number,project_name,pi_name,cycle_number,direction,data_center,dc_reference,data_state_indicator,data_mode,platform_type,float_serial_no,firmware_version,wmo_inst_type,time,time_qc,time_location,latitude,longitude,position_qc"
            + "&cycle_number<3";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_2b", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "fileNumber,data_type,format_version,handbook_version,reference_date_time,date_creation,date_update,platform_number,project_name,pi_name,cycle_number,direction,data_center,dc_reference,data_state_indicator,data_mode,platform_type,float_serial_no,firmware_version,wmo_inst_type,time,time_qc,time_location,latitude,longitude,position_qc\n"
            + ",,,,UTC,UTC,UTC,,,,,,,,,,,,,,UTC,,UTC,degrees_north,degrees_east,\n"
            + "7900364,Argo profile,3.1,1.2,1950-01-01T00:00:00Z,2013-05-24T17:02:04Z,2017-07-07T17:28:19Z,7900364,AWI,Gerd ROHARDT,1,A,IF,29532210,2B,R,NEMO,185,,860,2012-12-27T09:19:57Z,1,2012-12-27T09:19:57Z,-66.3326,-11.662600000000001,1\n"
            + "7900364,Argo profile,3.1,1.2,1950-01-01T00:00:00Z,2013-05-24T17:02:04Z,2017-07-07T17:28:19Z,7900364,AWI,Gerd ROHARDT,2,A,IF,29532211,2B,R,NEMO,185,,860,2012-12-30T05:57:58Z,1,2012-12-30T05:57:58Z,-66.3135,-11.6555,1\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // adding &direction="A" should yield the same results
    userDapQuery =
        "fileNumber,data_type,format_version,handbook_version,reference_date_time,date_creation,date_update,platform_number,project_name,pi_name,cycle_number,direction,data_center,dc_reference,data_state_indicator,data_mode,platform_type,float_serial_no,firmware_version,wmo_inst_type,time,time_qc,time_location,latitude,longitude,position_qc"
            + "&cycle_number<3&direction=\"A\"";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_3b", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // test distinct for a char var
    userDapQuery = "position_qc" + "&distinct()";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_4b", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "position_qc\n" + "\n" + "1\n" + "2\n" + "4\n" + "8\n";
    // String2.log(results);
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // test > char var
    userDapQuery = "platform_number,cycle_number,position_qc&position_qc>\"3\"&position_qc<\"8\"";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_5b", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "platform_number,cycle_number,position_qc\n"
            + ",,\n"
            + "7900364,17,4\n"
            + "7900364,18,4\n"
            + "7900364,21,4\n"
            + "7900364,23,4\n"
            + "7900364,25,4\n"
            + "7900364,27,4\n"
            + "7900364,28,4\n"
            + "7900364,29,4\n";
    // String2.log(results);
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // test = for a char var
    userDapQuery = "platform_number,cycle_number,position_qc&position_qc=\"2\"";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_6c", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "platform_number,cycle_number,position_qc\n"
            + ",,\n"
            + "7900594,96,2\n"
            + "7900594,97,2\n"
            + "7900594,101,2\n"
            + "7900594,102,2\n";
    // String2.log(results);
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
  }

  @org.junit.jupiter.api.Test
  @TagSlowTests
  void testScriptOnlyRequest() throws Throwable {

    EDDTableFromMultidimNcFiles edd = (EDDTableFromMultidimNcFiles) EDDTestDataset.getTS_ATMP_AAD();
    String dir = EDStatic.fullTestCacheDirectory;
    // edd.makeNewFileForDapQuery(0, null, null, )
    String fileTypeExtension = ".csv";
    String fileName = "testScriptOnlyRequest" + fileTypeExtension;
    String fullName = dir + fileName;
    OutputStreamSource outputStreamSource =
        new OutputStreamSourceSimple(new BufferedOutputStream(new FileOutputStream(fullName)));
    edd.respondToDapQuery(
        0,
        null,
        null,
        null,
        null,
        "erddap/tabledap/TS_ATMP_AAD.csv",
        "TS_ATMP_AAD.csv",
        "url_metadata&distinct()",
        outputStreamSource,
        dir,
        fileName,
        fileTypeExtension);
    String results = File2.directReadFrom88591File(fullName);
    assertEquals(
        "url_metadata\n"
            + //
            "\n"
            + //
            "https://data-erddap.emodnet-physics.eu/erddap/tabledap/EP_PLATFORMS_METADATA.htmlTable?&PLATFORMCODE=%22Casey Skiway%22&integrator_id=%22aad%22&distinct()\n"
            + //
            "https://data-erddap.emodnet-physics.eu/erddap/tabledap/EP_PLATFORMS_METADATA.htmlTable?&PLATFORMCODE=%22Casey%22&integrator_id=%22aad%22&distinct()\n"
            + //
            "https://data-erddap.emodnet-physics.eu/erddap/tabledap/EP_PLATFORMS_METADATA.htmlTable?&PLATFORMCODE=%22Davis%22&integrator_id=%22aad%22&distinct()\n"
            + //
            "https://data-erddap.emodnet-physics.eu/erddap/tabledap/EP_PLATFORMS_METADATA.htmlTable?&PLATFORMCODE=%22Macquarie Island%22&integrator_id=%22aad%22&distinct()\n"
            + //
            "https://data-erddap.emodnet-physics.eu/erddap/tabledap/EP_PLATFORMS_METADATA.htmlTable?&PLATFORMCODE=%22Mawson%22&integrator_id=%22aad%22&distinct()\n",
        results);
  }
}
