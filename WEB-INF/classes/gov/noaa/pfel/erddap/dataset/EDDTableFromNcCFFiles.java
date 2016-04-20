/* 
 * EDDTableFromNcCFFiles Copyright 2011, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.PrimitiveArray;
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
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;


/** 
 * This class represents a table of data from a collection of FeatureDatasets
 * using CF Discrete Sampling Geometries (was Point Observation Conventions), 
 * http://cfconventions.org/Data/cf-conventions/cf-conventions-1.6/build/cf-conventions.html#discrete-sampling-geometries
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2011-01-27
 */
public class EDDTableFromNcCFFiles extends EDDTableFromFiles { 


    /** 
     * The constructor just calls the super constructor. 
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
     * @param tFileDir the base URL or file directory. 
     *    See http://www.unidata.ucar.edu/software/netcdf-java/v4.2/javadoc/index.html
     *    FeatureDatasetFactoryManager open().
     *    This may be a
     *    <ul>
     *    <li> local file (.nc or compatible)
     *    <li> thredds catalog#dataset (with a thredds: prefix)
     *    <li> cdmremote dataset (with an cdmremote: prefix)
     *    <li> collection dataset (with a collection: prefix)
     *    </ul>
     * @param tSortedColumnSourceName can't be for a char/String variable
     *   because NcHelper binary searches are currently set up for numeric vars only.
     *
     */
    public EDDTableFromNcCFFiles(
        String tDatasetID, String tAccessibleTo, String tGraphsAccessibleTo,
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
        boolean tSourceNeedsExpandedFP_EQ, 
        boolean tFileTableInMemory, boolean tAccessibleViaFiles) 
        throws Throwable {

        super("EDDTableFromNcCFFiles",  
            tDatasetID, tAccessibleTo, tGraphsAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File, 
            tSosOfferingPrefix,
            tDefaultDataQuery, tDefaultGraphQuery,
            tAddGlobalAttributes, 
            tDataVariables, tReloadEveryNMinutes, tUpdateEveryNMillis,
            tFileDir, tFileNameRegex, tRecursive, tPathRegex, tMetadataFrom,
            tCharset, tColumnNamesRow, tFirstDataRow,  //irrelevant
            tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
            tSortedColumnSourceName, //irrelevant
            tSortFilesBySourceNames,
            tSourceNeedsExpandedFP_EQ, tFileTableInMemory, tAccessibleViaFiles);

    }

    /**
     * This gets source data from one file.
     * See documentation in EDDTableFromFiles.
     * 
     * <p>For this class, sortedColumn is always time. See constructor.
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
        
        //get the data from the source file
        Table table = new Table();
        table.readNcCF(fileDir + fileName, sourceDataNames,
            sourceConVars, sourceConOps, sourceConValues);
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
     * @param tReloadEveryNMinutes  e.g., 10080 for weekly
     * @param tPreExtractRegex       part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tPostExtractRegex      part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tExtractRegex          part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tColumnNameForExtract  part of info for extracting e.g., stationName from file name. Set to "" if not needed.
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
        int tReloadEveryNMinutes, 
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex,
        String tColumnNameForExtract, 
        String tSortFilesBySourceNames, 
        String tInfoUrl, String tInstitution, String tSummary, String tTitle,
        Attributes externalAddGlobalAttributes) throws Throwable {

        String2.log("EDDTableFromNcCFFiles.generateDatasetsXml" +
            "\n  sampleFileName=" + sampleFileName);
        if (!String2.isSomething(tFileDir))
            throw new IllegalArgumentException("fileDir wasn't specified.");
        tFileDir = File2.addSlash(tFileDir); //ensure it has trailing slash
        if (tReloadEveryNMinutes <= 0 || tReloadEveryNMinutes == Integer.MAX_VALUE)
            tReloadEveryNMinutes = 1440; //1440 works well with suggestedUpdateEveryNMillis
        if (!String2.isSomething(sampleFileName)) 
            String2.log("Found/using sampleFileName=" +
                (sampleFileName = FileVisitorDNLS.getSampleFileName(
                    tFileDir, tFileNameRegex, true, ".*"))); //recursive, pathRegex

        String2.log("Let's see if netcdf-java can tell us the structure of the sample file:");
        String2.log(NcHelper.dumpString(sampleFileName, false));

        //*** basically, make a table to hold the sourceAttributes 
        //and a parallel table to hold the addAttributes
        Table dataSourceTable = new Table();
        Table dataAddTable = new Table();
        dataSourceTable.readNcCF(sampleFileName, null, null, null, null);
        for (int c = 0; c < dataSourceTable.nColumns(); c++) {
            String colName = dataSourceTable.getColumnName(c);
            Attributes sourceAtts = dataSourceTable.columnAttributes(c);
            dataAddTable.addColumn(c, colName,
                dataSourceTable.getColumn(c),
                makeReadyToUseAddVariableAttributesForDatasetsXml(
                    dataSourceTable.globalAttributes(), sourceAtts, colName, 
                    true, true)); //addColorBarMinMax, tryToFindLLAT
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
        //after dataVariables known, add global attributes in the dataAddTable
        dataAddTable.globalAttributes().set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(), 
                dataSourceTable.globalAttributes().getString("cdm_data_type"),
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
            tSortFilesBySourceNames = tColumnNameForExtract.trim();
        sb.append(
            directionsForGenerateDatasetsXml() +
            "-->\n\n" +
            "<dataset type=\"EDDTableFromNcCFFiles\" datasetID=\"" + 
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
            "    <sortFilesBySourceNames>" + XML.encodeAsXML(tSortFilesBySourceNames) + "</sortFilesBySourceNames>\n" +
            "    <fileTableInMemory>false</fileTableInMemory>\n" +
            "    <accessibleViaFiles>false</accessibleViaFiles>\n");
        sb.append(writeAttsForDatasetsXml(false, dataSourceTable.globalAttributes(), "    "));
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
        //debugMode = true;

        try {
            //public static String generateDatasetsXml(
            //    String tFileDir, String tFileNameRegex, String sampleFileName, 
            //    int tReloadEveryNMinutes,
            //    String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex,
            //    String tColumnNameForExtract, 
            //    String tSortFilesBySourceNames, 
            //    String tInfoUrl, String tInstitution, String tSummary, String tTitle,
            //    Attributes externalAddGlobalAttributes) throws Throwable {

            String results = generateDatasetsXml(
                EDStatic.unitTestDataDir + "nccf/", "ncCF1b\\.nc",
                EDStatic.unitTestDataDir + "nccf/ncCF1b.nc",
                1440,
                "", "", "", "", //just for test purposes; station is already a column in the file
                "line_station time", 
                "", "", "", "", null) + "\n";

            //GenerateDatasetsXml
            String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
                "EDDTableFromNcCFFiles",
                EDStatic.unitTestDataDir + "nccf/", "ncCF1b\\.nc",
                EDStatic.unitTestDataDir + "nccf/ncCF1b.nc",
                "1440",
                "", "", "", "", //just for test purposes; station is already a column in the file
                "line_station time", 
                "", "", "", "", ""},
                false); //doIt loop?
            Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

String expected = 
directionsForGenerateDatasetsXml() +
"-->\n" +
"\n" +
"<dataset type=\"EDDTableFromNcCFFiles\" datasetID=\"nccf_8867_6a37_8e8f\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>" + EDStatic.unitTestDataDir + "nccf/</fileDir>\n" +
"    <fileNameRegex>ncCF1b\\.nc</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <preExtractRegex></preExtractRegex>\n" +
"    <postExtractRegex></postExtractRegex>\n" +
"    <extractRegex></extractRegex>\n" +
"    <columnNameForExtract></columnNameForExtract>\n" +
"    <sortFilesBySourceNames>line_station time</sortFilesBySourceNames>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <accessibleViaFiles>false</accessibleViaFiles>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"cdm_data_type\">TimeSeries</att>\n" +
"        <att name=\"cdm_timeseries_variables\">line_station</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, Unidata Dataset Discovery v1.0</att>\n" +
"        <att name=\"Easternmost_Easting\" type=\"float\">-123.49333</att>\n" +
"        <att name=\"featureType\">TimeSeries</att>\n" +
"        <att name=\"geospatial_lat_max\" type=\"float\">33.388332</att>\n" +
"        <att name=\"geospatial_lat_min\" type=\"float\">32.245</att>\n" +
"        <att name=\"geospatial_lat_units\">degrees_north</att>\n" +
"        <att name=\"geospatial_lon_max\" type=\"float\">-123.49333</att>\n" +
"        <att name=\"geospatial_lon_min\" type=\"float\">-124.32333</att>\n" +
"        <att name=\"geospatial_lon_units\">degrees_east</att>\n" +
"        <att name=\"geospatial_vertical_max\" type=\"float\">-211.5</att>\n" +
"        <att name=\"geospatial_vertical_min\" type=\"float\">-216.7</att>\n" +
"        <att name=\"geospatial_vertical_positive\">up</att>\n" +
"        <att name=\"geospatial_vertical_units\">m</att>\n" +
"        <att name=\"history\">Data originally from CalCOFI project.\n" +
"At ERD, Roy Mendelssohn processed the data into .nc files.\n" +
"2010-12-28 At ERD, Lynn DeWitt made the files available to Bob Simons.\n" +
"2010-12-31 Bob Simons reprocessed the files with Projects.calcofiBio().\n" +
"2012-08-02T16:13:53Z (local files)\n" +
"2012-08-02T16:13:53Z http://127.0.0.1:8080/cwexperimental/tabledap/erdCalcofiBio.ncCF?line_station,longitude,latitude,altitude,time,obsScientific,obsValue,obsUnits&amp;station=100.0&amp;time&gt;=2004-11-12T00:00:00Z&amp;time&lt;=2004-11-19T08:32:00Z&amp;obsUnits=&#37;22number&#37;20of&#37;20larvae&#37;22&amp;orderBy&#37;28&#37;22line_station,time,obsScientific&#37;22&#37;29</att>\n" +
"        <att name=\"id\">ncCF1b</att>\n" +
"        <att name=\"infoUrl\">http://www.calcofi.org/newhome/publications/Atlases/atlases.htm</att>\n" +
"        <att name=\"institution\">CalCOFI</att>\n" +
"        <att name=\"keywords\">Biosphere &gt; Aquatic Ecosystems &gt; Coastal Habitat,\n" +
"Biosphere &gt; Aquatic Ecosystems &gt; Marine Habitat,\n" +
"Biological Classification &gt; Animals/Vertebrates &gt; Fish,\n" +
"Oceans &gt; Aquatic Sciences &gt; Fisheries,\n" +
"1984-2004, altitude, atmosphere, biology, calcofi, code, common, count, cruise, fish, height, identifier, larvae, line, name, number, observed, occupancy, order, scientific, ship, start, station, time, tow, units, value</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.</att>\n" +
"        <att name=\"Metadata_Conventions\">COARDS, CF-1.6, Unidata Dataset Discovery v1.0</att>\n" +
"        <att name=\"Northernmost_Northing\" type=\"float\">33.388332</att>\n" +
"        <att name=\"sourceUrl\">(local files)</att>\n" +
"        <att name=\"Southernmost_Northing\" type=\"float\">32.245</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF-12</att>\n" +
"        <att name=\"subsetVariables\">line_station</att>\n" +
"        <att name=\"summary\">This is the CalCOFI distributional atlas of fish larvae. Routine oceanographic sampling within the California Current System has occurred under the auspices of the California Cooperative Oceanic Fisheries Investigations (CalCOFI) since 1949, providing one of the longest existing time-series of the physics, chemistry and biology of a dynamic oceanic regime.</att>\n" +
"        <att name=\"time_coverage_end\">2004-11-16T21:20:00Z</att>\n" +
"        <att name=\"time_coverage_start\">2004-11-12T16:26:00Z</att>\n" +
"        <att name=\"title\">CalCOFI Fish Larvae Count, 1984-2004</att>\n" +
"        <att name=\"Westernmost_Easting\" type=\"float\">-124.32333</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"creator_name\">CalCOFI</att>\n" +
"        <att name=\"creator_url\">http://www.calcofi.org/newhome/publications/Atlases/atlases.htm</att>\n" +
"        <att name=\"keywords\">1984-2004, altitude, animals, aquatic, atmosphere,\n" +
"Atmosphere &gt; Altitude &gt; Station Height,\n" +
"biological,\n" +
"Biological Classification &gt; Animals/Vertebrates &gt; Fish,\n" +
"biology, biosphere,\n" +
"Biosphere &gt; Aquatic Ecosystems &gt; Coastal Habitat,\n" +
"Biosphere &gt; Aquatic Ecosystems &gt; Marine Habitat,\n" +
"calcofi, california, classification, coastal, code, common, cooperative, count, cruise, data, ecosystems, fish, fisheries, habitat, height, identifier, investigations, larvae, latitude, line, line_station, longitude, marine, name, number, observed, obsScientific, obsUnits, obsValue, occupancy, ocean, oceanic, oceans,\n" +
"Oceans &gt; Aquatic Sciences &gt; Fisheries,\n" +
"order, sciences, scientific, ship, start, station, time, tow, units, value, vertebrates</att>\n" +
"        <att name=\"Metadata_Conventions\">null</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v29</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>line_station</sourceName>\n" +
"        <destinationName>line_station</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"cf_role\">timeseries_id</att>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">CalCOFI Line + Station</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>longitude</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Lon</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-124.32333 -123.49333</att>\n" +
"            <att name=\"axis\">X</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>latitude</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Lat</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">32.245 33.388332</att>\n" +
"            <att name=\"axis\">Y</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>altitude</sourceName>\n" +
"        <destinationName>altitude</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Height</att>\n" +
"            <att name=\"_CoordinateZisPositive\">up</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-216.7 -211.5</att>\n" +
"            <att name=\"axis\">Z</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Altitude at Start of Tow</att>\n" +
"            <att name=\"positive\">up</att>\n" +
"            <att name=\"standard_name\">altitude</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">-210.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-218.0</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Time</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">1.10027676E9 1.10064E9</att>\n" +
"            <att name=\"axis\">T</att>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Time</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"time_origin\">01-JAN-1970 00:00:00</att>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">1.1007E9</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">1.1002E9</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>obsScientific</sourceName>\n" +
"        <destinationName>obsScientific</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"coordinates\">time latitude longitude altitude</att>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Observed (Scientific Name)</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"coordinates\">null</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>obsValue</sourceName>\n" +
"        <destinationName>obsValue</destinationName>\n" +
"        <dataType>int</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"actual_range\" type=\"intList\">1 22</att>\n" +
"            <att name=\"coordinates\">time latitude longitude altitude</att>\n" +
"            <att name=\"ioos_category\">Biology</att>\n" +
"            <att name=\"long_name\">Observed Value</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">25.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"coordinates\">null</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>obsUnits</sourceName>\n" +
"        <destinationName>obsUnits</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"coordinates\">time latitude longitude altitude</att>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Observed Units</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"coordinates\">null</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";
            Test.ensureEqual(results, expected, "results=\n" + results);
            //Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), 
            //    expected, "");

            EDD edd = oneFromXmlFragment(null, results);
            Test.ensureEqual(edd.datasetID(), "nccf_8867_6a37_8e8f", "");
            Test.ensureEqual(edd.title(), "CalCOFI Fish Larvae Count, 1984-2004", "");
            Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
                "line_station, longitude, latitude, altitude, time, obsScientific, obsValue, obsUnits", 
                "");

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nError using generateDatasetsXml."); 
        }

    }

    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test1(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDTableFromNcCFFiles.test1() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);

        String id = "testNcCF1b";
        if (deleteCachedDatasetInfo) 
            deleteCachedDatasetInfo(id);

        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, id); 


        //.csv    for one lat,lon,time
        userDapQuery = "";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, eddTable.className() + "_test1a", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"line_station,longitude,latitude,altitude,time,obsScientific,obsValue,obsUnits\n" +
",degrees_east,degrees_north,m,UTC,,,\n" +
"076.7_100,-124.32333,33.388332,-214.1,2004-11-16T21:20:00Z,Argyropelecus sladeni,2,number of larvae\n" +
"076.7_100,-124.32333,33.388332,-214.1,2004-11-16T21:20:00Z,Chauliodus macouni,3,number of larvae\n" +
"076.7_100,-124.32333,33.388332,-214.1,2004-11-16T21:20:00Z,Danaphos oculatus,4,number of larvae\n" +
"076.7_100,-124.32333,33.388332,-214.1,2004-11-16T21:20:00Z,Diogenichthys atlanticus,3,number of larvae\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);


        //.csv    only outer vars
        userDapQuery = "line_station";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery,
            EDStatic.fullTestCacheDirectory, eddTable.className() + "_test1b", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"line_station\n" +
"\n" +
"076.7_100\n" +
"080_100\n" +
"083.3_100\n"; //4 row with all mv was removed
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


    }

    /**
     * This tests for a bug Kevin O'Brien reported.
     *
     * @throws Throwable if trouble
     */
    public static void testKevin20130109() throws Throwable {
        String2.log("\n****************** EDDTableFromNcCFFiles.testKevin20130109() *****************\n");
        testVerboseOn();
        boolean oDebugMode = debugMode; debugMode = true;
        boolean oTableDebug = Table.debugMode; Table.debugMode = true;

        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);

        String id = "testKevin20130109";
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, id); 
        
        //test time <    first time is 2011-02-15T00:00:00Z
        userDapQuery = "traj,obs,time,longitude,latitude,temp,ve,vn&traj<26.5&time<2011-02-15T00:05";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery,
            EDStatic.fullTestCacheDirectory, eddTable.className() + "_test1Kevin20130109a", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"traj,obs,time,longitude,latitude,temp,ve,vn\n" +
",,UTC,degrees_east,degrees_north,Deg C,cm/s,cm/s\n" +
"1.0,1.0,2011-02-15T00:00:00Z,-111.344,-38.71,18.508,-14.618,17.793\n" +
"22.0,7387.0,2011-02-15T00:00:00Z,91.875,-54.314,3.018,64.135,1.534\n" +
"26.0,9139.0,2011-02-15T00:00:00Z,168.892,-48.516,11.381,24.49,4.884\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test time >    last time is 2011-09-30T18
        userDapQuery = "traj,obs,time,longitude,latitude,temp,ve,vn&traj<6&time>=2011-09-30T17:50";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery,
            EDStatic.fullTestCacheDirectory, eddTable.className() + "_test1Kevin20130109a", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"traj,obs,time,longitude,latitude,temp,ve,vn\n" +
",,UTC,degrees_east,degrees_north,Deg C,cm/s,cm/s\n" +
"1.0,912.0,2011-09-30T18:00:00Z,-91.252,-33.43,15.28,NaN,NaN\n" +
"2.0,1352.0,2011-09-30T18:00:00Z,145.838,38.44,22.725,NaN,NaN\n" +
"3.0,1794.0,2011-09-30T18:00:00Z,156.895,39.877,21.517,NaN,NaN\n" +
"4.0,2233.0,2011-09-30T18:00:00Z,150.312,34.38,26.658,-78.272,41.257\n" +
"5.0,2676.0,2011-09-30T18:00:00Z,162.9,36.15,26.129,-4.85,15.724\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        debugMode = oDebugMode;
        Table.debugMode = oTableDebug;

    }

    /**
     * This tests att with no name throws error.
     *
     * @throws Throwable if trouble
     */
    public static void testNoAttName() throws Throwable {
        String2.log("\n****************** EDDTableFromNcCFFiles.testNoAttName() *****************\n");

        try {
            EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, "testNoAttName1"); 
            throw new SimpleException("shouldn't get here");
        } catch (Throwable t) {
            String msg = t.toString();
            Test.ensureLinesMatch(msg, 
"java.lang.RuntimeException: datasets.xml error on line #\\d{1,7}: An <att> tag doesn't have a \"name\" attribute.", 
                "");
        }

        try {
            EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, "testNoAttName1"); 
            throw new SimpleException("shouldn't get here");
        } catch (Throwable t) {
            String msg = t.toString();
            Test.ensureLinesMatch(msg, 
"java.lang.RuntimeException: datasets.xml error on line #\\d{1,7}: An <att> tag doesn't have a \"name\" attribute.", 
                "");
        }

    }


    /**
     * This tests sos code and global attribute with name=null.
     *
     * @throws Throwable if trouble
     */
    public static void testBridger() throws Throwable {
        String2.log("\n****************** EDDTableFromNcCFFiles.testBridger() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
        Table table;

        table = new Table();
        table.readNcCF("c:/data/bridger/B01.accelerometer.historical.nc", null, null, null, null);
        Test.ensureSomethingUtf8(table.globalAttributes(), "historical global attributes");

        table = new Table();
        table.readNcCF("c:/data/bridger/B01.accelerometer.realtime.nc", null, null, null, null);
        Test.ensureSomethingUtf8(table.globalAttributes(), "realtime global attributes");

        String id = "UMaineAccB01";
        deleteCachedDatasetInfo(id);
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, id); 

        try {
            //.dds    
            tName = eddTable.makeNewFileForDapQuery(null, null, "", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_bridger", ".dds"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
    "Dataset {\n" +
    "  Sequence {\n" +
    "    String station;\n" +
    "    Float32 longitude;\n" +
    "    Float32 latitude;\n" +
    "    Float32 depth;\n" +
    "    Float64 time;\n" +
    "    Float64 time_created;\n" +
    "    Float64 time_modified;\n" +
    "    Float32 significant_wave_height;\n" +
    "    Byte significant_wave_height_qc;\n" +
    "    Float32 dominant_wave_period;\n" +
    "    Byte dominant_wave_period_qc;\n" +
    "  } s;\n" +
    "} s;\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

            //.das    
            tName = eddTable.makeNewFileForDapQuery(null, null, "", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_bridger", ".das"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
    "Attributes \\{\n" +
    " s \\{\n" +
    "  station \\{\n" +
    "    String cf_role \"timeseries_id\";\n" +
    "    String ioos_category \"Unknown\";\n" +
    "    String long_name \"B01\";\n" +
    "    String name \"B01\";\n" +
    "    String short_name \"B01\";\n" +
    "    String standard_name \"station_name\";\n" +
    "  \\}\n" +
    "  longitude \\{\n" +
    "    String _CoordinateAxisType \"Lon\";\n" +
    "    Float32 actual_range -70.42779, -70.42755;\n" +
    "    String axis \"X\";\n" +
    "    Float64 colorBarMaximum 180.0;\n" +
    "    Float64 colorBarMinimum -180.0;\n" +
    "    String ioos_category \"Location\";\n" +
    "    String long_name \"Longitude\";\n" +
    "    String standard_name \"longitude\";\n" +
    "    String units \"degrees_east\";\n" +
    "  \\}\n" +
    "  latitude \\{\n" +
    "    String _CoordinateAxisType \"Lat\";\n" +
    "    Float32 actual_range 43.18019, 43.18044;\n" +
    "    String axis \"Y\";\n" +
    "    Float64 colorBarMaximum 90.0;\n" +
    "    Float64 colorBarMinimum -90.0;\n" +
    "    String ioos_category \"Location\";\n" +
    "    String long_name \"Latitude\";\n" +
    "    String standard_name \"latitude\";\n" +
    "    String units \"degrees_north\";\n" +
    "  \\}\n" +
    "  depth \\{\n" +
    "    String _CoordinateAxisType \"Height\";\n" +
    "    String _CoordinateZisPositive \"down\";\n" +
    "    Float32 actual_range 0.0, 0.0;\n" +
    "    String axis \"Z\";\n" +
    "    Float64 colorBarMaximum 8000.0;\n" +
    "    Float64 colorBarMinimum 0.0;\n" +
    "    String colorBarPalette \"OceanDepth\";\n" +
    "    String ioos_category \"Location\";\n" +
    "    String long_name \"Depth\";\n" +
    "    String positive \"down\";\n" +
    "    String standard_name \"depth\";\n" +
    "    String units \"m\";\n" +
    "  \\}\n" +
    "  time \\{\n" +
    "    Int32 _ChunkSizes 1;\n" +
    "    String _CoordinateAxisType \"Time\";\n" +
    "    Float64 actual_range 1.0173492e\\+9, 1.3907502000000134e\\+9;\n" +
    "    String axis \"T\";\n" +
    "    String calendar \"gregorian\";\n" +
    "    String ioos_category \"Time\";\n" +
    "    String long_name \"Time\";\n" +
    "    String standard_name \"time\";\n" +
    "    String time_origin \"01-JAN-1970 00:00:00\";\n" +
    "    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
    "  \\}\n" +
    "  time_created \\{\n" +
    "    Float64 _FillValue NaN;\n" +
    "    Float64 actual_range 1.3717448871222715e\\+9, 1.3907507452187824e\\+9;\n" +
    "    String coordinates \"time lon lat depth\";\n" +
    "    String ioos_category \"Time\";\n" +
    "    String long_name \"Time Record Created\";\n" +
    "    String short_name \"time_cr\";\n" +
    "    String standard_name \"time\";\n" +
    "    String time_origin \"01-JAN-1970 00:00:00\";\n" +
    "    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
    "    Float64 valid_range 0.0, 99999.0;\n" +
    "  \\}\n" +
    "  time_modified \\{\n" +
    "    Float64 _FillValue NaN;\n" +
    "    Float64 actual_range 1.3717448871222715e\\+9, 1.3907507452187824e\\+9;\n" +
    "    String coordinates \"time lon lat depth\";\n" +
    "    String ioos_category \"Time\";\n" +
    "    String long_name \"Time Record Last Modified\";\n" +
    "    String short_name \"time_mod\";\n" +
    "    String standard_name \"time\";\n" +
    "    String time_origin \"01-JAN-1970 00:00:00\";\n" +
    "    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
    "    Float64 valid_range 0.0, 99999.0;\n" +
    "  \\}\n" +
    "  significant_wave_height \\{\n" +
    "    Int32 _ChunkSizes 1;\n" +
    "    Float32 _FillValue -999.0;\n" +
    "    Float64 accuracy 0.5;\n" +
    "    Float32 actual_range 0.009102137, 9.613417;\n" +
    "    String ancillary_variables \"significant_wave_height_qc\";\n" +
    "    Float64 colorBarMaximum 10.0;\n" +
    "    Float64 colorBarMinimum 0.0;\n" +
    "    String coordinates \"time lon lat depth\";\n" +
    "    Int32 epic_code 4061;\n" +
    "    String ioos_category \"Surface Waves\";\n" +
    "    Float64 is_dead 0.0;\n" +
    "    String long_name \"Significant Wave Height\";\n" +
    "    String measurement_type \"Computed\";\n" +
    "    Float64 precision 0.1;\n" +
    "    String short_name \"SWH\";\n" +
    "    String standard_name \"significant_height_of_wind_and_swell_waves\";\n" +
    "    String units \"m\";\n" +
    "    Float32 valid_range 0.0, 10.0;\n" +
    "  \\}\n" +
    "  significant_wave_height_qc \\{\n" +
    "    Int32 _ChunkSizes 1;\n" +
    "    Byte _FillValue -128;\n" +
    "    Byte actual_range 0, 99;\n" +
    "    Float64 colorBarMaximum 128.0;\n" +
    "    Float64 colorBarMinimum 0.0;\n" +
    "    String coordinates \"time lon lat depth\";\n" +
    "    String flag_meanings \"quality_good out_of_range sensor_nonfunctional algorithm_failure_no_infl_pt\";\n" +
    "    Byte flag_values 0, 1, 2, 3;\n" +
    "    String intent \"data_quality\";\n" +
    "    String ioos_category \"Quality\";\n" +
    "    String long_name \"Significant Wave Height Quality Control\";\n" +
    "    String short_name \"SWHQC\";\n" +
    "    String standard_name \"significant_height_of_wind_and_swell_waves data_quality\";\n" +
    "    String units \"1\";\n" +
    "    Byte valid_range -127, 127;\n" +
    "  \\}\n" +
    "  dominant_wave_period \\{\n" +
    "    Int32 _ChunkSizes 1;\n" +
    "    Float32 _FillValue -999.0;\n" +
    "    Float64 accuracy 2.0;\n" +
    "    Float32 actual_range 1.032258, 16.0;\n" +
    "    String ancillary_variables \"dominant_wave_period_qc\";\n" +
    "    Float64 colorBarMaximum 40.0;\n" +
    "    Float64 colorBarMinimum 0.0;\n" +
    "    String coordinates \"time lon lat depth\";\n" +
    "    Int32 epic_code 4063;\n" +
    "    String ioos_category \"Surface Waves\";\n" +
    "    Float64 is_dead 0.0;\n" +
    "    String long_name \"Dominant Wave Period\";\n" +
    "    String measurement_type \"Computed\";\n" +
    "    Float64 precision 1.0;\n" +
    "    Float64 sensor_depth 0.0;\n" +
    "    String short_name \"DWP\";\n" +
    "    String standard_name \"period\";\n" +
    "    String units \"s\";\n" +
    "    Float32 valid_range 0.0, 32.0;\n" +
    "  \\}\n" +
    "  dominant_wave_period_qc \\{\n" +
    "    Int32 _ChunkSizes 1;\n" +
    "    Byte _FillValue -128;\n" +
    "    Byte actual_range 0, 99;\n" +
    "    Float64 colorBarMaximum 128.0;\n" +
    "    Float64 colorBarMinimum 0.0;\n" +
    "    String coordinates \"time lon lat depth\";\n" +
    "    String flag_meanings \"quality_good out_of_range sensor_nonfunctional algorithm_failure_no_infl_pt\";\n" +
    "    Byte flag_values 0, 1, 2, 3;\n" +
    "    String intent \"data_quality\";\n" +
    "    String ioos_category \"Quality\";\n" +
    "    String long_name \"Dominant Wave Period Quality\";\n" +
    "    String short_name \"DWPQ\";\n" +
    "    String standard_name \"period data_quality\";\n" +
    "    String units \"1\";\n" +
    "    Byte valid_range -127, 127;\n" +
    "  \\}\n" +
    " \\}\n" +
    "  NC_GLOBAL \\{\n" +
    "    String accelerometer_serial_number \"SUMAC0902A01107\";\n" +
    "    String algorithm_ids \"Waves_SWH_DWP_1.12:  12-Jun-2013 15:15:53\";\n" +
    "    Float64 averaging_period 17.07;\n" +
    "    String averaging_period_units \"Minutes\";\n" +
    "    Int32 breakout_id 7;\n" +
    "    String buffer_type \"accelerometer\";\n" +
    "    String cdm_data_type \"TimeSeries\";\n" +
    "    String cdm_timeseries_variables \"station\";\n" +
    "    String clock_time \"Center of period\";\n" +
    "    String contact \"nealp@maine.edu,ljm@umeoce.maine.edu,bfleming@umeoce.maine.edu\";\n" +
    "    String control_box_serial_number \"UMECB124\";\n" +
    "    String Conventions \"CF-1.6, COARDS, ACDD-1.3\";\n" +
    "    String creator_email \"nealp@maine.edu,ljm@umeoce.maine.edu,bfleming@umeoce.maine.edu\";\n" +
    "    String creator_name \"Neal Pettigrew\";\n" +
    "    String creator_url \"http://gyre.umeoce.maine.edu\";\n" +
    "    String depth_datum \"Sea Level\";\n" +
    "    Float64 Easternmost_Easting -70.42755;\n" +
    "    String featureType \"TimeSeries\";\n" +
    "    Float64 geospatial_lat_max 43.18044;\n" +
    "    Float64 geospatial_lat_min 43.18019;\n" +
    "    String geospatial_lat_units \"degrees_north\";\n" +
    "    Float64 geospatial_lon_max -70.42755;\n" +
    "    Float64 geospatial_lon_min -70.42779;\n" +
    "    String geospatial_lon_units \"degrees_east\";\n" +
    "    Float64 geospatial_vertical_max 0.0;\n" +
    "    Float64 geospatial_vertical_min 0.0;\n" +
    "    String geospatial_vertical_positive \"down\";\n" +
    "    String geospatial_vertical_units \"m\";\n" +
    "    String goes_platform_id \"044250DC\";\n" +
    "    String history \"2014-01-03 11:20:56:  Parameter dominant_wave_period marked as non-functional as of julian day 56660.395833 \\(2014-01-03 09:30:00\\)\n" +
    "2014-01-03 11:20:46:  Parameter significant_wave_height marked as non-functional as of julian day 56660.395833 \\(2014-01-03 09:30:00\\)\n" +
    "2013-06-25 11:57:07:  Modified \\[lon,lat\\] to \\[-70.427787,43.180192\\].\n" +
    "Thu Jun 20 16:50:01 2013: /usr/local/bin/ncrcat -d time,56463.65625,56464.00 B0125.accelerometer.realtime.nc B0125.accelerometer.realtime.nc.new\n" +
    "\n" +
    today + "T.{8}Z \\(local files\\)\n" +
    today + "T.{8}Z http://localhost:8080/cwexperimental/tabledap/UMaineAccB01.das\";\n" +
    "    String id \"B01\";\n" +
    "    String infoUrl \"http://gyre.umeoce.maine.edu/\";\n" +
    "    String institution \"Department of Physical Oceanography, School of Marine Sciences, University of Maine\";\n" +
    "    String institution_url \"http://gyre.umeoce.maine.edu\";\n" +
    "    Int32 instrument_number 0;\n" +
    "    String keywords \"accelerometer, b01, buoy, chemistry, chlorophyll, circulation, conductivity, control, currents, data, density, department, depth, dominant, dominant_wave_period data_quality, height, level, maine, marine, name, o2, ocean, oceanography, oceans,\n" +
    "Oceans > Ocean Chemistry > Chlorophyll,\n" +
    "Oceans > Ocean Chemistry > Oxygen,\n" +
    "Oceans > Ocean Circulation > Ocean Currents,\n" +
    "Oceans > Ocean Optics > Turbidity,\n" +
    "Oceans > Ocean Pressure > Sea Level Pressure,\n" +
    "Oceans > Ocean Temperature > Water Temperature,\n" +
    "Oceans > Ocean Waves > Significant Wave Height,\n" +
    "Oceans > Ocean Waves > Swells,\n" +
    "Oceans > Ocean Waves > Wave Period,\n" +
    "Oceans > Ocean Winds > Surface Winds,\n" +
    "Oceans > Salinity/Density > Conductivity,\n" +
    "Oceans > Salinity/Density > Density,\n" +
    "Oceans > Salinity/Density > Salinity,\n" +
    "optics, oxygen, period, physical, pressure, quality, salinity, school, sciences, sea, seawater, sensor, significant, significant_height_of_wind_and_swell_waves, significant_wave_height data_quality, station, station_name, surface, surface waves, swell, swells, temperature, time, turbidity, university, water, wave, waves, wind, winds\";\n" +
    "    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
    "    Float64 latitude 43.18019230109601;\n" +
    "    String license \"The data may be used and redistributed for free but is not intended\n" +
    "for legal use, since it may contain inaccuracies. Neither the data\n" +
    "Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
    "of their employees or contractors, makes any warranty, express or\n" +
    "implied, including warranties of merchantability and fitness for a\n" +
    "particular purpose, or assumes any legal liability for the accuracy,\n" +
    "completeness, or usefulness, of this information.\";\n" +
    "    String long_name \"B01\";\n" +
    "    Float64 longitude -70.42778651970477;\n" +
    "    Float64 magnetic_variation -16.3;\n" +
    "    String mooring_site_desc \"Western Maine Shelf\";\n" +
    "    String mooring_site_id \"B0125\";\n" +
    "    String mooring_type \"Slack\";\n" +
    "    String naming_authority \"edu.maine\";\n" +
    "    Int32 nco_openmp_thread_number 1;\n" +
    "    String ndbc_site_id \"44030\";\n" +
    "    Float64 Northernmost_Northing 43.18044;\n" +
    "    Int32 number_observations_per_hour 2;\n" +
    "    Int32 number_samples_per_observation 2048;\n" +
    "    String position_datum \"WGS 84\";\n" +
    "    String processing \"realtime\";\n" +
    "    String project \"NERACOOS\";\n" +
    "    String project_url \"http://gomoos.org\";\n" +
    "    String projejct \"NERACOOS\";\n" +
    "    String publisher \"Department of Physical Oceanography, School of Marine Sciences, University of Maine\";\n" +
    "    String publisher_email \"info@neracoos.org\";\n" +
    "    String publisher_name \"Northeastern Regional Association of Coastal and Ocean Observing Systems \\(NERACOOS\\)\";\n" +
    "    String publisher_phone \"\\(603\\) 319 1785\";\n" +
    "    String publisher_url \"http://www.neracoos.org/\";\n" +
    "    String references \"http://gyre.umeoce.maine.edu/data/gomoos/buoy/doc/buoy_system_doc/buoy_system/book1.html\";\n" +
    "    String short_name \"B01\";\n" +
    "    String source \"Ocean Data Acquisition Systems \\(ODAS\\) Buoy\";\n" +
    "    String sourceUrl \"\\(local files\\)\";\n" +
    "    Float64 Southernmost_Northing 43.18019;\n" +
    "    String standard_name_vocabulary \"CF-1.6\";\n" +
    "    String station_name \"B01\";\n" +
    "    String station_photo \"http://gyre.umeoce.maine.edu/gomoos/images/generic_buoy.png\";\n" +
    "    String station_type \"Surface Mooring\";\n" +
    "    String subsetVariables \"station\";\n" +
    "    String summary \"Ocean observation data from the Northeastern Regional Association of Coastal &amp; Ocean Observing Systems \\(NERACOOS\\). The NERACOOS region includes the northeast United States and Canadian Maritime provinces, as part of the United States Integrated Ocean Observing System \\(IOOS\\).  These data are served by Unidata's Thematic Realtime Environmental Distributed Data Services \\(THREDDS\\) Data Server \\(TDS\\) in a variety of interoperable data services and output formats.\";\n" +
    "    String time_coverage_end \"2014-01-26T15:30:00Z\";\n" +
    "    String time_coverage_start \"2002-03-28T21:00:00Z\";\n" +
    "    String time_zone \"UTC\";\n" +
    "    String title \"University of Maine, B01 Accelerometer Buoy Sensor\";\n" +
    "    String uscg_light_list_letter \"B\";\n" +
    "    String uscg_light_list_number \"113\";\n" +
    "    Int32 watch_circle_radius 45;\n" +
    "    Float64 water_depth 62.0;\n" +
    "    Float64 Westernmost_Easting -70.42779;\n" +
    "  \\}\n" +
    "\\}\n";
            Test.ensureLinesMatch(results, expected, "results=\n" + results);

            //.csv    for start time time
            //"    String time_coverage_start \"2002-03-28T21:00:00Z\";\n" +
            userDapQuery = "&time<=2002-03-28T22:00:00Z";
            tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_bridger1", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
    "station,longitude,latitude,depth,time,time_created,time_modified,significant_wave_height,significant_wave_height_qc,dominant_wave_period,dominant_wave_period_qc\n" +
    ",degrees_east,degrees_north,m,UTC,UTC,UTC,m,1,s,1\n" +
    "B01,-70.42755,43.18044,0.0,2002-03-28T21:00:00Z,,,2.605597,0,10.66667,0\n" +
    "B01,-70.42755,43.18044,0.0,2002-03-28T22:00:00Z,,,1.720958,0,10.66667,0\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t)); 
        }

        try {    
            //.csv    for end time
            //"    String time_coverage_end \"2014-01-26T15:30:00Z\";\n" +
            userDapQuery = "&time>=2014-01-26T15:00:00Z";
            tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_bridger2", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
    "station,longitude,latitude,depth,time,time_created,time_modified,significant_wave_height,significant_wave_height_qc,dominant_wave_period,dominant_wave_period_qc\n" +
    ",degrees_east,degrees_north,m,UTC,UTC,UTC,m,1,s,1\n" +
    "B01,-70.42779,43.18019,0.0,2014-01-26T15:00:00Z,2014-01-26T15:12:04Z,2014-01-26T15:12:04Z,1.3848689,0,4.0,0\n" +
    "B01,-70.42779,43.18019,0.0,2014-01-26T15:30:00Z,2014-01-26T15:39:05Z,2014-01-26T15:39:05Z,1.3212088,0,4.0,0\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

            //.csv    only outer vars
            userDapQuery = "station&distinct()";
            tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery,
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_bridger3", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
    "station\n" +
    "\n" +
    "B01\n"; 
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t)); 
        }

        String2.log("\n*** EDDTableFromNcCFFiles.testBridger() finished.");
    }

    
    /**
     * This tests sos code and global attribute with name=null.
     *
     * @throws Throwable if trouble
     */
    public static void testNcml() throws Throwable {
        String2.log("\n****************** EDDTableFromNcCFFiles.testNcml() *****************\n");
        testVerboseOn();
       String baseName = //don't make this public via GitHub
            "/data/medrano/CTZ-T500-MCT-NS5649-Z408-INS12-REC14";
        String results, expected;
        Table table;
        try {

            //ncdump the .nc file
            String2.log("Here's the ncdump of " + baseName + ".nc:");
            results = NcHelper.dumpString(baseName + ".nc", false);
            expected = 
"netcdf CTZ-T500-MCT-NS5649-Z408-INS12-REC14.nc {\n" +
"  dimensions:\n" +
"    time = UNLIMITED;   // (101 currently)\n" +
"    one = 1;\n" +
"    ni_Srec = 93;\n" +
"    lat = 1;\n" +
"    lon = 1;\n" +
"  variables:\n" +
"    double Cond(time=101);\n" +
"      :long_name = \"Conductividad\";\n" +
"      :units = \"S/m\";\n" +
"\n" +
"    double Pres(time=101);\n" +
"      :long_name = \"Presion\";\n" +
"      :units = \"dBar\";\n" +
"\n" +
"    double ProfDiseno(one=1);\n" +
"      :long_name = \"Profundidad de diseno\";\n" +
"      :units = \"m\";\n" +
"\n" +
"    double ProfEstimada(one=1);\n" +
"      :long_name = \"Profundidad estimada\";\n" +
"      :units = \"m\";\n" +
"\n" +
"    double Sal(time=101);\n" +
"      :long_name = \"Salinidad\";\n" +
"      :units = \"PSU\";\n" +
"\n" +
"    double Temp(time=101);\n" +
"      :long_name = \"Temperatura\";\n" +
"      :units = \"\uFFFDC\";\n" + //65533
"\n" +
"    double TiranteDiseno(one=1);\n" +
"      :long_name = \"Tirante diseno\";\n" +
"      :units = \"m\";\n" +
"\n" +
"    double TiranteEstimado(one=1);\n" +
"      :long_name = \"Tirante estimado\";\n" +
"      :units = \"m\";\n" +
"\n" +
"    double i_Salrec(ni_Srec=93);\n" +
"      :long_name = \"Indices salinidad reconstruida\";\n" +
"      :units = \"N/A\";\n" +
"\n" +
"    double jd(time=101);\n" +
"      :long_name = \"tiempo en dias Julianos\";\n" +
"      :units = \"days since 0000-01-01 00:00:00 \";\n" +
"      :time_origin = \"0000-01-01 00:00:00\";\n" +
"\n" +
"    double lat(lat=1);\n" +
"      :long_name = \"Latitud\";\n" +
"      :Units = \"degrees_north\";\n" +
"\n" +
"    double lon(lon=1);\n" +
"      :long_name = \"Longitud\";\n" +
"      :units = \"degrees_east\";\n" +
"\n" +
"    double var_pres(one=1);\n" +
"      :long_name = \"Bandera presion\";\n" +
"      :units = \"N/A\";\n" +
"\n" +
"  // global attributes:\n" +
"  :Title = \"Datos MCT para  el anclaje CTZ-T500 crucero CANEK 14\";\n" +
"  :Anclaje = \"CTZ-T500\";\n" +
"  :Equipo = \"MCT\";\n" +
"  :Numero_de_serie = \"5649\";\n" +
"  :Source_file = \"CTZ-T500-MCT-NS5649-Z408-INS12-REC14.mat\";\n" +
"  :Final_NC_file = \"CTZ-T500-MCT-NS5649-Z408-INS12-REC14.nc\";\n" +
"  :Creation_date = \"06-Aug-2014 12:22:59\";\n" +
"  :NCO = \"\\\"4.5.2\\\"\";\n" +
" data:\n" +
"}\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //ncdump the .ncml file
            String2.log("\nHere's the ncdump of " + baseName + ".ncml:");
            results = NcHelper.dumpString(baseName + ".ncml", false);
            expected = 
"<?xml version='1.0' encoding='UTF-8'?>\n" +
"<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'\n" +
"    location='file:/data/medrano/CTZ-T500-MCT-NS5649-Z408-INS12-REC14.ncml' >\n" +
"\n" +
"  <dimension name='time' length='101' />\n" +
"  <dimension name='station' length='1' />\n" +
"\n" +
"  <attribute name='Anclaje' value='CTZ-T500' />\n" +
"  <attribute name='Equipo' value='MCT' />\n" +
"  <attribute name='Numero_de_serie' value='5649' />\n" +
"  <attribute name='Source_file' value='CTZ-T500-MCT-NS5649-Z408-INS12-REC14.mat' />\n" +
"  <attribute name='Final_NC_file' value='CTZ-T500-MCT-NS5649-Z408-INS12-REC14.nc' />\n" +
"  <attribute name='NCO' value='&quot;4.5.2&quot;' />\n" +
"  <attribute name='Conventions' value='CF-1.6' />\n" +
"  <attribute name='featureType' value='timeSeries' />\n" +
"  <attribute name='standard_name_vocabulary' value='CF-1.6' />\n" +
"  <attribute name='title' value='CTZ-T500-MCT-NS5649-Z408-INS12-REC14' />\n" +
"  <attribute name='cdm_data_type' value='TimeSeries' />\n" +
"  <attribute name='cdm_timeseries_variables' value='station' />\n" +
"  <attribute name='date_created' value='06-Aug-2014 12:22:59' />\n" +
"\n" +
"  <variable name='station' type='int' shape='station' >\n" +
"    <attribute name='long_name' value='CTZ-T500-MCT-NS5649-Z408-INS12-REC14' />\n" +
"    <attribute name='cf_role' value='timeseries_id' />\n" +
"  </variable>\n" +
"  <variable name='time' type='double' shape='station time' >\n" +
"    <attribute name='long_name' value='tiempo en dias Julianos' />\n" +
"    <attribute name='units' value='days since 0000-01-01 00:00:00 ' />\n" +
"    <attribute name='time_origin' value='0000-01-01 00:00:00' />\n" +
"    <attribute name='standard_name' value='time' />\n" +
"    <attribute name='axis' value='T' />\n" +
"    <attribute name='calendar' value='julian' />\n" +
"  </variable>\n" +
"  <variable name='Cond' type='double' shape='station time' >\n" +
"    <attribute name='long_name' value='Conductividad' />\n" +
"    <attribute name='units' value='S/m' />\n" +
"    <attribute name='standard_name' value='sea_water_electrical_conductivity' />\n" +
"    <attribute name='coordinates' value='time latitude longitude z' />\n" +
"  </variable>\n" +
"  <variable name='Pres' type='double' shape='station time' >\n" +
"    <attribute name='long_name' value='Presion' />\n" +
"    <attribute name='units' value='dBar' />\n" +
"    <attribute name='standard_name' value='sea_water_pressure' />\n" +
"    <attribute name='coordinates' value='time latitude longitude z' />\n" +
"  </variable>\n" +
"  <variable name='Temp' type='double' shape='station time' >\n" +
"    <attribute name='long_name' value='Temperatura' />\n" +
"    <attribute name='units' value='degree_celsius' />\n" +
"    <attribute name='standard_name' value='sea_water_temperature' />\n" +
"    <attribute name='coordinates' value='time latitude longitude z' />\n" +
"  </variable>\n" +
"  <variable name='Sal' type='double' shape='station time' >\n" +
"    <attribute name='long_name' value='Salinidad' />\n" +
"    <attribute name='units' value='PSU' />\n" +
"    <attribute name='standard_name' value='sea_water_salinity' />\n" +
"    <attribute name='coordinates' value='time latitude longitude z' />\n" +
"  </variable>\n" +
"  <variable name='latitude' type='double' shape='station' >\n" +
"    <attribute name='long_name' value='Latitud' />\n" +
"    <attribute name='standard_name' value='latitude' />\n" +
"    <attribute name='units' value='degrees_north' />\n" +
"    <attribute name='axis' value='Y' />\n" +
"  </variable>\n" +
"  <variable name='longitude' type='double' shape='station' >\n" +
"    <attribute name='long_name' value='Longitud' />\n" +
"    <attribute name='units' value='degrees_east' />\n" +
"    <attribute name='standard_name' value='longitude' />\n" +
"    <attribute name='axis' value='X' />\n" +
"  </variable>\n" +
"  <variable name='z' type='double' shape='station' >\n" +
"    <attribute name='long_name' value='profundidad' />\n" +
"    <attribute name='units' value='m' />\n" +
"    <attribute name='standard_name' value='depth' />\n" +
"    <attribute name='axis' value='Z' />\n" +
"  </variable>\n" +
"  <variable name='ProfDiseno' type='double' shape='station' >\n" +
"    <attribute name='long_name' value='Profundidad de diseno' />\n" +
"    <attribute name='units' value='m' />\n" +
"  </variable>\n" +
"  <variable name='TiranteDiseno' type='double' shape='station' >\n" +
"    <attribute name='long_name' value='Tirante diseno' />\n" +
"    <attribute name='units' value='m' />\n" +
"  </variable>\n" +
"  <variable name='TiranteEstimado' type='double' shape='station' >\n" +
"    <attribute name='long_name' value='Tirante estimado' />\n" +
"    <attribute name='units' value='m' />\n" +
"  </variable>\n" +
"  <variable name='var_pres' type='double' shape='station' >\n" +
"    <attribute name='long_name' value='Bandera presion' />\n" +
"    <attribute name='units' value='N/A' />\n" +
"  </variable>\n" +
"</netcdf>\n";
//Before netcdf-java 4.6.4: ProfDiseno, TiranteDiseno, TiranteEstimado, and var_pres
//appeared as shape='station one' even though he got rid of 'one' dimension
//via .ncml
            Test.ensureEqual(results, expected, "results=\n" + results);

            //read the .ncml via table.readNcCF
            table = new Table();
            table.readNcCF(baseName + ".ncml", null, null, null, null);
            results = table.toCSVString(5);
            results = String2.replaceAll(results, '\t', ' ');
            expected = 
"{\n" +
"dimensions:\n" +
" row = 101 ;\n" +
"variables:\n" +
" double Cond(row) ;\n" +
"  Cond:coordinates = \"time latitude longitude z\" ;\n" +
"  Cond:long_name = \"Conductividad\" ;\n" +
"  Cond:standard_name = \"sea_water_electrical_conductivity\" ;\n" +
"  Cond:units = \"S/m\" ;\n" +
" double Pres(row) ;\n" +
"  Pres:coordinates = \"time latitude longitude z\" ;\n" +
"  Pres:long_name = \"Presion\" ;\n" +
"  Pres:standard_name = \"sea_water_pressure\" ;\n" +
"  Pres:units = \"dBar\" ;\n" +
" double Temp(row) ;\n" +
"  Temp:coordinates = \"time latitude longitude z\" ;\n" +
"  Temp:long_name = \"Temperatura\" ;\n" +
"  Temp:standard_name = \"sea_water_temperature\" ;\n" +
"  Temp:units = \"degree_celsius\" ;\n" +
" double Sal(row) ;\n" +
"  Sal:coordinates = \"time latitude longitude z\" ;\n" +
"  Sal:long_name = \"Salinidad\" ;\n" +
"  Sal:standard_name = \"sea_water_salinity\" ;\n" +
"  Sal:units = \"PSU\" ;\n" +
" double ProfDiseno(row) ;\n" +
"  ProfDiseno:long_name = \"Profundidad de diseno\" ;\n" +
"  ProfDiseno:units = \"m\" ;\n" +
" double TiranteDiseno(row) ;\n" +
"  TiranteDiseno:long_name = \"Tirante diseno\" ;\n" +
"  TiranteDiseno:units = \"m\" ;\n" +
" double TiranteEstimado(row) ;\n" +
"  TiranteEstimado:long_name = \"Tirante estimado\" ;\n" +
"  TiranteEstimado:units = \"m\" ;\n" +
" double var_pres(row) ;\n" +
"  var_pres:long_name = \"Bandera presion\" ;\n" +
"  var_pres:units = \"N/A\" ;\n" +
" int station(row) ;\n" +
"  station:cf_role = \"timeseries_id\" ;\n" +
"  station:long_name = \"CTZ-T500-MCT-NS5649-Z408-INS12-REC14\" ;\n" +
" double time(row) ;\n" +
"  time:_CoordinateAxisType = \"Time\" ;\n" +
"  time:axis = \"T\" ;\n" +
"  time:calendar = \"julian\" ;\n" +
"  time:long_name = \"tiempo en dias Julianos\" ;\n" +
"  time:standard_name = \"time\" ;\n" +
"  time:time_origin = \"0000-01-01 00:00:00\" ;\n" +
"  time:units = \"days since 0000-01-01 00:00:00 \" ;\n" +
" double latitude(row) ;\n" +
"  latitude:_CoordinateAxisType = \"Lat\" ;\n" +
"  latitude:axis = \"Y\" ;\n" +
"  latitude:long_name = \"Latitud\" ;\n" +
"  latitude:standard_name = \"latitude\" ;\n" +
"  latitude:units = \"degrees_north\" ;\n" +
" double longitude(row) ;\n" +
"  longitude:_CoordinateAxisType = \"Lon\" ;\n" +
"  longitude:axis = \"X\" ;\n" +
"  longitude:long_name = \"Longitud\" ;\n" +
"  longitude:standard_name = \"longitude\" ;\n" +
"  longitude:units = \"degrees_east\" ;\n" +
" double z(row) ;\n" +
"  z:_CoordinateAxisType = \"Height\" ;\n" +
"  z:axis = \"Z\" ;\n" +
"  z:long_name = \"profundidad\" ;\n" +
"  z:standard_name = \"depth\" ;\n" +
"  z:units = \"m\" ;\n" +
"\n" +
"// global attributes:\n" +
"  :_CoordSysBuilder = \"ucar.nc2.dataset.conv.CF1Convention\" ;\n" +
"  :Anclaje = \"CTZ-T500\" ;\n" +
"  :cdm_data_type = \"TimeSeries\" ;\n" +
"  :cdm_timeseries_variables = \"ProfDiseno, TiranteDiseno, TiranteEstimado, var_pres, station, latitude, longitude, z\" ;\n" +
"  :Conventions = \"CF-1.6\" ;\n" +
"  :date_created = \"06-Aug-2014 12:22:59\" ;\n" +
"  :Equipo = \"MCT\" ;\n" +
"  :featureType = \"timeSeries\" ;\n" +
"  :Final_NC_file = \"CTZ-T500-MCT-NS5649-Z408-INS12-REC14.nc\" ;\n" +
"  :NCO = \"\\\"4.5.2\\\"\" ;\n" +
"  :Numero_de_serie = \"5649\" ;\n" +
"  :Source_file = \"CTZ-T500-MCT-NS5649-Z408-INS12-REC14.mat\" ;\n" +
"  :standard_name_vocabulary = \"CF-1.6\" ;\n" +
"  :subsetVariables = \"ProfDiseno, TiranteDiseno, TiranteEstimado, var_pres, station, latitude, longitude, z\" ;\n" +
"  :title = \"CTZ-T500-MCT-NS5649-Z408-INS12-REC14\" ;\n" +
"}\n" +
"row,Cond,Pres,Temp,Sal,ProfDiseno,TiranteDiseno,TiranteEstimado,var_pres,station,time,latitude,longitude,z\n" +
"0,3.88991,409.629,10.3397,35.310065426337346,408.0,500.0,498.0,1.0,0,733358.7847222222,18.843666666666667,-94.81761666666667,406.0\n" +
"1,3.88691,409.12,10.3353,35.28414747593317,408.0,500.0,498.0,1.0,0,733358.786111111,18.843666666666667,-94.81761666666667,406.0\n" +
"2,3.88678,408.803,10.3418,35.27667928948258,408.0,500.0,498.0,1.0,0,733358.7875,18.843666666666667,-94.81761666666667,406.0\n" +
"3,3.88683,408.623,10.3453,35.273879094537904,408.0,500.0,498.0,1.0,0,733358.7888888889,18.843666666666667,-94.81761666666667,406.0\n" +
"4,3.88808,408.517,10.3687,35.26394801644307,408.0,500.0,498.0,1.0,0,733358.7902777778,18.843666666666667,-94.81761666666667,406.0\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //read the .ncml via table.readNcCF -- just station info
            table = new Table();
            table.readNcCF(baseName + ".ncml", 
                StringArray.fromCSV("station,latitude,longitude,z,ProfDiseno,TiranteDiseno,TiranteEstimado,var_pres"), null, null, null);
            results = table.dataToCSVString();
            results = String2.replaceAll(results, '\t', ' ');
            expected = 
"station,latitude,longitude,z,ProfDiseno,TiranteDiseno,TiranteEstimado,var_pres\n" +
"0,18.843666666666667,-94.81761666666667,406.0,408.0,500.0,498.0,1.0\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t)); 
        }

        String2.log("\n*** EDDTableFromNcCFFiles.testNcml() finished.");
    }
    
    
    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {
        /* 
        testGenerateDatasetsXml();
        test1(true); //deleteCachedDatasetInfo
        test1(false); 
        testKevin20130109();
        testNoAttName();
        testBridger();
*/        testNcml();

        //not usually run
    }
}

