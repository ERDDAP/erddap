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
import gov.noaa.pfel.coastwatch.sgt.SgtUtil;
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
import ucar.nc2.dods.*;
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
        int tReloadEveryNMinutes, Attributes externalAddGlobalAttributes) throws Throwable {

        return generateDatasetsXml("EDDGridFromNcFiles",
            tFileDir, tFileNameRegex, sampleFileName, 
            tReloadEveryNMinutes, externalAddGlobalAttributes);
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
        boolean tAccessibleViaFiles) throws Throwable {

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
            tAccessibleViaFiles);
    }


    /** This tests generateDatasetsXml. 
     * @throws Throwable if touble
     */
    public static void testGenerateDatasetsXml() throws Throwable {

        String2.log("\n*** EDDGridFromNcFiles.testGenerateDatasetsXml");

        String results = generateDatasetsXml(
            EDStatic.unitTestDataDir + "erdQSwind1day/", 
            ".*_03\\.nc", 
            EDStatic.unitTestDataDir + "erdQSwind1day/erdQSwind1day_20080101_03.nc",
            DEFAULT_RELOAD_EVERY_N_MINUTES, null) + "\n";
        String suggDatasetID = suggestDatasetID(
            EDStatic.unitTestDataDir + "erdQSwind1day/.*_03\\.nc");

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDGridFromNcFiles",
            EDStatic.unitTestDataDir + "erdQSwind1day/", ".*_03\\.nc", 
            EDStatic.unitTestDataDir + "erdQSwind1day/erdQSwind1day_20080101_03.nc",
            "" + DEFAULT_RELOAD_EVERY_N_MINUTES},
            false); //doIt loop?
        Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt. " + 
            gdxResults.length() + " " + results.length());

        String expected = 
directionsForGenerateDatasetsXml() +
"!!! The source for erdQSwind1day_5ac2_f1cc_0bfc has nGridVariables=7,\n" +
"but this dataset will only serve 3 because the others use different dimensions.\n" +
"-->\n" +
"\n" +
"<dataset type=\"EDDGridFromNcFiles\" datasetID=\"" + suggDatasetID + "\" active=\"true\">\n" +
"    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>" + EDStatic.unitTestDataDir + "erdQSwind1day/</fileDir>\n" +
"    <fileNameRegex>.*_03\\.nc</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <matchAxisNDigits>20</matchAxisNDigits>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <accessibleViaFiles>false</accessibleViaFiles>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"acknowledgement\">NOAA NESDIS COASTWATCH, NOAA SWFSC ERD</att>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"composite\">true</att>\n" +
"        <att name=\"contributor_name\">Remote Sensing Systems, Inc</att>\n" +
"        <att name=\"contributor_role\">Source of level 2 data.</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.0, Unidata Dataset Discovery v1.0</att>\n" +
"        <att name=\"creator_email\">dave.foley@noaa.gov</att>\n" +
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
             //still numeric ip because file was generated long ago
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
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v29</att>\n" +
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
            DEFAULT_RELOAD_EVERY_N_MINUTES, null);
        String suggDatasetID = suggestDatasetID(
            String2.unitTestBigDataDir + "geosgrib/.*");

        String expected = //as of 2012-02-20. Will change if John Caron fixes bugs I reported.
directionsForGenerateDatasetsXml() +
"!!! The source for " + suggDatasetID + " has nGridVariables=17,\n" +
"but this dataset will only serve 3 because the others use different dimensions.\n" +
"-->\n" +
"\n" +
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
"    <accessibleViaFiles>false</accessibleViaFiles>\n" +
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
"        <att name=\"creator_url\">http://www.ncep.noaa.gov/</att>\n" + 
"        <att name=\"infoUrl\">http://www.ncep.noaa.gov/</att>\n" +
"        <att name=\"institution\">NCEP</att>\n" +
"        <att name=\"keywords\">centers, data, direction, Direction_of_swell_waves_ordered_sequence_of_data, earth, Earth Science &gt; Oceans &gt; Ocean Waves &gt; Significant Wave Height, Earth Science &gt; Oceans &gt; Ocean Waves &gt; Swells, Earth Science &gt; Oceans &gt; Ocean Waves &gt; Wave Period, environmental, height, local, mean, Mean_period_of_swell_waves_ordered_sequence_of_data, national, ncep, ocean, oceans, ordered, ordered_sequence_of_data, period, prediction, science, sea, sea_surface_swell_wave_period, sea_surface_swell_wave_significant_height, sea_surface_swell_wave_to_direction, sequence, significant, Significant_height_of_swell_waves_ordered_sequence_of_data, source, surface, surface waves, swell, swells, time, wave, waves</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v29</att>\n" +
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
"            <att name=\"ioos_category\">Surface Waves</att>\n" +
"            <att name=\"standard_name\">sea_surface_swell_wave_significant_height</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //ensure it is ready-to-use by making a dataset from it
        //EDD edd = oneFromXmlFragment(null, results);
        //Test.ensureEqual(edd.datasetID(), "erdQSwind1day_52db_1ed3_22ce", "");
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
            12000, null) + "\n";
        String suggDatasetID = suggestDatasetID(sDir + sRegex);

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDGridFromNcFiles", sDir, sRegex, "", "" + 12000},
            false); //doIt loop?
        Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt. " + 
            gdxResults.length() + " " + results.length());

        String expected = 
//wind_speed is important additional test where scale_factor=1 and add_offset=0
directionsForGenerateDatasetsXml() +
"!!! The source for 1day_1fe9_4979_3661 has nGridVariables=14,\n" +
"but this dataset will only serve 11 because the others use different dimensions.\n" +
"-->\n" +
"\n" +
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
"    <accessibleViaFiles>false</accessibleViaFiles>\n" +
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
"        <att name=\"date_created\">2013-04-26T02:56:04Z</att>\n" +
"        <att name=\"easternmost_longitude\">null</att>\n" +
"        <att name=\"file_quality_level\">null</att>\n" +
"        <att name=\"infoUrl\">http://pathfinder.nodc.noaa.gov/ISO-AVHRR_Pathfinder-NODC-L3C-v5.2.html</att>\n" +
"        <att name=\"institution\">NOAA NODC</att>\n" +
"        <att name=\"keywords\">10m, advanced, aerosol, aerosol_dynamic_indicator, analysis, area, atmosphere, atmospheric, avhrr, bias, center, climate, collated, cryosphere, data, deviation, difference, distribution, dt_analysis, dynamic, earth, Earth Science &gt; Atmosphere &gt; Atmospheric Winds &gt; Surface Winds, Earth Science &gt; Cryosphere &gt; Sea Ice &gt; Ice Extent, Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature, Earth Science &gt; Oceans &gt; Sea Ice &gt; Ice Extent, error, estimate, extent, flag, flags, fraction, ghrsst, global, high, high-resolution, ice, ice distribution, indicator, l2p, l2p_flags, l3-collated, l3c, measurement, national, ncei, nesdis, noaa, nodc, ocean, oceanographic, oceans, optical, optical properties, pathfinder, pathfinder_quality_level, properties, quality, quality_level, radiometer, record, reference, resolution, science, sea, sea_ice_area_fraction, sea_ice_fraction, sea_surface_skin_temperature, sea_surface_temperature, sensor, single, skin, speed, sses, sses_bias, sses_standard_deviation, sst, sst_dtime, standard, statistics, surface, temperature, time, version, very, vhrr, wind, wind_speed, winds</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"Metadata_Conventions\">null</att>\n" +
"        <att name=\"netcdf_version_id\">null</att>\n" +
"        <att name=\"northernmost_latitude\">null</att>\n" +
"        <att name=\"principal_year_day_for_collated_orbits\">null</att>\n" +
"        <att name=\"publisher_type\">group</att>\n" +
"        <att name=\"publisher_url\">https://www.ghrsst.org</att>\n" +
"        <att name=\"southernmost_latitude\">null</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v29</att>\n" +
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
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Statistics</att>\n" +
"            <att name=\"reference\">null</att>\n" +
"            <att name=\"references\">AVHRR_OI, with inland values populated from AVHRR_Pathfinder daily climatological SST. For more information on this reference field see https://data.nodc.noaa.gov/cgi-bin/iso?id=gov.noaa.nodc:0071180.</att>\n" +
"            <att name=\"units\">degree_C</att>\n" +
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
"            <att name=\"ioos_category\">Ice Distribution</att>\n" +
"            <att name=\"reference\">null</att>\n" +
"            <att name=\"references\">Reynolds, et al.(2006) Daily High-resolution Blended Analyses. Available at ftp://eclipse.ncdc.noaa.gov/pub/OI-daily/daily-sst.pdf</att>\n" +
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
"            <att name=\"colorBarMaximum\" type=\"double\">300.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
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
        try {

        //takes a long time and no longer useful
        //String2.pressEnterToContinue(
        //    "\nCopy the latest file from coastwatch\n" +
        //    "https://coastwatch.pfeg.noaa.gov/erddap/files/jplMURSST41/ \n" + 
        //    "to /u00/satellite/MUR41/ssta/1day on this computer.");

        String results = generateDatasetsXml(
            "/u00/satellite/MUR41/ssta/1day/", "2.*\\.nc", "", 
            -1, null) + "\n";

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDGridFromNcFiles", 
            "/u00/satellite/MUR41/ssta/1day/", "2.*\\.nc", "",
            "-1"},
            false); //doIt loop?
        Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt. " + 
            gdxResults.length() + " " + results.length());

        String expected = 
directionsForGenerateDatasetsXml() +
"!!! The source for 1day_328e_9133_37c0 has nGridVariables=8,\n" +
"but this dataset will only serve 5 because the others use different dimensions.\n" +
"-->\n" +
"\n" +
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
"    <accessibleViaFiles>false</accessibleViaFiles>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"acknowledgment\">Please acknowledge the use of these data with the following statement:  These data were provided by JPL under support by NASA MEaSUREs program.</att>\n" +
"        <att name=\"cdm_data_type\">grid</att>\n" +
"        <att name=\"comment\">Interim-MUR(nrt) will be replaced by MUR-Final in about 3 days; MUR = &quot;Multi-scale Ultra-high Reolution&quot;</att>\n" +
"        <att name=\"Conventions\">CF-1.5</att>\n" +
"        <att name=\"creator_email\">ghrsst@podaac.jpl.nasa.gov</att>\n" +
"        <att name=\"creator_name\">JPL MUR SST project</att>\n" +
"        <att name=\"creator_url\">http://mur.jpl.nasa.gov</att>\n" +
"        <att name=\"date_created\">20180124T021359Z</att>\n" +
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
"        <att name=\"platform\">Terra, Aqua, GCOM-W, NOAA-19, MetOp-A</att>\n" +
"        <att name=\"processing_level\">L4</att>\n" +
"        <att name=\"product_version\">04.1nrt</att>\n" +
"        <att name=\"project\">NASA Making Earth Science Data Records for Use in Research Environments (MEaSUREs) Program</att>\n" +
"        <att name=\"publisher_email\">ghrsst-po@nceo.ac.uk</att>\n" +
"        <att name=\"publisher_name\">GHRSST Project Office</att>\n" +
"        <att name=\"publisher_url\">http://www.ghrsst.org</att>\n" +
"        <att name=\"references\">http://podaac.jpl.nasa.gov/Multi-scale_Ultra-high_Resolution_MUR-SST</att>\n" +
"        <att name=\"sensor\">MODIS, AMSR2, AVHRR</att>\n" +
"        <att name=\"source\">MODIS_T-JPL, MODIS_A-JPL, AMSR2-REMSS, AVHRR19_G-NAVO, AVHRRMTA_G-NAVO, Ice_Conc-OSISAF</att>\n" +
"        <att name=\"southernmost_latitude\" type=\"float\">-90.0</att>\n" +
"        <att name=\"spatial_resolution\">0.01 degrees</att>\n" +
"        <att name=\"standard_name_vocabulary\">NetCDF Climate and Forecast (CF) Metadata Convention</att>\n" +
"        <att name=\"start_time\">20180122T090000Z</att>\n" +
"        <att name=\"stop_time\">20180122T090000Z</att>\n" +
"        <att name=\"summary\">A merged, multi-sensor L4 Foundation SST analysis product from JPL.</att>\n" +
"        <att name=\"time_coverage_end\">20180122T210000Z</att>\n" +
"        <att name=\"time_coverage_start\">20180121T210000Z</att>\n" +
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
"        <att name=\"creator_url\">https://mur.jpl.nasa.gov</att>\n" +
"        <att name=\"date_created\">2018-01-24T02:13:59Z</att>\n" +
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
"        <att name=\"references\">https://podaac.jpl.nasa.gov/Multi-scale_Ultra-high_Resolution_MUR-SST</att>\n" +
"        <att name=\"southernmost_latitude\">null</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v29</att>\n" +
"        <att name=\"start_time\">null</att>\n" +
"        <att name=\"stop_time\">null</att>\n" +
"        <att name=\"summary\">A merged, multi-sensor L4 Foundation Sea Surface Temperature (SST) analysis product from Jet Propulsion Laboratory (JPL).</att>\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, 
            "results.length=" + results.length() + " expected.length=" + expected.length() + 
            "\nresults=\n" + results);

        try {
            expected = 
        "<att name=\"testOutOfDate\">now-5days</att>\n" +
"        <att name=\"time_coverage_end\">2018-01-22T21:00:00Z</att>\n" +
"        <att name=\"time_coverage_start\">2018-01-21T21:00:00Z</att>\n";
            int po3 = results.indexOf(expected.substring(0, 25));
            Test.ensureEqual(results.substring(po3, po3 + expected.length()), expected,
                "results=\n" + results);
        } catch (Throwable t3) {
            String2.pressEnterToContinue(MustBe.throwableToString(t3) + 
                "\nThe dates will vary depending on when a jplMURSST41 file was last download to this computer."); 
        }


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
"            <att name=\"source\">MODIS_T-JPL, MODIS_A-JPL, AMSR2-REMSS, AVHRR19_G-NAVO, AVHRRMTA_G-NAVO, Ice_Conc-OSISAF</att>\n" +
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
        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t)); 
        }

    }


    /** This tests generateDatasetsXml with an AWS S3 dataset. 
     * @throws Throwable if touble
     */
    public static void testGenerateDatasetsXmlAwsS3() throws Throwable {

        String2.log("\n*** EDDGridFromNcFiles.testGenerateDatasetsXmlAwsS3");
        if ("s".equals(String2.getStringFromSystemIn(
            "This test is very slow. Continue (y) or skip this (s)?")))
            return;

        try {

        String dir = "http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS"; //intentionally left off trailing /
        String regex = ".*_CESM1-CAM5_.*\\.nc";
        String name = dir + "/tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_CESM1-CAM5_200601-201012.nc";
        int reload = 1000000;

        String results = generateDatasetsXml(dir, regex, name, reload, null) + "\n";
        String suggDatasetID = suggestDatasetID(dir + "/" + regex);

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDGridFromNcFiles", dir, regex, name, "" + reload},
            false); //doIt loop?
        Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt. " + 
            gdxResults.length() + " " + results.length());

        String expected = 
directionsForGenerateDatasetsXml() +
"!!! The source for s3nasanex_5c8a_8589_b680 has nGridVariables=4,\n" +
"but this dataset will only serve 1 because the others use different dimensions.\n" +
"-->\n" +
"\n" +
"<dataset type=\"EDDGridFromNcFiles\" datasetID=\"s3nasanex_5c8a_8589_b680\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1000000</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>0</updateEveryNMillis>\n" +
"    <fileDir>http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/</fileDir>\n" +
"    <fileNameRegex>.*_CESM1-CAM5_.*\\.nc</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <matchAxisNDigits>20</matchAxisNDigits>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <accessibleViaFiles>false</accessibleViaFiles>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"CMIPtable\">Amon</att>\n" +
"        <att name=\"contact\">Dr. Rama Nemani: rama.nemani@nasa.gov, Dr. Bridget Thrasher: bridget@climateanalyticsgroup.org, and Dr. Mark Snyder: mark@climateanalyticsgroup.org</att>\n" +
"        <att name=\"Conventions\">CF-1.4</att>\n" +
"        <att name=\"creation_date\">Wed Sep 12 14:44:16 PDT 2012</att>\n" +
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
"        <att name=\"tracking_id\">d7ed8c4a-af11-11e2-9608-e41f13ef9be2</att>\n" +
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
"        <att name=\"driving_data_tracking_ids\">null</att>\n" +
"        <att name=\"infoUrl\">http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/</att>\n" +
"        <att name=\"keywords\">800m, air, air_temperature, ames, atmosphere,\n" +
"Atmosphere &gt; Atmospheric Temperature &gt; Air Temperature,\n" +
"Atmosphere &gt; Atmospheric Temperature &gt; Surface Air Temperature,\n" +
"atmospheric, center, climate, cmip5, continental, daily, data, day, downscaled, earth, exchange, field, intercomparison, minimum, model, moffett, nasa, near, near-surface, nex, project, projections, research, surface, tasmin, temperature, time, US</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v29</att>\n" +
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
        EDD edd = oneFromXmlFragment(null, results);
        Test.ensureEqual(edd.datasetID(), suggDatasetID, "");
        Test.ensureEqual(edd.title(), "800m Downscaled NEX CMIP5 Climate Projections for the Continental US", "");
        Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
            "tasmin", "");

        String2.log("\nEDDGridFromNcFiles.testGenerateDatasetsXmlAwsS3 passed the test.");

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nUnexpected error: Have you updated your AWS credentials lately?\n"); 
        }
    }

    /**
     * This tests reading NetCDF .nc files with this class.
     *
     * @throws Throwable if trouble
     */
    public static void testAwsS3(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDGridFromNcFiles.testAwsS3() *****************\n");
        if ("s".equals(String2.getStringFromSystemIn(
            "This test is very slow. Continue (y) or skip this (s)?")))
            return;

        testVerboseOn();
        try {

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
        expected = 
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
"    Float32 _FillValue 1.0E20;\n" +
"    String associated_files \"baseURL: http://cmip-pcmdi.llnl.gov/CMIP5/dataLocation gridspecFile: gridspec_atmos_fx_CESM1-CAM5_rcp26_r0i0p0.nc areacella: areacella_fx_CESM1-CAM5_rcp26_r0i0p0.nc\";\n" +
"    String cell_measures \"area: areacella\";\n" +
"    String cell_methods \"time: minimum (interval: 30 days) within days time: mean over days\";\n" +
"    Float64 colorBarMaximum 313.0;\n" +
"    Float64 colorBarMinimum 263.0;\n" +
"    String comment \"TREFMNAV no change, CMIP5_table_comment: monthly mean of the daily-minimum near-surface air temperature.\";\n" +
"    String history \"2012-06-09T00:36:32Z altered by CMOR: Treated scalar dimension: 'height'. 2012-06-09T00:36:32Z altered by CMOR: Reordered dimensions, original order: lat lon time. 2012-06-09T00:36:32Z altered by CMOR: replaced missing value flag (-1e+32) with standard missing value (1e+20).\";\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Daily Minimum Near-Surface Air Temperature\";\n" +
"    Float32 missing_value 1.0E20;\n" +
"    String original_name \"TREFMNAV\";\n" +
"    String standard_name \"air_temperature\";\n" +
"    String units \"K\";\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String CMIPtable \"Amon\";\n" +
"    String contact \"Dr. Rama Nemani: rama.nemani@nasa.gov, Dr. Bridget Thrasher: bridget@climateanalyticsgroup.org, and Dr. Mark Snyder: mark@climateanalyticsgroup.org\";\n" +
"    String Conventions \"CF-1.6, COARDS, ACDD-1.3\";\n" +
"    String creation_date \"Wed Sep 12 14:44:43 PDT 2012\";\n" +
"    String creator_email \"rama.nemani@nasa.gov\";\n" +
"    String creator_name \"Rama Nemani\";\n" +
"    String creator_url \"https://www.nasa.gov/\";\n" +
"    String DOI \"http://dx.doi.org/10.7292/W0WD3XH4\";\n" +
"    String downscalingModel \"BCSD\";\n" +
"    String driving_data_tracking_ids \"N/A\";\n" +
"    String driving_experiment \"historical\";\n" +
"    String driving_experiment_name \"historical\";\n" +
"    String driving_model_ensemble_member \"r3i1p1\";\n" +
"    String driving_model_id \"CESM1-CAM5\";\n" +
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
    "String infoUrl \"http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/\";\n" +
"    String initialization_method \"1\";\n" +
"    String institute_id \"NASA-Ames\";\n" +
"    String institution \"NASA Earth Exchange, NASA Ames Research Center, Moffett Field, CA 94035\";\n" +
"    String keywords \"800m, air, air_temperature, ames, atmosphere,\n" +
"Atmosphere > Atmospheric Temperature > Air Temperature,\n" +
"Atmosphere > Atmospheric Temperature > Surface Air Temperature,\n" +
"atmospheric, center, climate, cmip5, continental, daily, data, day, downscaled, earth, exchange, field, intercomparison, minimum, model, moffett, nasa, near, near-surface, nex, project, projections, research, surface, tasmin, temperature, time, US\";\n" +
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
"    String region_lexicon \"http://en.wikipedia.org/wiki/Contiguous_United_States\";\n" +
"    String resolution_id \"800m\";\n" +
"    String sourceUrl \"(remote files)\";\n" +
"    Float64 Southernmost_Northing 24.0625;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
"    String summary \"800m Downscaled NEX Climate Model Intercomparison Project 5 (CMIP5) Climate Projections for the Continental US\";\n" +
"    String table_id \"Table Amon\";\n" +
"    String time_coverage_end \"2099-12-16T12:00:00Z\";\n" +
"    String time_coverage_start \"2006-01-16T12:00:00Z\";\n" +
"    String title \"800m Downscaled NEX CMIP5 Climate Projections for the Continental US\";\n" +
"    String tracking_id \"d62220e2-af11-11e2-bdc9-e41f13ef5cee\";\n" +
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
"2099-12-16T12:00:00Z,42.50416665929,262.50416665566,261.5762\n";         
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //img
        tName = eddGrid.makeNewFileForDapQuery(null, null, 
            "tasmin[(2099-12-16T12:00:00Z)][(24.0625):(49.92916665632)][(234.97916666666998):" +
            "(293.51249997659)]&.draw=surface&.vars=longitude|latitude|tasmin" +
            "&.colorBar=|||||&.land=under",
            tDir, "testAwsS3", ".png"); 
        SSR.displayInBrowser("file://" + tDir + tName);

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nUnexpected error: Have you updated your AWS credentials lately?\n"); 
        }

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
"*GLOBAL*,Conventions,\"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.0\"\n" +
"*GLOBAL*,acknowledgement,\"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\"\n" +
"*GLOBAL*,cdm_data_type,Grid\n" +
"*GLOBAL*,composite,true\n" +
"*GLOBAL*,contributor_name,\"Remote Sensing Systems, Inc\"\n" +
"*GLOBAL*,contributor_role,Source of level 2 data.\n" +
"*GLOBAL*,creator_email,dave.foley@noaa.gov\n" +
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
"*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v29\n" +
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
"*GLOBAL*,Conventions,\"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.0\"\n" +
"*GLOBAL*,acknowledgement,\"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\"\n" +
"*GLOBAL*,cdm_data_type,Grid\n" +
"*GLOBAL*,composite,true\n" +
"*GLOBAL*,contributor_name,\"Remote Sensing Systems, Inc\"\n" +
"*GLOBAL*,contributor_role,Source of level 2 data.\n" +
"*GLOBAL*,creator_email,dave.foley@noaa.gov\n" +
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
"*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v29\n" +
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
"*GLOBAL*,Conventions,\"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.0\"\n" +
"*GLOBAL*,acknowledgement,\"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\"\n" +
"*GLOBAL*,cdm_data_type,Grid\n" +
"*GLOBAL*,composite,true\n" +
"*GLOBAL*,contributor_name,\"Remote Sensing Systems, Inc\"\n" +
"*GLOBAL*,contributor_role,Source of level 2 data.\n" +
"*GLOBAL*,creator_email,dave.foley@noaa.gov\n" +
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
"*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v29\n" +
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
"    String creator_email \"dave.foley@noaa.gov\";\n" +
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
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
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
//http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.csv?y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]
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
//http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.csvp?y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]
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
//http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.csv0?y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv  with data from several files
        String2.log("\n*** .nc test read from several files\n");       
        userDapQuery = "y_wind[(1.1991888e9):3:(1.1999664e9)][0][(36.5)][(230)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_Data1", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
//verified with 
//http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.csv?y_wind[(1.1991888e9):3:(1.1999664e9)][0][(36.5)][(230)]
"time,altitude,latitude,longitude,y_wind\n" +
"UTC,m,degrees_north,degrees_east,m s-1\n" +
"2008-01-01T12:00:00Z,0.0,36.625,230.125,7.6282454\n" +
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
//http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.tsv?y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]
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
//http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.csvp?y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]
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
//http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.csv0?y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]
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
        try {

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
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
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
        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t)); 
        }
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

        String2.log(NcHelper.dumpString(String2.unitTestBigDataDir + 
            "geosgrib/multi_1.glo_30m.all.grb2", false));

        //generateDatasetsXml
        try {   
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
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
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
        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t)); 
        }
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
        try {

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
try {
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
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
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
        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t)); 
        }
        
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
        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t)); 
        }
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
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
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
"http://cfconventions.org/Data/cf-conventions/cf-conventions-1.7/cf-conventions.html#_naming_conventions\n" +
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
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
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
        boolean oReallyVerbose = reallyVerbose;
        reallyVerbose = false;
        String outName;
        //2017-10-13 I switched from getFile to curl
        //  The advantage is: curl will detect if outputstream isn't being closed.
        String baseRequest = "curl http://localhost:8080/cwexperimental/griddap/testGriddedNcFiles"; 
        String userDapQuery = "?" + SSR.minimalPercentEncode("y_wind[(1.1999664e9)][0][][0:719]" + //719 avoids esriAsc cross lon=180
            "&.vec="); //avoid get cached response
        String baseOut = EDStatic.fullTestCacheDirectory + "EDDGridFromNcFilesTestSpeed";
        ArrayList al;
        int timeOutSeconds = 120;
        String extensions[] = new String[]{
            ".asc", ".csv", ".csvp", ".csv0", 
            ".das", ".dds", ".dods", ".esriAscii", 
            ".graph", ".html", ".htmlTable",   //.help not available at this level
            ".json", ".jsonlCSV", ".jsonlKVP", ".mat", 
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
            //now Java 1.8,1.7/M4700      //was Java 1.6 times            //was java 1.5 times
            387, 1105, 1011, 1000,        //734, 6391, 6312, ?            //1250, 9750, 9562, ?                                  
            215, 215, 290, 8312,          //15, 15, 109/156, 16875            //15, 15, 547, 18859
            263, 247, 603,                //63, 47, 2032,                 //93, 31, ...,
            1121, 1268, 1465, 325,        //6422, ., ., 203,              //9621, ., ., 625,  
            341, 341,                     //234, 250,                     //500, 500, 
            1206, 215, 390,
            1448, 202,                    //9547, ?                       //13278, ?
            1011, 1011, 1011,             //6297, 6281, ?,                //8766, 8844, ?,
            2000,  //but really slow if hard drive is busy!   //8625,     //11469, 
            700, 210,                     //656, 110,         //687, 94,  //Java 1.7 was 390r until change to new netcdf-Java
            444, 976, 1178,               //860, 2859, 3438,              //2188, 4063, 3797,   //small varies greatly
            280, 348, 532,                //438, 468, 1063,               //438, 469, 1188,     //small varies greatly
            788};                         //1703                          //2359};
        int bytes[]    = new int[]   {
            5875592, 23734053, 23734063, 23733974, 
            6006, 303, 2085486, 4701074, 
            60787, 51428, 11980027, 
            31827797, 28198736, 54118736, 2085800, 
            2090600, 5285, 
            25961828, 5244, 5877820,
            24337084, 58,
            23734053, 23734063, 23733974, 
            69372795, 
            523113, 3601, 
            478774, 2189656, 2904880, 
            30852, 76777, 277494, 
            335307};

        //warm up
        outName = baseOut + "Warmup.csvp.csv";
        al = SSR.dosShell(baseRequest + ".csvp" + userDapQuery + Math2.random(1000) + 
            " -o " + outName, timeOutSeconds);
        //String2.log(String2.toNewlineString(al.toArray()));

        outName = baseOut + "Warmup.png.png";
        al = SSR.dosShell(baseRequest + ".png" + userDapQuery + Math2.random(1000) + 
            " -o " + outName, timeOutSeconds);

        outName = baseOut + "Warmup.pdf.pdf";
        al = SSR.dosShell(baseRequest + ".pdf" + userDapQuery + Math2.random(1000) + 
            " -o " + outName, timeOutSeconds);
       
        lastTest = Math.min(lastTest, extensions.length - 1);
        for (int ext = firstTest; ext <= lastTest; ext++) {
            String dotExt = extensions[ext];
            try {
                String2.log("\n*** EDDGridFromNcFiles.testSpeed test#" + ext + ": " + 
                    dotExt + " speed\n");
                long time = 0, cLength = 0;
                for (int chance = 0; chance < 3; chance++) {
                    Math2.gcAndWait(); //in a test
                    time = System.currentTimeMillis();
                    outName = baseOut + chance + dotExt;
                    al = SSR.dosShell(baseRequest + dotExt + userDapQuery + Math2.random(1000) + 
                        " -o " + outName, timeOutSeconds);

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
                if (time < (expectedMs[ext] <= 50? 0.1 : 0.5) * expectedMs[ext])
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
                    "\nUnexpected ERROR for Test#" + ext + ": " + dotExt + "."); 
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
            NcHelper.dumpString(
            "/u00/data/viirs/MappedMonthly4km/V20120012012031.L3m_MO_NPP_CHL_chlor_a_4km", 
            false) + "\n");

        String results = Projects.dumpTimeLatLon("/u00/data/viirs/MappedMonthly4km/m4.ncml");
        String expected = 
"ncmlName=/u00/data/viirs/MappedMonthly4km/m4.ncml\n" +
"latitude [0]=89.97916666666667 [1]=89.9375 [4319]=-89.97916666666664\n" +
"longitude [0]=-179.97916666666666 [1]=-179.9375 [8639]=179.97916666666666\n" +
"time [0]=15340 [1]=15371 [1]=15371\n";
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
        results = NcHelper.dumpString(tDir + tName, true);
        expected = 
":time_coverage_end = \"1984-02-01T12:00:59.401Z\";\n" + 
"  :time_coverage_start = \"1984-02-01T12:00:59.001Z\";\n" +  
"  :title = \"L1b Magnetometer (MAG) Geomagnetic Field Product\";\n" +
" data:\n" +
"time =\n" +
"  {4.44484859001E8, 4.44484859101E8, 4.44484859201E8, 4.44484859301E8, 4.4448485940099996E8}\n" +
"ECEF_X =\n" +
"  {9.96921E36, 9.96921E36, 9.96921E36, 9.96921E36, 9.96921E36}\n" +
"IB_time =\n" +
"  {7.60017659E8, 7.600176591E8, 7.600176592E8, 7.600176593E8, 7.600176594E8}\n" +  
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

        String2.log(NcHelper.dumpString(EDStatic.unitTestDataDir + "simpleTest.nc", true));

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
        results = NcHelper.dumpString(tDir + tName, true);
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
"      :ioos_category = \"Unknown\";\n" +
"\n" +
"    short shorts(days=2);\n" +
"      :ioos_category = \"Unknown\";\n" +
"\n" +
"    int ints(days=2);\n" +
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
"  :standard_name_vocabulary = \"CF Standard Name Table v29\";\n" +
"  :summary = \"My summary.\";\n" +
"  :title = \"My Title\";\n" +
" data:\n" +
"days =\n" +
"  {172800.0, 259200.0}\n" +
"hours =\n" +
"  {3.155544E8, 3.15558E8}\n" +
"minutes =\n" +
"  {6.311526E8, 6.3115266E8}\n" +
"seconds =\n" +
"  {9.46684821E8, 9.46684822E8}\n" +
"millis =\n" +
"  {1.262304000031E9, 1.262304000032E9}\n" +
"bytes =\n" +
"  {41, 42}\n" +
"shorts =\n" +
"  {10001, 10002}\n" +
"ints =\n" +
"  {1000001, 1000002}\n" +
"floats =\n" +
"  {1.1, 2.2}\n" +
"doubles =\n" +
"  {1.0000000000001E12, 1.0000000000002E12}\n" +
"Strings =\"10\", \"20\"\n" +
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
"  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
"<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
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
        String2.log("\n!!!! KNOWN PROBLEM: SgtGraph DOESN'T SUPPORT TWO TIME AXES. !!!!\n" +
            "See SgtGraph \"yIsTimeAxis = false;\".\n");
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

        String2.log(NcHelper.dumpString(EDStatic.unitTestDataDir + "simpleTest.nc", true));

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
        results = NcHelper.dumpString(tDir + tName, true);
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
"  :standard_name_vocabulary = \"CF Standard Name Table v29\";\n" +
"  :summary = \"My summary.\";\n" +
"  :title = \"My Title\";\n" +
" data:\n" +
"days =\n" +
"  {259200.0, 345600.0}\n" +
"bytes =\n" +
"  {42, 43}\n" +
"doubles =\n" +
"  {1.0000000000002E12, 1.0000000000003E12}\n" +
"Strings =\"20\", \"30\"\n" +
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
"  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
"<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
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
        String2.log(NcHelper.dumpString(dir + fileName, false));

        String results = generateDatasetsXml(dir, regex, dir + fileName,
            DEFAULT_RELOAD_EVERY_N_MINUTES, null) + "\n";
        String expected = 
directionsForGenerateDatasetsXml() +
"-->\n" +
"\n" +
"zztop\n\n";
        Test.ensureEqual(results, expected, 
            "results.length=" + results.length() + " expected.length=" + expected.length() + 
            "\nresults=\n" + results);

        //ensure it is ready-to-use by making a dataset from it
        EDD edd = oneFromXmlFragment(null, results);
        Test.ensureEqual(edd.datasetID(), "erdQSwind1day_52db_1ed3_22ce", "");
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
            File2.rename(dataDir, "erdQSwind1day_20080101_03.nc", "erdQSwind1day_20080101_03.nc2");
            for (int i = 0; i < 5; i++) {
                String2.log("after rename .nc to .nc2, update #" + i + " " + eddGrid.update());
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
            File2.rename(dataDir, "erdQSwind1day_20080101_03.nc2", "erdQSwind1day_20080101_03.nc");
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
            File2.rename(dataDir, "bad.notnc", "bad.nc");
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
        } finally {
            //rename it back to original
            String2.log("\n*** rename it back to original\n");       
            File2.rename(dataDir, "bad.nc", "bad.notnc");
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
        String2.pressEnterToContinue("time/update() = " + (cumTime/1000.0) +
            "ms (diverse results 0.001 - 11.08ms on Bob's M4700)" +
            "\nNot an error, just FYI."); 
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
"    String creator_email \"dave.foley@noaa.gov\";\n" +
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
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
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
            File2.rename(dataDir, "erdQSwind1day_20080101_03.nc", "erdQSwind1day_20080101_03.nc2");
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
                results = "shouldn't happen";
            } catch (Throwable t2) {
                results = t2.getMessage();
            }
            expected = 
"There was a (temporary?) problem.  Wait a minute, then try again.  (In a browser, click the Reload button.)\n" +
"(Cause: java.io.FileNotFoundException: \\erddapTest\\erdQSwind1day\\erdQSwind1day_20080101_03.nc (The system cannot find the file specified))";      
            Test.ensureEqual(results, expected, "\nresults=\n" + results);

        } finally {
            //rename file back to original
            String2.log("\n*** rename it back to original\n");       
            File2.rename(dataDir, "erdQSwind1day_20080101_03.nc2", "erdQSwind1day_20080101_03.nc");
            //delete badFile file so the 20080101 file is re-considered
            File2.delete(badFileMapFileName(tDatasetID));
            while (!File2.isFile(dataDir + "erdQSwind1day_20080101_03.nc")) {
                String2.log("Waiting for " + dataDir + "erdQSwind1day_20080101_03.nc2 to be named back to .nc"); 
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
            -1, null);
        //String2.log(results);
        expected = "zztop";
        Test.ensureEqual(results, expected, "results=\n" + results);

        String2.log("\n*** EDDGridFromNcFiles.testGenerateDatasetsXmlWithRemoteHyraxFiles() finished successfully\n");
   
    }

    /**
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
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
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
        String dir = "https://data.nodc.noaa.gov/thredds/catalog/aquarius/nodc_binned_V4.0/"; //catalog.html

      try {
        //dir  is a /thredds/catalog/.../  [implied catalog.html] URL!
        //file is a /thredds/fileServer/... not compressed data file.
        results = generateDatasetsXml( 
            dir, 
            "sss_binned_L3_MON_SCI_V4.0_\\d{4}\\.nc", 
            //sample file is a thredds/fileServer/.../...nc URL!
            "https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V4.0/monthly/sss_binned_L3_MON_SCI_V4.0_2011.nc", 
            -1, null);
        //String2.log(results);
String2.setClipboardString(results);

        expected = 
"<!--\n" +
" DISCLAIMER:\n" +
"   The chunk of datasets.xml made by GenerageDatasetsXml isn't perfect.\n" +
"   YOU MUST READ AND EDIT THE XML BEFORE USING IT IN A PUBLIC ERDDAP.\n" +
"   GenerateDatasetsXml relies on a lot of rules-of-thumb which aren't always\n" +
"   correct.  *YOU* ARE RESPONSIBLE FOR ENSURING THE CORRECTNESS OF THE XML\n" +
"   THAT YOU ADD TO ERDDAP'S datasets.xml FILE.\n" +
"\n" +
" DIRECTIONS:\n" +
" * Read about this type of dataset in\n" +
"   https://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html .\n" +
" * Read https://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html#addAttributes\n" +
"   so that you understand about sourceAttributes and addAttributes.\n" +
" * Note: Global sourceAttributes and variable sourceAttributes are listed\n" +
"   below as comments, for informational purposes only.\n" +
"   ERDDAP combines sourceAttributes and addAttributes (which have\n" +
"   precedence) to make the combinedAttributes that are shown to the user.\n" +
"   (And other attributes are automatically added to longitude, latitude,\n" +
"   altitude, depth, and time variables).\n" +
" * If you don't like a sourceAttribute, overwrite it by adding an\n" +
"   addAttribute with the same name but a different value\n" +
"   (or no value, if you want to remove it).\n" +
" * All of the addAttributes are computer-generated suggestions. Edit them!\n" +
"   If you don't like an addAttribute, change it.\n" +
" * If you want to add other addAttributes, add them.\n" +
" * If you want to change a destinationName, change it.\n" +
"   But don't change sourceNames.\n" +
" * You can change the order of the dataVariables or remove any of them.\n" +
"-->\n" +
"\n" +
"<dataset type=\"EDDGridFromNcFiles\" datasetID=\"noaa_nodc_0ba9_b245_c4c4\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>0</updateEveryNMillis>\n" +
"    <fileDir>https://data.nodc.noaa.gov/thredds/catalog/aquarius/nodc_binned_V4.0/</fileDir>\n" +
"    <fileNameRegex>sss_binned_L3_MON_SCI_V4.0_\\d{4}\\.nc</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <matchAxisNDigits>20</matchAxisNDigits>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <accessibleViaFiles>false</accessibleViaFiles>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"Conventions\">CF-1.6</att>\n" +
"        <att name=\"creator_email\">Yongsheng.Zhang@noaa.gov</att>\n" +
"        <att name=\"creator_institution\">US National Oceanographic Data Center</att>\n" +
"        <att name=\"creator_name\">Yongsheng Zhang, Ph.D.</att>\n" +
"        <att name=\"creator_url\">https://www.nodc.noaa.gov/SatelliteData</att>\n" +
"        <att name=\"date_created\">20140716T073150Z</att>\n" +
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
"        <att name=\"institution\">JPL,  California Institute of Technology</att>\n" +
"        <att name=\"keywords\">Earth Science &gt;Oceans &gt; Surface Salinity</att>\n" +
"        <att name=\"keywords_vocabulary\">NASA Global Change Master Directory (GCMD) Science Keywords</att>\n" +
"        <att name=\"license\">These data are available for use without restriction.</att>\n" +
"        <att name=\"Metadata_Conventions\">Unidata Dataset Discovery v1.0</att>\n" +
"        <att name=\"mission\">SAC-D Aquarius</att>\n" +
"        <att name=\"necdf_version_id\">3.6.3</att>\n" +
"        <att name=\"nodc_template_version\">NODC_NetCDF_Grid_Template_v1.0</att>\n" +
"        <att name=\"publisher_name\">NOAA/NESDIS/NODC - US National Oceanographic Data Center</att>\n" +
"        <att name=\"references\">Aquarius users guide, V6.0, PO.DAAC, JPL/NASA. Jun 2, 2014</att>\n" +
"        <att name=\"sensor\">Aquarius</att>\n" +
"        <att name=\"source\">Jet Propulsion Laboratory, California Institute of Technology</att>\n" +
"        <att name=\"summary\">This dataset is created by NODC Satellite Oceanography Group from Aquarius level-2 SCI V4.0 data,using 1.0x1.0 (lon/lat) degree box average</att>\n" +
"        <att name=\"title\">Gridded monthly mean Sea Surface Salinity calculated from Aquarius level-2 SCI V4.0 data</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"Conventions\">CF-1.6, COARDS, ACDD-1.3</att>\n" +
"        <att name=\"infoUrl\">https://data.nodc.noaa.gov/thredds/catalog/aquarius/nodc_binned_V4.0/catalog.html</att>\n" +
"        <att name=\"institution\">JPL, California Institute of Technology</att>\n" +
"        <att name=\"keywords\">aquarius, calculate, calculated, california, center, data, earth,\n" +
"Earth Science &gt;Oceans &gt; Surface Salinity,\n" +
"gridded, institute, jet, jpl, laboratory, level, level-2, mean, month, monthly, national, ncei, noaa, nodc, number, observation, observations, ocean, oceanographic, oceans, propulsion, salinity, sci, science, sea, sea_surface_salinity, sea_surface_salinity_number_of_observations, sss, sss_obs, statistics, surface, swath, technology, time, used, v4.0, valid</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"Metadata_Conventions\">null</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v29</att>\n" +
"        <att name=\"summary\">This dataset is created by National Oceanographic Data Center (NODC) Satellite Oceanography Group from Aquarius level-2 SCI V4.0 data,using 1.0x1.0 (lon/lat) degree box average</att>\n" +
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
"            <att name=\"colorBarMaximum\" type=\"double\">37.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">32.0</att>\n" +
"            <att name=\"coordinates\">null</att>\n" +
"            <att name=\"ioos_category\">Salinity</att>\n" +
"            <att name=\"scale_factor\">null</att>\n" +
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
"            <att name=\"ioos_category\">Statistics</att>\n" +
"            <att name=\"scale_factor\">null</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        String2.log("\n*** EDDGridFromNcFiles.testGenerateDatasetsXmlWithRemoteThreddsFiles() finished successfully\n");
        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\n2016-02-29 This started failing with netcdf-java 4.6.4 with\n" +
                "message about End of File at position 20.\n" +
                "I reported to netcdf-java github BugTracker,\n" +
                "but they aren't interested in pursuing if I'm not."); 
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
"    Float64 actual_range 1.3133664e+9, 1.4263776e+9;\n" +
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
"    Float32 actual_range 89.5, -89.5;\n" +
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
"    String standard_name \"Sea_Surface_Salinity\";\n" +
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
"    String standard_name \"Sea_Surface_Salinity_number_of_observations\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String Conventions \"CF-1.6, COARDS, ACDD-1.3\";\n" +
"    String creator_email \"Yongsheng.Zhang@noaa.gov\";\n" +
"    String creator_institution \"US National Oceanographic Data Center\";\n" +
"    String creator_name \"Yongsheng Zhang, Ph.D.\";\n" +
"    String creator_url \"https://www.nodc.noaa.gov/SatelliteData\";\n" +
"    String date_created \"20150424T093015Z\";\n" +
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
"    String keywords \"aquarius, calculate, calculated, california, center, data, earth,\n" +
"Earth Science >Oceans > Surface Salinity,\n" +
"gridded, institute, jet, jpl, laboratory, level, level-2, mean, month, monthly, national, ncei, noaa, nodc, number, observation, observations, ocean, oceanographic, oceans, propulsion, salinity, sci, science, sea, sea_surface_salinity, sea_surface_salinity_number_of_observations, sss, sss_obs, statistics, surface, swath, technology, time, used, v4.0, valid\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"These data are available for use without restriction.\";\n" +
"    String mission \"SAC-D Aquarius\";\n" +
"    String necdf_version_id \"3.6.3\";\n" +
"    String nodc_template_version \"NODC_NetCDF_Grid_Template_v1.0\";\n" +
"    Float64 Northernmost_Northing 89.5;\n" +
"    String publisher_name \"NOAA/NESDIS/NODC - US National Oceanographic Data Center\";\n" +
"    String references \"Aquarius users guide, V6.0, PO.DAAC, JPL/NASA. Jun 2, 2014\";\n" +
"    String sensor \"Aquarius\";\n" +
"    String source \"Jet Propulsion Laboratory, California Institute of Technology\";\n" +
"    String sourceUrl \"(remote files)\";\n" +
"    Float64 Southernmost_Northing -89.5;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
"    String summary \"This dataset is created by National Oceanographic Data Center (NODC) Satellite Oceanography Group from Aquarius level-2 SCI V4.0 data,using 1.0x1.0 (lon/lat) degree box average\";\n" +
"    String time_coverage_end \"2015-03-15T00:00:00Z\";\n" +
"    String time_coverage_start \"2011-08-15T00:00:00Z\";\n" +
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
"  Float64 time[time = 44];\n" +
"  Float32 latitude[latitude = 180];\n" +
"  Float32 longitude[longitude = 360];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 sss[time = 44][latitude = 180][longitude = 360];\n" +
"    MAPS:\n" +
"      Float64 time[time = 44];\n" +
"      Float32 latitude[latitude = 180];\n" +
"      Float32 longitude[longitude = 360];\n" +
"  } sss;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 sss_obs[time = 44][latitude = 180][longitude = 360];\n" +
"    MAPS:\n" +
"      Float64 time[time = 44];\n" +
"      Float32 latitude[latitude = 180];\n" +
"      Float32 longitude[longitude = 360];\n" +
"  } sss_obs;\n" +
"} testRemoteThreddsFiles;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv  with data from one file
        String2.log("\n*** test read from one file\n");       
        userDapQuery = "sss[43][100:2:106][50:53]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_trtf", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"time,latitude,longitude,sss\n" +
"UTC,degrees_north,degrees_east,psu\n" +
"2015-03-15T00:00:00Z,-10.5,-129.5,35.764122\n" +
"2015-03-15T00:00:00Z,-10.5,-128.5,35.747765\n" +
"2015-03-15T00:00:00Z,-10.5,-127.5,35.75875\n" +
"2015-03-15T00:00:00Z,-10.5,-126.5,35.791206\n" +
"2015-03-15T00:00:00Z,-12.5,-129.5,35.893097\n" +
"2015-03-15T00:00:00Z,-12.5,-128.5,35.968\n" +
"2015-03-15T00:00:00Z,-12.5,-127.5,35.920654\n" +
"2015-03-15T00:00:00Z,-12.5,-126.5,35.961067\n" +
"2015-03-15T00:00:00Z,-14.5,-129.5,36.069397\n" +
"2015-03-15T00:00:00Z,-14.5,-128.5,36.19855\n" +
"2015-03-15T00:00:00Z,-14.5,-127.5,36.24694\n" +
"2015-03-15T00:00:00Z,-14.5,-126.5,36.085762\n" +
"2015-03-15T00:00:00Z,-16.5,-129.5,36.118313\n" +
"2015-03-15T00:00:00Z,-16.5,-128.5,36.377663\n" +
"2015-03-15T00:00:00Z,-16.5,-127.5,36.298218\n" +
"2015-03-15T00:00:00Z,-16.5,-126.5,36.25797\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\n2016-02-29 This started failing with netcdf-java 4.6.4 with\n" +
                "message about End of File at position 20 for each file\n" +
                "(I reported to netcdf-java github BugTracker,\n" +
                "but they aren't interested in pursuing if I'm not),\n" +
                "so no valid files."); 
        }        
        //  */
    }

    /** This tests matchAxisNDigits */
    public static void testMatchAxisNDigits() throws Throwable {

        String2.log("\n *** EDDGridFromNcFiles.testMatchAxisNDigits() ***");

        //force reload files
        File2.delete("/u00/cwatch/erddap2/dataset/ay/erdATssta3day/fileTable.nc");

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
     * and stored in /erddapTest/unsigned/
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
"      :scale_factor = 7.17185E-4f; // float\n" + //32768-> 23.50071808, so many values are higher
"      :add_offset = -2.0f; // float\n" +
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
" data:\n" +
"}\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //one time
        //String2.log(generateDatasetsXml(fileDir, fileName, fileDir + fileName,
        //    DEFAULT_RELOAD_EVERY_N_MINUTES, null));        

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
"    Float32 actual_range -89.95834, 89.95834;\n" + //test of descending lat axis
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
"    Float32 _FillValue 45.000717;\n" +   //important test of UInt16
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
"    String units \"deg_C\";\n" +  // ??? did ERDDAP add that?
"    Float32 valid_range -2.0, -1.9985657;\n" +
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
"    String keywords \"aqua, data, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, image, imaging, L3, l3m_data, l3m_qual, mapped, moderate, modis, modis a, ocean, oceans, quality, resolution, sea, sea_surface_temperature, smi, spectroradiometer, standard, surface, temperature, time\";\n" +
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
"      Float32 sst_quality[time = 1][latitude = 2160][longitude = 25];\n" +
"    MAPS:\n" +
"      Float64 time[time = 1];\n" +
"      Float32 latitude[latitude = 2160];\n" +
"      Float32 longitude[longitude = 25];\n" +
"  } sst_quality;\n" +
"} testUInt16File;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv data values
        userDapQuery = "sst[0][0:100:2159][(-134.95833513)]"; 
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, eddGrid.className(), ".csv"); 
        results = String2.directReadFrom88591File(
            EDStatic.fullTestCacheDirectory + tName);
        String2.log(results);
        expected = //difference from testUInt16Dap: lat lon are float here, not double
"time,latitude,longitude,sst\n" +
"UTC,degrees_north,degrees_east,deg_C\n" +
"2002-07-04T00:00:00Z,89.958336,-134.95833,-0.84102905\n" +
"2002-07-04T00:00:00Z,81.62501,-134.95833,-1.6371044\n" +
"2002-07-04T00:00:00Z,73.291664,-134.95833,-0.11021753\n" +
"2002-07-04T00:00:00Z,64.958336,-134.95833,NaN\n" + //_FillValue's correctly caught
"2002-07-04T00:00:00Z,56.625008,-134.95833,NaN\n" +
"2002-07-04T00:00:00Z,48.291664,-134.95833,12.6406145\n" +
"2002-07-04T00:00:00Z,39.958336,-134.95833,17.95137\n" +
"2002-07-04T00:00:00Z,31.625,-134.95833,20.432829\n" +
"2002-07-04T00:00:00Z,23.291664,-134.95833,19.664007\n" +
"2002-07-04T00:00:00Z,14.958336,-134.95833,24.482773\n" +
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
"    Float32 _FillValue -32767.0;\n" +
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
"    Float32 _FillValue -32767.0;\n" +
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
"    String date_created \"2015-10-02T00:07:10Z\";\n" +
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
"    String processing_control_input_parameters_datamax \"20.000000\";\n" +
"    String processing_control_input_parameters_datamin \"0.010000\";\n" +
"    String processing_control_input_parameters_deflate \"4\";\n" +
"    String processing_control_input_parameters_gap_fill \"0\";\n" +
"    String processing_control_input_parameters_ifile \"S19980011998031.L3b_MO_CHL.nc\";\n" +
"    String processing_control_input_parameters_latnorth \"90.000000\";\n" +
"    String processing_control_input_parameters_latsouth \"-90.000000\";\n" +
"    String processing_control_input_parameters_loneast \"180.000000\";\n" +
"    String processing_control_input_parameters_lonwest \"-180.000000\";\n" +
"    String processing_control_input_parameters_meas \"1\";\n" +
"    String processing_control_input_parameters_minobs \"0\";\n" +
"    String processing_control_input_parameters_ofile \"S19980011998031.L3m_MO_CHL_chlor_a_9km.nc\";\n" +
"    String processing_control_input_parameters_oformat \"netCDF4\";\n" +
"    String processing_control_input_parameters_palfile \"/sdps/sdpsoper/Science/OCSSW/V2015.3/data/common/palette/default.pal\";\n" +
"    String processing_control_input_parameters_precision \"F\";\n" +
"    String processing_control_input_parameters_processing \"2014.0\";\n" +
"    String processing_control_input_parameters_prod \"chlor_a\";\n" +
"    String processing_control_input_parameters_projection \"RECT\";\n" +
"    String processing_control_input_parameters_resolution \"9km\";\n" +
"    String processing_control_input_parameters_seam_lon \"-180.000000\";\n" +
"    String processing_control_input_parameters_stype \"2\";\n" +
"    String processing_control_l2_flag_names \"ATMFAIL,LAND,HILT,HISATZEN,STRAYLIGHT,CLDICE,COCCOLITH,LOWLW,CHLWARN,CHLFAIL,NAVWARN,MAXAERITER,ATMWARN,HISOLZEN,NAVFAIL,FILTER,HIGLINT\";\n" +
"    String processing_control_software_name \"smigen\";\n" +
"    String processing_control_software_version \"5.04\";\n" +
"    String processing_control_source \"S19980011998031.L3b_MO_CHL.nc\";\n" +
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
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
"    String summary \"NASA GSFC Ocean Color Web distributes science-quality chlorophyll-a\n" +
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
"    String title \"Chlorophyll-a, Orbview-2 SeaWiFS, R2014.0, 0.1\u00b0, Global, 1997-2010 (Monthly Composite)\";\n" +
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
"    Float32 _FillValue -32767.0;\n" +
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
"    Float32 _FillValue -32767.0;\n" +
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
"    String date_created \"2015-10-02T00:07:10Z\";\n" +
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
"    String processing_control_input_parameters_datamax \"20.000000\";\n" +
"    String processing_control_input_parameters_datamin \"0.010000\";\n" +
"    String processing_control_input_parameters_deflate \"4\";\n" +
"    String processing_control_input_parameters_gap_fill \"0\";\n" +
"    String processing_control_input_parameters_ifile \"S19980011998031.L3b_MO_CHL.nc\";\n" +
"    String processing_control_input_parameters_latnorth \"90.000000\";\n" +
"    String processing_control_input_parameters_latsouth \"-90.000000\";\n" +
"    String processing_control_input_parameters_loneast \"180.000000\";\n" +
"    String processing_control_input_parameters_lonwest \"-180.000000\";\n" +
"    String processing_control_input_parameters_meas \"1\";\n" +
"    String processing_control_input_parameters_minobs \"0\";\n" +
"    String processing_control_input_parameters_ofile \"S19980011998031.L3m_MO_CHL_chlor_a_9km.nc\";\n" +
"    String processing_control_input_parameters_oformat \"netCDF4\";\n" +
"    String processing_control_input_parameters_palfile \"/sdps/sdpsoper/Science/OCSSW/V2015.3/data/common/palette/default.pal\";\n" +
"    String processing_control_input_parameters_precision \"F\";\n" +
"    String processing_control_input_parameters_processing \"2014.0\";\n" +
"    String processing_control_input_parameters_prod \"chlor_a\";\n" +
"    String processing_control_input_parameters_projection \"RECT\";\n" +
"    String processing_control_input_parameters_resolution \"9km\";\n" +
"    String processing_control_input_parameters_seam_lon \"-180.000000\";\n" +
"    String processing_control_input_parameters_stype \"2\";\n" +
"    String processing_control_l2_flag_names \"ATMFAIL,LAND,HILT,HISATZEN,STRAYLIGHT,CLDICE,COCCOLITH,LOWLW,CHLWARN,CHLFAIL,NAVWARN,MAXAERITER,ATMWARN,HISOLZEN,NAVFAIL,FILTER,HIGLINT\";\n" +
"    String processing_control_software_name \"smigen\";\n" +
"    String processing_control_software_version \"5.04\";\n" +
"    String processing_control_source \"S19980011998031.L3b_MO_CHL.nc\";\n" +
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
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
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
"    Float32 _FillValue -32767.0;\n" +
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
"    Float32 _FillValue -32767.0;\n" +
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
"    String date_created \"2015-10-02T00:07:10Z\";\n" +
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
"    String processing_control_input_parameters_datamax \"20.000000\";\n" +
"    String processing_control_input_parameters_datamin \"0.010000\";\n" +
"    String processing_control_input_parameters_deflate \"4\";\n" +
"    String processing_control_input_parameters_gap_fill \"0\";\n" +
"    String processing_control_input_parameters_ifile \"S19980011998031.L3b_MO_CHL.nc\";\n" +
"    String processing_control_input_parameters_latnorth \"90.000000\";\n" +
"    String processing_control_input_parameters_latsouth \"-90.000000\";\n" +
"    String processing_control_input_parameters_loneast \"180.000000\";\n" +
"    String processing_control_input_parameters_lonwest \"-180.000000\";\n" +
"    String processing_control_input_parameters_meas \"1\";\n" +
"    String processing_control_input_parameters_minobs \"0\";\n" +
"    String processing_control_input_parameters_ofile \"S19980011998031.L3m_MO_CHL_chlor_a_9km.nc\";\n" +
"    String processing_control_input_parameters_oformat \"netCDF4\";\n" +
"    String processing_control_input_parameters_palfile \"/sdps/sdpsoper/Science/OCSSW/V2015.3/data/common/palette/default.pal\";\n" +
"    String processing_control_input_parameters_precision \"F\";\n" +
"    String processing_control_input_parameters_processing \"2014.0\";\n" +
"    String processing_control_input_parameters_prod \"chlor_a\";\n" +
"    String processing_control_input_parameters_projection \"RECT\";\n" +
"    String processing_control_input_parameters_resolution \"9km\";\n" +
"    String processing_control_input_parameters_seam_lon \"-180.000000\";\n" +
"    String processing_control_input_parameters_stype \"2\";\n" +
"    String processing_control_l2_flag_names \"ATMFAIL,LAND,HILT,HISATZEN,STRAYLIGHT,CLDICE,COCCOLITH,LOWLW,CHLWARN,CHLFAIL,NAVWARN,MAXAERITER,ATMWARN,HISOLZEN,NAVFAIL,FILTER,HIGLINT\";\n" +
"    String processing_control_software_name \"smigen\";\n" +
"    String processing_control_software_version \"5.04\";\n" +
"    String processing_control_source \"S19980011998031.L3b_MO_CHL.nc\";\n" +
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
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
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
     */
    public static void testGenerateDatasetsXmlGroups() throws Throwable {
        //A test for Jessica Hausman
        //the test file is from ftp://podaac.jpl.nasa.gov/allData/aquarius/L2/V4/2011/237/
        String results = generateDatasetsXml(
            EDStatic.unitTestDataDir + "hdf/", 
            "Q2011237000100.L2_SCI_V4\\.0", "",
            DEFAULT_RELOAD_EVERY_N_MINUTES, null) + "\n";

        String expected = 
directionsForGenerateDatasetsXml() +
"!!! The source for nc_7d6c_1a03_1145 has nGridVariables=154,\n" +
"but this dataset will only serve 1 because the others use different dimensions.\n" +
"-->\n" +
"\n" +
"<dataset type=\"EDDGridFromNcFiles\" datasetID=\"nc_7d6c_1a03_1145\" active=\"true\">\n" +
"    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>" + EDStatic.unitTestDataDir + "nc/</fileDir>\n" +
"    <fileNameRegex>Q2011237000100.L2_SCI_V4\\.0\\.nc</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <matchAxisNDigits>20</matchAxisNDigits>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <accessibleViaFiles>false</accessibleViaFiles>\n" +
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
"        <att name=\"creator_url\">https://oceancolor.gsfc.nasa.gov/cms/</att>\n" +
"        <att name=\"Data_Center\">null</att>\n" +
"        <att name=\"End_Day\">null</att>\n" +
"        <att name=\"End_Millisec\">null</att>\n" +
"        <att name=\"End_Time\">null</att>\n" +
"        <att name=\"End_Year\">null</att>\n" +
"        <att name=\"infoUrl\">https://oceancolor.gsfc.nasa.gov/cms/</att>\n" +
"        <att name=\"keywords\">aquarius, Aquarius_Flags_rad_rfi_flags, Aquarius_Flags_unnamedDim0, Aquarius_Flags_unnamedDim1, Aquarius_Flags_unnamedDim2, Aquarius_Flags_unnamedDim3, biology, center, color, data, flight, goddard, group, gsfc, level, nasa, obpg, ocean, processing, quality, space</att>\n" +
"        <att name=\"Latitude_Units\">null</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"Longitude_Units\">null</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v29</att>\n" +
"        <att name=\"Start_Day\">null</att>\n" +
"        <att name=\"Start_Millisec\">null</att>\n" +
"        <att name=\"Start_Time\">null</att>\n" +
"        <att name=\"Start_Year\">null</att>\n" +
"        <att name=\"summary\">Aquarius Level 2 Data. NASA/Goddard Space Flight Center (GSFC) Ocean Biology Processing Group (OBPG) data from a local source.</att>\n" +
"        <att name=\"Title\">null</att>\n" +
"        <att name=\"title\">Aquarius Level 2 Data</att>\n" +
"    </addAttributes>\n" +
"    <axisVariable>\n" +
"        <sourceName>Aquarius_Flags/_unnamedDim0</sourceName>\n" +
"        <destinationName>Aquarius_Flags_unnamedDim0</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"            <att name=\"long_name\">Aquarius Flags Unnamed Dim0</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>Aquarius_Flags/_unnamedDim1</sourceName>\n" +
"        <destinationName>Aquarius_Flags_unnamedDim1</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"            <att name=\"long_name\">Aquarius Flags Unnamed Dim1</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>Aquarius_Flags/_unnamedDim2</sourceName>\n" +
"        <destinationName>Aquarius_Flags_unnamedDim2</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"            <att name=\"long_name\">Aquarius Flags Unnamed Dim2</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>Aquarius_Flags/_unnamedDim3</sourceName>\n" +
"        <destinationName>Aquarius_Flags_unnamedDim3</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"            <att name=\"long_name\">Aquarius Flags Unnamed Dim3</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Aquarius_Flags/rad_rfi_flags</sourceName>\n" +
"        <destinationName>Aquarius_Flags_rad_rfi_flags</destinationName>\n" +
"        <dataType>byte</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_Unsigned\">true</att>\n" +
"            <att name=\"long_name\">Radiometer RFI flags</att>\n" +
"            <att name=\"valid_max\" type=\"byte\">0</att>\n" +
"            <att name=\"valid_min\" type=\"byte\">0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";
        Test.ensureEqual(results, expected, 
            "results.length=" + results.length() + " expected.length=" + expected.length() + 
            "\nresults=\n" + results);

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
                    String2.pressEnterToContinue("Shouldn't get here!");
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
//http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.csv?y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]
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
     */
    public static void testBigRequestSpeed(int nTimePoints, String fileType, 
        int expectedBytes, int expectedMs) throws Throwable {
        testVerboseOn();
        Math2.gcAndWait();  //in a test
        Math2.gcAndWait();  //in a test
        String2.log("\n*** EDDGridFromDap.testBigRequestSpeed  partialRequestMaxBytes=" +
            EDStatic.partialRequestMaxBytes + 
            "\n nTimePoints=" + nTimePoints +
            " estimated nPartialRequests=" + 
            Math2.hiDiv(nTimePoints * 4320 * 8640, EDStatic.partialRequestMaxBytes));
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, "nceiPH53sstd1day"); 
        String query = "sea_surface_temperature[0:" + (nTimePoints - 1) + "][][]";
        String dir = EDStatic.fullTestCacheDirectory;
        String tName;

        //tName = eddGrid.makeNewFileForDapQuery(null, null, query,
        //    dir, eddGrid.className() + "_testBigRequest", fileType); 

        //time the second request
        long time = System.currentTimeMillis();
        tName = eddGrid.makeNewFileForDapQuery(null, null, query,
            dir, eddGrid.className() + "_testBigRequest2", fileType); 
        String2.pressEnterToContinue("fileType=" + fileType + " finished.\n" +
            "size=" + File2.length(dir + tName) + " (expected=" + expectedBytes + ")\n" + 
            "time=" + (System.currentTimeMillis() - time) +
            "ms (expected=" + expectedMs + "ms but it can be 10x slower)");
    }



    /**
     * This tests this class.
     *
     * @throws Throwable if trouble
     */
    public static void test(boolean deleteCachedDatasetInfo) throws Throwable {
/* for releases, this line should have open/close comment */
        testNc(deleteCachedDatasetInfo);
        testCwHdf(deleteCachedDatasetInfo);
        testHdf();
        testNcml();
        testNccsv();
        testGrib_43(deleteCachedDatasetInfo);  //42 or 43 for netcdfAll 4.2- or 4.3+
        testGrib2_43(deleteCachedDatasetInfo); //42 or 43 for netcdfAll 4.2- or 4.3+
        testGenerateDatasetsXml();
        testGenerateDatasetsXml2();
        testGenerateDatasetsXml3();
        testGenerateDatasetsXml4();
        testSpeed(0, 1000);  
        testAVDVSameSource();
        test2DVSameSource();
        testAVDVSameDestination();
        test2DVSameDestination();
        testUInt16File();
        testTimePrecisionMillis();
        testSimpleTestNc();
        testSimpleTestNc2();
        testSpecialAxis0Time();
        testSpecialAxis0FileNameInt();
        testSpecialAxis0GlobalDouble();
//unfinished:    testRTechHdf();
        testUpdate();
        testQuickRestart();
        testBigRequestSpeed(3, ".dods", 895847390, 9783); //expected bytes, ms
        //tests of remote sources on-the-fly
        //NetcdfFile.open(          "https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V4.0/monthly/sss_binned_L3_MON_SCI_V4.0_2011.nc");
        //NetcdfDataset.openDataset("https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V4.0/monthly/sss_binned_L3_MON_SCI_V4.0_2011.nc");
        //from command line: curl --head https://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V4.0/monthly/sss_binned_L3_MON_SCI_V4.0_2011.nc
        testGenerateDatasetsXmlWithRemoteThreddsFiles();
        testRemoteThreddsFiles(false); //deleteCachedInfo 
        testMatchAxisNDigits();
        testIgor();
        testBadNcFile(false);  //runIncrediblySlowTest?
        testInvalidShortenedNcFile();

        testGenerateDatasetsXmlAwsS3();   //VERY SLOW
        testAwsS3(false);  //deleteCachedInfo   //VERY SLOW

        /* */

        //not usually run
        //testQuickRestart2();

        //NOT FINISHED
        //none

        //Remote Hyrax
        //  These were tests of reading remote data files on Hyrax without downloading,
        //  but test Hyrax server doesn't support Byte Ranges.
        //testGenerateDatasetsXmlWithRemoteHyraxFiles();  //Test server doesn't support Byte Ranges.
        //testRemoteHyraxFiles(false); //deleteCachedInfo //Test server doesn't support Byte Ranges.

        //NOT FINISHED. test file for groups also has no named dimensions.  
        //testGenerateDatasetsXmlGroups();
        //testGroups();

        //one time tests
        //String fiName = String2.unitTestBigDataDir + "geosgrib/multi_1.glo_30m.all.grb2";
        //String2.log(NcHelper.dumpString(fiName, false));
        //NetcdfDataset in = NetcdfDataset.openDataset(fiName);
        //in.close();

    }


}

