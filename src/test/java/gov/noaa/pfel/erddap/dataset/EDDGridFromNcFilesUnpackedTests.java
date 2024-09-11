package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Image2Tests;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.Units2;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tags.TagImageComparison;
import tags.TagIncompleteTest;
import testDataset.EDDTestDataset;
import testDataset.Initialization;
import ucar.ma2.Array;
import ucar.ma2.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

class EDDGridFromNcFilesUnpackedTests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /**
   * This tests generateDatasetsXml.
   *
   * @throws Throwable if touble
   */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest
  void testGenerateDatasetsXml() throws Throwable {

    // String2.log("\n*** EDDGridFromNcFilesUnpacked.testGenerateDatasetsXml");

    String sampleDir =
        Path.of(EDDGridFromNcFilesUnpackedTests.class.getResource("/data/nc/").toURI()).toString()
            + "/";
    String sampleRegex = "scale_factor\\.nc";
    String sampleName = sampleDir + "scale_factor.nc";

    // test that sample file has short analysed_sst with scale_factor and add_offset
    String results = NcHelper.ncdump(sampleName, ""); // short data
    String expected =
        "netcdf scale_factor.nc {\n"
            + "  dimensions:\n"
            + "    time = 2;\n"
            + "    lat = 10;\n"
            + "    lon = 10;\n"
            + "  variables:\n"
            + "    short analysed_sst(time=2, lat=10, lon=10);\n"
            + "      :long_name = \"analysed sea surface temperature\";\n"
            + "      :standard_name = \"sea_surface_foundation_temperature\";\n"
            + "      :units = \"kelvin\";\n"
            + "      :_FillValue = -32768S; // short\n"
            + "      :add_offset = 298.15; // double\n"
            + "      :scale_factor = 0.001; // double\n"
            + "      :valid_min = -32767S; // short\n"
            + "      :valid_max = 32767S; // short\n"
            + "      :comment = \"Interim near-real-time (nrt) version; to be replaced by Final version\";\n"
            + "      :coordinates = \"time lat lon \";\n"
            + "\n"
            + "    int time(time=2);\n"
            + "      :long_name = \"reference time of sst field\";\n"
            + "      :standard_name = \"time\";\n"
            + "      :axis = \"T\";\n"
            + "      :units = \"seconds since 1981-01-01 00:00:00 UTC\";\n"
            + "      :_CoordinateAxisType = \"Time\";\n"
            + "\n"
            + "    float lat(lat=10);\n"
            + "      :long_name = \"latitude\";\n"
            + "      :standard_name = \"latitude\";\n"
            + "      :axis = \"Y\";\n"
            + "      :units = \"degrees_north\";\n"
            + "      :valid_min = -90.0f; // float\n"
            + "      :valid_max = 90.0f; // float\n"
            + "      :_CoordinateAxisType = \"Lat\";\n"
            + "\n"
            + "    float lon(lon=10);\n"
            + "      :long_name = \"longitude\";\n"
            + "      :standard_name = \"longitude\";\n"
            + "      :axis = \"X\";\n"
            + "      :units = \"degrees_east\";\n"
            + "      :valid_min = -180.0f; // float\n"
            + "      :valid_max = 180.0f; // float\n"
            + "      :_CoordinateAxisType = \"Lon\";\n"
            + "\n"
            + "  // global attributes:\n"
            + "  :title = \"Daily MUR SST, Interim near-real-time (nrt) product\";\n"
            + "  :comment = \"Interim-MUR(nrt) will be replaced by MUR-Final in about 3 days; MUR = \\\"Multi-scale Ultra-high Reolution\\\"; produced under NASA MEaSUREs program.\";\n"
            + "  :Conventions = \"CF-1.0\";\n"
            + "  :DSD_entry_id = \"JPL-L4UHfnd-GLOB-MUR\";\n"
            + "  :references = \"ftp://mariana.jpl.nasa.gov/mur_sst/tmchin/docs/ATBD/\";\n"
            + "  :source_data = \"AVHRR19_G-NAVO, AVHRR_METOP_A-EUMETSAT, MODIS_A-JPL, MODIS_T-JPL, WSAT-REMSS, iQUAM-NOAA/NESDIS, Ice_Conc-OSISAF\";\n"
            + "  :institution = \"Jet Propulsion Laboratory\";\n"
            + "  :contact = \"ghrsst@podaac.jpl.nasa.gov\";\n"
            + "  :GDS_version_id = \"GDS-v1.0-rev1.6\";\n"
            + "  :netcdf_version_id = \"3.5\";\n"
            + "  :creation_date = \"2015-10-06\";\n"
            + "  :product_version = \"04nrt\";\n"
            + "  :history = \"Interim near-real-time (nrt) version created at nominal 1-day latency.\";\n"
            + "  :spatial_resolution = \"0.011 degrees\";\n"
            + "  :start_date = \"2015-10-05\";\n"
            + "  :start_time = \"09:00:00 UTC\";\n"
            + "  :stop_date = \"2015-10-05\";\n"
            + "  :stop_time = \"09:00:00 UTC\";\n"
            + "  :southernmost_latitude = -90.0f; // float\n"
            + "  :northernmost_latitude = 90.0f; // float\n"
            + "  :westernmost_longitude = -180.0f; // float\n"
            + "  :easternmost_longitude = 180.0f; // float\n"
            + "  :file_quality_index = \"0\";\n"
            + "  :History = \"Translated to CF-1.0 Conventions by Netcdf-Java CDM (NetcdfCFWriter)\n"
            + "Original Dataset = satellite/MUR/ssta/1day; Translation Date = Thu Oct 08 09:39:01 PDT 2015\";\n"
            + "\n"
            + "  data:\n"
            + "    analysed_sst = \n"
            + "      {\n"
            + "        {\n"
            + "          {1779, 1790, 1802, 1815, 1827, 1839, 1851, 1862, 1874, 1886},\n"
            + "          {1782, 1792, 1804, 1816, 1828, 1840, 1851, 1863, 1875, 1887},\n"
            + "          {1786, 1795, 1805, 1817, 1828, 1839, 1851, 1862, 1874, 1885},\n"
            + "          {1789, 1798, 1807, 1817, 1828, 1838, 1849, 1860, 1871, 1882},\n"
            + "          {1793, 1800, 1808, 1817, 1827, 1836, 1846, 1856, 1866, 1876},\n"
            + "          {1795, 1801, 1809, 1816, 1825, 1833, 1842, 1851, 1859, 1868},\n"
            + "          {1796, 1802, 1808, 1815, 1822, 1829, 1836, 1844, 1851, 1858},\n"
            + "          {1797, 1801, 1807, 1812, 1818, 1824, 1830, 1836, 1842, 1848},\n"
            + "          {1796, 1800, 1804, 1809, 1813, 1818, 1822, 1827, 1832, 1836},\n"
            + "          {1794, 1797, 1801, 1804, 1807, 1811, 1814, 1817, 1821, 1824}\n"
            + "        },\n"
            + "        {\n"
            + "          {1773, 1777, 1782, 1787, 1792, 1798, 1803, 1809, 1815, 1821},\n"
            + "          {1776, 1780, 1784, 1789, 1794, 1800, 1805, 1811, 1817, 1823},\n"
            + "          {1778, 1782, 1787, 1792, 1797, 1802, 1807, 1813, 1819, 1825},\n"
            + "          {1781, 1785, 1789, 1794, 1799, 1804, 1809, 1815, 1820, 1826},\n"
            + "          {1783, 1787, 1791, 1796, 1800, 1805, 1810, 1816, 1821, 1826},\n"
            + "          {1786, 1789, 1793, 1798, 1802, 1806, 1811, 1816, 1821, 1826},\n"
            + "          {1788, 1791, 1795, 1799, 1803, 1807, 1812, 1816, 1821, 1825},\n"
            + "          {1790, 1793, 1796, 1800, 1804, 1807, 1811, 1815, 1820, 1824},\n"
            + "          {1791, 1794, 1797, 1800, 1804, 1807, 1811, 1814, 1818, 1822},\n"
            + "          {1792, 1795, 1797, 1800, 1803, 1806, 1809, 1812, 1816, 1819}\n"
            + "        }\n"
            + "      }\n"
            + "    time = \n"
            + "      {1096880400, 1096966800}\n"
            + "    lat = \n"
            + "      {20.0006, 20.0116, 20.0226, 20.0336, 20.0446, 20.0555, 20.0665, 20.0775, 20.0885, 20.0995}\n"
            + "    lon = \n"
            + "      {-134.995, -134.984, -134.973, -134.962, -134.951, -134.94, -134.929, -134.918, -134.907, -134.896}\n"
            + "}\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // scale_factor.nc analysed_sst is short with scale_factor and add offset.
    // Loading via openDataset tells netcdf-java to unpack the variable
    // so here it appears not to be double var with no scale_factor or add_offset
    results =
        EDDGridFromNcFilesUnpacked.generateDatasetsXml(
                sampleDir,
                sampleRegex,
                sampleName,
                "", // group
                "",
                EDDGridFromNcFilesUnpacked.DEFAULT_RELOAD_EVERY_N_MINUTES,
                null,
                null)
            + "\n"; // dimensionsCSV,
    // reloadMinutes,
    // cacheFromUrl
    String suggDatasetID = EDDGridFromNcFilesUnpacked.suggestDatasetID(sampleDir + sampleRegex);

    // GenerateDatasetsXml
    String gdxResults =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {
                  "-verbose",
                  "EDDGridFromNcFilesUnpacked",
                  sampleDir,
                  sampleRegex,
                  sampleName,
                  "", // group
                  "",
                  "" + EDDGridFromNcFilesUnpacked.DEFAULT_RELOAD_EVERY_N_MINUTES,
                  ""
                }, // dimensionsCSV,
                // reloadMinutes, cacheFromUrl
                false); // doIt loop?
    Test.ensureEqual(
        gdxResults,
        results,
        "Unexpected results from GenerateDatasetsXml.doIt. "
            + gdxResults.length()
            + " "
            + results.length());

    expected =
        "<dataset type=\"EDDGridFromNcFilesUnpacked\" datasetID=\""
            + suggDatasetID
            + "\" active=\"true\">\n"
            + "    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n"
            + "    <updateEveryNMillis>10000</updateEveryNMillis>\n"
            + "    <fileDir>"
            + sampleDir
            + "</fileDir>\n"
            + "    <fileNameRegex>scale_factor\\.nc</fileNameRegex>\n"
            + "    <recursive>true</recursive>\n"
            + "    <pathRegex>.*</pathRegex>\n"
            + "    <metadataFrom>last</metadataFrom>\n"
            + "    <matchAxisNDigits>20</matchAxisNDigits>\n"
            + "    <fileTableInMemory>false</fileTableInMemory>\n"
            + "    <!-- sourceAttributes>\n"
            + "        <att name=\"comment\">Interim-MUR(nrt) will be replaced by MUR-Final in about 3 days; MUR = &quot;Multi-scale Ultra-high Reolution&quot;; produced under NASA MEaSUREs program.</att>\n"
            + "        <att name=\"contact\">ghrsst@podaac.jpl.nasa.gov</att>\n"
            + "        <att name=\"Conventions\">CF-1.0</att>\n"
            + "        <att name=\"creation_date\">2015-10-06</att>\n"
            + "        <att name=\"DSD_entry_id\">JPL-L4UHfnd-GLOB-MUR</att>\n"
            + "        <att name=\"easternmost_longitude\" type=\"float\">180.0</att>\n"
            + "        <att name=\"file_quality_index\">0</att>\n"
            + "        <att name=\"GDS_version_id\">GDS-v1.0-rev1.6</att>\n"
            + "        <att name=\"History\">Translated to CF-1.0 Conventions by Netcdf-Java CDM (NetcdfCFWriter)\n"
            + "Original Dataset = satellite/MUR/ssta/1day; Translation Date = Thu Oct 08 09:39:01 PDT 2015</att>\n"
            + "        <att name=\"history\">Interim near-real-time (nrt) version created at nominal 1-day latency.</att>\n"
            + "        <att name=\"institution\">Jet Propulsion Laboratory</att>\n"
            + "        <att name=\"netcdf_version_id\">3.5</att>\n"
            + "        <att name=\"northernmost_latitude\" type=\"float\">90.0</att>\n"
            + "        <att name=\"product_version\">04nrt</att>\n"
            + "        <att name=\"references\">ftp://mariana.jpl.nasa.gov/mur_sst/tmchin/docs/ATBD/</att>\n"
            + "        <att name=\"source_data\">AVHRR19_G-NAVO, AVHRR_METOP_A-EUMETSAT, MODIS_A-JPL, MODIS_T-JPL, WSAT-REMSS, iQUAM-NOAA/NESDIS, Ice_Conc-OSISAF</att>\n"
            + "        <att name=\"southernmost_latitude\" type=\"float\">-90.0</att>\n"
            + "        <att name=\"spatial_resolution\">0.011 degrees</att>\n"
            + "        <att name=\"start_date\">2015-10-05</att>\n"
            + "        <att name=\"start_time\">09:00:00 UTC</att>\n"
            + "        <att name=\"stop_date\">2015-10-05</att>\n"
            + "        <att name=\"stop_time\">09:00:00 UTC</att>\n"
            + "        <att name=\"title\">Daily MUR SST, Interim near-real-time (nrt) product</att>\n"
            + "        <att name=\"westernmost_longitude\" type=\"float\">-180.0</att>\n"
            + "    </sourceAttributes -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cdm_data_type\">Grid</att>\n"
            + "        <att name=\"Conventions\">CF-1.10, COARDS, ACDD-1.3</att>\n"
            + "        <att name=\"creator_email\">ghrsst@podaac.jpl.nasa.gov</att>\n"
            + "        <att name=\"creator_name\">GHRSST</att>\n"
            + "        <att name=\"creator_type\">group</att>\n"
            + "        <att name=\"creator_url\">https://podaac.jpl.nasa.gov/</att>\n"
            + "        <att name=\"easternmost_longitude\">null</att>\n"
            + "        <att name=\"History\">null</att>\n"
            + "        <att name=\"infoUrl\">https://podaac.jpl.nasa.gov/</att>\n"
            + "        <att name=\"keywords\">analysed, analysed_sst, daily, data, day, earth, Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature, environments, foundation, high, interim, jet, laboratory, making, measures, multi, multi-scale, mur, near, near real time, near-real-time, nrt, ocean, oceans, product, propulsion, real, records, research, resolution, scale, science, sea, sea_surface_foundation_temperature, sst, surface, system, temperature, time, ultra, ultra-high, use</att>\n"
            + "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n"
            + "        <att name=\"license\">[standard]</att>\n"
            + "        <att name=\"netcdf_version_id\">null</att>\n"
            + "        <att name=\"northernmost_latitude\">null</att>\n"
            + "        <att name=\"southernmost_latitude\">null</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"start_date\">null</att>\n"
            + "        <att name=\"start_time\">null</att>\n"
            + "        <att name=\"stop_date\">null</att>\n"
            + "        <att name=\"stop_time\">null</att>\n"
            + "        <att name=\"summary\">Interim-Multi-scale Ultra-high Resolution (MUR)(nrt) will be replaced by MUR-Final in about 3 days; MUR = &quot;Multi-scale Ultra-high Reolution&quot;; produced under NASA Making Earth System Data Records for Use in Research Environments (MEaSUREs) program.</att>\n"
            + "        <att name=\"westernmost_longitude\">null</att>\n"
            + "    </addAttributes>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>time</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Time</att>\n"
            + "            <att name=\"axis\">T</att>\n"
            + "            <att name=\"long_name\">reference time of sst field</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>lat</sourceName>\n"
            + "        <destinationName>latitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Lat</att>\n"
            + "            <att name=\"axis\">Y</att>\n"
            + "            <att name=\"long_name\">latitude</att>\n"
            + "            <att name=\"standard_name\">latitude</att>\n"
            + "            <att name=\"units\">degrees_north</att>\n"
            + "            <att name=\"valid_max\" type=\"float\">90.0</att>\n"
            + "            <att name=\"valid_min\" type=\"float\">-90.0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Latitude</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>lon</sourceName>\n"
            + "        <destinationName>longitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Lon</att>\n"
            + "            <att name=\"axis\">X</att>\n"
            + "            <att name=\"long_name\">longitude</att>\n"
            + "            <att name=\"standard_name\">longitude</att>\n"
            + "            <att name=\"units\">degrees_east</att>\n"
            + "            <att name=\"valid_max\" type=\"float\">180.0</att>\n"
            + "            <att name=\"valid_min\" type=\"float\">-180.0</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Longitude</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>analysed_sst</sourceName>\n"
            + "        <destinationName>analysed_sst</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"double\">NaN</att>\n"
            + "            <att name=\"comment\">Interim near-real-time (nrt) version; to be replaced by Final version</att>\n"
            + "            <att name=\"coordinates\">time lat lon </att>\n"
            + "            <att name=\"long_name\">analysed sea surface temperature</att>\n"
            + "            <att name=\"standard_name\">sea_surface_foundation_temperature</att>\n"
            + "            <att name=\"units\">degree_K</att>\n"
            + "            <att name=\"valid_max\" type=\"double\">330.917</att>\n"
            + "            <att name=\"valid_min\" type=\"double\">265.383</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">305.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">273.0</att>\n"
            + "            <att name=\"coordinates\">null</att>\n"
            + "            <att name=\"ioos_category\">Temperature</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n\n";
    Test.ensureEqual(
        results,
        expected,
        "results.length="
            + results.length()
            + " expected.length="
            + expected.length()
            + "\nresults=\n"
            + results);

    // ensure it is ready-to-use by making a dataset from it
    String tDatasetID = suggDatasetID;
    EDD.deleteCachedDatasetInfo(tDatasetID);
    EDD edd = EDDGridFromNcFilesUnpacked.oneFromXmlFragment(null, results);
    Test.ensureEqual(edd.datasetID(), tDatasetID, "");
    Test.ensureEqual(edd.title(), "Daily MUR SST, Interim near-real-time (nrt) product", "");
    Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), "analysed_sst", "");

    String2.log("\nEDDGridFromNcFilesUnpacked.testGenerateDatasetsXml passed the test.");
  }

  /**
   * @throws Throwable if trouble
   */
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void testBasic(boolean deleteCachedDatasetInfo) throws Throwable {
    // String2.log("\n*** EDDGridFromNcFilesUnpacked.testBasic()\n");
    // testVerboseOn();
    int language = 0;
    String name, tName, results, tResults, expected, userDapQuery, tQuery;
    String error = "";
    EDV edv;
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);

    // generateDatasetsXml
    String id = "testEDDGridFromNcFilesUnpacked";
    if (deleteCachedDatasetInfo) EDDGridFromNcFilesUnpacked.deleteCachedDatasetInfo(id);
    EDDGrid eddGrid = (EDDGrid) EDDTestDataset.gettestEDDGridFromNcFilesUnpacked();

    // *** test getting das for entire dataset
    String2.log("\n*** test das dds for entire dataset\n");
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, "", EDStatic.fullTestCacheDirectory, eddGrid.className(), ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "Attributes {\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range 1.4440356e+9, 1.444122e+9;\n"
            + "    String axis \"T\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"reference time of sst field\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float32 actual_range 20.0006, 20.0995;\n"
            + "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "    Float32 valid_max 90.0;\n"
            + "    Float32 valid_min -90.0;\n"
            + "  }\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float32 actual_range -134.995, -134.896;\n"
            + "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "    Float32 valid_max 180.0;\n"
            + "    Float32 valid_min -180.0;\n"
            + "  }\n"
            + "  analysed_sst {\n"
            + "    Float64 _FillValue NaN;\n"
            + "    Float64 colorBarMaximum 305.0;\n"
            + "    Float64 colorBarMinimum 273.0;\n"
            + "    String comment \"Interim near-real-time (nrt) version; to be replaced by Final version\";\n"
            + "    String ioos_category \"Temperature\";\n"
            + "    String long_name \"analysed sea surface temperature\";\n"
            + "    String standard_name \"sea_surface_foundation_temperature\";\n"
            + "    String units \"degree_K\";\n"
            + "    Float64 valid_max 330.917;\n"
            + "    Float64 valid_min 265.383;\n"
            + "  }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_data_type \"Grid\";\n"
            + "    String comment \"Interim-MUR(nrt) will be replaced by MUR-Final in about 3 days; MUR = \\\"Multi-scale Ultra-high Resolution\\\"; produced under NASA MEaSUREs program.\";\n"
            + "    String contact \"ghrsst@podaac.jpl.nasa.gov\";\n"
            + "    String Conventions \"CF-1.6, COARDS, ACDD-1.3\";\n"
            + "    String creation_date \"2015-10-06\";\n"
            + "    String creator_email \"ghrsst@podaac.jpl.nasa.gov\";\n"
            + "    String creator_name \"GHRSST\";\n"
            + "    String creator_url \"https://podaac.jpl.nasa.gov/\";\n"
            + "    String DSD_entry_id \"JPL-L4UHfnd-GLOB-MUR\";\n"
            + "    Float64 Easternmost_Easting -134.896;\n"
            + "    String file_quality_index \"0\";\n"
            + "    String GDS_version_id \"GDS-v1.0-rev1.6\";\n"
            + "    Float64 geospatial_lat_max 20.0995;\n"
            + "    Float64 geospatial_lat_min 20.0006;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max -134.896;\n"
            + "    Float64 geospatial_lon_min -134.995;\n"
            + "    Float64 geospatial_lon_resolution 0.011000000000001996;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    String history \"Interim near-real-time (nrt) version created at nominal 1-day latency.\n"
            + today;
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "results=\n" + results);

    // T22:27:15Z (local files)
    // 2015-10-08T22:27:15Z

    expected =
        "/griddap/testEDDGridFromNcFilesUnpacked.das\";\n"
            + "    String infoUrl \"https://podaac.jpl.nasa.gov/\";\n"
            + "    String institution \"Jet Propulsion Laboratory\";\n"
            + "    String keywords \"analysed, analysed_sst, daily, data, day, earth, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, environments, foundation, high, interim, jet, laboratory, making, measures, multi, multi-scale, mur, near, near real time, near-real-time, nrt, ocean, oceans, product, propulsion, real, records, research, resolution, scale, sea, sea_surface_foundation_temperature, sst, surface, system, temperature, time, ultra, ultra-high, use\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    String netcdf_version_id \"3.5\";\n"
            + "    Float64 Northernmost_Northing 20.0995;\n"
            + "    String product_version \"04nrt\";\n"
            + "    String references \"ftp://mariana.jpl.nasa.gov/mur_sst/tmchin/docs/ATBD/\";\n"
            + "    String source_data \"AVHRR19_G-NAVO, AVHRR_METOP_A-EUMETSAT, MODIS_A-JPL, MODIS_T-JPL, WSAT-REMSS, iQUAM-NOAA/NESDIS, Ice_Conc-OSISAF\";\n"
            + "    String sourceUrl \"(local files)\";\n"
            + "    Float64 Southernmost_Northing 20.0006;\n"
            + "    String spatial_resolution \"0.011 degrees\";\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String summary \"Interim-Multi-scale Ultra-high Resolution (MUR)(nrt) will be replaced by MUR-Final in about 3 days; MUR = \\\"Multi-scale Ultra-high Resolution\\\"; produced under NASA Making Earth System Data Records for Use in Research Environments (MEaSUREs) program.\";\n"
            + "    String time_coverage_end \"2015-10-06T09:00:00Z\";\n"
            + "    String time_coverage_start \"2015-10-05T09:00:00Z\";\n"
            + "    String title \"Daily MUR SST, Interim near-real-time (nrt) product\";\n"
            + "    Float64 Westernmost_Easting -134.995;\n"
            + "  }\n"
            + "}\n";
    int tPo = results.indexOf(expected.substring(0, 17));
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    tResults = results.substring(tPo, Math.min(results.length(), tPo + expected.length()));
    Test.ensureEqual(tResults, expected, "results=\n" + results);

    // *** test getting dds for entire dataset
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, "", EDStatic.fullTestCacheDirectory, eddGrid.className(), ".dds");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + "  Float64 time[time = 2];\n"
            + "  Float32 latitude[latitude = 10];\n"
            + "  Float32 longitude[longitude = 10];\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float64 analysed_sst[time = 2][latitude = 10][longitude = 10];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 2];\n"
            + "      Float32 latitude[latitude = 10];\n"
            + "      Float32 longitude[longitude = 10];\n"
            + "  } analysed_sst;\n"
            + "} testEDDGridFromNcFilesUnpacked;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .csv with data from one file
    String2.log("\n*** test read from one file\n");
    userDapQuery = "analysed_sst[0][0:2:6][0:2:6]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            eddGrid.className(),
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "time,latitude,longitude,analysed_sst\n"
            + "UTC,degrees_north,degrees_east,degree_K\n"
            + "2015-10-05T09:00:00Z,20.0006,-134.995,299.929\n"
            + // note double values (Kelvin)
            "2015-10-05T09:00:00Z,20.0006,-134.973,299.952\n"
            + "2015-10-05T09:00:00Z,20.0006,-134.951,299.977\n"
            + "2015-10-05T09:00:00Z,20.0006,-134.929,300.001\n"
            + "2015-10-05T09:00:00Z,20.0226,-134.995,299.936\n"
            + "2015-10-05T09:00:00Z,20.0226,-134.973,299.955\n"
            + "2015-10-05T09:00:00Z,20.0226,-134.951,299.97799999999995\n"
            + "2015-10-05T09:00:00Z,20.0226,-134.929,300.001\n"
            + "2015-10-05T09:00:00Z,20.0446,-134.995,299.943\n"
            + "2015-10-05T09:00:00Z,20.0446,-134.973,299.95799999999997\n"
            + "2015-10-05T09:00:00Z,20.0446,-134.951,299.977\n"
            + "2015-10-05T09:00:00Z,20.0446,-134.929,299.996\n"
            + "2015-10-05T09:00:00Z,20.0665,-134.995,299.94599999999997\n"
            + "2015-10-05T09:00:00Z,20.0665,-134.973,299.95799999999997\n"
            + "2015-10-05T09:00:00Z,20.0665,-134.951,299.972\n"
            + "2015-10-05T09:00:00Z,20.0665,-134.929,299.986\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // */
  }

  /**
   * Test file created from
   * https://thredds.jpl.nasa.gov/thredds/ncss/grid/ncml_aggregation/OceanTemperature/modis/aqua/11um/9km/aggregate__MODIS_AQUA_L3_SST_THERMAL_8DAY_9KM_DAYTIME.ncml/dataset.html
   * and stored in /erddapTest/unsigned/
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagImageComparison
  void testUInt16File() throws Throwable {
    // String2.log("\n*** EDDGridFromNcFilesUnpacked.testUInt16File()");
    // testVerboseOn();
    int language = 0;
    String name, tName, results, tResults, expected, userDapQuery;
    String today = Calendar2.getCurrentISODateTimeStringZulu() + "Z";
    String fileDir =
        Path.of(EDDGridFromNcFilesUnpackedTests.class.getResource("/data/unsigned/").toURI())
                .toString()
            + "/";
    String fileName = "9km_aggregate__MODIS_AQUA_L3_SST_THERMAL_8DAY_9KM_DAYTIME.nc";
    boolean oNcDebugMode = NcHelper.debugMode;
    NcHelper.debugMode = true;
    boolean oAttDebugMode = Attributes.debugMode;
    Attributes.debugMode = true;
    String tDir = EDStatic.fullTestCacheDirectory;

    // DumpString
    results = NcHelper.ncdump(fileDir + fileName, "-h");
    expected =
        "netcdf 9km_aggregate__MODIS_AQUA_L3_SST_THERMAL_8DAY_9KM_DAYTIME.nc {\n"
            + "  dimensions:\n"
            + "    time = 1;\n"
            + "    lat = 2160;\n"
            + "    lon = 25;\n"
            + "  variables:\n"
            + "    short l3m_data(time=1, lat=2160, lon=25);\n"
            + "      :_Unsigned = \"true\";\n"
            + "      :long_name = \"l3m_data\";\n"
            + "      :scale_factor = 7.17185E-4f; // float\n"
            + // halfway=32768-> 23.50071808, so, many data values
            // are higher
            "      :add_offset = -2.0f; // float\n"
            + "      :_FillValue = -1S; // short\n"
            + // In a sense wrong: cf says it should be actual value:
            // 65535(ushort)
            "      :Scaling = \"linear\";\n"
            + "      :Scaling_Equation = \"(Slope*l3m_data) + Intercept = Parameter value\";\n"
            + "      :Slope = 7.17185E-4f; // float\n"
            + "      :Intercept = -2.0f; // float\n"
            + "      :coordinates = \"time Number_of_Lines Number_of_Columns lat lon\";\n"
            + "\n"
            + "    int time(time=1);\n"
            + "      :standard_name = \"time\";\n"
            + "      :axis = \"T\";\n"
            + "      :units = \"days since 2002-01-01\";\n"
            + "      :_CoordinateAxisType = \"Time\";\n"
            + "\n"
            + "    float Number_of_Lines(lat=2160);\n"
            + // note that ncss knows this is lat, but didn't rename it
            "      :long_name = \"latitude\";\n"
            + "      :units = \"degrees_north\";\n"
            + "      :_CoordinateAxisType = \"Lat\";\n"
            + "      :standard_name = \"latitude\";\n"
            + "\n"
            + "    float Number_of_Columns(lon=25);\n"
            + // note that ncss knows this is lon, but didn't rename it
            "      :long_name = \"longitude\";\n"
            + "      :units = \"degrees_east\";\n"
            + "      :_CoordinateAxisType = \"Lon\";\n"
            + "      :standard_name = \"longitude\";\n"
            + "\n"
            + "    byte l3m_qual(time=1, lat=2160, lon=25);\n"
            + "      :_Unsigned = \"true\";\n"
            + "      :long_name = \"l3m_qual\";\n"
            + "      :scale_factor = 7.17185E-4f; // float\n"
            + // I suspect that's wrong
            "      :add_offset = -2.0f; // float\n"
            + // I suspect that's wrong
            "      :valid_range = 0, 2; // int\n"
            + "      :coordinates = \"time Number_of_Lines Number_of_Columns lat lon\";\n"
            + // that's bizarre
            "\n"
            + "  // global attributes:\n"
            + "  :Product_Name = \"A20092652009272.L3m_8D_SST_9\";\n"
            + "  :Sensor_Name = \"MODISA\";\n"
            + "  :Sensor = \"\";\n"
            + "  :Title = \"MODISA Level-3 Standard Mapped Image\";\n"
            + "  :Data_Center = \"\";\n"
            + "  :Station_Name = \"\";\n"
            + "  :Station_Latitude = 0.0f; // float\n"
            + "  :Station_Longitude = 0.0f; // float\n"
            + "  :Mission = \"\";\n"
            + "  :Mission_Characteristics = \"\";\n"
            + "  :Sensor_Characteristics = \"\";\n"
            + "  :Product_Type = \"8-day\";\n"
            + "  :Replacement_Flag = \"ORIGINAL\";\n"
            + "  :Software_Name = \"smigen\";\n"
            + "  :Software_Version = \"4.0\";\n"
            + "  :Processing_Time = \"2009282201111000\";\n"
            + "  :Input_Files = \"A20092652009272.L3b_8D_SST.main\";\n"
            + "  :Processing_Control = \"smigen par=A20092652009272.L3m_8D_SST_9.param\";\n"
            + "  :Input_Parameters = \"IFILE = /data3/sdpsoper/vdc/vpu2/workbuf/A20092652009272.L3b_8D_SST.main|OFILE = A20092652009272.L3m_8D_SST_9|PFILE = |PROD = sst|PALFILE = DEFAULT|RFLAG = ORIGINAL|MEAS = 1|STYPE = 0|DATAMIN = 0.000000|DATAMAX = 0.000000|LONWEST = -180.000000|LONEAST = 180.000000|LATSOUTH = -90.000000|LATNORTH = 90.000000|RESOLUTION = 9km|PROJECTION = RECT|GAP_FILL = 0|SEAM_LON = -180.000000|PRECISION=I\";\n"
            + "  :L2_Flag_Names = \"LAND,HISOLZ\";\n"
            + "  :Period_Start_Year = 2009S; // short\n"
            + "  :Period_Start_Day = 265S; // short\n"
            + "  :Period_End_Year = 2009S; // short\n"
            + "  :Period_End_Day = 270S; // short\n"
            + "  :Start_Time = \"2009265000008779\";\n"
            + "  :End_Time = \"2009271030006395\";\n"
            + "  :Start_Year = 2009S; // short\n"
            + "  :Start_Day = 265S; // short\n"
            + "  :Start_Millisec = 8779; // int\n"
            + "  :End_Year = 2009S; // short\n"
            + "  :End_Day = 271S; // short\n"
            + "  :End_Millisec = 10806395; // int\n"
            + "  :Start_Orbit = 0; // int\n"
            + "  :End_Orbit = 0; // int\n"
            + "  :Orbit = 0; // int\n"
            + "  :Map_Projection = \"Equidistant Cylindrical\";\n"
            + "  :Latitude_Units = \"degrees North\";\n"
            + "  :Longitude_Units = \"degrees East\";\n"
            + "  :Northernmost_Latitude = 90.0f; // float\n"
            + "  :Southernmost_Latitude = -90.0f; // float\n"
            + "  :Westernmost_Longitude = -180.0f; // float\n"
            + "  :Easternmost_Longitude = 180.0f; // float\n"
            + "  :Latitude_Step = 0.083333336f; // float\n"
            + "  :Longitude_Step = 0.083333336f; // float\n"
            + "  :SW_Point_Latitude = -89.958336f; // float\n"
            + "  :SW_Point_Longitude = -179.95833f; // float\n"
            + "  :Data_Bins = 14234182; // int\n"
            + "  :Number_of_Lines = 2160; // int\n"
            + "  :Number_of_Columns = 4320; // int\n"
            + "  :Parameter = \"Sea Surface Temperature\";\n"
            + "  :Measure = \"Mean\";\n"
            + "  :Units = \"deg-C\";\n"
            + "  :Scaling = \"linear\";\n"
            + "  :Scaling_Equation = \"(Slope*l3m_data) + Intercept = Parameter value\";\n"
            + "  :Slope = 7.17185E-4f; // float\n"
            + "  :Intercept = -2.0f; // float\n"
            + "  :Scaled_Data_Minimum = -2.0f; // float\n"
            + "  :Scaled_Data_Maximum = 45.0f; // float\n"
            + "  :Data_Minimum = -1.999999f; // float\n"
            + "  :Data_Maximum = 36.915f; // float\n"
            + "  :start_date = \"2002-07-04 UTC\";\n"
            + "  :start_time = \"00:00:00 UTC\";\n"
            + "  :stop_date = \"2015-03-06 UTC\";\n"
            + "  :stop_time = \"23:59:59 UTC\";\n"
            + "  :Conventions = \"CF-1.0\";\n"
            + "  :History = \"Translated to CF-1.0 Conventions by Netcdf-Java CDM (NetcdfCFWriter)\n"
            + "Original Dataset = file:/usr/ftp/ncml/catalog_ncml/OceanTemperature/modis/aqua/11um/9km/aggregate__MODIS_AQUA_L3_SST_THERMAL_8DAY_9KM_DAYTIME.ncml; Translation Date = Fri Oct 30 09:44:07 GMT-08:00 2015\";\n"
            + "  :geospatial_lat_min = -89.95833587646484; // double\n"
            + "  :geospatial_lat_max = 89.95833587646484; // double\n"
            + "  :geospatial_lon_min = -136.04165649414062; // double\n"
            + "  :geospatial_lon_max = -134.04165649414062; // double\n"
            + "}\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    String suggDatasetID = EDDGridFromNcFilesUnpacked.suggestDatasetID(fileDir + fileName);

    // generateDatasetsXml
    results =
        EDDGridFromNcFilesUnpacked.generateDatasetsXml(
            fileDir, fileName, "", "", // group
            "", -1, "", null);
    expected =
        "<dataset type=\"EDDGridFromNcFilesUnpacked\" datasetID=\""
            + suggDatasetID
            + "\" active=\"true\">\n"
            + // same id as EDDGridFromNcFiles !!!
            "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n"
            + "    <updateEveryNMillis>10000</updateEveryNMillis>\n"
            + "    <fileDir>"
            + fileDir
            + "</fileDir>\n"
            + "    <fileNameRegex>9km_aggregate__MODIS_AQUA_L3_SST_THERMAL_8DAY_9KM_DAYTIME.nc</fileNameRegex>\n"
            + "    <recursive>true</recursive>\n"
            + "    <pathRegex>.*</pathRegex>\n"
            + "    <metadataFrom>last</metadataFrom>\n"
            + "    <matchAxisNDigits>20</matchAxisNDigits>\n"
            + "    <fileTableInMemory>false</fileTableInMemory>\n"
            + "    <!-- sourceAttributes>\n"
            + "        <att name=\"Conventions\">CF-1.0</att>\n"
            + "        <att name=\"Data_Bins\" type=\"int\">14234182</att>\n"
            + "        <att name=\"Data_Maximum\" type=\"float\">36.915</att>\n"
            + "        <att name=\"Data_Minimum\" type=\"float\">-1.999999</att>\n"
            + "        <att name=\"Easternmost_Longitude\" type=\"float\">180.0</att>\n"
            + "        <att name=\"End_Day\" type=\"short\">271</att>\n"
            + "        <att name=\"End_Millisec\" type=\"int\">10806395</att>\n"
            + "        <att name=\"End_Orbit\" type=\"int\">0</att>\n"
            + "        <att name=\"End_Time\">2009271030006395</att>\n"
            + "        <att name=\"End_Year\" type=\"short\">2009</att>\n"
            + "        <att name=\"geospatial_lat_max\" type=\"double\">89.95833587646484</att>\n"
            + "        <att name=\"geospatial_lat_min\" type=\"double\">-89.95833587646484</att>\n"
            + "        <att name=\"geospatial_lon_max\" type=\"double\">-134.04165649414062</att>\n"
            + "        <att name=\"geospatial_lon_min\" type=\"double\">-136.04165649414062</att>\n"
            + "        <att name=\"History\">Translated to CF-1.0 Conventions by Netcdf-Java CDM (NetcdfCFWriter)\n"
            + "Original Dataset = file:/usr/ftp/ncml/catalog_ncml/OceanTemperature/modis/aqua/11um/9km/aggregate__MODIS_AQUA_L3_SST_THERMAL_8DAY_9KM_DAYTIME.ncml; Translation Date = Fri Oct 30 09:44:07 GMT-08:00 2015</att>\n"
            + "        <att name=\"Input_Files\">A20092652009272.L3b_8D_SST.main</att>\n"
            + "        <att name=\"Input_Parameters\">IFILE = /data3/sdpsoper/vdc/vpu2/workbuf/A20092652009272.L3b_8D_SST.main|OFILE = A20092652009272.L3m_8D_SST_9|PFILE = |PROD = sst|PALFILE = DEFAULT|RFLAG = ORIGINAL|MEAS = 1|STYPE = 0|DATAMIN = 0.000000|DATAMAX = 0.000000|LONWEST = -180.000000|LONEAST = 180.000000|LATSOUTH = -90.000000|LATNORTH = 90.000000|RESOLUTION = 9km|PROJECTION = RECT|GAP_FILL = 0|SEAM_LON = -180.000000|PRECISION=I</att>\n"
            + "        <att name=\"Intercept\" type=\"float\">-2.0</att>\n"
            + "        <att name=\"L2_Flag_Names\">LAND,HISOLZ</att>\n"
            + "        <att name=\"Latitude_Step\" type=\"float\">0.083333336</att>\n"
            + "        <att name=\"Latitude_Units\">degrees North</att>\n"
            + "        <att name=\"Longitude_Step\" type=\"float\">0.083333336</att>\n"
            + "        <att name=\"Longitude_Units\">degrees East</att>\n"
            + "        <att name=\"Map_Projection\">Equidistant Cylindrical</att>\n"
            + "        <att name=\"Measure\">Mean</att>\n"
            + "        <att name=\"Northernmost_Latitude\" type=\"float\">90.0</att>\n"
            + "        <att name=\"Number_of_Columns\" type=\"int\">4320</att>\n"
            + "        <att name=\"Number_of_Lines\" type=\"int\">2160</att>\n"
            + "        <att name=\"Orbit\" type=\"int\">0</att>\n"
            + "        <att name=\"Parameter\">Sea Surface Temperature</att>\n"
            + "        <att name=\"Period_End_Day\" type=\"short\">270</att>\n"
            + "        <att name=\"Period_End_Year\" type=\"short\">2009</att>\n"
            + "        <att name=\"Period_Start_Day\" type=\"short\">265</att>\n"
            + "        <att name=\"Period_Start_Year\" type=\"short\">2009</att>\n"
            + "        <att name=\"Processing_Control\">smigen par=A20092652009272.L3m_8D_SST_9.param</att>\n"
            + "        <att name=\"Processing_Time\">2009282201111000</att>\n"
            + "        <att name=\"Product_Name\">A20092652009272.L3m_8D_SST_9</att>\n"
            + "        <att name=\"Product_Type\">8-day</att>\n"
            + "        <att name=\"Replacement_Flag\">ORIGINAL</att>\n"
            + "        <att name=\"Scaled_Data_Maximum\" type=\"float\">45.0</att>\n"
            + "        <att name=\"Scaled_Data_Minimum\" type=\"float\">-2.0</att>\n"
            + "        <att name=\"Scaling\">linear</att>\n"
            + "        <att name=\"Scaling_Equation\">(Slope*l3m_data) + Intercept = Parameter value</att>\n"
            + "        <att name=\"Sensor_Name\">MODISA</att>\n"
            + "        <att name=\"Slope\" type=\"float\">7.17185E-4</att>\n"
            + "        <att name=\"Software_Name\">smigen</att>\n"
            + "        <att name=\"Software_Version\">4.0</att>\n"
            + "        <att name=\"Southernmost_Latitude\" type=\"float\">-90.0</att>\n"
            + "        <att name=\"start_date\">2002-07-04 UTC</att>\n"
            + "        <att name=\"Start_Day\" type=\"short\">265</att>\n"
            + "        <att name=\"Start_Millisec\" type=\"int\">8779</att>\n"
            + "        <att name=\"Start_Orbit\" type=\"int\">0</att>\n"
            + "        <att name=\"Start_Time\">2009265000008779</att>\n"
            + "        <att name=\"start_time\">00:00:00 UTC</att>\n"
            + "        <att name=\"Start_Year\" type=\"short\">2009</att>\n"
            + "        <att name=\"Station_Latitude\" type=\"float\">0.0</att>\n"
            + "        <att name=\"Station_Longitude\" type=\"float\">0.0</att>\n"
            + "        <att name=\"stop_date\">2015-03-06 UTC</att>\n"
            + "        <att name=\"stop_time\">23:59:59 UTC</att>\n"
            + "        <att name=\"SW_Point_Latitude\" type=\"float\">-89.958336</att>\n"
            + "        <att name=\"SW_Point_Longitude\" type=\"float\">-179.95833</att>\n"
            + "        <att name=\"Title\">MODISA Level-3 Standard Mapped Image</att>\n"
            + "        <att name=\"Units\">deg-C</att>\n"
            + "        <att name=\"Westernmost_Longitude\" type=\"float\">-180.0</att>\n"
            + "    </sourceAttributes -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cdm_data_type\">Grid</att>\n"
            + "        <att name=\"Conventions\">CF-1.10, COARDS, ACDD-1.3</att>\n"
            + "        <att name=\"Data_Bins\">null</att>\n"
            + "        <att name=\"Data_Maximum\">null</att>\n"
            + "        <att name=\"Data_Minimum\">null</att>\n"
            + "        <att name=\"Easternmost_Longitude\">null</att>\n"
            + "        <att name=\"End_Day\">null</att>\n"
            + "        <att name=\"End_Millisec\">null</att>\n"
            + "        <att name=\"End_Orbit\">null</att>\n"
            + "        <att name=\"End_Time\">null</att>\n"
            + "        <att name=\"End_Year\">null</att>\n"
            + "        <att name=\"History\">null</att>\n"
            + "        <att name=\"history\">Translated to CF-1.0 Conventions by Netcdf-Java CDM (NetcdfCFWriter)\n"
            + "Original Dataset = file:/usr/ftp/ncml/catalog_ncml/OceanTemperature/modis/aqua/11um/9km/aggregate__MODIS_AQUA_L3_SST_THERMAL_8DAY_9KM_DAYTIME.ncml; Translation Date = Fri Oct 30 09:44:07 GMT-08:00 2015</att>\n"
            + "        <att name=\"infoUrl\">???</att>\n"
            + "        <att name=\"Input_Files\">null</att>\n"
            + "        <att name=\"institution\">???</att>\n"
            + "        <att name=\"Intercept\">null</att>\n"
            + "        <att name=\"keywords\">aqua, data, earth, Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature, image, imaging, L3, l3m_data, l3m_qual, mapped, moderate, modis, modisa, ocean, oceans, quality, resolution, science, sea, sea_surface_temperature, smi, spectroradiometer, standard, surface, temperature, time</att>\n"
            + "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n"
            + "        <att name=\"Latitude_Step\">null</att>\n"
            + "        <att name=\"Latitude_Units\">null</att>\n"
            + "        <att name=\"license\">[standard]</att>\n"
            + "        <att name=\"Longitude_Step\">null</att>\n"
            + "        <att name=\"Longitude_Units\">null</att>\n"
            + "        <att name=\"Northernmost_Latitude\">null</att>\n"
            + "        <att name=\"Number_of_Columns\">null</att>\n"
            + "        <att name=\"Number_of_Lines\">null</att>\n"
            + "        <att name=\"Orbit\">null</att>\n"
            + "        <att name=\"Parameter\">null</att>\n"
            + "        <att name=\"Period_End_Day\">null</att>\n"
            + "        <att name=\"Period_End_Year\">null</att>\n"
            + "        <att name=\"Period_Start_Day\">null</att>\n"
            + "        <att name=\"Period_Start_Year\">null</att>\n"
            + "        <att name=\"Scaling\">null</att>\n"
            + "        <att name=\"Scaling_Equation\">null</att>\n"
            + "        <att name=\"Slope\">null</att>\n"
            + "        <att name=\"Southernmost_Latitude\">null</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"start_date\">null</att>\n"
            + "        <att name=\"Start_Day\">null</att>\n"
            + "        <att name=\"Start_Millisec\">null</att>\n"
            + "        <att name=\"Start_Orbit\">null</att>\n"
            + "        <att name=\"Start_Time\">null</att>\n"
            + "        <att name=\"start_time\">null</att>\n"
            + "        <att name=\"Start_Year\">null</att>\n"
            + "        <att name=\"Station_Latitude\">null</att>\n"
            + "        <att name=\"Station_Longitude\">null</att>\n"
            + "        <att name=\"stop_date\">null</att>\n"
            + "        <att name=\"stop_time\">null</att>\n"
            + "        <att name=\"summary\">Moderate Resolution Imaging Spectroradiometer on Aqua (MODISA) Level-3 Standard Mapped Image</att>\n"
            + "        <att name=\"SW_Point_Latitude\">null</att>\n"
            + "        <att name=\"SW_Point_Longitude\">null</att>\n"
            + "        <att name=\"Title\">null</att>\n"
            + "        <att name=\"title\">MODISA L3 SMI,</att>\n"
            + "        <att name=\"Units\">null</att>\n"
            + "        <att name=\"Westernmost_Longitude\">null</att>\n"
            + "    </addAttributes>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>time</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_CoordinateAxisType\">Time</att>\n"
            + "            <att name=\"axis\">T</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"long_name\">Time</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>lat</sourceName>\n"
            + // correct based on what is in the file, but needs to be
            // Number_of_Lines
            "        <destinationName>latitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Latitude</att>\n"
            + "            <att name=\"standard_name\">latitude</att>\n"
            + "            <att name=\"units\">degrees_north</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>lon</sourceName>\n"
            + // correct based on what is in the file, but needs to be
            // Number_of_Columns
            "        <destinationName>longitude</destinationName>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Location</att>\n"
            + "            <att name=\"long_name\">Longitude</att>\n"
            + "            <att name=\"standard_name\">longitude</att>\n"
            + "            <att name=\"units\">degrees_east</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>l3m_data</sourceName>\n"
            + "        <destinationName>sst</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">NaN</att>\n"
            + // a result of unpacking
            "            <att name=\"coordinates\">time Number_of_Lines Number_of_Columns lat lon</att>\n"
            +
            // " <att name=\"Intercept\" type=\"float\">-2.0</att>\n" + //this is removed by
            // the unpacking process
            "            <att name=\"long_name\">l3m_data</att>\n"
            +
            // " <att name=\"Scaling\">linear</att>\n" + //this is removed by the unpacking
            // process
            // " <att name=\"Scaling_Equation\">(Slope*l3m_data) + Intercept = Parameter
            // value</att>\n" + //this is removed by the unpacking process
            // " <att name=\"Slope\" type=\"float\">7.17185E-4</att>\n" + //this is removed
            // by the unpacking process
            // scale_factor and add_offset are also removed by the unpacking process
            "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"coordinates\">null</att>\n"
            + "            <att name=\"ioos_category\">Temperature</att>\n"
            + "            <att name=\"long_name\">Sea Surface Temperature</att>\n"
            + "            <att name=\"standard_name\">sea_surface_temperature</att>\n"
            + "            <att name=\"units\">deg_C</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>l3m_qual</sourceName>\n"
            + "        <destinationName>sst_quality</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"coordinates\">time Number_of_Lines Number_of_Columns lat lon</att>\n"
            + "            <att name=\"long_name\">l3m_qual</att>\n"
            + "            <att name=\"valid_range\" type=\"floatList\">-2.0 -1.9985657</att>\n"
            + // trouble:
            // incorrect files
            // -> crazy result
            // of erroneous
            // scale_factor and
            // add_offset
            "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"coordinates\">null</att>\n"
            + "            <att name=\"ioos_category\">Quality</att>\n"
            + "            <att name=\"long_name\">Sea Surface Temperature Quality</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // ensure files are reread
    // NOTE that testUInt16FileUnpacked uses
    // Number_of_Lines (the 1D var which uses lat dimension) and
    // Number_of_Columns (the 1D var which uses lon dimension)
    File2.deleteAllFiles(EDDGridFromNcFilesUnpacked.datasetDir("testUInt16FileUnpacked"));
    EDDGrid eddGrid = (EDDGrid) EDDTestDataset.gettestUInt16FileUnpacked();
    // in uaf erddap, this is nasa_jpl_c688_be2f_cf9d

    // re-pack apparent missing value
    // 45.000717 +2=> 47.000717 /7.17185E-4=> 65535

    // .das das isn't affected by userDapQuery
    tName =
        eddGrid.makeNewFileForDapQuery(language, null, null, "", tDir, eddGrid.className(), ".das");
    results = File2.readFromFile88591(tDir + tName)[1];
    expected =
        "Attributes {\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range 1.0257408e+9, 1.0257408e+9;\n"
            + "    String axis \"T\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float32 actual_range -89.95834, 89.95834;\n"
            + // a test of descending lat axis
            "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float32 actual_range -136.0417, -134.0417;\n"
            + "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  sst {\n"
            + "    Float32 _FillValue NaN;\n"
            + // important test of UInt16 and Unpacked
            "    Float64 colorBarMaximum 32.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Temperature\";\n"
            + "    String long_name \"Sea Surface Temperature\";\n"
            + "    String standard_name \"sea_surface_temperature\";\n"
            + "    String units \"deg_C\";\n"
            + "  }\n"
            + "  sst_quality {\n"
            + "    Float64 colorBarMaximum 150.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Quality\";\n"
            + "    String long_name \"Sea Surface Temperature Quality\";\n"
            + "    Float32 valid_range -2.0, -1.9985657;\n"
            + // unpacking did that
            "  }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_data_type \"Grid\";\n"
            + "    String Conventions \"CF-1.6, COARDS, ACDD-1.3\";\n"
            + "    Float64 Easternmost_Easting -134.0417;\n"
            + "    Float64 geospatial_lat_max 89.95834;\n"
            + "    Float64 geospatial_lat_min -89.95834;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max -134.0417;\n"
            + "    Float64 geospatial_lon_min -136.0417;\n"
            + "    Float64 geospatial_lon_resolution 0.08333333333333333;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    String history \"Translated to CF-1.0 Conventions by Netcdf-Java CDM (NetcdfCFWriter)\n"
            + "Original Dataset = file:/usr/ftp/ncml/catalog_ncml/OceanTemperature/modis/aqua/11um/9km/aggregate__MODIS_AQUA_L3_SST_THERMAL_8DAY_9KM_DAYTIME.ncml; Translation Date = Fri Oct 30 09:44:07 GMT-08:00 2015\n";
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

    expected =
        // "2015-10-30T18:17:10Z (local files)
        // 2015-10-30T18:17:10Z
        // http://localhost:8080/cwexperimental/griddap/testUInt16File.das";
        "    String infoUrl \"???\";\n"
            + "    String Input_Parameters \"IFILE = /data3/sdpsoper/vdc/vpu2/workbuf/A20092652009272.L3b_8D_SST.main|OFILE = A20092652009272.L3m_8D_SST_9|PFILE = |PROD = sst|PALFILE = DEFAULT|RFLAG = ORIGINAL|MEAS = 1|STYPE = 0|DATAMIN = 0.000000|DATAMAX = 0.000000|LONWEST = -180.000000|LONEAST = 180.000000|LATSOUTH = -90.000000|LATNORTH = 90.000000|RESOLUTION = 9km|PROJECTION = RECT|GAP_FILL = 0|SEAM_LON = -180.000000|PRECISION=I\";\n"
            + "    String institution \"???\";\n"
            + "    String keywords \"aqua, data, earth, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, image, imaging, L3, l3m_data, l3m_qual, mapped, moderate, modis, modisa, ocean, oceans, quality, resolution, science, sea, sea_surface_temperature, smi, spectroradiometer, standard, surface, temperature, time\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String L2_Flag_Names \"LAND,HISOLZ\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    String Map_Projection \"Equidistant Cylindrical\";\n"
            + "    String Measure \"Mean\";\n"
            + "    Float64 Northernmost_Northing 89.95834;\n"
            + "    String Processing_Control \"smigen par=A20092652009272.L3m_8D_SST_9.param\";\n"
            + "    String Processing_Time \"2009282201111000\";\n"
            + "    String Product_Name \"A20092652009272.L3m_8D_SST_9\";\n"
            + "    String Product_Type \"8-day\";\n"
            + "    String Replacement_Flag \"ORIGINAL\";\n"
            + "    Float32 Scaled_Data_Maximum 45.0;\n"
            + "    Float32 Scaled_Data_Minimum -2.0;\n"
            + "    String Sensor_Name \"MODISA\";\n"
            + "    String Software_Name \"smigen\";\n"
            + "    String Software_Version \"4.0\";\n"
            + "    String sourceUrl \"(local files)\";\n"
            + "    Float64 Southernmost_Northing -89.95834;\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String summary \"Moderate Resolution Imaging Spectroradiometer on Aqua (MODISA) Level-3 Standard Mapped Image\";\n"
            + "    String time_coverage_end \"2002-07-04T00:00:00Z\";\n"
            + "    String time_coverage_start \"2002-07-04T00:00:00Z\";\n"
            + "    String title \"MODISA L3 SMI,\";\n"
            + "    Float64 Westernmost_Easting -136.0417;\n"
            + "  }\n"
            + "}\n";
    int tpo = results.indexOf(expected.substring(0, 17));
    Test.ensureTrue(tpo >= 0, "tpo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tpo, Math.min(results.length(), tpo + expected.length())),
        expected,
        "results=\n" + results);

    // .dds dds isn't affected by userDapQuery
    tName =
        eddGrid.makeNewFileForDapQuery(language, null, null, "", tDir, eddGrid.className(), ".dds");
    results = File2.directReadFrom88591File(tDir + tName);
    expected = // difference from testUInt16Dap: lat lon are float here, not double
        "Dataset {\n"
            + "  Float64 time[time = 1];\n"
            + "  Float32 latitude[latitude = 2160];\n"
            + "  Float32 longitude[longitude = 25];\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 sst[time = 1][latitude = 2160][longitude = 25];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 1];\n"
            + "      Float32 latitude[latitude = 2160];\n"
            + "      Float32 longitude[longitude = 25];\n"
            + "  } sst;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 sst_quality[time = 1][latitude = 2160][longitude = 25];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 1];\n"
            + "      Float32 latitude[latitude = 2160];\n"
            + "      Float32 longitude[longitude = 25];\n"
            + "  } sst_quality;\n"
            + "} testUInt16FileUnpacked;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .csv data values
    userDapQuery =
        "sst[0][0:100:2159][(-134.95833513)],sst_quality[0][0:100:2159][(-134.95833513)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, tDir, eddGrid.className(), ".csv");
    results = File2.directReadFrom88591File(tDir + tName);
    String2.log(results);
    expected = // difference from testUInt16Dap: lat lon are float here, not double
        "time,latitude,longitude,sst,sst_quality\n"
            + "UTC,degrees_north,degrees_east,deg_C,\n"
            + // quality values should be 0, 1, 2, 3, 4, 5
            "2002-07-04T00:00:00Z,89.958336,-134.95833,-0.84102905,-1.9992828\n"
            + // quality values are
            // wrong because file
            // incorrectly has
            // scale_factor,
            // add_offset for them
            "2002-07-04T00:00:00Z,81.62501,-134.95833,-1.6371044,-2.0\n"
            + "2002-07-04T00:00:00Z,73.291664,-134.95833,-0.11021753,-2.0\n"
            + "2002-07-04T00:00:00Z,64.958336,-134.95833,NaN,-1.8171178\n"
            + // NaN shows _FillValue=-1S was
            // correctly caught
            "2002-07-04T00:00:00Z,56.625008,-134.95833,NaN,-1.8171178\n"
            + "2002-07-04T00:00:00Z,48.291664,-134.95833,12.6406145,-2.0\n"
            + "2002-07-04T00:00:00Z,39.958336,-134.95833,17.95137,-2.0\n"
            + "2002-07-04T00:00:00Z,31.625,-134.95833,20.432829,-2.0\n"
            + "2002-07-04T00:00:00Z,23.291664,-134.95833,19.664007,-1.9985657\n"
            + "2002-07-04T00:00:00Z,14.958336,-134.95833,24.482773,-2.0\n"
            + // sst>23.5 shows unsigned values
            // correctly caught
            "2002-07-04T00:00:00Z,6.625,-134.95833,29.068455,-2.0\n"
            + "2002-07-04T00:00:00Z,-1.7083359,-134.95833,27.240349,-2.0\n"
            + "2002-07-04T00:00:00Z,-10.041664,-134.95833,27.210228,-2.0\n"
            + "2002-07-04T00:00:00Z,-18.375,-134.95833,26.713936,-2.0\n"
            + "2002-07-04T00:00:00Z,-26.708336,-134.95833,21.580326,-2.0\n"
            + "2002-07-04T00:00:00Z,-35.041668,-134.95833,15.789774,-2.0\n"
            + "2002-07-04T00:00:00Z,-43.375,-134.95833,NaN,-1.8171178\n"
            + "2002-07-04T00:00:00Z,-51.708336,-134.95833,6.1673026,-1.9985657\n"
            + "2002-07-04T00:00:00Z,-60.041668,-134.95833,0.40400413,-2.0\n"
            + "2002-07-04T00:00:00Z,-68.375,-134.95833,NaN,-1.8171178\n"
            + "2002-07-04T00:00:00Z,-76.708336,-134.95833,NaN,-1.8171178\n"
            + "2002-07-04T00:00:00Z,-85.04167,-134.95833,NaN,-1.8171178\n";
    Test.ensureEqual(
        results, expected, "\nresults=\n" + results + "\nFIX THIS problem with unsigned values.");

    // display the image
    String2.log("\n\n* PNG ");
    String baseName = eddGrid.className() + "_UInt16_Map";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            "sst[0][][]&.land=under",
            Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR),
            baseName,
            ".png");
    // Test.displayInBrowser("file://" + tDir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    NcHelper.debugMode = oNcDebugMode;
    Attributes.debugMode = oAttDebugMode;
  }

  /**
   * Test file from Yibo Jiang (PODAAC) and stored in /erddapTest/nc/ .
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  void testSuperPreciseTimeUnits() throws Throwable {
    // String2.log("\n*** EDDGridFromNcFilesUnpacked.testSuperPreciseTimeUnits");
    // testVerboseOn();
    int language = 0;
    String name, tName, results, tResults, expected, userDapQuery;
    String fileDir =
        Path.of(EDDGridFromNcFilesUnpackedTests.class.getResource("/largeFiles/nc/").toURI())
                .toString()
            + "/";
    String tDir = EDStatic.fullTestCacheDirectory;
    boolean oDebugMode = NcHelper.debugMode;
    NcHelper.debugMode = true;

    // ncdump of file showing time units and values
    results = NcHelper.ncdump(fileDir + "superPreciseTimeUnits.nc", "-v time");
    expected =
        "netcdf superPreciseTimeUnits.nc {\n"
            + "  dimensions:\n"
            + "    time = 24;\n"
            + "    lat = 400;\n"
            + "    lon = 1800;\n"
            + "  variables:\n"
            + "    byte time(time=24);\n"
            + "      :long_name = \"Reference time of file\";\n"
            + "      :standard_name = \"time\";\n"
            + "      :calendar = \"gregorian\";\n"
            + "      :comment = \"Timestamp coordinate at the center of the 1 hr bin, at 1 hour resolution. Range is one UTC day.\";\n"
            + "      :units = \"hours since 2020-01-01 00:30:00.000000000\";\n"
            + "      :_ChunkSizes = 24U; // uint\n"
            + "\n"
            + "    int epoch_time(time=24);\n"
            + "      :long_name = \"Time Centering of Data Based on Epoch Reference\";\n"
            + "      :standard_name = \"time\";\n"
            + "      :calendar = \"gregorian\";\n"
            + "      :comment = \"Timestamp coordinate is at the center of the 1 hr bin, at 1 hour resolution referenced by the historical Epoch reference date/time. The Epoch reference date/time corresponds to the first observation time window in the CYGNSS historical data record. Total number of timestamps in a file corresponds to one UTC day. This value is rounded to the nearest hour since leap seconds may have occured making the number of hours since the start of the mission not exact.\";\n"
            + "      :units = \"hours since 2017-03-18 00:30:00.000000000\";\n"
            + "      :_ChunkSizes = 24U; // uint\n"
            + "\n"
            + "    float lat(lat=400);\n"
            + "      :long_name = \"Latitude\";\n"
            + "      :standard_name = \"latitude\";\n"
            + "      :units = \"degrees_north\";\n"
            + "      :comment = \"Latitude coordinate at the center of the 0.2 degree bin, degrees_north, at 0.2 degree resolution. Range is -39.9 .. 39.9.\";\n"
            + "      :_ChunkSizes = 400U; // uint\n"
            + "\n"
            + "    float lon(lon=1800);\n"
            + "      :long_name = \"Longitude\";\n"
            + "      :standard_name = \"longitude\";\n"
            + "      :units = \"degrees_east\";\n"
            + "      :comment = \"Longitude coordinate at the center of the 0.2 degree bin, degrees_east, at 0.2 degree resolution. Range is 0.1 .. 359.9.\";\n"
            + "      :_ChunkSizes = 1800U; // uint\n"
            + "\n"
            + "    float wind_speed(time=24, lat=400, lon=1800);\n"
            + "      :long_name = \"Wind speed\";\n"
            + "      :standard_name = \"wind_speed\";\n"
            + "      :units = \"m s-1\";\n"
            + "      :_FillValue = -9999.0f; // float\n"
            + "      :valid_range = -5.0, 100.0; // double\n"
            + "      :comment = \"Minimum variance estimate of the mean wind speed in the bin over the spatial and temporal intervals specified by the bin's boundaries. This is done using an inverse-variance weighted average of all L2 samples of the wind speed that were made within the bin.\";\n"
            + "      :_ChunkSizes = 8U, 134U, 600U; // uint\n"
            + "\n"
            + "    float wind_speed_uncertainty(time=24, lat=400, lon=1800);\n"
            + "      :long_name = \"Wind speed uncertainty\";\n"
            + "      :units = \"m s-1\";\n"
            + "      :_FillValue = -9999.0f; // float\n"
            + "      :valid_range = 0.0, 10.0; // double\n"
            + "      :comment = \"Standard deviation of the error in the mean of all L2 samples of the wind speed within the bin.\";\n"
            + "      :_ChunkSizes = 8U, 134U, 600U; // uint\n"
            + "\n"
            + "    int num_wind_speed_samples(time=24, lat=400, lon=1800);\n"
            + "      :long_name = \"Number of wind speed samples\";\n"
            + "      :units = \"1\";\n"
            + "      :_FillValue = -99; // int\n"
            + "      :valid_range = 1, 100000; // int\n"
            + "      :comment = \"The number of L2 wind speed samples used to calculate wind_speed.\";\n"
            + "      :_ChunkSizes = 8U, 134U, 600U; // uint\n"
            + "\n"
            + "    float yslf_wind_speed(time=24, lat=400, lon=1800);\n"
            + "      :long_name = \"Young sea limited fetch wind speed\";\n"
            + "      :standard_name = \"yslf_wind_speed\";\n"
            + "      :units = \"m s-1\";\n"
            + "      :_FillValue = -9999.0f; // float\n"
            + "      :valid_range = -5.0, 100.0; // double\n"
            + "      :comment = \"Minimum variance estimate of the young sea limited fetch mean wind speed in the bin over the spatial and temporal intervals specified by the bin's boundaries. This is done using an inverse-variance weighted average of all L2 samples of the wind speed that were made within the bin.\";\n"
            + "      :_ChunkSizes = 8U, 134U, 600U; // uint\n"
            + "\n"
            + "    float yslf_wind_speed_uncertainty(time=24, lat=400, lon=1800);\n"
            + "      :long_name = \"Young sea limited fetch wind speed uncertainty\";\n"
            + "      :_FillValue = -9999.0f; // float\n"
            + "      :valid_range = 0.0, 10.0; // double\n"
            + "      :comment = \"Standard deviation of the error in the mean of all L2 samples of the young sea limited fetch wind speed within the bin.\";\n"
            + "      :units = \"m s-1\";\n"
            + "      :_ChunkSizes = 8U, 134U, 600U; // uint\n"
            + "\n"
            + "    int num_yslf_wind_speed_samples(time=24, lat=400, lon=1800);\n"
            + "      :long_name = \"Number of young sea limited fetch wind speed samples\";\n"
            + "      :units = \"1\";\n"
            + "      :_FillValue = -99; // int\n"
            + "      :valid_range = 1, 100000; // int\n"
            + "      :comment = \"The number of L2 young sea limited fetch wind speed samples used to calculate yslf_wind_speed.\";\n"
            + "      :_ChunkSizes = 8U, 134U, 600U; // uint\n"
            + "\n"
            + "    float mean_square_slope(time=24, lat=400, lon=1800);\n"
            + "      :long_name = \"Mean square slope\";\n"
            + "      :units = \"1\";\n"
            + "      :_FillValue = -9999.0f; // float\n"
            + "      :valid_range = 0.0, 0.04; // double\n"
            + "      :comment = \"Mean MSS in the bin over the spatial and temporal intervals specified by the bin's boundaries.\";\n"
            + "      :_ChunkSizes = 8U, 134U, 600U; // uint\n"
            + "\n"
            + "    float mean_square_slope_uncertainty(time=24, lat=400, lon=1800);\n"
            + "      :long_name = \"Mean square slope uncertainty\";\n"
            + "      :units = \"1\";\n"
            + "      :_FillValue = -9999.0f; // float\n"
            + "      :valid_range = 0.0, 0.08; // double\n"
            + "      :comment = \"Standard deviation of the error in the mean of all L2 samples of the MSS within the bin.\";\n"
            + "      :_ChunkSizes = 8U, 134U, 600U; // uint\n"
            + "\n"
            + "    int num_mss_samples(time=24, lat=400, lon=1800);\n"
            + "      :long_name = \"Number of mean square slope samples\";\n"
            + "      :units = \"1\";\n"
            + "      :_FillValue = -99; // int\n"
            + "      :valid_range = 1, 100000; // int\n"
            + "      :comment = \"The number of L2 MSS samples used to calculate mean_square_slope.\";\n"
            + "      :_ChunkSizes = 8U, 134U, 600U; // uint\n"
            + "\n"
            + "    float merra2_wind_speed(time=24, lat=400, lon=1800);\n"
            + "      :long_name = \"MERRA-2 reference wind speed\";\n"
            + "      :units = \"m s-1\";\n"
            + "      :_FillValue = -9999.0f; // float\n"
            + "      :valid_range = 0.0, 100.0; // double\n"
            + "      :comment = \"Mean MERRA-2 wind speed in the bin over the spatial and temporal intervals specified by the bin's boundaries. See https://disc.gsfc.nasa.gov/datasets/M2I1NXASM_5.12.4/summary?keywords=%22MERRA-2%22\";\n"
            + "      :_ChunkSizes = 8U, 134U, 600U; // uint\n"
            + "\n"
            + "    int num_merra2_wind_speed_samples(time=24, lat=400, lon=1800);\n"
            + "      :units = \"1\";\n"
            + "      :_FillValue = -99; // int\n"
            + "      :valid_range = 1, 100000; // int\n"
            + "      :comment = \"The number of L2 MERRA-2 wind speed samples used to calculate merra2_wind_speed.\";\n"
            + "      :long_name = \"Number of MERRA-2 wind speed samples\";\n"
            + "      :_ChunkSizes = 8U, 134U, 600U; // uint\n"
            + "\n"
            + "  // global attributes:\n"
            + "  :Conventions = \"CF-1.6, ACDD-1.3, ISO-8601\";\n"
            + "  :standard_name_vocabulary = \"CF Standard Name Table v30\";\n"
            + "  :project = \"CYGNSS\";\n"
            + "  :summary = \"CYGNSS is a NASA Earth Venture mission, managed by the Earth System Science Pathfinder Program. The mission consists of a constellation of eight small satellites. The eight observatories comprise a constellation that measures the ocean surface wind field with very high temporal resolution and spatial coverage, under all precipitating conditions, and over the full dynamic range of wind speeds experienced in a tropical cyclone. The CYGNSS observatories fly in 510 km circular orbits at a common inclination of 35°. Each observatory includes a Delay Doppler Mapping Instrument (DDMI) consisting of a modified GPS receiver capable of measuring surface scattering, a low gain zenith antenna for measurement of the direct GPS signal, and two high gain nadir antennas for measurement of the weaker scattered signal. Each DDMI is capable of measuring 4 simultaneous bi-static reflections, resulting in a total of 32 wind measurements per second by the full constellation.\";\n"
            + "  :program = \"CYGNSS\";\n"
            + "  :references = \"Ruf, C., P. Chang, M.P. Clarizia, S. Gleason, Z. Jelenak, J. Murray, M. Morris, S. Musko, D. Posselt, D. Provost, D. Starkenburg, V. Zavorotny, CYGNSS Handbook, Ann Arbor, MI, Michigan Pub., ISBN 978-1-60785-380-0, 154 pp, 1 Apr 2016. http://clasp-research.engin.umich.edu/missions/cygnss/reference/cygnss-mission/CYGNSS_Handbook_April2016.pdf\n"
            + "Global Modeling and Assimilation Office (GMAO) (2015), MERRA-2 inst1_2d_asm_Nx: 2d,1-Hourly,Instantaneous,Single-Level,Assimilation,Single-Level Diagnostics V5.12.4, Greenbelt, MD, USA, Goddard Earth Sciences Data and Information Services Center (GES DISC), Accessed: {dates differ for each L1 file. See 'source' L1 files for exact timestamps}, https://doi.org/10.5067/3Z173KIE2TPD\";\n"
            + "  :processing_level = \"3\";\n"
            + "  :comment = \"This Level 3 gridded product combines all 8 x 4 = 32 wind speed and mean square slope (MSS) measurements made by the CYGNSS constellation each second, uniformly sampled in latitude, longitude and time.\";\n"
            + "  :creator_type = \"institution\";\n"
            + "  :institution = \"University of Michigan Space Physics Research Lab (SPRL)\";\n"
            + "  :creator_name = \"CYGNSS Science Operations Center\";\n"
            + "  :publisher_name = \"PO.DAAC\";\n"
            + "  :publisher_email = \"podaac@podaac.jpl.nasa.gov\";\n"
            + "  :publisher_url = \"\u200Bhttp://podaac.jpl.nasa.gov\";\n"
            + // ! zero width space #8203
            "  :sensor = \"Delay Doppler Mapping Instrument (DDMI)\";\n"
            + "  :geospatial_lat_min = \"-39.9N\";\n"
            + "  :geospatial_lat_max = \"39.9N\";\n"
            + "  :geospatial_lon_min = \"0.1E\";\n"
            + "  :geospatial_lon_max = \"359.9E\";\n"
            + "  :version_id = \"1.0\";\n"
            + "  :title = \"CYGNSS Level 3 Climate Data Record Version 1.0\";\n"
            + "  :ShortName = \"CYGNSS_L3_CDR_V1.0\";\n"
            + "  :id = \"PODAAC-CYGNS-L3C10\";\n"
            + "  :netcdf_version_id = \"4.3.3.1 of Dec 10 2015 16:44:18 $\";\n"
            + "  :l3_algorithm_version = \"cdr-v1.0\";\n"
            + "  :date_created = \"2020-04-28T17:49:27Z\";\n"
            + "  :date_issued = \"2020-04-28T17:49:27Z\";\n"
            + "  :source = \"cyg.ddmi.s20200101-000000-e20200101-235959.l2.wind-mss-cdr.a10.d10.nc\";\n"
            + "  :history = \"Tue Apr 28 17:49:28 2020: ncks -O -L1 -a /tmp/qt_temp.J61461 /tmp/qt_temp.T61461\n"
            + "/data/ops/op_cdr_1_0/apps/src/produce-L3-files/produce-L3-files --dstore production_1@cygnss-data-1.engin.umich.edu --day 2020-01-01\";\n"
            + "  :time_coverage_start = \"2020-01-01T00:30:00Z\";\n"
            + "  :time_coverage_end = \"2020-01-01T23:30:00Z\";\n"
            + "  :time_coverage_duration = \"P1DT00H00M00S\";\n"
            + "  :time_coverage_resolution = \"P0DT1H0M0S\";\n"
            + "  :platform = \"Observatory References: cyg1, cyg2, cyg3, cyg4, cyg5, cyg6, cyg7, cyg8\";\n"
            + "  :NCO = \"4.4.4\";\n"
            + "\n"
            + "  data:\n"
            + "    time = \n"
            + "      {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23}\n"
            + "}\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

    // ensure files are reread
    File2.deleteAllFiles(EDDGridFromNcFilesUnpacked.datasetDir("testSuperPreciseTimeUnits"));
    EDDGrid eddGrid = (EDDGrid) EDDTestDataset.gettestSuperPreciseTimeUnits();

    // .dds dds isn't affected by userDapQuery
    tName =
        eddGrid.makeNewFileForDapQuery(language, null, null, "", tDir, eddGrid.className(), ".dds");
    results = File2.directReadFrom88591File(tDir + tName);
    expected = // difference from testUInt16Dap: lat lon are float here, not double
        "Dataset {\n"
            + "  Float64 time[time = 24];\n"
            + "  Float32 latitude[latitude = 400];\n"
            + "  Float32 longitude[longitude = 1800];\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 wind_speed[time = 24][latitude = 400][longitude = 1800];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 24];\n"
            + "      Float32 latitude[latitude = 400];\n"
            + "      Float32 longitude[longitude = 1800];\n"
            + "  } wind_speed;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 wind_speed_uncertainty[time = 24][latitude = 400][longitude = 1800];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 24];\n"
            + "      Float32 latitude[latitude = 400];\n"
            + "      Float32 longitude[longitude = 1800];\n"
            + "  } wind_speed_uncertainty;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Int32 num_wind_speed_samples[time = 24][latitude = 400][longitude = 1800];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 24];\n"
            + "      Float32 latitude[latitude = 400];\n"
            + "      Float32 longitude[longitude = 1800];\n"
            + "  } num_wind_speed_samples;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 yslf_wind_speed[time = 24][latitude = 400][longitude = 1800];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 24];\n"
            + "      Float32 latitude[latitude = 400];\n"
            + "      Float32 longitude[longitude = 1800];\n"
            + "  } yslf_wind_speed;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 yslf_wind_speed_uncertainty[time = 24][latitude = 400][longitude = 1800];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 24];\n"
            + "      Float32 latitude[latitude = 400];\n"
            + "      Float32 longitude[longitude = 1800];\n"
            + "  } yslf_wind_speed_uncertainty;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Int32 num_yslf_wind_speed_samples[time = 24][latitude = 400][longitude = 1800];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 24];\n"
            + "      Float32 latitude[latitude = 400];\n"
            + "      Float32 longitude[longitude = 1800];\n"
            + "  } num_yslf_wind_speed_samples;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 mean_square_slope[time = 24][latitude = 400][longitude = 1800];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 24];\n"
            + "      Float32 latitude[latitude = 400];\n"
            + "      Float32 longitude[longitude = 1800];\n"
            + "  } mean_square_slope;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 mean_square_slope_uncertainty[time = 24][latitude = 400][longitude = 1800];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 24];\n"
            + "      Float32 latitude[latitude = 400];\n"
            + "      Float32 longitude[longitude = 1800];\n"
            + "  } mean_square_slope_uncertainty;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Int32 num_mss_samples[time = 24][latitude = 400][longitude = 1800];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 24];\n"
            + "      Float32 latitude[latitude = 400];\n"
            + "      Float32 longitude[longitude = 1800];\n"
            + "  } num_mss_samples;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 merra2_wind_speed[time = 24][latitude = 400][longitude = 1800];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 24];\n"
            + "      Float32 latitude[latitude = 400];\n"
            + "      Float32 longitude[longitude = 1800];\n"
            + "  } merra2_wind_speed;\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Int32 num_merra2_wind_speed_samples[time = 24][latitude = 400][longitude = 1800];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 24];\n"
            + "      Float32 latitude[latitude = 400];\n"
            + "      Float32 longitude[longitude = 1800];\n"
            + "  } num_merra2_wind_speed_samples;\n"
            + "} testSuperPreciseTimeUnits;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .csv data values
    userDapQuery = "time";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, "time", tDir, eddGrid.className(), ".csv");
    results = File2.directReadFrom88591File(tDir + tName);
    String2.log(results);
    expected = // difference from testUInt16Dap: lat lon are float here, not double
        "time\n"
            + "UTC\n"
            + "2020-01-01T00:30:00Z\n"
            + "2020-01-01T01:30:00Z\n"
            + "2020-01-01T02:30:00Z\n"
            + "2020-01-01T03:30:00Z\n"
            + "2020-01-01T04:30:00Z\n"
            + "2020-01-01T05:30:00Z\n"
            + "2020-01-01T06:30:00Z\n"
            + "2020-01-01T07:30:00Z\n"
            + "2020-01-01T08:30:00Z\n"
            + "2020-01-01T09:30:00Z\n"
            + "2020-01-01T10:30:00Z\n"
            + "2020-01-01T11:30:00Z\n"
            + "2020-01-01T12:30:00Z\n"
            + "2020-01-01T13:30:00Z\n"
            + "2020-01-01T14:30:00Z\n"
            + "2020-01-01T15:30:00Z\n"
            + "2020-01-01T16:30:00Z\n"
            + "2020-01-01T17:30:00Z\n"
            + "2020-01-01T18:30:00Z\n"
            + "2020-01-01T19:30:00Z\n"
            + "2020-01-01T20:30:00Z\n"
            + "2020-01-01T21:30:00Z\n"
            + "2020-01-01T22:30:00Z\n"
            + "2020-01-01T23:30:00Z\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    NcHelper.debugMode = oDebugMode;
  }

  /**
   * Test files from https://oceandata.sci.gsfc.nasa.gov/MODIS-Aqua/L3SMI and stored in
   * /erddapTest/unsigned/
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagImageComparison
  void testMissingValue() throws Throwable {
    // String2.log("\n*** EDDGridFromNcFilesUnpacked.testMissingValue");
    // testVerboseOn();
    int language = 0;
    String name, tName, results, tResults, expected, userDapQuery;
    String today = Calendar2.getCurrentISODateTimeStringZulu() + "Z";
    String tDir = EDStatic.fullTestCacheDirectory;
    String fileDir =
        Path.of(EDDGridFromNcFilesUnpackedTests.class.getResource("/data/unpacked/").toURI())
                .toString()
            + "/";
    String fileName1 = "A2003001.L3m_DAY_POC_poc_4km.nc";
    String fileName2 = "A2016241.L3m_DAY_POC_poc_4km.nc";
    Variable var;
    Attributes atts;
    Array array;
    PrimitiveArray pa;
    boolean oDebugMode = NcHelper.debugMode;
    NcHelper.debugMode = true;

    // **** fileName1 -- not packed data: poc is float
    // DumpString
    results = NcHelper.ncdump(fileDir + fileName1, "-h");
    expected =
        "netcdf A2003001.L3m_DAY_POC_poc_4km.nc {\n"
            + "  dimensions:\n"
            + "    lon = 8640;\n"
            + "    eightbitcolor = 256;\n"
            + "    rgb = 3;\n"
            + "    lat = 4320;\n"
            + "  variables:\n"
            + "    float poc(lat=4320, lon=8640);\n"
            + "      :long_name = \"Particulate Organic Carbon, D. Stramski, 2007 (443/555 version)\";\n"
            + "      :units = \"mg m^-3\";\n"
            + "      :standard_name = \"mole_concentration_of_particulate_organic_carbon_in_sea_water\";\n"
            + "      :_FillValue = -32767.0f; // float\n"
            + "      :valid_min = 0.0f; // float\n"
            + "      :valid_max = 1000.0f; // float\n"
            + "      :display_scale = \"log\";\n"
            + "      :display_min = 10.0; // double\n"
            + "      :display_max = 1000.0; // double\n"
            + "      :scale_factor = 1.0f; // float\n"
            + "      :add_offset = 0.0f; // float\n"
            + "      :reference = \"Stramski, D., et al. \\\"Relationships between the surface concentration of particulate organic carbon and optical properties in the eastern South Pacific and eastern Atlantic Oceans.\\\" Biogeosciences 5.1 (2008): 171-201.\";\n"
            + "      :_ChunkSizes = 64U, 64U; // uint\n"
            + "\n"
            + "    float lon(lon=8640);\n"
            + "      :long_name = \"Longitude\";\n"
            + "      :units = \"degree_east\";\n"
            + "      :_FillValue = -32767.0f; // float\n"
            + "      :valid_min = -180.0f; // float\n"
            + "      :valid_max = 180.0f; // float\n"
            + "\n"
            + "    ubyte palette(rgb=3, eightbitcolor=256);\n"
            +
            // " :_FillValue = -1UB; // byte\n" + //gone with
            // " :_Unsigned = \"true\";\n" +
            "\n"
            + "    float lat(lat=4320);\n"
            + "      :long_name = \"Latitude\";\n"
            + "      :units = \"degree_north\";\n"
            + "      :_FillValue = -32767.0f; // float\n"
            + "      :valid_min = -90.0f; // float\n"
            + "      :valid_max = 90.0f; // float\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    NetcdfFile ncFile = NcHelper.openFile(fileDir + fileName1);
    try {

      // lon
      var = ncFile.findVariable("lon");
      atts = new Attributes();
      NcHelper.getVariableAttributes(var, atts);
      results = atts.toString();
      expected =
          "    _FillValue=-32767.0f\n"
              + "    long_name=Longitude\n"
              + "    units=degree_east\n"
              + // what's in the file
              "    valid_max=180.0f\n"
              + "    valid_min=-180.0f\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      Units2.unpackVariableAttributes(atts, var.getFullName(), NcHelper.getElementPAType(var));
      results = atts.toString();
      expected =
          "    _FillValue=NaNf\n"
              + // converted to PA standard mv
              "    long_name=Longitude\n"
              + "    units=degrees_east\n"
              + // standardized to degrees_east
              "    valid_max=180.0f\n"
              + "    valid_min=-180.0f\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // palette
      var = ncFile.findVariable("palette");
      atts = new Attributes();
      NcHelper.getVariableAttributes(var, atts);
      results = atts.toString();
      expected =
          // " _FillValue=-1b\n" +
          // " _Unsigned=true\n"; //disappeared w netcdf-java 5.2, so I added back in with
          // code in getVariableAttributes. 2020 now gone again because it is just a part
          // of the dataType
          "";
      Test.ensureEqual(results, expected, "results=\n" + results);

      Units2.unpackVariableAttributes(atts, var.getFullName(), NcHelper.getElementPAType(var));
      results = atts.toString();
      expected = "";
      // " _FillValue=32767s\n"; //byte -> short //converted to PA standard mv
      // " _Unsigned=true\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // palette as unsigned byte
      pa =
          NcHelper.getPrimitiveArray(
              var.read(new int[] {0, 0}, new int[] {1, 10}), true, true); // origin, shape,
      // buildStringFromChar
      Test.ensureEqual(pa.elementTypeString(), "ubyte", "");
      results = pa.toString();
      expected = "147, 0, 108, 144, 0, 111, 141, 0, 114, 138";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // palette ubyte
      pa = NcHelper.unpackPA(var, pa, true, true); // lookForStringTimes, lookForUnsigned
      Test.ensureEqual(pa.elementTypeString(), "ubyte", "");
      results = pa.toString();
      expected = "147, 0, 108, 144, 0, 111, 141, 0, 114, 138"; // unsigned
      Test.ensureEqual(results, expected, "results=\n" + results);

      // poc
      var = ncFile.findVariable("poc");
      atts = new Attributes();
      NcHelper.getVariableAttributes(var, atts);
      results = atts.toString();
      expected =
          "    _ChunkSizes=64ui,64ui\n"
              + "    _FillValue=-32767.0f\n"
              + "    add_offset=0.0f\n"
              + "    display_max=1000.0d\n"
              + "    display_min=10.0d\n"
              + "    display_scale=log\n"
              + "    long_name=\"Particulate Organic Carbon, D. Stramski, 2007 (443/555 version)\"\n"
              + "    reference=\"Stramski, D., et al. \"\"Relationships between the surface concentration of particulate organic carbon and optical properties in the eastern South Pacific and eastern Atlantic Oceans.\"\" Biogeosciences 5.1 (2008): 171-201.\"\n"
              + "    scale_factor=1.0f\n"
              + "    standard_name=mole_concentration_of_particulate_organic_carbon_in_sea_water\n"
              + "    units=mg m^-3\n"
              + "    valid_max=1000.0f\n"
              + "    valid_min=0.0f\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      Units2.unpackVariableAttributes(atts, var.getFullName(), NcHelper.getElementPAType(var));
      results = atts.toString();
      expected =
          "    _ChunkSizes=64ui,64ui\n"
              + "    _FillValue=NaNf\n"
              + // standardized
              // " add_offset=0.0f\n" + //removed
              "    display_max=1000.0d\n"
              + "    display_min=10.0d\n"
              + "    display_scale=log\n"
              + "    long_name=\"Particulate Organic Carbon, D. Stramski, 2007 (443/555 version)\"\n"
              + "    reference=\"Stramski, D., et al. \"\"Relationships between the surface concentration of particulate organic carbon and optical properties in the eastern South Pacific and eastern Atlantic Oceans.\"\" Biogeosciences 5.1 (2008): 171-201.\"\n"
              +
              // " scale_factor=1.0f\n" + //removed
              "    standard_name=mole_concentration_of_particulate_organic_carbon_in_sea_water\n"
              + "    units=mg m-3\n"
              + "    valid_max=1000.0f\n"
              + "    valid_min=0.0f\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // poc as packed values (shorts)
      pa =
          NcHelper.getPrimitiveArray(
              var.read(new Section("(0:4100:1000,0:8100:1000)")), true, false); // start:end:stride,
      // buildStringFromChar, isUnsigned
      Test.ensureEqual(pa.elementTypeString(), "float", "");
      results = pa.toString();
      expected =
          "-32767.0, -32767.0, -32767.0, -32767.0, -32767.0, -32767.0, -32767.0, -32767.0, "
              + "-32767.0, -32767.0, -32767.0, -32767.0, -32767.0, -32767.0, -32767.0, -32767.0, "
              + "-32767.0, -32767.0, -32767.0, -32767.0, -32767.0, -32767.0, -32767.0, -32767.0, "
              + "-32767.0, -32767.0, 29.476826, -32767.0, -32767.0, -32767.0, 431.7499, -32767.0, "
              + "36.19993, -32767.0, -32767.0, -32767.0, -32767.0, -32767.0, -32767.0, -32767.0, "
              + "-32767.0, -32767.0, -32767.0, -32767.0, -32767.0";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // poc as unpacked values (float)
      pa = NcHelper.unpackPA(var, pa, true, true); // lookForStringTimes, lookForUnsigned
      Test.ensureEqual(pa.elementTypeString(), "float", "");
      results = pa.toString();
      expected = // standardized mv
          "NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, "
              + "NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, 29.476826, NaN, NaN, NaN, "
              + "431.7499, NaN, 36.19993, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN";
      Test.ensureEqual(results, expected, "results=\n" + results);
    } finally {
      try {
        if (ncFile != null) ncFile.close();
      } catch (Exception e9) {
      }
    }

    // **** fileName2 -- packed data: poc is short
    // DumpString
    results = NcHelper.ncdump(fileDir + fileName2, "-h");
    expected =
        "netcdf A2016241.L3m_DAY_POC_poc_4km.nc {\n"
            + "  dimensions:\n"
            + "    eightbitcolor = 256;\n"
            + "    rgb = 3;\n"
            + "    lat = 4320;\n"
            + "    lon = 8640;\n"
            + "  variables:\n"
            + "    short poc(lat=4320, lon=8640);\n"
            + "      :long_name = \"Particulate Organic Carbon, D. Stramski, 2007 (443/555 version)\";\n"
            + "      :scale_factor = 0.2f; // float\n"
            + "      :add_offset = 6400.0f; // float\n"
            + "      :units = \"mg m^-3\";\n"
            + "      :standard_name = \"mole_concentration_of_particulate_organic_carbon_in_sea_water\";\n"
            + "      :_FillValue = -32767S; // short\n"
            + "      :valid_min = -32000S; // short\n"
            + "      :valid_max = -27000S; // short\n"
            + "      :reference = \"Stramski, D., et al. \\\"Relationships between the surface concentration of particulate organic carbon and optical properties in the eastern South Pacific and eastern Atlantic Oceans.\\\" Biogeosciences 5.1 (2008): 171-201.\";\n"
            + "      :display_scale = \"log\";\n"
            + "      :display_min = 10.0f; // float\n"
            + "      :display_max = 1000.0f; // float\n"
            + "      :_ChunkSizes = 40U, 1729U; // uint\n"
            + "\n"
            + "    ubyte palette(rgb=3, eightbitcolor=256);\n"
            + // 'u' added in netcdf-java 5.2
            // " :_FillValue = -1UB; // byte\n" + //gone in netcdf-java 5.2
            // " :_Unsigned = \"true\";\n" +
            "\n"
            + "    float lat(lat=4320);\n"
            + "      :long_name = \"Latitude\";\n"
            + "      :units = \"degree_north\";\n"
            + "      :_FillValue = -999.0f; // float\n"
            + "      :valid_min = -90.0f; // float\n"
            + "      :valid_max = 90.0f; // float\n"
            + "\n"
            + "    float lon(lon=8640);\n"
            + "      :long_name = \"Longitude\";\n"
            + "      :units = \"degree_east\";\n"
            + "      :_FillValue = -999.0f; // float\n"
            + "      :valid_min = -180.0f; // float\n"
            + "      :valid_max = 180.0f; // float\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    ncFile = NcHelper.openFile(fileDir + fileName2);
    try {

      // lon
      var = ncFile.findVariable("lon");
      atts = new Attributes();
      NcHelper.getVariableAttributes(var, atts);
      results = atts.toString();
      expected =
          "    _FillValue=-999.0f\n"
              + "    long_name=Longitude\n"
              + "    units=degree_east\n"
              + // in file
              "    valid_max=180.0f\n"
              + "    valid_min=-180.0f\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      Units2.unpackVariableAttributes(atts, var.getFullName(), NcHelper.getElementPAType(var));
      results = atts.toString();
      expected =
          "    _FillValue=NaNf\n"
              + // converted to PA standard mv
              "    long_name=Longitude\n"
              + "    units=degrees_east\n"
              + // stardardized to 's'
              "    valid_max=180.0f\n"
              + "    valid_min=-180.0f\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // palette
      var = ncFile.findVariable("palette");
      atts = new Attributes();
      NcHelper.getVariableAttributes(var, atts);
      results = atts.toString();
      expected = "";
      // " _FillValue=-1b\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      Units2.unpackVariableAttributes(atts, var.getFullName(), NcHelper.getElementPAType(var));
      results = atts.toString();
      expected = "";
      // " _FillValue=32767s\n"; //byte -> short //converted to PA standard mv. gone
      // in netcdf-java 5.2
      Test.ensureEqual(results, expected, "results=\n" + results);

      // palette as unsigned byte
      pa =
          NcHelper.getPrimitiveArray(
              var.read(new int[] {0, 0}, new int[] {1, 10}), true, true); // origin, shape,
      // buildStringFromChar, isUnsigned
      Test.ensureEqual(pa.elementTypeString(), "ubyte", "");
      results = pa.toString();
      expected = "147, 0, 108, 144, 0, 111, 141, 0, 114, 138";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // palette ubyte
      pa = NcHelper.unpackPA(var, pa, true, true); // lookForStringTimes, lookForUnsigned
      Test.ensureEqual(pa.elementTypeString(), "ubyte", "");
      results = pa.toString();
      expected = "147, 0, 108, 144, 0, 111, 141, 0, 114, 138";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // poc
      var = ncFile.findVariable("poc");
      atts = new Attributes();
      NcHelper.getVariableAttributes(var, atts);
      results = atts.toString();
      expected =
          "    _ChunkSizes=40ui,1729ui\n"
              + "    _FillValue=-32767s\n"
              + "    add_offset=6400.0f\n"
              + "    display_max=1000.0f\n"
              + "    display_min=10.0f\n"
              + "    display_scale=log\n"
              + "    long_name=\"Particulate Organic Carbon, D. Stramski, 2007 (443/555 version)\"\n"
              + "    reference=\"Stramski, D., et al. \"\"Relationships between the surface concentration of particulate organic carbon and optical properties in the eastern South Pacific and eastern Atlantic Oceans.\"\" Biogeosciences 5.1 (2008): 171-201.\"\n"
              + "    scale_factor=0.2f\n"
              + "    standard_name=mole_concentration_of_particulate_organic_carbon_in_sea_water\n"
              + "    units=mg m^-3\n"
              + // in file
              "    valid_max=-27000s\n"
              + "    valid_min=-32000s\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      Units2.unpackVariableAttributes(atts, var.getFullName(), NcHelper.getElementPAType(var));
      results = atts.toString();
      expected =
          "    _ChunkSizes=40ui,1729ui\n"
              + "    _FillValue=NaNf\n"
              + // standardized
              // " add_offset=6400.0\n" + //removed
              "    display_max=1000.0f\n"
              + "    display_min=10.0f\n"
              + "    display_scale=log\n"
              + "    long_name=\"Particulate Organic Carbon, D. Stramski, 2007 (443/555 version)\"\n"
              + "    reference=\"Stramski, D., et al. \"\"Relationships between the surface concentration of particulate organic carbon and optical properties in the eastern South Pacific and eastern Atlantic Oceans.\"\" Biogeosciences 5.1 (2008): 171-201.\"\n"
              +
              // " scale_factor=0.2f\n" + removed
              "    standard_name=mole_concentration_of_particulate_organic_carbon_in_sea_water\n"
              + "    units=mg m-3\n"
              + // standardized
              "    valid_max=1000.0f\n"
              + // unpacked
              "    valid_min=0.0f\n"; // unpacked
      Test.ensureEqual(results, expected, "results=\n" + results);

      // poc as packed values (shorts)
      pa =
          NcHelper.getPrimitiveArray( // odd start to catch some data, not just mv
              var.read(new Section("(70:4100:1000,70:8100:1000)")),
              true,
              NcHelper.isUnsigned(var)); // start:end:stride,
      // buildStringFromChar
      Test.ensureEqual(pa.elementTypeString(), "short", "");
      results = pa.toString();
      expected =
          "-32767, -32767, -32767, -32767, -32767, -32767, -32767, -32767, -32767, -32767, "
              + "-32767, -32767, -31518, -32767, -31186, -32767, -32767, -31609, -32767, -32767, "
              + "-32767, -32767, -32767, -32767, -32767, -32767, -31867, -32767, -32767, -32767, "
              + "-32767, -32767, -32767, -32767, -32767, -32767, -32767, -32767, -32767, -32767, "
              + "-32767, -32767, -32767, -32767, -32767";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // poc as unpacked values (float)
      pa = NcHelper.unpackPA(var, pa, true, true); // lookForStringTimes, lookForUnsigned
      Test.ensureEqual(pa.elementTypeString(), "float", "");
      results = pa.toString();
      expected =
          "NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, 96.4, NaN, 162.8, "
              + "NaN, NaN, 78.2, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, 26.6, NaN, NaN, NaN, "
              + "NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN";
      Test.ensureEqual(results, expected, "results=\n" + results);
    } finally {
      try {
        if (ncFile != null) ncFile.close();
      } catch (Exception e9) {
      }
    }

    // ***** test erdMPOC1day dataset
    EDDGrid eddGrid = (EDDGrid) EDDTestDataset.geterdMPOC1day();

    // .das
    tName =
        eddGrid.makeNewFileForDapQuery(language, null, null, "", tDir, eddGrid.className(), ".das");
    results = File2.readFromFile88591(tDir + tName)[1];
    expected =
        "Attributes {\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range 1.0414224e+9, 1.472472e+9;\n"
            + "    String axis \"T\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Centered Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float32 _FillValue NaN;\n"
            + "    Float32 actual_range -89.97918, 89.97916;\n"
            + // a test of descending lat axis
            "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "    Float32 valid_max 90.0;\n"
            + "    Float32 valid_min -90.0;\n"
            + "  }\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float32 _FillValue NaN;\n"
            + "    Float32 actual_range -179.9792, 179.9792;\n"
            + "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "    Float32 valid_max 180.0;\n"
            + "    Float32 valid_min -180.0;\n"
            + "  }\n"
            + "  poc {\n"
            + "    Float32 _FillValue NaN;\n"
            + "    Float64 colorBarMaximum 1000.0;\n"
            + "    Float64 colorBarMinimum 10.0;\n"
            + "    String colorBarScale \"Log\";\n"
            + "    String ioos_category \"Ocean Color\";\n"
            + "    String long_name \"Particulate Organic Carbon, D. Stramski, 2007 (443/555 version)\";\n"
            + "    String references \"Stramski, D., et al. \\\"Relationships between the surface concentration of particulate organic carbon and optical properties in the eastern South Pacific and eastern Atlantic Oceans.\\\" Biogeosciences 5.1 (2008): 171-201.\";\n"
            + "    String standard_name \"mole_concentration_of_particulate_organic_carbon_in_sea_water\";\n"
            + "    String units \"mg m-3\";\n"
            + "    Float32 valid_max 1000.0;\n"
            + "    Float32 valid_min 0.0;\n"
            + "  }\n"
            + "  NC_GLOBAL {\n"
            + "    String _lastModified \"YYYY-MM-DDThh:mm:ss.000Z\";\n"
            + "    String cdm_data_type \"Grid\";\n"
            + "    String Conventions \"CF-1.6, COARDS, ACDD-1.3\";\n"
            + "    String creator_email \"data@oceancolor.gsfc.nasa.gov\";\n"
            + "    String creator_name \"NASA/GSFC/OBPG\";\n"
            + "    String creator_type \"group\";\n"
            + "    String creator_url \"https://oceandata.sci.gsfc.nasa.gov\";\n"
            + "    String date_created \"YYYY-MM-DDThh:mm:ss.000Z\";\n"
            + "    Float64 Easternmost_Easting 179.9792;\n"
            + "    Float64 geospatial_lat_max 89.97916;\n"
            + "    Float64 geospatial_lat_min -89.97918;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max 179.9792;\n"
            + "    Float64 geospatial_lon_min -179.9792;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n"
            + "    String grid_mapping_name \"latitude_longitude\";\n"
            + "    String history \"Datafiles are downloaded ASAP from https://oceandata.sci.gsfc.nasa.gov/MODIS-Aqua/L3SMI to NOAA NMFS SWFSC ERD.\n"
            + "NOAA NMFS SWFSC ERD (erd.data@noaa.gov) uses ERDDAP to add the time variable and slightly modify the metadata.\n"
            + "Direct read of HDF4 file through CDM library.\n";
    results =
        results.replaceAll(
            "String _lastModified \"....-..-..T..:..:...000Z",
            "String _lastModified \"YYYY-MM-DDThh:mm:ss.000Z");
    results =
        results.replaceAll(
            "String date_created \"....-..-..T..:..:...000Z",
            "String date_created \"YYYY-MM-DDThh:mm:ss.000Z");
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

    expected =
        // "2015-10-30T18:17:10Z (local files)
        // 2015-10-30T18:17:10Z
        // http://localhost:8080/cwexperimental/griddap/testUInt16File.das";
        "    String identifier_product_doi \"10.5067/AQUA/MODIS_OC.2014.0\";\n"
            + "    String identifier_product_doi_authority \"https://dx.doi.org\";\n"
            + "    String infoUrl \"https://coastwatch.pfeg.noaa.gov/infog/MPOC_las.html\";\n"
            + "    String institution \"NASA/GSFC OBPG\";\n"
            + "    String instrument \"MODIS\";\n"
            + "    String keywords \"443/555, biology, carbon, center, chemistry, chlorophyll, color, concentration, data, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, Earth Science > Oceans > Ocean Optics > Ocean Color, ecology, flight, goddard, group, gsfc, image, imaging, L3, laboratory, level, level-3, mapped, moderate, modis, mole, mole_concentration_of_particulate_organic_carbon_in_sea_water, nasa, ocean, ocean color, oceans, optics, organic, particulate, poc, processing, resolution, sea, seawater, smi, space, spectroradiometer, standard, stramski, time, version, water\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String l2_flag_names \"ATMFAIL,LAND,HILT,HISATZEN,STRAYLIGHT,CLDICE,COCCOLITH,LOWLW,CHLWARN,CHLFAIL,NAVWARN,MAXAERITER,ATMWARN,HISOLZEN,NAVFAIL,FILTER,HIGLINT\";\n"
            + "    String license \"https://science.nasa.gov/earth-science/earth-science-data/data-information-policy/\n"
            + "The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    String map_projection \"Equidistant Cylindrical\";\n"
            + "    String measure \"Mean\";\n"
            + "    String naming_authority \"gov.noaa.pfeg.coastwatch\";\n"
            + "    Float64 Northernmost_Northing 89.97916;\n"
            + "    String platform \"Aqua\";\n"
            + "    String processing_level \"L3 Mapped\";\n"
            + "    String processing_version \"VERSION\";\n"
            + "    String project \"Ocean Biology Processing Group (NASA/GSFC/OBPG)\";\n"
            + "    String publisher_email \"erd.data@noaa.gov\";\n"
            + "    String publisher_name \"NOAA NMFS SWFSC ERD\";\n"
            + "    String publisher_type \"institution\";\n"
            + "    String publisher_url \"https://www.pfeg.noaa.gov\";\n"
            + "    String sourceUrl \"(local files)\";\n"
            + "    Float64 Southernmost_Northing -89.97918;\n"
            + "    String spatialResolution \"X.X km\";\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String summary \"MODIS Aqua, Level-3 Standard Mapped Image (SMI), Global, 4km, Particulate Organic Carbon (POC) (1 Day Composite)\";\n"
            + "    String temporal_range \"day\";\n"
            + "    String testOutOfDate \"now-4days\";\n"
            + "    String time_coverage_end \"2016-08-29T12:00:00Z\";\n"
            + "    String time_coverage_start \"2003-01-01T12:00:00Z\";\n"
            + "    String title \"MODIS Aqua, Level-3 SMI, Global, 4km, Particulate Organic Carbon, 2003-present (1 Day Composite)\";\n"
            + "    Float64 Westernmost_Easting -179.9792;\n"
            + "  }\n"
            + "}\n";
    results =
        results.replaceAll(
            "String processing_version \\\"[\\w\\d]+(\\.[\\w\\d]+)*\\\"",
            "String processing_version \\\"VERSION\\\"");
    results =
        results.replaceAll(
            "String spatialResolution \\\"[0-9]+.[0-9]+ km\\\"",
            "String spatialResolution \"X.X km\"");
    int tpo = results.indexOf(expected.substring(0, 50));
    Test.ensureTrue(tpo >= 0, "tpo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tpo, Math.min(results.length(), tpo + expected.length())),
        expected,
        "results=\n" + results);

    // .dds dds isn't affected by userDapQuery
    tName =
        eddGrid.makeNewFileForDapQuery(language, null, null, "", tDir, eddGrid.className(), ".dds");
    results = File2.directReadFrom88591File(tDir + tName);
    expected =
        "Dataset {\n"
            + "  Float64 time[time = 4];\n"
            + // 2 unpacked files + 2 packed files
            "  Float32 latitude[latitude = 4320];\n"
            + "  Float32 longitude[longitude = 8640];\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 poc[time = 4][latitude = 4320][longitude = 8640];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = 4];\n"
            + "      Float32 latitude[latitude = 4320];\n"
            + "      Float32 longitude[longitude = 8640];\n"
            + "  } poc;\n"
            + "} erdMPOC1day;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .csv time values
    userDapQuery = "time";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, tDir, eddGrid.className() + "time", ".csv");
    results = File2.directReadFrom88591File(tDir + tName);
    String2.log(results);
    expected =
        "time\n"
            + "UTC\n"
            + "2003-01-01T12:00:00Z\n"
            + "2003-01-02T12:00:00Z\n"
            + "2016-08-28T12:00:00Z\n"
            + "2016-08-29T12:00:00Z\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .csv poc values
    userDapQuery =
        "poc[(2003-01-01T12:00:00Z)][0:1000:4000][0:1000:8000]"; // match direct read above
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, tDir, eddGrid.className() + "poc1", ".csv");
    results = File2.directReadFrom88591File(tDir + tName);
    String2.log(results);
    expected =
        "time,latitude,longitude,poc\n"
            + "UTC,degrees_north,degrees_east,mg m-3\n"
            + "2003-01-01T12:00:00Z,89.979164,-179.97917,NaN\n"
            + "2003-01-01T12:00:00Z,89.979164,-138.3125,NaN\n"
            + "2003-01-01T12:00:00Z,89.979164,-96.64583,NaN\n"
            + "2003-01-01T12:00:00Z,89.979164,-54.979168,NaN\n"
            + "2003-01-01T12:00:00Z,89.979164,-13.312495,NaN\n"
            + "2003-01-01T12:00:00Z,89.979164,28.354177,NaN\n"
            + "2003-01-01T12:00:00Z,89.979164,70.020836,NaN\n"
            + "2003-01-01T12:00:00Z,89.979164,111.68752,NaN\n"
            + "2003-01-01T12:00:00Z,89.979164,153.35417,NaN\n"
            + "2003-01-01T12:00:00Z,48.3125,-179.97917,NaN\n"
            + "2003-01-01T12:00:00Z,48.3125,-138.3125,NaN\n"
            + "2003-01-01T12:00:00Z,48.3125,-96.64583,NaN\n"
            + "2003-01-01T12:00:00Z,48.3125,-54.979168,NaN\n"
            + "2003-01-01T12:00:00Z,48.3125,-13.312495,NaN\n"
            + "2003-01-01T12:00:00Z,48.3125,28.354177,NaN\n"
            + "2003-01-01T12:00:00Z,48.3125,70.020836,NaN\n"
            + "2003-01-01T12:00:00Z,48.3125,111.68752,NaN\n"
            + "2003-01-01T12:00:00Z,48.3125,153.35417,NaN\n"
            + "2003-01-01T12:00:00Z,6.6458306,-179.97917,NaN\n"
            + "2003-01-01T12:00:00Z,6.6458306,-138.3125,NaN\n"
            + "2003-01-01T12:00:00Z,6.6458306,-96.64583,NaN\n"
            + "2003-01-01T12:00:00Z,6.6458306,-54.979168,NaN\n"
            + "2003-01-01T12:00:00Z,6.6458306,-13.312495,NaN\n"
            + "2003-01-01T12:00:00Z,6.6458306,28.354177,NaN\n"
            + "2003-01-01T12:00:00Z,6.6458306,70.020836,NaN\n"
            + "2003-01-01T12:00:00Z,6.6458306,111.68752,NaN\n"
            + "2003-01-01T12:00:00Z,6.6458306,153.35417,29.476826\n"
            + "2003-01-01T12:00:00Z,-35.020832,-179.97917,NaN\n"
            + "2003-01-01T12:00:00Z,-35.020832,-138.3125,NaN\n"
            + "2003-01-01T12:00:00Z,-35.020832,-96.64583,NaN\n"
            + "2003-01-01T12:00:00Z,-35.020832,-54.979168,431.7499\n"
            + "2003-01-01T12:00:00Z,-35.020832,-13.312495,NaN\n"
            + "2003-01-01T12:00:00Z,-35.020832,28.354177,36.19993\n"
            + "2003-01-01T12:00:00Z,-35.020832,70.020836,NaN\n"
            + "2003-01-01T12:00:00Z,-35.020832,111.68752,NaN\n"
            + "2003-01-01T12:00:00Z,-35.020832,153.35417,NaN\n"
            + "2003-01-01T12:00:00Z,-76.68751,-179.97917,NaN\n"
            + "2003-01-01T12:00:00Z,-76.68751,-138.3125,NaN\n"
            + "2003-01-01T12:00:00Z,-76.68751,-96.64583,NaN\n"
            + "2003-01-01T12:00:00Z,-76.68751,-54.979168,NaN\n"
            + "2003-01-01T12:00:00Z,-76.68751,-13.312495,NaN\n"
            + "2003-01-01T12:00:00Z,-76.68751,28.354177,NaN\n"
            + "2003-01-01T12:00:00Z,-76.68751,70.020836,NaN\n"
            + "2003-01-01T12:00:00Z,-76.68751,111.68752,NaN\n"
            + "2003-01-01T12:00:00Z,-76.68751,153.35417,NaN\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .csv poc values 70:4100:1000,70:8100:1000
    userDapQuery =
        "poc[(2016-08-28T12:00:00Z)][70:1000:4100][70:1000:8100]"; // match direct read above
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, tDir, eddGrid.className() + "poc2", ".csv");
    results = File2.directReadFrom88591File(tDir + tName);
    String2.log(results);
    expected =
        "time,latitude,longitude,poc\n"
            + "UTC,degrees_north,degrees_east,mg m-3\n"
            + "2016-08-28T12:00:00Z,87.0625,-177.0625,NaN\n"
            + "2016-08-28T12:00:00Z,87.0625,-135.39583,NaN\n"
            + "2016-08-28T12:00:00Z,87.0625,-93.729164,NaN\n"
            + "2016-08-28T12:00:00Z,87.0625,-52.062496,NaN\n"
            + "2016-08-28T12:00:00Z,87.0625,-10.3958235,NaN\n"
            + "2016-08-28T12:00:00Z,87.0625,31.270834,NaN\n"
            + "2016-08-28T12:00:00Z,87.0625,72.93751,NaN\n"
            + "2016-08-28T12:00:00Z,87.0625,114.60418,NaN\n"
            + "2016-08-28T12:00:00Z,87.0625,156.27083,NaN\n"
            + "2016-08-28T12:00:00Z,45.395832,-177.0625,NaN\n"
            + "2016-08-28T12:00:00Z,45.395832,-135.39583,NaN\n"
            + "2016-08-28T12:00:00Z,45.395832,-93.729164,NaN\n"
            + "2016-08-28T12:00:00Z,45.395832,-52.062496,96.4\n"
            + "2016-08-28T12:00:00Z,45.395832,-10.3958235,NaN\n"
            + "2016-08-28T12:00:00Z,45.395832,31.270834,162.8\n"
            + "2016-08-28T12:00:00Z,45.395832,72.93751,NaN\n"
            + "2016-08-28T12:00:00Z,45.395832,114.60418,NaN\n"
            + "2016-08-28T12:00:00Z,45.395832,156.27083,78.2\n"
            + "2016-08-28T12:00:00Z,3.7291667,-177.0625,NaN\n"
            + "2016-08-28T12:00:00Z,3.7291667,-135.39583,NaN\n"
            + "2016-08-28T12:00:00Z,3.7291667,-93.729164,NaN\n"
            + "2016-08-28T12:00:00Z,3.7291667,-52.062496,NaN\n"
            + "2016-08-28T12:00:00Z,3.7291667,-10.3958235,NaN\n"
            + "2016-08-28T12:00:00Z,3.7291667,31.270834,NaN\n"
            + "2016-08-28T12:00:00Z,3.7291667,72.93751,NaN\n"
            + "2016-08-28T12:00:00Z,3.7291667,114.60418,NaN\n"
            + "2016-08-28T12:00:00Z,3.7291667,156.27083,26.6\n"
            + "2016-08-28T12:00:00Z,-37.937504,-177.0625,NaN\n"
            + "2016-08-28T12:00:00Z,-37.937504,-135.39583,NaN\n"
            + "2016-08-28T12:00:00Z,-37.937504,-93.729164,NaN\n"
            + "2016-08-28T12:00:00Z,-37.937504,-52.062496,NaN\n"
            + "2016-08-28T12:00:00Z,-37.937504,-10.3958235,NaN\n"
            + "2016-08-28T12:00:00Z,-37.937504,31.270834,NaN\n"
            + "2016-08-28T12:00:00Z,-37.937504,72.93751,NaN\n"
            + "2016-08-28T12:00:00Z,-37.937504,114.60418,NaN\n"
            + "2016-08-28T12:00:00Z,-37.937504,156.27083,NaN\n"
            + "2016-08-28T12:00:00Z,-79.60418,-177.0625,NaN\n"
            + "2016-08-28T12:00:00Z,-79.60418,-135.39583,NaN\n"
            + "2016-08-28T12:00:00Z,-79.60418,-93.729164,NaN\n"
            + "2016-08-28T12:00:00Z,-79.60418,-52.062496,NaN\n"
            + "2016-08-28T12:00:00Z,-79.60418,-10.3958235,NaN\n"
            + "2016-08-28T12:00:00Z,-79.60418,31.270834,NaN\n"
            + "2016-08-28T12:00:00Z,-79.60418,72.93751,NaN\n"
            + "2016-08-28T12:00:00Z,-79.60418,114.60418,NaN\n"
            + "2016-08-28T12:00:00Z,-79.60418,156.27083,NaN\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // display an image
    String2.log("\n\n* PNG ");
    String baseName = eddGrid.className() + "_testMissingValue";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            "poc[(2016-08-28T12:00:00Z)][][]",
            Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR),
            baseName,
            ".png");
    // Test.displayInBrowser("file://" + tDir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    NcHelper.debugMode = oDebugMode;
  }
}
