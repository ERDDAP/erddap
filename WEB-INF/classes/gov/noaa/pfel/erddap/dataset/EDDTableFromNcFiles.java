/* 
 * EDDTableFromNcFiles Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.CharArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
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

import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.sgt.SgtUtil;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.Tally;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

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
 * This class represents a table of data from a collection of n-dimensional (1,2,3,4,...) .nc data files.
 * The dimensions are e.g., time,depth,lat,lon.
 * <br>A given file may have multiple values for each of the dimensions 
 *   and the values may be different in different files.
 * <br>[Was: only the leftmost dimension (e.g., time) could have multiple values.]
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2009-02-13
 */
public class EDDTableFromNcFiles extends EDDTableFromFiles { 


    /** 
     * The constructor just calls the super constructor. 
     *
     * <p>The sortedColumnSourceName can't be for a char/String variable
     *   because NcHelper binary searches are currently set up for numeric vars only.
     *
     * @param tAccessibleTo is a comma separated list of 0 or more
     *    roles which will have access to this dataset.
     *    <br>If null, everyone will have access to this dataset (even if not logged in).
     *    <br>If "", no one will have access to this dataset.
     * @param tFgdcFile This should be the fullname of a file with the FGDC
     *    that should be used for this dataset, or "" (to cause ERDDAP not
     *    to try to generate FGDC metadata for this dataset), or null (to allow
     *    ERDDAP to try to generate FGDC metadata for this dataset).
     * @param tIso19115 This is like tFgdcFile, but for the ISO 19119-2/19139 metadata.
     */
    public EDDTableFromNcFiles(String tDatasetID, String tAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tSosOfferingPrefix,
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes, int tUpdateEveryNMillis,
        String tFileDir, String tFileNameRegex, boolean tRecursive, String tPathRegex, 
        String tMetadataFrom, String tCharset, int tColumnNamesRow, int tFirstDataRow,
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex, 
        String tColumnNameForExtract,
        String tSortedColumnSourceName, String tSortFilesBySourceNames,
        boolean tSourceNeedsExpandedFP_EQ, boolean tFileTableInMemory, 
        boolean tAccessibleViaFiles) 
        throws Throwable {

        super("EDDTableFromNcFiles", tDatasetID, tAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix, 
            tDefaultDataQuery, tDefaultGraphQuery,
            tAddGlobalAttributes, 
            tDataVariables, tReloadEveryNMinutes, tUpdateEveryNMillis,
            tFileDir, tFileNameRegex, tRecursive, tPathRegex, tMetadataFrom,
            tCharset, tColumnNamesRow, tFirstDataRow,
            tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
            tSortedColumnSourceName, tSortFilesBySourceNames,
            tSourceNeedsExpandedFP_EQ, tFileTableInMemory, tAccessibleViaFiles);

    }

    /** 
     * The constructor for subclasses.
     */
    public EDDTableFromNcFiles(String tClassName, 
        String tDatasetID, String tAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tSosOfferingPrefix,
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes, int tUpdateEveryNMillis,
        String tFileDir, String tFileNameRegex, boolean tRecursive, String tPathRegex, 
        String tMetadataFrom, String tCharset, int tColumnNamesRow, int tFirstDataRow,
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex, 
        String tColumnNameForExtract,
        String tSortedColumnSourceName, String tSortFilesBySourceNames,
        boolean tSourceNeedsExpandedFP_EQ, boolean tFileTableInMemory, 
        boolean tAccessibleViaFiles) 
        throws Throwable {

        super(tClassName, tDatasetID, tAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix, 
            tDefaultDataQuery, tDefaultGraphQuery,
            tAddGlobalAttributes, 
            tDataVariables, tReloadEveryNMinutes, tUpdateEveryNMillis,
            tFileDir, tFileNameRegex, tRecursive, tPathRegex, tMetadataFrom,
            tCharset, tColumnNamesRow, tFirstDataRow,
            tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
            tSortedColumnSourceName, tSortFilesBySourceNames,
            tSourceNeedsExpandedFP_EQ, tFileTableInMemory, tAccessibleViaFiles);

    }


    /**
     * This gets source data from one file.
     * See documentation in EDDTableFromFiles.
     *
     * @throws an exception if too much data.
     *  This won't throw an exception if no data.
     */
    public Table lowGetSourceDataFromFile(String fileDir, String fileName, 
        StringArray sourceDataNames, String sourceDataTypes[],
        double sortedSpacing, double minSorted, double maxSorted, 
        StringArray sourceConVars, StringArray sourceConOps, StringArray sourceConValues,
        boolean getMetadata, boolean mustGetData) 
        throws Throwable {

        //Future: more efficient if !mustGetData is handled differently

        //read the file
        Table table = new Table();
        table.readNDNc(fileDir + fileName, sourceDataNames.toArray(),
            sortedSpacing >= 0 && !Double.isNaN(minSorted)? sortedColumnSourceName : null,
                minSorted, maxSorted, 
            getMetadata);
        //String2.log("  EDDTableFromNcFiles.lowGetSourceDataFromFile table.nRows=" + table.nRows());
        //table.saveAsDDS(System.out, "s");

        return table;
    }


    /** 
     * This generates a ready-to-use datasets.xml entry for an EDDTableFromNcFiles.
     * The XML can then be edited by hand and added to the datasets.xml file.
     *
     * <p>This can't be made into a web service because it would allow any user
     * to looks at (possibly) private .nc files on the server.
     *
     * @param tFileDir the starting (parent) directory for searching for files
     * @param tFileNameRegex  the regex that each filename (no directory info) must match 
     *    (e.g., ".*\\.nc")  (usually only 1 backslash; 2 here since it is Java code). 
     *    If null or "", it is generated to catch the same extension as the sampleFileName
     *    (usually ".*\\.nc").
     * @param sampleFileName the full file name of one of the files in the collection
     * @param useDimensionsCSV If null or "", this finds the group of variables sharing the
     *    highest number of dimensions. Otherwise, it find the variables using
     *    these dimensions (plus related char variables).
     * @param tReloadEveryNMinutes  e.g., 10080 for weekly
     * @param tPreExtractRegex       part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tPostExtractRegex      part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tExtractRegex          part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tColumnNameForExtract  part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tSortedColumnSourceName   use "" if not known or not needed. 
     * @param tSortFilesBySourceNames   This is useful, because it ultimately determines default results order.
     * @param tInfoUrl       or "" if in externalAddGlobalAttributes or if not available
     * @param tInstitution   or "" if in externalAddGlobalAttributes or if not available
     * @param tSummary       or "" if in externalAddGlobalAttributes or if not available
     * @param tTitle         or "" if in externalAddGlobalAttributes or if not available
     * @param externalAddGlobalAttributes  These attributes are given priority.  Use null in none available.
     * @return a suggested chunk of xml for this dataset for use in datasets.xml 
     * @throws Throwable if trouble, e.g., if no Grid or Array variables are found.
     *    If no trouble, then a valid dataset.xml chunk has been returned.
     */
    public static String generateDatasetsXml(
        String tFileDir, String tFileNameRegex, String sampleFileName, 
        String useDimensionsCSV, int tReloadEveryNMinutes, 
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex,
        String tColumnNameForExtract, String tSortedColumnSourceName,
        String tSortFilesBySourceNames, 
        String tInfoUrl, String tInstitution, String tSummary, String tTitle,
        Attributes externalAddGlobalAttributes) throws Throwable {

        String2.log("EDDTableFromNcFiles.generateDatasetsXml" +
            "\n  sampleFileName=" + sampleFileName);

        if (!String2.isSomething(tFileDir))
            throw new IllegalArgumentException("fileDir wasn't specified.");
        tFileDir = File2.addSlash(tFileDir); //ensure it has trailing slash
        String[] useDimensions = StringArray.arrayFromCSV(useDimensionsCSV);
        if (tReloadEveryNMinutes <= 0 || tReloadEveryNMinutes == Integer.MAX_VALUE)
            tReloadEveryNMinutes = 1440; //1440 works well with suggestedUpdateEveryNMillis

        //show structure of sample file
        String2.log("Let's see if netcdf-java can tell us the structure of the sample file:");
        String2.log(NcHelper.dumpString(sampleFileName, false));

        //*** basically, make a table to hold the sourceAttributes 
        //and a parallel table to hold the addAttributes
        Table dataSourceTable = new Table();
        Table dataAddTable = new Table();

        //new way
        StringArray varNames = new StringArray();
        if (useDimensions.length > 0) {
            //find the varNames
            NetcdfFile ncFile = NcHelper.openFile(sampleFileName);
            try {

                Group rootGroup = ncFile.getRootGroup();
                List rootGroupVariables = rootGroup.getVariables(); 
                for (int v = 0; v < rootGroupVariables.size(); v++) {
                    Variable var = (Variable)rootGroupVariables.get(v);
                    boolean isChar = var.getDataType() == DataType.CHAR;
                    if (var.getRank() + (isChar? -1 : 0) == useDimensions.length) {
                        boolean matches = true;
                        for (int d = 0; d < useDimensions.length; d++) {
                            if (!var.getDimension(d).getFullName().equals(useDimensions[d])) {
                                matches = false;
                                break;
                            }
                        }
                        if (matches) 
                            varNames.add(var.getFullName());
                    }
                }
                ncFile.close(); 

            } catch (Exception e) {
                //make sure ncFile is explicitly closed
                try {
                    ncFile.close(); 
                } catch (Exception e2) {
                    //don't care
                }
                String2.log(MustBe.throwableToString(e)); 
            }
            Test.ensureTrue(varNames.size() > 0, 
                "The file has no variables with dimensions: " + useDimensionsCSV);
        }

        //then read the file
        dataSourceTable.readNDNc(sampleFileName, varNames.toStringArray(), 
            null, 0, 0, true); //getMetadata
        for (int c = 0; c < dataSourceTable.nColumns(); c++) {
            String colName = dataSourceTable.getColumnName(c);
            Attributes sourceAtts = dataSourceTable.columnAttributes(c);
            dataAddTable.addColumn(c, colName,
                dataSourceTable.getColumn(c),
                makeReadyToUseAddVariableAttributesForDatasetsXml(
                    dataSourceTable.globalAttributes(), sourceAtts, colName, 
                    true, true)); //addColorBarMinMax, tryToFindLLAT

            //if a variable has timeUnits, files are likely sorted by time
            //and no harm if files aren't sorted that way
            if (tSortedColumnSourceName.length() == 0 && 
                EDVTimeStamp.hasTimeUnits(sourceAtts, null))
                tSortedColumnSourceName = colName;
        }
        //String2.log("SOURCE COLUMN NAMES=" + dataSourceTable.getColumnNamesCSSVString());
        //String2.log("DEST   COLUMN NAMES=" + dataSourceTable.getColumnNamesCSSVString());

        //globalAttributes
        if (externalAddGlobalAttributes == null)
            externalAddGlobalAttributes = new Attributes();
        if (tInfoUrl     != null && tInfoUrl.length()     > 0) externalAddGlobalAttributes.add("infoUrl",     tInfoUrl);
        if (tInstitution != null && tInstitution.length() > 0) externalAddGlobalAttributes.add("institution", tInstitution);
        if (tSummary     != null && tSummary.length()     > 0) externalAddGlobalAttributes.add("summary",     tSummary);
        if (tTitle       != null && tTitle.length()       > 0) externalAddGlobalAttributes.add("title",       tTitle);
        externalAddGlobalAttributes.setIfNotAlreadySet("sourceUrl", 
            "(" + (String2.isRemote(tFileDir)? "remote" : "local") + " files)");
        //externalAddGlobalAttributes.setIfNotAlreadySet("subsetVariables", "???");
        //after dataVariables known, add global attributes in the dataAddTable
        dataAddTable.globalAttributes().set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(), 
                //another cdm_data_type could be better; this is ok
                probablyHasLonLatTime(dataSourceTable, dataAddTable)? "Point" : "Other",
                tFileDir, externalAddGlobalAttributes, 
                suggestKeywords(dataSourceTable, dataAddTable)));

        //add the columnNameForExtract variable
        if (tColumnNameForExtract.length() > 0) {
            Attributes atts = new Attributes();
            atts.add("ioos_category", "Identifier");
            atts.add("long_name", EDV.suggestLongName(null, tColumnNameForExtract, null));
            //no units or standard_name
            dataSourceTable.addColumn(0, tColumnNameForExtract, new StringArray(), new Attributes());
            dataAddTable.addColumn(   0, tColumnNameForExtract, new StringArray(), atts);
        }

        //write the information
        StringBuilder sb = new StringBuilder();
        String suggestedRegex = (tFileNameRegex == null || tFileNameRegex.length() == 0)? 
            ".*\\" + File2.getExtension(sampleFileName) :
            tFileNameRegex;
        if (tSortFilesBySourceNames.length() == 0)
            tSortFilesBySourceNames = (tColumnNameForExtract + 
                (tSortedColumnSourceName.length() == 0? "" : " " + tSortedColumnSourceName)).trim();
        sb.append(
            directionsForGenerateDatasetsXml() +
            "-->\n\n" +
            "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"" + 
                suggestDatasetID(tFileDir + suggestedRegex) +  //dirs can't be made public
                "\" active=\"true\">\n" +
            "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n" +  
            "    <updateEveryNMillis>" + suggestUpdateEveryNMillis(tFileDir) + 
            "</updateEveryNMillis>\n" +  
            "    <fileDir>" + XML.encodeAsXML(tFileDir) + "</fileDir>\n" +
            "    <fileNameRegex>" + XML.encodeAsXML(suggestedRegex) + "</fileNameRegex>\n" +
            "    <recursive>true</recursive>\n" +
            "    <pathRegex>.*</pathRegex>\n" +
            "    <metadataFrom>last</metadataFrom>\n" +
            "    <preExtractRegex>" + XML.encodeAsXML(tPreExtractRegex) + "</preExtractRegex>\n" +
            "    <postExtractRegex>" + XML.encodeAsXML(tPostExtractRegex) + "</postExtractRegex>\n" +
            "    <extractRegex>" + XML.encodeAsXML(tExtractRegex) + "</extractRegex>\n" +
            "    <columnNameForExtract>" + XML.encodeAsXML(tColumnNameForExtract) + "</columnNameForExtract>\n" +
            "    <sortedColumnSourceName>" + XML.encodeAsXML(tSortedColumnSourceName) + "</sortedColumnSourceName>\n" +
            "    <sortFilesBySourceNames>" + XML.encodeAsXML(tSortFilesBySourceNames) + "</sortFilesBySourceNames>\n" +
            "    <fileTableInMemory>false</fileTableInMemory>\n" +
            "    <accessibleViaFiles>false</accessibleViaFiles>\n");
        sb.append(writeAttsForDatasetsXml(false, dataSourceTable.globalAttributes(), "    "));
        sb.append(cdmSuggestion());
        sb.append(writeAttsForDatasetsXml(true,     dataAddTable.globalAttributes(), "    "));

        //last 3 params: includeDataType, tryToFindLLAT, questionDestinationName
        sb.append(writeVariablesForDatasetsXml(dataSourceTable, dataAddTable, 
            "dataVariable", true, true, false));
        sb.append(
            "</dataset>\n" +
            "\n");

        String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
        return sb.toString();
        
    }


    /**
     * testGenerateDatasetsXml
     */
    public static void testGenerateDatasetsXml() throws Throwable {
        testVerboseOn();

        try {
            String results = generateDatasetsXml(
                "C:/u00/data/points/ndbcMet", "",
                "C:/u00/data/points/ndbcMet/NDBC_41004_met.nc",
                "",
                1440,
                "^.{5}", ".{7}$", ".*", "stationID", //just for test purposes; station is already a column in the file
                "TIME", "stationID TIME", 
                "", "", "", "", null) + "\n";

            //GenerateDatasetsXml
            String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
                "EDDTableFromNcFiles",
                "C:/u00/data/points/ndbcMet", "",
                "C:/u00/data/points/ndbcMet/NDBC_41004_met.nc",
                "",
                "1440",
                "^.{5}", ".{7}$", ".*", "stationID", //just for test purposes; station is already a column in the file
                "TIME", "stationID TIME", 
                "", "", "", ""},
                false); //doIt loop?
            Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

String expected = 
directionsForGenerateDatasetsXml() +
"-->\n" +
"\n" +
"<dataset type=\"EDDTableFromNcFiles\" datasetID=\"ndbcMet_5df7_b363_ad99\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>C:/u00/data/points/ndbcMet/</fileDir>\n" +
"    <fileNameRegex>.*\\.nc</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <preExtractRegex>^.{5}</preExtractRegex>\n" +
"    <postExtractRegex>.{7}$</postExtractRegex>\n" +
"    <extractRegex>.*</extractRegex>\n" +
"    <columnNameForExtract>stationID</columnNameForExtract>\n" +
"    <sortedColumnSourceName>TIME</sortedColumnSourceName>\n" +
"    <sortFilesBySourceNames>stationID TIME</sortFilesBySourceNames>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <accessibleViaFiles>false</accessibleViaFiles>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"acknowledgement\">NOAA NDBC and NOAA CoastWatch (West Coast Node)</att>\n" +
"        <att name=\"cdm_data_type\">Station</att>\n" +
"        <att name=\"contributor_name\">NOAA NDBC and NOAA CoastWatch (West Coast Node)</att>\n" +
"        <att name=\"contributor_role\">Source of data.</att>\n" +
//2012-07-27 "Unidata Observation Dataset v1.0" should disappear soon
"        <att name=\"Conventions\">COARDS, CF-1.4, Unidata Dataset Discovery v1.0, Unidata Observation Dataset v1.0</att>\n" +
"        <att name=\"creator_email\">dave.foley@noaa.gov</att>\n" +
"        <att name=\"creator_name\">NOAA CoastWatch, West Coast Node</att>\n" +
"        <att name=\"creator_url\">http://coastwatch.pfeg.noaa.gov</att>\n" +
"        <att name=\"date_created\">2015-07-20Z</att>\n" + //changes
"        <att name=\"date_issued\">2015-07-20Z</att>\n" +  //changes
"        <att name=\"Easternmost_Easting\" type=\"float\">-79.099</att>\n" +
"        <att name=\"geospatial_lat_max\" type=\"float\">32.501</att>\n" +
"        <att name=\"geospatial_lat_min\" type=\"float\">32.501</att>\n" +
"        <att name=\"geospatial_lat_units\">degrees_north</att>\n" +
"        <att name=\"geospatial_lon_max\" type=\"float\">-79.099</att>\n" +
"        <att name=\"geospatial_lon_min\" type=\"float\">-79.099</att>\n" +
"        <att name=\"geospatial_lon_units\">degrees_east</att>\n" +
"        <att name=\"geospatial_vertical_max\" type=\"float\">0.0</att>\n" +
"        <att name=\"geospatial_vertical_min\" type=\"float\">0.0</att>\n" +
"        <att name=\"geospatial_vertical_positive\">down</att>\n" +
"        <att name=\"geospatial_vertical_units\">m</att>\n" +
"        <att name=\"history\">NOAA NDBC</att>\n" +
"        <att name=\"id\">NDBC_41004_met</att>\n" +
"        <att name=\"institution\">NOAA National Data Buoy Center and Participators in Data Assembly Center.</att>\n" +
"        <att name=\"keywords\">EARTH SCIENCE &gt; Oceans</att>\n" +
"        <att name=\"license\">The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither NOAA, NDBC, CoastWatch, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.</att>\n" +
"        <att name=\"Metadata_Conventions\">COARDS, CF-1.4, Unidata Dataset Discovery v1.0, Unidata Observation Dataset v1.0</att>\n" +
"        <att name=\"naming_authority\">gov.noaa.pfeg.coastwatch</att>\n" +
"        <att name=\"NDBCMeasurementDescriptionUrl\">http://www.ndbc.noaa.gov/measdes.shtml</att>\n" +
"        <att name=\"Northernmost_Northing\" type=\"float\">32.501</att>\n" +
"        <att name=\"project\">NOAA NDBC and NOAA CoastWatch (West Coast Node)</att>\n" +
"        <att name=\"quality\">Automated QC checks with periodic manual QC</att>\n" +
"        <att name=\"source\">station observation</att>\n" +
"        <att name=\"Southernmost_Northing\" type=\"float\">32.501</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF-12</att>\n" +
"        <att name=\"summary\">The National Data Buoy Center (NDBC) distributes meteorological data from moored buoys maintained by NDBC and others. Moored buoys are the weather sentinels of the sea. They are deployed in the coastal and offshore waters from the western Atlantic to the Pacific Ocean around Hawaii, and from the Bering Sea to the South Pacific. NDBC&#39;s moored buoys measure and transmit barometric pressure; wind direction, speed, and gust; air and sea temperature; and wave energy spectra from which significant wave height, dominant wave period, and average wave period are derived. Even the direction of wave propagation is measured on many moored buoys. \n" +
"\n" + //changes 2 places...  date is old, but this is what's in the file
"This dataset has both historical data (quality controlled, before 2011-05-01T00:00:00) and near real time data (less quality controlled, from 2011-05-01T00:00:00 on).</att>\n" +
"        <att name=\"time_coverage_end\">2015-07-20T15:00:00Z</att>\n" + //changes
"        <att name=\"time_coverage_resolution\">P1H</att>\n" +
"        <att name=\"time_coverage_start\">1978-06-27T13:00:00Z</att>\n" +
"        <att name=\"title\">NOAA NDBC Standard Meteorological</att>\n" +
"        <att name=\"Westernmost_Easting\" type=\"float\">-79.099</att>\n" +
"    </sourceAttributes -->\n" +
cdmSuggestion() +
"    <addAttributes>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3, Unidata Observation Dataset v1.0</att>\n" +
"        <att name=\"infoUrl\">http://coastwatch.pfeg.noaa.gov</att>\n" +
"        <att name=\"institution\">NOAA NDBC and Participators in Data Assembly Center.</att>\n" +
"        <att name=\"keywords\">air, air_pressure_at_sea_level, air_temperature, altitude, APD, assembly, atmosphere,\n" +
"Atmosphere &gt; Air Quality &gt; Visibility,\n" +
"Atmosphere &gt; Altitude &gt; Planetary Boundary Layer Height,\n" +
"Atmosphere &gt; Atmospheric Pressure &gt; Atmospheric Pressure Measurements,\n" +
"Atmosphere &gt; Atmospheric Pressure &gt; Pressure Tendency,\n" +
"Atmosphere &gt; Atmospheric Pressure &gt; Sea Level Pressure,\n" +
"Atmosphere &gt; Atmospheric Pressure &gt; Static Pressure,\n" +
"Atmosphere &gt; Atmospheric Temperature &gt; Air Temperature,\n" +
"Atmosphere &gt; Atmospheric Temperature &gt; Dew Point Temperature,\n" +
"Atmosphere &gt; Atmospheric Temperature &gt; Surface Air Temperature,\n" +
"Atmosphere &gt; Atmospheric Water Vapor &gt; Dew Point Temperature,\n" +
"Atmosphere &gt; Atmospheric Winds &gt; Surface Winds,\n" +
"atmospheric, ATMP, average, BAR, boundary, buoy, center, control, data, depth, dew, dew point, dew_point_temperature, DEWP, dewpoint, direction, dominant, DPD, eastward, eastward_wind, GST, gust, height, identifier, LAT, latitude, layer, level, LON, longitude, measurements, meridional, meteorological, meteorology, MWD, national, ndbc, near, noaa, northward, northward_wind, nrt, ocean, oceans,\n" +
"Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature,\n" +
"Oceans &gt; Ocean Waves &gt; Significant Wave Height,\n" +
"Oceans &gt; Ocean Waves &gt; Swells,\n" +
"Oceans &gt; Ocean Waves &gt; Wave Period,\n" +
"participators, period, planetary, point, pressure, PTDY, quality, real, sea, sea level, sea_surface_swell_wave_period, sea_surface_swell_wave_significant_height, sea_surface_swell_wave_to_direction, sea_surface_temperature, seawater, significant, speed, sst, standard, static, station, station_id, surface, surface waves, surface_altitude, swell, swells, swh, temperature, tendency, tendency_of_air_pressure, TIDE, time, vapor, VIS, visibility, visibility_in_air, water, wave, waves, wind, wind_from_direction, wind_speed, wind_speed_of_gust, winds, WSPD, WSPU, WSPV, WTMP, WVHT, zonal</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"Metadata_Conventions\">null</att>\n" +
"        <att name=\"sourceUrl\">(local files)</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v29</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>stationID</sourceName>\n" +
"        <destinationName>stationID</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Station ID</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>TIME</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Time</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">2.678004E8 1.4374044E9</att>\n" + //changes
"            <att name=\"axis\">T</att>\n" +
"            <att name=\"comment\">Time in seconds since 1970-01-01T00:00:00Z. The original times are rounded to the nearest hour.</att>\n" +
"            <att name=\"long_name\">Time</att>\n" +
"            <att name=\"point_spacing\">even</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"time_origin\">01-JAN-1970 00:00:00</att>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">1.5E9</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>DEPTH</sourceName>\n" +
"        <destinationName>depth</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Height</att>\n" +
"            <att name=\"_CoordinateZisPositive\">down</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.0 0.0</att>\n" +
"            <att name=\"axis\">Z</att>\n" +
"            <att name=\"comment\">The depth of the station, nominally 0 (see station information for details).</att>\n" +
"            <att name=\"long_name\">Depth</att>\n" +
"            <att name=\"positive\">down</att>\n" +
"            <att name=\"standard_name\">depth</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">8000.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"colorBarPalette\">OceanDepth</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>LAT</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Lat</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">32.501 32.501</att>\n" +
"            <att name=\"axis\">Y</att>\n" +
"            <att name=\"comment\">The latitude of the station.</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>LON</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Lon</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-79.099 -79.099</att>\n" +
"            <att name=\"axis\">X</att>\n" +
"            <att name=\"comment\">The longitude of the station.</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>WD</sourceName>\n" +
"        <destinationName>WD</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"short\">32767</att>\n" +
"            <att name=\"actual_range\" type=\"shortList\">0 359</att>\n" +
"            <att name=\"comment\">Wind direction (the direction the wind is coming from in degrees clockwise from true N) during the same period used for WSPD. See Wind Averaging Methods.</att>\n" +
"            <att name=\"long_name\">Wind Direction</att>\n" +
"            <att name=\"missing_value\" type=\"short\">32767</att>\n" +
"            <att name=\"standard_name\">wind_from_direction</att>\n" +
"            <att name=\"units\">degrees_true</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>WSPD</sourceName>\n" +
"        <destinationName>WSPD</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.0 26.0</att>\n" +
"            <att name=\"comment\">Wind speed (m/s) averaged over an eight-minute period for buoys and a two-minute period for land stations. Reported Hourly. See Wind Averaging Methods.</att>\n" +
"            <att name=\"long_name\">Wind Speed</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">wind_speed</att>\n" +
"            <att name=\"units\">m s-1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>GST</sourceName>\n" +
"        <destinationName>GST</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.0 33.9</att>\n" +
"            <att name=\"comment\">Peak 5 or 8 second gust speed (m/s) measured during the eight-minute or two-minute period. The 5 or 8 second period can be determined by payload, See the Sensor Reporting, Sampling, and Accuracy section.</att>\n" +
"            <att name=\"long_name\">Wind Gust Speed</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">wind_speed_of_gust</att>\n" +
"            <att name=\"units\">m s-1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">30.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>WVHT</sourceName>\n" +
"        <destinationName>WVHT</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.0 12.53</att>\n" +
"            <att name=\"comment\">Significant wave height (meters) is calculated as the average of the highest one-third of all of the wave heights during the 20-minute sampling period. See the Wave Measurements section.</att>\n" +
"            <att name=\"long_name\">Wave Height</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">sea_surface_swell_wave_significant_height</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Surface Waves</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>DPD</sourceName>\n" +
"        <destinationName>DPD</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.0 20.0</att>\n" +
"            <att name=\"comment\">Dominant wave period (seconds) is the period with the maximum wave energy. See the Wave Measurements section.</att>\n" +
"            <att name=\"long_name\">Wave Period, Dominant</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">sea_surface_swell_wave_period</att>\n" +
"            <att name=\"units\">s</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">20.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Surface Waves</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>APD</sourceName>\n" +
"        <destinationName>APD</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.0 13.1</att>\n" +
"            <att name=\"comment\">Average wave period (seconds) of all waves during the 20-minute period. See the Wave Measurements section.</att>\n" +
"            <att name=\"long_name\">Wave Period, Average</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">sea_surface_swell_wave_period</att>\n" +
"            <att name=\"units\">s</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">20.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Surface Waves</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>MWD</sourceName>\n" +
"        <destinationName>MWD</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"short\">32767</att>\n" +
"            <att name=\"actual_range\" type=\"shortList\">0 359</att>\n" +
"            <att name=\"comment\">Mean wave direction corresponding to energy of the dominant period (DOMPD). The units are degrees from true North just like wind direction. See the Wave Measurements section.</att>\n" +
"            <att name=\"long_name\">Wave Direction</att>\n" +
"            <att name=\"missing_value\" type=\"short\">32767</att>\n" +
"            <att name=\"standard_name\">sea_surface_swell_wave_to_direction</att>\n" +
"            <att name=\"units\">degrees_true</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Surface Waves</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>BAR</sourceName>\n" +
"        <destinationName>BAR</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">976.5 1041.5</att>\n" +
"            <att name=\"comment\">Air pressure (hPa). (&#39;PRES&#39; on some NDBC tables.) For C-MAN sites and Great Lakes buoys, the recorded pressure is reduced to sea level using the method described in NWS Technical Procedures Bulletin 291 (11/14/80).</att>\n" +
"            <att name=\"long_name\">Air Pressure</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">air_pressure_at_sea_level</att>\n" +
"            <att name=\"units\">hPa</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">1050.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">950.0</att>\n" +
"            <att name=\"ioos_category\">Pressure</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>ATMP</sourceName>\n" +
"        <destinationName>ATMP</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-6.1 31.7</att>\n" +
"            <att name=\"comment\">Air temperature (Celsius). For sensor heights on buoys, see Hull Descriptions. For sensor heights at C-MAN stations, see C-MAN Sensor Locations.</att>\n" +
"            <att name=\"long_name\">Air Temperature</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">air_temperature</att>\n" +
"            <att name=\"units\">degree_C</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">40.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-10.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>WTMP</sourceName>\n" +
"        <destinationName>WTMP</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-6.1 32.2</att>\n" +
"            <att name=\"comment\">Sea surface temperature (Celsius). For sensor depth, see Hull Description.</att>\n" +
"            <att name=\"long_name\">SST</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">sea_surface_temperature</att>\n" +
"            <att name=\"units\">degree_C</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>DEWP</sourceName>\n" +
"        <destinationName>DEWP</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-10.6 29.1</att>\n" +
"            <att name=\"comment\">Dewpoint temperature taken at the same height as the air temperature measurement.</att>\n" +
"            <att name=\"long_name\">Dewpoint Temperature</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">dew_point_temperature</att>\n" +
"            <att name=\"units\">degree_C</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">40.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>VIS</sourceName>\n" +
"        <destinationName>VIS</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.0 58.1</att>\n" +
"            <att name=\"comment\">Station visibility (km, originally statute miles). Note that buoy stations are limited to reports from 0 to 1.9 miles.</att>\n" +
"            <att name=\"long_name\">Station Visibility</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">visibility_in_air</att>\n" +
"            <att name=\"units\">km</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Meteorology</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>PTDY</sourceName>\n" +
"        <destinationName>PTDY</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-3.1 3.8</att>\n" +
"            <att name=\"comment\">Pressure Tendency is the direction (plus or minus) and the amount of pressure change (hPa) for a three hour period ending at the time of observation.</att>\n" +
"            <att name=\"long_name\">Pressure Tendency</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">tendency_of_air_pressure</att>\n" +
"            <att name=\"units\">hPa</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">3.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-3.0</att>\n" +
"            <att name=\"ioos_category\">Pressure</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>TIDE</sourceName>\n" +
"        <destinationName>TIDE</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"comment\">The water level in meters (originally feet) above or below Mean Lower Low Water (MLLW).</att>\n" +
"            <att name=\"long_name\">Water Level</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">surface_altitude</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">5.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-5.0</att>\n" +
"            <att name=\"ioos_category\">Sea Level</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>WSPU</sourceName>\n" +
"        <destinationName>WSPU</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-17.9 21.0</att>\n" +
"            <att name=\"comment\">The zonal wind speed (m/s) indicates the u component of where the wind is going, derived from Wind Direction and Wind Speed.</att>\n" +
"            <att name=\"long_name\">Wind Speed, Zonal</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">eastward_wind</att>\n" +
"            <att name=\"units\">m s-1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-15.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>WSPV</sourceName>\n" +
"        <destinationName>WSPV</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-25.0 20.9</att>\n" +
"            <att name=\"comment\">The meridional wind speed (m/s) indicates the v component of where the wind is going, derived from Wind Direction and Wind Speed.</att>\n" +
"            <att name=\"long_name\">Wind Speed, Meridional</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">northward_wind</att>\n" +
"            <att name=\"units\">m s-1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-15.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>ID</sourceName>\n" +
"        <destinationName>ID</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"comment\">The station identifier.</att>\n" +
"            <att name=\"long_name\">Station Identifier</att>\n" +
"            <att name=\"standard_name\">station_id</att>\n" +
"            <att name=\"units\">unitless</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"units\">null</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";

            Test.ensureEqual(results, expected, "results=\n" + results);
            //Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), 
            //    expected, "");

            //ensure it is ready-to-use by making a dataset from it
            //with one small change to addAttributes:
            results = String2.replaceAll(results, 
                "        <att name=\"infoUrl\">http://coastwatch.pfeg.noaa.gov</att>\n",
                "        <att name=\"infoUrl\">http://coastwatch.pfeg.noaa.gov</att>\n" +
                "        <att name=\"cdm_data_type\">Other</att>\n");
            String2.log(results);

            EDD edd = oneFromXmlFragment(null, results);
            Test.ensureEqual(edd.datasetID(), "ndbcMet_5df7_b363_ad99", "");
            Test.ensureEqual(edd.title(), "NOAA NDBC Standard Meteorological", "");
            Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
                "stationID, time, depth, latitude, longitude, WD, WSPD, GST, WVHT, " +
                "DPD, APD, MWD, BAR, ATMP, WTMP, DEWP, VIS, PTDY, TIDE, WSPU, WSPV, ID", 
                "");

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nError using generateDatasetsXml."); 
        }

    }

    /**
     * testGenerateDatasetsXml2
     */
    public static void testGenerateDatasetsXml2() throws Throwable {
        testVerboseOn();

        try {
            String results = generateDatasetsXml(
        "c:/data/ngdcJasonSwath/", ".*\\.nc", 
        "c:/data/ngdcJasonSwath/JA2_OPN_2PcS088_239_20101201_005323_20101201_025123.nc", 
        "time",  //not "time, meas_ind"
        DEFAULT_RELOAD_EVERY_N_MINUTES, 
        "", "", "", 
        "", "", 
        "time", 
        "", "", "", "", new Attributes());

            EDD edd = oneFromXmlFragment(null, results);
            Test.ensureEqual(edd.datasetID(), "ngdcJasonSwath_c70d_5281_4d5c", "");
            Test.ensureEqual(edd.title(), "OGDR, Standard dataset", "");
            Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
"time, latitude, longitude, surface_type, alt_echo_type, rad_surf_type, " +
"qual_alt_1hz_range_ku, qual_alt_1hz_range_c, qual_alt_1hz_swh_ku, qual_alt_1hz_swh_c, " +
"qual_alt_1hz_sig0_ku, qual_alt_1hz_sig0_c, qual_alt_1hz_off_nadir_angle_wf_ku, " +
"qual_alt_1hz_off_nadir_angle_pf, qual_inst_corr_1hz_range_ku, qual_inst_corr_1hz_range_c, " +
"qual_inst_corr_1hz_swh_ku, qual_inst_corr_1hz_swh_c, qual_inst_corr_1hz_sig0_ku, " +
"qual_inst_corr_1hz_sig0_c, qual_rad_1hz_tb187, qual_rad_1hz_tb238, qual_rad_1hz_tb340, " +
"alt_state_flag_oper, alt_state_flag_c_band, alt_state_flag_band_seq, " +
"alt_state_flag_ku_band_status, alt_state_flag_c_band_status, rad_state_flag_oper, " +
"orb_state_flag_diode, orb_state_flag_rest, ecmwf_meteo_map_avail, rain_flag, ice_flag, " +
"interp_flag_tb, interp_flag_mean_sea_surface, interp_flag_mdt, " +
"interp_flag_ocean_tide_sol1, interp_flag_ocean_tide_sol2, interp_flag_meteo, alt, " +
"orb_alt_rate, range_ku, range_c, range_rms_ku, range_rms_c, range_numval_ku, " +
"range_numval_c, net_instr_corr_range_ku, net_instr_corr_range_c, model_dry_tropo_corr, " +
"model_wet_tropo_corr, rad_wet_tropo_corr, iono_corr_alt_ku, iono_corr_gim_ku, " +
"sea_state_bias_ku, sea_state_bias_c, swh_ku, swh_c, swh_rms_ku, swh_rms_c, " +
"swh_numval_ku, swh_numval_c, net_instr_corr_swh_ku, net_instr_corr_swh_c, sig0_ku, " +
"sig0_c, sig0_rms_ku, sig0_rms_c, sig0_numval_ku, sig0_numval_c, agc_ku, agc_c, " +
"agc_rms_ku, agc_rms_c, agc_numval_ku, agc_numval_c, net_instr_corr_sig0_ku, " +
"net_instr_corr_sig0_c, atmos_corr_sig0_ku, atmos_corr_sig0_c, off_nadir_angle_wf_ku, " +
"off_nadir_angle_pf, tb_187, tb_238, tb_340, mean_sea_surface, mean_topography, geoid, " +
"bathymetry, inv_bar_corr, hf_fluctuations_corr, ocean_tide_sol1, ocean_tide_sol2, " +
"ocean_tide_equil, ocean_tide_non_equil, load_tide_sol1, load_tide_sol2, " +
"solid_earth_tide, pole_tide, wind_speed_model_u, wind_speed_model_v, " +
"wind_speed_alt, wind_speed_rad, rad_water_vapor, rad_liquid_water, ssha", 
                ""); 

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nUnexpected error."); 
        }

    }

    /**
     * This tests the methods in this class with a 1D dataset.
     *
     * @throws Throwable if trouble
     */
    public static void test1D(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.test1D() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

        String id = "erdCinpKfmSFNH";
        if (deleteCachedDatasetInfo)
            deleteCachedDatasetInfo(id);

        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, id); 

        //*** test getting das for entire dataset
        String2.log("\n****************** EDDTableFromNcFiles 1D test das and dds for entire dataset\n");
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Entire", ".das"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Attributes {\n" +
" s {\n" +
"  id {\n" +
"    String cf_role \"timeseries_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station Identifier\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -120.4, -118.4;\n" +
"    String axis \"X\";\n" +
"    Float64 colorBarMaximum -118.4;\n" +
"    Float64 colorBarMinimum -120.4;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range 32.8, 34.05;\n" +
"    String axis \"Y\";\n" +
"    Float64 colorBarMaximum 34.5;\n" +
"    Float64 colorBarMinimum 32.5;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  depth {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"down\";\n" +
"    Float64 actual_range 5.0, 17.0;\n" +
"    String axis \"Z\";\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Depth\";\n" +
"    String positive \"down\";\n" +
"    String standard_name \"depth\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 4.89024e+8, 1.183248e+9;\n" +
"    String axis \"T\";\n" +
"    Float64 colorBarMaximum 1.183248e+9;\n" +
"    Float64 colorBarMinimum 4.89024e+8;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  common_name {\n" +
"    String ioos_category \"Taxonomy\";\n" +
"    String long_name \"Common Name\";\n" +
"  }\n" +
"  species_name {\n" +
"    String ioos_category \"Taxonomy\";\n" +
"    String long_name \"Species Name\";\n" +
"  }\n" +
"  size {\n" +
"    Int16 actual_range 1, 385;\n" +
"    String ioos_category \"Biology\";\n" +
"    String long_name \"Size\";\n" +
"    String units \"mm\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String acknowledgement \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD, Channel Islands National Park, National Park Service\";\n" +
"    String cdm_data_type \"TimeSeries\";\n" +
"    String cdm_timeseries_variables \"id, longitude, latitude\";\n" +
"    String contributor_email \"David_Kushner@nps.gov\";\n" +
"    String contributor_name \"Channel Islands National Park, National Park Service\";\n" +
"    String contributor_role \"Source of data.\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String creator_email \"Roy.Mendelssohn@noaa.gov\";\n" +
"    String creator_name \"NOAA NMFS SWFSC ERD\";\n" +
"    String creator_url \"http://www.pfel.noaa.gov\";\n" +
"    String date_created \"2008-06-11T21:43:28Z\";\n" +
"    String date_issued \"2008-06-11T21:43:28Z\";\n" +
"    Float64 Easternmost_Easting -118.4;\n" +
"    String featureType \"TimeSeries\";\n" +
"    Float64 geospatial_lat_max 34.05;\n" +
"    Float64 geospatial_lat_min 32.8;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max -118.4;\n" +
"    Float64 geospatial_lon_min -120.4;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    Float64 geospatial_vertical_max 17.0;\n" +
"    Float64 geospatial_vertical_min 5.0;\n" +
"    String geospatial_vertical_positive \"down\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"Channel Islands National Park, National Park Service\n" +
"2008-06-11T21:43:28Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD\n" + //will be SWFSC when reprocessed
today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

//+ " (local files)\n" +
//today + " " + EDStatic.erddapUrl + //in tests, always use non-https url
expected =
"/tabledap/erdCinpKfmSFNH.das\";\n" +
"    String infoUrl \"http://www.nps.gov/chis/naturescience/index.htm\";\n" +
"    String institution \"CINP\";\n" +
"    String keywords \"Biosphere > Aquatic Ecosystems > Coastal Habitat,\n" +
"Biosphere > Aquatic Ecosystems > Marine Habitat,\n" +
"aquatic, atmosphere, biology, biosphere, channel, cinp, coastal, common, depth, ecosystems, forest, frequency, habitat, height, identifier, islands, kelp, marine, monitoring, name, natural, size, species, station, taxonomy, time\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.  National Park Service Disclaimer: The National Park Service shall not be held liable for improper or incorrect use of the data described and/or contained herein. These data and related graphics are not legal documents and are not intended to be used as such. The information contained in these data is dynamic and may change over time. The data are not better than the original sources from which they were derived. It is the responsibility of the data user to use the data appropriately and consistent within the limitation of geospatial data in general and these data in particular. The related graphics are intended to aid the data user in acquiring relevant data; it is not appropriate to use the related graphics as data. The National Park Service gives no warranty, expressed or implied, as to the accuracy, reliability, or completeness of these data. It is strongly recommended that these data are directly acquired from an NPS server and not indirectly through other sources which may have changed the data in some way. Although these data have been processed successfully on computer systems at the National Park Service, no warranty expressed or implied is made regarding the utility of the data on other systems for general or scientific purposes, nor shall the act of distribution constitute any such warranty. This disclaimer applies both to individual use of the data and aggregate use with other data.\";\n" +
"    String naming_authority \"gov.noaa.pfel.coastwatch\";\n" +
"    Float64 Northernmost_Northing 34.05;\n" +
"    String observationDimension \"row\";\n" + //2012-07-27 this should disappear soon
"    String project \"NOAA NMFS SWFSC ERD (http://www.pfel.noaa.gov/)\";\n" +
"    String references \"Channel Islands National Parks Inventory and Monitoring information: http://nature.nps.gov/im/units/medn . Kelp Forest Monitoring Protocols: http://www.nature.nps.gov/im/units/chis/Reports_PDF/Marine/KFM-HandbookVol1.pdf .\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing 32.8;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" + 
"    String subsetVariables \"id, longitude, latitude, common_name, species_name\";\n" +
"    String summary \"This dataset has measurements of the size of selected animal species at selected locations in the Channel Islands National Park. Sampling is conducted annually between the months of May-October, so the Time data in this file is July 1 of each year (a nominal value). The size frequency measurements were taken within 10 meters of the transect line at each site.  Depths at the site vary some, but we describe the depth of the site along the transect line where that station's temperature logger is located, a typical depth for the site.\";\n" +
"    String time_coverage_end \"2007-07-01T00:00:00Z\";\n" +
"    String time_coverage_start \"1985-07-01T00:00:00Z\";\n" +
"    String title \"Channel Islands, Kelp Forest Monitoring, Size and Frequency, Natural Habitat\";\n" +
"    Float64 Westernmost_Easting -120.4;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Entire", ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String id;\n" +
"    Float64 longitude;\n" +
"    Float64 latitude;\n" +
"    Float64 depth;\n" +
"    Float64 time;\n" +
"    String common_name;\n" +
"    String species_name;\n" +
"    Int16 size;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //*** test make data files
        String2.log("\n****************** EDDTableFromNcFiles.test 1D make DATA FILES\n");       

        //.csv    for one lat,lon,time
        userDapQuery = "" +
            "&longitude=-119.05&latitude=33.46666666666&time=2005-07-01T00:00:00";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_1Station", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"id,longitude,latitude,depth,time,common_name,species_name,size\n" +
",degrees_east,degrees_north,m,UTC,,,mm\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,14.0,2005-07-01T00:00:00Z,Bat star,Asterina miniata,57\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,14.0,2005-07-01T00:00:00Z,Bat star,Asterina miniata,41\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,14.0,2005-07-01T00:00:00Z,Bat star,Asterina miniata,55\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = //last 3 lines
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,14.0,2005-07-01T00:00:00Z,Purple sea urchin,Strongylocentrotus purpuratus,15\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,14.0,2005-07-01T00:00:00Z,Purple sea urchin,Strongylocentrotus purpuratus,23\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,14.0,2005-07-01T00:00:00Z,Purple sea urchin,Strongylocentrotus purpuratus,19\n";
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "\nresults=\n" + results);


        //.csv    for one lat,lon,time      via lon > <
        userDapQuery = "" +
            "&longitude>-119.06&longitude<=-119.04&latitude=33.46666666666&time=2005-07-01T00:00:00";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_1StationGTLT", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"id,longitude,latitude,depth,time,common_name,species_name,size\n" +
",degrees_east,degrees_north,m,UTC,,,mm\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,14.0,2005-07-01T00:00:00Z,Bat star,Asterina miniata,57\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,14.0,2005-07-01T00:00:00Z,Bat star,Asterina miniata,41\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,14.0,2005-07-01T00:00:00Z,Bat star,Asterina miniata,55\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = //last 3 lines
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,14.0,2005-07-01T00:00:00Z,Purple sea urchin,Strongylocentrotus purpuratus,15\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,14.0,2005-07-01T00:00:00Z,Purple sea urchin,Strongylocentrotus purpuratus,23\n" +
"Santa Barbara (Webster's Arch),-119.05,33.4666666666667,14.0,2005-07-01T00:00:00Z,Purple sea urchin,Strongylocentrotus purpuratus,19\n";
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "\nresults=\n" + results);


        //.csv for test requesting all stations, 1 time, 1 species
        userDapQuery = "" +
            "&time=2005-07-01&common_name=\"Red+abalone\"";
        long time = System.currentTimeMillis();
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_eq", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"id,longitude,latitude,depth,time,common_name,species_name,size\n" +
",degrees_east,degrees_north,m,UTC,,,mm\n" +
"San Miguel (Hare Rock),-120.35,34.05,5.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,13\n" +
"San Miguel (Miracle Mile),-120.4,34.0166666666667,10.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,207\n" +
"San Miguel (Miracle Mile),-120.4,34.0166666666667,10.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,203\n" +
"San Miguel (Miracle Mile),-120.4,34.0166666666667,10.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,193\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = //last 3 lines
"Santa Rosa (South Point),-120.116666666667,33.8833333333333,13.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,185\n" +
"Santa Rosa (Trancion Canyon),-120.15,33.9,9.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,198\n" +
"Santa Rosa (Trancion Canyon),-120.15,33.9,9.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,85\n";
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "\nresults=\n" + results);


        //.csv for test requesting all stations, 1 time, 1 species    String !=
        userDapQuery = "" +
            "&time=2005-07-01&id!=\"San+Miguel+(Hare+Rock)\"&common_name=\"Red+abalone\"";
        time = System.currentTimeMillis();
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_NE", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"id,longitude,latitude,depth,time,common_name,species_name,size\n" +
",degrees_east,degrees_north,m,UTC,,,mm\n" +
"San Miguel (Miracle Mile),-120.4,34.0166666666667,10.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,207\n" +
"San Miguel (Miracle Mile),-120.4,34.0166666666667,10.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,203\n" +
"San Miguel (Miracle Mile),-120.4,34.0166666666667,10.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,193\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = //last 3 lines
"Santa Rosa (South Point),-120.116666666667,33.8833333333333,13.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,185\n" +
"Santa Rosa (Trancion Canyon),-120.15,33.9,9.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,198\n" +
"Santa Rosa (Trancion Canyon),-120.15,33.9,9.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,85\n";
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "\nresults=\n" + results);


        //.csv for test requesting all stations, 1 time, 1 species   String > <
        userDapQuery = "" +
            "&time=2005-07-01&id>\"San+Miguel+(G\"&id<=\"San+Miguel+(I\"&common_name=\"Red+abalone\"";
        time = System.currentTimeMillis();
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_gtlt", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"id,longitude,latitude,depth,time,common_name,species_name,size\n" +
",degrees_east,degrees_north,m,UTC,,,mm\n" +
"San Miguel (Hare Rock),-120.35,34.05,5.0,2005-07-01T00:00:00Z,Red abalone,Haliotis rufescens,13\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv for test requesting all stations, 1 time, 1 species     REGEX
        userDapQuery = "longitude,latitude,depth,time,id,species_name,size" + //no common_name
            "&time=2005-07-01&id=~\"(zztop|.*Hare+Rock.*)\"&common_name=\"Red+abalone\"";   //but common_name here
        time = System.currentTimeMillis();
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_regex", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude,latitude,depth,time,id,species_name,size\n" +
"degrees_east,degrees_north,m,UTC,,,mm\n" +
"-120.35,34.05,5.0,2005-07-01T00:00:00Z,San Miguel (Hare Rock),Haliotis rufescens,13\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

    }

    /**
     * This tests the methods in this class with a 2D dataset.
     *
     * @throws Throwable if trouble
     */
    public static void test2D(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.test2D() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;

        //the test files were made with makeTestFiles();
        String id = "testNc2D";       
        if (deleteCachedDatasetInfo) 
            deleteCachedDatasetInfo(id);

        //touch a good and a bad file, so they are checked again
        File2.touch("c:/u00/data/points/nc2d/NDBC_32012_met.nc");
        File2.touch("c:/u00/data/points/nc2d/NDBC_4D_met.nc");

        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, id); 
        //just comment out when working on datasets below
/* currently not active
        Test.ensureTrue(eddTable.sosOfferings().indexOf("41002") >= 0, eddTable.sosOfferings().toString());
        //Test.ensureEqual(eddTable.sosObservedProperties()[0], 
        //    "http://www.csc.noaa.gov/ioos/schema/IOOS-DIF/IOOS/0.6.0/dictionaries/phenomenaDictionary.xml#AverageWavePeriod", 
        //    "");
*/
        //*** test getting das for entire dataset
        String2.log("\n****************** EDDTableFromNcFiles 2D test das dds for entire dataset\n");
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Entire", ".das"); 
        results = String2.annotatedString(new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray()));
        //String2.log(results);
        expected = 
"  time {[10]\n" +
"    String _CoordinateAxisType \"Time\";[10]\n" +
"    Float64 actual_range 8.67456e+7, 1.2075984e+9;[10]\n" +
"    String axis \"T\";[10]\n" +
"    String comment \"Time in seconds since 1970-01-01T00:00:00Z. The original times are rounded to the nearest hour.\";[10]\n" +
"    String ioos_category \"Time\";[10]\n" +
"    String long_name \"Time\";[10]\n" +
"    String standard_name \"time\";[10]\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";[10]\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";[10]\n" +
"  }[10]\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = 
"  wd {[10]\n" +
"    Int16 _FillValue 32767;[10]\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = 
"  wspv {[10]\n" +
"    Float32 _FillValue -9999999.0;[10]\n" +
"    Float32 actual_range"; //varies with subset -6.1, 11.0;[10]  
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = 
"    String comment \"The meridional wind speed (m/s) indicates the v component of where the wind is going, derived from Wind Direction and Wind Speed.\";[10]\n" +
"    String ioos_category \"Wind\";[10]\n" +
"    String long_name \"Wind Speed, Meridional\";[10]\n" +
"    Float32 missing_value -9999999.0;[10]\n" +
"    String standard_name \"northward_wind\";[10]\n" +
"    String units \"m s-1\";[10]\n" +
"  }[10]\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Entire", ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String station;\n" +
"    Float32 latitude;\n" +
"    Float64 time;\n" +      //no altitude or longitude
"    Int16 wd;\n" +
"    Float32 wspd;\n" +
"    Float32 gst;\n" +
"    Float32 wvht;\n" +
"    Float32 dpd;\n" +
"    Float32 apd;\n" +
"    Int16 mwd;\n" +
"    Float32 bar;\n" +
"    Float32 atmp;\n" +
"    Float32 wtmp;\n" +
"    Float32 dewp;\n" +
"    Float32 vis;\n" +
"    Float32 ptdy;\n" +
"    Float32 tide;\n" +
"    Float32 wspu;\n" +
"    Float32 wspv;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //*** test make data files
        String2.log("\n****************** EDDTableFromNcFiles.test2D make DATA FILES\n");       

        //.csv
        //from NdbcMetStation.test31201
        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2005 04 19 00 00 999 99.0 99.0  1.40  9.00 99.00 999 9999.0 999.0  24.4 999.0 99.0 99.00 first available
        //double seconds = Calendar2.isoStringToEpochSeconds("2005-04-19T00");
        //int row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        //Test.ensureEqual(table.getStringData(sosOfferingIndex, row), "31201", "");
        //Test.ensureEqual(table.getFloatData(latIndex, row), -27.7f, "");
        //Test.ensureEqual(table.getFloatData(lonIndex, row), -48.13f, "");

        userDapQuery = "latitude,time,station,wvht,dpd,wtmp,dewp" +
            "&latitude=-27.7&time=2005-04-19T00";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data1", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"latitude,time,station,wvht,dpd,wtmp,dewp\n" +
"degrees_north,UTC,,m,s,degree_C,degree_C\n" +
"-27.7,2005-04-19T00:00:00Z,31201,1.4,9.0,24.4,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2005 04 25 18 00 999 99.0 99.0  3.90  8.00 99.00 999 9999.0 999.0  23.9 999.0 99.0 99.00
        userDapQuery = "latitude,time,station,wvht,dpd,wtmp,dewp" +
            "&latitude=-27.7&time>=2005-04-01&time<=2005-04-26";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data2", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = "latitude,time,station,wvht,dpd,wtmp,dewp\n";
        Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);
        expected = "degrees_north,UTC,,m,s,degree_C,degree_C\n";
        Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);
        expected = "-27.7,2005-04-19T00:00:00Z,31201,1.4,9.0,24.4,NaN\n"; //time above
        Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);
        expected = "-27.7,2005-04-25T18:00:00Z,31201,3.9,8.0,23.9,NaN\n"; //this time
        Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);

        //test requesting a lat area
        userDapQuery = "latitude,time,station,wvht,dpd,wtmp,dewp" +
            "&latitude>35&latitude<39&time=2005-04-01";
        long time = System.currentTimeMillis();
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data3", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"latitude,time,station,wvht,dpd,wtmp,dewp\n" +
"degrees_north,UTC,,m,s,degree_C,degree_C\n" +
"35.01,2005-04-01T00:00:00Z,41025,1.34,10.0,9.1,14.9\n" +
"38.47,2005-04-01T00:00:00Z,44004,2.04,11.43,9.8,4.9\n" +
"38.46,2005-04-01T00:00:00Z,44009,1.3,10.0,5.0,5.7\n" +
"36.61,2005-04-01T00:00:00Z,44014,1.67,11.11,6.5,8.6\n" +
"37.36,2005-04-01T00:00:00Z,46012,2.55,12.5,13.7,NaN\n" +
"38.23,2005-04-01T00:00:00Z,46013,2.3,12.9,13.9,NaN\n" +
"37.75,2005-04-01T00:00:00Z,46026,1.96,12.12,14.0,NaN\n" +
"35.74,2005-04-01T00:00:00Z,46028,2.57,12.9,16.3,NaN\n" +
"36.75,2005-04-01T00:00:00Z,46042,2.21,17.39,14.5,NaN\n" +
"37.98,2005-04-01T00:00:00Z,46059,2.51,14.29,12.9,NaN\n" +
"36.83,2005-04-01T00:00:00Z,46091,NaN,NaN,NaN,NaN\n" +
"36.75,2005-04-01T00:00:00Z,46092,NaN,NaN,NaN,NaN\n" +
"36.69,2005-04-01T00:00:00Z,46093,NaN,NaN,14.3,NaN\n" +
"37.57,2005-04-01T00:00:00Z,46214,2.5,9.0,12.8,NaN\n" +
"35.21,2005-04-01T00:00:00Z,46215,1.4,10.0,11.4,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //test that constraint vars are sent to low level data request
        userDapQuery = "latitude,station,wvht,dpd,wtmp,dewp" + //no "time" here
            "&latitude>35&latitude<39&time=2005-04-01"; //"time" here
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data4", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"latitude,station,wvht,dpd,wtmp,dewp\n" +
"degrees_north,,m,s,degree_C,degree_C\n" +
"35.01,41025,1.34,10.0,9.1,14.9\n" +
"38.47,44004,2.04,11.43,9.8,4.9\n" +
"38.46,44009,1.3,10.0,5.0,5.7\n" +
"36.61,44014,1.67,11.11,6.5,8.6\n" +
"37.36,46012,2.55,12.5,13.7,NaN\n" +
"38.23,46013,2.3,12.9,13.9,NaN\n" +
"37.75,46026,1.96,12.12,14.0,NaN\n" +
"35.74,46028,2.57,12.9,16.3,NaN\n" +
"36.75,46042,2.21,17.39,14.5,NaN\n" +
"37.98,46059,2.51,14.29,12.9,NaN\n" +
"36.83,46091,NaN,NaN,NaN,NaN\n" +
"36.75,46092,NaN,NaN,NaN,NaN\n" +
"36.69,46093,NaN,NaN,14.3,NaN\n" +
"37.57,46214,2.5,9.0,12.8,NaN\n" +
"35.21,46215,1.4,10.0,11.4,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //test that constraint vars are sent to low level data request
        //and that constraint causing 0rows for a station doesn't cause problems
        userDapQuery = "latitude,wtmp&time>=2008-03-14T18:00:00Z&time<=2008-03-14T18:00:00Z&wtmp>20";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data5", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"latitude,wtmp\n" +
"degrees_north,degree_C\n" +
"32.31,23.5\n" +
"28.5,21.3\n" +
"28.95,23.7\n" +
"30.0,20.1\n" +
"14.53,25.3\n" +
"20.99,25.7\n" +
"27.47,23.8\n" +
"31.9784,22.0\n" +
"28.4,21.0\n" +
"27.55,21.8\n" +
"25.9,24.1\n" +
"25.17,23.9\n" +
"26.07,26.1\n" +
"22.01,24.4\n" +
"19.87,26.8\n" +
"15.01,26.4\n" +
"27.3403,20.2\n" +
"29.06,21.8\n" +
"38.47,20.4\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
    }

    /**
     * This tests the methods in this class with a 3D dataset.
     *
     * @throws Throwable if trouble
     */
    public static void test3D(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.test3D() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String id = "testNc3D";
        
        if (deleteCachedDatasetInfo) 
            deleteCachedDatasetInfo(id);

        //touch a good and a bad file, so they are checked again
        File2.touch("c:/u00/data/points/nc3d/NDBC_32012_met.nc");
        File2.touch("c:/u00/data/points/nc3d/NDBC_4D_met.nc");

        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, id); 
        //just comment out when working on datasets below
/* currently not active
        //test sos-server values
        Test.ensureTrue(eddTable.sosOfferings().indexOf("32012") >= 0, eddTable.sosOfferings().toString());
        //Test.ensureEqual(eddTable.sosObservedProperties()[0], 
        //    "http://www.csc.noaa.gov/ioos/schema/IOOS-DIF/IOOS/0.6.0/dictionaries/phenomenaDictionary.xml#AverageWavePeriod", 
        //    "");
*/
        //*** test getting das for entire dataset
        String2.log("\n****************** EDDTableFromNcFiles test3D das dds for entire dataset\n");
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Entire", ".das"); 
        results = String2.annotatedString(new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray()));
        //String2.log(results);
        expected = 
"  time {[10]\n" +
"    String _CoordinateAxisType \"Time\";[10]\n" +
"    Float64 actual_range 8.67456e+7, 1.2075984e+9;[10]\n" +
"    String axis \"T\";[10]\n" +
"    String comment \"Time in seconds since 1970-01-01T00:00:00Z. The original times are rounded to the nearest hour.\";[10]\n" +
"    String ioos_category \"Time\";[10]\n" +
"    String long_name \"Time\";[10]\n" +
"    String standard_name \"time\";[10]\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";[10]\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";[10]\n" +
"  }[10]\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = 
"  wd {[10]\n" +
"    Int16 _FillValue 32767;[10]\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = 
"  wspv {[10]\n" +
"    Float32 _FillValue -9999999.0;[10]\n" +
"    Float32 actual_range"; //varies with subset -6.1, 11.0;[10]  
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = 
"    String comment \"The meridional wind speed (m/s) indicates the v component of where the wind is going, derived from Wind Direction and Wind Speed.\";[10]\n" +
"    String ioos_category \"Wind\";[10]\n" +
"    String long_name \"Wind Speed, Meridional\";[10]\n" +
"    Float32 missing_value -9999999.0;[10]\n" +
"    String standard_name \"northward_wind\";[10]\n" +
"    String units \"m s-1\";[10]\n" +
"  }[10]\n";
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Entire", ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String station;\n" +
"    Float32 longitude;\n" +
"    Float32 latitude;\n" +
"    Float64 time;\n" +      //no altitude
"    Int16 wd;\n" +
"    Float32 wspd;\n" +
"    Float32 gst;\n" +
"    Float32 wvht;\n" +
"    Float32 dpd;\n" +
"    Float32 apd;\n" +
"    Int16 mwd;\n" +
"    Float32 bar;\n" +
"    Float32 atmp;\n" +
"    Float32 wtmp;\n" +
"    Float32 dewp;\n" +
"    Float32 vis;\n" +
"    Float32 ptdy;\n" +
"    Float32 tide;\n" +
"    Float32 wspu;\n" +
"    Float32 wspv;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //*** test make data files
        String2.log("\n****************** EDDTableFromNcFiles.test3D make DATA FILES\n");       

        //.csv
        //from NdbcMetStation.test31201
        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2005 04 19 00 00 999 99.0 99.0  1.40  9.00 99.00 999 9999.0 999.0  24.4 999.0 99.0 99.00 first available
        //double seconds = Calendar2.isoStringToEpochSeconds("2005-04-19T00");
        //int row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        //Test.ensureEqual(table.getStringData(sosOfferingIndex, row), "31201", "");
        //Test.ensureEqual(table.getFloatData(latIndex, row), -27.7f, "");
        //Test.ensureEqual(table.getFloatData(lonIndex, row), -48.13f, "");

        userDapQuery = "longitude,latitude,time,station,wvht,dpd,wtmp,dewp" +
            "&longitude=-48.13&latitude=-27.7&time=2005-04-19T00";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data1", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude,latitude,time,station,wvht,dpd,wtmp,dewp\n" +
"degrees_east,degrees_north,UTC,,m,s,degree_C,degree_C\n" +
"-48.13,-27.7,2005-04-19T00:00:00Z,31201,1.4,9.0,24.4,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2005 04 25 18 00 999 99.0 99.0  3.90  8.00 99.00 999 9999.0 999.0  23.9 999.0 99.0 99.00
        userDapQuery = "longitude,latitude,time,station,wvht,dpd,wtmp,dewp" +
            "&longitude=-48.13&latitude=-27.7&time>=2005-04-01&time<=2005-04-26";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data2", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = "longitude,latitude,time,station,wvht,dpd,wtmp,dewp\n";
        Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);
        expected = "degrees_east,degrees_north,UTC,,m,s,degree_C,degree_C\n";
        Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);
        expected = "-48.13,-27.7,2005-04-19T00:00:00Z,31201,1.4,9.0,24.4,NaN\n"; //time above
        Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);
        expected = "-48.13,-27.7,2005-04-25T18:00:00Z,31201,3.9,8.0,23.9,NaN\n"; //this time
        Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);

        //test requesting a lat lon area
        userDapQuery = "longitude,latitude,time,station,wvht,dpd,wtmp,dewp" +
            "&longitude>-125&longitude<-121&latitude>35&latitude<39&time=2005-04-01";
        long time = System.currentTimeMillis();
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data3", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude,latitude,time,station,wvht,dpd,wtmp,dewp\n" +
"degrees_east,degrees_north,UTC,,m,s,degree_C,degree_C\n" +
"-122.88,37.36,2005-04-01T00:00:00Z,46012,2.55,12.5,13.7,NaN\n" +
"-123.32,38.23,2005-04-01T00:00:00Z,46013,2.3,12.9,13.9,NaN\n" +
"-122.82,37.75,2005-04-01T00:00:00Z,46026,1.96,12.12,14.0,NaN\n" +
"-121.89,35.74,2005-04-01T00:00:00Z,46028,2.57,12.9,16.3,NaN\n" +
"-122.42,36.75,2005-04-01T00:00:00Z,46042,2.21,17.39,14.5,NaN\n" +
"-121.9,36.83,2005-04-01T00:00:00Z,46091,NaN,NaN,NaN,NaN\n" +
"-122.02,36.75,2005-04-01T00:00:00Z,46092,NaN,NaN,NaN,NaN\n" +
"-122.41,36.69,2005-04-01T00:00:00Z,46093,NaN,NaN,14.3,NaN\n" +
"-123.28,37.57,2005-04-01T00:00:00Z,46214,2.5,9.0,12.8,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //test that constraint vars are sent to low level data request
        userDapQuery = "longitude,latitude,station,wvht,dpd,wtmp,dewp" + //no "time" here
            "&longitude>-125&longitude<-121&latitude>35&latitude<39&time=2005-04-01"; //"time" here
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data4", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude,latitude,station,wvht,dpd,wtmp,dewp\n" +
"degrees_east,degrees_north,,m,s,degree_C,degree_C\n" +
"-122.88,37.36,46012,2.55,12.5,13.7,NaN\n" +
"-123.32,38.23,46013,2.3,12.9,13.9,NaN\n" +
"-122.82,37.75,46026,1.96,12.12,14.0,NaN\n" +
"-121.89,35.74,46028,2.57,12.9,16.3,NaN\n" +
"-122.42,36.75,46042,2.21,17.39,14.5,NaN\n" +
"-121.9,36.83,46091,NaN,NaN,NaN,NaN\n" +
"-122.02,36.75,46092,NaN,NaN,NaN,NaN\n" +
"-122.41,36.69,46093,NaN,NaN,14.3,NaN\n" +
"-123.28,37.57,46214,2.5,9.0,12.8,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //test that constraint vars are sent to low level data request
        //and that constraint causing 0rows for a station doesn't cause problems
        userDapQuery = "longitude,latitude,wtmp&time>=2008-03-14T18:00:00Z&time<=2008-03-14T18:00:00Z&wtmp>20";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data5", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude,latitude,wtmp\n" +
"degrees_east,degrees_north,degree_C\n" +
"-75.35,32.31,23.5\n" +
"-80.17,28.5,21.3\n" +
"-78.47,28.95,23.7\n" +
"-80.6,30.0,20.1\n" +
"-46.0,14.53,25.3\n" +
"-65.01,20.99,25.7\n" +
"-71.49,27.47,23.8\n" +
"-69.649,31.9784,22.0\n" +
"-80.53,28.4,21.0\n" +
"-80.22,27.55,21.8\n" +
"-89.67,25.9,24.1\n" +
"-94.42,25.17,23.9\n" +
"-85.94,26.07,26.1\n" +
"-94.05,22.01,24.4\n" +
"-85.06,19.87,26.8\n" +
"-67.5,15.01,26.4\n" +
"-84.245,27.3403,20.2\n" +
"-88.09,29.06,21.8\n" +
"-70.56,38.47,20.4\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
    }

    /**
     * One time: make c:/u00/data/points/nc2d and String2.testU00Dir/u00/data/points/nc3d test files 
     * from c:/u00/data/points/ndbcMet nc4d files.
     */
    public static void makeTestFiles() {
        //get list of files
        String fromDir = "c:/u00/data/points/ndbcMet/";
        String dir3 =    "c:/u00/data/points/nc3d/";
        String dir2 =    "c:/u00/data/points/nc2d/";
        String[] names = RegexFilenameFilter.list(fromDir, "NDBC_(3|4).*nc");        
        
        //for each file
        for (int f = 0; f < names.length; f++) {
            NetcdfFile in = null;
            NetcdfFileWriteable out2 = null;
            NetcdfFileWriteable out3 = null;

            try {
                String2.log("in #" + f + "=" + fromDir + names[f]);
                if (f == 0) 
                    String2.log(NcHelper.dumpString(fromDir + names[f], false));

                in = NcHelper.openFile(fromDir + names[f]);
                out2 = NetcdfFileWriteable.createNew(dir2 + names[f], false);
                out3 = NetcdfFileWriteable.createNew(dir3 + names[f], false);

                //write the globalAttributes
                Attributes atts = new Attributes();
                NcHelper.getGlobalAttributes(in, atts);
                NcHelper.setAttributes(out2, "NC_GLOBAL", atts);
                NcHelper.setAttributes(out3, "NC_GLOBAL", atts);

                Variable vars[] = NcHelper.find4DVariables(in, null);
                Variable timeVar = in.findVariable("TIME");
                Variable latVar  = in.findVariable("LAT");
                Variable lonVar  = in.findVariable("LON");
                EDStatic.ensureArraySizeOkay(timeVar.getSize(), "EDDTableFromNcFiles.makeTestFiles");
                Dimension tDim2 = out2.addDimension("TIME", (int)timeVar.getSize()); //safe since checked above
                Dimension tDim3 = out3.addDimension("TIME", (int)timeVar.getSize()); //safe since checked above
                Dimension yDim2 = out2.addDimension("LAT", 1);
                Dimension yDim3 = out3.addDimension("LAT", 1);
                Dimension xDim3 = out3.addDimension("LON", 1);
                
                //create axis variables
                out2.addVariable("TIME", timeVar.getDataType(), new Dimension[]{tDim2}); 
                out2.addVariable("LAT",  latVar.getDataType(),  new Dimension[]{yDim2}); 

                out3.addVariable("TIME", timeVar.getDataType(), new Dimension[]{tDim3}); 
                out3.addVariable("LAT",  latVar.getDataType(),  new Dimension[]{yDim3}); 
                out3.addVariable("LON",  lonVar.getDataType(),  new Dimension[]{xDim3}); 

                //write the axis variable attributes
                atts.clear();
                NcHelper.getVariableAttributes(timeVar, atts);
                NcHelper.setAttributes(out2, "TIME", atts);
                NcHelper.setAttributes(out3, "TIME", atts);

                atts.clear();
                NcHelper.getVariableAttributes(latVar, atts);
                NcHelper.setAttributes(out2, "LAT", atts);
                NcHelper.setAttributes(out3, "LAT", atts);

                atts.clear();
                NcHelper.getVariableAttributes(lonVar, atts);
                NcHelper.setAttributes(out2, "LON", atts);
                NcHelper.setAttributes(out3, "LON", atts);

                //create data variables
                for (int col = 0; col < vars.length; col++) {
                    //create the data variables
                    Variable var = vars[col];
                    String varName = var.getFullName();
                    Array ar = var.read();
                    out2.addVariable(varName, var.getDataType(), new Dimension[]{tDim2, yDim2}); 
                    out3.addVariable(varName, var.getDataType(), new Dimension[]{tDim3, yDim3, xDim3}); 

                    //write the data variable attributes
                    atts.clear();
                    NcHelper.getVariableAttributes(var, atts);
                    NcHelper.setAttributes(out2, varName, atts);
                    NcHelper.setAttributes(out3, varName, atts);
                }

                //leave "define" mode
                out2.create();
                out3.create();

                //write axis data
                Array ar = in.findVariable("TIME").read();
                out2.write("TIME", ar);
                out3.write("TIME", ar);
                ar = in.findVariable("LAT").read();
                out2.write("LAT", ar);
                out3.write("LAT", ar);
                ar = in.findVariable("LON").read();
                out3.write("LON", ar);
                 
                for (int col = 0; col < vars.length; col++) {
                    //write the data for each var
                    Variable var = vars[col];
                    String varName = var.getFullName();
                    ar = var.read();
                    int oldShape[] = ar.getShape();
                    int newShape2[] = {oldShape[0], 1};
                    int newShape3[] = {oldShape[0], 1, 1};
                    out2.write(varName, ar.reshape(newShape2));
                    out3.write(varName, ar.reshape(newShape3));
                }

                in.close();
                out2.close();
                out3.close();

                if (f == 0) {
                    String2.log("\nout2=" + NcHelper.dumpString(dir2 + names[f], false));
                    String2.log("\nout3=" + NcHelper.dumpString(dir3 + names[f], false));
                }

            } catch (Throwable t) {
                String2.log(MustBe.throwableToString(t));
                try { if (in != null)  in.close();    } catch (Exception t2) {}
                try { if (out2 != null) out2.close(); } catch (Exception t2) {}
                try { if (out3 != null) out3.close(); } catch (Exception t2) {}
            }
        }
    }

    /**
     * This tests the methods in this class with a 4D dataset.
     *
     * @throws Throwable if trouble
     */
    public static void test4D(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.test4D() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        int po;
        EDV edv;

        String id = "cwwcNDBCMet";
        if (deleteCachedDatasetInfo) 
            deleteCachedDatasetInfo(id);

        //touch a good and a bad file, so they are checked again
        File2.touch("c:/u00/data/points/ndbcMet/NDBC_32012_met.nc");
        File2.touch("c:/u00/data/points/ndbcMet/NDBC_3D_met.nc");

        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, id); 
        //just comment out when working on datasets below
/* currently not active
        Test.ensureTrue(eddTable.sosOfferings().indexOf("32012") >= 0, eddTable.sosOfferings().toString());
        //Test.ensureEqual(eddTable.sosObservedProperties()[0], 
        //    "http://www.csc.noaa.gov/ioos/schema/IOOS-DIF/IOOS/0.6.0/dictionaries/phenomenaDictionary.xml#AverageWavePeriod", 
        //    "");
*/
        //*** test getting das for entire dataset
        String2.log("\n****************** EDDTableFromNcFiles test4D das dds for entire dataset\n");
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Entire", ".das"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);

        expected = 
"  wd {\n" +
"    Int16 _FillValue 32767;\n";
        int tPo = results.indexOf(expected.substring(0,10));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(results.substring(tPo, tPo + expected.length()), expected, "\nresults=\n" + results);

        expected = 
"  wvht {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 0.0, 92.39;\n" +
"    Float64 colorBarMaximum 10.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"Significant wave height (meters) is calculated as the average of the highest one-third of all of the wave heights during " +
"the 20-minute sampling period. See the Wave Measurements section.\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Wave Height\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"sea_surface_swell_wave_significant_height\";\n" +
"    String units \"m\";\n" +
"  }\n";
        tPo = results.indexOf(expected.substring(0,10));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(results.substring(tPo, tPo + expected.length()), expected, 
            "\nresults=\n" + results);

        expected = 
"  wspv {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range"; //varies with subset -6.1, 11.0;  
        tPo = results.indexOf(expected.substring(0,10));
        if (tPo < 0) String2.log("\ntPo=-1 results=\n" + results);
        Test.ensureEqual(results.substring(tPo, tPo + expected.length()), expected, 
            "\nresults=\n" + results);

        expected = 
"The meridional wind speed (m/s) indicates the v component of where the wind is going, derived from Wind Direction and Wind Speed.\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind Speed, Meridional\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"northward_wind\";\n" +
"    String units \"m s-1\";\n" +
"  }\n";
        tPo = results.indexOf(expected.substring(0,10));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(results.substring(tPo, tPo + expected.length()), expected, "\nresults=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Entire", ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String station;\n" +
"    Float32 longitude;\n" +
"    Float32 latitude;\n" +
"    Float64 time;\n" +
"    Int16 wd;\n" +
"    Float32 wspd;\n" +
"    Float32 gst;\n" +
"    Float32 wvht;\n" +
"    Float32 dpd;\n" +
"    Float32 apd;\n" +
"    Int16 mwd;\n" +
"    Float32 bar;\n" +
"    Float32 atmp;\n" +
"    Float32 wtmp;\n" +
"    Float32 dewp;\n" +
"    Float32 vis;\n" +
"    Float32 ptdy;\n" +
"    Float32 tide;\n" +
"    Float32 wspu;\n" +
"    Float32 wspv;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //*** test make data files
        String2.log("\n****************** EDDTableFromNcFiles.test4D make DATA FILES\n");       

        //.csv
        //from NdbcMetStation.test31201
        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2005 04 19 00 00 999 99.0 99.0  1.40  9.00 99.00 999 9999.0 999.0  24.4 999.0 99.0 99.00 first available
        //double seconds = Calendar2.isoStringToEpochSeconds("2005-04-19T00");
        //int row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        //Test.ensureEqual(table.getStringData(sosOfferingIndex, row), "31201", "");
        //Test.ensureEqual(table.getFloatData(latIndex, row), -27.7f, "");
        //Test.ensureEqual(table.getFloatData(lonIndex, row), -48.13f, "");

        
        //2011-04-12 was -48.13&latitude=-27.7  now 27.705 S 48.134 W
        userDapQuery = "longitude,latitude,time,station,wvht,dpd,wtmp,dewp" +
            "&longitude=-48.134&latitude=-27.705&time=2005-04-19T00";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data1", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude,latitude,time,station,wvht,dpd,wtmp,dewp\n" +
"degrees_east,degrees_north,UTC,,m,s,degree_C,degree_C\n" +
"-48.134,-27.705,2005-04-19T00:00:00Z,31201,1.4,9.0,24.4,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2005 04 25 18 00 999 99.0 99.0  3.90  8.00 99.00 999 9999.0 999.0  23.9 999.0 99.0 99.00
        userDapQuery = "longitude,latitude,time,station,wvht,dpd,wtmp,dewp" +
            "&longitude=-48.134&latitude=-27.705&time>=2005-04-01&time<=2005-04-26";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data2", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = "longitude,latitude,time,station,wvht,dpd,wtmp,dewp\n";
        Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);
        expected = "degrees_east,degrees_north,UTC,,m,s,degree_C,degree_C\n";
        Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);
        expected = "-48.134,-27.705,2005-04-19T00:00:00Z,31201,1.4,9.0,24.4,NaN\n"; //time above
        Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);
        expected = "-48.134,-27.705,2005-04-25T18:00:00Z,31201,3.9,8.0,23.9,NaN\n"; //this time
        Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);

        //test requesting a lat lon area
        userDapQuery = "longitude,latitude,time,station,wvht,dpd,wtmp,dewp" +
            "&longitude>-125&longitude<-121&latitude>35&latitude<39&time=2005-04-01";
        long time = System.currentTimeMillis();
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data3", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = //changed 2011-04-12 after reprocessing everything: 
                   //more precise lat lon: from mostly 2 decimal digits to mostly 3.
"longitude,latitude,time,station,wvht,dpd,wtmp,dewp\n" +
"degrees_east,degrees_north,UTC,,m,s,degree_C,degree_C\n" +
"-122.881,37.363,2005-04-01T00:00:00Z,46012,2.55,12.5,13.7,NaN\n" +
"-123.301,38.242,2005-04-01T00:00:00Z,46013,2.3,12.9,13.9,NaN\n" +
"-122.833,37.759,2005-04-01T00:00:00Z,46026,1.96,12.12,14.0,NaN\n" +
"-121.884,35.741,2005-04-01T00:00:00Z,46028,2.57,12.9,16.3,NaN\n" +
"-122.404,36.789,2005-04-01T00:00:00Z,46042,2.21,17.39,14.5,NaN\n" +
"-121.899,36.835,2005-04-01T00:00:00Z,46091,NaN,NaN,NaN,NaN\n" +
"-122.02,36.75,2005-04-01T00:00:00Z,46092,NaN,NaN,NaN,NaN\n" +
"-122.41,36.69,2005-04-01T00:00:00Z,46093,NaN,NaN,14.3,NaN\n" +
"-123.47,37.945,2005-04-01T00:00:00Z,46214,2.5,9.0,12.8,NaN\n" +
"-122.298,37.772,2005-04-01T00:00:00Z,AAMC1,NaN,NaN,15.5,NaN\n" +
"-123.708,38.913,2005-04-01T00:00:00Z,ANVC1,NaN,NaN,NaN,NaN\n" +
"-122.465,37.807,2005-04-01T00:00:00Z,FTPC1,NaN,NaN,NaN,NaN\n" +
"-121.888,36.605,2005-04-01T00:00:00Z,MTYC1,NaN,NaN,15.1,NaN\n" +
"-122.038,38.057,2005-04-01T00:00:00Z,PCOC1,NaN,NaN,14.9,NaN\n" +
"-123.74,38.955,2005-04-01T00:00:00Z,PTAC1,NaN,NaN,NaN,NaN\n" +
"-122.4,37.928,2005-04-01T00:00:00Z,RCMC1,NaN,NaN,14.0,NaN\n" +
"-122.21,37.507,2005-04-01T00:00:00Z,RTYC1,NaN,NaN,14.2,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //test that constraint vars are sent to low level data request
        userDapQuery = "longitude,latitude,station,wvht,dpd,wtmp,dewp" + //no "time" here
            "&longitude>-125&longitude<-121&latitude>35&latitude<39&time=2005-04-01"; //"time" here
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data4", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = //changed 2011-04-12 after reprocessing everything: 
                   //more precise lat lon: from mostly 2 decimal digits to mostly 3.
"longitude,latitude,station,wvht,dpd,wtmp,dewp\n" +
"degrees_east,degrees_north,,m,s,degree_C,degree_C\n" +
"-122.881,37.363,46012,2.55,12.5,13.7,NaN\n" +
"-123.301,38.242,46013,2.3,12.9,13.9,NaN\n" +
"-122.833,37.759,46026,1.96,12.12,14.0,NaN\n" +
"-121.884,35.741,46028,2.57,12.9,16.3,NaN\n" +
"-122.404,36.789,46042,2.21,17.39,14.5,NaN\n" +
"-121.899,36.835,46091,NaN,NaN,NaN,NaN\n" +
"-122.02,36.75,46092,NaN,NaN,NaN,NaN\n" +
"-122.41,36.69,46093,NaN,NaN,14.3,NaN\n" +
"-123.47,37.945,46214,2.5,9.0,12.8,NaN\n" +
"-122.298,37.772,AAMC1,NaN,NaN,15.5,NaN\n" +
"-123.708,38.913,ANVC1,NaN,NaN,NaN,NaN\n" +
"-122.465,37.807,FTPC1,NaN,NaN,NaN,NaN\n" +
"-121.888,36.605,MTYC1,NaN,NaN,15.1,NaN\n" +
"-122.038,38.057,PCOC1,NaN,NaN,14.9,NaN\n" +
"-123.74,38.955,PTAC1,NaN,NaN,NaN,NaN\n" +
"-122.4,37.928,RCMC1,NaN,NaN,14.0,NaN\n" +
"-122.21,37.507,RTYC1,NaN,NaN,14.2,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //test that constraint vars are sent to low level data request
        //and that constraint causing 0rows for a station doesn't cause problems
        userDapQuery = "longitude,latitude,wtmp&time>=2008-03-14T18:00:00Z&time<=2008-03-14T18:00:00Z&wtmp>20";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data5", ".csv"); 
        String2.log("queryTime=" + (System.currentTimeMillis() - time));
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = //changed 2011-04-12 after reprocessing everything: 
                   //more precise lat lon: from mostly 2 decimal digits to mostly 3.
"longitude,latitude,wtmp\n" +
"degrees_east,degrees_north,degree_C\n" +
"-80.166,28.519,21.2\n" +
"-78.471,28.906,23.7\n" +
"-80.533,30.041,20.1\n" +
"-46.008,14.357,25.3\n" +
"-64.966,21.061,25.7\n" +
"-71.491,27.469,23.8\n" +
"-69.649,31.978,22.0\n" +
"-80.53,28.4,21.6\n" +
"-80.225,27.551,21.7\n" +
"-89.658,25.888,24.1\n" +
"-93.666,25.79,23.9\n" +
"-85.612,26.044,26.1\n" +
"-94.046,22.017,24.4\n" +
"-85.059,19.874,26.8\n" +
"-67.472,15.054,26.4\n" +
"-84.245,27.34,20.2\n" +
"-88.09,29.06,21.7\n" +
"-157.808,17.094,24.3\n" +
"-160.66,19.087,24.7\n" +
"-152.382,17.525,24.0\n" +
"-153.913,0.0,25.0\n" +
"-158.116,21.673,24.3\n" +
"-157.668,21.417,24.2\n" +
"144.789,13.354,28.1\n" +
"-90.42,29.789,20.4\n" +
"-64.92,18.335,27.7\n" +
"-81.872,26.647,22.2\n" +
"-80.097,25.59,23.5\n" +
"-156.472,20.898,25.0\n" +
"167.737,8.737,27.6\n" +
"-81.808,24.553,23.9\n" +
"-80.862,24.843,23.8\n" +
"-64.753,17.697,26.0\n" +
"-67.047,17.972,27.1\n" +
"-80.38,25.01,24.2\n" +
"-81.807,26.13,23.7\n" +
"-170.688,-14.28,29.6\n" +
"-157.867,21.307,25.5\n" +
"-96.388,28.452,20.1\n" +
"-82.773,24.693,22.8\n" +
"-97.215,26.06,20.1\n" +
"-82.627,27.76,21.7\n" +
"-66.117,18.462,28.3\n" +
"-177.36,28.212,21.8\n" +
"-80.593,28.415,22.7\n" +
"166.618,19.29,27.9\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

    }

    /**
     * This is run by hand (special setup) to test dealing with the last 24 hours.
     * Before running this, run NDBCMet updateLastNDays, then copy some files to /u00/data/points/ndbcMet
     * so files have very recent data.
     *
     * @throws Throwable if trouble
     */
    public static void test24Hours() throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.test24Hours() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;

        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, "cwwcNDBCMet"); 

        //!!!change time to be ~nowLocal+16 (= nowZulu+8);  e.g., T20 for local time 4pm
        userDapQuery = "longitude,latitude,time,station,wd,wtmp&time%3E=2009-03-12T20"; 
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_24hours", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        String2.log(results);

        //in log output, look at end of constructor for "maxTime is within last 24hrs, so setting maxTime to NaN (i.e., Now)."
        //in log output, look for stations saying "file maxTime is within last 24hrs, so ERDDAP is pretending file maxTime is now+4hours."
    }


    /**
     * This test &amp;distinct().
     *
     * @throws Throwable if trouble
     */
    public static void testDistinct() throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.testDistinct() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, "cwwcNDBCMet"); 

        //time constraints force erddap to get actual data, (not just station variables)
        //  and order of variables says to sort by lon first
        userDapQuery = "longitude,latitude,station&station=~\"5.*\"&time>=2008-03-11&time<2008-03-12&distinct()"; 
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_distincts", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = //2011-04-12 changed with reprocessing. Mostly 2 to mostly 3 decimal digits
"longitude,latitude,station\n" +
"degrees_east,degrees_north,\n" +
"-162.279,23.445,51001\n" +
"-162.058,24.321,51101\n" +
"-160.66,19.087,51003\n" +
"-158.116,21.673,51201\n" +
"-157.808,17.094,51002\n" +
"-157.668,21.417,51202\n" +
"-157.01,20.788,51203\n" +
"-153.913,0.0,51028\n" +
"-152.382,17.525,51004\n" +
"144.789,13.354,52200\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //if no time constraint, erddap can use SUBSET_FILENAME
        String2.log("\n* now testing just subset variables");
        userDapQuery = "longitude,latitude,station&station=~\"5.*\"&distinct()"; 
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_distincts2", ".nc");  //nc so test metadata
        results = NcHelper.dumpString(EDStatic.fullTestCacheDirectory + tName, true);
        //String2.log(results);
        expected = //note sorted by lon first (because of &distinct()), not order in subset file
           //2011-04-12 lots of small changes due to full reprocessing
           //2014-08-07 small changes
"netcdf EDDTableFromNcFiles_distincts2.nc {\n" +
"  dimensions:\n" +
"    row = 26;\n" +
"    station_strlen = 5;\n" +
"  variables:\n" +
"    float longitude(row=26);\n" +
"      :_CoordinateAxisType = \"Lon\";\n" +
"      :actual_range = -170.493f, 171.395f; // float\n" +
"      :axis = \"X\";\n" +
"      :comment = \"The longitude of the station.\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Longitude\";\n" +
"      :standard_name = \"longitude\";\n" +
"      :units = \"degrees_east\";\n" +
"\n" +
"    float latitude(row=26);\n" +
"      :_CoordinateAxisType = \"Lat\";\n" +
"      :actual_range = -14.265f, 24.321f; // float\n" +
"      :axis = \"Y\";\n" +
"      :comment = \"The latitude of the station.\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Latitude\";\n" +
"      :standard_name = \"latitude\";\n" +
"      :units = \"degrees_north\";\n" +
"\n" +
"    char station(row=26, station_strlen=5);\n" +
"      :cf_role = \"timeseries_id\";\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"Station Name\";\n" +
"\n" +
"  // global attributes:\n" +
"  :acknowledgement = \"NOAA NDBC and NOAA CoastWatch (West Coast Node)\";\n" +
"  :cdm_data_type = \"TimeSeries\";\n" +
"  :cdm_timeseries_variables = \"station, longitude, latitude\";\n" +
"  :contributor_name = \"NOAA NDBC and NOAA CoastWatch (West Coast Node)\";\n" +
"  :contributor_role = \"Source of data.\";\n" +
"  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"  :creator_email = \"erd.data@noaa.gov\";\n" +
"  :creator_name = \"NOAA NMFS SWFSC ERD\";\n" +
"  :creator_url = \"http://www.pfeg.noaa.gov\";\n" +
"  :Easternmost_Easting = 171.395f; // float\n" +
"  :featureType = \"TimeSeries\";\n" +
"  :geospatial_lat_max = 24.321f; // float\n" +
"  :geospatial_lat_min = -14.265f; // float\n" +
"  :geospatial_lat_units = \"degrees_north\";\n" +
"  :geospatial_lon_max = 171.395f; // float\n" +
"  :geospatial_lon_min = -170.493f; // float\n" +
"  :geospatial_lon_units = \"degrees_east\";\n" +
"  :geospatial_vertical_positive = \"down\";\n" +
"  :geospatial_vertical_units = \"m\";\n" +
"  :history = \"NOAA NDBC\n" +
today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

//+ " http://www.ndbc.noaa.gov/\n" +
//today + " http://127.0.0.1:8080/...

expected = 
"http://127.0.0.1:8080/cwexperimental/tabledap/cwwcNDBCMet.nc\\?longitude,latitude,station&station=~\\\\\"5\\.\\*\\\\\"&distinct\\(\\)\";\n" +
"  :id = \"EDDTableFromNcFiles_distincts2\";\n" +
"  :infoUrl = \"http://www.ndbc.noaa.gov/\";\n" +
"  :institution = \"NOAA NDBC, NOAA NMFS SWFSC ERD\";\n" +
"  :keywords = \"Atmosphere > Air Quality > Visibility,\n" +
"Atmosphere > Altitude > Planetary Boundary Layer Height,\n" +
"Atmosphere > Atmospheric Pressure > Atmospheric Pressure Measurements,\n" +
"Atmosphere > Atmospheric Pressure > Pressure Tendency,\n" +
"Atmosphere > Atmospheric Pressure > Sea Level Pressure,\n" +
"Atmosphere > Atmospheric Pressure > Static Pressure,\n" +
"Atmosphere > Atmospheric Temperature > Air Temperature,\n" +
"Atmosphere > Atmospheric Temperature > Dew Point Temperature,\n" +
"Atmosphere > Atmospheric Water Vapor > Dew Point Temperature,\n" +
"Atmosphere > Atmospheric Winds > Surface Winds,\n" +
"Oceans > Ocean Temperature > Sea Surface Temperature,\n" +
"Oceans > Ocean Waves > Significant Wave Height,\n" +
"Oceans > Ocean Waves > Swells,\n" +
"Oceans > Ocean Waves > Wave Period,\n" +
"air, air_pressure_at_sea_level, air_temperature, atmosphere, atmospheric, average, boundary, buoy, coastwatch, data, dew point, dew_point_temperature, direction, dominant, eastward, eastward_wind, from, gust, height, identifier, layer, level, measurements, meridional, meteorological, meteorology, name, ndbc, noaa, northward, northward_wind, ocean, oceans, period, planetary, pressure, quality, sea, sea level, sea_surface_swell_wave_period, sea_surface_swell_wave_significant_height, sea_surface_swell_wave_to_direction, sea_surface_temperature, seawater, significant, speed, sst, standard, static, station, surface, surface waves, surface_altitude, swell, swells, temperature, tendency, tendency_of_air_pressure, time, vapor, visibility, visibility_in_air, water, wave, waves, wcn, wind, wind_from_direction, wind_speed, wind_speed_of_gust, winds, zonal\";\n" +
"  :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
"  :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"  :naming_authority = \"gov.noaa.pfeg.coastwatch\";\n" +
"  :NDBCMeasurementDescriptionUrl = \"http://www.ndbc.noaa.gov/measdes.shtml\";\n" +
"  :Northernmost_Northing = 24.321f; // float\n" +
"  :project = \"NOAA NDBC and NOAA CoastWatch \\(West Coast Node\\)\";\n" +
"  :publisher_email = \"erd.data@noaa.gov\";\n" +
"  :publisher_name = \"NOAA NMFS SWFSC ERD\";\n" +
"  :publisher_url = \"http://www.pfeg.noaa.gov\";\n" +
"  :quality = \"Automated QC checks with periodic manual QC\";\n" +
"  :source = \"station observation\";\n" +
"  :sourceUrl = \"http://www.ndbc.noaa.gov/\";\n" +
"  :Southernmost_Northing = -14.265f; // float\n" +
"  :standard_name_vocabulary = \"CF-12\";\n" +
"  :subsetVariables = \"station, longitude, latitude\";\n" +
"  :summary = \"The National Data Buoy Center \\(NDBC\\) distributes meteorological data from\n" +
"moored buoys maintained by NDBC and others. Moored buoys are the weather\n" +
"sentinels of the sea. They are deployed in the coastal and offshore waters\n" +
"from the western Atlantic to the Pacific Ocean around Hawaii, and from the\n" +
"Bering Sea to the South Pacific. NDBC's moored buoys measure and transmit\n" +
"barometric pressure; wind direction, speed, and gust; air and sea\n" +
"temperature; and wave energy spectra from which significant wave height,\n" +
"dominant wave period, and average wave period are derived. Even the\n" +
"direction of wave propagation is measured on many moored buoys.\n" +
"\n" +
"The data is from NOAA NDBC. It has been reformatted by NOAA Coastwatch,\n" +
"West Coast Node. This dataset only has the data that is closest to a\n" +
"given hour. The time values in the dataset are rounded to the nearest hour.\n" +
"\n" +
"This dataset has both historical data \\(quality controlled, before\n" +
"20.{8}T00:00:00Z\\) and near real time data \\(less quality controlled, from\n" + //changes
"20.{8}T00:00:00Z on\\).\";\n" +  //changes   
"  :time_coverage_end = \"20.{11}:00:00Z\";\n" +
"  :time_coverage_resolution = \"P1H\";\n" +
"  :time_coverage_start = \"1970-02-26T20:00:00Z\";\n" +
"  :title = \"NDBC Standard Meteorological Buoy Data\";\n" +
"  :Westernmost_Easting = -170.493f; // float\n" +
" data:\n" +
"longitude =\n" +
"  \\{-170.493, -162.279, -162.058, -160.66, -159.575, -158.303, -158.124, -158.116, -157.808, -157.753, -157.668, -157.1, -157.01, -156.93, -156.427, -156.1, -154.97, -154.056, -153.913, -153.9, -152.382, -144.668, 144.789, 144.812, 145.662, 171.395\\}\n" +
"latitude =\n" +
"  \\{-14.265, 23.445, 24.321, 19.087, 22.286, 21.096, 21.281, 21.673, 17.094, 21.477, 21.417, 20.4, 20.788, 21.35, 21.019, 20.4, 19.78, 23.546, 0.0, 23.558, 17.525, 13.729, 13.354, 13.683, 15.267, 7.092\\}\n" +
"station =\"51209\", \"51001\", \"51101\", \"51003\", \"51208\", \"51200\", \"51204\", \"51201\", \"51002\", \"51207\", \"51202\", \"51027\", \"51203\", \"51026\", \"51205\", \"51005\", \"51206\", \"51000\", \"51028\", \"51100\", \"51004\", \"52009\", \"52200\", \"52202\", \"52211\", \"52201\"\n" +
"\\}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureLinesMatch(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);

        //if just one var, erddap can use DISTINCT_SUBSET_FILENAME
        String2.log("\n* now testing just distinct subset variables");
        userDapQuery = "longitude&longitude>-154&longitude<-153&distinct()"; 
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_distincts3", ".nc"); //nc so test metadata
        results = NcHelper.dumpString(EDStatic.fullTestCacheDirectory + tName, true);
        //String2.log(results);
        expected = 
"netcdf EDDTableFromNcFiles_distincts3.nc {\n" +
"  dimensions:\n" +
"    row = 3;\n" +
"  variables:\n" +
"    float longitude(row=3);\n" +
"      :_CoordinateAxisType = \"Lon\";\n" +
"      :actual_range = -153.913f, -153.348f; // float\n" +
"      :axis = \"X\";\n" +
"      :comment = \"The longitude of the station.\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Longitude\";\n" +
"      :standard_name = \"longitude\";\n" +
"      :units = \"degrees_east\";\n" +
"\n" +
"  // global attributes:\n" +
"  :acknowledgement = \"NOAA NDBC and NOAA CoastWatch (West Coast Node)\";\n" +
"  :cdm_data_type = \"TimeSeries\";\n" +
"  :cdm_timeseries_variables = \"station, longitude, latitude\";\n" +
"  :contributor_name = \"NOAA NDBC and NOAA CoastWatch (West Coast Node)\";\n" +
"  :contributor_role = \"Source of data.\";\n" +
"  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"  :creator_email = \"erd.data@noaa.gov\";\n" +
"  :creator_name = \"NOAA NMFS SWFSC ERD\";\n" +
"  :creator_url = \"http://www.pfeg.noaa.gov\";\n" +
"  :Easternmost_Easting = -153.348f; // float\n" +
"  :featureType = \"TimeSeries\";\n" +
"  :geospatial_lat_max = 71.502; // double\n" +
"  :geospatial_lat_min = -27.705; // double\n" +
"  :geospatial_lat_units = \"degrees_north\";\n" +
"  :geospatial_lon_max = -153.348f; // float\n" +
"  :geospatial_lon_min = -153.913f; // float\n" +
"  :geospatial_lon_units = \"degrees_east\";\n" +
"  :geospatial_vertical_positive = \"down\";\n" +
"  :geospatial_vertical_units = \"m\";\n" +
"  :history = \"NOAA NDBC\n" +
today; //"2013-05-28T18:14:07Z http://www.ndbc.noaa.gov/
//2013-05-28T18:14:07Z 
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);


expected = 
"http://127.0.0.1:8080/cwexperimental/tabledap/cwwcNDBCMet.nc\\?longitude&longitude>-154&longitude<-153&distinct\\(\\)\";\n" +
"  :id = \"EDDTableFromNcFiles_distincts3\";\n" +
"  :infoUrl = \"http://www.ndbc.noaa.gov/\";\n" +
"  :institution = \"NOAA NDBC, NOAA NMFS SWFSC ERD\";\n" +
"  :keywords = \"Atmosphere > Air Quality > Visibility,\n" +
"Atmosphere > Altitude > Planetary Boundary Layer Height,\n" +
"Atmosphere > Atmospheric Pressure > Atmospheric Pressure Measurements,\n" +
"Atmosphere > Atmospheric Pressure > Pressure Tendency,\n" +
"Atmosphere > Atmospheric Pressure > Sea Level Pressure,\n" +
"Atmosphere > Atmospheric Pressure > Static Pressure,\n" +
"Atmosphere > Atmospheric Temperature > Air Temperature,\n" +
"Atmosphere > Atmospheric Temperature > Dew Point Temperature,\n" +
"Atmosphere > Atmospheric Water Vapor > Dew Point Temperature,\n" +
"Atmosphere > Atmospheric Winds > Surface Winds,\n" +
"Oceans > Ocean Temperature > Sea Surface Temperature,\n" +
"Oceans > Ocean Waves > Significant Wave Height,\n" +
"Oceans > Ocean Waves > Swells,\n" +
"Oceans > Ocean Waves > Wave Period,\n" +
"air, air_pressure_at_sea_level, air_temperature, atmosphere, atmospheric, average, boundary, buoy, coastwatch, data, dew point, dew_point_temperature, direction, dominant, eastward, eastward_wind, from, gust, height, identifier, layer, level, measurements, meridional, meteorological, meteorology, name, ndbc, noaa, northward, northward_wind, ocean, oceans, period, planetary, pressure, quality, sea, sea level, sea_surface_swell_wave_period, sea_surface_swell_wave_significant_height, sea_surface_swell_wave_to_direction, sea_surface_temperature, seawater, significant, speed, sst, standard, static, station, surface, surface waves, surface_altitude, swell, swells, temperature, tendency, tendency_of_air_pressure, time, vapor, visibility, visibility_in_air, water, wave, waves, wcn, wind, wind_from_direction, wind_speed, wind_speed_of_gust, winds, zonal\";\n" +
"  :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
"  :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"  :naming_authority = \"gov.noaa.pfeg.coastwatch\";\n" +
"  :NDBCMeasurementDescriptionUrl = \"http://www.ndbc.noaa.gov/measdes.shtml\";\n" +
"  :Northernmost_Northing = 71.502; // double\n" +
"  :project = \"NOAA NDBC and NOAA CoastWatch \\(West Coast Node\\)\";\n" +
"  :publisher_email = \"erd.data@noaa.gov\";\n" +
"  :publisher_name = \"NOAA NMFS SWFSC ERD\";\n" +
"  :publisher_url = \"http://www.pfeg.noaa.gov\";\n" +
"  :quality = \"Automated QC checks with periodic manual QC\";\n" +
"  :source = \"station observation\";\n" +
"  :sourceUrl = \"http://www.ndbc.noaa.gov/\";\n" +
"  :Southernmost_Northing = -27.705; // double\n" +
"  :standard_name_vocabulary = \"CF-12\";\n" +
"  :subsetVariables = \"station, longitude, latitude\";\n" +
"  :summary = \"The National Data Buoy Center \\(NDBC\\) distributes meteorological data from\n" +
"moored buoys maintained by NDBC and others. Moored buoys are the weather\n" +
"sentinels of the sea. They are deployed in the coastal and offshore waters\n" +
"from the western Atlantic to the Pacific Ocean around Hawaii, and from the\n" +
"Bering Sea to the South Pacific. NDBC's moored buoys measure and transmit\n" +
"barometric pressure; wind direction, speed, and gust; air and sea\n" +
"temperature; and wave energy spectra from which significant wave height,\n" +
"dominant wave period, and average wave period are derived. Even the\n" +
"direction of wave propagation is measured on many moored buoys.\n" +
"\n" +
"The data is from NOAA NDBC. It has been reformatted by NOAA Coastwatch,\n" +
"West Coast Node. This dataset only has the data that is closest to a\n" +
"given hour. The time values in the dataset are rounded to the nearest hour.\n" +
"\n" +
"This dataset has both historical data \\(quality controlled, before\n" +
"20.{8}T00:00:00Z\\) and near real time data \\(less quality controlled, from\n" + //changes
"20.{8}T00:00:00Z on\\).\";\n" +    //changes
"  :time_coverage_end = \"20.{11}:00:00Z\";\n" + //changes
"  :time_coverage_resolution = \"P1H\";\n" +
"  :time_coverage_start = \"1970-02-26T20:00:00Z\";\n" +
"  :title = \"NDBC Standard Meteorological Buoy Data\";\n" +
"  :Westernmost_Easting = -153.913f; // float\n" +
" data:\n" +
"longitude =\n" +
"  \\{-153.913, -153.9, -153.348\\}\n" +
"\\}\n";
        tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureLinesMatch(results.substring(tPo), expected, "results=\n" + results);

    }


    /** This test getting just station ids. */
    public static void testId() throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.testId() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;

        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, "cwwcNDBCMet"); 

        userDapQuery = "station&station>\"5\"&station<\"6\""; 
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_id", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"station\n" +
"\n" +
"51000\n" +
"51001\n" +
"51002\n" +
"51003\n" +
"51004\n" +
"51005\n" +
"51026\n" +
"51027\n" +
"51028\n" +
"51100\n" +
"51101\n" +
"51200\n" +
"51201\n" +
"51202\n" +
"51203\n" +
"51204\n" +
"51205\n" + //added 2012-03-25
"51206\n" + //added 2012-06-29
"51207\n" + //added 2014-08-07
"51208\n" + //added 2014-08-07
"51209\n" + //added 2015-07-31
"52009\n" +
"52200\n" +
"52201\n" +
"52202\n" + //added 2014-08-07
"52211\n";  //added 2014-08-07
        Test.ensureEqual(results, expected, "results=\n" + results);
    }
     
    /**
     * This tests orderBy.
     *
     * @throws Throwable if trouble
     */
    public static void testOrderBy() throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.testOrderBy() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";

        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, "cwwcNDBCMet"); 

        //.csv
        //from NdbcMetStation.test31201
        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2005 04 19 00 00 999 99.0 99.0  1.40  9.00 99.00 999 9999.0 999.0  24.4 999.0 99.0 99.00 first available
        userDapQuery = "time,station,wtmp,atmp&station>\"5\"&station<\"6\"" +
            "&time>=2005-04-19T21&time<2005-04-20&orderBy(\"station,time\")";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_ob", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"time,station,wtmp,atmp\n" +
"UTC,,degree_C,degree_C\n" +
"2005-04-19T21:00:00Z,51001,24.1,23.5\n" +
"2005-04-19T22:00:00Z,51001,24.2,23.6\n" +
"2005-04-19T23:00:00Z,51001,24.2,22.1\n" +
"2005-04-19T21:00:00Z,51002,25.1,25.4\n" +
"2005-04-19T22:00:00Z,51002,25.2,25.4\n" +
"2005-04-19T23:00:00Z,51002,25.2,24.8\n" +
"2005-04-19T21:00:00Z,51003,25.3,23.9\n" +
"2005-04-19T22:00:00Z,51003,25.4,24.3\n" +
"2005-04-19T23:00:00Z,51003,25.4,24.7\n" +
"2005-04-19T21:00:00Z,51004,25.0,24.0\n" +
"2005-04-19T22:00:00Z,51004,25.0,23.8\n" +
"2005-04-19T23:00:00Z,51004,25.0,24.3\n" +
"2005-04-19T21:00:00Z,51028,27.7,27.6\n" +
"2005-04-19T22:00:00Z,51028,27.8,27.1\n" +
"2005-04-19T23:00:00Z,51028,27.8,27.5\n" +
"2005-04-19T21:00:00Z,51201,25.0,NaN\n" +
"2005-04-19T22:00:00Z,51201,24.9,NaN\n" +
"2005-04-19T23:00:00Z,51201,25.0,NaN\n" +
"2005-04-19T21:00:00Z,51202,24.5,NaN\n" +
"2005-04-19T22:00:00Z,51202,24.6,NaN\n" +
"2005-04-19T23:00:00Z,51202,24.5,NaN\n" +
"2005-04-19T21:00:00Z,52200,28.0,NaN\n" +
"2005-04-19T22:00:00Z,52200,28.0,NaN\n" +
"2005-04-19T23:00:00Z,52200,28.0,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv
        //from NdbcMetStation.test31201
        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2005 04 19 00 00 999 99.0 99.0  1.40  9.00 99.00 999 9999.0 999.0  24.4 999.0 99.0 99.00 first available
        userDapQuery = "time,station,wtmp,atmp&station>\"5\"&station<\"6\"" +
            "&time>=2005-04-19T21&time<2005-04-20&orderBy(\"time,station\")";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_ob2", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"time,station,wtmp,atmp\n" +
"UTC,,degree_C,degree_C\n" +
"2005-04-19T21:00:00Z,51001,24.1,23.5\n" +
"2005-04-19T21:00:00Z,51002,25.1,25.4\n" +
"2005-04-19T21:00:00Z,51003,25.3,23.9\n" +
"2005-04-19T21:00:00Z,51004,25.0,24.0\n" +
"2005-04-19T21:00:00Z,51028,27.7,27.6\n" +
"2005-04-19T21:00:00Z,51201,25.0,NaN\n" +
"2005-04-19T21:00:00Z,51202,24.5,NaN\n" +
"2005-04-19T21:00:00Z,52200,28.0,NaN\n" +
"2005-04-19T22:00:00Z,51001,24.2,23.6\n" +
"2005-04-19T22:00:00Z,51002,25.2,25.4\n" +
"2005-04-19T22:00:00Z,51003,25.4,24.3\n" +
"2005-04-19T22:00:00Z,51004,25.0,23.8\n" +
"2005-04-19T22:00:00Z,51028,27.8,27.1\n" +
"2005-04-19T22:00:00Z,51201,24.9,NaN\n" +
"2005-04-19T22:00:00Z,51202,24.6,NaN\n" +
"2005-04-19T22:00:00Z,52200,28.0,NaN\n" +
"2005-04-19T23:00:00Z,51001,24.2,22.1\n" +
"2005-04-19T23:00:00Z,51002,25.2,24.8\n" +
"2005-04-19T23:00:00Z,51003,25.4,24.7\n" +
"2005-04-19T23:00:00Z,51004,25.0,24.3\n" +
"2005-04-19T23:00:00Z,51028,27.8,27.5\n" +
"2005-04-19T23:00:00Z,51201,25.0,NaN\n" +
"2005-04-19T23:00:00Z,51202,24.5,NaN\n" +
"2005-04-19T23:00:00Z,52200,28.0,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

    }

    /**
     * This tests orderByMax.
     *
     * @throws Throwable if trouble
     */
    public static void testOrderByMax() throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.testOrderByMax() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";

        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, "cwwcNDBCMet"); 

        //test orderMyMax(twoVars)
        //from NdbcMetStation.test31201
        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2005 04 19 00 00 999 99.0 99.0  1.40  9.00 99.00 999 9999.0 999.0  24.4 999.0 99.0 99.00 first available
        userDapQuery = "time,station,wtmp,atmp&station>\"5\"&station<\"6\"" +
            "&time>=2005-04-19T21&time<2005-04-20&orderByMax(\"station,time\")";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_obmax", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"time,station,wtmp,atmp\n" +
"UTC,,degree_C,degree_C\n" +
"2005-04-19T23:00:00Z,51001,24.2,22.1\n" +
"2005-04-19T23:00:00Z,51002,25.2,24.8\n" +
"2005-04-19T23:00:00Z,51003,25.4,24.7\n" +
"2005-04-19T23:00:00Z,51004,25.0,24.3\n" +
"2005-04-19T23:00:00Z,51028,27.8,27.5\n" +
"2005-04-19T23:00:00Z,51201,25.0,NaN\n" +
"2005-04-19T23:00:00Z,51202,24.5,NaN\n" +
"2005-04-19T23:00:00Z,52200,28.0,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //test orderMyMax(oneVar)
        userDapQuery = "time,station,wtmp,atmp&station=\"51002\"" +
            "&time>=2005-04-19T21&time<2005-04-20&orderByMax(\"time\")";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_obmax2", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"time,station,wtmp,atmp\n" +
"UTC,,degree_C,degree_C\n" +
"2005-04-19T23:00:00Z,51002,25.2,24.8\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //test orderMyMax(twoVars -- different order)
        userDapQuery = "time,station,wtmp,atmp&station>\"5\"&station<\"6\"" +
            "&time>=2005-04-19T21&time<2005-04-20&orderByMax(\"time,station\")";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_obmax3", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"time,station,wtmp,atmp\n" +
"UTC,,degree_C,degree_C\n" +
"2005-04-19T21:00:00Z,52200,28.0,NaN\n" +
"2005-04-19T22:00:00Z,52200,28.0,NaN\n" +
"2005-04-19T23:00:00Z,52200,28.0,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

    }

    /**
     * This tests orderByMin.
     *
     * @throws Throwable if trouble
     */
    public static void testOrderByMin() throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.testOrderByMin() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";

        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, "cwwcNDBCMet"); 

        //test orderMyMin(twoVars)
        //from NdbcMetStation.test31201
        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2005 04 19 00 00 999 99.0 99.0  1.40  9.00 99.00 999 9999.0 999.0  24.4 999.0 99.0 99.00 first available
        userDapQuery = "time,station,wtmp,atmp&station>\"5\"&station<\"6\"" +
            "&time>=2005-04-19T21&time<2005-04-20&orderByMin(\"station,time\")";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_obmin1", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"time,station,wtmp,atmp\n" +
"UTC,,degree_C,degree_C\n" +
"2005-04-19T21:00:00Z,51001,24.1,23.5\n" +
"2005-04-19T21:00:00Z,51002,25.1,25.4\n" +
"2005-04-19T21:00:00Z,51003,25.3,23.9\n" +
"2005-04-19T21:00:00Z,51004,25.0,24.0\n" +
"2005-04-19T21:00:00Z,51028,27.7,27.6\n" +
"2005-04-19T21:00:00Z,51201,25.0,NaN\n" +
"2005-04-19T21:00:00Z,51202,24.5,NaN\n" +
"2005-04-19T21:00:00Z,52200,28.0,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //test orderMyMin(oneVar)
        userDapQuery = "time,station,wtmp,atmp&station=\"51002\"" +
            "&time>=2005-04-19T21&time<2005-04-20&orderByMin(\"time\")";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_obmin2", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"time,station,wtmp,atmp\n" +
"UTC,,degree_C,degree_C\n" +
"2005-04-19T21:00:00Z,51002,25.1,25.4\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //test orderMyMin(twoVars -- different order)
        userDapQuery = "time,station,wtmp,atmp&station>\"5\"&station<\"6\"" +
            "&time>=2005-04-19T21&time<2005-04-20&orderByMin(\"time,station\")";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_obmin3", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"time,station,wtmp,atmp\n" +
"UTC,,degree_C,degree_C\n" +
"2005-04-19T21:00:00Z,51001,24.1,23.5\n" +
"2005-04-19T22:00:00Z,51001,24.2,23.6\n" +
"2005-04-19T23:00:00Z,51001,24.2,22.1\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

    }

    /**
     * This tests orderByMinMax.
     *
     * @throws Throwable if trouble
     */
    public static void testOrderByMinMax() throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.testOrderByMinMax() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";

        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, "cwwcNDBCMet"); 

        //test orderMyMinMax(twoVars)
        //from NdbcMetStation.test31201
        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2005 04 19 00 00 999 99.0 99.0  1.40  9.00 99.00 999 9999.0 999.0  24.4 999.0 99.0 99.00 first available
        userDapQuery = "time,station,wtmp,atmp&station>\"5\"&station<\"6\"" +
            "&time>=2005-04-19T21&time<2005-04-20&orderByMinMax(\"station,time\")";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_obminmax1", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"time,station,wtmp,atmp\n" +
"UTC,,degree_C,degree_C\n" +
"2005-04-19T21:00:00Z,51001,24.1,23.5\n" +
"2005-04-19T23:00:00Z,51001,24.2,22.1\n" +
"2005-04-19T21:00:00Z,51002,25.1,25.4\n" +
"2005-04-19T23:00:00Z,51002,25.2,24.8\n" +
"2005-04-19T21:00:00Z,51003,25.3,23.9\n" +
"2005-04-19T23:00:00Z,51003,25.4,24.7\n" +
"2005-04-19T21:00:00Z,51004,25.0,24.0\n" +
"2005-04-19T23:00:00Z,51004,25.0,24.3\n" +
"2005-04-19T21:00:00Z,51028,27.7,27.6\n" +
"2005-04-19T23:00:00Z,51028,27.8,27.5\n" +
"2005-04-19T21:00:00Z,51201,25.0,NaN\n" +
"2005-04-19T23:00:00Z,51201,25.0,NaN\n" +
"2005-04-19T21:00:00Z,51202,24.5,NaN\n" +
"2005-04-19T23:00:00Z,51202,24.5,NaN\n" +
"2005-04-19T21:00:00Z,52200,28.0,NaN\n" +
"2005-04-19T23:00:00Z,52200,28.0,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //test orderMyMinMax(oneVar)
        userDapQuery = "time,station,wtmp,atmp&station=\"51002\"" +
            "&time>=2005-04-19T21&time<2005-04-20&orderByMinMax(\"time\")";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_obminmax2", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"time,station,wtmp,atmp\n" +
"UTC,,degree_C,degree_C\n" +
"2005-04-19T21:00:00Z,51002,25.1,25.4\n" +
"2005-04-19T23:00:00Z,51002,25.2,24.8\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //test orderMyMinMax(twoVars -- different order)
        userDapQuery = "time,station,wtmp,atmp&station>\"5\"&station<\"6\"" +
            "&time>=2005-04-19T21&time<2005-04-20&orderByMinMax(\"time,station\")";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_obminmax3", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"time,station,wtmp,atmp\n" +
"UTC,,degree_C,degree_C\n" +
"2005-04-19T21:00:00Z,51001,24.1,23.5\n" +
"2005-04-19T21:00:00Z,52200,28.0,NaN\n" +
"2005-04-19T22:00:00Z,51001,24.2,23.6\n" +
"2005-04-19T22:00:00Z,52200,28.0,NaN\n" +
"2005-04-19T23:00:00Z,51001,24.2,22.1\n"+
"2005-04-19T23:00:00Z,52200,28.0,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

    }

    /**
     * This tests station,lon,lat.
     *
     * @throws Throwable if trouble
     */
    public static void testStationLonLat() throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.testStationLonLat() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";

        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, "cwwcNDBCMet"); 

        //.csv
        //from NdbcMetStation.test31201
        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2005 04 19 00 00 999 99.0 99.0  1.40  9.00 99.00 999 9999.0 999.0  24.4 999.0 99.0 99.00 first available
        userDapQuery = "station,longitude,latitude&distinct()";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_sll", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"station,longitude,latitude\n" +
",degrees_east,degrees_north\n" +
"0Y2W3,-87.313,44.794\n" +
"18CI3,-86.91,41.73\n" +
"20CM4,-86.49,42.09\n" +
"23020,38.5,22.162\n" +
"31201,-48.134,-27.705\n" +
"32012,-85.384,-19.616\n" +
"32301,-105.2,-9.9\n" +
"32302,-85.1,-18.0\n" +
"32487,-77.737,3.517\n" +
"32488,-77.511,6.258\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    }



    /** This tests converting global metadata into data. */
    public static void testGlobal() throws Throwable {
        testVerboseOn();
        String name, baseName, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        int epo;

        //variant of calcofi Subsurface (has additional ID from global:id)
        EDDTable csub = (EDDTableFromNcFiles)oneFromDatasetsXml(null, "testGlobal"); 
        baseName = csub.className() + "Global";
        String csubDapQuery = "&longitude=-106.11667";

        //min max
        edv = csub.findDataVariableByDestinationName("longitude");
        Test.ensureEqual(edv.destinationMin(), -164.0833, "");
        Test.ensureEqual(edv.destinationMax(), -106.1167, "");

        tName = csub.makeNewFileForDapQuery(null, null, csubDapQuery, EDStatic.fullTestCacheDirectory, baseName, ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"ID,line_station,line,station,longitude,latitude,time,depth,chlorophyll,dark,light_percent,NH3,NO2,NO3,oxygen,PO4,pressure,primprod,salinity,silicate,temperature\n" +
",,,,degrees_east,degrees_north,UTC,m,mg m-3,mg m-3 experiment-1,mg m-3 experiment-1,ugram-atoms L-1,ugram-atoms L-1,ugram-atoms L-1,mL L-1,ugram-atoms L-1,dbar,mg m-3 experiment-1,PSU,ugram-atoms L-1,degree_C\n" +
"171_040,171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,0.0,NaN,NaN,NaN,NaN,NaN,NaN,4.53,NaN,NaN,NaN,34.38,NaN,27.74\n" +
"171_040,171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,10.0,NaN,NaN,NaN,NaN,NaN,NaN,4.82,NaN,NaN,NaN,34.39,NaN,27.5\n" +
"171_040,171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,29.0,NaN,NaN,NaN,NaN,NaN,NaN,4.16,NaN,NaN,NaN,34.35,NaN,26.11\n" +
"171_040,171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,48.0,NaN,NaN,NaN,NaN,NaN,NaN,3.12,NaN,NaN,NaN,34.43,NaN,22.64\n" +
"171_040,171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,71.0,NaN,NaN,NaN,NaN,NaN,NaN,0.34,NaN,NaN,NaN,34.63,NaN,17.04\n" +
"171_040,171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,94.0,NaN,NaN,NaN,NaN,NaN,NaN,0.2,NaN,NaN,NaN,34.74,NaN,14.89\n" +
"171_040,171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,118.0,NaN,NaN,NaN,NaN,NaN,NaN,0.3,NaN,NaN,NaN,34.76,NaN,13.69\n" +
"171_040,171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,155.0,NaN,NaN,NaN,NaN,NaN,NaN,0.21,NaN,NaN,NaN,34.79,NaN,12.51\n" +
"171_040,171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,193.0,NaN,NaN,NaN,NaN,NaN,NaN,0.24,NaN,NaN,NaN,34.79,NaN,11.98\n" +
"171_040,171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,239.0,NaN,NaN,NaN,NaN,NaN,NaN,0.35,NaN,NaN,NaN,34.76,NaN,11.8\n" +
"171_040,171_040,171.0,40.0,-106.11667,21.05,1956-12-05T21:00:00Z,286.0,NaN,NaN,NaN,NaN,NaN,NaN,0.19,NaN,NaN,NaN,34.76,NaN,11.42\n";
        Test.ensureEqual(results, expected, "results=\n" + results);


    } //end of testGlobal


    public static void testGenerateBreakUpPostDatasetsXml() throws Throwable {
            //String tFileDir, String tFileNameRegex, String sampleFileName, 
            //int tReloadEveryNMinutes,
            //String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex,
            //String tColumnNameForExtract, String tSortedColumnSourceName,
            //String tSortFilesBySourceNames, 
            //String tInfoUrl, String tInstitution, String tSummary, String tTitle,
            //Attributes externalAddGlobalAttributes) 
        String2.log(generateDatasetsXml(
            "c:/u00/cwatch/erddap2/copy/tcPostDet3/",
            ".*\\.nc", 
            "c:/u00/cwatch/erddap2/copy/tcPostDet3/Barbarax20Block/LAMNAx20DITROPIS/Nx2fA.nc",
            "",
            100000000, 
            "", "", "", "", "unique_tag_id",
            "PI, scientific_name, stock", 
            "", "", "", "",
            new Attributes()));
    }


    /** NOT FOR GENERAL USE. Bob used this to generate the GTSPP datasets.xml content.
     */
    public static void bobGenerateGtsppDatasetsXml() throws Throwable {
        String2.log(EDDTableFromNcFiles.generateDatasetsXml(
        "c:/data/gtspp/bestNcConsolidated/", ".*\\.nc", 
        "c:/data/gtspp/bestNcConsolidated/2010/01/2010-01_0E_-60N.nc.nc", 
        "",
        DEFAULT_RELOAD_EVERY_N_MINUTES, 
        "", "\\.nc", ".*", //tPreExtractRegex, tPostExtractRegex, tExtractRegex
        "station_id", "depth", //tColumnNameForExtract, tSortedColumnSourceName
        "time station_id", //tSortFilesBySourceNames
        "http://www.nodc.noaa.gov/GTSPP/", "NOAA NODC", //tInfoUrl, tInstitution        
        "put the summary here", //summary
        "Global Temperature-Salinity Profile Program", //tTitle
        new Attributes())); //externalAddGlobalAttributes) 
    }

    /** NOT FOR GENERAL USE. Bob uses this to consolidate the individual GTSPP
     * data files into 30° x 30° x 1 month files (tiles).
     * 30° x 30° leads to 12x6=72 files for a given time point, so a request
     * for a short time but entire world opens ~72 files.
     * There are ~240 months worth of data, so a request for a small lon lat 
     * range for all time opens ~240 files.
     *
     * <p>Why tile? Because there are ~10^6 profiles/year now, so ~10^7 total.
     * And if 100 bytes of info per file for EDDTableFromFiles fileTable, that's 1 GB!.
     * So there needs to be fewer files.
     * We want to balance number of files for 1 time point (all region tiles), 
     * and number of time point files (I'll stick with their use of 1 month).
     * The tiling size selected is ok, but searches for single profile (by name)
     * are slow since a given file may have a wide range of station_ids.
     *
     * <p>Quality flags
     * <br>http://www.nodc.noaa.gov/GTSPP/document/qcmans/GTSPP_RT_QC_Manual_20090916.pdf
     * <br>http://www.ifremer.fr/gosud/formats/gtspp_qcflags.htm
     * <br>CODE  SIGNIFICATION
     * <br>0     NOT CONTROLLED VALUE
     * <br>1     CORRECT VALUE
     * <br>2     VALUE INCONSISTENT WITH STATISTICS
     * <br>3     DOUBTFUL VALUE (spike, ...)
     * <br>4     FALSE VALUE (out of scale, constant profile, vertical instability, ...)
     * <br>5     VALUE MODIFIED DURING QC (only for interpolate location or date)
     * <br>6-8   Not USED
     * <br>9     NO VALUE
     * <br>
     * <br>I interpret as: okay values are 1, 2, 5
     *
     * @param firstYear  e.g., 1990
     * @param firstMonth e.g., 1  (1..)
     * @param lastYear  e.g., 2010
     * @param lastMonth e.g., 12  (1..)     
     * @param testMode if true, this just processes .nc files 
     *    already in testTempDir f:/data/gtspp/testTemp/
     *    and puts results in testDestDir f:/data/gtspp/testDest/.
     *    So the first/last/Year/Month params are ignored.
     */
    public static void bobConsolidateGtsppTgz(int firstYear, int firstMonth,
        int lastYear, int lastMonth, boolean testMode) throws Throwable {

        int chunkSize = 45;  //lon width, lat height of a tile, in degrees
        int minLat = -90;
        int maxLat = 90;
        int minLon = -180;
        int maxLon = 180;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10); //to nearest day
        String sevenZip    = "c:\\progra~1\\7-Zip\\7z";
        String zipDir      = "c:\\data\\gtspp\\bestNcZip\\"; //gtspp_at199001.tgz
        String destDir     = "c:\\data\\gtspp\\bestNcConsolidated\\";
        String tempDir     = "c:\\data\\gtspp\\temp\\"; 
        String testTempDir = "c:\\data\\gtspp\\testTemp\\"; //tempDir if testMode=true 
        String testDestDir = "c:\\data\\gtspp\\testDest\\"; //destDir if testMode=true
        String logFile     = "c:\\data\\gtspp\\log" + 
            String2.replaceAll(today, "-", "") + ".txt"; 
        File2.makeDirectory(tempDir);
        //http://www.nodc.noaa.gov/GTSPP/document/qcmans/qcflags.htm
        //1=correct, 2=probably correct, 5=modified (so now correct)
        //pre 2012-04-15 was {1,2,5}
        //pre 2012-05-25 was {1,2}
        int okQF[] = {1,2,5}; 
        String okQFCsv = String2.toCSSVString(okQF);
        float depthMV       = 99999; //was -99;
        float temperatureMV = 99999; //was -99;
        float salinityMV    = 99999; //was -99;
        int qMV = 9;
        String timeUnits = "days since 1900-01-01 00:00:00"; //causes roundoff error(!)
        double timeBaseAndFactor[] = Calendar2.getTimeBaseAndFactor(timeUnits);
        //impossible values:
        float minDepth       = -0.4f, maxDepth = 10000;  //-0.4 allows for imprecise values
        float minTemperature = -4,    maxTemperature = 40;
        float minSalinity    = 0,     maxSalinity = 41;


        if (testMode) {
            firstYear = 1990; firstMonth = 1;
            lastYear = 1990; lastMonth = 1;
        }

        SSR.verbose = false;
        
        String2.setupLog(true, false, 
            logFile, false, 1000000000);
        String2.log("*** starting bobConsolidateGtsppTgz " + 
            Calendar2.getCurrentISODateTimeStringLocal() + "\n" +
            "logFile=" + String2.logFileName() + "\n" +
            String2.standardHelpAboutMessage());
        long elapsedTime = System.currentTimeMillis();
        //q_pos (position quality flag), q_date_time (time quality flag)
        int stationCol = -1, organizationCol = -1, dataTypeCol = -1, 
            platformCol = -1, cruiseCol = -1,
            longitudeCol = -1, latitudeCol = -1, timeCol = -1, 
            depthCol = -1, temperatureCol = -1, salinityCol = -1;
        int totalNGoodStation = 0, totalNGoodPos = 0, totalNGoodTime = 0, 
            totalNGoodDepth = 0, totalNGoodTemperature = 0, totalNGoodSalinity = 0;
        int totalNBadStation = 0, totalNBadPos = 0, totalNBadTime = 0, 
            totalNBadDepth = 0, totalNBadTemperature = 0, totalNBadSalinity = 0,
            totalNWarnings = 0, totalNExceptions = 0;
        long totalNGoodRows = 0, totalNBadRows = 0;
        StringArray impossibleNanLat = new StringArray();
        StringArray impossibleMinLat = new StringArray();
        StringArray impossibleMaxLat = new StringArray();
        StringArray impossibleNanLon = new StringArray();
        StringArray impossibleMinLon = new StringArray();
        StringArray impossibleMaxLon = new StringArray();
        //StringArray impossibleNaNDepth = new StringArray();
        StringArray impossibleMinDepth = new StringArray();
        StringArray impossibleMaxDepth = new StringArray();
        //StringArray impossibleNanTemperature = new StringArray();
        StringArray impossibleMinTemperature = new StringArray();
        StringArray impossibleMaxTemperature = new StringArray();
        //StringArray impossibleNanSalinity = new StringArray();
        StringArray impossibleMinSalinity = new StringArray();
        StringArray impossibleMaxSalinity = new StringArray();
        int nLons = 0, nLats = 0, nFiles = 0;
        int lonSum = 0, latSum = 0;
        long profilesSum = 0;
        long rowsSum = 0;


        //*** process a month's data
        int year = firstYear;
        int month = firstMonth;
        long chunkTime = System.currentTimeMillis();
        while (year <= lastYear) {
            String2.log("\n*** " + Calendar2.getCurrentISODateTimeStringLocal() +
                " start processing year=" + year + " month=" + month);

            String zMonth  = String2.zeroPad("" + month,       2);
            String zMonth1 = String2.zeroPad("" + (month + 1), 2);
            double minEpochSeconds = Calendar2.isoStringToEpochSeconds(year + "-" + zMonth  + "-01");
            double maxEpochSeconds = Calendar2.isoStringToEpochSeconds(year + "-" + zMonth1 + "-01");                   

            //destination directory
            String tDestDir = testMode? testDestDir : destDir + year + "\\" + zMonth + "\\";
            File2.makeDirectory(tDestDir);
            HashMap tableHashMap = new HashMap();
            //make sure all files are deleted 
            int waitSeconds = 2;
            int nAttempts = 10;
            long cmdTime = System.currentTimeMillis();
            String cmd = "del/q " + tDestDir + "*.*"; 
            for (int attempt = 0; attempt < nAttempts; attempt++) {
                if (attempt % 8 == 0) {
                    String2.log(cmd);
                    SSR.dosShell(cmd, 30*60); //10 minutes*60 seconds
                    //File2.deleteAllFiles(tempDir);  //previous method
                }
                Math2.gc(waitSeconds * 1000); //gtspp: give OS time to settle
                File destDirFile = new File(tDestDir);
                File files[] = destDirFile.listFiles();
                String2.log("  nRemainingFiles=" + files.length);
                if (files.length == 0)
                    break;
                waitSeconds = 2 * nAttempts;
            }
            String2.log("  cmd total time=" + 
                Calendar2.elapsedTimeString(System.currentTimeMillis() - cmdTime));


            //unzip all atlantic, indian, and pacific .zip files for that month 
            String region2[] = {"at", "in", "pa"};
            int nRegions = testMode? 1 : 3;
            for (int region = 0; region < nRegions; region++) {
                String sourceBaseName = "gtspp4_" + region2[region] + year + zMonth;
                String sourceZipJustFileName = sourceBaseName + ".tgz";
                String sourceZipName = zipDir + sourceZipJustFileName;

                if (!testMode) {

                    //delete all files in tempDir
                    waitSeconds = 2;
                    nAttempts = 10;
                    cmdTime = System.currentTimeMillis();
                    cmd = "del/q " + tempDir + "*.*"; 
                    String2.log(""); //blank line
                    for (int attempt = 0; attempt < nAttempts; attempt++) {
                        String2.log(cmd);
                        SSR.dosShell(cmd, 30*60); //30 minutes*60 seconds
                        //File2.deleteAllFiles(tempDir);  //previous method

                        //delete dirs too
                        File2.deleteAllFiles(tempDir, true, true);
 
                        Math2.gc(waitSeconds * 1000); //gtspp: give OS time to settle
                        String2.log("  " + Math2.memoryString());
                        File tempDirFile = new File(tempDir);
                        File files[] = tempDirFile.listFiles();
                        String2.log("  nRemainingFiles=" + files.length);
                        if (files.length == 0)
                            break;
                        waitSeconds = 2 * nAttempts;
                    }
                    String2.log("  cmd total time=" + 
                        Calendar2.elapsedTimeString(System.currentTimeMillis() - cmdTime));

                    //unzip file into tempDir         //gtspp_at199001.zip
                    cmd = sevenZip + " -y e " + sourceZipName + " -o" + tempDir + " -r"; 
                    cmdTime = System.currentTimeMillis();
                    String2.log("\n*** " + cmd);
                    SSR.dosShell(cmd, 30*60); //10 minutes*60 seconds
                    String2.log("  cmd time=" + 
                        Calendar2.elapsedTimeString(System.currentTimeMillis() - cmdTime));

                    //extract from the .tar file   //gtspp4_at199001.tar
                    cmd = sevenZip + " -y e " + tempDir + sourceBaseName + ".tar -o" + tempDir + " -r"; 
                    cmdTime = System.currentTimeMillis();
                    String2.log("\n*** " + cmd);
                    SSR.dosShell(cmd, 120*60); //120 minutes*60 seconds
                    String2.log("  cmd time=" + 
                        Calendar2.elapsedTimeString(System.currentTimeMillis() - cmdTime));
                    
                    //previous method
                    //SSR.unzip(sourceZipName,
                    //    tempDir, true, 100 * 60); //ignoreZipDirectories, timeOutSeconds 100 minutes
                }

                //read each file and put data in proper table
                String tTempDir = testMode? testTempDir : tempDir;
                File tTempDirAsFile = new File(tTempDir);
                String sourceFileNames[] = tTempDirAsFile.list(); //just the file names
                String2.log("\nunzipped " + sourceFileNames.length + " files");
                int nSourceFileNames = //testMode? 100 : 
                    sourceFileNames.length;
                int nGoodStation = 0, nGoodPos = 0, nGoodTime = 0, 
                    nGoodDepth = 0, nGoodTemperature = 0, nGoodSalinity = 0, nGoodRows = 0;
                int nBadStation = 0, nBadPos = 0, nBadTime = 0, 
                    nBadDepth = 0, nBadTemperature = 0, nBadSalinity = 0, nBadRows = 0,
                    nWarnings = 0, nExceptions = 0;
                long fileReadTime = System.currentTimeMillis();
                profilesSum += nSourceFileNames;
                for (int sfi = 0; sfi < nSourceFileNames; sfi++) {
                    String sourceFileName = sourceFileNames[sfi];
                    if (sfi % 10000 == 0) {
                        //if (sfi > 0)    //2012-12-13 commented out. Let Java handle it.
                        //    Math2.gc(3 * 1000); //gtspp: give OS time to settle
                        //high water mark is ~160 MB, so memory not a problem
                        String2.log("file #" + sfi + " " + Math2.memoryString());
                    }

                    if (!sourceFileName.endsWith(".nc")) {
                        //String2.log("ERROR: not a .nc file: " + sourceFileName);
                        continue;
                    }

                    NetcdfFile ncFile = null; 

                    try {
                        //get the station name
                        //gtspp_13635162_te_111.nc  gtspp_10313692_cu_111.nc
                        if (!sourceFileName.matches("gtspp_[0-9]+_.*\\.nc")) { //was "\\d+")) {//all digits
                            nBadStation++;
                            throw new SimpleException("Invalid sourceFileName=" + sourceFileName);
                        }
                        int po = sourceFileName.indexOf('_', 6);
                        if (po < 0) {
                            nBadStation++;
                            throw new SimpleException("Invalid sourceFileName=" + sourceFileName);
                        }
                        int station = String2.parseInt(sourceFileName.substring(6, po));
                        nGoodStation++;
                        String key = sourceZipJustFileName + " " + sourceFileName;

                        //open the file
                        ncFile = NcHelper.openFile(tTempDir + sourceFileName);
                        Variable var;
                        Attributes tVarAtts = new Attributes();
                        String tUnits;

                        //get all of the data 

                        //stream_ident
                        var = ncFile.findVariable("stream_ident");                        
                        String organization = "";
                        String dataType = "";
                        if (var == null) {
                            nWarnings++;
                            String2.log("WARNING: No stream_ident in " + sourceFileName);
                        } else {
                            PrimitiveArray streamPA = NcHelper.getPrimitiveArray(var);
                            if (streamPA instanceof StringArray && streamPA.size() > 0) {
                                String stream = streamPA.getString(0);
                                if (stream.length() >= 4) {
                                    organization = stream.substring(0, 2).trim();
                                    dataType = stream.substring(2, 4).trim();
                                } else {
                                    String2.log("WARNING: stream_ident isn't a 4 char string: " + stream);
                                }
                            } else {
                                String2.log("WARNING: stream_ident isn't a StringArray: " + 
                                    streamPA.toString());
                            }
                        }

                        //platform_code
                        var = ncFile.findVariable("gtspp_platform_code");                        
                        String platform = "";
                        if (var == null) {
                            //a small percentage have this problem
                            //nWarnings++;
                            //String2.log("WARNING: No gtspp_platform_code in " + sourceFileName);
                        } else {
                            PrimitiveArray pa = NcHelper.getPrimitiveArray(var);
                            if (pa instanceof StringArray && pa.size() > 0) {
                                platform = pa.getString(0).trim();
                                //String2.log("platform_code=" + platform_code);
                            } else {
                                String2.log("WARNING: gtspp_platform_code isn't a StringArray: " + 
                                    pa.toString());
                            }
                        }

                        //cruise
                        var = ncFile.findVariable("cruise_id");                        
                        String cruise = "";
                        if (var == null) {
                            nWarnings++;
                            String2.log("WARNING: No cruise_id in " + sourceFileName);
                        } else {
                            PrimitiveArray cruisePA = NcHelper.getPrimitiveArray(var);
                            if (cruisePA instanceof StringArray && cruisePA.size() > 0) {
                                cruise = cruisePA.getString(0).trim();
                            } else {
                                String2.log("WARNING: cruise_id isn't a StringArray: " + 
                                    cruisePA.toString());
                            }
                        }

                        //prof_type  is TEMP or PSAL so don't save it.
                        /*var = ncFile.findVariable("prof_type");                        
                        String prof_type = "";
                        if (var == null) {
                            nWarnings++;
                            String2.log("WARNING: No prof_type in " + sourceFileName);
                        } else {
                            PrimitiveArray pa = NcHelper.getPrimitiveArray(var);
                            if (pa instanceof StringArray && pa.size() > 0) {
                                prof_type = pa.getString(0).trim();
                                String2.log("prof_type=" + prof_type);
                            } else {
                                String2.log("WARNING: prof_type isn't a StringArray: " + 
                                    pa.toString());
                            }
                        }*/

                        //position quality flag 
                        var = ncFile.findVariable("position_quality_flag"); //was "q_pos");                        
                        if (var == null) {
                            nWarnings++;
                            String2.log("WARNING: No position_quality_flag in " + sourceFileName);
                        } else {
                            PrimitiveArray q_pos = NcHelper.getPrimitiveArray(var);
                            if (!(q_pos instanceof IntArray) || q_pos.size() != 1) 
                                throw new SimpleException("Invalid position_quality_flag=" + q_pos);
                            int ti = q_pos.getInt(0);
                            if (String2.indexOf(okQF, ti) < 0) {
                                nBadPos++;
                                continue;
                            }
                            //nGoodPos++; is below
                        }

                        //time quality flag 
                        var = ncFile.findVariable("time_quality_flag"); //q_date_time");                        
                        if (var == null) {
                            nWarnings++;
                            String2.log("WARNING: No time_quality_flag in " + sourceFileName);
                        } else {
                            PrimitiveArray q_date_time = NcHelper.getPrimitiveArray(var);
                            if (!(q_date_time instanceof IntArray) || q_date_time.size() != 1) 
                                throw new SimpleException("Invalid time_quality_flag=" + q_date_time);
                            int ti = q_date_time.getInt(0);
                            if (String2.indexOf(okQF, ti) < 0) {
                                nBadTime++;
                                continue;
                            }
                            //nGoodTime is below
                        }

                        //time
                        var = ncFile.findVariable("time");                        
                        if (var == null) 
                            throw new SimpleException("No time!");
                        tVarAtts.clear();
                        NcHelper.getVariableAttributes(var, tVarAtts);
                        tUnits = tVarAtts.getString("units");
                        if (!timeUnits.equals(tUnits)) 
                            throw new SimpleException("Invalid time units=" + tUnits);
                        PrimitiveArray time = NcHelper.getPrimitiveArray(var);
                        if (!(time instanceof DoubleArray) || time.size() != 1) 
                            throw new SimpleException("Invalid time=" + time);
                        double tTime = Calendar2.unitsSinceToEpochSeconds(
                            timeBaseAndFactor[0], timeBaseAndFactor[1], time.getDouble(0));
                        if (tTime < minEpochSeconds || tTime > maxEpochSeconds) 
                            throw new SimpleException("Invalid tTime=" + 
                                Calendar2.safeEpochSecondsToIsoStringTZ(tTime, ""));
                        //original times (that I looked at) are to nearest second
                        //so round to nearest second (fix .99999 problems)
                        tTime = Math.rint(tTime); 
                        nGoodTime++;

                        //longitude  (position qFlag is good)
                        var = ncFile.findVariable("longitude");                        
                        if (var == null) {
                            impossibleNanLon.add(key + " lon=null");
                            continue;
                        }
                        PrimitiveArray longitude = NcHelper.getPrimitiveArray(var);
                        if (!(longitude instanceof FloatArray) || longitude.size() != 1) {
                            impossibleNanLon.add(key + " lon=wrongTypeOrSize");
                            continue;
                        }
                        float lon = longitude.getFloat(0);
                        if (Float.isNaN(lon)) { 
                            impossibleNanLon.add(key + " lon=NaN");
                            continue;
                        } else if (lon < minLon) {
                            impossibleMinLon.add(key + " lon=" + lon);
                            //fall through
                        } else if (lon > maxLon) { 
                            impossibleMaxLon.add(key + " lon=" + lon);
                            //fall through
                        }
                        lon = (float)Math2.anglePM180(lon);

                        //latitude (position qFlag is good)
                        var = ncFile.findVariable("latitude");                        
                        if (var == null) {
                            impossibleNanLat.add(key + " lat=null");
                            continue;
                        }
                        PrimitiveArray latitude = NcHelper.getPrimitiveArray(var);
                        if (!(latitude instanceof FloatArray) || latitude.size() != 1) {
                            impossibleNanLat.add(key + " lat=wrongTypeOrSize");
                            continue;
                        }
                        float lat = latitude.getFloat(0);
                        if (Float.isNaN(lat)) { 
                            impossibleNanLat.add(key + " lat=NaN");
                            continue;
                        } else if (lat < minLat) {
                            impossibleMinLat.add(key + " lat=" + lat);
                            continue;
                        } else if (lat > maxLat) { 
                            impossibleMaxLat.add(key + " lat=" + lat);
                            continue;
                        }
                        nGoodPos++;

                        //depth
                        var = ncFile.findVariable("z");                        
                        if (var == null) 
                            throw new SimpleException("No z!");
                        PrimitiveArray depth = NcHelper.getPrimitiveArray(var);
                        if (!(depth instanceof FloatArray) || depth.size() == 0) 
                            throw new SimpleException("Invalid z=" + depth);
                        int nDepth = depth.size();

                        //DEPH_qparm
                        var = ncFile.findVariable("z_variable_quality_flag"); //DEPH_qparm");                        
                        if (var == null) 
                            throw new SimpleException("No z_variable_quality_flag!");
                        PrimitiveArray DEPH_qparm = NcHelper.getPrimitiveArray(var);
                        if (!(DEPH_qparm instanceof IntArray) || DEPH_qparm.size() != nDepth) 
                            throw new SimpleException("Invalid z_variable_quality_flag=" + DEPH_qparm);
                        //nGoodDepth is below

                        //temperature
                        var = ncFile.findVariable("temperature");                        
                        PrimitiveArray temperature;
                        PrimitiveArray TEMP_qparm;
                        float temperatureFV = temperatureMV;
                        if (var == null) {
                            //nWarnings++;
                            //String2.log("WARNING: No temperature in " + sourceFileName); reasonably common
                            temperature = PrimitiveArray.factory(float.class,  nDepth, "" + temperatureMV);
                            TEMP_qparm  = PrimitiveArray.factory(int.class,    nDepth, "" + qMV);
                        } else {            
                            temperature = NcHelper.getPrimitiveArray(var);
                            if (!(temperature instanceof FloatArray) || temperature.size() != nDepth) 
                                throw new SimpleException("Invalid temperature=" + temperature);

                            tVarAtts.clear();
                            NcHelper.getVariableAttributes(var, tVarAtts);
                            temperatureFV = tVarAtts.getFloat("_FillValue");
                            if (!Float.isNaN(temperatureFV) && temperatureFV != temperatureMV)
                                throw new SimpleException("Invalid temperature _FillValue=" + temperatureFV);

                            //TEMP_qparm
                            var = ncFile.findVariable("temperature_quality_flag"); //TEMP_qparm");                        
                            if (var == null) {
                                nWarnings++;
                                String2.log("WARNING: No temperature_quality_flag in " + sourceFileName);
                                TEMP_qparm = PrimitiveArray.factory(int.class,  nDepth, "" + qMV);
                            } else {
                                TEMP_qparm = NcHelper.getPrimitiveArray(var);
                                if (!(TEMP_qparm instanceof IntArray) || TEMP_qparm.size() != nDepth) 
                                    throw new SimpleException("Invalid temperature_quality_flag=" + TEMP_qparm);
                            }
                        }

                        //salinity
                        var = ncFile.findVariable("salinity");                        
                        PrimitiveArray salinity;
                        PrimitiveArray PSAL_qparm;
                        float salinityFV = salinityMV;
                        if (var == null) {
                            //String2.log("WARNING: No salinity in " + sourceFileName);   //very common
                            salinity   = PrimitiveArray.factory(float.class,  nDepth, "" + salinityMV);
                            PSAL_qparm = PrimitiveArray.factory(int.class,    nDepth, "" + qMV);
                        } else {
                            salinity = NcHelper.getPrimitiveArray(var);
                            if (!(salinity instanceof FloatArray) || salinity.size() != nDepth) 
                                throw new SimpleException("Invalid salinity=" + salinity);

                            tVarAtts.clear();
                            NcHelper.getVariableAttributes(var, tVarAtts);
                            salinityFV = tVarAtts.getFloat("_FillValue");
                            if (!Float.isNaN(salinityFV) && salinityFV != salinityMV)
                                throw new SimpleException("Invalid salinity _FillValue=" + salinityFV);

                            //PSAL_qparm
                            var = ncFile.findVariable("salinity_quality_flag"); //PSAL_qparm");                        
                            if (var == null) {
                                nWarnings++;
                                String2.log("WARNING: No salinity_quality_flag in " + sourceFileName);
                                PSAL_qparm = PrimitiveArray.factory(int.class,  nDepth, "" + qMV);
                            } else {
                                PSAL_qparm = NcHelper.getPrimitiveArray(var);
                                if (!(PSAL_qparm instanceof IntArray) || PSAL_qparm.size() != nDepth) 
                                    throw new SimpleException("Invalid salinity_quality_flag=" + PSAL_qparm);
                            }                   
                        }

                        //clean the data
                        //(good to do it here so memory usage is low -- table remains as small as possible)
                        //Change "impossible" data to NaN
                        //(from http://www.nodc.noaa.gov/GTSPP/document/qcmans/GTSPP_RT_QC_Manual_20090916.pdf
                        //pg 61 has Table 2.1: Global Impossible Parameter Values).
                        BitSet keep = new BitSet();
                        keep.set(0, nDepth);  //all true 

                        //find worst impossible depth/temperature/salinity for this station
                        //boolean tImpossibleNanDepth       = false;
                        //boolean tImpossibleNanTemperature = false;
                        //boolean tImpossibleNanSalinity    = false;
                        float tImpossibleMinDepth = minDepth;
                        float tImpossibleMaxDepth = maxDepth;
                        float tImpossibleMinTemperature = minTemperature;
                        float tImpossibleMaxTemperature = maxTemperature;
                        float tImpossibleMinSalinity = minSalinity;
                        float tImpossibleMaxSalinity = maxSalinity;


                        for (int row = 0; row < nDepth; row++) {

                            //DEPH_qparm
                            int qs = DEPH_qparm.getInt(row);
                            float f = depth.getFloat(row);
                            if (String2.indexOf(okQF, qs) < 0) {
                                nBadDepth++;
                                keep.clear(row);
                                continue;
                            } else if (Float.isNaN(f) || f == depthMV) { //"impossible" depth
                                //tImpossibleNanDepth = true;
                                nBadDepth++;
                                keep.clear(row);
                                continue;
                            } else if (f < minDepth) {
                                tImpossibleMinDepth = Math.min(tImpossibleMinDepth, f);
                                nBadDepth++;
                                keep.clear(row);
                                continue;
                            } else if (f > maxDepth) { 
                                tImpossibleMaxDepth = Math.max(tImpossibleMaxDepth, f);
                                nBadDepth++;
                                keep.clear(row);
                                continue;
                            }
                            nGoodDepth++;

                            boolean hasData = false;

                            //temperature
                            qs = TEMP_qparm.getInt(row);
                            f = temperature.getFloat(row);
                            if (String2.indexOf(okQF, qs) < 0) {
                                temperature.setString(row, "");  //so bad value is now NaN
                                nBadTemperature++;
                            } else if (Float.isNaN(f) || f == temperatureMV) {
                                temperature.setString(row, "");  //so missing value is now NaN
                                nBadTemperature++;
                            } else if (f < minTemperature) { //"impossible" water temperature
                                tImpossibleMinTemperature = Math.min(tImpossibleMinTemperature, f);
                                temperature.setString(row, "");  //so impossible value is now NaN
                                nBadTemperature++;
                            } else if (f > maxTemperature) { //"impossible" water temperature
                                tImpossibleMaxTemperature = Math.max(tImpossibleMaxTemperature, f);
                                temperature.setString(row, "");  //so impossible value is now NaN
                                nBadTemperature++;
                            } else {
                                nGoodTemperature++;
                                hasData = true;
                            }

                            //salinity
                            qs = PSAL_qparm.getInt(row);
                            f = salinity.getFloat(row);
                            if (String2.indexOf(okQF, qs) < 0) {
                                salinity.setString(row, "");  //so bad value is now NaN
                                nBadSalinity++;
                            } else if (Float.isNaN(f) || f == salinityMV) {
                                salinity.setString(row, "");  //so missing value is now NaN
                                nBadSalinity++;
                            } else if (f < minSalinity) { //"impossible" salinity
                                tImpossibleMinSalinity = Math.min(tImpossibleMinSalinity, f);
                                salinity.setString(row, "");  //so impossible value is now NaN
                                nBadSalinity++;
                            } else if (f > maxSalinity) { //"impossible" salinity
                                tImpossibleMaxSalinity = Math.max(tImpossibleMaxSalinity, f);
                                salinity.setString(row, "");  //so impossible value is now NaN
                                nBadSalinity++;
                            } else {
                                nGoodSalinity++;
                                hasData = true;
                            }

                            //no valid temperature or salinity data?
                            if (!hasData) {           
                                keep.clear(row);
                            }
                        }

                        //ensure sizes still correct
                        Test.ensureEqual(depth.size(),       nDepth, "depth.size changed!");
                        Test.ensureEqual(temperature.size(), nDepth, "temperature.size changed!");
                        Test.ensureEqual(salinity.size(),    nDepth, "salinity.size changed!");

                        //actually remove the bad rows
                        int tnGood = keep.cardinality();
                        if (testMode && verbose) String2.log(sourceFileName + 
                            ": nGoodRows=" + tnGood + 
                            " nBadRows=" + (nDepth - tnGood));
                        nGoodRows += tnGood;
                        nBadRows += nDepth - tnGood;
                        depth.justKeep(keep);
                        temperature.justKeep(keep);
                        salinity.justKeep(keep);
                        nDepth = depth.size();

                        //impossible
                        //if (tImpossibleNanDepth)
                        //     impossibleNanDepth.add(key + " hasNaN=true");
                        //if (tImpossibleNanTemperature)
                        //     impossibleNanTemperature.add(key + " hasNaN=true");
                        //if (tImpossibleNanSalinity)
                        //     impossibleNanSalinity.add(key + " hasNaN=true");

                        if (tImpossibleMinDepth < minDepth)
                             impossibleMinDepth.add(key + " worst = " + tImpossibleMinDepth);
                        if (tImpossibleMaxDepth > maxDepth)
                             impossibleMaxDepth.add(key + " worst = " + tImpossibleMaxDepth);
                        if (tImpossibleMinTemperature < minTemperature)
                             impossibleMinTemperature.add(key + " worst = " + tImpossibleMinTemperature);
                        if (tImpossibleMaxTemperature > maxTemperature)
                             impossibleMaxTemperature.add(key + " worst = " + tImpossibleMaxTemperature);
                        if (tImpossibleMinSalinity < minSalinity)
                             impossibleMinSalinity.add(key + " worst = " + tImpossibleMinSalinity);
                        if (tImpossibleMaxSalinity > maxSalinity)
                             impossibleMaxSalinity.add(key + " worst = " + tImpossibleMaxSalinity);


                        //which table
                        if (tnGood == 0)
                            continue;
                        int loni = Math2.roundToInt(Math.floor((Math.min(lon, maxLon-0.1f) - minLon) / chunkSize));
                        int lati = Math2.roundToInt(Math.floor((Math.min(lat, maxLat-0.1f) - minLat) / chunkSize));
                        String outTableName = 
                            (minLon + loni * chunkSize) + "E_" + (minLat + lati * chunkSize) + "N";
                            //String2.replaceAll(cruise + "_" + organization + dataType, ' ', '_'); //too many: 3000+/month in 2011
                        Table tTable = (Table)tableHashMap.get(outTableName);
                            
                        if (tTable == null) {

                            Attributes ncGlobalAtts = new Attributes();
                            NcHelper.getGlobalAttributes(ncFile, ncGlobalAtts);
                            String tHistory = ncGlobalAtts.getString("history");
                            tHistory =  tHistory != null && tHistory.length() > 0?
                                tHistory + "\n" : "";

                            //make a table for this platform
                            tTable = new Table();
                            Attributes ga = tTable.globalAttributes();
                            String ack = "These data were acquired from the US NOAA National Oceanographic Data Center (NODC) on " + 
                                today + " from http://www.nodc.noaa.gov/GTSPP/.";
                            ga.add("acknowledgment", ack);
                            ga.add("license", 
                                "These data are openly available to the public.  " +
                                "Please acknowledge the use of these data with:\n" +
                                ack + "\n\n" +
                                "[standard]");
                            ga.add("history", 
                                tHistory + 
                                ".tgz files from ftp.nodc.noaa.gov /pub/gtspp/best_nc/ (http://www.nodc.noaa.gov/GTSPP/)\n" +
                                today + " Most recent ingest, clean, and reformat at ERD (bob.simons at noaa.gov).");
                            ga.add("infoUrl",    "http://www.nodc.noaa.gov/GTSPP/");
                            ga.add("institution","NOAA NODC");
                            ga.add("title",      "Global Temperature and Salinity Profile Programme (GTSPP) Data");

                            String attName = "gtspp_ConventionVersion";
                            String attValue = ncGlobalAtts.getString(attName);
                            if (attValue != null && attValue.length() > 0)
                                ga.add(attName, attValue);
                          
                            attName = "gtspp_program";
                            attValue = ncGlobalAtts.getString(attName);
                            if (attValue != null && attValue.length() > 0)
                                ga.add(attName, attValue);
                          
                            attName = "gtspp_programVersion";
                            attValue = ncGlobalAtts.getString(attName);
                            if (attValue != null && attValue.length() > 0)
                                ga.add(attName, attValue);
                          
                            attName = "gtspp_handbook_version";
                            attValue = ncGlobalAtts.getString(attName);
                            if (attValue != null && attValue.length() > 0)
                                ga.add(attName, attValue);
                          
                            organizationCol  = tTable.addColumn(tTable.nColumns(), "org",               new StringArray(),
                                new Attributes());
                            platformCol      = tTable.addColumn(tTable.nColumns(), "platform",          new StringArray(),
                                new Attributes());
                            dataTypeCol      = tTable.addColumn(tTable.nColumns(), "type",              new StringArray(),
                                new Attributes());
                            cruiseCol        = tTable.addColumn(tTable.nColumns(), "cruise",            new StringArray(),
                                new Attributes());
                            stationCol       = tTable.addColumn(tTable.nColumns(), "station_id",        new IntArray(),
                                new Attributes());
                            longitudeCol     = tTable.addColumn(tTable.nColumns(), "longitude",         new FloatArray(),
                                (new Attributes()).add("units", EDV.LON_UNITS));
                            latitudeCol      = tTable.addColumn(tTable.nColumns(), "latitude",          new FloatArray(),
                                (new Attributes()).add("units", EDV.LAT_UNITS));
                            timeCol          = tTable.addColumn(tTable.nColumns(), "time",              new DoubleArray(),
                                (new Attributes()).add("units", EDV.TIME_UNITS));
                            depthCol         = tTable.addColumn(tTable.nColumns(), "depth",             new FloatArray(),
                                (new Attributes()).add("units", "m"));
                            temperatureCol   = tTable.addColumn(tTable.nColumns(), "temperature",       new FloatArray(),
                                (new Attributes()).add("units", "degree_C"));
                            salinityCol      = tTable.addColumn(tTable.nColumns(), "salinity",          new FloatArray(),
                                (new Attributes()).add("units", "1e-3")); //PSU changed to 1e-3 with CF std names 25

                            tableHashMap.put(outTableName, tTable);
                        }

                        //put data in tTable
                        int oNRows = tTable.nRows();
                        ((StringArray)tTable.getColumn(organizationCol)).addN(nDepth, organization);
                        ((StringArray)tTable.getColumn(platformCol)).addN(nDepth, platform);
                        ((StringArray)tTable.getColumn(dataTypeCol)).addN(nDepth, dataType);
                        ((StringArray)tTable.getColumn(cruiseCol)).addN(nDepth, cruise);
                        ((IntArray   )tTable.getColumn(stationCol)).addN(nDepth, station);
                        ((FloatArray )tTable.getColumn(longitudeCol)).addN(nDepth, lon);
                        ((FloatArray )tTable.getColumn(latitudeCol)).addN(nDepth, lat);
                        ((DoubleArray)tTable.getColumn(timeCol)).addN(nDepth, tTime);
                        ((FloatArray )tTable.getColumn(depthCol)).append(depth);
                        ((FloatArray )tTable.getColumn(temperatureCol)).append(temperature);
                        ((FloatArray )tTable.getColumn(salinityCol)).append(salinity);

                        //ensure the table is valid (same size for each column)
                        tTable.ensureValid();

                    } catch (Throwable t) {
                        nExceptions++;
                        String2.log("ERROR while processing " + sourceFileName + "\n  " + 
                            MustBe.throwableToString(t));
                    } finally {
                        //always close the ncFile
                        if (ncFile != null) {
                            try {
                                ncFile.close(); 
                            } catch (Throwable t) {
                                String2.log("ERROR: unable to close " + sourceFileName + "\n" +
                                    MustBe.getShortErrorMessage(t));
                            }
                        }
                    }
                }

                String2.log("\n  time to read all those files = " + 
                    Calendar2.elapsedTimeString(System.currentTimeMillis() - fileReadTime));

                //end of region loop
                String2.log("\nIn zip=" + sourceZipName + 
                    "\n nExceptions=    " + nExceptions     + "        nWarnings="        + nWarnings +
                    "\n nBadStation=    " + nBadStation     + "        nGoodStation="     + nGoodStation +
                    "\n nBadPos=        " + nBadPos         + "        nGoodPos="         + nGoodPos +
                    "\n nBadTime=       " + nBadTime        + "        nGoodTime="        + nGoodTime +
                    "\n nBadDepth=      " + nBadDepth       + "        nGoodDepth="       + nGoodDepth +
                    "\n nBadTemperature=" + nBadTemperature + "        nGoodTemperature=" + nGoodTemperature +
                    "\n nBadSalinity=   " + nBadSalinity    + "        nGoodSalinity="    + nGoodSalinity);
                totalNGoodStation += nGoodStation;
                totalNGoodPos += nGoodPos;
                totalNGoodTime += nGoodTime;
                totalNGoodDepth += nGoodDepth; 
                totalNGoodTemperature += nGoodTemperature; 
                totalNGoodSalinity += nGoodSalinity;
                totalNGoodRows += nGoodRows;
                totalNBadPos += nBadPos; 
                totalNBadTime += nBadTime; 
                totalNBadDepth += nBadDepth; 
                totalNBadTemperature += nBadTemperature; 
                totalNBadSalinity += nBadSalinity;
                totalNBadRows += nBadRows;
                totalNWarnings += nWarnings;
                totalNExceptions += nExceptions;
            } //end of region loop

            //save by outTableName
            boolean filePrinted = false;
            Object keys[] = tableHashMap.keySet().toArray();
            int nKeys = keys.length;
            String2.log("\n*** saving nFiles=" + nKeys);
            for (int keyi = 0; keyi < nKeys; keyi++) {
                String key = keys[keyi].toString();
                Table tTable = (Table)tableHashMap.remove(key);
                if (tTable == null || tTable.nRows() == 0) {
                    String2.log("Unexpected: no table for key=" + key);
                    continue;
                }

                //sort by time, station, depth  
                //depth matches the source files: from surface to deepest
                tTable.sort(new int[]{timeCol, stationCol, depthCol}, 
                        new boolean[]{true,    true,       true});

                //is this saving a small lat lon range?
                double stationStats[] = tTable.getColumn(stationCol).calculateStats();
                //double lonStats[]     = tTable.getColumn(longitudeCol).calculateStats();
                //double latStats[]     = tTable.getColumn(latitudeCol).calculateStats();
                //nLats++;
                //double latRange = latStats[PrimitiveArray.STATS_MAX] - latStats[PrimitiveArray.STATS_MIN];
                //latSum += latRange;
                rowsSum += tTable.nRows();
                String2.log("    stationRange=" + Math2.roundToInt(stationStats[PrimitiveArray.STATS_MAX] - stationStats[PrimitiveArray.STATS_MIN]) +
                            //"  lonRange="     + Math2.roundToInt(lonStats[    PrimitiveArray.STATS_MAX] - lonStats[    PrimitiveArray.STATS_MIN]) +
                            //"  latRange="     + Math2.roundToInt(latRange) +
                              "  nRows="        + tTable.nRows());

                //save it
                String tName = tDestDir + 
                    String2.encodeFileNameSafe(key);
                /*if (lonStats[PrimitiveArray.STATS_MAX] > 45 &&
                    lonStats[PrimitiveArray.STATS_MIN] < -45) {

                    //NO MORE: This happened with 1 file/cruise, 
                    //  but won't happen now with lon/lat tiles.
                    //crosses dateline (or widely across lon=0)?  split into 2 files
                    Table ttTable = (Table)tTable.clone();
                    ttTable.oneStepApplyConstraint(0, "longitude", "<", "0");
                    ttTable.saveAsFlatNc(tName + "_W.nc", "row", false);
                    double lonStatsW[] = ttTable.getColumn(longitudeCol).calculateStats();
                    nLons++;
                    double lonRangeW = lonStatsW[PrimitiveArray.STATS_MAX] - lonStatsW[PrimitiveArray.STATS_MIN];
                    lonSum += lonRangeW;

                    ttTable = (Table)tTable.clone();
                    ttTable.oneStepApplyConstraint(0, "longitude", ">=", "0");
                    ttTable.saveAsFlatNc(tName + "_E.nc", "row", false);
                    double lonStatsE[] = ttTable.getColumn(longitudeCol).calculateStats();
                    nLons++;
                    double lonRangeE = lonStatsE[PrimitiveArray.STATS_MAX] - lonStatsE[PrimitiveArray.STATS_MIN];
                    lonSum += lonRangeE;
                    String2.log("  westLonRange=" + Math2.roundToInt(lonRangeW) +
                                "  eastLonRange=" + Math2.roundToInt(lonRangeE));
                } else */
                {
                    //nLons++;
                    nFiles++;

                    //create trajectory variable: platform + cruise
                    StringArray pl = (StringArray)tTable.getColumn("platform");
                    StringArray cr = (StringArray)tTable.getColumn("cruise");
                    StringArray or = (StringArray)tTable.getColumn("org");
                    StringArray ty = (StringArray)tTable.getColumn("type");
                    StringArray tr = new StringArray();
                    int n = pl.size();
                    for (int i = 0; i < n; i++) {
                        pl.set(i, String2.whitespacesToSpace(pl.get(i)));
                        cr.set(i, String2.whitespacesToSpace(cr.get(i)));
                        or.set(i, String2.whitespacesToSpace(or.get(i)));
                        ty.set(i, String2.whitespacesToSpace(ty.get(i)));
                        tr.add(or.getString(i) + "_" + ty.getString(i) + "_" + 
                               pl.getString(i) + "_" + cr.getString(i));
                    }
                    tTable.addColumn(0, "trajectory", tr, new Attributes());

                    tTable.saveAsFlatNc(tName + ".nc",
                        "row", false); //convertToFakeMissingValues  (keep mv's as NaNs)
                }

                //print a file
                if (testMode && !filePrinted) {
                    filePrinted = true;
                    String2.log(NcHelper.dumpString(tName, true));
                }
            }
            String2.log("\ncumulative nProfiles=" + profilesSum + " nRows=" + rowsSum +
                " mean nRows/file=" + (rowsSum / Math.max(1, nFiles)));
            //if (nLats > 0) 
            //    String2.log(  "cumulative nLats=" + nLats + " meanLatRange=" + (float)(latSum / nLats));
            //if (nLons > 0) {
            //    String2.log(  "cumulative nLons=" + nLons + " meanLonRange=" + (float)(lonSum / nLons));
            //    String2.log("mean nRows per saved file = " + (rowsSum / nLons));
            //}

            //print list of impossible at end of year or end of run
            if (month == 12 || (year == lastYear && month == lastMonth)) {

                String2.log("\n*** " + Calendar2.getCurrentISODateTimeStringLocal() +
                        " bobConsolidateGtsppTgz finished the chunk ending " + 
                        year + "-" + month + "\n" +
                    "chunkTime=" + 
                        Calendar2.elapsedTimeString(System.currentTimeMillis() - chunkTime));
                chunkTime = System.currentTimeMillis();

                //print impossible statistics
                String2.log("\nCumulative number of stations with:\n" +
                    "impossibleNanLon         = " + impossibleNanLon.size() + "\n" +
                    "impossibleMinLon         = " + impossibleMinLon.size() + "\n" +
                    "impossibleMaxLon         = " + impossibleMaxLon.size() + "\n" +
                    "impossibleNanLat         = " + impossibleNanLat.size() + "\n" +
                    "impossibleMinLat         = " + impossibleMinLat.size() + "\n" +
                    "impossibleMaxLat         = " + impossibleMaxLat.size() + "\n" +
                    "impossibleMinDepth       = " + impossibleMinDepth.size() + "\n" +
                    "impossibleMaxDepth       = " + impossibleMaxDepth.size() + "\n" +
                    //"impossibleLatLon      = " + impossibleLatLon.size() + "\n" +
                    "impossibleMinTemperature = " + impossibleMinTemperature.size() + "\n" +
                    "impossibleMaxTemperature = " + impossibleMaxTemperature.size() + "\n" +
                    "impossibleMinSalinity    = " + impossibleMinSalinity.size() + "\n" +
                    "impossibleMaxSalinity    = " + impossibleMaxSalinity.size() + "\n");

                //lon
                String2.log("\n*** " + impossibleNanLon.size() + 
                    " stations had invalid lon" +  
                    " and good pos quality flags (" + okQFCsv + ").");
                impossibleNanLon.sortIgnoreCase();
                String2.log(impossibleNanLon.toNewlineString());

                String2.log("\n*** " + impossibleMinLon.size() + 
                    " stations had lon<" + minLon +  
                    " and good pos quality flags (" + okQFCsv + ").");
                impossibleMinLon.sortIgnoreCase();
                String2.log(impossibleMinLon.toNewlineString());

                String2.log("\n*** " + impossibleMaxLon.size() + 
                    " stations had lon>" + maxLon +  
                    " and good pos quality flags (" + okQFCsv + ").");
                impossibleMaxLon.sortIgnoreCase();
                String2.log(impossibleMaxLon.toNewlineString());

                //lat
                String2.log("\n*** " + impossibleNanLat.size() + 
                    " stations had invalid lat" +  
                    " and good pos quality flags (" + okQFCsv + ").");
                impossibleNanLat.sortIgnoreCase();
                String2.log(impossibleNanLat.toNewlineString());

                String2.log("\n*** " + impossibleMinLat.size() + 
                    " stations had lat<" + minLat +  
                    " and good pos quality flags (" + okQFCsv + ").");
                impossibleMinLat.sortIgnoreCase();
                String2.log(impossibleMinLat.toNewlineString());

                String2.log("\n*** " + impossibleMaxLat.size() + 
                    " stations had lat>" + maxLat +  
                    " and good pos quality flags (" + okQFCsv + ").");
                impossibleMaxLat.sortIgnoreCase();
                String2.log(impossibleMaxLat.toNewlineString());

                //depth 
                String2.log("\n*** " + impossibleMinDepth.size() + 
                    " stations had depth<" + minDepth +  
                    " and good depth quality flags (" + okQFCsv + ").");
                impossibleMinDepth.sortIgnoreCase();
                String2.log(impossibleMinDepth.toNewlineString());

                String2.log("\n*** " + impossibleMaxDepth.size() + 
                    " stations had depth>" + maxDepth + 
                    " and good depth quality flags (" + okQFCsv + ").");
                impossibleMaxDepth.sortIgnoreCase();
                String2.log(impossibleMaxDepth.toNewlineString());

                //sa = impossibleLatLon.toArray();
                //Arrays.sort(sa);
                //String2.log("\n*** " + sa.length + " stations had impossible latitude or longitude values" +
                //    " and good q_pos quality flags.");
                //String2.log(String2.toNewlineString(sa));

                String2.log("\n*** " + impossibleMinTemperature.size() + 
                    " stations had temperature<" + minTemperature + 
                    " and good temperature quality flags (" + okQFCsv + ").");
                impossibleMinTemperature.sortIgnoreCase();
                String2.log(impossibleMinTemperature.toNewlineString());

                String2.log("\n*** " + impossibleMaxTemperature.size() + 
                    " stations had temperature>" + maxTemperature + 
                    " and good temperature quality flags (" + okQFCsv + ").");
                impossibleMaxTemperature.sortIgnoreCase();
                String2.log(impossibleMaxTemperature.toNewlineString());

                String2.log("\n*** " + impossibleMinSalinity.size() + 
                    " stations had salinity<" + minSalinity + 
                    " and good salinity quality flags (" + okQFCsv + ").");
                impossibleMinSalinity.sortIgnoreCase();
                String2.log(impossibleMinSalinity.toNewlineString());

                String2.log("\n*** " + impossibleMaxSalinity.size() + 
                    " stations had salinity>" + maxSalinity + 
                    " and good salinity quality flags (" + okQFCsv + ").");
                impossibleMaxSalinity.sortIgnoreCase();
                String2.log(impossibleMaxSalinity.toNewlineString());

            }

            //are we done?
            if (year == lastYear && month == lastMonth)
                break;

            //increment the month
            month++;
            if (month == 13) {
                year++; 
                month = 1;
            }

        }  //end of month/year loop

        String2.log("\n*** bobConsolidateGtspp completely finished " + 
            firstYear + "-" + firstMonth +
            " through " + lastYear + "-" + lastMonth);

        String2.log("\n***" +
            "\ntotalNExceptions=    " + totalNExceptions     + "        totalNWarnings=       " + totalNWarnings +
            "\ntotalNBadStation=    " + totalNBadStation     + "        totalNGoodStation=    " + totalNGoodStation + 
            "\ntotalNBadPos=        " + totalNBadPos         + "        totalNGoodPos=        " + totalNGoodPos + 
            "\ntotalNBadTime=       " + totalNBadTime        + "        totalNGoodTime=       " + totalNGoodTime + 
            "\ntotalNBadDepth=      " + totalNBadDepth       + "        totalNGoodDepth=      " + totalNGoodDepth + 
            "\ntotalNBadTemperature=" + totalNBadTemperature + "        totalNGoodTemperature=" + totalNGoodTemperature + 
            "\ntotalNBadSalinity=   " + totalNBadSalinity    + "        totalNGoodSalinity=   " + totalNGoodSalinity + 
            "\ntotalNBadRows=       " + totalNBadRows        + "        totalNGoodRows=       " + totalNGoodRows + 
            "\nlogFile=F:/data/gtspp/log.txt" +
            "\n\n*** all finished time=" + 
            Calendar2.elapsedTimeString(System.currentTimeMillis() - elapsedTime));
        String2.returnLoggingToSystemOut();
    }

    /**
     * Test erdGtsppBest against source files (.zip to .nc to ncdump).
     * @param datasetID The datasetID to be tested: erdGtsppBestNc or erdGtsppBest (.ncCF)
     */
    public static void testErdGtsppBest(String tDatasetID) throws Throwable {

        String2.log("\n*** EDDTableFromNcFiles.testErdGtsppBest test:" + tDatasetID);
        EDDTable tedd = (EDDTable)oneFromDatasetsXml(null, tDatasetID); //should work
        String tName, error, results = null, expected;
        int po;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec. 

    try {

        //*** .das
        tName = tedd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            "gtspp", ".das"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"Attributes \\{\n" +
" s \\{\n" +
"  trajectory \\{\n" +
"    String cf_role \"trajectory_id\";\n" +
"    String comment \"Constructed from org_type_platform_cruise\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Trajectory ID\";\n" +
"  \\}\n" +
"  org \\{\n" +
"    String comment \"From the first 2 characters of stream_ident:\n" +
"Code  Meaning\n" +
"AD  Australian Oceanographic Data Centre\n" +
"AF  Argentina Fisheries \\(Fisheries Research and Development National Institute \\(INIDEP\\), Mar del Plata, Argentina\n" +
"AO  Atlantic Oceanographic and Meteorological Lab\n" +
"AP  Asia-Pacific \\(International Pacific Research Center/ Asia-Pacific Data-Research Center\\)\n" +
"BI  BIO Bedford institute of Oceanography\n" +
"CF  Canadian Navy\n" +
"CS  CSIRO in Australia\n" +
"DA  Dalhousie University\n" +
"FN  FNOC in Monterey, California\n" +
"FR  Orstom, Brest\n" +
"FW  Fresh Water Institute \\(Winnipeg\\)\n" +
"GE  BSH, Germany\n" +
"IC  ICES\n" +
"II  IIP\n" +
"IK  Institut fur Meereskunde, Kiel\n" +
"IM  IML\n" +
"IO  IOS in Pat Bay, BC\n" +
"JA  Japanese Meteorologocal Agency\n" +
"JF  Japan Fisheries Agency\n" +
"ME  EDS\n" +
"MO  Moncton\n" +
"MU  Memorial University\n" +
"NA  NAFC\n" +
"NO  NODC \\(Washington\\)\n" +
"NW  US National Weather Service\n" +
"OD  Old Dominion Univ, USA\n" +
"RU  Russian Federation\n" +
"SA  St Andrews\n" +
"SI  Scripps Institute of Oceanography\n" +
"SO  Southampton Oceanographic Centre, UK\n" +
"TC  TOGA Subsurface Data Centre \\(France\\)\n" +
"TI  Tiberon lab US\n" +
"UB  University of BC\n" +
"UQ  University of Quebec at Rimouski\n" +
"VL  Far Eastern Regional Hydromet. Res. Inst. of V\n" +
"WH  Woods Hole\n" +
"\n" +
"from http://www.nodc.noaa.gov/GTSPP/document/codetbls/gtsppcode.html#ref006\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Organization\";\n" +
"  \\}\n" +
"  type \\{\n" +
"    String comment \"From the 3rd and 4th characters of stream_ident:\n" +
"Code  Meaning\n" +
"AR  Animal mounted recorder\n" +
"BA  BATHY message\n" +
"BF  Undulating Oceanographic Recorder \\(e.g. Batfish CTD\\)\n" +
"BO  Bottle\n" +
"BT  general BT data\n" +
"CD  CTD down trace\n" +
"CT  CTD data, up or down\n" +
"CU  CTD up trace\n" +
"DB  Drifting buoy\n" +
"DD  Delayed mode drifting buoy data\n" +
"DM  Delayed mode version from originator\n" +
"DT  Digital BT\n" +
"IC  Ice core\n" +
"ID  Interpolated drifting buoy data\n" +
"IN  Ship intake samples\n" +
"MB  MBT\n" +
"MC  CTD and bottle data are mixed for the station\n" +
"MI  Data from a mixed set of instruments\n" +
"ML  Minilog\n" +
"OF  Real-time oxygen and fluorescence\n" +
"PF  Profiling float\n" +
"RM  Radio message\n" +
"RQ  Radio message with scientific QC\n" +
"SC  Sediment core\n" +
"SG  Thermosalinograph data\n" +
"ST  STD data\n" +
"SV  Sound velocity probe\n" +
"TE  TESAC message\n" +
"TG  Thermograph data\n" +
"TK  TRACKOB message\n" +
"TO  Towed CTD\n" +
"TR  Thermistor chain\n" +
"XB  XBT\n" +
"XC  Expendable CTD\n" +
"\n" +
"from http://www.nodc.noaa.gov/GTSPP/document/codetbls/gtsppcode.html#ref082\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Data Type\";\n" +
"  \\}\n" +
"  platform \\{\n" +
"    String comment \"See the list of platform codes \\(sorted in various ways\\) at http://www.nodc.noaa.gov/GTSPP/document/codetbls/calllist.html\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"GTSPP Platform Code\";\n" +
"    String references \"http://www.nodc.noaa.gov/gtspp/document/codetbls/callist.html\";\n" +
"  \\}\n" +
"  cruise \\{\n" +
"    String comment \"Radio callsign \\+ year for real time data, or NODC reference number for delayed mode data.  See\n" +
"http://www.nodc.noaa.gov/GTSPP/document/codetbls/calllist.html .\n" +
"'X' indicates a missing value.\n" +
"Two or more adjacent spaces in the original cruise names have been compacted to 1 space.\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Cruise_ID\";\n" +
"  \\}\n" +
"  station_id \\{\n" +
"    Int32 _FillValue 2147483647;\n" +
"    Int32 actual_range 1, 24919149;\n" +  //changes every month  //don't regex this. It's important to see the changes.
"    String cf_role \"profile_id\";\n" +
"    String comment \"Identification number of the station \\(profile\\) in the GTSPP Continuously Managed Database\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station ID Number\";\n" +
"    Int32 missing_value 2147483647;\n" +
"  \\}\n" +
"  longitude \\{\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 _FillValue NaN;\n" +
"    Float32 actual_range -180.0, 179.999;\n" +
"    String axis \"X\";\n" +
"    String C_format \"%9.4f\";\n" +
"    Float64 colorBarMaximum 180.0;\n" +
"    Float64 colorBarMinimum -180.0;\n" +
"    Int32 epic_code 502;\n" +
"    String FORTRAN_format \"F9.4\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"    Float32 valid_max 180.0;\n" +
"    Float32 valid_min -180.0;\n" +
"  \\}\n" +
"  latitude \\{\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 _FillValue NaN;\n" +
"    Float32 actual_range -78.579, 90.0;\n" +
"    String axis \"Y\";\n" +
"    String C_format \"%8.4f\";\n" +
"    Float64 colorBarMaximum 90.0;\n" +
"    Float64 colorBarMinimum -90.0;\n" +
"    Int32 epic_code 500;\n" +
"    String FORTRAN_format \"F8.4\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"    Float32 valid_max 90.0;\n" +
"    Float32 valid_min -90.0;\n" +
"  \\}\n" +
"  time \\{\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 _FillValue NaN;\n" +
"    Float64 actual_range 6.31152e\\+8, 1.45390068e\\+9;\n" + //2nd value changes   use \\+
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    Float64 missing_value NaN;\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  \\}\n" +
"  depth \\{\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"down\";\n" +
"    Float32 _FillValue NaN;\n" +
"    Float32 actual_range -0.4, 9910.0;\n" +
"    String axis \"Z\";\n" +
"    String C_format \"%6.2f\";\n" +
"    Float64 colorBarMaximum 5000.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    Int32 epic_code 3;\n" +
"    String FORTRAN_format \"F6.2\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Depth of the Observations\";\n" +
"    Float32 missing_value NaN;\n" +
"    String positive \"down\";\n" +
"    String standard_name \"depth\";\n" +
"    String units \"m\";\n" +
"  \\}\n" +
"  temperature \\{\n" +
"    Float32 _FillValue NaN;\n" +
"    Float32 actual_range -3.91, 40.0;\n" +
"    String C_format \"%9.4f\";\n" +
"    String cell_methods \"time: point longitude: point latitude: point depth: point\";\n" +
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String coordinates \"time latitude longitude depth\";\n" +
"    Int32 epic_code 28;\n" +
"    String FORTRAN_format \"F9.4\";\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Sea Water Temperature\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_water_temperature\";\n" +
"    String units \"degree_C\";\n" +
"  \\}\n" +
"  salinity \\{\n" +
"    Float32 _FillValue NaN;\n" +
"    Float32 actual_range 0.0, 41.0;\n" +  
"    String C_format \"%9.4f\";\n" +
"    String cell_methods \"time: point longitude: point latitude: point depth: point\";\n" +
"    Float64 colorBarMaximum 37.0;\n" +
"    Float64 colorBarMinimum 32.0;\n" +
"    String coordinates \"time latitude longitude depth\";\n" +
"    Int32 epic_code 41;\n" +
"    String FORTRAN_format \"F9.4\";\n" +
"    String ioos_category \"Salinity\";\n" +
"    String long_name \"Practical Salinity\";\n" +
"    Float32 missing_value NaN;\n" +
"    String salinity_scale \"PSU\";\n" +
"    String standard_name \"sea_water_practical_salinity\";\n" +
"    String units \"PSU\";\n" +
"  \\}\n" +
" \\}\n" +
"  NC_GLOBAL \\{\n" +  
"    String acknowledgment \"These data were acquired from the US NOAA National Oceanographic " +
    "Data Center \\(NODC\\) on 2016-02-10 from http://www.nodc.noaa.gov/GTSPP/.\";\n" + //changes monthly
"    String cdm_altitude_proxy \"depth\";\n" +
"    String cdm_data_type \"TrajectoryProfile\";\n" +
"    String cdm_profile_variables \"station_id, longitude, latitude, time\";\n" +
"    String cdm_trajectory_variables \"trajectory, org, type, platform, cruise\";\n" +
"    String Conventions \"COARDS, WOCE, GTSPP, CF-1.6, ACDD-1.3\";\n" +
"    String creator_email \"nodc.gtspp@noaa.gov\";\n" +
"    String creator_name \"NOAA NESDIS NODC \\(IN295\\)\";\n" +
"    String creator_url \"http://www.nodc.noaa.gov/GTSPP/\";\n" +
"    String crs \"EPSG:4326\";\n" +                  //2 changes
"    String defaultGraphQuery \"longitude,latitude,station_id&time%3E=2016-01-24&time%3C=2016-02-01&.draw=markers&.marker=1\\|5\";\n" +
"    Float64 Easternmost_Easting 179.999;\n" +
"    String featureType \"TrajectoryProfile\";\n" +
"    String file_source \"The GTSPP Continuously Managed Data Base\";\n" +
"    Float64 geospatial_lat_max 90.0;\n" +
"    Float64 geospatial_lat_min -78.579;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 179.999;\n" +
"    Float64 geospatial_lon_min -180.0;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    Float64 geospatial_vertical_max 9910.0;\n" +
"    Float64 geospatial_vertical_min -0.4;\n" +
"    String geospatial_vertical_positive \"down\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String gtspp_ConventionVersion \"GTSPP4.0\";\n" +
"    String gtspp_handbook_version \"GTSPP Data User's Manual 1.0\";\n" +
"    String gtspp_program \"writeGTSPPnc40.f90\";\n" +
"    String gtspp_programVersion \"1.7\";\n" +  
"    String history \"2016-02-01 csun writeGTSPPnc40.f90 Version 1.7\n" +//date changes
".tgz files from ftp.nodc.noaa.gov /pub/gtspp/best_nc/ \\(http://www.nodc.noaa.gov/GTSPP/\\)\n" +
"2016-02-10 Most recent ingest, clean, and reformat at ERD \\(bob.simons at noaa.gov\\).\n"; //date changes

        po = results.indexOf("bob.simons at noaa.gov).\n");
        String tResults = results.substring(0, po + 25);
        //String2.log("tResults=" + tResults);
        Test.ensureLinesMatch(tResults, expected, "\nresults=\n" + results);

//+ " (local files)\n" +
//today + " http://127.0.0.1:8080/cwexperimental/
expected = 
"    String id \"erdGtsppBest\";\n" +
"    String infoUrl \"http://www.nodc.noaa.gov/GTSPP/\";\n" +
"    String institution \"NOAA NODC\";\n" +
"    String keywords \"Oceans > Ocean Temperature > Water Temperature,\n" +
"Oceans > Salinity/Density > Salinity,\n" +
"cruise, data, density, depth, global, gtspp, identifier, noaa, nodc, observation, ocean, oceans, organization, profile, program, salinity, sea, sea_water_practical_salinity, sea_water_temperature, seawater, station, temperature, temperature-salinity, time, type, water\";\n" +
"    String keywords_vocabulary \"NODC Data Types, CF Standard Names, GCMD Science Keywords\";\n" +
"    String LEXICON \"NODC_GTSPP\";\n" +                                      //date below changes
"    String license \"These data are openly available to the public.  Please acknowledge the use of these data with:\n" +
"These data were acquired from the US NOAA National Oceanographic Data Center \\(NODC\\) on 2016-02-10 from http://www.nodc.noaa.gov/GTSPP/.\n" +
"\n" +
"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String naming_authority \"gov.noaa.nodc\";\n" +
"    Float64 Northernmost_Northing 90.0;\n" +
"    String project \"Joint IODE/JCOMM Global Temperature-Salinity Profile Programme\";\n" +
"    String references \"http://www.nodc.noaa.gov/GTSPP/\";\n" +
"    String sourceUrl \"\\(local files\\)\";\n" +
"    Float64 Southernmost_Northing -78.579;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
"    String subsetVariables \"trajectory, org, type, platform, cruise\";\n" +
"    String summary \"The Global Temperature-Salinity Profile Programme \\(GTSPP\\) develops and maintains a global ocean temperature and salinity resource with data that are both up-to-date and of the highest quality. It is a joint World Meteorological Organization \\(WMO\\) and Intergovernmental Oceanographic Commission \\(IOC\\) program.  It includes data from XBTs, CTDs, moored and drifting buoys, and PALACE floats. For information about organizations contributing data to GTSPP, see http://gosic.org/goos/GTSPP-data-flow.htm .  The U.S. National Oceanographic Data Center \\(NODC\\) maintains the GTSPP Continuously Managed Data Base and releases new 'best-copy' data once per month.\n" +
"\n" +
"WARNING: This dataset has a \\*lot\\* of data.  If you request too much data, your request will fail.\n" +
"\\* If you don't specify a longitude and latitude bounding box, don't request more than a month's data at a time.\n" +
"\\* If you do specify a longitude and latitude bounding box, you can request data for a proportionally longer time period.\n" +
"Requesting data for a specific station_id may be slow, but it works.\n" +
"\n" +                       
"\\*\\*\\* This ERDDAP dataset has data for the entire world for all available times \\(currently, " +
    "up to and including the January 2016 data\\) but is a subset of the " + //month changes
    "original NODC 'best-copy' data.  It only includes data where the quality flags indicate the data is 1=CORRECT, 2=PROBABLY GOOD, or 5=MODIFIED. It does not include some of the metadata, any of the history data, or any of the quality flag data of the original dataset. You can always get the complete, up-to-date dataset \\(and additional, near-real-time data\\) from the source: http://www.nodc.noaa.gov/GTSPP/ .  Specific differences are:\n" +
"\\* Profiles with a position_quality_flag or a time_quality_flag other than 1\\|2\\|5 were removed.\n" +
"\\* Rows with a depth \\(z\\) value less than -0.4 or greater than 10000 or a z_variable_quality_flag other than 1\\|2\\|5 were removed.\n" +
"\\* Temperature values less than -4 or greater than 40 or with a temperature_quality_flag other than 1\\|2\\|5 were set to NaN.\n" +
"\\* Salinity values less than 0 or greater than 41 or with a salinity_quality_flag other than 1\\|2\\|5 were set to NaN.\n" +
"\\* Time values were converted from \\\\\"days since 1900-01-01 00:00:00\\\\\" to \\\\\"seconds since 1970-01-01T00:00:00\\\\\".\n" +
"\n" +
"See the Quality Flag definitions on page 5 and \\\\\"Table 2.1: Global Impossible Parameter Values\\\\\" on page 61 of\n" +
"http://www.nodc.noaa.gov/GTSPP/document/qcmans/GTSPP_RT_QC_Manual_20090916.pdf .\n" +
"The Quality Flag definitions are also at\n" +
"http://www.nodc.noaa.gov/GTSPP/document/qcmans/qcflags.htm .\";\n" +
"    String time_coverage_end \"2016-01-27T13:18:00Z\";\n" + //changes
"    String time_coverage_start \"1990-01-01T00:00:00Z\";\n" +
"    String title \"Global Temperature and Salinity Profile Programme \\(GTSPP\\) Data\";\n" +
"    Float64 Westernmost_Easting -180.0;\n" +
"  \\}\n" +
"\\}\n";
        int tPo = results.indexOf(expected.substring(0, 14)); //not more than 14 because of regex special chars
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        tResults = results.substring(tPo);
        Test.ensureLinesMatch(tResults, expected, "\nresults=\n" + results);
       
    } catch (Throwable t) {
        String2.pressEnterToContinue("Unexpected error:\n" + MustBe.throwableToString(t));
    }
    try {

        //*** .dds
        tName = tedd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            "gtspp", ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String trajectory;\n" +
"    String org;\n" +
"    String type;\n" +
"    String platform;\n" +
"    String cruise;\n" +
"    Int32 station_id;\n" +
"    Float32 longitude;\n" +
"    Float32 latitude;\n" +
"    Float64 time;\n" +
"    Float32 depth;\n" +
"    Float32 temperature;\n" +
"    Float32 salinity;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

    } catch (Throwable t) {
        String2.pressEnterToContinue("Unexpected error:\n" + MustBe.throwableToString(t));
    }
    try {

        //station_id    (slow for .nc, faster for .ncCF)
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "&station_id=1254666", 
            EDStatic.fullTestCacheDirectory, "gtspp1254666", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"trajectory,org,type,platform,cruise,station_id,longitude,latitude,time,depth,temperature,salinity\n" +
",,,,,,degrees_east,degrees_north,UTC,m,degree_C,PSU\n" +
"ME_BA_33TT_21004 00,ME,BA,33TT,21004 00,1254666,134.9833,28.9833,2000-01-01T03:00:00Z,0.0,20.8,NaN\n" +
"ME_BA_33TT_21004 00,ME,BA,33TT,21004 00,1254666,134.9833,28.9833,2000-01-01T03:00:00Z,50.0,20.7,NaN\n" +
"ME_BA_33TT_21004 00,ME,BA,33TT,21004 00,1254666,134.9833,28.9833,2000-01-01T03:00:00Z,100.0,20.7,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //tests that should fail quickly
        String tests[] = {
            "&station_id<1",                           "&station_id=NaN",
            "&longitude<-180",   "&longitude>180",     "&longitude=NaN",
            "&latitude<-90",     "&latitude>90",       "&latitude=NaN",
            "&depth<-0.4",       "&depth>10000",       "&depth=NaN",
            "&time<1990-01-01",  "&time>" + today,     "&time=NaN",
            "&temperature<-4",   "&temperature>40",    
            "&salinity<0",       "&salinity>41",       };
        for (int test = 0; test < tests.length; test++) { 
            try {
                String2.log("\n*** start testing " + tests[test]);
                error = "";
                results = "";
                long eTime = System.currentTimeMillis();
                tName = tedd.makeNewFileForDapQuery(null, null, tests[test], 
                    EDStatic.fullTestCacheDirectory, "gtspp" + test, ".csv"); 
                String2.log("\n*** finished testing " + tests[test] + " time=" + (System.currentTimeMillis() - eTime));
                results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            } catch (Throwable t) {
                error = MustBe.throwableToString(t);
            }
            Test.ensureEqual(results, "", "results=\n" + results);
            Test.ensureTrue(error.indexOf(MustBe.THERE_IS_NO_DATA) >= 0, "error=" + error);
        }

    } catch (Throwable t) {
        String2.pressEnterToContinue("Unexpected error:\n" + MustBe.throwableToString(t));
    }
    try {

        //latitude = -78.579002  the minmin value as of 2014-07-21
        //should succeed quickly (except for println statements here)
        tName = tedd.makeNewFileForDapQuery(null, null, "&latitude=-78.579002", 
            EDStatic.fullTestCacheDirectory, "gtspp77", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"trajectory,org,type,platform,cruise,station_id,longitude,latitude,time,depth,temperature,salinity\n" +
",,,,,,degrees_east,degrees_north,UTC,m,degree_C,PSU\n" +
"ME_BA_49K6_JNZL 05,ME,BA,49K6,JNZL 05,2509050,-165.432,-78.579,2005-01-31T02:31:00Z,4.0,-0.9,NaN\n" +
"ME_BA_49K6_JNZL 05,ME,BA,49K6,JNZL 05,2509050,-165.432,-78.579,2005-01-31T02:31:00Z,100.0,-0.9,NaN\n" +
"ME_BA_49K6_JNZL 05,ME,BA,49K6,JNZL 05,2509050,-165.432,-78.579,2005-01-31T02:31:00Z,200.0,-1.9,NaN\n" +
"ME_BA_49K6_JNZL 05,ME,BA,49K6,JNZL 05,2509050,-165.432,-78.579,2005-01-31T02:31:00Z,487.0,-1.9,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //time range     should succeed quickly (except for println statements here)
        //!!! sort order of nc (by time, station_id ?) vs ncCF (by time, lat, lon ?!) is different
        long eTime = System.currentTimeMillis();
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "&time>2000-01-01T02:59:59Z&time<2000-01-01T03:00:01Z&orderBy(\"station_id,depth\")", 
            EDStatic.fullTestCacheDirectory, "gtsppLL", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"trajectory,org,type,platform,cruise,station_id,longitude,latitude,time,depth,temperature,salinity\n" +
",,,,,,degrees_east,degrees_north,UTC,m,degree_C,PSU\n" +
"ME_BA_33TT_21004 00,ME,BA,33TT,21004 00,1254666,134.9833,28.9833,2000-01-01T03:00:00Z,0.0,20.8,NaN\n" +
"ME_BA_33TT_21004 00,ME,BA,33TT,21004 00,1254666,134.9833,28.9833,2000-01-01T03:00:00Z,50.0,20.7,NaN\n" +
"ME_BA_33TT_21004 00,ME,BA,33TT,21004 00,1254666,134.9833,28.9833,2000-01-01T03:00:00Z,100.0,20.7,NaN\n" +
"ME_BA_33TT_21002 00,ME,BA,33TT,21002 00,1254689,134.5333,37.9,2000-01-01T03:00:00Z,0.0,14.7,NaN\n" +
"ME_BA_33TT_21002 00,ME,BA,33TT,21002 00,1254689,134.5333,37.9,2000-01-01T03:00:00Z,50.0,14.8,NaN\n" +
"ME_BA_33TT_21002 00,ME,BA,33TT,21002 00,1254689,134.5333,37.9,2000-01-01T03:00:00Z,100.0,14.7,NaN\n" +
"ME_BA_33TT_22001 00,ME,BA,33TT,22001 00,1254716,126.3,28.1667,2000-01-01T03:00:00Z,0.0,22.3,NaN\n" +
"ME_BA_33TT_22001 00,ME,BA,33TT,22001 00,1254716,126.3,28.1667,2000-01-01T03:00:00Z,50.0,21.3,NaN\n" +
"ME_BA_33TT_22001 00,ME,BA,33TT,22001 00,1254716,126.3,28.1667,2000-01-01T03:00:00Z,100.0,19.8,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
        String2.log("time. elapsedTime=" + (System.currentTimeMillis() - eTime));
        //String2.pressEnterToContinue();

    } catch (Throwable t) {
        String2.pressEnterToContinue("Unexpected error:\n" + MustBe.throwableToString(t));
    }

    }

    /**  Really one time: resort the gtspp files.
     */
    public static void resortGtspp() throws Throwable {

        String sourceDir = "c:/data/gtspp/bestNcConsolidated/";

        //get the names of all of the .nc files
        String names[] = RegexFilenameFilter.recursiveFullNameList(sourceDir, ".*\\.nc", false);
        int n = 1; //names.length;
        for (int i = 0; i < n; i++) {
            if (i % 100 == 0) 
                String2.log("#" + i + " " + names[i]);

            //read
            Table table = new Table();
            table.readFlatNc(names[i], null, 0);

            //table.globalAttributes().set("history",
            //    "(From .zip files from http://www.nodc.noaa.gov/GTSPP/)\n" +
            //    "2010-06-16 Incremental ingest, clean, and reformat at ERD (bob.simons at noaa.gov).");

            //resort
            table.sort(
                new int[]{
                    table.findColumnNumber("time"), 
                    table.findColumnNumber("station_id"), 
                    table.findColumnNumber("depth")}, 
                new boolean[]{true, true, true});

            //write
            String tName = String2.replaceAll(names[i], "bestNcConsolidated/", "bestNcConsolidated2/");
            File2.makeDirectory(File2.getDirectory(tName));
            table.saveAsFlatNc(tName, "row", false);

            //compare
            if (i == 0) {
                String ncdump1 = NcHelper.dumpString(names[i], false);
                String ncdump2 = NcHelper.dumpString(tName,    false);
                Test.ensureEqual(ncdump1, ncdump2, "");

                String2.log("\n*** Old:\n" + NcHelper.dumpString(names[i], true));
                String2.log("\n*** New:\n" + NcHelper.dumpString(tName,    true));
            }
        }
    }

    /**
     * This creates the /u00/data/points/gtspp/*.ncCF GTSPP files 
     * from the /data/gtspp/bestNcConsolidated/... .nc GTSPP files
     * by making calls to the testGtsppBest dataset.  
     * This doesn't need/use localhost ERDDAP.
     *
     * @param firstYear e.g., 1990
     * @param firstMonth e.g., 1=January
     * @param lastYear e.g., 2014
     * @param lastMonth e.g., 1=January
     */
    public static void bobCreateGtsppNcCFFiles(int firstYear, int firstMonth, 
        int lastYear, int lastMonth) throws Throwable {

        String2.log("*** bobCreateGtsppNcCFFiles");
        long time = System.currentTimeMillis();
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, "erdGtsppBestNc"); 
        for (int year = firstYear; year <= lastYear; year++) {
            int tFirstMonth = year == firstYear? firstMonth : 1;
            int tLastMonth  = year == lastYear? lastMonth : 12;
            for (int month = tFirstMonth; month <= tLastMonth; month++) {
                String2.log("\n*** year=" + year + " month=" + month);
                String ym     = year + String2.zeroPad("" + month, 2);
                String ydm    = year + "-" + String2.zeroPad("" + month, 2);
                String zp1    = ydm + "-01";
                String zp16   = ydm + "-16";
                String zpNext = year + "-" + String2.zeroPad("" + (month + 1), 2) + "-01";
                //? orderBy not very relevant, because .ncCF forces a storage order?
                String qa = 
                    "&time>=" + zp1 +
                    "&time<"  + zp16 + 
                    "&orderBy(\"trajectory,station_id,time,depth\")"; 
                String qb = 
                    "&time>=" + zp16 +
                    "&time<"  + zpNext +
                    "&orderBy(\"trajectory,station_id,time,depth\")";
                String2.log(qa + "\n" + qb);
                eddTable.makeNewFileForDapQuery(null, null, qa, 
                    "/u00/data/points/gtsppNcCF/", ym + "a", ".ncCF"); 
                eddTable.makeNewFileForDapQuery(null, null, qb, 
                    "/u00/data/points/gtsppNcCF/", ym + "b", ".ncCF"); 
            }
        }
        String2.log("bobCreateGtsppNcCFFiles finished successfully in " + 
            Calendar2.elapsedTimeString(System.currentTimeMillis() - time));
    }

    /** one time(?) test for Bob */
    public static void bobFindGtsppDuplicateCruises() throws Throwable {
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, "testErdGtsppBest"); 
        eddTable.makeNewFileForDapQuery(null, null, 
            "cruise,type,org,platform&orderBy(\"cruise,type,org,platform\")&distinct()", 
             "/temp/", "gtsppDuplicates", ".nc"); 
        Table table = new Table();
        table.readFlatNc("/temp/gtsppDuplicates.nc", null, 0);
        int n = table.nRows();
        BitSet keep = new BitSet(n);  //all false
        PrimitiveArray cr = table.getColumn(0);
        PrimitiveArray ty = table.getColumn(1);
        PrimitiveArray or = table.getColumn(2);
        String2.log("nRows=" + n);
        for (int row = 1; row < n; row++) { //look back
            if (cr.getString(row - 1).equals(cr.getString(row)) && 
                ty.getString(row - 1).equals(ty.getString(row)) && 
                or.getString(row - 1).equals(or.getString(row))) {
                keep.set(row - 1);
                keep.set(row);
            }
        }
        table.justKeep(keep);
        String2.log("nRows=" + table.nRows());
        String2.log(table.toString("row", Integer.MAX_VALUE));
    }

            

    /** This test making transparentPngs.
     */
    public static void testTransparentPng() throws Throwable {
        String2.log("\n*** testTransparentPng");
        //testVerboseOn();
        reallyVerbose = false;
        String dir = EDStatic.fullTestCacheDirectory;
        String name, tName, userDapQuery, results, expected, error;
        String dapQuery;

        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, "cwwcNDBCMet");
/*  */
        //markers on a map
        dapQuery = 
            "longitude,latitude,wtmp&time=2010-01-02T00:00:00Z" +
            "&longitude>=-140&longitude<=-110&latitude>=20&latitude<=50" +
            "&.draw=markers&.marker=5|5&.color=0xff0000&.colorBar=|||||";
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery, 
            dir, eddTable.className() + "_markersMap",  ".png"); 
        SSR.displayInBrowser("file://" + dir + tName);
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery, 
            dir, eddTable.className() + "_TPmarkersMap",  ".transparentPng"); 
        SSR.displayInBrowser("file://" + dir + tName);
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery + "&.size=500|400", 
            dir, eddTable.className() + "_TPmarkersMap500400",  ".transparentPng"); 
        SSR.displayInBrowser("file://" + dir + tName);

        //vector map
        dapQuery = 
            "longitude,latitude,wspu,wspv&time=2010-01-02T00:00:00Z" +
            "&longitude>=-140&longitude<=-110&latitude>=20&latitude<=50" +
            "&.draw=vectors&.color=0xff0000";
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery, 
            dir, eddTable.className() + "_vectors", ".png"); 
        SSR.displayInBrowser("file://" + dir + tName);
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery, 
            dir, eddTable.className() + "_TPvectors", ".transparentPng"); 
        SSR.displayInBrowser("file://" + dir + tName);
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery + "&.size=360|150", 
            dir, eddTable.className() + "_TPvectors360150", ".transparentPng"); 
        SSR.displayInBrowser("file://" + dir + tName);

        //lines on a graph
        dapQuery = 
            "time,wtmp" +
            "&station=\"41009\"&time>=2000-01-01T00:00:00Z&time<=2000-01-02T00:00:00Z" +
            "&.draw=lines&.color=0xff0000&.colorBar=|||||";
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery, 
            dir, eddTable.className() + "_lines", ".png"); 
        SSR.displayInBrowser("file://" + dir + tName);
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery, 
            dir, eddTable.className() + "_TPlines", ".transparentPng"); 
        SSR.displayInBrowser("file://" + dir + tName);
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery + "&.size=500|400", 
            dir, eddTable.className() + "_TPlines500400", ".transparentPng"); 
        SSR.displayInBrowser("file://" + dir + tName);

        //markers on a graph
        dapQuery = 
            "time,wtmp" +
            "&station=\"41009\"&time>=2000-01-01T00:00:00Z&time<=2000-01-02T00:00:00Z" +
            "&.draw=markers&.marker=5|5&.color=0xff0000&.colorBar=|||||";
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery, 
            dir, eddTable.className() + "_markers",  ".png"); 
        SSR.displayInBrowser("file://" + dir + tName);
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery, 
            dir, eddTable.className() + "_TPmarkers",  ".transparentPng"); 
        SSR.displayInBrowser("file://" + dir + tName);
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery + "&.size=500|400", 
            dir, eddTable.className() + "_TPmarkers500400",  ".transparentPng"); 
        SSR.displayInBrowser("file://" + dir + tName);

        //sticks on a graph
        dapQuery = 
            "time,wspu,wspv" +
            "&station=\"41009\"&time>=2000-01-01T00:00:00Z&time<=2000-01-02T00:00:00Z" +
            "&.draw=sticks&.color=0xff0000";
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery, 
            dir, eddTable.className() + "_sticks", ".png"); 
        SSR.displayInBrowser("file://" + dir + tName);
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery, 
            dir, eddTable.className() + "_TPsticks", ".transparentPng"); 
        SSR.displayInBrowser("file://" + dir + tName);
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery + "&.size=500|400", 
            dir, eddTable.className() + "_TPsticks500400", ".transparentPng"); 
        SSR.displayInBrowser("file://" + dir + tName);
/* */
    }

    /** This tests a long time graph.
     */
    public static void testTimeAxis() throws Throwable {
        String2.log("\n*** testTimeAxis");
        testVerboseOn();
        reallyVerbose = true;
        String dir = EDStatic.fullTestCacheDirectory;
        String name, tName, userDapQuery, results, expected, error;
        String dapQuery;

        String id = "testTimeAxis";
        deleteCachedDatasetInfo(id);
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, id);

        //test reject based on time>lastTime
        //Note that dataset's last time value is exactly 2011-07-01
        try {
            dapQuery = "time,irradiance&time>=2012-01-09T01:02:03.123Z";
            tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery, 
                dir, eddTable.className() + "_afterMaxTime",  ".csv"); 
            results = "shouldn't get here";
        } catch (Throwable t) {
            results = MustBe.throwableToString(t);
        }        
        expected = "SimpleException: Your query produced no matching results. " +
            //request is .123, but it has fudge applied in EDDTable.getSourceQuery... String2.genEFormat10(
            "(time>=2012-01-09T01:02:03.123Z is outside of the variable's actual_range: " +
            "1610-07-01T00:00:00.000Z to 2011-07-01T00:00:00.000Z)";
        Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), 
            expected, "results=" + results);
      
        //test the interesting time axis cases and the extremes of each sub-axis type
        String time[] = { 
            "0000-01-01", "0100-01-01", "0500-01-01", "1000-01-01", "1300-01-01", //century
            "1400-01-01", "1900-01-01", "1940-01-01",     //decade
            "1950-01-01", "1990-01-01", "2009-01-01",     //year
            "2010-01-01", "2011-01-01", "2011-04-09",     //month
            "2011-04-10", "2011-06-01", "2011-06-28T06",  //date
            "2011-06-29T12",         "2011-06-30",             "2011-06-30T20",            //hours
            "2011-06-30T21",         "2011-06-30T23:00",       "2011-06-30T23:56:15",      //minutes
            "2011-06-30T23:56:30",   "2011-06-30T23:58:30",    "2011-06-30T23:59:56",      //seconds
            "2011-06-30T23:59:57",   "2011-06-30T23:59:58",    "2011-06-30T23:59:59",      //millis
            "2011-06-30T23:59:59.7", "2011-06-30T23:59:59.97", "2011-06-30T23:59:59.997",  //millis
            "2011-07-01"};  //min=max
        for (int i = 0; i < time.length; i++) { 
//        for (int i = 0; i < 3; i++) {

            for (int lowToHigh = 0; lowToHigh < 2; lowToHigh++) {
                dapQuery = "time,irradiance&time>=" + time[i] + "&.xRange=||" + (lowToHigh == 0);
                tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery, 
                    dir, eddTable.className() + "_TimeAxis" + i + "_" + lowToHigh,  ".png"); 
                SSR.displayInBrowser("file://" + dir + tName);
            }
        }
    }

    /** This tests modifying the time axis with add_offset (so source times are fractional years) 
     * and units="years since 0000-07-01" (to again make the times all at July 1).
     */
    public static void testModTime() throws Throwable {
        String2.log("\n*** testModTime");
        testVerboseOn();
        reallyVerbose = true;
        boolean oDebugMode = debugMode;
        debugMode = true;
        String dir = EDStatic.fullTestCacheDirectory;
        String name, tName, userDapQuery, results, expected, error;
        String dapQuery;

        String id = "testModTime";
        deleteCachedDatasetInfo(id);
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, id);

        expected = 
"time,irradiance\n" +
"UTC,W/m^2\n" +
"2011-07-01T00:00:00.000Z,1361.3473\n";

        dapQuery = "time,irradiance&time=\"2011-07-01\"";
        tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery, 
            dir, eddTable.className() + "_ModTime2",  ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        Test.ensureEqual(results, expected, "results=\n" + results);

        debugMode = oDebugMode;    
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
        String2.log("\n*** EDDTableFromNcFiles.testSpeed\n" + 
            SgtUtil.isBufferedImageAccelerated() + "\n");
        boolean oReallyVerbose = reallyVerbose;
        reallyVerbose = false;
        String tName;
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, "cwwcNDBCMet"); 
        //userDapQuery will make time series graphs
        //not just a 121000 points at the same location on a map
        String userDapQuery = "time,wtmp,station,longitude,latitude,wd,wspd,gst,wvht,dpd,apd,mwd,bar,atmp,dewp,vis,ptdy,tide,wspu,wspv&station=\"41006\""; 
        String dir = EDStatic.fullTestCacheDirectory;
        String extensions[] = new String[] {  //.help not available at this level            
            ".asc", ".csv", ".csvp", ".csv0", 
            ".das", ".dds", ".dods", 
            ".esriCsv", ".geoJson", ".graph", ".html", 
            ".htmlTable", ".json", 
            ".mat", ".nc", ".ncHeader", 
            ".odvTxt", ".subset", ".tsv", ".tsvp", ".tsv0", 
            ".xhtml",
            ".kml", ".smallPdf", ".pdf", ".largePdf", 
            ".smallPng", ".png", ".largePng"};  
        int expectedMs[] = new int[] { 
            //now Java 1.7 M4700      //was Java 1.6                   //was Java 1.5
            //graphics tests changed a little 1.5 -> 1.6, so not perfectly comparable
            619, 672, 515, 515,       //3125, 2625, 2687, ?,           //4469, 4125, 4094, ?, 
            8, 4, 176,                //2014-09 .dods slower 176->508 why? // 16, 31, 687 //16, 32, 1782, 
            763, 1126, 60, 72,        //3531, 5219, 47, 31,            //5156, 6922, 125, 100, 
            534, 523,                 //1672, 2719,                    //4109, 4921, 
            469, 535, 665,            //1531, 1922, 1797,              //4921, 4921, 4610, 
            896, 65, 485, 482, 480,   //4266, 32, 2562, 2531, ?,       //8359, 31, 3969, 3921, ?, 
            729,                      //2014-09 slower 729->1140 why?  //4266,    //6531, 
            967, 763, 686, 740,       //2078, 2500, 2063, 2047,        //4500, 5800, 5812, 5610, 
            924, 904, 1022};          //2984, 3125, 3391               //5421, 5204, 5343};         
        int bytes[]    = new int[] {
            18989646, 13058166, 13058203, 13057934, 
            14544, 394, 11703093, 
            16391423, 55007762, 142273, 178477, 
            13606792, 17827554, 
            10485696, 9887104, 14886, 
            10698295, 24007, 13058166, 13058203, 13057934, 
            59642150, 
            4790, 82780, 135305, 161613, 
            7482, 11367, 23684};

        //warm up
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, 
            dir, eddTable.className() + "_testSpeedw", ".pdf"); 
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, 
            dir, eddTable.className() + "_testSpeedw", ".png"); 
        
        int firstExt = whichTest < 0? 0 : whichTest;
        int lastExt  = whichTest < 0? extensions.length - 1 : whichTest;
        for (int ext = firstExt; ext <= lastExt; ext++) {
            try { 
                String2.log("\n*** EDDTableFromNcFiles.testSpeed test#" + ext + ": " + 
                    extensions[ext] + " speed\n");
                long time = 0, cLength = 0;
                for (int chance = 0; chance < 3; chance++) {
                    Math2.gcAndWait(); //in a test
                    time = System.currentTimeMillis();
                    tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, 
                        dir, eddTable.className() + 
                        "_testSpeed" + chance + extensions[ext].substring(1) + ext, 
                        extensions[ext]); 
                    time = System.currentTimeMillis() - time;
                    cLength = File2.length(dir + tName);
                    String2.log("\n*** EDDTableFromNcFiles.testSpeed test#" + ext +
                        " chance#" + chance + ": " + extensions[ext] + " done.\n  " + 
                        cLength + " bytes (expected=" + bytes[ext] + ").  time=" + 
                        time + " ms (expected=" + expectedMs[ext] + ")\n"); Math2.sleep(3000);

                    //if not too slow or too fast, break
                    if (time > 1.5 * Math.max(100, expectedMs[ext]) ||
                        time < (expectedMs[ext] <= 50? 0.1 : 0.5) * expectedMs[ext]) {
                        //give it another chance
                    } else {
                        break;
                    }
                }

                //display?
                if (false && String2.indexOf(imageFileTypeNames, extensions[ext]) >= 0) {
                    SSR.displayInBrowser("file://" + dir + tName);
                    Math2.gc(5000); //in a test, pause for image display
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
                if (time > 1.5 * Math.max(100, expectedMs[ext]))
                    throw new SimpleException(
                        "Slower than expected. observed=" + time + 
                        " expected=~" + expectedMs[ext] + " ms.");
                if (time < (expectedMs[ext] <= 50? 0 : 0.5) * expectedMs[ext])
                    throw new SimpleException(
                        "Faster than expected! observed=" + time + 
                        " expected=~" + expectedMs[ext] + " ms.");

                //data test for .nc (especially string column)
                //This is important test of EDDTable.saveAsFlatNc for nRows > partialRequestMaxCells
                //(especially since changes on 2010-09-07).
                if (extensions[ext].equals(".nc")) {
                    Table table = new Table();
                    table.readFlatNc(dir + tName, null, 1);
                    int nRows = table.nRows();
                    String2.log(".nc fileName=" + dir + tName + "\n" +
                        "nRows=" + nRows);
                    String results = table.dataToCSVString(2);
                    String expected = 
"row,time,wtmp,station,longitude,latitude,wd,wspd,gst,wvht,dpd,apd,mwd,bar,atmp,dewp,vis,ptdy,tide,wspu,wspv\n" +
"0,3.912804E8,24.8,41006,-77.4,29.3,297,4.1,5.3,1.0,8.3,4.8,,1014.7,22.0,,,,,3.7,-1.9\n" +
"1,3.91284E8,24.7,41006,-77.4,29.3,,,,1.1,9.1,4.5,,1014.6,22.2,,,,,,\n";
                    String2.log("results=\n" + results);
                    Test.ensureEqual(results, expected, "");
                    for (int row = 0; row < nRows; row++) {
                        Test.ensureEqual(table.getStringData(2, row), "41006", "");
                        Test.ensureEqual(table.getFloatData(3, row), -77.4f, "");
                        Test.ensureEqual(table.getFloatData(4, row), 29.3f, "");
                    }
                }
                //String2.pressEnterToContinue("Okay."); 
            } catch (Exception e) {
                String2.pressEnterToContinue(
                    MustBe.throwableToString(e) +
                    "\nUnexpected ERROR for Test#" + ext + ": " + extensions[ext] + "."); 
            }
        }
        reallyVerbose = oReallyVerbose;
    }


    /** 
     * This a graph with many years.
     *
     * @param whichTest -1 for all, or 0..
     */
    public static void testManyYears() throws Throwable {
        String2.log("\n*** EDDTableFromNcFiles.testManyYears\n");
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, "erdCAMarCatSY"); 
        String dir = EDStatic.fullTestCacheDirectory;
        String dapQuery = 
        "time,landings&port=%22Santa%20Barbara%22&fish=%22Abalone%22&.draw=lines&.color=0x000000";
        String tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery, 
            dir, eddTable.className() + "_manyYears",  ".png"); 
        SSR.displayInBrowser("file://" + dir + tName);
    }


    private static void metadataToData(Table table, String colName,
        String attName, String newColName, Class tClass) throws Exception {

        int col = table.findColumnNumber(colName);
        if (col < 0)
            throw new RuntimeException("col=" + colName + " not found in " + 
                table.getColumnNamesCSSVString());
        String value = table.columnAttributes(col).getString(attName);
        table.columnAttributes(col).remove(attName);
        if (value == null)
            value = "";
        table.addColumn(table.nColumns(), newColName, 
            PrimitiveArray.factory(tClass, table.nRows(), value), 
            new Attributes());
    }

    /** NOT FOR GENERAL USE. Bob uses this to consolidate the individual WOD
     * data files into 45° x 45° x 1 month files (tiles).
     * 45° x 45° leads to 8x4=32 files for a given time point, so a request
     * for a short time but entire world opens ~32 files.
     * There are ~240 months worth of data, so a request for a small lon lat 
     * range for all time opens ~240 files.
     *
     * <p>Why tile? Because there are ~10^6 profiles/year now, so ~10^7 total.
     * And if 100 bytes of info per file for EDDTableFromFiles fileTable, that's 1 GB!.
     * So there needs to be fewer files.
     * We want to balance number of files for 1 time point (all region tiles), 
     * and number of time point files (I'll stick with their use of 1 month).
     * The tiling size selected is ok, but searches for single profile (by name)
     * are slow since a given file may have a wide range of station_ids.
     *
     * @param type the 3 letter WOD file type e.g., APB
     * @param previousDownloadDate iso date after previous download finished (e.g., 2011-05-15)
     */
/*    public static void bobConsolidateWOD(String type, String previousDownloadDate) 
        throws Throwable {

        //constants
        int chunkSize = 45;  //lon width, lat height of a tile, in degrees
        String sourceDir   = "c:/data/wod/monthly/" + type + "/"; 
        String destDir     = "c:/data/wod/consolidated/" + type + "/";
        String logFile     = "c:/data/wod/bobConsolidateWOD.log"; 

        //derived
        double previousDownload = Calendar2.isoStringToEpochSeconds(previousDownloadDate);
        int nLon = 360 / chunkSize;
        int nLat = 180 / chunkSize;
        Table.verbose = false;
        Table.reallyVerbose = false;
        NcHelper.verbose = false;
        String2.setupLog(true, false, logFile, false, 1000000000);
        String2.log("*** starting bobConsolidateWOD(" + type + ", " + previousDownloadDate + ") " + 
            Calendar2.getCurrentISODateTimeStringLocal() + "\n" +
            "logFile=" + String2.logFileName() + "\n" +
            String2.standardHelpAboutMessage());

        //go through the source dirs
        String sourceMonthDirs[] = RegexFilenameFilter.list(sourceDir, ".*");
        for (int sd = 0; sd < sourceMonthDirs.length; sd++) {
            String2.log("\n*** Look for new files in " + sourceDir + sourceMonthDirs[sd]);
            String sourceFiles[] = RegexFilenameFilter.list(
                sourceDir + sourceMonthDirs[sd], ".*\\.nc");
            int nSourceFiles = 
100; //sourceFiles.length;

            //are any files newer than lastDownload?
            boolean newer = false;
            for (int sf = 0; sf < nSourceFiles; sf++) {
                if (File2.getLastModified(sourceDir + sourceMonthDirs[sd] + "/" + sourceFiles[sf]) >
                    previousDownload) {
                    newer = true;
                    break;
                }
            }
            if (!newer) 
                continue;

            //make/empty the destDirectory for this sourceMonths
            File2.makeDirectory( destDir + sourceMonthDirs[sd]);
            File2.deleteAllFiles(destDir + sourceMonthDirs[sd]);

            //read source files
            Table cumTable = new Table();
            for (int sf = 0; sf < nSourceFiles; sf++) {
                String tFileName = sourceDir + sourceMonthDirs[sd] + "/" + sourceFiles[sf];
                try {
                    if (sf % 100 == 0) 
                        String2.log("reading file #" + sf + " of " + nSourceFiles);
                    Table tTable = new Table();
                    tTable.readFlat0Nc(tFileName, null, 0, -1);  //0=don't unpack  -1=read all rows
                    int tNRows = tTable.nRows();

                    //ensure expected columns
                    if (type.equals("APB")) {
                        tTable.justKeepColumns(
                                String.class, //id
                                  int.class,
                                  float.class,
                                  float.class,
                                  double.class,
                                int.class, //date
                                  float.class, 
                                  int.class,
                                  char.class,
                                  float.class,
                                char.class, //dataset
                                  float.class,
                                  float.class, //Temp
                                  int.class,
                                  int.class,
                                float.class, //Sal
                                  int.class,
                                  int.class,
                                  float.class, //Press
                                  int.class,
                                int.class,
                                  int.class, //crs
                                  int.class, 
                                  int.class, 
                                  int.class, 
                                int.class},  //WODfd
                              new String[]{
                                "WOD_cruise_identifier", 
                                  "wod_unique_cast",  
                                  "lat", 
                                  "lon", 
                                  "time", 
                                "date", 
                                  "GMT_time", 
                                  "Access_no", 
                                  "Institute", 
                                  "Orig_Stat_Num", 
                                "dataset", 
                                  "z", 
                                  "Temperature", 
                                  "Temperature_sigfigs", 
                                  "Temperature_WODflag", 
                                "Salinity", 
                                  "Salinity_sigfigs", 
                                  "Salinity_WODflag", 
                                  "Pressure",
                                "Pressure_sigfigs",
                                  "Pressure_WODflag",
                                  "crs",
                                  "profile",
                                  "WODf",
                                  "WODfp",
                                "WODfd"},
                            new Class[]{
                            });



                        if (!tTable.getColumnName(0).equals("WOD_cruise_identifier")) {
                            tTable.addColumn(0, "WOD_cruise_identifier", 
                                PrimitiveArray.factory(String.class, tNRows, ""),
                                new Attributes());
                        }
                        if (!tTable.getColumnName(6).equals("GMT_time")) {
                            tTable.addColumn(0, "GMT_time", 
                                PrimitiveArray.factory(float.class, tNRows, ""),
                                new Attributes());
                        }

float GMT_time ;
        GMT_time:long_name = "GMT_time" ;
int Access_no ;
        Access_no:long_name = "NODC_accession_number" ;
        Access_no:units = "NODC_code" ;
        Access_no:comment = "used to find original data at NODC" ;
char Platform(strnlen) ;
        Platform:long_name = "Platform_name" ;
        Platform:comment = "name of platform from which measurements were taken" ;
float Orig_Stat_Num ;
        Orig_Stat_Num:long_name = "Originators_Station_Number" ;
        Orig_Stat_Num:comment = "number assigned to a given station by data originator" ;
char Cast_Direction(strnlen) ;
        Cast_Direction:long_name = "Cast_Direction" ;
char dataset(strnlen) ;
        dataset:long_name = "WOD_dataset" ;
char Recorder(strnlen) ;
        Recorder:long_name = "Recorder" ;
        Recorder:units = "WMO code 4770" ;
        Recorder:comment = "Device which recorded measurements" ;
char real_time(strnlen) ;
        real_time:long_name = "real_time_data" ;
        real_time:comment = "set if data are from the global telecommuncations system" ;
char dbase_orig(strnlen) ;
        dbase_orig:long_name = "database_origin" ;
        dbase_orig:comment = "Database from which data were extracted" ;
float z(z) ;

                        if (!tTable.getColumnName(18).equals("Salinity")) {
                            tTable.addColumn(18, "Salinity", 
                                PrimitiveArray.factory(float.class, tNRows, ""),
                                new Attributes());
                            tTable.addColumn(19, "Salinity_sigfigs", 
                                PrimitiveArray.factory(int.class, tNRows, ""),
                                new Attributes());
                            tTable.addColumn(20, "Salinity_WODflag", 
                                PrimitiveArray.factory(int.class, tNRows, ""),
                                (new Attributes()));
                        }
                        if (!tTable.getColumnName(21).equals("Pressure")) {
                            tTable.addColumn(21, "Pressure", 
                                PrimitiveArray.factory(float.class, tNRows, ""),
                                new Attributes());
                            tTable.addColumn(22, "Pressure_sigfigs", 
                                PrimitiveArray.factory(int.class, tNRows, ""),
                                new Attributes());
                            tTable.addColumn(23, "Pressure_WODflag", 
                                PrimitiveArray.factory(int.class, tNRows, ""),
                                (new Attributes()));
                        }
                    }


                    //convert metadata to data
                    if (type.equals("APB")) {

                        //WOD_cruise_identifier
                        metadataToData(tTable, "WOD_cruise_identifier", "country",
                            "WOD_cruise_country", String.class);
                        metadataToData(tTable, "WOD_cruise_identifier", "originators_cruise_identifier",
                            "WOD_cruise_originatorsID", String.class);
                        metadataToData(tTable, "WOD_cruise_identifier", "Primary_Investigator",
                            "WOD_cruise_Primary_Investigator", String.class);

                        //Temperature
                        metadataToData(tTable, "Temperature", "Instrument_(WOD_code)",
                            "Temperature_Instrument", String.class);
                        metadataToData(tTable, "Temperature", "WODprofile_flag",
                            "Temperature_WODprofile_flag", int.class);
                    }

                    //validate
                    double stats[];
                    int tNCols = tTable.nColumns();
                    PrimitiveArray col;

                    col = tTable.getColumn("lon");
                    stats = col.calculateStats();
                    if (stats[PrimitiveArray.STATS_MIN] < -180) 
                        String2.log("  ! minLon=" + stats[PrimitiveArray.STATS_MIN]);
                    if (stats[PrimitiveArray.STATS_MAX] > 180) 
                        String2.log("  ! maxLon=" + stats[PrimitiveArray.STATS_MAX]);

                    col = tTable.getColumn("lat");
                    stats = col.calculateStats();
                    if (stats[PrimitiveArray.STATS_MIN] < -90) 
                        String2.log("  ! minLat=" + stats[PrimitiveArray.STATS_MIN]);
                    if (stats[PrimitiveArray.STATS_MAX] > 90) 
                        String2.log("  ! maxLat=" + stats[PrimitiveArray.STATS_MAX]);

                    //append
                    if (sf == 0) {
                        cumTable = tTable;
                    } else {
                        //ensure colNames same
                        Test.ensureEqual(
                            tTable.getColumnNamesCSSVString(),
                            cumTable.getColumnNamesCSSVString(),
                            "Different column names.");

                        //append
                        cumTable.append(tTable);
                    }

                } catch (Throwable t) {
                    String2.log("ERROR: when processing " + tFileName + "\n" + 
                        MustBe.throwableToString(t));
                }
            }

            //sort
            String2.log("sorting");
            int timeCol = cumTable.findColumnNumber("time");
            int idCol   = cumTable.findColumnNumber("wod_unique_cast");
            Test.ensureNotEqual(timeCol, -1, "time column not found in " + 
                cumTable.getColumnNamesCSSVString());
            Test.ensureNotEqual(idCol, -1, "wod_unique_cast column not found in " + 
                cumTable.getColumnNamesCSSVString());
            cumTable.ascendingSort(new int[]{timeCol, idCol});

            //write consolidated data as tiles
            int cumNRows = cumTable.nRows();
            PrimitiveArray lonCol = cumTable.getColumn("lon");
            PrimitiveArray latCol = cumTable.getColumn("lat");
            for (int loni = 0; loni < nLon; loni++) {
                double minLon = -180 + loni * chunkSize;
                double maxLon = minLon + chunkSize + (loni == nLon-1? 0.1 : 0);
                for (int lati = 0; lati < nLat; lati++) {
                    double minLat = -90 + lati * chunkSize;
                    double maxLat = minLat + chunkSize + (lati == nLat-1? 0.1 : 0);
                    Table tTable = (Table)cumTable.clone();
                    BitSet keep = new BitSet(cumNRows);                    
                    for (int row = 0; row < cumNRows; row++) {
                        double lon = lonCol.getDouble(row);
                        double lat = latCol.getDouble(row);
                        keep.set(row, 
                            lon >= minLon && lon < maxLon &&
                            lat >= minLat && lat < maxLat);
                    }
                    tTable.justKeep(keep);
                    if (tTable.nRows() == 0) {
                        String2.log("No data for minLon=" + minLon + " minLat=" + minLat);
                    } else {
                        tTable.saveAsFlatNc(
                            destDir + sourceMonthDirs[sd] + "/" +
                                sourceMonthDirs[sd] + "_" + 
                                Math.round(minLon) + "E_" +
                                Math.round(minLat) + "N",
                            "row", false); //convertToFakeMissingValues
                    }
                }
            }
        }
        String2.returnLoggingToSystemOut();
    }
*/

    /** For WOD, get all source variable names and file they are in.
     */
    public static void getAllSourceVariableNames(String dir, String fileNameRegex) {
        HashSet hashset = new HashSet();
        String2.log("\n*** getAllsourceVariableNames from " + dir + " " + fileNameRegex);
        Table.verbose = false;
        Table.reallyVerbose = false;
        String sourceFiles[] = RegexFilenameFilter.recursiveFullNameList(
            dir, fileNameRegex, false);
        int nSourceFiles = sourceFiles.length;

        Table table = new Table();
        for (int sf = 0; sf < nSourceFiles; sf++) {
            try {
                table.readNDNc(sourceFiles[sf], null, null, 0, 0, true); // getMetadata
            } catch (Throwable t) {
                String2.log(MustBe.throwableToString(t));
            }
            int nCols = table.nColumns();
            for (int c = 0; c < nCols; c++) {
                String colName = table.getColumnName(c);
                if (hashset.add(colName)) {
                    String2.log("colName=" + colName + 
                          "  " + table.getColumn(c).elementClassString() +
                        "\n  file=" + sourceFiles[sf] + 

                        "\n  attributes=\n" + table.columnAttributes(c).toString());
                }
            }
        }
    }

    /*  rare in APB:
colName=Salinity
  file=f:/data/wod/monthly/APB/199905-200407/wod_010868950O.nc
  attributes=
    coordinates="time lat lon z"
    flag_definitions="WODfp"
    grid_mapping="crs"
    long_name="Salinity"
    standard_name="sea_water_salinity"
    WODprofile_flag=6

colName=Salinity_sigfigs
  file=f:/data/wod/monthly/APB/199905-200407/wod_010868950O.nc
  attributes=

colName=Salinity_WODflag
  file=f:/data/wod/monthly/APB/199905-200407/wod_010868950O.nc
  attributes=
    flag_definitions="WODf"

colName=Pressure
  file=f:/data/wod/monthly/APB/199905-200407/wod_010868950O.nc
  attributes=
    coordinates="time lat lon z"
    grid_mapping="crs"
    long_name="Pressure"
    standard_name="sea_water_pressure"
    units="dbar"

colName=Pressure_sigfigs
  file=f:/data/wod/monthly/APB/199905-200407/wod_010868950O.nc
  attributes=

colName=Pressure_WODflag
  file=f:/data/wod/monthly/APB/199905-200407/wod_010868950O.nc
  attributes=
    flag_definitions="WODf"

colName=Orig_Stat_Num
  file=f:/data/wod/monthly/APB/199905-200407/wod_010868950O.nc
  attributes=
    comment="number assigned to a given station by data originator"
    long_name="Originators_Station_Number"

colName=Bottom_Depth
  file=f:/data/wod/monthly/APB/200904-200906/wod_012999458O.nc
  attributes=
    long_name="Bottom_Depth"
    units="meters"
*/

    /** Tests the data created by getCAMarCatShort() and getCAMarCatLong()
     * and served by erdCAMarCatSM, SY, LM, LY. */
    public static void testCAMarCat() throws Throwable {
        EDDTable eddTable;
        String dir = EDStatic.fullTestCacheDirectory;
        String tName, results, expected;

        //***test short name list
        //http://las.pfeg.noaa.gov:8082/thredds/dodsC/CA_market_catch/ca_fish_grouped_short.nc.ascii
        //  ?landings[12:23][1=Los Angeles][1=Barracuda, California]
/*
landings.landings[12][1][1]
[0][0], 15356
[1][0], 93891
[2][0], 178492
[3][0], 367186
[4][0], 918303
[5][0], 454981
[6][0], 342464
[7][0], 261587
[8][0], 175979
[9][0], 113828
[10][0], 46916
[11][0], 73743

landings.time_series[12]
254568.0, 255312.0, 255984.0, 256728.0, 257448.0, 258192.0, 258912.0, 259656.0, 260400.0, 261120.0, 261864.0, 262584.0

254568 hours since 1900-01-01 = 1929-01-16T00:00:00Z
262584 hours since 1900-01-01 = 1929-12-16T00:00:00Z
*/

        for (int i = 0; i < 2; i++) {
            char SL = i==0? 'S' : 'L';

            //*** monthly
            String2.log("\n**** test erdCAMarCat" + SL + "M");
            eddTable = (EDDTable)oneFromDatasetsXml(null, "erdCAMarCat" + SL + "M"); 
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&port=\"Los Angeles\"&fish=\"Barracuda, California\"&year=1929", 
                dir, eddTable.className() + "_" + SL + "M", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected = 
"time,year,fish,port,landings\n" +
"UTC,,,,pounds\n" +
"1929-01-16T00:00:00Z,1929,\"Barracuda, California\",Los Angeles,15356\n" +
"1929-02-16T00:00:00Z,1929,\"Barracuda, California\",Los Angeles,93891\n" +
"1929-03-16T00:00:00Z,1929,\"Barracuda, California\",Los Angeles,178492\n" +
"1929-04-16T00:00:00Z,1929,\"Barracuda, California\",Los Angeles,367186\n" +
"1929-05-16T00:00:00Z,1929,\"Barracuda, California\",Los Angeles,918303\n" +
"1929-06-16T00:00:00Z,1929,\"Barracuda, California\",Los Angeles,454981\n" +
"1929-07-16T00:00:00Z,1929,\"Barracuda, California\",Los Angeles,342464\n" +
"1929-08-16T00:00:00Z,1929,\"Barracuda, California\",Los Angeles,261587\n" +
"1929-09-16T00:00:00Z,1929,\"Barracuda, California\",Los Angeles,175979\n" +
"1929-10-16T00:00:00Z,1929,\"Barracuda, California\",Los Angeles,113828\n" +
"1929-11-16T00:00:00Z,1929,\"Barracuda, California\",Los Angeles,46916\n" +
"1929-12-16T00:00:00Z,1929,\"Barracuda, California\",Los Angeles,73743\n";
            Test.ensureEqual(results, expected, "erdCAMarCat" + SL + "M results=\n" + results);

            //salmon became separable in 1972. Separation appears in Long list of names, not Short.
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&port=\"All\"&fish=\"Salmon\"&year>=1976&year<=1977", 
                dir, eddTable.className() + "_" + SL + "M", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected = SL == 'S'?
"time,year,fish,port,landings\n" +
"UTC,,,,pounds\n" +
"1976-01-16T00:00:00Z,1976,Salmon,All,0\n" +
"1976-02-16T00:00:00Z,1976,Salmon,All,0\n" +
"1976-03-16T00:00:00Z,1976,Salmon,All,0\n" +
"1976-04-16T00:00:00Z,1976,Salmon,All,465896\n" +
"1976-05-16T00:00:00Z,1976,Salmon,All,1932912\n" +
"1976-06-16T00:00:00Z,1976,Salmon,All,2770715\n" +
"1976-07-16T00:00:00Z,1976,Salmon,All,1780115\n" +
"1976-08-16T00:00:00Z,1976,Salmon,All,581702\n" +
"1976-09-16T00:00:00Z,1976,Salmon,All,244695\n" +
"1976-10-16T00:00:00Z,1976,Salmon,All,485\n" +
"1976-11-16T00:00:00Z,1976,Salmon,All,0\n" +
"1976-12-16T00:00:00Z,1976,Salmon,All,0\n" +
"1977-01-16T00:00:00Z,1977,Salmon,All,0\n" +
"1977-02-16T00:00:00Z,1977,Salmon,All,43\n" +
"1977-03-16T00:00:00Z,1977,Salmon,All,0\n" +
"1977-04-16T00:00:00Z,1977,Salmon,All,680999\n" +
"1977-05-16T00:00:00Z,1977,Salmon,All,1490628\n" +
"1977-06-16T00:00:00Z,1977,Salmon,All,980842\n" +
"1977-07-16T00:00:00Z,1977,Salmon,All,1533176\n" +
"1977-08-16T00:00:00Z,1977,Salmon,All,928234\n" +
"1977-09-16T00:00:00Z,1977,Salmon,All,314464\n" +
"1977-10-16T00:00:00Z,1977,Salmon,All,247\n" +
"1977-11-16T00:00:00Z,1977,Salmon,All,0\n" +
"1977-12-16T00:00:00Z,1977,Salmon,All,0\n"
:
"time,year,fish,port,landings\n" +
"UTC,,,,pounds\n" +
"1976-01-16T00:00:00Z,1976,Salmon,All,0\n" +
"1976-02-16T00:00:00Z,1976,Salmon,All,0\n" +
"1976-03-16T00:00:00Z,1976,Salmon,All,0\n" +
"1976-04-16T00:00:00Z,1976,Salmon,All,465896\n" +
"1976-05-16T00:00:00Z,1976,Salmon,All,1932912\n" +
"1976-06-16T00:00:00Z,1976,Salmon,All,2770715\n" +
"1976-07-16T00:00:00Z,1976,Salmon,All,1780115\n" +
"1976-08-16T00:00:00Z,1976,Salmon,All,581702\n" +
"1976-09-16T00:00:00Z,1976,Salmon,All,244695\n" +
"1976-10-16T00:00:00Z,1976,Salmon,All,485\n" +
"1976-11-16T00:00:00Z,1976,Salmon,All,0\n" +
"1976-12-16T00:00:00Z,1976,Salmon,All,0\n" +
"1977-01-16T00:00:00Z,1977,Salmon,All,0\n" +
"1977-02-16T00:00:00Z,1977,Salmon,All,0\n" +
"1977-03-16T00:00:00Z,1977,Salmon,All,0\n" +
"1977-04-16T00:00:00Z,1977,Salmon,All,1223\n" +
"1977-05-16T00:00:00Z,1977,Salmon,All,1673\n" +
"1977-06-16T00:00:00Z,1977,Salmon,All,2333\n" +
"1977-07-16T00:00:00Z,1977,Salmon,All,1813\n" +
"1977-08-16T00:00:00Z,1977,Salmon,All,1300\n" +
"1977-09-16T00:00:00Z,1977,Salmon,All,924\n" +
"1977-10-16T00:00:00Z,1977,Salmon,All,0\n" +
"1977-11-16T00:00:00Z,1977,Salmon,All,0\n" +
"1977-12-16T00:00:00Z,1977,Salmon,All,0\n";
            Test.ensureEqual(results, expected, "erdCAMarCat" + SL + "M results=\n" + results);

            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&port=\"All\"&fish=\"Salmon\"&time>=1976-01-01&time<=1977-12-31", 
                dir, eddTable.className() + "_" + SL + "M", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            Test.ensureEqual(results, expected, "erdCAMarCat" + SL + "M results=\n" + results);


            //*** yearly 
            String2.log("\n**** test erdCAMarCat" + SL + "Y");
            eddTable = (EDDTable)oneFromDatasetsXml(null, "erdCAMarCat" + SL + "Y"); 
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&port=\"Los Angeles\"&fish=\"Barracuda, California\"&year=1929", 
                dir, eddTable.className() + "_" + SL + "Y", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected = 
"time,year,fish,port,landings\n" +
"UTC,,,,pounds\n" +
"1929-07-01T00:00:00Z,1929,\"Barracuda, California\",Los Angeles,3042726\n";
            Test.ensureEqual(results, expected, "erdCAMarCat" + SL + "Y results=\n" + results);

            //salmon became separable in 1972. Separation appears in Long list of names, not Short.
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&port=\"All\"&fish=\"Salmon\"&year>=1971&year<=1980", 
                dir, eddTable.className() + "_" + SL + "M", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            expected = SL == 'S'?
"time,year,fish,port,landings\n" +
"UTC,,,,pounds\n" +
"1971-07-01T00:00:00Z,1971,Salmon,All,8115432\n" +
"1972-07-01T00:00:00Z,1972,Salmon,All,6422171\n" +
"1973-07-01T00:00:00Z,1973,Salmon,All,9668966\n" +
"1974-07-01T00:00:00Z,1974,Salmon,All,8749013\n" +
"1975-07-01T00:00:00Z,1975,Salmon,All,6925082\n" +
"1976-07-01T00:00:00Z,1976,Salmon,All,7776520\n" +
"1977-07-01T00:00:00Z,1977,Salmon,All,5928633\n" +
"1978-07-01T00:00:00Z,1978,Salmon,All,6810880\n" +
"1979-07-01T00:00:00Z,1979,Salmon,All,8747677\n" +
"1980-07-01T00:00:00Z,1980,Salmon,All,6021953\n"
:
"time,year,fish,port,landings\n" +
"UTC,,,,pounds\n" +
"1971-07-01T00:00:00Z,1971,Salmon,All,8115432\n" +
"1972-07-01T00:00:00Z,1972,Salmon,All,6422171\n" +
"1973-07-01T00:00:00Z,1973,Salmon,All,9668966\n" +
"1974-07-01T00:00:00Z,1974,Salmon,All,8749013\n" +
"1975-07-01T00:00:00Z,1975,Salmon,All,6925082\n" +
"1976-07-01T00:00:00Z,1976,Salmon,All,7776520\n" +
"1977-07-01T00:00:00Z,1977,Salmon,All,9266\n" +
"1978-07-01T00:00:00Z,1978,Salmon,All,21571\n" +
"1979-07-01T00:00:00Z,1979,Salmon,All,30020\n" +
"1980-07-01T00:00:00Z,1980,Salmon,All,40653\n";
            Test.ensureEqual(results, expected, "erdCAMarCat" + SL + "M results=\n" + results);

            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&port=\"All\"&fish=\"Salmon\"&time>=1971-01-01&time<=1980-12-31", 
                dir, eddTable.className() + "_" + SL + "M", ".csv"); 
            results = new String((new ByteArray(dir + tName)).toArray());
            Test.ensureEqual(results, expected, "erdCAMarCat" + SL + "M results=\n" + results);

        
        }
        String2.log("\n**** test erdCAMarCat finished successfully");
    }


    /** Tests the data created by getCAMarCatLong()
     * and served by erdCAMarCatLM and erdCAMarCatLY. */
    public static void testCAMarCatL() throws Throwable {
        EDDTable eddTable;
        String dir = EDStatic.fullTestCacheDirectory;
        String tName, results, expected;

        //*** test long name list
        //http://las.pfeg.noaa.gov:8082/thredds/dodsC/CA_market_catch/ca_fish_grouped.nc.ascii
        //  ?landings[12:23][1=Los Angeles][2=Barracuda, California]
/*
landings.landings[12][1][1]
[0][0], 15356
[1][0], 93891
[2][0], 178492
[3][0], 367186
[4][0], 918303
[5][0], 454981
[6][0], 342464
[7][0], 261587
[8][0], 175979
[9][0], 113828
[10][0], 46916
[11][0], 73743
*/

    }

    /**
     * Test making an .ncCF Point file.
     */
    public static void testNcCFPoint() throws Throwable {

        try {

        String2.log("\n*** EDDTableFromNcFiles.testNcCFPoint");
        //this dataset is not fromNcFiles, but test here with other testNcCF tests
        EDDTable tedd = (EDDTable)oneFromDatasetsXml(null, "nwioosCoral"); 
        String tName, error, results, expected;
        int po;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
        String2.log("\nafter constructor, combinedGlobal=\n" + tedd.combinedGlobalAttributes());

        //lon lat time range 
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "longitude,latitude,depth,time,taxa_scientific,institution,species_code" +
            "&taxa_scientific=\"Alyconaria unident.\"", 
            EDStatic.fullTestCacheDirectory, "ncCF", ".ncCF"); 
        results = NcHelper.dumpString(EDStatic.fullTestCacheDirectory + tName, true);
        //String2.log(results);
        expected = 
"netcdf ncCF.nc {\n" +
"  dimensions:\n" +
"    row = 4;\n" +
"    taxa_scientific_strlen = 19;\n" +
"    institution_strlen = 4;\n" +
"  variables:\n" +
"    double longitude(row=4);\n" +
"      :_CoordinateAxisType = \"Lon\";\n" +
"      :actual_range = -125.00184631347656, -121.3140640258789; // double\n" +
"      :axis = \"X\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Longitude\";\n" +
"      :standard_name = \"longitude\";\n" +
"      :units = \"degrees_east\";\n" +
"\n" +
"    double latitude(row=4);\n" +
"      :_CoordinateAxisType = \"Lat\";\n" +
"      :actual_range = 34.911373138427734, 47.237003326416016; // double\n" +
"      :axis = \"Y\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Latitude\";\n" +
"      :standard_name = \"latitude\";\n" +
"      :units = \"degrees_north\";\n" +
"\n" +
"    double depth(row=4);\n" +
"      :_CoordinateAxisType = \"Height\";\n" +
"      :_CoordinateZisPositive = \"down\";\n" +
"      :actual_range = 77.0, 474.0; // double\n" +  // note that this is for a subset of the dataset
"      :axis = \"Z\";\n" +
"      :colorBarMaximum = 1500.0; // double\n" +
"      :colorBarMinimum = 0.0; // double\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Depth\";\n" +
"      :positive = \"down\";\n" +
"      :standard_name = \"depth\";\n" +
"      :units = \"m\";\n" +
"\n" +
"    double time(row=4);\n" +
"      :_CoordinateAxisType = \"Time\";\n" +
"      :actual_range = 8.836128E8, 9.783072E8; // double\n" +
"      :axis = \"T\";\n" +
"      :Description = \"Year of Survey.\";\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Time (Beginning of Survey Year)\";\n" +
"      :standard_name = \"time\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    char taxa_scientific(row=4, taxa_scientific_strlen=19);\n" +
"      :coordinates = \"time latitude longitude depth\";\n" +
"      :Description = \"Scientific name of taxa\";\n" +
"      :ioos_category = \"Taxonomy\";\n" +
"      :long_name = \"Taxa Scientific\";\n" +
"\n" +
"    char institution(row=4, institution_strlen=4);\n" +
"      :coordinates = \"time latitude longitude depth\";\n" +
"      :Description = \"Institution is either: Northwest Fisheries Science Center (FRAM Division) or Alaska Fisheries Science Center (RACE Division)\";\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"Institution\";\n" +
"\n" +
"    double species_code(row=4);\n" +
"      :actual_range = 41101.0, 41101.0; // double\n" +
"      :coordinates = \"time latitude longitude depth\";\n" +
"      :Description = \"Unique identifier for species.\";\n" +
"      :ioos_category = \"Taxonomy\";\n" +
"      :long_name = \"Species Code\";\n" +
"\n" +
"  // global attributes:\n" +
"  :cdm_data_type = \"Point\";\n" +
"  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"  :Easternmost_Easting = -121.3140640258789; // double\n" +
"  :featureType = \"Point\";\n" +
"  :geospatial_lat_max = 47.237003326416016; // double\n" +
"  :geospatial_lat_min = 34.911373138427734; // double\n" +
"  :geospatial_lat_units = \"degrees_north\";\n" +
"  :geospatial_lon_max = -121.3140640258789; // double\n" +
"  :geospatial_lon_min = -125.00184631347656; // double\n" +
"  :geospatial_lon_units = \"degrees_east\";\n" +
"  :geospatial_vertical_max = 474.0; // double\n" +
"  :geospatial_vertical_min = 77.0; // double\n" +
"  :geospatial_vertical_positive = \"down\";\n" +
"  :geospatial_vertical_units = \"m\";\n" +
"  :history = \"" + today;
        String tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

//  + " http://nwioos.coas.oregonstate.edu:8080/dods/drds/Coral%201980-2005\n" +
//today + 

expected = 
"http://127.0.0.1:8080/cwexperimental/tabledap/nwioosCoral.ncCF?longitude,latitude,depth,time,taxa_scientific,institution,species_code&taxa_scientific=\\\"Alyconaria unident.\\\"\";\n" +
"  :id = \"ncCF\";\n" +
"  :infoUrl = \"http://nwioos.coas.oregonstate.edu:8080/dods/drds/Coral%201980-2005.info\";\n" +
"  :institution = \"NOAA NWFSC\";\n" +
"  :keywords = \"Biosphere > Aquatic Ecosystems > Coastal Habitat,\n" +
"Biosphere > Aquatic Ecosystems > Marine Habitat,\n" +
"Biological Classification > Animals/Invertebrates > Cnidarians > Anthozoans/Hexacorals > Hard Or Stony Corals,\n" +
"1980-2005, abbreviation, atmosphere, beginning, coast, code, collected, coral, data, depth, family, genus, height, identifier, institution, noaa, nwfsc, off, order, scientific, species, station, survey, taxa, taxonomic, taxonomy, time, west, west coast, year\";\n" +
"  :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
"  :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"  :Northernmost_Northing = 47.237003326416016; // double\n" +
"  :sourceUrl = \"http://nwioos.coas.oregonstate.edu:8080/dods/drds/Coral%201980-2005\";\n" +
"  :Southernmost_Northing = 34.911373138427734; // double\n" +
"  :standard_name_vocabulary = \"CF Standard Name Table v29\";\n" +
"  :subsetVariables = \"longitude, latitude, depth, time, institution, institution_id, species_code, taxa_scientific, taxonomic_order, order_abbreviation, taxonomic_family, family_abbreviation, taxonomic_genus\";\n" +
"  :summary = \"This data contains the locations of some observations of\n" +
"cold-water/deep-sea corals off the west coast of the United States.\n" +
"Records of coral catch originate from bottom trawl surveys conducted\n" +
"from 1980 to 2001 by the Alaska Fisheries Science Center (AFSC) and\n" +
"2001 to 2005 by the Northwest Fisheries Science Center (NWFSC).\n" +
"Locational information represent the vessel mid positions (for AFSC\n" +
"survey trawls) or \\\"best position\\\" (i.e., priority order: 1) gear\n" +
"midpoint 2) vessel midpoint, 3) vessel start point, 4) vessel end\n" +
"point, 5) station coordinates for NWFSC survey trawls) conducted as\n" +
"part of regular surveys of groundfish off the coasts of Washington,\n" +
"Oregon and California by NOAA Fisheries. Only records where corals\n" +
"were identified in the total catch are included. Each catch sample\n" +
"of coral was identified down to the most specific taxonomic level\n" +
"possible by the biologists onboard, therefore identification was\n" +
"dependent on their expertise. When positive identification was not\n" +
"possible, samples were sometimes archived for future identification\n" +
"by systematist experts. Data were compiled by the NWFSC, Fishery\n" +
"Resource Analysis & Monitoring Division\n" +
"\n" +
"Purpose - Examination of the spatial and temporal distributions of\n" +
"observations of cold-water/deep-sea corals off the west coast of the\n" +
"United States, including waters off the states of Washington, Oregon,\n" +
"and California. It is important to note that these records represent\n" +
"only presence of corals in the area swept by the trawl gear. Since\n" +
"bottom trawls used during these surveys are not designed to sample\n" +
"epibenthic invertebrates, absence of corals in the catch does not\n" +
"necessary mean they do not occupy the area swept by the trawl gear.\n" +
"\n" +
"Data Credits - NOAA Fisheries, Alaska Fisheries Science Center,\n" +
"Resource Assessment & Conservation Engineering Division (RACE) NOAA\n" +
"Fisheries, Northwest Fisheries Science Center, Fishery Resource\n" +
"Analysis & Monitoring Division (FRAM)\n" +
"\n" +
"Contact: Curt Whitmire, NOAA NWFSC, Curt.Whitmire@noaa.gov\";\n" +
"  :time_coverage_end = \"2001-01-01T00:00:00Z\";\n" +
"  :time_coverage_start = \"1998-01-01T00:00:00Z\";\n" +
"  :title = \"NWFSC Coral Data Collected off West Coast of US (1980-2005)\";\n" +
"  :Westernmost_Easting = -125.00184631347656; // double\n" +
" data:\n" +
"longitude =\n" +
"  {-125.00184631347656, -124.56075286865234, -121.42329406738281, -121.3140640258789}\n" +
"latitude =\n" +
"  {47.237003326416016, 47.082645416259766, 34.911720275878906, 34.911373138427734}\n" +
"depth =\n" +
"  {450.0, 77.0, 462.0, 474.0}\n" +
"time =\n" +
"  {9.783072E8, 9.783072E8, 8.836128E8, 8.836128E8}\n" +
"taxa_scientific =\"Alyconaria unident.\", \"Alyconaria unident.\", \"Alyconaria unident.\", \"Alyconaria unident.\"\n" +
"institution =\"RACE\", \"RACE\", \"RACE\", \"RACE\"\n" +
"species_code =\n" +
"  {41101.0, 41101.0, 41101.0, 41101.0}\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);

        String2.log("\n*** EDDTableFromNcFiles.testNcCFPoint finished.");


        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\n2014-08-08 Known problem: nwioos source currently isn't working."); 
        }

    }

    /**
     * Test making an .ncCF TimeSeries file.  
     */
    public static void testNcCF1a() throws Throwable {

        String2.log("\n*** EDDTableFromNcFiles.testNcCF1a");
        EDDTable tedd = (EDDTable)oneFromDatasetsXml(null, "cwwcNDBCMet"); //should work
        String tName, error, results, expected;
        int po;
        String query = "longitude,latitude,station,time,atmp,wtmp" +
            "&longitude>-123&longitude<-122&latitude>37&latitude<38" +
            "&time>=2005-05-01T00&time<=2005-05-01T03";
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

        //lon lat time range 
        tName = tedd.makeNewFileForDapQuery(null, null, query,
            EDStatic.fullTestCacheDirectory, "ncCF1a", ".ncCF"); 
        results = NcHelper.dumpString(EDStatic.fullTestCacheDirectory + tName, true);
        //String2.log(results);
        expected = 
"netcdf ncCF1a.nc {\n" +
"  dimensions:\n" +
"    timeseries = 7;\n" +
"    obs = 28;\n" +
"    station_strlen = 5;\n" +
"  variables:\n" +
"    float longitude(timeseries=7);\n" +
"      :_CoordinateAxisType = \"Lon\";\n" +
"      :actual_range = -122.975f, -122.21f; // float\n" +
"      :axis = \"X\";\n" +
"      :comment = \"The longitude of the station.\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Longitude\";\n" +
"      :standard_name = \"longitude\";\n" +
"      :units = \"degrees_east\";\n" +
"\n" +
"    float latitude(timeseries=7);\n" +
"      :_CoordinateAxisType = \"Lat\";\n" +
"      :actual_range = 37.363f, 37.997f; // float\n" +
"      :axis = \"Y\";\n" +
"      :comment = \"The latitude of the station.\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Latitude\";\n" +
"      :standard_name = \"latitude\";\n" +
"      :units = \"degrees_north\";\n" +
"\n" +
"    char station(timeseries=7, station_strlen=5);\n" +
"      :cf_role = \"timeseries_id\";\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"Station Name\";\n" +
"\n" +
"    int rowSize(timeseries=7);\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"Number of Observations for this TimeSeries\";\n" +
"      :sample_dimension = \"obs\";\n" +  
"\n" +
"    double time(obs=28);\n" +
"      :_CoordinateAxisType = \"Time\";\n" +
"      :actual_range = 1.1149056E9, 1.1149164E9; // double\n" +
"      :axis = \"T\";\n" +
"      :comment = \"Time in seconds since 1970-01-01T00:00:00Z. The original times are rounded to the nearest hour.\";\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Time\";\n" +
"      :standard_name = \"time\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    float atmp(obs=28);\n" +
"      :_FillValue = -9999999.0f; // float\n" +
"      :actual_range = 13.2f, 15.5f; // float\n" +
"      :colorBarMaximum = 40.0; // double\n" +
"      :colorBarMinimum = 0.0; // double\n" +
"      :comment = \"Air temperature (Celsius). For sensor heights on buoys, see Hull Descriptions. For sensor heights at C-MAN stations, see C-MAN Sensor Locations.\";\n" +
"      :coordinates = \"time latitude longitude\";\n" +
"      :ioos_category = \"Temperature\";\n" +
"      :long_name = \"Air Temperature\";\n" +
"      :missing_value = -9999999.0f; // float\n" +
"      :standard_name = \"air_temperature\";\n" +
"      :units = \"degree_C\";\n" +
"\n" +
"    float wtmp(obs=28);\n" +
"      :_FillValue = -9999999.0f; // float\n" +
"      :actual_range = 9.3f, 17.1f; // float\n" +
"      :colorBarMaximum = 32.0; // double\n" +
"      :colorBarMinimum = 0.0; // double\n" +
"      :comment = \"Sea surface temperature (Celsius). For sensor depth, see Hull Description.\";\n" +
"      :coordinates = \"time latitude longitude\";\n" +
"      :ioos_category = \"Temperature\";\n" +
"      :long_name = \"SST\";\n" +
"      :missing_value = -9999999.0f; // float\n" +
"      :standard_name = \"sea_surface_temperature\";\n" +
"      :units = \"degree_C\";\n" +
"\n" +
"  // global attributes:\n" +
"  :acknowledgement = \"NOAA NDBC and NOAA CoastWatch (West Coast Node)\";\n" +
"  :cdm_data_type = \"TimeSeries\";\n" +
"  :cdm_timeseries_variables = \"station, longitude, latitude\";\n" +
"  :contributor_name = \"NOAA NDBC and NOAA CoastWatch (West Coast Node)\";\n" +
"  :contributor_role = \"Source of data.\";\n" +
"  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"  :creator_email = \"erd.data@noaa.gov\";\n" +
"  :creator_name = \"NOAA NMFS SWFSC ERD\";\n" +
"  :creator_url = \"http://www.pfeg.noaa.gov\";\n" +
"  :Easternmost_Easting = -122.21f; // float\n" +
"  :featureType = \"TimeSeries\";\n" + 
"  :geospatial_lat_max = 37.997f; // float\n" +
"  :geospatial_lat_min = 37.363f; // float\n" +
"  :geospatial_lat_units = \"degrees_north\";\n" +
"  :geospatial_lon_max = -122.21f; // float\n" +
"  :geospatial_lon_min = -122.975f; // float\n" +
"  :geospatial_lon_units = \"degrees_east\";\n" +
"  :geospatial_vertical_positive = \"down\";\n" +
"  :geospatial_vertical_units = \"m\";\n" +
"  :history = \"NOAA NDBC\n" +
today;
        String tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
       
//+ " http://www.ndbc.noaa.gov/\n" +
//today + " http://127.0.0.1:8080/cwexperimental/
String expected2 = 
"tabledap/cwwcNDBCMet.ncCF\\?longitude,latitude,station,time,atmp,wtmp&longitude>-123&longitude<-122&latitude>37&latitude<38&time>=2005-05-01T00&time<=2005-05-01T03\";\n" +
"  :id = \"ncCF1a\";\n" +
"  :infoUrl = \"http://www.ndbc.noaa.gov/\";\n" +
"  :institution = \"NOAA NDBC, NOAA NMFS SWFSC ERD\";\n" +
"  :keywords = \"Atmosphere > Air Quality > Visibility,\n" +
"Atmosphere > Altitude > Planetary Boundary Layer Height,\n" +
"Atmosphere > Atmospheric Pressure > Atmospheric Pressure Measurements,\n" +
"Atmosphere > Atmospheric Pressure > Pressure Tendency,\n" +
"Atmosphere > Atmospheric Pressure > Sea Level Pressure,\n" +
"Atmosphere > Atmospheric Pressure > Static Pressure,\n" +
"Atmosphere > Atmospheric Temperature > Air Temperature,\n" +
"Atmosphere > Atmospheric Temperature > Dew Point Temperature,\n" +
"Atmosphere > Atmospheric Water Vapor > Dew Point Temperature,\n" +
"Atmosphere > Atmospheric Winds > Surface Winds,\n" +
"Oceans > Ocean Temperature > Sea Surface Temperature,\n" +
"Oceans > Ocean Waves > Significant Wave Height,\n" +
"Oceans > Ocean Waves > Swells,\n" +
"Oceans > Ocean Waves > Wave Period,\n" +
"air, air_pressure_at_sea_level, air_temperature, atmosphere, atmospheric, average, boundary, buoy, coastwatch, data, dew point, dew_point_temperature, direction, dominant, eastward, eastward_wind, from, gust, height, identifier, layer, level, measurements, meridional, meteorological, meteorology, name, ndbc, noaa, northward, northward_wind, ocean, oceans, period, planetary, pressure, quality, sea, sea level, sea_surface_swell_wave_period, sea_surface_swell_wave_significant_height, sea_surface_swell_wave_to_direction, sea_surface_temperature, seawater, significant, speed, sst, standard, static, station, surface, surface waves, surface_altitude, swell, swells, temperature, tendency, tendency_of_air_pressure, time, vapor, visibility, visibility_in_air, water, wave, waves, wcn, wind, wind_from_direction, wind_speed, wind_speed_of_gust, winds, zonal\";\n" +
"  :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
"  :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"  :naming_authority = \"gov.noaa.pfeg.coastwatch\";\n" +
"  :NDBCMeasurementDescriptionUrl = \"http://www.ndbc.noaa.gov/measdes.shtml\";\n" +
"  :Northernmost_Northing = 37.997f; // float\n" +
"  :project = \"NOAA NDBC and NOAA CoastWatch \\(West Coast Node\\)\";\n" +
"  :publisher_email = \"erd.data@noaa.gov\";\n" +
"  :publisher_name = \"NOAA NMFS SWFSC ERD\";\n" +
"  :publisher_url = \"http://www.pfeg.noaa.gov\";\n" +
"  :quality = \"Automated QC checks with periodic manual QC\";\n" +
"  :source = \"station observation\";\n" +
"  :sourceUrl = \"http://www.ndbc.noaa.gov/\";\n" +
"  :Southernmost_Northing = 37.363f; // float\n" +
"  :standard_name_vocabulary = \"CF-12\";\n" +
"  :subsetVariables = \"station, longitude, latitude\";\n" +
"  :summary = \"The National Data Buoy Center \\(NDBC\\) distributes meteorological data from\n" +
"moored buoys maintained by NDBC and others. Moored buoys are the weather\n" +
"sentinels of the sea. They are deployed in the coastal and offshore waters\n" +
"from the western Atlantic to the Pacific Ocean around Hawaii, and from the\n" +
"Bering Sea to the South Pacific. NDBC's moored buoys measure and transmit\n" +
"barometric pressure; wind direction, speed, and gust; air and sea\n" +
"temperature; and wave energy spectra from which significant wave height,\n" +
"dominant wave period, and average wave period are derived. Even the\n" +
"direction of wave propagation is measured on many moored buoys.\n" +
"\n" +
"The data is from NOAA NDBC. It has been reformatted by NOAA Coastwatch,\n" +
"West Coast Node. This dataset only has the data that is closest to a\n" +
"given hour. The time values in the dataset are rounded to the nearest hour.\n" +
"\n" +
"This dataset has both historical data \\(quality controlled, before\n" +
"20.{8}T00:00:00Z\\) and near real time data \\(less quality controlled, from\n" + //changes
"20.{8}T00:00:00Z on\\).\";\n" +                                                 //changes
"  :time_coverage_end = \"2005-05-01T03:00:00Z\";\n" +
"  :time_coverage_resolution = \"P1H\";\n" +
"  :time_coverage_start = \"2005-05-01T00:00:00Z\";\n" +
"  :title = \"NDBC Standard Meteorological Buoy Data\";\n" +
"  :Westernmost_Easting = -122.975f; // float\n" +
" data:\n";

String expected3 = expected2 + 
"longitude =\n" +
"  \\{-122.881, -122.833, -122.298, -122.465, -122.975, -122.4, -122.21\\}\n" +
"latitude =\n" +
"  \\{37.363, 37.759, 37.772, 37.807, 37.997, 37.928, 37.507\\}\n" +
"station =\"46012\", \"46026\", \"AAMC1\", \"FTPC1\", \"PRYC1\", \"RCMC1\", \"RTYC1\"\n" +
"rowSize =\n" +
"  \\{4, 4, 4, 4, 4, 4, 4\\}\n" +
"time =\n" +
"  \\{1.1149056E9, 1.1149092E9, 1.1149128E9, 1.1149164E9, 1.1149056E9, 1.1149092E9, 1.1149128E9, 1.1149164E9, 1.1149056E9, 1.1149092E9, 1.1149128E9, 1.1149164E9, 1.1149056E9, 1.1149092E9, 1.1149128E9, 1.1149164E9, 1.1149056E9, 1.1149092E9, 1.1149128E9, 1.1149164E9, 1.1149056E9, 1.1149092E9, 1.1149128E9, 1.1149164E9, 1.1149056E9, 1.1149092E9, 1.1149128E9, 1.1149164E9\\}\n" +
"atmp =\n" +
"  \\{13.3, 13.3, 13.3, 13.3, -9999999.0, 13.3, 13.3, 13.2, 14.9, 14.5, 14.7, 14.2, 13.8, 13.9, 13.6, 13.3, -9999999.0, -9999999.0, -9999999.0, -9999999.0, 15.5, 15.1, 14.5, 13.6, 14.7, 14.7, 15.0, 14.3\\}\n" +
"wtmp =\n" +
"  \\{13.3, 13.3, 13.4, 13.3, -9999999.0, 13.4, 13.3, 13.2, 17.1, 16.6, 15.6, 15.2, -9999999.0, -9999999.0, -9999999.0, -9999999.0, 9.5, 9.3, -9999999.0, 9.6, 14.7, 14.7, 14.5, 14.4, 16.6, 16.6, 16.6, 16.5\\}\n" +
"\\}\n";
        int tPo = results.indexOf(expected3.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureLinesMatch(results.substring(tPo), expected3, "results=\n" + results);

        // .ncCFHeader
        tName = tedd.makeNewFileForDapQuery(null, null, query,
            EDStatic.fullTestCacheDirectory, "ncCF1a", ".ncCFHeader"); 
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];

        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
        
        expected3 = expected2 + "}\n";
        tPo = results.indexOf(expected3.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureLinesMatch(results.substring(tPo), expected3, "results=\n" + results);

        String2.log("\n*** EDDTableFromNcFiles.testNcCF1a finished.");

    }

    /**
     * Test making an .ncCFMA TimeSeries file.
     */
    public static void testNcCFMA1a() throws Throwable {

        String2.log("\n*** EDDTableFromNcFiles.testNcCFMA1a");
        EDDTable tedd = (EDDTable)oneFromDatasetsXml(null, "cwwcNDBCMet"); //should work
        String tName, error, results, expected;
        int po;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

        //lon lat time range 
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "longitude,latitude,station,time,atmp,wtmp" +
            "&longitude>-123&longitude<-122&latitude>37&latitude<38" +
            "&time>=2005-05-01T00&time<=2005-05-01T03", 
            EDStatic.fullTestCacheDirectory, "ncCFMA1a", ".ncCFMA"); 
        results = NcHelper.dumpString(EDStatic.fullTestCacheDirectory + tName, true);
        //String2.log(results);
        expected = 
"netcdf ncCFMA1a.nc {\n" +
"  dimensions:\n" +
"    timeseries = 7;\n" +
"    obs = 4;\n" +
"    station_strlen = 5;\n" +
"  variables:\n" +
"    float longitude(timeseries=7);\n" +
"      :_CoordinateAxisType = \"Lon\";\n" +
"      :actual_range = -122.975f, -122.21f; // float\n" +
"      :axis = \"X\";\n" +
"      :comment = \"The longitude of the station.\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Longitude\";\n" +
"      :standard_name = \"longitude\";\n" +
"      :units = \"degrees_east\";\n" +
"\n" +
"    float latitude(timeseries=7);\n" +
"      :_CoordinateAxisType = \"Lat\";\n" +
"      :actual_range = 37.363f, 37.997f; // float\n" +
"      :axis = \"Y\";\n" +
"      :comment = \"The latitude of the station.\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Latitude\";\n" +
"      :standard_name = \"latitude\";\n" +
"      :units = \"degrees_north\";\n" +
"\n" +
"    char station(timeseries=7, station_strlen=5);\n" +
"      :cf_role = \"timeseries_id\";\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"Station Name\";\n" +
"\n" +
"    double time(timeseries=7, obs=4);\n" +
"      :_CoordinateAxisType = \"Time\";\n" +
"      :_FillValue = NaN; // double\n" +
"      :actual_range = 1.1149056E9, 1.1149164E9; // double\n" +
"      :axis = \"T\";\n" +
"      :comment = \"Time in seconds since 1970-01-01T00:00:00Z. The original times are rounded to the nearest hour.\";\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Time\";\n" +
"      :standard_name = \"time\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    float atmp(timeseries=7, obs=4);\n" +
"      :_FillValue = -9999999.0f; // float\n" +
"      :actual_range = 13.2f, 15.5f; // float\n" +
"      :colorBarMaximum = 40.0; // double\n" +
"      :colorBarMinimum = 0.0; // double\n" +
"      :comment = \"Air temperature (Celsius). For sensor heights on buoys, see Hull Descriptions. For sensor heights at C-MAN stations, see C-MAN Sensor Locations.\";\n" +
"      :coordinates = \"time latitude longitude\";\n" +
"      :ioos_category = \"Temperature\";\n" +
"      :long_name = \"Air Temperature\";\n" +
"      :missing_value = -9999999.0f; // float\n" +
"      :standard_name = \"air_temperature\";\n" +
"      :units = \"degree_C\";\n" +
"\n" +
"    float wtmp(timeseries=7, obs=4);\n" +
"      :_FillValue = -9999999.0f; // float\n" +
"      :actual_range = 9.3f, 17.1f; // float\n" +
"      :colorBarMaximum = 32.0; // double\n" +
"      :colorBarMinimum = 0.0; // double\n" +
"      :comment = \"Sea surface temperature (Celsius). For sensor depth, see Hull Description.\";\n" +
"      :coordinates = \"time latitude longitude\";\n" +
"      :ioos_category = \"Temperature\";\n" +
"      :long_name = \"SST\";\n" +
"      :missing_value = -9999999.0f; // float\n" +
"      :standard_name = \"sea_surface_temperature\";\n" +
"      :units = \"degree_C\";\n" +
"\n" +
"  // global attributes:\n" +
"  :acknowledgement = \"NOAA NDBC and NOAA CoastWatch (West Coast Node)\";\n" +
"  :cdm_data_type = \"TimeSeries\";\n" +
"  :cdm_timeseries_variables = \"station, longitude, latitude\";\n" +
"  :contributor_name = \"NOAA NDBC and NOAA CoastWatch (West Coast Node)\";\n" +
"  :contributor_role = \"Source of data.\";\n" +
"  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"  :creator_email = \"erd.data@noaa.gov\";\n" +
"  :creator_name = \"NOAA NMFS SWFSC ERD\";\n" +
"  :creator_url = \"http://www.pfeg.noaa.gov\";\n" +
"  :Easternmost_Easting = -122.21f; // float\n" +
"  :featureType = \"TimeSeries\";\n" +
"  :geospatial_lat_max = 37.997f; // float\n" +
"  :geospatial_lat_min = 37.363f; // float\n" +
"  :geospatial_lat_units = \"degrees_north\";\n" +
"  :geospatial_lon_max = -122.21f; // float\n" +
"  :geospatial_lon_min = -122.975f; // float\n" +
"  :geospatial_lon_units = \"degrees_east\";\n" +
"  :geospatial_vertical_positive = \"down\";\n" +
"  :geospatial_vertical_units = \"m\";\n" +
"  :history = \"NOAA NDBC\n" +
today;
        String tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
        
//      + " http://www.ndbc.noaa.gov/\n" +
//today + " http://127.0.0.1:8080/cwexperimental/
expected = 
"tabledap/cwwcNDBCMet.ncCFMA\\?longitude,latitude,station,time,atmp,wtmp&longitude>-123&longitude<-122&latitude>37&latitude<38&time>=2005-05-01T00&time<=2005-05-01T03\";\n" +
"  :id = \"ncCFMA1a\";\n" +
"  :infoUrl = \"http://www.ndbc.noaa.gov/\";\n" +
"  :institution = \"NOAA NDBC, NOAA NMFS SWFSC ERD\";\n" +
"  :keywords = \"Atmosphere > Air Quality > Visibility,\n" +
"Atmosphere > Altitude > Planetary Boundary Layer Height,\n" +
"Atmosphere > Atmospheric Pressure > Atmospheric Pressure Measurements,\n" +
"Atmosphere > Atmospheric Pressure > Pressure Tendency,\n" +
"Atmosphere > Atmospheric Pressure > Sea Level Pressure,\n" +
"Atmosphere > Atmospheric Pressure > Static Pressure,\n" +
"Atmosphere > Atmospheric Temperature > Air Temperature,\n" +
"Atmosphere > Atmospheric Temperature > Dew Point Temperature,\n" +
"Atmosphere > Atmospheric Water Vapor > Dew Point Temperature,\n" +
"Atmosphere > Atmospheric Winds > Surface Winds,\n" +
"Oceans > Ocean Temperature > Sea Surface Temperature,\n" +
"Oceans > Ocean Waves > Significant Wave Height,\n" +
"Oceans > Ocean Waves > Swells,\n" +
"Oceans > Ocean Waves > Wave Period,\n" +
"air, air_pressure_at_sea_level, air_temperature, atmosphere, atmospheric, average, boundary, buoy, coastwatch, data, dew point, dew_point_temperature, direction, dominant, eastward, eastward_wind, from, gust, height, identifier, layer, level, measurements, meridional, meteorological, meteorology, name, ndbc, noaa, northward, northward_wind, ocean, oceans, period, planetary, pressure, quality, sea, sea level, sea_surface_swell_wave_period, sea_surface_swell_wave_significant_height, sea_surface_swell_wave_to_direction, sea_surface_temperature, seawater, significant, speed, sst, standard, static, station, surface, surface waves, surface_altitude, swell, swells, temperature, tendency, tendency_of_air_pressure, time, vapor, visibility, visibility_in_air, water, wave, waves, wcn, wind, wind_from_direction, wind_speed, wind_speed_of_gust, winds, zonal\";\n" +
"  :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
"  :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"  :naming_authority = \"gov.noaa.pfeg.coastwatch\";\n" +
"  :NDBCMeasurementDescriptionUrl = \"http://www.ndbc.noaa.gov/measdes.shtml\";\n" +
"  :Northernmost_Northing = 37.997f; // float\n" +
"  :project = \"NOAA NDBC and NOAA CoastWatch \\(West Coast Node\\)\";\n" +
"  :publisher_email = \"erd.data@noaa.gov\";\n" +
"  :publisher_name = \"NOAA NMFS SWFSC ERD\";\n" +
"  :publisher_url = \"http://www.pfeg.noaa.gov\";\n" +
"  :quality = \"Automated QC checks with periodic manual QC\";\n" +
"  :source = \"station observation\";\n" +
"  :sourceUrl = \"http://www.ndbc.noaa.gov/\";\n" +
"  :Southernmost_Northing = 37.363f; // float\n" +
"  :standard_name_vocabulary = \"CF-12\";\n" +
"  :subsetVariables = \"station, longitude, latitude\";\n" +
"  :summary = \"The National Data Buoy Center \\(NDBC\\) distributes meteorological data from\n" +
"moored buoys maintained by NDBC and others. Moored buoys are the weather\n" +
"sentinels of the sea. They are deployed in the coastal and offshore waters\n" +
"from the western Atlantic to the Pacific Ocean around Hawaii, and from the\n" +
"Bering Sea to the South Pacific. NDBC's moored buoys measure and transmit\n" +
"barometric pressure; wind direction, speed, and gust; air and sea\n" +
"temperature; and wave energy spectra from which significant wave height,\n" +
"dominant wave period, and average wave period are derived. Even the\n" +
"direction of wave propagation is measured on many moored buoys.\n" +
"\n" +
"The data is from NOAA NDBC. It has been reformatted by NOAA Coastwatch,\n" +
"West Coast Node. This dataset only has the data that is closest to a\n" +
"given hour. The time values in the dataset are rounded to the nearest hour.\n" +
"\n" +  //dates below change
"This dataset has both historical data \\(quality controlled, before\n" +
"20.{8}T00:00:00Z\\) and near real time data \\(less quality controlled, from\n" +
"20.{8}T00:00:00Z on\\).\";\n" +
"  :time_coverage_end = \"2005-05-01T03:00:00Z\";\n" +
"  :time_coverage_resolution = \"P1H\";\n" +
"  :time_coverage_start = \"2005-05-01T00:00:00Z\";\n" +
"  :title = \"NDBC Standard Meteorological Buoy Data\";\n" +
"  :Westernmost_Easting = -122.975f; // float\n" +
" data:\n" +
"longitude =\n" +
"  \\{-122.881, -122.833, -122.298, -122.465, -122.975, -122.4, -122.21\\}\n" +
"latitude =\n" +
"  \\{37.363, 37.759, 37.772, 37.807, 37.997, 37.928, 37.507\\}\n" +
"station =\"46012\", \"46026\", \"AAMC1\", \"FTPC1\", \"PRYC1\", \"RCMC1\", \"RTYC1\"\n" +
"time =\n" +
"  \\{\n" +
"    \\{1.1149056E9, 1.1149092E9, 1.1149128E9, 1.1149164E9\\},\n" +
"    \\{1.1149056E9, 1.1149092E9, 1.1149128E9, 1.1149164E9\\},\n" +
"    \\{1.1149056E9, 1.1149092E9, 1.1149128E9, 1.1149164E9\\},\n" +
"    \\{1.1149056E9, 1.1149092E9, 1.1149128E9, 1.1149164E9\\},\n" +
"    \\{1.1149056E9, 1.1149092E9, 1.1149128E9, 1.1149164E9\\},\n" +
"    \\{1.1149056E9, 1.1149092E9, 1.1149128E9, 1.1149164E9\\},\n" +
"    \\{1.1149056E9, 1.1149092E9, 1.1149128E9, 1.1149164E9\\}\n" +
"  \\}\n" +
"atmp =\n" +
"  \\{\n" +
"    \\{13.3, 13.3, 13.3, 13.3\\},\n" +
"    \\{-9999999.0, 13.3, 13.3, 13.2\\},\n" +
"    \\{14.9, 14.5, 14.7, 14.2\\},\n" +
"    \\{13.8, 13.9, 13.6, 13.3\\},\n" +
"    \\{-9999999.0, -9999999.0, -9999999.0, -9999999.0\\},\n" +
"    \\{15.5, 15.1, 14.5, 13.6\\},\n" +
"    \\{14.7, 14.7, 15.0, 14.3\\}\n" +
"  \\}\n" +
"wtmp =\n" +
"  \\{\n" +
"    \\{13.3, 13.3, 13.4, 13.3\\},\n" +
"    \\{-9999999.0, 13.4, 13.3, 13.2\\},\n" +
"    \\{17.1, 16.6, 15.6, 15.2\\},\n" +
"    \\{-9999999.0, -9999999.0, -9999999.0, -9999999.0\\},\n" +
"    \\{9.5, 9.3, -9999999.0, 9.6\\},\n" +
"    \\{14.7, 14.7, 14.5, 14.4\\},\n" +
"    \\{16.6, 16.6, 16.6, 16.5\\}\n" +
"  \\}\n" +
"\\}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureLinesMatch(results.substring(tPo), expected, "results=\n" + results);

        String2.log("\n*** EDDTableFromNcFiles.testNcCFMA1a finished.");
    }


    /**
     * Test making an .ncCF TimeSeries file (notably, different number obs per feature).
     */
    public static void testNcCF1b() throws Throwable {

        String2.log("\n*** EDDTableFromNcFiles.testNcCF1b");
        EDDTable tedd = (EDDTable)oneFromDatasetsXml(null, "erdFedRockfishStation"); 
        String tName, error, results, expected;
        int po;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

        //lon lat time range 
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "cruise,time,longitude,latitude,bucket_temperature&cruise=~%22(0002|0103)%22", 
            EDStatic.fullTestCacheDirectory, "ncCF1b", ".ncCF"); 
        results = NcHelper.dumpString(EDStatic.fullTestCacheDirectory + tName, true);
        //String2.log(results);
        expected = 
"netcdf ncCF1b.nc {\n" +
"  dimensions:\n" +
"    trajectory = 2;\n" +
"    obs = 523;\n" +
"    cruise_strlen = 4;\n" +
"  variables:\n" +
"    char cruise(trajectory=2, cruise_strlen=4);\n" +
"      :cf_role = \"trajectory_id\";\n" +
"      :comment = \"The first two digits are the last two digits of the year and the last two are the consecutive cruise number for that particular vessel (01, 02, 03, ...).\";\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"Cruise\";\n" +
"\n" +
"    int rowSize(trajectory=2);\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"Number of Observations for this Trajectory\";\n" +
"      :sample_dimension = \"obs\";\n" +
"\n" +
"    double time(obs=523);\n" +
"      :_CoordinateAxisType = \"Time\";\n" +
"      :actual_range = 9.5810382E8, 9.9199518E8; // double\n" +
"      :axis = \"T\";\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Time\";\n" +
"      :standard_name = \"time\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    float longitude(obs=523);\n" +
"      :_CoordinateAxisType = \"Lon\";\n" +
"      :actual_range = -124.6335f, -121.854f; // float\n" +
"      :axis = \"X\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Longitude\";\n" +
"      :standard_name = \"longitude\";\n" +
"      :units = \"degrees_east\";\n" +
"\n" +
"    float latitude(obs=523);\n" +
"      :_CoordinateAxisType = \"Lat\";\n" +
"      :actual_range = 36.5567f, 38.8782f; // float\n" +
"      :axis = \"Y\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Latitude\";\n" +
"      :standard_name = \"latitude\";\n" +
"      :units = \"degrees_north\";\n" +
"\n" +
"    float bucket_temperature(obs=523);\n" +
"      :actual_range = 9.1f, 15.5f; // float\n" +
"      :colorBarMaximum = 32.0; // double\n" +
"      :colorBarMinimum = 0.0; // double\n" +
"      :coordinates = \"time latitude longitude\";\n" +
"      :ioos_category = \"Temperature\";\n" +
"      :long_name = \"Bucket Temperature\";\n" +
"      :standard_name = \"sea_water_temperature\";\n" +
"      :units = \"degree_C\";\n" +
"\n" +
"  // global attributes:\n" +
"  :cdm_data_type = \"Trajectory\";\n" +
"  :cdm_trajectory_variables = \"cruise\";\n" +
"  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"  :creator_email = \"Keith.Sakuma@noaa.gov\";\n" +
"  :creator_name = \"Keith Sakuma\";\n" +
"  :Easternmost_Easting = -121.854f; // float\n" +
"  :featureType = \"Trajectory\";\n" +
"  :geospatial_lat_max = 38.8782f; // float\n" +
"  :geospatial_lat_min = 36.5567f; // float\n" +
"  :geospatial_lat_units = \"degrees_north\";\n" +
"  :geospatial_lon_max = -121.854f; // float\n" +
"  :geospatial_lon_min = -124.6335f; // float\n" +
"  :geospatial_lon_units = \"degrees_east\";\n" +
"  :history = \"2013-04-08 source files from Keith Sakuma (FED) to Lynn Dewitt (ERD) to Bob Simons (ERD)\n" +
"2013-04-09 Bob Simons (bob.simons@noaa.gov) converted files from .csv to .nc\n" +
today;
        String tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
        
expected =
"  :id = \"ncCF1b\";\n" +
"  :infoUrl = \"http://swfsc.noaa.gov/GroundfishAnalysis/\";\n" +
"  :institution = \"NOAA SWFSC FED\";\n" +
"  :keywords = \"bottom, bucket, California, coast, cruise, ctd, data, depth, FED, fisheries, fixed, fluorometer, header, hydrographic, index, juvenile, latitude, longitude, midwater, NMFS, NOAA, pacific, rockfish, salinity, species, station, survey, SWFSC, temperature, thermosalinometer, time, transimissometer, trawl\";\n" +
"  :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"  :Northernmost_Northing = 38.8782f; // float\n" +
"  :sourceUrl = \"(local files; contact erd.data@noaa.gov)\";\n" +
"  :Southernmost_Northing = 36.5567f; // float\n" +
"  :standard_name_vocabulary = \"CF Standard Name Table v29\";\n" +
"  :subsetVariables = \"cruise,ctd_index,ctd_no,station,time,longitude,latitude\";\n" +
"  :summary = \"SWFSC FED Mid Water Trawl Juvenile Rockfish Survey: Station Information and Surface Data.\n" +
"Surveys have been conducted along the central California coast in May/June \n" +
"every year since 1983. In 2004 the survey area was expanded to cover the \n" +
"entire coast from San Diego to Cape Mendocino.  The survey samples a series \n" +
"of fixed trawl stations using a midwater trawl. The midwater trawl survey \n" +
"gear captures significant numbers of approximately 10 rockfish species during\n" +
"their pelagic juvenile stage (i.e., 50-150 days old), by which time annual\n" +
"reproductive success has been established. Catch-per-unit-effort data from\n" +
"the survey are analyzed and serve as the basis for predicting future \n" +
"recruitment to rockfish fisheries. Results for several species (e.g., \n" +
"bocaccio, chilipepper [S. goodei], and widow rockfish [S. entomelas]) have\n" +
"shown that the survey data can be useful in predicting year-class strength\n" +
"in age-based stock assessments.\n" +
"\n" +
"The survey's data on YOY Pacific whiting has also been used in the stock\n" +
"assessment process. To assist in obtaining additional northward spatial\n" +
"coverage of YOY Pacific whiting off Oregon and Washington, in 2001 the\n" +
"Pacific Whiting Conservation Cooperative in cooperation with the NOAA NMFS\n" +
"Northwest Fisheries Science Center began a midwater trawl survey patterned\n" +
"after the NOAA NMFS SWFSC Fisheries Ecology Division's (FED) existing survey. \n" +
"Both surveys work cooperatively together each year in order to resolve \n" +
"interannual abundance patterns of YOY rockfish and Pacific whiting on a \n" +
"coastwide basis, which provides expedient, critical information that can be \n" +
"used in the fisheries management process.\n" +
"\n" +
"The large quantity of physical data collected during the surveys (e.g., CTD\n" +
"with attached transimissometer and fluorometer, thermosalinometer, and ADCP)\n" +
"have provided a better understanding of the hydrographic conditions off the\n" +
"California coast and analysis of these data have been distributed through the\n" +
"publication of NOAA NMFS Technical Memoranda.\n" +
"\n" +
"For more information, see http://swfsc.noaa.gov/GroundfishAnalysis/ and\n" +
"http://www.sanctuarysimon.org/projects/project_info.php?projectID=100118\";\n" +
"  :time_coverage_end = \"2001-06-08T10:13:00Z\";\n" +
"  :time_coverage_start = \"2000-05-12T03:57:00Z\";\n" +
"  :title = \"SWFSC FED Mid Water Trawl Juvenile Rockfish Survey, Surface Data\";\n" +
"  :Westernmost_Easting = -124.6335f; // float\n" +
" data:\n" +
"cruise =\"0002\", \"0103\"\n" +
"rowSize =\n" +
"  {250, 273}\n" +
"time =\n" +
"  {9.5810382E8, 9.5810652E8, 9.5811444E8, 9.581178E8, 9.5812548E8, 9.58128E8, 9.581349E8, 9.5814078E8, 9.581462" +
"4E8, 9.5815128E8, 9.5815656E8, 9.5816232E8, 9.5816772E8, 9.5817312E8, 9.581784E8, 9.5818404E8, 9.581892E8, 9.58" +
"1928E8, 9.5820828E8, 9.5821176E8, 9.5822364E8, 9.582279E8, 9.5823264E8, 9.5823852E8, 9.582444E8, 9.5825022E8, 9" +
".5825532E8, 9.5826018E8, 9.582657E8, 9.5827068E8, 9.5827944E8, 9.5828862E8, 9.582906E8, 9.583074E8, 9.5831346E8" +
", 9.5831862E8, 9.583233E8, 9.583293E8, 9.5833446E8, 9.583392E8, 9.5834442E8, 9.5834928E8, 9.583536E8, 9.5836542" +
"E8, 9.5837448E8, 9.583788E8, 9.583932E8, 9.5840292E8, 9.584082E8, 9.5841432E8, 9.5842032E8, 9.5842524E8, 9.5842" +
"986E8, 9.5843568E8, 9.5844126E8, 9.584517E8, 9.584604E8, 9.5846454E8, 9.5847534E8, 9.584814E8, 9.5848698E8, 9.5" +
"84919E8, 9.5849766E8, 9.5850462E8, 9.5851146E8, 9.5851752E8, 9.5852418E8, 9.585291E8, 9.5853804E8, 9.5855208E8," +
" 9.5855478E8, 9.5856072E8, 9.585741E8, 9.5857968E8, 9.5862468E8, 9.5863734E8, 9.5871054E8, 9.5872044E8, 9.58722" +
"96E8, 9.5872968E8, 9.5873136E8, 9.587391E8, 9.587436E8, 9.5874924E8, 9.5875512E8, 9.5876082E8, 9.5876586E8, 9.5" +
"87718E8, 9.5877876E8, 9.5878512E8, 9.5879142E8, 9.5879706E8, 9.5880948E8, 9.588123E8, 9.58821E8, 9.5882808E8, 9" +
".588336E8, 9.588387E8, 9.5884476E8, 9.5885076E8, 9.588567E8, 9.588621E8, 9.5886696E8, 9.5887254E8, 9.5887698E8," +
" 9.5888226E8, 9.5889264E8, 9.5889516E8, 9.5890968E8, 9.5891736E8, 9.5892216E8, 9.589269E8, 9.5893272E8, 9.58937" +
"58E8, 9.5894208E8, 9.5894772E8, 9.5895198E8, 9.5895768E8, 9.5896992E8, 9.5897928E8, 9.5898348E8, 9.589983E8, 9." +
"59007E8, 9.5901216E8, 9.5901888E8, 9.5902548E8, 9.5903496E8, 9.5904306E8, 9.59049E8, 9.5905548E8, 9.5906838E8, " +
"9.5907216E8, 9.5908914E8, 9.5910342E8, 9.5914278E8, 9.591747E8, 9.5921568E8, 9.5925504E8, 9.593121E8, 9.593166E" +
"8, 9.5932494E8, 9.5932806E8, 9.5933724E8, 9.593403E8, 9.5934798E8, 9.595827E8, 9.5958966E8, 9.59592E8, 9.596554" +
"8E8, 9.5966238E8, 9.5974764E8, 9.5975562E8, 9.5975856E8, 9.5976948E8, 9.5977182E8, 9.5977812E8, 9.597828E8, 9.5" +
"978844E8, 9.597936E8, 9.597996E8, 9.5980722E8, 9.598143E8, 9.5981922E8, 9.5982486E8, 9.5983104E8, 9.5983542E8, " +
"9.598518E8, 9.598662E8, 9.598701E8, 9.5987502E8, 9.5988144E8, 9.5988756E8, 9.5989326E8, 9.598986E8, 9.59904E8, " +
"9.599097E8, 9.5991444E8, 9.59922E8, 9.5992944E8, 9.5993118E8, 9.5995062E8, 9.5995758E8, 9.5996232E8, 9.5996736E" +
"8, 9.5997324E8, 9.5997786E8, 9.5998278E8, 9.5998842E8, 9.599934E8, 9.600069E8, 9.600183E8, 9.6002196E8, 9.60037" +
"2E8, 9.6004446E8, 9.600495E8, 9.600555E8, 9.600612E8, 9.6006618E8, 9.6007122E8, 9.6007668E8, 9.6008232E8, 9.600" +
"936E8, 9.6010272E8, 9.6010728E8, 9.601158E8, 9.601221E8, 9.601278E8, 9.6013218E8, 9.6013758E8, 9.6014442E8, 9.6" +
"015084E8, 9.6015612E8, 9.601614E8, 9.601662E8, 9.6018E8, 9.601947E8, 9.601974E8, 9.6020892E8, 9.6021384E8, 9.60" +
"27684E8, 9.6028956E8, 9.6031308E8, 9.6032172E8, 9.603291E8, 9.603642E8, 9.6039156E8, 9.604035E8, 9.6042414E8, 9" +
".6043758E8, 9.6044334E8, 9.6045168E8, 9.604587E8, 9.6046824E8, 9.604749E8, 9.6048732E8, 9.60498E8, 9.605127E8, " +
"9.6052104E8, 9.6052794E8, 9.6053244E8, 9.6055512E8, 9.605712E8, 9.6057462E8, 9.6057876E8, 9.6058284E8, 9.605872" +
"2E8, 9.6059022E8, 9.6059232E8, 9.6059418E8, 9.6059664E8, 9.896394E8, 9.896415E8, 9.896562E8, 9.8965908E8, 9.896" +
"676E8, 9.8967702E8, 9.8968158E8, 9.8968626E8, 9.896913E8, 9.8969604E8, 9.897015E8, 9.8970702E8, 9.8971176E8, 9." +
"89715E8, 9.897168E8, 9.8972808E8, 9.8974602E8, 9.897489E8, 9.8976126E8, 9.897657E8, 9.8977026E8, 9.8977572E8, 9" +
".89781E8, 9.8978628E8, 9.8979138E8, 9.8979624E8, 9.8980176E8, 9.8980632E8, 9.898137E8, 9.898227E8, 9.898251E8, " +
"9.898353E8, 9.898392E8, 9.8984694E8, 9.898569E8, 9.898614E8, 9.898659E8, 9.8987136E8, 9.8987616E8, 9.8987964E8," +
" 9.8988132E8, 9.8989134E8, 9.898956E8, 9.898992E8, 9.899088E8, 9.899127E8, 9.899211E8, 9.899244E8, 9.8993388E8," +
" 9.8993856E8, 9.8994366E8, 9.899493E8, 9.89955E8, 9.8995968E8, 9.8996424E8, 9.899703E8, 9.8997588E8, 9.899862E8" +
", 9.899964E8, 9.900009E8, 9.900117E8, 9.900159E8, 9.9002424E8, 9.900291E8, 9.9003456E8, 9.9004146E8, 9.9004854E" +
"8, 9.9005436E8, 9.9006E8, 9.9006486E8, 9.90072E8, 9.900837E8, 9.900867E8, 9.9009528E8, 9.901023E8, 9.9010824E8," +
" 9.901593E8, 9.901698E8, 9.901794E8, 9.902469E8, 9.902562E8, 9.902589E8, 9.902661E8, 9.902682E8, 9.902772E8, 9." +
"9028962E8, 9.9029466E8, 9.9029988E8, 9.9030474E8, 9.9030996E8, 9.9031596E8, 9.9032082E8, 9.9032634E8, 9.9033342" +
"E8, 9.9034974E8, 9.9035304E8, 9.9036408E8, 9.903678E8, 9.9037236E8, 9.9037818E8, 9.9038376E8, 9.903897E8, 9.903" +
"9486E8, 9.903996E8, 9.9040524E8, 9.9041022E8, 9.9041964E8, 9.9043026E8, 9.9043212E8, 9.9044046E8, 9.9044514E8, " +
"9.9045594E8, 9.9046056E8, 9.9046506E8, 9.9047154E8, 9.904758E8, 9.9048042E8, 9.90486E8, 9.904905E8, 9.904947E8," +
" 9.9050604E8, 9.9051576E8, 9.9051936E8, 9.9052824E8, 9.9053124E8, 9.9053838E8, 9.9054204E8, 9.9054684E8, 9.9055" +
"344E8, 9.9055884E8, 9.9056352E8, 9.905679E8, 9.905739E8, 9.9057954E8, 9.905925E8, 9.9060282E8, 9.906069E8, 9.90" +
"61734E8, 9.906234E8, 9.9062808E8, 9.9063336E8, 9.906387E8, 9.9064518E8, 9.906516E8, 9.9065694E8, 9.9066288E8, 9" +
".9066804E8, 9.906792E8, 9.906948E8, 9.9069762E8, 9.9070482E8, 9.9070998E8, 9.9071406E8, 9.9071928E8, 9.9076548E" +
"8, 9.9077514E8, 9.9077898E8, 9.9079908E8, 9.9080478E8, 9.908112E8, 9.9081738E8, 9.9082386E8, 9.9083046E8, 9.908" +
"361E8, 9.908418E8, 9.9084768E8, 9.9085314E8, 9.9086388E8, 9.9086958E8, 9.908814E8, 9.9088746E8, 9.9089442E8, 9." +
"909003E8, 9.9090642E8, 9.9091164E8, 9.9091686E8, 9.909222E8, 9.9092838E8, 9.9093456E8, 9.9094992E8, 9.9095598E8" +
", 9.90966E8, 9.9097104E8, 9.9097692E8, 9.9098262E8, 9.909912E8, 9.9099738E8, 9.9127836E8, 9.912849E8, 9.9129342" +
"E8, 9.9129642E8, 9.9130326E8, 9.9130554E8, 9.9131472E8, 9.913197E8, 9.9132486E8, 9.913296E8, 9.9133512E8, 9.913" +
"3998E8, 9.9134454E8, 9.913494E8, 9.9135702E8, 9.913623E8, 9.913677E8, 9.91371E8, 9.913857E8, 9.9138978E8, 9.914" +
"0214E8, 9.914061E8, 9.9141084E8, 9.9141624E8, 9.9142146E8, 9.9142722E8, 9.9143238E8, 9.914373E8, 9.914433E8, 9." +
"914481E8, 9.91458E8, 9.914655E8, 9.914685E8, 9.9147786E8, 9.914862E8, 9.9149634E8, 9.9150156E8, 9.9150732E8, 9." +
"9151452E8, 9.9152418E8, 9.9153606E8, 9.9154488E8, 9.915525E8, 9.915564E8, 9.916725E8, 9.916776E8, 9.916827E8, 9" +
".9168786E8, 9.9169596E8, 9.91701E8, 9.91707E8, 9.917127E8, 9.9171726E8, 9.9172824E8, 9.917337E8, 9.917448E8, 9." +
"9175254E8, 9.9175794E8, 9.91764E8, 9.9176946E8, 9.9177426E8, 9.9177924E8, 9.917847E8, 9.9179016E8, 9.9180414E8," +
" 9.918135E8, 9.9181848E8, 9.918342E8, 9.9183978E8, 9.9184446E8, 9.9189126E8, 9.9190008E8, 9.9190422E8, 9.919140" +
"6E8, 9.9191994E8, 9.919251E8, 9.919302E8, 9.9193548E8, 9.9194106E8, 9.9194808E8, 9.9195492E8, 9.9196068E8, 9.91" +
"9665E8, 9.9198552E8, 9.9199188E8, 9.9199518E8}\n" +
"longitude =\n" +
"  {-121.9383, -121.983, -121.8678, -121.978, -121.8947, -121.8622, -121.9445, -122.165, -122.2658, -122.372, -1" +
"22.4718, -122.5868, -122.6765, -122.7827, -122.6795, -122.4735, -122.2703, -122.1743, -122.0333, -122.0493, -12" +
"2.1712, -122.1672, -122.3698, -122.576, -122.7827, -122.9822, -122.8845, -122.7843, -122.5792, -122.3702, -122." +
"2922, -122.4007, -122.4288, -122.602, -122.4748, -122.6752, -122.887, -123.0893, -123.19, -123.0853, -122.8807," +
" -122.6773, -122.4762, -122.5677, -122.6557, -122.8165, -122.9778, -122.988, -123.1957, -123.4012, -123.589, -1" +
"23.6052, -123.6057, -123.4007, -123.1935, -123.0453, -123.1905, -123.14, -123.3443, -123.4868, -123.5028, -123." +
"7077, -123.9117, -124.1147, -123.914, -123.7028, -123.503, -123.2983, -123.3922, -123.2657, -123.1785, -123.083" +
"2, -123.294, -123.0938, -122.87, -122.8973, -121.9863, -121.8953, -121.9795, -121.8875, -121.861, -121.9867, -1" +
"22.1645, -122.2668, -122.3707, -122.4753, -122.5802, -122.6745, -122.6843, -122.4757, -122.2707, -122.1758, -12" +
"2.0458, -122.05, -122.1108, -122.1307, -122.1677, -122.3698, -122.5767, -122.7825, -122.99, -122.8838, -122.785" +
"5, -122.5772, -122.3728, -122.2993, -122.3583, -122.4263, -122.5778, -122.4738, -122.6782, -122.8847, -123.0892" +
", -123.1888, -123.0898, -122.8842, -122.7025, -122.4732, -122.5672, -122.6413, -122.8162, -122.9707, -122.9878," +
" -123.1925, -123.3987, -123.6032, -123.605, -123.4057, -123.1952, -123.0448, -123.217, -123.151, -123.478, -123" +
".9125, -122.5752, -123.0175, -123.5002, -122.8497, -123.0917, -123.0008, -123.0675, -123.1683, -123.2533, -123." +
"3672, -123.5777, -122.865, -122.8885, -122.8492, -121.9338, -121.9888, -121.9952, -121.8622, -121.978, -121.888" +
"5, -121.8595, -121.9693, -122.1527, -122.2703, -122.3707, -122.4725, -122.574, -122.7818, -122.6802, -122.4757," +
" -122.2742, -122.1773, -122.0398, -122.1692, -122.1678, -122.3703, -122.5735, -122.7852, -122.9817, -122.8832, " +
"-122.7847, -122.5795, -122.3728, -122.2923, -122.3835, -122.4223, -122.5922, -122.4752, -122.6767, -122.8852, -" +
"123.0895, -123.1907, -123.0882, -122.8853, -122.6787, -122.5675, -122.6608, -122.8157, -122.9975, -122.9915, -1" +
"23.1962, -123.4022, -123.6065, -123.6048, -123.6067, -123.4015, -123.1925, -123.039, -123.2132, -123.1407, -123" +
".3305, -123.486, -123.5062, -123.7095, -123.9142, -124.1162, -123.9128, -123.7062, -123.504, -123.2943, -123.37" +
"3, -123.2622, -123.167, -123.3012, -123.0875, -122.8728, -122.8223, -122.9352, -123.0797, -123.1307, -123.5353," +
" -123.555, -123.5022, -123.0465, -123.5415, -123.5448, -123.4182, -123.5035, -123.407, -123.2483, -123.0523, -1" +
"23.2832, -123.5358, -123.4152, -123.5, -123.4767, -123.278, -123.9938, -123.9098, -123.821, -123.7398, -123.659" +
"2, -123.6073, -123.5595, -123.515, -123.4487, -121.935, -121.9837, -121.8783, -121.9773, -121.9295, -121.9675, " +
"-122.1682, -122.2683, -122.3718, -122.4732, -122.5798, -122.7845, -122.681, -122.274, -122.4772, -122.1835, -12" +
"2.0435, -122.0428, -122.1655, -122.1667, -122.3745, -122.5795, -122.785, -122.9898, -122.89, -122.7855, -122.57" +
"97, -122.2278, -122.294, -122.3792, -122.4268, -122.6173, -122.7585, -122.7515, -122.472, -122.6803, -122.8853," +
" -123.1043, -123.1953, -122.8838, -123.0907, -122.6807, -122.48, -122.567, -122.6508, -122.815, -123.0032, -123" +
".0265, -122.884, -122.9882, -123.195, -123.4022, -123.6077, -123.6063, -123.606, -123.3978, -123.1942, -123.040" +
"8, -123.2157, -123.1318, -123.3233, -123.5045, -123.5042, -123.7242, -123.9135, -124.118, -123.9128, -123.7062," +
" -123.5037, -123.2938, -123.2952, -123.1697, -123.1165, -123.0172, -123.0933, -123.2963, -122.895, -122.9217, -" +
"122.8875, -121.9975, -121.8617, -121.9662, -121.9013, -121.939, -121.854, -122.172, -122.2683, -122.3733, -122." +
"4725, -122.5828, -122.7793, -122.6798, -122.4717, -122.1695, -122.025, -122.0505, -122.162, -122.1665, -122.369" +
"2, -122.5718, -122.78, -122.9942, -122.8835, -122.7828, -122.5775, -122.3755, -122.2805, -122.3933, -122.4087, " +
"-122.6017, -122.7418, -122.4722, -122.6818, -122.8838, -123.0885, -123.1875, -123.0882, -122.8838, -122.6778, -" +
"122.4737, -122.575, -122.6643, -122.8052, -123.0057, -123.0277, -123.028, -122.9965, -123.1975, -123.4015, -123" +
".6075, -123.6053, -123.604, -123.4007, -123.1945, -123.0337, -123.2143, -123.1398, -123.3497, -123.5195, -123.5" +
"04, -123.7067, -123.9122, -124.1182, -123.9112, -123.7072, -123.5017, -123.2992, -123.3752, -123.2623, -123.182" +
"2, -123.0678, -123.006, -123.091, -123.2953, -122.846, -122.9152, -122.879, -123.4812, -123.579, -123.6772, -12" +
"3.7663, -123.8853, -123.9257, -123.988, -124.0528, -124.1277, -124.2243, -124.2975, -124.3948, -124.2242, -124." +
"0617, -123.8733, -123.7113, -123.5368, -123.356, -123.5675, -123.7787, -123.9937, -124.2048, -124.4, -124.6335," +
" -124.427, -124.2418, -124.0467, -123.8775, -123.6748, -123.4848, -122.9317, -121.9948, -121.8585, -121.9563, -" +
"121.8952, -121.878, -121.9432, -122.1668, -122.2668, -122.3738, -122.4737, -122.5777, -122.6775, -122.7813, -12" +
"2.6748, -122.4732, -122.272, -122.1878, -122.0237, -122.038, -122.1575, -122.1555, -122.3737, -122.5758, -122.7" +
"843, -122.9858, -122.8855, -122.7838, -122.5775, -122.3718, -122.3078, -122.372, -122.433, -122.5848, -122.7522" +
", -122.4738, -122.6763, -122.8827, -123.087, -123.0877, -122.8822, -122.6765, -122.4728, -122.5847, -122.4728, " +
"-122.6772, -122.8783, -123.0867, -123.194, -123.09, -122.8865, -122.6798, -122.6605, -122.808, -122.9938, -123." +
"0082, -122.9867, -123.1928, -123.4003, -123.6047, -123.6043, -123.605, -123.4015, -123.1943, -123.06, -123.1995" +
", -123.1495, -123.3497, -123.2977, -123.0918, -123.02, -123.0633, -123.175, -123.2713, -123.352, -123.2973, -12" +
"3.5008, -123.7057, -123.9122, -124.1138, -123.9128, -123.709, -123.5042, -122.8775, -122.9085, -122.843}\n" +
"latitude =\n" +
"  {36.8817, 36.8435, 36.7667, 36.7393, 36.7065, 36.644, 36.6793, 36.668, 36.7722, 36.6667, 36.7713, 36.6627, 36" +
".772, 36.669, 36.5625, 36.5625, 36.563, 36.584, 36.5778, 36.6458, 36.7925, 36.8727, 36.8805, 36.8783, 36.8783, " +
"36.8748, 36.9762, 37.0815, 37.082, 37.0797, 36.9813, 36.9822, 36.9827, 37.014, 37.1702, 37.1777, 37.1735, 37.18" +
"07, 37.2717, 37.3698, 37.3707, 37.3713, 37.3717, 37.2727, 37.2963, 37.2712, 37.291, 37.5142, 37.513, 37.513, 37" +
".5133, 37.6382, 37.7675, 37.7682, 37.7693, 37.6605, 37.671, 37.7405, 37.873, 37.8625, 38.0275, 38.0293, 38.0282" +
", 38.1673, 38.3095, 38.3077, 38.3085, 38.3082, 38.1842, 38.1542, 38.1793, 38.1622, 38.026, 38.0265, 37.7957, 37" +
".6812, 36.845, 36.7605, 36.7428, 36.685, 36.6423, 36.654, 36.6698, 36.7718, 36.6678, 36.7723, 36.6675, 36.7705," +
" 36.5618, 36.5635, 36.5592, 36.5845, 36.5932, 36.6468, 36.6918, 36.7428, 36.8765, 36.8772, 36.877, 36.8773, 36." +
"8778, 36.9835, 37.0838, 37.0845, 37.0835, 36.9902, 36.9677, 36.9862, 36.9652, 37.1795, 37.1793, 37.1793, 37.178" +
"5, 37.2765, 37.372, 37.3727, 37.369, 37.3717, 37.2748, 37.2532, 37.2778, 37.2448, 37.5135, 37.512, 37.5105, 37." +
"5118, 37.6397, 37.7702, 37.768, 37.6587, 37.6768, 37.7363, 37.8702, 38.0265, 37.2858, 37.2407, 38.3072, 37.7687" +
", 38.0268, 38.166, 38.1422, 38.1685, 38.146, 38.1675, 38.166, 37.7893, 37.6617, 37.6132, 36.8827, 36.8553, 36.8" +
"523, 36.7605, 36.7432, 36.699, 36.6422, 36.671, 36.6672, 36.7747, 36.6662, 36.7725, 36.6663, 36.67, 36.5642, 36" +
".5567, 36.5605, 36.5828, 36.5728, 36.781, 36.8768, 36.8785, 36.8768, 36.877, 36.876, 36.9863, 37.0832, 37.0778," +
" 37.0835, 36.9845, 36.9863, 36.983, 36.9812, 37.1793, 37.1807, 37.179, 37.1807, 37.2772, 37.3725, 37.359, 37.37" +
"25, 37.2762, 37.2865, 37.2782, 37.2897, 37.5112, 37.5145, 37.5128, 37.5147, 37.6398, 37.7695, 37.7717, 37.771, " +
"37.6555, 37.6612, 37.7475, 37.886, 37.8637, 38.0247, 38.0273, 38.0293, 38.1695, 38.312, 38.311, 38.3108, 38.307" +
"7, 38.1657, 38.1712, 38.1682, 38.0267, 38.0282, 37.7862, 37.5865, 37.499, 37.6465, 37.7122, 38.0843, 38.0692, 3" +
"7.9557, 37.985, 38.2377, 38.2072, 38.09, 38.0832, 37.9738, 37.9252, 38.1003, 38.17, 38.2578, 38.1047, 38.0927, " +
"38.0208, 38.1807, 38.2947, 38.3463, 38.3983, 38.4457, 38.4998, 38.543, 38.576, 38.6038, 38.6472, 36.8833, 36.84" +
"55, 36.7733, 36.7415, 36.7025, 36.6543, 36.6658, 36.7705, 36.6667, 36.7703, 36.6658, 36.668, 36.5632, 36.5622, " +
"36.5602, 36.5782, 36.5978, 36.6432, 36.7628, 36.8748, 36.8775, 36.8785, 36.8767, 36.876, 36.9833, 37.0825, 37.0" +
"838, 36.9957, 36.9817, 36.983, 36.9843, 37.0058, 36.986, 36.8898, 37.1792, 37.1808, 37.1787, 37.1797, 37.2737, " +
"37.3707, 37.37, 37.3722, 37.3717, 37.2748, 37.2625, 37.265, 37.2867, 37.3452, 37.4033, 37.5152, 37.5127, 37.514" +
"3, 37.5127, 37.6407, 37.7693, 37.7677, 37.7693, 37.6572, 37.656, 37.7328, 37.8805, 37.8885, 38.0233, 38.0258, 3" +
"8.027, 38.1678, 38.308, 38.3105, 38.3058, 38.31, 38.1683, 38.1663, 38.1862, 38.171, 38.0267, 38.0253, 37.805, 3" +
"7.7008, 37.9318, 36.8495, 36.7617, 36.7387, 36.7135, 36.6593, 36.6427, 36.6693, 36.7695, 36.6702, 36.8053, 36.6" +
"667, 36.6653, 36.5648, 36.5617, 36.5815, 36.59, 36.6458, 36.7795, 36.8777, 36.878, 36.8738, 36.8737, 36.8782, 3" +
"6.9822, 37.0807, 37.0828, 37.0833, 36.9792, 36.994, 36.9765, 36.9773, 36.9822, 37.1772, 37.1777, 37.1778, 37.17" +
"85, 37.2757, 37.3703, 37.3715, 37.3722, 37.3708, 37.2833, 37.2833, 37.2678, 37.2902, 37.3468, 37.4335, 37.5132," +
" 37.517, 37.5162, 37.5128, 37.6413, 37.769, 37.7712, 37.7698, 37.655, 37.6743, 37.7422, 37.8772, 37.8957, 38.02" +
"55, 38.0305, 38.0292, 38.168, 38.3112, 38.3097, 38.3072, 38.3078, 38.176, 38.1612, 38.1783, 38.1502, 38.1668, 3" +
"8.0237, 38.0262, 37.6073, 37.6992, 37.8022, 38.1178, 38.2673, 38.418, 38.57, 38.7202, 38.8782, 38.716, 38.5608," +
" 38.4067, 38.2563, 38.0858, 37.9538, 38.0365, 38.1548, 38.2703, 38.3548, 38.4442, 38.5348, 38.5365, 38.5382, 38" +
".538, 38.5352, 38.5328, 38.5408, 38.459, 38.409, 38.3392, 38.4155, 38.194, 38.1185, 36.8823, 36.8472, 36.766, 3" +
"6.7403, 36.705, 36.6472, 36.6602, 36.6675, 36.7712, 36.6658, 36.7718, 36.6688, 36.7712, 36.6678, 36.5613, 36.56" +
"25, 36.5622, 36.5862, 36.5687, 36.6405, 36.7865, 36.874, 36.8773, 36.877, 36.8775, 36.8765, 36.9822, 37.0822, 3" +
"7.083, 37.0828, 36.9958, 36.9708, 36.9935, 36.968, 36.9753, 37.1785, 37.177, 37.1798, 37.1787, 37.3697, 37.3712" +
", 37.3712, 37.3703, 37.2902, 37.3727, 37.3722, 37.3722, 37.3707, 37.2755, 37.1757, 37.1783, 37.1775, 37.2835, 3" +
"7.2637, 37.2825, 37.3348, 37.5122, 37.5138, 37.5125, 37.5135, 37.6375, 37.7692, 37.7695, 37.7698, 37.671, 37.64" +
"38, 37.7538, 37.8475, 38.0265, 38.0268, 38.18, 38.145, 38.1737, 38.1498, 38.1502, 38.3067, 38.3068, 38.3058, 38" +
".3068, 38.1687, 38.0272, 38.025, 38.0267, 37.8, 37.6967, 37.6037}\n" +
"bucket_temperature =\n" +
"  {12.0, 11.3, 12.0, 11.7, 11.0, 12.6, 11.1, 12.4, 12.8, 13.7, 13.5, 13.5, 12.7, 13.5, 14.2, 14.0, 13.5, 13.2, " +
"11.2, 11.7, 10.8, 10.8, 11.3, 11.9, 14.0, 12.7, 13.8, 14.1, 13.2, 12.5, 11.6, 12.4, 12.3, 11.7, 10.8, 12.5, 12." +
"6, 13.3, 13.3, 13.4, 13.2, 11.6, 12.0, 12.5, 12.2, 13.3, 13.5, 12.2, 13.5, 13.5, 13.5, 13.8, 13.8, 13.5, 13.3, " +
"11.3, 12.5, 12.8, 13.5, 12.8, 13.3, 12.8, 13.8, 14.0, 13.7, 13.7, 13.5, 13.0, 13.5, 12.8, 11.7, 10.5, 12.4, 10." +
"6, 11.6, 11.5, 12.9, 12.5, 12.5, 13.0, 13.9, 12.2, 12.2, 11.9, 13.4, 12.9, 14.4, 14.3, 14.8, 14.9, 14.1, 13.1, " +
"12.3, 12.0, 11.9, 11.8, 12.8, 11.6, 12.2, 13.2, 13.9, 13.2, 12.7, 12.2, 11.2, 10.6, 10.8, 11.5, 11.7, 11.5, 12." +
"0, 12.6, 11.9, 12.5, 12.2, 12.4, 14.2, 13.5, 12.9, 13.2, 11.8, 11.9, 11.4, 11.5, 10.7, 13.3, 13.2, 10.8, 11.8, " +
"11.5, 11.5, 11.3, 10.1, 13.2, 12.4, 12.2, 10.1, 10.9, 10.2, 10.3, 10.0, 9.5, 9.3, 9.5, 10.1, 10.2, 10.2, 10.2, " +
"13.5, 11.9, 10.8, 11.7, 11.2, 12.2, 13.5, 10.7, 10.2, 11.0, 12.2, 11.5, 12.4, 12.9, 12.9, 12.2, 11.5, 10.9, 10." +
"8, 10.8, 10.8, 11.0, 11.5, 12.2, 13.6, 12.5, 12.6, 11.8, 10.5, 10.2, 11.2, 11.1, 11.8, 10.2, 11.2, 11.5, 13.2, " +
"12.3, 12.5, 12.2, 12.5, 12.1, 11.5, 11.0, 12.0, 10.5, 10.4, 11.2, 13.4, 11.8, 11.8, 11.8, 10.3, 11.0, 11.6, 10." +
"5, 10.1, 10.4, 9.9, 13.2, 13.7, 12.9, 14.2, 14.9, 12.2, 11.3, 11.0, 10.1, 10.8, NaN, 11.8, 13.0, 12.8, 11.8, 12" +
".2, 11.3, 11.7, 11.2, 12.5, 10.3, 13.0, 12.3, 11.0, 11.2, 10.5, 11.2, 11.9, 10.3, 11.5, 10.7, 11.2, 10.6, 10.8," +
" 12.7, 13.3, 13.7, 13.6, 12.2, 12.8, 11.8, 12.1, 10.6, 13.8, 13.1, 11.6, 12.0, 12.2, 11.2, 13.2, 12.5, 12.3, 12" +
".8, 13.4, 13.6, 13.9, 12.8, 13.7, 13.7, 11.2, 11.5, 13.4, 12.2, 12.3, 13.2, 12.5, 13.5, 12.1, 12.2, 12.2, 13.2," +
" 12.8, 11.9, 12.4, 12.7, 12.3, 12.4, 11.9, 12.0, 12.7, 13.0, 12.2, 13.4, 12.9, 12.4, 13.3, 13.0, 12.7, 11.8, 12" +
".0, 12.3, 12.1, 11.6, 13.1, 13.5, 13.7, 13.8, 13.6, 13.8, 12.8, 13.2, 13.3, 12.5, 12.9, 13.4, 13.0, 13.0, 11.4," +
" 11.7, 12.5, 11.3, 11.1, 10.6, 10.5, 10.5, 10.4, 10.6, 10.6, 10.0, 11.7, 11.3, 10.2, 12.3, 12.6, 11.4, 11.9, 11" +
".6, 13.2, 12.7, 12.9, 12.6, 13.1, 13.6, 14.4, 14.8, 13.7, 13.0, 12.3, 12.9, 13.1, 12.9, 13.0, 13.4, 13.6, 13.8," +
" 13.2, 13.3, 12.0, 13.0, 12.3, 12.7, 12.7, 13.3, 12.5, 11.4, 12.4, 12.5, 12.3, 12.7, 13.0, 13.2, 12.7, 12.4, 12" +
".9, 12.5, 12.6, 12.7, 12.6, 12.9, 13.1, 13.4, 13.1, 14.2, 14.5, 14.4, 13.2, 13.3, 13.3, 13.5, 13.0, 11.7, 12.3," +
" 10.9, 13.0, 13.7, 13.5, 13.2, 12.2, 11.2, 11.4, 10.4, 11.0, 11.4, 11.8, 11.9, 11.8, 11.1, 11.8, 11.9, 11.5, 10" +
".2, 10.1, 10.5, 10.4, 10.8, 11.2, 10.8, 12.9, 12.9, 12.4, 12.7, 12.8, 13.2, 13.1, 11.0, 10.7, 10.6, 10.5, 10.6," +
" 10.6, 11.5, 12.5, 12.7, 12.3, 12.4, 12.5, 12.5, 10.7, 12.5, 11.0, 14.0, 13.1, 13.3, 12.5, 13.4, 14.1, 14.1, 13" +
".4, 13.3, 15.1, 13.8, 14.8, 14.0, 15.1, 15.3, 14.6, 15.5, 15.5, 14.4, 14.5, 14.2, 13.4, 13.3, 13.7, 14.0, 14.7," +
" 13.5, 13.5, 13.1, 12.7, 11.7, 12.0, 12.0, 12.4, 12.2, 11.8, 12.9, 12.6, 12.5, 12.5, 11.6, 13.0, 12.3, 12.5, 11" +
".5, 11.9, 11.7, 11.3, 12.3, 12.0, 12.2, 12.1, 12.3, 11.9, 12.1, 12.1, 10.5, 10.6, 11.7, 12.2, 12.5, 12.6, 12.9," +
" 10.7, 10.7, 10.6, 10.4, 12.3, 9.7, 9.7, 11.2, 10.0, 9.6, 9.4, 9.6, 9.1, 9.6, 9.8, 10.5, 11.6, 11.3, 12.5, 10.8" +
", 10.8, 10.7, 11.0}\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, tPo + expected.length()),
            expected, "results=\n" + results);

        String2.log("\n*** EDDTableFromNcFiles.testNcCF1b finished.");

    }

    /**
     * Test making an .ncCFMA TimeSeries file (notably, different number obs per feature).
     */
    public static void testNcCFMA1b() throws Throwable {

        String2.log("\n*** EDDTableFromNcFiles.testNcCFMA1b");
        EDDTable tedd = (EDDTable)oneFromDatasetsXml(null, "erdFedRockfishStation"); //should work
        String tName, error, results, expected;
        int po;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

        //lon lat time range 
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "cruise,time,longitude,latitude,bucket_temperature&cruise=~%22(0002|0103)%22", 
            EDStatic.fullTestCacheDirectory, "ncCFMA1b", ".ncCFMA"); 
        results = NcHelper.dumpString(EDStatic.fullTestCacheDirectory + tName, true);
        //String2.log(results);
        expected = 
"netcdf ncCFMA1b.nc {\n" +
"  dimensions:\n" +
"    trajectory = 2;\n" +
"    obs = 273;\n" +
"    cruise_strlen = 4;\n" +
"  variables:\n" +
"    char cruise(trajectory=2, cruise_strlen=4);\n" +
"      :cf_role = \"trajectory_id\";\n" +
"      :comment = \"The first two digits are the last two digits of the year and the last two are the consecutive cruise number for that particular vessel (01, 02, 03, ...).\";\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"Cruise\";\n" +
"\n" +
"    double time(trajectory=2, obs=273);\n" +
"      :_CoordinateAxisType = \"Time\";\n" +
"      :_FillValue = NaN; // double\n" +
"      :actual_range = 9.5810382E8, 9.9199518E8; // double\n" +
"      :axis = \"T\";\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Time\";\n" +
"      :standard_name = \"time\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    float longitude(trajectory=2, obs=273);\n" +
"      :_CoordinateAxisType = \"Lon\";\n" +
"      :_FillValue = NaNf; // float\n" +
"      :actual_range = -124.6335f, -121.854f; // float\n" +
"      :axis = \"X\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Longitude\";\n" +
"      :standard_name = \"longitude\";\n" +
"      :units = \"degrees_east\";\n" +
"\n" +
"    float latitude(trajectory=2, obs=273);\n" +
"      :_CoordinateAxisType = \"Lat\";\n" +
"      :_FillValue = NaNf; // float\n" +
"      :actual_range = 36.5567f, 38.8782f; // float\n" +
"      :axis = \"Y\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Latitude\";\n" +
"      :standard_name = \"latitude\";\n" +
"      :units = \"degrees_north\";\n" +
"\n" +
"    float bucket_temperature(trajectory=2, obs=273);\n" +
"      :_FillValue = NaNf; // float\n" +
"      :actual_range = 9.1f, 15.5f; // float\n" +
"      :colorBarMaximum = 32.0; // double\n" +
"      :colorBarMinimum = 0.0; // double\n" +
"      :coordinates = \"time latitude longitude\";\n" +
"      :ioos_category = \"Temperature\";\n" +
"      :long_name = \"Bucket Temperature\";\n" +
"      :standard_name = \"sea_water_temperature\";\n" +
"      :units = \"degree_C\";\n" +
"\n" +
"  // global attributes:\n" +
"  :cdm_data_type = \"Trajectory\";\n" +
"  :cdm_trajectory_variables = \"cruise\";\n" +
"  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"  :creator_email = \"Keith.Sakuma@noaa.gov\";\n" +
"  :creator_name = \"Keith Sakuma\";\n" +
"  :Easternmost_Easting = -121.854f; // float\n" +
"  :featureType = \"Trajectory\";\n" +
"  :geospatial_lat_max = 38.8782f; // float\n" +
"  :geospatial_lat_min = 36.5567f; // float\n" +
"  :geospatial_lat_units = \"degrees_north\";\n" +
"  :geospatial_lon_max = -121.854f; // float\n" +
"  :geospatial_lon_min = -124.6335f; // float\n" +
"  :geospatial_lon_units = \"degrees_east\";\n" +
"  :history = \"2013-04-08 source files from Keith Sakuma (FED) to Lynn Dewitt (ERD) to Bob Simons (ERD)\n" +
"2013-04-09 Bob Simons (bob.simons@noaa.gov) converted files from .csv to .nc\n" +
today;
        String tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
        
//+ " (local files)\n" +
//today + " http://127.0.0.1:8080/cwexperimental/
expected = 
"  :id = \"ncCFMA1b\";\n" +
"  :infoUrl = \"http://swfsc.noaa.gov/GroundfishAnalysis/\";\n" +
"  :institution = \"NOAA SWFSC FED\";\n" +
"  :keywords = \"bottom, bucket, California, coast, cruise, ctd, data, depth, FED, fisheries, fixed, fluorometer, header, hydrographic, index, juvenile, latitude, longitude, midwater, NMFS, NOAA, pacific, rockfish, salinity, species, station, survey, SWFSC, temperature, thermosalinometer, time, transimissometer, trawl\";\n" +
"  :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"  :Northernmost_Northing = 38.8782f; // float\n" +
"  :sourceUrl = \"(local files; contact erd.data@noaa.gov)\";\n" +
"  :Southernmost_Northing = 36.5567f; // float\n" +
"  :standard_name_vocabulary = \"CF Standard Name Table v29\";\n" +
"  :subsetVariables = \"cruise,ctd_index,ctd_no,station,time,longitude,latitude\";\n" +
"  :summary = \"SWFSC FED Mid Water Trawl Juvenile Rockfish Survey: Station Information and Surface Data.\n" +
"Surveys have been conducted along the central California coast in May/June \n" +
"every year since 1983. In 2004 the survey area was expanded to cover the \n" +
"entire coast from San Diego to Cape Mendocino.  The survey samples a series \n" +
"of fixed trawl stations using a midwater trawl. The midwater trawl survey \n" +
"gear captures significant numbers of approximately 10 rockfish species during\n" +
"their pelagic juvenile stage (i.e., 50-150 days old), by which time annual\n" +
"reproductive success has been established. Catch-per-unit-effort data from\n" +
"the survey are analyzed and serve as the basis for predicting future \n" +
"recruitment to rockfish fisheries. Results for several species (e.g., \n" +
"bocaccio, chilipepper [S. goodei], and widow rockfish [S. entomelas]) have\n" +
"shown that the survey data can be useful in predicting year-class strength\n" +
"in age-based stock assessments.\n" +
"\n" +
"The survey's data on YOY Pacific whiting has also been used in the stock\n" +
"assessment process. To assist in obtaining additional northward spatial\n" +
"coverage of YOY Pacific whiting off Oregon and Washington, in 2001 the\n" +
"Pacific Whiting Conservation Cooperative in cooperation with the NOAA NMFS\n" +
"Northwest Fisheries Science Center began a midwater trawl survey patterned\n" +
"after the NOAA NMFS SWFSC Fisheries Ecology Division's (FED) existing survey. \n" +
"Both surveys work cooperatively together each year in order to resolve \n" +
"interannual abundance patterns of YOY rockfish and Pacific whiting on a \n" +
"coastwide basis, which provides expedient, critical information that can be \n" +
"used in the fisheries management process.\n" +
"\n" +
"The large quantity of physical data collected during the surveys (e.g., CTD\n" +
"with attached transimissometer and fluorometer, thermosalinometer, and ADCP)\n" +
"have provided a better understanding of the hydrographic conditions off the\n" +
"California coast and analysis of these data have been distributed through the\n" +
"publication of NOAA NMFS Technical Memoranda.\n" +
"\n" +
"For more information, see http://swfsc.noaa.gov/GroundfishAnalysis/ and\n" +
"http://www.sanctuarysimon.org/projects/project_info.php?projectID=100118\";\n" +
"  :time_coverage_end = \"2001-06-08T10:13:00Z\";\n" +
"  :time_coverage_start = \"2000-05-12T03:57:00Z\";\n" +
"  :title = \"SWFSC FED Mid Water Trawl Juvenile Rockfish Survey, Surface Data\";\n" +
"  :Westernmost_Easting = -124.6335f; // float\n" +
" data:\n" +
"cruise =\"0002\", \"0103\"\n" +
"time =\n" +
"  {\n" +
"    {9.5810382E8, 9.5810652E8, 9.5811444E8, 9.581178E8, 9.5812548E8, 9.58128E8, 9.581349E8, 9.5814078E8, 9.5814" +
"624E8, 9.5815128E8, 9.5815656E8, 9.5816232E8, 9.5816772E8, 9.5817312E8, 9.581784E8, 9.5818404E8, 9.581892E8, 9." +
"581928E8, 9.5820828E8, 9.5821176E8, 9.5822364E8, 9.582279E8, 9.5823264E8, 9.5823852E8, 9.582444E8, 9.5825022E8," +
" 9.5825532E8, 9.5826018E8, 9.582657E8, 9.5827068E8, 9.5827944E8, 9.5828862E8, 9.582906E8, 9.583074E8, 9.5831346" +
"E8, 9.5831862E8, 9.583233E8, 9.583293E8, 9.5833446E8, 9.583392E8, 9.5834442E8, 9.5834928E8, 9.583536E8, 9.58365" +
"42E8, 9.5837448E8, 9.583788E8, 9.583932E8, 9.5840292E8, 9.584082E8, 9.5841432E8, 9.5842032E8, 9.5842524E8, 9.58" +
"42986E8, 9.5843568E8, 9.5844126E8, 9.584517E8, 9.584604E8, 9.5846454E8, 9.5847534E8, 9.584814E8, 9.5848698E8, 9" +
".584919E8, 9.5849766E8, 9.5850462E8, 9.5851146E8, 9.5851752E8, 9.5852418E8, 9.585291E8, 9.5853804E8, 9.5855208E" +
"8, 9.5855478E8, 9.5856072E8, 9.585741E8, 9.5857968E8, 9.5862468E8, 9.5863734E8, 9.5871054E8, 9.5872044E8, 9.587" +
"2296E8, 9.5872968E8, 9.5873136E8, 9.587391E8, 9.587436E8, 9.5874924E8, 9.5875512E8, 9.5876082E8, 9.5876586E8, 9" +
".587718E8, 9.5877876E8, 9.5878512E8, 9.5879142E8, 9.5879706E8, 9.5880948E8, 9.588123E8, 9.58821E8, 9.5882808E8," +
" 9.588336E8, 9.588387E8, 9.5884476E8, 9.5885076E8, 9.588567E8, 9.588621E8, 9.5886696E8, 9.5887254E8, 9.5887698E" +
"8, 9.5888226E8, 9.5889264E8, 9.5889516E8, 9.5890968E8, 9.5891736E8, 9.5892216E8, 9.589269E8, 9.5893272E8, 9.589" +
"3758E8, 9.5894208E8, 9.5894772E8, 9.5895198E8, 9.5895768E8, 9.5896992E8, 9.5897928E8, 9.5898348E8, 9.589983E8, " +
"9.59007E8, 9.5901216E8, 9.5901888E8, 9.5902548E8, 9.5903496E8, 9.5904306E8, 9.59049E8, 9.5905548E8, 9.5906838E8" +
", 9.5907216E8, 9.5908914E8, 9.5910342E8, 9.5914278E8, 9.591747E8, 9.5921568E8, 9.5925504E8, 9.593121E8, 9.59316" +
"6E8, 9.5932494E8, 9.5932806E8, 9.5933724E8, 9.593403E8, 9.5934798E8, 9.595827E8, 9.5958966E8, 9.59592E8, 9.5965" +
"548E8, 9.5966238E8, 9.5974764E8, 9.5975562E8, 9.5975856E8, 9.5976948E8, 9.5977182E8, 9.5977812E8, 9.597828E8, 9" +
".5978844E8, 9.597936E8, 9.597996E8, 9.5980722E8, 9.598143E8, 9.5981922E8, 9.5982486E8, 9.5983104E8, 9.5983542E8" +
", 9.598518E8, 9.598662E8, 9.598701E8, 9.5987502E8, 9.5988144E8, 9.5988756E8, 9.5989326E8, 9.598986E8, 9.59904E8" +
", 9.599097E8, 9.5991444E8, 9.59922E8, 9.5992944E8, 9.5993118E8, 9.5995062E8, 9.5995758E8, 9.5996232E8, 9.599673" +
"6E8, 9.5997324E8, 9.5997786E8, 9.5998278E8, 9.5998842E8, 9.599934E8, 9.600069E8, 9.600183E8, 9.6002196E8, 9.600" +
"372E8, 9.6004446E8, 9.600495E8, 9.600555E8, 9.600612E8, 9.6006618E8, 9.6007122E8, 9.6007668E8, 9.6008232E8, 9.6" +
"00936E8, 9.6010272E8, 9.6010728E8, 9.601158E8, 9.601221E8, 9.601278E8, 9.6013218E8, 9.6013758E8, 9.6014442E8, 9" +
".6015084E8, 9.6015612E8, 9.601614E8, 9.601662E8, 9.6018E8, 9.601947E8, 9.601974E8, 9.6020892E8, 9.6021384E8, 9." +
"6027684E8, 9.6028956E8, 9.6031308E8, 9.6032172E8, 9.603291E8, 9.603642E8, 9.6039156E8, 9.604035E8, 9.6042414E8," +
" 9.6043758E8, 9.6044334E8, 9.6045168E8, 9.604587E8, 9.6046824E8, 9.604749E8, 9.6048732E8, 9.60498E8, 9.605127E8" +
", 9.6052104E8, 9.6052794E8, 9.6053244E8, 9.6055512E8, 9.605712E8, 9.6057462E8, 9.6057876E8, 9.6058284E8, 9.6058" +
"722E8, 9.6059022E8, 9.6059232E8, 9.6059418E8, 9.6059664E8, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, Na" +
"N, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN},\n" +
"    {9.896394E8, 9.896415E8, 9.896562E8, 9.8965908E8, 9.896676E8, 9.8967702E8, 9.8968158E8, 9.8968626E8, 9.8969" +
"13E8, 9.8969604E8, 9.897015E8, 9.8970702E8, 9.8971176E8, 9.89715E8, 9.897168E8, 9.8972808E8, 9.8974602E8, 9.897" +
"489E8, 9.8976126E8, 9.897657E8, 9.8977026E8, 9.8977572E8, 9.89781E8, 9.8978628E8, 9.8979138E8, 9.8979624E8, 9.8" +
"980176E8, 9.8980632E8, 9.898137E8, 9.898227E8, 9.898251E8, 9.898353E8, 9.898392E8, 9.8984694E8, 9.898569E8, 9.8" +
"98614E8, 9.898659E8, 9.8987136E8, 9.8987616E8, 9.8987964E8, 9.8988132E8, 9.8989134E8, 9.898956E8, 9.898992E8, 9" +
".899088E8, 9.899127E8, 9.899211E8, 9.899244E8, 9.8993388E8, 9.8993856E8, 9.8994366E8, 9.899493E8, 9.89955E8, 9." +
"8995968E8, 9.8996424E8, 9.899703E8, 9.8997588E8, 9.899862E8, 9.899964E8, 9.900009E8, 9.900117E8, 9.900159E8, 9." +
"9002424E8, 9.900291E8, 9.9003456E8, 9.9004146E8, 9.9004854E8, 9.9005436E8, 9.9006E8, 9.9006486E8, 9.90072E8, 9." +
"900837E8, 9.900867E8, 9.9009528E8, 9.901023E8, 9.9010824E8, 9.901593E8, 9.901698E8, 9.901794E8, 9.902469E8, 9.9" +
"02562E8, 9.902589E8, 9.902661E8, 9.902682E8, 9.902772E8, 9.9028962E8, 9.9029466E8, 9.9029988E8, 9.9030474E8, 9." +
"9030996E8, 9.9031596E8, 9.9032082E8, 9.9032634E8, 9.9033342E8, 9.9034974E8, 9.9035304E8, 9.9036408E8, 9.903678E" +
"8, 9.9037236E8, 9.9037818E8, 9.9038376E8, 9.903897E8, 9.9039486E8, 9.903996E8, 9.9040524E8, 9.9041022E8, 9.9041" +
"964E8, 9.9043026E8, 9.9043212E8, 9.9044046E8, 9.9044514E8, 9.9045594E8, 9.9046056E8, 9.9046506E8, 9.9047154E8, " +
"9.904758E8, 9.9048042E8, 9.90486E8, 9.904905E8, 9.904947E8, 9.9050604E8, 9.9051576E8, 9.9051936E8, 9.9052824E8," +
" 9.9053124E8, 9.9053838E8, 9.9054204E8, 9.9054684E8, 9.9055344E8, 9.9055884E8, 9.9056352E8, 9.905679E8, 9.90573" +
"9E8, 9.9057954E8, 9.905925E8, 9.9060282E8, 9.906069E8, 9.9061734E8, 9.906234E8, 9.9062808E8, 9.9063336E8, 9.906" +
"387E8, 9.9064518E8, 9.906516E8, 9.9065694E8, 9.9066288E8, 9.9066804E8, 9.906792E8, 9.906948E8, 9.9069762E8, 9.9" +
"070482E8, 9.9070998E8, 9.9071406E8, 9.9071928E8, 9.9076548E8, 9.9077514E8, 9.9077898E8, 9.9079908E8, 9.9080478E" +
"8, 9.908112E8, 9.9081738E8, 9.9082386E8, 9.9083046E8, 9.908361E8, 9.908418E8, 9.9084768E8, 9.9085314E8, 9.90863" +
"88E8, 9.9086958E8, 9.908814E8, 9.9088746E8, 9.9089442E8, 9.909003E8, 9.9090642E8, 9.9091164E8, 9.9091686E8, 9.9" +
"09222E8, 9.9092838E8, 9.9093456E8, 9.9094992E8, 9.9095598E8, 9.90966E8, 9.9097104E8, 9.9097692E8, 9.9098262E8, " +
"9.909912E8, 9.9099738E8, 9.9127836E8, 9.912849E8, 9.9129342E8, 9.9129642E8, 9.9130326E8, 9.9130554E8, 9.9131472" +
"E8, 9.913197E8, 9.9132486E8, 9.913296E8, 9.9133512E8, 9.9133998E8, 9.9134454E8, 9.913494E8, 9.9135702E8, 9.9136" +
"23E8, 9.913677E8, 9.91371E8, 9.913857E8, 9.9138978E8, 9.9140214E8, 9.914061E8, 9.9141084E8, 9.9141624E8, 9.9142" +
"146E8, 9.9142722E8, 9.9143238E8, 9.914373E8, 9.914433E8, 9.914481E8, 9.91458E8, 9.914655E8, 9.914685E8, 9.91477" +
"86E8, 9.914862E8, 9.9149634E8, 9.9150156E8, 9.9150732E8, 9.9151452E8, 9.9152418E8, 9.9153606E8, 9.9154488E8, 9." +
"915525E8, 9.915564E8, 9.916725E8, 9.916776E8, 9.916827E8, 9.9168786E8, 9.9169596E8, 9.91701E8, 9.91707E8, 9.917" +
"127E8, 9.9171726E8, 9.9172824E8, 9.917337E8, 9.917448E8, 9.9175254E8, 9.9175794E8, 9.91764E8, 9.9176946E8, 9.91" +
"77426E8, 9.9177924E8, 9.917847E8, 9.9179016E8, 9.9180414E8, 9.918135E8, 9.9181848E8, 9.918342E8, 9.9183978E8, 9" +
".9184446E8, 9.9189126E8, 9.9190008E8, 9.9190422E8, 9.9191406E8, 9.9191994E8, 9.919251E8, 9.919302E8, 9.9193548E" +
"8, 9.9194106E8, 9.9194808E8, 9.9195492E8, 9.9196068E8, 9.919665E8, 9.9198552E8, 9.9199188E8, 9.9199518E8}\n" +
"  }\n" +
"longitude =\n" +
"  {\n" +
"    {-121.9383, -121.983, -121.8678, -121.978, -121.8947, -121.8622, -121.9445, -122.165, -122.2658, -122.372, " +
"-122.4718, -122.5868, -122.6765, -122.7827, -122.6795, -122.4735, -122.2703, -122.1743, -122.0333, -122.0493, -" +
"122.1712, -122.1672, -122.3698, -122.576, -122.7827, -122.9822, -122.8845, -122.7843, -122.5792, -122.3702, -12" +
"2.2922, -122.4007, -122.4288, -122.602, -122.4748, -122.6752, -122.887, -123.0893, -123.19, -123.0853, -122.880" +
"7, -122.6773, -122.4762, -122.5677, -122.6557, -122.8165, -122.9778, -122.988, -123.1957, -123.4012, -123.589, " +
"-123.6052, -123.6057, -123.4007, -123.1935, -123.0453, -123.1905, -123.14, -123.3443, -123.4868, -123.5028, -12" +
"3.7077, -123.9117, -124.1147, -123.914, -123.7028, -123.503, -123.2983, -123.3922, -123.2657, -123.1785, -123.0" +
"832, -123.294, -123.0938, -122.87, -122.8973, -121.9863, -121.8953, -121.9795, -121.8875, -121.861, -121.9867, " +
"-122.1645, -122.2668, -122.3707, -122.4753, -122.5802, -122.6745, -122.6843, -122.4757, -122.2707, -122.1758, -" +
"122.0458, -122.05, -122.1108, -122.1307, -122.1677, -122.3698, -122.5767, -122.7825, -122.99, -122.8838, -122.7" +
"855, -122.5772, -122.3728, -122.2993, -122.3583, -122.4263, -122.5778, -122.4738, -122.6782, -122.8847, -123.08" +
"92, -123.1888, -123.0898, -122.8842, -122.7025, -122.4732, -122.5672, -122.6413, -122.8162, -122.9707, -122.987" +
"8, -123.1925, -123.3987, -123.6032, -123.605, -123.4057, -123.1952, -123.0448, -123.217, -123.151, -123.478, -1" +
"23.9125, -122.5752, -123.0175, -123.5002, -122.8497, -123.0917, -123.0008, -123.0675, -123.1683, -123.2533, -12" +
"3.3672, -123.5777, -122.865, -122.8885, -122.8492, -121.9338, -121.9888, -121.9952, -121.8622, -121.978, -121.8" +
"885, -121.8595, -121.9693, -122.1527, -122.2703, -122.3707, -122.4725, -122.574, -122.7818, -122.6802, -122.475" +
"7, -122.2742, -122.1773, -122.0398, -122.1692, -122.1678, -122.3703, -122.5735, -122.7852, -122.9817, -122.8832" +
", -122.7847, -122.5795, -122.3728, -122.2923, -122.3835, -122.4223, -122.5922, -122.4752, -122.6767, -122.8852," +
" -123.0895, -123.1907, -123.0882, -122.8853, -122.6787, -122.5675, -122.6608, -122.8157, -122.9975, -122.9915, " +
"-123.1962, -123.4022, -123.6065, -123.6048, -123.6067, -123.4015, -123.1925, -123.039, -123.2132, -123.1407, -1" +
"23.3305, -123.486, -123.5062, -123.7095, -123.9142, -124.1162, -123.9128, -123.7062, -123.504, -123.2943, -123." +
"373, -123.2622, -123.167, -123.3012, -123.0875, -122.8728, -122.8223, -122.9352, -123.0797, -123.1307, -123.535" +
"3, -123.555, -123.5022, -123.0465, -123.5415, -123.5448, -123.4182, -123.5035, -123.407, -123.2483, -123.0523, " +
"-123.2832, -123.5358, -123.4152, -123.5, -123.4767, -123.278, -123.9938, -123.9098, -123.821, -123.7398, -123.6" +
"592, -123.6073, -123.5595, -123.515, -123.4487, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN" +
", NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN},\n" +
"    {-121.935, -121.9837, -121.8783, -121.9773, -121.9295, -121.9675, -122.1682, -122.2683, -122.3718, -122.473" +
"2, -122.5798, -122.7845, -122.681, -122.274, -122.4772, -122.1835, -122.0435, -122.0428, -122.1655, -122.1667, " +
"-122.3745, -122.5795, -122.785, -122.9898, -122.89, -122.7855, -122.5797, -122.2278, -122.294, -122.3792, -122." +
"4268, -122.6173, -122.7585, -122.7515, -122.472, -122.6803, -122.8853, -123.1043, -123.1953, -122.8838, -123.09" +
"07, -122.6807, -122.48, -122.567, -122.6508, -122.815, -123.0032, -123.0265, -122.884, -122.9882, -123.195, -12" +
"3.4022, -123.6077, -123.6063, -123.606, -123.3978, -123.1942, -123.0408, -123.2157, -123.1318, -123.3233, -123." +
"5045, -123.5042, -123.7242, -123.9135, -124.118, -123.9128, -123.7062, -123.5037, -123.2938, -123.2952, -123.16" +
"97, -123.1165, -123.0172, -123.0933, -123.2963, -122.895, -122.9217, -122.8875, -121.9975, -121.8617, -121.9662" +
", -121.9013, -121.939, -121.854, -122.172, -122.2683, -122.3733, -122.4725, -122.5828, -122.7793, -122.6798, -1" +
"22.4717, -122.1695, -122.025, -122.0505, -122.162, -122.1665, -122.3692, -122.5718, -122.78, -122.9942, -122.88" +
"35, -122.7828, -122.5775, -122.3755, -122.2805, -122.3933, -122.4087, -122.6017, -122.7418, -122.4722, -122.681" +
"8, -122.8838, -123.0885, -123.1875, -123.0882, -122.8838, -122.6778, -122.4737, -122.575, -122.6643, -122.8052," +
" -123.0057, -123.0277, -123.028, -122.9965, -123.1975, -123.4015, -123.6075, -123.6053, -123.604, -123.4007, -1" +
"23.1945, -123.0337, -123.2143, -123.1398, -123.3497, -123.5195, -123.504, -123.7067, -123.9122, -124.1182, -123" +
".9112, -123.7072, -123.5017, -123.2992, -123.3752, -123.2623, -123.1822, -123.0678, -123.006, -123.091, -123.29" +
"53, -122.846, -122.9152, -122.879, -123.4812, -123.579, -123.6772, -123.7663, -123.8853, -123.9257, -123.988, -" +
"124.0528, -124.1277, -124.2243, -124.2975, -124.3948, -124.2242, -124.0617, -123.8733, -123.7113, -123.5368, -1" +
"23.356, -123.5675, -123.7787, -123.9937, -124.2048, -124.4, -124.6335, -124.427, -124.2418, -124.0467, -123.877" +
"5, -123.6748, -123.4848, -122.9317, -121.9948, -121.8585, -121.9563, -121.8952, -121.878, -121.9432, -122.1668," +
" -122.2668, -122.3738, -122.4737, -122.5777, -122.6775, -122.7813, -122.6748, -122.4732, -122.272, -122.1878, -" +
"122.0237, -122.038, -122.1575, -122.1555, -122.3737, -122.5758, -122.7843, -122.9858, -122.8855, -122.7838, -12" +
"2.5775, -122.3718, -122.3078, -122.372, -122.433, -122.5848, -122.7522, -122.4738, -122.6763, -122.8827, -123.0" +
"87, -123.0877, -122.8822, -122.6765, -122.4728, -122.5847, -122.4728, -122.6772, -122.8783, -123.0867, -123.194" +
", -123.09, -122.8865, -122.6798, -122.6605, -122.808, -122.9938, -123.0082, -122.9867, -123.1928, -123.4003, -1" +
"23.6047, -123.6043, -123.605, -123.4015, -123.1943, -123.06, -123.1995, -123.1495, -123.3497, -123.2977, -123.0" +
"918, -123.02, -123.0633, -123.175, -123.2713, -123.352, -123.2973, -123.5008, -123.7057, -123.9122, -124.1138, " +
"-123.9128, -123.709, -123.5042, -122.8775, -122.9085, -122.843}\n" +
"  }\n" +
"latitude =\n" +
"  {\n" +
"    {36.8817, 36.8435, 36.7667, 36.7393, 36.7065, 36.644, 36.6793, 36.668, 36.7722, 36.6667, 36.7713, 36.6627, " +
"36.772, 36.669, 36.5625, 36.5625, 36.563, 36.584, 36.5778, 36.6458, 36.7925, 36.8727, 36.8805, 36.8783, 36.8783" +
", 36.8748, 36.9762, 37.0815, 37.082, 37.0797, 36.9813, 36.9822, 36.9827, 37.014, 37.1702, 37.1777, 37.1735, 37." +
"1807, 37.2717, 37.3698, 37.3707, 37.3713, 37.3717, 37.2727, 37.2963, 37.2712, 37.291, 37.5142, 37.513, 37.513, " +
"37.5133, 37.6382, 37.7675, 37.7682, 37.7693, 37.6605, 37.671, 37.7405, 37.873, 37.8625, 38.0275, 38.0293, 38.02" +
"82, 38.1673, 38.3095, 38.3077, 38.3085, 38.3082, 38.1842, 38.1542, 38.1793, 38.1622, 38.026, 38.0265, 37.7957, " +
"37.6812, 36.845, 36.7605, 36.7428, 36.685, 36.6423, 36.654, 36.6698, 36.7718, 36.6678, 36.7723, 36.6675, 36.770" +
"5, 36.5618, 36.5635, 36.5592, 36.5845, 36.5932, 36.6468, 36.6918, 36.7428, 36.8765, 36.8772, 36.877, 36.8773, 3" +
"6.8778, 36.9835, 37.0838, 37.0845, 37.0835, 36.9902, 36.9677, 36.9862, 36.9652, 37.1795, 37.1793, 37.1793, 37.1" +
"785, 37.2765, 37.372, 37.3727, 37.369, 37.3717, 37.2748, 37.2532, 37.2778, 37.2448, 37.5135, 37.512, 37.5105, 3" +
"7.5118, 37.6397, 37.7702, 37.768, 37.6587, 37.6768, 37.7363, 37.8702, 38.0265, 37.2858, 37.2407, 38.3072, 37.76" +
"87, 38.0268, 38.166, 38.1422, 38.1685, 38.146, 38.1675, 38.166, 37.7893, 37.6617, 37.6132, 36.8827, 36.8553, 36" +
".8523, 36.7605, 36.7432, 36.699, 36.6422, 36.671, 36.6672, 36.7747, 36.6662, 36.7725, 36.6663, 36.67, 36.5642, " +
"36.5567, 36.5605, 36.5828, 36.5728, 36.781, 36.8768, 36.8785, 36.8768, 36.877, 36.876, 36.9863, 37.0832, 37.077" +
"8, 37.0835, 36.9845, 36.9863, 36.983, 36.9812, 37.1793, 37.1807, 37.179, 37.1807, 37.2772, 37.3725, 37.359, 37." +
"3725, 37.2762, 37.2865, 37.2782, 37.2897, 37.5112, 37.5145, 37.5128, 37.5147, 37.6398, 37.7695, 37.7717, 37.771" +
", 37.6555, 37.6612, 37.7475, 37.886, 37.8637, 38.0247, 38.0273, 38.0293, 38.1695, 38.312, 38.311, 38.3108, 38.3" +
"077, 38.1657, 38.1712, 38.1682, 38.0267, 38.0282, 37.7862, 37.5865, 37.499, 37.6465, 37.7122, 38.0843, 38.0692," +
" 37.9557, 37.985, 38.2377, 38.2072, 38.09, 38.0832, 37.9738, 37.9252, 38.1003, 38.17, 38.2578, 38.1047, 38.0927" +
", 38.0208, 38.1807, 38.2947, 38.3463, 38.3983, 38.4457, 38.4998, 38.543, 38.576, 38.6038, 38.6472, NaN, NaN, Na" +
"N, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN},\n" +
"    {36.8833, 36.8455, 36.7733, 36.7415, 36.7025, 36.6543, 36.6658, 36.7705, 36.6667, 36.7703, 36.6658, 36.668," +
" 36.5632, 36.5622, 36.5602, 36.5782, 36.5978, 36.6432, 36.7628, 36.8748, 36.8775, 36.8785, 36.8767, 36.876, 36." +
"9833, 37.0825, 37.0838, 36.9957, 36.9817, 36.983, 36.9843, 37.0058, 36.986, 36.8898, 37.1792, 37.1808, 37.1787," +
" 37.1797, 37.2737, 37.3707, 37.37, 37.3722, 37.3717, 37.2748, 37.2625, 37.265, 37.2867, 37.3452, 37.4033, 37.51" +
"52, 37.5127, 37.5143, 37.5127, 37.6407, 37.7693, 37.7677, 37.7693, 37.6572, 37.656, 37.7328, 37.8805, 37.8885, " +
"38.0233, 38.0258, 38.027, 38.1678, 38.308, 38.3105, 38.3058, 38.31, 38.1683, 38.1663, 38.1862, 38.171, 38.0267," +
" 38.0253, 37.805, 37.7008, 37.9318, 36.8495, 36.7617, 36.7387, 36.7135, 36.6593, 36.6427, 36.6693, 36.7695, 36." +
"6702, 36.8053, 36.6667, 36.6653, 36.5648, 36.5617, 36.5815, 36.59, 36.6458, 36.7795, 36.8777, 36.878, 36.8738, " +
"36.8737, 36.8782, 36.9822, 37.0807, 37.0828, 37.0833, 36.9792, 36.994, 36.9765, 36.9773, 36.9822, 37.1772, 37.1" +
"777, 37.1778, 37.1785, 37.2757, 37.3703, 37.3715, 37.3722, 37.3708, 37.2833, 37.2833, 37.2678, 37.2902, 37.3468" +
", 37.4335, 37.5132, 37.517, 37.5162, 37.5128, 37.6413, 37.769, 37.7712, 37.7698, 37.655, 37.6743, 37.7422, 37.8" +
"772, 37.8957, 38.0255, 38.0305, 38.0292, 38.168, 38.3112, 38.3097, 38.3072, 38.3078, 38.176, 38.1612, 38.1783, " +
"38.1502, 38.1668, 38.0237, 38.0262, 37.6073, 37.6992, 37.8022, 38.1178, 38.2673, 38.418, 38.57, 38.7202, 38.878" +
"2, 38.716, 38.5608, 38.4067, 38.2563, 38.0858, 37.9538, 38.0365, 38.1548, 38.2703, 38.3548, 38.4442, 38.5348, 3" +
"8.5365, 38.5382, 38.538, 38.5352, 38.5328, 38.5408, 38.459, 38.409, 38.3392, 38.4155, 38.194, 38.1185, 36.8823," +
" 36.8472, 36.766, 36.7403, 36.705, 36.6472, 36.6602, 36.6675, 36.7712, 36.6658, 36.7718, 36.6688, 36.7712, 36.6" +
"678, 36.5613, 36.5625, 36.5622, 36.5862, 36.5687, 36.6405, 36.7865, 36.874, 36.8773, 36.877, 36.8775, 36.8765, " +
"36.9822, 37.0822, 37.083, 37.0828, 36.9958, 36.9708, 36.9935, 36.968, 36.9753, 37.1785, 37.177, 37.1798, 37.178" +
"7, 37.3697, 37.3712, 37.3712, 37.3703, 37.2902, 37.3727, 37.3722, 37.3722, 37.3707, 37.2755, 37.1757, 37.1783, " +
"37.1775, 37.2835, 37.2637, 37.2825, 37.3348, 37.5122, 37.5138, 37.5125, 37.5135, 37.6375, 37.7692, 37.7695, 37." +
"7698, 37.671, 37.6438, 37.7538, 37.8475, 38.0265, 38.0268, 38.18, 38.145, 38.1737, 38.1498, 38.1502, 38.3067, 3" +
"8.3068, 38.3058, 38.3068, 38.1687, 38.0272, 38.025, 38.0267, 37.8, 37.6967, 37.6037}\n" +
"  }\n" +
"bucket_temperature =\n" +
"  {\n" +
"    {12.0, 11.3, 12.0, 11.7, 11.0, 12.6, 11.1, 12.4, 12.8, 13.7, 13.5, 13.5, 12.7, 13.5, 14.2, 14.0, 13.5, 13.2" +
", 11.2, 11.7, 10.8, 10.8, 11.3, 11.9, 14.0, 12.7, 13.8, 14.1, 13.2, 12.5, 11.6, 12.4, 12.3, 11.7, 10.8, 12.5, 1" +
"2.6, 13.3, 13.3, 13.4, 13.2, 11.6, 12.0, 12.5, 12.2, 13.3, 13.5, 12.2, 13.5, 13.5, 13.5, 13.8, 13.8, 13.5, 13.3" +
", 11.3, 12.5, 12.8, 13.5, 12.8, 13.3, 12.8, 13.8, 14.0, 13.7, 13.7, 13.5, 13.0, 13.5, 12.8, 11.7, 10.5, 12.4, 1" +
"0.6, 11.6, 11.5, 12.9, 12.5, 12.5, 13.0, 13.9, 12.2, 12.2, 11.9, 13.4, 12.9, 14.4, 14.3, 14.8, 14.9, 14.1, 13.1" +
", 12.3, 12.0, 11.9, 11.8, 12.8, 11.6, 12.2, 13.2, 13.9, 13.2, 12.7, 12.2, 11.2, 10.6, 10.8, 11.5, 11.7, 11.5, 1" +
"2.0, 12.6, 11.9, 12.5, 12.2, 12.4, 14.2, 13.5, 12.9, 13.2, 11.8, 11.9, 11.4, 11.5, 10.7, 13.3, 13.2, 10.8, 11.8" +
", 11.5, 11.5, 11.3, 10.1, 13.2, 12.4, 12.2, 10.1, 10.9, 10.2, 10.3, 10.0, 9.5, 9.3, 9.5, 10.1, 10.2, 10.2, 10.2" +
", 13.5, 11.9, 10.8, 11.7, 11.2, 12.2, 13.5, 10.7, 10.2, 11.0, 12.2, 11.5, 12.4, 12.9, 12.9, 12.2, 11.5, 10.9, 1" +
"0.8, 10.8, 10.8, 11.0, 11.5, 12.2, 13.6, 12.5, 12.6, 11.8, 10.5, 10.2, 11.2, 11.1, 11.8, 10.2, 11.2, 11.5, 13.2" +
", 12.3, 12.5, 12.2, 12.5, 12.1, 11.5, 11.0, 12.0, 10.5, 10.4, 11.2, 13.4, 11.8, 11.8, 11.8, 10.3, 11.0, 11.6, 1" +
"0.5, 10.1, 10.4, 9.9, 13.2, 13.7, 12.9, 14.2, 14.9, 12.2, 11.3, 11.0, 10.1, 10.8, NaN, 11.8, 13.0, 12.8, 11.8, " +
"12.2, 11.3, 11.7, 11.2, 12.5, 10.3, 13.0, 12.3, 11.0, 11.2, 10.5, 11.2, 11.9, 10.3, 11.5, 10.7, 11.2, 10.6, 10." +
"8, 12.7, 13.3, 13.7, 13.6, 12.2, 12.8, 11.8, 12.1, 10.6, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN," +
" NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN},\n" +
"    {13.8, 13.1, 11.6, 12.0, 12.2, 11.2, 13.2, 12.5, 12.3, 12.8, 13.4, 13.6, 13.9, 12.8, 13.7, 13.7, 11.2, 11.5" +
", 13.4, 12.2, 12.3, 13.2, 12.5, 13.5, 12.1, 12.2, 12.2, 13.2, 12.8, 11.9, 12.4, 12.7, 12.3, 12.4, 11.9, 12.0, 1" +
"2.7, 13.0, 12.2, 13.4, 12.9, 12.4, 13.3, 13.0, 12.7, 11.8, 12.0, 12.3, 12.1, 11.6, 13.1, 13.5, 13.7, 13.8, 13.6" +
", 13.8, 12.8, 13.2, 13.3, 12.5, 12.9, 13.4, 13.0, 13.0, 11.4, 11.7, 12.5, 11.3, 11.1, 10.6, 10.5, 10.5, 10.4, 1" +
"0.6, 10.6, 10.0, 11.7, 11.3, 10.2, 12.3, 12.6, 11.4, 11.9, 11.6, 13.2, 12.7, 12.9, 12.6, 13.1, 13.6, 14.4, 14.8" +
", 13.7, 13.0, 12.3, 12.9, 13.1, 12.9, 13.0, 13.4, 13.6, 13.8, 13.2, 13.3, 12.0, 13.0, 12.3, 12.7, 12.7, 13.3, 1" +
"2.5, 11.4, 12.4, 12.5, 12.3, 12.7, 13.0, 13.2, 12.7, 12.4, 12.9, 12.5, 12.6, 12.7, 12.6, 12.9, 13.1, 13.4, 13.1" +
", 14.2, 14.5, 14.4, 13.2, 13.3, 13.3, 13.5, 13.0, 11.7, 12.3, 10.9, 13.0, 13.7, 13.5, 13.2, 12.2, 11.2, 11.4, 1" +
"0.4, 11.0, 11.4, 11.8, 11.9, 11.8, 11.1, 11.8, 11.9, 11.5, 10.2, 10.1, 10.5, 10.4, 10.8, 11.2, 10.8, 12.9, 12.9" +
", 12.4, 12.7, 12.8, 13.2, 13.1, 11.0, 10.7, 10.6, 10.5, 10.6, 10.6, 11.5, 12.5, 12.7, 12.3, 12.4, 12.5, 12.5, 1" +
"0.7, 12.5, 11.0, 14.0, 13.1, 13.3, 12.5, 13.4, 14.1, 14.1, 13.4, 13.3, 15.1, 13.8, 14.8, 14.0, 15.1, 15.3, 14.6" +
", 15.5, 15.5, 14.4, 14.5, 14.2, 13.4, 13.3, 13.7, 14.0, 14.7, 13.5, 13.5, 13.1, 12.7, 11.7, 12.0, 12.0, 12.4, 1" +
"2.2, 11.8, 12.9, 12.6, 12.5, 12.5, 11.6, 13.0, 12.3, 12.5, 11.5, 11.9, 11.7, 11.3, 12.3, 12.0, 12.2, 12.1, 12.3" +
", 11.9, 12.1, 12.1, 10.5, 10.6, 11.7, 12.2, 12.5, 12.6, 12.9, 10.7, 10.7, 10.6, 10.4, 12.3, 9.7, 9.7, 11.2, 10." +
"0, 9.6, 9.4, 9.6, 9.1, 9.6, 9.8, 10.5, 11.6, 11.3, 12.5, 10.8, 10.8, 10.7, 11.0}\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);

        String2.log("\n*** EDDTableFromNcFiles.testNcCFMA1b finished.");

    }


    /**
     * Test making an .ncCF TrajectoryProfile file.
     */
    public static void testNcCF2a() throws Throwable {

        String2.log("\n*** EDDTableFromNcFiles.testNcCF2a");
        EDDTable tedd = (EDDTable)oneFromDatasetsXml(null, "erdGlobecBottle"); //should work
        String tName, error, results, expected;
        int po;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

        //lon lat time range 
        tName = tedd.makeNewFileForDapQuery(null, null, 
            //for nwioosAdcp1995
            //"yearday,longitude,latitude,altitude,eastv,northv" +
            //"&yearday>=241.995&yearday<=242", 
            //for erdGlobecBottle
            "cruise_id,ship,cast,longitude,latitude,time,bottle_posn,temperature0" +
            "&time>=2002-08-19T08:00:00Z&time<=2002-08-19T12:00:00Z",
            EDStatic.fullTestCacheDirectory, "ncCF2a", ".ncCF"); 
        results = NcHelper.dumpString(EDStatic.fullTestCacheDirectory + tName, true);
        //String2.log(results);
        expected = 
"netcdf ncCF2a.nc {\n" +
"  dimensions:\n" +
"    trajectory = 1;\n" +
"    profile = 2;\n" +
"    obs = 13;\n" +
"    cruise_id_strlen = 6;\n" +
"    ship_strlen = 11;\n" +
"  variables:\n" +
"    char cruise_id(trajectory=1, cruise_id_strlen=6);\n" +
"      :cf_role = \"trajectory_id\";\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"Cruise ID\";\n" +
"\n" +
"    char ship(trajectory=1, ship_strlen=11);\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"Ship\";\n" +
"\n" +
"    short cast(profile=2);\n" +
"      :_FillValue = 32767S; // short\n" +
"      :actual_range = 127S, 127S; // short\n" +
"      :colorBarMaximum = 140.0; // double\n" +
"      :colorBarMinimum = 0.0; // double\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"Cast Number\";\n" +
"      :missing_value = 32767S; // short\n" +
"\n" +
"    float longitude(profile=2);\n" +
"      :_CoordinateAxisType = \"Lon\";\n" +
"      :_FillValue = NaNf; // float\n" +
"      :actual_range = -124.3f, -124.18f; // float\n" +
"      :axis = \"X\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Longitude\";\n" +
"      :missing_value = NaNf; // float\n" +
"      :standard_name = \"longitude\";\n" +
"      :units = \"degrees_east\";\n" +
"\n" +
"    float latitude(profile=2);\n" +
"      :_CoordinateAxisType = \"Lat\";\n" +
"      :_FillValue = NaNf; // float\n" +
"      :actual_range = 44.65f, 44.65f; // float\n" +
"      :axis = \"Y\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Latitude\";\n" +
"      :missing_value = NaNf; // float\n" +
"      :standard_name = \"latitude\";\n" +
"      :units = \"degrees_north\";\n" +
"\n" +
"    double time(profile=2);\n" +
"      :_CoordinateAxisType = \"Time\";\n" +
"      :_FillValue = NaN; // double\n" +
"      :actual_range = 1.02974748E9, 1.02975156E9; // double\n" +
"      :axis = \"T\";\n" +
"      :cf_role = \"profile_id\";\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Time\";\n" +
"      :missing_value = NaN; // double\n" +
"      :standard_name = \"time\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    int trajectoryIndex(profile=2);\n" +
"      :instance_dimension = \"trajectory\";\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"The trajectory to which this profile is associated.\";\n" +
"\n" +
"    int rowSize(profile=2);\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"Number of Observations for this Profile\";\n" +
"      :sample_dimension = \"obs\";\n" +
"\n" +
"    byte bottle_posn(obs=13);\n" +
"      :_CoordinateAxisType = \"Height\";\n" +
"      :_FillValue = 127B; // byte\n" +
"      :actual_range = 1B, 7B; // byte\n" +
"      :axis = \"Z\";\n" +
"      :colorBarMaximum = 12.0; // double\n" +
"      :colorBarMinimum = 0.0; // double\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Bottle Number\";\n" +
"      :missing_value = -128B; // byte\n" +
"\n" +
"    float temperature0(obs=13);\n" +
"      :_FillValue = -9999999.0f; // float\n" +
"      :actual_range = 7.223f, 9.62f; // float\n" +
"      :colorBarMaximum = 32.0; // double\n" +
"      :colorBarMinimum = 0.0; // double\n" +
"      :coordinates = \"time latitude longitude bottle_posn\";\n" +
"      :ioos_category = \"Temperature\";\n" +
"      :long_name = \"Sea Water Temperature from T0 Sensor\";\n" +
"      :missing_value = -9999.0f; // float\n" +
"      :standard_name = \"sea_water_temperature\";\n" +
"      :units = \"degree_C\";\n" +
"\n" +
"  // global attributes:\n" +
"  :cdm_altitude_proxy = \"bottle_posn\";\n" +
"  :cdm_data_type = \"TrajectoryProfile\";\n" +
"  :cdm_profile_variables = \"cast, longitude, latitude, time\";\n" +
"  :cdm_trajectory_variables = \"cruise_id, ship\";\n" +
"  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"  :Easternmost_Easting = -124.18f; // float\n" +
"  :featureType = \"TrajectoryProfile\";\n" +
"  :geospatial_lat_max = 44.65f; // float\n" +
"  :geospatial_lat_min = 44.65f; // float\n" +
"  :geospatial_lat_units = \"degrees_north\";\n" +
"  :geospatial_lon_max = -124.18f; // float\n" +
"  :geospatial_lon_min = -124.3f; // float\n" +
"  :geospatial_lon_units = \"degrees_east\";\n" +
"  :history = \"" +
today;
        String tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
        
//+ " http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle\n" +
//today + " http://127.0.0.1:8080/cwexperimental/
expected = 
"tabledap/erdGlobecBottle.ncCF?cruise_id,ship,cast,longitude,latitude,time,bottle_posn,temperature0&time>=2002-08-19T08:00:00Z&time<=2002-08-19T12:00:00Z\";\n" +
"  :id = \"ncCF2a\";\n" +
"  :infoUrl = \"http://www.globec.org/\";\n" +
"  :institution = \"GLOBEC\";\n" +
"  :keywords = \"10um,\n" +
"Biosphere > Vegetation > Photosynthetically Active Radiation,\n" +
"Oceans > Ocean Chemistry > Ammonia,\n" +
"Oceans > Ocean Chemistry > Chlorophyll,\n" +
"Oceans > Ocean Chemistry > Nitrate,\n" +
"Oceans > Ocean Chemistry > Nitrite,\n" +
"Oceans > Ocean Chemistry > Nitrogen,\n" +
"Oceans > Ocean Chemistry > Oxygen,\n" +
"Oceans > Ocean Chemistry > Phosphate,\n" +
"Oceans > Ocean Chemistry > Pigments,\n" +
"Oceans > Ocean Chemistry > Silicate,\n" +
"Oceans > Ocean Optics > Attenuation/Transmission,\n" +
"Oceans > Ocean Temperature > Water Temperature,\n" +
"Oceans > Salinity/Density > Salinity,\n" +
"active, after, ammonia, ammonium, attenuation, biosphere, bottle, cast, chemistry, chlorophyll, chlorophyll-a, color, concentration, concentration_of_chlorophyll_in_sea_water, cruise, data, density, dissolved, dissolved nutrients, dissolved o2, fluorescence, fraction, from, globec, identifier, mass, mole, mole_concentration_of_ammonium_in_sea_water, mole_concentration_of_nitrate_in_sea_water, mole_concentration_of_nitrite_in_sea_water, mole_concentration_of_phosphate_in_sea_water, mole_concentration_of_silicate_in_sea_water, moles, moles_of_nitrate_and_nitrite_per_unit_mass_in_sea_water, n02, nep, nh4, nitrate, nitrite, nitrogen, no3, number, nutrients, o2, ocean, ocean color, oceans, optical, optical properties, optics, oxygen, passing, per, phaeopigments, phosphate, photosynthetically, pigments, plus, po4, properties, radiation, rosette, salinity, screen, sea, sea_water_practical_salinity, sea_water_temperature, seawater, sensor, sensors, ship, silicate, temperature, time, total, transmission, transmissivity, unit, vegetation, voltage, volume, volume_fraction_of_oxygen_in_sea_water, water\";\n" +
"  :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
"  :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"  :Northernmost_Northing = 44.65f; // float\n" +
"  :sourceUrl = \"(local files; contact erd.data@noaa.gov)\";\n" +
"  :Southernmost_Northing = 44.65f; // float\n" +
"  :standard_name_vocabulary = \"CF Standard Name Table v29\";\n" +
"  :subsetVariables = \"cruise_id, ship, cast, longitude, latitude, time\";\n" +
"  :summary = \"GLOBEC (GLOBal Ocean ECosystems Dynamics) NEP (Northeast Pacific)\n" +
"Rosette Bottle Data from New Horizon Cruise (NH0207: 1-19 August 2002).\n" +
"Notes:\n" +
"Physical data processed by Jane Fleischbein (OSU).\n" +
"Chlorophyll readings done by Leah Feinberg (OSU).\n" +
"Nutrient analysis done by Burke Hales (OSU).\n" +
"Sal00 - salinity calculated from primary sensors (C0,T0).\n" +
"Sal11 - salinity calculated from secondary sensors (C1,T1).\n" +
"secondary sensor pair was used in final processing of CTD data for\n" +
"most stations because the primary had more noise and spikes. The\n" +
"primary pair were used for cast #9, 24, 48, 111 and 150 due to\n" +
"multiple spikes or offsets in the secondary pair.\n" +
"Nutrient samples were collected from most bottles; all nutrient data\n" +
"developed from samples frozen during the cruise and analyzed ashore;\n" +
"data developed by Burke Hales (OSU).\n" +
"Operation Detection Limits for Nutrient Concentrations\n" +
"Nutrient  Range         Mean    Variable         Units\n" +
"PO4       0.003-0.004   0.004   Phosphate        micromoles per liter\n" +
"N+N       0.04-0.08     0.06    Nitrate+Nitrite  micromoles per liter\n" +
"Si        0.13-0.24     0.16    Silicate         micromoles per liter\n" +
"NO2       0.003-0.004   0.003   Nitrite          micromoles per liter\n" +
"Dates and Times are UTC.\n" +
"\n" +
"For more information, see\n" +
"http://cis.whoi.edu/science/bcodmo/dataset.cfm?id=10180&flag=view\n" +
"\n" +
"Inquiries about how to access this data should be directed to\n" +
"Dr. Hal Batchelder (hbatchelder@coas.oregonstate.edu).\";\n" +
"  :time_coverage_end = \"2002-08-19T10:06:00Z\";\n" +
"  :time_coverage_start = \"2002-08-19T08:58:00Z\";\n" +
"  :title = \"GLOBEC NEP Rosette Bottle Data (2002)\";\n" +
"  :Westernmost_Easting = -124.3f; // float\n" +
" data:\n" +
"cruise_id =\"nh0207\"\n" +
"ship =\"New_Horizon\"\n" +
"cast =\n" +
"  {127, 127}\n" +
"longitude =\n" +
"  {-124.3, -124.18}\n" +
"latitude =\n" +
"  {44.65, 44.65}\n" +
"time =\n" +
"  {1.02974748E9, 1.02975156E9}\n" +
"trajectoryIndex =\n" +
"  {0, 0}\n" +
"rowSize =\n" +
"  {7, 6}\n" +
"bottle_posn =\n" +
"  {1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6}\n" +
"temperature0 =\n" +
"  {7.314, 7.47, 7.223, 7.962, 9.515, 9.576, 9.62, 7.378, 7.897, 7.335, 8.591, 8.693, 8.708}\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);

        String2.log("\n*** EDDTableFromNcFiles.testNcCF2a finished.");

    }


    /**
     * Test making an .ncCFMA TrajectoryProfile file.
     */
    public static void testNcCFMA2a() throws Throwable {

        String2.log("\n*** EDDTableFromNcFiles.testNcCFMA2a");
        EDDTable tedd = (EDDTable)oneFromDatasetsXml(null, "erdGlobecBottle"); //should work
        String tName, error, results, expected;
        int po;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

        //lon lat time range 
        tName = tedd.makeNewFileForDapQuery(null, null, 
            //for nwioosAdcp1995
            //"yearday,longitude,latitude,altitude,eastv,northv" +
            //"&yearday>=241.995&yearday<=242", 
            //for erdGlobecBottle
            "cruise_id,ship,cast,longitude,latitude,time,bottle_posn,temperature0" +
            "&time>=2002-08-19T08:00:00Z&time<=2002-08-19T12:00:00Z",
            EDStatic.fullTestCacheDirectory, "ncCFMA2a", ".ncCFMA"); 
        results = NcHelper.dumpString(EDStatic.fullTestCacheDirectory + tName, true);
        //String2.log(results);
        expected = 
"netcdf ncCFMA2a.nc {\n" +
"  dimensions:\n" +
"    trajectory = 1;\n" +
"    profile = 2;\n" +
"    obs = 7;\n" +
"    cruise_id_strlen = 6;\n" +
"    ship_strlen = 11;\n" +
"  variables:\n" +
"    char cruise_id(trajectory=1, cruise_id_strlen=6);\n" +
"      :cf_role = \"trajectory_id\";\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"Cruise ID\";\n" +
"\n" +
"    char ship(trajectory=1, ship_strlen=11);\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"Ship\";\n" +
"\n" +
"    short cast(trajectory=1, profile=2);\n" +
"      :_FillValue = 32767S; // short\n" +
"      :actual_range = 127S, 127S; // short\n" +
"      :colorBarMaximum = 140.0; // double\n" +
"      :colorBarMinimum = 0.0; // double\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"Cast Number\";\n" +
"      :missing_value = 32767S; // short\n" +
"\n" +
"    float longitude(trajectory=1, profile=2);\n" +
"      :_CoordinateAxisType = \"Lon\";\n" +
"      :_FillValue = NaNf; // float\n"+
"      :actual_range = -124.3f, -124.18f; // float\n" +
"      :axis = \"X\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Longitude\";\n" +
"      :missing_value = NaNf; // float\n"+
"      :standard_name = \"longitude\";\n" +
"      :units = \"degrees_east\";\n" +
"\n" +
"    float latitude(trajectory=1, profile=2);\n" +
"      :_CoordinateAxisType = \"Lat\";\n" +
"      :_FillValue = NaNf; // float\n"+
"      :actual_range = 44.65f, 44.65f; // float\n" +
"      :axis = \"Y\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Latitude\";\n" +
"      :missing_value = NaNf; // float\n"+
"      :standard_name = \"latitude\";\n" +
"      :units = \"degrees_north\";\n" +
"\n" +
"    double time(trajectory=1, profile=2);\n" +
"      :_CoordinateAxisType = \"Time\";\n" +
"      :_FillValue = NaN; // double\n"+
"      :actual_range = 1.02974748E9, 1.02975156E9; // double\n" +
"      :axis = \"T\";\n" +
"      :cf_role = \"profile_id\";\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Time\";\n" +
"      :missing_value = NaN; // double\n"+
"      :standard_name = \"time\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    byte bottle_posn(trajectory=1, profile=2, obs=7);\n" +
"      :_CoordinateAxisType = \"Height\";\n" +
"      :_FillValue = 127B; // byte\n" +
"      :actual_range = 1B, 7B; // byte\n" +
"      :axis = \"Z\";\n" +
"      :colorBarMaximum = 12.0; // double\n" +
"      :colorBarMinimum = 0.0; // double\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Bottle Number\";\n" +
"      :missing_value = -128B; // byte\n" +
"\n" +
"    float temperature0(trajectory=1, profile=2, obs=7);\n" +
"      :_FillValue = -9999999.0f; // float\n" +
"      :actual_range = 7.223f, 9.62f; // float\n" +
"      :colorBarMaximum = 32.0; // double\n" +
"      :colorBarMinimum = 0.0; // double\n" +
"      :coordinates = \"time latitude longitude bottle_posn\";\n" +
"      :ioos_category = \"Temperature\";\n" +
"      :long_name = \"Sea Water Temperature from T0 Sensor\";\n" +
"      :missing_value = -9999.0f; // float\n" +
"      :standard_name = \"sea_water_temperature\";\n" +
"      :units = \"degree_C\";\n" +
"\n" +
"  // global attributes:\n" +
"  :cdm_altitude_proxy = \"bottle_posn\";\n" +
"  :cdm_data_type = \"TrajectoryProfile\";\n" +
"  :cdm_profile_variables = \"cast, longitude, latitude, time\";\n" +
"  :cdm_trajectory_variables = \"cruise_id, ship\";\n" +
"  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"  :Easternmost_Easting = -124.18f; // float\n" +
"  :featureType = \"TrajectoryProfile\";\n" +
"  :geospatial_lat_max = 44.65f; // float\n" +
"  :geospatial_lat_min = 44.65f; // float\n" +
"  :geospatial_lat_units = \"degrees_north\";\n" +
"  :geospatial_lon_max = -124.18f; // float\n" +
"  :geospatial_lon_min = -124.3f; // float\n" +
"  :geospatial_lon_units = \"degrees_east\";\n" +
"  :history = \"" +
today;
        String tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
        
//+ " http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle\n" +
//today + " http://127.0.0.1:8080/cwexperimental/
expected = 
"tabledap/erdGlobecBottle.ncCFMA?cruise_id,ship,cast,longitude,latitude,time,bottle_posn,temperature0&time>=2002-08-19T08:00:00Z&time<=2002-08-19T12:00:00Z\";\n" +
"  :id = \"ncCFMA2a\";\n" +
"  :infoUrl = \"http://www.globec.org/\";\n" +
"  :institution = \"GLOBEC\";\n" +
"  :keywords = \"10um,\n" +
"Biosphere > Vegetation > Photosynthetically Active Radiation,\n" +
"Oceans > Ocean Chemistry > Ammonia,\n" +
"Oceans > Ocean Chemistry > Chlorophyll,\n" +
"Oceans > Ocean Chemistry > Nitrate,\n" +
"Oceans > Ocean Chemistry > Nitrite,\n" +
"Oceans > Ocean Chemistry > Nitrogen,\n" +
"Oceans > Ocean Chemistry > Oxygen,\n" +
"Oceans > Ocean Chemistry > Phosphate,\n" +
"Oceans > Ocean Chemistry > Pigments,\n" +
"Oceans > Ocean Chemistry > Silicate,\n" +
"Oceans > Ocean Optics > Attenuation/Transmission,\n" +
"Oceans > Ocean Temperature > Water Temperature,\n" +
"Oceans > Salinity/Density > Salinity,\n" +
"active, after, ammonia, ammonium, attenuation, biosphere, bottle, cast, chemistry, chlorophyll, chlorophyll-a, color, concentration, concentration_of_chlorophyll_in_sea_water, cruise, data, density, dissolved, dissolved nutrients, dissolved o2, fluorescence, fraction, from, globec, identifier, mass, mole, mole_concentration_of_ammonium_in_sea_water, mole_concentration_of_nitrate_in_sea_water, mole_concentration_of_nitrite_in_sea_water, mole_concentration_of_phosphate_in_sea_water, mole_concentration_of_silicate_in_sea_water, moles, moles_of_nitrate_and_nitrite_per_unit_mass_in_sea_water, n02, nep, nh4, nitrate, nitrite, nitrogen, no3, number, nutrients, o2, ocean, ocean color, oceans, optical, optical properties, optics, oxygen, passing, per, phaeopigments, phosphate, photosynthetically, pigments, plus, po4, properties, radiation, rosette, salinity, screen, sea, sea_water_practical_salinity, sea_water_temperature, seawater, sensor, sensors, ship, silicate, temperature, time, total, transmission, transmissivity, unit, vegetation, voltage, volume, volume_fraction_of_oxygen_in_sea_water, water\";\n" +
"  :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
"  :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"  :Northernmost_Northing = 44.65f; // float\n" +
"  :sourceUrl = \"(local files; contact erd.data@noaa.gov)\";\n" +
"  :Southernmost_Northing = 44.65f; // float\n" +
"  :standard_name_vocabulary = \"CF Standard Name Table v29\";\n" +
"  :subsetVariables = \"cruise_id, ship, cast, longitude, latitude, time\";\n" +
"  :summary = \"GLOBEC (GLOBal Ocean ECosystems Dynamics) NEP (Northeast Pacific)\n" +
"Rosette Bottle Data from New Horizon Cruise (NH0207: 1-19 August 2002).\n" +
"Notes:\n" +
"Physical data processed by Jane Fleischbein (OSU).\n" +
"Chlorophyll readings done by Leah Feinberg (OSU).\n" +
"Nutrient analysis done by Burke Hales (OSU).\n" +
"Sal00 - salinity calculated from primary sensors (C0,T0).\n" +
"Sal11 - salinity calculated from secondary sensors (C1,T1).\n" +
"secondary sensor pair was used in final processing of CTD data for\n" +
"most stations because the primary had more noise and spikes. The\n" +
"primary pair were used for cast #9, 24, 48, 111 and 150 due to\n" +
"multiple spikes or offsets in the secondary pair.\n" +
"Nutrient samples were collected from most bottles; all nutrient data\n" +
"developed from samples frozen during the cruise and analyzed ashore;\n" +
"data developed by Burke Hales (OSU).\n" +
"Operation Detection Limits for Nutrient Concentrations\n" +
"Nutrient  Range         Mean    Variable         Units\n" +
"PO4       0.003-0.004   0.004   Phosphate        micromoles per liter\n" +
"N+N       0.04-0.08     0.06    Nitrate+Nitrite  micromoles per liter\n" +
"Si        0.13-0.24     0.16    Silicate         micromoles per liter\n" +
"NO2       0.003-0.004   0.003   Nitrite          micromoles per liter\n" +
"Dates and Times are UTC.\n" +
"\n" +
"For more information, see\n" +
"http://cis.whoi.edu/science/bcodmo/dataset.cfm?id=10180&flag=view\n" +
"\n" +
"Inquiries about how to access this data should be directed to\n" +
"Dr. Hal Batchelder (hbatchelder@coas.oregonstate.edu).\";\n" +
"  :time_coverage_end = \"2002-08-19T10:06:00Z\";\n" +
"  :time_coverage_start = \"2002-08-19T08:58:00Z\";\n" +
"  :title = \"GLOBEC NEP Rosette Bottle Data (2002)\";\n" +
"  :Westernmost_Easting = -124.3f; // float\n" +
" data:\n" +
"cruise_id =\"nh0207\"\n" +
"ship =\"New_Horizon\"\n" +
"cast =\n" +
"  {\n" +
"    {127, 127}\n" +
"  }\n" +
"longitude =\n" +
"  {\n" +
"    {-124.3, -124.18}\n" +
"  }\n" +
"latitude =\n" +
"  {\n" +
"    {44.65, 44.65}\n" +
"  }\n" +
"time =\n" +
"  {\n" +
"    {1.02974748E9, 1.02975156E9}\n" +
"  }\n" +
"bottle_posn =\n" +
"  {\n" +
"    {\n" +
"      {1, 2, 3, 4, 5, 6, 7},\n" +
"      {1, 2, 3, 4, 5, 6, -128}\n" +
"    }\n" +
"  }\n" +
"temperature0 =\n" +
"  {\n" +
"    {\n" +
"      {7.314, 7.47, 7.223, 7.962, 9.515, 9.576, 9.62},\n" +
"      {7.378, 7.897, 7.335, 8.591, 8.693, 8.708, -9999999.0}\n" +
"    }\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);

        String2.log("\n*** EDDTableFromNcFiles.testNcCFMA2a finished.");
    }


    /**
     * Test making an .ncCF TrajectoryProfile file (notably with different numbers of 
     * profiles and obs).
     */
    public static void testNcCF2b() throws Throwable {

        String2.log("\n*** EDDTableFromNcFiles.testNcCF2b");
        EDDTable tedd = (EDDTable)oneFromDatasetsXml(null, "erdGtsppBest"); //should work
        String tName, error, results, expected;
        int po;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

        //lon lat time range 
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "platform,cruise,org,type,station_id,longitude,latitude,time,depth," +
            "temperature,salinity&cruise=~%22%28SHIP%2012|Q990046312%29%22" +
            "&longitude%3E=170&time%3E=2012-04-23T00:00:00Z&time%3C=2012-04-24T00:00:00Z",
            EDStatic.fullTestCacheDirectory, "ncCF2b", ".ncCF"); 
        results = NcHelper.dumpString(EDStatic.fullTestCacheDirectory + tName, true);
        //String2.log(results);
        expected = 
"netcdf ncCF2b.nc \\{\n" +
"  dimensions:\n" +
"    trajectory = 2;\n" +
"    profile = 3;\n" +
"    obs = 53;\n" +
"    trajectory_strlen = 21;\n" +
"    platform_strlen = 4;\n" +
"    cruise_strlen = 10;\n" +
"    org_strlen = 2;\n" +
"    type_strlen = 2;\n" +
"  variables:\n" +
"    char trajectory\\(trajectory=2, trajectory_strlen=21\\);\n" +
"      :cf_role = \"trajectory_id\";\n" +
"      :comment = \"Constructed from org_type_platform_cruise\";\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"Trajectory ID\";\n" +
"\n" +                                                                   
"    char platform\\(trajectory=2, platform_strlen=4\\);\n" +
"      :comment = \"See the list of platform codes \\(sorted in various ways\\) at http://www.nodc.noaa.gov/GTSPP/document/codetbls/calllist.html\";\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"GTSPP Platform Code\";\n" +
"      :references = \"http://www.nodc.noaa.gov/gtspp/document/codetbls/callist.html\";\n" +
"\n" +                                                                   
"    char cruise\\(trajectory=2, cruise_strlen=10\\);\n" +
"      :comment = \"Radio callsign \\+ year for real time data, or NODC reference number for delayed mode data.  See\n" +
"http://www.nodc.noaa.gov/GTSPP/document/codetbls/calllist.html .\n" +
"'X' indicates a missing value.\n" +
"Two or more adjacent spaces in the original cruise names have been compacted to 1 space.\";\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"Cruise_ID\";\n" +
"\n" +                                                                   
"    char org\\(trajectory=2, org_strlen=2\\);\n" +
"      :comment = \"From the first 2 characters of stream_ident:\n" +
"Code  Meaning\n" +
"AD  Australian Oceanographic Data Centre\n" +
"AF  Argentina Fisheries \\(Fisheries Research and Development National Institute \\(INIDEP\\), Mar del Plata, Argentina\n" +
"AO  Atlantic Oceanographic and Meteorological Lab\n" +
"AP  Asia-Pacific \\(International Pacific Research Center/ Asia-Pacific Data-Research Center\\)\n" +
"BI  BIO Bedford institute of Oceanography\n" +
"CF  Canadian Navy\n" +
"CS  CSIRO in Australia\n" +
"DA  Dalhousie University\n" +
"FN  FNOC in Monterey, California\n" +
"FR  Orstom, Brest\n" +
"FW  Fresh Water Institute \\(Winnipeg\\)\n" +
"GE  BSH, Germany\n" +
"IC  ICES\n" +
"II  IIP\n" +
"IK  Institut fur Meereskunde, Kiel\n" +
"IM  IML\n" +
"IO  IOS in Pat Bay, BC\n" +
"JA  Japanese Meteorologocal Agency\n" +
"JF  Japan Fisheries Agency\n" +
"ME  EDS\n" +
"MO  Moncton\n" +
"MU  Memorial University\n" +
"NA  NAFC\n" +
"NO  NODC \\(Washington\\)\n" +
"NW  US National Weather Service\n" +
"OD  Old Dominion Univ, USA\n" +
"RU  Russian Federation\n" +
"SA  St Andrews\n" +
"SI  Scripps Institute of Oceanography\n" +
"SO  Southampton Oceanographic Centre, UK\n" +
"TC  TOGA Subsurface Data Centre \\(France\\)\n" +
"TI  Tiberon lab US\n" +
"UB  University of BC\n" +
"UQ  University of Quebec at Rimouski\n" +
"VL  Far Eastern Regional Hydromet. Res. Inst. of V\n" +
"WH  Woods Hole\n" +
"\n" +
"from http://www.nodc.noaa.gov/GTSPP/document/codetbls/gtsppcode.html#ref006\";\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"Organization\";\n" +
"\n" +                                                                   
"    char type\\(trajectory=2, type_strlen=2\\);\n" +
"      :comment = \"From the 3rd and 4th characters of stream_ident:\n" +
"Code  Meaning\n" +
"AR  Animal mounted recorder\n" +
"BA  BATHY message\n" +
"BF  Undulating Oceanographic Recorder \\(e.g. Batfish CTD\\)\n" +
"BO  Bottle\n" +
"BT  general BT data\n" +
"CD  CTD down trace\n" +
"CT  CTD data, up or down\n" +
"CU  CTD up trace\n" +
"DB  Drifting buoy\n" +
"DD  Delayed mode drifting buoy data\n" +
"DM  Delayed mode version from originator\n" +
"DT  Digital BT\n" +
"IC  Ice core\n" +
"ID  Interpolated drifting buoy data\n" +
"IN  Ship intake samples\n" +
"MB  MBT\n" +
"MC  CTD and bottle data are mixed for the station\n" +
"MI  Data from a mixed set of instruments\n" +
"ML  Minilog\n" +
"OF  Real-time oxygen and fluorescence\n" +
"PF  Profiling float\n" +
"RM  Radio message\n" +
"RQ  Radio message with scientific QC\n" +
"SC  Sediment core\n" +
"SG  Thermosalinograph data\n" +
"ST  STD data\n" +
"SV  Sound velocity probe\n" +
"TE  TESAC message\n" +
"TG  Thermograph data\n" +
"TK  TRACKOB message\n" +
"TO  Towed CTD\n" +
"TR  Thermistor chain\n" +
"XB  XBT\n" +
"XC  Expendable CTD\n" +
"\n" +
"from http://www.nodc.noaa.gov/GTSPP/document/codetbls/gtsppcode.html#ref082\";\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"Data Type\";\n" +
"\n" +                                                                   
"    int station_id\\(profile=3\\);\n" +
"      :_FillValue = 2147483647; // int\n" +
"      :actual_range = 13933177, 13968850; // int\n" +
"      :cf_role = \"profile_id\";\n" +
"      :comment = \"Identification number of the station \\(profile\\) in the GTSPP Continuously Managed Database\";\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"Station ID Number\";\n" +
"      :missing_value = 2147483647; // int\n" +
"\n" +                                                                   
"    float longitude\\(profile=3\\);\n" +
"      :_CoordinateAxisType = \"Lon\";\n" +
"      :_FillValue = NaNf; // float\n" +
"      :actual_range = 173.54f, 176.64f; // float\n" +
"      :axis = \"X\";\n" +
"      :C_format = \"%9.4f\";\n" +
"      :colorBarMaximum = 180.0; // double\n" +
"      :colorBarMinimum = -180.0; // double\n" +
"      :epic_code = 502; // int\n" +
"      :FORTRAN_format = \"F9.4\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Longitude\";\n" +
"      :missing_value = NaNf; // float\n" +
"      :standard_name = \"longitude\";\n" +
"      :units = \"degrees_east\";\n" +
"      :valid_max = 180.0f; // float\n" +
"      :valid_min = -180.0f; // float\n" +
"\n" +                                                                   
"    float latitude\\(profile=3\\);\n" +
"      :_CoordinateAxisType = \"Lat\";\n" +
"      :_FillValue = NaNf; // float\n" +
"      :actual_range = -75.45f, -34.58f; // float\n" +
"      :axis = \"Y\";\n" +
"      :C_format = \"%8.4f\";\n" +
"      :colorBarMaximum = 90.0; // double\n" +
"      :colorBarMinimum = -90.0; // double\n" +
"      :epic_code = 500; // int\n" +
"      :FORTRAN_format = \"F8.4\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Latitude\";\n" +
"      :missing_value = NaNf; // float\n" +
"      :standard_name = \"latitude\";\n" +
"      :units = \"degrees_north\";\n" +
"      :valid_max = 90.0f; // float\n" +
"      :valid_min = -90.0f; // float\n" +
"\n" +                                                                   
"    double time\\(profile=3\\);\n" +
"      :_CoordinateAxisType = \"Time\";\n" +
"      :_FillValue = NaN; // double\n" +
"      :actual_range = 1.33514142E9, 1.335216E9; // double\n" +
"      :axis = \"T\";\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Time\";\n" +
"      :missing_value = NaN; // double\n" +
"      :standard_name = \"time\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +                                                                   
"    int trajectoryIndex\\(profile=3\\);\n" +
"      :instance_dimension = \"trajectory\";\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"The trajectory to which this profile is associated.\";\n" +
"\n" +                                                                   
"    int rowSize\\(profile=3\\);\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"Number of Observations for this Profile\";\n" +
"      :sample_dimension = \"obs\";\n" +
"\n" +                                                                   
"    float depth\\(obs=53\\);\n" +
"      :_CoordinateAxisType = \"Height\";\n" +
"      :_CoordinateZisPositive = \"down\";\n" +
"      :_FillValue = NaNf; // float\n" +
"      :actual_range = 4.0f, 368.0f; // float\n" +
"      :axis = \"Z\";\n" +
"      :C_format = \"%6.2f\";\n" +
"      :colorBarMaximum = 5000.0; // double\n" +
"      :colorBarMinimum = 0.0; // double\n" +
"      :epic_code = 3; // int\n" +
"      :FORTRAN_format = \"F6.2\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Depth of the Observations\";\n" +
"      :missing_value = NaNf; // float\n" +
"      :positive = \"down\";\n" +
"      :standard_name = \"depth\";\n" +
"      :units = \"m\";\n" +
"\n" +                                                                   
"    float temperature\\(obs=53\\);\n" +
"      :_FillValue = NaNf; // float\n" +
"      :actual_range = -1.84f, 20.0f; // float\n" +
"      :C_format = \"%9.4f\";\n" +
"      :cell_methods = \"time: point longitude: point latitude: point depth: point\";\n" +
"      :colorBarMaximum = 32.0; // double\n" +
"      :colorBarMinimum = 0.0; // double\n" +
"      :coordinates = \"time latitude longitude depth\";\n" +
"      :epic_code = 28; // int\n" +
"      :FORTRAN_format = \"F9.4\";\n" +
"      :ioos_category = \"Temperature\";\n" +
"      :long_name = \"Sea Water Temperature\";\n" +
"      :missing_value = NaNf; // float\n" +
"      :standard_name = \"sea_water_temperature\";\n" +
"      :units = \"degree_C\";\n" +
"\n" +                                                                   
"    float salinity\\(obs=53\\);\n" +
"      :_FillValue = NaNf; // float\n" +
"      :actual_range = 35.5f, 36.0f; // float\n" +
"      :C_format = \"%9.4f\";\n" +
"      :cell_methods = \"time: point longitude: point latitude: point depth: point\";\n" +
"      :colorBarMaximum = 37.0; // double\n" +
"      :colorBarMinimum = 32.0; // double\n" +
"      :coordinates = \"time latitude longitude depth\";\n" +
"      :epic_code = 41; // int\n" +
"      :FORTRAN_format = \"F9.4\";\n" +
"      :ioos_category = \"Salinity\";\n" +
"      :long_name = \"Practical Salinity\";\n" +
"      :missing_value = NaNf; // float\n" +
"      :salinity_scale = \"PSU\";\n" +
"      :standard_name = \"sea_water_practical_salinity\";\n" +
"      :units = \"PSU\";\n" +
"\n" +                                                                   
"  // global attributes:\n" +
"  :acknowledgment = \"These data were acquired from the US NOAA National Oceanographic Data " +
     "Center \\(NODC\\) on 20.{8} from http://www.nodc.noaa.gov/GTSPP/.\";\n" +  //date changes
"  :cdm_altitude_proxy = \"depth\";\n" +
"  :cdm_data_type = \"TrajectoryProfile\";\n" +
"  :cdm_profile_variables = \"station_id, longitude, latitude, time\";\n" +
"  :cdm_trajectory_variables = \"trajectory, org, type, platform, cruise\";\n" +
"  :Conventions = \"COARDS, WOCE, GTSPP, CF-1.6, ACDD-1.3\";\n" +
"  :creator_email = \"nodc.gtspp@noaa.gov\";\n" +
"  :creator_name = \"NOAA NESDIS NODC \\(IN295\\)\";\n" +
"  :creator_url = \"http://www.nodc.noaa.gov/GTSPP/\";\n" +
"  :crs = \"EPSG:4326\";\n" +
"  :defaultGraphQuery = \"longitude,latitude,station_id&time%3E=201.-.{5}&time%3C=201.-..-01&.draw=markers&.marker=1\\|5\";\n" +
"  :Easternmost_Easting = 176.64f; // float\n" +
"  :featureType = \"TrajectoryProfile\";\n" +
"  :file_source = \"The GTSPP Continuously Managed Data Base\";\n" +
"  :geospatial_lat_max = -34.58f; // float\n" +
"  :geospatial_lat_min = -75.45f; // float\n" +
"  :geospatial_lat_units = \"degrees_north\";\n" +
"  :geospatial_lon_max = 176.64f; // float\n" +
"  :geospatial_lon_min = 173.54f; // float\n" +
"  :geospatial_lon_units = \"degrees_east\";\n" +
"  :geospatial_vertical_max = 368.0f; // float\n" +
"  :geospatial_vertical_min = 4.0f; // float\n" +
"  :geospatial_vertical_positive = \"down\";\n" +
"  :geospatial_vertical_units = \"m\";\n" +
"  :gtspp_ConventionVersion = \"GTSPP4.0\";\n" +
"  :gtspp_handbook_version = \"GTSPP Data User's Manual 1.0\";\n" +
"  :gtspp_program = \"writeGTSPPnc40.f90\";\n" +
"  :gtspp_programVersion = \"1.7\";\n" +  
"  :history = \"20.{8} csun writeGTSPPnc40.f90 Version 1.7\n" + //date changes
".tgz files from ftp.nodc.noaa.gov /pub/gtspp/best_nc/ \\(http://www.nodc.noaa.gov/GTSPP/\\)\n" +
"20.{8} Most recent ingest, clean, and reformat at ERD \\(bob.simons at noaa.gov\\)."; //date changes
        int tPo = results.indexOf("(bob.simons at noaa.gov).");
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureLinesMatch(results.substring(0, tPo + 25), expected, "\nresults=\n" + results);

//today + " (local files)\n" +  //from upwell, so "today" won't be to the second until 1.40 release
//today + " http://upwell.pfeg.noaa.gov/erddap/tabledap/erdGtsppBest.das\n" +
//today + " (local files)\n" +
//today + " http://127.0.0.1:8080/cwexperimental/tabledap/
/* 2013-10-28 I never got summary regex working. Not worth the effort. Comment it out
expected = 
"erdGtsppBest.ncCF\\?platform,cruise,org,type,station_id,longitude,latitude,time,depth,temperature,salinity&cruise=~%22%28SHIP%20%20%20%2012\\|Q990046312%29%22&longitude%3E=170&time%3E=2012-04-23T00:00:00Z&time%3C=2012-04-24T00:00:00Z\";\n" +
"  :id = \"ncCF2b\";\n" +
"  :infoUrl = \"http://www.nodc.noaa.gov/GTSPP/\";\n" +
"  :institution = \"NOAA NODC\";\n" +
"  :keywords = \"Oceans > Ocean Temperature > Water Temperature,\n" +
"Oceans > Salinity/Density > Salinity,\n" +
"cruise, data, density, depth, global, gtspp, identifier, noaa, nodc, observation, ocean, oceans, organization, profile, program, salinity, sea, sea_water_salinity, sea_water_temperature, seawater, station, temperature, temperature-salinity, time, type, water\";\n" +
"  :keywords_vocabulary = \"NODC Data Types, CF Standard Names, GCMD Science Keywords\";\n" +
"  :LEXICON = \"NODC_GTSPP\";\n" +                                                 //date below changes
"  :license = \"These data are openly available to the public.  Please acknowledge the use of these data with:\n" +
"These data were acquired from the US NOAA National Oceanographic Data Center \\(NODC\\) on 20.{8} from http://www.nodc.noaa.gov/GTSPP/.\n" +
"\n" +
"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"  :naming_authority = \"gov.noaa.nodc\";\n" +
"  :Northernmost_Northing = -34.58f; // float\n" +
"  :project = \"Joint IODE/JCOMM Global Temperature-Salinity Profile Programme\";\n" +
"  :references = \"http://www.nodc.noaa.gov/GTSPP/\";\n" +
"  :sourceUrl = \"\\(local files\\)\";\n" +
"  :Southernmost_Northing = -75.45f; // float\n" +
"  :standard_name_vocabulary = \"CF Standard Name Table v29\";\n" +
"  :subsetVariables = \"trajectory, org, type, platform, cruise\";\n" +  
"  :summary = \"The Global Temperature-Salinity Profile Programme \\(GTSPP\\) develops and " +
    "maintains a global ocean temperature and salinity resource with data that are both " +
    "up-to-date and of the highest quality. It is a joint World Meteorological Organization " +
    "\\(WMO\\) and Intergovernmental Oceanographic Commission \\(IOC\\) program.  It includes data " +
    "from XBTs, CTDs, moored and drifting buoys, and PALACE floats. For information about " +
    "organizations contributing data to GTSPP, see http://gosic.org/goos/GTSPP-data-flow.htm .  " +
    "The U.S. National Oceanographic Data Center \\(NODC\\) maintains the GTSPP Continuously " +
    "Managed Data Base and releases new 'best-copy' data once per month.\\+n\\+nWARNING: " +
    "This dataset has a \\*lot\\* of data.  To avoid having your request fail because you are " +
    "requesting too much data at once, you should almost always specify either:\\+n\\* a small " +
    "time bounding box \\(at most, a few days\\), and/or\\+n\\* a small longitude and latitude " +
    "bounding box \\(at most, several degrees square\\).\\+nRequesting data for a specific " +
    "platform, cruise, org, type, and/or station_id may be slow, but it works.\\+n\\+n\\*\\*\\* " +
    "This ERDDAP dataset has data for the entire world for all available times \\(currently, " +
    "up to and including the .{9,16} data\\) but is a subset of the original NODC " + //changes
    "'best-copy' data.  It only includes data where the quality flags indicate the data " +
    "is 1=CORRECT, 2=PROBABLY GOOD, or 5=MODIFIED. It does not include some of the metadata, " +
    "any of the history data, or any of the quality flag data of the original dataset. You " +
    "can always get the complete, up-to-date dataset \\(and additional, near-real-time data\\) " +
    "from the source: http://www.nodc.noaa.gov/GTSPP/ .  Specific differences are:\\+n\\* " +
    "Profiles with a position_quality_flag or a time_quality_flag other than 1\\|2\\|5 were " +
    "removed.\\+n\\* Rows with a depth \\(z\\) value less than -0.4 or greater than 10000 or a " +
    "z_variable_quality_flag other than 1\\|2\\|5 were removed.\\+n\\* Temperature values less " +
    "than -4 or greater than 40 or with a temperature_quality_flag other than 1\\|2\\|5 were " +
    "set to NaN.\\+n\\* Salinity values less than 0 or greater than 41 or with a " +
    "salinity_quality_flag other than 1|2|5 were set to NaN.\\+n\\* Time values were converted " +
    "from \\+\\\"days since 1900-01-01 00:00:00\\+\\\" to \\+\\\"seconds since 1970-01-01" +
    "T00:00:00\\+\\\".\\+n\\+nSee the Quality Flag definitions on page 5 and \\+\\\"Table " +
    "2.1: Global Impossible Parameter Values\\+\\\" on page 61 of\\+nhttp://www.nodc.noaa.gov" +
    "/GTSPP/document/qcmans/GTSPP_RT_QC_Manual_20090916.pdf .\\+nThe Quality Flag definitions " +
    "are also at\\+nhttp://www.nodc.noaa.gov/GTSPP/document/qcmans/qcflags.htm .\";\n" +
*/
expected = 
"  :time_coverage_end = \"2012-04-23T21:20:00Z\";\n" +
"  :time_coverage_start = \"2012-04-23T00:37:00Z\";\n" +
"  :title = \"Global Temperature and Salinity Profile Programme \\(GTSPP\\) Data\";\n" +
"  :Westernmost_Easting = 173.54f; // float\n" +
" data:\n" +
"trajectory =\"ME_BA_9999_SHIP 12\", \"ME_TE_33P2_Q990046312\"\n" +
"platform =\"9999\", \"33P2\"\n" +
"cruise =\"SHIP 12\", \"Q990046312\"\n" +
"org =\"ME\", \"ME\"\n" +
"type =\"BA\", \"TE\"\n" +
"station_id =\n" +
"  \\{13933177, 13968849, 13968850\\}\n" +
"longitude =\n" +
"  \\{173.54, 176.64, 176.64\\}\n" +
"latitude =\n" +
"  \\{-34.58, -75.45, -75.43\\}\n" +
"time =\n" +
"  \\{1.33514142E9, 1.3351446E9, 1.335216E9\\}\n" +
"trajectoryIndex =\n" +
"  \\{0, 1, 1\\}\n" +
"rowSize =\n" +
"  \\{21, 16, 16\\}\n" +
"depth =\n" +
"  \\{4.0, 51.0, 69.0, 89.0, 105.0, 136.0, 143.0, 160.0, 163.0, 173.0, 192.0, 201.0, 209.0, 217.0, 227.0, 239.0, 245.0, 275.0, 294.0, 312.0, 329.0, 4.0, 10.0, 20.0, 30.0, 49.0, 99.0, 138.0, 142.0, 146.0, 148.0, 170.0, 198.0, 247.0, 297.0, 356.0, 366.0, 4.0, 10.0, 20.0, 30.0, 49.0, 99.0, 142.0, 144.0, 148.0, 154.0, 198.0, 208.0, 235.0, 297.0, 328.0, 368.0\\}\n" +
"temperature =\n" +
"  \\{20.0, 19.8, 17.9, 17.4, 16.8, 16.4, 15.8, 15.5, 15.2, 14.7, 14.6, 14.5, 14.1, 14.0, 13.5, 13.4, 13.1, 12.4, 12.2, 12.1, 11.7, -1.84, -1.84, -1.83, -1.83, -1.83, -1.82, -1.78, -1.6, -1.18, -1.1, -1.25, -1.29, -1.13, -1.25, -1.76, -1.8, -1.84, -1.84, -1.84, -1.83, -1.83, -1.82, -1.77, -1.74, -1.36, -1.12, -1.23, -1.07, -1.04, -1.32, -1.65, -1.74\\}\n" +
"salinity =\n" +
"  \\{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, 35.64, 35.64, 35.64, 35.64, 35.64, 35.63, 35.59, 35.52, 35.56, 35.77, 35.81, 35.82, 35.88, 35.94, 35.99, 36.0, 35.64, 35.64, 35.64, 35.64, 35.63, 35.63, 35.61, 35.58, 35.5, 35.77, 35.81, 35.86, 35.9, 35.93, 35.96, 35.98\\}\n" +
"\\}\n";
        tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureLinesMatch(
            results.substring(tPo), expected, "results=\n" + results);

        String2.log("\n*** EDDTableFromNcFiles.testNcCF2b finished.");

    }


    /**
     * Test making an .ncCFMA TrajectoryProfile file (notably with different numbers of 
     * profiles and obs).
     */
    public static void testNcCFMA2b() throws Throwable {

        String2.log("\n*** EDDTableFromNcFiles.testNcCFMA2b");
        EDDTable tedd = (EDDTable)oneFromDatasetsXml(null, "erdGtsppBest"); //should work
        String tName, error, results, expected;
        int po;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
        String query = 
            "platform,cruise,org,type,station_id,longitude,latitude,time,depth," +
            "temperature,salinity&cruise=~%22%28SHIP%2012|Q990046312%29%22" +
            "&longitude%3E=170&time%3E=2012-04-23T00:00:00Z&time%3C=2012-04-24T00:00:00Z";

        //lon lat time range 
        tName = tedd.makeNewFileForDapQuery(null, null, query,
            EDStatic.fullTestCacheDirectory, "ncCFMA2b", ".ncCFMA"); 
        results = NcHelper.dumpString(EDStatic.fullTestCacheDirectory + tName, true);
        //String2.log(results);
        expected = 
"netcdf ncCFMA2b.nc \\{\n" +
"  dimensions:\n" +
"    trajectory = 2;\n" +
"    profile = 2;\n" +
"    obs = 21;\n" +
"    trajectory_strlen = 21;\n" +
"    platform_strlen = 4;\n" +
"    cruise_strlen = 10;\n" +
"    org_strlen = 2;\n" +
"    type_strlen = 2;\n" +
"  variables:\n" +
"    char trajectory\\(trajectory=2, trajectory_strlen=21\\);\n" +
"      :cf_role = \"trajectory_id\";\n" +
"      :comment = \"Constructed from org_type_platform_cruise\";\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"Trajectory ID\";\n" +
"\n" +
"    char platform\\(trajectory=2, platform_strlen=4\\);\n" +
"      :comment = \"See the list of platform codes \\(sorted in various ways\\) at http://www.nodc.noaa.gov/GTSPP/document/codetbls/calllist.html\";\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"GTSPP Platform Code\";\n" +
"      :references = \"http://www.nodc.noaa.gov/gtspp/document/codetbls/callist.html\";\n" +
"\n" +
"    char cruise\\(trajectory=2, cruise_strlen=10\\);\n" +
"      :comment = \"Radio callsign \\+ year for real time data, or NODC reference number for delayed mode data.  See\n" +
"http://www.nodc.noaa.gov/GTSPP/document/codetbls/calllist.html .\n" +
"'X' indicates a missing value.\n" +
"Two or more adjacent spaces in the original cruise names have been compacted to 1 space.\";\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"Cruise_ID\";\n" +
"\n" +
"    char org\\(trajectory=2, org_strlen=2\\);\n" +
"      :comment = \"From the first 2 characters of stream_ident:\n" +
"Code  Meaning\n" +
"AD  Australian Oceanographic Data Centre\n" +
"AF  Argentina Fisheries \\(Fisheries Research and Development National Institute \\(INIDEP\\), Mar del Plata, Argentina\n" +
"AO  Atlantic Oceanographic and Meteorological Lab\n" +
"AP  Asia-Pacific \\(International Pacific Research Center/ Asia-Pacific Data-Research Center\\)\n" +
"BI  BIO Bedford institute of Oceanography\n" +
"CF  Canadian Navy\n" +
"CS  CSIRO in Australia\n" +
"DA  Dalhousie University\n" +
"FN  FNOC in Monterey, California\n" +
"FR  Orstom, Brest\n" +
"FW  Fresh Water Institute \\(Winnipeg\\)\n" +
"GE  BSH, Germany\n" +
"IC  ICES\n" +
"II  IIP\n" +
"IK  Institut fur Meereskunde, Kiel\n" +
"IM  IML\n" +
"IO  IOS in Pat Bay, BC\n" +
"JA  Japanese Meteorologocal Agency\n" +
"JF  Japan Fisheries Agency\n" +
"ME  EDS\n" +
"MO  Moncton\n" +
"MU  Memorial University\n" +
"NA  NAFC\n" +
"NO  NODC \\(Washington\\)\n" +
"NW  US National Weather Service\n" +
"OD  Old Dominion Univ, USA\n" +
"RU  Russian Federation\n" +
"SA  St Andrews\n" +
"SI  Scripps Institute of Oceanography\n" +
"SO  Southampton Oceanographic Centre, UK\n" +
"TC  TOGA Subsurface Data Centre \\(France\\)\n" +
"TI  Tiberon lab US\n" +
"UB  University of BC\n" +
"UQ  University of Quebec at Rimouski\n" +
"VL  Far Eastern Regional Hydromet. Res. Inst. of V\n" +
"WH  Woods Hole\n" +
"\n" +
"from http://www.nodc.noaa.gov/GTSPP/document/codetbls/gtsppcode.html#ref006\";\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"Organization\";\n" +
"\n" +
"    char type\\(trajectory=2, type_strlen=2\\);\n" +
"      :comment = \"From the 3rd and 4th characters of stream_ident:\n" +
"Code  Meaning\n" +
"AR  Animal mounted recorder\n" +
"BA  BATHY message\n" +
"BF  Undulating Oceanographic Recorder \\(e.g. Batfish CTD\\)\n" +
"BO  Bottle\n" +
"BT  general BT data\n" +
"CD  CTD down trace\n" +
"CT  CTD data, up or down\n" +
"CU  CTD up trace\n" +
"DB  Drifting buoy\n" +
"DD  Delayed mode drifting buoy data\n" +
"DM  Delayed mode version from originator\n" +
"DT  Digital BT\n" +
"IC  Ice core\n" +
"ID  Interpolated drifting buoy data\n" +
"IN  Ship intake samples\n" +
"MB  MBT\n" +
"MC  CTD and bottle data are mixed for the station\n" +
"MI  Data from a mixed set of instruments\n" +
"ML  Minilog\n" +
"OF  Real-time oxygen and fluorescence\n" +
"PF  Profiling float\n" +
"RM  Radio message\n" +
"RQ  Radio message with scientific QC\n" +
"SC  Sediment core\n" +
"SG  Thermosalinograph data\n" +
"ST  STD data\n" +
"SV  Sound velocity probe\n" +
"TE  TESAC message\n" +
"TG  Thermograph data\n" +
"TK  TRACKOB message\n" +
"TO  Towed CTD\n" +
"TR  Thermistor chain\n" +
"XB  XBT\n" +
"XC  Expendable CTD\n" +
"\n" +
"from http://www.nodc.noaa.gov/GTSPP/document/codetbls/gtsppcode.html#ref082\";\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"Data Type\";\n" +
"\n" +
"    int station_id\\(trajectory=2, profile=2\\);\n" +
"      :_FillValue = 2147483647; // int\n" +
"      :actual_range = 13933177, 13968850; // int\n" +
"      :cf_role = \"profile_id\";\n" +
"      :comment = \"Identification number of the station \\(profile\\) in the GTSPP Continuously Managed Database\";\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"Station ID Number\";\n" +
"      :missing_value = 2147483647; // int\n" +
"\n" +
"    float longitude\\(trajectory=2, profile=2\\);\n" +
"      :_CoordinateAxisType = \"Lon\";\n" +
"      :_FillValue = NaNf; // float\n" +
"      :actual_range = 173.54f, 176.64f; // float\n" +
"      :axis = \"X\";\n" +
"      :C_format = \"%9.4f\";\n" +
"      :colorBarMaximum = 180.0; // double\n" +
"      :colorBarMinimum = -180.0; // double\n" +
"      :epic_code = 502; // int\n" +
"      :FORTRAN_format = \"F9.4\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Longitude\";\n" +
"      :missing_value = NaNf; // float\n" +
"      :standard_name = \"longitude\";\n" +
"      :units = \"degrees_east\";\n" +
"      :valid_max = 180.0f; // float\n" +
"      :valid_min = -180.0f; // float\n" +
"\n" +
"    float latitude\\(trajectory=2, profile=2\\);\n" +
"      :_CoordinateAxisType = \"Lat\";\n" +
"      :_FillValue = NaNf; // float\n" +
"      :actual_range = -75.45f, -34.58f; // float\n" +
"      :axis = \"Y\";\n" +
"      :C_format = \"%8.4f\";\n" +
"      :colorBarMaximum = 90.0; // double\n" +
"      :colorBarMinimum = -90.0; // double\n" +
"      :epic_code = 500; // int\n" +
"      :FORTRAN_format = \"F8.4\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Latitude\";\n" +
"      :missing_value = NaNf; // float\n" +
"      :standard_name = \"latitude\";\n" +
"      :units = \"degrees_north\";\n" +
"      :valid_max = 90.0f; // float\n" +
"      :valid_min = -90.0f; // float\n" +
"\n" +
"    double time\\(trajectory=2, profile=2\\);\n" +
"      :_CoordinateAxisType = \"Time\";\n" +
"      :_FillValue = NaN; // double\n" +
"      :actual_range = 1.33514142E9, 1.335216E9; // double\n" +
"      :axis = \"T\";\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Time\";\n" +
"      :missing_value = NaN; // double\n" +
"      :standard_name = \"time\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    float depth\\(trajectory=2, profile=2, obs=21\\);\n" +
"      :_CoordinateAxisType = \"Height\";\n" +
"      :_CoordinateZisPositive = \"down\";\n" +
"      :_FillValue = NaNf; // float\n" +
"      :actual_range = 4.0f, 368.0f; // float\n" +
"      :axis = \"Z\";\n" +
"      :C_format = \"%6.2f\";\n" +
"      :colorBarMaximum = 5000.0; // double\n" +
"      :colorBarMinimum = 0.0; // double\n" +
"      :epic_code = 3; // int\n" +
"      :FORTRAN_format = \"F6.2\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Depth of the Observations\";\n" +
"      :missing_value = NaNf; // float\n" +
"      :positive = \"down\";\n" +
"      :standard_name = \"depth\";\n" +
"      :units = \"m\";\n" +
"\n" +
"    float temperature\\(trajectory=2, profile=2, obs=21\\);\n" +
"      :_FillValue = NaNf; // float\n" +
"      :actual_range = -1.84f, 20.0f; // float\n" +
"      :C_format = \"%9.4f\";\n" +
"      :cell_methods = \"time: point longitude: point latitude: point depth: point\";\n" +
"      :colorBarMaximum = 32.0; // double\n" +
"      :colorBarMinimum = 0.0; // double\n" +
"      :coordinates = \"time latitude longitude depth\";\n" +
"      :epic_code = 28; // int\n" +
"      :FORTRAN_format = \"F9.4\";\n" +
"      :ioos_category = \"Temperature\";\n" +
"      :long_name = \"Sea Water Temperature\";\n" +
"      :missing_value = NaNf; // float\n" +
"      :standard_name = \"sea_water_temperature\";\n" +
"      :units = \"degree_C\";\n" +
"\n" +
"    float salinity\\(trajectory=2, profile=2, obs=21\\);\n" +
"      :_FillValue = NaNf; // float\n" +
"      :actual_range = 35.5f, 36.0f; // float\n" +
"      :C_format = \"%9.4f\";\n" +
"      :cell_methods = \"time: point longitude: point latitude: point depth: point\";\n" +
"      :colorBarMaximum = 37.0; // double\n" +
"      :colorBarMinimum = 32.0; // double\n" +
"      :coordinates = \"time latitude longitude depth\";\n" +
"      :epic_code = 41; // int\n" +
"      :FORTRAN_format = \"F9.4\";\n" +
"      :ioos_category = \"Salinity\";\n" +
"      :long_name = \"Practical Salinity\";\n" +
"      :missing_value = NaNf; // float\n" +
"      :salinity_scale = \"PSU\";\n" +
"      :standard_name = \"sea_water_practical_salinity\";\n" +
"      :units = \"PSU\";\n" +
"\n" +
"  // global attributes:\n" +
"  :acknowledgment = \"These data were acquired from the US NOAA National Oceanographic " +
     "Data Center \\(NODC\\) on 20.{8} from http://www.nodc.noaa.gov/GTSPP/.\";\n" +
"  :cdm_altitude_proxy = \"depth\";\n" +
"  :cdm_data_type = \"TrajectoryProfile\";\n" +
"  :cdm_profile_variables = \"station_id, longitude, latitude, time\";\n" +
"  :cdm_trajectory_variables = \"trajectory, org, type, platform, cruise\";\n" +
"  :Conventions = \"COARDS, WOCE, GTSPP, CF-1.6, ACDD-1.3\";\n" +
"  :creator_email = \"nodc.gtspp@noaa.gov\";\n" +
"  :creator_name = \"NOAA NESDIS NODC \\(IN295\\)\";\n" +
"  :creator_url = \"http://www.nodc.noaa.gov/GTSPP/\";\n" +
"  :crs = \"EPSG:4326\";\n" +
"  :defaultGraphQuery = \"longitude,latitude,station_id&time%3E=201.-..-..&time%3C=201.-..-01&.draw=markers&.marker=1\\|5\";\n" +
"  :Easternmost_Easting = 176.64f; // float\n" +
"  :featureType = \"TrajectoryProfile\";\n" +
"  :file_source = \"The GTSPP Continuously Managed Data Base\";\n" +
"  :geospatial_lat_max = -34.58f; // float\n" +
"  :geospatial_lat_min = -75.45f; // float\n" +
"  :geospatial_lat_units = \"degrees_north\";\n" +
"  :geospatial_lon_max = 176.64f; // float\n" +
"  :geospatial_lon_min = 173.54f; // float\n" +
"  :geospatial_lon_units = \"degrees_east\";\n" +
"  :geospatial_vertical_max = 368.0f; // float\n" +
"  :geospatial_vertical_min = 4.0f; // float\n" +
"  :geospatial_vertical_positive = \"down\";\n" +
"  :geospatial_vertical_units = \"m\";\n" +
"  :gtspp_ConventionVersion = \"GTSPP4.0\";\n" +
"  :gtspp_handbook_version = \"GTSPP Data User's Manual 1.0\";\n" +
"  :gtspp_program = \"writeGTSPPnc40.f90\";\n" +
"  :gtspp_programVersion = \"1.7\";\n" +
"  :history = \"20.{8} csun writeGTSPPnc40.f90 Version 1.7\n" +
".tgz files from ftp.nodc.noaa.gov /pub/gtspp/best_nc/ \\(http://www.nodc.noaa.gov/GTSPP/\\)\n" +
"20.{8} Most recent ingest, clean, and reformat at ERD \\(bob.simons at noaa.gov\\).";  //date changes
        int tPo = results.indexOf("(bob.simons at noaa.gov).");
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureLinesMatch(results.substring(0, tPo + 25), expected, "\nresults=\n" + results);

//today + " (local files)\n" +  //from upwell, so time not to seconds until ver 1.40
//today + " http://upwell.pfeg.noaa.gov/erddap/tabledap/erdGtsppBest.das\n" +
//today + " (local files)\n" +
//today + " http://127.0.0.1:8080/cwexperimental/tabledap/
/* 2013-10-28 Too hard to get summary converted to regex. So skip testing it.
String expected2 = 
"erdGtsppBest.ncCFMA?platform,cruise,org,type,station_id,longitude,latitude,time,depth,temperature,salinity&cruise=~%22%28SHIP%20%20%20%2012|Q990046312%29%22&longitude%3E=170&time%3E=2012-04-23T00:00:00Z&time%3C=2012-04-24T00:00:00Z\";\n" +
"  :id = \"ncCFMA2b\";\n" +
"  :infoUrl = \"http://www.nodc.noaa.gov/GTSPP/\";\n" +
"  :institution = \"NOAA NODC\";\n" +
"  :keywords = \"Oceans > Ocean Temperature > Water Temperature,\n" +
"Oceans > Salinity/Density > Salinity,\n" +
"cruise, data, density, depth, global, gtspp, identifier, noaa, nodc, observation, ocean, oceans, organization, profile, program, salinity, sea, sea_water_salinity, sea_water_temperature, seawater, station, temperature, temperature-salinity, time, type, water\";\n" +
"  :keywords_vocabulary = \"NODC Data Types, CF Standard Names, GCMD Science Keywords\";\n" +
"  :LEXICON = \"NODC_GTSPP\";\n" +                                                //date below changes
"  :license = \"These data are openly available to the public.  Please acknowledge the use of these data with:\n" +
"These data were acquired from the US NOAA National Oceanographic Data Center (NODC) on 2013-06-13 from http://www.nodc.noaa.gov/GTSPP/.\n" +
"\n" +
"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"  :naming_authority = \"gov.noaa.nodc\";\n" +
"  :Northernmost_Northing = -34.58f; // float\n" +
"  :project = \"Joint IODE/JCOMM Global Temperature-Salinity Profile Programme\";\n" +
"  :references = \"http://www.nodc.noaa.gov/GTSPP/\";\n" +
"  :sourceUrl = \"(local files)\";\n" +
"  :Southernmost_Northing = -75.45f; // float\n" +
"  :standard_name_vocabulary = \"CF Standard Name Table v29\";\n" +
"  :subsetVariables = \"platform, cruise, org, type\";\n" +                   
"  :summary = \"The Global Temperature-Salinity Profile Programme (GTSPP) develops and maintains " +
    "a global ocean temperature and salinity resource with data that are both up-to-date and of " +
    "the highest quality. It is a joint World Meteorological Organization (WMO) and Intergovernmental " +
    "Oceanographic Commission (IOC) program.  It includes data from XBTs, CTDs, moored and drifting " +
    "buoys, and PALACE floats. For information about organizations contributing data to GTSPP, see " +
    "http://gosic.org/goos/GTSPP-data-flow.htm .  The U.S. National Oceanographic Data Center " +
    "(NODC) maintains the GTSPP Continuously Managed Data Base and releases new 'best-copy' " +
    "data once per month.\\\\n\\\\nWARNING: This dataset has a *lot* of data.  To avoid having " +
    "your request fail because you are requesting too much data at once, you should almost " +
    "always specify either:\\\\n* a small time bounding box (at most, a few days), and/or" +
    "\\\\n* a small longitude and latitude bounding box (at most, several degrees square)." +
    "\\\\nRequesting data for a specific platform, cruise, org, type, and/or station_id may " +
    "be slow, but it works.\\\\n\\\\n*** This ERDDAP dataset has data for the entire world for " +
    "all available times (currently, up to and including the May 2013 data) but is a " + //date changes
    "subset of the original NODC 'best-copy' data.  It only includes data where the quality " +
    "flags indicate the data is 1=CORRECT, 2=PROBABLY GOOD, or 5=MODIFIED. It does not include " +
    "some of the metadata, any of the history data, or any of the quality flag data of the " +
    "original dataset. You can always get the complete, up-to-date dataset (and additional, " +
    "near-real-time data) from the source: http://www.nodc.noaa.gov/GTSPP/ .  Specific " +
    "differences are:\\\\n* Profiles with a position_quality_flag or a time_quality_flag other " +
    "than 1|2|5 were removed.\\\\n* Rows with a depth (z) value less than -0.4 or greater than " +
    "10000 or a z_variable_quality_flag other than 1|2|5 were removed.\\\\n* Temperature values " +
    "less than -4 or greater than 40 or with a temperature_quality_flag other than 1|2|5 were " +
    "set to NaN.\\\\n* Salinity values less than 0 or greater than 41 or with a " +
    "salinity_quality_flag other than 1|2|5 were set to NaN.\\\\n* Time values were converted from " +
    "\\\\\\\"days since 1900-01-01 00:00:00\\\\\\\" to \\\\\\\"seconds since 1970-01-01T00:00:00" +
    "\\\\\\\".\\\\n\\\\nSee the Quality Flag definitions on page 5 and \\\\\\\"Table 2.1: Global " +
    "Impossible Parameter Values\\\\\\\" on page 61 of" +
    "\\\\nhttp://www.nodc.noaa.gov/GTSPP/document/qcmans/GTSPP_RT_QC_Manual_20090916.pdf ." +
    "\\\\nThe Quality Flag definitions are also at" +
    "\\\\nhttp://www.nodc.noaa.gov/GTSPP/document/qcmans/qcflags.htm .\";\n" +
*/
String expected2 = 
"  :time_coverage_end = \"2012-04-23T21:20:00Z\";\n" +
"  :time_coverage_start = \"2012-04-23T00:37:00Z\";\n" +
"  :title = \"Global Temperature and Salinity Profile Programme (GTSPP) Data\";\n" +
"  :Westernmost_Easting = 173.54f; // float\n" +
" data:\n";
        tPo = results.indexOf(expected2.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, tPo + expected2.length()),
                expected2, "results=\n" + results);

String expected3 = expected2 + 
"trajectory =\"ME_BA_9999_SHIP 12\", \"ME_TE_33P2_Q990046312\"\n" +
"platform =\"9999\", \"33P2\"\n" +
"cruise =\"SHIP 12\", \"Q990046312\"\n" +
"org =\"ME\", \"ME\"\n" +
"type =\"BA\", \"TE\"\n" +
"station_id =\n" +
"  {\n" +
"    {13933177, 2147483647},\n" +
"    {13968849, 13968850}\n" +
"  }\n" +
"longitude =\n" +
"  {\n" +
"    {173.54, NaN},\n" +
"    {176.64, 176.64}\n" +
"  }\n" +
"latitude =\n" +
"  {\n" +
"    {-34.58, NaN},\n" +
"    {-75.45, -75.43}\n" +
"  }\n" +
"time =\n" +
"  {\n" +
"    {1.33514142E9, NaN},\n" +
"    {1.3351446E9, 1.335216E9}\n" +
"  }\n" +
"depth =\n" +
"  {\n" +
"    {\n" +
"      {4.0, 51.0, 69.0, 89.0, 105.0, 136.0, 143.0, 160.0, 163.0, 173.0, 192.0, 201.0, 209.0, 217.0, 227.0, 239.0, 245.0, 275.0, 294.0, 312.0, 329.0},\n" +
"      {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}\n" +
"    },\n" +
"    {\n" +
"      {4.0, 10.0, 20.0, 30.0, 49.0, 99.0, 138.0, 142.0, 146.0, 148.0, 170.0, 198.0, 247.0, 297.0, 356.0, 366.0, NaN, NaN, NaN, NaN, NaN},\n" +
"      {4.0, 10.0, 20.0, 30.0, 49.0, 99.0, 142.0, 144.0, 148.0, 154.0, 198.0, 208.0, 235.0, 297.0, 328.0, 368.0, NaN, NaN, NaN, NaN, NaN}\n" +
"    }\n" +
"  }\n" +
"temperature =\n" +
"  {\n" +
"    {\n" +
"      {20.0, 19.8, 17.9, 17.4, 16.8, 16.4, 15.8, 15.5, 15.2, 14.7, 14.6, 14.5, 14.1, 14.0, 13.5, 13.4, 13.1, 12.4, 12.2, 12.1, 11.7},\n" +
"      {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}\n" +
"    },\n" +
"    {\n" +
"      {-1.84, -1.84, -1.83, -1.83, -1.83, -1.82, -1.78, -1.6, -1.18, -1.1, -1.25, -1.29, -1.13, -1.25, -1.76, -1.8, NaN, NaN, NaN, NaN, NaN},\n" +
"      {-1.84, -1.84, -1.84, -1.83, -1.83, -1.82, -1.77, -1.74, -1.36, -1.12, -1.23, -1.07, -1.04, -1.32, -1.65, -1.74, NaN, NaN, NaN, NaN, NaN}\n" +
"    }\n" +
"  }\n" +
"salinity =\n" +
"  {\n" +
"    {\n" +
"      {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN},\n" +
"      {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}\n" +
"    },\n" +
"    {\n" +
"      {35.64, 35.64, 35.64, 35.64, 35.64, 35.63, 35.59, 35.52, 35.56, 35.77, 35.81, 35.82, 35.88, 35.94, 35.99, 36.0, NaN, NaN, NaN, NaN, NaN},\n" +
"      {35.64, 35.64, 35.64, 35.64, 35.63, 35.63, 35.61, 35.58, 35.5, 35.77, 35.81, 35.86, 35.9, 35.93, 35.96, 35.98, NaN, NaN, NaN, NaN, NaN}\n" +
"    }\n" +
"  }\n" +
"}\n";
        tPo = results.indexOf(expected3.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected3.length())),
            expected3, "results=\n" + results);

        //.ncCFMAHeader
        tName = tedd.makeNewFileForDapQuery(null, null, query,
            EDStatic.fullTestCacheDirectory, "ncCFMA2b", ".ncCFMAHeader"); 
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
        tPo = results.indexOf("(bob.simons at noaa.gov).");
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureLinesMatch(results.substring(0, tPo + 25), expected, "\nresults=\n" + results);

        expected3 = expected2 + "}\n";
        tPo = results.indexOf(expected3.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected3.length())),
            expected3, "results=\n" + results);

        String2.log("\n*** EDDTableFromNcFiles.testNcCFMA2b finished.");
    }


    /** Test speed of Data Access Form.
     *  Sometimes: use this with profiler: -agentlib:hprof=cpu=samples,depth=20,file=/JavaHeap.txt   
     */
    public static void testSpeedDAF() throws Throwable {
        //setup and warmup
        EDD.testVerbose(false);
        EDDTable tableDataset = (EDDTable)oneFromDatasetsXml(null, "cwwcNDBCMet"); 
        String fileName = EDStatic.fullTestCacheDirectory + "tableTestSpeedDAF.txt";
        Writer writer = new FileWriter(fileName);
        tableDataset.writeDapHtmlForm(null, "", writer);

        //time it DAF
        String2.log("start timing"); 
        long time = System.currentTimeMillis();
        int n = 100;  //use 1000 so it dominates program run time if profiling
        for (int i = 0; i < n; i++)  
            tableDataset.writeDapHtmlForm(null, "", writer);
        double results = ((System.currentTimeMillis() - time) / (float)n);
        double expected = 5.31;
        String2.log("\nEDDTableFromNcFiles.testSpeedDAF time per .html = " +
            results + "ms (java 1.7M4700 " + expected + "ms, 1.6 14.6ms, 1.5 40.8ms)\n" +  //slow because of info for sliders and subset variables
            "  outputFileName=" + fileName);
        if (results > expected * 2)
            String2.pressEnterToContinue(String2.beep(1) + "\nSlow."); 

        EDD.testVerbose(true);
    }

    /** Test speed of Make A Graph Form.
     *  Sometimes: use this with profiler: -agentlib:hprof=cpu=samples,depth=20,file=/JavaHeap.txt   
     */
    public static void testSpeedMAG() throws Throwable {
        //setup and warmup
        EDD.testVerbose(false);
        EDDTable tableDataset = (EDDTable)oneFromDatasetsXml(null, "cwwcNDBCMet"); 
        String fileName = EDStatic.fullTestCacheDirectory + "tableTestSpeedMAG.txt";
        String2.log("fileName=" + fileName);
        OutputStreamSource oss = new OutputStreamSourceSimple(new FileOutputStream(fileName));
        tableDataset.respondToGraphQuery(null, null, "", "", oss, null, null, null);

        //time it 
        String2.log("start timing");
        long time2 = System.currentTimeMillis();
        int n = 100; //1000 so it dominates program run time if profiling
        for (int i = 0; i < n; i++) 
            tableDataset.respondToGraphQuery(null, null, "", "", oss,
                EDStatic.fullTestCacheDirectory, "testSpeedMAG.txt", ".graph");
        double observe = (System.currentTimeMillis() - time2) / (float)n;
        double expect = 8; //2014-09 java 1.7 was 4.38ms, java 1.6 10.7ms, java 1.5 55.172ms
        String2.log("\nEDDTableFromNcFiles.testSpeedMAG time per .graph = " +
            observe + 
            "ms (java 1.7M4700 " + expect + "ms)\n" + //slow because of info for sliders and subset variables
            "  outputFileName=" + fileName);
        if (observe > 1.5 * expect) 
            String2.pressEnterToContinue("Too slow!");
        EDD.testVerbose(true);
    }

    /** Test speed of Subset Form.
     *  Sometimes: use this with profiler: -agentlib:hprof=cpu=samples,depth=20,file=/JavaHeap.txt   
     */
    public static void testSpeedSubset() throws Throwable {
        //setup and warmup
        EDD.testVerbose(false);
        EDDTable tableDataset = (EDDTable)oneFromDatasetsXml(null, "cwwcNDBCMet"); 
        String fileName = EDStatic.fullTestCacheDirectory + "tableTestSpeedSubset.txt";
        String2.log("fileName=" + fileName);
        OutputStreamSource oss = new OutputStreamSourceSimple(new FileOutputStream(fileName));
        tableDataset.respondToGraphQuery(null, null, "", "", oss,
            EDStatic.fullTestCacheDirectory, "testSpeedSubset.txt", ".graph");

        //time it 
        String2.log("start timing");
        long time2 = System.currentTimeMillis();
        int n = 100;  //use 1000 so it dominates program run time if profiling
        for (int i = 0; i < n; i++) 
            tableDataset.respondToGraphQuery(null, null, "", "", oss,
                EDStatic.fullTestCacheDirectory, "testSpeedSubset.txt", ".graph");

        double observe = (System.currentTimeMillis() - time2) / (float)n;
        double expect = 4.23; //2013-10-28 ~10ms   
        String2.log("\nEDDTableFromNcFiles.testSpeedSubset time per .graph = " +
            observe + 
            "ms (java 1.7M4700 " + expect + "ms, 1.6 17.36ms)\n" +
            "  outputFileName=" + fileName);
        if (observe > 1.5 * expect) 
            String2.pressEnterToContinue("Too slow!");
        EDD.testVerbose(true);
    }

    /**
     * Test requesting float=NaN.
     */
    public static void testEqualsNaN() throws Throwable {

        String2.log("\n*** EDDTableFromNcFiles.testEqualsNaN");
        EDDTable tedd = (EDDTable)oneFromDatasetsXml(null, "erdGtsppBest"); 
        String tName, error, results, expected;

        //lon lat time range 
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "&trajectory=%22ME_BA_067F_66021%2014%22&time=2014-01-01T00:00:00Z&salinity=NaN",
            EDStatic.fullTestCacheDirectory, "equalsNaN", ".csv"); 
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
        //String2.log(results);
        expected = 
"trajectory,org,type,platform,cruise,station_id,longitude,latitude,time,depth,temperature,salinity\n" +
",,,,,,degrees_east,degrees_north,UTC,m,degree_C,PSU\n" +
"ME_BA_067F_66021 14,ME,BA,067F,66021 14,18867634,13.92,54.88,2014-01-01T00:00:00Z,2.0,5.2,NaN\n" +
"ME_BA_067F_66021 14,ME,BA,067F,66021 14,18867634,13.92,54.88,2014-01-01T00:00:00Z,5.0,5.2,NaN\n" +
"ME_BA_067F_66021 14,ME,BA,067F,66021 14,18867634,13.92,54.88,2014-01-01T00:00:00Z,7.0,5.2,NaN\n" +
"ME_BA_067F_66021 14,ME,BA,067F,66021 14,18867634,13.92,54.88,2014-01-01T00:00:00Z,25.0,5.1,NaN\n" +
"ME_BA_067F_66021 14,ME,BA,067F,66021 14,18867634,13.92,54.88,2014-01-01T00:00:00Z,40.0,8.0,NaN\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

        String2.log("\n*** EDDTableFromNcFiles.testEqualsNaN finished.");
    }


    /**
     * Test requesting altitude=-2.
     * This tests fix of bug where EDDTable.getSourceQueryFromDapQuery didn't use
     * scale_factor to convert altitude constraints to source units (e.g., /-1)!
     * 
     * <p>And this tests altitude&gt;= should become depth&lt;= internally. (and related)
     */
    public static void testAltitude() throws Throwable {

        String2.log("\n*** EDDTableFromNcFiles.testAltitude");

        //tests of REVERSED_OPERATOR
        EDDTable tedd; 
        String tName, error, results, expected;

        tedd = (EDDTable)oneFromDatasetsXml(null, "erdCinpKfmT"); 
        expected = 
"station,longitude,latitude,depth,time,temperature\n" +
",degrees_east,degrees_north,m,UTC,degree_C\n" +
"Santa_Rosa_Johnsons_Lee_North,-120.1,33.883335,11,2007-09-26T22:13:00Z,16.38\n" +
"Santa_Rosa_Johnsons_Lee_North,-120.1,33.883335,11,2007-09-26T23:13:00Z,16.7\n";

        // >= 
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "&depth<=17&time>=2007-09-26T22",  //what we want to work
            EDStatic.fullTestCacheDirectory, "depth", ".csv"); 
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

        // > 
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "&depth<18&time>=2007-09-26T22",  //what we want to work
            EDStatic.fullTestCacheDirectory, "depth", ".csv"); 
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

        // <= 
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "&depth<=11&time>=2007-09-26T22",  //what we want to work
            EDStatic.fullTestCacheDirectory, "depth", ".csv"); 
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

        // <
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "&depth>10.9&time>=2007-09-26T22",  //what we want to work
            EDStatic.fullTestCacheDirectory, "depth", ".csv"); 
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

        // =
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "&depth=11&time>=2007-09-26T22",  //what we want to work
            EDStatic.fullTestCacheDirectory, "depth", ".csv"); 
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

        // !=
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "&depth!=10&time>=2007-09-26T22",  //what we want to work
            EDStatic.fullTestCacheDirectory, "depth", ".csv"); 
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

        // =~
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "&depth=~\"(1000|11)\"&time>=2007-09-26T22",  //what we want to work
            EDStatic.fullTestCacheDirectory, "depth", ".csv"); 
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);



        //*** original test
        tedd = (EDDTable)oneFromDatasetsXml(null, "epaseamapTimeSeriesProfiles"); 

        //lon lat time range 
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "&depth=2",  //what we want to work
            EDStatic.fullTestCacheDirectory, "depth", ".csv"); 
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
        //String2.log(results);
        expected = 
"station_name,station,latitude,longitude,time,depth,WaterTemperature,salinity,chlorophyll,Nitrogen,Phosphate,Ammonium\n" +
",,degrees_north,degrees_east,UTC,m,Celsius,psu,mg_m-3,percent,percent,percent\n" +
"ED16,1,29.728,-88.00584,2004-06-08T18:00:00Z,2.0,27.2501,34.843,NaN,NaN,NaN,NaN\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

        String2.log("\n*** EDDTableFromNcFiles.testAltitude finished.");

    }


    /** These tests an odd response related to missing values. */
    public static void testMV() throws Throwable {
        String2.log("\n*** EDDTableFromNcFiles.testMV\n" +
            "NaN<=-5 = " + (Double.NaN <= -5) + " = " + PrimitiveArray.testValueOpValue(Double.NaN, "<=" , -5));
        String name, tName, results, tResults, expected, dapQuery;
        String error = "";
        try {
            EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, "cwwcNDBCMet"); 
            String baseName = eddTable.className() + "TestMV";
            dapQuery = "station,longitude,latitude,time,wtmp&station<=\"41024\"&time>=2012-06-20T00:00:00Z&time<=2012-06-20T02:00:00Z&wtmp<=-5";

            tName = eddTable.makeNewFileForDapQuery(null, null, dapQuery, 
                EDStatic.fullTestCacheDirectory, baseName, ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            String2.log(results);
            expected = "Shouldn't get here!";  
            Test.ensureEqual(results, expected, "");
            String2.pressEnterToContinue("\nWrong! It shouldn't reply with NaN's."); 
 
        } catch (Throwable t) {
            String msg = t.toString();
            if (msg.indexOf(MustBe.THERE_IS_NO_DATA) < 0) 
                String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                    "\nUnexpected error.   expected=" + MustBe.THERE_IS_NO_DATA); 
        }
        
    } //end of testMV

    /** 
     * This tests if e.g., long_names are consistent for several variables
     * for all the files in a collection (e.g., an fsuResearch ship).
     *
     * @param regex e.g., ".*\\.nc"
     * @param vars e.g., ["DIR2", "T2", "RAIN2"]
     * @param attribute e.g., long_name
     */
    public static void displayAttributeFromFiles(String dir, String regex, 
        String vars[], String attribute) {
        ArrayList arrayList = new ArrayList();
        RegexFilenameFilter.recursiveFullNameList(arrayList, dir, 
            regex, true); //recursive?
        Table table = new Table();
        Tally tally = new Tally();
        for (int i = 0; i < arrayList.size(); i++) {
            table.clear();
            try {
                table.readNDNc((String)arrayList.get(i), vars, 
                    null, Double.NaN, Double.NaN, true); //getMetadata
                for (int v = 0; v < table.nColumns(); v++) {
                    tally.add(table.getColumnName(v), 
                        table.columnAttributes(v).getString(attribute));
                }
            } catch (Throwable t) {
                //String2.log(t.toString());
            }
        }
        String2.log("\n" + tally.toString());
    }

    /**
     * The basic tests of this class.
     */
    public static void testGlobec() throws Throwable {
        testVerboseOn();
        String2.log("\n***EDDTableFromFiles.testGlobec");
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        int po, epo;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

        //*** test things that should throw exceptions
        StringArray rv = new StringArray();
        StringArray cv = new StringArray();
        StringArray co = new StringArray();
        StringArray cv2 = new StringArray();

        String mapDapQuery = "longitude,latitude,NO3,time&latitude>0&altitude>-5&time>=2002-08-03";
        userDapQuery = "longitude,NO3,time,ship&latitude>0&altitude>-5&time>=2002-08-03";
        String regexDapQuery = "longitude,NO3,time,ship&latitude>0&altitude>-5&time>=2002-08-03" +
            "&longitude" + PrimitiveArray.REGEX_OP + "\".*11.*\"";

        //testGlobecBottle is like erdGlobecBottle, but with the addition
        //  of a fixed value altitude=0 variable
        EDDTable globecBottle = (EDDTable)oneFromDatasetsXml(null, "testGlobecBottle"); //should work

 
        //getEmpiricalMinMax just do once
        //globecBottle.getEmpiricalMinMax("2002-07-01", "2002-09-01", false, true);
        //if (true) System.exit(1);

        //*** test valid queries
        String2.log("\n****************** EDDTableFromNcFiles.test valid queries \n");
        globecBottle.parseUserDapQuery("longitude,NO3", rv, cv, co, cv2, false);  
        Test.ensureEqual(rv.toString(), "longitude, NO3", "");
        Test.ensureEqual(cv.toString(), "", "");
        Test.ensureEqual(co.toString(), "", "");
        Test.ensureEqual(cv2.toString(), "", "");

        globecBottle.parseUserDapQuery("longitude,NO3&altitude=0", rv, cv, co, cv2, false);  
        Test.ensureEqual(rv.toString(), "longitude, NO3", "");
        Test.ensureEqual(cv.toString(), "altitude", "");
        Test.ensureEqual(co.toString(), "=", "");
        Test.ensureEqual(cv2.toString(), "0", "");

        //test: no resultsVariables interpreted as all resultsVariables
        String allVars = 
            "cruise_id, ship, cast, longitude, latitude, altitude, time, bottle_posn, " +
            "chl_a_total, chl_a_10um, phaeo_total, phaeo_10um, sal00, sal11, temperature0, " +
            "temperature1, fluor_v, xmiss_v, PO4, N_N, NO3, Si, NO2, NH4, oxygen, par";

        globecBottle.parseUserDapQuery("", rv, cv, co, cv2, false);  
        Test.ensureEqual(rv.toString(), allVars, "");
        Test.ensureEqual(cv.toString(), "", "");
        Test.ensureEqual(co.toString(), "", "");
        Test.ensureEqual(cv2.toString(), "", "");

        globecBottle.parseUserDapQuery("&altitude%3E=0", rv, cv, co, cv2, false);  
        Test.ensureEqual(rv.toString(), allVars, "");
        Test.ensureEqual(cv.toString(), "altitude", "");
        Test.ensureEqual(co.toString(), ">=", "");
        Test.ensureEqual(cv2.toString(), "0", "");

        globecBottle.parseUserDapQuery("s", rv, cv, co, cv2, false);  
        Test.ensureEqual(rv.toString(), allVars, "");
        Test.ensureEqual(cv.toString(), "", "");
        Test.ensureEqual(co.toString(), "", "");
        Test.ensureEqual(cv2.toString(), "", "");

        globecBottle.parseUserDapQuery("s&s.altitude=0", rv, cv, co, cv2, false);  
        Test.ensureEqual(rv.toString(), allVars, "");
        Test.ensureEqual(cv.toString(), "altitude", "");
        Test.ensureEqual(co.toString(), "=", "");
        Test.ensureEqual(cv2.toString(), "0", "");

        globecBottle.parseUserDapQuery("s.longitude,s.altitude&s.altitude=0", rv, cv, co, cv2, false);  
        Test.ensureEqual(rv.toString(), "longitude, altitude", "");
        Test.ensureEqual(cv.toString(), "altitude", "");
        Test.ensureEqual(co.toString(), "=", "");
        Test.ensureEqual(cv2.toString(), "0", "");


        //Tests of time related to "now"  -- Many fail because this dataset has no recent data.
        GregorianCalendar gc;
        String s;

        gc = Calendar2.newGCalendarZulu(); 
        s = Calendar2.formatAsISODateTimeT(gc);
        try {
            globecBottle.parseUserDapQuery("time&time=now", rv, cv, co, cv2, false);  //non-regex EDVTimeStamp conValues will be ""+epochSeconds
            throw new SimpleException("shouldn't get here");
        } catch (Throwable t) {
            results = t.toString();
            Test.ensureLinesMatch(results, 
                "com.cohort.util.SimpleException: Your query produced no matching results. " +
                "\\(time=" + s.substring(0, 14) + ".{5}Z is outside of the variable's actual_range: " +
                "2002-05-30T03:21:00Z to 2002-08-19T20:18:00Z\\)", 
                "results=\n" + results);
        }

        gc = Calendar2.newGCalendarZulu(); 
        gc.add(Calendar2.SECOND, -7);
        s = Calendar2.formatAsISODateTimeT(gc);
        try {
            globecBottle.parseUserDapQuery("time&time=now-7seconds", rv, cv, co, cv2, false);  //non-regex EDVTimeStamp conValues will be ""+epochSeconds
            throw new SimpleException("shouldn't get here");
        } catch (Throwable t) {
            results = t.toString();
            Test.ensureLinesMatch(results, 
                "com.cohort.util.SimpleException: Your query produced no matching results. " +
                "\\(time=" + s.substring(0, 17) + ".{2}Z is outside of the variable's actual_range: " +
                "2002-05-30T03:21:00Z to 2002-08-19T20:18:00Z\\)", 
                "results=\n" + results);
        }

        gc = Calendar2.newGCalendarZulu(); 
        gc.add(Calendar2.MINUTE, -5);
        s = Calendar2.formatAsISODateTimeT(gc);
        try {
            globecBottle.parseUserDapQuery("time&time=now-5minutes", rv, cv, co, cv2, false);  //non-regex EDVTimeStamp conValues will be ""+epochSeconds
            throw new SimpleException("shouldn't get here");
        } catch (Throwable t) {
            results = t.toString();
            Test.ensureLinesMatch(results, 
                "com.cohort.util.SimpleException: Your query produced no matching results. " +
                "\\(time=" + s.substring(0, 17) + ".{2}Z is outside of the variable's actual_range: " +
                "2002-05-30T03:21:00Z to 2002-08-19T20:18:00Z\\)", 
                "results=\n" + results);
        }

        gc = Calendar2.newGCalendarZulu(); 
        gc.add(Calendar2.HOUR_OF_DAY, -4);
        s = Calendar2.formatAsISODateTimeT(gc);
        try {
            globecBottle.parseUserDapQuery("time&time=now-4hours", rv, cv, co, cv2, false);  //non-regex EDVTimeStamp conValues will be ""+epochSeconds
            throw new SimpleException("shouldn't get here");
        } catch (Throwable t) {
            results = t.toString();
            Test.ensureLinesMatch(results, 
                "com.cohort.util.SimpleException: Your query produced no matching results. " +
                "\\(time=" + s.substring(0, 17) + ".{2}Z is outside of the variable's actual_range: " +
                "2002-05-30T03:21:00Z to 2002-08-19T20:18:00Z\\)", 
                "results=\n" + results);
        }

        gc = Calendar2.newGCalendarZulu(); 
        gc.add(Calendar2.DATE, -2);
        s = Calendar2.formatAsISODateTimeT(gc);
        try {
            globecBottle.parseUserDapQuery("time&time=now-2days", rv, cv, co, cv2, false);  //non-regex EDVTimeStamp conValues will be ""+epochSeconds
            throw new SimpleException("shouldn't get here");
        } catch (Throwable t) {
            results = t.toString();
            Test.ensureLinesMatch(results, 
                "com.cohort.util.SimpleException: Your query produced no matching results. " +
                "\\(time=" + s.substring(0, 17) + ".{2}Z is outside of the variable's actual_range: " +
                "2002-05-30T03:21:00Z to 2002-08-19T20:18:00Z\\)", 
                "results=\n" + results);
        }

        gc = Calendar2.newGCalendarZulu(); 
        gc.add(Calendar2.MONTH, -3);
        s = Calendar2.formatAsISODateTimeT(gc);
        try {
            globecBottle.parseUserDapQuery("time&time=now-3months", rv, cv, co, cv2, false);  //non-regex EDVTimeStamp conValues will be ""+epochSeconds
            throw new SimpleException("shouldn't get here");
        } catch (Throwable t) {
            results = t.toString();
            Test.ensureLinesMatch(results, 
                "com.cohort.util.SimpleException: Your query produced no matching results. " +
                "\\(time=" + s.substring(0, 17) + ".{2}Z is outside of the variable's actual_range: " +
                "2002-05-30T03:21:00Z to 2002-08-19T20:18:00Z\\)", 
                "results=\n" + results);
        }

        gc = Calendar2.newGCalendarZulu(); 
        gc.add(Calendar2.YEAR, -2);
        s = Calendar2.formatAsISODateTimeT(gc);
        try {
            globecBottle.parseUserDapQuery("time&time=now-2years", rv, cv, co, cv2, false);  //non-regex EDVTimeStamp conValues will be ""+epochSeconds
            throw new SimpleException("shouldn't get here");
        } catch (Throwable t) {
            results = t.toString();
            Test.ensureLinesMatch(results, 
                "com.cohort.util.SimpleException: Your query produced no matching results. " +
                "\\(time=" + s.substring(0, 17) + ".{2}Z is outside of the variable's actual_range: " +
                "2002-05-30T03:21:00Z to 2002-08-19T20:18:00Z\\)", 
                "results=\n" + results);
        }


        //longitude converted to lon
        //altitude is fixed value, so test done by getSourceQueryFromDapQuery
        //time test deferred till later, so 'datetime_epoch' included in sourceQuery
        //also, lat is added to var list because it is used in constraints
        globecBottle.getSourceQueryFromDapQuery(userDapQuery, rv, cv, co, cv2);        
        Test.ensureEqual(formatAsDapQuery(rv.toArray(), cv.toArray(), co.toArray(), cv2.toArray()),
            "lon100,no3,datetime_epoch,ship,lat100&lat100>0&datetime_epoch>=1.0283328E9",  
            "Unexpected sourceDapQuery from userDapQuery=" + userDapQuery);
 
        //test invalid queries
        try {
            //lon is the source name
            globecBottle.getSourceQueryFromDapQuery("lon,cast", rv, cv, co, cv2);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0], 
            "SimpleException: Query error: Unrecognized variable=\"lon\"", 
            "error=" + error);

        error = "";
        try {
            //a variable can't be listed twice
            globecBottle.getSourceQueryFromDapQuery("cast,longitude,cast,latitude", rv, cv, co, cv2);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0], 
            "SimpleException: Query error: variable=cast is listed twice in the results variables list.", 
            "error=" + error);

        error = "";
        try {
            //if s is used, it must be the only request var
            globecBottle.getSourceQueryFromDapQuery("s.latitude,s", rv, cv, co, cv2);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0], 
            "SimpleException: Query error: If s is requested, it must be the only requested variable.", 
            "error=" + error);

        error = "";
        try {
            //zztop isn't valid variable
            globecBottle.getSourceQueryFromDapQuery("cast,zztop", rv, cv, co, cv2);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0], 
            "SimpleException: Query error: Unrecognized variable=\"zztop\"", "error=" + error);

        error = "";
        try {
            //alt is fixedValue=0 so will always return NO_DATA
            globecBottle.getSourceQueryFromDapQuery("cast&altitude<-1", rv, cv, co, cv2);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0], 
            "SimpleException: Your query produced no matching results. (altitude<-1 is outside of the variable's actual_range: 0 to 0)", 
            "error=" + error);

        error = "";
        try {
            //lon isn't a valid var
            globecBottle.getSourceQueryFromDapQuery("NO3, Si&lon=0", rv, cv, co, cv2);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0], 
            "SimpleException: Query error: Unrecognized constraint variable=\"lon\"", "error=" + error);

        error = "";
        try {
            //should be ==, not =
            globecBottle.getSourceQueryFromDapQuery("NO3,Si&altitude==0", rv, cv, co, cv2);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0], 
            "SimpleException: Query error: Use '=' instead of '==' in constraints.", 
            "error=" + error);

        error = "";
        try {
            //regex operator should be =~, not ~=
            globecBottle.getSourceQueryFromDapQuery("NO3,Si&altitude~=(0|1.*)", rv, cv, co, cv2);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0], 
            "SimpleException: Query error: Use '=~' instead of '~=' in constraints.", 
            "error=" + error);

        error = "";
        try {
            //string regex values must be in quotes
            globecBottle.getSourceQueryFromDapQuery("NO3,Si&ship=New_Horizon", rv, cv, co, cv2);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0], 
            "SimpleException: Query error: For constraints of String variables, the right-hand-side value must be surrounded by double quotes.",
            "error=" + error);
        Test.ensureEqual(String2.split(error, '\n')[1], 
            "Bad constraint: ship=New_Horizon", 
            "error=" + error);

        error = "";
        try {
            //numeric variable regex values must be in quotes
            globecBottle.getSourceQueryFromDapQuery("NO3,Si&altitude=~(0|1.*)", rv, cv, co, cv2);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0], 
            "SimpleException: Query error: For =~ constraints of numeric variables, " +
            "the right-hand-side value must be surrounded by double quotes.",
            "error=" + error);
        Test.ensureEqual(String2.split(error, '\n')[1], 
            "Bad constraint: altitude=~(0|1.*)", 
            "error=" + error);

        error = "";
        try {
            globecBottle.getSourceQueryFromDapQuery("NO3,Si&altitude=0|longitude>-180", 
                rv, cv, co, cv2);  //invalid query format caught as invalid NaN
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0], 
            "SimpleException: Query error: Numeric tests of NaN must use \"NaN\", " +
            "not value=\"0|longitude>-180\".", "error=" + error);

        error = "";
        String nowQ[] = {"nowa", "now-1 day", "now-", "now-5.5days", "now-5date", "now-9dayss"};
        for (int i = 0; i < nowQ.length; i++) {
            try {
                globecBottle.getSourceQueryFromDapQuery("time&time=" + nowQ[i], 
                    rv, cv, co, cv2);  //invalid query format caught as invalid NaN
            } catch (Throwable t) {
                error = MustBe.throwableToString(t);
            }
            Test.ensureEqual(String2.split(error, '\n')[0], 
                "SimpleException: Query error: Invalid \"now\" constraint: \"" + nowQ[i] + "\". " +
                "Timestamp constraints with \"now\" must be in " +
                "the form \"now[+|-positiveInteger[millis|seconds|minutes|hours|days|months|years]]\"" +
                " (or singular units).", "error=" + error);
        }

        //time tests are perfectly precise: actual_range 1.02272886e+9, 1.02978828e+9;                
        try {
            globecBottle.getSourceQueryFromDapQuery("&time=1.022728859e9", rv, cv, co, cv2);   //min -1 in last+1 digit
            throw new SimpleException("shouldn't get here");
        } catch (Throwable t) {
            error = t.toString();
            Test.ensureEqual(error, "com.cohort.util.SimpleException: Your query produced no matching results. " +
                "(time=2002-05-30T03:20:59Z is outside of the variable's actual_range: " +
                "2002-05-30T03:21:00Z to 2002-08-19T20:18:00Z)", "");
        }

        try {
            globecBottle.getSourceQueryFromDapQuery("&time=1.029788281e9", rv, cv, co, cv2);   //max +1 in last+1 digit
            throw new SimpleException("shouldn't get here");
        } catch (Throwable t) {
            error = t.toString();
            Test.ensureEqual(error, "com.cohort.util.SimpleException: Your query produced no matching results. " +
                "(time=2002-08-19T20:18:01Z is outside of the variable's actual_range: " +
                "2002-05-30T03:21:00Z to 2002-08-19T20:18:00Z)", "");
        }

        //impossible queries    lon range is float -126.2, -124.1
        String impossibleQuery[] = new String[]{
            "&longitude!=NaN&longitude<=NaN",
            "&longitude!=NaN&longitude>=NaN",
            "&longitude!=NaN&longitude=NaN",
            "&longitude=NaN&longitude!=NaN",
            "&longitude=NaN&longitude<3.0",
            "&longitude=NaN&longitude<=3.0",
            "&longitude=NaN&longitude>-125.0",
            "&longitude=NaN&longitude>=-125.0",
            "&longitude=NaN&longitude=-125.0",
            "&longitude<2.0&longitude=NaN",
            "&longitude<=2.0&longitude=NaN",
            "&longitude<=2.0&longitude<=NaN",
            "&longitude<-126.0&longitude>-125.0",
            "&longitude<=-126.0&longitude>=-125.0",
            "&longitude<=-126.0&longitude=-125.0",
            "&longitude>-125.0&longitude=NaN",
            "&longitude>=-125.0&longitude=NaN",
            "&longitude>=-125.0&longitude<=NaN",
            "&longitude>-125.0&longitude=-126.0",
            "&longitude>=-125.0&longitude=-126.0",
            "&longitude>=-125.0&longitude<=-126.0",
            "&longitude=-125.0&longitude<=NaN",
            "&longitude=-125.0&longitude>=NaN",
            "&longitude=-125.0&longitude=NaN",
            "&longitude=-126.0&longitude>-125.0",
            "&longitude=-126.0&longitude>=-125.0",
            "&longitude=-125.0&longitude<-126.0",
            "&longitude=-125.0&longitude<=-126.0",
            "&longitude=-125.0&longitude=-126.0",
            "&longitude=-126.0&longitude!=-126.0"
            };
        for (int i = 0; i < impossibleQuery.length; i++) {
            error = "";
            try {globecBottle.getSourceQueryFromDapQuery(impossibleQuery[i], rv, cv, co, cv2);  
            } catch (Throwable t) {
                error = MustBe.throwableToString(t); 
            }
            Test.ensureEqual(String2.split(error, '\n')[0], 
                "SimpleException: Query error: " + 
                String2.replaceAll(impossibleQuery[i].substring(1), "&", " and ") + 
                " will never both be true.", 
                "error=" + error);
        }
        //possible queries   lon range is float -126.2, -124.1
        globecBottle.getSourceQueryFromDapQuery("&longitude=-126.2", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&longitude<=-126.2", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&longitude<-126.2", rv, cv, co, cv2);  //good: fuzzy test allows it
        globecBottle.getSourceQueryFromDapQuery("&longitude=-124.1", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&longitude>=-124.1", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&longitude>-124.1", rv, cv, co, cv2);  //good: fuzzy test allows it
        globecBottle.getSourceQueryFromDapQuery("&longitude<-126.0&longitude<=-125.0", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&longitude>=-126.0&longitude>-125.0", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&longitude=-126.0&longitude<-125.0", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&longitude>-126.0&longitude=-125.0", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&longitude>=NaN", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&longitude<=NaN", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&longitude!=NaN", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&longitude<-126.0&longitude!=NaN", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&longitude!=NaN&longitude=-125.0", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&longitude=NaN&longitude<=NaN", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&longitude<=NaN&longitude=NaN", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&longitude=NaN&longitude!=-125.0", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&longitude!=-125.0&longitude=NaN", rv, cv, co, cv2);  
        //time tests are perfectly precise: actual_range 1.02272886e+9, 1.02978828e+9;                
        globecBottle.getSourceQueryFromDapQuery("&time=1.02272886e9", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&time<1.02272886e9", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&time<=1.02272886e9", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&time=1.02978828e9", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&time>1.02978828e9", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&time>=1.02978828e9", rv, cv, co, cv2);  


        //*** test dapInstructions
        //StringWriter sw = new StringWriter();
        //writeGeneralDapHtmlDocument(EDStatic.erddapUrl, sw); //for testing, use the non-https url
        //results = sw.toString();
        //expected = "Requests for Tabular Data in ";
        //Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        //expected = "In ERDDAP, time variables always have the name \"" + EDV.TIME_NAME + "\"";
        //Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);

        //test sliderCsvValues
        edv = globecBottle.findDataVariableByDestinationName("longitude");
        results = edv.sliderCsvValues();
        expected = "-126.2, -126.19, -126.18, -126.17, -126.16,";  
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
        expected = ".13, -124.12, -124.11, -124.1";  
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "results=\n" + results);

        edv = globecBottle.findDataVariableByDestinationName("latitude");
        results = edv.sliderCsvValues();
        expected = "41.9, 41.92, 41.94, 41.96, 41.98"; 
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
        expected = "44.56, 44.58, 44.6, 44.62, 44.64, 44.65";  
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "results=\n" + results);

        edv = globecBottle.findDataVariableByDestinationName("altitude");
        results = edv.sliderCsvValues();
        expected = "0"; //0
        Test.ensureEqual(results, expected, "results=\n" + results);

        edv = globecBottle.findDataVariableByDestinationName("time");
        results = edv.sliderCsvValues();
        //2002-05-30T03:21:00Z	   2002-08-19T20:18:00Z
        expected = "\"2002-05-30T03:21:00Z\", \"2002-05-30T12:00:00Z\", \"2002-05-31\", \"2002-05-31T12:00:00Z\",";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
        expected = "\"2002-08-19\", \"2002-08-19T12:00:00Z\", \"2002-08-19T20:18:00Z\"";
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "results=\n" + results);

        edv = globecBottle.findDataVariableByDestinationName("phaeo_total");
        results = edv.sliderCsvValues();
        expected = "-3.111, -3, -2.8, -2.6, -2.4, -2.2,"; //-3.111
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
        expected = "32.8, 33, 33.2, 33.4, 33.6, 33.821"; //33.821
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "results=\n" + results);

        edv = globecBottle.findDataVariableByDestinationName("cruise_id");
        results = edv.sliderCsvValues();
        expected = null;
        Test.ensureEqual(results, expected, "results=\n" + results);


        //*** test getting das for entire dataset
        String2.log("\n****************** EDDTableFromNcFiles.test das dds for entire dataset\n");
        tName = globecBottle.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Entire", ".das"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        String expectedDas1 = //see OpendapHelper.EOL for comments
"Attributes {\n" +
" s {\n" +
"  cruise_id {\n" +
"    String cf_role \"trajectory_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Cruise ID\";\n" +
"  }\n" +
"  ship {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Ship\";\n" +
"  }\n" +
"  cast {\n" +
"    Int16 _FillValue 32767;\n" +
"    Int16 actual_range 1, 127;\n" +
"    Float64 colorBarMaximum 140.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Cast Number\";\n" +
"    Int16 missing_value 32767;\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 _FillValue NaN;\n" +
"    Float32 actual_range -126.2, -124.1;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 _FillValue NaN;\n" +
"    Float32 actual_range 41.9, 44.65;\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    Int32 actual_range 0, 0;\n" +
"    String axis \"Z\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 _FillValue NaN;\n" +
"    Float64 actual_range 1.02272886e+9, 1.02978828e+9;\n" +
"    String axis \"T\";\n" +
"    String cf_role \"profile_id\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    Float64 missing_value NaN;\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  bottle_posn {\n" +
"    Byte _FillValue 127;\n" +
"    Byte actual_range 0, 12;\n" +
"    Float64 colorBarMaximum 12.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Bottle Number\";\n" +
"    Byte missing_value -128;\n" +
"  }\n" +
"  chl_a_total {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range -2.602, 40.17;\n" +
"    Float64 colorBarMaximum 30.0;\n" +
"    Float64 colorBarMinimum 0.03;\n" +
"    String colorBarScale \"Log\";\n" +
"    String ioos_category \"Ocean Color\";\n" +
"    String long_name \"Chlorophyll-a\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"concentration_of_chlorophyll_in_sea_water\";\n" +
"    String units \"ug L-1\";\n" +
"  }\n";
        Test.ensureEqual(results.substring(0, expectedDas1.length()), expectedDas1, "\nresults=\n" + results);

        String expectedDas2 = 
   "String id \"Globec_bottle_data_2002\";\n" +
"    String infoUrl \"http://www.globec.org/\";\n" +
"    String institution \"GLOBEC\";\n" +
"    String keywords \"10um,\n" +
"Biosphere > Vegetation > Photosynthetically Active Radiation,\n" +
"Oceans > Ocean Chemistry > Ammonia,\n" +
"Oceans > Ocean Chemistry > Chlorophyll,\n" +
"Oceans > Ocean Chemistry > Nitrate,\n" +
"Oceans > Ocean Chemistry > Nitrite,\n" +
"Oceans > Ocean Chemistry > Nitrogen,\n" +
"Oceans > Ocean Chemistry > Oxygen,\n" +
"Oceans > Ocean Chemistry > Phosphate,\n" +
"Oceans > Ocean Chemistry > Pigments,\n" +
"Oceans > Ocean Chemistry > Silicate,\n" +
"Oceans > Ocean Optics > Attenuation/Transmission,\n" +
"Oceans > Ocean Temperature > Water Temperature,\n" +
"Oceans > Salinity/Density > Salinity,\n" +
"active, after, ammonia, ammonium, attenuation, biosphere, bottle, cast, chemistry, chlorophyll, chlorophyll-a, color, concentration, concentration_of_chlorophyll_in_sea_water, cruise, data, density, dissolved, dissolved nutrients, dissolved o2, fluorescence, fraction, from, globec, identifier, mass, mole, mole_concentration_of_ammonium_in_sea_water, mole_concentration_of_nitrate_in_sea_water, mole_concentration_of_nitrite_in_sea_water, mole_concentration_of_phosphate_in_sea_water, mole_concentration_of_silicate_in_sea_water, moles, moles_of_nitrate_and_nitrite_per_unit_mass_in_sea_water, n02, nep, nh4, nitrate, nitrite, nitrogen, no3, number, nutrients, o2, ocean, ocean color, oceans, optical, optical properties, optics, oxygen, passing, per, phaeopigments, phosphate, photosynthetically, pigments, plus, po4, properties, radiation, rosette, salinity, screen, sea, sea_water_practical_salinity, sea_water_temperature, seawater, sensor, sensors, ship, silicate, temperature, time, total, transmission, transmissivity, unit, vegetation, voltage, volume, volume_fraction_of_oxygen_in_sea_water, water\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    Float64 Northernmost_Northing 44.65;\n" +
"    String sourceUrl \"(local files; contact erd.data@noaa.gov)\";\n" +
"    Float64 Southernmost_Northing 41.9;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
"    String subsetVariables \"cruise_id, ship, cast, longitude, latitude, time\";\n" +
"    String summary \"GLOBEC (GLOBal Ocean ECosystems Dynamics) NEP (Northeast Pacific)\n" +
"Rosette Bottle Data from New Horizon Cruise (NH0207: 1-19 August 2002).\n" +
"Notes:\n" +
"Physical data processed by Jane Fleischbein (OSU).\n" +
"Chlorophyll readings done by Leah Feinberg (OSU).\n" +
"Nutrient analysis done by Burke Hales (OSU).\n" +
"Sal00 - salinity calculated from primary sensors (C0,T0).\n" +
"Sal11 - salinity calculated from secondary sensors (C1,T1).\n" +
"secondary sensor pair was used in final processing of CTD data for\n" +
"most stations because the primary had more noise and spikes. The\n" +
"primary pair were used for cast #9, 24, 48, 111 and 150 due to\n" +
"multiple spikes or offsets in the secondary pair.\n" +
"Nutrient samples were collected from most bottles; all nutrient data\n" +
"developed from samples frozen during the cruise and analyzed ashore;\n" +
"data developed by Burke Hales (OSU).\n" +
"Operation Detection Limits for Nutrient Concentrations\n" +
"Nutrient  Range         Mean    Variable         Units\n" +
"PO4       0.003-0.004   0.004   Phosphate        micromoles per liter\n" +
"N+N       0.04-0.08     0.06    Nitrate+Nitrite  micromoles per liter\n" +
"Si        0.13-0.24     0.16    Silicate         micromoles per liter\n" +
"NO2       0.003-0.004   0.003   Nitrite          micromoles per liter\n" +
"Dates and Times are UTC.\n" +
"\n" +
"For more information, see\n" +
"http://cis.whoi.edu/science/bcodmo/dataset.cfm?id=10180&flag=view\n" +
"\n" +
"Inquiries about how to access this data should be directed to\n" +
"Dr. Hal Batchelder (hbatchelder@coas.oregonstate.edu).\";\n" +
"    String time_coverage_end \"2002-08-19T20:18:00Z\";\n" +
"    String time_coverage_start \"2002-05-30T03:21:00Z\";\n" +
"    String title \"GLOBEC NEP Rosette Bottle Data (2002)\";\n" +
"    Float64 Westernmost_Easting -126.2;\n" +
"  }\n" +
"}\n";
        po = results.indexOf(expectedDas2.substring(0, 17));
        Test.ensureEqual(results.substring(Math.max(0, po)), expectedDas2, "\nresults=\n" + results);


        //*** test getting dds for entire dataset
        tName = globecBottle.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Entire", ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String cruise_id;\n" +
"    String ship;\n" +
"    Int16 cast;\n" +
"    Float32 longitude;\n" +
"    Float32 latitude;\n" +
"    Int32 altitude;\n" +
"    Float64 time;\n" +
"    Byte bottle_posn;\n" +
"    Float32 chl_a_total;\n" +
"    Float32 chl_a_10um;\n" +
"    Float32 phaeo_total;\n" +
"    Float32 phaeo_10um;\n" +
"    Float32 sal00;\n" +
"    Float32 sal11;\n" +
"    Float32 temperature0;\n" +
"    Float32 temperature1;\n" +
"    Float32 fluor_v;\n" +
"    Float32 xmiss_v;\n" +
"    Float32 PO4;\n" +
"    Float32 N_N;\n" +
"    Float32 NO3;\n" +
"    Float32 Si;\n" +
"    Float32 NO2;\n" +
"    Float32 NH4;\n" +
"    Float32 oxygen;\n" +
"    Float32 par;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //*** test DAP data access form
        tName = globecBottle.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Entire", ".html"); 
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
 

        //*** test make data files
        String2.log("\n****************** EDDTableFromNcFiles.test make DATA FILES\n");       

        //.asc
        tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Data", ".asc"); 
        results = String2.annotatedString(new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray()));
        //String2.log(results);
        expected = 
"Dataset {[10]\n" +
"  Sequence {[10]\n" +
"    Float32 longitude;[10]\n" +
"    Float32 NO3;[10]\n" +
"    Float64 time;[10]\n" +
"    String ship;[10]\n" +
"  } s;[10]\n" +
"} s;[10]\n" +
"---------------------------------------------[10]\n" +
"s.longitude, s.NO3, s.time, s.ship[10]\n" +
"-124.4, 35.7, 1.02833814E9, \"New_Horizon\"[10]\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = "-124.8, -9999.0, 1.02835902E9, \"New_Horizon\"[10]\n"; //row with missing value  has source missing value
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = "-124.1, 24.45, 1.02978828E9, \"New_Horizon\"[10]\n[end]"; //last row
        Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

        //.csv
        tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, globecBottle.className() + "_Data", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude,NO3,time,ship\n" +
"degrees_east,micromoles L-1,UTC,\n" +
"-124.4,35.7,2002-08-03T01:29:00Z,New_Horizon\n" +
"-124.4,35.48,2002-08-03T01:29:00Z,New_Horizon\n" +
"-124.4,31.61,2002-08-03T01:29:00Z,New_Horizon\n"; 
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = "-124.82,NaN,2002-08-17T00:49:00Z,New_Horizon\n"; //row with missing value  has "NaN" missing value
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = "-124.1,24.45,2002-08-19T20:18:00Z,New_Horizon\n"; //last row
        Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

        //.csvp  and &units("UCUM")
        tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery + "&units(\"UCUM\")", 
            EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Data", ".csvp"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude (deg{east}),NO3 (umol.L-1),time (UTC),ship\n" +
"-124.4,35.7,2002-08-03T01:29:00Z,New_Horizon\n" +
"-124.4,35.48,2002-08-03T01:29:00Z,New_Horizon\n" +
"-124.4,31.61,2002-08-03T01:29:00Z,New_Horizon\n"; 
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = "-124.82,NaN,2002-08-17T00:49:00Z,New_Horizon\n"; //row with missing value  has "NaN" missing value
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = "-124.1,24.45,2002-08-19T20:18:00Z,New_Horizon\n"; //last row
        Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

        //.csv0
        tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Data", ".csv0"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"-124.4,35.7,2002-08-03T01:29:00Z,New_Horizon\n" +
"-124.4,35.48,2002-08-03T01:29:00Z,New_Horizon\n" +
"-124.4,31.61,2002-08-03T01:29:00Z,New_Horizon\n"; 
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = "-124.82,NaN,2002-08-17T00:49:00Z,New_Horizon\n"; //row with missing value  has "NaN" missing value
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = "-124.1,24.45,2002-08-19T20:18:00Z,New_Horizon\n"; //last row
        Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

        //.csv   test of datasetName.dataVarName notation
        String dotDapQuery = "s.longitude,altitude,NO3,s.time,ship" +
            "&s.latitude>0&altitude>-5&s.time>=2002-08-03";
        tName = globecBottle.makeNewFileForDapQuery(null, null, dotDapQuery, EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_DotNotation", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude,altitude,NO3,time,ship\n" +
"degrees_east,m,micromoles L-1,UTC,\n" +
"-124.4,0,35.7,2002-08-03T01:29:00Z,New_Horizon\n" +
"-124.4,0,35.48,2002-08-03T01:29:00Z,New_Horizon\n" +
"-124.4,0,31.61,2002-08-03T01:29:00Z,New_Horizon\n";
        Test.ensureTrue(results.indexOf(expected) == 0, "\nresults=\n" + results);
        expected = "-124.1,0,24.45,2002-08-19T20:18:00Z,New_Horizon\n"; //last row
        Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

        //.csv  test of regex on numeric variable
        tName = globecBottle.makeNewFileForDapQuery(null, null, regexDapQuery, EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_NumRegex", ".csv"); 
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude,NO3,time,ship\n" +
"degrees_east,micromoles L-1,UTC,\n" +
"-125.11,33.91,2002-08-09T05:03:00Z,New_Horizon\n" +
"-125.11,26.61,2002-08-09T05:03:00Z,New_Horizon\n" +
"-125.11,10.8,2002-08-09T05:03:00Z,New_Horizon\n" +
"-125.11,8.42,2002-08-09T05:03:00Z,New_Horizon\n" +
"-125.11,6.34,2002-08-09T05:03:00Z,New_Horizon\n" +
"-125.11,1.29,2002-08-09T05:03:00Z,New_Horizon\n" +
"-125.11,0.02,2002-08-09T05:03:00Z,New_Horizon\n" +
"-125.11,0.0,2002-08-09T05:03:00Z,New_Horizon\n" +
"-125.11,10.81,2002-08-09T05:03:00Z,New_Horizon\n" +
"-125.11,42.39,2002-08-18T23:49:00Z,New_Horizon\n" +
"-125.11,33.84,2002-08-18T23:49:00Z,New_Horizon\n" +
"-125.11,27.67,2002-08-18T23:49:00Z,New_Horizon\n" +
"-125.11,15.93,2002-08-18T23:49:00Z,New_Horizon\n" +
"-125.11,8.69,2002-08-18T23:49:00Z,New_Horizon\n" +
"-125.11,4.6,2002-08-18T23:49:00Z,New_Horizon\n" +
"-125.11,2.17,2002-08-18T23:49:00Z,New_Horizon\n" +
"-125.11,8.61,2002-08-18T23:49:00Z,New_Horizon\n" +
"-125.11,0.64,2002-08-18T23:49:00Z,New_Horizon\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv  test of String=
        try {
            String tDapQuery = "longitude,NO3,time,ship&latitude>0&altitude>-5" +
                "&time>=2002-08-07T00&time<=2002-08-07T06&ship=\"New_Horizon\"";
            tName = globecBottle.makeNewFileForDapQuery(null, null, tDapQuery, EDStatic.fullTestCacheDirectory, 
                globecBottle.className() + "_StrEq", ".csv"); 
            //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
"longitude,NO3,time,ship\n" +
"degrees_east,micromoles L-1,UTC,\n" +
"-124.8,34.54,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,29.98,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,17.24,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,12.74,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,11.43,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,NaN,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,9.74,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,5.62,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,4.4,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,4.21,2002-08-07T01:52:00Z,New_Horizon\n" +
"-125.0,35.28,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,30.87,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,25.2,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,20.66,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,NaN,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,NaN,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,10.85,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,5.44,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,4.69,2002-08-07T03:43:00Z,New_Horizon\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nUnexpected error."); 
        }

        //.csv  test of String< >
        try {
            String tDapQuery = "longitude,NO3,time,ship&latitude>0&altitude>-5" +
                "&time>=2002-08-07T00&time<=2002-08-07T06&ship>\"Nev\"&ship<\"Nex\"";
            tName = globecBottle.makeNewFileForDapQuery(null, null, tDapQuery, EDStatic.fullTestCacheDirectory, 
                globecBottle.className() + "_GTLT", ".csv"); 
            //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
"longitude,NO3,time,ship\n" +
"degrees_east,micromoles L-1,UTC,\n" +
"-124.8,34.54,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,29.98,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,17.24,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,12.74,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,11.43,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,NaN,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,9.74,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,5.62,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,4.4,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,4.21,2002-08-07T01:52:00Z,New_Horizon\n" +
"-125.0,35.28,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,30.87,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,25.2,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,20.66,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,NaN,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,NaN,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,10.85,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,5.44,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,4.69,2002-08-07T03:43:00Z,New_Horizon\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nUnexpected error."); 
        }

        //.csv    test of String regex
        //!!! I also tested this with 
        //            <sourceCanConstrainStringRegex>~=</sourceCanConstrainStringRegex>
        //but it fails: 
        //Exception in thread "main" dods.dap.DODSException: "Your Query Produced No Matching Results."
        //and it isn't an encoding problem, opera encodes unencoded request as
        //http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle.dods?lon,NO3,datetime_epoch,ship,lat&lat%3E0&datetime_epoch%3E=1.0286784E9&datetime_epoch%3C=1.0287E9&ship~=%22(zztop|.*Horiz.*)%22
        //which fails the same way
        //Other simpler regex tests succeed.
        //It seems that the regex syntax is different for drds than erddap/java.
        //So recommend that people not say that drds servers can constrain String regex
        try {
            String tDapQuery = "longitude,NO3,time,ship&latitude>0" +
                "&time>=2002-08-07T00&time<=2002-08-07T06&ship=~\"(zztop|.*Horiz.*)\""; //source fails with this
                //"&time>=2002-08-07T00&time<=2002-08-07T06&ship=~\".*Horiz.*\"";       //source works with this
            tName = globecBottle.makeNewFileForDapQuery(null, null, tDapQuery, EDStatic.fullTestCacheDirectory, 
                globecBottle.className() + "_regex", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
"longitude,NO3,time,ship\n" +
"degrees_east,micromoles L-1,UTC,\n" +
"-124.8,34.54,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,29.98,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,17.24,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,12.74,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,11.43,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,NaN,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,9.74,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,5.62,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,4.4,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,4.21,2002-08-07T01:52:00Z,New_Horizon\n" +
"-125.0,35.28,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,30.87,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,25.2,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,20.66,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,NaN,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,NaN,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,10.85,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,5.44,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,4.69,2002-08-07T03:43:00Z,New_Horizon\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nUnexpected error."); 
        }


        //.das     das isn't affected by userDapQuery
        tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Data", ".das"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        Test.ensureEqual(
            results.substring(0, expectedDas1.length()), expectedDas1, "results=\n" + results);

        int tpo = results.indexOf(expectedDas2.substring(0, 17));
        Test.ensureEqual(results.substring(Math.max(tpo, 0)), expectedDas2, "results=\n" + results);

        //.dds 
        tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Data", ".dds"); 
        results = String2.annotatedString(new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray()));
        //String2.log(results);
        expected = 
"Dataset {[10]\n" +
"  Sequence {[10]\n" +
"    Float32 longitude;[10]\n" +
"    Float32 NO3;[10]\n" +
"    Float64 time;[10]\n" +
"    String ship;[10]\n" +
"  } s;[10]\n" +
"} s;[10]\n" +
"[end]";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.dods
        //tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
        //    globecBottle.className() + "_Data", ".dods"); 
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        try {
            String2.log("\ndo .dods test");
            String tUrl = EDStatic.erddapUrl + //in tests, always use non-https url
                "/tabledap/" + globecBottle.datasetID;
            //for diagnosing during development:
            //String2.log(String2.annotatedString(SSR.getUrlResponseString(
            //    "http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_vpt.dods?stn_id&unique()")));
            //String2.log("\nDAS RESPONSE=" + SSR.getUrlResponseString(tUrl + ".das?" + userDapQuery));
            //String2.log("\nDODS RESPONSE=" + String2.annotatedString(SSR.getUrlResponseString(tUrl + ".dods?" + userDapQuery)));

            //test if table.readOpendapSequence works with Erddap opendap server
            //!!!THIS READS DATA FROM ERDDAP SERVER RUNNING ON EDStatic.erddapUrl!!! //in tests, always use non-https url                
            //!!!THIS IS NOT JUST A LOCAL TEST!!!
            Table tTable = new Table();
            tTable.readOpendapSequence(tUrl + "?" + userDapQuery, false);
            Test.ensureEqual(tTable.globalAttributes().getString("title"), "GLOBEC NEP Rosette Bottle Data (2002)", "");
            Test.ensureEqual(tTable.columnAttributes(2).getString("units"), EDV.TIME_UNITS, "");
            Test.ensureEqual(tTable.getColumnNames(), new String[]{"longitude", "NO3", "time", "ship"}, "");
            Test.ensureEqual(tTable.getFloatData(0, 0), -124.4f, "");
            Test.ensureEqual(tTable.getFloatData(1, 0), 35.7f, "");
            Test.ensureEqual(tTable.getDoubleData(2, 0), 1.02833814E9, "");
            Test.ensureEqual(tTable.getStringData(3, 0), "New_Horizon", "");
            String2.log("  .dods test succeeded");
        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nError accessing " + EDStatic.erddapUrl + //in tests, always use non-https url
                " and reading erddap as a data source."); 
        }


        //.esriCsv
        tName = globecBottle.makeNewFileForDapQuery(null, null, 
            "&time>=2002-08-03", 
            EDStatic.fullTestCacheDirectory, 
            "testEsri5", ".esriCsv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"cruise_id,ship,cast,X,Y,altitude,date,time,bottle_pos,chl_a_tota,chl_a_10um,phaeo_tota,phaeo_10um,sal00,sal11,temperatur,temperatuA,fluor_v,xmiss_v,PO4,N_N,NO3,Si,NO2,NH4,oxygen,par\n" +
"nh0207,New_Horizon,20,-124.4,44.0,0,2002-08-03,1:29:00 am,1,-9999.0,-9999.0,-9999.0,-9999.0,33.9939,33.9908,7.085,7.085,0.256,0.518,2.794,35.8,35.7,71.11,0.093,0.037,-9999.0,0.1545\n" +
"nh0207,New_Horizon,20,-124.4,44.0,0,2002-08-03,1:29:00 am,2,-9999.0,-9999.0,-9999.0,-9999.0,33.8154,33.8111,7.528,7.53,0.551,0.518,2.726,35.87,35.48,57.59,0.385,0.018,-9999.0,0.1767\n" +
"nh0207,New_Horizon,20,-124.4,44.0,0,2002-08-03,1:29:00 am,3,1.463,-9999.0,1.074,-9999.0,33.5858,33.5834,7.572,7.573,0.533,0.518,2.483,31.92,31.61,48.54,0.307,0.504,-9999.0,0.3875\n" +
"nh0207,New_Horizon,20,-124.4,44.0,0,2002-08-03,1:29:00 am,4,2.678,-9999.0,1.64,-9999.0,33.2905,33.2865,8.093,8.098,1.244,0.518,2.262,27.83,27.44,42.59,0.391,0.893,-9999.0,0.7674\n" +
"nh0207,New_Horizon,20,-124.4,44.0,0,2002-08-03,1:29:00 am,5,4.182,-9999.0,2.363,-9999.0,33.2871,33.2863,8.157,8.141,1.458,0.518,2.202,26.15,25.73,40.25,0.424,1.204,-9999.0,0.7609\n" +
"nh0207,New_Horizon,20,-124.4,44.0,0,2002-08-03,1:29:00 am,6,7.601,-9999.0,3.959,-9999.0,33.3753,33.3678,11.733,11.73,3.685,0.518,1.092,8.96,8.75,16.31,0.211,1.246,-9999.0,1.9563\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);


        //.geoJson    mapDapQuery so lon and lat are in query
        tName = globecBottle.makeNewFileForDapQuery(null, null, mapDapQuery, EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_DataGJ", ".geoJson"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"{\n" +
"  \"type\": \"FeatureCollection\",\n" +
"  \"propertyNames\": [\"NO3\", \"time\"],\n" +
"  \"propertyUnits\": [\"micromoles L-1\", \"UTC\"],\n" +
"  \"features\": [\n" +
"\n" +
"{\"type\": \"Feature\",\n" +
"  \"geometry\": {\n" +
"    \"type\": \"Point\",\n" +
"    \"coordinates\": [-124.4, 44.0] },\n" +
"  \"properties\": {\n" +
"    \"NO3\": 35.7,\n" +
"    \"time\": \"2002-08-03T01:29:00Z\" }\n" +
"},\n" +
"{\"type\": \"Feature\",\n" +
"  \"geometry\": {\n" +
"    \"type\": \"Point\",\n" +
"    \"coordinates\": [-124.4, 44.0] },\n" +
"  \"properties\": {\n" +
"    \"NO3\": 35.48,\n" +
"    \"time\": \"2002-08-03T01:29:00Z\" }\n" +
"},\n";
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

expected = 
"{\"type\": \"Feature\",\n" +
"  \"geometry\": {\n" +
"    \"type\": \"Point\",\n" +
"    \"coordinates\": [-124.1, 44.65] },\n" +
"  \"properties\": {\n" +
"    \"NO3\": 24.45,\n" +
"    \"time\": \"2002-08-19T20:18:00Z\" }\n" +
"}\n" +
"\n" +
"  ],\n" +
"  \"bbox\": [-126.0, 41.9, -124.1, 44.65]\n" +
"}\n";
        tResults = results.substring(results.length() - expected.length());
        Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

        //.geoJson    just lon and lat in response
        tName = globecBottle.makeNewFileForDapQuery(null, null, 
            "longitude,latitude&latitude>0&altitude>-5&time>=2002-08-03",
            EDStatic.fullTestCacheDirectory, globecBottle.className() + "_DataGJLL", ".geoJson"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"{\n" +
"  \"type\": \"MultiPoint\",\n" +
"  \"coordinates\": [\n" +
"\n" +
"[-124.4, 44.0],\n" +
"[-124.4, 44.0],\n";
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

expected = 
"[-124.1, 44.65]\n" +
"\n" +
"  ],\n" +
"  \"bbox\": [-126.0, 41.9, -124.1, 44.65]\n" +
"}\n";
        tResults = results.substring(results.length() - expected.length());
        Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

        //.geoJson   with jsonp
        String jsonp = "myFunctionName";
        tName = globecBottle.makeNewFileForDapQuery(null, null, 
            "longitude,latitude&latitude>0&altitude>-5&time>=2002-08-03" + "&.jsonp=" + SSR.percentEncode(jsonp),
            EDStatic.fullTestCacheDirectory, globecBottle.className() + "_DataGJLL", ".geoJson"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = jsonp + "(" +
"{\n" +
"  \"type\": \"MultiPoint\",\n" +
"  \"coordinates\": [\n" +
"\n" +
"[-124.4, 44.0],\n" +
"[-124.4, 44.0],\n";
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

expected = 
"[-124.1, 44.65]\n" +
"\n" +
"  ],\n" +
"  \"bbox\": [-126.0, 41.9, -124.1, 44.65]\n" +
"}\n" +
")"; 
        tResults = results.substring(results.length() - expected.length());
        Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);


        //.htmlTable
        tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Data", ".htmlTable"); 
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
EDStatic.startHeadHtml(EDStatic.erddapUrl((String)null), "EDDTableFromNcFiles_Data") + "\n" +
"</head>\n" +
EDStatic.startBodyHtml(null) + "\n" +
"&nbsp;\n" +
"<form action=\"\">\n" +
"<input type=\"button\" value=\"Back\" onClick=\"history.go(-1);return true;\">\n" +
"</form>\n" +
"\n" +
"&nbsp;\n" +
"<table class=\"erd commonBGColor\" cellspacing=\"0\">\n" +
"<tr>\n" +
"<th>longitude\n" +
"<th>NO3\n" +
"<th>time\n" +
"<th>ship\n" +
"</tr>\n" +
"<tr>\n" +
"<th>degrees_east\n" +
"<th>micromoles L-1\n" +
"<th>UTC\n" +
"<th>&nbsp;\n" + //note &nbsp;
"</tr>\n" +
"<tr>\n" +
"<td nowrap align=\"right\">-124.4\n" +
"<td align=\"right\">35.7\n" +
"<td nowrap>2002-08-03T01:29:00Z\n" +
"<td nowrap>New_Horizon\n" +
"</tr>\n";
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected,  "\ntResults=\n" + tResults);
        expected =  //row with missing value  has "&nbsp;" missing value
"<tr>\n" +
"<td nowrap align=\"right\">-124.1\n" +
"<td align=\"right\">24.45\n" +
"<td nowrap>2002-08-19T20:18:00Z\n" +
"<td nowrap>New_Horizon\n" +
"</tr>\n" +
"</table>\n" +
EDStatic.endBodyHtml(EDStatic.erddapUrl((String)null)) + "\n" +
"</html>\n";
        tResults = results.substring(results.length() - expected.length());
        Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

        //.json
        tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Data", ".json"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"{\n" +
"  \"table\": {\n" +
"    \"columnNames\": [\"longitude\", \"NO3\", \"time\", \"ship\"],\n" +
"    \"columnTypes\": [\"float\", \"float\", \"String\", \"String\"],\n" +
"    \"columnUnits\": [\"degrees_east\", \"micromoles L-1\", \"UTC\", null],\n" +
"    \"rows\": [\n" +
"      [-124.4, 35.7, \"2002-08-03T01:29:00Z\", \"New_Horizon\"],\n" +
"      [-124.4, 35.48, \"2002-08-03T01:29:00Z\", \"New_Horizon\"],\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = 
"      [-125.0, null, \"2002-08-18T13:03:00Z\", \"New_Horizon\"],\n"; //row with missing value  has "null"
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = 
        "      [-124.1, 24.45, \"2002-08-19T20:18:00Z\", \"New_Horizon\"]\n" +
        "    ]\n" +
        "  }\n" +
        "}\n"; //last rows
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);

        //.json  with jsonp query
        tName = globecBottle.makeNewFileForDapQuery(null, null, 
            userDapQuery + "&.jsonp=" + SSR.percentEncode(jsonp), 
            EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Data", ".json"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = jsonp + "(" +
"{\n" +
"  \"table\": {\n" +
"    \"columnNames\": [\"longitude\", \"NO3\", \"time\", \"ship\"],\n" +
"    \"columnTypes\": [\"float\", \"float\", \"String\", \"String\"],\n" +
"    \"columnUnits\": [\"degrees_east\", \"micromoles L-1\", \"UTC\", null],\n" +
"    \"rows\": [\n" +
"      [-124.4, 35.7, \"2002-08-03T01:29:00Z\", \"New_Horizon\"],\n" +
"      [-124.4, 35.48, \"2002-08-03T01:29:00Z\", \"New_Horizon\"],\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = 
        "      [-124.1, 24.45, \"2002-08-19T20:18:00Z\", \"New_Horizon\"]\n" +
        "    ]\n" +
        "  }\n" +
        "}\n" +
        ")"; //last rows  
        Test.ensureEqual(results.substring(results.length() - expected.length()), 
            expected, "\nresults=\n" + results);
 
        //.mat     [I can't test that missing value is NaN.]
        //octave> load('c:/temp/tabledap/EDDTableFromNcFiles_Data.mat');
        //octave> testGlobecBottle
        //2010-07-14 Roy can read this file in Matlab, previously. text didn't show up.
        tName = globecBottle.makeNewFileForDapQuery(null, null, regexDapQuery, EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Data", ".mat"); 
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        results = File2.hexDump(EDStatic.fullTestCacheDirectory + tName, 1000000);
        //String2.log(results);
        Test.ensureEqual(
            results.substring(0, 71 * 4) + results.substring(71 * 7), //remove the creation dateTime
"4d 41 54 4c 41 42 20 35   2e 30 20 4d 41 54 2d 66   MATLAB 5.0 MAT-f |\n" +
"69 6c 65 2c 20 43 72 65   61 74 65 64 20 62 79 3a   ile, Created by: |\n" +
"20 67 6f 76 2e 6e 6f 61   61 2e 70 66 65 6c 2e 63    gov.noaa.pfel.c |\n" +
"6f 61 73 74 77 61 74 63   68 2e 4d 61 74 6c 61 62   oastwatch.Matlab |\n" +
//"2c 20 43 72 65 61 74 65   64 20 6f 6e 3a 20 4d 6f   , Created on: Mo |\n" +
//"6e 20 44 65 63 20 38 20   31 32 3a 34 35 3a 34 36   n Dec 8 12:45:46 |\n" +
//"20 32 30 30 38 20 20 20   20 20 20 20 20 20 20 20    2008            |\n" +
"20 20 20 20 00 00 00 00   00 00 00 00 01 00 4d 49                 MI |\n" +
"00 00 00 0e 00 00 04 58   00 00 00 06 00 00 00 08          X         |\n" +
"00 00 00 02 00 00 00 00   00 00 00 05 00 00 00 08                    |\n" +
"00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 10                    |\n" +
"74 65 73 74 47 6c 6f 62   65 63 42 6f 74 74 6c 65   testGlobecBottle |\n" +
"00 04 00 05 00 00 00 20   00 00 00 01 00 00 00 80                    |\n" +
"6c 6f 6e 67 69 74 75 64   65 00 00 00 00 00 00 00   longitude        |\n" +
"00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
"4e 4f 33 00 00 00 00 00   00 00 00 00 00 00 00 00   NO3              |\n" +
"00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
"74 69 6d 65 00 00 00 00   00 00 00 00 00 00 00 00   time             |\n" +
"00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
"73 68 69 70 00 00 00 00   00 00 00 00 00 00 00 00   ship             |\n" +
"00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
"00 00 00 0e 00 00 00 78   00 00 00 06 00 00 00 08          x         |\n" +
"00 00 00 07 00 00 00 00   00 00 00 05 00 00 00 08                    |\n" +
"00 00 00 12 00 00 00 01   00 00 00 01 00 00 00 00                    |\n" +
"00 00 00 07 00 00 00 48   c2 fa 38 52 c2 fa 38 52          H  8R  8R |\n" +
"c2 fa 38 52 c2 fa 38 52   c2 fa 38 52 c2 fa 38 52     8R  8R  8R  8R |\n" +
"c2 fa 38 52 c2 fa 38 52   c2 fa 38 52 c2 fa 38 52     8R  8R  8R  8R |\n" +
"c2 fa 38 52 c2 fa 38 52   c2 fa 38 52 c2 fa 38 52     8R  8R  8R  8R |\n" +
"c2 fa 38 52 c2 fa 38 52   c2 fa 38 52 c2 fa 38 52     8R  8R  8R  8R |\n" +
"00 00 00 0e 00 00 00 78   00 00 00 06 00 00 00 08          x         |\n" +
"00 00 00 07 00 00 00 00   00 00 00 05 00 00 00 08                    |\n" +
"00 00 00 12 00 00 00 01   00 00 00 01 00 00 00 00                    |\n" +
"00 00 00 07 00 00 00 48   42 07 a3 d7 41 d4 e1 48          HB   A  H |\n" +
"41 2c cc cd 41 06 b8 52   40 ca e1 48 3f a5 1e b8   A,  A  R@  H?    |\n" +
"3c a3 d7 0a 00 00 00 00   41 2c f5 c3 42 29 8f 5c   <       A,  B) \\ |\n" +
"42 07 5c 29 41 dd 5c 29   41 7e e1 48 41 0b 0a 3d   B \\)A \\)A~ HA  = |\n" +
"40 93 33 33 40 0a e1 48   41 09 c2 8f 3f 23 d7 0a   @ 33@  HA   ?#   |\n" +
"00 00 00 0e 00 00 00 c0   00 00 00 06 00 00 00 08                    |\n" +
"00 00 00 06 00 00 00 00   00 00 00 05 00 00 00 08                    |\n" +
"00 00 00 12 00 00 00 01   00 00 00 01 00 00 00 00                    |\n" +
"00 00 00 09 00 00 00 90   41 ce a9 a6 82 00 00 00           A        |\n" +
"41 ce a9 a6 82 00 00 00   41 ce a9 a6 82 00 00 00   A       A        |\n" +
"41 ce a9 a6 82 00 00 00   41 ce a9 a6 82 00 00 00   A       A        |\n" +
"41 ce a9 a6 82 00 00 00   41 ce a9 a6 82 00 00 00   A       A        |\n" +
"41 ce a9 a6 82 00 00 00   41 ce a9 a6 82 00 00 00   A       A        |\n" +
"41 ce b0 19 36 00 00 00   41 ce b0 19 36 00 00 00   A   6   A   6    |\n" +
"41 ce b0 19 36 00 00 00   41 ce b0 19 36 00 00 00   A   6   A   6    |\n" +
"41 ce b0 19 36 00 00 00   41 ce b0 19 36 00 00 00   A   6   A   6    |\n" +
"41 ce b0 19 36 00 00 00   41 ce b0 19 36 00 00 00   A   6   A   6    |\n" +
"41 ce b0 19 36 00 00 00   00 00 00 0e 00 00 01 c0   A   6            |\n" +
"00 00 00 06 00 00 00 08   00 00 00 04 00 00 00 00                    |\n" +
"00 00 00 05 00 00 00 08   00 00 00 12 00 00 00 0b                    |\n" +
"00 00 00 01 00 00 00 00   00 00 00 04 00 00 01 8c                    |\n" +
"00 4e 00 4e 00 4e 00 4e   00 4e 00 4e 00 4e 00 4e    N N N N N N N N |\n" +
"00 4e 00 4e 00 4e 00 4e   00 4e 00 4e 00 4e 00 4e    N N N N N N N N |\n" +
"00 4e 00 4e 00 65 00 65   00 65 00 65 00 65 00 65    N N e e e e e e |\n" +
"00 65 00 65 00 65 00 65   00 65 00 65 00 65 00 65    e e e e e e e e |\n" +
"00 65 00 65 00 65 00 65   00 77 00 77 00 77 00 77    e e e e w w w w |\n" +
"00 77 00 77 00 77 00 77   00 77 00 77 00 77 00 77    w w w w w w w w |\n" +
"00 77 00 77 00 77 00 77   00 77 00 77 00 5f 00 5f    w w w w w w _ _ |\n" +
"00 5f 00 5f 00 5f 00 5f   00 5f 00 5f 00 5f 00 5f    _ _ _ _ _ _ _ _ |\n" +
"00 5f 00 5f 00 5f 00 5f   00 5f 00 5f 00 5f 00 5f    _ _ _ _ _ _ _ _ |\n" +
"00 48 00 48 00 48 00 48   00 48 00 48 00 48 00 48    H H H H H H H H |\n" +
"00 48 00 48 00 48 00 48   00 48 00 48 00 48 00 48    H H H H H H H H |\n" +
"00 48 00 48 00 6f 00 6f   00 6f 00 6f 00 6f 00 6f    H H o o o o o o |\n" +
"00 6f 00 6f 00 6f 00 6f   00 6f 00 6f 00 6f 00 6f    o o o o o o o o |\n" +
"00 6f 00 6f 00 6f 00 6f   00 72 00 72 00 72 00 72    o o o o r r r r |\n" +
"00 72 00 72 00 72 00 72   00 72 00 72 00 72 00 72    r r r r r r r r |\n" +
"00 72 00 72 00 72 00 72   00 72 00 72 00 69 00 69    r r r r r r i i |\n" +
"00 69 00 69 00 69 00 69   00 69 00 69 00 69 00 69    i i i i i i i i |\n" +
"00 69 00 69 00 69 00 69   00 69 00 69 00 69 00 69    i i i i i i i i |\n" +
"00 7a 00 7a 00 7a 00 7a   00 7a 00 7a 00 7a 00 7a    z z z z z z z z |\n" +
"00 7a 00 7a 00 7a 00 7a   00 7a 00 7a 00 7a 00 7a    z z z z z z z z |\n" +
"00 7a 00 7a 00 6f 00 6f   00 6f 00 6f 00 6f 00 6f    z z o o o o o o |\n" +
"00 6f 00 6f 00 6f 00 6f   00 6f 00 6f 00 6f 00 6f    o o o o o o o o |\n" +
"00 6f 00 6f 00 6f 00 6f   00 6e 00 6e 00 6e 00 6e    o o o o n n n n |\n" +
"00 6e 00 6e 00 6e 00 6e   00 6e 00 6e 00 6e 00 6e    n n n n n n n n |\n" +
"00 6e 00 6e 00 6e 00 6e   00 6e 00 6e 00 00 00 00    n n n n n n     |\n",
"\nresults=\n" + results);

        //.nc    
        //!!! This is also a test of missing_value and _FillValue both active
        String tUserDapQuery = "longitude,NO3,time,ship&latitude>0&altitude>-5&time>=2002-08-14&time<=2002-08-15";
        tName = globecBottle.makeNewFileForDapQuery(null, null, tUserDapQuery, 
            EDStatic.fullTestCacheDirectory, globecBottle.className() + "_Data", ".nc"); 
        results = NcHelper.dumpString(EDStatic.fullTestCacheDirectory + tName, true);
        String tHeader1 = 
"netcdf EDDTableFromNcFiles_Data.nc {\n" +
"  dimensions:\n" +
"    row = 100;\n" +
"    ship_strlen = 11;\n" +
"  variables:\n" +
"    float longitude(row=100);\n" +
"      :_CoordinateAxisType = \"Lon\";\n" +
"      :_FillValue = NaNf; // float\n" +
"      :actual_range = -125.67f, -124.8f; // float\n" +
"      :axis = \"X\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Longitude\";\n" +
"      :missing_value = NaNf; // float\n" +
"      :standard_name = \"longitude\";\n" +
"      :units = \"degrees_east\";\n" +
"\n" +
"    float NO3(row=100);\n" +
"      :_FillValue = -99.0f; // float\n" +
"      :actual_range = 0.46f, 34.09f; // float\n" +
"      :colorBarMaximum = 50.0; // double\n" +
"      :colorBarMinimum = 0.0; // double\n" +
"      :ioos_category = \"Dissolved Nutrients\";\n" +
"      :long_name = \"Nitrate\";\n" +
"      :missing_value = -9999.0f; // float\n" +
"      :standard_name = \"mole_concentration_of_nitrate_in_sea_water\";\n" +
"      :units = \"micromoles L-1\";\n" +
"\n" +
"    double time(row=100);\n" +
"      :_CoordinateAxisType = \"Time\";\n" +
"      :_FillValue = NaN; // double\n" +
"      :actual_range = 1.02928674E9, 1.02936804E9; // double\n" +
"      :axis = \"T\";\n" +
"      :cf_role = \"profile_id\";\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Time\";\n" +
"      :missing_value = NaN; // double\n" +
"      :standard_name = \"time\";\n" +  
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    char ship(row=100, ship_strlen=11);\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"Ship\";\n" +
"\n" +
"  // global attributes:\n" +
"  :cdm_data_type = \"TrajectoryProfile\";\n" +
"  :cdm_profile_variables = \"cast, longitude, latitude, time\";\n" +
"  :cdm_trajectory_variables = \"cruise_id, ship\";\n" +
"  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"  :Easternmost_Easting = -124.8f; // float\n" +
"  :featureType = \"TrajectoryProfile\";\n" +
"  :geospatial_lat_units = \"degrees_north\";\n" +
"  :geospatial_lon_max = -124.8f; // float\n" +
"  :geospatial_lon_min = -125.67f; // float\n" +
"  :geospatial_lon_units = \"degrees_east\";\n" +
"  :geospatial_vertical_positive = \"up\";\n" +
"  :geospatial_vertical_units = \"m\";\n" +
"  :history = \"" + today;
        tResults = results.substring(0, tHeader1.length());
        Test.ensureEqual(tResults, tHeader1, "\nresults=\n" + results);
        
//        + " http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle\n" +
//today + " " + EDStatic.erddapUrl + //in tests, always use non-https url
String tHeader2 = 
"/tabledap/testGlobecBottle.nc?longitude,NO3,time,ship&latitude>0&altitude>-5&time>=2002-08-14&time<=2002-08-15\";\n" +
"  :id = \"Globec_bottle_data_2002\";\n" +
"  :infoUrl = \"http://www.globec.org/\";\n" +
"  :institution = \"GLOBEC\";\n" +
"  :keywords = \"10um,\n" +
"Biosphere > Vegetation > Photosynthetically Active Radiation,\n" +
"Oceans > Ocean Chemistry > Ammonia,\n" +
"Oceans > Ocean Chemistry > Chlorophyll,\n" +
"Oceans > Ocean Chemistry > Nitrate,\n" +
"Oceans > Ocean Chemistry > Nitrite,\n" +
"Oceans > Ocean Chemistry > Nitrogen,\n" +
"Oceans > Ocean Chemistry > Oxygen,\n" +
"Oceans > Ocean Chemistry > Phosphate,\n" +
"Oceans > Ocean Chemistry > Pigments,\n" +
"Oceans > Ocean Chemistry > Silicate,\n" +
"Oceans > Ocean Optics > Attenuation/Transmission,\n" +
"Oceans > Ocean Temperature > Water Temperature,\n" +
"Oceans > Salinity/Density > Salinity,\n" +
"active, after, ammonia, ammonium, attenuation, biosphere, bottle, cast, chemistry, chlorophyll, chlorophyll-a, color, concentration, concentration_of_chlorophyll_in_sea_water, cruise, data, density, dissolved, dissolved nutrients, dissolved o2, fluorescence, fraction, from, globec, identifier, mass, mole, mole_concentration_of_ammonium_in_sea_water, mole_concentration_of_nitrate_in_sea_water, mole_concentration_of_nitrite_in_sea_water, mole_concentration_of_phosphate_in_sea_water, mole_concentration_of_silicate_in_sea_water, moles, moles_of_nitrate_and_nitrite_per_unit_mass_in_sea_water, n02, nep, nh4, nitrate, nitrite, nitrogen, no3, number, nutrients, o2, ocean, ocean color, oceans, optical, optical properties, optics, oxygen, passing, per, phaeopigments, phosphate, photosynthetically, pigments, plus, po4, properties, radiation, rosette, salinity, screen, sea, sea_water_practical_salinity, sea_water_temperature, seawater, sensor, sensors, ship, silicate, temperature, time, total, transmission, transmissivity, unit, vegetation, voltage, volume, volume_fraction_of_oxygen_in_sea_water, water\";\n" +
"  :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
"  :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"  :sourceUrl = \"(local files; contact erd.data@noaa.gov)\";\n" +
"  :standard_name_vocabulary = \"CF Standard Name Table v29\";\n" +
"  :subsetVariables = \"cruise_id, ship, cast, longitude, latitude, time\";\n" +
"  :summary = \"GLOBEC (GLOBal Ocean ECosystems Dynamics) NEP (Northeast Pacific)\n" +
"Rosette Bottle Data from New Horizon Cruise (NH0207: 1-19 August 2002).\n" +
"Notes:\n" +
"Physical data processed by Jane Fleischbein (OSU).\n" +
"Chlorophyll readings done by Leah Feinberg (OSU).\n" +
"Nutrient analysis done by Burke Hales (OSU).\n" +
"Sal00 - salinity calculated from primary sensors (C0,T0).\n" +
"Sal11 - salinity calculated from secondary sensors (C1,T1).\n" +
"secondary sensor pair was used in final processing of CTD data for\n" +
"most stations because the primary had more noise and spikes. The\n" +
"primary pair were used for cast #9, 24, 48, 111 and 150 due to\n" +
"multiple spikes or offsets in the secondary pair.\n" +
"Nutrient samples were collected from most bottles; all nutrient data\n" +
"developed from samples frozen during the cruise and analyzed ashore;\n" +
"data developed by Burke Hales (OSU).\n" +
"Operation Detection Limits for Nutrient Concentrations\n" +
"Nutrient  Range         Mean    Variable         Units\n" +
"PO4       0.003-0.004   0.004   Phosphate        micromoles per liter\n" +
"N+N       0.04-0.08     0.06    Nitrate+Nitrite  micromoles per liter\n" +
"Si        0.13-0.24     0.16    Silicate         micromoles per liter\n" +
"NO2       0.003-0.004   0.003   Nitrite          micromoles per liter\n" +
"Dates and Times are UTC.\n" +
"\n" +
"For more information, see\n" +
"http://cis.whoi.edu/science/bcodmo/dataset.cfm?id=10180&flag=view\n" +
"\n" +
"Inquiries about how to access this data should be directed to\n" +
"Dr. Hal Batchelder (hbatchelder@coas.oregonstate.edu).\";\n" +
"  :time_coverage_end = \"2002-08-14T23:34:00Z\";\n" +
"  :time_coverage_start = \"2002-08-14T00:59:00Z\";\n" +
"  :title = \"GLOBEC NEP Rosette Bottle Data (2002)\";\n" +
"  :Westernmost_Easting = -125.67f; // float\n" +
" data:\n";
        int tPo = results.indexOf(tHeader2.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(results.substring(tPo, tPo + tHeader2.length()), tHeader2, 
            "results=\n" + results);

expected = 
"longitude =\n" +
"  {-124.8, -124.8, -124.8, -124.8, -124.8, -124.8, -124.8, -124.8, -124.9, -124.9, -124.9, -124.9, -124.9, -124.9, -124.9, -124.9, -124.9, -125.0, -125.0, -125.0, -125.0, -125.0, -125.0, -125.0, -125.0, -125.0, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2, -125.43, -125.43, -125.43, -125.43, -125.43, -125.43, -125.43, -125.43, -125.43, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.66, -125.66, -125.66, -125.66, -125.66, -125.66, -125.66, -125.66, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.5, -125.5, -125.5, -125.5, -125.5, -125.5, -125.5, -125.5, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2}\n" +
"NO3 =\n" +
"  {33.66, 30.43, 28.22, 26.4, 25.63, 23.54, 22.38, 20.15, 33.55, 31.48, 24.93, -99.0, 21.21, 20.54, 17.87, -9999.0, 16.32, 33.61, 33.48, 30.7, 27.05, 25.13, 24.5, 23.95, 16.0, 14.42, 33.28, 28.3, 26.74, 24.96, 23.78, 20.76, 17.72, 16.01, 31.22, 27.47, 13.28, 10.66, 9.61, 8.36, 6.53, 2.86, 0.96, 34.05, 29.47, 18.87, 15.17, 13.84, 9.61, 4.95, 3.46, 34.09, 23.29, 16.01, 10.35, 7.72, 4.37, 2.97, 27.25, 29.98, 22.56, 9.82, 9.19, 6.57, 5.23, 3.81, 0.96, 30.08, 19.88, 8.44, 4.59, 2.67, 1.53, 0.94, 0.47, 30.73, 20.28, 10.61, 7.48, 6.53, 4.51, 3.04, 1.36, 0.89, 32.21, 23.75, 12.04, 7.67, 5.73, 1.14, 1.02, 0.46, 33.16, 27.33, 15.16, 9.7, 9.47, 8.66, 7.65, 4.84}\n" +
"time =\n" +
"  {1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936804E9, 1.02936804E9, 1.02936804E9, 1.02936804E9, 1.02936804E9, 1.02936804E9, 1.02936804E9, 1.02936804E9}\n" +
"ship =\"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\"\n" +
"}\n";
        tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);

        //.ncHeader
        tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Data", ".ncHeader"); 
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        String2.log(results);

        tResults = results.substring(0, tHeader1.length());
        Test.ensureEqual(tResults, tHeader1, "\nresults=\n" + results);

        expected = tHeader2 + "}\n";
        tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);


        //.odvTxt
        try {
            tName = globecBottle.makeNewFileForDapQuery(null, null, 
                "&latitude>0&time>=2002-08-03", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_ODV", ".odvTxt"); 
            String2.log("ODV fileName=" + EDStatic.fullTestCacheDirectory + tName);
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
    "//<Creator>http://www.globec.org/</Creator>\n" +
    "//<CreateTime>" + today;
            Test.ensureEqual(results.substring(0, expected.length()), expected, 
                "\nresults=\n" + String2.annotatedString(results));


    //T21:09:15
            expected = 
//"//<Creator>http://www.globec.org/</Creator>\n" +
//"//<CreateTime>2013-03-22T17:52:05</CreateTime>\n" +
"//<Software>ERDDAP - Version " + EDStatic.erddapVersion + "</Software>\n" +
"//<Source>http://127.0.0.1:8080/cwexperimental/tabledap/testGlobecBottle.html</Source>\n" +
"//<Version>ODV Spreadsheet V4.0</Version>\n" +
"//<DataField>GeneralField</DataField>\n" +
"//<DataType>GeneralType</DataType>\n" +
"Type:METAVAR:TEXT:2\tStation:METAVAR:TEXT:2\tCruise:METAVAR:TEXT:7\tship:METAVAR:TEXT:12\tcast:SHORT\tLongitude [degrees_east]:METAVAR:FLOAT\tLatitude [degrees_north]:METAVAR:FLOAT\taltitude [m]:PRIMARYVAR:INTEGER\tyyyy-mm-ddThh:mm:ss.sss\tbottle_posn:BYTE\tchl_a_total [ug L-1]:FLOAT\tchl_a_10um [ug L-1]:FLOAT\tphaeo_total [ug L-1]:FLOAT\tphaeo_10um [ug L-1]:FLOAT\tsal00 [PSU]:FLOAT\tsal11 [PSU]:FLOAT\ttemperature0 [degree_C]:FLOAT\ttemperature1 [degree_C]:FLOAT\tfluor_v [volts]:FLOAT\txmiss_v [volts]:FLOAT\tPO4 [micromoles L-1]:FLOAT\tN_N [micromoles L-1]:FLOAT\tNO3 [micromoles L-1]:FLOAT\tSi [micromoles L-1]:FLOAT\tNO2 [micromoles L-1]:FLOAT\tNH4 [micromoles L-1]:FLOAT\toxygen [mL L-1]:FLOAT\tpar [volts]:FLOAT\n" +
"*\t\tnh0207\tNew_Horizon\t20\t-124.4\t44.0\t0\t2002-08-03T01:29:00Z\t1\t\t\t\t\t33.9939\t33.9908\t7.085\t7.085\t0.256\t0.518\t2.794\t35.8\t35.7\t71.11\t0.093\t0.037\t\t0.1545\n" +
"*\t\tnh0207\tNew_Horizon\t20\t-124.4\t44.0\t0\t2002-08-03T01:29:00Z\t2\t\t\t\t\t33.8154\t33.8111\t7.528\t7.53\t0.551\t0.518\t2.726\t35.87\t35.48\t57.59\t0.385\t0.018\t\t0.1767\n" +
"*\t\tnh0207\tNew_Horizon\t20\t-124.4\t44.0\t0\t2002-08-03T01:29:00Z\t3\t1.463\t\t1.074\t\t33.5858\t33.5834\t7.572\t7.573\t0.533\t0.518\t2.483\t31.92\t31.61\t48.54\t0.307\t0.504\t\t0.3875\n";
            po = results.indexOf(expected.substring(0, 13));
            Test.ensureEqual(results.substring(po, po + expected.length()), expected, 
                "\nresults=\n" + String2.annotatedString(results));
        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nUnexpected error."); 
        }


        //.tsv
        tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Data", ".tsv"); 
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude\tNO3\ttime\tship\n" +
"degrees_east\tmicromoles L-1\tUTC\t\n" +
"-124.4\t35.7\t2002-08-03T01:29:00Z\tNew_Horizon\n";
        Test.ensureTrue(results.indexOf(expected) == 0, "\nresults=\n" + results);
        expected = "-124.8\tNaN\t2002-08-03T07:17:00Z\tNew_Horizon\n"; //row with missing value  has "NaN" missing value
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = "-124.1\t24.45\t2002-08-19T20:18:00Z\tNew_Horizon\n"; //last row
        Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

        //.tsvp
        tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Data", ".tsvp"); 
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude (degrees_east)\tNO3 (micromoles L-1)\ttime (UTC)\tship\n" +
"-124.4\t35.7\t2002-08-03T01:29:00Z\tNew_Horizon\n";
        Test.ensureTrue(results.indexOf(expected) == 0, "\nresults=\n" + results);
        expected = "-124.8\tNaN\t2002-08-03T07:17:00Z\tNew_Horizon\n"; //row with missing value  has "NaN" missing value
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = "-124.1\t24.45\t2002-08-19T20:18:00Z\tNew_Horizon\n"; //last row
        Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

        //.tsv0
        tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Data", ".tsv0"); 
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"-124.4\t35.7\t2002-08-03T01:29:00Z\tNew_Horizon\n";
        Test.ensureTrue(results.indexOf(expected) == 0, "\nresults=\n" + results);
        expected = "-124.8\tNaN\t2002-08-03T07:17:00Z\tNew_Horizon\n"; //row with missing value  has "NaN" missing value
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = "-124.1\t24.45\t2002-08-19T20:18:00Z\tNew_Horizon\n"; //last row
        Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

        //.xhtml
        tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Data", ".xhtml"); 
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
"  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
"<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
"<head>\n" +
"  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n" +
"  <title>EDDTableFromNcFiles_Data</title>\n" +
"</head>\n" +
"<body style=\"color:black; background:white; font-family:Arial,Helvetica,sans-serif; font-size:85%; line-height:130%;\">\n" +
"\n" +
"&nbsp;\n" +
"<table border=\"1\" cellpadding=\"2\" cellspacing=\"0\">\n" +
"<tr>\n" +
"<th>longitude</th>\n" +
"<th>NO3</th>\n" +
"<th>time</th>\n" +
"<th>ship</th>\n" +
"</tr>\n" +
"<tr>\n" +
"<th>degrees_east</th>\n" +
"<th>micromoles L-1</th>\n" +
"<th>UTC</th>\n" +
"<th></th>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\" align=\"right\">-124.4</td>\n" +
"<td align=\"right\">35.7</td>\n" +
"<td nowrap=\"nowrap\">2002-08-03T01:29:00Z</td>\n" +
"<td nowrap=\"nowrap\">New_Horizon</td>\n" +
"</tr>\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected =  //row with missing value  has "" missing value
"<tr>\n" +
"<td nowrap=\"nowrap\" align=\"right\">-124.1</td>\n" +
"<td align=\"right\">24.45</td>\n" +
"<td nowrap=\"nowrap\">2002-08-19T20:18:00Z</td>\n" +
"<td nowrap=\"nowrap\">New_Horizon</td>\n" +
"</tr>\n" +
"</table>\n" +
"</body>\n" +
"</html>\n";
        tResults = results.substring(results.length() - expected.length());
        Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

        //data for mapExample
        tName = globecBottle.makeNewFileForDapQuery(null, null, 
            "longitude,latitude&time>=2002-08-03&time<=2002-08-04", 
            EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "Map", ".csv");
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude,latitude\n" +
"degrees_east,degrees_north\n" +
"-124.4,44.0\n" +
"-124.6,44.0\n" +
"-124.8,44.0\n" +
"-125.0,44.0\n" +
"-125.2,44.0\n" +
"-125.4,44.0\n" +
"-125.6,43.8\n" +
"-125.86,43.5\n" +
"-125.63,43.5\n" +
"-125.33,43.5\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);  


        try {

            //test treat itself as a dataset
            EDDTable eddTable2 = new EDDTableFromDapSequence(
                "erddapGlobecBottle", //String tDatasetID, 
                null, null, null, null, null, "", "", null,
                new Object[][]{  //dataVariables: sourceName, addAttributes
                    {"longitude", null, null},
                    {"latitude", null, null},
                    {"altitude", null, null},
                    {"time", null, null},
                    {"ship", null, null},
                    {"cruise_id", null, null},
                    {"cast", null, null},
                    {"bottle_posn", null, null},
                    {"chl_a_total", null, null},
                    {"chl_a_10um", null, null},
                    {"phaeo_total", null, null},
                    {"phaeo_10um", null, null},
                    {"sal00", null, null},
                    {"sal11", null, null}, 
                    {"temperature0", null, null},  
                    {"temperature1", null, null}, 
                    {"fluor_v", null, null},
                    {"xmiss_v", null, null},
                    {"PO4", null, null},
                    {"N_N", null, null},
                    {"NO3", null, null},
                    {"Si", null, null},
                    {"NO2", null, null},
                    {"NH4", null, null},
                    {"oxygen", null, null},
                    {"par", null, null}},
                60, //int tReloadEveryNMinutes,
                EDStatic.erddapUrl + //in tests, always use non-https url
                    "/tabledap/testGlobecBottle", //sourceUrl);
                "s", null, //outerSequenceName innerSequenceName
                true, //NeedsExpandedFP_EQ
                true, //sourceCanConstrainStringEQNE
                true, //sourceCanConstrainStringGTLT
                PrimitiveArray.REGEX_OP,
                false); 

            //.xhtml from local dataset made from Erddap
            tName = eddTable2.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
                eddTable2.className() + "_Itself", ".xhtml"); 
            //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
"  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
"<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
"<head>\n" +
"  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n" +
"  <title>EDDTableFromDapSequence_Itself</title>\n" +
"</head>\n" +
"<body style=\"color:black; background:white; font-family:Arial,Helvetica,sans-serif; font-size:85%; line-height:130%;\">\n" +
"\n" +
"&nbsp;\n" +
"<table border=\"1\" cellpadding=\"2\" cellspacing=\"0\">\n" +
"<tr>\n" +
"<th>longitude</th>\n" +
"<th>NO3</th>\n" +
"<th>time</th>\n" +
"<th>ship</th>\n" +
"</tr>\n" +
"<tr>\n" +
"<th>degrees_east</th>\n" +
"<th>micromoles L-1</th>\n" +
"<th>UTC</th>\n" +
"<th></th>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\" align=\"right\">-124.4</td>\n" +
"<td align=\"right\">35.7</td>\n" +
"<td nowrap=\"nowrap\">2002-08-03T01:29:00Z</td>\n" +
"<td nowrap=\"nowrap\">New_Horizon</td>\n" +
"</tr>\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nError creating a dataset from the dataset at " + 
                EDStatic.erddapUrl); //in tests, always use non-https url                
        }
        // */

    } //end of testBasic

    /**
     * Test saveAsKml.
     */
    public static void testKml() throws Throwable {
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String mapDapQuery = "longitude,latitude,NO3,time&latitude>0&altitude>-5&time>=2002-08-03";

        String2.log("\n****************** EDDTableFromNcFiles.testKml\n");
        EDDTable globecBottle = (EDDTable)oneFromDatasetsXml(null, "testGlobecBottle"); //should work

        //kml
        tName = globecBottle.makeNewFileForDapQuery(null, null, mapDapQuery, 
            EDStatic.fullTestCacheDirectory, globecBottle.className() + "_MapKml", ".kml"); 
        //String2.log(String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1]);
        SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    }

    /**
     * The basic graphics tests of this class (testGlobecBottle).
     */
    public static void testGraphics(boolean doAll) throws Throwable {
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String mapDapQuery = "longitude,latitude,NO3,time&latitude>0&altitude>-5&time>=2002-08-03";
        userDapQuery = "longitude,NO3,time,ship&latitude>0&altitude>-5&time>=2002-08-03";

        String2.log("\n****************** EDDTableFromNcFiles.testGraphics\n");
        EDDTable globecBottle = (EDDTable)oneFromDatasetsXml(null, "testGlobecBottle"); //should work

            //kml
            tName = globecBottle.makeNewFileForDapQuery(null, null, mapDapQuery, 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_MapKml", ".kml"); 
            //String2.log(String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1]);
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

        if (doAll) {

            //*** test make graphs
            //there is no .transparentPng for EDDTable

            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery + "&.size=128|256&.font=.75", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphTiny", 
                ".largePng"); //to show it is irrelevant
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphS", ".smallPng"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphM", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphL", ".largePng"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery + "&.size=1700|1800", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphHuge", 
                ".smallPng"); //to show it is irrelevant
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphPdfSmall", ".smallPdf"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphPdf", ".pdf"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphPdfLarge", ".largePdf"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);


            //*** test make MAP
            String2.log("\n******************* EDDTableFromNcFiles.test make MAP\n");
            tName = globecBottle.makeNewFileForDapQuery(null, null, mapDapQuery, 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_MapS", ".smallPng"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, mapDapQuery, 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_MapM", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, mapDapQuery, 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_MapL", ".largePng"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, mapDapQuery, 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_MapS", ".smallPdf"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, mapDapQuery, 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_MapM", ".pdf"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, mapDapQuery, 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_MapL", ".largePdf"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
  

            //kml
            tName = globecBottle.makeNewFileForDapQuery(null, null, mapDapQuery, 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_MapKml", ".kml"); 
            //String2.log(String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1]);
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);


            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery + "&.legend=Off&.trim=10", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphMLegendOff", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery + "&.legend=Only", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphSLegendOnly", ".smallPng"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery + "&.legend=Only", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphMLegendOnly", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery + "&.legend=Only", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphLLegendOnly", ".largePng"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);


        }


         
        //test of .graphics commands
        if (true) {
            tQuery = "NO3,NH4&altitude>-5&time>=2002-08-03&NO3>=0";
            tName = globecBottle.makeNewFileForDapQuery(null, null, tQuery + 
                //I specify colorBar, but it isn't used
                "&.draw=markers&.marker=1|5&.color=0x0000FF&.colorBar=Rainbow|C|Linear", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphWithMarkersNoColorBar", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tQuery = "NO3,NH4,sal00&altitude>-5&time>=2002-08-03&NO3>=0";
            tName = globecBottle.makeNewFileForDapQuery(null, null, tQuery + 
                "&.draw=lines&.marker=9|7", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphWithLines", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tQuery = "NO3,NH4,sal00&altitude>-5&time>=2002-08-03&NO3>=0";
            tName = globecBottle.makeNewFileForDapQuery(null, null, tQuery + 
                "&.draw=linesAndMarkers&.marker=9|7&.colorBar=Rainbow|C|Linear", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphWithLinesAndMarkers", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tQuery = "NO3,NH4,sal00&altitude>-5&time>=2002-08-03&NO3>=0";
            tName = globecBottle.makeNewFileForDapQuery(null, null, tQuery + 
                //color and colorBar aren't specified; default is used
                "&.draw=markers&.marker=9|7", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphWithMarkers", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tQuery = "longitude,latitude,sal00&altitude>-5&time>=2002-08-03";
            tName = globecBottle.makeNewFileForDapQuery(null, null, tQuery + 
                "&.draw=lines&.color=0xFF8800", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_MapWithLines", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tQuery = "longitude,latitude,sal00&altitude>-5&time>=2002-08-03";
            tName = globecBottle.makeNewFileForDapQuery(null, null, tQuery + 
                "&.draw=linesAndMarkers&.marker=5|5&.colorBar=Rainbow|D|Linear", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_MapWithLinesAndMarkers", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tQuery = "longitude,latitude,sal00&altitude>-5&time>=2002-08-03";
            tName = globecBottle.makeNewFileForDapQuery(null, null, tQuery + 
                "&.draw=markers&.marker=5|5&.colorBar=Rainbow|D|Linear", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_MapWithMarkers", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tQuery = "time,sal00,sal11&altitude>-5&time>=2002-08-03&NO3>=0";
            tName = globecBottle.makeNewFileForDapQuery(null, null, tQuery + 
                "&.draw=sticks&.color=0xFF8800", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphWithSticks", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tQuery = "longitude,latitude,sal00,sal11&altitude>-5&time>=2002-08-03";
            tName = globecBottle.makeNewFileForDapQuery(null, null, tQuery + 
                "&.draw=vectors&.color=0xFF0088&.vec=30", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_MapWithVectors", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tQuery = "longitude,latitude,sal00,sal11&altitude>-5&time>=2002-08-03&cast>200";
            tName = globecBottle.makeNewFileForDapQuery(null, null, tQuery + 
                "&.draw=vectors&.color=0xFF0088&.vec=30", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_MapWithVectorsNoData", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        }

    }

    public static void testNetcdf() throws Throwable {

        //use testGlobecBottle which has fixed altitude=0, not erdGlobecBottle
        EDDTable globecBottle = (EDDTableFromNcFiles)oneFromDatasetsXml(null, "testGlobecBottle"); //should work
        String tUrl = EDStatic.erddapUrl + //in tests, always use non-https url
            "/tabledap/" + globecBottle.datasetID;
        String mapDapQuery = "longitude,latitude,NO3,time&latitude>0&altitude>-5&time>=2002-08-03";
        String results, expected;

        //TEST READING AS OPENDAP SERVER -- how specify constraint expression?
        //test reading via netcdf-java    similar to .dods test
        //tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, 
        //    EDStatic.fullTestCacheDirectory, globecBottle.className() + "_Data", ".dods"); 
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        try {
            String2.log("\n*** do netcdf-java opendap test");
            //!!!THIS READS DATA FROM LOCAL ERDDAP SERVER RUNNING ON EDStatic.erddapUrl!!! //in tests, always use non-https url                
            //!!!THIS IS NOT JUST A READ-FROM-FILE TEST!!!
            NetcdfFile nc = NetcdfDataset.openFile(tUrl, null);
            try {
                results = nc.toString();
                expected = 
"netcdf " + String2.replaceAll(EDStatic.erddapUrl, "http:" , "dods:") + //in tests, always use non-https url
        "/tabledap/testGlobecBottle {\n" +
" variables:\n" +
"\n" + //2009-02-26 this line was added with switch to netcdf-java 4.0
"   Structure {\n" +
"     float longitude;\n" +
"       :_CoordinateAxisType = \"Lon\";\n" +
"       :actual_range = -126.2f, -124.1f; // float\n" +
"       :axis = \"X\";\n" +
"       :colorBarMaximum = -115.0; // double\n" +
"       :colorBarMinimum = -135.0; // double\n" +
"       :ioos_category = \"Location\";\n" +
"       :long_name = \"Longitude\";\n" +
"       :standard_name = \"longitude\";\n" +
"       :units = \"degrees_east\";\n" +
"     float latitude;\n" +
"       :_CoordinateAxisType = \"Lat\";\n" +
"       :actual_range = 41.9f, 44.65f; // float\n" +
"       :axis = \"Y\";\n" +
"       :colorBarMaximum = 55.0; // double\n" +
"       :colorBarMinimum = 30.0; // double\n" +
"       :ioos_category = \"Location\";\n" +
"       :long_name = \"Latitude\";\n" +
"       :standard_name = \"latitude\";\n" +
"       :units = \"degrees_north\";\n" +
"     int altitude;\n" +
"       :_CoordinateAxisType = \"Height\";\n" +
"       :_CoordinateZisPositive = \"up\";\n" +
"       :actual_range = 0, 0; // int\n" +
"       :axis = \"Z\";\n" +
"       :ioos_category = \"Location\";\n" +
"       :long_name = \"Altitude\";\n" +
"       :positive = \"up\";\n" +
"       :standard_name = \"altitude\";\n" +
"       :units = \"m\";\n" +
"     double time;\n" +
"       :_CoordinateAxisType = \"Time\";\n";
                //odd: the spaces don't show up in console window
                Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);

                expected = 
" :time_coverage_start = \"2002-05-30T03:21:00Z\";\n" +
" :title = \"GLOBEC NEP Rosette Bottle Data (2002)\";\n" +
" :Westernmost_Easting = -126.2; // double\n" +
"}\n";
                Test.ensureEqual(results.substring(results.indexOf(" :time_coverage_start")), expected, "RESULTS=\n" + results);

                Attributes attributes = new Attributes();
                NcHelper.getGlobalAttributes(nc, attributes);
                Test.ensureEqual(attributes.getString("title"), "GLOBEC NEP Rosette Bottle Data (2002)", "");

                //get attributes for a dimension 
                Variable ncLat = nc.findVariable("s.latitude");
                attributes.clear();
                NcHelper.getVariableAttributes(ncLat, attributes);
                Test.ensureEqual(attributes.getString("units"), EDV.LAT_UNITS, "");

                //get attributes for grid variable
                Variable ncChl = nc.findVariable("s.chl_a_total");
                attributes.clear();
                NcHelper.getVariableAttributes(ncChl, attributes);
                Test.ensureEqual(attributes.getString("standard_name"),"concentration_of_chlorophyll_in_sea_water", "");

                //get sequence data
                //it's awkward. Do later if needed.
                //???How specify constraint expression?

            } finally {
                nc.close();
            }

        } catch (Throwable t) {
            Test.knownProblem(
                "!!!!! EEEK: opendap bytes are 0 - 255, Java/ERDDAP bytes are -128 to 127 !",
                MustBe.throwableToString(t) + 
                "\nError accessing " + EDStatic.erddapUrl + //in tests, always use non-https url
                " via netcdf-java."); 
        }

        //OTHER APPROACH: GET .NC FILE  -- HOW SPECIFY CONSTRAINT EXPRESSION???
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        if (false) {
            try {
                String2.log("\n*** do netcdf-java .nc test");
                //!!!THIS READS DATA FROM ERDDAP SERVER RUNNING ON COASTWATCH CWEXPERIMENTAL!!!
                //!!!THIS IS NOT JUST A LOCAL TEST!!!
                NetcdfFile nc = NetcdfDataset.openFile(tUrl + ".nc?" + mapDapQuery, null);
                try {
                    results = nc.toString();
                    expected = "zz";
                    Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);

                } finally {
                    nc.close();
                }

            } catch (Throwable t) {
                String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                    "\nError accessing " + EDStatic.erddapUrl + //in tests, always use non-https url
                    " via netcdf-java."); 
            }
        }       
    }

    /**
     * erdGlobecBird flight_dir uses scale_factor=10 add_offset=0, so a good test of scaleAddOffset.
     */
    public static void testGlobecBirds() throws Throwable {
        testVerboseOn();
        String results, query, tName, expected;
        String baseQuery = "&time>=2000-08-07&time<2000-08-08"; 
        EDDTable tedd = (EDDTable)oneFromDatasetsXml(null, "erdGlobecBirds");

        //the basicQuery
        try {
            //an easy query
            tName = tedd.makeNewFileForDapQuery(null, null, baseQuery, EDStatic.fullTestCacheDirectory, 
                tedd.className() + "_bird1", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"trans_no,trans_id,longitude,latitude,time,area,behav_code,flight_dir,head_c,number,number_adj,species,wspd\n" +
",,degrees_east,degrees_north,UTC,km2,,degrees_true,degrees_true,count,count,,knots\n" +
"22001,8388607,-125.023,43.053,2000-08-07T00:00:00Z,1.3,1,180,240,1,0.448,SHSO,15\n" +
"22001,8388607,-125.023,43.053,2000-08-07T00:00:00Z,1.3,2,0,240,1,1.0,FUNO,15\n" +
"22001,8388607,-125.023,43.053,2000-08-07T00:00:00Z,1.3,3,0,240,1,1.0,SKMA,15\n" +
"22005,8388607,-125.225,43.955,2000-08-07T00:00:00Z,1.3,2,0,240,3,3.0,AKCA,15\n" +
"22009,8388607,-125.467,43.84,2000-08-07T00:00:00Z,1.3,1,200,240,2,0.928,PHRE,20\n" +
"22013,8388607,-125.648,43.768,2000-08-07T00:00:00Z,1.3,1,270,240,1,1.104,STLE,20\n" +
"22015,8388607,-125.745,43.73,2000-08-07T00:00:00Z,1.3,1,180,240,4,1.616,PHRE,20\n" +
"22018,8388607,-125.922,43.668,2000-08-07T00:00:00Z,1.3,2,0,240,1,1.0,AKCA,20\n" +
"22019,8388607,-125.935,43.662,2000-08-07T00:00:00Z,1.3,1,270,340,1,0.601,STLE,25\n" +
"22020,8388607,-125.968,43.693,2000-08-07T00:00:00Z,1.6,1,40,340,1,0.67,STLE,25\n" +
"22022,8388607,-125.978,43.727,2000-08-07T00:00:00Z,1.3,1,50,150,1,0.469,STLE,25\n" +
"22023,8388607,-125.953,43.695,2000-08-07T00:00:00Z,1.3,2,0,150,1,1.0,PHRE,25\n" +
"22025,8388607,-125.903,43.628,2000-08-07T00:00:00Z,1.3,1,50,150,1,0.469,STLE,25\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
            //unscaled flight_dir values are 0..36 so see if >=40 is properly handled 
            tName = tedd.makeNewFileForDapQuery(null, null, baseQuery + "&flight_dir>=40", 
                EDStatic.fullTestCacheDirectory, tedd.className() + "_bird2", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"trans_no,trans_id,longitude,latitude,time,area,behav_code,flight_dir,head_c,number,number_adj,species,wspd\n" +
",,degrees_east,degrees_north,UTC,km2,,degrees_true,degrees_true,count,count,,knots\n" +
"22001,8388607,-125.023,43.053,2000-08-07T00:00:00Z,1.3,1,180,240,1,0.448,SHSO,15\n" +
"22009,8388607,-125.467,43.84,2000-08-07T00:00:00Z,1.3,1,200,240,2,0.928,PHRE,20\n" +
"22013,8388607,-125.648,43.768,2000-08-07T00:00:00Z,1.3,1,270,240,1,1.104,STLE,20\n" +
"22015,8388607,-125.745,43.73,2000-08-07T00:00:00Z,1.3,1,180,240,4,1.616,PHRE,20\n" +
"22019,8388607,-125.935,43.662,2000-08-07T00:00:00Z,1.3,1,270,340,1,0.601,STLE,25\n" +
"22020,8388607,-125.968,43.693,2000-08-07T00:00:00Z,1.6,1,40,340,1,0.67,STLE,25\n" +
"22022,8388607,-125.978,43.727,2000-08-07T00:00:00Z,1.3,1,50,150,1,0.469,STLE,25\n" +
"22025,8388607,-125.903,43.628,2000-08-07T00:00:00Z,1.3,1,50,150,1,0.469,STLE,25\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
        
        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nUnexpected error for erdGlobecBirds."); 
        }

        try {        
            //try getting no data -- exception should be MustBe.THERE_IS_NO_DATA
            tName = tedd.makeNewFileForDapQuery(null, null, baseQuery + "&flight_dir>=4000", 
                EDStatic.fullTestCacheDirectory, tedd.className() + "_bird2", ".csv"); 
            throw new Exception("Shouldn't have gotten here.");      
        } catch (Throwable t) {
            //test that this is the expected exception
            if (t.toString().indexOf(MustBe.THERE_IS_NO_DATA) < 0)
                throw new RuntimeException( 
                    "Exception should have been MustBe.THERE_IS_NO_DATA=\"" + 
                    MustBe.THERE_IS_NO_DATA + "\":\n" +
                    MustBe.throwableToString(t));
        }

    }

    /** NEEDS WORK.   This tests reading a .pngInfo file. */
    public static void testReadPngInfo() throws Throwable{
        /* needs work
        String2.log("\n* EDD.testReadPngInfo");
        Object oa[] = readPngInfo(null,
            EDStatic.unitTestDataDir + "graphs/testGlobecBottle_2603962601.json");
        double graphDoubleWESN[] = (double[])oa[0];
        Test.ensureEqual(graphDoubleWESN, 
            new double[]{-126.30999908447265, -123.45999755859376, 41.9, 44.75000152587891}, "");

        int graphIntWESN[] = (int[])oa[1];
        Test.ensureEqual(graphIntWESN, 
            new int[]{29, 331, 323, 20}, "");
        */
    }

        /** This tests lat lon requests. */
    public static void testLatLon() throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.testLatLon\n");
        testVerboseOn();
        String results, query, tName, expected;

        //the basicQuery
        try {
            EDDTable edd = (EDDTable)oneFromDatasetsXml(null, "erdGlobecMoc1"); 

            //*** test a TableWriter that doesn't convert time to iso format
            query = "cruise_id,station_id,longitude,latitude&latitude=44.6517&distinct()";             
           
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_LL", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"cruise_id,station_id,longitude,latitude\n" +
",,degrees_east,degrees_north\n" +
"NH0005,NH15,-124.4117,44.6517\n" +
"NH0005,NH25,-124.65,44.6517\n" +
"NH0007,NH05,-124.175,44.6517\n" +
"NH0007,NH15,-124.4117,44.6517\n" +
"NH0007,NH25,-124.65,44.6517\n" +
"W0004B,NH05,-124.175,44.6517\n" +
"W0004B,NH15,-124.4117,44.6517\n" +
"W0004B,NH25,-124.65,44.6517\n" +
"W0004B,NH45,-125.1167,44.6517\n" +
"W0007A,NH15,-124.4117,44.6517\n" +
"W0007A,NH25,-124.65,44.6517\n" +
"W0009A,NH15,-124.4117,44.6517\n" +
"W0009A,NH25,-124.65,44.6517\n" +
"W0204A,NH25,-124.65,44.6517\n" +
"W0205A,NH15,-124.4117,44.6517\n" +
"W0205A,NH25,-124.65,44.6517\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.pressEnterToContinue("\n" + MustBe.throwableToString(t) + 
                "\nUnexpected error for testLatLon."); 
        }
    }


    /** This tests an altitude axis variable. This requires testTableWithAltitude dataset in localhost ERDDAP. */
    public static void testTableWithAltitude() throws Throwable {
        String2.log("\n*** EDDTableFromNcFiles.testTableWithAltitude");
        String results, expected, tName;
        int po;
       
        //!!! I no longer have a test dataset with real altitude data!

        /*
        String url = "http://www.marine.csiro.au/dods/nph-dods/dods-data/bl/BRAN2.1/bodas/19921014.bodas_ts.nc";
        results = generateDatasetsXml(true, url, 
            null, null, null, DEFAULT_RELOAD_EVERY_N_MINUTES, null);
        po = results.indexOf("<sourceName>z</sourceName>");
        Test.ensureTrue(po > 0, "results=\n" + results);
        expected = 
"<sourceName>z</sourceName>\n" +
"        <destinationName>depth</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"cartesian_axis\">Z</att>\n" +
"            <att name=\"long_name\">Depth</att>\n" +
"            <att name=\"positive\">down</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"standard_name\">depth</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>";
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, 
            "results=\n" + results);

        //Test that constructor of EDVAlt added proper metadata for altitude variable.
        EDDTableFromNcFiles tableDataset = (EDDTableFromNcFiles)oneFromDatasetsXml(null, "erdCalcofiBio");         
        tName = tableDataset.makeNewFileForDapQuery(null, null, "",
            EDStatic.fullTestCacheDirectory, tableDataset.className() + "testTableWithAltitude", ".das"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        po = results.indexOf("depth {");
        Test.ensureTrue(po > 0, "results=\n" + results);
        expected = 
  "depth {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"down\";\n" +
"    Float32 actual_range 6.3, 267.9;\n" +
"    String axis \"Z\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Depth at Start of Tow\";\n" +
"    String positive \"down\";\n" +
"    String standard_name \"depth\";\n" +
"    String units \"m\";\n" +
"  }";
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, 
            "results=\n" + results);

        //FGDC should deal with altitude correctly -- 
        //But it isn't like grids.  Nothing interesting since not a true axis. 

        //ISO 19115 should deal with altitude correctly
        tName = tableDataset.makeNewFileForDapQuery(null, null, "",
            EDStatic.fullTestCacheDirectory, tableDataset.className() + "testTableWithAltitude", ".iso19115"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());

        po = results.indexOf(
"codeListValue=\"vertical\">");
        Test.ensureTrue(po > 0, "results=\n" + results);
        expected = 
                 "codeListValue=\"vertical\">vertical</gmd:MD_DimensionNameTypeCode>\n" +
"          </gmd:dimensionName>\n" +
"          <gmd:dimensionSize gco:nilReason=\"unknown\"/>\n" +
"        </gmd:MD_Dimension>\n" +
"      </gmd:axisDimensionProperties>\n";
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, 
            "results=\n" + results);

        po = results.indexOf(
"<gmd:verticalElement>");
        Test.ensureTrue(po > 0, "results=\n" + results);
        expected = 
           "<gmd:verticalElement>\n" +
"            <gmd:EX_VerticalExtent>\n" +
"              <gmd:minimumValue><gco:Real>-267.9</gco:Real></gmd:minimumValue>\n" +
"              <gmd:maximumValue><gco:Real>-6.3</gco:Real></gmd:maximumValue>\n" +
"              <gmd:verticalCRS gco:nilReason=\"missing\"/>\n" +
"            </gmd:EX_VerticalExtent>\n" +
"          </gmd:verticalElement>";
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, 
            "results=\n" + results);

        */
        
    }


    /** This tests a depth variable. This requires testTableWithDepth dataset in localhost ERDDAP. */
    public static void testTableWithDepth() throws Throwable {
        String2.log("\n*** EDDTableFromNcFiles.testTableWithDepth");
        String results, expected, tName;
        int po;

        //Test that constructor of EDVDepth added proper metadata for depth variable.
        EDDTableFromNcFiles tableDataset = (EDDTableFromNcFiles)oneFromDatasetsXml(null, "testTableWithDepth");         
        tName = tableDataset.makeNewFileForDapQuery(null, null, "",
            EDStatic.fullTestCacheDirectory, tableDataset.className() + "testTableWithDepth", ".das"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        po = results.indexOf("depth {");
        Test.ensureTrue(po > 0, "results=\n" + results);
        expected = 
  "depth {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"down\";\n" +
"    Float32 actual_range -8.0, -3.0;\n" +
"    String axis \"Z\";\n" +
"    Int32 epic_code 3;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Depth\";\n" +
"    String positive \"down\";\n" +
"    String standard_name \"depth\";\n" +
"    String type \"EVEN\";\n" +
"    String units \"m\";\n" +
"  }";
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, 
            "results=\n" + results);

        //FGDC should deal with altitude correctly -- 
        //But it isn't like grids.  Nothing interesting since not a true axis. 

        //ISO 19115 should deal with depth correctly
        String2.log("!!timeIndex=" + tableDataset.timeIndex);
        EDVTime timeEdv = (EDVTime)tableDataset.dataVariables[tableDataset.timeIndex];
        String2.log("!!time destinationMin=" + timeEdv.destinationMin() + "=" + timeEdv.destinationMinString() +
                          " destinationMax=" + timeEdv.destinationMax() + "=" + timeEdv.destinationMaxString());
        tName = tableDataset.makeNewFileForDapQuery(null, null, "",
            EDStatic.fullTestCacheDirectory, tableDataset.className() + "testTableWithDepth", ".iso19115"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());

        expected = 
        "<gmd:EX_Extent>\n" +
"          <gmd:geographicElement>\n" +
"            <gmd:EX_GeographicBoundingBox>\n" +
"              <gmd:extentTypeCode>\n" +
"                <gco:Boolean>1</gco:Boolean>\n" +
"              </gmd:extentTypeCode>\n" +
"              <gmd:westBoundLongitude>\n" +
"                <gco:Decimal>-180.0</gco:Decimal>\n" +
"              </gmd:westBoundLongitude>\n" +
"              <gmd:eastBoundLongitude>\n" +
"                <gco:Decimal>180.0</gco:Decimal>\n" +
"              </gmd:eastBoundLongitude>\n" +
"              <gmd:southBoundLatitude>\n" +
"                <gco:Decimal>-25.0</gco:Decimal>\n" +
"              </gmd:southBoundLatitude>\n" +
"              <gmd:northBoundLatitude>\n" +
"                <gco:Decimal>21.0</gco:Decimal>\n" +
"              </gmd:northBoundLatitude>\n" +
"            </gmd:EX_GeographicBoundingBox>\n" +
"          </gmd:geographicElement>\n" +
"          <gmd:temporalElement>\n" +
"            <gmd:EX_TemporalExtent>\n" +
"              <gmd:extent>\n" +
"                <gml:TimePeriod gml:id=\"ED_gmdExtent_timePeriod_id\">\n" +
"                  <gml:description>seconds</gml:description>\n" +
"                  <gml:beginPosition>1977-11-06T12:00:00Z</gml:beginPosition>\n" +
"                  <gml:endPosition( indeterminatePosition=\"now\" />|>20.{8}T12:00:00Z</gml:endPosition>)\n" +  //important test
"                </gml:TimePeriod>\n" +
"              </gmd:extent>\n" +
"            </gmd:EX_TemporalExtent>\n" +
"          </gmd:temporalElement>\n" +
"          <gmd:verticalElement>\n" +
"            <gmd:EX_VerticalExtent>\n" +
"              <gmd:minimumValue><gco:Real>3.0</gco:Real></gmd:minimumValue>\n" +
"              <gmd:maximumValue><gco:Real>8.0</gco:Real></gmd:maximumValue>\n" +
"              <gmd:verticalCRS gco:nilReason=\"missing\"/>\n" +
"            </gmd:EX_VerticalExtent>\n" +
"          </gmd:verticalElement>\n" +
"        </gmd:EX_Extent>";
        po = results.indexOf("<gmd:EX_Extent>");
        int po2 = results.indexOf("</gmd:EX_Extent>", po + 10);
        if (po < 0 || po2 < 0)
            String2.log("po=" + po + " po2=" + po2 + " results=\n" + results);
        Test.ensureLinesMatch(results.substring(po, po2 + 16), expected, 
            "results=\n" + results);

        po = results.indexOf("<gml:TimePeriod gml:id=\"DI_gmdExtent_timePeriod_id\">");
        Test.ensureTrue(po >= 0, results);
        po = results.indexOf("<gml:TimePeriod gml:id=\"ED_gmdExtent_timePeriod_id\">");
        Test.ensureTrue(po >= 0, results);
        po = results.indexOf("<gml:TimePeriod gml:id=\"OD_gmdExtent_timePeriod_id\">");
        Test.ensureTrue(po >= 0, results);
        po = results.indexOf("<gml:TimePeriod gml:id=\"SUB_gmdExtent_timePeriod_id\">");
        Test.ensureTrue(po >= 0, results);
        po = results.indexOf(">OPeNDAP:OPeNDAP<");
        Test.ensureTrue(po >= 0, results);
        po = results.indexOf(">ERDDAP:tabledap<");
        Test.ensureTrue(po >= 0, results);
      
    }

    public static void testLegend() throws Throwable {
        String time1 = "now-11months";
        double time2 = Calendar2.nowStringToEpochSeconds(time1);
        String time3 = Calendar2.epochSecondsToIsoStringT(time2) + "Z";
        String queries[];
        String dir = EDStatic.fullTestCacheDirectory;
        String tName, start;
        EDDTable eddTable;

        //lon shouldn't appear
        eddTable = (EDDTable)oneFromDatasetsXml(null, "fsuNoaaShipWTEPnrt");
        start = "longitude,latitude,airPressure&airPressure>900&airPressure!=NaN" +
            "&airPressure=~\"(.*)\"&.marker=1|5&longitude%3E=-180&time%3E=";
        queries = new String[]{time1, "" + time2, time3};
        for (int i = 0; i < queries.length; i++) {
            tName = eddTable.makeNewFileForDapQuery(null, null, start + queries[i], 
                dir, eddTable.className() + "_testLegendA" + i,  ".png"); 
            SSR.displayInBrowser("file://" + dir + tName);
        }

        //time_precision
        eddTable = (EDDTable)oneFromDatasetsXml(null, "earthCubeKgsBoreTempWV");
        start = "longitude,latitude,MeasuredTemperature&longitude%3E=-180&time!=NaN" +
            "&State=\"West Virginia\"&time%3E=";
        queries = new String[]{
            "2010-03-01T00:00:00",
            "2010-03",
            "1267401600"};
        for (int i = 0; i < queries.length; i++) {
            tName = eddTable.makeNewFileForDapQuery(null, null, start + queries[i], 
                dir, eddTable.className() + "_testLegendB" + i,  ".png"); 
            SSR.displayInBrowser("file://" + dir + tName);
        }
        
    }

    /**
     * This tests saving a big request in a .nc file. (Re Keven O'Brien's email 2013-11-08)
     *
     * @throws Throwable if trouble
     */
    public static void testBigRequest() throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.testBigRequest() *****************\n");
        Table.verbose = false;
        testVerboseOff();
        boolean oReallyVerbose = reallyVerbose;
        reallyVerbose = false;

        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        int po;
        long resultLength = -1, expectedLength;

        String id = "cwwcNDBCMet";
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, id); 
        String dir = EDStatic.fullTestCacheDirectory;
        String baseName = eddTable.className() + "_BigRequest";

        String extensions[] = new String[] {  //.help not available at this level            
            //geoJson and .odvTxt require lon, lat (and time) data
            ".asc", ".csv", ".csvp", ".dods", ".esriCsv", 
            ".geoJson", ".htmlTable", ".json", ".mat", ".nc", 
            ".ncHeader", ".odvTxt", ".tsv", ".tsvp", ".xhtml",
            ".kml", ".smallPdf", ".pdf", ".largePdf", ".smallPng", //image file types
            ".png", ".largePng"};  
        int kmli = 15; //first image fileType.  kmli+1 is first displayable image type
        long bytes[]    = new long[] { //these will change as buoys get added (monthly)
            699049662, 1286009013, 1286009015, 734862352, 1362557418,    
            3247007700L, 18789440, 1959632896, 489908488, 489913680,     //.htmlTable is size-limited
            5417, 796336246, 1286009013, 1286009015, 3490596462L,
            280755, 489518, 2017546, 4589852, 24655, //I think redundant markers are not drawn
            48537, 179343};
        //2015-02-25 I give up doing timings. Timings vary greatly on different days.
        //  I think McAfee AV slows it down a lot. Its settings are not under my control.
        int expectedMs[] = new int[] { 
            //Java 1.7 M4700
            16941, 61839, 58656, 10686, 141881,    
            145004, 1352, 165740, 24779, 26390, 
            12752, 44120, 61769, 61620, 89530,
            15604, 16732, 14489, 41531, 15000, //2014-09-22 changed kml and smallPdf to be much faster
            17600, 17600};  //2014-09-02 both changed to be much faster

        //warm up
        tName = eddTable.makeNewFileForDapQuery(null, null, 
            "time&time<2013-09-01", 
            dir, baseName, ".nc");
        File2.delete(dir + tName);

        for (int i = 0; i < extensions.length; i++) {
            if (extensions[i].equals(".ncHeader"))
                File2.delete(dir + baseName + ".nc");

            long time = 0;
            for (int chance = 0; chance < 1; chance++) { //was 3 when testing time
                Math2.gcAndWait();  //in a test
                time = System.currentTimeMillis();
                tName = eddTable.makeNewFileForDapQuery(null, null, 
                    extensions[i].equals(".geoJson") || extensions[i].equals(".odvTxt")?
                        "longitude,latitude,time,atmp&time<2000-01-01" :
                    i >= kmli?
                        "longitude,latitude,wd,time&time<2000-01-01&.draw=markers" :
                    "time&time<=2013-06-25T15", 
                    dir, baseName + extensions[i].substring(1) + chance, 
                    extensions[i]); 
                resultLength = File2.length(dir + tName);
                time = System.currentTimeMillis() - time;

                String2.log(
                    "\n*** fileName=" + dir + tName + "\n" +
                    "ext#" + i + " chance#" + chance + ": " + extensions[i] + 
                    ", length=" + resultLength + " expected=" + bytes[i] + ", " +
                    "time=" + time + " expected=" + expectedMs[i]);

                //if not too slow or too fast, break
                //if (time < expectedMs[i] / 2 || time > expectedMs[i] * 2) {
                //    //give it another chance
                //} else {
                //    break;
                //}
            }
            

            if (i >= kmli + 1)
                SSR.displayInBrowser("file://" + dir + tName);
            if (resultLength < 0.9 * bytes[i] || resultLength > 1.2 * bytes[i])
                //|| time < expectedMs[i] / 2 || time > expectedMs[i] * 2) 
                String2.pressEnterToContinue(
                    "Unexpected length or time." + String2.beep(1)); 
        }

        testVerboseOn();
        reallyVerbose = oReallyVerbose;
    }


    /**
     * Test pmelTaoAirT against web site.
     */
    public static void testPmelTaoAirt() throws Throwable {

        String2.log("\n*** EDDTableFromNcFiles.testAirt");
        EDDTable tedd = (EDDTable)oneFromDatasetsXml(null, "pmelTaoDyAirt"); //should work
        String tName, error, results, tResults, expected;
        int po;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

        //*** .das
        tName = tedd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            "testAirt", ".das"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected =   //2013-01-04 several changes related to new array and wmo_platform_code
"Attributes \\{\n" +
" s \\{\n" +
"  array \\{\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Array\";\n" +
"  \\}\n" +
"  station \\{\n" +
"    String cf_role \"timeseries_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station\";\n" +
"  \\}\n" +
"  wmo_platform_code \\{\n" +
"    Int32 actual_range 13001, 56055;\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"WMO Platform Code\";\n" +
"    Int32 missing_value 2147483647;\n" +
"  \\}\n" +
"  longitude \\{\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 actual_range 0.0, 350.0;\n" +
"    String axis \"X\";\n" +
"    Int32 epic_code 502;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Nominal Longitude\";\n" +
"    Float32 missing_value 1.0E35;\n" +
"    String standard_name \"longitude\";\n" +
"    String type \"EVEN\";\n" +
"    String units \"degrees_east\";\n" +
"  \\}\n" +
"  latitude \\{\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range -25.0, 21.0;\n" +  //before 2012-09-06 was -19, before 2012-03-20 was 38
"    String axis \"Y\";\n" +
"    Int32 epic_code 500;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Nominal Latitude\";\n" +
"    Float32 missing_value 1.0E35;\n" +
"    String standard_name \"latitude\";\n" +
"    String type \"EVEN\";\n" +
"    String units \"degrees_north\";\n" +
"  \\}\n" +
"  time \\{\n" +
"    String _CoordinateAxisType \"Time\";\n" +            
"    Float64 actual_range 2.476656e\\+8, .{8,16};\n" + //2nd number changes daily   
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Centered Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String type \"EVEN\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  \\}\n" +
"  depth \\{\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"down\";\n" +
"    Float32 actual_range -8.0, -3.0;\n" +
"    String axis \"Z\";\n" +
"    Int32 epic_code 3;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Depth\";\n" +
"    Float32 missing_value 1.0E35;\n" +
"    String positive \"down\";\n" +
"    String standard_name \"depth\";\n" +
"    String type \"EVEN\";\n" +
"    String units \"m\";\n" +
"  \\}\n" +
"  AT_21 \\{\n" +
"    Float32 actual_range 14.37, 34.14;\n" + //before 2013-08-28 was 17.05, before 2012-09-06 was 0.03, before 2012-03-20 was 2.59, 41.84;\n" +
"    Float64 colorBarMaximum 40.0;\n" +
"    Float64 colorBarMinimum -10.0;\n" +
"    Int32 epic_code 21;\n" +
"    String generic_name \"atemp\";\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Air Temperature\";\n" +
"    Float32 missing_value 1.0E35;\n" +
"    String name \"AT\";\n" +
"    String standard_name \"air_temperature\";\n" +
"    String units \"degree_C\";\n" +
"  }\n" +
"  QAT_5021 \\{\n" +
"    Float32 actual_range 0.0, 5.0;\n" +
"    String colorBarContinuous \"false\";\n" +
"    Float64 colorBarMaximum 6.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String description \"Quality: 0=missing data, 1=highest, 2=standard, 3=lower, 4=questionable, 5=bad, -9=contact Dai.C.McClurg@noaa.gov.  To get probably valid data only, request QAT_5021>=1 and QAT_5021<=3.\";\n" +
"    Int32 epic_code 5021;\n" +
"    String generic_name \"qat\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Air Temperature Quality\";\n" +
"    Float32 missing_value 1.0E35;\n" +
"    String name \"QAT\";\n" +
"  \\}\n" +
"  SAT_6021 \\{\n" +
"    Float32 actual_range 0.0, 6.0;\n" +
"    String colorBarContinuous \"false\";\n" +
"    Float64 colorBarMaximum 8.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String description \"Source Codes:\n" +
"0 = No Sensor, No Data\n" +
"1 = Real Time \\(Telemetered Mode\\)\n" +
"2 = Derived from Real Time\n" +
"3 = Temporally Interpolated from Real Time\n" +
"4 = Source Code Inactive at Present\n" +
"5 = Recovered from Instrument RAM \\(Delayed Mode\\)\n" +
"6 = Derived from RAM\n" +
"7 = Temporally Interpolated from RAM\";\n" +
"    Int32 epic_code 6021;\n" +
"    String generic_name \"sat\";\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Air Temperature Source\";\n" +
"    Float32 missing_value 1.0E35;\n" +
"    String name \"SAT\";\n" +
"  \\}\n" +
" \\}\n" +
"  NC_GLOBAL \\{\n" +
"    String cdm_data_type \"TimeSeries\";\n" +
"    String cdm_timeseries_variables \"array, station, wmo_platform_code, longitude, latitude\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String creator_email \"Dai.C.McClurg@noaa.gov\";\n" +
"    String creator_name \"TAO Project Office/NOAA/PMEL\";\n" +
"    String creator_url \"http://www.pmel.noaa.gov/tao/proj_over/proj_over.html\";\n" +
"    String Data_info \"Contact Paul Freitag: 206-526-6727\";\n" +
"    String Data_Source \"TAO Project Office/NOAA/PMEL\";\n" +
"    String defaultGraphQuery \"longitude,latitude,AT_21&time>=now-7days\";\n" +
"    Float64 Easternmost_Easting 350.0;\n" +
"    String featureType \"TimeSeries\";\n" +
"    String File_info \"Contact: Dai.C.McClurg@noaa.gov\";\n" +
"    Float64 geospatial_lat_max 21.0;\n" +
"    Float64 geospatial_lat_min -25.0;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 350.0;\n" +
"    Float64 geospatial_lon_min 0.0;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +  
"    Float64 geospatial_vertical_max -3.0;\n" +
"    Float64 geospatial_vertical_min -8.0;\n" +
"    String geospatial_vertical_positive \"down\";\n" +
"    String geospatial_vertical_units \"m\";\n" + 
"    String history \"This dataset has data from the TAO/TRITON, RAMA, and PIRATA projects.\n" +
"This dataset is a product of the TAO Project Office at NOAA/PMEL.\n" +
//The date below changes monthly  DON'T REGEX THIS. I WANT TO SEE THE CHANGES.
"2016-02-02 Bob Simons at NOAA/NMFS/SWFSC/ERD \\(bob.simons@noaa.gov\\) fully refreshed ERD's copy of this dataset by downloading all of the .cdf files from the PMEL TAO FTP site.  Since then, the dataset has been partially refreshed everyday by downloading and merging the latest version of the last 25 days worth of data\\.";
        int tPo = results.indexOf("worth of data.");
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureLinesMatch(results.substring(0, tPo + 14), expected, "\nresults=\n" + results);

//+ " (local files)\n" +
//today + " http://127.0.0.1:8080/cwexperimental/
expected = 
"tabledap/pmelTaoDyAirt.das\";\n" +
"    String infoUrl \"http://www.pmel.noaa.gov/tao/proj_over/proj_over.html\";\n" +
"    String institution \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\";\n" +
"    String keywords \"Atmosphere > Atmospheric Temperature > Air Temperature,\n" +
"Atmosphere > Atmospheric Temperature > Surface Air Temperature,\n" +
"air, air_temperature, atmosphere, atmospheric, buoys, centered, daily, depth, identifier, noaa, pirata, pmel, quality, rama, source, station, surface, tao, temperature, time, triton\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"Request for Acknowledgement: If you use these data in publications or presentations, please acknowledge the TAO Project Office of NOAA/PMEL. Also, we would appreciate receiving a preprint and/or reprint of publications utilizing the data for inclusion in our bibliography. Relevant publications should be sent to: TAO Project Office, NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, Seattle, WA 98115\n" +
"\n" +
"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    Float64 Northernmost_Northing 21.0;\n" + 
"    String project \"TAO/TRITON, RAMA, PIRATA\";\n" +
"    String Request_for_acknowledgement \"If you use these data in publications " +
    "or presentations, please acknowledge the TAO Project Office of NOAA/PMEL. " +
    "Also, we would appreciate receiving a preprint and/or reprint of " +
    "publications utilizing the data for inclusion in our bibliography. " +
    "Relevant publications should be sent to: TAO Project Office, " +
    "NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, " +
    "Seattle, WA 98115\";\n" +
"    String sourceUrl \"\\(local files\\)\";\n" +
"    Float64 Southernmost_Northing -25.0;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
"    String subsetVariables \"array, station, wmo_platform_code, longitude, latitude\";\n" +
"    String summary \"This dataset has daily Air Temperature data from the\n" +
"TAO/TRITON \\(Pacific Ocean, http://www.pmel.noaa.gov/tao/\\),\n" +
"RAMA \\(Indian Ocean, http://www.pmel.noaa.gov/tao/rama/\\), and\n" +
"PIRATA \\(Atlantic Ocean, http://www.pmel.noaa.gov/pirata/\\)\n" +
"arrays of moored buoys which transmit oceanographic and meteorological data " +
    "to shore in real-time via the Argos satellite system.  These buoys are " +
    "major components of the CLIVAR climate analysis project and the GOOS, " +
    "GCOS, and GEOSS observing systems.  Daily averages are computed starting " +
    "at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more " +
    "information, see\n" +
"http://www.pmel.noaa.gov/tao/proj_over/proj_over.html .\";\n" +
"    String time_coverage_end \"20.{8}T12:00:00Z\";\n" +  //changes daily
"    String time_coverage_start \"1977-11-06T12:00:00Z\";\n" + //before 2012-03-20 was 1980-03-07T12:00:00
"    String title \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, Air Temperature\";\n" +
"    Float64 Westernmost_Easting 0.0;\n" +
"  }\n" +
"}\n";
        tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureLinesMatch(results.substring(tPo), expected, "results=\n" + results);
        

        //*** .dds
        tName = tedd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            "testAirt", ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String array;\n" +
"    String station;\n" +
"    Int32 wmo_platform_code;\n" +
"    Float32 longitude;\n" +
"    Float32 latitude;\n" +
"    Float64 time;\n" +
"    Float32 depth;\n" +
"    Float32 AT_21;\n" +
"    Float32 QAT_5021;\n" +
"    Float32 SAT_6021;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //Are there new stations??? 
        //  Check C:/u00/data/points/tao/daily/airt
        //  Currently 139 files: 138 .cdf and 1 update.txt.
        //  When new stations appear, need to modify 
        //    EDDTableFromTao.updateOneTaoDataset and the stations specified by the URLS.
        //  (It is unfortunate that this is hard-coded.)
        tName = tedd.makeNewFileForDapQuery(null, null, "station&distinct()", 
            EDStatic.fullTestCacheDirectory, "testAirtStations", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        //good test of salinity values: I hand checked this from 1989427.nc in gtspp_at199505.zip
        expected = 
/* changes noticed 2012-03-20   new stations and stations which disappeared:
Dai McClurg says the new stations are old and currently inactive but valid.
and the removed stations were temporary and aren't part of their public distribution.
So the changes seem good. */
"station\n" +
"\n" +
"0.7n110w\n" +
"0.7s110w\n" +
"0n0e\n" +
"0n108w\n" +
"0n10w\n" +
"0n110.5w\n" +
"0n110w\n" +
"0n125w\n" +
"0n137e\n" +
"0n140w\n" +
"0n143e\n" +
"0n147e\n" +
"0n152w\n" +
"0n154e\n" +
"0n155e\n" +
"0n155w\n" +
"0n156e\n" +
"0n158e\n" +
"0n161e\n" +
"0n165e\n" +
"0n170e\n" +
"0n170w\n" +
"0n176w\n" +
"0n180w\n" +
"0n23w\n" +
"0n35w\n" +
"0n67e\n" +
"0n80.5e\n" +
"0n85w\n" +
"0n90e\n" +
"0n95w\n" +
"1.5n80.5e\n" +
"1.5n90e\n" +
"1.5s67e\n" + //added 2013-09-05
"1.5s80.5e\n" +
"1.5s90e\n" +
"10n95w\n" +
"10s10w\n" +
"12n23w\n" +
"12n38w\n" +
"12n90e\n" +
"12n95w\n" +
"12s55e\n" +
"12s67e\n" +
"12s80.5e\n" +
"12s93e\n" +
//"13n114e\n" + //removed 2013-09-05
"14s32w\n" +
"15n38w\n" +
"15n90e\n" +
"16s55e\n" +
"16s80.5e\n" +
"19s34w\n" +
"1n153w\n" +
"1n155e\n" +
"1s153w\n" +
"1s167e\n" +
//"20n117e\n" + //removed 2013-09-05
"20n38w\n" +
"21n23w\n" +
"25s100e\n" + //added 2012-09-06
"2n10w\n" +
"2n110w\n" +
"2n125w\n" +
"2n137e\n" +
"2n140w\n" +
"2n147e\n" +
"2n155w\n" +
"2n156e\n" +
"2n157w\n" +
"2n165e\n" +
"2n170w\n" +
"2n180w\n" +
"2n95w\n" +
"2s10w\n" +
"2s110w\n" +
"2s125w\n" +
"2s140w\n" +
"2s155w\n" +
"2s156e\n" +
"2s165e\n" +
"2s170w\n" +
"2s180w\n" +
"2s95w\n" +
"3.5n95w\n" +
"4n23w\n" +
"4n38w\n" +
"4n90e\n" +
"4n95w\n" +
"4s57e\n" + //2015-12-28 added
"4s67e\n" +
"4s80.5e\n" +
"5n110w\n" +
"5n125w\n" +
"5n137e\n" +
"5n140w\n" +
"5n147e\n" +
"5n155w\n" +
"5n156e\n" +
"5n165e\n" +
"5n170w\n" +
"5n180w\n" +
"5n95w\n" +
"5s10w\n" +
"5s110w\n" +
"5s125w\n" +
"5s140w\n" +
"5s155w\n" +
"5s156e\n" +
"5s165e\n" +
"5s170w\n" +
"5s180w\n" +
"5s95e\n" +
"5s95w\n" +
"6n150w\n" +
"6s10w\n" +
"6s8e\n" +
"7n132w\n" +
"7n137e\n" +
"7n140w\n" +
"7n147w\n" +
"7n150w\n" +
"8n110w\n" +
"8n125w\n" +
"8n130e\n" +
"8n137e\n" +
"8n150w\n" +
"8n155w\n" +
"8n156e\n" +
"8n165e\n" +
"8n167e\n" +
"8n168e\n" +
"8n170w\n" +
"8n180w\n" +
"8n38w\n" +
"8n90e\n" +
"8n95w\n" +
"8s100e\n" +
"8s110w\n" +
"8s125w\n" +
"8s155w\n" +
"8s165e\n" +
"8s170w\n" +
"8s180w\n" +
"8s30w\n" +
"8s55e\n" +
"8s67e\n" +
"8s80.5e\n" +
"8s95e\n" +
"8s95w\n" +
"9n140w\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);



        //data for station=2s180w
        //the initial run of this test tests whole file download 2011-07-19 updated on 2011-07-25.  
        //Does ERDDAP update match results from web site on 2011-07-25?
        //using http://www.pmel.noaa.gov/tao/data_deliv/deliv-nojava.html
        //to generate
        //http://www.pmel.noaa.gov/cgi-tao/cover.cgi?p83=2s180w&P18=137&P19=265&P20=-8&P21=9&P1=deliv&P2=airt&P3=dy&P4=2011&P5=7&P6=10&P7=2011&P8=7&P9=25&P10=buoy&P11=ascii&P12=None&P13=tt&P14=anonymous&P16=anonymous&P15=&NOTHING=&P17=&p22=html&script=disdel%2Fnojava.csh
        //yields
//Location:  2S 180W  10 Jul 2011 to 24 Jul 2011 (   15 times,  1 blocks) Gen. Date Jul 25 2011
// Units: Air Temperature (C), -9.99 = missing
// Time: 1200 10 Jul 2011 to 1200 24 Jul 2011 (index     1 to    15,   15 times)
// Depth (M):       -3 QUALITY SOURCE
// YYYYMMDD HHMM  AIRT Q S
//...
// 20110714 1200 28.34 2 1
// 20110715 1200 28.53 2 1
// 20110716 1200 27.84 2 1
// 20110717 1200 28.30 2 1
// 20110718 1200 28.42 2 1
// 20110719 1200 28.21 2 1
// 20110720 1200 28.41 2 1
// 20110721 1200 28.38 2 1
// 20110722 1200 28.06 2 1
// 20110723 1200 28.02 2 1
// 20110724 1200 28.31 2 1
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "&station=\"2s180w\"&time>2011-07-14&time<2011-07-25", 
            EDStatic.fullTestCacheDirectory, "testAirtData", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"array,station,wmo_platform_code,longitude,latitude,time,depth,AT_21,QAT_5021,SAT_6021\n" +
",,,degrees_east,degrees_north,UTC,m,degree_C,,\n" +
"TAO/TRITON,2s180w,52312,180.0,-2.0,2011-07-14T12:00:00Z,-3.0,28.34,1.0,5.0\n" +
"TAO/TRITON,2s180w,52312,180.0,-2.0,2011-07-15T12:00:00Z,-3.0,28.53,1.0,5.0\n" +
"TAO/TRITON,2s180w,52312,180.0,-2.0,2011-07-16T12:00:00Z,-3.0,27.84,1.0,5.0\n" +
"TAO/TRITON,2s180w,52312,180.0,-2.0,2011-07-17T12:00:00Z,-3.0,28.3,1.0,5.0\n" +
"TAO/TRITON,2s180w,52312,180.0,-2.0,2011-07-18T12:00:00Z,-3.0,28.43,1.0,5.0\n" +
"TAO/TRITON,2s180w,52312,180.0,-2.0,2011-07-19T12:00:00Z,-3.0,28.21,1.0,5.0\n" +
"TAO/TRITON,2s180w,52312,180.0,-2.0,2011-07-20T12:00:00Z,-3.0,28.41,1.0,5.0\n" +
"TAO/TRITON,2s180w,52312,180.0,-2.0,2011-07-21T12:00:00Z,-3.0,28.38,1.0,5.0\n" +
"TAO/TRITON,2s180w,52312,180.0,-2.0,2011-07-22T12:00:00Z,-3.0,28.06,1.0,5.0\n" +
"TAO/TRITON,2s180w,52312,180.0,-2.0,2011-07-23T12:00:00Z,-3.0,28.02,1.0,5.0\n" +
"TAO/TRITON,2s180w,52312,180.0,-2.0,2011-07-24T12:00:00Z,-3.0,28.31,1.0,5.0\n";

        Test.ensureEqual(results.substring(0, expected.length()), expected, 
            "\nresults=\n" + results);

    }

    /**
     */
    public static void testNow() throws Throwable {

        String2.log("\n*** EDDTableFromNcFiles.testNow");
        EDDTable tedd = (EDDTable)oneFromDatasetsXml(null, "ndbcSosWaves"); //has very recent data

        //these query tests need a dataset that has recent data (or request is rejected)
        //these tests moved here 2014-01-23
        GregorianCalendar gc = Calendar2.newGCalendarZulu();
        gc.set(Calendar2.MILLISECOND, 0);
        gc.add(Calendar2.SECOND, 1);  //now it is "now"
        long nowMillis = gc.getTimeInMillis();
        String s;
        StringArray rv = new StringArray();
        StringArray cv = new StringArray();
        StringArray co = new StringArray();
        StringArray cv2 = new StringArray();

        gc = Calendar2.newGCalendarZulu(nowMillis); 
        String2.log("now          = " + Calendar2.formatAsISODateTimeT3(gc));
        //non-regex EDVTimeStamp conValues will be ""+epochSeconds
        tedd.parseUserDapQuery("time&time=now", rv, cv, co, cv2, false);  
        Test.ensureEqual(rv.toString(), "time", "");
        Test.ensureEqual(cv.toString(), "time", "");
        Test.ensureEqual(co.toString(), "=", "");        
        Test.ensureEqual(cv2.toString(), "" + Calendar2.gcToEpochSeconds(gc), "");

        gc = Calendar2.newGCalendarZulu(nowMillis); 
        gc.add(Calendar2.SECOND, -1);
        String2.log("now-1second  = " + Calendar2.formatAsISODateTimeT3(gc));
        //non-regex EDVTimeStamp conValues will be ""+epochSeconds
        tedd.parseUserDapQuery("time&time=now-1second", rv, cv, co, cv2, false);  
        Test.ensureEqual(rv.toString(), "time", "");
        Test.ensureEqual(cv.toString(), "time", "");
        Test.ensureEqual(co.toString(), "=", "");        
        Test.ensureEqual(cv2.toString(), "" + Calendar2.gcToEpochSeconds(gc), "");

        gc = Calendar2.newGCalendarZulu(nowMillis); 
        gc.add(Calendar2.SECOND, 2);
        String2.log("now+2seconds = " + Calendar2.formatAsISODateTimeT3(gc));
        //non-regex EDVTimeStamp conValues will be ""+epochSeconds
        tedd.parseUserDapQuery("time&time=now%2B2seconds", rv, cv, co, cv2, false);  
        Test.ensureEqual(cv2.toString(), "" + Calendar2.gcToEpochSeconds(gc), "");

        //non-%encoded '+' will be decoded as ' ', so treat ' ' as equal to '+' 
        gc = Calendar2.newGCalendarZulu(nowMillis); 
        gc.add(Calendar2.SECOND, 2);
        String2.log("now 2seconds = " + Calendar2.formatAsISODateTimeT3(gc));
        //non-regex EDVTimeStamp conValues will be ""+epochSeconds 
        tedd.parseUserDapQuery("time&time=now 2seconds", rv, cv, co, cv2, false); 
        Test.ensureEqual(cv2.toString(), "" + Calendar2.gcToEpochSeconds(gc), "");

        gc = Calendar2.newGCalendarZulu(nowMillis); 
        gc.add(Calendar2.MINUTE, -3);
        String2.log("now-3minutes = " + Calendar2.formatAsISODateTimeT3(gc));
        //non-regex EDVTimeStamp conValues will be ""+epochSeconds 
        tedd.parseUserDapQuery("time&time=now-3minutes", rv, cv, co, cv2, false); 
        Test.ensureEqual(cv2.toString(), "" + Calendar2.gcToEpochSeconds(gc), "");

        gc = Calendar2.newGCalendarZulu(nowMillis); 
        gc.add(Calendar2.HOUR, -4);
        String2.log("now-4hours   = " + Calendar2.formatAsISODateTimeT3(gc));
        //non-regex EDVTimeStamp conValues will be ""+epochSeconds 
        tedd.parseUserDapQuery("time&time=now-4hours", rv, cv, co, cv2, false); 
        Test.ensureEqual(cv2.toString(), "" + Calendar2.gcToEpochSeconds(gc), "");

        gc = Calendar2.newGCalendarZulu(nowMillis); 
        gc.add(Calendar2.DATE, -5);
        String2.log("now-5days    = " + Calendar2.formatAsISODateTimeT3(gc));
        //non-regex EDVTimeStamp conValues will be ""+epochSeconds
        tedd.parseUserDapQuery("time&time=now-5days", rv, cv, co, cv2, false);  
        Test.ensureEqual(cv2.toString(), "" + Calendar2.gcToEpochSeconds(gc), "");

        gc = Calendar2.newGCalendarZulu(nowMillis); 
        gc.add(Calendar2.MONTH, -6);
        String2.log("now-6months  = " + Calendar2.formatAsISODateTimeT3(gc));
        //non-regex EDVTimeStamp conValues will be ""+epochSeconds 
        tedd.parseUserDapQuery("time&time=now-6months", rv, cv, co, cv2, false); 
        Test.ensureEqual(cv2.toString(), "" + Calendar2.gcToEpochSeconds(gc), "");

        gc = Calendar2.newGCalendarZulu(nowMillis); 
        gc.add(Calendar2.YEAR, -2);
        String2.log("now-7years   = " + Calendar2.formatAsISODateTimeT3(gc));
        //non-regex EDVTimeStamp conValues will be ""+epochSeconds 
        tedd.parseUserDapQuery("time&time=now-2years", rv, cv, co, cv2, false); 
        Test.ensureEqual(cv2.toString(), "" + Calendar2.gcToEpochSeconds(gc), "");
        //if (true) throw new RuntimeException("stop here");
    }

    /**
     */
    public static void testMinMaxConstraints() throws Throwable {

        String2.log("\n*** EDDTableFromNcFiles.testMinMaxConstraints");
        //dataset is fromFiles (so min,max are known) and no recent data (so stable)
        EDDTable tedd = (EDDTable)oneFromDatasetsXml(null, "LiquidR_HBG3_2015_weather"); 

        String s;
        StringArray rv  = new StringArray();
        StringArray cv  = new StringArray();
        StringArray co  = new StringArray();
        StringArray cv2 = new StringArray();
        EDV pVar = tedd.findDataVariableByDestinationName("pressure");
        double minP = pVar.destinationMin();
        double maxP = pVar.destinationMax();
        Test.ensureEqual(minP, 912.4, "");
        Test.ensureEqual(maxP, 2759, "");
        EDV timeVar = tedd.findDataVariableByDestinationName("time");
        double minT = timeVar.destinationMin();
        double maxT = timeVar.destinationMax();
        Test.ensureEqual(minT, 1432151400, ""); //2015-05-20T19:50:00Z
        Test.ensureEqual(maxT, 1446664200, ""); //2015-11-04T19:10:00Z

        tedd.parseUserDapQuery("pressure" +
            "&pressure<max(pressure)&pressure<max(pressure)-1.5" +
            "&pressure>min(pressure)&pressure>min(pressure)+2.5" +
            "&time<max(time)&time<max(time)-1.5&time<max(time)-2minutes" +
            "&time>min(time)&time>min(time)+2.5&time>min(time) 3milli",
            rv, cv, co, cv2, false); //repair?  
        Test.ensureEqual(EDDTable.formatAsDapQuery(rv, cv, co, cv2), 
            "pressure" +
            "&pressure<" + (maxP) + "&pressure<" + (maxP-1.5) +  
            "&pressure>" + (minP) + "&pressure>" + (minP+2.5) +  
            "&time<" + (maxT) + "&time<" + (maxT-1.5) + "&time<" + (maxT-120) +  
            "&time>" + (minT) + "&time>" + (minT+2.5) + "&time>" + (minT+0.003),  
            "");

        //test repair (invalid constraints are discarded)
        tedd.parseUserDapQuery("pressure" +
            "&pressure<max(pressure)&pressure<max(p&pressure<max(p)&pressure<max(pressure)-2minutes" +
            "&pressure>min(pressure)&pressure>min(pressure)2.5a&pressure>min(pressure)+2.5a" +
            "&time<max(time)&time<max(t&time<max(t)&time<max(t)2" +
            "&time>min(time)&time>min(time)+2.5milli&time>min(time)+3millia",
            rv, cv, co, cv2, true); //repair?  
        Test.ensureEqual(EDDTable.formatAsDapQuery(rv, cv, co, cv2), 
            "pressure" +
            "&pressure<" + (maxP) + 
            "&pressure>" + (minP) + 
            "&time<" + (maxT) + 
            "&time>" + (minT),  
            "");

        //test early error
        try {
            tedd.parseUserDapQuery("pressure&pressure<max(p)",
                rv, cv, co, cv2, false); //repair?  
            throw new SimpleException("shouldn't get here");
        } catch (Throwable t) {
            Test.ensureEqual(t.toString(),
                "com.cohort.util.SimpleException: Error: destinationVariableName=p wasn't found.", 
                "");
        }

        //test late time error
        try {
            tedd.parseUserDapQuery("pressure&pressure<max(time)-2.5seconds",
                rv, cv, co, cv2, false); //repair?  
            throw new SimpleException("shouldn't get here");
        } catch (Throwable t) {
            Test.ensureEqual(t.toString(),
"com.cohort.util.SimpleException: Query error: Invalid \"max()\" constraint: \"max(time)-2.5seconds\". " +
"Timestamp constraints with \"max()\" must be in the form " +
"\"max(varName)[+|-positiveInteger[millis|seconds|minutes|hours|days|months|years]]\" (or singular units).",
                "");
        }

        //test late non-time error
        try {
            tedd.parseUserDapQuery("pressure&pressure<max(pressure)-2.5a",
                rv, cv, co, cv2, false); //repair?  
            throw new SimpleException("shouldn't get here");
        } catch (Throwable t) {
            Test.ensureEqual(t.toString(),
"com.cohort.util.SimpleException: Query error: Invalid \"max()\" constraint: \"max(pressure)-2.5a\". " +
"Non-timestamp constraints with \"max()\" must be in the form \"max(varName)[+|-positiveNumber]\".", 
                "");
        }

    }

    /**
     * This tests sub-second time_precision in all output file types.
     *
     * @throws Throwable if trouble
     */
    public static void testTimePrecisionMillis() throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.testTimePrecisionMillis() *****************\n");
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, "testTimePrecisionMillisTable"); 
        String tDir = EDStatic.fullTestCacheDirectory;
        String userDapQuery = "time,ECEF_X,IB_time" +
            "&time>1984-02-01T12:00:59.001Z" + //value is .001, so this just barely fails
            "&time<=1984-02-01T12:00:59.401Z"; //value is .401, so this just barely succeeds
        String fName = "testTimePrecisionMillis";
        String tName, results, ts, expected;
        int po;

        //.asc  
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".asc"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    Float64 time;\n" +
"    Float32 ECEF_X;\n" +
"    Float64 IB_time;\n" +
"  } s;\n" +
"} s;\n" +
"---------------------------------------------\n" +
"s.time, s.ECEF_X, s.IB_time\n" +
"4.44484859101E8, 9.96921E36, 7.600176591E8\n" +
"4.44484859201E8, 9.96921E36, 7.600176592E8\n" +
"4.44484859301E8, 9.96921E36, 7.600176593E8\n" +
"4.4448485940099996E8, 9.96921E36, 7.600176594E8\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv  
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"time,ECEF_X,IB_time\n" +
"UTC,m,UTC\n" +
"1984-02-01T12:00:59.101Z,9.96921E36,1994-01-31T12:00:59.100Z\n" +
"1984-02-01T12:00:59.201Z,9.96921E36,1994-01-31T12:00:59.200Z\n" +
"1984-02-01T12:00:59.301Z,9.96921E36,1994-01-31T12:00:59.300Z\n" +
"1984-02-01T12:00:59.401Z,9.96921E36,1994-01-31T12:00:59.400Z\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.dods  doesn't write strings

        //.htmlTable
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
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
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".json"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"{\n" +
"  \"table\": {\n" +
"    \"columnNames\": [\"time\", \"ECEF_X\", \"IB_time\"],\n" +
"    \"columnTypes\": [\"String\", \"float\", \"String\"],\n" +
"    \"columnUnits\": [\"UTC\", \"m\", \"UTC\"],\n" +
"    \"rows\": [\n" +
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
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".nc"); 
        results = NcHelper.dumpString(tDir + tName, true);
        expected = 
"netcdf testTimePrecisionMillis.nc {\n" +
"  dimensions:\n" +
"    row = 4;\n" +
"  variables:\n" +
"    double time(row=4);\n" +
"      :_CoordinateAxisType = \"Time\";\n" +
"      :actual_range = 4.44484859101E8, 4.4448485940099996E8; // double\n" + //important full precision
"      :axis = \"T\";\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"L1a Outboard intermediate timestamp. Seconds since the J2K epoch\";\n" +
"      :standard_name = \"time\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :time_precision = \"1970-01-01T00:00:00.000Z\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    float ECEF_X(row=4);\n" +
"      :actual_range = 9.96921E36f, 9.96921E36f; // float\n" +
"      :ioos_category = \"Other\";\n" +
"      :long_name = \"Spacecraft ECEF position X value\";\n" +
"      :units = \"m\";\n" +
"\n" +
"    double IB_time(row=4);\n" +
"      :actual_range = 7.600176591E8, 7.600176594E8; // double\n" + //important full precision
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"L1a Inboard intermediate timestamp. Seconds since the J2K epoch\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :time_precision = \"1970-01-01T00:00:00.000Z\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"  // global attributes:\n" +
"  :algorithm_date = \"2014-06-18\";\n" +
"  :algorithm_version = \"OR_ALG_MAG_L1b_GEOF_v01r01.tgz\";\n" +
"  :cdm_data_type = \"Other\";\n" +
"  :dataset_name = \"IT_MAG-L1b-GEOF_G16_s2044065031446_e2044065031546_c2014064151446.nc\";\n";
        ts = results.substring(0, expected.length()); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);
        
        expected = 
"  :time_coverage_end = \"1984-02-01T12:00:59.401Z\";\n" +
"  :time_coverage_start = \"1984-02-01T12:00:59.101Z\";\n" +
"  :title = \"L1b Magnetometer (MAG) Geomagnetic Field Product\";\n" +
" data:\n" +
"time =\n" +
"  {4.44484859101E8, 4.44484859201E8, 4.44484859301E8, 4.4448485940099996E8}\n" +
"ECEF_X =\n" +
"  {9.96921E36, 9.96921E36, 9.96921E36, 9.96921E36}\n" +
"IB_time =\n" +
"  {7.600176591E8, 7.600176592E8, 7.600176593E8, 7.600176594E8}\n" +
"}\n";
        po = results.indexOf("  :time_coverage_end");
        ts = results.substring(Math.max(0, po), Math.min(results.length(), po + expected.length())); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

        //.odvTxt
        /* can't test because it needs lon lat values
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".odvTxt"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"zztop\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
        */

        //.xhtml  
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
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
    public static void testSimpleTestNcTable() throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.testSimpleTestNcTable() *****************\n");
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, "testSimpleTestNcTable"); 
        String tDir = EDStatic.fullTestCacheDirectory;
        String userDapQuery = "time,hours,minutes,seconds,millis,latitude," +
            "longitude,ints,floats,doubles,Strings" +
            "&time>=1970-01-02T00:00:00Z" + //should just barely succeed, 86400
            "&time<1970-01-05T00:00:00Z";   //should just barely fail, 345600
        String fName = "testSimpleTestNcTable";
        String tName, results, ts, expected;
        int po;

        String2.log(NcHelper.dumpString(EDStatic.unitTestDataDir + "simpleTest.nc", true));

        //all  
        tName = eddTable.makeNewFileForDapQuery(null, null, "", tDir, 
            fName, ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"time,hours,minutes,seconds,millis,latitude,longitude,ints,floats,doubles,Strings\n" +
"UTC,UTC,UTC,UTC,UTC,degrees_north,degrees_east,,,,\n" +
"1970-01-02T00:00:00Z,1980-01-01T05:00:00Z,1990-01-01T00:09:00Z,2000-01-01T00:00:20Z,2010-01-01T00:00:00.030Z,40,10000,1000000,0.0,1.0E12,0\n" +
"1970-01-03T00:00:00Z,1980-01-01T06:00:00Z,1990-01-01T00:10:00Z,2000-01-01T00:00:21Z,2010-01-01T00:00:00.031Z,41,10001,1000001,1.1,1.0000000000001E12,10\n" +
"1970-01-04T00:00:00Z,1980-01-01T07:00:00Z,1990-01-01T00:11:00Z,2000-01-01T00:00:22Z,2010-01-01T00:00:00.032Z,42,10002,1000002,2.2,1.0000000000002E12,20\n" +
"1970-01-05T00:00:00Z,1980-01-01T08:00:00Z,1990-01-01T00:12:00Z,2000-01-01T00:00:23Z,2010-01-01T00:00:00.033Z,43,10004,1000004,4.4,1.0000000000003E12,30\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.asc  
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".asc"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    Float64 time;\n" +
"    Float64 hours;\n" +
"    Float64 minutes;\n" +
"    Float64 seconds;\n" +
"    Float64 millis;\n" +
"    Byte latitude;\n" +
"    Int16 longitude;\n" +
"    Int32 ints;\n" +
"    Float32 floats;\n" +
"    Float64 doubles;\n" +
"    String Strings;\n" +
"  } s;\n" +
"} s;\n" +
"---------------------------------------------\n" +
"s.time, s.hours, s.minutes, s.seconds, s.millis, s.latitude, s.longitude, s.ints, s.floats, s.doubles, s.Strings\n" +
"86400.0, 3.155508E8, 6.3115254E8, 9.4668482E8, 1.26230400003E9, 40, 10000, 1000000, 0.0, 1.0E12, \"0\"\n" +
"172800.0, 3.155544E8, 6.311526E8, 9.46684821E8, 1.262304000031E9, 41, 10001, 1000001, 1.1, 1.0000000000001E12, \"10\"\n" +
"259200.0, 3.15558E8, 6.3115266E8, 9.46684822E8, 1.262304000032E9, 42, 10002, 1000002, 2.2, 1.0000000000002E12, \"20\"\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv  
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"time,hours,minutes,seconds,millis,latitude,longitude,ints,floats,doubles,Strings\n" +
"UTC,UTC,UTC,UTC,UTC,degrees_north,degrees_east,,,,\n" +
"1970-01-02T00:00:00Z,1980-01-01T05:00:00Z,1990-01-01T00:09:00Z,2000-01-01T00:00:20Z,2010-01-01T00:00:00.030Z,40,10000,1000000,0.0,1.0E12,0\n" +
"1970-01-03T00:00:00Z,1980-01-01T06:00:00Z,1990-01-01T00:10:00Z,2000-01-01T00:00:21Z,2010-01-01T00:00:00.031Z,41,10001,1000001,1.1,1.0000000000001E12,10\n" +
"1970-01-04T00:00:00Z,1980-01-01T07:00:00Z,1990-01-01T00:11:00Z,2000-01-01T00:00:22Z,2010-01-01T00:00:00.032Z,42,10002,1000002,2.2,1.0000000000002E12,20\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.dods  hard to test

        //.geoJson  
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".geoJson"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"{\n" +
"  \"type\": \"FeatureCollection\",\n" +
"  \"propertyNames\": [\"time\", \"hours\", \"minutes\", \"seconds\", \"millis\", \"ints\", \"floats\", \"doubles\", \"Strings\"],\n" +
"  \"propertyUnits\": [\"UTC\", \"UTC\", \"UTC\", \"UTC\", \"UTC\", null, null, null, null],\n" +
"  \"features\": [\n" +
"\n" +
"{\"type\": \"Feature\",\n" +
"  \"geometry\": {\n" +
"    \"type\": \"Point\",\n" +
"    \"coordinates\": [10000.0, 40.0] },\n" +
"  \"properties\": {\n" +
"    \"time\": \"1970-01-02T00:00:00Z\",\n" +
"    \"hours\": \"1980-01-01T05:00:00Z\",\n" +
"    \"minutes\": \"1990-01-01T00:09:00Z\",\n" +
"    \"seconds\": \"2000-01-01T00:00:20Z\",\n" +
"    \"millis\": \"2010-01-01T00:00:00.030Z\",\n" +
"    \"ints\": 1000000,\n" +
"    \"floats\": 0.0,\n" +
"    \"doubles\": 1.0E12,\n" +
"    \"Strings\": \"0\" }\n" +
"},\n" +
"{\"type\": \"Feature\",\n" +
"  \"geometry\": {\n" +
"    \"type\": \"Point\",\n" +
"    \"coordinates\": [10001.0, 41.0] },\n" +
"  \"properties\": {\n" +
"    \"time\": \"1970-01-03T00:00:00Z\",\n" +
"    \"hours\": \"1980-01-01T06:00:00Z\",\n" +
"    \"minutes\": \"1990-01-01T00:10:00Z\",\n" +
"    \"seconds\": \"2000-01-01T00:00:21Z\",\n" +
"    \"millis\": \"2010-01-01T00:00:00.031Z\",\n" +
"    \"ints\": 1000001,\n" +
"    \"floats\": 1.1,\n" +
"    \"doubles\": 1.0000000000001E12,\n" +
"    \"Strings\": \"10\" }\n" +
"},\n" +
"{\"type\": \"Feature\",\n" +
"  \"geometry\": {\n" +
"    \"type\": \"Point\",\n" +
"    \"coordinates\": [10002.0, 42.0] },\n" +
"  \"properties\": {\n" +
"    \"time\": \"1970-01-04T00:00:00Z\",\n" +
"    \"hours\": \"1980-01-01T07:00:00Z\",\n" +
"    \"minutes\": \"1990-01-01T00:11:00Z\",\n" +
"    \"seconds\": \"2000-01-01T00:00:22Z\",\n" +
"    \"millis\": \"2010-01-01T00:00:00.032Z\",\n" +
"    \"ints\": 1000002,\n" +
"    \"floats\": 2.2,\n" +
"    \"doubles\": 1.0000000000002E12,\n" +
"    \"Strings\": \"20\" }\n" +
"}\n" +
"\n" +
"  ],\n" +
"  \"bbox\": [10000.0, 40.0, 10002.0, 42.0]\n" +
"}\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.htmlTable
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".htmlTable"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"<table class=\"erd commonBGColor\" cellspacing=\"0\">\n" +
"<tr>\n" +
"<th>time\n" +
"<th>hours\n" +
"<th>minutes\n" +
"<th>seconds\n" +
"<th>millis\n" +
"<th>latitude\n" +
"<th>longitude\n" +
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
"<th>degrees_north\n" +
"<th>degrees_east\n" +
"<th>&nbsp;\n" +
"<th>&nbsp;\n" +
"<th>&nbsp;\n" +
"<th>&nbsp;\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap>1970-01-02\n" +
"<td nowrap>1980-01-01T05Z\n" +
"<td nowrap>1990-01-01T00:09Z\n" +
"<td nowrap>2000-01-01T00:00:20Z\n" +
"<td nowrap>2010-01-01T00:00:00.030Z\n" +
"<td align=\"right\">40\n" +
"<td align=\"right\">10000\n" +
"<td align=\"right\">1000000\n" +
"<td align=\"right\">0.0\n" +
"<td align=\"right\">1.0E12\n" +
"<td nowrap>0\n" +
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
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".json"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"{\n" +
"  \"table\": {\n" +
"    \"columnNames\": [\"time\", \"hours\", \"minutes\", \"seconds\", \"millis\", \"latitude\", \"longitude\", \"ints\", \"floats\", \"doubles\", \"Strings\"],\n" +
"    \"columnTypes\": [\"String\", \"String\", \"String\", \"String\", \"String\", \"byte\", \"short\", \"int\", \"float\", \"double\", \"String\"],\n" +
"    \"columnUnits\": [\"UTC\", \"UTC\", \"UTC\", \"UTC\", \"UTC\", \"degrees_north\", \"degrees_east\", null, null, null, null],\n" +
"    \"rows\": [\n" +
"      [\"1970-01-02T00:00:00Z\", \"1980-01-01T05:00:00Z\", \"1990-01-01T00:09:00Z\", \"2000-01-01T00:00:20Z\", \"2010-01-01T00:00:00.030Z\", 40, 10000, 1000000, 0.0, 1.0E12, \"0\"],\n" +
"      [\"1970-01-03T00:00:00Z\", \"1980-01-01T06:00:00Z\", \"1990-01-01T00:10:00Z\", \"2000-01-01T00:00:21Z\", \"2010-01-01T00:00:00.031Z\", 41, 10001, 1000001, 1.1, 1.0000000000001E12, \"10\"],\n" +
"      [\"1970-01-04T00:00:00Z\", \"1980-01-01T07:00:00Z\", \"1990-01-01T00:11:00Z\", \"2000-01-01T00:00:22Z\", \"2010-01-01T00:00:00.032Z\", 42, 10002, 1000002, 2.2, 1.0000000000002E12, \"20\"]\n" +
"    ]\n" +
"  }\n" +
"}\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.kml  
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".kml"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
"<Document>\n" +
"  <name>My Title</name>\n" +
"  <description><![CDATA[Data courtesy of NOAA NMFS SWFSC ERD\n" +
"<br />My summary.\n" +
"<br /><a href=\"http&#x3a;&#x2f;&#x2f;127&#x2e;0&#x2e;0&#x2e;1&#x3a;8080&#x2f;cwexperimental&#x2f;tabledap&#x2f;" +
"testSimpleTestNcTable&#x2e;html&#x3f;time&#x2c;hours&#x2c;minutes&#x2c;seconds&#x2c;millis&#x2c;latitude&#x2c;l" +
"ongitude&#x2c;ints&#x2c;floats&#x2c;doubles&#x2c;Strings&#x26;time&#x3e;&#x3d;1970&#x2d;01&#x2d;02T00&#x3a;00&#" +
"x3a;00Z&#x26;time&#x3c;1970&#x2d;01&#x2d;05T00&#x3a;00&#x3a;00Z\">View/download more data from this dataset.</a>\n" +
"    ]]></description>\n" +
"  <open>1</open>\n" +
"  <Style id=\"BUOY ON\">\n" +
"    <IconStyle>\n" +
"      <color>ff0099ff</color>\n" +
"      <scale>1.2000000000000002</scale>\n" +
"      <Icon>\n" +
"        <href>http://maps.google.com/mapfiles/kml/shapes/placemark_circle.png</href>\n" +
"      </Icon>\n" +
"    </IconStyle>\n" +
"  </Style>\n" +
"  <Style id=\"BUOY OUT\">\n" +
"    <IconStyle>\n" +
"      <color>ff0099ff</color>\n" +
"      <scale>0.8</scale>\n" +
"      <Icon>\n" +
"        <href>http://maps.google.com/mapfiles/kml/shapes/placemark_circle.png</href>\n" +
"      </Icon>\n" +
"    </IconStyle>\n" +
"    <LabelStyle><scale>0</scale></LabelStyle>\n" +
"  </Style>\n" +
"  <StyleMap id=\"BUOY\">\n" +
"    <Pair><key>normal</key><styleUrl>#BUOY OUT</styleUrl></Pair>\n" +
"    <Pair><key>highlight</key><styleUrl>#BUOY ON</styleUrl></Pair>\n" +
"  </StyleMap>\n" +
"  <Placemark>\n" +
"    <name>Lat=40, Lon=-80</name>\n" +
"    <description><![CDATA[My Title\n" +
"<br />Data courtesy of NOAA NMFS SWFSC ERD\n" +
"<br />time = 1970-01-02\n" +
"<br />hours = 1980-01-01T05Z\n" +
"<br />minutes = 1990-01-01T00:09Z\n" +
"<br />seconds = 2000-01-01T00:00:20Z\n" +
"<br />millis = 2010-01-01T00:00:00.030Z\n" +
"<br />latitude = 40 degrees_north\n" +
"<br />longitude = 10000 degrees_east\n" +
"<br />ints = 1000000\n" +
"<br />floats = 0.0\n" +
"<br />doubles = 1.0E12\n" +
"<br />Strings = 0\n" +
"<br /><a href=\"http&#x3a;&#x2f;&#x2f;127&#x2e;0&#x2e;0&#x2e;1&#x3a;8080&#x2f;cwexperimental&#x2f;tabledap&#x2f;" +
"testSimpleTestNcTable&#x2e;htmlTable&#x3f;&#x26;time&#x25;3E&#x3d;1969&#x2d;12&#x2d;28&#x26;time&#x25;3C&#x3d;1" +
"970&#x2d;01&#x2d;04&#x26;longitude&#x25;3E9999&#x2e;99&#x26;longitude&#x25;3C10000&#x2e;01&#x26;latitude&#x25;3" +
"E39&#x2e;99&#x26;latitude&#x25;3C40&#x2e;01\">View tabular data for this location.</a>\n" +
"\n" +
"<br /><a href=\"http&#x3a;&#x2f;&#x2f;127&#x2e;0&#x2e;0&#x2e;1&#x3a;8080&#x2f;cwexperimental&#x2f;tabledap&#x2f;" +
"testSimpleTestNcTable&#x2e;html&#x3f;time&#x2c;hours&#x2c;minutes&#x2c;seconds&#x2c;millis&#x2c;latitude&#x2c;l" +
"ongitude&#x2c;ints&#x2c;floats&#x2c;doubles&#x2c;Strings&#x26;time&#x3e;&#x3d;1970&#x2d;01&#x2d;02T00&#x3a;00&#" +
"x3a;00Z&#x26;time&#x3c;1970&#x2d;01&#x2d;05T00&#x3a;00&#x3a;00Z\">View/download more data from this dataset.</a>\n" +
"]]></description>\n" +
"    <styleUrl>#BUOY</styleUrl>\n" +
"    <Point>\n" +
"      <coordinates>-79.99999999999972,40.0</coordinates>\n" +
"    </Point>\n" +
"  </Placemark>\n" +
"  <Placemark>\n" +
"    <name>Lat=41, Lon=-79</name>\n" +
"    <description><![CDATA[My Title\n" +
"<br />Data courtesy of NOAA NMFS SWFSC ERD\n" +
"<br />time = 1970-01-03\n" +
"<br />hours = 1980-01-01T06Z\n" +
"<br />minutes = 1990-01-01T00:10Z\n" +
"<br />seconds = 2000-01-01T00:00:21Z\n" +
"<br />millis = 2010-01-01T00:00:00.031Z\n" +
"<br />latitude = 41 degrees_north\n" +
"<br />longitude = 10001 degrees_east\n" +
"<br />ints = 1000001\n" +
"<br />floats = 1.1\n" +
"<br />doubles = 1.0000000000001E12\n" +
"<br />Strings = 10\n" +
"<br /><a href=\"http&#x3a;&#x2f;&#x2f;127&#x2e;0&#x2e;0&#x2e;1&#x3a;8080&#x2f;cwexperimental&#x2f;tabledap&#x2f;" +
"testSimpleTestNcTable&#x2e;htmlTable&#x3f;&#x26;time&#x25;3E&#x3d;1969&#x2d;12&#x2d;28&#x26;time&#x25;3C&#x3d;1" +
"970&#x2d;01&#x2d;04&#x26;longitude&#x25;3E10000&#x2e;99&#x26;longitude&#x25;3C10001&#x2e;01&#x26;latitude&#x25;" +
"3E40&#x2e;99&#x26;latitude&#x25;3C41&#x2e;01\">View tabular data for this location.</a>\n" +
"\n" +
"<br /><a href=\"http&#x3a;&#x2f;&#x2f;127&#x2e;0&#x2e;0&#x2e;1&#x3a;8080&#x2f;cwexperimental&#x2f;tabledap&#x2f;" +
"testSimpleTestNcTable&#x2e;html&#x3f;time&#x2c;hours&#x2c;minutes&#x2c;seconds&#x2c;millis&#x2c;latitude&#x2c;l" +
"ongitude&#x2c;ints&#x2c;floats&#x2c;doubles&#x2c;Strings&#x26;time&#x3e;&#x3d;1970&#x2d;01&#x2d;02T00&#x3a;00&#" +
"x3a;00Z&#x26;time&#x3c;1970&#x2d;01&#x2d;05T00&#x3a;00&#x3a;00Z\">View/download more data from this dataset.</a>\n" +
"]]></description>\n" +
"    <styleUrl>#BUOY</styleUrl>\n" +
"    <Point>\n" +
"      <coordinates>-79.00000000000023,41.0</coordinates>\n" +
"    </Point>\n" +
"  </Placemark>\n" +
"  <Placemark>\n" +
"    <name>Lat=42, Lon=-78</name>\n" +
"    <description><![CDATA[My Title\n" +
"<br />Data courtesy of NOAA NMFS SWFSC ERD\n" +
"<br />time = 1970-01-04\n" +
"<br />hours = 1980-01-01T07Z\n" +
"<br />minutes = 1990-01-01T00:11Z\n" +
"<br />seconds = 2000-01-01T00:00:22Z\n" +
"<br />millis = 2010-01-01T00:00:00.032Z\n" +
"<br />latitude = 42 degrees_north\n" +
"<br />longitude = 10002 degrees_east\n" +
"<br />ints = 1000002\n" +
"<br />floats = 2.2\n" +
"<br />doubles = 1.0000000000002E12\n" +
"<br />Strings = 20\n" +
"<br /><a href=\"http&#x3a;&#x2f;&#x2f;127&#x2e;0&#x2e;0&#x2e;1&#x3a;8080&#x2f;cwexperimental&#x2f;tabledap&#x2f;" +
"testSimpleTestNcTable&#x2e;htmlTable&#x3f;&#x26;time&#x25;3E&#x3d;1969&#x2d;12&#x2d;28&#x26;time&#x25;3C&#x3d;1" +
"970&#x2d;01&#x2d;04&#x26;longitude&#x25;3E10001&#x2e;99&#x26;longitude&#x25;3C10002&#x2e;01&#x26;latitude&#x25;" +
"3E41&#x2e;99&#x26;latitude&#x25;3C42&#x2e;01\">View tabular data for this location.</a>\n" +
"\n" +
"<br /><a href=\"http&#x3a;&#x2f;&#x2f;127&#x2e;0&#x2e;0&#x2e;1&#x3a;8080&#x2f;cwexperimental&#x2f;tabledap&#x2f;" +
"testSimpleTestNcTable&#x2e;html&#x3f;time&#x2c;hours&#x2c;minutes&#x2c;seconds&#x2c;millis&#x2c;latitude&#x2c;l" +
"ongitude&#x2c;ints&#x2c;floats&#x2c;doubles&#x2c;Strings&#x26;time&#x3e;&#x3d;1970&#x2d;01&#x2d;02T00&#x3a;00&#" +
"x3a;00Z&#x26;time&#x3c;1970&#x2d;01&#x2d;05T00&#x3a;00&#x3a;00Z\">View/download more data from this dataset.</a>\n" +
"]]></description>\n" +
"    <styleUrl>#BUOY</styleUrl>\n" +
"    <Point>\n" +
"      <coordinates>-77.99999999999943,42.0</coordinates>\n" +
"    </Point>\n" +
"  </Placemark>\n" +
"  <LookAt>\n" +
"    <longitude>-79.00000000000023</longitude>\n" +
"    <latitude>41.0</latitude>\n" +
"    <range>466666.6666666667</range>\n" +
"  </LookAt>\n" +
"  <ScreenOverlay id=\"Logo\">\n" +
"    <description>http://127.0.0.1:8080/cwexperimental</description>\n" +
"    <name>Logo</name>\n" +
"    <Icon><href>http://127.0.0.1:8080/cwexperimental/images/nlogo.gif</href></Icon>\n" +
"    <overlayXY x=\"0.005\" y=\".04\" xunits=\"fraction\" yunits=\"fraction\"/>\n" +
"    <screenXY x=\"0.005\" y=\".04\" xunits=\"fraction\" yunits=\"fraction\"/>\n" +
"    <size x=\"0\" y=\"0\" xunits=\"pixels\" yunits=\"pixels\"/>\n" +
"  </ScreenOverlay>\n" +
"  </Document>\n" +
"</kml>\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.mat  is hard to test
//!!! but need to test to ensure not rounding to the nearest second

        //.nc  
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".nc"); 
        results = NcHelper.dumpString(tDir + tName, true);
        expected = 
"netcdf testSimpleTestNcTable.nc {\n" +
"  dimensions:\n" +
"    row = 3;\n" +
"    Strings_strlen = 2;\n" +
"  variables:\n" +
"    double time(row=3);\n" +
"      :_CoordinateAxisType = \"Time\";\n" +
"      :actual_range = 86400.0, 259200.0; // double\n" +
"      :axis = \"T\";\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Time\";\n" +
"      :standard_name = \"time\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :time_precision = \"1970-01-01\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    double hours(row=3);\n" +
"      :actual_range = 3.155508E8, 3.15558E8; // double\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Hours\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :time_precision = \"1970-01-01T00Z\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    double minutes(row=3);\n" +
"      :actual_range = 6.3115254E8, 6.3115266E8; // double\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Minutes\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :time_precision = \"1970-01-01T00:00Z\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    double seconds(row=3);\n" +
"      :actual_range = 9.4668482E8, 9.46684822E8; // double\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Seconds\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :time_precision = \"not valid\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    double millis(row=3);\n" +
"      :actual_range = 1.26230400003E9, 1.262304000032E9; // double\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Millis\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :time_precision = \"1970-01-01T00:00:00.000Z\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    byte latitude(row=3);\n" +
"      :_CoordinateAxisType = \"Lat\";\n" +
"      :actual_range = 40B, 42B; // byte\n" +
"      :axis = \"Y\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Latitude\";\n" +
"      :standard_name = \"latitude\";\n" +
"      :units = \"degrees_north\";\n" +
"\n" +
"    short longitude(row=3);\n" +
"      :_CoordinateAxisType = \"Lon\";\n" +
"      :actual_range = 10000S, 10002S; // short\n" +
"      :axis = \"X\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Longitude\";\n" +
"      :standard_name = \"longitude\";\n" +
"      :units = \"degrees_east\";\n" +
"\n" +
"    int ints(row=3);\n" +
"      :actual_range = 1000000, 1000002; // int\n" +
"      :ioos_category = \"Unknown\";\n" +
"\n" +
"    float floats(row=3);\n" +
"      :actual_range = 0.0f, 2.2f; // float\n" +
"      :ioos_category = \"Unknown\";\n" +
"\n" +
"    double doubles(row=3);\n" +
"      :actual_range = 1.0E12, 1.0000000000002E12; // double\n" +
"      :ioos_category = \"Unknown\";\n" +
"\n" +
"    char Strings(row=3, Strings_strlen=2);\n" +
"      :ioos_category = \"Unknown\";\n" +
"\n" +
"  // global attributes:\n" +
"  :cdm_data_type = \"Point\";\n" +
"  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"  :Easternmost_Easting = 10002.0; // double\n" +
"  :featureType = \"Point\";\n" +
"  :geospatial_lat_max = 42.0; // double\n" +
"  :geospatial_lat_min = 40.0; // double\n" +
"  :geospatial_lat_units = \"degrees_north\";\n" +
"  :geospatial_lon_max = 10002.0; // double\n" +
"  :geospatial_lon_min = 10000.0; // double\n" +
"  :geospatial_lon_units = \"degrees_east\";\n" +
"  :history = \"";  //2014-10-22T16:16:21Z (local files)\n";
        ts = results.substring(0, expected.length()); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

expected = 
//"2014-10-22T16:16:21Z http://127.0.0.1:8080/cwexperimental
"/tabledap/testSimpleTestNcTable.nc?time,hours,minutes,seconds,millis,latitude,longitude,ints,floats,doubles,Strings&time>=1970-01-02T00:00:00Z&time<1970-01-05T00:00:00Z\";\n" +
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
"  :Northernmost_Northing = 42.0; // double\n" +
"  :sourceUrl = \"(local files)\";\n" +
"  :Southernmost_Northing = 40.0; // double\n" +
"  :standard_name_vocabulary = \"CF Standard Name Table v29\";\n" +
"  :summary = \"My summary.\";\n" +
"  :time_coverage_end = \"1970-01-04\";\n" +
"  :time_coverage_start = \"1970-01-02\";\n" +
"  :title = \"My Title\";\n" +
"  :Westernmost_Easting = 10000.0; // double\n" +
" data:\n" +
"time =\n" +
"  {86400.0, 172800.0, 259200.0}\n" +
"hours =\n" +
"  {3.155508E8, 3.155544E8, 3.15558E8}\n" +
"minutes =\n" +
"  {6.3115254E8, 6.311526E8, 6.3115266E8}\n" +
"seconds =\n" +
"  {9.4668482E8, 9.46684821E8, 9.46684822E8}\n" +
"millis =\n" +
"  {1.26230400003E9, 1.262304000031E9, 1.262304000032E9}\n" +
"latitude =\n" +
"  {40, 41, 42}\n" +
"longitude =\n" +
"  {10000, 10001, 10002}\n" +
"ints =\n" +
"  {1000000, 1000001, 1000002}\n" +
"floats =\n" +
"  {0.0, 1.1, 2.2}\n" +
"doubles =\n" +
"  {1.0E12, 1.0000000000001E12, 1.0000000000002E12}\n" +
"Strings =\"0\", \"10\", \"20\"\n" +
"}\n";
        po = results.indexOf("/tabledap/testSimpleTestNcTable.nc?");
        ts = results.substring(Math.max(0, po), Math.min(results.length(), po + expected.length())); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

        //.odvTxt
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".odvTxt"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
////<Creator>???</Creator>
////<CreateTime>2014-10-22T21:33:31</CreateTime>
////<Software>ERDDAP - Version 1.53</Software>
////<Source>http://127.0.0.1:8080/cwexperimental/tabledap/testSimpleTestNcTable.html</Source>
"//<Version>ODV Spreadsheet V4.0</Version>\n" +
"//<DataField>GeneralField</DataField>\n" +
"//<DataType>GeneralType</DataType>\n" +
"Type:METAVAR:TEXT:2\tCruise:METAVAR:TEXT:2\tStation:METAVAR:TEXT:2\tyyyy-mm-ddThh:mm:ss.sss\ttime_ISO8601\ttime_ISO8601\ttime_ISO8601\ttime_ISO8601\tLatitude [degrees_north]:METAVAR:BYTE\tLongitude [degrees_east]:METAVAR:SHORT\tints:PRIMARYVAR:INTEGER\tfloats:FLOAT\tdoubles:DOUBLE\tStrings:METAVAR:TEXT:3\n" +
"*\t\t\t1970-01-02T00:00:00Z\t1980-01-01T05:00:00Z\t1990-01-01T00:09:00Z\t2000-01-01T00:00:20Z\t2010-01-01T00:00:00.030Z\t40\t10000\t1000000\t0.0\t1.0E12\t0\n" +
"*\t\t\t1970-01-03T00:00:00Z\t1980-01-01T06:00:00Z\t1990-01-01T00:10:00Z\t2000-01-01T00:00:21Z\t2010-01-01T00:00:00.031Z\t41\t10001\t1000001\t1.1\t1.0000000000001E12\t10\n" +
"*\t\t\t1970-01-04T00:00:00Z\t1980-01-01T07:00:00Z\t1990-01-01T00:11:00Z\t2000-01-01T00:00:22Z\t2010-01-01T00:00:00.032Z\t42\t10002\t1000002\t2.2\t1.0000000000002E12\t20\n";
        po = results.indexOf("//<Version>");
        ts = results.substring(Math.max(0, po), Math.min(results.length(), po + expected.length())); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

        //.xhtml  
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".xhtml"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
"  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
"<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
"<head>\n" +
"  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n" +
"  <title>testSimpleTestNcTable</title>\n" +
"</head>\n" +
"<body style=\"color:black; background:white; font-family:Arial,Helvetica,sans-serif; font-size:85%; line-height:130%;\">\n" +
"\n" +
"&nbsp;\n" +
"<table border=\"1\" cellpadding=\"2\" cellspacing=\"0\">\n" +
"<tr>\n" +
"<th>time</th>\n" +
"<th>hours</th>\n" +
"<th>minutes</th>\n" +
"<th>seconds</th>\n" +
"<th>millis</th>\n" +
"<th>latitude</th>\n" +
"<th>longitude</th>\n" +
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
"<th>degrees_north</th>\n" +
"<th>degrees_east</th>\n" +
"<th></th>\n" +
"<th></th>\n" +
"<th></th>\n" +
"<th></th>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\">1970-01-02T00:00:00Z</td>\n" +
"<td nowrap=\"nowrap\">1980-01-01T05:00:00Z</td>\n" +
"<td nowrap=\"nowrap\">1990-01-01T00:09:00Z</td>\n" +
"<td nowrap=\"nowrap\">2000-01-01T00:00:20Z</td>\n" +
"<td nowrap=\"nowrap\">2010-01-01T00:00:00.030Z</td>\n" +
"<td align=\"right\">40</td>\n" +
"<td align=\"right\">10000</td>\n" +
"<td align=\"right\">1000000</td>\n" +
"<td align=\"right\">0.0</td>\n" +
"<td align=\"right\">1.0E12</td>\n" +
"<td nowrap=\"nowrap\">0</td>\n" +
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


    }

    /**
     * This tests timestamps and other things.
     *
     * @throws Throwable if trouble
     */
    public static void testSimpleTestNc2Table() throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.testSimpleTestNc2Table() *****************\n");
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, "testSimpleTestNcTable"); 
        String tDir = EDStatic.fullTestCacheDirectory;
        String userDapQuery = "time,millis,latitude,longitude,doubles,Strings" +
            "&millis>2010-01-01T00:00:00.030Z" + //should reject .030 and accept 0.031 
            "&millis<=2010-01-01T00:00:00.032Z"; //should accept 0.032, but reject 0.033
        String fName = "testSimpleTestNc2Table";
        String tName, results, ts, expected;
        int po;

        String2.log(NcHelper.dumpString(EDStatic.unitTestDataDir + "simpleTest.nc", true));

        //.asc  
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".asc"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    Float64 time;\n" +
"    Float64 millis;\n" +
"    Byte latitude;\n" +
"    Int16 longitude;\n" +
"    Float64 doubles;\n" +
"    String Strings;\n" +
"  } s;\n" +
"} s;\n" +
"---------------------------------------------\n" +
"s.time, s.millis, s.latitude, s.longitude, s.doubles, s.Strings\n" +
"172800.0, 1.262304000031E9, 41, 10001, 1.0000000000001E12, \"10\"\n" +
"259200.0, 1.262304000032E9, 42, 10002, 1.0000000000002E12, \"20\"\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv  
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"time,millis,latitude,longitude,doubles,Strings\n" +
"UTC,UTC,degrees_north,degrees_east,,\n" +
"1970-01-03T00:00:00Z,2010-01-01T00:00:00.031Z,41,10001,1.0000000000001E12,10\n" +
"1970-01-04T00:00:00Z,2010-01-01T00:00:00.032Z,42,10002,1.0000000000002E12,20\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.dods  hard to test

        //.geoJson  
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".geoJson"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"{\n" +
"  \"type\": \"FeatureCollection\",\n" +
"  \"propertyNames\": [\"time\", \"millis\", \"doubles\", \"Strings\"],\n" +
"  \"propertyUnits\": [\"UTC\", \"UTC\", null, null],\n" +
"  \"features\": [\n" +
"\n" +
"{\"type\": \"Feature\",\n" +
"  \"geometry\": {\n" +
"    \"type\": \"Point\",\n" +
"    \"coordinates\": [10001.0, 41.0] },\n" +
"  \"properties\": {\n" +
"    \"time\": \"1970-01-03T00:00:00Z\",\n" +
"    \"millis\": \"2010-01-01T00:00:00.031Z\",\n" +
"    \"doubles\": 1.0000000000001E12,\n" +
"    \"Strings\": \"10\" }\n" +
"},\n" +
"{\"type\": \"Feature\",\n" +
"  \"geometry\": {\n" +
"    \"type\": \"Point\",\n" +
"    \"coordinates\": [10002.0, 42.0] },\n" +
"  \"properties\": {\n" +
"    \"time\": \"1970-01-04T00:00:00Z\",\n" +
"    \"millis\": \"2010-01-01T00:00:00.032Z\",\n" +
"    \"doubles\": 1.0000000000002E12,\n" +
"    \"Strings\": \"20\" }\n" +
"}\n" +
"\n" +
"  ],\n" +
"  \"bbox\": [10001.0, 41.0, 10002.0, 42.0]\n" +
"}\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.htmlTable
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".htmlTable"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"<table class=\"erd commonBGColor\" cellspacing=\"0\">\n" +
"<tr>\n" +
"<th>time\n" +
"<th>millis\n" +
"<th>latitude\n" +
"<th>longitude\n" +
"<th>doubles\n" +
"<th>Strings\n" +
"</tr>\n" +
"<tr>\n" +
"<th>UTC\n" +
"<th>UTC\n" +
"<th>degrees_north\n" +
"<th>degrees_east\n" +
"<th>&nbsp;\n" +
"<th>&nbsp;\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap>1970-01-03\n" +
"<td nowrap>2010-01-01T00:00:00.031Z\n" +
"<td align=\"right\">41\n" +
"<td align=\"right\">10001\n" +
"<td align=\"right\">1.0000000000001E12\n" +
"<td nowrap>10\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap>1970-01-04\n" +
"<td nowrap>2010-01-01T00:00:00.032Z\n" +
"<td align=\"right\">42\n" +
"<td align=\"right\">10002\n" +
"<td align=\"right\">1.0000000000002E12\n" +
"<td nowrap>20\n" +
"</tr>\n" +
"</table>\n";
        po = results.indexOf("<table class");
        ts = results.substring(Math.max(0, po), Math.min(results.length(), po + expected.length())); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

        //.json  
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".json"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"{\n" +
"  \"table\": {\n" +
"    \"columnNames\": [\"time\", \"millis\", \"latitude\", \"longitude\", \"doubles\", \"Strings\"],\n" +
"    \"columnTypes\": [\"String\", \"String\", \"byte\", \"short\", \"double\", \"String\"],\n" +
"    \"columnUnits\": [\"UTC\", \"UTC\", \"degrees_north\", \"degrees_east\", null, null],\n" +
"    \"rows\": [\n" +
"      [\"1970-01-03T00:00:00Z\", \"2010-01-01T00:00:00.031Z\", 41, 10001, 1.0000000000001E12, \"10\"],\n" +
"      [\"1970-01-04T00:00:00Z\", \"2010-01-01T00:00:00.032Z\", 42, 10002, 1.0000000000002E12, \"20\"]\n" +
"    ]\n" +
"  }\n" +
"}\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.kml  
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".kml"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
"<Document>\n" +
"  <name>My Title</name>\n" +
"  <description><![CDATA[Data courtesy of NOAA NMFS SWFSC ERD\n" +
"<br />My summary.\n" +
"<br /><a href=\"http&#x3a;&#x2f;&#x2f;127&#x2e;0&#x2e;0&#x2e;1&#x3a;8080&#x2f;cwexperimental&#x2f;tabledap&#x2f;" +
"testSimpleTestNcTable&#x2e;html&#x3f;time&#x2c;millis&#x2c;latitude&#x2c;longitude&#x2c;doubles&#x2c;Strings&#x" +
"26;millis&#x3e;2010&#x2d;01&#x2d;01T00&#x3a;00&#x3a;00&#x2e;030Z&#x26;millis&#x3c;&#x3d;2010&#x2d;01&#x2d;01T00" +
"&#x3a;00&#x3a;00&#x2e;032Z\">View/download more data from this dataset.</a>\n" +
"    ]]></description>\n" +
"  <open>1</open>\n" +
"  <Style id=\"BUOY ON\">\n" +
"    <IconStyle>\n" +
"      <color>ff0099ff</color>\n" +
"      <scale>1.2000000000000002</scale>\n" +
"      <Icon>\n" +
"        <href>http://maps.google.com/mapfiles/kml/shapes/placemark_circle.png</href>\n" +
"      </Icon>\n" +
"    </IconStyle>\n" +
"  </Style>\n" +
"  <Style id=\"BUOY OUT\">\n" +
"    <IconStyle>\n" +
"      <color>ff0099ff</color>\n" +
"      <scale>0.8</scale>\n" +
"      <Icon>\n" +
"        <href>http://maps.google.com/mapfiles/kml/shapes/placemark_circle.png</href>\n" +
"      </Icon>\n" +
"    </IconStyle>\n" +
"    <LabelStyle><scale>0</scale></LabelStyle>\n" +
"  </Style>\n" +
"  <StyleMap id=\"BUOY\">\n" +
"    <Pair><key>normal</key><styleUrl>#BUOY OUT</styleUrl></Pair>\n" +
"    <Pair><key>highlight</key><styleUrl>#BUOY ON</styleUrl></Pair>\n" +
"  </StyleMap>\n" +
"  <Placemark>\n" +
"    <name>Lat=41, Lon=-79</name>\n" +
"    <description><![CDATA[My Title\n" +
"<br />Data courtesy of NOAA NMFS SWFSC ERD\n" +
"<br />time = 1970-01-03\n" +
"<br />millis = 2010-01-01T00:00:00.031Z\n" +
"<br />latitude = 41 degrees_north\n" +
"<br />longitude = 10001 degrees_east\n" +
"<br />doubles = 1.0000000000001E12\n" +
"<br />Strings = 10\n" +
"<br /><a href=\"http&#x3a;&#x2f;&#x2f;127&#x2e;0&#x2e;0&#x2e;1&#x3a;8080&#x2f;cwexperimental&#x2f;tabledap&#x2f;" +
"testSimpleTestNcTable&#x2e;htmlTable&#x3f;&#x26;time&#x25;3E&#x3d;1969&#x2d;12&#x2d;28&#x26;time&#x25;3C&#x3d;1" +
"970&#x2d;01&#x2d;04&#x26;longitude&#x25;3E10000&#x2e;99&#x26;longitude&#x25;3C10001&#x2e;01&#x26;latitude&#x25;" +
"3E40&#x2e;99&#x26;latitude&#x25;3C41&#x2e;01\">View tabular data for this location.</a>\n" +
"\n" +
"<br /><a href=\"http&#x3a;&#x2f;&#x2f;127&#x2e;0&#x2e;0&#x2e;1&#x3a;8080&#x2f;cwexperimental&#x2f;tabledap&#x2f;" +
"testSimpleTestNcTable&#x2e;html&#x3f;time&#x2c;millis&#x2c;latitude&#x2c;longitude&#x2c;doubles&#x2c;Strings&#x" +
"26;millis&#x3e;2010&#x2d;01&#x2d;01T00&#x3a;00&#x3a;00&#x2e;030Z&#x26;millis&#x3c;&#x3d;2010&#x2d;01&#x2d;01T00" +
"&#x3a;00&#x3a;00&#x2e;032Z\">View/download more data from this dataset.</a>\n" +
"]]></description>\n" +
"    <styleUrl>#BUOY</styleUrl>\n" +
"    <Point>\n" +
"      <coordinates>-79.00000000000023,41.0</coordinates>\n" +
"    </Point>\n" +
"  </Placemark>\n" +
"  <Placemark>\n" +
"    <name>Lat=42, Lon=-78</name>\n" +
"    <description><![CDATA[My Title\n" +
"<br />Data courtesy of NOAA NMFS SWFSC ERD\n" +
"<br />time = 1970-01-04\n" +
"<br />millis = 2010-01-01T00:00:00.032Z\n" +
"<br />latitude = 42 degrees_north\n" +
"<br />longitude = 10002 degrees_east\n" +
"<br />doubles = 1.0000000000002E12\n" +
"<br />Strings = 20\n" +
"<br /><a href=\"http&#x3a;&#x2f;&#x2f;127&#x2e;0&#x2e;0&#x2e;1&#x3a;8080&#x2f;cwexperimental&#x2f;tabledap&#x2f;" +
"testSimpleTestNcTable&#x2e;htmlTable&#x3f;&#x26;time&#x25;3E&#x3d;1969&#x2d;12&#x2d;28&#x26;time&#x25;3C&#x3d;1" +
"970&#x2d;01&#x2d;04&#x26;longitude&#x25;3E10001&#x2e;99&#x26;longitude&#x25;3C10002&#x2e;01&#x26;latitude&#x25;" +
"3E41&#x2e;99&#x26;latitude&#x25;3C42&#x2e;01\">View tabular data for this location.</a>\n" +
"\n" +
"<br /><a href=\"http&#x3a;&#x2f;&#x2f;127&#x2e;0&#x2e;0&#x2e;1&#x3a;8080&#x2f;cwexperimental&#x2f;tabledap&#x2f;" +
"testSimpleTestNcTable&#x2e;html&#x3f;time&#x2c;millis&#x2c;latitude&#x2c;longitude&#x2c;doubles&#x2c;Strings&#x" +
"26;millis&#x3e;2010&#x2d;01&#x2d;01T00&#x3a;00&#x3a;00&#x2e;030Z&#x26;millis&#x3c;&#x3d;2010&#x2d;01&#x2d;01T00" +
"&#x3a;00&#x3a;00&#x2e;032Z\">View/download more data from this dataset.</a>\n" +
"]]></description>\n" +
"    <styleUrl>#BUOY</styleUrl>\n" +
"    <Point>\n" +
"      <coordinates>-77.99999999999943,42.0</coordinates>\n" +
"    </Point>\n" +
"  </Placemark>\n" +
"  <LookAt>\n" +
"    <longitude>-78.49999999999977</longitude>\n" +
"    <latitude>41.5</latitude>\n" +
"    <range>466666.6666666667</range>\n" +
"  </LookAt>\n" +
"  <ScreenOverlay id=\"Logo\">\n" +
"    <description>http://127.0.0.1:8080/cwexperimental</description>\n" +
"    <name>Logo</name>\n" +
"    <Icon><href>http://127.0.0.1:8080/cwexperimental/images/nlogo.gif</href></Icon>\n" +
"    <overlayXY x=\"0.005\" y=\".04\" xunits=\"fraction\" yunits=\"fraction\"/>\n" +
"    <screenXY x=\"0.005\" y=\".04\" xunits=\"fraction\" yunits=\"fraction\"/>\n" +
"    <size x=\"0\" y=\"0\" xunits=\"pixels\" yunits=\"pixels\"/>\n" +
"  </ScreenOverlay>\n" +
"  </Document>\n" +
"</kml>\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.mat  hard to test
//!!! but need to test to ensure not rounding to the nearest second

        //.nc  
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".nc"); 
        results = NcHelper.dumpString(tDir + tName, true);
        expected = 
"netcdf testSimpleTestNc2Table.nc {\n" +
"  dimensions:\n" +
"    row = 2;\n" +
"    Strings_strlen = 2;\n" +
"  variables:\n" +
"    double time(row=2);\n" +
"      :_CoordinateAxisType = \"Time\";\n" +
"      :actual_range = 172800.0, 259200.0; // double\n" +
"      :axis = \"T\";\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Time\";\n" +
"      :standard_name = \"time\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :time_precision = \"1970-01-01\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    double millis(row=2);\n" +
"      :actual_range = 1.262304000031E9, 1.262304000032E9; // double\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Millis\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :time_precision = \"1970-01-01T00:00:00.000Z\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    byte latitude(row=2);\n" +
"      :_CoordinateAxisType = \"Lat\";\n" +
"      :actual_range = 41B, 42B; // byte\n" +
"      :axis = \"Y\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Latitude\";\n" +
"      :standard_name = \"latitude\";\n" +
"      :units = \"degrees_north\";\n" +
"\n" +
"    short longitude(row=2);\n" +
"      :_CoordinateAxisType = \"Lon\";\n" +
"      :actual_range = 10001S, 10002S; // short\n" +
"      :axis = \"X\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Longitude\";\n" +
"      :standard_name = \"longitude\";\n" +
"      :units = \"degrees_east\";\n" +
"\n" +
"    double doubles(row=2);\n" +
"      :actual_range = 1.0000000000001E12, 1.0000000000002E12; // double\n" +
"      :ioos_category = \"Unknown\";\n" +
"\n" +
"    char Strings(row=2, Strings_strlen=2);\n" +
"      :ioos_category = \"Unknown\";\n" +
"\n" +
"  // global attributes:\n" +
"  :cdm_data_type = \"Point\";\n" +
"  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"  :Easternmost_Easting = 10002.0; // double\n" +
"  :featureType = \"Point\";\n" +
"  :geospatial_lat_max = 42.0; // double\n" +
"  :geospatial_lat_min = 41.0; // double\n" +
"  :geospatial_lat_units = \"degrees_north\";\n" +
"  :geospatial_lon_max = 10002.0; // double\n" +
"  :geospatial_lon_min = 10001.0; // double\n" +
"  :geospatial_lon_units = \"degrees_east\";\n" +
"  :history = \""; //2014-10-22T16:16:21Z (local files)\n";
        ts = results.substring(0, expected.length()); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

expected = 
//"2014-10-22T16:16:21Z http://127.0.0.1:8080/cwexperimental
"/tabledap/testSimpleTestNcTable.nc?time,millis,latitude,longitude,doubles,Strings&millis>2010-01-01T00:00:00.030Z&millis<=2010-01-01T00:00:00.032Z\";\n" +
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
"  :Northernmost_Northing = 42.0; // double\n" +
"  :sourceUrl = \"(local files)\";\n" +
"  :Southernmost_Northing = 41.0; // double\n" +
"  :standard_name_vocabulary = \"CF Standard Name Table v29\";\n" +
"  :summary = \"My summary.\";\n" +
"  :time_coverage_end = \"1970-01-04\";\n" +
"  :time_coverage_start = \"1970-01-03\";\n" +
"  :title = \"My Title\";\n" +
"  :Westernmost_Easting = 10001.0; // double\n" +
" data:\n" +
"time =\n" +
"  {172800.0, 259200.0}\n" +
"millis =\n" +
"  {1.262304000031E9, 1.262304000032E9}\n" +
"latitude =\n" +
"  {41, 42}\n" +
"longitude =\n" +
"  {10001, 10002}\n" +
"doubles =\n" +
"  {1.0000000000001E12, 1.0000000000002E12}\n" +
"Strings =\"10\", \"20\"\n" +
"}\n";
        po = results.indexOf("/tabledap/testSimpleTestNcTable.nc?");
        ts = results.substring(Math.max(0, po), Math.min(results.length(), po + expected.length())); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

        //.odvTxt
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".odvTxt"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
//<Creator>???</Creator>
//<CreateTime>2014-10-22T22:43:55</CreateTime>
//<Software>ERDDAP - Version 1.53</Software>
//<Source>http://127.0.0.1:8080/cwexperimental/tabledap/testSimpleTestNcTable.html</Source>
"//<Version>ODV Spreadsheet V4.0</Version>\n" +
"//<DataField>GeneralField</DataField>\n" +
"//<DataType>GeneralType</DataType>\n" +
"Type:METAVAR:TEXT:2\tCruise:METAVAR:TEXT:2\tStation:METAVAR:TEXT:2\tyyyy-mm-ddThh:mm:ss.sss\ttime_ISO8601\tLatitude [degrees_north]:METAVAR:BYTE\tLongitude [degrees_east]:METAVAR:SHORT\tdoubles:PRIMARYVAR:DOUBLE\tStrings:METAVAR:TEXT:3\n" +
"*\t\t\t1970-01-03T00:00:00Z\t2010-01-01T00:00:00.031Z\t41\t10001\t1.0000000000001E12\t10\n" +
"*\t\t\t1970-01-04T00:00:00Z\t2010-01-01T00:00:00.032Z\t42\t10002\t1.0000000000002E12\t20\n";
        po = results.indexOf("//<Version>");
        ts = results.substring(Math.max(0, po), Math.min(results.length(), po + expected.length())); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

        //.xhtml  
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".xhtml"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
"  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
"<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
"<head>\n" +
"  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n" +
"  <title>testSimpleTestNc2Table</title>\n" +
"</head>\n" +
"<body style=\"color:black; background:white; font-family:Arial,Helvetica,sans-serif; font-size:85%; line-height:130%;\">\n" +
"\n" +
"&nbsp;\n" +
"<table border=\"1\" cellpadding=\"2\" cellspacing=\"0\">\n" +
"<tr>\n" +
"<th>time</th>\n" +
"<th>millis</th>\n" +
"<th>latitude</th>\n" +
"<th>longitude</th>\n" +
"<th>doubles</th>\n" +
"<th>Strings</th>\n" +
"</tr>\n" +
"<tr>\n" +
"<th>UTC</th>\n" +
"<th>UTC</th>\n" +
"<th>degrees_north</th>\n" +
"<th>degrees_east</th>\n" +
"<th></th>\n" +
"<th></th>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\">1970-01-03T00:00:00Z</td>\n" +
"<td nowrap=\"nowrap\">2010-01-01T00:00:00.031Z</td>\n" +
"<td align=\"right\">41</td>\n" +
"<td align=\"right\">10001</td>\n" +
"<td align=\"right\">1.0000000000001E12</td>\n" +
"<td nowrap=\"nowrap\">10</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\">1970-01-04T00:00:00Z</td>\n" +
"<td nowrap=\"nowrap\">2010-01-01T00:00:00.032Z</td>\n" +
"<td align=\"right\">42</td>\n" +
"<td align=\"right\">10002</td>\n" +
"<td align=\"right\">1.0000000000002E12</td>\n" +
"<td nowrap=\"nowrap\">20</td>\n" +
"</tr>\n" +
"</table>\n" +
"</body>\n" +
"</html>\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
    }

    /**
     * This tests the EDDTableFromFiles.update().
     * This tests with a variable from the file name, a fixed value variable,
     * and 2 global: variables.
     *
     * @throws Throwable if trouble
     */
    public static void testUpdate() throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.testUpdate() *****************\n");
        EDDTableFromNcFiles eddTable = (EDDTableFromNcFiles)oneFromDatasetsXml(null, "miniNdbc"); 
        EDV timeEdv = eddTable.dataVariables()[eddTable.timeIndex];
        EDV lonEdv  = eddTable.dataVariables()[eddTable.lonIndex];
        String dataDir = eddTable.fileDir;
        String tDir = EDStatic.fullTestCacheDirectory;
        String subsetQuery = "station,longitude,latitude&distinct()";
        String dataQuery = "station,longitude,latitude,geolon,geolat,luckySeven,time,atmp&time=\"2014-12-01T00:00:00\"";
        String tName, results, expected;

        //fix trouble if left in bad state previously
        if (!File2.isFile(dataDir + "NDBC_41025_met.nc") &&
             File2.isFile(dataDir + "NDBC_41025_met.nc2")) {
            File2.rename(dataDir, "NDBC_41025_met.nc2", "NDBC_41025_met.nc");
            for (int i = 0; i < 3; i++) { //Windows is slow, give it a few tries
                Math2.sleep(1000);
                String2.log("after rename .nc2 to .nc, call update() i=" + i + ": " + 
                    eddTable.update());
            }
        }

        String originalExpectedSubset = 
"station,longitude,latitude\n" +
",degrees_east,degrees_north\n" +
"41024,-78.489,33.848\n" +
"41025,-75.402,35.006\n" + //this has max lon and min and max time
"41029,-79.63,32.81\n" +
"41033,-80.41,32.28\n"; 

        String originalExpectedData = 
"station,longitude,latitude,geolon,geolat,luckySeven,time,atmp\n" +
",degrees_east,degrees_north,degrees_north,degrees_north,m,UTC,degree_C\n" +
"41024,-78.489,33.848,-78.489,33.848,7.0,2014-12-01T00:00:00Z,14.2\n" +
"41025,-75.402,35.006,-75.402,35.006,7.0,2014-12-01T00:00:00Z,19.5\n" +
"41029,-79.63,32.81,-79.63,32.81,7.0,2014-12-01T00:00:00Z,14.5\n" +
"41033,-80.41,32.28,-80.41,32.28,7.0,2014-12-01T00:00:00Z,NaN\n";

        String oldMinTime   = "2003-03-28T19:00:00Z";
        String oldMinMillis = "1.048878E9";
        String newMinTime   = "2005-02-23T15:00:00Z"; //after renaming a file to make it invalid
        String newMinMillis = "1.1091708E9";
        String oldMaxTime   = "2015-01-23T22:00:00Z";
        String oldMaxMillis = "1.4220504E9";
        String newMaxTime   = "2015-01-23T21:00:00Z";
        String newMaxMillis = "1.4220468E9";

        String oldMinLon = "-80.41";
        String oldMaxLon = "-75.402";
        String newMaxLon = "-78.489";

        //subsetVariables
        tName = eddTable.makeNewFileForDapQuery(null, null, subsetQuery, tDir, 
            eddTable.className() + "_update_0sub", ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        Test.ensureEqual(results, originalExpectedSubset, "\nresults=\n" + results);

        //time min/max
        Test.ensureEqual(timeEdv.destinationMinString(), 
            oldMinTime, "edvTime.destinationMin");
        Test.ensureEqual(timeEdv.destinationMaxString(), 
            oldMaxTime, "edvTime.destinationMax");
        Test.ensureEqual(timeEdv.combinedAttributes().get("actual_range").toString(),
            oldMinMillis + ", " + oldMaxMillis, "actual_range");
        Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("time_coverage_start"),
            oldMinTime, "time_coverage_start");
        Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("time_coverage_end"),
            oldMaxTime, "time_coverage_end");

        //lon min/max
        Test.ensureEqual(lonEdv.destinationMinString(), 
            oldMinLon, "edvLon.destinationMin");
        Test.ensureEqual(lonEdv.destinationMaxString(), 
            oldMaxLon, "edvLon.destinationMax");
        Test.ensureEqual(lonEdv.combinedAttributes().get("actual_range").toString(),
            oldMinLon + ", " + oldMaxLon, "actual_range");
        Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("geospatial_lon_min"),
            oldMinLon, "geospatial_lon_min");
        Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("geospatial_lon_max"),
            oldMaxLon, "geospatial_lon_max");

        //*** read the original data
        String2.log("\n*** read original data\n");       
        tName = eddTable.makeNewFileForDapQuery(null, null, dataQuery, tDir, 
            eddTable.className() + "_update_0d", ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        Test.ensureEqual(results, originalExpectedData, "\nresults=\n" + results);

        //*** rename a data file so it doesn't match regex
        try {
            String2.log("\n*** rename a data file so it doesn't match regex\n");       
            File2.rename(dataDir, "NDBC_41025_met.nc", "NDBC_41025_met.nc2");
            for (int i = 0; i < 5; i++) { //Windows is slow, give it a few tries
                Math2.sleep(1000);
                String2.log("after rename .nc to .nc2, call update() i=" + i + ": " + 
                    eddTable.update());
            }

            //subsetVariables should be different
            tName = eddTable.makeNewFileForDapQuery(null, null, subsetQuery, tDir, 
                eddTable.className() + "_update_1sub", ".csv"); 
            results = new String((new ByteArray(tDir + tName)).toArray());
            expected = 
"station,longitude,latitude\n" +
",degrees_east,degrees_north\n" +
"41024,-78.489,33.848\n" +
//"41025,-75.402,35.006\n" + //shouldn't be in results!
"41029,-79.63,32.81\n" +
"41033,-80.41,32.28\n"; 
            try {
                Test.ensureEqual(results, expected, "\nresults=\n" + results);
            } catch (Throwable t3) {
                String2.pressEnterToContinue(MustBe.throwableToString(t3) + 
                    "Eeek!!! Known problem!!!\n" +
                    "update() doesn't update subsetVariables (which is fine most of the time).");
            }

            //min and max time should be different
            Test.ensureEqual(timeEdv.destinationMinString(), 
                newMinTime, "edvTime.destinationMin");
            Test.ensureEqual(timeEdv.destinationMaxString(), 
                newMaxTime, "edvTime.destinationMax");
            Test.ensureEqual(timeEdv.combinedAttributes().get("actual_range").toString(),
                newMinMillis + ", " + newMaxMillis, "actual_range");
            Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("time_coverage_start"),
                newMinTime, "time_coverage_start");
            Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("time_coverage_end"),
                newMaxTime, "time_coverage_end");

            //max lon should be different
            Test.ensureEqual(lonEdv.destinationMinString(), 
                oldMinLon, "edvLon.destinationMin");
            Test.ensureEqual(lonEdv.destinationMaxString(), 
                newMaxLon, "edvLon.destinationMax");
            Test.ensureEqual(lonEdv.combinedAttributes().get("actual_range").toString(),
                oldMinLon + ", " + newMaxLon, "actual_range");
            Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("geospatial_lon_min"),
                oldMinLon, "geospatial_lon_min");
            Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("geospatial_lon_max"),
                newMaxLon, "geospatial_lon_max");

            //data will be different
            tName = eddTable.makeNewFileForDapQuery(null, null, dataQuery, tDir, 
                eddTable.className() + "_update_1d", ".csv"); 
            results = new String((new ByteArray(tDir + tName)).toArray());
            expected = 
"station,longitude,latitude,geolon,geolat,luckySeven,time,atmp\n" +
",degrees_east,degrees_north,degrees_north,degrees_north,m,UTC,degree_C\n" +
"41024,-78.489,33.848,-78.489,33.848,7.0,2014-12-01T00:00:00Z,14.2\n" +
//"41025,-75.402,35.006,-75.402,35.006,7.0,2014-12-01T00:00:00Z,19.5\n" +
"41029,-79.63,32.81,-79.63,32.81,7.0,2014-12-01T00:00:00Z,14.5\n" +
"41033,-80.41,32.28,-80.41,32.28,7.0,2014-12-01T00:00:00Z,NaN\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);

        } finally {
            //rename it back to original
            String2.log("\n*** rename it back to original\n");       
            File2.rename(dataDir, "NDBC_41025_met.nc2", "NDBC_41025_met.nc");
            for (int i = 0; i < 5; i++) { //Windows is slow, give it a few tries
                Math2.sleep(1000);
                String2.log("after rename .nc2 to .nc, call update() i=" + i + ": " + 
                    eddTable.update());
            }
        }

        //*** back to original
        String2.log("\n*** after back to original\n");       

        //should be original subsetVariables
        tName = eddTable.makeNewFileForDapQuery(null, null, subsetQuery, tDir, 
            eddTable.className() + "_update_2sub", ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        Test.ensureEqual(results, originalExpectedSubset, "\nresults=\n" + results);

        //should be original min/max time
        Test.ensureEqual(timeEdv.destinationMinString(), 
            oldMinTime, "edvTime.destinationMin");
        Test.ensureEqual(timeEdv.destinationMaxString(), 
            oldMaxTime, "edvTime.destinationMax");
        Test.ensureEqual(timeEdv.combinedAttributes().get("actual_range").toString(),
            oldMinMillis + ", " + oldMaxMillis, "actual_range");
        Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("time_coverage_start"),
            oldMinTime, "time_coverage_start");
        Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("time_coverage_end"),
            oldMaxTime, "time_coverage_end");

        //should be original min/max lon
        Test.ensureEqual(lonEdv.destinationMinString(), 
            oldMinLon, "edvLon.destinationMin");
        Test.ensureEqual(lonEdv.destinationMaxString(), 
            oldMaxLon, "edvLon.destinationMax");
        Test.ensureEqual(lonEdv.combinedAttributes().get("actual_range").toString(),
            oldMinLon + ", " + oldMaxLon, "actual_range");
        Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("geospatial_lon_min"),
            oldMinLon, "geospatial_lon_min");
        Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("geospatial_lon_max"),
            oldMaxLon, "geospatial_lon_max");

        //should be original data
        tName = eddTable.makeNewFileForDapQuery(null, null, dataQuery, tDir, 
            eddTable.className() + "_update_2d", ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        Test.ensureEqual(results, originalExpectedData, "\nresults=\n" + results);

        //*** rename a non-data file so it matches the regex
        try {
            String2.log("\n*** rename an invalid file to be a valid name\n");       
            File2.rename(dataDir, "image.png", "NDBC_image_met.nc");
            for (int i = 0; i < 3; i++) { //Windows is slow, give it a few tries
                Math2.sleep(1000);
                String2.log("after rename .notnc to .nc, call update i=" + i + ": " + 
                    eddTable.update());
            }

            //should be original subsetVariables
            tName = eddTable.makeNewFileForDapQuery(null, null, subsetQuery, tDir, 
                eddTable.className() + "_update_3sub", ".csv"); 
            results = new String((new ByteArray(tDir + tName)).toArray());
            Test.ensureEqual(results, originalExpectedSubset, "\nresults=\n" + results);

            //should be original min/max time
            Test.ensureEqual(timeEdv.destinationMinString(), 
                oldMinTime, "edvTime.destinationMin");
            Test.ensureEqual(timeEdv.destinationMaxString(), 
                oldMaxTime, "edvTime.destinationMax");
            Test.ensureEqual(timeEdv.combinedAttributes().get("actual_range").toString(),
                oldMinMillis + ", " + oldMaxMillis, "actual_range");
            Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("time_coverage_start"),
                oldMinTime, "time_coverage_start");
            Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("time_coverage_end"),
                oldMaxTime, "time_coverage_end");

            //should be original lon min/max
            Test.ensureEqual(lonEdv.destinationMinString(), 
                oldMinLon, "edvLon.destinationMin");
            Test.ensureEqual(lonEdv.destinationMaxString(), 
                oldMaxLon, "edvLon.destinationMax");
            Test.ensureEqual(lonEdv.combinedAttributes().get("actual_range").toString(),
                oldMinLon + ", " + oldMaxLon, "actual_range");
            Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("geospatial_lon_min"),
                oldMinLon, "geospatial_lon_min");
            Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("geospatial_lon_max"),
                oldMaxLon, "geospatial_lon_max");

            //should be original data
            tName = eddTable.makeNewFileForDapQuery(null, null, dataQuery, tDir, 
                eddTable.className() + "_update_3d", ".csv"); 
            results = new String((new ByteArray(tDir + tName)).toArray());
            Test.ensureEqual(results, originalExpectedData, "\nresults=\n" + results);


        } finally {
            //rename it back to original
            String2.log("\n*** rename it back to original\n");       
            File2.rename(dataDir, "NDBC_image_met.nc", "image.png");
            Math2.sleep(1000);
            String2.log("after rename .nc to .notnc, call update():\n" + eddTable.update());
        }

        String2.log("\n*** after back to original again\n");       

        //should be original subsetVariables
        tName = eddTable.makeNewFileForDapQuery(null, null, subsetQuery, tDir, 
            eddTable.className() + "_update_4sub", ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        Test.ensureEqual(results, originalExpectedSubset, "\nresults=\n" + results);

        //should be original min/max time
        Test.ensureEqual(timeEdv.destinationMinString(), 
            oldMinTime, "edvTime.destinationMin");
        Test.ensureEqual(timeEdv.destinationMaxString(), 
            oldMaxTime, "edvTime.destinationMax");
        Test.ensureEqual(timeEdv.combinedAttributes().get("actual_range").toString(),
            oldMinMillis + ", " + oldMaxMillis, "actual_range");
        Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("time_coverage_start"),
            oldMinTime, "time_coverage_start");
        Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("time_coverage_end"),
            oldMaxTime, "time_coverage_end");

        //should be original lon min/max
        Test.ensureEqual(lonEdv.destinationMinString(), 
            oldMinLon, "edvLon.destinationMin");
        Test.ensureEqual(lonEdv.destinationMaxString(), 
            oldMaxLon, "edvLon.destinationMax");
        Test.ensureEqual(lonEdv.combinedAttributes().get("actual_range").toString(),
            oldMinLon + ", " + oldMaxLon, "actual_range");
        Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("geospatial_lon_min"),
            oldMinLon, "geospatial_lon_min");
        Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("geospatial_lon_max"),
            oldMaxLon, "geospatial_lon_max");

        //should be original data
        tName = eddTable.makeNewFileForDapQuery(null, null, dataQuery, tDir, 
            eddTable.className() + "_update_4d", ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        Test.ensureEqual(results, originalExpectedData, "\nresults=\n" + results);

    }

    /**
     * This tests the EDDTableFromFiles quickRestart().
     *
     * @throws Throwable if trouble
     */
    public static void testQuickRestart() throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.testQuickRestart() *****************\n");
        EDDTableFromNcFiles eddTable; 
        EDV timeEdv, lonEdv;
        String tDir = EDStatic.fullTestCacheDirectory;
        String subsetQuery = "station,longitude,latitude&distinct()";
        String dataQuery = "station,longitude,latitude,geolon,geolat,luckySeven,time,atmp&time=\"2014-12-01T00:00:00\"";
        String tName, results, expected;
        int po;

        String originalDas1 =
"Attributes {\n" +
" s {\n" +
"  station {\n" +
"    String cf_role \"timeseries_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station Name\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 actual_range -80.41, -75.402;\n" +
"    String axis \"X\";\n" +
"    String comment \"The longitude of the station.\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range 32.28, 35.006;\n" +
"    String axis \"Y\";\n" +
"    String comment \"The latitude of the station.\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.048878e+9, 1.4220504e+9;\n" +
"    String axis \"T\";\n" +
"    String comment \"Time in seconds since 1970-01-01T00:00:00Z. The original times are rounded to the nearest hour.\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  luckySeven {\n" +
"    Float32 actual_range 7.0, 7.0;\n" +
"    String comment \"fixed value\";\n" +
"    String ioos_category \"Other\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  geolon {\n" +
"    Float32 actual_range -80.41, -75.402;\n" +
"    String ioos_category \"Location\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  geolat {\n" +
"    Float32 actual_range 32.28, 35.006;\n" +
"    String ioos_category \"Location\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  wd {\n" +
"    Int16 _FillValue 32767;\n" +
"    Int16 actual_range 0, 359;\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"Wind direction (the direction the wind is coming from in degrees clockwise from true N) during the same period used for WSPD. See Wind Averaging Methods.\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind Direction\";\n" +
"    Int16 missing_value 32767;\n" +
"    String standard_name \"wind_from_direction\";\n" +
"    String units \"degrees_true\";\n" +
"  }\n" +
"  wspd {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 0.0, 96.0;\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"Wind speed (m/s) averaged over an eight-minute period for buoys and a two-minute period for land stations. Reported Hourly. See Wind Averaging Methods.\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind Speed\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"wind_speed\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  gst {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 0.0, 60.0;\n" +
"    Float64 colorBarMaximum 30.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"Peak 5 or 8 second gust speed (m/s) measured during the eight-minute or two-minute period. The 5 or 8 second period can be determined by payload, See the Sensor Reporting, Sampling, and Accuracy section.\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind Gust Speed\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"wind_speed_of_gust\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  wvht {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 0.0, 13.63;\n" +
"    Float64 colorBarMaximum 10.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"Significant wave height (meters) is calculated as the average of the highest one-third of all of the wave heights during the 20-minute sampling period. See the Wave Measurements section.\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Wave Height\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"sea_surface_wave_significant_height\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  dpd {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 0.0, 30.77;\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"Dominant wave period (seconds) is the period with the maximum wave energy. See the Wave Measurements section.\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Wave Period, Dominant\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"sea_surface_swell_wave_period\";\n" +
"    String units \"s\";\n" +
"  }\n" +
"  apd {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 0.0, 14.26;\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"Average wave period (seconds) of all waves during the 20-minute period. See the Wave Measurements section.\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Wave Period, Average\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"sea_surface_swell_wave_period\";\n" +
"    String units \"s\";\n" +
"  }\n" +
"  mwd {\n" +
"    Int16 _FillValue 32767;\n" +
"    Int16 actual_range 0, 359;\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"Mean wave direction corresponding to energy of the dominant period (DOMPD). The units are degrees from true North just like wind direction. See the Wave Measurements section.\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Wave Direction\";\n" +
"    Int16 missing_value 32767;\n" +
"    String standard_name \"sea_surface_wave_to_direction\";\n" +
"    String units \"degrees_true\";\n" +
"  }\n" +
"  bar {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 984.5, 1043.2;\n" +
"    Float64 colorBarMaximum 1050.0;\n" +
"    Float64 colorBarMinimum 950.0;\n" +
"    String comment \"Air pressure (hPa). ('PRES' on some NDBC tables.) For C-MAN sites and Great Lakes buoys, the recorded pressure is reduced to sea level using the method described in NWS Technical Procedures Bulletin 291 (11/14/80).\";\n" +
"    String ioos_category \"Pressure\";\n" +
"    String long_name \"Air Pressure\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"air_pressure_at_sea_level\";\n" +
"    String units \"hPa\";\n" +
"  }\n" +
"  atmp {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range -5.9, 35.9;\n" +
"    Float64 colorBarMaximum 40.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"Air temperature (Celsius). For sensor heights on buoys, see Hull Descriptions. For sensor heights at C-MAN stations, see C-MAN Sensor Locations.\";\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Air Temperature\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"air_temperature\";\n" +
"    String units \"degree_C\";\n" +
"  }\n" +
"  wtmp {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 4.3, 32.6;\n" +
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"Sea surface temperature (Celsius). For sensor depth, see Hull Description.\";\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"SST\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"sea_surface_temperature\";\n" +
"    String units \"degree_C\";\n" +
"  }\n" +
"  dewp {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range -11.6, 28.2;\n" +
"    Float64 colorBarMaximum 40.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"Dewpoint temperature taken at the same height as the air temperature measurement.\";\n" +
"    String ioos_category \"Meteorology\";\n" +
"    String long_name \"Dewpoint Temperature\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"dew_point_temperature\";\n" +
"    String units \"degree_C\";\n" +
"  }\n" +
"  vis {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 0.0, 10.5;\n" +
"    Float64 colorBarMaximum 100.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"Station visibility (km, originally statute miles). Note that buoy stations are limited to reports from 0 to 1.9 miles.\";\n" +
"    String ioos_category \"Meteorology\";\n" +
"    String long_name \"Station Visibility\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"visibility_in_air\";\n" +
"    String units \"km\";\n" +
"  }\n" +
"  ptdy {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range -5.5, 4.1;\n" +
"    Float64 colorBarMaximum 3.0;\n" +
"    Float64 colorBarMinimum -3.0;\n" +
"    String comment \"Pressure Tendency is the direction (plus or minus) and the amount of pressure change (hPa) for a three hour period ending at the time of observation.\";\n" +
"    String ioos_category \"Pressure\";\n" +
"    String long_name \"Pressure Tendency\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"tendency_of_air_pressure\";\n" +
"    String units \"hPa\";\n" +
"  }\n" +
"  tide {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float64 colorBarMaximum 5.0;\n" +
"    Float64 colorBarMinimum -5.0;\n" +
"    String comment \"The water level in meters (originally feet) above or below Mean Lower Low Water (MLLW).\";\n" +
"    String ioos_category \"Sea Level\";\n" +
"    String long_name \"Water Level\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"surface_altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  wspu {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range -27.2, 19.1;\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum -15.0;\n" +
"    String comment \"The zonal wind speed (m/s) indicates the u component of where the wind is going, derived from Wind Direction and Wind Speed.\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind Speed, Zonal\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"eastward_wind\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  wspv {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range -26.6, 24.2;\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum -15.0;\n" +
"    String comment \"The meridional wind speed (m/s) indicates the v component of where the wind is going, derived from Wind Direction and Wind Speed.\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind Speed, Meridional\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"northward_wind\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String acknowledgement \"NOAA NDBC and NOAA CoastWatch (West Coast Node)\";\n" +
"    String cdm_data_type \"TimeSeries\";\n" +
"    String cdm_timeseries_variables \"station, longitude, latitude\";\n" +
"    String contributor_name \"NOAA NDBC and NOAA CoastWatch (West Coast Node)\";\n" +
"    String contributor_role \"Source of data.\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String creator_email \"dave.foley@noaa.gov\";\n" +
"    String creator_name \"NOAA CoastWatch, West Coast Node\";\n" +
"    String creator_url \"http://coastwatch.pfeg.noaa.gov\";\n" +
"    Float64 Easternmost_Easting -75.402;\n" +
"    String featureType \"TimeSeries\";\n" +
"    Float64 geospatial_lat_max 35.006;\n" +
"    Float64 geospatial_lat_min 32.28;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max -75.402;\n" +
"    Float64 geospatial_lon_min -80.41;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String geospatial_vertical_positive \"down\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"NOAA NDBC\n";
        
//"2015-09-10T15:31:18Z http://www.ndbc.noaa.gov/\n" +
//"2015-09-10T15:31:18Z 
        String originalDas2 =
"http://127.0.0.1:8080/cwexperimental/tabledap/miniNdbc.das\";\n" +
"    String infoUrl \"http://www.ndbc.noaa.gov/\";\n" +
"    String institution \"NOAA NDBC, CoastWatch WCN\";\n" +
"    String keywords \"Atmosphere > Air Quality > Visibility,\n" +
"Atmosphere > Altitude > Planetary Boundary Layer Height,\n" +
"Atmosphere > Atmospheric Pressure > Atmospheric Pressure Measurements,\n" +
"Atmosphere > Atmospheric Pressure > Pressure Tendency,\n" +
"Atmosphere > Atmospheric Pressure > Sea Level Pressure,\n" +
"Atmosphere > Atmospheric Pressure > Static Pressure,\n" +
"Atmosphere > Atmospheric Temperature > Air Temperature,\n" +
"Atmosphere > Atmospheric Temperature > Dew Point Temperature,\n" +
"Atmosphere > Atmospheric Water Vapor > Dew Point Temperature,\n" +
"Atmosphere > Atmospheric Winds > Surface Winds,\n" +
"Oceans > Ocean Temperature > Sea Surface Temperature,\n" +
"Oceans > Ocean Waves > Significant Wave Height,\n" +
"Oceans > Ocean Waves > Swells,\n" +
"Oceans > Ocean Waves > Wave Period,\n" +
"air, air_pressure_at_sea_level, air_temperature, atmosphere, atmospheric, average, boundary, buoy, coastwatch, data, dew point, dew_point_temperature, direction, dominant, eastward, eastward_wind, from, gust, height, identifier, layer, level, measurements, meridional, meteorological, meteorology, name, ndbc, noaa, northward, northward_wind, ocean, oceans, period, planetary, pressure, quality, sea, sea level, sea_surface_swell_wave_period, sea_surface_swell_wave_significant_height, sea_surface_swell_wave_to_direction, sea_surface_temperature, seawater, significant, speed, sst, standard, static, station, surface, surface waves, surface_altitude, swell, swells, temperature, tendency, tendency_of_air_pressure, time, vapor, visibility, visibility_in_air, water, wave, waves, wcn, wind, wind_from_direction, wind_speed, wind_speed_of_gust, winds, zonal\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String naming_authority \"gov.noaa.pfeg.coastwatch\";\n" +
"    String NDBCMeasurementDescriptionUrl \"http://www.ndbc.noaa.gov/measdes.shtml\";\n" +
"    Float64 Northernmost_Northing 35.006;\n" +
"    String project \"NOAA NDBC and NOAA CoastWatch (West Coast Node)\";\n" +
"    String quality \"Automated QC checks with periodic manual QC\";\n" +
"    String source \"station observation\";\n" +
"    String sourceUrl \"http://www.ndbc.noaa.gov/\";\n" +
"    Float64 Southernmost_Northing 32.28;\n" +
"    String standard_name_vocabulary \"CF-12\";\n" +
"    String subsetVariables \"station, longitude, latitude\";\n" +
"    String summary \"The National Data Buoy Center (NDBC) distributes meteorological data from\n" +
"moored buoys maintained by NDBC and others. Moored buoys are the weather\n" +
"sentinels of the sea. They are deployed in the coastal and offshore waters\n" +
"from the western Atlantic to the Pacific Ocean around Hawaii, and from the\n" +
"Bering Sea to the South Pacific. NDBC's moored buoys measure and transmit\n" +
"barometric pressure; wind direction, speed, and gust; air and sea\n" +
"temperature; and wave energy spectra from which significant wave height,\n" +
"dominant wave period, and average wave period are derived. Even the\n" +
"direction of wave propagation is measured on many moored buoys.\n" +
"\n" +
"The data is from NOAA NDBC. It has been reformatted by NOAA Coastwatch,\n" +
"West Coast Node. This dataset only has the data that is closest to a\n" +
"given hour. The time values in the dataset are rounded to the nearest hour.\n" +
"\n" +
"This dataset has both historical data (quality controlled, before\n" +
"2015-01-01T00:00:00Z) and near real time data (less quality controlled, from\n" +
"2015-01-01T00:00:00Z on).\";\n" +
"    String time_coverage_end \"2015-01-23T22:00:00Z\";\n" +
"    String time_coverage_resolution \"P1H\";\n" +
"    String time_coverage_start \"2003-03-28T19:00:00Z\";\n" +
"    String title \"NDBC Standard Meteorological Buoy Data\";\n" +
"    Float64 Westernmost_Easting -80.41;\n" +
"  }\n" +
"}\n";
        String originalSubsetExpected = 
"station,longitude,latitude\n" +
",degrees_east,degrees_north\n" +
"41024,-78.489,33.848\n" +
"41025,-75.402,35.006\n" + //this has max lon and max time
"41029,-79.63,32.81\n" +
"41033,-80.41,32.28\n"; 

        String oldMinTime   = "2003-03-28T19:00:00Z";
        String oldMinMillis = "1.048878E9";
        String newMinTime   = "2005-02-23T15:00:00Z"; //after renaming a file to make it invalid
        String newMinMillis = "1.1091708E9";
        String oldMaxTime   = "2015-01-23T22:00:00Z";
        String oldMaxMillis = "1.4220504E9";
        String newMaxTime   = "2015-01-23T21:00:00Z";
        String newMaxMillis = "1.4220468E9";

        String oldMinLon = "-80.41";
        String oldMaxLon = "-75.402";

        String originalExpectedData = 
"station,longitude,latitude,geolon,geolat,luckySeven,time,atmp\n" +
",degrees_east,degrees_north,degrees_north,degrees_north,m,UTC,degree_C\n" +
"41024,-78.489,33.848,-78.489,33.848,7.0,2014-12-01T00:00:00Z,14.2\n" +
"41025,-75.402,35.006,-75.402,35.006,7.0,2014-12-01T00:00:00Z,19.5\n" +
"41029,-79.63,32.81,-79.63,32.81,7.0,2014-12-01T00:00:00Z,14.5\n" +
"41033,-80.41,32.28,-80.41,32.28,7.0,2014-12-01T00:00:00Z,NaN\n";

        //*** Do tests of original data
        eddTable = (EDDTableFromNcFiles)oneFromDatasetsXml(null, "miniNdbc"); 
        String dataDir = eddTable.fileDir;
        timeEdv = eddTable.dataVariables()[eddTable.timeIndex];
        lonEdv  = eddTable.dataVariables()[eddTable.lonIndex];
        long oCreationTimeMillis = eddTable.creationTimeMillis();

        //das
        tName = eddTable.makeNewFileForDapQuery(null, null, "", tDir, 
            eddTable.className() + "_qr_0das", ".das"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        Test.ensureEqual(results.substring(0, originalDas1.length()), originalDas1, "\nresults=\n" + results);

        po = results.indexOf(originalDas2.substring(0, 80));
        Test.ensureEqual(results.substring(po), originalDas2, "\nresults=\n" + results);

        //subsetVariables
        tName = eddTable.makeNewFileForDapQuery(null, null, subsetQuery, tDir, 
            eddTable.className() + "_qr_0sub", ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        Test.ensureEqual(results, originalSubsetExpected, "\nresults=\n" + results);

        //original min/max time
        Test.ensureEqual(timeEdv.destinationMinString(), 
            oldMinTime, "edvTime.destinationMin");
        Test.ensureEqual(timeEdv.destinationMaxString(), 
            oldMaxTime, "edvTime.destinationMax");
        Test.ensureEqual(timeEdv.combinedAttributes().get("actual_range").toString(),
            oldMinMillis + ", " + oldMaxMillis, "actual_range");
        Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("time_coverage_start"),
            oldMinTime, "time_coverage_start");
        Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("time_coverage_end"),
            oldMaxTime, "time_coverage_end");

        //original lon min/max
        Test.ensureEqual(lonEdv.destinationMinString(), 
            oldMinLon, "edvLon.destinationMin");
        Test.ensureEqual(lonEdv.destinationMaxString(), 
            oldMaxLon, "edvLon.destinationMax");
        Test.ensureEqual(lonEdv.combinedAttributes().get("actual_range").toString(),
            oldMinLon + ", " + oldMaxLon, "actual_range");
        Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("geospatial_lon_min"),
            oldMinLon, "geospatial_lon_min");
        Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("geospatial_lon_max"),
            oldMaxLon, "geospatial_lon_max");

        //read the original data
        String2.log("\n*** read original data\n");       
        tName = eddTable.makeNewFileForDapQuery(null, null, dataQuery, tDir, 
            eddTable.className() + "_qr_0d", ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        Test.ensureEqual(results, originalExpectedData, "\nresults=\n" + results);


        //*** rename a data file so it doesn't match regex
        try {
            String2.log("\n*** rename a data file so it doesn't match regex\n");       
            File2.rename(dataDir, "NDBC_41025_met.nc", "NDBC_41025_met.nc2");
            Math2.sleep(1000);
            EDDTableFromFiles.testQuickRestart = true;
            eddTable = (EDDTableFromNcFiles)oneFromDatasetsXml(null, "miniNdbc"); 
            timeEdv = eddTable.dataVariables()[eddTable.timeIndex];
            lonEdv  = eddTable.dataVariables()[eddTable.lonIndex];
            Test.ensureEqual(eddTable.creationTimeMillis(), oCreationTimeMillis, "");

            //should be original das
            tName = eddTable.makeNewFileForDapQuery(null, null, "", tDir, 
                eddTable.className() + "_qr_1das", ".das"); 
            results = new String((new ByteArray(tDir + tName)).toArray());
            Test.ensureEqual(results.substring(0, originalDas1.length()), originalDas1, "\nresults=\n" + results);

            po = results.indexOf(originalDas2.substring(0, 80));
            Test.ensureEqual(results.substring(po), originalDas2, "\nresults=\n" + results);

            //should be original subsetVariables
            tName = eddTable.makeNewFileForDapQuery(null, null, subsetQuery, tDir, 
                eddTable.className() + "_qr_1sub", ".csv"); 
            results = new String((new ByteArray(tDir + tName)).toArray());
            Test.ensureEqual(results, originalSubsetExpected, "\nresults=\n" + results);

            //should be original min/max time
            Test.ensureEqual(timeEdv.destinationMinString(), 
                oldMinTime, "edvTime.destinationMin");
            Test.ensureEqual(timeEdv.destinationMaxString(), 
                oldMaxTime, "edvTime.destinationMax");
            Test.ensureEqual(timeEdv.combinedAttributes().get("actual_range").toString(),
                oldMinMillis + ", " + oldMaxMillis, "actual_range");
            Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("time_coverage_start"),
                oldMinTime, "time_coverage_start");
            Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("time_coverage_end"),
                oldMaxTime, "time_coverage_end");

            //should be original lon min/max
            Test.ensureEqual(lonEdv.destinationMinString(), 
                oldMinLon, "edvLon.destinationMin");
            Test.ensureEqual(lonEdv.destinationMaxString(), 
                oldMaxLon, "edvLon.destinationMax");
            Test.ensureEqual(lonEdv.combinedAttributes().get("actual_range").toString(),
                oldMinLon + ", " + oldMaxLon, "actual_range");
            Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("geospatial_lon_min"),
                oldMinLon, "geospatial_lon_min");
            Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("geospatial_lon_max"),
                oldMaxLon, "geospatial_lon_max");

            //but read the original data will be different
            try {
                tName = eddTable.makeNewFileForDapQuery(null, null, dataQuery, tDir, 
                    eddTable.className() + "_qr_1d", ".csv"); 
                results = "shouldn't happen";
            } catch (Throwable t2) {
                results = t2.getMessage();
            }
            expected = 
"There was a (temporary?) problem.  Wait a minute, then try again.  (In a browser, click the Reload button.)\n" +
"(Cause: java.io.FileNotFoundException: \\erddapTest\\miniNdbc\\NDBC_41025_met.nc (The system cannot find the file specified))";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);


        } finally {
            //rename it back to original
            String2.log("\n*** rename it back to original\n");       
            File2.rename(dataDir, "NDBC_41025_met.nc2", "NDBC_41025_met.nc");
            //ensure testQuickRestart is set back to false
            EDDTableFromFiles.testQuickRestart = false;
            Math2.sleep(1000);
        }

        //*** back to original
        String2.log("\n*** after back to original\n");       
        eddTable = (EDDTableFromNcFiles)oneFromDatasetsXml(null, "miniNdbc"); 
        timeEdv = eddTable.dataVariables()[eddTable.timeIndex];
        lonEdv  = eddTable.dataVariables()[eddTable.lonIndex];
        //creationTime should have changed
        Test.ensureNotEqual(eddTable.creationTimeMillis(), oCreationTimeMillis, "");

        //but everything else should be back to original
        //das
        tName = eddTable.makeNewFileForDapQuery(null, null, "", tDir, 
            eddTable.className() + "_qr_2das", ".das"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        Test.ensureEqual(results.substring(0, originalDas1.length()), originalDas1, "\nresults=\n" + results);

        po = results.indexOf(originalDas2.substring(0, 80));
        Test.ensureEqual(results.substring(po), originalDas2, "\nresults=\n" + results);

        //should be original subsetVariables
        tName = eddTable.makeNewFileForDapQuery(null, null, subsetQuery, tDir, 
            eddTable.className() + "_qr_2sub", ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        Test.ensureEqual(results, originalSubsetExpected, "\nresults=\n" + results);

        //should be original min/max time
        Test.ensureEqual(timeEdv.destinationMinString(), 
            oldMinTime, "edvTime.destinationMin");
        Test.ensureEqual(timeEdv.destinationMaxString(), 
            oldMaxTime, "edvTime.destinationMax");
        Test.ensureEqual(timeEdv.combinedAttributes().get("actual_range").toString(),
            oldMinMillis + ", " + oldMaxMillis, "actual_range");
        Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("time_coverage_start"),
            oldMinTime, "time_coverage_start");
        Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("time_coverage_end"),
            oldMaxTime, "time_coverage_end");

        //should be original lon min/max
        Test.ensureEqual(lonEdv.destinationMinString(), 
            oldMinLon, "edvLon.destinationMin");
        Test.ensureEqual(lonEdv.destinationMaxString(), 
            oldMaxLon, "edvLon.destinationMax");
        Test.ensureEqual(lonEdv.combinedAttributes().get("actual_range").toString(),
            oldMinLon + ", " + oldMaxLon, "actual_range");
        Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("geospatial_lon_min"),
            oldMinLon, "geospatial_lon_min");
        Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("geospatial_lon_max"),
            oldMaxLon, "geospatial_lon_max");

        //should be original data
        tName = eddTable.makeNewFileForDapQuery(null, null, dataQuery, tDir, 
            eddTable.className() + "_qr_2d", ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        Test.ensureEqual(results, originalExpectedData, "\nresults=\n" + results);

    }

    /**
     * This tests the new handling of timestamp destinationMax (2015-02-24):
     * destinationMax is always set (even if recent) and EDDTable.parseUserDapQuery
     * treats timestamp destinationMax as NaN if recent (in last 2 days).
     *
     * @throws Throwable if trouble
     */
    public static void testNewTime() throws Throwable {

        String2.pressEnterToContinue(
            "\n****************** EDDTableFromNcFiles.testNewTime() *****************\n" +
            "Copy /u00/data/points/ndbcMet/NDBC_46088_met.nc from coastwatch to this computer."); 

        EDDTableFromNcFiles eddTable = (EDDTableFromNcFiles)oneFromDatasetsXml(null, "cwwcNDBCMet"); 
        EDV timeEdv = eddTable.dataVariables()[eddTable.timeIndex];
        String dataDir = eddTable.fileDir;
        String tDir = EDStatic.fullTestCacheDirectory;
        String tName, results, expected;

        double destMinD = timeEdv.destinationMin();
        double destMaxD = timeEdv.destinationMax();
        String destMinS = Calendar2.safeEpochSecondsToIsoStringTZ(destMinD, "");
        String destMaxS = Calendar2.safeEpochSecondsToIsoStringTZ(destMaxD, "");

        Test.ensureTrue(!Double.isNaN(destMinD), "edvTime.destinationMin");
        Test.ensureTrue(!Double.isNaN(destMaxD), "edvTime.destinationMax");
        Test.ensureEqual(timeEdv.combinedAttributes().get("actual_range").toString(),
            destMinD + ", " + destMaxD, "actual_range");
        Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("time_coverage_start"),
            destMinS, "time_coverage_start");
        Test.ensureEqual(eddTable.combinedGlobalAttributes().getString("time_coverage_end"),
            destMaxS, "time_coverage_end");

        //request data where time>=destMaxD
        tName = eddTable.makeNewFileForDapQuery(null, null, 
            "station,time,atmp&time>=" + destMaxD, 
            tDir, eddTable.className() + "_newTime1", ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"station,time,atmp\n" +
",UTC,degree_C\n" +
"46088," + destMaxS + ",";
            Test.ensureEqual(results.substring(0, expected.length()), expected, 
                "\nmaxTime=" + destMaxS + " results=\n" + results);

        String2.log("\nEDDTableFromNcFiles.testNewTime() finished successfully. maxTime=" + 
            destMaxS + "\n");

    }



    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test(boolean doAllGraphicsTests) throws Throwable {
/* */
        test1D(false); //deleteCachedDatasetInfo
        test2D(true); 
        test3D(false);
        test4D(false);
        testGlobec();
        testKml();
        testGraphics(doAllGraphicsTests);
        testLegend();
        testTimeAxis();
        testModTime();
        testNetcdf();  //!!!!! EEEK: opendap bytes are 0 - 255, Java/ERDDAP bytes are -128 to 127 !
        testGlobecBirds();
        testLatLon();
        testId();
        testDistinct();
        testOrderBy();
        testOrderByMax();
        testOrderByMin();
        testOrderByMinMax();
        testStationLonLat();
        testGlobal();  //tests global: metadata to data conversion
        testGenerateDatasetsXml();
        testGenerateDatasetsXml2();
        testErdGtsppBest("erdGtsppBestNc");
        testErdGtsppBest("erdGtsppBest");
        testTransparentPng();
        testSpeed(-1);  //15=.odv 25=png
        testManyYears();
        testCAMarCat();
        testNcCFPoint();
        testNcCF1a();
        testNcCFMA1a();
        testNcCF1b();
        testNcCFMA1b(); 
        testNcCF2a();
        testNcCFMA2a();
        testNcCF2b();
        testNcCFMA2b();
        testSpeedDAF();
        testSpeedMAG();
        testSpeedSubset();
        testEqualsNaN();
        testAltitude();
        //testTableWithAltitude(); !!!2013-12-27 DATASET GONE! NO SUITABLE REPLACEMENT  
        testTableWithDepth();
        testMV();     
//testBigRequest(); //very slow -- just run this occasionally
        testPmelTaoAirt();
        testNow();
        testMinMaxConstraints();
        testTimePrecisionMillis();
        testSimpleTestNcTable();
        testSimpleTestNc2Table();
        testUpdate();
        testQuickRestart();
        testNewTime();
        /* */

        //not usually run
        //test24Hours();  //requires special set up

        //NOT FINISHED
        //testReadPngInfo();  //needs work
    }
}

