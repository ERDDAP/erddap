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
     * This returns the default value for standardizeWhat for this subclass.
     * See Attributes.unpackVariable for options.
     * The default was chosen to mimic the subclass' behavior from
     * before support for standardizeWhat options was added.
     */
    public int defaultStandardizeWhat() {return DEFAULT_STANDARDIZEWHAT; } 
    public static int DEFAULT_STANDARDIZEWHAT = 0;


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
    public EDDTableFromWFSFiles(String tDatasetID, 
        String tAccessibleTo, String tGraphsAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tSosOfferingPrefix, 
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes, int tUpdateEveryNMillis,
        String tFileDir, String tFileNameRegex, boolean tRecursive, String tPathRegex, 
        String tMetadataFrom, String tCharset, 
        int tColumnNamesRow, int tFirstDataRow, String tColumnSeparator,
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex, 
        String tColumnNameForExtract,
        String tSortedColumnSourceName, String tSortFilesBySourceNames,
        boolean tSourceNeedsExpandedFP_EQ, boolean tFileTableInMemory, 
        boolean tAccessibleViaFiles, boolean tRemoveMVRows, 
        int tStandardizeWhat, int tNThreads, 
        String tCacheFromUrl, int tCacheSizeGB, String tCachePartialPathRegex) 
        throws Throwable {

        super("EDDTableFromWFSFiles", tDatasetID, 
            tAccessibleTo, tGraphsAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix, 
            tDefaultDataQuery, tDefaultGraphQuery,
            tAddGlobalAttributes, 
            tDataVariables, tReloadEveryNMinutes, tUpdateEveryNMillis,
            tFileDir, tFileNameRegex, tRecursive, tPathRegex, tMetadataFrom,
            tCharset, tColumnNamesRow, tFirstDataRow, tColumnSeparator,
            tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
            tSortedColumnSourceName, tSortFilesBySourceNames,
            tSourceNeedsExpandedFP_EQ, tFileTableInMemory, tAccessibleViaFiles,
            tRemoveMVRows, tStandardizeWhat, 
            tNThreads, null, -1, null); //tCacheFromUrl, tCacheSizeGB, tCachePartialPathRegex);
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
     * @return error string ("" if no error)
     */
    public static String downloadData(String tSourceUrl, String tRowElementXPath,
        String fullFileName) {

        try {
            if (verbose) String2.log("EDDTableFromWFSFiles.downloadData");
            if (tRowElementXPath == null || tRowElementXPath.length() == 0)
                tRowElementXPath = DefaultRowElementXPath;

            //read the table
            Table table = new Table();
            InputStream is = SSR.getUrlBufferedInputStream(tSourceUrl); 
            BufferedReader in = new BufferedReader(new InputStreamReader(is, String2.UTF_8));
            table.readXml(in, false, //no validate since no .dtd
                tRowElementXPath, 
                null, false);  //row attributes,  simplify=false

            //save as UTF-8 ASCII TSV file
            //This writes to temp file first and throws Exception if trouble.
            table.saveAsTabbedASCII(fullFileName, String2.UTF_8); 
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
     *   See https://webhelp.esri.com/arcims/9.2/general/mergedProjects/wfs_connect/wfs_connector/get_feature.htm
     *   See (was) https://support.esri.com/arcims/9.2/general/mergedProjects/wfs_connect/wfs_connector/get_feature.htm
     *   For example, this GetCapabilities
     *     http://services.azgs.az.gov/ArcGIS/services/aasggeothermal/COBoreholeTemperatures/MapServer/WFSServer?
     *     request=GetCapabilities&service=WFS
     *   lead to this tSourceUrl
     *     https://kgs.uky.edu/usgin/services/aasggeothermal/WVBoreholeTemperatures/MapServer/WFSServer?
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
        int tStandardizeWhat, 
        Attributes externalAddGlobalAttributes) throws Throwable {

        String2.log("\n*** EDDTableFromWFSFiles.generateDatasetsXml" +
            "\nsourceUrl=" + tSourceUrl +
            "\nrowElementXPath=" + tRowElementXPath +
            " reloadEveryNMinutes=" + tReloadEveryNMinutes +
            "\ninfoUrl=" + tInfoUrl + 
            "\ninstitution=" + tInstitution +
            "\nsummary=" + tSummary +
            "\ntitle=" + tTitle +
            "\nexternalAddGlobalAttributes=" + externalAddGlobalAttributes);
        if (!String2.isSomething(tSourceUrl))
            throw new IllegalArgumentException("sourceUrl wasn't specified.");
        if (tReloadEveryNMinutes <= 0 || tReloadEveryNMinutes == Integer.MAX_VALUE)
            tReloadEveryNMinutes = 1440; //1440 works well with suggestedUpdateEveryNMillis

        //*** basically, make a table to hold the sourceAttributes 
        //and a parallel table to hold the addAttributes
        Table dataSourceTable = new Table();
        Table dataAddTable = new Table();

        if (tRowElementXPath == null || tRowElementXPath.length() == 0)
            tRowElementXPath = DefaultRowElementXPath;

        if (developmentMode) {
            dataSourceTable.readXml(new BufferedReader(new FileReader(
                "c:/programs/mapserver/WVBoreholeResponse.xml")), 
                false, tRowElementXPath, null, true); //simplify=true
        } else {
//            InputStream is = SSR.getUrlBufferedInputStream(tSourceUrl); 
//            BufferedReader in = new BufferedReader(new InputStreamReader(is, String2.UTF_8));
//            dataSourceTable.readXml(in, false, //no validate since no .dtd
//                tRowElementXPath, 
//                null, true);  //row attributes,  here, simplify to get suggested dataTypes
        }
        if (verbose)
            String2.log("Table.readXml parsed the source xml to this table:\n" +
                dataSourceTable.toString(5));

        tStandardizeWhat = tStandardizeWhat < 0 || tStandardizeWhat == Integer.MAX_VALUE?
            DEFAULT_STANDARDIZEWHAT : tStandardizeWhat;
        dataSourceTable.unpack(tStandardizeWhat);

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
            PrimitiveArray sourcePA = (PrimitiveArray)dataSourceTable.getColumn(col).clone();
            String timeUnits = "";
            if (sourcePA instanceof StringArray) {
                timeUnits = Calendar2.suggestDateTimeFormat((StringArray)sourcePA, false); //evenIfPurelyNumeric
                if (timeUnits.length() > 0) 
                    sourceAtts.set("units", timeUnits); //just temporarily to trick makeReadyToUse...
            }
            PrimitiveArray destPA = makeDestPAForGDX(sourcePA, sourceAtts); //possibly with temporary time units

            //make addAtts
            Attributes addAtts = makeReadyToUseAddVariableAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(), sourceAtts, null, colName, 
                destPA.elementClass() != String.class, //tryToAddStandardName
                destPA.elementClass() != String.class, //addColorBarMinMax
                true); //tryToFindLLAT

            //put time units
            if (timeUnits.length() > 0) {
                sourceAtts.remove("units"); //undo above
                addAtts.set("units", timeUnits); //correct place
            }

            //add to dataAddTable
            dataAddTable.addColumn(col, colName, destPA, addAtts);                

            //files are likely sorted by first date time variable
            //and no harm if files aren't sorted that way
            if (tSortedColumnSourceName.length() == 0 && timeUnits.length() > 0) 
                tSortedColumnSourceName = colName;

            //add missing_value and/or _FillValue if needed
            addMvFvAttsIfNeeded(colName, destPA, sourceAtts, addAtts);

        }

        //tryToFindLLAT
        tryToFindLLAT(dataSourceTable, dataAddTable);

        //after dataVariables known, add global attributes in the dataAddTable
        String tDatasetID = suggestDatasetID(tSourceUrl);
        dataAddTable.globalAttributes().set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(), 
                //another cdm_data_type could be better; this is good for now
                hasLonLatTime(dataAddTable)? "Point" : "Other",
                EDStatic.fullCopyDirectory + tDatasetID + "/", 
                externalAddGlobalAttributes, 
                suggestKeywords(dataSourceTable, dataAddTable)));
        dataAddTable.globalAttributes().set("rowElementXPath", tRowElementXPath);

        //subsetVariables
        if (dataSourceTable.globalAttributes().getString("subsetVariables") == null &&
               dataAddTable.globalAttributes().getString("subsetVariables") == null) 
            dataAddTable.globalAttributes().add("subsetVariables",
                suggestSubsetVariables(dataSourceTable, dataAddTable, false));

        //write the information
        StringBuilder sb = new StringBuilder();
        sb.append(
            "<!-- NOTE! Since the source files don't have any metadata, you must add metadata\n" +
            "  below, notably 'units' for each of the dataVariables. -->\n" +
            "<dataset type=\"EDDTableFromWFSFiles\" datasetID=\"" + tDatasetID +
                "\" active=\"true\">\n" +
            "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n" +  
            "    <updateEveryNMillis>0</updateEveryNMillis>\n" +  //files are only added by full reload
            //"    <fileNameRegex>.*\\.tsv</fileNameRegex>\n" + //irrelevant/overridden
            //"    <recursive>false</recursive>\n" +            //irrelevant/overridden 
            //"    <pathRegex>.*</pathRegex>\n" +               //irrelevant/overridden
            //"    <fileDir>" + EDStatic.fullCopyDirectory + tDatasetID + "/</fileDir>\n" +
            "    <metadataFrom>last</metadataFrom>\n" +
            "    <standardizeWhat>" + tStandardizeWhat + "</standardizeWhat>\n" +
            "    <fileTableInMemory>false</fileTableInMemory>\n" +
            "    <accessibleViaFiles>false</accessibleViaFiles>\n");
            //"    <charset>UTF-8</charset>\n" +
            //"    <columnNamesRow>1</columnNamesRow>\n" +
            //"    <firstDataRow>3</firstDataRow>\n" +
            //(String2.isSomething(tColumnNameForExtract)? //Discourage Extract. Encourage sourceName=***fileName,...
            //  "    <preExtractRegex>" + XML.encodeAsXML(tPreExtractRegex) + "</preExtractRegex>\n" +
            //  "    <postExtractRegex>" + XML.encodeAsXML(tPostExtractRegex) + "</postExtractRegex>\n" +
            //  "    <extractRegex>" + XML.encodeAsXML(tExtractRegex) + "</extractRegex>\n" +
            //  "    <columnNameForExtract>" + tColumnNameForExtract + "</columnNameForExtract>\n" : "") +
            //"    <sortedColumnSourceName>" + tSortedColumnSourceName + "</sortedColumnSourceName>\n" +
            //"    <sortFilesBySourceNames>" + tSortFilesBySourceNames + "</sortFilesBySourceNames>\n");
        sb.append(writeAttsForDatasetsXml(false, dataSourceTable.globalAttributes(), "    "));
        sb.append(cdmSuggestion());
        sb.append(writeAttsForDatasetsXml(true,     dataAddTable.globalAttributes(), "    "));

        sb.append(writeVariablesForDatasetsXml(dataSourceTable, dataAddTable, 
            "dataVariable", 
            true, false)); //includeDataType, questionDestinationName
        sb.append(
            "</dataset>\n" +
            "\n");

        String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
        return sb.toString();
        
    }

    /**
     * testGenerateDatasetsXml
     * See response from URL at /data/wfs/wfs.xml
     */
    public static void testGenerateDatasetsXml(boolean tDevelopmentMode) throws Throwable {
        testVerboseOn();
        boolean oDevelopmentMode = developmentMode;
        developmentMode = tDevelopmentMode;
        try {

        Attributes externalAddAttributes = new Attributes();
        externalAddAttributes.add("title", "Old Title!");
        String results = generateDatasetsXml(
            "https://kgs.uky.edu/usgin/services/aasggeothermal/WVBoreholeTemperatures/MapServer/WFSServer?request=GetFeature&service=WFS&typename=aasg:BoreholeTemperature&format=\"text/xml;%20subType=gml/3.1.1/profiles/gmlsf/1.0.0/0\"", 
            "/wfs:FeatureCollection/wfs:member", //tRowElementXPath,
            DEFAULT_RELOAD_EVERY_N_MINUTES, 
            "https://www.uky.edu/KGS/", 
            "Kentucky Geological Survey", 
            "The summary.", 
            "The Title",
            -1, //defaultStandardizeWhat
            externalAddAttributes) + "\n";

String expected = 
"<!-- NOTE! Since the source files don't have any metadata, you must add metadata\n" +
"  below, notably 'units' for each of the dataVariables. -->\n" +
"<dataset type=\"EDDTableFromWFSFiles\" datasetID=\"uky_kgs_658a_9173_c8f0\" active=\"true\">\n" +
"    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>0</updateEveryNMillis>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <standardizeWhat>0</standardizeWhat>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <accessibleViaFiles>false</accessibleViaFiles>\n" +
"    <!-- sourceAttributes>\n" +
"    </sourceAttributes -->\n" +
"    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" +
"        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n" +
"        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n" +
"    -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Point</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"creator_name\">Kentucky Geological Survey</att>\n" +
"        <att name=\"creator_url\">https://www.uky.edu/KGS/</att>\n" +
"        <att name=\"infoUrl\">https://www.uky.edu/KGS/</att>\n" +
"        <att name=\"institution\">Kentucky Geological Survey</att>\n" +
"        <att name=\"keywords\">apino, bit, BitDiameterTD, bore, bottom, casing, CasingBottomDepthDriller, CasingPipeDiameter, CasingThickness, CasingTopDepth, CasingWeight, circulation, CirculationDuration, commodity, CommodityOfInterest, corrected, CorrectedTemperature, county, data, date, degree, depth, DepthOfMeasurement, DepthReferencePoint, diameter, driller, DrillerTotalDepth, drilling, duration, elevation, ElevationDF, ElevationGL, ElevationKB, ended, EndedDrillingDate, field, formation, FormationTD, function, geological, header, HeaderURI, information, InformationSource, interest, interval, kentucky, label, latitude, lease, LeaseName, LeaseNo, LeaseOwner, length, LengthUnits, LocationUncertaintyRadius, LocationUncertaintyStatement, long, longitude, maximum, MaximumRecordedTemperature, measured, MeasuredTemperature, measurement, MeasurementDateTime, MeasurementFormation, MeasurementProcedure, MeasurementSource, name, notes, observation, ObservationURI, operator, OtherName, owner, pipe, point, procedure, producing, ProducingInterval, production, quality, radius, recorded, reference, related, RelatedResource, release, ReleaseDate, resource, salinity, shape, Shape_gml_Point_latitude, Shape_gml_Point_longitude, since, source, spud, srs, state, statement, status, StatusDate, survey, temperature, TemperatureUnits, thickness, time, TimeSinceCirculation, title, top, total, true, TrueVerticalDepth, type, uncertainty, units, utm, UTM_E, UTM_N, vertical, weight, well, WellBoreShape, WellName, WellType</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"rowElementXPath\">/wfs:FeatureCollection/wfs:member</att>\n" +
"        <att name=\"sourceUrl\">https://kgs.uky.edu/usgin/services/aasggeothermal/WVBoreholeTemperatures/MapServer/WFSServer?request=GetFeature&amp;service=WFS&amp;typename=aasg:BoreholeTemperature&amp;format=&quot;text/xml;&#37;20subType=gml/3.1.1/profiles/gmlsf/1.0.0/0&quot;</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v55</att>\n" +
"        <att name=\"subsetVariables\">State, UTM_E, UTM_N, SRS, LocationUncertaintyRadius, LengthUnits, ElevationKB, ElevationDF, BitDiameterTD, MaximumRecordedTemperature, CorrectedTemperature, TemperatureUnits, CirculationDuration, MeasurementSource, CasingBottomDepthDriller, CasingTopDepth, CasingPipeDiameter, CasingWeight, CasingThickness, pH</att>\n" +
"        <att name=\"summary\">The summary. Kentucky Geological Survey data from a local source.</att>\n" +
"        <att name=\"title\">The Title</att>\n" +
"    </addAttributes>\n" +
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
"            <att name=\"source_name\">aasg:BoreholeTemperature/aasg:SpudDate</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"time_precision\">1970-01-01T00:00:00Z</att>\n" +
"            <att name=\"units\">yyyy-MM-dd&#39;T&#39;HH:mm:ss</att>\n" +
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
"            <att name=\"time_precision\">1970-01-01T00:00:00Z</att>\n" +
"            <att name=\"units\">yyyy-MM-dd&#39;T&#39;HH:mm:ss</att>\n" +
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
"        <sourceName>aasg:BoreholeTemperature/aasg:StatusDate</sourceName>\n" +
"        <destinationName>StatusDate</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Status Date</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"time_precision\">1970-01-01T00:00:00Z</att>\n" +
"            <att name=\"units\">yyyy-MM-dd&#39;T&#39;HH:mm:ss</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:ReleaseDate</sourceName>\n" +
"        <destinationName>ReleaseDate</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Release Date</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"time_precision\">1970-01-01T00:00:00Z</att>\n" +
"            <att name=\"units\">yyyy-MM-dd&#39;T&#39;HH:mm:ss</att>\n" +
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
"        <sourceName>aasg:BoreholeTemperature/aasg:UTM_E</sourceName>\n" +
"        <destinationName>UTM_E</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">UTM E</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:UTM_N</sourceName>\n" +
"        <destinationName>UTM_N</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">UTM N</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:LatDegree</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Lat Degree</att>\n" +
"            <att name=\"source_name\">aasg:BoreholeTemperature/aasg:LatDegree</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:LongDegree</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Long Degree</att>\n" +
"            <att name=\"source_name\">aasg:BoreholeTemperature/aasg:LongDegree</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
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
"        <sourceName>aasg:BoreholeTemperature/aasg:LocationUncertaintyRadius</sourceName>\n" +
"        <destinationName>LocationUncertaintyRadius</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"            <att name=\"long_name\">Location Uncertainty Radius</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:DrillerTotalDepth</sourceName>\n" +
"        <destinationName>DrillerTotalDepth</destinationName>\n" +
"        <dataType>float</dataType>\n" +
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
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">True Vertical Depth</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:ElevationKB</sourceName>\n" +
"        <destinationName>ElevationKB</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Elevation KB</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:ElevationDF</sourceName>\n" +
"        <destinationName>ElevationDF</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Elevation DF</att>\n" +
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
"        <sourceName>aasg:BoreholeTemperature/aasg:BitDiameterTD</sourceName>\n" +
"        <destinationName>BitDiameterTD</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Bit Diameter TD</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:MaximumRecordedTemperature</sourceName>\n" +
"        <destinationName>MaximumRecordedTemperature</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Maximum Recorded Temperature</att>\n" +
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
"        <sourceName>aasg:BoreholeTemperature/aasg:CorrectedTemperature</sourceName>\n" +
"        <destinationName>CorrectedTemperature</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Corrected Temperature</att>\n" +
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
"        <sourceName>aasg:BoreholeTemperature/aasg:CirculationDuration</sourceName>\n" +
"        <destinationName>CirculationDuration</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Circulation Duration</att>\n" +
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
"        <dataType>int</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Depth Of Measurement</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:MeasurementDateTime</sourceName>\n" +
"        <destinationName>MeasurementDateTime</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Measurement Date Time</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"time_precision\">1970-01-01T00:00:00Z</att>\n" +
"            <att name=\"units\">yyyy-MM-dd&#39;T&#39;HH:mm:ss</att>\n" +
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
"        <sourceName>aasg:BoreholeTemperature/aasg:CasingBottomDepthDriller</sourceName>\n" +
"        <destinationName>CasingBottomDepthDriller</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Casing Bottom Depth Driller</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:CasingTopDepth</sourceName>\n" +
"        <destinationName>CasingTopDepth</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Casing Top Depth</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:CasingPipeDiameter</sourceName>\n" +
"        <destinationName>CasingPipeDiameter</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Casing Pipe Diameter</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:CasingWeight</sourceName>\n" +
"        <destinationName>CasingWeight</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Casing Weight</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:CasingThickness</sourceName>\n" +
"        <destinationName>CasingThickness</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Casing Thickness</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:pH</sourceName>\n" +
"        <destinationName>pH</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Salinity</att>\n" +
"            <att name=\"long_name\">pH</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:InformationSource</sourceName>\n" +
"        <destinationName>InformationSource</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Information Source</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:Shape/gml:Point/latitude</sourceName>\n" +
"        <destinationName>Shape_gml_Point_latitude</destinationName>\n" +
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
"        <destinationName>Shape_gml_Point_longitude</destinationName>\n" +
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
"        <sourceName>aasg:BoreholeTemperature/aasg:LeaseOwner</sourceName>\n" +
"        <destinationName>LeaseOwner</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Lease Owner</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:LeaseNo</sourceName>\n" +
"        <destinationName>LeaseNo</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Lease No</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>aasg:BoreholeTemperature/aasg:TimeSinceCirculation</sourceName>\n" +
"        <destinationName>TimeSinceCirculation</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"_FillValue\" type=\"short\">32767</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Time Since Circulation</att>\n" +
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
"</dataset>\n\n\n";

            Test.ensureEqual(results, expected, "results=\n" + results);
            //Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), 
            //    expected, "");

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDTableFromWFSFiles",
            "https://kgs.uky.edu/usgin/services/aasggeothermal/WVBoreholeTemperatures/MapServer/WFSServer?request=GetFeature&service=WFS&typename=aasg:BoreholeTemperature&format=\"text/xml;%20subType=gml/3.1.1/profiles/gmlsf/1.0.0/0\"", 
            "/wfs:FeatureCollection/wfs:member", //tRowElementXPath,
            "" + DEFAULT_RELOAD_EVERY_N_MINUTES, 
            "https://www.uky.edu/KGS/", 
            "Kentucky Geological Survey", 
            "The summary.", 
            "The Title", 
            "-1"}, //defaultStandardizeWhat
            false); //doIt loop?
        Test.ensureEqual(gdxResults, expected, "Unexpected results from GenerateDatasetsXml.doIt.");


            //ensure it is ready-to-use by making a dataset from it
            String tDatasetID = "uky_kgs_658a_9173_c8f0";
            EDD.deleteCachedDatasetInfo(tDatasetID);
            EDD edd = oneFromXmlFragment(null, results);
            //these won't actually be tested...
            Test.ensureEqual(edd.datasetID(), tDatasetID, "");
            Test.ensureEqual(edd.title(), "The Title", "");
            Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
"ObservationURI, WellName, APINo, HeaderURI, OtherName, Label, Operator, time, " +
"EndedDrillingDate, WellType, StatusDate, ReleaseDate, Field, County, State, " +
"UTM_E, UTM_N, latitude, longitude, SRS, LocationUncertaintyStatement, LocationUncertaintyRadius, " +
"DrillerTotalDepth, DepthReferencePoint, LengthUnits, WellBoreShape, TrueVerticalDepth, ElevationKB, " +
"ElevationDF, ElevationGL, FormationTD, BitDiameterTD, MaximumRecordedTemperature, MeasuredTemperature, " +
"CorrectedTemperature, TemperatureUnits, CirculationDuration, MeasurementProcedure, DepthOfMeasurement, " +
"MeasurementDateTime, MeasurementFormation, MeasurementSource, RelatedResource, CasingBottomDepthDriller, " +
"CasingTopDepth, CasingPipeDiameter, CasingWeight, CasingThickness, pH, InformationSource, " +
"Shape_gml_Point_latitude, Shape_gml_Point_longitude, LeaseName, LeaseOwner, LeaseNo, " +
"TimeSinceCirculation, Status, CommodityOfInterest, Function, Production, ProducingInterval, Notes", "");


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
            String2.pressEnterToContinue(MustBe.throwableToString(t)); 
        }
        developmentMode = oDevelopmentMode;

    }


    /**
     * Basic tests of this class.
     */
    public static void testBasic() throws Throwable {

        String2.log("\n*** EDDTableFromWFSFiles.testBasic");
        try {

        EDDTable tedd = (EDDTable)oneFromDatasetsXml(null, "kgsBoreTempWVTRUE"); //should work
        String tName, error, results, tResults, expected;
        int po;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

        //*** .das
        tName = tedd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            "kgsBoreTempWVTRUE", ".das"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected =   
"Attributes {\n" +
" s {\n" +
"  ObservationURI {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Observation URI\";\n" +
"  }\n" +
"  WellName {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Well Name\";\n" +
"  }\n" +
"  APINo {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"APINo\";\n" +
"  }\n" +
"  HeaderURI {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Header URI\";\n" +
"  }\n" +
"  OtherName {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Other Name\";\n" +
"  }\n" +
"  Label {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Label\";\n" +
"  }\n" +
"  Operator {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Operator\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range -2.2379328e+9, 1.3441248e+9;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Spud Date\";\n" +
"    String source_name \"aasg:BoreholeTemperature/aasg:SpudDate\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String time_precision \"1970-01-01T00:00:00Z\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  EndedDrillingDate {\n" +
"    Float64 actual_range -2.5245216e+9, 1.3554432e+9;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Ended Drilling Date\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String time_precision \"1970-01-01T00:00:00Z\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  WellType {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Well Type\";\n" +
"  }\n" +
"  StatusDate {\n" +
"    Float64 actual_range -2.2089888e+9, -2.2089888e+9;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Status Date\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String time_precision \"1970-01-01T00:00:00Z\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  ReleaseDate {\n" +
"    Float64 actual_range -2.2089888e+9, -2.2089888e+9;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Release Date\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String time_precision \"1970-01-01T00:00:00Z\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  Field {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Field\";\n" +
"  }\n" +
"  County {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"County\";\n" +
"  }\n" +
"  State {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"State\";\n" +
"  }\n" +
"  UTM_E {\n" +
"    Float32 actual_range 0.0, 0.0;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"UTM E\";\n" +
"  }\n" +
"  UTM_N {\n" +
"    Float32 actual_range 0.0, 0.0;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"UTM N\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range 37.246728999, 39.982674999;\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Lat Degree\";\n" +
"    String source_name \"aasg:BoreholeTemperature/aasg:LatDegree\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -82.549986, -78.803193;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Long Degree\";\n" +
"    String source_name \"aasg:BoreholeTemperature/aasg:LongDegree\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  SRS {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"SRS\";\n" +
"  }\n" +
"  LocationUncertaintyStatement {\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Location Uncertainty Statement\";\n" +
"  }\n" +
"  LocationUncertaintyRadius {\n" +
"    Float32 actual_range 0.0, 0.0;\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Location Uncertainty Radius\";\n" +
"  }\n" +
"  DrillerTotalDepth {\n" +
"    Float32 actual_range 0.0, 12996.0;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Driller Total Depth\";\n" +
"  }\n" +
"  DepthReferencePoint {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Depth Reference Point\";\n" +
"  }\n" +
"  LengthUnits {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Length Units\";\n" +
"  }\n" +
"  WellBoreShape {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Well Bore Shape\";\n" +
"  }\n" +
"  TrueVerticalDepth {\n" +
"    Float32 actual_range 0.0, 11792.0;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"True Vertical Depth\";\n" +
"  }\n" +
"  ElevationKB {\n" +
"    Float32 actual_range 0.0, 0.0;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Elevation KB\";\n" +
"  }\n" +
"  ElevationDF {\n" +
"    Float32 actual_range 0.0, 0.0;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Elevation DF\";\n" +
"  }\n" +
"  ElevationGL {\n" +
"    Int16 actual_range 568, 4333;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Elevation GL\";\n" +
"  }\n" +
"  FormationTD {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Formation TD\";\n" +
"  }\n" +
"  BitDiameterTD {\n" +
"    Float32 actual_range 0.0, 0.0;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Bit Diameter TD\";\n" +
"  }\n" +
"  MaximumRecordedTemperature {\n" +
"    Float32 actual_range 0.0, 0.0;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Maximum Recorded Temperature\";\n" +
"  }\n" +
"  MeasuredTemperature {\n" +
"    Float32 actual_range 8.0, 908.0;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Measured Temperature\";\n" +
"  }\n" +
"  CorrectedTemperature {\n" +
"    Float32 actual_range 0.0, 0.0;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Corrected Temperature\";\n" +
"  }\n" +
"  TemperatureUnits {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Temperature Units\";\n" +
"  }\n" +
"  CirculationDuration {\n" +
"    Float32 actual_range 0.0, 0.0;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Circulation Duration\";\n" +
"  }\n" +
"  MeasurementProcedure {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Measurement Procedure\";\n" +
"  }\n" +
"  DepthOfMeasurement {\n" +
"    Int32 actual_range 23, 36885;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Depth Of Measurement\";\n" +
"  }\n" +
"  MeasurementDateTime {\n" +
"    Float64 actual_range -2.2089888e+9, 1.325376e+9;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Measurement Date Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String time_precision \"1970-01-01T00:00:00Z\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  MeasurementFormation {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Measurement Formation\";\n" +
"  }\n" +
"  MeasurementSource {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Measurement Source\";\n" +
"  }\n" +
"  RelatedResource {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Related Resource\";\n" +
"  }\n" +
"  CasingBottomDepthDriller {\n" +
"    Float32 actual_range 0.0, 0.0;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Casing Bottom Depth Driller\";\n" +
"  }\n" +
"  CasingTopDepth {\n" +
"    Float32 actual_range 0.0, 0.0;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Casing Top Depth\";\n" +
"  }\n" +
"  CasingPipeDiameter {\n" +
"    Float32 actual_range 0.0, 0.0;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Casing Pipe Diameter\";\n" +
"  }\n" +
"  CasingWeight {\n" +
"    Float32 actual_range 0.0, 0.0;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Casing Weight\";\n" +
"  }\n" +
"  CasingThickness {\n" +
"    Float32 actual_range 0.0, 0.0;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Casing Thickness\";\n" +
"  }\n" +
"  pH {\n" +
"    Float32 actual_range 0.0, 0.0;\n" +
"    String ioos_category \"Salinity\";\n" +
"    String long_name \"pH\";\n" +
"  }\n" +
"  InformationSource {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Information Source\";\n" +
"  }\n" +
"  Shape_gml_Point_latitude {\n" +
"    Float64 actual_range 37.24672899900008, 39.98267499900004;\n" +
"    Float64 colorBarMaximum 90.0;\n" +
"    Float64 colorBarMinimum -90.0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  Shape_gml_Point_longitude {\n" +
"    Float64 actual_range -82.54998599999993, -78.80319299999991;\n" +
"    Float64 colorBarMaximum 180.0;\n" +
"    Float64 colorBarMinimum -180.0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  LeaseName {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Lease Name\";\n" +
"  }\n" +
"  LeaseOwner {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Lease Owner\";\n" +
"  }\n" +
"  LeaseNo {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Lease No\";\n" +
"  }\n" +
"  TimeSinceCirculation {\n" +
"    Int16 actual_range 2, 2301;\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Time Since Circulation\";\n" +
"  }\n" +
"  Status {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Status\";\n" +
"  }\n" +
"  CommodityOfInterest {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Commodity Of Interest\";\n" +
"  }\n" +
"  Function {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Function\";\n" +
"  }\n" +
"  Production {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Production\";\n" +
"  }\n" +
"  ProducingInterval {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Producing Interval\";\n" +
"  }\n" +
"  Notes {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Notes\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"Point\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String creator_name \"Kentucky Geological Survey\";\n" +
"    String creator_url \"https://www.uky.edu/KGS/\";\n" +
"    Float64 Easternmost_Easting -78.803193;\n" +
"    String featureType \"Point\";\n" +
"    Float64 geospatial_lat_max 39.982674999;\n" +
"    Float64 geospatial_lat_min 37.246728999;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max -78.803193;\n" +
"    Float64 geospatial_lon_min -82.549986;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String history \"" + today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

expected = 
    "String infoUrl \"https://www.uky.edu/KGS/\";\n" +
"    String institution \"Kentucky Geological Survey\";\n" +
"    String keywords \"apino, bit, BitDiameterTD, bore, bottom, casing, CasingBottomDepthDriller, CasingPipeDiameter, CasingThickness, CasingTopDepth, CasingWeight, circulation, CirculationDuration, commodity, CommodityOfInterest, corrected, CorrectedTemperature, county, data, date, degree, depth, DepthOfMeasurement, DepthReferencePoint, diameter, driller, DrillerTotalDepth, drilling, duration, elevation, ElevationDF, ElevationGL, ElevationKB, ended, EndedDrillingDate, field, formation, FormationTD, function, geological, header, HeaderURI, information, InformationSource, interest, interval, kentucky, label, latitude, lease, LeaseName, LeaseNo, LeaseOwner, length, LengthUnits, LocationUncertaintyRadius, LocationUncertaintyStatement, long, longitude, maximum, MaximumRecordedTemperature, measured, MeasuredTemperature, measurement, MeasurementDateTime, MeasurementFormation, MeasurementProcedure, MeasurementSource, name, notes, observation, ObservationURI, operator, OtherName, owner, pipe, point, procedure, producing, ProducingInterval, production, quality, radius, recorded, reference, related, RelatedResource, release, ReleaseDate, resource, salinity, shape, Shape_gml_Point_latitude, Shape_gml_Point_longitude, since, source, spud, srs, state, statement, status, StatusDate, survey, temperature, TemperatureUnits, thickness, time, TimeSinceCirculation, title, top, total, true, TrueVerticalDepth, type, uncertainty, units, utm, UTM_E, UTM_N, vertical, weight, well, WellBoreShape, WellName, WellType\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    Float64 Northernmost_Northing 39.982674999;\n" +
"    String rowElementXPath \"/wfs:FeatureCollection/wfs:member\";\n" +
"    String sourceUrl \"https://kgs.uky.edu/usgin/services/aasggeothermal/WVBoreholeTemperatures/MapServer/WFSServer?request=GetFeature&service=WFS&typename=aasg:BoreholeTemperature&format=\\\"text/xml;%20subType=gml/3.1.1/profiles/gmlsf/1.0.0/0\\\"\";\n" +
"    Float64 Southernmost_Northing 37.246728999;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v55\";\n" +
"    String subsetVariables \"State, UTM_E, UTM_N, SRS, LocationUncertaintyRadius, LengthUnits, ElevationKB, ElevationDF, BitDiameterTD, MaximumRecordedTemperature, CorrectedTemperature, TemperatureUnits, CirculationDuration, MeasurementSource, CasingBottomDepthDriller, CasingTopDepth, CasingPipeDiameter, CasingWeight, CasingThickness, pH\";\n" +
"    String summary \"The summary. Kentucky Geological Survey data from a local source.\";\n" +
"    String time_coverage_end \"2012-08-05T00:00:00Z\";\n" +
"    String time_coverage_start \"1899-01-31T00:00:00Z\";\n" +
"    String title \"The Title\";\n" +
"    Float64 Westernmost_Easting -82.549986;\n" +
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
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String ObservationURI;\n" +
"    String WellName;\n" +
"    String APINo;\n" +
"    String HeaderURI;\n" +
"    String OtherName;\n" +
"    String Label;\n" +
"    String Operator;\n" +
"    Float64 time;\n" +
"    Float64 EndedDrillingDate;\n" +
"    String WellType;\n" +
"    Float64 StatusDate;\n" +
"    Float64 ReleaseDate;\n" +
"    String Field;\n" +
"    String County;\n" +
"    String State;\n" +
"    Float32 UTM_E;\n" +
"    Float32 UTM_N;\n" +
"    Float64 latitude;\n" +
"    Float64 longitude;\n" +
"    String SRS;\n" +
"    String LocationUncertaintyStatement;\n" +
"    Float32 LocationUncertaintyRadius;\n" +
"    Float32 DrillerTotalDepth;\n" +
"    String DepthReferencePoint;\n" +
"    String LengthUnits;\n" +
"    String WellBoreShape;\n" +
"    Float32 TrueVerticalDepth;\n" +
"    Float32 ElevationKB;\n" +
"    Float32 ElevationDF;\n" +
"    Int16 ElevationGL;\n" +
"    String FormationTD;\n" +
"    Float32 BitDiameterTD;\n" +
"    Float32 MaximumRecordedTemperature;\n" +
"    Float32 MeasuredTemperature;\n" +
"    Float32 CorrectedTemperature;\n" +
"    String TemperatureUnits;\n" +
"    Float32 CirculationDuration;\n" +
"    String MeasurementProcedure;\n" +
"    Int32 DepthOfMeasurement;\n" +
"    Float64 MeasurementDateTime;\n" +
"    String MeasurementFormation;\n" +
"    String MeasurementSource;\n" +
"    String RelatedResource;\n" +
"    Float32 CasingBottomDepthDriller;\n" +
"    Float32 CasingTopDepth;\n" +
"    Float32 CasingPipeDiameter;\n" +
"    Float32 CasingWeight;\n" +
"    Float32 CasingThickness;\n" +
"    Float32 pH;\n" +
"    String InformationSource;\n" +
"    Float64 Shape_gml_Point_latitude;\n" +
"    Float64 Shape_gml_Point_longitude;\n" +
"    String LeaseName;\n" +
"    String LeaseOwner;\n" +
"    String LeaseNo;\n" +
"    Int16 TimeSinceCirculation;\n" +
"    String Status;\n" +
"    String CommodityOfInterest;\n" +
"    String Function;\n" +
"    String Production;\n" +
"    String ProducingInterval;\n" +
"    String Notes;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        tName = tedd.makeNewFileForDapQuery(null, null, 
            "&APINo=\"4700102422\"", 
            EDStatic.fullTestCacheDirectory, "kgsBoreTempWVTRUE", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"ObservationURI,WellName,APINo,HeaderURI,OtherName,Label,Operator,time,EndedDrillingDate,WellType,StatusDate,ReleaseDate,Field,County,State,UTM_E,UTM_N,latitude,longitude,SRS,LocationUncertaintyStatement,LocationUncertaintyRadius,DrillerTotalDepth,DepthReferencePoint,LengthUnits,WellBoreShape,TrueVerticalDepth,ElevationKB,ElevationDF,ElevationGL,FormationTD,BitDiameterTD,MaximumRecordedTemperature,MeasuredTemperature,CorrectedTemperature,TemperatureUnits,CirculationDuration,MeasurementProcedure,DepthOfMeasurement,MeasurementDateTime,MeasurementFormation,MeasurementSource,RelatedResource,CasingBottomDepthDriller,CasingTopDepth,CasingPipeDiameter,CasingWeight,CasingThickness,pH,InformationSource,Shape_gml_Point_latitude,Shape_gml_Point_longitude,LeaseName,LeaseOwner,LeaseNo,TimeSinceCirculation,Status,CommodityOfInterest,Function,Production,ProducingInterval,Notes\n" +
",,,,,,,UTC,UTC,,UTC,UTC,,,,,,degrees_north,degrees_east,,,,,,,,,,,,,,,,,,,,,UTC,,,,,,,,,,,degrees_north,degrees_east,,,,,,,,,,\n" +
"http://resources.usgin.org/uri-gin/wvges/bhtemp/4700102422_105/,\"Fuel Resources, Inc.  Zona Bernard 2\",4700102422,http://resources.usgin.org/uri-gin/wvges/well/api:4700102422/,Fuel Resources Inc,4700102422,\"Fuel Resources, Inc.\",1989-03-15T00:00:00Z,1989-03-21T00:00:00Z,Gas,,,Belington,Barbour,West Virginia,0.0,0.0,38.989951999,-79.964635,EPSG:4326,Location recorded as received from official permit application converted to NAD83 if required,0.0,5479.0,G.L.,ft,vertical,5479.0,0.0,0.0,2028,Fox,0.0,0.0,105.0,0.0,F,0.0,Temperature log evaluated by WVGES staff for deepest stable log segment to extract data otherwise used given bottom hole temperature on log header if available,4650,,Elk,Well Temperature Log,TL | GR | DEN | IL | CAL,0.0,0.0,0.0,0.0,0.0,0.0,,38.98995199900008,-79.96463499999993,,,,5,Missing,Missing,Missing,Missing,Missing,\n";

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
        String2.log("\n*** EDDTableFromWFSFiles.test()");

/* for releases, this line should have open/close comment */
        testGenerateDatasetsXml(true);  //developmentMode (read from local file)
        testBasic();
    }
}

