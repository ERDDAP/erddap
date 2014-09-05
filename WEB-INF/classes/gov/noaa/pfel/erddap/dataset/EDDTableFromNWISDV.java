/* 
 * EDDTableFromNWISDV Copyright 2010, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
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

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
//import java.util.GregorianCalendar;


/** 
 * THIS WAS WORKING. BUT SERVICE CHANGED.  SINCE LITTLE/NO DEMAND, STOP SUPPORTING IT 2011-12-16.
 * This class aggregates data from a group of fixed-location sites (stations), all served by
 * NWIS Daily Values Service.
 * (It can't support all WaterML servers because they use different REST 
 * interfaces to get the data.)
 * The sites all serve the same "variable"
 * (the phenomena that was measured)
 * (which has several associated "values" (what ERDDAP calls variables)).
 * Different sites for a given dataset all serve a specific set of values.
 *
 * <p>WaterML is from CUAHSI, a consortium of universities, focused on hydrology. 
 * <p>Web page: http://his.cuahsi.org/wofws.html .
 *   <br>API  info http://water.sdsc.edu
 *   <br>http://river.sdsc.edu/wiki/Default.aspx?Page=WaterML&NS=&AspxAutoDetectCookieSupport=1
 *   <br>Schema: http://his.cuahsi.org/documents/cuahsiTimeSeries_v1_0.xsd
 *   <br>WaterML is very similar to an OGC standard. They're working to make it so.
 *      OGC Discussion Paper: See CUASHI WaterML at http://www.opengeospatial.org/standards/dp
 *   <br>Programming advice: http://waterservices.usgs.gov/docs/portable_code.html
 * <p>WaterML services:
 *   <ul>
 *   <li>Get a list of sites -- No service!  Bob got Daily Value stations from David Briar.
 *   <li>GetSiteInfo -- No service?  Bob got site info by screen scraping NWIS web pages.
 *   <li>GetValues -- get actual data for one site (agency+siteCode) + one paramCode + 
 *      one statisticsCode at a time.  
 *      The response is a stand-alone XML document with limited station information.
 *   </ul>
 * <p>Other URLs 
 * <ul>
 * <li>USGS Daily Values http://waterservices.usgs.gov/rest/USGS-DV-Service.html
 * <li>Statistics codes http://river.sdsc.edu/Wiki/NWIS%20Statistics%20codes%20stat_cd.ashx
 *   <br>and http://waterservices.usgs.gov/rest/USGS-DV-Service.html#Stats
 * <li>Agency codes http://nwis.waterdata.usgs.gov/nwis/help/?read_file=nwis_agency_codes&format=table
 * <li>Hydrologic Unit Codes (HUC) http://nwis.waterdata.usgs.gov/tutorial/huc_def.html
 * <li>Site info web page e.g., http://waterdata.usgs.gov/nwis/inventory?agency_code=USGS&site_no=07243500
 * <li>Request Surface Water daily values: http://waterdata.usgs.gov/nwis/dv/?referred_module=sw
 * </ul>
 *
 * <p>This class insists that each site is indeed a site at one fixed lat lon point.
 *    
 * @author Bob Simons (bob.simons@noaa.gov) 2010-11-24
 */
public class EDDTableFromNWISDV extends EDDTable{ 

    /** column numbers in stationTable.
     * Source must be [tomcat]/content/erddap/subset/[datasetID].json.
     * Constructor creates .nc version in datasetDir()/stationFileName) 
     */
    final protected static String stationTableColumnNames[] = {
        "agencySiteCode", "agency", "siteCode", "siteID", "siteName", 
        "siteType", "network", "longitude", "latitude", "srs", 
        "altitude", "vertDatum", "county", "state", "country", 
        "beginDate", "endDate"};
    final protected static int stationTableColAgencySiteCode = 0;
    final protected static int stationTableColAgency = 1;
    final protected static int stationTableColSiteCode = 2; 
    final protected static int stationTableColSiteID = 3; 
    final protected static int stationTableColSiteName = 4;
    final protected static int stationTableColSiteType = 5;
    final protected static int stationTableColNetwork = 6;
    final protected static int stationTableColLongitude = 7; 
    final protected static int stationTableColLatitude = 8; 
    final protected static int stationTableColSrs = 9;
    final protected static int stationTableColAltitude = 10;
    final protected static int stationTableColVertDatum = 11;
    final protected static int stationTableColCounty = 12;
    final protected static int stationTableColState = 13;
    final protected static int stationTableColCountry = 14;
    final protected static int stationTableColBeginDate = 15;
    final protected static int stationTableColEndDate = 16;

    //set by constructor
    protected String parameterCode, statisticCode;

    /** Lowercase NWIS statistic names for codes 0 - 24 (higher numbers not included here)
     * from  http://river.sdsc.edu/Wiki/NWIS%20Statistics%20codes%20stat_cd.ashx  2011-01-21.
     */
    public static String[] NWISStatisticNames = {
        "", "maximum", "minimum", "mean", "am",    //0..
        "pm", "sum",   "mode",    "median", "std", //5..
        "variance", "instantaneous", "equivalent mean", "skewness", "", //10..
        "", "", "", "", "", //15..
        "", "tidal high-high", "tidal low-high", "tidal high-low", "tidal low-low"}; //20

    /** qualifier codes used by generateDatasetsXml
     * from http://waterdata.usgs.gov/usa/nwis/uv?codes_help
     * I modified/removed "write protected" comments
     */
    public static String NWISDailyValueQualificationCodes =
"Daily Value Qualification Codes (dv_rmk_cd)\n" +
"\n" +
"Code Description\n" +
"---- ----------------------------\n" +
"A    Approved for publication -- Processing and review completed.\n" +
"e    Value has been edited or estimated by USGS personnel\n" +
"&    Value was computed from affected unit values\n" +
"E    Value was computed from estimated unit values.\n" +
"P    Provisional data subject to revision.\n" +
"<    The value is known to be less than reported value.\n" +
">    The value is known to be greater than reported value.\n" +
"1    Value is write protected without any remark code to be printed\n" +
"2    Remark is write protected without any remark code to be printed\n" +
"Ssn  Parameter monitored seasonally\n" +
"Ice  Ice affected\n" +
"Pr   Partial-record site\n" +
"Rat  Rating being developed or revised\n" +
"Nd   Not determined\n" +
"Eqp  Equipment malfunction\n" +
"Fld  Flood damage\n" +
"Dry  Zero flow\n" +
"Dis  Data-collection discontinued\n" +
"--   Parameter not determined\n" +
"***  Temporarily unavailable\n";

    public static String NWISInstantaneousValueQualificationCodes =
"Instantaneous Value Qualification Codes (uv_rmk_cd)\n" +
"\n" +
"Code Description\n" +
"---- ----------------------------\n" +
"e    The value has been edited or estimated by USGS personnel\n" +
"A    The value is affected by ice at the measurement site.\n" +
"B    The value is affected by backwater at the measurement site.\n" +
"R    The rating is undefined for this value\n" +
"&    This value is affected by unspecified reasons.\n" +
"K    The value is affected by  instrument calibration drift.\n" +
"X    The value is erroneous. It will not be used.\n" +
"<    The Value is known to be less than reported value\n" +
">    The value is known to be greater than reported value\n" +
"E    The value was computed from an estimated value\n" +
"F    The value was modified due to automated filtering.\n" +
"Ssn  Parameter monitored seasonally\n" +
"Ice  Ice affected\n" +
"Pr   Partial-record site\n" +
"Rat  Rating being developed or revised\n" +
"Nd   Not determined\n" +
"Eqp  Equipment malfunction\n" +
"Fld  Flood damage\n" +
"Dry  Zero flow\n" +
"Dis  Data-collection discontinued\n" +
"--   Parameter not determined\n" +
"***  Temporarily unavailable\n";

    public static String NWISDVOtherSummary = 
        "Daily values are derived by summarizing time-series data for each day for the " +
        "period of record. These time-series data are used to calculate daily value " +
        "statistics.  Daily data includes approved, quality-assured data that may be " +
        "published, and provisional data, whose accuracy has not been verified -- " +
        "always check the Qualifier variables.\n" +
        "\n" +
        "NOTE: To get actual daily value data (not just station information), you " +
        "must request data for just one station at a time.";

    /**
     * This constructs an EDDTableFromNWISDV based on the information in an .xml file.
     * 
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableFromNWISDV"&gt; 
     *    having just been read.  
     * @return an EDDTableFromNWISDV.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDTableFromNWISDV fromXml(SimpleXMLReader xmlReader) throws Throwable {

        if (verbose) String2.log("\n*** constructing EDDTableFromNWISDV(xmlReader)...");
        String tDatasetID = xmlReader.attributeValue("datasetID"); 

        Attributes tGlobalAttributes = null;
        ArrayList tDataVariables = new ArrayList();
        int tReloadEveryNMinutes = Integer.MAX_VALUE;
        String tAccessibleTo = null;
        StringArray tOnChange = new StringArray();
        String tFgdcFile = null;
        String tIso19115File = null;
        String tSosOfferingPrefix = null;
        String tLocalSourceUrl = null;
        String tParameterCode = null;
        String tStatisticCode = null;
        String tDefaultDataQuery = null;
        String tDefaultGraphQuery = null;

        //process the tags
        String startOfTags = xmlReader.allTags();
        int startOfTagsN = xmlReader.stackSize();
        int startOfTagsLength = startOfTags.length();
        while (true) {
            xmlReader.nextTag();
            String tags = xmlReader.allTags();
            String content = xmlReader.content();
            //if (reallyVerbose) String2.log("  tags=" + tags + content);
            if (xmlReader.stackSize() == startOfTagsN) 
                break; //the </dataset> tag
            String localTags = tags.substring(startOfTagsLength);

            //try to make the tag names as consistent, descriptive and readable as possible
            if      (localTags.equals("<addAttributes>"))
                tGlobalAttributes = getAttributesFromXml(xmlReader);
            else if (localTags.equals( "<altitudeMetersPerSourceUnit>")) 
                throw new SimpleException(EDVAlt.stopUsingAltitudeMetersPerSourceUnit);
            else if (localTags.equals( "<dataVariable>")) 
                tDataVariables.add(getSDADVariableFromXml(xmlReader));           
            else if (localTags.equals( "<accessibleTo>")) {}
            else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
            else if (localTags.equals( "<reloadEveryNMinutes>")) {}
            else if (localTags.equals("</reloadEveryNMinutes>")) tReloadEveryNMinutes = String2.parseInt(content); 
            else if (localTags.equals( "<sourceUrl>")) {}
            else if (localTags.equals("</sourceUrl>")) tLocalSourceUrl = content; 
            else if (localTags.equals( "<onChange>")) {}
            else if (localTags.equals("</onChange>")) tOnChange.add(content); 
            else if (localTags.equals( "<fgdcFile>")) {}
            else if (localTags.equals("</fgdcFile>"))     tFgdcFile = content; 
            else if (localTags.equals( "<iso19115File>")) {}
            else if (localTags.equals("</iso19115File>")) tIso19115File = content; 
            else if (localTags.equals( "<sosOfferingPrefix>")) {}
            else if (localTags.equals("</sosOfferingPrefix>")) tSosOfferingPrefix = content; 
            else if (localTags.equals( "<defaultDataQuery>")) {}
            else if (localTags.equals("</defaultDataQuery>")) tDefaultDataQuery = content; 
            else if (localTags.equals( "<defaultGraphQuery>")) {}
            else if (localTags.equals("</defaultGraphQuery>")) tDefaultGraphQuery = content; 

            else xmlReader.unexpectedTagException();
        }
        int ndv = tDataVariables.size();
        Object ttDataVariables[][] = new Object[ndv][];
        for (int i = 0; i < tDataVariables.size(); i++)
            ttDataVariables[i] = (Object[])tDataVariables.get(i);

        return new EDDTableFromNWISDV(tDatasetID, tAccessibleTo,
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
            tDefaultDataQuery, tDefaultGraphQuery, tGlobalAttributes,
            ttDataVariables,
            tReloadEveryNMinutes, tLocalSourceUrl);

    }

    /**
     * The constructor.
     *
     * <p>Yes, lots of detailed information must be supplied here
     * that is sometimes available in metadata. If it is in metadata,
     * make a subclass that extracts info from metadata and calls this 
     * constructor.
     *
     * <p>This constructor doesn't get any sourceAtts (attributes from
     * the data source, as e.g., a .das file might provide). So the addAttributes
     * must supply all of the attributes.
     *
     * <p>This dataset MUST have the stationTable .json file with stationTableColumns in
     * [tomcat]/content/erddap/subset/[datasetID].json.
     *
     * @param tDatasetID is a very short string identifier 
     *   (required: just safe characters: A-Z, a-z, 0-9, _, -, or .)
     *   for this dataset. See EDD.datasetID().
     * @param tAccessibleTo is a comma separated list of 0 or more
     *    roles which will have access to this dataset.
     *    <br>If null, everyone will have access to this dataset (even if not logged in).
     *    <br>If "", no one will have access to this dataset.
     * @param tOnChange 0 or more actions (starting with "http://" or "mailto:")
     *    to be done whenever the dataset changes significantly
     * @param tFgdcFile This should be the fullname of a file with the FGDC
     *    that should be used for this dataset, or "" (to cause ERDDAP not
     *    to try to generate FGDC metadata for this dataset), or null (to allow
     *    ERDDAP to try to generate FGDC metadata for this dataset).
     * @param tIso19115 This is like tFgdcFile, but for the ISO 19119-2/19139 metadata.
     * @param tAddGlobalAttributes are global attributes which will
     *   be added to (and take precedence over) the data source's global attributes.
     *   This may be null if you have nothing to add.
     *   The combined global attributes must include:
     *   <ul>
     *   <li> "title" - the short (&lt; 80 characters) description of the dataset 
     *   <li> "summary" - the longer description of the dataset.
     *      It may have newline characters (usually at &lt;= 72 chars per line). 
     *   <li> "institution" - the source of the data 
     *      (best if &lt; 50 characters so it fits in a graph's legend).
     *   <li> "infoUrl" - the url with information about this data set 
     *   <li> "cdm_data_type" - one of the EDD.CDM_xxx options
     *   </ul>
     *   Special case: value="null" causes that item to be removed from combinedGlobalAttributes.
     *   Special case: if combinedGlobalAttributes name="license", any instance of "[standard]"
     *     will be converted to the EDStatic.standardLicense.
     * @param tDataVariables is an Object[nDataVariables][3]: 
     *    <br>[0]=String sourceName (the name of the data variable in the dataset source, 
     *         without the outer or inner sequence name),
     *    <br>[1]=String destinationName (the name to be presented to the ERDDAP user, 
     *        or null to use the sourceName),
     *    <br>[2]=Attributes addAttributes (at ERD, this must have "ioos_category" - 
     *        a category from EDV.ioosCategories). 
     *        Special case: value="null" causes that item to be removed from combinedAttributes.
     *    <br>[3]=dataType
     *    <br>The order of variables you define doesn't have to match the
     *       order in the source.
     *    <p>If there is a time variable,  
     *      either tAddAttributes (read first) or tSourceAttributes must have "units"
     *      which is either <ul>
     *      <li> a UDUunits string (containing " since ")
     *        describing how to interpret source time values 
     *        (which should always be numeric since they are a dimension of a grid)
     *        (e.g., "seconds since 1970-01-01T00:00:00").
     *      <li> a org.joda.time.format.DateTimeFormat string
     *        (which is compatible with java.text.SimpleDateFormat) describing how to interpret 
     *        string times  (e.g., the ISO8601TZ_FORMAT "yyyy-MM-dd'T'HH:mm:ssZ", see 
     *        http://joda-time.sourceforge.net/api-release/org/joda/time/format/DateTimeFormat.html or 
     *        http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html).
     *      </ul>
     * @param tReloadEveryNMinutes indicates how often the source should
     *    be checked for new data.
     * @param tLocalSourceUrl the url to which .das or .dds or ... can be added
     * @throws Throwable if trouble
     */
    public EDDTableFromNWISDV(
        String tDatasetID, String tAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tSosOfferingPrefix,
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes,
        String tLocalSourceUrl) throws Throwable {

        if (verbose) String2.log(
            "\n*** constructing EDDTableFromNWISDV: " + tDatasetID); 
        long constructionStartMillis = System.currentTimeMillis();
        String errorInMethod = "Error in EDDTableFromNWISDV(" + 
            tDatasetID + ") constructor:\n";
            
        //save some of the parameters
        className = "EDDTableFromNWISDV";
        datasetID = tDatasetID;
        setAccessibleTo(tAccessibleTo);
        onChange = tOnChange;
        fgdcFile = tFgdcFile;
        iso19115File = tIso19115File;
        sosOfferingPrefix = tSosOfferingPrefix;
        defaultDataQuery = tDefaultDataQuery;
        defaultGraphQuery = tDefaultGraphQuery;
        if (tAddGlobalAttributes == null)
            tAddGlobalAttributes = new Attributes();
        addGlobalAttributes = tAddGlobalAttributes;
        addGlobalAttributes.set("sourceUrl", convertToPublicSourceUrl(tLocalSourceUrl));
        localSourceUrl = tLocalSourceUrl;
        setReloadEveryNMinutes(tReloadEveryNMinutes);
      
        //in general, WaterML sets all constraints to PARTIAL and regex to PrimitiveArray.REGEX_OP
        sourceCanConstrainNumericData = CONSTRAIN_PARTIAL; 
        sourceCanConstrainStringData  = CONSTRAIN_PARTIAL; 
        sourceCanConstrainStringRegex = PrimitiveArray.REGEX_OP; //standardizeResultsTable always (re)tests regex constraints

        //get global attributes
        combinedGlobalAttributes = new Attributes(addGlobalAttributes); 
        String tLicense = combinedGlobalAttributes.getString("license");
        if (tLicense != null)
            combinedGlobalAttributes.set("license", 
                String2.replaceAll(tLicense, "[standard]", EDStatic.standardLicense));
        combinedGlobalAttributes.removeValue("null");
        parameterCode = combinedGlobalAttributes.getString("parameterCode");
        statisticCode = combinedGlobalAttributes.getString("statisticCode");
        if (parameterCode == null)
            throw new SimpleException("\"parameterCode\" must be included in global <addAttributes>.");
        if (statisticCode == null)
            throw new SimpleException("\"statisticCode\" must be included in global <addAttributes>.");

        //create structures to hold the sourceAttributes temporarily
        int ndv = tDataVariables.length;
        Attributes tDataSourceAttributes[] = new Attributes[ndv];
        String tDataSourceTypes[] = new String[ndv];
        String tDataSourceNames[] = new String[ndv];
        for (int dv = 0; dv < ndv; dv++) {
            tDataSourceNames[dv] = (String)tDataVariables[dv][0];
        }


        //create dataVariables[]
        dataVariables = new EDV[ndv];
        for (int dv = 0; dv < ndv; dv++) {
            String tSourceName = (String)tDataVariables[dv][0];
            String tDestName = (String)tDataVariables[dv][1];
            if (tDestName == null || tDestName.trim().length() == 0)
                tDestName = tSourceName;
            Attributes tSourceAtt = new Attributes();
            Attributes tAddAtt = (Attributes)tDataVariables[dv][2];
            String tSourceType = (String)tDataVariables[dv][3];
            //if (reallyVerbose) String2.log("  dv=" + dv + " sourceName=" + tSourceName + " sourceType=" + tSourceType);

            //ensure tSourceType was specified
            if (tSourceName.startsWith("=")) {
                //if isFixedValue, sourceType can be inferred 
            } else if (tSourceType == null) {
                throw new IllegalArgumentException(errorInMethod + 
                    "<dataType> wasn't specified for dataVariable#" + dv + 
                    "=" + tSourceName + ".");
            }

            Double tMin = Double.NaN;
            Double tMax = Double.NaN;

            //make the variable
            if (EDV.LON_NAME.equals(tDestName)) {
                dataVariables[dv] = new EDVLon(tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType, tMin, tMax); 
                lonIndex = dv;
            } else if (EDV.LAT_NAME.equals(tDestName)) {
                dataVariables[dv] = new EDVLat(tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType, tMin, tMax); 
                latIndex = dv;
            } else if (EDV.ALT_NAME.equals(tDestName)) {
                dataVariables[dv] = new EDVAlt(tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType, tMin, tMax);
                altIndex = dv;
            } else if (EDV.DEPTH_NAME.equals(tDestName)) {
                dataVariables[dv] = new EDVDepth(tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType, tMin, tMax);
                depthIndex = dv;
            } else if (EDV.TIME_NAME.equals(tDestName)) {  //look for TIME_NAME before check hasTimeUnits (next)
                //time won't be in the stationTable, so no getNMinMax
                dataVariables[dv] = new EDVTime(tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType); //this constructor gets source / sets destination actual_range 
                timeIndex = dv; 
            } else if (EDVTimeStamp.hasTimeUnits(tSourceAtt, tAddAtt)) {
                dataVariables[dv] = new EDVTimeStamp(tSourceName, tDestName, 
                    tSourceAtt, tAddAtt, 
                    tSourceType); //this constructor gets source / sets destination actual_range
            } else {
                dataVariables[dv] = new EDV(tSourceName, tDestName, 
                    tSourceAtt, tAddAtt,
                    tSourceType, tMin, tMax);
            }
        }

        //read the stationTable (i.e., the subsetVariablesDataTable)
        Table stationTable = null;
        try {
            stationTable = subsetVariablesDataTable(null);
        } catch (Throwable t) {
            throw new RuntimeException(errorInMethod + 
                "Unable to create subsetVariablesDataTable.\n" +
                "(Perhaps the required, admin-provided subset file in " +
                    "[tomcat]/content/erddap/subset/\n" +
                "wasn't found or couldn't be read.)", t);
        }

        //set the variables' min/max
        for (int dv = 0; dv < ndv; dv++) {
            EDV edv = dataVariables[dv];
            PrimitiveArray stationPA = null;
            int stpo = stationTable.findColumnNumber(edv.destinationName());

            //if var in stationTable
            if (stpo >= 0) {
                stationPA = stationTable.getColumn(stpo);

                //if numeric, set min,max
                if (edv.sourceDataTypeClass() != String.class) {
                    double stats[] = stationPA.calculateStats();
                    if (stats[PrimitiveArray.STATS_N] > 0) {
                        edv.setDestinationMin(stats[PrimitiveArray.STATS_MIN]);
                        edv.setDestinationMax(stats[PrimitiveArray.STATS_MAX]); 
                        edv.setActualRangeFromDestinationMinMax();
                    }
    
                //if time  (but it won't be in stationTable)
                } else if (edv instanceof EDVTime) {
                    //do nothing
    
                //if beginDate or endDate, set min,max
                } else if (edv instanceof EDVTimeStamp) {
                    //this assumes beginDate endDate columns have ISO String time values 
                    //(which sort correctly)
                    String nMinMax[] = ((StringArray)stationPA).getNMinMax();
                    //tAddAtt.set("data_min", nMinMax[1]);
                    //tAddAtt.set("data_max", nMinMax[2]); 
                    edv.setDestinationMin(Calendar2.safeIsoStringToEpochSeconds(nMinMax[1]));
                    edv.setDestinationMax(Calendar2.safeIsoStringToEpochSeconds(nMinMax[2]));
                    edv.setActualRangeFromDestinationMinMax();
                }
            }
        }

        //ensure the setup is valid  
        //It will read the /subset/ table or fail trying to get the data if the file isn't present.
        ensureValid();

        //finally
        if (verbose) String2.log(
            (reallyVerbose? "\n" + toString() : "") +
            "\n*** EDDTableFromNWISDV " + datasetID + " constructor finished. TIME=" + 
            (System.currentTimeMillis() - constructionStartMillis) + "\n"); 

    }


    /** 
     * This gets the data (chunk by chunk) from this EDDTable for the 
     * OPeNDAP DAP-style query and writes it to the TableWriter. 
     * See the EDDTable method documentation.
     *
     * @param loggedInAs the user's login name if logged in (or null if not logged in).
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     * @param userDapQuery the part of the user's request after the '?', still percentEncoded, may be null.
     * @param tableWriter
     * @throws Throwable if trouble (notably, WaitThenTryAgainException)
     */
    public void getDataForDapQuery(String loggedInAs, String requestUrl, 
        String userDapQuery, TableWriter tableWriter) throws Throwable {

        //make the sourceQuery
        StringArray resultsVariables    = new StringArray();
        StringArray constraintVariables = new StringArray();
        StringArray constraintOps       = new StringArray();
        StringArray constraintValues    = new StringArray();
        getSourceQueryFromDapQuery(userDapQuery,
            resultsVariables,
            constraintVariables, constraintOps, constraintValues); //timeStamp constraints other than regex are epochSeconds

        //load stationTable (the subsetTable)   
        Table stationTable = subsetVariablesDataTable(loggedInAs);
        //beginDate and endDate are StringArrays
        int beginDateCol = stationTable.findColumnNumber("beginDate");
        int endDateCol   = stationTable.findColumnNumber("endDate");
        if (beginDateCol < 0 || endDateCol < 0)
            throw new SimpleException(
                "Setup error: the subset table MUST have beginDate and endDate columns.");
        PrimitiveArray beginDatePA = stationTable.getColumn(beginDateCol);
        PrimitiveArray endDatePA   = stationTable.getColumn(endDateCol);
        //String2.log("beginDate class=" + beginDatePA.elementClassString());

        //go through the station table to find the stations of interest
        //and find beginTime>= endTime<= 
        int nStations = stationTable.nRows();
        int nStationColumns = stationTable.nColumns();
        BitSet keep = new BitSet();
        keep.set(0, nStations, true);
        int nKeep = nStations;
        double beginSeconds = Double.NaN;
        double endSeconds = Double.NaN;
        int nConstraints = constraintVariables.size();
        for (int c = 0; c < nConstraints; c++) { 
            String conVar = constraintVariables.get(c);
            String conOp  = constraintOps.get(c);
            char conOp0  = conOp.charAt(0);
            String conVal = constraintValues.get(c);
            if (conVar.equals("time")) {
                double cSeconds = String2.parseDouble(conVal);
                if (conOp0 == '>') {
                    beginSeconds = Math2.finiteMax(beginSeconds, cSeconds);
                } else if (conOp0 == '<') {
                    endSeconds =   Math2.finiteMin(  endSeconds, cSeconds);
                } else if (conOp.equals("=")) {
                    beginSeconds = Math2.finiteMax(beginSeconds, cSeconds);
                    endSeconds =   Math2.finiteMin(  endSeconds, cSeconds);
                }
                continue;
            }

            //is this constraint for a column that is in the stationTable?
            nKeep = stationTable.tryToApplyConstraint(
                    stationTable.findColumnNumber(conVar), 
                    conVar, conOp, conVal, keep);
            if (nKeep == 0)
                throw new SimpleException(MustBe.THERE_IS_NO_DATA + 
                    " (There are no matching sites.)");
        }

        //I checked. NWIS DV query needn't have &StartDate or &EndDate (or can be "")
        //http://interim.waterservices.usgs.gov/NWISQuery/GetDV1?
        //SiteNum=01311143&ParameterCode=00003&StatisticCode=00001&AgencyCode=USGS&action=Submit
        //??? Does WaterML/NWIS have a maximum time range for the request?  
        //  Instantaneous Values has 120 day limit

        //Daily Values: round beginTime up, endTime down to nearest day
        int spd = Calendar2.SECONDS_PER_DAY;
        beginSeconds = Math.ceil( beginSeconds / spd) * spd; //NaN stays as NaN
        endSeconds   = Math.floor(  endSeconds / spd) * spd;
        if (!Double.isNaN(beginSeconds) && !Double.isNaN(endSeconds) &&
            beginSeconds > endSeconds)  //e.g., begin=end=noon of some day
            throw new SimpleException(MustBe.THERE_IS_NO_DATA + 
                " (The data for each day is associated with the start of each day: T00:00:00.)");
        String beginTime = ""; 
        String endTime   = "";
        //do special tests (so no test done if stationValue is "")
        if (!Double.isNaN(beginSeconds) && nKeep > 0) {
            //requested beginTime
            beginTime = Calendar2.epochSecondsToIsoStringT(beginSeconds).substring(0, 10); 
            nKeep = 0;
            int row = keep.nextSetBit(0);
            while (row >= 0) {
                String stationEndDate = endDatePA.getString(row); 
                if (stationEndDate.length() == 0) {
                    //don't test
                    nKeep++;
                } else {
                    if (PrimitiveArray.testValueOpValue(beginTime, "<=", stationEndDate)) 
                        nKeep++;
                    else 
                        keep.clear(row);
                }
                row = keep.nextSetBit(row + 1);
            }
        }
        if (!Double.isNaN(endSeconds) && nKeep > 0) {
            //requested endTime
            endTime = Calendar2.epochSecondsToIsoStringT(endSeconds).substring(0, 10); 
            nKeep = 0;
            int row = keep.nextSetBit(0);
            while (row >= 0) {
                String stationBeginDate = beginDatePA.getString(row); 
                if (stationBeginDate.length() == 0) {
                    //don't test
                    nKeep++;
                } else {
                    if (PrimitiveArray.testValueOpValue(endTime, ">=", stationBeginDate)) 
                        nKeep++;
                    else 
                        keep.clear(row);
                }
                row = keep.nextSetBit(row + 1);
            }
        }
        if (nKeep == 0)
            throw new SimpleException(MustBe.THERE_IS_NO_DATA + 
                " (There are no matching sites (time).)");


        //** Is request for station variables only?
        //If so, request is applied to stationTable (not full dataset, not distinct())
        //  Not perfect, but very reasonable 
        //  (more reasonable than correct approach, which is full dataset).
        boolean justStationVars = true;
        int nRV = resultsVariables.size();
        for (int rv = 0; rv < nRV; rv++) {
            if (stationTable.findColumnNumber(resultsVariables.get(rv)) < 0) {
                justStationVars = false;
                break;
            }
        }
        if (justStationVars) {
            if (reallyVerbose) String2.log("justStationVars");
            stationTable.justKeep(keep);
            standardizeResultsTable(requestUrl, userDapQuery, stationTable);
            if (stationTable.nRows() > 0) 
                tableWriter.writeSome(stationTable);    
            tableWriter.finish();
            return;
        }

        //** ensure just ONE valid station 
        //It is too easy to request data for multiple stations (e.g., make-a-graph)
        //and NWIS website specifically asks that users be conservative in amount
        //of data they ask for.
        if (nKeep != 1) 
            throw new SimpleException(
                "To get actual daily value data (not just station information), you " +
                "must request data for just one station at a time.  " +
                "(There were " + nKeep + " matching stations.)");

        //** get the data for the ONE valid station 
        int stationRow = keep.nextSetBit(0);
        boolean errorPrinted = false;
        //while (stationRow >= 0) {

            //format the query
            //http://interim.waterservices.usgs.gov/NWISQuery/GetDV1?
            //SiteNum=07334200&ParameterCode=00060&StatisticCode=00003&AgencyCode=
            //&StartDate=2000-11-16&EndDate=2000-12-15&action=Submit
            String tAgency   = stationTable.getStringData(stationTableColAgency,   stationRow);
            String tSiteCode = stationTable.getStringData(stationTableColSiteCode, stationRow);
            String encodedSourceUrl = localSourceUrl +
                "?SiteNum=" + tSiteCode +    //no need to percent encode, all digits     
                "&ParameterCode=" + parameterCode +
                "&StatisticCode=" + statisticCode +
                "&AgencyCode=" + SSR.minimalPercentEncode(tAgency) +
                "&StartDate=" + beginTime +
                "&EndDate="   + endTime +
                "&action=Submit";

            Table table;
            try {
                //get data from waterML service
                if (reallyVerbose) String2.log(encodedSourceUrl);
                table = getValuesTable(
                    SSR.getUncompressedUrlInputStream(encodedSourceUrl)); 
            } catch (Throwable t) {
                if (!reallyVerbose)  //if reallyVerbose, it was done above
                    String2.log(encodedSourceUrl);
                throw new Throwable(EDStatic.errorFromDataSource + t.toString(), t);
            }

            int nRows = table.nRows();
            if (nRows == 0) {
                if (!reallyVerbose)  //if reallyVerbose, it was done above
                    String2.log(encodedSourceUrl);
                throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (from source)");
            }

            //verify site Information (stationTable vs here)
            Attributes globalAtts = table.globalAttributes();
            Test.ensureEqual(
                tSiteCode, 
                globalAtts.getString("siteCode"), 
                "Unexpected siteCode in response from data source!");
            Test.ensureEqual(
                stationTable.getStringData(stationTableColSiteName, stationRow), 
                globalAtts.getString("siteName"), 
                "Unexpected siteName in response from data source!");
            Test.ensureEqual(
                stationTable.getDoubleData(stationTableColLongitude, stationRow), 
                globalAtts.getDouble("longitude"), 
                "Unexpected longitude in response from data source!");
            Test.ensureEqual(
                stationTable.getDoubleData(stationTableColLatitude, stationRow), 
                globalAtts.getDouble("latitude"), 
                "Unexpected latitude in response from data source!");

            //add requested columns from stationTable
            for (int col = 0; col < nStationColumns; col++) { 
                String colName = stationTable.getColumnName(col);
                if (resultsVariables.indexOf(colName) >= 0) {
                    table.addColumn(table.nColumns(), colName,
                        //one value, replicated nRows times
                        PrimitiveArray.factory(
                            stationTable.getColumn(col).elementClass(),
                            nRows, stationTable.getStringData(col, stationRow)),
                        new Attributes());
                }
            }

            //String2.log("\nstationInfo+valuesColumns table=\n" + table.dataToCSVString());
            if (table.nRows() > 0)
                standardizeResultsTable(requestUrl, userDapQuery, table);

            if (table.nRows() > 0) {
                tableWriter.writeSome(table);
                //Activate this if stationRow loop reactivated
                //if (tableWriter.noMoreDataPlease) {
                //    tableWrite.logCaughtNoMoreDataPlease(datasetID);
                //    break;
                //}
            }

        //    stationRow = keep.nextSetBit(stationRow + 1);
        //}

        tableWriter.finish();
    }


    /**
     * This makes a table of values from the data found in one response XML.
     * See sample XML response from URL constructor at
     * http://waterservices.usgs.gov/rest/USGS-DV-Service.html .
     * which makes a query like
     * http://interim.waterservices.usgs.gov/NWISQuery/GetDV1?SiteNum=07334200&ParameterCode=00060&StatisticCode=00003&AgencyCode=&StartDate=2000-11-16&EndDate=2000-12-15&action=Submit
     * (Bob has copy in c:/programs/waterML/nwisGetDV1.xml )
     * Page numbers in comments are from CUAHSI WaterML 1.0 document.
     *
     * @param in a non-buffered inputstream with an XML getValues response.
     *    Afterwards, this closes the input stream (if all goes well).
     * @return a table of values from the data found in one response XML.
     *    The columns are time
     *    Several site values are returned as 
     * @throws Throwable if trouble
     */
    public static Table getValuesTable(InputStream in) throws Throwable {

        SimpleXMLReader xmlReader = new SimpleXMLReader(in);
        xmlReader.nextTag();
        String firstTag = xmlReader.tag(0);
        if (!firstTag.endsWith("timeSeriesResponse")) {  //e.g., may be ns2:timeSeriesResponse
            in.close();
            throw new SimpleException("XML Response ERROR: The first tag=\"" + firstTag + 
                "\" should have been timeSeriesResponse.");
        }
        Table table = new Table();
        Attributes globalAtts = table.globalAttributes();
        Attributes variableAtts = new Attributes();
        StringArray timePA = new StringArray();
        table.addColumn("time", timePA);
        HashMap timeRowMap = new HashMap(); //stringTime -> Integer(row)
        //source info
        String tSiteCode = null;
        String tSiteID = null;
        String tSiteName = null;
        String tSiteNetwork = null;
        String tSRS = null;
        double tLongitude = Double.NaN; //spec says double
        double tLatitude  = Double.NaN;
        double tElevation = Double.NaN;
        String tVerticalDatum = null;
        StringBuilder sourceNotes     = new StringBuilder();
        //variable info
        StringBuilder variableCode    = new StringBuilder();
        StringBuilder variableOptions = new StringBuilder();
        StringBuilder variableNotes   = new StringBuilder();
        String tVariableName = null;
        String tUnitsAbbrev = null;
        String tVariableCodeVocabulary = null;
        //values info
        StringArray valueTimes      = new StringArray();
        DoubleArray valueValues     = new DoubleArray();  //spec says decimal
        StringArray valueQualifiers = new StringArray();
        String valueMethodID = null;
        String valueMethodDescription = null;
        String tQualifierCode = null;
        StringBuilder valueQualifierDescription  = new StringBuilder();
        Attributes valueQualifierAtts = new Attributes();
        int noMethodNumber = 1;

        while (true) {
            xmlReader.nextTag();
            int stackSize = xmlReader.stackSize();
            if (stackSize == 1) 
                break; 
            //String2.log(xmlReader.allTags());
            if (stackSize < 3 ||
                //!firstTag.equals(xmlReader.tag(0)) || //not necessary to check. this exits when /firstTag tag is reached
                !"timeSeries".equals(xmlReader.tag(1))) 
                continue;
            String tag2 = xmlReader.tag(2);
            
            //skip <timeSeriesResponse><queryInfo>     

            /*read <timeSeriesResponse><timeSeries><sourceInfo>        pg 19
<sourceInfo xsi:type="SiteInfoType" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <siteName>02N-06E-34 CCD 1 Byrds Mill Spring nr Fittstown,OK</siteName>
  <siteCode siteID="232418" network="NWISDV">07334200</siteCode>
  <timeZoneInfo>
    <defaultTimeZone ZoneOffset="-06:00" ZoneAbbreviation="CST"/>
    <daylightSavingsTimeZone ZoneOffset="-05:00" ZoneAbbreviation="CDT"/></timeZoneInfo>
  <geoLocation>
    <geogLocation xsi:type="LatLonPointType" srs="EPSG:4269">
      <latitude>34.59453434</latitude>
      <longitude>-96.6655632</longitude>
    </geogLocation></geoLocation>
  <note>Agency:USGS</note>
</sourceInfo>*/
            if ("sourceInfo".equals(tag2) && stackSize > 3) {  //page 19
                String tag3 = xmlReader.tag(3);
                String tag4 = xmlReader.tag(4);
                String tag5 = xmlReader.tag(5);
                if        ("/siteName".equals(tag3))  {tSiteName = xmlReader.content();
                } else if ("siteCode".equals(tag3) && stackSize == 4) {
                    tSiteID = xmlReader.attributeValue("siteID");
                    tSiteNetwork = xmlReader.attributeValue("network");
                } else if ("/siteCode".equals(tag3))  {tSiteCode = xmlReader.content();
                //??? store timeZoneInfo?
                } else if ("geoLocation".equals(tag3) && stackSize > 4) {
                    if ("geogLocation".equals(tag4) && stackSize == 5) {
                        tSRS = xmlReader.attributeValue("srs");
                    } else if ("/longitude".equals(tag5)) {tLongitude = String2.parseDouble(xmlReader.content());
                    } else if ("/latitude".equals(tag5))  {tLatitude  = String2.parseDouble(xmlReader.content());
                    }
                } else if ("/elevation_m".equals(tag3))   {tElevation = String2.parseDouble(xmlReader.content());
                } else if ("/verticalDatum".equals(tag3)) {tVerticalDatum = xmlReader.content();
                } else if ("<note>".equals(tag4) && stackSize == 5) { //may be many
                    if (sourceNotes.length() > 0)
                        sourceNotes.append('\n');
                    String s = xmlReader.attributeValue("href");  //optional identifiers
                    if (s != null) sourceNotes.append( "href=\"" + XML.encodeAsHTMLAttribute(s) + "\" ");
                    s = xmlReader.attributeValue("title"); 
                    if (s != null) sourceNotes.append( "title=\"" + XML.encodeAsHTMLAttribute(s) + "\" ");
                    s = xmlReader.attributeValue("show");  
                    if (s != null) sourceNotes.append( "show=\"" + XML.encodeAsHTMLAttribute(s) + "\" ");
                } else if ("</note>".equals(tag4)) { 
                    sourceNotes.append(xmlReader.content());
                }

            /* read <timeSeriesResponse><timeSeries><variable>   
<variable>
  <variableCode vocabulary="NWISDV">00060</variableCode>
  <variableName>Discharge</variableName>
  <variableDescription>Discharge, cubic feet per second</variableDescription>
  <valueType>Derived Value</valueType>
  <dataType>Average</dataType>
  <units unitsAbbreviation="cfs">cubic feet per second</units>
  <options><option optionCode="00003" name="Statistic">Mean</option></options>
  <NoDataValue>-999999</NoDataValue>
  <timeSupport isRegular="true">
    <unit>
      <UnitName>day</UnitName>
      <UnitType>Time</UnitType>
      <UnitAbbreviation>d</UnitAbbreviation>
    </unit>
    <timeInterval>1</timeInterval>
  </timeSupport>
</variable>*/
            } else if ("variable".equals(tag2) && stackSize > 3) {   //page 28
                //as much as reasonable: make attributes with names identical to WaterML names
                //  e.g., variableName -> variableName
                String tag3 = xmlReader.tag(3);
                if (stackSize == 4 && "variableCode".equals(tag3)) {
                    tVariableCodeVocabulary = xmlReader.attributeValue("vocabulary");
                } else if ("/variableCode".equals(tag3)) { // if >1, 1 per line
                    if (variableCode.length() > 0)
                        variableCode.append('\n');
                    variableCode.append(tVariableCodeVocabulary == null? "" : 
                        (tVariableCodeVocabulary + ":" + xmlReader.content()));
                } else if ("/variableName".equals(tag3))       {tVariableName = xmlReader.content();
                } else if ("/variableDescription".equals(tag3)){variableAtts.add("variableDescription", xmlReader.content());
                } else if ("/valueType".equals(tag3))          {variableAtts.add("valueType",           xmlReader.content());
                } else if ("/dataType".equals(tag3))           {variableAtts.add("dataType",            xmlReader.content());
                } else if ("/generalCategory".equals(tag3))    {variableAtts.add("generalCategory",     xmlReader.content());
                } else if ("/sampleMedium".equals(tag3))       {variableAtts.add("sampleMedium",        xmlReader.content());
                } else if (stackSize == 4 && "units".equals(tag3)) {
                    tUnitsAbbrev = xmlReader.attributeValue("unitsAbbreviation");
                } else if ("/units".equals(tag3)) {
                    variableAtts.add("long_units", xmlReader.content());
                    variableAtts.add("units", 
                        tUnitsAbbrev == null? xmlReader.content() : tUnitsAbbrev);
                } else if ("options".equals(tag3) && stackSize > 4) { 
                    String tag4 = xmlReader.tag(4);
                    if ("option".equals(tag4) && stackSize == 5) { // if >1, 1 per line
                        if (variableOptions.length() > 0)
                            variableOptions.append('\n');
                        String s = xmlReader.attributeValue("name");
                        if (s != null) variableOptions.append("name=\"" + s + "\" "); //string
                        s = xmlReader.attributeValue("optionCode");
                        if (s != null) variableOptions.append("optionCode=\"" + s + "\" "); //token(?)
                        s = xmlReader.attributeValue("optionID");
                        if (s != null) variableOptions.append("optionID=" + s + " ");  //int
                    } else if ("/option".equals(tag4))         {
                        variableOptions.append(xmlReader.content());
                    }
                } else if ("<note>".equals(tag3) && stackSize == 4) { // if >1, 1 per line
                    if (variableNotes.length() > 0)
                        variableNotes.append('\n');
                    String s = xmlReader.attributeValue("href");  //optional identifiers
                    if (s != null) variableNotes.append( "href=\"" + XML.encodeAsHTMLAttribute(s) + "\" ");
                    s = xmlReader.attributeValue("title"); 
                    if (s != null) variableNotes.append( "title=\"" + XML.encodeAsHTMLAttribute(s) + "\" ");
                    s = xmlReader.attributeValue("show");  
                    if (s != null) variableNotes.append( "show=\"" + XML.encodeAsHTMLAttribute(s) + "\" ");
                } else if ("</note>".equals(tag3)) { 
                    variableNotes.append(xmlReader.content());
                } else if ("<related>".equals(tag3)) {
                    if (reallyVerbose) String2.log("  ignoring <related> tag");
                } else if ("<extension>".equals(tag3)) {
                    if (reallyVerbose) String2.log("  ignoring <extension> tag");
                } else if ("/NoDataValue".equals(tag3))  {
                    //values are doubles, so mv is, too
                    double tMV = String2.parseDouble(xmlReader.content());
                    variableAtts.add("NoDataValue",   tMV);  
                    variableAtts.add("missing_value", tMV);  
                } else if ("<timeSupport>".equals(tag3)) {
                    //if reallyVerbose) String2.log("  ignoring <timeSupport> tag");
                } 

            /* read <timeSeriesResponse><timeSeries><values>  
<values count="30">
<value qualifiers="A" dateTime="2000-11-16T00:00:00">5.4</value>
<value qualifiers="Ae" dateTime="2000-11-20T00:00:00">5.6</value>
...
<qualifier vocabulary="dv_rmk_cd" network="USGS" qualifierCode="A">Approved for publication -- Processing and review completed.</qualifier>
<qualifier vocabulary="dv_rmk_cd" network="USGS" qualifierCode="e">Value has been estimated.</qualifier>
<method methodID="4">
  <MethodDescription>sensor:Creek flow via DCP</MethodDescription></method></values>  */
            } else if ("values".equals(tag2) && stackSize > 3) {    //page 34
                //String2.log(" v="+xmlReader.allTags());
                String tag3 = xmlReader.tag(3);
                if (stackSize == 4 && "value".equals(tag3)) {
                    //??? There are lots of other attributes.  Do I need to grab them?
                    valueQualifiers.add(xmlReader.attributeValue("qualifiers"));
                    valueTimes.add(xmlReader.attributeValue("dateTime"));
                } else if ("/value".equals(tag3))  {
                    valueValues.add(String2.parseDouble(xmlReader.content()));
                } else if ("qualifier".equals(tag3) && stackSize == 4) {
                    //??? this assumes all qualifier codes will use same vocabulary and network!
                    valueQualifierAtts.add("vocabulary", xmlReader.attributeValue("vocabulary"));
                    valueQualifierAtts.add("network",    xmlReader.attributeValue("network"));
                    tQualifierCode = xmlReader.attributeValue("qualifierCode");
                } else if ("/qualifier".equals(tag3)) {
                    if (valueQualifierDescription.length() > 0)
                        valueQualifierDescription.append('\n');
                    valueQualifierDescription.append(tQualifierCode + " = " + xmlReader.content());
                } else if ("method".equals(tag3)) {
                    if (stackSize == 4) {
                        valueMethodID = xmlReader.attributeValue("methodID");
                    } else if ("/MethodDescription".equals(xmlReader.tag(4))) {
                        valueMethodDescription = xmlReader.content();
                    }
                }

            } else if ("/values".equals(tag2)) {
                //String2.log("/v="+xmlReader.allTags() + " nValues=" + valueValues.size());
                //try to add the data
                String varName;
                if (valueMethodID != null && valueMethodID.length() > 0) {
                    varName = "method" + valueMethodID;
                } else {
                    String nmn = noMethodNumber == 1? "" : "" + noMethodNumber;
                    noMethodNumber++;
                    varName = tVariableName != null && tVariableName.length() > 0? 
                        tVariableName + nmn : "values" + nmn;
                }

                //if this method wasn't static, I could check if methodID is a sourceName
                //  and create newPA with correct data type
                DoubleArray newVPA = new DoubleArray(); //values
                StringArray newQPA = new StringArray(); //qualifiers
                int nRows = table.nRows();
                newVPA.addN(nRows, Double.NaN); 
                newQPA.addN(nRows, ""); 
                String tLongName = 
                    (tVariableName == null? "" : tVariableName) +
                    (tVariableName == null || tVariableName.length() == 0 ||
                     valueMethodDescription == null || valueMethodDescription.length() == 0 ? "" :
                        ", ") +
                    (valueMethodDescription == null? "" : valueMethodDescription);                            
                Attributes vAttributes = ((Attributes)(variableAtts.clone()))
                    .add("ioos_category", "Hydrology")
                    .add("long_name",     tLongName)
                    .add("note",          variableNotes.toString())  
                    .add("options",       variableOptions.toString())
                    .add("variableCode",  variableCode.toString());
                Attributes qAttributes = ((Attributes)(valueQualifierAtts.clone()))
                    .add("description",   valueQualifierDescription.toString())
                    .add("ioos_category", "Quality");
                table.addColumn(table.nColumns(), varName, 
                    newVPA, vAttributes);
                table.addColumn(table.nColumns(), varName + "Qualifiers", 
                    newQPA, qAttributes);
                int nValues = valueValues.size();
                for (int i = 0; i < nValues; i++) {
                    String tTime = valueTimes.get(i);
                    if (tTime != null) {
                        Integer row = (Integer)timeRowMap.get(tTime);
                        if (row == null) {
                            //add a row
                            row = new Integer(nRows);
                            timeRowMap.put(tTime, row);
                            table.insertBlankRow(nRows++);
                            timePA.set(nRows - 1, tTime);
                        } 
                        int iRow = row.intValue();
                        newVPA.set(iRow, valueValues.get(i));
                        newQPA.set(iRow, valueQualifiers.get(i));
                    }
                }

                //clear for next <value> 
                valueTimes.clear();
                valueValues.clear();
                valueQualifiers.clear();
                valueMethodID = null;
                valueMethodDescription = null;
                valueQualifierAtts = new Attributes();
                valueQualifierDescription.setLength(0);
            }
        }
        in.close();

        //sort based on time values
        table.leftToRightSort(1);

        globalAtts.add("notes", variableNotes.toString());  //may be null

        //add global metadata for site info
        //if reasonable, keep names identical to WaterML names
        globalAtts.add("siteCode",  tSiteCode);
        globalAtts.add("network",   tSiteNetwork);
        globalAtts.add("siteID",    tSiteID);
        globalAtts.add("siteName",  tSiteName);
        globalAtts.add("longitude", tLongitude);
        globalAtts.add("latitude",  tLatitude);
        globalAtts.add("srs",       tSRS);
        globalAtts.add("altitude",  tElevation);
        globalAtts.add("verticalDatum", tVerticalDatum);

        return table;
    }

    /** Test getValuesTable */
    public static void testGetValuesTable() throws Throwable {
        String2.log("\n*** EDDTableFromNWISDV.testGetValuesTable");
        Table table;
        String results, expected;

/*        //has <method> tags
        //http://interim.waterservices.usgs.gov/NWISQuery/GetDV1?SiteNum=07334200&ParameterCode=00060&StatisticCode=00003&AgencyCode=&StartDate=2000-11-16&EndDate=2000-12-15&action=Submit
        table = getValuesTable(new FileInputStream("c:/programs/waterML/nwisGetDV1.xml"));
        results = table.toCSSVString(5);
        expected = 
"zztop\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
*/
        //doesn't have <method> tags
        //http://interim.waterservices.usgs.gov/NWISQuery/GetDV1?SiteNum=01010000&ParameterCode=00060&StatisticCode=00003&AgencyCode=&USGSStartDate=2009-01-31&EndDate=2009-02-01&action=Submit
        table = getValuesTable(new FileInputStream(
            "c:/data/waterML/nwisValues.xml"));
        results = table.toCSVString();
        expected = 
"{\n" +
"dimensions:\n" +
"\trow = 2 ;\n" +
"\ttimeStringLength = 19 ;\n" +
"\tDischargeQualifiersStringLength = 2 ;\n" +
"variables:\n" +
"\tchar time(row, timeStringLength) ;\n" +
"\tdouble Discharge(row) ;\n" +
"\t\tDischarge:dataType = \"Average\" ;\n" +
"\t\tDischarge:ioos_category = \"Hydrology\" ;\n" +
"\t\tDischarge:long_name = \"Discharge\" ;\n" +
"\t\tDischarge:long_units = \"cubic feet per second\" ;\n" +
"\t\tDischarge:missing_value = -999999.0 ;\n" +
"\t\tDischarge:NoDataValue = -999999.0 ;\n" +
"\t\tDischarge:options = \"name=\\\"Statistic\\\" optionCode=\\\"00003\\\" Mean\" ;\n" +
"\t\tDischarge:units = \"cfs\" ;\n" +
"\t\tDischarge:valueType = \"Derived Value\" ;\n" +
"\t\tDischarge:variableCode = \"NWISDV:00060\" ;\n" +
"\t\tDischarge:variableDescription = \"Discharge, cubic feet per second\" ;\n" +
"\tchar DischargeQualifiers(row, DischargeQualifiersStringLength) ;\n" +
"\t\tDischargeQualifiers:description = \"A = Approved for publication -- Processing and review completed.\n" +
"e = Value has been estimated.\" ;\n" +
"\t\tDischargeQualifiers:ioos_category = \"Quality\" ;\n" +
"\t\tDischargeQualifiers:network = \"USGS\" ;\n" +
"\t\tDischargeQualifiers:vocabulary = \"dv_rmk_cd\" ;\n" +
"\n" +
"// global attributes:\n" +
"\t\t:altitude = NaN ;\n" +
"\t\t:latitude = 46.70055556 ;\n" +
"\t\t:longitude = -69.7155556 ;\n" +
"\t\t:network = \"NWISDV\" ;\n" +
"\t\t:siteCode = \"01010000\" ;\n" +
"\t\t:siteID = \"1266238\" ;\n" +
"\t\t:siteName = \"St. John River at Ninemile Bridge, Maine\" ;\n" +
"\t\t:srs = \"EPSG:4269\" ;\n" +
"}\n" +
"row,time,Discharge,DischargeQualifiers\n" +
"0,2009-01-31T00:00:00,388.0,Ae\n" +
"1,2009-02-01T00:00:00,393.0,Ae\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

    }

    /**
     * This generates the xml for datasets.xml and the /subset/ table 
     * for a system like NWIS that uses WaterML for values requests.
     *
     * @param tLocalWaterMLUrl the URL of the WaterML-based data service, e.g.,
     *   The URL constructor at 
     *   http://waterservices.usgs.gov/rest/USGS-DV-Service.html
     *   generates the WaterML request URL
     *   http://interim.waterservices.usgs.gov/NWISQuery/GetDV1?
     *   SiteNum=07334200&amp;ParameterCode=00060&amp;StatisticCode=00003
     *      &amp;AgencyCode=USGS&amp;StartDate=2000-11-16&amp;EndDate=2000-12-15&amp;action=Submit
     *   so the tLocalWaterMLUrl is http://interim.waterservices.usgs.gov/NWISQuery/GetDV1 .
     *   If the service is local, it may need to be a numeric IP.
     *   See &lt;convertToPublicSourceUrl&gt; in datasets.xml
     * @param tSiteCode
     * @param tAgency  e.g., USGS  just used for the query
     * @param tParameterCode  AKA parm_cd, e.g., 00060="Stream flow, mean. daily"
     *   See the list at http://qwwebservices.usgs.gov/public_srsnames.html
     * @param tCharacteristicName 
     *    For NWIS, look at http://qwwebservices.usgs.gov/public_srsnames.html
     *    to find a parm_cd (00060) and the corresponding 
     *    characteristicname ("Stream flow, mean. daily").
     * @param tStatisticCode e.g., 00003=MEAN
     *   See the list at http://river.sdsc.edu/Wiki/NWIS%20Statistics%20codes%20stat_cd.ashx 
     * @param tStatisticName for the title and summary, e.g., MEAN or mean (will be toLowerCase()'d)
     * @param tDataVariables if not null, only these data variables are kept
     * @param tEndDate the station's end date for data (to use for a request for data)
     * @param tStarterTable has the columns that the station table will provide
     *   e.g., longitude, latitude, site_code, site_name, ...
     *   but not beginTime, endTime, or variables.
     *   The data is not important. The variable names and attributes are important.
     * @param tInfoUrl e.g., http://waterservices.usgs.gov/rest/USGS-DV-Service.html
     * @param tInstitution e.g., USGS
     * @param startTitle e.g., NWIS Daily Values
     * @param tQualificationCodes  usually either NWISDailyValue or NWISInstananeousValueQualificationCodes
     *    (see above).
     * @return a suggested chunk of xml for this dataset for use in datasets.xml 
     * @throws Throwable if trouble, e.g., if no Grid or Array variables are found.
     *    If no trouble, then a valid dataset.xml chunk has been returned.
     */
    public static String generateDatasetsXml(boolean includeInstructions, boolean displayResults,
        String tLocalWaterMLUrl, String tSiteCode, String tAgency, 
        String tParameterCode, String tCharacteristicName, 
        String tStatisticCode, String tStatisticName,
        String tDataVariables[],
        String tEndDate,
        Table tStationTable,
        String tInfoUrl, String tInstitution, String otherSummary,
        String startTitle, String tQualificationCodes) throws Throwable {

        String2.log("\n*** EDDTableFromNWISDV.generateDatasetsXml " +
            "param=" + tParameterCode + " stat=" + tStatisticCode + 
            "\n  tCharacteristicName=" + tCharacteristicName +
            "\n  tLocalWaterMLUrl=" + tLocalWaterMLUrl);

        String tPublicWaterMLUrl = convertToPublicSourceUrl(tLocalWaterMLUrl);
        String tStationTableColumnNames = String2.toCSSVString(tStationTable.getColumnNames());
        int otStationTableNCols = tStationTable.nColumns();
        tStatisticName = tStatisticName.toLowerCase();
        if (tCharacteristicName.length() == 0)
            tCharacteristicName = "paramCode=" + tParameterCode;
        if (tStatisticName.length() == 0)
            tStatisticName = "statisticCode=" + tStatisticCode;

        //read some values and see what variables it returns
        //http://interim.waterservices.usgs.gov/NWISQuery/GetDV1?
        //SiteNum=07334200&ParameterCode=00060&StatisticCode=00003
        //&AgencyCode=&StartDate=2000-11-16&EndDate=2000-12-15&action=Submit
        //most params are digits only, so don't need percent encoding
        String tStartDate = Calendar2.formatAsISODate(
            Calendar2.isoDateTimeAdd(tEndDate, -5, Calendar2.DAY_OF_YEAR)); 
        String valuesUrl = tLocalWaterMLUrl + 
            "?SiteNum=" + tSiteCode + 
            "&ParameterCode=" + tParameterCode +
            "&StatisticCode=" + tStatisticCode + 
            "&AgencyCode=" + SSR.minimalPercentEncode(tAgency) +
            "&StartDate=" + tStartDate +
            "&EndDate=" + tEndDate +
            "&action=Submit";
        if (verbose) String2.log(valuesUrl);
        Table valuesTable;
        try {
            valuesTable = getValuesTable(SSR.getUncompressedUrlInputStream(valuesUrl));
        } catch (Throwable t) {
            String2.log(valuesUrl + "\n" +
                MustBe.throwableToString(t) +
                "Trying again...");
            Math2.gc(1000); //generateDatasetsXml: if trouble, pause, then try again.
            valuesTable = getValuesTable(SSR.getUncompressedUrlInputStream(valuesUrl));
        }

        if (valuesTable == null || valuesTable.nRows() == 0)
            throw new SimpleException("No data was found!");
        if (displayResults && reallyVerbose) 
            String2.log("valuesTable=\n" + valuesTable.toCSVString());

        //do things with/to the variable 
        int nValuesTableVars = valuesTable.nColumns();
        for (int dv = 0; dv < nValuesTableVars; dv++) {
            String colName = valuesTable.getColumnName(dv);
            if (tDataVariables == null ||
                colName.equals("time") ||
                String2.indexOf(tDataVariables, colName) >= 0) {

                //add the tDataVariables from the values table to the tStationTable
                Attributes atts = valuesTable.columnAttributes(dv);
                int col = tStationTable.addColumn(tStationTable.nColumns(),
                    colName, valuesTable.getColumn(dv), atts);
            }
        }
        tStationTable.makeColumnsSameSize();


        //*** basically, use tStationTable for sourceAttributes 
        //and make a parallel table to hold the addAttributes
        Table dataSourceTable = tStationTable;
        Table dataAddTable = new Table();
        int nCols = dataSourceTable.nColumns();
        StringArray nonQualifierVars = new StringArray();
        for (int col = 0; col < nCols; col++) { 
           String sourceName = dataSourceTable.getColumnName(col);
           Attributes addAtts = (Attributes)dataSourceTable.columnAttributes(col).clone();
           dataSourceTable.columnAttributes(col).clear();
           String destName = suggestDestinationName(sourceName, 
               addAtts.getString("units"), 
               addAtts.getString("positive"), 
               Float.NaN, true);

           //constructor won't read source atts
           //so put all atts in add atts
           Attributes tAtts = makeReadyToUseAddVariableAttributesForDatasetsXml(
               addAtts, sourceName, true, true); //addColorBarMinMax, tryToFindLLAT
           addAtts.add(tAtts);  //tAtts have precedence

           //add a similar column to dataAddTable
           dataAddTable.addColumn(col, destName, 
               PrimitiveArray.factory(dataSourceTable.getColumn(col).elementClass(), 1, false),
               addAtts); 

           //modify the addAtts
           if (destName.endsWith("Qualifiers")) 
               //add: the entire list of known qualifiers
               addAtts.add("description", tQualificationCodes);           
           else if (col > otStationTableNCols) { //=otStationTableNCols column is time
               nonQualifierVars.add(destName);
               addAtts.remove("options");
               //there are probably lots of units that need conversion to UDUNITS, 
               //I don't have a system for finding/fixing them
               String tUnits = addAtts.getString("units");
               if ("deg C".equals(tUnits))
                   addAtts.set("units", "degree_C");
               else if ("deg F".equals(tUnits))
                   addAtts.set("units", "degree_F");
           }

           if (destName.equals("agency") ||
               destName.startsWith("site") ||
               destName.equals("network"))                
               addAtts.add("ioos_category", "Identifier");

           if (destName.equals("agencySiteCode")) {
               addAtts.add("cf_role", "timeseries_id");
               addAtts.add("description", "Unlike any other single variable, agencySiteCode unambiguously identifies a site.");
               addAtts.add("ioos_category", "Identifier");
               addAtts.add("long_name", "Agency:SiteCode");
           }

           if (destName.equals("time")) {
               addAtts.add("long_name", "Time at Start of Day");
               addAtts.add("units", "yyyy-MM-dd'T'HH:mm:ss");
           }

           if (destName.equals("srs")) 
               addAtts.add("long_name", "Spatial Reference System");

           if (destName.equals("vertDatum")) 
               addAtts.add("long_name", "Vertical Datum");

           if (destName.equals("beginDate")) {
               addAtts.add("ioos_category", "Time");
               addAtts.add("long_name", "Start of Data Collection");
               addAtts.add("units", "yyyy-MM-dd");
           }

           if (destName.equals("endDate")) {
               addAtts.add("comment", "A missing value indicates that data is available to the present.");
               addAtts.add("ioos_category", "Time");
               addAtts.add("long_name", "End of Data Collection");
               addAtts.add("units", "yyyy-MM-dd");
           }
        }

        //work on globalAttributes        
        String tSummary = "This dataset has " + tCharacteristicName + 
            " (" + tStatisticName + ") (" + nonQualifierVars.toString() + 
            ") data from the " + tInstitution + 
            " National Water Information System (NWIS) WaterML data service.  " +
            otherSummary;
        //e.g.,         NWIS Daily Value, Stream flow, Mean (Discharge) 
        String tTitle = startTitle + ", " + tCharacteristicName + 
            " (" + tStatisticName + "): " + nonQualifierVars.toString();
        dataAddTable.globalAttributes()
            .add("cdm_data_type", "Station")
            .add("cdm_timeseries_variables", tStationTableColumnNames)
            .add("subsetVariables",       tStationTableColumnNames)
            .add("dataVariables", nonQualifierVars.toString())
            .add("drawLandMask", "under")
            .add("infoUrl", tInfoUrl)
            .add("institution", tAgency)
            .add("license", //modified slightly from comment at top of wqx response
"Some of this data may be provisional and subject to revision. The data are\n" +
"released on the condition that neither the USGS nor the United States\n" +
"Government may be held liable for any damages resulting from its authorized\n" +
"or unauthorized use.\n" +
"\n" +
"[standard]")
            .add("parameterCode", tParameterCode)
            .add("parameterName", tCharacteristicName)
            .add("statisticCode", tStatisticCode)
            .add("statisticName", tStatisticName)
            .add("summary", tSummary) 
            .add("title", tTitle);
        //String2.log("\ndataAddTable=\n" + dataAddTable.toCSSVString());

        //write the information
        StringBuilder sb = new StringBuilder();
        String tDatasetID = suggestDatasetID(
            tPublicWaterMLUrl + "?" + tParameterCode + "_" + tStatisticCode + "_" +
            nonQualifierVars.toString());
        sb.append(
            (includeInstructions? directionsForGenerateDatasetsXml() + "-->\n\n" : "") +
            "<dataset type=\"EDDTableFromNWISDV\" datasetID=\"" + tDatasetID + 
                "\" active=\"true\">\n" +
            "    <sourceUrl>" + tLocalWaterMLUrl + "</sourceUrl>\n" +
            "    <reloadEveryNMinutes>1000000000</reloadEveryNMinutes>\n"); //no point in reloading
        sb.append(writeAttsForDatasetsXml(false, dataSourceTable.globalAttributes(), "    "));
        //sb.append(cdmSuggestion());  //no, this method does a good job of setting up cdm
        sb.append(writeAttsForDatasetsXml(true,     dataAddTable.globalAttributes(), "    "));        
        sb.append(writeVariablesForDatasetsXml(dataSourceTable, dataAddTable, 
            "dataVariable", 
            true, false, false)); // includeDataType, tryToFindLLAT, questionDestinationName
        sb.append(
            "</dataset>\n" +
            "\n");
        if (displayResults) {
            String2.log("\n\n");
            String2.log(sb.toString());
        }

        String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
        return sb.toString();
    }

    /**
     * Bob used this to generate all of the NWIS Daily Value datasetsXml.
     */
    public static void bobGenerateNWISDVDatasetsXml() throws Throwable {
        String2.log("\n*** bobGenerateNWISDVDatasetsXml()");
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);
        String dataStationsDir    = "c:/data/waterML/briarDatasetStations/";
        String paramCodesFileName = "c:/data/waterML/nwisParametercodes.tsv";
        String outputFileName     = "c:/data/waterML/NWISDVDatasets" + today + ".xml";
        String logFileName        = "c:/data/waterML/NWISDVDatasetFailures" + today + ".txt";
        FileWriter logFile = new FileWriter(logFileName, true);  //append
        String subsetDir          = EDStatic.contentDirectory + "subset/";
        String fileNames[] = RegexFilenameFilter.list(dataStationsDir, ".*\\.json");
        StringBuilder results  = new StringBuilder();
        int nSkip = 0;
        int nFailures = 0;
        logFile.write("\n*** bobGenerateNWISDVDatasetsXml() " + today + "\n");

        //get the parameter codes table
        Table paramCodes = new Table();
        paramCodes.readASCII(paramCodesFileName,
            15, 17, null, null, null, null, false);  //don't simplify
        String2.log("paramCodes=\n" + paramCodes.dataToCSVString(3));
        Test.ensureEqual(String2.toCSSVString(paramCodes.getColumnNames()), 
            "parameter_cd, parameter_group_nm, parameter_nm, casrn, srsname, parameter_units", "");
        StringArray pcParamPA      = (StringArray)paramCodes.getColumn(0);
        StringArray pcParamName1PA = (StringArray)paramCodes.getColumn(2); //e.g., Discharge
        StringArray pcParamName2PA = (StringArray)paramCodes.getColumn(4); //e.g., Stream flow...


        //make a dataset for each .json file
        int nFileNames = fileNames.length;   //or smaller number for testing
        for (int fileNamei = 0; fileNamei < nFileNames; fileNamei++) {
            String fileName = fileNames[fileNamei]; //"00010_00001_0.json"
            String2.log("\n*** " +fileNamei + " of " + nFileNames + ": " + fileName);
            try {
                //get param and statistic info from fileName
                String fileNameParts[] = String2.split(fileName, '_');
                String tParamCode = fileNameParts[0];
                //statisticsCode
                String tStatisticCode = fileNameParts[1];
                int statistici = String2.parseInt(tStatisticCode);
                if (statistici < 1 || statistici > 24) {
                    nSkip++;
                    String msg = "SKIP: #" + fileNamei + "=" + fileName + " has statisticCode=" + 
                        tStatisticCode + ".\n";
                    String2.log(msg);
                    logFile.write(msg);
                    continue;
                }
                String tStatisticName = NWISStatisticNames[statistici];
                if (tStatisticName.length() == 0) {
                    String msg = "WARNING: #" + fileNamei + "=" + fileName + 
                        " has statisticCode=" + tStatisticCode + " with name=\"\".\n";
                    String2.log(msg);
                    logFile.write(msg);
                }
                //paramCode
                int paramCodePo = pcParamPA.indexOf(tParamCode);
                String tParamName = "";
                if (paramCodePo < 0) {
                    String msg = "WARNING: #" + fileNamei + "=" + fileName + " has paramCode=" + 
                        tParamCode + ": not found.\n";
                    String2.log(msg);
                    logFile.write(msg);
                } else {
                    tParamName = pcParamName2PA.get(paramCodePo);
                    if (tParamName.length() == 0) {
                        String msg = "WARNING: #" + fileNamei + "=" + fileName + " has paramCode=" + 
                            tParamCode + " with name2=\"\".\n";
                        String2.log(msg);
                        logFile.write(msg);
                        //get name1
                        tParamName = pcParamName1PA.get(paramCodePo);
                        if (tParamName.length() == 0) {
                            msg = "WARNING: #" + fileNamei + "=" + fileName + " has paramCode=" + 
                                tParamCode + " with name1=\"\".\n";
                            String2.log(msg);
                            logFile.write(msg);
                        }
                    }
                }

                //read the proto-stationTable
                Table stationTable = new Table();
                stationTable.readJson(dataStationsDir + fileName);
                Test.ensureEqual(stationTable.getColumnNames(),
                    new String[]{
                        "agency", "siteCode", "siteID", "siteName", "siteType", 
                        "network", "longitude", "latitude", "srs", "altitude", 
                        "vertDatum", "county", "state", "country", "beginDate", 
                        "endDate", "variables"}, 
                    "Unexpected columnNames in stationTable.");
                StringArray agencyPA   = (StringArray)stationTable.getColumn("agency");
                StringArray siteCodePA = (StringArray)stationTable.getColumn("siteCode");
                StringArray endDatePA  = (StringArray)stationTable.getColumn("endDate");
                PrimitiveArray longitudePA  = stationTable.getColumn("longitude");
                PrimitiveArray latitudePA   = stationTable.getColumn("latitude");
                int nRows = endDatePA.size();

                //remove stations with longitude==0 and latitude==0
                BitSet keep = new BitSet(nRows);
                for (int row = 0; row < nRows; row++) 
                    keep.set(row, 
                        (latitudePA.getDouble(row) == 0 &&
                         longitudePA.getDouble(row) == 0)? false : true);
                stationTable.justKeep(keep);
                nRows = endDatePA.size();
                                               
                //add col0= agencySiteCode
                StringArray agencySiteCode = new StringArray(nRows, false);
                for (int row = 0; row < nRows; row++) 
                    agencySiteCode.add(row, agencyPA.get(row) + ":" + siteCodePA.get(row));
                stationTable.addColumn(0, "agencySiteCode", agencySiteCode, new Attributes());
                int nCols = stationTable.nColumns();

                //revise the stationTable (save it below)
                //sort by agencySiteCode   //probably already is
                stationTable.leftToRightSortIgnoreCase(1);
                //remove 'variables' column at end
                String variables[] = StringArray.arrayFromCSV(
                    stationTable.getStringData(nCols - 1, 0));
                stationTable.removeColumn(nCols - 1);

                //make the starterTable for generateDatasetsXml
                Table starterTable = stationTable.subset(0, 1, 0);

                String tResults = null;
                int tryStation = 0;
                while (tResults == null) {
                    if (tryStation >= nRows)
                        throw new RuntimeException("!!! No more stations to try!");
                    try {
                        tResults = generateDatasetsXml(false, fileNamei == 0,
                            "http://interim.waterservices.usgs.gov/NWISQuery/GetDV1",
                            siteCodePA.get(tryStation), //"01018035", 
                            agencyPA.get(tryStation), //"USGS", 
                            //???fix up  e.g., Water Temperature?
                            tParamCode, tParamName, //"00010", "Temperature, water",  
                            tStatisticCode, tStatisticName, //"00001", "maximum",
                            variables,
                            endDatePA.get(tryStation), //"2008-11-14", 
                            starterTable,
                            "http://waterservices.usgs.gov/rest/USGS-DV-Service.html", //infoUrl
                            "USGS", //institution
                            NWISDVOtherSummary,
                            "NWIS Daily Values",  //start of title
                            NWISDailyValueQualificationCodes);                
                    } catch (Exception e) {
                        logFile.write("tryStation=" + tryStation + " failed:\n" + 
                            MustBe.throwableToString(e));
                    }
                    tryStation++;
                }

                results.append(tResults);

                //if endDate >2010-12-10 (12-15 in the files), set to "" ('present')
                //(endDatePA is part of stationTable, which is saved below)
                for (int row = 0; row < nRows; row++) {
                    String date = endDatePA.get(row);
                    if (String2.max("2010-12-10", date).equals(date))
                        endDatePA.set(row, "");
                }

                //extract the datasetID and save stationTable with that name
                int idPo1 = tResults.indexOf("datasetID=\"");
                idPo1 += 11;
                int idPo2 = tResults.indexOf('\"', idPo1);
                String tDatasetID = tResults.substring(idPo1, idPo2);
                stationTable.saveAsJson(subsetDir + tDatasetID + ".json", -1, false);

            } catch (Throwable t) {
                String msg = "ERROR for #" + fileNamei + "=" + fileName + ":\n" +
                    MustBe.throwableToString(t);
                String2.log(msg);
                nFailures++;
                logFile.write(msg);
                logFile.write("\n");
            }
        }

        //write the results to 
        String2.writeToFile(outputFileName, results.toString());
        String msg = 
            "\n" +
            "*** DONE.  nSkip=" + nSkip + " nFailures=" + nFailures + "\n" +
            "logFileName=" + logFileName + "\n" +
            "datasets.xml is in " + outputFileName + "\n" +
            "bobGenerateNWISDVDatasetsXml() finished successfully nDatasets=" + 
                (fileNames.length - nFailures - nSkip) + " of " + fileNames.length + "\n";
        String2.log(msg);
        logFile.write(msg);
        logFile.close(); 

    }
    
    /**
     * This tests generateDatasetsXml.
     */
    public static void testGenerateDatasetsXml() throws Throwable {
        Table starterTable = new Table();
        starterTable.readJson("c:/data/waterML/briarDatasetStations/00010_00001_0.json");
        int nCols = starterTable.nColumns();
        int nRows = starterTable.nRows();
        //remove 'variables' at end
        starterTable.removeColumn(nCols - 1); 
        starterTable.addColumn(0, "agencySiteCode", 
            PrimitiveArray.factory(String.class, nRows, ""), //values are irrelevant
            new Attributes());

        String results = generateDatasetsXml(true, true, //includeInstructions, displayResults
            "http://waterservices.usgs.gov/nwis/dv",
            "01018035", "USGS", 
            //???fix up  e.g., Water Temperature?
            "00010", "Temperature, water",  
            "00001", "MAXIMUM",
            new String[]{"Temperature, water", "Temperature, waterQualifiers"},
            "2008-11-14", starterTable,
            "http://waterservices.usgs.gov/rest/USGS-DV-Service.html", //infoUrl
            "USGS", //institution
            NWISDVOtherSummary,
            "NWIS Daily Values",  //start of title
            NWISDailyValueQualificationCodes);

        String expected = 
directionsForGenerateDatasetsXml() +
"-->\n" +
"\n" +
"<dataset type=\"EDDTableFromNWISDV\" datasetID=\"usgs_waterservices_5411_c757_e669\" active=\"true\">\n" +
"    <sourceUrl>http://interim.waterservices.usgs.gov/NWISQuery/GetDV1</sourceUrl>\n" +
"    <reloadEveryNMinutes>1000000000</reloadEveryNMinutes>\n" +
"    <!-- sourceAttributes>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Station</att>\n" +
"        <att name=\"cdm_timeseries_variables\">agencySiteCode, agency, siteCode, siteID, siteName, siteType, network, longitude, latitude, srs, altitude, vertDatum, county, state, country, beginDate, endDate</att>\n" +
"        <att name=\"dataVariables\">Temperature_water</att>\n" +
"        <att name=\"drawLandMask\">under</att>\n" +
"        <att name=\"infoUrl\">http://waterservices.usgs.gov/rest/USGS-DV-Service.html</att>\n" +
"        <att name=\"institution\">USGS</att>\n" +
"        <att name=\"license\">Some of this data may be provisional and subject to revision. The data are\n" +
"released on the condition that neither the USGS nor the United States\n" +
"Government may be held liable for any damages resulting from its authorized\n" +
"or unauthorized use.\n" +
"\n" +
"[standard]</att>\n" +
"        <att name=\"parameterCode\">00010</att>\n" +
"        <att name=\"parameterName\">Temperature, water</att>\n" +
"        <att name=\"statisticCode\">00001</att>\n" +
"        <att name=\"statisticName\">maximum</att>\n" +
"        <att name=\"subsetVariables\">agencySiteCode, agency, siteCode, siteID, siteName, siteType, network, longitude, latitude, srs, altitude, vertDatum, county, state, country, beginDate, endDate</att>\n" +
"        <att name=\"summary\">This dataset has Temperature, water (maximum) (Temperature_water) data from the USGS National Water Information System (NWIS) WaterML data service.  Daily values are derived by summarizing time-series data for each day for the period of record. These time-series data are used to calculate daily value statistics.  Daily data includes approved, quality-assured data that may be published, and provisional data, whose accuracy has not been verified -- always check the Qualifier variables.\n" +
"\n" +
"NOTE: To get actual daily value data (not just station information), you must request data for just one station at a time.</att>\n" +
"        <att name=\"title\">NWIS Daily Values, Temperature, water (maximum): Temperature_water</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>agencySiteCode</sourceName>\n" +
"        <destinationName>agencySiteCode</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"cf_role\">timeseries_id</att>\n" +
"            <att name=\"description\">Unlike any other single variable, agencySiteCode unambiguously identifies a site.</att>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Agency:SiteCode</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>agency</sourceName>\n" +
"        <destinationName>agency</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Agency</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>siteCode</sourceName>\n" +
"        <destinationName>siteCode</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Site Code</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>siteID</sourceName>\n" +
"        <destinationName>siteID</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Site ID</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>siteName</sourceName>\n" +
"        <destinationName>siteName</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Site Name</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>siteType</sourceName>\n" +
"        <destinationName>siteType</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Site Type</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>network</sourceName>\n" +
"        <destinationName>network</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Network</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>longitude</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>latitude</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>srs</sourceName>\n" +
"        <destinationName>srs</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Spatial Reference System</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>altitude</sourceName>\n" +
"        <destinationName>altitude</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Altitude</att>\n" +
"            <att name=\"standard_name\">altitude</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>vertDatum</sourceName>\n" +
"        <destinationName>vertDatum</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Vertical Datum</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>county</sourceName>\n" +
"        <destinationName>county</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">County</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>state</sourceName>\n" +
"        <destinationName>state</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">State</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>country</sourceName>\n" +
"        <destinationName>country</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Country</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>beginDate</sourceName>\n" +
"        <destinationName>beginDate</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Start of Data Collection</att>\n" +
"            <att name=\"units\">yyyy-MM-dd</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>endDate</sourceName>\n" +
"        <destinationName>endDate</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"comment\">A missing value indicates that data is available to the present.</att>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">End of Data Collection</att>\n" +
"            <att name=\"units\">yyyy-MM-dd</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Time at Start of Day</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"units\">yyyy-MM-dd&#39;T&#39;HH:mm:ss</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Temperature, water</sourceName>\n" +
"        <destinationName>Temperature_water</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"dataType\">Maximum</att>\n" +
"            <att name=\"ioos_category\">Hydrology</att>\n" +
"            <att name=\"long_name\">Temperature, water</att>\n" +
"            <att name=\"long_units\">degrees Celsius</att>\n" +
"            <att name=\"missing_value\" type=\"double\">-999999.0</att>\n" +
"            <att name=\"NoDataValue\" type=\"double\">-999999.0</att>\n" +
"            <att name=\"units\">degree_C</att>\n" +
"            <att name=\"valueType\">Derived Value</att>\n" +
"            <att name=\"variableCode\">NWISDV:00010</att>\n" +
"            <att name=\"variableDescription\">Temperature, water, degrees Celsius</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Temperature, waterQualifiers</sourceName>\n" +
"        <destinationName>Temperature_waterQualifiers</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"description\">Daily Value Qualification Codes (dv_rmk_cd)\n" +
"\n" +
"Code Description\n" +
"---- ----------------------------\n" +
"A    Approved for publication -- Processing and review completed.\n" +
"e    Value has been edited or estimated by USGS personnel\n" +
"&amp;    Value was computed from affected unit values\n" +
"E    Value was computed from estimated unit values.\n" +
"P    Provisional data subject to revision.\n" +
"&lt;    The value is known to be less than reported value.\n" +
"&gt;    The value is known to be greater than reported value.\n" +
"1    Value is write protected without any remark code to be printed\n" +
"2    Remark is write protected without any remark code to be printed\n" +
"Ssn  Parameter monitored seasonally\n" +
"Ice  Ice affected\n" +
"Pr   Partial-record site\n" +
"Rat  Rating being developed or revised\n" +
"Nd   Not determined\n" +
"Eqp  Equipment malfunction\n" +
"Fld  Flood damage\n" +
"Dry  Zero flow\n" +
"Dis  Data-collection discontinued\n" +
"--   Parameter not determined\n" +
"***  Temporarily unavailable\n" +
"</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"            <att name=\"long_name\">Temperature, Water Qualifiers</att>\n" +
"            <att name=\"network\">USGS</att>\n" +
"            <att name=\"vocabulary\">dv_rmk_cd</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n";
        Test.ensureEqual(results, expected, "results=\n" + results); 
    }
    


    /** This returns a table with State and N2 columns. */
    public static Table stateTable() {
        Table table = new Table();
        StringArray sn, n2;
        table.addColumn("State", sn = new StringArray());
        table.addColumn("N2",    n2 = new StringArray());
        sn.add("Alabama"); n2.add("AL");
        sn.add("Alaska");  n2.add("AK");
        sn.add("American Samoa"); n2.add("AS");
        sn.add("Arizona"); n2.add("AZ");
        sn.add("Arkansas"); n2.add("AR");
        sn.add("California"); n2.add("CA");
        sn.add("Colorado"); n2.add("CO");
        sn.add("Connecticut"); n2.add("	CT");
        sn.add("Delaware"); n2.add("DE");
        sn.add("District of Columbia"); n2.add("DC");
        sn.add("Federated States of Micronesia"); n2.add("FM");
        sn.add("Florida"); n2.add("FL");
        sn.add("Georgia"); n2.add("GA");
        sn.add("Guam"); n2.add("GU");
        sn.add("Hawaii"); n2.add("HI");
        sn.add("Idaho"); n2.add("ID");
        sn.add("Illinois"); n2.add("IL");
        sn.add("Indiana"); n2.add("IN");
        sn.add("Iowa"); n2.add("IA");
        sn.add("Kansas"); n2.add("KS");
        sn.add("Kentucky"); n2.add("KY");
        sn.add("Louisiana"); n2.add("LA");
        sn.add("Maine"); n2.add("ME");
        sn.add("Marshall Islands"); n2.add("MH");
        sn.add("Maryland"); n2.add("MD");
        sn.add("Massachusetts"); n2.add("MA");
        sn.add("Michigan"); n2.add("MI");
        sn.add("Minnesota"); n2.add("MN");
        sn.add("Mississippi"); n2.add("MS");
        sn.add("Missouri"); n2.add("MO");
        sn.add("Montana"); n2.add("MT");
        sn.add("Nebraska"); n2.add("NE");
        sn.add("Nevada"); n2.add("NV");
        sn.add("New Hampshire"); n2.add("NH");
        sn.add("New Jersey"); n2.add("NJ");
        sn.add("New Mexico"); n2.add("NM");
        sn.add("New York"); n2.add("NY");
        sn.add("North Carolina"); n2.add("NC");
        sn.add("North Dakota"); n2.add("ND");
        sn.add("Northern Mariana Islands"); n2.add("MP");
        sn.add("Ohio"); n2.add("OH");
        sn.add("Oklahoma"); n2.add("OK");
        sn.add("Oregon"); n2.add("OR");
        sn.add("Palau"); n2.add("PW");
        sn.add("Pennsylvania"); n2.add("PA");
        sn.add("Puerto Rico"); n2.add("PR");
        sn.add("Rhode Island"); n2.add("RI");
        sn.add("South Carolina"); n2.add("SC");
        sn.add("South Dakota"); n2.add("SD");
        sn.add("Tennessee"); n2.add("TN");
        sn.add("Texas"); n2.add("TX");
        sn.add("Utah"); n2.add("UT");
        sn.add("Vermont"); n2.add("VT");
        sn.add("Virgin Islands"); n2.add("VI");
        sn.add("Virginia"); n2.add("VA");
        sn.add("Washington"); n2.add("WA");
        sn.add("West Virginia"); n2.add("WV");
        sn.add("Wisconsin"); n2.add("WI");
        sn.add("Wyoming"); n2.add("WY");
        return table;
    }


    /** Bob used this to screen scrape NWIS waterML stations information
     * for the stations in the briar stations file. 
     *
     * @param justAgency use null for all. If null, info is not written to file.
     * @param firstSite use null for all 
     * @param lastSite use null for all 
     */
    public static void bobScrapeNWISStations(String justAgency,
        String firstSite, String lastSite) throws Throwable {

        String siteInfoService = "http://waterdata.usgs.gov/nwis/inventory/";
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);
        String outputFile = "c:/data/waterML/scrapeNWISStations" + today + ".json";
        String logFile = "c:/data/waterML/logScrapeNWIS" + 
            String2.replaceAll(today, "-", "") + ".txt";
        if (justAgency != null) 
            String2.setupLog(true, false, logFile, false, false, 1000000000);
        String2.log("*** EDDTableFromNWISDV bobScrapeNWISStations " + today + "\n" +
            "logFile=" + String2.logFileName() + "\n" +
            String2.standardHelpAboutMessage());

        long totalTime = System.currentTimeMillis();

        Table stateTable = stateTable();
        StringArray stNames = (StringArray)stateTable.getColumn(0);
        StringArray stCodes = (StringArray)stateTable.getColumn(1);

        //read the briar stations table
        //agency_cd	site_no	parm_cd	stat_cd	begin_date	end_date	count_nu
        //USGS	09426620	00060	00003	1988-10-01	2010-12-15	8111
        Table briarStations = new Table();
        briarStations.readASCII("c:/data/waterML/daily_value_series_catalog.20101216",
            0, 1, null, null, null, null, false);  //don't simplify
        Test.ensureEqual(String2.toCSSVString(briarStations.getColumnNames()), 
            "agency_cd, site_no, parm_cd, stat_cd, begin_date, end_date, count_nu", "");
        StringArray briarAgencyPA = (StringArray)briarStations.getColumn(0);
        StringArray briarSitePA   = (StringArray)briarStations.getColumn(1);
        briarStations.removeColumns(2, briarStations.nColumns());
        //sort it 
        briarStations.sortIgnoreCase(new int[]{0, 1}, new boolean[]{true, true});
        briarStations.removeDuplicates();
        int nBriarStations = briarStations.nRows();

        //remove rows if justAgency is active
        if (justAgency != null) {
            int po = briarAgencyPA.indexOf(justAgency);
            po = briarSitePA.indexOf(firstSite, po);
            if (po > 0) briarStations.removeRows(0, po - 1);
            po = briarSitePA.indexOf(lastSite);
            if (po > 0) briarStations.removeRows(po + 1, briarStations.nRows());
            nBriarStations = briarStations.nRows();
        }

        //add new columns        
        StringArray siteTypePA, countyPA, statePA, countryPA,
            altitudePA, vertDatumPA;
        briarStations.addColumn("siteType",  siteTypePA = new StringArray(nBriarStations, true));
        briarStations.addColumn("county",    countyPA   = new StringArray(nBriarStations, true));
        briarStations.addColumn("state",     statePA    = new StringArray(nBriarStations, true));
        briarStations.addColumn("country",   countryPA  = new StringArray(nBriarStations, true));
        briarStations.addColumn("altitude",  altitudePA = new StringArray(nBriarStations, true));
        briarStations.addColumn("vertDatum", vertDatumPA= new StringArray(nBriarStations, true));                   
        
        //go through the briarStations
        int nTotalCountyFound = 0;
        int nTotalCountyNotFound = 0;
        HashSet countyFound = new HashSet();
        HashSet totalCountyFound = new HashSet();
        int nTotalVertDatumFound = 0;
        int nTotalVertDatumNotFound = 0;
        HashSet vertDatumFound = new HashSet();
        HashSet totalVertDatumFound = new HashSet();
        HashSet siteTypes = new HashSet();
        HashSet stateSet = new HashSet();
        HashSet countrySet = new HashSet();
        HashSet onlyCity = new HashSet();

        //nBriarStations = 10; //if testing
        for (int briarStation = 0; briarStation < nBriarStations; briarStation++) {

            //get the station's info from the briar table 
            String agency    = briarAgencyPA.get(briarStation);
            String site      = briarSitePA.get(  briarStation);           
            String2.log("#" + briarStation + " of " + nBriarStations + " site=" + site);
            String siteType = "", county = "", state = "", country = "", 
                altitude = "", vertDatum = "";

            //screen scrape site info
            try {
                //http://waterdata.usgs.gov/nwis/inventory/?site_no=02289050&agency_cd=FL005&amp;
                String siteInfoUrl = siteInfoService + "?site_no=" + site + "&agency_cd=" + agency + "&amp;";
                String2.log("  siteInfoUrl=" + siteInfoUrl);
                String siteInfo = SSR.getUncompressedUrlResponseString(siteInfoUrl);
                int po1, po2;

                //fallback, get city and country from siteName    <h2>...</h2>
                String tCity = "", tCountry = "";
                po1 = siteInfo.indexOf("<h2>");
                if (po1 >= 0) {
                    po2 = siteInfo.indexOf("</h2>", po1 + 4);
                    if (po2 >= 0) { 
                        String sn = siteInfo.substring(po1 + 4, po2);
                        String snlc = sn.toLowerCase();
                        String lookFor = "";
                        po1 = snlc.lastIndexOf(lookFor = " nr ");
                        if (po1 < 0) po1 = snlc.lastIndexOf(lookFor = " near ");
                        if (po1 < 0) po1 = snlc.lastIndexOf(lookFor = " above ");
                        if (po1 < 0) po1 = snlc.lastIndexOf(lookFor = " bl ");
                        if (po1 < 0) po1 = snlc.lastIndexOf(lookFor = " below ");
                        if (po1 < 0) po1 = snlc.lastIndexOf(lookFor = " at ");
                        if (po1 < 0) {
                            po1 = snlc.indexOf(lookFor = ", ");
                            if (po1 < 0) po1 = snlc.lastIndexOf(lookFor = " ");
                            if (po1 > 0 && snlc.indexOf(", ", po1 + 1) < 0) { //last comma!
                                po2 = po1;
                                po1 = snlc.lastIndexOf(lookFor = ", ", po2-1); //previous comma
                                if (po1 < 0) po1 = snlc.lastIndexOf(lookFor = " ", po2-1); //previous space
                            }
                        }
                        if (po1 >= 0) {
                            String ts = sn.substring(po1 + lookFor.length());
                            po2 = ts.indexOf(", ");
                            if (po2 >= 0) {
                                tCity = String2.toTitleCase(ts.substring(0, po2).trim());
                                tCountry = String2.toTitleCase(ts.substring(po2 + 2).trim());
                            } else {
                                tCity = String2.toTitleCase(ts).trim();
                            }
                        }
                    }
                }

                //<h3>&nbsp; Stream Site</h3>
                po1 = siteInfo.indexOf("<h3>&nbsp; ");
                if (po1 >= 0) {
                    po2 = siteInfo.indexOf("</h3>");
                    if (po2 >= 0) {
                        siteType = siteInfo.substring(po1 + 11, po2);
                        if (siteType.endsWith(" Site")) {
                            siteType = siteType.substring(0, siteType.length() - 5).trim();
                            siteTypes.add(siteType);
                        }
                    }
                }

                //<dt><strong>DESCRIPTION:</strong></dt>
                //<dd>Latitude  35°40'26", &nbsp; Longitude  96°04'06" &nbsp; NAD27<br /></dd>
                //<dd>Okmulgee County, Oklahoma,  Hydrologic Unit 11100303</dd>
                po1 = siteInfo.indexOf("DESCRIPTION", Math.max(po1, 0));
                int descPo = po1;
                if (po1 >= 0) {
                    String lookFor = "";
                    int countyPo = siteInfo.indexOf(lookFor = " County, ", po1);
                    if (countyPo < 0)
                        countyPo = siteInfo.indexOf(lookFor = " North Star Borough, ", po1);
                    if (countyPo < 0)
                        countyPo = siteInfo.indexOf(lookFor = " Borough, ", po1);
                    if (countyPo >= 0) {
                        po2 = siteInfo.lastIndexOf(">", countyPo);  // br or dd
                        country = "US";
                        county = siteInfo.substring(po2 + 1, countyPo).trim();
                        countyFound.add(county);
                        totalCountyFound.add(county);
                        int commaPo1 = siteInfo.indexOf(",", countyPo + lookFor.length());
                        int commaPo2 = siteInfo.indexOf("<", countyPo + lookFor.length());
                        int commaPo = 
                            commaPo1 == -1? commaPo2 :
                            commaPo2 == -1? commaPo1 : Math.min(commaPo1, commaPo2);
                        //String2.log(countyPo + " " + commaPo);
                        if (commaPo >= 0 && commaPo - countyPo < 50) {
                            state = siteInfo.substring(countyPo + lookFor.length(), commaPo).trim();
                            if (state.endsWith("&nbsp;"))
                                state = state.substring(0, state.length() - 6).trim();
                            int spo = stNames.indexOfIgnoreCase(state);
                            if (spo >= 0)
                                state = stCodes.get(spo);
                        }
                    }
                }
                if (county.length() == 0) {
                    county = tCity;
                    country = tCountry;
                    if (country.length() == 0) 
                        onlyCity.add(tCity);
                }

                //<dd>Datum of gage: 632.55 feet above sea level &nbsp; NGVD29.</dd>
                //Land surface altitude: 680\nfeet above sea level NAVD88.
                double tAlt = Double.NaN;
                String tUnit = "feet above sea level";
                int unitPo = siteInfo.indexOf(tUnit, Math.max(po1, 0));
                if (unitPo >= 0) {
                    //feet to meters  to 1 decimal places  
                    po2 = siteInfo.lastIndexOf(" ", unitPo - 2);
                    tAlt = Math2.roundTo(
                        String2.parseDouble(
                            String2.replaceAll(siteInfo.substring(po2 + 1, unitPo).trim(), ",", "")) * 0.3048, 2); 
                } else {
                    tUnit = "meters above sea level";
                    unitPo = siteInfo.indexOf(tUnit, Math.max(po1, 0));
                    if (unitPo >= 0) {
                        po2 = siteInfo.lastIndexOf(" ", unitPo - 2);
                        tAlt = String2.parseDouble(
                            String2.replaceAll(siteInfo.substring(po2 + 1, unitPo).trim(), ",", "")); 
                    }
                }
                if (!Double.isNaN(tAlt)) {
                    unitPo += tUnit.length();
                    po2 = siteInfo.indexOf(".", unitPo);
                    if (po2 >= 0 && po2 - unitPo < 20) {
                        po1 = siteInfo.lastIndexOf(" ", po2 - 1);
                        if (po1 >= 0 && po2 - po1 < 15) {
                            altitude = "" + tAlt;
                            vertDatum = siteInfo.substring(po1, po2).trim();
                            vertDatumFound.add(vertDatum);
                            totalVertDatumFound.add(vertDatum);
                        }
                    }
                }

                siteTypePA .set(briarStation, siteType);
                countyPA   .set(briarStation, county);
                statePA    .set(briarStation, state);
                countryPA  .set(briarStation, country);
                altitudePA .set(briarStation, altitude);
                vertDatumPA.set(briarStation, vertDatum);

                stateSet.add(state);
                countrySet.add(country);

                if (descPo < 0) {
                    String2.log("  DESCRIPTION not found!");
                } else {
                    String2.log("  siteType=" + siteType + 
                        " county=" + county + ", " + state + ", " + country + 
                        " altitude=" + altitude + " " + vertDatum);
                    String si = siteInfo.substring(descPo, descPo + 400);
                    if ((county.length() == 0 && si.indexOf("County") > 0) ||
                        (altitude.length() == 0 && si.indexOf("above sea level") > 0))
                        String2.log(si + "\n");
                }

            } catch (Throwable t) {
                String2.log(MustBe.throwableToString(t));
            }
            if (county.length() > 0) nTotalCountyFound++;
            else                     nTotalCountyNotFound++;
            
            if (vertDatum.length() > 0) nTotalVertDatumFound++;
            else                        nTotalVertDatumNotFound++;

        }
        if (justAgency == null) {
            String2.log(briarStations.dataToCSVString(Math.min(100, nBriarStations)));
            briarStations.saveAsJson(outputFile, -1, false);
        } else {
            briarStations.saveAsJson(System.out, -1, false);
        }

        String2.log("\n*** EDDTableFromNWISDV.bobSrapeNWISStations finished." +
            "\nsiteTypes: " + String2.toCSSVString(siteTypes.toArray()) +
            "\nnTotalCounty succeeded=" + nTotalCountyFound +
                " failed=" + nTotalCountyNotFound + 
                "\n  county: " + 
                    String2.toCSSVString(totalCountyFound.toArray()) +
            "\nnTotalVertDatum succeeded=" + nTotalVertDatumFound +
                " failed=" + nTotalVertDatumNotFound + 
                "\n  totalVertDatums: " + 
                    String2.toCSSVString(totalVertDatumFound.toArray()) +
            "\nOnlyCity: " + String2.toCSSVString(onlyCity.toArray()) +
            "\nStates: " + String2.toCSSVString(stateSet.toArray()) +
            "\nCountries: " + String2.toCSSVString(countrySet.toArray()) +
            "\nTotalTime=" + 
            Calendar2.elapsedTimeString(System.currentTimeMillis() - totalTime));
        if (justAgency != null)
            String2.returnLoggingToSystemOut();
        String2.getStringFromSystemIn("Press enter to continue..."); 
    }

    /** Bob used this to screen scrape NWIS waterML stations information
     * to get station names for stations in the briar stations file. 
     *
     */
    public static void bobScrapeNWISStationNames(String justAgency, 
        String firstSite, String lastSite) throws Throwable {

        String siteInfoService = "http://waterdata.usgs.gov/nwis/inventory/";
        String2.log("*** EDDTableFromNWISDV bobScrapeNWISStationNames");

        //read the briar stations table
        //agency_cd	site_no	parm_cd	stat_cd	begin_date	end_date	count_nu
        //USGS	09426620	00060	00003	1988-10-01	2010-12-15	8111
        Table briarStations = new Table();
        briarStations.readASCII("c:/data/waterML/daily_value_series_catalog.20101216",
            0, 1, null, null, null, null, false);  //don't simplify
        Test.ensureEqual(String2.toCSSVString(briarStations.getColumnNames()), 
            "agency_cd, site_no, parm_cd, stat_cd, begin_date, end_date, count_nu", "");
        StringArray briarAgencyPA = (StringArray)briarStations.getColumn(0);
        StringArray briarSitePA   = (StringArray)briarStations.getColumn(1);
        briarStations.removeColumns(2, briarStations.nColumns());
        //sort it 
        briarStations.sortIgnoreCase(new int[]{0, 1}, new boolean[]{true, true});
        briarStations.removeDuplicates();
        int nBriarStations = briarStations.nRows();


        //nBriarStations = 10; //if testing
        for (int briarStation = 0; briarStation < nBriarStations; briarStation++) {

            //get the station's info from the briar table 
            String agency    = briarAgencyPA.get(briarStation);
            String site      = briarSitePA.get(  briarStation);
            if (!justAgency.equals(agency) ||
                String2.min(site, firstSite).equals(site) ||
                String2.max(site, lastSite).equals(site))
                continue;

            //screen scrape site info
            try {
                //http://waterdata.usgs.gov/nwis/inventory/?site_no=02289050&agency_cd=FL005&amp;
                String siteInfoUrl = siteInfoService + "?site_no=" + site + "&agency_cd=" + agency + "&amp;";
                //String2.log("  siteInfoUrl=" + siteInfoUrl);
                String siteInfo = SSR.getUncompressedUrlResponseString(siteInfoUrl);
                int po1, po2;

                //<h2>...</h2>
                po1 = siteInfo.indexOf("<h2>");
                if (po1 >= 0) {
                    po2 = siteInfo.indexOf("</h2>");
                    if (po2 >= 0) 
                        String2.log(siteInfo.substring(po1 + 4, po2));
                }

            } catch (Throwable t) {
                String2.log(MustBe.throwableToString(t));
            }
        }
    }

    
    /** Bob used this to generate the NWIS waterML stations table for each dataset 
     * from the stations table from David Briar email 2010-12-16. 
     *
     * This uses station info from bobScrapeNWISStations() in F:/data/waterML/scrapeNWISStations.json.
     */
    public static void bobMakeNWISDatasets() throws Throwable {
        String stationsFileName = "c:/data/waterML/scrapeNWISStations20110123.json";
        String serviceUrl = "http://interim.waterservices.usgs.gov/NWISQuery/GetDV1";
        String siteInfoService = "http://waterdata.usgs.gov/nwis/inventory";
        String paramCodesFileName = "c:/data/waterML/nwisParametercodes.tsv";
        String dsDir   = "c:/data/waterML/briarDatasetStations/";
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);
        String logFile = "c:/data/waterML/logNWISDatasets" + 
            String2.replaceAll(today, "-", "") + ".txt";
        String2.setupLog(true, false, logFile, false, false, 1000000000);
        String2.log("*** EDDTableFromNWISDV bobMakeNWISDatasets " + today + "\n" +
            "logFile=" + String2.logFileName() + "\n" +
            String2.standardHelpAboutMessage());
        long totalTime = System.currentTimeMillis();

        //get the parameter codes table
        Table paramCodes = new Table();
        paramCodes.readASCII(paramCodesFileName,
            15, 17, null, null, null, null, false);  //don't simplify
        String2.log("paramCodes=\n" + paramCodes.dataToCSVString(3));
        Test.ensureEqual(String2.toCSSVString(paramCodes.getColumnNames()), 
            "parameter_cd, parameter_group_nm, parameter_nm, casrn, srsname, parameter_units", "");
        StringArray pcParamPA      = (StringArray)paramCodes.getColumn(0);
        StringArray pcParamName1PA = (StringArray)paramCodes.getColumn(2); //e.g., Discharge
        StringArray pcParamName2PA = (StringArray)paramCodes.getColumn(4); //e.g., Stream flow...

        //read the scrapeNWISStation table
        //"agency_cd", "site_no", "siteType", "county", "state", "country", "altitude", "vertDatum"
        Table scrapeStations = new Table();
        scrapeStations.readJson(stationsFileName);  
        Test.ensureEqual(String2.toCSSVString(scrapeStations.getColumnNames()), 
            "agency_cd, site_no, siteType, county, state, country, altitude, vertDatum", "");
        StringArray scrapeAgencyPA    = (StringArray)scrapeStations.getColumn(0);
        StringArray scrapeSitePA      = (StringArray)scrapeStations.getColumn(1);
        StringArray scrapeSiteTypePA  = (StringArray)scrapeStations.getColumn(2);
        StringArray scrapeCountyPA    = (StringArray)scrapeStations.getColumn(3);
        StringArray scrapeStatePA     = (StringArray)scrapeStations.getColumn(4);
        StringArray scrapeCountryPA   = (StringArray)scrapeStations.getColumn(5);
        StringArray scrapeAltitudePA  = (StringArray)scrapeStations.getColumn(6);
        StringArray scrapeVertDatumPA = (StringArray)scrapeStations.getColumn(7);
        int nScrapeStations = scrapeStations.nRows();
        //sort by site, then agency for faster searches below
        scrapeStations.ascendingSort(new int[]{1,0});

        //read the briar stations table
        //agency_cd	site_no	parm_cd	stat_cd	begin_date	end_date	count_nu
        //USGS	09426620	00060	00003	1988-10-01	2010-12-15	8111
        Table briarStations = new Table();
        briarStations.readASCII("c:/data/waterML/daily_value_series_catalog.20101216",
            0, 1, null, null, null, null, false);  //don't simplify
        Test.ensureEqual(String2.toCSSVString(briarStations.getColumnNames()), 
            "agency_cd, site_no, parm_cd, stat_cd, begin_date, end_date, count_nu", "");
        StringArray briarAgencyPA = (StringArray)briarStations.getColumn(0);
        StringArray briarSitePA   = (StringArray)briarStations.getColumn(1);
        StringArray briarParamPA  = (StringArray)briarStations.getColumn(2);
        StringArray briarStatPA   = (StringArray)briarStations.getColumn(3);
        StringArray briarBeginPA  = (StringArray)briarStations.getColumn(4);
        StringArray briarEndPA    = (StringArray)briarStations.getColumn(5);
                   
        //sort it  (order is important for code below: param, stat, site, beginDate
        briarStations.ascendingSort(new int[]{2,3,0,1,4});
        
        //go through the briarStations
        int paramStatStartRow = 0;
        int nBriarStations = briarStations.nRows();
//nBriarStations = 100; //for testing
        Table datasetStations = null;
        PrimitiveArray dsAgency =  null, dsSiteCode = null, dsSiteID = null, dsSiteName = null,
            dsSiteType = null, dsNetwork = null, dsLongitude = null, dsLatitude = null,
            dsSrs = null, dsAltitude = null, dsVertDatum = null,
            dsCountry = null, dsState = null, dsCounty = null,
            dsBeginDate = null, dsEndDate = null, dsVariables = null;
        int nTotalGetDataSucceeded = 0;
            int nGetDataSucceeded = 0;
            int nTotalGetDataFailed = 0;
            StringArray getDataFailed = new StringArray();
        int nParamNotInParamList = 0;
        int nParamStatCombos = 0;
        HashSet variablesSeen = new HashSet();

        int briarStart = 0;   //use another number to restart in the middle
        for (int briarStation = briarStart; briarStation < nBriarStations; briarStation++) {

            //get the station's info from the briar table 
            String agency    = briarAgencyPA.get(briarStation);
            String site      = briarSitePA.get(  briarStation);
            String param     = briarParamPA.get( briarStation);
            String stat      = briarStatPA.get(  briarStation);
            String beginDate = briarBeginPA.get( briarStation);
            String endDate   = briarEndPA.get(   briarStation); 
            String siteName  = "";
            String siteType  = "";
            String longitude = "";
            String latitude  = "";
            String srs       = "";
            String altitude  = "";
            String vertDatum = "";
            String country   = "";
            String state     = "";
            String county    = "";
            String network   = "";
            String siteID    = "";
            String variables = "";
//for testing, just 00052 + 00003
//if (!param.equals("00052") || !stat.equals("00003"))
//   continue;
            String2.log("#" + briarStation + " of " + nBriarStations + 
                " param=" + param + " stat=" + stat + " site=" + site);

            //combine with next row? 
            //This occurs if station operated, then stopped (1st row), then started up again (second row)
            int oBriarStation = briarStation;
            while (briarStation + 1 < nBriarStations &&
                agency.equals(briarAgencyPA.get(briarStation + 1)) &&
                site.equals(  briarSitePA.get(  briarStation + 1)) &&
                param.equals( briarParamPA.get( briarStation + 1)) &&
                stat.equals(  briarStatPA.get(  briarStation + 1))) {
                //remember min beginDate and max endDate
                beginDate = String2.min(beginDate, briarBeginPA.get(briarStation + 1));
                endDate   = String2.max(endDate,   briarEndPA.get(  briarStation + 1));
                briarStation++;
                String2.log("  combine briarStation=" + (briarStation-1) + " and " + briarStation);
            }

            //get the paramNames
            int po = pcParamPA.indexOf(param);
            if (po < 0) {
                String2.log("  !param=" + param + " not found in " + paramCodesFileName);
                nParamNotInParamList++;
                continue;
            }
            String paramName1 = pcParamName1PA.get(po); //e.g., Discharge
            String paramName2 = pcParamName2PA.get(po); //e.g., Stream flow...  //may be ""

            //if new param+stat combo, make a new datasetStations
            if (briarStation == 0 ||
                !param.equals(briarParamPA.get(oBriarStation - 1)) ||
                !stat.equals(briarStatPA.get(oBriarStation - 1))) {
                String2.log("new param=" + param + " stat=" + stat);
                nParamStatCombos++;
                paramStatStartRow = briarStation;
                variablesSeen.clear();

                datasetStations = new Table();
                datasetStations.addColumn("agency",    dsAgency = new StringArray());
                datasetStations.addColumn("siteCode",  dsSiteCode = new StringArray());
                datasetStations.addColumn("siteID",    dsSiteID = new StringArray());
                datasetStations.addColumn("siteName",  dsSiteName = new StringArray());
                datasetStations.addColumn("siteType",  dsSiteType = new StringArray());
                datasetStations.addColumn("network",   dsNetwork = new StringArray());
                datasetStations.addColumn("longitude", dsLongitude = new DoubleArray());
                datasetStations.addColumn("latitude",  dsLatitude = new DoubleArray());
                datasetStations.addColumn("srs",       dsSrs = new StringArray());
                datasetStations.addColumn("altitude",  dsAltitude = new DoubleArray());
                datasetStations.addColumn("vertDatum", dsVertDatum = new StringArray());
                datasetStations.addColumn("county",    dsCounty = new StringArray());
                datasetStations.addColumn("state",     dsState = new StringArray()); 
                datasetStations.addColumn("country",   dsCountry = new StringArray()); 
                datasetStations.addColumn("beginDate", dsBeginDate = new StringArray());
                datasetStations.addColumn("endDate",   dsEndDate = new StringArray());
                datasetStations.addColumn("variables", dsVariables = new StringArray());

            }   

            //some datasetStations columns are filled with briarStation info (e.g., agency) above

            //get the scrapeStation info for this station  
            int ssi = scrapeSitePA.indexOf(site);
            if (ssi >= 0) {
                ssi = scrapeAgencyPA.indexOf(agency, ssi);
                if (ssi >= 0 && 
                    scrapeSitePA.get(ssi).equals(site) &&
                    scrapeAgencyPA.get(ssi).equals(agency)) {

                    siteType  = scrapeSiteTypePA.get(ssi);
                    county    = scrapeCountyPA.get(ssi);
                    state     = scrapeStatePA.get(ssi);
                    country   = scrapeCountryPA.get(ssi);
                    altitude  = scrapeAltitudePA.get(ssi);
                    vertDatum = scrapeVertDatumPA.get(ssi);
                } else {
                    String2.log("  !scrapeStation not found!");
                }
            }


            //for each briarStation, get last 2 day's data
            String tStartDate = Calendar2.formatAsISODate(
                Calendar2.isoDateTimeAdd(endDate, -1, Calendar2.DATE)); 

            String getDataUrl = serviceUrl +
                "?SiteNum=" + site +  //no need to percent encode, all digits     
                "&ParameterCode=" + param +
                "&StatisticCode=" + stat +
                "&AgencyCode=" + SSR.minimalPercentEncode(agency) +
                "&StartDate=" + tStartDate +
                "&EndDate="   + endDate +
                "&action=Submit";
            String2.log(getDataUrl);
            boolean siteOK = false;
            
            try {
                //test: does the service work?   get data from the waterML service
                Table data;
                try {
                    data = getValuesTable(
                        SSR.getUncompressedUrlInputStream(getDataUrl));
                } catch (Throwable t) {
                    //wait, then try again
                    String2.log("  ERROR getting data for site=" + site + "\n" + 
                        MustBe.throwableToString(t) +
                        "Trying again...");
                    Math2.gc(1000); //generateDatasetsXml: if trouble, pause, then try again.
                    data = getValuesTable(
                        SSR.getUncompressedUrlInputStream(getDataUrl));
                }

                Attributes gAtts = data.globalAttributes();
                
                //success: gather briarStation information, response variables
                //In general, if info is available, this is the BEST data source.
                String s;   //siteCode must be same
                s = gAtts.getString("network");       if (s != null) network = s;
                s = gAtts.getString("siteID");        if (s != null) siteID = s;
                s = gAtts.getString("siteName");      if (s != null) siteName = s;
                String tLon = gAtts.getString("longitude"); 
                String tLat = gAtts.getString("latitude");   
                String tSrs = gAtts.getString("srs");     
                if (tLon != null && tLon.length() > 0 &&
                    tLat != null && tLat.length() > 0 &&
                    tSrs != null && tSrs.length() > 0) {
                    if (String2.parseDouble(tLon) == 0 &&
                        String2.parseDouble(tLat) == 0) 
                        throw new SimpleException("Station rejected because lon=0 and lat=0!");
                    longitude = tLon;
                    latitude  = tLat;
                    srs       = tSrs;
                }
                String tAlt = gAtts.getString("altitude");      
                String tVDt = gAtts.getString("verticalDatum"); 
                if (tAlt != null && tAlt.length() > 0 &&
                    tVDt != null && tVDt.length() > 0) { 
                    altitude = tAlt;
                    vertDatum = tVDt;
                }

                //note the response variables
                if (data.getColumnName(0).equals("time"))
                    data.removeColumn(0);
                variables = String2.toCSSVString(data.getColumnNames());
                String2.log("  variables=" + variables);
                int nCols = data.nColumns();
                for (int col = 0; col < nCols; col++) {
                    String varSeen = data.getColumnName(col);
                    if (!varSeen.endsWith("Qualifiers"))
                        variablesSeen.add(varSeen);
                }

                nTotalGetDataSucceeded++;
                nGetDataSucceeded++;
                siteOK = true;                
            } catch (Throwable t) {
                //fail: no data available
//??? don't add this site to datasetStations???
                String2.log("  ERROR getting data for site=" + site + "\n" + 
                    MustBe.throwableToString(t));
                nTotalGetDataFailed++;
                getDataFailed.add(site);
            }

            if (siteOK) {           
                //okay, add this station's data to datasetStations  all at once (so safe, consistent)
                dsAgency.addString(agency);
                dsSiteCode.addString(site);
                dsSiteID.addString(siteID);
                dsSiteName.addString(siteName);
                dsSiteType.addString(siteType);
                dsNetwork.addString(network);
                dsLongitude.addString(longitude);
                dsLatitude.addString(latitude);
                dsSrs.addString(srs);
                dsAltitude.addString(altitude);
                dsVertDatum.addString(vertDatum);
                dsCountry.addString(country); 
                dsState.addString(state); 
                dsCounty.addString(county);
                dsBeginDate.addString(beginDate);
                dsEndDate.addString(endDate);
                dsVariables.addString(variables);
            }

            //end of a param+stat group
            if (briarStation == nBriarStations - 1 || 
                !param.equals(briarParamPA.get(briarStation + 1)) ||
                !stat.equals( briarStatPA.get( briarStation + 1))) {

                //diagnostic info  
                StringArray variablesSeenSA = new StringArray(
                    String2.toStringArray(variablesSeen.toArray()));
                variablesSeenSA.sortIgnoreCase();
                String2.log("\n*** End of param=" + param + " stat=" + stat + 
                        " nSites=" + dsAgency.size() +
                    "\nnGetData succeeded=" + nGetDataSucceeded + 
                        " failed=" + getDataFailed.size() + 
                        "\n  " + getDataFailed.toString() +
                    "\nvariablesSeen n=" + variablesSeenSA.size() +
                        ": " + variablesSeenSA.toString());

                if (dsAgency.size() > 0) {
                    //sort by siteCode
                    datasetStations.sort(
                        new int[]{datasetStations.findColumnNumber("siteCode")}, 
                        new boolean[]{true}); 
String2.log("\ndatasetStations=\n" + datasetStations.dataToCSVString());

                    //write stations for each variableSeen to separate datasetsStations table
                    int nDSRows = datasetStations.nRows();
                    int nVS = variablesSeenSA.size();
                    for (int vs = 0; vs < nVS; vs++) {
                        //make a subset table
                        Table subset = (Table)datasetStations.clone();
                        BitSet keep = new BitSet(nDSRows);

                        String varSeen = variablesSeenSA.get(vs);
                        String lookFor = 
                            (varSeen.indexOf(',') >= 0? String2.toJson(varSeen) : varSeen) + 
                            ", ";

                        //check each station for this variable
                        for (int dsRow = 0; dsRow < nDSRows; dsRow++) {
                            keep.set(dsRow, 
                                (dsVariables.getString(dsRow) + ", ").indexOf(lookFor) >= 0);
                        }
                        subset.justKeep(keep);
                        
                        //make new variables column  with just "varSeen, varSeenQualifiers"
                        String tvar = lookFor + 
                            (varSeen.indexOf(',') >= 0? 
                                String2.toJson(varSeen + "Qualifiers") : 
                                varSeen + "Qualifiers");
                        subset.setColumn(subset.findColumnNumber("variables"),
                            PrimitiveArray.factory(String.class, subset.nRows(), tvar));

                        //save the subset table
                        String2.log("\nwriting chunk for " + param + "_" + stat + " " + lookFor + 
                            " (nRows=" + subset.nRows() + ")\n" +
                            subset.dataToCSVString(3));
                        subset.saveAsJson(
                            dsDir + param + "_" + stat + "_" + vs + ".json",
                            -1, false);
                    }
                }

                nGetDataSucceeded = 0;
                getDataFailed.clear();
                variablesSeen.clear();

                datasetStations = null;
                dsAgency = null; //not nec to set this or others to null, but for safety
            }               
        }
        String2.log("\n*** EDDTableFromNWISDV.bobMakeNWISDatasets finished." +
            "\nnParamNotInParamList=" + nParamNotInParamList +
            "\nnParamStatCombos=" + nParamStatCombos +
            "\nnTotalGetData succeeded=" + nTotalGetDataSucceeded + 
                " failed=" + nTotalGetDataFailed + 
            "\nTotalTime=" + 
            Calendar2.elapsedTimeString(System.currentTimeMillis() - totalTime) + 
            "\nDiagnostics are in logFile=" + logFile);
        String2.returnLoggingToSystemOut();
        String2.getStringFromSystemIn("Press enter to continue..."); 
    }

    /** This runs some basic tests of a dataset from this class. */
    public static void testBasic() throws Throwable {
        EDD edd = EDD.oneFromDatasetXml("usgs_waterservices_f8b2_b8b1_96dd");
        String dir = EDStatic.fullTestCacheDirectory;
        String tName, query, tRestuls, results[], expected;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);
        Table table;

        //dds
        tName = edd.makeNewFileForDapQuery(null, null, "", dir, 
            edd.className() + "_Entire", ".dds"); 
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName);
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String agencySiteCode;\n" +
"    String agency;\n" +
"    String siteCode;\n" +
"    String siteID;\n" +
"    String siteName;\n" +
"    String siteType;\n" +
"    String network;\n" +
"    Float64 longitude;\n" +
"    Float64 latitude;\n" +
"    String srs;\n" +
"    Float64 altitude;\n" +
"    String vertDatum;\n" +
"    String county;\n" +
"    String state;\n" +
"    String country;\n" +
"    Float64 beginDate;\n" +
"    Float64 endDate;\n" +
"    Float64 time;\n" +
"    Float64 Sampling_depth;\n" +
"    String Sampling_depthQualifiers;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results[0], "", "results[0]=\n" + results[0]);
        Test.ensureEqual(results[1], expected, "results[1]=\n" + results[1]);

        //das
        tName = edd.makeNewFileForDapQuery(null, null, "", dir, 
            edd.className() + "_Entire", ".das"); 
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName);
        expected = 
"Attributes {\n" +
" s {\n" +
"  agencySiteCode {\n" +
"    String cf_role \"timeseries_id\";\n" +
"    String description \"Unlike any other single variable, agencySiteCode unambiguously identifies a site.\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Agency:SiteCode\";\n" +
"  }\n" +
"  agency {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Agency\";\n" +
"  }\n" +
"  siteCode {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Site Code\";\n" +
"  }\n" +
"  siteID {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Site ID\";\n" +
"  }\n" +
"  siteName {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Site Name\";\n" +
"  }\n" +
"  siteType {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Site Type\";\n" +
"  }\n" +
"  network {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Network\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -79.4230754, -73.1431667;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range 34.74888889, 40.9628611;\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  srs {\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Spatial Reference System\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    Float64 actual_range 0.0, 130.72;\n" +
"    String axis \"Z\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  vertDatum {\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Vertical Datum\";\n" +
"  }\n" +
"  county {\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"County\";\n" +
"  }\n" +
"  state {\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"State\";\n" +
"  }\n" +
"  country {\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Country\";\n" +
"  }\n" +
"  beginDate {\n" +
"    Float64 actual_range 8.843904e+8, 1.2876192e+9;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Start of Data Collection\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  endDate {\n" +
"    Float64 actual_range 1.0490688e+9, 1.2135744e+9;\n" +
"    String comment \"A missing value indicates that data is available to the present.\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"End of Data Collection\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time at Start of Day\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  Sampling_depth {\n" +
"    String dataType \"Maximum\";\n" +
"    String ioos_category \"Hydrology\";\n" +
"    String long_name \"Sampling depth\";\n" +
"    String long_units \"feet\";\n" +
"    Float64 missing_value -999999.0;\n" +
"    Float64 NoDataValue -999999.0;\n" +
"    String units \"ft\";\n" +
"    String valueType \"Derived Value\";\n" +
"    String variableCode \"NWISDV:00003\";\n" +
"    String variableDescription \"Sampling depth, feet\";\n" +
"  }\n" +
"  Sampling_depthQualifiers {\n" +
"    String description \"Daily Value Qualification Codes (dv_rmk_cd)\n" +
"\n" +
"Code Description\n" +
"---- ----------------------------\n" +
"A    Approved for publication -- Processing and review completed.\n" +
"e    Value has been edited or estimated by USGS personnel\n" +
"&    Value was computed from affected unit values\n" +
"E    Value was computed from estimated unit values.\n" +
"P    Provisional data subject to revision.\n" +
"<    The value is known to be less than reported value.\n" +
">    The value is known to be greater than reported value.\n" +
"1    Value is write protected without any remark code to be printed\n" +
"2    Remark is write protected without any remark code to be printed\n" +
"Ssn  Parameter monitored seasonally\n" +
"Ice  Ice affected\n" +
"Pr   Partial-record site\n" +
"Rat  Rating being developed or revised\n" +
"Nd   Not determined\n" +
"Eqp  Equipment malfunction\n" +
"Fld  Flood damage\n" +
"Dry  Zero flow\n" +
"Dis  Data-collection discontinued\n" +
"--   Parameter not determined\n" +
"***  Temporarily unavailable\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Sampling Depth Qualifiers\";\n" +
"    String network \"USGS\";\n" +
"    String vocabulary \"dv_rmk_cd\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"TimeSeries\";\n" +
"    String cdm_timeseries_variables \"agencySiteCode, agency, siteCode, siteID, siteName, siteType, network, longitude, latitude, srs, altitude, vertDatum, county, state, country, beginDate, endDate\";\n" +
"    String dataVariables \"Sampling_depth\";\n" +
"    String drawLandMask \"under\";\n" +
"    Float64 Easternmost_Easting -73.1431667;\n" +
"    Float64 geospatial_lat_max 40.9628611;\n" +
"    Float64 geospatial_lat_min 34.74888889;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max -73.1431667;\n" +
"    Float64 geospatial_lon_min -79.4230754;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    Float64 geospatial_vertical_max 130.72;\n" +
"    Float64 geospatial_vertical_min 0.0;\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"" + today + " http://interim.waterservices.usgs.gov/NWISQuery/GetDV1\n" +
today + " http://127.0.0.1:8080/cwexperimental/tabledap/usgs_waterservices_f8b2_b8b1_96dd.das\";\n" +
"    String infoUrl \"http://waterservices.usgs.gov/rest/USGS-DV-Service.html\";\n" +
"    String institution \"USGS\";\n" +
"    String license \"Some of this data may be provisional and subject to revision. The data are\n" +
"released on the condition that neither the USGS nor the United States\n" +
"Government may be held liable for any damages resulting from its authorized\n" +
"or unauthorized use.\n" +
"\n" +
"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    Float64 Northernmost_Northing 40.9628611;\n" +
"    String parameterCode \"00003\";\n" +
"    String parameterName \"Sampling depth, feet\";\n" +
"    String sourceUrl \"http://interim.waterservices.usgs.gov/NWISQuery/GetDV1\";\n" +
"    Float64 Southernmost_Northing 34.74888889;\n" +
"    String statisticCode \"00001\";\n" +
"    String statisticName \"maximum\";\n" +
"    String subsetVariables \"agencySiteCode, agency, siteCode, siteID, siteName, siteType, network, longitude, latitude, srs, altitude, vertDatum, county, state, country, beginDate, endDate\";\n" +
"    String summary \"This dataset has Sampling depth, feet (maximum) (Sampling_depth) data from the USGS National Water Information System (NWIS) WaterML data service.  Daily values are derived by summarizing time-series data for each day for the period of record. These time-series data are used to calculate daily value statistics.  Daily data includes approved, quality-assured data that may be published, and provisional data, whose accuracy has not been verified -- always check the Qualifier variables.\n" +
"\n" +
"TO USE THIS DATASET:\n" +
"To make a graph or get actual daily value data (not just station\n" +
"information), you must request data for just one station at a time.\n" +
"1) Use the 'subset' web page to select just one station.\n" +
"2) Then use 'graph' web page to Make A Graph or use the Data Access Form to\n" +
"   get data.\";\n" +
"    String title \"NWIS Daily Values, Sampling depth, feet (maximum): Sampling_depth\";\n" +
"    Float64 Westernmost_Easting -79.4230754;\n" +
"  }\n" +
"}\n";
        Test.ensureEqual(results[0], "", "results[0]=\n" + results[0]);
        Test.ensureEqual(results[1], expected, "results[1]=\n" + results[1]);

//see station info for this station at f:/data/waterML/briarDatasetStations/00003_00001_0.json

        //get data
        //["USGS01302250", "USGS", "01302250", "2458432", "EAST CREEK AT SANDS POINT NY", 
        //"Estuary", "NWISDV", -73.7101944, 40.8662222, "EPSG:4269", null, "", 
        //"Nassau", "NY", "US", "2007-12-13", ""],
//http://interim.waterservices.usgs.gov/NWISQuery/GetDV1?
//ParameterCode=00003&StatisticCode=00001&AgencyCode=USGS
//&StartDate=2010-12-01&EndDate=2010-12-04&action=Submit           
        String2.log("\n* test #1");
        query = "&agency=\"USGS\"&siteCode=\"01302250\"&time>=2010-12-01&time<=2010-12-04";             
        tName = edd.makeNewFileForDapQuery(null, null, query, dir, 
            edd.className() + "_3_1_test1", ".csv"); 
        results = String2.readFromFile(dir + tName);
        expected = 
"agencySiteCode,agency,siteCode,siteID,siteName,siteType,network,longitude,latitude,srs,altitude,vertDatum,county,state,country,beginDate,endDate,time,Sampling_depth,Sampling_depthQualifiers\n" +
",,,,,,,degrees_east,degrees_north,,m,,,,,UTC,UTC,UTC,ft,\n" +
"USGS:01302250,USGS,01302250,2458432,EAST CREEK AT SANDS POINT NY,Estuary,NWISDV,-73.7101944,40.8662222,EPSG:4269,NaN,,Nassau,NY,US,2007-12-13T00:00:00Z,,2010-12-01T00:00:00Z,3.75,P\n" +
"USGS:01302250,USGS,01302250,2458432,EAST CREEK AT SANDS POINT NY,Estuary,NWISDV,-73.7101944,40.8662222,EPSG:4269,NaN,,Nassau,NY,US,2007-12-13T00:00:00Z,,2010-12-02T00:00:00Z,3.2,P\n" +
"USGS:01302250,USGS,01302250,2458432,EAST CREEK AT SANDS POINT NY,Estuary,NWISDV,-73.7101944,40.8662222,EPSG:4269,NaN,,Nassau,NY,US,2007-12-13T00:00:00Z,,2010-12-03T00:00:00Z,3.94,P\n" +
"USGS:01302250,USGS,01302250,2458432,EAST CREEK AT SANDS POINT NY,Estuary,NWISDV,-73.7101944,40.8662222,EPSG:4269,NaN,,Nassau,NY,US,2007-12-13T00:00:00Z,,2010-12-04T00:00:00Z,3.61,P\n";
        Test.ensureEqual(results[0], "", "results[0]=\n" + results[0]);
        Test.ensureEqual(results[1], expected, "results[1]=\n" + results[1]);

        //test time before data available
        String2.log("\n* test #2");
        query = "&agency=\"USGS\"&siteCode=\"01302250\"&time>=2005-12-01&time<=2005-12-04";             
        try {
            tName = edd.makeNewFileForDapQuery(null, null, query, dir, 
                edd.className() + "_3_1_test2", ".csv"); 
            results = String2.readFromFile(dir + tName);
            String2.log("results[0]=\n" + results[0]);
            String2.log("results[1]=\n" + results[1]);
            throw new SimpleException("shouldn't get here");
        } catch (Throwable t) {
            String msg = MustBe.throwableToString(t);
            int po = msg.indexOf("SimpleException: Your query produced no matching results. " +
                "(There are no matching sites (time).)");
            if (po < 0)
                throw new SimpleException("Unexpected Error: " + msg);
        }

        //test time in future
        String2.log("\n* test #3");
        query = "&agency=\"USGS\"&siteCode=\"01302250\"&time>=2020-12-01&time<=2020-12-04";             
        try {
            tName = edd.makeNewFileForDapQuery(null, null, query, dir, 
                edd.className() + "_3_1_test3", ".csv"); 
            results = String2.readFromFile(dir + tName);
            String2.log("results[0]=\n" + results[0]);
            String2.log("results[1]=\n" + results[1]);
            throw new SimpleException("shouldn't get here");
        } catch (Throwable t) {
            String msg = MustBe.throwableToString(t);
            int po = msg.indexOf("SimpleException: Your query produced no matching results.");
            if (po < 0)
                throw new SimpleException("Unexpected Error: " + msg);
        }

        //test one time: noon (so no data)
        String2.log("\n* test #3b");
        query = "&agency=\"USGS\"&siteCode=\"01302250\"&time=2010-12-01T12:00:00";             
        try {
            tName = edd.makeNewFileForDapQuery(null, null, query, dir, 
                edd.className() + "_3_1_test3b", ".csv"); 
            results = String2.readFromFile(dir + tName);
            String2.log("results[0]=\n" + results[0]);
            String2.log("results[1]=\n" + results[1]);
            throw new SimpleException("shouldn't get here");
        } catch (Throwable t) {
            String msg = MustBe.throwableToString(t);
            int po = msg.indexOf("SimpleException: Your query produced no matching results.");
            if (po < 0)
                throw new SimpleException("Unexpected Error: " + msg);
        }

        //test   actual data for more than one station (not allowed)
        String2.log("\n* test #4");
        query = "&time=2010-12-01";             
        try {
            tName = edd.makeNewFileForDapQuery(null, null, query, dir, 
                edd.className() + "_3_1_test4", ".csv"); 
            results = String2.readFromFile(dir + tName);
            String2.log("results[0]=\n" + results[0]);
            String2.log("results[1]=\n" + results[1]);
            throw new SimpleException("shouldn't get here");
        } catch (Throwable t) {
            String msg = MustBe.throwableToString(t);
            int po = msg.indexOf("SimpleException: To get actual daily value data " +
                "(not just station information), you must request " +
                "data for just one station at a time.  (There were 9 matching stations.)");
            if (po < 0)
                throw new SimpleException("Unexpected Error: " + msg);
        }

           
        //test one station, no time specified -> all time
        //http://interim.waterservices.usgs.gov/NWISQuery/GetDV1?
        //SiteNum=0209303205&ParameterCode=00003&StatisticCode=00001&AgencyCode=USGS&action=Submit
        String2.log("\n* test #5");
        query = "&agency=\"USGS\"&siteCode=\"0209303205\"";             
        tName = edd.makeNewFileForDapQuery(null, null, query, dir, 
            edd.className() + "_3_1_test5", ".csv"); 
        results = String2.readFromFile(dir + tName);
        Test.ensureEqual(results[0], "", "results[0]=\n" + results[0]);
        expected = 
"agencySiteCode,agency,siteCode,siteID,siteName,siteType,network,longitude,latitude,srs,altitude,vertDatum,county,state,country,beginDate,endDate,time,Sampling_depth,Sampling_depthQualifiers\n" +
",,,,,,,degrees_east,degrees_north,,m,,,,,UTC,UTC,UTC,ft,\n" +
"USGS:0209303205,USGS,0209303205,2456089,\"NEW RIVER BELOW HWY17 BRIDGE AT JACKSONVILLE, NC\",Stream,NWISDV,-77.43777778,34.74888889,EPSG:4269,0.0,NAVD88,Onslow,NC,US,2007-12-22T00:00:00Z,2008-06-16T00:00:00Z,2007-12-22T00:00:00Z,4.8,P\n" +
"USGS:0209303205,USGS,0209303205,2456089,\"NEW RIVER BELOW HWY17 BRIDGE AT JACKSONVILLE, NC\",Stream,NWISDV,-77.43777778,34.74888889,EPSG:4269,0.0,NAVD88,Onslow,NC,US,2007-12-22T00:00:00Z,2008-06-16T00:00:00Z,2007-12-23T00:00:00Z,5.0,P\n" +
"USGS:0209303205,USGS,0209303205,2456089,\"NEW RIVER BELOW HWY17 BRIDGE AT JACKSONVILLE, NC\",Stream,NWISDV,-77.43777778,34.74888889,EPSG:4269,0.0,NAVD88,Onslow,NC,US,2007-12-22T00:00:00Z,2008-06-16T00:00:00Z,2007-12-24T00:00:00Z,5.0,P\n";
        Test.ensureEqual(results[1].substring(0, expected.length()), expected, "results[1]=\n" + results[1]);
        expected = 
"USGS:0209303205,USGS,0209303205,2456089,\"NEW RIVER BELOW HWY17 BRIDGE AT JACKSONVILLE, NC\",Stream,NWISDV,-77.43777778,34.74888889,EPSG:4269,0.0,NAVD88,Onslow,NC,US,2007-12-22T00:00:00Z,2008-06-16T00:00:00Z,2008-06-14T00:00:00Z,4.6,P\n" +
"USGS:0209303205,USGS,0209303205,2456089,\"NEW RIVER BELOW HWY17 BRIDGE AT JACKSONVILLE, NC\",Stream,NWISDV,-77.43777778,34.74888889,EPSG:4269,0.0,NAVD88,Onslow,NC,US,2007-12-22T00:00:00Z,2008-06-16T00:00:00Z,2008-06-15T00:00:00Z,4.4,P\n" +
"USGS:0209303205,USGS,0209303205,2456089,\"NEW RIVER BELOW HWY17 BRIDGE AT JACKSONVILLE, NC\",Stream,NWISDV,-77.43777778,34.74888889,EPSG:4269,0.0,NAVD88,Onslow,NC,US,2007-12-22T00:00:00Z,2008-06-16T00:00:00Z,2008-06-16T00:00:00Z,4.5,P\n";
        Test.ensureEqual(results[1].substring(results[1].length() - expected.length()), 
            expected, "results[1]=\n" + results[1]);


        //test just station variables,   siteType="Estuary"
        String2.log("\n* test #6");
        query = "agencySiteCode,agency,siteCode,siteID,siteName,siteType,network," +
            "longitude,latitude,srs,altitude,vertDatum,beginDate,endDate&siteType=\"Estuary\"";             
        tName = edd.makeNewFileForDapQuery(null, null, query, dir, 
            edd.className() + "_3_1_test6", ".csv"); 
        results = String2.readFromFile(dir + tName);
        expected = 
"agencySiteCode,agency,siteCode,siteID,siteName,siteType,network,longitude,latitude,srs,altitude,vertDatum,beginDate,endDate\n" +
",,,,,,,degrees_east,degrees_north,,m,,UTC,UTC\n" +
"USGS:01302250,USGS,01302250,2458432,EAST CREEK AT SANDS POINT NY,Estuary,NWISDV,-73.7101944,40.8662222,EPSG:4269,NaN,,2007-12-13T00:00:00Z,\n" +
"USGS:01302845,USGS,01302845,2458431,FROST CREEK AT SHEEP LN BRIDGE AT LATTINGTOWN NY,Estuary,NWISDV,-73.5931944,40.9051111,EPSG:4269,NaN,,2007-12-13T00:00:00Z,\n" +
"USGS:01304057,USGS,01304057,2458433,FLAX POND AT OLD FIELD NY,Estuary,NWISDV,-73.1431667,40.9628611,EPSG:4269,NaN,,2008-04-08T00:00:00Z,\n" +
"USGS:01310740,USGS,01310740,1496694,REYNOLDS CHANNEL AT POINT LOOKOUT NY,Estuary,NWISDV,-73.5837396,40.5934366,EPSG:4269,NaN,,2004-10-28T00:00:00Z,\n" +
"USGS:01311143,USGS,01311143,2532028,HOG ISLAND CHANNEL AT ISLAND PARK NY,Estuary,NWISDV,-73.6561111,40.6088333,EPSG:4269,NaN,,2010-10-21T00:00:00Z,\n";
        Test.ensureEqual(results[0], "", "results[0]=\n" + results[0]);
        Test.ensureEqual(results[1], expected, "results[1]=\n" + results[1]);

        //test regex of station variable  siteName=~".*BRIDGE.*"
        String2.log("\n* test #7");
        query = "agencySiteCode,agency,siteCode,siteID,siteName,siteType,network," +
            "longitude,latitude,srs,altitude,vertDatum&siteName=~\".*BRIDGE.*\"";             
        tName = edd.makeNewFileForDapQuery(null, null, query, dir, 
            edd.className() + "_3_1_test7", ".csv"); 
        results = String2.readFromFile(dir + tName);
        expected = 
"agencySiteCode,agency,siteCode,siteID,siteName,siteType,network,longitude,latitude,srs,altitude,vertDatum\n" +
",,,,,,,degrees_east,degrees_north,,m,\n" +
"USGS:01302845,USGS,01302845,2458431,FROST CREEK AT SHEEP LN BRIDGE AT LATTINGTOWN NY,Estuary,NWISDV,-73.5931944,40.9051111,EPSG:4269,NaN,\n" +
"USGS:0209303205,USGS,0209303205,2456089,\"NEW RIVER BELOW HWY17 BRIDGE AT JACKSONVILLE, NC\",Stream,NWISDV,-77.43777778,34.74888889,EPSG:4269,0.0,NAVD88\n";
        Test.ensureEqual(results[0], "", "results[0]=\n" + results[0]);
        Test.ensureEqual(results[1], expected, "results[1]=\n" + results[1]);


    }

    /** Bob uses this to look for odd values of county, state, country, ... in the stations table. */
    public static void bobListUnique() throws Throwable {
        String2.log("*** bobListUnique()");
        String stationsFileName = "c:/data/waterML/scrapeNWISStations20110123.json";
        Table table = new Table();
        table.readJson(stationsFileName);
        String lookAt[] = new String[]{
            "agency_cd", "siteType", "vertDatum", "county", "state", "country"};
        for (int i = 0; i < lookAt.length; i++) {
            String colName = lookAt[i];
            StringArray pa = (StringArray)table.getColumn(colName);
            pa.sortIgnoreCase();
            pa.removeDuplicates();
            String2.log("\n***" + colName + "\n" + pa.toNewlineString());
        }
    }

    /** Test avoiding stack overflow if admin-supplied subset table is absent.
     */
    public static void testAvoidStackOverflow() throws Throwable {
        try {
            EDD edd = EDD.oneFromDatasetXml("testAvoidStackOverflow");
            String2.getStringFromSystemIn(
                "You shouldn't have gotten here!\n" + 
                "A specific exception should have been thrown.\n" +
                "Press ^C to stop or Enter to continue..."); 
        } catch (Throwable t) {
            String msg = MustBe.throwableToString(t);
            if (msg.indexOf("(in danger of stack overflow)") < 0)
                String2.getStringFromSystemIn(
                    "Unexpected exception:\n" + msg +
                    "\nPress ^C to stop or Enter to continue..."); 
        }
    }
    

    /** This runs all of the tests. */
    public static void test() throws Throwable {
        String2.log("\n*** EDDTableFromNWISDV.test");
        //testGetValuesTable();
        testGenerateDatasetsXml();  
        testBasic();
        testAvoidStackOverflow();

    }


}
