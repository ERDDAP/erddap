/* 
 * EDDGridFromNcFilesUnpacked Copyright 2015, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.ShortArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.Projects;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.sgt.SgtUtil;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.text.MessageFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Get netcdf-X.X.XX.jar from
 * http://www.unidata.ucar.edu/software/thredds/current/netcdf-java/index.html
 * and copy it to <context>/WEB-INF/lib renamed as netcdf-latest.jar.
 * Get slf4j-jdk14.jar from 
 * ftp://ftp.unidata.ucar.edu/pub/netcdf-java/slf4j-jdk14.jar
 * and copy it to <context>/WEB-INF/lib.
 * Put both of these .jar files in the classpath for the compiler and for Java.
 */
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dods.*;
import ucar.nc2.util.*;
import ucar.ma2.*;

/** 
 * This class represents gridded data aggregated from a collection of 
 * NetCDF .nc (http://www.unidata.ucar.edu/software/netcdf/),
 * GRIB .grb (https://en.wikipedia.org/wiki/GRIB),
 * (and related) netcdfFiles which are unpacked at a low level.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2009-01-05
 */
public class EDDGridFromNcFilesUnpacked extends EDDGridFromNcLow { 

    /** subclasses have different classNames. */
    public String subClassName() {
        return "EDDGridFromNcFilesUnpacked";
    }

    /** 
     * Subclasses override this: 
     * EDDGridFromNcFilesUnpacked applies scale_factor and add_offset and
     * converts times variables to epochSeconds. */
    public boolean unpack() {
        return true;
    } 

    /** subclasses call lower version */
    public static String generateDatasetsXml(
        String tFileDir, String tFileNameRegex, String sampleFileName, 
        int tReloadEveryNMinutes, Attributes externalAddGlobalAttributes) throws Throwable {

        return generateDatasetsXml("EDDGridFromNcFilesUnpacked",
            tFileDir, tFileNameRegex, sampleFileName, 
            tReloadEveryNMinutes, externalAddGlobalAttributes);
    }
    
    /** The constructor just calls the super constructor. */
    public EDDGridFromNcFilesUnpacked(String tDatasetID, 
        String tAccessibleTo, String tGraphsAccessibleTo, boolean tAccessibleViaWMS,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object[][] tAxisVariables,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes, int tUpdateEveryNMillis,
        String tFileDir, String tFileNameRegex, 
        boolean tRecursive, String tPathRegex, String tMetadataFrom,
        int tMatchAxisNDigits, boolean tFileTableInMemory,
        boolean tAccessibleViaFiles) throws Throwable {

        super("EDDGridFromNcFilesUnpacked", tDatasetID, 
            tAccessibleTo, tGraphsAccessibleTo, tAccessibleViaWMS,
            tOnChange, tFgdcFile, tIso19115File, 
            tDefaultDataQuery, tDefaultGraphQuery, 
            tAddGlobalAttributes,
            tAxisVariables,
            tDataVariables,
            tReloadEveryNMinutes, tUpdateEveryNMillis,
            tFileDir, tFileNameRegex, tRecursive, tPathRegex, tMetadataFrom,
            tMatchAxisNDigits, tFileTableInMemory,
            tAccessibleViaFiles);
    }



    /** This tests generateDatasetsXml. 
     * @throws Throwable if touble
     */
    public static void testGenerateDatasetsXml() throws Throwable {

        String2.log("\n*** EDDGridFromNcFilesUnpacked.testGenerateDatasetsXml");

        String sampleDir = EDStatic.unitTestDataDir + "nc/";
        String sampleRegex = "scale_factor\\.nc";
        String sampleName = sampleDir + "scale_factor.nc";

        //test that sample file has short analysed_sst with scale_factor and add_offset
        String results = NcHelper.dumpString(sampleName, true); //short data
        String expected = 
"netcdf scale_factor.nc {\n" +
"  dimensions:\n" +
"    time = 2;\n" +
"    lat = 10;\n" +
"    lon = 10;\n" +
"  variables:\n" +
"    short analysed_sst(time=2, lat=10, lon=10);\n" +
"      :long_name = \"analysed sea surface temperature\";\n" +
"      :standard_name = \"sea_surface_foundation_temperature\";\n" +
"      :units = \"kelvin\";\n" +
"      :_FillValue = -32768S; // short\n" +
"      :add_offset = 298.15; // double\n" +
"      :scale_factor = 0.001; // double\n" +
"      :valid_min = -32767S; // short\n" +
"      :valid_max = 32767S; // short\n" +
"      :comment = \"Interim near-real-time (nrt) version; to be replaced by Final version\";\n" +
"      :coordinates = \"time lat lon \";\n" +
"\n" +
"    int time(time=2);\n" +
"      :long_name = \"reference time of sst field\";\n" +
"      :standard_name = \"time\";\n" +
"      :axis = \"T\";\n" +
"      :units = \"seconds since 1981-01-01 00:00:00 UTC\";\n" +
"      :_CoordinateAxisType = \"Time\";\n" +
"\n" +
"    float lat(lat=10);\n" +
"      :long_name = \"latitude\";\n" +
"      :standard_name = \"latitude\";\n" +
"      :axis = \"Y\";\n" +
"      :units = \"degrees_north\";\n" +
"      :valid_min = -90.0f; // float\n" +
"      :valid_max = 90.0f; // float\n" +
"      :_CoordinateAxisType = \"Lat\";\n" +
"\n" +
"    float lon(lon=10);\n" +
"      :long_name = \"longitude\";\n" +
"      :standard_name = \"longitude\";\n" +
"      :axis = \"X\";\n" +
"      :units = \"degrees_east\";\n" +
"      :valid_min = -180.0f; // float\n" +
"      :valid_max = 180.0f; // float\n" +
"      :_CoordinateAxisType = \"Lon\";\n" +
"\n" +
"  // global attributes:\n" +
"  :title = \"Daily MUR SST, Interim near-real-time (nrt) product\";\n" +
"  :comment = \"Interim-MUR(nrt) will be replaced by MUR-Final in about 3 days; MUR = \\\"Multi-scale Ultra-high Reolution\\\"; produced under NASA MEaSUREs program.\";\n" +
"  :Conventions = \"CF-1.0\";\n" +
"  :DSD_entry_id = \"JPL-L4UHfnd-GLOB-MUR\";\n" +
"  :references = \"ftp://mariana.jpl.nasa.gov/mur_sst/tmchin/docs/ATBD/\";\n" +
"  :source_data = \"AVHRR19_G-NAVO, AVHRR_METOP_A-EUMETSAT, MODIS_A-JPL, MODIS_T-JPL, WSAT-REMSS, iQUAM-NOAA/NESDIS, Ice_Conc-OSISAF\";\n" +
"  :institution = \"Jet Propulsion Laboratory\";\n" +
"  :contact = \"ghrsst@podaac.jpl.nasa.gov\";\n" +
"  :GDS_version_id = \"GDS-v1.0-rev1.6\";\n" +
"  :netcdf_version_id = \"3.5\";\n" +
"  :creation_date = \"2015-10-06\";\n" +
"  :product_version = \"04nrt\";\n" +
"  :history = \"Interim near-real-time (nrt) version created at nominal 1-day latency.\";\n" +
"  :spatial_resolution = \"0.011 degrees\";\n" +
"  :start_date = \"2015-10-05\";\n" +
"  :start_time = \"09:00:00 UTC\";\n" +
"  :stop_date = \"2015-10-05\";\n" +
"  :stop_time = \"09:00:00 UTC\";\n" +
"  :southernmost_latitude = -90.0f; // float\n" +
"  :northernmost_latitude = 90.0f; // float\n" +
"  :westernmost_longitude = -180.0f; // float\n" +
"  :easternmost_longitude = 180.0f; // float\n" +
"  :file_quality_index = \"0\";\n" +
"  :History = \"Translated to CF-1.0 Conventions by Netcdf-Java CDM (NetcdfCFWriter)\n" +
"Original Dataset = satellite/MUR/ssta/1day; Translation Date = Thu Oct 08 09:39:01 PDT 2015\";\n" +
" data:\n" +
"analysed_sst =\n" +
"  {\n" +
"    {\n" +
"      {1779, 1790, 1802, 1815, 1827, 1839, 1851, 1862, 1874, 1886},\n" +
"      {1782, 1792, 1804, 1816, 1828, 1840, 1851, 1863, 1875, 1887},\n" +
"      {1786, 1795, 1805, 1817, 1828, 1839, 1851, 1862, 1874, 1885},\n" +
"      {1789, 1798, 1807, 1817, 1828, 1838, 1849, 1860, 1871, 1882},\n" +
"      {1793, 1800, 1808, 1817, 1827, 1836, 1846, 1856, 1866, 1876},\n" +
"      {1795, 1801, 1809, 1816, 1825, 1833, 1842, 1851, 1859, 1868},\n" +
"      {1796, 1802, 1808, 1815, 1822, 1829, 1836, 1844, 1851, 1858},\n" +
"      {1797, 1801, 1807, 1812, 1818, 1824, 1830, 1836, 1842, 1848},\n" +
"      {1796, 1800, 1804, 1809, 1813, 1818, 1822, 1827, 1832, 1836},\n" +
"      {1794, 1797, 1801, 1804, 1807, 1811, 1814, 1817, 1821, 1824}\n" +
"    },\n" +
"    {\n" +
"      {1773, 1777, 1782, 1787, 1792, 1798, 1803, 1809, 1815, 1821},\n" +
"      {1776, 1780, 1784, 1789, 1794, 1800, 1805, 1811, 1817, 1823},\n" +
"      {1778, 1782, 1787, 1792, 1797, 1802, 1807, 1813, 1819, 1825},\n" +
"      {1781, 1785, 1789, 1794, 1799, 1804, 1809, 1815, 1820, 1826},\n" +
"      {1783, 1787, 1791, 1796, 1800, 1805, 1810, 1816, 1821, 1826},\n" +
"      {1786, 1789, 1793, 1798, 1802, 1806, 1811, 1816, 1821, 1826},\n" +
"      {1788, 1791, 1795, 1799, 1803, 1807, 1812, 1816, 1821, 1825},\n" +
"      {1790, 1793, 1796, 1800, 1804, 1807, 1811, 1815, 1820, 1824},\n" +
"      {1791, 1794, 1797, 1800, 1804, 1807, 1811, 1814, 1818, 1822},\n" +
"      {1792, 1795, 1797, 1800, 1803, 1806, 1809, 1812, 1816, 1819}\n" +
"    }\n" +
"  }\n" +
"time =\n" +
"  {1096880400, 1096966800}\n" +
"lat =\n" +
"  {20.0006, 20.0116, 20.0226, 20.0336, 20.0446, 20.0555, 20.0665, 20.0775, 20.0885, 20.0995}\n" +
"lon =\n" +
"  {-134.995, -134.984, -134.973, -134.962, -134.951, -134.94, -134.929, -134.918, -134.907, -134.896}\n" +
"}\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //scale_factor.nc analysed_sst is short with scale_factor and add offset.
        //  Loading via openDataset tells netcdf-java to unpack the variable
        //  so here it appears not to be double var with no scale_factor or add_offset
        results = generateDatasetsXml(
            sampleDir, sampleRegex, sampleName,
            DEFAULT_RELOAD_EVERY_N_MINUTES, null) + "\n";
        String suggDatasetID = suggestDatasetID(sampleDir + sampleRegex);

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDGridFromNcFilesUnpacked",
            sampleDir, sampleRegex, sampleName,
            "" + DEFAULT_RELOAD_EVERY_N_MINUTES},
            false); //doIt loop?
        Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt. " + 
            gdxResults.length() + " " + results.length());

        expected = 
directionsForGenerateDatasetsXml() +
"-->\n" +
"\n" +
"<dataset type=\"EDDGridFromNcFilesUnpacked\" datasetID=\"" + suggDatasetID + "\" active=\"true\">\n" +
"    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>" + EDStatic.unitTestDataDir + "nc/</fileDir>\n" +
"    <fileNameRegex>scale_factor\\.nc</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <matchAxisNDigits>20</matchAxisNDigits>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <accessibleViaFiles>false</accessibleViaFiles>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"comment\">Interim-MUR(nrt) will be replaced by MUR-Final in about 3 days; MUR = &quot;Multi-scale Ultra-high Reolution&quot;; produced under NASA MEaSUREs program.</att>\n" +
"        <att name=\"contact\">ghrsst@podaac.jpl.nasa.gov</att>\n" +
"        <att name=\"Conventions\">CF-1.0</att>\n" +
"        <att name=\"creation_date\">2015-10-06</att>\n" +
"        <att name=\"DSD_entry_id\">JPL-L4UHfnd-GLOB-MUR</att>\n" +
"        <att name=\"easternmost_longitude\" type=\"float\">180.0</att>\n" +
"        <att name=\"file_quality_index\">0</att>\n" +
"        <att name=\"GDS_version_id\">GDS-v1.0-rev1.6</att>\n" +
"        <att name=\"History\">Translated to CF-1.0 Conventions by Netcdf-Java CDM (NetcdfCFWriter)\n" +
"Original Dataset = satellite/MUR/ssta/1day; Translation Date = Thu Oct 08 09:39:01 PDT 2015</att>\n" +
"        <att name=\"history\">Interim near-real-time (nrt) version created at nominal 1-day latency.</att>\n" +
"        <att name=\"institution\">Jet Propulsion Laboratory</att>\n" +
"        <att name=\"netcdf_version_id\">3.5</att>\n" +
"        <att name=\"northernmost_latitude\" type=\"float\">90.0</att>\n" +
"        <att name=\"product_version\">04nrt</att>\n" +
"        <att name=\"references\">ftp://mariana.jpl.nasa.gov/mur_sst/tmchin/docs/ATBD/</att>\n" +
"        <att name=\"source_data\">AVHRR19_G-NAVO, AVHRR_METOP_A-EUMETSAT, MODIS_A-JPL, MODIS_T-JPL, WSAT-REMSS, iQUAM-NOAA/NESDIS, Ice_Conc-OSISAF</att>\n" +
"        <att name=\"southernmost_latitude\" type=\"float\">-90.0</att>\n" +
"        <att name=\"spatial_resolution\">0.011 degrees</att>\n" +
"        <att name=\"start_date\">2015-10-05</att>\n" +
"        <att name=\"start_time\">09:00:00 UTC</att>\n" +
"        <att name=\"stop_date\">2015-10-05</att>\n" +
"        <att name=\"stop_time\">09:00:00 UTC</att>\n" +
"        <att name=\"title\">Daily MUR SST, Interim near-real-time (nrt) product</att>\n" +
"        <att name=\"westernmost_longitude\" type=\"float\">-180.0</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"Conventions\">CF-1.6, COARDS, ACDD-1.3</att>\n" +
"        <att name=\"creator_email\">ghrsst@podaac.jpl.nasa.gov</att>\n" +
"        <att name=\"creator_name\">GHRSST</att>\n" +
"        <att name=\"creator_url\">https://podaac.jpl.nasa.gov/</att>\n" +
"        <att name=\"easternmost_longitude\">null</att>\n" +
"        <att name=\"GDS_version_id\">null</att>\n" +
"        <att name=\"History\">null</att>\n" +
"        <att name=\"infoUrl\">https://podaac.jpl.nasa.gov/</att>\n" +
"        <att name=\"keywords\">analysed, analysed_sst, daily, data, day, earth, environments, foundation, high, interim, jet, laboratory, making, measures, multi, multi-scale, mur, near, near real time, near-real-time, nrt, ocean, oceans,\n" +
"Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature,\n" +
"product, propulsion, real, records, research, resolution, scale, sea, sea_surface_foundation_temperature, sst, surface, system, temperature, time, ultra, ultra-high, use</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"northernmost_latitude\">null</att>\n" +
"        <att name=\"southernmost_latitude\">null</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v29</att>\n" +
"        <att name=\"start_date\">null</att>\n" +
"        <att name=\"start_time\">null</att>\n" +
"        <att name=\"stop_date\">null</att>\n" +
"        <att name=\"stop_time\">null</att>\n" +
"        <att name=\"summary\">Interim-Multi-scale Ultra-high Resolution (MUR)(nrt) will be replaced by MUR-Final in about 3 days; MUR = &quot;Multi-scale Ultra-high Reolution&quot;; produced under NASA Making Earth System Data Records for Use in Research Environments (MEaSUREs) program.</att>\n" +
"        <att name=\"westernmost_longitude\">null</att>\n" +
"    </addAttributes>\n" +
"    <axisVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Time</att>\n" +
"            <att name=\"axis\">T</att>\n" +
"            <att name=\"long_name\">reference time of sst field</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>lat</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Lat</att>\n" +
"            <att name=\"axis\">Y</att>\n" +
"            <att name=\"long_name\">latitude</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"            <att name=\"valid_max\" type=\"float\">90.0</att>\n" +
"            <att name=\"valid_min\" type=\"float\">-90.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>lon</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Lon</att>\n" +
"            <att name=\"axis\">X</att>\n" +
"            <att name=\"long_name\">longitude</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"            <att name=\"valid_max\" type=\"float\">180.0</att>\n" +
"            <att name=\"valid_min\" type=\"float\">-180.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>analysed_sst</sourceName>\n" +
"        <destinationName>analysed_sst</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"double\">NaN</att>\n" +
"            <att name=\"comment\">Interim near-real-time (nrt) version; to be replaced by Final version</att>\n" +
"            <att name=\"coordinates\">time lat lon </att>\n" +
"            <att name=\"long_name\">analysed sea surface temperature</att>\n" +
"            <att name=\"standard_name\">sea_surface_foundation_temperature</att>\n" +
"            <att name=\"units\">kelvin</att>\n" +
"            <att name=\"valid_max\" type=\"double\">330.917</att>\n" +
"            <att name=\"valid_min\" type=\"double\">265.383</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">305.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">273.0</att>\n" +
"            <att name=\"coordinates\">null</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";
        Test.ensureEqual(results, expected, results.length() + " " + expected.length() + 
            "\nresults=\n" + results);

        //ensure it is ready-to-use by making a dataset from it
        EDD edd = oneFromXmlFragment(null, results);
        Test.ensureEqual(edd.datasetID(), suggDatasetID, "");
        Test.ensureEqual(edd.title(), "Daily MUR SST, Interim near-real-time (nrt) product", "");
        Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
            "analysed_sst", "");

        String2.log("\nEDDGridFromNcFilesUnpacked.testGenerateDatasetsXml passed the test.");
    }


    /**
     *
     * @throws Throwable if trouble
     */
    public static void testBasic(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDGridFromNcFilesUnpacked.testBasic() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
        try {

        //generateDatasetsXml        
        String id = "testEDDGridFromNcFilesUnpacked";
        if (deleteCachedDatasetInfo) 
            deleteCachedDatasetInfo(id);
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, id); 

        //*** test getting das for entire dataset
        String2.log("\n*** test das dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className(), ".das"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.4440356e+9, 1.444122e+9;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"reference time of sst field\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range 20.0006, 20.0995;\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"    Float32 valid_max 90.0;\n" +
"    Float32 valid_min -90.0;\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 actual_range -134.995, -134.896;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"    Float32 valid_max 180.0;\n" +
"    Float32 valid_min -180.0;\n" +
"  }\n" +
"  analysed_sst {\n" +
"    Float64 _FillValue NaN;\n" +
"    Float64 colorBarMaximum 305.0;\n" +
"    Float64 colorBarMinimum 273.0;\n" +
"    String comment \"Interim near-real-time (nrt) version; to be replaced by Final version\";\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"analysed sea surface temperature\";\n" +
"    String standard_name \"sea_surface_foundation_temperature\";\n" +
"    String units \"kelvin\";\n" +
"    Float64 valid_max 330.917;\n" +
"    Float64 valid_min 265.383;\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String comment \"Interim-MUR(nrt) will be replaced by MUR-Final in about 3 days; MUR = \\\"Multi-scale Ultra-high Reolution\\\"; produced under NASA MEaSUREs program.\";\n" +
"    String contact \"ghrsst@podaac.jpl.nasa.gov\";\n" +
"    String Conventions \"CF-1.6, COARDS, ACDD-1.3\";\n" +
"    String creation_date \"2015-10-06\";\n" +
"    String creator_email \"ghrsst@podaac.jpl.nasa.gov\";\n" +
"    String creator_name \"GHRSST\";\n" +
"    String creator_url \"https://podaac.jpl.nasa.gov/\";\n" +
"    String DSD_entry_id \"JPL-L4UHfnd-GLOB-MUR\";\n" +
"    Float64 Easternmost_Easting -134.896;\n" +
"    String file_quality_index \"0\";\n" +
"    Float64 geospatial_lat_max 20.0995;\n" +
"    Float64 geospatial_lat_min 20.0006;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max -134.896;\n" +
"    Float64 geospatial_lon_min -134.995;\n" +
"    Float64 geospatial_lon_resolution 0.011000000000001996;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String history \"Interim near-real-time (nrt) version created at nominal 1-day latency.\n" +
today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);

//T22:27:15Z (local files)
//2015-10-08T22:27:15Z  

expected = 
"http://localhost:8080/cwexperimental/griddap/testEDDGridFromNcFilesUnpacked.das\";\n" +
"    String infoUrl \"https://podaac.jpl.nasa.gov/\";\n" +
"    String institution \"Jet Propulsion Laboratory\";\n" +
"    String keywords \"analysed, analysed_sst, daily, data, day, earth, environments, foundation, high, interim, jet, laboratory, making, measures, multi, multi-scale, mur, near, near real time, near-real-time, nrt, ocean, oceans,\n" +
"Oceans > Ocean Temperature > Sea Surface Temperature,\n" +
"product, propulsion, real, records, research, resolution, scale, sea, sea_surface_foundation_temperature, sst, surface, system, temperature, time, ultra, ultra-high, use\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String netcdf_version_id \"3.5\";\n" +
"    Float64 Northernmost_Northing 20.0995;\n" +
"    String product_version \"04nrt\";\n" +
"    String references \"ftp://mariana.jpl.nasa.gov/mur_sst/tmchin/docs/ATBD/\";\n" +
"    String source_data \"AVHRR19_G-NAVO, AVHRR_METOP_A-EUMETSAT, MODIS_A-JPL, MODIS_T-JPL, WSAT-REMSS, iQUAM-NOAA/NESDIS, Ice_Conc-OSISAF\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing 20.0006;\n" +
"    String spatial_resolution \"0.011 degrees\";\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
"    String summary \"Interim-Multi-scale Ultra-high Resolution (MUR)(nrt) will be replaced by MUR-Final in about 3 days; MUR = \\\"Multi-scale Ultra-high Reolution\\\"; produced under NASA Making Earth System Data Records for Use in Research Environments (MEaSUREs) program.\";\n" +
"    String time_coverage_end \"2015-10-06T09:00:00Z\";\n" +
"    String time_coverage_start \"2015-10-05T09:00:00Z\";\n" +
"    String title \"Daily MUR SST, Interim near-real-time (nrt) product\";\n" +
"    Float64 Westernmost_Easting -134.995;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        tResults = results.substring(tPo, Math.min(results.length(), tPo +  expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className(), ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Float64 time[time = 2];\n" +
"  Float32 latitude[latitude = 10];\n" +
"  Float32 longitude[longitude = 10];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 analysed_sst[time = 2][latitude = 10][longitude = 10];\n" +
"    MAPS:\n" +
"      Float64 time[time = 2];\n" +
"      Float32 latitude[latitude = 10];\n" +
"      Float32 longitude[longitude = 10];\n" +
"  } analysed_sst;\n" +
"} testEDDGridFromNcFilesUnpacked;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv  with data from one file
        String2.log("\n*** test read from one file\n");       
        userDapQuery = "analysed_sst[0][0:2:6][0:2:6]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className(), ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"time,latitude,longitude,analysed_sst\n" +
"UTC,degrees_north,degrees_east,kelvin\n" +
"2015-10-05T09:00:00Z,20.0006,-134.995,299.929\n" +  //note double values (Kelvin)
"2015-10-05T09:00:00Z,20.0006,-134.973,299.952\n" +
"2015-10-05T09:00:00Z,20.0006,-134.951,299.977\n" +
"2015-10-05T09:00:00Z,20.0006,-134.929,300.001\n" +
"2015-10-05T09:00:00Z,20.0226,-134.995,299.936\n" +
"2015-10-05T09:00:00Z,20.0226,-134.973,299.955\n" +
"2015-10-05T09:00:00Z,20.0226,-134.951,299.97799999999995\n" +
"2015-10-05T09:00:00Z,20.0226,-134.929,300.001\n" +
"2015-10-05T09:00:00Z,20.0446,-134.995,299.943\n" +
"2015-10-05T09:00:00Z,20.0446,-134.973,299.95799999999997\n" +
"2015-10-05T09:00:00Z,20.0446,-134.951,299.977\n" +
"2015-10-05T09:00:00Z,20.0446,-134.929,299.996\n" +
"2015-10-05T09:00:00Z,20.0665,-134.995,299.94599999999997\n" +
"2015-10-05T09:00:00Z,20.0665,-134.973,299.95799999999997\n" +
"2015-10-05T09:00:00Z,20.0665,-134.951,299.972\n" +
"2015-10-05T09:00:00Z,20.0665,-134.929,299.986\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //  */
        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t)); 
        }
    }

    /**
     * Test file created from 
     * http://thredds.jpl.nasa.gov/thredds/ncss/grid/ncml_aggregation/OceanTemperature/modis/aqua/11um/9km/aggregate__MODIS_AQUA_L3_SST_THERMAL_8DAY_9KM_DAYTIME.ncml/dataset.html
     * and stored in /erddapTest/unsigned/
     *
     * @throws Throwable if trouble
     */
    public static void testUInt16File() throws Throwable {
        String2.log("\n*** testUInt16File");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery;
        String today = Calendar2.getCurrentISODateTimeStringZulu() + "Z";
        String fileDir = EDStatic.unitTestDataDir + "unsigned/";
        String fileName = "9km_aggregate__MODIS_AQUA_L3_SST_THERMAL_8DAY_9KM_DAYTIME.nc";
        boolean oDebugMode = NcHelper.debugMode;
NcHelper.debugMode = true;

        //DumpString
        results = NcHelper.dumpString(fileDir + fileName, false);
        expected = 
"netcdf 9km_aggregate__MODIS_AQUA_L3_SST_THERMAL_8DAY_9KM_DAYTIME.nc {\n" +
"  dimensions:\n" +
"    time = 1;\n" +
"    lat = 2160;\n" +
"    lon = 25;\n" +
"  variables:\n" +
"    short l3m_data(time=1, lat=2160, lon=25);\n" +
"      :_Unsigned = \"true\";\n" +
"      :long_name = \"l3m_data\";\n" +
"      :scale_factor = 7.17185E-4f; // float\n" + //32768-> 23.50071808, so, many data values are higher
"      :add_offset = -2.0f; // float\n" +
"      :_FillValue = -1S; // short\n" + //wrong: cf says it should be actual value: 65535(int)
"      :Scaling = \"linear\";\n" +
"      :Scaling_Equation = \"(Slope*l3m_data) + Intercept = Parameter value\";\n" +
"      :Slope = 7.17185E-4f; // float\n" +
"      :Intercept = -2.0f; // float\n" +
"      :coordinates = \"time Number_of_Lines Number_of_Columns lat lon\";\n" +
"\n" +
"    int time(time=1);\n" +
"      :standard_name = \"time\";\n" +
"      :axis = \"T\";\n" +
"      :units = \"days since 2002-01-01\";\n" +
"      :_CoordinateAxisType = \"Time\";\n" +
"\n" +
"    float Number_of_Lines(lat=2160);\n" + //note that ncss knows this is lat, but didn't rename it
"      :long_name = \"latitude\";\n" +
"      :units = \"degrees_north\";\n" +
"      :_CoordinateAxisType = \"Lat\";\n" +
"      :standard_name = \"latitude\";\n" +
"\n" +
"    float Number_of_Columns(lon=25);\n" + //note that ncss knows this is lon, but didn't rename it
"      :long_name = \"longitude\";\n" +
"      :units = \"degrees_east\";\n" +
"      :_CoordinateAxisType = \"Lon\";\n" +
"      :standard_name = \"longitude\";\n" +
"\n" +
"    byte l3m_qual(time=1, lat=2160, lon=25);\n" +
"      :_Unsigned = \"true\";\n" +
"      :long_name = \"l3m_qual\";\n" +
"      :scale_factor = 7.17185E-4f; // float\n" +  //I suspect that's wrong
"      :add_offset = -2.0f; // float\n" +          //I suspect that's wrong
"      :valid_range = 0, 2; // int\n" +
"      :coordinates = \"time Number_of_Lines Number_of_Columns lat lon\";\n" + //that's bizarre
"\n" +
"  // global attributes:\n" +
"  :Product_Name = \"A20092652009272.L3m_8D_SST_9\";\n" +
"  :Sensor_Name = \"MODISA\";\n" +
"  :Sensor = \"\";\n" +
"  :Title = \"MODISA Level-3 Standard Mapped Image\";\n" +
"  :Data_Center = \"\";\n" +
"  :Station_Name = \"\";\n" +
"  :Station_Latitude = 0.0f; // float\n" +
"  :Station_Longitude = 0.0f; // float\n" +
"  :Mission = \"\";\n" +
"  :Mission_Characteristics = \"\";\n" +
"  :Sensor_Characteristics = \"\";\n" +
"  :Product_Type = \"8-day\";\n" +
"  :Replacement_Flag = \"ORIGINAL\";\n" +
"  :Software_Name = \"smigen\";\n" +
"  :Software_Version = \"4.0\";\n" +
"  :Processing_Time = \"2009282201111000\";\n" +
"  :Input_Files = \"A20092652009272.L3b_8D_SST.main\";\n" +
"  :Processing_Control = \"smigen par=A20092652009272.L3m_8D_SST_9.param\";\n" +
"  :Input_Parameters = \"IFILE = /data3/sdpsoper/vdc/vpu2/workbuf/A20092652009272.L3b_8D_SST.main|OFILE = A20092652009272.L3m_8D_SST_9|PFILE = |PROD = sst|PALFILE = DEFAULT|RFLAG = ORIGINAL|MEAS = 1|STYPE = 0|DATAMIN = 0.000000|DATAMAX = 0.000000|LONWEST = -180.000000|LONEAST = 180.000000|LATSOUTH = -90.000000|LATNORTH = 90.000000|RESOLUTION = 9km|PROJECTION = RECT|GAP_FILL = 0|SEAM_LON = -180.000000|PRECISION=I\";\n" +
"  :L2_Flag_Names = \"LAND,HISOLZ\";\n" +
"  :Period_Start_Year = 2009S; // short\n" +
"  :Period_Start_Day = 265S; // short\n" +
"  :Period_End_Year = 2009S; // short\n" +
"  :Period_End_Day = 270S; // short\n" +
"  :Start_Time = \"2009265000008779\";\n" +
"  :End_Time = \"2009271030006395\";\n" +
"  :Start_Year = 2009S; // short\n" +
"  :Start_Day = 265S; // short\n" +
"  :Start_Millisec = 8779; // int\n" +
"  :End_Year = 2009S; // short\n" +
"  :End_Day = 271S; // short\n" +
"  :End_Millisec = 10806395; // int\n" +
"  :Start_Orbit = 0; // int\n" +
"  :End_Orbit = 0; // int\n" +
"  :Orbit = 0; // int\n" +
"  :Map_Projection = \"Equidistant Cylindrical\";\n" +
"  :Latitude_Units = \"degrees North\";\n" +
"  :Longitude_Units = \"degrees East\";\n" +
"  :Northernmost_Latitude = 90.0f; // float\n" +
"  :Southernmost_Latitude = -90.0f; // float\n" +
"  :Westernmost_Longitude = -180.0f; // float\n" +
"  :Easternmost_Longitude = 180.0f; // float\n" +
"  :Latitude_Step = 0.083333336f; // float\n" +
"  :Longitude_Step = 0.083333336f; // float\n" +
"  :SW_Point_Latitude = -89.958336f; // float\n" +
"  :SW_Point_Longitude = -179.95833f; // float\n" +
"  :Data_Bins = 14234182; // int\n" +
"  :Number_of_Lines = 2160; // int\n" +
"  :Number_of_Columns = 4320; // int\n" +
"  :Parameter = \"Sea Surface Temperature\";\n" +
"  :Measure = \"Mean\";\n" +
"  :Units = \"deg-C\";\n" +
"  :Scaling = \"linear\";\n" +
"  :Scaling_Equation = \"(Slope*l3m_data) + Intercept = Parameter value\";\n" +
"  :Slope = 7.17185E-4f; // float\n" +
"  :Intercept = -2.0f; // float\n" +
"  :Scaled_Data_Minimum = -2.0f; // float\n" +
"  :Scaled_Data_Maximum = 45.0f; // float\n" +
"  :Data_Minimum = -1.999999f; // float\n" +
"  :Data_Maximum = 36.915f; // float\n" +
"  :start_date = \"2002-07-04 UTC\";\n" +
"  :start_time = \"00:00:00 UTC\";\n" +
"  :stop_date = \"2015-03-06 UTC\";\n" +
"  :stop_time = \"23:59:59 UTC\";\n" +
"  :Conventions = \"CF-1.0\";\n" +
"  :History = \"Translated to CF-1.0 Conventions by Netcdf-Java CDM (NetcdfCFWriter)\n" +
"Original Dataset = file:/usr/ftp/ncml/catalog_ncml/OceanTemperature/modis/aqua/11um/9km/aggregate__MODIS_AQUA_L3_SST_THERMAL_8DAY_9KM_DAYTIME.ncml; Translation Date = Fri Oct 30 09:44:07 GMT-08:00 2015\";\n" +
"  :geospatial_lat_min = -89.95833587646484; // double\n" +
"  :geospatial_lat_max = 89.95833587646484; // double\n" +
"  :geospatial_lon_min = -136.04165649414062; // double\n" +
"  :geospatial_lon_max = -134.04165649414062; // double\n" +
" data:\n" +
"}\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //one time
        if (false)
            String2.log(generateDatasetsXml(fileDir, fileName, fileDir + fileName,
                DEFAULT_RELOAD_EVERY_N_MINUTES, null));        

        //ensure files are reread
        File2.deleteAllFiles(datasetDir("testUInt16FileUnpacked"));
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, "testUInt16FileUnpacked"); 
        //in uaf erddap, this is nasa_jpl_c688_be2f_cf9d

//re-pack apparent missing value
//45.000717 +2=> 47.000717 /7.17185E-4=> 65535

        //.das     das isn't affected by userDapQuery
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", 
            EDStatic.fullTestCacheDirectory, eddGrid.className(), ".das"); 
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
        expected = 
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.0257408e+9, 1.0257408e+9;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range 89.95834, -89.95834;\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 actual_range -136.0417, -134.0417;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  sst {\n" +
"    Float32 _FillValue NaN;\n" +   //important test of UInt16 and Unpacked
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Sea Surface Temperature\";\n" +
"    String standard_name \"sea_surface_temperature\";\n" +
"    String units \"deg_C\";\n" +
"  }\n" +
"  sst_quality {\n" +
"    Float64 colorBarMaximum 150.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Sea Surface Temperature Quality\";\n" +
"    String units \"deg_C\";\n" +   //??? did ERDDAP add that?
"    Float32 valid_range -2.0, -1.9985657;\n" + //unpacking did that
"  }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String Conventions \"CF-1.6, COARDS, ACDD-1.3\";\n" +
"    Float64 Easternmost_Easting -134.0417;\n" +
"    Float64 geospatial_lat_max 89.95834;\n" +
"    Float64 geospatial_lat_min -89.95834;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max -134.0417;\n" +
"    Float64 geospatial_lon_min -136.0417;\n" +
"    Float64 geospatial_lon_resolution 0.08333333333333333;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String history \"Translated to CF-1.0 Conventions by Netcdf-Java CDM (NetcdfCFWriter)\n" +
"Original Dataset = file:/usr/ftp/ncml/catalog_ncml/OceanTemperature/modis/aqua/11um/9km/aggregate__MODIS_AQUA_L3_SST_THERMAL_8DAY_9KM_DAYTIME.ncml; Translation Date = Fri Oct 30 09:44:07 GMT-08:00 2015\n";
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
       
expected = 
//"2015-10-30T18:17:10Z (local files)
//2015-10-30T18:17:10Z http://localhost:8080/cwexperimental/griddap/testUInt16File.das";
"    String infoUrl \"???\";\n" +
"    String Input_Files \"A20092652009272.L3b_8D_SST.main\";\n" +
"    String Input_Parameters \"IFILE = /data3/sdpsoper/vdc/vpu2/workbuf/A20092652009272.L3b_8D_SST.main|OFILE = A20092652009272.L3m_8D_SST_9|PFILE = |PROD = sst|PALFILE = DEFAULT|RFLAG = ORIGINAL|MEAS = 1|STYPE = 0|DATAMIN = 0.000000|DATAMAX = 0.000000|LONWEST = -180.000000|LONEAST = 180.000000|LATSOUTH = -90.000000|LATNORTH = 90.000000|RESOLUTION = 9km|PROJECTION = RECT|GAP_FILL = 0|SEAM_LON = -180.000000|PRECISION=I\";\n" +
"    String institution \"???\";\n" +
"    String keywords \"aqua, data, image, imaging, L3, l3m_data, l3m_qual, mapped, moderate, modis, modis a, ocean, oceans,\n" +
"Oceans > Ocean Temperature > Sea Surface Temperature,\n" +
"quality, resolution, sea, sea_surface_temperature, smi, spectroradiometer, standard, surface, temperature, time\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String L2_Flag_Names \"LAND,HISOLZ\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String Map_Projection \"Equidistant Cylindrical\";\n" +
"    String Measure \"Mean\";\n" +
"    Float64 Northernmost_Northing 89.95834;\n" +
"    String Processing_Control \"smigen par=A20092652009272.L3m_8D_SST_9.param\";\n" +
"    String Processing_Time \"2009282201111000\";\n" +
"    String Product_Name \"A20092652009272.L3m_8D_SST_9\";\n" +
"    String Product_Type \"8-day\";\n" +
"    String Replacement_Flag \"ORIGINAL\";\n" +
"    Float32 Scaled_Data_Maximum 45.0;\n" +
"    Float32 Scaled_Data_Minimum -2.0;\n" +
"    String Sensor_Name \"MODISA\";\n" +
"    String Software_Name \"smigen\";\n" +
"    String Software_Version \"4.0\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing -89.95834;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
"    String summary \"Moderate Resolution Imaging Spectroradiometer on Aqua (MODISA) Level-3 Standard Mapped Image\";\n" +
"    String time_coverage_end \"2002-07-04T00:00:00Z\";\n" +
"    String time_coverage_start \"2002-07-04T00:00:00Z\";\n" +
"    String title \"MODISA L3 SMI,\";\n" +
"    Float64 Westernmost_Easting -136.0417;\n" +
"  }\n" +
"}\n";
        int tpo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tpo >= 0, "tpo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tpo, Math.min(results.length(), tpo + expected.length())),
            expected, "results=\n" + results);


        //.dds     dds isn't affected by userDapQuery
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", 
            EDStatic.fullTestCacheDirectory, eddGrid.className(), ".dds"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = //difference from testUInt16Dap: lat lon are float here, not double
"Dataset {\n" +
"  Float64 time[time = 1];\n" +
"  Float32 latitude[latitude = 2160];\n" +
"  Float32 longitude[longitude = 25];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 sst[time = 1][latitude = 2160][longitude = 25];\n" +
"    MAPS:\n" +
"      Float64 time[time = 1];\n" +
"      Float32 latitude[latitude = 2160];\n" +
"      Float32 longitude[longitude = 25];\n" +
"  } sst;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 sst_quality[time = 1][latitude = 2160][longitude = 25];\n" +
"    MAPS:\n" +
"      Float64 time[time = 1];\n" +
"      Float32 latitude[latitude = 2160];\n" +
"      Float32 longitude[longitude = 25];\n" +
"  } sst_quality;\n" +
"} testUInt16FileUnpacked;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv data values
        userDapQuery = "sst[0][0:100:2159][(-134.95833513)]"; 
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, eddGrid.className(), ".csv"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        String2.log(results);
        expected = //difference from testUInt16Dap: lat lon are float here, not double
"time,latitude,longitude,sst\n" +
"UTC,degrees_north,degrees_east,deg_C\n" +
"2002-07-04T00:00:00Z,89.958336,-134.95833,-0.84102905\n" +
"2002-07-04T00:00:00Z,81.62501,-134.95833,-1.6371044\n" +
"2002-07-04T00:00:00Z,73.291664,-134.95833,-0.11021753\n" +
"2002-07-04T00:00:00Z,64.958336,-134.95833,NaN\n" + //shows _FillValue's correctly caught
"2002-07-04T00:00:00Z,56.625008,-134.95833,NaN\n" +
"2002-07-04T00:00:00Z,48.291664,-134.95833,12.6406145\n" +
"2002-07-04T00:00:00Z,39.958336,-134.95833,17.95137\n" +
"2002-07-04T00:00:00Z,31.625,-134.95833,20.432829\n" +  
"2002-07-04T00:00:00Z,23.291664,-134.95833,19.664007\n" +
"2002-07-04T00:00:00Z,14.958336,-134.95833,24.482773\n" + //>23.5 shows unsigned values correctly caught
"2002-07-04T00:00:00Z,6.625,-134.95833,29.068455\n" +
"2002-07-04T00:00:00Z,-1.7083359,-134.95833,27.240349\n" +
"2002-07-04T00:00:00Z,-10.041664,-134.95833,27.210228\n" +
"2002-07-04T00:00:00Z,-18.375,-134.95833,26.713936\n" +
"2002-07-04T00:00:00Z,-26.708336,-134.95833,21.580326\n" +
"2002-07-04T00:00:00Z,-35.041668,-134.95833,15.789774\n" +
"2002-07-04T00:00:00Z,-43.375,-134.95833,NaN\n" +
"2002-07-04T00:00:00Z,-51.708336,-134.95833,6.1673026\n" +
"2002-07-04T00:00:00Z,-60.041668,-134.95833,0.40400413\n" +
"2002-07-04T00:00:00Z,-68.375,-134.95833,NaN\n" +
"2002-07-04T00:00:00Z,-76.708336,-134.95833,NaN\n" +
"2002-07-04T00:00:00Z,-85.04167,-134.95833,NaN\n"; 
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //display the image
        String2.log("\n\n* PNG ");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "sst[0][][]&.land=under", 
            EDStatic.fullTestCacheDirectory, eddGrid.className() + "_UInt16_Map", ".png"); 
        SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

        NcHelper.debugMode = oDebugMode;
    }



    /**
     * This tests this class.
     *
     * @throws Throwable if trouble
     */
    public static void test(boolean deleteCachedDatasetInfo) throws Throwable {
/* 
        testGenerateDatasetsXml();
        testBasic(deleteCachedDatasetInfo);
   */     testUInt16File();
    }


}

