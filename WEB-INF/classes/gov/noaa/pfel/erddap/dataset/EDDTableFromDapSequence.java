/*
 * EDDTableFromDapSequence Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.PAOne;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.XML;
import dods.dap.*;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.handlers.EDDTableFromDapSequenceHandler;
import gov.noaa.pfel.erddap.handlers.SaxHandlerClass;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * This class represents a table of data from an opendap sequence source.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-06-08
 */
@SaxHandlerClass(EDDTableFromDapSequenceHandler.class)
public class EDDTableFromDapSequence extends EDDTable {

  protected String outerSequenceName, innerSequenceName;
  protected boolean sourceCanConstrainStringEQNE,
      sourceCanConstrainStringGTLT,
      skipDapperSpacerRows;
  protected boolean isOuterVar[];

  /**
   * Indicates if data can be transmitted in a compressed form. It is unlikely anyone would want to
   * change this.
   */
  public static boolean acceptDeflate = true;

  /**
   * This constructs an EDDTableFromDapSequence based on the information in an .xml file.
   *
   * @param erddap if known in this context, else null
   * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableFromDapSequence"&gt;
   *     having just been read.
   * @return an EDDTableFromDapSequence. When this returns, xmlReader will have just read
   *     &lt;erddapDatasets&gt;&lt;/dataset&gt; .
   * @throws Throwable if trouble
   */
  @EDDFromXmlMethod
  public static EDDTableFromDapSequence fromXml(Erddap erddap, SimpleXMLReader xmlReader)
      throws Throwable {

    // data to be obtained (or not)
    if (verbose) String2.log("\n*** constructing EDDTableFromDapSequence(xmlReader)...");
    String tDatasetID = xmlReader.attributeValue("datasetID");
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
    String tOuterSequenceName = null;
    String tInnerSequenceName = null;
    boolean tSourceNeedsExpandedFP_EQ = true;
    boolean tSourceCanConstrainStringEQNE = true;
    boolean tSourceCanConstrainStringGTLT = true;
    String tSourceCanConstrainStringRegex = null;
    boolean tSkipDapperSpacerRows = false;
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
      else if (localTags.equals("<outerSequenceName>")) {
      } else if (localTags.equals("</outerSequenceName>")) tOuterSequenceName = content;
      else if (localTags.equals("<innerSequenceName>")) {
      } else if (localTags.equals("</innerSequenceName>")) tInnerSequenceName = content;
      else if (localTags.equals("<sourceNeedsExpandedFP_EQ>")) {
      } else if (localTags.equals("</sourceNeedsExpandedFP_EQ>"))
        tSourceNeedsExpandedFP_EQ = String2.parseBoolean(content);
      else if (localTags.equals("<sourceCanConstrainStringEQNE>")) {
      } else if (localTags.equals("</sourceCanConstrainStringEQNE>"))
        tSourceCanConstrainStringEQNE = String2.parseBoolean(content);
      else if (localTags.equals("<sourceCanConstrainStringGTLT>")) {
      } else if (localTags.equals("</sourceCanConstrainStringGTLT>"))
        tSourceCanConstrainStringGTLT = String2.parseBoolean(content);
      else if (localTags.equals("<sourceCanConstrainStringRegex>")) {
      } else if (localTags.equals("</sourceCanConstrainStringRegex>"))
        tSourceCanConstrainStringRegex = content;
      else if (localTags.equals("<skipDapperSpacerRows>")) {
      } else if (localTags.equals("</skipDapperSpacerRows>"))
        tSkipDapperSpacerRows = String2.parseBoolean(content);
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

    return new EDDTableFromDapSequence(
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
        tOuterSequenceName,
        tInnerSequenceName,
        tSourceNeedsExpandedFP_EQ,
        tSourceCanConstrainStringEQNE,
        tSourceCanConstrainStringGTLT,
        tSourceCanConstrainStringRegex,
        tSkipDapperSpacerRows);
  }

  /**
   * The constructor.
   *
   * <p>Assumptions about what constraints the source can handle: <br>
   * Numeric variables: outer sequence (any operator except regex), inner sequence (no constraints).
   * <br>
   * String variables: <br>
   * outer sequence <br>
   * support for = and != is set by sourceCanConstrainStringEQNE (default=true) <br>
   * support for &lt; &lt;= &gt; &gt;= is set by sourceCanConstrainStringGTLT (default=true), <br>
   * regex support set by sourceCanConstrainStringRegex (default ""), <br>
   * inner sequence (no constraints ever).
   *
   * <p>Yes, lots of detailed information must be supplied here that is sometimes available in
   * metadata. If it is in metadata, make a subclass that extracts info from metadata and calls this
   * constructor.
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
   *     Special case: if combinedGlobalAttributes name="license", any instance of
   *     value="[standard]" will be converted to the EDStatic.standardLicense.
   * @param tDataVariables is an Object[nDataVariables][3]: <br>
   *     [0]=String sourceName (the name of the data variable in the dataset source, without the
   *     outer or inner sequence name), <br>
   *     [1]=String destinationName (the name to be presented to the ERDDAP user, or null to use the
   *     sourceName), <br>
   *     [2]=Attributes addAttributes (at ERD, this must have "ioos_category" - a category from
   *     EDV.ioosCategories). Special case: value="null" causes that item to be removed from
   *     combinedAttributes. <br>
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
   * @param tOuterSequenceName
   * @param tInnerSequenceName or null or "" if only 1-level sequence
   * @param tSourceNeedsExpandedFP_EQ
   * @param tSourceCanConstrainStringEQNE if true, the source accepts constraints on outer sequence
   *     String variables with the = and != operators.
   * @param tSourceCanConstrainStringGTLT if true, the source accepts constraints on outer sequence
   *     String variables with the &lt;, &lt;=, &gt;, and &gt;= operators.
   * @param tSourceCanConstrainStringRegex "=~" (the standard), "~=" (mistakenly supported by some
   *     servers), or null or "" (indicates not supported).
   * @param tSkipDapperSpacerRows if true, this skips the last row of each innerSequence other than
   *     the last innerSequence (because Dapper puts NaNs in the row to act as a spacer).
   * @throws Throwable if trouble
   */
  public EDDTableFromDapSequence(
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
      String tOuterSequenceName,
      String tInnerSequenceName,
      boolean tSourceNeedsExpandedFP_EQ,
      boolean tSourceCanConstrainStringEQNE,
      boolean tSourceCanConstrainStringGTLT,
      String tSourceCanConstrainStringRegex,
      boolean tSkipDapperSpacerRows)
      throws Throwable {

    if (verbose) String2.log("\n*** constructing EDDTableFromDapSequence " + tDatasetID);
    long constructionStartMillis = System.currentTimeMillis();
    String errorInMethod = "Error in EDDTableFromDapSequence(" + tDatasetID + ") constructor:\n";

    // save some of the parameters
    className = "EDDTableFromDapSequence";
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
    outerSequenceName = tOuterSequenceName;
    innerSequenceName =
        tInnerSequenceName == null || tInnerSequenceName.length() == 0 ? null : tInnerSequenceName;
    skipDapperSpacerRows = tSkipDapperSpacerRows;
    sourceNeedsExpandedFP_EQ = tSourceNeedsExpandedFP_EQ;
    sourceCanConstrainStringEQNE = tSourceCanConstrainStringEQNE;
    sourceCanConstrainStringGTLT = tSourceCanConstrainStringGTLT;

    // in general, opendap sequence support:
    sourceCanConstrainNumericData = CONSTRAIN_PARTIAL; // outer vars, yes; inner, no
    sourceCanConstrainStringData = CONSTRAIN_PARTIAL; // outer vars, varies; inner, no
    sourceCanConstrainStringRegex =
        tSourceCanConstrainStringRegex == null
            ? ""
            : tSourceCanConstrainStringRegex; // but only outer vars

    // Design decision: this doesn't use e.g., ucar.nc2.dt.StationDataSet
    //  because it determines axes via _CoordinateAxisType (or similar) metadata
    //  which most datasets we use don't have yet.
    //  One could certainly write another class that did use ucar.nc2.dt.StationDataSet.

    // quickRestart
    Attributes quickRestartAttributes = null;
    if (EDStatic.quickRestart
        && EDStatic.initialLoadDatasets()
        && File2.isFile(quickRestartFullFileName())) {
      // try to do quick initialLoadDatasets()
      // If this fails anytime during construction, the dataset will be loaded
      //  during the next major loadDatasets,
      //  which is good because it allows quick loading of other datasets to continue.
      // This will fail (good) if dataset has changed significantly and
      //  quickRestart file has outdated information.
      quickRestartAttributes = NcHelper.readAttributesFromNc3(quickRestartFullFileName());

      if (verbose) String2.log("  using info from quickRestartFile");

      // set creationTimeMillis to time of previous creation, so next time
      // to be reloaded will be same as if ERDDAP hadn't been restarted.
      creationTimeMillis = quickRestartAttributes.getLong("creationTimeMillis");
    }

    // DAS
    byte dasBytes[] =
        quickRestartAttributes == null
            ? SSR.getUrlResponseBytes(localSourceUrl + ".das")
            : // has timeout and descriptive error
            ((ByteArray) quickRestartAttributes.get("dasBytes")).toArray();
    DAS das = new DAS();
    das.parse(new ByteArrayInputStream(dasBytes));

    // DDS
    byte ddsBytes[] =
        quickRestartAttributes == null
            ? SSR.getUrlResponseBytes(localSourceUrl + ".dds")
            : // has timeout and descriptive error
            ((ByteArray) quickRestartAttributes.get("ddsBytes")).toArray();
    DDS dds = new DDS();
    dds.parse(new ByteArrayInputStream(ddsBytes));

    // get global attributes
    sourceGlobalAttributes = new Attributes();
    OpendapHelper.getAttributes(das, "GLOBAL", sourceGlobalAttributes);
    combinedGlobalAttributes =
        new Attributes(addGlobalAttributes, sourceGlobalAttributes); // order is important
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
    isOuterVar = new boolean[ndv]; // default is all false
    for (int dv = 0; dv < ndv; dv++) {
      tDataSourceNames[dv] = (String) tDataVariables[dv][0];
    }

    // delve into the outerSequence
    BaseType outerVariable = (BaseType) dds.getVariable(outerSequenceName);
    if (!(outerVariable instanceof DSequence))
      throw new RuntimeException(
          errorInMethod
              + "outerVariable not a DSequence: name="
              + outerVariable.getName()
              + " type="
              + outerVariable.getTypeName());
    DSequence outerSequence = (DSequence) outerVariable;
    int nOuterColumns = outerSequence.elementCount();
    AttributeTable outerAttributeTable = das.getAttributeTable(outerSequenceName);
    for (int outerCol = 0; outerCol < nOuterColumns; outerCol++) {

      // look at the variables in the outer sequence
      BaseType obt = (BaseType) outerSequence.getVar(outerCol);
      String oName = obt.getName();
      if (innerSequenceName != null && oName.equals(innerSequenceName)) {

        // look at the variables in the inner sequence
        DSequence innerSequence = (DSequence) obt;
        AttributeTable innerAttributeTable = das.getAttributeTable(innerSequence.getName());
        Enumeration ien = innerSequence.getVariables();
        while (ien.hasMoreElements()) {
          BaseType ibt = (BaseType) ien.nextElement();
          String iName = ibt.getName();

          // is iName in tDataVariableNames?  i.e., are we interested in this variable?
          int dv = String2.indexOf(tDataSourceNames, iName);
          if (dv < 0) {
            if (reallyVerbose) String2.log("  ignoring source iName=" + iName);
            continue;
          }

          // get the sourceType
          tDataSourceTypes[dv] =
              PAType.toCohortString(OpendapHelper.getElementPAType(ibt.newPrimitiveVector()));

          // get the ibt attributes
          // (some servers return innerAttributeTable, some don't -- see test cases)
          Attributes tAtt = new Attributes();
          if (innerAttributeTable == null) {
            // Dapper needs this approach
            // note use of getLongName here
            OpendapHelper.getAttributes(das, ibt.getLongName(), tAtt);
            // drds needs this approach
            if (tAtt.size() == 0) OpendapHelper.getAttributes(das, iName, tAtt);
          } else {
            // note use of getName in this section
            // if (reallyVerbose) String2.log("try getting attributes for inner " + iName);
            dods.dap.Attribute attribute = innerAttributeTable.getAttribute(iName);
            // it should be a container with the attributes for this column
            if (attribute == null) {
              String2.log("WARNING!!! Unexpected: no attribute for innerVar=" + iName + ".");
            } else if (attribute.isContainer()) {
              OpendapHelper.getAttributes(attribute.getContainer(), tAtt);
            } else {
              String2.log(
                  "WARNING!!! Unexpected: attribute for innerVar="
                      + iName
                      + " not a container: "
                      + attribute.getName()
                      + "="
                      + attribute.getValueAt(0));
            }
          }
          // tAtt.set("source_sequence", "inner");
          tDataSourceAttributes[dv] = tAtt; // may be empty, that's ok
        } // inner elements loop

      } else {
        // deal with an outer column
        // is oName in tDataVariableNames?  i.e., are we interested in this variable?
        int dv = String2.indexOf(tDataSourceNames, oName);
        if (dv < 0) {
          // for testing only:  throw new RuntimeException("  ignoring source oName=" + oName);
          if (verbose) String2.log("  ignoring source outer variable name=" + oName);
          continue;
        }
        isOuterVar[dv] = true;

        // get the sourceDataType
        tDataSourceTypes[dv] =
            PAType.toCohortString(OpendapHelper.getElementPAType(obt.newPrimitiveVector()));

        // get the attributes
        Attributes tAtt = new Attributes();
        if (outerAttributeTable == null) {
          // Dapper needs this approach
          // note use of getLongName here
          OpendapHelper.getAttributes(das, obt.getLongName(), tAtt);
          // drds needs this approach
          if (tAtt.size() == 0) OpendapHelper.getAttributes(das, oName, tAtt);
        } else {
          // note use of getName in this section
          // if (reallyVerbose) String2.log("try getting attributes for outer " + oName);
          dods.dap.Attribute attribute = outerAttributeTable.getAttribute(oName);
          // it should be a container with the attributes for this column
          if (attribute == null) {
            String2.log("WARNING!!! Unexpected: no attribute for outerVar=" + oName + ".");
          } else if (attribute.isContainer()) {
            OpendapHelper.getAttributes(attribute.getContainer(), tAtt);
          } else {
            String2.log(
                "WARNING!!! Unexpected: attribute for outerVar="
                    + oName
                    + " not a container: "
                    + attribute.getName()
                    + "="
                    + attribute.getValueAt(0));
          }
        }
        // tAtt.set("source_sequence", "outer"); //just mark inner
        tDataSourceAttributes[dv] = tAtt;
      }
    }

    // create dataVariables[]
    dataVariables = new EDV[ndv];
    for (int dv = 0; dv < ndv; dv++) {
      String tSourceName = (String) tDataVariables[dv][0];
      String tDestName = (String) tDataVariables[dv][1];
      if (tDestName == null || tDestName.trim().length() == 0) tDestName = tSourceName;
      Attributes tSourceAtt = tDataSourceAttributes[dv];
      Attributes tAddAtt = (Attributes) tDataVariables[dv][2];
      String tSourceType = tDataSourceTypes[dv];
      // if (reallyVerbose) String2.log("  dv=" + dv + " sourceName=" + tSourceName + " sourceType="
      // + tSourceType);

      // if _Unsigned=true or false, change tSourceType
      if (tSourceAtt == null) tSourceAtt = new Attributes();
      if (tAddAtt == null) tAddAtt = new Attributes();
      tSourceType = Attributes.adjustSourceType(tSourceType, tSourceAtt, tAddAtt);

      // ensure the variable was found
      if (tSourceName.startsWith("=")) {
        // if isFixedValue, sourceType can be inferred
      } else if (tSourceType == null) {
        throw new IllegalArgumentException(
            errorInMethod
                + "dataVariable#"
                + dv
                + " name="
                + tSourceName
                + " not found in data source.");
      }

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

    // make addVariablesWhereAttNames and addVariablesWhereAttValues
    makeAddVariablesWhereAttNamesAndValues(tAddVariablesWhere);

    // ensure the setup is valid
    ensureValid();

    // save quickRestart info
    if (quickRestartAttributes == null) { // i.e., there is new info
      try {
        quickRestartAttributes = new Attributes();
        quickRestartAttributes.set("creationTimeMillis", "" + creationTimeMillis);
        quickRestartAttributes.set("dasBytes", new ByteArray(dasBytes));
        quickRestartAttributes.set("ddsBytes", new ByteArray(ddsBytes));
        File2.makeDirectory(File2.getDirectory(quickRestartFullFileName()));
        NcHelper.writeAttributesToNc3(quickRestartFullFileName(), quickRestartAttributes);
      } catch (Throwable t) {
        String2.log(MustBe.throwableToString(t));
      }
    }

    // finally
    long cTime = System.currentTimeMillis() - constructionStartMillis;
    if (verbose)
      String2.log(
          (debugMode ? "\n" + toString() : "")
              + "\n*** EDDTableFromDapSequence "
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
  } // because this gets info from a remote service

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
  public void getDataForDapQuery(
      int language,
      String loggedInAs,
      String requestUrl,
      String userDapQuery,
      TableWriter tableWriter)
      throws Throwable {

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

    // further prune constraints
    // sourceCanConstrainNumericData = CONSTRAIN_PARTIAL; //outer vars, yes; inner, no
    // sourceCanConstrainStringData  = CONSTRAIN_PARTIAL; //outer vars, varies; inner, no
    // sourceCanConstrainStringRegex = dataset dependent, but only outer vars
    // work backwards since deleting some
    for (int c = constraintVariables.size() - 1; c >= 0; c--) {
      String constraintVariable = constraintVariables.get(c);
      int dv = String2.indexOf(dataVariableSourceNames(), constraintVariable);
      if (!isOuterVar[dv]) {
        // just remove constraints for all inner vars (string and numeric)
        constraintVariables.remove(c);
        constraintOps.remove(c);
        constraintValues.remove(c);
        continue;
      }

      // for string constraints
      EDV edv = dataVariables[dv];
      String op = constraintOps.get(c);
      if (edv.sourceDataPAType() == PAType.STRING) {

        // remove EQNE constraints
        if (!sourceCanConstrainStringEQNE && (op.equals("=") || op.equals("!="))) {
          constraintVariables.remove(c);
          constraintOps.remove(c);
          constraintValues.remove(c);
          continue;
        }

        // remove GTLT constraints
        if (!sourceCanConstrainStringGTLT && String2.indexOf(GTLT_OPERATORS, op) >= 0) {
          constraintVariables.remove(c);
          constraintOps.remove(c);
          constraintValues.remove(c);
          continue;
        }

        // convert time constraints (epochSeconds) to source String format
        if ((edv instanceof EDVTimeStamp ets)
            && !op.equals(PrimitiveArray.REGEX_OP)) { // but if regex, leave as string
          constraintValues.set(
              c, ets.epochSecondsToSourceTimeString(String2.parseDouble(constraintValues.get(c))));
        }

        // finally: put quotes around String constraint "value"
        constraintValues.set(c, "\"" + constraintValues.get(c) + "\"");

      } else {
        // convert time constraints (epochSeconds) to source units
        if ((edv instanceof EDVTimeStamp ets)
            && !op.equals(PrimitiveArray.REGEX_OP)) { // but if regex, leave as string
          constraintValues.set(
              c, ets.epochSecondsToSourceTimeString(String2.parseDouble(constraintValues.get(c))));
        }
      }

      // convert REGEX_OP to sourceCanConstrainStringRegex
      if (op.equals(PrimitiveArray.REGEX_OP)) {
        constraintOps.set(c, sourceCanConstrainStringRegex);
      }
    }

    // It is very easy for this class since the sourceDapQuery can be
    // sent directly to the source after minimal processing.
    String encodedSourceDapQuery =
        formatAsPercentEncodedDapQuery(
            resultsVariables.toArray(),
            constraintVariables.toArray(),
            constraintOps.toArray(),
            constraintValues.toArray());

    // Read all data, then write to tableWriter.
    // Very unfortunate: This is NOT memory efficient.
    // JDAP reads all rows when it deserializes (see java docs for DSequence)
    // (that's why it can return getRowCount)
    // so there is no easy way to read an opendapSequence in chunks (or row by row).
    // I can't split into subsets because I don't know which variable
    //  to constrain or how to constrain it (it would change with different
    //  userDapQuery's).
    // I could write my own procedure to read DSequence (eek!).
    Table table = new Table();
    try {
      table.readOpendapSequence(localSourceUrl + "?" + encodedSourceDapQuery, skipDapperSpacerRows);
    } catch (Throwable t) {
      EDStatic.rethrowClientAbortException(t); // first thing in catch{}

      // if no data, convert to ERDDAP standard message
      String tToString = t.toString();
      if (tToString.indexOf("Your Query Produced No Matching Results.") >= 0) // the DAP standard
      throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (says DAP)", t);

      // if OutOfMemoryError or too much data, rethrow t
      if (Thread.currentThread().isInterrupted()
          || t instanceof InterruptedException
          || t instanceof OutOfMemoryError
          || tToString.indexOf(Math2.memoryTooMuchData) >= 0
          || tToString.indexOf(Math2.TooManyOpenFiles) >= 0) throw t;

      // any other error is real trouble
      String2.log(MustBe.throwableToString(t));
      throw t instanceof WaitThenTryAgainException
          ? t
          : new WaitThenTryAgainException(
              EDStatic.simpleBilingual(language, EDStatic.waitThenTryAgainAr)
                  + "\n("
                  + EDStatic.errorFromDataSource
                  + tToString
                  + ")",
              t);
    }
    // String2.log(table.toString());
    standardizeResultsTable(language, requestUrl, userDapQuery, table);
    tableWriter.writeAllAndFinish(table);
  }

  /**
   * This does its best to generate a read-to-use datasets.xml entry for an EDDTableFromDapSequence.
   * <br>
   * The XML can then be edited by hand and added to the datasets.xml file. <br>
   * This uses the first outerSequence (and if present, first innerSequence) found. <br>
   * Other sequences are skipped.
   *
   * @param tLocalSourceUrl
   * @param tReloadEveryNMinutes must be a valid value, e.g., 1440 for once per day. Use, e.g.,
   *     1000000000, for never reload.
   * @param externalGlobalAttributes globalAttributes gleaned from external sources, e.g., a THREDDS
   *     catalog.xml file. These have priority over other sourceGlobalAttributes. Okay to use null
   *     if none.
   * @return a suggested chunk of xml for this dataset for use in datasets.xml
   * @throws Throwable if trouble, e.g., if no Grid or Array variables are found. If no trouble,
   *     then a valid dataset.xml chunk has been returned.
   */
  public static String generateDatasetsXml(
      String tLocalSourceUrl, int tReloadEveryNMinutes, Attributes externalGlobalAttributes)
      // String outerSequenceName, String innerSequenceName, boolean sortColumnsByName)
      throws Throwable {

    tLocalSourceUrl = EDStatic.updateUrls(tLocalSourceUrl); // http: to https:
    String2.log(
        "EDDTableFromDapSequence.generateDatasetsXml"
            + "\ntLocalSourceUrl="
            + tLocalSourceUrl
            + "\nreloadEveryNMinutes="
            + tReloadEveryNMinutes
            + "\nexternalGlobalAttributes="
            + externalGlobalAttributes);
    String tPublicSourceUrl = convertToPublicSourceUrl(tLocalSourceUrl);

    // get DConnect
    if (tLocalSourceUrl.endsWith(".html"))
      tLocalSourceUrl = tLocalSourceUrl.substring(0, tLocalSourceUrl.length() - 5);
    DConnect dConnect = new DConnect(tLocalSourceUrl, acceptDeflate, 1, 1);
    DAS das = null;
    try {
      das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
    } catch (Throwable t) {
      throw new RuntimeException("Error while getting DAS from " + tLocalSourceUrl + ".das .", t);
    }
    // String2.log("das.getNames=" + String2.toCSSVString(das.getNames()));
    // AttributeTable att = OpendapHelper.getAttributeTable(das, outerSequenceName);
    // Attributes atts2 = new Attributes();
    // OpendapHelper.getAttributes(att, atts2);
    // String2.log("outer attributes=" + (att == null? "null" : atts2.toString()));

    DDS dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);

    // *** basically, make a table to hold the sourceAttributes
    // and a parallel table to hold the addAttributes
    Table dataSourceTable = new Table();
    Table dataAddTable = new Table();

    // get source global attributes
    OpendapHelper.getAttributes(das, "GLOBAL", dataSourceTable.globalAttributes());

    // get all of the vars
    String outerSequenceName = null;
    String innerSequenceName = null;
    Enumeration datasetVars = dds.getVariables();
    int nOuterVars = 0; // so outerVars are first in dataAddTable
    Attributes gridMappingAtts = null;
    while (datasetVars.hasMoreElements()) {
      BaseType datasetVar = (BaseType) datasetVars.nextElement();

      // is this the pseudo-data grid_mapping variable?
      if (gridMappingAtts == null) {
        Attributes tSourceAtts = new Attributes();
        OpendapHelper.getAttributes(das, datasetVar.getName(), tSourceAtts);
        gridMappingAtts = NcHelper.getGridMappingAtts(tSourceAtts);
      }

      if (outerSequenceName == null && datasetVar instanceof DSequence outerSequence) {
        outerSequenceName = outerSequence.getName();

        // get list of outerSequence variables
        Enumeration outerVars = outerSequence.getVariables();
        while (outerVars.hasMoreElements()) {
          BaseType outerVar = (BaseType) outerVars.nextElement();

          // catch innerSequence
          if (outerVar instanceof DSequence innerSequence) {
            if (innerSequenceName == null) {
              innerSequenceName = outerVar.getName();
              Enumeration innerVars = innerSequence.getVariables();
              while (innerVars.hasMoreElements()) {
                // inner variable
                BaseType innerVar = (BaseType) innerVars.nextElement();
                if (innerVar instanceof DConstructor || innerVar instanceof DVector) {
                } else {
                  String varName = innerVar.getName();
                  Attributes sourceAtts = new Attributes();
                  OpendapHelper.getAttributes(das, varName, sourceAtts);
                  if (sourceAtts.size() == 0)
                    OpendapHelper.getAttributes(
                        das,
                        outerSequenceName + "." + innerSequenceName + "." + varName,
                        sourceAtts);

                  PrimitiveArray sourcePA =
                      PrimitiveArray.factory(OpendapHelper.getElementPAType(innerVar), 1, false);
                  if ("true".equals(sourceAtts.getString("_Unsigned")))
                    sourcePA = sourcePA.makeUnsignedPA();
                  PrimitiveArray destPA =
                      (PrimitiveArray)
                          sourcePA
                              .clone(); // !This doesn't handle change in type from scale_factor,
                  // add_offset
                  dataSourceTable.addColumn(
                      dataSourceTable.nColumns(), varName, sourcePA, sourceAtts);
                  dataAddTable.addColumn(
                      dataAddTable.nColumns(),
                      varName,
                      destPA,
                      makeReadyToUseAddVariableAttributesForDatasetsXml(
                          dataSourceTable.globalAttributes(),
                          sourceAtts,
                          null,
                          varName,
                          destPA.elementType() != PAType.STRING, // tryToAddStandardName
                          destPA.elementType() != PAType.STRING, // addColorBarMinMax
                          true)); // tryToFindLLAT
                }
              }
            } else {
              if (verbose) String2.log("Skipping the other innerSequence: " + outerVar.getName());
            }
          } else if (outerVar instanceof DConstructor) {
            // skip it
          } else {
            // outer variable
            String varName = outerVar.getName();
            Attributes sourceAtts = new Attributes();
            OpendapHelper.getAttributes(das, varName, sourceAtts);
            if (sourceAtts.size() == 0)
              OpendapHelper.getAttributes(das, outerSequenceName + "." + varName, sourceAtts);

            PrimitiveArray sourcePA =
                PrimitiveArray.factory(OpendapHelper.getElementPAType(outerVar), 1, false);
            if ("true".equals(sourceAtts.getString("_Unsigned")))
              sourcePA = sourcePA.makeUnsignedPA();
            PrimitiveArray destPA =
                (PrimitiveArray)
                    sourcePA.clone(); // !This doesn't handle change in type from scale_factor,
            // add_offset
            Attributes addAtts =
                makeReadyToUseAddVariableAttributesForDatasetsXml(
                    dataSourceTable.globalAttributes(),
                    sourceAtts,
                    null,
                    varName,
                    destPA.elementType() != PAType.STRING, // tryToAddStandardName
                    destPA.elementType() != PAType.STRING, // addColorBarMinMax
                    true); // tryToFindLLAT
            dataSourceTable.addColumn(nOuterVars, varName, sourcePA, sourceAtts);
            dataAddTable.addColumn(nOuterVars, varName, destPA, addAtts);
            nOuterVars++;
          }
        }
      }
    }

    // add missing_value and/or _FillValue if needed
    addMvFvAttsIfNeeded(dataSourceTable, dataAddTable);

    // tryToFindLLAT
    tryToFindLLAT(dataSourceTable, dataAddTable);

    // don't suggestSubsetVariables(), instead:
    // subset vars are nOuterVars (which are the first nOuterVar columns)
    StringArray tSubsetVariables = new StringArray();
    for (int col = 0; col < nOuterVars; col++)
      tSubsetVariables.add(dataAddTable.getColumnName(col));
    dataAddTable.globalAttributes().add("subsetVariables", tSubsetVariables.toString());

    // get global attributes and ensure required entries are present
    // after dataVariables known, add global attributes in the dataAddTable
    dataAddTable
        .globalAttributes()
        .set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(),
                // another cdm_data_type could be better; this is ok
                hasLonLatTime(dataAddTable) ? "Point" : "Other",
                tLocalSourceUrl,
                externalGlobalAttributes,
                suggestKeywords(dataSourceTable, dataAddTable)));
    if (outerSequenceName == null)
      throw new SimpleException("No Sequence variable was found for " + tLocalSourceUrl + ".dds.");
    if (gridMappingAtts != null) dataAddTable.globalAttributes().add(gridMappingAtts);

    // write the information
    boolean isDapper = tLocalSourceUrl.indexOf("dapper") > 0;
    StringBuilder sb = new StringBuilder();
    sb.append(
        "<dataset type=\"EDDTableFromDapSequence\" datasetID=\""
            + suggestDatasetID(tPublicSourceUrl)
            + "\" active=\"true\">\n"
            + "    <sourceUrl>"
            + XML.encodeAsXML(tLocalSourceUrl)
            + "</sourceUrl>\n"
            + "    <outerSequenceName>"
            + XML.encodeAsXML(outerSequenceName)
            + "</outerSequenceName>\n"
            + (innerSequenceName == null
                ? ""
                : "    <innerSequenceName>"
                    + XML.encodeAsXML(innerSequenceName)
                    + "</innerSequenceName>\n")
            + "    <skipDapperSpacerRows>"
            + isDapper
            + "</skipDapperSpacerRows>\n"
            + "    <sourceCanConstrainStringEQNE>"
            + !isDapper
            + "</sourceCanConstrainStringEQNE>\n"
            + // DAPPER doesn't support string constraints
            "    <sourceCanConstrainStringGTLT>"
            + !isDapper
            + "</sourceCanConstrainStringGTLT>\n"
            + // see email from Joe Sirott 1/21/2009
            "    <sourceCanConstrainStringRegex></sourceCanConstrainStringRegex>\n"
            + // was ~=, now ""; see notes.txt for 2009-01-16
            "    <reloadEveryNMinutes>"
            + tReloadEveryNMinutes
            + "</reloadEveryNMinutes>\n");
    sb.append(writeAttsForDatasetsXml(false, dataSourceTable.globalAttributes(), "    "));
    sb.append(cdmSuggestion());
    sb.append(writeAttsForDatasetsXml(true, dataAddTable.globalAttributes(), "    "));

    // last 2 params: includeDataType, questionDestinationName
    sb.append(
        writeVariablesForDatasetsXml(dataSourceTable, dataAddTable, "dataVariable", false, false));
    sb.append("</dataset>\n" + "\n");

    String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
    return sb.toString();
  }
}
