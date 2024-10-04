/*
 * EDDTableFromSOS Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
import com.cohort.array.PAOne;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.Units2;
import com.cohort.util.XML;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.handlers.EDDTableFromSOSHandler;
import gov.noaa.pfel.erddap.handlers.SaxHandlerClass;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;

/**
 * This class aggregates data from a group of stations, all served by one SOS server. The stations
 * all serve the same set of variables (although the source for each station doesn't have to serve
 * all variables).
 *
 * <p>The SWE (Sensor Web Enablement) and SOS (Sensor Observation Service) home page is
 * https://www.opengeospatial.org/standards/sos .
 *
 * <p>See OpenGIS® Sensor Observation Service Implementation Specification (OGC 06-009r6) Version:
 * 1.0.0 https://www.opengeospatial.org/standards/sos . (Bob has a copy in c:/projects/sos)
 *
 * <p>See OGC Web Services Common Specification ver 1.1.0 (OGC 06-121r3) from
 * https://www.opengeospatial.org/standards/common which covers construction of GET and POST queries
 * (e.g., section 7.2.3 and section 9). (Bob has a copy in c:/projects/sos)
 *
 * <p>SOS overview:
 *
 * <ul>
 *   <li>If you send a getCapabilities xml request to a SOS server (sourceUrl +
 *       "?service=SOS&request=GetCapabilities"), you get an xml result with a list of stations and
 *       the observedProperties that they have data for. e.g.,
 *   <li>An observedProperty is a formal URI reference to a property e.g.,
 *       urn:ogc:phenomenon:longitude:wgs84 or (was
 *       http://marinemetadata.org/cf#sea_water_temperature - http://marinemetadata.org is GONE!)
 *       http://mmisw.org/ont/cf/parameter/sea_water_temperature
 *   <li>An observedProperty isn't a variable.
 *   <li>More than one variable may have the same observedProperty (e.g., insideTemp and outsideTemp
 *       might both have observedProperty (was http://marinemetadata.org/cf#air_temperature -
 *       http://marinemetadata.org is GONE!) http://mmisw.org/ont/cf/parameter/air_temperature See
 *       the observedProperty schema to find the variables associated with each observedProperty.
 *   <li>If you send a getObservation xml request to a SOS server, you get an xml result with
 *       descriptions of field names in the response, field units, and the data. The field names
 *       will include longitude, latitude, depth(perhaps), and time.
 *   <li>This class just gets data for one station at a time to avoid requests for huge amounts of
 *       data at one time (which ties up memory and is slow getting initial response to user).
 *   <li>Currently, some SOS servers (e.g., GoMOOS in the past) respond to getObservation requests
 *       for more than one observedProperty by just returning results for the first of the
 *       observedProperties. (No error message!) See the constructor parameter
 *       tRequestObservedPropertiesSeparately.
 *   <li>!!!The getCapabilities result doesn't include the results variables (they say field names).
 *       The only way to know them ahead of time is to request some data, look at the xml, and note
 *       the field name (and units). E.g., For a given station (e.g., D01) and observedProperty
 *       (e.g., ...sea_water_temperature) look at the results from the getObservation url:
 *       http://dev.gomoos.org/cgi-bin/sos/oostethys_sos?REQUEST=GetObservation&OFFERING=D01&OBSERVEDPROPERTY=sea_water_temperature&TIME=2007-06-01T00:00:00/2007-06-24T23:59:00
 *       Similarly, getCapabilities doesn't include altitude/depth information. But maybe this info
 *       is in the getDescription results (info for specific station).
 * </ul>
 *
 * <p>See Phenomenon dictionary at
 * https://www.seegrid.csiro.au/subversion/xmml/OGC/branches/SWE_gml2/sweCommon/current/examples/phenomena.xml
 *
 * <p>Info about a sample server: https://www.coast.noaa.gov/DTL/dtl_proj3_oostethys.html <br>
 * The csc server uses code from
 * http://www.oostethys.org/ogc-oceans-interoperability-experiment/experiment-1 . [GONE] <br>
 * It seems to be set up to just serve several instances of lon,lat,alt,time,1variable instead of
 * lon,lat,alt,time,manyVariables.(?)
 *
 * <p>Sample queries for different time periods
 * http://www.oostethys.org/Members/tcook/sos-xml-time-encodings/?searchterm=Time [GONE]
 *
 * <p>In general, if there is an error in processing the getCapabilities xml for one station, only
 * that station is rejected (with a message to the log.txt file). So it is good to read the logs
 * periodically to see if there are unexpected errors.
 *
 * <p>This class insists that each station is indeed a station at one lat lon point: lowerCorner's
 * lat lon must equal upperCorner's lat lon or the station is ignored.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-09-21
 */
@SaxHandlerClass(EDDTableFromSOSHandler.class)
public class EDDTableFromSOS extends EDDTable {

  /** stationTable */
  protected static final int stationLonCol = 0;

  protected static final int stationLatCol = 1;
  protected static final int stationBeginTimeCol = 2; // epochSeconds
  protected static final int stationEndTimeCol = 3; // epochSeconds
  protected static final int stationIDCol = 4;
  protected static final int stationProcedureCol = 5;
  protected static final String defaultStationIdSourceName = "station_id";
  protected static final String stationIdDestinationName = "station_id";
  static String sosCopyDir = EDStatic.fullCopyDirectory + "_SOS_cache/";

  /**
   * The first nFixedVariables dataVariables are always created automatically (don't include in
   * constructor tDataVariables): longitude, latitude, stationID, altitude, time.
   */
  protected static final int nFixedVariables = 5;

  private static boolean testQuickRestart =
      false; // to test, set this to true in test method, not here.
  protected static boolean timeParts =
      false; // some test methods set this to true for timing test purposes only

  /** Variables set by the constructor. */
  protected String sosVersion;

  protected String lonSourceName, latSourceName, altSourceName, timeSourceName, stationIdSourceName;
  protected boolean requestObservedPropertiesSeparately;
  protected String responseFormat; // "" -> default
  protected String bboxOffering; // offering name to use with lon lat BBOX request
  protected String bboxParameter; // parameter prefix to use with lon lat BBOX request
  protected StringArray uniqueSourceObservedProperties;
  protected Table stationTable;
  protected boolean[][] stationHasObsProp; // [station#][uniqueSourceObservedProperties index]
  protected String sosServerType = ""; // may be "".   This variable will be lowercased.
  public static final String SosServerTypeIoos52N = "IOOS_52N";
  public static final String SosServerTypeIoosNdbc = "IOOS_NDBC";
  public static final String SosServerTypeIoosNcSOS = "IOOS_NcSOS";
  public static final String SosServerTypeIoosNos = "IOOS_NOS";
  public static final String SosServerTypeOostethys = "OOSTethys";
  public static final String SosServerTypeWhoi = "WHOI";
  public static final String[] SosServerTypes = { // order not important
    SosServerTypeIoos52N, SosServerTypeIoosNdbc, SosServerTypeIoosNcSOS,
    SosServerTypeIoosNos, SosServerTypeOostethys, SosServerTypeWhoi
  };
  public static final String slowSummaryWarning =
      "\n\nThe source SOS server for this dataset is very slow, so requests will "
          + "take minutes to be fulfilled or will fail because of a timeout.";
  protected boolean ioos52NServer = false;
  protected boolean ioosNdbcServer = false;
  protected boolean ioosNcSOSServer = false;
  protected boolean ioosNosServer = false;
  protected boolean oostethysServer = false;
  protected boolean whoiServer = false; // one must be specified

  public static String defaultResponseFormat(String tSosServerType) {
    tSosServerType = tSosServerType == null ? "" : tSosServerType.toLowerCase();
    if (SosServerTypeIoos52N.toLowerCase().equals(tSosServerType))
      return "text/xml; subtype=\"om/1.0.0/profiles/ioos_sos/1.0\"";
    if (SosServerTypeIoosNdbc.toLowerCase().equals(tSosServerType)
        || SosServerTypeIoosNos.toLowerCase().equals(tSosServerType)) return "text/csv";
    return "text/xml; subtype=\"om/1.0.0\"";
  }

  /**
   * This constructs an EDDTableFromSOS based on the information in an .xml file.
   *
   * @param erddap if known in this context, else null
   * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableFromSOS"&gt; having
   *     just been read.
   * @return an EDDTableFromSOS. When this returns, xmlReader will have just read
   *     &lt;erddapDatasets&gt;&lt;/dataset&gt; .
   * @throws Throwable if trouble
   */
  @EDDFromXmlMethod
  public static EDDTableFromSOS fromXml(Erddap erddap, SimpleXMLReader xmlReader) throws Throwable {

    // data to be obtained (or not)
    if (verbose) String2.log("\n*** constructing EDDTableFromSOS(xmlReader)...");
    String tDatasetID = xmlReader.attributeValue("datasetID");
    Attributes tGlobalAttributes = null;
    String tSosServerType = "";
    String tStationIdSourceName = defaultStationIdSourceName;
    String tLongitudeSourceName = null;
    String tLatitudeSourceName = null;
    String tAltitudeSourceName = null; // currently alt, not depth
    double tAltitudeMetersPerSourceUnit = 1;
    double tAltitudeSourceMinimum = Double.NaN;
    double tAltitudeSourceMaximum = Double.NaN;
    String tTimeSourceName = null;
    String tTimeSourceFormat = null;
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

    // process the tags
    String startOfTags = xmlReader.allTags();
    int startOfTagsN = xmlReader.stackSize();
    int startOfTagsLength = startOfTags.length();
    while (true) {
      xmlReader.nextTag();
      String tags = xmlReader.allTags();
      String content = xmlReader.content();
      // if (reallyVerbose) String2.log("  tags=" + tags + content);
      if (xmlReader.stackSize() == startOfTagsN) break; // the </dataset> tag
      String localTags = tags.substring(startOfTagsLength);

      // try to make the tag names as consistent, descriptive and readable as possible
      if (localTags.equals("<addAttributes>")) tGlobalAttributes = getAttributesFromXml(xmlReader);
      else if (localTags.equals("<sosVersion>")) {
      } else if (localTags.equals("</sosVersion>")) tSosVersion = content;
      else if (localTags.equals("<sosServerType>")) {
      } else if (localTags.equals("</sosServerType>")) tSosServerType = content;
      else if (localTags.equals("<stationIdSourceName>")) {
      } else if (localTags.equals("</stationIdSourceName>")) tStationIdSourceName = content;
      else if (localTags.equals("<longitudeSourceName>")) {
      } else if (localTags.equals("</longitudeSourceName>")) tLongitudeSourceName = content;
      else if (localTags.equals("<latitudeSourceName>")) {
      } else if (localTags.equals("</latitudeSourceName>")) tLatitudeSourceName = content;
      else if (localTags.equals("<altitudeSourceName>")) {
      } else if (localTags.equals("</altitudeSourceName>")) tAltitudeSourceName = content;
      else if (localTags.equals("<altitudeSourceMinimum>")) {
      } else if (localTags.equals("</altitudeSourceMinimum>"))
        tAltitudeSourceMinimum = String2.parseDouble(content);
      else if (localTags.equals("<altitudeSourceMaximum>")) {
      } else if (localTags.equals("</altitudeSourceMaximum>"))
        tAltitudeSourceMaximum = String2.parseDouble(content);
      else if (localTags.equals("<altitudeMetersPerSourceUnit>")) {
      } else if (localTags.equals("</altitudeMetersPerSourceUnit>"))
        tAltitudeMetersPerSourceUnit = String2.parseDouble(content);
      else if (localTags.equals("<timeSourceName>")) {
      } else if (localTags.equals("</timeSourceName>")) tTimeSourceName = content;
      else if (localTags.equals("<timeSourceFormat>")) {
      } else if (localTags.equals("</timeSourceFormat>")) tTimeSourceFormat = content;
      else if (localTags.equals("<dataVariable>"))
        tDataVariables.add(getSDADVariableFromXml(xmlReader));
      else if (localTags.equals("<accessibleTo>")) {
      } else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
      else if (localTags.equals("<graphsAccessibleTo>")) {
      } else if (localTags.equals("</graphsAccessibleTo>")) tGraphsAccessibleTo = content;
      else if (localTags.equals("<reloadEveryNMinutes>")) {
      } else if (localTags.equals("</reloadEveryNMinutes>"))
        tReloadEveryNMinutes = String2.parseInt(content);
      else if (localTags.equals("<sourceUrl>")) {
      } else if (localTags.equals("</sourceUrl>")) tLocalSourceUrl = content;
      else if (localTags.equals("<observationOfferingIdRegex>")) {
      } else if (localTags.equals("</observationOfferingIdRegex>"))
        tObservationOfferingIdRegex = content;
      else if (localTags.equals("<requestObservedPropertiesSeparately>")) {
      } else if (localTags.equals("</requestObservedPropertiesSeparately>"))
        tRequestObservedPropertiesSeparately = content.equals("true");
      else if (localTags.equals("<responseFormat>")) {
      } else if (localTags.equals("</responseFormat>")) tResponseFormat = content;
      else if (localTags.equals("<bboxOffering>")) {
      } else if (localTags.equals("</bboxOffering>")) tBBoxOffering = content;
      else if (localTags.equals("<bboxParameter>")) {
      } else if (localTags.equals("</bboxParameter>")) tBBoxParameter = content;
      else if (localTags.equals("<sourceNeedsExpandedFP_EQ>")) {
      } else if (localTags.equals("</sourceNeedsExpandedFP_EQ>"))
        tSourceNeedsExpandedFP_EQ = String2.parseBoolean(content);
      else if (localTags.equals("<onChange>")) {
      } else if (localTags.equals("</onChange>")) tOnChange.add(content);
      else if (localTags.equals("<fgdcFile>")) {
      } else if (localTags.equals("</fgdcFile>")) tFgdcFile = content;
      else if (localTags.equals("<iso19115File>")) {
      } else if (localTags.equals("</iso19115File>")) tIso19115File = content;
      else if (localTags.equals("<sosOfferingPrefix>")) {
      } else if (localTags.equals("</sosOfferingPrefix>")) tSosOfferingPrefix = content;
      else if (localTags.equals("<defaultDataQuery>")) {
      } else if (localTags.equals("</defaultDataQuery>")) tDefaultDataQuery = content;
      else if (localTags.equals("<defaultGraphQuery>")) {
      } else if (localTags.equals("</defaultGraphQuery>")) tDefaultGraphQuery = content;
      else if (localTags.equals("<addVariablesWhere>")) {
      } else if (localTags.equals("</addVariablesWhere>")) tAddVariablesWhere = content;
      else xmlReader.unexpectedTagException();
    }
    int ndv = tDataVariables.size();
    Object ttDataVariables[][] = new Object[ndv][];
    for (int i = 0; i < tDataVariables.size(); i++)
      ttDataVariables[i] = (Object[]) tDataVariables.get(i);

    return new EDDTableFromSOS(
        tDatasetID,
        tAccessibleTo,
        tGraphsAccessibleTo,
        tOnChange,
        tFgdcFile,
        tIso19115File,
        tSosOfferingPrefix,
        tDefaultDataQuery,
        tDefaultGraphQuery,
        tAddVariablesWhere,
        tGlobalAttributes,
        tSosServerType,
        tStationIdSourceName,
        tLongitudeSourceName,
        tLatitudeSourceName,
        tAltitudeSourceName,
        tAltitudeSourceMinimum,
        tAltitudeSourceMaximum,
        tAltitudeMetersPerSourceUnit,
        tTimeSourceName,
        tTimeSourceFormat,
        ttDataVariables,
        tReloadEveryNMinutes,
        tLocalSourceUrl,
        tSosVersion,
        tObservationOfferingIdRegex,
        tRequestObservedPropertiesSeparately,
        tResponseFormat,
        tBBoxOffering,
        tBBoxParameter,
        tSourceNeedsExpandedFP_EQ);
  }

  /**
   * The constructor.
   *
   * @param tDatasetID is a very short string identifier (recommended: [A-Za-z][A-Za-z0-9_]* ) for
   *     this dataset. See EDD.datasetID().
   * @param tAccessibleTo is a comma separated list of 0 or more roles which will have access to
   *     this dataset. <br>
   *     If null, everyone will have access to this dataset (even if not logged in). <br>
   *     If "", no one will have access to this dataset.
   * @param tOnChange 0 or more actions (starting with http://, https://, or mailto: ) to be done
   *     whenever the dataset changes significantly
   * @param tFgdcFile This should be the fullname of a file with the FGDC that should be used for
   *     this dataset, or "" (to cause ERDDAP not to try to generate FGDC metadata for this
   *     dataset), or null (to allow ERDDAP to try to generate FGDC metadata for this dataset).
   * @param tIso19115 This is like tFgdcFile, but for the ISO 19119-2/19139 metadata.
   * @param tAddGlobalAttributes are global attributes which will be added to (and take precedence
   *     over) the data source's global attributes. This may be null if you have nothing to add. The
   *     combined global attributes must include:
   *     <ul>
   *       <li>"title" - the short (&lt; 80 characters) description of the dataset
   *       <li>"summary" - the longer description of the dataset. It may have newline characters
   *           (usually at &lt;= 72 chars per line).
   *       <li>"institution" - the source of the data (best if &lt; 50 characters so it fits in a
   *           graph's legend).
   *       <li>"infoUrl" - the url with information about this data set
   *       <li>"cdm_data_type" - one of the EDD.CDM_xxx options
   *     </ul>
   *     Special case: value="null" causes that item to be removed from combinedGlobalAttributes.
   *     Special case: if combinedGlobalAttributes name="license", any instance of "[standard]" will
   *     be converted to the EDStatic.standardLicense. Special case: if addGlobalAttributes
   *     name="summary", then "[standard]" within the value will be replaced by the standardSummary
   *     (from this class).
   * @param tLonSourceName the results field name for the longitude variable (e.g., longitude). The
   *     name can vary, but the basic meaning must be longitude degrees East. If lon isn't in the
   *     results, use "longitude" here as a placeholder.
   * @param tLatSourceName the results field name for the latitude variable (e.g., latitude) The
   *     name can vary, but the basic meaning must be latitude degrees North. If lat isn't in the
   *     results, use "latitude" here as a placeholder.
   * @param tAltSourceName the results field name for the altitude variable (e.g., depth) (use this
   *     even if no alt variable) If alt isn't in the results, use "altitude" here as a placeholder.
   * @param tSourceMinAlt (in source units) or NaN if not known. This info is explicitly supplied
   *     because it isn't in getCapabilities. [I use eddTable.getEmpiricalMinMax(language,
   *     "2007-02-01", "2007-02-01", false, true); below to get it.]
   * @param tSourceMaxAlt (in source units) or NaN if not known
   * @param tAltMetersPerSourceUnit the factor needed to convert the source alt values to/from
   *     meters above sea level.
   * @param tTimeSourceName the results field name for the time variable (e.g., time) If time isn't
   *     in the results, use "time" here as a placeholder.
   * @param tTimeSourceFormat is either
   *     <ul>
   *       <li>a udunits string (containing " since ") describing how to interpret numbers (e.g.,
   *           "seconds since 1970-01-01T00:00:00Z"),
   *       <li>a java.text.SimpleDateFormat string describing how to interpret string times (see
   *           https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/text/SimpleDateFormat.html)).
   *     </ul>
   *
   * @param tDataVariables is an Object[nDataVariables][4]: <br>
   *     [0]=String sourceName (the field name of the data variable in the tabular results, e.g.,
   *     Salinity, not the ObservedProperty name), <br>
   *     [1]=String destinationName (the name to be presented to the ERDDAP user, or null to use the
   *     sourceName), <br>
   *     [2]=Attributes addAttributes (at ERD, this must have "ioos_category" - a category from
   *     EDV.ioosCategories). Special case: value="null" causes that item to be removed from
   *     combinedAttributes. <br>
   *     [3]=String source dataType (e.g., "int", "float", "String"). Some data sources have
   *     ambiguous data types, so it needs to be specified here. <br>
   *     !!!Unique to EDDTableFromSOS: the longitude, latitude, and stationID variables are created
   *     automatically. <br>
   *     Since these datasets can be served via ERDDAP's SOS server, each tDataVariable will have an
   *     "observedProperty" attribute. "observedProperty" defines the observedProperty SOS clients
   *     request in order to get data for that variable. For non-composite observedProperty, this is
   *     the observedProperty's xlink:href attribute value, e.g., (was
   *     "http://marinemetadata.org/cf#sea_water_temperature" - http://marinemetadata.org is GONE!)
   *     http://mmisw.org/ont/cf/parameter/sea_water_temperature For composite observedProperty,
   *     this is the CompositePhenomenon's gml:id attribute value, e.g., "WEATHER_OBSERVABLES". <br>
   *     !!!You can get station information by visiting the tLocalSourceUrl (which by default
   *     returns the sos:Capabilities XML document).
   * @param tReloadEveryNMinutes indicates how often the source should be checked for new data.
   * @param tLocalSourceUrl the url to which queries are sent
   * @param tSosVersion if a specific version must be specified to the server, this is it (e.g.,
   *     1.0.0). May be null.
   * @param tObservationOfferingIdRegex only observationOfferings with IDs (usually the station
   *     names) which match this regular expression are included in the dataset (".+" will catch all
   *     station names)
   * @param tRequestObservedPropertiesSeparately if true, the observedProperties will be requested
   *     separately. If false, they will be requested all at once.
   * @param tResponseFormat Not yet percent-encoded. Use null or "" for the default.
   * @param tBBoxOffering the offering name to use with lon lat BBOX requests (or null or "")
   * @param tBBoxParameter the parameter prefix to use with lon lat BBOX request (or null or "")
   * @param tSourceNeedsExpandedFP_EQ
   * @throws Throwable if trouble
   */
  public EDDTableFromSOS(
      String tDatasetID,
      String tAccessibleTo,
      String tGraphsAccessibleTo,
      StringArray tOnChange,
      String tFgdcFile,
      String tIso19115File,
      String tSosOfferingPrefix,
      String tDefaultDataQuery,
      String tDefaultGraphQuery,
      String tAddVariablesWhere,
      Attributes tAddGlobalAttributes,
      String tSosServerType,
      String tStationIdSourceName,
      String tLonSourceName,
      String tLatSourceName,
      String tAltSourceName,
      double tSourceMinAlt,
      double tSourceMaxAlt,
      double tAltMetersPerSourceUnit,
      String tTimeSourceName,
      String tTimeSourceFormat,
      Object[][] tDataVariables,
      int tReloadEveryNMinutes,
      String tLocalSourceUrl,
      String tSosVersion,
      String tObservationOfferingIdRegex,
      boolean tRequestObservedPropertiesSeparately,
      String tResponseFormat,
      String tBBoxOffering,
      String tBBoxParameter,
      boolean tSourceNeedsExpandedFP_EQ)
      throws Throwable {

    if (verbose) String2.log("\n*** constructing EDDTableFromSOS " + tDatasetID);
    long constructionStartMillis = System.currentTimeMillis();
    String errorInMethod = "Error in EDDTableFromSOS(" + tDatasetID + ") constructor:\n";

    // save some of the parameters
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
    if (tAddGlobalAttributes == null) tAddGlobalAttributes = new Attributes();
    addGlobalAttributes = tAddGlobalAttributes;
    addGlobalAttributes.set("sourceUrl", convertToPublicSourceUrl(tLocalSourceUrl));

    sosServerType = tSosServerType == null ? "" : tSosServerType.trim();
    tSosServerType = sosServerType.toLowerCase();
    ioos52NServer = tSosServerType.equals(SosServerTypeIoos52N.toLowerCase());
    ioosNdbcServer = tSosServerType.equals(SosServerTypeIoosNdbc.toLowerCase());
    ioosNcSOSServer = tSosServerType.equals(SosServerTypeIoosNcSOS.toLowerCase());
    ioosNosServer = tSosServerType.equals(SosServerTypeIoosNos.toLowerCase());
    oostethysServer = tSosServerType.equals(SosServerTypeOostethys.toLowerCase());
    whoiServer = tSosServerType.equals(SosServerTypeWhoi.toLowerCase()); // required
    if (String2.caseInsensitiveIndexOf(SosServerTypes, sosServerType) < 0)
      throw new RuntimeException(
          "<sosServerType>="
              + sosServerType
              + " must be one of "
              + String2.toCSSVString(SosServerTypes));
    if (ioos52NServer) tSosVersion = "1.0.0"; // server supports 2.0.0, but this class doesn't yet.

    localSourceUrl = tLocalSourceUrl;
    String tSummary = addGlobalAttributes.getString("summary");
    if (tSummary != null)
      addGlobalAttributes.set(
          "summary", String2.replaceAll(tSummary, "[standard]", standardSummary));
    stationIdSourceName =
        tStationIdSourceName == null ? defaultStationIdSourceName : tStationIdSourceName;
    lonSourceName = tLonSourceName;
    latSourceName = tLatSourceName;
    altSourceName = tAltSourceName;
    timeSourceName = tTimeSourceName;
    setReloadEveryNMinutes(tReloadEveryNMinutes);
    Test.ensureNotNothing(localSourceUrl, "sourceUrl wasn't specified.");
    requestObservedPropertiesSeparately = tRequestObservedPropertiesSeparately;
    responseFormat = tResponseFormat == null ? "" : tResponseFormat.trim();
    if (responseFormat.length() == 0) responseFormat = defaultResponseFormat(tSosServerType);
    sosVersion = tSosVersion;

    // bbox - set both or neither
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

    // CONSTRAIN_PARTIAL is great because EDDTable will always check all constraints.
    // justStationTableInfo below relies on both being CONSTRAIN_PARTIAL.
    sourceNeedsExpandedFP_EQ = tSourceNeedsExpandedFP_EQ;
    sourceCanConstrainNumericData = CONSTRAIN_PARTIAL; // just lat and lon
    sourceCanConstrainStringData =
        CONSTRAIN_PARTIAL; // time (but not != or regex), station_id (even REGEX_OP), but nothing
    // else
    sourceCanConstrainStringRegex = PrimitiveArray.REGEX_OP; // just for station_id

    // set source attributes (none available from source)
    sourceGlobalAttributes = new Attributes();
    if (addGlobalAttributes.getString("subsetVariables") == null)
      addGlobalAttributes.add("subsetVariables", "station_id, longitude, latitude");
    combinedGlobalAttributes =
        new Attributes(addGlobalAttributes, sourceGlobalAttributes); // order is important
    String tLicense = combinedGlobalAttributes.getString("license");
    if (tLicense != null)
      combinedGlobalAttributes.set(
          "license", String2.replaceAll(tLicense, "[standard]", EDStatic.standardLicense));
    combinedGlobalAttributes.removeValue("\"null\"");

    // get all dv sourceObservedProperties
    uniqueSourceObservedProperties = new StringArray();
    for (int dv = 0; dv < tDataVariables.length; dv++) {
      // no sourceAtt
      String tSourceName = (String) tDataVariables[dv][0];
      Attributes tAddAtt = (Attributes) tDataVariables[dv][2];
      String
          //    op = tAddAtt.getString("sourceObservedProperty"); //preference for
          // sourceObservedProperty
          // if (op == null || op.length() == 0)
          op =
          tAddAtt.getString(EDV.observedProperty); // otherwise, source = regular observedProperty
      if (op == null || op.length() == 0)
        throw new IllegalArgumentException(
            // "Neither 'sourceObservedProperty' nor 'observervedProperty' attributes were " +
            "The 'observervedProperty' attribute wasn't "
                + "set for dataVariable sourceName="
                + tSourceName
                + ".");
      if (uniqueSourceObservedProperties.indexOf(op) < 0) uniqueSourceObservedProperties.add(op);
    }

    // *** read the getCapabilities xml and set up stationTable
    stationTable = new Table();
    stationTable.addColumn("Lon", new DoubleArray());
    stationTable.addColumn("Lat", new DoubleArray());
    stationTable.addColumn("BeginTime", new DoubleArray());
    stationTable.addColumn("EndTime", new DoubleArray());
    stationTable.addColumn("ID", new StringArray());
    stationTable.addColumn("Procedure", new StringArray());
    ArrayList tStationHasObsPropAL = new ArrayList(); // for each valid station, the boolean[]
    // values that persist for a while
    double tLon = Double.NaN, tLat = Double.NaN, tBeginTime = Double.NaN, tEndTime = Double.NaN;
    String tIndeterminateEnd = null, tStationID = "", tStationProcedure = "";
    double currentEpochSeconds = Calendar2.gcToEpochSeconds(Calendar2.newGCalendarZulu());
    boolean tStationHasObsProp[] = new boolean[uniqueSourceObservedProperties.size()];
    String tDVNames[] = new String[tDataVariables.length];
    for (int dv = 0; dv < tDataVariables.length; dv++)
      tDVNames[dv] = (String) tDataVariables[dv][0];
    // use KVP (KeyValuePair) HTTP GET request to getCapabilities
    // see section 7.2.3 of OGC 06-121r3 (OGC Web Services Common Specification) ver 1.1.0
    String tUrl = localSourceUrl + "?service=SOS&request=GetCapabilities";
    if (sosVersion != null && sosVersion.length() > 0) {
      if (whoiServer) // ioosNcbcServer || ioosNosServer) //not needed. 1.0.0 is default and only.
      tUrl += "&version=" + sosVersion;
      if (ioos52NServer) // AcceptVersions needed. This class only supports 1.0.0 currently.
      tUrl += "&AcceptVersions=" + sosVersion;
    }
    if (reallyVerbose) String2.log("  GetCapUrl=" + tUrl);

    // if (debugMode)
    //    String2.log(SSR.getUrlResponseStringUnchanged(tUrl));
    // File2.writeToFileUtf8("f:/programs/sos/ndbcSosWind_capabilities_90721.xml",
    // SSR.getUrlResponseStringUnchanged(tUrl));

    // get getCapabilities   (perhaps reuse quickRestartFile)
    String quickRestartFileName = File2.forceExtension(quickRestartFullFileName(), ".xml");
    boolean quickRestartFileExists = File2.isFile(quickRestartFileName);
    if (verbose) String2.log("  quickRestartFile exists=" + quickRestartFileExists);
    if (quickRestartFileExists
        && (testQuickRestart || (EDStatic.quickRestart && EDStatic.initialLoadDatasets()))) {
      // use the quickRestartFile
      // Note that if this fails (any reason, e.g., damaged quickRestartFile)
      //  the dataset will reload at next majorLoadDatasets,
      //  since EDStatic.initialLoadDatasets() will be false.

      // set creationTimeMillis as if dataset was created when getCapabilities xml was downloaded
      creationTimeMillis = File2.getLastModified(quickRestartFileName); // 0 if trouble
      if (verbose)
        String2.log(
            "  using getCapabilities from quickRestartFile "
                + Calendar2.millisToIsoStringTZ(creationTimeMillis));
      if (reallyVerbose) String2.log("    " + quickRestartFileName);

    } else {
      // re-download the file
      if (quickRestartFileExists) File2.delete(quickRestartFileName);
      if (verbose) String2.log("  downloading getCapabilities from " + tUrl);
      long downloadTime = System.currentTimeMillis();
      SSR.downloadFile(tUrl, quickRestartFileName, true); // use compression
      if (verbose)
        String2.log("  download time=" + (System.currentTimeMillis() - downloadTime) + "ms");
    }

    SimpleXMLReader xmlReader =
        new SimpleXMLReader(File2.getDecompressedBufferedInputStream(quickRestartFileName));
    try {
      xmlReader.nextTag();
      String tags = xmlReader.allTags();
      String sosPrefix = "";
      if (xmlReader.tag(0).equals("Capabilities")) {
        sosPrefix = "";
      } else if (xmlReader.tag(0).equals("sos:Capabilities")) {
        sosPrefix = "sos:";
        // oostethys or ioosNdbcServer?
      } else {
        xmlReader.close();
        if (debugMode) {
          try {
            String qrFile[] = File2.readFromFileUtf8(quickRestartFileName);
            String2.log(qrFile[1]);
          } catch (Throwable t) {
          }
        }
        throw new IllegalArgumentException(
            "The first SOS capabilities tag="
                + tags
                + " should have been <Capabilities> or <sos:Capabilities>.");
      }
      // String2.log("attributesCSV=" + xmlReader.attributesCSV());
      if (verbose) String2.log("  sosPrefix=" + sosPrefix + " sosVersion=" + sosVersion);
      String offeringTag =
          "<"
              + sosPrefix
              + "Capabilities><"
              + sosPrefix
              + "Contents><"
              + sosPrefix
              + "ObservationOfferingList><"
              + sosPrefix
              + "ObservationOffering>";
      String offeringEndTag =
          "<"
              + sosPrefix
              + "Capabilities><"
              + sosPrefix
              + "Contents><"
              + sosPrefix
              + "ObservationOfferingList></"
              + sosPrefix
              + "ObservationOffering>";

      // default beginTime: a year ago      tamuSos needs this
      GregorianCalendar dbt = Calendar2.newGCalendarZulu();
      dbt.add(Calendar2.YEAR, -1);
      double defaultBeginTime =
          Calendar2.gcToEpochSeconds(Calendar2.clearSmallerFields(dbt, Calendar2.MONTH));
      do {
        // process the tags
        // String2.log("tags=" + tags + xmlReader.content());

        if (tags.endsWith("<ows:ServiceIdentification></ows:ServiceTypeVersion>")) {
          if (sosVersion == null || sosVersion.length() == 0) {
            sosVersion = xmlReader.content(); // e.g., "0.0.31" or "1.0.0"
            if (reallyVerbose)
              String2.log("  sosVersion(from ows:ServiceTypeVersion)=" + sosVersion);
          }

        } else if (tags.startsWith(offeringTag)) {
          String endOfTag = tags.substring(offeringTag.length());
          String content = xmlReader.content();
          String fatalError = null;

          // String2.log("endOfTag=" + endOfTag + xmlReader.content());

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

          // if (endOfTag.equals("")) {  //ioosServer has different gml:id than gml:name; requests
          // need gml:name.
          //    //e.g., 44004
          //    if (ioosServer) tStationID = xmlReader.attributeValue("gml:id");

          if (endOfTag.length() == 0) {
            if (whoiServer) {
              tStationID = xmlReader.attributeValue("gml:id");
              if (reallyVerbose) String2.log("  stationID(from gml:id)=" + tStationID);
            } else if (reallyVerbose) {
              String2.log("  start of offeringTag");
            }

          } else if (endOfTag.equals("</gml:name>")) { // ioos_53N, OOSTethys have this
            // e.g., 44004
            if (!whoiServer) {
              tStationID = content;
              if (reallyVerbose) String2.log("  stationID(from </gml:name>)=" + tStationID);
            }

            // if (ioosServer) {
            //    //remove
            //    String pre = ":station:";
            //    int prePo = tStationID.indexOf(pre);
            //    if (prePo >= 0)
            //        tStationID = tStationID.substring(prePo + pre.length());
            // }

          } else if (endOfTag.equals("<gml:boundedBy><gml:Envelope></gml:lowerCorner>")
              || // IOOS_52N, OOSTethys
              endOfTag.equals(
                  "<gml:boundedBy><gml:boundedBy><gml:Envelope></gml:lowerCorner>")) { // VAST
            // e.g., 38.48 -70.43    order is lat lon [alt/depth]
            StringArray lla = StringArray.wordsAndQuotedPhrases(content);
            if (lla.size() >= 2) {
              tLat = String2.parseDouble(lla.get(0));
              tLon = String2.parseDouble(lla.get(1));
              if (reallyVerbose)
                String2.log("    lat lon(from </gml:lowerCorner>)=" + tLat + " " + tLon);
              // for now, ignore alt/depth
              if (Double.isNaN(tLon) || Double.isNaN(tLat))
                String2.log("Warning while parsing gml: Invalid <gml:lowerCorner>=" + content);
            }

          } else if (endOfTag.equals("<gml:boundedBy><gml:Envelope></gml:upperCorner>")
              || // IOOS_52N, OOSTethys
              endOfTag.equals(
                  "<gml:boundedBy><gml:boundedBy><gml:Envelope></gml:upperCorner>")) { // VAST
            // ensure upperCorner = lowerCorner
            // e.g., 38.48 -70.43    order is lat lon [alt/depth]
            StringArray lla = StringArray.wordsAndQuotedPhrases(content);
            double ttLat = Double.NaN, ttLon = Double.NaN;
            if (lla.size() >= 2) {
              ttLat = String2.parseDouble(lla.get(0));
              ttLon = String2.parseDouble(lla.get(1));
            }
            // for now, ignore alt/depth
            // disable test for VAST development?!
            // VAST has tLat=34.723 ttLat=34.725 tLon=-86.646 ttLon=-86.644
            if (Double.isNaN(ttLon) || Double.isNaN(ttLat))
              String2.log("Warning while parsing gml: Invalid <gml:upperCorner>=" + content);
            if (tLat != ttLat || tLon != ttLon) {
              String2.log(
                  "Warning while parsing gml: lowerCorner!=upperCorner"
                      + " tLat="
                      + tLat
                      + " ttLat="
                      + ttLat
                      + " tLon="
                      + tLon
                      + " ttLon="
                      + ttLon);
              tLon = Double.NaN;
              tLat = Double.NaN; // invalidate this station
            }

          } else if (endOfTag.equals("<" + sosPrefix + "time><gml:TimePeriod></gml:beginPosition>")
              || // ioosServer and OOSTethys
              endOfTag.equals("<sos:eventTime><gml:TimePeriod></gml:beginPosition>")) { // VAST
            // e.g., 2001-07-10T06:30:00Z
            // This is workaround for invalid value "TZ" in NOS SOS GetCapabilities.
            try {
              tBeginTime = Calendar2.isoStringToEpochSeconds(content);
              if (reallyVerbose)
                String2.log(
                    "    beginTime(from <gml:TimePeriod></gml:beginPosition>)=" + tBeginTime);
            } catch (Throwable t) {
              String2.log("Warning while parsing gml:beginPosition ISO time: " + t.toString());
              tBeginTime = defaultBeginTime;
            }

          } else if (endOfTag.equals("<" + sosPrefix + "time><gml:TimePeriod><gml:endPosition>")
              || // ioosServer and OOSTethys
              endOfTag.equals(
                  "<sos:eventTime><gml:TimePeriod><gml:endPosition>")) { // VAST has this
            tIndeterminateEnd = xmlReader.attributeValue("indeterminatePosition");
            if (reallyVerbose) String2.log("    indeterminateEnd time=" + tIndeterminateEnd);

          } else if (endOfTag.equals("<" + sosPrefix + "time><gml:TimePeriod></gml:endPosition>")
              || // ioosServer and OOSTethys
              endOfTag.equals(
                  "<sos:eventTime><gml:TimePeriod></gml:endPosition>")) { // VAST has this) {
            // usually:  <gml:endPosition indeterminatePosition="now"/>
            // could be: e.g., 2001-07-10T06:30:00Z
            if (content.length() > 0) {
              try {
                tEndTime = Calendar2.isoStringToEpochSeconds(content);
              } catch (Throwable t) {
                String2.log("Warning while parsing gml:endPosition ISO time: " + t.toString());
                tEndTime = Double.NaN;
              }
              // are they being precise about recent end time?  change to NaN
              if (!Double.isNaN(tEndTime)
                  && currentEpochSeconds - tEndTime < 2 * Calendar2.SECONDS_PER_DAY)
                tEndTime = Double.NaN;
            } else if (tIndeterminateEnd != null
                && (tIndeterminateEnd.equals("now")
                    || // ioosServer has this
                    tIndeterminateEnd.equals("unknown"))) { // OOSTethys has this
              // leave as NaN...
              // tEndTime = Calendar2.gcToEpochSeconds(Calendar2.newGCalendarZulu() +
              //    Calendar2.SECONDS_PER_HOUR); //buffer
            } else {
              String2.log(
                  "Warning while parsing TimePeriod /gml:endPosition; content=\"\" tIndeterminateEnd="
                      + tIndeterminateEnd);
              tBeginTime =
                  Double.NaN; // mark as invalid; use tBeginTime since tIndeterminateEnd isn't
              // required.
            }
            if (reallyVerbose)
              String2.log("    endTime(from <gml:TimePeriod></gml:endPosition>)=" + tEndTime);

          } else if (endOfTag.equals("<" + sosPrefix + "procedure>")) {
            // <sos:procedure xlink:href="urn:gomoos.org:source.mooring#A01"/>
            // attribute is without quotes
            String tXlink = xmlReader.attributeValue("xlink:href");
            if (tXlink == null)
              String2.log("Warning while parsing 'procedure': no xlink:href attribute!");
            else tStationProcedure = tXlink;
            if (reallyVerbose) String2.log("    procedure>=" + tStationProcedure);

          } else if (endOfTag.equals("<" + sosPrefix + "observedProperty>")) {
            // handle non-composite observedProperty
            // <sos:observedProperty
            // xlink:href="http://marinemetadata.org/cf#sea_water_temperature"/>
            //                      http://marinemetadata.org is GONE!
            // now
            // xlink:href="http://mmisw.org/ont/cf/parameter/sea_water_temperature"
            String tXlink = xmlReader.attributeValue("xlink:href"); // without quotes
            if (tXlink != null) {
              int opPo = uniqueSourceObservedProperties.indexOf(tXlink);
              if (opPo >= 0) tStationHasObsProp[opPo] = true;
              if (reallyVerbose) String2.log("    has observedProperty> #" + opPo + " " + tXlink);
            }
          } else if (endOfTag.equals(
              "<" + sosPrefix + "observedProperty><swe:CompositePhenomenon>")) {
            // handle composite observedProperty
            // <swe:CompositePhenomenon gml:id="WEATHER_OBSERVABLES">
            String tid = xmlReader.attributeValue("gml:id"); // without quotes
            if (tid != null) {
              int opPo = uniqueSourceObservedProperties.indexOf(tid);
              if (opPo >= 0) tStationHasObsProp[opPo] = true;
              if (reallyVerbose)
                String2.log("    has composite observedProperty> #" + opPo + " " + tid);
            }
          }

          // handle the error
          // but this isn't used; problems above are logged, but only cause this station not to be
          // used (see 'invalid' below)
          if (fatalError != null)
            throw new IllegalArgumentException(
                "Error on xml line #"
                    + xmlReader.lineNumber()
                    + " stationID="
                    + tStationID
                    + ": "
                    + fatalError);

          // all data gathered; create the station
        } else if (tags.equals(offeringEndTag)) {

          // look for invalid values
          boolean hasObservedProperties = false;
          for (int i = 0; i < uniqueSourceObservedProperties.size(); i++) {
            if (tStationHasObsProp[i]) {
              hasObservedProperties = true;
              break;
            }
          }
          String invalid = null;
          if (Double.isNaN(tLon)) invalid = "longitude";
          else if (Double.isNaN(tLat)) invalid = "latitude";
          else if (Double.isNaN(tBeginTime)) invalid = "beginTime";
          // endTime may be NaN
          else if (tStationProcedure.length() == 0) invalid = "stationProcedure";
          else if (!hasObservedProperties) invalid = "stationHasRelevantObservedProperties";

          if (tStationID == null || tStationID.length() == 0) {
            if (reallyVerbose) String2.log("  station_id=\"" + tStationID + "\" rejected.");

          } else if (!tStationID.matches(tObservationOfferingIdRegex)) {
            if (reallyVerbose)
              String2.log(
                  "  station_id=\""
                      + tStationID
                      + "\" rejected: didn't match the observationOfferingIdRegex="
                      + tObservationOfferingIdRegex);

          } else if (invalid != null) {
            if (reallyVerbose)
              String2.log(
                  "  station_id=\""
                      + tStationID
                      + "\" rejected: The "
                      + invalid
                      + " value wasn't set.");

          } else {
            // add the station;  add a row of data to stationTable
            stationTable.getColumn(stationLonCol).addDouble(tLon);
            stationTable.getColumn(stationLatCol).addDouble(tLat);
            stationTable.getColumn(stationBeginTimeCol).addDouble(tBeginTime); // epochSeconds
            stationTable.getColumn(stationEndTimeCol).addDouble(tEndTime); // epochSeconds
            stationTable.getColumn(stationIDCol).addString(tStationID);
            stationTable.getColumn(stationProcedureCol).addString(tStationProcedure);
            tStationHasObsPropAL.add(tStationHasObsProp);
          }

          // reset values for next station
          tLon = Double.NaN;
          tLat = Double.NaN;
          tBeginTime = Double.NaN;
          tEndTime = Double.NaN;
          tIndeterminateEnd = null;
          tStationID = "";
          tStationProcedure = "";
          tStationHasObsProp = new boolean[uniqueSourceObservedProperties.size()];
        }

        // get the next tag
        xmlReader.nextTag();
        tags = xmlReader.allTags();
      } while (!tags.startsWith("</"));

    } finally {
      xmlReader.close();
    }
    if (sosVersion == null || sosVersion.length() == 0) sosVersion = "1.0.0"; // default

    if (stationTable.nRows() == 0)
      throw new RuntimeException(datasetID + " has no valid stations.");
    stationHasObsProp = new boolean[tStationHasObsPropAL.size()][];
    for (int i = 0; i < tStationHasObsPropAL.size(); i++)
      stationHasObsProp[i] = (boolean[]) tStationHasObsPropAL.get(i);
    if (reallyVerbose)
      String2.log("Station Table=\n" + stationTable.saveAsJsonString(stationBeginTimeCol, true));

    // cdm_data_type
    String cdmType = combinedGlobalAttributes.getString("cdm_data_type");
    String allowedCdmTypes[] =
        new String[] {
          CDM_OTHER,
          CDM_POINT,
          CDM_TIMESERIES,
          CDM_TIMESERIESPROFILE,
          CDM_TRAJECTORY,
          CDM_TRAJECTORYPROFILE
        };
    if (String2.indexOf(allowedCdmTypes, cdmType) < 0)
      throw new RuntimeException(
          "Currently, EDDTableFromSOS only supports cdm_data_type="
              + String2.toCSSVString(allowedCdmTypes)
              + ", not \""
              + cdmType
              + "\".");

    // make the fixedVariables
    dataVariables = new EDV[nFixedVariables + tDataVariables.length];

    lonIndex = 0;
    PAOne stats[] = stationTable.getColumn(stationLonCol).calculatePAOneStats();
    dataVariables[lonIndex] =
        new EDVLon(
            datasetID,
            tLonSourceName,
            null,
            null,
            "double",
            stats[PrimitiveArray.STATS_MIN],
            stats[PrimitiveArray.STATS_MAX]);

    latIndex = 1;
    stats = stationTable.getColumn(stationLatCol).calculatePAOneStats();
    dataVariables[latIndex] =
        new EDVLat(
            datasetID,
            tLatSourceName,
            null,
            null,
            "double",
            stats[PrimitiveArray.STATS_MIN],
            stats[PrimitiveArray.STATS_MAX]);

    sosOfferingIndex = 2; // aka stationID
    Attributes tAtts =
        new Attributes().add("long_name", "Station ID").add("ioos_category", "Identifier");
    if (CDM_TIMESERIES.equals(cdmType) || CDM_TIMESERIESPROFILE.equals(cdmType))
      tAtts.add("cf_role", "timeseries_id");
    else if (CDM_TRAJECTORY.equals(cdmType) || CDM_TRAJECTORYPROFILE.equals(cdmType))
      tAtts.add("cf_role", "trajectory_id");
    dataVariables[sosOfferingIndex] =
        new EDV(
            datasetID,
            stationIdSourceName,
            stationIdDestinationName,
            null,
            tAtts,
            "String"); // the constructor that reads actual_range
    // no need to call setActualRangeFromDestinationMinMax() since they are NaNs

    // alt axis isn't set up in datasets.xml.
    altIndex = 3;
    depthIndex = -1; // 2012-12-20 consider using depthIndex???
    Attributes altAddAtts = new Attributes();
    altAddAtts.set("units", "m");
    if (tAltMetersPerSourceUnit != 1) altAddAtts.set("scale_factor", tAltMetersPerSourceUnit);
    dataVariables[altIndex] =
        new EDVAlt(
            datasetID,
            tAltSourceName,
            null,
            altAddAtts,
            "double",
            PAOne.fromDouble(tSourceMinAlt),
            PAOne.fromDouble(tSourceMaxAlt));

    timeIndex = 4; // times in epochSeconds
    stats =
        stationTable
            .getColumn(stationBeginTimeCol)
            .calculatePAOneStats(); // to get the minimum begin value
    PAOne stats2[] =
        stationTable
            .getColumn(stationEndTimeCol)
            .calculatePAOneStats(); // to get the maximum end value
    PAOne tTimeMin = stats[PrimitiveArray.STATS_MIN];
    PAOne tTimeMax =
        stats2[PrimitiveArray.STATS_N].getInt() < stationTable.getColumn(stationEndTimeCol).size()
            ? // some indeterminant
            PAOne.fromDouble(Double.NaN)
            : stats2[PrimitiveArray.STATS_MAX];
    tAtts = new Attributes().add("units", tTimeSourceFormat);
    if (CDM_TIMESERIESPROFILE.equals(cdmType) || CDM_TRAJECTORYPROFILE.equals(cdmType))
      tAtts.add("cf_role", "profile_id");
    EDVTime edvTime =
        new EDVTime(
            datasetID,
            tTimeSourceName,
            null,
            tAtts,
            "String"); // this constructor gets source / sets destination actual_range
    edvTime.setDestinationMinMax(tTimeMin, tTimeMax);
    edvTime.setActualRangeFromDestinationMinMax();
    dataVariables[timeIndex] = edvTime;

    // create non-fixed dataVariables[]
    for (int dv = 0; dv < tDataVariables.length; dv++) {
      String tSourceName = (String) tDataVariables[dv][0];
      String tDestName = (String) tDataVariables[dv][1];
      if (tDestName == null || tDestName.trim().length() == 0) tDestName = tSourceName;
      Attributes tSourceAtt = null; // (none available from source)
      Attributes tAddAtt = (Attributes) tDataVariables[dv][2];
      String tSourceType = (String) tDataVariables[dv][3];

      if (EDVTimeStamp.hasTimeUnits(tSourceAtt, tAddAtt)) {
        dataVariables[nFixedVariables + dv] =
            new EDVTimeStamp(
                datasetID,
                tSourceName,
                tDestName,
                tSourceAtt,
                tAddAtt,
                tSourceType); // this constructor gets source / sets destination actual_range
      } else {
        dataVariables[nFixedVariables + dv] =
            new EDV(
                datasetID,
                tSourceName,
                tDestName,
                tSourceAtt,
                tAddAtt,
                tSourceType); // the constructor that reads actual_range
        dataVariables[nFixedVariables + dv].setActualRangeFromDestinationMinMax();
      }
      Test.ensureNotNothing(
          dataVariables[nFixedVariables + dv].combinedAttributes().getString(EDV.observedProperty),
          "\""
              + EDV.observedProperty
              + "\" attribute not assigned for variable sourceName="
              + tSourceName);
    }

    // Gather information to serve this dataset via ERDDAP's SOS server.
    // This has an advantage over the generic gathering of SOS data:
    //  it can determine the min/max lon/lat/time of each station.
    // SOS datasets always have actual lon,lat values and stationTable time is always epochSeconds,
    //  so I can just use source station info directly (without conversion).
    // Note that times are often too wide a range because they are for all observedProperties,
    //  not just the one used by this dataset.
    if (preliminaryAccessibleViaSOS().length() == 0) { // it should succeed
      sosMinLon = stationTable.getColumn(stationLonCol);
      sosMaxLon = sosMinLon;
      sosMinLat = stationTable.getColumn(stationLatCol);
      sosMaxLat = sosMinLat;
      sosMinTime = stationTable.getColumn(stationBeginTimeCol);
      sosMaxTime = stationTable.getColumn(stationEndTimeCol);
      sosOfferings =
          (StringArray)
              stationTable.getColumn(stationIDCol).clone(); // clone since it changes below
      int nSosOfferings = sosOfferings.size();
      // convert sosOfferings to short name
      // !!! This should determine sosOfferingPrefix, then for each offering: ensure it exists and
      // remove it
      for (int offering = 0; offering < nSosOfferings; offering++) {
        String so = sosOfferings.getString(offering);
        int cpo = so.lastIndexOf(":");
        if (cpo >= 0) sosOfferings.setString(offering, so.substring(cpo + 1));
      }
    }

    // make addVariablesWhereAttNames and addVariablesWhereAttValues
    makeAddVariablesWhereAttNamesAndValues(tAddVariablesWhere);

    // ensure the setup is valid
    ensureValid();

    // done
    long cTime = System.currentTimeMillis() - constructionStartMillis;
    if (verbose)
      String2.log(
          (debugMode ? "\n" + toString() : "")
              + "\n*** EDDTableFromSOS "
              + datasetID
              + " constructor finished. TIME="
              + cTime
              + "ms"
              + (cTime >= 600000 ? "  (>10m!)" : cTime >= 10000 ? "  (>10s!)" : "")
              + "\n");
  }

  /**
   * This returns true if this EDDTable knows each variable's actual_range (e.g., EDDTableFromFiles)
   * or false if it doesn't (e.g., EDDTableFromDatabase).
   *
   * @returns true if this EDDTable knows each variable's actual_range (e.g., EDDTableFromFiles) or
   *     false if it doesn't (e.g., EDDTableFromDatabase).
   */
  @Override
  public boolean knowsActualRange() {
    return false;
  } // because data is from a remote service

  /**
   * This gets the data (chunk by chunk) from this EDDTable for the OPeNDAP DAP-style query and
   * writes it to the TableWriter. See the EDDTable method documentation.
   *
   * @param language the index of the selected language
   * @param loggedInAs the user's login name if logged in (or null if not logged in).
   * @param userDapQuery the part after the '?', still percentEncoded, may be null.
   * @throws Throwable if trouble (notably, WaitThenTryAgainException)
   */
  @Override
  public void getDataForDapQuery(
      int language,
      String loggedInAs,
      String requestUrl,
      String userDapQuery,
      TableWriter tableWriter)
      throws Throwable {
    long getTime = System.currentTimeMillis();

    // get the sourceDapQuery (a query that the source can handle)
    StringArray resultsVariables = new StringArray();
    StringArray constraintVariables = new StringArray();
    StringArray constraintOps = new StringArray();
    StringArray constraintValues = new StringArray();
    getSourceQueryFromDapQuery(
        language,
        userDapQuery,
        resultsVariables,
        constraintVariables,
        constraintOps,
        constraintValues); // timeStamp constraints other than regex are epochSeconds
    int nConstraints = constraintVariables.size();

    // further prune constraints
    // sourceCanConstrainNumericData = CONSTRAIN_PARTIAL; //just lat and lon
    // sourceCanConstrainStringData  = CONSTRAIN_PARTIAL; //time (but not != or regex), station_id
    // (even PrimitiveArray.REGEX_OP), but nothing else
    // sourceCanConstrainStringRegex = PrimitiveArray.REGEX_OP; //just for station_id
    // work backwards since deleting some
    int conDVI[] = new int[nConstraints];
    boolean justStationTableInfo = true;
    boolean hasStationConstraint = false;
    for (int c = constraintVariables.size() - 1; c >= 0; c--) {
      String constraintVariable = constraintVariables.get(c);
      String constraintOp = constraintOps.get(c);
      double dConstraintValue = String2.parseDouble(constraintValues.get(c));
      int dv = String2.indexOf(dataVariableSourceNames(), constraintVariable);
      conDVI[c] = dv;
      EDV edv = dataVariables[dv];
      if (dv != lonIndex && dv != latIndex && dv != sosOfferingIndex) // aka stationID
      justStationTableInfo = false;

      if (dv == latIndex || dv == lonIndex) {
        // ok

      } else if (dv == sosOfferingIndex) { // aka stationID
        hasStationConstraint = true; // any kind: =, <, >, regex, ...

      } else if (edv instanceof EDVTimeStamp) {
        // remove if != or regex
        if (constraintOp.equals("!=") || constraintOp.equals(PrimitiveArray.REGEX_OP)) {
          // remove constraint
          constraintVariables.remove(c);
          constraintOps.remove(c);
          constraintValues.remove(c);
        }

      } else {
        // remove all other constraints
        constraintVariables.remove(c);
        constraintOps.remove(c);
        constraintValues.remove(c);
      }
    }

    // make tableDVI -- the dataVariable indices corresponding to the columns needed for the results
    // table.
    // Always include lon,lat,alt,time,id to facilitate merging observedProperties correctly.
    IntArray tableDVI = new IntArray(); // .get(tableColumnIndex) -> dataVariableIndex
    // add the nFixedVariables
    int tableLonCol = 0, tableLatCol = 1, tableAltCol = 2, tableTimeCol = 3, tableStationIdCol = 4;
    tableDVI.add(lonIndex);
    tableDVI.add(latIndex);
    tableDVI.add(altIndex);
    tableDVI.add(timeIndex);
    tableDVI.add(sosOfferingIndex); // ada stationID
    StringArray requestObservedProperties = new StringArray();
    for (int rv = 0; rv < resultsVariables.size(); rv++) {
      int dvi = String2.indexOf(dataVariableSourceNames(), resultsVariables.get(rv));
      // only add if not already included
      if (tableDVI.indexOf(dvi, 0) < 0) tableDVI.add(dvi);
      if (dvi != lonIndex && dvi != latIndex && dvi != sosOfferingIndex) // aka stationID
      justStationTableInfo = false;
      // make list of desired observedProperties
      // remember that >1 var may refer to same obsProp (eg, insideTemp and outsideTemp refer to
      // ...Temperature)
      if (dvi >= nFixedVariables) {
        String
            //    top = dataVariables[dvi].combinedAttributes().getString("sourceObservedProperty");
            // //preferred
            // if (top == null)
            top = dataVariables[dvi].combinedAttributes().getString(EDV.observedProperty);
        if (requestObservedProperties.indexOf(top) < 0) requestObservedProperties.add(top);
      }
    }

    // handle justStationTableInfo
    // This relies on all CONSTRAIN_PARTIAL above.
    if (reallyVerbose) String2.log("  justStationTableInfo=" + justStationTableInfo);
    if (justStationTableInfo) {
      // make a table of just lon,lat,id
      tableDVI.clear(); // .get(tableColumnIndex) -> dataVariableIndex
      tableDVI.add(lonIndex);
      tableDVI.add(latIndex);
      tableDVI.add(sosOfferingIndex); // stationID
      Table table = makeEmptySourceTable(tableDVI.toArray(), 128);

      // add all of the station info
      table.getColumn(0).append(stationTable.getColumn(stationLonCol));
      table.getColumn(1).append(stationTable.getColumn(stationLatCol));
      table.getColumn(2).append(stationTable.getColumn(stationIDCol));

      // do the final writeToTableWriter (it will
      writeChunkToTableWriter(language, requestUrl, userDapQuery, table, tableWriter, true);
      if (reallyVerbose)
        String2.log(
            "  getDataForDapQuery done. TIME=" + (System.currentTimeMillis() - getTime) + "ms");
      return;
    }

    // always at least one observedProperty
    // If request is for lon,lat,id with time constraints
    //  you get no data if you don't ask for a property.
    // So add the first var after nFixedVariables
    // This is not a perfect solution, because results might be different if you chose a different
    // property.
    if (requestObservedProperties.size() == 0) {
      int dvi = nFixedVariables;
      tableDVI.add(dvi);
      String
          //    top = dataVariables[dvi].combinedAttributes().getString("sourceObservedProperty");
          // //preferred
          // if (top == null)
          top = dataVariables[dvi].combinedAttributes().getString(EDV.observedProperty);
      if (requestObservedProperties.indexOf(top) < 0) requestObservedProperties.add(top);
    }

    // get requestedMin,Max
    double requestedDestinationMin[] = new double[4]; // LLAT   time in userDapQuery is epochSeconds
    double requestedDestinationMax[] = new double[4];
    getRequestedDestinationMinMax(
        language, userDapQuery, true, requestedDestinationMin, requestedDestinationMax);

    // doBBoxQuery?
    boolean doBBoxQuery = !hasStationConstraint && bboxOffering != null && bboxParameter != null;
    String tBBoxParameter = "";
    if (doBBoxQuery) {
      if (reallyVerbose) String2.log("doBBoxQuery=true");
      // fill in missing bbox bounds with known dataset lon,lat bounds
      double fudge = 0.001;
      if (Double.isNaN(requestedDestinationMin[0]))
        requestedDestinationMin[0] = dataVariables[lonIndex].destinationMinDouble() - fudge;
      if (Double.isNaN(requestedDestinationMax[0]))
        requestedDestinationMax[0] = dataVariables[lonIndex].destinationMaxDouble() + fudge;

      if (Double.isNaN(requestedDestinationMin[1]))
        requestedDestinationMin[1] = dataVariables[latIndex].destinationMinDouble() - fudge;
      if (Double.isNaN(requestedDestinationMax[1]))
        requestedDestinationMax[1] = dataVariables[latIndex].destinationMaxDouble() + fudge;

      // ndbc objects if min=max
      if (requestedDestinationMin[0] == requestedDestinationMax[0]) {
        requestedDestinationMin[0] -= fudge;
        requestedDestinationMax[0] += fudge;
      }
      if (requestedDestinationMin[1] == requestedDestinationMax[1]) {
        requestedDestinationMin[1] -= fudge;
        requestedDestinationMax[1] += fudge;
      }

      tBBoxParameter =
          "&"
              + bboxParameter
              + requestedDestinationMin[0]
              + ","
              + requestedDestinationMin[1]
              + ","
              + requestedDestinationMax[0]
              + ","
              + requestedDestinationMax[1];
    }

    // makeTable
    Table table = makeEmptySourceTable(tableDVI.toArray(), 128);
    HashMap llatHash = new HashMap(); // llat info -> table row number (as a String)
    // IntArray fixedColumnsInTable = new IntArray();
    // for (int col = 0; col < tableDVI.size(); col++)
    //    if (tableDVI.get(col) < nFixedVariables)
    //        fixedColumnsInTable.add(col);

    // Go through the stations, request data if station is relevant.
    // SOS queries may be limited to 1 station at a time (based on 'procedure'?)
    //  But that's a good thing because I want to limit response chunk size.
    //  ???Extreme cases: may need to further limit to chunks of time.
    int nStations = stationTable.nRows();
    boolean aConstraintShown = false;
    boolean matchingStation = false;
    STATION_LOOP:
    for (int station = 0; station < nStations; station++) {
      if (Thread.currentThread().isInterrupted())
        throw new SimpleException(
            "EDDTableFromSOS.getDataForDapQuery" + EDStatic.caughtInterruptedAr[0]);

      String tStationLonString = "",
          tStationLatString = "",
          tStationAltString = "",
          tStationID = "";

      if (doBBoxQuery) {
        if (station >= 1) break;

        // when station=0, get data for all stations
        tStationID = bboxOffering;

      } else {
        // make sure this station is relevant
        // test lon,lat,time   (station alt not available)
        boolean stationOK = true;
        for (int i = 0; i < 4; i++) { // 0..3 are LLAT in requestedDestinationMin/Max
          String tName = "???";
          int stationMinCol = -1, stationMaxCol = -1, precision = 4;
          if (i == 0) {
            tName = "longitude";
            stationMinCol = stationLonCol;
            stationMaxCol = stationLonCol;
          } else if (i == 1) {
            tName = "latitude";
            stationMinCol = stationLatCol;
            stationMaxCol = stationLatCol;
          } else if (i == 3) {
            tName = "time";
            stationMinCol = stationBeginTimeCol;
            stationMaxCol = stationEndTimeCol;
            precision = 9;
          }
          if (stationMinCol >= 0) {
            double rMin = requestedDestinationMin[i];
            double rMax = requestedDestinationMax[i];
            double sMin = stationTable.getDoubleData(stationMinCol, station);
            double sMax = stationTable.getDoubleData(stationMaxCol, station);
            if ((Double.isFinite(rMax)
                    && Double.isFinite(sMin)
                    && !Math2.greaterThanAE(precision, rMax, sMin))
                || (Double.isFinite(rMin)
                    && Double.isFinite(sMax)
                    && !Math2.lessThanAE(precision, rMin, sMax))) {
              if (reallyVerbose)
                String2.log(
                    "  reject station="
                        + station
                        + ": no overlap between station "
                        + tName
                        + " (min="
                        + sMin
                        + ", max="
                        + sMax
                        + ") and requested range (min="
                        + rMin
                        + ", max="
                        + rMax
                        + ").");
              stationOK = false;
              break;
            }
          }
        }
        if (!stationOK) continue;

        // test stationID constraint
        tStationID = stationTable.getStringData(stationIDCol, station);
        for (int con = 0; con < nConstraints; con++) {
          if (conDVI[con] == sosOfferingIndex) { // aka stationID
            String op = constraintOps.get(con);
            boolean pass =
                PrimitiveArray.testValueOpValue(tStationID, op, constraintValues.get(con));
            if (!pass) {
              if (reallyVerbose)
                String2.log(
                    "  rejecting station="
                        + station
                        + " because stationID=\""
                        + tStationID
                        + "\" isn't "
                        + op
                        + " \""
                        + constraintValues.get(con)
                        + "\".");
              stationOK = false;
              break; // constraint loop
            }
          }
        }
        if (!stationOK) continue;
        if (reallyVerbose) String2.log("\nquerying stationID=" + tStationID);

        // The station is in the bounding box!
        tStationLonString = "" + stationTable.getDoubleData(stationLonCol, station);
        tStationLatString = "" + stationTable.getDoubleData(stationLatCol, station);
        tStationAltString = "";
      }
      matchingStation = true;

      // request the observedProperties (together or separately)
      // see class javadocs above explaining why this is needed
      for (int obsProp = 0; obsProp < requestObservedProperties.size(); obsProp++) {

        StringArray tRequestObservedProperties = new StringArray();
        if (requestObservedPropertiesSeparately) {
          // separately
          tRequestObservedProperties.add(requestObservedProperties.get(obsProp));
        } else {
          // we're already done
          if (obsProp == 1) break;

          // get all at once
          tRequestObservedProperties = (StringArray) requestObservedProperties.clone();
        }
        if (reallyVerbose) {
          String2.log("\nobsProp=" + obsProp + " ttObsProp=" + tRequestObservedProperties);
          // String2.pressEnterToContinue();
        }

        if (doBBoxQuery) {
          // we're checking all stations, so don't check if this stationHasObsProp
        } else {
          // remove obsProp from tRequestObservedProperties if station doesn't support it
          for (int i = tRequestObservedProperties.size() - 1; i >= 0; i--) {
            int po = uniqueSourceObservedProperties.indexOf(tRequestObservedProperties.get(i));
            if (!stationHasObsProp[station][po]) tRequestObservedProperties.remove(i);
          }
          if (tRequestObservedProperties.size() == 0) {
            if (reallyVerbose)
              String2.log(
                  "  reject station="
                      + station
                      + ": It has none of the requested observedProperties.");
            continue;
          }
        }

        if (ioosNdbcServer || ioosNosServer) {

          // make kvp   -- no examples or info for XML request
          // example from https://sdf.ndbc.noaa.gov/sos/
          // if (reallyVerbose && obsProp == 0) String2.log("\n  sample=" +
          //  "https://sdf.ndbc.noaa.gov/sos/server.php?request=GetObservation" +
          //  "&service=SOS&offering=NDBC:46088&observedproperty=currents" +
          //
          // "&responseformat=text/xml;schema=%22ioos/0.6.1%22&eventtime=2008-06-01T00:00Z/2008-06-02T00:00Z");
          // 2012-11-15 NDBC sample revised to be
          //  https://sdf.ndbc.noaa.gov/sos/server.php?request=GetObservation&service=SOS
          //    &version=1.0.0&offering=urn:ioos:station:wmo:41012
          //    &observedproperty=air_pressure_at_sea_level
          //    &responseformat=text/xml;subtype=%22om/1.0.0%22
          //    &eventtime=2011-03-01T00:00Z/2011-03-02T00:00Z
          // ???How should a client know how to convert GetCapabilities observedProperty (long)
          //  into GetObservation observedProperty (short)?
          // int hashPo = tRequestObservedProperties.get(obsProp).lastIndexOf(':'); //for NOS SOS
          String kvp =
              "?service=SOS"
                  + "&version="
                  + sosVersion
                  + "&request=GetObservation"
                  + "&offering="
                  + SSR.minimalPercentEncode(tStationID)
                  +
                  // "&observedProperty=" + tRequestObservedProperties.get(obsProp).substring(hashPo
                  // + 1) +
                  "&observedProperty="
                  + tRequestObservedProperties.get(obsProp)
                  + "&responseFormat="
                  + SSR.minimalPercentEncode(responseFormat)
                  + "&eventTime="
                  + // requestedDestination times are epochSeconds
                  Calendar2.epochSecondsToIsoStringTZ(requestedDestinationMin[3])
                  + (requestedDestinationMin[3] == requestedDestinationMax[3]
                      ? ""
                      : "/" + Calendar2.epochSecondsToIsoStringTZ(requestedDestinationMax[3]))
                  + tBBoxParameter;
          if (reallyVerbose && !aConstraintShown) {
            String2.log("  requestURL=" + localSourceUrl + kvp);
            // aConstraintShown = true;
          }

          // read IOOS response
          readFromIoosNdbcNos(language, kvp, table, llatHash);

        } else { // non-ioosServer    e.g., Oostethys

          // make the xml constraint: basics and offering
          // important help getting this to work from f:/programs/sos/oie_sos_time_range_obs.cgi
          // which is from
          // http://www.oostethys.org/ogc-oceans-interoperability-experiment/experiment-1/sos-client-testing-source-code [GONE]
          StringBuilder getSB = new StringBuilder();
          getSB.append(
              "?service=SOS"
                  + "&version="
                  + sosVersion
                  + "&responseFormat="
                  + SSR.minimalPercentEncode(responseFormat)
                  + "&request=GetObservation"
                  + "&offering="
                  + SSR.minimalPercentEncode(tStationID)
                  + "&observedProperty="
                  + SSR.minimalPercentEncode(
                      String2.toSVString(tRequestObservedProperties.toArray(), ",", false)));

          // eventTime
          // older SOS spec has <ogc:During>; v1.0 draft has <ogc:T_During>
          // new oostethys server ignores <ogc:During>, wants T_During
          // see http://www.oostethys.org/Members/tcook/sos-xml-time-encodings/ [GONE]
          String tMinTimeS = Calendar2.epochSecondsToIsoStringTZ(requestedDestinationMin[3]);
          String tMaxTimeS = Calendar2.epochSecondsToIsoStringTZ(requestedDestinationMax[3]);
          if (requestedDestinationMin[3] == requestedDestinationMax[3]) {
            getSB.append("&eventTime=" + tMinTimeS);
          } else {
            getSB.append("&eventTime=" + tMinTimeS + "/" + tMaxTimeS);
          }

          // BBOX
          getSB.append(tBBoxParameter); // "" if inactive; BBOX=minlon,minlat,maxlon,maxlat

          // See above.
          // responseFormat=text%2Fxml%3B%20subtype%3D%22om%2F1.0.0%22
          // was resultFormat until 2010-04-20
          // getSB.append("&resultFormat=application/com-xml"); //until 2010-04-20
          // see http://www.oostethys.org/best-practices/best-practices-get [GONE]
          // getSB.append("&text%2Fxml%3B%20subtype%3D%22om%2F1.0.0%22");

          if (reallyVerbose && !aConstraintShown) {
            String2.log("  requestURL=" + localSourceUrl + getSB.toString());
            // aConstraintShown = true;
          }
          if (false) { // debugMode) {
            String2.log("*** Begin response");
            String2.log(SSR.getUrlResponseStringUnchanged(localSourceUrl + getSB.toString()));
            String2.log("*** End response");
          }

          // *** read the data
          if (whoiServer) {
            readFromWhoiServer(
                language,
                getSB.toString(),
                table,
                llatHash,
                tStationLonString,
                tStationLatString,
                tStationAltString,
                tStationID);
          } else {
            readFromOostethys(
                language,
                getSB.toString(),
                table,
                llatHash,
                tStationLonString,
                tStationLatString,
                tStationAltString,
                tStationID);
          }
        } // end of non-ioosServer get chunk of data

        if (reallyVerbose) String2.log("\nstation=" + tStationID + " tableNRows=" + table.nRows());
        // if (debugMode) String2.log("table=\n" + table);
      } // end of obsProp loop

      // writeToTableWriter
      // this can't be in obsProp loop (obsProps are merged)
      if (writeChunkToTableWriter(language, requestUrl, userDapQuery, table, tableWriter, false)) {
        table = makeEmptySourceTable(tableDVI.toArray(), 128);
        llatHash.clear(); // llat info -> table row number (as a String)
        if (tableWriter.noMoreDataPlease) {
          tableWriter.logCaughtNoMoreDataPlease(datasetID);
          break STATION_LOOP;
        }
      }
    } // end station loop
    if (!matchingStation)
      throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (There was no matching station.)");

    // do the final writeToTableWriter
    writeChunkToTableWriter(language, requestUrl, userDapQuery, table, tableWriter, true);
    if (reallyVerbose)
      String2.log(
          "  getDataForDapQuery done. TIME=" + (System.currentTimeMillis() - getTime) + "ms");
  }

  /**
   * This reads the data from an IOOS server.
   *
   * @param kvp the string to be added to the sourceUrl
   * @param table the table to which rows will be added
   * @param llatHash lon+lat+alt+time+stationID goes to row#
   */
  protected void readFromIoosNdbcNos(int language, String kvp, Table table, HashMap llatHash)
      throws Throwable {

    // downloading data may take time
    // so write to file, then quickly read and process
    // also this simplifies catching/processing xml error
    String grabFileName = cacheDirectory() + "grabFile" + String2.md5Hex12(kvp);
    long downloadTime = System.currentTimeMillis();
    try {
      if (EDStatic.developmentMode && File2.isFile(grabFileName)) {
      } else SSR.downloadFile(localSourceUrl + kvp, grabFileName, true);
      downloadTime = System.currentTimeMillis() - downloadTime;
    } catch (Throwable t) {
      // Normal error returns xml ExceptionReport.  So package anything else as
      // WaitThenTryAgainException.
      String2.log(
          "ERROR while trying to download from "
              + localSourceUrl
              + kvp
              + "\n"
              + MustBe.throwableToString(t));
      throw t instanceof WaitThenTryAgainException
          ? t
          : new WaitThenTryAgainException(
              EDStatic.simpleBilingual(language, EDStatic.waitThenTryAgainAr)
                  + "\n("
                  + t.toString()
                  + ")");
    }

    try {

      // read the file
      String sar[] = File2.readFromFile(grabFileName, null, 2);
      // an error message from File2.readFromFile?
      if (sar[0].length() > 0) throw new SimpleException(sar[0]);
      if (reallyVerbose)
        String2.log(
            "ASCII response:\n" + sar[1].substring(0, Math.min(4000, sar[1].length())) + " ...\n");

      // is it an xml file (presumably an error report)?
      if (sar[1].startsWith("<?xml")) {
        sar = null; // let it be garbage collected
        SimpleXMLReader xmlReader =
            new SimpleXMLReader(File2.getDecompressedBufferedInputStream(grabFileName));
        try {
          xmlReader.nextTag();
          String tags = xmlReader.allTags();
          String ofInterest1 = null;
          String ofInterest2 = null;
          String ofInterest3 = null;

          // response is error message
          if (tags.equals("<ServiceExceptionReport>")
              || tags.equals("<ExceptionReport>")
              || // nos coops 2014-12-24
              tags.equals("<ows:ExceptionReport>")) { // ioosService
            // 2014-12-22 ioos:
            // https://sdf.ndbc.noaa.gov/sos/server.php?service=SOS&version=1.0.0&request=GetObservation&offering=urn:ioos:station:wmo:46013&observedProperty=http://mmisw.org/ont/cf/parameter/waves&responseFormat=text/csv&eventTime=2008-08-01T00:00:00Z/2008-09-05T00:00:00Z
            // <ows:ExceptionReport xmlns:ows="http://www.opengis.net/ows/1.1"
            // xmlns:xsi="https://www.w3.org/2001/XMLSchema-instance"
            // xsi:schemaLocation="http://www.opengis.net/ows/1.1 owsExceptionReport.xsd"
            // version="1.0.0" xml:lang="en">
            // <ows:Exception exceptionCode="InvalidParameterValue" locator="eventTime">
            // <ows:ExceptionText>No more than 31 days of data can be requested.</ows:ExceptionText>
            // </ows:Exception>
            // </ows:ExceptionReport>
            // <?xml version="1.0"?>
            // <ExceptionReport xmlns="http://www.opengis.net/ows"
            // xmlns:xsi="https://www.w3.org/2001/XMLSchema-instance"
            // xsi:schemaLocation="http://www.opengis.net/ows owsExceptionReport.xsd"
            // version="1.0.0" language="en">
            // <Exception locator="service" exceptionCode="InvalidParamterValue">
            // <ExceptionText>Unknown observedProperty parameter:
            // sea_water_temperature,sea_water_salinity</ExceptionText></Exception>
            // </ExceptionReport>
            String errorText = "";
            String2.log("\n  Error from " + localSourceUrl + kvp);
            do {
              // log full exception report
              String content = xmlReader.content();
              String2.log(tags);
              if (content.length() > 0) String2.log("  content=" + content);
              String atts = xmlReader.attributesCSV();
              if (atts.length() > 0) String2.log("  atts=" + atts);

              // process the tags
              // String2.log("tags=" + tags + xmlReader.content());
              if (tags.equals("<ExceptionReport><Exception>")
                  || tags.equals("<ows:ExceptionReport><ows:Exception>")) {
                if (xmlReader.attributeValue("exceptionCode") != null)
                  errorText += xmlReader.attributeValue("exceptionCode") + ": ";
                if (xmlReader.attributeValue("locator") != null)
                  errorText += xmlReader.attributeValue("locator") + ": ";
              }

              if (tags.equals("<ServiceExceptionReport></ServiceException>")
                  || tags.equals("<ExceptionReport><Exception></ExceptionText>")
                  || tags.equals("<ows:ExceptionReport><ows:Exception></ows:ExceptionText>")) {
                errorText = "Source Exception=\"" + errorText + content + "\".";
                if (content.indexOf("No ") == 0
                    && (content.indexOf("No data for ") == 0
                        || content.indexOf(" data found for this station") > 0)) {
                  // content is now something like
                  // No currents data found for this station and date/time".
                  // but "currents" will change
                } else if (errorText.indexOf("NoApplicableCode:") >= 0) {
                  // occurs if station advertises data, but GetObservation returns error
                  // java.lang.Exception: Source Exception="NoApplicableCode: 1619910:
                  // There is no Air Temperature sensor installed at station
                  // urn:x-noaa:def:station:NOAA.NOS.CO-OPS:1619910 or dissemination has been
                  // stopped by CORMS.".
                } else {
                  throw new RuntimeException(errorText);
                }
              }

              // get the next tag
              xmlReader.nextTag();
              tags = xmlReader.allTags();
            } while (!tags.startsWith("</"));
            if (errorText == null)
              throw new RuntimeException("Source sent an ExceptionReport (no text).");
            else return;

            // response is unexpected
          } else {
            throw new RuntimeException("Error: unexpected XML content: first tag=" + tags + ".");
          }
        } finally {
          xmlReader.close();
        }
      }

      // read and process
      long processTime = System.currentTimeMillis();
      Table sosTable = new Table();
      boolean simplify = false;
      sosTable.readASCII(
          grabFileName,
          new BufferedReader(new StringReader(sar[1])),
          "",
          "",
          0,
          1,
          "",
          null,
          null,
          null,
          null,
          simplify);
      sar = null; // encourage garbage collection

      // !!! DANGER: if server changes source names or units,
      //  the code below will just return NaN (wrong!)
      //  so the datasets.xml must be updated.
      // So ensure that this dataset knows about all sosTable sourceNames.
      // !!! So all IOOS SOS datasets need to include all available variables!
      StringArray unexpectedColumns = new StringArray();
      dataVariableSourceNames(); // ensure it has been created
      int nSosTableColumns = sosTable.nColumns();
      for (int col = 0; col < nSosTableColumns; col++) {
        if (String2.indexOf(dataVariableSourceNames, sosTable.getColumnName(col)) < 0)
          unexpectedColumns.add(sosTable.getColumnName(col));
      }
      if (unexpectedColumns.size() > 0) {
        String2.log("dataVariableSourceNames=" + String2.toCSSVString(dataVariableSourceNames));
        throw new SimpleException(
            String2.ERROR
                + ": unexpected column(s) in SOS response: "
                + unexpectedColumns.toString()
                + ".");
      }

      // find the corresponding column number in sosTable  (may be -1)
      int nTableColumns = table.nColumns();
      int columnInSosTable[] = new int[nTableColumns];
      StringArray notFound = new StringArray();
      for (int col = 0; col < nTableColumns; col++) {
        columnInSosTable[col] = sosTable.findColumnNumber(table.getColumnName(col));
        if (columnInSosTable[col] < 0) notFound.add(table.getColumnName(col));
      }
      // DANGER: This is the dangerous flip side of the 'Danger' situation above.
      // If the server stops serving a variable, there is no way for ERDDAP to know.
      // There just won't be any data for that variable -- all NaNs.
      // At least all NaNs is an appropriate response.
      if (notFound.size() > 0)
        String2.log(
            "WARNING: desired sourceNames not in SOS response: "
                + notFound.toString()
                + "\n  sosTable has "
                + String2.toCSSVString(sosTable.getColumnNames()));
      else if (reallyVerbose)
        String2.log(
            "SOS sourceNames="
                + String2.toCSSVString(sosTable.getColumnNames())
                + "\n  matchingColumnsInTable="
                + String2.toCSSVString(columnInSosTable));

      // find sosTable columns with LLATI
      int sosTableLonCol = sosTable.findColumnNumber(lonSourceName);
      int sosTableLatCol = sosTable.findColumnNumber(latSourceName);
      int sosTableAltCol = sosTable.findColumnNumber(altSourceName);
      int sosTableTimeCol = sosTable.findColumnNumber(timeSourceName);
      int sosTableStationIdCol = sosTable.findColumnNumber(stationIdSourceName);

      // ensure lon, lat, time, id were found
      String tError1 = "Unexpected SOS response format: sourceName=";
      String tError2 = " wasn't found text/csv response from URL=\n" + localSourceUrl + kvp;
      if (sosTableLonCol < 0) throw new SimpleException(tError1 + lonSourceName + tError2);
      if (sosTableLatCol < 0) throw new SimpleException(tError1 + latSourceName + tError2);
      if (sosTableTimeCol < 0) throw new SimpleException(tError1 + timeSourceName + tError2);
      if (sosTableStationIdCol < 0)
        throw new SimpleException(tError1 + stationIdSourceName + tError2);

      // go through rows, adding to table  (and llatHash)
      int nSosRows = sosTable.nRows();
      for (int sosRow = 0; sosRow < nSosRows; sosRow++) {
        // process a row of data

        // make the hash key    (alt is the only optional col)
        String tHash =
            sosTable.getStringData(sosTableLonCol, sosRow)
                + ","
                + sosTable.getStringData(sosTableLatCol, sosRow)
                + ","
                + (sosTableAltCol >= 0 ? sosTable.getStringData(sosTableAltCol, sosRow) : "")
                + ","
                + sosTable.getStringData(sosTableTimeCol, sosRow)
                + ","
                + sosTable.getStringData(sosTableStationIdCol, sosRow);

        // does a row with identical LonLatAltTimeID exist in table?
        int tRow = String2.parseInt((String) llatHash.get(tHash));
        if (tRow < Integer.MAX_VALUE) {
          // merge this data into that row
          for (int col = 0; col < nTableColumns; col++) {
            int stci = columnInSosTable[col];
            String ts = stci < 0 ? "" : sosTable.getStringData(stci, sosRow);
            String tso = table.getStringData(col, tRow);
            if (ts == null) ts = "";
            // setting a number may change it a little, so do it
            table.setStringData(col, tRow, ts);
            // if there was an old value, ensure that old value = new value
            // leave this test in as insurance!
            String tsn = table.getStringData(col, tRow);
            if (tso.length() > 0 && !tso.equals(tsn)) {
              String2.log("URL=" + localSourceUrl + kvp);
              throw new SimpleException(
                  "Error: there are two rows for lon,lat,alt,time,id="
                      + tHash
                      + " and they have different data values (column="
                      + table.getColumnName(col)
                      + "="
                      + tso
                      + " and "
                      + tsn
                      + ").");
            }
          }
        } else {
          // add this row
          for (int col = 0; col < nTableColumns; col++) {
            int stci = columnInSosTable[col];
            String ts = stci < 0 ? "" : sosTable.getStringData(stci, sosRow);
            table.getColumn(col).addString(ts == null ? "" : ts);
          }

          llatHash.put(tHash, "" + (table.nRows() - 1));
        }
      }

      processTime = System.currentTimeMillis() - processTime;
      if (verbose)
        String2.log(
            "EDDTableFromSos nRows="
                + table.nRows()
                + " downloadTime="
                + downloadTime
                + "ms processTime="
                + processTime
                + "ms");

      if (!EDStatic.developmentMode)
        File2.simpleDelete(grabFileName); // don't keep in cache. SOS datasets change frequently.

    } catch (Throwable t) {
      String2.log(
          "  "
              + String2.ERROR
              + " while processing response from requestUrl="
              + localSourceUrl
              + kvp);
      if (!EDStatic.developmentMode)
        File2.simpleDelete(grabFileName); // don't keep in cache. SOS datasets change frequently.
      throw t;
    }
  }

  /**
   * This gets the data from a whoiServer.
   *
   * @param kvp the string to be added to the sourceUrl
   * @param table the table to which rows will be added
   * @return the same table or a new table
   */
  protected void readFromWhoiServer(
      int language,
      String kvp,
      Table table,
      HashMap llatHash,
      String tStationLonString,
      String tStationLatString,
      String tStationAltString,
      String tStationID)
      throws Throwable {

    if (debugMode) String2.log("* readFromWhoiServer tStationID=" + tStationID);

    int tableLonCol = table.findColumnNumber(lonSourceName);
    int tableLatCol = table.findColumnNumber(latSourceName);
    int tableAltCol = table.findColumnNumber(altSourceName);
    int tableTimeCol = table.findColumnNumber(timeSourceName);
    int tableStationIdCol = table.findColumnNumber(stationIdSourceName);

    // make tableDVI, tableObservedProperties, isStringCol   (lon/lat/time/alt/id will be null)
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
    // values that need to be parsed from the xml and held
    IntArray fieldToCol = new IntArray(); // converts results field# to table col#
    String tokenSeparator = null, blockSeparator = null, decimalSeparator = null;
    try {
      in = SSR.getUrlBufferedInputStream(localSourceUrl + kvp);
      xmlReader = new SimpleXMLReader(in);
      // request the data
      // ???Future: need to break the request up into smaller time chunks???
      xmlReader.nextTag();
    } catch (Throwable t) {
      // Normal error returns xml ExceptionReport.  So package anything else as
      // WaitThenTryAgainException.
      String2.log(
          "ERROR while trying to getUrlInputStream from "
              + localSourceUrl
              + kvp
              + "\n"
              + MustBe.throwableToString(t));
      throw t instanceof WaitThenTryAgainException
          ? t
          : new WaitThenTryAgainException(
              EDStatic.simpleBilingual(language, EDStatic.waitThenTryAgainAr)
                  + "\n("
                  + t.toString()
                  + ")");
    }
    try {

      String tags = xmlReader.allTags();
      if (tags.equals("<ServiceExceptionReport>")) {
        // <?xml version="1.0" encoding="UTF-8"?>
        // <ServiceExceptionReport version="1.0">
        // <ServiceException>
        // Format - text/xml; subtype="om/1.0.0" - is not available for offering ADCP_DATA
        // </ServiceException>
        // </ServiceExceptionReport>
        boolean okay = false;
        do {
          // process the tags
          if (debugMode) String2.log("tags=" + tags + xmlReader.content());
          if (tags.equals("<ServiceExceptionReport></ServiceException>")) {
            String content = xmlReader.content();
            // if no data, whoiServer returns empty swe:values csv table
            // if (content.startsWith("Data not available")) {
            //    okay = true; //no data for one station is not fatal error
            //    if (reallyVerbose) String2.log("Exception: " + content);
            // } else {
            throw new RuntimeException("Source Exception=\"" + content + "\".");
            // }
          }

          // get the next tag
          xmlReader.nextTag();
          tags = xmlReader.allTags();
        } while (!tags.startsWith("</"));

        if (!okay) throw new RuntimeException("Source sent an ExceptionReport (no text).");
      } else {

        String ofInterest = null;
        if (tags.equals("<om:Observation>")) ofInterest = tags;
        else if (tags.equals("<om:ObservationCollection>"))
          ofInterest = "<om:ObservationCollection><om:member><om:Observation>";
        else
          throw new RuntimeException(
              "Data source error when reading source xml: First tag="
                  + tags
                  + " should have been <om:Observation> or <om:ObservationCollection>.");

        do {
          // process the tags
          // String2.log("tags=" + tags + xmlReader.content());
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

          if (tags.startsWith(ofInterest)) { // i.e., within <om:Observation>
            String endOfTag = tags.substring(ofInterest.length());
            String content = xmlReader.content();
            String error = null;

            if (endOfTag.equals("<om:observedProperty><swe:CompositePhenomenon><swe:component>")) {
              // e.g., xlink:href="urn:ogc:phenomenon:time:iso8601" />
              String fieldName = xmlReader.attributeValue("xlink:href");
              int col = table.findColumnNumber(fieldName);
              fieldToCol.add(col);
              if (debugMode) String2.log("  field=" + fieldName + " col=" + col);

            } else if (endOfTag.equals("<swe:encoding><swe:TextBlock>")) {
              // encoding indicates how the data is stored
              // tokenSeparator="," blockSeparator=" " decimalSeparator="."
              tokenSeparator = xmlReader.attributeValue("tokenSeparator");
              blockSeparator = xmlReader.attributeValue("blockSeparator");
              decimalSeparator = xmlReader.attributeValue("decimalSeparator");
              if (debugMode)
                String2.log(
                    "  token="
                        + tokenSeparator
                        + " block="
                        + blockSeparator
                        + " decimal="
                        + decimalSeparator);

            } else if (endOfTag.equals("<om:result><swe:DataArray></swe:values>")) {
              // the results in one big csv block
              // first, ensure fieldToCol doesn't have 2 references to same column
              int nFields = fieldToCol.size();
              if (reallyVerbose) String2.log("fieldToCol=" + fieldToCol);
              for (int field = 0; field < nFields; field++) {
                int col = fieldToCol.get(field);
                if (col >= 0) { // several may be -1
                  if (fieldToCol.indexOf(col, 0) != field) // ensure none before it are the same
                  throw new RuntimeException(
                        "Two fieldToCol="
                            + fieldToCol
                            + " have the same table column reference (col#"
                            + col
                            + "="
                            + table.getColumnName(col)
                            + ").");
                }
              }

              // ensure separators are set (to defaults)
              if (tokenSeparator == null) tokenSeparator = ",";
              if (blockSeparator == null) blockSeparator = " "; // rowSeparator
              if (decimalSeparator == null) decimalSeparator = ".";
              boolean changeDecimalSeparator = !decimalSeparator.equals(".");

              // process the content (the results in one big csv block)
              // 2008-04-09T00:00:00,41.3366,-70.5564,0.0,1128.3,73.2,9,0.06,0.16,97.8,71.1,38.4,6,10,5,156.0,159.4,155.3,9.6,3.1,0,0,0,0,0
              // ???how are Strings quoted?
              int po = 0; // next po to look at
              int contentLength = content.length();
              int nCols = table.nColumns();
              while (po < contentLength) {

                // process a row of data
                int nRows1 = table.nRows() + 1;
                String rowValues[] = new String[nCols];
                for (int field = 0; field < nFields; field++) {
                  String sep = field < nFields - 1 ? tokenSeparator : blockSeparator;
                  int po2 = content.indexOf(sep, po);
                  if (po2 < 0) po2 = contentLength;
                  String value = content.substring(po, po2);
                  int col = fieldToCol.get(field);
                  if (col >= 0) {
                    // deal with decimalSeparator for numeric Columns
                    if (changeDecimalSeparator && !isStringCol[col])
                      value = String2.replaceAll(value, decimalSeparator, ".");
                    rowValues[col] = value;
                    if (debugMode)
                      String2.log(
                          "field="
                              + field
                              + " col="
                              + col
                              + " "
                              + table.getColumnName(col)
                              + " value="
                              + value);
                  }
                  po = Math.min(contentLength, po2 + sep.length());
                }

                // add lat lon alt data
                // it's usually in the result fields, but not always
                if (tableLonCol >= 0 && rowValues[tableLonCol] == null)
                  rowValues[tableLonCol] = tStationLonString;
                if (tableLatCol >= 0 && rowValues[tableLatCol] == null)
                  rowValues[tableLatCol] = tStationLatString;
                if (tableAltCol >= 0 && rowValues[tableAltCol] == null)
                  rowValues[tableAltCol] = tStationAltString;
                if (tableStationIdCol >= 0 && rowValues[tableStationIdCol] == null)
                  rowValues[tableStationIdCol] = tStationID;

                // make the hash key
                String tHash =
                    rowValues[tableLonCol]
                        + ","
                        + rowValues[tableLatCol]
                        + ","
                        + rowValues[tableAltCol]
                        + ","
                        + rowValues[tableTimeCol]
                        + ","
                        + rowValues[tableStationIdCol];

                // ensure lon, lat, time, id where found
                String tError1 = "Unexpected SOS response format: ";
                String tError2 =
                    " wasn't found.\n"
                        + "(L,L,A,T,ID="
                        + tHash
                        + ")\n"
                        + "URL="
                        + localSourceUrl
                        + kvp;
                if (rowValues[tableLonCol] == null || rowValues[tableLonCol].length() == 0)
                  throw new SimpleException(tError1 + "longitude" + tError2);
                if (rowValues[tableLatCol] == null || rowValues[tableLatCol].length() == 0)
                  throw new SimpleException(tError1 + "latitude" + tError2);
                if (rowValues[tableTimeCol] == null || rowValues[tableTimeCol].length() == 0)
                  throw new SimpleException(tError1 + "time" + tError2);
                if (rowValues[tableStationIdCol] == null
                    || rowValues[tableStationIdCol].length() == 0)
                  throw new SimpleException(tError1 + stationIdSourceName + tError2);

                // does a row with identical LonLatAltTimeID exist in table?
                int tRow = String2.parseInt((String) llatHash.get(tHash));
                if (tRow < Integer.MAX_VALUE) {
                  // merge this data into that row
                  for (int col = 0; col < nCols; col++) {
                    String ts = rowValues[col];
                    if (ts != null) {
                      PrimitiveArray pa = table.getColumn(col);
                      // if (true || verbose) {
                      // if there was an old value, ensure that old value = new value
                      // leave this test in as insurance!
                      String tso = pa.getString(tRow);
                      pa.setString(tRow, ts);
                      ts = pa.getString(tRow); // setting a number changes it, e.g., 1 -> 1.0
                      if (tso.length() > 0 && !tso.equals(ts)) {
                        String2.log("URL=" + localSourceUrl + kvp);
                        throw new SimpleException(
                            "Error: there are two rows for lon,lat,alt,time,id="
                                + tHash
                                + " and they have different data values (column="
                                + table.getColumnName(col)
                                + "="
                                + tso
                                + " and "
                                + ts
                                + ").");
                      }
                      // } else {
                      //   pa.setString(tRow, ts);
                      // }
                    }
                  }
                } else {
                  // add this row
                  for (int col = 0; col < nCols; col++) {
                    String ts = rowValues[col];
                    table.getColumn(col).addString(ts == null ? "" : ts);
                    // String2.log(col + " " + table.getColumnName(col) + " " + ts);
                  }

                  llatHash.put(tHash, "" + (table.nRows() - 1));
                }
              }
            }

            // handle the error
            if (error != null)
              throw new RuntimeException(
                  "Data source error on xml line #" + xmlReader.lineNumber() + ": " + error);
          }

          // get the next tag
          xmlReader.nextTag();
          tags = xmlReader.allTags();
        } while (!tags.startsWith("</"));
      }
    } finally {
      xmlReader.close();
    }
  }

  /**
   * This gets the data from an Oostethys SOS server. Before 2012-04, was
   * http://www.gomoos.org/cgi-bin/sos/V1.0/oostethys_sos.cgi Now
   * http://oceandata.gmri.org/cgi-bin/sos/V1.0/oostethys_sos.cgi
   *
   * @param kvp the string to be added to the sourceUrl
   * @param table the table to which rows will be added
   * @return the same table or a new table
   */
  protected void readFromOostethys(
      int language,
      String kvp,
      Table table,
      HashMap llatHash,
      String tStationLonString,
      String tStationLatString,
      String tStationAltString,
      String tStationID)
      throws Throwable {

    int tableLonCol = table.findColumnNumber(lonSourceName);
    int tableLatCol = table.findColumnNumber(latSourceName);
    int tableAltCol = table.findColumnNumber(altSourceName);
    int tableTimeCol = table.findColumnNumber(timeSourceName);
    int tableStationIdCol = table.findColumnNumber(stationIdSourceName);

    // make tableDVI, tableObservedProperties, isStringCol   (lon/lat/time/alt/id will be null)
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

    // values that need to be parsed from the xml and held
    IntArray fieldToCol = new IntArray(); // converts results field# to table col#
    String tokenSeparator = null, blockSeparator = null, decimalSeparator = null;

    try {
      in = SSR.getUrlBufferedInputStream(localSourceUrl + kvp);
      // request the data
      // ???Future: need to break the request up into smaller time chunks???
      xmlReader = new SimpleXMLReader(in);
      xmlReader.nextTag();
    } catch (Throwable t) {
      // Normal error returns xml ExceptionReport.  So package anything else as
      // WaitThenTryAgainException.
      String2.log(
          "ERROR while trying to getUrlInputStream from "
              + localSourceUrl
              + kvp
              + "\n"
              + MustBe.throwableToString(t));
      throw t instanceof WaitThenTryAgainException
          ? t
          : new WaitThenTryAgainException(
              EDStatic.simpleBilingual(language, EDStatic.waitThenTryAgainAr)
                  + "\n("
                  + t.toString()
                  + ")");
    }

    try {

      String tags = xmlReader.allTags();
      // String2.log("tags=" + tags + "\ncontent=" + xmlReader.content());
      if (tags.equals("<ServiceExceptionReport>")
          || // tamu
          tags.equals("<ows:ExceptionReport>")) { // gomoos, ioos
        // 2014-12-22 ioos:
        // https://sdf.ndbc.noaa.gov/sos/server.php?service=SOS&version=1.0.0&request=GetObservation&offering=urn:ioos:station:wmo:46013&observedProperty=http://mmisw.org/ont/cf/parameter/waves&responseFormat=text/csv&eventTime=2008-08-01T00:00:00Z/2008-09-05T00:00:00Z
        // <ows:ExceptionReport xmlns:ows="http://www.opengis.net/ows/1.1"
        // xmlns:xsi="https://www.w3.org/2001/XMLSchema-instance"
        // xsi:schemaLocation="http://www.opengis.net/ows/1.1 owsExceptionReport.xsd"
        // version="1.0.0" xml:lang="en">
        // <ows:Exception exceptionCode="InvalidParameterValue" locator="eventTime">
        // <ows:ExceptionText>No more than 31 days of data can be requested.</ows:ExceptionText>
        // </ows:Exception>
        // </ows:ExceptionReport>
        boolean okay = false;
        do {
          // process the tags
          if (debugMode) String2.log("tags=" + tags + xmlReader.content());
          if (tags.equals("<ServiceExceptionReport></ServiceException>")
              || // tamu
              tags.equals(
                  "<ows:ExceptionReport><ows:Exception></ows:ExceptionText>")) { // gomoos, ioos
            String content = xmlReader.content();
            if (content.startsWith("Data not available")) {
              okay = true; // no data for one station is not fatal error
              if (reallyVerbose) String2.log("Exception: " + content);
            } else {
              throw new RuntimeException("Source Exception=\"" + content + "\".");
            }
          }

          // get the next tag
          xmlReader.nextTag();
          tags = xmlReader.allTags();
        } while (!tags.startsWith("</"));

        if (!okay) throw new RuntimeException("Source sent an ExceptionReport (no text).");
      } else {

        String ofInterest = null;
        if (tags.equals("<om:Observation>")) ofInterest = tags;
        else if (tags.equals("<om:ObservationCollection>"))
          ofInterest = "<om:ObservationCollection><om:member><om:Observation>";
        else
          throw new RuntimeException(
              "Data source error when reading source xml: First tag="
                  + tags
                  + " should have been <om:Observation> or <om:ObservationCollection>.");

        do {
          // process the tags
          if (debugMode) String2.log(tags + xmlReader.content());

          if (tags.startsWith(ofInterest)) { // i.e., within <om:Observation>
            String endOfTag = tags.substring(ofInterest.length());
            String content = xmlReader.content();
            String error = null;

            if (endOfTag.equals(
                "<om:featureOfInterest><swe:GeoReferenceableFeature>"
                    + "<gml:location><gml:Point></gml:coordinates>")) {
              // lat lon alt    if present, has precedence over station table
              // VAST has this; others don't
              String lla[] = String2.split(content, ' ');
              if (lla.length >= 2) {
                tStationLatString = lla[0];
                tStationLonString = lla[1];
                if (lla.length >= 3) tStationAltString = lla[2];
              }
            } else if ( // endOfTag.equals("<om:resultDefinition><swe:DataBlockDefinition>" +
            //         "<swe:components><swe:DataRecord><swe:field>") ||  //old?
            endOfTag.equals(
                "<om:result><swe:DataArray>" + "<swe:elementType><swe:DataRecord><swe:field>")) {
              // field    PlatformName, latitude, longitude, time, depth have this
              // other fields have "observedProperty6"; see "definition" below
              String fieldName = xmlReader.attributeValue("name");
              int col = table.findColumnNumber(fieldName);
              fieldToCol.add(col);
              if (debugMode)
                String2.log("*** field name found: col=" + col + " fieldName=" + fieldName);

            } else if (endOfTag.equals(
                "<om:result><swe:DataArray>"
                    + "<swe:elementType><swe:DataRecord><swe:field><swe:Quantity>")) {
              // definition   use this if field name was "observedProperty"i
              int nFields = fieldToCol.size();
              if (nFields > 0 && fieldToCol.get(nFields - 1) < 0) {
                String definition = xmlReader.attributeValue("definition");
                int col = String2.indexOf(tableObservedProperties, definition);
                fieldToCol.set(nFields - 1, col); // change from -1 to col
                if (debugMode)
                  String2.log(
                      "*** field definition found: col=" + col + " definition=" + definition);
              }

            } else if ( // endOfTag.equals("<om:resultDefinition><swe:DataBlockDefinition>" +
            //         "<swe:encoding><swe:AsciiBlock>") ||  //old oostethys has this
            // endOfTag.equals("<om:resultDefinition><swe:DataBlockDefinition>" +
            //         "<swe:encoding><swe:TextBlock>")) {   //old VAST has this
            endOfTag.equals("<om:result><swe:DataArray><swe:encoding>")) {
              // encoding indicates how the data is stored
              // tokenSeparator="," blockSeparator=" " decimalSeparator="."
              tokenSeparator = xmlReader.attributeValue("tokenSeparator");
              blockSeparator = xmlReader.attributeValue("blockSeparator");
              decimalSeparator = xmlReader.attributeValue("decimalSeparator");
              if (debugMode)
                String2.log(
                    "  token="
                        + tokenSeparator
                        + " block="
                        + blockSeparator
                        + " decimal="
                        + decimalSeparator);

            } else if ( // endOfTag.equals("</om:result>")) { //old
            endOfTag.equals("<om:result><swe:DataArray></swe:values>")) {
              // the results in one big block
              // first, ensure fieldToCol doesn't have 2 references to same column
              int nFields = fieldToCol.size();
              if (reallyVerbose) String2.log("fieldToCol=" + fieldToCol);
              for (int field = 0; field < nFields; field++) {
                int col = fieldToCol.get(field);
                if (col >= 0) { // several may be -1
                  if (fieldToCol.indexOf(col, 0) != field) // ensure none before it are the same
                  throw new RuntimeException(
                        "Two fieldToCol="
                            + fieldToCol
                            + " have the same table column reference (col#"
                            + col
                            + "="
                            + table.getColumnName(col)
                            + ").");
                }
              }

              // ensure separators are set (to defaults)
              if (tokenSeparator == null) tokenSeparator = ",";
              if (blockSeparator == null) blockSeparator = " "; // rowSeparator
              if (decimalSeparator == null) decimalSeparator = ".";
              boolean changeDecimalSeparator = !decimalSeparator.equals(".");

              // process the content (the results in one big block)
              //  <om:result>2007-06-18T00:50:00Z,34.68,-72.66,0,24.3 ...
              // ???how are Strings quoted?
              int po = 0; // next po to look at
              int contentLength = content.length();
              int nCols = table.nColumns();
              while (po < contentLength) {

                // process a row of data
                int nRows1 = table.nRows() + 1;
                String rowValues[] = new String[nCols];
                for (int field = 0; field < nFields; field++) {
                  String sep = field < nFields - 1 ? tokenSeparator : blockSeparator;
                  int po2 = content.indexOf(sep, po);
                  if (po2 < 0) po2 = contentLength;
                  String value = content.substring(po, po2);
                  int col = fieldToCol.get(field);
                  if (col >= 0) {
                    // deal with decimalSeparator for numeric Columns
                    if (changeDecimalSeparator && !isStringCol[col])
                      value = String2.replaceAll(value, decimalSeparator, ".");
                    rowValues[col] = value;
                    if (debugMode)
                      String2.log(
                          "field="
                              + field
                              + " col="
                              + col
                              + " "
                              + table.getColumnName(col)
                              + " value="
                              + value);
                  }
                  po = Math.min(contentLength, po2 + sep.length());
                }

                // add lat lon alt data
                // it's usually in the result fields, but not always
                if (tableLonCol >= 0 && rowValues[tableLonCol] == null)
                  rowValues[tableLonCol] = tStationLonString;
                if (tableLatCol >= 0 && rowValues[tableLatCol] == null)
                  rowValues[tableLatCol] = tStationLatString;
                if (tableAltCol >= 0 && rowValues[tableAltCol] == null)
                  rowValues[tableAltCol] = tStationAltString;
                if (tableStationIdCol >= 0 && rowValues[tableStationIdCol] == null)
                  rowValues[tableStationIdCol] = tStationID;

                // make the hash key
                String tHash =
                    rowValues[tableLonCol]
                        + ","
                        + rowValues[tableLatCol]
                        + ","
                        + rowValues[tableAltCol]
                        + ","
                        + rowValues[tableTimeCol]
                        + ","
                        + rowValues[tableStationIdCol];

                // ensure lon, lat, time, id where found
                String tError1 = "Unexpected SOS response format: ";
                String tError2 =
                    " wasn't found.\n"
                        + "(L,L,A,T,ID="
                        + tHash
                        + ")\n"
                        + "URL="
                        + localSourceUrl
                        + kvp;
                if (rowValues[tableLonCol] == null || rowValues[tableLonCol].length() == 0)
                  throw new SimpleException(tError1 + "longitude" + tError2);
                if (rowValues[tableLatCol] == null || rowValues[tableLatCol].length() == 0)
                  throw new SimpleException(tError1 + "latitude" + tError2);
                if (rowValues[tableTimeCol] == null || rowValues[tableTimeCol].length() == 0)
                  throw new SimpleException(tError1 + "time" + tError2);
                if (rowValues[tableStationIdCol] == null
                    || rowValues[tableStationIdCol].length() == 0)
                  throw new SimpleException(tError1 + stationIdSourceName + tError2);

                // does a row with identical LonLatAltTimeID exist in table?
                int tRow = String2.parseInt((String) llatHash.get(tHash));
                if (tRow < Integer.MAX_VALUE) {
                  // merge this data into that row
                  for (int col = 0; col < nCols; col++) {
                    String ts = rowValues[col];
                    if (ts != null) {
                      PrimitiveArray pa = table.getColumn(col);
                      // if (true || verbose) {
                      // if there was an old value, ensure that old value = new value
                      // leave this test in as insurance!
                      String tso = pa.getString(tRow);
                      pa.setString(tRow, ts);
                      ts = pa.getString(tRow); // setting a number changes it, e.g., 1 -> 1.0
                      if (tso.length() > 0 && !tso.equals(ts)) {
                        String2.log("URL=" + localSourceUrl + kvp);
                        throw new SimpleException(
                            "Error: there are two rows for lon,lat,alt,time,id="
                                + tHash
                                + " and they have different data values (column="
                                + table.getColumnName(col)
                                + "="
                                + tso
                                + " and "
                                + ts
                                + ").");
                      }
                      // } else {
                      //   pa.setString(tRow, ts);
                      // }
                    }
                  }
                } else {
                  // add this row
                  for (int col = 0; col < nCols; col++) {
                    String ts = rowValues[col];
                    table.getColumn(col).addString(ts == null ? "" : ts);
                    // String2.log(col + " " + table.getColumnName(col) + " " + ts);
                  }

                  llatHash.put(tHash, "" + (table.nRows() - 1));
                }
              }
            }

            // handle the error
            if (error != null)
              throw new RuntimeException(
                  "Data source error on xml line #" + xmlReader.lineNumber() + ": " + error);
          }

          // get the next tag
          xmlReader.nextTag();
          tags = xmlReader.allTags();
        } while (!tags.startsWith("</"));
      }
    } finally {
      xmlReader.close();
    }
  }

  /**
   * @param inputStream Best if buffered.
   */
  public static Table readIoos52NXmlDataTable(InputStream inputStream) throws Exception {
    SimpleXMLReader xmlReader = new SimpleXMLReader(inputStream);
    try {
      xmlReader.nextTag();
      String tags = xmlReader.allTags();

      if (!xmlReader.tag(0).equals("om:ObservationCollection"))
        throw new IllegalArgumentException(
            "For IOOS 52N SOS servers, the first SOS tag should have been "
                + "<om:ObservationCollection>, not "
                + tags
                + ".");
      Table table = new Table();
      /* */
      // <om:ObservationCollection><om:member><om:Observation><om:result>
      // <swe2:DataRecord>
      // <swe2:field name="observationData">  //there are lots of fields; only this matters
      // <swe2:DataArray
      // definition="http://mmisw.org/ont/ioos/swe_element_type/sensorObservationCollection">
      //  <swe2:elementCount><swe2:Count><swe2:value>5312</swe2:value>
      //  <swe2:elementType name="observations">
      //    <swe2:DataRecord
      // definition="http://mmisw.org/ont/ioos/swe_element_type/sensorObservations">
      //      <swe2:field name="time">
      //      ... other fields

      //        <swe2:DataChoice definition="http://mmisw.org/ont/ioos/swe_element_type/sensors">
      //          <swe2:item name="comps_42013_airpressure">
      //            <swe2:DataRecord definition="http://mmisw.org/ont/ioos/swe_element_type/sensor">
      //              <swe2:field name="air_pressure">
      //                <swe2:Quantity definition="http://mmisw.org/ont/cf/parameter/air_pressure">
      //                  <swe2:uom xlink:href="urn:ogc:def:uom:udunits:2:hPa"/>

      //        <swe2:encoding>
      //          <swe2:TextEncoding decimalSeparator="." tokenSeparator=","
      // blockSeparator="&#10;"/>

      int capacity = 128;
      String obsDataTags = // but only if attribute name="observationData"
          "<om:ObservationCollection><om:member><om:Observation><om:result>"
              + "<swe2:DataRecord><swe2:field>";
      String dataArrayTags = obsDataTags + "<swe2:DataArray>";
      String elementCountTags = dataArrayTags + "<swe2:elementCount><swe2:Count></swe2:value>";
      String fieldName1Tags =
          dataArrayTags
              + "<swe2:elementType><swe2:DataRecord><swe2:field>"; // name="time" or "sensor">
      String fieldName2Tags =
          fieldName1Tags
              + "<swe2:DataChoice><swe2:item><swe2:DataRecord><swe2:field>"; // name="air_pressure">
      String unitsTags =
          fieldName2Tags
              + "<swe2:Quantity><swe2:uom>"; // xlink:href="urn:ogc:def:uom:udunits:2:hPa"
      String endDataRecordTags = fieldName1Tags + "<swe2:DataChoice><swe2:item></swe2:DataRecord>";
      String textEncodingTags = dataArrayTags + "<swe2:encoding><swe2:TextEncoding>";
      String csvTags = dataArrayTags + "</swe2:values>";
      char tokenSeparator = ',';
      String blockSeparator = "\n";

      // skip quickly to obsDataTags with name=observationData
      while (true) {
        xmlReader.nextTag();
        // uninteresting <swe2:field>?
        if (xmlReader.stackSize() == 0)
          throw new RuntimeException("Expected tag not found: " + obsDataTags);
        if (xmlReader.stackSize() == 6
            && xmlReader.topTag().equals("swe2:field")
            && xmlReader.allTags().equals(obsDataTags)
            && "observationData".equals(xmlReader.attributeValue("name"))) break;
      }

      xmlReader.nextTag();
      tags = xmlReader.allTags();
      do {
        // process the tags
        if (debugMode) String2.log("tags=" + tags + xmlReader.content());

        // elementCount
        if (tags.equals(elementCountTags)) {
          int ti = String2.parseInt(xmlReader.content());
          if (ti > 0 && ti < Integer.MAX_VALUE) {
            capacity = ti;
            if (debugMode) String2.log("  elementCount=" + ti);
          }

          // primary fieldName
        } else if (tags.equals(fieldName1Tags)) {
          String ts = xmlReader.attributeValue("name");
          if (String2.isSomething(ts)) {
            table.addColumn(
                ts.equals("sensor") ? stationIdDestinationName : ts,
                PrimitiveArray.factory(
                    ts.equals("time") || ts.equals("sensor") ? PAType.STRING : PAType.DOUBLE,
                    capacity,
                    false)); // active?
          }

          // secondary fieldName
        } else if (tags.equals(fieldName2Tags)) {
          String ts = xmlReader.attributeValue("name");
          if (String2.isSomething(ts)) {
            table.addColumn(ts, PrimitiveArray.factory(PAType.DOUBLE, capacity, false)); // active?
          }

          // units (for last created column)
        } else if (tags.equals(unitsTags)) {
          String ts = xmlReader.attributeValue("xlink:href");
          if (String2.isSomething(ts)) {
            table
                .columnAttributes(table.nColumns() - 1)
                .add("units", ts.substring(ts.lastIndexOf(":") + 1));
          }

          // end of secondary fieldNames and units
        } else if (tags.equals(endDataRecordTags)) {
          xmlReader.skipToStackSize(8);
          // should be at </swe2:elementType> after outer </swe2:DataRecord>
          Test.ensureEqual(xmlReader.topTag(), "/swe2:elementType", "skipToStacksize is wrong");

          // encoding
        } else if (tags.equals(textEncodingTags)) {
          String ts = xmlReader.attributeValue("tokenSeparator");
          if (String2.isSomething(ts)) {
            tokenSeparator = ts.charAt(0);
            if (reallyVerbose) String2.log("  tokenSeparator=" + String2.annotatedString("" + ts));
          }
          ts = xmlReader.attributeValue("blockSeparator");
          if (String2.isSomething(ts)) {
            blockSeparator = ts;
            if (reallyVerbose) String2.log("  blockSeparator=" + String2.annotatedString(ts));
          }

          // data
        } else if (tags.equals(csvTags)) {
          String content = xmlReader.content();
          int start = 0;
          int stop = content.indexOf(blockSeparator);
          boolean isCsv = ',' == tokenSeparator;
          int nCols = table.nColumns();
          int stationCol = table.findColumnNumber(stationIdDestinationName);
          PrimitiveArray pa[] = new PrimitiveArray[nCols];
          for (int col = 0; col < nCols; col++) pa[col] = table.getColumn(col);
          while (true) {
            // get the items on one line
            String oneLine = content.substring(start, stop == -1 ? content.length() : stop).trim();
            String items[] =
                isCsv
                    ? StringArray.arrayFromCSV(oneLine)
                    : // does handle "'d phrases
                    String2.split(oneLine, tokenSeparator);
            if (items.length > stationCol) {
              int po = items[stationCol].lastIndexOf("_");
              if (po >= 0) items[stationCol] = items[stationCol].substring(0, po);
            }

            // store the items
            for (int col = 0; col < nCols; col++) {
              // so always fill each column in table
              pa[col].addString(col < items.length ? items[col] : "");
            }

            // get the next start stop
            if (stop == -1 || stop >= content.length() - 1) break;
            start = stop + 1;
            stop = content.indexOf(blockSeparator, start);
          }
        }

        // read next tag
        xmlReader.nextTag();
        tags = xmlReader.allTags();

      } while (!tags.startsWith("</")); // end of file

      return table;
    } finally {
      xmlReader.close();
    }
  }

  private static String
      standardSummary = // from http://www.oostethys.org/ogc-oceans-interoperability-experiment
          // [GONE]
          "The OCEANS IE -- formally approved as an OGC Interoperability\n"
              + "Experiment in December 2006 -- engages data managers and scientists\n"
              + "in the Ocean-Observing community to advance their understanding and\n"
              + "application of various OGC specifications, solidify demonstrations\n"
              + "for Ocean Science application areas, harden software\n"
              + "implementations, and produce candidate OGC Best Practices documents\n"
              + "that can be used to inform the broader ocean-observing community.\n"
              +
              // "To achieve these goals, the OCEANS IE engages the OGC membership\n" +
              // "to assure that any recommendations from the OCEANS IE will\n" +
              // "properly leverage the OGC specifications. The OCEANS IE could\n" +
              // "prompt Change Requests on OGC Specifications, which would be\n" +
              // "provided to the OGC Technical Committee to influence the\n" +
              // "underlying specifications. However, this IE will not develop\n" +
              // "any new specifications, rather, participants will implement,\n" +
              // "test and document experiences with existing specifications.\n" +
              "\n"
              + "Because of the nature of SOS requests, requests for data MUST\n"
              + "include constraints for the longitude, latitude, time, and/or\n"
              + "station_id variables.\n"
              + "\n"
              + "Initiators: SURA (lead), Texas A&M University, MBARI, GoMOOS and\n"
              + "Unidata.\n"
              + "\n"
              + "Specific goals:\n"
              + "* Compare Sensor Observation Service (SOS) from the OGC's Sensor\n"
              + "  Web Enablement (SWE) initiative to the Web Feature Service (WFS)\n"
              + "  as applied to ocean data in a variety of data formats including\n"
              + "  text files, netCDF files, relational databases, and possibly\n"
              + "  native sensor output; (see Experiment #1 for details)\n"
              + "* Make use of semantic mediation via Semantic Web technologies to\n"
              + "  allow plurality of identification for source types (platforms\n"
              + "  and sensors) and phenomena types;\n"
              + "* Test aggregation services and caching strategies to provide\n"
              + "  efficient queries;\n"
              + "* Explore possible enhancements of THREDDS server, so that THREDDS\n"
              + "  resident data sources might be made available via SOS or WFS;"; // better

  // summary?

  // IRIS - This SOS listed at
  //  http://www.oostethys.org/ogc-oceans-interoperability-experiment/experiment-1 [GONE]
  //  (it may list others in future)
  // Specifically http://demo.transducerml.org:8080/ogc/
  // But the results are a tml reference. For now, don't get into that.
  // WAIT!!! Can I request other results format???

  /**
   * NOT FINISHED. But useful for what it does so far. <br>
   * This generates a rough draft of the datasets.xml entry for an EDDTableFromSOS. <br>
   * The XML can then be edited by hand and added to the datasets.xml file.
   *
   * @param useCachedInfo for testing purposes, this uses a cached GetCapabilities document if
   *     possible (else caches what it gets)
   * @param tLocalSourceUrl
   * @param sosVersion e.g., 1.0.0 (or null or "")
   * @param sosServerType may be "", or one of IOOS_52N, IOOS_NcSOS, IOOS_NDBC, IOOS_NOS, OOSTethys,
   *     WHOI (case insensitive)
   * @return a suggested chunk of xml for this dataset for use in datasets.xml
   * @throws Throwable if trouble, e.g., if no Grid or Array variables are found. If no trouble,
   *     then a valid dataset.xml chunk has been returned.
   */
  public static String generateDatasetsXml(
      boolean useCachedInfo, String tLocalSourceUrl, String sosVersion, String sosServerType)
      throws Throwable {

    tLocalSourceUrl = EDStatic.updateUrls(tLocalSourceUrl); // http: to https:
    String2.log(
        "\n*** EDDTableFromSos.generateDatasetsXml"
            + "\nuseCachedInfo="
            + useCachedInfo
            + " localSourceUrl="
            + tLocalSourceUrl
            + "\nsosVersion="
            + sosVersion
            + " sosServerType="
            + sosServerType);
    StringBuilder sb = new StringBuilder();
    String tPublicSourceUrl = convertToPublicSourceUrl(tLocalSourceUrl);
    sosServerType = sosServerType == null ? "" : sosServerType.trim();
    String sstlc = sosServerType.toLowerCase();
    boolean ioos52NServer = sstlc.equals(SosServerTypeIoos52N.toLowerCase());
    boolean ioosNdbcServer = sstlc.equals(SosServerTypeIoosNdbc.toLowerCase());
    boolean ioosNcSOSServer = sstlc.equals(SosServerTypeIoosNcSOS.toLowerCase());
    boolean ioosNosServer = sstlc.equals(SosServerTypeIoosNos.toLowerCase());
    boolean oostethysServer = sstlc.equals(SosServerTypeOostethys.toLowerCase());
    boolean whoiServer = sstlc.equals(SosServerTypeWhoi.toLowerCase());

    String tUrl = tLocalSourceUrl + "?service=SOS&request=GetCapabilities";
    if (sosVersion != null && sosVersion.length() > 0) {
      // if (ioosNcbcServer || ioosNosServer) //not needed. 1.0.0 is default and only.
      //    tUrl += "&version=" + sosVersion;
      if (ioos52NServer) // AcceptVersions needed. This class only supports 1.0.0 currently.
      tUrl += "&AcceptVersions=" + sosVersion;
    }
    // if (reallyVerbose) String2.log(SSR.getUrlResponseStringUnchanged(tUrl));
    File2.makeDirectory(sosCopyDir);
    String safeFileName = sosCopyDir + String2.md5Hex12(tUrl);
    if (!useCachedInfo || !File2.isFile(safeFileName)) {
      String2.log("downloading GetCapabilities" + "\n  from: " + tUrl + "\n  to: " + safeFileName);
      SSR.downloadFile(tUrl, safeFileName, false); // tryToUseCompression
    } else {
      String2.log(
          "using cached GetCapabilities"
              + "\n  from file: "
              + safeFileName
              + "\n  from URL: "
              + tUrl);
    }

    // gather station info and uniqueObservedProperties
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

    SimpleXMLReader xmlReader =
        new SimpleXMLReader(File2.getDecompressedBufferedInputStream(safeFileName));
    try {
      xmlReader.nextTag();
      String tags = xmlReader.allTags();
      boolean ioosServer = false;
      if (xmlReader.tag(0).equals("Capabilities")) {
        sosPrefix = ""; // ioosServer
        if (sosServerType.length() == 0) // not explicitly declared
        ioosServer = true;
      } else if (xmlReader.tag(0).equals("sos:Capabilities")) {
        sosPrefix = "sos:"; // oostethys, ioos52N
      } else {
        String2.log(SSR.getUrlResponseStringUnchanged(tUrl));
        throw new RuntimeException(
            "The first SOS capabilities tag=\""
                + tags
                + "\" should have been <Capabilities> or <sos:Capabilities>.");
      }
      offeringTag =
          "<"
              + sosPrefix
              + "Capabilities><"
              + sosPrefix
              + "Contents><"
              + sosPrefix
              + "ObservationOfferingList><"
              + sosPrefix
              + "ObservationOffering>";
      offeringEndTag =
          "<"
              + sosPrefix
              + "Capabilities><"
              + sosPrefix
              + "Contents><"
              + sosPrefix
              + "ObservationOfferingList></"
              + sosPrefix
              + "ObservationOffering>";

      do {
        // process the tags
        if (debugMode) String2.log("tags=" + tags + xmlReader.content());

        if (tags.endsWith("<ows:ServiceIdentification></ows:ServiceTypeVersion>")) {
          String s = xmlReader.content();
          if (verbose) String2.log("  ServiceTypeVersion=" + s);
          if (sosVersion == null
              || sosVersion.length() == 0
              || s.equals("1.0.0")) { // preference to 1.0.0 (ioos52N also offers 2.0.0)
            sosVersion = s; // e.g., "0.0.31" or "1.0.0"
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
          if (!s.isEmpty() && !s.toLowerCase().equals("none")) {
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
          if (tags.equals(offeringTag)) offeringTagCount++;
          // if (debugMode) String2.log("offering=" + endOfTag + xmlReader.content());

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

          } else if (endOfTag.equals("</gml:name>")) { // ioosServer and OOSTethys have this
            // e.g., 44004
            tStationID = content;
            // remove  so names are shorter on table below
            String pre = ":station:";
            int prePo = tStationID.indexOf(pre);
            if (prePo >= 0) tStationID = tStationID.substring(prePo + pre.length());
            if (reallyVerbose) String2.log("  tStationID=" + tStationID);

          } else if (endOfTag.equals("<" + sosPrefix + "observedProperty>")) {
            // handle non-composite observedProperty
            // <sos:observedProperty
            // xlink:href="http://marinemetadata.org/cf#sea_water_temperature"/>
            //                      http://marinemetadata.org is GONE!
            // NOW
            // xlink:href="http://mmisw.org/ont/cf/parameter/sea_water_temperature"

            String tXlink = xmlReader.attributeValue("xlink:href"); // without quotes
            if (tXlink != null && !tXlink.toLowerCase().equals("none")) {
              if (reallyVerbose) String2.log("  observedProperty=" + tXlink);
              int opPo = uniqueObsProp.indexOf(tXlink);
              if (opPo < 0) {
                opPo = uniqueObsProp.size();
                uniqueObsProp.add(tXlink);
              }
              while (tStationObsPropList.length() <= opPo) tStationObsPropList.append(' ');
              tStationObsPropList.setCharAt(opPo, (char) (65 + opPo));
            }

          } else if (endOfTag.equals(
              "<" + sosPrefix + "observedProperty><swe:CompositePhenomenon>")) {
            // handle composite observedProperty
            // <swe:CompositePhenomenon gml:id="WEATHER_OBSERVABLES">
            String tXlink = xmlReader.attributeValue("gml:id"); // without quotes
            if (tXlink != null && !tXlink.toLowerCase().equals("none")) {
              if (reallyVerbose) String2.log("  composite observedProperty=" + tXlink);
              int opPo = uniqueObsProp.indexOf(tXlink);
              if (opPo < 0) {
                opPo = uniqueObsProp.size();
                uniqueObsProp.add(tXlink);
              }
              while (tStationObsPropList.length() <= opPo) tStationObsPropList.append(' ');
              tStationObsPropList.setCharAt(opPo, (char) (65 + opPo));
            }
          }

          // handle the error
          if (error != null)
            throw new RuntimeException(
                "Error on capabilities xml line #"
                    + xmlReader.lineNumber()
                    + " stationID="
                    + tStationID
                    + ": "
                    + error);

          // end of a station
        } else if (tags.startsWith(offeringEndTag)) {
          // String2.log("endTag");
          if (tStationID.length() > 0 && tStationObsPropList.length() > 0) {
            stationIDs.add(tStationID);
            stationHasObsProp.add(tStationObsPropList.toString());
          } else {
            String2.log(
                "Invalid Station id="
                    + tStationID
                    + " tStationObsPropList='"
                    + tStationObsPropList
                    + "'");
          }
          tStationID = "";
          tStationObsPropList.setLength(0);
        }

        // get the next tag
        xmlReader.nextTag();
        tags = xmlReader.allTags();
      } while (!tags.startsWith("</"));
    } finally {
      xmlReader.close();
    }

    if (verbose)
      String2.log(
          "\n"
              + "sosPrefix="
              + sosPrefix
              + "\n"
              + "offeringTag="
              + offeringTag
              + " count="
              + offeringTagCount);
    if (sosVersion == null || sosVersion.length() == 0) sosVersion = "1.0.0"; // default

    // write the station/obsProp info
    if (stationIDs.size() == 0)
      throw new RuntimeException(
          "No stations found! Try a different sosServerType.\n"
              + "  offeringTag="
              + offeringTag
              + "  stationID name tag="
              + "<gml:name>");
    sb.append(
        "<!-- You have to choose which observedProperties will be used for this dataset.\n\n");
    int longestStationID = Math.max(19, stationIDs.maxStringLength());
    int longestHasProp = stationHasObsProp.maxStringLength();
    sb.append(
        "\n"
            + String2.left("   n  Station (shortened)", 6 + longestStationID)
            + "  Has ObservedProperty\n"
            + "----  "
            + String2.makeString('-', longestStationID)
            + "  "
            + String2.makeString('-', 60)
            + "\n");
    for (int si = 0; si < stationIDs.size(); si++) {
      sb.append(
          String2.right("" + si, 4)
              + "  "
              + String2.left(stationIDs.get(si), longestStationID)
              + "  "
              + stationHasObsProp.get(si)
              + "\n");
    }

    // list the props
    if (uniqueObsProp.size() == 0)
      throw new RuntimeException(
          "No observedProperties found! Try a different sosServerType.\n"
              + "  offeringTag="
              + offeringTag
              + "\n"
              + "  observedPropertyTag="
              + "<"
              + sosPrefix
              + "observedProperty>");
    sb.append(
        "\n id  ObservedProperty\n" + "---  --------------------------------------------------\n");
    for (int op = 0; op < uniqueObsProp.size(); op++)
      sb.append(String2.right("" + (char) (65 + op), 3) + "  " + uniqueObsProp.get(op) + "\n");

    // generate a table with the datasets information
    Table table = new Table();
    Attributes sgAtts = new Attributes(); // source global atts
    sgAtts.add("infoUrl", infoUrl);
    sgAtts.add("institution", institution);
    if (ioos52NServer) summary += slowSummaryWarning;
    sgAtts.add("summary", summary);
    sgAtts.add("title", title);
    sgAtts.add("license", license);
    sgAtts.add("cdm_timeseries_variables", "station_id, longitude, latitude");
    sgAtts.add("subsetVariables", "station_id, longitude, latitude");

    Attributes gAddAtts =
        makeReadyToUseAddGlobalAttributesForDatasetsXml(
            sgAtts,
            "TimeSeries",
            tLocalSourceUrl,
            new Attributes(), // externalAtts
            new HashSet()); // suggestedKeywords
    Attributes gAtts = table.globalAttributes();
    gAtts.add(sgAtts); // since only addAtts will be printed
    gAtts.add(gAddAtts);
    for (int op = 0; op < uniqueObsProp.size(); op++) {
      String prop = uniqueObsProp.get(op);
      String dvName = prop;
      int po = dvName.lastIndexOf("#");
      if (po < 0) po = dvName.lastIndexOf("/");
      if (po < 0) po = dvName.lastIndexOf(":");
      if (po >= 0) dvName = dvName.substring(po + 1);
      Attributes sourceAtts = new Attributes();
      sourceAtts.add("standard_name", dvName);
      PrimitiveArray destPA = new DoubleArray();
      Attributes addAtts =
          makeReadyToUseAddVariableAttributesForDatasetsXml(
              sgAtts,
              sourceAtts,
              null,
              dvName,
              destPA.elementType() != PAType.STRING, // tryToAddStandardName
              destPA.elementType() != PAType.STRING,
              false); // addColorBarMinMax, tryToFindLLAT
      // then add the sourceAtts to the addAtts (since addAtts is all that will be shown)
      addAtts.add("observedProperty", prop);
      addAtts.add("standard_name", dvName);
      addAtts.add("units", "???");
      table.addColumn(op, dvName, destPA, addAtts);
    }

    // tryToFindLLAT. LLAT are already known, but this will fix invalid destinationNames.
    tryToFindLLAT(null, table);

    // don't use suggestSubsetVariables() since sourceTable not available

    // *** generate the datasets.xml
    sb.append(
        "\n"
            + "NOTE! For SOS datasets, you must look at the observedProperty's\n"
            + "phenomenaDictionary URL (or an actual GetObservations response)\n"
            + "to see which dataVariables will be returned for a given phenomenon.\n"
            + "(longitude, latitude, altitude, and time are handled separately.)\n"
            + "-->\n");

    sb.append(
        "<dataset type=\"EDDTableFromSOS\" datasetID=\""
            + suggestDatasetID(tPublicSourceUrl)
            + "\" active=\"true\">\n"
            + "    <sourceUrl>"
            + XML.encodeAsXML(tLocalSourceUrl)
            + "</sourceUrl>\n"
            + "    <sosVersion>"
            + XML.encodeAsXML(sosVersion)
            + "</sosVersion>\n"
            + "    <sosServerType>"
            + sosServerType
            + "</sosServerType>\n"
            + "    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n"
            + "    <observationOfferingIdRegex>.+</observationOfferingIdRegex>\n"
            + "    <requestObservedPropertiesSeparately>"
            + (ioos52NServer || ioosNdbcServer || ioosNosServer ? "true" : "false")
            + "</requestObservedPropertiesSeparately>\n"
            + "    <longitudeSourceName>longitude</longitudeSourceName>\n"
            + "    <latitudeSourceName>latitude</latitudeSourceName>\n"
            + "    <altitudeSourceName>???depth???ioos:VerticalPosition</altitudeSourceName>\n"
            + "    <altitudeMetersPerSourceUnit>-1</altitudeMetersPerSourceUnit>\n"
            + "    <timeSourceName>time</timeSourceName>\n"
            + "    <timeSourceFormat>yyyy-MM-dd'T'HH:mm:ss"
            + (ioos52NServer ? ".000" : "")
            + "Z</timeSourceFormat>\n"
            + "    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below,\n"
            + "      notably, cdm_timeseries_variables, subsetVariables.\n"
            + "    -->\n");
    sb.append(writeAttsForDatasetsXml(true, table.globalAttributes(), "    "));
    // last 2 params: includeDataType, questionDestinationName
    sb.append(writeVariablesForDatasetsXml(null, table, "dataVariable", true, false));
    sb.append("</dataset>\n");

    String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
    return sb.toString();
  }

  /**
   * This generates ready-to-use datasets.xml (one dataset per observed property) for an IOOS SOS
   * server. <br>
   * The XML can then be edited by hand and added to the datasets.xml file.
   *
   * @param tLocalSourceUrl with no '?', e.g., https://sdf.ndbc.noaa.gov/sos/server.php
   * @param sosVersion e.g., "1.0.0" (or "" or null)
   * @return a suggested chunk of xml for this dataset for use in datasets.xml
   * @throws Throwable if trouble, e.g., if no Grid or Array variables are found. If no trouble,
   *     then a valid dataset.xml chunk has been returned.
   */
  public static String generateDatasetsXmlFromIOOS(
      boolean useCachedInfo, String tLocalSourceUrl, String sosVersion, String sosServerType)
      throws Throwable {

    String2.log(
        "EDDTableFromSos.generateDatasetsXmlFromIOOS" + "\n  tLocalSourceUrl=" + tLocalSourceUrl);
    String tPbublicSourceUrl = convertToPublicSourceUrl(tLocalSourceUrl);
    sosServerType = sosServerType == null ? "" : sosServerType.trim();
    boolean isIoos52N = sosServerType.toLowerCase().equals(SosServerTypeIoos52N.toLowerCase());

    String tUrl = tLocalSourceUrl + "?service=SOS&request=GetCapabilities";
    if (sosVersion != null && sosVersion.length() > 0) {
      // if (ioosNcbcServer || ioosNosServer) //not needed. 1.0.0 is default and only.
      //    tUrl += "&version=" + sosVersion;
      if (isIoos52N) // AcceptVersions needed. This class only supports 1.0.0 currently.
      tUrl += "&AcceptVersions=" + sosVersion;
    }
    // if (reallyVerbose) String2.log(SSR.getUrlResponseStringUnchanged(tUrl));
    File2.makeDirectory(sosCopyDir);
    String safeFileName = sosCopyDir + String2.md5Hex12(tUrl);
    if (!useCachedInfo || !File2.isFile(safeFileName)) {
      String2.log("downloading GetCapabilities" + "\n  from: " + tUrl + "\n  to: " + safeFileName);
      SSR.downloadFile(tUrl, safeFileName, false); // tryToUseCompression
    } else {
      String2.log(
          "using cached GetCapabilities"
              + "\n  from file: "
              + safeFileName
              + "\n  from URL: "
              + tUrl);
    }

    // gather station info and uniqueObservedProperties
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
    String tSosVersion = null; // e.g., "1.0.0"
    String offeringTag;
    String offeringEndTag;
    String sosPrefix = "";

    SimpleXMLReader xmlReader =
        new SimpleXMLReader(File2.getDecompressedBufferedInputStream(safeFileName));
    try {
      xmlReader.nextTag();
      String tags = xmlReader.allTags();
      boolean ioosServer = false;
      if (xmlReader.tag(0).equals("Capabilities")) {
        sosPrefix = ""; // ioosServer
        if (sosServerType.length() == 0) // not explicitly declared
        ioosServer = true;
      } else if (xmlReader.tag(0).equals("sos:Capabilities")) {
        sosPrefix = "sos:"; // oostethys
      } else {
        String2.log(File2.readFromFile(safeFileName, File2.UTF_8)[1]);
        throw new RuntimeException(
            "The first SOS capabilities tag=\""
                + tags
                + "\" should have been <Capabilities> or <sos:Capabilities>.");
      }
      if (verbose) String2.log("  sosPrefix=" + sosPrefix);
      offeringTag =
          "<"
              + sosPrefix
              + "Capabilities><"
              + sosPrefix
              + "Contents><"
              + sosPrefix
              + "ObservationOfferingList><"
              + sosPrefix
              + "ObservationOffering>";
      offeringEndTag =
          "<"
              + sosPrefix
              + "Capabilities><"
              + sosPrefix
              + "Contents><"
              + sosPrefix
              + "ObservationOfferingList></"
              + sosPrefix
              + "ObservationOffering>";

      do {
        // process the tags
        if (debugMode) String2.log("tags=" + tags + xmlReader.content());

        if (tags.endsWith("<ows:ServiceIdentification></ows:ServiceTypeVersion>")) {
          if (verbose) String2.log("ServiceTypeVersion=" + xmlReader.content());
          if (tSosVersion == null || tSosVersion.length() == 0) {
            tSosVersion = xmlReader.content(); // e.g., "0.0.31" or "1.0.0"
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
          if (!s.isEmpty() && !s.toLowerCase().equals("none")) {
            tLicense = s;
            if (verbose) String2.log("  license(from AccessConstraints)=" + tLicense);
          }

        } else if (tags.endsWith("<ows:ServiceProvider></ows:ProviderName>")) {
          tInstitution = xmlReader.content();
          if (tInstitution.equals("National Data Buoy Center")) tInstitution = "NOAA NDBC";
          else if (tInstitution.equals("National Ocean Service")) tInstitution = "NOAA NOS";
          if (verbose) String2.log("  institution(from ProviderName)=" + tInstitution);

        } else if (tags.endsWith("<ows:ServiceProvider><ows:ProviderSite>")) {
          tInfoUrl = xmlReader.attributeValue("xlink:href");
          if (verbose) String2.log("  infoUrl(from ProviderSite)=" + tInfoUrl);

        } else if (tags.startsWith(offeringTag)) {
          String endOfTag = tags.substring(offeringTag.length());
          String content = xmlReader.content();
          String error = null;
          if (tags.equals(offeringTag)) offeringTagCount++;
          // String2.log("endOfTag=" + endOfTag + xmlReader.content());

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
            // String2.log("startTag");
            tStationID = "";
            tStationObsPropList.setLength(0);

          } else if (endOfTag.equals("</gml:name>")) { // ioosServer, OOSTethys, ioos52N have this
            // e.g., 44004
            tStationID = content;

            if (tStationID.endsWith(":all")) {
              tOfferingAll = tStationID;
            }

            if (ioosServer) {
              // remove  so names are shorter on table below
              String pre = "::";
              int prePo = tStationID.indexOf(pre);
              if (prePo >= 0) tStationID = tStationID.substring(prePo + pre.length());
            }

          } else if (endOfTag.equals("<" + sosPrefix + "observedProperty>")) {
            // handle non-composite observedProperty
            // <sos:observedProperty
            // xlink:href="http://marinemetadata.org/cf#sea_water_temperature"/>
            // NOW http://mmisw.org/ont/cf/parameter/sea_water_salinity
            //    http://marinemetadata.org is GONE!
            String tXlink = xmlReader.attributeValue("xlink:href"); // without quotes
            if (tXlink != null && !tXlink.toLowerCase().equals("none")) {
              if (!isIoos52N) {
                // shorten it
                int po = tXlink.lastIndexOf("#");
                if (po < 0) po = tXlink.lastIndexOf("/");
                if (po < 0) po = tXlink.lastIndexOf(":");
                if (po >= 0) tXlink = tXlink.substring(po + 1);
              }

              // insert in list if not there already
              int opPo = uniqueObsProp.indexOf(tXlink);
              if (opPo < 0) {
                opPo = uniqueObsProp.size();
                uniqueObsProp.add(tXlink);
              }
              while (tStationObsPropList.length() <= opPo) tStationObsPropList.append(' ');
              tStationObsPropList.setCharAt(opPo, (char) (65 + opPo));
            }

          } else if (endOfTag.equals(
              "<" + sosPrefix + "observedProperty><swe:CompositePhenomenon>")) {
            // handle composite observedProperty
            // <swe:CompositePhenomenon gml:id="WEATHER_OBSERVABLES">
            String tXlink = xmlReader.attributeValue("gml:id"); // without quotes
            if (tXlink != null && !tXlink.toLowerCase().equals("none")) {
              int opPo = uniqueObsProp.indexOf(tXlink);
              if (opPo < 0) {
                opPo = uniqueObsProp.size();
                uniqueObsProp.add(tXlink);
              }
              while (tStationObsPropList.length() <= opPo) tStationObsPropList.append(' ');
              tStationObsPropList.setCharAt(opPo, (char) (65 + opPo));
            }
          }

          // handle the error
          if (error != null)
            throw new RuntimeException(
                "Error on SOS GetCapabilities xml line #"
                    + xmlReader.lineNumber()
                    + " stationID="
                    + tStationID
                    + ": "
                    + error);

          // end of a station
        } else if (tags.startsWith(offeringEndTag)) {
          // String2.log("endTag");
          if (tStationID.length() > 0 && tStationObsPropList.length() > 0) {
            stationIDs.add(tStationID);
            stationHasObsProp.add(tStationObsPropList.toString());
          } else {
            String2.log(
                "Invalid Station id="
                    + tStationID
                    + " tStationObsPropList='"
                    + tStationObsPropList
                    + "'");
          }
          tStationID = "";
          tStationObsPropList.setLength(0);
        }

        // get the next tag
        xmlReader.nextTag();
        tags = xmlReader.allTags();
      } while (!tags.startsWith("</"));
    } finally {
      xmlReader.close();
    }
    if (tSosVersion == null || tSosVersion.length() == 0) tSosVersion = "1.0.0"; // default
    if (stationIDs.size() == 0)
      throw new RuntimeException(
          "No stations found! Try a different sosServerType.\n"
              + "  offeringTag="
              + offeringTag
              + "  stationID name tag="
              + "<gml:name>");

    // write the station/obsProp info
    StringBuilder sb = new StringBuilder();
    sb.append(
        "<!-- NOTE! Some of the standard_names below probably aren't CF standard names!\n"
            + "   Check them and delete the ones that aren't CF standard names.\n"
            + "\n"
            + "NOTE! Be wary of suggested dataType=byte. It may just mean there was no\n"
            + "   data for that variable in the data that was sampled.\n"
            + "   Change it to short/int/float/double as needed.\n"
            + "\n");
    int longestStationID = Math.max(7, stationIDs.maxStringLength());
    int longestHasProp = stationHasObsProp.maxStringLength();
    sb.append(
        String2.left("  n  Station", 5 + longestStationID)
            + "  Has observed_property\n"
            + "___  "
            + String2.makeString('_', longestStationID)
            + "  "
            + String2.makeString('_', 60)
            + "\n");
    for (int si = 0; si < stationIDs.size(); si++) {
      sb.append(
          String2.right("" + si, 3)
              + "  "
              + String2.left(stationIDs.get(si), longestStationID)
              + "  "
              + stationHasObsProp.get(si)
              + "\n");
    }

    // list the observed_property
    if (uniqueObsProp.size() == 0)
      throw new RuntimeException(
          "No observedProperties found! Try a different sosServerType.\n"
              + "  offeringTag="
              + offeringTag
              + "\n"
              + "  observedPropertyTag="
              + "<"
              + sosPrefix
              + "observedProperty>");
    sb.append(
        "\n id  observed_property\n" + "___  __________________________________________________\n");
    for (int op = 0; op < uniqueObsProp.size(); op++)
      sb.append(String2.right("" + (char) (65 + op), 3) + "  " + uniqueObsProp.get(op) + "\n");
    sb.append("-->\n");

    // make a dataset for each observed_property
    if (tOfferingAll == null) tOfferingAll = "urn:ioos:network:noaa.nws.ndbc:all";
    double tEpochSeconds = System.currentTimeMillis() / 1000.0;
    tEpochSeconds = Calendar2.backNDays(7, tEpochSeconds);
    String time1 = Calendar2.safeEpochSecondsToIsoStringTZ(tEpochSeconds, "");
    tEpochSeconds += Calendar2.SECONDS_PER_DAY;
    String time2 = Calendar2.safeEpochSecondsToIsoStringTZ(tEpochSeconds, "");

    String pre =
        tLocalSourceUrl
            + "?service=SOS&version="
            + tSosVersion
            + "&request=GetObservation&offering="
            + tOfferingAll
            + "&observedProperty=";
    String post =
        "&responseFormat="
            + SSR.minimalPercentEncode(defaultResponseFormat(sosServerType))
            + "&eventTime="
            + time1
            + "/"
            + time2
            + (isIoos52N ? "" : "&featureofinterest=BBOX:-180,-90,180,90");
    int nUniqueObsProp = uniqueObsProp.size(); // often set to 1 when testing !!!
    for (int op = 0; op < nUniqueObsProp; op++) {
      try {
        sb.append(
            generateDatasetsXmlFromOneIOOS(
                useCachedInfo,
                pre + uniqueObsProp.get(op) + post,
                sosVersion,
                sosServerType,
                tInfoUrl,
                tInstitution,
                tLicense));
      } catch (Throwable t) {
        sb.append(
            "\n<!-- ERROR for "
                + pre
                + uniqueObsProp.get(op)
                + post
                + "\n"
                + MustBe.throwableToString(t)
                + "-->\n\n");
      }
    }

    String2.log("\n\n*** generateDatasetsXmlFromIOOS finished successfully.\n\n");
    return sb.toString();
  }

  /**
   * This generates a readyToUse datasets.xml entry for an IOOS EDDTableFromSOS. <br>
   * The XML can then be edited by hand and added to the datasets.xml file.
   *
   * @param tUrl a URL to get data for responseFormat=text/csv, for a single observedProperty and,
   *     for a BoundingBox e.g., https://sdf.ndbc.noaa.gov/sos/server.php?service=SOS&version=1.0.0
   *     &request=GetObservation&offering=urn:ioos:network:noaa.nws.ndbc:all
   *     &observedProperty=sea_water_salinity
   *     &responseFormat=text/csv&eventTime=2010-05-27T00:00:00Z/2010-05-27T01:00:00Z
   *     &featureofinterest=BBOX:-151.719,17.93,-65.927,60.8
   * @param tInfoUrl the suggested infoUrl (use null or "" if nothing to suggest)
   * @param tInstitution the suggested institution (use null or "" if nothing to suggest)
   * @param tLicense the suggested license (use null or "" for the default)
   * @return a suggested chunk of xml for this dataset for use in datasets.xml
   * @throws Throwable if trouble, e.g., if no Grid or Array variables are found. If no trouble,
   *     then a valid dataset.xml chunk has been returned.
   */
  public static String generateDatasetsXmlFromOneIOOS(
      boolean useCachedInfo,
      String tUrl,
      String sosVersion,
      String sosServerType,
      String tInfoUrl,
      String tInstitution,
      String tLicense)
      throws Throwable {

    sosVersion = sosVersion == null ? "" : sosVersion.trim();
    sosServerType = sosServerType == null ? "" : sosServerType.trim();
    boolean isIoos52N = sosServerType.toLowerCase().equals(SosServerTypeIoos52N.toLowerCase());
    String2.log(
        "\nEDDTableFromSos.generateDatasetsXmlFromOneIOOS isIoos52N="
            + isIoos52N
            + "\n  tUrl="
            + tUrl);

    if (!String2.isSomething(tLicense)) tLicense = "[standard]";
    int po, po1;

    // get the response
    File2.makeDirectory(sosCopyDir);
    String safeFileName = sosCopyDir + String2.md5Hex12(tUrl);
    if (!useCachedInfo || !File2.isFile(safeFileName)) {
      String2.log("downloading GetObservation" + "\n  from: " + tUrl + "\n  to: " + safeFileName);
      SSR.downloadFile(tUrl, safeFileName, false); // tryToUseCompression
    } else {
      String2.log(
          "using cached GetObservation"
              + "\n  from file: "
              + safeFileName
              + "\n  from URL: "
              + tUrl);
    }

    // parse the response into a data table
    Table sosTable;
    String timeSourceName, longitudeSourceName, latitudeSourceName, altitudeSourceName;
    double altitudeMPSU; // <altitudeMetersPerSourceUnit>
    if (isIoos52N) {
      // xml with csv payload
      sosTable = readIoos52NXmlDataTable(File2.getDecompressedBufferedInputStream(safeFileName));
      timeSourceName = "time";
      longitudeSourceName = "longitude";
      latitudeSourceName = "latitude";
      altitudeSourceName = "altitude";
      altitudeMPSU = 1;

    } else {

      // read the file
      BufferedReader br = File2.getDecompressedBufferedFileReader(safeFileName, null);
      /* needs fix to work with BufferedReader
      if (reallyVerbose) {
          String2.log("ASCII response=");
          int stop = Math.min(100, sa.size());
          for (int i = 0; i < stop; i++)
              String2.log(sa.get(i));
          String2.log("...\n");
      }*/

      // is it an xml file (presumably an error report)?
      br.mark(10000); // max read-ahead bytes
      String s = br.readLine();
      if (s.startsWith("<?xml")) {
        StringBuilder sb = new StringBuilder();
        while (s != null) { // initially, s has first line
          sb.append(s);
          sb.append('\n');
          s = br.readLine();
        }
        throw new SimpleException(sb.toString());
      }
      br.reset();

      // read into sosTable
      boolean simplify = true;
      sosTable = new Table();
      sosTable.readASCII(safeFileName, br, "", "", 0, 1, "", null, null, null, null, simplify);
      timeSourceName = "date_time";
      longitudeSourceName = "longitude (degree)";
      latitudeSourceName = "latitude (degree)";
      altitudeSourceName = "depth (m)";
      altitudeMPSU = -1;
    }
    if (reallyVerbose) String2.log("response table=\n" + sosTable.toString(4));

    // tBaseUrl  https://sdf.ndbc.noaa.gov/sos/server.php
    po = tUrl.indexOf('?');
    if (po < 0) throw new SimpleException(String2.ERROR + ": '?' not found in tUrl=" + tUrl);
    String tLocalBaseUrl = tUrl.substring(0, po);
    String tPublicBaseUrl = convertToPublicSourceUrl(tLocalBaseUrl);

    // tBBoxOffering  urn:ioos:network:noaa.nws.ndbc:all
    po = tUrl.indexOf("&offering=");
    if (po < 0)
      throw new SimpleException(String2.ERROR + ": '&offering=' not found in tUrl=" + tUrl);
    po1 = tUrl.indexOf("&", po + 1);
    if (po1 < 0) po1 = tUrl.length();
    String tBBoxOffering = tUrl.substring(po + 10, po1);

    // shortObservedProperty   sea_water_salinity
    // from &observedProperty=http://mmisw.org/ont/cf/parameter/sea_water_salinity
    po = tUrl.indexOf("&observedProperty=");
    if (po < 0)
      throw new SimpleException(
          String2.ERROR + ": '&observedProperty=' not found in sourceUrl=" + tUrl);
    po1 = tUrl.indexOf("&", po + 18);
    if (po1 < 0) po1 = tUrl.length();
    String tObservedProperty = tUrl.substring(po + 18, po1);
    po = tObservedProperty.lastIndexOf('/'); // ok if -1
    String shortObservedProperty = tObservedProperty.substring(po + 1);

    // tInfoUrl  https://sdf.ndbc.noaa.gov/sos/
    if (tInfoUrl == null || tInfoUrl.length() == 0) tInfoUrl = File2.getDirectory(tPublicBaseUrl);

    // tInstitution
    if (tInstitution == null || tInstitution.length() == 0)
      tInstitution = suggestInstitution(tPublicBaseUrl);

    // tDatasetID
    String tDatasetID = suggestDatasetID(tPublicBaseUrl + "?" + shortObservedProperty);

    // remove LLATI columns
    // they are identified below
    po = sosTable.findColumnNumber(longitudeSourceName);
    if (po >= 0) sosTable.removeColumn(po);
    po = sosTable.findColumnNumber(latitudeSourceName);
    if (po >= 0) sosTable.removeColumn(po);
    po = sosTable.findColumnNumber(altitudeSourceName);
    if (po >= 0) sosTable.removeColumn(po);
    po = sosTable.findColumnNumber(timeSourceName);
    if (po >= 0) sosTable.removeColumn(po);
    po = sosTable.findColumnNumber("station_id");
    if (po >= 0) sosTable.removeColumn(po);

    // write the main parts
    StringBuilder sb = new StringBuilder();
    sb.append(
        "<dataset type=\"EDDTableFromSOS\" datasetID=\""
            + tDatasetID
            + "\" active=\"true\">\n"
            + "    <sourceUrl>"
            + XML.encodeAsXML(tLocalBaseUrl)
            + "</sourceUrl>\n"
            + "    <sosVersion>"
            + sosVersion
            + "</sosVersion>\n"
            + "    <sosServerType>"
            + sosServerType
            + "</sosServerType>\n"
            + "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n"
            + "    <observationOfferingIdRegex>.+</observationOfferingIdRegex>\n"
            + "    <requestObservedPropertiesSeparately>true</requestObservedPropertiesSeparately>\n"
            + "    <bboxOffering>"
            + XML.encodeAsXML(tBBoxOffering)
            + "</bboxOffering>\n"
            + "    <bboxParameter>"
            + (isIoos52N ? "BBOX=" : "featureofinterest=BBOX:")
            + "</bboxParameter>\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cdm_data_type\">TimeSeries</att>\n"
            + "        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
            + "        <att name=\"infoUrl\">"
            + XML.encodeAsXML(tInfoUrl)
            + "</att>\n"
            + "        <att name=\"institution\">"
            + XML.encodeAsXML(tInstitution)
            + "</att>\n"
            + "        <att name=\"license\">"
            + XML.encodeAsXML(tLicense)
            + "</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n"
            + "        <att name=\"summary\">This SOS server is part of the IOOS DIF SOS Project.  "
            + "The stations in this dataset have "
            + XML.encodeAsXML(shortObservedProperty)
            + " data.\n"
            + "\n"
            + "Because of the nature of SOS requests, requests for data MUST include constraints for the longitude, latitude, time, and/or station_id variables."
            + (isIoos52N ? slowSummaryWarning : "")
            + "</att>\n"
            + "        <att name=\"title\">"
            + XML.encodeAsXML(tInstitution)
            + " SOS - "
            + XML.encodeAsXML(shortObservedProperty)
            + "</att>\n"
            + "    </addAttributes>\n"
            + "    <longitudeSourceName>"
            + XML.encodeAsXML(longitudeSourceName)
            + "</longitudeSourceName>\n"
            + "    <latitudeSourceName>"
            + XML.encodeAsXML(latitudeSourceName)
            + "</latitudeSourceName>\n"
            + "    <altitudeSourceName>"
            + XML.encodeAsXML(altitudeSourceName)
            + "</altitudeSourceName>\n"
            + "    <altitudeMetersPerSourceUnit>"
            + altitudeMPSU
            + "</altitudeMetersPerSourceUnit>\n"
            + "    <timeSourceName>"
            + XML.encodeAsXML(timeSourceName)
            + "</timeSourceName>\n"
            + "    <timeSourceFormat>yyyy-MM-dd'T'HH:mm:ss"
            + (isIoos52N ? ".SSS" : "")
            + "Z</timeSourceFormat>\n");

    // make addTable with destinationNames and addAttributes
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
          tUnits = Units2.safeUcumToUdunits(colName.substring(po + 2, colName.length() - 1));
        }
      }

      // make addAtts
      Attributes sourceAtts = sosTable.columnAttributes(col);
      Attributes addAtts = new Attributes();
      PAType tPAType = sosTable.getColumn(col).elementType();
      sourceAtts.add("standard_name", colNameNoParen); // add now, remove later
      PrimitiveArray destPA = PrimitiveArray.factory(tPAType, 1, false);
      addAtts =
          makeReadyToUseAddVariableAttributesForDatasetsXml(
              sosTable.globalAttributes(), // but there are none
              sourceAtts,
              null,
              colNameNoParen,
              destPA.elementType() != PAType.STRING, // tryToAddStandardName
              destPA.elementType() != PAType.STRING, // addColorBarMinMax
              true); // tryToFindLLAT
      if (tUnits != null) addAtts.add("units", tUnits);
      sourceAtts.remove("standard_name");
      addAtts.add("standard_name", colNameNoParen);
      addAtts.add("observedProperty", tObservedProperty);

      // add column to addTable
      addTable.addColumn(addTable.nColumns(), colNameNoParen, destPA, addAtts);
    }

    // LLT are known, but this further cleans destNames
    tryToFindLLAT(sosTable, addTable);

    // writeVariablesForDatasetsXml
    // last 2 params: includeDataType, questionDestinationName
    if (reallyVerbose) {
      for (int col = 0; col < nCol; col++)
        String2.log(
            String2.left(sosTable.getColumn(col).elementTypeString(), 10)
                + String2.left(sosTable.getColumnName(col), 15)
                + " units="
                + sosTable.columnAttributes(col).getString("units"));
    }
    sb.append(writeVariablesForDatasetsXml(sosTable, addTable, "dataVariable", true, false));
    sb.append("</dataset>\n\n");

    String2.log("\n\n*** generateDatasetsXmlFromOneIOOS finished successfully.\n\n");
    return sb.toString();
  }

  /**
   * NOT FINISHED. NOT ACTIVE. This parses a phenomenon dictionary and generates a HashMap with
   * key=phenomenonURI, value=StringArray of non-composite phenomena (variables) that it is
   * comprised of.
   *
   * @param url
   * @param hashMap the hashMap to which with phenomena will be added
   * @throws Throwable if trouble
   */
  public static void getPhenomena(String url, HashMap hashMap) throws Throwable {

    String2.log("EDDTableFromSOS.getPhenomena" + "\nurl=" + url);

    SimpleXMLReader xmlReader = new SimpleXMLReader(SSR.getUrlBufferedInputStream(url));
    try {
      xmlReader.nextTag();
      String tags = xmlReader.allTags();
      String sosPrefix = "";
      boolean ioosServer = false;
      String startTag = "<gml:Dictionary>";
      String endTag = "</gml:Dictionary>";

      if (!tags.equals(startTag))
        throw new RuntimeException(
            "The first PhenomenaDictionary tag=" + tags + " should have been " + startTag);

      String codeSpace = null;
      String tID = null;
      StringArray tComponents = null;

      do {
        // process the tags
        // String2.log("tags=" + tags + xmlReader.content());

        String endOfTag = tags.substring(startTag.length());
        String content = xmlReader.content();
        String error = null;
        // String2.log("endOfTag=" + endOfTag + xmlReader.content());

        /* separate phenomena
                        <sos:observedProperty xlink:href="http://marinemetadata.org/cf#sea_water_temperature"/>

        <gml:Dictionary ...>
            <gml:identifier codeSpace="urn:x-noaa:ioos:def:phenomenonNames">PhenomenaDictionary</gml:identifier>
            ...
            */
        if (endOfTag.equals("<gml:identifier>")) {
          codeSpace = xmlReader.attributeValue("codeSpace");
          if (reallyVerbose) String2.log("  codeSpace=" + codeSpace);

          // phenomenon
          /*  <gml:definitionMember >
                  <swe:Phenomenon gml:id="WaterTemperature">
                      <gml:description>Temperature of the water.</gml:description>
                      <gml:identifier codeSpace="urn:x-noaa:ioos:def:phenomenonNames">WaterTemperature</gml:identifier>
                  </swe:Phenomenon>
              </gml:definitionMember>
          */
        } else if (endOfTag.equals("<gml:definitionMember><swe:Phenomenon>")) {
          tID = xmlReader.attributeValue("gml:id");
          if (tID == null) xmlReader.throwException("<swe:Phenomenon> tag has no gml:id.");
          tComponents = new StringArray();
          tComponents.add(codeSpace + "#" + tID);
          hashMap.put(codeSpace + "#" + tID, tComponents);

          // compositePhenomenon
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

        } else if (endOfTag.equals("<gml:definitionMember><swe:CompositePhenomenon><swe:base>")
            || endOfTag.equals("<gml:definitionMember><swe:CompositePhenomenon><swe:component>")) {
          String href = xmlReader.attributeValue("xlink:href");
          if (href == null)
            String2.log(
                "WARNING: on XML line #"
                    + xmlReader.lineNumber()
                    + ": "
                    + endOfTag
                    + " doesn't have an xlink:href.");
          else {
            // get referenced item's components
            href = (href.startsWith("#") ? codeSpace : "") + href;
            StringArray tsa = (StringArray) hashMap.get(href);
            if (tsa == null)
              xmlReader.throwException(
                  href
                      + " isn't already defined in this document "
                      + "(Bob's assumption is that components of composite will be already defined).");
            tComponents.append(tsa);
          }

        } else if (endOfTag.equals("<gml:definitionMember></swe:CompositePhenomenon>")) {
          hashMap.put(codeSpace + "#" + tID, tComponents);
        }

        // get the next tag
        xmlReader.nextTag();
        tags = xmlReader.allTags();
      } while (!tags.startsWith("</gml:Dictionary>"));
    } finally {
      xmlReader.close();
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
}
