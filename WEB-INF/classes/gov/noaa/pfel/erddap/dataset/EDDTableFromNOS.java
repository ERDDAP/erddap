/* 
 * EDDTableFromNOS Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
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
import gov.noaa.pfel.coastwatch.pointdata.TableXmlHandler;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/* This uses axis.jar, jaxrps.jar, saaj.jar, and wsdl4j.jar. */
import javax.xml.soap.*;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/** 
 * THIS CLASS IS NO LONGER SUPPORTED. 
 * THE CODE WORKED FOR YEARS, BUT THE ONE SAMPLE DATASET STOPPED WORKING 2010-09-08,
 * SO THERE IS NO WAY TO TEST IT AND PROPERLY SUPPORT IT.
 * This class represents a table of data from a NOAA NOS source,
 * which uses soap+xml for requests and responses.
 * It is very specific to NOAA NOS's XML.
 *
 * <p>The stationUrl provides information about station lat and lon.
 * It would be nice if there were altitude/depth information.
 * It would be nice if there were time range information for each station.
 *
 * <p>Note: NOS station 8723970 requests fail: connection times out.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2007-06-08
 */
public class EDDTableFromNOS extends EDDTable{ 

    /** The first 4 dataVariables are from station table: longitude, latitude, 
     * station_name, station_id. */
    protected final static int nFixedVariables = 4; 
    protected final static int stationNameIndex = 2; 
    protected final static int stationIDIndex = 3; 

    //station table variables
    public static String stationUrl = "http://opendap.co-ops.nos.noaa.gov/stations/stationsXML.jsp";
    protected final static int STATION_NAME_COL = 0, //String
        STATION_ID_COL = 1, //int
        STATION_LON_COL = 2, STATION_LAT_COL = 3, //double
        STATION_TIME_COL = 4; //double, epochSeconds
    protected static Table stationTable;


    /** Variables set by the constructor. */
    protected String xmlns, getWhat, wsdlUrl, requestTimeFormat, rowElementXPath;
    protected SimpleDateFormat simpleRequestDateFormat; //to use, always synchronize(simpleRequestDateFormat)!!!


    /** 
     * NOS' stated max is 366. But 365 has problems(?). 
     * In the end, a smaller number is fine.
     * I don't think there is any reason to ever change this.
     */
    public static int maxDaysPerRequest = 200;  


    /**
     * This constructs an EDDTableFromNOS based on the information in an .xml file.
     * 
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableFromNOS"&gt;
     *    having just been read.  
     * @return an EDDTableFromNOS.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDTableFromNOS fromXml(SimpleXMLReader xmlReader) throws Throwable {

        //data to be obtained (or not)
        if (verbose) String2.log("\n*** constructing EDDTableFromNOS(xmlReader)...");
        String tDatasetID = xmlReader.attributeValue("datasetID"); 
        Attributes tGlobalAttributes = null;
        ArrayList tDataVariables = new ArrayList();
        int tReloadEveryNMinutes = Integer.MAX_VALUE;
        String tAccessibleTo = null;
        StringArray tOnChange = new StringArray();
        String tFgdcFile = null;
        String tIso19115File = null;
        String tLocalSourceUrl = null;
        String tXmlns = null, tGetWhat = null, tWsdlUrl = null, tRequestTimeFormat = null,
            tRowElementXPath = null;
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
            else if (localTags.equals( "<xmlns>")) {}
            else if (localTags.equals("</xmlns>")) tXmlns = content; 
            else if (localTags.equals( "<getWhat>")) {}
            else if (localTags.equals("</getWhat>")) tGetWhat = content; 
            else if (localTags.equals( "<wsdlUrl>")) {}
            else if (localTags.equals("</wsdlUrl>")) tWsdlUrl = content; 
            else if (localTags.equals( "<requestTimeFormat>")) {}
            else if (localTags.equals("</requestTimeFormat>")) tRequestTimeFormat = content; 
            else if (localTags.equals( "<rowElementXPath>")) {}
            else if (localTags.equals("</rowElementXPath>")) tRowElementXPath = content; 
            else if (localTags.equals( "<onChange>")) {}
            else if (localTags.equals("</onChange>")) tOnChange.add(content); 
            else if (localTags.equals( "<fgdcFile>")) {}
            else if (localTags.equals("</fgdcFile>"))     tFgdcFile = content; 
            else if (localTags.equals( "<iso19115File>")) {}
            else if (localTags.equals("</iso19115File>")) tIso19115File = content; 
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

        return new EDDTableFromNOS(tDatasetID, tAccessibleTo,
            tOnChange, tFgdcFile, tIso19115File,
            tDefaultDataQuery, tDefaultGraphQuery, tGlobalAttributes,
            ttDataVariables,
            tReloadEveryNMinutes, tLocalSourceUrl,
            tXmlns, tGetWhat, tWsdlUrl, tRequestTimeFormat, tRowElementXPath);
    }

    /**
     * The constructor.
     *
     * <p>This class is specific to data from http://opendap.co-ops.nos.noaa.gov/axis/ ,
     *   which uses soap+xml for requests and responses.
     * <br>This class assumes station info is available as an xml file from stationUrl.  
     * <br>This class assumes that no metadata is available from the data source.
     *
     * <p>Yes, lots of detailed information must be supplied here
     * that is sometimes available in metadata. If it is in metadata,
     * make a subclass that extracts info from metadata and calls this 
     * constructor.
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
     * @param tDataVariables is an Object[nDataVariables][4]: 
     *    <br>[0]=String sourceName (the name of the data variable in the dataset source),
     *    <br>[1]=String destinationName (the name to be presented to the ERDDAP user, 
     *        or null to use the sourceName),
     *    <br>[2]=Attributes addAttributes (at ERD, this must have "ioos_category" - 
     *        a category from EDV.ioosCategories). 
     *        Special case: value="null" causes that item to be removed from combinedAttributes.
     *    <br>[3]=String source dataType (e.g., "int", "float", "String"). 
     *        Some data sources have ambiguous data types, so it needs to be specified here.
     *    <br>The order of variables you define doesn't have to match the
     *       order in the source.
     *    <br>!!!Unique to EDDTableFromNOS: the longitude, latitude, stationName, and 
     *       stationID variables are created automatically (based on NOS station data).
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
     *   The altitude and time variables (if any) should have actual_range metadata.
     * @param tReloadEveryNMinutes indicates how often the source should
     *    be checked for new data.
     * @param tLocalSourceUrl the url to which queries are sent
     * @param tXmlns   e.g., wind
     * @param tGetWhat e.g., getWind
     * @param tWsdlUrl  actually, this is the xmlnsUrl, but for NOS it is always the
     *     wsdlUrl.
     * @param tRequestTimeFormat the SimpleDateFormat for formatting the time requests
     *    e.g., "yyyyMMdd HH:mm"
     *    (see http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html).
     * @param tRowElementXPath  e.g., /soapenv:Envelope/soapenv:Body/WindMeasurements/data/item
     * @throws Throwable if trouble
     */
    public EDDTableFromNOS(String tDatasetID, String tAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes,
        String tLocalSourceUrl, String tXmlns, String tGetWhat, String tWsdlUrl,
        String tRequestTimeFormat,
        String tRowElementXPath) throws Throwable {

        if (verbose) String2.log(
            "\n*** constructing EDDTableFromNOS " + tDatasetID); 
        long constructionStartMillis = System.currentTimeMillis();
        String errorInMethod = "Error in EDDTableFromNOS(" + 
            tDatasetID + ") constructor:\n";
            
        //save some of the parameters
        className = "EDDTableFromNOS"; 
        datasetID = tDatasetID;
        setAccessibleTo(tAccessibleTo);
        onChange = tOnChange;
        fgdcFile = tFgdcFile;
        iso19115File = tIso19115File;
        defaultDataQuery = tDefaultDataQuery;
        defaultGraphQuery = tDefaultGraphQuery;
        if (tAddGlobalAttributes == null)
            tAddGlobalAttributes = new Attributes();
        addGlobalAttributes = tAddGlobalAttributes;
        addGlobalAttributes.set("sourceUrl", convertToPublicSourceUrl(tLocalSourceUrl));
        localSourceUrl = tLocalSourceUrl;
        setReloadEveryNMinutes(tReloadEveryNMinutes);
        xmlns = tXmlns;
        getWhat = tGetWhat;
        wsdlUrl = tWsdlUrl;
        requestTimeFormat = tRequestTimeFormat;
        rowElementXPath = tRowElementXPath;
        sourceNeedsExpandedFP_EQ = false;
        sourceCanConstrainNumericData = CONSTRAIN_PARTIAL; //stationID, lon, lat, time, but not others
        sourceCanConstrainStringData  = CONSTRAIN_PARTIAL; //stationName anything, but others nothing
        sourceCanConstrainStringRegex = PrimitiveArray.REGEX_OP; //partial: stationName only

        Test.ensureNotNothing(tLocalSourceUrl,   "sourceUrl wasn't specified.");
        Test.ensureNotNothing(xmlns,             "xmlns wasn't specified.");
        Test.ensureNotNothing(getWhat,           "getWhat wasn't specified.");
        Test.ensureNotNothing(wsdlUrl,           "wsdlUrl wasn't specified.");
        Test.ensureNotNothing(requestTimeFormat, "requestTimeFormat wasn't specified.");
        Test.ensureNotNothing(rowElementXPath,   "rowElementXPath wasn't specified.");

        simpleRequestDateFormat = new SimpleDateFormat(requestTimeFormat); 
        simpleRequestDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
      
        //set source attributes (none available from source)
        sourceGlobalAttributes = new Attributes();
        combinedGlobalAttributes = new Attributes(addGlobalAttributes, sourceGlobalAttributes); //order is important
        String tLicense = combinedGlobalAttributes.getString("license");
        if (tLicense != null)
            combinedGlobalAttributes.set("license", 
                String2.replaceAll(tLicense, "[standard]", EDStatic.standardLicense));
        combinedGlobalAttributes.removeValue("null");


        //*** get the station table   (many datasets may share this)
        if (stationTable == null) {
            stationTable = new Table();
            InputStreamReader reader = new InputStreamReader(SSR.getUrlInputStream(stationUrl));            
            stationTable.readXml(reader, false, //no validate since no .dtd
                "/stations/station", new String[]{"name", "ID"}, true);
            Test.ensureEqual(stationTable.getColumnName(STATION_NAME_COL), "name", "\"name\" column not found in station table.");
            Test.ensureEqual(stationTable.getColumnName(STATION_ID_COL), "ID", "\"ID\" column not found in station table.");
            int nRows = stationTable.nRows();

            //convert lon  "48 28.7 W"  to decimal degrees
            int col = stationTable.findColumnNumber("metadata/location/long");
            if (col < 0)
                throw new RuntimeException("\"long\" column not found in station table.");
            stationTable.moveColumn(col, STATION_LON_COL);
            stationTable.setColumnName(STATION_LON_COL, EDV.LON_NAME);
            DoubleArray lonCol = new DoubleArray(nRows, false);
            for (int row = 0; row < nRows; row++) {
                StringBuilder sb = new StringBuilder(stationTable.getStringData(STATION_LON_COL, row));
                int po = sb.indexOf(" ");
                if (po > 0) {
                    sb.setCharAt(po, '°');
                    po = sb.indexOf(" ", po + 1);
                    if (po > 0) {
                        sb.setCharAt(po, '\'');
                    }
                }
                lonCol.add(EDV.toDecimalDegrees(sb.toString()));
            }
            stationTable.setColumn(STATION_LON_COL, lonCol);

            //convert latitude  "48 28.7 N"  to decimal degrees
            col = stationTable.findColumnNumber("metadata/location/lat");
            if (col < 0)
                throw new RuntimeException("\"lat\" column not found in station table.");
            stationTable.moveColumn(col, STATION_LAT_COL);
            stationTable.setColumnName(STATION_LAT_COL, EDV.LAT_NAME);
            DoubleArray latCol = new DoubleArray(nRows, false);
            for (int row = 0; row < nRows; row++) {
                StringBuilder sb = new StringBuilder(stationTable.getStringData(STATION_LAT_COL, row));
                int po = sb.indexOf(" ");
                if (po > 0) {
                    sb.setCharAt(po, '°');
                    po = sb.indexOf(" ", po + 1);
                    if (po > 0) {
                        sb.setCharAt(po, '\'');
                    }
                }
                latCol.add(EDV.toDecimalDegrees(sb.toString()));
            }
            stationTable.setColumn(STATION_LAT_COL, latCol);

            //convert date_established "2003-01-01"  to startTime (epoch sec)
            col = stationTable.findColumnNumber("metadata/date_established");
            if (col < 0)
                throw new RuntimeException("\"date_established\" column not found in station table.");
            stationTable.moveColumn(col, STATION_TIME_COL);
            stationTable.setColumnName(STATION_TIME_COL, "startTime");
            DoubleArray timeCol = new DoubleArray(nRows, false); //epochSeconds
            for (int row = 0; row < nRows; row++) {
                double tDate = Double.NaN;
                String ts = stationTable.getStringData(STATION_TIME_COL, row);
                try {
                    tDate = Calendar2.isoStringToEpochSeconds(ts); 
                } catch (Throwable t) {
                    if (reallyVerbose) String2.log("  Unable to interpret station=" + row + " date=" + ts);
                }
                timeCol.add(tDate);
            }
            stationTable.setColumn(STATION_TIME_COL, timeCol);

            //delete other columns
            stationTable.removeColumns(STATION_TIME_COL + 1, stationTable.nColumns());

            if (reallyVerbose)
                String2.log("\nstationTable=\n" + stationTable.toString("row", Integer.MAX_VALUE));
        }

        //create the fixed dataVariables[] and lon, lat, stationName, stationID variables
        //("nFixedVariables +" because 0=lon, 1=lat, 2=stationName, 3=stationID)
        dataVariables = new EDV[nFixedVariables + tDataVariables.length];

        lonIndex = 0;
        double stats[] = stationTable.getColumn(STATION_LON_COL).calculateStats();
        dataVariables[lonIndex] = new EDVLon(EDV.LON_NAME,
            null, null, "double", 
            stats[PrimitiveArray.STATS_MIN], stats[PrimitiveArray.STATS_MAX]); 

        latIndex = 1;
        stats = stationTable.getColumn(STATION_LAT_COL).calculateStats();
        dataVariables[latIndex] = new EDVLat(EDV.LAT_NAME,
            null, null, "double", 
            stats[PrimitiveArray.STATS_MIN], stats[PrimitiveArray.STATS_MAX]); 

        dataVariables[stationNameIndex /* 2 */] = new EDV("stationName", "station_name",
            null, 
            (new Attributes())
                .add("ioos_category", "Identifier")
                .add("long_name", "Station Name"), 
            "String"); //the constructor that reads actual_range 
        //no need to call setActualRangeFromDestinationMinMax() since they are NaNs

        dataVariables[stationIDIndex /* 3 */] = new EDV("stationID", "station_id",
            null, 
            (new Attributes())
                .add("ioos_category", "Identifier")
                .add("long_name", "Station ID"), 
            "String");  //.wsdl says xsd:string, not int   //the constructor that reads actual_range
        //no need to call setActualRangeFromDestinationMinMax() since they are NaNs

        //create the other dataVariables 
        for (int dv = 0; dv < tDataVariables.length; dv++) {
            String tSourceName = (String)tDataVariables[dv][0];
            String tDestName = (String)tDataVariables[dv][1];
            if (tDestName == null || tDestName.trim().length() == 0)
                tDestName = tSourceName;
            Attributes tSourceAtt = null; //(none available from source)
            Attributes tAddAtt = (Attributes)tDataVariables[dv][2];
            String tSourceType = (String)tDataVariables[dv][3];

            if (EDV.ALT_NAME.equals(tDestName)) {
                altIndex = nFixedVariables + dv;
                dataVariables[altIndex] = new EDVAlt(tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType, Double.NaN, Double.NaN);
            } else if (EDV.DEPTH_NAME.equals(tDestName)) {
                depthIndex = nFixedVariables + dv;
                dataVariables[depthIndex] = new EDVDepth(tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType, Double.NaN, Double.NaN);
            } else if (EDV.TIME_NAME.equals(tDestName)) {  //look for TIME_NAME before check hasTimeUnits (next)
                timeIndex = nFixedVariables + dv;
                dataVariables[timeIndex] = new EDVTime(tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType);
            } else if (EDVTimeStamp.hasTimeUnits(tSourceAtt, tAddAtt)) {
                dataVariables[nFixedVariables + dv] = new EDVTimeStamp(tSourceName, tDestName, 
                    tSourceAtt, tAddAtt,
                    tSourceType); //the constructor that reads actual_range
                dataVariables[nFixedVariables + dv].setActualRangeFromDestinationMinMax();
            } else {
                dataVariables[nFixedVariables + dv] = new EDV(tSourceName, tDestName, 
                    tSourceAtt, tAddAtt,
                    tSourceType); //the constructor that reads actual_range
                dataVariables[nFixedVariables + dv].setActualRangeFromDestinationMinMax();
            }
        }

        //ensure the setup is valid
        ensureValid();

        //finally
        if (verbose) String2.log(
            (reallyVerbose? "\n" + toString() : "") +
            "\n*** EDDTableFromNOS " + datasetID + " constructor finished. TIME=" + 
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
     */
    public void getDataForDapQuery(String loggedInAs, String requestUrl, 
        String userDapQuery, TableWriter tableWriter) throws Throwable {

        //get the sourceDapQuery (a query that the source can handle)
        StringArray resultsVariables    = new StringArray();
        StringArray constraintVariables = new StringArray();
        StringArray constraintOps       = new StringArray();
        StringArray constraintValues    = new StringArray();
        getSourceQueryFromDapQuery(userDapQuery,
            resultsVariables,
            constraintVariables, constraintOps, constraintValues); //timeStamp constraints other than regex are epochSeconds

        //no need to further prune constraints. 
        //constraints are used as needed below.
        //sourceCanConstrainNumericData = CONSTRAIN_PARTIAL; //stationID, lon, lat, time, but not others
        //sourceCanConstrainStringData  = CONSTRAIN_PARTIAL; //stationName anything, but others nothing
        //sourceCanConstrainStringRegex = PrimitiveArray.REGEX_OP; //partial: stationName only

        //convert constraintVariables to the dv index
        EDVTime timeVariable = timeIndex < 0? null :
            (EDVTime)dataVariables[timeIndex];
        int nConstraints = constraintVariables.size();
        IntArray constraintVariablesI = new IntArray();
        DoubleArray constraintValuesD = new DoubleArray();
        boolean queryJustStationInfo = true;
        for (int constraint = 0; constraint < nConstraints; constraint++) {
            //constraints should be for just x,y,z
            String sourceName = constraintVariables.get(constraint);
            int conDVI = String2.indexOf(dataVariableSourceNames(), sourceName);
            constraintVariablesI.add(conDVI);
            String conValue = constraintValues.get(constraint);
            double d = String2.parseDouble(conValue);
            constraintValuesD.add(d);
            if (conDVI >= nFixedVariables)
                queryJustStationInfo = false;
        }

        for (int rv = 0; rv < resultsVariables.size(); rv++) {
            int rvDVI = String2.indexOf(dataVariableSourceNames(), resultsVariables.get(rv));
            if (rvDVI >= nFixedVariables)
                queryJustStationInfo = false;
        }
        if (reallyVerbose) String2.log("  queryJustStationInfo=" + queryJustStationInfo);

        //set up the table
        Object o2[] = makeTable();
        Table table = (Table)o2[0];
        XMLReader xr = (XMLReader)o2[1];

        //go through the stations   
        int nStations = stationTable.nRows();
        STATION_LOOP:
        for (int station = 0; station < nStations; station++) {
            //get info from stationTable
            String tName      = stationTable.getColumn(STATION_NAME_COL).getString(station);
            String tID        = stationTable.getColumn(STATION_ID_COL).getString(station);
            double tLon       = stationTable.getColumn(STATION_LON_COL).getDouble(station);
            double tLat       = stationTable.getColumn(STATION_LAT_COL).getDouble(station);
            double tStartTime = stationTable.getColumn(STATION_TIME_COL).getDouble(station); //epochSeconds

            //NOS requires begin/end time be specified; default = get all
            //requestedBeginTime and EndTime are epochSeconds
            double requestedBeginTime = Double.isNaN(tStartTime)?
//EEEEK! NOS only allows 1 yr/request, but don't want to miss any data (if tStartTime not specified)
//I know they go back at least to 1938. 
//This only affects 2 stations.
                Calendar2.isoStringToEpochSeconds("1970-01-01") :  //[why not e.g., 1900]?
                tStartTime; 
            double requestedEndTime = Calendar2.gcToEpochSeconds(Calendar2.newGCalendarZulu()) +
                Calendar2.SECONDS_PER_HOUR; //now + 1 hr

            //go through constraints to find ones of interest
            boolean getData = true;
            for (int constraint = 0; constraint < nConstraints; constraint++) {
                //all constraints get passed to this method
                int conDVI = constraintVariablesI.get(constraint);
                String op = constraintOps.get(constraint);
                String sValue = constraintValues.get(constraint);
                double dValue = constraintValuesD.get(constraint);

                //lon or lat
                if (conDVI == lonIndex || conDVI == latIndex) {
                    double t = conDVI == lonIndex? tLon : tLat;
                    boolean pass = op.equals(PrimitiveArray.REGEX_OP)?
                        PrimitiveArray.testValueOpValue("" + t, op, sValue) :
                        PrimitiveArray.testValueOpValue(t, op, dValue);
                    if (!pass) {
                        //if (reallyVerbose) String2.log("  rejecting station=" + station + 
                        //    " because lon/lat failure: " + t + op + sValue);
                        getData = false;
                        break; //constraint loop
                    }

                //time
                } else if (conDVI == timeIndex) {
                    if (op.equals("=") || op.equals("<=") || op.equals("<")) {
                        //requestedEndTime and dValue are epochSeconds
                        requestedEndTime = Math.min(requestedEndTime, dValue); 

                        //can do crude test based on station start time
                        if (!Double.isNaN(tStartTime)) {
                            boolean pass = PrimitiveArray.testValueOpValue(tStartTime, "<", dValue);
                            if (!pass) {
                                //if (reallyVerbose) String2.log("  rejecting station=" + station + 
                                //    " because tStartTime=" + tStartTime + " >= <=constraint=" + dValue);
                                getData = false;
                                break;  //constraint loop
                            }
                        }
                    }
                    if (op.equals("=") || op.equals(">=") || op.equals(">")) {
                        requestedBeginTime = Math.max(requestedBeginTime, dValue); 
                    } 

                    //other ops (!=, =~) handled by standardizeResultsTable

                //stationName or stationID
                } else if (conDVI == stationNameIndex || conDVI == stationIDIndex) {
                    String t = conDVI == stationNameIndex? tName : tID;
                    boolean pass = PrimitiveArray.testValueOpValue(t, op, sValue);
                    if (!pass) {
                        //if (reallyVerbose) String2.log("  rejecting station=" + station + 
                        //    " because '" + t + "' " + op + " '" + sValue + "'.");
                        getData = false;
                        break;  //constraint loop
                    }

                } else {
                    //ignore constraints for other string variables
                }
            }

            //getData
            if (getData) {
                if (queryJustStationInfo) {
                    //add data to the nFixedVariables columns
                    table.getColumn(0).addDouble(tLon);
                    table.getColumn(1).addDouble(tLat);
                    table.getColumn(2).addString(tName);
                    table.getColumn(3).addString(tID);
                } else {
                    if (requestedBeginTime > requestedEndTime)  //epochSeconds
                        throw new SimpleException(MustBe.THERE_IS_NO_DATA + 
                            " (The requested beginTime=" +
                            Calendar2.epochSecondsToIsoStringT(requestedBeginTime) +
                            " is after the requested endTime=" +
                            Calendar2.epochSecondsToIsoStringT(requestedEndTime) + ".)");

                    //break request into lots of requests for smaller time periods.
                    //NOS can't handle request >= 366 days 
                    //and it has trouble with 365, so stick to fewer: maxDaysPerRequest
                    double tRequestedBeginTime = requestedBeginTime;
                    double tRequestedEndTime = Math.min(requestedEndTime,
                        requestedBeginTime + maxDaysPerRequest * Calendar2.SECONDS_PER_DAY);
                    while (true) {
                        String msg = "  getting data for station=" + tID + 
                            "  beginTime=" + Calendar2.epochSecondsToIsoStringT(tRequestedBeginTime) +
                            "  endTime=" + Calendar2.epochSecondsToIsoStringT(tRequestedEndTime);
                        if (reallyVerbose) String2.log(msg);
                        long getTime = System.currentTimeMillis();
                        int oldNRows = table.nRows();

                        //NOS recommended approach: almost verbatim from example at 
                        //http://opendap.co-ops.nos.noaa.gov/axis/webservices/wind/samples/client.html 
                        SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
                        SOAPConnection connection = soapConnectionFactory.createConnection();
                        SOAPFactory soapFactory = SOAPFactory.newInstance();
                        
                        MessageFactory factory = MessageFactory.newInstance();
                        SOAPMessage message = factory.createMessage();
                        
                        SOAPBody body = message.getSOAPBody();
                        Name bodyName = soapFactory.createName(getWhat, xmlns, wsdlUrl);
                        SOAPBodyElement bodyElement = body.addBodyElement(bodyName);
                        
                        //Constructing the body for the request
                        //NOS requires all elements
                        Name name = soapFactory.createName("stationId");
                        SOAPElement symbol = bodyElement.addChildElement(name);    
                        symbol.addTextNode(tID);    

                        name = soapFactory.createName("beginDate");    
                        symbol = bodyElement.addChildElement(name);
                        GregorianCalendar gc = Calendar2.epochSecondsToGc(tRequestedBeginTime);
                        //SimpleDateFormats are not thread-safe, so have to synchronize their use
                        synchronized (simpleRequestDateFormat) {
                            symbol.addTextNode(simpleRequestDateFormat.format(gc.getTime()));
                        }

                        name = soapFactory.createName("endDate");    
                        symbol = bodyElement.addChildElement(name);
                        gc = Calendar2.epochSecondsToGc(tRequestedEndTime);
                        //SimpleDateFormats are not thread-safe, so have to synchronize their use
                        synchronized (simpleRequestDateFormat) {
                            symbol.addTextNode(simpleRequestDateFormat.format(gc.getTime()));
                        }

                        name = soapFactory.createName("timeZone");
                        symbol = bodyElement.addChildElement(name);
                        symbol.addTextNode("0");  //always UTC
                        
                        //System.out.print("\nPrinting the message that is being sent: \n\n");
                        //message.writeTo(System.out);
                        //System.out.println("\n\n");
                        SOAPMessage response = connection.call(message, new URL(localSourceUrl));
                        connection.close();
                        
                        //there must be a better way to connect these two...
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        response.writeTo(baos);
                        String responseString = baos.toString("UTF-8");
                        baos = null; //allow garbage collection
                        //String2.log("response for stationID=" + tID + "=\n" + responseString);
                        int po1 = responseString.indexOf("<faultstring>");
                        if (po1 >= 0) {
                            po1 += 13;
                            int po2 = responseString.indexOf("</faultstring>", po1);
                            if (po2 < 0) 
                                po2 = responseString.length();
                            //fault may just be no data, so log it and move on
                            String2.log("Source faultString for =" + responseString.substring(po1, po2) +
                                "\n(" + datasetID + " " + msg + ")");
                        } else {
                            //process the xml data
                            StringReader in = new StringReader(responseString);
                            xr.parse(new InputSource(in));
                        }

                        /*               
                        //My more direct approach -- I never got this so work
                        StringBuilder requestSB = new StringBuilder(
                            "  <" + xmlns + ":" + getWhat + " xmlns:" + xmlns + "=\"" + wsdlUrl + "\">\n" +
                            "    <stationId xmlns=\"\">" + tID + "</stationId> \n");

                        //see example and info about date format at
                        //http://opendap.co-ops.nos.noaa.gov/axis/webservices/wind/index.jsp
                        //Example is odd: beginDate=endDate gets 1 day's data.
                        //   (beginDate has implied time 00:00; endDate has implied time 23:59).
                        //   Fine, but it also allows hh:mm, so then is more precise.
                        GregorianCalendar gc = Calendar2.epochSecondsToGc(tRequestedBeginTime);
                        //SimpleDateFormats are not thread-safe, so have to synchronize their use
                        synchronized (simpleRequestDateFormat) {
                            requestSB.append("    <beginDate xmlns=\"\">" + 
                                simpleRequestDateFormat.format(gc.getTime()) +
                                "</beginDate> \n");
                        }

                        gc = Calendar2.epochSecondsToGc(tRequestedEndTime);
                        //SimpleDateFormats are not thread-safe, so have to synchronize their use
                        synchronized (simpleRequestDateFormat) {
                            requestSB.append("    <endDate xmlns=\"\">" + 
                                simpleRequestDateFormat.format(gc.getTime()) +
                                "</endDate> \n");
                        }

                        requestSB.append(
                            "    <timeZone xmlns=\"\">0</timeZone> \n" +
                            "  </" + xmlns + ":" + getWhat + ">\n");
                        String request = requestSB.toString();

                        String2.log("\n  request=\n" + request);
                        String2.log("\n  response=\n" + SSR.getSoapString(sourceUrl, request, "") + "\n");

                        BufferedReader in = new BufferedReader(new InputStreamReader(
                            SSR.getSoapInputStream(sourceUrl, request, "")));
                        xr.parse(new InputSource(in));

                        */

                        //fill in correct lon, lat, stationName, stationID values from oldNRows to nRows
                        //xr.parse set them to ""
                        int nRows = table.nRows();
                        if (reallyVerbose) String2.log(
                            "  getTime=" + (System.currentTimeMillis() - getTime) +
                            " oldNRows=" + oldNRows + " nRows=" + nRows + " gotNRows=" + (nRows - oldNRows));
                        //if (reallyVerbose) 
                        //    String2.log("  station=" + station + " nRowsAdded=" + (nRows - oldNRows) + 
                        //        " id=" + tID + " lon=" + (float)tLon + " lat=" + (float)tLat);
                        for (int col = 0; col < nFixedVariables; col++) {
                            String s = col == 0? "" + tLon :
                                       col == 1? "" + tLat :
                                       col == 2? tName : tID;                               
                            StringArray sa = ((StringArray)table.getColumn(col));
                            for (int row = oldNRows; row < nRows; row++) 
                                sa.set(row, s);  //this is speedy and memory efficient (lots of references to same string object)
                        }

                        //write to tableWriter?
                        if (writeChunkToTableWriter(requestUrl, userDapQuery, table, tableWriter, false)) {
                            o2 = makeTable();
                            table = (Table)o2[0];
                            xr = (XMLReader)o2[1];
                            if (tableWriter.noMoreDataPlease) {
                                tableWriter.logCaughtNoMoreDataPlease(datasetID);
                                break STATION_LOOP;
                            }
                        }

                        //increment requested times   (epochSeconds)
                        if (tRequestedEndTime >= requestedEndTime)
                            break; 
                        tRequestedBeginTime = tRequestedEndTime + Calendar2.SECONDS_PER_MINUTE; //+1 minute
                        tRequestedEndTime = Math.min(requestedEndTime,
                            tRequestedBeginTime + maxDaysPerRequest * Calendar2.SECONDS_PER_DAY);

                    } 
                }
            }        
        }

        //do the final writeChunkToTableWriter
        writeChunkToTableWriter(requestUrl, userDapQuery, table, tableWriter, true);
    }


    private Object[] makeTable() throws Throwable {
        Table table = new Table();
        XMLReader xr = TableXmlHandler.getXmlReader(table, 
            false, //validate, 
            rowElementXPath, null); //rowElementAttributes);
        //always add columns for lon, lat, stationName, stationID
        //always StringArray columns because TableXmlHandler expects StringArrays
        for (int col = 0; col < nFixedVariables; col++)
            table.addColumn(col, dataVariables[col].sourceName(), new StringArray()); 
        return new Object[]{table, xr};
    }


    /**
     * This tests the methods in this class.
     *
     * @param doLongTest
     * @throws Throwable if trouble
     */
    public static void test(boolean doLongTest) throws Throwable {
        String2.log("\n****************** EDDTableFromNOS.test() *****************\n");
        verbose = true;
        reallyVerbose = true;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);

/* during development, test...
            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection connection = soapConnectionFactory.createConnection();
            SOAPFactory soapFactory = SOAPFactory.newInstance();
            
            MessageFactory factory = MessageFactory.newInstance();
            SOAPMessage message = factory.createMessage();
            
            SOAPBody body = message.getSOAPBody();
            Name bodyName = soapFactory.createName("getWind", "wind", "http://opendap.co-ops.nos.noaa.gov/axis/webservices/wind/wsdl");
            SOAPBodyElement bodyElement = body.addBodyElement(bodyName);
            
            //Constructing the body for the request
            Name name = soapFactory.createName("stationId");
            SOAPElement symbol = bodyElement.addChildElement(name);    
            symbol.addTextNode("8454000");    
            name = soapFactory.createName("beginDate");    
            symbol = bodyElement.addChildElement(name);
            symbol.addTextNode("20060101 00:00");
            name = soapFactory.createName("endDate");    
            symbol = bodyElement.addChildElement(name);
            symbol.addTextNode("20060101 00:18");
            name = soapFactory.createName("timeZone");
            symbol = bodyElement.addChildElement(name);
            symbol.addTextNode("0");

            
            System.out.print("\nPrinting the message that is being sent: \n\n");
            message.writeTo(System.out);
            System.out.println("\n\n");
            
            URL endpoint = new URL ("http://opendap.co-ops.nos.noaa.gov/axis/services/Wind");
            SOAPMessage response = connection.call(message, endpoint);
            connection.close();
            
            System.out.println("\nPrinting the respone that was recieved: \n\n" );
            response.writeTo(System.out);
*/

/*
//during development, test getSoapString
String sourceUrl = "http://opendap.co-ops.nos.noaa.gov/axis/services/Wind";
String request = 
"<wind:getWind xmlns:wind=\"http://opendap.co-ops.nos.noaa.gov/axis/webservices/wind/wsdl\">\n" +
"  <stationId xmlns=\"\">9759110</stationId>\n" +
"  <beginDate xmlns=\"\">20000101</beginDate>\n" +
"  <endDate xmlns=\"\">20010102</endDate>\n" +
"  <timeZone xmlns=\"\">0</timeZone>\n" +
"</wind:getWind>\n";
String2.log("\n  response=\n" + SSR.getSoapString(sourceUrl, request, 
    "http://opendap.co-ops.nos.noaa.gov/axis/webservices/wind/wsdl/getWind"
    ) + "\n");
*/

//        EDDTable wind = testDataset();  //should work
        EDDTable wind = (EDDTable)oneFromDatasetXml("nosCoopsWind"); //should work

        double tLon, tLat;
        String name, tName, results, expected, userDapQuery;
        String error = "";

        //getEmpiricalMinMax   just do once
        //wind.getEmpiricalMinMax("2007-02-01", "2007-02-01", false, true);
        //if (true) System.exit(1);

        //getMinMaxTime   just do once   
        //I did this repeatedly to do binary search to find start date, 
        //  testing different years, then months
        //  looking for even one station with data.
        //last failure 1991-05-01     treat this as min time
        //last successful 1991-06-01
        //wind.getEmpiricalMinMax("1991-06-01", "1991-06-01", false, true);
        //if (true) System.exit(1);

        //*** test getting das for entire dataset
        String2.log("\n****************** EDDTableFromNOS.test das dds for entire dataset\n");
        tName = wind.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            wind.className() + "_Entire", ".das"); 
        results = String2.annotatedString(new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray()));
        //String2.log(results);
        expected = //see OpendapHelper.EOL for comments
"Attributes {[10]\n" +
" s {[10]\n" +
"  longitude {[10]\n" +
"    String _CoordinateAxisType \"Lon\";[10]\n" +
"    Float64 actual_range -177.36, 167.7362;[10]\n" + //changes periodically
"    String axis \"X\";[10]\n" +
"    String ioos_category \"Location\";[10]\n" +
"    String long_name \"Longitude\";[10]\n" +
"    String standard_name \"longitude\";[10]\n" +
"    String units \"degrees_east\";[10]\n" +
"  }[10]\n" +
"  latitude {[10]\n" +
"    String _CoordinateAxisType \"Lat\";[10]\n" +
"    Float64 actual_range -14.28, 71.3601;[10]\n" + //changes periodically
"    String axis \"Y\";[10]\n" +
"    String ioos_category \"Location\";[10]\n" +
"    String long_name \"Latitude\";[10]\n" +
"    String standard_name \"latitude\";[10]\n" +
"    String units \"degrees_north\";[10]\n" +
"  }[10]\n" +
"  station_name {[10]\n" +
"    String ioos_category \"Identifier\";[10]\n" +
"    String long_name \"Station Name\";[10]\n" +
"  }[10]\n" +
"  station_id {[10]\n" +
"    String ioos_category \"Identifier\";[10]\n" +
"    String long_name \"Station ID\";[10]\n" +
"  }[10]\n" +
"  time {[10]\n" +
"    String _CoordinateAxisType \"Time\";[10]\n" +
"    Float64 actual_range 6.73056e+8, NaN;[10]\n" +
"    String axis \"T\";[10]\n" +
"    String ioos_category \"Time\";[10]\n" +
"    String long_name \"Time\";[10]\n" +
"    String standard_name \"time\";[10]\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";[10]\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";[10]\n" +
"  }[10]\n" +
"  wind_speed {[10]\n" +
"    Float64 colorBarMaximum 15.0;[10]\n" +
"    Float64 colorBarMinimum 0.0;[10]\n" +
"    String ioos_category \"Wind\";[10]\n" +
"    String long_name \"Wind Speed\";[10]\n" +
"    String standard_name \"wind_speed\";[10]\n" +
"    String units \"m s-1\";[10]\n" +
"  }[10]\n" +
"  wind_from_direction {[10]\n" +
"    Float64 colorBarMaximum 360.0;[10]\n" +
"    Float64 colorBarMinimum 0.0;[10]\n" +
"    String ioos_category \"Wind\";[10]\n" +
"    String long_name \"Wind From Direction\";[10]\n" +
"    String standard_name \"wind_from_direction\";[10]\n" +
"    String units \"degrees_true\";[10]\n" +
"  }[10]\n" +
"  wind_speed_of_gust {[10]\n" +
"    Float64 colorBarMaximum 30.0;[10]\n" +
"    Float64 colorBarMinimum 0.0;[10]\n" +
"    String ioos_category \"Wind\";[10]\n" +
"    String long_name \"Wind Speed of Gust\";[10]\n" +
"    String standard_name \"wind_speed_of_gust\";[10]\n" +
"    String units \"m s-1\";[10]\n" +
"  }[10]\n" +
"  MaxExceeded {[10]\n" +
"    Float64 colorBarMaximum 1.0;[10]\n" +
"    Float64 colorBarMinimum 0.0;[10]\n" +
"    String ioos_category \"Wind\";[10]\n" +
"    String long_name \"Max Value Was Exceeded\";[10]\n" +
"    String units \"0=False, 1=True\";[10]\n" +
"  }[10]\n" +
"  RateExceeded {[10]\n" +
"    Float64 colorBarMaximum 1.0;[10]\n" +
"    Float64 colorBarMinimum 0.0;[10]\n" +
"    String ioos_category \"Wind\";[10]\n" +
"    String long_name \"Rate of Change Tolerance Limit Was Exceeded\";[10]\n" +
"    String units \"0=False, 1=True\";[10]\n" +
"  }[10]\n" +
" }[10]\n" +
"  NC_GLOBAL {[10]\n" +
"    String cdm_data_type \"TimeSeries\";[10]\n" +
"    String cdm_timeseries_variables \"???\";[10]\n" +
"    String Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";[10]\n" +
"    Float64 Easternmost_Easting 167.7362;[10]\n" +    //these values change periodically
"    Float64 geospatial_lat_max 71.3601;[10]\n" +
"    Float64 geospatial_lat_min -14.28;[10]\n" +
"    String geospatial_lat_units \"degrees_north\";[10]\n" +
"    Float64 geospatial_lon_max 167.7362;[10]\n" +
"    Float64 geospatial_lon_min -177.36;[10]\n" +
"    String geospatial_lon_units \"degrees_east\";[10]\n" +
"    String history \"" + today + " http://opendap.co-ops.nos.noaa.gov/axis/services/Wind[10]\n" +
today + " " + EDStatic.erddapUrl + //in tests, always use non-https url
                "/tabledap/nosCoopsWind.das\";[10]\n" +
"    String infoUrl \"http://opendap.co-ops.nos.noaa.gov/axis/\";[10]\n" +
"    String institution \"NOAA NOS\";[10]\n" +
"    String license \"The data may be used and redistributed for free but is not intended[10]\n" +
"for legal use, since it may contain inaccuracies. Neither the data[10]\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any[10]\n" +
"of their employees or contractors, makes any warranty, express or[10]\n" +
"implied, including warranties of merchantability and fitness for a[10]\n" +
"particular purpose, or assumes any legal liability for the accuracy,[10]\n" +
"completeness, or usefulness, of this information.\";[10]\n" +
"    String Metadata_Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";[10]\n" +
"    Float64 Northernmost_Northing 71.3601;[10]\n" +
"    String sourceUrl \"http://opendap.co-ops.nos.noaa.gov/axis/services/Wind\";[10]\n" +
"    Float64 Southernmost_Northing -14.28;[10]\n" +
"    String standard_name_vocabulary \"CF-12\";[10]\n" +
"    String summary \"[Normally, the summary describes the dataset. Here, it describes[10]\n" +
"the server.][10]\n" +
"NOS CO-OPS has developed a web-based application that serves as an[10]\n" +
"interface for end-users to browse the available Web Services.[10]\n" +
"Available through this interface is a set of APIs, along with[10]\n" +
"examples of how end-users can write client-programs to access[10]\n" +
"CO-OPS Web Services. Any updates, as well as suggestions and[10]\n" +
"comments regarding additional features and services the end-users[10]\n" +
"would like to have will be communicated through this web interface.[10]\n" +
"[10]\n" +
"A set of Web Services to provide CO-OPS data in new format and[10]\n" +
"through new transport medium is now available. These Web Services[10]\n" +
"include water level, meteorological and ancillary data. In[10]\n" +
"addition, current or water velocity, tide predictions and active[10]\n" +
"stations are also available. The list of these Web Services will[10]\n" +
"continue to grow as we move forward and as CO-OPS and the users'[10]\n" +
"community see necessary.\";[10]\n" +
"    String time_coverage_start \"1991-05-01T00:00:00Z\";[10]\n" +
"    String title \"Buoy Wind Data from the NOAA NOS SOAP+XML Server\";[10]\n" +
"    Float64 Westernmost_Easting -177.36;[10]\n" +
"  }[10]\n" +
"}[10]\n" +
"[end]";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = wind.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            wind.className() + "_Entire", ".dds"); 
        results = String2.annotatedString(new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray()));
        //String2.log(results);
        expected = 
"Dataset {[10]\n" +
"  Sequence {[10]\n" +
"    Float64 longitude;[10]\n" +
"    Float64 latitude;[10]\n" +
"    String station_name;[10]\n" +
"    String station_id;[10]\n" +
"    Float64 time;[10]\n" +
"    Float64 wind_speed;[10]\n" +
"    Int16 wind_from_direction;[10]\n" +
"    Float64 wind_speed_of_gust;[10]\n" +
"    Byte MaxExceeded;[10]\n" +
"    Byte RateExceeded;[10]\n" +
"  } s;[10]\n" +
"} s;[10]\n" +
"[end]";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //*** test make data files
        String2.log("\n****************** EDDTableFromNOS.test make DATA FILES\n");       
        tLon = -71.4012; //pre 2009-01-11, was -71.4017; //-(71 + 24.1/60);
        tLat = 41.8071; ; //pre 2009-01-11, was 41.8067; //41 + 48.4/60;

        //.asc test of just station data
        userDapQuery = "longitude,latitude,station_name,station_id" +
            "&longitude=" + tLon + "&latitude=" + tLat; 
        tName = wind.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            wind.className() + "_station1", ".asc"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    Float64 longitude;\n" +
"    Float64 latitude;\n" +
"    String station_name;\n" +
"    String station_id;\n" +
"  } s;\n" +
"} s;\n" +
"---------------------------------------------\n" +
"s.longitude, s.latitude, s.station_name, s.station_id\n" +
"-71.4012, 41.8071, \"Providence\", \"8454000\"\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
        

        //.asc test of just station data          REGEX
        userDapQuery = "longitude,latitude,station_name,station_id" +
            "&station_name" + PrimitiveArray.REGEX_OP + "\"P.*e.*\""; 
        tName = wind.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            wind.className() + "_station2", ".asc"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected =   
"Dataset {\n" +
"  Sequence {\n" +
"    Float64 longitude;\n" +
"    Float64 latitude;\n" +
"    String station_name;\n" +
"    String station_id;\n" +
"  } s;\n" +
"} s;\n" +
"---------------------------------------------\n" +
"s.longitude, s.latitude, s.station_name, s.station_id\n" +
"-71.3393, 41.6372, \"Potter Cove, Prudence Island\", \"8452951\"\n" + //pre 2009-03-12 was -71.34, 41.635
"-71.4012, 41.8071, \"Providence\", \"8454000\"\n" + //moved 2009-01-11
"-75.1417, 39.9333, \"Philadelphia\", \"8545240\"\n" +  //moved
"-76.2667, 38.4833, \"Parsons Creek\", \"8576941\"\n" +
"-76.5333, 38.1333, \"Piney Point\", \"8578240\"\n" +
//"-76.8318, 34.5344, \"Pine Knoll Shores HOBO\", \"8657163\"\n" +  //2009-11-15 disappeared
//"-81.3867, 30.1333, \"Palm Valley ICWW\", \"8720398\"\n" +    //disappeared
//"-81.6317, 29.6433, \"Palatka, St Johns River\", \"8720774\"\n" +
"-80.915, 29.0633, \"Ponce De Leon Inlet South\", \"8721147\"\n" +
"-80.0667, 26.8433, \"PGA Boulevard Bridge, Palm Beach\", \"8722548\"\n" + //added 2009-08-25
"-80.0517, 26.77, \"Port of West Palm Beach\", \"8722588\"\n" +
"-82.5621, 27.6387, \"Port Manatee\", \"8726384\"\n" + //pre 2009-06-30 was -82.5656, 27.6367
"-87.2112, 30.4044, \"Pensacola\", \"8729840\"\n" +
"-88.5, 30.2133, \"Petit Bois Island, Port of Pascagoula\", \"8741003\"\n" +
"-97.2033, 27.8217, \"Port Ingleside, Corpus Christi Bay\", \"8775283\"\n" +
"-97.2367, 27.6333, \"Packery Channel\", \"8775792\"\n" +
"-97.43, 26.565, \"Port Mansfield\", \"8778490\"\n" +
"-97.215, 26.06, \"Port Isabel\", \"8779770\"\n" +
"-122.9767, 37.9961, \"Point Reyes\", \"9415020\"\n" +  //moved
"-123.44, 48.125, \"Port Angeles\", \"9444090\"\n" +
"-122.758, 48.1117, \"Port Townsend\", \"9444900\"\n" +
"-134.647, 56.2467, \"Port Alexander\", \"9451054\"\n" +
//"-150.413, 61.0367, \"Point Possession\", \"9455866\"\n" + //disappeard 2009-12-28
"-136.223, 57.9567, \"Pelican Harbor, Lisianski Inlet, AK\", \"9452611\"\n" +  //added 2010-03-02
"-160.562, 55.99, \"Port Moller\", \"9463502\"\n" +
"-148.527, 70.4, \"Prudhoe Bay\", \"9497645\"\n" +
"-66.7618, 17.9725, \"Penuelas (Punta Guayanilla)\", \"9758053\"\n";
        try {
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nTHIS IS ALWAYS CHANGING A LITTLE.  IS IT CLOSE ENOUGH?\n" +
                "Press ^C to stop or Enter to continue..."); 
        }
        
        //wimpy request (used because it works) suggested by 
        //http://opendap.co-ops.nos.noaa.gov/axis/webservices/wind/samples/client.html
        //for id 8454000    //has data back to 1938!
        String beginDate = "2006-01-01T00:00:00Z";
        String endDate = "2006-01-01T00:18:00Z";
        userDapQuery = "longitude,latitude,time,station_id,station_name," +
            "wind_speed,wind_from_direction,wind_speed_of_gust,MaxExceeded,RateExceeded" +
            "&longitude=" + tLon + "&latitude=" + tLat + "&time>=" + beginDate + "&time<=" + endDate;


        //.asc    test 1 lon,lat point
        tName = wind.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            wind.className() + "_Data", ".asc"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    Float64 longitude;\n" +
"    Float64 latitude;\n" +
"    Float64 time;\n" +
"    String station_id;\n" +
"    String station_name;\n" +
"    Float64 wind_speed;\n" +
"    Int16 wind_from_direction;\n" +
"    Float64 wind_speed_of_gust;\n" +
"    Byte MaxExceeded;\n" +
"    Byte RateExceeded;\n" +
"  } s;\n" +
"} s;\n" +
"---------------------------------------------\n" +
"s.longitude, s.latitude, s.time, s.station_id, s.station_name, s.wind_speed, s.wind_from_direction, s.wind_speed_of_gust, s.MaxExceeded, s.RateExceeded\n" +
"-71.4012, 41.8071, 1.1360736E9, \"8454000\", \"Providence\", 3.3, 33, 5.0, 0, 0\n" +
"-71.4012, 41.8071, 1.13607396E9, \"8454000\", \"Providence\", 2.1, 30, 4.1, 0, 0\n" +
"-71.4012, 41.8071, 1.13607432E9, \"8454000\", \"Providence\", 3.3, 34, 4.4, 0, 0\n" +
"-71.4012, 41.8071, 1.13607468E9, \"8454000\", \"Providence\", 3.0, 33, 4.0, 0, 0\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //sourceCanConstrainNumericData = CONSTRAIN_PARTIAL; //stationID, lon, lat, time, but not others
        //sourceCanConstrainStringData  = CONSTRAIN_PARTIAL; //stationName anything, but others nothing
        //sourceCanConstrainStringRegex = PrimitiveArray.REGEX_OP; //partial: stationName only

        //.csv    test 1 lon,lat point    test numeric > < for lon and wind_speed
        userDapQuery = "longitude,latitude,time,station_id,station_name,wind_from_direction" + //no wind_speed
            "&longitude>=" + (tLon-0.01) + "&longitude<" + (tLon+0.01) + 
            "&wind_speed>2.2&wind_speed<3.2" +  //wind_speed
            "&latitude=" + tLat + "&time>=" + beginDate + "&time<=" + endDate;
        tName = wind.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            wind.className() + "_Num", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"longitude, latitude, time, station_id, station_name, wind_from_direction\n" +
"degrees_east, degrees_north, UTC, , , degrees_true\n" +
"-71.4012, 41.8071, 2006-01-01T00:18:00Z, 8454000, Providence, 33\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv    test 1 lon,lat point    test station_name  > < 
        userDapQuery = "longitude,latitude,time,station_id,station_name,wind_speed,wind_from_direction" + 
            "&station_name>\"Prov\"&station_name<\"Prow\"" +
            "&time>=" + beginDate + "&time<=" + endDate;
        tName = wind.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            wind.className() + "_name", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"longitude, latitude, time, station_id, station_name, wind_speed, wind_from_direction\n" +
"degrees_east, degrees_north, UTC, , , m s-1, degrees_true\n" +
"-71.4012, 41.8071, 2006-01-01T00:00:00Z, 8454000, Providence, 3.3, 33\n" +
"-71.4012, 41.8071, 2006-01-01T00:06:00Z, 8454000, Providence, 2.1, 30\n" +
"-71.4012, 41.8071, 2006-01-01T00:12:00Z, 8454000, Providence, 3.3, 34\n" +
"-71.4012, 41.8071, 2006-01-01T00:18:00Z, 8454000, Providence, 3.0, 33\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv    test 1 lon,lat point    test station_id  > < 
        userDapQuery = "longitude,latitude,time,station_id,station_name,wind_speed,wind_from_direction" + 
            "&station_id>\"8454\"&station_id<\"8455\"" +
            "&time>=" + beginDate + "&time<=" + endDate;
        tName = wind.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            wind.className() + "_id", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"longitude, latitude, time, station_id, station_name, wind_speed, wind_from_direction\n" +
"degrees_east, degrees_north, UTC, , , m s-1, degrees_true\n" +
"-71.4012, 41.8071, 2006-01-01T00:00:00Z, 8454000, Providence, 3.3, 33\n" +
"-71.4012, 41.8071, 2006-01-01T00:06:00Z, 8454000, Providence, 2.1, 30\n" +
"-71.4012, 41.8071, 2006-01-01T00:12:00Z, 8454000, Providence, 3.3, 34\n" +
"-71.4012, 41.8071, 2006-01-01T00:18:00Z, 8454000, Providence, 3.0, 33\n" +
"-71.411, 41.5868, 2006-01-01T00:06:00Z, 8454049, Quonset Point, 5.0, 42\n" +
"-71.411, 41.5868, 2006-01-01T00:12:00Z, 8454049, Quonset Point, 5.0, 42\n" +
"-71.411, 41.5868, 2006-01-01T00:18:00Z, 8454049, Quonset Point, 5.0, 41\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

  
        //!!!Unfortunately, there are no String data variables to test
        


        userDapQuery = "longitude,latitude,time,station_id,station_name," +
            "wind_speed,wind_from_direction,wind_speed_of_gust,MaxExceeded,RateExceeded" +
            "&longitude=" + tLon + "&latitude=" + tLat + "&time>=" + beginDate + "&time<=" + endDate;

/* KNOWN BUG IN NOS SYSTEM (OCCURS WITH THEIR WEB PAGE, TOO)
  I REPORTED IT 2007-08-16
        //.asc    just 1 time point   SHORT TIME REQUESTS FAIL, LONGER SUCCEED (THERE MAY BE PROBLEM THERE, TOO)
        userDapQuery = "longitude,latitude,time,station_id,station_name," +
            "wind_speed,wind_from_direction,wind_speed_of_gust,MaxExceeded,RateExceeded" +
            //"&stationID=9411340&time>=1993-09-09&time<=1993-09-12"; //succeeds, including data on 9/9
            //"&stationID=9411340&time>=1993-09-09&time<=1993-09-11"; //succeeds, including data on 9/9
            "&stationID=9411340&time>=1993-09-09&time<=1993-09-10";  //fails: no data
            //"&stationID=9411340&time=1993-09-09"; //fails: no data
        tName = wind.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            wind.className() + "_DataLLRange", ".xhtml"); 
        results = String2.annotatedString(new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray()));
        SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        expected = 
"Dataset {[10]\n" +
"  Sequence {[10]\n" +
"    Float64 longitude;[10]\n" +
"    Float64 latitude;[10]\n" +
"    Float64 time;[10]\n" +
"    String station_id;[10]\n" +
"    String station_name;[10]\n" +
"    Float64 wind_speed;[10]\n" +
"    Int16 wind_from_direction;[10]\n" +
"    Float64 wind_speed_of_gust;[10]\n" +
"    Byte MaxExceeded;[10]\n" +
"    Byte RateExceeded;[10]\n" +
"  } nos_coops_wind;[10]\n" +
"} nos_coops_wind;[10]\n" +
"---------------------------------------------[10]\n" +
"nos_coops_wind.longitude, nos_coops_wind.latitude, nos_coops_wind.time, nos_coops_wind.station_id, nos_coops_wind.station_name, nos_coops_wind.wind_speed, nos_coops_wind.wind_from_direction, nos_coops_wind.wind_speed_of_gust, nos_coops_wind.MaxExceeded, nos_coops_wind.RateExceeded[10]\n" +
"...[10]\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
*/

/* WHEN PROBLEM ABOVE IS SOLVED, ADD...
        //.asc    test a range of lon,lat, just 1 time point
        userDapQuery = "longitude,latitude,time,station_id,station_name," +
            "wind_speed,wind_from_direction,wind_speed_of_gust,MaxExceeded,RateExceeded" +
            "&longitude>-125&longitude<-115&latitude>30&latitude<35&time=2006-01-01";
        tName = wind.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            wind.className() + "_DataLLRange", ".xhtml"); 
        results = String2.annotatedString(new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray()));
        SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        expected = 
"Dataset {[10]\n" +
"  Sequence {[10]\n" +
"    Float64 longitude;[10]\n" +
"    Float64 latitude;[10]\n" +
"    Float64 time;[10]\n" +
"    String station_id;[10]\n" +
"    String station_name;[10]\n" +
"    Float64 wind_speed;[10]\n" +
"    Int16 wind_from_direction;[10]\n" +
"    Float64 wind_speed_of_gust;[10]\n" +
"    Byte MaxExceeded;[10]\n" +
"    Byte RateExceeded;[10]\n" +
"  } nos_coops_wind;[10]\n" +
"} nos_coops_wind;[10]\n" +
"---------------------------------------------[10]\n" +
"nos_coops_wind.longitude, nos_coops_wind.latitude, nos_coops_wind.time, nos_coops_wind.stationID, nos_coops_wind.stationName, nos_coops_wind.WindSpeed, nos_coops_wind.WindDirection, nos_coops_wind.WindGust, nos_coops_wind.MaxExceeded, nos_coops_wind.RateExceeded[10]\n" +
"???";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
*/

        if (doLongTest) {
            //.asc test of longer time series
            String tUserDapQuery = "longitude,latitude,time,station_name," +
                "wind_speed,wind_from_direction,wind_speed_of_gust" +
                "&longitude=" + tLon + "&latitude=" + tLat + "&time>=" + beginDate + "&time<=2007-05-01"; //>366 days (their limit)
            tName = wind.makeNewFileForDapQuery(null, null, tUserDapQuery, EDStatic.fullTestCacheDirectory, 
                wind.className() + "_DataTime", ".asc"); 
            results = String2.annotatedString(new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray()));
            expected = 
"Dataset {[10]\n" +
"  Sequence {[10]\n" +
"    Float64 longitude;[10]\n" +
"    Float64 latitude;[10]\n" +
"    Float64 time;[10]\n" +
"    String station_name;[10]\n" +
"    Float64 wind_speed;[10]\n" +
"    Int16 wind_from_direction;[10]\n" +
"    Float64 wind_speed_of_gust;[10]\n" +
"  } nos_coops_wind;[10]\n" +
"} nos_coops_wind;[10]\n" +
"---------------------------------------------[10]\n" +
"nos_coops_wind.longitude, nos_coops_wind.latitude, nos_coops_wind.time, nos_coops_wind.station_name, nos_coops_wind.wind_speed, nos_coops_wind.wind_from_direction, nos_coops_wind.wind_speed_of_gust[10]\n" +
"-71.4017, 41.8067, 1.1360736E9, \"Providence\", 3.3, 33, 5.0[10]\n" +
"-71.4017, 41.8067, 1.13607396E9, \"Providence\", 2.1, 30, 4.1[10]\n" +
"-71.4017, 41.8067, 1.13607432E9, \"Providence\", 3.3, 34, 4.4[10]\n" +
"-71.4017, 41.8067, 1.13607468E9, \"Providence\", 3.0, 33, 4.0[10]\n";
            Test.ensureTrue(results.startsWith(expected), "\nresults=\n" + results.substring(0, 1000));
            //2006-01-04 == 1136332800
            expected = 
"-71.4017, 41.8067, 1.1363328E9, \"Providence\", 7.6, 23, 10.6[10]\n";
            Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results.substring(0, 1000));
            //2007-05-01 == 1177977600
            expected = 
"-71.4017, 41.8067, 1.1779776E9, \"Providence\", 7.2, 331, 9.3[10]\n";
            Test.ensureTrue(results.indexOf(expected) > 0, 
                "\nresults=\n" + results.substring(results.length() - 200, results.length()));

        //data for mapExample
        tName = wind.makeNewFileForDapQuery(null, null, "longitude,latitude&time>=2007-12-01&time<=2007-12-02", 
            EDStatic.fullTestCacheDirectory, wind.className() + "Map", ".csv");
        results = String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1];
        expected = 
"longitude, latitude\n" +
"degrees_east, degrees_north\n" +
"180.099, 0.032\n" +
"189.971, -7.98\n" +
"235.016, -7.992\n" +
"165.189, -4.997\n" +
"189.966, 7.996\n" +
"205.006, -4.992\n" +
"264.994, 4.949\n" +
"264.737, -8.013\n" +
"235.595, 0.0\n" +
"219.975, 1.978\n" +
"264.828, -1.983\n" +
"204.987, -1.983\n" +
"220.073, -4.965\n" +
"164.883, -8.032\n" +
"189.929, -0.039\n" +
"218.7, 0.0\n" +
"235.117, 5.102\n" +
"165.016, 5.033\n" +
"249.898, 5.039\n" +
"204.988, -8.263\n" +
"164.998, -1.969\n" +
"180.123, -1.984\n" +
"264.938, -5.083\n" +
"180.122, 7.994\n" +
"190.003, 2.025\n" +
"264.978, 0.0\n" +
"180.207, 2.046\n" +
"180.095, 4.987\n" +
"205.026, 7.972\n" +
"234.971, -2.006\n" +
"249.855, 8.056\n" +
"165.061, 8.04\n" +
"204.958, 2.018\n" +
"235.027, 8.044\n" +
"220.041, -2.02\n" +
"264.679, 1.998\n" +
"249.919, -1.923\n" +
"220.03, 5.01\n" +
"165.013, 2.013\n" +
"235.049, -5.013\n" +
"186.719, -4.903\n" +
"249.776, 1.96\n" +
"265.053, 8.063\n" +
"250.035, 0.0\n" +
"165.026, 0.0\n" +
"190.001, 5.026\n" +
"219.745, 8.999\n" +
"180.142, -7.982\n" +
"204.843, -0.093\n" +
"180.103, -4.972\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);  
        
        }
        
    }



    /** This tries to get data from http://cdmo.baruch.sc.edu/webservices/index.cfm */
    public static void tryCdmoOld() throws Exception {

//<?php require_once('lib/nusoap.php');
//$wsdl=new soapclient('http://cdmo.baruch.sc.edu/webservices/xmldatarequest.cfc?wsdl');
//$wsdl->call('');
//echo 'Response: <xmp>'.$wsdl->response.'</xmp>';
//?>
        String getWhat = "exportStationCodesXML";
        String xmlns = "";
        String wsdlUrl = "http://cdmo.baruch.sc.edu/webservices/xmldatarequest.cfc?wsdl";
        String sourceUrl = "http://cdmo.baruch.sc.edu/webservices/xmldatarequest.cfc";

        SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
        SOAPConnection connection = soapConnectionFactory.createConnection();
        SOAPFactory soapFactory = SOAPFactory.newInstance();
        
        MessageFactory factory = MessageFactory.newInstance();
        SOAPMessage message = factory.createMessage();
        
        SOAPBody body = message.getSOAPBody();
        Name bodyName = soapFactory.createName(getWhat, xmlns, wsdlUrl);
        SOAPBodyElement bodyElement = body.addBodyElement(bodyName);

        /*
        //Constructing the body for the request
        //NOS requires all elements
        Name name = soapFactory.createName("stationId");
        SOAPElement symbol = bodyElement.addChildElement(name);    
        symbol.addTextNode(tID);    

        name = soapFactory.createName("beginDate");    
        symbol = bodyElement.addChildElement(name);
        GregorianCalendar gc = Calendar2.epochSecondsToGc(tRequestedBeginTime);
        //SimpleDateFormats are not thread-safe, so have to synchronize their use
        synchronized (simpleRequestDateFormat) {
            symbol.addTextNode(simpleRequestDateFormat.format(gc.getTime()));
        }

        name = soapFactory.createName("endDate");    
        symbol = bodyElement.addChildElement(name);
        gc = Calendar2.epochSecondsToGc(tRequestedEndTime);
        //SimpleDateFormats are not thread-safe, so have to synchronize their use
        synchronized (simpleRequestDateFormat) {
            symbol.addTextNode(simpleRequestDateFormat.format(gc.getTime()));
        }

        name = soapFactory.createName("timeZone");
        symbol = bodyElement.addChildElement(name);
        symbol.addTextNode("0");  //always UTC
        */
        //System.out.print("\nPrinting the message that is being sent: \n\n");
        //message.writeTo(System.out);
        //System.out.println("\n\n");
        SOAPMessage soapResponse = connection.call(message, new URL(sourceUrl));
        connection.close();
        
        //there must be a better way to connect these two...
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        soapResponse.writeTo(baos);
        String response = baos.toString("UTF-8");
        baos = null; //allow garbage collection
        String2.log("\nresponse=\n" + response);
/*
<?xml version="1.0" encoding="utf-8"?><soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:xsd="http://www.w3.o
rg/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
 <soapenv:Body>
  <ns1:exportStationCodesXMLResponse soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" xmlns:ns1="http://cdmo.baruch.sc.edu/
webservices/xmldatarequest.cfc?wsdl">
   <exportStationCodesXMLReturn xsi:type="ns2:Document" xmlns:ns2="http://xml.apache.org/xml-soap">
    <nds>
     <data>
      <r>
       <c v="NERR_Site_ID"/>
       <c v="Station_Code"/>
       <c v="Station_Name"/>
       <c v="Lat_Long"/>
       <c v="Latitude"/>
       <c v="Longitude"/>
       <c v="Status"/>
       <c v="Active_Dates"/>
       <c v="State"/>
       <c v="Reserve_Name"/>
       <c v="Params_Reported"/>
       <c v="Real_Time"/>
      </r>
      <r>
       <c v="ace                                               "/>
       <c v="acebbnut  "/>
       <c v="Big Bay                                 "/>
       <c v="32&#xB0; 29' 38.76 N, 80&#xB0; 19' 26.76 W"/>
       <c v="32.4941"/>
       <c v="80.3241"/>
       <c v="Active    "/>
       <c v="Feb 2002-"/>
       <c v="sc        "/>
       <c v="Ashepoo Combahee Edisto Basin                     "/>
       <c v="NO23F,PO4F,CHLA_N,NO3F,NO2F,DIN,NH4F"/>
       <c v="null"/>
      </r>
      <r>
       <c v="ace                                               "/>
       <c v="acebpmet  "/>
       <c v="Bennett's Point                         "/>
       <c v="32&#xB0; 33' 31.32 N, 80&#xB0; 27' 13.32 W"/>
       <c v="32.5587"/>
       <c v="80.4537"/>
       <c v="Active    "/>
       <c v="Mar 2001-"/>
       <c v="sc        "/>
       <c v="Ashepoo Combahee Edisto Basin                     "/>
       <c v="ATemp,RH,BP,WSpd,MaxWSpd,MaxWSpdT,Wdir,SDWDir,TotPrcp,TotPAR,CumPrcp"/>
       <c v="R         "/>
      </r>
*/
    }


    /** This tries to get data from http://cdmo.baruch.sc.edu/webservices/index.cfm */
    public static void tryCdmo() throws Exception {
        testVerboseOn();

        String wsdlUrl = "http://cdmo.baruch.sc.edu/webservices/xmldatarequest.cfc?wsdl";
        String sourceUrl = "http://cdmo.baruch.sc.edu/webservices/xmldatarequest.cfc";
        String getWhat = "exportStationCodesXML";

        requestSoap(wsdlUrl, sourceUrl, getWhat);
/*
<?xml version="1.0" encoding="utf-8"?><soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:xsd="http://www.w3.o
rg/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
 <soapenv:Body>
  <ns1:exportStationCodesXMLResponse soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" xmlns:ns1="http://cdmo.baruch.sc.edu/
webservices/xmldatarequest.cfc?wsdl">
   <exportStationCodesXMLReturn xsi:type="ns2:Document" xmlns:ns2="http://xml.apache.org/xml-soap">
    <nds>
     <data>
      <r>
       <c v="NERR_Site_ID"/>
       <c v="Station_Code"/>
       <c v="Station_Name"/>
       <c v="Lat_Long"/>
       <c v="Latitude"/>
       <c v="Longitude"/>
       <c v="Status"/>
       <c v="Active_Dates"/>
       <c v="State"/>
       <c v="Reserve_Name"/>
       <c v="Params_Reported"/>
       <c v="Real_Time"/>
      </r>
      <r>
       <c v="ace                                               "/>
       <c v="acebbnut  "/>
       <c v="Big Bay                                 "/>
       <c v="32&#xB0; 29' 38.76 N, 80&#xB0; 19' 26.76 W"/>
       <c v="32.4941"/>
       <c v="80.3241"/>
       <c v="Active    "/>
       <c v="Feb 2002-"/>
       <c v="sc        "/>
       <c v="Ashepoo Combahee Edisto Basin                     "/>
       <c v="NO23F,PO4F,CHLA_N,NO3F,NO2F,DIN,NH4F"/>
       <c v="null"/>
      </r>
      <r>
       <c v="ace                                               "/>
       <c v="acebpmet  "/>
       <c v="Bennett's Point                         "/>
       <c v="32&#xB0; 33' 31.32 N, 80&#xB0; 27' 13.32 W"/>
       <c v="32.5587"/>
       <c v="80.4537"/>
       <c v="Active    "/>
       <c v="Mar 2001-"/>
       <c v="sc        "/>
       <c v="Ashepoo Combahee Edisto Basin                     "/>
       <c v="ATemp,RH,BP,WSpd,MaxWSpd,MaxWSpdT,Wdir,SDWDir,TotPrcp,TotPAR,CumPrcp"/>
       <c v="R         "/>
      </r>
*/
    }


    /** This tries to get data from http://cdmo.baruch.sc.edu/webservices/index.cfm 
     * @param wsdlUrl e.g., "http://cdmo.baruch.sc.edu/webservices/xmldatarequest.cfc?wsdl";
     * @param sourceUrl e.g., "http://cdmo.baruch.sc.edu/webservices/xmldatarequest.cfc";
     * @param getWhat e.g., "exportStationCodesXML";
     */
    public static Table requestSoap(String wsdlUrl, String sourceUrl, String getWhat) 
        throws Exception {

        boolean testMode = true;
        String xmlns = "";
        String stationCodesFileName = "c:/u00/cwatch/testData/nerrs/stationCodes.xml";
        InputStream is = null;

        if (testMode && getWhat.equals("exportStationCodesXML")) {
            String2.log("\n*** getting station codes from " + stationCodesFileName); Math2.sleep(3000);
            is = new FileInputStream(stationCodesFileName);

        } else {

            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection connection = soapConnectionFactory.createConnection();
            SOAPFactory soapFactory = SOAPFactory.newInstance();
            
            MessageFactory factory = MessageFactory.newInstance();
            SOAPMessage sendMessage = factory.createMessage();
            
            SOAPBody body = sendMessage.getSOAPBody();
            Name bodyName = soapFactory.createName(getWhat, xmlns, wsdlUrl);
            SOAPBodyElement bodyElement = body.addBodyElement(bodyName);

            if (reallyVerbose) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                sendMessage.writeTo(baos);
                String2.log("\nsendMessage:\n" + baos.toString("UTF-8"));
            }
            SOAPMessage receiveMessage = connection.call(sendMessage, new URL(sourceUrl));
            connection.close();
            
            //there must be a better way to connect these two...
            ByteArrayOutputStream baos = new ByteArrayOutputStream();  
            receiveMessage.writeTo(baos);
            //if (reallyVerbose) String2.log("\nresponse=\n" + baos.toString("UTF-8"));
            is = new ByteArrayInputStream(baos.toByteArray());

            //one time: store stationCodes in a file
            if (false) {
                FileOutputStream fos = new FileOutputStream(stationCodesFileName);
                receiveMessage.writeTo(fos);
                fos.close();
            }
        }

        Table table = new Table();
        SimpleXMLReader xmlReader = new SimpleXMLReader(is);

        //read to <data>
        String tags;
        do {
            xmlReader.nextTag();
            tags = xmlReader.allTags();
            //if (reallyVerbose) String2.log("  tags=" + tags);
        } while (!tags.endsWith("<data>"));

        String startOfTags = xmlReader.allTags();
        int startOfTagsN = xmlReader.stackSize();
        int startOfTagsLength = startOfTags.length();
        int row = -1, col = -1, nCols = -1;
        boolean usual = false;
        while (true) {
            xmlReader.nextTag();
            tags = xmlReader.allTags();
//            if (reallyVerbose) String2.log("  tags=" + tags);
            if (xmlReader.stackSize() == startOfTagsN) 
                break; //the </data> tag
            String localTags = tags.substring(startOfTagsLength);

            //try to make the tag names as consistent, descriptive and readable as possible
            if      (localTags.equals("<r>")) {
                col = -1;
            } else if (localTags.equals("</r>")) {
                if (row == -1) {
                    nCols = table.nColumns();
                } else if ((col+1) != nCols) {
                    throw new SimpleException(
                        "Row=" + row + " had the wrong number of columns (" + (col+1) + "!=" + nCols + 
                        ") on XML line #" + xmlReader.lineNumber() + ".");
                }
                row++;
            } else if (localTags.equals("<r><c>")) {
                col++;
                String v = xmlReader.attributeValue("v");
                if (v == null)
                    throw new SimpleException(
                        "Unexpected response from server: no 'v' attribute for <c> tag on XML line #" +  
                        xmlReader.lineNumber() + ".");
//                if (reallyVerbose) String2.log("    v=\"" + v + "\"");
                v = v.trim();
                if (row == -1) {
                    table.addColumn(v, new StringArray());
                } else {
                    table.getColumn(col).addString(v.equals("null")? "" : v);
                }
            } else if (localTags.equals("<r></c>")) {
            } else {
                xmlReader.unexpectedTagException();
            }
        }

        if (verbose) String2.log("\n***table=\n" + table.saveAsCsvASCIIString());

        return table;
    }

}
