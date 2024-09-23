/*
 * EDDGridFromEDDTable Copyright 2015, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.CharArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.NDimensionalIndex;
import com.cohort.array.PAOne;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.handlers.EDDGridFromEDDTableHandler;
import gov.noaa.pfel.erddap.handlers.SaxHandlerClass;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;
import java.io.DataInputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class represents a grid dataset from an EDDTable source.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2015-01-27
 */
@SaxHandlerClass(EDDGridFromEDDTableHandler.class)
public class EDDGridFromEDDTable extends EDDGrid {

  protected EDDTable eddTable;

  protected int gapThreshold;
  static final int defaultGapThreshold = 1000;
  // for precision of axis value matching
  static final int floatPrecision = 5;
  static final int doublePrecision = 9;
  static final int fullPrecision = 16; // nominal, used to symbolize fullPrecision
  protected int avPrecision[];

  /**
   * This constructs an EDDGridFromEDDTable based on the information in an .xml file.
   *
   * @param erddap if known in this context, else null
   * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDGridFromEDDTable"&gt;
   *     having just been read.
   * @return an EDDGridFromEDDTable. When this returns, xmlReader will have just read
   *     &lt;erddapDatasets&gt;&lt;/dataset&gt; .
   * @throws Throwable if trouble
   */
  @EDDFromXmlMethod
  public static EDDGridFromEDDTable fromXml(Erddap erddap, SimpleXMLReader xmlReader)
      throws Throwable {

    // data to be obtained (or not)
    if (verbose) String2.log("\n*** constructing EDDGridFromEDDTable(xmlReader)...");
    String tDatasetID = xmlReader.attributeValue("datasetID");
    Attributes tGlobalAttributes = null;
    String tAccessibleTo = null;
    String tGraphsAccessibleTo = null;
    boolean tAccessibleViaWMS = true;
    boolean tAccessibleViaFiles = EDStatic.defaultAccessibleViaFiles;
    StringArray tOnChange = new StringArray();
    String tFgdcFile = null;
    String tIso19115File = null;
    ArrayList tAxisVariables = new ArrayList();
    ArrayList tDataVariables = new ArrayList();
    int tReloadEveryNMinutes = DEFAULT_RELOAD_EVERY_N_MINUTES;
    int tUpdateEveryNMillis = 0;
    String tLocalSourceUrl = null;
    String tDefaultDataQuery = null;
    String tDefaultGraphQuery = null;
    int tnThreads = -1; // interpret invalid values (like -1) as EDStatic.nGridThreads
    boolean tDimensionValuesInMemory = true;
    EDDTable tEDDTable = null;
    int tGapThreshold = defaultGapThreshold;

    // process the tags
    String startOfTags = xmlReader.allTags();
    int startOfTagsN = xmlReader.stackSize();
    int startOfTagsLength = startOfTags.length();

    while (true) {
      xmlReader.nextTag();
      String tags = xmlReader.allTags();
      String content = xmlReader.content();
      // String2.log(">>  tags=" + tags + content);
      if (xmlReader.stackSize() == startOfTagsN) break; // the </dataset> tag
      String localTags = tags.substring(startOfTagsLength);
      // String2.log(">>  localTags=" + localTags + content);

      // try to make the tag names as consistent, descriptive and readable as possible
      if (localTags.equals("<dataset>")) {
        if ("false".equals(xmlReader.attributeValue("active"))) {
          // skip it - read to </dataset>
          if (verbose)
            String2.log(
                "  skipping datasetID="
                    + xmlReader.attributeValue("datasetID")
                    + " because active=\"false\".");
          while (xmlReader.stackSize() != startOfTagsN + 1
              || !xmlReader.allTags().substring(startOfTagsLength).equals("</dataset>")) {
            xmlReader.nextTag();
            // String2.log("  skippping tags: " + xmlReader.allTags());
          }

        } else {
          String tType = xmlReader.attributeValue("type");
          if (tType == null || !tType.startsWith("EDDTable"))
            throw new SimpleException(
                "type=\""
                    + tType
                    + "\" is not allowed for the dataset within the EDDGridFromEDDTable. "
                    + "The type MUST start with \"EDDTable\".");
          String tableDatasetID = xmlReader.attributeValue("datasetID");
          if (tDatasetID == null || tableDatasetID == null || tDatasetID.equals(tableDatasetID))
            throw new SimpleException(
                "The inner eddTable datasetID must be different from the "
                    + "outer EDDGridFromEDDTable datasetID.");
          tEDDTable = (EDDTable) EDD.fromXml(erddap, tType, xmlReader);
        }

      } else if (localTags.equals("<addAttributes>"))
        tGlobalAttributes = getAttributesFromXml(xmlReader);
      else if (localTags.equals("<altitudeMetersPerSourceUnit>"))
        throw new SimpleException(EDVAlt.stopUsingAltitudeMetersPerSourceUnit);
      else if (localTags.equals("<axisVariable>"))
        tAxisVariables.add(getSDAVVariableFromXml(xmlReader));
      else if (localTags.equals("<dataVariable>"))
        tDataVariables.add(getSDADVariableFromXml(xmlReader));
      else if (localTags.equals("<accessibleTo>")) {
      } else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
      else if (localTags.equals("<graphsAccessibleTo>")) {
      } else if (localTags.equals("</graphsAccessibleTo>")) tGraphsAccessibleTo = content;
      else if (localTags.equals("<accessibleViaWMS>")) {
      } else if (localTags.equals("</accessibleViaWMS>"))
        tAccessibleViaWMS = String2.parseBoolean(content);
      else if (localTags.equals("<accessibleViaFiles>")) {
      } else if (localTags.equals("</accessibleViaFiles>"))
        tAccessibleViaFiles = String2.parseBoolean(content);
      else if (localTags.equals("<reloadEveryNMinutes>")) {
      } else if (localTags.equals("</reloadEveryNMinutes>"))
        tReloadEveryNMinutes = String2.parseInt(content);
      else if (localTags.equals("<updateEveryNMillis>")) {
      } else if (localTags.equals("</updateEveryNMillis>"))
        tUpdateEveryNMillis = String2.parseInt(content);
      else if (localTags.equals("<onChange>")) {
      } else if (localTags.equals("</onChange>")) tOnChange.add(content);
      else if (localTags.equals("<fgdcFile>")) {
      } else if (localTags.equals("</fgdcFile>")) tFgdcFile = content;
      else if (localTags.equals("<iso19115File>")) {
      } else if (localTags.equals("</iso19115File>")) tIso19115File = content;
      else if (localTags.equals("<defaultDataQuery>")) {
      } else if (localTags.equals("</defaultDataQuery>")) tDefaultDataQuery = content;
      else if (localTags.equals("<defaultGraphQuery>")) {
      } else if (localTags.equals("</defaultGraphQuery>")) tDefaultGraphQuery = content;
      else if (localTags.equals("<nThreads>")) {
      } else if (localTags.equals("</nThreads>")) tnThreads = String2.parseInt(content);
      else if (localTags.equals("<dimensionValuesInMemory>")) {
      } else if (localTags.equals("</dimensionValuesInMemory>"))
        tDimensionValuesInMemory = String2.parseBoolean(content);
      else if (localTags.equals("<gapThreshold>")) {
      } else if (localTags.equals("</gapThreshold>")) tGapThreshold = String2.parseInt(content);
      else xmlReader.unexpectedTagException();
    }
    int nav = tAxisVariables.size();
    Object ttAxisVariables[][] = nav == 0 ? null : new Object[nav][];
    for (int i = 0; i < tAxisVariables.size(); i++)
      ttAxisVariables[i] = (Object[]) tAxisVariables.get(i);

    int ndv = tDataVariables.size();
    Object ttDataVariables[][] = new Object[ndv][];
    for (int i = 0; i < tDataVariables.size(); i++)
      ttDataVariables[i] = (Object[]) tDataVariables.get(i);

    return new EDDGridFromEDDTable(
        tDatasetID,
        tAccessibleTo,
        tGraphsAccessibleTo,
        tAccessibleViaWMS,
        tAccessibleViaFiles,
        tOnChange,
        tFgdcFile,
        tIso19115File,
        tDefaultDataQuery,
        tDefaultGraphQuery,
        tGlobalAttributes,
        ttAxisVariables,
        ttDataVariables,
        tReloadEveryNMinutes,
        tUpdateEveryNMillis,
        tGapThreshold,
        tEDDTable,
        tnThreads,
        tDimensionValuesInMemory);
  }

  /**
   * The constructor. The axisVariables must be the same and in the same order for each
   * dataVariable.
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
   *     </ul>
   *     Special case: value="null" causes that item to be removed from combinedGlobalAttributes.
   *     Special case: if combinedGlobalAttributes name="license", any instance of "[standard]" will
   *     be converted to the EDStatic.standardLicense.
   * @param tAxisVariables is an Object[nAxisVariables][3]: <br>
   *     [0]=String sourceName (the name of the data variable in the dataset source), <br>
   *     [1]=String destinationName (the name to be presented to the ERDDAP user, or null to use the
   *     sourceName), <br>
   *     [2]=Attributes addAttributes (at ERD, this must have "ioos_category" - a category from
   *     EDV.ioosCategories, although they are added automatically for lon, lat, alt, and time).
   *     Special case: value="null" causes that item to be removed from combinedAttributes. <br>
   *     If there are longitude, latitude, altitude, or time variables, they must have that name as
   *     the destinationName (or sourceName) to be identified as such. <br>
   *     Or, use tAxisVariables=null if the axis variables need no addAttributes and the
   *     longitude,latitude,altitude,time variables (if present) all have their correct names in the
   *     source. <br>
   *     The order of variables you define must match the order in the source. <br>
   *     A time variable must have "units" specified in addAttributes (read first) or
   *     sourceAttributes. "units" must be a udunits string (containing " since ") describing how to
   *     interpret numbers (e.g., "seconds since 1970-01-01T00:00:00Z").
   * @param tDataVariables is an Object[nDataVariables][3]: <br>
   *     [0]=String sourceName (the name of the data variable in the dataset source), <br>
   *     [1]=String destinationName (the name to be presented to the ERDDAP user, or null to use the
   *     sourceName), <br>
   *     [2]=Attributes addAttributes (at ERD, this must have "ioos_category" - a category from
   *     EDV.ioosCategories). Special case: value="null" causes that item to be removed from
   *     combinedAttributes. <br>
   *     All dataVariables must share the same axis variables. <br>
   *     The order of variables you define doesn't have to match the order in the source.
   * @param tReloadEveryNMinutes indicates how often the source should be checked for new data (use
   *     Integer.MAX_VALUE for never). Use -1 to have ERDDAP suggest the value based on how recent
   *     the last time value is.
   * @param tGapThreshold
   * @param tEDDTable the source EDDTable. It must have a different datasetID. The destination
   *     information from the eddTable becomes the source information for the eddGrid.
   * @throws Throwable if trouble
   */
  public EDDGridFromEDDTable(
      String tDatasetID,
      String tAccessibleTo,
      String tGraphsAccessibleTo,
      boolean tAccessibleViaWMS,
      boolean tAccessibleViaFiles,
      StringArray tOnChange,
      String tFgdcFile,
      String tIso19115File,
      String tDefaultDataQuery,
      String tDefaultGraphQuery,
      Attributes tAddGlobalAttributes,
      Object tAxisVariables[][],
      Object tDataVariables[][],
      int tReloadEveryNMinutes,
      int tUpdateEveryNMillis,
      int tGapThreshold,
      EDDTable tEDDTable,
      int tnThreads,
      boolean tDimensionValuesInMemory)
      throws Throwable {

    if (verbose) String2.log("\n*** constructing EDDGridFromEDDTable " + tDatasetID);
    long constructionStartMillis = System.currentTimeMillis();
    String errorInMethod = "Error in EDDGridFromEDDTable(" + tDatasetID + ") constructor:\n";

    // save some of the parameters
    className = "EDDGridFromEDDTable";
    datasetID = tDatasetID;
    setAccessibleTo(tAccessibleTo);
    setGraphsAccessibleTo(tGraphsAccessibleTo);
    if (!tAccessibleViaWMS)
      accessibleViaWMS = String2.canonical(MessageFormat.format(EDStatic.noXxxAr[0], "WMS"));
    accessibleViaFiles =
        EDStatic.filesActive && tAccessibleViaFiles && tEDDTable.accessibleViaFiles;
    onChange = tOnChange;
    fgdcFile = tFgdcFile;
    iso19115File = tIso19115File;
    defaultDataQuery = tDefaultDataQuery;
    defaultGraphQuery = tDefaultGraphQuery;
    addGlobalAttributes = tAddGlobalAttributes == null ? new Attributes() : tAddGlobalAttributes;
    setReloadEveryNMinutes(tReloadEveryNMinutes);
    setUpdateEveryNMillis(tUpdateEveryNMillis);
    gapThreshold = tGapThreshold == Integer.MAX_VALUE ? defaultGapThreshold : tGapThreshold;
    // The destination information from the eddTable
    // becomes the source information for the eddGrid.
    eddTable = tEDDTable;
    nThreads = tnThreads; // interpret invalid values (like -1) as EDStatic.nGridThreads
    dimensionValuesInMemory = tDimensionValuesInMemory;

    // quickRestart is handled by contained eddGrid

    // get global attributes
    sourceGlobalAttributes = eddTable.combinedGlobalAttributes();
    combinedGlobalAttributes =
        new Attributes(addGlobalAttributes, sourceGlobalAttributes); // order is important
    String tLicense = combinedGlobalAttributes.getString("license");
    if (tLicense != null)
      combinedGlobalAttributes.set(
          "license", String2.replaceAll(tLicense, "[standard]", EDStatic.standardLicense));
    combinedGlobalAttributes.removeValue("\"null\"");
    if (combinedGlobalAttributes.getString("cdm_data_type") == null)
      combinedGlobalAttributes.add("cdm_data_type", "Grid");

    // create axisVariables[]
    int nav = tAxisVariables.length;
    axisVariables = new EDVGridAxis[nav];
    // for precision of axis value matching
    avPrecision = new int[nav]; // initially all 0, i.e., not set
    for (int av = 0; av < nav; av++) {
      String tSourceName = (String) tAxisVariables[av][0];
      String tDestName = (String) tAxisVariables[av][1];
      if (tDestName == null || tDestName.length() == 0) tDestName = tSourceName;
      EDV sourceEdv = eddTable.findDataVariableByDestinationName(tSourceName);
      Attributes tSourceAtts = sourceEdv.combinedAttributes();
      Attributes tAddAtts = (Attributes) tAxisVariables[av][2];
      String msg =
          errorInMethod + "For axisVariable[" + av + "] destinationName=" + tDestName + ": ";

      // get sourceValues from addAttributes!
      // but how update as time goes on???
      PrimitiveArray tSourceValues = tAddAtts.remove("axisValues");
      if (tSourceValues == null) {
        String avsss = "axisValuesStartStrideStop";
        PrimitiveArray sss = tAddAtts.remove(avsss);
        if (sss == null)
          throw new RuntimeException(msg + ": attribute=\"axisValues\" wasn't specified.");
        if (sss instanceof StringArray || sss instanceof CharArray)
          throw new RuntimeException(msg + avsss + "'s type must be a numeric type, not String.");
        if (sss.size() != 3)
          throw new RuntimeException(msg + avsss + " size=" + sss.size() + ". It must be 3.");
        double tStart = sss.getDouble(0);
        double tStride = sss.getDouble(1);
        double tStop = sss.getDouble(2);
        if (Double.isNaN(tStart)
            || Double.isNaN(tStride)
            || Double.isNaN(tStop)
            || tStart > tStop
            || tStride <= 0
            || tStride > tStop - tStart
            || ((tStop - tStart) / (double) tStride > 1e7))
          throw new RuntimeException(
              msg
                  + "Invalid "
                  + avsss
                  + ": start="
                  + tStart
                  + ", stride="
                  + tStride
                  + ", stop="
                  + tStop);
        int n = Math2.roundToInt((tStop - tStart) / (double) tStride) + 3; // 2 extra
        tSourceValues = PrimitiveArray.factory(sss.elementType(), n, false);
        for (int i = 0; i < n; i++) {
          double d = tStart + i * tStride; // more accurate than repeatedly add
          if (d > tStop) break;
          tSourceValues.addDouble(d);
        }
      }

      // make the axisVariable
      axisVariables[av] =
          makeAxisVariable(
              datasetID, av, tSourceName, tDestName, tSourceAtts, tAddAtts, tSourceValues);

      // ensure axis is ascending sorted
      if (!axisVariables[av].isAscending()) // source values
      throw new RuntimeException(msg + "The sourceValues must be ascending!");

      // precision  for matching axis values
      int tPrecision = axisVariables[av].combinedAttributes().getInt("precision");
      if (axisVariables[av] instanceof EDVTimeStampGridAxis) {
        avPrecision[av] = fullPrecision; // always compare times at full precision
      } else if (tSourceValues instanceof FloatArray) {
        avPrecision[av] = tPrecision == Integer.MAX_VALUE ? floatPrecision : tPrecision;
      } else if (tSourceValues instanceof DoubleArray) {
        avPrecision[av] = tPrecision == Integer.MAX_VALUE ? doublePrecision : tPrecision;
      } else { // all int types
        avPrecision[av] = fullPrecision;
      }
    }

    // create dataVariables[]
    dataVariables = new EDV[tDataVariables.length];
    for (int dv = 0; dv < tDataVariables.length; dv++) {
      String tSourceName = (String) tDataVariables[dv][0];
      String tDestName = (String) tDataVariables[dv][1];
      if (tDestName == null || tDestName.length() == 0) tDestName = tSourceName;
      EDV sourceEdv = eddTable.findDataVariableByDestinationName(tSourceName);
      String dvSourceDataType = sourceEdv.destinationDataType();
      Attributes tSourceAtts = sourceEdv.combinedAttributes();
      Attributes tAddAtts = (Attributes) tDataVariables[dv][2];

      // create the EDV dataVariable
      if (tDestName.equals(EDV.TIME_NAME))
        throw new RuntimeException(
            errorInMethod + "No EDDGrid dataVariable may have destinationName=" + EDV.TIME_NAME);
      else if (EDVTime.hasTimeUnits(tSourceAtts, tAddAtts))
        dataVariables[dv] =
            new EDVTimeStamp(
                datasetID, tSourceName, tDestName, tSourceAtts, tAddAtts, dvSourceDataType);
      else
        dataVariables[dv] =
            new EDV(
                datasetID,
                tSourceName,
                tDestName,
                tSourceAtts,
                tAddAtts,
                dvSourceDataType,
                PAOne.fromDouble(Double.NaN),
                PAOne.fromDouble(Double.NaN)); // hard to get min and max
      dataVariables[dv].extractAndSetActualRange();
    }

    // ensure the setup is valid
    ensureValid();

    // If the child is a FromErddap, try to subscribe to the remote dataset.
    if (eddTable instanceof FromErddap) tryToSubscribeToChildFromErddap(eddTable);

    // finally
    long cTime = System.currentTimeMillis() - constructionStartMillis;
    if (verbose)
      String2.log(
          (debugMode ? "\n" + toString() : "")
              + "\n*** EDDGridFromEDDTable "
              + datasetID
              + " constructor finished. TIME="
              + cTime
              + "ms"
              + (cTime >= 600000 ? "  (>10m!)" : cTime >= 10000 ? "  (>10s!)" : "")
              + "\n");

    // very last thing: saveDimensionValuesInFile
    if (!dimensionValuesInMemory) saveDimensionValuesInFile();
  }

  /**
   * This makes a sibling dataset, based on the new sourceUrl.
   *
   * @throws Throwable always (since this class doesn't support sibling())
   */
  @Override
  public EDDGrid sibling(
      String tLocalSourceUrl, int firstAxisToMatch, int matchAxisNDigits, boolean shareInfo)
      throws Throwable {
    throw new SimpleException(
        "Error: " + "EDDGridFromEDDTable doesn't support method=\"sibling\".");
  }

  /**
   * This does the actual incremental update of this dataset (i.e., for real time datasets).
   *
   * <p>Concurrency issue: The changes here are first prepared and then applied as quickly as
   * possible (but not atomically!). There is a chance that another thread will get inconsistent
   * information (from some things updated and some things not yet updated). But I don't want to
   * synchronize all activities of this class.
   *
   * @param language the index of the selected language
   * @param msg the start of a log message, e.g., "update(thisDatasetID): ".
   * @param startUpdateMillis the currentTimeMillis at the start of this update.
   * @return true if a change was made
   * @throws Throwable if serious trouble. For simple failures, this writes info to log.txt but
   *     doesn't throw an exception. If the dataset has changed in a serious / incompatible way and
   *     needs a full reload, this throws WaitThenTryAgainException (usually, catcher calls
   *     LoadDatasets.tryToUnload(...) and EDD.requestReloadASAP(tDatasetID)).. If the changes
   *     needed are probably fine but are too extensive to deal with here, this calls
   *     EDD.requestReloadASAP(tDatasetID) and returns without doing anything.
   */
  @Override
  public boolean lowUpdate(int language, String msg, long startUpdateMillis) throws Throwable {
    // but somehow, this should update the outer time axis in this eddGrid.

    // update the internal eddTable
    return eddTable.lowUpdate(language, msg, startUpdateMillis);
  }

  /**
   * This gets source data (not yet converted to destination data) from the data source for this
   * EDDGrid. Because this is called by GridDataAccessor, the request won't be the full user's
   * request, but will be a partial request (for less than EDStatic.partialRequestMaxBytes).
   *
   * @param language the index of the selected language
   * @param tDirTable If EDDGridFromFiles, this MAY be the dirTable, else null.
   * @param tFileTable If EDDGridFromFiles, this MAY be the fileTable, else null.
   * @param tDataVariables EDV[] with just the requested data variables
   * @param tConstraints int[nAxisVariables*3] where av*3+0=startIndex, av*3+1=stride,
   *     av*3+2=stopIndex. AxisVariables are counted left to right, e.g., sst[0=time][1=lat][2=lon].
   * @return a PrimitiveArray[] where the first axisVariables.length elements are the axisValues and
   *     the next tDataVariables.length elements are the dataValues. Both the axisValues and
   *     dataValues are straight from the source, not modified.
   * @throws Throwable if trouble (notably, WaitThenTryAgainException)
   */
  @Override
  public PrimitiveArray[] getSourceData(
      int language, Table tDirTable, Table tFileTable, EDV tDataVariables[], IntArray tConstraints)
      throws Throwable {

    // loggedInAs isn't known here, but EDDTable uses it for row-by-row
    // authentication. null = Not good, but okay/safe, since it just denies access.
    String loggedInAs = null;

    // create eddTable query from constraints
    int nav = axisVariables.length;
    int ndv = tDataVariables.length;
    int nCols = nav + ndv;

    // create the results[] PAs (filled with missing values) to hold the results
    PrimitiveArray results[] = new PrimitiveArray[nCols];
    int[] start = new int[nav];
    int[] stride = new int[nav];
    int[] stop = new int[nav];
    int[] nEachAV = new int[nav]; // for this request
    // This is for calculating arbitrary position within dataVariable results arrays.
    // This is similar to the digit in 100's place in an integer having a value of 100.
    // It reflects the size of the request as if all axes had stride=1.
    // (It is the amount of data that would/will be transferred, related to each axis.)
    int[] resultAvValue = new int[nav];
    // This is for gap sizes.
    // This is similar to the digit in 100's place in an integer having a value of 100.
    // It reflects the size of the request as if all axes had stride=1.
    // (It is the amount of data that would/will be transferred, related to each axis.)
    int[] gapAvValue = new int[nav];
    int[] fullNEachAV = new int[nav]; // full axis sizes for this dataset
    Arrays.fill(resultAvValue, 1);
    Arrays.fill(gapAvValue, 1);
    // If the user's request is huge, this request will be a small sub-request.
    // It is constrained by partialRequestMaxCells. So an int for nv will be fine.
    int willKeepNRows = 1; // the number of rows that will be kept
    // create results PAs for axisVariables, filled with correct source values
    for (int av = 0; av < nav; av++) {
      start[av] = tConstraints.get(av * 3);
      stride[av] = tConstraints.get(av * 3 + 1);
      stop[av] = tConstraints.get(av * 3 + 2);
      PrimitiveArray sourceValues = axisVariables[av].sourceValues();
      int tFullSize = sourceValues.size();
      fullNEachAV[av] = tFullSize;
      results[av] = sourceValues.subset(start[av], stride[av], stop[av]);
      int tSize = results[av].size();
      nEachAV[av] = tSize;
      willKeepNRows *= tSize;
      for (int av2 = 0; av2 < av; av2++) {
        resultAvValue[av2] *= tSize; // using actual stride
        gapAvValue[av2] *= (stop[av] - start[av] + 1); // as if stride=1
      }
    }
    // create results PAs for dataVariables, full-sized, filled with missing values
    for (int dv = 0; dv < ndv; dv++) {
      EDV edv = tDataVariables[dv];
      double smv = edv.safeDestinationMissingValue();
      results[nav + dv] =
          PrimitiveArray.factory(
              edv.sourceDataPAType(),
              willKeepNRows,
              Double.isNaN(smv) ? "" : "" + smv); // filled with missing values
    }

    // Think of source table as having all rows in order.
    // Think of axes left to right as, e.g., nav=3 sst[0=time][1=lat][2=lon]
    // If an axis' request stride times the gapAvValue of that dimension is big,
    //  the data requested may be
    //  1 or few rows, then a big gap (lots of rows), then
    //  1 or few rows, then a big gap (lots of rows), etc.
    // With just one big request, all the data (including the gaps)
    //  would be requested then thrown away below.
    // So go through axes right to left, looking for rightmost gap > gapThreshold.
    // That defines outerAxisVars (on left) and innerAxisVars (on right).
    // Then, I'll make subrequests with single values of outerAxisVars
    //  and the full range of innerAxisVars.
    // COMPLICATED CODE!  Great potential for off-by-one errors!
    int nInnerAxes = 0; // to start: will it be just 0?  It may end up 0 .. nav.
    int gap = 0;
    while (nInnerAxes < nav) {
      // if (stride-1) * gapAvValue < gapThreshold, continue
      // (stride=1 -> nSkip=0, so use stride-1 to find nSkipped)
      gap = (stride[(nav - 1) - nInnerAxes] - 1) * gapAvValue[(nav - 1) - nInnerAxes];
      if (debugMode)
        String2.log(
            "nInner="
                + nInnerAxes
                + " gap="
                + gap
                + " = ((stride="
                + stride[(nav - 1) - nInnerAxes]
                + ")-1) * gapAvValue="
                + gapAvValue[(nav - 1) - nInnerAxes]);
      if (gap >= gapThreshold) break;
      nInnerAxes++;
    }
    int nOuterAxes = nav - nInnerAxes;

    // make NDimensionalIndex for outerAxisVars
    int shape[];
    if (nOuterAxes == 0) {
      shape = new int[] {1};
    } else {
      shape = new int[nOuterAxes];
      for (int av = 0; av < nOuterAxes; av++) shape[av] = results[av].size();
    }
    // The outerNDIndex has the leftmost/outerAxis
    // It will walk us through the individual values.
    NDimensionalIndex outerNDIndex = new NDimensionalIndex(shape);
    int outerNDCurrent[] = outerNDIndex.getCurrent(); // it will be repeatedly updated
    if (verbose)
      String2.log(
          "* nOuterAxes=" + nOuterAxes + " of " + nav + " nOuterRequests=" + outerNDIndex.size());
    if (reallyVerbose)
      String2.log(
          "  axis sizes: result="
              + String2.toCSSVString(nEachAV)
              + " dataset="
              + String2.toCSSVString(fullNEachAV)
              + "\n  strides="
              + String2.toCSSVString(stride)
              + " gapAvValues="
              + String2.toCSSVString(gapAvValue)
              + (nOuterAxes == 0
                  ? ""
                  : ("\n  gap="
                      + gap
                      + "=((stride="
                      + stride[(nav - 1) - nInnerAxes]
                      + ")-1) * gapAvValue="
                      + gapAvValue[(nav - 1) - nInnerAxes]
                      + " >= gapThreshold="
                      + gapThreshold)));

    // make a subrequest for each combination of outerAxes values
    TableWriterAll twa =
        new TableWriterAll(
            language,
            null,
            null, // metadata not kept
            cacheDirectory(),
            suggestFileName(loggedInAs, datasetID, ".twa"));
    twa.ignoreFinish = true;
    int qn = 0;
    while (outerNDIndex.increment()) { // updates outerNDCurrent

      // build the eddTable query for this subset of full request
      // axisVariable sourceNames
      StringBuilder querySB =
          new StringBuilder(
              String2.toSVString(
                  axisVariableSourceNames(), ",", false)); // don't include trailing separator
      // requested dataVariable sourceNames
      for (int dv = 0; dv < ndv; dv++) querySB.append("," + tDataVariables[dv].sourceName());
      // outer constraints     COMPLICATED!
      for (int av = 0; av < nOuterAxes; av++) {
        EDVGridAxis edvga = axisVariables[av];
        String sn = edvga.sourceName();
        PrimitiveArray sv = edvga.sourceValues();
        querySB.append( // just request 1 value for outerAxes
            "&" + sn + "=" + sv.getDouble(start[av] + outerNDCurrent[av] * stride[av]));
      }
      // inner constraints
      for (int iav = 0; iav < nInnerAxes; iav++) {
        int av = nOuterAxes + iav;
        EDVGridAxis edvga = axisVariables[av];
        String sn = edvga.sourceName();
        PrimitiveArray sv = edvga.sourceValues();
        querySB.append( // always request the full range of innerAxes
            "&"
                + sn
                + ">="
                + sv.getDouble(start[av])
                +
                // unfortunately, no way to use the stride value in the constraint
                "&"
                + sn
                + "<="
                + sv.getDouble(stop[av]));
      }
      String query = querySB.toString();
      if (verbose) String2.log("query#" + qn++ + " for eddTable=" + query);

      // get the tabular results
      String requestUrl =
          "/erddap/tabledap/"
              + datasetID
              + ".twa"; // I think irrelevant, since not getting metadata from source
      try {
        if (eddTable.handleViaFixedOrSubsetVariables(
            language, loggedInAs, requestUrl, query, twa)) {
        } else eddTable.getDataForDapQuery(language, loggedInAs, requestUrl, query, twa);
      } catch (Throwable t) {
        String msg = t.toString();
        if (msg == null || msg.indexOf(MustBe.THERE_IS_NO_DATA) < 0) throw t;
      }
    }
    twa.ignoreFinish = false;
    twa.finish();

    // go through the tabular results in TableWriterAll
    PrimitiveArray twaPA[] = new PrimitiveArray[nCols];
    DataInputStream twaDIS[] = new DataInputStream[nCols];
    int nMatches = 0;
    long nRows = twa.nRows();
    try {
      for (int col = 0; col < nCols; col++) {
        twaPA[col] = twa.columnEmptyPA(col);
        twaDIS[col] = twa.dataInputStream(col);
      }

      int oAxisIndex[] = new int[nav]; // all 0's
      int axisIndex[] = new int[nav]; // all 0's
      int dataIndex = 0; // index for the data results[] PAs
      rowLoop:
      for (long row = 0; row < nRows; row++) {
        // read all of the twa values for this row
        for (int col = 0; col < nCols; col++) {
          twaPA[col].clear();
          twaPA[col].readDis(twaDIS[col], 1); // read 1 value
        }

        // see if this row matches a desired combo of axis values
        dataIndex = 0; // index for the data results[] PAs
        System.arraycopy(axisIndex, 0, oAxisIndex, 0, nav);
        for (int av = 0; av < nav; av++) {

          PAOne twaPAOne = new PAOne(twaPA[av], 0);
          if (debugMode) String2.log("row=" + row + " av=" + av + " value=" + twaPAOne);
          int navPA = results[av].size();
          int insertAt;
          int precision = avPrecision[av];
          int oIndex = oAxisIndex[av];
          if (precision == fullPrecision) {
            if (twaPAOne.equals(results[av], oIndex)) // same as last row?
            insertAt = oIndex;
            else insertAt = results[av].binarySearch(0, navPA - 1, twaPAOne);
          } else {
            if (twaPAOne.almostEqual(precision, new PAOne(results[av], oIndex))) {
              insertAt = oIndex;
            } else {
              insertAt = results[av].binaryFindFirstGAE(0, navPA - 1, twaPAOne, precision);
              if (insertAt >= navPA
                  || // not close
                  !twaPAOne.almostEqual(precision, new PAOne(results[av], insertAt))) insertAt = -1;
            }
          }
          if (insertAt >= 0) {
            if (debugMode) String2.log("  matched index=" + insertAt);
            axisIndex[av] = insertAt;
            dataIndex += insertAt * resultAvValue[av];
          } else {
            continue rowLoop;
          }
        }

        // all axes have a match! so copy data values into results[]
        nMatches++;
        if (debugMode)
          String2.log("  axisIndex=" + String2.toCSSVString(axisIndex) + " dataIndex=" + dataIndex);
        for (int dv = 0; dv < ndv; dv++) {
          int col = nav + dv;
          results[col].setFromPA(dataIndex, twaPA[col], 0);
        }
      }
    } finally {
      // release twa resources
      for (int col = 0; col < nCols; col++)
        try {
          twaDIS[col].close();
        } catch (Exception e) {
        }
      try {
        twa.releaseResources();
      } catch (Exception e) {
      }
    }

    String2.log(
        "EDDGridFromEDDTable nMatches=" + nMatches + " out of TableWriterAll nRows=" + nRows);
    return results;
  }

  /**
   * This returns a fileTable with valid files (or null if unavailable or any trouble). This is a
   * copy of any internal data, so client can modify the contents.
   *
   * @param nextPath is the partial path (with trailing slash) to be appended onto the local fileDir
   *     (or wherever files are, even url).
   * @return null if trouble, or Object[3] [0] is a sorted table with file "Name" (String), "Last
   *     modified" (long millis), "Size" (long), and "Description" (String, but usually no content),
   *     [1] is a sorted String[] with the short names of directories that are 1 level lower, and
   *     [2] is the local directory corresponding to this (or null, if not a local dir).
   */
  @Override
  public Object[] accessibleViaFilesFileTable(int language, String nextPath) {
    if (!accessibleViaFiles) return null;
    // Get childDataset or localChildDataset. Work with stable local reference.
    EDDTable tChildDataset = eddTable; // getChildDataset();
    return tChildDataset.accessibleViaFilesFileTable(language, nextPath);
  }

  /**
   * This converts a relativeFileName into a full localFileName (which may be a url).
   *
   * @param language the index of the selected language
   * @param relativeFileName (for most EDDTypes, just offset by fileDir)
   * @return full localFileName or null if any error (including, file isn't in list of valid files
   *     for this dataset)
   */
  @Override
  public String accessibleViaFilesGetLocal(int language, String relativeFileName) {
    if (!accessibleViaFiles) return null;
    // Get childDataset or localChildDataset. Work with stable local reference.
    EDDTable tChildDataset = eddTable; // getChildDataset();
    return tChildDataset.accessibleViaFilesGetLocal(language, relativeFileName);
  }

  /**
   * This creates a rough draft of a datasets.xml entry for an EDDGridFromEDDTable. The XML MUST
   * then be edited by hand and added to the datasets.xml file.
   *
   * @param eddTableID the datasetID of the underlying EDDTable dataset.
   * @param tReloadEveryNMinutes E.g., 1440 for once per day. Use, e.g., 1000000000, for never
   *     reload. -1 for the default.
   * @param externalAddGlobalAttributes globalAttributes gleaned from external sources, e.g., a
   *     THREDDS catalog.xml file. These have priority over other sourceGlobalAttributes. Okay to
   *     use null if none.
   * @return a suggested chunk of xml for this dataset for use in datasets.xml
   * @throws Throwable if trouble, e.g., if no Grid or Array variables are found. If no trouble,
   *     then a valid dataset.xml chunk has been returned.
   */
  public static String generateDatasetsXml(
      String eddTableID, int tReloadEveryNMinutes, Attributes externalAddGlobalAttributes)
      throws Throwable {

    String2.log(
        "\n*** EDDGridFromEDDTable.generateDatasetsXml"
            + "\neddTableID="
            + eddTableID
            + " reloadEveryNMinutes="
            + tReloadEveryNMinutes
            + "\nexternalAddGlobalAttributes="
            + externalAddGlobalAttributes);
    Table sourceAxisTable = new Table();
    Table sourceDataTable = new Table();
    Table addAxisTable = new Table();
    Table addDataTable = new Table();

    // get the eddTable
    EDDTable eddTable = (EDDTable) oneFromDatasetsXml(null, eddTableID);
    Attributes sourceGAtts = sourceDataTable.globalAttributes();
    Attributes destGAtts = addDataTable.globalAttributes();
    if (externalAddGlobalAttributes != null) destGAtts.set(externalAddGlobalAttributes);
    sourceGAtts.set(eddTable.combinedGlobalAttributes());
    destGAtts.add("cdm_data_type", "Grid");
    if (sourceGAtts.get("featureType") != null) destGAtts.set("featureType", "null");

    int ndv = eddTable.dataVariables().length;
    for (int dv = 0; dv < ndv; dv++) {
      EDV sourceEdv = eddTable.dataVariables()[dv];
      String destName = sourceEdv.destinationName();
      PAType tPAType = sourceEdv.destinationDataPAType();
      Attributes sourceAtts = sourceEdv.combinedAttributes();
      Attributes destAtts = new Attributes();
      if (destName.equals(EDV.TIME_NAME)
          || destName.equals(EDV.ALT_NAME)
          || destName.equals(EDV.DEPTH_NAME)
          || sourceAtts.get("instance_dimension") != null
          || sourceAtts.get("sample_dimension") != null) {
        // make it an axisVariable
        sourceAxisTable.addColumn(
            sourceAxisTable.nColumns(),
            destName,
            PrimitiveArray.factory(tPAType, 1, false),
            sourceAtts);
        addAxisTable.addColumn(
            addAxisTable.nColumns(), destName, PrimitiveArray.factory(tPAType, 1, false), destAtts);
        if (sourceAtts.get("cf_role") != null) destAtts.set("cf_role", "null");
        if (sourceAtts.get("instance_dimension") != null)
          destAtts.set("instance_dimension", "null");
        if (sourceAtts.get("sample_dimension") != null) destAtts.set("sample_dimension", "null");
      } else {
        // leave it as a dataVariable
        sourceDataTable.addColumn(
            sourceDataTable.nColumns(),
            destName,
            PrimitiveArray.factory(tPAType, 1, false),
            sourceAtts);
        addDataTable.addColumn(
            addDataTable.nColumns(),
            destName,
            makeDestPAForGDX(PrimitiveArray.factory(tPAType, 1, false), sourceAtts),
            destAtts);

        // Don't call because presumably already good:
        // add missing_value and/or _FillValue if needed
        // addMvFvAttsIfNeeded(destName, destPA, sourceAtts, addAtts);

      }
    }

    // write the information
    StringBuilder results = new StringBuilder();
    results.append(
        "<dataset type=\"EDDGridFromEDDTable\" datasetID=\""
            + suggestDatasetID(eddTableID + "/EDDGridFromEDDTable")
            + "\" active=\"true\">\n"
            + "    <reloadEveryNMinutes>"
            + (tReloadEveryNMinutes < 0 || tReloadEveryNMinutes == Integer.MAX_VALUE
                ? DEFAULT_RELOAD_EVERY_N_MINUTES
                : tReloadEveryNMinutes)
            + "</reloadEveryNMinutes>\n"
            + "    <gapThreshold>"
            + defaultGapThreshold
            + "</gapThreshold>\n");
    results.append(writeAttsForDatasetsXml(false, sourceDataTable.globalAttributes(), "    "));
    results.append(writeAttsForDatasetsXml(true, addDataTable.globalAttributes(), "    "));
    results.append(
        "\n"
            + "    <!-- *** If appropriate:\n"
            + "      * Change some of the <dataVariables> to be <axisVariables>.\n"
            + "      * Insert them here in the correct order.\n"
            + "      * For each one, add to its <addAttributes> one of:\n"
            + "        <att name=\"axisValues\" type=\"doubleList\">a CSV list of values</att>\n"
            + "        <att name=\"axisValuesStartStrideStop\" type=\"doubleList\">startValue, strideValue, stopValue</att>\n"
            + "      * For each one, if defaults aren't suitable, add\n"
            + "        <att name=\"precision\" type=\"int\">totalNumberOfDigitsToMatch</att>\n"
            + "    -->\n\n");
    results.append(
        writeVariablesForDatasetsXml(
            sourceAxisTable,
            addAxisTable,
            "axisVariable", // assume LLAT already identified
            false,
            false)); // includeDataType, questionDestinationName
    results.append("\n");
    results.append(
        writeVariablesForDatasetsXml(
            sourceDataTable,
            addDataTable,
            "dataVariable",
            false,
            false)); // includeDataType, questionDestinationName
    results.append(
        "\n"
            + "    <!-- *** Insert the entire <dataset> chunk for "
            + eddTableID
            + " here.\n"
            + "       If the original dataset will be accessible to users, change the\n"
            + "       datasetID here so they aren't the same. -->\n"
            + "    <dataset ... > ... </dataset>\n"
            + "\n"
            + "</dataset>\n"
            + "\n");

    String2.log("\n*** generateDatasetsXml finished successfully.\n");
    return results.toString();
  }
}
