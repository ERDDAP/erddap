/*
 * EDDTableFromAsciiService Copyright 2010, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.PAOne;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.handlers.EDDTableFromAsciiServiceHandler;
import gov.noaa.pfel.erddap.handlers.SaxHandlerClass;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;
import java.io.BufferedReader;
import java.util.ArrayList;

// import java.util.GregorianCalendar;

/**
 * This is the abstract superclass for classes which deal with a table of data from a web service
 * that returns ASCII files.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2010-11-12
 */
@SaxHandlerClass(EDDTableFromAsciiServiceHandler.class)
public abstract class EDDTableFromAsciiService extends EDDTable {

  protected String beforeData[];
  protected String afterData = null; // inactive if null or ""
  protected String noData = null; // inactive if null or ""
  protected int responseSubstringStart[] =
      null; // [dv]; a value is Integer.MAX_VALUE if not in addAttributes
  protected int responseSubstringEnd[] =
      null; // [dv]; a value is Integer.MAX_VALUE if not in addAttributes

  /**
   * This constructs an EDDTableFromAsciiServiceNOS based on the information in an .xml file.
   *
   * @param erddap if known in this context, else null
   * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset
   *     type="EDDTableFromAsciiServiceNOS"&gt; having just been read. /
   * @return an EDDTableFromAsciiServiceNOS. When this returns, xmlReader will have just read
   *     &lt;erddapDatasets&gt;&lt;/dataset&gt; .
   * @throws Throwable if trouble
   */
  @EDDFromXmlMethod
  public static EDDTableFromAsciiService fromXml(Erddap erddap, SimpleXMLReader xmlReader)
      throws Throwable {

    // data to be obtained (or not)
    if (verbose) String2.log("\n*** constructing EDDTableFromAsciiService(xmlReader)...");
    String tDatasetID = xmlReader.attributeValue("datasetID");
    String tDatasetType = xmlReader.attributeValue("type");

    Attributes tGlobalAttributes = null;
    ArrayList tDataVariables = new ArrayList();
    int tReloadEveryNMinutes = Integer.MAX_VALUE;
    String tAccessibleTo = null;
    String tGraphsAccessibleTo = null;
    StringArray tOnChange = new StringArray();
    String tFgdcFile = null;
    String tIso19115File = null;
    String tSosOfferingPrefix = null;
    String tLocalSourceUrl = null;

    String tBeforeData[] = new String[11]; // [0 unused, 1..10] correspond to beforeData1..10
    String tAfterData = null;
    String tNoData = null;
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
      else if (localTags.equals("<altitudeMetersPerSourceUnit>"))
        throw new SimpleException(EDVAlt.stopUsingAltitudeMetersPerSourceUnit);
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
      else if (localTags.equals("<beforeData1>")
          || localTags.equals("<beforeData2>")
          || localTags.equals("<beforeData3>")
          || localTags.equals("<beforeData4>")
          || localTags.equals("<beforeData5>")
          || localTags.equals("<beforeData6>")
          || localTags.equals("<beforeData7>")
          || localTags.equals("<beforeData8>")
          || localTags.equals("<beforeData9>")
          || localTags.equals("<beforeData10>")) {
      } else if (localTags.equals("</beforeData1>")
          || localTags.equals("</beforeData2>")
          || localTags.equals("</beforeData3>")
          || localTags.equals("</beforeData4>")
          || localTags.equals("</beforeData5>")
          || localTags.equals("</beforeData6>")
          || localTags.equals("</beforeData7>")
          || localTags.equals("</beforeData8>")
          || localTags.equals("</beforeData9>")
          || localTags.equals("</beforeData10>"))
        tBeforeData[String2.parseInt(localTags.substring(12, localTags.length() - 1))] = content;
      else if (localTags.equals("<afterData>")) {
      } else if (localTags.equals("</afterData>")) tAfterData = content;
      else if (localTags.equals("<noData>")) {
      } else if (localTags.equals("</noData>")) tNoData = content;
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

    if (tDatasetType.equals("EDDTableFromAsciiServiceNOS")) {

      return new EDDTableFromAsciiServiceNOS(
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
          ttDataVariables,
          tReloadEveryNMinutes,
          tLocalSourceUrl,
          tBeforeData,
          tAfterData,
          tNoData);
    } else {
      throw new Exception(
          "type=\""
              + tDatasetType
              + "\" needs to be added to EDDTableFromAsciiService.fromXml at end.");
    }
  }

  /**
   * The constructor.
   *
   * <p>Yes, lots of detailed information must be supplied here that is sometimes available in
   * metadata. If it is in metadata, make a subclass that extracts info from metadata and calls this
   * constructor.
   *
   * @param tDatasetType e.g., EDDTableFromAsciiServiceNOS
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
   * @param tIso19115File This is like tFgdcFile, but for the ISO 19119-2/19139 metadata.
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
   *     be converted to the EDStatic.standardLicense.
   * @param tDataVariables is an Object[nDataVariables][3]: <br>
   *     [0]=String sourceName (the name of the data variable in the dataset source, without the
   *     outer or inner sequence name), <br>
   *     [1]=String destinationName (the name to be presented to the ERDDAP user, or null to use the
   *     sourceName), <br>
   *     [2]=Attributes addAttributes (at ERD, this must have "ioos_category" - a category from
   *     EDV.ioosCategories). Special case: value="null" causes that item to be removed from
   *     combinedAttributes. <br>
   *     [3]=dataType <br>
   *     The order of variables you define doesn't have to match the order in the source.
   *     <p>If there is a time variable, either tAddAttributes (read first) or tSourceAttributes
   *     must have "units" which is either
   *     <ul>
   *       <li>a UDUunits string (containing " since ") describing how to interpret source time
   *           values (which should always be numeric since they are a dimension of a grid) (e.g.,
   *           "seconds since 1970-01-01T00:00:00").
   *       <li>a java.time.format.DateTimeFormatter string (which is compatible with
   *           java.text.SimpleDateFormat) describing how to interpret string times (e.g., the
   *           ISO8601TZ_FORMAT "yyyy-MM-dd'T'HH:mm:ssZ", see
   *           https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/format/DateTimeFormatter.html
   *           or
   *           https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/text/SimpleDateFormat.html)).
   *     </ul>
   *
   * @param tReloadEveryNMinutes indicates how often the source should be checked for new data.
   * @param tLocalSourceUrl the url to which .das or .dds or ... can be added
   * @throws Throwable if trouble
   */
  public EDDTableFromAsciiService(
      String tDatasetType,
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
      Object[][] tDataVariables,
      int tReloadEveryNMinutes,
      String tLocalSourceUrl,
      String tBeforeData[],
      String tAfterData,
      String tNoData)
      throws Throwable {

    if (verbose) String2.log("\n*** constructing " + tDatasetType + ": " + tDatasetID);
    long constructionStartMillis = System.currentTimeMillis();
    String errorInMethod = "Error in " + tDatasetType + "(" + tDatasetID + ") constructor:\n";

    // save some of the parameters
    className = tDatasetType;
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
    localSourceUrl = tLocalSourceUrl;
    setReloadEveryNMinutes(tReloadEveryNMinutes);
    beforeData = tBeforeData == null ? new String[0] : tBeforeData;
    afterData = tAfterData;
    noData = tNoData;

    // in general, AsciiService sets all constraints to PARTIAL and regex to REGEX_OP
    sourceCanConstrainNumericData = CONSTRAIN_PARTIAL;
    sourceCanConstrainStringData = CONSTRAIN_PARTIAL;
    sourceCanConstrainStringRegex =
        PrimitiveArray.REGEX_OP; // standardizeResultsTable always (re)tests regex constraints

    // get global attributes
    combinedGlobalAttributes = new Attributes(addGlobalAttributes);
    String tLicense = combinedGlobalAttributes.getString("license");
    if (tLicense != null)
      combinedGlobalAttributes.set(
          "license", String2.replaceAll(tLicense, "[standard]", EDStatic.standardLicense));
    combinedGlobalAttributes.removeValue("\"null\"");

    // create structures to hold the sourceAttributes temporarily
    int ndv = tDataVariables.length;
    Attributes tDataSourceAttributes[] = new Attributes[ndv];
    String tDataSourceTypes[] = new String[ndv];
    String tDataSourceNames[] = new String[ndv];
    for (int dv = 0; dv < ndv; dv++) {
      tDataSourceNames[dv] = (String) tDataVariables[dv][0];
    }

    // create dataVariables[]
    dataVariables = new EDV[ndv];
    responseSubstringStart = new int[ndv];
    responseSubstringEnd = new int[ndv];
    for (int dv = 0; dv < ndv; dv++) {
      String tSourceName = (String) tDataVariables[dv][0];
      String tDestName = (String) tDataVariables[dv][1];
      if (tDestName == null || tDestName.trim().length() == 0) tDestName = tSourceName;
      Attributes tSourceAtt = new Attributes();
      Attributes tAddAtt = (Attributes) tDataVariables[dv][2];
      String tSourceType = (String) tDataVariables[dv][3];
      // if (reallyVerbose) String2.log("  dv=" + dv + " sourceName=" + tSourceName + " sourceType="
      // + tSourceType);

      // ensure tSourceType was specified
      if (tSourceName.startsWith("=")) {
        // if isFixedValue, sourceType can be inferred
      } else if (tSourceType == null) {
        throw new IllegalArgumentException(
            errorInMethod
                + "<dataType> wasn't specified for dataVariable#"
                + dv
                + "="
                + tSourceName
                + ".");
      }

      // get responseSubstring start and end
      // <att name="responseSubstring">0, 7</att>
      String resSubS = tAddAtt.getString("responseSubstring");
      tAddAtt.remove("responseSubstring");
      String resSub[] = StringArray.arrayFromCSV(resSubS);
      if (resSub.length == 2) {
        int i1 = String2.parseInt(resSub[0]);
        int i2 = String2.parseInt(resSub[1]);
        responseSubstringStart[dv] = i1;
        responseSubstringEnd[dv] = i2;
        if (i1 < 0 || i1 > 100000 || i2 <= i1 || i2 > 100000)
          throw new SimpleException(
              errorInMethod
                  + "For destinationName="
                  + tDestName
                  + ", responseSubstring=\""
                  + resSubS
                  + "\" is invalid.");
      } else if (resSubS != null && resSubS.length() > 0) {
        throw new SimpleException(
            errorInMethod
                + "For destinationName="
                + tDestName
                + ", responseSubstring=\""
                + resSubS
                + "\" is invalid.");
      } else {
        responseSubstringStart[dv] = Integer.MAX_VALUE;
        responseSubstringEnd[dv] = Integer.MAX_VALUE;
      }

      // make the variable
      if (EDV.LON_NAME.equals(tDestName)) {
        dataVariables[dv] =
            new EDVLon(
                datasetID,
                tSourceName,
                tSourceAtt,
                tAddAtt,
                tSourceType,
                PAOne.fromDouble(Double.NaN),
                PAOne.fromDouble(Double.NaN));
        lonIndex = dv;
      } else if (EDV.LAT_NAME.equals(tDestName)) {
        dataVariables[dv] =
            new EDVLat(
                datasetID,
                tSourceName,
                tSourceAtt,
                tAddAtt,
                tSourceType,
                PAOne.fromDouble(Double.NaN),
                PAOne.fromDouble(Double.NaN));
        latIndex = dv;
      } else if (EDV.ALT_NAME.equals(tDestName)) {
        dataVariables[dv] =
            new EDVAlt(
                datasetID,
                tSourceName,
                tSourceAtt,
                tAddAtt,
                tSourceType,
                PAOne.fromDouble(Double.NaN),
                PAOne.fromDouble(Double.NaN));
        altIndex = dv;
      } else if (EDV.DEPTH_NAME.equals(tDestName)) {
        dataVariables[dv] =
            new EDVDepth(
                datasetID,
                tSourceName,
                tSourceAtt,
                tAddAtt,
                tSourceType,
                PAOne.fromDouble(Double.NaN),
                PAOne.fromDouble(Double.NaN));
        depthIndex = dv;
      } else if (EDV.TIME_NAME.equals(
          tDestName)) { // look for TIME_NAME before check hasTimeUnits (next)
        dataVariables[dv] =
            new EDVTime(
                datasetID,
                tSourceName,
                tSourceAtt,
                tAddAtt,
                tSourceType); // this constructor gets source / sets destination actual_range
        timeIndex = dv;
      } else if (EDVTimeStamp.hasTimeUnits(tSourceAtt, tAddAtt)) {
        dataVariables[dv] =
            new EDVTimeStamp(
                datasetID,
                tSourceName,
                tDestName,
                tSourceAtt,
                tAddAtt,
                tSourceType); // this constructor gets source / sets destination actual_range
      } else {
        dataVariables[dv] =
            new EDV(
                datasetID,
                tSourceName,
                tDestName,
                tSourceAtt,
                tAddAtt,
                tSourceType); // the constructor that reads actual_range
        dataVariables[dv].setActualRangeFromDestinationMinMax();
      }
    }
    if (verbose)
      String2.log(
          "responseSubstringStart="
              + String2.toCSSVString(responseSubstringStart)
              + "\n"
              + "responseSubstringEnd  ="
              + String2.toCSSVString(responseSubstringEnd));

    // make addVariablesWhereAttNames and addVariablesWhereAttValues
    makeAddVariablesWhereAttNamesAndValues(tAddVariablesWhere);

    // ensure the setup is valid
    // It will read the /subset/ table or fail trying to get the data if the file isn't present.
    ensureValid();

    // finally
    long cTime = System.currentTimeMillis() - constructionStartMillis;
    if (verbose)
      String2.log(
          (debugMode ? "\n" + toString() : "")
              + "\n*** "
              + tDatasetType
              + " "
              + datasetID
              + " constructor finished. TIME="
              + cTime
              + "ms"
              + (cTime >= 600000 ? "  (>10m!)" : cTime >= 10000 ? "  (>10s!)" : "")
              + "\n");
  }

  /**
   * This gets the data (chunk by chunk) from this EDDTable for the OPeNDAP DAP-style query and
   * writes it to the TableWriter. See the EDDTable method documentation.
   *
   * @param language the index of the selected language
   * @param loggedInAs the user's login name if logged in (or null if not logged in).
   * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
   * @param userDapQuery the part of the user's request after the '?', still percentEncoded, may be
   *     null.
   * @param tableWriter
   * @throws Throwable if trouble (notably, WaitThenTryAgainException)
   */
  @Override
  public abstract void getDataForDapQuery(
      int language,
      String loggedInAs,
      String requestUrl,
      String userDapQuery,
      TableWriter tableWriter)
      throws Throwable;

  /**
   * If your subclass uses the standard system of beforeData, afterData, and noData, you can use
   * this to get the table for the encodedSourceUrl. Custom getDataForDapQuery often use only slight
   * variants of this.
   *
   * @return a table where some of the PrimitiveArrays have data, some don't
   */
  public Table getTable(String encodedSourceUrl) throws Throwable {
    BufferedReader in = SSR.getBufferedUrlReader(encodedSourceUrl);
    try {
      String s = in.readLine();
      s = findBeforeData(in, s);
      return getTable(in, s);
    } finally {
      in.close();
    }
  }

  /**
   * This finds the beforeData strings (if any are defined)
   *
   * @param in
   * @param s is the next line to be processed
   * @return the next line to be processed
   * @throws Throwable if trouble, e.g., a beforeData string wasn't found
   */
  protected String findBeforeData(BufferedReader in, String s) throws Throwable {

    // look for beforeData
    if (debugMode) String2.log(">>findBeforeData\nold line=" + s);
    for (int bd = 1; bd < beforeData.length; bd++) {
      String tBeforeData = beforeData[bd];
      if (tBeforeData != null && tBeforeData.length() > 0) {
        s = find(in, s, tBeforeData, "beforeData" + bd + "=\"" + tBeforeData + "\" wasn't found");
      }
    }

    return s;
  }

  /**
   * This finds a string.
   *
   * @param in
   * @param s is the next line to be processed (right after 'find', even if at end of line)
   * @param find the string to be found
   * @param error the error to be thrown if 'find' isn't found
   * @return the next line to be processed
   * @throws Throwable if trouble, e.g., 'find' wasn't found
   */
  protected String find(BufferedReader in, String s, String find, String error) throws Throwable {

    if (debugMode) String2.log(">>find=" + find + "\nold line=" + s);
    while (s != null) {
      if (s.indexOf(noData) >= 0) {
        if (debugMode) String2.log(">>found noData=\"" + noData + "\"");
        throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (in response from source)");
      }
      int po = s.indexOf(find);
      if (po >= 0) {
        // success
        if (debugMode) String2.log(">>found=" + find);
        return s.substring(po + find.length());
      } else {
        s = in.readLine(); // read the next line
        if (debugMode) String2.log(">>new line=" + s);
      }
    }
    throw new SimpleException("Unexpected response from data source (" + error + ")");
  }

  /**
   * This assumes that the data is ready to be read, starting with s. This doesn't call
   * standardizeResultsTable().
   *
   * @param in Afterwards, this doesn't call in.close().
   * @param s This is a partial line (already read from 'in', with the first line of data) or an
   *     empty line (where the next line has the first line of data).
   * @return a table where some of the PrimitiveArrays have data, some don't
   * @throws throwable if trouble
   */
  protected Table getTable(BufferedReader in, String s) throws Throwable {

    if (s != null && s.length() == 0) s = in.readLine();
    if (s == null)
      throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (first data line is null)");

    // make the empty table with all of the columns (even fixedValue)
    Table table = makeEmptySourceTable(dataVariables, 32);
    int ndv = table.nColumns();
    PrimitiveArray pa[] = new PrimitiveArray[ndv];
    for (int dv = 0; dv < ndv; dv++) pa[dv] = table.getColumn(dv);

    // read the data lines
    boolean stopAfterThisLine = false;
    while (s != null) {
      if (s.indexOf(noData) >= 0)
        throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (says source)");
      int po = s.indexOf(afterData);
      if (po == 0) {
        // stop now
        // if (reallyVerbose) String2.log("  found afterData=\"" + afterData + "\" at the beginning
        // of a line.");
        break;
      } else if (po > 0) {
        // process this line then stop;
        // if (reallyVerbose) String2.log("  found afterData=\"" + afterData + "\" after the
        // beginning of a line.");
        s = s.substring(0, po);
        stopAfterThisLine = true;
      }

      // process this line's data
      int sLength = s.length();
      for (int dv = 0; dv < ndv; dv++) {
        int tStart = responseSubstringStart[dv];
        if (tStart == Integer.MAX_VALUE) {
          // data is supplied some other way
          // pa[dv].addString("");
        } else {
          // grab the responseSubstring  (be lenient)
          int tEnd = Math.min(sLength, responseSubstringEnd[dv]);
          // String2.log("dv=" + dv + " start=" + tStart + " end=" + tEnd + " " + s);
          pa[dv].addString(tEnd <= tStart ? "" : s.substring(tStart, tEnd).trim());
        }
      }

      // read the next line
      if (stopAfterThisLine) break;
      s = in.readLine();
      // String2.log(s);
    }

    // String2.log(table.dataToString());
    return table;
  }
}
