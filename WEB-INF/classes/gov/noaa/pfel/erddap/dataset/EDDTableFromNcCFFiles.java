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

import gov.noaa.pfel.coastwatch.pointdata.Table;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;


/** 
 * This class represents a table of data from a collection of FeatureDatasets
 * using CF Discrete Sampling Geometries (was Point Observation Conventions), 
 * http://cf-pcmdi.llnl.gov/documents/cf-conventions/1.6/cf-conventions.html#discrete-sampling-geometries
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
        String tDatasetID, String tAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes,
        String tFileDir, boolean tRecursive, String tFileNameRegex, String tMetadataFrom,
        String tCharset, int tColumnNamesRow, int tFirstDataRow,
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex, 
        String tColumnNameForExtract,
        String tSortedColumnSourceName, String tSortFilesBySourceNames,
        boolean tSourceNeedsExpandedFP_EQ, boolean tFileTableInMemory) 
        throws Throwable {

        super("EDDTableFromNcCFFiles", 
            true, //tIsLocal, 
            tDatasetID, tAccessibleTo, tOnChange, tFgdcFile, tIso19115File, 
            tDefaultDataQuery, tDefaultGraphQuery,
            tAddGlobalAttributes, 
            tDataVariables, tReloadEveryNMinutes,
            tFileDir, tRecursive, tFileNameRegex, tMetadataFrom,
            tCharset, tColumnNamesRow, tFirstDataRow,  //irrelevant
            tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
            tSortedColumnSourceName, //irrelevant
            tSortFilesBySourceNames,
            tSourceNeedsExpandedFP_EQ, tFileTableInMemory);

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
     * @throws Throwable if trouble
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
        tFileDir = File2.addSlash(tFileDir); //ensure it has trailing slash

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
                    sourceAtts, colName, true, true)); //addColorBarMinMax, tryToFindLLAT
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
        externalAddGlobalAttributes.setIfNotAlreadySet("sourceUrl", "(local files)");
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
            "    <fileDir>" + tFileDir + "</fileDir>\n" +
            "    <recursive>true</recursive>\n" +
            "    <fileNameRegex>" + suggestedRegex + "</fileNameRegex>\n" +
            "    <metadataFrom>last</metadataFrom>\n" +
            "    <preExtractRegex>" + tPreExtractRegex + "</preExtractRegex>\n" +
            "    <postExtractRegex>" + tPostExtractRegex + "</postExtractRegex>\n" +
            "    <extractRegex>" + tExtractRegex + "</extractRegex>\n" +
            "    <columnNameForExtract>" + tColumnNameForExtract + "</columnNameForExtract>\n" +
            "    <sortFilesBySourceNames>" + tSortFilesBySourceNames + "</sortFilesBySourceNames>\n" +
            "    <fileTableInMemory>false</fileTableInMemory>\n");
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
                "c:/data/nccf/", "ncCF1b\\.nc",
                "c:/data/nccf/ncCF1b.nc",
                1440,
                "", "", "", "", //just for test purposes; station is already a column in the file
                "line_station time", 
                "", "", "", "", null);

            //GenerateDatasetsXml
            GenerateDatasetsXml.doIt(new String[]{"-verbose", 
                "EDDTableFromNcCFFiles",
                "c:/data/nccf/", "ncCF1b\\.nc",
                "c:/data/nccf/ncCF1b.nc",
                "1440",
                "", "", "", "", //just for test purposes; station is already a column in the file
                "line_station time", 
                "", "", "", "", ""},
                false); //doIt loop?
            String gdxResults = String2.getClipboardString();
            Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

String expected = 
directionsForGenerateDatasetsXml() +
"-->\n" +
"\n" +
"<dataset type=\"EDDTableFromNcCFFiles\" datasetID=\"nccf_3492_0c4f_44b3\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <fileDir>c:/data/nccf/</fileDir>\n" +
"    <recursive>true</recursive>\n" +
"    <fileNameRegex>ncCF1b\\.nc</fileNameRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <preExtractRegex></preExtractRegex>\n" +
"    <postExtractRegex></postExtractRegex>\n" +
"    <extractRegex></extractRegex>\n" +
"    <columnNameForExtract></columnNameForExtract>\n" +
"    <sortFilesBySourceNames>line_station time</sortFilesBySourceNames>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
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
"        <att name=\"creator_name\">CalCOFI</att>\n" +
"        <att name=\"creator_url\">http://www.calcofi.org/newhome/publications/Atlases/atlases.htm</att>\n" +
"        <att name=\"keywords\">1984-2004, altitude, animals, aquatic, atmosphere,\n" +
"Atmosphere &gt; Altitude &gt; Station Height,\n" +
"biological,\n" +
"Biological Classification &gt; Animals/Vertebrates &gt; Fish,\n" +
"biology, biosphere,\n" +
"Biosphere &gt; Aquatic Ecosystems &gt; Coastal Habitat,\n" +
"Biosphere &gt; Aquatic Ecosystems &gt; Marine Habitat,\n" +
"calcofi, classification, coastal, code, common, count, cruise, ecosystems, fish, fisheries, habitat, height, identifier, larvae, line, marine, name, number, observed, occupancy, oceans,\n" +
"Oceans &gt; Aquatic Sciences &gt; Fisheries,\n" +
"order, sciences, scientific, ship, start, station, time, tow, units, value, vertebrates</att>\n" +
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
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n";

            Test.ensureEqual(results, expected, "results=\n" + results);
            //Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), 
            //    expected, "");

            EDD edd = oneFromXmlFragment(results);
            Test.ensureEqual(edd.datasetID(), "nccf_3492_0c4f_44b3", "");
            Test.ensureEqual(edd.title(), "CalCOFI Fish Larvae Count, 1984-2004", "");
            Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
                "line_station, longitude, latitude, altitude, time, obsScientific, obsValue, obsUnits", 
                "");

        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nError using generateDatasetsXml." + 
                "\nPress ^C to stop or Enter to continue..."); 
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
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);

        String id = "testNcCF1b";
        if (deleteCachedDatasetInfo) 
            deleteCachedDatasetInfo(id);

        EDDTable eddTable = (EDDTable)oneFromDatasetXml(id); 


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
        boolean oDebugMode = debugMode;  debugMode = true;
        boolean oDebug = Table.debug;    Table.debug = true;

        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);

        String id = "testKevin20130109";
        EDDTable eddTable = (EDDTable)oneFromDatasetXml(id); 
        
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
        Table.debug = oDebug;

    }

    
    
    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {
        /* */
        testGenerateDatasetsXml();
        test1(true); //deleteCachedDatasetInfo
        test1(false); 
        testKevin20130109();

        //not usually run
    }
}

