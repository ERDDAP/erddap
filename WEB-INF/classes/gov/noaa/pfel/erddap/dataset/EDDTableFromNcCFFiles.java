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
        String tDatasetID, String tAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tSosOfferingPrefix,
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
            tSosOfferingPrefix,
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
                "", "", "", "", null) + "\n";

            //GenerateDatasetsXml
            String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
                "EDDTableFromNcCFFiles",
                "c:/data/nccf/", "ncCF1b\\.nc",
                "c:/data/nccf/ncCF1b.nc",
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
     * This tests att with no name throws error.
     *
     * @throws Throwable if trouble
     */
    public static void testNoAttName() throws Throwable {
        String2.log("\n****************** EDDTableFromNcCFFiles.testNoAttName() *****************\n");

        try {
            EDDTable eddTable = (EDDTable)oneFromDatasetXml("testNoAttName1"); 
            throw new SimpleException("shouldn't get here");
        } catch (Throwable t) {
            String msg = t.toString();
            Test.ensureLinesMatch(msg, 
"java.lang.RuntimeException: datasets.xml error on line #\\d{1,7}: An <att> tag doesn't have a \"name\" attribute.", 
                "");
        }

        try {
            EDDTable eddTable = (EDDTable)oneFromDatasetXml("testNoAttName1"); 
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
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);
        Table table;

        table = new Table();
        table.readNcCF("c:/data/bridger/B01.accelerometer.historical.nc", null, null, null, null);
        Test.ensureSomethingUtf8(table.globalAttributes(), "historical global attributes");

        table = new Table();
        table.readNcCF("c:/data/bridger/B01.accelerometer.realtime.nc", null, null, null, null);
        Test.ensureSomethingUtf8(table.globalAttributes(), "realtime global attributes");

        String id = "UMaineAccB01";
        deleteCachedDatasetInfo(id);
        EDDTable eddTable = (EDDTable)oneFromDatasetXml(id); 

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
"    Int32 _ChunkSize 1;\n" +
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
"    Int32 _ChunkSize 1;\n" +
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
"    Int32 _ChunkSize 1;\n" +
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
"    Int16 valid_range -127, 127;\n" +
"  \\}\n" +
"  dominant_wave_period \\{\n" +
"    Int32 _ChunkSize 1;\n" +
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
"    Int32 _ChunkSize 1;\n" +
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
"    Int16 valid_range -127, 127;\n" +
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
"    String Conventions \"CF-1.6, COARDS, Unidata Dataset Discovery v1.0\";\n" +
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
today + "T.{8}Z http://127.0.0.1:8080/cwexperimental/tabledap/UMaineAccB01.das\";\n" +
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
"    String Metadata_Conventions \"CF-1.6, COARDS, Unidata Dataset Discovery v1.0\";\n" +
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
        testNoAttName();
        testBridger();

        //not usually run
    }
}

