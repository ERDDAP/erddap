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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Get netcdf-X.X.XX.jar from http://www.unidata.ucar.edu/software/netcdf-java/index.htm
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
 * GRIB .grb (http://en.wikipedia.org/wiki/GRIB),
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
     * Subclasses override this: 
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
        String tAccessibleTo, boolean tAccessibleViaWMS,
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

        super("EDDGridFromNcFiles", tDatasetID, tAccessibleTo, tAccessibleViaWMS,
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
"        <att name=\"institution\">NOAA CoastWatch WCN</att>\n" +
"        <att name=\"keywords\">altitude, atmosphere,\n" +
"Atmosphere &gt; Atmospheric Winds &gt; Surface Winds,\n" +
"atmospheric, coast, coastwatch, composite, data, day, global, latitude, longitude, meridional, mod, modulus, noaa, node, ocean, oceans,\n" +
"Oceans &gt; Ocean Winds &gt; Surface Winds,\n" +
"quality, quikscat, science, science quality, surface, time, wcn, west, wind, winds, x_wind, y_wind, zonal</att>\n" +
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
        Test.ensureEqual(results, expected, results.length() + " " + expected.length() + 
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
            "/erddapTestBig/geosgrib/", ".*", 
            "/erddapTestBig/geosgrib/multi_1.glo_30m.all.grb2",
            DEFAULT_RELOAD_EVERY_N_MINUTES, null);
        String suggDatasetID = suggestDatasetID(
            "/erddapTestBig/geosgrib/.*");

        String expected = //as of 2012-02-20. Will change if John Caron fixes bugs I reported.
directionsForGenerateDatasetsXml() +
"!!! The source for " + suggDatasetID + " has nGridVariables=13,\n" +
"but this dataset will only serve 3 because the others use different dimensions.\n" +
"-->\n" +
"\n" +
"<dataset type=\"EDDGridFromNcFiles\" datasetID=\"" + suggDatasetID + "\" active=\"true\">\n" +
"    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>/erddapTestBig/geosgrib/</fileDir>\n" +
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
"        <att name=\"history\">Read using CDM IOSP Grib2Collection</att>\n" +
"        <att name=\"Originating_or_generating_Center\">US National Weather Service, National Centres for Environmental Prediction (NCEP)</att>\n" +
"        <att name=\"Originating_or_generating_Subcenter\">0</att>\n" +
"        <att name=\"Type_of_generating_process\">Forecast</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"Conventions\">CF-1.6, COARDS, ACDD-1.3</att>\n" +
"        <att name=\"creator_name\">NCEP</att>\n" +
"        <att name=\"creator_url\">http://www.ncep.noaa.gov/</att>\n" + 
"        <att name=\"infoUrl\">http://www.ncep.noaa.gov/</att>\n" +
"        <att name=\"institution\">NCEP</att>\n" +
"        <att name=\"keywords\">centers, data, direction, Direction_of_swell_waves_ordered_sequence_of_data, environmental, height, local, mean, Mean_period_of_swell_waves_ordered_sequence_of_data, national, ncep, ocean, oceans,\n" +
"Oceans &gt; Ocean Waves &gt; Significant Wave Height,\n" +
"Oceans &gt; Ocean Waves &gt; Swells,\n" +
"Oceans &gt; Ocean Waves &gt; Wave Period,\n" +
"ordered, ordered_sequence_of_data, period, prediction, sea, sea_surface_swell_wave_period, sea_surface_swell_wave_significant_height, sea_surface_swell_wave_to_direction, sequence, significant, Significant_height_of_swell_waves_ordered_sequence_of_data, source, surface, surface waves, swell, swells, time, wave, waves</att>\n" +
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
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"units\">Hour since 2009-06-01T06:00:00Z</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Time</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>ordered_sequence_of_data</sourceName>\n" +
"        <destinationName>ordered_sequence_of_data</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"Grib2_level_type\" type=\"int\">241</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Taxonomy</att>\n" +
"            <att name=\"long_name\">Ordered Sequence Of Data</att>\n" +
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
"            <att name=\"Grib2_Generating_Process_Type\">Forecast</att>\n" +
"            <att name=\"Grib2_Level_Type\" type=\"int\">241</att>\n" +
"            <att name=\"Grib2_Parameter\" type=\"intList\">10 0 7</att>\n" +
"            <att name=\"Grib2_Parameter_Category\">Waves</att>\n" +
"            <att name=\"Grib2_Parameter_Discipline\">Oceanographic products</att>\n" +
"            <att name=\"Grib2_Parameter_Name\">Direction of swell waves</att>\n" +
"            <att name=\"Grib_Variable_Id\">VAR_10-0-7_L241</att>\n" +
"            <att name=\"long_name\">Direction of swell waves @ Ordered Sequence of Data</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"            <att name=\"units\">degree.true</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Surface Waves</att>\n" +
"            <att name=\"standard_name\">sea_surface_swell_wave_to_direction</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Significant_height_of_swell_waves_ordered_sequence_of_data</sourceName>\n" +
"        <destinationName>Significant_height_of_swell_waves_ordered_sequence_of_data</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"abbreviation\">SWELL</att>\n" +
"            <att name=\"Grib2_Generating_Process_Type\">Forecast</att>\n" +
"            <att name=\"Grib2_Level_Type\" type=\"int\">241</att>\n" +
"            <att name=\"Grib2_Parameter\" type=\"intList\">10 0 8</att>\n" +
"            <att name=\"Grib2_Parameter_Category\">Waves</att>\n" +
"            <att name=\"Grib2_Parameter_Discipline\">Oceanographic products</att>\n" +
"            <att name=\"Grib2_Parameter_Name\">Significant height of swell waves</att>\n" +
"            <att name=\"Grib_Variable_Id\">VAR_10-0-8_L241</att>\n" +
"            <att name=\"long_name\">Significant height of swell waves @ Ordered Sequence of Data</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Surface Waves</att>\n" +
"            <att name=\"standard_name\">sea_surface_swell_wave_significant_height</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Mean_period_of_swell_waves_ordered_sequence_of_data</sourceName>\n" +
"        <destinationName>Mean_period_of_swell_waves_ordered_sequence_of_data</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"abbreviation\">SWPER</att>\n" +
"            <att name=\"Grib2_Generating_Process_Type\">Forecast</att>\n" +
"            <att name=\"Grib2_Level_Type\" type=\"int\">241</att>\n" +
"            <att name=\"Grib2_Parameter\" type=\"intList\">10 0 9</att>\n" +
"            <att name=\"Grib2_Parameter_Category\">Waves</att>\n" +
"            <att name=\"Grib2_Parameter_Discipline\">Oceanographic products</att>\n" +
"            <att name=\"Grib2_Parameter_Name\">Mean period of swell waves</att>\n" +
"            <att name=\"Grib_Variable_Id\">VAR_10-0-9_L241</att>\n" +
"            <att name=\"long_name\">Mean period of swell waves @ Ordered Sequence of Data</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"            <att name=\"units\">s</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">20.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Surface Waves</att>\n" +
"            <att name=\"standard_name\">sea_surface_swell_wave_period</att>\n" +
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


    /** This tests generateDatasetsXml with an AWS S3 dataset. 
     * @throws Throwable if touble
     */
    public static void testGenerateDatasetsXmlAwsS3() throws Throwable {

        String2.log("\n*** EDDGridFromNcFiles.testGenerateDatasetsXmlAwsS3");
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
"        <att name=\"creator_url\">http://www.nasa.gov/</att>\n" +
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
"            <att name=\"_ChunkSize\" type=\"int\">1</att>\n" +
"            <att name=\"axis\">T</att>\n" +
"            <att name=\"bounds\">time_bnds</att>\n" +
"            <att name=\"calendar\">standard</att>\n" +
"            <att name=\"long_name\">time</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"units\">days since 1950-01-01 00:00:00</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSize\">null</att>\n" +
"            <att name=\"bounds\">null</att>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>lat</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSize\" type=\"int\">3105</att>\n" +
"            <att name=\"axis\">Y</att>\n" +
"            <att name=\"bounds\">lat_bnds</att>\n" +
"            <att name=\"long_name\">latitude</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSize\">null</att>\n" +
"            <att name=\"bounds\">null</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>lon</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_ChunkSize\" type=\"int\">7025</att>\n" +
"            <att name=\"axis\">X</att>\n" +
"            <att name=\"bounds\">lon_bnds</att>\n" +
"            <att name=\"long_name\">longitude</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"            <att name=\"valid_range\" type=\"doubleList\">0.0 360.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_ChunkSize\">null</att>\n" +
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
"            <att name=\"_ChunkSize\" type=\"intList\">1 369 836</att>\n" +
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
"            <att name=\"_ChunkSize\">null</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">313.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">263.0</att>\n" +
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
        Test.ensureEqual(edd.title(), "800m Downscaled NEX CMIP5 Climate Projections for the Continental US", "");
        Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
            "tasmin", "");

        String2.log("\nEDDGridFromNcFiles.testGenerateDatasetsXmlAwsS3 passed the test.");

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nExpected error: Have you updated your AWS credentials lately?\n"); 
        }
    }

    /**
     * This tests reading NetCDF .nc files with this class.
     *
     * @throws Throwable if trouble
     */
    public static void testAwsS3(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDGridFromNcFiles.testNc() *****************\n");
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
"    String creator_url \"http://www.nasa.gov/\";\n" +
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

//            + " http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/ux10/1day\n" +
//today + 

expected = 
//"2015-06-24T17:36:33Z http://127.0.0.1:8080/cwexperimental/griddap/testAwsS3.das\";\n" +
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
"    String sourceUrl \"(local files)\";\n" +
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
                "\nExpected error: Have you updated your AWS credentials lately?\n"); 
        }

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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
"    String creator_url \"http://coastwatch.pfel.noaa.gov\";\n" +
"    String date_created \"2008-08-29Z\";\n" +
"    String date_issued \"2008-08-29Z\";\n" +
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

//            + " http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/ux10/1day\n" +
//today + 

expected = " http://127.0.0.1:8080/cwexperimental/griddap/testGriddedNcFiles.das\";\n" +
"    String infoUrl \"http://coastwatch.pfel.noaa.gov/infog/QS_ux10_las.html\";\n" +
"    String institution \"NOAA CoastWatch, West Coast Node\";\n" +
"    String keywords \"EARTH SCIENCE > Oceans > Ocean Winds > Surface Winds\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.\";\n" +
"    String naming_authority \"gov.noaa.pfel.coastwatch\";\n" +
"    Float64 Northernmost_Northing 89.875;\n" +
"    String origin \"Remote Sensing Systems, Inc\";\n" +
"    String processing_level \"3\";\n" +
"    String project \"CoastWatch (http://coastwatch.noaa.gov/)\";\n" +
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
" http://127.0.0.1:8080/cwexperimental/griddap/testGribFiles_42.das\";\n" +
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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

        String2.log(NcHelper.dumpString("/erddapTestBig/geosgrib/multi_1.glo_30m.all.grb2", false));

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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
" http://127.0.0.1:8080/cwexperimental/griddap/testGrib2_42.das\";\n" +
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
"    String location \"/erddapTestBig/geosgrib/multi_1.glo_30m.all.grb2\";\n" +
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = //2013-09-03 The details of the GRIB attributes change frequently!
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
"    Float32 actual_range 10.0, 10.0;\n" +
"    String datum \"ground\";\n" +
"    Int32 Grib1_level_code 105;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Specified Height Level above Ground\";\n" +
"    String positive \"up\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range 88.75001, -88.74999;\n" +
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
"    Int32 Grib1_Center 74;\n" +
"    String Grib1_Level_Desc \"Specified height level above ground\";\n" +
"    Int32 Grib1_Level_Type 105;\n" +
"    Int32 Grib1_Parameter 32;\n" +
"    Int32 Grib1_Subcenter 0;\n" +
"    Int32 Grib1_TableVersion 1;\n" +
"    String Grib_Variable_Id \"VAR_74-0-1-32_L105\";\n" +
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
"    String history \"Read using CDM IOSP Grib1Collection\n" +
today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);

//+ " (local files)\n" +
//today + 
try {
expected = 
"http://127.0.0.1:8080/cwexperimental/griddap/testGribFiles_43.das\";\n" +
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
"    Float64 Northernmost_Northing 88.75001;\n" +
"    String Originating_or_generating_Center \"UK Meteorological Office ­ Exeter (RSMC)\";\n" + //- is #173!
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.243836e+9, 1.244484e+9;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Forecast Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range 90.0, -77.5;\n" +
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
"    String Grib2_Generating_Process_Type \"Forecast\";\n" +
"    Int32 Grib2_Level_Type 241;\n" +
"    Int32 Grib2_Parameter 10, 0, 9;\n" +
"    String Grib2_Parameter_Category \"Waves\";\n" +
"    String Grib2_Parameter_Discipline \"Oceanographic products\";\n" +
"    String Grib2_Parameter_Name \"Mean period of swell waves\";\n" +
"    String Grib_Variable_Id \"VAR_10-0-9_L241\";\n" +
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
"    String Grib2_Generating_Process_Type \"Forecast\";\n" +
"    Int32 Grib2_Level_Type 1;\n" +
"    Int32 Grib2_Parameter 10, 0, 6;\n" +
"    String Grib2_Parameter_Category \"Waves\";\n" +
"    String Grib2_Parameter_Discipline \"Oceanographic products\";\n" +
"    String Grib2_Parameter_Name \"Mean period of wind waves\";\n" +
"    String Grib_Variable_Id \"VAR_10-0-6_L1\";\n" +
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
"    String Grib2_Generating_Process_Type \"Forecast\";\n" +
"    Int32 Grib2_Level_Type 1;\n" +
"    Int32 Grib2_Parameter 10, 0, 11;\n" +
"    String Grib2_Parameter_Category \"Waves\";\n" +
"    String Grib2_Parameter_Discipline \"Oceanographic products\";\n" +
"    String Grib2_Parameter_Name \"Primary wave mean period\";\n" +
"    String Grib_Variable_Id \"VAR_10-0-11_L1\";\n" +
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
"    String Grib2_Generating_Process_Type \"Forecast\";\n" +
"    Int32 Grib2_Level_Type 1;\n" +
"    Int32 Grib2_Parameter 10, 0, 3;\n" +
"    String Grib2_Parameter_Category \"Waves\";\n" +
"    String Grib2_Parameter_Discipline \"Oceanographic products\";\n" +
"    String Grib2_Parameter_Name \"Significant height of combined wind waves and swell\";\n" +
"    String Grib_Variable_Id \"VAR_10-0-3_L1\";\n" +
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
"    String Grib2_Generating_Process_Type \"Forecast\";\n" +
"    Int32 Grib2_Level_Type 241;\n" +
"    Int32 Grib2_Parameter 10, 0, 8;\n" +
"    String Grib2_Parameter_Category \"Waves\";\n" +
"    String Grib2_Parameter_Discipline \"Oceanographic products\";\n" +
"    String Grib2_Parameter_Name \"Significant height of swell waves\";\n" +
"    String Grib_Variable_Id \"VAR_10-0-8_L241\";\n" +
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
"    String Grib2_Generating_Process_Type \"Forecast\";\n" +
"    Int32 Grib2_Level_Type 1;\n" +
"    Int32 Grib2_Parameter 10, 0, 5;\n" +
"    String Grib2_Parameter_Category \"Waves\";\n" +
"    String Grib2_Parameter_Discipline \"Oceanographic products\";\n" +
"    String Grib2_Parameter_Name \"Significant height of wind waves\";\n" +
"    String Grib_Variable_Id \"VAR_10-0-5_L1\";\n" +
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
"    String Grib2_Generating_Process_Type \"Forecast\";\n" +
"    Int32 Grib2_Level_Type 1;\n" +
"    Int32 Grib2_Parameter 0, 2, 2;\n" +
"    String Grib2_Parameter_Category \"Momentum\";\n" +
"    String Grib2_Parameter_Discipline \"Meteorological products\";\n" +
"    String Grib2_Parameter_Name \"u-component of wind\";\n" +
"    String Grib_Variable_Id \"VAR_0-2-2_L1\";\n" +
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
"    String Grib2_Generating_Process_Type \"Forecast\";\n" +
"    Int32 Grib2_Level_Type 1;\n" +
"    Int32 Grib2_Parameter 0, 2, 3;\n" +
"    String Grib2_Parameter_Category \"Momentum\";\n" +
"    String Grib2_Parameter_Discipline \"Meteorological products\";\n" +
"    String Grib2_Parameter_Name \"v-component of wind\";\n" +
"    String Grib_Variable_Id \"VAR_0-2-3_L1\";\n" +
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
"    String Grib2_Generating_Process_Type \"Forecast\";\n" +
"    Int32 Grib2_Level_Type 1;\n" +
"    Int32 Grib2_Parameter 0, 2, 1;\n" +
"    String Grib2_Parameter_Category \"Momentum\";\n" +
"    String Grib2_Parameter_Discipline \"Meteorological products\";\n" +
"    String Grib2_Parameter_Name \"Wind speed\";\n" +
"    String Grib_Variable_Id \"VAR_0-2-1_L1\";\n" +
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
"    String history \"Read using CDM IOSP Grib2Collection\n" +
today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);

//+ " (local files)\n" +
//today + 
expected= 
"http://127.0.0.1:8080/cwexperimental/griddap/testGrib2_43.das\";\n" +
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
"    String location \"/erddapTestBig/geosgrib/multi_1.glo_30m.all.grb2\";\n" +
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
"http://cfconventions.org/Data/cf-conventions/cf-conventions-1.6/build/cf-conventions.html#idp4775248\n" +
"says\n" +
"\"Variable, dimension and attribute names should begin with a letter and be\n" +
"composed of letters, digits, and underscores.\"\n" +
"BUT NOTHING HAS CHANGED!\n" +
"So now generatedDatasetsXml suggests setting original to null, \n" +
"and adds a variant with a valid CF attribute name."); 
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
        String2.log("\n****************** EDDGridFromNcFiles.testCwHdf() *****************\n");
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
        String2.log("\n*** testCwHdf test das dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_CwHdfEntire", ".das"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
" http://127.0.0.1:8080/cwexperimental/griddap/testCwHdf.das\";\n" +
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
     * @param whichTest -1 for all, or 0..
     */
    public static void testSpeed(int whichTest) throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testSpeed\n" + 
            SgtUtil.isBufferedImageAccelerated() + "\n");
        boolean oReallyVerbose = reallyVerbose;
        reallyVerbose = false;
        String tName;
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, "testGriddedNcFiles"); 
        String userDapQuery = "y_wind[(1.1999664e9)][0][][0:719]"; //719 avoids esriAsc cross lon=180
        String dir = EDStatic.fullTestCacheDirectory;
        String extensions[] = new String[]{
            ".asc", ".csv", ".csvp", ".csv0", 
            ".das", ".dds", ".dods", ".esriAscii", 
            ".graph", ".html", ".htmlTable",   //.help not available at this level
            ".json", ".mat", 
            ".nc", ".ncHeader", 
            ".odvTxt", ".tsv", ".tsvp", ".tsv0", ".xhtml", 
            ".geotif", ".kml", 
            ".smallPdf", ".pdf", ".largePdf", 
            ".smallPng", ".png", ".largePng", 
            ".transparentPng"};
        int expectedMs[] = new int[]  {  
            //now Java 1.7/M4700          //was Java 1.6 times            //was java 1.5 times
            187, 905, 811, 800,           //734, 6391, 6312, ?            //1250, 9750, 9562, ?                                  
            15, 15, 109, 8112,            //15, 15, 156, 16875            //15, 15, 547, 18859
            63, 47, 561,                  //63, 47, 2032,                 //93, 31, ...,
            921, 125,                     //6422, 203,                    //9621, 625,  
            121, 121,                     //2015-02 faster: 121. 2014-09 slower 163->331: java? netcdf-java? unsure //234, 250,   //500, 500, 
            1248, 811, 811, 811, 1139,    //9547, 6297, 6281, ?, 8625,    //13278, 8766, 8844, ?, 11469, 
            750, 10,                      //2015-02 kml faster: 10, 2014-09 kml slower 110->258. why?  656, 110,         //687, 94,  //Java 1.7 was 390r until change to new netcdf-Java
            444, 976, 1178,               //860, 2859, 3438,              //2188, 4063, 3797,   //small varies greatly
            160, 378, 492,                //2015-02 faster: 160, 2014-09 png slower 212,300->378. why? //438, 468, 1063,               //438, 469, 1188,     //small varies greatly
            758};                         //1703                          //2359};
        int bytes[]    = new int[]   {
            5875592, 23734053, 23734063, 23733974, 
            6006, 303, 2085486, 4701074, 
            60787, 51428, 14770799, 
            31827797, 2085800, 
            2090600, 5285, 
            24337084, 23734053, 23734063, 23733974, 90604796, 
            523113, 3601, 
            478774, 2189656, 2904880, 
            30852, 76777, 277494, 
            335307};

        //warm up
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            dir, eddGrid.className() + "_testSpeedw", ".csvp"); 
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            dir, eddGrid.className() + "_testSpeedw", ".png"); 
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            dir, eddGrid.className() + "_testSpeedw", ".pdf"); 
        
        int firstExt = whichTest < 0? 0 : whichTest;
        int lastExt  = whichTest < 0? extensions.length - 1 : whichTest;
        for (int ext = firstExt; ext <= lastExt; ext++) {
            try {
                String2.log("\n*** EDDGridFromNcFiles.testSpeed test#" + ext + ": " + 
                    extensions[ext] + " speed\n");
                long time = 0, cLength = 0;
                for (int chance = 0; chance < 3; chance++) {
                    Math2.gcAndWait(); //in a test
                    time = System.currentTimeMillis();
                    tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
                        dir, eddGrid.className() + 
                        "_testSpeed" + extensions[ext].substring(1) + chance + ext, 
                        extensions[ext]); 
                    time = System.currentTimeMillis() - time;
                    cLength = File2.length(dir + tName);
                    String2.log("\n*** EDDGridFromNcFiles.testSpeed test#" + ext + 
                        " chance#" + chance + ": " + extensions[ext] + " done.\n  " + 
                        cLength + " bytes (" + bytes[ext]+ 
                        ").  time=" + time + " ms (expected=" + expectedMs[ext] + ")\n");
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
                    "\n" + dir + tName);
                Test.ensureTrue(cLength < 1.1 * bytes[ext], 
                    "File longer than expected.  observed=" + 
                    cLength + " expected=~" + bytes[ext] +
                    "\n" + dir + tName);

                //time test
                if (time > 1.5 * Math.max(50, expectedMs[ext]))
                    throw new SimpleException(
                        "Slower than expected. observed=" + time + 
                        " expected=~" + expectedMs[ext] + " ms.");
                if (time < (expectedMs[ext] <= 50? 0.1 : 0.5) * expectedMs[ext])
                    throw new SimpleException(
                        "Faster than expected! observed=" + time + 
                        " expected=~" + expectedMs[ext] + " ms.");
            } catch (Exception e) {
                String2.pressEnterToContinue(MustBe.throwableToString(e) +
                    "\nUnexpected ERROR for Test#" + ext + ": " + extensions[ext] + "."); 
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
        String2.log("\n****************** EDDGridFromNcFiles.testAVDVSameSource() *****************\n");
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
        String2.log("\n****************** EDDGridFromNcFiles.test2DVSameSource() *****************\n");
        String error = "shouldn't happen";
        try {
            EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, "test2DVSameSource"); 
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
            error = String2.split(MustBe.throwableToString(t), '\n')[1]; 
        }

        Test.ensureEqual(error, 
            "Two variables have the same sourceName=IB_time.", 
            "Unexpected error message:\n" + error);
    }

    /**
     * This tests that ensureValid throws exception if an AxisVariable and 
     * a dataVariable use the same destinationName.
     *
     * @throws Throwable if trouble
     */
    public static void testAVDVSameDestination() throws Throwable {
        String2.log("\n****************** EDDGridFromNcFiles.testAVDVSameDestination() *****************\n");
        String error = "shouldn't happen";
        try {
            EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, "testAVDVSameDestination"); 
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
            error = String2.split(MustBe.throwableToString(t), '\n')[1]; 
        }

        Test.ensureEqual(error, 
            "Two variables have the same destinationName=OB_time.", 
            "Unexpected error message:\n" + error);
    }

    /**
     * This tests that ensureValid throws exception if 2  
     * dataVariables use the same destinationName.
     *
     * @throws Throwable if trouble
     */
    public static void test2DVSameDestination() throws Throwable {
        String2.log("\n****************** EDDGridFromNcFiles.test2DVSameDestination() *****************\n");
        String error = "shouldn't happen";
        try {
            EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, "test2DVSameDestination"); 
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
            error = String2.split(MustBe.throwableToString(t), '\n')[1]; 
        }

        Test.ensureEqual(error, 
            "Two variables have the same destinationName=IB_time.", 
            "Unexpected error message:\n" + error);
    }

    /**
     * This tests sub-second time_precision in all output file types.
     *
     * @throws Throwable if trouble
     */
    public static void testTimePrecisionMillis() throws Throwable {
        String2.log("\n****************** EDDGridFromNcFiles.testTimePrecisionMillis() *****************\n");
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
        results = new String((new ByteArray(tDir + tName)).toArray());
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
        results = new String((new ByteArray(tDir + tName)).toArray());
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
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"<table class=\"erd commonBGColor\" cellspacing=\"0\">\n" +
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
"<td nowrap>1984-02-01T12:00:59.001Z\n" +
"<td align=\"right\">9.96921E36\n" +
"<td nowrap>1994-01-31T12:00:59.000Z\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap>1984-02-01T12:00:59.101Z\n" +
"<td align=\"right\">9.96921E36\n" +
"<td nowrap>1994-01-31T12:00:59.100Z\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap>1984-02-01T12:00:59.201Z\n" +
"<td align=\"right\">9.96921E36\n" +
"<td nowrap>1994-01-31T12:00:59.200Z\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap>1984-02-01T12:00:59.301Z\n" +
"<td align=\"right\">9.96921E36\n" +
"<td nowrap>1994-01-31T12:00:59.300Z\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap>1984-02-01T12:00:59.401Z\n" +
"<td align=\"right\">9.96921E36\n" +
"<td nowrap>1994-01-31T12:00:59.400Z\n" +
"</tr>\n" +
"</table>\n";
        po = results.indexOf("<table class");
        ts = results.substring(Math.max(0, po), Math.min(results.length(), po + expected.length())); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

        //.json  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".json"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
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
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"zztop\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
        */

        //.xhtml  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".xhtml"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"<table border=\"1\" cellpadding=\"2\" cellspacing=\"0\">\n" +
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
"<td nowrap=\"nowrap\">1984-02-01T12:00:59.001Z</td>\n" +
"<td align=\"right\">9.96921E36</td>\n" +
"<td nowrap=\"nowrap\">1994-01-31T12:00:59.000Z</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\">1984-02-01T12:00:59.101Z</td>\n" +
"<td align=\"right\">9.96921E36</td>\n" +
"<td nowrap=\"nowrap\">1994-01-31T12:00:59.100Z</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\">1984-02-01T12:00:59.201Z</td>\n" +
"<td align=\"right\">9.96921E36</td>\n" +
"<td nowrap=\"nowrap\">1994-01-31T12:00:59.200Z</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\">1984-02-01T12:00:59.301Z</td>\n" +
"<td align=\"right\">9.96921E36</td>\n" +
"<td nowrap=\"nowrap\">1994-01-31T12:00:59.300Z</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\">1984-02-01T12:00:59.401Z</td>\n" +
"<td align=\"right\">9.96921E36</td>\n" +
"<td nowrap=\"nowrap\">1994-01-31T12:00:59.400Z</td>\n" +
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
        String2.log("\n****************** EDDGridFromNcFiles.testSimpleTestNc() *****************\n");
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
        results = new String((new ByteArray(tDir + tName)).toArray());
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
        results = new String((new ByteArray(tDir + tName)).toArray());
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
        results = new String((new ByteArray(tDir + tName)).toArray());
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
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"<table class=\"erd commonBGColor\" cellspacing=\"0\">\n" +
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
"<th>&nbsp;\n" +
"<th>&nbsp;\n" +
"<th>&nbsp;\n" +
"<th>&nbsp;\n" +
"<th>&nbsp;\n" +
"<th>&nbsp;\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap>1970-01-03\n" +
"<td nowrap>1980-01-01T06Z\n" +
"<td nowrap>1990-01-01T00:10Z\n" +
"<td nowrap>2000-01-01T00:00:21Z\n" +
"<td nowrap>2010-01-01T00:00:00.031Z\n" +
"<td align=\"right\">41\n" +
"<td align=\"right\">10001\n" +
"<td align=\"right\">1000001\n" +
"<td align=\"right\">1.1\n" +
"<td align=\"right\">1.0000000000001E12\n" +
"<td nowrap>10\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap>1970-01-04\n" +
"<td nowrap>1980-01-01T07Z\n" +
"<td nowrap>1990-01-01T00:11Z\n" +
"<td nowrap>2000-01-01T00:00:22Z\n" +
"<td nowrap>2010-01-01T00:00:00.032Z\n" +
"<td align=\"right\">42\n" +
"<td align=\"right\">10002\n" +
"<td align=\"right\">1000002\n" +
"<td align=\"right\">2.2\n" +
"<td align=\"right\">1.0000000000002E12\n" +
"<td nowrap>20\n" +
"</tr>\n" +
"</table>\n";
        po = results.indexOf("<table class");
        ts = results.substring(Math.max(0, po), Math.min(results.length(), po + expected.length())); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

        //.json  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".json"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
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
"      :ioos_category = \"Unknown\";\n" +
"\n" +
"  // global attributes:\n" +
"  :cdm_data_type = \"Grid\";\n" +
"  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"  :history = \"";  //2014-10-22T16:16:21Z (local files)\n";
        ts = results.substring(0, expected.length()); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

expected = 
//"2014-10-22T16:16:21Z http://127.0.0.1:8080/cwexperimental
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
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"zztop\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
        */

        //.xhtml  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".xhtml"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
"  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
"<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
"<head>\n" +
"  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n" +
"  <title>testSimpleTestNc</title>\n" +
"</head>\n" +
"<body style=\"color:black; background:white; font-family:Arial,Helvetica,sans-serif; font-size:85%; line-height:130%;\">\n" +
"\n" +
"&nbsp;\n" +
"<table border=\"1\" cellpadding=\"2\" cellspacing=\"0\">\n" +
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
"<td nowrap=\"nowrap\">1970-01-03T00:00:00Z</td>\n" +
"<td nowrap=\"nowrap\">1980-01-01T06:00:00Z</td>\n" +
"<td nowrap=\"nowrap\">1990-01-01T00:10:00Z</td>\n" +
"<td nowrap=\"nowrap\">2000-01-01T00:00:21Z</td>\n" +
"<td nowrap=\"nowrap\">2010-01-01T00:00:00.031Z</td>\n" +
"<td align=\"right\">41</td>\n" +
"<td align=\"right\">10001</td>\n" +
"<td align=\"right\">1000001</td>\n" +
"<td align=\"right\">1.1</td>\n" +
"<td align=\"right\">1.0000000000001E12</td>\n" +
"<td nowrap=\"nowrap\">10</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\">1970-01-04T00:00:00Z</td>\n" +
"<td nowrap=\"nowrap\">1980-01-01T07:00:00Z</td>\n" +
"<td nowrap=\"nowrap\">1990-01-01T00:11:00Z</td>\n" +
"<td nowrap=\"nowrap\">2000-01-01T00:00:22Z</td>\n" +
"<td nowrap=\"nowrap\">2010-01-01T00:00:00.032Z</td>\n" +
"<td align=\"right\">42</td>\n" +
"<td align=\"right\">10002</td>\n" +
"<td align=\"right\">1000002</td>\n" +
"<td align=\"right\">2.2</td>\n" +
"<td align=\"right\">1.0000000000002E12</td>\n" +
"<td nowrap=\"nowrap\">20</td>\n" +
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
        String2.log("\n****************** EDDGridFromNcFiles.testSimpleTestNc2() *****************\n");
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
        results = new String((new ByteArray(tDir + tName)).toArray());
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
        results = new String((new ByteArray(tDir + tName)).toArray());
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
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"<table class=\"erd commonBGColor\" cellspacing=\"0\">\n" +
"<tr>\n" +
"<th>days\n" +
"<th>bytes\n" +
"<th>doubles\n" +
"<th>Strings\n" +
"</tr>\n" +
"<tr>\n" +
"<th>UTC\n" +
"<th>&nbsp;\n" +
"<th>&nbsp;\n" +
"<th>&nbsp;\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap>1970-01-04\n" +
"<td align=\"right\">42\n" +
"<td align=\"right\">1.0000000000002E12\n" +
"<td nowrap>20\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap>1970-01-05\n" +
"<td align=\"right\">43\n" +
"<td align=\"right\">1.0000000000003E12\n" +
"<td nowrap>30\n" +
"</tr>\n" +
"</table>\n";
        po = results.indexOf("<table class");
        ts = results.substring(Math.max(0, po), Math.min(results.length(), po + expected.length())); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

        //.json  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".json"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
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
"      :ioos_category = \"Unknown\";\n" +
"\n" +
"  // global attributes:\n" +
"  :cdm_data_type = \"Grid\";\n" +
"  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"  :history = \""; //2014-10-22T16:16:21Z (local files)\n";
        ts = results.substring(0, expected.length()); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

expected = 
//"2014-10-22T16:16:21Z http://127.0.0.1:8080/cwexperimental
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
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"zztop\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
        */

        //.xhtml  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".xhtml"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
"  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
"<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
"<head>\n" +
"  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n" +
"  <title>testSimpleTestNc2</title>\n" +
"</head>\n" +
"<body style=\"color:black; background:white; font-family:Arial,Helvetica,sans-serif; font-size:85%; line-height:130%;\">\n" +
"\n" +
"&nbsp;\n" +
"<table border=\"1\" cellpadding=\"2\" cellspacing=\"0\">\n" +
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
"<td nowrap=\"nowrap\">1970-01-04T00:00:00Z</td>\n" +
"<td align=\"right\">42</td>\n" +
"<td align=\"right\">1.0000000000002E12</td>\n" +
"<td nowrap=\"nowrap\">20</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\">1970-01-05T00:00:00Z</td>\n" +
"<td align=\"right\">43</td>\n" +
"<td align=\"right\">1.0000000000003E12</td>\n" +
"<td nowrap=\"nowrap\">30</td>\n" +
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
        Test.ensureEqual(results, expected, results.length() + " " + expected.length() + 
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
        String2.log("\n****************** EDDGridFromNcFiles.testUpdate() *****************\n");
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
        results = new String((new ByteArray(tDir + tName)).toArray());
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
        results = new String((new ByteArray(tDir + tName)).toArray());
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
            results = new String((new ByteArray(tDir + tName)).toArray());
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
            results = new String((new ByteArray(tDir + tName)).toArray());
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
        results = new String((new ByteArray(tDir + tName)).toArray());
        Test.ensureEqual(results, originalExpectedAxis, "\nresults=\n" + results);

        tName = eddGrid.makeNewFileForDapQuery(null, null, dataQuery, tDir, 
            eddGrid.className() + "_update_3d", ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
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
            results = new String((new ByteArray(tDir + tName)).toArray());
            Test.ensureEqual(results, originalExpectedAxis, "\nresults=\n" + results);

            tName = eddGrid.makeNewFileForDapQuery(null, null, dataQuery, tDir, 
                eddGrid.className() + "_update_4d", ".csv"); 
            results = new String((new ByteArray(tDir + tName)).toArray());
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
        results = new String((new ByteArray(tDir + tName)).toArray());
        Test.ensureEqual(results, originalExpectedAxis, "\nresults=\n" + results);

        tName = eddGrid.makeNewFileForDapQuery(null, null, dataQuery, tDir, 
            eddGrid.className() + "_update_5d", ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
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
        String2.log("\n****************** EDDGridFromNcFiles.testQuickRestart() *****************\n");
        EDDGridFromNcFiles eddGrid = (EDDGridFromNcFiles)oneFromDatasetsXml(null, "testGriddedNcFiles"); 
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
"    String creator_url \"http://coastwatch.pfel.noaa.gov\";\n" +
"    String date_created \"2008-08-29Z\";\n" +
"    String date_issued \"2008-08-29Z\";\n" +
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
"http://127.0.0.1:8080/cwexperimental/griddap/testGriddedNcFiles.das\";\n" +
"    String infoUrl \"http://coastwatch.pfel.noaa.gov/infog/QS_ux10_las.html\";\n" +
"    String institution \"NOAA CoastWatch, West Coast Node\";\n" +
"    String keywords \"EARTH SCIENCE > Oceans > Ocean Winds > Surface Winds\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.\";\n" +
"    String naming_authority \"gov.noaa.pfel.coastwatch\";\n" +
"    Float64 Northernmost_Northing 89.875;\n" +
"    String origin \"Remote Sensing Systems, Inc\";\n" +
"    String processing_level \"3\";\n" +
"    String project \"CoastWatch (http://coastwatch.noaa.gov/)\";\n" +
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
        results = new String((new ByteArray(tDir + tName)).toArray());
        Test.ensureEqual(results.substring(0, originalDas1.length()), originalDas1, "\nresults=\n" + results);

        po = results.indexOf(originalDas2.substring(0, 80));
        Test.ensureEqual(results.substring(po), originalDas2, "\nresults=\n" + results);

        tName = eddGrid.makeNewFileForDapQuery(null, null, axisQuery, tDir, 
            eddGrid.className() + "_qr_1a", ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        Test.ensureEqual(results, originalExpectedAxis, "\nresults=\n" + results);

        tName = eddGrid.makeNewFileForDapQuery(null, null, dataQuery, tDir, 
            eddGrid.className() + "_qr_1d", ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
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
            eddGrid = (EDDGridFromNcFiles)oneFromDatasetsXml(null, "testGriddedNcFiles"); 

            //some responses are same
            Test.ensureEqual(eddGrid.creationTimeMillis(), oCreationTimeMillis, "");

            tName = eddGrid.makeNewFileForDapQuery(null, null, "", tDir, 
                eddGrid.className() + "_qr_2das", ".das"); 
            results = new String((new ByteArray(tDir + tName)).toArray());
            Test.ensureEqual(results.substring(0, originalDas1.length()), originalDas1, "\nresults=\n" + results);

            po = results.indexOf(originalDas2.substring(0, 80));
            Test.ensureEqual(results.substring(po), originalDas2, "\nresults=\n" + results);

            tName = eddGrid.makeNewFileForDapQuery(null, null, axisQuery, tDir, 
                eddGrid.className() + "_qr_2a", ".csv"); 
            results = new String((new ByteArray(tDir + tName)).toArray());
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
            //rename it back to original
            String2.log("\n*** rename it back to original\n");       
            File2.rename(dataDir, "erdQSwind1day_20080101_03.nc2", "erdQSwind1day_20080101_03.nc");
            Math2.sleep(1000);
            //ensure testQuickRestart is set back to false
            EDDGridFromFiles.testQuickRestart = false;
        }

        //*** redo original tests
        String2.log("\n*** redo original tests\n");       
        eddGrid = (EDDGridFromNcFiles)oneFromDatasetsXml(null, "testGriddedNcFiles"); 

        //creationTime should have changed
        Test.ensureNotEqual(eddGrid.creationTimeMillis(), oCreationTimeMillis, "");

        //but everything else should be back to original
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", tDir, 
            eddGrid.className() + "_qr_3das", ".das"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        Test.ensureEqual(results.substring(0, originalDas1.length()), originalDas1, "\nresults=\n" + results);

        po = results.indexOf(originalDas2.substring(0, 80));
        Test.ensureEqual(results.substring(po), originalDas2, "\nresults=\n" + results);

        tName = eddGrid.makeNewFileForDapQuery(null, null, axisQuery, tDir, 
            eddGrid.className() + "_qr_3a", ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        Test.ensureEqual(results, originalExpectedAxis, "\nresults=\n" + results);

        tName = eddGrid.makeNewFileForDapQuery(null, null, dataQuery, tDir, 
            eddGrid.className() + "_qr_3d", ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
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
        String2.log("\n****** EDDGridFromNcFiles.testGenerateDatasetsXmlWithRemoteHyraxFiles() *****************\n");
        testVerboseOn();
        String results, expected;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
        String dir = "http://podaac-opendap.jpl.nasa.gov/opendap/hyrax/allData/avhrr/L4/reynolds_er/v3b/monthly/netcdf/2014/";

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
        String2.log("\n****** EDDGridFromNcFiles.testRemoteHyraxFiles(" + deleteCachedInfo + 
            ") *****************\n");
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
" http://127.0.0.1:8080/cwexperimental/griddap/testCwHdf.das\";\n" +
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
        String2.log("\n****** EDDGridFromNcFiles.testGenerateDatasetsXmlWithRemoteThreddsFiles() *****************\n");
        testVerboseOn();
        String results, expected;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
        String dir = "http://data.nodc.noaa.gov/thredds/catalog/aquarius/nodc_binned_V3.0/"; //catalog.html

        //dir  is a /thredds/catalog/.../  [implied catalog.html] URL!
        //file is a /thredds/fileServer/... not compressed data file.
        results = generateDatasetsXml( 
            dir, 
            "sss_binned_L3_MON_SCI_V3.0_\\d{4}\\.nc", 
            //sample file is a thredds/fileServer/.../...nc URL!
            "http://data.nodc.noaa.gov/thredds/fileServer/aquarius/nodc_binned_V3.0/monthly/sss_binned_L3_MON_SCI_V3.0_2011.nc", 
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
"   http://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html .\n" +
" * Read http://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html#addAttributes\n" +
"   so that you understand about sourceAttributes and addAttributes.\n" +
" * Note: Global sourceAttributes and variable sourceAttributes are listed\n" +
"   below as comments, for informational purposes only.\n" +
"   ERDDAP combines sourceAttributes and addAttributes (which have\n" +
"   precedence) to make the combinedAttributes that are shown to the user.\n" +
"   (And other attributes are automatically added to longitude, latitude,\n" +
"   altitude, depth, and time variables).\n" +
" * If you don't like a sourceAttribute, override it by adding an\n" +
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
"    <fileDir>http://data.nodc.noaa.gov/thredds/catalog/aquarius/nodc_binned_V3.0/</fileDir>\n" +
"    <fileNameRegex>sss_binned_L3_MON_SCI_V3.0_\\d{4}\\.nc</fileNameRegex>\n" +
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
"        <att name=\"creator_url\">http://www.nodc.noaa.gov/SatelliteData</att>\n" +
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
"        <att name=\"history\">Aquarius Level-2 SCI CAP V3.0</att>\n" +
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
"        <att name=\"summary\">This dataset is created by NODC Satellite Oceanography Group from Aquarius level-2 SCI V3.0 data,using 1.0x1.0 (lon/lat) degree box average</att>\n" +
"        <att name=\"title\">Gridded monthly mean Sea Surface Salinity calculated from Aquarius level-2 SCI V3.0 data</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"Conventions\">CF-1.6, COARDS, ACDD-1.3</att>\n" +
"        <att name=\"infoUrl\">http://data.nodc.noaa.gov/thredds/catalog/aquarius/nodc_binned_V3.0/catalog.html</att>\n" +
"        <att name=\"institution\">JPL, California Institute of Technology</att>\n" +
"        <att name=\"keywords\">aquarius, calculate, calculated, california, center, data, earth,\n" +
"Earth Science &gt;Oceans &gt; Surface Salinity,\n" +
"gridded, institute, jet, jpl, laboratory, level, level-2, mean, month, monthly, national, ncei, noaa, nodc, number, observation, observations, ocean, oceanographic, oceans, propulsion, salinity, sci, science, sea, sea_surface_salinity, sea_surface_salinity_number_of_observations, sss, sss_obs, statistics, surface, swath, technology, time, used, v3.0, valid</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"Metadata_Conventions\">null</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v29</att>\n" +
"        <att name=\"summary\">This dataset is created by National Oceanographic Data Center (NODC) Satellite Oceanography Group from Aquarius level-2 SCI V3.0 data,using 1.0x1.0 (lon/lat) degree box average</att>\n" +
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
   
    }

    /**
     * This tests if netcdf-java can work with remote files
     * and thus if a dataset can be made from remote files.
     *
     * @throws Throwable if trouble
     */
    public static void testRemoteThreddsFiles(boolean deleteCachedInfo) throws Throwable {
        String2.log("\n****** EDDGridFromNcFiles.testRemoteThreddsFiles(" + deleteCachedInfo + 
            ") *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        int po;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
        String id = "testRemoteThreddsFiles";  //from generateDatasetsXml above but different datasetID
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, id);

        //*** test getting das for entire dataset
        String2.log("\n*** test das dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_trtf", ".das"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
"    String creator_url \"http://www.nodc.noaa.gov/SatelliteData\";\n" +
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
"    String history \"Aquarius Level-2 SCI CAP V3.0\n" +
today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);
        
//+ " (local files)\n" +
//today + 
    
expected =
"Z http://127.0.0.1:8080/cwexperimental/griddap/testRemoteThreddsFiles.das\";\n" +
"    String infoUrl \"http://data.nodc.noaa.gov/thredds/catalog/aquarius/nodc_binned_V3.0/catalog.html\";\n" +
"    String institution \"JPL, California Institute of Technology\";\n" +
"    String keywords \"aquarius, calculate, calculated, california, center, data, earth,\n" +
"Earth Science >Oceans > Surface Salinity,\n" +
"gridded, institute, jet, jpl, laboratory, level, level-2, mean, month, monthly, national, ncei, noaa, nodc, number, observation, observations, ocean, oceanographic, oceans, propulsion, salinity, sci, science, sea, sea_surface_salinity, sea_surface_salinity_number_of_observations, sss, sss_obs, statistics, surface, swath, technology, time, used, v3.0, valid\";\n" +
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
"    String summary \"This dataset is created by National Oceanographic Data Center (NODC) Satellite Oceanography Group from Aquarius level-2 SCI V3.0 data,using 1.0x1.0 (lon/lat) degree box average\";\n" +
"    String time_coverage_end \"2015-03-15T00:00:00Z\";\n" +
"    String time_coverage_start \"2011-08-15T00:00:00Z\";\n" +
"    String title \"Gridded monthly mean Sea Surface Salinity calculated from Aquarius level-2 SCI V3.0 data\";\n" +
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
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
//2015-10-30T18:17:10Z http://127.0.0.1:8080/cwexperimental/griddap/testUInt16File.das";
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
"} testUInt16File;\n";
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
        String2.log("\n************* EDDGridFromNcFiles.testSpecialAxis0Time() ************\n");
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
        results = new String((new ByteArray(tDir + tName)).toArray());
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
"    Float32 actual_range 89.95834, -89.95834;\n" +
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
"    String reference \"Hu, C., Lee Z., and Franz, B.A. (2012). Chlorophyll-a algorithms for oligotrophic oceans: A novel approach based on three-band reflectance difference, J. Geophys. Res., 117, C01011, doi:10.1029/2011JC007395.\";\n" +
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
"    String creator_url \"http://oceandata.sci.gsfc.nasa.gov\";\n" +
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

//            + " http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/ux10/1day\n" +
//today + 

expected = 
//    "    String id "S19980011998031.L3b_MO_CHL.nc/L3/S19980011998031.L3b_MO_CHL.nc";
"    String identifier_product_doi \"http://dx.doi.org\";\n" +
"    String identifier_product_doi_authority \"http://dx.doi.org\";\n" +
"    String infoUrl \"http://oceandata.sci.gsfc.nasa.gov\";\n" +
"    String institution \"NASA/GSFC OBPG\";\n" +
"    String instrument \"SeaWiFS\";\n" +
"    String keywords \"algorithm, biology, center, chemistry, chlor_a, chlorophyll, color, concentration, concentration_of_chlorophyll_in_sea_water, data, field, field-of-view, flight, goddard, group, gsfc, image, L3, level, level-3, mapped, nasa, noaa, obpg, ocean, ocean color, oceans,\n" +
"Oceans > Ocean Chemistry > Chlorophyll,\n" +
"Oceans > Ocean Chemistry > Chlorophyll,\n" +
"Oceans > Ocean Optics > Ocean Color,\n" +
"oci, optics, orbview, orbview-2, palette, processing, sea, sea-wide, seawater, seawifs, sensor, smi, space, standard, view, water, wide\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String l2_flag_names \"ATMFAIL,LAND,HILT,HISATZEN,STRAYLIGHT,CLDICE,COCCOLITH,LOWLW,CHLWARN,CHLFAIL,NAVWARN,MAXAERITER,ATMWARN,HISOLZEN,NAVFAIL,FILTER,HIGLINT\";\n" +
"    String license \"http://science.nasa.gov/earth-science/earth-science-data/data-information-policy/\n" +
"\n" +
"Please cite: NASA Goddard Space Flight Center, Ocean Ecology Laboratory, Ocean Biology Processing Group; (2014): SeaWiFS Ocean Color Data; NASA Goddard Space Flight Center, Ocean Ecology Laboratory, Ocean Biology Processing Group. http://dx.doi.org/10.5067/ORBVIEW-2/SEAWIFS_OC.2014.0\n" +
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
"    String publisher_url \"http://oceandata.sci.gsfc.nasa.gov\";\n" +
"    String references \"SeaWiFS information: http://oceancolor.gsfc.nasa.gov/SeaWiFS/ . NASA Ocean\n" +
"Color information: http://oceancolor.gsfc.nasa.gov/\n" +
"Processing reference: O'Reilly, J.E., Maritorena, S., Mitchell, B.G., Siegel, D.A., Carder, K.L., Garver, S.A., Kahru, M. and McClain, C. (1998). Ocean color chlorophyll algorithms for SeaWiFS. J. Geophys. Res., 103: 24, 937-24, 953.\n" +
"Processing reference: O'Reilly, J. E., and 21 others. 2000. Ocean color chlorophyll a algorithms for SeaWiFS, OC2 and OC4: Version 4. SeaWiFS Postlaunch Calibration and Validation Analyses, part 3. NASA SeaWiFS technical report series. pp. 8 226 22.\n" +
"Processing reference: Fu, G., Baith, K. S., and McClain, C. R. (1998). SeaDAS: The SeaWiFS Data Analysis System. Proceedings of \\\\\"The 4th Pacific Ocean Remote Sensing Conference\\\\\", Qingdao, China, July 28-31, 1998, 73-79.\n" +
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
"    String title \"Chlorophyll-a, Orbview-2 SeaWiFS, R2014.0, 0.1°, Global (Monthly Composite)\";\n" +
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
        results = new String((new ByteArray(tDir + tName)).toArray());
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
        results = new String((new ByteArray(tDir + tName)).toArray());
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
        String2.log("\n************* EDDGridFromNcFiles.testSpecialAxis0FileNameInt() ************\n");
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
        results = new String((new ByteArray(tDir + tName)).toArray());
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
"    Float32 actual_range 89.95834, -89.95834;\n" +
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
"    String reference \"Hu, C., Lee Z., and Franz, B.A. (2012). Chlorophyll-a algorithms for oligotrophic oceans: A novel approach based on three-band reflectance difference, J. Geophys. Res., 117, C01011, doi:10.1029/2011JC007395.\";\n" +
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
"    String creator_url \"http://oceandata.sci.gsfc.nasa.gov\";\n" +
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

//            + " http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/ux10/1day\n" +
//today + 

expected = 
//    "    String id "S19980011998031.L3b_MO_CHL.nc/L3/S19980011998031.L3b_MO_CHL.nc";
"    String identifier_product_doi \"http://dx.doi.org\";\n" +
"    String identifier_product_doi_authority \"http://dx.doi.org\";\n" +
"    String infoUrl \"http://oceandata.sci.gsfc.nasa.gov\";\n" +
"    String institution \"NASA/GSFC OBPG\";\n" +
"    String instrument \"SeaWiFS\";\n" +
"    String keywords \"algorithm, biology\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String l2_flag_names \"ATMFAIL,LAND,HILT,HISATZEN,STRAYLIGHT,CLDICE,COCCOLITH,LOWLW,CHLWARN,CHLFAIL,NAVWARN,MAXAERITER,ATMWARN,HISOLZEN,NAVFAIL,FILTER,HIGLINT\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
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
"    String publisher_url \"http://oceandata.sci.gsfc.nasa.gov\";\n" +
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
        results = new String((new ByteArray(tDir + tName)).toArray());
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
        results = new String((new ByteArray(tDir + tName)).toArray());
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
        results = new String((new ByteArray(tDir + tName)).toArray());
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
        String2.log("\n************* EDDGridFromNcFiles.testSpecialAxis0GlobalDouble() ************\n");
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
        results = new String((new ByteArray(tDir + tName)).toArray());
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
"    Float32 actual_range 89.95834, -89.95834;\n" +
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
"    String reference \"Hu, C., Lee Z., and Franz, B.A. (2012). Chlorophyll-a algorithms for oligotrophic oceans: A novel approach based on three-band reflectance difference, J. Geophys. Res., 117, C01011, doi:10.1029/2011JC007395.\";\n" +
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
"    String creator_url \"http://oceandata.sci.gsfc.nasa.gov\";\n" +
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

//            + " http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/ux10/1day\n" +
//today + 

expected = 
//    "    String id "S19980011998031.L3b_MO_CHL.nc/L3/S19980011998031.L3b_MO_CHL.nc";
"    String identifier_product_doi \"http://dx.doi.org\";\n" +
"    String identifier_product_doi_authority \"http://dx.doi.org\";\n" +
"    String infoUrl \"http://oceandata.sci.gsfc.nasa.gov\";\n" +
"    String institution \"NASA/GSFC OBPG\";\n" +
"    String instrument \"SeaWiFS\";\n" +
"    String keywords \"algorithm, biology\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String l2_flag_names \"ATMFAIL,LAND,HILT,HISATZEN,STRAYLIGHT,CLDICE,COCCOLITH,LOWLW,CHLWARN,CHLFAIL,NAVWARN,MAXAERITER,ATMWARN,HISOLZEN,NAVFAIL,FILTER,HIGLINT\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
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
"    String publisher_url \"http://oceandata.sci.gsfc.nasa.gov\";\n" +
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
        results = new String((new ByteArray(tDir + tName)).toArray());
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
        results = new String((new ByteArray(tDir + tName)).toArray());
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
        results = new String((new ByteArray(tDir + tName)).toArray());
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
     * This tests this class.
     *
     * @throws Throwable if trouble
     */
    public static void test(boolean deleteCachedDatasetInfo) throws Throwable {
/* */
        testNc(deleteCachedDatasetInfo);
        testCwHdf(deleteCachedDatasetInfo);
        testHdf();
        testNcml();
        testGrib_43(deleteCachedDatasetInfo);  //42 or 43 for netcdfAll 4.2- or 4.3+
        testGrib2_43(deleteCachedDatasetInfo); //42 or 43 for netcdfAll 4.2- or 4.3+
        testGenerateDatasetsXml();
        testGenerateDatasetsXml2();
        testSpeed(-1);  //-1 = all
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

        //tests of remote sources on-the-fly
        testGenerateDatasetsXmlAwsS3();
        testAwsS3(false);  //deleteCachedInfo
        testGenerateDatasetsXmlWithRemoteThreddsFiles();
        testRemoteThreddsFiles(false); //deleteCachedInfo 
        testMatchAxisNDigits();

        /* */

        //NOT FINISHED
        //none

        //Remote Hyrax
        //  These were tests of reading remote data files on Hyrax without downloading,
        //  but test Hyrax server doesn't support Byte Ranges.
        //testGenerateDatasetsXmlWithRemoteHyraxFiles();  //Test server doesn't support Byte Ranges.
        //testRemoteHyraxFiles(false); //deleteCachedInfo //Test server doesn't support Byte Ranges.

        //one time tests
        //String fiName = "/erddapTestBig/geosgrib/multi_1.glo_30m.all.grb2";
        //String2.log(NcHelper.dumpString(fiName, false));
        //NetcdfDataset in = NetcdfDataset.openDataset(fiName);
        //in.close();

    }


}

