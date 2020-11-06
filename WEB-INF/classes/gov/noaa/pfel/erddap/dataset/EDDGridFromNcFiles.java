/* 
 * EDDGridFromNcFiles Copyright 2009, NOAA.
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
import gov.noaa.pfel.coastwatch.sgt.SgtMap;
import gov.noaa.pfel.coastwatch.sgt.SgtUtil;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Get netcdfAll-......jar from ftp://ftp.unidata.ucar.edu/pub
 * and copy it to <context>/WEB-INF/lib renamed as netcdf-latest.jar.
 * Put it in the classpath for the compiler and for Java.
 */
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
//import ucar.nc2.dods.*;
import ucar.nc2.util.*;
import ucar.ma2.*;

/** 
 * This class represents gridded data aggregated from a collection of 
 * NetCDF .nc (https://www.unidata.ucar.edu/software/netcdf/),
 * GRIB .grb (https://en.wikipedia.org/wiki/GRIB),
 * (and related) NetcdfFiles.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2009-01-05
 */
public class EDDGridFromNcFiles extends EDDGridFromNcLow { 

    /** subclasses have different classNames. */
    public String subClassName() {
        return "EDDGridFromNcFiles";
    }
    
    /** 
     * Subclasses overwrite this: 
     * EDDGridFromNcFilesUnpacked applies scale_factor and add_offset and
     * converts times variables to epochSeconds. */
    public boolean unpack() {
        return false;
    } 

    /** subclasses call lower version */
    public static String generateDatasetsXml(
        String tFileDir, String tFileNameRegex, String sampleFileName, 
        String tGroup, String tDimensionsCSV, int tReloadEveryNMinutes, String tCacheFromUrl,
        Attributes externalAddGlobalAttributes) throws Throwable {

        return generateDatasetsXml("EDDGridFromNcFiles",
            tFileDir, tFileNameRegex, sampleFileName, 
            tGroup, tDimensionsCSV, tReloadEveryNMinutes, tCacheFromUrl, 
            externalAddGlobalAttributes);
    }
    
    /** The constructor just calls the super constructor. */
    public EDDGridFromNcFiles(String tDatasetID, 
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
        boolean tAccessibleViaFiles, int tnThreads, boolean tDimensionValuesInMemory, 
        String tCacheFromUrl, int tCacheSizeGB, String tCachePartialPathRegex) 
        throws Throwable {

        super("EDDGridFromNcFiles", tDatasetID, 
            tAccessibleTo, tGraphsAccessibleTo, tAccessibleViaWMS,
            tOnChange, tFgdcFile, tIso19115File, 
            tDefaultDataQuery, tDefaultGraphQuery, 
            tAddGlobalAttributes,
            tAxisVariables,
            tDataVariables,
            tReloadEveryNMinutes, tUpdateEveryNMillis,
            tFileDir, tFileNameRegex, tRecursive, tPathRegex, tMetadataFrom,
            tMatchAxisNDigits, tFileTableInMemory,
            tAccessibleViaFiles, 
            tnThreads, tDimensionValuesInMemory, 
            tCacheFromUrl, tCacheSizeGB, tCachePartialPathRegex);
    }


    /** This tests generateDatasetsXml. 
     * @throws Throwable if touble
     */
    public static void testGenerateDatasetsXml() throws Throwable {

        String2.log("\n*** EDDGridFromNcFiles.testGenerateDatasetsXml");

        String results = generateDatasetsXml(
            EDStatic.unitTestDataDir + "erdQSwind1day/", 
            ".*_03\\.nc(|.gz)", 
            EDStatic.unitTestDataDir + "erdQSwind1day/erdQSwind1day_20080101_03.nc.gz",
            "", //group
            "", DEFAULT_RELOAD_EVERY_N_MINUTES, null, null) + "\n";  //dimensionsCSV, reloadMinutes, cacheFromUrl
        String suggDatasetID = suggestDatasetID(
            EDStatic.unitTestDataDir + "erdQSwind1day/.*_03\\.nc(|.gz)");

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDGridFromNcFiles",
            EDStatic.unitTestDataDir + "erdQSwind1day/", ".*_03\\.nc(|.gz)", 
            EDStatic.unitTestDataDir + "erdQSwind1day/erdQSwind1day_20080101_03.nc.gz",
            "", //group
            "", "" + DEFAULT_RELOAD_EVERY_N_MINUTES, ""}, //dimensionsCSV, reloadMinutes, cacheFromUrl
            false); //doIt loop?
        Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt. " + 
            gdxResults.length() + " " + results.length());

        String expected = 
"<dataset type=\"EDDGridFromNcFiles\" datasetID=\"" + suggDatasetID + "\" active=\"true\">\n" +
"    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>" + EDStatic.unitTestDataDir + "erdQSwind1day/</fileDir>\n" +
"    <fileNameRegex>.*_03\\.nc(|.gz)</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <matchAxisNDigits>20</matchAxisNDigits>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"acknowledgement\">NOAA NESDIS COASTWATCH, NOAA SWFSC ERD</att>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"composite\">true</att>\n" +
"        <att name=\"contributor_name\">Remote Sensing Systems, Inc</att>\n" +
"        <att name=\"contributor_role\">Source of level 2 data.</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.0, Unidata Dataset Discovery v1.0</att>\n" +
"        <att name=\"creator_email\">dave.foley@noaa.gov</att>\n" +  //that's what's in the files
"        <att name=\"creator_name\">NOAA CoastWatch, West Coast Node</att>\n" +
"        <att name=\"creator_url\">http://coastwatch.pfel.noaa.gov</att>\n" +
"        <att name=\"date_created\">2008-08-29Z</att>\n" +
"        <att name=\"date_issued\">2008-08-29Z</att>\n" +
"        <att name=\"Easternmost_Easting\" type=\"double\">359.875</att>\n" +
"        <att name=\"geospatial_lat_max\" type=\"double\">89.875</att>\n" +
"        <att name=\"geospatial_lat_min\" type=\"double\">-89.875</att>\n" +
"        <att name=\"geospatial_lat_resolution\" type=\"double\">0.25</att>\n" +
"        <att name=\"geospatial_lat_units\">degrees_north</att>\n" +
"        <att name=\"geospatial_lon_max\" type=\"double\">359.875</att>\n" +
"        <att name=\"geospatial_lon_min\" type=\"double\">0.125</att>\n" +
"        <att name=\"geospatial_lon_resolution\" type=\"double\">0.25</att>\n" +
"        <att name=\"geospatial_lon_units\">degrees_east</att>\n" +
"        <att name=\"geospatial_vertical_max\" type=\"double\">0.0</att>\n" +
"        <att name=\"geospatial_vertical_min\" type=\"double\">0.0</att>\n" +
"        <att name=\"geospatial_vertical_positive\">up</att>\n" +
"        <att name=\"geospatial_vertical_units\">m</att>\n" +
"        <att name=\"history\">Remote Sensing Systems, Inc\n" +
"2008-08-29T00:31:43Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD\n" +
             //still numeric ip and http: (not https) because file was generated long ago
"2009-01-07 http://192.168.31.18/thredds/dodsC/satellite/QS/ux10/1day\n" +
"2009-01-07 http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.nc?x_wind[(2008-01-01T12:00:00Z):1:(2008-01-03T12:00:00Z)][(0.0):1:(0.0)][(-89.875):1:(89.875)][(0.125):1:(359.875)],y_wind[(2008-01-01T12:00:00Z):1:(2008-01-03T12:00:00Z)][(0.0):1:(0.0)][(-89.875):1:(89.875)][(0.125):1:(359.875)],mod[(2008-01-01T12:00:00Z):1:(2008-01-03T12:00:00Z)][(0.0):1:(0.0)][(-89.875):1:(89.875)][(0.125):1:(359.875)]</att>\n" +
"        <att name=\"infoUrl\">http://coastwatch.pfel.noaa.gov/infog/QS_ux10_las.html</att>\n" +
"        <att name=\"institution\">NOAA CoastWatch, West Coast Node</att>\n" +
"        <att name=\"keywords\">EARTH SCIENCE &gt; Oceans &gt; Ocean Winds &gt; Surface Winds</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.</att>\n" +
"        <att name=\"naming_authority\">gov.noaa.pfel.coastwatch</att>\n" +
"        <att name=\"Northernmost_Northing\" type=\"double\">89.875</att>\n" +
"        <att name=\"origin\">Remote Sensing Systems, Inc</att>\n" +
"        <att name=\"processing_level\">3</att>\n" +
"        <att name=\"project\">CoastWatch (http://coastwatch.noaa.gov/)</att>\n" +
"        <att name=\"projection\">geographic</att>\n" +
"        <att name=\"projection_type\">mapped</att>\n" +
"        <att name=\"references\">RSS Inc. Winds: http://www.remss.com/ .</att>\n" +
"        <att name=\"satellite\">QuikSCAT</att>\n" +
"        <att name=\"sensor\">SeaWinds</att>\n" +
"        <att name=\"source\">satellite observation: QuikSCAT, SeaWinds</att>\n" +
                     //still numeric ip because file was generated long ago
"        <att name=\"sourceUrl\">http://192.168.31.18/thredds/dodsC/satellite/QS/ux10/1day</att>\n" +
"        <att name=\"Southernmost_Northing\" type=\"double\">-89.875</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF-1.0</att>\n" +
"        <att name=\"summary\">Remote Sensing Inc. distributes science quality wind velocity data from the SeaWinds instrument onboard NASA&#39;s QuikSCAT satellite.  SeaWinds is a microwave scatterometer designed to measure surface winds over the global ocean.  Wind velocity fields are provided in zonal, meriodonal, and modulus sets. The reference height for all wind velocities is 10 meters.</att>\n" +
"        <att name=\"time_coverage_end\">2008-01-03T12:00:00Z</att>\n" +
"        <att name=\"time_coverage_start\">2008-01-01T12:00:00Z</att>\n" +
"        <att name=\"title\">Wind, QuikSCAT, Global, Science Quality (1 Day Composite)</att>\n" +
"        <att name=\"Westernmost_Easting\" type=\"double\">0.125</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"creator_email\">erd.data@noaa.gov</att>\n" +
"        <att name=\"creator_type\">institution</att>\n" +
"        <att name=\"creator_url\">https://coastwatch.pfeg.noaa.gov</att>\n" +
"        <att name=\"date_created\">2008-08-29</att>\n" +
"        <att name=\"date_issued\">2008-08-29</att>\n" +
"        <att name=\"history\">Remote Sensing Systems, Inc\n" +
"2008-08-29T00:31:43Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD\n" +
             //still numeric ip because file was generated long ago
"2009-01-07 http://192.168.31.18/thredds/dodsC/satellite/QS/ux10/1day\n" +
"2009-01-07 https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.nc?x_wind[(2008-01-01T12:00:00Z):1:(2008-01-03T12:00:00Z)][(0.0):1:(0.0)][(-89.875):1:(89.875)][(0.125):1:(359.875)],y_wind[(2008-01-01T12:00:00Z):1:(2008-01-03T12:00:00Z)][(0.0):1:(0.0)][(-89.875):1:(89.875)][(0.125):1:(359.875)],mod[(2008-01-01T12:00:00Z):1:(2008-01-03T12:00:00Z)][(0.0):1:(0.0)][(-89.875):1:(89.875)][(0.125):1:(359.875)]</att>\n" +
"        <att name=\"infoUrl\">https://coastwatch.pfeg.noaa.gov/infog/QS_ux10_las.html</att>\n" +
"        <att name=\"institution\">NOAA CoastWatch WCN</att>\n" +
"        <att name=\"keywords\">altitude, atmosphere, atmospheric, coast, coastwatch, composite, data, day, earth, Earth Science &gt; Atmosphere &gt; Atmospheric Winds &gt; Surface Winds, Earth Science &gt; Oceans &gt; Ocean Winds &gt; Surface Winds, global, latitude, longitude, meridional, mod, modulus, noaa, node, ocean, oceans, quality, quikscat, science, science quality, surface, time, wcn, west, wind, winds, x_wind, y_wind, zonal</att>\n" +
"        <att name=\"naming_authority\">gov.noaa.pfeg.coastwatch</att>\n" +
"        <att name=\"project\">CoastWatch (https://coastwatch.noaa.gov/)</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" +
"    </addAttributes>\n" +
"    <axisVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Time</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">1.1991888E9 1.1993616E9</att>\n" +
"            <att name=\"axis\">T</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">0</att>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Centered Time</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"time_origin\">01-JAN-1970 00:00:00</att>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>altitude</sourceName>\n" +
"        <destinationName>altitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Height</att>\n" +
"            <att name=\"_CoordinateZisPositive\">up</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">0.0 0.0</att>\n" +
"            <att name=\"axis\">Z</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Altitude</att>\n" +
"            <att name=\"positive\">up</att>\n" +
"            <att name=\"standard_name\">altitude</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>latitude</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Lat</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">-89.875 89.875</att>\n" +
"            <att name=\"axis\">Y</att>\n" +
"            <att name=\"coordsys\">geographic</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">2</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"            <att name=\"point_spacing\">even</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>longitude</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Lon</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">0.125 359.875</att>\n" +
"            <att name=\"axis\">X</att>\n" +
"            <att name=\"coordsys\">geographic</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">2</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"            <att name=\"point_spacing\">even</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>x_wind</sourceName>\n" +
"        <destinationName>x_wind</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-15.0</att>\n" +
"            <att name=\"coordsys\">geographic</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">1</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"long_name\">Zonal Wind</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">x_wind</att>\n" +
"            <att name=\"units\">m s-1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>y_wind</sourceName>\n" +
"        <destinationName>y_wind</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-15.0</att>\n" +
"            <att name=\"coordsys\">geographic</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">1</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"long_name\">Meridional Wind</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">y_wind</att>\n" +
"            <att name=\"units\">m s-1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>mod</sourceName>\n" +
"        <destinationName>mod</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">18.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"colorBarPalette\">WhiteRedBlack</att>\n" +
"            <att name=\"coordsys\">geographic</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">1</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"long_name\">Modulus of Wind</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"units\">m s-1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";
        Test.ensureEqual(results, expected, 
            "results.length=" + results.length() + " expected.length=" + expected.length() + 
            "\nresults=\n" + results);

        //ensure it is ready-to-use by making a dataset from it
        String tDatasetID = suggDatasetID;
        EDD.deleteCachedDatasetInfo(tDatasetID);
        EDD edd = oneFromXmlFragment(null, results);
        Test.ensureEqual(edd.datasetID(), suggDatasetID, "");
        Test.ensureEqual(edd.title(), "Wind, QuikSCAT, Global, Science Quality (1 Day Composite)", "");
        Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
            "x_wind, y_wind, mod", "");

        String2.log("\nEDDGridFromNcFiles.testGenerateDatasetsXml passed the test.");
    }

    /** This tests generateDatasetsXml, notably the invalid characters in global attribute
     * names created by netcdfAll-latest.jar. 
     *
     * @throws Throwable if touble
     */
    public static void testGenerateDatasetsXml2() throws Throwable {

        String2.log("\n*** EDDGridFromNcFiles.testGenerateDatasetsXml2");
        String results = generateDatasetsXml(
            String2.unitTestBigDataDir + "geosgrib/", ".*", 
            String2.unitTestBigDataDir + "geosgrib/multi_1.glo_30m.all.grb2",
            "", //group
            "", DEFAULT_RELOAD_EVERY_N_MINUTES, null, null); //dimensionsCSV, reloadMinutes, cacheFromUrl
        String suggDatasetID = suggestDatasetID(
            String2.unitTestBigDataDir + "geosgrib/.*");

        String expected = //as of 2012-02-20. Will change if John Caron fixes bugs I reported.
"<!-- NOTE! The source for this dataset has nGridVariables=13,\n" +
"  but this dataset will only serve 3 because the others use different dimensions. -->\n" +
"<dataset type=\"EDDGridFromNcFiles\" datasetID=\"" + suggDatasetID + "\" active=\"true\">\n" +
"    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>" + String2.unitTestBigDataDir + "geosgrib/</fileDir>\n" +
"    <fileNameRegex>.*</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <matchAxisNDigits>20</matchAxisNDigits>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"Analysis_or_forecast_generating_process_identifier_defined_by_originating_centre\">Global Multi-Grid Wave Model (Static Grids)</att>\n" +
"        <att name=\"Conventions\">CF-1.6</att>\n" +
"        <att name=\"featureType\">GRID</att>\n" +
"        <att name=\"file_format\">GRIB-2</att>\n" +
"        <att name=\"GRIB_table_version\">2,1</att>\n" +
"        <att name=\"history\">Read using CDM IOSP GribCollection v3</att>\n" + //changed in netcdf-java 4.6.4
"        <att name=\"Originating_or_generating_Center\">US National Weather Service, National Centres for Environmental Prediction (NCEP)</att>\n" +
"        <att name=\"Originating_or_generating_Subcenter\">0</att>\n" +
"        <att name=\"Type_of_generating_process\">Forecast</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"Conventions\">CF-1.6, COARDS, ACDD-1.3</att>\n" +
"        <att name=\"creator_name\">NCEP</att>\n" +
"        <att name=\"creator_type\">institution</att>\n" +
"        <att name=\"creator_url\">https://www.ncep.noaa.gov/</att>\n" + 
"        <att name=\"grid_mapping_earth_radius\" type=\"double\">6371229.0</att>\n" +
"        <att name=\"grid_mapping_name\">latitude_longitude</att>\n" +
"        <att name=\"infoUrl\">https://www.ncep.noaa.gov/</att>\n" +
"        <att name=\"institution\">NCEP</att>\n" +
"        <att name=\"keywords\">centers, data, direction, Direction_of_swell_waves_ordered_sequence_of_data, earth, Earth Science &gt; Oceans &gt; Ocean Waves &gt; Significant Wave Height, Earth Science &gt; Oceans &gt; Ocean Waves &gt; Swells, Earth Science &gt; Oceans &gt; Ocean Waves &gt; Wave Period, environmental, height, local, mean, Mean_period_of_swell_waves_ordered_sequence_of_data, national, ncep, ocean, oceans, ordered, ordered_sequence_of_data, period, prediction, science, sea, sea_surface_swell_wave_period, sea_surface_swell_wave_significant_height, sea_surface_swell_wave_to_direction, sequence, significant, Significant_height_of_swell_waves_ordered_sequence_of_data, source, surface, surface waves, swell, swells, time, wave, waves</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" +
"        <att name=\"summary\">National Centers for Environmental Prediction (NCEP) data from a local source.</att>\n" +
"        <att name=\"title\">NCEP data from a local source.</att>\n" +
"    </addAttributes>\n" +
"    <axisVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"calendar\">proleptic_gregorian</att>\n" + //new in netcdf-java 4.6.4
"            <att name=\"long_name\">GRIB forecast or observation time</att>\n" +  //new in netcdf-java 4.6.4
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"units\">Hour since 2009-06-01T06:00:00Z</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"units\">hours since 2009-06-01T06:00:00Z</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>ordered_sequence_of_data</sourceName>\n" +
"        <destinationName>ordered_sequence_of_data</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"Grib_level_type\" type=\"int\">241</att>\n" +
"            <att name=\"long_name\">Ordered Sequence of Data</att>\n" + //new in netcdf-java 4.6.4
"            <att name=\"positive\">up</att>\n" + //new in netcdf-java 4.6.4
"            <att name=\"units\">count</att>\n" + //'seq' new in netcdf-java 4.6.4, fixed in 4.6.5
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Statistics</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>lat</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>lon</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Direction_of_swell_waves_ordered_sequence_of_data</sourceName>\n" +
"        <destinationName>Direction_of_swell_waves_ordered_sequence_of_data</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"abbreviation\">SWDIR</att>\n" +
"            <att name=\"coordinates\">reftime time ordered_sequence_of_data lat lon </att>\n" + //new in netcdf-java 4.6.4
"            <att name=\"Grib2_Generating_Process_Type\">Forecast</att>\n" +
"            <att name=\"Grib2_Level_Desc\">Ordered Sequence of Data</att>\n" + //changed in netcdf-java 4.6.4; 4.6.11 name change from 'Type'
"            <att name=\"Grib2_Level_Type\" type=\"int\">241</att>\n" + //changed in netcdf-java 4.6.4
"            <att name=\"Grib2_Parameter\" type=\"intList\">10 0 7</att>\n" +
"            <att name=\"Grib2_Parameter_Category\">Waves</att>\n" +
"            <att name=\"Grib2_Parameter_Discipline\">Oceanographic products</att>\n" +
"            <att name=\"Grib2_Parameter_Name\">Direction of swell waves</att>\n" +
"            <att name=\"Grib2_Statistical_Process_Type\">UnknownStatType- - 1</att>\n" + //new in netcdf-java 5.2
"            <att name=\"Grib_Variable_Id\">VAR_10-0-7_L241</att>\n" +
"            <att name=\"grid_mapping\">LatLon_Projection</att>\n" + //new in netcdf-java 4.6.4
"            <att name=\"long_name\">Direction of swell waves @ Ordered Sequence of Data</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"            <att name=\"units\">degree_true</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"coordinates\">null</att>\n" + //new in netcdf-java 4.6.4
"            <att name=\"grid_mapping\">null</att>\n" + 
"            <att name=\"ioos_category\">Surface Waves</att>\n" +
"            <att name=\"standard_name\">sea_surface_swell_wave_to_direction</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Mean_period_of_swell_waves_ordered_sequence_of_data</sourceName>\n" +
"        <destinationName>Mean_period_of_swell_waves_ordered_sequence_of_data</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"abbreviation\">SWPER</att>\n" +
"            <att name=\"coordinates\">reftime time ordered_sequence_of_data lat lon </att>\n" + //new in netcdf-java 4.6.4
"            <att name=\"Grib2_Generating_Process_Type\">Forecast</att>\n" +
"            <att name=\"Grib2_Level_Desc\">Ordered Sequence of Data</att>\n" + //changed in netcdf-java 4.6.4
"            <att name=\"Grib2_Level_Type\" type=\"int\">241</att>\n" + //changed in netcdf-java 4.6.4
"            <att name=\"Grib2_Parameter\" type=\"intList\">10 0 9</att>\n" +
"            <att name=\"Grib2_Parameter_Category\">Waves</att>\n" +
"            <att name=\"Grib2_Parameter_Discipline\">Oceanographic products</att>\n" +
"            <att name=\"Grib2_Parameter_Name\">Mean period of swell waves</att>\n" +
"            <att name=\"Grib2_Statistical_Process_Type\">UnknownStatType- - 1</att>\n" + //new in netcdf-java 5.2
"            <att name=\"Grib_Variable_Id\">VAR_10-0-9_L241</att>\n" +
"            <att name=\"grid_mapping\">LatLon_Projection</att>\n" + //new in netcdf-java 4.6.4
"            <att name=\"long_name\">Mean period of swell waves @ Ordered Sequence of Data</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"            <att name=\"units\">s</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">20.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"coordinates\">null</att>\n" + //new in netcdf-java 4.6.4
"            <att name=\"grid_mapping\">null</att>\n" + 
"            <att name=\"ioos_category\">Surface Waves</att>\n" +
"            <att name=\"standard_name\">sea_surface_swell_wave_period</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Significant_height_of_swell_waves_ordered_sequence_of_data</sourceName>\n" +
"        <destinationName>Significant_height_of_swell_waves_ordered_sequence_of_data</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"abbreviation\">SWELL</att>\n" +
"            <att name=\"coordinates\">reftime time ordered_sequence_of_data lat lon </att>\n" + //new in netcdf-java 4.6.4
"            <att name=\"Grib2_Generating_Process_Type\">Forecast</att>\n" +
"            <att name=\"Grib2_Level_Desc\">Ordered Sequence of Data</att>\n" + ////changed in netcdf-java 4.6.4
"            <att name=\"Grib2_Level_Type\" type=\"int\">241</att>\n" + //changed in netcdf-java 4.6.4
"            <att name=\"Grib2_Parameter\" type=\"intList\">10 0 8</att>\n" +
"            <att name=\"Grib2_Parameter_Category\">Waves</att>\n" +
"            <att name=\"Grib2_Parameter_Discipline\">Oceanographic products</att>\n" +
"            <att name=\"Grib2_Parameter_Name\">Significant height of swell waves</att>\n" +
"            <att name=\"Grib2_Statistical_Process_Type\">UnknownStatType- - 1</att>\n" + //new in netcdf-java 5.2
"            <att name=\"Grib_Variable_Id\">VAR_10-0-8_L241</att>\n" +
"            <att name=\"grid_mapping\">LatLon_Projection</att>\n" + //new in netcdf-java 4.6.4
"            <att name=\"long_name\">Significant height of swell waves @ Ordered Sequence of Data</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"coordinates\">null</att>\n" + //new in netcdf-java 4.6.4
"            <att name=\"grid_mapping\">null</att>\n" + 
"            <att name=\"ioos_category\">Surface Waves</att>\n" +
"            <att name=\"standard_name\">sea_surface_swell_wave_significant_height</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //ensure it is ready-to-use by making a dataset from it
        //String tDatasetID = "erdQSwind1day_52db_1ed3_22ce";
        //EDD.deleteCachedDatasetInfo(tDatasetID);
        //EDD edd = oneFromXmlFragment(null, results);
        //Test.ensureEqual(edd.datasetID(), tDatasetID, "");
        //Test.ensureEqual(edd.title(), "Wind, QuikSCAT, Global, Science Quality (1 Day Composite)", "");
        //Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
        //    "x_wind, y_wind, mod", "");

        String2.log("\nEDDGridFromNcFiles.testGenerateDatasetsXml2 passed the test.");
    }

    /** This tests generateDatasetsXml, specifically that dataType reflects
     * scale_factor and add_offset. 
     * @throws Throwable if touble
     */
    public static void testGenerateDatasetsXml3() throws Throwable {

        String2.log("\n*** EDDGridFromNcFiles.testGenerateDatasetsXml3");

        String sDir = "/u00/satellite/PH2/sstd/1day/";
        String sRegex = "[12].*\\.nc";
        String results = generateDatasetsXml(sDir, sRegex, "", 
            "", //group
            "", 12000, "", null) + "\n"; //dimensionsCSV, reloadMinutes, cacheFromUrl
        String suggDatasetID = suggestDatasetID(sDir + sRegex);

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDGridFromNcFiles", sDir, sRegex, "", 
            "", //group
            "", "" + 12000, "", ""}, //dimensionsCSV, reloadMinutes, cacheFromUrl
            false); //doIt loop?
        Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt. " + 
            gdxResults.length() + " " + results.length());

        String expected = 
//wind_speed is important additional test where scale_factor=1 and add_offset=0
"<dataset type=\"EDDGridFromNcFiles\" datasetID=\"" + suggDatasetID + "\" active=\"true\">\n" +
"    <reloadEveryNMinutes>12000</reloadEveryNMinutes>\n" +  
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>" + sDir + "</fileDir>\n" +
"    <fileNameRegex>" + sRegex + "</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <matchAxisNDigits>20</matchAxisNDigits>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"acknowledgment\">Please acknowledge the use of these data with the following statement: These data were provided by GHRSST and the US National Oceanographic Data Center. This project was supported in part by a grant from the NOAA Climate Data Record (CDR) Program for satellites.</att>\n" +
"        <att name=\"cdm_data_type\">grid</att>\n" +
"        <att name=\"cdr_id\">gov.noaa.ncdc:C00804</att>\n" +
"        <att name=\"cdr_program\">NOAA Climate Data Record Program for satellites, FY 2011.</att>\n" +
"        <att name=\"cdr_variable\">sea_surface_temperature</att>\n" +
"        <att name=\"comment\">SST from AVHRR Pathfinder</att>\n" +
"        <att name=\"Conventions\">CF-1.5</att>\n" +
"        <att name=\"creator_email\">Kenneth.Casey@noaa.gov</att>\n" +
"        <att name=\"creator_name\">Kenneth S. Casey</att>\n" +
"        <att name=\"creator_url\">http://pathfinder.nodc.noaa.gov</att>\n" +
"        <att name=\"date_created\">20130426T025604Z</att>\n" +
"        <att name=\"day_or_night\">day</att>\n" +
"        <att name=\"easternmost_longitude\" type=\"double\">180.0</att>\n" +
"        <att name=\"file_quality_level\" type=\"int\">3</att>\n" +
"        <att name=\"gds_version_id\">2.0</att>\n" +
"        <att name=\"geospatial_lat_max\" type=\"double\">90.0</att>\n" +
"        <att name=\"geospatial_lat_min\" type=\"double\">-90.0</att>\n" +
"        <att name=\"geospatial_lat_resolution\">0.0417</att>\n" +
"        <att name=\"geospatial_lat_units\">degrees north</att>\n" +
"        <att name=\"geospatial_lon_max\" type=\"double\">180.0</att>\n" +
"        <att name=\"geospatial_lon_min\" type=\"double\">-180.0</att>\n" +
"        <att name=\"geospatial_lon_resolution\">0.0417</att>\n" +
"        <att name=\"geospatial_lon_units\">degrees east</att>\n" +
"        <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" +
"        <att name=\"history\">smigen_both ifile=2012366.b4kd3-pf5ap-n19-sst.hdf ofile=2012366.i4kd3-pf5ap-n19-sst.hdf prod=sst datamin=-3.0 datamax=40.0 precision=I projection=RECT resolution=4km gap_fill=2 ; ./hdf2nc_PFV52_L3C.x -v ./Data_PFV52/PFV52_HDF/2012/2012366.i4kd3-pf5ap-n19-sst.hdf</att>\n" +
"        <att name=\"id\">AVHRR_Pathfinder-NODC-L3C-v5.2</att>\n" +
"        <att name=\"institution\">NODC</att>\n" +
"        <att name=\"keywords\">Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature</att>\n" +
"        <att name=\"keywords_vocabulary\">NASA Global Change Master Directory (GCMD) Science Keywords</att>\n" +
"        <att name=\"license\">These data are available for use without restriction.</att>\n" +
"        <att name=\"Metadata_Conventions\">Unidata Dataset Discovery v1.0</att>\n" +
"        <att name=\"metadata_link\">http://pathfinder.nodc.noaa.gov/ISO-AVHRR_Pathfinder-NODC-L3C-v5.2.html</att>\n" +
"        <att name=\"naming_authority\">org.ghrsst</att>\n" +
"        <att name=\"netcdf_version_id\">4.1.2</att>\n" +
"        <att name=\"northernmost_latitude\" type=\"double\">90.0</att>\n" +
"        <att name=\"orbit_node\">ascending</att>\n" +
"        <att name=\"platform\">NOAA-19</att>\n" +
"        <att name=\"principal_year_day_for_collated_orbits\">2012366</att>\n" +
"        <att name=\"processing_level\">L3C</att>\n" +
"        <att name=\"product_version\">PFV5.2</att>\n" +
"        <att name=\"project\">Group for High Resolution Sea Surface Temperature</att>\n" +
"        <att name=\"publisher_email\">ghrsst-po@nceo.ac.uk</att>\n" +
"        <att name=\"publisher_name\">GHRSST Project Office</att>\n" +
"        <att name=\"publisher_url\">http://www.ghrsst.org</att>\n" +
"        <att name=\"references\">http://pathfinder.nodc.noaa.gov and Casey, K.S., T.B. Brandon, P. Cornillon, and R. Evans: The Past, Present and Future of the AVHRR Pathfinder SST Program, in Oceanography from Space: Revisited, eds. V. Barale, J.F.R. Gower, and L. Alberotanza, Springer, 2010. DOI: 10.1007/978-90-481-8681-5_16.</att>\n" +
"        <att name=\"sensor\">AVHRR_GAC</att>\n" +
"        <att name=\"source\">AVHRR_GAC-CLASS-L1B-NOAA_19-v1</att>\n" +
"        <att name=\"southernmost_latitude\" type=\"double\">-90.0</att>\n" +
"        <att name=\"spatial_resolution\">0.0417 degree</att>\n" +
"        <att name=\"standard_name_vocabulary\">NetCDF Climate and Forecast (CF) Metadata Convention</att>\n" +
"        <att name=\"start_time\">20121231T000731Z</att>\n" +
"        <att name=\"stop_time\">20130101T064326Z</att>\n" +
"        <att name=\"summary\">This netCDF-4 file contains sea surface temperature (SST) data produced as part of the AVHRR Pathfinder SST Project. These data were created using Version 5.2 of the Pathfinder algorithm and the file is nearly but not completely compliant with the GHRSST Data Specifications V2.0 (GDS2).  The file does not encode time according to the GDS2 specifications, and the sses_bias and sses_standard_deviation variables are empty.  Full compliance with GDS2 specifications will be achieved in the future Pathfinder Version 6. These data were created as a partnership between the University of Miami and the US NOAA/National Oceanographic Data Center (NODC).</att>\n" +
"        <att name=\"time_coverage_end\">20130101T064326Z</att>\n" +
"        <att name=\"time_coverage_start\">20121231T000731Z</att>\n" +
"        <att name=\"title\">AVHRR Pathfinder Version 5.2 L3-Collated (L3C) sea surface temperature</att>\n" +
"        <att name=\"uuid\">6D1F3BAB-C00F-40FA-A73E-6D96CE51530F</att>\n" +
"        <att name=\"westernmost_longitude\" type=\"double\">-180.0</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"acknowledgement\">Please acknowledge the use of these data with the following statement: These data were provided by GHRSST and the US National Oceanographic Data Center. This project was supported in part by a grant from the NOAA Climate Data Record (CDR) Program for satellites.</att>\n" +
"        <att name=\"acknowledgment\">null</att>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"Conventions\">CF-1.6, COARDS, ACDD-1.3</att>\n" +
"        <att name=\"creator_type\">person</att>\n" +
"        <att name=\"creator_url\">https://pathfinder.nodc.noaa.gov</att>\n" +
"        <att name=\"date_created\">2013-04-26T02:56:04Z</att>\n" +
"        <att name=\"easternmost_longitude\">null</att>\n" +
"        <att name=\"file_quality_level\">null</att>\n" +
"        <att name=\"infoUrl\">https://pathfinder.nodc.noaa.gov/ISO-AVHRR_Pathfinder-NODC-L3C-v5.2.html</att>\n" +
"        <att name=\"institution\">NOAA NODC</att>\n" +
"        <att name=\"keywords\">10m, advanced, aerosol, aerosol_dynamic_indicator, analysis, area, atmosphere, atmospheric, avhrr, bias, center, climate, collated, cryosphere, data, deviation, difference, distribution, dt_analysis, dynamic, earth, Earth Science &gt; Atmosphere &gt; Atmospheric Winds &gt; Surface Winds, Earth Science &gt; Cryosphere &gt; Sea Ice &gt; Ice Extent, Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature, Earth Science &gt; Oceans &gt; Sea Ice &gt; Ice Extent, error, estimate, extent, flag, flags, fraction, ghrsst, global, high, high-resolution, ice, ice distribution, indicator, l2p, l2p_flags, l3-collated, l3c, measurement, national, ncei, nesdis, noaa, nodc, ocean, oceanographic, oceans, optical, optical properties, pathfinder, pathfinder_quality_level, properties, quality, quality_level, radiometer, record, reference, resolution, science, sea, sea_ice_area_fraction, sea_ice_fraction, sea_surface_skin_temperature, sea_surface_temperature, sensor, single, skin, speed, sses, sses_bias, sses_standard_deviation, sst, sst_dtime, standard, statistics, surface, temperature, time, version, very, vhrr, wind, wind_speed, winds</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"Metadata_Conventions\">null</att>\n" +
"        <att name=\"metadata_link\">https://pathfinder.nodc.noaa.gov/ISO-AVHRR_Pathfinder-NODC-L3C-v5.2.html</att>\n" +
"        <att name=\"netcdf_version_id\">null</att>\n" +
"        <att name=\"northernmost_latitude\">null</att>\n" +
"        <att name=\"principal_year_day_for_collated_orbits\">null</att>\n" +
"        <att name=\"publisher_type\">group</att>\n" +
"        <att name=\"publisher_url\">https://www.ghrsst.org</att>\n" +
"        <att name=\"references\">https://pathfinder.nodc.noaa.gov and Casey, K.S., T.B. Brandon, P. Cornillon, and R. Evans: The Past, Present and Future of the AVHRR Pathfinder SST Program, in Oceanography from Space: Revisited, eds. V. Barale, J.F.R. Gower, and L. Alberotanza, Springer, 2010. DOI: 10.1007/978-90-481-8681-5_16.</att>\n" +
"        <att name=\"southernmost_latitude\">null</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" +
"        <att name=\"start_time\">null</att>\n" +
"        <att name=\"stop_time\">null</att>\n" +
"        <att name=\"summary\">Advanced Very High Resolution Radiometer (AVHRR) Pathfinder Version 5.2 L3-Collated (L3C) sea surface temperature. This netCDF-4 file contains sea surface temperature (SST) data produced as part of the AVHRR Pathfinder SST Project. These data were created using Version 5.2 of the Pathfinder algorithm and the file is nearly but not completely compliant with the Global High-Resolution Sea Surface Temperature (GHRSST) Data Specifications V2.0 (GDS2).  The file does not encode time according to the GDS2 specifications, and the sses_bias and sses_standard_deviation variables are empty.  Full compliance with GDS2 specifications will be achieved in the future Pathfinder Version 6. These data were created as a partnership between the University of Miami and the US NOAA/National Oceanographic Data Center (NODC).</att>\n" +
"        <att name=\"time_coverage_end\">2013-01-01T06:43:26Z</att>\n" +
"        <att name=\"time_coverage_start\">2012-12-31T00:07:31Z</att>\n" +
"        <att name=\"title\">AVHRR Pathfinder Version 5.2 L3-Collated (L3C) SST</att>\n" +
"        <att name=\"uuid\">null</att>\n" +
"        <att name=\"westernmost_longitude\">null</att>\n" +
"    </addAttributes>\n" +
"    <axisVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSizes\" type=\"int\">1</att>\n" +
"            <att name=\"axis\">T</att>\n" +
"            <att name=\"calendar\">Gregorian</att>\n" +
"            <att name=\"comment\">This is the reference time of the SST file. Add sst_dtime to this value to get pixel-by-pixel times. Note: in PFV5.2 that sst_dtime is empty. PFV6 will contain the correct sst_dtime values.</att>\n" +
"            <att name=\"long_name\">reference time of SST file</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"units\">seconds since 1981-01-01 00:00:00</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSizes\">null</att>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"units\">seconds since 1981-01-01T00:00:00Z</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>lat</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"axis\">Y</att>\n" +
"            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" +
"            <att name=\"reference_datum\">geographical coordinates, WGS84 projection</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"            <att name=\"valid_max\" type=\"double\">90.0</att>\n" +
"            <att name=\"valid_min\" type=\"double\">-90.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"grid_mapping\">null</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>lon</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"axis\">X</att>\n" +
"            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" +
"            <att name=\"reference_datum\">geographical coordinates, WGS84 projection</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"            <att name=\"valid_max\" type=\"double\">180.0</att>\n" +
"            <att name=\"valid_min\" type=\"double\">-180.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"grid_mapping\">null</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>sea_surface_temperature</sourceName>\n" +
"        <destinationName>sea_surface_temperature</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSizes\" type=\"intList\">1 540 540</att>\n" +
"            <att name=\"_FillValue\" type=\"short\">-32768</att>\n" +
"            <att name=\"add_offset\" type=\"double\">273.15</att>\n" +
"            <att name=\"comment\">Skin temperature of the ocean</att>\n" +
"            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" +
"            <att name=\"long_name\">NOAA Climate Data Record of sea surface skin temperature</att>\n" +
"            <att name=\"scale_factor\" type=\"double\">0.01</att>\n" +
"            <att name=\"standard_name\">sea_surface_skin_temperature</att>\n" +
"            <att name=\"units\">kelvin</att>\n" +
"            <att name=\"valid_max\" type=\"short\">4500</att>\n" +
"            <att name=\"valid_min\" type=\"short\">-180</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSizes\">null</att>\n" +
"            <att name=\"add_offset\" type=\"double\">0.0</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"grid_mapping\">null</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"units\">degree_C</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>sst_dtime</sourceName>\n" +
"        <destinationName>sst_dtime</destinationName>\n" +
"        <dataType>int</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSizes\" type=\"intList\">1 540 540</att>\n" +
"            <att name=\"_FillValue\" type=\"int\">-2147483648</att>\n" +
"            <att name=\"add_offset\" type=\"int\">0</att>\n" +
"            <att name=\"comment\">time plus sst_dtime gives seconds after 1981-01-01 00:00:00. Note: in PFV5.2 this sst_dtime is empty. PFV6 will contain the correct sst_dtime values.</att>\n" +
"            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" +
"            <att name=\"long_name\">time difference from reference time</att>\n" +
"            <att name=\"scale_factor\" type=\"int\">1</att>\n" +
"            <att name=\"units\">second</att>\n" +
"            <att name=\"valid_max\" type=\"int\">2147483647</att>\n" +
"            <att name=\"valid_min\" type=\"int\">-2147483647</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSizes\">null</att>\n" +
"            <att name=\"add_offset\">null</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-10.0</att>\n" +
"            <att name=\"grid_mapping\">null</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"scale_factor\">null</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>sses_bias</sourceName>\n" +
"        <destinationName>sses_bias</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSizes\" type=\"intList\">1 540 540</att>\n" +
"            <att name=\"_FillValue\" type=\"byte\">-128</att>\n" +
"            <att name=\"add_offset\" type=\"double\">0.0</att>\n" +
"            <att name=\"comment\">Bias estimate derived using the techniques described at http://www.ghrsst.org/SSES-Description-of-schemes.html. Note: in PFV5.2 this sses_bias is empty. PFV6 will contain the correct sses_bias values.</att>\n" +
"            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" +
"            <att name=\"long_name\">SSES bias estimate</att>\n" +
"            <att name=\"scale_factor\" type=\"double\">0.02</att>\n" +
"            <att name=\"units\">kelvin</att>\n" +
"            <att name=\"valid_max\" type=\"byte\">127</att>\n" +
"            <att name=\"valid_min\" type=\"byte\">-127</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSizes\">null</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">5.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"comment\">Bias estimate derived using the techniques described at https://www.ghrsst.org/SSES-Description-of-schemes.html. Note: in PFV5.2 this sses_bias is empty. PFV6 will contain the correct sses_bias values.</att>\n" +
"            <att name=\"grid_mapping\">null</att>\n" +
"            <att name=\"ioos_category\">Statistics</att>\n" +
"            <att name=\"units\">degree_C</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>sses_standard_deviation</sourceName>\n" +
"        <destinationName>sses_standard_deviation</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSizes\" type=\"intList\">1 540 540</att>\n" +
"            <att name=\"_FillValue\" type=\"byte\">-128</att>\n" +
"            <att name=\"add_offset\" type=\"double\">2.54</att>\n" +
"            <att name=\"comment\">Standard deviation estimate derived using the techniques described at http://www.ghrsst.org/SSES-Description-of-schemes.html. Note: in PFV5.2 this sses_standard_deviation is empty. PFV6 will contain the correct sses_standard_deviation values.</att>\n" +
"            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" +
"            <att name=\"long_name\">SSES standard deviation</att>\n" +
"            <att name=\"scale_factor\" type=\"double\">0.02</att>\n" +
"            <att name=\"units\">kelvin</att>\n" +
"            <att name=\"valid_max\" type=\"byte\">127</att>\n" +
"            <att name=\"valid_min\" type=\"byte\">-127</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSizes\">null</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">5.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"comment\">Standard deviation estimate derived using the techniques described at https://www.ghrsst.org/SSES-Description-of-schemes.html. Note: in PFV5.2 this sses_standard_deviation is empty. PFV6 will contain the correct sses_standard_deviation values.</att>\n" +
"            <att name=\"grid_mapping\">null</att>\n" +
"            <att name=\"ioos_category\">Statistics</att>\n" +
"            <att name=\"units\">degree_C</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>dt_analysis</sourceName>\n" +
"        <destinationName>dt_analysis</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSizes\" type=\"intList\">1 540 540</att>\n" +
"            <att name=\"_FillValue\" type=\"byte\">-128</att>\n" +
"            <att name=\"add_offset\" type=\"double\">0.0</att>\n" +
"            <att name=\"comment\">The difference between this SST and the previous day&#39;s SST.</att>\n" +
"            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" +
"            <att name=\"long_name\">Deviation from last SST analysis</att>\n" +
"            <att name=\"reference\">AVHRR_OI, with inland values populated from AVHRR_Pathfinder daily climatological SST. For more information on this reference field see http://accession.nodc.noaa.gov/0071180.</att>\n" +
"            <att name=\"scale_factor\" type=\"double\">0.1</att>\n" +
"            <att name=\"units\">kelvin</att>\n" +
"            <att name=\"valid_max\" type=\"byte\">127</att>\n" +
"            <att name=\"valid_min\" type=\"byte\">-127</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSizes\">null</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">5.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-5.0</att>\n" +
"            <att name=\"grid_mapping\">null</att>\n" +
"            <att name=\"ioos_category\">Statistics</att>\n" +
"            <att name=\"reference\">null</att>\n" +
"            <att name=\"references\">AVHRR_OI, with inland values populated from AVHRR_Pathfinder daily climatological SST. For more information on this reference field see https://data.nodc.noaa.gov/cgi-bin/iso?id=gov.noaa.nodc:0071180.</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>wind_speed</sourceName>\n" +
"        <destinationName>wind_speed</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSizes\" type=\"intList\">1 540 540</att>\n" +
"            <att name=\"_FillValue\" type=\"byte\">-128</att>\n" +
"            <att name=\"add_offset\" type=\"double\">0.0</att>\n" +
"            <att name=\"comment\">These wind speeds were created by NCEP-DOE Atmospheric Model Intercomparison Project (AMIP-II) reanalysis (R-2) and represent winds at 10 metres above the sea surface.</att>\n" +
"            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" +
"            <att name=\"height\">10 m</att>\n" +
"            <att name=\"long_name\">10m wind speed</att>\n" +
"            <att name=\"scale_factor\" type=\"double\">1.0</att>\n" +
"            <att name=\"source\">NCEP/DOE AMIP-II Reanalysis (Reanalysis-2): u_wind.10m.gauss.2012.nc, v_wind.10m.gauss.2012.nc</att>\n" +
"            <att name=\"standard_name\">wind_speed</att>\n" +
"            <att name=\"time_offset\" type=\"double\">6.0</att>\n" +
"            <att name=\"units\">m s-1</att>\n" +
"            <att name=\"valid_max\" type=\"byte\">127</att>\n" +
"            <att name=\"valid_min\" type=\"byte\">-127</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSizes\">null</att>\n" +
"            <att name=\"add_offset\">null</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"grid_mapping\">null</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"scale_factor\">null</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>sea_ice_fraction</sourceName>\n" +
"        <destinationName>sea_ice_fraction</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSizes\" type=\"intList\">1 540 540</att>\n" +
"            <att name=\"_FillValue\" type=\"byte\">-128</att>\n" +
"            <att name=\"add_offset\" type=\"double\">0.0</att>\n" +
"            <att name=\"comment\">Sea ice concentration data are taken from the EUMETSAT Ocean and Sea Ice Satellite Application Facility (OSISAF) Global Daily Sea Ice Concentration Reprocessing Data Set (http://accession.nodc.noaa.gov/0068294) when these data are available. The data are reprojected and interpolated from their original polar stereographic projection at 10km spatial resolution to the 4km Pathfinder Version 5.2 grid. When the OSISAF data are not available for both hemispheres on a given day, the sea ice concentration data are taken from the sea_ice_fraction variable found in the L4 GHRSST DailyOI SST product from NOAA/NCDC, and are interpolated from the 25km DailyOI grid to the 4km Pathfinder Version 5.2 grid.</att>\n" +
"            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" +
"            <att name=\"long_name\">sea ice fraction</att>\n" +
"            <att name=\"reference\">Reynolds, et al.(2006) Daily High-resolution Blended Analyses. Available at ftp://eclipse.ncdc.noaa.gov/pub/OI-daily/daily-sst.pdf</att>\n" +
"            <att name=\"scale_factor\" type=\"double\">0.01</att>\n" +
"            <att name=\"source\">NOAA/NESDIS/NCDC Daily optimum interpolation(OI) SST on 1/4-degree grid: 20121231-NCDC-L4LRblend-GLOB-v01-fv02_0-AVHRR_OI.nc.gz</att>\n" +
"            <att name=\"standard_name\">sea_ice_area_fraction</att>\n" +
"            <att name=\"time_offset\" type=\"double\">12.0</att>\n" +
"            <att name=\"valid_max\" type=\"byte\">127</att>\n" +
"            <att name=\"valid_min\" type=\"byte\">-127</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSizes\">null</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">1.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"comment\">Sea ice concentration data are taken from the EUMETSAT Ocean and Sea Ice Satellite Application Facility (OSISAF) Global Daily Sea Ice Concentration Reprocessing Data Set (https://data.nodc.noaa.gov/cgi-bin/iso?id=gov.noaa.nodc:0068294) when these data are available. The data are reprojected and interpolated from their original polar stereographic projection at 10km spatial resolution to the 4km Pathfinder Version 5.2 grid. When the OSISAF data are not available for both hemispheres on a given day, the sea ice concentration data are taken from the sea_ice_fraction variable found in the L4 GHRSST DailyOI SST product from NOAA/NCDC, and are interpolated from the 25km DailyOI grid to the 4km Pathfinder Version 5.2 grid.</att>\n" +
"            <att name=\"grid_mapping\">null</att>\n" +
"            <att name=\"ioos_category\">Ice Distribution</att>\n" +
"            <att name=\"reference\">null</att>\n" +
"            <att name=\"references\">Reynolds, et al.(2006) Daily High-resolution Blended Analyses. Available at https://journals.ametsoc.org/doi/full/10.1175/2007JCLI1824.1</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aerosol_dynamic_indicator</sourceName>\n" +
"        <destinationName>aerosol_dynamic_indicator</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSizes\" type=\"intList\">1 540 540</att>\n" +
"            <att name=\"_FillValue\" type=\"byte\">-128</att>\n" +
"            <att name=\"add_offset\" type=\"double\">1.1</att>\n" +
"            <att name=\"comment\">Aerosol optical thickness (100 KM) data are taken from the CLASS AERO100 products, which are created from AVHRR channel 1 optical thickness retrievals from AVHRR global area coverage (GAC) data. The aerosol optical thickness measurements are interpolated from their original 1 degree x 1 degree resolution to the 4km Pathfinder Version 5.2 grid.</att>\n" +
"            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" +
"            <att name=\"long_name\">aerosol dynamic indicator</att>\n" +
"            <att name=\"reference\">http://www.class.ncdc.noaa.gov/saa/products/search?sub_id=0&amp;datatype_family=AERO100&amp;submit.x=25&amp;submit.y=12</att>\n" +
"            <att name=\"scale_factor\" type=\"double\">0.01</att>\n" +
"            <att name=\"source\">CLASS_AERO100_AOT</att>\n" +
"            <att name=\"time_offset\" type=\"double\">-107.0</att>\n" +
"            <att name=\"valid_max\" type=\"byte\">127</att>\n" +
"            <att name=\"valid_min\" type=\"byte\">-127</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSizes\">null</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">3.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-1.0</att>\n" +
"            <att name=\"grid_mapping\">null</att>\n" +
"            <att name=\"ioos_category\">Optical Properties</att>\n" +
"            <att name=\"reference\">null</att>\n" +
"            <att name=\"references\">https://www.class.ncdc.noaa.gov/saa/products/search?sub_id=0&amp;datatype_family=AERO100&amp;submit.x=25&amp;submit.y=12</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>quality_level</sourceName>\n" +
"        <destinationName>quality_level</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSizes\" type=\"intList\">1 540 540</att>\n" +
"            <att name=\"_FillValue\" type=\"byte\">0</att>\n" +
"            <att name=\"comment\">Note, the native Pathfinder processing system returns quality levels ranging from 0 to 7 (7 is best quality; -1 represents missing data) and has been converted to the extent possible into the six levels required by the GDS2 (ranging from 0 to 5, where 5 is best). Below is the conversion table: \n" +
" GDS2 required quality_level 5  =  native Pathfinder quality level 7 == best_quality \n" +
" GDS2 required quality_level 4  =  native Pathfinder quality level 4-6 == acceptable_quality \n" +
" GDS2 required quality_level 3  =  native Pathfinder quality level 2-3 == low_quality \n" +
" GDS2 required quality_level 2  =  native Pathfinder quality level 1 == worst_quality \n" +
" GDS2 required quality_level 1  =  native Pathfinder quality level 0 = bad_data \n" +
" GDS2 required quality_level 0  =  native Pathfinder quality level -1 = missing_data \n" +
" The original Pathfinder quality level is recorded in the optional variable pathfinder_quality_level.</att>\n" +
"            <att name=\"flag_meanings\">bad_data worst_quality low_quality acceptable_quality best_quality</att>\n" +
"            <att name=\"flag_values\" type=\"byteList\">1 2 3 4 5</att>\n" +
"            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" +
"            <att name=\"long_name\">SST measurement quality</att>\n" +
"            <att name=\"valid_max\" type=\"byte\">5</att>\n" +
"            <att name=\"valid_min\" type=\"byte\">1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSizes\">null</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">6.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"grid_mapping\">null</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>pathfinder_quality_level</sourceName>\n" +
"        <destinationName>pathfinder_quality_level</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSizes\" type=\"intList\">1 540 540</att>\n" +
"            <att name=\"_FillValue\" type=\"byte\">-1</att>\n" +
"            <att name=\"comment\">This variable contains the native Pathfinder processing system quality levels, ranging from 0 to 7, where 0 is worst and 7 is best. And value -1 represents missing data.</att>\n" +
"            <att name=\"flag_meanings\">bad_data worst_quality low_quality low_quality acceptable_quality acceptable_quality acceptable_quality best_quality</att>\n" +
"            <att name=\"flag_values\" type=\"byteList\">0 1 2 3 4 5 6 7</att>\n" +
"            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" +
"            <att name=\"long_name\">Pathfinder SST quality flag</att>\n" +
"            <att name=\"valid_max\" type=\"byte\">7</att>\n" +
"            <att name=\"valid_min\" type=\"byte\">0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSizes\">null</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">8.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"grid_mapping\">null</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>l2p_flags</sourceName>\n" +
"        <destinationName>l2p_flags</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSizes\" type=\"intList\">1 540 540</att>\n" +
"            <att name=\"comment\">Bit zero (0) is always set to zero to indicate infrared data. Bit one (1) is set to zero for any pixel over water (ocean, lakes and rivers). Land pixels were determined by rasterizing the Global Self-consistent Hierarchical High-resolution Shoreline (GSHHS) Database from the NOAA National Geophysical Data Center. Any 4 km Pathfinder pixel whose area is 50&#37; or more covered by land has bit one (1) set to 1. Bit two (2) is set to 1 when the sea_ice_fraction is 0.15 or greater. Bits three (3) and four (4) indicate lake and river pixels, respectively, and were determined by rasterizing the US World Wildlife Fund&#39;s Global Lakes and Wetlands Database. Any 4 km Pathfinder pixel whose area is 50&#37; or more covered by lake has bit three (3) set to 1. Any 4 km Pathfinder pixel whose area is 50&#37; or more covered by river has bit four (4) set to 1.</att>\n" +
"            <att name=\"flag_masks\" type=\"shortList\">1 2 4 8 16 32 64 128 256</att>\n" +
"            <att name=\"flag_meanings\">microwave land ice lake river reserved_for_future_use unused_currently unused_currently unused_currently</att>\n" +
"            <att name=\"grid_mapping\">Equidistant Cylindrical</att>\n" +
"            <att name=\"long_name\">L2P flags</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSizes\">null</att>\n" +
"            <att name=\"_FillValue\" type=\"short\">32767</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">300.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"grid_mapping\">null</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";
        Test.ensureEqual(results, expected, 
            "results.length=" + results.length() + " expected.length=" + expected.length() + 
            "\nresults=\n" + results);


        String2.log("\nEDDGridFromNcFiles.testGenerateDatasetsXml3 passed the test.");
    }

    /** This tests generateDatasetsXml, specifically reloadEveryNMinutes and testOutOfDate. 
     * @throws Throwable if touble
     */
    public static void testGenerateDatasetsXml4() throws Throwable {

        String2.log("\n*** EDDGridFromNcFiles.testGenerateDatasetsXml4");
        reallyVerbose = true;

        //takes a long time and no longer useful
        //String2.pressEnterToContinue(
        //    "\nCopy the latest file from coastwatch\n" +
        //    "https://coastwatch.pfeg.noaa.gov/erddap/files/jplMURSST41/ \n" + 
        //    "to /u00/satellite/MUR41/ssta/1day on this computer.");

        String results = generateDatasetsXml(
            "/u00/satellite/MUR41/ssta/1day/", "2.*\\.nc", "", 
            "", //group
            "", -1, null, null) + "\n"; //dimensionsCSV, reloadMinutes, cacheFromUrl

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDGridFromNcFiles", 
            "/u00/satellite/MUR41/ssta/1day/", "2.*\\.nc", "",
            "", //group
            "", "-1", ""}, //dimensionsCSV, reloadMinutes, cacheFromUrl
            false); //doIt loop?
        Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt. " + 
            gdxResults.length() + " " + results.length());

        String expected = 
"<dataset type=\"EDDGridFromNcFiles\" datasetID=\"1day_328e_9133_37c0\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" + //because using updateEveryNMillis
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>/u00/satellite/MUR41/ssta/1day/</fileDir>\n" +
"    <fileNameRegex>2.*\\.nc</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <matchAxisNDigits>20</matchAxisNDigits>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"acknowledgment\">Please acknowledge the use of these data with the following statement:  These data were provided by JPL under support by NASA MEaSUREs program.</att>\n" +
"        <att name=\"cdm_data_type\">grid</att>\n" +
"        <att name=\"comment\">Interim-MUR(nrt) will be replaced by MUR-Final in about 3 days; MUR = &quot;Multi-scale Ultra-high Reolution&quot;</att>\n" +
"        <att name=\"Conventions\">CF-1.5</att>\n" +
"        <att name=\"creator_email\">ghrsst@podaac.jpl.nasa.gov</att>\n" +
"        <att name=\"creator_name\">JPL MUR SST project</att>\n" +
"        <att name=\"creator_url\">http://mur.jpl.nasa.gov</att>\n" +
"        <att name=\"date_created\">20180808T023831Z</att>\n" +  //changes
"        <att name=\"easternmost_longitude\" type=\"float\">180.0</att>\n" +
"        <att name=\"file_quality_level\">1</att>\n" +
"        <att name=\"gds_version_id\">2.0</att>\n" +
"        <att name=\"geospatial_lat_resolution\">0.01 degrees</att>\n" +
"        <att name=\"geospatial_lat_units\">degrees north</att>\n" +
"        <att name=\"geospatial_lon_resolution\">0.01 degrees</att>\n" +
"        <att name=\"geospatial_lon_units\">degrees east</att>\n" +
"        <att name=\"history\">near real time (nrt) version created at nominal 1-day latency.</att>\n" +
"        <att name=\"id\">MUR-JPL-L4-GLOB-v04.1</att>\n" +
"        <att name=\"institution\">Jet Propulsion Laboratory</att>\n" +
"        <att name=\"keywords\">Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature</att>\n" +
"        <att name=\"keywords_vocabulary\">NASA Global Change Master Directory (GCMD) Science Keywords</att>\n" +
"        <att name=\"license\">These data are available free of charge under data policy of JPL PO.DAAC.</att>\n" +
"        <att name=\"Metadata_Conventions\">Unidata Observation Dataset v1.0</att>\n" +
"        <att name=\"metadata_link\">http://podaac.jpl.nasa.gov/ws/metadata/dataset/?format=iso&amp;shortName=MUR-JPL-L4-GLOB-v04.1</att>\n" +
"        <att name=\"naming_authority\">org.ghrsst</att>\n" +
"        <att name=\"netcdf_version_id\">4.1</att>\n" +
"        <att name=\"northernmost_latitude\" type=\"float\">90.0</att>\n" +
"        <att name=\"platform\">Terra, Aqua, GCOM-W, NOAA-19, MetOp-A, Buoys/Ships</att>\n" +
"        <att name=\"processing_level\">L4</att>\n" +
"        <att name=\"product_version\">04.1nrt</att>\n" +
"        <att name=\"project\">NASA Making Earth Science Data Records for Use in Research Environments (MEaSUREs) Program</att>\n" +
"        <att name=\"publisher_email\">ghrsst-po@nceo.ac.uk</att>\n" +
"        <att name=\"publisher_name\">GHRSST Project Office</att>\n" +
"        <att name=\"publisher_url\">http://www.ghrsst.org</att>\n" +
"        <att name=\"references\">http://podaac.jpl.nasa.gov/Multi-scale_Ultra-high_Resolution_MUR-SST</att>\n" +
"        <att name=\"sensor\">MODIS, AMSR2, AVHRR, in-situ</att>\n" +
"        <att name=\"source\">MODIS_T-JPL, MODIS_A-JPL, AMSR2-REMSS, AVHRR19_G-NAVO, AVHRRMTA_G-NAVO, iQUAM-NOAA/NESDIS, Ice_Conc-OSISAF</att>\n" +
"        <att name=\"southernmost_latitude\" type=\"float\">-90.0</att>\n" +
"        <att name=\"spatial_resolution\">0.01 degrees</att>\n" +
"        <att name=\"standard_name_vocabulary\">NetCDF Climate and Forecast (CF) Metadata Convention</att>\n" +
"        <att name=\"start_time\">20180807T090000Z</att>\n" + //changes
"        <att name=\"stop_time\">20180807T090000Z</att>\n" +
"        <att name=\"summary\">A merged, multi-sensor L4 Foundation SST analysis product from JPL.</att>\n" +
"        <att name=\"time_coverage_end\">20180807T210000Z</att>\n" +  //changes
"        <att name=\"time_coverage_start\">20180806T210000Z</att>\n" +
"        <att name=\"title\">Daily MUR SST, Interim near-real-time (nrt) product</att>\n" +
"        <att name=\"uuid\">27665bc0-d5fc-11e1-9b23-0800200c9a66</att>\n" +
"        <att name=\"westernmost_longitude\" type=\"float\">-180.0</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"acknowledgement\">Please acknowledge the use of these data with the following statement:  These data were provided by JPL under support by NASA MEaSUREs program.</att>\n" +
"        <att name=\"acknowledgment\">null</att>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"Conventions\">CF-1.6, COARDS, ACDD-1.3</att>\n" +
"        <att name=\"creator_type\">group</att>\n" +
"        <att name=\"creator_url\">https://podaac.jpl.nasa.gov/dataset/MUR-JPL-L4-GLOB-v4.1</att>\n" +
"        <att name=\"date_created\">2018-08-08T02:38:31Z</att>\n" + //changes
"        <att name=\"easternmost_longitude\">null</att>\n" +
"        <att name=\"file_quality_level\">null</att>\n" +
"        <att name=\"infoUrl\">https://podaac.jpl.nasa.gov/ws/metadata/dataset/?format=iso&amp;shortName=MUR-JPL-L4-GLOB-v04.1</att>\n" +
"        <att name=\"keywords\">1km, analysed, analysed_sst, analysis_error, area, binary, composite, daily, data, day, deviation, distribution, dt_1km_data, earth, Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature, error, estimated, field, foundation, fraction, high, ice, ice distribution, identifier, interim, jet, jpl, laboratory, land, land_binary_mask, mask, most, multi, multi-scale, mur, near, near real time, near-real-time, nrt, ocean, oceans, product, propulsion, real, recent, resolution, scale, science, sea, sea ice area fraction, sea/land, sea_ice_fraction, sea_surface_foundation_temperature, sst, standard, statistics, surface, temperature, time, time to most recent 1km data, ultra, ultra-high</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"Metadata_Conventions\">null</att>\n" +
"        <att name=\"metadata_link\">https://podaac.jpl.nasa.gov/ws/metadata/dataset/?format=iso&amp;shortName=MUR-JPL-L4-GLOB-v04.1</att>\n" +
"        <att name=\"netcdf_version_id\">null</att>\n" +
"        <att name=\"northernmost_latitude\">null</att>\n" +
"        <att name=\"publisher_type\">group</att>\n" +
"        <att name=\"publisher_url\">https://www.ghrsst.org</att>\n" +
"        <att name=\"references\">https://podaac.jpl.nasa.gov/dataset/MUR-JPL-L4-GLOB-v4.1</att>\n" +
"        <att name=\"southernmost_latitude\">null</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" +
"        <att name=\"start_time\">null</att>\n" +
"        <att name=\"stop_time\">null</att>\n" +
"        <att name=\"summary\">A merged, multi-sensor L4 Foundation Sea Surface Temperature (SST) analysis product from Jet Propulsion Laboratory (JPL).</att>\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, 
            "results.length=" + results.length() + " expected.length=" + expected.length() + 
            "\nresults=\n" + results);

        /* this test usually aren't worth the extra effort
        try {
            expected = 
        "<att name=\"testOutOfDate\">now-3days</att>\n" +  //changes
"        <att name=\"time_coverage_end\">2018-08-07T21:00:00Z</att>\n" +  //changes
"        <att name=\"time_coverage_start\">2018-08-06T21:00:00Z</att>\n"; //changes
            int po3 = results.indexOf(expected.substring(0, 25));
            Test.ensureEqual(results.substring(po3, po3 + expected.length()), expected,
                "results=\n" + results);
        } catch (Throwable t3) {
            String2.pressEnterToContinue(MustBe.throwableToString(t3) + 
                "\nThe dates will vary (or fail because no testOutOfDate) depending " +
                "on when a jplMURSST41 file was last download to this computer."); 
        }
        */


expected = 
        "<att name=\"uuid\">null</att>\n" +
"        <att name=\"westernmost_longitude\">null</att>\n" +
"    </addAttributes>\n" +
"    <axisVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"axis\">T</att>\n" +
"            <att name=\"comment\">Nominal time of analyzed fields</att>\n" +
"            <att name=\"long_name\">reference time of sst field</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"units\">seconds since 1981-01-01 00:00:00 UTC</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"units\">seconds since 1981-01-01T00:00:00Z</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>lat</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSizes\" type=\"int\">17999</att>\n" +
"            <att name=\"axis\">Y</att>\n" +
"            <att name=\"comment\">none</att>\n" +
"            <att name=\"long_name\">latitude</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"            <att name=\"valid_max\" type=\"float\">90.0</att>\n" +
"            <att name=\"valid_min\" type=\"float\">-90.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSizes\">null</att>\n" +
"            <att name=\"comment\">null</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>lon</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSizes\" type=\"int\">36000</att>\n" +
"            <att name=\"axis\">X</att>\n" +
"            <att name=\"comment\">none</att>\n" +
"            <att name=\"long_name\">longitude</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"            <att name=\"valid_max\" type=\"float\">180.0</att>\n" +
"            <att name=\"valid_min\" type=\"float\">-180.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSizes\">null</att>\n" +
"            <att name=\"comment\">null</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>analysed_sst</sourceName>\n" +
"        <destinationName>analysed_sst</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSizes\" type=\"intList\">1 1023 2047</att>\n" +
"            <att name=\"_FillValue\" type=\"short\">-32768</att>\n" +
"            <att name=\"add_offset\" type=\"double\">298.15</att>\n" +
"            <att name=\"comment\">Interim near-real-time (nrt) version using Multi-Resolution Variational Analysis (MRVA) method for interpolation; to be replaced by Final version</att>\n" +
"            <att name=\"coordinates\">lon lat</att>\n" +
"            <att name=\"long_name\">analysed sea surface temperature</att>\n" +
"            <att name=\"scale_factor\" type=\"double\">0.001</att>\n" +
"            <att name=\"source\">MODIS_T-JPL, MODIS_A-JPL, AMSR2-REMSS, AVHRR19_G-NAVO, AVHRRMTA_G-NAVO, iQUAM-NOAA/NESDIS, Ice_Conc-OSISAF</att>\n" +
"            <att name=\"standard_name\">sea_surface_foundation_temperature</att>\n" +
"            <att name=\"units\">kelvin</att>\n" +
"            <att name=\"valid_max\" type=\"short\">32767</att>\n" +
"            <att name=\"valid_min\" type=\"short\">-32767</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSizes\">null</att>\n" +
"            <att name=\"add_offset\" type=\"double\">25.0</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"coordinates\">null</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"units\">degree_C</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>analysis_error</sourceName>\n" +
"        <destinationName>analysis_error</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSizes\" type=\"intList\">1 1023 2047</att>\n" +
"            <att name=\"_FillValue\" type=\"short\">-32768</att>\n" +
"            <att name=\"add_offset\" type=\"double\">0.0</att>\n" +
"            <att name=\"comment\">none</att>\n" +
"            <att name=\"coordinates\">lon lat</att>\n" +
"            <att name=\"long_name\">estimated error standard deviation of analysed_sst</att>\n" +
"            <att name=\"scale_factor\" type=\"double\">0.01</att>\n" +
"            <att name=\"units\">kelvin</att>\n" +
"            <att name=\"valid_max\" type=\"short\">32767</att>\n" +
"            <att name=\"valid_min\" type=\"short\">0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSizes\">null</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">5.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"comment\">null</att>\n" +
"            <att name=\"coordinates\">null</att>\n" +
"            <att name=\"ioos_category\">Statistics</att>\n" +
"            <att name=\"units\">degree_C</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>mask</sourceName>\n" +
"        <destinationName>mask</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSizes\" type=\"intList\">1 1447 2895</att>\n" +
"            <att name=\"_FillValue\" type=\"byte\">-128</att>\n" +
"            <att name=\"comment\">mask can be used to further filter the data.</att>\n" +
"            <att name=\"coordinates\">lon lat</att>\n" +
"            <att name=\"flag_masks\" type=\"byteList\">1 2 4 8 16</att>\n" +
"            <att name=\"flag_meanings\">1=open-sea, 2=land, 5=open-lake, 9=open-sea with ice in the grid, 13=open-lake with ice in the grid</att>\n" +
"            <att name=\"flag_values\" type=\"byteList\">1 2 5 9 13</att>\n" +
"            <att name=\"long_name\">sea/land field composite mask</att>\n" +
"            <att name=\"source\">GMT &quot;grdlandmask&quot;, ice flag from sea_ice_fraction data</att>\n" +
"            <att name=\"valid_max\" type=\"byte\">31</att>\n" +
"            <att name=\"valid_min\" type=\"byte\">1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSizes\">null</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">20.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"coordinates\">null</att>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"standard_name\">land_binary_mask</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>sea_ice_fraction</sourceName>\n" +
"        <destinationName>sea_ice_fraction</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSizes\" type=\"intList\">1 1447 2895</att>\n" +
"            <att name=\"_FillValue\" type=\"byte\">-128</att>\n" +
"            <att name=\"add_offset\" type=\"double\">0.0</att>\n" +
"            <att name=\"comment\">ice data interpolated by a nearest neighbor approach.</att>\n" +
"            <att name=\"coordinates\">lon lat</att>\n" +
"            <att name=\"long_name\">sea ice area fraction</att>\n" +
"            <att name=\"scale_factor\" type=\"double\">0.01</att>\n" +
"            <att name=\"source\">EUMETSAT OSI-SAF, copyright EUMETSAT</att>\n" +
"            <att name=\"standard_name\">sea ice area fraction</att>\n" +
"            <att name=\"units\">fraction (between 0 and 1)</att>\n" +
"            <att name=\"valid_max\" type=\"byte\">100</att>\n" +
"            <att name=\"valid_min\" type=\"byte\">0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSizes\">null</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">1.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"coordinates\">null</att>\n" +
"            <att name=\"ioos_category\">Ice Distribution</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>dt_1km_data</sourceName>\n" +
"        <destinationName>dt_1km_data</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSizes\" type=\"intList\">1 1447 2895</att>\n" +
"            <att name=\"_FillValue\" type=\"byte\">-128</att>\n" +
"            <att name=\"comment\">The grid value is hours between the analysis time and the most recent MODIS or VIIRS 1km L2P datum within 0.01 degrees from the grid point.  &quot;Fill value&quot; indicates absence of such 1km data at the grid point.</att>\n" +
"            <att name=\"coordinates\">lon lat</att>\n" +
"            <att name=\"long_name\">time to most recent 1km data</att>\n" +
"            <att name=\"source\">MODIS and VIIRS pixels ingested by MUR</att>\n" +
"            <att name=\"standard_name\">time to most recent 1km data</att>\n" +
"            <att name=\"units\">hours</att>\n" +
"            <att name=\"valid_max\" type=\"byte\">127</att>\n" +
"            <att name=\"valid_min\" type=\"byte\">-127</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSizes\">null</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">200.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-200.0</att>\n" +
"            <att name=\"coordinates\">null</att>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";
        int po = results.indexOf(expected.substring(0, 25));
        Test.ensureEqual(results.substring(po), expected, "results=\n" + results);

        String2.log("\nEDDGridFromNcFiles.testGenerateDatasetsXml4 passed the test.");

    }


    /** 
     * This tests generateDatasetsXml, specifically using different dimensionsCSV
     * parameter values to request datasets with different variables using different axes. 
     * dimensionsCSV picks one combo unless axes are specified.
     * @throws Throwable if touble
     */
    public static void testGenerateDatasetsXml5() throws Throwable {

        String2.log("\n*** EDDGridFromNcFiles.testGenerateDatasetsXml5");
        reallyVerbose = true;
        String results, gdxResults, expected;

        String fileDir = String2.unitTestBigDataDir + "nc/";
        String fileRegex = "V20172742017304\\.L3m_MO_SNPP_CHL_chlor_a_4km\\.nc";  //just 1 file, don't aggregate


        //*** dimensionsCSV=""
        results = generateDatasetsXml(fileDir, fileRegex, "",
            "", //group
            "", -1, null, null) + "\n"; //dimensionsCSV, reloadMinutes, cacheFromUrl

        //GenerateDatasetsXml
        gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDGridFromNcFiles", fileDir, fileRegex, "",
            "", //group
            "", "-1", ""}, //dimensionsCSV, reloadMinutes, cacheFromUrl
            false); //doIt loop?
        Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt. " + 
            gdxResults.length() + " " + results.length());

        expected = 
"<!-- NOTE! The source for this dataset has nGridVariables=2,\n" +
"  but this dataset will only serve 1 because the others use different dimensions. -->\n" +
"<dataset type=\"EDDGridFromNcFiles\" datasetID=\"nc_3077_1111_bdf4\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>/erddapTestBig/nc/</fileDir>\n" +
"    <fileNameRegex>V20172742017304\\.L3m_MO_SNPP_CHL_chlor_a_4km\\.nc</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <matchAxisNDigits>20</matchAxisNDigits>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"_lastModified\">2017-11-18T01:49:38.000Z</att>\n" +
"        <att name=\"cdm_data_type\">grid</att>\n" +
"        <att name=\"Conventions\">CF-1.6</att>\n" +
"        <att name=\"creator_email\">data@oceancolor.gsfc.nasa.gov</att>\n" +
"        <att name=\"creator_name\">NASA/GSFC/OBPG</att>\n" +
"        <att name=\"creator_url\">http://oceandata.sci.gsfc.nasa.gov</att>\n" +
"        <att name=\"data_bins\" type=\"int\">16868629</att>\n" +
"        <att name=\"data_maximum\" type=\"float\">99.27857</att>\n" +
"        <att name=\"data_minimum\" type=\"float\">0.0049333195</att>\n" +
"        <att name=\"date_created\">2017-11-18T01:49:38.000Z</att>\n" +
"        <att name=\"easternmost_longitude\" type=\"float\">180.0</att>\n" +
"        <att name=\"end_orbit_number\" type=\"int\">31151</att>\n" +
"        <att name=\"geospatial_lat_max\" type=\"float\">90.0</att>\n" +
"        <att name=\"geospatial_lat_min\" type=\"float\">-90.0</att>\n" +
"        <att name=\"geospatial_lat_resolution\" type=\"float\">4.6383123</att>\n" +
"        <att name=\"geospatial_lat_units\">degrees_north</att>\n" +
"        <att name=\"geospatial_lon_max\" type=\"float\">180.0</att>\n" +
"        <att name=\"geospatial_lon_min\" type=\"float\">-180.0</att>\n" +
"        <att name=\"geospatial_lon_resolution\" type=\"float\">4.6383123</att>\n" +
"        <att name=\"geospatial_lon_units\">degrees_east</att>\n" +
"        <att name=\"grid_mapping_name\">latitude_longitude</att>\n" +
"        <att name=\"history\">l3mapgen par=V20172742017304.L3m_MO_SNPP_CHL_chlor_a_4km.nc.param </att>\n" +
"        <att name=\"id\">V20172742017304.L3b_MO_SNPP_CHL.nc/L3/V20172742017304.L3b_MO_SNPP_CHL.nc</att>\n" +
"        <att name=\"identifier_product_doi\">http://dx.doi.org</att>\n" +
"        <att name=\"identifier_product_doi_authority\">http://dx.doi.org</att>\n" +
"        <att name=\"institution\">NASA Goddard Space Flight Center, Ocean Ecology Laboratory, Ocean Biology Processing Group</att>\n" +
"        <att name=\"instrument\">VIIRS</att>\n" +
"        <att name=\"keywords\">Oceans &gt; Ocean Chemistry &gt; Chlorophyll; Oceans &gt; Ocean Optics &gt; Ocean Color</att>\n" +
"        <att name=\"keywords_vocabulary\">NASA Global Change Master Directory (GCMD) Science Keywords</att>\n" +
"        <att name=\"l2_flag_names\">ATMFAIL,LAND,HILT,HISATZEN,STRAYLIGHT,CLDICE,COCCOLITH,LOWLW,CHLWARN,CHLFAIL,NAVWARN,MAXAERITER,ATMWARN,HISOLZEN,NAVFAIL,FILTER,HIGLINT</att>\n" +
"        <att name=\"latitude_step\" type=\"float\">0.041666668</att>\n" +
"        <att name=\"latitude_units\">degrees_north</att>\n" +
"        <att name=\"license\">http://science.nasa.gov/earth-science/earth-science-data/data-information-policy/</att>\n" +
"        <att name=\"longitude_step\" type=\"float\">0.041666668</att>\n" +
"        <att name=\"longitude_units\">degrees_east</att>\n" +
"        <att name=\"map_projection\">Equidistant Cylindrical</att>\n" +
"        <att name=\"measure\">Mean</att>\n" +
"        <att name=\"Metadata_Conventions\">Unidata Dataset Discovery v1.0</att>\n" +
"        <att name=\"naming_authority\">gov.nasa.gsfc.sci.oceandata</att>\n" +
"        <att name=\"northernmost_latitude\" type=\"float\">90.0</att>\n" +
"        <att name=\"number_of_columns\" type=\"int\">8640</att>\n" +
"        <att name=\"number_of_lines\" type=\"int\">4320</att>\n" +
"        <att name=\"platform\">Suomi-NPP</att>\n" +
"        <att name=\"processing_level\">L3 Mapped</att>\n" +
"        <att name=\"processing_version\">2014.0.2</att>\n" +
"        <att name=\"product_name\">V20172742017304.L3m_MO_SNPP_CHL_chlor_a_4km.nc</att>\n" +
"        <att name=\"project\">Ocean Biology Processing Group (NASA/GSFC/OBPG)</att>\n" +
"        <att name=\"publisher_email\">data@oceancolor.gsfc.nasa.gov</att>\n" +
"        <att name=\"publisher_name\">NASA/GSFC/OBPG</att>\n" +
"        <att name=\"publisher_url\">http://oceandata.sci.gsfc.nasa.gov</att>\n" +
"        <att name=\"southernmost_latitude\" type=\"float\">-90.0</att>\n" +
"        <att name=\"spatialResolution\">4.64 km</att>\n" +
"        <att name=\"standard_name_vocabulary\">NetCDF Climate and Forecast (CF) Metadata Convention</att>\n" +
"        <att name=\"start_orbit_number\" type=\"int\">30711</att>\n" +
"        <att name=\"suggested_image_scaling_applied\">No</att>\n" +
"        <att name=\"suggested_image_scaling_maximum\" type=\"float\">20.0</att>\n" +
"        <att name=\"suggested_image_scaling_minimum\" type=\"float\">0.01</att>\n" +
"        <att name=\"suggested_image_scaling_type\">LOG</att>\n" +
"        <att name=\"sw_point_latitude\" type=\"float\">-89.979164</att>\n" +
"        <att name=\"sw_point_longitude\" type=\"float\">-179.97917</att>\n" +
"        <att name=\"temporal_range\">month</att>\n" +
"        <att name=\"time_coverage_end\">2017-11-01T02:23:58.000Z</att>\n" +
"        <att name=\"time_coverage_start\">2017-10-01T00:18:00.000Z</att>\n" +
"        <att name=\"title\">VIIRSN Level-3 Standard Mapped Image</att>\n" +
"        <att name=\"westernmost_longitude\" type=\"float\">-180.0</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"Conventions\">CF-1.6, COARDS, ACDD-1.3</att>\n" +
"        <att name=\"creator_type\">group</att>\n" +
"        <att name=\"creator_url\">https://oceandata.sci.gsfc.nasa.gov</att>\n" +
"        <att name=\"data_bins\">null</att>\n" +
"        <att name=\"data_maximum\">null</att>\n" +
"        <att name=\"data_minimum\">null</att>\n" +
"        <att name=\"easternmost_longitude\">null</att>\n" +
"        <att name=\"end_orbit_number\">null</att>\n" +
"        <att name=\"history\">l3mapgen par=V20172742017304.L3m_MO_SNPP_CHL_chlor_a_4km.nc.param</att>\n" +
"        <att name=\"identifier_product_doi\">https://dx.doi.org</att>\n" +
"        <att name=\"identifier_product_doi_authority\">https://dx.doi.org</att>\n" +
"        <att name=\"infoUrl\">https://oceandata.sci.gsfc.nasa.gov</att>\n" +
"        <att name=\"institution\">NASA/GSFC OBPG</att>\n" +
"        <att name=\"keywords\">algorithm, biology, center, chemistry, chlor_a, chlorophyll, color, concentration, concentration_of_chlorophyll_in_sea_water, data, earth, Earth Science &gt; Oceans &gt; Ocean Chemistry &gt; Chlorophyll, Earth Science &gt; Oceans &gt; Ocean Optics &gt; Ocean Color, flight, goddard, group, gsfc, image, imager, imager/radiometer, imaging, infrared, L3, level, level-3, mapped, nasa, national, npp, obpg, ocean, ocean color, oceans, oci, optics, orbiting, partnership, polar, polar-orbiting, processing, radiometer, science, sea, seawater, smi, space, standard, suite, suite/suomi-npp, suomi, viirs, viirs-n, viirsn, visible, water</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"latitude_step\">null</att>\n" +
"        <att name=\"latitude_units\">null</att>\n" +
"        <att name=\"license\">https://science.nasa.gov/earth-science/earth-science-data/data-information-policy/</att>\n" +
"        <att name=\"longitude_step\">null</att>\n" +
"        <att name=\"longitude_units\">null</att>\n" +
"        <att name=\"Metadata_Conventions\">null</att>\n" +
"        <att name=\"northernmost_latitude\">null</att>\n" +
"        <att name=\"number_of_columns\">null</att>\n" +
"        <att name=\"number_of_lines\">null</att>\n" +
"        <att name=\"publisher_type\">group</att>\n" +
"        <att name=\"publisher_url\">https://oceandata.sci.gsfc.nasa.gov</att>\n" +
"        <att name=\"southernmost_latitude\">null</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" +
"        <att name=\"start_orbit_number\">null</att>\n" +
"        <att name=\"suggested_image_scaling_applied\">null</att>\n" +
"        <att name=\"suggested_image_scaling_maximum\">null</att>\n" +
"        <att name=\"suggested_image_scaling_minimum\">null</att>\n" +
"        <att name=\"suggested_image_scaling_type\">null</att>\n" +
"        <att name=\"summary\">Visible and Infrared Imager/Radiometer Suite/Suomi-NPP (VIIRSN) Level-3 Standard Mapped Image</att>\n" +
"        <att name=\"sw_point_latitude\">null</att>\n" +
"        <att name=\"sw_point_longitude\">null</att>\n" +
"        <att name=\"title\">VIIRSN L3 SMI,</att>\n" +
"        <att name=\"westernmost_longitude\">null</att>\n" +
"    </addAttributes>\n" +
"    <axisVariable>\n" +
"        <sourceName>lat</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-999.0</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"            <att name=\"valid_max\" type=\"float\">90.0</att>\n" +
"            <att name=\"valid_min\" type=\"float\">-90.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>lon</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-999.0</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"            <att name=\"valid_max\" type=\"float\">180.0</att>\n" +
"            <att name=\"valid_min\" type=\"float\">-180.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>chlor_a</sourceName>\n" +
"        <destinationName>chlor_a</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSizes\" type=\"intList\">44 87</att>\n" +
"            <att name=\"_FillValue\" type=\"float\">-32767.0</att>\n" +
"            <att name=\"display_max\" type=\"float\">20.0</att>\n" +
"            <att name=\"display_min\" type=\"float\">0.01</att>\n" +
"            <att name=\"display_scale\">log</att>\n" +
"            <att name=\"long_name\">Chlorophyll Concentration, OCI Algorithm</att>\n" +
"            <att name=\"reference\">Hu, C., Lee Z., and Franz, B.A. (2012). Chlorophyll-a algorithms for oligotrophic oceans: A novel approach based on three-band reflectance difference, J. Geophys. Res., 117, C01011, doi:10.1029/2011JC007395.</att>\n" +
"            <att name=\"standard_name\">mass_concentration_chlorophyll_concentration_in_sea_water</att>\n" +
"            <att name=\"units\">mg m^-3</att>\n" +
"            <att name=\"valid_max\" type=\"float\">100.0</att>\n" +
"            <att name=\"valid_min\" type=\"float\">0.001</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSizes\">null</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">30.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.03</att>\n" +
"            <att name=\"colorBarScale\">Log</att>\n" +
"            <att name=\"display_max\">null</att>\n" +
"            <att name=\"display_min\">null</att>\n" +
"            <att name=\"display_scale\">null</att>\n" +
"            <att name=\"ioos_category\">Ocean Color</att>\n" +
"            <att name=\"reference\">null</att>\n" +
"            <att name=\"references\">Hu, C., Lee Z., and Franz, B.A. (2012). Chlorophyll-a algorithms for oligotrophic oceans: A novel approach based on three-band reflectance difference, J. Geophys. Res., 117, C01011, doi:10.1029/2011JC007395.</att>\n" +
"            <att name=\"standard_name\">concentration_of_chlorophyll_in_sea_water</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n" +
"\n";
        Test.ensureEqual(results, expected, "results=\n" + results);


        //*** dimensionsCSV="lat,lon"
        results = generateDatasetsXml(fileDir, fileRegex, "",
            "", //group
            "lat, lon", -1, null, null) + "\n"; //dimensionsCSV, reloadMinutes, cacheFromUrl

        //expected is unchanged except for datasetID (because dimensionsCSV changed)
        expected = String2.replaceAll(expected, "nc_3077_1111_bdf4", "nc_7344_9960_7945");
        Test.ensureEqual(results, expected, "results=\n" + results);


        //*** dimensionsCSV="rgb,eightbitcolor"
        results = generateDatasetsXml(fileDir, fileRegex, "",
            "", //group
            "rgb, eightbitcolor", -1, null, null) + "\n"; //dimensionsCSV, reloadMinutes, cacheFromUrl

        expected = //this is used as testUnsignedGrid in datasets2.xml
"<!-- NOTE! The source for this dataset has nGridVariables=2,\n" +
"  but this dataset will only serve 1 because the others use different dimensions. -->\n" +
"<dataset type=\"EDDGridFromNcFiles\" datasetID=\"nc_0304_078a_e6f9\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>/erddapTestBig/nc/</fileDir>\n" +
"    <fileNameRegex>V20172742017304\\.L3m_MO_SNPP_CHL_chlor_a_4km\\.nc</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <matchAxisNDigits>20</matchAxisNDigits>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"_lastModified\">2017-11-18T01:49:38.000Z</att>\n" +
"        <att name=\"cdm_data_type\">grid</att>\n" +
"        <att name=\"Conventions\">CF-1.6</att>\n" +
"        <att name=\"creator_email\">data@oceancolor.gsfc.nasa.gov</att>\n" +
"        <att name=\"creator_name\">NASA/GSFC/OBPG</att>\n" +
"        <att name=\"creator_url\">http://oceandata.sci.gsfc.nasa.gov</att>\n" +
"        <att name=\"data_bins\" type=\"int\">16868629</att>\n" +
"        <att name=\"data_maximum\" type=\"float\">99.27857</att>\n" +
"        <att name=\"data_minimum\" type=\"float\">0.0049333195</att>\n" +
"        <att name=\"date_created\">2017-11-18T01:49:38.000Z</att>\n" +
"        <att name=\"easternmost_longitude\" type=\"float\">180.0</att>\n" +
"        <att name=\"end_orbit_number\" type=\"int\">31151</att>\n" +
"        <att name=\"geospatial_lat_max\" type=\"float\">90.0</att>\n" +
"        <att name=\"geospatial_lat_min\" type=\"float\">-90.0</att>\n" +
"        <att name=\"geospatial_lat_resolution\" type=\"float\">4.6383123</att>\n" +
"        <att name=\"geospatial_lat_units\">degrees_north</att>\n" +
"        <att name=\"geospatial_lon_max\" type=\"float\">180.0</att>\n" +
"        <att name=\"geospatial_lon_min\" type=\"float\">-180.0</att>\n" +
"        <att name=\"geospatial_lon_resolution\" type=\"float\">4.6383123</att>\n" +
"        <att name=\"geospatial_lon_units\">degrees_east</att>\n" +
"        <att name=\"grid_mapping_name\">latitude_longitude</att>\n" +
"        <att name=\"history\">l3mapgen par=V20172742017304.L3m_MO_SNPP_CHL_chlor_a_4km.nc.param </att>\n" +
"        <att name=\"id\">V20172742017304.L3b_MO_SNPP_CHL.nc/L3/V20172742017304.L3b_MO_SNPP_CHL.nc</att>\n" +
"        <att name=\"identifier_product_doi\">http://dx.doi.org</att>\n" +
"        <att name=\"identifier_product_doi_authority\">http://dx.doi.org</att>\n" +
"        <att name=\"institution\">NASA Goddard Space Flight Center, Ocean Ecology Laboratory, Ocean Biology Processing Group</att>\n" +
"        <att name=\"instrument\">VIIRS</att>\n" +
"        <att name=\"keywords\">Oceans &gt; Ocean Chemistry &gt; Chlorophyll; Oceans &gt; Ocean Optics &gt; Ocean Color</att>\n" +
"        <att name=\"keywords_vocabulary\">NASA Global Change Master Directory (GCMD) Science Keywords</att>\n" +
"        <att name=\"l2_flag_names\">ATMFAIL,LAND,HILT,HISATZEN,STRAYLIGHT,CLDICE,COCCOLITH,LOWLW,CHLWARN,CHLFAIL,NAVWARN,MAXAERITER,ATMWARN,HISOLZEN,NAVFAIL,FILTER,HIGLINT</att>\n" +
"        <att name=\"latitude_step\" type=\"float\">0.041666668</att>\n" +
"        <att name=\"latitude_units\">degrees_north</att>\n" +
"        <att name=\"license\">http://science.nasa.gov/earth-science/earth-science-data/data-information-policy/</att>\n" +
"        <att name=\"longitude_step\" type=\"float\">0.041666668</att>\n" +
"        <att name=\"longitude_units\">degrees_east</att>\n" +
"        <att name=\"map_projection\">Equidistant Cylindrical</att>\n" +
"        <att name=\"measure\">Mean</att>\n" +
"        <att name=\"Metadata_Conventions\">Unidata Dataset Discovery v1.0</att>\n" +
"        <att name=\"naming_authority\">gov.nasa.gsfc.sci.oceandata</att>\n" +
"        <att name=\"northernmost_latitude\" type=\"float\">90.0</att>\n" +
"        <att name=\"number_of_columns\" type=\"int\">8640</att>\n" +
"        <att name=\"number_of_lines\" type=\"int\">4320</att>\n" +
"        <att name=\"platform\">Suomi-NPP</att>\n" +
"        <att name=\"processing_level\">L3 Mapped</att>\n" +
"        <att name=\"processing_version\">2014.0.2</att>\n" +
"        <att name=\"product_name\">V20172742017304.L3m_MO_SNPP_CHL_chlor_a_4km.nc</att>\n" +
"        <att name=\"project\">Ocean Biology Processing Group (NASA/GSFC/OBPG)</att>\n" +
"        <att name=\"publisher_email\">data@oceancolor.gsfc.nasa.gov</att>\n" +
"        <att name=\"publisher_name\">NASA/GSFC/OBPG</att>\n" +
"        <att name=\"publisher_url\">http://oceandata.sci.gsfc.nasa.gov</att>\n" +
"        <att name=\"southernmost_latitude\" type=\"float\">-90.0</att>\n" +
"        <att name=\"spatialResolution\">4.64 km</att>\n" +
"        <att name=\"standard_name_vocabulary\">NetCDF Climate and Forecast (CF) Metadata Convention</att>\n" +
"        <att name=\"start_orbit_number\" type=\"int\">30711</att>\n" +
"        <att name=\"suggested_image_scaling_applied\">No</att>\n" +
"        <att name=\"suggested_image_scaling_maximum\" type=\"float\">20.0</att>\n" +
"        <att name=\"suggested_image_scaling_minimum\" type=\"float\">0.01</att>\n" +
"        <att name=\"suggested_image_scaling_type\">LOG</att>\n" +
"        <att name=\"sw_point_latitude\" type=\"float\">-89.979164</att>\n" +
"        <att name=\"sw_point_longitude\" type=\"float\">-179.97917</att>\n" +
"        <att name=\"temporal_range\">month</att>\n" +
"        <att name=\"time_coverage_end\">2017-11-01T02:23:58.000Z</att>\n" +
"        <att name=\"time_coverage_start\">2017-10-01T00:18:00.000Z</att>\n" +
"        <att name=\"title\">VIIRSN Level-3 Standard Mapped Image</att>\n" +
"        <att name=\"westernmost_longitude\" type=\"float\">-180.0</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"Conventions\">CF-1.6, COARDS, ACDD-1.3</att>\n" +
"        <att name=\"creator_type\">group</att>\n" +
"        <att name=\"creator_url\">https://oceandata.sci.gsfc.nasa.gov</att>\n" +
"        <att name=\"data_bins\">null</att>\n" +
"        <att name=\"data_maximum\">null</att>\n" +
"        <att name=\"data_minimum\">null</att>\n" +
"        <att name=\"easternmost_longitude\">null</att>\n" +
"        <att name=\"end_orbit_number\">null</att>\n" +
"        <att name=\"history\">l3mapgen par=V20172742017304.L3m_MO_SNPP_CHL_chlor_a_4km.nc.param</att>\n" +
"        <att name=\"identifier_product_doi\">https://dx.doi.org</att>\n" +
"        <att name=\"identifier_product_doi_authority\">https://dx.doi.org</att>\n" +
"        <att name=\"infoUrl\">https://oceandata.sci.gsfc.nasa.gov</att>\n" +
"        <att name=\"institution\">NASA/GSFC OBPG</att>\n" +
"        <att name=\"keywords\">biology, center, chemistry, chlorophyll, color, data, Earth Science &gt; Oceans &gt; Ocean Chemistry &gt; Chlorophyll, Earth Science &gt; Oceans &gt; Ocean Optics &gt; Ocean Color, eightbitcolor, flight, goddard, group, gsfc, image, imager, imager/radiometer, imaging, infrared, L3, level, level-3, mapped, nasa, national, npp, obpg, ocean, oceans, optics, orbiting, palette, partnership, polar, polar-orbiting, processing, radiometer, rgb, smi, space, standard, suite, suite/suomi-npp, suomi, viirs, viirs-n, viirsn, visible</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"latitude_step\">null</att>\n" +
"        <att name=\"latitude_units\">null</att>\n" +
"        <att name=\"license\">https://science.nasa.gov/earth-science/earth-science-data/data-information-policy/</att>\n" +
"        <att name=\"longitude_step\">null</att>\n" +
"        <att name=\"longitude_units\">null</att>\n" +
"        <att name=\"Metadata_Conventions\">null</att>\n" +
"        <att name=\"northernmost_latitude\">null</att>\n" +
"        <att name=\"number_of_columns\">null</att>\n" +
"        <att name=\"number_of_lines\">null</att>\n" +
"        <att name=\"publisher_type\">group</att>\n" +
"        <att name=\"publisher_url\">https://oceandata.sci.gsfc.nasa.gov</att>\n" +
"        <att name=\"southernmost_latitude\">null</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" +
"        <att name=\"start_orbit_number\">null</att>\n" +
"        <att name=\"suggested_image_scaling_applied\">null</att>\n" +
"        <att name=\"suggested_image_scaling_maximum\">null</att>\n" +
"        <att name=\"suggested_image_scaling_minimum\">null</att>\n" +
"        <att name=\"suggested_image_scaling_type\">null</att>\n" +
"        <att name=\"summary\">Visible and Infrared Imager/Radiometer Suite/Suomi-NPP (VIIRSN) Level-3 Standard Mapped Image</att>\n" +
"        <att name=\"sw_point_latitude\">null</att>\n" +
"        <att name=\"sw_point_longitude\">null</att>\n" +
"        <att name=\"title\">VIIRSN L3 SMI,</att>\n" +
"        <att name=\"westernmost_longitude\">null</att>\n" +
"    </addAttributes>\n" +
"    <axisVariable>\n" +
"        <sourceName>rgb</sourceName>\n" +
"        <destinationName>rgb</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Other</att>\n" +
"            <att name=\"long_name\">RGB</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>eightbitcolor</sourceName>\n" +
"        <destinationName>eightbitcolor</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Other</att>\n" +
"            <att name=\"long_name\">Eightbitcolor</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>palette</sourceName>\n" +
"        <destinationName>palette</destinationName>\n" +
"        <dataType>ubyte</dataType>\n" +  //NcHelper.ncdump() says "ubyte" in the file
"        <!-- sourceAttributes>\n" +
//"            <att name=\"_Unsigned\">true</att>\n" +  //now removed when dataType is read
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Other</att>\n" +
"            <att name=\"long_name\">Palette</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n" +
"\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        String2.log("\nEDDGridFromNcFiles.testGenerateDatasetsXml5 passed the test.");

    }

    /** This tests generateDatasetsXml with an AWS S3 dataset and using cacheFromUrl. 
     * @throws Throwable if touble
     */
    public static void testGenerateDatasetsXmlAwsS3() throws Throwable {

        String2.log("\n*** EDDGridFromNcFiles.testGenerateDatasetsXmlAwsS3");

        try {
        String cacheFromUrl = "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS"; //intentionally left off trailing /
        String regex = ".*_CESM1-CAM5_201.*\\.nc";
        String dir = "/u00/data/points/testAwsS3NexDcp/";
        String name = ""; //with cacheFromUrl, use nothing.   Was: dir + "tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_CESM1-CAM5_200601-201012.nc";
        int reload = 1000000;
        //if ("f".equals(answer))   //full test?
        {
            //delete the cached files
            String2.log("Emptying cache...  nRemain=" + File2.deleteAllFiles(dir, true, true)); //recursive, deleteEmptySubdirectories
            String2.log("Deleting " + dir + "  result=" + File2.delete(dir)); //must be empty      
        }

        String results = generateDatasetsXml(dir, regex, name, 
            "", //group
            "", reload, cacheFromUrl, null) + "\n";  //dimensionsCSV, reloadMinutes, cacheFromUrl
        String suggDatasetID = suggestDatasetID(dir + regex);

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDGridFromNcFiles", dir, regex, name, 
            "", //group
            "", "" + reload, cacheFromUrl}, 
            false); //doIt loop?
        Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt. " + 
            gdxResults.length() + " " + results.length());

        String expected = 
"<!-- NOTE! The source for this dataset has nGridVariables=4,\n" +
"  but this dataset will only serve 1 because the others use different dimensions. -->\n" +
"<dataset type=\"EDDGridFromNcFiles\" datasetID=\"testAwsS3NexDcp_3312_e3e8_65ae\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1000000</reloadEveryNMinutes>\n" +
"    <cacheFromUrl>https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS</cacheFromUrl>\n" +
"    <fileDir>/u00/data/points/testAwsS3NexDcp/</fileDir>\n" +
"    <fileNameRegex>.*_CESM1-CAM5_201.*\\.nc</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <matchAxisNDigits>20</matchAxisNDigits>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"CMIPtable\">Amon</att>\n" +
"        <att name=\"contact\">Dr. Rama Nemani: rama.nemani@nasa.gov, Dr. Bridget Thrasher: bridget@climateanalyticsgroup.org, and Dr. Mark Snyder: mark@climateanalyticsgroup.org</att>\n" +
"        <att name=\"Conventions\">CF-1.4</att>\n" +
"        <att name=\"creation_date\">Wed Sep 12 14:44:42 PDT 2012</att>\n" + //varies a little with sample file -- see other similar changes below
"        <att name=\"DOI\">http://dx.doi.org/10.7292/W0WD3XH4</att>\n" +
"        <att name=\"downscalingModel\">BCSD</att>\n" +
"        <att name=\"driving_data_tracking_ids\">N/A</att>\n" +
"        <att name=\"driving_experiment\">historical</att>\n" +
"        <att name=\"driving_experiment_name\">historical</att>\n" +
"        <att name=\"driving_model_ensemble_member\">r3i1p1</att>\n" +
"        <att name=\"driving_model_id\">CESM1-CAM5</att>\n" +
"        <att name=\"experiment\">RCP2.6</att>\n" +
"        <att name=\"experiment_id\">rcp26</att>\n" +
"        <att name=\"frequency\">mon</att>\n" +
"        <att name=\"initialization_method\">1</att>\n" +
"        <att name=\"institute_id\">NASA-Ames</att>\n" +
"        <att name=\"institution\">NASA Earth Exchange, NASA Ames Research Center, Moffett Field, CA 94035</att>\n" +
"        <att name=\"model_id\">BCSD</att>\n" +
"        <att name=\"modeling_realm\">atmos</att>\n" +
"        <att name=\"parent_experiment\">historical</att>\n" +
"        <att name=\"parent_experiment_id\">historical</att>\n" +
"        <att name=\"parent_experiment_rip\">r1i1p1</att>\n" +
"        <att name=\"physics_version\">1</att>\n" +
"        <att name=\"product\">downscaled</att>\n" +
"        <att name=\"project_id\">NEX</att>\n" +
"        <att name=\"realization\">1</att>\n" +
"        <att name=\"realm\">atmos</att>\n" +
"        <att name=\"references\">BCSD method: Wood AW, Maurer EP, Kumar A, Lettenmaier DP, 2002, J Geophys Res 107(D20):4429 &amp; \n" +
" Wood AW, Leung LR, Sridhar V, Lettenmaier DP, 2004, Clim Change 62:189-216\n" +
" Reference period obs: PRISM (http://www.prism.oregonstate.edu/)</att>\n" +
"        <att name=\"region\">CONUS</att>\n" +
"        <att name=\"region_id\">CONUS</att>\n" +
"        <att name=\"region_lexicon\">http://en.wikipedia.org/wiki/Contiguous_United_States</att>\n" +
"        <att name=\"resolution_id\">800m</att>\n" +
"        <att name=\"table_id\">Table Amon</att>\n" +
"        <att name=\"title\">800m Downscaled NEX CMIP5 Climate Projections for the Continental US</att>\n" +
"        <att name=\"tracking_id\">da8a69d2-af11-11e2-a9d5-e41f134d5304</att>\n" + //varies with sample file
"        <att name=\"variableName\">tasmin</att>\n" +
"        <att name=\"version\">1.0</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"Conventions\">CF-1.6, COARDS, ACDD-1.3</att>\n" +
"        <att name=\"creator_email\">rama.nemani@nasa.gov</att>\n" +
"        <att name=\"creator_name\">Rama Nemani</att>\n" +
"        <att name=\"creator_type\">person</att>\n" +
"        <att name=\"creator_url\">https://www.nasa.gov/</att>\n" +
"        <att name=\"DOI\">https://dx.doi.org/10.7292/W0WD3XH4</att>\n" +
"        <att name=\"driving_data_tracking_ids\">null</att>\n" +
"        <att name=\"infoUrl\">https://www.nasa.gov/</att>\n" +
"        <att name=\"keywords\">800m, air, air_temperature, ames, atmosphere, atmospheric, center, climate, cmip5, continental, daily, data, day, downscaled, earth, Earth Science &gt; Atmosphere &gt; Atmospheric Temperature &gt; Air Temperature, Earth Science &gt; Atmosphere &gt; Atmospheric Temperature &gt; Surface Air Temperature, exchange, field, intercomparison, minimum, model, moffett, nasa, near, near-surface, nex, project, projections, research, science, surface, tasmin, temperature, time, US</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"region_lexicon\">https://en.wikipedia.org/wiki/Contiguous_United_States</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" +
"        <att name=\"summary\">800m Downscaled NEX Climate Model Intercomparison Project 5 (CMIP5) Climate Projections for the Continental US</att>\n" +
"    </addAttributes>\n" +
"    <axisVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSizes\" type=\"int\">1</att>\n" +
"            <att name=\"axis\">T</att>\n" +
"            <att name=\"bounds\">time_bnds</att>\n" +
"            <att name=\"calendar\">standard</att>\n" +
"            <att name=\"long_name\">time</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"units\">days since 1950-01-01 00:00:00</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSizes\">null</att>\n" +
"            <att name=\"bounds\">null</att>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"units\">days since 1950-01-01T00:00:00Z</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>lat</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSizes\" type=\"int\">3105</att>\n" +
"            <att name=\"axis\">Y</att>\n" +
"            <att name=\"bounds\">lat_bnds</att>\n" +
"            <att name=\"long_name\">latitude</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSizes\">null</att>\n" +
"            <att name=\"bounds\">null</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>lon</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSizes\" type=\"int\">7025</att>\n" +
"            <att name=\"axis\">X</att>\n" +
"            <att name=\"bounds\">lon_bnds</att>\n" +
"            <att name=\"long_name\">longitude</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"            <att name=\"valid_range\" type=\"doubleList\">0.0 360.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSizes\">null</att>\n" +
"            <att name=\"bounds\">null</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>tasmin</sourceName>\n" +
"        <destinationName>tasmin</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSizes\" type=\"intList\">1 369 836</att>\n" +
"            <att name=\"_FillValue\" type=\"float\">1.0E20</att>\n" +
"            <att name=\"associated_files\">baseURL: http://cmip-pcmdi.llnl.gov/CMIP5/dataLocation gridspecFile: gridspec_atmos_fx_CESM1-CAM5_rcp26_r0i0p0.nc areacella: areacella_fx_CESM1-CAM5_rcp26_r0i0p0.nc</att>\n" +
"            <att name=\"cell_measures\">area: areacella</att>\n" +
"            <att name=\"cell_methods\">time: minimum (interval: 30 days) within days time: mean over days</att>\n" +
"            <att name=\"comment\">TREFMNAV no change, CMIP5_table_comment: monthly mean of the daily-minimum near-surface air temperature.</att>\n" +
"            <att name=\"coordinates\">height</att>\n" +
"            <att name=\"history\">2012-06-09T00:36:32Z altered by CMOR: Treated scalar dimension: &#39;height&#39;. 2012-06-09T00:36:32Z altered by CMOR: Reordered dimensions, original order: lat lon time. 2012-06-09T00:36:32Z altered by CMOR: replaced missing value flag (-1e+32) with standard missing value (1e+20).</att>\n" +
"            <att name=\"long_name\">Daily Minimum Near-Surface Air Temperature</att>\n" +
"            <att name=\"missing_value\" type=\"float\">1.0E20</att>\n" +
"            <att name=\"original_name\">TREFMNAV</att>\n" +
"            <att name=\"standard_name\">air_temperature</att>\n" +
"            <att name=\"units\">K</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSizes\">null</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">313.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">263.0</att>\n" +
"            <att name=\"coordinates\">null</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";
        Test.ensureEqual(results, expected, 
            "results.length=" + results.length() + " expected.length=" + expected.length() + 
            "\nresults=\n" + results);

        //ensure it is ready-to-use by making a dataset from it
        String tDatasetID = suggDatasetID;
        EDD.deleteCachedDatasetInfo(tDatasetID);
        EDD edd = oneFromXmlFragment(null, results);
        Test.ensureEqual(edd.datasetID(), suggDatasetID, "");
        Test.ensureEqual(edd.title(), "800m Downscaled NEX CMIP5 Climate Projections for the Continental US", "");
        Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
            "tasmin", "");

        String2.log("\nEDDGridFromNcFiles.testGenerateDatasetsXmlAwsS3 passed the test.");

        } catch (Throwable t) {
            throw new RuntimeException( 
                "If creation_date minor variation: it varies wth sample file.\n" +
                "Otherwise: Unexpected error: Have you updated your AWS credentials lately?\n", t); 
        }
    }

    /**
     * This tests reading NetCDF .nc files with this class.
     *
     * @throws Throwable if trouble
     */
    public static void testAwsS3(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDGridFromNcFiles.testAwsS3() *****************\n");

        testVerboseOn();

        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDVGridAxis edvga;
        String id = "testAwsS3";
        if (deleteCachedDatasetInfo) 
            deleteCachedDatasetInfo(id);
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, id); 
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
        String tDir = EDStatic.fullTestCacheDirectory;

        //*** test getting das for entire dataset
        String2.log("\n*** .nc test das dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_Entire", ".das"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = //2020-10-02 lots of small changes to source metadata
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.1374128e+9, 4.1011056e+9;\n" +
"    String axis \"T\";\n" +
"    String calendar \"standard\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range 24.0625, 49.92916665632;\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range 234.97916666666998, 293.51249997659;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"    Float64 valid_range 0.0, 360.0;\n" +
"  }\n" +
"  tasmin {\n" +
"    Float32 _FillValue 1.0e+20;\n" +
"    String associated_files \"baseURL: http://cmip-pcmdi.llnl.gov/CMIP5/dataLocation gridspecFile: gridspec_atmos_fx_bcc-csm1-1_rcp26_r0i0p0.nc areacella: areacella_fx_bcc-csm1-1_rcp26_r0i0p0.nc\";\n" +
"    String cell_measures \"area: areacella\";\n" +
"    String cell_methods \"time: minimum (interval: 20 mintues) within days time: mean over days\";\n" +  //mintues [sic]
"    Float64 colorBarMaximum 313.0;\n" +
"    Float64 colorBarMinimum 263.0;\n" +
"    String comment \"monthly mean of the daily-minimum near-surface air temperature.\";\n" +
"    String history \"2011-06-27T08:46:27Z altered by CMOR: Treated scalar dimension: 'height'.\";\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Daily Minimum Near-Surface Air Temperature\";\n" +
"    Float32 missing_value 1.0e+20;\n" +
"    String original_name \"TREFMNAV\";\n" +
"    String standard_name \"air_temperature\";\n" +
"    String units \"K\";\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String CMIPtable \"Amon\";\n" +
"    String contact \"Dr. Rama Nemani: rama.nemani@nasa.gov, Dr. Bridget Thrasher: bridget@climateanalyticsgroup.org, and Dr. Mark Snyder: mark@climateanalyticsgroup.org\";\n" +
"    String Conventions \"CF-1.6, COARDS, ACDD-1.3\";\n" +
"    String creation_date \"Thu Sep  6 19:48:06 PDT 2012\";\n" +
"    String creator_email \"rama.nemani@nasa.gov\";\n" +
"    String creator_name \"Rama Nemani\";\n" +
"    String creator_url \"https://www.nasa.gov/\";\n" +
"    String DOI \"https://dx.doi.org/10.7292/W0WD3XH4\";\n" +
"    String downscalingModel \"BCSD\";\n" +
"    String driving_data_tracking_ids \"N/A\";\n" +
"    String driving_experiment \"historical\";\n" +
"    String driving_experiment_name \"historical\";\n" +
"    String driving_model_ensemble_member \"r1i1p1\";\n" +
"    String driving_model_id \"bcc-csm1-1\";\n" +
"    Float64 Easternmost_Easting 293.51249997659;\n" +
"    String experiment \"RCP2.6\";\n" +
"    String experiment_id \"rcp26\";\n" +
"    String frequency \"mon\";\n" +
"    Float64 geospatial_lat_max 49.92916665632;\n" +
"    Float64 geospatial_lat_min 24.0625;\n" +
"    Float64 geospatial_lat_resolution 0.00833333333;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 293.51249997659;\n" +
"    Float64 geospatial_lon_min 234.97916666666998;\n" +
"    Float64 geospatial_lon_resolution 0.008333333330000001;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String history \"" + today;
//2015-06-24T17:36:33Z (local files)
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);

//            + " https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/ux10/1day\n" +
//today + 

expected = 
//"2015-06-24T17:36:33Z http://localhost:8080/cwexperimental/griddap/testAwsS3.das\";\n" +
    "String infoUrl \"https://registry.opendata.aws/nasanex/\";\n" +
"    String initialization_method \"1\";\n" +
"    String institute_id \"NASA-Ames\";\n" +
"    String institution \"NASA Earth Exchange, NASA Ames Research Center, Moffett Field, CA 94035\";\n" +
"    String keywords \"800m, air, air_temperature, ames, atmosphere, atmospheric, center, climate, cmip5, continental, daily, data, day, downscaled, earth, Earth Science > Atmosphere > Atmospheric Temperature > Air Temperature, Earth Science > Atmosphere > Atmospheric Temperature > Surface Air Temperature, exchange, field, intercomparison, minimum, model, moffett, nasa, near, near-surface, nex, project, projections, research, surface, tasmin, temperature, time, US\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String model_id \"BCSD\";\n" +
"    String modeling_realm \"atmos\";\n" +
"    Float64 Northernmost_Northing 49.92916665632;\n" +
"    String parent_experiment \"historical\";\n" +
"    String parent_experiment_id \"historical\";\n" +
"    String parent_experiment_rip \"r1i1p1\";\n" +
"    String physics_version \"1\";\n" +
"    String product \"downscaled\";\n" +
"    String project_id \"NEX\";\n" +
"    String realization \"1\";\n" +
"    String realm \"atmos\";\n" +
"    String references \"BCSD method: Wood AW, Maurer EP, Kumar A, Lettenmaier DP, 2002, J Geophys Res 107(D20):4429 & \n" +
" Wood AW, Leung LR, Sridhar V, Lettenmaier DP, 2004, Clim Change 62:189-216\n" +
" Reference period obs: PRISM (http://www.prism.oregonstate.edu/)\";\n" +
"    String region \"CONUS\";\n" +
"    String region_id \"CONUS\";\n" +
"    String region_lexicon \"https://en.wikipedia.org/wiki/Contiguous_United_States\";\n" +
"    String resolution_id \"800m\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing 24.0625;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String summary \"800m Downscaled NEX Climate Model Intercomparison Project 5 (CMIP5) Climate Projections for the Continental US\";\n" +
"    String table_id \"Table Amon\";\n" +
"    String time_coverage_end \"2099-12-16T12:00:00Z\";\n" +
"    String time_coverage_start \"2006-01-16T12:00:00Z\";\n" +
"    String title \"800m Downscaled NEX CMIP5 Climate Projections for the Continental US\";\n" +
"    String tracking_id \"2b55c74a-aec1-11e2-a0c6-e41f13ef4fd4\";\n" +
"    String variableName \"tasmin\";\n" +
"    String version \"1.0\";\n" +
"    Float64 Westernmost_Easting 234.97916666666998;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 25));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_Entire", ".dds"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Float64 time[time = 1128];\n" +
"  Float64 latitude[latitude = 3105];\n" +
"  Float64 longitude[longitude = 7025];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 tasmin[time = 1128][latitude = 3105][longitude = 7025];\n" +
"    MAPS:\n" +
"      Float64 time[time = 1128];\n" +
"      Float64 latitude[latitude = 3105];\n" +
"      Float64 longitude[longitude = 7025];\n" +
"  } tasmin;\n" +
"} testAwsS3;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv  with data from one file
        String2.log("\n*** .nc test read from one file\n");       
        userDapQuery = "tasmin[1127][(40):100:(43)][(260):100:(263)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_Data1", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
/*2020-10-02 was:      [ WTF?! the models run in 2012 just changed since this test last run so now they predict 4-6deg_c hotter in 2099?! ]
"time,latitude,longitude,tasmin\n" +
"UTC,degrees_north,degrees_east,K\n" +
"2099-12-16T12:00:00Z,40.00416666029,260.00416665666,263.7364\n" +
"2099-12-16T12:00:00Z,40.00416666029,260.83749998966,263.4652\n" +
"2099-12-16T12:00:00Z,40.00416666029,261.67083332266,263.61017\n" +
"2099-12-16T12:00:00Z,40.00416666029,262.50416665566,264.5588\n" +
"2099-12-16T12:00:00Z,40.837499993289995,260.00416665666,263.3948\n" +
"2099-12-16T12:00:00Z,40.837499993289995,260.83749998966,263.02103\n" +
"2099-12-16T12:00:00Z,40.837499993289995,261.67083332266,263.31967\n" +
"2099-12-16T12:00:00Z,40.837499993289995,262.50416665566,263.83475\n" +
"2099-12-16T12:00:00Z,41.670833326289994,260.00416665666,262.9155\n" +
"2099-12-16T12:00:00Z,41.670833326289994,260.83749998966,261.65094\n" +
"2099-12-16T12:00:00Z,41.670833326289994,261.67083332266,261.9907\n" +
"2099-12-16T12:00:00Z,41.670833326289994,262.50416665566,262.13275\n" +
"2099-12-16T12:00:00Z,42.50416665929,260.00416665666,263.9838\n" +
"2099-12-16T12:00:00Z,42.50416665929,260.83749998966,262.93536\n" +
"2099-12-16T12:00:00Z,42.50416665929,261.67083332266,262.64273\n" +
"2099-12-16T12:00:00Z,42.50416665929,262.50416665566,261.5762\n";         */
"time,latitude,longitude,tasmin\n" +
"UTC,degrees_north,degrees_east,K\n" +
"2099-12-16T12:00:00Z,40.00416666029,260.00416665666,267.82053\n" +
"2099-12-16T12:00:00Z,40.00416666029,260.83749998966,267.94974\n" +
"2099-12-16T12:00:00Z,40.00416666029,261.67083332266,268.42017\n" +
"2099-12-16T12:00:00Z,40.00416666029,262.50416665566,269.35843\n" +
"2099-12-16T12:00:00Z,40.837499993289995,260.00416665666,267.59042\n" +
"2099-12-16T12:00:00Z,40.837499993289995,260.83749998966,267.9106\n" +
"2099-12-16T12:00:00Z,40.837499993289995,261.67083332266,268.75568\n" +
"2099-12-16T12:00:00Z,40.837499993289995,262.50416665566,269.03436\n" +
"2099-12-16T12:00:00Z,41.670833326289994,260.00416665666,266.94617\n" +
"2099-12-16T12:00:00Z,41.670833326289994,260.83749998966,266.56305\n" +
"2099-12-16T12:00:00Z,41.670833326289994,261.67083332266,267.4424\n" +
"2099-12-16T12:00:00Z,41.670833326289994,262.50416665566,267.81042\n" +
"2099-12-16T12:00:00Z,42.50416665929,260.00416665666,268.1312\n" +
"2099-12-16T12:00:00Z,42.50416665929,260.83749998966,267.71857\n" +
"2099-12-16T12:00:00Z,42.50416665929,261.67083332266,267.5857\n" +
"2099-12-16T12:00:00Z,42.50416665929,262.50416665566,267.41855\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //img
        tName = eddGrid.makeNewFileForDapQuery(null, null, 
            "tasmin[(2099-12-16T12:00:00Z)][(24.0625):(49.92916665632)][(234.97916666666998):" +
            "(293.51249997659)]&.draw=surface&.vars=longitude|latitude|tasmin" +
            "&.colorBar=|||||&.land=under",
            tDir, "testAwsS3", ".png"); 
        SSR.displayInBrowser("file://" + tDir + tName);

    }

    /**
     * This tests writing .nccsvMetadata and .nccsv files with this class.
     *
     * @throws Throwable if trouble
     */
    public static void testNccsv() throws Throwable {
        String2.log("\n****************** EDDGridFromNcFiles.testNccsv() *****************\n");

        testVerboseOn();

        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDVGridAxis edvga;
        String id = "testGriddedNcFiles";
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, id); 
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
        String tDir = EDStatic.fullTestCacheDirectory;

        //*** test getting .nccsvMetadata for entire dataset
        String2.log("\n*** .nccsvMetadata for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_nccsvMeta", ".nccsvMetadata"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"*GLOBAL*,Conventions,\"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.1\"\n" +
"*GLOBAL*,acknowledgement,\"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\"\n" +
"*GLOBAL*,cdm_data_type,Grid\n" +
"*GLOBAL*,composite,true\n" +
"*GLOBAL*,contributor_name,\"Remote Sensing Systems, Inc\"\n" +
"*GLOBAL*,contributor_role,Source of level 2 data.\n" +
"*GLOBAL*,creator_email,erd.data@noaa.gov\n" +
"*GLOBAL*,creator_name,\"NOAA CoastWatch, West Coast Node\"\n" +
"*GLOBAL*,creator_url,https://coastwatch.pfeg.noaa.gov\n" +
"*GLOBAL*,date_created,\"2008-08-29\"\n" +
"*GLOBAL*,date_issued,\"2008-08-29\"\n" +
"*GLOBAL*,Easternmost_Easting,359.875d\n" +
"*GLOBAL*,geospatial_lat_max,89.875d\n" +
"*GLOBAL*,geospatial_lat_min,-89.875d\n" +
"*GLOBAL*,geospatial_lat_resolution,0.25d\n" +
"*GLOBAL*,geospatial_lat_units,degrees_north\n" +
"*GLOBAL*,geospatial_lon_max,359.875d\n" +
"*GLOBAL*,geospatial_lon_min,0.125d\n" +
"*GLOBAL*,geospatial_lon_resolution,0.25d\n" +
"*GLOBAL*,geospatial_lon_units,degrees_east\n" +
"*GLOBAL*,geospatial_vertical_max,0.0d\n" +
"*GLOBAL*,geospatial_vertical_min,0.0d\n" +
"*GLOBAL*,geospatial_vertical_positive,up\n" +
"*GLOBAL*,geospatial_vertical_units,m\n" +
"*GLOBAL*,history,\"Remote Sensing Systems, Inc\\n2008-08-29T00:31:43Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD\"\n" +
"*GLOBAL*,infoUrl,https://coastwatch.pfeg.noaa.gov/infog/QS_ux10_las.html\n" +
"*GLOBAL*,institution,\"NOAA CoastWatch, West Coast Node\"\n" +
"*GLOBAL*,keywords,Earth Science > Oceans > Ocean Winds > Surface Winds\n" +
"*GLOBAL*,keywords_vocabulary,GCMD Science Keywords\n" +
"*GLOBAL*,license,\"The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.\"\n" +
"*GLOBAL*,naming_authority,gov.noaa.pfeg.coastwatch\n" +
"*GLOBAL*,Northernmost_Northing,89.875d\n" +
"*GLOBAL*,origin,\"Remote Sensing Systems, Inc\"\n" +
"*GLOBAL*,processing_level,\"3\"\n" +
"*GLOBAL*,project,CoastWatch (https://coastwatch.noaa.gov/)\n" +
"*GLOBAL*,projection,geographic\n" +
"*GLOBAL*,projection_type,mapped\n" +
"*GLOBAL*,references,RSS Inc. Winds: http://www.remss.com/ .\n" +
"*GLOBAL*,satellite,QuikSCAT\n" +
"*GLOBAL*,sensor,SeaWinds\n" +
"*GLOBAL*,source,\"satellite observation: QuikSCAT, SeaWinds\"\n" +
"*GLOBAL*,sourceUrl,http://192.168.31.18/thredds/dodsC/satellite/QS/ux10/1day\n" +
"*GLOBAL*,Southernmost_Northing,-89.875d\n" +
"*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v70\n" +
"*GLOBAL*,summary,\"Remote Sensing Inc. distributes science quality wind velocity data from the SeaWinds instrument onboard NASA's QuikSCAT satellite.  SeaWinds is a microwave scatterometer designed to measure surface winds over the global ocean.  Wind velocity fields are provided in zonal, meriodonal, and modulus sets. The reference height for all wind velocities is 10 meters.\"\n" +
"*GLOBAL*,time_coverage_end,2008-01-10T12:00:00Z\n" +
"*GLOBAL*,time_coverage_start,2008-01-01T12:00:00Z\n" +
"*GLOBAL*,title,\"Wind, QuikSCAT, Global, Science Quality (1 Day Composite)\"\n" +
"*GLOBAL*,Westernmost_Easting,0.125d\n" +
"time,*DATA_TYPE*,String\n" +
"time,_CoordinateAxisType,Time\n" +
"time,actual_range,2008-01-01T12:00:00Z\\n2008-01-10T12:00:00Z\n" +
"time,axis,T\n" +
"time,fraction_digits,0i\n" +
"time,ioos_category,Time\n" +
"time,long_name,Centered Time\n" +
"time,standard_name,time\n" +
"time,time_origin,01-JAN-1970 00:00:00\n" +
"time,units,yyyy-MM-dd'T'HH:mm:ssZ\n" +
"altitude,*DATA_TYPE*,double\n" +
"altitude,_CoordinateAxisType,Height\n" +
"altitude,_CoordinateZisPositive,up\n" +
"altitude,actual_range,0.0d,0.0d\n" +
"altitude,axis,Z\n" +
"altitude,fraction_digits,0i\n" +
"altitude,ioos_category,Location\n" +
"altitude,long_name,Altitude\n" +
"altitude,positive,up\n" +
"altitude,standard_name,altitude\n" +
"altitude,units,m\n" +
"latitude,*DATA_TYPE*,double\n" +
"latitude,_CoordinateAxisType,Lat\n" +
"latitude,actual_range,-89.875d,89.875d\n" +
"latitude,axis,Y\n" +
"latitude,coordsys,geographic\n" +
"latitude,fraction_digits,2i\n" +
"latitude,ioos_category,Location\n" +
"latitude,long_name,Latitude\n" +
"latitude,point_spacing,even\n" +
"latitude,standard_name,latitude\n" +
"latitude,units,degrees_north\n" +
"longitude,*DATA_TYPE*,double\n" +
"longitude,_CoordinateAxisType,Lon\n" +
"longitude,actual_range,0.125d,359.875d\n" +
"longitude,axis,X\n" +
"longitude,coordsys,geographic\n" +
"longitude,fraction_digits,2i\n" +
"longitude,ioos_category,Location\n" +
"longitude,long_name,Longitude\n" +
"longitude,point_spacing,even\n" +
"longitude,standard_name,longitude\n" +
"longitude,units,degrees_east\n" +
"x_wind,*DATA_TYPE*,float\n" +
"x_wind,_FillValue,-9999999.0f\n" +
"x_wind,colorBarMaximum,15.0d\n" +
"x_wind,colorBarMinimum,-15.0d\n" +
"x_wind,coordsys,geographic\n" +
"x_wind,fraction_digits,1i\n" +
"x_wind,ioos_category,Wind\n" +
"x_wind,long_name,Zonal Wind\n" +
"x_wind,missing_value,-9999999.0f\n" +
"x_wind,standard_name,x_wind\n" +
"x_wind,units,m s-1\n" +
"y_wind,*DATA_TYPE*,float\n" +
"y_wind,_FillValue,-9999999.0f\n" +
"y_wind,colorBarMaximum,15.0d\n" +
"y_wind,colorBarMinimum,-15.0d\n" +
"y_wind,coordsys,geographic\n" +
"y_wind,fraction_digits,1i\n" +
"y_wind,ioos_category,Wind\n" +
"y_wind,long_name,Meridional Wind\n" +
"y_wind,missing_value,-9999999.0f\n" +
"y_wind,standard_name,y_wind\n" +
"y_wind,units,m s-1\n" +
"mod,*DATA_TYPE*,float\n" +
"mod,_FillValue,-9999999.0f\n" +
"mod,colorBarMaximum,18.0d\n" +
"mod,colorBarMinimum,0.0d\n" +
"mod,colorBarPalette,WhiteRedBlack\n" +
"mod,coordsys,geographic\n" +
"mod,fraction_digits,1i\n" +
"mod,ioos_category,Wind\n" +
"mod,long_name,Modulus of Wind\n" +
"mod,missing_value,-9999999.0f\n" +
"mod,units,m s-1\n" +
"\n" +
"*END_METADATA*\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        
        //.nccsv  just axis values
        String2.log("\n*** .nccsv just axis values\n");       
        userDapQuery = "latitude[0:100:700],longitude[0:10:100]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_nccsvAxis", ".nccsv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"*GLOBAL*,Conventions,\"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.1\"\n" +
"*GLOBAL*,acknowledgement,\"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\"\n" +
"*GLOBAL*,cdm_data_type,Grid\n" +
"*GLOBAL*,composite,true\n" +
"*GLOBAL*,contributor_name,\"Remote Sensing Systems, Inc\"\n" +
"*GLOBAL*,contributor_role,Source of level 2 data.\n" +
"*GLOBAL*,creator_email,erd.data@noaa.gov\n" +
"*GLOBAL*,creator_name,\"NOAA CoastWatch, West Coast Node\"\n" +
"*GLOBAL*,creator_url,https://coastwatch.pfeg.noaa.gov\n" +
"*GLOBAL*,date_created,\"2008-08-29\"\n" +
"*GLOBAL*,date_issued,\"2008-08-29\"\n" +
"*GLOBAL*,Easternmost_Easting,359.875d\n" +
"*GLOBAL*,geospatial_lat_max,89.875d\n" +
"*GLOBAL*,geospatial_lat_min,-89.875d\n" +
"*GLOBAL*,geospatial_lat_resolution,0.25d\n" +
"*GLOBAL*,geospatial_lat_units,degrees_north\n" +
"*GLOBAL*,geospatial_lon_max,359.875d\n" +
"*GLOBAL*,geospatial_lon_min,0.125d\n" +
"*GLOBAL*,geospatial_lon_resolution,0.25d\n" +
"*GLOBAL*,geospatial_lon_units,degrees_east\n" +
"*GLOBAL*,geospatial_vertical_max,0.0d\n" +
"*GLOBAL*,geospatial_vertical_min,0.0d\n" +
"*GLOBAL*,geospatial_vertical_positive,up\n" +
"*GLOBAL*,geospatial_vertical_units,m\n" +
"*GLOBAL*,history,\"Remote Sensing Systems, Inc\\n2008-08-29T00:31:43Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD\\n" + today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);

expected = 
//"T22:48:36Z http://192.168.31.18/thredds/dodsC/satellite/QS/ux10/1day\n2017-04-18T22:48:36Z
"http://localhost:8080/cwexperimental/griddap/testGriddedNcFiles.nccsv?latitude[0:100:700],longitude[0:10:100]\"\n" +
"*GLOBAL*,infoUrl,https://coastwatch.pfeg.noaa.gov/infog/QS_ux10_las.html\n" +
"*GLOBAL*,institution,\"NOAA CoastWatch, West Coast Node\"\n" +
"*GLOBAL*,keywords,Earth Science > Oceans > Ocean Winds > Surface Winds\n" +
"*GLOBAL*,keywords_vocabulary,GCMD Science Keywords\n" +
"*GLOBAL*,license,\"The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.\"\n" +
"*GLOBAL*,naming_authority,gov.noaa.pfeg.coastwatch\n" +
"*GLOBAL*,Northernmost_Northing,89.875d\n" +
"*GLOBAL*,origin,\"Remote Sensing Systems, Inc\"\n" +
"*GLOBAL*,processing_level,\"3\"\n" +
"*GLOBAL*,project,CoastWatch (https://coastwatch.noaa.gov/)\n" +
"*GLOBAL*,projection,geographic\n" +
"*GLOBAL*,projection_type,mapped\n" +
"*GLOBAL*,references,RSS Inc. Winds: http://www.remss.com/ .\n" +
"*GLOBAL*,satellite,QuikSCAT\n" +
"*GLOBAL*,sensor,SeaWinds\n" +
"*GLOBAL*,source,\"satellite observation: QuikSCAT, SeaWinds\"\n" +
"*GLOBAL*,sourceUrl,http://192.168.31.18/thredds/dodsC/satellite/QS/ux10/1day\n" +
"*GLOBAL*,Southernmost_Northing,-89.875d\n" +
"*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v70\n" +
"*GLOBAL*,summary,\"Remote Sensing Inc. distributes science quality wind velocity data from the SeaWinds instrument onboard NASA's QuikSCAT satellite.  SeaWinds is a microwave scatterometer designed to measure surface winds over the global ocean.  Wind velocity fields are provided in zonal, meriodonal, and modulus sets. The reference height for all wind velocities is 10 meters.\"\n" +
"*GLOBAL*,time_coverage_end,2008-01-10T12:00:00Z\n" +
"*GLOBAL*,time_coverage_start,2008-01-01T12:00:00Z\n" +
"*GLOBAL*,title,\"Wind, QuikSCAT, Global, Science Quality (1 Day Composite)\"\n" +
"*GLOBAL*,Westernmost_Easting,0.125d\n" +
"latitude,*DATA_TYPE*,double\n" +
"latitude,_CoordinateAxisType,Lat\n" +
"latitude,actual_range,-89.875d,85.125d\n" +
"latitude,axis,Y\n" +
"latitude,coordsys,geographic\n" +
"latitude,fraction_digits,2i\n" +
"latitude,ioos_category,Location\n" +
"latitude,long_name,Latitude\n" +
"latitude,point_spacing,even\n" +
"latitude,standard_name,latitude\n" +
"latitude,units,degrees_north\n" +
"longitude,*DATA_TYPE*,double\n" +
"longitude,_CoordinateAxisType,Lon\n" +
"longitude,actual_range,0.125d,25.125d\n" +
"longitude,axis,X\n" +
"longitude,coordsys,geographic\n" +
"longitude,fraction_digits,2i\n" +
"longitude,ioos_category,Location\n" +
"longitude,long_name,Longitude\n" +
"longitude,point_spacing,even\n" +
"longitude,standard_name,longitude\n" +
"longitude,units,degrees_east\n" +
"\n" +
"*END_METADATA*\n" +
"latitude,longitude\n" +
"-89.875,0.125\n" +
"-64.875,2.625\n" +
"-39.875,5.125\n" +
"-14.875,7.625\n" +
"10.125,10.125\n" +
"35.125,12.625\n" +
"60.125,15.125\n" +
"85.125,17.625\n" +
",20.125\n" +
",22.625\n" +
",25.125\n" +
"*END_DATA*\n";
        int tPo = results.indexOf(expected.substring(0, 70));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);

        
        //.nccsv  data values
        String2.log("\n*** .nccsv data values\n");       
        userDapQuery = "x_wind[0][0][0:100:400][0:10:30],y_wind[0][0][0:100:400][0:10:30]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_nccsvData", ".nccsv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"*GLOBAL*,Conventions,\"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.1\"\n" +
"*GLOBAL*,acknowledgement,\"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\"\n" +
"*GLOBAL*,cdm_data_type,Grid\n" +
"*GLOBAL*,composite,true\n" +
"*GLOBAL*,contributor_name,\"Remote Sensing Systems, Inc\"\n" +
"*GLOBAL*,contributor_role,Source of level 2 data.\n" +
"*GLOBAL*,creator_email,erd.data@noaa.gov\n" +
"*GLOBAL*,creator_name,\"NOAA CoastWatch, West Coast Node\"\n" +
"*GLOBAL*,creator_url,https://coastwatch.pfeg.noaa.gov\n" +
"*GLOBAL*,date_created,\"2008-08-29\"\n" +
"*GLOBAL*,date_issued,\"2008-08-29\"\n" +
"*GLOBAL*,Easternmost_Easting,359.875d\n" +
"*GLOBAL*,geospatial_lat_max,89.875d\n" +
"*GLOBAL*,geospatial_lat_min,-89.875d\n" +
"*GLOBAL*,geospatial_lat_resolution,0.25d\n" +
"*GLOBAL*,geospatial_lat_units,degrees_north\n" +
"*GLOBAL*,geospatial_lon_max,359.875d\n" +
"*GLOBAL*,geospatial_lon_min,0.125d\n" +
"*GLOBAL*,geospatial_lon_resolution,0.25d\n" +
"*GLOBAL*,geospatial_lon_units,degrees_east\n" +
"*GLOBAL*,geospatial_vertical_max,0.0d\n" +
"*GLOBAL*,geospatial_vertical_min,0.0d\n" +
"*GLOBAL*,geospatial_vertical_positive,up\n" +
"*GLOBAL*,geospatial_vertical_units,m\n" +
"*GLOBAL*,history,\"Remote Sensing Systems, Inc\\n2008-08-29T00:31:43Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD\\n" + today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);

expected = 
//"T23:00:11Z http://192.168.31.18/thredds/dodsC/satellite/QS/ux10/1day\n2017-04-18T23:00:11Z
"http://localhost:8080/cwexperimental/griddap/testGriddedNcFiles.nccsv?x_wind[0][0][0:100:400][0:10:30],y_wind[0][0][0:100:400][0:10:30]\"\n" +
"*GLOBAL*,infoUrl,https://coastwatch.pfeg.noaa.gov/infog/QS_ux10_las.html\n" +
"*GLOBAL*,institution,\"NOAA CoastWatch, West Coast Node\"\n" +
"*GLOBAL*,keywords,Earth Science > Oceans > Ocean Winds > Surface Winds\n" +
"*GLOBAL*,keywords_vocabulary,GCMD Science Keywords\n" +
"*GLOBAL*,license,\"The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.\"\n" +
"*GLOBAL*,naming_authority,gov.noaa.pfeg.coastwatch\n" +
"*GLOBAL*,Northernmost_Northing,89.875d\n" +
"*GLOBAL*,origin,\"Remote Sensing Systems, Inc\"\n" +
"*GLOBAL*,processing_level,\"3\"\n" +
"*GLOBAL*,project,CoastWatch (https://coastwatch.noaa.gov/)\n" +
"*GLOBAL*,projection,geographic\n" +
"*GLOBAL*,projection_type,mapped\n" +
"*GLOBAL*,references,RSS Inc. Winds: http://www.remss.com/ .\n" +
"*GLOBAL*,satellite,QuikSCAT\n" +
"*GLOBAL*,sensor,SeaWinds\n" +
"*GLOBAL*,source,\"satellite observation: QuikSCAT, SeaWinds\"\n" +
"*GLOBAL*,sourceUrl,http://192.168.31.18/thredds/dodsC/satellite/QS/ux10/1day\n" +
"*GLOBAL*,Southernmost_Northing,-89.875d\n" +
"*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v70\n" +
"*GLOBAL*,summary,\"Remote Sensing Inc. distributes science quality wind velocity data from the SeaWinds instrument onboard NASA's QuikSCAT satellite.  SeaWinds is a microwave scatterometer designed to measure surface winds over the global ocean.  Wind velocity fields are provided in zonal, meriodonal, and modulus sets. The reference height for all wind velocities is 10 meters.\"\n" +
"*GLOBAL*,time_coverage_end,2008-01-10T12:00:00Z\n" +
"*GLOBAL*,time_coverage_start,2008-01-01T12:00:00Z\n" +
"*GLOBAL*,title,\"Wind, QuikSCAT, Global, Science Quality (1 Day Composite)\"\n" +
"*GLOBAL*,Westernmost_Easting,0.125d\n" +
"time,*DATA_TYPE*,String\n" +
"time,_CoordinateAxisType,Time\n" +
"time,actual_range,2008-01-01T12:00:00Z\\n2008-01-01T12:00:00Z\n" +
"time,axis,T\n" +
"time,fraction_digits,0i\n" +
"time,ioos_category,Time\n" +
"time,long_name,Centered Time\n" +
"time,standard_name,time\n" +
"time,time_origin,01-JAN-1970 00:00:00\n" +
"time,units,yyyy-MM-dd'T'HH:mm:ssZ\n" +
"altitude,*DATA_TYPE*,double\n" +
"altitude,_CoordinateAxisType,Height\n" +
"altitude,_CoordinateZisPositive,up\n" +
"altitude,actual_range,0.0d,0.0d\n" +
"altitude,axis,Z\n" +
"altitude,fraction_digits,0i\n" +
"altitude,ioos_category,Location\n" +
"altitude,long_name,Altitude\n" +
"altitude,positive,up\n" +
"altitude,standard_name,altitude\n" +
"altitude,units,m\n" +
"latitude,*DATA_TYPE*,double\n" +
"latitude,_CoordinateAxisType,Lat\n" +
"latitude,actual_range,-89.875d,10.125d\n" +
"latitude,axis,Y\n" +
"latitude,coordsys,geographic\n" +
"latitude,fraction_digits,2i\n" +
"latitude,ioos_category,Location\n" +
"latitude,long_name,Latitude\n" +
"latitude,point_spacing,even\n" +
"latitude,standard_name,latitude\n" +
"latitude,units,degrees_north\n" +
"longitude,*DATA_TYPE*,double\n" +
"longitude,_CoordinateAxisType,Lon\n" +
"longitude,actual_range,0.125d,7.625d\n" +
"longitude,axis,X\n" +
"longitude,coordsys,geographic\n" +
"longitude,fraction_digits,2i\n" +
"longitude,ioos_category,Location\n" +
"longitude,long_name,Longitude\n" +
"longitude,point_spacing,even\n" +
"longitude,standard_name,longitude\n" +
"longitude,units,degrees_east\n" +
"x_wind,*DATA_TYPE*,float\n" +
"x_wind,_FillValue,-9999999.0f\n" +
"x_wind,colorBarMaximum,15.0d\n" +
"x_wind,colorBarMinimum,-15.0d\n" +
"x_wind,coordsys,geographic\n" +
"x_wind,fraction_digits,1i\n" +
"x_wind,ioos_category,Wind\n" +
"x_wind,long_name,Zonal Wind\n" +
"x_wind,missing_value,-9999999.0f\n" +
"x_wind,standard_name,x_wind\n" +
"x_wind,units,m s-1\n" +
"y_wind,*DATA_TYPE*,float\n" +
"y_wind,_FillValue,-9999999.0f\n" +
"y_wind,colorBarMaximum,15.0d\n" +
"y_wind,colorBarMinimum,-15.0d\n" +
"y_wind,coordsys,geographic\n" +
"y_wind,fraction_digits,1i\n" +
"y_wind,ioos_category,Wind\n" +
"y_wind,long_name,Meridional Wind\n" +
"y_wind,missing_value,-9999999.0f\n" +
"y_wind,standard_name,y_wind\n" +
"y_wind,units,m s-1\n" +
"\n" +
"*END_METADATA*\n" +
"time,altitude,latitude,longitude,x_wind,y_wind\n" +
"2008-01-01T12:00:00Z,0.0,-89.875,0.125,-9999999.0,-9999999.0\n" +
"2008-01-01T12:00:00Z,0.0,-89.875,2.625,-9999999.0,-9999999.0\n" +
"2008-01-01T12:00:00Z,0.0,-89.875,5.125,-9999999.0,-9999999.0\n" +
"2008-01-01T12:00:00Z,0.0,-89.875,7.625,-9999999.0,-9999999.0\n" +
"2008-01-01T12:00:00Z,0.0,-64.875,0.125,-9999999.0,-9999999.0\n" +
"2008-01-01T12:00:00Z,0.0,-64.875,2.625,-9999999.0,-9999999.0\n" +
"2008-01-01T12:00:00Z,0.0,-64.875,5.125,-3.06147,7.39104\n" +
"2008-01-01T12:00:00Z,0.0,-64.875,7.625,0.455063,5.78212\n" +
"2008-01-01T12:00:00Z,0.0,-39.875,0.125,-9999999.0,-9999999.0\n" +
"2008-01-01T12:00:00Z,0.0,-39.875,2.625,1.7765,2.89898\n" +
"2008-01-01T12:00:00Z,0.0,-39.875,5.125,3.68331,1.497664\n" +
"2008-01-01T12:00:00Z,0.0,-39.875,7.625,4.44164,3.2698202\n" +
"2008-01-01T12:00:00Z,0.0,-14.875,0.125,-2.73632,5.548705\n" +
"2008-01-01T12:00:00Z,0.0,-14.875,2.625,-3.71656,5.574005\n" +
"2008-01-01T12:00:00Z,0.0,-14.875,5.125,-4.201255,5.3461847\n" +
"2008-01-01T12:00:00Z,0.0,-14.875,7.625,-2.27968,4.8825197\n" +
"2008-01-01T12:00:00Z,0.0,10.125,0.125,-9999999.0,-9999999.0\n" +
"2008-01-01T12:00:00Z,0.0,10.125,2.625,-9999999.0,-9999999.0\n" +
"2008-01-01T12:00:00Z,0.0,10.125,5.125,-9999999.0,-9999999.0\n" +
"2008-01-01T12:00:00Z,0.0,10.125,7.625,-9999999.0,-9999999.0\n" +
"*END_DATA*\n";
        tPo = results.indexOf(expected.substring(0, 25));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);


    }

    /**
     * This tests reading NetCDF .nc files with this class.
     *
     * @throws Throwable if trouble
     */
    public static void testNc(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDGridFromNcFiles.testNc() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDVGridAxis edvga;
        String id = "testGriddedNcFiles";
        String dataDir = EDStatic.unitTestDataDir + "erdQSwind1day/";
        deleteCachedDatasetInfo(id);
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, id); 
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);

        //*** test getting das for entire dataset
        String2.log("\n*** .nc test das dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_Entire", ".das"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.1991888e+9, 1.1999664e+9;\n" +
"    String axis \"T\";\n" +
"    Int32 fraction_digits 0;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Centered Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    Float64 actual_range 0.0, 0.0;\n" +
"    String axis \"Z\";\n" +
"    Int32 fraction_digits 0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range -89.875, 89.875;\n" +
"    String axis \"Y\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 2;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String point_spacing \"even\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range 0.125, 359.875;\n" +
"    String axis \"X\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 2;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String point_spacing \"even\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  x_wind {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum -15.0;\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 1;\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Zonal Wind\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"x_wind\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  y_wind {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum -15.0;\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 1;\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Meridional Wind\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"y_wind\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  mod {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float64 colorBarMaximum 18.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String colorBarPalette \"WhiteRedBlack\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 1;\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Modulus of Wind\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String acknowledgement \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" + 
"    String cdm_data_type \"Grid\";\n" +
"    String composite \"true\";\n" +
"    String contributor_name \"Remote Sensing Systems, Inc\";\n" +
"    String contributor_role \"Source of level 2 data.\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String creator_email \"erd.data@noaa.gov\";\n" +
"    String creator_name \"NOAA CoastWatch, West Coast Node\";\n" +
"    String creator_url \"https://coastwatch.pfeg.noaa.gov\";\n" +
"    String date_created \"2008-08-29\";\n" +
"    String date_issued \"2008-08-29\";\n" +
"    Float64 Easternmost_Easting 359.875;\n" +
"    Float64 geospatial_lat_max 89.875;\n" +
"    Float64 geospatial_lat_min -89.875;\n" +
"    Float64 geospatial_lat_resolution 0.25;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 359.875;\n" +
"    Float64 geospatial_lon_min 0.125;\n" +
"    Float64 geospatial_lon_resolution 0.25;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    Float64 geospatial_vertical_max 0.0;\n" +
"    Float64 geospatial_vertical_min 0.0;\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"Remote Sensing Systems, Inc\n" +
"2008-08-29T00:31:43Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD\n" + 
today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);

//            + " https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/ux10/1day\n" +
//today + 

expected = " http://localhost:8080/cwexperimental/griddap/testGriddedNcFiles.das\";\n" +
"    String infoUrl \"https://coastwatch.pfeg.noaa.gov/infog/QS_ux10_las.html\";\n" +
"    String institution \"NOAA CoastWatch, West Coast Node\";\n" +
"    String keywords \"Earth Science > Oceans > Ocean Winds > Surface Winds\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.\";\n" +
"    String naming_authority \"gov.noaa.pfeg.coastwatch\";\n" +
"    Float64 Northernmost_Northing 89.875;\n" +
"    String origin \"Remote Sensing Systems, Inc\";\n" +
"    String processing_level \"3\";\n" +
"    String project \"CoastWatch (https://coastwatch.noaa.gov/)\";\n" +
"    String projection \"geographic\";\n" +
"    String projection_type \"mapped\";\n" +
"    String references \"RSS Inc. Winds: http://www.remss.com/ .\";\n" +
"    String satellite \"QuikSCAT\";\n" +
"    String sensor \"SeaWinds\";\n" +
"    String source \"satellite observation: QuikSCAT, SeaWinds\";\n" +
                     //numeric IP because these are files captured long ago
"    String sourceUrl \"http://192.168.31.18/thredds/dodsC/satellite/QS/ux10/1day\";\n" +
"    Float64 Southernmost_Northing -89.875;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String summary \"Remote Sensing Inc. distributes science quality wind velocity data from the SeaWinds instrument onboard NASA's QuikSCAT satellite.  SeaWinds is a microwave scatterometer designed to measure surface winds over the global ocean.  Wind velocity fields are provided in zonal, meriodonal, and modulus sets. The reference height for all wind velocities is 10 meters.\";\n" +
"    String time_coverage_end \"2008-01-10T12:00:00Z\";\n" +
"    String time_coverage_start \"2008-01-01T12:00:00Z\";\n" +
"    String title \"Wind, QuikSCAT, Global, Science Quality (1 Day Composite)\";\n" +
"    Float64 Westernmost_Easting 0.125;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_Entire", ".dds"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Float64 time[time = 10];\n" +
"  Float64 altitude[altitude = 1];\n" +
"  Float64 latitude[latitude = 720];\n" +
"  Float64 longitude[longitude = 1440];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 x_wind[time = 10][altitude = 1][latitude = 720][longitude = 1440];\n" +
"    MAPS:\n" +
"      Float64 time[time = 10];\n" +
"      Float64 altitude[altitude = 1];\n" +
"      Float64 latitude[latitude = 720];\n" +
"      Float64 longitude[longitude = 1440];\n" +
"  } x_wind;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 y_wind[time = 10][altitude = 1][latitude = 720][longitude = 1440];\n" +
"    MAPS:\n" +
"      Float64 time[time = 10];\n" +
"      Float64 altitude[altitude = 1];\n" +
"      Float64 latitude[latitude = 720];\n" +
"      Float64 longitude[longitude = 1440];\n" +
"  } y_wind;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 mod[time = 10][altitude = 1][latitude = 720][longitude = 1440];\n" +
"    MAPS:\n" +
"      Float64 time[time = 10];\n" +
"      Float64 altitude[altitude = 1];\n" +
"      Float64 latitude[latitude = 720];\n" +
"      Float64 longitude[longitude = 1440];\n" +
"  } mod;\n" +
"} testGriddedNcFiles;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);



        //.csv  with data from one file
        String2.log("\n*** .nc test read from one file\n");       
        userDapQuery = "y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_Data1", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
//verified with 
//https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.csv?y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]
"time,altitude,latitude,longitude,y_wind\n" +
"UTC,m,degrees_north,degrees_east,m s-1\n";

String csvExpected = 
"2008-01-10T12:00:00Z,0.0,36.625,230.125,3.555585\n" +
"2008-01-10T12:00:00Z,0.0,36.625,230.875,2.82175\n" +
"2008-01-10T12:00:00Z,0.0,36.625,231.625,4.539375\n" +
"2008-01-10T12:00:00Z,0.0,36.625,232.375,4.975015\n" +
"2008-01-10T12:00:00Z,0.0,36.625,233.125,5.643055\n" +
"2008-01-10T12:00:00Z,0.0,36.625,233.875,2.72394\n" +
"2008-01-10T12:00:00Z,0.0,36.625,234.625,1.39762\n" +
"2008-01-10T12:00:00Z,0.0,36.625,235.375,2.10711\n" +
"2008-01-10T12:00:00Z,0.0,36.625,236.125,3.019165\n" +
"2008-01-10T12:00:00Z,0.0,36.625,236.875,3.551915\n" +
"2008-01-10T12:00:00Z,0.0,36.625,237.625,NaN\n";          //test of NaN
        expected += csvExpected;

        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csvp  with data from one file
        String2.log("\n*** .nc test read from one file  .csvp\n");       
        userDapQuery = "y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_Data1", ".csvp"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
//verified with 
//https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.csvp?y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]
"time (UTC),altitude (m),latitude (degrees_north),longitude (degrees_east),y_wind (m s-1)\n" +
csvExpected;       
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv0  with data from one file
        String2.log("\n*** .nc test read from one file  .csv0\n");       
        userDapQuery = "y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_Data1", ".csv0"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = csvExpected;
//verified with 
//https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.csv0?y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv  with data from several files
        String2.log("\n*** .nc test read from several files\n");       
        userDapQuery = "y_wind[(1199448000):3:(1.1999664e9)][0][(36.5)][(230)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_Data1", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
//verified with 
//https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.csv?y_wind[(1.1991888e9):3:(1.1999664e9)][0][(36.5)][(230)]
"time,altitude,latitude,longitude,y_wind\n" +
"UTC,m,degrees_north,degrees_east,m s-1\n" +
"2008-01-04T12:00:00Z,0.0,36.625,230.125,-12.3\n" +
"2008-01-07T12:00:00Z,0.0,36.625,230.125,-5.974585\n" +
"2008-01-10T12:00:00Z,0.0,36.625,230.125,3.555585\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.tsv  with data from one file
        String2.log("\n*** .nc test read from one file\n");       
        userDapQuery = "y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_Data1", ".tsv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
//verified with 
//https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.tsv?y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]
"time\taltitude\tlatitude\tlongitude\ty_wind\n" +
"UTC\tm\tdegrees_north\tdegrees_east\tm s-1\n";

String tsvExpected = 
"2008-01-10T12:00:00Z\t0.0\t36.625\t230.125\t3.555585\n" +
"2008-01-10T12:00:00Z\t0.0\t36.625\t230.875\t2.82175\n" +
"2008-01-10T12:00:00Z\t0.0\t36.625\t231.625\t4.539375\n" +
"2008-01-10T12:00:00Z\t0.0\t36.625\t232.375\t4.975015\n" +
"2008-01-10T12:00:00Z\t0.0\t36.625\t233.125\t5.643055\n" +
"2008-01-10T12:00:00Z\t0.0\t36.625\t233.875\t2.72394\n" +
"2008-01-10T12:00:00Z\t0.0\t36.625\t234.625\t1.39762\n" +
"2008-01-10T12:00:00Z\t0.0\t36.625\t235.375\t2.10711\n" +
"2008-01-10T12:00:00Z\t0.0\t36.625\t236.125\t3.019165\n" +
"2008-01-10T12:00:00Z\t0.0\t36.625\t236.875\t3.551915\n" +
"2008-01-10T12:00:00Z\t0.0\t36.625\t237.625\tNaN\n";          //test of NaN
        expected += tsvExpected;

        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.tsvp  with data from one file
        String2.log("\n*** .nc test read from one file  .tsvp\n");       
        userDapQuery = "y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_Data1", ".tsvp"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
//verified with 
//https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.csvp?y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]
"time (UTC)\taltitude (m)\tlatitude (degrees_north)\tlongitude (degrees_east)\ty_wind (m s-1)\n" +
tsvExpected;       
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.tsv0  with data from one file
        String2.log("\n*** .nc test read from one file  .tsv0\n");       
        userDapQuery = "y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_Data1", ".tsv0"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = tsvExpected;
//verified with 
//https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.csv0?y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //  */
    }

    

    /**
     * For netcdfAll version 4.2 and below, this tests reading GRIB .grb files with this class.
     *
     * @throws Throwable if trouble
     */
    public static void testGrib_42(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDGridFromNcFiles.testGrib_42() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);

        //generateDatasetsXml
        
        //  /*
        String id = "testGribFiles_42";
        if (deleteCachedDatasetInfo) 
            deleteCachedDatasetInfo(id);
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, id); 

        //*** test getting das for entire dataset
        String2.log("\n*** .grb test das dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_GribEntire_42", ".das"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 3.471984e+8, 6.600528e+8;\n" +
"    String axis \"T\";\n" +
"    String GRIB2_significanceOfRTName \"Start of forecast\";\n" +
"    String GRIB_orgReferenceTime \"1981-01-01T12:00:00Z\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Forecast Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  height_above_ground {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    Float64 actual_range 10.0, 10.0;\n" +
"    String GRIB_level_type \"105\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Specified Height Level above Ground\";\n" +
"    String positive \"up\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range 88.75, -88.75;\n" +
"    String axis \"Y\";\n" +
"    String grid_spacing \"-2.5 degrees_north\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range 0.0, 356.25;\n" +
"    String axis \"X\";\n" +
"    String grid_spacing \"3.75 degrees_east\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  wind_speed {\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    Int32 GRIB_center_id 74;\n" +
"    Int32 GRIB_level_type 105;\n" +
"    Int32 GRIB_param_id 1, 74, 1, 32;\n" +
"    String GRIB_param_name \"Wind_speed\";\n" +
"    Int32 GRIB_param_number 32;\n" +
"    String GRIB_param_short_name \"VAR32\";\n" +
"    String GRIB_product_definition_type \"Initialized analysis product\";\n" +
"    Int32 GRIB_table_id 1;\n" +
"    String GRIB_VectorComponentFlag \"easterlyNortherlyRelative\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind_speed @ height_above_ground\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"wind_speed\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String _CoordinateModelRunDate \"1981-01-01T12:00:00Z\";\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String CF:feature_type \"GRID\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
//"    String creator_name \"UK Meteorological Office Bracknell (RSMC) subcenter = 0\";\n" +
"    Float64 Easternmost_Easting 356.25;\n" +
"    String file_format \"GRIB-1\";\n" +
//"    String Generating_Process_or_Model \"Unknown\";\n" +
"    Float64 geospatial_lat_max 88.75;\n" +
"    Float64 geospatial_lat_min -88.75;\n" +
"    Float64 geospatial_lat_resolution 2.5;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 356.25;\n" +
"    Float64 geospatial_lon_min 0.0;\n" +
"    Float64 geospatial_lon_resolution 3.75;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String history \"Direct read of GRIB-1 into NetCDF-Java 4 API\n" +
today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);

//+ " (local files)\n" +
//today + 

expected = 
" http://localhost:8080/cwexperimental/griddap/testGribFiles_42.das\";\n" +
"    String infoUrl \"http://www.nceas.ucsb.edu/scicomp/GISSeminar/UseCases/ExtractGRIBClimateWithR/ExtractGRIBClimateWithR.html\";\n" +
"    String institution \"UK Met RSMC\";\n" +
"    String keywords \"Atmosphere > Atmospheric Winds > Surface Winds\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +    
"    String location \"" + EDStatic.unitTestDataDir + "grib/HADCM3_A2_wind_1981-1990.grb\";\n" +
"    Float64 Northernmost_Northing 88.75;\n" +
"    String Originating_center \"U.K. Met Office - Exeter (RSMC) (74)\";\n" +
"    String Product_Type \"Initialized analysis product\";\n" +
"    String source \"Initialized analysis product\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing -88.75;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v55\";\n" +
"    String summary \"This is a test of EDDGridFromNcFiles with GRIB files.\";\n" +
"    String time_coverage_end \"1990-12-01T12:00:00Z\";\n" +
"    String time_coverage_start \"1981-01-01T12:00:00Z\";\n" +
"    String title \"Test of EDDGridFromNcFiles with GRIB files\";\n" +
"    Float64 Westernmost_Easting 0.0;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        tResults = results.substring(tPo, Math.min(results.length(), tPo +  expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_GribEntire_42", ".dds"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Float64 time[time = 120];\n" +
"  Float64 height_above_ground[height_above_ground = 1];\n" +
"  Float64 latitude[latitude = 72];\n" +
"  Float64 longitude[longitude = 96];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 wind_speed[time = 120][height_above_ground = 1][latitude = 72][longitude = 96];\n" +
"    MAPS:\n" +
"      Float64 time[time = 120];\n" +
"      Float64 height_above_ground[height_above_ground = 1];\n" +
"      Float64 latitude[latitude = 72];\n" +
"      Float64 longitude[longitude = 96];\n" +
"  } wind_speed;\n" +
"} testGribFiles_42;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv  with data from one file
        String2.log("\n*** .grb test read from one file\n");       
        userDapQuery = "wind_speed[(4e8):10:(5e8)][0][(36.5)][(200):5:(238)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_GribData1", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"time,height_above_ground,latitude,longitude,wind_speed\n" +
"UTC,m,degrees_north,degrees_east,m s-1\n" +
"1982-09-01T12:00:00Z,10.0,36.25,198.75,8.129883\n" +
"1982-09-01T12:00:00Z,10.0,36.25,217.5,5.25\n" +
"1982-09-01T12:00:00Z,10.0,36.25,236.25,3.1298828\n" +
"1983-07-01T12:00:00Z,10.0,36.25,198.75,5.379883\n" +
"1983-07-01T12:00:00Z,10.0,36.25,217.5,5.25\n" +
"1983-07-01T12:00:00Z,10.0,36.25,236.25,2.6298828\n" +
"1984-05-01T12:00:00Z,10.0,36.25,198.75,5.38\n" +
"1984-05-01T12:00:00Z,10.0,36.25,217.5,7.7501173\n" +
"1984-05-01T12:00:00Z,10.0,36.25,236.25,3.88\n" +
"1985-03-01T12:00:00Z,10.0,36.25,198.75,8.629883\n" +
"1985-03-01T12:00:00Z,10.0,36.25,217.5,9.0\n" +
"1985-03-01T12:00:00Z,10.0,36.25,236.25,3.25\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //  */
    }

    /**
     * For netcdfAll version 4.2 and below, this tests reading GRIB2 .grb2 files with this class.
     *
     * @throws Throwable if trouble
     */
    public static void testGrib2_42(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDGridFromNcFiles.testGrib2_42() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);

        String2.log(NcHelper.ncdump(String2.unitTestBigDataDir + 
            "geosgrib/multi_1.glo_30m.all.grb2", "-h"));

        //generateDatasetsXml
        String id = "testGrib2_42";
        if (deleteCachedDatasetInfo) 
            deleteCachedDatasetInfo(id);
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, id); 

        //*** test getting das for entire dataset
        String2.log("\n*** .grb2 test das dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_GribEntire_42", ".das"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.243836e+9, 1.244484e+9;\n" +
"    String axis \"T\";\n" +
//"    String GRIB2_significanceOfRTName \"Start of forecast\";\n" +
//"    String GRIB_orgReferenceTime \"2009-06-01T06:00:00Z\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Forecast Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range 90.0, -77.5;\n" +
"    String axis \"Y\";\n" +
"    String grid_spacing \"-0.5 degrees_north\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range 0.0, 359.5;\n" +
"    String axis \"X\";\n" +
"    String grid_spacing \"0.5 degrees_east\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  Direction_of_swell_waves {\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String GRIB_generating_process_type \"Forecast\";\n" +
"    Int32 GRIB_level_type 241;\n" +
"    String GRIB_level_type_name \"ordered_sequence_of_data\";\n" +
"    String GRIB_param_category \"Waves\";\n" +
"    String GRIB_param_discipline \"Oceanographic_products\";\n" +
"    Int32 GRIB_param_id 2, 10, 0, 7;\n" +
"    String GRIB_param_name \"Direction_of_swell_waves\";\n" +
"    Int32 GRIB_product_definition_template 0;\n" +
"    String GRIB_product_definition_template_desc \"Analysis/forecast at horizontal level/layer at a point in time\";\n" +
"    String GRIB_VectorComponentFlag \"easterlyNortherlyRelative\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Direction_of_swell_waves @ ordered_sequence_of_data\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_swell_wave_to_direction\";\n" +
"    String units \"degrees\";\n" +
"  }\n" +
"  Direction_of_wind_waves {\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String GRIB_generating_process_type \"Forecast\";\n" +
"    Int32 GRIB_level_type 1;\n" +
"    String GRIB_level_type_name \"surface\";\n" +
"    String GRIB_param_category \"Waves\";\n" +
"    String GRIB_param_discipline \"Oceanographic_products\";\n" +
"    Int32 GRIB_param_id 2, 10, 0, 4;\n" +
"    String GRIB_param_name \"Direction_of_wind_waves\";\n" +
"    Int32 GRIB_product_definition_template 0;\n" +
"    String GRIB_product_definition_template_desc \"Analysis/forecast at horizontal level/layer at a point in time\";\n" +
"    String GRIB_VectorComponentFlag \"easterlyNortherlyRelative\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Direction_of_wind_waves @ surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_wind_wave_to_direction\";\n" +
"    String units \"degrees\";\n" +
"  }\n" +
"  Mean_period_of_swell_waves {\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String GRIB_generating_process_type \"Forecast\";\n" +
"    Int32 GRIB_level_type 241;\n" +
"    String GRIB_level_type_name \"ordered_sequence_of_data\";\n" +
"    String GRIB_param_category \"Waves\";\n" +
"    String GRIB_param_discipline \"Oceanographic_products\";\n" +
"    Int32 GRIB_param_id 2, 10, 0, 9;\n" +
"    String GRIB_param_name \"Mean_period_of_swell_waves\";\n" +
"    Int32 GRIB_product_definition_template 0;\n" +
"    String GRIB_product_definition_template_desc \"Analysis/forecast at horizontal level/layer at a point in time\";\n" +
"    String GRIB_VectorComponentFlag \"easterlyNortherlyRelative\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Mean_period_of_swell_waves @ ordered_sequence_of_data\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_swell_wave_mean_period_from_variance_spectral_density_first_frequency_moment\";\n" +
"    String units \"s\";\n" +
"  }\n" +
"  Mean_period_of_wind_waves {\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String GRIB_generating_process_type \"Forecast\";\n" +
"    Int32 GRIB_level_type 1;\n" +
"    String GRIB_level_type_name \"surface\";\n" +
"    String GRIB_param_category \"Waves\";\n" +
"    String GRIB_param_discipline \"Oceanographic_products\";\n" +
"    Int32 GRIB_param_id 2, 10, 0, 6;\n" +
"    String GRIB_param_name \"Mean_period_of_wind_waves\";\n" +
"    Int32 GRIB_product_definition_template 0;\n" +
"    String GRIB_product_definition_template_desc \"Analysis/forecast at horizontal level/layer at a point in time\";\n" +
"    String GRIB_VectorComponentFlag \"easterlyNortherlyRelative\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Mean_period_of_wind_waves @ surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_wind_wave_mean_period_from_variance_spectral_density_first_frequency_moment\";\n" +
"    String units \"s\";\n" +
"  }\n" +
"  Primary_wave_direction {\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String GRIB_generating_process_type \"Forecast\";\n" +
"    Int32 GRIB_level_type 1;\n" +
"    String GRIB_level_type_name \"surface\";\n" +
"    String GRIB_param_category \"Waves\";\n" +
"    String GRIB_param_discipline \"Oceanographic_products\";\n" +
"    Int32 GRIB_param_id 2, 10, 0, 10;\n" +
"    String GRIB_param_name \"Primary_wave_direction\";\n" +
"    Int32 GRIB_product_definition_template 0;\n" +
"    String GRIB_product_definition_template_desc \"Analysis/forecast at horizontal level/layer at a point in time\";\n" +
"    String GRIB_VectorComponentFlag \"easterlyNortherlyRelative\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Primary_wave_direction @ surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_wave_to_direction\";\n" +
"    String units \"degrees\";\n" +
"  }\n" +
"  Primary_wave_mean_period {\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String GRIB_generating_process_type \"Forecast\";\n" +
"    Int32 GRIB_level_type 1;\n" +
"    String GRIB_level_type_name \"surface\";\n" +
"    String GRIB_param_category \"Waves\";\n" +
"    String GRIB_param_discipline \"Oceanographic_products\";\n" +
"    Int32 GRIB_param_id 2, 10, 0, 11;\n" +
"    String GRIB_param_name \"Primary_wave_mean_period\";\n" +
"    Int32 GRIB_product_definition_template 0;\n" +
"    String GRIB_product_definition_template_desc \"Analysis/forecast at horizontal level/layer at a point in time\";\n" +
"    String GRIB_VectorComponentFlag \"easterlyNortherlyRelative\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Primary_wave_mean_period @ surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_wave_mean_period_from_variance_spectral_density_first_frequency_moment\";\n" +
"    String units \"s\";\n" +
"  }\n" +
"  Significant_height_of_combined_wind_waves_and_swell {\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String GRIB_generating_process_type \"Forecast\";\n" +
"    Int32 GRIB_level_type 1;\n" +
"    String GRIB_level_type_name \"surface\";\n" +
"    String GRIB_param_category \"Waves\";\n" +
"    String GRIB_param_discipline \"Oceanographic_products\";\n" +
"    Int32 GRIB_param_id 2, 10, 0, 3;\n" +
"    String GRIB_param_name \"Significant_height_of_combined_wind_waves_and_swell\";\n" +
"    Int32 GRIB_product_definition_template 0;\n" +
"    String GRIB_product_definition_template_desc \"Analysis/forecast at horizontal level/layer at a point in time\";\n" +
"    String GRIB_VectorComponentFlag \"easterlyNortherlyRelative\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Significant_height_of_combined_wind_waves_and_swell @ surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_wave_significant_height\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  Significant_height_of_swell_waves {\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String GRIB_generating_process_type \"Forecast\";\n" +
"    Int32 GRIB_level_type 241;\n" +
"    String GRIB_level_type_name \"ordered_sequence_of_data\";\n" +
"    String GRIB_param_category \"Waves\";\n" +
"    String GRIB_param_discipline \"Oceanographic_products\";\n" +
"    Int32 GRIB_param_id 2, 10, 0, 8;\n" +
"    String GRIB_param_name \"Significant_height_of_swell_waves\";\n" +
"    Int32 GRIB_product_definition_template 0;\n" +
"    String GRIB_product_definition_template_desc \"Analysis/forecast at horizontal level/layer at a point in time\";\n" +
"    String GRIB_VectorComponentFlag \"easterlyNortherlyRelative\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Significant_height_of_swell_waves @ ordered_sequence_of_data\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_swell_wave_significant_height\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  Significant_height_of_wind_waves {\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String GRIB_generating_process_type \"Forecast\";\n" +
"    Int32 GRIB_level_type 1;\n" +
"    String GRIB_level_type_name \"surface\";\n" +
"    String GRIB_param_category \"Waves\";\n" +
"    String GRIB_param_discipline \"Oceanographic_products\";\n" +
"    Int32 GRIB_param_id 2, 10, 0, 5;\n" +
"    String GRIB_param_name \"Significant_height_of_wind_waves\";\n" +
"    Int32 GRIB_product_definition_template 0;\n" +
"    String GRIB_product_definition_template_desc \"Analysis/forecast at horizontal level/layer at a point in time\";\n" +
"    String GRIB_VectorComponentFlag \"easterlyNortherlyRelative\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Significant_height_of_wind_waves @ surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_wind_wave_significant_height\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  U_component_of_wind {\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String GRIB_generating_process_type \"Forecast\";\n" +
"    Int32 GRIB_level_type 1;\n" +
"    String GRIB_level_type_name \"surface\";\n" +
"    String GRIB_param_category \"Momentum\";\n" +
"    String GRIB_param_discipline \"Meteorological_products\";\n" +
"    Int32 GRIB_param_id 2, 0, 2, 2;\n" +
"    String GRIB_param_name \"U-component_of_wind\";\n" +
"    Int32 GRIB_product_definition_template 0;\n" +
"    String GRIB_product_definition_template_desc \"Analysis/forecast at horizontal level/layer at a point in time\";\n" +
"    String GRIB_VectorComponentFlag \"easterlyNortherlyRelative\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"U-component_of_wind @ surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"x_wind\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  V_component_of_wind {\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String GRIB_generating_process_type \"Forecast\";\n" +
"    Int32 GRIB_level_type 1;\n" +
"    String GRIB_level_type_name \"surface\";\n" +
"    String GRIB_param_category \"Momentum\";\n" +
"    String GRIB_param_discipline \"Meteorological_products\";\n" +
"    Int32 GRIB_param_id 2, 0, 2, 3;\n" +
"    String GRIB_param_name \"V-component_of_wind\";\n" +
"    Int32 GRIB_product_definition_template 0;\n" +
"    String GRIB_product_definition_template_desc \"Analysis/forecast at horizontal level/layer at a point in time\";\n" +
"    String GRIB_VectorComponentFlag \"easterlyNortherlyRelative\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"V-component_of_wind @ surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"y_wind\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  Wind_direction_from_which_blowing {\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String GRIB_generating_process_type \"Forecast\";\n" +
"    Int32 GRIB_level_type 1;\n" +
"    String GRIB_level_type_name \"surface\";\n" +
"    String GRIB_param_category \"Momentum\";\n" +
"    String GRIB_param_discipline \"Meteorological_products\";\n" +
"    Int32 GRIB_param_id 2, 0, 2, 0;\n" +
"    String GRIB_param_name \"Wind_direction\";\n" +
"    Int32 GRIB_product_definition_template 0;\n" +
"    String GRIB_product_definition_template_desc \"Analysis/forecast at horizontal level/layer at a point in time\";\n" +
"    String GRIB_VectorComponentFlag \"easterlyNortherlyRelative\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind_direction_from_which_blowing @ surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"wind_from_direction\";\n" +
"    String units \"degrees\";\n" +
"  }\n" +
"  Wind_speed {\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String GRIB_generating_process_type \"Forecast\";\n" +
"    Int32 GRIB_level_type 1;\n" +
"    String GRIB_level_type_name \"surface\";\n" +
"    String GRIB_param_category \"Momentum\";\n" +
"    String GRIB_param_discipline \"Meteorological_products\";\n" +
"    Int32 GRIB_param_id 2, 0, 2, 1;\n" +
"    String GRIB_param_name \"Wind_speed\";\n" +
"    Int32 GRIB_product_definition_template 0;\n" +
"    String GRIB_product_definition_template_desc \"Analysis/forecast at horizontal level/layer at a point in time\";\n" +
"    String GRIB_VectorComponentFlag \"easterlyNortherlyRelative\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind_speed @ surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"wind_speed\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String _CoordinateModelRunDate \"2009-06-01T06:00:00Z\";\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String CF:feature_type \"GRID\";\n" +  //Eeeek!
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    Float64 Easternmost_Easting 359.5;\n" +
"    String file_format \"GRIB-2\";\n" +
"    String Generating_Model \"Global Multi-Grid Wave Model\";\n" +
"    Float64 geospatial_lat_max 90.0;\n" +
"    Float64 geospatial_lat_min -77.5;\n" +
"    Float64 geospatial_lat_resolution 0.5;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 359.5;\n" +
"    Float64 geospatial_lon_min 0.0;\n" +
"    Float64 geospatial_lon_resolution 0.5;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String history \"Direct read of GRIB-2 into NetCDF-Java 4 API\n" +
today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);

//+ " (local files)\n" +
//today + 
expected= 
" http://localhost:8080/cwexperimental/griddap/testGrib2_42.das\";\n" +
"    String infoUrl \"???\";\n" +
"    String institution \"???\";\n" +
"    String keywords \"Atmosphere > Atmospheric Winds > Surface Winds,\n" +
"Oceans > Ocean Waves > Wave Height,\n" +
"Oceans > Ocean Waves > Wave Speed/Direction\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String location \"" + String2.unitTestBigDataDir + "geosgrib/multi_1.glo_30m.all.grb2\";\n" +
"    Float64 Northernmost_Northing 90.0;\n" +
"    String Originating_center \"US National Weather Service - NCEP(WMC) (7)\";\n" +
"    String Product_Status \"Operational products\";\n" +
"    String Product_Type \"Forecast products\";\n" +
"    String source \"Type: Forecast products Status: Operational products\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing -77.5;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v55\";\n" +
"    String summary \"???\";\n" +
"    String time_coverage_end \"2009-06-08T18:00:00Z\";\n" +
"    String time_coverage_start \"2009-06-01T06:00:00Z\";\n" +
"    String title \"Test of Grib2\";\n" +
"    Float64 Westernmost_Easting 0.0;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        tResults = results.substring(tPo, Math.min(results.length(), tPo +  expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_GribEntire_42", ".dds"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Float64 time[time = 61];\n" +
"  Float64 latitude[latitude = 336];\n" +
"  Float64 longitude[longitude = 720];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Direction_of_swell_waves[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float64 latitude[latitude = 336];\n" +
"      Float64 longitude[longitude = 720];\n" +
"  } Direction_of_swell_waves;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Direction_of_wind_waves[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float64 latitude[latitude = 336];\n" +
"      Float64 longitude[longitude = 720];\n" +
"  } Direction_of_wind_waves;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Mean_period_of_swell_waves[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float64 latitude[latitude = 336];\n" +
"      Float64 longitude[longitude = 720];\n" +
"  } Mean_period_of_swell_waves;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Mean_period_of_wind_waves[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float64 latitude[latitude = 336];\n" +
"      Float64 longitude[longitude = 720];\n" +
"  } Mean_period_of_wind_waves;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Primary_wave_direction[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float64 latitude[latitude = 336];\n" +
"      Float64 longitude[longitude = 720];\n" +
"  } Primary_wave_direction;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Primary_wave_mean_period[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float64 latitude[latitude = 336];\n" +
"      Float64 longitude[longitude = 720];\n" +
"  } Primary_wave_mean_period;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Significant_height_of_combined_wind_waves_and_swell[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float64 latitude[latitude = 336];\n" +
"      Float64 longitude[longitude = 720];\n" +
"  } Significant_height_of_combined_wind_waves_and_swell;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Significant_height_of_swell_waves[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float64 latitude[latitude = 336];\n" +
"      Float64 longitude[longitude = 720];\n" +
"  } Significant_height_of_swell_waves;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Significant_height_of_wind_waves[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float64 latitude[latitude = 336];\n" +
"      Float64 longitude[longitude = 720];\n" +
"  } Significant_height_of_wind_waves;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 U_component_of_wind[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float64 latitude[latitude = 336];\n" +
"      Float64 longitude[longitude = 720];\n" +
"  } U_component_of_wind;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 V_component_of_wind[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float64 latitude[latitude = 336];\n" +
"      Float64 longitude[longitude = 720];\n" +
"  } V_component_of_wind;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Wind_direction_from_which_blowing[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float64 latitude[latitude = 336];\n" +
"      Float64 longitude[longitude = 720];\n" +
"  } Wind_direction_from_which_blowing;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Wind_speed[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float64 latitude[latitude = 336];\n" +
"      Float64 longitude[longitude = 720];\n" +
"  } Wind_speed;\n" +
"} testGrib2_42;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv  with data from one file
        String2.log("\n*** .grb test read from one file\n");       
        userDapQuery = "Wind_speed[0][(30)][(200):5:(238)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_GribData1_42", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"time,latitude,longitude,Wind_speed\n" +
"UTC,degrees_north,degrees_east,m s-1\n" +
"2009-06-01T06:00:00Z,30.0,200.0,6.17\n" +
"2009-06-01T06:00:00Z,30.0,202.5,6.73\n" +
"2009-06-01T06:00:00Z,30.0,205.0,8.13\n" +
"2009-06-01T06:00:00Z,30.0,207.5,6.7\n" +
"2009-06-01T06:00:00Z,30.0,210.0,4.62\n" +
"2009-06-01T06:00:00Z,30.0,212.5,1.48\n" +
"2009-06-01T06:00:00Z,30.0,215.0,3.03\n" +
"2009-06-01T06:00:00Z,30.0,217.5,4.63\n" +
"2009-06-01T06:00:00Z,30.0,220.0,5.28\n" +
"2009-06-01T06:00:00Z,30.0,222.5,5.3\n" +
"2009-06-01T06:00:00Z,30.0,225.0,4.04\n" +
"2009-06-01T06:00:00Z,30.0,227.5,3.64\n" +
"2009-06-01T06:00:00Z,30.0,230.0,5.3\n" +
"2009-06-01T06:00:00Z,30.0,232.5,2.73\n" +
"2009-06-01T06:00:00Z,30.0,235.0,3.15\n" +
"2009-06-01T06:00:00Z,30.0,237.5,4.23\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //  */
    }

    /**
     * For netcdfAll version 4.3 and above, this tests reading GRIB .grb files with this class.
     *
     * @throws Throwable if trouble
     */
    public static void testGrib_43(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDGridFromNcFiles.testGrib_43() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);

        //generateDatasetsXml
        
        //  /*
        String id = "testGribFiles_43";
        if (deleteCachedDatasetInfo) 
            deleteCachedDatasetInfo(id);
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, id); 

        //*** test getting das for entire dataset
        String2.log("\n*** .grb test das dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_GribEntire_43", ".das"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = //2013-09-03 The details of the GRIB attributes change frequently!
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 3.471984e+8, 6.600528e+8;\n" +
"    String axis \"T\";\n" +
"    String calendar \"proleptic_gregorian\";\n" + //new in netcdf-java 4.6.4
"    String GRIB2_significanceOfRTName \"Start of forecast\";\n" +
"    String GRIB_orgReferenceTime \"1981-01-01T12:00:00Z\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Forecast Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  height_above_ground {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    Float32 actual_range 10.0, 10.0;\n" +
"    String datum \"ground\";\n" +
"    Int32 Grib_level_type 105;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Specified Height Level above Ground\";\n" +
"    String positive \"up\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range -88.74999, 88.75001;\n" + //a test of descending lat values
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 actual_range 0.0, 356.25;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  wind_speed {\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String coordinates \"reftime time height_above_ground lat lon \";\n" + //new in netcdf-java 4.6.4
"    String description \"Wind speed\";\n" +
"    Int32 Grib1_Center 74;\n" +
"    String Grib1_Interval_Name \"Unknown Time Range Indicator -1\";\n" +
"    Int32 Grib1_Interval_Type -1;\n" +
"    String Grib1_Level_Desc \"Specified height level above ground\";\n" +
"    Int32 Grib1_Level_Type 105;\n" +
"    Int32 Grib1_Parameter 32;\n" +
"    Int32 Grib1_Subcenter 0;\n" +
"    Int32 Grib1_TableVersion 1;\n" +
"    String Grib_Variable_Id \"VAR_74-0-1-32_L105\";\n" +
"    String grid_mapping \"LatLon_Projection\";\n" + //new in netcdf-java 4.6.4
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind speed @ Specified height level above ground\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"wind_speed\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String _CoordinateModelRunDate \"1981-01-01T12:00:00Z\";\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    Float64 Easternmost_Easting 356.25;\n" +
"    String file_format \"GRIB-1\";\n" +
"    Float64 geospatial_lat_max 88.75001;\n" +
"    Float64 geospatial_lat_min -88.74999;\n" +
"    Float64 geospatial_lat_resolution 2.5;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 356.25;\n" +
"    Float64 geospatial_lon_min 0.0;\n" +
"    Float64 geospatial_lon_resolution 3.75;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String GRIB_table_version \"0,1\";\n" + //new in netcdf-java 4.6.4   comma!
"    String history \"Read using CDM IOSP GribCollection v3\n" + //changed in netcdf-java 4.6.4
today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);

//+ " (local files)\n" +
//today + 
expected = 
"http://localhost:8080/cwexperimental/griddap/testGribFiles_43.das\";\n" +
"    String infoUrl \"http://www.nceas.ucsb.edu/scicomp/GISSeminar/UseCases/ExtractGRIBClimateWithR/ExtractGRIBClimateWithR.html\";\n" +
"    String institution \"UK Met RSMC\";\n" +
"    String keywords \"Earth Science > Atmosphere > Atmospheric Winds > Surface Winds\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String location \"" + EDStatic.unitTestDataDir + "grib/HADCM3_A2_wind_1981-1990.grb\";\n" +
"    Float64 Northernmost_Northing 88.75001;\n" +
"    String Originating_or_generating_Center \"UK Meteorological Office \u00ad Exeter (RSMC)\";\n" + //- is #173! I think this is a test.
"    String Originating_or_generating_Subcenter \"0\";\n" +
"    String Product_Type \"Initialized analysis product\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing -88.74999;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String summary \"This is a test of EDDGridFromNcFiles with GRIB files.\";\n" +
"    String time_coverage_end \"1990-12-01T12:00:00Z\";\n" +
"    String time_coverage_start \"1981-01-01T12:00:00Z\";\n" +
"    String title \"Test of EDDGridFromNcFiles with GRIB files\";\n" +
"    Float64 Westernmost_Easting 0.0;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_GribEntire_43", ".dds"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Float64 time[time = 120];\n" +
"  Float32 height_above_ground[height_above_ground = 1];\n" +
"  Float32 latitude[latitude = 72];\n" +
"  Float32 longitude[longitude = 96];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 wind_speed[time = 120][height_above_ground = 1][latitude = 72][longitude = 96];\n" +
"    MAPS:\n" +
"      Float64 time[time = 120];\n" +
"      Float32 height_above_ground[height_above_ground = 1];\n" +
"      Float32 latitude[latitude = 72];\n" +
"      Float32 longitude[longitude = 96];\n" +
"  } wind_speed;\n" +
"} testGribFiles_43;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv  with data from one file
        String2.log("\n*** .grb test read from one file\n");       
        userDapQuery = "wind_speed[(4e8):10:(5e8)][0][(36.5)][(200):5:(238)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_GribData1_43", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"time,height_above_ground,latitude,longitude,wind_speed\n" +
"UTC,m,degrees_north,degrees_east,m s-1\n" +
"1982-09-01T12:00:00Z,10.0,36.250008,198.75002,8.129883\n" +
"1982-09-01T12:00:00Z,10.0,36.250008,217.50002,5.25\n" +
"1982-09-01T12:00:00Z,10.0,36.250008,236.25002,3.1298828\n" +
"1983-07-01T12:00:00Z,10.0,36.250008,198.75002,5.379883\n" +
"1983-07-01T12:00:00Z,10.0,36.250008,217.50002,5.25\n" +
"1983-07-01T12:00:00Z,10.0,36.250008,236.25002,2.6298828\n" +
"1984-05-01T12:00:00Z,10.0,36.250008,198.75002,5.38\n" +
"1984-05-01T12:00:00Z,10.0,36.250008,217.50002,7.7501173\n" +
"1984-05-01T12:00:00Z,10.0,36.250008,236.25002,3.88\n" +
"1985-03-01T12:00:00Z,10.0,36.250008,198.75002,8.629883\n" +
"1985-03-01T12:00:00Z,10.0,36.250008,217.50002,9.0\n" +
"1985-03-01T12:00:00Z,10.0,36.250008,236.25002,3.25\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //  */
    }

    /**
     * For netcdfAll version 4.3 and above, this tests reading GRIB2 .grb2 files with this class.
     *
     * @throws Throwable if trouble
     */
    public static void testGrib2_43(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDGridFromNcFiles.testGrib2_43() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);

        //generateDatasetsXml
        //file dir is EDStatic.unitTestDataDir: /erddapTest/
        try {   
        String id = "testGrib2_43";
        if (deleteCachedDatasetInfo) 
            deleteCachedDatasetInfo(id);
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, id); 

        //*** test getting das for entire dataset
        String2.log("\n*** .grb2 test das dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_GribEntire_43", ".das"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.243836e+9, 1.244484e+9;\n" +
"    String axis \"T\";\n" +
"    String calendar \"proleptic_gregorian\";\n" + //new in netcdf-java 4.6.4
"    String ioos_category \"Time\";\n" +
"    String long_name \"Forecast Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range -77.5, 90.0;\n" + //a test of descending lat values
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 actual_range 0.0, 359.5;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  Direction_of_swell_waves_degree_true_ordered_sequence_of_data {\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String standard_name \"sea_surface_swell_wave_to_direction\";\n" +
"  }\n" +
"  Direction_of_wind_waves_degree_true_surface {\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String standard_name \"sea_surface_wind_wave_to_direction\";\n" +
"  }\n" +
"  Mean_period_of_swell_waves_ordered_sequence_of_data {\n" +
"    String abbreviation \"SWPER\";\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String coordinates \"reftime time ordered_sequence_of_data lat lon \";\n" + //new in netcdf-java 4.6.4
"    String Grib2_Generating_Process_Type \"Forecast\";\n" +
"    String Grib2_Level_Desc \"Ordered Sequence of Data\";\n" + //changed in netcdf-java 4.6.4; was Type before 4.6.11 (and all similar below)
"    Int32 Grib2_Level_Type 241;\n" +
"    Int32 Grib2_Parameter 10, 0, 9;\n" +
"    String Grib2_Parameter_Category \"Waves\";\n" +
"    String Grib2_Parameter_Discipline \"Oceanographic products\";\n" +
"    String Grib2_Parameter_Name \"Mean period of swell waves\";\n" +
"    String Grib2_Statistical_Process_Type \"UnknownStatType--1\";\n" + //new in netcdf-java 5.2
"    String Grib_Variable_Id \"VAR_10-0-9_L241\";\n" +
"    String grid_mapping \"LatLon_Projection\";\n" + //new in netcdf-java 4.6.4
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Mean period of swell waves @ Ordered Sequence of Data\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_swell_wave_mean_period_from_variance_spectral_density_first_frequency_moment\";\n" +
"    String units \"s\";\n" +
"  }\n" +
"  Mean_period_of_wind_waves_surface {\n" +
"    String abbreviation \"WVPER\";\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String coordinates \"reftime time lat lon \";\n" + //new in netcdf-java 4.6.4
"    String Grib2_Generating_Process_Type \"Forecast\";\n" +
"    String Grib2_Level_Desc \"Ground or water surface\";\n" + //changed in netcdf-java 4.6.4
"    Int32 Grib2_Level_Type 1;\n" +
"    Int32 Grib2_Parameter 10, 0, 6;\n" +
"    String Grib2_Parameter_Category \"Waves\";\n" +
"    String Grib2_Parameter_Discipline \"Oceanographic products\";\n" +
"    String Grib2_Parameter_Name \"Mean period of wind waves\";\n" +
"    String Grib2_Statistical_Process_Type \"UnknownStatType--1\";\n" + //new in netcdf-java 5.2
"    String Grib_Variable_Id \"VAR_10-0-6_L1\";\n" +
"    String grid_mapping \"LatLon_Projection\";\n" + //new in netcdf-java 4.6.4
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Mean period of wind waves @ Ground or water surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_wind_wave_mean_period_from_variance_spectral_density_first_frequency_moment\";\n" +
"    String units \"s\";\n" +
"  }\n" +
"  Primary_wave_direction_degree_true_surface {\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String standard_name \"sea_surface_wave_to_direction\";\n" +
"  }\n" +
"  Primary_wave_mean_period_surface {\n" +
"    String abbreviation \"PERPW\";\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String coordinates \"reftime time lat lon \";\n" + //new in netcdf-java 4.6.4
"    String Grib2_Generating_Process_Type \"Forecast\";\n" +
"    String Grib2_Level_Desc \"Ground or water surface\";\n" + //changed in netcdf-java 4.6.4
"    Int32 Grib2_Level_Type 1;\n" +
"    Int32 Grib2_Parameter 10, 0, 11;\n" +
"    String Grib2_Parameter_Category \"Waves\";\n" +
"    String Grib2_Parameter_Discipline \"Oceanographic products\";\n" +
"    String Grib2_Parameter_Name \"Primary wave mean period\";\n" +
"    String Grib2_Statistical_Process_Type \"UnknownStatType--1\";\n" + //new in netcdf-java 5.2
"    String Grib_Variable_Id \"VAR_10-0-11_L1\";\n" +
"    String grid_mapping \"LatLon_Projection\";\n" + //new in netcdf-java 4.6.4
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Primary wave mean period @ Ground or water surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_wave_mean_period_from_variance_spectral_density_first_frequency_moment\";\n" +
"    String units \"s\";\n" +
"  }\n" +
"  Significant_height_of_combined_wind_waves_and_swell_surface {\n" +
"    String abbreviation \"HTSGW\";\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String coordinates \"reftime time lat lon \";\n" + //new in netcdf-java 4.6.4
"    String Grib2_Generating_Process_Type \"Forecast\";\n" +
"    String Grib2_Level_Desc \"Ground or water surface\";\n" + //changed in netcdf-java 4.6.4
"    Int32 Grib2_Level_Type 1;\n" +
"    Int32 Grib2_Parameter 10, 0, 3;\n" +
"    String Grib2_Parameter_Category \"Waves\";\n" +
"    String Grib2_Parameter_Discipline \"Oceanographic products\";\n" +
"    String Grib2_Parameter_Name \"Significant height of combined wind waves and swell\";\n" +
"    String Grib2_Statistical_Process_Type \"UnknownStatType--1\";\n" + //new in netcdf-java 5.2
"    String Grib_Variable_Id \"VAR_10-0-3_L1\";\n" +
"    String grid_mapping \"LatLon_Projection\";\n" + //new in netcdf-java 4.6.4
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Significant height of combined wind waves and swell @ Ground or water surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_wave_significant_height\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  Significant_height_of_swell_waves_ordered_sequence_of_data {\n" +
"    String abbreviation \"SWELL\";\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String coordinates \"reftime time ordered_sequence_of_data lat lon \";\n" + //new in netcdf-java 4.6.4
"    String Grib2_Generating_Process_Type \"Forecast\";\n" +
"    String Grib2_Level_Desc \"Ordered Sequence of Data\";\n" + //changed in netcdf-java 4.6.4
"    Int32 Grib2_Level_Type 241;\n" +
"    Int32 Grib2_Parameter 10, 0, 8;\n" +
"    String Grib2_Parameter_Category \"Waves\";\n" +
"    String Grib2_Parameter_Discipline \"Oceanographic products\";\n" +
"    String Grib2_Parameter_Name \"Significant height of swell waves\";\n" +
"    String Grib2_Statistical_Process_Type \"UnknownStatType--1\";\n" + //new in netcdf-java 5.2
"    String Grib_Variable_Id \"VAR_10-0-8_L241\";\n" +
"    String grid_mapping \"LatLon_Projection\";\n" + //new in netcdf-java 4.6.4
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Significant height of swell waves @ Ordered Sequence of Data\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_swell_wave_significant_height\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  Significant_height_of_wind_waves_surface {\n" +
"    String abbreviation \"WVHGT\";\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String coordinates \"reftime time lat lon \";\n" + //new in netcdf-java 4.6.4
"    String Grib2_Generating_Process_Type \"Forecast\";\n" +
"    String Grib2_Level_Desc \"Ground or water surface\";\n" + //changed in netcdf-java 4.6.4
"    Int32 Grib2_Level_Type 1;\n" +
"    Int32 Grib2_Parameter 10, 0, 5;\n" +
"    String Grib2_Parameter_Category \"Waves\";\n" +
"    String Grib2_Parameter_Discipline \"Oceanographic products\";\n" +
"    String Grib2_Parameter_Name \"Significant height of wind waves\";\n" +
"    String Grib2_Statistical_Process_Type \"UnknownStatType--1\";\n" + //new in netcdf-java 5.2
"    String Grib_Variable_Id \"VAR_10-0-5_L1\";\n" +
"    String grid_mapping \"LatLon_Projection\";\n" + //new in netcdf-java 4.6.4
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Significant height of wind waves @ Ground or water surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_wind_wave_significant_height\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  U_component_of_wind {\n" +
"    String abbreviation \"UGRD\";\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String coordinates \"reftime time lat lon \";\n" + //new in netcdf-java 4.6.4
"    String Grib2_Generating_Process_Type \"Forecast\";\n" +
"    String Grib2_Level_Desc \"Ground or water surface\";\n" + //changed in netcdf-java 4.6.4
"    Int32 Grib2_Level_Type 1;\n" +
"    Int32 Grib2_Parameter 0, 2, 2;\n" +
"    String Grib2_Parameter_Category \"Momentum\";\n" +
"    String Grib2_Parameter_Discipline \"Meteorological products\";\n" +
"    String Grib2_Parameter_Name \"u-component of wind\";\n" +
"    String Grib2_Statistical_Process_Type \"UnknownStatType--1\";\n" + //new in netcdf-java 5.2
"    String Grib_Variable_Id \"VAR_0-2-2_L1\";\n" +
"    String grid_mapping \"LatLon_Projection\";\n" + //new in netcdf-java 4.6.4
"    String ioos_category \"Wind\";\n" +
"    String long_name \"u-component of wind @ Ground or water surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"x_wind\";\n" +
"    String units \"m/s\";\n" +
"  }\n" +
"  V_component_of_wind {\n" +
"    String abbreviation \"VGRD\";\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String coordinates \"reftime time lat lon \";\n" + //new in netcdf-java 4.6.4
"    String Grib2_Generating_Process_Type \"Forecast\";\n" +
"    String Grib2_Level_Desc \"Ground or water surface\";\n" + //changed in netcdf-java 4.6.4
"    Int32 Grib2_Level_Type 1;\n" +
"    Int32 Grib2_Parameter 0, 2, 3;\n" +
"    String Grib2_Parameter_Category \"Momentum\";\n" +
"    String Grib2_Parameter_Discipline \"Meteorological products\";\n" +
"    String Grib2_Parameter_Name \"v-component of wind\";\n" +
"    String Grib2_Statistical_Process_Type \"UnknownStatType--1\";\n" + //new in netcdf-java 5.2
"    String Grib_Variable_Id \"VAR_0-2-3_L1\";\n" +
"    String grid_mapping \"LatLon_Projection\";\n" + //new in netcdf-java 4.6.4
"    String ioos_category \"Wind\";\n" +
"    String long_name \"v-component of wind @ Ground or water surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"y_wind\";\n" +
"    String units \"m/s\";\n" +
"  }\n" +
"  Wind_direction_from_which_blowing_degree_true_surface {\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Wind\";\n" +
"    String standard_name \"wind_from_direction\";\n" +
"  }\n" +
"  Wind_speed_surface {\n" +
"    String abbreviation \"WIND\";\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String coordinates \"reftime time lat lon \";\n" + //new in netcdf-java 4.6.4
"    String Grib2_Generating_Process_Type \"Forecast\";\n" +
"    String Grib2_Level_Desc \"Ground or water surface\";\n" + //changed in netcdf-java 4.6.4
"    Int32 Grib2_Level_Type 1;\n" +
"    Int32 Grib2_Parameter 0, 2, 1;\n" +
"    String Grib2_Parameter_Category \"Momentum\";\n" +
"    String Grib2_Parameter_Discipline \"Meteorological products\";\n" +
"    String Grib2_Parameter_Name \"Wind speed\";\n" +
"    String Grib2_Statistical_Process_Type \"UnknownStatType--1\";\n" + //new in netcdf-java 5.2
"    String Grib_Variable_Id \"VAR_0-2-1_L1\";\n" +
"    String grid_mapping \"LatLon_Projection\";\n" + //new in netcdf-java 4.6.4
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind speed @ Ground or water surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"wind_speed\";\n" +
"    String units \"m/s\";\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String Analysis_or_forecast_generating_process_identifier_defined_by_originating_centre \"Global Multi-Grid Wave Model (Static Grids)\";\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    Float64 Easternmost_Easting 359.5;\n" +
"    String file_format \"GRIB-2\";\n" +
"    Float64 geospatial_lat_max 90.0;\n" +
"    Float64 geospatial_lat_min -77.5;\n" +
"    Float64 geospatial_lat_resolution 0.5;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 359.5;\n" +
"    Float64 geospatial_lon_min 0.0;\n" +
"    Float64 geospatial_lon_resolution 0.5;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String GRIB_table_version \"2,1\";\n" +
"    String GRIB_table_version_master_local \"2/1\";\n" +
"    String history \"Read using CDM IOSP GribCollection v3\n" + //changed in "netcdf-java 4.6.4"

today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);

//+ " (local files)\n" +
//today + 
expected= 
"http://localhost:8080/cwexperimental/griddap/testGrib2_43.das\";\n" +
"    String infoUrl \"???\";\n" +
"    String institution \"???\";\n" +
"    String keywords \"Earth Science > Atmosphere > Atmospheric Winds > Surface Winds, Earth Science > Oceans > Ocean Waves > Wave Height, Earth Science > Oceans > Ocean Waves > Wave Speed/Direction\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String location \"" + String2.unitTestBigDataDir + "geosgrib/multi_1.glo_30m.all.grb2\";\n" +
"    Float64 Northernmost_Northing 90.0;\n" +
"    String Originating_generating_Center \"US National Weather Service, National Centres for Environmental Prediction (NCEP)\";\n" +
"    String Originating_generating_Subcenter \"0\";\n" +
"    String Originating_or_generating_Center \"US National Weather Service, National Centres for Environmental Prediction (NCEP)\";\n" +
"    String Originating_or_generating_Subcenter \"0\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing -77.5;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String summary \"???\";\n" +
"    String time_coverage_end \"2009-06-08T18:00:00Z\";\n" +
"    String time_coverage_start \"2009-06-01T06:00:00Z\";\n" +
"    String title \"Test of Grib2\";\n" +
"    String Type_of_generating_process \"Forecast\";\n" +
"    Float64 Westernmost_Easting 0.0;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_GribEntire_43", ".dds"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Float64 time[time = 61];\n" +
"  Float32 latitude[latitude = 336];\n" +
"  Float32 longitude[longitude = 720];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Direction_of_swell_waves_degree_true_ordered_sequence_of_data[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float32 latitude[latitude = 336];\n" +
"      Float32 longitude[longitude = 720];\n" +
"  } Direction_of_swell_waves_degree_true_ordered_sequence_of_data;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Direction_of_wind_waves_degree_true_surface[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float32 latitude[latitude = 336];\n" +
"      Float32 longitude[longitude = 720];\n" +
"  } Direction_of_wind_waves_degree_true_surface;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Mean_period_of_swell_waves_ordered_sequence_of_data[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float32 latitude[latitude = 336];\n" +
"      Float32 longitude[longitude = 720];\n" +
"  } Mean_period_of_swell_waves_ordered_sequence_of_data;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Mean_period_of_wind_waves_surface[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float32 latitude[latitude = 336];\n" +
"      Float32 longitude[longitude = 720];\n" +
"  } Mean_period_of_wind_waves_surface;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Primary_wave_direction_degree_true_surface[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float32 latitude[latitude = 336];\n" +
"      Float32 longitude[longitude = 720];\n" +
"  } Primary_wave_direction_degree_true_surface;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Primary_wave_mean_period_surface[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float32 latitude[latitude = 336];\n" +
"      Float32 longitude[longitude = 720];\n" +
"  } Primary_wave_mean_period_surface;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Significant_height_of_combined_wind_waves_and_swell_surface[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float32 latitude[latitude = 336];\n" +
"      Float32 longitude[longitude = 720];\n" +
"  } Significant_height_of_combined_wind_waves_and_swell_surface;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Significant_height_of_swell_waves_ordered_sequence_of_data[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float32 latitude[latitude = 336];\n" +
"      Float32 longitude[longitude = 720];\n" +
"  } Significant_height_of_swell_waves_ordered_sequence_of_data;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Significant_height_of_wind_waves_surface[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float32 latitude[latitude = 336];\n" +
"      Float32 longitude[longitude = 720];\n" +
"  } Significant_height_of_wind_waves_surface;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 U_component_of_wind[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float32 latitude[latitude = 336];\n" +
"      Float32 longitude[longitude = 720];\n" +
"  } U_component_of_wind;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 V_component_of_wind[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float32 latitude[latitude = 336];\n" +
"      Float32 longitude[longitude = 720];\n" +
"  } V_component_of_wind;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Wind_direction_from_which_blowing_degree_true_surface[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float32 latitude[latitude = 336];\n" +
"      Float32 longitude[longitude = 720];\n" +
"  } Wind_direction_from_which_blowing_degree_true_surface;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Wind_speed_surface[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float32 latitude[latitude = 336];\n" +
"      Float32 longitude[longitude = 720];\n" +
"  } Wind_speed_surface;\n" +
"} testGrib2_43;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv  with data from one file
        String2.log("\n*** .grb test read from one file\n");       
        userDapQuery = "Wind_speed_surface[0][(30)][(200):5:(238)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_GribData1_43", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"time,latitude,longitude,Wind_speed_surface\n" +
"UTC,degrees_north,degrees_east,m/s\n" +
"2009-06-01T06:00:00Z,30.0,200.0,6.17\n" +
"2009-06-01T06:00:00Z,30.0,202.5,6.73\n" +
"2009-06-01T06:00:00Z,30.0,205.0,8.13\n" +
"2009-06-01T06:00:00Z,30.0,207.5,6.7\n" +
"2009-06-01T06:00:00Z,30.0,210.0,4.62\n" +
"2009-06-01T06:00:00Z,30.0,212.5,1.48\n" +
"2009-06-01T06:00:00Z,30.0,215.0,3.03\n" +
"2009-06-01T06:00:00Z,30.0,217.5,4.63\n" +
"2009-06-01T06:00:00Z,30.0,220.0,5.28\n" +
"2009-06-01T06:00:00Z,30.0,222.5,5.3\n" +
"2009-06-01T06:00:00Z,30.0,225.0,4.04\n" +
"2009-06-01T06:00:00Z,30.0,227.5,3.64\n" +
"2009-06-01T06:00:00Z,30.0,230.0,5.3\n" +
"2009-06-01T06:00:00Z,30.0,232.5,2.73\n" +
"2009-06-01T06:00:00Z,30.0,235.0,3.15\n" +
"2009-06-01T06:00:00Z,30.0,237.5,4.23\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //  */
        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) +
"\n2012-07-12 with change to Java 4.3.8, this doesn't pass because of\n" +
"spaces and parens in attribute names. John Caron says he will fix.\n" +
"2013-02-20 better but not all fixed.\n" +
"https://cfconventions.org/Data/cf-conventions/cf-conventions-1.8/cf-conventions.html#_naming_conventions\n" +
"says\n" +
"\"Variable, dimension and attribute names should begin with a letter and be\n" +
"composed of letters, digits, and underscores.\"\n" +
"but nothing has changed!\n" +
"[2015?] Now generatedDatasetsXml suggests setting original to null \n" +
"and adds a variant with a valid CF attribute name.\n"); 
        }
    }



    /**
     * This tests reading CoastWatch Mercator .hdf files with this class.
     * <br>This also tests dimension without corresponding coordinate axis variable.
     * <br>This file has lots of data variables; this just tests two of them.
     *
     * @throws Throwable if trouble
     */
    public static void testCwHdf(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testCwHdf()\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

        //generateDatasetsXml
        
        //  /*
        String id = "testCwHdf";
        if (deleteCachedDatasetInfo) 
            deleteCachedDatasetInfo(id);
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, id); 

        //*** test getting das for entire dataset
        String2.log("\n*** EDDGridFromNcFiles.testCwHdf test das dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_CwHdfEntire", ".das"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"Attributes {\n" +
"  rows {\n" +
"    Int32 actual_range 0, 1247;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Rows\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  cols {\n" +
"    Int16 actual_range 0, 1139;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Cols\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  avhrr_ch1 {\n" +
"    Float64 _FillValue -327.68;\n" +
"    Float64 add_offset_err 0.0;\n" +
"    Int32 calibrated_nt 0;\n" +
"    String coordsys \"Mercator\";\n" +
"    Int32 fraction_digits 2;\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"AVHRR Channel 1\";\n" +
"    Float64 missing_value -327.68;\n" +
"    Float64 scale_factor_err 0.0;\n" +
"    String standard_name \"isotropic_spectral_radiance_in_air\";\n" +
"    String units \"percent\";\n" +
"  }\n" +
"  sst {\n" +
"    Float64 _FillValue -327.68;\n" +
"    Float64 add_offset_err 0.0;\n" +
"    Int32 calibrated_nt 0;\n" +
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String coordsys \"Mercator\";\n" +
"    Int32 fraction_digits 2;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Sea Surface Temperature\";\n" +
"    Float64 missing_value -327.68;\n" +
"    Float64 scale_factor_err 0.0;\n" +
"    String sst_equation_day \"nonlinear split window\";\n" +
"    String sst_equation_night \"linear triple window modified\";\n" +
"    String standard_name \"sea_surface_temperature\";\n" +
"    String units \"celsius\";\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String _History \"Direct read of HDF4 file through CDM library\";\n" +
"    String autonav_performed \"true\";\n" +
"    Int32 autonav_quality 2;\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String history \"Direct read of HDF4 file through CDM library\n" +
today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);
        
//+ " (local files)\n" +
//today + 
    
expected =
" http://localhost:8080/cwexperimental/griddap/testCwHdf.das\";\n" +
"    String infoUrl \"???\";\n" +
"    String institution \"NOAA CoastWatch\";\n" +
"    String keywords \"Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String origin \"USDOC/NOAA/NESDIS CoastWatch\";\n" +
"    String pass_type \"day\";\n" +
"    String projection \"Mercator\";\n" +
"    String projection_type \"mapped\";\n" +
"    String satellite \"noaa-18\";\n" +
"    String sensor \"avhrr\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String summary \"???\";\n" +
"    String title \"Test of CoastWatch HDF files\";\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_CwHdfEntire", ".dds"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Int32 rows[rows = 1248];\n" +
"  Int16 cols[cols = 1140];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 avhrr_ch1[rows = 1248][cols = 1140];\n" +
"    MAPS:\n" +
"      Int32 rows[rows = 1248];\n" +
"      Int16 cols[cols = 1140];\n" +
"  } avhrr_ch1;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 sst[rows = 1248][cols = 1140];\n" +
"    MAPS:\n" +
"      Int32 rows[rows = 1248];\n" +
"      Int16 cols[cols = 1140];\n" +
"  } sst;\n" +
"} testCwHdf;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv  with data from one file
        String2.log("\n*** EDDGridFromNcFiles.testCwHdf test read from one file\n");       
        userDapQuery = "sst[600:2:606][500:503]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_CwHdfData1", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"rows,cols,sst\n" +
"count,count,celsius\n" +
"600,500,21.07\n" +
"600,501,20.96\n" +
"600,502,21.080000000000002\n" +
"600,503,20.93\n" +
"602,500,21.16\n" +
"602,501,21.150000000000002\n" +
"602,502,21.2\n" +
"602,503,20.95\n" +
"604,500,21.34\n" +
"604,501,21.13\n" +
"604,502,21.13\n" +
"604,503,21.25\n" +
"606,500,21.37\n" +
"606,501,21.11\n" +
"606,502,21.0\n" +
"606,503,21.02\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //  */
    }

    /** 
     * This test the speed of all types of responses.
     * This test is in this class because the source data is in a file, 
     * so it has reliable access speed.
     * This gets a pretty big chunk of data.
     *
     * @param firstTest 0..
     * @param lastTest (inclusive) Any number greater than last available is 
     *   interpreted as last available.
     */
    public static void testSpeed(int firstTest, int lastTest) throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testSpeed\n" +
            "THIS REQUIRES THE testGriddedNcFiles DATASET TO BE IN LOCALHOST ERDDAP!!!\n" +
            SgtUtil.isBufferedImageAccelerated() + "\n");
        //gc and sleep to give computer time to catch up from previous tests
        for (int i = 0; i < 4; i++) 
            Math2.gc(5000);
        boolean oReallyVerbose = reallyVerbose;
        reallyVerbose = false;
        String outName;
        //2017-10-13 I switched from getFile to curl
        //  The advantage is: curl will detect if outputstream isn't being closed.
        //2018-05-17 problems with curl, switched to SSR.downloadFile
        String baseRequest = "http://localhost:8080/cwexperimental/griddap/testGriddedNcFiles"; 
        String userDapQuery = "?" + SSR.minimalPercentEncode("y_wind[(2008-01-07T12:00:00Z)][0][][0:719]") + //719 avoids esriAsc cross lon=180
            "&.vec="; //avoid get cached response
        String baseOut = EDStatic.fullTestCacheDirectory + "EDDGridFromNcFilesTestSpeed";
        ArrayList al;
        int timeOutSeconds = 120;
        String extensions[] = new String[]{
            ".asc", ".csv", ".csvp", ".csv0", 
            ".das", ".dds", ".dods", ".esriAscii", 
            ".graph", ".html", ".htmlTable",   //.help not available at this level
            ".json", ".jsonlCSV", ".jsonlCSV1", ".jsonlKVP", ".mat", 
            ".nc", ".ncHeader", 
            ".nccsv", ".nccsvMetadata", ".ncoJson",
            ".odvTxt", ".timeGaps", 
            ".tsv", ".tsvp", ".tsv0", 
            ".xhtml", 
            ".geotif", ".kml", 
            ".smallPdf", ".pdf", ".largePdf", 
            ".smallPng", ".png", ".largePng", 
            ".transparentPng"};
        int expectedMs[] = new int[]  {  
            //2017-10-13 I added 200 ms with change from getFile to curl
            //2018-05-17 I adjusted (e.g., small files 200ms faster) with switch to SSR.downloadFile
            //now Lenovo was Java 1.8/M4700 //was Java 1.6 times            //was java 1.5 times
            550, 1759, 1635, 1544,        //734, 6391, 6312, ?            //1250, 9750, 9562, ?                                  
            //why is esriAscii so slow??? was ~9000 for a long time. Then jumped to 23330.
            15, 15, 663, 12392,           //15, 15, 109/156, 16875        //15, 15, 547, 18859
            40, 25, 477,                  //63, 47, 2032,                 //93, 31, ...,
            1843, 1568, 1568, 2203, 666,  //6422, ., ., 203,              //9621, ., ., 625,  
            173, 117,                     //234, 250,                     //500, 500, 
            3485, 16, 390,
            2446, 13,                     //9547, ?                       //13278, ?
            1411, 1411, 1411,             //6297, 6281, ?,                //8766, 8844, ?,
            2204,  //but really slow if hard drive is busy!   //8625,     //11469, 
            500, 20,                      //656, 110,         //687, 94,  //Java 1.7 was 390r until change to new netcdf-Java
            266, 976, 1178,               //860, 2859, 3438,              //2188, 4063, 3797,   //small varies greatly
            131, 209, 459,                //438, 468, 1063,               //438, 469, 1188,     //small varies greatly
            720};                         //1703                          //2359};
        int bytes[]    = new int[]   {
            5875592, 23734053, 23734063, 23733974, 
            6006, 303, 2085486, 4701074, 
            60787, 51428, 11980027, 
            31827797, 28198736, 28198830, 54118736, 2085800, 
            2090600, 5285, 
            25961828, 5244, 5877820,
            26929890, 58,
            23734053, 23734063, 23733974, 
            69372795, 
            523113, 3601, 
            478774, 2189656, 2904880, 
            30764, 76777, 277494,   //small png flips between 26906 and 30764
            335307};

        //warm up
        boolean tryToCompress = true;
        outName = baseOut + "Warmup.csvp.csv";
        SSR.downloadFile(baseRequest + ".csvp" + userDapQuery + Math2.random(1000), outName, tryToCompress);
        //was al = SSR.dosShell(baseRequest + ".csvp" + userDapQuery + Math2.random(1000) + 
        //    " -o " + outName, timeOutSeconds);
        //String2.log(String2.toNewlineString(al.toArray()));

        outName = baseOut + "Warmup.png.png";
        SSR.downloadFile(baseRequest + ".png" + userDapQuery + Math2.random(1000), outName, tryToCompress);
        //al = SSR.dosShell(baseRequest + ".png" + userDapQuery + Math2.random(1000) + 
        //    " -o " + outName, timeOutSeconds);

        outName = baseOut + "Warmup.pdf.pdf";
        SSR.downloadFile(baseRequest + ".pdf" + userDapQuery + Math2.random(1000), outName, tryToCompress);
        //al = SSR.dosShell(baseRequest + ".pdf" + userDapQuery + Math2.random(1000) + 
        //    " -o " + outName, timeOutSeconds);
       
        lastTest = Math.min(lastTest, extensions.length - 1);
        for (int ext = firstTest; ext <= lastTest; ext++) {
            //String2.pressEnterToContinue("");
            Math2.sleep(3000);
            String dotExt = extensions[ext];
            try {
                String2.log("\n*** EDDGridFromNcFiles.testSpeed test#" + ext + ": " + 
                    dotExt + " speed\n");
                long time = 0, cLength = 0;
                for (int chance = 0; chance < 3; chance++) {
                    Math2.gcAndWait(); //in a test
                    time = System.currentTimeMillis();
                    outName = baseOut + chance + dotExt;
                    SSR.downloadFile(baseRequest + dotExt + userDapQuery + Math2.random(1000), outName, tryToCompress);
                    //al = SSR.dosShell(baseRequest + dotExt + userDapQuery + Math2.random(1000) + 
                    //    " -o " + outName, timeOutSeconds);

                    time = System.currentTimeMillis() - time;
                    cLength = File2.length(outName);
                    String2.log("\n*** EDDGridFromNcFiles.testSpeed test#" + ext + 
                        " chance#" + chance + ": " + dotExt + " done.\n  " + 
                        cLength + " bytes (" + bytes[ext]+ 
                        ").  time=" + time + "ms (expected=" + expectedMs[ext] + ")\n");
                    Math2.sleep(3000);

                    //if not too slow or too fast, break
                    if (time > 1.5 * Math.max(50, expectedMs[ext]) ||
                        time < (expectedMs[ext] <= 50? 0 : 0.5) * expectedMs[ext]) {
                        //give it another chance
                    } else {
                        break;
                    }
                }

                //size test
                Test.ensureTrue(cLength > 0.9 * bytes[ext], 
                    "File shorter than expected.  observed=" + 
                    cLength + " expected=~" + bytes[ext] +
                    "\n" + outName);
                Test.ensureTrue(cLength < 1.1 * bytes[ext], 
                    "File longer than expected.  observed=" + 
                    cLength + " expected=~" + bytes[ext] +
                    "\n" + outName);

                //time test
                if (time > 1.5 * Math.max(50, expectedMs[ext]))
                    throw new SimpleException(
                        "Slower than expected. observed=" + time + 
                        " expected=~" + expectedMs[ext] + " ms.");
                if (expectedMs[ext] >= 50 && time < 0.5 * expectedMs[ext])
                    throw new SimpleException(
                        "Faster than expected! observed=" + time + 
                        " expected=~" + expectedMs[ext] + " ms.");

                //display last image
                if (ext == extensions.length - 1) {
                    File2.rename(outName, outName + ".png");
                    SSR.displayInBrowser( outName + ".png");
                }

            } catch (Exception e) {
                String2.pressEnterToContinue(MustBe.throwableToString(e) + 
                    "Unexpected error for Test#" + ext + ": " + dotExt + "."); 
            }
        }
        reallyVerbose = oReallyVerbose;
    }

    /** test reading an .hdf file */
    public static void testHdf() throws Throwable {

    }


    /** test reading an .ncml file */
    public static void testNcml() throws Throwable {

        String2.log("\n*** EDDGridFromNcFiles.testNcml");

        //2013-10-25 netcdf-java's reading of the source HDF4 file changed dramatically.
        //  Attributes that had internal spaces now have underscores.
        //  So the previous m4.ncml was making changes to atts that no longer existed
        //  and making changes that no longer need to be made.
        //  So I renamed old version as m4R20131025.ncml and revised m4.ncml.
        String2.log("\nOne of the source files that will be aggregated and modified:\n" + 
            NcHelper.ncdump(
            "/u00/data/viirs/MappedMonthly4km/V20120012012031.L3m_MO_NPP_CHL_chlor_a_4km", 
            "-h") + "\n");

        String results = Projects.dumpTimeLatLon("/u00/data/viirs/MappedMonthly4km/m4.ncml");
        String expected = 
"ncmlName=/u00/data/viirs/MappedMonthly4km/m4.ncml\n" +
"latitude [0]=89.97916666666667 [1]=89.9375 [4319]=-89.97916666666664\n" +
"longitude [0]=-179.97916666666666 [1]=-179.9375 [8639]=179.97916666666666\n" +
"time [0]=15340.0 [1]=15371.0 [1]=15371.0\n";
        Test.ensureEqual(results, expected, "results=\n" + results);
    }

    /**
     * This tests that ensureValid throws exception if an AxisVariable and 
     * a dataVariable use the same sourceName.
     *
     * @throws Throwable if trouble
     */
    public static void testAVDVSameSource() throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testAVDVSameSource()\n");
        String error = "shouldn't happen";
        try {
            EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, "testAVDVSameSource"); 
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
            error = String2.split(MustBe.throwableToString(t), '\n')[1]; 
        }

        Test.ensureEqual(error, 
            "Two variables have the same sourceName=OB_time.", 
            "Unexpected error message:\n" + error);
    }

    /**
     * This tests that ensureValid throws exception if 2  
     * dataVariables use the same sourceName.
     *
     * @throws Throwable if trouble
     */
    public static void test2DVSameSource() throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.test2DVSameSource()\n");
        String error = "shouldn't happen";
        try {
            EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, "test2DVSameSource"); 
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
            error = String2.split(MustBe.throwableToString(t), '\n')[0]; 
        }

        Test.ensureTrue(error.indexOf(
            "ERROR: Duplicate dataVariableSourceNames: [0] and [1] are both \"IB_time\".") >= 0, 
            "Unexpected error message:\n" + error);
    }

    /**
     * This tests that ensureValid throws exception if an AxisVariable and 
     * a dataVariable use the same destinationName.
     *
     * @throws Throwable if trouble
     */
    public static void testAVDVSameDestination() throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testAVDVSameDestination()\n");
        String error = "shouldn't happen";
        try {
            EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, "testAVDVSameDestination"); 
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
            error = String2.split(MustBe.throwableToString(t), '\n')[1]; 
        }

        Test.ensureTrue(error.indexOf( 
            "Two variables have the same destinationName=OB_time.") >= 0, 
            "Unexpected error message:\n" + error);
    }

    /**
     * This tests that ensureValid throws exception if 2  
     * dataVariables use the same destinationName.
     *
     * @throws Throwable if trouble
     */
    public static void test2DVSameDestination() throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.test2DVSameDestination()\n");
        String error = "shouldn't happen";
        try {
            EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, "test2DVSameDestination"); 
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
            error = String2.split(MustBe.throwableToString(t), '\n')[0]; 
        }

        Test.ensureTrue(error.indexOf(
            "ERROR: Duplicate dataVariableDestinationNames: [0] and [2] are both \"IB_time\".") >= 0, 
            "Unexpected error message:\n" + error);
    }

    /**
     * This tests sub-second time_precision in all output file types.
     *
     * @throws Throwable if trouble
     */
    public static void testTimePrecisionMillis() throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testTimePrecisionMillis()\n");
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, "testTimePrecisionMillis"); 
        String tDir = EDStatic.fullTestCacheDirectory;
        String aq = "[(1984-02-01T12:00:59.001Z):1:(1984-02-01T12:00:59.401Z)]";
        String userDapQuery = "ECEF_X" + aq + ",IB_time" + aq;
        String fName = "testTimePrecisionMillis";
        String tName, results, ts, expected;
        int po;

        //Yes. EDDGrid.parseAxisBrackets parses ISO 8601 times to millis precision.
        //I checked with this query and by turning on debug messages in parseAxisBrackets.

        //.asc  
        // !!!!! THIS IS ALSO THE ONLY TEST OF AN IMPORTANT BUG
        // In EDDGridFromFiles could cause 
        //  intermediate results to use data type from another variable,
        //   which could be lesser precision.
        //  (only occurred if vars had different precisions and not first var was requested,
        //  and if less precision mattered (e.g., double->float lost info))  
        // (SEE NOTES 2014-10-20)
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".asc"); 
        results = String2.directReadFrom88591File(tDir + tName);
        expected = 
"Dataset {\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 ECEF_X[time = 5];\n" +
"    MAPS:\n" +
"      Float64 time[time = 5];\n" +
"  } ECEF_X;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 IB_time[time = 5];\n" +
"    MAPS:\n" +
"      Float64 time[time = 5];\n" +
"  } IB_time;\n" +
"} testTimePrecisionMillis;\n" +
"---------------------------------------------\n" +
"ECEF_X.ECEF_X[5]\n" +
//missing first value is where outer dimension values would be, e.g., [0] (but none here)
", 9.96921E36, 9.96921E36, 9.96921E36, 9.96921E36, 9.96921E36\n" +
"\n" +
"ECEF_X.time[5]\n" +
"4.44484859001E8, 4.44484859101E8, 4.44484859201E8, 4.44484859301E8, 4.4448485940099996E8\n" +
"IB_time.IB_time[5]\n" +
//missing first value is where outer dimension values would be, e.g., [0] (but none here)
//THESE ARE THE VALUES THAT WERE AFFECTED BY THE BUG!!!
", 7.60017659E8, 7.600176591E8, 7.600176592E8, 7.600176593E8, 7.600176594E8\n" +
"\n" +
"IB_time.time[5]\n" +
"4.44484859001E8, 4.44484859101E8, 4.44484859201E8, 4.44484859301E8, 4.4448485940099996E8\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        expected = 
"time,ECEF_X,IB_time\n" +
"UTC,m,UTC\n" +
"1984-02-01T12:00:59.001Z,9.96921E36,1994-01-31T12:00:59.000Z\n" +
"1984-02-01T12:00:59.101Z,9.96921E36,1994-01-31T12:00:59.100Z\n" +
"1984-02-01T12:00:59.201Z,9.96921E36,1994-01-31T12:00:59.200Z\n" +
"1984-02-01T12:00:59.301Z,9.96921E36,1994-01-31T12:00:59.300Z\n" +
"1984-02-01T12:00:59.401Z,9.96921E36,1994-01-31T12:00:59.400Z\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.dods  hard to test

        //.htmlTable
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".htmlTable"); 
        results = String2.directReadFromUtf8File(tDir + tName);
        expected = 
"<table class=\"erd commonBGColor nowrap\">\n" +
"<tr>\n" +
"<th>time\n" +
"<th>ECEF_X\n" +
"<th>IB_time\n" +
"</tr>\n" +
"<tr>\n" +
"<th>UTC\n" +
"<th>m\n" +
"<th>UTC\n" +
"</tr>\n" +
"<tr>\n" +
"<td>1984-02-01T12:00:59.001Z\n" +
"<td class=\"R\">9.96921E36\n" +
"<td>1994-01-31T12:00:59.000Z\n" +
"</tr>\n" +
"<tr>\n" +
"<td>1984-02-01T12:00:59.101Z\n" +
"<td class=\"R\">9.96921E36\n" +
"<td>1994-01-31T12:00:59.100Z\n" +
"</tr>\n" +
"<tr>\n" +
"<td>1984-02-01T12:00:59.201Z\n" +
"<td class=\"R\">9.96921E36\n" +
"<td>1994-01-31T12:00:59.200Z\n" +
"</tr>\n" +
"<tr>\n" +
"<td>1984-02-01T12:00:59.301Z\n" +
"<td class=\"R\">9.96921E36\n" +
"<td>1994-01-31T12:00:59.300Z\n" +
"</tr>\n" +
"<tr>\n" +
"<td>1984-02-01T12:00:59.401Z\n" +
"<td class=\"R\">9.96921E36\n" +
"<td>1994-01-31T12:00:59.400Z\n" +
"</tr>\n" +
"</table>\n";
        po = results.indexOf("<table class=\"erd commonBGColor nowrap\">");
        ts = results.substring(Math.max(0, po), Math.min(results.length(), po + expected.length())); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

        //.json  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".json"); 
        results = String2.directReadFromUtf8File(tDir + tName);
        expected = 
"{\n" +
"  \"table\": {\n" +
"    \"columnNames\": [\"time\", \"ECEF_X\", \"IB_time\"],\n" +
"    \"columnTypes\": [\"String\", \"float\", \"String\"],\n" +
"    \"columnUnits\": [\"UTC\", \"m\", \"UTC\"],\n" +
"    \"rows\": [\n" +
"      [\"1984-02-01T12:00:59.001Z\", 9.96921E36, \"1994-01-31T12:00:59.000Z\"],\n" +
"      [\"1984-02-01T12:00:59.101Z\", 9.96921E36, \"1994-01-31T12:00:59.100Z\"],\n" +
"      [\"1984-02-01T12:00:59.201Z\", 9.96921E36, \"1994-01-31T12:00:59.200Z\"],\n" +
"      [\"1984-02-01T12:00:59.301Z\", 9.96921E36, \"1994-01-31T12:00:59.300Z\"],\n" +
"      [\"1984-02-01T12:00:59.401Z\", 9.96921E36, \"1994-01-31T12:00:59.400Z\"]\n" +
"    ]\n" +
"  }\n" +
"}\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.jsonlCSV1  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".jsonlCSV1"); 
        results = String2.directReadFromUtf8File(tDir + tName);
        expected = 
"[\"time\", \"ECEF_X\", \"IB_time\"]\n" +
"[\"1984-02-01T12:00:59.001Z\", 9.96921E36, \"1994-01-31T12:00:59.000Z\"]\n" +
"[\"1984-02-01T12:00:59.101Z\", 9.96921E36, \"1994-01-31T12:00:59.100Z\"]\n" +
"[\"1984-02-01T12:00:59.201Z\", 9.96921E36, \"1994-01-31T12:00:59.200Z\"]\n" +
"[\"1984-02-01T12:00:59.301Z\", 9.96921E36, \"1994-01-31T12:00:59.300Z\"]\n" +
"[\"1984-02-01T12:00:59.401Z\", 9.96921E36, \"1994-01-31T12:00:59.400Z\"]\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.jsonlCSV  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".jsonlCSV"); 
        results = String2.directReadFromUtf8File(tDir + tName);
        expected = 
"[\"1984-02-01T12:00:59.001Z\", 9.96921E36, \"1994-01-31T12:00:59.000Z\"]\n" +
"[\"1984-02-01T12:00:59.101Z\", 9.96921E36, \"1994-01-31T12:00:59.100Z\"]\n" +
"[\"1984-02-01T12:00:59.201Z\", 9.96921E36, \"1994-01-31T12:00:59.200Z\"]\n" +
"[\"1984-02-01T12:00:59.301Z\", 9.96921E36, \"1994-01-31T12:00:59.300Z\"]\n" +
"[\"1984-02-01T12:00:59.401Z\", 9.96921E36, \"1994-01-31T12:00:59.400Z\"]\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.jsonlKVP  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".jsonlKVP"); 
        results = String2.directReadFromUtf8File(tDir + tName);
        expected = 
"{\"time\":\"1984-02-01T12:00:59.001Z\", \"ECEF_X\":9.96921E36, \"IB_time\":\"1994-01-31T12:00:59.000Z\"}\n" +
"{\"time\":\"1984-02-01T12:00:59.101Z\", \"ECEF_X\":9.96921E36, \"IB_time\":\"1994-01-31T12:00:59.100Z\"}\n" +
"{\"time\":\"1984-02-01T12:00:59.201Z\", \"ECEF_X\":9.96921E36, \"IB_time\":\"1994-01-31T12:00:59.200Z\"}\n" +
"{\"time\":\"1984-02-01T12:00:59.301Z\", \"ECEF_X\":9.96921E36, \"IB_time\":\"1994-01-31T12:00:59.300Z\"}\n" +
"{\"time\":\"1984-02-01T12:00:59.401Z\", \"ECEF_X\":9.96921E36, \"IB_time\":\"1994-01-31T12:00:59.400Z\"}\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.mat  doesn't write strings
//!!! but need to test to ensure not rounding to the nearest second

        //.nc  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".nc"); 
        results = NcHelper.ncdump(tDir + tName, "");
        expected = 
":time_coverage_end = \"1984-02-01T12:00:59.401Z\";\n" + 
"  :time_coverage_start = \"1984-02-01T12:00:59.001Z\";\n" +  
"  :title = \"L1b Magnetometer (MAG) Geomagnetic Field Product\";\n" +
"\n" +
"  data:\n" +
"    time = \n" +
"      {4.44484859001E8, 4.44484859101E8, 4.44484859201E8, 4.44484859301E8, 4.4448485940099996E8}\n" +
"    ECEF_X = \n" +
"      {9.96921E36, 9.96921E36, 9.96921E36, 9.96921E36, 9.96921E36}\n" +
"    IB_time = \n" +
"      {7.60017659E8, 7.600176591E8, 7.600176592E8, 7.600176593E8, 7.600176594E8}\n" +  
"}\n";
        po = results.indexOf(":time_coverage_end");
        ts = results.substring(Math.max(0, po), Math.min(results.length(), po + expected.length())); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

        //.odvTxt
        /* can't test because it needs lon lat values
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".odvTxt"); 
        results = String2.directReadFrom88591File(tDir + tName);
        expected = 
"zztop\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
        */

        //.xhtml  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".xhtml"); 
        results = String2.directReadFromUtf8File(tDir + tName);
        expected = 
"<table class=\"erd commonBGColor nowrap\">\n" +
"<tr>\n" +
"<th>time</th>\n" +
"<th>ECEF_X</th>\n" +
"<th>IB_time</th>\n" +
"</tr>\n" +
"<tr>\n" +
"<th>UTC</th>\n" +
"<th>m</th>\n" +
"<th>UTC</th>\n" +
"</tr>\n" +
"<tr>\n" +
"<td>1984-02-01T12:00:59.001Z</td>\n" +
"<td class=\"R\">9.96921E36</td>\n" +
"<td>1994-01-31T12:00:59.000Z</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td>1984-02-01T12:00:59.101Z</td>\n" +
"<td class=\"R\">9.96921E36</td>\n" +
"<td>1994-01-31T12:00:59.100Z</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td>1984-02-01T12:00:59.201Z</td>\n" +
"<td class=\"R\">9.96921E36</td>\n" +
"<td>1994-01-31T12:00:59.200Z</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td>1984-02-01T12:00:59.301Z</td>\n" +
"<td class=\"R\">9.96921E36</td>\n" +
"<td>1994-01-31T12:00:59.300Z</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td>1984-02-01T12:00:59.401Z</td>\n" +
"<td class=\"R\">9.96921E36</td>\n" +
"<td>1994-01-31T12:00:59.400Z</td>\n" +
"</tr>\n" +
"</table>\n";
        po = results.indexOf("<table ");
        ts = results.substring(Math.max(0, po), Math.min(results.length(), po + expected.length())); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);


    }

    /**
     * This tests timestamps and other things.
     *
     * @throws Throwable if trouble
     */
    public static void testSimpleTestNc() throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testSimpleTestNc()\n");
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, "testSimpleTestNc"); 
        String tDir = EDStatic.fullTestCacheDirectory;
        String userDapQuery = "hours[1:2],minutes[1:2],seconds[1:2],millis[1:2],bytes[1:2]," +
            "shorts[1:2],ints[1:2],floats[1:2],doubles[1:2],Strings[1:2]";
        String fName = "testSimpleTestNc";
        String tName, results, ts, expected;
        int po;

        String2.log(NcHelper.ncdump(EDStatic.unitTestDataDir + "simpleTest.nc", ""));

        //all  
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", tDir, 
            fName, ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        expected = 
"days,hours,minutes,seconds,millis,bytes,shorts,ints,floats,doubles,Strings\n" +
"UTC,UTC,UTC,UTC,UTC,,,,,,\n" +
"1970-01-02T00:00:00Z,1980-01-01T05:00:00Z,1990-01-01T00:09:00Z,2000-01-01T00:00:20Z,2010-01-01T00:00:00.030Z,40,10000,1000000,0.0,1.0E12,0\n" +
"1970-01-03T00:00:00Z,1980-01-01T06:00:00Z,1990-01-01T00:10:00Z,2000-01-01T00:00:21Z,2010-01-01T00:00:00.031Z,41,10001,1000001,1.1,1.0000000000001E12,10\n" +
"1970-01-04T00:00:00Z,1980-01-01T07:00:00Z,1990-01-01T00:11:00Z,2000-01-01T00:00:22Z,2010-01-01T00:00:00.032Z,42,10002,1000002,2.2,1.0000000000002E12,20\n" +
"1970-01-05T00:00:00Z,1980-01-01T08:00:00Z,1990-01-01T00:12:00Z,2000-01-01T00:00:23Z,2010-01-01T00:00:00.033Z,43,10004,1000004,4.4,1.0000000000003E12,30\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.asc  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".asc"); 
        results = String2.directReadFrom88591File(tDir + tName);
        expected = 
"Dataset {\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 hours[days = 2];\n" +
"    MAPS:\n" +
"      Float64 days[days = 2];\n" +
"  } hours;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 minutes[days = 2];\n" +
"    MAPS:\n" +
"      Float64 days[days = 2];\n" +
"  } minutes;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 seconds[days = 2];\n" +
"    MAPS:\n" +
"      Float64 days[days = 2];\n" +
"  } seconds;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 millis[days = 2];\n" +
"    MAPS:\n" +
"      Float64 days[days = 2];\n" +
"  } millis;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Byte bytes[days = 2];\n" +
"    MAPS:\n" +
"      Float64 days[days = 2];\n" +
"  } bytes;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Int16 shorts[days = 2];\n" +
"    MAPS:\n" +
"      Float64 days[days = 2];\n" +
"  } shorts;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Int32 ints[days = 2];\n" +
"    MAPS:\n" +
"      Float64 days[days = 2];\n" +
"  } ints;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 floats[days = 2];\n" +
"    MAPS:\n" +
"      Float64 days[days = 2];\n" +
"  } floats;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 doubles[days = 2];\n" +
"    MAPS:\n" +
"      Float64 days[days = 2];\n" +
"  } doubles;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      String Strings[days = 2];\n" +
"    MAPS:\n" +
"      Float64 days[days = 2];\n" +
"  } Strings;\n" +
"} testSimpleTestNc;\n" +
"---------------------------------------------\n" +
"hours.hours[2]\n" +
", 3.155544E8, 3.15558E8\n" +
"\n" +
"hours.days[2]\n" +
"172800.0, 259200.0\n" +
"minutes.minutes[2]\n" +
", 6.311526E8, 6.3115266E8\n" +
"\n" +
"minutes.days[2]\n" +
"172800.0, 259200.0\n" +
"seconds.seconds[2]\n" +
", 9.46684821E8, 9.46684822E8\n" +
"\n" +
"seconds.days[2]\n" +
"172800.0, 259200.0\n" +
"millis.millis[2]\n" +
", 1.262304000031E9, 1.262304000032E9\n" +
"\n" +
"millis.days[2]\n" +
"172800.0, 259200.0\n" +
"bytes.bytes[2]\n" +
", 41, 42\n" +
"\n" +
"bytes.days[2]\n" +
"172800.0, 259200.0\n" +
"shorts.shorts[2]\n" +
", 10001, 10002\n" +
"\n" +
"shorts.days[2]\n" +
"172800.0, 259200.0\n" +
"ints.ints[2]\n" +
", 1000001, 1000002\n" +
"\n" +
"ints.days[2]\n" +
"172800.0, 259200.0\n" +
"floats.floats[2]\n" +
", 1.1, 2.2\n" +
"\n" +
"floats.days[2]\n" +
"172800.0, 259200.0\n" +
"doubles.doubles[2]\n" +
", 1.0000000000001E12, 1.0000000000002E12\n" +
"\n" +
"doubles.days[2]\n" +
"172800.0, 259200.0\n" +
"Strings.Strings[2]\n" +
", 10, 20\n" +
"\n" +
"Strings.days[2]\n" +
"172800.0, 259200.0\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        expected = 
"days,hours,minutes,seconds,millis,bytes,shorts,ints,floats,doubles,Strings\n" +
"UTC,UTC,UTC,UTC,UTC,,,,,,\n" +
"1970-01-03T00:00:00Z,1980-01-01T06:00:00Z,1990-01-01T00:10:00Z,2000-01-01T00:00:21Z,2010-01-01T00:00:00.031Z,41,10001,1000001,1.1,1.0000000000001E12,10\n" +
"1970-01-04T00:00:00Z,1980-01-01T07:00:00Z,1990-01-01T00:11:00Z,2000-01-01T00:00:22Z,2010-01-01T00:00:00.032Z,42,10002,1000002,2.2,1.0000000000002E12,20\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.dods  hard to test

        //.htmlTable
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".htmlTable"); 
        results = String2.directReadFromUtf8File(tDir + tName);
        expected = 
"<table class=\"erd commonBGColor nowrap\">\n" +
"<tr>\n" +
"<th>days\n" +
"<th>hours\n" +
"<th>minutes\n" +
"<th>seconds\n" +
"<th>millis\n" +
"<th>bytes\n" +
"<th>shorts\n" +
"<th>ints\n" +
"<th>floats\n" +
"<th>doubles\n" +
"<th>Strings\n" +
"</tr>\n" +
"<tr>\n" +
"<th>UTC\n" +
"<th>UTC\n" +
"<th>UTC\n" +
"<th>UTC\n" +
"<th>UTC\n" +
"<th>\n" +
"<th>\n" +
"<th>\n" +
"<th>\n" +
"<th>\n" +
"<th>\n" +
"</tr>\n" +
"<tr>\n" +
"<td>1970-01-03\n" +
"<td>1980-01-01T06Z\n" +
"<td>1990-01-01T00:10Z\n" +
"<td>2000-01-01T00:00:21Z\n" +
"<td>2010-01-01T00:00:00.031Z\n" +
"<td class=\"R\">41\n" +
"<td class=\"R\">10001\n" +
"<td class=\"R\">1000001\n" +
"<td class=\"R\">1.1\n" +
"<td class=\"R\">1.0000000000001E12\n" +
"<td>10\n" +
"</tr>\n" +
"<tr>\n" +
"<td>1970-01-04\n" +
"<td>1980-01-01T07Z\n" +
"<td>1990-01-01T00:11Z\n" +
"<td>2000-01-01T00:00:22Z\n" +
"<td>2010-01-01T00:00:00.032Z\n" +
"<td class=\"R\">42\n" +
"<td class=\"R\">10002\n" +
"<td class=\"R\">1000002\n" +
"<td class=\"R\">2.2\n" +
"<td class=\"R\">1.0000000000002E12\n" +
"<td>20\n" +
"</tr>\n" +
"</table>\n";
        po = results.indexOf("<table class=\"erd commonBGColor nowrap\">");
        ts = results.substring(Math.max(0, po), Math.min(results.length(), po + expected.length())); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

        //.json  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".json"); 
        results = String2.directReadFromUtf8File(tDir + tName);
        expected = 
"{\n" +
"  \"table\": {\n" +
"    \"columnNames\": [\"days\", \"hours\", \"minutes\", \"seconds\", \"millis\", \"bytes\", \"shorts\", \"ints\", \"floats\", \"doubles\", \"Strings\"],\n" +
"    \"columnTypes\": [\"String\", \"String\", \"String\", \"String\", \"String\", \"byte\", \"short\", \"int\", \"float\", \"double\", \"String\"],\n" +
"    \"columnUnits\": [\"UTC\", \"UTC\", \"UTC\", \"UTC\", \"UTC\", null, null, null, null, null, null],\n" +
"    \"rows\": [\n" +
"      [\"1970-01-03T00:00:00Z\", \"1980-01-01T06:00:00Z\", \"1990-01-01T00:10:00Z\", \"2000-01-01T00:00:21Z\", \"2010-01-01T00:00:00.031Z\", 41, 10001, 1000001, 1.1, 1.0000000000001E12, \"10\"],\n" +
"      [\"1970-01-04T00:00:00Z\", \"1980-01-01T07:00:00Z\", \"1990-01-01T00:11:00Z\", \"2000-01-01T00:00:22Z\", \"2010-01-01T00:00:00.032Z\", 42, 10002, 1000002, 2.2, 1.0000000000002E12, \"20\"]\n" +
"    ]\n" +
"  }\n" +
"}\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.mat  doesn't write strings
//!!! but need to test to ensure not rounding to the nearest second

        //.nc  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".nc"); 
        results = NcHelper.ncdump(tDir + tName, "");
        expected = 
"netcdf testSimpleTestNc.nc {\n" +
"  dimensions:\n" +
"    days = 2;\n" +
"    Strings_strlen = 2;\n" +
"  variables:\n" +
"    double days(days=2);\n" +
"      :actual_range = 172800.0, 259200.0; // double\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Days\";\n" +
"      :standard_name = \"time\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :time_precision = \"1970-01-01\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    double hours(days=2);\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Hours\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :time_precision = \"1970-01-01T00Z\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    double minutes(days=2);\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Minutes\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :time_precision = \"1970-01-01T00:00Z\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    double seconds(days=2);\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Seconds\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :time_precision = \"not valid\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    double millis(days=2);\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Millis\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :time_precision = \"1970-01-01T00:00:00.000Z\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    byte bytes(days=2);\n" +
"      :_FillValue = 127B; // byte\n" +       //important test
"      :ioos_category = \"Unknown\";\n" +
"\n" +
"    short shorts(days=2);\n" +
"      :_FillValue = 32767S; // short\n" +    //important test
"      :ioos_category = \"Unknown\";\n" +
"\n" +
"    int ints(days=2);\n" +
"      :_FillValue = 2147483647; // int\n" +  //important test
"      :ioos_category = \"Unknown\";\n" +
"\n" +
"    float floats(days=2);\n" +
"      :ioos_category = \"Unknown\";\n" +
"\n" +
"    double doubles(days=2);\n" +
"      :ioos_category = \"Unknown\";\n" +
"\n" +
"    char Strings(days=2, Strings_strlen=2);\n" +
"      :_Encoding = \"ISO-8859-1\";\n" +
"      :ioos_category = \"Unknown\";\n" +
"\n" +
"  // global attributes:\n" +
"  :cdm_data_type = \"Grid\";\n" +
"  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"  :history = \"";  //2014-10-22T16:16:21Z (local files)\n";
        ts = results.substring(0, expected.length()); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

expected = 
//"2014-10-22T16:16:21Z http://localhost:8080/cwexperimental
"/griddap/testSimpleTestNc.nc?hours[1:2],minutes[1:2],seconds[1:2],millis[1:2],bytes[1:2],shorts[1:2],ints[1:2],floats[1:2],doubles[1:2],Strings[1:2]\";\n" +
"  :id = \"simpleTest\";\n" +
"  :infoUrl = \"???\";\n" +
"  :institution = \"NOAA NMFS SWFSC ERD\";\n" +
"  :keywords = \"data, local, longs, source, strings\";\n" +
"  :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"  :sourceUrl = \"(local files)\";\n" +
"  :standard_name_vocabulary = \"CF Standard Name Table v70\";\n" +
"  :summary = \"My summary.\";\n" +
"  :title = \"My Title\";\n" +
"\n" +
"  data:\n" +
"    days = \n" +
"      {172800.0, 259200.0}\n" +
"    hours = \n" +
"      {3.155544E8, 3.15558E8}\n" +
"    minutes = \n" +
"      {6.311526E8, 6.3115266E8}\n" +
"    seconds = \n" +
"      {9.46684821E8, 9.46684822E8}\n" +
"    millis = \n" +
"      {1.262304000031E9, 1.262304000032E9}\n" +
"    bytes = \n" +
"      {41, 42}\n" +
"    shorts = \n" +
"      {10001, 10002}\n" +
"    ints = \n" +
"      {1000001, 1000002}\n" +
"    floats = \n" +
"      {1.1, 2.2}\n" +
"    doubles = \n" +
"      {1.0000000000001E12, 1.0000000000002E12}\n" +
"    Strings = \"10\", \"20\"\n" +
"}\n";
        po = results.indexOf("/griddap/testSimpleTestNc.nc?");
        ts = results.substring(Math.max(0, po), Math.min(results.length(), po + expected.length())); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

        //.odvTxt
        /* can't test because it needs lon lat values
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".odvTxt"); 
        results = String2.directReadFrom88591File(tDir + tName);
        expected = 
"zztop\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
        */

        //.xhtml  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".xhtml"); 
        results = String2.directReadFromUtf8File(tDir + tName);
        expected = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
"  \"https://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
"<html xmlns=\"https://www.w3.org/1999/xhtml\">\n" +
"<head>\n" +
"  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n" +
"  <title>testSimpleTestNc</title>\n" +
"  <link rel=\"stylesheet\" type=\"text/css\" href=\"http://localhost:8080/cwexperimental/images/erddap2.css\" />\n" +
"</head>\n" +
"<body>\n" +
"\n" +
"&nbsp;\n" +
"<table class=\"erd commonBGColor nowrap\">\n" +
"<tr>\n" +
"<th>days</th>\n" +
"<th>hours</th>\n" +
"<th>minutes</th>\n" +
"<th>seconds</th>\n" +
"<th>millis</th>\n" +
"<th>bytes</th>\n" +
"<th>shorts</th>\n" +
"<th>ints</th>\n" +
"<th>floats</th>\n" +
"<th>doubles</th>\n" +
"<th>Strings</th>\n" +
"</tr>\n" +
"<tr>\n" +
"<th>UTC</th>\n" +
"<th>UTC</th>\n" +
"<th>UTC</th>\n" +
"<th>UTC</th>\n" +
"<th>UTC</th>\n" +
"<th></th>\n" +
"<th></th>\n" +
"<th></th>\n" +
"<th></th>\n" +
"<th></th>\n" +
"<th></th>\n" +
"</tr>\n" +
"<tr>\n" +
"<td>1970-01-03T00:00:00Z</td>\n" +
"<td>1980-01-01T06:00:00Z</td>\n" +
"<td>1990-01-01T00:10:00Z</td>\n" +
"<td>2000-01-01T00:00:21Z</td>\n" +
"<td>2010-01-01T00:00:00.031Z</td>\n" +
"<td class=\"R\">41</td>\n" +
"<td class=\"R\">10001</td>\n" +
"<td class=\"R\">1000001</td>\n" +
"<td class=\"R\">1.1</td>\n" +
"<td class=\"R\">1.0000000000001E12</td>\n" +
"<td>10</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td>1970-01-04T00:00:00Z</td>\n" +
"<td>1980-01-01T07:00:00Z</td>\n" +
"<td>1990-01-01T00:11:00Z</td>\n" +
"<td>2000-01-01T00:00:22Z</td>\n" +
"<td>2010-01-01T00:00:00.032Z</td>\n" +
"<td class=\"R\">42</td>\n" +
"<td class=\"R\">10002</td>\n" +
"<td class=\"R\">1000002</td>\n" +
"<td class=\"R\">2.2</td>\n" +
"<td class=\"R\">1.0000000000002E12</td>\n" +
"<td>20</td>\n" +
"</tr>\n" +
"</table>\n" +
"</body>\n" +
"</html>\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //test time on x and y axis
        tName = eddGrid.makeNewFileForDapQuery(null, null, 
            "hours[(1970-01-02):(1970-01-05)]&.draw=linesAndMarkers" +
            "&.vars=days|hours|&.marker=5|5&.color=0x000000&.colorBar=|||||",
            tDir, fName,  ".png"); 
        SSR.displayInBrowser("file://" + tDir + tName);
        Test.knownProblem("SgtGraph DOESN'T SUPPORT TWO TIME AXES !!!!",
            "See SgtGraph \"yIsTimeAxis = false;\".");
        Math2.sleep(10000);       

    }

    /**
     * This tests timestamps and other things.
     *
     * @throws Throwable if trouble
     */
    public static void testSimpleTestNc2() throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testSimpleTestNc2()\n");
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, "testSimpleTestNc"); 
        String tDir = EDStatic.fullTestCacheDirectory;
        String userDapQuery = "bytes[2:3],doubles[2:3],Strings[2:3]";
        String fName = "testSimpleTestNc2";
        String tName, results, ts, expected;
        int po;

        String2.log(NcHelper.ncdump(EDStatic.unitTestDataDir + "simpleTest.nc", ""));

        //.asc  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".asc"); 
        results = String2.directReadFrom88591File(tDir + tName);
        expected = 
"Dataset {\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Byte bytes[days = 2];\n" +
"    MAPS:\n" +
"      Float64 days[days = 2];\n" +
"  } bytes;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 doubles[days = 2];\n" +
"    MAPS:\n" +
"      Float64 days[days = 2];\n" +
"  } doubles;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      String Strings[days = 2];\n" +
"    MAPS:\n" +
"      Float64 days[days = 2];\n" +
"  } Strings;\n" +
"} testSimpleTestNc;\n" +
"---------------------------------------------\n" +
"bytes.bytes[2]\n" +
", 42, 43\n" +
"\n" +
"bytes.days[2]\n" +
"259200.0, 345600.0\n" +
"doubles.doubles[2]\n" +
", 1.0000000000002E12, 1.0000000000003E12\n" +
"\n" +
"doubles.days[2]\n" +
"259200.0, 345600.0\n" +
"Strings.Strings[2]\n" +
", 20, 30\n" +
"\n" +
"Strings.days[2]\n" +
"259200.0, 345600.0\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        expected = 
"days,bytes,doubles,Strings\n" +
"UTC,,,\n" +
"1970-01-04T00:00:00Z,42,1.0000000000002E12,20\n" +
"1970-01-05T00:00:00Z,43,1.0000000000003E12,30\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.dods  doesn't write strings

        //.htmlTable
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".htmlTable"); 
        results = String2.directReadFromUtf8File(tDir + tName);
        expected = 
"<table class=\"erd commonBGColor nowrap\">\n" +
"<tr>\n" +
"<th>days\n" +
"<th>bytes\n" +
"<th>doubles\n" +
"<th>Strings\n" +
"</tr>\n" +
"<tr>\n" +
"<th>UTC\n" +
"<th>\n" +
"<th>\n" +
"<th>\n" +
"</tr>\n" +
"<tr>\n" +
"<td>1970-01-04\n" +
"<td class=\"R\">42\n" +
"<td class=\"R\">1.0000000000002E12\n" +
"<td>20\n" +
"</tr>\n" +
"<tr>\n" +
"<td>1970-01-05\n" +
"<td class=\"R\">43\n" +
"<td class=\"R\">1.0000000000003E12\n" +
"<td>30\n" +
"</tr>\n" +
"</table>\n";
        po = results.indexOf("<table class=\"erd commonBGColor nowrap\">");
        ts = results.substring(Math.max(0, po), Math.min(results.length(), po + expected.length())); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

        //.json  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".json"); 
        results = String2.directReadFromUtf8File(tDir + tName);
        expected = 
"{\n" +
"  \"table\": {\n" +
"    \"columnNames\": [\"days\", \"bytes\", \"doubles\", \"Strings\"],\n" +
"    \"columnTypes\": [\"String\", \"byte\", \"double\", \"String\"],\n" +
"    \"columnUnits\": [\"UTC\", null, null, null],\n" +
"    \"rows\": [\n" +
"      [\"1970-01-04T00:00:00Z\", 42, 1.0000000000002E12, \"20\"],\n" +
"      [\"1970-01-05T00:00:00Z\", 43, 1.0000000000003E12, \"30\"]\n" +
"    ]\n" +
"  }\n" +
"}\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.mat  doesn't write strings
//!!! but need to test to ensure not rounding to the nearest second

        //.nc  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".nc"); 
        results = NcHelper.ncdump(tDir + tName, "");
        expected = 
"netcdf testSimpleTestNc2.nc {\n" +
"  dimensions:\n" +
"    days = 2;\n" +
"    Strings_strlen = 2;\n" +
"  variables:\n" +
"    double days(days=2);\n" +
"      :actual_range = 259200.0, 345600.0; // double\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Days\";\n" +
"      :standard_name = \"time\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :time_precision = \"1970-01-01\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    byte bytes(days=2);\n" +
"      :_FillValue = 127B; // byte\n" +
"      :ioos_category = \"Unknown\";\n" +
"\n" +
"    double doubles(days=2);\n" +
"      :ioos_category = \"Unknown\";\n" +
"\n" +
"    char Strings(days=2, Strings_strlen=2);\n" +
"      :_Encoding = \"ISO-8859-1\";\n" +
"      :ioos_category = \"Unknown\";\n" +
"\n" +
"  // global attributes:\n" +
"  :cdm_data_type = \"Grid\";\n" +
"  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"  :history = \""; //2014-10-22T16:16:21Z (local files)\n";
        ts = results.substring(0, expected.length()); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

expected = 
//"2014-10-22T16:16:21Z http://localhost:8080/cwexperimental
"/griddap/testSimpleTestNc.nc?bytes[2:3],doubles[2:3],Strings[2:3]\";\n" +
"  :id = \"simpleTest\";\n" +
"  :infoUrl = \"???\";\n" +
"  :institution = \"NOAA NMFS SWFSC ERD\";\n" +
"  :keywords = \"data, local, longs, source, strings\";\n" +
"  :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"  :sourceUrl = \"(local files)\";\n" +
"  :standard_name_vocabulary = \"CF Standard Name Table v70\";\n" +
"  :summary = \"My summary.\";\n" +
"  :title = \"My Title\";\n" +
"\n" +
"  data:\n" +
"    days = \n" +
"      {259200.0, 345600.0}\n" +
"    bytes = \n" +
"      {42, 43}\n" +
"    doubles = \n" +
"      {1.0000000000002E12, 1.0000000000003E12}\n" +
"    Strings = \"20\", \"30\"\n" +
"}\n";
        po = results.indexOf("/griddap/testSimpleTestNc.nc?");
        ts = results.substring(Math.max(0, po), Math.min(results.length(), po + expected.length())); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

        //.odvTxt
        /* can't test because it needs lon lat values
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".odvTxt"); 
        results = String2.directReadFrom88591File(tDir + tName);
        expected = 
"zztop\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
        */

        //.xhtml  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".xhtml"); 
        results = String2.directReadFromUtf8File(tDir + tName);
        expected = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
"  \"https://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
"<html xmlns=\"https://www.w3.org/1999/xhtml\">\n" +
"<head>\n" +
"  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n" +
"  <title>testSimpleTestNc2</title>\n" +
"  <link rel=\"stylesheet\" type=\"text/css\" href=\"http://localhost:8080/cwexperimental/images/erddap2.css\" />\n" +
"</head>\n" +
"<body>\n" +
"\n" +
"&nbsp;\n" +
"<table class=\"erd commonBGColor nowrap\">\n" +
"<tr>\n" +
"<th>days</th>\n" +
"<th>bytes</th>\n" +
"<th>doubles</th>\n" +
"<th>Strings</th>\n" +
"</tr>\n" +
"<tr>\n" +
"<th>UTC</th>\n" +
"<th></th>\n" +
"<th></th>\n" +
"<th></th>\n" +
"</tr>\n" +
"<tr>\n" +
"<td>1970-01-04T00:00:00Z</td>\n" +
"<td class=\"R\">42</td>\n" +
"<td class=\"R\">1.0000000000002E12</td>\n" +
"<td>20</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td>1970-01-05T00:00:00Z</td>\n" +
"<td class=\"R\">43</td>\n" +
"<td class=\"R\">1.0000000000003E12</td>\n" +
"<td>30</td>\n" +
"</tr>\n" +
"</table>\n" +
"</body>\n" +
"</html>\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
    }

    /** This tests working with hdf file. 
     * @throws Throwable if touble
     */
    public static void testRTechHdf() throws Throwable {

        String2.log("\n*** EDDGridFromNcFiles.testRTechHdf");
        String dir = "c:/data/rtech/";
        String regex = "MT.*\\.hdf";
        String fileName = "MT1_L2-FLUX-SCASL1A2-1.06_2015-01-01T01-42-24_V1-00.hdf";
        String2.log(NcHelper.ncdump(dir + fileName, "-h"));

        String results = generateDatasetsXml(dir, regex, dir + fileName,
            "", //group
            "", DEFAULT_RELOAD_EVERY_N_MINUTES, null, null) + "\n"; //dimensionsCSV, reloadMinutes, cacheFromUrl
        String expected = 
"zztop\n\n";
        Test.ensureEqual(results, expected, 
            "results.length=" + results.length() + " expected.length=" + expected.length() + 
            "\nresults=\n" + results);

        //ensure it is ready-to-use by making a dataset from it
        String tDatasetID = "erdQSwind1day_52db_1ed3_22ce";
        EDD.deleteCachedDatasetInfo(tDatasetID);
        EDD edd = oneFromXmlFragment(null, results);
        Test.ensureEqual(edd.datasetID(), tDatasetID, "");
        Test.ensureEqual(edd.title(), "Wind, QuikSCAT, Global, Science Quality (1 Day Composite)", "");
        Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
            "x_wind, y_wind, mod", "");

        String2.log("\nEDDGridFromNcFiles.testGenerateDatasetsXml passed the test.");
    }

    /**
     * This tests the EDDGridFromFiles.update().
     *
     * @throws Throwable if trouble
     */
    public static void testUpdate() throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testUpdate()\n");
        EDDGridFromNcFiles eddGrid = (EDDGridFromNcFiles)oneFromDatasetsXml(null, "testGriddedNcFiles"); 
        String dataDir = eddGrid.fileDir;
        String tDir = EDStatic.fullTestCacheDirectory;
        String axisQuery = "time[]";
        String dataQuery = "x_wind[][][100][100]";
        String tName, results, expected;

        //*** read the original data
        String2.log("\n*** read original data\n");       
        tName = eddGrid.makeNewFileForDapQuery(null, null, axisQuery, tDir, 
            eddGrid.className() + "_update_1a", ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        String originalExpectedAxis = 
"time\n" +
"UTC\n" +
"2008-01-01T12:00:00Z\n" +
"2008-01-02T12:00:00Z\n" +
"2008-01-03T12:00:00Z\n" +
"2008-01-04T12:00:00Z\n" +
"2008-01-05T12:00:00Z\n" +
"2008-01-06T12:00:00Z\n" +
"2008-01-07T12:00:00Z\n" +
"2008-01-08T12:00:00Z\n" +
"2008-01-09T12:00:00Z\n" +
"2008-01-10T12:00:00Z\n";      
        Test.ensureEqual(results, originalExpectedAxis, "\nresults=\n" + results);

        //expected values
        String oldMinTime   = "2008-01-01T12:00:00Z";
        String oldMinMillis = "1.1991888E9";
        String newMinTime   = "2008-01-04T12:00:00Z";
        String newMinMillis = "1.199448E9";
        String oldMaxTime   = "2008-01-10T12:00:00Z";
        String oldMaxMillis = "1.1999664E9";

        tName = eddGrid.makeNewFileForDapQuery(null, null, dataQuery, tDir, 
            eddGrid.className() + "_update_1d", ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        String originalExpectedData = 
"time,altitude,latitude,longitude,x_wind\n" +
"UTC,m,degrees_north,degrees_east,m s-1\n" +
"2008-01-01T12:00:00Z,0.0,-64.875,25.125,-3.24724\n" +
"2008-01-02T12:00:00Z,0.0,-64.875,25.125,6.00287\n" +
"2008-01-03T12:00:00Z,0.0,-64.875,25.125,NaN\n" +
"2008-01-04T12:00:00Z,0.0,-64.875,25.125,-3.0695\n" +
"2008-01-05T12:00:00Z,0.0,-64.875,25.125,-7.76223\n" +
"2008-01-06T12:00:00Z,0.0,-64.875,25.125,-12.8834\n" +
"2008-01-07T12:00:00Z,0.0,-64.875,25.125,4.782275\n" +
"2008-01-08T12:00:00Z,0.0,-64.875,25.125,9.80197\n" +
"2008-01-09T12:00:00Z,0.0,-64.875,25.125,-7.5635605\n" +
"2008-01-10T12:00:00Z,0.0,-64.875,25.125,-7.974725\n";      
        Test.ensureEqual(results, originalExpectedData, "\nresults=\n" + results);

        Test.ensureEqual(eddGrid.axisVariables()[0].destinationMinString(), 
            oldMinTime, "av[0].destinationMin");
        Test.ensureEqual(eddGrid.axisVariables()[0].destinationMaxString(), 
            oldMaxTime, "av[0].destinationMax");
        Test.ensureEqual(eddGrid.axisVariables()[0].combinedAttributes().get("actual_range").toString(),
            oldMinMillis + ", " + oldMaxMillis, "actual_range");
        Test.ensureEqual(eddGrid.combinedGlobalAttributes().getString("time_coverage_start"),
            oldMinTime, "time_coverage_start");
        Test.ensureEqual(eddGrid.combinedGlobalAttributes().getString("time_coverage_end"),
            oldMaxTime, "time_coverage_end");

        //*** rename a data file so it doesn't match regex
        try {
            String2.log("\n*** rename a data file so it doesn't match regex\n");       
            File2.rename(dataDir, "erdQSwind1day_20080101_03.nc.gz", "erdQSwind1day_20080101_03.nc.gz2");
            for (int i = 0; i < 5; i++) {
                String2.log("after rename .nc.gz to .nc.gz2, update #" + i + " " + eddGrid.update());
                Math2.sleep(1000);
            }

            tName = eddGrid.makeNewFileForDapQuery(null, null, axisQuery, tDir, 
                eddGrid.className() + "_update_2a", ".csv"); 
            results = String2.directReadFrom88591File(tDir + tName);
            expected = 
"time\n" +
"UTC\n" +
"2008-01-04T12:00:00Z\n" +
"2008-01-05T12:00:00Z\n" +
"2008-01-06T12:00:00Z\n" +
"2008-01-07T12:00:00Z\n" +
"2008-01-08T12:00:00Z\n" +
"2008-01-09T12:00:00Z\n" +
"2008-01-10T12:00:00Z\n";      
            Test.ensureEqual(results, expected, "\nresults=\n" + results);

            tName = eddGrid.makeNewFileForDapQuery(null, null, dataQuery, tDir, 
                eddGrid.className() + "_update_2d", ".csv"); 
            results = String2.directReadFrom88591File(tDir + tName);
            expected = 
"time,altitude,latitude,longitude,x_wind\n" +
"UTC,m,degrees_north,degrees_east,m s-1\n" +
"2008-01-04T12:00:00Z,0.0,-64.875,25.125,-3.0695\n" +
"2008-01-05T12:00:00Z,0.0,-64.875,25.125,-7.76223\n" +
"2008-01-06T12:00:00Z,0.0,-64.875,25.125,-12.8834\n" +
"2008-01-07T12:00:00Z,0.0,-64.875,25.125,4.782275\n" +
"2008-01-08T12:00:00Z,0.0,-64.875,25.125,9.80197\n" +
"2008-01-09T12:00:00Z,0.0,-64.875,25.125,-7.5635605\n" +
"2008-01-10T12:00:00Z,0.0,-64.875,25.125,-7.974725\n";      
            Test.ensureEqual(results, expected, "\nresults=\n" + results);

            Test.ensureEqual(eddGrid.axisVariables()[0].destinationMinString(), 
                newMinTime, "av[0].destinationMin");
            Test.ensureEqual(eddGrid.axisVariables()[0].destinationMaxString(), 
                oldMaxTime, "av[0].destinationMax");
            Test.ensureEqual(eddGrid.axisVariables()[0].combinedAttributes().get("actual_range").toString(),
                newMinMillis + ", " + oldMaxMillis, "actual_range");
            Test.ensureEqual(eddGrid.combinedGlobalAttributes().getString("time_coverage_start"),
                newMinTime, "time_coverage_start");
            Test.ensureEqual(eddGrid.combinedGlobalAttributes().getString("time_coverage_end"),
                oldMaxTime, "time_coverage_end");

        } finally {
            //rename it back to original
            String2.log("\n*** rename it back to original\n");       
            File2.rename(dataDir, "erdQSwind1day_20080101_03.nc.gz2", "erdQSwind1day_20080101_03.nc.gz");
            for (int i = 0; i < 5; i++) {
                String2.log("after rename .nc2 to .nc, update #" + i + " " + eddGrid.update());
                Math2.sleep(1000);
            }
        }

        //*** back to original
        String2.log("\n*** after back to original\n");       
        tName = eddGrid.makeNewFileForDapQuery(null, null, axisQuery, tDir, 
            eddGrid.className() + "_update_3a", ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        Test.ensureEqual(results, originalExpectedAxis, "\nresults=\n" + results);

        tName = eddGrid.makeNewFileForDapQuery(null, null, dataQuery, tDir, 
            eddGrid.className() + "_update_3d", ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        Test.ensureEqual(results, originalExpectedData, "\nresults=\n" + results);

        Test.ensureEqual(eddGrid.axisVariables()[0].destinationMinString(), 
            oldMinTime, "av[0].destinationMin");
        Test.ensureEqual(eddGrid.axisVariables()[0].destinationMaxString(), 
            oldMaxTime, "av[0].destinationMax");
        Test.ensureEqual(eddGrid.axisVariables()[0].combinedAttributes().get("actual_range").toString(),
            oldMinMillis + ", " + oldMaxMillis, "actual_range");
        Test.ensureEqual(eddGrid.combinedGlobalAttributes().getString("time_coverage_start"),
            oldMinTime, "time_coverage_start");
        Test.ensureEqual(eddGrid.combinedGlobalAttributes().getString("time_coverage_end"),
            oldMaxTime, "time_coverage_end");

        //*** rename a non-data file so it matches the regex
        try {
            String2.log("\n*** rename an invalid file to be a valid name\n");       
            try {
                //change the invalid file's name so it will be noticed
                if ( File2.isFile(dataDir + "bad.notnc") &&
                    !File2.isFile(dataDir + "bad.nc"))
                File2.rename(dataDir, "bad.notnc", "bad.nc");
            } catch (Exception e) {
                //rename will fail if file already in place
            }
            for (int i = 0; i < 5; i++) {
                String2.log("after rename .notnc to .nc, update #" + i + " " + eddGrid.update());
                Math2.sleep(1000);
            }

            tName = eddGrid.makeNewFileForDapQuery(null, null, axisQuery, tDir, 
                eddGrid.className() + "_update_4a", ".csv"); 
            results = String2.directReadFrom88591File(tDir + tName);
            Test.ensureEqual(results, originalExpectedAxis, "\nresults=\n" + results);

            tName = eddGrid.makeNewFileForDapQuery(null, null, dataQuery, tDir, 
                eddGrid.className() + "_update_4d", ".csv"); 
            results = String2.directReadFrom88591File(tDir + tName);
            Test.ensureEqual(results, originalExpectedData, "\nresults=\n" + results);

            Test.ensureEqual(eddGrid.axisVariables()[0].destinationMinString(), 
                oldMinTime, "av[0].destinationMin");
            Test.ensureEqual(eddGrid.axisVariables()[0].destinationMaxString(), 
                oldMaxTime, "av[0].destinationMax");
            Test.ensureEqual(eddGrid.axisVariables()[0].combinedAttributes().get("actual_range").toString(),
                oldMinMillis + ", " + oldMaxMillis, "actual_range");
            Test.ensureEqual(eddGrid.combinedGlobalAttributes().getString("time_coverage_start"),
                oldMinTime, "time_coverage_start");
            Test.ensureEqual(eddGrid.combinedGlobalAttributes().getString("time_coverage_end"),
                oldMaxTime, "time_coverage_end");
        } catch (Exception e) {
            String2.log("Note exception being thrown:\n" + MustBe.throwableToString(e));
            throw e;
        } finally {
            //rename it back to original
            String2.log("\n*** rename it back to original\n");       
            try {
                //put things back to initial state
                File2.rename(dataDir, "bad.nc", "bad.notnc");
            } catch (Exception e) {
                throw new RuntimeException("2020-05-15 This fails half(?) the time. Why? Because the original rename failed?");
            }
            for (int i = 0; i < 5; i++) {
                String2.log("after rename .nc to .notnc, update #" + i + " " + eddGrid.update());
                Math2.sleep(1000);
            }
        }

        String2.log("\n*** after back to original again\n");       
        tName = eddGrid.makeNewFileForDapQuery(null, null, axisQuery, tDir, 
            eddGrid.className() + "_update_5a", ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        Test.ensureEqual(results, originalExpectedAxis, "\nresults=\n" + results);

        tName = eddGrid.makeNewFileForDapQuery(null, null, dataQuery, tDir, 
            eddGrid.className() + "_update_5d", ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        Test.ensureEqual(results, originalExpectedData, "\nresults=\n" + results);

        Test.ensureEqual(eddGrid.axisVariables()[0].destinationMinString(), 
            oldMinTime, "av[0].destinationMin");
        Test.ensureEqual(eddGrid.axisVariables()[0].destinationMaxString(), 
            oldMaxTime, "av[0].destinationMax");
        Test.ensureEqual(eddGrid.axisVariables()[0].combinedAttributes().get("actual_range").toString(),
            oldMinMillis + ", " + oldMaxMillis, "actual_range");
        Test.ensureEqual(eddGrid.combinedGlobalAttributes().getString("time_coverage_start"),
            oldMinTime, "time_coverage_start");
        Test.ensureEqual(eddGrid.combinedGlobalAttributes().getString("time_coverage_end"),
            oldMaxTime, "time_coverage_end");

        //test time for update if 0 events
        long cumTime = 0;
        for (int i = 0; i < 1000; i++) {
            long time = System.currentTimeMillis();
            eddGrid.update();
            cumTime += (System.currentTimeMillis() - time);
            Math2.sleep(2); //updateEveryNMillis=1, so always a valid new update()
        }
        Test.ensureTrue(cumTime/1000.0 < 11.08,
            "Too slow! time/update() = " + (cumTime/1000.0) +
            "ms (diverse results 0.001 - 11.08ms on Bob's M4700)"); 
    }

    /**
     * This tests quickRestart().
     *
     * @throws Throwable if trouble
     */
    public static void testQuickRestart() throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testQuickRestart()\n");
        //delete any cached info
        String tDatasetID = "testGriddedNcFiles";
        deleteCachedDatasetInfo(tDatasetID);

        EDDGridFromNcFiles eddGrid = (EDDGridFromNcFiles)oneFromDatasetsXml(null, tDatasetID); 
        String dataDir = eddGrid.fileDir;
        String tDir = EDStatic.fullTestCacheDirectory;
        String axisQuery = "time[]";
        String dataQuery = "x_wind[][][100][100]";
        String tName, results, expected;
        int po;

        //expected values
        String originalDas1 = 
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.1991888e+9, 1.1999664e+9;\n" +
"    String axis \"T\";\n" +
"    Int32 fraction_digits 0;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Centered Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    Float64 actual_range 0.0, 0.0;\n" +
"    String axis \"Z\";\n" +
"    Int32 fraction_digits 0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range -89.875, 89.875;\n" +
"    String axis \"Y\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 2;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String point_spacing \"even\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range 0.125, 359.875;\n" +
"    String axis \"X\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 2;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String point_spacing \"even\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  x_wind {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum -15.0;\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 1;\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Zonal Wind\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"x_wind\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  y_wind {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum -15.0;\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 1;\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Meridional Wind\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"y_wind\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  mod {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float64 colorBarMaximum 18.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String colorBarPalette \"WhiteRedBlack\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 1;\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Modulus of Wind\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String acknowledgement \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String composite \"true\";\n" +
"    String contributor_name \"Remote Sensing Systems, Inc\";\n" +
"    String contributor_role \"Source of level 2 data.\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String creator_email \"erd.data@noaa.gov\";\n" +
"    String creator_name \"NOAA CoastWatch, West Coast Node\";\n" +
"    String creator_url \"https://coastwatch.pfeg.noaa.gov\";\n" +
"    String date_created \"2008-08-29\";\n" +
"    String date_issued \"2008-08-29\";\n" +
"    Float64 Easternmost_Easting 359.875;\n" +
"    Float64 geospatial_lat_max 89.875;\n" +
"    Float64 geospatial_lat_min -89.875;\n" +
"    Float64 geospatial_lat_resolution 0.25;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 359.875;\n" +
"    Float64 geospatial_lon_min 0.125;\n" +
"    Float64 geospatial_lon_resolution 0.25;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    Float64 geospatial_vertical_max 0.0;\n" +
"    Float64 geospatial_vertical_min 0.0;\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"Remote Sensing Systems, Inc\n" +
"2008-08-29T00:31:43Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD\n";
//2015-09-09T22:18:53Z http://192.168.31.18/thredds/dodsC/satellite/QS/ux10/1day
//2015-09-09T22:18:53Z 
    String originalDas2 = 
"http://localhost:8080/cwexperimental/griddap/testGriddedNcFiles.das\";\n" +
"    String infoUrl \"https://coastwatch.pfeg.noaa.gov/infog/QS_ux10_las.html\";\n" +
"    String institution \"NOAA CoastWatch, West Coast Node\";\n" +
"    String keywords \"Earth Science > Oceans > Ocean Winds > Surface Winds\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.\";\n" +
"    String naming_authority \"gov.noaa.pfeg.coastwatch\";\n" +
"    Float64 Northernmost_Northing 89.875;\n" +
"    String origin \"Remote Sensing Systems, Inc\";\n" +
"    String processing_level \"3\";\n" +
"    String project \"CoastWatch (https://coastwatch.noaa.gov/)\";\n" +
"    String projection \"geographic\";\n" +
"    String projection_type \"mapped\";\n" +
"    String references \"RSS Inc. Winds: http://www.remss.com/ .\";\n" +
"    String satellite \"QuikSCAT\";\n" +
"    String sensor \"SeaWinds\";\n" +
"    String source \"satellite observation: QuikSCAT, SeaWinds\";\n" +
"    String sourceUrl \"http://192.168.31.18/thredds/dodsC/satellite/QS/ux10/1day\";\n" +
"    Float64 Southernmost_Northing -89.875;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String summary \"Remote Sensing Inc. distributes science quality wind velocity data from the SeaWinds instrument onboard NASA's QuikSCAT satellite.  SeaWinds is a microwave scatterometer designed to measure surface winds over the global ocean.  Wind velocity fields are provided in zonal, meriodonal, and modulus sets. The reference height for all wind velocities is 10 meters.\";\n" +
"    String time_coverage_end \"2008-01-10T12:00:00Z\";\n" +
"    String time_coverage_start \"2008-01-01T12:00:00Z\";\n" +
"    String title \"Wind, QuikSCAT, Global, Science Quality (1 Day Composite)\";\n" +
"    Float64 Westernmost_Easting 0.125;\n" +
"  }\n" +
"}\n";
        String originalExpectedAxis = 
"time\n" +
"UTC\n" +
"2008-01-01T12:00:00Z\n" +
"2008-01-02T12:00:00Z\n" +
"2008-01-03T12:00:00Z\n" +
"2008-01-04T12:00:00Z\n" +
"2008-01-05T12:00:00Z\n" +
"2008-01-06T12:00:00Z\n" +
"2008-01-07T12:00:00Z\n" +
"2008-01-08T12:00:00Z\n" +
"2008-01-09T12:00:00Z\n" +
"2008-01-10T12:00:00Z\n";      
        String originalExpectedData = 
"time,altitude,latitude,longitude,x_wind\n" +
"UTC,m,degrees_north,degrees_east,m s-1\n" +
"2008-01-01T12:00:00Z,0.0,-64.875,25.125,-3.24724\n" +
"2008-01-02T12:00:00Z,0.0,-64.875,25.125,6.00287\n" +
"2008-01-03T12:00:00Z,0.0,-64.875,25.125,NaN\n" +
"2008-01-04T12:00:00Z,0.0,-64.875,25.125,-3.0695\n" +
"2008-01-05T12:00:00Z,0.0,-64.875,25.125,-7.76223\n" +
"2008-01-06T12:00:00Z,0.0,-64.875,25.125,-12.8834\n" +
"2008-01-07T12:00:00Z,0.0,-64.875,25.125,4.782275\n" +
"2008-01-08T12:00:00Z,0.0,-64.875,25.125,9.80197\n" +
"2008-01-09T12:00:00Z,0.0,-64.875,25.125,-7.5635605\n" +
"2008-01-10T12:00:00Z,0.0,-64.875,25.125,-7.974725\n";      

        String oldMinTime   = "2008-01-01T12:00:00Z";
        String oldMinMillis = "1.1991888E9";
        String newMinTime   = "2008-01-04T12:00:00Z";
        String newMinMillis = "1.199448E9";
        String oldMaxTime   = "2008-01-10T12:00:00Z";
        String oldMaxMillis = "1.1999664E9";


        //*** read the original data
        String2.log("\n*** read original data\n");     
        long oCreationTimeMillis = eddGrid.creationTimeMillis();

        tName = eddGrid.makeNewFileForDapQuery(null, null, "", tDir, 
            eddGrid.className() + "_qr_1das", ".das"); 
        results = String2.directReadFrom88591File(tDir + tName);
        Test.ensureEqual(results.substring(0, originalDas1.length()), originalDas1, "\nresults=\n" + results);

        po = results.indexOf(originalDas2.substring(0, 80));
        Test.ensureEqual(results.substring(po), originalDas2, "\nresults=\n" + results);

        tName = eddGrid.makeNewFileForDapQuery(null, null, axisQuery, tDir, 
            eddGrid.className() + "_qr_1a", ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        Test.ensureEqual(results, originalExpectedAxis, "\nresults=\n" + results);

        tName = eddGrid.makeNewFileForDapQuery(null, null, dataQuery, tDir, 
            eddGrid.className() + "_qr_1d", ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        Test.ensureEqual(results, originalExpectedData, "\nresults=\n" + results);

        Test.ensureEqual(eddGrid.axisVariables()[0].destinationMinString(), 
            oldMinTime, "av[0].destinationMin");
        Test.ensureEqual(eddGrid.axisVariables()[0].destinationMaxString(), 
            oldMaxTime, "av[0].destinationMax");
        Test.ensureEqual(eddGrid.axisVariables()[0].combinedAttributes().get("actual_range").toString(),
            oldMinMillis + ", " + oldMaxMillis, "actual_range");
        Test.ensureEqual(eddGrid.combinedGlobalAttributes().getString("time_coverage_start"),
            oldMinTime, "time_coverage_start");
        Test.ensureEqual(eddGrid.combinedGlobalAttributes().getString("time_coverage_end"),
            oldMaxTime, "time_coverage_end");


        try {
            //*** rename a data file so it doesn't match regex
            //set testQuickRestart=true
            //and reload dataset
            String2.log("\n*** testQuickRestart=true\n");       
            File2.rename(dataDir, "erdQSwind1day_20080101_03.nc.gz", "erdQSwind1day_20080101_03.nc.gz2"); //throws exception if trouble
            File2.deleteAllFiles(decompressedDirectory(tDatasetID)); //ensure cache is empty
            Math2.sleep(1000);
            EDDGridFromFiles.testQuickRestart = true;
            eddGrid = (EDDGridFromNcFiles)oneFromDatasetsXml(null, tDatasetID); 

            //some responses are same
            Test.ensureEqual(eddGrid.creationTimeMillis(), oCreationTimeMillis, "");

            tName = eddGrid.makeNewFileForDapQuery(null, null, "", tDir, 
                eddGrid.className() + "_qr_2das", ".das"); 
            results = String2.directReadFrom88591File(tDir + tName);
            Test.ensureEqual(results.substring(0, originalDas1.length()), originalDas1, "\nresults=\n" + results);

            po = results.indexOf(originalDas2.substring(0, 80));
            Test.ensureEqual(results.substring(po), originalDas2, "\nresults=\n" + results);

            tName = eddGrid.makeNewFileForDapQuery(null, null, axisQuery, tDir, 
                eddGrid.className() + "_qr_2a", ".csv"); 
            results = String2.directReadFrom88591File(tDir + tName);
            Test.ensureEqual(results, originalExpectedAxis, "\nresults=\n" + results);

            Test.ensureEqual(eddGrid.axisVariables()[0].destinationMinString(), 
                oldMinTime, "av[0].destinationMin");
            Test.ensureEqual(eddGrid.axisVariables()[0].destinationMaxString(), 
                oldMaxTime, "av[0].destinationMax");
            Test.ensureEqual(eddGrid.axisVariables()[0].combinedAttributes().get("actual_range").toString(),
                oldMinMillis + ", " + oldMaxMillis, "actual_range");
            Test.ensureEqual(eddGrid.combinedGlobalAttributes().getString("time_coverage_start"),
                oldMinTime, "time_coverage_start");
            Test.ensureEqual(eddGrid.combinedGlobalAttributes().getString("time_coverage_end"),
                oldMaxTime, "time_coverage_end");

            //but request for data should fail
            try {
                tName = eddGrid.makeNewFileForDapQuery(null, null, dataQuery, tDir, 
                    eddGrid.className() + "_qr_2d", ".csv"); 
                results = String2.directReadFrom88591File(tDir + tName); //shouldn't happen. We expect fileNotFound.
            } catch (Throwable t2) {
                results = t2.getMessage();
            }
            expected = 
"There was a (temporary?) problem.  Wait a minute, then try again.  (In a browser, click the Reload button.)\n" +
"(Cause: java.io.FileNotFoundException: \\erddapTest\\erdQSwind1day\\erdQSwind1day_20080101_03.nc.gz (The system cannot find the file specified))";      
            Test.ensureEqual(results, expected, "\nresults=\n" + results);

        } finally {
            //rename file back to original
            String2.log("\n*** rename it back to original\n");       
            File2.rename(dataDir, "erdQSwind1day_20080101_03.nc.gz2", "erdQSwind1day_20080101_03.nc.gz");
            //delete badFile file so the 20080101 file is re-considered
            File2.delete(badFileMapFileName(tDatasetID));
            while (!File2.isFile(dataDir + "erdQSwind1day_20080101_03.nc.gz")) {
                String2.log("Waiting for " + dataDir + "erdQSwind1day_20080101_03.nc.gz2 to be named back to .nc.gz"); 
                Math2.sleep(1000);
            }

            //ensure testQuickRestart is set back to false
            EDDGridFromFiles.testQuickRestart = false;
        }

        //*** redo original tests
        String2.log("\n*** redo original tests\n");       
        eddGrid = (EDDGridFromNcFiles)oneFromDatasetsXml(null, tDatasetID); 

        //creationTime should have changed
        Test.ensureNotEqual(eddGrid.creationTimeMillis(), oCreationTimeMillis, "");

        //but everything else should be back to original
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", tDir, 
            eddGrid.className() + "_qr_3das", ".das"); 
        results = String2.directReadFrom88591File(tDir + tName);
        Test.ensureEqual(results.substring(0, originalDas1.length()), originalDas1, "\nresults=\n" + results);

        po = results.indexOf(originalDas2.substring(0, 80));
        Test.ensureEqual(results.substring(po), originalDas2, "\nresults=\n" + results);

        tName = eddGrid.makeNewFileForDapQuery(null, null, axisQuery, tDir, 
            eddGrid.className() + "_qr_3a", ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        Test.ensureEqual(results, originalExpectedAxis, "\nresults=\n" + results);

        tName = eddGrid.makeNewFileForDapQuery(null, null, dataQuery, tDir, 
            eddGrid.className() + "_qr_3d", ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        Test.ensureEqual(results, originalExpectedData, "\nresults=\n" + results);

        Test.ensureEqual(eddGrid.axisVariables()[0].destinationMinString(), 
            oldMinTime, "av[0].destinationMin");
        Test.ensureEqual(eddGrid.axisVariables()[0].destinationMaxString(), 
            oldMaxTime, "av[0].destinationMax");
        Test.ensureEqual(eddGrid.axisVariables()[0].combinedAttributes().get("actual_range").toString(),
            oldMinMillis + ", " + oldMaxMillis, "actual_range");
        Test.ensureEqual(eddGrid.combinedGlobalAttributes().getString("time_coverage_start"),
            oldMinTime, "time_coverage_start");
        Test.ensureEqual(eddGrid.combinedGlobalAttributes().getString("time_coverage_end"),
            oldMaxTime, "time_coverage_end");

    }

    /**
     * !!! NOT FINISHED/TESTABLE: this test server doesn't support byte ranges!!!
     *
     * This tests if netcdf-java can work with remote Hyrax files
     * and thus if a dataset can be made from remote Hyrax files.
     *
     * @throws Throwable if trouble
     */
    public static void testGenerateDatasetsXmlWithRemoteHyraxFiles() throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testGenerateDatasetsXmlWithRemoteHyraxFiles()\n");
        testVerboseOn();
        String results, expected;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
        String dir = "https://opendap.jpl.nasa.gov/opendap/hyrax/allData/avhrr/L4/reynolds_er/v3b/monthly/netcdf/2014/";

        results = generateDatasetsXml( //dir is a HYRAX catalog.html URL!
            dir + "contents.html", 
            ".*\\.nc",  
            dir + "ersst.201401.nc", 
            "", //group
            "", -1, null, null); //dimensionsCSV, reloadMinutes, cacheFromUrl
        //String2.log(results);
        expected = "zztop";
        Test.ensureEqual(results, expected, "results=\n" + results);

        String2.log("\n*** EDDGridFromNcFiles.testGenerateDatasetsXmlWithRemoteHyraxFiles() finished successfully\n");
   
    }

    /**
     * !!! DON'T DO THIS! Use &lt;cacheFromUrl&gt; instead.
     * !!! NOT FINISHED/TESTABLE: this test server doesn't support byte ranges!!!
     *
     * This tests if netcdf-java can work with remote files
     * and thus if a dataset can be made from remote files.
     *
     * @throws Throwable if trouble
     */
    public static void testRemoteHyraxFiles(boolean deleteCachedInfo) throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testRemoteHyraxFiles(" + deleteCachedInfo + 
            ")\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        int po;
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
        String id = "testRemoteHyraxFiles";

/*
        //*** test getting das for entire dataset
        String2.log("\n*** testCwHdf test das dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_CwHdfEntire", ".das"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"Attributes {\n" +
"  rows {\n" +
"    Int32 actual_range 0, 1247;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Rows\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  cols {\n" +
"    Int16 actual_range 0, 1139;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Cols\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  avhrr_ch1 {\n" +
"    Float64 _FillValue -327.68;\n" +
"    Float64 add_offset_err 0.0;\n" +
"    Int32 calibrated_nt 0;\n" +
"    String coordsys \"Mercator\";\n" +
"    Int32 fraction_digits 2;\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"AVHRR Channel 1\";\n" +
"    Float64 missing_value -327.68;\n" +
"    Float64 scale_factor_err 0.0;\n" +
"    String standard_name \"isotropic_spectral_radiance_in_air\";\n" +
"    String units \"percent\";\n" +
"  }\n" +
"  sst {\n" +
"    Float64 _FillValue -327.68;\n" +
"    Float64 add_offset_err 0.0;\n" +
"    Int32 calibrated_nt 0;\n" +
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String coordsys \"Mercator\";\n" +
"    Int32 fraction_digits 2;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Sea Surface Temperature\";\n" +
"    Float64 missing_value -327.68;\n" +
"    Float64 scale_factor_err 0.0;\n" +
"    String sst_equation_day \"nonlinear split window\";\n" +
"    String sst_equation_night \"linear triple window modified\";\n" +
"    String standard_name \"sea_surface_temperature\";\n" +
"    String units \"celsius\";\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String _History \"Direct read of HDF4 file through CDM library\";\n" +
"    String autonav_performed \"true\";\n" +
"    Int32 autonav_quality 2;\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String history \"Direct read of HDF4 file through CDM library\n" +
today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);
        
//+ " (local files)\n" +
//today + 
    
expected =
" http://localhost:8080/cwexperimental/griddap/testCwHdf.das\";\n" +
"    String infoUrl \"???\";\n" +
"    String institution \"NOAA CoastWatch\";\n" +
"    String keywords \"Oceans > Ocean Temperature > Sea Surface Temperature\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String origin \"USDOC/NOAA/NESDIS CoastWatch\";\n" +
"    String pass_type \"day\";\n" +
"    String projection \"Mercator\";\n" +
"    String projection_type \"mapped\";\n" +
"    String satellite \"noaa-18\";\n" +
"    String sensor \"avhrr\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v55\";\n" +
"    String summary \"???\";\n" +
"    String title \"Test of CoastWatch HDF files\";\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_CwHdfEntire", ".dds"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Int32 rows[rows = 1248];\n" +
"  Int16 cols[cols = 1140];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 avhrr_ch1[rows = 1248][cols = 1140];\n" +
"    MAPS:\n" +
"      Int32 rows[rows = 1248];\n" +
"      Int16 cols[cols = 1140];\n" +
"  } avhrr_ch1;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 sst[rows = 1248][cols = 1140];\n" +
"    MAPS:\n" +
"      Int32 rows[rows = 1248];\n" +
"      Int16 cols[cols = 1140];\n" +
"  } sst;\n" +
"} testCwHdf;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv  with data from one file
        String2.log("\n*** testCwHdf test read from one file\n");       
        userDapQuery = "sst[600:2:606][500:503]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_CwHdfData1", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"rows,cols,sst\n" +
"count,count,celsius\n" +
"600,500,21.07\n" +
"600,501,20.96\n" +
"600,502,21.080000000000002\n" +
"600,503,20.93\n" +
"602,500,21.16\n" +
"602,501,21.150000000000002\n" +
"602,502,21.2\n" +
"602,503,20.95\n" +
"604,500,21.34\n" +
"604,501,21.13\n" +
"604,502,21.13\n" +
"604,503,21.25\n" +
"606,500,21.37\n" +
"606,501,21.11\n" +
"606,502,21.0\n" +
"606,503,21.02\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //  */
    }

    /**
     * This tests if netcdf-java can work with remote Thredds files
     * and thus if a dataset can be made from remote Thredds files.
     *
     * @throws Throwable if trouble
     */
    public static void testGenerateDatasetsXmlWithRemoteThreddsFiles() throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testGenerateDatasetsXmlWithRemoteThreddsFiles()\n");
        testVerboseOn();
        String results, expected;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
        //2018-08-08 used to work with /catalog/. Now needs /fileServer/
        String dir = "https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V4.0/"; //catalog.html

if (true) Test.knownProblem("2020-10-26 This needs a new test url. The current one is unreliable: " + dir);

      try {
        //dir  is a /thredds/catalog/.../  [implied catalog.html] URL!
        //file is a /thredds/fileServer/... not compressed data file.
        results = generateDatasetsXml( 
            dir, 
            "sss_binned_L3_MON_SCI_V4.0_\\d{4}\\.nc", 
            //sample file is a thredds/fileServer/.../...nc URL!
            dir + "monthly/sss_binned_L3_MON_SCI_V4.0_2011.nc", 
            "", //group
            "", -1, null, null); //dimensionsCSV, reloadMinutes, cacheFromUrl
        //String2.log(results);
String2.setClipboardString(results);

        expected = 
"<dataset type=\"EDDGridFromNcFiles\" datasetID=\"noaa_nodc_a2c2_29ed_1915\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>0</updateEveryNMillis>\n" +
"    <fileDir>https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V4.0/</fileDir>\n" +
"    <fileNameRegex>sss_binned_L3_MON_SCI_V4.0_\\d{4}\\.nc</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <matchAxisNDigits>20</matchAxisNDigits>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"Conventions\">CF-1.6</att>\n" +
"        <att name=\"creator_email\">Yongsheng.Zhang@noaa.gov</att>\n" +
"        <att name=\"creator_name\">Yongsheng Zhang, Ph.D.</att>\n" +
"        <att name=\"creator_url\">http://www.nodc.noaa.gov/SatelliteData</att>\n" +
"        <att name=\"date_created\">20151212T082610Z</att>\n" +  //changes
"        <att name=\"geospatial_lat_max\" type=\"double\">90.0</att>\n" +
"        <att name=\"geospatial_lat_min\" type=\"double\">-90.0</att>\n" +
"        <att name=\"geospatial_lat_resolution\">1.0 degree grids</att>\n" +
"        <att name=\"geospatial_lat_units\">degrees_north</att>\n" +
"        <att name=\"geospatial_lon_max\" type=\"double\">180.0</att>\n" +
"        <att name=\"geospatial_lon_min\" type=\"double\">-180.0</att>\n" +
"        <att name=\"geospatial_lon_resolution\">1.0 degree grids</att>\n" +
"        <att name=\"geospatial_lon_units\">degrees_east</att>\n" +
"        <att name=\"grid_mapping_name\">latitude_longitude</att>\n" +
"        <att name=\"history\">Aquarius Level-2 SCI CAP V4.0</att>\n" +
"        <att name=\"institution\">US National Centers for Environmental Information</att>\n" +
"        <att name=\"keywords\">Earth Science &gt;Oceans &gt; Surface Salinity</att>\n" +
"        <att name=\"keywords_vocabulary\">NASA Global Change Master Directory (GCMD) Science Keywords</att>\n" +
"        <att name=\"license\">These data are available for use without restriction.</att>\n" +
"        <att name=\"Metadata_Conventions\">Unidata Dataset Discovery v1.0</att>\n" +
"        <att name=\"mission\">SAC-D Aquarius</att>\n" +
"        <att name=\"necdf_version_id\">3.6.3</att>\n" +
"        <att name=\"nodc_template_version\">NODC_NetCDF_Grid_Template_v1.0</att>\n" +
"        <att name=\"publisher_name\">NOAA/NESDIS/NCEI - US National Centers for Environmental Information</att>\n" +
"        <att name=\"references\">Aquarius users guide, V7.0, PO.DAAC, JPL/NASA. Jul 14, 2015</att>\n" +
"        <att name=\"sensor\">Aquarius</att>\n" +
"        <att name=\"source\">Jet Propulsion Laboratory, California Institute of Technology</att>\n" +
"        <att name=\"summary\">This dataset is created by NCEI Satellite Oceanography Group from Aquarius level-2 SCI V4.0 data,using 1.0x1.0 (lon/lat) degree box average</att>\n" +
"        <att name=\"title\">Gridded monthly mean Sea Surface Salinity calculated from Aquarius level-2 SCI V4.0 data</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"Conventions\">CF-1.6, COARDS, ACDD-1.3</att>\n" +
"        <att name=\"creator_type\">person</att>\n" +
"        <att name=\"creator_url\">https://www.nodc.noaa.gov/SatelliteData</att>\n" +
"        <att name=\"date_created\">2015-12-12T08:26:10Z</att>\n" +
"        <att name=\"infoUrl\">https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V4.0/</att>\n" +
"        <att name=\"keywords\">aquarius, calculate, calculated, centers, data, density, earth, Earth Science &gt; Oceans &gt; Salinity/Density &gt; Salinity, Earth Science &gt;Oceans &gt; Surface Salinity, environmental, gridded, information, level, level-2, mean, month, monthly, national, ncei, nesdis, noaa, number, observation, observations, ocean, oceans, practical, salinity, sci, science, sea, sea_surface_salinity_number_of_observations, sea_water_practical_salinity, seawater, sss, sss_obs, statistics, surface, swath, time, US, used, v4.0, valid, water</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"Metadata_Conventions\">null</att>\n" +
"        <att name=\"publisher_type\">group</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" +
"        <att name=\"summary\">This dataset is created by National Centers for Environmental Information (NCEI) Satellite Oceanography Group from Aquarius level-2 SCI V4.0 data,using 1.0x1.0 (lon/lat) degree box average</att>\n" +
"    </addAttributes>\n" +
"    <axisVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"axis\">T</att>\n" +
"            <att name=\"calendar\">gregorian</att>\n" +
"            <att name=\"long_name\">time</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"units\">months since 2010-01-15 00:00:00</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"units\">months since 2010-01-15T00:00:00Z</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>lat</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-89.5 89.5</att>\n" +
"            <att name=\"axis\">Y</att>\n" +
"            <att name=\"coordinate_defines\">center</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>lon</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-179.5 179.5</att>\n" +
"            <att name=\"axis\">X</att>\n" +
"            <att name=\"coordinate_defines\">center</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>sss</sourceName>\n" +
"        <destinationName>sss</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999.0</att>\n" +
"            <att name=\"add_offset\" type=\"float\">0.0</att>\n" +
"            <att name=\"ancillary_variables\">sss_obs </att>\n" +
"            <att name=\"cell_methods\">time: mean over one month</att>\n" +
"            <att name=\"coordinates\">time lat lon</att>\n" +
"            <att name=\"grid_mapping\">crs</att>\n" +
"            <att name=\"long_name\"> Monthly mean Sea Surface Salinity from level-2 swath data</att>\n" +
"            <att name=\"scale_factor\" type=\"float\">1.0</att>\n" +
"            <att name=\"standard_name\">Sea_Surface_Salinity</att>\n" +
"            <att name=\"units\">psu</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"add_offset\">null</att>\n" +
"            <att name=\"ancillary_variables\">sss_obs</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">37.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">32.0</att>\n" +
"            <att name=\"coordinates\">null</att>\n" +
"            <att name=\"grid_mapping\">null</att>\n" +
"            <att name=\"ioos_category\">Salinity</att>\n" +
"            <att name=\"long_name\">Monthly mean Sea Surface Salinity from level-2 swath data</att>\n" +
"            <att name=\"scale_factor\">null</att>\n" +
"            <att name=\"standard_name\">sea_water_practical_salinity</att>\n" +
"            <att name=\"units\">PSU</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>sss_obs</sourceName>\n" +
"        <destinationName>sss_obs</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999.0</att>\n" +
"            <att name=\"add_offset\" type=\"float\">0.0</att>\n" +
"            <att name=\"cell_methods\">time: sum over one month</att>\n" +
"            <att name=\"coordinates\">time lat lon</att>\n" +
"            <att name=\"grid_mapping\">crs</att>\n" +
"            <att name=\"long_name\">Valid observation number used to calculate SSS from level-2 swath data</att>\n" +
"            <att name=\"scale_factor\" type=\"float\">1.0</att>\n" +
"            <att name=\"standard_name\">Sea_Surface_Salinity_number_of_observations</att>\n" +
"            <att name=\"units\">count</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"add_offset\">null</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"coordinates\">null</att>\n" +
"            <att name=\"grid_mapping\">null</att>\n" +
"            <att name=\"ioos_category\">Statistics</att>\n" +
"            <att name=\"scale_factor\">null</att>\n" +
"            <att name=\"standard_name\">sea_surface_salinity_number_of_observations</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        String2.log("\n*** EDDGridFromNcFiles.testGenerateDatasetsXmlWithRemoteThreddsFiles() finished successfully\n");
        } catch (Throwable t) {
            throw new RuntimeException("Unexpected error", 
                //"\n2018-06-20 FIXED ?! \n" +
                //"2016-02-29 This started failing with netcdf-java 4.6.4 with\n" +
                //"message about End of File at position 20.\n" +
                //"I reported to netcdf-java github BugTracker,\n" +
                //"but they aren't interested in pursuing if I'm not."
                t); 
        }        
  
    }

    /**
     * This tests if netcdf-java can work with remote files
     * and thus if a dataset can be made from remote files.
     *
     * @throws Throwable if trouble
     */
    public static void testRemoteThreddsFiles(boolean deleteCachedInfo) throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testRemoteThreddsFiles(" + deleteCachedInfo + 
            ")\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        int po;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
        String id = "testRemoteThreddsFiles";  //from generateDatasetsXml above but different datasetID
      try {   
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, id);

        //*** test getting das for entire dataset
        String2.log("\n*** test das dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_trtf", ".das"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.3133664e+9, 1.3555296e+9;\n" + //was 1.3870656e+9 2013-12-15T00:00:00Z  and should be 2015-...
"    String axis \"T\";\n" +
"    String calendar \"gregorian\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range -89.5, 89.5;\n" +
"    String axis \"Y\";\n" +
"    String coordinate_defines \"center\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 actual_range -179.5, 179.5;\n" +
"    String axis \"X\";\n" +
"    String coordinate_defines \"center\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  sss {\n" +
"    Float32 _FillValue -9999.0;\n" +
"    String ancillary_variables \"sss_obs \";\n" +
"    String cell_methods \"time: mean over one month\";\n" +
"    Float64 colorBarMaximum 37.0;\n" +
"    Float64 colorBarMinimum 32.0;\n" +
"    String grid_mapping \"crs\";\n" +
"    String ioos_category \"Salinity\";\n" +
"    String long_name \" Monthly mean Sea Surface Salinity from level-2 swath data\";\n" +
"    String standard_name \"sea_surface_salinity\";\n" +
"    String units \"psu\";\n" +
"  }\n" +
"  sss_obs {\n" +
"    Float32 _FillValue -9999.0;\n" +
"    String cell_methods \"time: sum over one month\";\n" +
"    Float64 colorBarMaximum 100.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String grid_mapping \"crs\";\n" +
"    String ioos_category \"Statistics\";\n" +
"    String long_name \"Valid observation number used to calculate SSS from level-2 swath data\";\n" +
"    String standard_name \"sea_surface_salinity_number_of_observations\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String Conventions \"CF-1.6, COARDS, ACDD-1.3\";\n" +
"    String creator_email \"Yongsheng.Zhang@noaa.gov\";\n" +
"    String creator_name \"Yongsheng Zhang, Ph.D.\";\n" +
"    String creator_url \"https://www.nodc.noaa.gov/SatelliteData\";\n" +
"    String date_created \"2015-12-12T08:26:10Z\";\n" +  
"    Float64 Easternmost_Easting 179.5;\n" +
"    Float64 geospatial_lat_max 89.5;\n" +
"    Float64 geospatial_lat_min -89.5;\n" +
"    Float64 geospatial_lat_resolution 1.0;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 179.5;\n" +
"    Float64 geospatial_lon_min -179.5;\n" +
"    Float64 geospatial_lon_resolution 1.0;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String grid_mapping_name \"latitude_longitude\";\n" +
"    String history \"Aquarius Level-2 SCI CAP V4.0\n" +
today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);
        
//+ " (local files)\n" +
//today + 
    
expected =
"Z http://localhost:8080/cwexperimental/griddap/testRemoteThreddsFiles.das\";\n" +
"    String infoUrl \"https://data.nodc.noaa.gov/thredds/catalog/aquarius/nodc_binned_V4.0/catalog.html\";\n" +
"    String institution \"JPL, California Institute of Technology\";\n" +
"    String keywords \"aquarius, calculate, calculated, california, center, data, earth, Earth Science >Oceans > Surface Salinity, gridded, institute, jet, jpl, laboratory, level, level-2, mean, month, monthly, national, ncei, noaa, nodc, number, observation, observations, ocean, oceanographic, oceans, propulsion, salinity, sci, science, sea, sea_surface_salinity, sea_surface_salinity_number_of_observations, sss, sss_obs, statistics, surface, swath, technology, time, used, v4.0, valid\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"These data are available for use without restriction.\";\n" +
"    String mission \"SAC-D Aquarius\";\n" +
"    String necdf_version_id \"3.6.3\";\n" +
"    String nodc_template_version \"NODC_NetCDF_Grid_Template_v1.0\";\n" +
"    Float64 Northernmost_Northing 89.5;\n" +
"    String publisher_name \"NOAA/NESDIS/NCEI - US National Centers for Environmental Information\";\n" +
"    String references \"Aquarius users guide, V7.0, PO.DAAC, JPL/NASA. Jul 14, 2015\";\n" +
"    String sensor \"Aquarius\";\n" +
"    String source \"Jet Propulsion Laboratory, California Institute of Technology\";\n" +
"    String sourceUrl \"(remote files)\";\n" +
"    Float64 Southernmost_Northing -89.5;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v55\";\n" +
"    String summary \"This dataset is created by National Oceanographic Data Center (NODC) Satellite Oceanography Group from Aquarius level-2 SCI V4.0 data,using 1.0x1.0 (lon/lat) degree box average\";\n" +
"    String time_coverage_end \"2012-12-15T00:00:00Z\";\n" +   //varies
"    String time_coverage_start \"2011-08-15T00:00:00Z\";\n" + //varies
"    String title \"Gridded monthly mean Sea Surface Salinity calculated from Aquarius level-2 SCI V4.0 data\";\n" +
"    Float64 Westernmost_Easting -179.5;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_trtf", ".dds"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Float64 time[time = 17];\n" +  //varies
"  Float32 latitude[latitude = 180];\n" +
"  Float32 longitude[longitude = 360];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 sss[time = 17][latitude = 180][longitude = 360];\n" +
"    MAPS:\n" +
"      Float64 time[time = 17];\n" +
"      Float32 latitude[latitude = 180];\n" +
"      Float32 longitude[longitude = 360];\n" +
"  } sss;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 sss_obs[time = 17][latitude = 180][longitude = 360];\n" +
"    MAPS:\n" +
"      Float64 time[time = 17];\n" +
"      Float32 latitude[latitude = 180];\n" +
"      Float32 longitude[longitude = 360];\n" +
"  } sss_obs;\n" +
"} testRemoteThreddsFiles;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv  with data from one file
        String2.log("\n*** test read from one file\n");       
        userDapQuery = "sss[17][100:2:106][50:53]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_trtf", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"time,latitude,longitude,sss\n" +
"UTC,degrees_north,degrees_east,psu\n" +
"2013-01-15T00:00:00Z,-10.5,-129.5,35.65809\n" +
"2013-01-15T00:00:00Z,-10.5,-128.5,35.731663\n" +
"2013-01-15T00:00:00Z,-10.5,-127.5,35.701374\n" +
"2013-01-15T00:00:00Z,-10.5,-126.5,35.746387\n" +
"2013-01-15T00:00:00Z,-12.5,-129.5,35.796375\n" +
"2013-01-15T00:00:00Z,-12.5,-128.5,35.85154\n" +
"2013-01-15T00:00:00Z,-12.5,-127.5,35.765373\n" +
"2013-01-15T00:00:00Z,-12.5,-126.5,35.82581\n" +
"2013-01-15T00:00:00Z,-14.5,-129.5,35.930275\n" +
"2013-01-15T00:00:00Z,-14.5,-128.5,35.853966\n" +
"2013-01-15T00:00:00Z,-14.5,-127.5,35.830906\n" +
"2013-01-15T00:00:00Z,-14.5,-126.5,35.924076\n" +
"2013-01-15T00:00:00Z,-16.5,-129.5,36.03288\n" +
"2013-01-15T00:00:00Z,-16.5,-128.5,36.052273\n" +
"2013-01-15T00:00:00Z,-16.5,-127.5,35.98247\n" +
"2013-01-15T00:00:00Z,-16.5,-126.5,36.004646\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t)); 
                //"\n2018-06-20 FIXED ?! \n" +
                //"\n2016-02-29 This started failing with netcdf-java 4.6.4 with\n" +
                //"message about End of File at position 20 for each file\n" +
                //"(I reported to netcdf-java github BugTracker,\n" +
                //"but they aren't interested in pursuing if I'm not),\n" +
                //"so no valid files."); 
        }        
        //  */
    }

    /** This tests matchAxisNDigits */
    public static void testMatchAxisNDigits() throws Throwable {

        String2.log("\n *** EDDGridFromNcFiles.testMatchAxisNDigits() ***");

        //force reload files
        File2.delete("/data/_erddapBPD/dataset/ay/erdATssta3day/fileTable.nc");

        //load dataset
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String cDir = EDStatic.fullTestCacheDirectory;
        String error = "";
        int po;
        String id = "erdATssta3day"; 
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, id);
        Table table;
        PrimitiveArray pa1, pa2;

        //there should be two time values
        String2.log("\n*** test all time values\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "time", 
            cDir, eddGrid.className() + "_tmnd0", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"time\n" +
"UTC\n" +
"2004-02-18T12:00:00Z\n" +
"2004-03-20T12:00:00Z\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

//a further test by hand:
//change erdATssta3day's <matchAxisNValues> to 20
//  and above test will fail because n time points will be 1 
//  (because axes don't match: because lat [641](30.0125 != 30.012500000000003)

        //*** even though the 2 files have slightly different axis values
        //    test that lats returned from requests are always the same
        String2.log("\n*** test get one lon, all lats\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "sst[0][0][][0]", 
            cDir, eddGrid.className() + "_tmnd1", ".csv"); 
        table = new Table();
        table.readASCII(cDir + tName, 0, 2);
        pa1 = table.getColumn("latitude");

        tName = eddGrid.makeNewFileForDapQuery(null, null, "sst[1][0][][0]", 
            cDir, eddGrid.className() + "_tmnd2", ".csv"); 
        table = new Table();
        table.readASCII(cDir + tName, 0, 2);
        pa2 = table.getColumn("latitude");

        String2.log("pa1[0:10]=" + pa1.subset(0, 1, 10).toString());
        Test.ensureEqual(pa1.testEquals(pa2), "", "");

        //*** even though the 2 files have slightly different axis values
        //    test that lons returned from requests are always the same
        String2.log("\n*** test get one lat, all lons\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "sst[0][0][0][]", 
            cDir, eddGrid.className() + "_tmnd3", ".csv"); 
        table = new Table();
        table.readASCII(cDir + tName, 0, 2);
        pa1 = table.getColumn("longitude");

        tName = eddGrid.makeNewFileForDapQuery(null, null, "sst[1][0][0][]", 
            cDir, eddGrid.className() + "_tmnd4", ".csv"); 
        table = new Table();
        table.readASCII(cDir + tName, 0, 2);
        pa2 = table.getColumn("longitude");

        String2.log("pa1[0:10]=" + pa1.subset(0, 1, 10).toString());
        Test.ensureEqual(pa1.testEquals(pa2), "", "");

    }

    /**
     * Test file created from 
     * https://thredds.jpl.nasa.gov/thredds/ncss/grid/ncml_aggregation/OceanTemperature/modis/aqua/11um/9km/aggregate__MODIS_AQUA_L3_SST_THERMAL_8DAY_9KM_DAYTIME.ncml/dataset.html
     * and stored in /erddapTest/unsigned/.
     * Note that the data file has errors: qual var has erroneous scale_factor and add_offset.
     *
     * @throws Throwable if trouble
     */
    public static void testUInt16File() throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testUInt16File");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery;
        String today = Calendar2.getCurrentISODateTimeStringZulu() + "Z";
        String fileDir = EDStatic.unitTestDataDir + "unsigned/";
        String fileName = "9km_aggregate__MODIS_AQUA_L3_SST_THERMAL_8DAY_9KM_DAYTIME.nc";

        //DumpString
        results = NcHelper.ncdump(fileDir + fileName, "-h");
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
"      :scale_factor = 7.17185E-4f; // float\n" + //32768*7.17185E-4 - 2-> 21.50071808, so many values are higher
"      :add_offset = -2.0f; // float\n" +         //65535*7.17185E-4 - 2 = 45.00071898
"      :_FillValue = -1S; // short\n" + //technically wrong: cf says it should be actual value: 65535(int)
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
"      :scale_factor = 7.17185E-4f; // float\n" +
"      :add_offset = -2.0f; // float\n" +
"      :valid_range = 0, 2; // int\n" +
"      :coordinates = \"time Number_of_Lines Number_of_Columns lat lon\";\n" +
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
"}\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //generateDatasetsXml
        //String2.log(generateDatasetsXml(fileDir, fileName, fileDir + fileName,
        //    DEFAULT_RELOAD_EVERY_N_MINUTES, null, null));  //cacheFromUrl
        results = generateDatasetsXml(fileDir, fileName, "", 
            "", //group
            "", -1, "", null);
        expected = 
"<dataset type=\"EDDGridFromNcFiles\" datasetID=\"unsigned_55f5_4ca9_09f2\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>/erddapTest/unsigned/</fileDir>\n" +
"    <fileNameRegex>9km_aggregate__MODIS_AQUA_L3_SST_THERMAL_8DAY_9KM_DAYTIME.nc</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <matchAxisNDigits>20</matchAxisNDigits>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"Conventions\">CF-1.0</att>\n" +
"        <att name=\"Data_Bins\" type=\"int\">14234182</att>\n" +
"        <att name=\"Data_Maximum\" type=\"float\">36.915</att>\n" +
"        <att name=\"Data_Minimum\" type=\"float\">-1.999999</att>\n" +
"        <att name=\"Easternmost_Longitude\" type=\"float\">180.0</att>\n" +
"        <att name=\"End_Day\" type=\"short\">271</att>\n" +
"        <att name=\"End_Millisec\" type=\"int\">10806395</att>\n" +
"        <att name=\"End_Orbit\" type=\"int\">0</att>\n" +
"        <att name=\"End_Time\">2009271030006395</att>\n" +
"        <att name=\"End_Year\" type=\"short\">2009</att>\n" +
"        <att name=\"geospatial_lat_max\" type=\"double\">89.95833587646484</att>\n" +
"        <att name=\"geospatial_lat_min\" type=\"double\">-89.95833587646484</att>\n" +
"        <att name=\"geospatial_lon_max\" type=\"double\">-134.04165649414062</att>\n" +
"        <att name=\"geospatial_lon_min\" type=\"double\">-136.04165649414062</att>\n" +
"        <att name=\"History\">Translated to CF-1.0 Conventions by Netcdf-Java CDM (NetcdfCFWriter)\n" +
"Original Dataset = file:/usr/ftp/ncml/catalog_ncml/OceanTemperature/modis/aqua/11um/9km/aggregate__MODIS_AQUA_L3_SST_THERMAL_8DAY_9KM_DAYTIME.ncml; Translation Date = Fri Oct 30 09:44:07 GMT-08:00 2015</att>\n" +
"        <att name=\"Input_Files\">A20092652009272.L3b_8D_SST.main</att>\n" +
"        <att name=\"Input_Parameters\">IFILE = /data3/sdpsoper/vdc/vpu2/workbuf/A20092652009272.L3b_8D_SST.main|OFILE = A20092652009272.L3m_8D_SST_9|PFILE = |PROD = sst|PALFILE = DEFAULT|RFLAG = ORIGINAL|MEAS = 1|STYPE = 0|DATAMIN = 0.000000|DATAMAX = 0.000000|LONWEST = -180.000000|LONEAST = 180.000000|LATSOUTH = -90.000000|LATNORTH = 90.000000|RESOLUTION = 9km|PROJECTION = RECT|GAP_FILL = 0|SEAM_LON = -180.000000|PRECISION=I</att>\n" +
"        <att name=\"Intercept\" type=\"float\">-2.0</att>\n" +
"        <att name=\"L2_Flag_Names\">LAND,HISOLZ</att>\n" +
"        <att name=\"Latitude_Step\" type=\"float\">0.083333336</att>\n" +
"        <att name=\"Latitude_Units\">degrees North</att>\n" +
"        <att name=\"Longitude_Step\" type=\"float\">0.083333336</att>\n" +
"        <att name=\"Longitude_Units\">degrees East</att>\n" +
"        <att name=\"Map_Projection\">Equidistant Cylindrical</att>\n" +
"        <att name=\"Measure\">Mean</att>\n" +
"        <att name=\"Northernmost_Latitude\" type=\"float\">90.0</att>\n" +
"        <att name=\"Number_of_Columns\" type=\"int\">4320</att>\n" +
"        <att name=\"Number_of_Lines\" type=\"int\">2160</att>\n" +
"        <att name=\"Orbit\" type=\"int\">0</att>\n" +
"        <att name=\"Parameter\">Sea Surface Temperature</att>\n" +
"        <att name=\"Period_End_Day\" type=\"short\">270</att>\n" +
"        <att name=\"Period_End_Year\" type=\"short\">2009</att>\n" +
"        <att name=\"Period_Start_Day\" type=\"short\">265</att>\n" +
"        <att name=\"Period_Start_Year\" type=\"short\">2009</att>\n" +
"        <att name=\"Processing_Control\">smigen par=A20092652009272.L3m_8D_SST_9.param</att>\n" +
"        <att name=\"Processing_Time\">2009282201111000</att>\n" +
"        <att name=\"Product_Name\">A20092652009272.L3m_8D_SST_9</att>\n" +
"        <att name=\"Product_Type\">8-day</att>\n" +
"        <att name=\"Replacement_Flag\">ORIGINAL</att>\n" +
"        <att name=\"Scaled_Data_Maximum\" type=\"float\">45.0</att>\n" +
"        <att name=\"Scaled_Data_Minimum\" type=\"float\">-2.0</att>\n" +
"        <att name=\"Scaling\">linear</att>\n" +
"        <att name=\"Scaling_Equation\">(Slope*l3m_data) + Intercept = Parameter value</att>\n" +
"        <att name=\"Sensor_Name\">MODISA</att>\n" +
"        <att name=\"Slope\" type=\"float\">7.17185E-4</att>\n" +
"        <att name=\"Software_Name\">smigen</att>\n" +
"        <att name=\"Software_Version\">4.0</att>\n" +
"        <att name=\"Southernmost_Latitude\" type=\"float\">-90.0</att>\n" +
"        <att name=\"start_date\">2002-07-04 UTC</att>\n" +
"        <att name=\"Start_Day\" type=\"short\">265</att>\n" +
"        <att name=\"Start_Millisec\" type=\"int\">8779</att>\n" +
"        <att name=\"Start_Orbit\" type=\"int\">0</att>\n" +
"        <att name=\"Start_Time\">2009265000008779</att>\n" +
"        <att name=\"start_time\">00:00:00 UTC</att>\n" +
"        <att name=\"Start_Year\" type=\"short\">2009</att>\n" +
"        <att name=\"Station_Latitude\" type=\"float\">0.0</att>\n" +
"        <att name=\"Station_Longitude\" type=\"float\">0.0</att>\n" +
"        <att name=\"stop_date\">2015-03-06 UTC</att>\n" +
"        <att name=\"stop_time\">23:59:59 UTC</att>\n" +
"        <att name=\"SW_Point_Latitude\" type=\"float\">-89.958336</att>\n" +
"        <att name=\"SW_Point_Longitude\" type=\"float\">-179.95833</att>\n" +
"        <att name=\"Title\">MODISA Level-3 Standard Mapped Image</att>\n" +
"        <att name=\"Units\">deg-C</att>\n" +
"        <att name=\"Westernmost_Longitude\" type=\"float\">-180.0</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"Conventions\">CF-1.6, COARDS, ACDD-1.3</att>\n" +
"        <att name=\"Data_Bins\">null</att>\n" +
"        <att name=\"Data_Maximum\">null</att>\n" +
"        <att name=\"Data_Minimum\">null</att>\n" +
"        <att name=\"Easternmost_Longitude\">null</att>\n" +
"        <att name=\"End_Day\">null</att>\n" +
"        <att name=\"End_Millisec\">null</att>\n" +
"        <att name=\"End_Orbit\">null</att>\n" +
"        <att name=\"End_Time\">null</att>\n" +
"        <att name=\"End_Year\">null</att>\n" +
"        <att name=\"History\">null</att>\n" +
"        <att name=\"history\">Translated to CF-1.0 Conventions by Netcdf-Java CDM (NetcdfCFWriter)\n" +
"Original Dataset = file:/usr/ftp/ncml/catalog_ncml/OceanTemperature/modis/aqua/11um/9km/aggregate__MODIS_AQUA_L3_SST_THERMAL_8DAY_9KM_DAYTIME.ncml; Translation Date = Fri Oct 30 09:44:07 GMT-08:00 2015</att>\n" +
"        <att name=\"infoUrl\">???</att>\n" +
"        <att name=\"Input_Files\">null</att>\n" +
"        <att name=\"institution\">???</att>\n" +
"        <att name=\"Intercept\">null</att>\n" +
"        <att name=\"keywords\">aqua, data, earth, Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature, image, imaging, L3, l3m_data, l3m_qual, mapped, moderate, modis, modisa, ocean, oceans, quality, resolution, science, sea, sea_surface_temperature, smi, spectroradiometer, standard, surface, temperature, time</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"Latitude_Step\">null</att>\n" +
"        <att name=\"Latitude_Units\">null</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"Longitude_Step\">null</att>\n" +
"        <att name=\"Longitude_Units\">null</att>\n" +
"        <att name=\"Northernmost_Latitude\">null</att>\n" +
"        <att name=\"Number_of_Columns\">null</att>\n" +
"        <att name=\"Number_of_Lines\">null</att>\n" +
"        <att name=\"Orbit\">null</att>\n" +
"        <att name=\"Parameter\">null</att>\n" +
"        <att name=\"Period_End_Day\">null</att>\n" +
"        <att name=\"Period_End_Year\">null</att>\n" +
"        <att name=\"Period_Start_Day\">null</att>\n" +
"        <att name=\"Period_Start_Year\">null</att>\n" +
"        <att name=\"Scaling\">null</att>\n" +
"        <att name=\"Scaling_Equation\">null</att>\n" +
"        <att name=\"Slope\">null</att>\n" +
"        <att name=\"Southernmost_Latitude\">null</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" +
"        <att name=\"start_date\">null</att>\n" +
"        <att name=\"Start_Day\">null</att>\n" +
"        <att name=\"Start_Millisec\">null</att>\n" +
"        <att name=\"Start_Orbit\">null</att>\n" +
"        <att name=\"Start_Time\">null</att>\n" +
"        <att name=\"start_time\">null</att>\n" +
"        <att name=\"Start_Year\">null</att>\n" +
"        <att name=\"Station_Latitude\">null</att>\n" +
"        <att name=\"Station_Longitude\">null</att>\n" +
"        <att name=\"stop_date\">null</att>\n" +
"        <att name=\"stop_time\">null</att>\n" +
"        <att name=\"summary\">Moderate Resolution Imaging Spectroradiometer on Aqua (MODISA) Level-3 Standard Mapped Image</att>\n" +
"        <att name=\"SW_Point_Latitude\">null</att>\n" +
"        <att name=\"SW_Point_Longitude\">null</att>\n" +
"        <att name=\"Title\">null</att>\n" +
"        <att name=\"title\">MODISA L3 SMI,</att>\n" +
"        <att name=\"Units\">null</att>\n" +
"        <att name=\"Westernmost_Longitude\">null</att>\n" +
"    </addAttributes>\n" +
"    <axisVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Time</att>\n" +
"            <att name=\"axis\">T</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"units\">days since 2002-01-01</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Time</att>\n" +
"            <att name=\"units\">days since 2002-01-01T00:00:00Z</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>lat</sourceName>\n" +  //correct based on what is in the file, but needs to be Number_of_Lines
"        <destinationName>latitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>lon</sourceName>\n" +  //correct based on what is in the file, but needs to be Number_of_Columns
"        <destinationName>longitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>l3m_data</sourceName>\n" +
"        <destinationName>sst</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"ushort\">65535</att>\n" +
"            <att name=\"add_offset\" type=\"float\">-2.0</att>\n" +
"            <att name=\"coordinates\">time Number_of_Lines Number_of_Columns lat lon</att>\n" +
"            <att name=\"Intercept\" type=\"float\">-2.0</att>\n" +
"            <att name=\"long_name\">l3m_data</att>\n" +
"            <att name=\"scale_factor\" type=\"float\">7.17185E-4</att>\n" +
"            <att name=\"Scaling\">linear</att>\n" +
"            <att name=\"Scaling_Equation\">(Slope*l3m_data) + Intercept = Parameter value</att>\n" +
"            <att name=\"Slope\" type=\"float\">7.17185E-4</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"coordinates\">null</att>\n" +
"            <att name=\"Intercept\">null</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Sea Surface Temperature</att>\n" +
"            <att name=\"Scaling\">null</att>\n" +
"            <att name=\"Scaling_Equation\">null</att>\n" +
"            <att name=\"Slope\">null</att>\n" +
"            <att name=\"standard_name\">sea_surface_temperature</att>\n" +
"            <att name=\"units\">deg_C</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>l3m_qual</sourceName>\n" +
"        <destinationName>sst_quality</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"add_offset\" type=\"float\">-2.0</att>\n" +
"            <att name=\"coordinates\">time Number_of_Lines Number_of_Columns lat lon</att>\n" +
"            <att name=\"long_name\">l3m_qual</att>\n" +
"            <att name=\"scale_factor\" type=\"float\">7.17185E-4</att>\n" +  //this is crazy error in data file. see notes in datasets.xml
"            <att name=\"valid_range\" type=\"uintList\">0 2</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"ubyte\">255</att>\n" + //important test of addMvFvAttsIfNeeded
"            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"coordinates\">null</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"            <att name=\"long_name\">Sea Surface Temperature Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n";
        Test.ensureEqual(results, expected, "results=\n" + results);


        //ensure files are reread
        File2.deleteAllFiles(datasetDir("testUInt16File"));
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, "testUInt16File"); 
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
"    Float32 actual_range -89.95834, 89.95834;\n" +
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
"    Float32 _FillValue 45.000717;\n" +   //important test of UInt16. Or should it be 65535*7.17185E-4 - 2 = 45.00071898 ?   2020-08-04 changed from NaN with change to maxIsMV support
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Sea Surface Temperature\";\n" +
"    String standard_name \"sea_surface_temperature\";\n" +
"    String units \"deg_C\";\n" +
"  }\n" +
"  sst_quality {\n" +
"    String _Unsigned \"true\";\n" + //important test of _Unsigned
"    Float64 colorBarMaximum 150.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Sea Surface Temperature Quality\";\n" +
"    Byte missing_value -1;\n" + //important test of _Unsigned
"    Byte valid_range 0, 2;\n" + //important test: ubyte internally -> byte in DAP which doesn't support ubyte (acually, dap byte is unsigned!)
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
"    String Input_Parameters \"IFILE = /data3/sdpsoper/vdc/vpu2/workbuf/A20092652009272.L3b_8D_SST.main|OFILE = A20092652009272.L3m_8D_SST_9|PFILE = |PROD = sst|PALFILE = DEFAULT|RFLAG = ORIGINAL|MEAS = 1|STYPE = 0|DATAMIN = 0.000000|DATAMAX = 0.000000|LONWEST = -180.000000|LONEAST = 180.000000|LATSOUTH = -90.000000|LATNORTH = 90.000000|RESOLUTION = 9km|PROJECTION = RECT|GAP_FILL = 0|SEAM_LON = -180.000000|PRECISION=I\";\n" +
"    String institution \"???\";\n" +
"    String keywords \"aqua, data, earth, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, image, imaging, L3, l3m_data, l3m_qual, mapped, moderate, modis, modisa, ocean, oceans, quality, resolution, science, sea, sea_surface_temperature, smi, spectroradiometer, standard, surface, temperature, time\";\n" +
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
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
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
        results = String2.directReadFrom88591File(
            EDStatic.fullTestCacheDirectory + tName);
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
"      Byte sst_quality[time = 1][latitude = 2160][longitude = 25];\n" +
"    MAPS:\n" +
"      Float64 time[time = 1];\n" +
"      Float32 latitude[latitude = 2160];\n" +
"      Float32 longitude[longitude = 25];\n" +
"  } sst_quality;\n" +
"} testUInt16File;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv data values
        userDapQuery = "sst[0][0:100:2159][(-134.95833513)],sst_quality[0][0:100:2159][(-134.95833513)]"; 
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, eddGrid.className(), ".csv"); 
        results = String2.directReadFrom88591File(
            EDStatic.fullTestCacheDirectory + tName);
        String2.log(results);
        expected = //difference from testUInt16Dap: lat lon are float here, not double
"time,latitude,longitude,sst,sst_quality\n" +
"UTC,degrees_north,degrees_east,deg_C,\n" +
"2002-07-04T00:00:00Z,89.958336,-134.95833,-0.84102905,1\n" +
"2002-07-04T00:00:00Z,81.62501,-134.95833,-1.6371044,0\n" +
"2002-07-04T00:00:00Z,73.291664,-134.95833,-0.11021753,0\n" +
"2002-07-04T00:00:00Z,64.958336,-134.95833,NaN,NaN\n" + //!!!quality would be 255, but I added missing_value=255, so it appears as NaN
"2002-07-04T00:00:00Z,56.625008,-134.95833,NaN,NaN\n" + //  but should it be "NaN" 
"2002-07-04T00:00:00Z,48.291664,-134.95833,12.6406145,0\n" +
"2002-07-04T00:00:00Z,39.958336,-134.95833,17.95137,0\n" +
"2002-07-04T00:00:00Z,31.625,-134.95833,20.432829,0\n" +
"2002-07-04T00:00:00Z,23.291664,-134.95833,19.664007,2\n" +
"2002-07-04T00:00:00Z,14.958336,-134.95833,24.482773,0\n" +
"2002-07-04T00:00:00Z,6.625,-134.95833,29.068455,0\n" +
"2002-07-04T00:00:00Z,-1.7083359,-134.95833,27.240349,0\n" +
"2002-07-04T00:00:00Z,-10.041664,-134.95833,27.210228,0\n" +
"2002-07-04T00:00:00Z,-18.375,-134.95833,26.713936,0\n" +
"2002-07-04T00:00:00Z,-26.708336,-134.95833,21.580326,0\n" +
"2002-07-04T00:00:00Z,-35.041668,-134.95833,15.789774,0\n" +
"2002-07-04T00:00:00Z,-43.375,-134.95833,NaN,NaN\n" +
"2002-07-04T00:00:00Z,-51.708336,-134.95833,6.1673026,2\n" +
"2002-07-04T00:00:00Z,-60.041668,-134.95833,0.40400413,0\n" +
"2002-07-04T00:00:00Z,-68.375,-134.95833,NaN,NaN\n" +
"2002-07-04T00:00:00Z,-76.708336,-134.95833,NaN,NaN\n" +
"2002-07-04T00:00:00Z,-85.04167,-134.95833,NaN,NaN\n"; 
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //write to nc3 then ncump
        userDapQuery = "sst[0][0:100:2159][(-134.95833513)],sst_quality[0][0:100:2159][(-134.95833513)]"; 
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, eddGrid.className(), ".nc"); 
        results = NcHelper.ncdump(EDStatic.fullTestCacheDirectory + tName, "");
        String2.log(results);
        expected = //difference from testUInt16Dap: lat lon are float here, not double
"netcdf EDDGridFromNcFiles.nc {\n" +
"  dimensions:\n" +
"    time = 1;\n" +
"    latitude = 22;\n" +
"    longitude = 1;\n" +
"  variables:\n" +
"    double time(time=1);\n" +
"      :_CoordinateAxisType = \"Time\";\n" +
"      :actual_range = 1.0257408E9, 1.0257408E9; // double\n" +
"      :axis = \"T\";\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Time\";\n" +
"      :standard_name = \"time\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    float latitude(latitude=22);\n" +
"      :_CoordinateAxisType = \"Lat\";\n" +
"      :actual_range = -85.04167f, 89.958336f; // float\n" +
"      :axis = \"Y\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Latitude\";\n" +
"      :standard_name = \"latitude\";\n" +
"      :units = \"degrees_north\";\n" +
"\n" +
"    float longitude(longitude=1);\n" +
"      :_CoordinateAxisType = \"Lon\";\n" +
"      :actual_range = -134.95833f, -134.95833f; // float\n" +
"      :axis = \"X\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Longitude\";\n" +
"      :standard_name = \"longitude\";\n" +
"      :units = \"degrees_east\";\n" +
"\n" +
"    float sst(time=1, latitude=22, longitude=1);\n" +
"      :_FillValue = 45.000717f; // float\n" +
"      :colorBarMaximum = 32.0; // double\n" +
"      :colorBarMinimum = 0.0; // double\n" +
"      :ioos_category = \"Temperature\";\n" +
"      :long_name = \"Sea Surface Temperature\";\n" +
"      :standard_name = \"sea_surface_temperature\";\n" +
"      :units = \"deg_C\";\n" +
"\n" +
"    byte sst_quality(time=1, latitude=22, longitude=1);\n" +
"      :_Unsigned = \"true\";\n" +        //technically, dap bytes are unsigned, but historically thredds and erddap treat as signed
"      :colorBarMaximum = 150.0; // double\n" +
"      :colorBarMinimum = 0.0; // double\n" +
"      :ioos_category = \"Quality\";\n" +
"      :long_name = \"Sea Surface Temperature Quality\";\n" +
"      :missing_value = -1B; // byte\n" + //technically, dap bytes are unsigned, but historically thredds and erddap treat as signed
"      :valid_range = 0B, 2B; // byte\n" +
"\n" +
"  // global attributes:\n" +
"  :cdm_data_type = \"Grid\";\n" +
"  :Conventions = \"CF-1.6, COARDS, ACDD-1.3\";\n" +
"  :Easternmost_Easting = -134.95833f; // float\n" +
"  :geospatial_lat_max = 89.958336f; // float\n" +
"  :geospatial_lat_min = -85.04167f; // float\n" +
"  :geospatial_lat_units = \"degrees_north\";\n" +
"  :geospatial_lon_max = -134.95833f; // float\n" +
"  :geospatial_lon_min = -134.95833f; // float\n" +
"  :geospatial_lon_resolution = 0.08333333333333333; // double\n" +
"  :geospatial_lon_units = \"degrees_east\";\n" +
"  :history = \"Translated to CF-1.0 Conventions by Netcdf-Java CDM (NetcdfCFWriter)\n" +
"Original Dataset = file:/usr/ftp/ncml/catalog_ncml/OceanTemperature/modis/aqua/11um/9km/aggregate__MODIS_AQUA_L3_SST_THERMAL_8DAY_9KM_DAYTIME.ncml; Translation Date = Fri Oct 30 09:44:07 GMT-08:00 2015\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

//"2020-06-11T16:26:03Z (local files)\n" +
//"2020-06-11T16:26:03Z 
expected = 
"http://localhost:8080/cwexperimental/griddap/testUInt16File.nc?sst[0][0:100:2159][(-134.95833513)],sst_quality[0][0:100:2159][(-134.95833513)]\";\n" +
"  :infoUrl = \"???\";\n" +
"  :Input_Parameters = \"IFILE = /data3/sdpsoper/vdc/vpu2/workbuf/A20092652009272.L3b_8D_SST.main|OFILE = A20092652009272.L3m_8D_SST_9|PFILE = |PROD = sst|PALFILE = DEFAULT|RFLAG = ORIGINAL|MEAS = 1|STYPE = 0|DATAMIN = 0.000000|DATAMAX = 0.000000|LONWEST = -180.000000|LONEAST = 180.000000|LATSOUTH = -90.000000|LATNORTH = 90.000000|RESOLUTION = 9km|PROJECTION = RECT|GAP_FILL = 0|SEAM_LON = -180.000000|PRECISION=I\";\n" +
"  :institution = \"???\";\n" +
"  :keywords = \"aqua, data, earth, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, image, imaging, L3, l3m_data, l3m_qual, mapped, moderate, modis, modisa, ocean, oceans, quality, resolution, science, sea, sea_surface_temperature, smi, spectroradiometer, standard, surface, temperature, time\";\n" +
"  :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
"  :L2_Flag_Names = \"LAND,HISOLZ\";\n" +
"  :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"  :Map_Projection = \"Equidistant Cylindrical\";\n" +
"  :Measure = \"Mean\";\n" +
"  :Northernmost_Northing = 89.958336f; // float\n" +
"  :Processing_Control = \"smigen par=A20092652009272.L3m_8D_SST_9.param\";\n" +
"  :Processing_Time = \"2009282201111000\";\n" +
"  :Product_Name = \"A20092652009272.L3m_8D_SST_9\";\n" +
"  :Product_Type = \"8-day\";\n" +
"  :Replacement_Flag = \"ORIGINAL\";\n" +
"  :Scaled_Data_Maximum = 45.0f; // float\n" +
"  :Scaled_Data_Minimum = -2.0f; // float\n" +
"  :Sensor_Name = \"MODISA\";\n" +
"  :Software_Name = \"smigen\";\n" +
"  :Software_Version = \"4.0\";\n" +
"  :sourceUrl = \"(local files)\";\n" +
"  :Southernmost_Northing = -85.04167f; // float\n" +
"  :standard_name_vocabulary = \"CF Standard Name Table v70\";\n" +
"  :summary = \"Moderate Resolution Imaging Spectroradiometer on Aqua (MODISA) Level-3 Standard Mapped Image\";\n" +
"  :time_coverage_end = \"2002-07-04T00:00:00Z\";\n" +
"  :time_coverage_start = \"2002-07-04T00:00:00Z\";\n" +
"  :title = \"MODISA L3 SMI,\";\n" +
"  :Westernmost_Easting = -134.95833f; // float\n" +
"\n" +
"  data:\n" +
"    time = \n" +
"      {1.0257408E9}\n" +
"    latitude = \n" +
"      {89.958336, 81.62501, 73.291664, 64.958336, 56.625008, 48.291664, 39.958336, 31.625, 23.291664, 14.958336, 6.625, -1.7083359, -10.041664, -18.375, -26.708336, -35.041668, -43.375, -51.708336, -60.041668, -68.375, -76.708336, -85.04167}\n" +
"    longitude = \n" +
"      {-134.95833}\n" +
"    sst = \n" +
"      {\n" +
"        {\n" +
"          {-0.84102905},\n" +
"          {-1.6371044},\n" +
"          {-0.11021753},\n" +
"          {45.000717},\n" +
"          {45.000717},\n" +
"          {12.6406145},\n" +
"          {17.95137},\n" +
"          {20.432829},\n" +
"          {19.664007},\n" +
"          {24.482773},\n" +
"          {29.068455},\n" +
"          {27.240349},\n" +
"          {27.210228},\n" +
"          {26.713936},\n" +
"          {21.580326},\n" +
"          {15.789774},\n" +
"          {45.000717},\n" +
"          {6.1673026},\n" +
"          {0.40400413},\n" +
"          {45.000717},\n" +
"          {45.000717},\n" +
"          {45.000717}\n" +
"        }\n" +
"      }\n" +
"    sst_quality = \n" +
"      {\n" +
"        {\n" +
"          {1},\n" +
"          {0},\n" +
"          {0},\n" +
"          {-1},\n" +
"          {-1},\n" +
"          {0},\n" +
"          {0},\n" +
"          {0},\n" +
"          {2},\n" +
"          {0},\n" +
"          {0},\n" +
"          {0},\n" +
"          {0},\n" +
"          {0},\n" +
"          {0},\n" +
"          {0},\n" +
"          {-1},\n" +
"          {2},\n" +
"          {0},\n" +
"          {-1},\n" +
"          {-1},\n" +
"          {-1}\n" +
"        }\n" +
"      }\n" +
"}\n"; 
        int po = results.indexOf(expected.substring(0, 50));
        Test.ensureEqual(results.substring(po), expected, "\nresults=\n" + results);

        //display the image
        String2.log("\n\n* PNG ");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "sst[0][][]&.land=under", 
            EDStatic.fullTestCacheDirectory, eddGrid.className() + "_UInt16_Map", ".png"); 
        SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

    }

    /**
     * This tests special axis0 with time.
     *
     * @throws Throwable if trouble
     */
    public static void testSpecialAxis0Time() throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testSpecialAxis0Time()\n");
        testVerboseOn();

        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDVGridAxis edvga;
        String id = "erdSW1chlamday";
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, id); 
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
        String tDir = EDStatic.fullTestCacheDirectory;
        String testName = "EDDGridFromNcFiles_Axis0Time";

        //*** test getting das for entire dataset
        String2.log("\n*** .nc test das dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", 
            tDir, testName + "_Entire", ".das"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 8.849088e+8, 8.875872e+8;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Centered Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range -89.95834, 89.95834;\n" + //test of descending lat axis
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
"    Float32 actual_range -179.9583, 179.9584;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"    Float32 valid_max 180.0;\n" +
"    Float32 valid_min -180.0;\n" +
"  }\n" +
"  chlorophyll {\n" +
"    Float32 _FillValue -32767.0;\n" +
"    Float64 colorBarMaximum 30.0;\n" +
"    Float64 colorBarMinimum 0.03;\n" +
"    String colorBarScale \"Log\";\n" +
"    String ioos_category \"Ocean Color\";\n" +
"    String long_name \"Chlorophyll Concentration, OCI Algorithm\";\n" +
"    String references \"Hu, C., Lee Z., and Franz, B.A. (2012). Chlorophyll-a algorithms for oligotrophic oceans: A novel approach based on three-band reflectance difference, J. Geophys. Res., 117, C01011, doi:10.1029/2011JC007395.\";\n" +
"    String standard_name \"concentration_of_chlorophyll_in_sea_water\";\n" +
"    String units \"mg m^-3\";\n" +
"    Float32 valid_max 100.0;\n" +
"    Float32 valid_min 0.001;\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String _lastModified \"2015-10-02T00:07:10.000Z\";\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String Conventions \"CF-1.6, COARDS, ACDD-1.3\";\n" +
"    String creator_email \"data@oceancolor.gsfc.nasa.gov\";\n" +
"    String creator_name \"NASA/GSFC/OBPG\";\n" +
"    String creator_type \"group\";\n" +
"    String creator_url \"https://oceandata.sci.gsfc.nasa.gov\";\n" +
"    String date_created \"2015-10-02T00:07:10.000Z\";\n" +
"    Float64 Easternmost_Easting 179.9584;\n" +
"    Float64 geospatial_lat_max 89.95834;\n" +
"    Float64 geospatial_lat_min -89.95834;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 179.9584;\n" +
"    Float64 geospatial_lon_min -179.9583;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String grid_mapping_name \"latitude_longitude\";\n" +
"    String history \"smigen par=S19980011998031.L3m_MO_CHL_chlor_a_9km.nc.param\n" +
today; // (local files)
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);

//            + " https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/ux10/1day\n" +
//today + 

expected = 
//    "    String id "S19980011998031.L3b_MO_CHL.nc/L3/S19980011998031.L3b_MO_CHL.nc";
"    String identifier_product_doi \"https://dx.doi.org\";\n" +
"    String identifier_product_doi_authority \"https://dx.doi.org\";\n" +
"    String infoUrl \"https://oceancolor.gsfc.nasa.gov/data/seawifs/\";\n" +
"    String institution \"NASA/GSFC OBPG\";\n" +
"    String instrument \"SeaWiFS\";\n" +
"    String keywords \"algorithm, biology, center, chemistry, chlor_a, chlorophyll, color, concentration, concentration_of_chlorophyll_in_sea_water, data, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, Earth Science > Oceans > Ocean Optics > Ocean Color, field, field-of-view, flight, goddard, group, gsfc, image, L3, level, level-3, mapped, nasa, noaa, obpg, ocean, ocean color, oceans, oci, optics, orbview, orbview-2, palette, processing, sea, sea-wide, seawater, seawifs, sensor, smi, space, standard, view, water, wide\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String l2_flag_names \"ATMFAIL,LAND,HILT,HISATZEN,STRAYLIGHT,CLDICE,COCCOLITH,LOWLW,CHLWARN,CHLFAIL,NAVWARN,MAXAERITER,ATMWARN,HISOLZEN,NAVFAIL,FILTER,HIGLINT\";\n" +
"    String license \"https://science.nasa.gov/earth-science/earth-science-data/data-information-policy/\n" +
"\n" +
"Please cite: NASA Goddard Space Flight Center, Ocean Ecology Laboratory, Ocean Biology Processing Group; (2014): SeaWiFS Ocean Color Data; NASA Goddard Space Flight Center, Ocean Ecology Laboratory, Ocean Biology Processing Group. https://dx.doi.org/10.5067/ORBVIEW-2/SEAWIFS_OC.2014.0\n" +
"\n" +
"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String map_projection \"Equidistant Cylindrical\";\n" +
"    String measure \"Mean\";\n" +
"    String naming_authority \"gov.noaa.pfeg.coastwatch\";\n" +
"    Float64 Northernmost_Northing 89.95834;\n" +
"    String platform \"Orbview-2\";\n" +
"    String processing_level \"L3 Mapped\";\n" +
"    String processing_version \"2014.0\";\n" +
"    String product_name \"S19980011998031.L3m_MO_CHL_chlor_a_9km.nc\";\n" +
"    String project \"Ocean Biology Processing Group (NASA/GSFC/OBPG)\";\n" +
"    String publisher_email \"data@oceancolor.gsfc.nasa.gov\";\n" +
"    String publisher_name \"NASA/GSFC/OBPG\";\n" +
"    String publisher_url \"https://oceandata.sci.gsfc.nasa.gov\";\n" +
"    String references \"SeaWiFS information: https://oceancolor.gsfc.nasa.gov/SeaWiFS/ . NASA Ocean\n" +
"Color information: https://oceancolor.gsfc.nasa.gov/\n" +
"Processing reference: O'Reilly, J.E., Maritorena, S., Mitchell, B.G., Siegel, D.A., Carder, K.L., Garver, S.A., Kahru, M. and McClain, C. (1998). Ocean color chlorophyll algorithms for SeaWiFS. J. Geophys. Res., 103: 24, 937-24, 953.\n" +
"Processing reference: O'Reilly, J. E., and 21 others. 2000. Ocean color chlorophyll a algorithms for SeaWiFS, OC2 and OC4: Version 4. SeaWiFS Postlaunch Calibration and Validation Analyses, part 3. NASA SeaWiFS technical report series. pp. 8 226 22.\n" +
"Processing reference: Fu, G., Baith, K. S., and McClain, C. R. (1998). SeaDAS: The SeaWiFS Data Analysis System. Proceedings of \\\"The 4th Pacific Ocean Remote Sensing Conference\\\", Qingdao, China, July 28-31, 1998, 73-79.\n" +
"Validation reference: Hooker, S.B., and C.R. McClain (2000). The Calibration and Validation of SeaWiFS Data. Prog. Oceanogr., 45, 427-465.\n" +
"R2014.0 processing reference: Hu, C., Lee Z., and Franz, B.A. (2012). Chlorophyll-a algorithms for oligotrophic oceans: A novel approach based on three-band reflectance difference, J. Geophys. Res., 117, C01011, doi:10.1029/2011JC007395.\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing -89.95834;\n" +
"    String spatialResolution \"9.20 km\";\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String summary \"THIS VERSION IS DEPRECATED. SEE THE NEW R2018.0 VERSION IN erdSW2018chlamday. (Feb 2018)\n" +
"\n" +
"NASA GSFC Ocean Color Web distributes science-quality chlorophyll-a\n" +
"concentration data from the Sea-viewing Wide Field-of-view Sensor (SeaWiFS)\n" +
"on the Orbview-2 satellite. This version is the 2014.0 Reprocessing (R2014.0).\n" +
"\n" +
"The SeaWiFS instrument was launched by Orbital Sciences Corporation on the\n" +
"OrbView-2 (a.k.a. SeaStar) satellite in August 1997, and collected data from\n" +
"September 1997 until the end of mission in December 2010. SeaWiFS had 8\n" +
"spectral bands from 412 to 865 nm. It collected global data at 4 km\n" +
"resolution, and local data (limited onboard storage and direct broadcast)\n" +
"at 1 km. The mission and sensor were optimized for ocean color measurements,\n" +
"with a local noon (descending) equator crossing time orbit, fore-and-aft\n" +
"tilt capability, full dynamic range, and low polarization sensitivity.\";\n" +
"    String temporal_range \"month\";\n" +
"    String time_coverage_end \"1998-02-16T00:00:00Z\";\n" +
"    String time_coverage_start \"1998-01-16T00:00:00Z\";\n" +
"    String title \"Chlorophyll-a, Orbview-2 SeaWiFS, R2014.0, 0.1\u00b0, Global, 1997-2010 (Monthly Composite) DEPRECATED\";\n" +
"    Float64 Westernmost_Easting -179.9583;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 25));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", 
            tDir, testName + "_Entire", ".dds"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Float64 time[time = 2];\n" +
"  Float32 latitude[latitude = 2160];\n" +
"  Float32 longitude[longitude = 4320];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 chlorophyll[time = 2][latitude = 2160][longitude = 4320];\n" +
"    MAPS:\n" +
"      Float64 time[time = 2];\n" +
"      Float32 latitude[latitude = 2160];\n" +
"      Float32 longitude[longitude = 4320];\n" +
"  } chlorophyll;\n" +
"} erdSW1chlamday;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv  with data from one file
        String2.log("\n*** .nc test read from one file\n");       
        userDapQuery = "chlorophyll[(1998-02-16T00:00:00Z)][(38):4:(37)][(-123.6):4:(-122.6)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            tDir, testName + "_Data1", ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"time,latitude,longitude,chlorophyll\n" +
"UTC,degrees_north,degrees_east,mg m^-3\n" +
"1998-02-16T00:00:00Z,38.041664,-123.625,0.321632\n" +
"1998-02-16T00:00:00Z,38.041664,-123.291664,2.886503\n" +
"1998-02-16T00:00:00Z,38.041664,-122.958336,3.058165\n" +
"1998-02-16T00:00:00Z,38.041664,-122.625,NaN\n" +
"1998-02-16T00:00:00Z,37.708332,-123.625,0.300352\n" +
"1998-02-16T00:00:00Z,37.708332,-123.291664,0.374681\n" +
"1998-02-16T00:00:00Z,37.708332,-122.958336,3.047059\n" +
"1998-02-16T00:00:00Z,37.708332,-122.625,4.57632\n" +
"1998-02-16T00:00:00Z,37.374996,-123.625,0.454626\n" +
"1998-02-16T00:00:00Z,37.374996,-123.291664,0.373263\n" +
"1998-02-16T00:00:00Z,37.374996,-122.958336,0.810264\n" +
"1998-02-16T00:00:00Z,37.374996,-122.625,2.5943\n" +
"1998-02-16T00:00:00Z,37.041664,-123.625,0.32735\n" +
"1998-02-16T00:00:00Z,37.041664,-123.291664,0.384492\n" +
"1998-02-16T00:00:00Z,37.041664,-122.958336,0.444622\n" +
"1998-02-16T00:00:00Z,37.041664,-122.625,1.436202\n";         
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //img
        tName = eddGrid.makeNewFileForDapQuery(null, null, 
            "chlorophyll[(1998-02-16T00:00:00Z)][(90):(-90)][(-180):(180)]",
            tDir, testName + "_img", ".png"); 
        SSR.displayInBrowser("file://" + tDir + tName);
    }


    /**
     * This tests special axis0 with an integer from the fileName.
     *
     * @throws Throwable if trouble
     */
    public static void testSpecialAxis0FileNameInt() throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testSpecialAxis0FileNameInt()\n");
        testVerboseOn();

        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDVGridAxis edvga;
        String id = "testSpecialAxis0FileNameInt";
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, id); 
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
        String tDir = EDStatic.fullTestCacheDirectory;
        String testName = "EDDGridFromNcFiles_Axis0FileNameInt";

        //*** test getting das for entire dataset
        String2.log("\n*** .nc test das dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", 
            tDir, testName + "_Entire", ".das"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"Attributes {\n" +
"  myInt {\n" +
"    Int32 actual_range 1, 32;\n" +
"    String ioos_category \"Other\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range -89.95834, 89.95834;\n" + //test of descending lat axis
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
"    Float32 actual_range -179.9583, 179.9584;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"    Float32 valid_max 180.0;\n" +
"    Float32 valid_min -180.0;\n" +
"  }\n" +
"  chlorophyll {\n" +
"    Float32 _FillValue -32767.0;\n" +
"    Float64 colorBarMaximum 30.0;\n" +
"    Float64 colorBarMinimum 0.03;\n" +
"    String colorBarScale \"Log\";\n" +
"    String ioos_category \"Ocean Color\";\n" +
"    String long_name \"Chlorophyll Concentration, OCI Algorithm\";\n" +
"    String references \"Hu, C., Lee Z., and Franz, B.A. (2012). Chlorophyll-a algorithms for oligotrophic oceans: A novel approach based on three-band reflectance difference, J. Geophys. Res., 117, C01011, doi:10.1029/2011JC007395.\";\n" +
"    String standard_name \"concentration_of_chlorophyll_in_sea_water\";\n" +
"    String units \"mg m^-3\";\n" +
"    Float32 valid_max 100.0;\n" +
"    Float32 valid_min 0.001;\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String _lastModified \"2015-10-02T00:07:10.000Z\";\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String Conventions \"CF-1.6, COARDS, ACDD-1.3\";\n" +
"    String creator_email \"data@oceancolor.gsfc.nasa.gov\";\n" +
"    String creator_name \"NASA/GSFC/OBPG\";\n" +
"    String creator_type \"group\";\n" +
"    String creator_url \"https://oceandata.sci.gsfc.nasa.gov\";\n" +
"    String date_created \"2015-10-02T00:07:10.000Z\";\n" +
"    Float64 Easternmost_Easting 179.9584;\n" +
"    Float64 geospatial_lat_max 89.95834;\n" +
"    Float64 geospatial_lat_min -89.95834;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 179.9584;\n" +
"    Float64 geospatial_lon_min -179.9583;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String grid_mapping_name \"latitude_longitude\";\n" +
"    String history \"smigen par=S19980011998031.L3m_MO_CHL_chlor_a_9km.nc.param\n" +
today; // (local files)
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);

//            + " https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/ux10/1day\n" +
//today + 

expected = 
//    "    String id "S19980011998031.L3b_MO_CHL.nc/L3/S19980011998031.L3b_MO_CHL.nc";
"    String identifier_product_doi \"https://dx.doi.org\";\n" +
"    String identifier_product_doi_authority \"https://dx.doi.org\";\n" +
"    String infoUrl \"https://oceandata.sci.gsfc.nasa.gov\";\n" +
"    String institution \"NASA/GSFC OBPG\";\n" +
"    String instrument \"SeaWiFS\";\n" +
"    String keywords \"algorithm, biology\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String l2_flag_names \"ATMFAIL,LAND,HILT,HISATZEN,STRAYLIGHT,CLDICE,COCCOLITH,LOWLW,CHLWARN,CHLFAIL,NAVWARN,MAXAERITER,ATMWARN,HISOLZEN,NAVFAIL,FILTER,HIGLINT\";\n" +
"    String license \"https://science.nasa.gov/earth-science/earth-science-data/data-information-policy/\n" +
"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String map_projection \"Equidistant Cylindrical\";\n" +
"    String measure \"Mean\";\n" +
"    String naming_authority \"gov.nasa.gsfc.sci.oceandata\";\n" +
"    Float64 Northernmost_Northing 89.95834;\n" +
"    String platform \"Orbview-2\";\n" +
"    String processing_level \"L3 Mapped\";\n" +
"    String processing_version \"2014.0\";\n" +
"    String product_name \"S19980011998031.L3m_MO_CHL_chlor_a_9km.nc\";\n" +
"    String project \"Ocean Biology Processing Group (NASA/GSFC/OBPG)\";\n" +
"    String publisher_email \"data@oceancolor.gsfc.nasa.gov\";\n" +
"    String publisher_name \"NASA/GSFC/OBPG\";\n" +
"    String publisher_url \"https://oceandata.sci.gsfc.nasa.gov\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing -89.95834;\n" +
"    String spatialResolution \"9.20 km\";\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String summary \"summary of testSpecialAxis0FileNameInt\";\n" +
"    String temporal_range \"month\";\n" +
"    String title \"title of testSpecialAxis0FileNameInt\";\n" +
"    Float64 Westernmost_Easting -179.9583;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 25));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", 
            tDir, testName + "_Entire", ".dds"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Int32 myInt[myInt = 2];\n" +
"  Float32 latitude[latitude = 2160];\n" +
"  Float32 longitude[longitude = 4320];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 chlorophyll[myInt = 2][latitude = 2160][longitude = 4320];\n" +
"    MAPS:\n" +
"      Int32 myInt[myInt = 2];\n" +
"      Float32 latitude[latitude = 2160];\n" +
"      Float32 longitude[longitude = 4320];\n" +
"  } chlorophyll;\n" +
"} testSpecialAxis0FileNameInt;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv  with int values
        String2.log("\n*** .nc test get myInt[]\n");       
        userDapQuery = "myInt";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            tDir, testName + "_axis0", ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"myInt\n" +
"m\n" +
"1\n" +
"32\n";         
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv  with data from one file
        String2.log("\n*** .nc test read from one file\n");       
        userDapQuery = "chlorophyll[(32)][(38):4:(37)][(-123.6):4:(-122.6)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            tDir, testName + "_Data1", ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"myInt,latitude,longitude,chlorophyll\n" +
"m,degrees_north,degrees_east,mg m^-3\n" +
"32,38.041664,-123.625,0.321632\n" +
"32,38.041664,-123.291664,2.886503\n" +
"32,38.041664,-122.958336,3.058165\n" +
"32,38.041664,-122.625,NaN\n" +
"32,37.708332,-123.625,0.300352\n" +
"32,37.708332,-123.291664,0.374681\n" +
"32,37.708332,-122.958336,3.047059\n" +
"32,37.708332,-122.625,4.57632\n" +
"32,37.374996,-123.625,0.454626\n" +
"32,37.374996,-123.291664,0.373263\n" +
"32,37.374996,-122.958336,0.810264\n" +
"32,37.374996,-122.625,2.5943\n" +
"32,37.041664,-123.625,0.32735\n" +
"32,37.041664,-123.291664,0.384492\n" +
"32,37.041664,-122.958336,0.444622\n" +
"32,37.041664,-122.625,1.436202\n";         
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //img
        tName = eddGrid.makeNewFileForDapQuery(null, null, 
            "chlorophyll[(32)][(90):(-90)][(-180):(180)]",
            tDir, testName + "_img", ".png"); 
        SSR.displayInBrowser("file://" + tDir + tName);
    }

    /**
     * This tests special axis0 with an integer from the fullFileName.
     *
     * @throws Throwable if trouble
     */
    public static void testSpecialAxis0PathNameInt() throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testSpecialAxis0PathNameInt()\n");
        testVerboseOn();

        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDVGridAxis edvga;
        String id = "testSpecialAxis0PathNameInt";
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, id); 
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
        String tDir = EDStatic.fullTestCacheDirectory;
        String testName = "EDDGridFromNcFiles_Axis0PathNameInt";

        //*** test getting das for entire dataset
        String2.log("\n*** .nc test das dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", 
            tDir, testName + "_Entire", ".das"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"Attributes {\n" +
"  myInt {\n" +
"    Int32 actual_range 1, 32;\n" +
"    String ioos_category \"Other\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range -89.95834, 89.95834;\n" + //test of descending lat axis
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
"    Float32 actual_range -179.9583, 179.9584;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"    Float32 valid_max 180.0;\n" +
"    Float32 valid_min -180.0;\n" +
"  }\n" +
"  chlorophyll {\n" +
"    Float32 _FillValue -32767.0;\n" +
"    Float64 colorBarMaximum 30.0;\n" +
"    Float64 colorBarMinimum 0.03;\n" +
"    String colorBarScale \"Log\";\n" +
"    String ioos_category \"Ocean Color\";\n" +
"    String long_name \"Chlorophyll Concentration, OCI Algorithm\";\n" +
"    String references \"Hu, C., Lee Z., and Franz, B.A. (2012). Chlorophyll-a algorithms for oligotrophic oceans: A novel approach based on three-band reflectance difference, J. Geophys. Res., 117, C01011, doi:10.1029/2011JC007395.\";\n" +
"    String standard_name \"concentration_of_chlorophyll_in_sea_water\";\n" +
"    String units \"mg m^-3\";\n" +
"    Float32 valid_max 100.0;\n" +
"    Float32 valid_min 0.001;\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String _lastModified \"2015-10-02T00:07:10.000Z\";\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String Conventions \"CF-1.6, COARDS, ACDD-1.3\";\n" +
"    String creator_email \"data@oceancolor.gsfc.nasa.gov\";\n" +
"    String creator_name \"NASA/GSFC/OBPG\";\n" +
"    String creator_type \"group\";\n" +
"    String creator_url \"https://oceandata.sci.gsfc.nasa.gov\";\n" +
"    String date_created \"2015-10-02T00:07:10.000Z\";\n" +
"    Float64 Easternmost_Easting 179.9584;\n" +
"    Float64 geospatial_lat_max 89.95834;\n" +
"    Float64 geospatial_lat_min -89.95834;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 179.9584;\n" +
"    Float64 geospatial_lon_min -179.9583;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String grid_mapping_name \"latitude_longitude\";\n" +
"    String history \"smigen par=S19980011998031.L3m_MO_CHL_chlor_a_9km.nc.param\n" +
today; // (local files)
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);

//            + " https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/ux10/1day\n" +
//today + 

expected = 
//    "    String id "S19980011998031.L3b_MO_CHL.nc/L3/S19980011998031.L3b_MO_CHL.nc";
"    String identifier_product_doi \"https://dx.doi.org\";\n" +
"    String identifier_product_doi_authority \"https://dx.doi.org\";\n" +
"    String infoUrl \"https://oceandata.sci.gsfc.nasa.gov\";\n" +
"    String institution \"NASA/GSFC OBPG\";\n" +
"    String instrument \"SeaWiFS\";\n" +
"    String keywords \"algorithm, biology\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String l2_flag_names \"ATMFAIL,LAND,HILT,HISATZEN,STRAYLIGHT,CLDICE,COCCOLITH,LOWLW,CHLWARN,CHLFAIL,NAVWARN,MAXAERITER,ATMWARN,HISOLZEN,NAVFAIL,FILTER,HIGLINT\";\n" +
"    String license \"https://science.nasa.gov/earth-science/earth-science-data/data-information-policy/\n" +
"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String map_projection \"Equidistant Cylindrical\";\n" +
"    String measure \"Mean\";\n" +
"    String naming_authority \"gov.nasa.gsfc.sci.oceandata\";\n" +
"    Float64 Northernmost_Northing 89.95834;\n" +
"    String platform \"Orbview-2\";\n" +
"    String processing_level \"L3 Mapped\";\n" +
"    String processing_version \"2014.0\";\n" +
"    String product_name \"S19980011998031.L3m_MO_CHL_chlor_a_9km.nc\";\n" +
"    String project \"Ocean Biology Processing Group (NASA/GSFC/OBPG)\";\n" +
"    String publisher_email \"data@oceancolor.gsfc.nasa.gov\";\n" +
"    String publisher_name \"NASA/GSFC/OBPG\";\n" +
"    String publisher_url \"https://oceandata.sci.gsfc.nasa.gov\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing -89.95834;\n" +
"    String spatialResolution \"9.20 km\";\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String summary \"summary of testSpecialAxis0FileNameInt\";\n" +
"    String temporal_range \"month\";\n" +
"    String title \"title of testSpecialAxis0FileNameInt\";\n" +
"    Float64 Westernmost_Easting -179.9583;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 25));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", 
            tDir, testName + "_Entire", ".dds"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Int32 myInt[myInt = 2];\n" +
"  Float32 latitude[latitude = 2160];\n" +
"  Float32 longitude[longitude = 4320];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 chlorophyll[myInt = 2][latitude = 2160][longitude = 4320];\n" +
"    MAPS:\n" +
"      Int32 myInt[myInt = 2];\n" +
"      Float32 latitude[latitude = 2160];\n" +
"      Float32 longitude[longitude = 4320];\n" +
"  } chlorophyll;\n" +
"} testSpecialAxis0PathNameInt;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv  with int values
        String2.log("\n*** .nc test get myInt[]\n");       
        userDapQuery = "myInt";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            tDir, testName + "_axis0", ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"myInt\n" +
"m\n" +
"1\n" +
"32\n";         
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv  with data from one file
        String2.log("\n*** .nc test read from one file\n");       
        userDapQuery = "chlorophyll[(32)][(38):4:(37)][(-123.6):4:(-122.6)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            tDir, testName + "_Data1", ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"myInt,latitude,longitude,chlorophyll\n" +
"m,degrees_north,degrees_east,mg m^-3\n" +
"32,38.041664,-123.625,0.321632\n" +
"32,38.041664,-123.291664,2.886503\n" +
"32,38.041664,-122.958336,3.058165\n" +
"32,38.041664,-122.625,NaN\n" +
"32,37.708332,-123.625,0.300352\n" +
"32,37.708332,-123.291664,0.374681\n" +
"32,37.708332,-122.958336,3.047059\n" +
"32,37.708332,-122.625,4.57632\n" +
"32,37.374996,-123.625,0.454626\n" +
"32,37.374996,-123.291664,0.373263\n" +
"32,37.374996,-122.958336,0.810264\n" +
"32,37.374996,-122.625,2.5943\n" +
"32,37.041664,-123.625,0.32735\n" +
"32,37.041664,-123.291664,0.384492\n" +
"32,37.041664,-122.958336,0.444622\n" +
"32,37.041664,-122.625,1.436202\n";         
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //img
        tName = eddGrid.makeNewFileForDapQuery(null, null, 
            "chlorophyll[(32)][(90):(-90)][(-180):(180)]",
            tDir, testName + "_img", ".png"); 
        SSR.displayInBrowser("file://" + tDir + tName);
    }

    /**
     * This tests special axis0 with a double from a global attribute.
     *
     * @throws Throwable if trouble
     */
    public static void testSpecialAxis0GlobalDouble() throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testSpecialAxis0GlobalDouble()\n");
        testVerboseOn();

        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDVGridAxis edvga;

        //ncdump a source .nc file
        tName = "/u00/satellite/SW1/mday/S19980321998059.L3m_MO_CHL_chlor_a_9km.nc";
        String2.log("\n*** ncdump of " + tName);
        String2.log(NcHelper.ncdump(tName, "-h"));

        //make the dataset
        String id = "testSpecialAxis0GlobalDouble";
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, id); 
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
        String tDir = EDStatic.fullTestCacheDirectory;
        String testName = "EDDGridFromNcFiles_Axis0GlobalDouble";

        //*** test getting das for entire dataset
        String2.log("\n*** .nc test das dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", 
            tDir, testName + "_Entire", ".das"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"Attributes {\n" +
"  myDouble {\n" +
"    Float64 actual_range 1998001.0, 1998032.0;\n" +
"    String ioos_category \"Other\";\n" +
"    String units \"kg\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range -89.95834, 89.95834;\n" +
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
"    Float32 actual_range -179.9583, 179.9584;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"    Float32 valid_max 180.0;\n" +
"    Float32 valid_min -180.0;\n" +
"  }\n" +
"  chlorophyll {\n" +
"    Float32 _FillValue -32767.0;\n" +
"    Float64 colorBarMaximum 30.0;\n" +
"    Float64 colorBarMinimum 0.03;\n" +
"    String colorBarScale \"Log\";\n" +
"    String ioos_category \"Ocean Color\";\n" +
"    String long_name \"Chlorophyll Concentration, OCI Algorithm\";\n" +
"    String references \"Hu, C., Lee Z., and Franz, B.A. (2012). Chlorophyll-a algorithms for oligotrophic oceans: A novel approach based on three-band reflectance difference, J. Geophys. Res., 117, C01011, doi:10.1029/2011JC007395.\";\n" +
"    String standard_name \"concentration_of_chlorophyll_in_sea_water\";\n" +
"    String units \"mg m^-3\";\n" +
"    Float32 valid_max 100.0;\n" +
"    Float32 valid_min 0.001;\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String _lastModified \"2015-10-02T00:07:10.000Z\";\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String Conventions \"CF-1.6, COARDS, ACDD-1.3\";\n" +
"    String creator_email \"data@oceancolor.gsfc.nasa.gov\";\n" +
"    String creator_name \"NASA/GSFC/OBPG\";\n" +
"    String creator_type \"group\";\n" +
"    String creator_url \"https://oceandata.sci.gsfc.nasa.gov\";\n" +
"    String date_created \"2015-10-02T00:07:10.000Z\";\n" +
"    Float64 Easternmost_Easting 179.9584;\n" +
"    Float64 geospatial_lat_max 89.95834;\n" +
"    Float64 geospatial_lat_min -89.95834;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 179.9584;\n" +
"    Float64 geospatial_lon_min -179.9583;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String grid_mapping_name \"latitude_longitude\";\n" +
"    String history \"smigen par=S19980011998031.L3m_MO_CHL_chlor_a_9km.nc.param\n" +
today; // (local files)
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);

//            + " https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/ux10/1day\n" +
//today + 

expected = 
//    "    String id "S19980011998031.L3b_MO_CHL.nc/L3/S19980011998031.L3b_MO_CHL.nc";
"    String identifier_product_doi \"https://dx.doi.org\";\n" +
"    String identifier_product_doi_authority \"https://dx.doi.org\";\n" +
"    String infoUrl \"https://oceandata.sci.gsfc.nasa.gov\";\n" +
"    String institution \"NASA/GSFC OBPG\";\n" +
"    String instrument \"SeaWiFS\";\n" +
"    String keywords \"algorithm, biology\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String l2_flag_names \"ATMFAIL,LAND,HILT,HISATZEN,STRAYLIGHT,CLDICE,COCCOLITH,LOWLW,CHLWARN,CHLFAIL,NAVWARN,MAXAERITER,ATMWARN,HISOLZEN,NAVFAIL,FILTER,HIGLINT\";\n" +
"    String license \"https://science.nasa.gov/earth-science/earth-science-data/data-information-policy/\n" +
"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String map_projection \"Equidistant Cylindrical\";\n" +
"    String measure \"Mean\";\n" +
"    String naming_authority \"gov.nasa.gsfc.sci.oceandata\";\n" +
"    Float64 Northernmost_Northing 89.95834;\n" +
"    String platform \"Orbview-2\";\n" +
"    String processing_level \"L3 Mapped\";\n" +
"    String processing_version \"2014.0\";\n" +
"    String product_name \"S19980011998031.L3m_MO_CHL_chlor_a_9km.nc\";\n" +
"    String project \"Ocean Biology Processing Group (NASA/GSFC/OBPG)\";\n" +
"    String publisher_email \"data@oceancolor.gsfc.nasa.gov\";\n" +
"    String publisher_name \"NASA/GSFC/OBPG\";\n" +
"    String publisher_type \"group\";\n" +
"    String publisher_url \"https://oceandata.sci.gsfc.nasa.gov\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing -89.95834;\n" +
"    String spatialResolution \"9.20 km\";\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String summary \"summary of testSpecialAxis0GlobalDouble\";\n" +
"    String temporal_range \"month\";\n" +
"    String title \"title of testSpecialAxis0GlobalDouble\";\n" +
"    Float64 Westernmost_Easting -179.9583;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 25));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", 
            tDir, testName + "_Entire", ".dds"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Float64 myDouble[myDouble = 2];\n" +
"  Float32 latitude[latitude = 2160];\n" +
"  Float32 longitude[longitude = 4320];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 chlorophyll[myDouble = 2][latitude = 2160][longitude = 4320];\n" +
"    MAPS:\n" +
"      Float64 myDouble[myDouble = 2];\n" +
"      Float32 latitude[latitude = 2160];\n" +
"      Float32 longitude[longitude = 4320];\n" +
"  } chlorophyll;\n" +
"} testSpecialAxis0GlobalDouble;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv  with int values
        String2.log("\n*** .nc test get myDouble[]\n");       
        userDapQuery = "myDouble";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            tDir, testName + "_axis0", ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"myDouble\n" +
"kg\n" +
"1998001.0\n" +
"1998032.0\n";         
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv  with data from one file
        String2.log("\n*** .nc test read from one file\n");       
        userDapQuery = "chlorophyll[(1998032)][(38):4:(37)][(-123.6):4:(-122.6)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            tDir, testName + "_Data1", ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"myDouble,latitude,longitude,chlorophyll\n" +
"kg,degrees_north,degrees_east,mg m^-3\n" +
"1998032.0,38.041664,-123.625,0.321632\n" +
"1998032.0,38.041664,-123.291664,2.886503\n" +
"1998032.0,38.041664,-122.958336,3.058165\n" +
"1998032.0,38.041664,-122.625,NaN\n" +
"1998032.0,37.708332,-123.625,0.300352\n" +
"1998032.0,37.708332,-123.291664,0.374681\n" +
"1998032.0,37.708332,-122.958336,3.047059\n" +
"1998032.0,37.708332,-122.625,4.57632\n" +
"1998032.0,37.374996,-123.625,0.454626\n" +
"1998032.0,37.374996,-123.291664,0.373263\n" +
"1998032.0,37.374996,-122.958336,0.810264\n" +
"1998032.0,37.374996,-122.625,2.5943\n" +
"1998032.0,37.041664,-123.625,0.32735\n" +
"1998032.0,37.041664,-123.291664,0.384492\n" +
"1998032.0,37.041664,-122.958336,0.444622\n" +
"1998032.0,37.041664,-122.625,1.436202\n";         
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //img
        tName = eddGrid.makeNewFileForDapQuery(null, null, 
            "chlorophyll[(1998032)][(90):(-90)][(-180):(180)]",
            tDir, testName + "_img", ".png"); 
        SSR.displayInBrowser("file://" + tDir + tName);
    }

    /**
     * This tests generateDatasetsXml when there are groups.
     *
     * This is an hdf file with no Dimensions, just some array sizes.
     * !!! Support for these files is incomplete. You need a way to specify array sizes
     * in the dimensionsCSV parameter, to identify matching variables/
     */
    public static void testGenerateDatasetsXmlGroups() throws Throwable {
        //A test for Jessica Hausman
        //the test file is from ftp://podaac.jpl.nasa.gov/allData/aquarius/L2/V4/2011/237/
        EDDGrid eddGrid;
        String tName, results, tResults, expected, userDapQuery;
        int tPo;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);

        results = generateDatasetsXml(
            EDStatic.unitTestDataDir + "hdf/", 
            "Q2011237000100.L2_SCI_V4\\.0", "",
            "", //group
            "", DEFAULT_RELOAD_EVERY_N_MINUTES, null, null) + "\n"; //dimensionsCSV, reloadMinutes, cacheFromUrl

        expected = 
"<!-- NOTE! The source for this dataset has nGridVariables=154,\n" +
"  but this dataset will only serve 1 because the others use different dimensions. -->\n" +
"<dataset type=\"EDDGridFromNcFiles\" datasetID=\"hdf_4fef_5b21_e56a\" active=\"true\">\n" +
"    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>" + EDStatic.unitTestDataDir + "hdf/</fileDir>\n" +
"    <fileNameRegex>Q2011237000100.L2_SCI_V4\\.0</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <matchAxisNDigits>20</matchAxisNDigits>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"_lastModified\">2015155145346000</att>\n" +
"        <att name=\"Conventions\">CF-1.6</att>\n" +
"        <att name=\"Cycle_Number\" type=\"int\">1</att>\n" +
"        <att name=\"Data_Center\">NASA/GSFC Aquarius Data Processing Center</att>\n" +
"        <att name=\"Data_Type\">SCI</att>\n" +
"        <att name=\"Delta_TND_H_coefficient\" type=\"floatList\">3.549088E-4 6.833503E-5 5.9092673E-4</att>\n" +
"        <att name=\"Delta_TND_V_coefficient\" type=\"floatList\">1.9098911E-4 1.6030413E-4 7.3374447E-4</att>\n" +
"        <att name=\"End_Day\" type=\"int\">237</att>\n" +
"        <att name=\"End_Millisec\" type=\"int\">5939338</att>\n" +
"        <att name=\"End_Time\">2011237013859338</att>\n" +
"        <att name=\"End_Year\" type=\"int\">2011</att>\n" +
"        <att name=\"Input_Files\">Q2011237000100.L1A_SCI</att>\n" +
"        <att name=\"institution\">NASA/GSFC OBPG</att>\n" +
"        <att name=\"Latitude_Units\">degrees North</att>\n" +
"        <att name=\"Longitude_Units\">degrees East</att>\n" +
"        <att name=\"Mean_Solar_1415_MHz_Flux\" type=\"float\">89.5</att>\n" +
"        <att name=\"Mission\">SAC-D Aquarius</att>\n" +
"        <att name=\"Mission_Characteristics\">Nominal orbit: inclination=98.0 (Sun-synchronous); node=6PM (ascending); eccentricity=&lt;0.002; altitude=657 km; ground speed=6.825 km/sec</att>\n" +
"        <att name=\"Node_Crossing_Time\">2011237002530000</att>\n" +
"        <att name=\"Nominal_Navigation\">TRUE</att>\n" +
"        <att name=\"Number_of_Beams\" type=\"int\">3</att>\n" +
"        <att name=\"Number_of_Blocks\" type=\"int\">4083</att>\n" +
"        <att name=\"Orbit_Node_Longitude\" type=\"float\">-95.19445</att>\n" +
"        <att name=\"Orbit_Number\" type=\"int\">1110</att>\n" +
"        <att name=\"Pass_Number\" type=\"int\">1</att>\n" +
"        <att name=\"Percent_RFI\" type=\"float\">8.80208</att>\n" +
"        <att name=\"Percent_Water\" type=\"float\">0.624676</att>\n" +
"        <att name=\"Processing_Control\">ifile=/data1/sdpsoper/vdc/vpu0/workbuf/Q2011237000100.L1A_SCI ofile=/data1/sdpsoper/vdc/vpu0/workbuf/Q2011237000100.L2_SCI_V4.0 yancfile1=y2011082418.h5 yancfile2=y2011082500.h5 yancfile3=y2011082506.h5 c_delta_TND=0.005957319097863 0.012382688000750 0.000688525722399 0.005498736477495 0.009869297059855 0.000587055056684 0.006316487443884 0.013593252195015 0.000713618469860 0.005016272510612 0.010186625543718 0.000650544867204 0.006233499611709 0.009903518037236 0.000529897823645 0.006068613199395 0.010888564961881 0.000618946224004 ta_nominal_files=$OCDATAROOT/aquarius/radiometer/Ta_nom_4Apr2014_v2.lst wind_errortab_file=$OCDATAROOT/aquarius/radiometer/error_tab.h5 emiss_coeff_harm_file=$OCDATAROOT/aquarius/radiometer/deW_harm_coeffs_V9A_MI.h5 scat_coeff_harm_file=$OCDATAROOT/aquarius/radiometer/sigma0_harm_coeffs_V9A_MI.h5 climate_sal_file=$OCDATAROOT/aquarius/radiometer/AQ_SSS_clim_map_testbedV41_2year.h5 dtbw_win_sigma_file=$OCDATAROOT/aquarius/radiometer/dtbw_residual_win_sigma_bin_flag_V9_HHH_A.h5 dtbw_win_wav_file=$OCDATAROOT/aquarius/radiometer/dtbw_residual_win_wav_bin_V9_HHH_A.h5 rad_dta_gal_file=$OCDATAROOT/aquarius/radiometer/dta_gal_symm.h5 sss_algorithm=SIGMA0_HHH l2prod=default:rad_hh_wind_speed,rad_hhh_wind_speed,rad_exp_TaV_hhh,rad_exp_TaH_hhh,rad_exp_Ta3_hhh,anc_swh,rad_galact_Ta_ref_GO_V,rad_galact_Ta_ref_GO_H,rad_galact_dTa_V,rad_galact_dTa_H,rad_dtb_sst_wspd_V,rad_dtb_sst_wspd_H,density,SSS_unc_ran,SSS_unc_sys rad_offset_corr_file=$OCVARROOT/aquarius/rad_offset_corr_V400_Liang.h5 rad_apc_file=$OCDATAROOT/aquarius/radiometer/apc_matrix_may2013_1.h5 coeff_loss_file=$OCDATAROOT/aquarius/radiometer/coeff_loss_V4.txt rfi_mask_file=$OCDATAROOT/aquarius/radiometer/rfi_mask_1.h5 radflaglimitsfile=$OCDATAROOT/aquarius/radiometer/radiometer_flag_limits.txt dtb_emiss_wspd_file=$OCDATAROOT/aquarius/radiometer/dtb_emiss_wspd.h5 dI_U_coeff_file=$OCDATAROOT/aquarius/radiometer/dI_U_fit.h5 l2_uncertainty_maps_file=$OCDATAROOT/aquarius/radiometer/L2_uncertainty_maps_AD_V4.0.h5 rad_gainice_file=$OCDATAROOT/aquarius/radiometer/gain_ice_V3.6.h5 rad_landcorr_file=$OCDATAROOT/aquarius/radiometer/land_corr_tables_V3.6.h5 rad_landtables_file=$OCDATAROOT/aquarius/radiometer/land_tables_V3.6.h5 rad_sun_file=$OCDATAROOT/aquarius/radiometer/sun_tables_V3.6.h5 pversion=V4.0 xrayfile1=/data1/sdpsoper/vdc/vpu0/workbuf/N201123600_XRAY_GOES_24h.h5 xrayfile2=/data1/sdpsoper/vdc/vpu0/workbuf/N201123700_XRAY_GOES_24h.h5 rad_tausq_file=$OCDATAROOT/aquarius/radiometer/tausq.h5 rad_sunbak_file=$OCDATAROOT/aquarius/radiometer/sun_bak_tables.h5 rad_oceanrefl_file=$OCDATAROOT/aquarius/radiometer/ocean_reflectance.h5 rad_galwind_file=$OCDATAROOT/aquarius/radiometer/galaxy_wind_tables_V2.0.h5 coeff_nl_file=$OCDATAROOT/aquarius/radiometer/coeff_nl.txt matchup_lat=-999 matchup_lon=-999 matchup_delta_lat=1.0 matchup_delta_lon=1.0 matchup_min_distance=35.0 browse=false iopt_rfi=true iopt_nosa1=true iopt_l1b=false matchup_limits_file= rpy_adj=-0.51 0.16 0.00 instrument_gain_corr=0.00 0.00 0.00 0.00 0.00 0.00</att>\n" +
"        <att name=\"Processing_Time\">2015155145346000</att>\n" +
"        <att name=\"Processing_Version\">V4.0</att>\n" +
"        <att name=\"Product_Name\">Q2011237000100.L2_SCI_V4.0</att>\n" +
"        <att name=\"RAD_ANCILLARY_FILE1\">y2011082418.h5:N2011236_SST_OIV2AVAM_24h.nc,N2011237_SST_OIV2AVAM_24h.nc,N201123618_QATM_NCEP_6h.h5,N201123618_QMET_NCEP_6h,N201123618_SWH_NCEP_6h.h5,N201123600_SEAICE_NCEP_24h.hdf,N201123700_SEAICE_NCEP_24h.hdf,N201123600_SALINITY_HYCOM_24h.h5</att>\n" +
"        <att name=\"RAD_ANCILLARY_FILE2\">y2011082500.h5:N2011236_SST_OIV2AVAM_24h.nc,N2011237_SST_OIV2AVAM_24h.nc,N201123700_QATM_NCEP_6h.h5,N201123700_QMET_NCEP_6h,N201123700_SWH_NCEP_6h.h5,N201123600_SEAICE_NCEP_24h.hdf,N201123700_SEAICE_NCEP_24h.hdf,N201123600_SALINITY_HYCOM_24h.h5</att>\n" +
"        <att name=\"RAD_ANCILLARY_FILE3\">y2011082506.h5:N2011236_SST_OIV2AVAM_24h.nc,N2011237_SST_OIV2AVAM_24h.nc,N201123706_QATM_NCEP_6h.h5,N201123706_QMET_NCEP_6h,N201123706_SWH_NCEP_6h.h5,N201123600_SEAICE_NCEP_24h.hdf,N201123700_SEAICE_NCEP_24h.hdf,N201123600_SALINITY_HYCOM_24h.h5</att>\n" +
"        <att name=\"Radiometer_Calibration_Files\">coeff_loss_V4.txt,coeff_nl.txt</att>\n" +
"        <att name=\"Radiometer_Data_Tables\">land_tables_V3.6.h5,gain_ice_V3.6.h5,tausq.h5,ocean_reflectance.h5,sun_tables_V3.6.h5,sun_bak_tables.h5,galaxy_wind_tables_V2.0.h5,apc_matrix_may2013_1.h5,land_corr_tables_V3.6.h5,error_tab.h5,deW_harm_coeffs_V9A_MI.h5,sigma0_harm_coeffs_V9A_MI.h5,dtbw_residual_win_sigma_bin_flag_V9_HHH_A.h5,dtbw_residual_win_wav_bin_V9_HHH_A.h5,AQ_SSS_clim_map_testbedV41_2year.h5,dta_gal_symm.h5</att>\n" +
"        <att name=\"Radiometer_Flag_Limits\">RFI: 15 7 Land: 0.001 0.01 Ice: 0.001 0.01 Wind: 15 20 Temp: 1 3 FluxD: 0.02 0.05 FluxR: 0.02 0.05 Glint: 0.02 0.05 Moon: 0.02 0.05 Gal: 0.02 0.05 RPY: 1 1 5 Flare: 5e-05 0.0001 Tb cons: 0.4 Cold Water: 5 0 TF-TA: -1 -0.3 -1 0.3 Refl 1st Stokes (moon): 0.25 0.5 Refl 1st Stokes (galaxy): 5.6 3.6 (wind): 3</att>\n" +
"        <att name=\"Radiometer_Offset_Correction\" type=\"floatList\">0.122411124 0.04955084 0.14458 0.14635497 0.185108 0.21241409</att>\n" +
"        <att name=\"Radiometer_Polarizations\" type=\"int\">4</att>\n" +
"        <att name=\"Radiometer_Signals_per_Subcycle\" type=\"int\">5</att>\n" +
"        <att name=\"Radiometer_Subcycles\" type=\"int\">12</att>\n" +
"        <att name=\"Scatterometer_Ancillary_Files\">SEAICEFILE1=N201123600_SEAICE_NCEP_24h.hdf,TECFILE1=N201123600_TEC_IGS_24h.h5,QMETFILE1=N201123618_QMET_NCEP_6h,QMETFILE2=N201123700_QMET_NCEP_6h,QMETFILE3=N201123706_QMET_NCEP_6h,SEAICEFILE2=N201123700_SEAICE_NCEP_24h.hdf,TECFILE2=N201123700_TEC_IGS_24h.h5</att>\n" +
"        <att name=\"Scatterometer_Coefficient_Files\">atc_prt_convert_v3.txt,ext_temps_constants_convert_v3.txt,scat_temps_convert_v1.txt,radiometer_constants_convert_v2.txt,cmd_gn.dat</att>\n" +
"        <att name=\"Scatterometer_Polarizations\" type=\"int\">6</att>\n" +
"        <att name=\"Scatterometer_Processing_Control\">-limits /sdps/sdpsoper/Science/OCSSW/V2015.2/data/aquarius/scatterometer/L1B_limits_flA_05-08-2012.txt -debug -1 -L2_filter_rfi -param_file /sdps/sdpsoper/Science/OCSSW/V2015.2/data/aquarius/scatterometer/params_pointing_fix_10-31-2012.txt -dir_dat /sdps/sdpsoper/Science/OCSSW/V2015.2/data/aquarius/scatterometer -dir_out /data1/sdpsoper/vdc/vpu0/workbuf -dir_scratch /dev/shm/tmp_76616093 -apc_file /sdps/sdpsoper/Science/OCSSW/V2015.2/data/aquarius/scatterometer/L2_APC_matrix_theory_10-04-2011.txt -cal_level 3 -suppress_tlm_warnings -i /data1/sdpsoper/vdc/vpu0/workbuf/Q2011237000100.L1A_SCI </att>\n" +
"        <att name=\"Scatterometer_Subcycles\" type=\"int\">8</att>\n" +
"        <att name=\"Sensor\">Aquarius</att>\n" +
"        <att name=\"Sensor_Characteristics\">Number of beams=3; channels per receiver=4; frequency 1.413 GHz; bits per sample=16; instatntaneous field of view=6.5 degrees; science data block period=1.44 sec.</att>\n" +
"        <att name=\"Software_ID\">4.00</att>\n" +
"        <att name=\"Start_Day\" type=\"int\">237</att>\n" +
"        <att name=\"Start_Millisec\" type=\"int\">61256</att>\n" +
"        <att name=\"Start_Time\">2011237000101256</att>\n" +
"        <att name=\"Start_Year\" type=\"int\">2011</att>\n" +
"        <att name=\"Title\">Aquarius Level 2 Data</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"Conventions\">CF-1.6, COARDS, ACDD-1.3</att>\n" +
"        <att name=\"creator_email\">webadmin@oceancolor.gsfc.nasa.gov</att>\n" +
"        <att name=\"creator_name\">NASA/GSFC OBPG</att>\n" +
"        <att name=\"creator_type\">group</att>\n" +
"        <att name=\"creator_url\">https://oceancolor.gsfc.nasa.gov/cms/</att>\n" +
"        <att name=\"Data_Center\">null</att>\n" +
"        <att name=\"End_Day\">null</att>\n" +
"        <att name=\"End_Millisec\">null</att>\n" +
"        <att name=\"End_Time\">null</att>\n" +
"        <att name=\"End_Year\">null</att>\n" +
"        <att name=\"infoUrl\">https://oceancolor.gsfc.nasa.gov/cms/</att>\n" +
"        <att name=\"Input_Files\">null</att>\n" +
"        <att name=\"keywords\">aquarius, Aquarius_Flags/rad_rfi_flags, axis0, axis1, axis2, axis3, biology, center, color, data, flags, flight, goddard, group, gsfc, level, nasa, obpg, ocean, processing, quality, radiometer, rfi, space</att>\n" +
"        <att name=\"Latitude_Units\">null</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"Longitude_Units\">null</att>\n" +
"        <att name=\"Scatterometer_Processing_Control\">-limits /sdps/sdpsoper/Science/OCSSW/V2015.2/data/aquarius/scatterometer/L1B_limits_flA_05-08-2012.txt -debug -1 -L2_filter_rfi -param_file /sdps/sdpsoper/Science/OCSSW/V2015.2/data/aquarius/scatterometer/params_pointing_fix_10-31-2012.txt -dir_dat /sdps/sdpsoper/Science/OCSSW/V2015.2/data/aquarius/scatterometer -dir_out /data1/sdpsoper/vdc/vpu0/workbuf -dir_scratch /dev/shm/tmp_76616093 -apc_file /sdps/sdpsoper/Science/OCSSW/V2015.2/data/aquarius/scatterometer/L2_APC_matrix_theory_10-04-2011.txt -cal_level 3 -suppress_tlm_warnings -i /data1/sdpsoper/vdc/vpu0/workbuf/Q2011237000100.L1A_SCI</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" +
"        <att name=\"Start_Day\">null</att>\n" +
"        <att name=\"Start_Millisec\">null</att>\n" +
"        <att name=\"Start_Time\">null</att>\n" +
"        <att name=\"Start_Year\">null</att>\n" +
"        <att name=\"summary\">Aquarius Level 2 Data. NASA/Goddard Space Flight Center (GSFC) Ocean Biology Processing Group (OBPG) data from a local source.</att>\n" +
"        <att name=\"Title\">null</att>\n" +
"        <att name=\"title\">Aquarius Level 2 Data</att>\n" +
"    </addAttributes>\n" +
"    <axisVariable>\n" +
"        <sourceName>axis0</sourceName>\n" +
"        <destinationName>axis0</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Axis0</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>axis1</sourceName>\n" +
"        <destinationName>axis1</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Axis1</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>axis2</sourceName>\n" +
"        <destinationName>axis2</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Axis2</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>axis3</sourceName>\n" +
"        <destinationName>axis3</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Axis3</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Aquarius_Flags/rad_rfi_flags</sourceName>\n" +
"        <destinationName>Aquarius_Flags_rad_rfi_flags</destinationName>\n" +
"        <dataType>ubyte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">Radiometer RFI flags</att>\n" +
"            <att name=\"valid_max\" type=\"byte\">0</att>\n" +
"            <att name=\"valid_min\" type=\"byte\">0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"ubyte\">255</att>\n" +        //important test of addMvFvAttsIfNeeded and unsigned var
"            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";
        String2.log("\nresults=\n" + results);
        Test.ensureEqual(results, expected, 
            "results.length=" + results.length() + " expected.length=" + expected.length());

        //*** test that dataset
        eddGrid = (EDDGrid)oneFromDatasetsXml(null, "testGridGroups"); 

        //*** test getting das for entire dataset
        String2.log("\n*** test das dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_testGroupsA_Entire", ".das"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"Attributes {\n" +
"  axis0 {\n" +
"    Int32 actual_range 0, 4082;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Axis0\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  axis1 {\n" +
"    Int16 actual_range 0, 2;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Axis1\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  axis2 {\n" +
"    Int16 actual_range 0, 3;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Axis2\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  axis3 {\n" +
"    Int16 actual_range 0, 11;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Axis3\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  Aquarius_Flags_rad_rfi_flags {\n" +
"    Byte _FillValue -1;\n" +        //important test of unsigned var  .das doesn't support ubyte, so byte here   
"    String _Unsigned \"true\";\n" + //important test of unsigned var
"    Float64 colorBarMaximum 150.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Radiometer RFI flags\";\n" +
"    Byte valid_max 0;\n" +
"    Byte valid_min 0;\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String _lastModified \"2015155145346000\";\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String Conventions \"CF-1.6, COARDS, ACDD-1.3\";\n" +
"    String creator_email \"webadmin@oceancolor.gsfc.nasa.gov\";\n" +
"    String creator_name \"NASA/GSFC OBPG\";\n" +
"    String creator_type \"group\";\n" +
"    String creator_url \"https://oceancolor.gsfc.nasa.gov/cms/\";\n" +
"    Int32 Cycle_Number 1;\n" +
"    String Data_Type \"SCI\";\n" +
"    Float32 Delta_TND_H_coefficient 3.549088e-4, 6.833503e-5, 5.9092673e-4;\n" +
"    Float32 Delta_TND_V_coefficient 1.9098911e-4, 1.6030413e-4, 7.3374447e-4;\n" +
"    String history \"" + today;
//2015-06-24T17:36:33Z (local files)
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);

//            + " https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/ux10/1day\n" +
//today + 

expected = 
//"2015-06-24T17:36:33Z http://localhost:8080/cwexperimental/griddap/testGridGroups2.das\";\n" +
    "String infoUrl \"https://oceancolor.gsfc.nasa.gov/cms/\";\n" +
"    String institution \"NASA/GSFC OBPG\";\n" +
"    String keywords \"aquarius, Aquarius_Flags/rad_rfi_flags, axis0, axis1, axis2, axis3, biology, center, color, data, flags, flight, goddard, group, gsfc, level, nasa, obpg, ocean, processing, quality, radiometer, rfi, space\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    Float32 Mean_Solar_1415_MHz_Flux 89.5;\n" +
"    String Mission \"SAC-D Aquarius\";\n" +
"    String Mission_Characteristics \"Nominal orbit: inclination=98.0 (Sun-synchronous); node=6PM (ascending); eccentricity=<0.002; altitude=657 km; ground speed=6.825 km/sec\";\n" +
"    String Node_Crossing_Time \"2011237002530000\";\n" +
"    String Nominal_Navigation \"TRUE\";\n" +
"    Int32 Number_of_Beams 3;\n" +
"    Int32 Number_of_Blocks 4083;\n" +
"    Float32 Orbit_Node_Longitude -95.19445;\n" +
"    Int32 Orbit_Number 1110;\n" +
"    Int32 Pass_Number 1;\n" +
"    Float32 Percent_RFI 8.80208;\n" +
"    Float32 Percent_Water 0.624676;\n" +
"    String Processing_Control \"ifile=/data1/sdpsoper/vdc/vpu0/workbuf/Q2011237000100.L1A_SCI ofile=/data1/sdpsoper/vdc/vpu0/workbuf/Q2011237000100.L2_SCI_V4.0 yancfile1=y2011082418.h5 yancfile2=y2011082500.h5 yancfile3=y2011082506.h5 c_delta_TND=0.005957319097863 0.012382688000750 0.000688525722399 0.005498736477495 0.009869297059855 0.000587055056684 0.006316487443884 0.013593252195015 0.000713618469860 0.005016272510612 0.010186625543718 0.000650544867204 0.006233499611709 0.009903518037236 0.000529897823645 0.006068613199395 0.010888564961881 0.000618946224004 ta_nominal_files=$OCDATAROOT/aquarius/radiometer/Ta_nom_4Apr2014_v2.lst wind_errortab_file=$OCDATAROOT/aquarius/radiometer/error_tab.h5 emiss_coeff_harm_file=$OCDATAROOT/aquarius/radiometer/deW_harm_coeffs_V9A_MI.h5 scat_coeff_harm_file=$OCDATAROOT/aquarius/radiometer/sigma0_harm_coeffs_V9A_MI.h5 climate_sal_file=$OCDATAROOT/aquarius/radiometer/AQ_SSS_clim_map_testbedV41_2year.h5 dtbw_win_sigma_file=$OCDATAROOT/aquarius/radiometer/dtbw_residual_win_sigma_bin_flag_V9_HHH_A.h5 dtbw_win_wav_file=$OCDATAROOT/aquarius/radiometer/dtbw_residual_win_wav_bin_V9_HHH_A.h5 rad_dta_gal_file=$OCDATAROOT/aquarius/radiometer/dta_gal_symm.h5 sss_algorithm=SIGMA0_HHH l2prod=default:rad_hh_wind_speed,rad_hhh_wind_speed,rad_exp_TaV_hhh,rad_exp_TaH_hhh,rad_exp_Ta3_hhh,anc_swh,rad_galact_Ta_ref_GO_V,rad_galact_Ta_ref_GO_H,rad_galact_dTa_V,rad_galact_dTa_H,rad_dtb_sst_wspd_V,rad_dtb_sst_wspd_H,density,SSS_unc_ran,SSS_unc_sys rad_offset_corr_file=$OCVARROOT/aquarius/rad_offset_corr_V400_Liang.h5 rad_apc_file=$OCDATAROOT/aquarius/radiometer/apc_matrix_may2013_1.h5 coeff_loss_file=$OCDATAROOT/aquarius/radiometer/coeff_loss_V4.txt rfi_mask_file=$OCDATAROOT/aquarius/radiometer/rfi_mask_1.h5 radflaglimitsfile=$OCDATAROOT/aquarius/radiometer/radiometer_flag_limits.txt dtb_emiss_wspd_file=$OCDATAROOT/aquarius/radiometer/dtb_emiss_wspd.h5 dI_U_coeff_file=$OCDATAROOT/aquarius/radiometer/dI_U_fit.h5 l2_uncertainty_maps_file=$OCDATAROOT/aquarius/radiometer/L2_uncertainty_maps_AD_V4.0.h5 rad_gainice_file=$OCDATAROOT/aquarius/radiometer/gain_ice_V3.6.h5 rad_landcorr_file=$OCDATAROOT/aquarius/radiometer/land_corr_tables_V3.6.h5 rad_landtables_file=$OCDATAROOT/aquarius/radiometer/land_tables_V3.6.h5 rad_sun_file=$OCDATAROOT/aquarius/radiometer/sun_tables_V3.6.h5 pversion=V4.0 xrayfile1=/data1/sdpsoper/vdc/vpu0/workbuf/N201123600_XRAY_GOES_24h.h5 xrayfile2=/data1/sdpsoper/vdc/vpu0/workbuf/N201123700_XRAY_GOES_24h.h5 rad_tausq_file=$OCDATAROOT/aquarius/radiometer/tausq.h5 rad_sunbak_file=$OCDATAROOT/aquarius/radiometer/sun_bak_tables.h5 rad_oceanrefl_file=$OCDATAROOT/aquarius/radiometer/ocean_reflectance.h5 rad_galwind_file=$OCDATAROOT/aquarius/radiometer/galaxy_wind_tables_V2.0.h5 coeff_nl_file=$OCDATAROOT/aquarius/radiometer/coeff_nl.txt matchup_lat=-999 matchup_lon=-999 matchup_delta_lat=1.0 matchup_delta_lon=1.0 matchup_min_distance=35.0 browse=false iopt_rfi=true iopt_nosa1=true iopt_l1b=false matchup_limits_file= rpy_adj=-0.51 0.16 0.00 instrument_gain_corr=0.00 0.00 0.00 0.00 0.00 0.00\";\n" +
"    String Processing_Time \"2015155145346000\";\n" +
"    String Processing_Version \"V4.0\";\n" +
"    String Product_Name \"Q2011237000100.L2_SCI_V4.0\";\n" +
"    String RAD_ANCILLARY_FILE1 \"y2011082418.h5:N2011236_SST_OIV2AVAM_24h.nc,N2011237_SST_OIV2AVAM_24h.nc,N201123618_QATM_NCEP_6h.h5,N201123618_QMET_NCEP_6h,N201123618_SWH_NCEP_6h.h5,N201123600_SEAICE_NCEP_24h.hdf,N201123700_SEAICE_NCEP_24h.hdf,N201123600_SALINITY_HYCOM_24h.h5\";\n" +
"    String RAD_ANCILLARY_FILE2 \"y2011082500.h5:N2011236_SST_OIV2AVAM_24h.nc,N2011237_SST_OIV2AVAM_24h.nc,N201123700_QATM_NCEP_6h.h5,N201123700_QMET_NCEP_6h,N201123700_SWH_NCEP_6h.h5,N201123600_SEAICE_NCEP_24h.hdf,N201123700_SEAICE_NCEP_24h.hdf,N201123600_SALINITY_HYCOM_24h.h5\";\n" +
"    String RAD_ANCILLARY_FILE3 \"y2011082506.h5:N2011236_SST_OIV2AVAM_24h.nc,N2011237_SST_OIV2AVAM_24h.nc,N201123706_QATM_NCEP_6h.h5,N201123706_QMET_NCEP_6h,N201123706_SWH_NCEP_6h.h5,N201123600_SEAICE_NCEP_24h.hdf,N201123700_SEAICE_NCEP_24h.hdf,N201123600_SALINITY_HYCOM_24h.h5\";\n" +
"    String Radiometer_Calibration_Files \"coeff_loss_V4.txt,coeff_nl.txt\";\n" +
"    String Radiometer_Data_Tables \"land_tables_V3.6.h5,gain_ice_V3.6.h5,tausq.h5,ocean_reflectance.h5,sun_tables_V3.6.h5,sun_bak_tables.h5,galaxy_wind_tables_V2.0.h5,apc_matrix_may2013_1.h5,land_corr_tables_V3.6.h5,error_tab.h5,deW_harm_coeffs_V9A_MI.h5,sigma0_harm_coeffs_V9A_MI.h5,dtbw_residual_win_sigma_bin_flag_V9_HHH_A.h5,dtbw_residual_win_wav_bin_V9_HHH_A.h5,AQ_SSS_clim_map_testbedV41_2year.h5,dta_gal_symm.h5\";\n" +
"    String Radiometer_Flag_Limits \"RFI: 15 7 Land: 0.001 0.01 Ice: 0.001 0.01 Wind: 15 20 Temp: 1 3 FluxD: 0.02 0.05 FluxR: 0.02 0.05 Glint: 0.02 0.05 Moon: 0.02 0.05 Gal: 0.02 0.05 RPY: 1 1 5 Flare: 5e-05 0.0001 Tb cons: 0.4 Cold Water: 5 0 TF-TA: -1 -0.3 -1 0.3 Refl 1st Stokes (moon): 0.25 0.5 Refl 1st Stokes (galaxy): 5.6 3.6 (wind): 3\";\n" +
"    Float32 Radiometer_Offset_Correction 0.122411124, 0.04955084, 0.14458, 0.14635497, 0.185108, 0.21241409;\n" +
"    Int32 Radiometer_Polarizations 4;\n" +
"    Int32 Radiometer_Signals_per_Subcycle 5;\n" +
"    Int32 Radiometer_Subcycles 12;\n" +
"    String Scatterometer_Ancillary_Files \"SEAICEFILE1=N201123600_SEAICE_NCEP_24h.hdf,TECFILE1=N201123600_TEC_IGS_24h.h5,QMETFILE1=N201123618_QMET_NCEP_6h,QMETFILE2=N201123700_QMET_NCEP_6h,QMETFILE3=N201123706_QMET_NCEP_6h,SEAICEFILE2=N201123700_SEAICE_NCEP_24h.hdf,TECFILE2=N201123700_TEC_IGS_24h.h5\";\n" +
"    String Scatterometer_Coefficient_Files \"atc_prt_convert_v3.txt,ext_temps_constants_convert_v3.txt,scat_temps_convert_v1.txt,radiometer_constants_convert_v2.txt,cmd_gn.dat\";\n" +
"    Int32 Scatterometer_Polarizations 6;\n" +
"    String Scatterometer_Processing_Control \"-limits /sdps/sdpsoper/Science/OCSSW/V2015.2/data/aquarius/scatterometer/L1B_limits_flA_05-08-2012.txt -debug -1 -L2_filter_rfi -param_file /sdps/sdpsoper/Science/OCSSW/V2015.2/data/aquarius/scatterometer/params_pointing_fix_10-31-2012.txt -dir_dat /sdps/sdpsoper/Science/OCSSW/V2015.2/data/aquarius/scatterometer -dir_out /data1/sdpsoper/vdc/vpu0/workbuf -dir_scratch /dev/shm/tmp_76616093 -apc_file /sdps/sdpsoper/Science/OCSSW/V2015.2/data/aquarius/scatterometer/L2_APC_matrix_theory_10-04-2011.txt -cal_level 3 -suppress_tlm_warnings -i /data1/sdpsoper/vdc/vpu0/workbuf/Q2011237000100.L1A_SCI\";\n" +
"    Int32 Scatterometer_Subcycles 8;\n" +
"    String Sensor \"Aquarius\";\n" +
"    String Sensor_Characteristics \"Number of beams=3; channels per receiver=4; frequency 1.413 GHz; bits per sample=16; instatntaneous field of view=6.5 degrees; science data block period=1.44 sec.\";\n" +
"    String Software_ID \"4.00\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String summary \"Aquarius Level 2 Data. NASA/Goddard Space Flight Center (GSFC) Ocean Biology Processing Group (OBPG) data from a local source.\";\n" +
"    String title \"Aquarius Level 2 Data\";\n" +
"  }\n" +
"}\n";
        tPo = results.indexOf(expected.substring(0, 25));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_testGroupsA_Entire", ".dds"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Int32 axis0[axis0 = 4083];\n" +
"  Int16 axis1[axis1 = 3];\n" +
"  Int16 axis2[axis2 = 4];\n" +
"  Int16 axis3[axis3 = 12];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Byte Aquarius_Flags_rad_rfi_flags[axis0 = 4083][axis1 = 3][axis2 = 4][axis3 = 12];\n" +
"    MAPS:\n" +
"      Int32 axis0[axis0 = 4083];\n" +
"      Int16 axis1[axis1 = 3];\n" +
"      Int16 axis2[axis2 = 4];\n" +
"      Int16 axis3[axis3 = 12];\n" +
"  } Aquarius_Flags_rad_rfi_flags;\n" +
"} testGridGroups;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv  with data from one file
        String2.log("\n*** read all data\n");       
        userDapQuery = "";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_testGroupsA_Data1", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"axis0,axis1,axis2,axis3,Aquarius_Flags_rad_rfi_flags\n" +
"count,count,count,count,\n" +
"0,0,0,0,2\n" +
"0,0,0,1,2\n" +
"0,0,0,2,2\n" +
"0,0,0,3,2\n" +
"0,0,0,4,2\n" +
"0,0,0,5,2\n" +
"0,0,0,6,2\n" +
"0,0,0,7,2\n" +
"0,0,0,8,0\n" +
"0,0,0,9,120\n";         
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results.substring(0, 500));

        //*********** test group
                results = generateDatasetsXml(
            EDStatic.unitTestDataDir + "hdf/", 
            "Q2011237000100.L2_SCI_V4\\.0", "",
            "Navigation", //group
            "", DEFAULT_RELOAD_EVERY_N_MINUTES, null, null) + "\n"; //dimensionsCSV, reloadMinutes, cacheFromUrl

        expected = 
"<!-- NOTE! The source for this dataset has nGridVariables=154,\n" +
"  but this dataset will only serve 6 because the others use different dimensions. -->\n" +
"<dataset type=\"EDDGridFromNcFiles\" datasetID=\"hdf_29a7_711d_1fb8\" active=\"true\">\n" +
"    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>/erddapTest/hdf/</fileDir>\n" +
"    <fileNameRegex>Q2011237000100.L2_SCI_V4\\.0</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <matchAxisNDigits>20</matchAxisNDigits>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"_lastModified\">2015155145346000</att>\n" +
"        <att name=\"Conventions\">CF-1.6</att>\n" +
"        <att name=\"Cycle_Number\" type=\"int\">1</att>\n" +
"        <att name=\"Data_Center\">NASA/GSFC Aquarius Data Processing Center</att>\n" +
"        <att name=\"Data_Type\">SCI</att>\n" +
"        <att name=\"Delta_TND_H_coefficient\" type=\"floatList\">3.549088E-4 6.833503E-5 5.9092673E-4</att>\n" +
"        <att name=\"Delta_TND_V_coefficient\" type=\"floatList\">1.9098911E-4 1.6030413E-4 7.3374447E-4</att>\n" +
"        <att name=\"End_Day\" type=\"int\">237</att>\n" +
"        <att name=\"End_Millisec\" type=\"int\">5939338</att>\n" +
"        <att name=\"End_Time\">2011237013859338</att>\n" +
"        <att name=\"End_Year\" type=\"int\">2011</att>\n" +
"        <att name=\"Input_Files\">Q2011237000100.L1A_SCI</att>\n" +
"        <att name=\"institution\">NASA/GSFC OBPG</att>\n" +
"        <att name=\"Latitude_Units\">degrees North</att>\n" +
"        <att name=\"Longitude_Units\">degrees East</att>\n" +
"        <att name=\"Mean_Solar_1415_MHz_Flux\" type=\"float\">89.5</att>\n" +
"        <att name=\"Mission\">SAC-D Aquarius</att>\n" +
"        <att name=\"Mission_Characteristics\">Nominal orbit: inclination=98.0 (Sun-synchronous); node=6PM (ascending); eccentricity=&lt;0.002; altitude=657 km; ground speed=6.825 km/sec</att>\n" +
"        <att name=\"Node_Crossing_Time\">2011237002530000</att>\n" +
"        <att name=\"Nominal_Navigation\">TRUE</att>\n" +
"        <att name=\"Number_of_Beams\" type=\"int\">3</att>\n" +
"        <att name=\"Number_of_Blocks\" type=\"int\">4083</att>\n" +
"        <att name=\"Orbit_Node_Longitude\" type=\"float\">-95.19445</att>\n" +
"        <att name=\"Orbit_Number\" type=\"int\">1110</att>\n" +
"        <att name=\"Pass_Number\" type=\"int\">1</att>\n" +
"        <att name=\"Percent_RFI\" type=\"float\">8.80208</att>\n" +
"        <att name=\"Percent_Water\" type=\"float\">0.624676</att>\n" +
"        <att name=\"Processing_Control\">ifile=/data1/sdpsoper/vdc/vpu0/workbuf/Q2011237000100.L1A_SCI ofile=/data1/sdpsoper/vdc/vpu0/workbuf/Q2011237000100.L2_SCI_V4.0 yancfile1=y2011082418.h5 yancfile2=y2011082500.h5 yancfile3=y2011082506.h5 c_delta_TND=0.005957319097863 0.012382688000750 0.000688525722399 0.005498736477495 0.009869297059855 0.000587055056684 0.006316487443884 0.013593252195015 0.000713618469860 0.005016272510612 0.010186625543718 0.000650544867204 0.006233499611709 0.009903518037236 0.000529897823645 0.006068613199395 0.010888564961881 0.000618946224004 ta_nominal_files=$OCDATAROOT/aquarius/radiometer/Ta_nom_4Apr2014_v2.lst wind_errortab_file=$OCDATAROOT/aquarius/radiometer/error_tab.h5 emiss_coeff_harm_file=$OCDATAROOT/aquarius/radiometer/deW_harm_coeffs_V9A_MI.h5 scat_coeff_harm_file=$OCDATAROOT/aquarius/radiometer/sigma0_harm_coeffs_V9A_MI.h5 climate_sal_file=$OCDATAROOT/aquarius/radiometer/AQ_SSS_clim_map_testbedV41_2year.h5 dtbw_win_sigma_file=$OCDATAROOT/aquarius/radiometer/dtbw_residual_win_sigma_bin_flag_V9_HHH_A.h5 dtbw_win_wav_file=$OCDATAROOT/aquarius/radiometer/dtbw_residual_win_wav_bin_V9_HHH_A.h5 rad_dta_gal_file=$OCDATAROOT/aquarius/radiometer/dta_gal_symm.h5 sss_algorithm=SIGMA0_HHH l2prod=default:rad_hh_wind_speed,rad_hhh_wind_speed,rad_exp_TaV_hhh,rad_exp_TaH_hhh,rad_exp_Ta3_hhh,anc_swh,rad_galact_Ta_ref_GO_V,rad_galact_Ta_ref_GO_H,rad_galact_dTa_V,rad_galact_dTa_H,rad_dtb_sst_wspd_V,rad_dtb_sst_wspd_H,density,SSS_unc_ran,SSS_unc_sys rad_offset_corr_file=$OCVARROOT/aquarius/rad_offset_corr_V400_Liang.h5 rad_apc_file=$OCDATAROOT/aquarius/radiometer/apc_matrix_may2013_1.h5 coeff_loss_file=$OCDATAROOT/aquarius/radiometer/coeff_loss_V4.txt rfi_mask_file=$OCDATAROOT/aquarius/radiometer/rfi_mask_1.h5 radflaglimitsfile=$OCDATAROOT/aquarius/radiometer/radiometer_flag_limits.txt dtb_emiss_wspd_file=$OCDATAROOT/aquarius/radiometer/dtb_emiss_wspd.h5 dI_U_coeff_file=$OCDATAROOT/aquarius/radiometer/dI_U_fit.h5 l2_uncertainty_maps_file=$OCDATAROOT/aquarius/radiometer/L2_uncertainty_maps_AD_V4.0.h5 rad_gainice_file=$OCDATAROOT/aquarius/radiometer/gain_ice_V3.6.h5 rad_landcorr_file=$OCDATAROOT/aquarius/radiometer/land_corr_tables_V3.6.h5 rad_landtables_file=$OCDATAROOT/aquarius/radiometer/land_tables_V3.6.h5 rad_sun_file=$OCDATAROOT/aquarius/radiometer/sun_tables_V3.6.h5 pversion=V4.0 xrayfile1=/data1/sdpsoper/vdc/vpu0/workbuf/N201123600_XRAY_GOES_24h.h5 xrayfile2=/data1/sdpsoper/vdc/vpu0/workbuf/N201123700_XRAY_GOES_24h.h5 rad_tausq_file=$OCDATAROOT/aquarius/radiometer/tausq.h5 rad_sunbak_file=$OCDATAROOT/aquarius/radiometer/sun_bak_tables.h5 rad_oceanrefl_file=$OCDATAROOT/aquarius/radiometer/ocean_reflectance.h5 rad_galwind_file=$OCDATAROOT/aquarius/radiometer/galaxy_wind_tables_V2.0.h5 coeff_nl_file=$OCDATAROOT/aquarius/radiometer/coeff_nl.txt matchup_lat=-999 matchup_lon=-999 matchup_delta_lat=1.0 matchup_delta_lon=1.0 matchup_min_distance=35.0 browse=false iopt_rfi=true iopt_nosa1=true iopt_l1b=false matchup_limits_file= rpy_adj=-0.51 0.16 0.00 instrument_gain_corr=0.00 0.00 0.00 0.00 0.00 0.00</att>\n" +
"        <att name=\"Processing_Time\">2015155145346000</att>\n" +
"        <att name=\"Processing_Version\">V4.0</att>\n" +
"        <att name=\"Product_Name\">Q2011237000100.L2_SCI_V4.0</att>\n" +
"        <att name=\"RAD_ANCILLARY_FILE1\">y2011082418.h5:N2011236_SST_OIV2AVAM_24h.nc,N2011237_SST_OIV2AVAM_24h.nc,N201123618_QATM_NCEP_6h.h5,N201123618_QMET_NCEP_6h,N201123618_SWH_NCEP_6h.h5,N201123600_SEAICE_NCEP_24h.hdf,N201123700_SEAICE_NCEP_24h.hdf,N201123600_SALINITY_HYCOM_24h.h5</att>\n" +
"        <att name=\"RAD_ANCILLARY_FILE2\">y2011082500.h5:N2011236_SST_OIV2AVAM_24h.nc,N2011237_SST_OIV2AVAM_24h.nc,N201123700_QATM_NCEP_6h.h5,N201123700_QMET_NCEP_6h,N201123700_SWH_NCEP_6h.h5,N201123600_SEAICE_NCEP_24h.hdf,N201123700_SEAICE_NCEP_24h.hdf,N201123600_SALINITY_HYCOM_24h.h5</att>\n" +
"        <att name=\"RAD_ANCILLARY_FILE3\">y2011082506.h5:N2011236_SST_OIV2AVAM_24h.nc,N2011237_SST_OIV2AVAM_24h.nc,N201123706_QATM_NCEP_6h.h5,N201123706_QMET_NCEP_6h,N201123706_SWH_NCEP_6h.h5,N201123600_SEAICE_NCEP_24h.hdf,N201123700_SEAICE_NCEP_24h.hdf,N201123600_SALINITY_HYCOM_24h.h5</att>\n" +
"        <att name=\"Radiometer_Calibration_Files\">coeff_loss_V4.txt,coeff_nl.txt</att>\n" +
"        <att name=\"Radiometer_Data_Tables\">land_tables_V3.6.h5,gain_ice_V3.6.h5,tausq.h5,ocean_reflectance.h5,sun_tables_V3.6.h5,sun_bak_tables.h5,galaxy_wind_tables_V2.0.h5,apc_matrix_may2013_1.h5,land_corr_tables_V3.6.h5,error_tab.h5,deW_harm_coeffs_V9A_MI.h5,sigma0_harm_coeffs_V9A_MI.h5,dtbw_residual_win_sigma_bin_flag_V9_HHH_A.h5,dtbw_residual_win_wav_bin_V9_HHH_A.h5,AQ_SSS_clim_map_testbedV41_2year.h5,dta_gal_symm.h5</att>\n" +
"        <att name=\"Radiometer_Flag_Limits\">RFI: 15 7 Land: 0.001 0.01 Ice: 0.001 0.01 Wind: 15 20 Temp: 1 3 FluxD: 0.02 0.05 FluxR: 0.02 0.05 Glint: 0.02 0.05 Moon: 0.02 0.05 Gal: 0.02 0.05 RPY: 1 1 5 Flare: 5e-05 0.0001 Tb cons: 0.4 Cold Water: 5 0 TF-TA: -1 -0.3 -1 0.3 Refl 1st Stokes (moon): 0.25 0.5 Refl 1st Stokes (galaxy): 5.6 3.6 (wind): 3</att>\n" +
"        <att name=\"Radiometer_Offset_Correction\" type=\"floatList\">0.122411124 0.04955084 0.14458 0.14635497 0.185108 0.21241409</att>\n" +
"        <att name=\"Radiometer_Polarizations\" type=\"int\">4</att>\n" +
"        <att name=\"Radiometer_Signals_per_Subcycle\" type=\"int\">5</att>\n" +
"        <att name=\"Radiometer_Subcycles\" type=\"int\">12</att>\n" +
"        <att name=\"Scatterometer_Ancillary_Files\">SEAICEFILE1=N201123600_SEAICE_NCEP_24h.hdf,TECFILE1=N201123600_TEC_IGS_24h.h5,QMETFILE1=N201123618_QMET_NCEP_6h,QMETFILE2=N201123700_QMET_NCEP_6h,QMETFILE3=N201123706_QMET_NCEP_6h,SEAICEFILE2=N201123700_SEAICE_NCEP_24h.hdf,TECFILE2=N201123700_TEC_IGS_24h.h5</att>\n" +
"        <att name=\"Scatterometer_Coefficient_Files\">atc_prt_convert_v3.txt,ext_temps_constants_convert_v3.txt,scat_temps_convert_v1.txt,radiometer_constants_convert_v2.txt,cmd_gn.dat</att>\n" +
"        <att name=\"Scatterometer_Polarizations\" type=\"int\">6</att>\n" +
"        <att name=\"Scatterometer_Processing_Control\">-limits /sdps/sdpsoper/Science/OCSSW/V2015.2/data/aquarius/scatterometer/L1B_limits_flA_05-08-2012.txt -debug -1 -L2_filter_rfi -param_file /sdps/sdpsoper/Science/OCSSW/V2015.2/data/aquarius/scatterometer/params_pointing_fix_10-31-2012.txt -dir_dat /sdps/sdpsoper/Science/OCSSW/V2015.2/data/aquarius/scatterometer -dir_out /data1/sdpsoper/vdc/vpu0/workbuf -dir_scratch /dev/shm/tmp_76616093 -apc_file /sdps/sdpsoper/Science/OCSSW/V2015.2/data/aquarius/scatterometer/L2_APC_matrix_theory_10-04-2011.txt -cal_level 3 -suppress_tlm_warnings -i /data1/sdpsoper/vdc/vpu0/workbuf/Q2011237000100.L1A_SCI </att>\n" +
"        <att name=\"Scatterometer_Subcycles\" type=\"int\">8</att>\n" +
"        <att name=\"Sensor\">Aquarius</att>\n" +
"        <att name=\"Sensor_Characteristics\">Number of beams=3; channels per receiver=4; frequency 1.413 GHz; bits per sample=16; instatntaneous field of view=6.5 degrees; science data block period=1.44 sec.</att>\n" +
"        <att name=\"Software_ID\">4.00</att>\n" +
"        <att name=\"Start_Day\" type=\"int\">237</att>\n" +
"        <att name=\"Start_Millisec\" type=\"int\">61256</att>\n" +
"        <att name=\"Start_Time\">2011237000101256</att>\n" +
"        <att name=\"Start_Year\" type=\"int\">2011</att>\n" +
"        <att name=\"Title\">Aquarius Level 2 Data</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"Conventions\">CF-1.6, COARDS, ACDD-1.3</att>\n" +
"        <att name=\"creator_email\">webadmin@oceancolor.gsfc.nasa.gov</att>\n" +
"        <att name=\"creator_name\">NASA/GSFC OBPG</att>\n" +
"        <att name=\"creator_type\">group</att>\n" +
"        <att name=\"creator_url\">https://oceancolor.gsfc.nasa.gov/cms/</att>\n" +
"        <att name=\"Data_Center\">null</att>\n" +
"        <att name=\"End_Day\">null</att>\n" +
"        <att name=\"End_Millisec\">null</att>\n" +
"        <att name=\"End_Time\">null</att>\n" +
"        <att name=\"End_Year\">null</att>\n" +
"        <att name=\"infoUrl\">https://oceancolor.gsfc.nasa.gov/cms/</att>\n" +
"        <att name=\"Input_Files\">null</att>\n" +
"        <att name=\"keywords\">aquarius, Aquarius_Flags/radiometer_flags, average, axis0, axis1, axis2, biology, Block_Attributes/rad_samples, center, color, data, east, flags, flight, footprint, geodectic, goddard, group, gsfc, latitude, latitudes, level, longitude, longitudes, nasa, Navigation/cellatfoot, Navigation/cellonfoot, Navigation/scat_latfoot, Navigation/scat_lonfoot, number, obpg, ocean, per, processing, quality, radiometer, samples, scatterometer, space, statistics</att>\n" +
"        <att name=\"Latitude_Units\">null</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"Longitude_Units\">null</att>\n" +
"        <att name=\"Scatterometer_Processing_Control\">-limits /sdps/sdpsoper/Science/OCSSW/V2015.2/data/aquarius/scatterometer/L1B_limits_flA_05-08-2012.txt -debug -1 -L2_filter_rfi -param_file /sdps/sdpsoper/Science/OCSSW/V2015.2/data/aquarius/scatterometer/params_pointing_fix_10-31-2012.txt -dir_dat /sdps/sdpsoper/Science/OCSSW/V2015.2/data/aquarius/scatterometer -dir_out /data1/sdpsoper/vdc/vpu0/workbuf -dir_scratch /dev/shm/tmp_76616093 -apc_file /sdps/sdpsoper/Science/OCSSW/V2015.2/data/aquarius/scatterometer/L2_APC_matrix_theory_10-04-2011.txt -cal_level 3 -suppress_tlm_warnings -i /data1/sdpsoper/vdc/vpu0/workbuf/Q2011237000100.L1A_SCI</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" +
"        <att name=\"Start_Day\">null</att>\n" +
"        <att name=\"Start_Millisec\">null</att>\n" +
"        <att name=\"Start_Time\">null</att>\n" +
"        <att name=\"Start_Year\">null</att>\n" +
"        <att name=\"summary\">Aquarius Level 2 Data. NASA/Goddard Space Flight Center (GSFC) Ocean Biology Processing Group (OBPG) data from a local source.</att>\n" +
"        <att name=\"Title\">null</att>\n" +
"        <att name=\"title\">Aquarius Level 2 Data</att>\n" +
"    </addAttributes>\n" +
"    <axisVariable>\n" +
"        <sourceName>axis0</sourceName>\n" +
"        <destinationName>axis0</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Axis0</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>axis1</sourceName>\n" +
"        <destinationName>axis1</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Axis1</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>axis2</sourceName>\n" +
"        <destinationName>axis2</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Axis2</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Aquarius_Flags/radiometer_flags</sourceName>\n" +
"        <destinationName>Aquarius_Flags_radiometer_flags</destinationName>\n" +
"        <dataType>uint</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"Cold_water\">COLDWATER</att>\n" +
"            <att name=\"Direct_solar_flux_contamination\">FLUXD</att>\n" +
"            <att name=\"Galactic_contamination\">GALACTIC</att>\n" +
"            <att name=\"Land_contamination\">LAND</att>\n" +
"            <att name=\"long_name\">Radiometer data quality flags</att>\n" +
"            <att name=\"Moon_contamination\">MOON</att>\n" +
"            <att name=\"Moon_Galaxy_contamination\">REFL_1STOKES</att>\n" +
"            <att name=\"Non-nominal_navigation\">NAV</att>\n" +
"            <att name=\"Pointing_anomaly\">POINTING</att>\n" +
"            <att name=\"Rain_in_main_beam\">RAIN</att>\n" +
"            <att name=\"Reflected_solar_flux_contamination\">FLUXR</att>\n" +
"            <att name=\"RFI_contamination\">RFI</att>\n" +
"            <att name=\"RFI_level\">TFTADIFF</att>\n" +
"            <att name=\"RFI_regional_contamination\">RFI_REGION</att>\n" +
"            <att name=\"Roughness_correction_failure\">ROUGH</att>\n" +
"            <att name=\"SA_overflow\">SAOVERFLOW</att>\n" +
"            <att name=\"Sea_ice_contamination\">ICE</att>\n" +
"            <att name=\"Solar_flare_contamination\">FLARE</att>\n" +
"            <att name=\"Sun_glint\">SUNGLINT</att>\n" +
"            <att name=\"Tb_consistency\">TBCONS</att>\n" +
"            <att name=\"Unusual_brighness_temperature\">TEMP</att>\n" +
"            <att name=\"valid_max\" type=\"int\">0</att>\n" +
"            <att name=\"valid_min\" type=\"int\">0</att>\n" +
"            <att name=\"Wind_foam_contamination\">WIND</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"uint\">4294967295</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"            <att name=\"Non-nominal_navigation\">null</att>\n" +
"            <att name=\"Non_nominal_navigation\">NAV</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Block_Attributes/rad_samples</sourceName>\n" +
"        <destinationName>Block_Attributes_rad_samples</destinationName>\n" +
"        <dataType>ushort</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">Number of radiometer samples per average</att>\n" +
"            <att name=\"valid_max\" type=\"short\">0</att>\n" +
"            <att name=\"valid_min\" type=\"short\">0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"ushort\">65535</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Statistics</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Navigation/cellatfoot</sourceName>\n" +
"        <destinationName>Navigation_cellatfoot</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999.0</att>\n" +
"            <att name=\"long_name\">Geodectic Latitudes (3 dB)</att>\n" +
"            <att name=\"units\">degrees</att>\n" +
"            <att name=\"valid_max\" type=\"float\">90.0</att>\n" +
"            <att name=\"valid_min\" type=\"float\">-90.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-100.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Navigation/cellonfoot</sourceName>\n" +
"        <destinationName>Navigation_cellonfoot</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999.0</att>\n" +
"            <att name=\"long_name\">East Longitudes (3 dB)</att>\n" +
"            <att name=\"units\">degrees</att>\n" +
"            <att name=\"valid_max\" type=\"float\">180.0</att>\n" +
"            <att name=\"valid_min\" type=\"float\">-180.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">200.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-200.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Navigation/scat_latfoot</sourceName>\n" +
"        <destinationName>Navigation_scat_latfoot</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999.0</att>\n" +
"            <att name=\"long_name\">Scatterometer Latitude Footprint</att>\n" +
"            <att name=\"units\">degrees</att>\n" +
"            <att name=\"valid_max\" type=\"float\">90.0</att>\n" +
"            <att name=\"valid_min\" type=\"float\">-90.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-100.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Navigation/scat_lonfoot</sourceName>\n" +
"        <destinationName>Navigation_scat_lonfoot</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999.0</att>\n" +
"            <att name=\"long_name\">Scatterometer Longitude Footprint</att>\n" +
"            <att name=\"units\">degrees</att>\n" +
"            <att name=\"valid_max\" type=\"float\">180.0</att>\n" +
"            <att name=\"valid_min\" type=\"float\">-180.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">200.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-200.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";
        String2.log("\nresults=\n" + results);
        Test.ensureEqual(results, expected, 
            "results.length=" + results.length() + " expected.length=" + expected.length());

        //*** test that dataset
        eddGrid = (EDDGrid)oneFromDatasetsXml(null, "testGridGroupsNavigation"); 

        //*** test getting das for entire dataset
        String2.log("\n*** test das dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_testGroups_Entire", ".das"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"Attributes {\n" +
"  axis0 {\n" +
"    Int32 actual_range 0, 4082;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Axis0\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  axis1 {\n" +
"    Int16 actual_range 0, 2;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Axis1\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  axis2 {\n" +
"    Int16 actual_range 0, 3;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Axis2\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  Aquarius_Flags_radiometer_flags {\n" +
"    UInt32 _FillValue 4294967295;\n" +  //important test of _Unsigned.  DAP 2.0 supports UInt32
"    String Cold_water \"COLDWATER\";\n" +
"    Float64 colorBarMaximum 150.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String Direct_solar_flux_contamination \"FLUXD\";\n" +
"    String Galactic_contamination \"GALACTIC\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String Land_contamination \"LAND\";\n" +
"    String long_name \"Radiometer data quality flags\";\n" +
"    String Moon_contamination \"MOON\";\n" +
"    String Moon_Galaxy_contamination \"REFL_1STOKES\";\n" +
"    String Non_nominal_navigation \"NAV\";\n" +
"    String Pointing_anomaly \"POINTING\";\n" +
"    String Rain_in_main_beam \"RAIN\";\n" +
"    String Reflected_solar_flux_contamination \"FLUXR\";\n" +
"    String RFI_contamination \"RFI\";\n" +
"    String RFI_level \"TFTADIFF\";\n" +
"    String RFI_regional_contamination \"RFI_REGION\";\n" +
"    String Roughness_correction_failure \"ROUGH\";\n" +
"    String SA_overflow \"SAOVERFLOW\";\n" +
"    String Sea_ice_contamination \"ICE\";\n" +
"    String Solar_flare_contamination \"FLARE\";\n" +
"    String Sun_glint \"SUNGLINT\";\n" +
"    String Tb_consistency \"TBCONS\";\n" +
"    String Unusual_brighness_temperature \"TEMP\";\n" +
"    UInt32 valid_max 0;\n" +  //import test of _Unsigned  
"    UInt32 valid_min 0;\n" +  //import test of _Unsigned
"    String Wind_foam_contamination \"WIND\";\n" +
"  }\n" +
"  Block_Attributes_rad_samples {\n" +
"    UInt16 _FillValue 65535;\n" +    //import test of _Unsigned.  DAP 2.0 supports UInt16
"    Float64 colorBarMaximum 100.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Statistics\";\n" +
"    String long_name \"Number of radiometer samples per average\";\n" +
"    UInt16 valid_max 0;\n" + //import test of _Unsigned
"    UInt16 valid_min 0;\n" + //import test of _Unsigned
"  }\n" +
"  Navigation_cellatfoot {\n" +
"    Float32 _FillValue -9999.0;\n" +
"    Float64 colorBarMaximum 100.0;\n" +
"    Float64 colorBarMinimum -100.0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Geodectic Latitudes (3 dB)\";\n" +
"    String units \"degrees\";\n" +
"    Float32 valid_max 90.0;\n" +
"    Float32 valid_min -90.0;\n" +
"  }\n" +
"  Navigation_cellonfoot {\n" +
"    Float32 _FillValue -9999.0;\n" +
"    Float64 colorBarMaximum 200.0;\n" +
"    Float64 colorBarMinimum -200.0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"East Longitudes (3 dB)\";\n" +
"    String units \"degrees\";\n" +
"    Float32 valid_max 180.0;\n" +
"    Float32 valid_min -180.0;\n" +
"  }\n" +
"  Navigation_scat_latfoot {\n" +
"    Float32 _FillValue -9999.0;\n" +
"    Float64 colorBarMaximum 100.0;\n" +
"    Float64 colorBarMinimum -100.0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Scatterometer Latitude Footprint\";\n" +
"    String units \"degrees\";\n" +
"    Float32 valid_max 90.0;\n" +
"    Float32 valid_min -90.0;\n" +
"  }\n" +
"  Navigation_scat_lonfoot {\n" +
"    Float32 _FillValue -9999.0;\n" +
"    Float64 colorBarMaximum 200.0;\n" +
"    Float64 colorBarMinimum -200.0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Scatterometer Longitude Footprint\";\n" +
"    String units \"degrees\";\n" +
"    Float32 valid_max 180.0;\n" +
"    Float32 valid_min -180.0;\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String _lastModified \"2015155145346000\";\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String Conventions \"CF-1.6, COARDS, ACDD-1.3\";\n" +
"    String creator_email \"webadmin@oceancolor.gsfc.nasa.gov\";\n" +
"    String creator_name \"NASA/GSFC OBPG\";\n" +
"    String creator_type \"group\";\n" +
"    String creator_url \"https://oceancolor.gsfc.nasa.gov/cms/\";\n" +
"    Int32 Cycle_Number 1;\n" +
"    String Data_Type \"SCI\";\n" +
"    Float32 Delta_TND_H_coefficient 3.549088e-4, 6.833503e-5, 5.9092673e-4;\n" +
"    Float32 Delta_TND_V_coefficient 1.9098911e-4, 1.6030413e-4, 7.3374447e-4;\n" +
"    String history \"" + today;
//2015-06-24T17:36:33Z (local files)
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);

//            + " https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/ux10/1day\n" +
//today + 

expected = 
//"2015-06-24T17:36:33Z http://localhost:8080/cwexperimental/griddap/testGridGroups2.das\";\n" +
    "String infoUrl \"https://oceancolor.gsfc.nasa.gov/cms/\";\n" +
"    String institution \"NASA/GSFC OBPG\";\n" +
"    String keywords \"aquarius, Aquarius_Flags/radiometer_flags, average, axis0, axis1, axis2, biology, Block_Attributes/rad_samples, center, color, data, east, flags, flight, footprint, geodectic, goddard, group, gsfc, latitude, latitudes, level, longitude, longitudes, nasa, Navigation/cellatfoot, Navigation/cellonfoot, Navigation/scat_latfoot, Navigation/scat_lonfoot, number, obpg, ocean, per, processing, quality, radiometer, samples, scatterometer, space, statistics\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    Float32 Mean_Solar_1415_MHz_Flux 89.5;\n" +
"    String Mission \"SAC-D Aquarius\";\n" +
"    String Mission_Characteristics \"Nominal orbit: inclination=98.0 (Sun-synchronous); node=6PM (ascending); eccentricity=<0.002; altitude=657 km; ground speed=6.825 km/sec\";\n" +
"    String Node_Crossing_Time \"2011237002530000\";\n" +
"    String Nominal_Navigation \"TRUE\";\n" +
"    Int32 Number_of_Beams 3;\n" +
"    Int32 Number_of_Blocks 4083;\n" +
"    Float32 Orbit_Node_Longitude -95.19445;\n" +
"    Int32 Orbit_Number 1110;\n" +
"    Int32 Pass_Number 1;\n" +
"    Float32 Percent_RFI 8.80208;\n" +
"    Float32 Percent_Water 0.624676;\n" +
"    String Processing_Control \"ifile=/data1/sdpsoper/vdc/vpu0/workbuf/Q2011237000100.L1A_SCI ofile=/data1/sdpsoper/vdc/vpu0/workbuf/Q2011237000100.L2_SCI_V4.0 yancfile1=y2011082418.h5 yancfile2=y2011082500.h5 yancfile3=y2011082506.h5 c_delta_TND=0.005957319097863 0.012382688000750 0.000688525722399 0.005498736477495 0.009869297059855 0.000587055056684 0.006316487443884 0.013593252195015 0.000713618469860 0.005016272510612 0.010186625543718 0.000650544867204 0.006233499611709 0.009903518037236 0.000529897823645 0.006068613199395 0.010888564961881 0.000618946224004 ta_nominal_files=$OCDATAROOT/aquarius/radiometer/Ta_nom_4Apr2014_v2.lst wind_errortab_file=$OCDATAROOT/aquarius/radiometer/error_tab.h5 emiss_coeff_harm_file=$OCDATAROOT/aquarius/radiometer/deW_harm_coeffs_V9A_MI.h5 scat_coeff_harm_file=$OCDATAROOT/aquarius/radiometer/sigma0_harm_coeffs_V9A_MI.h5 climate_sal_file=$OCDATAROOT/aquarius/radiometer/AQ_SSS_clim_map_testbedV41_2year.h5 dtbw_win_sigma_file=$OCDATAROOT/aquarius/radiometer/dtbw_residual_win_sigma_bin_flag_V9_HHH_A.h5 dtbw_win_wav_file=$OCDATAROOT/aquarius/radiometer/dtbw_residual_win_wav_bin_V9_HHH_A.h5 rad_dta_gal_file=$OCDATAROOT/aquarius/radiometer/dta_gal_symm.h5 sss_algorithm=SIGMA0_HHH l2prod=default:rad_hh_wind_speed,rad_hhh_wind_speed,rad_exp_TaV_hhh,rad_exp_TaH_hhh,rad_exp_Ta3_hhh,anc_swh,rad_galact_Ta_ref_GO_V,rad_galact_Ta_ref_GO_H,rad_galact_dTa_V,rad_galact_dTa_H,rad_dtb_sst_wspd_V,rad_dtb_sst_wspd_H,density,SSS_unc_ran,SSS_unc_sys rad_offset_corr_file=$OCVARROOT/aquarius/rad_offset_corr_V400_Liang.h5 rad_apc_file=$OCDATAROOT/aquarius/radiometer/apc_matrix_may2013_1.h5 coeff_loss_file=$OCDATAROOT/aquarius/radiometer/coeff_loss_V4.txt rfi_mask_file=$OCDATAROOT/aquarius/radiometer/rfi_mask_1.h5 radflaglimitsfile=$OCDATAROOT/aquarius/radiometer/radiometer_flag_limits.txt dtb_emiss_wspd_file=$OCDATAROOT/aquarius/radiometer/dtb_emiss_wspd.h5 dI_U_coeff_file=$OCDATAROOT/aquarius/radiometer/dI_U_fit.h5 l2_uncertainty_maps_file=$OCDATAROOT/aquarius/radiometer/L2_uncertainty_maps_AD_V4.0.h5 rad_gainice_file=$OCDATAROOT/aquarius/radiometer/gain_ice_V3.6.h5 rad_landcorr_file=$OCDATAROOT/aquarius/radiometer/land_corr_tables_V3.6.h5 rad_landtables_file=$OCDATAROOT/aquarius/radiometer/land_tables_V3.6.h5 rad_sun_file=$OCDATAROOT/aquarius/radiometer/sun_tables_V3.6.h5 pversion=V4.0 xrayfile1=/data1/sdpsoper/vdc/vpu0/workbuf/N201123600_XRAY_GOES_24h.h5 xrayfile2=/data1/sdpsoper/vdc/vpu0/workbuf/N201123700_XRAY_GOES_24h.h5 rad_tausq_file=$OCDATAROOT/aquarius/radiometer/tausq.h5 rad_sunbak_file=$OCDATAROOT/aquarius/radiometer/sun_bak_tables.h5 rad_oceanrefl_file=$OCDATAROOT/aquarius/radiometer/ocean_reflectance.h5 rad_galwind_file=$OCDATAROOT/aquarius/radiometer/galaxy_wind_tables_V2.0.h5 coeff_nl_file=$OCDATAROOT/aquarius/radiometer/coeff_nl.txt matchup_lat=-999 matchup_lon=-999 matchup_delta_lat=1.0 matchup_delta_lon=1.0 matchup_min_distance=35.0 browse=false iopt_rfi=true iopt_nosa1=true iopt_l1b=false matchup_limits_file= rpy_adj=-0.51 0.16 0.00 instrument_gain_corr=0.00 0.00 0.00 0.00 0.00 0.00\";\n" +
"    String Processing_Time \"2015155145346000\";\n" +
"    String Processing_Version \"V4.0\";\n" +
"    String Product_Name \"Q2011237000100.L2_SCI_V4.0\";\n" +
"    String RAD_ANCILLARY_FILE1 \"y2011082418.h5:N2011236_SST_OIV2AVAM_24h.nc,N2011237_SST_OIV2AVAM_24h.nc,N201123618_QATM_NCEP_6h.h5,N201123618_QMET_NCEP_6h,N201123618_SWH_NCEP_6h.h5,N201123600_SEAICE_NCEP_24h.hdf,N201123700_SEAICE_NCEP_24h.hdf,N201123600_SALINITY_HYCOM_24h.h5\";\n" +
"    String RAD_ANCILLARY_FILE2 \"y2011082500.h5:N2011236_SST_OIV2AVAM_24h.nc,N2011237_SST_OIV2AVAM_24h.nc,N201123700_QATM_NCEP_6h.h5,N201123700_QMET_NCEP_6h,N201123700_SWH_NCEP_6h.h5,N201123600_SEAICE_NCEP_24h.hdf,N201123700_SEAICE_NCEP_24h.hdf,N201123600_SALINITY_HYCOM_24h.h5\";\n" +
"    String RAD_ANCILLARY_FILE3 \"y2011082506.h5:N2011236_SST_OIV2AVAM_24h.nc,N2011237_SST_OIV2AVAM_24h.nc,N201123706_QATM_NCEP_6h.h5,N201123706_QMET_NCEP_6h,N201123706_SWH_NCEP_6h.h5,N201123600_SEAICE_NCEP_24h.hdf,N201123700_SEAICE_NCEP_24h.hdf,N201123600_SALINITY_HYCOM_24h.h5\";\n" +
"    String Radiometer_Calibration_Files \"coeff_loss_V4.txt,coeff_nl.txt\";\n" +
"    String Radiometer_Data_Tables \"land_tables_V3.6.h5,gain_ice_V3.6.h5,tausq.h5,ocean_reflectance.h5,sun_tables_V3.6.h5,sun_bak_tables.h5,galaxy_wind_tables_V2.0.h5,apc_matrix_may2013_1.h5,land_corr_tables_V3.6.h5,error_tab.h5,deW_harm_coeffs_V9A_MI.h5,sigma0_harm_coeffs_V9A_MI.h5,dtbw_residual_win_sigma_bin_flag_V9_HHH_A.h5,dtbw_residual_win_wav_bin_V9_HHH_A.h5,AQ_SSS_clim_map_testbedV41_2year.h5,dta_gal_symm.h5\";\n" +
"    String Radiometer_Flag_Limits \"RFI: 15 7 Land: 0.001 0.01 Ice: 0.001 0.01 Wind: 15 20 Temp: 1 3 FluxD: 0.02 0.05 FluxR: 0.02 0.05 Glint: 0.02 0.05 Moon: 0.02 0.05 Gal: 0.02 0.05 RPY: 1 1 5 Flare: 5e-05 0.0001 Tb cons: 0.4 Cold Water: 5 0 TF-TA: -1 -0.3 -1 0.3 Refl 1st Stokes (moon): 0.25 0.5 Refl 1st Stokes (galaxy): 5.6 3.6 (wind): 3\";\n" +
"    Float32 Radiometer_Offset_Correction 0.122411124, 0.04955084, 0.14458, 0.14635497, 0.185108, 0.21241409;\n" +
"    Int32 Radiometer_Polarizations 4;\n" +
"    Int32 Radiometer_Signals_per_Subcycle 5;\n" +
"    Int32 Radiometer_Subcycles 12;\n" +
"    String Scatterometer_Ancillary_Files \"SEAICEFILE1=N201123600_SEAICE_NCEP_24h.hdf,TECFILE1=N201123600_TEC_IGS_24h.h5,QMETFILE1=N201123618_QMET_NCEP_6h,QMETFILE2=N201123700_QMET_NCEP_6h,QMETFILE3=N201123706_QMET_NCEP_6h,SEAICEFILE2=N201123700_SEAICE_NCEP_24h.hdf,TECFILE2=N201123700_TEC_IGS_24h.h5\";\n" +
"    String Scatterometer_Coefficient_Files \"atc_prt_convert_v3.txt,ext_temps_constants_convert_v3.txt,scat_temps_convert_v1.txt,radiometer_constants_convert_v2.txt,cmd_gn.dat\";\n" +
"    Int32 Scatterometer_Polarizations 6;\n" +
"    String Scatterometer_Processing_Control \"-limits /sdps/sdpsoper/Science/OCSSW/V2015.2/data/aquarius/scatterometer/L1B_limits_flA_05-08-2012.txt -debug -1 -L2_filter_rfi -param_file /sdps/sdpsoper/Science/OCSSW/V2015.2/data/aquarius/scatterometer/params_pointing_fix_10-31-2012.txt -dir_dat /sdps/sdpsoper/Science/OCSSW/V2015.2/data/aquarius/scatterometer -dir_out /data1/sdpsoper/vdc/vpu0/workbuf -dir_scratch /dev/shm/tmp_76616093 -apc_file /sdps/sdpsoper/Science/OCSSW/V2015.2/data/aquarius/scatterometer/L2_APC_matrix_theory_10-04-2011.txt -cal_level 3 -suppress_tlm_warnings -i /data1/sdpsoper/vdc/vpu0/workbuf/Q2011237000100.L1A_SCI\";\n" +
"    Int32 Scatterometer_Subcycles 8;\n" +
"    String Sensor \"Aquarius\";\n" +
"    String Sensor_Characteristics \"Number of beams=3; channels per receiver=4; frequency 1.413 GHz; bits per sample=16; instatntaneous field of view=6.5 degrees; science data block period=1.44 sec.\";\n" +
"    String Software_ID \"4.00\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String summary \"Aquarius Level 2 Data. NASA/Goddard Space Flight Center (GSFC) Ocean Biology Processing Group (OBPG) data from a local source.\";\n" +
"    String title \"Aquarius Level 2 Data\";\n" +
"  }\n" +
"}\n";
        tPo = results.indexOf(expected.substring(0, 25));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_testGroups_Entire", ".dds"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Int32 axis0[axis0 = 4083];\n" +
"  Int16 axis1[axis1 = 3];\n" +
"  Int16 axis2[axis2 = 4];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      UInt32 Aquarius_Flags_radiometer_flags[axis0 = 4083][axis1 = 3][axis2 = 4];\n" +
"    MAPS:\n" +
"      Int32 axis0[axis0 = 4083];\n" +
"      Int16 axis1[axis1 = 3];\n" +
"      Int16 axis2[axis2 = 4];\n" +
"  } Aquarius_Flags_radiometer_flags;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      UInt16 Block_Attributes_rad_samples[axis0 = 4083][axis1 = 3][axis2 = 4];\n" +
"    MAPS:\n" +
"      Int32 axis0[axis0 = 4083];\n" +
"      Int16 axis1[axis1 = 3];\n" +
"      Int16 axis2[axis2 = 4];\n" +
"  } Block_Attributes_rad_samples;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Navigation_cellatfoot[axis0 = 4083][axis1 = 3][axis2 = 4];\n" +
"    MAPS:\n" +
"      Int32 axis0[axis0 = 4083];\n" +
"      Int16 axis1[axis1 = 3];\n" +
"      Int16 axis2[axis2 = 4];\n" +
"  } Navigation_cellatfoot;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Navigation_cellonfoot[axis0 = 4083][axis1 = 3][axis2 = 4];\n" +
"    MAPS:\n" +
"      Int32 axis0[axis0 = 4083];\n" +
"      Int16 axis1[axis1 = 3];\n" +
"      Int16 axis2[axis2 = 4];\n" +
"  } Navigation_cellonfoot;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Navigation_scat_latfoot[axis0 = 4083][axis1 = 3][axis2 = 4];\n" +
"    MAPS:\n" +
"      Int32 axis0[axis0 = 4083];\n" +
"      Int16 axis1[axis1 = 3];\n" +
"      Int16 axis2[axis2 = 4];\n" +
"  } Navigation_scat_latfoot;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Navigation_scat_lonfoot[axis0 = 4083][axis1 = 3][axis2 = 4];\n" +
"    MAPS:\n" +
"      Int32 axis0[axis0 = 4083];\n" +
"      Int16 axis1[axis1 = 3];\n" +
"      Int16 axis2[axis2 = 4];\n" +
"  } Navigation_scat_lonfoot;\n" +
"} testGridGroupsNavigation;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv  with data from one file
        String2.log("\n*** read all data\n");       
        userDapQuery = "";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_testGroups_Data1", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"axis0,axis1,axis2,Aquarius_Flags_radiometer_flags,Block_Attributes_rad_samples,Navigation_cellatfoot,Navigation_cellonfoot,Navigation_scat_latfoot,Navigation_scat_lonfoot\n" +
"count,count,count,,,degrees,degrees,degrees,degrees\n" +
"0,0,0,0,55,-78.5692,-0.8074951,-79.0485,-1.7279968\n" +
"0,0,1,278600,60,-79.08211,-2.3416138,-78.6658,-0.50601196\n" +
"0,0,2,8,60,-79.41049,-0.060028076,-78.9499,1.28453\n";         
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results.substring(0, 500));
    }

    /** 
     * This tests generateDatasetsXml with a multi-group file.
     * @throws Throwable if touble
     */
    public static void testGenerateDatasetsXmlGroups2() throws Throwable {

        String2.log("\n*** EDDGridFromNcFiles.testGenerateDatasetsXmlGroups2");
        reallyVerbose = true;
        String results, tResults, gdxResults, expected, tName, userDapQuery;
        int po, tPo;
        EDDGrid eddGrid; 
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
        String tDir = EDStatic.fullTestCacheDirectory;

        String fileDir = "/data/charles/";
        String fileRegex = "testGroups2\\.nc";  //just 1 file, don't aggregate

/* */
        //*** group="" dimensionsCSV=""
        results = generateDatasetsXml(fileDir, fileRegex, "", "", //group
            "", -1, null, null) + "\n"; //dimensionsCSV, reloadMinutes, cacheFromUrl

        //GenerateDatasetsXml
        gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDGridFromNcFiles", fileDir, fileRegex, "",
            "", //group
            "", "-1", ""}, //dimensionsCSV, reloadMinutes, cacheFromUrl
            false); //doIt loop?
        Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt. " + 
            gdxResults.length() + " " + results.length());

        expected = 
"<dataset type=\"EDDGridFromNcFiles\" datasetID=\"charles_2c88_14af_91ba\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>/data/charles/</fileDir>\n" +
"    <fileNameRegex>testGroups2\\.nc</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <matchAxisNDigits>20</matchAxisNDigits>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"CHS_Data_Format\">Version_1</att>\n" +
"        <att name=\"Latitude_Units\">deg</att>\n" +
"        <att name=\"Longitude_Units\">deg</att>\n" +
"        <att name=\"Project\">USACE_S2G</att>\n" +
"        <att name=\"Record_Interval\" type=\"double\">15.0</att>\n" +
"        <att name=\"Record_Interval_Units\">min</att>\n" +
"        <att name=\"Region\">Sabine_to_Galveston</att>\n" +
"        <att name=\"Save_Point_Depth\" type=\"double\">13.2322</att>\n" +
"        <att name=\"Save_Point_Depth_Units\">m</att>\n" +
"        <att name=\"Save_Point_ID\" type=\"double\">2491.0</att>\n" +
"        <att name=\"Save_Point_Latitude\" type=\"double\">29.1969</att>\n" +
"        <att name=\"Save_Point_Longitude\" type=\"double\">-94.1768</att>\n" +
"        <att name=\"Storm_ID\">032</att>\n" +
"        <att name=\"Storm_Name\">Synthetic_032</att>\n" +
"        <att name=\"Storm_Type\">Tropical_Synthetic</att>\n" +
"        <att name=\"Vertical_Datum\">NAVD88</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"infoUrl\">???</att>\n" +
"        <att name=\"institution\">???</att>\n" +
"        <att name=\"keywords\">1/time, 1/water, 1/x, 1/y, air, air_pressure, atmosphere, atmospheric, averaged, currents, data, depth, earth, Earth Science &gt; Atmosphere &gt; Atmospheric Pressure &gt; Atmospheric Pressure Measurements, Earth Science &gt; Atmosphere &gt; Atmospheric Pressure &gt; Sea Level Pressure, Earth Science &gt; Atmosphere &gt; Atmospheric Pressure &gt; Static Pressure, elevation, level, local, measurements, pressure, science, sea, seawater, source, static, synthetic, Synthetic_032_-_1/Atmospheric_Pressure, Synthetic_032_-_1/time, Synthetic_032_-_1/Water_Elevation, Synthetic_032_-_1/X_Depth_Averaged_Velocity, Synthetic_032_-_1/X_Wind_Velocity, Synthetic_032_-_1/Y_Depth_Averaged_Velocity, Synthetic_032_-_1/Y_Wind_Velocity, time, velocity, water, wind</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"Latitude_Units\">null</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"Longitude_Units\">null</att>\n" +
"        <att name=\"Project\">null</att>\n" +
"        <att name=\"project\">USACE_S2G</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" +
"        <att name=\"summary\">Data from a local source.</att>\n" +
"        <att name=\"title\">Data from a local source.</att>\n" +
"    </addAttributes>\n" +
"    <axisVariable>\n" +
"        <sourceName>Synthetic_032_-_1/time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">seconds since 1800-01-01T00:00:00Z</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Synthetic 032 - 1/time</att>\n" +
"            <att name=\"source_name\">Synthetic_032_-_1/time</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Synthetic_032_-_1/Y_Depth_Averaged_Velocity</sourceName>\n" +
"        <destinationName>Synthetic_032_1_Y_Depth_Averaged_Velocity</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"Model_Variable\">VV00</att>\n" +
"            <att name=\"Units\">m/s</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Currents</att>\n" +
"            <att name=\"long_name\">Synthetic 032 - 1/Y Depth Averaged Velocity</att>\n" +
"            <att name=\"Units\">null</att>\n" +
"            <att name=\"units\">m/s</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Synthetic_032_-_1/Y_Wind_Velocity</sourceName>\n" +
"        <destinationName>Synthetic_032_1_Y_Wind_Velocity</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"Model_Variable\">RMV00</att>\n" +
"            <att name=\"Units\">m/s</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"long_name\">Synthetic 032 - 1/Y Wind Velocity</att>\n" +
"            <att name=\"Units\">null</att>\n" +
"            <att name=\"units\">m/s</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Synthetic_032_-_1/Water_Elevation</sourceName>\n" +
"        <destinationName>Synthetic_032_1_Water_Elevation</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"Model_Variable\">ET00</att>\n" +
"            <att name=\"Units\">m</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Synthetic 032 - 1/Water Elevation</att>\n" +
"            <att name=\"Units\">null</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Synthetic_032_-_1/X_Depth_Averaged_Velocity</sourceName>\n" +
"        <destinationName>Synthetic_032_1_X_Depth_Averaged_Velocity</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"Model_Variable\">UU00</att>\n" +
"            <att name=\"Units\">m/s</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Currents</att>\n" +
"            <att name=\"long_name\">Synthetic 032 - 1/X Depth Averaged Velocity</att>\n" +
"            <att name=\"Units\">null</att>\n" +
"            <att name=\"units\">m/s</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Synthetic_032_-_1/X_Wind_Velocity</sourceName>\n" +
"        <destinationName>Synthetic_032_1_X_Wind_Velocity</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"Model_Variable\">RMU00</att>\n" +
"            <att name=\"Units\">m/s</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"long_name\">Synthetic 032 - 1/X Wind Velocity</att>\n" +
"            <att name=\"Units\">null</att>\n" +
"            <att name=\"units\">m/s</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Synthetic_032_-_1/Atmospheric_Pressure</sourceName>\n" +
"        <destinationName>Synthetic_032_1_Atmospheric_Pressure</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"Model_Variable\">RMP00</att>\n" +
"            <att name=\"Units\">hPa</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">1050.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">950.0</att>\n" +
"            <att name=\"ioos_category\">Pressure</att>\n" +
"            <att name=\"long_name\">Air Pressure</att>\n" +
"            <att name=\"standard_name\">air_pressure</att>\n" +
"            <att name=\"Units\">null</att>\n" +
"            <att name=\"units\">hPa</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n" +
"\n";
        String2.log("results=\n" + results);
        Test.ensureEqual(results, expected, "");

        //*** test that dataset  !!! Notably getting appropriated global metadata from the group
        eddGrid = (EDDGrid)oneFromDatasetsXml(null, "testGridGroups2a"); 

        //*** test getting das for entire dataset
        String2.log("\n*** .nc test das dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_testGroups2_Entire", ".das"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 2.1324465e+9, 2.132784e+9;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Synthetic 032 - 1/time\";\n" +
"    String source_name \"Synthetic_032_-_1/time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  Synthetic_032_1_Y_Depth_Averaged_Velocity {\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Synthetic 032 - 1/Y Depth Averaged Velocity\";\n" +
"    String Model_Variable \"VV00\";\n" +
"    String units \"m/s\";\n" +
"  }\n" +
"  Synthetic_032_1_Y_Wind_Velocity {\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Synthetic 032 - 1/Y Wind Velocity\";\n" +
"    String Model_Variable \"RMV00\";\n" +
"    String units \"m/s\";\n" +
"  }\n" +
"  Synthetic_032_1_Water_Elevation {\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Synthetic 032 - 1/Water Elevation\";\n" +
"    String Model_Variable \"ET00\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  Synthetic_032_1_X_Depth_Averaged_Velocity {\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Synthetic 032 - 1/X Depth Averaged Velocity\";\n" +
"    String Model_Variable \"UU00\";\n" +
"    String units \"m/s\";\n" +
"  }\n" +
"  Synthetic_032_1_X_Wind_Velocity {\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Synthetic 032 - 1/X Wind Velocity\";\n" +
"    String Model_Variable \"RMU00\";\n" +
"    String units \"m/s\";\n" +
"  }\n" +
"  Synthetic_032_1_Atmospheric_Pressure {\n" +
"    Float64 colorBarMaximum 1050.0;\n" +
"    Float64 colorBarMinimum 950.0;\n" +
"    String ioos_category \"Pressure\";\n" +
"    String long_name \"Air Pressure\";\n" +
"    String Model_Variable \"RMP00\";\n" +
"    String standard_name \"air_pressure\";\n" +
"    String units \"hPa\";\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String CHS_Data_Format \"Version_1\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String history \"" + today;
//2015-06-24T17:36:33Z (local files)
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);

//            + " https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/ux10/1day\n" +
//today + 

expected = 
//"2015-06-24T17:36:33Z http://localhost:8080/cwexperimental/griddap/testGridGroups2a.das\";\n" +
    "String infoUrl \"???\";\n" +
"    String institution \"???\";\n" +
"    String keywords \"1/time, 1/water, 1/x, 1/y, air, air_pressure, atmosphere, atmospheric, averaged, currents, data, depth, earth, Earth Science > Atmosphere > Atmospheric Pressure > Atmospheric Pressure Measurements, Earth Science > Atmosphere > Atmospheric Pressure > Sea Level Pressure, Earth Science > Atmosphere > Atmospheric Pressure > Static Pressure, elevation, level, local, measurements, pressure, science, sea, seawater, source, static, synthetic, Synthetic_032_-_1/Atmospheric_Pressure, Synthetic_032_-_1/time, Synthetic_032_-_1/Water_Elevation, Synthetic_032_-_1/X_Depth_Averaged_Velocity, Synthetic_032_-_1/X_Wind_Velocity, Synthetic_032_-_1/Y_Depth_Averaged_Velocity, Synthetic_032_-_1/Y_Wind_Velocity, time, velocity, water, wind\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String project \"USACE_S2G\";\n" +
"    Float64 Record_Interval 15.0;\n" +
"    String Record_Interval_Units \"min\";\n" +
"    String Region \"Sabine_to_Galveston\";\n" +
"    Float64 Save_Point_Depth 13.2322;\n" +
"    String Save_Point_Depth_Units \"m\";\n" +
"    Float64 Save_Point_ID 2491.0;\n" +                //global att
"    Float64 Save_Point_Latitude 29.1969;\n" +
"    Float64 Save_Point_Longitude -94.1768;\n" +
"    String sourceUrl \"(local files)\";\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String Storm_ID \"032\";\n" +                    //group global att
"    String Storm_Name \"Synthetic_032\";\n" +
"    String Storm_Type \"Tropical_Synthetic\";\n" +
"    String summary \"Data from a local source.\";\n" +
"    String time_coverage_end \"2037-08-02T00:00:00Z\";\n" +
"    String time_coverage_start \"2037-07-29T02:15:00Z\";\n" +
"    String title \"Data from a local source.\";\n" +
"    String Vertical_Datum \"NAVD88\";\n" +
"  }\n" +
"}\n";
        tPo = results.indexOf(expected.substring(0, 25));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_testGroups2_Entire", ".dds"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Float64 time[time = 376];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 Synthetic_032_1_Y_Depth_Averaged_Velocity[time = 376];\n" +
"    MAPS:\n" +
"      Float64 time[time = 376];\n" +
"  } Synthetic_032_1_Y_Depth_Averaged_Velocity;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 Synthetic_032_1_Y_Wind_Velocity[time = 376];\n" +
"    MAPS:\n" +
"      Float64 time[time = 376];\n" +
"  } Synthetic_032_1_Y_Wind_Velocity;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 Synthetic_032_1_Water_Elevation[time = 376];\n" +
"    MAPS:\n" +
"      Float64 time[time = 376];\n" +
"  } Synthetic_032_1_Water_Elevation;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 Synthetic_032_1_X_Depth_Averaged_Velocity[time = 376];\n" +
"    MAPS:\n" +
"      Float64 time[time = 376];\n" +
"  } Synthetic_032_1_X_Depth_Averaged_Velocity;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 Synthetic_032_1_X_Wind_Velocity[time = 376];\n" +
"    MAPS:\n" +
"      Float64 time[time = 376];\n" +
"  } Synthetic_032_1_X_Wind_Velocity;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 Synthetic_032_1_Atmospheric_Pressure[time = 376];\n" +
"    MAPS:\n" +
"      Float64 time[time = 376];\n" +
"  } Synthetic_032_1_Atmospheric_Pressure;\n" +
"} testGridGroups2a;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv  with data from one file
        String2.log("\n*** read all data\n");       
        userDapQuery = "";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_testGroups2_Data1", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"time,Synthetic_032_1_Y_Depth_Averaged_Velocity,Synthetic_032_1_Y_Wind_Velocity,Synthetic_032_1_Water_Elevation,Synthetic_032_1_X_Depth_Averaged_Velocity,Synthetic_032_1_X_Wind_Velocity,Synthetic_032_1_Atmospheric_Pressure\n" +
"UTC,m/s,m/s,m,m/s,m/s,hPa\n" +
"2037-07-29T02:15:00Z,3.4896736231E-5,0.065448165431,0.2762999689,-1.6722598515E-4,-0.34801462559,1012.3599659803921\n" +
"2037-07-29T02:30:00Z,1.5355913617E-4,0.13043440868,0.27629984735,-6.6624414582E-4,-0.69545874298,1012.3475211764705\n" +
"2037-07-29T02:45:00Z,3.7648943771E-4,0.19499354242,0.2762994609,-0.0014906568827,-1.0408543331,1012.3350494117647\n";         
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);


        //*** bad dimensionsCSV 
        try {
            results = generateDatasetsXml(fileDir, fileRegex, "",
                "zz", //group
                "rgb, eightbitcolor", -1, null, null) + "\n"; //dimensionsCSV, reloadMinutes, cacheFromUrl
            results = "shouldn't get here";
        } catch (Exception e) {
            results = e.toString();
        }    
        expected = "java.lang.RuntimeException: ERROR: dimension=rgb not found in the file!";
        Test.ensureEqual(results, expected, "results=\n" + results);

/* */
        //group="Synthetic_035_-_3"
        results = generateDatasetsXml(fileDir, fileRegex, "",
            "Synthetic_035_-_3", //group
            "", -1, null, null) + "\n"; //dimensionsCSV, reloadMinutes, cacheFromUrl
        expected = 
"<dataset type=\"EDDGridFromNcFiles\" datasetID=\"charles_9f77_002c_68c1\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>/data/charles/</fileDir>\n" +
"    <fileNameRegex>testGroups2\\.nc</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <matchAxisNDigits>20</matchAxisNDigits>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <!-- sourceAttributes>\n" +  
"        <att name=\"CHS_Data_Format\">Version_1</att>\n" +                     //rootGroup global att
"        <att name=\"Latitude_Units\">deg</att>\n" +                            //rootGroup global att
"        <att name=\"Longitude_Units\">deg</att>\n" +                           //rootGroup global att
"        <att name=\"Project\">USACE_S2G</att>\n" +                             //rootGroup global att
"        <att name=\"Record_Interval\" type=\"double\">15.0</att>\n" +
"        <att name=\"Record_Interval_Units\">min</att>\n" +
"        <att name=\"Region\">Sabine_to_Galveston</att>\n" +                    //rootGroup global att
"        <att name=\"Save_Point_Depth\" type=\"double\">13.2322</att>\n" +
"        <att name=\"Save_Point_Depth_Units\">m</att>\n" +
"        <att name=\"Save_Point_ID\" type=\"double\">2491.0</att>\n" +          //rootGroup global att
"        <att name=\"Save_Point_Latitude\" type=\"double\">29.1969</att>\n" +   //rootGroup global att
"        <att name=\"Save_Point_Longitude\" type=\"double\">-94.1768</att>\n" + //rootGroup global att
"        <att name=\"Storm_ID\">035</att>\n" +
"        <att name=\"Storm_Name\">Synthetic_035</att>\n" +
"        <att name=\"Storm_Type\">Tropical_Synthetic</att>\n" +
"        <att name=\"Vertical_Datum\">NAVD88</att>\n" +                         //rootGroup global att
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"infoUrl\">???</att>\n" +
"        <att name=\"institution\">???</att>\n" +
"        <att name=\"keywords\">3/time, 3/water, 3/x, 3/y, air, air_pressure, atmosphere, atmospheric, averaged, currents, data, depth, earth, Earth Science &gt; Atmosphere &gt; Atmospheric Pressure &gt; Atmospheric Pressure Measurements, Earth Science &gt; Atmosphere &gt; Atmospheric Pressure &gt; Sea Level Pressure, Earth Science &gt; Atmosphere &gt; Atmospheric Pressure &gt; Static Pressure, elevation, level, local, measurements, pressure, science, sea, seawater, source, static, synthetic, Synthetic_035_-_3/Atmospheric_Pressure, Synthetic_035_-_3/time, Synthetic_035_-_3/Water_Elevation, Synthetic_035_-_3/X_Depth_Averaged_Velocity, Synthetic_035_-_3/X_Wind_Velocity, Synthetic_035_-_3/Y_Depth_Averaged_Velocity, Synthetic_035_-_3/Y_Wind_Velocity, time, velocity, water, wind</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"Latitude_Units\">null</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"Longitude_Units\">null</att>\n" +
"        <att name=\"Project\">null</att>\n" +
"        <att name=\"project\">USACE_S2G</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" +
"        <att name=\"summary\">Data from a local source.</att>\n" +
"        <att name=\"title\">Data from a local source.</att>\n" +
"    </addAttributes>\n" +
"    <axisVariable>\n" +
"        <sourceName>Synthetic_035_-_3/time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">seconds since 1800-01-01T00:00:00Z</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Synthetic 035 - 3/time</att>\n" +
"            <att name=\"source_name\">Synthetic_035_-_3/time</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Synthetic_035_-_3/X_Depth_Averaged_Velocity</sourceName>\n" +
"        <destinationName>Synthetic_035_3_X_Depth_Averaged_Velocity</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"Model_Variable\">UU00</att>\n" +
"            <att name=\"Units\">m/s</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Currents</att>\n" +
"            <att name=\"long_name\">Synthetic 035 - 3/X Depth Averaged Velocity</att>\n" +
"            <att name=\"Units\">null</att>\n" +
"            <att name=\"units\">m/s</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Synthetic_035_-_3/X_Wind_Velocity</sourceName>\n" +
"        <destinationName>Synthetic_035_3_X_Wind_Velocity</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"Model_Variable\">RMU00</att>\n" +
"            <att name=\"Units\">m/s</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"long_name\">Synthetic 035 - 3/X Wind Velocity</att>\n" +
"            <att name=\"Units\">null</att>\n" +
"            <att name=\"units\">m/s</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Synthetic_035_-_3/Y_Depth_Averaged_Velocity</sourceName>\n" +
"        <destinationName>Synthetic_035_3_Y_Depth_Averaged_Velocity</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"Model_Variable\">VV00</att>\n" +
"            <att name=\"Units\">m/s</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Currents</att>\n" +
"            <att name=\"long_name\">Synthetic 035 - 3/Y Depth Averaged Velocity</att>\n" +
"            <att name=\"Units\">null</att>\n" +
"            <att name=\"units\">m/s</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Synthetic_035_-_3/Y_Wind_Velocity</sourceName>\n" +
"        <destinationName>Synthetic_035_3_Y_Wind_Velocity</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"Model_Variable\">RMV00</att>\n" +
"            <att name=\"Units\">m/s</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"long_name\">Synthetic 035 - 3/Y Wind Velocity</att>\n" +
"            <att name=\"Units\">null</att>\n" +
"            <att name=\"units\">m/s</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Synthetic_035_-_3/Water_Elevation</sourceName>\n" +
"        <destinationName>Synthetic_035_3_Water_Elevation</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"Model_Variable\">ET00</att>\n" +
"            <att name=\"Units\">m</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Synthetic 035 - 3/Water Elevation</att>\n" +
"            <att name=\"Units\">null</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Synthetic_035_-_3/Atmospheric_Pressure</sourceName>\n" +
"        <destinationName>Synthetic_035_3_Atmospheric_Pressure</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"Model_Variable\">RMP00</att>\n" +
"            <att name=\"Units\">hPa</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">1050.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">950.0</att>\n" +
"            <att name=\"ioos_category\">Pressure</att>\n" +
"            <att name=\"long_name\">Air Pressure</att>\n" +
"            <att name=\"standard_name\">air_pressure</att>\n" +
"            <att name=\"Units\">null</att>\n" +
"            <att name=\"units\">hPa</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n" +
"\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //same thing but with specific dimensionsCSV (fullName) (same result)
        results = generateDatasetsXml(fileDir, fileRegex, "",
            "Synthetic_035_-_3", //group
            "Synthetic_035_-_3/time", -1, null, null) + "\n"; //dimensionsCSV, reloadMinutes, cacheFromUrl
        //only difference is new datasetID
        results = String2.replaceAll(results,
            "ncSynthetic_035___3_dc8e_ae64_2f1c",
            "charles_9f77_002c_68c1");
        String2.log("results=\n" + results);
        Test.ensureEqual(results, expected, "");
        
        //*** test that dataset
        eddGrid = (EDDGrid)oneFromDatasetsXml(null, "testGridGroups2"); 

        //*** test getting das for entire dataset
        String2.log("\n*** .nc test das dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_testGroups2_Entire", ".das"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 2.2271409e+9, 2.2274784e+9;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Synthetic 035 - 3/time\";\n" +
"    String source_name \"Synthetic_035_-_3/time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  Synthetic_035_3_X_Depth_Averaged_Velocity {\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Synthetic 035 - 3/X Depth Averaged Velocity\";\n" +
"    String Model_Variable \"UU00\";\n" +
"    String units \"m/s\";\n" +
"  }\n" +
"  Synthetic_035_3_X_Wind_Velocity {\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Synthetic 035 - 3/X Wind Velocity\";\n" +
"    String Model_Variable \"RMU00\";\n" +
"    String units \"m/s\";\n" +
"  }\n" +
"  Synthetic_035_3_Y_Depth_Averaged_Velocity {\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Synthetic 035 - 3/Y Depth Averaged Velocity\";\n" +
"    String Model_Variable \"VV00\";\n" +
"    String units \"m/s\";\n" +
"  }\n" +
"  Synthetic_035_3_Y_Wind_Velocity {\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Synthetic 035 - 3/Y Wind Velocity\";\n" +
"    String Model_Variable \"RMV00\";\n" +
"    String units \"m/s\";\n" +
"  }\n" +
"  Synthetic_035_3_Water_Elevation {\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Synthetic 035 - 3/Water Elevation\";\n" +
"    String Model_Variable \"ET00\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  Synthetic_035_3_Atmospheric_Pressure {\n" +
"    Float64 colorBarMaximum 1050.0;\n" +
"    Float64 colorBarMinimum 950.0;\n" +
"    String ioos_category \"Pressure\";\n" +
"    String long_name \"Air Pressure\";\n" +
"    String Model_Variable \"RMP00\";\n" +
"    String standard_name \"air_pressure\";\n" +
"    String units \"hPa\";\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String CHS_Data_Format \"Version_1\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String history \"" + today;
//2015-06-24T17:36:33Z (local files)
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);

//            + " https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/ux10/1day\n" +
//today + 

expected = 
//"2015-06-24T17:36:33Z http://localhost:8080/cwexperimental/griddap/testGridGroups2.das\";\n" +
    "String infoUrl \"???\";\n" +
"    String institution \"???\";\n" +
"    String keywords \"3/time, 3/water, 3/x, 3/y, air, air_pressure, atmosphere, atmospheric, averaged, currents, data, depth, earth, Earth Science > Atmosphere > Atmospheric Pressure > Atmospheric Pressure Measurements, Earth Science > Atmosphere > Atmospheric Pressure > Sea Level Pressure, Earth Science > Atmosphere > Atmospheric Pressure > Static Pressure, elevation, level, local, measurements, pressure, science, sea, seawater, source, static, synthetic, Synthetic_035_-_3/Atmospheric_Pressure, Synthetic_035_-_3/time, Synthetic_035_-_3/Water_Elevation, Synthetic_035_-_3/X_Depth_Averaged_Velocity, Synthetic_035_-_3/X_Wind_Velocity, Synthetic_035_-_3/Y_Depth_Averaged_Velocity, Synthetic_035_-_3/Y_Wind_Velocity, time, velocity, water, wind\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String project \"USACE_S2G\";\n" +
"    Float64 Record_Interval 15.0;\n" +
"    String Record_Interval_Units \"min\";\n" +
"    String Region \"Sabine_to_Galveston\";\n" +
"    Float64 Save_Point_Depth 13.2322;\n" +
"    String Save_Point_Depth_Units \"m\";\n" +
"    Float64 Save_Point_ID 2491.0;\n" +           //global att
"    Float64 Save_Point_Latitude 29.1969;\n" +
"    Float64 Save_Point_Longitude -94.1768;\n" +
"    String sourceUrl \"(local files)\";\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String Storm_ID \"035\";\n" +                //group global att
"    String Storm_Name \"Synthetic_035\";\n" +
"    String Storm_Type \"Tropical_Synthetic\";\n" +
"    String summary \"Data from a local source.\";\n" +
"    String time_coverage_end \"2040-08-02T00:00:00Z\";\n" +
"    String time_coverage_start \"2040-07-29T02:15:00Z\";\n" +
"    String title \"Data from a local source.\";\n" +
"    String Vertical_Datum \"NAVD88\";\n" +
"  }\n" +
"}\n";
        tPo = results.indexOf(expected.substring(0, 25));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_testGroups2_Entire", ".dds"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Float64 time[time = 376];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 Synthetic_035_3_X_Depth_Averaged_Velocity[time = 376];\n" +
"    MAPS:\n" +
"      Float64 time[time = 376];\n" +
"  } Synthetic_035_3_X_Depth_Averaged_Velocity;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 Synthetic_035_3_X_Wind_Velocity[time = 376];\n" +
"    MAPS:\n" +
"      Float64 time[time = 376];\n" +
"  } Synthetic_035_3_X_Wind_Velocity;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 Synthetic_035_3_Y_Depth_Averaged_Velocity[time = 376];\n" +
"    MAPS:\n" +
"      Float64 time[time = 376];\n" +
"  } Synthetic_035_3_Y_Depth_Averaged_Velocity;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 Synthetic_035_3_Y_Wind_Velocity[time = 376];\n" +
"    MAPS:\n" +
"      Float64 time[time = 376];\n" +
"  } Synthetic_035_3_Y_Wind_Velocity;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 Synthetic_035_3_Water_Elevation[time = 376];\n" +
"    MAPS:\n" +
"      Float64 time[time = 376];\n" +
"  } Synthetic_035_3_Water_Elevation;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 Synthetic_035_3_Atmospheric_Pressure[time = 376];\n" +
"    MAPS:\n" +
"      Float64 time[time = 376];\n" +
"  } Synthetic_035_3_Atmospheric_Pressure;\n" +
"} testGridGroups2;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv  with data from one file
        String2.log("\n*** read all data\n");       
        userDapQuery = "";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_testGroups2_Data1", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"time,Synthetic_035_3_X_Depth_Averaged_Velocity,Synthetic_035_3_X_Wind_Velocity,Synthetic_035_3_Y_Depth_Averaged_Velocity,Synthetic_035_3_Y_Wind_Velocity,Synthetic_035_3_Water_Elevation,Synthetic_035_3_Atmospheric_Pressure\n" +
"UTC,m/s,m/s,m/s,m/s,m,hPa\n" +
"2040-07-29T02:15:00Z,-1.6610081086E-4,-0.34663751684,3.6339346152E-5,0.068556443098,0.27629997937,1012.3623841176469\n" +
"2040-07-29T02:30:00Z,-6.6159450328E-4,-0.69268631167,1.5929486577E-4,0.13671213723,0.27629982464,1012.3523820588234\n" +
"2040-07-29T02:45:00Z,-0.0014798735286,-1.0366665842,3.8926085033E-4,0.20446523794,0.27629963069,1012.3423397058822\n";         
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);


        //similar request but with shortName dimensionsCSV, fails
        try {
            results = generateDatasetsXml(fileDir, fileRegex, "",
                "Synthetic_035_-_3", //group
                "time", -1, null, null) + "\n"; //dimensionsCSV, reloadMinutes, cacheFromUrl
            results = "shouldn't get here";
        } catch (Exception e) {
            results = e.toString();
        }    
        expected = 
"java.lang.RuntimeException: ERROR: dimension=time not found in the file!";
        Test.ensureEqual(results, expected, "results=\n" + results);
        
        //*** bad group="zz"
        try {
            results = generateDatasetsXml(fileDir, fileRegex, "",
                "zz", //group
                "", -1, null, null) + "\n"; //dimensionsCSV, reloadMinutes, cacheFromUrl
            results = "shouldn't get here";
        } catch (Exception e) {
            results = e.toString();
        }    
        expected = 
"java.lang.RuntimeException: ERROR in NcHelper.findMaxDVariables: group=\"zz\" isn't in the NetCDF file.";
        Test.ensureEqual(results, expected, "results=\n" + results);

        String2.log("\nEDDGridFromNcFiles.testGenerateDatasetsXmlGroups2 passed the test.");

    }


    /**
     * This tests writing Igor Text Files .itx.
     *
     * @throws Throwable if trouble
     */
    public static void testIgor() throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testIgor()\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String dir = EDStatic.fullTestCacheDirectory;
        String error = "";
        int po;
        EDV edv;

        String id = "jplMURSST41";
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, id); 
        
        //test axes-only request
        userDapQuery = "time[(2002-06-02T09:00:00Z)],latitude[(-75):500:(75)],longitude[(-179.99):500:(180.0)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            "IgorAxes", ".itx"); 
        results = String2.directReadFrom88591File(dir + tName);
        results = String2.replaceAll(results, '\r', '\n');
        //String2.log(results);
        expected = 
"IGOR\n" +
"WAVES/D time2\n" + //time2 since time is an Igor reserved word
"BEGIN\n" +
"3.1058532E9\n" +
"END\n" +
"X SetScale d 3.1058532E9,3.1058532E9, \"dat\", time2\n" +
"\n" +
"WAVES/S latitude\n" +
"BEGIN\n" +
"-75.0\n" +
"-70.0\n" +
"-65.0\n" +
"-60.0\n" +
"-55.0\n" +
"-50.0\n" +
"-45.0\n" +
"-40.0\n" +
"-35.0\n" +
"-30.0\n" +
"-25.0\n" +
"-20.0\n" +
"-15.0\n" +
"-10.0\n" +
"-5.0\n" +
"0.0\n" +
"5.0\n" +
"10.0\n" +
"15.0\n" +
"20.0\n" +
"25.0\n" +
"30.0\n" +
"35.0\n" +
"40.0\n" +
"45.0\n" +
"50.0\n" +
"55.0\n" +
"60.0\n" +
"65.0\n" +
"70.0\n" +
"75.0\n" +
"END\n" +
"X SetScale d -75.0,75.0, \"degrees_north\", latitude\n" +
"\n" +
"WAVES/S longitude\n" +
"BEGIN\n" +
"-179.99\n" +
"-174.99\n" +
"-169.99\n" +
"-164.99\n" +
"-159.99\n" +
"-154.99\n" +
"-149.99\n" +
"-144.99\n" +
"-139.99\n" +
"-134.99\n" +
"-129.99\n" +
"-124.99\n" +
"-119.99\n" +
"-114.99\n" +
"-109.99\n" +
"-104.99\n" +
"-99.99\n" +
"-94.99\n" +
"-89.99\n" +
"-84.99\n" +
"-79.99\n" +
"-74.99\n" +
"-69.99\n" +
"-64.99\n" +
"-59.99\n" +
"-54.99\n" +
"-49.99\n" +
"-44.99\n" +
"-39.99\n" +
"-34.99\n" +
"-29.99\n" +
"-24.99\n" +
"-19.99\n" +
"-14.99\n" +
"-9.99\n" +
"-4.99\n" +
"0.01\n" +
"5.01\n" +
"10.01\n" +
"15.01\n" +
"20.01\n" +
"25.01\n" +
"30.01\n" +
"35.01\n" +
"40.01\n" +
"45.01\n" +
"50.01\n" +
"55.01\n" +
"60.01\n" +
"65.01\n" +
"70.01\n" +
"75.01\n" +
"80.01\n" +
"85.01\n" +
"90.01\n" +
"95.01\n" +
"100.01\n" +
"105.01\n" +
"110.01\n" +
"115.01\n" +
"120.01\n" +
"125.01\n" +
"130.01\n" +
"135.01\n" +
"140.01\n" +
"145.01\n" +
"150.01\n" +
"155.01\n" +
"160.01\n" +
"165.01\n" +
"170.01\n" +
"175.01\n" +
"END\n" +
"X SetScale d -179.99,175.01, \"degrees_east\", longitude\n" +
"\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //test grid request
        //when plotted, should be tiny map of world with mvs for continents:
        //  /downloads/IgorTestJplMURSST41.png
        userDapQuery = "analysed_sst[(2002-06-02T09:00:00Z)][(-75):500:(75)][(-179.5):500:(179.5)],mask[(2002-06-02T09:00:00Z)][(-75):500:(75)][(-179.5):500:(179.5)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, dir, 
            "IgorGrid", ".itx"); 
        results = String2.directReadFrom88591File(dir + tName);
        results = String2.replaceAll(results, '\r', '\n');
        //String2.log(results);
        expected = 
"IGOR\n" +
"WAVES/D time2\n" + //time2 since time is an Igor reserved word
"BEGIN\n" +
"3.1058532E9\n" +
"END\n" +
"X SetScale d 3.1058532E9,3.1058532E9, \"dat\", time2\n" + 
"\n" +
"WAVES/S latitude\n" +
"BEGIN\n" +
"-75.0\n" +
"-70.0\n" +
"-65.0\n" +
"-60.0\n" +
"-55.0\n" +
"-50.0\n" +
"-45.0\n" +
"-40.0\n" +
"-35.0\n" +
"-30.0\n" +
"-25.0\n" +
"-20.0\n" +
"-15.0\n" +
"-10.0\n" +
"-5.0\n" +
"0.0\n" +
"5.0\n" +
"10.0\n" +
"15.0\n" +
"20.0\n" +
"25.0\n" +
"30.0\n" +
"35.0\n" +
"40.0\n" +
"45.0\n" +
"50.0\n" +
"55.0\n" +
"60.0\n" +
"65.0\n" +
"70.0\n" +
"75.0\n" +
"END\n" +
"X SetScale d -75.0,75.0, \"degrees_north\", latitude\n" +
"\n" +
"WAVES/S longitude\n" +
"BEGIN\n" +
"-179.5\n" +
"-174.5\n" +
"-169.5\n" +
"-164.5\n" +
"-159.5\n" +
"-154.5\n" +
"-149.5\n" +
"-144.5\n" +
"-139.5\n" +
"-134.5\n" +
"-129.5\n" +
"-124.5\n" +
"-119.5\n" +
"-114.5\n" +
"-109.5\n" +
"-104.5\n" +
"-99.5\n" +
"-94.5\n" +
"-89.5\n" +
"-84.5\n" +
"-79.5\n" +
"-74.5\n" +
"-69.5\n" +
"-64.5\n" +
"-59.5\n" +
"-54.5\n" +
"-49.5\n" +
"-44.5\n" +
"-39.5\n" +
"-34.5\n" +
"-29.5\n" +
"-24.5\n" +
"-19.5\n" +
"-14.5\n" +
"-9.5\n" +
"-4.5\n" +
"0.5\n" +
"5.5\n" +
"10.5\n" +
"15.5\n" +
"20.5\n" +
"25.5\n" +
"30.5\n" +
"35.5\n" +
"40.5\n" +
"45.5\n" +
"50.5\n" +
"55.5\n" +
"60.5\n" +
"65.5\n" +
"70.5\n" +
"75.5\n" +
"80.5\n" +
"85.5\n" +
"90.5\n" +
"95.5\n" +
"100.5\n" +
"105.5\n" +
"110.5\n" +
"115.5\n" +
"120.5\n" +
"125.5\n" +
"130.5\n" +
"135.5\n" +
"140.5\n" +
"145.5\n" +
"150.5\n" +
"155.5\n" +
"160.5\n" +
"165.5\n" +
"170.5\n" +
"175.5\n" +
"END\n" +
"X SetScale d -179.5,175.5, \"degrees_east\", longitude\n" +
"\n" +
"WAVES/D/N=(31,72,1) analysed_sst\n" + //Igor wants nRow,nCol,nLayer[,nChunk]! row and col are switched from logical order!
"BEGIN\n" +
"-1.8000000000000007\n" +
"-1.8000000000000007\n" +
"-1.8000000000000007\n" +
"-1.8000000000000007\n" +
"-1.8000000000000007\n" +
"-1.8000000000000007\n" +
"-1.7950000000000017\n" +
"-1.7959999999999994\n" +
"NaN\n";
        Test.ensureEqual(results.substring(0, expected.length()), 
            expected, "\nresults=\n" + results);

expected = 
"END\n" +
"X SetScale d -1.8000000000000007,31.895, \"degree_C\", analysed_sst\n" +
"X SetScale /I z, 1.0230084E9,1.0230084E9, \"dat\", analysed_sst\n" +
"X SetScale /P y, -75.0,5.0, \"degrees_north\", analysed_sst\n" +
"X SetScale /P x, -179.5,5.0, \"degrees_east\", analysed_sst\n" +
"X Note analysed_sst, \"RowsDim:longitude;ColumnsDim:latitude;LayersDim:time2\"\n" +
"\n" +
"WAVES/B/N=(31,72,1) mask\n" +
"BEGIN\n" +
"9\n" +
"9\n" +
"9\n" +
"9\n";
        po = Math.max(0, results.indexOf(expected.substring(0,80)));
        Test.ensureEqual(results.substring(po, po + expected.length()), 
            expected, "\nresults=\n" + results + "\npo=" + po);

expected = 
"2\n" +
"9\n" +
"9\n" +
"9\n" +
"9\n" +
"9\n" +
"END\n" +
"X SetScale d 1,9, \"\", mask\n" +
"X SetScale /I z, 1.0230084E9,1.0230084E9, \"dat\", mask\n" +
"X SetScale /P y, -75.0,5.0, \"degrees_north\", mask\n" +
"X SetScale /P x, -179.5,5.0, \"degrees_east\", mask\n" +
"X Note mask, \"RowsDim:longitude;ColumnsDim:latitude;LayersDim:time2\"\n" +
"\n";
        Test.ensureEqual(results.substring(results.length() - expected.length()), 
            expected, "\nresults=\n" + results);

        String2.log("\n*** EDDGridFromNcFiles.testIgor finished successfully.\n" +
            "The grid test file is " + dir + tName);

    }



    /**
     * THIS ISN'T FINISHED. 
     * THE FILE HAS GROUPS BUT THE VARIABLES HAVE NO NAMED DIMENSIONS.
     * ERDDAP CURRENTLY REQUIRES NAMED DIMENSIONS. 
     * This reading variables in groups.
     *
     * @throws Throwable if trouble
     */
    public static void testGroups() throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testGroups()\n");
        testVerboseOn();

        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDVGridAxis edvga;
        //the test file is from ftp://podaac.jpl.nasa.gov/allData/aquarius/L2/V4/2011/237/

        String id = "testGroups";
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, id); 
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
        String tDir = EDStatic.fullTestCacheDirectory;
        String testName = "EDDGridFromNcFiles_groups";

    }


    /** 
     * This looks for resource leaks from repeated attempts to read an 
     * invalid nc file. 
     */
    public static void testBadNcFile(boolean runIncrediblySlowTest) throws Throwable {

        String2.log("\n*** EDDGridFromNcFiles.testBadNcFile()\n");
        testVerboseOff();
        String fileName = EDStatic.unitTestDataDir + "nc/invalidShortened2.nc";
        String id = "testBadNcFile";

        //try to read it many times
        for (int i = 0; i <= 1000000; i++) {
            if (i % 100000 == 0)
                String2.log("test #" + i);
            NetcdfFile ncFile = null; 
            try {
                ncFile = NetcdfFile.open(fileName); //this is what fails with 1/10000th file
                Variable var = ncFile.findVariable("MWcdom");  //size=[1,1,2321,4001]
                Array array = var.read();          
                System.out.println("shape=" + Arrays.toString(array.getShape()));

                array = var.read("0,0,0:2000:1000,0:4000:1000");
                System.out.println("shape=" + Arrays.toString(array.getShape()));

                //System.out.println(array.toString());
                System.out.println("Shouldn't get here!");
                String2.pressEnterToContinue("Shouldn't get here!");
                ncFile.close();
            } catch (Throwable t) {
                //expected
                try {
                    if (ncFile != null)
                        ncFile.close();
                } catch (Throwable t2) {
                    //don't care
                }
                if (i == 0) 
                    System.out.println("i=0 caught: " + t.toString());
            }
        }

        if (runIncrediblySlowTest) {
            //try to create the dataset many times
            for (int i = 0; i < 1000000; i++) {
                try {
                    EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, id); 
                    throw new RuntimeException("Shouldn't get here!");
                } catch (Throwable t) {
                    //expected
                    if (i == 0) 
                        System.out.println("i=0 caught: " + t.toString());
                }
            }
        }

    }

    /** 
     * This ensures that  looks for resource leaks from repeated attempts to read an 
     * invalid nc file. 
     * This catches the invalid file if NcHelper has
     *   ucar.nc2.iosp.netcdf3.N3header.disallowFileTruncation = true;
     * but fails to catch it if 
     *   ucar.nc2.iosp.netcdf3.N3header.disallowFileTruncation = false; //the default
     */
    public static void testInvalidShortenedNcFile() throws Throwable {

        String2.log("\n*** EDDGridFromNcFiles.testInvalidShortenedNcFile()\n");
        testVerboseOff();
        String fileName = EDStatic.unitTestDataDir + "nc/invalidShortened.nc"; //header is intact
        String id = "testBadNcFile";

        NetcdfFile ncFile = null; 
        try {
            ncFile = NetcdfFile.open(fileName); //this is what fails with 1/10000th file
            System.out.println("1) Shouldn't get here!"); //It doesn't get here. Good! Test is done early on.

            Variable var = ncFile.findVariable("MWcdom");  //size=[1,1,2321,4001]
            System.out.println("2) Shouldn't get here!");

            Array array = var.read();          
            System.out.println("shape=" + Arrays.toString(array.getShape()));
            System.out.println("3) Shouldn't get here!");

            array = var.read("0,0,0:2000:1000,0:4000:1000");
            System.out.println("shape=" + Arrays.toString(array.getShape()));

            //System.out.println(array.toString());
            //This does happen if disallowFileTruncation = false;
            System.out.println("4) Shouldn't get here!");
            String2.pressEnterToContinue(); 
            ncFile.close();
        } catch (Throwable t) {
            //expected
            try {
                if (ncFile != null)
                    ncFile.close();
            } catch (Throwable t2) {
                //don't care
            }
            String msg = t.toString();
            String2.log("caught: " + msg);
            Test.ensureEqual(msg,
                "java.io.IOException: java.io.IOException: File is truncated calculated size= 37201448 actual = 6200241",
                "");
        }
    }


    /**
     * This tests that a dataset can be quick restarted, 
     */
    public static void testQuickRestart2() throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testQuickRestart2\n");
        String datasetID = "testGriddedNcFiles";
        String fullName = EDStatic.unitTestDataDir + 
            "erdQSwind1day/subfolder/erdQSwind1day_20080108_10.nc";
        long timestamp = File2.getLastModified(fullName); //orig 2009-01-07T11:55 local
        try {
            //restart local erddap
            String2.pressEnterToContinue(
                "Restart the local erddap with quickRestart=true and with datasetID=" +
                datasetID + " .\n" +
                "Wait until all datasets are loaded.");

            //change the file's timestamp
            File2.setLastModified(fullName, timestamp - 60000); //1 minute earlier
            Math2.sleep(1000);

            //request info from that dataset
            //.csv  with data from one file
            String2.log("\n*** .nc test read from one file\n");       
            String userDapQuery = "y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]";
            String results = SSR.getUrlResponseStringUnchanged(
                EDStatic.erddapUrl + "/griddap/" + datasetID + ".csv?" + userDapQuery);
            String expected = 
//verified with 
//https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.csv?y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]
"time,altitude,latitude,longitude,y_wind\n" +
"UTC,m,degrees_north,degrees_east,m s-1\n" +
"2008-01-10T12:00:00Z,0.0,36.625,230.125,3.555585\n" +
"2008-01-10T12:00:00Z,0.0,36.625,230.875,2.82175\n" +
"2008-01-10T12:00:00Z,0.0,36.625,231.625,4.539375\n" +
"2008-01-10T12:00:00Z,0.0,36.625,232.375,4.975015\n" +
"2008-01-10T12:00:00Z,0.0,36.625,233.125,5.643055\n" +
"2008-01-10T12:00:00Z,0.0,36.625,233.875,2.72394\n" +
"2008-01-10T12:00:00Z,0.0,36.625,234.625,1.39762\n" +
"2008-01-10T12:00:00Z,0.0,36.625,235.375,2.10711\n" +
"2008-01-10T12:00:00Z,0.0,36.625,236.125,3.019165\n" +
"2008-01-10T12:00:00Z,0.0,36.625,236.875,3.551915\n" +
"2008-01-10T12:00:00Z,0.0,36.625,237.625,NaN\n";          //test of NaN
            Test.ensureEqual(results, expected, "\nresults=\n" + results);

            //request status.html
            SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/status.html");
            Math2.sleep(1000);
            SSR.displayInBrowser("file://" + EDStatic.bigParentDirectory + "logs/log.txt");

            String2.pressEnterToContinue(
                "Look at log.txt to see if update was run and successfully "+
                "noticed the changed file.");

        } finally {
            //change timestamp back to original
            File2.setLastModified(fullName, timestamp);
        }
    }

    /** 
     * The request size will be ~22MB/timePoint.
     * If partialRequestMaxBytes=100000000  (10^8),
     * <br>Small request (e.g., 1,2,3,4) will be handled with one source query.
     * <br>Large request (e.g., 6+, depending on setup.xml partialRequestMaxBytes)
     * <br>  will be handled with multiple source queries (one per timePoint).
     *
     * <p>Note that THREDDS has a default limit of 500MB for opendap responses.
     * https://www.unidata.ucar.edu/software/thredds/current/tds/reference/ThreddsConfigXMLFile.html#opendap
     * partialRequestMaxBytes=10^8 stays well under that.
     * 
     * @param extectedTimeS the expected time in seconds
     */
    public static void testBigRequestSpeed(int nTimePoints, String fileType, 
        int expectedBytes, int expectedTimeS) throws Throwable {
        testVerboseOn();
        Math2.gcAndWait();  //in a test
        Math2.gcAndWait();  //in a test
        String msg ="\n*** EDDGridFromDap.testBigRequestSpeed(nTimePoints=" + nTimePoints + ", " + fileType + ")" +
            "\n  partialRequestMaxBytes=" + EDStatic.partialRequestMaxBytes + 
                " estimated nPartialRequests=" + 
            Math2.hiDiv(nTimePoints * 4320 * 8640, EDStatic.partialRequestMaxBytes) +
            "\n  expected size=" + expectedBytes + "  expected time=" + expectedTimeS + "s but it can be 10x slower\n";
        String2.log(msg);
        StringBuilder results = new StringBuilder(msg);

        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, "nceiPH53sstd1day"); 
        String query = "sea_surface_temperature[0:" + (nTimePoints - 1) + "][][]";
        String dir = EDStatic.fullTestCacheDirectory;
        String tName;

        //tName = eddGrid.makeNewFileForDapQuery(null, null, query,
        //    dir, eddGrid.className() + "_testBigRequest", fileType); 

        //debugMode AFTER first request
        GridDataAccessor.debugMode = true;
        for (int i = 3; i >= -3; i--) {  //usually +3 -3
            if (i == 0)
                continue;
            try {
                eddGrid.nThreads = Math.abs(i);
                //time the second request
                long time = System.currentTimeMillis();
                tName = eddGrid.makeNewFileForDapQuery(null, null, query,
                    dir, eddGrid.className() + "_testBigRequest2", fileType); 
                msg = "  nThreads=" + eddGrid.nThreads + " size=" + File2.length(dir + tName) +  
                    " time=" + (System.currentTimeMillis() - time)/1000 + "ms\n";
                String2.log(msg);
                results.append(msg);
            } catch (Throwable t) {
                String2.log("caught:\n" + MustBe.throwableToString(t));
            }
        }
        GridDataAccessor.debugMode = false;
        throw new RuntimeException("\n*** Not a problem, just a diagnostic:\n" + 
            results.toString());
/* 2018-08-14 results  
*** nThreads=3 time= 57ms 
*** nThreads=2 time=109ms
*** nThreads=1 time= 94ms 
*** nThreads=1 time= 79ms 
*** nThreads=2 time= 82ms 
*/
    }

    /**
     * This tests makeCopyFileTasks.
     */
    public static void testMakeCopyFileTasks(boolean deleteAllLocalFiles) throws Exception {

        String2.log("\n*** EDDGridFromNcFiles.testMakeCopyFileTasks");
        FileVisitorDNLS.verbose = true;
        FileVisitorDNLS.reallyVerbose = true;
        FileVisitorDNLS.debugMode=true;

        boolean testMode = false;
        boolean tRecursive = true;
        boolean tDirectoriesToo = false;
        String tSourceUrl = "https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/"; //contents.html
        String tFileNameRegex = "[0-9]{14}-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02\\.0-fv04\\.1\\.nc";
        boolean recursive = true;
        //or String tPathRegex = ".*/v4\\.1/(|2018/(|01./))";  //for test, just get 01x dirs/files. Read regex: "(|2018/(|/[0-9]{3}/))";
        String tPathRegex = ".*/v4\\.1/(|2018/(|01[012]/))";  //for test, just get 01[012] dirs/files. Read regex: "(|2018/(|/[0-9]{3}/))";
        String tLocalDir = "/u00/data/points/testEDDGridMakeCopyFileTasks/";
        String tDatasetID = "testEDDGridMakeCopyFileTasks";
        String results, expected;

        //what does oneStep see in source?
        String2.log("What does one step see in source?");

        results = FileVisitorDNLS.oneStep(tSourceUrl, tFileNameRegex, tRecursive,
            tPathRegex, tDirectoriesToo).dataToString();
expected = 
"directory,name,lastModified,size\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/2018/010/,20180110090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc,1526396428000,400940089\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/2018/011/,20180111090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc,1526400024000,402342953\n" +
"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/2018/012/,20180112090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc,1526403626000,407791965\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        if (deleteAllLocalFiles) {
            File2.deleteAllFiles(tLocalDir, true, true);
        } else {
            //delete one of the local files
            File2.delete(tLocalDir + "v4.1/2018/011/20180111090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc");

            //what does oneStep see locally?
            String2.log("What does one step see in tLocalDir=" + tLocalDir + " ?");
            results = FileVisitorDNLS.oneStep(tLocalDir, tFileNameRegex, tRecursive,
                tPathRegex, tDirectoriesToo).dataToString();
    expected = 
    "directory,name,lastModified,size\n" +
    "/u00/data/points/testEDDGridMakeCopyFileTasks/v4.1/2018/010/,20180110090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc,1526396428000,400940089\n" +
    "/u00/data/points/testEDDGridMakeCopyFileTasks/v4.1/2018/012/,20180112090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc,1526403626000,407791965\n";
            Test.ensureEqual(results, expected, "results=\n" + results);
        }

        //refresh the local copy
        int nTasks = EDStatic.makeCopyFileTasks(tDatasetID, 
            EDStatic.DefaultMaxMakeCopyFileTasks, tDatasetID, 
            tSourceUrl, tFileNameRegex, tRecursive, tPathRegex, tLocalDir);
        int expectedN = deleteAllLocalFiles? 3 : 1;
        Test.ensureEqual(nTasks, expectedN, "nFilesToDownload");
        String2.pressEnterToContinue("\nPress Enter when the download tasks are finished (" +
            expectedN + " minutes?).");
        results = FileVisitorDNLS.oneStep(tLocalDir, 
            tFileNameRegex, tRecursive, tPathRegex, false).dataToString();
expected = 
"directory,name,lastModified,size\n" +
"/u00/data/points/testEDDGridMakeCopyFileTasks/v4.1/2018/010/,20180110090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc,1526396428000,400940089\n" +
"/u00/data/points/testEDDGridMakeCopyFileTasks/v4.1/2018/011/,20180111090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc,1526400024000,402342953\n" +
"/u00/data/points/testEDDGridMakeCopyFileTasks/v4.1/2018/012/,20180112090000-JPL-L4_GHRSST-SSTfnd-MUR-GLOB-v02.0-fv04.1.nc,1526403626000,407791965\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        String2.log("\n*** EDDGridFromNcFiles.testMakeCopyFileTasks finished successfully");

        FileVisitorDNLS.debugMode=false;

    }

    /** This tests generateDatasetsXml with cacheFromUrl. 
     * @throws Throwable if touble
     */
    public static void testGenerateDatasetsXmlCopy() throws Throwable {

        String2.log("\n*** EDDGridFromNcFiles.testGenerateDatasetsXmlCopy\n" +
            "*** This needs erdMWchla1day in localhost ERDDAP.");

        boolean testMode = false;
        boolean tRecursive = true;
        boolean tDirectoriesToo = false;
        String tSourceUrl = "http://localhost:8080/cwexperimental/files/erdMWchla1day/"; //contents.html
        String tFileNameRegex = "MW200219.*\\.nc(|\\.gz)"; //only 10 files match
        boolean recursive = true;
        String tLocalDir = "/u00/data/points/testEDDGridCopyFiles/";
        String results, expected;

        //delete all cached files
        File2.deleteAllFiles(tLocalDir, true, true);

//      public static String generateDatasetsXml(
//          String tFileDir, String tFileNameRegex, String sampleFileName, 
//          int tReloadEveryNMinutes, String tCacheFromUrl,
//          Attributes externalAddGlobalAttributes) throws Throwable {
        results = generateDatasetsXml(
            tLocalDir, tFileNameRegex, "", 
            "", //group
            "", DEFAULT_RELOAD_EVERY_N_MINUTES, tSourceUrl, null) + "\n";  //dimensionsCSV, reloadMinutes, cacheFromUrl

        expected = 
"<dataset type=\"EDDGridFromNcFiles\" datasetID=\"testEDDGridCopyFiles_2192_6b1a_7764\" active=\"true\">\n" +
"    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" +
"    <cacheFromUrl>http://localhost:8080/cwexperimental/files/erdMWchla1day/</cacheFromUrl>\n" +
"    <fileDir>/u00/data/points/testEDDGridCopyFiles/</fileDir>\n" +
"    <fileNameRegex>MW200219.*\\.nc(|\\.gz)</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <matchAxisNDigits>20</matchAxisNDigits>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"acknowledgement\">NOAA NESDIS COASTWATCH, NOAA SWFSC ERD</att>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"cols\" type=\"int\">4001</att>\n" +
"        <att name=\"composite\">true</att>\n" +
"        <att name=\"contributor_name\">NASA GSFC (OBPG)</att>\n" +
"        <att name=\"contributor_role\">Source of level 2 data.</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.0, Unidata Dataset Discovery v1.0, CWHDF</att>\n" +
"        <att name=\"creator_email\">dave.foley@noaa.gov</att>\n" + //that's what's in the file
"        <att name=\"creator_name\">NOAA CoastWatch, West Coast Node</att>\n" +
"        <att name=\"creator_url\">http://coastwatch.pfel.noaa.gov</att>\n" +
"        <att name=\"cwhdf_version\">3.4</att>\n" +
"        <att name=\"date_created\">2012-03-17Z</att>\n" +
"        <att name=\"date_issued\">2012-03-17Z</att>\n" +
"        <att name=\"Easternmost_Easting\" type=\"double\">255.0</att>\n" +
"        <att name=\"et_affine\" type=\"doubleList\">0.0 0.0125 0.0125 0.0 205.0 22.0</att>\n" +
"        <att name=\"gctp_datum\" type=\"int\">12</att>\n" +
"        <att name=\"gctp_parm\" type=\"doubleList\">0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0</att>\n" +
"        <att name=\"gctp_sys\" type=\"int\">0</att>\n" +
"        <att name=\"gctp_zone\" type=\"int\">0</att>\n" +
"        <att name=\"geospatial_lat_max\" type=\"double\">51.0</att>\n" +
"        <att name=\"geospatial_lat_min\" type=\"double\">22.0</att>\n" +
"        <att name=\"geospatial_lat_resolution\" type=\"double\">0.0125</att>\n" +
"        <att name=\"geospatial_lat_units\">degrees_north</att>\n" +
"        <att name=\"geospatial_lon_max\" type=\"double\">255.0</att>\n" +
"        <att name=\"geospatial_lon_min\" type=\"double\">205.0</att>\n" +
"        <att name=\"geospatial_lon_resolution\" type=\"double\">0.0125</att>\n" +
"        <att name=\"geospatial_lon_units\">degrees_east</att>\n" +
"        <att name=\"geospatial_vertical_max\" type=\"double\">0.0</att>\n" +
"        <att name=\"geospatial_vertical_min\" type=\"double\">0.0</att>\n" +
"        <att name=\"geospatial_vertical_positive\">up</att>\n" +
"        <att name=\"geospatial_vertical_units\">m</att>\n" +
"        <att name=\"history\">NASA GSFC (OBPG)\n" +
"2012-03-17T20:11:50Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD</att>\n" +
"        <att name=\"id\">LMWchlaS1day_20020709120000</att>\n" +
"        <att name=\"institution\">NOAA CoastWatch, West Coast Node</att>\n" +
"        <att name=\"keywords\">EARTH SCIENCE &gt; Oceans &gt; Ocean Chemistry &gt; Chlorophyll</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.</att>\n" +
"        <att name=\"naming_authority\">gov.noaa.pfel.coastwatch</att>\n" +
"        <att name=\"Northernmost_Northing\" type=\"double\">51.0</att>\n" +
"        <att name=\"origin\">NASA GSFC (OBPG)</att>\n" +
"        <att name=\"pass_date\" type=\"int\">11877</att>\n" +
"        <att name=\"polygon_latitude\" type=\"doubleList\">22.0 51.0 51.0 22.0 22.0</att>\n" +
"        <att name=\"polygon_longitude\" type=\"doubleList\">205.0 205.0 255.0 255.0 205.0</att>\n" +
"        <att name=\"processing_level\">3</att>\n" +
"        <att name=\"project\">CoastWatch (http://coastwatch.noaa.gov/)</att>\n" +
"        <att name=\"projection\">geographic</att>\n" +
"        <att name=\"projection_type\">mapped</att>\n" +
"        <att name=\"references\">Aqua/MODIS information: http://oceancolor.gsfc.nasa.gov/ . MODIS information: http://coastwatch.noaa.gov/modis_ocolor_overview.html .</att>\n" +
"        <att name=\"rows\" type=\"int\">2321</att>\n" +
"        <att name=\"satellite\">Aqua</att>\n" +
"        <att name=\"sensor\">MODIS</att>\n" +
"        <att name=\"source\">satellite observation: Aqua, MODIS</att>\n" +
"        <att name=\"Southernmost_Northing\" type=\"double\">22.0</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF-1.0</att>\n" +
"        <att name=\"start_time\" type=\"double\">0.0</att>\n" +
"        <att name=\"summary\">NOAA CoastWatch distributes chlorophyll-a concentration data from NASA&#39;s Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.  The algorithm currently used in processing the water leaving radiance to chlorophyll concentration has not yet been accepted as science quality.  In addition, assumptions are made in the atmospheric correction in order to provide the data in a timely manner.</att>\n" +
"        <att name=\"time_coverage_end\">2002-07-10T00:00:00Z</att>\n" +
"        <att name=\"time_coverage_start\">2002-07-09T00:00:00Z</att>\n" +
"        <att name=\"title\">Chlorophyll-a, Aqua MODIS, NPP, 0.0125 degrees, West US, EXPERIMENTAL</att>\n" +
"        <att name=\"Westernmost_Easting\" type=\"double\">205.0</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cols\">null</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"creator_email\">erd.data@noaa.gov</att>\n" +
"        <att name=\"creator_type\">institution</att>\n" +
"        <att name=\"creator_url\">https://coastwatch.pfeg.noaa.gov</att>\n" +
"        <att name=\"cwhdf_version\">null</att>\n" +
"        <att name=\"date_created\">2012-03-17</att>\n" +
"        <att name=\"date_issued\">2012-03-17</att>\n" +
"        <att name=\"et_affine\">null</att>\n" +
"        <att name=\"gctp_datum\">null</att>\n" +
"        <att name=\"gctp_parm\">null</att>\n" +
"        <att name=\"gctp_sys\">null</att>\n" +
"        <att name=\"gctp_zone\">null</att>\n" +
"        <att name=\"infoUrl\">https://coastwatch.pfeg.noaa.gov</att>\n" +
"        <att name=\"institution\">NOAA CoastWatch WCN</att>\n" +
"        <att name=\"keywords\">altitude, aqua, chemistry, chla, chlorophyll, chlorophyll-a, coast, coastwatch, color, concentration, concentration_of_chlorophyll_in_sea_water, data, degrees, earth, Earth Science &gt; Oceans &gt; Ocean Chemistry &gt; Chlorophyll, experimental, imaging, moderate, modis, MWchla, national, noaa, node, npp, ocean, ocean color, oceans, orbiting, partnership, polar, polar-orbiting, resolution, science, sea, seawater, spectroradiometer, time, US, water, wcn, west</att>\n" +
"        <att name=\"naming_authority\">gov.noaa.pfeg.coastwatch</att>\n" +
"        <att name=\"pass_date\">null</att>\n" +
"        <att name=\"polygon_latitude\">null</att>\n" +
"        <att name=\"polygon_longitude\">null</att>\n" +
"        <att name=\"project\">CoastWatch (https://coastwatch.noaa.gov/)</att>\n" +
"        <att name=\"references\">Aqua/MODIS information: https://oceancolor.gsfc.nasa.gov/ . MODIS information: https://coastwatch.noaa.gov/modis_ocolor_overview.html .</att>\n" +
"        <att name=\"rows\">null</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" +
"        <att name=\"start_time\">null</att>\n" +
"    </addAttributes>\n" +
"    <axisVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Time</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">1.026216E9 1.026216E9</att>\n" +
"            <att name=\"axis\">T</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">0</att>\n" +
"            <att name=\"long_name\">Centered Time</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>altitude</sourceName>\n" +
"        <destinationName>altitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Height</att>\n" +
"            <att name=\"_CoordinateZisPositive\">up</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">0.0 0.0</att>\n" +
"            <att name=\"axis\">Z</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">0</att>\n" +
"            <att name=\"long_name\">Altitude</att>\n" +
"            <att name=\"positive\">up</att>\n" +
"            <att name=\"standard_name\">altitude</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>lat</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Lat</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">22.0 51.0</att>\n" +
"            <att name=\"axis\">Y</att>\n" +
"            <att name=\"coordsys\">geographic</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">4</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"            <att name=\"point_spacing\">even</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>lon</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Lon</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">205.0 255.0</att>\n" +
"            <att name=\"axis\">X</att>\n" +
"            <att name=\"coordsys\">geographic</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">4</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"            <att name=\"point_spacing\">even</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>MWchla</sourceName>\n" +
"        <destinationName>MWchla</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.001 466.74</att>\n" +
"            <att name=\"coordsys\">geographic</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">2</att>\n" +
"            <att name=\"long_name\">Chlorophyll-a, Aqua MODIS, NPP, 0.0125 degrees, West US, EXPERIMENTAL</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"numberOfObservations\" type=\"int\">2260568</att>\n" +
"            <att name=\"percentCoverage\" type=\"double\">0.24342987928157986</att>\n" +
"            <att name=\"standard_name\">concentration_of_chlorophyll_in_sea_water</att>\n" +
"            <att name=\"units\">mg m-3</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">30.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.03</att>\n" +
"            <att name=\"colorBarScale\">Log</att>\n" +
"            <att name=\"ioos_category\">Ocean Color</att>\n" +
"            <att name=\"numberOfObservations\">null</att>\n" +
"            <att name=\"percentCoverage\">null</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";
        Test.ensureEqual(results, expected, 
            "results.length=" + results.length() + " expected.length=" + expected.length() + 
            "\nresults=\n" + results);

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDGridFromNcFiles",
            tLocalDir, tFileNameRegex, "", "",
            "", //group
            "" + DEFAULT_RELOAD_EVERY_N_MINUTES, tSourceUrl},  //cacheFromUrl
            false); //doIt loop?
        Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt. " + 
            gdxResults.length() + " " + results.length());


        //ensure it is ready-to-use by making a dataset from it
        String tDatasetID = "testEDDGridCopyFiles_2192_6b1a_7764";
        EDD.deleteCachedDatasetInfo(tDatasetID);
        EDD edd = oneFromXmlFragment(null, results);
        Math2.sleep(9000);
        String2.pressEnterToContinue("\nWhen the taskThreads have finished, press Enter.");
        edd = oneFromXmlFragment(null, results);
        Test.ensureEqual(edd.datasetID(), tDatasetID, "");
        Test.ensureEqual(edd.title(), 
            "Chlorophyll-a, Aqua MODIS, NPP, 0.0125 degrees, West US, EXPERIMENTAL", "");
        Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
            "MWchla", "");

        String2.log("\nEDDGridFromNcFiles.testGenerateDatasetsXmlCopy passed the test.");
    }

    /**
     * This tests cacheFromUrl where the entire dataset is copied.
     *
     * @throws Throwable if trouble
     */
    public static void testCopyFiles(boolean deleteDataFiles) throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testCopyFiles(" + deleteDataFiles + ")\n" +
            "If deleteDataFiles is true, this requires erdMWchla1day in localhost ERDDAP.");
        testVerboseOn();
FileVisitorDNLS.verbose = true;
FileVisitorDNLS.reallyVerbose = true;
FileVisitorDNLS.debugMode = true;

        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        int po;
        EDV edv;

        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
        String tDir = EDStatic.fullTestCacheDirectory;
        String id = "testEDDGridCopyFiles";
        if (deleteDataFiles) {
            File2.deleteAllFiles("/u00/data/points/testEDDGridCopyFiles/", true, true);
            deleteCachedDatasetInfo(id);
        }

        //first attempt will start file downloads (but if deleteCachedInfo, will fail to load)
        EDDGrid eddGrid;
        try {
            eddGrid = (EDDGrid)oneFromDatasetsXml(null, id); 
            //no error if deleteCachedInfo=false
        } catch (Exception e2) {            
            String2.log(
                (deleteDataFiles? "Caught e" : "Un") +
                "xpected error:\n" + MustBe.throwableToString(e2));
            if (deleteDataFiles) {
                Math2.sleep(5000);
                String2.pressEnterToContinue("Wait for taskThread tasks to finish, then:");
            } else {
                System.exit(1);
            }
            eddGrid = (EDDGrid)oneFromDatasetsXml(null, id); 
        }

        //*** test getting das for entire dataset
        String2.log("\n****************** EDDGridCopyFiles  das and dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", tDir, 
            eddGrid.className() + "_copyFiles_Entire", ".das"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.026216e+9, 1.0269936e+9;\n" +
"    String axis \"T\";\n" +
"    Int32 fraction_digits 0;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Centered Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    Float64 actual_range 0.0, 0.0;\n" +
"    String axis \"Z\";\n" +
"    Int32 fraction_digits 0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range 22.0, 51.0;\n" +
"    String axis \"Y\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 4;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String point_spacing \"even\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range 205.0, 255.0;\n" +
"    String axis \"X\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 4;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String point_spacing \"even\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  MWchla {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float64 colorBarMaximum 30.0;\n" +
"    Float64 colorBarMinimum 0.03;\n" +
"    String colorBarScale \"Log\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 2;\n" +
"    String ioos_category \"Ocean Color\";\n" +
"    String long_name \"Chlorophyll-a, Aqua MODIS, NPP, 0.0125 degrees, West US, EXPERIMENTAL\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"concentration_of_chlorophyll_in_sea_water\";\n" +
"    String units \"mg m-3\";\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String acknowledgement \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String composite \"true\";\n" +
"    String contributor_name \"NASA GSFC (OBPG)\";\n" +
"    String contributor_role \"Source of level 2 data.\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String creator_email \"erd.data@noaa.gov\";\n" +
"    String creator_name \"NOAA CoastWatch, West Coast Node\";\n" +
"    String creator_type \"institution\";\n" +
"    String creator_url \"https://coastwatch.pfeg.noaa.gov\";\n" +
"    String date_created \"2012-03-17\";\n" +
"    String date_issued \"2012-03-17\";\n" +
"    Float64 Easternmost_Easting 255.0;\n" +
"    Float64 geospatial_lat_max 51.0;\n" +
"    Float64 geospatial_lat_min 22.0;\n" +
"    Float64 geospatial_lat_resolution 0.0125;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 255.0;\n" +
"    Float64 geospatial_lon_min 205.0;\n" +
"    Float64 geospatial_lon_resolution 0.0125;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    Float64 geospatial_vertical_max 0.0;\n" +
"    Float64 geospatial_vertical_min 0.0;\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"NASA GSFC (OBPG)\n" +
"2012-03-17T20:15:52Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD\n";
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
    
expected = 
    "String infoUrl \"https://coastwatch.pfeg.noaa.gov\";\n" +
"    String institution \"NOAA CoastWatch WCN\";\n" +
"    String keywords \"altitude, aqua, chemistry, chla, chlorophyll, chlorophyll-a, coast, coastwatch, color, concentration, concentration_of_chlorophyll_in_sea_water, data, degrees, earth, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, experimental, imaging, moderate, modis, MWchla, national, noaa, node, npp, ocean, ocean color, oceans, orbiting, partnership, polar, polar-orbiting, resolution, science, sea, seawater, spectroradiometer, time, US, water, wcn, west\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.\";\n" +
"    String naming_authority \"gov.noaa.pfeg.coastwatch\";\n" +
"    Float64 Northernmost_Northing 51.0;\n" +
"    String origin \"NASA GSFC (OBPG)\";\n" +
"    String processing_level \"3\";\n" +
"    String project \"CoastWatch (https://coastwatch.noaa.gov/)\";\n" +
"    String projection \"geographic\";\n" +
"    String projection_type \"mapped\";\n" +
"    String references \"Aqua/MODIS information: https://oceancolor.gsfc.nasa.gov/ . MODIS information: https://coastwatch.noaa.gov/modis_ocolor_overview.html .\";\n" +
"    String satellite \"Aqua\";\n" +
"    String sensor \"MODIS\";\n" +
"    String source \"satellite observation: Aqua, MODIS\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing 22.0;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String summary \"NOAA CoastWatch distributes chlorophyll-a concentration data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.  The algorithm currently used in processing the water leaving radiance to chlorophyll concentration has not yet been accepted as science quality.  In addition, assumptions are made in the atmospheric correction in order to provide the data in a timely manner.\";\n" +
"    String time_coverage_end \"2002-07-18T12:00:00Z\";\n" +
"    String time_coverage_start \"2002-07-09T12:00:00Z\";\n" +
"    String title \"Chlorophyll-a, Aqua MODIS, NPP, 0.0125 degrees, West US, EXPERIMENTAL\";\n" +
"    Float64 Westernmost_Easting 205.0;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);


        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", tDir, 
            eddGrid.className() + "_copyFiles_Entire", ".dds"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Float64 time[time = 10];\n" +
"  Float64 altitude[altitude = 1];\n" +
"  Float64 latitude[latitude = 2321];\n" +
"  Float64 longitude[longitude = 4001];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 MWchla[time = 10][altitude = 1][latitude = 2321][longitude = 4001];\n" +
"    MAPS:\n" +
"      Float64 time[time = 10];\n" +
"      Float64 altitude[altitude = 1];\n" +
"      Float64 latitude[latitude = 2321];\n" +
"      Float64 longitude[longitude = 4001];\n" +
"  } MWchla;\n" +
"} testEDDGridCopyFiles;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
 

        //*** test make data files
        String2.log("\n****************** EDDGridCopyFiles make DATA FILES\n");       

        //.csv   
        userDapQuery = 
            "MWchla[(2002-07-09T12:00:00Z)][][0:1000:last][0:1000:last]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            eddGrid.className() + "_copyFiles_data", ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"time,altitude,latitude,longitude,MWchla\n" +
"UTC,m,degrees_north,degrees_east,mg m-3\n" +
"2002-07-09T12:00:00Z,0.0,22.0,205.0,NaN\n" +
"2002-07-09T12:00:00Z,0.0,22.0,217.5,NaN\n" +
"2002-07-09T12:00:00Z,0.0,22.0,230.0,NaN\n" +
"2002-07-09T12:00:00Z,0.0,22.0,242.5,0.07\n" +
"2002-07-09T12:00:00Z,0.0,22.0,255.0,NaN\n" +
"2002-07-09T12:00:00Z,0.0,34.5,205.0,0.086\n" +
"2002-07-09T12:00:00Z,0.0,34.5,217.5,NaN\n" +
"2002-07-09T12:00:00Z,0.0,34.5,230.0,NaN\n" +
"2002-07-09T12:00:00Z,0.0,34.5,242.5,NaN\n" +
"2002-07-09T12:00:00Z,0.0,34.5,255.0,NaN\n" +
"2002-07-09T12:00:00Z,0.0,47.0,205.0,NaN\n" +
"2002-07-09T12:00:00Z,0.0,47.0,217.5,0.192\n" +
"2002-07-09T12:00:00Z,0.0,47.0,230.0,0.266\n" +
"2002-07-09T12:00:00Z,0.0,47.0,242.5,NaN\n" +
"2002-07-09T12:00:00Z,0.0,47.0,255.0,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        String2.log("\n*** EDDGridFromNcFiles.testCopyFiles() finished successfully.");
FileVisitorDNLS.verbose = false;
FileVisitorDNLS.reallyVerbose = false;
FileVisitorDNLS.debugMode = false;
        /* */
    }

    /**
     * This tests cacheFromUrl where the dataset uses cacheSizeGB=1.
     *
     * @throws Throwable if trouble
     */
    public static void testCacheFiles(boolean deleteDataFiles) throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testCacheFiles(" + deleteDataFiles + ")\n" +
            "This requires erdMWchla1day in localhost ERDDAP.");
        testVerboseOn();
FileVisitorDNLS.verbose = true;
FileVisitorDNLS.reallyVerbose = true;
FileVisitorDNLS.debugMode = true;

        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        int po;
        EDV edv;

        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
        String tDir = EDStatic.fullTestCacheDirectory;
        String id = "testEDDGridCacheFiles";
        deleteCachedDatasetInfo(id); //always
        if (deleteDataFiles)
            File2.deleteAllFiles("/u00/data/points/testEDDGridCacheFiles/", true, true);

        EDDGrid eddGrid;
        eddGrid = (EDDGrid)oneFromDatasetsXml(null, id); 

        //*** test getting das for entire dataset
        String2.log("\n****************** EDDGridCacheFiles  das and dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", tDir, 
            eddGrid.className() + "_cacheFiles_Entire", ".das"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.025784e+9, 1.030536e+9;\n" +
"    String axis \"T\";\n" +
"    Int32 fraction_digits 0;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Centered Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    Float64 actual_range 0.0, 0.0;\n" +
"    String axis \"Z\";\n" +
"    Int32 fraction_digits 0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range 22.0, 51.0;\n" +
"    String axis \"Y\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 4;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String point_spacing \"even\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range 205.0, 255.0;\n" +
"    String axis \"X\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 4;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String point_spacing \"even\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  MWchla {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float64 colorBarMaximum 30.0;\n" +
"    Float64 colorBarMinimum 0.03;\n" +
"    String colorBarScale \"Log\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 2;\n" +
"    String ioos_category \"Ocean Color\";\n" +
"    String long_name \"Chlorophyll-a, Aqua MODIS, NPP, 0.0125 degrees, West US, EXPERIMENTAL\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"concentration_of_chlorophyll_in_sea_water\";\n" +
"    String units \"mg m-3\";\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String acknowledgement \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String composite \"true\";\n" +
"    String contributor_name \"NASA GSFC (OBPG)\";\n" +
"    String contributor_role \"Source of level 2 data.\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String creator_email \"erd.data@noaa.gov\";\n" +
"    String creator_name \"NOAA CoastWatch, West Coast Node\";\n" +
"    String creator_type \"institution\";\n" +
"    String creator_url \"https://coastwatch.pfeg.noaa.gov\";\n" +
"    String date_created \"2012-03-17\";\n" +
"    String date_issued \"2012-03-17\";\n" +
"    Float64 Easternmost_Easting 255.0;\n" +
"    Float64 geospatial_lat_max 51.0;\n" +
"    Float64 geospatial_lat_min 22.0;\n" +
"    Float64 geospatial_lat_resolution 0.0125;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 255.0;\n" +
"    Float64 geospatial_lon_min 205.0;\n" +
"    Float64 geospatial_lon_resolution 0.0125;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    Float64 geospatial_vertical_max 0.0;\n" +
"    Float64 geospatial_vertical_min 0.0;\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"NASA GSFC (OBPG)\n" +
"2012-03-17T20:33:28Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD\n";
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
    
expected = 
    "String infoUrl \"https://coastwatch.pfeg.noaa.gov\";\n" +
"    String institution \"NOAA CoastWatch WCN\";\n" +
"    String keywords \"altitude, aqua, chemistry, chla, chlorophyll, chlorophyll-a, coast, coastwatch, color, concentration, concentration_of_chlorophyll_in_sea_water, data, degrees, earth, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, experimental, imaging, moderate, modis, MWchla, national, noaa, node, npp, ocean, ocean color, oceans, orbiting, partnership, polar, polar-orbiting, resolution, science, sea, seawater, spectroradiometer, time, US, water, wcn, west\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.\";\n" +
"    String naming_authority \"gov.noaa.pfeg.coastwatch\";\n" +
"    Float64 Northernmost_Northing 51.0;\n" +
"    String origin \"NASA GSFC (OBPG)\";\n" +
"    String processing_level \"3\";\n" +
"    String project \"CoastWatch (https://coastwatch.noaa.gov/)\";\n" +
"    String projection \"geographic\";\n" +
"    String projection_type \"mapped\";\n" +
"    String references \"Aqua/MODIS information: https://oceancolor.gsfc.nasa.gov/ . MODIS information: https://coastwatch.noaa.gov/modis_ocolor_overview.html .\";\n" +
"    String satellite \"Aqua\";\n" +
"    String sensor \"MODIS\";\n" +
"    String source \"satellite observation: Aqua, MODIS\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing 22.0;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String summary \"NOAA CoastWatch distributes chlorophyll-a concentration data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.  The algorithm currently used in processing the water leaving radiance to chlorophyll concentration has not yet been accepted as science quality.  In addition, assumptions are made in the atmospheric correction in order to provide the data in a timely manner.\";\n" +
"    String time_coverage_end \"2002-08-28T12:00:00Z\";\n" +
"    String time_coverage_start \"2002-07-04T12:00:00Z\";\n" +
"    String title \"Chlorophyll-a, Aqua MODIS, NPP, 0.0125 degrees, West US, EXPERIMENTAL\";\n" +
"    Float64 Westernmost_Easting 205.0;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);


        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", tDir, 
            eddGrid.className() + "_cacheFiles_Entire", ".dds"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Float64 time[time = 49];\n" +
"  Float64 altitude[altitude = 1];\n" +
"  Float64 latitude[latitude = 2321];\n" +
"  Float64 longitude[longitude = 4001];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 MWchla[time = 49][altitude = 1][latitude = 2321][longitude = 4001];\n" +
"    MAPS:\n" +
"      Float64 time[time = 49];\n" +
"      Float64 altitude[altitude = 1];\n" +
"      Float64 latitude[latitude = 2321];\n" +
"      Float64 longitude[longitude = 4001];\n" +
"  } MWchla;\n" +
"} testEDDGridCacheFiles;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //*** test make data files
        String2.log("\n****************** EDDGridCacheFiles make DATA FILES\n");       

        //.csv that needs every file
        userDapQuery = "MWchla[0:last][][0][3000]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            eddGrid.className() + "_cacheFiles_data", ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"time,altitude,latitude,longitude,MWchla\n" +
"UTC,m,degrees_north,degrees_east,mg m-3\n" +
"2002-07-04T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-07-05T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-07-06T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-07-07T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-07-08T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-07-09T12:00:00Z,0.0,22.0,242.5,0.07\n" +
"2002-07-10T12:00:00Z,0.0,22.0,242.5,0.082\n" +
"2002-07-11T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-07-12T12:00:00Z,0.0,22.0,242.5,0.052\n" +
"2002-07-13T12:00:00Z,0.0,22.0,242.5,0.068\n" +
"2002-07-14T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-07-15T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-07-16T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-07-17T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-07-18T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-07-19T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-07-20T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-07-21T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-07-22T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-07-23T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-07-24T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-07-25T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-07-26T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-07-27T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-07-28T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-07-29T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-08-06T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-08-07T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-08-08T12:00:00Z,0.0,22.0,242.5,0.075\n" +
"2002-08-09T12:00:00Z,0.0,22.0,242.5,0.06\n" +
"2002-08-10T12:00:00Z,0.0,22.0,242.5,0.065\n" +
"2002-08-11T12:00:00Z,0.0,22.0,242.5,0.068\n" +
"2002-08-12T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-08-13T12:00:00Z,0.0,22.0,242.5,0.074\n" +
"2002-08-14T12:00:00Z,0.0,22.0,242.5,0.054\n" +
"2002-08-15T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-08-16T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-08-17T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-08-18T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-08-19T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-08-20T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-08-21T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-08-22T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-08-23T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-08-24T12:00:00Z,0.0,22.0,242.5,0.065\n" +
"2002-08-25T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-08-26T12:00:00Z,0.0,22.0,242.5,0.06\n" +
"2002-08-27T12:00:00Z,0.0,22.0,242.5,NaN\n" +
"2002-08-28T12:00:00Z,0.0,22.0,242.5,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        String2.log("\n*** EDDGridFromNcFiles.testCacheFiles() finished successfully.");
FileVisitorDNLS.verbose = false;
FileVisitorDNLS.reallyVerbose = false;
FileVisitorDNLS.debugMode = false;
        /* */
    }


    /**
     * This tests erdMH1chlamday, which uses axis0=***fileName
     *
     * @throws Throwable if trouble
     */
    public static void testFileName(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testFileName(" + deleteCachedDatasetInfo + ")");
        testVerboseOn();

        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        int po;
        EDV edv;

        String tDir = EDStatic.fullTestCacheDirectory;
        String id = "erdMH1chlamday";
        if (deleteCachedDatasetInfo)
            EDD.deleteCachedDatasetInfo(id);

        EDDGrid eddGrid;
        eddGrid = (EDDGrid)oneFromDatasetsXml(null, id); 

        //*** test getting das for entire dataset
        String2.log("\n****************** testFileName  das and dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", tDir, 
            eddGrid.className() + "_testFileName", ".das"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.0426752e+9, 1.0453536e+9;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Centered Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range -89.97918, 89.97916;\n" +
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
"    Float32 actual_range -179.9792, 179.9792;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"    Float32 valid_max 180.0;\n" +
"    Float32 valid_min -180.0;\n" +
"  }\n" +
"  chlorophyll {\n" +
"    Float32 _FillValue NaN;\n" +
"    Float64 colorBarMaximum 30.0;\n" +
"    Float64 colorBarMinimum 0.03;\n" +
"    String colorBarScale \"Log\";\n" +
"    String ioos_category \"Ocean Color\";\n" +
"    String long_name \"Mean Chlorophyll a Concentration\";\n" +
"    String references \"Hu, C., Lee Z., and Franz, B.A. (2012). Chlorophyll-a algorithms for oligotrophic oceans: A novel approach based on three-band reflectance difference, J. Geophys. Res., 117, C01011, doi:10.1029/2011JC007395.\";\n" +
"    String standard_name \"concentration_of_chlorophyll_in_sea_water\";\n" +
"    String units \"mg m-3\";\n" +
"    Float32 valid_max 100.0;\n" +
"    Float32 valid_min 0.001;\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String _lastModified \"2015-06-26T11:26:12.000Z\";\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String Conventions \"CF-1.6, COARDS, ACDD-1.3\";\n" +
"    String creator_email \"data@oceancolor.gsfc.nasa.gov\";\n" +
"    String creator_name \"NASA/GSFC/OBPG\";\n" +
"    String creator_type \"group\";\n" +
"    String creator_url \"https://oceandata.sci.gsfc.nasa.gov\";\n" +
"    String date_created \"2015-06-26T11:26:12.000Z\";\n" +
"    Float64 Easternmost_Easting 179.9792;\n" +
"    Float64 geospatial_lat_max 89.97916;\n" +
"    Float64 geospatial_lat_min -89.97918;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 179.9792;\n" +
"    Float64 geospatial_lon_min -179.9792;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String grid_mapping_name \"latitude_longitude\";\n" +
"    String history \"Files downloaded daily from https://oceandata.sci.gsfc.nasa.gov/MODIS-Aqua/L3SMI to NOAA SWFSC ERD (erd.data@noaa.gov)";
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
    
expected = 
    "String identifier_product_doi \"10.5067/AQUA/MODIS_OC.2014.0\";\n" +
"    String identifier_product_doi_authority \"https://dx.doi.org\";\n" +
"    String infoUrl \"https://coastwatch.pfeg.noaa.gov/infog/MH1_chla_las.html\";\n" +
"    String institution \"NASA/GSFC OBPG\";\n" +
"    String instrument \"MODIS\";\n" +
"    String keywords \"algorithm, biology, center, chemistry, chlor_a, chlorophyll, color, concentration, concentration_of_chlorophyll_in_sea_water, data, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, Earth Science > Oceans > Ocean Optics > Ocean Color, flight, goddard, group, gsfc, image, imaging, L3, level, level-3, mapped, moderate, modis, nasa, obpg, ocean, ocean color, oceans, oci, optics, processing, resolution, sea, seawater, smi, space, spectroradiometer, standard, time, water\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String l2_flag_names \"ATMFAIL,LAND,HILT,HISATZEN,STRAYLIGHT,CLDICE,COCCOLITH,LOWLW,CHLWARN,CHLFAIL,NAVWARN,MAXAERITER,ATMWARN,HISOLZEN,NAVFAIL,FILTER,HIGLINT\";\n" +
"    String license \"https://science.nasa.gov/earth-science/earth-science-data/data-information-policy/\n" +
"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String map_projection \"Equidistant Cylindrical\";\n" +
"    String measure \"Mean\";\n" +
"    String naming_authority \"gov.noaa.pfeg.coastwatch\";\n" +
"    Float64 Northernmost_Northing 89.97916;\n" +
"    String platform \"Aqua\";\n" +
"    String processing_level \"L3 Mapped\";\n" +
"    String processing_version \"2014.0\";\n" +
"    String product_name \"A20030322003059.L3m_MO_CHL_chlor_a_4km.nc\";\n" +
"    String project \"Ocean Biology Processing Group (NASA/GSFC/OBPG)\";\n" +
"    String publisher_email \"erd.data@noaa.gov\";\n" +
"    String publisher_name \"NOAA NMFS SWFSC ERD\";\n" +
"    String publisher_type \"institution\";\n" +
"    String publisher_url \"https://www.pfeg.noaa.gov\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing -89.97918;\n" +
"    String spatialResolution \"4.60 km\";\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String summary \"This dataset has Level 3, Standard Mapped Image, 4km, chlorophyll-a concentration data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.  This is Science Quality data.  This is the August 2015 version of this dataset.\";\n" +
"    String temporal_range \"month\";\n" +
"    String testOutOfDate \"now-70days\";\n" +
"    String time_coverage_end \"2003-02-16T00:00:00Z\";\n" +
"    String time_coverage_start \"2003-01-16T00:00:00Z\";\n" +
"    String title \"Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (Monthly Composite)\";\n" +
"    Float64 Westernmost_Easting -179.9792;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);


        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", tDir, 
            eddGrid.className() + "_testFileName", ".dds"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Float64 time[time = 2];\n" +
"  Float32 latitude[latitude = 4320];\n" +
"  Float32 longitude[longitude = 8640];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 chlorophyll[time = 2][latitude = 4320][longitude = 8640];\n" +
"    MAPS:\n" +
"      Float64 time[time = 2];\n" +
"      Float32 latitude[latitude = 4320];\n" +
"      Float32 longitude[longitude = 8640];\n" +
"  } chlorophyll;\n" +
"} erdMH1chlamday;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //*** test make data files
        String2.log("\n****************** testRepaceWithFileName make DATA FILES\n");       

        //time values
        userDapQuery = "time[]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            eddGrid.className() + "_testFileName" + "Time", ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"time\n" +
"UTC\n" +
"2003-01-16T00:00:00Z\n" +
"2003-02-16T00:00:00Z\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv that needs every file
        userDapQuery = "chlorophyll[0:last][1600:1601][10]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            eddGrid.className() + "_testFileName", ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"time,latitude,longitude,chlorophyll\n" +
"UTC,degrees_north,degrees_east,mg m-3\n" +
"2003-01-16T00:00:00Z,23.312494,-179.5625,0.076702\n" +
"2003-01-16T00:00:00Z,23.27083,-179.5625,0.074305\n" +
"2003-02-16T00:00:00Z,23.312494,-179.5625,0.067577\n" +
"2003-02-16T00:00:00Z,23.27083,-179.5625,0.068499\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        String2.log("\n*** EDDGridFromNcFiles.testFileName() finished successfully.");
        /* */
    }

    /**
     * This tests nceiPH53sstd1day, which uses axis0=***replaceFromFileName
     *
     * @throws Throwable if trouble
     */
    public static void testReplaceFromFileName(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testReplaceFromFileName(" + deleteCachedDatasetInfo + ")");
        testVerboseOn();

        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        int po;
        EDV edv;

        String tDir = EDStatic.fullTestCacheDirectory;
        String id = "nceiPH53sstd1day";
        if (deleteCachedDatasetInfo)
            EDD.deleteCachedDatasetInfo(id);

        EDDGrid eddGrid;
        eddGrid = (EDDGrid)oneFromDatasetsXml(null, id); 

        //*** test getting das for entire dataset
        String2.log("\n****************** testReplaceFromFileName  das and dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", tDir, 
            eddGrid.className() + "_testReplaceFromFileName", ".das"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 3.675888e+8, 3.681072e+8;\n" +
"    String axis \"T\";\n" +
"    String comment \"This is the centered, reference time.\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Centered Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range -89.97916, 89.97917;\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String reference_datum \"Geographical coordinates, WGS84 datum\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"    Float32 valid_max 90.0;\n" +
"    Float32 valid_min -90.0;\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 actual_range -179.9792, 179.9792;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String reference_datum \"Geographical coordinates, WGS84 datum\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"    Float32 valid_max 180.0;\n" +
"    Float32 valid_min -180.0;\n" +
"  }\n" +
"  sea_surface_temperature {\n" +
"    Float64 _FillValue -327.68;\n" +
"    String ancillary_variables \"quality_level pathfinder_quality_level l2p_flags\";\n" +
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"Skin temperature of the ocean\";\n" +
"    String coverage_content_type \"physicalMeasurement\";\n" +
"    String grid_mapping \"crs\";\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"NOAA Climate Data Record of sea surface skin temperature\";\n" +
"    String platform \"NOAA-7\";\n" +
"    String source \"AVHRR_GAC-CLASS-L1B-NOAA_07-v1\";\n" +
"    String standard_name \"sea_surface_skin_temperature\";\n" +
"    String units \"degree_C\";\n" +
"    Float64 valid_max 45.0;\n" +
"    Float64 valid_min -1.8;\n" +
"  }\n" +
"  dt_analysis {\n" +
"    Float64 _FillValue -12.8;\n" +
"    Float64 colorBarMaximum 5.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"The difference between this SST and the previous day's SST.\";\n" +
"    String coverage_content_type \"auxiliaryInformation\";\n" +
"    String grid_mapping \"crs\";\n" +
"    String ioos_category \"Statistics\";\n" +
"    String long_name \"deviation from last SST analysis\";\n" +
"    String platform \"NOAA-7\";\n" +
"    String references \"AVHRR_OI, with inland values populated from AVHRR_Pathfinder daily climatological SST. For more information on this reference field see https://data.nodc.noaa.gov/cgi-bin/iso?id=gov.noaa.nodc:0071180.\";\n" +
"    String source \"NOAA Daily 25km Global Optimally Interpolated Sea Surface Temperature (OISST)\";\n" +
"    String units \"degree_C\";\n" +
"    Float64 valid_max 12.700000000000001;\n" +  //2020-08-04 changed from NaN with change to maxIsMV support
"    Float64 valid_min -12.700000000000001;\n" +
"  }\n" +
"  wind_speed {\n" +
"    Byte _FillValue -128;\n" +
"    String _Unsigned \"false\";\n" + //ERDDAP adds
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"These wind speeds were created by NCEP-DOE Atmospheric Model Intercomparison Project (AMIP-II) reanalysis (R-2) and represent winds at 10 metres above the sea surface.\";\n" +
"    String coverage_content_type \"auxiliaryInformation\";\n" +
"    String grid_mapping \"crs\";\n" +
"    String height \"10 m\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"10m wind speed\";\n" +
"    String source \"NCEP/DOE AMIP-II Reanalysis (Reanalysis-2): u_wind.10m.gauss.1981.nc, v_wind.10m.gauss.1981.nc\";\n" +
"    String standard_name \"wind_speed\";\n" +
"    Float64 time_offset 2.0946;\n" +
"    String units \"m s-1\";\n" +
"    Byte valid_max 127;\n" +
"    Byte valid_min -127;\n" +
"  }\n" +
"  sea_ice_fraction {\n" +
"    Float64 _FillValue -1.28;\n" +
"    Float64 colorBarMaximum 1.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"Sea ice concentration data are taken from the EUMETSAT Ocean and Sea Ice Satellite Application Facility (OSISAF) Global Daily Sea Ice Concentration Reprocessing Data Set (https://data.nodc.noaa.gov/cgi-bin/iso?id=gov.noaa.nodc:0068294) when these data are available. The data are reprojected and interpolated from their original polar stereographic projection at 10km spatial resolution to the 4km Pathfinder Version 5.3 grid. When the OSISAF data are not available for both hemispheres on a given day, the sea ice concentration data are taken from the sea_ice_fraction variable found in the L4 GHRSST DailyOI SST product from NOAA/NCDC, and are interpolated from the 25km DailyOI grid to the 4km Pathfinder Version 5.3 grid.\";\n" +
"    String coverage_content_type \"auxiliaryInformation\";\n" +
"    String grid_mapping \"crs\";\n" +
"    String ioos_category \"Ice Distribution\";\n" +
"    String long_name \"sea ice fraction\";\n" +
"    String references \"Reynolds, et al.(2006) Daily High-resolution Blended Analyses. Available at http://doi.org/10.7289/V5SQ8XB5\";\n" +
"    String source \"NOAA/NESDIS/NCDC Daily optimum interpolation(OI) SST on 1/4-degree grid: 19810901-NCDC-L4LRblend-GLOB-v01-fv02_0-AVHRR_OI.nc.gz\";\n" +
"    String standard_name \"sea_ice_area_fraction\";\n" +
"    Float64 time_offset 2.0;\n" +
"    String units \"percent\";\n" +
"    Float64 valid_max 1.27;\n" +  //2020-08-04 changed from NaN with change to maxIsMV support
"    Float64 valid_min -1.27;\n" +
"  }\n" +
"  quality_level {\n" +
"    Byte _FillValue 0;\n" +
"    String _Unsigned \"false\";\n" + //ERDDAP adds
"    String ancillary_variables \"pathfinder_quality_level\";\n" +
"    String colorBarContinuous \"false\";\n" +
"    Float64 colorBarMaximum 6.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    Int32 colorBarNSections 6;\n" +
"    String comment \"These are the overall quality indicators and are used for all GHRSST SSTs. Note, the native Pathfinder processing system returns quality levels ranging from 0 to 7 (7 is best quality; -1 represents missing data) and has been converted to the extent possible into the six levels required by the GDS2 (ranging from 0 to 5, where 5 is best). Below is the conversion table: \n" +
" GDS2 required quality_level 5  =  native Pathfinder quality level 7 == best_quality \n" +
" GDS2 required quality_level 4  =  native Pathfinder quality level 4-6 == acceptable_quality \n" +
" GDS2 required quality_level 3  =  native Pathfinder quality level 2-3 == low_quality \n" +
" GDS2 required quality_level 2  =  native Pathfinder quality level 1 == worst_quality \n" +
" GDS2 required quality_level 1  =  native Pathfinder quality level 0 = bad_data \n" +
" GDS2 required quality_level 0  =  native Pathfinder quality level -1 = missing_data \n" +
" The original Pathfinder quality level is recorded in the optional variable pathfinder_quality_level.\";\n" +
"    String coverage_content_type \"qualityInformation\";\n" +
"    String flag_meanings \"no_data bad_data worst_quality low_quality acceptable_quality best_quality\";\n" +
"    Byte flag_values 0, 1, 2, 3, 4, 5;\n" +
"    String grid_mapping \"crs\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"quality level of SST pixel\";\n" +
"    String platform \"NOAA-7\";\n" +
"    String source \"AVHRR_GAC-CLASS-L1B-NOAA_07-v1\";\n" +
"    String units \"1\";\n" +
"    Byte valid_max 5;\n" +
"    Byte valid_min 1;\n" +
"  }\n" +
"  pathfinder_quality_level {\n" +
"    Byte _FillValue -1;\n" +
"    String _Unsigned \"false\";\n" + //ERDDAP adds
"    String colorBarContinuous \"false\";\n" +
"    Float64 colorBarMaximum 8.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    Int32 colorBarNSections 8;\n" +
"    String comment \"This variable contains the native Pathfinder processing system quality levels, ranging from 0 to 7, where 0 is worst and 7 is best. And value -1 represents missing data.\";\n" +
"    String coverage_content_type \"qualityInformation\";\n" +
"    String flag_meanings \"bad_data worst_quality low_quality low_quality acceptable_quality acceptable_quality acceptable_quality best_quality\";\n" +
"    Byte flag_values 0, 1, 2, 3, 4, 5, 6, 7;\n" +
"    String grid_mapping \"crs\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Pathfinder SST quality flag\";\n" +
"    String platform \"NOAA-7\";\n" +
"    String source \"AVHRR_GAC-CLASS-L1B-NOAA_07-v1\";\n" +
"    String units \"1\";\n" +
"    Byte valid_max 7;\n" +
"    Byte valid_min 0;\n" +
"  }\n" +
"  l2p_flags {\n" +
"    Int16 _FillValue 32767;\n" +
"    Float64 colorBarMaximum 300.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"Bit zero (0) is always set to zero to indicate infrared data. Bit one (1) is set to zero for any pixel over water (ocean, lakes and rivers). Land pixels were determined by rasterizing the Global Self-consistent Hierarchical High-resolution Shoreline (GSHHS) Database from the NOAA National Geophysical Data Center. Any 4 km Pathfinder pixel whose area is 50% or more covered by land has bit one (1) set to 1. Bit two (2) is set to 1 when the sea_ice_fraction is 0.15 or greater. Bits three (3) and four (4) indicate lake and river pixels, respectively, and were determined by rasterizing the US World Wildlife Fund's Global Lakes and Wetlands Database. Any 4 km Pathfinder pixel whose area is 50% or more covered by lake has bit three (3) set to 1. Any 4 km Pathfinder pixel whose area is 50% or more covered by river has bit four (4) set to 1. Bits six (6) indicates the daytime unrealistic SST values (>39.8°C) that remain in pf_quality_level 4 to 7. Users are recommended to avoid these values.\";\n" +
"    String coverage_content_type \"auxiliaryInformation\";\n" +
"    Int16 flag_masks 1, 2, 4, 8, 16, 32, 64, 128, 256;\n" +
"    String flag_meanings \"microwave land ice lake river reserved_for_future_use extreme_sst unused_currently unused_currently\";\n" +
"    String grid_mapping \"crs\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"L2P flags\";\n" +
"    String platform \"NOAA-7\";\n" +
"    String source \"AVHRR_GAC-CLASS-L1B-NOAA_07-v1\";\n" +
"    String units \"1\";\n" +
"    Int16 valid_max 256;\n" +
"    Int16 valid_min 0;\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String acknowledgement \"Please acknowledge the use of these data with the following statement: These data were provided by GHRSST and the NOAA National Centers for Environmental Information (NCEI). This project was supported in part by a grant from the NOAA Climate Data Record (CDR) Program for satellites.\";\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String cdr_id \"gov.noaa.ncdc:C00983\";\n" +
"    String cdr_program \"NOAA Climate Data Record Program for satellites\";\n" +
"    String cdr_variable \"sea_surface_temperature\";\n" +
"    String comment \"SST from AVHRR Pathfinder\";\n" +
"    String contributor_name \"Robert Evans\";\n" +
"    String contributor_role \"Principal Investigator\";\n" +
"    String Conventions \"CF-1.6, ACDD-1.3, COARDS\";\n" +
"    String creator_email \"Kenneth.Casey@noaa.gov\";\n" +
"    String creator_institution \"US DOC; NOAA; National Environmental Satellite Data and Information Service; National Centers for Environmental Information\";\n" +
"    String creator_name \"Kenneth S. Casey\";\n" +
"    String creator_type \"person\";\n" +
"    String creator_url \"https://pathfinder.nodc.noaa.gov\";\n" +
"    String date_created \"2016-05-12T14:51:42Z\";\n" +
"    String date_issued \"2016-03-01T00:00:00Z\";\n" +
"    String date_metadata_modified \"2016-01-25T00:00:00Z\";\n" +
"    String date_modified \"2016-05-12T14:51:42Z\";\n" +
"    String day_or_night \"Day\";\n" +
"    Float64 Easternmost_Easting 179.9792;\n" +
"    String gds_version_id \"2.0\";\n" +
"    String geospatial_bounds \"-180.0000 -90.0000, 180.0000 90.0000\";\n" +
"    String geospatial_bounds_crs \"EPSG:4326\";\n" +
"    Float64 geospatial_lat_max 89.97917;\n" +
"    Float64 geospatial_lat_min -89.97916;\n" +
"    Float64 geospatial_lat_resolution 0.04166666589488307;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 179.9792;\n" +
"    Float64 geospatial_lon_min -179.9792;\n" +
"    Float64 geospatial_lon_resolution 0.041666674383609215;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String history \"smigen_both ifile=1981243.b4kd3-pf53ap-n07-sst.hdf ofile=1981243.i4kd3-pf53ap-n07-sst.hdf prod=sst datamin=-3.0 datamax=40.0 precision=I projection=RECT resolution=4km gap_fill=2 ; /srv/disk_v1t/PFV5.3CONV/bin/Converter/hdf2nc_PFV53_L3C.x -v /srv/disk_v1t/PFV5.3CONV/Data_PFV53/PFV53_HDF_L3C/1981/1981243.i4kd3-pf53ap-n07-sst.hdf";
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
    
expected = 
   "String id \"AVHRR_Pathfinder-NCEI-L3C-v5.3\";\n" +
"    String infoUrl \"https://data.nodc.noaa.gov/cgi-bin/iso?id=gov.noaa.nodc:AVHRR_Pathfinder-NCEI-L3C-v5.3\";\n" +
"    String institution \"NCEI\";\n" +
"    String instrument \"AVHRR-2\";\n" +
"    String instrument_vocabulary \"NASA Global Change Master Directory (GCMD) Science Keywords v8.4\";\n" +
"    String keywords \"10m, advanced, aerosol, aerosol_dynamic_indicator, analysis, area, atmosphere, atmospheric, avhrr, bias, centers, climate, collated, cryosphere, data, deviation, difference, distribution, dt_analysis, dynamic, Earth Science > Atmosphere > Atmospheric Winds > Surface Winds, Earth Science > Cryosphere > Sea Ice > Ice Extent, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, Earth Science > Oceans > Sea Ice > Ice Extent, environmental, error, estimate, extent, flag, flags, fraction, ghrsst, global, high, high-resolution, ice, ice distribution, indicator, information, l2p, l2p_flags, l3-collated, l3c, level, national, ncei, noaa, ocean, oceans, optical, optical properties, pathfinder, pathfinder_quality_level, pixel, properties, quality, quality_level, radiometer, record, reference, resolution, sea, sea_ice_area_fraction, sea_ice_fraction, sea_surface_skin_temperature, sea_surface_temperature, sensor, single, skin, speed, sses, sses_bias, sses_standard_deviation, sst, sst_dtime, standard, statistics, surface, temperature, time, version, very, vhrr, wind, wind_speed, winds\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"These data are available for use without restriction.\n" +
"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String metadata_link \"https://data.nodc.noaa.gov/cgi-bin/iso?id=gov.noaa.nodc:AVHRR_Pathfinder-NCEI-L3C-v5.3\";\n" +
"    String naming_authority \"org.ghrsst\";\n" +
"    String ncei_template_version \"NCEI_NetCDF_Grid_Template_v2.0\";\n" +
"    Float64 Northernmost_Northing 89.97917;\n" +
"    String orbit_node \"Ascending\";\n" +
"    String platform \"NOAA-7\";\n" +
"    String platform_vocabulary \"NASA Global Change Master Directory (GCMD) Science Keywords v8.4\";\n" +
"    String processing_level \"L3C\";\n" +
"    String product_version \"PFV5.3\";\n" +
"    String program \"NOAA Climate Data Record (CDR) Program for satellites\";\n" +
"    String project \"Group for High Resolution Sea Surface Temperature\";\n" +
"    String publisher_email \"ghrsst-po@nceo.ac.uk\";\n" +
"    String publisher_name \"GHRSST Project Office\";\n" +
"    String publisher_type \"group\";\n" +
"    String publisher_url \"https://www.ghrsst.org\";\n" +
"    String references \"https://pathfinder.nodc.noaa.gov and Casey, K.S., T.B. Brandon, P. Cornillon, and R. Evans: The Past, Present and Future of the AVHRR Pathfinder SST Program, in Oceanography from Space: Revisited, eds. V. Barale, J.F.R. Gower, and L. Alberotanza, Springer, 2010. DOI: 10.1007/978-90-481-8681-5_16.\";\n" +
"    String sea_name \"World-Wide Distribution\";\n" +
"    String sensor \"AVHRR-2\";\n" +
"    String source \"AVHRR_GAC-CLASS-L1B-NOAA_07-v1\";\n" +
"    String sourceUrl \"https://data.nodc.noaa.gov/pathfinder/Version5.3/L3C/\";\n" +
"    Float64 Southernmost_Northing -89.97916;\n" +
"    String spatial_resolution \"0.0416667 degree\";\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String summary \"Advanced Very High Resolution Radiometer (AVHRR) Pathfinder Version 5.3 L3-Collated (L3C) sea surface temperature. This netCDF-4 file contains sea surface temperature (SST) data produced as part of the AVHRR Pathfinder SST Project. These data were created using Version 5.3 of the Pathfinder algorithm and the file is nearly but not completely compliant with the Global High-Resolution Sea Surface Temperature (GHRSST) Data Specifications V2.0 (GDS2). The sses_bias and sses_standard_deviation variables are empty. Full compliance with GDS2 specifications will be achieved in the future Pathfinder Version 6. These data were created by the NOAA National Centers for Environmental Information (NCEI).\";\n" +
"    String time_coverage_duration \"P1D\";\n" +
"    String time_coverage_end \"1981-08-31T12:00:00Z\";\n" +
"    String time_coverage_resolution \"P1D\";\n" +
"    String time_coverage_start \"1981-08-25T12:00:00Z\";\n" +
"    String title \"AVHRR Pathfinder Version 5.3 L3-Collated (L3C) SST, Global, 0.0417°, 1981-present, Daytime (1 Day Composite)\";\n" +
"    Float64 Westernmost_Easting -179.9792;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);


        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", tDir, 
            eddGrid.className() + "_testReplaceFromFileName", ".dds"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Float64 time[time = 7];\n" +
"  Float32 latitude[latitude = 4320];\n" +
"  Float32 longitude[longitude = 8640];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 sea_surface_temperature[time = 7][latitude = 4320][longitude = 8640];\n" +
"    MAPS:\n" +
"      Float64 time[time = 7];\n" +
"      Float32 latitude[latitude = 4320];\n" +
"      Float32 longitude[longitude = 8640];\n" +
"  } sea_surface_temperature;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 dt_analysis[time = 7][latitude = 4320][longitude = 8640];\n" +
"    MAPS:\n" +
"      Float64 time[time = 7];\n" +
"      Float32 latitude[latitude = 4320];\n" +
"      Float32 longitude[longitude = 8640];\n" +
"  } dt_analysis;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Byte wind_speed[time = 7][latitude = 4320][longitude = 8640];\n" +
"    MAPS:\n" +
"      Float64 time[time = 7];\n" +
"      Float32 latitude[latitude = 4320];\n" +
"      Float32 longitude[longitude = 8640];\n" +
"  } wind_speed;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 sea_ice_fraction[time = 7][latitude = 4320][longitude = 8640];\n" +
"    MAPS:\n" +
"      Float64 time[time = 7];\n" +
"      Float32 latitude[latitude = 4320];\n" +
"      Float32 longitude[longitude = 8640];\n" +
"  } sea_ice_fraction;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Byte quality_level[time = 7][latitude = 4320][longitude = 8640];\n" +
"    MAPS:\n" +
"      Float64 time[time = 7];\n" +
"      Float32 latitude[latitude = 4320];\n" +
"      Float32 longitude[longitude = 8640];\n" +
"  } quality_level;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Byte pathfinder_quality_level[time = 7][latitude = 4320][longitude = 8640];\n" +
"    MAPS:\n" +
"      Float64 time[time = 7];\n" +
"      Float32 latitude[latitude = 4320];\n" +
"      Float32 longitude[longitude = 8640];\n" +
"  } pathfinder_quality_level;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Int16 l2p_flags[time = 7][latitude = 4320][longitude = 8640];\n" +
"    MAPS:\n" +
"      Float64 time[time = 7];\n" +
"      Float32 latitude[latitude = 4320];\n" +
"      Float32 longitude[longitude = 8640];\n" +
"  } l2p_flags;\n" +
"} nceiPH53sstd1day;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //*** test make data files
        String2.log("\n****************** testRepaceWithFileName make DATA FILES\n");       

        //time values
        userDapQuery = "time[]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            eddGrid.className() + "_testReplaceFromFileName" + "Time", ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"time\n" +
"UTC\n" +
"1981-08-25T12:00:00Z\n" +
"1981-08-26T12:00:00Z\n" +
"1981-08-27T12:00:00Z\n" +
"1981-08-28T12:00:00Z\n" +
"1981-08-29T12:00:00Z\n" +
"1981-08-30T12:00:00Z\n" +
"1981-08-31T12:00:00Z\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv that needs every file
        userDapQuery = "sea_surface_temperature[0:last][1600:1601][10]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            eddGrid.className() + "_testReplaceFromFileName", ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"time,latitude,longitude,sea_surface_temperature\n" +
"UTC,degrees_north,degrees_east,degree_C\n" +
"1981-08-25T12:00:00Z,23.312502,-179.5625,1.35\n" +
"1981-08-25T12:00:00Z,23.270836,-179.5625,-0.58\n" +
"1981-08-26T12:00:00Z,23.312502,-179.5625,19.0\n" +
"1981-08-26T12:00:00Z,23.270836,-179.5625,22.580000000000002\n" +
"1981-08-27T12:00:00Z,23.312502,-179.5625,27.22\n" +
"1981-08-27T12:00:00Z,23.270836,-179.5625,26.48\n" +
"1981-08-28T12:00:00Z,23.312502,-179.5625,28.98\n" +
"1981-08-28T12:00:00Z,23.270836,-179.5625,28.93\n" +
"1981-08-29T12:00:00Z,23.312502,-179.5625,28.150000000000002\n" +
"1981-08-29T12:00:00Z,23.270836,-179.5625,27.68\n" +
"1981-08-30T12:00:00Z,23.312502,-179.5625,28.36\n" +
"1981-08-30T12:00:00Z,23.270836,-179.5625,28.51\n" +
"1981-08-31T12:00:00Z,23.312502,-179.5625,-3.0\n" +
"1981-08-31T12:00:00Z,23.270836,-179.5625,-3.0\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        String2.log("\n*** EDDGridFromNcFiles.testReplaceFromFileName() finished successfully.");
        /* */
    }

    /**
     * This tests testMinimalReadSource, which uses axis0=***replaceFromFileName
     * and matchAxisNDigits=0!
     *
     * @throws Throwable if trouble
     */
    public static void testMinimalReadSource() throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testMinimalReadSource");
        testVerboseOn();
        boolean oDebugMode = debugMode;
        debugMode = true;

        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        int po;
        EDV edv;

        String tDir = EDStatic.fullTestCacheDirectory;
        String id = "testMinimalReadSource";
        deleteCachedDatasetInfo(id); //always

        EDDGrid eddGrid;
        eddGrid = (EDDGrid)oneFromDatasetsXml(null, id); 
        String2.pressEnterToContinue(
            "\nCheck that all file reads above (except the first) have:\n" +
            "  >> EDDGridFromFiles.getSourceAxisValues just using known info, not reading the file.\n" +
            "  >> EDDGridFromFiles.getSourceMetadata   just using known info, not reading the file.\n");

        //*** test getting das for entire dataset
        String2.log("\n****************** testMinimalReadSource  das and dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", tDir, 
            eddGrid.className() + "_testMinimalReadSource", ".das"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 3.675888e+8, 3.681072e+8;\n" +
"    String axis \"T\";\n" +
"    String comment \"This is the centered, reference time.\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Centered Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range -89.97916, 89.97917;\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String reference_datum \"Geographical coordinates, WGS84 datum\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"    Float32 valid_max 90.0;\n" +
"    Float32 valid_min -90.0;\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 actual_range -179.9792, 179.9792;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String reference_datum \"Geographical coordinates, WGS84 datum\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"    Float32 valid_max 180.0;\n" +
"    Float32 valid_min -180.0;\n" +
"  }\n" +
"  sea_surface_temperature {\n" +
"    Float64 _FillValue -327.68;\n" +
"    String ancillary_variables \"quality_level pathfinder_quality_level l2p_flags\";\n" +
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"Skin temperature of the ocean\";\n" +
"    String coverage_content_type \"physicalMeasurement\";\n" +
"    String grid_mapping \"crs\";\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"NOAA Climate Data Record of sea surface skin temperature\";\n" +
"    String platform \"NOAA-7\";\n" +
"    String source \"AVHRR_GAC-CLASS-L1B-NOAA_07-v1\";\n" +
"    String standard_name \"sea_surface_skin_temperature\";\n" +
"    String units \"degree_C\";\n" +
"    Float64 valid_max 45.0;\n" +
"    Float64 valid_min -1.8;\n" +
"  }\n" +
"  dt_analysis {\n" +
"    Float64 _FillValue -12.8;\n" +
"    Float64 colorBarMaximum 5.0;\n" +
"    Float64 colorBarMinimum -5.0;\n" +
"    String comment \"The difference between this SST and the previous day's SST.\";\n" +
"    String coverage_content_type \"auxiliaryInformation\";\n" +
"    String grid_mapping \"crs\";\n" +
"    String ioos_category \"Statistics\";\n" +
"    String long_name \"deviation from last SST analysis\";\n" +
"    String platform \"NOAA-7\";\n" +
"    String references \"AVHRR_OI, with inland values populated from AVHRR_Pathfinder daily climatological SST. For more information on this reference field see https://data.nodc.noaa.gov/cgi-bin/iso?id=gov.noaa.nodc:0071180.\";\n" +
"    String source \"NOAA Daily 25km Global Optimally Interpolated Sea Surface Temperature (OISST)\";\n" +
"    String units \"degree_C\";\n" +
"    Float64 valid_max 12.700000000000001;\n" +  //good test of maxIsMV=false
"    Float64 valid_min -12.700000000000001;\n" +
"  }\n" +
"  wind_speed {\n" +
"    Byte _FillValue -128;\n" +
"    String _Unsigned \"false\";\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"These wind speeds were created by NCEP-DOE Atmospheric Model Intercomparison Project (AMIP-II) reanalysis (R-2) and represent winds at 10 metres above the sea surface.\";\n" +
"    String coverage_content_type \"auxiliaryInformation\";\n" +
"    String grid_mapping \"crs\";\n" +
"    String height \"10 m\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"10m wind speed\";\n" +
"    String source \"NCEP/DOE AMIP-II Reanalysis (Reanalysis-2): u_wind.10m.gauss.1981.nc, v_wind.10m.gauss.1981.nc\";\n" +
"    String standard_name \"wind_speed\";\n" +
"    Float64 time_offset 2.0946;\n" +
"    String units \"m s-1\";\n" +
"    Byte valid_max 127;\n" +
"    Byte valid_min -127;\n" +
"  }\n" +
"  sea_ice_fraction {\n" +
"    Float64 _FillValue -1.28;\n" +
"    Float64 colorBarMaximum 1.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"Sea ice concentration data are taken from the EUMETSAT Ocean and Sea Ice Satellite Application Facility (OSISAF) Global Daily Sea Ice Concentration Reprocessing Data Set (https://data.nodc.noaa.gov/cgi-bin/iso?id=gov.noaa.nodc:0068294) when these data are available. The data are reprojected and interpolated from their original polar stereographic projection at 10km spatial resolution to the 4km Pathfinder Version 5.3 grid. When the OSISAF data are not available for both hemispheres on a given day, the sea ice concentration data are taken from the sea_ice_fraction variable found in the L4 GHRSST DailyOI SST product from NOAA/NCDC, and are interpolated from the 25km DailyOI grid to the 4km Pathfinder Version 5.3 grid.\";\n" +
"    String coverage_content_type \"auxiliaryInformation\";\n" +
"    String grid_mapping \"crs\";\n" +
"    String ioos_category \"Ice Distribution\";\n" +
"    String long_name \"sea ice fraction\";\n" +
"    String references \"Reynolds, et al.(2006) Daily High-resolution Blended Analyses. Available at http://doi.org/10.7289/V5SQ8XB5\";\n" +
"    String source \"NOAA/NESDIS/NCDC Daily optimum interpolation(OI) SST on 1/4-degree grid: 19810901-NCDC-L4LRblend-GLOB-v01-fv02_0-AVHRR_OI.nc.gz\";\n" +
"    String standard_name \"sea_ice_area_fraction\";\n" +
"    Float64 time_offset 2.0;\n" +
"    String units \"percent\";\n" +
"    Float64 valid_max 1.27;\n" +  //good test of maxIsMV=false
"    Float64 valid_min -1.27;\n" +
"  }\n" +
"  quality_level {\n" +
"    Byte _FillValue 0;\n" +
"    String _Unsigned \"false\";\n" +
"    String ancillary_variables \"pathfinder_quality_level\";\n" +
"    String colorBarContinuous \"false\";\n" +
"    Float64 colorBarMaximum 6.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    Int32 colorBarNSections 6;\n" +
"    String comment \"These are the overall quality indicators and are used for all GHRSST SSTs. Note, the native Pathfinder processing system returns quality levels ranging from 0 to 7 (7 is best quality; -1 represents missing data) and has been converted to the extent possible into the six levels required by the GDS2 (ranging from 0 to 5, where 5 is best). Below is the conversion table: \n" +
" GDS2 required quality_level 5  =  native Pathfinder quality level 7 == best_quality \n" +
" GDS2 required quality_level 4  =  native Pathfinder quality level 4-6 == acceptable_quality \n" +
" GDS2 required quality_level 3  =  native Pathfinder quality level 2-3 == low_quality \n" +
" GDS2 required quality_level 2  =  native Pathfinder quality level 1 == worst_quality \n" +
" GDS2 required quality_level 1  =  native Pathfinder quality level 0 = bad_data \n" +
" GDS2 required quality_level 0  =  native Pathfinder quality level -1 = missing_data \n" +
" The original Pathfinder quality level is recorded in the optional variable pathfinder_quality_level.\";\n" +
"    String coverage_content_type \"qualityInformation\";\n" +
"    String flag_meanings \"no_data bad_data worst_quality low_quality acceptable_quality best_quality\";\n" +
"    Byte flag_values 0, 1, 2, 3, 4, 5;\n" +
"    String grid_mapping \"crs\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"quality level of SST pixel\";\n" +
"    String platform \"NOAA-7\";\n" +
"    String source \"AVHRR_GAC-CLASS-L1B-NOAA_07-v1\";\n" +
"    String units \"1\";\n" +
"    Byte valid_max 5;\n" +
"    Byte valid_min 1;\n" +
"  }\n" +
"  pathfinder_quality_level {\n" +
"    Byte _FillValue -1;\n" +
"    String _Unsigned \"false\";\n" +
"    String colorBarContinuous \"false\";\n" +
"    Float64 colorBarMaximum 8.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    Int32 colorBarNSections 8;\n" +
"    String comment \"This variable contains the native Pathfinder processing system quality levels, ranging from 0 to 7, where 0 is worst and 7 is best. And value -1 represents missing data.\";\n" +
"    String coverage_content_type \"qualityInformation\";\n" +
"    String flag_meanings \"bad_data worst_quality low_quality low_quality acceptable_quality acceptable_quality acceptable_quality best_quality\";\n" +
"    Byte flag_values 0, 1, 2, 3, 4, 5, 6, 7;\n" +
"    String grid_mapping \"crs\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Pathfinder SST quality flag\";\n" +
"    String platform \"NOAA-7\";\n" +
"    String source \"AVHRR_GAC-CLASS-L1B-NOAA_07-v1\";\n" +
"    String units \"1\";\n" +
"    Byte valid_max 7;\n" +
"    Byte valid_min 0;\n" +
"  }\n" +
"  l2p_flags {\n" +
"    Float64 colorBarMaximum 300.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"Bit zero (0) is always set to zero to indicate infrared data. Bit one (1) is set to zero for any pixel over water (ocean, lakes and rivers). Land pixels were determined by rasterizing the Global Self-consistent Hierarchical High-resolution Shoreline (GSHHS) Database from the NOAA National Geophysical Data Center. Any 4 km Pathfinder pixel whose area is 50% or more covered by land has bit one (1) set to 1. Bit two (2) is set to 1 when the sea_ice_fraction is 0.15 or greater. Bits three (3) and four (4) indicate lake and river pixels, respectively, and were determined by rasterizing the US World Wildlife Fund's Global Lakes and Wetlands Database. Any 4 km Pathfinder pixel whose area is 50% or more covered by lake has bit three (3) set to 1. Any 4 km Pathfinder pixel whose area is 50% or more covered by river has bit four (4) set to 1. Bits six (6) indicates the daytime unrealistic SST values (>39.8°C) that remain in pf_quality_level 4 to 7. Users are recommended to avoid these values.\";\n" +
"    String coverage_content_type \"auxiliaryInformation\";\n" +
"    Int16 flag_masks 1, 2, 4, 8, 16, 32, 64, 128, 256;\n" +
"    String flag_meanings \"microwave land ice lake river reserved_for_future_use extreme_sst unused_currently unused_currently\";\n" +
"    String grid_mapping \"crs\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"L2P flags\";\n" +
"    String platform \"NOAA-7\";\n" +
"    String source \"AVHRR_GAC-CLASS-L1B-NOAA_07-v1\";\n" +
"    String units \"1\";\n" +
"    Int16 valid_max 256;\n" +
"    Int16 valid_min 0;\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String acknowledgement \"Please acknowledge the use of these data with the following statement: These data were provided by GHRSST and the NOAA National Centers for Environmental Information (NCEI). This project was supported in part by a grant from the NOAA Climate Data Record (CDR) Program for satellites.\";\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String cdr_id \"gov.noaa.ncdc:C00983\";\n" +
"    String cdr_program \"NOAA Climate Data Record Program for satellites\";\n" +
"    String cdr_variable \"sea_surface_temperature\";\n" +
"    String comment \"SST from AVHRR Pathfinder\";\n" +
"    String contributor_name \"Robert Evans\";\n" +
"    String contributor_role \"Principal Investigator\";\n" +
"    String Conventions \"CF-1.6, ACDD-1.3, COARDS\";\n" +
"    String creator_email \"Kenneth.Casey@noaa.gov\";\n" +
"    String creator_institution \"US DOC; NOAA; National Environmental Satellite Data and Information Service; National Centers for Environmental Information\";\n" +
"    String creator_name \"Kenneth S. Casey\";\n" +
"    String creator_type \"person\";\n" +
"    String creator_url \"https://pathfinder.nodc.noaa.gov\";\n" +
"    String date_created \"2016-05-12T14:51:42Z\";\n" +
"    String date_issued \"2016-03-01T00:00:00Z\";\n" +
"    String date_metadata_modified \"2016-01-25T00:00:00Z\";\n" +
"    String date_modified \"2016-05-12T14:51:42Z\";\n" +
"    String day_or_night \"Day\";\n" +
"    Float64 Easternmost_Easting 179.9792;\n" +
"    String gds_version_id \"2.0\";\n" +
"    String geospatial_bounds \"-180.0000 -90.0000, 180.0000 90.0000\";\n" +
"    String geospatial_bounds_crs \"EPSG:4326\";\n" +
"    Float64 geospatial_lat_max 89.97917;\n" +
"    Float64 geospatial_lat_min -89.97916;\n" +
"    Float64 geospatial_lat_resolution 0.04166666589488307;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 179.9792;\n" +
"    Float64 geospatial_lon_min -179.9792;\n" +
"    Float64 geospatial_lon_resolution 0.041666674383609215;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String history \"smigen_both ifile=1981243.b4kd3-pf53ap-n07-sst.hdf ofile=1981243.i4kd3-pf53ap-n07-sst.hdf prod=sst datamin=-3.0 datamax=40.0 precision=I projection=RECT resolution=4km gap_fill=2 ; /srv/disk_v1t/PFV5.3CONV/bin/Converter/hdf2nc_PFV53_L3C.x -v /srv/disk_v1t/PFV5.3CONV/Data_PFV53/PFV53_HDF_L3C/1981/1981243.i4kd3-pf53ap-n07-sst.hdf";
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
    
expected = 
   "String id \"AVHRR_Pathfinder-NCEI-L3C-v5.3\";\n" +
"    String infoUrl \"https://data.nodc.noaa.gov/cgi-bin/iso?id=gov.noaa.nodc:AVHRR_Pathfinder-NCEI-L3C-v5.3\";\n" +
"    String institution \"NCEI\";\n" +
"    String instrument \"AVHRR-2\";\n" +
"    String instrument_vocabulary \"NASA Global Change Master Directory (GCMD) Science Keywords v8.4\";\n" +
"    String keywords \"10m, advanced, aerosol, aerosol_dynamic_indicator, analysis, area, atmosphere, atmospheric, avhrr, bias, centers, climate, collated, cryosphere, data, deviation, difference, distribution, dt_analysis, dynamic, Earth Science > Atmosphere > Atmospheric Winds > Surface Winds, Earth Science > Cryosphere > Sea Ice > Ice Extent, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, Earth Science > Oceans > Sea Ice > Ice Extent, environmental, error, estimate, extent, flag, flags, fraction, ghrsst, global, high, high-resolution, ice, ice distribution, indicator, information, l2p, l2p_flags, l3-collated, l3c, level, national, ncei, noaa, ocean, oceans, optical, optical properties, pathfinder, pathfinder_quality_level, pixel, properties, quality, quality_level, radiometer, record, reference, resolution, sea, sea_ice_area_fraction, sea_ice_fraction, sea_surface_skin_temperature, sea_surface_temperature, sensor, single, skin, speed, sses, sses_bias, sses_standard_deviation, sst, sst_dtime, standard, statistics, surface, temperature, time, version, very, vhrr, wind, wind_speed, winds\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"These data are available for use without restriction.\n" +
"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String metadata_link \"https://data.nodc.noaa.gov/cgi-bin/iso?id=gov.noaa.nodc:AVHRR_Pathfinder-NCEI-L3C-v5.3\";\n" +
"    String naming_authority \"org.ghrsst\";\n" +
"    String ncei_template_version \"NCEI_NetCDF_Grid_Template_v2.0\";\n" +
"    Float64 Northernmost_Northing 89.97917;\n" +
"    String orbit_node \"Ascending\";\n" +
"    String platform \"NOAA-7\";\n" +
"    String platform_vocabulary \"NASA Global Change Master Directory (GCMD) Science Keywords v8.4\";\n" +
"    String processing_level \"L3C\";\n" +
"    String product_version \"PFV5.3\";\n" +
"    String program \"NOAA Climate Data Record (CDR) Program for satellites\";\n" +
"    String project \"Group for High Resolution Sea Surface Temperature\";\n" +
"    String publisher_email \"ghrsst-po@nceo.ac.uk\";\n" +
"    String publisher_name \"GHRSST Project Office\";\n" +
"    String publisher_type \"group\";\n" +
"    String publisher_url \"https://www.ghrsst.org\";\n" +
"    String references \"https://pathfinder.nodc.noaa.gov and Casey, K.S., T.B. Brandon, P. Cornillon, and R. Evans: The Past, Present and Future of the AVHRR Pathfinder SST Program, in Oceanography from Space: Revisited, eds. V. Barale, J.F.R. Gower, and L. Alberotanza, Springer, 2010. DOI: 10.1007/978-90-481-8681-5_16.\";\n" +
"    String sea_name \"World-Wide Distribution\";\n" +
"    String sensor \"AVHRR-2\";\n" +
"    String source \"AVHRR_GAC-CLASS-L1B-NOAA_07-v1\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing -89.97916;\n" +
"    String spatial_resolution \"0.0416667 degree\";\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String summary \"Advanced Very High Resolution Radiometer (AVHRR) Pathfinder Version 5.3 L3-Collated (L3C) sea surface temperature. This netCDF-4 file contains sea surface temperature (SST) data produced as part of the AVHRR Pathfinder SST Project. These data were created using Version 5.3 of the Pathfinder algorithm and the file is nearly but not completely compliant with the Global High-Resolution Sea Surface Temperature (GHRSST) Data Specifications V2.0 (GDS2). The sses_bias and sses_standard_deviation variables are empty. Full compliance with GDS2 specifications will be achieved in the future Pathfinder Version 6. These data were created by the NOAA National Centers for Environmental Information (NCEI).\";\n" +
"    String time_coverage_duration \"P1D\";\n" +
"    String time_coverage_end \"1981-08-31T12:00:00Z\";\n" +
"    String time_coverage_resolution \"P1D\";\n" +
"    String time_coverage_start \"1981-08-25T12:00:00Z\";\n" +
"    String title \"AVHRR Pathfinder Version 5.3 L3-Collated (L3C) SST, Global, 0.0417°, 1981-2018, Daytime (1 Day Composite)\";\n" +
"    Float64 Westernmost_Easting -179.9792;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);

        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", tDir, 
            eddGrid.className() + "_testMinimalReadSource", ".dds"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Float64 time[time = 7];\n" +
"  Float32 latitude[latitude = 4320];\n" +
"  Float32 longitude[longitude = 8640];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 sea_surface_temperature[time = 7][latitude = 4320][longitude = 8640];\n" +
"    MAPS:\n" +
"      Float64 time[time = 7];\n" +
"      Float32 latitude[latitude = 4320];\n" +
"      Float32 longitude[longitude = 8640];\n" +
"  } sea_surface_temperature;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 dt_analysis[time = 7][latitude = 4320][longitude = 8640];\n" +
"    MAPS:\n" +
"      Float64 time[time = 7];\n" +
"      Float32 latitude[latitude = 4320];\n" +
"      Float32 longitude[longitude = 8640];\n" +
"  } dt_analysis;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Byte wind_speed[time = 7][latitude = 4320][longitude = 8640];\n" +
"    MAPS:\n" +
"      Float64 time[time = 7];\n" +
"      Float32 latitude[latitude = 4320];\n" +
"      Float32 longitude[longitude = 8640];\n" +
"  } wind_speed;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 sea_ice_fraction[time = 7][latitude = 4320][longitude = 8640];\n" +
"    MAPS:\n" +
"      Float64 time[time = 7];\n" +
"      Float32 latitude[latitude = 4320];\n" +
"      Float32 longitude[longitude = 8640];\n" +
"  } sea_ice_fraction;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Byte quality_level[time = 7][latitude = 4320][longitude = 8640];\n" +
"    MAPS:\n" +
"      Float64 time[time = 7];\n" +
"      Float32 latitude[latitude = 4320];\n" +
"      Float32 longitude[longitude = 8640];\n" +
"  } quality_level;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Byte pathfinder_quality_level[time = 7][latitude = 4320][longitude = 8640];\n" +
"    MAPS:\n" +
"      Float64 time[time = 7];\n" +
"      Float32 latitude[latitude = 4320];\n" +
"      Float32 longitude[longitude = 8640];\n" +
"  } pathfinder_quality_level;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Int16 l2p_flags[time = 7][latitude = 4320][longitude = 8640];\n" +
"    MAPS:\n" +
"      Float64 time[time = 7];\n" +
"      Float32 latitude[latitude = 4320];\n" +
"      Float32 longitude[longitude = 8640];\n" +
"  } l2p_flags;\n" +
"} testMinimalReadSource;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //*** test make data files
        String2.log("\n****************** testRepaceWithFileName make DATA FILES\n");       

        //time values
        userDapQuery = "time[]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            eddGrid.className() + "_testMinimalReadSource" + "Time", ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"time\n" +
"UTC\n" +
"1981-08-25T12:00:00Z\n" +
"1981-08-26T12:00:00Z\n" +
"1981-08-27T12:00:00Z\n" +
"1981-08-28T12:00:00Z\n" +
"1981-08-29T12:00:00Z\n" +
"1981-08-30T12:00:00Z\n" +
"1981-08-31T12:00:00Z\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv that needs every file
        userDapQuery = "sea_surface_temperature[0:last][1600:1601][10]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            eddGrid.className() + "_testMinimalReadSource", ".csv"); 
        results = String2.directReadFrom88591File(tDir + tName);
        //String2.log(results);
        expected = 
"time,latitude,longitude,sea_surface_temperature\n" +
"UTC,degrees_north,degrees_east,degree_C\n" +
"1981-08-25T12:00:00Z,23.312502,-179.5625,1.35\n" +
"1981-08-25T12:00:00Z,23.270836,-179.5625,-0.58\n" +
"1981-08-26T12:00:00Z,23.312502,-179.5625,19.0\n" +
"1981-08-26T12:00:00Z,23.270836,-179.5625,22.580000000000002\n" +
"1981-08-27T12:00:00Z,23.312502,-179.5625,27.22\n" +
"1981-08-27T12:00:00Z,23.270836,-179.5625,26.48\n" +
"1981-08-28T12:00:00Z,23.312502,-179.5625,28.98\n" +
"1981-08-28T12:00:00Z,23.270836,-179.5625,28.93\n" +
"1981-08-29T12:00:00Z,23.312502,-179.5625,28.150000000000002\n" +
"1981-08-29T12:00:00Z,23.270836,-179.5625,27.68\n" +
"1981-08-30T12:00:00Z,23.312502,-179.5625,28.36\n" +
"1981-08-30T12:00:00Z,23.270836,-179.5625,28.51\n" +
"1981-08-31T12:00:00Z,23.312502,-179.5625,-3.0\n" +
"1981-08-31T12:00:00Z,23.270836,-179.5625,-3.0\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        String2.log("\n*** EDDGridFromNcFiles.testMinimalReadSource() finished successfully.");
        debugMode = oDebugMode;
        /* */
    }

    /**
     * This makes a series of graphs which test log axes.
     * @param whichChunk -1 (all) or 0 - 4.
     */
    public static void testLogAxis(int whichChunk) throws Throwable {

        String2.log("\n*** EDDGridFromNcFiles.testLogAxis()");
        String tDir = EDStatic.fullTestCacheDirectory;
        String tName, start, query, results, expected;
        EDDGrid eddGrid;

        //test default=linear
        eddGrid = (EDDGrid)oneFromDatasetsXml(null, "erdBAssta5day");

        if (whichChunk < 0 || whichChunk == 0) {

            //test if x is not axis var: not allowed
            start = "testLogAxis_ErrorXVarMustBeAxisVar";
            query = "sst[last-7:last][0][(25)][(242)]&.draw=lines&.vars=sst|time";
            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query,
                    tDir, start + "DefaultIsLinear", ".png")); 
        }

        if (whichChunk < 0 || whichChunk == 1) {
            start = "testLogAxis_TimeSst_";
            query = "sst[last-7:last][0][(25)][(242)]&.draw=lines&.vars=time|sst";
            /* */
            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query,
                tDir, start + "DefaultIsLinear", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.yRange=5|500||",
                tDir, start + "5_500DefaultIsAscendingLinear", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.yRange=5|500|true|Linear",
                tDir, start + "AscendingLinear", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.yRange=5|500|true|log",
                tDir, start + "AscendingLog", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.yRange=5|500|false|Log",
                tDir, start + "DescendingLog", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.yRange=1e-5|1e5||Log",
                tDir, start + "WideRangeLog", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.yRange=15|35||Log", //hard intra-decade, no power of 10 in range, 2 small tics visible
                tDir, start + "HardIntraDecadeLog", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.yRange=20.2|29||Log", //super hard intra-decade, no power of 10 in range
                tDir, start + "SuperHardIntraDecadeLog", ".png")); 
            /* */
        }



        if (whichChunk < 0 || whichChunk == 2) {

            //swap x and y axes
            start = "gridTestLogAxis_LonSst_";
            query = "sst[last][0][(25)][]&.draw=lines&.vars=longitude|sst";
            /* */

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query,
                tDir, start + "DefaultIsLinear", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.xRange=5|500||",
                tDir, start + "5_500DefaultIsAscendingLinear", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.xRange=5|500|true|Linear",
                tDir, start + "AscendingLinear", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.xRange=5|500|true|log",
                tDir, start + "AscendingLog", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.xRange=5|500|false|Log",
                tDir, start + "DescendingLog", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.xRange=1e-5|1e5||Log",
                tDir, start + "WideRangeLog", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.xRange=181|310||Log", //hard intra-decade, no power of 10 in range, 2 small tics visible
                tDir, start + "HardIntraDecadeLog", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.xRange=191|199||Log", //super hard intra-decade, no power of 10 in range
                tDir, start + "SuperHardIntraDecadeLog", ".png")); 
            /* */
        }


        //test default=Log      
        eddGrid = (EDDGrid)oneFromDatasetsXml(null, "erdMHchla8day");

        if (whichChunk < 0 || whichChunk == 3) {
            start = "gridTestLogAxis_TimeChla_";
            String2.log(String2.directReadFrom88591File(tDir +  
                eddGrid.makeNewFileForDapQuery(null, null, "chlorophyll[last][0][(50):(51)][(144):(145)]",
                tDir, start + "GetData", ".csv"))); 
            query = "chlorophyll[last-7:last][0][(50.7825885)][(144.016668)]&.draw=lines&.vars=time|chlorophyll";  //lat lon chosen for high chl values

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query,
                tDir, start + "DefaultIsLog", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.yRange=5|500||",
                tDir, start + "DefaultIsAscendingLog", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.yRange=5|500|true|Linear",
                tDir, start + "AscendingLinear", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.yRange=5|500|true|Log",
                tDir, start + "AscendingLog", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.yRange=5|500|false|Log",
                tDir, start + "DescendingLog", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.yRange=1e-5|1e5||Log",
                tDir, start + "WideRangeLog", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.yRange=5e-6|5e5||Log",
                tDir, start + "WideRangeLog5", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.yRange=5e-6|5e5|false|Log",
                tDir, start + "WideRangeLog5", ".png")); 
            /* */
        }

        if (whichChunk < 0 || whichChunk == 4) {

            //swap x and y axes
            start = "gridTestLogAxis_LonChla_";
            query = "chlorophyll[last][0][(50.7825885)][]&.draw=lines&.vars=longitude|chlorophyll";  //lat chosen for high chl values
            /* */

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query,
                tDir, start + "DefaultIsLinear", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.xRange=5|500||",
                tDir, start + "DefaultIsAscendingLinear", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.xRange=5|500|true|Linear",
                tDir, start + "AscendingLinear", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.xRange=5|500|true|Log",
                tDir, start + "AscendingLog", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.xRange=5|500|false|Log",
                tDir, start + "DescendingLog", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.xRange=1e-5|1e5||Log",
                tDir, start + "WideRangeLog", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.xRange=5e-6|5e5||Log",
                tDir, start + "WideRangeLog5", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.xRange=181|310||Log", //hard intra-decade, no power of 10 in range, 2 small tics visible
                tDir, start + "HardIntraDecadeLog", ".png")); 

            SSR.displayInBrowser("file://" + tDir + 
                eddGrid.makeNewFileForDapQuery(null, null, query + "&.xRange=205|229||Log", //super hard intra-decade, no power of 10 in range
                tDir, start + "SuperHardIntraDecadeLog", ".png")); 
            /* */
        }

        //String2.pressEnterToContinue();
    }


    /**
     * Test DAP errors.
     */
    public static void testDapErrors() throws Throwable {
        String baseRequest = "http://localhost:8080/cwexperimental/griddap/"; 
        String results, expected;

        String2.log("\n*** EDDGridFromNcFiles.testDapErrors()");
        String comment = "\n!!! These tests require erdBAssta5day and noaaPassiveAcoustic in localhost ERDDAP.";

/*400 Bad Request - for request syntax errors
  401 Unauthorized - for when the user isn't authorized to access a given dataset
  403 Forbidden - when the user is on the blacklist
  404 Not Found - for dataset not found or "Your query produced no matching results."
  408 Timeout - for dataset not found or "Your query produced no matching results."
  413 Payload Too Large 
  416 Range Not Satisfiable - for invalid byte range requests
  500 Internal Server Error - for errors while responding to the request and unexpected errors
*/

        //400 Bad Request: syntax error
        try {
            results = SSR.getUrlResponseStringNewline(baseRequest + "erdBAssta5day.csv?" + 
                SSR.minimalPercentEncode("time[[(2002-07-06)]"));  // [[
        } catch (Throwable t) {
            results = t.toString();
        }
        Test.ensureEqual(results, 
"java.io.IOException: HTTP status code=400 for URL: http://localhost:8080/cwexperimental/griddap/erdBAssta5day.csv?time%5B%5B(2002-07-06)%5D\n" +
"(Error {\n" +
"    code=400;\n" +
"    message=\"Bad Request: Query error: For variable=time axis#0=time Constraint=\\\"[[(2002-07-06)]\\\": Start=\\\"[(2002-07-06)\\\" is invalid.  It must be an integer between 0 and 3879.\";\n" +
"})", 
            "results=\n" + results + comment);

        //400 Bad Request: unsupported file type  (vs 404: emphasis here on syntax error, and permanent error)
        try {
            results = SSR.getUrlResponseStringNewline(baseRequest + "erdBAssta5day.zztop");
        } catch (Throwable t) {
            results = t.toString();
        }
        Test.ensureEqual(results, 
"java.io.IOException: HTTP status code=400 for URL: http://localhost:8080/cwexperimental/griddap/erdBAssta5day.zztop\n" +
"(Error {\n" +
"    code=400;\n" +
"    message=\"Bad Request: Query error: fileType=.zztop isn't supported by this dataset.\";\n" +
"})", 
            "results=\n" + results + comment);

        //401 Unauthorized
        try {
            results = SSR.getUrlResponseStringNewline(baseRequest + "testPrivate.html");
        } catch (Throwable t) {
            results = t.toString();
        }
        Test.ensureEqual(results, 
"java.io.IOException: HTTP status code=401 for URL: http://localhost:8080/cwexperimental/griddap/testPrivate.html\n" +
"(Error {\n" +
"    code=401;\n" +
"    message=\"Unauthorized: loggedInAs=null isn't authorized to access datasetID=testPrivate.\";\n" +
"})", 
            "results=\n" + results + comment);

        //404 not found: unknown datasetID (vs 400: emphasis here on correct syntax, but no such resource (perhaps temporary)
        try {
            results = SSR.getUrlResponseStringNewline(baseRequest + "erdBAssta5dayzztop.html");
        } catch (Throwable t) {
            results = t.toString();
        }
        Test.ensureEqual(results, 
"java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/griddap/erdBAssta5dayzztop.html\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: Currently unknown datasetID=erdBAssta5dayzztop\";\n" +
"})", 
            "results=\n" + results + comment);

        //404 not found: data out of range
        try {
            results = SSR.getUrlResponseStringNewline(baseRequest + "erdBAssta5day.csv?time%5B(2000-01-01):(2000-01-01)%5D");
        } catch (Throwable t) {
            results = t.toString();
        }
        Test.ensureEqual(results, 
"java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/griddap/erdBAssta5day.csv?time%5B(2000-01-01):(2000-01-01)%5D\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: Your query produced no matching results. Query error: For variable=time axis#0=time Constraint=\\\"[(2000-01-01):(2000-01-01)]\\\": Start=\\\"2000-01-01\\\" is less than the axis minimum=2002-07-06T12:00:00Z (and even 2002-07-05T22:49:16Z).\";\n" +
"})", 
            "results=\n" + results + comment);

        //413 Payload Too Large
        try {
            results = SSR.getUrlResponseStringNewline(baseRequest + "erdBAssta5day.nc?sst");
        } catch (Throwable t) {
            results = t.toString();
        }
        Test.ensureEqual(results, 
"java.io.IOException: HTTP status code=413 for URL: http://localhost:8080/cwexperimental/griddap/erdBAssta5day.nc?sst\n" +
"(Error {\n" +
"    code=413;\n" +
"    message=\"Payload Too Large: Your query produced too much data.  Try to request less data.  80001 MB is more than the .nc 2 GB limit.\";\n" +
"})", 
            "results=\n" + results + comment);

        //how test 500 Internal Server Error?
        /*
        try {
            results = SSR.getUrlResponseStringNewline(baseRequest + "");
        } catch (Throwable t) {
            results = t.toString();
       }
        Test.ensureEqual(results, 
"zztop", 
            "results=\n" + results + comment);
        */
        
    }


    /**
     * This tests reading a file with a long variable.
     *
     * @throws Throwable if trouble
     */
    public static void testGenerateDatasetsXmlLong2() throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testGenerateDatasetsXmlLong2()");
        testVerboseOn();

        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        int po;
        EDV edv;

        results = EDDGridFromNcFiles.generateDatasetsXml(
            //file isn't in /erddapTest because it is proprietary
            "/data/long2/", ".*\\.nc", "", 
            "", //group
            "", -1, "", null); //dimensionsCSV, reloadMinutes, cacheFromUrl
        String2.log(results);
        //remove testOutOfDate (varies or missing)
        results = results.replaceAll("        <att name=\"testOutOfDate\">now-\\d*days</att>\n",
                                     "");
        expected = 
"<dataset type=\"EDDGridFromNcFiles\" datasetID=\"long2_fb1b_899d_dde4\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>/data/long2/</fileDir>\n" +
"    <fileNameRegex>.*\\.nc</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <matchAxisNDigits>20</matchAxisNDigits>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"Conventions\">CF-1.6</att>\n" +
"        <att name=\"forcing_data_source\">Met Office; Global UM</att>\n";
        //proprietary
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

expected = 
    "<addAttributes>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"Conventions\">CF-1.6, COARDS, ACDD-1.3</att>\n" +
"        <att name=\"grid_mapping_inverse_flattening\" type=\"float\">298.25723</att>\n" +
"        <att name=\"grid_mapping_longitude_of_prime_meridian\" type=\"float\">0.0</att>\n" +
"        <att name=\"grid_mapping_name\">latitude_longitude</att>\n" +
"        <att name=\"grid_mapping_semi_major_axis\" type=\"float\">6378137.0</att>\n" +
"        <att name=\"history_of_appended_files\">Wed Jul  3 07:35:02 2019: Appended file maxwvht.tmp.nc had no &quot;history&quot; attribute</att>\n" +
"        <att name=\"infoUrl\">???</att>\n" +
"        <att name=\"institution\">???</att>\n" +
"        <att name=\"keywords\">data, height, latitude, local, longitude, maximum, sea, sea_surface_wave_maximum_height, source, surface, surface waves, time, VHMAX, wave, waves</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"NCO\">netCDF Operators version 4.7.6 (Homepage = http://nco.sf.net, Code = https://github.com/nco/nco)</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" +
"        <att name=\"summary\">Data from a local source.</att>\n" +
"        <att name=\"title\">Data from a local source.</att>\n" +
"    </addAttributes>\n" +
"    <axisVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSizes\" type=\"int\">524288</att>\n" +
"            <att name=\"axis\">T</att>\n" +
"            <att name=\"calendar\">Julian</att>\n" +
"            <att name=\"long_name\">time</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSizes\">null</att>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>latitude</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSizes\" type=\"int\">391</att>\n" +
"            <att name=\"axis\">Y</att>\n" +
"            <att name=\"long_name\">latitude</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degree_north</att>\n" +
"            <att name=\"valid_max\" type=\"float\">90.0</att>\n" +
"            <att name=\"valid_min\" type=\"float\">-90.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSizes\">null</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>longitude</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSizes\" type=\"int\">737</att>\n" +
"            <att name=\"axis\">X</att>\n" +
"            <att name=\"long_name\">longitude</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degree_east</att>\n" +
"            <att name=\"valid_max\" type=\"float\">360.0</att>\n" +
"            <att name=\"valid_min\" type=\"float\">0.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSizes\">null</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>VHMAX</sourceName>\n" +
"        <destinationName>VHMAX</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSizes\" type=\"intList\">61 391 737</att>\n" +
"            <att name=\"_FillValue\" type=\"short\">-32767</att>\n" +
"            <att name=\"add_offset\" type=\"float\">0.0</att>\n" +
"            <att name=\"calculation_method\">Expected wave height greater than 95.0th percentile of distribution</att>\n" +
"            <att name=\"cell_methods\">time:point</att>\n" +
"            <att name=\"coordinates\">forecast_period forecast_reference_time</att>\n" +
"            <att name=\"grid_mapping\">crs</att>\n" +
"            <att name=\"long_name\">Maximum wave height</att>\n" +
"            <att name=\"pdf\">Rayleigh</att>\n" +
"            <att name=\"scale_factor\" type=\"float\">0.002</att>\n" +
"            <att name=\"standard_name\">sea_surface_wave_maximum_height</att>\n" +
"            <att name=\"units\">m</att>\n" +
"            <att name=\"valid_max\" type=\"int\">16000</att>\n" +
"            <att name=\"valid_min\" type=\"int\">0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSizes\">null</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">40.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"coordinates\">null</att>\n" +
"            <att name=\"grid_mapping\">null</att>\n" +
"            <att name=\"ioos_category\">Surface Waves</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n";
        po = results.indexOf(expected.substring(0,15));
        Test.ensureEqual(results.substring(po), expected, "results=\n" + results);
        
        //ensure it is ready-to-use by making a dataset from it
        String tDatasetID = "long2_fb1b_899d_dde4";
        EDD.deleteCachedDatasetInfo(tDatasetID);
        EDD edd = oneFromXmlFragment(null, results);
        Test.ensureEqual(edd.datasetID(), tDatasetID, "");
        Test.ensureEqual(edd.title(), "Data from a local source.", "");
        Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
            "VHMAX", "");
        tName = edd.makeNewFileForDapQuery(null, null, 
            "VHMAX[(2019-07-03T00:00:00Z):1:(2019-07-03T00:00:00Z)][(25):1:(25.01)][(52):1:(52.01)]", 
            EDStatic.fullTestCacheDirectory, 
            edd.className() + "_TestLong2Grid", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        expected = 
"time,latitude,longitude,VHMAX\n" +
"UTC,degrees_north,degrees_east,m\n" +
"2019-07-03T00:00:00Z,25.004124,52.004135,0.552\n" +
"2019-07-03T00:00:00Z,25.004124,52.01247,0.566\n" +
"2019-07-03T00:00:00Z,25.012457,52.004135,0.558\n" +
"2019-07-03T00:00:00Z,25.012457,52.01247,0.566\n";
        Test.ensureEqual(results, expected, "results=\n" + results);        

        String2.log("\nEDDGridFromNcFiles.testGenerateDatasetsXmlLong2 passed the test.");

    }

    /**
     * This tests reading data from a variable with _Unsigned=true.
     *
     * @throws Throwable if trouble
     */
    public static void testUnsignedGrid() throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testUnsignedGrid()");
        testVerboseOn();

        String name, tName, results, tResults, expected;
        String error = "";
        int po;
        EDV edv;        
        String id = "testUnsignedGrid";  
        EDD.deleteCachedDatasetInfo(id);
        EDD edd = oneFromDatasetsXml(null, id); 
        String dapQuery = "palette[0][146:151]";  //a ubyte variable

        results = NcHelper.ncdump(String2.unitTestBigDataDir + 
            "nc/V20172742017304.L3m_MO_SNPP_CHL_chlor_a_4km.nc", "-h");
        expected = 
"netcdf V20172742017304.L3m_MO_SNPP_CHL_chlor_a_4km.nc {\n" +
"  dimensions:\n" +
"    lat = 4320;\n" +
"    rgb = 3;\n" +
"    eightbitcolor = 256;\n" +
"    lon = 8640;\n" +
"  variables:\n" +
"    float chlor_a(lat=4320, lon=8640);\n" +
"      :long_name = \"Chlorophyll Concentration, OCI Algorithm\";\n" +
"      :units = \"mg m^-3\";\n" +
"      :standard_name = \"mass_concentration_chlorophyll_concentration_in_sea_water\";\n" +
"      :_FillValue = -32767.0f; // float\n" +
"      :valid_min = 0.001f; // float\n" +
"      :valid_max = 100.0f; // float\n" +
"      :reference = \"Hu, C., Lee Z., and Franz, B.A. (2012). Chlorophyll-a algorithms for oligotrophic oceans: A novel approach based on three-band reflectance difference, J. Geophys. Res., 117, C01011, doi:10.1029/2011JC007395.\";\n" +
"      :display_scale = \"log\";\n" +
"      :display_min = 0.01f; // float\n" +
"      :display_max = 20.0f; // float\n" +
"      :_ChunkSizes = 44U, 87U; // uint\n" +
"\n" +
"    float lat(lat=4320);\n" +
"      :long_name = \"Latitude\";\n" +
"      :units = \"degrees_north\";\n" +
"      :standard_name = \"latitude\";\n" +
"      :_FillValue = -999.0f; // float\n" +
"      :valid_min = -90.0f; // float\n" +
"      :valid_max = 90.0f; // float\n" +
"\n" +
"    ubyte palette(rgb=3, eightbitcolor=256);\n" +
"\n" +
"    float lon(lon=8640);\n" +
"      :long_name = \"Longitude\";\n" +
"      :units = \"degrees_east\";\n" +
"      :standard_name = \"longitude\";\n" +
"      :_FillValue = -999.0f; // float\n" +
"      :valid_min = -180.0f; // float\n" +
"      :valid_max = 180.0f; // float\n" +
"\n" +
"  group: processing_control {\n" +
"    group: input_parameters {\n" +
"      // group attributes:\n" +
"      :par = \"V20172742017304.L3m_MO_SNPP_CHL_chlor_a_4km.nc.param\";\n" +
"      :ifile = \"V20172742017304.L3b_MO_SNPP_CHL.nc\";\n" +
"      :ofile = \"V20172742017304.L3m_MO_SNPP_CHL_chlor_a_4km.nc\";\n" +
"      :oformat = \"2\";\n" +
"      :oformat2 = \"png\";\n" +
"      :deflate = \"4\";\n" +
"      :product = \"chlor_a\";\n" +
"      :resolution = \"4km\";\n" +
"      :projection = \"smi\";\n" +
"      :central_meridian = \"-999\";\n" +
"      :interp = \"area\";\n" +
"      :north = \"90.000\";\n" +
"      :south = \"-90.000\";\n" +
"      :east = \"180.000\";\n" +
"      :west = \"-180.000\";\n" +
"      :apply_pal = \"yes\";\n" +
"      :palette_dir = \"$OCDATAROOT/common/palette\";\n" +
"      :quiet = \"false\";\n" +
"      :pversion = \"2014.0.2\";\n" +
"      :use_quality = \"yes\";\n" +
"      :use_rgb = \"no\";\n" +
"      :trimNSEW = \"yes\";\n" +
"      :product_rgb = \"rhos_671,rhos_551,rhos_486\";\n" +
"      :fudge = \"1.0\";\n" +
"      :threshold = \"0\";\n" +
"    }\n" +
"\n" +
"    // group attributes:\n" +
"    :source = \"V20172742017304.L3b_MO_SNPP_CHL.nc\";\n" +
"    :l2_flag_names = \"ATMFAIL,LAND,HILT,HISATZEN,STRAYLIGHT,CLDICE,COCCOLITH,LOWLW,CHLWARN,CHLFAIL,NAVWARN,MAXAERITER,ATMWARN,HISOLZEN,NAVFAIL,FILTER,HIGLINT\";\n" +
"    :software_version = \"1.0.1-V2016.4.3\";\n" +
"    :software_name = \"l3mapgen\";\n" +
"  }\n" +
"\n" +
"  // global attributes:\n" +
"  :product_name = \"V20172742017304.L3m_MO_SNPP_CHL_chlor_a_4km.nc\";\n" +
"  :instrument = \"VIIRS\";\n" +
"  :title = \"VIIRSN Level-3 Standard Mapped Image\";\n" +
"  :project = \"Ocean Biology Processing Group (NASA/GSFC/OBPG)\";\n" +
"  :platform = \"Suomi-NPP\";\n" +
"  :temporal_range = \"month\";\n" +
"  :processing_version = \"2014.0.2\";\n" +
"  :date_created = \"2017-11-18T01:49:38.000Z\";\n" +
"  :history = \"l3mapgen par=V20172742017304.L3m_MO_SNPP_CHL_chlor_a_4km.nc.param \";\n" +
"  :l2_flag_names = \"ATMFAIL,LAND,HILT,HISATZEN,STRAYLIGHT,CLDICE,COCCOLITH,LOWLW,CHLWARN,CHLFAIL,NAVWARN,MAXAERITER,ATMWARN,HISOLZEN,NAVFAIL,FILTER,HIGLINT\";\n" +
"  :time_coverage_start = \"2017-10-01T00:18:00.000Z\";\n" +
"  :time_coverage_end = \"2017-11-01T02:23:58.000Z\";\n" +
"  :start_orbit_number = 30711; // int\n" +
"  :end_orbit_number = 31151; // int\n" +
"  :map_projection = \"Equidistant Cylindrical\";\n" +
"  :latitude_units = \"degrees_north\";\n" +
"  :longitude_units = \"degrees_east\";\n" +
"  :northernmost_latitude = 90.0f; // float\n" +
"  :southernmost_latitude = -90.0f; // float\n" +
"  :westernmost_longitude = -180.0f; // float\n" +
"  :easternmost_longitude = 180.0f; // float\n" +
"  :geospatial_lat_max = 90.0f; // float\n" +
"  :geospatial_lat_min = -90.0f; // float\n" +
"  :geospatial_lon_max = 180.0f; // float\n" +
"  :geospatial_lon_min = -180.0f; // float\n" +
"  :grid_mapping_name = \"latitude_longitude\";\n" +
"  :latitude_step = 0.041666668f; // float\n" +
"  :longitude_step = 0.041666668f; // float\n" +
"  :sw_point_latitude = -89.979164f; // float\n" +
"  :sw_point_longitude = -179.97917f; // float\n" +
"  :geospatial_lon_resolution = 4.6383123f; // float\n" +
"  :geospatial_lat_resolution = 4.6383123f; // float\n" +
"  :geospatial_lat_units = \"degrees_north\";\n" +
"  :geospatial_lon_units = \"degrees_east\";\n" +
"  :spatialResolution = \"4.64 km\";\n" +
"  :number_of_lines = 4320; // int\n" +
"  :number_of_columns = 8640; // int\n" +
"  :measure = \"Mean\";\n" +
"  :suggested_image_scaling_minimum = 0.01f; // float\n" +
"  :suggested_image_scaling_maximum = 20.0f; // float\n" +
"  :suggested_image_scaling_type = \"LOG\";\n" +
"  :suggested_image_scaling_applied = \"No\";\n" +
"  :_lastModified = \"2017-11-18T01:49:38.000Z\";\n" +
"  :Conventions = \"CF-1.6\";\n" +
"  :institution = \"NASA Goddard Space Flight Center, Ocean Ecology Laboratory, Ocean Biology Processing Group\";\n" +
"  :standard_name_vocabulary = \"NetCDF Climate and Forecast (CF) Metadata Convention\";\n" +
"  :Metadata_Conventions = \"Unidata Dataset Discovery v1.0\";\n" +
"  :naming_authority = \"gov.nasa.gsfc.sci.oceandata\";\n" +
"  :id = \"V20172742017304.L3b_MO_SNPP_CHL.nc/L3/V20172742017304.L3b_MO_SNPP_CHL.nc\";\n" +
"  :license = \"http://science.nasa.gov/earth-science/earth-science-data/data-information-policy/\";\n" +
"  :creator_name = \"NASA/GSFC/OBPG\";\n" +
"  :publisher_name = \"NASA/GSFC/OBPG\";\n" +
"  :creator_email = \"data@oceancolor.gsfc.nasa.gov\";\n" +
"  :publisher_email = \"data@oceancolor.gsfc.nasa.gov\";\n" +
"  :creator_url = \"http://oceandata.sci.gsfc.nasa.gov\";\n" +
"  :publisher_url = \"http://oceandata.sci.gsfc.nasa.gov\";\n" +
"  :processing_level = \"L3 Mapped\";\n" +
"  :cdm_data_type = \"grid\";\n" +
"  :identifier_product_doi_authority = \"http://dx.doi.org\";\n" +
"  :identifier_product_doi = \"http://dx.doi.org\";\n" +
"  :keywords = \"Oceans > Ocean Chemistry > Chlorophyll; Oceans > Ocean Optics > Ocean Color\";\n" +
"  :keywords_vocabulary = \"NASA Global Change Master Directory (GCMD) Science Keywords\";\n" +
"  :data_bins = 16868629; // int\n" +
"  :data_minimum = 0.0049333195f; // float\n" +
"  :data_maximum = 99.27857f; // float\n" +
"}\n";
        Test.ensureEqual(results, expected, "results=\n" + results);        

        results = edd.findDataVariableByDestinationName("palette").combinedAttributes().toString();
        expected = 
//"    _Unsigned=true\n" +  //no longer needed internally
                            //test that _FillValue wasn't added by EDV constructor. 
"    ioos_category=Other\n" +
"    long_name=Palette\n";  
        Test.ensureEqual(results, expected, "results=\n" + results);        

        //.asc
        tName = edd.makeNewFileForDapQuery(null, null, 
            dapQuery, //DataType=ubyte
            EDStatic.fullTestCacheDirectory, 
            edd.className() + "_testUnsignedGrid", ".asc"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        expected = 
"Dataset {\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Byte palette[rgb = 1][eightbitcolor = 6];\n" +
"    MAPS:\n" +
"      Int32 rgb[rgb = 1];\n" +
"      Int16 eightbitcolor[eightbitcolor = 6];\n" +
"  } palette;\n" +
"} testUnsignedGrid;\n" +
"---------------------------------------------\n" +
"palette.palette[1][6]\n" +
"[0], 252, 0, 0, 255, 0, 5\n" +
"\n" +
"palette.rgb[1]\n" +
"0\n" +
"\n" +
"palette.eightbitcolor[6]\n" +
"146, 147, 148, 149, 150, 151\n";
        Test.ensureEqual(results, expected, "results=\n" + results);        

        //.csv
        tName = edd.makeNewFileForDapQuery(null, null, 
            dapQuery, //DataType=ubyte
            EDStatic.fullTestCacheDirectory, 
            edd.className() + "_testUnsignedGrid", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        expected = 
"rgb,eightbitcolor,palette\n" +
"count,count,\n" +
"0,146,252\n" + //from -4, important test of ubyte
"0,147,0\n" +
"0,148,0\n" +
"0,149,255\n" + //from -1, not a missing value.  This is important test of sourceMaxIsMV=false. 
"0,150,0\n" +
"0,151,5\n";
        Test.ensureEqual(results, expected, "results=\n" + results);        

        //.das
        tName = edd.makeNewFileForDapQuery(null, null, dapQuery, 
            EDStatic.fullTestCacheDirectory, 
            edd.className() + "_testUnsignedGrid", ".das"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        results = results.replaceAll("2\\d{3}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z ", "[TODAY] ");
        expected = 
"Attributes {\n" +
"  rgb {\n" +
"    Int32 actual_range 0, 2;\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"RGB\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  eightbitcolor {\n" +
"    Int16 actual_range 0, 255;\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Eightbitcolor\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  palette {\n" +
"    String _Unsigned \"true\";\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Palette\";\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String _lastModified \"2017-11-18T01:49:38.000Z\";\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String Conventions \"CF-1.6, COARDS, ACDD-1.3\";\n" +
"    String creator_email \"data@oceancolor.gsfc.nasa.gov\";\n" +
"    String creator_name \"NASA/GSFC/OBPG\";\n" +
"    String creator_type \"group\";\n" +
"    String creator_url \"https://oceandata.sci.gsfc.nasa.gov\";\n" +
"    String date_created \"2017-11-18T01:49:38.000Z\";\n" +
"    String grid_mapping_name \"latitude_longitude\";\n" +
"    String history \"l3mapgen par=V20172742017304.L3m_MO_SNPP_CHL_chlor_a_4km.nc.param\n" +
"[TODAY] (local files)\n" +
"[TODAY] http://localhost:8080/cwexperimental/griddap/testUnsignedGrid.das\";\n" +
"    String id \"V20172742017304.L3b_MO_SNPP_CHL.nc/L3/V20172742017304.L3b_MO_SNPP_CHL.nc\";\n" +
"    String identifier_product_doi \"https://dx.doi.org\";\n" +
"    String identifier_product_doi_authority \"https://dx.doi.org\";\n" +
"    String infoUrl \"https://oceandata.sci.gsfc.nasa.gov\";\n" +
"    String institution \"NASA/GSFC OBPG\";\n" +
"    String instrument \"VIIRS\";\n" +
"    String keywords \"biology, center, chemistry, chlorophyll, color, data, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, Earth Science > Oceans > Ocean Optics > Ocean Color, eightbitcolor, flight, goddard, group, gsfc, image, imager, imager/radiometer, imaging, infrared, L3, level, level-3, mapped, nasa, national, npp, obpg, ocean, oceans, optics, orbiting, palette, partnership, polar, polar-orbiting, processing, radiometer, rgb, smi, space, standard, suite, suite/suomi-npp, suomi, viirs, viirs-n, viirsn, visible\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String l2_flag_names \"ATMFAIL,LAND,HILT,HISATZEN,STRAYLIGHT,CLDICE,COCCOLITH,LOWLW,CHLWARN,CHLFAIL,NAVWARN,MAXAERITER,ATMWARN,HISOLZEN,NAVFAIL,FILTER,HIGLINT\";\n" +
"    String license \"https://science.nasa.gov/earth-science/earth-science-data/data-information-policy/\";\n" +
"    String map_projection \"Equidistant Cylindrical\";\n" +
"    String measure \"Mean\";\n" +
"    String naming_authority \"gov.nasa.gsfc.sci.oceandata\";\n" +
"    String platform \"Suomi-NPP\";\n" +
"    String processing_level \"L3 Mapped\";\n" +
"    String processing_version \"2014.0.2\";\n" +
"    String product_name \"V20172742017304.L3m_MO_SNPP_CHL_chlor_a_4km.nc\";\n" +
"    String project \"Ocean Biology Processing Group (NASA/GSFC/OBPG)\";\n" +
"    String publisher_email \"data@oceancolor.gsfc.nasa.gov\";\n" +
"    String publisher_name \"NASA/GSFC/OBPG\";\n" +
"    String publisher_type \"group\";\n" +
"    String publisher_url \"https://oceandata.sci.gsfc.nasa.gov\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    String spatialResolution \"4.64 km\";\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String summary \"Visible and Infrared Imager/Radiometer Suite/Suomi-NPP (VIIRSN) Level-3 Standard Mapped Image\";\n" +
"    String temporal_range \"month\";\n" +
"    String title \"VIIRSN L3 SMI,\";\n" +
"  }\n" +
"}\n";
        Test.ensureEqual(results, expected, "results=\n" + results);        

        //.dds
        tName = edd.makeNewFileForDapQuery(null, null, dapQuery, 
            EDStatic.fullTestCacheDirectory, 
            edd.className() + "_testUnsignedGrid", ".dds"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        expected = 
"Dataset {\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Byte palette[rgb = 1][eightbitcolor = 6];\n" +
"    MAPS:\n" +
"      Int32 rgb[rgb = 1];\n" +
"      Int16 eightbitcolor[eightbitcolor = 6];\n" +
"  } palette;\n" +
"} testUnsignedGrid;\n";
        Test.ensureEqual(results, expected, "results=\n" + results);        

        //.esriAscii   //not available because no lat and lon

        //.htmlTable
        tName = edd.makeNewFileForDapQuery(null, null, dapQuery, 
            EDStatic.fullTestCacheDirectory, 
            edd.className() + "_testUnsignedGrid", ".htmlTable"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        expected = 
"<table class=\"erd commonBGColor nowrap\">\n" +
"<tr>\n" +
"<th>rgb\n" +
"<th>eightbitcolor\n" +
"<th>palette\n" +
"</tr>\n" +
"<tr>\n" +
"<th>count\n" +
"<th>count\n" +
"<th>\n" +
"</tr>\n" +
"<tr>\n" +
"<td class=\"R\">0\n" +
"<td class=\"R\">146\n" +
"<td class=\"R\">252\n" +
"</tr>\n" +
"<tr>\n" +
"<td class=\"R\">0\n" +
"<td class=\"R\">147\n" +
"<td class=\"R\">0\n" +
"</tr>\n" +
"<tr>\n" +
"<td class=\"R\">0\n" +
"<td class=\"R\">148\n" +
"<td class=\"R\">0\n" +
"</tr>\n" +
"<tr>\n" +
"<td class=\"R\">0\n" +
"<td class=\"R\">149\n" +
"<td class=\"R\">255\n" +
"</tr>\n" +
"<tr>\n" +
"<td class=\"R\">0\n" +
"<td class=\"R\">150\n" +
"<td class=\"R\">0\n" +
"</tr>\n" +
"<tr>\n" +
"<td class=\"R\">0\n" +
"<td class=\"R\">151\n" +
"<td class=\"R\">5\n" +
"</tr>\n" +
"</table>\n";
        po = results.indexOf(expected.substring(0, 40));
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, "results=\n" + results);        

        //.itx
        tName = edd.makeNewFileForDapQuery(null, null, dapQuery, 
            EDStatic.fullTestCacheDirectory, 
            edd.className() + "_testUnsignedGrid", ".itx"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        results = String2.replaceAll(results, '\r', '\n');
        expected = 
"IGOR\n" +
"WAVES/I rgb\n" +
"BEGIN\n" +
"0\n" +
"END\n" +
"X SetScale d 0,0, \"count\", rgb\n" +
"\n" +
"WAVES/W eightbitcolor\n" +
"BEGIN\n" +
"146\n" +
"147\n" +
"148\n" +
"149\n" +
"150\n" +
"151\n" +
"END\n" +
"X SetScale d 146,151, \"count\", eightbitcolor\n" +
"\n" +
"WAVES/B/U/N=(1,6) palette\n" +  //B/U is unsigned byte. See Table.writeIgorWave()
"BEGIN\n" +
"252\n" +
"0\n" +
"0\n" +
"255\n" +
"0\n" +
"5\n" +
"END\n" +
"X SetScale d 0,255, \"\", palette\n" +
"X SetScale /I y, 0,0, \"count\", palette\n" +
"X SetScale /P x, 146,1.0, \"count\", palette\n" +
"X Note palette, \"RowsDim:eightbitcolor;ColumnsDim:rgb\"\n" +
"\n";
        Test.ensureEqual(results, expected, "results=\n" + results);        

        //.json
        tName = edd.makeNewFileForDapQuery(null, null, dapQuery, 
            EDStatic.fullTestCacheDirectory, 
            edd.className() + "_testUnsignedGrid", ".json"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        expected = 
"{\n" +
"  \"table\": {\n" +
"    \"columnNames\": [\"rgb\", \"eightbitcolor\", \"palette\"],\n" +
"    \"columnTypes\": [\"int\", \"short\", \"ubyte\"],\n" +
"    \"columnUnits\": [\"count\", \"count\", null],\n" +
"    \"rows\": [\n" +
"      [0, 146, 252],\n" + //important test of ubyte
"      [0, 147, 0],\n" +
"      [0, 148, 0],\n" +
"      [0, 149, 255],\n" + //important test of ubyte sourceMaxIsMV=false
"      [0, 150, 0],\n" +
"      [0, 151, 5]\n" +
"    ]\n" +
"  }\n" +
"}\n";
        Test.ensureEqual(results, expected, "results=\n" + results);        

        //.jsonlCSV1
        tName = edd.makeNewFileForDapQuery(null, null, dapQuery, 
            EDStatic.fullTestCacheDirectory, 
            edd.className() + "_testUnsignedGrid", ".jsonlCSV1"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        expected = 
"[\"rgb\", \"eightbitcolor\", \"palette\"]\n" +
"[0, 146, 252]\n" +
"[0, 147, 0]\n" +
"[0, 148, 0]\n" +
"[0, 149, 255]\n" +
"[0, 150, 0]\n" +
"[0, 151, 5]\n";
        Test.ensureEqual(results, expected, "results=\n" + results);        

        //.jsonlCSV1
        tName = edd.makeNewFileForDapQuery(null, null, dapQuery, 
            EDStatic.fullTestCacheDirectory, 
            edd.className() + "_testUnsignedGrid", ".jsonlKVP"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        expected = 
"{\"rgb\":0, \"eightbitcolor\":146, \"palette\":252}\n" +
"{\"rgb\":0, \"eightbitcolor\":147, \"palette\":0}\n" +
"{\"rgb\":0, \"eightbitcolor\":148, \"palette\":0}\n" +
"{\"rgb\":0, \"eightbitcolor\":149, \"palette\":255}\n" +
"{\"rgb\":0, \"eightbitcolor\":150, \"palette\":0}\n" +
"{\"rgb\":0, \"eightbitcolor\":151, \"palette\":5}\n";
        Test.ensureEqual(results, expected, "results=\n" + results);        

        //.nccsv
        tName = edd.makeNewFileForDapQuery(null, null, dapQuery, 
            EDStatic.fullTestCacheDirectory, 
            edd.className() + "_testUnsignedGrid", ".nccsv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        results = results.replaceAll("2\\d{3}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z ", "[TODAY] ");
        expected = 
"*GLOBAL*,Conventions,\"CF-1.6, COARDS, ACDD-1.3, NCCSV-1.1\"\n" +
"*GLOBAL*,_lastModified,2017-11-18T01:49:38.000Z\n" +
"*GLOBAL*,cdm_data_type,Grid\n" +
"*GLOBAL*,creator_email,data@oceancolor.gsfc.nasa.gov\n" +
"*GLOBAL*,creator_name,NASA/GSFC/OBPG\n" +
"*GLOBAL*,creator_type,group\n" +
"*GLOBAL*,creator_url,https://oceandata.sci.gsfc.nasa.gov\n" +
"*GLOBAL*,date_created,2017-11-18T01:49:38.000Z\n" +
"*GLOBAL*,grid_mapping_name,latitude_longitude\n" +
"*GLOBAL*,history,l3mapgen par=V20172742017304.L3m_MO_SNPP_CHL_chlor_a_4km.nc.param\\n[TODAY] (local files)\\n[TODAY] http://localhost:8080/cwexperimental/griddap/testUnsignedGrid.nccsv?palette[0][146:151]\n" +
"*GLOBAL*,id,V20172742017304.L3b_MO_SNPP_CHL.nc/L3/V20172742017304.L3b_MO_SNPP_CHL.nc\n" +
"*GLOBAL*,identifier_product_doi,https://dx.doi.org\n" +
"*GLOBAL*,identifier_product_doi_authority,https://dx.doi.org\n" +
"*GLOBAL*,infoUrl,https://oceandata.sci.gsfc.nasa.gov\n" +
"*GLOBAL*,institution,NASA/GSFC OBPG\n" +
"*GLOBAL*,instrument,VIIRS\n" +
"*GLOBAL*,keywords,\"biology, center, chemistry, chlorophyll, color, data, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, Earth Science > Oceans > Ocean Optics > Ocean Color, eightbitcolor, flight, goddard, group, gsfc, image, imager, imager/radiometer, imaging, infrared, L3, level, level-3, mapped, nasa, national, npp, obpg, ocean, oceans, optics, orbiting, palette, partnership, polar, polar-orbiting, processing, radiometer, rgb, smi, space, standard, suite, suite/suomi-npp, suomi, viirs, viirs-n, viirsn, visible\"\n" +
"*GLOBAL*,keywords_vocabulary,GCMD Science Keywords\n" +
"*GLOBAL*,l2_flag_names,\"ATMFAIL,LAND,HILT,HISATZEN,STRAYLIGHT,CLDICE,COCCOLITH,LOWLW,CHLWARN,CHLFAIL,NAVWARN,MAXAERITER,ATMWARN,HISOLZEN,NAVFAIL,FILTER,HIGLINT\"\n" +
"*GLOBAL*,license,https://science.nasa.gov/earth-science/earth-science-data/data-information-policy/\n" +
"*GLOBAL*,map_projection,Equidistant Cylindrical\n" +
"*GLOBAL*,measure,Mean\n" +
"*GLOBAL*,naming_authority,gov.nasa.gsfc.sci.oceandata\n" +
"*GLOBAL*,platform,Suomi-NPP\n" +
"*GLOBAL*,processing_level,L3 Mapped\n" +
"*GLOBAL*,processing_version,\"2014.0.2\"\n" +
"*GLOBAL*,product_name,V20172742017304.L3m_MO_SNPP_CHL_chlor_a_4km.nc\n" +
"*GLOBAL*,project,Ocean Biology Processing Group (NASA/GSFC/OBPG)\n" +
"*GLOBAL*,publisher_email,data@oceancolor.gsfc.nasa.gov\n" +
"*GLOBAL*,publisher_name,NASA/GSFC/OBPG\n" +
"*GLOBAL*,publisher_type,group\n" +
"*GLOBAL*,publisher_url,https://oceandata.sci.gsfc.nasa.gov\n" +
"*GLOBAL*,sourceUrl,(local files)\n" +
"*GLOBAL*,spatialResolution,4.64 km\n" +
"*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v70\n" +
"*GLOBAL*,summary,Visible and Infrared Imager/Radiometer Suite/Suomi-NPP (VIIRSN) Level-3 Standard Mapped Image\n" +
"*GLOBAL*,temporal_range,month\n" +
"*GLOBAL*,title,\"VIIRSN L3 SMI,\"\n" +
"rgb,*DATA_TYPE*,int\n" +
"rgb,actual_range,0i,0i\n" +
"rgb,ioos_category,Other\n" +
"rgb,long_name,RGB\n" +
"rgb,units,count\n" +
"eightbitcolor,*DATA_TYPE*,short\n" +
"eightbitcolor,actual_range,146s,151s\n" +
"eightbitcolor,ioos_category,Other\n" +
"eightbitcolor,long_name,Eightbitcolor\n" +
"eightbitcolor,units,count\n" +
"palette,*DATA_TYPE*,ubyte\n" +
"palette,ioos_category,Other\n" +
"palette,long_name,Palette\n" +
"\n" +
"*END_METADATA*\n" +
"rgb,eightbitcolor,palette\n" +
"0,146,252\n" +
"0,147,0\n" +
"0,148,0\n" +
"0,149,255\n" +
"0,150,0\n" +
"0,151,5\n" +
"*END_DATA*\n";
        Test.ensureEqual(results, expected, "results=\n" + results);        

        //.ncml
        tName = edd.makeNewFileForDapQuery(null, null, dapQuery, 
            EDStatic.fullTestCacheDirectory, 
            edd.className() + "_testUnsignedGrid", ".ncml"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        expected = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<netcdf xmlns=\"https://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2\" location=\"http://localhost:8080/griddap/testUnsignedGrid\">\n" +
"  <attribute name=\"_lastModified\" value=\"2017-11-18T01:49:38.000Z\" />\n" +
"  <attribute name=\"cdm_data_type\" value=\"Grid\" />\n" +
"  <attribute name=\"Conventions\" value=\"CF-1.6, COARDS, ACDD-1.3\" />\n" +
"  <attribute name=\"creator_email\" value=\"data@oceancolor.gsfc.nasa.gov\" />\n" +
"  <attribute name=\"creator_name\" value=\"NASA/GSFC/OBPG\" />\n" +
"  <attribute name=\"creator_type\" value=\"group\" />\n" +
"  <attribute name=\"creator_url\" value=\"https://oceandata.sci.gsfc.nasa.gov\" />\n" +
"  <attribute name=\"date_created\" value=\"2017-11-18T01:49:38.000Z\" />\n" +
"  <attribute name=\"grid_mapping_name\" value=\"latitude_longitude\" />\n" +
"  <attribute name=\"history\" value=\"l3mapgen par=V20172742017304.L3m_MO_SNPP_CHL_chlor_a_4km.nc.param\" />\n" +
"  <attribute name=\"id\" value=\"V20172742017304.L3b_MO_SNPP_CHL.nc/L3/V20172742017304.L3b_MO_SNPP_CHL.nc\" />\n" +
"  <attribute name=\"identifier_product_doi\" value=\"https://dx.doi.org\" />\n" +
"  <attribute name=\"identifier_product_doi_authority\" value=\"https://dx.doi.org\" />\n" +
"  <attribute name=\"infoUrl\" value=\"https://oceandata.sci.gsfc.nasa.gov\" />\n" +
"  <attribute name=\"institution\" value=\"NASA/GSFC OBPG\" />\n" +
"  <attribute name=\"instrument\" value=\"VIIRS\" />\n" +
"  <attribute name=\"keywords\" value=\"biology, center, chemistry, chlorophyll, color, data, Earth Science &gt; Oceans &gt; Ocean Chemistry &gt; Chlorophyll, Earth Science &gt; Oceans &gt; Ocean Optics &gt; Ocean Color, eightbitcolor, flight, goddard, group, gsfc, image, imager, imager/radiometer, imaging, infrared, L3, level, level-3, mapped, nasa, national, npp, obpg, ocean, oceans, optics, orbiting, palette, partnership, polar, polar-orbiting, processing, radiometer, rgb, smi, space, standard, suite, suite/suomi-npp, suomi, viirs, viirs-n, viirsn, visible\" />\n" +
"  <attribute name=\"keywords_vocabulary\" value=\"GCMD Science Keywords\" />\n" +
"  <attribute name=\"l2_flag_names\" value=\"ATMFAIL,LAND,HILT,HISATZEN,STRAYLIGHT,CLDICE,COCCOLITH,LOWLW,CHLWARN,CHLFAIL,NAVWARN,MAXAERITER,ATMWARN,HISOLZEN,NAVFAIL,FILTER,HIGLINT\" />\n" +
"  <attribute name=\"license\" value=\"https://science.nasa.gov/earth-science/earth-science-data/data-information-policy/\" />\n" +
"  <attribute name=\"map_projection\" value=\"Equidistant Cylindrical\" />\n" +
"  <attribute name=\"measure\" value=\"Mean\" />\n" +
"  <attribute name=\"naming_authority\" value=\"gov.nasa.gsfc.sci.oceandata\" />\n" +
"  <attribute name=\"platform\" value=\"Suomi-NPP\" />\n" +
"  <attribute name=\"processing_level\" value=\"L3 Mapped\" />\n" +
"  <attribute name=\"processing_version\" value=\"2014.0.2\" />\n" +
"  <attribute name=\"product_name\" value=\"V20172742017304.L3m_MO_SNPP_CHL_chlor_a_4km.nc\" />\n" +
"  <attribute name=\"project\" value=\"Ocean Biology Processing Group (NASA/GSFC/OBPG)\" />\n" +
"  <attribute name=\"publisher_email\" value=\"data@oceancolor.gsfc.nasa.gov\" />\n" +
"  <attribute name=\"publisher_name\" value=\"NASA/GSFC/OBPG\" />\n" +
"  <attribute name=\"publisher_type\" value=\"group\" />\n" +
"  <attribute name=\"publisher_url\" value=\"https://oceandata.sci.gsfc.nasa.gov\" />\n" +
"  <attribute name=\"sourceUrl\" value=\"(local files)\" />\n" +
"  <attribute name=\"spatialResolution\" value=\"4.64 km\" />\n" +
"  <attribute name=\"standard_name_vocabulary\" value=\"CF Standard Name Table v70\" />\n" +
"  <attribute name=\"summary\" value=\"Visible and Infrared Imager/Radiometer Suite/Suomi-NPP (VIIRSN) Level-3 Standard Mapped Image\" />\n" +
"  <attribute name=\"temporal_range\" value=\"month\" />\n" +
"  <attribute name=\"title\" value=\"VIIRSN L3 SMI,\" />\n" +
"  <dimension name=\"rgb\" length=\"3\" />\n" +
"  <dimension name=\"eightbitcolor\" length=\"256\" />\n" +
"  <variable name=\"rgb\" shape=\"rgb\" type=\"int\">\n" +
"    <attribute name=\"actual_range\" type=\"int\" value=\"0 2\" />\n" +
"    <attribute name=\"ioos_category\" value=\"Other\" />\n" +
"    <attribute name=\"long_name\" value=\"RGB\" />\n" +
"    <attribute name=\"units\" value=\"count\" />\n" +
"  </variable>\n" +
"  <variable name=\"eightbitcolor\" shape=\"eightbitcolor\" type=\"short\">\n" +
"    <attribute name=\"actual_range\" type=\"short\" value=\"0 255\" />\n" +
"    <attribute name=\"ioos_category\" value=\"Other\" />\n" +
"    <attribute name=\"long_name\" value=\"Eightbitcolor\" />\n" +
"    <attribute name=\"units\" value=\"count\" />\n" +
"  </variable>\n" +
"  <variable name=\"palette\" shape=\"rgb eightbitcolor\" type=\"ubyte\">\n" +
"    <attribute name=\"ioos_category\" value=\"Other\" />\n" +
"    <attribute name=\"long_name\" value=\"Palette\" />\n" +
"  </variable>\n" +
"</netcdf>\n";
        Test.ensureEqual(results, expected, "results=\n" + results);        

        //.odvTxt -- must have lat and lon

        //.tsv
        tName = edd.makeNewFileForDapQuery(null, null, dapQuery, 
            EDStatic.fullTestCacheDirectory, 
            edd.className() + "_testUnsignedGrid", ".tsv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        expected = 
"rgb\teightbitcolor\tpalette\n" +
"count\tcount\t\n" +
"0\t146\t252\n" +
"0\t147\t0\n" +
"0\t148\t0\n" +
"0\t149\t255\n" +
"0\t150\t0\n" +
"0\t151\t5\n";
        Test.ensureEqual(results, expected, "results=\n" + results);        

        //.xhtml
        tName = edd.makeNewFileForDapQuery(null, null, dapQuery, 
            EDStatic.fullTestCacheDirectory, 
            edd.className() + "_testUnsignedGrid", ".xhtml"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        expected = 
"<table class=\"erd commonBGColor nowrap\">\n" +
"<tr>\n" +
"<th>rgb</th>\n" +
"<th>eightbitcolor</th>\n" +
"<th>palette</th>\n" +
"</tr>\n" +
"<tr>\n" +
"<th>count</th>\n" +
"<th>count</th>\n" +
"<th></th>\n" +
"</tr>\n" +
"<tr>\n" +
"<td class=\"R\">0</td>\n" +
"<td class=\"R\">146</td>\n" +
"<td class=\"R\">252</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td class=\"R\">0</td>\n" +
"<td class=\"R\">147</td>\n" +
"<td class=\"R\">0</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td class=\"R\">0</td>\n" +
"<td class=\"R\">148</td>\n" +
"<td class=\"R\">0</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td class=\"R\">0</td>\n" +
"<td class=\"R\">149</td>\n" +
"<td class=\"R\">255</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td class=\"R\">0</td>\n" +
"<td class=\"R\">150</td>\n" +
"<td class=\"R\">0</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td class=\"R\">0</td>\n" +
"<td class=\"R\">151</td>\n" +
"<td class=\"R\">5</td>\n" +
"</tr>\n" +
"</table>";
        po = results.indexOf(expected.substring(0, 40));
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, "results=\n" + results);        

        String2.log("\nEDDGridFromNcFiles.testUnsignedGrid passed the test.");

    }

    /**
     * This tests nThreads with making requests for 1 datum from numerous files.
     * This also ensures the .gz caching works (although nothing in cache is reused). 
     *
     * @param maxNThreads E.g. 3 will test nThreads=3,2,1,1,2,3.
     * @throws Throwable if trouble
     */
    public static void testNThreads(int maxNThreads) throws Throwable {
        String2.log("\n****************** EDDGridFromNcFiles.testNThreads() *****************\n");

        testVerboseOn();
        String tName, results,  expected, userDapQuery, tQuery;
        String error = "";
        String tDir = EDStatic.fullTestCacheDirectory;
        String id = "testGridNThreads";
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, id); 
        StringBuilder bigResults = new StringBuilder("\nEDDGridFromNcFiles.testNThreads finished.\n");

        for (int nt = -maxNThreads; nt <= maxNThreads; nt++) {
            if (nt == 0)
                continue;
            //delete all files in .gz cache dir
            File2.deleteAllFiles(eddGrid.decompressedDirectory());
            Math2.gc(30000); //let system settle

            long tTime = System.currentTimeMillis();
            eddGrid.nThreads = Math.abs(nt);
            tName = eddGrid.makeNewFileForDapQuery(null, null, 
                "taux[0:150][][(30)][(330)]", //around time=[210], it slows way down for awhile (something in Windows: virus check?)
                tDir, "EDDGridFromNcFiles.testNThreads" + nt, ".csv"); 
            results = String2.directReadFrom88591File(tDir + tName);
            //String2.log(results);            
            expected = 
"time,altitude,latitude,longitude,taux\n" +
"UTC,m,degrees_north,degrees_east,Pa\n" +
"2009-10-03T12:00:00Z,0.0,30.0,330.0,NaN\n" +
"2009-10-04T12:00:00Z,0.0,30.0,330.0,NaN\n" +
"2009-10-05T12:00:00Z,0.0,30.0,330.0,0.0740024\n" +
"2009-10-06T12:00:00Z,0.0,30.0,330.0,NaN\n" +
"2009-10-07T12:00:00Z,0.0,30.0,330.0,-0.00820897\n" +
"2009-10-08T12:00:00Z,0.0,30.0,330.0,NaN\n" +
"2009-10-09T12:00:00Z,0.0,30.0,330.0,-0.025654\n" +
"2009-10-10T12:00:00Z,0.0,30.0,330.0,-0.0355719\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

            String msg = "nThreads=" + Math.abs(nt) + " time=" + (System.currentTimeMillis() - tTime) + "ms\n";
            String2.log(msg);
            bigResults.append(msg);
        }
        String2.log(bigResults.toString());
        String2.log("  (Lenovo: 2 cores: nThreads/s 3/4,2/3,1/3,1/4,2/3,3/3");
        Math2.gc(10000);
    }

    /**
     * This tests the /files/ "files" system.
     * This requires nceiPH53sstn1day in the local ERDDAP.
     *
     * EDDGridFromNcFiles.testFiles() has more tests than any other testFiles().
     */
    public static void testFiles() throws Throwable {

        String2.log("\n*** EDDGridFromNcFiles.testFiles()\n");
        String tDir = EDStatic.fullTestCacheDirectory;
        String dapQuery, tName, start, query, results, expected;
        int po;

        try {
            //get /files/.csv
            results = String2.annotatedString(SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/.csv"));
            Test.ensureTrue(results.indexOf("Name,Last modified,Size,Description[10]") 
                == 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("nceiPH53sstn1day/,NaN,NaN,\"AVHRR Pathfinder Version 5.3 L3-Collated (L3C) SST, Global, 0.0417\\u00b0, 1981-present, Nighttime (1 Day Composite)\"[10]") 
                > 0,  "results=\n" + results);
            Test.ensureTrue(results.indexOf("testTableAscii/,NaN,NaN,The Title for testTableAscii[10]")               
                > 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("documentation.html,") 
                > 0, "results=\n" + results);

            //get /files/datasetID/.csv
            results = SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/nceiPH53sstn1day/.csv");
            expected = 
"Name,Last modified,Size,Description\n" +
"1981/,NaN,NaN,\n" +
"1994/,NaN,NaN,\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //get /files/datasetID/
            results = SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/nceiPH53sstn1day/");
            Test.ensureTrue(results.indexOf("1981&#x2f;") > 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("1981/")      > 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("1994&#x2f;") > 0, "results=\n" + results);

            //get /files/datasetID  //missing trailing slash will be redirected
            results = SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/nceiPH53sstn1day");
            Test.ensureTrue(results.indexOf("1981&#x2f;") > 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("1981/")      > 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("1994&#x2f;") > 0, "results=\n" + results);

            //get /files/datasetID/subdir/.csv
            results = SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/nceiPH53sstn1day/1994/.csv");
            expected = 
"Name,Last modified,Size,Description\n" +
"data/,NaN,NaN,\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //get /files/datasetID/subdir/subdir.csv
            results = SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/nceiPH53sstn1day/1994/data/.csv");
            expected = 
"Name,Last modified,Size,Description\n" +
"19940913000030-NCEI-L3C_GHRSST-SSTskin-AVHRR_Pathfinder-PFV5.3_NOAA09_G_1994256_night-v02.0-fv01.0.nc,1471330800000,12484412,\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //download a file in root -- none available

            //download a file in subdir
            results = String2.annotatedString(SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/nceiPH53sstn1day/1994/data/" +
                "19940913000030-NCEI-L3C_GHRSST-SSTskin-AVHRR_Pathfinder-PFV5.3_NOAA09_G_1994256_night-v02.0-fv01.0.nc").substring(0, 50));
            expected = 
"[137]HDF[10]\n" +
"[26][10]\n" +
"[2][8][8][0][0][0][0][0][0][0][0][0][255][255][255][255][255][255][255][255]<[127][190][0][0][0][0][0]0[0][0][0][0][0][0][0][199](*yOHD[end]";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //query with // at start fails
            try {
                results = SSR.getUrlResponseStringNewline(
                    "http://localhost:8080/cwexperimental/files//.csv");
            } catch (Exception e) { 
                results = e.toString();
            }
            expected = 
"java.io.IOException: HTTP status code=400 for URL: http://localhost:8080/cwexperimental/files//.csv\n" +
"(Error {\n" +
"    code=400;\n" +
"    message=\"Bad Request: Query error: // is not allowed!\";\n" +
"})";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //query with // later fails
            try {
                results = SSR.getUrlResponseStringNewline(
                    "http://localhost:8080/cwexperimental/files/nceiPH53sstn1day//.csv");
            } catch (Exception e) { 
                results = e.toString();
            }
            expected = 
"java.io.IOException: HTTP status code=400 for URL: http://localhost:8080/cwexperimental/files/nceiPH53sstn1day//.csv\n" +
"(Error {\n" +
"    code=400;\n" +
"    message=\"Bad Request: Query error: // is not allowed!\";\n" +
"})";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //query with /../ fails
            try {
                results = SSR.getUrlResponseStringNewline(
                    "http://localhost:8080/cwexperimental/files/nceiPH53sstn1day/../");
            } catch (Exception e) { 
                results = e.toString();
            }
            expected = 
"java.io.IOException: HTTP status code=400 for URL: http://localhost:8080/cwexperimental/files/nceiPH53sstn1day/../\n" +
"(Error {\n" +
"    code=400;\n" +
"    message=\"Bad Request: Query error: /../ is not allowed!\";\n" +
"})";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //try to download a non-existent dataset
            try {
                results = SSR.getUrlResponseStringNewline(
                    "http://localhost:8080/cwexperimental/files/gibberish/");
            } catch (Exception e) { 
                results = e.toString();
            }
            expected = 
"java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/gibberish/\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: Currently unknown datasetID=gibberish\";\n" +
"})";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //try to download a non-existent datasetID
            try {
                results = SSR.getUrlResponseStringNewline(
                    "http://localhost:8080/cwexperimental/files/gibberish/");
            } catch (Exception e) { 
                results = e.toString();
            }
            expected = 
"java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/gibberish/\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: Currently unknown datasetID=gibberish\";\n" +
"})";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //try to download a existent subdirectory but without trailing slash
            try {
                results = SSR.getUrlResponseStringNewline(
                    "http://localhost:8080/cwexperimental/files/nceiPH53sstn1day/GLsubdir");
            } catch (Exception e) { 
                results = e.toString();
            }
            expected = 
"java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/nceiPH53sstn1day/GLsubdir\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: File not found: GLsubdir .\";\n" +
"})";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //try to download a non-existent directory
            try {
                results = SSR.getUrlResponseStringNewline(
                    "http://localhost:8080/cwexperimental/files/nceiPH53sstn1day/gibberish/");
            } catch (Exception e) { 
                results = e.toString();
            }
            expected = 
"java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/nceiPH53sstn1day/gibberish/\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: Resource not found: directory=gibberish/\";\n" +
"})";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //try to download a non-existent file in root
            try {
                results = SSR.getUrlResponseStringNewline(
                    "http://localhost:8080/cwexperimental/files/nceiPH53sstn1day/gibberish.csv");
            } catch (Exception e) { 
                results = e.toString();
            }
            expected = 
"java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/nceiPH53sstn1day/gibberish.csv\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: File not found: gibberish.csv .\";\n" +
"})";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //try to download a non-existent file in existent subdir
            try {
                results = SSR.getUrlResponseStringNewline(
                    "http://localhost:8080/cwexperimental/files/nceiPH53sstn1day/GLsubdir/gibberish.csv");
            } catch (Exception e) { 
                results = e.toString();
            }
            expected = 
"java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/nceiPH53sstn1day/GLsubdir/gibberish.csv\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: File not found: gibberish.csv .\";\n" +
"})";
            Test.ensureEqual(results, expected, "results=\n" + results);


        } catch (Throwable t) {
            throw new RuntimeException("Unexpected error. This test requires nceiPH53sstn1day in the localhost ERDDAP.", t); 
        } 
    }


    /**
     * This tests the bathymetry shift for an island.
     *
     * @throws Throwable if trouble
     */
    public static void testIslandShift() throws Throwable {
        String2.log("\n****************** EDDGridFromNcFiles.testIslandShift() *****************\n");
        testVerboseOn();

        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDVGridAxis edvga;
        String id = "testIslandShift";
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, id); 
        String tDir = EDStatic.fullTestCacheDirectory;

        for (int i = 1; i <= 4; i++) {
            tName = eddGrid.makeNewFileForDapQuery(null, null, 
                "&.land=" + SgtMap.drawLandMask_OPTIONS[i],
                tDir, "testIslandShift_" + SgtMap.drawLandMask_OPTIONS[i], ".png"); 
            SSR.displayInBrowser("file://" + tDir + tName);
        }
    }


    /**
     * This runs all of the interactive or not interactive tests for this class.
     *
     * @param errorSB all caught exceptions are logged to this.
     * @param interactive  If true, this runs all of the interactive tests; 
     *   otherwise, this runs all of the non-interactive tests.
     * @param doSlowTestsToo If true, this runs the slow tests, too.
     * @param firstTest The first test to be run (0...).  Test numbers may change.
     * @param lastTest The last test to be run, inclusive (0..., or -1 for the last test). 
     *   Test numbers may change.
     */
    public static void test(StringBuilder errorSB, boolean interactive, 
        boolean doSlowTestsToo, int firstTest, int lastTest) {
        if (lastTest < 0)
            lastTest = interactive? 11 : 54;
        String msg = "\n^^^ EDDGridFromNcFiles.test(" + interactive + ") test=";

        boolean deleteCachedDatasetInfo = true; 

        for (int test = firstTest; test <= lastTest; test++) {
            try {
                long time = System.currentTimeMillis();
                String2.log(msg + test);
            
                if (interactive) {
                    if (test ==  0) testIslandShift();
                    if (test ==  1) testSpeed(0, 1000);  //0, 1000

                    if (test ==  3) testGenerateDatasetsXmlLong2();
                    if (test ==  4) testSpecialAxis0GlobalDouble();
                    if (test ==  5) testMinimalReadSource();
                    if (test ==  6) testMakeCopyFileTasks(true); 
                    if (test ==  7) testMakeCopyFileTasks(false);
                    if (test ==  8) testGenerateDatasetsXmlCopy(); //requires localhost erdMWchla1day
                    if (test ==  9) testLogAxis(-1); //-1=all
                    if (test == 10) testCopyFiles(true);  //deleteDataFiles  //does require localhost erddap
                    if (test == 11) testCopyFiles(false); //uses cachePartialPathRegex  //doesn't require localhost erddap

                    //not usually run
                    //if (test == 1000) testQuickRestart2();

                } else {
                    if (test ==  0) testNc(deleteCachedDatasetInfo);
                    if (test ==  1) testCwHdf(deleteCachedDatasetInfo);
                    if (test ==  2) testHdf();
                    if (test ==  3) testNcml();
                    if (test ==  4) testNccsv();
                    if (test ==  5) testGrib_43(deleteCachedDatasetInfo);  //42 or 43 for netcdfAll 4.2- or 4.3+
                    if (test ==  6) testGrib2_43(deleteCachedDatasetInfo); //42 or 43 for netcdfAll 4.2- or 4.3+
                    if (test ==  7) testGenerateDatasetsXml();  //also tests decompressIfNeeded
                    if (test ==  8) testGenerateDatasetsXml2();
                    if (test ==  9) testGenerateDatasetsXml3();
                    if (test == 10) testGenerateDatasetsXml4();
                    if (test == 11) testGenerateDatasetsXml5(); 
                    if (test == 12) testGenerateDatasetsXmlGroups();
                    if (test == 13) testGenerateDatasetsXmlGroups2();

                    if (test == 14) testAVDVSameSource();
                    if (test == 15) test2DVSameSource();
                    if (test == 16) testAVDVSameDestination();
                    if (test == 17) test2DVSameDestination();
                    if (test == 19) testTimePrecisionMillis();
                    if (test == 20) testSimpleTestNc();
                    if (test == 21) testSimpleTestNc2();
                    if (test == 22) testSpecialAxis0Time();
                    if (test == 23) testSpecialAxis0FileNameInt();
                    if (test == 24) testSpecialAxis0PathNameInt();
                    if (test == 26) testFileName(true);
                    if (test == 27) testFileName(false);
                    if (test == 28) testReplaceFromFileName(true);
                    if (test == 29) testReplaceFromFileName(false);
                    if (test == 31) testDapErrors();
                    if (test == 32) testFiles();  
                    if (test == 33) testUInt16File();
                    if (test == 34) testUnsignedGrid();  

                    //unfinished: if (test == 35) testRTechHdf();
                    if (test == 37) testUpdate();
                    if (test == 38) testQuickRestart();

                    if (test == 40) testCacheFiles(true);  //deleteDataFiles //does require localhost erddap 
                    if (test == 41) testCacheFiles(false);                   //does require localhost erddap

                    //tests of remote sources on-the-fly
                    //NetcdfFile.open(          "https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V4.0/monthly/sss_binned_L3_MON_SCI_V4.0_2011.nc");
                    //NetcdfDataset.openDataset("https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V4.0/monthly/sss_binned_L3_MON_SCI_V4.0_2011.nc");
                    //from command line: curl --head https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V4.0/monthly/sss_binned_L3_MON_SCI_V4.0_2011.nc
                    if (test == 42) testGenerateDatasetsXmlWithRemoteThreddsFiles();  
                    //if (test == 43) testRemoteThreddsFiles(true); //deleteCachedInfo. Don't do this. Use <cacheFromUrl> instead
                    if (test == 44) testMatchAxisNDigits();
                    if (test == 45) testIgor();
                    if (test == 46) testBadNcFile(false);  //runIncrediblySlowTest?
                    if (test == 47) testInvalidShortenedNcFile();

                    if (test == 50 && doSlowTestsToo) testBigRequestSpeed(3, ".dods", 895847390, 20); //nTimePoints (usually 3), expected bytes, expectedTimeInSeconds. Also testNThreads.
                    if (test == 51 && doSlowTestsToo) testNThreads(3);
                    if (test == 52 && doSlowTestsToo) testGenerateDatasetsXmlAwsS3();       
                    if (test == 53 && doSlowTestsToo) testAwsS3(true);  //deleteCachedInfo   //Make the tests smaller!  Is this "making the data publicly accessible"?
                    if (test == 54 && doSlowTestsToo) testAwsS3(false);  //deleteCachedInfo 

                    //NOT FINISHED
                    //none

                    //Remote Hyrax
                    //  These were tests of reading remote data files on Hyrax without downloading,
                    //  but test Hyrax server doesn't support Byte Ranges.
                    //if (test == 1001) testGenerateDatasetsXmlWithRemoteHyraxFiles();  //Test server doesn't support Byte Ranges.
                    //if (test == 1002) testRemoteHyraxFiles(false); //deleteCachedInfo //Test server doesn't support Byte Ranges.

                    //NOT FINISHED. test file for groups also has no named dimensions.  
                    //if (test == 1003) testGenerateDatasetsXmlGroups();
                    //if (test == 1004) testGroups();

                }

                String2.log(msg + test + " finished successfully in " + (System.currentTimeMillis() - time) + " ms.");
            } catch (Throwable testThrowable) {
                String eMsg = msg + test + " caught throwable:\n" + 
                    MustBe.throwableToString(testThrowable);
                errorSB.append(eMsg);
                String2.log(eMsg);
                if (interactive) 
                    String2.pressEnterToContinue("");
            }
        }
    }

}

