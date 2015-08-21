/* 
 * EDDTableFromWFSFiles Copyright 2013, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.FloatArray;
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
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/** 
 * This class represents a table of data from a file downloaded from a WFS server.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2013-01-16
 */
public class EDDTableFromWFSFiles extends EDDTableFromAsciiFiles { 

    public final static String DefaultRowElementXPath = "/wfs:FeatureCollection/gml:featureMember";

    /** For testing, you may set this to true programmatically where needed, not here. */
    public static boolean developmentMode = false;


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
    public EDDTableFromWFSFiles(String tDatasetID, String tAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tSosOfferingPrefix, 
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes, int tUpdateEveryNMillis,
        String tFileDir, boolean tRecursive, String tFileNameRegex, String tMetadataFrom,
        String tCharset, int tColumnNamesRow, int tFirstDataRow,
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex, 
        String tColumnNameForExtract,
        String tSortedColumnSourceName, String tSortFilesBySourceNames,
        boolean tSourceNeedsExpandedFP_EQ, boolean tFileTableInMemory, 
        boolean tAccessibleViaFiles) 
        throws Throwable {

        super("EDDTableFromWFSFiles", tDatasetID, tAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix, 
            tDefaultDataQuery, tDefaultGraphQuery,
            tAddGlobalAttributes, 
            tDataVariables, tReloadEveryNMinutes, tUpdateEveryNMillis,
            tFileDir, tRecursive, tFileNameRegex, tMetadataFrom,
            tCharset, tColumnNamesRow, tFirstDataRow,
            tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
            tSortedColumnSourceName, tSortFilesBySourceNames,
            tSourceNeedsExpandedFP_EQ, tFileTableInMemory, tAccessibleViaFiles);
    }

    /**
     * This downloads data (as Strings) from a WFS server 
     * and saves to a UTF-8 ASCII TSV file.
     * (ASCII files have the advantage of supporting UTF-8, unlike .nc,
     * and variable length strings are stored space efficiently.)
     * ColumnNames are unchanged from Table.readXml.
     * Data is left as Strings.
     *
     * @param tSourceUrl  a request=GetFeatures URL which requests all of the data.
     *    See description and examples in JavaDocs for generateDatasetsXml.
     * @param tRowElementXPath the element (XPath style) identifying a row, 
     *    If null or "", the default will be used: /wfs:FeatureCollection/gml:featureMember .
     * @param fullFileName recommended: EDStatic.fullCopyDirectory + datasetID + "/data.tsv"
     * @returns error string ("" if no error)
     */
    public static String downloadData(String tSourceUrl, String tRowElementXPath,
        String fullFileName) {

        try {
            if (verbose) String2.log("EDDTableFromWFSFiles.downloadData");
            if (tRowElementXPath == null || tRowElementXPath.length() == 0)
                tRowElementXPath = DefaultRowElementXPath;

            //read the table
            Table table = new Table();
            InputStream is = SSR.getUrlInputStream(tSourceUrl); 
            BufferedReader in = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            table.readXml(in, false, //no validate since no .dtd
                tRowElementXPath, 
                null, false);  //row attributes,  simplify=false

            //save as UTF-8 ASCII TSV file
            //This writes to temp file first and throws Exception if trouble.
            table.saveAsTabbedASCII(fullFileName, "UTF-8"); 
            return "";

        } catch (Exception e) {
            return String2.ERROR + " in EDDTableFromWFSFiles.downloadData" +
                "\n  sourceUrl=" + tSourceUrl + 
                "\n  rowElementXPath=" + tRowElementXPath +
                "\n  " + MustBe.throwableToShortString(e);
        }

    }

    /** 
     * This generates a ready-to-use datasets.xml entry for an EDDTableFromWFSFiles.
     * The XML can then be edited by hand and added to the datasets.xml file.
     *
     * @param tSourceUrl  a request=GetFeatures URL which requests all of the data.
     *   See http://webhelp.esri.com/arcims/9.2/general/mergedProjects/wfs_connect/wfs_connector/get_feature.htm
     *   For example, this GetCapabilities
     *     http://services.azgs.az.gov/ArcGIS/services/aasggeothermal/COBoreholeTemperatures/MapServer/WFSServer?
     *     request=GetCapabilities&service=WFS
     *   lead to this tSourceUrl
     *     http://kgs.uky.edu/arcgis/services/aasggeothermal/WVBoreholeTemperatures/MapServer/WFSServer?
     *     request=GetFeature&service=WFS&typename=aasg:BoreholeTemperature&format="text/xml; subType=gml/3.1.1/profiles/gmlsf/1.0.0/0"
     *   See Bob's sample files in c:/data/mapserver
     * @param tRowElementXPath
     * @param tReloadEveryNMinutes
     * @param tInfoUrl       or "" if in externalAddGlobalAttributes or if not available (but try hard!)
     * @param tInstitution   or "" if in externalAddGlobalAttributes or if not available (but try hard!)
     * @param tSummary       or "" if in externalAddGlobalAttributes or if not available (but try hard!)
     * @param tTitle         or "" if in externalAddGlobalAttributes or if not available (but try hard!)
     * @param externalAddGlobalAttributes  These attributes are given priority.  Use null in none available.
     * @return a suggested chunk of xml for this dataset for use in datasets.xml 
     * @throws Throwable if trouble, e.g., if no Grid or Array variables are found.
     *    If no trouble, then a valid dataset.xml chunk has been returned.
     */
    public static String generateDatasetsXml(String tSourceUrl, String tRowElementXPath,
        int tReloadEveryNMinutes, 
        String tInfoUrl, String tInstitution, String tSummary, String tTitle,
        Attributes externalAddGlobalAttributes) throws Throwable {

        String2.log("EDDTableFromWFSFiles.generateDatasetsXml");
        if (tReloadEveryNMinutes <= 0 || tReloadEveryNMinutes == Integer.MAX_VALUE)
            tReloadEveryNMinutes = 1440; //1440 works well with suggestedUpdateEveryNMillis

        //*** basically, make a table to hold the sourceAttributes 
        //and a parallel table to hold the addAttributes
        Table dataSourceTable = new Table();
        Table dataAddTable = new Table();

        if (tRowElementXPath == null || tRowElementXPath.length() == 0)
            tRowElementXPath = DefaultRowElementXPath;

        Table table = new Table();
        if (developmentMode) {
            dataSourceTable.readXml(new BufferedReader(new FileReader(
                "c:/programs/mapserver/WVBoreholeResponsePretty.xml")), 
                false, tRowElementXPath, null, true); //simplify=true
        } else {
            InputStream is = SSR.getUrlInputStream(tSourceUrl); 
            BufferedReader in = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            dataSourceTable.readXml(in, false, //no validate since no .dtd
                tRowElementXPath, 
                null, true);  //row attributes,  here, simplify to get suggested dataTypes
        }

        //remove col[0], OBJECTID, which is an internal number created within this WFS response.

        if (dataSourceTable.nColumns() > 1 &&
            dataSourceTable.getColumnName(0).toLowerCase().endsWith("objectid"))
            dataSourceTable.removeColumn(0);

        //globalAttributes 
        if (externalAddGlobalAttributes == null)
            externalAddGlobalAttributes = new Attributes();
        if (tInfoUrl     != null && tInfoUrl.length()     > 0) externalAddGlobalAttributes.add("infoUrl",     tInfoUrl);
        if (tInstitution != null && tInstitution.length() > 0) externalAddGlobalAttributes.add("institution", tInstitution);
        if (tSummary     != null && tSummary.length()     > 0) externalAddGlobalAttributes.add("summary",     tSummary);
        if (tTitle       != null && tTitle.length()       > 0) externalAddGlobalAttributes.add("title",       tTitle);
        externalAddGlobalAttributes.set("sourceUrl", tSourceUrl);
        //externalAddGlobalAttributes.setIfNotAlreadySet("subsetVariables", "???");

        String tSortedColumnSourceName = "";
        for (int col = 0; col < dataSourceTable.nColumns(); col++) {
            String colName = dataSourceTable.getColumnName(col);
            Attributes sourceAtts = dataSourceTable.columnAttributes(col);

            //isDateTime?
            PrimitiveArray pa = (PrimitiveArray)dataSourceTable.getColumn(col).clone();
            String timeUnits = "";
            if (pa instanceof StringArray) {
                timeUnits = Calendar2.suggestDateTimeFormat((StringArray)pa);
                if (timeUnits.length() > 0) 
                    sourceAtts.set("units", timeUnits); //just temporarily to trick makeReadyToUse...
            }

            //make addAtts
            Attributes addAtts = makeReadyToUseAddVariableAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(), sourceAtts, colName, 
                timeUnits.length() == 0, true); //addColorBarMinMax, tryToFindLLAT

            //put time units
            if (timeUnits.length() > 0) {
                sourceAtts.remove("units"); //undo above
                addAtts.set("units", timeUnits); //correct place
            }

            //add to dataAddTable
            dataAddTable.addColumn(col, colName, pa, addAtts);                

            //files are likely sorted by first date time variable
            //and no harm if files aren't sorted that way
            if (tSortedColumnSourceName.length() == 0 && timeUnits.length() > 0) 
                tSortedColumnSourceName = colName;
        }

        //after dataVariables known, add global attributes in the dataAddTable
        String tDatasetID = suggestDatasetID(tSourceUrl);
        dataAddTable.globalAttributes().set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(), 
                //another cdm_data_type could be better; this is good for now
                probablyHasLonLatTime(dataSourceTable, dataAddTable)? "Point" : "Other",
                EDStatic.fullCopyDirectory + tDatasetID + "/", 
                externalAddGlobalAttributes, 
                suggestKeywords(dataSourceTable, dataAddTable)));
        dataAddTable.globalAttributes().set("rowElementXPath", tRowElementXPath);

        //write the information
        StringBuilder sb = new StringBuilder();
        sb.append(
            directionsForGenerateDatasetsXml() +
            " * Since the source files don't have any metadata, you must add metadata\n" +
            "   below, notably 'units' for each of the dataVariables.\n" +
            "-->\n\n" +
            "<dataset type=\"EDDTableFromWFSFiles\" datasetID=\"" + tDatasetID +
                "\" active=\"true\">\n" +
            "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n" +  
            "    <updateEveryNMillis>0</updateEveryNMillis>\n" +  //files are only added by full reload
            //"    <recursive>false</recursive>\n" +
            //"    <fileDir>" + EDStatic.fullCopyDirectory + tDatasetID + "/</fileDir>\n" +
            //"    <fileNameRegex>.*\\.tsv</fileNameRegex>\n" +
            "    <metadataFrom>last</metadataFrom>\n" +
            "    <fileTableInMemory>false</fileTableInMemory>\n" +
            "    <accessibleViaFiles>false</accessibleViaFiles>\n");
            //"    <charset>UTF-8</charset>\n" +
            //"    <columnNamesRow>1</columnNamesRow>\n" +
            //"    <firstDataRow>3</firstDataRow>\n" +
            //"    <preExtractRegex>" + XML.encodeAsXML(tPreExtractRegex) + "</preExtractRegex>\n" +
            //"    <postExtractRegex>" + XML.encodeAsXML(tPostExtractRegex) + "</postExtractRegex>\n" +
            //"    <extractRegex>" + XML.encodeAsXML(tExtractRegex) + "</extractRegex>\n" +
            //"    <columnNameForExtract>" + tColumnNameForExtract + "</columnNameForExtract>\n" +
            //"    <sortedColumnSourceName>" + tSortedColumnSourceName + "</sortedColumnSourceName>\n" +
            //"    <sortFilesBySourceNames>" + tSortFilesBySourceNames + "</sortFilesBySourceNames>\n");
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
        boolean oDevelopmentMode = developmentMode;
        developmentMode = false;
        try {

        Attributes externalAddAttributes = new Attributes();
        externalAddAttributes.add("title", "Old Title!");
        String results = generateDatasetsXml(
            "http://kgs.uky.edu/arcgis/services/aasggeothermal/WVBoreholeTemperatures/MapServer/WFSServer?request=GetFeature&service=WFS&typename=aasg:BoreholeTemperature&format=\"text/xml;%20subType=gml/3.1.1/profiles/gmlsf/1.0.0/0\"", 
            null, //default tRowElementXPath,
            DEFAULT_RELOAD_EVERY_N_MINUTES, 
            "http://www.uky.edu/KGS/", 
            "Kentucky Geological Survey", 
            "The summary.", 
            "The Title",
            externalAddAttributes) + "\n";

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDTableFromWFSFiles",
            "http://kgs.uky.edu/arcgis/services/aasggeothermal/WVBoreholeTemperatures/MapServer/WFSServer?request=GetFeature&service=WFS&typename=aasg:BoreholeTemperature&format=\"text/xml;%20subType=gml/3.1.1/profiles/gmlsf/1.0.0/0\"", 
            "", //default tRowElementXPath,
            "" + DEFAULT_RELOAD_EVERY_N_MINUTES, 
            "http://www.uky.edu/KGS/", 
            "Kentucky Geological Survey", 
            "The summary.", 
            "The Title"},
            false); //doIt loop?
        Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

String expected = 
directionsForGenerateDatasetsXml() +
" * Since the source files don't have any metadata, you must add metadata\n" +
"   below, notably 'units' for each of the dataVariables.\n" +
"-->\n" +
"\n" +
"<dataset type=\"EDDTableFromWFSFiles\" datasetID=\"uky_2ed7_6297_9e6e\" active=\"true\">\n" +
"    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>0</updateEveryNMillis>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <accessibleViaFiles>false</accessibleViaFiles>\n" +
"    <!-- sourceAttributes>\n" +
"    </sourceAttributes -->\n" +
"    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" +
"        <att name=\"cdm_timeseries_variables\">station, longitude, latitude</att>\n" +
"        <att name=\"subsetVariables\">station, longitude, latitude</att>\n" +
"    -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Point</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"creator_name\">Kentucky Geological Survey</att>\n" +
"        <att name=\"creator_url\">http://www.uky.edu/KGS/</att>\n" +
"        <att name=\"infoUrl\">http://www.uky.edu/KGS/</att>\n" +
"        <att name=\"institution\">Kentucky Geological Survey</att>\n" +
"        <att name=\"keywords\">aasg:BoreholeTemperature/aasg:APINo, aasg:BoreholeTemperature/aasg:CommodityOfInterest, aasg:BoreholeTemperature/aasg:County, aasg:BoreholeTemperature/aasg:DepthOfMeasurement, aasg:BoreholeTemperature/aasg:DepthReferencePoint, aasg:BoreholeTemperature/aasg:DrillerTotalDepth, aasg:BoreholeTemperature/aasg:ElevationGL, aasg:BoreholeTemperature/aasg:EndedDrillingDate, aasg:BoreholeTemperature/aasg:Field, aasg:BoreholeTemperature/aasg:FormationTD, aasg:BoreholeTemperature/aasg:Function, aasg:BoreholeTemperature/aasg:HeaderURI, aasg:BoreholeTemperature/aasg:Label, aasg:BoreholeTemperature/aasg:LatDegree, aasg:BoreholeTemperature/aasg:LeaseName, aasg:BoreholeTemperature/aasg:LengthUnits, aasg:BoreholeTemperature/aasg:LocationUncertaintyStatement, aasg:BoreholeTemperature/aasg:LongDegree, aasg:BoreholeTemperature/aasg:MeasuredTemperature, aasg:BoreholeTemperature/aasg:MeasurementFormation, aasg:BoreholeTemperature/aasg:MeasurementProcedure, aasg:BoreholeTemperature/aasg:MeasurementSource, aasg:BoreholeTemperature/aasg:Notes, aasg:BoreholeTemperature/aasg:ObservationURI, aasg:BoreholeTemperature/aasg:Operator, aasg:BoreholeTemperature/aasg:OtherName, aasg:BoreholeTemperature/aasg:ProducingInterval, aasg:BoreholeTemperature/aasg:Production, aasg:BoreholeTemperature/aasg:RelatedResource, aasg:BoreholeTemperature/aasg:Shape/gml:Point/latitude, aasg:BoreholeTemperature/aasg:Shape/gml:Point/longitude, aasg:BoreholeTemperature/aasg:SpudDate, aasg:BoreholeTemperature/aasg:SRS, aasg:BoreholeTemperature/aasg:State, aasg:BoreholeTemperature/aasg:Status, aasg:BoreholeTemperature/aasg:TemperatureUnits, aasg:BoreholeTemperature/aasg:TimeSinceCirculation, aasg:BoreholeTemperature/aasg:TrueVerticalDepth, aasg:BoreholeTemperature/aasg:WellBoreShape, aasg:BoreholeTemperature/aasg:WellName, aasg:BoreholeTemperature/aasg:WellType, apino, bore, circulation, commodity, county, data, date, degree, depth, driller, drilling, elevation, ended, field, formation, function, geological, header, interest, interval, kentucky, label, latitude, lease, length, long, longitude, measured, measurement, name, notes, observation, operator, point, procedure, producing, production, quality, reference, related, resource, shape, since, source, spud, srs, state, statement, status, survey, temperature, time, title, total, true, type, uncertainty, units, vertical, well</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"rowElementXPath\">/wfs:FeatureCollection/gml:featureMember</att>\n" +
"        <att name=\"sourceUrl\">http://kgs.uky.edu/arcgis/services/aasggeothermal/WVBoreholeTemperatures/MapServer/WFSServer?request=GetFeature&amp;service=WFS&amp;typename=aasg:BoreholeTemperature&amp;format=&quot;text/xml;&#37;20subType=gml/3.1.1/profiles/gmlsf/1.0.0/0&quot;</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v29</att>\n" +
"        <att name=\"summary\">The summary. Kentucky Geological Survey data from a local source.</att>\n" +
"        <att name=\"title\">The Title</att>\n" +
"    </addAttributes>\n" +
//removed.  It is an internal number in the WFS response.
//"    <dataVariable>\n" +
//"        <sourceName>aasg:BoreholeTemperature/aasg:OBJECTID</sourceName>\n" +
//"        <destinationName>OBJECTID</destinationName>\n" +
//"        <dataType>short</dataType>\n" +
//"        <!-- sourceAttributes>\n" +
//"        </sourceAttributes -->\n" +
//"        <addAttributes>\n" +
//"            <att name=\"ioos_category\">Temperature</att>\n" +
//"            <att name=\"long_name\">OBJECTID</att>\n" +
//"        </addAttributes>\n" +
//"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:ObservationURI</sourceName>\n" +
"        <destinationName>ObservationURI</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Observation URI</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:WellName</sourceName>\n" +
"        <destinationName>WellName</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Well Name</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:APINo</sourceName>\n" +
"        <destinationName>APINo</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">APINo</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:HeaderURI</sourceName>\n" +
"        <destinationName>HeaderURI</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Header URI</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:Label</sourceName>\n" +
"        <destinationName>Label</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Label</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:Operator</sourceName>\n" +
"        <destinationName>Operator</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Operator</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:SpudDate</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Spud Date</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"units\">yyyy-MM-dd&#39;T&#39;HH:mm:ssZ</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:EndedDrillingDate</sourceName>\n" +
"        <destinationName>EndedDrillingDate</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Ended Drilling Date</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"units\">yyyy-MM-dd&#39;T&#39;HH:mm:ssZ</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:WellType</sourceName>\n" +
"        <destinationName>WellType</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Well Type</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:Status</sourceName>\n" +
"        <destinationName>Status</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Status</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:CommodityOfInterest</sourceName>\n" +
"        <destinationName>CommodityOfInterest</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Commodity Of Interest</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:Function</sourceName>\n" +
"        <destinationName>Function</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Function</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:Production</sourceName>\n" +
"        <destinationName>Production</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Production</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:ProducingInterval</sourceName>\n" +
"        <destinationName>ProducingInterval</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Producing Interval</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:Field</sourceName>\n" +
"        <destinationName>Field</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Field</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:County</sourceName>\n" +
"        <destinationName>County</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">County</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:State</sourceName>\n" +
"        <destinationName>State</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">State</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:LatDegree</sourceName>\n" +
"        <destinationName>LatDegree</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Lat Degree</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:LongDegree</sourceName>\n" +
"        <destinationName>LongDegree</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Long Degree</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:SRS</sourceName>\n" +
"        <destinationName>SRS</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">SRS</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:LocationUncertaintyStatement</sourceName>\n" +
"        <destinationName>LocationUncertaintyStatement</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"            <att name=\"long_name\">Location Uncertainty Statement</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:DrillerTotalDepth</sourceName>\n" +
"        <destinationName>DrillerTotalDepth</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Driller Total Depth</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:DepthReferencePoint</sourceName>\n" +
"        <destinationName>DepthReferencePoint</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Depth Reference Point</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:LengthUnits</sourceName>\n" +
"        <destinationName>LengthUnits</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Length Units</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:WellBoreShape</sourceName>\n" +
"        <destinationName>WellBoreShape</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Well Bore Shape</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:TrueVerticalDepth</sourceName>\n" +
"        <destinationName>TrueVerticalDepth</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">True Vertical Depth</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:ElevationGL</sourceName>\n" +
"        <destinationName>ElevationGL</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Elevation GL</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:FormationTD</sourceName>\n" +
"        <destinationName>FormationTD</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Formation TD</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:MeasuredTemperature</sourceName>\n" +
"        <destinationName>MeasuredTemperature</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Measured Temperature</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:TemperatureUnits</sourceName>\n" +
"        <destinationName>TemperatureUnits</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Temperature Units</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:MeasurementProcedure</sourceName>\n" +
"        <destinationName>MeasurementProcedure</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Measurement Procedure</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:DepthOfMeasurement</sourceName>\n" +
"        <destinationName>DepthOfMeasurement</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Depth Of Measurement</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:MeasurementFormation</sourceName>\n" +
"        <destinationName>MeasurementFormation</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Measurement Formation</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:MeasurementSource</sourceName>\n" +
"        <destinationName>MeasurementSource</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Measurement Source</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:RelatedResource</sourceName>\n" +
"        <destinationName>RelatedResource</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Related Resource</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:Shape/gml:Point/latitude</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:Shape/gml:Point/longitude</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:TimeSinceCirculation</sourceName>\n" +
"        <destinationName>TimeSinceCirculation</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Time Since Circulation</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:OtherName</sourceName>\n" +
"        <destinationName>OtherName</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Other Name</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:LeaseName</sourceName>\n" +
"        <destinationName>LeaseName</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Lease Name</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:Notes</sourceName>\n" +
"        <destinationName>Notes</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Notes</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";

            Test.ensureEqual(results, expected, "results=\n" + results);
            //Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), 
            //    expected, "");

            //ensure it is ready-to-use by making a dataset from it
            EDD edd = oneFromXmlFragment(results);
            //these won't actually be tested...
            Test.ensureEqual(edd.datasetID(), "uky_2ed7_6297_9e6e", "");
            Test.ensureEqual(edd.title(), "The Title", "");
            Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
"ObservationURI, WellName, APINo, HeaderURI, Label, Operator, time, EndedDrillingDate, " +
"WellType, Status, CommodityOfInterest, Function, Production, ProducingInterval, Field, County, " +
"State, LatDegree, LongDegree, SRS, LocationUncertaintyStatement, DrillerTotalDepth, " +
"DepthReferencePoint, LengthUnits, WellBoreShape, TrueVerticalDepth, ElevationGL, FormationTD, " +
"MeasuredTemperature, TemperatureUnits, MeasurementProcedure, DepthOfMeasurement, " +
"MeasurementFormation, MeasurementSource, RelatedResource, latitude, longitude, " +
"TimeSinceCirculation, OtherName, LeaseName, Notes", "");


            //ensure LatDegree=latitude and LongDegree=longitude
            Table table = new Table();
            table.readASCII(EDStatic.fullCopyDirectory + edd.datasetID() + "/data.tsv", 0, 2);
            PrimitiveArray pa1 = new FloatArray(table.getColumn("aasg:BoreholeTemperature/aasg:LatDegree"));
            PrimitiveArray pa2 = new FloatArray(table.getColumn("aasg:BoreholeTemperature/aasg:Shape/gml:Point/latitude"));
            Test.ensureEqual(pa1, pa2, "");

            pa1 = new FloatArray(table.getColumn("aasg:BoreholeTemperature/aasg:LongDegree"));
            pa2 = new FloatArray(table.getColumn("aasg:BoreholeTemperature/aasg:Shape/gml:Point/longitude"));
            Test.ensureEqual(pa1, pa2, "");

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "*** The server disappeared ~July 2015. Find another?"); 
        }
        developmentMode = oDevelopmentMode;

    }


    /**
     * Basic tests of this class.
     */
    public static void testBasic() throws Throwable {

        String2.log("\n*** EDDTableFromWFSFiles.testBasic");
        EDDTable tedd = (EDDTable)oneFromDatasetXml("kgsBoreTempWVTRUE"); //should work
        String tName, error, results, tResults, expected;
        int po;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
        try {

        //*** .das
        tName = tedd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            "kgsBoreTempWVTRUE", ".das"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected =   
"Attributes {\n" +
" s {\n" +
"  ObservationURI {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Observation URI\";\n" +
"  }\n" +
"  WellName {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Well Name\";\n" +
"  }\n" +
"  APINo {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"APINo\";\n" +
"  }\n" +
"  HeaderURI {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Header URI\";\n" +
"  }\n" +
"  Label {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Label\";\n" +
"  }\n" +
"  Operator {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Operator\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range -2.2379328e+9, 1.3441248e+9;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Spud Date\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String time_precision \"1970-01-01\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  EndedDrillingDate {\n" +
"    Float64 actual_range -2.5245216e+9, 1.3554432e+9;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Ended Drilling Date\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String time_precision \"1970-01-01\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  WellType {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Well Type\";\n" +
"  }\n" +
"  Field {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Field\";\n" +
"  }\n" +
"  County {\n" +
"    Float64 colorBarMaximum 100.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Statistics\";\n" +
"    String long_name \"County\";\n" +
"  }\n" +
"  State {\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"State\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range 37.24673, 39.98267;\n" +
"    String axis \"Y\";\n" +
"    Float64 colorBarMaximum 90.0;\n" +
"    Float64 colorBarMinimum -90.0;\n" +
"    String ioos_category \"Location\";\n" +
"    String LocationUncertaintyStatement \"Location recorded as received from official permit application converted to NAD83 if required\";\n" +
"    String long_name \"Latitude\";\n" +
"    String SRS \"NAD 83\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 actual_range -82.54999, -78.80319;\n" +
"    String axis \"X\";\n" +
"    Float64 colorBarMaximum 180.0;\n" +
"    Float64 colorBarMinimum -180.0;\n" +
"    String ioos_category \"Location\";\n" +
"    String LocationUncertaintyStatement \"Location recorded as received from official permit application converted to NAD83 if required\";\n" +
"    String long_name \"Longitude\";\n" +
"    String SRS \"NAD 83\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  DrillerTotalDepth {\n" +
"    Int16 actual_range 1161, 12996;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Driller Total Depth\";\n" +
"    String units \"ft\";\n" +
"  }\n" +
"  DepthReferencePoint {\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Depth Reference Point\";\n" +
"  }\n" +
"  WellBoreShape {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Well Bore Shape\";\n" +
"  }\n" +
"  TrueVerticalDepth {\n" +
"    Int16 actual_range 1161, 11792;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"True Vertical Depth\";\n" +
"    String units \"ft\";\n" +
"  }\n" +
"  ElevationGL {\n" +
"    Int16 actual_range 568, 4333;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Elevation GL\";\n" +
"    String units \"ft\";\n" +
"  }\n" +
"  FormationTD {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Formation TD\";\n" +
"  }\n" +
"  MeasuredTemperature {\n" +
"    Float32 actual_range 8.0, 908.0;\n" +
"    Float64 colorBarMaximum 1000.0;\n" +  
"    Float64 colorBarMinimum 0.0;\n" +   
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Measured Temperature\";\n" +
"    String MeasurementProcedure \"Temperature log evaluated by WVGES staff for deepest stable log segment to extract data otherwise used given bottom hole temperature on log header if available\";\n" +
"    String MeasurementSource \"Well Temperature Log\";\n" +
"    String units \"degree_F\";\n" +
"  }\n" +
"  DepthOfMeasurement {\n" +
"    Int16 actual_range 23, 26181;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Depth Of Measurement\";\n" +
"    String units \"ft\";\n" +
"  }\n" +
"  MeasurementFormation {\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Measurement Formation\";\n" +
"  }\n" +
"  RelatedResource {\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Related Resource\";\n" +
"  }\n" +
"  TimeSinceCirculation {\n" +
"    Int16 actual_range 2, 2301;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time Since Circulation\";\n" +
"    String units \"?\";\n" +
"  }\n" +
"  OtherName {\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Other Name\";\n" +
"  }\n" +
"  LeaseName {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Lease Name\";\n" +
"  }\n" +
"  Notes {\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Notes\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"Point\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String creator_email \"doug@uky.edu\";\n" +
"    String creator_name \"Doug Curl\";\n" +
"    String creator_url \"http://www.uky.edu/KGS/\";\n" +
"    String drawLandMask \"under\";\n" +
"    Float64 Easternmost_Easting -78.80319;\n" +
"    String featureType \"Point\";\n" +
"    Float64 geospatial_lat_max 39.98267;\n" +
"    Float64 geospatial_lat_min 37.24673;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max -78.80319;\n" +
"    Float64 geospatial_lon_min -82.54999;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String history \"" + today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

expected = 
"String infoUrl \"http://www.uky.edu/KGS/\";\n" +
"    String institution \"Kentucky Geological Survey\";\n" +
"    String keywords \"apino, bore, borehole, circulation, commodity, county, date, degree, depth, driller, drilling, elevation, ended, field, formation, function, geological, geothermal, header, interest, interval, kentucky, label, lease, length, long, measured, measurement, name, notes, observation, operator, point, procedure, producing, production, quality, reference, related, resource, shape, since, source, spud, srs, state, statement, statistics, status, survey, temperature, temperatures, time, total, true, type, uncertainty, units, URI, vertical, well, West Virginia\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    Float64 Northernmost_Northing 39.98267;\n" +
"    String rowElementXPath \"/wfs:FeatureCollection/gml:featureMember\";\n" +
"    String sourceUrl \"http://kgs.uky.edu/arcgis/services/aasggeothermal/WVBoreholeTemperatures/MapServer/WFSServer?request=GetFeature&service=WFS&typename=aasg:BoreholeTemperature&format=\\\"text/xml;%20subType=gml/3.1.1/profiles/gmlsf/1.0.0/0\\\"\";\n" +
"    Float64 Southernmost_Northing 37.24673;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
"    String subsetVariables \"WellName,APINo,Label,Operator,WellType,Field,County,State,FormationTD,OtherName\";\n" +
"    String summary \"Borehole temperature measurements in West Virginia\";\n" +
"    String time_coverage_end \"2012-08-05\";\n" +
"    String time_coverage_start \"1899-01-31\";\n" +
"    String title \"West Virginia Borehole Temperatures, AASG State Geothermal Data\";\n" +
"    Float64 Westernmost_Easting -82.54999;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);
        

        //*** .dds
        tName = tedd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            "kgsBoreTempWVTRUE", ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String ObservationURI;\n" +
"    String WellName;\n" +
"    String APINo;\n" +
"    String HeaderURI;\n" +
"    String Label;\n" +
"    String Operator;\n" +
"    Float64 time;\n" +
"    Float64 EndedDrillingDate;\n" +
"    String WellType;\n" +
"    String Field;\n" +
"    String County;\n" +
"    String State;\n" +
"    Float32 latitude;\n" +
"    Float32 longitude;\n" +
"    Int16 DrillerTotalDepth;\n" +
"    String DepthReferencePoint;\n" +
"    String WellBoreShape;\n" +
"    Int16 TrueVerticalDepth;\n" +
"    Int16 ElevationGL;\n" +
"    String FormationTD;\n" +
"    Float32 MeasuredTemperature;\n" +
"    Int16 DepthOfMeasurement;\n" +
"    String MeasurementFormation;\n" +
"    String RelatedResource;\n" +
"    Int16 TimeSinceCirculation;\n" +
"    String OtherName;\n" +
"    String LeaseName;\n" +
"    String Notes;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        tName = tedd.makeNewFileForDapQuery(null, null, 
            "&APINo=\"4700102422\"", 
            EDStatic.fullTestCacheDirectory, "kgsBoreTempWVTRUE", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"ObservationURI,WellName,APINo,HeaderURI,Label,Operator,time,EndedDrillingDate,WellType,Field,County,State,latitude,longitude,DrillerTotalDepth,DepthReferencePoint,WellBoreShape,TrueVerticalDepth,ElevationGL,FormationTD,MeasuredTemperature,DepthOfMeasurement,MeasurementFormation,RelatedResource,TimeSinceCirculation,OtherName,LeaseName,Notes\n" +
",,,,,,UTC,UTC,,,,,degrees_north,degrees_east,ft,,,ft,ft,,degree_F,ft,,,?,,,\n" +
"http://resources.usgin.org/uri-gin/wvges/bhtemp/4700102422_105/,\"Fuel Resources, Inc.  Zona Bernard 2\",4700102422,http://resources.usgin.org/uri-gin/wvges/well/api:4700102422/,4700102422,\"Fuel Resources, Inc.\",1989-03-15T00:00:00Z,1989-03-21T00:00:00Z,Gas,Belington,Barbour,West Virginia,38.989952,-79.96464,5479,G.L.,vertical,5479,2028,Fox,105.0,4650,Elk,TL | GR | DEN | IL | CAL,5,Fuel Resources Inc,,\n";

        Test.ensureEqual(results.substring(0, expected.length()), expected, 
            "\nresults=\n" + results);

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "Unexpected error"); 
        }

    }


    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {
        testGenerateDatasetsXml();
        testBasic();
    }
}

