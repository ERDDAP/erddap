package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tags.TagIncompleteTest;
import tags.TagMissingFile;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

class EDDTableFromNcCFFilesTests {

  @BeforeAll
  static void init() {
    Initialization.edStatic();
    Table.debugMode = true;
  }

  /**
   * testGenerateDatasetsXml. This doesn't test suggestTestOutOfDate, except that for old data it
   * doesn't suggest anything.
   */
  @org.junit.jupiter.api.Test
  void testGenerateDatasetsXml() throws Throwable {
    // testVerboseOn();
    // debugMode = true;

    // public static String generateDatasetsXml(
    // String tFileDir, String tFileNameRegex, String sampleFileName,
    // int tReloadEveryNMinutes,
    // String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex,
    // String tColumnNameForExtract,
    // String tSortFilesBySourceNames,
    // String tInfoUrl, String tInstitution, String tSummary, String tTitle,
    // Attributes externalAddGlobalAttributes) throws Throwable {
    String dataDir =
        File2.addSlash(
            Path.of(EDDTableFromNcCFFilesTests.class.getResource("/data/nccf/").toURI())
                .toString());
    String fileNameRegex = "ncCF1b\\.nc";
    String results =
        EDDTableFromNcCFFiles.generateDatasetsXml(
                dataDir,
                fileNameRegex,
                dataDir + "/ncCF1b.nc",
                1440,
                "",
                "",
                "",
                "", // just for test purposes; station is already a column in the file
                "line_station time",
                "",
                "",
                "",
                "",
                -1,
                null, // defaultStandardizeWhat
                null)
            + "\n";

    // GenerateDatasetsXml
    String gdxResults =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {
                  "-verbose",
                  "EDDTableFromNcCFFiles",
                  dataDir,
                  fileNameRegex,
                  dataDir + "/ncCF1b.nc",
                  "1440",
                  "",
                  "",
                  "",
                  "", // just for test purposes; station is already a column in the file
                  "line_station time",
                  "",
                  "",
                  "",
                  "",
                  "",
                  "-1",
                  ""
                }, // defaultStandardizeWhat
                false); // doIt loop?
    Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");
    String tDatasetID = EDDTableFromNcCFFiles.suggestDatasetID(dataDir + fileNameRegex);
    String expected =
        "<dataset type=\"EDDTableFromNcCFFiles\" datasetID=\""
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
            + "    <sortFilesBySourceNames>line_station time</sortFilesBySourceNames>\n"
            + "    <fileTableInMemory>false</fileTableInMemory>\n"
            + "    <!-- sourceAttributes>\n"
            + "        <att name=\"cdm_data_type\">TimeSeries</att>\n"
            + "        <att name=\"cdm_timeseries_variables\">line_station</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.6, Unidata Dataset Discovery v1.0</att>\n"
            + "        <att name=\"Easternmost_Easting\" type=\"float\">-123.49333</att>\n"
            + "        <att name=\"featureType\">TimeSeries</att>\n"
            + "        <att name=\"geospatial_lat_max\" type=\"float\">33.388332</att>\n"
            + "        <att name=\"geospatial_lat_min\" type=\"float\">32.245</att>\n"
            + "        <att name=\"geospatial_lat_units\">degrees_north</att>\n"
            + "        <att name=\"geospatial_lon_max\" type=\"float\">-123.49333</att>\n"
            + "        <att name=\"geospatial_lon_min\" type=\"float\">-124.32333</att>\n"
            + "        <att name=\"geospatial_lon_units\">degrees_east</att>\n"
            + "        <att name=\"geospatial_vertical_max\" type=\"float\">-211.5</att>\n"
            + "        <att name=\"geospatial_vertical_min\" type=\"float\">-216.7</att>\n"
            + "        <att name=\"geospatial_vertical_positive\">up</att>\n"
            + "        <att name=\"geospatial_vertical_units\">m</att>\n"
            + "        <att name=\"history\">Data originally from CalCOFI project.\n"
            + "At ERD, Roy Mendelssohn processed the data into .nc files.\n"
            + "2010-12-28 At ERD, Lynn DeWitt made the files available to Bob Simons.\n"
            + "2010-12-31 Bob Simons reprocessed the files with Projects.calcofiBio().\n"
            + "2012-08-02T16:13:53Z (local files)\n"
            + "2012-08-02T16:13:53Z http://127.0.0.1:8080/cwexperimental/tabledap/erdCalcofiBio.ncCF?line_station,longitude,latitude,altitude,time,obsScientific,obsValue,obsUnits&amp;station=100.0&amp;time&gt;=2004-11-12T00:00:00Z&amp;time&lt;=2004-11-19T08:32:00Z&amp;obsUnits=&#37;22number&#37;20of&#37;20larvae&#37;22&amp;orderBy&#37;28&#37;22line_station,time,obsScientific&#37;22&#37;29</att>\n"
            + "        <att name=\"id\">ncCF1b</att>\n"
            + "        <att name=\"infoUrl\">http://www.calcofi.org/newhome/publications/Atlases/atlases.htm</att>\n"
            + "        <att name=\"institution\">CalCOFI</att>\n"
            + "        <att name=\"keywords\">Biosphere &gt; Aquatic Ecosystems &gt; Coastal Habitat,\n"
            + "Biosphere &gt; Aquatic Ecosystems &gt; Marine Habitat,\n"
            + "Biological Classification &gt; Animals/Vertebrates &gt; Fish,\n"
            + "Oceans &gt; Aquatic Sciences &gt; Fisheries,\n"
            + "1984-2004, altitude, atmosphere, biology, calcofi, code, common, count, cruise, fish, height, identifier, larvae, line, name, number, observed, occupancy, order, scientific, ship, start, station, time, tow, units, value</att>\n"
            + "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n"
            + "        <att name=\"license\">The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.</att>\n"
            + "        <att name=\"Metadata_Conventions\">COARDS, CF-1.6, Unidata Dataset Discovery v1.0</att>\n"
            + "        <att name=\"Northernmost_Northing\" type=\"float\">33.388332</att>\n"
            + "        <att name=\"sourceUrl\">(local files)</att>\n"
            + "        <att name=\"Southernmost_Northing\" type=\"float\">32.245</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF-12</att>\n"
            + "        <att name=\"subsetVariables\">line_station</att>\n"
            + "        <att name=\"summary\">This is the CalCOFI distributional atlas of fish larvae. Routine oceanographic sampling within the California Current System has occurred under the auspices of the California Cooperative Oceanic Fisheries Investigations (CalCOFI) since 1949, providing one of the longest existing time-series of the physics, chemistry and biology of a dynamic oceanic regime.</att>\n"
            + "        <att name=\"time_coverage_end\">2004-11-16T21:20:00Z</att>\n"
            + "        <att name=\"time_coverage_start\">2004-11-12T16:26:00Z</att>\n"
            + "        <att name=\"title\">CalCOFI Fish Larvae Count, 1984-2004</att>\n"
            + "        <att name=\"Westernmost_Easting\" type=\"float\">-124.32333</att>\n"
            + "    </sourceAttributes -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
            + "        <att name=\"creator_name\">CalCOFI</att>\n"
            + "        <att name=\"creator_type\">institution</att>\n"
            + "        <att name=\"creator_url\">http://www.calcofi.org/newhome/publications/Atlases/atlases.htm</att>\n"
            + "        <att name=\"keywords\">1984-2004, altitude, animals, animals/vertebrates, aquatic, atmosphere, biological, biology, biosphere, calcofi, california, classification, coastal, code, common, cooperative, count, cruise, data, earth, Earth Science &gt; Atmosphere &gt; Altitude &gt; Station Height, Earth Science &gt; Biological Classification &gt; Animals/Vertebrates &gt; Fish, Earth Science &gt; Biosphere &gt; Aquatic Ecosystems &gt; Coastal Habitat, Earth Science &gt; Biosphere &gt; Aquatic Ecosystems &gt; Marine Habitat, Earth Science &gt; Oceans &gt; Aquatic Sciences &gt; Fisheries, ecosystems, fish, fisheries, habitat, height, identifier, investigations, larvae, latitude, line, line_station, longitude, marine, name, number, observed, obsScientific, obsUnits, obsValue, occupancy, ocean, oceanic, oceans, order, science, sciences, scientific, ship, start, station, time, tow, units, value, vertebrates</att>\n"
            + "        <att name=\"Metadata_Conventions\">null</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "    </addAttributes>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>line_station</sourceName>\n"
            + "        <destinationName>line_station</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"cf_role\">timeseries_id</att>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "            <att name=\"long_name\">CalCOFI Line + Station</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>longitude</sourceName>\n"
            + "        <destinationName>longitude</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Lon</att>\n"
            + "            <att name=\"actual_range\" type=\"floatList\">-124.32333 -123.49333</att>\n"
            + "            <att name=\"axis\">X</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Longitude</att>\n"
            + "            <att name=\"standard_name\">longitude</att>\n"
            + "            <att name=\"units\">degrees_east</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>latitude</sourceName>\n"
            + "        <destinationName>latitude</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Lat</att>\n"
            + "            <att name=\"actual_range\" type=\"floatList\">32.245 33.388332</att>\n"
            + "            <att name=\"axis\">Y</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Latitude</att>\n"
            + "            <att name=\"standard_name\">latitude</att>\n"
            + "            <att name=\"units\">degrees_north</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>altitude</sourceName>\n"
            + "        <destinationName>altitude</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Height</att>\n"
            + "            <att name=\"_CoordinateZisPositive\">up</att>\n"
            + "            <att name=\"actual_range\" type=\"floatList\">-216.7 -211.5</att>\n"
            + "            <att name=\"axis\">Z</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Altitude at Start of Tow</att>\n"
            + "            <att name=\"positive\">up</att>\n"
            + "            <att name=\"standard_name\">altitude</att>\n"
            + "            <att name=\"units\">m</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">-210.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-218.0</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>time</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Time</att>\n"
            + "            <att name=\"actual_range\" type=\"doubleList\">1.10027676E9 1.10064E9</att>\n"
            + "            <att name=\"axis\">T</att>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"long_name\">Time</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"time_origin\">01-JAN-1970 00:00:00</att>\n"
            + "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">1.1007E9</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">1.1002E9</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>obsScientific</sourceName>\n"
            + "        <destinationName>obsScientific</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"coordinates\">time latitude longitude altitude</att>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "            <att name=\"long_name\">Observed (Scientific Name)</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"coordinates\">null</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>obsValue</sourceName>\n"
            + "        <destinationName>obsValue</destinationName>\n"
            + "        <dataType>int</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"actual_range\" type=\"intList\">1 22</att>\n"
            + "            <att name=\"coordinates\">time latitude longitude altitude</att>\n"
            + "            <att name=\"ioos_category\">Biology</att>\n"
            + "            <att name=\"long_name\">Observed Value</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"int\">2147483647</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">25.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"coordinates\">null</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>obsUnits</sourceName>\n"
            + "        <destinationName>obsUnits</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"coordinates\">time latitude longitude altitude</att>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "            <att name=\"long_name\">Observed Units</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"coordinates\">null</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    // Test.ensureEqual(results.substring(0, Math.min(results.length(),
    // expected.length())),
    // expected, "");

    // String tDatasetID = "ncCF1b_983e_1fc7_39ff";
    EDD.deleteCachedDatasetInfo(tDatasetID);
    EDD edd = EDDTableFromNcCFFiles.oneFromXmlFragment(null, results);
    Test.ensureEqual(edd.datasetID(), tDatasetID, "");
    Test.ensureEqual(edd.title(), "CalCOFI Fish Larvae Count, 1984-2004", "");
    Test.ensureEqual(
        String2.toCSSVString(edd.dataVariableDestinationNames()),
        "line_station, longitude, latitude, altitude, time, obsScientific, obsValue, obsUnits",
        "");
  }

  /**
   * testGenerateDatasetsXml2. This doesn't test suggestTestOutOfDate, except that for old data it
   * doesn't suggest anything.
   */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest // Simple ERROR on line #1 of ind199105_ctd.nc *GLOBAL*,Conventions,"...,
  // NCCSV-..." not found on line 1.
  void testGenerateDatasetsXml2() throws Throwable {
    // testVerboseOn();
    // debugMode = true;

    // public static String generateDatasetsXml(
    // String tFileDir, String tFileNameRegex, String sampleFileName,
    // int tReloadEveryNMinutes,
    // String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex,
    // String tColumnNameForExtract,
    // String tSortFilesBySourceNames,
    // String tInfoUrl, String tInstitution, String tSummary, String tTitle,
    // Attributes externalAddGlobalAttributes) throws Throwable {

    // From Ajay Krishnan, NCEI/NODC, from
    // https://data.nodc.noaa.gov/thredds/catalog/testdata/wod_ragged/05052016/catalog.html?dataset=testdata/wod_ragged/05052016/ind199105_ctd.nc
    // See low level reading test: Table.testReadNcCF7SampleDims()
    String dataDir =
        File2.addSlash(
            Path.of(EDDTableFromNcCFFilesTests.class.getResource("/data/nccf/ncei/").toURI())
                .toString());
    String fileNameRegex = "ind199105_ctd\\.nc";

    String results =
        EDDTableFromNccsvFiles.generateDatasetsXml(
                dataDir,
                fileNameRegex,
                "",
                1440,
                "",
                "",
                "",
                "", // just for test purposes; station is already a column in the file
                "WOD_cruise_identifier, time",
                "",
                "",
                "",
                "",
                -1,
                null, // defaultStandardizeWhat
                null)
            + "\n";

    // GenerateDatasetsXml
    String gdxResults =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {
                  "-verbose",
                  "EDDTableFromNcCFFiles",
                  dataDir,
                  fileNameRegex,
                  "",
                  "1440",
                  "",
                  "",
                  "",
                  "", // just for test purposes; station is already a column in the file
                  "WOD_cruise_identifier, time",
                  "",
                  "",
                  "",
                  "",
                  "",
                  "-1",
                  ""
                }, // defaultStandardizeWhat
                false); // doIt loop?
    Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");
    String tDatasetID = EDDTableFromNcCFFiles.suggestDatasetID(dataDir + fileNameRegex);
    String expected =
        "<dataset type=\"EDDTableFromNcCFFiles\" datasetID=\""
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
            + "    <sortFilesBySourceNames>WOD_cruise_identifier, time</sortFilesBySourceNames>\n"
            + "    <fileTableInMemory>false</fileTableInMemory>\n"
            + "    <!-- sourceAttributes>\n"
            + "        <att name=\"cdm_data_type\">Profile</att>\n"
            + "        <att name=\"cdm_profile_variables\">country, WOD_cruise_identifier, originators_cruise_identifier, wod_unique_cast, lat, lon, time, date, GMT_time, Access_no, Project, Platform, Institute, Cast_Tow_number, Orig_Stat_Num, Bottom_Depth, Cast_Duration, Cast_Direction, High_res_pair, dataset, dbase_orig, origflagset, Temperature_row_size, Temperature_WODprofileflag, Temperature_Scale, Temperature_Instrument, Salinity_row_size, Salinity_WODprofileflag, Salinity_Scale, Salinity_Instrument, Oxygen_row_size, Oxygen_WODprofileflag, Oxygen_Instrument, Oxygen_Original_units, Pressure_row_size, Chlorophyll_row_size, Chlorophyll_WODprofileflag, Chlorophyll_Instrument, Chlorophyll_uncalibrated, Conductivit_row_size, crs, WODf, WODfp, WODfd</att>\n"
            + "        <att name=\"Conventions\">CF-1.6</att>\n"
            + "        <att name=\"creator_email\">OCLhelp@noaa.gov</att>\n"
            + "        <att name=\"creator_name\">Ocean Climate Lab/NODC</att>\n"
            + "        <att name=\"creator_url\">http://www.nodc.noaa.gov</att>\n"
            + "        <att name=\"date_created\">2016-05-02</att>\n"
            + "        <att name=\"date_modified\">2016-05-02</att>\n"
            + "        <att name=\"featureType\">Profile</att>\n"
            + "        <att name=\"geospatial_lat_max\" type=\"float\">13.273334</att>\n"
            + "        <att name=\"geospatial_lat_min\" type=\"float\">-48.9922</att>\n"
            + "        <att name=\"geospatial_lat_resolution\">point</att>\n"
            + "        <att name=\"geospatial_lon_max\" type=\"float\">147.0</att>\n"
            + "        <att name=\"geospatial_lon_min\" type=\"float\">43.986668</att>\n"
            + "        <att name=\"geospatial_lon_resolution\">point</att>\n"
            + "        <att name=\"geospatial_vertical_max\" type=\"float\">5088.485</att>\n"
            + "        <att name=\"geospatial_vertical_min\" type=\"float\">0.99160606</att>\n"
            + "        <att name=\"geospatial_vertical_positive\">down</att>\n"
            + "        <att name=\"geospatial_vertical_units\">meters</att>\n"
            + "        <att name=\"grid_mapping_epsg_code\">EPSG:4326</att>\n"
            + "        <att name=\"grid_mapping_inverse_flattening\" type=\"float\">298.25723</att>\n"
            + "        <att name=\"grid_mapping_longitude_of_prime_meridian\" type=\"float\">0.0</att>\n"
            + "        <att name=\"grid_mapping_name\">latitude_longitude</att>\n"
            + "        <att name=\"grid_mapping_semi_major_axis\" type=\"float\">6378137.0</att>\n"
            + "        <att name=\"id\">ind199105_ctd.nc</att>\n"
            + "        <att name=\"institution\">National Oceanographic Data Center(NODC), NOAA</att>\n"
            + "        <att name=\"naming_authority\">gov.noaa.nodc</att>\n"
            + "        <att name=\"project\">World Ocean Database</att>\n"
            + "        <att name=\"publisher_email\">NODC.Services@noaa.gov</att>\n"
            + "        <att name=\"publisher_name\">US DOC; NESDIS; NATIONAL OCEANOGRAPHIC DATA CENTER - IN295</att>\n"
            + "        <att name=\"publisher_url\">http://www.nodc.noaa.gov</att>\n"
            + "        <att name=\"references\">World Ocean Database 2013. URL:http://data.nodc.noaa.gov/woa/WOD/DOC/wod_intro.pdf</att>\n"
            + "        <att name=\"source\">World Ocean Database</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF-1.6</att>\n"
            + "        <att name=\"subsetVariables\">country, WOD_cruise_identifier, originators_cruise_identifier, wod_unique_cast, lat, lon, time, date, GMT_time, Access_no, Project, Platform, Institute, Cast_Tow_number, Orig_Stat_Num, Bottom_Depth, Cast_Duration, Cast_Direction, High_res_pair, dataset, dbase_orig, origflagset, Temperature_row_size, Temperature_WODprofileflag, Temperature_Scale, Temperature_Instrument, Salinity_row_size, Salinity_WODprofileflag, Salinity_Scale, Salinity_Instrument, Oxygen_row_size, Oxygen_WODprofileflag, Oxygen_Instrument, Oxygen_Original_units, Pressure_row_size, Chlorophyll_row_size, Chlorophyll_WODprofileflag, Chlorophyll_Instrument, Chlorophyll_uncalibrated, Conductivit_row_size, crs, WODf, WODfp, WODfd</att>\n"
            + "        <att name=\"summary\">Data for multiple casts from the World Ocean Database</att>\n"
            + "        <att name=\"time_coverage_end\">1991-05-31</att>\n"
            + "        <att name=\"time_coverage_start\">1991-05-01</att>\n"
            + "        <att name=\"title\">World Ocean Database - Multi-cast file</att>\n"
            + "    </sourceAttributes -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"Conventions\">CF-1.10, COARDS, ACDD-1.3</att>\n"
            + "        <att name=\"creator_type\">institution</att>\n"
            + "        <att name=\"creator_url\">https://www.nodc.noaa.gov</att>\n"
            + "        <att name=\"history\">World Ocean Database</att>\n"
            + "        <att name=\"infoUrl\">https://www.nodc.noaa.gov</att>\n"
            + "        <att name=\"institution\">NGDC(NODC), NOAA</att>\n"
            + "        <att name=\"keywords\">Access_no, accession, bathymetry, below, cast, Cast_Direction, Cast_Duration, Cast_Tow_number, center, chemistry, chlorophyll, Chlorophyll_Instrument, Chlorophyll_row_size, Chlorophyll_uncalibrated, Chlorophyll_WODprofileflag, color, concentration, concentration_of_chlorophyll_in_sea_water, conductivit, Conductivit_row_size, country, crs, cruise, data, database, dataset, date, dbase_orig, depth, direction, dissolved, dissolved o2, duration, earth, Earth Science &gt; Oceans &gt; Bathymetry/Seafloor Topography &gt; Bathymetry, Earth Science &gt; Oceans &gt; Ocean Chemistry &gt; Chlorophyll, file, flag, floor, geophysical, GMT_time, high, High_res_pair, identifier, institute, instrument, latitude, level, longitude, measured, multi, multi-cast, name, national, ncei, nesdis, ngdc, noaa, nodc, number, O2, observation, observations, ocean, ocean color, oceanographic, oceans, Orig_Stat_Num, origflagset, origin, original, originators, originators_cruise_identifier, oxygen, Oxygen_Instrument, Oxygen_Original_units, Oxygen_row_size, Oxygen_WODprofileflag, pair, platform, pressure, Pressure_row_size, profile, project, quality, resolution, responsible, salinity, Salinity_Instrument, Salinity_row_size, Salinity_Scale, Salinity_WODprofileflag, scale, science, sea, sea_floor_depth, seafloor, seawater, sigfig, station, statistics, temperature, Temperature_Instrument, Temperature_row_size, Temperature_Scale, Temperature_WODprofileflag, time, topography, tow, unique, units, upon, values, water, which, wod, WOD_cruise_identifier, wod_unique_cast, WODf, WODfd, wodflag, WODfp, wodprofileflag, world, z_sigfig, z_WODflag</att>\n"
            + "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n"
            + "        <att name=\"license\">[standard]</att>\n"
            + "        <att name=\"publisher_type\">institution</att>\n"
            + "        <att name=\"publisher_url\">https://www.nodc.noaa.gov</att>\n"
            + "        <att name=\"references\">World Ocean Database 2013. URL:https://data.nodc.noaa.gov/woa/WOD/DOC/wod_intro.pdf</att>\n"
            + "        <att name=\"sourceUrl\">(local files)</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"summary\">World Ocean Database - Multi-cast file. Data for multiple casts from the World Ocean Database</att>\n"
            + "        <att name=\"title\">World Ocean Database, Multi-cast file</att>\n"
            + "    </addAttributes>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>country</sourceName>\n"
            + "        <destinationName>country</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Country</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>WOD_cruise_identifier</sourceName>\n"
            + "        <destinationName>WOD_cruise_identifier</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"comment\">two byte country code + WOD cruise number (unique to country code)</att>\n"
            + "            <att name=\"long_name\">WOD_cruise_identifier</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>originators_cruise_identifier</sourceName>\n"
            + "        <destinationName>originators_cruise_identifier</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "            <att name=\"long_name\">Originators Cruise Identifier</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>wod_unique_cast</sourceName>\n"
            + "        <destinationName>wod_unique_cast</destinationName>\n"
            + "        <dataType>int</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"cf_role\">profile_id</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"int\">2147483647</att>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "            <att name=\"long_name\">Wod Unique Cast</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>lat</sourceName>\n"
            + "        <destinationName>latitude</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">latitude</att>\n"
            + "            <att name=\"standard_name\">latitude</att>\n"
            + "            <att name=\"units\">degrees_north</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Latitude</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>lon</sourceName>\n"
            + "        <destinationName>longitude</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">longitude</att>\n"
            + "            <att name=\"standard_name\">longitude</att>\n"
            + "            <att name=\"units\">degrees_east</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Longitude</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>time</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">time</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"units\">days since 1770-01-01 00:00:00</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"units\">days since 1770-01-01T00:00:00Z</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>date</sourceName>\n"
            + "        <destinationName>date</destinationName>\n"
            + "        <dataType>int</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"comment\">YYYYMMDD</att>\n"
            + "            <att name=\"long_name\">date</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"int\">2147483647</att>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>GMT_time</sourceName>\n"
            + "        <destinationName>GMT_time</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">GMT_time</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Access_no</sourceName>\n"
            + "        <destinationName>Access_no</destinationName>\n"
            + "        <dataType>int</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"comment\">used to find original data at NODC</att>\n"
            + "            <att name=\"long_name\">NODC_accession_number</att>\n"
            + "            <att name=\"units_wod\">NODC_code</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"int\">2147483647</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Statistics</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Project</sourceName>\n"
            + "        <destinationName>Project</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"comment\">name or acronym of project under which data were measured</att>\n"
            + "            <att name=\"long_name\">Project_name</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Identifier</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Platform</sourceName>\n"
            + "        <destinationName>Platform</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"comment\">name of platform from which measurements were taken</att>\n"
            + "            <att name=\"long_name\">Platform_name</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Institute</sourceName>\n"
            + "        <destinationName>Institute</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"comment\">name of institute which collected data</att>\n"
            + "            <att name=\"long_name\">Responsible_institute</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Cast_Tow_number</sourceName>\n"
            + "        <destinationName>Cast_Tow_number</destinationName>\n"
            + "        <dataType>int</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"comment\">originator assigned sequential cast or tow_no</att>\n"
            + "            <att name=\"long_name\">Cast_or_Tow_number</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"int\">-2147483647</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Statistics</att>\n"
            + "            <att name=\"missing_value\" type=\"int\">2147483647</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Orig_Stat_Num</sourceName>\n"
            + "        <destinationName>Orig_Stat_Num</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"comment\">number assigned to a given station by data originator</att>\n"
            + "            <att name=\"long_name\">Originators_Station_Number</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">9.96921E36</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Statistics</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Bottom_Depth</sourceName>\n"
            + "        <destinationName>depth</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">Bottom_Depth</att>\n"
            + "            <att name=\"units\">meters</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">8000.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-8000.0</att>\n"
            + "            <att name=\"colorBarPalette\">TopographyDepth</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"source_name\">Bottom_Depth</att>\n"
            + "            <att name=\"standard_name\">sea_floor_depth</att>\n"
            + "            <att name=\"units\">m</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Cast_Duration</sourceName>\n"
            + "        <destinationName>Cast_Duration</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">Cast_Duration</att>\n"
            + "            <att name=\"units\">hours</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">9.96921E36</att>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Cast_Direction</sourceName>\n"
            + "        <destinationName>Cast_Direction</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">Cast_Direction</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>High_res_pair</sourceName>\n"
            + "        <destinationName>High_res_pair</destinationName>\n"
            + "        <dataType>int</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"comment\">WOD unique cast number for bottle/CTD from same rosette</att>\n"
            + "            <att name=\"long_name\">WOD_high_resolution_pair_number</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"int\">-2147483647</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Statistics</att>\n"
            + "            <att name=\"missing_value\" type=\"int\">2147483647</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>dataset</sourceName>\n"
            + "        <destinationName>dataset</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">WOD_dataset</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>dbase_orig</sourceName>\n"
            + "        <destinationName>dbase_orig</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"comment\">Database from which data were extracted</att>\n"
            + "            <att name=\"long_name\">database_origin</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Database Origin</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>origflagset</sourceName>\n"
            + "        <destinationName>origflagset</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"comment\">set of originators flag codes to use</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "            <att name=\"long_name\">Origflagset</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>z</sourceName>\n"
            + "        <destinationName>z</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">depth_below_sea_level</att>\n"
            + "            <att name=\"positive\">down</att>\n"
            + "            <att name=\"standard_name\">altitude</att>\n"
            + "            <att name=\"units\">m</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">8000.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-8000.0</att>\n"
            + "            <att name=\"colorBarPalette\">TopographyDepth</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Depth Below Sea Level</att>\n"
            + "            <att name=\"standard_name\">depth</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>z_WODflag</sourceName>\n"
            + "        <destinationName>z_WODflag</destinationName>\n"
            + "        <dataType>short</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"flag_definitions\">WODfd</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"short\">32767</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "            <att name=\"long_name\">Z WODflag</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>z_sigfig</sourceName>\n"
            + "        <destinationName>z_sigfig</destinationName>\n"
            + "        <dataType>short</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"short\">32767</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Z Sigfig</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Temperature_row_size</sourceName>\n"
            + "        <destinationName>Temperature_row_size</destinationName>\n"
            + "        <dataType>short</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">number of Temperature observations for this cast</att>\n"
            + "            <att name=\"sample_dimension\">Temperature_obs</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"short\">32767</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Statistics</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Temperature_WODprofileflag</sourceName>\n"
            + "        <destinationName>Temperature_WODprofileflag</destinationName>\n"
            + "        <dataType>short</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"flag_definitions\">WODfp</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"short\">32767</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "            <att name=\"long_name\">Temperature WODprofileflag</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Temperature_Scale</sourceName>\n"
            + "        <destinationName>Temperature_Scale</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">Scale upon which values were measured</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Temperature</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Temperature_Instrument</sourceName>\n"
            + "        <destinationName>Temperature_Instrument</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"comment\">Device used for measurement</att>\n"
            + "            <att name=\"long_name\">Instrument</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Temperature</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Salinity_row_size</sourceName>\n"
            + "        <destinationName>Salinity_row_size</destinationName>\n"
            + "        <dataType>short</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">number of Salinity observations for this cast</att>\n"
            + "            <att name=\"sample_dimension\">Salinity_obs</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"short\">32767</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Statistics</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Salinity_WODprofileflag</sourceName>\n"
            + "        <destinationName>Salinity_WODprofileflag</destinationName>\n"
            + "        <dataType>short</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"flag_definitions\">WODfp</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"short\">32767</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "            <att name=\"long_name\">Salinity WODprofileflag</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Salinity_Scale</sourceName>\n"
            + "        <destinationName>Salinity_Scale</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">Scale upon which values were measured</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Salinity</att>\n"
            + // no standard_name or units because String
            // data
            "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Salinity_Instrument</sourceName>\n"
            + "        <destinationName>Salinity_Instrument</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"comment\">Device used for measurement</att>\n"
            + "            <att name=\"long_name\">Instrument</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Salinity</att>\n"
            + // no standard_name or units because String
            // data
            "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Oxygen_row_size</sourceName>\n"
            + "        <destinationName>Oxygen_row_size</destinationName>\n"
            + "        <dataType>short</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">number of Oxygen observations for this cast</att>\n"
            + "            <att name=\"sample_dimension\">Oxygen_obs</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"short\">32767</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Statistics</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Oxygen_WODprofileflag</sourceName>\n"
            + "        <destinationName>Oxygen_WODprofileflag</destinationName>\n"
            + "        <dataType>short</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"flag_definitions\">WODfp</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"short\">32767</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "            <att name=\"long_name\">Oxygen WODprofileflag</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Oxygen_Instrument</sourceName>\n"
            + "        <destinationName>Oxygen_Instrument</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"comment\">Device used for measurement</att>\n"
            + "            <att name=\"long_name\">Instrument</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Dissolved O2</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Oxygen_Original_units</sourceName>\n"
            + "        <destinationName>Oxygen_Original_units</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"comment\">Units originally used: coverted to standard units</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Dissolved O2</att>\n"
            + "            <att name=\"long_name\">Oxygen Original Units</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Pressure_row_size</sourceName>\n"
            + "        <destinationName>Pressure_row_size</destinationName>\n"
            + "        <dataType>short</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">number of Pressure observations for this cast</att>\n"
            + "            <att name=\"sample_dimension\">Pressure_obs</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"short\">32767</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Statistics</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Chlorophyll_row_size</sourceName>\n"
            + "        <destinationName>Chlorophyll_row_size</destinationName>\n"
            + "        <dataType>short</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">number of Chlorophyll observations for this cast</att>\n"
            + "            <att name=\"sample_dimension\">Chlorophyll_obs</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"short\">999</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Statistics</att>\n"
            + "            <att name=\"missing_value\" type=\"short\">32767</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Chlorophyll_WODprofileflag</sourceName>\n"
            + "        <destinationName>Chlorophyll_WODprofileflag</destinationName>\n"
            + "        <dataType>short</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"flag_definitions\">WODfp</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"short\">32767</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "            <att name=\"long_name\">Chlorophyll WODprofileflag</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Chlorophyll_Instrument</sourceName>\n"
            + "        <destinationName>Chlorophyll_Instrument</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"comment\">Device used for measurement</att>\n"
            + "            <att name=\"long_name\">Instrument</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Ocean Color</att>\n"
            + // no standard_name or units because
            // String data
            "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Chlorophyll_uncalibrated</sourceName>\n"
            + "        <destinationName>Chlorophyll_uncalibrated</destinationName>\n"
            + "        <dataType>int</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"comment\">set if measurements have not been calibrated</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"int\">-2147483647</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">30.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.03</att>\n"
            + "            <att name=\"colorBarScale\">Log</att>\n"
            + "            <att name=\"ioos_category\">Ocean Color</att>\n"
            + "            <att name=\"long_name\">Concentration Of Chlorophyll In Sea Water</att>\n"
            + "            <att name=\"missing_value\" type=\"int\">2147483647</att>\n"
            + "            <att name=\"standard_name\">concentration_of_chlorophyll_in_sea_water</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>Conductivit_row_size</sourceName>\n"
            + "        <destinationName>Conductivit_row_size</destinationName>\n"
            + "        <dataType>short</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"long_name\">number of Conductivit observations for this cast</att>\n"
            + "            <att name=\"sample_dimension\">Conductivit_obs</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"short\">999</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Statistics</att>\n"
            + "            <att name=\"missing_value\" type=\"short\">32767</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>crs</sourceName>\n"
            + "        <destinationName>crs</destinationName>\n"
            + "        <dataType>int</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"epsg_code\">EPSG:4326</att>\n"
            + "            <att name=\"grid_mapping_name\">latitude_longitude</att>\n"
            + "            <att name=\"inverse_flattening\" type=\"float\">298.25723</att>\n"
            + "            <att name=\"longitude_of_prime_meridian\" type=\"float\">0.0</att>\n"
            + "            <att name=\"semi_major_axis\" type=\"float\">6378137.0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"int\">-2147483647</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">CRS</att>\n"
            + "            <att name=\"missing_value\" type=\"int\">2147483647</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>WODf</sourceName>\n"
            + "        <destinationName>WODf</destinationName>\n"
            + "        <dataType>short</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"flag_meanings\">accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient</att>\n"
            + "            <att name=\"flag_values\" type=\"shortList\">0 1 2 3 4 5 6 7 8 9</att>\n"
            + "            <att name=\"long_name\">WOD_observation_flag</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"short\">-32767</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "            <att name=\"missing_value\" type=\"short\">32767</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>WODfp</sourceName>\n"
            + "        <destinationName>WODfp</destinationName>\n"
            + "        <dataType>short</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"flag_meanings\">accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out</att>\n"
            + "            <att name=\"flag_values\" type=\"shortList\">0 1 2 3 4 5 6 7 8 9</att>\n"
            + "            <att name=\"long_name\">WOD_profile_flag</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"short\">-32767</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "            <att name=\"missing_value\" type=\"short\">32767</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>WODfd</sourceName>\n"
            + "        <destinationName>WODfd</destinationName>\n"
            + "        <dataType>short</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"flag_meanings\">accepted duplicate_or_inversion density_inversion</att>\n"
            + "            <att name=\"flag_values\" type=\"shortList\">0 1 2</att>\n"
            + "            <att name=\"long_name\">WOD_depth_level_</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"short\">-32767</att>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">2.5</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"missing_value\" type=\"short\">32767</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
    // Test.ensureEqual(results.substring(0, Math.min(results.length(),
    // expected.length())),
    // expected, "");

  }

  /**
   * This tests the methods in this class.
   *
   * @throws Throwable if trouble
   */
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void test1(boolean deleteCachedDatasetInfo) throws Throwable {
    // String2.log("\n****************** EDDTableFromNcCFFiles.test1()
    // *****************\n");
    // testVerboseOn();
    int language = 0;
    String name, tName, results, tResults, expected, userDapQuery, tQuery;
    String error = "";
    EDV edv;
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);

    String id = "testNcCF1b";
    if (deleteCachedDatasetInfo) EDDTableFromNcCFFiles.deleteCachedDatasetInfo(id);

    EDDTable eddTable = (EDDTable) EDDTestDataset.gettestNcCF1b();

    // .csv for one lat,lon,time
    userDapQuery = "";
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_test1a",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "line_station,longitude,latitude,altitude,time,obsScientific,obsValue,obsUnits\n"
            + ",degrees_east,degrees_north,m,UTC,,,\n"
            + "076.7_100,-124.32333,33.388332,-214.1,2004-11-16T21:20:00Z,Argyropelecus sladeni,2,number of larvae\n"
            + "076.7_100,-124.32333,33.388332,-214.1,2004-11-16T21:20:00Z,Chauliodus macouni,3,number of larvae\n"
            + "076.7_100,-124.32333,33.388332,-214.1,2004-11-16T21:20:00Z,Danaphos oculatus,4,number of larvae\n"
            + "076.7_100,-124.32333,33.388332,-214.1,2004-11-16T21:20:00Z,Diogenichthys atlanticus,3,number of larvae\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // .csv only outer vars
    userDapQuery = "line_station";
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_test1b",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "line_station\n"
            + "\n"
            + "076.7_100\n"
            + "080_100\n"
            + "083.3_100\n"; // 4 row with all mv was removed
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
  }

  /**
   * This tests for a bug Kevin O'Brien reported.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  void testKevin20130109() throws Throwable {
    // String2.log("\n****************** EDDTableFromNcCFFiles.testKevin20130109()
    // *****************\n");
    // testVerboseOn();
    int language = 0;
    // boolean oDebugMode = debugMode; debugMode = true;
    // boolean oTableDebug = Table.debugMode; Table.debugMode = true;

    String name, tName, results, tResults, expected, userDapQuery, tQuery;
    String error = "";
    EDV edv;
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);

    String id = "testKevin20130109";
    EDDTable eddTable = (EDDTable) EDDTestDataset.gettestKevin20130109();

    // test time < first time is 2011-02-15T00:00:00Z
    userDapQuery = "traj,obs,time,longitude,latitude,temp,ve,vn&traj<26.5&time<2011-02-15T00:05";
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_test1Kevin20130109a",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "traj,obs,time,longitude,latitude,temp,ve,vn\n"
            + ",,UTC,degrees_east,degrees_north,Deg C,cm/s,cm/s\n"
            + "1.0,1.0,2011-02-15T00:00:00Z,-111.344,-38.71,18.508,-14.618,17.793\n"
            + "22.0,7387.0,2011-02-15T00:00:00Z,91.875,-54.314,3.018,64.135,1.534\n"
            + "26.0,9139.0,2011-02-15T00:00:00Z,168.892,-48.516,11.381,24.49,4.884\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test time > last time is 2011-09-30T18
    userDapQuery = "traj,obs,time,longitude,latitude,temp,ve,vn&traj<6&time>=2011-09-30T17:50";
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_test1Kevin20130109a",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "traj,obs,time,longitude,latitude,temp,ve,vn\n"
            + ",,UTC,degrees_east,degrees_north,Deg C,cm/s,cm/s\n"
            + "1.0,912.0,2011-09-30T18:00:00Z,-91.252,-33.43,15.28,NaN,NaN\n"
            + "2.0,1352.0,2011-09-30T18:00:00Z,145.838,38.44,22.725,NaN,NaN\n"
            + "3.0,1794.0,2011-09-30T18:00:00Z,156.895,39.877,21.517,NaN,NaN\n"
            + "4.0,2233.0,2011-09-30T18:00:00Z,150.312,34.38,26.658,-78.272,41.257\n"
            + "5.0,2676.0,2011-09-30T18:00:00Z,162.9,36.15,26.129,-4.85,15.724\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // debugMode = oDebugMode;
    // Table.debugMode = oTableDebug;

  }

  /**
   * This tests for a bug Kevin O'Brien reported.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  void testKevin20160519() throws Throwable {
    // String2.log("\n****************** EDDTableFromNcCFFiles.testKevin20160519()
    // *****************\n");
    // testVerboseOn();
    int language = 0;
    // boolean oDebugMode = debugMode; debugMode = true;
    // boolean oTableDebug = Table.debugMode; Table.debugMode = true;

    String tName, results, expected, userDapQuery;
    String dir = EDStatic.fullTestCacheDirectory;
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);

    String id = "pmelTaoMonPos";
    EDDTable eddTable = (EDDTable) EDDTestDataset.getpmelTaoMonPos();
    Table table;

    // query with commas is okay
    userDapQuery =
        "array,station,wmo_platform_code,longitude,latitude,time,depth,"
            + "LON_502,QX_5502,LAT_500,QY_5500&time>=2016-01-10&time<=2016-01-20&station=\"0n110w\"";
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            dir,
            eddTable.className() + "_testKevin20160519_1",
            ".ncCF");
    table = new Table();
    table.readNcCF(
        dir + tName,
        null,
        0, // standardizeWhat
        null,
        null,
        null);
    results = table.dataToString();
    expected = // depth/time are unexpected order because of .ncCF file read then flatten
        "array,station,wmo_platform_code,longitude,latitude,depth,time,LON_502,QX_5502,LAT_500,QY_5500\n"
            + "TAO/TRITON,0n110w,32323,250.0,0.0,0.0,1.4529456E9,250.06406,2.0,0.03540476,2.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // percent-encoded query is okay for other file type(s)
    userDapQuery =
        "array%2Cstation%2Cwmo_platform_code%2Clongitude%2Clatitude"
            + "%2Ctime%2Cdepth%2CLON_502%2CQX_5502%2CLAT_500%2CQY_5500"
            + "&time>=2016-01-10&time<=2016-01-20&station=\"0n110w\"";
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            dir,
            eddTable.className() + "_testKevin20160519_2",
            ".nc");
    table = new Table();
    table.readNDNc(
        dir + tName,
        null,
        0, // standardizeWhat
        null,
        0,
        0);
    // expected is same except there's an additional 'row' column, remove it
    table.removeColumn(table.findColumnNumber("row"));
    expected = // then same except for order depth/time
        "array,station,wmo_platform_code,longitude,latitude,time,depth,LON_502,QX_5502,LAT_500,QY_5500\n"
            + "TAO/TRITON,0n110w,32323,250.0,0.0,1.4529456E9,0.0,250.06406,2.0,0.03540476,2.0\n";
    results = table.dataToString();
    Test.ensureEqual(results, expected, "results=\n" + results);

    // percent encoded query for .ncCF fails with error
    // "HTTP Status 500 - Query error: variable=station is listed twice in the
    // results variables list."
    userDapQuery =
        "array%2Cstation%2Cwmo_platform_code%2Clongitude%2Clatitude"
            + "%2Ctime%2Cdepth%2CLON_502%2CQX_5502%2CLAT_500%2CQY_5500"
            + "&time>=2016-01-10&time<=2016-01-20&station=\"0n110w\"";
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            dir,
            eddTable.className() + "_testKevin20160519_3",
            ".ncCF");
    table = new Table();
    table.readNcCF(
        dir + tName,
        null,
        0, // standardizeWhat
        null,
        null,
        null);
    results = table.dataToString();
    expected = // depth/time are unexpected order because of .ncCF file read then flatten
        "array,station,wmo_platform_code,longitude,latitude,depth,time,LON_502,QX_5502,LAT_500,QY_5500\n"
            + "TAO/TRITON,0n110w,32323,250.0,0.0,0.0,1.4529456E9,250.06406,2.0,0.03540476,2.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // debugMode = oDebugMode;
    // Table.debugMode = oTableDebug;
  }

  /**
   * This tests att with no name throws error.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  void testNoAttName() throws Throwable {
    String2.log("\n****************** EDDTableFromNcCFFiles.testNoAttName() *****************\n");

    EDDTable eddTable = null;
    try {
      eddTable = (EDDTable) EDDTestDataset.gettestNoAttName1();
    } catch (Throwable t) {
      String msg = t.toString();
      Test.ensureLinesMatch(
          msg,
          "java.lang.RuntimeException: datasets.xml error on line #\\d{1,7}: An <att> tag doesn't have a \"name\" attribute.",
          "");
    }
    if (EDStatic.useSaxParser) {
      Test.ensureEqual(
          eddTable, null, "Dataset should be null from exception during construction.");
    }

    try {
      eddTable = (EDDTable) EDDTestDataset.gettestNoAttName1();
    } catch (Throwable t) {
      String msg = t.toString();
      Test.ensureLinesMatch(
          msg,
          "java.lang.RuntimeException: datasets.xml error on line #\\d{1,7}: An <att> tag doesn't have a \"name\" attribute.",
          "");
    }
    if (EDStatic.useSaxParser) {
      Test.ensureEqual(
          eddTable, null, "Dataset should be null from exception during construction.");
    }
  }

  /**
   * This tests sos code and global attribute with name=null.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  void testBridger() throws Throwable {
    // String2.log("\n****************** EDDTableFromNcCFFiles.testBridger()
    // *****************\n");
    // testVerboseOn();
    int language = 0;
    String name, tName, results, tResults, expected, userDapQuery, tQuery;
    String error = "";
    EDV edv;
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
    Table table;

    String dataDir =
        Path.of(EDDTableFromNcCFFilesTests.class.getResource("/data/bridger/").toURI()).toString();

    table = new Table();
    table.readNcCF(
        dataDir + "/B01.accelerometer.historical.nc",
        null,
        0, // standardizeWhat
        null,
        null,
        null);
    Test.ensureSomethingUnicode(table.globalAttributes(), "historical global attributes");

    table = new Table();
    table.readNcCF(
        dataDir + "/B01.accelerometer.realtime.nc",
        null,
        0, // standardizeWhat
        null,
        null,
        null);
    Test.ensureSomethingUnicode(table.globalAttributes(), "realtime global attributes");

    String id = "UMaineAccB01";
    EDDTableFromNcCFFiles.deleteCachedDatasetInfo(id);
    EDDTable eddTable = (EDDTable) EDDTestDataset.getUMaineAccB01();

    // .dds
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_bridger",
            ".dds");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    String station;\n"
            + "    Float32 longitude;\n"
            + "    Float32 latitude;\n"
            + "    Float32 depth;\n"
            + "    Float64 time;\n"
            + "    Float64 time_created;\n"
            + "    Float64 time_modified;\n"
            + "    Float32 significant_wave_height;\n"
            + "    Byte significant_wave_height_qc;\n"
            + "    Float32 dominant_wave_period;\n"
            + "    Byte dominant_wave_period_qc;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // .das
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_bridger",
            ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    boolean simplifiedTimeBlocks =
        results.indexOf("Float64 actual_range 56463.676934285555, 56683.652143735904") > -1;
    expected =
        "Attributes \\{\n"
            + " s \\{\n"
            + "  station \\{\n"
            + "    String cf_role \"timeseries_id\";\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"B01\";\n"
            + "    String name \"B01\";\n"
            + "    String short_name \"B01\";\n"
            + "    String standard_name \"station_name\";\n"
            + "  \\}\n"
            + "  longitude \\{\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float32 actual_range -70.42779, -70.42755;\n"
            + "    String axis \"X\";\n"
            + "    Float64 colorBarMaximum 180.0;\n"
            + "    Float64 colorBarMinimum -180.0;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  \\}\n"
            + "  latitude \\{\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float32 actual_range 43.18019, 43.18044;\n"
            + "    String axis \"Y\";\n"
            + "    Float64 colorBarMaximum 90.0;\n"
            + "    Float64 colorBarMinimum -90.0;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  \\}\n"
            + "  depth \\{\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    String _CoordinateZisPositive \"down\";\n"
            + "    Float32 actual_range 0.0, 0.0;\n"
            + "    String axis \"Z\";\n"
            + "    Float64 colorBarMaximum 8000.0;\n"
            + "    Float64 colorBarMinimum -8000.0;\n"
            + "    String colorBarPalette \"TopographyDepth\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Depth\";\n"
            + "    String positive \"down\";\n"
            + "    String standard_name \"depth\";\n"
            + "    String units \"m\";\n"
            + "  \\}\n"
            + "  time \\{\n"
            + (results.indexOf("UInt32 _ChunkSizes 1;\n") > -1 ? "    UInt32 _ChunkSizes 1;\n" : "")
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range 1.0173492e\\+9, 1.3907502e\\+9;\n"
            + "    String axis \"T\";\n"
            + "    String calendar \"gregorian\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  \\}\n"
            + "  time_created \\{\n"
            + (results.indexOf("Float64 actual_range 1.371744887122e") > -1
                ? "    Float64 actual_range 1.371744887122e\\+9, 1.390750745219e\\+9;\n"
                : "")
            + (simplifiedTimeBlocks
                ? "    Float64 actual_range 56463.676934285555, 56683.652143735904;\n"
                : "")
            + (!simplifiedTimeBlocks ? "    String coordinates \"time lon lat depth\";\n" : "")
            + "    String ioos_category \"Time\";\n"
            + (!simplifiedTimeBlocks ? "    String long_name \"Time Record Created\";\n" : "")
            + (!simplifiedTimeBlocks ? "    String short_name \"time_cr\";\n" : "")
            + "    String standard_name \"time\";\n"
            + (!simplifiedTimeBlocks ? "    String time_origin \"01-JAN-1970 00:00:00\";\n" : "")
            + (!simplifiedTimeBlocks
                ? "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
                : "")
            + (!simplifiedTimeBlocks ? "    Float64 valid_range 0.0, 99999.0;\n" : "")
            + "  \\}\n"
            + "  time_modified \\{\n"
            + (results.indexOf("Float64 actual_range 1.371744887122e") > -1
                ? "    Float64 actual_range 1.371744887122e\\+9, 1.390750745219e\\+9;\n"
                : "")
            + (simplifiedTimeBlocks
                ? "    Float64 actual_range 56463.676934285555, 56683.652143735904;\n"
                : "")
            + (!simplifiedTimeBlocks ? "    String coordinates \"time lon lat depth\";\n" : "")
            + "    String ioos_category \"Time\";\n"
            + (!simplifiedTimeBlocks ? "    String long_name \"Time Record Last Modified\";\n" : "")
            + (!simplifiedTimeBlocks ? "    String short_name \"time_mod\";\n" : "")
            + "    String standard_name \"time\";\n"
            + (!simplifiedTimeBlocks ? "    String time_origin \"01-JAN-1970 00:00:00\";\n" : "")
            + (!simplifiedTimeBlocks
                ? "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
                : "")
            + (!simplifiedTimeBlocks ? "    Float64 valid_range 0.0, 99999.0;\n" : "")
            + "  \\}\n"
            + "  significant_wave_height \\{\n"
            + (results.indexOf("UInt32 _ChunkSizes 1;\n") > -1 ? "    UInt32 _ChunkSizes 1;\n" : "")
            + "    Float32 _FillValue -999.0;\n"
            + (results.indexOf("Float64 accuracy 0.5;") > -1 ? "    Float64 accuracy 0.5;\n" : "")
            + "    Float32 actual_range 0.009102137, 9.613417;\n"
            + "    String ancillary_variables \"significant_wave_height_qc\";\n"
            + "    Float64 colorBarMaximum 10.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String coordinates \"time lon lat depth\";\n"
            + "    Int32 epic_code 4061;\n"
            + "    String ioos_category \"Surface Waves\";\n"
            + "    Float64 is_dead 0.0;\n"
            + "    String long_name \"Significant Wave Height\";\n"
            + "    String measurement_type \"Computed\";\n"
            + "    Float64 precision 0.1;\n"
            + "    String short_name \"SWH\";\n"
            + "    String standard_name \"significant_height_of_wind_and_swell_waves\";\n"
            + "    String units \"m\";\n"
            + "    Float32 valid_range 0.0, 10.0;\n"
            + "  \\}\n"
            + "  significant_wave_height_qc \\{\n"
            + (results.indexOf("UInt32 _ChunkSizes 1;\n") > -1 ? "    UInt32 _ChunkSizes 1;\n" : "")
            + "    Byte _FillValue -128;\n"
            + "    String _Unsigned \"false\";\n"
            + "    Byte actual_range 0, 99;\n"
            + "    Float64 colorBarMaximum 128.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String coordinates \"time lon lat depth\";\n"
            + "    String flag_meanings \"quality_good out_of_range sensor_nonfunctional algorithm_failure_no_infl_pt\";\n"
            + "    Byte flag_values 0, 1, 2, 3;\n"
            + "    String intent \"data_quality\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Significant Wave Height Quality Control\";\n"
            + "    String short_name \"SWHQC\";\n"
            + "    String standard_name \"significant_height_of_wind_and_swell_waves data_quality\";\n"
            + "    String units \"1\";\n"
            + "    Byte valid_range -127, 127;\n"
            + "  \\}\n"
            + "  dominant_wave_period \\{\n"
            + (results.indexOf("UInt32 _ChunkSizes 1;\n") > -1 ? "    UInt32 _ChunkSizes 1;\n" : "")
            + "    Float32 _FillValue -999.0;\n"
            + "    Float64 accuracy 2.0;\n"
            + "    Float32 actual_range 1.032258, 16.0;\n"
            + "    String ancillary_variables \"dominant_wave_period_qc\";\n"
            + "    Float64 colorBarMaximum 40.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String coordinates \"time lon lat depth\";\n"
            + "    Int32 epic_code 4063;\n"
            + "    String ioos_category \"Surface Waves\";\n"
            + "    Float64 is_dead 0.0;\n"
            + "    String long_name \"Dominant Wave Period\";\n"
            + "    String measurement_type \"Computed\";\n"
            + "    Float64 precision 1.0;\n"
            + "    Float64 sensor_depth 0.0;\n"
            + "    String short_name \"DWP\";\n"
            + "    String standard_name \"period\";\n"
            + "    String units \"s\";\n"
            + "    Float32 valid_range 0.0, 32.0;\n"
            + "  \\}\n"
            + "  dominant_wave_period_qc \\{\n"
            + (results.indexOf("UInt32 _ChunkSizes 1;\n") > -1 ? "    UInt32 _ChunkSizes 1;\n" : "")
            + "    Byte _FillValue -128;\n"
            + "    String _Unsigned \"false\";\n"
            + "    Byte actual_range 0, 99;\n"
            + "    Float64 colorBarMaximum 128.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String coordinates \"time lon lat depth\";\n"
            + "    String flag_meanings \"quality_good out_of_range sensor_nonfunctional algorithm_failure_no_infl_pt\";\n"
            + "    Byte flag_values 0, 1, 2, 3;\n"
            + "    String intent \"data_quality\";\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Dominant Wave Period Quality\";\n"
            + "    String short_name \"DWPQ\";\n"
            + "    String standard_name \"period data_quality\";\n"
            + "    String units \"1\";\n"
            + "    Byte valid_range -127, 127;\n"
            + "  \\}\n"
            + " \\}\n"
            + "  NC_GLOBAL \\{\n"
            + "    String accelerometer_serial_number \"SUMAC0902A01107\";\n"
            + "    String algorithm_ids \"Waves_SWH_DWP_1.12:  12-Jun-2013 15:15:53\";\n"
            + "    Float64 averaging_period 17.07;\n"
            + "    String averaging_period_units \"Minutes\";\n"
            + "    Int32 breakout_id 7;\n"
            + "    String buffer_type \"accelerometer\";\n"
            + "    String cdm_data_type \"TimeSeries\";\n"
            + "    String cdm_timeseries_variables \"station\";\n"
            + "    String clock_time \"Center of period\";\n"
            + "    String contact \"nealp@maine.edu,ljm@umeoce.maine.edu,bfleming@umeoce.maine.edu\";\n"
            + "    String control_box_serial_number \"UMECB124\";\n"
            + "    String Conventions \"CF-1.6, COARDS, ACDD-1.3\";\n"
            + "    String creator_email \"nealp@maine.edu,ljm@umeoce.maine.edu,bfleming@umeoce.maine.edu\";\n"
            + "    String creator_name \"Neal Pettigrew\";\n"
            + "    String creator_url \"http://gyre.umeoce.maine.edu\";\n"
            + (EDStatic.useSaxParser ? "    Int32 delta_t 30;\n" : "")
            + "    String depth_datum \"Sea Level\";\n"
            + "    Float64 Easternmost_Easting -70.42755;\n"
            + (EDStatic.useSaxParser
                ? "    Float64 ending_julian_day_number 56683.64583333349;\n"
                    + "    String ending_julian_day_string \"2014-01-26 15:30:00\";\n"
                : "")
            + "    String featureType \"TimeSeries\";\n"
            + "    Float64 geospatial_lat_max 43.18044;\n"
            + "    Float64 geospatial_lat_min 43.18019;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max -70.42755;\n"
            + "    Float64 geospatial_lon_min -70.42779;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    Float64 geospatial_vertical_max 0.0;\n"
            + "    Float64 geospatial_vertical_min 0.0;\n"
            + "    String geospatial_vertical_positive \"down\";\n"
            + "    String geospatial_vertical_units \"m\";\n"
            + "    String goes_platform_id \"044250DC\";\n"
            + "    String history \"2014-01-03 11:20:56:  Parameter dominant_wave_period marked as non-functional as of julian day 56660.395833 \\(2014-01-03 09:30:00\\)\n"
            + "2014-01-03 11:20:46:  Parameter significant_wave_height marked as non-functional as of julian day 56660.395833 \\(2014-01-03 09:30:00\\)\n"
            + "2013-06-25 11:57:07:  Modified \\[lon,lat\\] to \\[-70.427787,43.180192\\].\n"
            + "Thu Jun 20 16:50:01 2013: /usr/local/bin/ncrcat -d time,56463.65625,56464.00 B0125.accelerometer.realtime.nc B0125.accelerometer.realtime.nc.new\n"
            + "\n"
            + today
            + "T.{8}Z \\(local files\\)\n"
            + today
            + "T.{8}Z http://localhost:8080/erddap/tabledap/UMaineAccB01.das\";\n"
            + "    String id \"B01\";\n"
            + "    String infoUrl \"http://gyre.umeoce.maine.edu/\";\n"
            + "    String institution \"Department of Physical Oceanography, School of Marine Sciences, University of Maine\";\n"
            + "    String institution_url \"http://gyre.umeoce.maine.edu\";\n"
            + "    Int32 instrument_number 0;\n"
            + (EDStatic.useSaxParser
                ? "    String julian_day_convention \"Julian date convention begins at 00:00:00 UTC on 17 November 1858 AD\";\n"
                : "")
            + "    String keywords \"accelerometer, b01, buoy, chemistry, chlorophyll, circulation, conductivity, control, currents, data, density, department, depth, dominant, dominant_wave_period data_quality, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, Earth Science > Oceans > Ocean Chemistry > Oxygen, Earth Science > Oceans > Ocean Circulation > Ocean Currents, Earth Science > Oceans > Ocean Optics > Turbidity, Earth Science > Oceans > Ocean Pressure > Sea Level Pressure, Earth Science > Oceans > Ocean Temperature > Water Temperature, Earth Science > Oceans > Ocean Waves > Significant Wave Height, Earth Science > Oceans > Ocean Waves > Swells, Earth Science > Oceans > Ocean Waves > Wave Period, Earth Science > Oceans > Ocean Winds > Surface Winds, Earth Science > Oceans > Salinity/Density > Conductivity, Earth Science > Oceans > Salinity/Density > Density, Earth Science > Oceans > Salinity/Density > Salinity, height, level, maine, marine, name, o2, ocean, oceanography, oceans, optics, oxygen, period, physical, pressure, quality, salinity, school, sciences, sea, seawater, sensor, significant, significant_height_of_wind_and_swell_waves, significant_wave_height data_quality, station, station_name, surface, surface waves, swell, swells, temperature, time, turbidity, university, water, wave, waves, wind, winds\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    Float64 latitude 43.18019230109601;\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    String long_name \"B01\";\n"
            + "    Float64 longitude -70.42778651970477;\n"
            + "    Float64 magnetic_variation -16.3;\n"
            + "    String mooring_site_desc \"Western Maine Shelf\";\n"
            + "    String mooring_site_id \"B0125\";\n"
            + "    String mooring_type \"Slack\";\n"
            + "    String naming_authority \"edu.maine\";\n"
            + "    Int32 nco_openmp_thread_number 1;\n"
            + "    String ndbc_site_id \"44030\";\n"
            + "    Float64 Northernmost_Northing 43.18044;\n"
            + "    Int32 number_observations_per_hour 2;\n"
            + "    Int32 number_samples_per_observation 2048;\n"
            + "    String position_datum \"WGS 84\";\n"
            + "    String processing \"realtime\";\n"
            + "    String project \"NERACOOS\";\n"
            + "    String project_url \"http://www.neracoos.org\";\n"
            + "    String publisher \"Department of Physical Oceanography, School of Marine Sciences, University of Maine\";\n"
            + "    String publisher_email \"info@neracoos.org\";\n"
            + "    String publisher_name \"Northeastern Regional Association of Coastal and Ocean Observing Systems \\(NERACOOS\\)\";\n"
            + "    String publisher_phone \"\\(603\\) 319 1785\";\n"
            + "    String publisher_url \"http://www.neracoos.org/\";\n"
            + "    String references \"http://gyre.umeoce.maine.edu/data/gomoos/buoy/doc/buoy_system_doc/buoy_system/book1.html\";\n"
            + "    String short_name \"B01\";\n"
            + "    String source \"Ocean Data Acquisition Systems \\(ODAS\\) Buoy\";\n"
            + "    String sourceUrl \"\\(local files\\)\";\n"
            + "    Float64 Southernmost_Northing 43.18019;\n"
            + "    String standard_name_vocabulary \"CF-1.6\";\n"
            + (EDStatic.useSaxParser
                ? "    Float64 starting_julian_day_number 56463.66666666651;\n"
                    + "    String starting_julian_day_string \"2013-06-20 16:00:00\";\n"
                : "")
            + "    String station_name \"B01\";\n"
            + "    String station_photo \"http://gyre.umeoce.maine.edu/gomoos/images/generic_buoy.png\";\n"
            + "    String station_type \"Surface Mooring\";\n"
            + "    String subsetVariables \"station\";\n"
            + "    String summary \"Ocean observation data from the Northeastern Regional Association of Coastal &amp; Ocean Observing Systems \\(NERACOOS\\). The NERACOOS region includes the northeast United States and Canadian Maritime provinces, as part of the United States Integrated Ocean Observing System \\(IOOS\\).  These data are served by Unidata's Thematic Realtime Environmental Distributed Data Services \\(THREDDS\\) Data Server \\(TDS\\) in a variety of interoperable data services and output formats.\";\n"
            + "    String time_coverage_end \"2014-01-26T15:30:00Z\";\n"
            + "    String time_coverage_start \"2002-03-28T21:00:00Z\";\n"
            + "    String time_zone \"UTC\";\n"
            + "    String title \"University of Maine, B01 Accelerometer Buoy Sensor\";\n"
            + "    String uscg_light_list_letter \"B\";\n"
            + "    String uscg_light_list_number \"113\";\n"
            + "    Int32 watch_circle_radius 45;\n"
            + "    Float64 water_depth 62.0;\n"
            + "    Float64 Westernmost_Easting -70.42779;\n"
            + "  \\}\n"
            + "\\}\n";
    Test.ensureLinesMatch(results, expected, "results=\n" + results);

    // .csv for start time time
    // " String time_coverage_start \"2002-03-28T21:00:00Z\";\n" +
    userDapQuery = "&time<=2002-03-28T22:00:00Z";
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_bridger1",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "station,longitude,latitude,depth,time,time_created,time_modified,significant_wave_height,significant_wave_height_qc,dominant_wave_period,dominant_wave_period_qc\n"
            + ",degrees_east,degrees_north,m,UTC,UTC,UTC,m,1,s,1\n"
            + "B01,-70.42755,43.18044,0.0,2002-03-28T21:00:00Z,,,2.605597,0,10.66667,0\n"
            + "B01,-70.42755,43.18044,0.0,2002-03-28T22:00:00Z,,,1.720958,0,10.66667,0\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // .csv for end time
    // " String time_coverage_end \"2014-01-26T15:30:00Z\";\n" +
    userDapQuery = "&time>=2014-01-26T15:00:00Z";
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_bridger2",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "station,longitude,latitude,depth,time,time_created,time_modified,significant_wave_height,significant_wave_height_qc,dominant_wave_period,dominant_wave_period_qc\n"
            + ",degrees_east,degrees_north,m,UTC,UTC,UTC,m,1,s,1\n"
            + "B01,-70.42779,43.18019,0.0,2014-01-26T15:00:00Z,2014-01-26T15:12:04Z,2014-01-26T15:12:04Z,1.3848689,0,4.0,0\n"
            + "B01,-70.42779,43.18019,0.0,2014-01-26T15:30:00Z,2014-01-26T15:39:05Z,2014-01-26T15:39:05Z,1.3212088,0,4.0,0\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // .csv only outer vars
    userDapQuery = "station&distinct()";
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_bridger3",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected = "station\n" + "\n" + "B01\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    String2.log("\n*** EDDTableFromNcCFFiles.testBridger() finished.");
  }

  /**
   * This tests a Profile Contiguous Ragged Array with 7 sample_dimension variables. !!!Because the
   * file has featureType=Profile, to be a cdm_data_type=Profile in ERDDAP, it must have an
   * altitude/depth variable. That's fine if reading the z-obs variables. But for others (e.g.,
   * temperature_obs) I change to cdm_data_type=TimeSeries in datasets.xml. Also, for
   * wod_unique_cast, I changed cf_role=profile_id to cf_role-timeseries_id in datasets.xml.
   *
   * <p>!!!This tests that ERDDAP can read the variables associated with any one of the
   * sample_dimensions (including the non zobs_dimension, here temperature_obs) and convert the
   * cdm_data_type=Profile into TimeSeries (since no altitude/depth) and make a dataset from it.
   *
   * @throws Throwable if trouble
   */
  void test7SampleDimensions() throws Throwable {
    // String2.log("\n******************
    // EDDTableFromNcCFFiles.test7SampleDimensions() *****************\n");
    // testVerboseOn();
    int language = 0;
    String name, tName, results, tResults, expected, userDapQuery, tQuery;
    String error = "";
    EDV edv;
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
    Table table;
    String testCacheDir = EDStatic.fullTestCacheDirectory;
    String scalarVars = ",crs,WODf,WODfd";

    // From Ajay Krishnan, NCEI/NODC, from
    // https://data.nodc.noaa.gov/thredds/catalog/testdata/wod_ragged/05052016/catalog.html?dataset=testdata/wod_ragged/05052016/ind199105_ctd.nc
    // See low level reading test: Table.testReadNcCF7SampleDims()
    String fileName =
        Path.of(
                EDDTableFromNcCFFilesTests.class
                    .getResource("/data/nccf/ncei/ind199105_ctd.nc")
                    .toURI())
            .toString();
    // String2.log(NcHelper.ncdump(fileName, "-h"));

    String id = "testNcCF7SampleDimensions";
    EDDTableFromNcCFFiles.deleteCachedDatasetInfo(id);
    EDDTable eddTable = (EDDTable) EDDTestDataset.gettestNcCF7SampleDimensions();

    // .dds
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            testCacheDir,
            eddTable.className() + "_7SampleDimensions",
            ".dds");
    results = File2.directReadFrom88591File(testCacheDir + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    Int32 wod_unique_cast;\n"
            + "    Float32 latitude;\n"
            + "    Float32 longitude;\n"
            + "    Float64 time;\n"
            + "    Int32 Access_no;\n"
            + "    String Project;\n"
            + "    String Platform;\n"
            + "    String Institute;\n"
            + "    Int32 Cast_Tow_number;\n"
            + "    Int16 Temperature_WODprofileFlag;\n"
            + "    String Temperature_Scale;\n"
            + "    String Temperature_instrument;\n"
            + "    Float32 Temperature;\n"
            + "    Int16 Temperature_sigfigs;\n"
            + "    Int16 Temperature_WODflag;\n"
            + "    Int16 Temperature_origflag;\n"
            + "    Int32 crs;\n"
            + "    Int16 WODf;\n"
            + "    Int16 WODfp;\n"
            + "    Int16 WODfd;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // .das
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            testCacheDir,
            eddTable.className() + "_7SampleDimensions",
            ".das");
    results = File2.directReadFrom88591File(testCacheDir + tName);
    // String2.log(results);
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  wod_unique_cast {\n"
            + "    Int32 actual_range 3390296, 10587111;\n"
            + "    String cf_role \"timeseries_id\";\n"
            + "    String ioos_category \"Other\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range 6.73082939999e+8, 6.75733075002e+8;\n"
            + "    String axis \"T\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  Access_no {\n"
            + "    Int32 actual_range 841, 9700263;\n"
            + "    String comment \"used to find original data at NODC\";\n"
            + "    String ioos_category \"Other\";\n"
            + "    String long_name \"NODC_accession_number\";\n"
            + "    String units_wod \"NODC_code\";\n"
            + "  }\n"
            + "  Project {\n"
            + "    String comment \"name or acronym of project under which data were measured\";\n"
            + "    String ioos_category \"Other\";\n"
            + "    String long_name \"Project_name\";\n"
            + "  }\n"
            + "  Platform {\n"
            + "    String comment \"name of platform from which measurements were taken\";\n"
            + "    String ioos_category \"Other\";\n"
            + "    String long_name \"Platform_name\";\n"
            + "  }\n"
            + "  Institute {\n"
            + "    String comment \"name of institute which collected data\";\n"
            + "    String ioos_category \"Other\";\n"
            + "    String long_name \"Responsible_institute\";\n"
            + "  }\n"
            + "  Cast_Tow_number {\n"
            + "    Int32 actual_range -2147483647, 1;\n"
            + "    String comment \"originator assigned sequential cast or tow_no\";\n"
            + "    String ioos_category \"Other\";\n"
            + "    String long_name \"Cast_or_Tow_number\";\n"
            + "  }\n"
            + "  Temperature_WODprofileFlag {\n"
            + "    String ioos_category \"Other\";\n"
            + "  }\n"
            + "  Temperature_Scale {\n"
            + "    String ioos_category \"Other\";\n"
            + "    String long_name \"Scale upon which values were measured\";\n"
            + "  }\n"
            + "  Temperature_instrument {\n"
            + "    String ioos_category \"Other\";\n"
            + "  }\n"
            + "  Temperature {\n"
            + "    Float32 actual_range 0.425, 31.042;\n"
            + "    String coordinates \"time lat lon z\";\n"
            + "    String grid_mapping \"crs\";\n"
            + "    String ioos_category \"Other\";\n"
            + "    String long_name \"Temperature\";\n"
            + "    String standard_name \"sea_water_temperature\";\n"
            + "    String units \"degree_C\";\n"
            + "  }\n"
            + "  Temperature_sigfigs {\n"
            + "    Int16 actual_range 4, 6;\n"
            + "    String ioos_category \"Other\";\n"
            + "  }\n"
            + "  Temperature_WODflag {\n"
            + "    Int16 actual_range 0, 3;\n"
            + "    String flag_definitions \"WODf\";\n"
            + "    String ioos_category \"Other\";\n"
            + "  }\n"
            + "  Temperature_origflag {\n"
            + "    Int16 actual_range -32767, -32767;\n"
            + "    String flag_definitions \"Oflag\";\n"
            + "    String ioos_category \"Other\";\n"
            + "  }\n"
            + "  crs {\n"
            + "    Int32 actual_range -2147483647, -2147483647;\n"
            + "    String epsg_code \"EPSG:4326\";\n"
            + "    String grid_mapping_name \"latitude_longitude\";\n"
            + "    Float32 inverse_flattening 298.25723;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"CRS\";\n"
            + "    Float32 longitude_of_prime_meridian 0.0;\n"
            + "    Float32 semi_major_axis 6378137.0;\n"
            + "  }\n"
            + "  WODf {\n"
            + "    Int16 actual_range -32767, -32767;\n"
            + "    Float64 colorBarMaximum 10.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String flag_meanings \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\";\n"
            + "    Int16 flag_values 0, 1, 2, 3, 4, 5, 6, 7, 8, 9;\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"WOD_observation_flag\";\n"
            + "  }\n"
            + "  WODfp {\n"
            + "    Int16 actual_range -32767, -32767;\n"
            + "    Float64 colorBarMaximum 10.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String flag_meanings \"accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out\";\n"
            + "    Int16 flag_values 0, 1, 2, 3, 4, 5, 6, 7, 8, 9;\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"WOD_profile_flag\";\n"
            + "  }\n"
            + "  WODfd {\n"
            + "    Int16 actual_range -32767, -32767;\n"
            + "    Float64 colorBarMaximum 2.5;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String flag_meanings \"accepted duplicate_or_inversion density_inversion\";\n"
            + "    Int16 flag_values 0, 1, 2;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"WOD_depth_level_\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_data_type \"TimeSeries\";\n"
            + "    String cdm_timeseries_variables \"wod_unique_cast,latitude,longitude,time,Access_no,Project,Platform,Institute,Cast_Tow_number,Temperature_WODprofileFlag,Temperature_Scale,Temperature_instrument\";\n"
            + "    String Conventions \"CF-1.6, ACDD-1.3, COARDS\";\n"
            + "    String creator_email \"OCLhelp@noaa.gov\";\n"
            + "    String creator_name \"Ocean Climate Lab/NODC\";\n"
            + "    String creator_url \"https://www.nodc.noaa.gov\";\n"
            + "    String date_created \"2016-05-02\";\n"
            + "    String date_modified \"2016-05-02\";\n"
            + "    String featureType \"TimeSeries\";\n"
            + "    String geospatial_lat_resolution \"point\";\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    String geospatial_lon_resolution \"point\";\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    String geospatial_vertical_positive \"down\";\n"
            + "    String geospatial_vertical_units \"meters\";\n"
            + "    String grid_mapping_epsg_code \"EPSG:4326\";\n"
            + "    Float32 grid_mapping_inverse_flattening 298.25723;\n"
            + "    Float32 grid_mapping_longitude_of_prime_meridian 0.0;\n"
            + "    String grid_mapping_name \"latitude_longitude\";\n"
            + "    Float32 grid_mapping_semi_major_axis 6378137.0;\n"
            + "    String history";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // "2016-06-10T18:38:03Z (local files)
    // 2016-06-10T18:38:03Z
    // http://localhost:8080/cwexperimental/tabledap/testNcCF7SampleDimensions.das";
    expected =
        "String id \"ind199105_ctd.nc\";\n"
            + "    String infoUrl \"https://www.nodc.noaa.gov/OC5/WOD/pr_wod.html\";\n"
            + "    String institution \"National Oceanographic Data Center(NODC), NOAA\";\n"
            + "    String keywords \"temperature\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    String naming_authority \"gov.noaa.nodc\";\n"
            + "    String project \"World Ocean Database\";\n"
            + "    String publisher_email \"NODC.Services@noaa.gov\";\n"
            + "    String publisher_name \"US DOC; NESDIS; NATIONAL OCEANOGRAPHIC DATA CENTER - IN295\";\n"
            + "    String publisher_url \"https://www.nodc.noaa.gov\";\n"
            + "    String references \"World Ocean Database 2013. URL:https://data.nodc.noaa.gov/woa/WOD/DOC/wod_intro.pdf\";\n"
            + "    String source \"World Ocean Database\";\n"
            + "    String sourceUrl \"(local files)\";\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String subsetVariables \"wod_unique_cast,latitude,longitude,time,Access_no,Project,Platform,Institute,Cast_Tow_number,Temperature_WODprofileFlag,Temperature_Scale,Temperature_instrument\";\n"
            + "    String summary \"Test WOD .ncCF file\";\n"
            + "    String time_coverage_end \"1991-05-31T23:37:55Z\";\n"
            + "    String time_coverage_start \"1991-05-01T07:28:59Z\";\n"
            + "    String title \"Test WOD .ncCF file\";\n"
            + "  }\n"
            + "}\n";
    int po = Math.max(0, results.indexOf(expected.substring(0, 20)));
    Test.ensureEqual(results.substring(po), expected, "results=\n" + results);

    // .csv all vars
    userDapQuery = "&time=1991-05-02T02:08:00Z";
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            testCacheDir,
            eddTable.className() + "_7SampleDimensions_all",
            ".csv");
    results = File2.directReadFrom88591File(testCacheDir + tName);
    // String2.log(results);
    expected =
        "wod_unique_cast,latitude,longitude,time,Access_no,Project,Platform,Institute,"
            + "Cast_Tow_number,Temperature_WODprofileFlag,Temperature_Scale,Temperature_instrument,"
            + "Temperature,Temperature_sigfigs,Temperature_WODflag,Temperature_origflag,crs,WODf,WODfp,WODfd\n"
            + ",degrees_north,degrees_east,UTC,,,,,,,,,degree_C,,,,,,,\n"
            + "3390310,NaN,NaN,1991-05-02T02:08:00Z,841,WORLD OCEAN CIRCULATION EXPERIMENT (WOCE),"
            + "MARION DUFRESNE (C.s.FNGB;built 1972;decomm-d 1995;renamed Fres;IMO7208388),"
            + "NATIONAL MUSEUM OF NATURAL HISTORY (PARIS),1,NaN,,,7.738,5,0,-32767,-2147483647,-32767,-32767,-32767\n"
            + "3390310,NaN,NaN,1991-05-02T02:08:00Z,841,WORLD OCEAN CIRCULATION EXPERIMENT (WOCE),"
            + "MARION DUFRESNE (C.s.FNGB;built 1972;decomm-d 1995;renamed Fres;IMO7208388),"
            + "NATIONAL MUSEUM OF NATURAL HISTORY (PARIS),1,NaN,,,7.74,5,0,-32767,-2147483647,-32767,-32767,-32767\n"
            + "3390310,NaN,NaN,1991-05-02T02:08:00Z,841,WORLD OCEAN CIRCULATION EXPERIMENT (WOCE),"
            + "MARION DUFRESNE (C.s.FNGB;built 1972;decomm-d 1995;renamed Fres;IMO7208388),"
            + "NATIONAL MUSEUM OF NATURAL HISTORY (PARIS),1,NaN,,,7.713,5,0,-32767,-2147483647,-32767,-32767,-32767\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // .csv outer and inner vars
    userDapQuery =
        "wod_unique_cast,latitude,longitude,time,Temperature"
            + scalarVars
            + "&time=1991-05-02T02:08:00Z";
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            testCacheDir,
            eddTable.className() + "_7SampleDimensions_outerInner",
            ".csv");
    results = File2.directReadFrom88591File(testCacheDir + tName);
    // String2.log(results);
    expected =
        "wod_unique_cast,latitude,longitude,time,Temperature,crs,WODf,WODfd\n"
            + ",degrees_north,degrees_east,UTC,degree_C,,,\n"
            + "3390310,NaN,NaN,1991-05-02T02:08:00Z,7.738,-2147483647,-32767,-32767\n"
            + "3390310,NaN,NaN,1991-05-02T02:08:00Z,7.74,-2147483647,-32767,-32767\n"
            + "3390310,NaN,NaN,1991-05-02T02:08:00Z,7.713,-2147483647,-32767,-32767\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // .csv outer vars only
    userDapQuery =
        "wod_unique_cast,latitude,longitude,time" + scalarVars + "&time=1991-05-02T02:08:00Z";
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            testCacheDir,
            eddTable.className() + "_7SampleDimensions_outer",
            ".csv");
    results = File2.directReadFrom88591File(testCacheDir + tName);
    // String2.log(results);
    expected =
        "wod_unique_cast,latitude,longitude,time,crs,WODf,WODfd\n"
            + ",degrees_north,degrees_east,UTC,,,\n"
            + "3390310,NaN,NaN,1991-05-02T02:08:00Z,-2147483647,-32767,-32767\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // .csv scalar vars only
    userDapQuery = "crs,WODf,WODfd" + "&time=1991-05-02T02:08:00Z";
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            testCacheDir,
            eddTable.className() + "_7SampleDimensions_scalar",
            ".csv");
    results = File2.directReadFrom88591File(testCacheDir + tName);
    // String2.log(results);
    expected = "crs,WODf,WODfd\n" + ",,\n" + "-2147483647,-32767,-32767\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // .csv inner vars vars only
    userDapQuery = "Temperature" + "&time=1991-05-02T02:08:00Z";
    tName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            testCacheDir,
            eddTable.className() + "_7SampleDimensions_inner",
            ".csv");
    results = File2.directReadFrom88591File(testCacheDir + tName);
    // String2.log(results);
    expected = "Temperature\n" + "degree_C\n" + "7.738\n" + "7.74\n" + "7.713\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

    String2.log("\n*** EDDTableFromNcCFFiles.test7SampleDimensions() finished.");
  }

  /**
   * This tests sos code and global attribute with name=null.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagMissingFile
  void testNcml() throws Throwable {
    // String2.log("\n****************** EDDTableFromNcCFFiles.testNcml()
    // *****************\n");
    // testVerboseOn();
    String baseName = // don't make this public via GitHub
        "/data/medrano/CTZ-T500-MCT-NS5649-Z408-INS12-REC14";
    String results, expected;
    Table table;

    // ncdump the .nc file
    String2.log("Here's the ncdump of " + baseName + ".nc:");
    results = NcHelper.ncdump(baseName + ".nc", "-h");
    expected =
        "netcdf CTZ-T500-MCT-NS5649-Z408-INS12-REC14.nc {\n"
            + "  dimensions:\n"
            + "    time = UNLIMITED;   // (101 currently)\n"
            + "    one = 1;\n"
            + "    ni_Srec = 93;\n"
            + "    lat = 1;\n"
            + "    lon = 1;\n"
            + "  variables:\n"
            + "    double Cond(time=101);\n"
            + "      :long_name = \"Conductividad\";\n"
            + "      :units = \"S/m\";\n"
            + "\n"
            + "    double Pres(time=101);\n"
            + "      :long_name = \"Presion\";\n"
            + "      :units = \"dBar\";\n"
            + "\n"
            + "    double ProfDiseno(one=1);\n"
            + "      :long_name = \"Profundidad de diseno\";\n"
            + "      :units = \"m\";\n"
            + "\n"
            + "    double ProfEstimada(one=1);\n"
            + "      :long_name = \"Profundidad estimada\";\n"
            + "      :units = \"m\";\n"
            + "\n"
            + "    double Sal(time=101);\n"
            + "      :long_name = \"Salinidad\";\n"
            + "      :units = \"PSU\";\n"
            + "\n"
            + "    double Temp(time=101);\n"
            + "      :long_name = \"Temperatura\";\n"
            + "      :units = \"\uFFFDC\";\n"
            + // 65533 which is "unknown character". Not right!!!???
            "\n"
            + "    double TiranteDiseno(one=1);\n"
            + "      :long_name = \"Tirante diseno\";\n"
            + "      :units = \"m\";\n"
            + "\n"
            + "    double TiranteEstimado(one=1);\n"
            + "      :long_name = \"Tirante estimado\";\n"
            + "      :units = \"m\";\n"
            + "\n"
            + "    double i_Salrec(ni_Srec=93);\n"
            + "      :long_name = \"Indices salinidad reconstruida\";\n"
            + "      :units = \"N/A\";\n"
            + "\n"
            + "    double jd(time=101);\n"
            + "      :long_name = \"tiempo en dias Julianos\";\n"
            + "      :units = \"days since 0000-01-01 00:00:00 \";\n"
            + "      :time_origin = \"0000-01-01 00:00:00\";\n"
            + "\n"
            + "    double lat(lat=1);\n"
            + "      :long_name = \"Latitud\";\n"
            + "      :Units = \"degrees_north\";\n"
            + "\n"
            + "    double lon(lon=1);\n"
            + "      :long_name = \"Longitud\";\n"
            + "      :units = \"degrees_east\";\n"
            + "\n"
            + "    double var_pres(one=1);\n"
            + "      :long_name = \"Bandera presion\";\n"
            + "      :units = \"N/A\";\n"
            + "\n"
            + "  // global attributes:\n"
            + "  :Title = \"Datos MCT para  el anclaje CTZ-T500 crucero CANEK 14\";\n"
            + "  :Anclaje = \"CTZ-T500\";\n"
            + "  :Equipo = \"MCT\";\n"
            + "  :Numero_de_serie = \"5649\";\n"
            + "  :Source_file = \"CTZ-T500-MCT-NS5649-Z408-INS12-REC14.mat\";\n"
            + "  :Final_NC_file = \"CTZ-T500-MCT-NS5649-Z408-INS12-REC14.nc\";\n"
            + "  :Creation_date = \"06-Aug-2014 12:22:59\";\n"
            + "  :NCO = \"\\\"4.5.2\\\"\";\n"
            + "}\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // ncdump the .ncml file
    String2.log("\nHere's the ncdump of " + baseName + ".ncml:");
    results = NcHelper.ncdump(baseName + ".ncml", "-h");
    expected =
        "netcdf CTZ-T500-MCT-NS5649-Z408-INS12-REC14.ncml {\n"
            + "  dimensions:\n"
            + "    time = 101;\n"
            + "    station = 1;\n"
            + "  variables:\n"
            + "    double Cond(station=1, time=101);\n"
            + "      :long_name = \"Conductividad\";\n"
            + "      :units = \"S/m\";\n"
            + "      :standard_name = \"sea_water_electrical_conductivity\";\n"
            + "      :coordinates = \"time latitude longitude z\";\n"
            + "\n"
            + "    double Pres(station=1, time=101);\n"
            + "      :long_name = \"Presion\";\n"
            + "      :units = \"dBar\";\n"
            + "      :standard_name = \"sea_water_pressure\";\n"
            + "      :coordinates = \"time latitude longitude z\";\n"
            + "\n"
            + "    double Temp(station=1, time=101);\n"
            + "      :long_name = \"Temperatura\";\n"
            + "      :units = \"degree_celsius\";\n"
            + "      :standard_name = \"sea_water_temperature\";\n"
            + "      :coordinates = \"time latitude longitude z\";\n"
            + "\n"
            + "    double Sal(station=1, time=101);\n"
            + "      :long_name = \"Salinidad\";\n"
            + "      :units = \"PSU\";\n"
            + "      :standard_name = \"sea_water_salinity\";\n"
            + "      :coordinates = \"time latitude longitude z\";\n"
            + "\n"
            + "    double ProfDiseno(station=1);\n"
            + "      :long_name = \"Profundidad de diseno\";\n"
            + "      :units = \"m\";\n"
            + "\n"
            + "    double TiranteDiseno(station=1);\n"
            + "      :long_name = \"Tirante diseno\";\n"
            + "      :units = \"m\";\n"
            + "\n"
            + "    double TiranteEstimado(station=1);\n"
            + "      :long_name = \"Tirante estimado\";\n"
            + "      :units = \"m\";\n"
            + "\n"
            + "    double var_pres(station=1);\n"
            + "      :long_name = \"Bandera presion\";\n"
            + "      :units = \"N/A\";\n"
            + "\n"
            + "    double station(station=1);\n"
            + // 2020-01-23 this was int before netcdf-java 5.2!
            "      :long_name = \"CTZ-T500-MCT-NS5649-Z408-INS12-REC14\";\n"
            + "      :cf_role = \"timeseries_id\";\n"
            + "\n"
            + "    double time(station=1, time=101);\n"
            + "      :long_name = \"tiempo en dias Julianos\";\n"
            + "      :units = \"days since 0000-01-01 00:00:00 \";\n"
            + "      :time_origin = \"0000-01-01 00:00:00\";\n"
            + "      :standard_name = \"time\";\n"
            + "      :axis = \"T\";\n"
            + "      :calendar = \"julian\";\n"
            + "      :_CoordinateAxisType = \"Time\";\n"
            + "\n"
            + "    double latitude(station=1);\n"
            + "      :long_name = \"Latitud\";\n"
            + "      :standard_name = \"latitude\";\n"
            + "      :units = \"degrees_north\";\n"
            + "      :axis = \"Y\";\n"
            + "      :_CoordinateAxisType = \"Lat\";\n"
            + "\n"
            + "    double longitude(station=1);\n"
            + "      :long_name = \"Longitud\";\n"
            + "      :units = \"degrees_east\";\n"
            + "      :standard_name = \"longitude\";\n"
            + "      :axis = \"X\";\n"
            + "      :_CoordinateAxisType = \"Lon\";\n"
            + "\n"
            + "    double z(station=1);\n"
            + "      :long_name = \"profundidad\";\n"
            + "      :units = \"m\";\n"
            + "      :standard_name = \"depth\";\n"
            + "      :axis = \"Z\";\n"
            + "      :_CoordinateAxisType = \"Height\";\n"
            + "\n"
            + "  // global attributes:\n"
            + "  :Anclaje = \"CTZ-T500\";\n"
            + "  :Equipo = \"MCT\";\n"
            + "  :Numero_de_serie = \"5649\";\n"
            + "  :Source_file = \"CTZ-T500-MCT-NS5649-Z408-INS12-REC14.mat\";\n"
            + "  :Final_NC_file = \"CTZ-T500-MCT-NS5649-Z408-INS12-REC14.nc\";\n"
            + "  :NCO = \"\\\"4.5.2\\\"\";\n"
            + "  :Conventions = \"CF-1.6\";\n"
            + "  :featureType = \"timeSeries\";\n"
            + "  :standard_name_vocabulary = \"CF-1.6\";\n"
            + "  :title = \"CTZ-T500-MCT-NS5649-Z408-INS12-REC14\";\n"
            + "  :cdm_data_type = \"TimeSeries\";\n"
            + "  :cdm_timeseries_variables = \"station\";\n"
            + "  :date_created = \"06-Aug-2014 12:22:59\";\n"
            + "  :_CoordSysBuilder = \"ucar.nc2.dataset.conv.CF1Convention\";\n"
            + "}\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // read the .ncml via table.readNcCF
    table = new Table();
    table.readNcCF(
        baseName + ".ncml",
        null,
        0, // standardizeWhat
        null,
        null,
        null);
    results = table.toString(5);
    results = String2.replaceAll(results, '\t', ' ');
    expected =
        "{\n"
            + "dimensions:\n"
            + " row = 101 ;\n"
            + "variables:\n"
            + " double Cond(row) ;\n"
            + "  Cond:coordinates = \"time latitude longitude z\" ;\n"
            + "  Cond:long_name = \"Conductividad\" ;\n"
            + "  Cond:standard_name = \"sea_water_electrical_conductivity\" ;\n"
            + "  Cond:units = \"S/m\" ;\n"
            + " double Pres(row) ;\n"
            + "  Pres:coordinates = \"time latitude longitude z\" ;\n"
            + "  Pres:long_name = \"Presion\" ;\n"
            + "  Pres:standard_name = \"sea_water_pressure\" ;\n"
            + "  Pres:units = \"dBar\" ;\n"
            + " double Temp(row) ;\n"
            + "  Temp:coordinates = \"time latitude longitude z\" ;\n"
            + "  Temp:long_name = \"Temperatura\" ;\n"
            + "  Temp:standard_name = \"sea_water_temperature\" ;\n"
            + "  Temp:units = \"degree_celsius\" ;\n"
            + " double Sal(row) ;\n"
            + "  Sal:coordinates = \"time latitude longitude z\" ;\n"
            + "  Sal:long_name = \"Salinidad\" ;\n"
            + "  Sal:standard_name = \"sea_water_salinity\" ;\n"
            + "  Sal:units = \"PSU\" ;\n"
            + " double ProfDiseno(row) ;\n"
            + "  ProfDiseno:long_name = \"Profundidad de diseno\" ;\n"
            + "  ProfDiseno:units = \"m\" ;\n"
            + " double TiranteDiseno(row) ;\n"
            + "  TiranteDiseno:long_name = \"Tirante diseno\" ;\n"
            + "  TiranteDiseno:units = \"m\" ;\n"
            + " double TiranteEstimado(row) ;\n"
            + "  TiranteEstimado:long_name = \"Tirante estimado\" ;\n"
            + "  TiranteEstimado:units = \"m\" ;\n"
            + " double var_pres(row) ;\n"
            + "  var_pres:long_name = \"Bandera presion\" ;\n"
            + "  var_pres:units = \"N/A\" ;\n"
            + " double station(row) ;\n"
            + // was int!
            "  station:cf_role = \"timeseries_id\" ;\n"
            + "  station:long_name = \"CTZ-T500-MCT-NS5649-Z408-INS12-REC14\" ;\n"
            + " double time(row) ;\n"
            + "  time:_CoordinateAxisType = \"Time\" ;\n"
            + "  time:axis = \"T\" ;\n"
            + "  time:calendar = \"julian\" ;\n"
            + "  time:long_name = \"tiempo en dias Julianos\" ;\n"
            + "  time:standard_name = \"time\" ;\n"
            + "  time:time_origin = \"0000-01-01 00:00:00\" ;\n"
            + "  time:units = \"days since 0000-01-01 00:00:00 \" ;\n"
            + " double latitude(row) ;\n"
            + "  latitude:_CoordinateAxisType = \"Lat\" ;\n"
            + "  latitude:axis = \"Y\" ;\n"
            + "  latitude:long_name = \"Latitud\" ;\n"
            + "  latitude:standard_name = \"latitude\" ;\n"
            + "  latitude:units = \"degrees_north\" ;\n"
            + " double longitude(row) ;\n"
            + "  longitude:_CoordinateAxisType = \"Lon\" ;\n"
            + "  longitude:axis = \"X\" ;\n"
            + "  longitude:long_name = \"Longitud\" ;\n"
            + "  longitude:standard_name = \"longitude\" ;\n"
            + "  longitude:units = \"degrees_east\" ;\n"
            + " double z(row) ;\n"
            + "  z:_CoordinateAxisType = \"Height\" ;\n"
            + "  z:axis = \"Z\" ;\n"
            + "  z:long_name = \"profundidad\" ;\n"
            + "  z:standard_name = \"depth\" ;\n"
            + "  z:units = \"m\" ;\n"
            + "\n"
            + "// global attributes:\n"
            + "  :_CoordSysBuilder = \"ucar.nc2.dataset.conv.CF1Convention\" ;\n"
            + "  :Anclaje = \"CTZ-T500\" ;\n"
            + "  :cdm_data_type = \"TimeSeries\" ;\n"
            + "  :cdm_timeseries_variables = \"ProfDiseno, TiranteDiseno, TiranteEstimado, var_pres, station, latitude, longitude, z\" ;\n"
            + "  :Conventions = \"CF-1.6\" ;\n"
            + "  :date_created = \"06-Aug-2014 12:22:59\" ;\n"
            + "  :Equipo = \"MCT\" ;\n"
            + "  :featureType = \"timeSeries\" ;\n"
            + "  :Final_NC_file = \"CTZ-T500-MCT-NS5649-Z408-INS12-REC14.nc\" ;\n"
            + "  :NCO = \"\\\"4.5.2\\\"\" ;\n"
            + "  :Numero_de_serie = \"5649\" ;\n"
            + "  :Source_file = \"CTZ-T500-MCT-NS5649-Z408-INS12-REC14.mat\" ;\n"
            + "  :standard_name_vocabulary = \"CF-1.6\" ;\n"
            + "  :subsetVariables = \"ProfDiseno, TiranteDiseno, TiranteEstimado, var_pres, station, latitude, longitude, z\" ;\n"
            + "  :title = \"CTZ-T500-MCT-NS5649-Z408-INS12-REC14\" ;\n"
            + "}\n"
            + "Cond,Pres,Temp,Sal,ProfDiseno,TiranteDiseno,TiranteEstimado,var_pres,station,time,latitude,longitude,z\n"
            + "3.88991,409.629,10.3397,35.310065426337346,408.0,500.0,498.0,1.0,0.0,733358.7847222222,18.843666666666667,-94.81761666666667,406.0\n"
            + "3.88691,409.12,10.3353,35.28414747593317,408.0,500.0,498.0,1.0,0.0,733358.786111111,18.843666666666667,-94.81761666666667,406.0\n"
            + "3.88678,408.803,10.3418,35.27667928948258,408.0,500.0,498.0,1.0,0.0,733358.7875,18.843666666666667,-94.81761666666667,406.0\n"
            + "3.88683,408.623,10.3453,35.273879094537904,408.0,500.0,498.0,1.0,0.0,733358.7888888889,18.843666666666667,-94.81761666666667,406.0\n"
            + "3.88808,408.517,10.3687,35.26394801644307,408.0,500.0,498.0,1.0,0.0,733358.7902777778,18.843666666666667,-94.81761666666667,406.0\n"
            + "...\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // read the .ncml via table.readNcCF -- just station info
    table = new Table();
    table.readNcCF(
        baseName + ".ncml",
        StringArray.fromCSV(
            "station,latitude,longitude,z,ProfDiseno,TiranteDiseno,TiranteEstimado,var_pres"),
        0, // standardizeWhat
        null,
        null,
        null);
    results = table.dataToString();
    results = String2.replaceAll(results, '\t', ' ');
    expected =
        "station,latitude,longitude,z,ProfDiseno,TiranteDiseno,TiranteEstimado,var_pres\n"
            + "0.0,18.843666666666667,-94.81761666666667,406.0,408.0,500.0,498.0,1.0\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    String2.log("\n*** EDDTableFromNcCFFiles.testNcml() finished.");
  }

  /**
   * This tests an NCEI WOD file with a "structure".
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest // Unable to open file or file not .nc-compatible.
  void testJP14323() throws Throwable {
    // String2.log("\n****************** EDDTableFromNcCFFiles.testJP14323()
    // *****************\n");
    // testVerboseOn();
    String dir =
        Path.of(EDDTableFromNcCFFilesTests.class.getResource("/data/nccf/ncei/").toURI())
            .toString();
    String sampleName = "biology_JP14323.nc";
    String results, expected;
    Table table;

    // ncdump the .nc file
    String2.log("Here's the ncdump of " + dir + sampleName);
    results = NcHelper.ncdump(dir + sampleName, "-h");
    String2.log(results);
    expected =
        "netcdf biology_JP14323.nc {\n"
            + "  dimensions:\n"
            + "    casts = 52;\n"
            + "    z_obs = 74;\n"
            + "    Temperature_obs = 74;\n"
            + "    strnlen = 170;\n"
            + "    strnlensmall = 35;\n"
            + "    biosets = 52;\n"
            + "  variables:\n"
            + "    char country(casts=52, strnlensmall=35);\n"
            + "\n"
            + "    char WOD_cruise_identifier(casts=52, strnlensmall=35);\n"
            + "      :comment = \"two byte country code + WOD cruise number (unique to country code)\";\n"
            + "      :long_name = \"WOD_cruise_identifier\";\n"
            + "\n"
            + "    int wod_unique_cast(casts=52);\n"
            + "      :cf_role = \"profile_id\";\n"
            + "\n"
            + "    float lat(casts=52);\n"
            + "      :standard_name = \"latitude\";\n"
            + "      :long_name = \"latitude\";\n"
            + "      :units = \"degrees_north\";\n"
            + "\n"
            + "    float lon(casts=52);\n"
            + "      :standard_name = \"longitude\";\n"
            + "      :long_name = \"longitude\";\n"
            + "      :units = \"degrees_east\";\n"
            + "\n"
            + "    double time(casts=52);\n"
            + "      :standard_name = \"time\";\n"
            + "      :long_name = \"time\";\n"
            + "      :units = \"days since 1770-01-01 00:00:00\";\n"
            + "\n"
            + "    int date(casts=52);\n"
            + "      :long_name = \"date\";\n"
            + "      :comment = \"YYYYMMDD\";\n"
            + "\n"
            + "    float GMT_time(casts=52);\n"
            + "      :long_name = \"GMT_time\";\n"
            + "\n"
            + "    int Access_no(casts=52);\n"
            + "      :long_name = \"NODC_accession_number\";\n"
            + "      :units_wod = \"NODC_code\";\n"
            + "      :comment = \"used to find original data at NODC\";\n"
            + "\n"
            + "    char Platform(casts=52, strnlen=170);\n"
            + "      :long_name = \"Platform_name\";\n"
            + "      :comment = \"name of platform from which measurements were taken\";\n"
            + "\n"
            + "    char Institute(casts=52, strnlen=170);\n"
            + "      :long_name = \"Responsible_institute\";\n"
            + "      :comment = \"name of institute which collected data\";\n"
            + "\n"
            + "    char dataset(casts=52, strnlen=170);\n"
            + "      :long_name = \"WOD_dataset\";\n"
            + "\n"
            + "    float z(z_obs=74);\n"
            + "      :standard_name = \"altitude\";\n"
            + "      :long_name = \"depth_below_sea_level\";\n"
            + "      :units = \"m\";\n"
            + "      :positive = \"down\";\n"
            + "\n"
            + "    short z_WODflag(z_obs=74);\n"
            + "      :flag_definitions = \"WODfd\";\n"
            + "\n"
            + "    short z_sigfig(z_obs=74);\n"
            + "\n"
            + "    int z_row_size(casts=52);\n"
            + "      :long_name = \"number of depth observations for this cast\";\n"
            + "      :sample_dimension = \"z_obs\";\n"
            + "\n"
            + "    float Temperature(Temperature_obs=74);\n"
            + "      :long_name = \"Temperature\";\n"
            + "      :standard_name = \"sea_water_temperature\";\n"
            + "      :units = \"degree_C\";\n"
            + "      :coordinates = \"time lat lon z\";\n"
            + "      :grid_mapping = \"crs\";\n"
            + "\n"
            + "    short Temperature_sigfigs(Temperature_obs=74);\n"
            + "\n"
            + "    short Temperature_row_size(casts=52);\n"
            + "      :long_name = \"number of Temperature observations for this cast\";\n"
            + "      :sample_dimension = \"Temperature_obs\";\n"
            + "\n"
            + "    short Temperature_WODflag(Temperature_obs=74);\n"
            + "      :flag_definitions = \"WODf\";\n"
            + "\n"
            + "    short Temperature_WODprofileflag(casts=52);\n"
            + "      :flag_definitions = \"WODfp\";\n"
            + "\n"
            + "    float Mesh_size(casts=52);\n"
            + "      :long_name = \"Mesh_size\";\n"
            + "      :units = \"microns\";\n"
            + "\n"
            + "    char Type_tow(casts=52, strnlen=170);\n"
            + "      :long_name = \"Type_of_tow\";\n"
            + "      :units = \"WOD_code\";\n"
            + "      :comment = \"0\";\n"
            + "\n"
            + "    char Gear_code(casts=52, strnlen=170);\n"
            + "      :long_name = \"WOD_code\";\n"
            + "      :comment = \"Gear_code\";\n"
            + "\n"
            + "    float net_mouth_area(casts=52);\n"
            + "      :long_name = \"net_mouth_area\";\n"
            + "      :units = \"m2\";\n"
            + "      :comment = \"sampling input area (net mouth)\";\n"
            + "\n"
            + "    float GMT_sample_start_time(casts=52);\n"
            + "      :long_name = \"GMT_sample_start_time\";\n"
            + "      :units = \"hour\";\n"
            + "      :comment = \"Start time (GMT) of the sampling event\";\n"
            + "\n"
            + "    int Biology_Accno(casts=52);\n"
            + "      :long_name = \"Biology_Accn#\";\n"
            + "      :units = \"NODC_code\";\n"
            + "      :comment = \"Accession # for the biology component\";\n"
            + "\n"
            + "\n"
            + "    Structure {\n"
            + "      char taxa_name_bio(100);\n"
            + "      float upper_z_bio;\n"
            + "      float lower_z_bio;\n"
            + "      int measure_abund_bio;\n"
            + "      char measure_type_bio(15);\n"
            + "      float measure_val_bio;\n"
            + "      char measure_units_bio(10);\n"
            + "      int measure_flag_bio;\n"
            + "      float cbv_value_bio;\n"
            + "      int cbv_flag_bio;\n"
            + "      char cbv_units_bio(6);\n"
            + "      float cbv_method_bio;\n"
            + "      int pgc_code_bio;\n"
            + "      int taxa_modifier_bio;\n"
            + "      int taxa_sex_bio;\n"
            + "      int taxa_stage_bio;\n"
            + "      int taxa_troph_bio;\n"
            + "      int taxa_realm_bio;\n"
            + "      int taxa_feature_bio;\n"
            + "      int taxa_method_bio;\n"
            + "      int taxa_minsize_desc_bio;\n"
            + "      float taxa_minsize_val_bio;\n"
            + "      int taxa_maxsize_desc_bio;\n"
            + "      float taxa_maxsize_val_bio;\n"
            + "      float taxa_length_bio;\n"
            + "      float taxa_width_bio;\n"
            + "      float taxa_radius_bio;\n"
            + "      float sample_volume_bio;\n"
            + "    } plankton(biosets=52);\n"
            + "\n"
            + "\n"
            + "    int plankton_row_size(casts=52);\n"
            + "\n"
            + "    int crs;\n"
            + "      :grid_mapping_name = \"latitude_longitude\";\n"
            + "      :epsg_code = \"EPSG:4326\";\n"
            + "      :longitude_of_prime_meridian = 0.0f; // float\n"
            + "      :semi_major_axis = 6378137.0f; // float\n"
            + "      :inverse_flattening = 298.25723f; // float\n"
            + "\n"
            + "    short WODf;\n"
            + "      :long_name = \"WOD_observation_flag\";\n"
            + "      :flag_values = 0S, 1S, 2S, 3S, 4S, 5S, 6S, 7S, 8S, 9S; // short\n"
            + "      :flag_meanings = \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\";\n"
            + "\n"
            + "    short WODfp;\n"
            + "      :long_name = \"WOD_profile_flag\";\n"
            + "      :flag_values = 0S, 1S, 2S, 3S, 4S, 5S, 6S, 7S, 8S, 9S; // short\n"
            + "      :flag_meanings = \"accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out\";\n"
            + "\n"
            + "    short WODfd;\n"
            + "      :long_name = \"WOD_depth_level_\";\n"
            + "      :flag_values = 0S, 1S, 2S; // short\n"
            + "      :flag_meanings = \"accepted duplicate_or_inversion density_inversion\";\n"
            + "\n"
            + "  // global attributes:\n"
            + "  :institution = \"National Oceanographic Data Center(NODC), NOAA\";\n"
            + "  :source = \"World Ocean Database\";\n"
            + "  :references = \"World Ocean Database 2013. URL:https://data.nodc.noaa.gov/woa/WOD/DOC/wod_intro.pdf\";\n"
            + "  :title = \"World Ocean Database - Multi-cast file\";\n"
            + "  :summary = \"Data for multiple casts from the World Ocean Database\";\n"
            + "  :id = \"biology_JP14323.nc\";\n"
            + "  :naming_authority = \"gov.noaa.nodc\";\n"
            + "  :geospatial_lat_min = 36.3f; // float\n"
            + "  :geospatial_lat_max = 42.866665f; // float\n"
            + "  :geospatial_lat_resolution = \"point\";\n"
            + "  :geospatial_lon_min = 140.61667f; // float\n"
            + "  :geospatial_lon_max = 147.0f; // float\n"
            + "  :geospatial_lon_resolution = \"point\";\n"
            + "  :time_coverage_start = \"1970-10-08\";\n"
            + "  :time_coverage_end = \"1971-01-08\";\n"
            + "  :geospatial_vertical_min = 0.0f; // float\n"
            + "  :geospatial_vertical_max = 100.0f; // float\n"
            + "  :geospatial_vertical_positive = \"down\";\n"
            + "  :geospatial_vertical_units = \"meters\";\n"
            + "  :creator_name = \"Ocean Climate Lab/NODC\";\n"
            + "  :creator_email = \"OCLhelp@noaa.gov\";\n"
            + "  :creator_url = \"https://www.nodc.noaa.gov\";\n"
            + "  :project = \"World Ocean Database\";\n"
            + "  :acknowledgements = \"\";\n"
            + "  :processing_level = \"\";\n"
            + "  :keywords = \"\";\n"
            + "  :keywords_vocabulary = \"\";\n"
            + "  :date_created = \"2016-05-20\";\n"
            + "  :date_modified = \"2016-05-20\";\n"
            + "  :publisher_name = \"US DOC; NESDIS; NATIONAL OCEANOGRAPHIC DATA CENTER - IN295\";\n"
            + "  :publisher_url = \"https://www.nodc.noaa.gov\";\n"
            + "  :publisher_email = \"NODC.Services@noaa.gov\";\n"
            + "  :history = \"\";\n"
            + "  :license = \"\";\n"
            + "  :standard_name_vocabulary = \"CF-1.6\";\n"
            + "  :featureType = \"Profile\";\n"
            + "  :cdm_data_type = \"Profile\";\n"
            + "  :Conventions = \"CF-1.6\";\n"
            + " data:\n"
            + "}\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // read the table
    table = new Table();
    table.readNcCF(
        dir + sampleName,
        null,
        0, // standardizeWhat
        null,
        null,
        null);

    results = table.dataToString(5);
    expected = "zztop\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    /*
     * //read the .ncml via table.readNcCF
     * table = new Table();
     * table.readNcCF(baseName + ".ncml", null, null, null, null);
     * results = table.toCSVString(5);
     * results = String2.replaceAll(results, '\t', ' ');
     * expected =
     * "{\n" +
     * "dimensions:\n" +
     * " row = 101 ;\n" +
     * "variables:\n" +
     * " double Cond(row) ;\n" +
     * "  Cond:coordinates = \"time latitude longitude z\" ;\n" +
     * "  Cond:long_name = \"Conductividad\" ;\n" +
     * "  Cond:standard_name = \"sea_water_electrical_conductivity\" ;\n" +
     * "  Cond:units = \"S/m\" ;\n" +
     * " double Pres(row) ;\n" +
     * "  Pres:coordinates = \"time latitude longitude z\" ;\n" +
     * "  Pres:long_name = \"Presion\" ;\n" +
     * "  Pres:standard_name = \"sea_water_pressure\" ;\n" +
     * "  Pres:units = \"dBar\" ;\n" +
     * " double Temp(row) ;\n" +
     * "  Temp:coordinates = \"time latitude longitude z\" ;\n" +
     * "  Temp:long_name = \"Temperatura\" ;\n" +
     * "  Temp:standard_name = \"sea_water_temperature\" ;\n" +
     * "  Temp:units = \"degree_celsius\" ;\n" +
     * " double Sal(row) ;\n" +
     * "  Sal:coordinates = \"time latitude longitude z\" ;\n" +
     * "  Sal:long_name = \"Salinidad\" ;\n" +
     * "  Sal:standard_name = \"sea_water_salinity\" ;\n" +
     * "  Sal:units = \"PSU\" ;\n" +
     * " double ProfDiseno(row) ;\n" +
     * "  ProfDiseno:long_name = \"Profundidad de diseno\" ;\n" +
     * "  ProfDiseno:units = \"m\" ;\n" +
     * " double TiranteDiseno(row) ;\n" +
     * "  TiranteDiseno:long_name = \"Tirante diseno\" ;\n" +
     * "  TiranteDiseno:units = \"m\" ;\n" +
     * " double TiranteEstimado(row) ;\n" +
     * "  TiranteEstimado:long_name = \"Tirante estimado\" ;\n" +
     * "  TiranteEstimado:units = \"m\" ;\n" +
     * " double var_pres(row) ;\n" +
     * "  var_pres:long_name = \"Bandera presion\" ;\n" +
     * "  var_pres:units = \"N/A\" ;\n" +
     * " int station(row) ;\n" +
     * "  station:cf_role = \"timeseries_id\" ;\n" +
     * "  station:long_name = \"CTZ-T500-MCT-NS5649-Z408-INS12-REC14\" ;\n" +
     * " double time(row) ;\n" +
     * "  time:_CoordinateAxisType = \"Time\" ;\n" +
     * "  time:axis = \"T\" ;\n" +
     * "  time:calendar = \"julian\" ;\n" +
     * "  time:long_name = \"tiempo en dias Julianos\" ;\n" +
     * "  time:standard_name = \"time\" ;\n" +
     * "  time:time_origin = \"0000-01-01 00:00:00\" ;\n" +
     * "  time:units = \"days since 0000-01-01 00:00:00 \" ;\n" +
     * " double latitude(row) ;\n" +
     * "  latitude:_CoordinateAxisType = \"Lat\" ;\n" +
     * "  latitude:axis = \"Y\" ;\n" +
     * "  latitude:long_name = \"Latitud\" ;\n" +
     * "  latitude:standard_name = \"latitude\" ;\n" +
     * "  latitude:units = \"degrees_north\" ;\n" +
     * " double longitude(row) ;\n" +
     * "  longitude:_CoordinateAxisType = \"Lon\" ;\n" +
     * "  longitude:axis = \"X\" ;\n" +
     * "  longitude:long_name = \"Longitud\" ;\n" +
     * "  longitude:standard_name = \"longitude\" ;\n" +
     * "  longitude:units = \"degrees_east\" ;\n" +
     * " double z(row) ;\n" +
     * "  z:_CoordinateAxisType = \"Height\" ;\n" +
     * "  z:axis = \"Z\" ;\n" +
     * "  z:long_name = \"profundidad\" ;\n" +
     * "  z:standard_name = \"depth\" ;\n" +
     * "  z:units = \"m\" ;\n" +
     * "\n" +
     * "// global attributes:\n" +
     * "  :_CoordSysBuilder = \"ucar.nc2.dataset.conv.CF1Convention\" ;\n" +
     * "  :Anclaje = \"CTZ-T500\" ;\n" +
     * "  :cdm_data_type = \"TimeSeries\" ;\n" +
     * "  :cdm_timeseries_variables = \"ProfDiseno, TiranteDiseno, TiranteEstimado, var_pres, station, latitude, longitude, z\" ;\n"
     * +
     * "  :Conventions = \"CF-1.6\" ;\n" +
     * "  :date_created = \"06-Aug-2014 12:22:59\" ;\n" +
     * "  :Equipo = \"MCT\" ;\n" +
     * "  :featureType = \"timeSeries\" ;\n" +
     * "  :Final_NC_file = \"CTZ-T500-MCT-NS5649-Z408-INS12-REC14.nc\" ;\n" +
     * "  :NCO = \"\\\"4.5.2\\\"\" ;\n" +
     * "  :Numero_de_serie = \"5649\" ;\n" +
     * "  :Source_file = \"CTZ-T500-MCT-NS5649-Z408-INS12-REC14.mat\" ;\n" +
     * "  :standard_name_vocabulary = \"CF-1.6\" ;\n" +
     * "  :subsetVariables = \"ProfDiseno, TiranteDiseno, TiranteEstimado, var_pres, station, latitude, longitude, z\" ;\n"
     * +
     * "  :title = \"CTZ-T500-MCT-NS5649-Z408-INS12-REC14\" ;\n" +
     * "}\n" +
     * "Cond,Pres,Temp,Sal,ProfDiseno,TiranteDiseno,TiranteEstimado,var_pres,station,time,latitude,longitude,z\n"
     * +
     * "3.88991,409.629,10.3397,35.310065426337346,408.0,500.0,498.0,1.0,0,733358.7847222222,18.843666666666667,-94.81761666666667,406.0\n"
     * +
     * "3.88691,409.12,10.3353,35.28414747593317,408.0,500.0,498.0,1.0,0,733358.786111111,18.843666666666667,-94.81761666666667,406.0\n"
     * +
     * "3.88678,408.803,10.3418,35.27667928948258,408.0,500.0,498.0,1.0,0,733358.7875,18.843666666666667,-94.81761666666667,406.0\n"
     * +
     * "3.88683,408.623,10.3453,35.273879094537904,408.0,500.0,498.0,1.0,0,733358.7888888889,18.843666666666667,-94.81761666666667,406.0\n"
     * +
     * "3.88808,408.517,10.3687,35.26394801644307,408.0,500.0,498.0,1.0,0,733358.7902777778,18.843666666666667,-94.81761666666667,406.0\n"
     * +
     * "...\n";
     * Test.ensureEqual(results, expected, "results=\n" + results);
     *
     * //read the .ncml via table.readNcCF -- just station info
     * table = new Table();
     * table.readNcCF(baseName + ".ncml",
     * StringArray.fromCSV(
     * "station,latitude,longitude,z,ProfDiseno,TiranteDiseno,TiranteEstimado,var_pres"
     * ), null, null, null);
     * results = table.dataToString();
     * results = String2.replaceAll(results, '\t', ' ');
     * expected =
     * "station,latitude,longitude,z,ProfDiseno,TiranteDiseno,TiranteEstimado,var_pres\n"
     * +
     * "0,18.843666666666667,-94.81761666666667,406.0,408.0,500.0,498.0,1.0\n";
     * Test.ensureEqual(results, expected, "results=\n" + results);
     */

    // String2.log("\n*** EDDTableFromNcCFFiles.testJP14323() finished.");
  }
}
