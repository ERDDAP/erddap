/* 
 * EDDTableFromSOS Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.PAOne;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.SimpleException;
import com.cohort.util.Test;
import com.cohort.util.Units2;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.griddata.FileNameUtility;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;


/** 
 * This class aggregates data from a group of stations, all served by
 * one SOS server. The stations all serve the same set of variables
 * (although the source for each station doesn't have to serve all variables).
 * <p>The SWE (Sensor Web Enablement) and SOS (Sensor Observation Service) home page
 *   is  https://www.opengeospatial.org/standards/sos .
 * <p>See OpenGIS® Sensor Observation Service Implementation Specification
 *   (OGC 06-009r6) Version: 1.0.0
 *   https://www.opengeospatial.org/standards/sos .
 *   (Bob has a copy in c:/projects/sos)
 * <p>See OGC Web Services Common Specification ver 1.1.0 (OGC 06-121r3)
 *    from https://www.opengeospatial.org/standards/common
 *    which covers construction of GET and POST queries (e.g., section 7.2.3 
 *    and section 9).
 *   (Bob has a copy in c:/projects/sos)
 * <p>SOS overview:
 *   <ul>
 *   <li>If you send a getCapabilities xml request to a SOS server
 *      (sourceUrl + "?service=SOS&request=GetCapabilities"), 
 *      you get an xml result
 *      with a list of stations and the observedProperties that they have data for.
 *      e.g., 
 *   <li>An observedProperty is a formal URI reference to a property
 *      e.g., urn:ogc:phenomenon:longitude:wgs84 or
 *      (was http://marinemetadata.org/cf#sea_water_temperature - http://marinemetadata.org is GONE!)
 *      http://mmisw.org/ont/cf/parameter/sea_water_temperature
 *   <li>An observedProperty isn't a variable.
 *   <li>More than one variable may have the same observedProperty 
 *     (e.g., insideTemp and outsideTemp might both have observedProperty 
 *       (was http://marinemetadata.org/cf#air_temperature - http://marinemetadata.org is GONE!)
 *       http://mmisw.org/ont/cf/parameter/air_temperature
 *     See the observedProperty schema to find the variables associated
 *     with each observedProperty.
 *   <li>If you send a getObservation xml request to a SOS server, you get an xml result
 *      with descriptions of field names in the response, field units, and the data.
 *      The field names will include longitude, latitude, depth(perhaps), and time.
 *   <li>This class just gets data for one station at a time to avoid
 *      requests for huge amounts of data at one time (which ties up memory
 *      and is slow getting initial response to user).
 *   <li>Currently, some SOS servers (e.g., GoMOOS in the past) respond to getObservation requests
 *      for more than one observedProperty by just returning results for
 *      the first of the observedProperties. (No error message!)
 *      See the constructor parameter tRequestObservedPropertiesSeparately.  
 *   <li>!!!The getCapabilities result doesn't include the results variables (they say field names).
 *       The only way to know them ahead of time is to request some data,
 *       look at the xml, and note the field name (and units).
 *       E.g., For a given station (e.g., D01) and observedProperty (e.g., ...sea_water_temperature)
 *       look at the results from the getObservation url:
 *       http://dev.gomoos.org/cgi-bin/sos/oostethys_sos?REQUEST=GetObservation&OFFERING=D01&OBSERVEDPROPERTY=sea_water_temperature&TIME=2007-06-01T00:00:00/2007-06-24T23:59:00
 *       Similarly, getCapabilities doesn't include altitude/depth information.
 *       But maybe this info is in the getDescription results (info for specific station).
 *   </ul>
 * <p>See Phenomenon dictionary at
 *   https://www.seegrid.csiro.au/subversion/xmml/OGC/branches/SWE_gml2/sweCommon/current/examples/phenomena.xml
 * <p>Info about a sample server: https://www.coast.noaa.gov/DTL/dtl_proj3_oostethys.html
 *   <br>The csc server uses code from
 *     http://www.oostethys.org/ogc-oceans-interoperability-experiment/experiment-1 . [GONE]
 *   <br>It seems to be set up to just serve several instances of lon,lat,alt,time,1variable
 *     instead of lon,lat,alt,time,manyVariables.(?)
 *
 * <p>Sample queries for different time periods
 * http://www.oostethys.org/Members/tcook/sos-xml-time-encodings/?searchterm=Time [GONE]
 *
 * <p>In general, if there is an error in processing the getCapabilities xml
 * for one station, only that station is rejected (with a message to the log.txt file).
 * So it is good to read the logs periodically to see if there are unexpected errors.
 *
 * <p>This class insists that each station is indeed a station at one lat lon point:
 *    lowerCorner's lat lon must equal upperCorner's lat lon or the station is ignored.
 *    
 * @author Bob Simons (bob.simons@noaa.gov) 2007-09-21
 */
public class EDDTableFromSOS extends EDDTable{ 

    /** stationTable */
    protected final static int stationLonCol = 0; 
    protected final static int stationLatCol = 1; 
    protected final static int stationBeginTimeCol = 2; //epochSeconds
    protected final static int stationEndTimeCol = 3;   //epochSeconds
    protected final static int stationIDCol = 4; 
    protected final static int stationProcedureCol = 5; 
    protected final static String defaultStationIdSourceName = "station_id";
    protected final static String stationIdDestinationName = "station_id";
    static String sosCopyDir = EDStatic.fullCopyDirectory + "_SOS_cache/";

    /** The first nFixedVariables dataVariables are always created automatically 
     * (don't include in constructor tDataVariables): 
     * longitude, latitude, stationID, altitude, time. */
    protected final static int nFixedVariables = 5; 

    private static boolean testQuickRestart = false;  //to test, set this to true in test method, not here.
    protected static boolean timeParts = false; //some test methods set this to true for timing test purposes only


    /** Variables set by the constructor. */
    protected String sosVersion;
    protected String lonSourceName, latSourceName, altSourceName, timeSourceName, 
        stationIdSourceName;
    protected boolean requestObservedPropertiesSeparately;
    protected String responseFormat; //"" -> default
    protected String bboxOffering;   //offering name to use with lon lat BBOX request
    protected String bboxParameter;  //parameter prefix to use with lon lat BBOX request
    protected StringArray uniqueSourceObservedProperties;
    protected Table stationTable;
    protected boolean[][] stationHasObsProp; //[station#][uniqueSourceObservedProperties index]
    protected String sosServerType = "";     //may be "".   This variable will be lowercased.
    public final static String SosServerTypeIoos52N   = "IOOS_52N";
    public final static String SosServerTypeIoosNdbc  = "IOOS_NDBC";
    public final static String SosServerTypeIoosNcSOS = "IOOS_NcSOS";
    public final static String SosServerTypeIoosNos   = "IOOS_NOS";
    public final static String SosServerTypeOostethys = "OOSTethys";
    public final static String SosServerTypeWhoi      = "WHOI";
    public final static String[] SosServerTypes = { //order not important
         SosServerTypeIoos52N, SosServerTypeIoosNdbc, SosServerTypeIoosNcSOS,
         SosServerTypeIoosNos, SosServerTypeOostethys, SosServerTypeWhoi};
    public final static String slowSummaryWarning = 
        "\n\nThe source SOS server for this dataset is very slow, so requests will " +
        "take minutes to be fulfilled or will fail because of a timeout.";
    protected boolean ioos52NServer = false;  
    protected boolean ioosNdbcServer = false;  
    protected boolean ioosNcSOSServer = false;   
    protected boolean ioosNosServer = false;   
    protected boolean oostethysServer = false; 
    protected boolean whoiServer = false;       //one must be specified

    public static String defaultResponseFormat(String tSosServerType) { 
        tSosServerType = tSosServerType == null? "" : tSosServerType.toLowerCase();
        if (SosServerTypeIoos52N.toLowerCase().equals(tSosServerType))
            return "text/xml; subtype=\"om/1.0.0/profiles/ioos_sos/1.0\"";
        if (SosServerTypeIoosNdbc.toLowerCase().equals(tSosServerType) ||
             SosServerTypeIoosNos.toLowerCase().equals(tSosServerType))
            return "text/csv";
        return "text/xml; subtype=\"om/1.0.0\"";
    }

    /**
     * This constructs an EDDTableFromSOS based on the information in an .xml file.
     * 
     * @param erddap if known in this context, else null
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableFromSOS"&gt;
     *    having just been read.  
     * @return an EDDTableFromSOS.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDTableFromSOS fromXml(Erddap erddap, 
        SimpleXMLReader xmlReader) throws Throwable {

        //data to be obtained (or not)
        if (verbose) String2.log("\n*** constructing EDDTableFromSOS(xmlReader)...");
        String tDatasetID = xmlReader.attributeValue("datasetID"); 
        Attributes tGlobalAttributes = null;
        String tSosServerType = "";
        String tStationIdSourceName = defaultStationIdSourceName;
        String tLongitudeSourceName = null;  
        String tLatitudeSourceName = null;  
        String tAltitudeSourceName = null;  //currently alt, not depth
        double tAltitudeMetersPerSourceUnit = 1; 
        double tAltitudeSourceMinimum = Double.NaN;  
        double tAltitudeSourceMaximum = Double.NaN;  
        String tTimeSourceName = null; String tTimeSourceFormat = null;
        ArrayList tDataVariables = new ArrayList();
        int tReloadEveryNMinutes = Integer.MAX_VALUE;
        String tAccessibleTo = null;
        String tGraphsAccessibleTo = null;
        StringArray tOnChange = new StringArray();
        String tFgdcFile = null;
        String tIso19115File = null;
        String tSosOfferingPrefix = null;
        String tLocalSourceUrl = null, tObservationOfferingIdRegex = null;
        boolean tRequestObservedPropertiesSeparately = false;
        String tResponseFormat = null;
        String tBBoxOffering = null;
        String tBBoxParameter = null;
        String tSosVersion = null;
        boolean tSourceNeedsExpandedFP_EQ = true;
        String tDefaultDataQuery = null;
        String tDefaultGraphQuery = null;
        String tAddVariablesWhere = null;

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
            else if (localTags.equals( "<sosVersion>")) {}
            else if (localTags.equals("</sosVersion>")) tSosVersion = content; 
            else if (localTags.equals( "<sosServerType>")) {}
            else if (localTags.equals("</sosServerType>")) tSosServerType = content; 
            else if (localTags.equals( "<stationIdSourceName>")) {}
            else if (localTags.equals("</stationIdSourceName>")) tStationIdSourceName = content; 
            else if (localTags.equals( "<longitudeSourceName>")) {}
            else if (localTags.equals("</longitudeSourceName>")) tLongitudeSourceName = content; 
            else if (localTags.equals( "<latitudeSourceName>"))  {}
            else if (localTags.equals("</latitudeSourceName>"))  tLatitudeSourceName = content; 
            else if (localTags.equals( "<altitudeSourceName>"))  {}
            else if (localTags.equals("</altitudeSourceName>"))  tAltitudeSourceName = content; 
            else if (localTags.equals( "<altitudeSourceMinimum>")) {}
            else if (localTags.equals("</altitudeSourceMinimum>")) tAltitudeSourceMinimum = String2.parseDouble(content); 
            else if (localTags.equals( "<altitudeSourceMaximum>")) {}
            else if (localTags.equals("</altitudeSourceMaximum>")) tAltitudeSourceMaximum = String2.parseDouble(content); 
            else if (localTags.equals( "<altitudeMetersPerSourceUnit>")) {}
            else if (localTags.equals("</altitudeMetersPerSourceUnit>")) 
                tAltitudeMetersPerSourceUnit = String2.parseDouble(content); 
            else if (localTags.equals( "<timeSourceName>"))   {}
            else if (localTags.equals("</timeSourceName>"))   tTimeSourceName = content; 
            else if (localTags.equals( "<timeSourceFormat>")) {}
            else if (localTags.equals("</timeSourceFormat>")) tTimeSourceFormat = content; 
            else if (localTags.equals( "<dataVariable>")) 
                tDataVariables.add(getSDADVariableFromXml(xmlReader));           
            else if (localTags.equals( "<accessibleTo>")) {}
            else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
            else if (localTags.equals( "<graphsAccessibleTo>")) {}
            else if (localTags.equals("</graphsAccessibleTo>")) tGraphsAccessibleTo = content;
            else if (localTags.equals( "<reloadEveryNMinutes>")) {}
            else if (localTags.equals("</reloadEveryNMinutes>")) tReloadEveryNMinutes = String2.parseInt(content); 
            else if (localTags.equals( "<sourceUrl>")) {}
            else if (localTags.equals("</sourceUrl>")) tLocalSourceUrl = content; 
            else if (localTags.equals( "<observationOfferingIdRegex>")) {}
            else if (localTags.equals("</observationOfferingIdRegex>")) tObservationOfferingIdRegex = content; 
            else if (localTags.equals( "<requestObservedPropertiesSeparately>")) {}
            else if (localTags.equals("</requestObservedPropertiesSeparately>")) 
                tRequestObservedPropertiesSeparately = content.equals("true"); 
            else if (localTags.equals( "<responseFormat>")) {}
            else if (localTags.equals("</responseFormat>")) tResponseFormat = content; 
            else if (localTags.equals( "<bboxOffering>")) {}
            else if (localTags.equals("</bboxOffering>")) tBBoxOffering = content;
            else if (localTags.equals( "<bboxParameter>")) {}
            else if (localTags.equals("</bboxParameter>")) tBBoxParameter = content;
            else if (localTags.equals( "<sourceNeedsExpandedFP_EQ>")) {}
            else if (localTags.equals("</sourceNeedsExpandedFP_EQ>")) tSourceNeedsExpandedFP_EQ = String2.parseBoolean(content); 
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
            else if (localTags.equals( "<addVariablesWhere>")) {}
            else if (localTags.equals("</addVariablesWhere>")) tAddVariablesWhere = content; 

            else xmlReader.unexpectedTagException();
        }
        int ndv = tDataVariables.size();
        Object ttDataVariables[][] = new Object[ndv][];
        for (int i = 0; i < tDataVariables.size(); i++)
            ttDataVariables[i] = (Object[])tDataVariables.get(i);

        return new EDDTableFromSOS(tDatasetID, tAccessibleTo, tGraphsAccessibleTo,
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
            tDefaultDataQuery, tDefaultGraphQuery, tAddVariablesWhere, 
            tGlobalAttributes, tSosServerType,
            tStationIdSourceName, tLongitudeSourceName, tLatitudeSourceName,
            tAltitudeSourceName, tAltitudeSourceMinimum, tAltitudeSourceMaximum, 
            tAltitudeMetersPerSourceUnit,
            tTimeSourceName, tTimeSourceFormat,
            ttDataVariables,
            tReloadEveryNMinutes, tLocalSourceUrl, tSosVersion,
            tObservationOfferingIdRegex, tRequestObservedPropertiesSeparately,
            tResponseFormat, 
            tBBoxOffering, tBBoxParameter,
            tSourceNeedsExpandedFP_EQ);
    }

    /**
     * The constructor.
     *
     * @param tDatasetID is a very short string identifier 
     *  (recommended: [A-Za-z][A-Za-z0-9_]* )
     *   for this dataset. See EDD.datasetID().
     * @param tAccessibleTo is a comma separated list of 0 or more
     *    roles which will have access to this dataset.
     *    <br>If null, everyone will have access to this dataset (even if not logged in).
     *    <br>If "", no one will have access to this dataset.
     * @param tOnChange 0 or more actions (starting with http://, https://, or mailto: )
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
     *   Special case: if addGlobalAttributes name="summary",
     *     then "[standard]" within the value will be replaced by the standardSummary 
     *     (from this class).
     * @param tLonSourceName the results field name for the longitude variable 
     *    (e.g., longitude).
     *    The name can vary, but the basic meaning must be longitude degrees East.
     *    If lon isn't in the results, use "longitude" here as a placeholder.
     * @param tLatSourceName the results field name for the latitude variable 
     *    (e.g., latitude)
     *    The name can vary, but the basic meaning must be latitude degrees North.
     *    If lat isn't in the results, use "latitude" here as a placeholder.
     * @param tAltSourceName the results field name for the altitude variable 
     *    (e.g., depth) (use this even if no alt variable)
     *    If alt isn't in the results, use "altitude" here as a placeholder.
     * @param tSourceMinAlt (in source units) or NaN if not known.
     *    This info is explicitly supplied because it isn't in getCapabilities.
     *    [I use eddTable.getEmpiricalMinMax("2007-02-01", "2007-02-01", false, true); below to get it.]
     * @param tSourceMaxAlt (in source units) or NaN if not known
     * @param tAltMetersPerSourceUnit the factor needed to convert the source
     *    alt values to/from meters above sea level.
     * @param tTimeSourceName the results field name for the time variable 
     *    (e.g., time)
     *    If time isn't in the results, use "time" here as a placeholder.
     * @param tTimeSourceFormat is either<ul>
     *    <li> a udunits string (containing " since ")
     *      describing how to interpret numbers 
     *      (e.g., "seconds since 1970-01-01T00:00:00Z"),
     *    <li> a java.text.SimpleDateFormat string describing how to interpret string times  
     *      (see https://docs.oracle.com/javase/8/docs/api/index.html?java/text/SimpleDateFormat.html)).
     *    </ul>
     * @param tDataVariables is an Object[nDataVariables][4]: 
     *    <br>[0]=String sourceName (the field name of the data variable in the tabular results,
     *        e.g., Salinity, not the ObservedProperty name),
     *    <br>[1]=String destinationName (the name to be presented to the ERDDAP user, 
     *        or null to use the sourceName),
     *    <br>[2]=Attributes addAttributes (at ERD, this must have "ioos_category" - 
     *        a category from EDV.ioosCategories). 
     *        Special case: value="null" causes that item to be removed from combinedAttributes.
     *    <br>[3]=String source dataType (e.g., "int", "float", "String"). 
     *        Some data sources have ambiguous data types, so it needs to be specified here.
     *    <br>!!!Unique to EDDTableFromSOS: the longitude, latitude, and stationID 
     *       variables are created automatically.
     *    <br>Since these datasets can be served via ERDDAP's SOS server,
     *       each tDataVariable will have an "observedProperty" attribute.
     *       "observedProperty" defines the observedProperty SOS clients request in order
     *       to get data for that variable.
     *       For non-composite observedProperty, this is the observedProperty's 
     *         xlink:href attribute value, e.g., 
     *         (was "http://marinemetadata.org/cf#sea_water_temperature" - http://marinemetadata.org is GONE!)
     *         http://mmisw.org/ont/cf/parameter/sea_water_temperature
     *       For composite observedProperty,  this is the CompositePhenomenon's
     *         gml:id attribute value, e.g., "WEATHER_OBSERVABLES".
     *    <br>!!!You can get station information by visiting the tLocalSourceUrl
     *       (which by default returns the sos:Capabilities XML document).
     * @param tReloadEveryNMinutes indicates how often the source should
     *    be checked for new data.
     * @param tLocalSourceUrl the url to which queries are sent
     * @param tSosVersion if a specific version must be specified to the server,
     *     this is it (e.g., 1.0.0). May be null.
     * @param tObservationOfferingIdRegex only observationOfferings with IDs 
     *    (usually the station names) which match this regular expression
     *    are included in the dataset
     *    (".+" will catch all station names)
     * @param tRequestObservedPropertiesSeparately if true, the observedProperties
     *   will be requested separately. If false, they will be requested all at once.
     * @param tResponseFormat  Not yet percent-encoded. Use null or "" for the default.
     * @param tBBoxOffering  the offering name to use with lon lat BBOX requests (or null or "")
     * @param tBBoxParameter the parameter prefix to use with lon lat BBOX request (or null or "")
     * @param tSourceNeedsExpandedFP_EQ
     * @throws Throwable if trouble
     */
    public EDDTableFromSOS(String tDatasetID, 
        String tAccessibleTo, String tGraphsAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tSosOfferingPrefix,
        String tDefaultDataQuery, String tDefaultGraphQuery, String tAddVariablesWhere, 
        Attributes tAddGlobalAttributes, String tSosServerType,
        String tStationIdSourceName,
        String tLonSourceName,
        String tLatSourceName,
        String tAltSourceName, double tSourceMinAlt, double tSourceMaxAlt, double tAltMetersPerSourceUnit, 
        String tTimeSourceName, String tTimeSourceFormat,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes,
        String tLocalSourceUrl, String tSosVersion, String tObservationOfferingIdRegex, 
        boolean tRequestObservedPropertiesSeparately,
        String tResponseFormat,
        String tBBoxOffering, String tBBoxParameter,
        boolean tSourceNeedsExpandedFP_EQ) throws Throwable {

        if (verbose) String2.log(
            "\n*** constructing EDDTableFromSOS " + tDatasetID); 
        long constructionStartMillis = System.currentTimeMillis();
        String errorInMethod = "Error in EDDTableFromSOS(" + 
            tDatasetID + ") constructor:\n";
            
        //save some of the parameters
        className = "EDDTableFromSOS"; 
        datasetID = tDatasetID;
        setAccessibleTo(tAccessibleTo);
        setGraphsAccessibleTo(tGraphsAccessibleTo);
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

        sosServerType = tSosServerType == null? "" : tSosServerType.trim();
        tSosServerType = sosServerType.toLowerCase();
        ioos52NServer   = tSosServerType.equals(SosServerTypeIoos52N.toLowerCase());
        ioosNdbcServer  = tSosServerType.equals(SosServerTypeIoosNdbc.toLowerCase());
        ioosNcSOSServer = tSosServerType.equals(SosServerTypeIoosNcSOS.toLowerCase());
        ioosNosServer   = tSosServerType.equals(SosServerTypeIoosNos.toLowerCase());
        oostethysServer = tSosServerType.equals(SosServerTypeOostethys.toLowerCase());
        whoiServer      = tSosServerType.equals(SosServerTypeWhoi.toLowerCase()); //required
        if (String2.caseInsensitiveIndexOf(SosServerTypes, sosServerType) < 0)
            throw new RuntimeException("<sosServerType>=" + sosServerType +
                " must be one of " + String2.toCSSVString(SosServerTypes));
        if (ioos52NServer) 
            tSosVersion = "1.0.0"; //server supports 2.0.0, but this class doesn't yet.

        localSourceUrl = tLocalSourceUrl;
        String tSummary = addGlobalAttributes.getString("summary");
        if (tSummary != null)
            addGlobalAttributes.set("summary", 
                String2.replaceAll(tSummary, "[standard]", standardSummary));
        stationIdSourceName = tStationIdSourceName == null? defaultStationIdSourceName :
            tStationIdSourceName;
        lonSourceName = tLonSourceName;
        latSourceName = tLatSourceName;
        altSourceName = tAltSourceName;
        timeSourceName = tTimeSourceName;
        setReloadEveryNMinutes(tReloadEveryNMinutes);
        Test.ensureNotNothing(localSourceUrl, "sourceUrl wasn't specified.");
        requestObservedPropertiesSeparately = tRequestObservedPropertiesSeparately;
        responseFormat = tResponseFormat == null? "" : tResponseFormat.trim();
        if (responseFormat.length() == 0)
            responseFormat = defaultResponseFormat(tSosServerType);
        sosVersion = tSosVersion;

        //bbox - set both or neither
        if (tBBoxOffering != null && tBBoxOffering.length() > 0) {
            if (tBBoxParameter != null && tBBoxParameter.length() > 0) {
                bboxOffering = tBBoxOffering;
                bboxParameter = tBBoxParameter; 
            } else {
                throw new IllegalArgumentException(
                    "If <bboxOffering> is specified, <bboxParameter> should be, too.");
            }
        } else if (tBBoxParameter != null && tBBoxParameter.length() > 0) {
            throw new IllegalArgumentException(
                "If <bboxParameter> is specified, <bboxOffering> should be, too.");
        }

        //CONSTRAIN_PARTIAL is great because EDDTable will always check all constraints.
        //justStationTableInfo below relies on both being CONSTRAIN_PARTIAL.
        sourceNeedsExpandedFP_EQ      = tSourceNeedsExpandedFP_EQ;
        sourceCanConstrainNumericData = CONSTRAIN_PARTIAL; //just lat and lon
        sourceCanConstrainStringData  = CONSTRAIN_PARTIAL; //time (but not != or regex), station_id (even REGEX_OP), but nothing else
        sourceCanConstrainStringRegex = PrimitiveArray.REGEX_OP; //just for station_id
      
        //set source attributes (none available from source)
        sourceGlobalAttributes = new Attributes();
        if (addGlobalAttributes.getString("subsetVariables") == null)
            addGlobalAttributes.add("subsetVariables", "station_id, longitude, latitude");
        combinedGlobalAttributes = new Attributes(addGlobalAttributes, sourceGlobalAttributes); //order is important
        String tLicense = combinedGlobalAttributes.getString("license");
        if (tLicense != null)
            combinedGlobalAttributes.set("license", 
                String2.replaceAll(tLicense, "[standard]", EDStatic.standardLicense));
        combinedGlobalAttributes.removeValue("\"null\""); 

        //get all dv sourceObservedProperties
        uniqueSourceObservedProperties = new StringArray();
        for (int dv = 0; dv < tDataVariables.length; dv++) {
            //no sourceAtt
            String tSourceName = (String)tDataVariables[dv][0];
            Attributes tAddAtt = (Attributes)tDataVariables[dv][2];
            String 
            //    op = tAddAtt.getString("sourceObservedProperty"); //preference for sourceObservedProperty
            //if (op == null || op.length() == 0) 
                op = tAddAtt.getString(EDV.observedProperty); //otherwise, source = regular observedProperty
            if (op == null || op.length() == 0) 
                throw new IllegalArgumentException(
                    //"Neither 'sourceObservedProperty' nor 'observervedProperty' attributes were " +
                    "The 'observervedProperty' attribute wasn't " +
                    "set for dataVariable sourceName=" + tSourceName + "."); 
            if (uniqueSourceObservedProperties.indexOf(op) < 0)
                uniqueSourceObservedProperties.add(op);
        }

        //*** read the getCapabilities xml and set up stationTable
        stationTable = new Table();
        stationTable.addColumn("Lon", new DoubleArray());
        stationTable.addColumn("Lat", new DoubleArray());
        stationTable.addColumn("BeginTime", new DoubleArray());
        stationTable.addColumn("EndTime", new DoubleArray());
        stationTable.addColumn("ID", new StringArray());
        stationTable.addColumn("Procedure", new StringArray());
        ArrayList tStationHasObsPropAL = new ArrayList();  //for each valid station, the boolean[]
        //values that persist for a while
        double tLon = Double.NaN, tLat = Double.NaN, 
            tBeginTime = Double.NaN, tEndTime = Double.NaN;
        String tIndeterminateEnd = null, tStationID = "", tStationProcedure = "";     
        double currentEpochSeconds = Calendar2.gcToEpochSeconds(Calendar2.newGCalendarZulu());
        boolean tStationHasObsProp[] = new boolean[uniqueSourceObservedProperties.size()];
        String tDVNames[] = new String[tDataVariables.length];
        for (int dv = 0; dv < tDataVariables.length; dv++)
            tDVNames[dv] = (String)tDataVariables[dv][0];
        //use KVP (KeyValuePair) HTTP GET request to getCapabilities
        //see section 7.2.3 of OGC 06-121r3 (OGC Web Services Common Specification) ver 1.1.0
        String tUrl = localSourceUrl + "?service=SOS&request=GetCapabilities"; 
        if (sosVersion != null && sosVersion.length() > 0) {
            if (whoiServer) //ioosNcbcServer || ioosNosServer) //not needed. 1.0.0 is default and only.
                tUrl += "&version=" + sosVersion;
            if (ioos52NServer) //AcceptVersions needed. This class only supports 1.0.0 currently.
                tUrl += "&AcceptVersions=" + sosVersion;
        }
        if (reallyVerbose) String2.log("  GetCapUrl=" + tUrl);
               
        //if (debugMode)
        //    String2.log(SSR.getUrlResponseStringUnchanged(tUrl));
        //String2.writeToFile("f:/programs/sos/ndbcSosWind_capabilities_90721.xml", SSR.getUrlResponseStringUnchanged(tUrl));

        //get getCapabilities   (perhaps reuse quickRestartFile)
        String quickRestartFileName = File2.forceExtension(quickRestartFullFileName(), ".xml");
        boolean quickRestartFileExists = File2.isFile(quickRestartFileName);
        if (verbose) 
            String2.log("  quickRestartFile exists=" + quickRestartFileExists);
        if (quickRestartFileExists &&
            (testQuickRestart || (EDStatic.quickRestart && EDStatic.initialLoadDatasets()))) {
            //use the quickRestartFile
            //Note that if this fails (any reason, e.g., damaged quickRestartFile)
            //  the dataset will reload at next majorLoadDatasets,
            //  since EDStatic.initialLoadDatasets() will be false.

            //set creationTimeMillis as if dataset was created when getCapabilities xml was downloaded
            creationTimeMillis = File2.getLastModified(quickRestartFileName); //0 if trouble
            if (verbose)
                String2.log("  using getCapabilities from quickRestartFile " +
                    Calendar2.millisToIsoStringTZ(creationTimeMillis));
            if (reallyVerbose) String2.log("    " + quickRestartFileName);

        } else {
            //re-download the file
            if (quickRestartFileExists)
                File2.delete(quickRestartFileName);
            if (verbose) 
                String2.log("  downloading getCapabilities from " + tUrl); 
            long downloadTime = System.currentTimeMillis();
            SSR.downloadFile(tUrl, quickRestartFileName, true); //use compression
            if (verbose)
                String2.log("  download time=" + (System.currentTimeMillis() - downloadTime) + "ms"); 
        }

        SimpleXMLReader xmlReader = new SimpleXMLReader(
            File2.getDecompressedBufferedInputStream(quickRestartFileName));
        try {
            xmlReader.nextTag();
            String tags = xmlReader.allTags();
            String sosPrefix = "";
            if (xmlReader.tag(0).equals("Capabilities")) {
                sosPrefix = "";          
            } else if (xmlReader.tag(0).equals("sos:Capabilities")) {
                sosPrefix = "sos:"; 
                //oostethys or ioosNdbcServer?
            } else {
                xmlReader.close();
                if (debugMode) {
                    try {
                        String qrFile[] = String2.readFromFile(quickRestartFileName);
                        String2.log(qrFile[1]);
                    } catch (Throwable t) {
                    }
                }
                throw new IllegalArgumentException("The first SOS capabilities tag=" + 
                    tags + " should have been <Capabilities> or <sos:Capabilities>.");
            }
            //String2.log("attributesCSV=" + xmlReader.attributesCSV());
            if (verbose) String2.log("  sosPrefix=" + sosPrefix + " sosVersion=" + sosVersion);
            String offeringTag = "<" + 
                sosPrefix + "Capabilities><" + 
                sosPrefix + "Contents><" + 
                sosPrefix + "ObservationOfferingList><" + 
                sosPrefix + "ObservationOffering>";
            String offeringEndTag = "<" + 
                sosPrefix + "Capabilities><" + 
                sosPrefix + "Contents><" + 
                sosPrefix + "ObservationOfferingList></" + 
                sosPrefix + "ObservationOffering>";

            //default beginTime: a year ago      tamuSos needs this
            GregorianCalendar dbt = Calendar2.newGCalendarZulu();
            dbt.add(Calendar2.YEAR, -1); 
            double defaultBeginTime = Calendar2.gcToEpochSeconds(  
                Calendar2.clearSmallerFields(dbt, Calendar2.MONTH));
            do {
                //process the tags
                //String2.log("tags=" + tags + xmlReader.content());

                if (tags.endsWith("<ows:ServiceIdentification></ows:ServiceTypeVersion>")) {
                    if (sosVersion == null || sosVersion.length() == 0) {
                        sosVersion = xmlReader.content(); //e.g., "0.0.31" or "1.0.0"
                        if (reallyVerbose) String2.log("  sosVersion(from ows:ServiceTypeVersion)=" + sosVersion);
                    }

                } else if (tags.startsWith(offeringTag)) {
                    String endOfTag = tags.substring(offeringTag.length());
                    String content = xmlReader.content();
                    String fatalError = null;

                    //String2.log("endOfTag=" + endOfTag + xmlReader.content());

    /* separate phenomena
                <sos:ObservationOffering xmlns:xlink="https://www.w3.org/1999/xlink" gml:id="A01">
                    <gml:description> Latest data from Mooring A01 from the Gulf of Maine Ocean Observing System (GoMOOS), located in the Gulf of Maine Massachusetts Bay </gml:description>
                    <gml:name>A01</gml:name>
                    <gml:boundedBy>
                        <gml:Envelope>
                            <gml:lowerCorner srsName="urn:ogc:def:crs:EPSG:6.5:4329">42.5277 -70.5665</gml:lowerCorner>
                            <gml:upperCorner srsName="urn:ogc:def:crs:EPSG:6.5:4329">42.5277 -70.5665</gml:upperCorner>
                        </gml:Envelope>
                    </gml:boundedBy>
                    <sos:time>
                        <gml:TimePeriod gml:id="AVAILABLE_OFFERING_TIME">
                            <gml:beginPosition>2001-07-10T06:30:00Z</gml:beginPosition>
                            <gml:endPosition indeterminatePosition="now"/>
                            <gml:timeInterval unit="hour">1</gml:timeInterval>
                        </gml:TimePeriod>
                    </sos:time>
                    <sos:procedure xlink:href="urn:gomoos.org:source.mooring#A01"/>
                    <sos:observedProperty xlink:href="http://marinemetadata.org/cf#sea_water_temperature"/>
                    <sos:observedProperty xlink:href="http://marinemetadata.org/cf#sea_water_salinity"/>
                    <sos:featureOfInterest xlink:href="urn:something:bodyOfWater"/>
                    <sos:responseFormat>application/com-xml</sos:responseFormat>
                </sos:ObservationOffering>
    */
    /* or compositePhenomenon...
                    <sos:observedProperty>
                        <swe:CompositePhenomenon gml:id="WEATHER_OBSERVABLES">
                            <gml:name>Weather measurements</gml:name>
                            <swe:component xlink:href="http://vast.uah.edu/dictionary/phenomena.xml#AirTemperature"/>
                            <swe:component xlink:href="http://vast.uah.edu/dictionary/phenomena.xml#AtmosphericPressure"/>
                            <swe:component xlink:href="http://vast.uah.edu/dictionary/phenomena.xml#WindSpeed"/>
                            <swe:component xlink:href="http://vast.uah.edu/dictionary/phenomena.xml#WindDirection"/>
                        </swe:CompositePhenomenon>
                    </sos:observedProperty>
    */

                    //if (endOfTag.equals("")) {  //ioosServer has different gml:id than gml:name; requests need gml:name.
                    //    //e.g., 44004
                    //    if (ioosServer) tStationID = xmlReader.attributeValue("gml:id");

                    if (endOfTag.length() == 0) {
                        if (whoiServer) {
                            tStationID = xmlReader.attributeValue("gml:id");
                            if (reallyVerbose) String2.log(
                                "  stationID(from gml:id)=" + tStationID);
                        } else if (reallyVerbose) {
                            String2.log("  start of offeringTag");
                        }

                    } else if (endOfTag.equals("</gml:name>")) { //ioos_53N, OOSTethys have this
                        //e.g., 44004
                        if (!whoiServer) {
                            tStationID = content;
                            if (reallyVerbose) String2.log(
                                "  stationID(from </gml:name>)=" + tStationID);
                        }


                        //if (ioosServer) { 
                        //    //remove 
                        //    String pre = ":station:";
                        //    int prePo = tStationID.indexOf(pre);
                        //    if (prePo >= 0)
                        //        tStationID = tStationID.substring(prePo + pre.length());
                        //}

                    } else if (endOfTag.equals(               "<gml:boundedBy><gml:Envelope></gml:lowerCorner>") || //IOOS_52N, OOSTethys
                               endOfTag.equals("<gml:boundedBy><gml:boundedBy><gml:Envelope></gml:lowerCorner>")) { //VAST 
                        //e.g., 38.48 -70.43    order is lat lon [alt/depth]
                        StringArray lla = StringArray.wordsAndQuotedPhrases(content);
                        if (lla.size() >= 2) {
                            tLat = String2.parseDouble(lla.get(0));
                            tLon = String2.parseDouble(lla.get(1));
                            if (reallyVerbose) String2.log(
                                "    lat lon(from </gml:lowerCorner>)=" + tLat + " " + tLon);
                            //for now, ignore alt/depth
                            if (Double.isNaN(tLon) || Double.isNaN(tLat))
                                String2.log("Warning while parsing gml: Invalid <gml:lowerCorner>=" + content);
                        }

                    } else if (endOfTag.equals(               "<gml:boundedBy><gml:Envelope></gml:upperCorner>") || //IOOS_52N, OOSTethys
                               endOfTag.equals("<gml:boundedBy><gml:boundedBy><gml:Envelope></gml:upperCorner>")) { //VAST
                        //ensure upperCorner = lowerCorner
                        //e.g., 38.48 -70.43    order is lat lon [alt/depth]
                        StringArray lla = StringArray.wordsAndQuotedPhrases(content);
                        double ttLat = Double.NaN, ttLon = Double.NaN;
                        if (lla.size() >= 2) {
                            ttLat = String2.parseDouble(lla.get(0));
                            ttLon = String2.parseDouble(lla.get(1));
                        }
                        //for now, ignore alt/depth
                        //disable test for VAST development?!   
                        //VAST has tLat=34.723 ttLat=34.725 tLon=-86.646 ttLon=-86.644
                        if (Double.isNaN(ttLon) || Double.isNaN(ttLat))
                            String2.log("Warning while parsing gml: Invalid <gml:upperCorner>=" + content);
                        if (tLat != ttLat || tLon != ttLon) {
                            String2.log("Warning while parsing gml: lowerCorner!=upperCorner" +
                                " tLat=" + tLat + " ttLat=" + ttLat + 
                                " tLon=" + tLon + " ttLon=" + ttLon);
                            tLon = Double.NaN; tLat = Double.NaN; //invalidate this station
                        }                   

                    } else if (endOfTag.equals("<" + sosPrefix + "time><gml:TimePeriod></gml:beginPosition>") || //ioosServer and OOSTethys 
                               endOfTag.equals(        "<sos:eventTime><gml:TimePeriod></gml:beginPosition>")) { //VAST
                        //e.g., 2001-07-10T06:30:00Z
                        //This is workaround for invalid value "TZ" in NOS SOS GetCapabilities.
                        try {
                            tBeginTime = Calendar2.isoStringToEpochSeconds(content);
                            if (reallyVerbose) String2.log(
                                "    beginTime(from <gml:TimePeriod></gml:beginPosition>)=" + tBeginTime);
                        } catch (Throwable t) {
                            String2.log("Warning while parsing gml:beginPosition ISO time: " + t.toString());
                            tBeginTime = defaultBeginTime;
                        }

                    } else if (endOfTag.equals("<" + sosPrefix + "time><gml:TimePeriod><gml:endPosition>") || //ioosServer and OOSTethys
                               endOfTag.equals(        "<sos:eventTime><gml:TimePeriod><gml:endPosition>")) { //VAST has this
                        tIndeterminateEnd = xmlReader.attributeValue("indeterminatePosition");
                        if (reallyVerbose) String2.log(
                            "    indeterminateEnd time=" + tIndeterminateEnd);

                    } else if (endOfTag.equals("<" + sosPrefix + "time><gml:TimePeriod></gml:endPosition>") || //ioosServer and OOSTethys
                               endOfTag.equals(        "<sos:eventTime><gml:TimePeriod></gml:endPosition>")) { //VAST has this) {
                        //usually:  <gml:endPosition indeterminatePosition="now"/>
                        //could be: e.g., 2001-07-10T06:30:00Z
                        if (content.length() > 0) {
                            try {
                                tEndTime = Calendar2.isoStringToEpochSeconds(content);
                            } catch (Throwable t) {
                                String2.log("Warning while parsing gml:endPosition ISO time: " + t.toString());
                                tEndTime = Double.NaN;
                            }
                            //are they being precise about recent end time?  change to NaN
                            if (!Double.isNaN(tEndTime) && 
                                currentEpochSeconds - tEndTime < 2*Calendar2.SECONDS_PER_DAY)
                                tEndTime = Double.NaN;   
                        } else if (tIndeterminateEnd != null && 
                            (tIndeterminateEnd.equals("now") ||       //ioosServer has this
                             tIndeterminateEnd.equals("unknown"))) {  //OOSTethys has this
                            //leave as NaN...
                            //tEndTime = Calendar2.gcToEpochSeconds(Calendar2.newGCalendarZulu() + 
                            //    Calendar2.SECONDS_PER_HOUR); //buffer
                        } else {
                            String2.log("Warning while parsing TimePeriod /gml:endPosition; content=\"\" tIndeterminateEnd=" + 
                                tIndeterminateEnd);
                            tBeginTime = Double.NaN; //mark as invalid; use tBeginTime since tIndeterminateEnd isn't required.
                        }
                        if (reallyVerbose) String2.log(
                            "    endTime(from <gml:TimePeriod></gml:endPosition>)=" + tEndTime);

                    } else if (endOfTag.equals("<" + sosPrefix + "procedure>")) {
                        //<sos:procedure xlink:href="urn:gomoos.org:source.mooring#A01"/>
                        //attribute is without quotes
                        String tXlink = xmlReader.attributeValue("xlink:href");
                        if (tXlink == null) 
                            String2.log("Warning while parsing 'procedure': no xlink:href attribute!");
                        else 
                            tStationProcedure = tXlink;                    
                        if (reallyVerbose) String2.log("    procedure>=" + tStationProcedure);

                    } else if (endOfTag.equals("<" + sosPrefix + "observedProperty>")) {
                        //handle non-composite observedProperty
                        //<sos:observedProperty xlink:href="http://marinemetadata.org/cf#sea_water_temperature"/>
                        //                      http://marinemetadata.org is GONE!
                        //now                   xlink:href="http://mmisw.org/ont/cf/parameter/sea_water_temperature"
                        String tXlink = xmlReader.attributeValue("xlink:href"); //without quotes
                        if (tXlink != null) {
                            int opPo = uniqueSourceObservedProperties.indexOf(tXlink);   
                            if (opPo >= 0) 
                                tStationHasObsProp[opPo] = true;
                            if (reallyVerbose) String2.log(
                                "    has observedProperty> #" + opPo + " " + tXlink);
                        }
                    } else if (endOfTag.equals("<" + sosPrefix + "observedProperty><swe:CompositePhenomenon>")) {
                        //handle composite observedProperty
                        //<swe:CompositePhenomenon gml:id="WEATHER_OBSERVABLES">
                        String tid = xmlReader.attributeValue("gml:id"); //without quotes
                        if (tid != null) {
                            int opPo = uniqueSourceObservedProperties.indexOf(tid);   
                            if (opPo >= 0) 
                                tStationHasObsProp[opPo] = true;
                            if (reallyVerbose) String2.log(
                                "    has composite observedProperty> #" + opPo + " " + tid);
                        }
                    }

                    //handle the error
                    //but this isn't used; problems above are logged, but only cause this station not to be used (see 'invalid' below)
                    if (fatalError != null)
                        throw new IllegalArgumentException(
                            "Error on xml line #" + xmlReader.lineNumber() + 
                            " stationID=" + tStationID + ": " + fatalError);

                //all data gathered; create the station
                } else if (tags.equals(offeringEndTag)) {

                    //look for invalid values
                    boolean hasObservedProperties = false;
                    for (int i = 0; i < uniqueSourceObservedProperties.size(); i++) {
                        if (tStationHasObsProp[i]) {
                            hasObservedProperties = true; 
                            break;
                        }
                    }
                    String invalid = null;
                    if (Double.isNaN(tLon))                   invalid = "longitude";
                    else if (Double.isNaN(tLat))              invalid = "latitude";
                    else if (Double.isNaN(tBeginTime))        invalid = "beginTime";
                    //endTime may be NaN
                    else if (tStationProcedure.length() == 0) invalid = "stationProcedure";
                    else if (!hasObservedProperties)          invalid = "stationHasRelevantObservedProperties";

                    if (tStationID == null || tStationID.length() == 0) {
                        if (reallyVerbose) String2.log("  station_id=\"" + tStationID + 
                            "\" rejected.");

                    } else if (!tStationID.matches(tObservationOfferingIdRegex)) {
                        if (reallyVerbose) String2.log("  station_id=\"" + tStationID + 
                            "\" rejected: didn't match the observationOfferingIdRegex=" + 
                            tObservationOfferingIdRegex);

                    } else if (invalid != null) {
                        if (reallyVerbose) String2.log("  station_id=\"" + tStationID + 
                            "\" rejected: The " + invalid + " value wasn't set.");

                    } else {
                        //add the station;  add a row of data to stationTable
                        stationTable.getColumn(stationLonCol).addDouble(tLon);
                        stationTable.getColumn(stationLatCol).addDouble(tLat);
                        stationTable.getColumn(stationBeginTimeCol).addDouble(tBeginTime); //epochSeconds
                        stationTable.getColumn(stationEndTimeCol).addDouble(tEndTime);     //epochSeconds
                        stationTable.getColumn(stationIDCol).addString(tStationID);
                        stationTable.getColumn(stationProcedureCol).addString(tStationProcedure);
                        tStationHasObsPropAL.add(tStationHasObsProp);
                    }

                    //reset values for next station
                    tLon = Double.NaN;
                    tLat = Double.NaN;
                    tBeginTime = Double.NaN;
                    tEndTime = Double.NaN;
                    tIndeterminateEnd = null;
                    tStationID = "";                
                    tStationProcedure = "";                
                    tStationHasObsProp = new boolean[uniqueSourceObservedProperties.size()];

                }

                //get the next tag
                xmlReader.nextTag();
                tags = xmlReader.allTags();
            } while (!tags.startsWith("</"));

        } finally {
            xmlReader.close();      
        }
        if (sosVersion == null || sosVersion.length() == 0)
            sosVersion = "1.0.0"; //default

        if (stationTable.nRows() == 0)
            throw new RuntimeException(datasetID + " has no valid stations.");
        stationHasObsProp = new boolean[tStationHasObsPropAL.size()][];
        for (int i = 0; i < tStationHasObsPropAL.size(); i++)
            stationHasObsProp[i] = (boolean[])tStationHasObsPropAL.get(i);
        if (reallyVerbose) String2.log("Station Table=\n" + stationTable.saveAsJsonString(stationBeginTimeCol, true));

        //cdm_data_type 
        String cdmType = combinedGlobalAttributes.getString("cdm_data_type"); 
        String allowedCdmTypes[] = new String[]{CDM_OTHER, CDM_POINT, 
            CDM_TIMESERIES, CDM_TIMESERIESPROFILE, CDM_TRAJECTORY, CDM_TRAJECTORYPROFILE};
        if (String2.indexOf(allowedCdmTypes, cdmType) < 0)
            throw new RuntimeException("Currently, EDDTableFromSOS only supports cdm_data_type=" +
                String2.toCSSVString(allowedCdmTypes) + ", not \"" + cdmType + "\".");

        //make the fixedVariables
        dataVariables = new EDV[nFixedVariables + tDataVariables.length];

        lonIndex = 0;
        PAOne stats[] = stationTable.getColumn(stationLonCol).calculatePAOneStats();
        dataVariables[lonIndex] = new EDVLon(datasetID, tLonSourceName, null, null,
            "double", stats[PrimitiveArray.STATS_MIN], stats[PrimitiveArray.STATS_MAX]);  

        latIndex = 1;
        stats = stationTable.getColumn(stationLatCol).calculatePAOneStats();
        dataVariables[latIndex] = new EDVLat(datasetID, tLatSourceName, null, null,
            "double", stats[PrimitiveArray.STATS_MIN], stats[PrimitiveArray.STATS_MAX]);  

        sosOfferingIndex = 2;   //aka stationID
        Attributes tAtts = (new Attributes()) 
            .add("long_name", "Station ID")
            .add("ioos_category", "Identifier");
        if (CDM_TIMESERIES.equals(cdmType) || CDM_TIMESERIESPROFILE.equals(cdmType)) 
            tAtts.add("cf_role", "timeseries_id");
        else if (CDM_TRAJECTORY.equals(cdmType) || CDM_TRAJECTORYPROFILE.equals(cdmType)) 
            tAtts.add("cf_role", "trajectory_id");
        dataVariables[sosOfferingIndex] = new EDV(datasetID, stationIdSourceName, stationIdDestinationName, 
            null, tAtts, "String");//the constructor that reads actual_range
        //no need to call setActualRangeFromDestinationMinMax() since they are NaNs

        //alt axis isn't set up in datasets.xml. 
        altIndex = 3; depthIndex = -1;  //2012-12-20 consider using depthIndex???
        Attributes altAddAtts = new Attributes();
        altAddAtts.set("units", "m");
        if (tAltMetersPerSourceUnit != 1)
            altAddAtts.set("scale_factor", tAltMetersPerSourceUnit);
        dataVariables[altIndex] = new EDVAlt(datasetID, tAltSourceName, null, altAddAtts,
            "double", PAOne.fromDouble(tSourceMinAlt), PAOne.fromDouble(tSourceMaxAlt));

        timeIndex = 4;  //times in epochSeconds
        stats = stationTable.getColumn(stationBeginTimeCol).calculatePAOneStats();        //to get the minimum begin value
        PAOne stats2[] = stationTable.getColumn(stationEndTimeCol).calculatePAOneStats(); //to get the maximum end value
        PAOne tTimeMin = stats[PrimitiveArray.STATS_MIN];
        PAOne tTimeMax = stats2[PrimitiveArray.STATS_N].getInt() < stationTable.getColumn(stationEndTimeCol).size()? //some indeterminant
            PAOne.fromDouble(Double.NaN) : stats2[PrimitiveArray.STATS_MAX]; 
        tAtts = (new Attributes())
            .add("units", tTimeSourceFormat);
        if (CDM_TIMESERIESPROFILE.equals(cdmType) || CDM_TRAJECTORYPROFILE.equals(cdmType)) 
            tAtts.add("cf_role", "profile_id");
        EDVTime edvTime = new EDVTime(datasetID, tTimeSourceName, null, tAtts, 
            "String"); //this constructor gets source / sets destination actual_range
        edvTime.setDestinationMinMax(tTimeMin, tTimeMax);
        edvTime.setActualRangeFromDestinationMinMax();
        dataVariables[timeIndex] = edvTime;

        //create non-fixed dataVariables[]
        for (int dv = 0; dv < tDataVariables.length; dv++) {
            String tSourceName = (String)tDataVariables[dv][0];
            String tDestName = (String)tDataVariables[dv][1];
            if (tDestName == null || tDestName.trim().length() == 0)
                tDestName = tSourceName;
            Attributes tSourceAtt = null; //(none available from source)
            Attributes tAddAtt = (Attributes)tDataVariables[dv][2];
            String tSourceType = (String)tDataVariables[dv][3];

            if (EDVTimeStamp.hasTimeUnits(tSourceAtt, tAddAtt)) {
                dataVariables[nFixedVariables + dv] = new EDVTimeStamp(datasetID, tSourceName, tDestName, 
                    tSourceAtt, tAddAtt, tSourceType); //this constructor gets source / sets destination actual_range
            } else {
                dataVariables[nFixedVariables + dv] = new EDV(datasetID, tSourceName, tDestName, 
                    tSourceAtt, tAddAtt, tSourceType); //the constructor that reads actual_range
                dataVariables[nFixedVariables + dv].setActualRangeFromDestinationMinMax();
            }
            Test.ensureNotNothing(
                dataVariables[nFixedVariables + dv].combinedAttributes().getString(EDV.observedProperty),
                "\"" + EDV.observedProperty + "\" attribute not assigned for variable sourceName=" + tSourceName);
        }

        //Gather information to serve this dataset via ERDDAP's SOS server.
        //This has an advantage over the generic gathering of SOS data:
        //  it can determine the min/max lon/lat/time of each station.
        //SOS datasets always have actual lon,lat values and stationTable time is always epochSeconds,
        //  so I can just use source station info directly (without conversion).
        //Note that times are often too wide a range because they are for all observedProperties,
        //  not just the one used by this dataset.
        if (preliminaryAccessibleViaSOS().length() == 0) {  //it should succeed
            sosMinLon    = stationTable.getColumn(stationLonCol);
            sosMaxLon    = sosMinLon;
            sosMinLat    = stationTable.getColumn(stationLatCol);
            sosMaxLat    = sosMinLat;
            sosMinTime   = stationTable.getColumn(stationBeginTimeCol);
            sosMaxTime   = stationTable.getColumn(stationEndTimeCol);
            sosOfferings = (StringArray)stationTable.getColumn(stationIDCol).clone(); //clone since it changes below
            int nSosOfferings = sosOfferings.size();
            //convert sosOfferings to short name
//!!! This should determine sosOfferingPrefix, then for each offering: ensure it exists and remove it
            for (int offering = 0; offering < nSosOfferings; offering++) {
                String so = sosOfferings.getString(offering);
                int cpo = so.lastIndexOf(":");
                if (cpo >= 0) 
                    sosOfferings.setString(offering, so.substring(cpo + 1));
            }
        }

        //make addVariablesWhereAttNames and addVariablesWhereAttValues
        makeAddVariablesWhereAttNamesAndValues(tAddVariablesWhere);

        //ensure the setup is valid
        ensureValid();

        //done
        long cTime = System.currentTimeMillis() - constructionStartMillis;
        if (verbose) String2.log(
            (debugMode? "\n" + toString() : "") +
            "\n*** EDDTableFromSOS " + datasetID + 
            " constructor finished. TIME=" + 
            cTime + "ms" + (cTime >= 10000? "  (>10s!)" : "") + "\n"); 

    }


    /** 
     * This gets the data (chunk by chunk) from this EDDTable for the 
     * OPeNDAP DAP-style query and writes it to the TableWriter. 
     * See the EDDTable method documentation.
     *
     * @param loggedInAs the user's login name if logged in (or null if not logged in).
     * @param userDapQuery the part after the '?', still percentEncoded, may be null.
     * @throws Throwable if trouble (notably, WaitThenTryAgainException)
     */
    public void getDataForDapQuery(String loggedInAs, String requestUrl, 
        String userDapQuery, TableWriter tableWriter) throws Throwable {
        long getTime = System.currentTimeMillis();

        //get the sourceDapQuery (a query that the source can handle)
        StringArray resultsVariables    = new StringArray();
        StringArray constraintVariables = new StringArray();
        StringArray constraintOps       = new StringArray();
        StringArray constraintValues    = new StringArray();
        getSourceQueryFromDapQuery(userDapQuery,
            resultsVariables,
            constraintVariables, constraintOps, constraintValues); //timeStamp constraints other than regex are epochSeconds
        int nConstraints = constraintVariables.size();

        //further prune constraints 
        //sourceCanConstrainNumericData = CONSTRAIN_PARTIAL; //just lat and lon
        //sourceCanConstrainStringData  = CONSTRAIN_PARTIAL; //time (but not != or regex), station_id (even PrimitiveArray.REGEX_OP), but nothing else
        //sourceCanConstrainStringRegex = PrimitiveArray.REGEX_OP; //just for station_id
        //work backwards since deleting some
        int conDVI[] = new int[nConstraints];
        boolean justStationTableInfo = true;
        boolean hasStationConstraint = false;
        for (int c = constraintVariables.size() - 1; c >= 0; c--) { 
            String constraintVariable = constraintVariables.get(c);
            String constraintOp       = constraintOps.get(c);
            double dConstraintValue   = String2.parseDouble(constraintValues.get(c));
            int dv = String2.indexOf(dataVariableSourceNames(), constraintVariable);
            conDVI[c] = dv;
            EDV edv = dataVariables[dv];
            if (dv != lonIndex && dv != latIndex && dv != sosOfferingIndex) //aka stationID
                justStationTableInfo = false;

            if (dv == latIndex || dv == lonIndex) {
                //ok

            } else if (dv == sosOfferingIndex) { //aka stationID
                hasStationConstraint = true;  //any kind: =, <, >, regex, ...

            } else if (edv instanceof EDVTimeStamp) {
                //remove if != or regex
                if (constraintOp.equals("!=") || constraintOp.equals(PrimitiveArray.REGEX_OP)) {
                    //remove constraint
                    constraintVariables.remove(c);
                    constraintOps.remove(c);
                    constraintValues.remove(c);
                }

            } else {
                //remove all other constraints
                constraintVariables.remove(c);
                constraintOps.remove(c);
                constraintValues.remove(c);
            }
        }


        //make tableDVI -- the dataVariable indices corresponding to the columns needed for the results table.
        //Always include lon,lat,alt,time,id to facilitate merging observedProperties correctly.
        IntArray tableDVI = new IntArray(); //.get(tableColumnIndex) -> dataVariableIndex
        //add the nFixedVariables
        int tableLonCol = 0, tableLatCol = 1, tableAltCol = 2, 
            tableTimeCol = 3, 
            tableStationIdCol = 4;
        tableDVI.add(lonIndex);
        tableDVI.add(latIndex);
        tableDVI.add(altIndex);
        tableDVI.add(timeIndex);
        tableDVI.add(sosOfferingIndex); //ada stationID
        StringArray requestObservedProperties = new StringArray();
        for (int rv = 0; rv < resultsVariables.size(); rv++) {
            int dvi = String2.indexOf(dataVariableSourceNames(), resultsVariables.get(rv));
            //only add if not already included
            if (tableDVI.indexOf(dvi, 0) < 0) 
                tableDVI.add(dvi);
            if (dvi != lonIndex && dvi != latIndex && dvi != sosOfferingIndex) //aka stationID
                justStationTableInfo = false;
            //make list of desired observedProperties
            //remember that >1 var may refer to same obsProp (eg, insideTemp and outsideTemp refer to ...Temperature)
            if (dvi >= nFixedVariables) {
                String  
                //    top = dataVariables[dvi].combinedAttributes().getString("sourceObservedProperty"); //preferred
                //if (top == null)
                    top = dataVariables[dvi].combinedAttributes().getString(EDV.observedProperty);
                if (requestObservedProperties.indexOf(top) < 0)
                    requestObservedProperties.add(top);
            }
        }


        //handle justStationTableInfo
        //This relies on all CONSTRAIN_PARTIAL above.
        if (reallyVerbose) String2.log("  justStationTableInfo=" + justStationTableInfo);
        if (justStationTableInfo) {
            //make a table of just lon,lat,id
            tableDVI.clear(); //.get(tableColumnIndex) -> dataVariableIndex
            tableDVI.add(lonIndex);
            tableDVI.add(latIndex);
            tableDVI.add(sosOfferingIndex);  //stationID
            Table table = makeEmptySourceTable(tableDVI.toArray(), 128);

            //add all of the station info
            table.getColumn(0).append(stationTable.getColumn(stationLonCol)); 
            table.getColumn(1).append(stationTable.getColumn(stationLatCol)); 
            table.getColumn(2).append(stationTable.getColumn(stationIDCol)); 

            //do the final writeToTableWriter (it will 
            writeChunkToTableWriter(requestUrl, userDapQuery, table, tableWriter, true);
            if (reallyVerbose) String2.log("  getDataForDapQuery done. TIME=" +
                (System.currentTimeMillis() - getTime) + "ms"); 
            return;
        }

        
        //always at least one observedProperty
        //If request is for lon,lat,id with time constraints
        //  you get no data if you don't ask for a property.
        //So add the first var after nFixedVariables
        //This is not a perfect solution, because results might be different if you chose a different property.
        if (requestObservedProperties.size() == 0) {
            int dvi = nFixedVariables;
            tableDVI.add(dvi);
            String 
            //    top = dataVariables[dvi].combinedAttributes().getString("sourceObservedProperty"); //preferred
            //if (top == null)
                top = dataVariables[dvi].combinedAttributes().getString(EDV.observedProperty);
            if (requestObservedProperties.indexOf(top) < 0)
                requestObservedProperties.add(top);
        }

        //get requestedMin,Max  
        double requestedDestinationMin[] = new double[4]; //LLAT   time in userDapQuery is epochSeconds
        double requestedDestinationMax[] = new double[4];
        getRequestedDestinationMinMax(userDapQuery, true,
            requestedDestinationMin, requestedDestinationMax);        

        //doBBoxQuery? 
        boolean doBBoxQuery = !hasStationConstraint &&
            bboxOffering != null && bboxParameter != null;
        String tBBoxParameter = "";
        if (doBBoxQuery) {
            if (reallyVerbose) String2.log("doBBoxQuery=true"); 
            //fill in missing bbox bounds with known dataset lon,lat bounds
            double fudge = 0.001;
            if (Double.isNaN(requestedDestinationMin[0]))
                             requestedDestinationMin[0] = dataVariables[lonIndex].destinationMinDouble() - fudge;
            if (Double.isNaN(requestedDestinationMax[0]))
                             requestedDestinationMax[0] = dataVariables[lonIndex].destinationMaxDouble() + fudge;
                
            if (Double.isNaN(requestedDestinationMin[1]))
                             requestedDestinationMin[1] = dataVariables[latIndex].destinationMinDouble() - fudge;
            if (Double.isNaN(requestedDestinationMax[1]))
                             requestedDestinationMax[1] = dataVariables[latIndex].destinationMaxDouble() + fudge;

            //ndbc objects if min=max
            if (requestedDestinationMin[0] == requestedDestinationMax[0]) {
                requestedDestinationMin[0] -= fudge;
                requestedDestinationMax[0] += fudge;
            }
            if (requestedDestinationMin[1] == requestedDestinationMax[1]) {
                requestedDestinationMin[1] -= fudge;
                requestedDestinationMax[1] += fudge;
            }

            tBBoxParameter = 
                "&" + bboxParameter + 
                requestedDestinationMin[0] + "," +
                requestedDestinationMin[1] + "," +
                requestedDestinationMax[0] + "," +
                requestedDestinationMax[1];
        }


        //makeTable
        Table table = makeEmptySourceTable(tableDVI.toArray(), 128);
        HashMap llatHash = new HashMap(); //llat info -> table row number (as a String)
        //IntArray fixedColumnsInTable = new IntArray();
        //for (int col = 0; col < tableDVI.size(); col++)
        //    if (tableDVI.get(col) < nFixedVariables)
        //        fixedColumnsInTable.add(col);


        //Go through the stations, request data if station is relevant.
        //SOS queries may be limited to 1 station at a time (based on 'procedure'?)
        //  But that's a good thing because I want to limit response chunk size.
        //  ???Extreme cases: may need to further limit to chunks of time.
        int nStations = stationTable.nRows();
        boolean aConstraintShown = false;
        boolean matchingStation = false;
        STATION_LOOP:
        for (int station = 0; station < nStations; station++) {
            if (Thread.currentThread().isInterrupted())
                throw new SimpleException("EDDTableFromSOS.getDataForDapQuery" +
                    EDStatic.caughtInterrupted);

            String tStationLonString = "", tStationLatString = "", tStationAltString = "", 
                tStationID = "";

            if (doBBoxQuery) {
                if (station >= 1)
                    break;

                //when station=0, get data for all stations
                tStationID = bboxOffering;

            } else {
                //make sure this station is relevant
                //test lon,lat,time   (station alt not available)
                boolean stationOK = true;
                for (int i = 0; i < 4; i++) {  //0..3 are LLAT in requestedDestinationMin/Max
                    String tName = "???";
                    int stationMinCol = -1, stationMaxCol = -1, precision = 4;
                    if      (i == 0) {tName = "longitude";  stationMinCol = stationLonCol;       stationMaxCol = stationLonCol;}
                    else if (i == 1) {tName = "latitude";   stationMinCol = stationLatCol;       stationMaxCol = stationLatCol;}
                    else if (i == 3) {tName = "time";       stationMinCol = stationBeginTimeCol; stationMaxCol = stationEndTimeCol; 
                                      precision = 9;}
                    if (stationMinCol >= 0) {
                        double rMin = requestedDestinationMin[i];
                        double rMax = requestedDestinationMax[i];
                        double sMin = stationTable.getDoubleData(stationMinCol, station);
                        double sMax = stationTable.getDoubleData(stationMaxCol, station);
                        if ((Double.isFinite(rMax) && Double.isFinite(sMin) && !Math2.greaterThanAE(precision, rMax, sMin)) || 
                            (Double.isFinite(rMin) && Double.isFinite(sMax) && !Math2.lessThanAE(   precision, rMin, sMax))) {
                            if (reallyVerbose) String2.log("  reject station=" + station + 
                                ": no overlap between station " + tName + " (min=" + sMin + ", max=" + sMax +
                                ") and requested range (min=" + rMin + ", max=" + rMax + ").");
                            stationOK = false; 
                            break;
                        }
                    }
                }
                if (!stationOK)
                    continue;

                //test stationID constraint
                tStationID  = stationTable.getStringData(stationIDCol, station);
                for (int con = 0; con < nConstraints; con++) {
                    if (conDVI[con] == sosOfferingIndex) { //aka stationID
                        String op = constraintOps.get(con);
                        boolean pass = PrimitiveArray.testValueOpValue(tStationID, op, constraintValues.get(con));
                        if (!pass) {
                            if (reallyVerbose) String2.log("  rejecting station=" + station + 
                                " because stationID=\"" + tStationID + "\" isn't " + op + 
                                " \"" + constraintValues.get(con) + "\".");
                            stationOK = false;
                            break;  //constraint loop
                        }
                    }
                }
                if (!stationOK)
                    continue;
                if (reallyVerbose) String2.log("\nquerying stationID=" + tStationID);

                //The station is in the bounding box!
                tStationLonString = "" + stationTable.getDoubleData(stationLonCol, station);
                tStationLatString = "" + stationTable.getDoubleData(stationLatCol, station);
                tStationAltString = "";
            }
            matchingStation = true;


            //request the observedProperties (together or separately)
            //see class javadocs above explaining why this is needed
            for (int obsProp = 0; obsProp < requestObservedProperties.size(); obsProp++) {
                
                StringArray tRequestObservedProperties = new StringArray();
                if (requestObservedPropertiesSeparately) {  
                    //separately
                    tRequestObservedProperties.add(requestObservedProperties.get(obsProp));
                } else {
                    //we're already done
                    if (obsProp == 1) 
                        break;

                    //get all at once
                    tRequestObservedProperties = (StringArray)requestObservedProperties.clone();
                }
                if (reallyVerbose) {
                    String2.log("\nobsProp=" + obsProp + " ttObsProp=" + tRequestObservedProperties);
                    //String2.pressEnterToContinue();
                }

                if (doBBoxQuery) { 
                    //we're checking all stations, so don't check if this stationHasObsProp
                } else {
                    //remove obsProp from tRequestObservedProperties if station doesn't support it 
                    for (int i = tRequestObservedProperties.size() - 1; i >= 0; i--) {
                        int po = uniqueSourceObservedProperties.indexOf(tRequestObservedProperties.get(i));
                        if (!stationHasObsProp[station][po])
                            tRequestObservedProperties.remove(i);
                    }
                    if (tRequestObservedProperties.size() == 0) {
                        if (reallyVerbose) String2.log("  reject station=" + station + 
                            ": It has none of the requested observedProperties.");
                        continue;
                    }
                }

                if (ioosNdbcServer || ioosNosServer) {
                 
                    //make kvp   -- no examples or info for XML request
                    //example from https://sdf.ndbc.noaa.gov/sos/
                    //if (reallyVerbose && obsProp == 0) String2.log("\n  sample=" +
                    //  "https://sdf.ndbc.noaa.gov/sos/server.php?request=GetObservation" +
                    //  "&service=SOS&offering=NDBC:46088&observedproperty=currents" +
                    //  "&responseformat=text/xml;schema=%22ioos/0.6.1%22&eventtime=2008-06-01T00:00Z/2008-06-02T00:00Z");
                    //2012-11-15 NDBC sample revised to be 
                    //  https://sdf.ndbc.noaa.gov/sos/server.php?request=GetObservation&service=SOS
                    //    &version=1.0.0&offering=urn:ioos:station:wmo:41012
                    //    &observedproperty=air_pressure_at_sea_level
                    //    &responseformat=text/xml;subtype=%22om/1.0.0%22
                    //    &eventtime=2011-03-01T00:00Z/2011-03-02T00:00Z
                    //???How should a client know how to convert GetCapabilities observedProperty (long)
                    //  into GetObservation observedProperty (short)?
                    //int hashPo = tRequestObservedProperties.get(obsProp).lastIndexOf(':'); //for NOS SOS
                    String kvp = 
                        "?service=SOS" +
                        "&version=" + sosVersion +
                        "&request=GetObservation" + 
                        "&offering=" + SSR.minimalPercentEncode(tStationID) + 
                        //"&observedProperty=" + tRequestObservedProperties.get(obsProp).substring(hashPo + 1) + 
                        "&observedProperty=" + tRequestObservedProperties.get(obsProp) + 
                        "&responseFormat=" + SSR.minimalPercentEncode(responseFormat) +  
                        "&eventTime=" +   //requestedDestination times are epochSeconds
                            Calendar2.epochSecondsToIsoStringTZ(requestedDestinationMin[3]) + 
                        (requestedDestinationMin[3] == requestedDestinationMax[3]? "" : 
                            "/" + Calendar2.epochSecondsToIsoStringTZ(requestedDestinationMax[3])) +
                         tBBoxParameter;
                    if (reallyVerbose && !aConstraintShown) {
                        String2.log("  requestURL=" + localSourceUrl + kvp);
                        //aConstraintShown = true;
                    }

                    //read IOOS response
                    readFromIoosNdbcNos(kvp, table, llatHash);

                } else { //non-ioosServer    e.g., Oostethys
  
                    //make the xml constraint: basics and offering
                    //important help getting this to work from f:/programs/sos/oie_sos_time_range_obs.cgi
                    //which is from http://www.oostethys.org/ogc-oceans-interoperability-experiment/experiment-1/sos-client-testing-source-code [GONE]
                    StringBuilder getSB = new StringBuilder();
                    getSB.append("?service=SOS" +
                        "&version=" + sosVersion +  
                        "&responseFormat=" + SSR.minimalPercentEncode(responseFormat) +   
                        "&request=GetObservation" +
                        "&offering=" + SSR.minimalPercentEncode(tStationID) + 
                        "&observedProperty=" + SSR.minimalPercentEncode(
                            String2.toSVString(tRequestObservedProperties.toArray(), ",", false)));

                    //eventTime
                    //older SOS spec has <ogc:During>; v1.0 draft has <ogc:T_During>
                    //new oostethys server ignores <ogc:During>, wants T_During 
                    //see http://www.oostethys.org/Members/tcook/sos-xml-time-encodings/ [GONE]
                    String tMinTimeS = Calendar2.epochSecondsToIsoStringTZ(requestedDestinationMin[3]);
                    String tMaxTimeS = Calendar2.epochSecondsToIsoStringTZ(requestedDestinationMax[3]);
                    if (requestedDestinationMin[3] == requestedDestinationMax[3]) {
                        getSB.append("&eventTime=" + tMinTimeS);
                    } else {
                        getSB.append("&eventTime=" + tMinTimeS + "/" + tMaxTimeS);
                    }

                    //BBOX 
                    getSB.append(tBBoxParameter);  //"" if inactive; BBOX=minlon,minlat,maxlon,maxlat

                    //See above.
                    //responseFormat=text%2Fxml%3B%20subtype%3D%22om%2F1.0.0%22    
                    //was resultFormat until 2010-04-20
                    //getSB.append("&resultFormat=application/com-xml"); //until 2010-04-20
                    //see http://www.oostethys.org/best-practices/best-practices-get [GONE]
                    //getSB.append("&text%2Fxml%3B%20subtype%3D%22om%2F1.0.0%22"); 

                    if (reallyVerbose && !aConstraintShown) {
                        String2.log("  requestURL=" + localSourceUrl + getSB.toString());
                        //aConstraintShown = true;
                    }
                    if (false) { //debugMode) { 
                        String2.log("*** Begin response");
                        String2.log(SSR.getUrlResponseStringUnchanged(localSourceUrl + getSB.toString()));
                        String2.log("*** End response");
                    }

                    //*** read the data
                    if (whoiServer) {
                        readFromWhoiServer(getSB.toString(), table, llatHash,
                            tStationLonString, tStationLatString, tStationAltString, tStationID);
                    } else {
                        readFromOostethys(getSB.toString(), table, llatHash,
                            tStationLonString, tStationLatString, tStationAltString, tStationID);
                    }

                } //end of non-ioosServer get chunk of data

                if (reallyVerbose) String2.log("\nstation=" + tStationID + " tableNRows=" + table.nRows());
                //if (debugMode) String2.log("table=\n" + table);
            } //end of obsProp loop

            //writeToTableWriter
            //this can't be in obsProp loop (obsProps are merged)
            if (writeChunkToTableWriter(requestUrl, userDapQuery, table, tableWriter, false)) {
                table = makeEmptySourceTable(tableDVI.toArray(), 128);
                llatHash.clear(); //llat info -> table row number (as a String)
                if (tableWriter.noMoreDataPlease) {
                    tableWriter.logCaughtNoMoreDataPlease(datasetID);
                    break STATION_LOOP;
                }
            }

        } //end station loop
        if (!matchingStation)
            throw new SimpleException(MustBe.THERE_IS_NO_DATA + 
                " (There was no matching station.)");

        //do the final writeToTableWriter
        writeChunkToTableWriter(requestUrl, userDapQuery, table, tableWriter, true);
        if (reallyVerbose) String2.log("  getDataForDapQuery done. TIME=" +
            (System.currentTimeMillis() - getTime) + "ms"); 

    }


    /**
     * This reads the data from an IOOS server.
     *
     * @param kvp  the string to be added to the sourceUrl
     * @param table the table to which rows will be added
     * @param llatHash lon+lat+alt+time+stationID goes to row#

     */
    protected void readFromIoosNdbcNos(String kvp, Table table, HashMap llatHash) 
        throws Throwable {

        //downloading data may take time
        //so write to file, then quickly read and process
        //also this simplifies catching/processing xml error
        String grabFileName = cacheDirectory() + 
            "grabFile" + String2.md5Hex12(kvp);
        long downloadTime = System.currentTimeMillis();
        try {
            if (EDStatic.developmentMode && File2.isFile(grabFileName)) {} 
            else SSR.downloadFile(localSourceUrl + kvp, grabFileName, true);
            downloadTime = System.currentTimeMillis() - downloadTime;
        } catch (Throwable t) {
            //Normal error returns xml ExceptionReport.  So package anything else as WaitThenTryAgainException.
            String2.log("ERROR while trying to download from " + localSourceUrl + kvp + "\n" +
                MustBe.throwableToString(t));
            throw t instanceof WaitThenTryAgainException? t : 
                new WaitThenTryAgainException(EDStatic.waitThenTryAgain + 
                    "\n(" + t.toString() + ")"); 
        }

        try {

            //read the file
            String sar[] = String2.readFromFile(grabFileName, null, 2);
            //an error message from String2.readFromFile?
            if (sar[0].length() > 0) 
                throw new SimpleException(sar[0]);
            if (reallyVerbose) String2.log("ASCII response:\n" + 
                sar[1].substring(0, Math.min(4000, sar[1].length())) + " ...\n");
            
            //is it an xml file (presumably an error report)?
            if (sar[1].startsWith("<?xml")) {
                sar = null; //let it be garbage collected
                SimpleXMLReader xmlReader = new SimpleXMLReader(
                    File2.getDecompressedBufferedInputStream(grabFileName));
                try {
                    xmlReader.nextTag();
                    String tags = xmlReader.allTags();
                    String ofInterest1 = null;
                    String ofInterest2 = null;
                    String ofInterest3 = null;

                    //response is error message
                    if (tags.equals("<ServiceExceptionReport>") ||
                        tags.equals("<ExceptionReport>") || //nos coops 2014-12-24
                        tags.equals("<ows:ExceptionReport>")) { //ioosService
    //2014-12-22 ioos:
    //https://sdf.ndbc.noaa.gov/sos/server.php?service=SOS&version=1.0.0&request=GetObservation&offering=urn:ioos:station:wmo:46013&observedProperty=http://mmisw.org/ont/cf/parameter/waves&responseFormat=text/csv&eventTime=2008-08-01T00:00:00Z/2008-09-05T00:00:00Z
    //<ows:ExceptionReport xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:xsi="https://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.opengis.net/ows/1.1 owsExceptionReport.xsd" version="1.0.0" xml:lang="en">
    //<ows:Exception exceptionCode="InvalidParameterValue" locator="eventTime">
    //<ows:ExceptionText>No more than 31 days of data can be requested.</ows:ExceptionText>
    //</ows:Exception>
    //</ows:ExceptionReport>
                        //<?xml version="1.0"?>
                        //<ExceptionReport xmlns="http://www.opengis.net/ows" xmlns:xsi="https://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.opengis.net/ows owsExceptionReport.xsd" version="1.0.0" language="en">
                        //<Exception locator="service" exceptionCode="InvalidParamterValue">
                        //<ExceptionText>Unknown observedProperty parameter: sea_water_temperature,sea_water_salinity</ExceptionText></Exception>
                        //</ExceptionReport>
                        String errorText = "";
                        String2.log("\n  Error from " + localSourceUrl + kvp);
                        do {
                            //log full exception report
                            String content = xmlReader.content();
                            String2.log(tags);
                            if (content.length() > 0) String2.log("  content=" + content);
                            String atts = xmlReader.attributesCSV();
                            if (atts.length() > 0) String2.log("  atts=" + atts);

                            //process the tags
                            //String2.log("tags=" + tags + xmlReader.content());
                            if (tags.equals("<ExceptionReport><Exception>") ||
                                tags.equals("<ows:ExceptionReport><ows:Exception>")) {
                                if (xmlReader.attributeValue("exceptionCode") != null)
                                    errorText += xmlReader.attributeValue("exceptionCode") + ": ";
                                if (xmlReader.attributeValue("locator") != null)
                                    errorText += xmlReader.attributeValue("locator") + ": ";
                            }

                            if (tags.equals("<ServiceExceptionReport></ServiceException>") ||
                                tags.equals("<ExceptionReport><Exception></ExceptionText>") ||
                                tags.equals("<ows:ExceptionReport><ows:Exception></ows:ExceptionText>")) {
                                errorText = "Source Exception=\"" + errorText + content + "\".";
                                if (content.indexOf("No ") == 0 &&
                                    (content.indexOf("No data for ") == 0 ||
                                     content.indexOf(" data found for this station") > 0)) {
                                    //content is now something like
                                    //No currents data found for this station and date/time".
                                    //but "currents" will change
                                } else if (errorText.indexOf("NoApplicableCode:") >= 0) {
                                    //occurs if station advertises data, but GetObservation returns error
                                    //java.lang.Exception: Source Exception="NoApplicableCode: 1619910: 
                                    //There is no Air Temperature sensor installed at station 
                                    //urn:x-noaa:def:station:NOAA.NOS.CO-OPS:1619910 or dissemination has been stopped by CORMS.".
                                } else {
                                    throw new RuntimeException(errorText);
                                }
                            }

                            //get the next tag
                            xmlReader.nextTag();
                            tags = xmlReader.allTags();
                        } while (!tags.startsWith("</"));
                        if (errorText == null)
                            throw new RuntimeException("Source sent an ExceptionReport (no text).");
                        else return;

                    //response is unexpected
                    } else {
                        throw new RuntimeException("Error: unexpected XML content: first tag=" + tags + ".");
                    }
                } finally {
                    xmlReader.close();
                }
            }

            //read and process
            long processTime = System.currentTimeMillis();
            Table sosTable = new Table();
            boolean simplify = false;
            sosTable.readASCII(grabFileName, 
                new BufferedReader(new StringReader(sar[1])), 
                "", "", 0, 1, "", null, null, null, null, simplify); 
            sar = null; //encourage garbage collection

            //!!! DANGER: if server changes source names or units, 
            //  the code below will just return NaN (wrong!)
            //  so the datasets.xml must be updated.
            //So ensure that this dataset knows about all sosTable sourceNames.
            //!!! So all IOOS SOS datasets need to include all available variables!
            StringArray unexpectedColumns = new StringArray();
            dataVariableSourceNames(); //ensure it has been created
            int nSosTableColumns = sosTable.nColumns();
            for (int col = 0; col < nSosTableColumns; col++) {
                if (String2.indexOf(dataVariableSourceNames, sosTable.getColumnName(col)) < 0)
                    unexpectedColumns.add(sosTable.getColumnName(col));
            }
            if (unexpectedColumns.size() > 0) {
                String2.log("dataVariableSourceNames=" + String2.toCSSVString(dataVariableSourceNames));
                throw new SimpleException(String2.ERROR + ": unexpected column(s) in SOS response: " + 
                    unexpectedColumns.toString() + ".");
            }
            

            //find the corresponding column number in sosTable  (may be -1)
            int nTableColumns = table.nColumns();
            int columnInSosTable[] = new int[nTableColumns];
            StringArray notFound = new StringArray();
            for (int col = 0; col < nTableColumns; col++) { 
                columnInSosTable[col] = sosTable.findColumnNumber(table.getColumnName(col));
                if (columnInSosTable[col] < 0)
                    notFound.add(table.getColumnName(col));
            }
            //DANGER: This is the dangerous flip side of the 'Danger' situation above.
            //If the server stops serving a variable, there is no way for ERDDAP to know.
            //There just won't be any data for that variable -- all NaNs. 
            //At least all NaNs is an appropriate response.
            if (notFound.size() > 0)
                String2.log("WARNING: desired sourceNames not in SOS response: " + notFound.toString() +
                    "\n  sosTable has " + String2.toCSSVString(sosTable.getColumnNames()));
            else if (reallyVerbose) 
                String2.log(
                "SOS sourceNames=" + String2.toCSSVString(sosTable.getColumnNames()) +
                "\n  matchingColumnsInTable=" + String2.toCSSVString(columnInSosTable));

            //find sosTable columns with LLATI
            int sosTableLonCol       = sosTable.findColumnNumber(lonSourceName);
            int sosTableLatCol       = sosTable.findColumnNumber(latSourceName);
            int sosTableAltCol       = sosTable.findColumnNumber(altSourceName);
            int sosTableTimeCol      = sosTable.findColumnNumber(timeSourceName);
            int sosTableStationIdCol = sosTable.findColumnNumber(stationIdSourceName);

            //ensure lon, lat, time, id were found
            String tError1 = "Unexpected SOS response format: sourceName=";
            String tError2 = " wasn't found text/csv response from URL=\n" +
                localSourceUrl + kvp;
            if (sosTableLonCol       < 0) throw new SimpleException(tError1 + lonSourceName       + tError2);
            if (sosTableLatCol       < 0) throw new SimpleException(tError1 + latSourceName       + tError2);
            if (sosTableTimeCol      < 0) throw new SimpleException(tError1 + timeSourceName      + tError2);
            if (sosTableStationIdCol < 0) throw new SimpleException(tError1 + stationIdSourceName + tError2);

            //go through rows, adding to table  (and llatHash)
            int nSosRows = sosTable.nRows();
            for (int sosRow = 0; sosRow < nSosRows; sosRow++) {
                //process a row of data

                //make the hash key    (alt is the only optional col)
                String tHash =                  sosTable.getStringData(sosTableLonCol,       sosRow)       + "," +
                                                sosTable.getStringData(sosTableLatCol,       sosRow)       + "," +
                    (sosTableAltCol       >= 0? sosTable.getStringData(sosTableAltCol,       sosRow) : "") + "," + 
                                                sosTable.getStringData(sosTableTimeCol,      sosRow)       + "," +
                                                sosTable.getStringData(sosTableStationIdCol, sosRow); 

                //does a row with identical LonLatAltTimeID exist in table?
                int tRow = String2.parseInt((String)llatHash.get(tHash));
                if (tRow < Integer.MAX_VALUE) {
                    //merge this data into that row
                    for (int col = 0; col < nTableColumns; col++) {
                        int stci = columnInSosTable[col];
                        String ts  = stci < 0? "" : sosTable.getStringData(stci, sosRow);
                        String tso = table.getStringData(col, tRow);
                        if (ts  == null) ts = "";
                        //setting a number may change it a little, so do it 
                        table.setStringData(col, tRow, ts);
                        //if there was an old value, ensure that old value = new value
                        //leave this test in as insurance!
                        String tsn = table.getStringData(col, tRow); 
                        if (tso.length() > 0 && !tso.equals(tsn)) {
                            String2.log("URL=" + localSourceUrl + kvp);
                            throw new SimpleException(
                                "Error: there are two rows for lon,lat,alt,time,id=" + tHash + 
                                " and they have different data values (column=" + 
                                table.getColumnName(col) + "=" + tso + " and " + tsn + ").");
                        }
                    }
                } else {
                    //add this row
                    for (int col = 0; col < nTableColumns; col++) {
                        int stci = columnInSosTable[col];
                        String ts = stci < 0? "" : sosTable.getStringData(stci, sosRow);
                        table.getColumn(col).addString(ts == null? "" : ts);
                    }

                    llatHash.put(tHash, "" + (table.nRows() - 1));
                }
            }

            processTime = System.currentTimeMillis() - processTime;
            if (verbose) 
                String2.log("EDDTableFromSos nRows=" + table.nRows() + 
                    " downloadTime=" + downloadTime + 
                    "ms processTime=" + processTime + "ms");

        } catch (Throwable t) {
            String2.log("  " + String2.ERROR + " while processing response from requestUrl=" + localSourceUrl + kvp);
            throw t;

        } finally {
            if (!EDStatic.developmentMode) 
                File2.simpleDelete(grabFileName);  //don't keep in cache. SOS datasets change frequently.
        }
    }


    /**
     * This gets the data from a whoiServer.
     *
     * @param kvp  the string to be added to the sourceUrl
     * @param table the table to which rows will be added
     * @return the same table or a new table
     */
    protected void readFromWhoiServer(String kvp, Table table, HashMap llatHash,
        String tStationLonString, String tStationLatString, String tStationAltString, String tStationID) 
        throws Throwable {

        if (debugMode) String2.log("* readFromWhoiServer tStationID=" + tStationID);

        int tableLonCol       = table.findColumnNumber(lonSourceName);
        int tableLatCol       = table.findColumnNumber(latSourceName);
        int tableAltCol       = table.findColumnNumber(altSourceName);
        int tableTimeCol      = table.findColumnNumber(timeSourceName);
        int tableStationIdCol = table.findColumnNumber(stationIdSourceName);

        //make tableDVI, tableObservedProperties, isStringCol   (lon/lat/time/alt/id will be null)
        String tDataVariableSourceNames[] = dataVariableSourceNames();
        int nCol = table.nColumns();
        IntArray tableDVI = new IntArray();
        String tableObservedProperties[] = new String[nCol];
        boolean isStringCol[] = new boolean[nCol];
        for (int col = 0; col < nCol; col++) {
            int dvi = String2.indexOf(tDataVariableSourceNames, table.getColumnName(col));
            tableDVI.add(dvi);
            EDV edv = dataVariables[dvi];
            tableObservedProperties[col] = edv.combinedAttributes().getString("observedProperty");
            isStringCol[col] = edv.sourceDataPAType().equals(PAType.STRING);
        }


        InputStream in;
        SimpleXMLReader xmlReader;
        //values that need to be parsed from the xml and held
        IntArray fieldToCol = new IntArray(); //converts results field# to table col#
        String tokenSeparator = null, blockSeparator = null, decimalSeparator = null;
        try {    
            in = SSR.getUrlBufferedInputStream(localSourceUrl + kvp);
            xmlReader = new SimpleXMLReader(in);
            //request the data
            //???Future: need to break the request up into smaller time chunks???
            xmlReader.nextTag();
        } catch (Throwable t) {
            //Normal error returns xml ExceptionReport.  So package anything else as WaitThenTryAgainException.
            String2.log("ERROR while trying to getUrlInputStream from " + localSourceUrl + kvp + "\n" +
                MustBe.throwableToString(t));
            throw t instanceof WaitThenTryAgainException? t : 
                new WaitThenTryAgainException(EDStatic.waitThenTryAgain + 
                    "\n(" + t.toString() + ")"); 
        }
        try {

            String tags = xmlReader.allTags();
            if (tags.equals("<ServiceExceptionReport>")) {   
                //<?xml version="1.0" encoding="UTF-8"?>
                //<ServiceExceptionReport version="1.0">
                //<ServiceException>
                //Format - text/xml; subtype="om/1.0.0" - is not available for offering ADCP_DATA
                //</ServiceException>
                //</ServiceExceptionReport>
                boolean okay = false;
                do {
                    //process the tags
                    if (debugMode)
                        String2.log("tags=" + tags + xmlReader.content());
                    if (tags.equals("<ServiceExceptionReport></ServiceException>")) { 
                        String content = xmlReader.content();
                        //if no data, whoiServer returns empty swe:values csv table
                        //if (content.startsWith("Data not available")) {
                        //    okay = true; //no data for one station is not fatal error
                        //    if (reallyVerbose) String2.log("Exception: " + content);
                        //} else {
                            throw new RuntimeException("Source Exception=\"" + content + "\".");
                        //}
                    }

                    //get the next tag
                    xmlReader.nextTag();
                    tags = xmlReader.allTags();
                } while (!tags.startsWith("</"));

                if (!okay)
                    throw new RuntimeException("Source sent an ExceptionReport (no text).");
            } else {

                String ofInterest = null;
                if (tags.equals("<om:Observation>")) 
                    ofInterest = tags;
                else if (tags.equals("<om:ObservationCollection>")) 
                    ofInterest = "<om:ObservationCollection><om:member><om:Observation>";
                else throw new RuntimeException("Data source error when reading source xml: First tag=" + tags + 
                    " should have been <om:Observation> or <om:ObservationCollection>.");

                do {
                    //process the tags
                    //String2.log("tags=" + tags + xmlReader.content());
    /*  2020-07-09 whoiSos IS GONE. see email from Mathew Biddle.
       from http://mvcodata.whoi.edu:8080/q2o/adcp?service=SOS&version=1.0.0&responseFormat=text/xml;%20subtype%3D%22om/1.0%22&request=GetObservation&offering=ADCP_DATA&observedProperty=ALL_DATA&eventTime=2008-04-09T00:00:00Z/2008-04-09T01:00:00Z
       stored as C:/data/whoiSos/GetObervations.xml
    <om:ObservationCollection gml:id="ADCP_Observation" xmlns:gml="http://www.opengis.net/gml" xmlns:om="http://www.opengis.net/om/1.0" xmlns:swe="http://www.opengis.net/swe/1.0.1" xmlns:xlink="https://www.w3.org/1999/xlink" xmlns:xsi="https://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.opengis.net/om/1.0 http://schemas.opengis.net/om/1.0.0/observation.xsd">
        <!-- Observation name -->
        <gml:name>Data from ADCP profiler</gml:name>
        <om:member>
            <om:Observation>
                <!-- Observation time -->
                <om:samplingTime>
                    <gml:TimePeriod>
                        <gml:beginPosition>2008-04-09T00:00:00Z</gml:beginPosition>
                        <gml:endPosition>2008-04-09T01:00:00Z</gml:endPosition>
                    </gml:TimePeriod>
                </om:samplingTime>
                <!-- Sensor description (SensorML) -->
                <om:procedure xlink:href="http://mvcodata.whoi.edu/downloads/sensorML/v1.0/examples/sensors/ADCP_2.2/ADCP_System.xml"/>
                <!-- Observable is a composite containing all data for now  -->
                <om:observedProperty>
                    <swe:CompositePhenomenon dimension="24" gml:id="ALL_OBSERVABLES">
                        <gml:description>ALL Mesurements from ADCP_System including QC flags and bad data</gml:description>
                        <gml:name>ADCP measurements</gml:name>
                        <swe:component xlink:href="urn:ogc:phenomenon:time:iso8601"/>
                        ... other components (columns in the csv table)
    */

                    if (debugMode) String2.log(tags + xmlReader.content());

                    if (tags.startsWith(ofInterest)) { //i.e., within <om:Observation>
                        String endOfTag = tags.substring(ofInterest.length());
                        String content = xmlReader.content();
                        String error = null;

                        if (endOfTag.equals(
                            "<om:observedProperty><swe:CompositePhenomenon><swe:component>")) {
                            //e.g., xlink:href="urn:ogc:phenomenon:time:iso8601" />
                            String fieldName = xmlReader.attributeValue("xlink:href");
                            int col = table.findColumnNumber(fieldName);
                            fieldToCol.add(col);
                            if (debugMode) String2.log("  field=" + fieldName + " col=" + col);

                        } else if (endOfTag.equals("<swe:encoding><swe:TextBlock>")) {
                            //encoding indicates how the data is stored
                            //tokenSeparator="," blockSeparator=" " decimalSeparator="."
                            tokenSeparator   = xmlReader.attributeValue("tokenSeparator");
                            blockSeparator   = xmlReader.attributeValue("blockSeparator");
                            decimalSeparator = xmlReader.attributeValue("decimalSeparator");
                            if (debugMode) String2.log("  token=" + tokenSeparator + 
                                " block=" + blockSeparator + 
                                " decimal=" + decimalSeparator);

                        } else if (endOfTag.equals("<om:result><swe:DataArray></swe:values>")) {
                            //the results in one big csv block
                            //first, ensure fieldToCol doesn't have 2 references to same column
                            int nFields = fieldToCol.size();
                            if (reallyVerbose) String2.log("fieldToCol=" + fieldToCol);
                            for (int field = 0; field < nFields; field++) {
                                int col = fieldToCol.get(field);
                                if (col >= 0) { //several may be -1
                                    if (fieldToCol.indexOf(col, 0) != field) //ensure none before it are the same
                                        throw new RuntimeException("Two fieldToCol=" + fieldToCol + 
                                            " have the same table column reference (col#" + 
                                            col + "=" + table.getColumnName(col) + ").");
                                }
                            }

                            //ensure separators are set (to defaults)
                            if (tokenSeparator   == null) tokenSeparator = ",";
                            if (blockSeparator   == null) blockSeparator = " ";  //rowSeparator
                            if (decimalSeparator == null) decimalSeparator = ".";
                            boolean changeDecimalSeparator = !decimalSeparator.equals(".");

                            //process the content (the results in one big csv block)
                            //2008-04-09T00:00:00,41.3366,-70.5564,0.0,1128.3,73.2,9,0.06,0.16,97.8,71.1,38.4,6,10,5,156.0,159.4,155.3,9.6,3.1,0,0,0,0,0 
        //???how are Strings quoted?
                            int po = 0;  //next po to look at
                            int contentLength = content.length();
                            int nCols = table.nColumns();
                            while (po < contentLength) {

                                //process a row of data
                                int nRows1 = table.nRows() + 1;
                                String rowValues[] = new String[nCols];
                                for (int field = 0; field < nFields; field++) {
                                    String sep = field < nFields - 1? tokenSeparator : blockSeparator;
                                    int po2 = content.indexOf(sep, po);
                                    if (po2 < 0) 
                                        po2 = contentLength;
                                    String value = content.substring(po, po2);
                                    int col = fieldToCol.get(field);
                                    if (col >= 0) {
                                        //deal with decimalSeparator for numeric Columns
                                        if (changeDecimalSeparator && !isStringCol[col])
                                            value = String2.replaceAll(value, decimalSeparator, ".");
                                        rowValues[col] = value;
                                        if (debugMode) 
                                            String2.log("field=" + field + " col=" + col + " " + 
                                            table.getColumnName(col) + " value=" + value);
                                    }                            
                                    po = Math.min(contentLength, po2 + sep.length());
                                }

                                //add lat lon alt data
                                //it's usually in the result fields, but not always
                                if (tableLonCol >= 0 && rowValues[tableLonCol] == null)
                                    rowValues[tableLonCol] = tStationLonString;
                                if (tableLatCol >= 0 && rowValues[tableLatCol] == null)
                                    rowValues[tableLatCol] = tStationLatString;
                                if (tableAltCol >= 0 && rowValues[tableAltCol] == null)
                                    rowValues[tableAltCol] = tStationAltString;
                                if (tableStationIdCol >= 0 && rowValues[tableStationIdCol] == null)
                                    rowValues[tableStationIdCol] = tStationID;

                                //make the hash key
                                String tHash = rowValues[tableLonCol] + "," + rowValues[tableLatCol] + 
                                         "," + rowValues[tableAltCol] + "," + rowValues[tableTimeCol] +
                                         "," + rowValues[tableStationIdCol];

                                //ensure lon, lat, time, id where found
                                String tError1 = "Unexpected SOS response format: ";
                                String tError2 = " wasn't found.\n" +
                                    "(L,L,A,T,ID=" + tHash + ")\n" +
                                    "URL=" + localSourceUrl + kvp;
                                if (rowValues[tableLonCol] == null || rowValues[tableLonCol].length() == 0)
                                    throw new SimpleException(tError1 + "longitude" + tError2);
                                if (rowValues[tableLatCol] == null || rowValues[tableLatCol].length() == 0)
                                    throw new SimpleException(tError1 + "latitude" + tError2);
                                if (rowValues[tableTimeCol] == null || rowValues[tableTimeCol].length() == 0)
                                    throw new SimpleException(tError1 + "time" + tError2);
                                if (rowValues[tableStationIdCol] == null || rowValues[tableStationIdCol].length() == 0)
                                    throw new SimpleException(tError1 + stationIdSourceName + tError2);

                                //does a row with identical LonLatAltTimeID exist in table?
                                int tRow = String2.parseInt((String)llatHash.get(tHash));
                                if (tRow < Integer.MAX_VALUE) {
                                    //merge this data into that row
                                    for (int col = 0; col < nCols; col++) {
                                        String ts = rowValues[col];
                                        if (ts != null) {
                                            PrimitiveArray pa = table.getColumn(col);
                                            if (true || verbose) { 
                                                //if there was an old value, ensure that old value = new value
                                                //leave this test in as insurance!
                                                String tso = pa.getString(tRow);
                                                pa.setString(tRow, ts);
                                                ts = pa.getString(tRow); //setting a number changes it, e.g., 1 -> 1.0
                                                if (tso.length() > 0 && !tso.equals(ts)) {
                                                    String2.log("URL=" + localSourceUrl + kvp);
                                                    throw new SimpleException(
                                                        "Error: there are two rows for lon,lat,alt,time,id=" + tHash + 
                                                        " and they have different data values (column=" + 
                                                        table.getColumnName(col) + "=" + tso + " and " + ts + ").");
                                                }
                                            } else {
                                                pa.setString(tRow, ts);
                                            }
                                        }
                                    }
                                } else {
                                    //add this row
                                    for (int col = 0; col < nCols; col++) {
                                        String ts = rowValues[col];
                                        table.getColumn(col).addString(ts == null? "" : ts);
                                        //String2.log(col + " " + table.getColumnName(col) + " " + ts);
                                    }

                                    llatHash.put(tHash, "" + (table.nRows() - 1));
                                }
                            }
                        }

                        //handle the error
                        if (error != null)
                            throw new RuntimeException(
                                "Data source error on xml line #" + xmlReader.lineNumber() + 
                                ": " + error);
                    }

                    //get the next tag
                    xmlReader.nextTag();
                    tags = xmlReader.allTags();
                } while (!tags.startsWith("</"));

            }
        } finally {
            xmlReader.close();  
        }

    }

    /**
     * This gets the data from an Oostethys SOS server.
     * Before 2012-04, was http://www.gomoos.org/cgi-bin/sos/V1.0/oostethys_sos.cgi 
     * Now http://oceandata.gmri.org/cgi-bin/sos/V1.0/oostethys_sos.cgi
     *
     * @param kvp  the string to be added to the sourceUrl
     * @param table the table to which rows will be added
     * @return the same table or a new table
     */
    protected void readFromOostethys(String kvp, Table table, HashMap llatHash,
        String tStationLonString, String tStationLatString, String tStationAltString, 
        String tStationID) throws Throwable {

        int tableLonCol       = table.findColumnNumber(lonSourceName);
        int tableLatCol       = table.findColumnNumber(latSourceName);
        int tableAltCol       = table.findColumnNumber(altSourceName);
        int tableTimeCol      = table.findColumnNumber(timeSourceName);
        int tableStationIdCol = table.findColumnNumber(stationIdSourceName);

        //make tableDVI, tableObservedProperties, isStringCol   (lon/lat/time/alt/id will be null)
        String tDataVariableSourceNames[] = dataVariableSourceNames();
        int nCol = table.nColumns();
        IntArray tableDVI = new IntArray();
        String tableObservedProperties[] = new String[nCol];
        boolean isStringCol[] = new boolean[nCol];
        for (int col = 0; col < nCol; col++) {
            int dvi = String2.indexOf(tDataVariableSourceNames, table.getColumnName(col));
            tableDVI.add(dvi);
            EDV edv = dataVariables[dvi];
            tableObservedProperties[col] = edv.combinedAttributes().getString("observedProperty");
            isStringCol[col] = edv.sourceDataPAType().equals(PAType.STRING);
        }


        InputStream in;
        SimpleXMLReader xmlReader;

        //values that need to be parsed from the xml and held
        IntArray fieldToCol = new IntArray(); //converts results field# to table col#
        String tokenSeparator = null, blockSeparator = null, decimalSeparator = null;

        try {
            in = SSR.getUrlBufferedInputStream(localSourceUrl + kvp);
            //request the data
            //???Future: need to break the request up into smaller time chunks???
            xmlReader = new SimpleXMLReader(in);
            xmlReader.nextTag();
        } catch (Throwable t) {
            //Normal error returns xml ExceptionReport.  So package anything else as WaitThenTryAgainException.
            String2.log("ERROR while trying to getUrlInputStream from " + localSourceUrl + kvp + "\n" +
                MustBe.throwableToString(t));
            throw t instanceof WaitThenTryAgainException? t : 
                new WaitThenTryAgainException(EDStatic.waitThenTryAgain + 
                    "\n(" + t.toString() + ")"); 
        }

        try {

            String tags = xmlReader.allTags();
            //String2.log("tags=" + tags + "\ncontent=" + xmlReader.content());
            if (tags.equals("<ServiceExceptionReport>") || //tamu
                tags.equals("<ows:ExceptionReport>")) {   //gomoos, ioos
    //2014-12-22 ioos:
    //https://sdf.ndbc.noaa.gov/sos/server.php?service=SOS&version=1.0.0&request=GetObservation&offering=urn:ioos:station:wmo:46013&observedProperty=http://mmisw.org/ont/cf/parameter/waves&responseFormat=text/csv&eventTime=2008-08-01T00:00:00Z/2008-09-05T00:00:00Z
    //<ows:ExceptionReport xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:xsi="https://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.opengis.net/ows/1.1 owsExceptionReport.xsd" version="1.0.0" xml:lang="en">
    //<ows:Exception exceptionCode="InvalidParameterValue" locator="eventTime">
    //<ows:ExceptionText>No more than 31 days of data can be requested.</ows:ExceptionText>
    //</ows:Exception>
    //</ows:ExceptionReport>
                boolean okay = false;
                do {
                    //process the tags
                    if (debugMode)
                        String2.log("tags=" + tags + xmlReader.content());
                    if (tags.equals("<ServiceExceptionReport></ServiceException>") ||  //tamu
                        tags.equals("<ows:ExceptionReport><ows:Exception></ows:ExceptionText>")) { //gomoos, ioos
                        String content = xmlReader.content();
                        if (content.startsWith("Data not available")) {
                            okay = true; //no data for one station is not fatal error
                            if (reallyVerbose) String2.log("Exception: " + content);
                        } else {
                            throw new RuntimeException("Source Exception=\"" + content + "\".");
                        }
                    }

                    //get the next tag
                    xmlReader.nextTag();
                    tags = xmlReader.allTags();
                } while (!tags.startsWith("</"));

                if (!okay)
                    throw new RuntimeException("Source sent an ExceptionReport (no text).");
            } else {

                String ofInterest = null;
                if (tags.equals("<om:Observation>")) 
                    ofInterest = tags;
                else if (tags.equals("<om:ObservationCollection>")) 
                    ofInterest = "<om:ObservationCollection><om:member><om:Observation>";
                else throw new RuntimeException("Data source error when reading source xml: First tag=" + tags + 
                    " should have been <om:Observation> or <om:ObservationCollection>.");

                do {
                    //process the tags
                    if (debugMode) String2.log(tags + xmlReader.content());

                    if (tags.startsWith(ofInterest)) { //i.e., within <om:Observation>
                        String endOfTag = tags.substring(ofInterest.length());
                        String content = xmlReader.content();
                        String error = null;

                        if (endOfTag.equals("<om:featureOfInterest><swe:GeoReferenceableFeature>" +
                            "<gml:location><gml:Point></gml:coordinates>")) {
                            //lat lon alt    if present, has precedence over station table
                            //VAST has this; others don't
                            String lla[] = String2.split(content, ' ');
                            if (lla.length >= 2) {
                                tStationLatString = lla[0];
                                tStationLonString = lla[1];
                                if (lla.length >= 3)
                                    tStationAltString = lla[2];
                            }
                        } else if (//endOfTag.equals("<om:resultDefinition><swe:DataBlockDefinition>" +
                                   //         "<swe:components><swe:DataRecord><swe:field>") ||  //old?
                                   endOfTag.equals("<om:result><swe:DataArray>" +
                                            "<swe:elementType><swe:DataRecord><swe:field>")) {
                            //field    PlatformName, latitude, longitude, time, depth have this
                            //other fields have "observedProperty6"; see "definition" below
                            String fieldName = xmlReader.attributeValue("name");
                            int col = table.findColumnNumber(fieldName);
                            fieldToCol.add(col);
                            if (debugMode) String2.log("*** field name found: col=" + col + 
                                " fieldName=" + fieldName);

                        } else if (endOfTag.equals("<om:result><swe:DataArray>" +
                                   "<swe:elementType><swe:DataRecord><swe:field><swe:Quantity>")) {
                            //definition   use this if field name was "observedProperty"i
                            int nFields = fieldToCol.size();
                            if (nFields > 0 && fieldToCol.get(nFields - 1) < 0) {
                                String definition = xmlReader.attributeValue("definition");
                                int col = String2.indexOf(tableObservedProperties, definition);
                                fieldToCol.set(nFields - 1, col); //change from -1 to col
                                if (debugMode) 
                                    String2.log("*** field definition found: col=" + col + 
                                        " definition=" + definition);
                            }

                        } else if (//endOfTag.equals("<om:resultDefinition><swe:DataBlockDefinition>" +
                                   //         "<swe:encoding><swe:AsciiBlock>") ||  //old oostethys has this
                                   //endOfTag.equals("<om:resultDefinition><swe:DataBlockDefinition>" +
                                   //         "<swe:encoding><swe:TextBlock>")) {   //old VAST has this
                                   endOfTag.equals("<om:result><swe:DataArray><swe:encoding>")) {
                            //encoding indicates how the data is stored
                            //tokenSeparator="," blockSeparator=" " decimalSeparator="."
                            tokenSeparator   = xmlReader.attributeValue("tokenSeparator");
                            blockSeparator   = xmlReader.attributeValue("blockSeparator");
                            decimalSeparator = xmlReader.attributeValue("decimalSeparator");
                            if (debugMode) String2.log("  token=" + tokenSeparator + 
                                " block=" + blockSeparator + 
                                " decimal=" + decimalSeparator);

                        } else if (//endOfTag.equals("</om:result>")) { //old
                                   endOfTag.equals("<om:result><swe:DataArray></swe:values>")) {
                            //the results in one big block
                            //first, ensure fieldToCol doesn't have 2 references to same column
                            int nFields = fieldToCol.size();
                            if (reallyVerbose) String2.log("fieldToCol=" + fieldToCol);
                            for (int field = 0; field < nFields; field++) {
                                int col = fieldToCol.get(field);
                                if (col >= 0) { //several may be -1
                                    if (fieldToCol.indexOf(col, 0) != field) //ensure none before it are the same
                                        throw new RuntimeException("Two fieldToCol=" + fieldToCol + 
                                            " have the same table column reference (col#" + 
                                            col + "=" + table.getColumnName(col) + ").");
                                }
                            }

                            //ensure separators are set (to defaults)
                            if (tokenSeparator   == null) tokenSeparator = ",";
                            if (blockSeparator   == null) blockSeparator = " ";  //rowSeparator
                            if (decimalSeparator == null) decimalSeparator = ".";
                            boolean changeDecimalSeparator = !decimalSeparator.equals(".");

                            //process the content (the results in one big block)
                            //  <om:result>2007-06-18T00:50:00Z,34.68,-72.66,0,24.3 ...
        //???how are Strings quoted?
                            int po = 0;  //next po to look at
                            int contentLength = content.length();
                            int nCols = table.nColumns();
                            while (po < contentLength) {

                                //process a row of data
                                int nRows1 = table.nRows() + 1;
                                String rowValues[] = new String[nCols];
                                for (int field = 0; field < nFields; field++) {
                                    String sep = field < nFields - 1? tokenSeparator : blockSeparator;
                                    int po2 = content.indexOf(sep, po);
                                    if (po2 < 0) 
                                        po2 = contentLength;
                                    String value = content.substring(po, po2);
                                    int col = fieldToCol.get(field);
                                    if (col >= 0) {
                                        //deal with decimalSeparator for numeric Columns
                                        if (changeDecimalSeparator && !isStringCol[col])
                                            value = String2.replaceAll(value, decimalSeparator, ".");
                                        rowValues[col] = value;
                                        if (debugMode) 
                                            String2.log("field=" + field + " col=" + col + " " + 
                                            table.getColumnName(col) + " value=" + value);
                                    }                            
                                    po = Math.min(contentLength, po2 + sep.length());
                                }

                                //add lat lon alt data
                                //it's usually in the result fields, but not always
                                if (tableLonCol >= 0 && rowValues[tableLonCol] == null)
                                    rowValues[tableLonCol] = tStationLonString;
                                if (tableLatCol >= 0 && rowValues[tableLatCol] == null)
                                    rowValues[tableLatCol] = tStationLatString;
                                if (tableAltCol >= 0 && rowValues[tableAltCol] == null)
                                    rowValues[tableAltCol] = tStationAltString;
                                if (tableStationIdCol >= 0 && rowValues[tableStationIdCol] == null)
                                    rowValues[tableStationIdCol] = tStationID;

                                //make the hash key
                                String tHash = rowValues[tableLonCol] + "," + rowValues[tableLatCol] + 
                                         "," + rowValues[tableAltCol] + "," + rowValues[tableTimeCol] +
                                         "," + rowValues[tableStationIdCol];

                                //ensure lon, lat, time, id where found
                                String tError1 = "Unexpected SOS response format: ";
                                String tError2 = " wasn't found.\n" +
                                    "(L,L,A,T,ID=" + tHash + ")\n" +
                                    "URL=" + localSourceUrl + kvp;
                                if (rowValues[tableLonCol] == null || rowValues[tableLonCol].length() == 0)
                                    throw new SimpleException(tError1 + "longitude" + tError2);
                                if (rowValues[tableLatCol] == null || rowValues[tableLatCol].length() == 0)
                                    throw new SimpleException(tError1 + "latitude" + tError2);
                                if (rowValues[tableTimeCol] == null || rowValues[tableTimeCol].length() == 0)
                                    throw new SimpleException(tError1 + "time" + tError2);
                                if (rowValues[tableStationIdCol] == null || rowValues[tableStationIdCol].length() == 0)
                                    throw new SimpleException(tError1 + stationIdSourceName + tError2);

                                //does a row with identical LonLatAltTimeID exist in table?
                                int tRow = String2.parseInt((String)llatHash.get(tHash));
                                if (tRow < Integer.MAX_VALUE) {
                                    //merge this data into that row
                                    for (int col = 0; col < nCols; col++) {
                                        String ts = rowValues[col];
                                        if (ts != null) {
                                            PrimitiveArray pa = table.getColumn(col);
                                            if (true || verbose) { 
                                                //if there was an old value, ensure that old value = new value
                                                //leave this test in as insurance!
                                                String tso = pa.getString(tRow);
                                                pa.setString(tRow, ts);
                                                ts = pa.getString(tRow); //setting a number changes it, e.g., 1 -> 1.0
                                                if (tso.length() > 0 && !tso.equals(ts)) {
                                                    String2.log("URL=" + localSourceUrl + kvp);
                                                    throw new SimpleException(
                                                        "Error: there are two rows for lon,lat,alt,time,id=" + tHash + 
                                                        " and they have different data values (column=" + 
                                                        table.getColumnName(col) + "=" + tso + " and " + ts + ").");
                                                }
                                            } else {
                                                pa.setString(tRow, ts);
                                            }
                                        }
                                    }
                                } else {
                                    //add this row
                                    for (int col = 0; col < nCols; col++) {
                                        String ts = rowValues[col];
                                        table.getColumn(col).addString(ts == null? "" : ts);
                                        //String2.log(col + " " + table.getColumnName(col) + " " + ts);
                                    }

                                    llatHash.put(tHash, "" + (table.nRows() - 1));
                                }
                            }
                        }

                        //handle the error
                        if (error != null)
                            throw new RuntimeException(
                                "Data source error on xml line #" + xmlReader.lineNumber() + 
                                ": " + error);
                    }

                    //get the next tag
                    xmlReader.nextTag();
                    tags = xmlReader.allTags();
                } while (!tags.startsWith("</"));

            }
        } finally {
            xmlReader.close();  
        }
    }

    /**
     *
     * @param inputStream Best if buffered.
     */
    public static Table readIoos52NXmlDataTable(InputStream inputStream) throws Exception {
        SimpleXMLReader xmlReader = new SimpleXMLReader(inputStream);
        try {
            xmlReader.nextTag();
            String tags = xmlReader.allTags();

            if (!xmlReader.tag(0).equals("om:ObservationCollection")) 
                throw new IllegalArgumentException(
                    "For IOOS 52N SOS servers, the first SOS tag should have been " +
                    "<om:ObservationCollection>, not " + tags + ".");
            Table table = new Table();
    /* */
    //<om:ObservationCollection><om:member><om:Observation><om:result>
    //<swe2:DataRecord>
    //<swe2:field name="observationData">  //there are lots of fields; only this matters
    //<swe2:DataArray definition="http://mmisw.org/ont/ioos/swe_element_type/sensorObservationCollection">
    //  <swe2:elementCount><swe2:Count><swe2:value>5312</swe2:value>
    //  <swe2:elementType name="observations">
    //    <swe2:DataRecord definition="http://mmisw.org/ont/ioos/swe_element_type/sensorObservations">
    //      <swe2:field name="time">
    //      ... other fields

    //        <swe2:DataChoice definition="http://mmisw.org/ont/ioos/swe_element_type/sensors">
    //          <swe2:item name="comps_42013_airpressure">
    //            <swe2:DataRecord definition="http://mmisw.org/ont/ioos/swe_element_type/sensor">
    //              <swe2:field name="air_pressure">
    //                <swe2:Quantity definition="http://mmisw.org/ont/cf/parameter/air_pressure">
    //                  <swe2:uom xlink:href="urn:ogc:def:uom:udunits:2:hPa"/>

    //        <swe2:encoding>
    //          <swe2:TextEncoding decimalSeparator="." tokenSeparator="," blockSeparator="&#10;"/>

            int capacity = 128;
            String obsDataTags = //but only if attribute name="observationData"
                "<om:ObservationCollection><om:member><om:Observation><om:result>" +
                "<swe2:DataRecord><swe2:field>"; 
            String dataArrayTags = obsDataTags + "<swe2:DataArray>";
            String elementCountTags = dataArrayTags + 
                "<swe2:elementCount><swe2:Count></swe2:value>";
            String fieldName1Tags = dataArrayTags + 
                "<swe2:elementType><swe2:DataRecord><swe2:field>"; // name="time" or "sensor">
            String fieldName2Tags = fieldName1Tags + 
                "<swe2:DataChoice><swe2:item><swe2:DataRecord><swe2:field>"; // name="air_pressure">
            String unitsTags = fieldName2Tags +
                "<swe2:Quantity><swe2:uom>"; // xlink:href="urn:ogc:def:uom:udunits:2:hPa"
            String endDataRecordTags = fieldName1Tags + 
                "<swe2:DataChoice><swe2:item></swe2:DataRecord>"; 
            String textEncodingTags = dataArrayTags +
                "<swe2:encoding><swe2:TextEncoding>";
            String csvTags = dataArrayTags + 
                "</swe2:values>";
            char tokenSeparator = ',';
            String blockSeparator = "\n";

            //skip quickly to obsDataTags with name=observationData
            while (true) {
                xmlReader.nextTag();
                //uninteresting <swe2:field>?
                if (xmlReader.stackSize() == 0) 
                    throw new RuntimeException("Expected tag not found: " + obsDataTags);
                if (xmlReader.stackSize() == 6 &&
                    xmlReader.topTag().equals("swe2:field") &&
                    xmlReader.allTags().equals(obsDataTags) && 
                    "observationData".equals(xmlReader.attributeValue("name"))) 
                    break;
            }
           
            xmlReader.nextTag();
            tags = xmlReader.allTags();
            do {
                //process the tags
                if (debugMode) String2.log("tags=" + tags + xmlReader.content());

                //elementCount
                if (tags.equals(elementCountTags)) {
                    int ti = String2.parseInt(xmlReader.content());
                    if (ti > 0 && ti < Integer.MAX_VALUE) {
                        capacity = ti;
                        if (debugMode) String2.log("  elementCount=" + ti);
                    }

                //primary fieldName
                } else if (tags.equals(fieldName1Tags)) {
                    String ts = xmlReader.attributeValue("name");
                    if (String2.isSomething(ts)) {
                        table.addColumn(
                            ts.equals("sensor")? stationIdDestinationName : ts,
                            PrimitiveArray.factory(
                                ts.equals("time") || ts.equals("sensor")? PAType.STRING : PAType.DOUBLE,
                                capacity, false)); //active?
                    }

                //secondary fieldName
                } else if (tags.equals(fieldName2Tags)) {
                    String ts = xmlReader.attributeValue("name");
                    if (String2.isSomething(ts)) {
                        table.addColumn(ts, 
                            PrimitiveArray.factory(PAType.DOUBLE, capacity, false)); //active?
                    }

                //units (for last created column)
                } else if (tags.equals(unitsTags)) {
                    String ts = xmlReader.attributeValue("xlink:href");
                    if (String2.isSomething(ts)) {
                        table.columnAttributes(table.nColumns() - 1).add("units",
                            ts.substring(ts.lastIndexOf(":") + 1));
                    }

                //end of secondary fieldNames and units 
                } else if (tags.equals(endDataRecordTags)) {
                    xmlReader.skipToStackSize(8);
                    //should be at </swe2:elementType> after outer </swe2:DataRecord>
                    Test.ensureEqual(xmlReader.topTag(), "/swe2:elementType", 
                        "skipToStacksize is wrong");

                //encoding 
                } else if (tags.equals(textEncodingTags)) {
                    String ts = xmlReader.attributeValue("tokenSeparator");
                    if (String2.isSomething(ts)) {
                        tokenSeparator = ts.charAt(0);
                        if (reallyVerbose) 
                            String2.log("  tokenSeparator=" + String2.annotatedString("" + ts));
                    }
                    ts = xmlReader.attributeValue("blockSeparator");
                    if (String2.isSomething(ts)) {
                        blockSeparator = ts;
                        if (reallyVerbose) 
                            String2.log("  blockSeparator=" + String2.annotatedString(ts));
                    }

                //data
                } else if (tags.equals(csvTags)) {
                    String content = xmlReader.content();
                    int start = 0;
                    int stop = content.indexOf(blockSeparator);
                    boolean isCsv = ",".equals(tokenSeparator);
                    int nCols = table.nColumns();
                    int stationCol = table.findColumnNumber(stationIdDestinationName);
                    PrimitiveArray pa[] = new PrimitiveArray[nCols];
                    for (int col = 0; col < nCols; col++)
                        pa[col] = table.getColumn(col);
                    while (true) {
                        //get the items on one line
                        String oneLine = content.substring(start, 
                            stop == -1? content.length() : stop).trim();
                        String items[] = isCsv?
                            StringArray.arrayFromCSV(oneLine) :  //does handle "'d phrases
                            String2.split(oneLine, tokenSeparator);
                        if (items.length > stationCol) {
                            int po = items[stationCol].lastIndexOf("_");
                            if (po >= 0)
                                items[stationCol] = items[stationCol].substring(0, po);
                        }

                        //store the items
                        for (int col = 0; col < nCols; col++) { 
                            //so always fill each column in table
                            pa[col].addString(col < items.length? items[col] : "");
                        }

                        //get the next start stop
                        if (stop == -1 || stop >= content.length() - 1)
                            break;
                        start = stop + 1;
                        stop = content.indexOf(blockSeparator, start);
                    }
                }

                //read next tag
                xmlReader.nextTag();
                tags = xmlReader.allTags();

            } while (!tags.startsWith("</")); //end of file

            return table;
        } finally {
            xmlReader.close();      
        }
    }

private static String standardSummary = //from http://www.oostethys.org/ogc-oceans-interoperability-experiment  [GONE]
"The OCEANS IE -- formally approved as an OGC Interoperability\n" +
"Experiment in December 2006 -- engages data managers and scientists\n" +
"in the Ocean-Observing community to advance their understanding and\n" +
"application of various OGC specifications, solidify demonstrations\n" +
"for Ocean Science application areas, harden software\n" +
"implementations, and produce candidate OGC Best Practices documents\n" +
"that can be used to inform the broader ocean-observing community.\n" +
//"To achieve these goals, the OCEANS IE engages the OGC membership\n" +
//"to assure that any recommendations from the OCEANS IE will\n" +
//"properly leverage the OGC specifications. The OCEANS IE could\n" +
//"prompt Change Requests on OGC Specifications, which would be\n" +
//"provided to the OGC Technical Committee to influence the\n" +
//"underlying specifications. However, this IE will not develop\n" +
//"any new specifications, rather, participants will implement,\n" +
//"test and document experiences with existing specifications.\n" +
"\n" +
"Because of the nature of SOS requests, requests for data MUST\n" +
"include constraints for the longitude, latitude, time, and/or\n" +
"station_id variables.\n" +
"\n" +
"Initiators: SURA (lead), Texas A&M University, MBARI, GoMOOS and\n" +
"Unidata.\n" +
"\n" +
"Specific goals:\n" +
"* Compare Sensor Observation Service (SOS) from the OGC's Sensor\n" +
"  Web Enablement (SWE) initiative to the Web Feature Service (WFS)\n" +
"  as applied to ocean data in a variety of data formats including\n" +
"  text files, netCDF files, relational databases, and possibly\n" +
"  native sensor output; (see Experiment #1 for details)\n" +
"* Make use of semantic mediation via Semantic Web technologies to\n" +
"  allow plurality of identification for source types (platforms\n" +
"  and sensors) and phenomena types;\n" +
"* Test aggregation services and caching strategies to provide\n" +
"  efficient queries;\n" +
"* Explore possible enhancements of THREDDS server, so that THREDDS\n" +
"  resident data sources might be made available via SOS or WFS;";   //better summary? 


// IRIS - This SOS listed at 
//  http://www.oostethys.org/ogc-oceans-interoperability-experiment/experiment-1 [GONE]
//  (it may list others in future)
// Specifically http://demo.transducerml.org:8080/ogc/
//But the results are a tml reference. For now, don't get into that.
//WAIT!!! Can I request other results format???





    /**
     * This should work, but server is in flux so it often breaks.
     * Send questions about NOS SOS to Andrea.Hardy@noaa.gov.
     *
     * @throws Throwable if trouble
     */
    public static void testNosSosATemp(String datasetIdPrefix) throws Throwable {
        testVerboseOn();
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, datasetIdPrefix + "nosSosATemp");  

        String name, tName, results, expected, userDapQuery;

        try { 

            //it was hard to find data. station advertises 1999+ for several composites.
            //but searching January's for each year found no data until 2006
            String2.log("\n*** EDDTableFromSOS nos AirTemperature test get one station .CSV data\n");
            tName = eddTable.makeNewFileForDapQuery(null, null,
                "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:8419317\"&time>=2006-01-01T00&time<=2006-01-01T01", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_nosSosATemp", ".csv"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"longitude,latitude,station_id,altitude,time,sensor_id,air_temperature,quality_flags\n" +
"degrees_east,degrees_north,,m,UTC,,degree_C,\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-3.9,0;0;0\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-3.9,0;0;0\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-3.9,0;0;0\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-3.9,0;0;0\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-3.9,0;0;0\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-3.9,0;0;0\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-3.9,0;0;0\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-4.0,0;0;0\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-4.0,0;0;0\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-4.1,0;0;0\n";
            Test.ensureEqual(results, expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NOS Server is in flux.", t); 
        }


        try {
            String2.log("\n*** EDDTableFromSOS nos AirTemperature test get one station .CSV data\n");
            tName = eddTable.makeNewFileForDapQuery(null, null,  //1612340, NaN, 2008-10-26
                "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:1612340\"&time>=2008-10-26T00&time<2008-10-26T01", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_nosSosATemp", ".csv"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"longitude,latitude,station_id,altitude,time,sensor_id,air_temperature,quality_flags\n" +
"degrees_east,degrees_north,,m,UTC,,degree_C,\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.9,0;0;0\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8,0;0;0\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.7,0;0;0\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8,0;0;0\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.7,0;0;0\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8,0;0;0\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.7,0;0;0\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8,0;0;0\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8,0;0;0\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.7,0;0;0\n";
            Test.ensureEqual(results, expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NOS Server is in flux.", t); 
        }

        try {
            String2.log("\n*** EDDTableFromSOS nos AirTemperature .das\n");
            String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
            tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                eddTable.className() + "_nosSosATemp", ".das"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"Attributes {\n" +
" s {\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -177.36, 166.6176;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range -14.2767, 70.4114;\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  station_id {\n" +
"    String cf_role \"timeseries_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station ID\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    String axis \"Z\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range -3.6757152e+9, NaN;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  sensor_id {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Sensor ID\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/air_temperature\";\n" +
"  }\n" +
"  air_temperature {\n" +
"    Float64 colorBarMaximum 40.0;\n" +
"    Float64 colorBarMinimum -10.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Air Temperature\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/air_temperature\";\n" +
"    String standard_name \"air_temperature\";\n" +
"    String units \"degree_C\";\n" +
"  }\n" +
"  quality_flags {\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Quality Flags\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/air_temperature\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"TimeSeries\";\n" +
"    String cdm_timeseries_variables \"station_id, longitude, latitude, sensor_id\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    Float64 Easternmost_Easting 166.6176;\n" +
"    String featureType \"TimeSeries\";\n" +
"    Float64 geospatial_lat_max 70.4114;\n" +
"    Float64 geospatial_lat_min -14.2767;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 166.6176;\n" +
"    Float64 geospatial_lon_min -177.36;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"" + today;
//+ " https://opendap.co-ops.nos.noaa.gov/ioos-dif-sos"; //"-test"
            Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);

expected = 
"    Float64 Southernmost_Northing -14.2767;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String subsetVariables \"station_id, longitude, latitude\";\n" +
"    String summary \"The NOAA NOS SOS server is part of the IOOS DIF SOS Project.  The stations in this dataset have air temperature data.  ****These services are for testing and evaluation use only****\n" +
"\n" +
"Because of the nature of SOS requests, requests for data MUST include constraints for the longitude, latitude, time, and/or station_id variables.\";\n" +
"    String time_coverage_start \"1853-07-10T00:00:00Z\";\n" +
"    String title \"NOAA NOS SOS, EXPERIMENTAL, 1853-present, Air Temperature\";\n" +
"    Float64 Westernmost_Easting -177.36;\n" +
"  }\n" +
"}\n";
            int po = Math.max(0, results.indexOf(expected.substring(0, 30)));
            Test.ensureEqual(results.substring(po), expected, "RESULTS=\n" + results);

        } catch (Throwable t) {
            throw new RuntimeException("Unexpected error " + datasetIdPrefix + "nosSosATemp. NOS SOS Server is in flux.", t); 
        }
    }

    /**
     * This should work, but server is in flux so it often breaks.
     *
     * @throws Throwable if trouble
     */
    public static void testNosSosATempAllStations(String datasetIdPrefix) throws Throwable {
        testVerboseOn();
        EDDTable eddTable;               
        String name, tName, results, expected, userDapQuery;

        try { 
            eddTable = (EDDTable)oneFromDatasetsXml(null, datasetIdPrefix + "nosSosATemp");  

            //it was hard to find data. station advertises 1999+ for several composites.
            //but searching January's for each year found no data until 2006
            String2.log("\n*** EDDTableFromSOS nos AirTemperature test get all stations .CSV data\n");
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&time>=2008-10-26T00&time<=2008-10-26T01&orderBy(\"station_id,time\")", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_nosSosATempAllStations", ".csv"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"longitude,latitude,station_id,altitude,time,sensor_id,air_temperature,quality_flags\n" +
"degrees_east,degrees_north,,m,UTC,,degree_C,\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.9,0;0;0\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8,0;0;0\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.7,0;0;0\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8,0;0;0\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.7,0;0;0\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8,0;0;0\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.7,0;0;0\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8,0;0;0\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8,0;0;0\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.7,0;0;0\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8,0;0;0\n" +
"-157.79,21.4331,urn:ioos:station:NOAA.NOS.CO-OPS:1612480,NaN,2008-10-26T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612480:D1,22.9,0;0;0\n" +
"-157.79,21.4331,urn:ioos:station:NOAA.NOS.CO-OPS:1612480,NaN,2008-10-26T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612480:D1,22.8,0;0;0\n" +
"-157.79,21.4331,urn:ioos:station:NOAA.NOS.CO-OPS:1612480,NaN,2008-10-26T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612480:D1,23.0,0;0;0\n" +
"-157.79,21.4331,urn:ioos:station:NOAA.NOS.CO-OPS:1612480,NaN,2008-10-26T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612480:D1,22.9,0;0;0\n" +
"-157.79,21.4331,urn:ioos:station:NOAA.NOS.CO-OPS:1612480,NaN,2008-10-26T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612480:D1,23.0,0;0;0\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);

        } catch (Throwable t) {
            throw new RuntimeException("Unexpected error " + datasetIdPrefix + "nosSosATemp. NOS SOS Server is in flux.", t); 
        }
    }

    /**
     * This should work, but server is in flux so it often breaks.
     *
     * @throws Throwable if trouble
     */
    public static void testNosSosATempStationList(String datasetIdPrefix) throws Throwable {
        testVerboseOn();
        EDDTable eddTable;               
        String name, tName, results, expected, userDapQuery;

        try { 
            eddTable = (EDDTable)oneFromDatasetsXml(null, datasetIdPrefix + "nosSosATemp");  

            String2.log("\n*** EDDTableFromSOS nos AirTemperature test get all stations .CSV data\n");
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "longitude,latitude,station_id&orderBy(\"station_id\")", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_nosSosATempStationList", ".csv"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"longitude,latitude,station_id\n" +
"degrees_east,degrees_north,\n" +
//"122.6003,37.7501,urn:ioos:station:NOAA.NOS.CO-OPS:1600012\n" + //disappeared 2012-08-17
"-159.3561,21.9544,urn:ioos:station:NOAA.NOS.CO-OPS:1611400\n" + //gone 2014-08-12, returned 2015-02-19
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340\n" +
"-157.79,21.4331,urn:ioos:station:NOAA.NOS.CO-OPS:1612480\n" +
"-156.4767,20.895,urn:ioos:station:NOAA.NOS.CO-OPS:1615680\n" +
"-155.8294,20.0366,urn:ioos:station:NOAA.NOS.CO-OPS:1617433\n";
//...
            Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);

        } catch (Throwable t) {
            throw new RuntimeException("Unexpected error " + datasetIdPrefix + "nosSosATemp. NOS SOS Server is in flux.", t); 
        }
    }

    /**
     * This should work, but server is in flux so it often breaks.
     *
     * @throws Throwable if trouble
     */
    public static void testNosSosBPres(String datasetIdPrefix) throws Throwable {
        testVerboseOn();
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, datasetIdPrefix + "nosSosBPres");  
        String name, tName, results, expected, userDapQuery;

        try { 

            //it was hard to find data. station advertises 1999+ for several composites.
            //but searching January's for each year found no data until 2006
            String2.log("\n*** EDDTableFromSOS nos Pressure test get one station .CSV data\n");
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:8419317\"&time>=2006-01-01T00&time<=2006-01-01T01", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_nosSosPressure", ".csv"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"longitude,latitude,station_id,altitude,time,sensor_id,air_pressure,quality_flags\n" +
"degrees_east,degrees_north,,m,UTC,,millibars,\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:F1,1010.7,0;0;0\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:F1,1010.8,0;0;0\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:F1,1010.7,0;0;0\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:F1,1010.7,0;0;0\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:F1,1010.7,0;0;0\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:F1,1010.9,0;0;0\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:F1,1011.1,0;0;0\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:F1,1011.4,0;0;0\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:F1,1011.4,0;0;0\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:F1,1011.4,0;0;0\n";
            Test.ensureEqual(results, expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NOS Server is in flux.", t); 
        }
            

        try {
            String2.log("\n*** EDDTableFromSOS nos Pressure test get one station .CSV data\n");
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:9491094\"&time>=2008-09-01T00&time<=2008-09-01T01", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_nosSosPressure", ".csv"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"longitude,latitude,station_id,altitude,time,sensor_id,air_pressure,quality_flags\n" +
"degrees_east,degrees_north,,m,UTC,,millibars,\n" +
"-164.0644,67.5758,urn:ioos:station:NOAA.NOS.CO-OPS:9491094,NaN,2008-09-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9491094:F1,1011.3,0;0;0\n" +
"-164.0644,67.5758,urn:ioos:station:NOAA.NOS.CO-OPS:9491094,NaN,2008-09-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9491094:F1,1011.2,0;0;0\n" +
"-164.0644,67.5758,urn:ioos:station:NOAA.NOS.CO-OPS:9491094,NaN,2008-09-01T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9491094:F1,1011.2,0;0;0\n" +
"-164.0644,67.5758,urn:ioos:station:NOAA.NOS.CO-OPS:9491094,NaN,2008-09-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9491094:F1,1011.1,0;0;0\n" +
"-164.0644,67.5758,urn:ioos:station:NOAA.NOS.CO-OPS:9491094,NaN,2008-09-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9491094:F1,1011.1,0;0;0\n" +
"-164.0644,67.5758,urn:ioos:station:NOAA.NOS.CO-OPS:9491094,NaN,2008-09-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9491094:F1,1011.1,0;0;0\n" +
"-164.0644,67.5758,urn:ioos:station:NOAA.NOS.CO-OPS:9491094,NaN,2008-09-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9491094:F1,1011.0,0;0;0\n" +
"-164.0644,67.5758,urn:ioos:station:NOAA.NOS.CO-OPS:9491094,NaN,2008-09-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9491094:F1,1010.9,0;0;0\n" +
"-164.0644,67.5758,urn:ioos:station:NOAA.NOS.CO-OPS:9491094,NaN,2008-09-01T00:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9491094:F1,1010.9,0;0;0\n" +
"-164.0644,67.5758,urn:ioos:station:NOAA.NOS.CO-OPS:9491094,NaN,2008-09-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9491094:F1,1010.9,0;0;0\n" +
"-164.0644,67.5758,urn:ioos:station:NOAA.NOS.CO-OPS:9491094,NaN,2008-09-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9491094:F1,1010.8,0;0;0\n";
            Test.ensureEqual(results, expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NOS Server is in flux.", t); 
        }

        try {
            String2.log("\n*** EDDTableFromSOS nos Pressure .das\n");
            String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
            tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                eddTable.className() + "_nosSosPressure", ".das"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"Attributes {\n" +
" s {\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -177.36, 167.7361;\n" + //2015-12-10 moved a little
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range -14.2767, 70.4114;\n" +  //2015-12-10 moved a little, again, pre 2011-04-15  min was -14.28! then 8.7316, then back
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  station_id {\n" +
"    String cf_role \"timeseries_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station ID\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    String axis \"Z\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range -3.6757152e+9, NaN;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  sensor_id {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Sensor ID\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/air_pressure\";\n" +
"  }\n" +
"  air_pressure {\n" +
"    Float64 colorBarMaximum 1050.0;\n" +
"    Float64 colorBarMinimum 950.0;\n" +
"    String ioos_category \"Pressure\";\n" +
"    String long_name \"Barometric Pressure\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/air_pressure\";\n" +
"    String standard_name \"air_pressure\";\n" +
"    String units \"millibars\";\n" +
"  }\n" +
"  quality_flags {\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Quality Flags\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/air_pressure\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"TimeSeries\";\n" +
"    String cdm_timeseries_variables \"station_id, longitude, latitude, sensor_id\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    Float64 Easternmost_Easting 167.7361;\n" +
"    String featureType \"TimeSeries\";\n" +
"    Float64 geospatial_lat_max 70.4114;\n" +
"    Float64 geospatial_lat_min -14.2767;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 167.7361;\n" +
"    Float64 geospatial_lon_min -177.36;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"" + today;
//+ " https://opendap.co-ops.nos.noaa.gov/ioos-dif-sos"; //-test
            Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Unexpected error " + datasetIdPrefix + "nosSosPressure. NOS SOS Server is in flux.", t); 
        }
    }

    /**
     * This should work, but server is in flux so it often breaks.
     *
     * @throws Throwable if trouble
     */
    public static void testNosSosCond(String datasetIdPrefix) throws Throwable {
        testVerboseOn();
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, datasetIdPrefix + "nosSosCond");  
        String name, tName, results, expected, userDapQuery;

        try { 

            //it was hard to find data. station advertises 1999+ for several composites.
            //but searching January's for each year found no data until 2006
            //2012-03-16 I used station=...8419317 for years, now station=...8419317 not found.

            //Use this to find a valid station and time range
            //testFindValidStation(eddTable, testNosStations,
            //    "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:",
            //    "\"&time>=2013-09-01T00:03&time<=2013-09-01T01");

            String2.log("\n*** EDDTableFromSOS nos Conductivity test get one station .CSV data\n");
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:8452660\"&time>=2013-09-01T00&time<=2013-09-01T01", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_nosSosCond", ".csv"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"longitude,latitude,station_id,altitude,time,sensor_id,conductivity,quality_flags\n" +
"degrees_east,degrees_north,,m,UTC,,mS cm-1,\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,42.6,0;0;0\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,42.5,0;0;0\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,42.6,0;0;0\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,42.6,0;0;0\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,42.6,0;0;0\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,42.6,0;0;0\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,42.6,0;0;0\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,42.6,0;0;0\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,42.6,0;0;0\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,42.7,0;0;0\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,42.6,0;0;0\n";
Test.ensureEqual(results, expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NOS Server is in flux.", t); 
        }
            
        try {
            String2.log("\n*** EDDTableFromSOS nos Conductivity .das\n");
            String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
            tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                eddTable.className() + "_nosSosCond", ".das"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"Attributes {\n" +
" s {\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -145.755, -71.1641;\n" + //changes sometimes
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range 29.3675, 60.5583;\n" + //changes sometimes
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  station_id {\n" +
"    String cf_role \"timeseries_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station ID\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    String axis \"Z\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range -2.1302784e+9, NaN;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  sensor_id {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Sensor ID\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/sea_water_electrical_conductivity\";\n" +
"  }\n" +
"  conductivity {\n" +
"    String ioos_category \"Salinity\";\n" +
"    String long_name \"Electrical Conductivity\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/sea_water_electrical_conductivity\";\n" +
"    String standard_name \"sea_water_electrical_conductivity\";\n" +
"    String units \"mS cm-1\";\n" +
"  }\n" +
"  quality_flags {\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Quality Flags\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/sea_water_electrical_conductivity\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"TimeSeries\";\n" +
"    String cdm_timeseries_variables \"station_id, longitude, latitude, sensor_id\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    Float64 Easternmost_Easting -71.1641;\n" + //changes sometimes
"    String featureType \"TimeSeries\";\n" +
"    Float64 geospatial_lat_max 60.5583;\n" + //changes sometimes
"    Float64 geospatial_lat_min 29.3675;\n" + //2013-05-11 was 29.48  then 29.6817, then...
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max -71.1641;\n" + //changes sometimes
"    Float64 geospatial_lon_min -145.755;\n" + //changes sometimes
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"" + today;
//+ " https://opendap.co-ops.nos.noaa.gov/ioos-dif-sos"; //-test
            Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Unexpected(?) error " + datasetIdPrefix + "nosSosCond. NOS SOS Server is in flux.", t); 
        }
    }

    /** Bob uses this to find a valid station and time period for unit tests. */
    static String testNosStations[] = {
        "g06010", "gl0101", "gl0201", "gl0301", "hb0101", "hb0201", "hb0301",
        "hb0401", "lc0101", "lc0201", "lc0301", "lm0101", "mb0101", "mb0301", 
        "mb0401", "n05010", "nb0201", "nb0301", "nl0101", "ps0201", "ps0301",
        "8447386", "8452660", "8454049", "8537121", "8574680", "8635750",
        "8637689", "8638610", "8638863", "8639348", "8737048", "8770613",
        "8771013", "9454050"};

    public static void testFindValidStation(EDDTable eddTable, String stations[],
            String preQuery, String postQuery) throws Exception {
        String2.log("\n*** EDDTableFromSOS.testFindValidStation");
        int i = -1; 
        while (++i < stations.length) {
            try {
                String tName = eddTable.makeNewFileForDapQuery(null, null, 
                    preQuery + stations[i] + postQuery, 
                    EDStatic.fullTestCacheDirectory, eddTable.className() + "_testFindValidStation", ".csv"); 
                String2.pressEnterToContinue("SUCCESS with station=" + stations[i]);
                break;
            } catch (Throwable t) {
                String2.log(MustBe.throwableToString(t));
            }
        }
        if (i >= stations.length)
            String2.pressEnterToContinue("FAILED to find a valid station.");
    }

    /**
     * This should work, but server is in flux so it often breaks.
     *
     * @throws Throwable if trouble
     */
    public static void testNosSosCurrents(String datasetIdPrefix) throws Throwable {
        testVerboseOn();
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, datasetIdPrefix + "nosSosCurrents");  
        String name, tName = "", results, expected, userDapQuery;
 
        try { 

            //worked until Feb 2013:  (then: no matching station)
            //That means a request for historical data stopped working!
            //  "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:g07010\"" +
            //  "&time>=2012-02-01T00:03&time<=2012-02-01T00:03", //It was hard to find a request that had data

            //Use this to find a valid station and time range
            //testFindValidStation(eddTable, testNosStations,
            //    "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:",
            //    "\"&time>=2013-09-01T00:03&time<=2013-09-01T00:03");

            String2.log("\n*** EDDTableFromSOS.testNosSosCurrents test get one station .CSV data\n");

            
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:lc0301\"" +
                "&time>=2013-09-01T00:03&time<=2013-09-01T00:03", //It was hard to find a request that had data
                 EDStatic.fullTestCacheDirectory, eddTable.className() + "_nosSosCurrents", ".csv"); 
                
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"longitude,latitude,station_id,altitude,time,sensor_id,sensor_depth,direction_of_sea_water_velocity,sea_water_speed,platform_orientation,platform_pitch_angle,platform_roll_angle,sea_water_temperature,orientation,sampling_rate,reporting_interval,processing_level,bin_size,first_bin_center,number_of_bins,bin\n" +
"degrees_east,degrees_north,,m,UTC,,m,degrees_true,cm s-1,degrees_true,degrees,degrees,degree_C,,Hz,s,,m,m,,count\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-5.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,283.0,7.6,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,1\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-9.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,288.0,13.0,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,2\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-13.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,301.0,10.0,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,3\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-17.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,283.0,12.4,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,4\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-21.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,291.0,13.9,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,5\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-25.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,284.0,14.8,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,6\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-29.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,293.0,16.2,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,7\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-33.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,276.0,11.5,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,8\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-37.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,282.0,9.0,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,9\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-41.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,285.0,7.6,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,10\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-45.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,293.0,10.4,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,11\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-49.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,299.0,10.8,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,12\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-53.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,296.0,12.5,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,13\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-57.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,287.0,11.6,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,14\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-61.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,298.0,8.7,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,15\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-65.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,296.0,14.9,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,16\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-69.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,302.0,8.4,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,17\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-73.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,289.0,14.3,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,18\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-77.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,290.0,11.5,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,19\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-81.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,293.0,9.6,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,20\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-85.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,300.0,8.6,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,21\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-89.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,298.0,9.4,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,22\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-93.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,300.0,10.4,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,23\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-97.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,292.0,11.1,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,24\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-101.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,282.0,10.4,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,25\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-105.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,282.0,9.5,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,26\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-109.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,290.0,10.5,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,27\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-113.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,289.0,8.5,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,28\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-117.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,289.0,6.4,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,29\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-121.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,297.0,8.4,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,30\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-125.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,295.0,10.8,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,31\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-129.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,282.0,8.9,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,32\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-133.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,290.0,7.4,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,33\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-137.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,288.0,11.8,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,34\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-141.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,302.0,6.7,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,35\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-145.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,336.0,3.6,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,36\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-149.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,290.0,5.7,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,37\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-153.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,311.0,3.7,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,38\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-157.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,171.0,1.7,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,39\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-161.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,133.0,1.7,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,40\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-165.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,269.0,2.8,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,41\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-169.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,64.0,3.1,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,42\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-173.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,277.0,1.2,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,43\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-177.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,310.0,1.5,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,44\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-181.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,313.0,0.6,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,45\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-185.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,102.0,1.8,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,46\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-189.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,302.0,3.7,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,47\n" +
"-93.2494,30.2178,urn:ioos:station:NOAA.NOS.CO-OPS:lc0301,-193.5,2013-09-01T00:03:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:lc0301:SONTEK-ADP-808,3,80.0,3.9,100.0,0.8,0.2,30.65,sidewaysLooking,0.0,360,RAW,4.0,5.5,48,48\n";
Test.ensureEqual(results, expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NOS Server is in flux.", t); 
        }
            
        try {
            String2.log("\n*** EDDTableFromSOS.testNosSosCurrents .das\n");
            String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
            tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                eddTable.className() + "_nosSosCurrents", ".das"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"Attributes {\n" +
" s {\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -167.9007, -66.9956;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range 19.6351, 61.2782;\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  station_id {\n" +
"    String cf_role \"timeseries_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station ID\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    String axis \"Z\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 8.63001e+8, NaN;\n" +
"    String axis \"T\";\n" +
"    String cf_role \"profile_id\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  sensor_id {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Sensor ID\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"  }\n" +
"  sensor_depth {\n" +
"    Float64 colorBarMaximum 100.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Currents\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  direction_of_sea_water_velocity {\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Currents\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String standard_name \"direction_of_sea_water_velocity\";\n" +
"    String units \"degrees_true\";\n" +
"  }\n" +
"  sea_water_speed {\n" +
"    Float64 colorBarMaximum 50.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Currents\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String standard_name \"sea_water_speed\";\n" +
"    String units \"cm s-1\";\n" +
"  }\n" +
"  platform_orientation {\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Currents\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String units \"degrees_true\";\n" +
"  }\n" +
"  platform_pitch_angle {\n" +
"    Float64 colorBarMaximum 50.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Currents\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String units \"degrees\";\n" +
"  }\n" +
"  platform_roll_angle {\n" +
"    Float64 colorBarMaximum 50.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Currents\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String units \"degrees\";\n" +
"  }\n" +
"  sea_water_temperature {\n" +
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Currents\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String standard_name \"sea_water_temperature\";\n" +
"    String units \"degree_C\";\n" +
"  }\n" +
"  orientation {\n" +
"    String ioos_category \"Currents\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"  }\n" +
"  sampling_rate {\n" +
"    Float64 colorBarMaximum 10000.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Currents\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String units \"Hz\";\n" +
"  }\n" +
"  reporting_interval {\n" +
"    Float64 colorBarMaximum 3600.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Currents\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String units \"s\";\n" +
"  }\n" +
"  processing_level {\n" +
"    String ioos_category \"Currents\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"  }\n" +
"  bin_size {\n" +
"    Float64 colorBarMaximum 50.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Currents\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  first_bin_center {\n" +
"    Float64 colorBarMaximum 50.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Currents\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  number_of_bins {\n" +
"    Float64 colorBarMaximum 100.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Currents\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"  }\n" +
"  bin {\n" +
"    Float64 colorBarMaximum 100.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Currents\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String units \"count\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"TimeSeriesProfile\";\n" +
"    String cdm_profile_variables \"time\";\n" +
"    String cdm_timeseries_variables \"station_id, longitude, latitude, sensor_id\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    Float64 Easternmost_Easting -66.9956;\n" +
"    String featureType \"TimeSeriesProfile\";\n" +
"    Float64 geospatial_lat_max 61.2782;\n" +
"    Float64 geospatial_lat_min 19.6351;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max -66.9956;\n" +
"    Float64 geospatial_lon_min -167.9007;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"" + today;
//+ " https://opendap.co-ops.nos.noaa.gov/ioos-dif-sos"; //-test
            Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
/*
https://opendap.co-ops.nos.noaa.gov/ioos-dif-sos/SOS?service=SOS&request=GetCapabilities
<sos:ObservationOffering gml:id="network_currentsactive">
<gml:description>
All CurrentsActive stations on NOAA.NOS.CO-OPS SOS server
</gml:description>
<gml:name>urn:ioos:network:noaa.nos.co-ops:currentsactive</gml:name>
<gml:boundedBy>
<gml:Envelope srsName="http://www.opengis.net/def/crs/EPSG/0/4326">
<gml:lowerCorner>27.6250 -124.2498</gml:lowerCorner>
<gml:upperCorner>48.8628 -71.1784</gml:upperCorner>
</gml:Envelope>
</gml:boundedBy>
<sos:time>
<gml:TimePeriod>
<gml:beginPosition>2011-03-30T01:00:00Z</gml:beginPosition>
<gml:endPosition indeterminatePosition="now"/>
</gml:TimePeriod>
</sos:time>
<sos:procedure xlink:href="urn:ioos:network:NOAA.NOS.CO-OPS:CurrentsActive"/>
<sos:observedProperty xlink:href="http://mmisw.org/ont/cf/parameter/direction_of_sea_water_velocity"/>
<sos:observedProperty xlink:href="http://mmisw.org/ont/cf/parameter/sea_water_speed"/>
<sos:featureOfInterest xlink:href="urn:cgi:feature:CGI:EarthOcean"/>
<sos:responseFormat>text/csv</sos:responseFormat>
<sos:responseFormat>text/tab-separated-values</sos:responseFormat>
<sos:responseFormat>application/vnd.google-earth.kml+xml</sos:responseFormat>
<sos:responseFormat>text/xml;subtype="om/1.0.0/profiles/ioos_sos/1.0"</sos:responseFormat>
<sos:resultModel>om:ObservationCollection</sos:resultModel>
<sos:responseMode>inline</sos:responseMode>
</sos:ObservationOffering>
 
<sos:ObservationOffering gml:id="network_currentssurvey">
<gml:description>
All CurrentsSurvey stations on NOAA.NOS.CO-OPS SOS server
</gml:description>
<gml:name>urn:ioos:network:noaa.nos.co-ops:currentssurvey</gml:name>
<gml:boundedBy>
<gml:Envelope srsName="http://www.opengis.net/def/crs/EPSG/0/4326">
<gml:lowerCorner>19.6351 -167.9007</gml:lowerCorner>
<gml:upperCorner>61.2782 -66.9956</gml:upperCorner>
</gml:Envelope>
</gml:boundedBy>
<sos:time>
<gml:TimePeriod>
<gml:beginPosition>1997-05-07T10:30:00Z</gml:beginPosition>
<gml:endPosition>2015-09-13T20:56:00Z</gml:endPosition>
</gml:TimePeriod>
</sos:time>
<sos:procedure xlink:href="urn:ioos:network:NOAA.NOS.CO-OPS:CurrentsSurvey"/>
<sos:observedProperty xlink:href="http://mmisw.org/ont/cf/parameter/direction_of_sea_water_velocity"/>
<sos:observedProperty xlink:href="http://mmisw.org/ont/cf/parameter/sea_water_speed"/>
<sos:featureOfInterest xlink:href="urn:cgi:feature:CGI:EarthOcean"/>
<sos:responseFormat>text/csv</sos:responseFormat>
<sos:responseFormat>text/tab-separated-values</sos:responseFormat>
<sos:responseFormat>application/vnd.google-earth.kml+xml</sos:responseFormat>
<sos:responseFormat>text/xml;subtype="om/1.0.0/profiles/ioos_sos/1.0"</sos:responseFormat>
<sos:resultModel>om:ObservationCollection</sos:resultModel>
<sos:responseMode>inline</sos:responseMode>
</sos:ObservationOffering>

  ERROR while processing response from requestUrl=https://opendap.co-ops.nos.noaa.gov/ioos-dif-sos/SOS?service=S
OS&version=1.0.0&request=GetObservation&offering=urn:ioos:station:NOAA.NOS.CO-OPS:lc0301&observedProperty=http:
//mmisw.org/ont/cf/parameter/direction_of_sea_water_velocity&responseFormat=text%2Fcsv&eventTime=2013-09-01T00:
03:00Z
java.lang.RuntimeException: Source Exception="InvalidParameterValue: observedProperty: Valid observedProperty v
alues: air_temperature, air_pressure, sea_water_electrical_conductivity, currents, sea_water_salinity, water_su
rface_height_above_reference_datum, sea_surface_height_amplitude_due_to_equilibrium_ocean_tide, sea_water_tempe
rature, winds, harmonic_constituents, datums, relative_humidity, rain_fall, visibility".
*/
            throw new RuntimeException( 
                "This started failing ~Feb 2013. Then okay.\n" +
                "2015-12-10 failing: Was observedProperty=currents\n" +
                "  now GetCapabilities says direction_of_sea_water_velocity, sea_water_speed\n" +
                "  but using those in datasets.xml says it must be one of ... currents\n" +
                "  Ah! I see there are 2 group procedures (not separate stations)\n" +
                "  but ERDDAP doesn't support such groups right now.",
                t); 
//FUTURE: add support for this new approach.    Changes are endless!!!!
        }
    }

    /**
     * This should work, but server is in flux so it often breaks.
     *
     * @throws Throwable if trouble
     */
    public static void testNosSosSalinity(String datasetIdPrefix) throws Throwable {
        testVerboseOn();
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, datasetIdPrefix + "nosSosSalinity");  
        String name, tName, results, expected, userDapQuery;

        try { 

            String2.log("\n*** EDDTableFromSOS.testNosSosSalinity .das\n");
            tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                eddTable.className() + "_nosSosSalinity", ".das"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"Attributes {\n" +
" s {\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -145.755, -71.1641;\n" + //..., pre 2013-11-04 was -94.985, changed 2012-03-16
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range 29.3675, 60.5583;\n" + //..., pre 2013-11-04 was 41.7043, pre 2013-06-28 was 29.6817 pre 2013-05-22 was 29.48 ;pre 2012-03-16 was 43.32
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  station_id {\n" +
"    String cf_role \"timeseries_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station ID\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    String axis \"Z\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n";
//"    Float64 actual_range -2.2184928e+9, NaN;\n" + //-2.1302784e+9
            Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NOS Server is in flux.", t); 
        }

        try {
            //it was hard to find data. station advertises 1999+ for several composites.
            //but searching January's for each year found no data until 2006
//??? Does the error message need to be treated as simple no data?
//<ExceptionReport><Exception>atts=exceptionCode="NoApplicableCode", locator="8419317"
            //2012-03-16 station=...8419317 stopped working
            //2012-11-18 I switched to station 8447386
            //2013-11-04 used
            //testFindValidStation(eddTable, testNosStations,
            //    "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:",
            //    "\"&time>=2013-09-01T00:03&time<=2013-09-01T01");

            String2.log("\n*** EDDTableFromSOS.testNosSosSalinity test get one station .CSV data\n");
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:8452660\"&time>=2013-09-01T00&time<=2013-09-01T01", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_nosSosSalinity", ".csv"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
//station 8419317: salinity is low because this is in estuary/river, not in ocean
//"longitude,latitude,station_id,altitude,time,sensor_id,sea_water_salinity\n" +
//"degrees_east,degrees_north,,m,UTC,,PSU\n" +
//"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,2.283\n" +
//"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,2.286\n" +
//"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,2.295\n" +
//"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,2.286\n" +
//"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,2.286\n" +
//"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,2.286\n" +
//"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,2.283\n" +
//"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,2.267\n" +
//"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,2.448\n" +
//"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,2.486\n";
//no it is because data was off by a factor of 10!!!
/*starting 2011-12-16:
"longitude,latitude,station_id,altitude,time,sensor_id,sea_water_salinity\n" +
"degrees_east,degrees_north,,m,UTC,,PSU\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,27.075\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,27.192\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,27.226\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,27.192\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,27.192\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,27.192\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,27.075\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,26.791\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,28.319\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,28.692\n";
//No! Wait!  after lunch 2011-12-16 the results became:
"longitude,latitude,station_id,altitude,time,sensor_id,sea_water_salinity\n" +
"degrees_east,degrees_north,,m,UTC,,PSU\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,27.158\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,27.192\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,27.31\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,27.192\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,27.192\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,27.192\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,27.158\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,26.957\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,29.2\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:SALINITY,29.675\n";    
retired 2013-11-04:
"longitude,latitude,station_id,altitude,time,sensor_id,sea_water_salinity\n" +
"degrees_east,degrees_north,,m,UTC,,PSU\n" +
"-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2012-01-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:SALINITY,26.718\n" +
"-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2012-01-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:SALINITY,26.611\n" +
"-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2012-01-01T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:SALINITY,26.718\n" +
"-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2012-01-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:SALINITY,26.718\n" +
"-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2012-01-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:SALINITY,26.747\n" +
"-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2012-01-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:SALINITY,26.825\n" +
"-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2012-01-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:SALINITY,26.854\n" +
"-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2012-01-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:SALINITY,26.854\n" +
"-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2012-01-01T00:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:SALINITY,26.747\n" +
"-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2012-01-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:SALINITY,26.854\n" +
"-71.1641,41.7043,urn:ioos:station:NOAA.NOS.CO-OPS:8447386,NaN,2012-01-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8447386:SALINITY,26.854\n";
2013-11-04 forced to switch stations 
"longitude,latitude,station_id,altitude,time,sensor_id,sea_water_salinity\n" +
"degrees_east,degrees_north,,m,UTC,,PSU\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.692\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.614\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.692\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.692\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.692\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.692\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.692\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.692\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.692\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.77\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.692\n";
2014-01-24 results modified a little 
"longitude,latitude,station_id,altitude,time,sensor_id,sea_water_salinity\n" +
"degrees_east,degrees_north,,m,UTC,,PSU\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.677\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.608\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.689\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.682\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.713\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.695\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.67\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.704\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.748\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.68\n";
//2015-12-10: Historical data keeps changing. It's absurd!   :12 is back, precision changed. */
"longitude,latitude,station_id,altitude,time,sensor_id,sea_water_salinity\n" +
"degrees_east,degrees_north,,m,UTC,,PSU\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.7\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.62\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.7\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.7\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.7\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.7\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.7\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.7\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.7\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.77\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:SALINITY,29.7\n";

//2015-12-28 sensor_id name changed to G1, 
//but 2016-01-19 it changed back
/*"longitude,latitude,station_id,altitude,time,sensor_id,sea_water_salinity\n" +
"degrees_east,degrees_north,,m,UTC,,PSU\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,29.7\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,29.62\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,29.7\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,29.7\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,29.7\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,29.7\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,29.7\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,29.7\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,29.7\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,29.77\n" +
"-71.3267,41.505,urn:ioos:station:NOAA.NOS.CO-OPS:8452660,NaN,2013-09-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8452660:G1,29.7\n";
*/
Test.ensureEqual(results, expected, "RESULTS=\n" + results);
            

        } catch (Throwable t) {
            throw new RuntimeException("Unexpected(?) error " + datasetIdPrefix + "nosSosSalinity." +
                "\nNOS SOS Server is in flux.", t); 
        }
    }

    /**
     * This should work, but server is in flux so it often breaks.
     *
     * @throws Throwable if trouble
     */
    public static void testNosSosWind(String datasetIdPrefix) throws Throwable {
        testVerboseOn();
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, datasetIdPrefix + "nosSosWind");  
        String name, tName, results, expected, userDapQuery;

        try { 

            // stopped working ~12/15/2008
            //    "&station_id=\"urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8419317\"&time>=2006-01-01T00&time<=2006-01-01T01", 
            //stopped working ~2009-03-26, and no recent data
            //    "&station_id=\"urn:x-noaa:def:station:NOAA.NOS.CO-OPS::9461380\"&time>=2008-09-01T00&time<=2008-09-01T01", 

            String2.log("\n*** EDDTableFromSOS.testNosSosWind test get one station .CSV data\n");
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:9468756\"&time>=2009-04-06T00&time<=2009-04-06T01", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_nosSosWind", ".csv"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"longitude,latitude,station_id,altitude,time,sensor_id,wind_from_direction,wind_speed,wind_speed_of_gust\n" +
"degrees_east,degrees_north,,m,UTC,,degrees_true,m s-1,m s-1\n" +
"-165.43,64.5,urn:ioos:station:NOAA.NOS.CO-OPS:9468756,NaN,2009-04-06T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9468756:C1,79.0,7.8,9.5\n" +
"-165.43,64.5,urn:ioos:station:NOAA.NOS.CO-OPS:9468756,NaN,2009-04-06T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9468756:C1,81.0,7.4,9.5\n" +
"-165.43,64.5,urn:ioos:station:NOAA.NOS.CO-OPS:9468756,NaN,2009-04-06T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9468756:C1,80.0,7.8,9.5\n" +
"-165.43,64.5,urn:ioos:station:NOAA.NOS.CO-OPS:9468756,NaN,2009-04-06T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9468756:C1,79.0,8.1,10.1\n" +
"-165.43,64.5,urn:ioos:station:NOAA.NOS.CO-OPS:9468756,NaN,2009-04-06T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9468756:C1,79.0,7.3,9.0\n" +
"-165.43,64.5,urn:ioos:station:NOAA.NOS.CO-OPS:9468756,NaN,2009-04-06T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9468756:C1,76.0,7.6,8.9\n" +
"-165.43,64.5,urn:ioos:station:NOAA.NOS.CO-OPS:9468756,NaN,2009-04-06T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9468756:C1,82.0,7.2,8.5\n" +
"-165.43,64.5,urn:ioos:station:NOAA.NOS.CO-OPS:9468756,NaN,2009-04-06T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9468756:C1,79.0,8.1,9.2\n" +
"-165.43,64.5,urn:ioos:station:NOAA.NOS.CO-OPS:9468756,NaN,2009-04-06T00:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9468756:C1,80.0,7.3,9.4\n" +
"-165.43,64.5,urn:ioos:station:NOAA.NOS.CO-OPS:9468756,NaN,2009-04-06T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9468756:C1,81.0,8.0,9.4\n" +
"-165.43,64.5,urn:ioos:station:NOAA.NOS.CO-OPS:9468756,NaN,2009-04-06T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9468756:C1,80.0,7.9,10.3\n";
            Test.ensureEqual(results, expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NOS Server is in flux.", t); 
        }

        try {
            String2.log("\n*** EDDTableFromSOS.testNosSosWind .das\n");
            String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
            tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                eddTable.className() + "_nosSosWind", ".das"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"Attributes {\n" +
" s {\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -176.632, 166.618;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range -14.28, 70.4;\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  station_id {\n" +
"    String cf_role \"timeseries_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station ID\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    String axis \"Z\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range -3.6757152e+9, NaN;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  sensor_id {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Sensor ID\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/winds\";\n" +
"  }\n" +
"  wind_from_direction {\n" +
"    Float64 colorBarMaximum 3600.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Wind\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/winds\";\n" +
"    String standard_name \"wind_from_direction\";\n" +
"    String units \"degrees_true\";\n" +
"  }\n" +
"  wind_speed {\n" +
"    Float64 colorBarMaximum 1500.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Wind\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/winds\";\n" +
"    String standard_name \"wind_speed\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  wind_speed_of_gust {\n" +
"    Float64 colorBarMaximum 300.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Wind\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/winds\";\n" +
"    String standard_name \"wind_speed_of_gust\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"TimeSeries\";\n" +
"    String cdm_timeseries_variables \"station_id, longitude, latitude, sensor_id\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    Float64 Easternmost_Easting 166.618;\n" +
"    String featureType \"TimeSeries\";\n" +
"    Float64 geospatial_lat_max 70.4;\n" +
"    Float64 geospatial_lat_min -14.28;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 166.618;\n" +
"    Float64 geospatial_lon_min -176.632;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"" + today;
//+ " https://opendap.co-ops.nos.noaa.gov/ioos-dif-sos"; //-test
            Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Unexpected error " + datasetIdPrefix + "nosSosWind." +
                "\nNOS SOS Server is in flux.", t); 
        }
    }

    /**
     * This should work, but server is in flux so it often breaks.
     *
     * @throws Throwable if trouble
     */
    public static void testNosSosWLevel(String datasetIdPrefix) throws Throwable {
        testVerboseOn();
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, datasetIdPrefix + "nosSosWLevel");  
        String name, tName, results, expected, userDapQuery;

        try { 

            //2013-11-04 used to find a station with data:
            //testFindValidStation(eddTable, testNosStations,
            //    "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:",
            //    "\"&time>=2013-09-01T00:03&time<=2013-09-01T01");

            String2.log("\n*** EDDTableFromSOS.testNosSosWLevel .das\n");
            String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
            tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                eddTable.className() + "_nosSosWLevel", ".das"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"Attributes {\n" +
" s {\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -177.36, 167.7361;\n" + //changes
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range -14.2767, 70.4114;\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  station_id {\n" +
"    String cf_role \"timeseries_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station ID\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    String axis \"Z\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range -3.6757152e+9, NaN;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  sensor_id {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Sensor ID\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/water_surface_height_above_reference_datum\";\n" +
"  }\n" +
"  water_level {\n" +
"    Float64 colorBarMaximum 5.0;\n" +
"    Float64 colorBarMinimum -5.0;\n" +
"    String ioos_category \"Sea Level\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/water_surface_height_above_reference_datum\";\n" +
"    String standard_name \"water_surface_height_above_reference_datum\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  datum_id {\n" +
"    String ioos_category \"Sea Level\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/water_surface_height_above_reference_datum\";\n" +
"  }\n" +
"  vertical_position {\n" +
"    String ioos_category \"Sea Level\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/water_surface_height_above_reference_datum\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  sigma {\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Sigma\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/water_surface_height_above_reference_datum\";\n" +
"  }\n" +
"  quality_flags {\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Quality Flags\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/water_surface_height_above_reference_datum\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"TimeSeries\";\n" +
"    String cdm_timeseries_variables \"station_id, longitude, latitude, sensor_id\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    Float64 Easternmost_Easting 167.7361;\n" +
"    String featureType \"TimeSeries\";\n" +
"    Float64 geospatial_lat_max 70.4114;\n" +
"    Float64 geospatial_lat_min -14.2767;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 167.7361;\n" +
"    Float64 geospatial_lon_min -177.36;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"" + today;
//+ " https://opendap.co-ops.nos.noaa.gov/ioos-dif-sos"; //-test
            Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NOS Server is in flux.", t); 
        }


        try {            
            String2.log("\n*** EDDTableFromSOS.testNosSosWLevel test get one station .CSV data\n");
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:9454050\"&time>=2013-09-01T00&time<=2013-09-01T01", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_nosSosWLevel", ".csv"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
/*//pre 2009-11-15 and post 2009-12-13
"longitude,latitude,station_id,altitude,time,WaterLevel\n" +
"degrees_east,degrees_north,,m,UTC,m\n" +
"-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:00:00Z,75.014\n" +
"-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:06:00Z,75.014\n" +
"-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:12:00Z,75.018\n" +
"-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:18:00Z,75.015\n" +
"-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:24:00Z,75.012\n" +
"-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:30:00Z,75.012\n" +
"-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:36:00Z,75.016\n" +
"-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:42:00Z,75.018\n" +
"-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:48:00Z,75.02\n" +
"-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:54:00Z,75.019\n" +
"-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T15:00:00Z,75.017\n";
  */        
/*"longitude,latitude,station_id,altitude,time,WaterLevel\n" +
"degrees_east,degrees_north,,m,UTC,m\n" +
"-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:00:00Z,75.021\n" +
"-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:06:00Z,75.022\n" +
"-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:12:00Z,75.025\n" +
"-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:18:00Z,75.023\n" +
"-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:24:00Z,75.02\n" +
"-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:30:00Z,75.019\n" +
"-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:36:00Z,75.023\n" +
"-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:42:00Z,75.025\n" +
"-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:48:00Z,75.027\n" +
"-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T14:54:00Z,75.026\n" +
"-75.9345,44.3311,urn:x-noaa:def:station:NOAA.NOS.CO-OPS::8311062,-999.0,2008-08-01T15:00:00Z,75.024\n";

"longitude,latitude,station_id,altitude,time,sensor_id,water_level,datum_id,vertical_position\n" +
"degrees_east,degrees_north,,m,UTC,,m,,m\n" +
"-145.753,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.601,urn:ioos:def:datum:noaa::MLLW,1.916\n" +
"-145.753,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.584,urn:ioos:def:datum:noaa::MLLW,1.916\n" +
"-145.753,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.569,urn:ioos:def:datum:noaa::MLLW,1.916\n" +
"-145.753,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.558,urn:ioos:def:datum:noaa::MLLW,1.916\n" +
"-145.753,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.547,urn:ioos:def:datum:noaa::MLLW,1.916\n" +
"-145.753,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.534,urn:ioos:def:datum:noaa::MLLW,1.916\n" +
"-145.753,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.522,urn:ioos:def:datum:noaa::MLLW,1.916\n" +
"-145.753,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.509,urn:ioos:def:datum:noaa::MLLW,1.916\n" +
"-145.753,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.5,urn:ioos:def:datum:noaa::MLLW,1.916\n" +
"-145.753,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.495,urn:ioos:def:datum:noaa::MLLW,1.916\n" +
"-145.753,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.491,urn:ioos:def:datum:noaa::MLLW,1.916\n";
2015-12-11: */
"longitude,latitude,station_id,altitude,time,sensor_id,water_level,datum_id,vertical_position,sigma,quality_flags\n" +
"degrees_east,degrees_north,,m,UTC,,m,,m,,\n" +
"-145.755,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.601,urn:ioos:def:datum:noaa::MLLW,1.916,0.003,0;0;0;0\n" +
"-145.755,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.584,urn:ioos:def:datum:noaa::MLLW,1.916,0.002,0;0;0;0\n" +
"-145.755,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.569,urn:ioos:def:datum:noaa::MLLW,1.916,0.003,0;0;0;0\n" +
"-145.755,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.558,urn:ioos:def:datum:noaa::MLLW,1.916,0.006,0;0;0;0\n" +
"-145.755,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.547,urn:ioos:def:datum:noaa::MLLW,1.916,0.001,0;0;0;0\n" +
"-145.755,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.534,urn:ioos:def:datum:noaa::MLLW,1.916,0.002,0;0;0;0\n" +
"-145.755,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.522,urn:ioos:def:datum:noaa::MLLW,1.916,0.003,0;0;0;0\n" +
"-145.755,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.509,urn:ioos:def:datum:noaa::MLLW,1.916,0.002,0;0;0;0\n" +
"-145.755,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.5,urn:ioos:def:datum:noaa::MLLW,1.916,0.001,0;0;0;0\n" +
"-145.755,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.495,urn:ioos:def:datum:noaa::MLLW,1.916,0.001,0;0;0;0\n" +
"-145.755,60.5583,urn:ioos:station:NOAA.NOS.CO-OPS:9454050,NaN,2013-09-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9454050:A1,1.491,urn:ioos:def:datum:noaa::MLLW,1.916,0.001,0;0;0;0\n";
            Test.ensureEqual(results, expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NOS Server is in flux.", t); 
        }
    }

    /**
     * This should work, but server is in flux so it often breaks.
     *
     * @throws Throwable if trouble
     */
    public static void testNosSosWTemp(String datasetIdPrefix) throws Throwable {
        testVerboseOn();
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, datasetIdPrefix + "nosSosWTemp");  
        String name, tName, results, expected, userDapQuery;

        try { 

            String2.log("\n*** EDDTableFromSOS.testNosSosWTemp .das\n");
            String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
            tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                eddTable.className() + "_nosSosWTemp", ".das"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"Attributes {\n" +
" s {\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -177.36, 167.7361;\n" + //changes
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range -14.2767, 70.4114;\n" + //changes
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  station_id {\n" +
"    String cf_role \"timeseries_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station ID\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    String axis \"Z\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range -3.6757152e+9, NaN;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  sensor_id {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Sensor ID\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/sea_water_temperature\";\n" +
"  }\n" +
"  sea_water_temperature {\n" +
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/sea_water_temperature\";\n" +
"    String standard_name \"sea_water_temperature\";\n" +
"    String units \"degree_C\";\n" +
"  }\n" +
"  quality_flags {\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Quality Flags\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/sea_water_temperature\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"TimeSeries\";\n" +
"    String cdm_timeseries_variables \"station_id, longitude, latitude, sensor_id\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    Float64 Easternmost_Easting 167.7361;\n" +
"    String featureType \"TimeSeries\";\n" +
"    Float64 geospatial_lat_max 70.4114;\n" +
"    Float64 geospatial_lat_min -14.2767;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 167.7361;\n" +
"    Float64 geospatial_lon_min -177.36;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"" + today;
//+ " https://opendap.co-ops.nos.noaa.gov/ioos-dif-sos"; //-test
            Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NOS Server is in flux.", t); 
        }

        try {
            String2.log("\n*** EDDTableFromSOS.testNosSosWTemp test get one station .CSV data\n");
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:8311062\"&time>=2008-08-01T14:00&time<2008-08-01T15:00", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_nosSosWTemp", ".csv"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
/*
"longitude,latitude,station_id,altitude,time,sensor_id,sea_water_temperature\n" +
"degrees_east,degrees_north,,m,UTC,,degree_C\n" +
"-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.349\n" +
"-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.4\n" +
"-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.4\n" +
"-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.4\n" +
"-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.366\n" +
"-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.4\n" +
"-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.37\n" +
"-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.379\n" +
"-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.382\n" +
"-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.389\n";
2015-12-11 precision changed from 3 to 1 decimal digits: */
"longitude,latitude,station_id,altitude,time,sensor_id,sea_water_temperature,quality_flags\n" +
"degrees_east,degrees_north,,m,UTC,,degree_C,\n" +
"-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.3,0;0;0\n" +
"-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.4,0;0;0\n" +
"-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.4,0;0;0\n" +
"-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.4,0;0;0\n" +
"-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.4,0;0;0\n" +
"-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.4,0;0;0\n" +
"-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.4,0;0;0\n" +
"-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.4,0;0;0\n" +
"-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.4,0;0;0\n" +
"-75.9345,44.3311,urn:ioos:station:NOAA.NOS.CO-OPS:8311062,NaN,2008-08-01T14:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8311062:E1,22.4,0;0;0\n";
            Test.ensureEqual(results, expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NOS Server is in flux.", t); 
        }
            
        try {
            String2.log("\n*** EDDTableFromSOS.testNosSosWTemp test get one station .CSV data\n");
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:9432780\"&time>=2008-09-01T14:00&time<2008-09-01T15:00", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_nosSosWTemp", ".csv"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"longitude,latitude,station_id,altitude,time,sensor_id,sea_water_temperature,quality_flags\n" +
"degrees_east,degrees_north,,m,UTC,,degree_C,\n" +
"-124.322,43.345,urn:ioos:station:NOAA.NOS.CO-OPS:9432780,NaN,2008-09-01T14:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1,11.6,0;0;0\n" +
"-124.322,43.345,urn:ioos:station:NOAA.NOS.CO-OPS:9432780,NaN,2008-09-01T14:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1,11.6,0;0;0\n" +
"-124.322,43.345,urn:ioos:station:NOAA.NOS.CO-OPS:9432780,NaN,2008-09-01T14:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1,11.6,0;0;0\n" +
"-124.322,43.345,urn:ioos:station:NOAA.NOS.CO-OPS:9432780,NaN,2008-09-01T14:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1,11.7,0;0;0\n" +
"-124.322,43.345,urn:ioos:station:NOAA.NOS.CO-OPS:9432780,NaN,2008-09-01T14:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1,11.7,0;0;0\n" +
"-124.322,43.345,urn:ioos:station:NOAA.NOS.CO-OPS:9432780,NaN,2008-09-01T14:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1,11.6,0;0;0\n" +
"-124.322,43.345,urn:ioos:station:NOAA.NOS.CO-OPS:9432780,NaN,2008-09-01T14:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1,11.6,0;0;0\n" +
"-124.322,43.345,urn:ioos:station:NOAA.NOS.CO-OPS:9432780,NaN,2008-09-01T14:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1,11.6,0;0;0\n" +
"-124.322,43.345,urn:ioos:station:NOAA.NOS.CO-OPS:9432780,NaN,2008-09-01T14:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1,11.6,0;0;0\n" +
"-124.322,43.345,urn:ioos:station:NOAA.NOS.CO-OPS:9432780,NaN,2008-09-01T14:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1,11.6,0;0;0\n";
            Test.ensureEqual(results, expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NOS Server is in flux.", t); 
        }


        try {
            String2.log("\n*** EDDTableFromSOS.testNosSosWTemp test .geoJson lat, lon, alt\n");
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "altitude,longitude,latitude&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:9432780\"&time>=2008-09-01T14:00&time<2008-09-01T15:00", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_nosSosWTemp1", ".geoJson"); 
            results = String2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"{\n" +
"  \"type\": \"MultiPoint\",\n" +
"  \"coordinates\": [\n" +
"[-124.322, 43.345],\n" +
"[-124.322, 43.345],\n" +
"[-124.322, 43.345],\n" +
"[-124.322, 43.345],\n" +
"[-124.322, 43.345],\n" +
"[-124.322, 43.345],\n" +
"[-124.322, 43.345],\n" +
"[-124.322, 43.345],\n" +
"[-124.322, 43.345],\n" +
"[-124.322, 43.345]\n" +
"  ],\n" +
"  \"bbox\": [-124.322, 43.345, -124.322, 43.345]\n" +
"}\n";
            Test.ensureEqual(results, expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NOS Server is in flux.", t); 
        }
    
        try {
            String2.log("\n*** EDDTableFromSOS.testNosSosWTemp test .geoJson all vars\n");
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:9432780\"&time>=2008-09-01T14:00&time<2008-09-01T15:00", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_nosSosWTemp2", ".geoJson"); 
            results = String2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"{\n" +
"  \"type\": \"FeatureCollection\",\n" +
"  \"propertyNames\": [\"station_id\", \"time\", \"sensor_id\", \"sea_water_temperature\", \"quality_flags\"],\n" +
"  \"propertyUnits\": [null, \"UTC\", null, \"degree_C\", null],\n" +
"  \"features\": [\n" +
"{\"type\": \"Feature\",\n" +
"  \"geometry\": {\n" +
"    \"type\": \"Point\",\n" +
//2019-03-20 was "    \"coordinates\": [-124.322, 43.345, null] },\n" +
"    \"coordinates\": [-124.322, 43.345] },\n" +
"  \"properties\": {\n" +
"    \"station_id\": \"urn:ioos:station:NOAA.NOS.CO-OPS:9432780\",\n" +
"    \"time\": \"2008-09-01T14:00:00Z\",\n" +
"    \"sensor_id\": \"urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1\",\n" +
"    \"sea_water_temperature\": 11.6,\n" +
"    \"quality_flags\": \"0;0;0\" }\n" +
"},\n" +
"{\"type\": \"Feature\",\n" +
"  \"geometry\": {\n" +
"    \"type\": \"Point\",\n" +
"    \"coordinates\": [-124.322, 43.345] },\n" +
"  \"properties\": {\n" +
"    \"station_id\": \"urn:ioos:station:NOAA.NOS.CO-OPS:9432780\",\n" +
"    \"time\": \"2008-09-01T14:06:00Z\",\n" +
"    \"sensor_id\": \"urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1\",\n" +
"    \"sea_water_temperature\": 11.6,\n" +
"    \"quality_flags\": \"0;0;0\" }\n" +
"},\n" +
"{\"type\": \"Feature\",\n" +
"  \"geometry\": {\n" +
"    \"type\": \"Point\",\n" +
"    \"coordinates\": [-124.322, 43.345] },\n" +
"  \"properties\": {\n" +
"    \"station_id\": \"urn:ioos:station:NOAA.NOS.CO-OPS:9432780\",\n" +
"    \"time\": \"2008-09-01T14:12:00Z\",\n" +
"    \"sensor_id\": \"urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1\",\n" +
"    \"sea_water_temperature\": 11.6,\n" +
"    \"quality_flags\": \"0;0;0\" }\n" +
"},\n" +
"{\"type\": \"Feature\",\n" +
"  \"geometry\": {\n" +
"    \"type\": \"Point\",\n" +
"    \"coordinates\": [-124.322, 43.345] },\n" +
"  \"properties\": {\n" +
"    \"station_id\": \"urn:ioos:station:NOAA.NOS.CO-OPS:9432780\",\n" +
"    \"time\": \"2008-09-01T14:18:00Z\",\n" +
"    \"sensor_id\": \"urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1\",\n" +
"    \"sea_water_temperature\": 11.7,\n" +
"    \"quality_flags\": \"0;0;0\" }\n" +
"},\n" +
"{\"type\": \"Feature\",\n" +
"  \"geometry\": {\n" +
"    \"type\": \"Point\",\n" +
"    \"coordinates\": [-124.322, 43.345] },\n" +
"  \"properties\": {\n" +
"    \"station_id\": \"urn:ioos:station:NOAA.NOS.CO-OPS:9432780\",\n" +
"    \"time\": \"2008-09-01T14:24:00Z\",\n" +
"    \"sensor_id\": \"urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1\",\n" +
"    \"sea_water_temperature\": 11.7,\n" +
"    \"quality_flags\": \"0;0;0\" }\n" +
"},\n" +
"{\"type\": \"Feature\",\n" +
"  \"geometry\": {\n" +
"    \"type\": \"Point\",\n" +
"    \"coordinates\": [-124.322, 43.345] },\n" +
"  \"properties\": {\n" +
"    \"station_id\": \"urn:ioos:station:NOAA.NOS.CO-OPS:9432780\",\n" +
"    \"time\": \"2008-09-01T14:30:00Z\",\n" +
"    \"sensor_id\": \"urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1\",\n" +
"    \"sea_water_temperature\": 11.6,\n" +
"    \"quality_flags\": \"0;0;0\" }\n" +
"},\n" +
"{\"type\": \"Feature\",\n" +
"  \"geometry\": {\n" +
"    \"type\": \"Point\",\n" +
"    \"coordinates\": [-124.322, 43.345] },\n" +
"  \"properties\": {\n" +
"    \"station_id\": \"urn:ioos:station:NOAA.NOS.CO-OPS:9432780\",\n" +
"    \"time\": \"2008-09-01T14:36:00Z\",\n" +
"    \"sensor_id\": \"urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1\",\n" +
"    \"sea_water_temperature\": 11.6,\n" +
"    \"quality_flags\": \"0;0;0\" }\n" +
"},\n" +
"{\"type\": \"Feature\",\n" +
"  \"geometry\": {\n" +
"    \"type\": \"Point\",\n" +
"    \"coordinates\": [-124.322, 43.345] },\n" +
"  \"properties\": {\n" +
"    \"station_id\": \"urn:ioos:station:NOAA.NOS.CO-OPS:9432780\",\n" +
"    \"time\": \"2008-09-01T14:42:00Z\",\n" +
"    \"sensor_id\": \"urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1\",\n" +
"    \"sea_water_temperature\": 11.6,\n" +
"    \"quality_flags\": \"0;0;0\" }\n" +
"},\n" +
"{\"type\": \"Feature\",\n" +
"  \"geometry\": {\n" +
"    \"type\": \"Point\",\n" +
"    \"coordinates\": [-124.322, 43.345] },\n" +
"  \"properties\": {\n" +
"    \"station_id\": \"urn:ioos:station:NOAA.NOS.CO-OPS:9432780\",\n" +
"    \"time\": \"2008-09-01T14:48:00Z\",\n" +
"    \"sensor_id\": \"urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1\",\n" +
"    \"sea_water_temperature\": 11.6,\n" +
"    \"quality_flags\": \"0;0;0\" }\n" +
"},\n" +
"{\"type\": \"Feature\",\n" +
"  \"geometry\": {\n" +
"    \"type\": \"Point\",\n" +
"    \"coordinates\": [-124.322, 43.345] },\n" +
"  \"properties\": {\n" +
"    \"station_id\": \"urn:ioos:station:NOAA.NOS.CO-OPS:9432780\",\n" +
"    \"time\": \"2008-09-01T14:54:00Z\",\n" +
"    \"sensor_id\": \"urn:ioos:sensor:NOAA.NOS.CO-OPS:9432780:E1\",\n" +
"    \"sea_water_temperature\": 11.6,\n" +
"    \"quality_flags\": \"0;0;0\" }\n" +
"}\n" +
"  ],\n" +
//2019-03-20 was "  \"bbox\": [-124.322, 43.345, null, -124.322, 43.345, null]\n" +
"  \"bbox\": [-124.322, 43.345, -124.322, 43.345]\n" +
"}\n";
            Test.ensureEqual(results, expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NOS Server is in flux.", t); 
        }
    
    }

    /**
     * This should work, but server is in flux so it often breaks.
     * Note: starting in 2009, there is a test server at https://sdftest.ndbc.noaa.gov/sos/ .
     * @throws Throwable if trouble
     */
    public static void testNdbcSosCurrents(String datasetIdPrefix) throws Throwable {
        String2.log("\n*** EDDTableFromSOS.testNdbcSosCurrents");
        testVerboseOn();
        //see 6/12/08 email from Roy
        //   see web page:  https://sdf.ndbc.noaa.gov/sos/
        //https://sdf.ndbc.noaa.gov/sos/server.php?request=GetObservation&service=SOS
        //  &offering=NDBC:46088&observedproperty=currents&responseformat=text/xml;
        //  schema=%22ioos/0.6.1%22&eventtime=2008-06-01T00:00Z/2008-06-02T00:00Z
        //request from  -70.14          43.53     1193961600            NaN     NDBC:44007
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, datasetIdPrefix + "ndbcSosCurrents");  
        double tLon, tLat;
        String name, tName, results = null, expected, userDapQuery;
        Table table;
        String error = "";

        try { 

            String2.log("\n*** EDDTableFromSOS.testNdbcSosCurrents .das\n");
            String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
            tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                eddTable.className() + "_ndbc_test1", ".das"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"Attributes {\n" +
" s {\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -172.088, -66.588;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range 17.037, 60.799;\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  station_id {\n" +
"    String cf_role \"timeseries_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station ID\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    String axis \"Z\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.187181e+9, NaN;\n" +
"    String axis \"T\";\n" +
"    String cf_role \"profile_id\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" + 
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  sensor_id {\n" +
"    String comment \"Always check the quality_flags before using this data.\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Sensor ID\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"  }\n" +
"  bin {\n" +
"    String comment \"Always check the quality_flags before using this data.\";\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Bin\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  direction_of_sea_water_velocity {\n" +
"    Float64 colorBarMaximum 0.5;\n" +
"    Float64 colorBarMinimum -0.5;\n" +
"    String comment \"Always check the quality_flags before using this data.\";\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Direction Of Sea Water Velocity\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String standard_name \"direction_of_sea_water_velocity\";\n" +
"    String units \"degrees_true\";\n" +
"  }\n" +
"  sea_water_speed {\n" +
"    Float64 colorBarMaximum 0.5;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"Always check the quality_flags before using this data.\";\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Sea Water Speed\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String standard_name \"sea_water_speed\";\n" +
"    String units \"cm/s\";\n" +
"  }\n" +
"  upward_sea_water_velocity {\n" +
"    Float64 colorBarMaximum 0.5;\n" +
"    Float64 colorBarMinimum -0.5;\n" +
"    String comment \"Always check the quality_flags before using this data.\";\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Upward Sea Water Velocity\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String standard_name \"upward_sea_water_velocity\";\n" +
"    String units \"cm/s\";\n" +
"  }\n" +
"  error_velocity {\n" +
"    String comment \"Always check the quality_flags before using this data.\";\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Error Velocity\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String units \"cm/s\";\n" +
"  }\n" +
"  platform_orientation {\n" +
"    String comment \"Always check the quality_flags before using this data.\";\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Platform Orientation\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String units \"degrees_true\";\n" +
"  }\n" +
"  platform_pitch_angle {\n" +
"    String comment \"Always check the quality_flags before using this data.\";\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Platform Pitch Angle\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String units \"degree\";\n" +
"  }\n" +
"  platform_roll_angle {\n" +
"    String comment \"Always check the quality_flags before using this data.\";\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Platform Roll Angle\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String units \"degree\";\n" +
"  }\n" +
"  sea_water_temperature {\n" +
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"Always check the quality_flags before using this data.\";\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Sea Water Temperature\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String standard_name \"sea_water_temperature\";\n" +
"    String units \"Cel\";\n" +
"  }\n" +
"  pct_good_3_beam {\n" +
"    String comment \"Always check the quality_flags before using this data.\";\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Percent Good 3 Beam\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String units \"percent\";\n" +
"  }\n" +
"  pct_good_4_beam {\n" +
"    String comment \"Always check the quality_flags before using this data.\";\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Percent Good 4 Beam\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String units \"percent\";\n" +
"  }\n" +
"  pct_rejected {\n" +
"    String comment \"Always check the quality_flags before using this data.\";\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Percent Rejected\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String units \"percent\";\n" +
"  }\n" +
"  pct_bad {\n" +
"    String comment \"Always check the quality_flags before using this data.\";\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Percent Bad\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String units \"percent\";\n" +
"  }\n" +
"  echo_intensity_beam1 {\n" +
"    String comment \"Always check the quality_flags before using this data.\";\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Echo Intensity Beam 1\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  echo_intensity_beam2 {\n" +
"    String comment \"Always check the quality_flags before using this data.\";\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Echo Intensity Beam #2\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  echo_intensity_beam3 {\n" +
"    String comment \"Always check the quality_flags before using this data.\";\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Echo Intensity Beam 3\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  echo_intensity_beam4 {\n" +
"    String comment \"Always check the quality_flags before using this data.\";\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Echo Intensity Beam 4\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  correlation_magnitude_beam1 {\n" +
"    String comment \"Always check the quality_flags before using this data.\";\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Correlation Magnitude Beam 1\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  correlation_magnitude_beam2 {\n" +
"    String comment \"Always check the quality_flags before using this data.\";\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Correlation Magnitude Beam #2\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  correlation_magnitude_beam3 {\n" +
"    String comment \"Always check the quality_flags before using this data.\";\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Correlation Magnitude Beam 3\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  correlation_magnitude_beam4 {\n" +
"    String comment \"Always check the quality_flags before using this data.\";\n" +
"    String ioos_category \"Currents\";\n" +
"    String long_name \"Correlation Magnitude Beam 4\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  quality_flags {\n" +
"    String comment \"Always check the quality_flags before using this data.\";\n" +
"    String description \"These nine, semi-colon-separated quality flags represent the results of the following quality tests based on their position (left to right) in the flags field:\n" +
"Flag#  Meaning\n" +
"-----  ------------------------------------------\n" +
"  1    The overall bin status\n" +
"  2    The ADCP Built-In Test (BIT) status\n" +
"  3    The Error Velocity test status\n" +
"  4    The Percent Good test status\n" +
"  5    The Correlation Magnitude test status\n" +
"  6    The Vertical Velocity test status\n" +
"  7    The North Horizontal Velocity test status\n" +
"  8    The East Horizontal Velocity test status\n" +
"  9    The Echo Intensity test status\n" +
"\n" +
"Valid flag values are:\n" +
"0 = quality not evaluated\n" +
"1 = failed quality test\n" +
"2 = questionable or suspect data\n" +
"3 = good data/passed quality test\n" +
"9 = missing data\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Quality Flags\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/currents\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"TimeSeriesProfile\";\n" +
"    String cdm_profile_variables \"time\";\n" +
"    String cdm_timeseries_variables \"station_id, longitude, latitude, sensor_id\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    Float64 Easternmost_Easting -66.588;\n" +  //2019-03-19 small changes to bbox
"    String featureType \"TimeSeriesProfile\";\n" +
"    Float64 geospatial_lat_max 60.799;\n" +
"    Float64 geospatial_lat_min 17.037;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max -66.588;\n" +
"    Float64 geospatial_lon_min -172.088;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"" + today;
        String tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NDBC Server is in flux.", t); 
        }

        try {
//+ " https://sdf" + datasetIdPrefix + ".ndbc.noaa.gov/sos/server.php\n" +
//today + " " + EDStatic.erddapUrl + //in tests, always use non-https url
//                "/tabledap/" + 
expected = 
datasetIdPrefix + "ndbcSosCurrents.das\";\n" +
"    String infoUrl \"https://sdf.ndbc.noaa.gov/sos/\";\n" +
"    String institution \"NOAA NDBC\";\n" +
"    String keywords \"angle, atmosphere, bad, beam, bin, circulation, correlation, currents, depth, direction, direction_of_sea_water_velocity, Earth Science > Oceans > Ocean Circulation > Ocean Currents, Earth Science > Oceans > Ocean Temperature > Water Temperature, echo, error, flags, good, height, identifier, intensity, magnitude, ndbc, noaa, ocean, oceans, orientation, percent, pitch, platform, quality, rejected, roll, sea, sea_water_speed, sea_water_temperature, seawater, sensor, sos, speed, station, temperature, time, upward, upward_sea_water_velocity, velocity, water\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    Float64 Northernmost_Northing 60.799;\n" +
"    String sourceUrl \"https://sdf.ndbc.noaa.gov/sos/server.php\";\n" +
"    Float64 Southernmost_Northing 17.037;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String subsetVariables \"station_id, longitude, latitude\";\n" +
"    String summary \"The NOAA NDBC SOS server is part of the IOOS DIF SOS Project.  The stations in this dataset have currents data.\n" +
"\n" +
"Because of the nature of SOS requests, requests for data MUST include constraints for the longitude, latitude, time, and/or station_id variables.\n" +
"\n" +
"WARNING: Always check the quality_flags before using this data. A simple criterion is: only use a row of data if the first quality_flags value for the row (overall bin status) is 3 (good data/passed quality test). You can do this by appending &quality_flags=~\\\"3;.*\\\" to your request.\";\n" +
"    String time_coverage_start \"2007-08-15T12:30:00Z\";\n" +
"    String title \"NOAA NDBC SOS, 2007-present, currents\";\n" +
"    Float64 Westernmost_Easting -172.088;\n" +
"  }\n" +
"}\n";
            int tPo = results.indexOf(expected.substring(0, 17));
            Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
            Test.ensureEqual(
                results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
                expected, "results=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NDBC Server is in flux.", t); 
        }

        try {
            //test lon lat (numeric) = > <, and time > <
            String2.log("\n*** EDDTableFromSOS.testNdbcSosCurrents test get one station .CSV data\n");
            tName = eddTable.makeNewFileForDapQuery(null, null, "" +
                //2019-03-19 was -87.94,29.16    now -87.944,29.108
                "&longitude=-87.944&latitude>=29.1&latitude<29.2&time>=2008-06-01T14:00&time<=2008-06-01T14:30", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_ndbc_test1", ".csv"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"longitude,latitude,station_id,altitude,time,sensor_id,bin,direction_of_sea_water_velocity,sea_water_speed,upward_sea_water_velocity,error_velocity,platform_orientation,platform_pitch_angle,platform_roll_angle,sea_water_temperature,pct_good_3_beam,pct_good_4_beam,pct_rejected,pct_bad,echo_intensity_beam1,echo_intensity_beam2,echo_intensity_beam3,echo_intensity_beam4,correlation_magnitude_beam1,correlation_magnitude_beam2,correlation_magnitude_beam3,correlation_magnitude_beam4,quality_flags\n" +
"degrees_east,degrees_north,,m,UTC,,count,degrees_true,cm/s,cm/s,cm/s,degrees_true,degree,degree,Cel,percent,percent,percent,percent,count,count,count,count,count,count,count,count,\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-56.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,1,83,30.2,-2.6,-4.7,NaN,NaN,NaN,NaN,0,100,0,NaN,189,189,189,193,241,239,242,240,3;9;3;3;3;3;3;3;0\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-88.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,2,96,40.5,-2.5,-3.7,NaN,NaN,NaN,NaN,0,100,0,NaN,177,174,180,178,237,235,230,237,3;9;3;3;3;3;3;3;0\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-120.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,3,96,40.7,-1.3,-9.6,NaN,NaN,NaN,NaN,0,100,0,NaN,165,163,159,158,232,234,238,236,3;9;3;3;3;3;3;3;0\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-152.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,4,96,35.3,-2.0,-2.2,NaN,NaN,NaN,NaN,0,100,0,NaN,151,147,160,153,232,235,237,241,3;9;3;3;3;3;3;3;0\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);

            expected = 
"-87.944,29.108,urn:ioos:station:wmo:42376,-952.8,2008-06-01T14:23:00Z,urn:ioos:sensor:wmo:42376::adcp0,29,89,4.0,0.5,0.0,NaN,NaN,NaN,NaN,100,0,0,NaN,74,75,96,48,236,237,NaN,239,2;9;2;3;9;3;3;3;2\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-984.8,2008-06-01T14:23:00Z,urn:ioos:sensor:wmo:42376::adcp0,30,0,0.0,0.0,0.0,NaN,NaN,NaN,NaN,0,0,0,NaN,69,69,93,39,NaN,NaN,NaN,217,1;9;2;1;9;1;1;1;2\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-1016.8,2008-06-01T14:23:00Z,urn:ioos:sensor:wmo:42376::adcp0,31,0,0.0,0.0,0.0,NaN,NaN,NaN,NaN,0,0,0,NaN,62,65,89,33,NaN,NaN,NaN,245,1;9;2;1;9;1;1;1;2\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-1048.8,2008-06-01T14:23:00Z,urn:ioos:sensor:wmo:42376::adcp0,32,0,0.0,0.0,0.0,NaN,NaN,NaN,NaN,0,0,0,NaN,199,111,73,1,NaN,NaN,NaN,212,1;9;2;1;9;1;1;1;2\n";
            Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NDBC Server is in flux.", t); 
        }

        try {
            //test quality_flags regex (just GOOD data): &quality_flags=~"3;.*"
            String2.log("\n*** EDDTableFromSOS.testNcbcSosCurrents test get one station .CSV data\n");
            tName = eddTable.makeNewFileForDapQuery(null, null, "" +
                //2019-03-19 was -87.94    now -87.944
                "&longitude=-87.944&latitude>=29.1&latitude<29.2&time>=2008-06-01T14:00&time<=2008-06-01T14:30" +
                "&quality_flags=~\"3;.*\"", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_ndbc_test1", ".csv"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"longitude,latitude,station_id,altitude,time,sensor_id,bin,direction_of_sea_water_velocity,sea_water_speed,upward_sea_water_velocity,error_velocity,platform_orientation,platform_pitch_angle,platform_roll_angle,sea_water_temperature,pct_good_3_beam,pct_good_4_beam,pct_rejected,pct_bad,echo_intensity_beam1,echo_intensity_beam2,echo_intensity_beam3,echo_intensity_beam4,correlation_magnitude_beam1,correlation_magnitude_beam2,correlation_magnitude_beam3,correlation_magnitude_beam4,quality_flags\n" +
"degrees_east,degrees_north,,m,UTC,,count,degrees_true,cm/s,cm/s,cm/s,degrees_true,degree,degree,Cel,percent,percent,percent,percent,count,count,count,count,count,count,count,count,\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-56.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,1,83,30.2,-2.6,-4.7,NaN,NaN,NaN,NaN,0,100,0,NaN,189,189,189,193,241,239,242,240,3;9;3;3;3;3;3;3;0\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-88.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,2,96,40.5,-2.5,-3.7,NaN,NaN,NaN,NaN,0,100,0,NaN,177,174,180,178,237,235,230,237,3;9;3;3;3;3;3;3;0\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-120.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,3,96,40.7,-1.3,-9.6,NaN,NaN,NaN,NaN,0,100,0,NaN,165,163,159,158,232,234,238,236,3;9;3;3;3;3;3;3;0\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-152.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,4,96,35.3,-2.0,-2.2,NaN,NaN,NaN,NaN,0,100,0,NaN,151,147,160,153,232,235,237,241,3;9;3;3;3;3;3;3;0\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);

            expected = //this is different from previous test
"-87.944,29.108,urn:ioos:station:wmo:42376,-600.8,2008-06-01T14:23:00Z,urn:ioos:sensor:wmo:42376::adcp0,18,160,4.6,0.1,-0.2,NaN,NaN,NaN,NaN,0,100,0,NaN,119,120,88,108,240,239,240,242,3;9;3;3;3;3;3;3;3\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-632.8,2008-06-01T14:23:00Z,urn:ioos:sensor:wmo:42376::adcp0,19,166,5.9,-0.6,-0.7,NaN,NaN,NaN,NaN,0,100,0,NaN,112,113,89,106,241,240,240,240,3;9;3;3;3;3;3;3;3\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-664.8,2008-06-01T14:23:00Z,urn:ioos:sensor:wmo:42376::adcp0,20,142,3.7,-1.7,-3.8,NaN,NaN,NaN,NaN,0,100,0,NaN,107,108,90,102,241,240,240,240,3;9;3;3;3;3;3;3;3\n";
            Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NDBC Server is in flux.", t); 
        }

        try {
            //test station regex
            String2.log("\n*** EDDTableFromSOS.testNcbcSosCurrents test get 2 stations from regex, 1 time, .CSV data\n");
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&station_id=~\"(urn:ioos:station:wmo:41035|urn:ioos:station:wmo:42376)\"" +
                "&time>=2008-06-01T14:00&time<=2008-06-01T14:15", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_ndbc_test1b", ".csv"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
//Before revamping 2008-10, test returned values below.   NOW DIFFERENT!
//    "urn:ioos:station:wmo:41035,-77.28,34.48,-1.6,2008-06-01T14:00:00Z,223,3.3\n" +    now 74,15.2
//    "urn:ioos:station:wmo:42376,-87.94,29.16,-3.8,2008-06-01T14:00:00Z,206,19.0\n";
"longitude,latitude,station_id,altitude,time,sensor_id,bin,direction_of_sea_water_velocity,sea_water_speed,upward_sea_water_velocity,error_velocity,platform_orientation,platform_pitch_angle,platform_roll_angle,sea_water_temperature,pct_good_3_beam,pct_good_4_beam,pct_rejected,pct_bad,echo_intensity_beam1,echo_intensity_beam2,echo_intensity_beam3,echo_intensity_beam4,correlation_magnitude_beam1,correlation_magnitude_beam2,correlation_magnitude_beam3,correlation_magnitude_beam4,quality_flags\n" +
"degrees_east,degrees_north,,m,UTC,,count,degrees_true,cm/s,cm/s,cm/s,degrees_true,degree,degree,Cel,percent,percent,percent,percent,count,count,count,count,count,count,count,count,\n" +
"-77.28,34.476,urn:ioos:station:wmo:41035,-1.6,2008-06-01T14:00:00Z,urn:ioos:sensor:wmo:41035::pscm0,1,74,15.2,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-56.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,1,83,30.2,-2.6,-4.7,NaN,NaN,NaN,NaN,0,100,0,NaN,189,189,189,193,241,239,242,240,3;9;3;3;3;3;3;3;0\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-88.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,2,96,40.5,-2.5,-3.7,NaN,NaN,NaN,NaN,0,100,0,NaN,177,174,180,178,237,235,230,237,3;9;3;3;3;3;3;3;0\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-120.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,3,96,40.7,-1.3,-9.6,NaN,NaN,NaN,NaN,0,100,0,NaN,165,163,159,158,232,234,238,236,3;9;3;3;3;3;3;3;0\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-152.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,4,96,35.3,-2.0,-2.2,NaN,NaN,NaN,NaN,0,100,0,NaN,151,147,160,153,232,235,237,241,3;9;3;3;3;3;3;3;0\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-184.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,5,89,31.9,-1.9,-1.1,NaN,NaN,NaN,NaN,0,100,0,NaN,145,144,151,151,239,241,237,241,3;9;3;3;3;3;3;3;0\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-216.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,6,90,25.2,-2.7,-3.8,NaN,NaN,NaN,NaN,0,100,0,NaN,144,145,141,148,240,240,239,239,3;9;3;3;3;3;3;3;0\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-248.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,7,74,22.5,-2.6,-4.0,NaN,NaN,NaN,NaN,0,100,0,NaN,142,144,133,131,240,238,241,241,3;9;3;3;3;3;3;3;0\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-280.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,8,65,21.5,-1.7,-0.8,NaN,NaN,NaN,NaN,0,100,0,NaN,132,133,133,119,237,237,241,234,3;9;3;3;3;3;3;3;0\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-312.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,9,71,16.4,-0.2,1.9,NaN,NaN,NaN,NaN,0,100,0,NaN,115,117,132,124,238,238,239,240,3;9;3;3;3;3;3;3;0\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-344.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,10,96,13.8,-1.2,0.6,NaN,NaN,NaN,NaN,0,100,0,NaN,101,102,135,133,242,241,239,240,3;9;3;3;3;3;3;3;0\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-376.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,11,110,14.2,-0.5,0.0,NaN,NaN,NaN,NaN,0,100,0,NaN,101,102,133,133,240,241,240,242,3;9;3;3;3;3;3;3;0\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-408.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,12,121,12.9,-1.6,-4.4,NaN,NaN,NaN,NaN,0,100,0,NaN,103,105,125,137,241,240,240,240,3;9;3;3;3;3;3;3;0\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-440.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,13,111,7.4,-1.9,-4.6,NaN,NaN,NaN,NaN,0,100,0,NaN,106,108,116,136,239,240,241,240,3;9;3;3;3;3;3;3;0\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-472.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,14,6,2.0,0.1,0.4,NaN,NaN,NaN,NaN,0,100,0,NaN,110,114,104,130,241,241,240,241,3;9;3;3;3;3;3;3;0\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-504.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,15,17,2.8,1.0,2.3,NaN,NaN,NaN,NaN,0,100,0,NaN,118,122,94,125,242,241,241,241,3;9;3;3;3;3;3;3;0\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-536.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,16,37,3.8,0.6,2.5,NaN,NaN,NaN,NaN,0,100,0,NaN,121,124,89,122,240,240,241,240,3;9;3;3;3;3;3;3;3\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-568.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,17,124,0.4,1.2,2.3,NaN,NaN,NaN,NaN,0,100,0,NaN,122,124,88,115,240,240,239,240,3;9;3;3;3;3;3;3;3\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-600.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,18,147,4.5,0.0,0.5,NaN,NaN,NaN,NaN,0,100,0,NaN,119,120,88,108,240,240,241,241,3;9;3;3;3;3;3;3;3\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-632.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,19,167,5.7,-0.1,-1.2,NaN,NaN,NaN,NaN,0,100,0,NaN,113,114,89,105,241,240,240,239,3;9;3;3;3;3;3;3;3\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-664.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,20,150,3.8,-0.8,-3.1,NaN,NaN,NaN,NaN,0,100,0,NaN,108,109,90,101,241,241,240,239,3;9;3;3;3;3;3;3;3\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-696.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,21,317,4.1,0.0,0.0,NaN,NaN,NaN,NaN,100,0,0,NaN,104,104,92,98,241,242,NaN,239,2;9;2;3;9;3;3;3;3\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-728.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,22,31,2.9,0.3,0.0,NaN,NaN,NaN,NaN,100,0,0,NaN,102,103,96,97,240,240,NaN,241,2;9;2;3;9;3;3;3;2\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-760.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,23,62,3.4,-0.6,0.0,NaN,NaN,NaN,NaN,100,0,0,NaN,98,100,99,87,239,240,NaN,240,2;9;2;3;9;3;3;3;2\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-792.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,24,24,4.4,0.2,0.0,NaN,NaN,NaN,NaN,100,0,0,NaN,92,95,102,88,241,241,NaN,241,2;9;2;3;9;3;3;3;2\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-824.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,25,56,4.8,-0.2,0.0,NaN,NaN,NaN,NaN,100,0,0,NaN,88,90,103,81,239,240,NaN,240,2;9;2;3;9;3;3;3;2\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-856.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,26,55,2.8,0.0,0.0,NaN,NaN,NaN,NaN,100,0,0,NaN,87,87,103,112,240,240,NaN,239,2;9;2;3;9;3;3;3;2\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-888.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,27,68,4.6,-0.3,0.0,NaN,NaN,NaN,NaN,100,0,0,NaN,86,87,104,166,240,239,NaN,238,2;9;2;3;9;3;3;3;2\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-920.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,28,68,5.2,-0.3,0.0,NaN,NaN,NaN,NaN,100,0,0,NaN,78,80,99,78,238,238,NaN,238,2;9;2;3;9;3;3;3;2\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-952.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,29,75,4.3,0.4,0.0,NaN,NaN,NaN,NaN,100,0,0,NaN,73,75,96,48,237,238,NaN,240,2;9;2;3;9;3;3;3;2\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-984.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,30,0,0.0,0.0,0.0,NaN,NaN,NaN,NaN,0,0,0,NaN,69,68,92,37,NaN,NaN,NaN,222,1;9;2;1;9;1;1;1;2\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-1016.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,31,0,0.0,0.0,0.0,NaN,NaN,NaN,NaN,0,0,0,NaN,63,64,89,31,NaN,NaN,NaN,244,1;9;2;1;9;1;1;1;2\n" +
"-87.944,29.108,urn:ioos:station:wmo:42376,-1048.8,2008-06-01T14:03:00Z,urn:ioos:sensor:wmo:42376::adcp0,32,0,0.0,0.0,0.0,NaN,NaN,NaN,NaN,0,0,0,NaN,197,112,72,0,NaN,NaN,NaN,215,1;9;2;1;9;1;1;1;2\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NDBC Server is in flux.", t); 
        }

        try {
            //test station =   
            String2.log("\n*** EDDTableFromSOS.testNdbcSosCurrents test get by station name, multi depths, .CSV data\n");
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&station_id=\"urn:ioos:station:wmo:41012\"&time>=2008-06-01T00&time<=2008-06-01T01", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_ndbc_test2", ".csv"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"longitude,latitude,station_id,altitude,time,sensor_id,bin,direction_of_sea_water_velocity,sea_water_speed,upward_sea_water_velocity,error_velocity,platform_orientation,platform_pitch_angle,platform_roll_angle,sea_water_temperature,pct_good_3_beam,pct_good_4_beam,pct_rejected,pct_bad,echo_intensity_beam1,echo_intensity_beam2,echo_intensity_beam3,echo_intensity_beam4,correlation_magnitude_beam1,correlation_magnitude_beam2,correlation_magnitude_beam3,correlation_magnitude_beam4,quality_flags\n" +
"degrees_east,degrees_north,,m,UTC,,count,degrees_true,cm/s,cm/s,cm/s,degrees_true,degree,degree,Cel,percent,percent,percent,percent,count,count,count,count,count,count,count,count,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-5.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,1,56,6.3,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-7.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,2,59,14.8,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-9.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,3,57,20.6,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-11.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,4,56,22.2,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-13.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,5,59,25.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-15.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,6,63,27.6,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-17.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,7,70,31.8,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-19.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,8,73,33.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-21.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,9,74,33.8,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-23.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,10,75,33.5,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-25.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,11,76,32.8,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-27.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,12,75,31.5,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-29.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,13,77,28.9,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-31.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,14,78,25.4,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-33.0,2008-06-01T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,15,80,23.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-5.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,1,52,14.9,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-7.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,2,63,23.8,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-9.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,3,68,28.9,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-11.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,4,71,31.5,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-13.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,5,74,32.5,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-15.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,6,81,31.9,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-17.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,7,83,31.3,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-19.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,8,85,31.3,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-21.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,9,84,32.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-23.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,10,85,30.7,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-25.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,11,86,29.6,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-27.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,12,85,28.2,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-29.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,13,86,26.9,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-31.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,14,86,25.2,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-33.0,2008-06-01T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,15,85,22.6,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n";
            Test.ensureEqual(results, expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NDBC Server is in flux.", t); 
        }

        try {
            //many stations   (this was "long hard test", now with text/csv it is quick and easy)
            String2.log("\n*** EDDTableFromSOS.testNdbcSosCurrents test get data from many stations\n");
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&time>=2008-06-14T00&time<=2008-06-14T02&altitude=-25", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_ndbc_test3", ".csv"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"longitude,latitude,station_id,altitude,time,sensor_id,bin,direction_of_sea_water_velocity,sea_water_speed,upward_sea_water_velocity,error_velocity,platform_orientation,platform_pitch_angle,platform_roll_angle,sea_water_temperature,pct_good_3_beam,pct_good_4_beam,pct_rejected,pct_bad,echo_intensity_beam1,echo_intensity_beam2,echo_intensity_beam3,echo_intensity_beam4,correlation_magnitude_beam1,correlation_magnitude_beam2,correlation_magnitude_beam3,correlation_magnitude_beam4,quality_flags\n" +
"degrees_east,degrees_north,,m,UTC,,count,degrees_true,cm/s,cm/s,cm/s,degrees_true,degree,degree,Cel,percent,percent,percent,percent,count,count,count,count,count,count,count,count,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-25.0,2008-06-14T00:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,11,93,22.4,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-25.0,2008-06-14T01:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,11,96,19.7,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-25.0,2008-06-14T02:00:00Z,urn:ioos:sensor:wmo:41012::adcp0,11,103,19.7,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-76.949,34.207,urn:ioos:station:wmo:41036,-25.0,2008-06-14T00:00:00Z,urn:ioos:sensor:wmo:41036::adcp0,13,170,11.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-76.949,34.207,urn:ioos:station:wmo:41036,-25.0,2008-06-14T01:00:00Z,urn:ioos:sensor:wmo:41036::adcp0,13,190,11.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n" +
"-76.949,34.207,urn:ioos:station:wmo:41036,-25.0,2008-06-14T02:00:00Z,urn:ioos:sensor:wmo:41036::adcp0,13,220,9.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,\n";
            Test.ensureEqual(results, expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NDBC Server is in flux.", t); 
        }


        try {
            String2.log("\n*** EDDTableFromSOS.testNdbcSosCurrents test display error in .png\n");
            tName = eddTable.makeNewFileForDapQuery(null, null, "station_id,longitude,latitude,altitude,time,zztop" +
                "&station_id=\"urn:ioos:network:noaa.nws.ndbc:all\"&time=2008-06-14T00",
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_ndbc_testError", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NDBC Server is in flux.", t); 
        }

    }


    /**
     * This should work, but server is in flux so it often breaks.
     *
     * @throws Throwable if trouble
     */
    public static void testNdbcSosSalinity(String datasetIdPrefix) throws Throwable {
        testVerboseOn();
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, datasetIdPrefix + "ndbcSosSalinity");  
        String name, tName, results = null, expected, userDapQuery;

        try { 

            String2.log("\n*** EDDTableFromSOS.testNdbcSosSalinity .das\n");
            String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
            tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                eddTable.className() + "_ndbcSosSalinity", ".das"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"Attributes {\n" +
" s {\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -148.263, -60.521;\n" + //2014-08-11 was -65.927 //2012-10-10 was -151.719 //2016-09-21 was -64.763
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range -55.0, 60.799;\n" +  //2014-08-12 was 24.843, 2010-10-10 was 17.93, 2016-09-21 was 17.86
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  station_id {\n" +
"    String cf_role \"timeseries_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station ID\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    String axis \"Z\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.1939616e+9, NaN;\n" +  //changes
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  sensor_id {\n" +
"    String comment \"Always check the quality_flags before using this data.\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Sensor ID\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/sea_water_salinity\";\n" +
"  }\n" +
"  sea_water_salinity {\n" +
"    Float64 colorBarMaximum 37.0;\n" +
"    Float64 colorBarMinimum 32.0;\n" +
"    String ioos_category \"Salinity\";\n" +
"    String long_name \"Sea Water Practical Salinity\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/sea_water_salinity\";\n" +
"    String standard_name \"sea_water_practical_salinity\";\n" +
"    String units \"PSU\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"TimeSeries\";\n" +
"    String cdm_timeseries_variables \"station_id, longitude, latitude, sensor_id\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    Float64 Easternmost_Easting -60.521;\n" +
"    String featureType \"TimeSeries\";\n" +
"    Float64 geospatial_lat_max 60.799;\n" +
"    Float64 geospatial_lat_min -55.0;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max -60.521;\n" +
"    Float64 geospatial_lon_min -148.263;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"" + today;
        String tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NDBC Server is in flux.", t); 
        }

        try {
//+ " https://sdf" + datasetIdPrefix + ".ndbc.noaa.gov/sos/server.php\n" +
//today + " " + EDStatic.erddapUrl + //in tests, always use non-https url
//                "/tabledap/" + 
expected = 
datasetIdPrefix + "ndbcSosSalinity.das\";\n" +
"    String infoUrl \"https://sdf.ndbc.noaa.gov/sos/\";\n" +
"    String institution \"NOAA NDBC\";\n" +
"    String keywords \"altitude, atmosphere, density, Earth Science > Atmosphere > Altitude > Station Height, Earth Science > Oceans > Salinity/Density > Salinity, height, identifier, ndbc, noaa, oceans, salinity, sea, sea_water_practical_salinity, seawater, sensor, sos, station, time, water\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    Float64 Northernmost_Northing 60.799;\n" +
"    String sourceUrl \"https://sdf" + datasetIdPrefix + ".ndbc.noaa.gov/sos/server.php\";\n" +
"    Float64 Southernmost_Northing -55.0;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String subsetVariables \"station_id, longitude, latitude\";\n" +
"    String summary \"The NOAA NDBC SOS server is part of the IOOS DIF SOS Project.  The stations in this dataset have sea_water_practical_salinity data.\n" +
"\n" +
"Because of the nature of SOS requests, requests for data MUST include constraints for the longitude, latitude, time, and/or station_id variables.\";\n" +
"    String time_coverage_start \"2007-11-02T00:00:00Z\";\n" +  //changes
"    String title \"NOAA NDBC SOS, 2007-present, sea_water_practical_salinity\";\n" +
"    Float64 Westernmost_Easting -148.263;\n" +
"  }\n" +
"}\n";
            int tPo = results.indexOf(expected.substring(0, 17));
            Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
            Test.ensureEqual(
                results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
                expected, "results=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NDBC Server is in flux.", t); 
        }

        try {
            String2.log("\n*** EDDTableFromSOS.testNdbcSosSalinity test get one station .CSV data\n");
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&station_id=\"urn:ioos:station:wmo:46013\"&time>=2008-08-01&time<2008-08-02", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_ndbcSosSalinity", ".csv"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"longitude,latitude,station_id,altitude,time,sensor_id,sea_water_salinity\n" +
"degrees_east,degrees_north,,m,UTC,,PSU\n" +
"-123.307,38.238,urn:ioos:station:wmo:46013,-1.0,2008-08-01T20:50:00Z,urn:ioos:sensor:wmo:46013::ct1,33.89\n" +
"-123.307,38.238,urn:ioos:station:wmo:46013,-1.0,2008-08-01T22:50:00Z,urn:ioos:sensor:wmo:46013::ct1,33.89\n";
            Test.ensureEqual(results, expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NDBC Server is in flux.", t); 
        }

        try {            
            String2.log("\n*** EDDTableFromSOS.testNdbcSosSalinity test get all stations .CSV data\n");
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&time>=2010-05-27T00:00:00Z&time<=2010-05-27T01:00:00Z", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_ndbcSosSalinityAll", ".csv"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = //46081 appeared 2010-07-20, anrn6 and apqf1 disappeared 2010-10-10
"longitude,latitude,station_id,altitude,time,sensor_id,sea_water_salinity\n" +
"degrees_east,degrees_north,,m,UTC,,PSU\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-1.0,2010-05-27T00:50:00Z,urn:ioos:sensor:wmo:41012::ct1,35.58\n" +
"-65.912,42.327,urn:ioos:station:wmo:44024,-1.0,2010-05-27T01:00:00Z,urn:ioos:sensor:wmo:44024::ct1,32.4\n" +
"-70.566,42.523,urn:ioos:station:wmo:44029,-1.0,2010-05-27T00:00:00Z,urn:ioos:sensor:wmo:44029::ct1,30.2\n" +
"-70.566,42.523,urn:ioos:station:wmo:44029,-1.0,2010-05-27T01:00:00Z,urn:ioos:sensor:wmo:44029::ct1,30.2\n" +
"-70.426,43.179,urn:ioos:station:wmo:44030,-1.0,2010-05-27T01:00:00Z,urn:ioos:sensor:wmo:44030::ct1,30.5\n" +
"-68.996,44.055,urn:ioos:station:wmo:44033,-1.0,2010-05-27T01:00:00Z,urn:ioos:sensor:wmo:44033::ct1,30.8\n" +
"-68.112,44.103,urn:ioos:station:wmo:44034,-1.0,2010-05-27T00:00:00Z,urn:ioos:sensor:wmo:44034::ct1,31.6\n" +
"-68.112,44.103,urn:ioos:station:wmo:44034,-1.0,2010-05-27T01:00:00Z,urn:ioos:sensor:wmo:44034::ct1,31.6\n" +
"-148.263,60.799,urn:ioos:station:wmo:46081,-2.5,2010-05-27T00:50:00Z,urn:ioos:sensor:wmo:46081::ct1,25.24\n";
//"-73.926,42.027,urn:ioos:station:wmo:anrn6,NaN,2010-05-27T00:45:00Z,urn:ioos:sensor:wmo:anrn6::ct1,0.1\n" +
//"-73.926,42.027,urn:ioos:station:wmo:anrn6,NaN,2010-05-27T01:00:00Z,urn:ioos:sensor:wmo:anrn6::ct1,0.1\n" +
//"-84.875,29.786,urn:ioos:station:wmo:apqf1,NaN,2010-05-27T00:15:00Z,urn:ioos:sensor:wmo:apqf1::ct1,2.2\n";
            Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NDBC Server is in flux.", t); 
        }
    }

    /**
     * This is a test to determine longest allowed time request.
     *
     * @throws Throwable if trouble
     */
    public static void testNdbcSosLongTime(String datasetIdPrefix) throws Throwable {
        testVerboseOn();
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, datasetIdPrefix + "ndbcSosWTemp");  
        String name, tName, results, expected, userDapQuery;

        try { 

            String2.log("\n*** EDDTableFromSOS.testNdbcSosLongTime, one station .CSV data\n");
            tName = eddTable.makeNewFileForDapQuery(null, null,   
                "&station_id=\"urn:ioos:station:wmo:41012\"&time>=2002-06-01", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_LongTime", ".csv"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);

            int nb = Math.min(results.length(), 500);
            String2.log(
                "\ncwwcNDBCMet indicates that 41012 has water temperature data from" +
                "\n2002-06-25T19:00:00Z to the present." +
                "\n\nstart of sos results=\n" + results.substring(0, nb) +
                "\n\nend of results=\n" + results.substring(results.length() - nb) +
                "\nnRows ~= " + (results.length()/80));

            expected = 
"longitude,latitude,station_id,altitude,time,sensor_id,sea_water_temperature\n" +
"degrees_east,degrees_north,,m,UTC,,degree_C\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-0.6,2008-07-21T20:50:00Z,urn:ioos:sensor:wmo:41012::watertemp1,28.0\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-0.6,2008-07-21T21:50:00Z,urn:ioos:sensor:wmo:41012::watertemp1,28.0\n" +
"-80.534,30.042,urn:ioos:station:wmo:41012,-0.6,2008-07-21T22:50:00Z,urn:ioos:sensor:wmo:41012::watertemp1,28.0\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, 
                "RESULTS=\n" + results.substring(0, nb));
        } catch (Throwable t) {
            Test.knownProblem(
                "NDBC SOS is still limited to 31 days of data per request."); 
        }

        try {

/* 2012-04-25
Error from https://sdf.ndbc.noaa.gov/sos/server.php?service=SOS&version=1.0.0&request=GetObservation&offering=urn:ioos:station:wmo:41012&observedProperty=http://mmisw.org/ont/cf/parameter/sea_water_temperature&responseFormat=text/csv&eventTime=2006-07-27T21:10:00Z/2012-04-25T19:07:11Z
<ExceptionReport>
  atts=xmlns="http://www.opengis.net/ows/1.1", xmlns:xsi="https://www.w3.org/2001/XMLSchema-instance", xsi:schemaLocation="http://www.opengis
.net/ows/1.1 owsExceptionReport.xsd", version="1.0.0", xml:lang="en"
<ExceptionReport><Exception>
  atts=exceptionCode="InvalidParameterValue", locator="eventTime"
<ExceptionReport><Exception><ExceptionText>
<ExceptionReport><Exception></ExceptionText>
  content=No more than thirty days of data can be requested.
  ERROR is from requestUrl=https://sdf.ndbc.noaa.gov/sos/server.php?service=SOS&version=1.0.0&request=GetObservation&offering=urn:ioos:station:wmo:41012&observedProperty=http://mmisw.org/ont/cf/parameter/sea_water_temperature&responseFormat=text/csv&eventTime=2006-07-27T21:10:00Z/2012-04-25T19:07:11Z

java.lang.RuntimeException: Source Exception="InvalidParameterValue: eventTime: No more than thirty days of data can be requested.".
 at gov.noaa.pfel.erddap.dataset.EDDTableFromSOS.readFromIoosNdbcNos(EDDTableFromSOS.java:1392)
 at gov.noaa.pfel.erddap.dataset.EDDTableFromSOS.getDataForDapQuery(EDDTableFromSOS.java:1231)
 at gov.noaa.pfel.erddap.dataset.EDDTable.respondToDapQuery(EDDTable.java:2266)
 at gov.noaa.pfel.erddap.dataset.EDD.lowMakeFileForDapQuery(EDD.java:2354)
 at gov.noaa.pfel.erddap.dataset.EDD.makeNewFileForDapQuery(EDD.java:2273)
 at gov.noaa.pfel.erddap.dataset.EDDTableFromSOS.testNdbcSosLongTime(EDDTableFromSOS.java:4028)
 at gov.noaa.pfel.erddap.dataset.EDDTableFromSOS.test(EDDTableFromSOS.java:7077)
 at gov.noaa.pfel.coastwatch.TestAll.main(TestAll.java:1417)    
*/
        } catch (Throwable t) {
            throw new RuntimeException("As of 2012-04-09, this fails because requests again limited to 30 days." +
                "\nNDBC SOS Server is in flux.", t); 
        }
    }

    /**
     * This should work, but server is in flux so it often breaks.
     *
     * @throws Throwable if trouble
     */
    public static void testNdbcSosWLevel(String datasetIdPrefix) throws Throwable {
        testVerboseOn();
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, datasetIdPrefix + "ndbcSosWLevel");  
        String name, tName, results = null, expected, userDapQuery;

        try { 

            String2.log("\n*** EDDTableFromSOS.testNdbcSosWLevel .das\n");
            String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
            tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                eddTable.className() + "_ndbcSosWLevel", ".das"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"Attributes {\n" +
" s {\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -176.262, 178.219;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range -46.83, 57.654;\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  station_id {\n" +
"    String cf_role \"timeseries_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station ID\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    Float64 actual_range -8000.0, 0.0;\n" +
"    String axis \"Z\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.2095577e+9, NaN;\n" + //was a value, but many stations have "now", which is propertly converted to NaN here
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  sensor_id {\n" +
"    String comment \"Always check the quality_flags before using this data.\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Sensor ID\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/sea_floor_depth_below_sea_surface\";\n" +
"  }\n" +
"  averaging_interval {\n" +
"    String ioos_category \"Sea Level\";\n" +
"    String long_name \"Averaging Interval\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/sea_floor_depth_below_sea_surface\";\n" +
"    String units \"s\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"TimeSeries\";\n" +
"    String cdm_timeseries_variables \"station_id, longitude, latitude, sensor_id\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    Float64 Easternmost_Easting 178.219;\n" +
"    String featureType \"TimeSeries\";\n" +
"    Float64 geospatial_lat_max 57.654;\n" +
"    Float64 geospatial_lat_min -46.83;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 178.219;\n" +
"    Float64 geospatial_lon_min -176.262;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    Float64 geospatial_vertical_max 0.0;\n" +
"    Float64 geospatial_vertical_min -8000.0;\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"" + today;
        String tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NDBC Server is in flux.", t); 
        }

        try {

//+ " https://sdf" + datasetIdPrefix + ".ndbc.noaa.gov/sos/server.php\n" +
//today + " " + EDStatic.erddapUrl + //in tests, always use non-https url
//                "/tabledap/" + 
expected = 
datasetIdPrefix + "ndbcSosWLevel.das\";\n" +
"    String infoUrl \"https://sdf.ndbc.noaa.gov/sos/\";\n" +
"    String institution \"NOAA NDBC\";\n" +
"    String keywords \"altitude, atmosphere, averaging, below, depth, Earth Science > Oceans > Bathymetry/Seafloor Topography > Bathymetry, floor, height, identifier, interval, level, ndbc, noaa, sea, sea level, sensor, sos, station, surface, time\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    Float64 Northernmost_Northing 57.654;\n" +
"    String sourceUrl \"https://sdf" + datasetIdPrefix + ".ndbc.noaa.gov/sos/server.php\";\n" +
"    Float64 Southernmost_Northing -46.83;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String subsetVariables \"station_id, longitude, latitude\";\n" +
"    String summary \"The NOAA NDBC SOS server is part of the IOOS DIF SOS Project.  The stations in this dataset have sea_floor_depth_below_sea_surface data.\n" +
"\n" +
"Because of the nature of SOS requests, requests for data MUST include constraints for the longitude, latitude, time, and/or station_id variables.\";\n" +
"    String time_coverage_start \"2008-04-30T12:15:00Z\";\n" +    
"    String title \"NOAA NDBC SOS, 2008-present, sea_floor_depth_below_sea_surface\";\n" +
"    Float64 Westernmost_Easting -176.262;\n" +
"  }\n" +
"}\n";
            int tPo = results.indexOf(expected.substring(0, 17));
            Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
            Test.ensureEqual(
                results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
                expected, "results=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NDBC Server is in flux.", t); 
        }

        try {
            String2.log("\n*** EDDTableFromSOS.testNdbcSosWLevel test get one station .CSV data\n");
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&station_id=\"urn:ioos:station:wmo:55015\"&time>=2008-08-01T14:00&time<=2008-08-01T15:00", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_ndbcSosWLevel", ".csv"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"longitude,latitude,station_id,altitude,time,sensor_id,averaging_interval\n" +
"degrees_east,degrees_north,,m,UTC,,s\n" +
"160.256,-46.83,urn:ioos:station:wmo:55015,-4944.303,2008-08-01T14:00:00Z,urn:ioos:sensor:wmo:55015::tsunameter0,900\n" +
"160.256,-46.83,urn:ioos:station:wmo:55015,-4944.215,2008-08-01T14:15:00Z,urn:ioos:sensor:wmo:55015::tsunameter0,900\n" +
"160.256,-46.83,urn:ioos:station:wmo:55015,-4944.121,2008-08-01T14:30:00Z,urn:ioos:sensor:wmo:55015::tsunameter0,900\n" +
"160.256,-46.83,urn:ioos:station:wmo:55015,-4944.025,2008-08-01T14:45:00Z,urn:ioos:sensor:wmo:55015::tsunameter0,900\n" +
"160.256,-46.83,urn:ioos:station:wmo:55015,-4943.93,2008-08-01T15:00:00Z,urn:ioos:sensor:wmo:55015::tsunameter0,900\n";
            Test.ensureEqual(results, expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NDBC Server is in flux.", t); 
        }
    }

    /**
     * This should work, but server is in flux so it often breaks.
     *
     * @throws Throwable if trouble
     */
    public static void testNdbcSosWTemp(String datasetIdPrefix) throws Throwable {
        testVerboseOn();
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, datasetIdPrefix + "ndbcSosWTemp");  
        String name, tName, results = null, expected, userDapQuery;

        try { 

            String2.log("\n*** EDDTableFromSOS.testNdbcSosWTemp .das\n");
            String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
            tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                eddTable.className() + "_ndbcSosWTemp", ".das"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"Attributes {\n" +
" s {\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -178.343, 180.0;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range -55.0, 71.758;\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  station_id {\n" +
"    String cf_role \"timeseries_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station ID\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    String axis \"Z\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.1360742e+9, NaN;\n" +  //pre 2013-11-01 was 1.1540346e+9
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  sensor_id {\n" +
"    String comment \"Always check the quality_flags before using this data.\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Sensor ID\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/sea_water_temperature\";\n" +
"  }\n" +
"  sea_water_temperature {\n" +
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Sea Water Temperature\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/sea_water_temperature\";\n" +
"    String standard_name \"sea_water_temperature\";\n" +
"    String units \"degree_C\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"TimeSeries\";\n" +
"    String cdm_timeseries_variables \"station_id, longitude, latitude, sensor_id\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    Float64 Easternmost_Easting 180.0;\n" +
"    String featureType \"TimeSeries\";\n" +
"    Float64 geospatial_lat_max 71.758;\n" +
"    Float64 geospatial_lat_min -55.0;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 180.0;\n" +
"    Float64 geospatial_lon_min -178.343;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"" + today;
        String tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NDBC Server is in flux.", t); 
        }

        try {
//+ " https://sdf" + datasetIdPrefix + ".ndbc.noaa.gov/sos/server.php\n" +
//today + " " + EDStatic.erddapUrl + //in tests, always use non-https url
//                "/tabledap/" + 
expected = 
datasetIdPrefix + "ndbcSosWTemp.das\";\n" +
"    String infoUrl \"https://sdf.ndbc.noaa.gov/sos/\";\n" +
"    String institution \"NOAA NDBC\";\n" +
"    String keywords \"altitude, atmosphere, Earth Science > Atmosphere > Altitude > Station Height, Earth Science > Oceans > Ocean Temperature > Water Temperature, height, identifier, ndbc, noaa, ocean, oceans, sea, sea_water_temperature, seawater, sensor, sos, station, temperature, time, water\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    Float64 Northernmost_Northing 71.758;\n" +
"    String sourceUrl \"https://sdf" + datasetIdPrefix + ".ndbc.noaa.gov/sos/server.php\";\n" +
"    Float64 Southernmost_Northing -55.0;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String subsetVariables \"station_id, longitude, latitude\";\n" +
"    String summary \"The NOAA NDBC SOS server is part of the IOOS DIF SOS Project.  The stations in this dataset have sea_water_temperature data.\n" +
"\n" +
"Because of the nature of SOS requests, requests for data MUST include constraints for the longitude, latitude, time, and/or station_id variables.\";\n" +
"    String time_coverage_start \"2006-01-01T00:10:00Z\";\n" +  //pre 2013-11-01 was 2006-07-27T21:10:00Z
"    String title \"NOAA NDBC SOS, 2006-present, sea_water_temperature\";\n" +
"    Float64 Westernmost_Easting -178.343;\n" +
"  }\n" +
"}\n";
            int tPo = results.indexOf(expected.substring(0, 17));
            Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
            Test.ensureEqual(
                results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
                expected, "results=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NDBC Server is in flux.", t); 
        }

        try {
            String2.log("\n*** EDDTableFromSOS.testNdbcSosWTemp test get one station .CSV data\n");
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&station_id=\"urn:ioos:station:wmo:46013\"&time>=2008-08-01T14:00&time<2008-08-01T20:00", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_ndbcSosWTemp", ".csv"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = //2019-03-19 was -0.6, now -1.0
"longitude,latitude,station_id,altitude,time,sensor_id,sea_water_temperature\n" +
"degrees_east,degrees_north,,m,UTC,,degree_C\n" +
"-123.307,38.238,urn:ioos:station:wmo:46013,-1.0,2008-08-01T14:50:00Z,urn:ioos:sensor:wmo:46013::watertemp1,10.9\n" +
"-123.307,38.238,urn:ioos:station:wmo:46013,-1.0,2008-08-01T15:50:00Z,urn:ioos:sensor:wmo:46013::watertemp1,10.9\n" +
"-123.307,38.238,urn:ioos:station:wmo:46013,-1.0,2008-08-01T16:50:00Z,urn:ioos:sensor:wmo:46013::watertemp1,10.9\n" +
"-123.307,38.238,urn:ioos:station:wmo:46013,-1.0,2008-08-01T17:50:00Z,urn:ioos:sensor:wmo:46013::watertemp1,10.9\n" +
"-123.307,38.238,urn:ioos:station:wmo:46013,-1.0,2008-08-01T18:50:00Z,urn:ioos:sensor:wmo:46013::watertemp1,11.0\n" +
"-123.307,38.238,urn:ioos:station:wmo:46013,-1.0,2008-08-01T19:50:00Z,urn:ioos:sensor:wmo:46013::watertemp1,11.1\n";
            Test.ensureEqual(results, expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NDBC Server is in flux.", t); 
        }
    }

    /**
     * This should work, but server is in flux so it often breaks.
     *
     * @throws Throwable if trouble
     */
    public static void testNdbcSosBig(String datasetIdPrefix) throws Throwable {
        //no, I want it to run fast: testVerboseOn();
        reallyVerbose = false;
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, datasetIdPrefix + "ndbcSosWTemp");  
        String name, tName, results, expected, query;

        try { 
            String2.log("\n*** EDDTableFromSOS.testNdbcSosBig test get one station .CSV data\n");
            query = "&station_id=\"urn:ioos:station:wmo:46013\""; //for all time
            name = eddTable.className() + "_ndbcSosWTemp";

            timeParts =  true;
            tName = eddTable.makeNewFileForDapQuery(null, null, query, 
                EDStatic.fullTestCacheDirectory, name, ".csv"); 

            timeParts = false;
            tName = eddTable.makeNewFileForDapQuery(null, null, query,
                EDStatic.fullTestCacheDirectory, name, ".csv"); 

            timeParts =  true;
            tName = eddTable.makeNewFileForDapQuery(null, null, query,
                EDStatic.fullTestCacheDirectory, name, ".csv"); 

            timeParts = false;
            tName = eddTable.makeNewFileForDapQuery(null, null, query,
                EDStatic.fullTestCacheDirectory, name, ".csv"); 

        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NDBC Server is in flux.", t); 
        }
        timeParts = false;
    }

    /**
     * This should work, but server is in flux so it often breaks.
     *
     * @throws Throwable if trouble
     */
    public static void testNdbcSosWaves(String datasetIdPrefix) throws Throwable {
        testVerboseOn();
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, datasetIdPrefix + "ndbcSosWaves");  
        String name, tName, results = null, expected = null, userDapQuery;

        try { 

            String2.log("\n*** EDDTableFromSOS.testNdbcSosWaves .das\n");
            String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
            tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                eddTable.className() + "_ndbcSosWaves", ".das"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"Attributes {\n" +
" s {\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -177.75, 179.0;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range -19.713, 71.758;\n" +  //2010-10-10 was 60.8
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  station_id {\n" +
"    String cf_role \"timeseries_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station ID\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    String axis \"Z\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.1939616e+9, NaN;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  sensor_id {\n" +
"    String comment \"Always check the quality_flags before using this data.\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Sensor ID\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n" +
"  }\n" +
"  sea_surface_wave_significant_height {\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Sea Surface Wave Significant Height\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n" +
"    String standard_name \"sea_surface_wave_significant_height\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  sea_surface_wave_peak_period {\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Sea Surface Wave Peak Period\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n" +
"    String standard_name \"sea_surface_wave_peak_period\";\n" +
"    String units \"s\";\n" +
"  }\n" +
"  sea_surface_wave_mean_period {\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Sea Surface Wave Mean Period\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n" +
"    String standard_name \"sea_surface_wave_mean_period\";\n" +
"    String units \"s\";\n" +
"  }\n" +
"  sea_surface_swell_wave_significant_height {\n" +
"    Float64 colorBarMaximum 10.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Sea Surface Swell Wave Significant Height\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n" +
"    String standard_name \"sea_surface_swell_wave_significant_height\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  sea_surface_swell_wave_period {\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Sea Surface Swell Wave Period\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n" +
"    String standard_name \"sea_surface_swell_wave_period\";\n" +
"    String units \"s\";\n" +
"  }\n" +
"  sea_surface_wind_wave_significant_height {\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Sea Surface Wind Wave Significant Height\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n" +
"    String standard_name \"sea_surface_wind_wave_significant_height\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  sea_surface_wind_wave_period {\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Sea Surface Wind Wave Period\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n" +
"    String standard_name \"sea_surface_wind_wave_period\";\n" +
"    String units \"s\";\n" +
"  }\n" +
"  sea_water_temperature {\n" +
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Sea Water Temperature\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n" +
"    String standard_name \"sea_water_temperature\";\n" +
"    String units \"degree_C\";\n" +
"  }\n" +
"  sea_surface_wave_to_direction {\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Sea Surface Wave To Direction\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n" +
"    String standard_name \"sea_surface_wave_to_direction\";\n" +
"    String units \"degrees_true\";\n" +
"  }\n" +
"  sea_surface_swell_wave_to_direction {\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Sea Surface Swell Wave To Direction\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n" +
"    String standard_name \"sea_surface_swell_wave_to_direction\";\n" +
"    String units \"degrees_true\";\n" +
"  }\n" +
"  sea_surface_wind_wave_to_direction {\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Sea Surface Wind Wave To Direction\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n" +
"    String standard_name \"sea_surface_wind_wave_to_direction\";\n" +
"    String units \"degrees_true\";\n" +
"  }\n" +
"  number_of_frequencies {\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Number Of Frequencies\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  center_frequencies {\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Center Frequencies\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n" +
"    String units \"Hz\";\n" +
"  }\n" +
"  bandwidths {\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Bandwidths\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n" +
"    String units \"Hz\";\n" +
"  }\n" +
"  spectral_energy {\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Spectral Energy\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n" +
"    String units \"m^2/Hz\";\n" +
"  }\n" +
"  mean_wave_direction {\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Mean Wave Direction\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n" +
"    String standard_name \"mean_wave_direction\";\n" +
"    String units \"degrees_true\";\n" +
"  }\n" +
"  principal_wave_direction {\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Principal Wave Direction\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n" +
"    String units \"degrees_true\";\n" +
"  }\n" +
"  polar_coordinate_r1 {\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Polar Coordinate R1\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n" +
"    String units \"1\";\n" +
"  }\n" +
"  polar_coordinate_r2 {\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Polar Coordinate R2\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n" +
"    String units \"1\";\n" +
"  }\n" +
"  calculation_method {\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Calculation Method\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n" +
"  }\n" +
"  sampling_rate {\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Sampling Rate\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/waves\";\n" +
"    String units \"Hz\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"TimeSeries\";\n" +
"    String cdm_timeseries_variables \"station_id, longitude, latitude, sensor_id\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    Float64 Easternmost_Easting 179.0;\n" +
"    String featureType \"TimeSeries\";\n" +
"    Float64 geospatial_lat_max 71.758;\n" +
"    Float64 geospatial_lat_min -19.713;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 179.0;\n" +
"    Float64 geospatial_lon_min -177.75;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"" + today;
        String tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NDBC Server is in flux.", t); 
        }

        try {
//+ " https://sdf" + datasetIdPrefix + ".ndbc.noaa.gov/sos/server.php\n" +
//today + " " + EDStatic.erddapUrl + //in tests, always use non-https url
//                "/tabledap/" + 
expected = 
datasetIdPrefix + "ndbcSosWaves.das\";\n" +
"    String infoUrl \"https://sdf.ndbc.noaa.gov/sos/\";\n" +
"    String institution \"NOAA NDBC\";\n" +
"    String keywords \"Atmosphere > Altitude > Station Height,\n" +
"Oceans > Ocean Temperature > Water Temperature,\n" +
"Oceans > Ocean Waves > Significant Wave Height,\n" +
"Oceans > Ocean Waves > Swells,\n" +
"Oceans > Ocean Waves > Wave Frequency,\n" +
"Oceans > Ocean Waves > Wave Period,\n" +
"Oceans > Ocean Waves > Wave Speed/Direction,\n" +
"Oceans > Ocean Waves > Wind Waves,\n" +
"altitude, atmosphere, bandwidths, calculation, center, coordinate, direction, energy, frequencies, height, identifier, mean, mean_wave_direction, method, ndbc, noaa, number, ocean, oceans, peak, period, polar, principal, rate, sampling, sea, sea_surface_swell_wave_period, sea_surface_swell_wave_significant_height, sea_surface_swell_wave_to_direction, sea_surface_wave_mean_period, sea_surface_wave_peak_period, sea_surface_wave_significant_height, sea_surface_wave_to_direction, sea_surface_wind_wave_period, sea_surface_wind_wave_significant_height, sea_surface_wind_wave_to_direction, sea_water_temperature, seawater, sensor, significant, sos, spectral, speed, station, surface, surface waves, swell, swells, temperature, time, water, wave, waves, wind\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    Float64 Northernmost_Northing 71.758;\n" +
"    String sourceUrl \"https://sdf" + datasetIdPrefix + ".ndbc.noaa.gov/sos/server.php\";\n" +
"    Float64 Southernmost_Northing -19.713;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String subsetVariables \"station_id, longitude, latitude\";\n" +
"    String summary \"The NOAA NDBC SOS server is part of the IOOS DIF SOS Project.  The stations in this dataset have waves data.\n" +
"\n" +
"Because of the nature of SOS requests, requests for data MUST include constraints for the longitude, latitude, time, and/or station_id variables.\";\n" +
"    String time_coverage_start \"2007-11-02T00:00:00Z\";\n" +
"    String title \"NOAA NDBC SOS - waves\";\n" +
"    Float64 Westernmost_Easting -177.75;\n" +
"  }\n" +
"}\n";
            int tPo = results.indexOf(expected.substring(0, 17));
            Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
            Test.ensureEqual(
                results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
                expected, "results=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NDBC Server is in flux.", t); 
        }

        try {
            //test  station=    and superfluous string data > < constraints
            String2.log("\n*** EDDTableFromSOS.testNdbcSosWaves test get one station .CSV data\n");
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&station_id=\"urn:ioos:station:wmo:46013\"&time>=2008-08-01T14&time<=2008-08-01T17" +
                "&calculation_method>=\"A\"&calculation_method<=\"Lonh\"", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_ndbcSosWaves", ".csv"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
// was
//"longitude,latitude,station_id,altitude,time,SignificantWaveHeight,DominantWavePeriod,AverageWavePeriod,SwellHeight,SwellPeriod,WindWaveHeight,WindWavePeriod,WaterTemperature,WaveDuration,CalculationMethod,SamplingRate,NumberOfFrequencies,CenterFrequencies,Bandwidths,SpectralEnergy,MeanWaveDirectionPeakPeriod,SwellWaveDirection,WindWaveDirection,MeanWaveDirection,PrincipalWaveDirection,PolarCoordinateR1,PolarCoordinateR2,DirectionalWaveParameter,FourierCoefficientA1,FourierCoefficientA2,FourierCoefficientB1,FourierCoefficientB2\n" +
//"degrees_east,degrees_north,,m,UTC,m,s,s,m,s,m,s,degree_C,s,,Hz,count,Hz,Hz,m2 Hz-1,degrees_true,degrees_true,degrees_true,degrees_true,degrees_true,degrees_true,degrees_true,degrees_true,m,m,m,m\n" +
////before 2008-10-29 (last week?),I think the DirectionalWaveParameters were different
////it changed again (~11am) vs earlier today (~9am)  2008-10-29
////see email to jeffDlB 2008-10-29
//  "-123.32,38.23,urn:ioos:station:wmo:46013,NaN,2008-08-01T14:50:00Z,1.62,14.81,5.21,1.12,14.8,1.17,4.3,NaN,NaN,Longuet-Higgins (1964),NaN,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.779157;0.372876;2.02274;0.714328;0.675131;0.296029;0.138154;0.605274;1.96737;1.16217;0.884235;0.462599;0.57436;0.504724;0.218129;0.38115;0.504237;0.45285;0.708456;0.626689;0.747685;0.883292;0.632856;0.448383;0.331531;0.123811;0.265022;0.214203;0.208534;0.21145;0.223251;0.114582;0.10544;0.130131;0.118191;0.0652535;0.0604571;0.0167055;0.0158453;0.00866108;0.00483522,176.0,176.0,312.0,158.0;336.0;188.0;11.0;78.0;263.0;189.0;176.0;196.0;212.0;249.0;182.0;267.0;292.0;299.0;306.0;290.0;299.0;304.0;294.0;301.0;304.0;320.0;314.0;311.0;303.0;312.0;317.0;315.0;307.0;316.0;314.0;305.0;317.0;306.0;311.0;310.0;303.0;294.0;308.0;298.0;302.0;303.0;113.0;127.0;113.0,183.0;8.0;181.0;7.0;3.0;301.0;275.0;134.0;123.0;262.0;279.0;92.0;281.0;293.0;299.0;308.0;292.0;306.0;310.0;300.0;304.0;306.0;326.0;317.0;310.0;303.0;312.0;318.0;316.0;309.0;319.0;318.0;306.0;319.0;307.0;313.0;309.0;305.0;286.0;309.0;271.0;296.0;254.0;111.0;99.0;92.0,0.212213;0.112507;0.304966;0.35902;0.254397;0.488626;0.263791;0.515939;0.462758;0.430386;0.497566;0.12097;0.497566;0.653071;0.826652;0.841777;0.702193;0.768824;0.797214;0.741445;0.797214;0.797214;0.826652;0.841777;0.857178;0.841777;0.938514;0.921652;0.905092;0.8118;0.826652;0.826652;0.78289;0.872861;0.905092;0.857178;0.88883;0.841777;0.768824;0.857178;0.677187;0.826652;0.629814;0.797214;0.677187;0.715041,0.768824;0.78289;0.826652;0.497566;0.220049;0.263791;0.488626;0.372277;0.125437;0.386024;0.304966;0.278536;0.310546;0.365588;0.653071;0.66502;0.607386;0.488626;0.525379;0.430386;0.462758;0.446279;0.585756;0.544779;0.564896;0.534991;0.78289;0.768824;0.66502;0.43826;0.525379;0.462758;0.379088;0.715041;0.689577;0.585756;0.641337;0.544779;0.333904;0.554746;0.137339;0.564896;0.134872;0.372277;0.108501;0.340013,,,,,\n" +
////"-123.32,38.23,urn:ioos:station:wmo:46013,NaN,2008-08-01T14:50:00Z,1.62,14.81,5.21,1.12,14.8,1.17,4.3,NaN,NaN,Longuet-Higgins (1964),NaN,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.779157;0.372876;2.02274;0.714328;0.675131;0.296029;0.138154;0.605274;1.96737;1.16217;0.884235;0.462599;0.57436;0.504724;0.218129;0.38115;0.504237;0.45285;0.708456;0.626689;0.747685;0.883292;0.632856;0.448383;0.331531;0.123811;0.265022;0.214203;0.208534;0.21145;0.223251;0.114582;0.10544;0.130131;0.118191;0.0652535;0.0604571;0.0167055;0.0158453;0.00866108;0.00483522,176.0,176.0,312.0,158.0;336.0;188.0;11.0;78.0;263.0;189.0;176.0;196.0;212.0;249.0;182.0;267.0;292.0;299.0;306.0;290.0;299.0;304.0;294.0;301.0;304.0;320.0;314.0;311.0;303.0;312.0;317.0;315.0;307.0;316.0;314.0;305.0;317.0;306.0;311.0;310.0;303.0;294.0;308.0;298.0;302.0;303.0;113.0;127.0;113.0,0.2;0.1;0.3;0.4;0.3;0.5;0.3;0.5;0.5;0.4;0.5;0.1;0.5;0.7;0.8;0.8;0.7;0.8;0.8;0.7;0.8;0.8;0.8;0.8;0.9;0.8;0.9;0.9;0.9;0.8;0.8;0.8;0.8;0.9;0.9;0.9;0.9;0.8;0.8;0.9;0.7;0.8;0.6;0.8;0.7;0.7,0.212213;0.112507;0.304966;0.35902;0.254397;0.488626;0.263791;0.515939;0.462758;0.430386;0.497566;0.12097;0.497566;0.653071;0.826652;0.841777;0.702193;0.768824;0.797214;0.741445;0.797214;0.797214;0.826652;0.841777;0.857178;0.841777;0.938514;0.921652;0.905092;0.8118;0.826652;0.826652;0.78289;0.872861;0.905092;0.857178;0.88883;0.841777;0.768824;0.857178;0.677187;0.826652;0.629814;0.797214;0.677187;0.715041,0.768824;0.78289;0.826652;0.497566;0.220049;0.263791;0.488626;0.372277;0.125437;0.386024;0.304966;0.278536;0.310546;0.365588;0.653071;0.66502;0.607386;0.488626;0.525379;0.430386;0.462758;0.446279;0.585756;0.544779;0.564896;0.534991;0.78289;0.768824;0.66502;0.43826;0.525379;0.462758;0.379088;0.715041;0.689577;0.585756;0.641337;0.544779;0.333904;0.554746;0.137339;0.564896;0.134872;0.372277;0.108501;0.340013,,,,,\n" +
////"-123.32,38.23,urn:ioos:station:wmo:46013,NaN,2008-08-01T14:50:00Z,1.62,14.81,5.21,1.12,14.8,1.17,4.3,NaN,NaN,Longuet-Higgins (1964),NaN,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.779157;0.372876;2.02274;0.714328;0.675131;0.296029;0.138154;0.605274;1.96737;1.16217;0.884235;0.462599;0.57436;0.504724;0.218129;0.38115;0.504237;0.45285;0.708456;0.626689;0.747685;0.883292;0.632856;0.448383;0.331531;0.123811;0.265022;0.214203;0.208534;0.21145;0.223251;0.114582;0.10544;0.130131;0.118191;0.0652535;0.0604571;0.0167055;0.0158453;0.00866108;0.00483522,176.0,176.0,312.0,158.0;336.0;188.0;11.0;78.0;263.0;189.0;176.0;196.0;212.0;249.0;182.0;267.0;292.0;299.0;306.0;290.0;299.0;304.0;294.0;301.0;304.0;320.0;314.0;311.0;303.0;312.0;317.0;315.0;307.0;316.0;314.0;305.0;317.0;306.0;311.0;310.0;303.0;294.0;308.0;298.0;302.0;303.0;113.0;127.0;113.0,0.2;0.1;0.3;0.4;0.3;0.5;0.3;0.5;0.5;0.4;0.5;0.1;0.5;0.7;0.8;0.8;0.7;0.8;0.8;0.7;0.8;0.8;0.8;0.8;0.9;0.8;0.9;0.9;0.9;0.8;0.8;0.8;0.8;0.9;0.9;0.9;0.9;0.8;0.8;0.9;0.7;0.8;0.6;0.8;0.7;0.7,0.212213;0.112507;0.304966;0.35902;0.254397;0.488626;0.263791;0.515939;0.462758;0.430386;0.497566;0.12097;0.497566;0.653071;0.826652;0.841777;0.702193;0.768824;0.797214;0.741445;0.797214;0.797214;0.826652;0.841777;0.857178;0.841777;0.938514;0.921652;0.905092;0.8118;0.826652;0.826652;0.78289;0.872861;0.905092;0.857178;0.88883;0.841777;0.768824;0.857178;0.677187;0.826652;0.629814;0.797214;0.677187;0.715041,0.212213;0.112507;0.304966;0.35902;0.254397;0.488626;0.263791;0.515939;0.462758;0.430386;0.497566;0.12097;0.497566;0.653071;0.826652;0.841777;0.702193;0.768824;0.797214;0.741445;0.797214;0.797214;0.826652;0.841777;0.857178;0.841777;0.938514;0.921652;0.905092;0.8118;0.826652;0.826652;0.78289;0.872861;0.905092;0.857178;0.88883;0.841777;0.768824;0.857178;0.677187;0.826652;0.629814;0.797214;0.677187;0.715041,,,,,\n" +
//  "-123.32,38.23,urn:ioos:station:wmo:46013,NaN,2008-08-01T15:50:00Z,1.52,9.09,4.98,1.0,9.1,1.15,5.6,NaN,NaN,Longuet-Higgins (1964),NaN,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.244172;0.874391;0.950049;0.992051;0.292122;0.416385;0.116264;0.32567;1.0886;1.49577;0.707195;0.412901;0.383;0.336784;0.162325;0.507266;0.721374;0.521185;0.317616;0.580232;0.620904;0.720338;0.544952;0.400361;0.457406;0.340211;0.190368;0.295531;0.258054;0.13138;0.178793;0.207494;0.162191;0.0901461;0.101774;0.0468724;0.036226;0.0442694;0.0218615;0.0143249;0.00447678,291.0,291.0,297.0,245.0;180.0;16.0;1.0;157.0;253.0;192.0;221.0;234.0;193.0;171.0;182.0;331.0;297.0;291.0;287.0;304.0;296.0;295.0;307.0;306.0;297.0;309.0;309.0;310.0;301.0;321.0;300.0;296.0;315.0;295.0;305.0;311.0;311.0;312.0;312.0;311.0;309.0;305.0;310.0;317.0;304.0;303.0;125.0;132.0;122.0,179.0;190.0;1.0;1.0;176.0;292.0;110.0;311.0;321.0;118.0;143.0;131.0;335.0;305.0;296.0;290.0;313.0;310.0;298.0;321.0;306.0;298.0;308.0;308.0;310.0;301.0;323.0;300.0;294.0;320.0;295.0;308.0;312.0;313.0;315.0;311.0;311.0;310.0;305.0;311.0;316.0;309.0;298.0;121.0;132.0;109.0,0.153123;0.340013;0.288822;0.254397;0.372277;0.379088;0.400278;0.430386;0.299487;0.14767;0.228175;0.393087;0.534991;0.741445;0.78289;0.857178;0.702193;0.741445;0.797214;0.689577;0.857178;0.921652;0.905092;0.768824;0.905092;0.88883;0.921652;0.88883;0.872861;0.872861;0.872861;0.872861;0.921652;0.88883;0.857178;0.857178;0.88883;0.857178;0.78289;0.797214;0.728123;0.741445;0.826652;0.8118;0.872861;0.728123,0.768824;0.575231;0.564896;0.689577;0.575231;0.497566;0.150371;0.259051;0.259051;0.728123;0.400278;0.393087;0.728123;0.689577;0.66502;0.677187;0.607386;0.400278;0.575231;0.534991;0.677187;0.8118;0.75501;0.365588;0.741445;0.741445;0.8118;0.689577;0.653071;0.653071;0.629814;0.618498;0.768824;0.689577;0.585756;0.596473;0.66502;0.629814;0.454444;0.415059;0.283632;0.216095;0.488626;0.488626;0.618498;0.236601,,,,,\n" +
////"-123.32,38.23,urn:ioos:station:wmo:46013,NaN,2008-08-01T15:50:00Z,1.52,9.09,4.98,1.0,9.1,1.15,5.6,NaN,NaN,Longuet-Higgins (1964),NaN,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.244172;0.874391;0.950049;0.992051;0.292122;0.416385;0.116264;0.32567;1.0886;1.49577;0.707195;0.412901;0.383;0.336784;0.162325;0.507266;0.721374;0.521185;0.317616;0.580232;0.620904;0.720338;0.544952;0.400361;0.457406;0.340211;0.190368;0.295531;0.258054;0.13138;0.178793;0.207494;0.162191;0.0901461;0.101774;0.0468724;0.036226;0.0442694;0.0218615;0.0143249;0.00447678,291.0,291.0,297.0,245.0;180.0;16.0;1.0;157.0;253.0;192.0;221.0;234.0;193.0;171.0;182.0;331.0;297.0;291.0;287.0;304.0;296.0;295.0;307.0;306.0;297.0;309.0;309.0;310.0;301.0;321.0;300.0;296.0;315.0;295.0;305.0;311.0;311.0;312.0;312.0;311.0;309.0;305.0;310.0;317.0;304.0;303.0;125.0;132.0;122.0,0.2;0.3;0.3;0.3;0.4;0.4;0.4;0.4;0.3;0.1;0.2;0.4;0.5;0.7;0.8;0.9;0.7;0.7;0.8;0.7;0.9;0.9;0.9;0.8;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.8;0.8;0.7;0.7;0.8;0.8;0.9;0.7,0.153123;0.340013;0.288822;0.254397;0.372277;0.379088;0.400278;0.430386;0.299487;0.14767;0.228175;0.393087;0.534991;0.741445;0.78289;0.857178;0.702193;0.741445;0.797214;0.689577;0.857178;0.921652;0.905092;0.768824;0.905092;0.88883;0.921652;0.88883;0.872861;0.872861;0.872861;0.872861;0.921652;0.88883;0.857178;0.857178;0.88883;0.857178;0.78289;0.797214;0.728123;0.741445;0.826652;0.8118;0.872861;0.728123,0.768824;0.575231;0.564896;0.689577;0.575231;0.497566;0.150371;0.259051;0.259051;0.728123;0.400278;0.393087;0.728123;0.689577;0.66502;0.677187;0.607386;0.400278;0.575231;0.534991;0.677187;0.8118;0.75501;0.365588;0.741445;0.741445;0.8118;0.689577;0.653071;0.653071;0.629814;0.618498;0.768824;0.689577;0.585756;0.596473;0.66502;0.629814;0.454444;0.415059;0.283632;0.216095;0.488626;0.488626;0.618498;0.236601,,,,,\n" +
////"-123.32,38.23,urn:ioos:station:wmo:46013,NaN,2008-08-01T15:50:00Z,1.52,9.09,4.98,1.0,9.1,1.15,5.6,NaN,NaN,Longuet-Higgins (1964),NaN,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.244172;0.874391;0.950049;0.992051;0.292122;0.416385;0.116264;0.32567;1.0886;1.49577;0.707195;0.412901;0.383;0.336784;0.162325;0.507266;0.721374;0.521185;0.317616;0.580232;0.620904;0.720338;0.544952;0.400361;0.457406;0.340211;0.190368;0.295531;0.258054;0.13138;0.178793;0.207494;0.162191;0.0901461;0.101774;0.0468724;0.036226;0.0442694;0.0218615;0.0143249;0.00447678,291.0,291.0,297.0,245.0;180.0;16.0;1.0;157.0;253.0;192.0;221.0;234.0;193.0;171.0;182.0;331.0;297.0;291.0;287.0;304.0;296.0;295.0;307.0;306.0;297.0;309.0;309.0;310.0;301.0;321.0;300.0;296.0;315.0;295.0;305.0;311.0;311.0;312.0;312.0;311.0;309.0;305.0;310.0;317.0;304.0;303.0;125.0;132.0;122.0,0.2;0.3;0.3;0.3;0.4;0.4;0.4;0.4;0.3;0.1;0.2;0.4;0.5;0.7;0.8;0.9;0.7;0.7;0.8;0.7;0.9;0.9;0.9;0.8;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.8;0.8;0.7;0.7;0.8;0.8;0.9;0.7,0.153123;0.340013;0.288822;0.254397;0.372277;0.379088;0.400278;0.430386;0.299487;0.14767;0.228175;0.393087;0.534991;0.741445;0.78289;0.857178;0.702193;0.741445;0.797214;0.689577;0.857178;0.921652;0.905092;0.768824;0.905092;0.88883;0.921652;0.88883;0.872861;0.872861;0.872861;0.872861;0.921652;0.88883;0.857178;0.857178;0.88883;0.857178;0.78289;0.797214;0.728123;0.741445;0.826652;0.8118;0.872861;0.728123,0.153123;0.340013;0.288822;0.254397;0.372277;0.379088;0.400278;0.430386;0.299487;0.14767;0.228175;0.393087;0.534991;0.741445;0.78289;0.857178;0.702193;0.741445;0.797214;0.689577;0.857178;0.921652;0.905092;0.768824;0.905092;0.88883;0.921652;0.88883;0.872861;0.872861;0.872861;0.872861;0.921652;0.88883;0.857178;0.857178;0.88883;0.857178;0.78289;0.797214;0.728123;0.741445;0.826652;0.8118;0.872861;0.728123,,,,,\n" +
//  "-123.32,38.23,urn:ioos:station:wmo:46013,NaN,2008-08-01T16:50:00Z,1.49,14.81,5.11,1.01,14.8,1.1,4.8,NaN,NaN,Longuet-Higgins (1964),NaN,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.724702;0.909481;2.34661;0.698133;0.516662;0.499779;0.284884;0.407779;0.94326;1.08406;0.29313;0.464502;0.346171;0.393304;0.327266;0.531525;0.423195;0.328752;0.332852;0.702979;0.627516;0.379029;0.603016;0.337529;0.385623;0.308393;0.266641;0.207837;0.0681764;0.212742;0.18737;0.138199;0.122643;0.130927;0.0889706;0.0656523;0.0608267;0.0359928;0.0115031;0.0100742;0.00469153,175.0,175.0,309.0,287.0;208.0;76.0;353.0;123.0;193.0;205.0;175.0;198.0;155.0;196.0;246.0;285.0;304.0;297.0;324.0;298.0;296.0;299.0;303.0;299.0;298.0;304.0;306.0;309.0;304.0;311.0;299.0;317.0;301.0;308.0;314.0;314.0;325.0;315.0;301.0;312.0;322.0;306.0;305.0;324.0;302.0;326.0;119.0;139.0;137.0,350.0;193.0;12.0;13.0;171.0;135.0;151.0;161.0;162.0;158.0;143.0;301.0;313.0;303.0;304.0;321.0;320.0;303.0;302.0;306.0;299.0;300.0;307.0;305.0;311.0;302.0;316.0;299.0;317.0;299.0;308.0;317.0;320.0;346.0;313.0;304.0;312.0;327.0;305.0;306.0;331.0;299.0;333.0;115.0;139.0;143.0,0.177024;0.224075;0.333904;0.393087;0.273532;0.310546;0.299487;0.534991;0.506669;0.43826;0.249826;0.327905;0.340013;0.8118;0.78289;0.585756;0.653071;0.741445;0.826652;0.826652;0.857178;0.797214;0.841777;0.857178;0.905092;0.88883;0.872861;0.921652;0.905092;0.88883;0.872861;0.872861;0.8118;0.728123;0.872861;0.905092;0.857178;0.8118;0.872861;0.826652;0.841777;0.826652;0.857178;0.78289;0.797214;0.8118,0.596473;0.544779;0.488626;0.228175;0.316228;0.506669;0.479847;0.415059;0.372277;0.236601;0.228175;0.346234;0.479847;0.66502;0.534991;0.534991;0.288822;0.554746;0.728123;0.641337;0.728123;0.525379;0.653071;0.575231;0.768824;0.702193;0.618498;0.741445;0.741445;0.689577;0.629814;0.618498;0.430386;0.400278;0.629814;0.75501;0.629814;0.446279;0.641337;0.488626;0.585756;0.454444;0.618498;0.340013;0.454444;0.422653,,,,,\n";
////"-123.32,38.23,urn:ioos:station:wmo:46013,NaN,2008-08-01T16:50:00Z,1.49,14.81,5.11,1.01,14.8,1.1,4.8,NaN,NaN,Longuet-Higgins (1964),NaN,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.724702;0.909481;2.34661;0.698133;0.516662;0.499779;0.284884;0.407779;0.94326;1.08406;0.29313;0.464502;0.346171;0.393304;0.327266;0.531525;0.423195;0.328752;0.332852;0.702979;0.627516;0.379029;0.603016;0.337529;0.385623;0.308393;0.266641;0.207837;0.0681764;0.212742;0.18737;0.138199;0.122643;0.130927;0.0889706;0.0656523;0.0608267;0.0359928;0.0115031;0.0100742;0.00469153,175.0,175.0,309.0,287.0;208.0;76.0;353.0;123.0;193.0;205.0;175.0;198.0;155.0;196.0;246.0;285.0;304.0;297.0;324.0;298.0;296.0;299.0;303.0;299.0;298.0;304.0;306.0;309.0;304.0;311.0;299.0;317.0;301.0;308.0;314.0;314.0;325.0;315.0;301.0;312.0;322.0;306.0;305.0;324.0;302.0;326.0;119.0;139.0;137.0,0.2;0.2;0.3;0.4;0.3;0.3;0.3;0.5;0.5;0.4;0.2;0.3;0.3;0.8;0.8;0.6;0.7;0.7;0.8;0.8;0.9;0.8;0.8;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.8;0.7;0.9;0.9;0.9;0.8;0.9;0.8;0.8;0.8;0.9;0.8;0.8;0.8,0.177024;0.224075;0.333904;0.393087;0.273532;0.310546;0.299487;0.534991;0.506669;0.43826;0.249826;0.327905;0.340013;0.8118;0.78289;0.585756;0.653071;0.741445;0.826652;0.826652;0.857178;0.797214;0.841777;0.857178;0.905092;0.88883;0.872861;0.921652;0.905092;0.88883;0.872861;0.872861;0.8118;0.728123;0.872861;0.905092;0.857178;0.8118;0.872861;0.826652;0.841777;0.826652;0.857178;0.78289;0.797214;0.8118,0.596473;0.544779;0.488626;0.228175;0.316228;0.506669;0.479847;0.415059;0.372277;0.236601;0.228175;0.346234;0.479847;0.66502;0.534991;0.534991;0.288822;0.554746;0.728123;0.641337;0.728123;0.525379;0.653071;0.575231;0.768824;0.702193;0.618498;0.741445;0.741445;0.689577;0.629814;0.618498;0.430386;0.400278;0.629814;0.75501;0.629814;0.446279;0.641337;0.488626;0.585756;0.454444;0.618498;0.340013;0.454444;0.422653,,,,,\n";
////"-123.32,38.23,urn:ioos:station:wmo:46013,NaN,2008-08-01T16:50:00Z,1.49,14.81,5.11,1.01,14.8,1.1,4.8,NaN,NaN,Longuet-Higgins (1964),NaN,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.724702;0.909481;2.34661;0.698133;0.516662;0.499779;0.284884;0.407779;0.94326;1.08406;0.29313;0.464502;0.346171;0.393304;0.327266;0.531525;0.423195;0.328752;0.332852;0.702979;0.627516;0.379029;0.603016;0.337529;0.385623;0.308393;0.266641;0.207837;0.0681764;0.212742;0.18737;0.138199;0.122643;0.130927;0.0889706;0.0656523;0.0608267;0.0359928;0.0115031;0.0100742;0.00469153,175.0,175.0,309.0,287.0;208.0;76.0;353.0;123.0;193.0;205.0;175.0;198.0;155.0;196.0;246.0;285.0;304.0;297.0;324.0;298.0;296.0;299.0;303.0;299.0;298.0;304.0;306.0;309.0;304.0;311.0;299.0;317.0;301.0;308.0;314.0;314.0;325.0;315.0;301.0;312.0;322.0;306.0;305.0;324.0;302.0;326.0;119.0;139.0;137.0,0.2;0.2;0.3;0.4;0.3;0.3;0.3;0.5;0.5;0.4;0.2;0.3;0.3;0.8;0.8;0.6;0.7;0.7;0.8;0.8;0.9;0.8;0.8;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.9;0.8;0.7;0.9;0.9;0.9;0.8;0.9;0.8;0.8;0.8;0.9;0.8;0.8;0.8,0.177024;0.224075;0.333904;0.393087;0.273532;0.310546;0.299487;0.534991;0.506669;0.43826;0.249826;0.327905;0.340013;0.8118;0.78289;0.585756;0.653071;0.741445;0.826652;0.826652;0.857178;0.797214;0.841777;0.857178;0.905092;0.88883;0.872861;0.921652;0.905092;0.88883;0.872861;0.872861;0.8118;0.728123;0.872861;0.905092;0.857178;0.8118;0.872861;0.826652;0.841777;0.826652;0.857178;0.78289;0.797214;0.8118,0.177024;0.224075;0.333904;0.393087;0.273532;0.310546;0.299487;0.534991;0.506669;0.43826;0.249826;0.327905;0.340013;0.8118;0.78289;0.585756;0.653071;0.741445;0.826652;0.826652;0.857178;0.797214;0.841777;0.857178;0.905092;0.88883;0.872861;0.921652;0.905092;0.88883;0.872861;0.872861;0.8118;0.728123;0.872861;0.905092;0.857178;0.8118;0.872861;0.826652;0.841777;0.826652;0.857178;0.78289;0.797214;0.8118,,,,,\n";
//

//Their bug: (see constraint above) Starting with switch to text/csv 2010-06-07, calculation_method became UNKNOWN!
//Fixed 2010-06-22
  "longitude,latitude,station_id,altitude,time,sensor_id,sea_surface_wave_significant_height,sea_surface_wave_peak_period,sea_surface_wave_mean_period,sea_surface_swell_wave_significant_height,sea_surface_swell_wave_period,sea_surface_wind_wave_significant_height,sea_surface_wind_wave_period,sea_water_temperature,sea_surface_wave_to_direction,sea_surface_swell_wave_to_direction,sea_surface_wind_wave_to_direction,number_of_frequencies,center_frequencies,bandwidths,spectral_energy,mean_wave_direction,principal_wave_direction,polar_coordinate_r1,polar_coordinate_r2,calculation_method,sampling_rate\n" +
  "degrees_east,degrees_north,,m,UTC,,m,s,s,m,s,m,s,degree_C,degrees_true,degrees_true,degrees_true,count,Hz,Hz,m^2/Hz,degrees_true,degrees_true,1,1,,Hz\n" +
//2015-03-10 was 
//"-123.32,38.23,urn:ioos:station:wmo:46013,NaN,2008-08-01T14:50:00Z,urn:ioos:sensor:wmo:46013::wpm1,1.62,14.81,5.21,1.12,14.8,1.17,4.3,NaN,176.0,176.0,312.0,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.779157;0.372876;2.02274;0.714328;0.675131;0.296029;0.138154;0.605274;1.96737;1.16217;0.884235;0.462599;0.57436;0.504724;0.218129;0.38115;0.504237;0.45285;0.708456;0.626689;0.747685;0.883292;0.632856;0.448383;0.331531;0.123811;0.265022;0.214203;0.208534;0.21145;0.223251;0.114582;0.10544;0.130131;0.118191;0.0652535;0.0604571;0.0167055;0.0158453;0.00866108;0.00483522,158.0;336.0;188.0;11.0;78.0;263.0;189.0;176.0;196.0;212.0;249.0;182.0;267.0;292.0;299.0;306.0;290.0;299.0;304.0;294.0;301.0;304.0;320.0;314.0;311.0;303.0;312.0;317.0;315.0;307.0;316.0;314.0;305.0;317.0;306.0;311.0;310.0;303.0;294.0;308.0;298.0;302.0;303.0;113.0;127.0;113.0,183.0;8.0;181.0;7.0;3.0;301.0;275.0;134.0;123.0;262.0;279.0;92.0;281.0;293.0;299.0;308.0;292.0;306.0;310.0;300.0;304.0;306.0;326.0;317.0;310.0;303.0;312.0;318.0;316.0;309.0;319.0;318.0;306.0;319.0;307.0;313.0;309.0;305.0;286.0;309.0;271.0;296.0;254.0;111.0;99.0;92.0,0.212213;0.112507;0.304966;0.35902;0.254397;0.488626;0.263791;0.515939;0.462758;0.430386;0.497566;0.12097;0.497566;0.653071;0.826652;0.841777;0.702193;0.768824;0.797214;0.741445;0.797214;0.797214;0.826652;0.841777;0.857178;0.841777;0.938514;0.921652;0.905092;0.8118;0.826652;0.826652;0.78289;0.872861;0.905092;0.857178;0.88883;0.841777;0.768824;0.857178;0.677187;0.826652;0.629814;0.797214;0.677187;0.715041,0.768824;0.78289;0.826652;0.497566;0.220049;0.263791;0.488626;0.372277;0.125437;0.386024;0.304966;0.278536;0.310546;0.365588;0.653071;0.66502;0.607386;0.488626;0.525379;0.430386;0.462758;0.446279;0.585756;0.544779;0.564896;0.534991;0.78289;0.768824;0.66502;0.43826;0.525379;0.462758;0.379088;0.715041;0.689577;0.585756;0.641337;0.544779;0.333904;0.554746;0.137339;0.564896;0.134872;0.372277;0.108501;0.340013,Longuet-Higgins (1964),NaN\n" +
//"-123.32,38.23,urn:ioos:station:wmo:46013,NaN,2008-08-01T15:50:00Z,urn:ioos:sensor:wmo:46013::wpm1,1.52,9.09,4.98,1.0,9.1,1.15,5.6,NaN,291.0,291.0,297.0,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.244172;0.874391;0.950049;0.992051;0.292122;0.416385;0.116264;0.32567;1.0886;1.49577;0.707195;0.412901;0.383;0.336784;0.162325;0.507266;0.721374;0.521185;0.317616;0.580232;0.620904;0.720338;0.544952;0.400361;0.457406;0.340211;0.190368;0.295531;0.258054;0.13138;0.178793;0.207494;0.162191;0.0901461;0.101774;0.0468724;0.036226;0.0442694;0.0218615;0.0143249;0.00447678,245.0;180.0;16.0;1.0;157.0;253.0;192.0;221.0;234.0;193.0;171.0;182.0;331.0;297.0;291.0;287.0;304.0;296.0;295.0;307.0;306.0;297.0;309.0;309.0;310.0;301.0;321.0;300.0;296.0;315.0;295.0;305.0;311.0;311.0;312.0;312.0;311.0;309.0;305.0;310.0;317.0;304.0;303.0;125.0;132.0;122.0,179.0;190.0;1.0;1.0;176.0;292.0;110.0;311.0;321.0;118.0;143.0;131.0;335.0;305.0;296.0;290.0;313.0;310.0;298.0;321.0;306.0;298.0;308.0;308.0;310.0;301.0;323.0;300.0;294.0;320.0;295.0;308.0;312.0;313.0;315.0;311.0;311.0;310.0;305.0;311.0;316.0;309.0;298.0;121.0;132.0;109.0,0.153123;0.340013;0.288822;0.254397;0.372277;0.379088;0.400278;0.430386;0.299487;0.14767;0.228175;0.393087;0.534991;0.741445;0.78289;0.857178;0.702193;0.741445;0.797214;0.689577;0.857178;0.921652;0.905092;0.768824;0.905092;0.88883;0.921652;0.88883;0.872861;0.872861;0.872861;0.872861;0.921652;0.88883;0.857178;0.857178;0.88883;0.857178;0.78289;0.797214;0.728123;0.741445;0.826652;0.8118;0.872861;0.728123,0.768824;0.575231;0.564896;0.689577;0.575231;0.497566;0.150371;0.259051;0.259051;0.728123;0.400278;0.393087;0.728123;0.689577;0.66502;0.677187;0.607386;0.400278;0.575231;0.534991;0.677187;0.8118;0.75501;0.365588;0.741445;0.741445;0.8118;0.689577;0.653071;0.653071;0.629814;0.618498;0.768824;0.689577;0.585756;0.596473;0.66502;0.629814;0.454444;0.415059;0.283632;0.216095;0.488626;0.488626;0.618498;0.236601,Longuet-Higgins (1964),NaN\n" +
//"-123.32,38.23,urn:ioos:station:wmo:46013,NaN,2008-08-01T16:50:00Z,urn:ioos:sensor:wmo:46013::wpm1,1.49,14.81,5.11,1.01,14.8,1.1,4.8,NaN,175.0,175.0,309.0,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.724702;0.909481;2.34661;0.698133;0.516662;0.499779;0.284884;0.407779;0.94326;1.08406;0.29313;0.464502;0.346171;0.393304;0.327266;0.531525;0.423195;0.328752;0.332852;0.702979;0.627516;0.379029;0.603016;0.337529;0.385623;0.308393;0.266641;0.207837;0.0681764;0.212742;0.18737;0.138199;0.122643;0.130927;0.0889706;0.0656523;0.0608267;0.0359928;0.0115031;0.0100742;0.00469153,287.0;208.0;76.0;353.0;123.0;193.0;205.0;175.0;198.0;155.0;196.0;246.0;285.0;304.0;297.0;324.0;298.0;296.0;299.0;303.0;299.0;298.0;304.0;306.0;309.0;304.0;311.0;299.0;317.0;301.0;308.0;314.0;314.0;325.0;315.0;301.0;312.0;322.0;306.0;305.0;324.0;302.0;326.0;119.0;139.0;137.0,350.0;193.0;12.0;13.0;171.0;135.0;151.0;161.0;162.0;158.0;143.0;301.0;313.0;303.0;304.0;321.0;320.0;303.0;302.0;306.0;299.0;300.0;307.0;305.0;311.0;302.0;316.0;299.0;317.0;299.0;308.0;317.0;320.0;346.0;313.0;304.0;312.0;327.0;305.0;306.0;331.0;299.0;333.0;115.0;139.0;143.0,0.177024;0.224075;0.333904;0.393087;0.273532;0.310546;0.299487;0.534991;0.506669;0.43826;0.249826;0.327905;0.340013;0.8118;0.78289;0.585756;0.653071;0.741445;0.826652;0.826652;0.857178;0.797214;0.841777;0.857178;0.905092;0.88883;0.872861;0.921652;0.905092;0.88883;0.872861;0.872861;0.8118;0.728123;0.872861;0.905092;0.857178;0.8118;0.872861;0.826652;0.841777;0.826652;0.857178;0.78289;0.797214;0.8118,0.596473;0.544779;0.488626;0.228175;0.316228;0.506669;0.479847;0.415059;0.372277;0.236601;0.228175;0.346234;0.479847;0.66502;0.534991;0.534991;0.288822;0.554746;0.728123;0.641337;0.728123;0.525379;0.653071;0.575231;0.768824;0.702193;0.618498;0.741445;0.741445;0.689577;0.629814;0.618498;0.430386;0.400278;0.629814;0.75501;0.629814;0.446279;0.641337;0.488626;0.585756;0.454444;0.618498;0.340013;0.454444;0.422653,Longuet-Higgins (1964),NaN\n";
  "-123.307,38.238,urn:ioos:station:wmo:46013,NaN,2008-08-01T14:50:00Z,urn:ioos:sensor:wmo:46013::wpm1,1.62,14.81,5.21,1.12,14.8,1.17,4.3,NaN,356.0,356.0,132.0,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.779157;0.372876;2.02274;0.714328;0.675131;0.296029;0.138154;0.605274;1.96737;1.16217;0.884235;0.462599;0.57436;0.504724;0.218129;0.38115;0.504237;0.45285;0.708456;0.626689;0.747685;0.883292;0.632856;0.448383;0.331531;0.123811;0.265022;0.214203;0.208534;0.21145;0.223251;0.114582;0.10544;0.130131;0.118191;0.0652535;0.0604571;0.0167055;0.0158453;0.00866108;0.00483522,158.0;336.0;188.0;11.0;78.0;263.0;189.0;176.0;196.0;212.0;249.0;182.0;267.0;292.0;299.0;306.0;290.0;299.0;304.0;294.0;301.0;304.0;320.0;314.0;311.0;303.0;312.0;317.0;315.0;307.0;316.0;314.0;305.0;317.0;306.0;311.0;310.0;303.0;294.0;308.0;298.0;302.0;303.0;113.0;127.0;113.0,183.0;8.0;181.0;7.0;3.0;301.0;275.0;134.0;123.0;262.0;279.0;92.0;281.0;293.0;299.0;308.0;292.0;306.0;310.0;300.0;304.0;306.0;326.0;317.0;310.0;303.0;312.0;318.0;316.0;309.0;319.0;318.0;306.0;319.0;307.0;313.0;309.0;305.0;286.0;309.0;271.0;296.0;254.0;111.0;99.0;92.0,0.212213;0.112507;0.304966;0.35902;0.254397;0.488626;0.263791;0.515939;0.462758;0.430386;0.497566;0.12097;0.497566;0.653071;0.826652;0.841777;0.702193;0.768824;0.797214;0.741445;0.797214;0.797214;0.826652;0.841777;0.857178;0.841777;0.938514;0.921652;0.905092;0.8118;0.826652;0.826652;0.78289;0.872861;0.905092;0.857178;0.88883;0.841777;0.768824;0.857178;0.677187;0.826652;0.629814;0.797214;0.677187;0.715041,0.768824;0.78289;0.826652;0.497566;0.220049;0.263791;0.488626;0.372277;0.125437;0.386024;0.304966;0.278536;0.310546;0.365588;0.653071;0.66502;0.607386;0.488626;0.525379;0.430386;0.462758;0.446279;0.585756;0.544779;0.564896;0.534991;0.78289;0.768824;0.66502;0.43826;0.525379;0.462758;0.379088;0.715041;0.689577;0.585756;0.641337;0.544779;0.333904;0.554746;0.137339;0.564896;0.134872;0.372277;0.108501;0.340013,Longuet-Higgins (1964),NaN\n" +
  "-123.307,38.238,urn:ioos:station:wmo:46013,NaN,2008-08-01T15:50:00Z,urn:ioos:sensor:wmo:46013::wpm1,1.52,9.09,4.98,1.0,9.1,1.15,5.6,NaN,111.0,111.0,117.0,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.244172;0.874391;0.950049;0.992051;0.292122;0.416385;0.116264;0.32567;1.0886;1.49577;0.707195;0.412901;0.383;0.336784;0.162325;0.507266;0.721374;0.521185;0.317616;0.580232;0.620904;0.720338;0.544952;0.400361;0.457406;0.340211;0.190368;0.295531;0.258054;0.13138;0.178793;0.207494;0.162191;0.0901461;0.101774;0.0468724;0.036226;0.0442694;0.0218615;0.0143249;0.00447678,245.0;180.0;16.0;1.0;157.0;253.0;192.0;221.0;234.0;193.0;171.0;182.0;331.0;297.0;291.0;287.0;304.0;296.0;295.0;307.0;306.0;297.0;309.0;309.0;310.0;301.0;321.0;300.0;296.0;315.0;295.0;305.0;311.0;311.0;312.0;312.0;311.0;309.0;305.0;310.0;317.0;304.0;303.0;125.0;132.0;122.0,179.0;190.0;1.0;1.0;176.0;292.0;110.0;311.0;321.0;118.0;143.0;131.0;335.0;305.0;296.0;290.0;313.0;310.0;298.0;321.0;306.0;298.0;308.0;308.0;310.0;301.0;323.0;300.0;294.0;320.0;295.0;308.0;312.0;313.0;315.0;311.0;311.0;310.0;305.0;311.0;316.0;309.0;298.0;121.0;132.0;109.0,0.153123;0.340013;0.288822;0.254397;0.372277;0.379088;0.400278;0.430386;0.299487;0.14767;0.228175;0.393087;0.534991;0.741445;0.78289;0.857178;0.702193;0.741445;0.797214;0.689577;0.857178;0.921652;0.905092;0.768824;0.905092;0.88883;0.921652;0.88883;0.872861;0.872861;0.872861;0.872861;0.921652;0.88883;0.857178;0.857178;0.88883;0.857178;0.78289;0.797214;0.728123;0.741445;0.826652;0.8118;0.872861;0.728123,0.768824;0.575231;0.564896;0.689577;0.575231;0.497566;0.150371;0.259051;0.259051;0.728123;0.400278;0.393087;0.728123;0.689577;0.66502;0.677187;0.607386;0.400278;0.575231;0.534991;0.677187;0.8118;0.75501;0.365588;0.741445;0.741445;0.8118;0.689577;0.653071;0.653071;0.629814;0.618498;0.768824;0.689577;0.585756;0.596473;0.66502;0.629814;0.454444;0.415059;0.283632;0.216095;0.488626;0.488626;0.618498;0.236601,Longuet-Higgins (1964),NaN\n" +
  "-123.307,38.238,urn:ioos:station:wmo:46013,NaN,2008-08-01T16:50:00Z,urn:ioos:sensor:wmo:46013::wpm1,1.49,14.81,5.11,1.01,14.8,1.1,4.8,NaN,355.0,355.0,129.0,46,0.0325;0.0375;0.0425;0.0475;0.0525;0.0575;0.0625;0.0675;0.0725;0.0775;0.0825;0.0875;0.0925;0.1000;0.1100;0.1200;0.1300;0.1400;0.1500;0.1600;0.1700;0.1800;0.1900;0.2000;0.2100;0.2200;0.2300;0.2400;0.2500;0.2600;0.2700;0.2800;0.2900;0.3000;0.3100;0.3200;0.3300;0.3400;0.3500;0.3650;0.3850;0.4050;0.4250;0.4450;0.4650;0.4850,0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0050;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0100;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200;0.0200,0;0;0;0;0;0.724702;0.909481;2.34661;0.698133;0.516662;0.499779;0.284884;0.407779;0.94326;1.08406;0.29313;0.464502;0.346171;0.393304;0.327266;0.531525;0.423195;0.328752;0.332852;0.702979;0.627516;0.379029;0.603016;0.337529;0.385623;0.308393;0.266641;0.207837;0.0681764;0.212742;0.18737;0.138199;0.122643;0.130927;0.0889706;0.0656523;0.0608267;0.0359928;0.0115031;0.0100742;0.00469153,287.0;208.0;76.0;353.0;123.0;193.0;205.0;175.0;198.0;155.0;196.0;246.0;285.0;304.0;297.0;324.0;298.0;296.0;299.0;303.0;299.0;298.0;304.0;306.0;309.0;304.0;311.0;299.0;317.0;301.0;308.0;314.0;314.0;325.0;315.0;301.0;312.0;322.0;306.0;305.0;324.0;302.0;326.0;119.0;139.0;137.0,350.0;193.0;12.0;13.0;171.0;135.0;151.0;161.0;162.0;158.0;143.0;301.0;313.0;303.0;304.0;321.0;320.0;303.0;302.0;306.0;299.0;300.0;307.0;305.0;311.0;302.0;316.0;299.0;317.0;299.0;308.0;317.0;320.0;346.0;313.0;304.0;312.0;327.0;305.0;306.0;331.0;299.0;333.0;115.0;139.0;143.0,0.177024;0.224075;0.333904;0.393087;0.273532;0.310546;0.299487;0.534991;0.506669;0.43826;0.249826;0.327905;0.340013;0.8118;0.78289;0.585756;0.653071;0.741445;0.826652;0.826652;0.857178;0.797214;0.841777;0.857178;0.905092;0.88883;0.872861;0.921652;0.905092;0.88883;0.872861;0.872861;0.8118;0.728123;0.872861;0.905092;0.857178;0.8118;0.872861;0.826652;0.841777;0.826652;0.857178;0.78289;0.797214;0.8118,0.596473;0.544779;0.488626;0.228175;0.316228;0.506669;0.479847;0.415059;0.372277;0.236601;0.228175;0.346234;0.479847;0.66502;0.534991;0.534991;0.288822;0.554746;0.728123;0.641337;0.728123;0.525379;0.653071;0.575231;0.768824;0.702193;0.618498;0.741445;0.741445;0.689577;0.629814;0.618498;0.430386;0.400278;0.629814;0.75501;0.629814;0.446279;0.641337;0.488626;0.585756;0.454444;0.618498;0.340013;0.454444;0.422653,Longuet-Higgins (1964),NaN\n";
            Test.ensureEqual(results, expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NDBC Server is in flux.", t); 
        }

        try {
            //same test, but with  superfluous string data regex test
            String2.log("\n*** EDDTableFromSOS.testNdbcSosWaves test get one station .CSV data\n");
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&station_id=\"urn:ioos:station:wmo:46013\"&time>=2008-08-01T14&time<=2008-08-01T17" +
//!!!!!! Starting with switch to text/csv 2010-06-07, calculation_method became UNKNOWN!
//but fixed 2010-06-22
                "&calculation_method=~\"(zztop|Long.*)\"", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_ndbcSosWaves", ".csv"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            Test.ensureEqual(results, expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NDBC Server is in flux.", t); 
        }

        try {
            //test error
            String2.log("\n*** EDDTableFromSOS.testNdbcSosWaves test >30 days\n");
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&station_id=\"urn:ioos:station:wmo:46013\"&time>=2008-08-01&time<=2008-09-05", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_ndbcSosWaves30", ".csv"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = "shouldn't get here";
            Test.ensureEqual(results, expected, "RESULTS=\n" + results);
        
        } catch (Throwable t) {
            String msg = MustBe.throwableToString(t);
            expected = "java.lang.RuntimeException: Source Exception=\"" +
                "InvalidParameterValue: eventTime: No more than 31 days of " +
                "data can be requested.\".";
            if (msg.indexOf(expected) < 0) {                    
                throw new RuntimeException("expected=" + expected +
                    "\nUnexpected error ndbcSosWaves. NDBC SOS Server is in flux.", t); 
            }
        }    
    }

    /**
     * This should work, but server is in flux so it often breaks.
     *
     * @throws Throwable if trouble
     */
    public static void testNdbcSosWind(String datasetIdPrefix) throws Throwable {
        testVerboseOn();
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, datasetIdPrefix + "ndbcSosWind");  
        String name, tName, results = null, expected, userDapQuery;

        try { 

            String2.log("\n*** EDDTableFromSOS.testNdbcSosWind .das\n");
            String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
            tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                eddTable.className() + "_ndbcSosWind", ".das"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = 
"Attributes {\n" +
" s {\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -177.738, 180.0;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range -55.0, 71.758;\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  station_id {\n" +
"    String cf_role \"timeseries_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station ID\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    String axis \"Z\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.1360742e+9, NaN;\n" + //pre 2013-11-01 was 1.1540346e+9
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  sensor_id {\n" +
"    String comment \"Always check the quality_flags before using this data.\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Sensor ID\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/winds\";\n" +
"  }\n" +
"  wind_from_direction {\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind From Direction\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/winds\";\n" +
"    String standard_name \"wind_from_direction\";\n" +
"    String units \"degrees_true\";\n" +
"  }\n" +
"  wind_speed {\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind Speed\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/winds\";\n" +
"    String standard_name \"wind_speed\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  wind_speed_of_gust {\n" +
"    Float64 colorBarMaximum 30.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind Speed of Gust\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/winds\";\n" +
"    String standard_name \"wind_speed_of_gust\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  upward_air_velocity {\n" +
"    Float64 colorBarMaximum 10.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Upward Air Velocity\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/winds\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"TimeSeries\";\n" +
"    String cdm_timeseries_variables \"station_id, longitude, latitude, sensor_id\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    Float64 Easternmost_Easting 180.0;\n" +
"    String featureType \"TimeSeries\";\n" +
"    Float64 geospatial_lat_max 71.758;\n" +
"    Float64 geospatial_lat_min -55.0;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 180.0;\n" +
"    Float64 geospatial_lon_min -177.738;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"" + today;
        String tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NDBC Server is in flux.", t); 
        }

        try {
//+ " https://sdf" + datasetIdPrefix + ".ndbc.noaa.gov/sos/server.php\n" +
//today + " " + EDStatic.erddapUrl + //in tests, always use non-https url
//                "/tabledap/" + 
expected = 
datasetIdPrefix + "ndbcSosWind.das\";\n" +
"    String infoUrl \"https://sdf.ndbc.noaa.gov/sos/\";\n" +
"    String institution \"NOAA NDBC\";\n" +
"    String keywords \"air, altitude, atmosphere, atmospheric, direction, Earth Science > Atmosphere > Altitude > Station Height, Earth Science > Atmosphere > Atmospheric Winds > Surface Winds, Earth Science > Atmosphere > Atmospheric Winds > Vertical Wind Motion, from, gust, height, identifier, ndbc, noaa, sensor, sos, speed, station, surface, time, upward, velocity, wind, wind_from_direction, wind_speed, wind_speed_of_gust, winds\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    Float64 Northernmost_Northing 71.758;\n" +
"    String sourceUrl \"https://sdf" + datasetIdPrefix + ".ndbc.noaa.gov/sos/server.php\";\n" +
"    Float64 Southernmost_Northing -55.0;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String subsetVariables \"station_id, longitude, latitude\";\n" +
"    String summary \"The NOAA NDBC SOS server is part of the IOOS DIF SOS Project.  The stations in this dataset have winds data.\n" +
"\n" +
"Because of the nature of SOS requests, requests for data MUST include constraints for the longitude, latitude, time, and/or station_id variables.\";\n" +
"    String time_coverage_start \"2006-01-01T00:10:00Z\";\n" + //pre 2013-11-01 was 2006-07-27T21:10:00Z
"    String title \"NOAA NDBC SOS, 2006-present, winds\";\n" +
"    Float64 Westernmost_Easting -177.738;\n" +
"  }\n" +
"}\n";
            int tPo = results.indexOf(expected.substring(0, 17));
            Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
            Test.ensureEqual(
                results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
                expected, "results=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NDBC Server is in flux.", t); 
        }

        try {

            String2.log("\n*** EDDTableFromSOS.testNdbcSosWind test get one station .CSV data\n");
            tName = eddTable.makeNewFileForDapQuery(null, null, 
                "&station_id=\"urn:ioos:station:wmo:41004\"&time>=2008-08-01T00:00&time<=2008-08-01T04", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "_ndbcSosWind", ".csv"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = //2019-03-19 was alt 5.0, now 4.0
"longitude,latitude,station_id,altitude,time,sensor_id,wind_from_direction,wind_speed,wind_speed_of_gust,upward_air_velocity\n" +
"degrees_east,degrees_north,,m,UTC,,degrees_true,m s-1,m s-1,m s-1\n" +
"-79.099,32.501,urn:ioos:station:wmo:41004,4.0,2008-08-01T00:50:00Z,urn:ioos:sensor:wmo:41004::anemometer1,229.0,10.1,12.6,NaN\n" +
"-79.099,32.501,urn:ioos:station:wmo:41004,4.0,2008-08-01T01:50:00Z,urn:ioos:sensor:wmo:41004::anemometer1,232.0,9.3,11.3,NaN\n" +
"-79.099,32.501,urn:ioos:station:wmo:41004,4.0,2008-08-01T02:50:00Z,urn:ioos:sensor:wmo:41004::anemometer1,237.0,7.8,11.5,NaN\n" +
"-79.099,32.501,urn:ioos:station:wmo:41004,4.0,2008-08-01T03:50:00Z,urn:ioos:sensor:wmo:41004::anemometer1,236.0,8.0,9.3,NaN\n";
            Test.ensureEqual(results, expected, "RESULTS=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. NDBC Server is in flux.", t); 
        }
    }

    /**
     * testOostethys  This tests datasetID=gomoosBuoy.
     *
     * @throws Throwable if trouble
     */
    public static void testOostethys() throws Throwable {
/*
I talked on phone with Eric Bridger (207 228-1662) of gomoos 
(gomoos now gomri and neracoos)
He said 
  Gomoos sos server is the perl Oostethys server 
    and I basically can treat as an oostethys reference implementation
  Luis Bermudez is person most responsible for oostethys,
    which is now unfunded and a volunteer effort
    and main web page is at google code
    http://code.google.com/p/oostethys/ but it links back to 
      http://oostethys.org/ for all but code-related info)
  Oostethys future: may get funding for Q2O project of QARTOD,
    to add quality flags, provenance, etc to SOS
So I will make ERDDAP able to read 
  Goomoos sos server (strict! to comply with ogc tests)
  and neracoos sos server (older version, less strict)
2010-04-29
2012-04-30 Server has been unavailable.  Eric restarted it on new server:
  was http://www.gomoos.org/cgi-bin/sos/V1.0/oostethys_sos.cgi 
  now http://oceandata.gmri.org/cgi-bin/sos/V1.0/oostethys_sos.cgi
*/
        boolean oSosActive = EDStatic.sosActive;
        EDStatic.sosActive = true;
        boolean oDebugMode = debugMode;
        debugMode = true;
        double tLon, tLat;
        String name, tName, results, expected, userDapQuery;
        String error = "";
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, "gomoosBuoy"); //should work

        String2.log("\n*** EDDTableFromSOS.testOostethys");
        testVerboseOn();

        try {
// /*
        //getEmpiricalMinMax just do once
        //useful for SOS: get alt values
        //eddTable.getEmpiricalMinMax("2007-02-01", "2007-02-01", false, true);
        //if (true) System.exit(1);

        //test sos-server values
        String2.log("nOfferings=" + eddTable.sosOfferings.size());
        String2.log(eddTable.sosOfferings.getString(0) + "  lon=" +
            eddTable.sosMinLon.getNiceDouble(0) + ", " +
            eddTable.sosMaxLon.getNiceDouble(0) + " lat=" +
            eddTable.sosMinLat.getNiceDouble(0) + ", " +
            eddTable.sosMaxLat.getNiceDouble(0) + " time=" +
            eddTable.sosMinTime.getNiceDouble(0) + ", " +
            eddTable.sosMaxTime.getNiceDouble(0));
//        String2.log(String2.toCSSVString(eddTable.sosObservedProperties()));

        Test.ensureTrue(eddTable.sosOfferings.size() >= 9, //was 15
            "nOfferings=" + eddTable.sosOfferings.size()); //changes sometimes
        int which = eddTable.sosOfferings.indexOf("A01");
        String2.log("which=" + which);
        Test.ensureEqual(eddTable.sosOfferings.getString(which), "A01", "");
        //2008-07-25 was .5655, pre 2008-10-09 was .5658, 2009-03-23 changed, 
        //pre 2010-07-08 was -70.5600967407227
        //pre 2013-06-28 was -70.5655
        //pre 2013-11-01 was -70.5645150078668
        //give up on =.  Use almostEqual
        Test.ensureAlmostEqual(4, eddTable.sosMinLon.getNiceDouble(which), -70.565, "");  
        Test.ensureAlmostEqual(4, eddTable.sosMaxLon.getNiceDouble(which), -70.565, "");
        //2008-07-25 was 42.5232, pre 2008-10-09 was .5226, 2009-03-23 changed
        //pre 2010-07-08 was 42.5261497497559;
        //pre 2013-06-28 was 42.5232
        //pre 2013-11-01 was 42.5223609076606
        //give up on =.  Use almostEqual
        Test.ensureAlmostEqual(4, eddTable.sosMinLat.getNiceDouble(which), 42.522, "");  
        Test.ensureAlmostEqual(4, eddTable.sosMaxLat.getNiceDouble(which), 42.522, "");
        Test.ensureEqual(eddTable.sosMinTime.getNiceDouble(which), 9.94734E8, "");
        Test.ensureEqual(eddTable.sosMaxTime.getNiceDouble(which), Double.NaN, "");
//        Test.ensureEqual(String2.toCSSVString(eddTable.sosObservedProperties()), 
//            "http://marinemetadata.org/cf#sea_water_salinity, " + //http://marinemetadata.org is GONE!
//            "http://marinemetadata.org/cf#sea_water_temperature", 
//            NOW http://mmisw.org/ont/cf/parameter/sea_water_salinity
//            "");
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. Oostethys Server is in flux.", t); 
        }

        try { 
        userDapQuery = "longitude,latitude,altitude,time,station_id," +
            "sea_water_temperature,sea_water_salinity" +
            "&longitude>=-70&longitude<=-69&latitude>=43&latitude<=44" + 
            "&time>=2007-07-04T00:00&time<=2007-07-04T01:00";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.datasetID() + "_Data", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        expected =
// before 2010-02-11 was
//"longitude,latitude,altitude,time,station_id,sea_water_temperature,sea_water_salinity\n" +
//"degrees_east,degrees_north,m,UTC,,degree_C,PSU\n" +
//"-69.8891420948262,43.7813123586226,-1.0,2007-07-04T00:00:00Z,D01,13.5,29.3161296844482\n" +
//"-69.8891420948262,43.7813123586226,-20.0,2007-07-04T00:00:00Z,D01,9.28999996185303,31.24924659729\n" +
//"-69.8891420948262,43.7813123586226,-35.0,2007-07-04T00:00:00Z,D01,8.30000019073486,NaN\n" +
//"-69.8891420948262,43.7813123586226,-1.0,2007-07-04T01:00:00Z,D01,13.5100002288818,29.192590713501\n" +
//"-69.8891420948262,43.7813123586226,-20.0,2007-07-04T01:00:00Z,D01,9.06999969482422,31.2218112945557\n" +
//"-69.8891420948262,43.7813123586226,-35.0,2007-07-04T01:00:00Z,D01,8.51000022888184,NaN\n" +
//"-69.9878833333333,43.7617166666667,-1.0,2007-07-04T00:00:00Z,D02,13.460000038147,29.9242057800293\n" +
//"-69.9878833333333,43.7617166666667,-10.0,2007-07-04T00:00:00Z,D02,11.8699998855591,31.2601585388184\n" +
//"-69.9878833333333,43.7617166666667,-1.0,2007-07-04T00:30:00Z,D02,13.4200000762939,29.9195117950439\n" +
//"-69.9878833333333,43.7617166666667,-1.0,2007-07-04T01:00:00Z,D02,13.3699998855591,29.930046081543\n" +
//"-69.9878833333333,43.7617166666667,-10.0,2007-07-04T01:00:00Z,D02,11.6599998474121,31.2559909820557\n" +
//"-69.3549346923828,43.7136993408203,-1.0,2007-07-04T00:00:00Z,E01,13.7600002288818,31.1920852661133\n" +
//"-69.3549346923828,43.7136993408203,-2.0,2007-07-04T00:00:00Z,E01,13.7034998,NaN\n" +
//"-69.3549346923828,43.7136993408203,-20.0,2007-07-04T00:00:00Z,E01,7.65000009536743,31.8228702545166\n" +
//"-69.3549346923828,43.7136993408203,-50.0,2007-07-04T00:00:00Z,E01,5.84700012207031,32.1141357421875\n" +
//"-69.3549346923828,43.7136993408203,-1.0,2007-07-04T00:30:00Z,E01,13.8900003433228,31.1868896484375\n" +
//"-69.3549346923828,43.7136993408203,-1.0,2007-07-04T01:00:00Z,E01,13.8500003814697,31.1843872070312\n" +
//"-69.3549346923828,43.7136993408203,-2.0,2007-07-04T01:00:00Z,E01,13.8292704,NaN\n" +
//"-69.3549346923828,43.7136993408203,-20.0,2007-07-04T01:00:00Z,E01,7.57000017166138,31.833927154541\n" +
//"-69.3549346923828,43.7136993408203,-50.0,2007-07-04T01:00:00Z,E01,5.81699991226196,32.0988731384277\n";
//before 12/17/07 was -69.8876
//before 5/19/08 was -69.9879,43.7617,
//changed again 2009-03-26:
//"longitude,latitude,altitude,time,station_id,sea_water_temperature,sea_water_salinity\n" +
//"degrees_east,degrees_north,m,UTC,,degree_C,PSU\n" +
//"-69.9878833333333,43.7617166666667,-1.0,2007-07-04T00:00:00Z,D02,13.460000038147,29.9242057800293\n" +
//"-69.9878833333333,43.7617166666667,-10.0,2007-07-04T00:00:00Z,D02,11.8699998855591,31.2601585388184\n" +
//"-69.9878833333333,43.7617166666667,-1.0,2007-07-04T00:30:00Z,D02,13.4200000762939,29.9195117950439\n" +
//"-69.9878833333333,43.7617166666667,-1.0,2007-07-04T01:00:00Z,D02,13.3699998855591,29.930046081543\n" +
//"-69.9878833333333,43.7617166666667,-10.0,2007-07-04T01:00:00Z,D02,11.6599998474121,31.2559909820557\n" +
//"-69.3549346923828,43.7136993408203,-1.0,2007-07-04T00:00:00Z,E01,13.7600002288818,31.1920852661133\n" +
//"-69.3549346923828,43.7136993408203,-2.0,2007-07-04T00:00:00Z,E01,13.7034998,NaN\n" +
//"-69.3549346923828,43.7136993408203,-20.0,2007-07-04T00:00:00Z,E01,7.65000009536743,31.8228702545166\n" +
//"-69.3549346923828,43.7136993408203,-50.0,2007-07-04T00:00:00Z,E01,5.84700012207031,32.1141357421875\n" +
//"-69.3549346923828,43.7136993408203,-1.0,2007-07-04T00:30:00Z,E01,13.8900003433228,31.1868896484375\n" +
//"-69.3549346923828,43.7136993408203,-1.0,2007-07-04T01:00:00Z,E01,13.8500003814697,31.1843872070312\n" +
//"-69.3549346923828,43.7136993408203,-2.0,2007-07-04T01:00:00Z,E01,13.8292704,NaN\n" +
//"-69.3549346923828,43.7136993408203,-20.0,2007-07-04T01:00:00Z,E01,7.57000017166138,31.833927154541\n" +
//"-69.3549346923828,43.7136993408203,-50.0,2007-07-04T01:00:00Z,E01,5.81699991226196,32.0988731384277\n";
//2010-07-08 When I switch to ALL_PLATFORMS BBOX request,
//  D01 (which is not currently in GetCapabilities) returned to the response
"longitude,latitude,altitude,time,station_id,sea_water_temperature,sea_water_salinity\n" +
"degrees_east,degrees_north,m,UTC,,degree_C,PSU\n" +
"-69.8891420948262,43.7813123586226,-1.0,2007-07-04T00:00:00Z,D01,13.5,29.3161296844482\n" +
"-69.8891420948262,43.7813123586226,-20.0,2007-07-04T00:00:00Z,D01,9.28999996185303,31.24924659729\n" +
"-69.8891420948262,43.7813123586226,-35.0,2007-07-04T00:00:00Z,D01,8.30000019073486,NaN\n" +
"-69.8891420948262,43.7813123586226,-1.0,2007-07-04T01:00:00Z,D01,13.5100002288818,29.192590713501\n" +
"-69.8891420948262,43.7813123586226,-20.0,2007-07-04T01:00:00Z,D01,9.06999969482422,31.2218112945557\n" +
"-69.8891420948262,43.7813123586226,-35.0,2007-07-04T01:00:00Z,D01,8.51000022888184,NaN\n" +
"-69.9878833333333,43.7617166666667,-1.0,2007-07-04T00:00:00Z,D02,13.460000038147,29.9242057800293\n" +
"-69.9878833333333,43.7617166666667,-10.0,2007-07-04T00:00:00Z,D02,11.8699998855591,31.2601585388184\n" +
"-69.9878833333333,43.7617166666667,-1.0,2007-07-04T00:30:00Z,D02,13.4200000762939,29.9195117950439\n" +
"-69.9878833333333,43.7617166666667,-1.0,2007-07-04T01:00:00Z,D02,13.3699998855591,29.930046081543\n" +
"-69.9878833333333,43.7617166666667,-10.0,2007-07-04T01:00:00Z,D02,11.6599998474121,31.2559909820557\n" +
"-69.3549346923828,43.7136993408203,-1.0,2007-07-04T00:00:00Z,E01,13.7600002288818,31.1920852661133\n" +
"-69.3549346923828,43.7136993408203,-2.0,2007-07-04T00:00:00Z,E01,13.7034998,NaN\n" +
"-69.3549346923828,43.7136993408203,-20.0,2007-07-04T00:00:00Z,E01,7.65000009536743,31.8228702545166\n" +
"-69.3549346923828,43.7136993408203,-50.0,2007-07-04T00:00:00Z,E01,5.84700012207031,32.1141357421875\n" +
"-69.3549346923828,43.7136993408203,-1.0,2007-07-04T00:30:00Z,E01,13.8900003433228,31.1868896484375\n" +
"-69.3549346923828,43.7136993408203,-1.0,2007-07-04T01:00:00Z,E01,13.8500003814697,31.1843872070312\n" +
"-69.3549346923828,43.7136993408203,-2.0,2007-07-04T01:00:00Z,E01,13.8292704,NaN\n" +
"-69.3549346923828,43.7136993408203,-20.0,2007-07-04T01:00:00Z,E01,7.57000017166138,31.833927154541\n" +
"-69.3549346923828,43.7136993408203,-50.0,2007-07-04T01:00:00Z,E01,5.81699991226196,32.0988731384277\n";
    Test.ensureEqual(results, expected, results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. Oostethys Server is in flux.", t); 
        }

        try {
        //data for mapExample  (no time)  just uses station table data
        tName = eddTable.makeNewFileForDapQuery(null, null, "longitude,latitude&longitude>-70", 
            EDStatic.fullTestCacheDirectory, eddTable.className() + "MapNT", ".csv");
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = //this changes a little periodically (NOT GOOD, stations have 1 point (which applies to historic data and changes!)
//before 2009-01-12 this was
//"longitude,latitude\n" +
//"degrees_east,degrees_north\n" +
//"-69.8891,43.7813\n" +
//"-69.9885,43.7612\n" +
//"-69.3578,43.7148\n" +
//"-68.998,44.0548\n" +
//"-68.1087,44.1058\n" +
//"-67.0123,44.8893\n" +
//"-66.5541,43.6276\n" +
//"-67.8798,43.4907\n" +
//"-65.9118,42.3279\n";
//before 2009-03-23 this was
//"longitude,latitude\n" +
//"degrees_east,degrees_north\n" +
//"-69.8891,43.7813\n" +
//"-69.9885,43.7612\n" +
//"-69.3578,43.7148\n" +
//"-68.998,44.0548\n" +
//"-68.1085,44.1057\n" +
//"-67.0123,44.8893\n" +
//"-66.5541,43.6276\n" +
//"-67.8798,43.4907\n" +
//"-65.9118,42.3279\n"; 
//before 2009-03-26 was
//"longitude,latitude\n" +
//"degrees_east,degrees_north\n" +
//"-69.8891,43.7813\n" +
//"-69.9885,43.7612\n" +
//"-69.3578,43.7148\n" +
//"-68.998,44.0548\n" +
//"-68.108,44.1052\n" +
//"-67.0123,44.8893\n" +
//"-66.5541,43.6276\n" +
//"-67.8798,43.4907\n" +
//"-65.9118,42.3279\n";
//"longitude,latitude\n" +
//"degrees_east,degrees_north\n" +
//"-69.8891,43.7813\n" +
//"-69.9877,43.7628\n" +
//"-69.3578,43.7148\n" +
//"-68.9982,44.0555\n" +
//"-68.108,44.1052\n" +
//"-67.0123,44.8893\n" +
//"-66.5541,43.6276\n" +
//"-67.8798,43.4907\n" +
//"-65.9118,42.3279\n";
//"longitude,latitude\n" +
//"degrees_east,degrees_north\n" +
//"-69.8891,43.7813\n" +
//"-69.9877,43.7628\n" +
//"-69.3578,43.7148\n" +
//"-68.9982,44.0555\n" +
//"-68.108,44.1052\n" +
//"-67.0123,44.8893\n" +
//"-66.5541,43.6276\n" +
//"-67.8798,43.4907\n" +
//"-65.9096,42.3276\n";
//"longitude,latitude\n" +
//"degrees_east,degrees_north\n" +
//"-69.8891,43.7813\n" +
//"-69.9877,43.7628\n" +
//"-69.3578,43.7148\n" +
//"-68.9982,44.0555\n" +
//"-68.1087,44.1058\n" +
//"-67.0123,44.8893\n" +
//"-66.5541,43.6276\n" +
//"-67.8798,43.4907\n" +
//"-65.9096,42.3276\n";
//"longitude,latitude\n" +  //starting 2009-09-28
//"degrees_east,degrees_north\n" +
//"-69.8891,43.7813\n" +
//"-69.9877,43.7628\n" +
//"-69.3578,43.7148\n" +
//"-68.9982,44.0555\n" +
//"-68.1087,44.1058\n" +
//"-67.0122575759888,44.8892910480499\n" +
//"-66.5541088046873,43.6276378712505\n" +
//"-67.8798,43.4907\n" +
//"-65.9066314697266,42.3259010314941\n";
//"longitude,latitude\n" +  //starting 2010-02-11
//"degrees_east,degrees_north\n" +
//"-69.9877,43.7628\n" +
//"-69.3578,43.7148\n" +
//"-68.9982,44.0555\n" +
//"-68.1087,44.1058\n" +
//"-67.8798,43.4907\n" +
//"-65.9066314697266,42.3259010314941\n";
//"longitude,latitude\n" + //starting 2010-07-08
//"degrees_east,degrees_north\n" +
//"-69.9877,43.7628\n" +
//"-69.3578,43.7148\n" +
//"-69.32,43.7065\n" + //added 2010-10-01
//"-68.9982,44.0555\n" +
//"-68.1087,44.1058\n" +
//"-67.8716659545898,43.490140914917\n" +  //small lat lon changes 2010-08-05
//"-65.9081802368164,42.3263664245605\n";
//"longitude,latitude\n" + //starting 2011-02-15
//"degrees_east,degrees_north\n" +
//"-69.9877,43.7628\n" +
//"-69.3578,43.7148\n" +
//"-69.319580078125,43.7063484191895\n" +
//"-68.9977645874023,44.0548324584961\n" +
//"-68.1087,44.1058\n" +
//"-67.8716659545898,43.490140914917\n" +
//"-65.9081802368164,42.3263664245605\n";
//"longitude,latitude\n" +  //starting 2011-07-24
//"degrees_east,degrees_north\n" +
//"-69.9877,43.7628\n" +
//"-69.3552169799805,43.714298248291\n" +
//"-69.319580078125,43.7063484191895\n" +
//"-68.9977645874023,44.0548324584961\n" +
//"-68.1087,44.1058\n" +
//"-67.8798,43.4907\n" +
//"-65.907,42.3303\n";   //pre 2011-09-05 was "-65.9081802368164,42.3263664245605\n";
//"longitude,latitude\n" + //starting 2012-03-16
//"degrees_east,degrees_north\n" +
//"-69.9877,43.7628\n" +
//"-69.3578,43.7148\n" +
//"-69.319580078125,43.7063484191895\n" +
//"-68.9982,44.0555\n" +
//"-68.996696472168,44.0559844970703\n" + //pre 2013-06-28 was "-68.9982,44.0555\n" + //pre 2012-06-18 was -68.9981,44.055
//"-68.109302520752,44.1061248779297\n" + //pre 2013-06-28 was "-68.1090882815856,44.105949650925\n" + //pre 2012-11-02 was -68.1087,44.1058
//"-67.8798,43.4907\n"
"longitude,latitude\n" +  //starting 2014-08-11
"degrees_east,degrees_north\n" +
"-69.9877,43.7628\n" +
"-69.3541278839111,43.7149534225464\n" + //2020-10-05 was -69.3578,43.7148, 2015-07-31 was -69.3550033569336,43.7163009643555\n" + //2014-09-22 was -69.3578,43.7148\n" +
"-69.319580078125,43.7063484191895\n" +
"-68.9982,44.0555\n" +
"-68.8249619164502,44.3871537467378\n" + //2020-10-05 was -68.82308,44.3878  2020-05-06 was "-68.8249619164502,44.3871537467378\n" +
"-68.1087,44.1058\n" + //2020-10-05 was -68.1121,44.1028 2015-07-31 was -68.1084442138672,44.1057103474935\n" +
"-67.8798,43.4907\n" +
"-65.907,42.3303\n"; //2014-09-22 was -65.9061666666667,42.3336666666667\n";
//"-65.9069544474284,42.3313840230306\n"; //pre 2013-06-28 was "-65.907,42.3303\n"; 
       Test.ensureEqual(results, expected, "\nresults=\n" + results);  

        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. Oostethys Server is in flux.", t); 
        }

        try {
        //data for mapExample (with time
        tName = eddTable.makeNewFileForDapQuery(null, null, "longitude,latitude&time=2007-12-11", 
            EDStatic.fullTestCacheDirectory, eddTable.className() + "MapWT", ".csv");
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
//changed a little on 2008-02-28, 2008-05-19, 2008-09-24, 2008-10-09 2009-01-12   -70.5658->-70.5655 42.5226->42.5232
//change a little on 2009-03-26, 2009-09-18
//"longitude,latitude\n" +
//"degrees_east,degrees_north\n" +
//"-70.5652267750436,42.5227725835475\n" +
//"-70.4273380011109,43.18053298369\n" +
//"-70.0584772405338,43.5671424043496\n" +
//"-69.8876,43.7823\n" +
//"-69.9878833333333,43.7617166666667\n" +
//"-69.3563516790217,43.7138879949396\n" +
//"-68.9979781363549,44.0556086654547\n" +
//"-68.1087458631928,44.1053852961787\n" +
//"-67.0122575759888,44.8892910480499\n";
//starting 2010-02-11:
//"longitude,latitude\n" +  
//"degrees_east,degrees_north\n" +
//"-70.5652267750436,42.5227725835475\n" +
//"-70.4273380011109,43.18053298369\n" +
//"-69.9878833333333,43.7617166666667\n" +
//"-69.3563516790217,43.7138879949396\n" +
//"-68.9979781363549,44.0556086654547\n" +
//"-68.1087458631928,44.1053852961787\n";
//starting 2010-07-08 with new ALL_PLATFORMS BBOX request,  D01 returns:
"longitude,latitude\n" +
"degrees_east,degrees_north\n" +
"-70.5652267750436,42.5227725835475\n" +
"-70.4273380011109,43.18053298369\n" +
"-70.0584772405338,43.5671424043496\n" +
"-69.8876,43.7823\n" +
"-69.9878833333333,43.7617166666667\n" +
"-69.3563516790217,43.7138879949396\n" +
"-68.9979781363549,44.0556086654547\n" +
"-68.1087458631928,44.1053852961787\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);  
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. Oostethys Server is in flux.", t); 
        }

        try {
        //data for all variables 
        tName = eddTable.makeNewFileForDapQuery(null, null, "&station_id=\"A01\"&time=2007-12-11", 
            EDStatic.fullTestCacheDirectory, eddTable.className() + "MapWTAV", ".csv");
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = //changed a little on 2008-02-28, 2008-05-19, 2008-09-24, 2008-10-09 2009-01-12   -70.5658->-70.5655 42.5226->42.5232
           //change a little on 2009-03-26, 2009-09-18
"longitude,latitude,station_id,altitude,time,air_temperature,chlorophyll,direction_of_sea_water_velocity,dominant_wave_period,sea_level_pressure,sea_water_density,sea_water_electrical_conductivity,sea_water_salinity,sea_water_speed,sea_water_temperature,wave_height,visibility_in_air,wind_from_direction,wind_gust,wind_speed\n" +
"degrees_east,degrees_north,,m,UTC,degree_C,mg m-3,degrees_true,s,hPa,kg m-3,S m-1,PSU,cm s-1,degree_C,m,m,degrees_true,m s-1,m s-1\n" +
"-70.5652267750436,42.5227725835475,A01,4.0,2007-12-11T00:00:00Z,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,332.100006103516,3.94600009918213,3.36700010299683\n" +
"-70.5652267750436,42.5227725835475,A01,3.0,2007-12-11T00:00:00Z,0.899999976158142,NaN,NaN,NaN,1024.82849121094,NaN,NaN,NaN,NaN,NaN,NaN,2920.6796875,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,0.0,2007-12-11T00:00:00Z,NaN,NaN,NaN,5.33333349,NaN,NaN,NaN,NaN,NaN,NaN,0.644578338,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-1.0,2007-12-11T00:00:00Z,NaN,NaN,NaN,NaN,NaN,25.3888969421387,33.2410011291504,32.4736976623535,NaN,7.30000019073486,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-2.0,2007-12-11T00:00:00Z,NaN,NaN,174.5672,NaN,NaN,NaN,NaN,NaN,9.38560009,7.34996223,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-3.0,2007-12-11T00:00:00Z,NaN,0.965432941913605,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-10.0,2007-12-11T00:00:00Z,NaN,NaN,182.0,NaN,NaN,NaN,NaN,NaN,4.016217,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-14.0,2007-12-11T00:00:00Z,NaN,NaN,197.0,NaN,NaN,NaN,NaN,NaN,3.605551,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-18.0,2007-12-11T00:00:00Z,NaN,NaN,212.0,NaN,NaN,NaN,NaN,NaN,3.471311,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-20.0,2007-12-11T00:00:00Z,NaN,NaN,NaN,NaN,NaN,25.3971004486084,33.2999992370605,32.4910888671875,NaN,7.34000015258789,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-22.0,2007-12-11T00:00:00Z,NaN,NaN,193.0,NaN,NaN,NaN,NaN,NaN,3.671512,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-26.0,2007-12-11T00:00:00Z,NaN,NaN,192.0,NaN,NaN,NaN,NaN,NaN,2.505993,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-30.0,2007-12-11T00:00:00Z,NaN,NaN,207.0,NaN,NaN,NaN,NaN,NaN,2.475884,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-34.0,2007-12-11T00:00:00Z,NaN,NaN,189.0,NaN,NaN,NaN,NaN,NaN,3.534119,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-38.0,2007-12-11T00:00:00Z,NaN,NaN,173.0,NaN,NaN,NaN,NaN,NaN,4.356604,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-42.0,2007-12-11T00:00:00Z,NaN,NaN,185.0,NaN,NaN,NaN,NaN,NaN,4.846648,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-46.0,2007-12-11T00:00:00Z,NaN,NaN,157.0,NaN,NaN,NaN,NaN,NaN,4.527693,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-50.0,2007-12-11T00:00:00Z,NaN,NaN,174.0,NaN,NaN,25.400972366333,33.2970008850098,32.4925308227539,3.255764,7.32000017166138,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-54.0,2007-12-11T00:00:00Z,NaN,NaN,220.0,NaN,NaN,NaN,NaN,NaN,0.72111,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-58.0,2007-12-11T00:00:00Z,NaN,NaN,323.0,NaN,NaN,NaN,NaN,NaN,3.956008,NaN,NaN,NaN,NaN,NaN,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);  

        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. Oostethys Server is in flux.", t); 
        } finally {
            EDStatic.sosActive = oSosActive;
            debugMode = oDebugMode;
        }
    }

    /**
     * testNeracoos should work.
     *
     * @throws Throwable if trouble
     */
    public static void testNeracoos() throws Throwable {
        String2.log("\n*** EDDTableFromSOS.testNeracoos");
        testVerboseOn();
        boolean oSosActive = EDStatic.sosActive;
        EDStatic.sosActive = true;
        boolean oDebugMode = debugMode;
        debugMode = true;
        double tLon, tLat;
        String name, tName, results, expected, userDapQuery;
        String error = "";
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, "neracoosSos"); //should work

        //getEmpiricalMinMax just do once
        //useful for SOS: get alt values
        //eddTable.getEmpiricalMinMax("2007-02-01", "2007-02-01", false, true);
        //if (true) System.exit(1);

        try {

        //test sos-server values
        String2.log("nOfferings=" + eddTable.sosOfferings.size());
        String2.log(eddTable.sosOfferings.getString(0) + "  lon=" +
            eddTable.sosMinLon.getNiceDouble(0) + ", " +
            eddTable.sosMaxLon.getNiceDouble(0) + " lat=" +
            eddTable.sosMinLat.getNiceDouble(0) + ", " +
            eddTable.sosMaxLat.getNiceDouble(0) + " time=" +
            eddTable.sosMinTime.getNiceDouble(0) + ", " +
            eddTable.sosMaxTime.getNiceDouble(0));
//        String2.log(String2.toCSSVString(eddTable.sosObservedProperties()));

        Test.ensureTrue(eddTable.sosOfferings.size() >= 9, //was 15
            "nOfferings=" + eddTable.sosOfferings.size()); //changes sometimes
        int which = eddTable.sosOfferings.indexOf("A01");
        String2.log("which=" + which);
        Test.ensureEqual(eddTable.sosOfferings.getString(which), "A01", "");
        //pre 2010-06-22 was -70.5600967407227
        //pre 2013-06-28 was -70.5655, ""); 
        //pre 2013-11-01 was -70.5645150078668
        //give up on =.  Use almostEqual
        Test.ensureAlmostEqual(4, eddTable.sosMinLon.getNiceDouble(which), -70.565, "");  
        Test.ensureAlmostEqual(4, eddTable.sosMaxLon.getNiceDouble(which), -70.565, "");
        //pre 2010-06-10 was 42.5261497497559
        //pre 2013-06-28 was 42.5232, ""); 
        //pre 2013-11-01 was 42.5223609076606
        //give up on =.  Use almostEqual
        Test.ensureAlmostEqual(4, eddTable.sosMinLat.getNiceDouble(which), 42.522, "");  
        Test.ensureAlmostEqual(4, eddTable.sosMaxLat.getNiceDouble(which), 42.522, "");

        Test.ensureEqual(eddTable.sosMinTime.getNiceDouble(which), 9.94734E8, "");
        Test.ensureEqual(eddTable.sosMaxTime.getNiceDouble(which), Double.NaN, "");
//        Test.ensureEqual(String2.toCSSVString(eddTable.sosObservedProperties()), 
//            "http://marinemetadata.org/cf#sea_water_salinity, " + //http://marinemetadata.org is GONE!
//            "http://marinemetadata.org/cf#sea_water_temperature", 
//            NOW http://mmisw.org/ont/cf/parameter/sea_water_salinity
//            "");
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. Neracoos Server is in flux.", t); 
        }


        try {
        userDapQuery = "longitude,latitude,altitude,time,station_id," +
            "sea_water_temperature,sea_water_salinity" +
            "&longitude>=-70&longitude<=-69&latitude>=43&latitude<=44" + 
            "&time>=2007-07-04T00:00&time<=2007-07-04T01:00";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.datasetID() + "_Data", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        expected =
//before 2010-07-08 when I started using ALL_PLATFORMS and BBOX,
//  there was no data for D01 in the response
"longitude,latitude,altitude,time,station_id,sea_water_temperature,sea_water_salinity\n" +
"degrees_east,degrees_north,m,UTC,,degree_C,PSU\n" +
"-69.8891420948262,43.7813123586226,-1.0,2007-07-04T00:00:00Z,D01,13.5,29.3161296844482\n" +
"-69.8891420948262,43.7813123586226,-20.0,2007-07-04T00:00:00Z,D01,9.28999996185303,31.24924659729\n" +
"-69.8891420948262,43.7813123586226,-35.0,2007-07-04T00:00:00Z,D01,8.30000019073486,NaN\n" +
"-69.8891420948262,43.7813123586226,-1.0,2007-07-04T01:00:00Z,D01,13.5100002288818,29.192590713501\n" +
"-69.8891420948262,43.7813123586226,-20.0,2007-07-04T01:00:00Z,D01,9.06999969482422,31.2218112945557\n" +
"-69.8891420948262,43.7813123586226,-35.0,2007-07-04T01:00:00Z,D01,8.51000022888184,NaN\n" +
"-69.9878833333333,43.7617166666667,-1.0,2007-07-04T00:00:00Z,D02,13.460000038147,29.9242057800293\n" +
"-69.9878833333333,43.7617166666667,-10.0,2007-07-04T00:00:00Z,D02,11.8699998855591,31.2601585388184\n" +
"-69.9878833333333,43.7617166666667,-1.0,2007-07-04T00:30:00Z,D02,13.4200000762939,29.9195117950439\n" +
"-69.9878833333333,43.7617166666667,-1.0,2007-07-04T01:00:00Z,D02,13.3699998855591,29.930046081543\n" +
"-69.9878833333333,43.7617166666667,-10.0,2007-07-04T01:00:00Z,D02,11.6599998474121,31.2559909820557\n" +
"-69.3549346923828,43.7136993408203,-1.0,2007-07-04T00:00:00Z,E01,13.7600002288818,31.1920852661133\n" +
"-69.3549346923828,43.7136993408203,-2.0,2007-07-04T00:00:00Z,E01,13.7034998,NaN\n" +
"-69.3549346923828,43.7136993408203,-20.0,2007-07-04T00:00:00Z,E01,7.65000009536743,31.8228702545166\n" +
"-69.3549346923828,43.7136993408203,-50.0,2007-07-04T00:00:00Z,E01,5.84700012207031,32.1141357421875\n" +
"-69.3549346923828,43.7136993408203,-1.0,2007-07-04T00:30:00Z,E01,13.8900003433228,31.1868896484375\n" +
"-69.3549346923828,43.7136993408203,-1.0,2007-07-04T01:00:00Z,E01,13.8500003814697,31.1843872070312\n" +
"-69.3549346923828,43.7136993408203,-2.0,2007-07-04T01:00:00Z,E01,13.8292704,NaN\n" +
"-69.3549346923828,43.7136993408203,-20.0,2007-07-04T01:00:00Z,E01,7.57000017166138,31.833927154541\n" +
"-69.3549346923828,43.7136993408203,-50.0,2007-07-04T01:00:00Z,E01,5.81699991226196,32.0988731384277\n";
        Test.ensureEqual(results, expected, results);
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. Neracoos Server is in flux.", t); 
        }

        try {
        //data for mapExample  (no time)  just uses station table data
        tName = eddTable.makeNewFileForDapQuery(null, null, "longitude,latitude,station_id&longitude>-70", 
            EDStatic.fullTestCacheDirectory, eddTable.className() + "NeraNT", ".csv");
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
//"longitude,latitude,station_id\n" +
//"degrees_east,degrees_north,\n" +
//"-69.9877,43.7628,D02\n" +
//"-69.3578,43.7148,E01\n" +
//"-69.32,43.7065,E02\n" + //added 2010-10-01
//"-68.9982,44.0555,F01\n" +
//"-68.1087,44.1058,I01\n" +
//"-67.8716659545898,43.490140914917,M01\n" + //pre 2010-06-22 was "-67.8798,43.4907,M01\n" +
//"-65.9081802368164,42.3263664245605,N01\n";  //small changes 2010-08-05
//starting 2011-02-15
//"longitude,latitude,station_id\n" +
//"degrees_east,degrees_north,\n" +
//"-69.9877,43.7628,D02\n" +
//"-69.3578,43.7148,E01\n" +
//"-69.319580078125,43.7063484191895,E02\n" +
//"-68.9977645874023,44.0548324584961,F01\n" +
//"-68.1087,44.1058,I01\n" +
//"-67.8716659545898,43.490140914917,M01\n" +
//"-65.9081802368164,42.3263664245605,N01\n";
//"longitude,latitude,station_id\n" + //starting on 2011-07-24
//"degrees_east,degrees_north,\n" +
//"-69.9877,43.7628,D02\n" +
//"-69.3552169799805,43.714298248291,E01\n" +
//"-69.319580078125,43.7063484191895,E02\n" +
//"-68.9977645874023,44.0548324584961,F01\n" +
//"-68.1087,44.1058,I01\n" +
//"-67.8798,43.4907,M01\n" +
//"-65.907,42.3303,N01\n";  //pre 2011-09-05 was "-65.9081802368164,42.3263664245605,N01\n";
//"longitude,latitude,station_id\n" +  //starting 2011-12-16
//"degrees_east,degrees_north,\n" +
//"-63.4082,44.5001,CDIP176\n" +
//"-69.9877,43.7628,D02\n" +
//"-69.3552169799805,43.714298248291,E01\n" +
//"-69.319580078125,43.7063484191895,E02\n" +
//"-68.9977645874023,44.0548324584961,F01\n" +
//"-68.1087,44.1058,I01\n" +
//"-67.8798,43.4907,M01\n" +
//"-65.907,42.3303,N01\n" +
//"-54.688,46.9813,SMB-MO-01\n" +
//"-54.1317,47.3255,SMB-MO-04\n" +
//"-54.0488,47.7893,SMB-MO-05\n";
"longitude,latitude,station_id\n" + 
"degrees_east,degrees_north,\n" +
"-63.\\d+,44.\\d+,CDIP176\n" +
"-69.\\d+,43.\\d+,D02\n" +
"-69.\\d+,43.\\d+,E01\n" +
"-69.\\d+,43.\\d+,E02\n" +
"-68.\\d+,44.\\d+,F01\n" + 
"-68.\\d+,44.\\d+,F02\n" +
"-68.\\d+,44.\\d+,I01\n" +
"-67.\\d+,43.\\d+,M01\n" +
"-65.\\d+,42.\\d+,N01\n" + 
"-54.\\d+,46.\\d+,SMB-MO-01\n" +
"-54.\\d+,47.\\d+,SMB-MO-04\n" +
"-54.\\d+,47.\\d+,SMB-MO-05\n"; 
        Test.testLinesMatch(results, expected, "\nresults=\n" + results);  
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. Neracoos Server is in flux.", t); 
        }


        //data for mapExample (with time
        try {
        tName = eddTable.makeNewFileForDapQuery(null, null, "longitude,latitude&time=2007-12-11", 
            EDStatic.fullTestCacheDirectory, eddTable.className() + "NeraWT", ".csv");
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
//before 2010-07-08 when I started using ALL_PLATFORMS and BBOX,
//  there was no data for D01 (-69.8876, 43.7823) in the response
"longitude,latitude\n" +
"degrees_east,degrees_north\n" +
"-70.5652267750436,42.5227725835475\n" +
"-70.4273380011109,43.18053298369\n" +
"-70.0584772405338,43.5671424043496\n" +
"-69.8876,43.7823\n" +
"-69.9878833333333,43.7617166666667\n" +
"-69.3563516790217,43.7138879949396\n" +
"-68.9979781363549,44.0556086654547\n" +
"-68.1087458631928,44.1053852961787\n" +
"-67.0122575759888,44.8892910480499\n"; //this line added 2011-12-16
        Test.ensureEqual(results, expected, "\nresults=\n" + results);  
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. Neracoos Server is in flux.", t); 
        }

        //data for all variables 
        try{
        tName = eddTable.makeNewFileForDapQuery(null, null, "&station_id=\"A01\"&time=2007-12-11", 
            EDStatic.fullTestCacheDirectory, eddTable.className() + "neraAV", ".csv");
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"longitude,latitude,station_id,altitude,time,air_temperature,chlorophyll,direction_of_sea_water_velocity,dominant_wave_period,sea_level_pressure,sea_water_density,sea_water_electrical_conductivity,sea_water_salinity,sea_water_speed,sea_water_temperature,wave_height,visibility_in_air,wind_from_direction,wind_gust,wind_speed\n" +
"degrees_east,degrees_north,,m,UTC,degree_C,mg m-3,degrees_true,s,hPa,kg m-3,S m-1,PSU,cm s-1,degree_C,m,m,degrees_true,m s-1,m s-1\n" +
"-70.5652267750436,42.5227725835475,A01,4.0,2007-12-11T00:00:00Z,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,332.100006103516,3.94600009918213,3.36700010299683\n" +
"-70.5652267750436,42.5227725835475,A01,3.0,2007-12-11T00:00:00Z,0.899999976158142,NaN,NaN,NaN,1024.82849121094,NaN,NaN,NaN,NaN,NaN,NaN,2920.6796875,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,0.0,2007-12-11T00:00:00Z,NaN,NaN,NaN,5.33333349,NaN,NaN,NaN,NaN,NaN,NaN,0.644578338,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-1.0,2007-12-11T00:00:00Z,NaN,NaN,NaN,NaN,NaN,25.3888969421387,33.2410011291504,32.4736976623535,NaN,7.30000019073486,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-2.0,2007-12-11T00:00:00Z,NaN,NaN,174.5672,NaN,NaN,NaN,NaN,NaN,9.38560009,7.34996223,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-3.0,2007-12-11T00:00:00Z,NaN,0.965432941913605,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-10.0,2007-12-11T00:00:00Z,NaN,NaN,182.0,NaN,NaN,NaN,NaN,NaN,4.016217,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-14.0,2007-12-11T00:00:00Z,NaN,NaN,197.0,NaN,NaN,NaN,NaN,NaN,3.605551,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-18.0,2007-12-11T00:00:00Z,NaN,NaN,212.0,NaN,NaN,NaN,NaN,NaN,3.471311,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-20.0,2007-12-11T00:00:00Z,NaN,NaN,NaN,NaN,NaN,25.3971004486084,33.2999992370605,32.4910888671875,NaN,7.34000015258789,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-22.0,2007-12-11T00:00:00Z,NaN,NaN,193.0,NaN,NaN,NaN,NaN,NaN,3.671512,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-26.0,2007-12-11T00:00:00Z,NaN,NaN,192.0,NaN,NaN,NaN,NaN,NaN,2.505993,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-30.0,2007-12-11T00:00:00Z,NaN,NaN,207.0,NaN,NaN,NaN,NaN,NaN,2.475884,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-34.0,2007-12-11T00:00:00Z,NaN,NaN,189.0,NaN,NaN,NaN,NaN,NaN,3.534119,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-38.0,2007-12-11T00:00:00Z,NaN,NaN,173.0,NaN,NaN,NaN,NaN,NaN,4.356604,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-42.0,2007-12-11T00:00:00Z,NaN,NaN,185.0,NaN,NaN,NaN,NaN,NaN,4.846648,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-46.0,2007-12-11T00:00:00Z,NaN,NaN,157.0,NaN,NaN,NaN,NaN,NaN,4.527693,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-50.0,2007-12-11T00:00:00Z,NaN,NaN,174.0,NaN,NaN,25.400972366333,33.2970008850098,32.4925308227539,3.255764,7.32000017166138,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-54.0,2007-12-11T00:00:00Z,NaN,NaN,220.0,NaN,NaN,NaN,NaN,NaN,0.72111,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"-70.5652267750436,42.5227725835475,A01,-58.0,2007-12-11T00:00:00Z,NaN,NaN,323.0,NaN,NaN,NaN,NaN,NaN,3.956008,NaN,NaN,NaN,NaN,NaN,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);  
        } catch (Throwable t) {
            throw new RuntimeException("Small changes are common. Neracoos Server is in flux.", t); 
        } finally {
            EDStatic.sosActive = oSosActive;
            debugMode = oDebugMode;
        }        
    }


    /**
     * testTamu doesn't work yet (as of 2009-09-30).
     * It doesn't support eventTime.
     * It requires RESPONSE_FORMAT instead of responseFormat.
     * I sent email to tcook@nsstc.uah.edu .
     *
     * @throws Throwable if trouble
     */
    public static void testTamu() throws Throwable {
        String2.log("\n*** EDDTableFromSOS.testTamu");
        testVerboseOn();
        boolean oSosActive = EDStatic.sosActive;
        EDStatic.sosActive = true;
        boolean oDebugMode = debugMode;
        debugMode = true;
        try {
            String name, tName, results, expected, userDapQuery;
            String error = "";
            String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

            EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, "tamuSos"); 

            //data for all variables 
            tName = eddTable.makeNewFileForDapQuery(null, null, "&time=2009-07-11", //&station_id=\"A01\"", 
                EDStatic.fullTestCacheDirectory, eddTable.className() + "tamu", ".csv");
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            //String2.log(results);
            expected = 
    "zztop\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);  

        } finally {
            EDStatic.sosActive = oSosActive;
            debugMode = oDebugMode;
        }

    }


    /** 
     * NOT FINISHED. But useful for what it does so far.
     * <br>This generates a rough draft of the datasets.xml entry for an EDDTableFromSOS.
     * <br>The XML can then be edited by hand and added to the datasets.xml file.
     *
     * @param useCachedInfo for testing purposes, this uses a cached GetCapabilities
     *   document if possible (else caches what it gets)
     * @param tLocalSourceUrl
     * @param sosVersion e.g., 1.0.0 (or null or "")
     * @param sosServerType may be "", or one of  IOOS_52N, IOOS_NcSOS, 
     *   IOOS_NDBC, IOOS_NOS, OOSTethys, WHOI  (case insensitive)
     * @return a suggested chunk of xml for this dataset for use in datasets.xml 
     * @throws Throwable if trouble, e.g., if no Grid or Array variables are found.
     *    If no trouble, then a valid dataset.xml chunk has been returned.
     */
    public static String generateDatasetsXml(boolean useCachedInfo,
        String tLocalSourceUrl, String sosVersion, 
        String sosServerType) throws Throwable {

        tLocalSourceUrl = EDStatic.updateUrls(tLocalSourceUrl); //http: to https:
        String2.log("\n*** EDDTableFromSos.generateDatasetsXml" +
            "\nuseCachedInfo=" + useCachedInfo + " localSourceUrl=" + tLocalSourceUrl +
            "\nsosVersion=" + sosVersion + " sosServerType=" + sosServerType);
        StringBuilder sb = new StringBuilder();
        String tPublicSourceUrl = convertToPublicSourceUrl(tLocalSourceUrl);
        sosServerType = sosServerType == null? "" : sosServerType.trim();
        String sstlc = sosServerType.toLowerCase();
        boolean ioos52NServer   = sstlc.equals(SosServerTypeIoos52N.toLowerCase()); 
        boolean ioosNdbcServer  = sstlc.equals(SosServerTypeIoosNdbc.toLowerCase());
        boolean ioosNcSOSServer = sstlc.equals(SosServerTypeIoosNcSOS.toLowerCase());
        boolean ioosNosServer   = sstlc.equals(SosServerTypeIoosNos.toLowerCase());
        boolean oostethysServer = sstlc.equals(SosServerTypeOostethys.toLowerCase());
        boolean whoiServer      = sstlc.equals(SosServerTypeWhoi.toLowerCase());  
        
        String tUrl = tLocalSourceUrl + "?service=SOS&request=GetCapabilities";
        if (sosVersion != null && sosVersion.length() > 0) {
            //if (ioosNcbcServer || ioosNosServer) //not needed. 1.0.0 is default and only.
            //    tUrl += "&version=" + sosVersion;
            if (ioos52NServer) //AcceptVersions needed. This class only supports 1.0.0 currently.
                tUrl += "&AcceptVersions=" + sosVersion;
        }
        //if (reallyVerbose) String2.log(SSR.getUrlResponseStringUnchanged(tUrl));
        File2.makeDirectory(sosCopyDir);
        String safeFileName = sosCopyDir + String2.md5Hex12(tUrl);
        if (!useCachedInfo || !File2.isFile(safeFileName)) {
            String2.log("downloading GetCapabilities" +
                "\n  from: " + tUrl +
                "\n  to: " + safeFileName);
            SSR.downloadFile(tUrl, safeFileName, false); //tryToUseCompression
        } else {
            String2.log("using cached GetCapabilities" +
                "\n  from file: " + safeFileName + 
                "\n  from URL: " + tUrl);
        }

        //gather station info and uniqueObservedProperties
        String sosPrefix = "";
        StringArray stationIDs = new StringArray();
        StringArray stationHasObsProp = new StringArray();
        StringArray uniqueObsProp = new StringArray();

        String infoUrl = "???";
        String institution = "???";
        String summary = "???";
        String title = "???";
        String license = "[standard]";
        String tStationID = "";
        StringBuilder tStationObsPropList = new StringBuilder();
        int offeringTagCount = 0;
        String offeringTag;
        String offeringEndTag; 

        SimpleXMLReader xmlReader = new SimpleXMLReader(File2.getDecompressedBufferedInputStream(safeFileName));
        try {
            xmlReader.nextTag();
            String tags = xmlReader.allTags();
            boolean ioosServer = false;
            if (xmlReader.tag(0).equals("Capabilities")) {
                sosPrefix = "";              //ioosServer
                if (sosServerType.length() == 0) //not explicitly declared
                    ioosServer = true;
            } else if (xmlReader.tag(0).equals("sos:Capabilities")) {
                sosPrefix = "sos:"; //oostethys, ioos52N
            } else {
                String2.log(SSR.getUrlResponseStringUnchanged(tUrl));
                throw new RuntimeException("The first SOS capabilities tag=\"" + 
                    tags + "\" should have been <Capabilities> or <sos:Capabilities>.");
            }
            offeringTag = "<" + 
                sosPrefix + "Capabilities><" + 
                sosPrefix + "Contents><" + 
                sosPrefix + "ObservationOfferingList><" + 
                sosPrefix + "ObservationOffering>";
            offeringEndTag = "<" + 
                sosPrefix + "Capabilities><" + 
                sosPrefix + "Contents><" + 
                sosPrefix + "ObservationOfferingList></" + 
                sosPrefix + "ObservationOffering>";


            do {
                //process the tags
                if (debugMode) String2.log("tags=" + tags + xmlReader.content());

                if (tags.endsWith("<ows:ServiceIdentification></ows:ServiceTypeVersion>")) {
                    String s = xmlReader.content();
                    if (verbose) String2.log("  ServiceTypeVersion=" + s);
                    if (sosVersion == null || sosVersion.length() == 0 ||
                        s.equals("1.0.0")) { //preference to 1.0.0 (ioos52N also offers 2.0.0)
                        sosVersion = s; //e.g., "0.0.31" or "1.0.0"
                        if (verbose) String2.log("  sosVersion(from ServiceTypeVersion)=" + s);
                    }
                } else if (tags.endsWith("<ows:ServiceIdentification></ows:Title>")) {
                    title = xmlReader.content();
                    if (verbose) String2.log("  title(from Title)=" + title);

                } else if (tags.endsWith("<ows:ServiceIdentification></ows:Abstract>")) {
                    summary = xmlReader.content();
                    if (verbose) String2.log("  summary(from Abstract)=" + summary);

                } else if (tags.endsWith("<ows:ServiceIdentification></ows:AccessConstraints>")) {
                    String s = xmlReader.content();
                    if (s.length() >= 0 &&
                        !s.toLowerCase().equals("none")) {
                        license = s;
                        if (verbose) String2.log("  license(from AccessConstraints)=" + license);
                    }

                } else if (tags.endsWith("<ows:ServiceProvider></ows:ProviderName>")) {
                    institution = xmlReader.content();
                    if (verbose) String2.log("  institution(from ProviderName)=" + institution);

                } else if (tags.endsWith("<ows:ServiceProvider><ows:ProviderSite>")) {
                    infoUrl = xmlReader.attributeValue("xlink:href");
                    if (verbose) String2.log("  infoUrl(from ProviderSite)=" + infoUrl);

                } else if (tags.startsWith(offeringTag)) {
                    String endOfTag = tags.substring(offeringTag.length());
                    String content = xmlReader.content();
                    String error = null;
                    if (tags.equals(offeringTag))
                        offeringTagCount++;
                    //if (debugMode) String2.log("offering=" + endOfTag + xmlReader.content());


    /* separate phenomena
                <sos:ObservationOffering xmlns:xlink="https://www.w3.org/1999/xlink" gml:id="A01">
                    <gml:description> Latest data from Mooring A01 from the Gulf of Maine Ocean Observing System (GoMOOS), located in the Gulf of Maine Massachusetts Bay </gml:description>
                    <gml:name>A01</gml:name>
                    <gml:boundedBy>
                        <gml:Envelope>
                            <gml:lowerCorner srsName="urn:ogc:def:crs:EPSG:6.5:4329">42.5277 -70.5665</gml:lowerCorner>
                            <gml:upperCorner srsName="urn:ogc:def:crs:EPSG:6.5:4329">42.5277 -70.5665</gml:upperCorner>
                        </gml:Envelope>
                    </gml:boundedBy>
                    <sos:time>
                        <gml:TimePeriod gml:id="AVAILABLE_OFFERING_TIME">
                            <gml:beginPosition>2001-07-10T06:30:00Z</gml:beginPosition>
                            <gml:endPosition indeterminatePosition="now"/>
                            <gml:timeInterval unit="hour">1</gml:timeInterval>
                        </gml:TimePeriod>
                    </sos:time>
                    <sos:procedure xlink:href="urn:gomoos.org:source.mooring#A01"/>
                    <sos:observedProperty xlink:href="http://marinemetadata.org/cf#sea_water_temperature"/>
                    <sos:observedProperty xlink:href="http://marinemetadata.org/cf#sea_water_salinity"/>
                    <sos:featureOfInterest xlink:href="urn:something:bodyOfWater"/>
                    <sos:responseFormat>application/com-xml</sos:responseFormat>
                </sos:ObservationOffering>
    */
    /* or compositePhenomenon...
                    <sos:observedProperty>
                        <swe:CompositePhenomenon gml:id="WEATHER_OBSERVABLES">
                            <gml:name>Weather measurements</gml:name>
                            <swe:component xlink:href="http://vast.uah.edu/dictionary/phenomena.xml#AirTemperature"/>
                            <swe:component xlink:href="http://vast.uah.edu/dictionary/phenomena.xml#AtmosphericPressure"/>
                            <swe:component xlink:href="http://vast.uah.edu/dictionary/phenomena.xml#WindSpeed"/>
                            <swe:component xlink:href="http://vast.uah.edu/dictionary/phenomena.xml#WindDirection"/>
                        </swe:CompositePhenomenon>
                    </sos:observedProperty>
    */

                    if (endOfTag.equals("")) {
                        tStationID = "";
                        tStationObsPropList.setLength(0);

                    } else if (endOfTag.equals("</gml:name>")) { //ioosServer and OOSTethys have this
                        //e.g., 44004
                        tStationID = content;
                        //remove  so names are shorter on table below
                        String pre = ":station:";
                        int prePo = tStationID.indexOf(pre);
                        if (prePo >= 0)
                            tStationID = tStationID.substring(prePo + pre.length());
                        if (reallyVerbose) String2.log("  tStationID=" + tStationID);

                    } else if (endOfTag.equals("<" + sosPrefix + "observedProperty>")) {
                        //handle non-composite observedProperty
                        //<sos:observedProperty xlink:href="http://marinemetadata.org/cf#sea_water_temperature"/>
                        //                      http://marinemetadata.org is GONE!
                        //NOW                   xlink:href="http://mmisw.org/ont/cf/parameter/sea_water_temperature"

                        String tXlink = xmlReader.attributeValue("xlink:href"); //without quotes
                        if (tXlink != null && !tXlink.toLowerCase().equals("none")) {
                            if (reallyVerbose) String2.log("  observedProperty=" + tXlink);
                            int opPo = uniqueObsProp.indexOf(tXlink);   
                            if (opPo < 0) {
                                opPo = uniqueObsProp.size();
                                uniqueObsProp.add(tXlink);
                            }
                            while (tStationObsPropList.length() <= opPo)
                                tStationObsPropList.append(' ');
                            tStationObsPropList.setCharAt(opPo, (char)(65 + opPo));
                        }

                    } else if (endOfTag.equals("<" + sosPrefix + "observedProperty><swe:CompositePhenomenon>")) {
                        //handle composite observedProperty
                        //<swe:CompositePhenomenon gml:id="WEATHER_OBSERVABLES">
                        String tXlink = xmlReader.attributeValue("gml:id"); //without quotes
                        if (tXlink != null && !tXlink.toLowerCase().equals("none")) {
                            if (reallyVerbose) String2.log("  composite observedProperty=" + tXlink);
                            int opPo = uniqueObsProp.indexOf(tXlink);   
                            if (opPo < 0) {
                                opPo = uniqueObsProp.size();
                                uniqueObsProp.add(tXlink);
                            }
                            while (tStationObsPropList.length() <= opPo)
                                tStationObsPropList.append(' ');
                            tStationObsPropList.setCharAt(opPo, (char)(65 + opPo));
                        }
                    }

                    //handle the error
                    if (error != null)
                        throw new RuntimeException(
                            "Error on capabilities xml line #" + xmlReader.lineNumber() + 
                            " stationID=" + tStationID + ": " + error);

                //end of a station
                } else if (tags.startsWith(offeringEndTag)) {
                    //String2.log("endTag");
                    if (tStationID.length() > 0 && 
                        tStationObsPropList.length() > 0) {
                        stationIDs.add(tStationID);
                        stationHasObsProp.add(tStationObsPropList.toString());
                    } else {
                        String2.log("Invalid Station id=" + tStationID + 
                            " tStationObsPropList='" + tStationObsPropList + "'");
                    }
                    tStationID = "";
                    tStationObsPropList.setLength(0);
                }

                //get the next tag
                xmlReader.nextTag();
                tags = xmlReader.allTags();
            } while (!tags.startsWith("</"));
        } finally { 
            xmlReader.close();      
        }

        if (verbose) String2.log("\n" +
            "sosPrefix=" + sosPrefix + "\n" +
            "offeringTag=" + offeringTag + " count=" + offeringTagCount);
        if (sosVersion == null || sosVersion.length() == 0) 
            sosVersion = "1.0.0"; //default

        //write the station/obsProp info
        if (stationIDs.size() == 0) 
            throw new RuntimeException("No stations found! Try a different sosServerType.\n" +
                "  offeringTag=" + offeringTag +
                "  stationID name tag=" + "<gml:name>");
        sb.append("<!-- You have to choose which observedProperties will be used for this dataset.\n\n");
        int longestStationID = Math.max(19, stationIDs.maxStringLength());
        int longestHasProp = stationHasObsProp.maxStringLength(); 
        sb.append("\n" + 
            String2.left("   n  Station (shortened)", 6+longestStationID) + "  Has ObservedProperty\n" +
            "----  " + String2.makeString('-', longestStationID) +
            "  " + String2.makeString('-', 60) + "\n");
        for (int si = 0; si < stationIDs.size(); si++) {
            sb.append(
                String2.right("" + si, 4) + "  " +
                String2.left(stationIDs.get(si), longestStationID) + "  " + 
                stationHasObsProp.get(si) + "\n");
        }
        
        //list the props
        if (uniqueObsProp.size() == 0) 
            throw new RuntimeException("No observedProperties found! Try a different sosServerType.\n" +
                "  offeringTag=" + offeringTag + "\n" +
                "  observedPropertyTag=" + "<" + sosPrefix + "observedProperty>");
        sb.append("\n id  ObservedProperty\n" +
                    "---  --------------------------------------------------\n");
        for (int op = 0; op < uniqueObsProp.size(); op++) 
            sb.append(String2.right("" + (char)(65 + op), 3) + "  " + uniqueObsProp.get(op) + "\n");

        //generate a table with the datasets information       
        Table table = new Table();
        Attributes sgAtts = new Attributes(); //source global atts
        sgAtts.add("infoUrl", infoUrl);
        sgAtts.add("institution", institution);
        if (ioos52NServer)
            summary += slowSummaryWarning;
        sgAtts.add("summary", summary);
        sgAtts.add("title", title);
        sgAtts.add("license", license);
        sgAtts.add("cdm_timeseries_variables", "station_id, longitude, latitude");
        sgAtts.add("subsetVariables",          "station_id, longitude, latitude");

        Attributes gAddAtts = makeReadyToUseAddGlobalAttributesForDatasetsXml(sgAtts, 
            "TimeSeries", tLocalSourceUrl, new Attributes(), //externalAtts
            new HashSet()); //suggestedKeywords
        Attributes gAtts = table.globalAttributes();
        gAtts.add(sgAtts); //since only addAtts will be printed
        gAtts.add(gAddAtts);
        for (int op = 0; op < uniqueObsProp.size(); op++) {
            String prop = uniqueObsProp.get(op);
            String dvName = prop;
            int po = dvName.lastIndexOf("#");
            if (po < 0)
               po = dvName.lastIndexOf("/");
            if (po < 0)
               po = dvName.lastIndexOf(":");
            if (po >= 0)
                dvName = dvName.substring(po + 1);                
            Attributes sourceAtts = new Attributes();
            sourceAtts.add("standard_name", dvName);
            PrimitiveArray destPA = new DoubleArray();
            Attributes addAtts = makeReadyToUseAddVariableAttributesForDatasetsXml(
                sgAtts, sourceAtts, null, dvName, 
                destPA.elementType() != PAType.STRING, //tryToAddStandardName
                destPA.elementType() != PAType.STRING, false); //addColorBarMinMax, tryToFindLLAT
            //then add the sourceAtts to the addAtts (since addAtts is all that will be shown)
            addAtts.add("observedProperty", prop);
            addAtts.add("standard_name", dvName);
            addAtts.add("units", "???");
            table.addColumn(op, dvName, destPA, addAtts);
        }

        //tryToFindLLAT. LLAT are already known, but this will fix invalid destinationNames.
        tryToFindLLAT(null, table);

        //don't use suggestSubsetVariables() since sourceTable not available

        //*** generate the datasets.xml       
        sb.append("\n" +
            "NOTE! For SOS datasets, you must look at the observedProperty's\n" +
            "phenomenaDictionary URL (or an actual GetObservations response)\n" +
            "to see which dataVariables will be returned for a given phenomenon.\n" +
            "(longitude, latitude, altitude, and time are handled separately.)\n" +
            "-->\n");

        sb.append(
            "<dataset type=\"EDDTableFromSOS\" datasetID=\"" + suggestDatasetID(tPublicSourceUrl) + 
                    "\" active=\"true\">\n" +
            "    <sourceUrl>" + XML.encodeAsXML(tLocalSourceUrl) + "</sourceUrl>\n" +
            "    <sosVersion>" + XML.encodeAsXML(sosVersion) + "</sosVersion>\n" +
            "    <sosServerType>" + sosServerType + "</sosServerType>\n" +
            "    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" +
            "    <observationOfferingIdRegex>.+</observationOfferingIdRegex>\n" +
            "    <requestObservedPropertiesSeparately>" +
                (ioos52NServer || ioosNdbcServer || ioosNosServer? "true" : "false") + 
                "</requestObservedPropertiesSeparately>\n" +
            "    <longitudeSourceName>longitude</longitudeSourceName>\n" +
            "    <latitudeSourceName>latitude</latitudeSourceName>\n" +
            "    <altitudeSourceName>???depth???ioos:VerticalPosition</altitudeSourceName>\n" +
            "    <altitudeMetersPerSourceUnit>-1</altitudeMetersPerSourceUnit>\n" +
            "    <timeSourceName>time</timeSourceName>\n" +
            "    <timeSourceFormat>yyyy-MM-dd'T'HH:mm:ss" +
                (ioos52NServer? ".000" : "") +
                "Z</timeSourceFormat>\n" +
            "    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below,\n" +
            "      notably, cdm_timeseries_variables, subsetVariables.\n" +
            "    -->\n");
        sb.append(writeAttsForDatasetsXml(true, table.globalAttributes(), "    "));
        //last 2 params: includeDataType, questionDestinationName
        sb.append(writeVariablesForDatasetsXml(null, table, "dataVariable", true, false));
        sb.append(
            "</dataset>\n");

        String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
        return sb.toString();
    }

    /**
     * testGenerateDatasetsXml
     */
    public static void testGenerateDatasetsXml(boolean useCachedInfo) throws Throwable {
        testVerboseOn();
        //debugMode = true;
        String2.log("\n*** EDDTableFromSOS.testGenerateDatasetsXml");

        try {
            String results = generateDatasetsXml(useCachedInfo, 
                "https://sdf.ndbc.noaa.gov/sos/server.php", "1.0.0", "IOOS_NDBC") + 
                "\n";

String expected1 = 
"<!-- You have to choose which observedProperties will be used for this dataset.\n" +
"\n" +
"\n" +
"   n  Station (shortened)                 Has ObservedProperty\n" +
"----  ----------------------------------  ------------------------------------------------------------\n" +
"   0  urn:ioos:network:noaa.nws.ndbc:all  ABCDEFGHI\n" +
"   1  wmo:0y2w3                           AB    G I\n" +
"   2  wmo:14040                           AB      I\n";

            Test.ensureEqual(results.substring(0, expected1.length()), expected1, 
                "results=\n" + results);

String expected2 = 
" id  ObservedProperty\n" +
"---  --------------------------------------------------\n" +
"  A  http://mmisw.org/ont/cf/parameter/air_temperature\n" +
"  B  http://mmisw.org/ont/cf/parameter/air_pressure_at_sea_level\n" +
"  C  http://mmisw.org/ont/cf/parameter/sea_water_electrical_conductivity\n" +
"  D  http://mmisw.org/ont/cf/parameter/currents\n" +
"  E  http://mmisw.org/ont/cf/parameter/sea_water_salinity\n" +
"  F  http://mmisw.org/ont/cf/parameter/sea_floor_depth_below_sea_surface\n" +
"  G  http://mmisw.org/ont/cf/parameter/sea_water_temperature\n" +
"  H  http://mmisw.org/ont/cf/parameter/waves\n" +
"  I  http://mmisw.org/ont/cf/parameter/winds\n" +
"\n" +
"NOTE! For SOS datasets, you must look at the observedProperty's\n" +
"phenomenaDictionary URL (or an actual GetObservations response)\n" +
"to see which dataVariables will be returned for a given phenomenon.\n" +
"(longitude, latitude, altitude, and time are handled separately.)\n" +
"-->\n" +
"<dataset type=\"EDDTableFromSOS\" datasetID=\"noaa_ndbc_6a36_2db7_1aab\" active=\"true\">\n" +
"    <sourceUrl>https://sdf.ndbc.noaa.gov/sos/server.php</sourceUrl>\n" +
"    <sosVersion>1.0.0</sosVersion>\n" +
"    <sosServerType>IOOS_NDBC</sosServerType>\n" +
"    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" +
"    <observationOfferingIdRegex>.+</observationOfferingIdRegex>\n" +
"    <requestObservedPropertiesSeparately>true</requestObservedPropertiesSeparately>\n" +
"    <longitudeSourceName>longitude</longitudeSourceName>\n" +
"    <latitudeSourceName>latitude</latitudeSourceName>\n" +
"    <altitudeSourceName>???depth???ioos:VerticalPosition</altitudeSourceName>\n" +
"    <altitudeMetersPerSourceUnit>-1</altitudeMetersPerSourceUnit>\n" +
"    <timeSourceName>time</timeSourceName>\n" +
"    <timeSourceFormat>yyyy-MM-dd'T'HH:mm:ssZ</timeSourceFormat>\n" +
"    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below,\n" +
"      notably, cdm_timeseries_variables, subsetVariables.\n" +
"    -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">TimeSeries</att>\n" +
"        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"creator_email\">webmaster.ndbc@noaa.gov</att>\n" +
"        <att name=\"creator_name\">NOAA NDBC</att>\n" +
"        <att name=\"creator_type\">institution</att>\n" +
"        <att name=\"creator_url\">https://www.ndbc.noaa.gov/</att>\n" +
"        <att name=\"infoUrl\">https://sdf.ndbc.noaa.gov/</att>\n" +
"        <att name=\"institution\">NOAA NDBC</att>\n" +
"        <att name=\"keywords\">buoy, center, data, national, ndbc, noaa, server.php, sos</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" +
"        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n" +
"        <att name=\"summary\">National Data Buoy Center SOS. NOAA NDBC data from https://sdf.ndbc.noaa.gov/sos/server.php.das .</att>\n" +
"        <att name=\"title\">National Data Buoy Center SOS (server.php)</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>air_temperature</sourceName>\n" +
"        <destinationName>air_temperature</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">40.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-10.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Air Temperature</att>\n" +
"            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/air_temperature</att>\n" +
"            <att name=\"standard_name\">air_temperature</att>\n" +
"            <att name=\"units\">???</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>air_pressure_at_sea_level</sourceName>\n" +
"        <destinationName>air_pressure_at_sea_level</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">1050.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">950.0</att>\n" +
"            <att name=\"ioos_category\">Pressure</att>\n" +
"            <att name=\"long_name\">Air Pressure At Sea Level</att>\n" +
"            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/air_pressure_at_sea_level</att>\n" +
"            <att name=\"standard_name\">air_pressure_at_sea_level</att>\n" +
"            <att name=\"units\">???</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>sea_water_electrical_conductivity</sourceName>\n" +
"        <destinationName>sea_water_electrical_conductivity</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">40.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">30.0</att>\n" +
"            <att name=\"ioos_category\">Salinity</att>\n" +
"            <att name=\"long_name\">Sea Water Electrical Conductivity</att>\n" +
"            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/sea_water_electrical_conductivity</att>\n" +
"            <att name=\"standard_name\">sea_water_electrical_conductivity</att>\n" +
"            <att name=\"units\">???</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>currents</sourceName>\n" +
"        <destinationName>currents</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Currents</att>\n" +
"            <att name=\"long_name\">Currents</att>\n" +
"            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/currents</att>\n" +
"            <att name=\"standard_name\">currents</att>\n" +
"            <att name=\"units\">???</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>sea_water_salinity</sourceName>\n" +
"        <destinationName>sea_water_salinity</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">37.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">32.0</att>\n" +
"            <att name=\"ioos_category\">Salinity</att>\n" +
"            <att name=\"long_name\">Sea Water Practical Salinity</att>\n" +
"            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/sea_water_salinity</att>\n" +
"            <att name=\"standard_name\">sea_water_salinity</att>\n" +
"            <att name=\"units\">???</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>sea_floor_depth_below_sea_surface</sourceName>\n" +
"        <destinationName>sea_floor_depth_below_sea_surface</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">8000.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-8000.0</att>\n" +
"            <att name=\"colorBarPalette\">TopographyDepth</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Sea Floor Depth Below Sea Surface</att>\n" +
"            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/sea_floor_depth_below_sea_surface</att>\n" +
"            <att name=\"standard_name\">sea_floor_depth_below_sea_surface</att>\n" +
"            <att name=\"units\">???</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>sea_water_temperature</sourceName>\n" +
"        <destinationName>sea_water_temperature</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Sea Water Temperature</att>\n" +
"            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/sea_water_temperature</att>\n" +
"            <att name=\"standard_name\">sea_water_temperature</att>\n" +
"            <att name=\"units\">???</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>waves</sourceName>\n" +
"        <destinationName>waves</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Surface Waves</att>\n" +
"            <att name=\"long_name\">Waves</att>\n" +
"            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/waves</att>\n" +
"            <att name=\"standard_name\">waves</att>\n" +
"            <att name=\"units\">???</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>winds</sourceName>\n" +
"        <destinationName>winds</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"long_name\">Winds</att>\n" +
"            <att name=\"observedProperty\">http://mmisw.org/ont/cf/parameter/winds</att>\n" +
"            <att name=\"standard_name\">winds</att>\n" +
"            <att name=\"units\">???</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n";

            int po = results.indexOf(expected2.substring(0, 40));
            if (po < 0)
                String2.log(results);
            Test.ensureEqual(
                results.substring(po, Math.min(results.length(), po + expected2.length())), 
                expected2, 
                "results=\n" + results);

            //GenerateDatasetsXml
            String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
                "EDDTableFromSOS",
                "https://sdf.ndbc.noaa.gov/sos/server.php", "1.0.0", "IOOS_NDBC", 
                "-1"}, //defaultStandardizeWhat
                false); //doIt loop?
            Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");


        } catch (Throwable t) {
            throw new RuntimeException("Error using generateDatasetsXml. This frequently changes a little.", t); 
        }

    }


    /** 
     * This generates ready-to-use datasets.xml (one dataset per observed property)
     * for an IOOS SOS server.
     * <br>The XML can then be edited by hand and added to the datasets.xml file.
     *
     * @param tLocalSourceUrl  with no '?', e.g., https://sdf.ndbc.noaa.gov/sos/server.php
     * @param sosVersion e.g., "1.0.0" (or "" or null)
     * @return a suggested chunk of xml for this dataset for use in datasets.xml 
     * @throws Throwable if trouble, e.g., if no Grid or Array variables are found.
     *    If no trouble, then a valid dataset.xml chunk has been returned.
     */
    public static String generateDatasetsXmlFromIOOS(boolean useCachedInfo,
        String tLocalSourceUrl, String sosVersion, 
        String sosServerType) throws Throwable {

        String2.log("EDDTableFromSos.generateDatasetsXmlFromIOOS" +
            "\n  tLocalSourceUrl=" + tLocalSourceUrl);
        String tPbublicSourceUrl = convertToPublicSourceUrl(tLocalSourceUrl);
        sosServerType = sosServerType == null? "" : sosServerType.trim();
        boolean isIoos52N = sosServerType.toLowerCase().equals(
            SosServerTypeIoos52N.toLowerCase());

        String tUrl = tLocalSourceUrl + "?service=SOS&request=GetCapabilities";
        if (sosVersion != null && sosVersion.length() > 0) {
            //if (ioosNcbcServer || ioosNosServer) //not needed. 1.0.0 is default and only.
            //    tUrl += "&version=" + sosVersion;
            if (isIoos52N) //AcceptVersions needed. This class only supports 1.0.0 currently.
                tUrl += "&AcceptVersions=" + sosVersion;
        }
        //if (reallyVerbose) String2.log(SSR.getUrlResponseStringUnchanged(tUrl));
        File2.makeDirectory(sosCopyDir);
        String safeFileName = sosCopyDir + String2.md5Hex12(tUrl);
        if (!useCachedInfo || !File2.isFile(safeFileName)) {
            String2.log("downloading GetCapabilities" +
                "\n  from: " + tUrl +
                "\n  to: " + safeFileName);
            SSR.downloadFile(tUrl, safeFileName, false); //tryToUseCompression
        } else {
            String2.log("using cached GetCapabilities" +
                "\n  from file: " + safeFileName + 
                "\n  from URL: " + tUrl);
        }

        //gather station info and uniqueObservedProperties
        StringArray stationIDs = new StringArray();
        StringArray stationHasObsProp = new StringArray();
        StringArray uniqueObsProp = new StringArray();

        String tInfoUrl = null;
        String tInstitution = null;
        String tLicense = "[standard]";
        String tSummary = null;
        String tTitle = null;
        String tOfferingAll = null;
        String tStationID = "";
        StringBuilder tStationObsPropList = new StringBuilder();
        int offeringTagCount = 0;
        String tSosVersion = null; //e.g., "1.0.0"
        String offeringTag;
        String offeringEndTag;
        String sosPrefix = "";

        SimpleXMLReader xmlReader = new SimpleXMLReader(File2.getDecompressedBufferedInputStream(safeFileName));
        try {
            xmlReader.nextTag();
            String tags = xmlReader.allTags();
            boolean ioosServer = false;
            if (xmlReader.tag(0).equals("Capabilities")) {
                sosPrefix = "";              //ioosServer
                if (sosServerType.length() == 0) //not explicitly declared
                    ioosServer = true;
            } else if (xmlReader.tag(0).equals("sos:Capabilities")) {
                sosPrefix = "sos:"; //oostethys
            } else {
                String2.log(String2.readFromFile(safeFileName, String2.UTF_8)[1]);
                throw new RuntimeException("The first SOS capabilities tag=\"" + 
                    tags + "\" should have been <Capabilities> or <sos:Capabilities>.");
            }
            if (verbose) String2.log("  sosPrefix=" + sosPrefix); 
            offeringTag = "<" + 
                sosPrefix + "Capabilities><" + 
                sosPrefix + "Contents><" + 
                sosPrefix + "ObservationOfferingList><" + 
                sosPrefix + "ObservationOffering>";
            offeringEndTag = "<" + 
                sosPrefix + "Capabilities><" + 
                sosPrefix + "Contents><" + 
                sosPrefix + "ObservationOfferingList></" + 
                sosPrefix + "ObservationOffering>";

            do {
                //process the tags
                if (debugMode) String2.log("tags=" + tags + xmlReader.content());

                if (tags.endsWith("<ows:ServiceIdentification></ows:ServiceTypeVersion>")) {
                    if (verbose) String2.log("ServiceTypeVersion=" + xmlReader.content());
                    if (tSosVersion == null || tSosVersion.length() == 0) {
                        tSosVersion = xmlReader.content(); //e.g., "0.0.31" or "1.0.0"
                        if (verbose) String2.log("  sosVersion(from ServiceTypeVersion)=" + tSosVersion);
                    }

                } else if (tags.endsWith("<ows:ServiceIdentification></ows:Title>")) {
                    tTitle = xmlReader.content();
                    if (verbose) String2.log("  title(from Title)=" + tTitle);

                } else if (tags.endsWith("<ows:ServiceIdentification></ows:Abstract>")) {
                    tSummary = xmlReader.content();
                    if (verbose) String2.log("  summary(from Abstract)=" + tTitle);

                } else if (tags.endsWith("<ows:ServiceIdentification></ows:AccessConstraints>")) {
                    String s = xmlReader.content();
                    if (s.length() >= 0 &&
                        !s.toLowerCase().equals("none")) {
                        tLicense = s;
                        if (verbose) String2.log("  license(from AccessConstraints)=" + tLicense);
                    }

                } else if (tags.endsWith("<ows:ServiceProvider></ows:ProviderName>")) {
                    tInstitution = xmlReader.content();
                    if (tInstitution.equals("National Data Buoy Center"))
                        tInstitution = "NOAA NDBC";
                    else if (tInstitution.equals("National Ocean Service"))
                        tInstitution = "NOAA NOS";
                    if (verbose) String2.log("  institution(from ProviderName)=" + tInstitution);

                } else if (tags.endsWith("<ows:ServiceProvider><ows:ProviderSite>")) {
                    tInfoUrl = xmlReader.attributeValue("xlink:href");
                    if (verbose) String2.log("  infoUrl(from ProviderSite)=" + tInfoUrl);

                } else if (tags.startsWith(offeringTag)) {
                    String endOfTag = tags.substring(offeringTag.length());
                    String content = xmlReader.content();
                    String error = null;
                    if (tags.equals(offeringTag))
                        offeringTagCount++;
    //String2.log("endOfTag=" + endOfTag + xmlReader.content());


    /* separate phenomena
                <sos:ObservationOffering xmlns:xlink="https://www.w3.org/1999/xlink" gml:id="A01">
                    <gml:description> Latest data from Mooring A01 from the Gulf of Maine Ocean Observing System (GoMOOS), located in the Gulf of Maine Massachusetts Bay </gml:description>
                    <gml:name>A01</gml:name>
                    <gml:boundedBy>
                        <gml:Envelope>
                            <gml:lowerCorner srsName="urn:ogc:def:crs:EPSG:6.5:4329">42.5277 -70.5665</gml:lowerCorner>
                            <gml:upperCorner srsName="urn:ogc:def:crs:EPSG:6.5:4329">42.5277 -70.5665</gml:upperCorner>
                        </gml:Envelope>
                    </gml:boundedBy>
                    <sos:time>
                        <gml:TimePeriod gml:id="AVAILABLE_OFFERING_TIME">
                            <gml:beginPosition>2001-07-10T06:30:00Z</gml:beginPosition>
                            <gml:endPosition indeterminatePosition="now"/>
                            <gml:timeInterval unit="hour">1</gml:timeInterval>
                        </gml:TimePeriod>
                    </sos:time>
                    <sos:procedure xlink:href="urn:gomoos.org:source.mooring#A01"/>
                    <sos:observedProperty xlink:href="http://marinemetadata.org/cf#sea_water_temperature"/>
                    <sos:observedProperty xlink:href="http://marinemetadata.org/cf#sea_water_salinity"/>
                    <sos:featureOfInterest xlink:href="urn:something:bodyOfWater"/>
                    <sos:responseFormat>application/com-xml</sos:responseFormat>
                </sos:ObservationOffering>
    */

                    if (endOfTag.equals("")) {
                        //String2.log("startTag");
                        tStationID = "";
                        tStationObsPropList.setLength(0);

                    } else if (endOfTag.equals("</gml:name>")) { //ioosServer, OOSTethys, ioos52N have this
                        //e.g., 44004
                        tStationID = content;

                        if (tStationID.endsWith(":all")) {
                            tOfferingAll = tStationID;
                        }

                        if (ioosServer) { 
                            //remove  so names are shorter on table below
                            String pre = "::";
                            int prePo = tStationID.indexOf(pre);
                            if (prePo >= 0)
                                tStationID = tStationID.substring(prePo + pre.length());
                        }



                    } else if (endOfTag.equals("<" + sosPrefix + "observedProperty>")) {
                        //handle non-composite observedProperty
                        //<sos:observedProperty xlink:href="http://marinemetadata.org/cf#sea_water_temperature"/>
                        //NOW http://mmisw.org/ont/cf/parameter/sea_water_salinity
                        //    http://marinemetadata.org is GONE!
                        String tXlink = xmlReader.attributeValue("xlink:href"); //without quotes
                        if (tXlink != null && !tXlink.toLowerCase().equals("none")) {
                            if (!isIoos52N) {
                                //shorten it
                                int po = tXlink.lastIndexOf("#");
                                if (po < 0)
                                   po = tXlink.lastIndexOf("/");
                                if (po < 0)
                                   po = tXlink.lastIndexOf(":");
                                if (po >= 0)
                                    tXlink = tXlink.substring(po + 1);                
                            }

                            //insert in list if not there already
                            int opPo = uniqueObsProp.indexOf(tXlink);   
                            if (opPo < 0) {
                                opPo = uniqueObsProp.size();
                                uniqueObsProp.add(tXlink);
                            }
                            while (tStationObsPropList.length() <= opPo)
                                tStationObsPropList.append(' ');
                            tStationObsPropList.setCharAt(opPo, (char)(65 + opPo));
                        }

                    } else if (endOfTag.equals("<" + sosPrefix + "observedProperty><swe:CompositePhenomenon>")) {
                        //handle composite observedProperty
                        //<swe:CompositePhenomenon gml:id="WEATHER_OBSERVABLES">
                        String tXlink = xmlReader.attributeValue("gml:id"); //without quotes
                        if (tXlink != null && !tXlink.toLowerCase().equals("none")) {
                            int opPo = uniqueObsProp.indexOf(tXlink);   
                            if (opPo < 0) {
                                opPo = uniqueObsProp.size();
                                uniqueObsProp.add(tXlink);
                            }
                            while (tStationObsPropList.length() <= opPo)
                                tStationObsPropList.append(' ');
                            tStationObsPropList.setCharAt(opPo, (char)(65 + opPo));
                        }
                    }

                    //handle the error
                    if (error != null)
                        throw new RuntimeException(
                            "Error on SOS GetCapabilities xml line #" + xmlReader.lineNumber() + 
                            " stationID=" + tStationID + ": " + error);

                //end of a station
                } else if (tags.startsWith(offeringEndTag)) {
                    //String2.log("endTag");
                    if (tStationID.length() > 0 && 
                        tStationObsPropList.length() > 0) {
                        stationIDs.add(tStationID);
                        stationHasObsProp.add(tStationObsPropList.toString());
                    } else {
                        String2.log("Invalid Station id=" + tStationID + 
                            " tStationObsPropList='" + tStationObsPropList + "'");
                    }
                    tStationID = "";
                    tStationObsPropList.setLength(0);
                }

                //get the next tag
                xmlReader.nextTag();
                tags = xmlReader.allTags();
            } while (!tags.startsWith("</"));
        } finally {
            xmlReader.close();      
        }
        if (tSosVersion == null || tSosVersion.length() == 0) 
            tSosVersion = "1.0.0"; //default
        if (stationIDs.size() == 0) 
            throw new RuntimeException("No stations found! Try a different sosServerType.\n" +
                "  offeringTag=" + offeringTag +
                "  stationID name tag=" + "<gml:name>");

        //write the station/obsProp info
        StringBuilder sb = new StringBuilder();
        sb.append(
            "<!-- NOTE! Some of the standard_names below probably aren't CF standard names!\n" +
            "   Check them and delete the ones that aren't CF standard names.\n" +
            "\n" +
            "NOTE! Be wary of suggested dataType=byte. It may just mean there was no\n" +
            "   data for that variable in the data that was sampled.\n" +
            "   Change it to short/int/float/double as needed.\n" +            
            "\n");
        int longestStationID = Math.max(7, stationIDs.maxStringLength());
        int longestHasProp = stationHasObsProp.maxStringLength(); 
        sb.append( 
            String2.left("  n  Station", 5+longestStationID) + "  Has observed_property\n" +
            "___  " + String2.makeString('_', longestStationID) +
            "  " + String2.makeString('_', 60) + "\n");
        for (int si = 0; si < stationIDs.size(); si++) {
            sb.append(
                String2.right("" + si, 3) + "  " +
                String2.left(stationIDs.get(si), longestStationID) + "  " + 
                stationHasObsProp.get(si) + "\n");
        }

        //list the observed_property
        if (uniqueObsProp.size() == 0) 
            throw new RuntimeException("No observedProperties found! Try a different sosServerType.\n" +
                "  offeringTag=" + offeringTag + "\n" +
                "  observedPropertyTag=" + "<" + sosPrefix + "observedProperty>");
        sb.append("\n id  observed_property\n" +
                    "___  __________________________________________________\n");
        for (int op = 0; op < uniqueObsProp.size(); op++) 
            sb.append(String2.right("" + (char)(65 + op), 3) + "  " + uniqueObsProp.get(op) + "\n");
        sb.append("-->\n");


        //make a dataset for each observed_property
        if (tOfferingAll == null)
            tOfferingAll = "urn:ioos:network:noaa.nws.ndbc:all";        
        double tEpochSeconds = System.currentTimeMillis() / 1000.0;
        tEpochSeconds = Calendar2.backNDays(7, tEpochSeconds);
        String time1 = Calendar2.safeEpochSecondsToIsoStringTZ(tEpochSeconds, "");
        tEpochSeconds += Calendar2.SECONDS_PER_DAY;
        String time2 = Calendar2.safeEpochSecondsToIsoStringTZ(tEpochSeconds, "");

        String pre  = tLocalSourceUrl + "?service=SOS&version=" + tSosVersion +
            "&request=GetObservation&offering=" + tOfferingAll +
            "&observedProperty=";
        String post =
            "&responseFormat=" + SSR.minimalPercentEncode(
                defaultResponseFormat(sosServerType)) + 
            "&eventTime=" + time1 + "/" + time2 +
            (isIoos52N? "" : "&featureofinterest=BBOX:-180,-90,180,90");
        int nUniqueObsProp = uniqueObsProp.size();  //often set to 1 when testing !!!
        for (int op = 0; op < nUniqueObsProp; op++) {
            try {
                sb.append(generateDatasetsXmlFromOneIOOS(useCachedInfo,
                    pre + uniqueObsProp.get(op) + post, 
                    sosVersion, sosServerType, tInfoUrl, tInstitution, tLicense));
            } catch (Throwable t) {
                sb.append("\n<!-- ERROR for " + pre + uniqueObsProp.get(op) + post + "\n" + 
                    MustBe.throwableToString(t) + "-->\n\n");
            }
        }

        String2.log("\n\n*** generateDatasetsXmlFromIOOS finished successfully.\n\n");
        return sb.toString();
    }


    /** 
     * This generates a readyToUse datasets.xml entry for an IOOS EDDTableFromSOS.
     * <br>The XML can then be edited by hand and added to the datasets.xml file.
     *
     * @param tUrl a URL to get data for responseFormat=text/csv, 
     *   for a single observedProperty and, for a BoundingBox
     *   e.g.,
https://sdf.ndbc.noaa.gov/sos/server.php?service=SOS&version=1.0.0
&request=GetObservation&offering=urn:ioos:network:noaa.nws.ndbc:all
&observedProperty=sea_water_salinity
&responseFormat=text/csv&eventTime=2010-05-27T00:00:00Z/2010-05-27T01:00:00Z
&featureofinterest=BBOX:-151.719,17.93,-65.927,60.8
     * @param tInfoUrl  the suggested infoUrl (use null or "" if nothing to suggest)
     * @param tInstitution  the suggested institution (use null or "" if nothing to suggest)
     * @param tLicense  the suggested license (use null or "" for the default)
     * @return a suggested chunk of xml for this dataset for use in datasets.xml 
     * @throws Throwable if trouble, e.g., if no Grid or Array variables are found.
     *    If no trouble, then a valid dataset.xml chunk has been returned.
     */
    public static String generateDatasetsXmlFromOneIOOS(boolean useCachedInfo,
        String tUrl, String sosVersion, String sosServerType, String tInfoUrl, 
        String tInstitution, String tLicense) throws Throwable {

        sosVersion    = sosVersion    == null? "" : sosVersion.trim();
        sosServerType = sosServerType == null? "" : sosServerType.trim();
        boolean isIoos52N = sosServerType.toLowerCase().equals(
            SosServerTypeIoos52N.toLowerCase());
        String2.log("\nEDDTableFromSos.generateDatasetsXmlFromOneIOOS isIoos52N=" + isIoos52N + 
            "\n  tUrl=" + tUrl);

        if (!String2.isSomething(tLicense))
            tLicense = "[standard]";
        int po, po1;

        //get the response
        File2.makeDirectory(sosCopyDir);
        String safeFileName = sosCopyDir + String2.md5Hex12(tUrl);
        if (!useCachedInfo || !File2.isFile(safeFileName)) {
            String2.log("downloading GetObservation" +
                "\n  from: " + tUrl +
                "\n  to: " + safeFileName);
            SSR.downloadFile(tUrl, safeFileName, false); //tryToUseCompression
        } else {
            String2.log("using cached GetObservation" +
                "\n  from file: " + safeFileName + 
                "\n  from URL: " + tUrl);
        }

        //parse the response into a data table
        Table sosTable;
        String timeSourceName, longitudeSourceName, latitudeSourceName, 
            altitudeSourceName; 
        double altitudeMPSU; //<altitudeMetersPerSourceUnit>
        if (isIoos52N) {
            //xml with csv payload
            sosTable = readIoos52NXmlDataTable(File2.getDecompressedBufferedInputStream(safeFileName));
            timeSourceName = "time";
            longitudeSourceName = "longitude";
            latitudeSourceName = "latitude";
            altitudeSourceName = "altitude";
            altitudeMPSU = 1;

        } else {

            //read the file
            BufferedReader br = File2.getDecompressedBufferedFileReader(safeFileName, null);
            /* needs fix to work with BufferedReader
            if (reallyVerbose) {
                String2.log("ASCII response=");
                int stop = Math.min(100, sa.size());
                for (int i = 0; i < stop; i++)
                    String2.log(sa.get(i));
                String2.log("...\n");
            }*/
            
            //is it an xml file (presumably an error report)?
            br.mark(10000); //max read-ahead bytes
            String s = br.readLine();
            if (s.startsWith("<?xml")) {
                StringBuilder sb = new StringBuilder();
                while (s != null) { //initially, s has first line
                    sb.append(s);
                    sb.append('\n');
                    s = br.readLine();
                }
                throw new SimpleException(sb.toString());
            }
            br.reset();

            //read into sosTable
            boolean simplify = true;
            sosTable = new Table();
            sosTable.readASCII(safeFileName, br, 
                "", "", 0, 1, "", null, null, null, null, simplify); 
            timeSourceName = "date_time";
            longitudeSourceName = "longitude (degree)";
            latitudeSourceName  = "latitude (degree)";
            altitudeSourceName  = "depth (m)";
            altitudeMPSU = -1;

        }
        if (reallyVerbose)
            String2.log("response table=\n" + sosTable.toString(4));


        //tBaseUrl  https://sdf.ndbc.noaa.gov/sos/server.php
        po = tUrl.indexOf('?');
        if (po < 0)
            throw new SimpleException(String2.ERROR + 
                ": '?' not found in tUrl=" + tUrl);
        String tLocalBaseUrl = tUrl.substring(0, po);
        String tPublicBaseUrl = convertToPublicSourceUrl(tLocalBaseUrl);

        //tBBoxOffering  urn:ioos:network:noaa.nws.ndbc:all
        po = tUrl.indexOf("&offering=");
        if (po < 0)
            throw new SimpleException(String2.ERROR + 
                ": '&offering=' not found in tUrl=" + tUrl);
        po1 = tUrl.indexOf("&", po + 1);
        if (po1 < 0) po1 = tUrl.length();
        String tBBoxOffering = tUrl.substring(po + 10, po1);

        //shortObservedProperty   sea_water_salinity
        //from &observedProperty=http://mmisw.org/ont/cf/parameter/sea_water_salinity
        po = tUrl.indexOf("&observedProperty=");
        if (po < 0)
            throw new SimpleException(String2.ERROR + 
                ": '&observedProperty=' not found in sourceUrl=" + tUrl);
        po1 = tUrl.indexOf("&", po + 18);
        if (po1 < 0) po1 = tUrl.length();
        String tObservedProperty = tUrl.substring(po + 18, po1);
        po = tObservedProperty.lastIndexOf('/');  //ok if -1
        String shortObservedProperty = tObservedProperty.substring(po + 1);

        //tInfoUrl  https://sdf.ndbc.noaa.gov/sos/
        if (tInfoUrl == null || tInfoUrl.length() == 0)
            tInfoUrl = File2.getDirectory(tPublicBaseUrl);       

        //tInstitution
        if (tInstitution == null || tInstitution.length() == 0)
            tInstitution = suggestInstitution(tPublicBaseUrl);

        //tDatasetID
        String tDatasetID = suggestDatasetID(tPublicBaseUrl + "?" + shortObservedProperty);

        //remove LLATI columns
        //they are identified below
        po = sosTable.findColumnNumber(longitudeSourceName);   if (po >= 0) sosTable.removeColumn(po);
        po = sosTable.findColumnNumber(latitudeSourceName);    if (po >= 0) sosTable.removeColumn(po);
        po = sosTable.findColumnNumber(altitudeSourceName);    if (po >= 0) sosTable.removeColumn(po); 
        po = sosTable.findColumnNumber(timeSourceName);        if (po >= 0) sosTable.removeColumn(po);
        po = sosTable.findColumnNumber("station_id");          if (po >= 0) sosTable.removeColumn(po);

        //write the main parts
        StringBuilder sb = new StringBuilder();
        sb.append(        
"<dataset type=\"EDDTableFromSOS\" datasetID=\"" + tDatasetID + "\" active=\"true\">\n" +
"    <sourceUrl>" + XML.encodeAsXML(tLocalBaseUrl) + "</sourceUrl>\n" +
"    <sosVersion>" + sosVersion + "</sosVersion>\n" +
"    <sosServerType>" + sosServerType + "</sosServerType>\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <observationOfferingIdRegex>.+</observationOfferingIdRegex>\n" +
"    <requestObservedPropertiesSeparately>true</requestObservedPropertiesSeparately>\n" +
"    <bboxOffering>" + XML.encodeAsXML(tBBoxOffering) + "</bboxOffering>\n" +
"    <bboxParameter>" + 
    (isIoos52N? "BBOX=" : "featureofinterest=BBOX:") + 
    "</bboxParameter>\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">TimeSeries</att>\n" +
"        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"infoUrl\">" + XML.encodeAsXML(tInfoUrl) + "</att>\n" +
"        <att name=\"institution\">" + XML.encodeAsXML(tInstitution) + "</att>\n" +
"        <att name=\"license\">" + XML.encodeAsXML(tLicense) + "</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" +
"        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n" +
"        <att name=\"summary\">This SOS server is part of the IOOS DIF SOS Project.  " +
"The stations in this dataset have " + XML.encodeAsXML(shortObservedProperty) + " data.\n" +
"\n" +
"Because of the nature of SOS requests, requests for data MUST include constraints for the longitude, latitude, time, and/or station_id variables." +
(isIoos52N? slowSummaryWarning : "") +
            "</att>\n" +
"        <att name=\"title\">" + XML.encodeAsXML(tInstitution) + 
            " SOS - " + XML.encodeAsXML(shortObservedProperty) + "</att>\n" +
"    </addAttributes>\n" +
"    <longitudeSourceName>" + XML.encodeAsXML(longitudeSourceName) + "</longitudeSourceName>\n" +
"    <latitudeSourceName>" + XML.encodeAsXML(latitudeSourceName) + "</latitudeSourceName>\n" +
"    <altitudeSourceName>" + XML.encodeAsXML(altitudeSourceName) + "</altitudeSourceName>\n" +
"    <altitudeMetersPerSourceUnit>" + altitudeMPSU + "</altitudeMetersPerSourceUnit>\n" +
"    <timeSourceName>" + XML.encodeAsXML(timeSourceName) + "</timeSourceName>\n" +
"    <timeSourceFormat>yyyy-MM-dd'T'HH:mm:ss" + (isIoos52N? ".SSS" : "") + 
    "Z</timeSourceFormat>\n");

        //make addTable with destinationNames and addAttributes
        Table addTable = new Table();
        int nCol = sosTable.nColumns();
        for (int col = 0; col < nCol; col++) {
            String colName = sosTable.getColumnName(col);
            String colNameNoParen = colName;
            String tUnits = sosTable.columnAttributes(col).getString("units");
            if (tUnits == null) {
                po = colName.indexOf(" (");
                if (po > 0) {
                    colNameNoParen = colName.substring(0, po);
                    tUnits = Units2.safeUcumToUdunits(
                        colName.substring(po + 2, colName.length() - 1));
                }
            }

            //make addAtts
            Attributes sourceAtts = sosTable.columnAttributes(col);
            Attributes addAtts = new Attributes();
            PAType tPAType = sosTable.getColumn(col).elementType();
            sourceAtts.add("standard_name", colNameNoParen);  //add now, remove later
            PrimitiveArray destPA = PrimitiveArray.factory(tPAType, 1, false);
            addAtts = makeReadyToUseAddVariableAttributesForDatasetsXml(
                sosTable.globalAttributes(), //but there are none
                sourceAtts, null, colNameNoParen, 
                destPA.elementType() != PAType.STRING, //tryToAddStandardName
                destPA.elementType() != PAType.STRING, //addColorBarMinMax
                true); //tryToFindLLAT
            if (tUnits != null) 
                addAtts.add("units", tUnits);
            sourceAtts.remove("standard_name");
            addAtts.add(      "standard_name", colNameNoParen);  
            addAtts.add("observedProperty", tObservedProperty);

            //add column to addTable
            addTable.addColumn(addTable.nColumns(), colNameNoParen, 
                destPA, addAtts);

        }

        //LLT are known, but this further cleans destNames 
        tryToFindLLAT(sosTable, addTable);

        //writeVariablesForDatasetsXml
        //last 2 params: includeDataType, questionDestinationName
        if (reallyVerbose) {
            for (int col = 0; col < nCol; col++) 
                String2.log(
                    String2.left(sosTable.getColumn(col).elementTypeString(), 10) + 
                    String2.left(sosTable.getColumnName(col), 15) + 
                    " units=" + sosTable.columnAttributes(col).getString("units"));
        }
        sb.append(
            writeVariablesForDatasetsXml(sosTable, addTable, 
                "dataVariable", true, false));
        sb.append(
"</dataset>\n\n");

        String2.log("\n\n*** generateDatasetsXmlFromOneIOOS finished successfully.\n\n");
        return sb.toString();
    }


    /** 
     * This generates ready-to-use datasets.xml (one dataset per observed property)
     * for an IOOS SOS server.
     * <br>The XML can then be edited by hand and added to the datasets.xml file.
     * <br>This tests https://sdf.ndbc.noaa.gov/sos/server.php
     *
     * @throws Throwable if trouble
     */
    public static void testGenerateDatasetsXmlFromIOOS(boolean useCachedInfo) 
        throws Throwable {
        String results = generateDatasetsXmlFromIOOS(useCachedInfo,
            "https://sdf.ndbc.noaa.gov/sos/server.php", "1.0.0", "IOOS_NDBC");
        String expected = expectedTestGenerateDatasetsXml(
            "https://sdf.ndbc.noaa.gov/", "1.0.0", "IOOS_NDBC", "NOAA NDBC", 
            "sea_water_salinity");
        String start = expected.substring(0, 80);
        int po = results.indexOf(start);
        if (po < 0) {
            String2.log("\nRESULTS=\n" + results);
            throw new SimpleException(
                "Start of 'expected' string not found in 'results' string:\n" + start);
        }
        String tResults = results.substring(po, 
            Math.min(results.length(), po + expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results + "\n\ntresults=\n" + tResults);
    }


    /**
     * Test generateDatasetsXmlFromIOOS. Their example from https://sdf.ndbc.noaa.gov/sos/ :
https://sdf.ndbc.noaa.gov/sos/server.php?request=GetObservation&service=SOS
&offering=urn:ioos:network:noaa.nws.ndbc:all&featureofinterest=BBOX:-90,25,-85,30
&observedproperty=sea_water_temperature&responseformat=text/csv
&eventtime=2008-08-01T00:00Z/2008-08-02T00:00Z
     */
    public static void testGenerateDatasetsXmlFromOneIOOS(boolean useCachedInfo) 
        throws Throwable {
        String results = generateDatasetsXmlFromOneIOOS(useCachedInfo,
            "https://sdf.ndbc.noaa.gov/sos/server.php?service=SOS&version=1.0.0" + 
            "&request=GetObservation" +
            "&observedProperty=sea_water_salinity" + 
            "&offering=urn:ioos:network:noaa.nws.ndbc:all" + 
            "&responseFormat=text/csv&eventTime=2010-05-27T00:00:00Z/2010-05-27T01:00:00Z" + 
            "&featureofinterest=BBOX:-180,-90,180,90", "1.0.0", "IOOS_NDBC", "", "", "");

        String expected = expectedTestGenerateDatasetsXml(
            "https://sdf.ndbc.noaa.gov/sos/", "1.0.0", "IOOS_NDBC", "NOAA NDBC", 
            "sea_water_salinity");
        Test.ensureEqual(results, expected, "results=\n" + results);
        
    }

    private static String expectedTestGenerateDatasetsXml(String tInfoUrl, 
        String tSosVersion, String tSosServerType, String tInstitution,
        String whichObsProp) { //air_temperature or sea_water_salinity
     
        tSosServerType = tSosServerType == null? "" : tSosServerType.trim();
        boolean isIoos52N = tSosServerType.toLowerCase().equals(
            SosServerTypeIoos52N.toLowerCase());
        return
"<dataset type=\"EDDTableFromSOS\" datasetID=\"noaa_ndbc_12f6_3b52_fb14\" active=\"true\">\n" +
"    <sourceUrl>https://sdf.ndbc.noaa.gov/sos/server.php</sourceUrl>\n" +
"    <sosVersion>"    + (tSosVersion    == null? "" : tSosVersion.trim())    + "</sosVersion>\n" +
"    <sosServerType>" + (tSosServerType == null? "" : tSosServerType.trim()) + "</sosServerType>\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <observationOfferingIdRegex>.+</observationOfferingIdRegex>\n" +
"    <requestObservedPropertiesSeparately>true</requestObservedPropertiesSeparately>\n" +
"    <bboxOffering>urn:ioos:network:noaa.nws.ndbc:all</bboxOffering>\n" +
"    <bboxParameter>" + 
    (isIoos52N? "BBOX=" : "featureofinterest=BBOX:") + 
    "</bboxParameter>\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">TimeSeries</att>\n" +
"        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"infoUrl\">" + tInfoUrl + "</att>\n" +
"        <att name=\"institution\">" + tInstitution + "</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" +
"        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n" +
"        <att name=\"summary\">This SOS server is part of the IOOS DIF SOS Project.  The stations in this dataset have " + whichObsProp + " data.\n" +
"\n" +
"Because of the nature of SOS requests, requests for data MUST include constraints for the longitude, latitude, time, and/or station_id variables.</att>\n" +
"        <att name=\"title\">NOAA NDBC SOS - " + whichObsProp + "</att>\n" +
"    </addAttributes>\n" +
"    <longitudeSourceName>longitude (degree)</longitudeSourceName>\n" +
"    <latitudeSourceName>latitude (degree)</latitudeSourceName>\n" +
"    <altitudeSourceName>" + 
     (isIoos52N? "altitude" : "depth (m)") + 
     "</altitudeSourceName>\n" +
"    <altitudeMetersPerSourceUnit>" + 
     (isIoos52N? "1.0" : "-1.0") + 
     "</altitudeMetersPerSourceUnit>\n" +
"    <timeSourceName>date_time</timeSourceName>\n" +
"    <timeSourceFormat>yyyy-MM-dd'T'HH:mm:ssZ</timeSourceFormat>\n" +
"    <dataVariable>\n" +
"        <sourceName>sensor_id</sourceName>\n" +
"        <destinationName>sensor_id</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Sensor Id</att>\n" +
"            <att name=\"observedProperty\">" + whichObsProp + "</att>\n" +
"            <att name=\"standard_name\">sensor_id</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +

(whichObsProp.equals("air_temperature")?
"    <dataVariable>\n" +
"        <sourceName>air_temperature (C)</sourceName>\n" +
"        <destinationName>air_temperature</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">40.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-10.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Air Temperature</att>\n" +
"            <att name=\"observedProperty\">air_temperature</att>\n" +
"            <att name=\"standard_name\">air_temperature</att>\n" +
"            <att name=\"units\">C</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" :

"    <dataVariable>\n" +
"        <sourceName>sea_water_salinity (psu)</sourceName>\n" +
"        <destinationName>sea_water_salinity</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">37.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">32.0</att>\n" +
"            <att name=\"ioos_category\">Salinity</att>\n" +
"            <att name=\"long_name\">Sea Water Practical Salinity</att>\n" +
"            <att name=\"observedProperty\">sea_water_salinity</att>\n" +
"            <att name=\"standard_name\">sea_water_salinity</att>\n" +
"            <att name=\"units\">psu</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n") +

"</dataset>\n" +
"\n";
    }

    /** 
     * NOT FINISHED. NOT ACTIVE.
     * This parses a phenomenon dictionary and generates a HashMap
     * with key=phenomenonURI, value=StringArray of non-composite phenomena
     * (variables) that it is comprised of.
     *
     * @param url
     * @param hashMap the hashMap to which with phenomena will be added
     * @throws Throwable if trouble
     */
    public static void getPhenomena(String url, HashMap hashMap) throws Throwable {

        String2.log("EDDTableFromSOS.getPhenomena" +
            "\nurl=" + url);
        StringBuilder sb = new StringBuilder();

        SimpleXMLReader xmlReader = new SimpleXMLReader(SSR.getUrlBufferedInputStream(url));
        try {
            xmlReader.nextTag();
            String tags = xmlReader.allTags();
            String sosPrefix = "";
            boolean ioosServer = false;
            String startTag = "<gml:Dictionary>";
            String endTag   = "</gml:Dictionary>";

            if (!tags.equals(startTag)) 
                throw new RuntimeException("The first PhenomenaDictionary tag=" + 
                    tags + " should have been " + startTag);

            String codeSpace = null;
            String tID = null;
            StringArray tComponents = null;

            do {
                //process the tags
                //String2.log("tags=" + tags + xmlReader.content());

                String endOfTag = tags.substring(startTag.length());
                String content = xmlReader.content();
                String error = null;
    //String2.log("endOfTag=" + endOfTag + xmlReader.content());


    /* separate phenomena
                    <sos:observedProperty xlink:href="http://marinemetadata.org/cf#sea_water_temperature"/>

    <gml:Dictionary ...>
        <gml:identifier codeSpace="urn:x-noaa:ioos:def:phenomenonNames">PhenomenaDictionary</gml:identifier>
        ...
        */
                if (endOfTag.equals("<gml:identifier>")) {
                    codeSpace = xmlReader.attributeValue("codeSpace"); 
                    if (reallyVerbose) String2.log("  codeSpace=" + codeSpace);

                //phenomenon
                /*  <gml:definitionMember >
                        <swe:Phenomenon gml:id="WaterTemperature">
                            <gml:description>Temperature of the water.</gml:description>
                            <gml:identifier codeSpace="urn:x-noaa:ioos:def:phenomenonNames">WaterTemperature</gml:identifier>
                        </swe:Phenomenon>
                    </gml:definitionMember> 
                */
                } else if (endOfTag.equals("<gml:definitionMember><swe:Phenomenon>")) {
                    tID = xmlReader.attributeValue("gml:id");
                    if (tID == null)
                        xmlReader.throwException("<swe:Phenomenon> tag has no gml:id.");
                    tComponents = new StringArray();
                    tComponents.add(codeSpace + "#" + tID);
                    hashMap.put(codeSpace + "#" + tID, tComponents); 


                //compositePhenomenon
                /*  <gml:definitionMember >
                        <swe:CompositePhenomenon gml:id="Winds" dimension="4">
                            <gml:description>Wind origin direction and speed.</gml:description>
                            <gml:identifier codeSpace="urn:x-noaa:ioos:def:phenomenonNames">Winds</gml:identifier>
                            <swe:base xlink:href="#MinimumWinds"/>
                            <swe:component xlink:href="#WindGust"/>
                        </swe:CompositePhenomenon>
                    </gml:definitionMember> 
                */
                } else if (endOfTag.equals("<gml:definitionMember><swe:CompositePhenomenon>")) {
                    tID = xmlReader.attributeValue("gml:id"); 
                    tComponents = new StringArray();

                } else if (endOfTag.equals("<gml:definitionMember><swe:CompositePhenomenon><swe:base>") ||
                           endOfTag.equals("<gml:definitionMember><swe:CompositePhenomenon><swe:component>")) {
                    String href = xmlReader.attributeValue("xlink:href"); 
                    if (href == null)
                        String2.log("WARNING: on XML line #" + xmlReader.lineNumber() + 
                            ": " + endOfTag + " doesn't have an xlink:href.");
                    else {
                        //get referenced item's components
                        href = (href.startsWith("#")? codeSpace : "") + href;
                        StringArray tsa = (StringArray)hashMap.get(href);
                        if (tsa == null) 
                            xmlReader.throwException(href + " isn't already defined in this document " +
                                "(Bob's assumption is that components of composite will be already defined).");
                        tComponents.append(tsa);
                    }

                } else if (endOfTag.equals("<gml:definitionMember></swe:CompositePhenomenon>")) {
                    hashMap.put(codeSpace + "#" + tID, tComponents);
                }

                //get the next tag
                xmlReader.nextTag();
                tags = xmlReader.allTags();
            } while (!tags.startsWith("</gml:Dictionary>"));
        } finally {
            xmlReader.close();      
        }

    }

    /**
     * NOT FINISHED. NOT ACTIVE.
     * This tests getPhenomena.
     */
    public static void testGetPhenomena() throws Throwable {
        String2.log("testGetPhenomena");
        testVerboseOn();
        HashMap hashMap = new HashMap();
        getPhenomena("https://ioos.github.io/sos-dif/gml/IOOS/0.6.1/dictionaries/phenomenaDictionary.xml", 
            hashMap);
        String sar[] = String2.toStringArray(hashMap.keySet().toArray());
        for (int i = 0; i < sar.length; i++) 
            String2.log(sar[i] + "\n" + ((StringArray)hashMap.get(sar[i])).toNewlineString() + "\n");

    }

    /**
     * This runs all of the tests for the ndbc test server.
     */
    public static void testNdbcTestServer() throws Throwable {
        String2.log("\n*** EDDTableFromSos.testNdbcTestServer()\n");
        testVerboseOn();

        testNdbcSosCurrents("test");    //TimeSeriesProfile
        testNdbcSosLongTime("test");
        testNdbcSosSalinity("test");  
        testNdbcSosWLevel("test");  
        testNdbcSosWTemp("test");  
        testNdbcSosWaves("test");
        testNdbcSosWind("test");
    }

    /**
     * This runs all of the tests for the nos test server.
     */
    public static void testNosTestServer() throws Throwable {
        String2.log("\n*** EDDTableFromSos.testNosTestServer()\n");
        testVerboseOn();

        testNosSosATemp("test");
        //testNosSosATempAllStations("test"); //long test
        testNosSosATempStationList("test");
        testNosSosBPres("test");
        testNosSosCond("test");
        testNosSosSalinity("test");
        testNosSosWLevel("test");  
        testNosSosWTemp("test");  
        testNosSosWind("test");
    }


    /**
     * This tests getting data from erddap's SOS server for cwwcNDBCMet.
     *
     * @throws Throwable if trouble
     */
    public static void testErddapSos() throws Throwable {
        String2.log("\n*** EDDTableFromSOS.testErddapSos()\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        int po;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
        EDDTable eddTable = (EDDTable)oneFromDatasetsXml(null, "testErddapSos"); 

        try {

            //*** test getting das for entire dataset
            String2.log("\n*** EDDTableFromSOS.testErddapSos() das dds for entire dataset\n");
            tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                eddTable.className() + "_Entire", ".das"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            //String2.log(results);
            expected = 
"  wspv {\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum -15.0;\n" +
"    String ioos_category \"Wind\";\n" +
"    String observedProperty \"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#cwwcNDBCMet\";\n" +
"    String standard_name \"northward_wind\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"TimeSeries\";\n";
            po = results.indexOf(expected.substring(0,9));
            if (po < 0) String2.log("\nresults=\n" + results);
            Test.ensureEqual(results.substring(po, po + expected.length()), expected, "\nresults=\n" + results);
           
            //*** test getting dds for entire dataset
            tName = eddTable.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                eddTable.className() + "_Entire", ".dds"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            //String2.log(results);
            expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    Float64 longitude;\n" +    //minor differences from cwwcNDBCMet: LLiAT order and data type
"    Float64 latitude;\n" +
"    String station_id;\n" +
"    Float64 altitude;\n" +
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
            String2.log("\n*** EDDTableFromSOS.testErddapSos() make DATA FILES\n");       

            //.csv
            //from NdbcMetStation.test31201
            //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
            //2005 04 19 00 00 999 99.0 99.0  1.40  9.00 99.00 999 9999.0 999.0  24.4 999.0 99.0 99.00 first available
            //double seconds = Calendar2.isoStringToEpochSeconds("2005-04-19T00");
            //int row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
            //Test.ensureEqual(table.getStringData(sosOfferingIndex, row), "31201", "");
            //Test.ensureEqual(table.getFloatData(latIndex, row), -27.7f, "");
            //Test.ensureEqual(table.getFloatData(lonIndex, row), -48.13f, "");

            userDapQuery = "longitude,latitude,altitude,time,station_id,wvht,dpd,wtmp,dewp" +
                "&longitude=-48.13&latitude=-27.7&time=2005-04-19T00";
            tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
                eddTable.className() + "_Data1", ".csv"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            //String2.log(results);
            expected = 
"longitude, latitude, altitude, time, station_id, wvht, dpd, wtmp, dewp\n" +
"degrees_east, degrees_north, m, UTC, , m, s, degree_C, degree_C\n" +
"-48.13, -27.7, NaN, 2005-04-19T00:00:00Z, urn:ioos:def:Station:1.0.0.127.cwwcNDBCMet:31201, 1.4, 9.0, 24.4, NaN\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);

            //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
            //2005 04 25 18 00 999 99.0 99.0  3.90  8.00 99.00 999 9999.0 999.0  23.9 999.0 99.0 99.00
            userDapQuery = "longitude,latitude,altitude,time,station_id,wvht,dpd,wtmp,dewp" +
                "&longitude=-48.13&latitude=-27.7&time>=2005-04-01&time<=2005-04-26";
            tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
                eddTable.className() + "_Data2", ".csv"); 
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            //String2.log(results);
            expected = "longitude,latitude,altitude,time,station_id,wvht,dpd,wtmp,dewp\n";
            Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);
            expected = "degrees_east,degrees_north,m,UTC,,m,s,degree_C,degree_C\n";
            Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);
            expected = "-48.13,-27.7,NaN,2005-04-19T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:31201,1.4,9.0,24.4,NaN\n"; //time above
            Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);
            expected = "-48.13,-27.7,NaN,2005-04-25T18:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:31201,3.9,8.0,23.9,NaN\n"; //this time
            Test.ensureTrue(results.indexOf(expected) >= 0, "\nresults=\n" + results);

            //test requesting a lat lon area
            userDapQuery = "longitude,latitude,altitude,time,station_id,wvht,dpd,wtmp,dewp" +
                "&longitude>-125&longitude<-121&latitude>35&latitude<39&time=2005-04-01";
            long time = System.currentTimeMillis();
            tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
                eddTable.className() + "_Data3", ".csv"); 
            String2.log("queryTime=" + (System.currentTimeMillis() - time));
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            //String2.log(results);
            expected = 
"longitude,latitude,altitude,time,station_id,wvht,dpd,wtmp,dewp\n" +
"degrees_east,degrees_north,m,UTC,,m,s,degree_C,degree_C\n" +
"-122.88,37.36,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46012,2.55,12.5,13.7,NaN\n" +
"-123.307,38.238,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46013,2.3,12.9,13.9,NaN\n" +
"-122.82,37.75,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46026,1.96,12.12,14.0,NaN\n" +
"-121.89,35.74,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46028,2.57,12.9,16.3,NaN\n" +
"-122.42,36.75,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46042,2.21,17.39,14.5,NaN\n" +
"-121.9,36.83,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46091,NaN,NaN,NaN,NaN\n" +
"-122.02,36.75,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46092,NaN,NaN,NaN,NaN\n" +
"-122.41,36.69,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46093,NaN,NaN,14.3,NaN\n" +
"-123.28,37.57,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46214,2.5,9.0,12.8,NaN\n" +
"-122.3,37.77,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:AAMC1,NaN,NaN,15.5,NaN\n" +
"-123.71,38.91,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:ANVC1,NaN,NaN,NaN,NaN\n" +
"-122.47,37.81,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:FTPC1,NaN,NaN,NaN,NaN\n" +
"-121.89,36.61,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:MTYC1,NaN,NaN,15.1,NaN\n" +
"-122.04,38.06,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:PCOC1,NaN,NaN,14.9,NaN\n" +
"-123.74,38.96,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:PTAC1,NaN,NaN,NaN,NaN\n" +
"-122.4,37.93,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:RCMC1,NaN,NaN,14.0,NaN\n" +
"-122.21,37.51,NaN,2005-04-01T00:00:00Z,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:RTYC1,NaN,NaN,14.2,NaN\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);

            //test that constraint vars are sent to low level data request
            userDapQuery = "longitude,latitude,altitude,station_id,wvht,dpd,wtmp,dewp" + //no "time" here
                "&longitude>-125&longitude<-121&latitude>35&latitude<39&time=2005-04-01"; //"time" here
            tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
                eddTable.className() + "_Data4", ".csv"); 
            String2.log("queryTime=" + (System.currentTimeMillis() - time));
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            //String2.log(results);
            expected = 
"longitude,latitude,altitude,station_id,wvht,dpd,wtmp,dewp\n" +
"degrees_east,degrees_north,m,,m,s,degree_C,degree_C\n" +
"-122.88,37.36,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46012,2.55,12.5,13.7,NaN\n" +
"-123.307,38.238,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46013,2.3,12.9,13.9,NaN\n" +
"-122.82,37.75,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46026,1.96,12.12,14.0,NaN\n" +
"-121.89,35.74,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46028,2.57,12.9,16.3,NaN\n" +
"-122.42,36.75,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46042,2.21,17.39,14.5,NaN\n" +
"-121.9,36.83,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46091,NaN,NaN,NaN,NaN\n" +
"-122.02,36.75,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46092,NaN,NaN,NaN,NaN\n" +
"-122.41,36.69,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46093,NaN,NaN,14.3,NaN\n" +
"-123.28,37.57,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:46214,2.5,9.0,12.8,NaN\n" +
"-122.3,37.77,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:AAMC1,NaN,NaN,15.5,NaN\n" +
"-123.71,38.91,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:ANVC1,NaN,NaN,NaN,NaN\n" +
"-122.47,37.81,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:FTPC1,NaN,NaN,NaN,NaN\n" +
"-121.89,36.61,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:MTYC1,NaN,NaN,15.1,NaN\n" +
"-122.04,38.06,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:PCOC1,NaN,NaN,14.9,NaN\n" +
"-123.74,38.96,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:PTAC1,NaN,NaN,NaN,NaN\n" +
"-122.4,37.93,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:RCMC1,NaN,NaN,14.0,NaN\n" +
"-122.21,37.51,NaN,urn:ioos:Station:1.0.0.127.cwwcNDBCMet:RTYC1,NaN,NaN,14.2,NaN\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);


            //test that constraint vars are sent to low level data request
            //and that constraint causing 0rows for a station doesn't cause problems
            userDapQuery = "longitude,latitude,wtmp&time>=2008-03-14T18:00:00Z&time<=2008-03-14T18:00:00Z&wtmp>20";
            tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
                eddTable.className() + "_Data5", ".csv"); 
            String2.log("queryTime=" + (System.currentTimeMillis() - time));
            results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            //String2.log(results);
            expected = 
"longitude,latitude,wtmp\n" +
"degrees_east,degrees_north,degree_C\n" +
"-80.17,28.5,21.2\n" +
"-78.47,28.95,23.7\n" +
"-80.6,30.0,20.1\n" +
"-46.0,14.53,25.3\n" +
"-65.01,20.99,25.7\n" +
"-71.49,27.47,23.8\n" +
"-69.649,31.9784,22.0\n" +
"-80.53,28.4,21.6\n" +
"-80.22,27.55,21.7\n" +
"-89.67,25.9,24.1\n" +
"-94.42,25.17,23.4\n" +
"-85.94,26.07,26.1\n" +
"-94.05,22.01,24.4\n" +
"-85.06,19.87,26.8\n" +
"-67.5,15.01,26.4\n" +
"-84.245,27.3403,20.2\n" +
"-88.09,29.06,21.7\n" +
"-157.79,17.14,24.3\n" +
"-160.74,19.16,24.7\n" +
"-152.48,17.52,24.0\n" +
"-153.87,0.02,25.0\n" +
"-158.12,21.67,24.3\n" +
"-157.68,21.42,24.2\n" +
"144.79,13.54,28.1\n" +
"-90.42,29.78,20.4\n" +
"-64.92,18.34,27.7\n" +
"-81.87,26.65,22.2\n" +
"-80.1,25.59,23.5\n" +
"-156.47,20.9,25.0\n" +
"167.74,8.74,27.6\n" +
"-81.81,24.55,23.9\n" +
"-80.86,24.84,23.8\n" +
"-64.75,17.7,26.0\n" +
"-67.05,17.97,27.1\n" +
"-80.38,25.01,24.2\n" +
"-81.81,26.13,23.7\n" +
"-170.688,-14.28,29.6\n" +
"-157.87,21.31,25.5\n" +
"-96.4,28.45,20.1\n" +
"-82.77,24.69,22.8\n" +
"-97.22,26.06,20.1\n" +
"-82.63,27.76,21.7\n" +
"-66.12,18.46,28.3\n" +
"-177.36,28.21,21.8\n" +
"-80.59,28.42,22.7\n" +
"166.62,19.29,27.9\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
        } catch (Throwable t) {
            throw new RuntimeException("THIS TEST REQUIRES THAT SOS SERVICES BE TURNED ON IN LOCAL ERDDAP.", t); 
        }

    }

    /* *
     * THIS IS INACTIVE.
     * This makes a table (stationID, stationName, longitude, latitude)
     * of the stations that have a certain observedProperty.
     * This was used (but not now) by EDDTableFromAsciiServiceNOS!
     *
     * @param xml
     * @param observedProperty e.g., http://mmisw.org/ont/cf/parameter/winds
     * @throws Exception if trouble
     */
    /* 
    public static Table getStationTable(InputStream in, String observedProperty) throws Throwable {
        if (verbose) String2.log("\n*** getStationTable(" + observedProperty + ")");
        SimpleXMLReader xmlReader = new SimpleXMLReader(in, "Capabilities");
        try {

            Table table = new Table();
            StringArray stationID = new StringArray();
            StringArray stationName = new StringArray();
            FloatArray longitude = new FloatArray();
            FloatArray latitude  = new FloatArray();
            table.addColumn("stationID",   stationID);
            table.addColumn("stationName", stationName);
            table.addColumn("longitude",   longitude);
            table.addColumn("latitude",    latitude);

            //process the tags
            String ofInterest = "<Capabilities><Contents><ObservationOfferingList>";            
            int ofInterestLength = ofInterest.length();
            String tStationID = "", tStationName = "";
            float tLongitude = Float.NaN, tLatitude = Float.NaN;
            boolean tHasProperty = false;
            while (true) {
                xmlReader.nextTag();
                String tags = xmlReader.allTags();
                String content = xmlReader.content();
                //if (reallyVerbose) String2.log("  tags=" + tags + content);
                if (xmlReader.stackSize() == 1) 
                    break; //the </content> tag
                if (!tags.startsWith(ofInterest))
                    continue;
                String localTags = tags.substring(ofInterest.length());
    //    <ObservationOffering gml:id="station-1612340">
    //        <gml:description>Honolulu</gml:description>
    //        <gml:boundedBy><gml:Envelope><gml:lowerCorner>21.3067 -157.867</gml:lowerCorner>
    //        <observedProperty xlink:href="https://ioos.noaa.gov/gml/IOOS/0.6.1/dictionaries/phenomenaDictionary.xml#WaterLevel" />

                if (localTags.equals( "<ObservationOffering>")) {
                    tStationID = xmlReader.attributeValue("gml:id");
                    if (tStationID.startsWith("station-"))
                        tStationID = tStationID.substring(8);

                } else if (localTags.equals("</ObservationOffering>")) {
                    String2.log(tStationID + " " + tStationName + " " + tLongitude + 
                        " " + tLatitude + " " + tHasProperty);
                    if (tStationID.length() > 0 && tStationName.length() > 0 &&
                        !Float.isNaN(tLongitude) && !Float.isNaN(tLatitude) &&
                        tHasProperty) {
                        stationID.add(tStationID);
                        stationName.add(tStationName);
                        longitude.add(tLongitude);
                        latitude.add(tLatitude);
                    }
                    tStationID = "";         tStationName = "";
                    tLongitude = Float.NaN;  tLatitude = Float.NaN;
                    tHasProperty = false;

                } else if (localTags.equals("<ObservationOffering></gml:description>")) {
                    tStationName = content;              

                } else if (localTags.equals("<ObservationOffering><gml:boundedBy><gml:Envelope></gml:lowerCorner>")) {
                    StringArray tsa = StringArray.wordsAndQuotedPhrases(content);
                    if (tsa.size() == 2) {
                        tLatitude  = String2.parseFloat(tsa.get(0));
                        tLongitude = String2.parseFloat(tsa.get(1));
                    }

                } else if (localTags.equals("<ObservationOffering><observedProperty>")) {
                    String prop = xmlReader.attributeValue("xlink:href");
                    if (prop != null && prop.equals(observedProperty))
                        tHasProperty = true;
                }
            }
        } finally {
            xmlReader.close();
        }
        return table;
    }

    public static void testGetStationTable() throws Throwable {
        String2.log("\n*** testGetStationTable\n");
        try {
            BufferedInputStream bis = File2.getDecompressedBufferedInputStream("f:/programs/nos/stations.xml");
            Table table = getStationTable(bis, 
                "http://mmisw.org/ont/cf/parameter/winds");
            String2.log(table.toCSSVString());
            String2.log("\n *** Done.  nRows=" + table.nRows());
        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nExpected error.  NOS SOS Server is in flux."); 
        }

    } */

    /**
     * This tests that ensureValid throws exception if 2  
     * dataVariables use the same sourceName.
     * These tests are in EDDTableFromSOS because EDDTableFromFiles has a separate test.
     *
     * @throws Throwable if trouble
     */
    public static void test2DVSameSource() throws Throwable {
        String2.log("\n*** EDDTableFromSOS.test2DVSameSource()\n");
        String error = "shouldn't happen";
        try {
            EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, "tabletest2DVSameSource"); 
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
            error = String2.split(MustBe.throwableToString(t), '\n')[0]; 
            int errorPo = error.indexOf("ERROR");  //the line number in datasets.xml varies
            if (errorPo > 0)
                error = error.substring(errorPo);
        }

        Test.ensureEqual(error, 
            "ERROR: Duplicate dataVariableSourceNames: [5] and [7] are both \"sensor_id\".", 
            "Unexpected error message:\n" + error);
    }

    /**
     * This tests that ensureValid throws exception if 2  
     * dataVariables use the same destinationName.
     * These tests are in EDDTableFromSOS because EDDTableFromFiles has a separate test.
     *
     * @throws Throwable if trouble
     */
    public static void test2DVSameDestination() throws Throwable {
        String2.log("\n*** EDDTableFromSOS.test2DVSameDestination()\n");
        String error = "shouldn't happen";
        try {
            EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, "tabletest2DVSameDestination"); 
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
            error = String2.split(MustBe.throwableToString(t), '\n')[0]; 
            int errorPo = error.indexOf("ERROR");  //the line number in datasets.xml varies
            if (errorPo > 0)
                error = error.substring(errorPo);
        }

        Test.ensureEqual(error, 
            "ERROR: Duplicate dataVariableDestinationNames: [5] and [7] are both \"sensor_id\".", 
            "Unexpected error message:\n" + error);
    }

    /**
     * GCOOS SOS sample URLs: http://data.gcoos.org/52N_SOS.php
     *
     * @throws Throwable if trouble
     */
    public static void testGcoos52N(boolean useCachedInfo) throws Throwable {
        boolean oDebugMode = debugMode;
debugMode = false;
        testVerboseOn();
        String dir = EDStatic.fullTestCacheDirectory;
        EDDTable eddTable;               
        String name, tName, results, tResults, expected, userDapQuery;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.


        //one time 
        String tSourceUrl = "http://data.gcoos.org:8080/52nSOS/sos/kvp";
        //String2.log(generateDatasetsXml(useCachedInfo,
        //    tSourceUrl, "1.0.0", "IOOS_52N"));
        //String2.log(generateDatasetsXmlFromIOOS(useCachedInfo,
        //    tSourceUrl, "1.0.0", "IOOS_52N"));
        String oneIoosUrl = tSourceUrl + 
            "?service=SOS&version=1.0.0&request=GetObservation" + 
            //"&offering=urn:ioos:network:gcoos:all" + SSR times out after 10 minutes
            "&offering=urn:ioos:station:comps:42022" + //gml:id has '_', name has ':'
            "&observedProperty=" +
            "http://mmisw.org/ont/cf/parameter/air_temperature," +
            "http://mmisw.org/ont/cf/parameter/air_pressure," +
            "http://mmisw.org/ont/cf/parameter/wind_speed," +
            "http://mmisw.org/ont/cf/parameter/wind_speed_of_gust," +
            "http://mmisw.org/ont/cf/parameter/wind_to_direction" + 
            "&responseFormat=text/xml;+subtype=\"om/1.0.0/profiles/ioos_sos/1.0\"" +
            "&eventTime=2015-02-25T00:00:00Z/2015-02-25T01:00:00Z";
            //csv part of response has interlaced info! e.g., for just 2 properties:
            //2015-02-25T00:05:00.000Z,comps_42022_airpressure,1013.98
            //2015-02-25T00:05:00.000Z,comps_42022_airtemperature,18.94
            //2015-02-25T00:35:00.000Z,comps_42022_airpressure,1013.99
            //2015-02-25T00:35:00.000Z,comps_42022_airtemperature,18.53

        String2.log(generateDatasetsXmlFromOneIOOS(useCachedInfo,
            oneIoosUrl, "1.0.0", "IOOS_52N", 
            "http://data.gcoos.org/", "GCOOS", "[standard]"));// infoUrl, institution, licence

/*      Not finished because 52N response format is weird: interlaced lines.

        //test readIoos52NXmlDataTable() 
        Table table = readIoos52NXmlDataTable(File2.getDecompressedBufferedInputStream(
            "/programs/sos/ioos52NgcoosAirPressure.xml"));
        results = table.toCSVString(5);
        expected = 
"{\n" +
"dimensions:\n" +
"\trow = 5312 ;\n" +
"\ttime_strlen = 24 ;\n" +
"\tstation_id_strlen = 14 ;\n" +
"variables:\n" +
"\tchar time(row, time_strlen) ;\n" +
"\tchar station_id(row, station_id_strlen) ;\n" +
"\tdouble air_pressure(row) ;\n" +
"\n" +
"// global attributes:\n" +
"}\n" +
"time,station_id,air_pressure\n" +
"2015-02-25T00:05:00.000Z,comps_42013,1013.93\n" +
"2015-02-25T00:05:00.000Z,comps_42022,1013.98\n" +
"2015-02-25T00:05:00.000Z,comps_42023,1014.55\n" +
"2015-02-25T00:06:00.000Z,comps_fhpf1,1016.0\n" +
"2015-02-25T00:06:00.000Z,comps_nfbf1,1016.56\n";
        Test.ensureEqual(results, expected, "results=" + results);
        Test.ensureEqual(table.nRows(), 5312, "results=" + results);
        Test.ensureEqual(table.getStringData(0, 5311), "2015-02-26T00:00:00.000Z", "data(0, 5311)");
        Test.ensureEqual(table.getStringData(1, 5311), "nos_8771341", "data(1, 5311)");
        Test.ensureEqual(table.getDoubleData(2, 5311), 1009.10, "data(2, 5311)");


testQuickRestart = true;
            eddTable = (EDDTable)oneFromDatasetsXml(null, "gcoosSosAirPressure");  

        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            "gcoosSosAirPressure_Entire", ".das"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"Attributes {\n" +
" s {\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -97.492, -80.033;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range 16.834, 30.766;\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  station_id {\n" +
"    String cf_role \"timeseries_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station ID\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    String axis \"Z\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 9.08541e+8, NaN;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  air_pressure {\n" +
"    Float64 colorBarMaximum 1050.0;\n" +
"    Float64 colorBarMinimum 950.0;\n" +
"    String ioos_category \"Pressure\";\n" +
"    String long_name \"Air Pressure\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/air_pressure\";\n" +
"    String standard_name \"air_pressure\";\n" +
"    String units \"hPa\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"TimeSeries\";\n" +
"    String cdm_timeseries_variables \"station_id, longitude, latitude\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    Float64 Easternmost_Easting -80.033;\n" +
"    String featureType \"TimeSeries\";\n" +
"    Float64 geospatial_lat_max 30.766;\n" +
"    Float64 geospatial_lat_min 16.834;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max -80.033;\n" +
"    Float64 geospatial_lon_min -97.492;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"" + today;       
//T15:47:07Z http://data.gcoos.org:8080/52nSOS/sos/kvp
//2015-03-09T15:47:07Z http://localhost:8080/cwexperimental/tabledap/gcoosSosAirPressure.das
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

expected =
"    String infoUrl \"http://data.gcoos.org/\";\n" +
"    String institution \"GCOOS\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    Float64 Northernmost_Northing 30.766;\n" +
"    String sourceUrl \"http://data.gcoos.org:8080/52nSOS/sos/kvp\";\n" +
"    Float64 Southernmost_Northing 16.834;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String subsetVariables \"station_id, longitude, latitude\";\n" +
"    String summary \"This SOS server is part of the IOOS DIF SOS Project.  The stations in this dataset have air_pressure data.\n" +
"\n" +
"Because of the nature of SOS requests, requests for data MUST include constraints for the longitude, latitude, time, and/or station_id variables.\";\n" +
"    String time_coverage_start \"1998-10-16T12:30:00Z\";\n" +
"    String title \"GCOOS SOS - air_pressure\";\n" +
"    Float64 Westernmost_Easting -97.492;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 20));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
            "gcoosSosAirPressure_Entire", ".dds"); 
        results = String2.directReadFrom88591File(dir + tName);
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    Float64 longitude;\n" +
"    Float64 latitude;\n" +
"    String station_id;\n" +
"    Float64 altitude;\n" +
"    Float64 time;\n" +
"    Float64 air_pressure;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

            String2.log("\n*** EDDTableFromSOS nos AirTemperature test get one station .CSV data\n");
            tName = eddTable.makeNewFileForDapQuery(null, null,
                "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:8419317\"&time>=2006-01-01T00&time<=2006-01-01T01", 
                dir, eddTable.className() + "_nosSosATemp", ".csv"); 
            results = String2.directReadFrom88591File(dir + tName);
            expected = 
"longitude,latitude,station_id,altitude,time,sensor_id,air_temperature\n" +
"degrees_east,degrees_north,,m,UTC,,degree_C\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-3.9\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-3.9\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-3.9\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-3.9\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-3.9\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-3.9\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-3.9\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-4.0\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-4.0\n" +
"-70.5633,43.32,urn:ioos:station:NOAA.NOS.CO-OPS:8419317,NaN,2006-01-01T01:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:8419317:D1,-4.1\n";
            Test.ensureEqual(results, expected, "RESULTS=\n" + results);
            
/*

            String2.log("\n*** EDDTableFromSOS nos AirTemperature test get one station .CSV data\n");
            tName = eddTable.makeNewFileForDapQuery(null, null,  //1612340, NaN, 2008-10-26
                "&station_id=\"urn:ioos:station:NOAA.NOS.CO-OPS:1612340\"&time>=2008-10-26T00&time<2008-10-26T01", 
                dir, eddTable.className() + "_nosSosATemp", ".csv"); 
            results = String2.directReadFrom88591File(dir + tName);
            expected = 
"longitude,latitude,station_id,altitude,time,sensor_id,air_temperature\n" +
"degrees_east,degrees_north,,m,UTC,,degree_C\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:00:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.9\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:06:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:12:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.7\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:18:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:24:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.7\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:30:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:36:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.7\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:42:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:48:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.8\n" +
"-157.867,21.3067,urn:ioos:station:NOAA.NOS.CO-OPS:1612340,NaN,2008-10-26T00:54:00Z,urn:ioos:sensor:NOAA.NOS.CO-OPS:1612340:D1,24.7\n";
            Test.ensureEqual(results, expected, "RESULTS=\n" + results);

            String2.log("\n*** EDDTableFromSOS nos AirTemperature .das\n");
            String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.
            tName = eddTable.makeNewFileForDapQuery(null, null, "", dir, 
                eddTable.className() + "_nosSosATemp", ".das"); 
            results = String2.directReadFrom88591File(dir + tName);
            expected = 
"Attributes {\n" +
" s {\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -177.36, 166.618;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range -14.28, 70.4;\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  station_id {\n" +
"    String cf_role \"timeseries_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station ID\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    String axis \"Z\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range -3.6757152e+9, NaN;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  sensor_id {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Sensor ID\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/air_temperature\";\n" +
"  }\n" +
"  air_temperature {\n" +
"    Float64 colorBarMaximum 40.0;\n" +
"    Float64 colorBarMinimum -10.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Air Temperature\";\n" +
"    String observedProperty \"http://mmisw.org/ont/cf/parameter/air_temperature\";\n" +
"    String standard_name \"air_temperature\";\n" +
"    String units \"degree_C\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"TimeSeries\";\n" +
"    String cdm_timeseries_variables \"station_id, longitude, latitude, sensor_id\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    Float64 Easternmost_Easting 166.618;\n" +
"    String featureType \"TimeSeries\";\n" +
"    Float64 geospatial_lat_max 70.4;\n" +
"    Float64 geospatial_lat_min -14.28;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 166.618;\n" +
"    Float64 geospatial_lon_min -177.36;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"" + today;
//+ " https://opendap.co-ops.nos.noaa.gov/ioos-dif-sos"; //"-test"
            Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);

expected = 
"    Float64 Southernmost_Northing -14.28;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
"    String subsetVariables \"station_id, longitude, latitude\";\n" +
"    String summary \"The NOAA NOS SOS server is part of the IOOS DIF SOS Project.  The stations in this dataset have air temperature data.  ****These services are for testing and evaluation use only****\n" +
"\n" +
"Because of the nature of SOS requests, requests for data MUST include constraints for the longitude, latitude, time, and/or station_id variables.\";\n" +
"    String time_coverage_start \"1853-07-10T00:00:00Z\";\n" +
"    String title \"NOAA NOS SOS, EXPERIMENTAL - Air Temperature\";\n" +
"    Float64 Westernmost_Easting -177.36;\n" +
"  }\n" +
"}\n";
            int po = Math.max(0, results.indexOf(expected.substring(0, 30)));
            Test.ensureEqual(results.substring(po), expected, "RESULTS=\n" + results);
    /* */

        debugMode = oDebugMode;
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
            lastTest = interactive? -1 : 30;
        String msg = "\n^^^ EDDTableFromSOS.test(" + interactive + ") test=";

        boolean useCachedInfo = true;
        boolean testQuickRestart = false;  //normally false. Only true when actively testing quickRestart.

        for (int test = firstTest; test <= lastTest; test++) {
            try {
                long time = System.currentTimeMillis();
                String2.log(msg + test);
            
                if (interactive) {
                    //if (test ==  0) ...;

                } else {
                    //Treat all tests as slow tests.
                    //These aren't actually slow tests, but they fail/change so often that it usually isn't worth the time to test.
                    //Stated the other way: the problems usually aren't ERDDAP problems.
                    if (test ==  0 && doSlowTestsToo) testGenerateDatasetsXml(useCachedInfo); 
                    if (test ==  1 && doSlowTestsToo) testGenerateDatasetsXmlFromOneIOOS(useCachedInfo);
                    if (test ==  2 && doSlowTestsToo) testGenerateDatasetsXmlFromIOOS(useCachedInfo);

                    if (test ==  3 && doSlowTestsToo) testOostethys(); //gomoosBuoy    //TimeSeriesProfile
                    if (test ==  4 && doSlowTestsToo) testNeracoos(); 

                    if (test ==  5 && doSlowTestsToo) testNdbcSosCurrents("");      //TimeSeriesProfile
                    if (test ==  6 && doSlowTestsToo) testNdbcSosLongTime("");
                    if (test ==  7 && doSlowTestsToo) testNdbcSosSalinity("");  
                    if (test ==  8 && doSlowTestsToo) testNdbcSosWLevel("");  
                    if (test ==  9 && doSlowTestsToo) testNdbcSosWTemp("");  
                    //if (test == 10 && doSlowTestsToo) testNdbcSosWaves(""); //changed significantly 2016-09-21, not yet fixed

                    if (test == 11 && doSlowTestsToo) test2DVSameSource();
                    if (test == 12 && doSlowTestsToo) test2DVSameDestination();

                    //test NDBC Wind and quickRestart
                    if (test == 15 && doSlowTestsToo) {
                        String qrName = File2.forceExtension(quickRestartFullFileName("ndbcSosWind"), ".xml");
                        String2.log("\n\n*** deleting quickRestartFile exists=" + File2.isFile(qrName) +
                            " qrName=" + qrName);
                        File2.delete(qrName);
                        testNdbcSosWind("");  
                    }
                    if (test == 16 && doSlowTestsToo) {testQuickRestart = true; testNdbcSosWind(""); testQuickRestart = false; }

                    if (test == 20 && doSlowTestsToo) testNosSosATemp("");
                    if (test == 21 && doSlowTestsToo) testNosSosATempAllStations(""); //long test; important because it tests NO DATA from a station
                    if (test == 22 && doSlowTestsToo) testNosSosATempStationList("");
                    if (test == 23 && doSlowTestsToo) testNosSosBPres("");
                    if (test == 24 && doSlowTestsToo) testNosSosCond("");  
                    //2015-11 Currents no longer works. mismatch between station observedProperties and allowed observedProperties -- see getCapabilities
                    //  Also, was individual stations, now only a network offers currents data
                    //  2015-12-11 I emailed Andrea Hardy
                    // if (test == 25 && doSlowTestsToo) testNosSosCurrents("");  
                    if (test == 26 && doSlowTestsToo) testNosSosSalinity(""); 
                    if (test == 27 && doSlowTestsToo) testNosSosWLevel(""); 
                    if (test == 28 && doSlowTestsToo) testNosSosWTemp("");  
                    //2015-11 Wind no longer works. mismatch between station observedProperties and allowed observedProperties -- see getCapabilities
                    //  2015-12-11 I emailed Andrea Hardy
                    // if (test == 29 && doSlowTestsToo) testNosSosWind("");  
                    //if (test == 31 && doSlowTestsToo) testGcoos52N(useCachedInfo);   not finished

               
                    //*** usually not run
                    //if (test == 1001 && doSlowTestsToo) testErddapSos();  //not up-to-date
                    //if (test == 1002 && doSlowTestsToo) testGetStationTable(); inactive
                    //if (test == 1003 && doSlowTestsToo) testNdbcTestServer();
                    //if (test == 1004 && doSlowTestsToo) testGetPhenomena();
                    //if (test == 1005 && doSlowTestsToo) testVast(); //not working;
                    //if (test == 1006 && doSlowTestsToo) testTamu(); //not working;

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
