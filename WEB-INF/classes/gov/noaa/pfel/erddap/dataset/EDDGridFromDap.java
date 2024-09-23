/*
 * EDDGridFromDap Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
import com.cohort.array.PAOne;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.ShortArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.ResourceBundle2;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;
import dods.dap.*;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.handlers.EDDGridFromDapHandler;
import gov.noaa.pfel.erddap.handlers.SaxHandlerClass;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;
import java.io.ByteArrayInputStream;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import thredds.client.catalog.Access;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.Documentation;
import thredds.client.catalog.ServiceType;
import thredds.client.catalog.ThreddsMetadata.Contributor;
import thredds.client.catalog.ThreddsMetadata.Source;
import thredds.client.catalog.ThreddsMetadata.Vocab;
import thredds.client.catalog.builder.CatalogBuilder;

/**
 * This class represents a grid dataset from an opendap DAP source.
 *
 * <p>Note that THREDDS has a default limit of 500MB for opendap responses. See
 * https://www.unidata.ucar.edu/software/thredds/current/tds/reference/ThreddsConfigXMLFile.html#opendap
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-06-04
 */
@SaxHandlerClass(EDDGridFromDapHandler.class)
public class EDDGridFromDap extends EDDGrid {

  /**
   * Indicates if data can be transmitted in a compressed form. It is unlikely anyone would want to
   * change this.
   */
  public static boolean acceptDeflate = true;

  /**
   * This constructs an EDDGridFromDap based on the information in an .xml file.
   *
   * @param erddap if known in this context, else null
   * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDGridFromDap"&gt; having
   *     just been read.
   * @return an EDDGridFromDap. When this returns, xmlReader will have just read
   *     &lt;erddapDatasets&gt;&lt;/dataset&gt; .
   * @throws Throwable if trouble
   */
  @EDDFromXmlMethod
  public static EDDGridFromDap fromXml(Erddap erddap, SimpleXMLReader xmlReader) throws Throwable {

    // data to be obtained (or not)
    if (verbose) String2.log("\n*** constructing EDDGridFromDap(xmlReader)...");
    String tDatasetID = xmlReader.attributeValue("datasetID");
    Attributes tGlobalAttributes = null;
    String tAccessibleTo = null;
    String tGraphsAccessibleTo = null;
    boolean tAccessibleViaWMS = true;
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
      if (localTags.equals("<addAttributes>")) tGlobalAttributes = getAttributesFromXml(xmlReader);
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
      else if (localTags.equals("<reloadEveryNMinutes>")) {
      } else if (localTags.equals("</reloadEveryNMinutes>"))
        tReloadEveryNMinutes = String2.parseInt(content);
      else if (localTags.equals("<updateEveryNMillis>")) {
      } else if (localTags.equals("</updateEveryNMillis>"))
        tUpdateEveryNMillis = String2.parseInt(content);
      else if (localTags.equals("<sourceUrl>")) {
      } else if (localTags.equals("</sourceUrl>")) tLocalSourceUrl = content;
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

    return new EDDGridFromDap(
        tDatasetID,
        tAccessibleTo,
        tGraphsAccessibleTo,
        tAccessibleViaWMS,
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
        tLocalSourceUrl,
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
   * @param tLocalSourceUrl the url to which .das or .dds or ... can be added
   * @throws Throwable if trouble
   */
  public EDDGridFromDap(
      String tDatasetID,
      String tAccessibleTo,
      String tGraphsAccessibleTo,
      boolean tAccessibleViaWMS,
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
      String tLocalSourceUrl,
      int tnThreads,
      boolean tDimensionValuesInMemory)
      throws Throwable {

    if (verbose) String2.log("\n*** constructing EDDGridFromDap " + tDatasetID);
    long constructionStartMillis = System.currentTimeMillis();
    String errorInMethod = "Error in EDDGridFromDap(" + tDatasetID + ") constructor:\n";

    // save some of the parameters
    className = "EDDGridFromDap";
    datasetID = tDatasetID;
    setAccessibleTo(tAccessibleTo);
    setGraphsAccessibleTo(tGraphsAccessibleTo);
    if (!tAccessibleViaWMS)
      accessibleViaWMS = String2.canonical(MessageFormat.format(EDStatic.noXxxAr[0], "WMS"));
    onChange = tOnChange;
    fgdcFile = tFgdcFile;
    iso19115File = tIso19115File;
    defaultDataQuery = tDefaultDataQuery;
    defaultGraphQuery = tDefaultGraphQuery;
    if (tAddGlobalAttributes == null) tAddGlobalAttributes = new Attributes();
    addGlobalAttributes = tAddGlobalAttributes;
    addGlobalAttributes.set("sourceUrl", convertToPublicSourceUrl(tLocalSourceUrl));
    localSourceUrl = tLocalSourceUrl;
    setReloadEveryNMinutes(tReloadEveryNMinutes);
    setUpdateEveryNMillis(tUpdateEveryNMillis);
    nThreads = tnThreads; // interpret invalid values (like -1) as EDStatic.nGridThreads
    dimensionValuesInMemory = tDimensionValuesInMemory;

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

    // open the connection to the opendap source
    // Design decision: this doesn't use ucar.nc2.dt.GridDataSet
    //  because GridDataSet determines axes via _CoordinateAxisType (or similar) metadata
    //  which most datasets we use don't have yet.
    //  One could certainly write another class that did use ucar.nc2.dt.GridDataSet.
    DConnect dConnect = null;
    if (quickRestartAttributes == null)
      dConnect = new DConnect(localSourceUrl, acceptDeflate, 1, 1);

    // DAS
    byte dasBytes[] =
        quickRestartAttributes == null
            ? SSR.getUrlResponseBytes(localSourceUrl + ".das")
            : // has timeout and descriptive error
            ((ByteArray) quickRestartAttributes.get("dasBytes")).toArray();
    DAS das = new DAS();
    // String2.log("\n***DAS=");
    // String2.log(String2.annotatedString(new String(dasBytes)));
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
    if (combinedGlobalAttributes.getString("cdm_data_type") == null)
      combinedGlobalAttributes.add("cdm_data_type", "Grid");

    // create dataVariables[]
    dataVariables = new EDV[tDataVariables.length];
    for (int dv = 0; dv < tDataVariables.length; dv++) {
      String tDataSourceName = (String) tDataVariables[dv][0];
      String tDataDestName = (String) tDataVariables[dv][1];
      if (tDataDestName == null || tDataDestName.length() == 0) tDataDestName = tDataSourceName;
      Attributes tDataSourceAtts = new Attributes();
      OpendapHelper.getAttributes(das, tDataSourceName, tDataSourceAtts);
      Attributes tDataAddAtts = (Attributes) tDataVariables[dv][2];
      if (tDataAddAtts == null) tDataAddAtts = new Attributes();

      // get the variable
      BaseType bt = dds.getVariable(tDataSourceName); // throws Throwable if not found
      DArray mainDArray;
      if (bt instanceof DGrid dgrid)
        mainDArray = (DArray) dgrid.getVar(0); // first element is always main array
      else if (bt instanceof DArray darray) mainDArray = darray;
      else
        throw new RuntimeException(
            "dataVariable="
                + tDataSourceName
                + " must be a DGrid or a DArray ("
                + bt.toString()
                + ").");

      // look at the dimensions
      PrimitiveVector pv = mainDArray.getPrimitiveVector(); // just gets the data type
      // if (reallyVerbose) String2.log(tDataSourceName + " pv=" + pv.toString());
      String dvSourceDataType = PAType.toCohortString(OpendapHelper.getElementPAType(pv));
      // String2.loge(">> dvSourceDataType=" + dvSourceDataType);
      int numDimensions = mainDArray.numDimensions();
      if (dv == 0) {
        axisVariables = new EDVGridAxis[numDimensions];
      } else {
        Test.ensureEqual(
            numDimensions,
            axisVariables.length,
            errorInMethod
                + "nDimensions was different for "
                + "dataVariable#0="
                + axisVariables[0].destinationName()
                + " and "
                + "dataVariable#"
                + dv
                + "="
                + tDataSourceName
                + ".");
      }
      for (int av = 0; av < numDimensions; av++) {

        DArrayDimension dad = mainDArray.getDimension(av);
        String tSourceAxisName = dad.getName();

        // ensure this dimension's name is the same as for the other dataVariables
        // (or as specified in tAxisVariables()[0])
        if (tAxisVariables == null) {
          if (dv > 0)
            Test.ensureEqual(
                tSourceAxisName,
                axisVariables[av].sourceName(),
                errorInMethod
                    + "Observed dimension name doesn't equal "
                    + "expected dimension name for dimension #"
                    + av
                    + " dataVariable#"
                    + dv
                    + "="
                    + tDataSourceName
                    + " (compared to dataVariable#0).");
        } else {
          Test.ensureEqual(
              tSourceAxisName,
              (String) tAxisVariables[av][0],
              errorInMethod
                  + "Observed dimension name doesn't equal "
                  + "expected dimension name for dimension #"
                  + av
                  + " dataVariable#"
                  + dv
                  + "="
                  + tDataSourceName
                  + ".");
        }

        // if dv!=0, nothing new to do, so continue
        if (dv != 0) continue;

        // do dv==0 things: create axisVariables[av]
        Attributes tSourceAttributes = new Attributes();
        PrimitiveArray tSourceValues = null;
        try {
          dds.getVariable(tSourceAxisName); // throws NoSuchVariableException
          OpendapHelper.getAttributes(das, tSourceAxisName, tSourceAttributes);
          tSourceValues =
              quickRestartAttributes == null
                  ? OpendapHelper.getPrimitiveArray(dConnect, "?" + tSourceAxisName)
                  : quickRestartAttributes.get(
                      "sourceValues_" + String2.encodeVariableNameSafe(tSourceAxisName));
          if (tSourceValues == null) throw new NoSuchVariableException(tSourceAxisName);
          if (reallyVerbose) {
            int nsv = tSourceValues.size();
            String2.log(
                "    "
                    + tSourceAxisName
                    + " source values #0="
                    + tSourceValues.getString(0)
                    + " #"
                    + (nsv - 1)
                    + "="
                    + tSourceValues.getString(nsv - 1));
          }
        } catch (NoSuchVariableException nsve) {
          // this occurs if no corresponding variable; ignore it
          // make tSourceValues 0..dimensionSize-1
          int dadSize1 = dad.getSize() - 1;
          tSourceValues =
              av > 0 && dadSize1 < 32000
                  ? // av==0 -> intArray is useful for incremental update
                  new ShortArray(0, dadSize1)
                  : new IntArray(0, dadSize1);
          tSourceAttributes.add(
              "units", "count"); // "count" is udunits;  "index" isn't, but better?
          if (reallyVerbose)
            String2.log(
                "    " + tSourceAxisName + " not found.  So made from indices 0 - " + dadSize1);
        } // but other exceptions aren't caught

        Attributes tAddAttributes =
            tAxisVariables == null ? new Attributes() : (Attributes) tAxisVariables[av][2];

        String tDestinationAxisName =
            tAxisVariables == null ? null : (String) tAxisVariables[av][1];
        if (tDestinationAxisName == null || tDestinationAxisName.trim().length() == 0)
          tDestinationAxisName = tSourceAxisName;

        // if _Unsigned=true or false, change tSourceType
        if (tAddAttributes == null) tAddAttributes = new Attributes();
        tSourceValues =
            Attributes.adjustSourceType(tSourceValues, tSourceAttributes, tAddAttributes);

        // make the axisVariable
        axisVariables[av] =
            makeAxisVariable(
                tDatasetID,
                av,
                tSourceAxisName,
                tDestinationAxisName,
                tSourceAttributes,
                tAddAttributes,
                tSourceValues);
      }

      // if _Unsigned=true or false, change tSourceType
      dvSourceDataType =
          Attributes.adjustSourceType(dvSourceDataType, tDataSourceAtts, tDataAddAtts);

      // create the EDV dataVariable
      if (tDataDestName.equals(EDV.TIME_NAME))
        throw new RuntimeException(
            errorInMethod + "No EDDGrid dataVariable may have destinationName=" + EDV.TIME_NAME);
      else if (EDVTime.hasTimeUnits(tDataSourceAtts, tDataAddAtts))
        dataVariables[dv] =
            new EDVTimeStamp(
                datasetID,
                tDataSourceName,
                tDataDestName,
                tDataSourceAtts,
                tDataAddAtts,
                dvSourceDataType);
      else
        dataVariables[dv] =
            new EDV(
                datasetID,
                tDataSourceName,
                tDataDestName,
                tDataSourceAtts,
                tDataAddAtts,
                dvSourceDataType,
                PAOne.fromDouble(Double.NaN),
                PAOne.fromDouble(Double.NaN)); // hard to get min and max
      dataVariables[dv].extractAndSetActualRange();
      // String2.log(">> EDDGridFromDap construct " + tDataDestName + " type=" + dvSourceDataType);
    }

    // ensure the setup is valid
    ensureValid();

    // save quickRestart info
    if (quickRestartAttributes == null) { // i.e., there is new info
      try {
        quickRestartAttributes = new Attributes();
        quickRestartAttributes.set("creationTimeMillis", "" + creationTimeMillis);
        quickRestartAttributes.set("dasBytes", new ByteArray(dasBytes));
        quickRestartAttributes.set("ddsBytes", new ByteArray(ddsBytes));
        for (int av = 0; av < axisVariables.length; av++) {
          quickRestartAttributes.set(
              "sourceValues_" + String2.encodeVariableNameSafe(axisVariables[av].sourceName()),
              axisVariables[av].sourceValues());
        }
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
              + "\n*** EDDGridFromDap "
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
   * This does the actual incremental update of this dataset (i.e., for real time datasets).
   * EDDGridFromDap's version deals with the leftmost axis growing.
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

    // read dds
    DConnect dConnect = new DConnect(localSourceUrl, acceptDeflate, 1, 1);
    byte ddsBytes[] = SSR.getUrlResponseBytes(localSourceUrl + ".dds");
    DDS dds = new DDS();
    dds.parse(new ByteArrayInputStream(ddsBytes));

    // has edvga[0] changed size?
    EDVGridAxis edvga = axisVariables[0];
    EDVTimeStampGridAxis edvtsga = edvga instanceof EDVTimeStampGridAxis t ? t : null;
    PrimitiveArray oldValues = edvga.sourceValues();
    int oldSize = oldValues.size();

    // get mainDArray
    BaseType bt = dds.getVariable(dataVariables[0].sourceName()); // throws NoSuchVariableException
    DArray mainDArray = null;
    if (bt instanceof DGrid dgrid) {
      mainDArray = (DArray) dgrid.getVar(0); // first element is always main array
    } else if (bt instanceof DArray darray) {
      mainDArray = darray;
    } else {
      String2.log(
          msg
              + String2.ERROR
              + ": Unexpected "
              + dataVariables[0].destinationName()
              + " source type="
              + bt.getTypeName()
              + ". So I called requestReloadASAP().");
      // requestReloadASAP()+WaitThenTryAgain might lead to endless cycle of full reloads
      requestReloadASAP();
      return false;
    }

    // get the leftmost dimension
    DArrayDimension dad = mainDArray.getDimension(0);
    int newSize = dad.getSize();
    if (newSize < oldSize)
      throw new WaitThenTryAgainException(
          EDStatic.simpleBilingual(language, EDStatic.waitThenTryAgainAr)
              + "\n("
              + msg
              + "["
              + edvga.destinationName()
              + "] newSize="
              + newSize
              + " < oldSize="
              + oldSize
              + ")");
    if (newSize == oldSize) {
      if (reallyVerbose) String2.log(msg + "no change to leftmost dimension");
      return false; // finally{} below sets lastUpdate = startUpdateMillis
    }

    // newSize > oldSize, get last old value (for testing below) and new values
    PrimitiveArray newValues = null;
    if (edvga.sourceDataPAType() == PAType.INT
        && // not a perfect test
        "count".equals(edvga.sourceAttributes().getString("units")))
      newValues = new IntArray(oldSize - 1, newSize - 1); // 0 based
    else {
      try {
        newValues =
            OpendapHelper.getPrimitiveArray(
                dConnect,
                "?" + edvga.sourceName() + "[" + (oldSize - 1) + ":" + (newSize - 1) + "]");
      } catch (NoSuchVariableException nsve) {
        // hopefully avoided by testing for units=count and int datatype above
        String2.log(
            msg
                + "caught NoSuchVariableException for sourceName="
                + edvga.sourceName()
                + ". Using index numbers.");
        newValues = new IntArray(oldSize - 1, newSize - 1); // 0 based
      } // but other exceptions aren't caught
    }

    // ensure newValues is valid
    if (newValues == null || newValues.size() < (newSize - oldSize + 1)) {
      String2.log(
          msg
              + String2.ERROR
              + ": Too few "
              + edvga.destinationName()
              + " values were received (got="
              + (newValues == null ? "null" : "" + (newValues.size() - 1))
              + "expected="
              + (newSize - oldSize)
              + ").");
      return false;
    }
    if (oldValues.elementType() != newValues.elementType()) // they're canonical, so != works
    throw new WaitThenTryAgainException(
          EDStatic.simpleBilingual(language, EDStatic.waitThenTryAgainAr)
              + "\n("
              + msg
              + edvga.destinationName()
              + " dataType changed: "
              + " new="
              + newValues.elementTypeString()
              + " != old="
              + oldValues.elementTypeString()
              + ")");

    // ensure last old value is unchanged
    if (oldValues.getDouble(oldSize - 1) != newValues.getDouble(0)) // they should be exactly equal
    throw new WaitThenTryAgainException(
          EDStatic.simpleBilingual(language, EDStatic.waitThenTryAgainAr)
              + "\n("
              + msg
              + edvga.destinationName()
              + "["
              + (oldSize - 1)
              + "] changed!  old="
              + oldValues.getDouble(oldSize - 1)
              + " != new="
              + newValues.getDouble(0));

    // prepare changes to update the dataset
    PAOne newMin = new PAOne(oldValues, 0);
    PAOne newMax = new PAOne(newValues, newValues.size() - 1);
    if (edvtsga != null) {
      newMin = PAOne.fromDouble(edvtsga.sourceTimeToEpochSeconds(newMin.getDouble()));
      newMax = PAOne.fromDouble(edvtsga.sourceTimeToEpochSeconds(newMax.getDouble()));
    } else if (edvga.scaleAddOffset()) {
      newMin = PAOne.fromDouble(newMin.getDouble() * edvga.scaleFactor() + edvga.addOffset());
      newMax = PAOne.fromDouble(newMax.getDouble() * edvga.scaleFactor() + edvga.addOffset());
    }

    // first, calculate newAverageSpacing (destination units, will be negative if isDescending)
    double newAverageSpacing = (newMax.getDouble() - newMin.getDouble()) / (newSize - 1);

    // second, test for min>max after extractScaleAddOffset, since order may have changed
    if (newMin.compareTo(newMax) > 0) {
      PAOne d = newMin;
      newMin = newMax;
      newMax = d;
    }

    // test isAscending  (having last old value is essential)
    String error = edvga.isAscending() ? newValues.isAscending() : newValues.isDescending();
    if (error.length() > 0)
      throw new WaitThenTryAgainException(
          EDStatic.simpleBilingual(language, EDStatic.waitThenTryAgainAr)
              + "\n("
              + edvga.destinationName()
              + " was "
              + (edvga.isAscending() ? "a" : "de")
              + "scending, but the newest values aren't ("
              + error
              + ").)");

    // if was isEvenlySpaced, test that new values are and have same averageSpacing
    // (having last old value is essential)
    boolean newIsEvenlySpaced = edvga.isEvenlySpaced(); // here, this is actually oldIsEvenlySpaced
    if (newIsEvenlySpaced) {
      error = newValues.isEvenlySpaced();
      if (error.length() > 0) {
        String2.log(
            msg
                + "changing "
                + edvga.destinationName()
                + ".isEvenlySpaced from true to false: "
                + error);
        newIsEvenlySpaced = false;

        // new spacing != old spacing ?  (precision=5, but times will be exact)
      } else if (!Math2.almostEqual(5, newAverageSpacing, edvga.averageSpacing())) {
        String2.log(
            msg
                + "changing "
                + edvga.destinationName()
                + ".isEvenlySpaced from true to false: newSpacing="
                + newAverageSpacing
                + " oldSpacing="
                + edvga.averageSpacing());
        newIsEvenlySpaced = false;
      }
    }

    // remove the last old value from newValues
    newValues.remove(0);

    // ensureCapacity of oldValues (may take time)
    oldValues.ensureCapacity(newSize); // so oldValues.append below is as fast as possible

    // right before making changes, make doubly sure another thread hasn't already (IMPERFECT TEST)
    if (oldValues.size() != oldSize) {
      String2.log(
          msg
              + "changes abandoned.  "
              + edvga.destinationName()
              + ".size changed (new="
              + oldValues.size()
              + " != old="
              + oldSize
              + ").  (By update() in another thread?)");
      return false;
    }

    // Swap changes into place quickly to minimize problems.  Better if changes were atomic.
    // Order of changes is important.
    // Other threads may be affected by some values being updated before others.
    // This is an imperfect alternative to synchronizing all uses of this dataset (which is far
    // worse).
    oldValues.append(
        newValues); // should be fast, and new size set at end to minimize concurrency problems
    edvga.setDestinationMinMax(newMin, newMax);
    edvga.setIsEvenlySpaced(newIsEvenlySpaced);
    edvga.initializeAverageSpacingAndCoarseMinMax();
    edvga.setActualRangeFromDestinationMinMax();
    if (edvga instanceof EDVTimeGridAxis)
      combinedGlobalAttributes.set(
          "time_coverage_end",
          Calendar2.epochSecondsToLimitedIsoStringT(
              edvga.combinedAttributes().getString(EDV.TIME_PRECISION), newMax.getDouble(), ""));
    edvga.clearSliderCsvValues(); // do last, to force recreation next time needed

    updateCount++;
    long thisTime = System.currentTimeMillis() - startUpdateMillis;
    cumulativeUpdateTime += thisTime;
    if (reallyVerbose)
      String2.log(
          msg
              + "succeeded. "
              + Calendar2.getCurrentISODateTimeStringLocalTZ()
              + " nValuesAdded="
              + newValues.size()
              + " time="
              + thisTime
              + "ms updateCount="
              + updateCount
              + " avgTime="
              + (cumulativeUpdateTime / updateCount)
              + "ms");
    return true;
  }

  /**
   * This makes a sibling dataset, based on the new sourceUrl.
   *
   * @param tLocalSourceUrl
   * @param firstAxisToMatch If 0, this tests if sourceValues for axis-variable #0+ are same. If 1,
   *     this tests if sourceValues for axis-variable #1+ are same.
   * @param shareInfo if true, this ensures that the sibling's axis and data variables are basically
   *     the same as this datasets, and then makes the new dataset point to this instance's data
   *     structures to save memory. (AxisVariable #0 isn't duplicated.) Saving memory is important
   *     if there are 1000's of siblings in ERDDAP.
   * @return EDDGrid
   * @throws Throwable if trouble (e.g., try to shareInfo, but datasets not similar)
   */
  @Override
  public EDDGrid sibling(
      String tLocalSourceUrl, int firstAxisToMatch, int matchAxisNDigits, boolean shareInfo)
      throws Throwable {
    if (verbose) String2.log("EDDGridFromDap.sibling " + tLocalSourceUrl);

    int nAv = axisVariables.length;
    Object tAxisVariables[][] = new Object[nAv][3];
    for (int av = 0; av < nAv; av++) {
      tAxisVariables[av][0] = axisVariables[av].sourceName();
      tAxisVariables[av][1] = axisVariables[av].destinationName();
      tAxisVariables[av][2] = axisVariables[av].addAttributes();
    }
    // String2.pressEnterToContinue("\nsibling axis0 addAtts=\n" +
    // axisVariables[0].addAttributes());

    int nDv = dataVariables.length;
    Object tDataVariables[][] = new Object[nDv][3];
    for (int dv = 0; dv < nDv; dv++) {
      tDataVariables[dv][0] = dataVariables[dv].sourceName();
      tDataVariables[dv][1] = dataVariables[dv].destinationName();
      tDataVariables[dv][2] = dataVariables[dv].addAttributes();
    }

    // need a unique datasetID for sibling
    //  so cached .das .dds axis values are stored separately.
    // So make tDatasetID by putting md5Hex12 in the middle of original datasetID
    //  so beginning and ending for tDatasetID are same as original.
    int po = datasetID.length() / 2;
    String tDatasetID =
        datasetID.substring(0, po)
            + "_"
            + String2.md5Hex12(tLocalSourceUrl)
            + "_"
            + datasetID.substring(po);

    // make the sibling
    EDDGridFromDap newEDDGrid =
        new EDDGridFromDap(
            tDatasetID,
            String2.toSSVString(accessibleTo),
            "auto",
            false, // accessibleViaWMS
            shareInfo ? onChange : (StringArray) onChange.clone(),
            "",
            "",
            "",
            "", // fgdc, iso19115, defaultDataQuery, defaultGraphQuery,
            addGlobalAttributes,
            tAxisVariables,
            tDataVariables,
            getReloadEveryNMinutes(),
            getUpdateEveryNMillis(),
            tLocalSourceUrl,
            nThreads,
            dimensionValuesInMemory);

    // if shareInfo, point to same internal data
    if (shareInfo) {

      // ensure similar
      boolean testAV0 = false;
      String results = similar(newEDDGrid, firstAxisToMatch, matchAxisNDigits, testAV0);
      if (results.length() > 0) throw new SimpleException("Error in EDDGrid.sibling: " + results);

      // shareInfo
      for (int av = 1; av < nAv; av++) // not av0
      newEDDGrid.axisVariables()[av] = axisVariables[av];
      newEDDGrid.dataVariables = dataVariables;

      // shareInfo  (the EDDGrid variables)
      newEDDGrid.axisVariableSourceNames = axisVariableSourceNames(); // () makes the array
      newEDDGrid.axisVariableDestinationNames = axisVariableDestinationNames();

      // shareInfo  (the EDD variables)
      newEDDGrid.dataVariableSourceNames = dataVariableSourceNames();
      newEDDGrid.dataVariableDestinationNames = dataVariableDestinationNames();
      newEDDGrid.title = title();
      newEDDGrid.summary = summary();
      newEDDGrid.institution = institution();
      newEDDGrid.infoUrl = infoUrl();
      newEDDGrid.cdmDataType = cdmDataType();
      newEDDGrid.searchBytes = searchBytes();
      // not sourceUrl, which will be different
      newEDDGrid.sourceGlobalAttributes = sourceGlobalAttributes();
      newEDDGrid.addGlobalAttributes = addGlobalAttributes();
      newEDDGrid.combinedGlobalAttributes = combinedGlobalAttributes();
    }

    return newEDDGrid;
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

    // build String form of the constraint
    // String errorInMethod = "Error in EDDGridFromDap.getSourceData for " + datasetID + ": ";
    String constraint = buildDapArrayQuery(tConstraints);

    DConnect dConnect = new DConnect(localSourceUrl, acceptDeflate, 1, 1);
    PrimitiveArray results[] = new PrimitiveArray[axisVariables.length + tDataVariables.length];
    for (int dv = 0; dv < tDataVariables.length; dv++) {
      // ???why not get all the dataVariables at once?
      // thredds has (and other servers may have) limits to the size of a given request
      // so breaking into parts avoids the problem.

      // get the data
      PrimitiveArray pa[] = null;
      try {
        pa =
            OpendapHelper.getPrimitiveArrays(
                dConnect, "?" + tDataVariables[dv].sourceName() + constraint);
      } catch (Throwable t) {
        EDStatic.rethrowClientAbortException(t); // first thing in catch{}

        // if OutOfMemoryError or too much data, rethrow t
        String tToString = t.toString();
        if (Thread.currentThread().isInterrupted()
            || t instanceof InterruptedException
            || t instanceof OutOfMemoryError
            || tToString.indexOf(Math2.memoryTooMuchData) >= 0
            || tToString.indexOf(Math2.TooManyOpenFiles) >= 0) throw t;

        String2.log(MustBe.throwableToString(t));
        throw t instanceof WaitThenTryAgainException
            ? t
            : new WaitThenTryAgainException(
                EDStatic.simpleBilingual(language, EDStatic.waitThenTryAgainAr)
                    + "\n("
                    + EDStatic.errorFromDataSource
                    + t.toString()
                    + ")",
                t);
      }

      if (pa.length == 1) {
        // it's a DArray
        if (dv == 0) {
          // GridDataAccessor compares observed and expected axis values
          int av3 = 0;
          for (int av = 0; av < axisVariables.length; av++) {
            results[av] =
                axisVariables[av]
                    .sourceValues()
                    .subset(
                        tConstraints.get(av3),
                        tConstraints.get(av3 + 1),
                        tConstraints.get(av3 + 2));
            av3 += 3;
          }
        }

      } else if (pa.length == axisVariables.length + 1) {
        // it's a DGrid;  test the axes
        if (dv == 0) {
          // GridDataAccessor compares observed and expected axis values
          for (int av = 0; av < axisVariables.length; av++) {
            results[av] = pa[av + 1];
          }
        } else if (pa.length != 1) {
          for (int av = 0; av < axisVariables.length; av++) {
            String tError = results[av].almostEqual(pa[av + 1]);
            if (tError.length() > 0)
              throw new WaitThenTryAgainException(
                  EDStatic.simpleBilingual(language, EDStatic.waitThenTryAgainAr)
                      + "\nDetails: The axis values for dataVariable=0,axis="
                      + av
                      + ")\ndon't equal the axis values for dataVariable="
                      + dv
                      + ",axis="
                      + av
                      + ".\n"
                      + tError);
          }
        }

      } else {
        throw new WaitThenTryAgainException(
            EDStatic.simpleBilingual(language, EDStatic.waitThenTryAgainAr)
                + "\nDetails: An unexpected data structure was returned from the source (size observed="
                + pa.length
                + ", expected="
                + (axisVariables.length + 1)
                + ").");
      }

      // store the grid data
      results[axisVariables.length + dv] = pa[0];
    }
    return results;
  }

  /**
   * This does its best to generate a clean, ready-to-use datasets.xml entry for an EDDGridFromDap.
   * The XML can then be edited by hand and added to the datasets.xml file.
   *
   * <p>If this fails because no Grid or Array variables found, it automatically calls
   * EDDTableFromDapSequence.generateDatasetsXml to see if that works.
   *
   * @param tLocalSourceUrl the base url for the dataset (no extension), e.g.,
   *     "https://thredds1.pfeg.noaa.gov/thredds/dodsC/satellite/BA/ssta/5day"
   * @param das The das for the tLocalSourceUrl, or null.
   * @param dds The dds for the tLocalSourceUrl, or null.
   * @param dimensionNames If not null, only the variables that use these dimensions, in this order,
   *     will be loaded. If it is null, the vars with the most dimensions (found first, if tie) will
   *     be loaded.
   * @param tReloadEveryNMinutes E.g., 1440 for once per day. Use, e.g., 1000000000, for never
   *     reload. Use -1 to have ERDDAP suggest the value based on how recent the last time value is.
   * @param externalAddGlobalAttributes globalAttributes gleaned from external sources, e.g., a
   *     THREDDS catalog.xml file. These have priority over other sourceGlobalAttributes. Okay to
   *     use null if none.
   * @return a suggested chunk of xml for this dataset for use in datasets.xml
   * @throws Throwable if trouble, e.g., if no Grid or Array variables are found. If no trouble,
   *     then a valid dataset.xml chunk has been returned.
   */
  public static String generateDatasetsXml(
      String tLocalSourceUrl,
      DAS das,
      DDS dds,
      String dimensionNames[],
      int tReloadEveryNMinutes,
      Attributes externalAddGlobalAttributes)
      throws Throwable {

    tLocalSourceUrl = EDStatic.updateUrls(tLocalSourceUrl); // http: to https:
    String2.log(
        "\n*** EDDGridFromDap.generateDatasetsXml"
            + "\ntLocalSourceUrl="
            + tLocalSourceUrl
            + "\ndimNames="
            + String2.toCSVString(dimensionNames)
            + " reloadEveryNMinutes="
            + tReloadEveryNMinutes
            + "\nexternalAddGlobalAttributes="
            + externalAddGlobalAttributes);
    String dimensionNamesCsv = String2.toCSSVString(dimensionNames);
    if (dimensionNames != null) String2.log("  dimensionNames=" + dimensionNamesCsv);
    String tPublicSourceUrl = convertToPublicSourceUrl(tLocalSourceUrl);
    if (tLocalSourceUrl.endsWith(".html"))
      tLocalSourceUrl = tLocalSourceUrl.substring(0, tLocalSourceUrl.length() - 5);

    // get DConnect
    long getDasDdsTime = System.currentTimeMillis();
    DConnect dConnect =
        new DConnect(tLocalSourceUrl, acceptDeflate, 1, 1); // open nRetries, data nRetries
    int timeOutMinutes = 5;
    int longTimeOut = (int) (timeOutMinutes * Calendar2.MILLIS_PER_MINUTE);
    int nRetries = 3;
    // String2.log(">nRetries=1"); nRetries = 1;
    for (int tri = 0; tri < nRetries; tri++) {
      try {
        if (das == null) {
          String2.log("getDAS try#" + tri + " (timeout is " + timeOutMinutes + " minutes)");
          das = dConnect.getDAS(longTimeOut);
        }
        break;
      } catch (Throwable t) {
        Math2.sleep(10000); // a good idea for most causes of trouble
        String msg =
            "Error while getting DAS from " + tLocalSourceUrl + ".das .\n" + t.getMessage();
        if (tri < nRetries - 1) {
          String2.log(msg);
        } else {
          String2.log(
              "getDasDds failed. time=" + (System.currentTimeMillis() - getDasDdsTime) + "ms");
          throw new SimpleException(msg, t);
        }
      }
    }

    for (int tri = 0; tri < nRetries; tri++) {
      try {
        if (dds == null) {
          String2.log("getDDS try#" + tri + " (timeout is " + timeOutMinutes + " minutes)");
          dds = dConnect.getDDS(longTimeOut);
        }
        break;
      } catch (Throwable t) {
        Math2.sleep(10000); // a good idea for most causes of trouble
        String msg =
            "Error while getting DDS from " + tLocalSourceUrl + ".dds .\n" + t.getMessage();
        if (tri < nRetries - 1) {
          String2.log(msg);
        } else {
          String2.log(
              "getDasDds failed. time=" + (System.currentTimeMillis() - getDasDdsTime) + "ms");
          throw new SimpleException(msg, t);
        }
      }
    }
    String2.log("getDasDds succeeded. time=" + (System.currentTimeMillis() - getDasDdsTime) + "ms");

    // create tables to hold info
    Table axisSourceTable = new Table();
    Table dataSourceTable = new Table();
    Table axisAddTable = new Table();
    Table dataAddTable = new Table();

    // get source global attributes
    OpendapHelper.getAttributes(das, "GLOBAL", axisSourceTable.globalAttributes());

    // read through the variables[]
    HashSet<String> dimensionNameCsvsFound = new HashSet();
    Enumeration vars = dds.getVariables();
    StringArray varNames = new StringArray();
    StringBuilder results = new StringBuilder();
    // if dimensionName!=null, this notes if a var with another dimension combo was found
    boolean otherComboFound = false;
    String sourceDimensionNamesInBrackets = null;
    String destDimensionNamesInBrackets = null;
    Attributes gridMappingAtts = null;
    NEXT_VAR:
    while (vars.hasMoreElements()) {
      BaseType bt = (BaseType) vars.nextElement();
      String dName = bt.getName();
      varNames.add(dName);

      Attributes sourceAtts = new Attributes();
      try {
        OpendapHelper.getAttributes(das, dName, sourceAtts);
      } catch (Throwable t) {
        // e.g., ignore exception for dimension without corresponding coordinate variable
      }

      // Is this the pseudo-data var with CF grid_mapping (projection) information?
      if (gridMappingAtts == null) gridMappingAtts = NcHelper.getGridMappingAtts(sourceAtts);

      // ensure it is a DGrid or DArray
      DArray mainDArray;
      if (bt instanceof DGrid dgrid)
        mainDArray =
            (DArray) dgrid.getVariables().nextElement(); // first element is always main array
      else if (bt instanceof DArray darray) mainDArray = darray;
      else continue;

      // if it's a coordinate variable, skip it
      int numDimensions = mainDArray.numDimensions();
      if (numDimensions == 1 && mainDArray.getDimension(0).getName().equals(dName)) continue;

      // reduce numDimensions by 1 if String var
      PrimitiveVector pv = mainDArray.getPrimitiveVector(); // just gets the data type
      String dvSourceDataType = PAType.toCohortString(OpendapHelper.getElementPAType(pv));
      if (dvSourceDataType.equals("String")) numDimensions--;

      // skip if numDimensions == 0
      if (numDimensions == 0) continue;

      // skip if combo is 1D bnds=bounds info
      if (numDimensions == 1) {
        String tName = mainDArray.getDimension(0).getName();
        if (tName == null
            || tName.endsWith("bnds")
            || tName.endsWith("bounds")
            || dName.equals("Number_of_Lines")
            || dName.equals("Number_of_Columns")) continue;
      }
      // skip if combo is 2D bnds=bounds info  (bnds, time_bnds, etc)
      if (numDimensions == 2) {
        String tName = mainDArray.getDimension(1).getName();
        if (tName == null || tName.endsWith("bnds") || tName.endsWith("bounds")) continue;
      }
      // skip if combo is 3D [][colorindex][rgb]  (or uppercase)
      if (numDimensions == 3
          && "colorindex".equalsIgnoreCase(mainDArray.getDimension(1).getName())
          && "rgb".equalsIgnoreCase(mainDArray.getDimension(2).getName())) continue;

      // skip if varName endsWith("bnds")
      if (dName.endsWith("bnds") || dName.endsWith("bounds")) continue;

      // if dimensionNames wasn't specified, use this method call to just look for dimensionName
      // combos
      if (dimensionNames == null) {
        // has this combo of dimensionNames been seen?
        String tDimensionNames[] = new String[numDimensions];
        for (int av = 0; av < numDimensions; av++)
          tDimensionNames[av] = mainDArray.getDimension(av).getName();
        String dimCsv = String2.toCSSVString(tDimensionNames);
        boolean alreadyExisted = !dimensionNameCsvsFound.add(dimCsv);
        if (reallyVerbose)
          String2.log(
              "  var="
                  + String2.left(dName, 12)
                  + String2.left(" dims=\"" + dimCsv + "\"", 50)
                  + " alreadyExisted="
                  + alreadyExisted);
        if (!alreadyExisted) {
          // It shouldn't fail. But if it does, keep going.
          try {
            results.append(
                generateDatasetsXml(
                    tLocalSourceUrl,
                    das,
                    dds,
                    tDimensionNames,
                    tReloadEveryNMinutes,
                    externalAddGlobalAttributes));
          } catch (Throwable t) {
            String2.log(
                "ERROR: Unexpected error in generateDatasetsXml for dimCsv=\""
                    + dimCsv
                    + "\"\nfor tLocalSourceUrl="
                    + tLocalSourceUrl
                    + "\n"
                    + MustBe.throwableToString(t));
          }
        }
        continue;
      }

      // if dimensionNames was specified, ensure current dimension names match it
      if (dimensionNames.length != numDimensions) {
        otherComboFound = true;
        continue NEXT_VAR;
      }
      for (int av = 0; av < numDimensions; av++) {
        DArrayDimension dad = mainDArray.getDimension(av);
        if (!dimensionNames[av].equals(dad.getName())) {
          otherComboFound = true;
          continue NEXT_VAR;
        }
      }
      // and if all ok, it falls through and continues

      // first data variable found? create axis tables
      if (axisSourceTable.nColumns() == 0) {
        StringBuilder sourceNamesInBrackets = new StringBuilder();
        for (int av = 0; av < numDimensions; av++) {
          DArrayDimension dad = mainDArray.getDimension(av);
          String aName = dad.getName();
          Attributes aSourceAtts = new Attributes();
          try {
            OpendapHelper.getAttributes(das, aName, aSourceAtts);
          } catch (Throwable t) {
            // e.g., ignore exception for dimension without corresponding coordinate variable
          }
          Attributes addAtts =
              makeReadyToUseAddVariableAttributesForDatasetsXml(
                  axisSourceTable.globalAttributes(),
                  aSourceAtts,
                  null,
                  aName,
                  true, // tryToAddStandardName
                  false,
                  true); // addColorBarMinMax, tryToFindLLAT
          axisSourceTable.addColumn(
              axisSourceTable.nColumns(),
              aName,
              new DoubleArray(),
              aSourceAtts); // type doesn't matter here
          axisAddTable.addColumn(
              axisAddTable.nColumns(),
              aName,
              new DoubleArray(),
              addAtts); // type doesn't matter here

          // accumulate namesInBrackets
          sourceNamesInBrackets.append("[" + aName + "]");
        }
        sourceDimensionNamesInBrackets = sourceNamesInBrackets.toString();
      }

      // add the data variable to dataAddTable
      Attributes addAtts =
          makeReadyToUseAddVariableAttributesForDatasetsXml(
              axisSourceTable.globalAttributes(),
              sourceAtts,
              null,
              dName,
              !dvSourceDataType.equals("String"), // tryToAddStandardName
              !dvSourceDataType.equals("String"), // addColorBarMinMax
              false); // tryToFindLLAT
      if (tLocalSourceUrl.indexOf("ncep") >= 0 && tLocalSourceUrl.indexOf("reanalysis") >= 0)
        addAtts.add("drawLandMask", "under");

      dataSourceTable.addColumn(
          dataSourceTable.nColumns(),
          dName,
          new DoubleArray(),
          sourceAtts); // type doesn't matter here
      dataAddTable.addColumn(
          dataAddTable.nColumns(), dName, new DoubleArray(), addAtts); // type doesn't matter here

      // Don't call because sourcePA not available:
      // add missing_value and/or _FillValue if needed
      // addMvFvAttsIfNeeded(dName, sourcePA, sourceAtts, addAtts);

    }

    // if dimensionNames wasn't specified, this is controller, so were're done
    if (dimensionNames == null) {
      // success?
      if (dimensionNameCsvsFound.size() > 0) return results.toString();

      try {
        // see if it is a DAP sequence dataset
        if (verbose)
          String2.log(
              "!!! No Grid or Array variables found, "
                  + "so ERDDAP will check if the dataset is a DAP sequence ...");
        return EDDTableFromDapSequence.generateDatasetsXml(
            tLocalSourceUrl, tReloadEveryNMinutes, externalAddGlobalAttributes);

      } catch (Throwable t) {
        // if EDDTableFromDapSequece throws exception, then throw exception (below) for original
        // problem
      }
    }

    // ensure that variables with the dimensionNames were found
    if (axisAddTable.nColumns() == 0 || dataAddTable.nColumns() == 0) {
      throw new SimpleException(
          "No Grid or Array variables with dimensions=\""
              + dimensionNamesCsv
              + "\" were found for "
              + tLocalSourceUrl
              + ".dds.");
    }

    // ***Here down, we know dimensionNames != null

    // tryToFindLLAT
    tryToFindLLAT(axisSourceTable, axisAddTable); // just axisTables
    ensureValidNames(dataSourceTable, dataAddTable);

    // *** after data variables known, improve global attributes in axisAddTable
    axisAddTable
        .globalAttributes()
        .set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                axisSourceTable.globalAttributes(),
                "Grid", // another cdm type could be better; this is ok
                tLocalSourceUrl,
                externalAddGlobalAttributes,
                EDD.chopUpCsvAndAdd(
                    axisAddTable.getColumnNamesCSVString(),
                    suggestKeywords(dataSourceTable, dataAddTable))));
    if (gridMappingAtts != null) axisAddTable.globalAttributes().add(gridMappingAtts);

    // if otherComboFound, add dimensionNameInBrackets to title and use to make datasetID
    String tDatasetID = suggestDatasetID(tPublicSourceUrl);
    if (otherComboFound) {

      // change title
      String tTitle = axisAddTable.globalAttributes().getString("title");
      if (tTitle == null) tTitle = axisSourceTable.globalAttributes().getString("title");
      axisAddTable.globalAttributes().set("title", tTitle + " " + sourceDimensionNamesInBrackets);

      // change tDatasetID
      // DON'T CHANGE THIS! else id's will change in various erddaps
      // (but changed from destDimNames 2013-01-09 because destDimNames were sometimes duplicated)
      tDatasetID = suggestDatasetID(tPublicSourceUrl + "?" + sourceDimensionNamesInBrackets);
    }

    // read all dimension values and ensure sorted
    // EDVGridAxis requires this. Might as well check here.
    int nav = axisSourceTable.nColumns();
    PrimitiveArray axisPAs[] = new PrimitiveArray[nav];
    int nit = 3;
    for (int av = 0; av < nav; av++) {
      String tSourceName = axisSourceTable.getColumnName(av);
      if (varNames.indexOf(tSourceName) < 0) {
        String2.log(
            "  skipping dimension=" + tSourceName + " because there is no corresponding variable.");
        continue;
      }
      axisPAs[av] = null; // explicit
      for (int it = 0; it < nit; it++) {
        try {
          axisPAs[av] = OpendapHelper.getPrimitiveArray(dConnect, "?" + tSourceName);
          break; // success
        } catch (Throwable t) {
          if (it < nit - 1) {
            String2.log(
                "WARNING #"
                    + it
                    + ": unable to get axis="
                    + tSourceName
                    + " values\n  from "
                    + tLocalSourceUrl
                    + "\n  "
                    + t.getMessage()
                    + "\n  Sleeping for 60 seconds...");
            Math2.sleep(60000);
          } else {
            Math2.sleep(60000);
            throw new SimpleException(
                String2.ERROR
                    + ": unable to get axis="
                    + tSourceName
                    + " values\n  from "
                    + tLocalSourceUrl
                    + "\n"
                    + MustBe.throwableToString(t));
          }
        }
      }

      // ensure sorted
      String error = axisPAs[av].isAscending();
      if (error.length() > 0) {
        String error2 = axisPAs[av].isDescending();
        if (error2.length() > 0)
          throw new SimpleException(
              String2.ERROR
                  + ": unsorted axis: "
                  + tLocalSourceUrl
                  + "?"
                  + tSourceName
                  + " : "
                  + error
                  + "  "
                  + error2);
      }
      // ensure no duplicates
      StringBuilder sb = new StringBuilder();
      if (axisPAs[av].removeDuplicates(false, sb) > 0)
        throw new SimpleException(
            String2.ERROR
                + ": duplicates in axis: "
                + tLocalSourceUrl
                + "?"
                + tSourceName
                + "\n"
                + sb.toString());

      // ensure no missing values or values > 1e20
      double stats[] = axisPAs[av].calculateStats();
      int nmv = Math2.roundToInt(axisPAs[av].size() - stats[PrimitiveArray.STATS_N]);
      if (nmv > 0)
        throw new SimpleException(
            String2.ERROR
                + ": axis has "
                + nmv
                + " missingValue(s)! "
                + tLocalSourceUrl
                + "?"
                + tSourceName);
      double largest =
          Math.max(
              Math.abs(stats[PrimitiveArray.STATS_MIN]), Math.abs(stats[PrimitiveArray.STATS_MAX]));
      if (largest > 1e20)
        throw new SimpleException(
            String2.ERROR
                + ": axis has suspect value (abs()="
                + largest
                + ")! "
                + tLocalSourceUrl
                + "?"
                + tSourceName);

      // if there is a defined _FillValue or missing_value, set it to null
      Attributes tSourceAtts = axisSourceTable.columnAttributes(av);
      Attributes tAddAtts = axisAddTable.columnAttributes(av);
      String ts = tSourceAtts.getString("_FillValue");
      if (ts != null) tAddAtts.set("_FillValue", "null");
      ts = tSourceAtts.getString("missing_value");
      if (ts != null) tAddAtts.set("missing_value", "null");
    }

    // suggestReloadEveryNMinutes and add ", startYear-EndYear" to the title
    String timeUnits = null;
    double es5mo =
        (System.currentTimeMillis() / 1000.0)
            - 150 * Calendar2.SECONDS_PER_DAY; // approximately 150 days ago
    String tTitle =
        getAddOrSourceAtt(
            axisAddTable.globalAttributes(), axisSourceTable.globalAttributes(), "title", null);
    if (tTitle == null) // shouldn't be
    tTitle = "";
    String timeRange = null;
    double latSpacing = Double.NaN; // will be set if found and evenly spaced
    double lonSpacing = Double.NaN;
    String oTestOutOfDate =
        getAddOrSourceAtt(
            axisAddTable.globalAttributes(),
            axisSourceTable.globalAttributes(),
            "testOutOfDate",
            null);
    String tTestOutOfDate = oTestOutOfDate;
    // find time axisVar  (look for units with " since ")
    for (int av = 0; av < nav; av++) {
      String tName = axisAddTable.getColumnName(av);
      Attributes avAddAtts = axisAddTable.columnAttributes(av);
      Attributes avSourceAtts = axisSourceTable.columnAttributes(av);
      String tUnits = getAddOrSourceAtt(avAddAtts, avSourceAtts, "units", null);
      PrimitiveArray pa = axisPAs[av];

      if (EDV.LON_NAME.equals(tName) || EDV.LAT_NAME.equals(tName)) { // tryToFindLLAT was run above
        if (pa.size() > 2 && pa.isEvenlySpaced().length() == 0) {
          double average =
              Math.abs(pa.getDouble(pa.size() - 1) - pa.getDouble(0)) / (pa.size() - 1.0);
          // I've never actually seen a packed axisVar, but...
          double tScale =
              String2.parseDouble(getAddOrSourceAtt(avAddAtts, avSourceAtts, "scale_factor", null));
          double tAddOffset =
              String2.parseDouble(getAddOrSourceAtt(avAddAtts, avSourceAtts, "add_offset", null));
          if (Double.isNaN(tScale)) tScale = 1.0;
          if (Double.isNaN(tAddOffset)) tAddOffset = 0.0;
          average = average * tScale + tAddOffset;
          if (Double.isFinite(average)) {
            if (EDV.LON_NAME.equals(tName)) lonSpacing = average;
            if (EDV.LAT_NAME.equals(tName)) latSpacing = average;
          }
        }
      }

      if (EDV.TIME_NAME.equals(tName)
          && // tryToFindLLAT was run above
          Calendar2.isNumericTimeUnits(
              tUnits)) { // should be clean if has " since ", but not certain

        // parse the " since " units
        String tSourceName = axisSourceTable.getColumnName(av);
        double tBaseFactor[] = null;
        try {
          tBaseFactor = Calendar2.getTimeBaseAndFactor(tUnits); // may throw exception
        } catch (Throwable t) {
          String2.log(
              "WARNING: unable to parse time units="
                  + tUnits
                  + " from axisVar="
                  + tSourceName
                  + ".\n"
                  + MustBe.throwableToString(t));
          // that was probably *the* time var, but trouble, so use default
          if (tReloadEveryNMinutes < 0) tReloadEveryNMinutes = DEFAULT_RELOAD_EVERY_N_MINUTES;
          continue;
        }

        // get the first and last time value
        try {
          double es0 =
              Calendar2.unitsSinceToEpochSeconds(tBaseFactor[0], tBaseFactor[1], pa.getDouble(0));
          double lastTime = pa.getDouble(pa.size() - 1);
          double esLast =
              Calendar2.unitsSinceToEpochSeconds(tBaseFactor[0], tBaseFactor[1], lastTime);
          String iso0 = Calendar2.safeEpochSecondsToIsoStringTZ(es0, "");
          String isoLast = Calendar2.safeEpochSecondsToIsoStringTZ(esLast, "");
          if (verbose) String2.log("timeRange: iso0=" + iso0 + " isoLast=" + isoLast);
          if (timeRange == null
              && // just get first likely one (time usually av=0) (should be only likely one)
              iso0.length() >= 4
              && isoLast.length() >= 4) {
            // add ", startYear-EndYear" to the title
            String tIso0 = iso0.substring(0, 4); // just year
            String tIsoLast = esLast > es5mo ? "present" : isoLast.substring(0, 4);
            if (tTitle.indexOf(tIso0) < 0 && tTitle.indexOf(tIsoLast) < 0)
              timeRange = ", " + tIso0 + (tIso0.equals(tIsoLast) ? "" : "-" + tIsoLast);
          }

          if (tReloadEveryNMinutes < 0) {
            // get suggestedReloadEveryNMinutes
            tReloadEveryNMinutes = suggestReloadEveryNMinutes(esLast);
            String msg =
                "suggestReloadEveryNMinutes="
                    + tReloadEveryNMinutes
                    + ": "
                    + tSourceName
                    + ": "
                    + lastTime
                    + " "
                    + tUnits
                    + " -> "
                    + isoLast;
            if (tReloadEveryNMinutes == DEFAULT_RELOAD_EVERY_N_MINUTES) // i.e., trouble
            String2.log("WARNING: lastTime=" + isoLast + " can't be right.\n" + msg);
            else if (reallyVerbose) String2.log(msg);
          }

          if (!String2.isSomething(tTestOutOfDate)) tTestOutOfDate = suggestTestOutOfDate(esLast);

        } catch (Throwable t) {
          String2.log(
              "WARNING: trouble while evaluating first and last time value from "
                  + tSourceName
                  + ".\n"
                  + MustBe.throwableToString(t));
          // that was probably *the* time var, but trouble, so use default
          if (tReloadEveryNMinutes < 0) tReloadEveryNMinutes = DEFAULT_RELOAD_EVERY_N_MINUTES;
        }
      }
    }
    if (verbose) String2.log("spacing: lat=" + latSpacing + " lon=" + lonSpacing);
    if (String2.isSomething(tTestOutOfDate) && !tTestOutOfDate.equals(oTestOutOfDate))
      axisAddTable.globalAttributes().set("testOutOfDate", tTestOutOfDate);
    if (Double.isFinite(latSpacing)
        && Math2.almostEqual(
            4, latSpacing, lonSpacing)) { // MHchlamday has lat=0.04167631 lon=0.04167149
      String ts = "" + (float) latSpacing;
      if (tTitle.indexOf(ts)
          < 0) { // not ideal. This will fail (in a safe way) if version# (or ...) in title and
        // spacing both equal 1.0.
        tTitle += ", " + ts + "°";
        axisAddTable.globalAttributes().set("title", tTitle);
      }
    }
    if (timeRange != null) {
      // String2.pressEnterToContinue("timeRange=" + timeRange);
      tTitle = tTitle + timeRange;
      axisAddTable.globalAttributes().set("title", tTitle);
    }
    if (tReloadEveryNMinutes < 0) {
      tReloadEveryNMinutes = Calendar2.MINUTES_PER_30DAYS;
      if (reallyVerbose)
        String2.log(
            "suggestReloadEveryNMinutes=-1: no \" since \" units found, "
                + "so using 30-day reloadEveryNMinutes="
                + tReloadEveryNMinutes
                + ".\n");
    }

    // write the information
    results.append(
        "<dataset type=\"EDDGridFromDap\" datasetID=\""
            + tDatasetID
            + "\" active=\"true\">\n"
            + "    <sourceUrl>"
            + XML.encodeAsXML(tLocalSourceUrl)
            + "</sourceUrl>\n"
            + "    <reloadEveryNMinutes>"
            + tReloadEveryNMinutes
            + "</reloadEveryNMinutes>\n");
    results.append(writeAttsForDatasetsXml(false, axisSourceTable.globalAttributes(), "    "));
    results.append(writeAttsForDatasetsXml(true, axisAddTable.globalAttributes(), "    "));

    // last 2 params: includeDataType, questionDestinationName
    results.append(
        writeVariablesForDatasetsXml(axisSourceTable, axisAddTable, "axisVariable", false, false));
    results.append(
        writeVariablesForDatasetsXml(dataSourceTable, dataAddTable, "dataVariable", false, false));
    results.append("</dataset>\n" + "\n");

    String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
    return results.toString();
  }

  /**
   * This calls generateDatasetsXml within try/catch so very safe. Only completely successful xml
   * will be added to results.
   *
   * @param tLocalSourceUrl
   * @param tReloadEveryNMinutes
   * @param externalAddGlobalAttributes
   * @param results to capture the results
   * @param summary captures the summary of what was done.
   * @param indent a string of spaces to be used to indent info added to summary
   * @param datasetSuccessTimes an int[String2.TimeDistributionSize] to capture successful
   *     generateDatasetXml times
   * @param datasetFailureTimes an int[String2.TimeDistributionSize] to capture unsuccessful
   *     generateDatasetXml times
   */
  public static void safelyGenerateDatasetsXml(
      String tLocalSourceUrl,
      int tReloadEveryNMinutes,
      Attributes externalAddGlobalAttributes,
      Writer results,
      StringBuilder summary,
      String indent,
      int datasetSuccessTimes[],
      int datasetFailureTimes[]) {

    long time = System.currentTimeMillis();
    tLocalSourceUrl = EDStatic.updateUrls(tLocalSourceUrl); // http: to https:
    try {
      // append to results  (it should succeed completely, or fail)
      results.write(
          generateDatasetsXml(
              tLocalSourceUrl,
              null,
              null,
              null,
              tReloadEveryNMinutes,
              externalAddGlobalAttributes));
      time = System.currentTimeMillis() - time;
      String2.distributeTime(time, datasetSuccessTimes);
      String ts = indent + tLocalSourceUrl + "  (" + time + " ms)\n";
      summary.append(ts);
      String2.log(ts);

    } catch (Throwable t) {
      String2.log(
          String2.ERROR
              + " in safelyGenerateDatasetsXml\n"
              + "  for tLocalSourceUrl="
              + tLocalSourceUrl
              + "\n"
              + MustBe.throwableToString(t));
      time = System.currentTimeMillis() - time;
      String2.distributeTime(time, datasetFailureTimes);
      String ts =
          indent
              + tLocalSourceUrl
              + "  ("
              + time
              + " ms)\n"
              + indent
              + "  "
              + String2.ERROR
              + ": "
              + String2.replaceAll(MustBe.getShortErrorMessage(t), "\n", "\n  " + indent)
              + "\n";
      summary.append(ts);
      String2.log(ts);
    }
  }

  /**
   * This gets matching datasetURLs from a thredds catalog.
   *
   * @param startUrl
   *     https://thredds1.pfeg.noaa.gov/thredds/catalog/Satellite/aggregsatMH/chla/catalog.xml
   * @param datasetNameRegex e.g. ".*\.nc"
   * @param recursive
   * @throws Exception
   */
  public static StringArray getUrlsFromThreddsCatalog(
      String startUrl, String datasetNameRegex, String pathRegex, String negativePathRegex)
      throws Exception {

    return crawlThreddsCatalog(startUrl, datasetNameRegex, pathRegex, negativePathRegex, null);
  }

  /**
   * This runs generateDatasetsXmlFromThreddsCatalog.
   *
   * @param oResultsFileName If null, the procedure calls Test.displayInBrowser.
   * @param oLocalSourceUrl A complete URL of a thredds catalog xml file in the form it needs to be
   *     called local to this computer (e.g., perhaps numeric ip).
   * @param datasetNameRegex The lowest level name of the dataset must match this, e.g., ".*" for
   *     all dataset Names.
   * @param tReloadEveryNMinutes Recommended: Use -1 to have ERDDAP suggest the value based on how
   *     recent the last time value is.
   */
  public static void generateDatasetsXmlFromThreddsCatalog(
      String oResultsFileName,
      String oLocalSourceUrl,
      String datasetNameRegex,
      String pathRegex,
      String negativePathRegex,
      int tReloadEveryNMinutes)
      throws Throwable {

    if (oLocalSourceUrl == null) throw new RuntimeException("'localSourceUrl' is null.");
    runGenerateDatasetsXmlFromThreddsCatalog(
        oResultsFileName,
        oLocalSourceUrl,
        datasetNameRegex,
        pathRegex,
        negativePathRegex,
        tReloadEveryNMinutes);
  }

  /**
   * This is a low level method used by generate.... and testGenerate... to run
   * generateDatasetsXmlFromThreddsCatalogs.
   *
   * @param oResultsFileName If null, the procedure creates /temp/datasetsDATETIME.xml
   *     /temp/datasetsDATETIME.xml.log.txt and calls SSR.displayInBrowser (so they are displayed in
   *     EditPlus).
   * @param oLocalSourceUrl A complete URL of a thredds catalog xml file. If null, a standard test
   *     will be done.
   * @param datasetNameRegex The lowest level name of the dataset must match this, e.g., ".*" for
   *     all dataset Names.
   * @param tReloadEveryNMinutes e.g., weekly=10080. Recommended: Use -1 to have ERDDAP suggest the
   *     value based on how recent the last time value is.
   */
  public static void runGenerateDatasetsXmlFromThreddsCatalog(
      String oResultsFileName,
      String oLocalSourceUrl,
      String datasetNameRegex,
      String pathRegex,
      String negativePathRegex,
      int tReloadEveryNMinutes)
      throws Throwable {

    String dateTime = Calendar2.getCompactCurrentISODateTimeStringLocal();
    String resultsFileName =
        oResultsFileName == null ? "/temp/datasets" + dateTime + ".xml" : oResultsFileName;

    String logFileName = resultsFileName + ".log.txt";
    String2.setupLog(true, false, logFileName, false, 1000000000);
    String2.log(
        "*** Starting runGenerateDatasetsXmlFromThreddsCatalog "
            + Calendar2.getCurrentISODateTimeStringLocalTZ()
            + "\n"
            + "oLocalSourceUrl="
            + oLocalSourceUrl
            + "\n"
            + "logFile="
            + String2.logFileName()
            + "\n"
            + String2.standardHelpAboutMessage());

    Writer results = File2.getBufferedFileWriterUtf8(resultsFileName);
    try {
      // crawl THREDDS catalog
      crawlThreddsCatalog(
          oLocalSourceUrl == null
              ? "https://thredds1.pfeg.noaa.gov/thredds/catalog/Satellite/aggregsatMH/chla/catalog.xml"
              : oLocalSourceUrl,
          datasetNameRegex,
          pathRegex,
          negativePathRegex,
          results);
    } finally {
      results.close();
    }

    String2.returnLoggingToSystemOut();

    if (oLocalSourceUrl != null) {
      Test.displayInBrowser(resultsFileName);
      Test.displayInBrowser(logFileName);
      return;
    }

    try {
      String resultsAr[] = File2.readFromFileUtf8(resultsFileName);
      String expected =
          // there are several datasets in the resultsAr, but this is the one that changes least
          // frequently (monthly)
          "<dataset type=\"EDDGridFromDap\" datasetID=\"noaa_pfeg_23ee_b161_d427\" active=\"true\">\n"
              + "    <sourceUrl>https://thredds1.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/mday</sourceUrl>\n"
              + "    <reloadEveryNMinutes>(2880|5760|11520|43200)</reloadEveryNMinutes>\n"
              + // 2880 or 5760, rarely 11520, 2014-09 now 43200 because not being updated
              "    <!-- sourceAttributes>\n"
              + "        <att name=\"acknowledgement\">NOAA NESDIS COASTWATCH, NOAA SWFSC ERD</att>\n"
              + "        <att name=\"cdm_data_type\">Grid</att>\n"
              + "        <att name=\"cols\" type=\"int\">8640</att>\n"
              + "        <att name=\"composite\">true</att>\n"
              + "        <att name=\"contributor_name\">NASA GSFC \\(OBPG\\)</att>\n"
              + "        <att name=\"contributor_role\">Source of level 2 data.</att>\n"
              + "        <att name=\"Conventions\">COARDS, CF-1.0, Unidata Dataset Discovery v1.0, CWHDF</att>\n"
              + "        <att name=\"creator_email\">dave.foley@noaa.gov</att>\n"
              + "        <att name=\"creator_name\">NOAA CoastWatch, West Coast Node</att>\n"
              + "        <att name=\"creator_url\">http://coastwatch.pfel.noaa.gov</att>\n"
              + "        <att name=\"cwhdf_version\">3.4</att>\n"
              + "        <att name=\"date_created\">20.{8}Z</att>\n"
              + // changes
              "        <att name=\"date_issued\">20.{8}Z</att>\n"
              + // changes
              "        <att name=\"Easternmost_Easting\" type=\"double\">360.0</att>\n"
              + "        <att name=\"et_affine\" type=\"doubleList\">0.0 0.041676313961565174 0.04167148975575877 0.0 0.0 -90.0</att>\n"
              + "        <att name=\"gctp_datum\" type=\"int\">12</att>\n"
              + "        <att name=\"gctp_parm\" type=\"doubleList\">0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0</att>\n"
              + "        <att name=\"gctp_sys\" type=\"int\">0</att>\n"
              + "        <att name=\"gctp_zone\" type=\"int\">0</att>\n"
              + "        <att name=\"geospatial_lat_max\" type=\"double\">90.0</att>\n"
              + "        <att name=\"geospatial_lat_min\" type=\"double\">-90.0</att>\n"
              + "        <att name=\"geospatial_lat_resolution\" type=\"double\">0.041676313961565174</att>\n"
              + "        <att name=\"geospatial_lat_units\">degrees_north</att>\n"
              + "        <att name=\"geospatial_lon_max\" type=\"double\">360.0</att>\n"
              + "        <att name=\"geospatial_lon_min\" type=\"double\">0.0</att>\n"
              + "        <att name=\"geospatial_lon_resolution\" type=\"double\">0.04167148975575877</att>\n"
              + "        <att name=\"geospatial_lon_units\">degrees_east</att>\n"
              + "        <att name=\"geospatial_vertical_max\" type=\"double\">0.0</att>\n"
              + "        <att name=\"geospatial_vertical_min\" type=\"double\">0.0</att>\n"
              + "        <att name=\"geospatial_vertical_positive\">up</att>\n"
              + "        <att name=\"geospatial_vertical_units\">m</att>\n"
              + "        <att name=\"history\">NASA GSFC \\(OBPG\\)";

      int po = resultsAr[1].indexOf(expected.substring(0, 80));
      int po2 = resultsAr[1].indexOf("<att name=\"history\">NASA GSFC (OBPG)", po + 80);
      String2.log("\npo=" + po + " po2=" + po2 + " results=\n" + resultsAr[1]);
      String2.log(""); // ensure previous is written
      Test.ensureLinesMatch(resultsAr[1].substring(po, po2 + 36), expected, "");

      /*"2010-01-08T00:51:12Z NOAA CoastWatch (West Coast Node) and NOAA SWFSC ERD</att>\n" +
      "        <att name=\"id\">LMHchlaSmday_20091216120000</att>\n" +
      "        <att name=\"institution\">NOAA CoastWatch, West Coast Node</att>\n" +
      "        <att name=\"keywords\">EARTH SCIENCE &gt; Oceans &gt; Ocean Chemistry &gt; Chlorophyll</att>\n" +
      "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
      "        <att name=\"license\">The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.</att>\n" +
      "        <att name=\"naming_authority\">gov.noaa.pfel.coastwatch</att>\n" +
      "        <att name=\"Northernmost_Northing\" type=\"double\">90.0</att>\n" +
      "        <att name=\"origin\">NASA GSFC (G. Feldman)</att>\n" +
      "        <att name=\"pass_date\" type=\"intList\">14549 14550 14551 14552 14553 14554 14555 14556 14557 14558 14559 14560 14561 14562 14563 14564 14565 14566 14567 14568 14569 14570 14571 14572 14573 14574 14575 14576 14577 14578</att>\n" +
      "        <att name=\"polygon_latitude\" type=\"doubleList\">-90.0 90.0 90.0 -90.0 -90.0</att>\n" +
      "        <att name=\"polygon_longitude\" type=\"doubleList\">0.0 0.0 360.0 360.0 0.0</att>\n" +
      "        <att name=\"processing_level\">3</att>\n" +
      "        <att name=\"project\">CoastWatch (http://coastwatch.noaa.gov/)</att>\n" +
      "        <att name=\"projection\">geographic</att>\n" +
      "        <att name=\"projection_type\">mapped</att>\n" +
      "        <att name=\"references\">Aqua/MODIS information: http://oceancolor.gsfc.nasa.gov/ . MODIS information: http://coastwatch.noaa.gov/modis_ocolor_overview.html .</att>\n" +
      "        <att name=\"rows\" type=\"int\">4320</att>\n" +
      "        <att name=\"satellite\">Aqua</att>\n" +
      "        <att name=\"sensor\">MODIS</att>\n" +
      "        <att name=\"source\">satellite observation: Aqua, MODIS</att>\n" +
      "        <att name=\"Southernmost_Northing\" type=\"double\">-90.0</att>\n" +
      "        <att name=\"standard_name_vocabulary\">CF-1.0</att>\n" +
      "        <att name=\"start_time\" type=\"doubleList\">0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0</att>\n" +
      "        <att name=\"summary\">NOAA CoastWatch distributes chlorophyll-a concentration data from NASA&#39;s Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.   This is Science Quality data.</att>\n" +
      "        <att name=\"time_coverage_end\">2010-01-01T00:00:00Z</att>\n" +  //changes
      "        <att name=\"time_coverage_start\">2009-12-01T00:00:00Z</att>\n" +
      "        <att name=\"title\">Chlorophyll-a, Aqua MODIS, NPP, 0.05 degrees, Global, Science Quality</att>\n" +
      "        <att name=\"Westernmost_Easting\" type=\"double\">0.0</att>\n" +
      "    </sourceAttributes -->\n" +
      "    <addAttributes>\n" +
      "        <att name=\"authority\">gov.noaa.pfeg.coastwatch</att>\n" +
      "        <att name=\"Conventions\">COARDS, CF-1.10, CWHDF</att>\n" +
      "        <att name=\"creator_name\">NASA GSFC (G. Feldman)</att>\n" +
      */

      String expected2 =
          "        <att name=\"infoUrl\">https://coastwatch.pfeg.noaa.gov/infog/MH_chla_las.html</att>\n"
              + "        <att name=\"institution\">NOAA CoastWatch WCN</att>\n"
              + "        <att name=\"keywords\">1-day, altitude, aqua, chemistry, chla, chlorophyll, chlorophyll-a, coast, coastwatch, color, concentration, concentration_of_chlorophyll_in_sea_water, data, day, degrees, deprecated, earth, Earth Science &gt; Oceans &gt; Ocean Chemistry &gt; Chlorophyll, global, imaging, latitude, longitude, MHchla, moderate, modis, national, noaa, node, npp, ocean, ocean color, oceans, older, orbiting, partnership, polar, polar-orbiting, quality, resolution, science, science quality, sea, seawater, spectroradiometer, time, version, water, wcn, west</att>\n"
              + "        <att name=\"naming_authority\">gov.noaa.pfeg.coastwatch</att>\n"
              + "        <att name=\"pass_date\">null</att>\n"
              + "        <att name=\"polygon_latitude\">null</att>\n"
              + "        <att name=\"polygon_longitude\">null</att>\n"
              + "        <att name=\"project\">CoastWatch \\(https://coastwatch.noaa.gov/\\)</att>\n"
              + "        <att name=\"publisher_email\">erd.data@noaa.gov</att>\n"
              + "        <att name=\"publisher_name\">NOAA NMFS SWFSC ERD</att>\n"
              + "        <att name=\"publisher_type\">institution</att>\n"
              + "        <att name=\"publisher_url\">https://coastwatch.pfeg.noaa.gov</att>\n"
              + "        <att name=\"references\">Aqua/MODIS information: https://oceancolor.gsfc.nasa.gov/ . MODIS information: https://coastwatch.noaa.gov/modis_ocolor_overview.html .</att>\n"
              + "        <att name=\"rows\">null</att>\n"
              + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
              + "        <att name=\"start_time\">null</att>\n"
              + "        <att name=\"summary\">Chlorophyll-a, Aqua MODIS, National Polar-orbiting Partnership \\(NPP\\), 0.05 degrees, Global, Science Quality. NOAA CoastWatch distributes chlorophyll-a concentration data from NASA&#39;s Aqua Spacecraft. Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer \\(MODIS\\) carried aboard the spacecraft. This is Science Quality data.</att>\n"
              + "        <att name=\"title\">Chlorophyll-a \\(Deprecated Older Version\\), Aqua MODIS, NPP, Global, Science Quality, 1-day, 2003-2013</att>\n"
              + "    </addAttributes>\n"
              + "    <axisVariable>\n"
              + "        <sourceName>time</sourceName>\n"
              + "        <destinationName>time</destinationName>\n"
              + "        <!-- sourceAttributes>\n"
              + "            <att name=\"_CoordinateAxisType\">Time</att>\n"
              + "            <att name=\"actual_range\" type=\"doubleList\">1.3660272E9 .{5,20}</att>\n"
              + // changes
              "            <att name=\"axis\">T</att>\n"
              + "            <att name=\"fraction_digits\" type=\"int\">0</att>\n"
              + "            <att name=\"long_name\">Centered Time</att>\n"
              + "            <att name=\"standard_name\">time</att>\n"
              + "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Time</att>\n"
              + "        </addAttributes>\n"
              + "    </axisVariable>\n"
              + "    <axisVariable>\n"
              + "        <sourceName>altitude</sourceName>\n"
              + "        <destinationName>altitude</destinationName>\n"
              + "        <!-- sourceAttributes>\n"
              + "            <att name=\"_CoordinateAxisType\">Height</att>\n"
              + "            <att name=\"_CoordinateZisPositive\">up</att>\n"
              + "            <att name=\"actual_range\" type=\"doubleList\">0.0 0.0</att>\n"
              + "            <att name=\"axis\">Z</att>\n"
              + "            <att name=\"fraction_digits\" type=\"int\">0</att>\n"
              + "            <att name=\"long_name\">Altitude</att>\n"
              + "            <att name=\"positive\">up</att>\n"
              + "            <att name=\"standard_name\">altitude</att>\n"
              + "            <att name=\"units\">m</att>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Location</att>\n"
              + "        </addAttributes>\n"
              + "    </axisVariable>\n"
              + "    <axisVariable>\n"
              + "        <sourceName>lat</sourceName>\n"
              + "        <destinationName>latitude</destinationName>\n"
              + "        <!-- sourceAttributes>\n"
              + "            <att name=\"_CoordinateAxisType\">Lat</att>\n"
              + "            <att name=\"actual_range\" type=\"doubleList\">-90.0 90.0</att>\n"
              + "            <att name=\"axis\">Y</att>\n"
              + "            <att name=\"coordsys\">geographic</att>\n"
              + "            <att name=\"fraction_digits\" type=\"int\">4</att>\n"
              + "            <att name=\"long_name\">Latitude</att>\n"
              + "            <att name=\"point_spacing\">even</att>\n"
              + "            <att name=\"standard_name\">latitude</att>\n"
              + "            <att name=\"units\">degrees_north</att>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Location</att>\n"
              + "        </addAttributes>\n"
              + "    </axisVariable>\n"
              + "    <axisVariable>\n"
              + "        <sourceName>lon</sourceName>\n"
              + "        <destinationName>longitude</destinationName>\n"
              + "        <!-- sourceAttributes>\n"
              + "            <att name=\"_CoordinateAxisType\">Lon</att>\n"
              + "            <att name=\"actual_range\" type=\"doubleList\">0.0 360.0</att>\n"
              + "            <att name=\"axis\">X</att>\n"
              + "            <att name=\"coordsys\">geographic</att>\n"
              + "            <att name=\"fraction_digits\" type=\"int\">4</att>\n"
              + "            <att name=\"long_name\">Longitude</att>\n"
              + "            <att name=\"point_spacing\">even</att>\n"
              + "            <att name=\"standard_name\">longitude</att>\n"
              + "            <att name=\"units\">degrees_east</att>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"ioos_category\">Location</att>\n"
              + "        </addAttributes>\n"
              + "    </axisVariable>\n"
              + "    <dataVariable>\n"
              + "        <sourceName>MHchla</sourceName>\n"
              + "        <destinationName>MHchla</destinationName>\n"
              + "        <!-- sourceAttributes>\n"
              + "            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n"
              + "            <att name=\"actual_range\" type=\"floatList\">.{1,10} .{1,10}</att>\n"
              + // changes
              "            <att name=\"coordsys\">geographic</att>\n"
              + "            <att name=\"fraction_digits\" type=\"int\">2</att>\n"
              + "            <att name=\"long_name\">Chlorophyll-a, Aqua MODIS, NPP, 0.05 degrees, Global, Science Quality</att>\n"
              + "            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n"
              + "            <att name=\"numberOfObservations\" type=\"int\">\\d{1,20}</att>\n"
              + // changes
              "            <att name=\"percentCoverage\" type=\"double\">.{5,20}</att>\n"
              + // changes
              "            <att name=\"standard_name\">concentration_of_chlorophyll_in_sea_water</att>\n"
              + "            <att name=\"units\">mg m-3</att>\n"
              + "        </sourceAttributes -->\n"
              + "        <addAttributes>\n"
              + "            <att name=\"colorBarMaximum\" type=\"double\">30.0</att>\n"
              + "            <att name=\"colorBarMinimum\" type=\"double\">0.03</att>\n"
              + "            <att name=\"colorBarScale\">Log</att>\n"
              + "            <att name=\"ioos_category\">Ocean Color</att>\n"
              + "            <att name=\"numberOfObservations\">null</att>\n"
              + "            <att name=\"percentCoverage\">null</att>\n"
              + "        </addAttributes>\n"
              + "    </dataVariable>\n"
              + "</dataset>";

      po = resultsAr[1].indexOf(expected2.substring(0, 80));
      po2 = resultsAr[1].indexOf("</dataset>", po + 80);
      if (po < 0 || po2 < 0)
        String2.log("\npo=" + po + " po2=" + po2 + " results=\n" + resultsAr[1]);
      Test.ensureLinesMatch(
          resultsAr[1].substring(po, po2 + 10), expected2, "results=\n" + resultsAr[1]);

      String2.log("\ntestGenerateDatasetsXmlFromThreddsCatalog passed the test.");

    } catch (Throwable t) {
      throw new RuntimeException(
          "Error using generateDatasetsXml on " + EDStatic.erddapUrl,
          t); // in tests, always non-https url
    }
  }

  public static String UAFSubThreddsCatalogs[] = {
    // (v1.0.1  as of 2010-02-18 and still on 2013-02-01,
    // 2010-04-07 still v1.0.1, but #58-61 added)
    // 2012-12-09 big new catalog
    // 2017-04-15 base url changed
    // from https://ferret.pmel.noaa.gov/geoide/catalog/geoIDECleanCatalog.xml"
    // to   https://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalog.xml
    // 2017-11-08 redone
    // 0=entire "clean catalog" http://ferret.pmel.noaa.gov/uaf/thredds/geoIDECleanCatalog.html
    // 2020-05-20 was ferret.pmel.noaa.gov
    "https://data.pmel.noaa.gov/uaf/thredds/CleanCatalog.html",
    // 1
    "https://data.pmel.noaa.gov/uaf/thredds/CleanCatalogs/ecowatch.ncddc.noaa.gov/thredds/oceanNomads/catalog_aggs.html",
    "https://data.pmel.noaa.gov/uaf/thredds/CleanCatalogs/www.ngdc.noaa.gov/thredds/catalog.html",
    "https://data.pmel.noaa.gov/uaf/thredds/CleanCatalogs/data.nodc.noaa.gov/thredds/catalog/ncml/regclim/catalog.html",
    "https://data.pmel.noaa.gov/uaf/thredds/CleanCatalogs/data.nodc.noaa.gov/thredds/catalog/ncml/woa13/catalog.html",
    "https://data.pmel.noaa.gov/uaf/thredds/CleanCatalogs/oceanwatch.pfeg.noaa.gov/thredds/catalog.html",
    // 6
    "https://data.pmel.noaa.gov/uaf/thredds/CleanCatalogs/edac-dap3.northerngulfinstitute.org/thredds/catalog/ncom_fukushima_agg/catalog.html",
    "https://data.pmel.noaa.gov/uaf/thredds/CleanCatalogs/cwcgom.aoml.noaa.gov/thredds/catalog.html",
    "https://data.pmel.noaa.gov/uaf/thredds/CleanCatalogs/www.esrl.noaa.gov/psd/thredds/catalog/Datasets/catalog.html",
    "https://data.pmel.noaa.gov/uaf/thredds/CleanCatalogs/data.pmel.noaa.gov/pmel/thredds/carbontracker.html",
    "https://data.pmel.noaa.gov/uaf/thredds/CleanCatalogs/data1.gfdl.noaa.gov/thredds/catalog.html",
    // 11
    "https://data.pmel.noaa.gov/uaf/thredds/CleanCatalogs/osmc.noaa.gov/thredds/catalog.html",
    "https://data.pmel.noaa.gov/uaf/thredds/CleanCatalogs/data.pmel.noaa.gov/pmel/thredds/uaf.html",
    "https://data.pmel.noaa.gov/uaf/thredds/CleanCatalogs/tds.marine.rutgers.edu/thredds/roms/espresso/2013_da/catalog.html",
    "https://data.pmel.noaa.gov/uaf/thredds/CleanCatalogs/oos.soest.hawaii.edu/thredds/idd/ocn_mod.html",
    "https://data.pmel.noaa.gov/uaf/thredds/CleanCatalogs/oos.soest.hawaii.edu/thredds/idd/atm_mod.html",
    // 16
    "https://data.pmel.noaa.gov/uaf/thredds/CleanCatalogs/tds.glos.us/thredds/glcfs/nowcast/glcfs_nowcast_all.html",
    "https://data.pmel.noaa.gov/uaf/thredds/CleanCatalogs/tds.glos.us/thredds/glcfs/glcfs_forecast.html",

    // motherlode isn't part of UAF
    "https://motherlode.ucar.edu/thredds/catalog.xml"
  };

  /**
   * This is for use by Bob at ERD -- others don't need it. This generates a rough draft of the
   * datasets.xml entry for an EDDGridFromDap for the datasets served by ERD's Thredds server. The
   * XML can then be edited by hand and added to the datasets.xml file.
   *
   * @param search1 e.g., Satellite/aggregsat
   * @param search2 e.g., satellite
   * @throws Throwable if trouble
   */
  public static String generateErdThreddsDatasetXml(String search1, String search2)
      throws Throwable {

    String2.log("EDDGridFromDap.generateErdThreddsDatasetXml");

    // read DataSet.properties
    ResourceBundle2 dataSetRB2 = new ResourceBundle2("gov.noaa.pfel.coastwatch.DataSet");

    // read the main catalog
    String baseUrl = "https://oceanwatch.pfeg.noaa.gov/thredds/";
    String mainCat = SSR.getUrlResponseStringUnchanged(baseUrl + "catalog.html");
    int mainCatPo = 0;
    int mainCount = 0;

    StringBuilder sb = new StringBuilder();

    while (true) {
      // search for and extract from...
      // <a href="Satellite/aggregsatMH/chla/catalog.html"><kbd>Chlorophyll-a, Aqua MODIS, NPP,
      // Global, Science Quality/</kbd></a></td>
      String mainSearch = "<a href=\"" + search1;
      mainCatPo = mainCat.indexOf(mainSearch, mainCatPo + 1);
      if (mainCatPo < 0) // || mainCount++ >= 2)
      break;

      int q1 = mainCatPo + 8;
      int q2 = mainCat.indexOf('"', q1 + 1);
      int tt1 = mainCat.indexOf("<kbd>", mainCatPo);
      int tt2 = mainCat.indexOf("/</kbd>", mainCatPo);
      String subCatUrl = mainCat.substring(q1 + 1, q2);
      String twoLetter =
          mainCat.substring(mainCatPo + mainSearch.length(), mainCatPo + mainSearch.length() + 2);
      String fourLetter =
          mainCat.substring(
              mainCatPo + mainSearch.length() + 3, mainCatPo + mainSearch.length() + 7);
      String title = mainCat.substring(tt1 + 4, tt2);
      String longName =
          EDV.suggestLongName(
              "", "", dataSetRB2.getString(twoLetter + fourLetter + "StandardName", ""));
      String2.log(twoLetter + fourLetter + " = " + title);

      // read the sub catalog
      String subCat = SSR.getUrlResponseStringUnchanged(baseUrl + subCatUrl);
      int subCatPo = 0;

      while (true) {
        // search for and extract from...
        // ?dataset=satellite/MH/chla/5day">
        String subSearch = "?dataset=" + search2 + "/" + twoLetter + "/" + fourLetter + "/";
        subCatPo = subCat.indexOf(subSearch, subCatPo + 1);
        if (subCatPo < 0) break;
        int sq = subCat.indexOf('"', subCatPo);
        Test.ensureTrue(sq >= 0, "subSearch close quote not found.");
        String timePeriod = subCat.substring(subCatPo + subSearch.length(), sq);
        String reload =
            title.indexOf("Science Quality") >= 0
                ? "" + DEFAULT_RELOAD_EVERY_N_MINUTES
                : // weekly   (10080)
                timePeriod.equals("hday") ? "60" : "360"; // hourly or 6hourly
        int tpLength = timePeriod.length();
        String niceTimePeriod =
            timePeriod.equals("hday")
                ? "Hourly"
                : timePeriod.equals("mday")
                    ? "Monthly Composite"
                    : timePeriod.substring(0, tpLength - 3) + " Day Composite";
        String2.log("  " + timePeriod + " => " + niceTimePeriod);

        sb.append(
            "    <dataset type=\"EDDGridFromDap\" datasetID=\"erd"
                + twoLetter
                + fourLetter
                + timePeriod
                + "\">\n"
                + "        <sourceUrl>https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/"
                + twoLetter
                + "/"
                + fourLetter
                + "/"
                + timePeriod
                + "</sourceUrl>\n"
                + "        <reloadEveryNMinutes>"
                + reload
                + "</reloadEveryNMinutes>\n"
                + "        <addAttributes> \n"
                + "            <att name=\"infoUrl\">https://coastwatch.pfeg.noaa.gov/infog/"
                + twoLetter
                + "_"
                + fourLetter
                + "_las.html</att>\n"
                + "            <att name=\"title\">"
                + title
                + " ("
                + niceTimePeriod
                + ")</att>\n"
                + "            <att name=\"cwhdf_version\" />\n"
                + "            <att name=\"cols\" />  \n"
                + "            <att name=\"et_affine\" />\n"
                + "            <att name=\"gctp_datum\" />\n"
                + "            <att name=\"gctp_parm\" />\n"
                + "            <att name=\"gctp_sys\" />\n"
                + "            <att name=\"gctp_zone\" />\n"
                + "            <att name=\"id\" />\n"
                + "            <att name=\"pass_date\" />\n"
                + "            <att name=\"polygon_latitude\" />\n"
                + "            <att name=\"polygon_longitude\" />\n"
                + "            <att name=\"rows\" />\n"
                + "            <att name=\"start_time\" />\n"
                + "            <att name=\"time_coverage_end\" />  \n"
                + "            <att name=\"time_coverage_start\" />\n"
                + "        </addAttributes>\n"
                + "        <longitudeSourceName>lon</longitudeSourceName>\n"
                + "        <latitudeSourceName>lat</latitudeSourceName>\n"
                + "        <altitudeSourceName>altitude</altitudeSourceName>\n"
                + "        <timeSourceName>time</timeSourceName>\n"
                + "        <timeSourceFormat></timeSourceFormat> \n"
                + "        <dataVariable>\n"
                + "            <sourceName>"
                + twoLetter
                + fourLetter
                + "</sourceName>\n"
                + "            <destinationName>???"
                + fourLetter
                + "</destinationName>\n"
                + "            <addAttributes> \n"
                + "                <att name=\"ioos_category\">???Temperature</att>\n"
                + (longName.length() > 0
                    ? "                <att name=\"long_name\">" + longName + "</att>\n"
                    : "")
                + "                <att name=\"actual_range\" /> \n"
                + "                <att name=\"numberOfObservations\" /> \n"
                + "                <att name=\"percentCoverage\" />\n"
                + "            </addAttributes>\n"
                + "        </dataVariable>\n"
                + "    </dataset>\n"
                + "\n");
      }
    }
    return sb.toString();
  }

  /**
   * Crawl a THREDDS catalog to gather base DAP URLs.
   *
   * @param catalogXmlUrl A url ending in catalog.xml e.g.,
   *     https://oceanwatch.pfeg.noaa.gov/thredds/catalog.xml or
   *     https://oceanwatch.pfeg.noaa.gov/thredds/Satellite/MPOC/catalog.html (will be changed to
   *     .xml)
   * @param pathRegex if a catalogUrl path doesn't match this, the catalog won't be processed.
   * @param negativePathRegex if this is something other than null or "", then if a path matches
   *     this regex, the catalog will be ignored.
   * @param writer if not null, this calls generateDatasetsXml and writes results to writer.
   * @return a StringArray with the base DAP URLs.
   * @throws Exception if trouble at high level. Low level errors are logged to String2.log.
   */
  public static StringArray crawlThreddsCatalog(
      String catalogXmlUrl,
      String datasetNameRegex,
      String pathRegex,
      String negativePathRegex,
      Writer writer)
      throws Exception {

    // This isn't a good solution. see 2022-07-07 emails to netcdf-java people.
    // String lookFor = "remoteCatalogService?catalog=";
    // int po = catalogXmlUrl.indexOf(lookFor);
    // if (po > 0)
    //    catalogXmlUrl = catalogXmlUrl.substring(po + lookFor.length());

    catalogXmlUrl = File2.forceExtension(catalogXmlUrl, ".xml");
    String2.log("\n*** crawlThreddsCatalog(" + catalogXmlUrl + ")");
    long time = System.currentTimeMillis();
    if (!String2.isSomething(datasetNameRegex)) datasetNameRegex = ".*";
    if (!String2.isSomething(pathRegex)) pathRegex = ".*";
    StringBuilder summary = new StringBuilder();
    int datasetSuccessTimes[] = new int[String2.TimeDistributionSize];
    int datasetFailureTimes[] = new int[String2.TimeDistributionSize];

    // read the catalog
    // 2020-01-17 in netcdf-java 4.6 was
    // CatalogFactory factory = new CatalogFactory("default", false); //validate?
    // Catalog catalog = (Catalog)factory.readXML(catalogXmlUrl);
    // StringBuilder errorSB = new StringBuilder();
    // if (!catalog.check(errorSB, false))  //reallyVerbose?   returns true if no fatal errors
    //    throw new RuntimeException(String2.ERROR +
    //        ": Invalid Thredds catalog at " + catalogXmlUrl + "\n" + errorSB.toString());
    // errorSB = null;
    // 2020-01-17 with netcdf-java 5.2 is (thanks to Roland Schweitzer)
    Catalog catalog = new CatalogBuilder().buildFromURI(new java.net.URI(catalogXmlUrl));
    Test.ensureTrue(catalog != null, "catalog is null!");

    // process the catalog's datasets
    // ???getDatasets or getDatasetsLogical()?
    List<Dataset> datasets = catalog.getDatasets(); // getDatasetsLogical();
    HashSet<String> set = new HashSet();
    if (datasets != null) {
      if (verbose) String2.log("crawlThreddsCatalog will process " + datasets.size() + " datasets");
      for (int i = 0; i < datasets.size(); i++) // usually just 1
      processThreddsDataset(
            datasets.get(i),
            set,
            datasetNameRegex,
            pathRegex,
            negativePathRegex,
            writer,
            summary,
            datasetSuccessTimes,
            datasetFailureTimes);
    }

    // print summary of what was done
    String2.log("\n------------- Summary -------------");
    String2.log(summary.toString());

    // print time distributions
    if (writer != null)
      String2.log(
          "\n"
              + "* datasetSuccessTimes:\n"
              + String2.getTimeDistributionStatistics(datasetSuccessTimes)
              + "\n"
              + "* datasetFailureTimes:\n"
              + String2.getTimeDistributionStatistics(datasetFailureTimes));

    // done
    String2.log(
        "\n*** crawlThreddsCatalog finished successfully. time="
            + Calendar2.elapsedTimeString(System.currentTimeMillis() - time));
    StringArray sa = new StringArray(set.toArray());
    sa.sortIgnoreCase();
    return sa;
  }

  /**
   * The recursive, low-level, work-horse of crawlThreddsCatalog. Errors are logged to String2.log.
   *
   * @param set new base DAP URLs are added to this.
   * @param pathRegex if a catalogUrl path doesn't match this, the catalog won't be processed.
   * @param negativePathRegex if this is something other than null or "", then if a path matches
   *     this regex, the catalog will be ignored.
   * @param writer if not null, this calls generateDatasetsXml and writes results to writer.
   * @param summary a summary of what was done
   * @param datasetSuccessTimes an int[String2.TimeDistributionSize] to capture successful
   *     generateDatasetXml times
   * @param datasetFailureTimes an int[String2.TimeDistributionSize] to capture unsuccessful
   *     generateDatasetXml times
   */
  public static void processThreddsDataset(
      Dataset dataset,
      HashSet<String> set,
      String datasetNameRegex,
      String pathRegex,
      String negativePathRegex,
      Writer writer,
      StringBuilder summary,
      int datasetSuccessTimes[],
      int datasetFailureTimes[]) {
    String catUrl = null;
    try {
      // if (debugMode)
      String2.log("{{ processThreddsDataset set.size=" + set.size());

      // does catUrl match pathRegex?
      catUrl = dataset.getCatalogUrl();
      if (catUrl != null) {
        if (!catUrl.matches(pathRegex)
            ||
            // catUrl.indexOf("oceanwatch.pfeg.noaa.gov") >= 0 ||
            (catUrl.startsWith("https://thredds.jpl.nasa.gov/thredds")
                && (catUrl.indexOf("/ncml_aggregation/Chlorophyll/modis/ARCHIVED") >= 0
                    || catUrl.indexOf("/ncml_aggregation/Chlorophyll/modis/PENDING") >= 0))) {
          if (reallyVerbose)
            String2.log("  reject " + catUrl + " because it doesn't match pathRegex=" + pathRegex);
          return;
        }
        if (String2.isSomething(negativePathRegex) && catUrl.matches(negativePathRegex)) {
          if (reallyVerbose)
            String2.log(
                "  reject "
                    + catUrl
                    + " because it matches negativePathRegex="
                    + negativePathRegex);
          return;
        }
        String2.log("  catUrl=" + catUrl);
      }

      // has opendap service?
      Access access = dataset.getAccess(ServiceType.OPENDAP);
      if (access != null) {
        String baseUrl = access.getStandardUrlName();
        if (verbose) String2.log("  found opendap baseUrl=" + baseUrl);
        if (File2.getNameAndExtension(baseUrl).matches(datasetNameRegex)) {
          // is there a port number in the url that can be removed?
          String port = String2.extractRegex(baseUrl, ":\\d{4}/", 0);
          if (port != null) {
            // there is a port number
            // String2.log("!!!found port=" + port); Math2.sleep(1000);
            String tbaseUrl = String2.replaceAll(baseUrl, port, "/");
            try {
              String dds = SSR.getUrlResponseStringUnchanged(tbaseUrl + ".dds");
              if (dds.startsWith("Dataset {")) {
                // it can be removed
                String msg = "port#" + port + " was removed from " + baseUrl;
                String2.log(msg);
                summary.append(msg + "\n");
                baseUrl = tbaseUrl;
              }
            } catch (Throwable t) {
              // port # can't be removed
              // String2.log(t.toString()); Math2.sleep(1000);
            }
          }

          boolean isNew = set.add(baseUrl);
          if (isNew && writer != null) {
            // so let's call generateDatasetsXml
            // gather metadata
            StringBuilder history = new StringBuilder();
            StringBuilder title = new StringBuilder();
            StringBuilder tAck = new StringBuilder();
            StringBuilder tLicense = new StringBuilder();
            StringBuilder tSummary = new StringBuilder();
            String infoUrl = null;
            Attributes atts = new Attributes();
            List list;
            if (String2.isSomething(dataset.getRights())) tLicense.append(dataset.getRights());
            if (String2.isSomething(dataset.getSummary())) tSummary.append(dataset.getSummary());

            list = dataset.getContributors();
            if (list != null && list.size() > 0) {
              StringBuilder names = new StringBuilder();
              StringBuilder roles = new StringBuilder();
              for (int i = 0; i < list.size(); i++) {
                Contributor contributor = (Contributor) list.get(i);
                String2.ifSomethingConcat(names, ", ", contributor.getName());
                String2.ifSomethingConcat(roles, ", ", contributor.getRole());
              }
              atts.add("contributor_name", names.toString());
              atts.add("contributor_role", roles.toString());
            }

            list = dataset.getCreators();
            if (list != null && list.size() > 0) {
              Source source = (Source) list.get(0);
              atts.add("creator_name", source.getName());
              atts.add("creator_email", source.getEmail());
              atts.add("creator_url", source.getUrl());
            }

            list = dataset.getDocumentation();
            if (list != null) {
              for (int i = 0; i < list.size(); i++) {
                Documentation id = (Documentation) list.get(i);
                // String2.log(">> Doc#" + i + ": type:" + id.getType());
                // String2.log(">> Doc#" + i + ": inlineContent:" + id.getInlineContent());
                // String2.log(">> Doc#" + i + ": URI:" + id.getURI());

                // first URI -> infoUrl
                java.net.URI ttUrl = id.getURI();
                String tUrl = ttUrl == null ? null : ttUrl.toString();
                if (String2.isSomething(tUrl) && !String2.isSomething(infoUrl)) infoUrl = tUrl;

                // other things require a type
                String tType = id.getType();
                if (!String2.isSomething(tType)) continue;
                String tContent = id.getInlineContent();
                if (tType.toLowerCase().equals("funding")
                    && !String2.looselyContains(tAck.toString(), tContent))
                  String2.ifSomethingConcat(tAck, " ", tContent);
                if (tType.toLowerCase().equals("rights")
                    && !String2.looselyContains(tLicense.toString(), tContent))
                  String2.ifSomethingConcat(tLicense, " ", tContent);
                if (tType.toLowerCase().equals("summary")
                    && !String2.looselyContains(tSummary.toString(), tContent))
                  String2.ifSomethingConcat(tSummary, " ", tContent);
              }
            }

            // String2.pressEnterToContinue(">> title=" + title.toString() + " name=" +
            // dataset.getName());
            String fullName = dataset.getName();
            if (fullName == null) fullName = "";
            Dataset tParentDataset = dataset.getParentDataset();
            while (tParentDataset != null) {
              String tpName = tParentDataset.getName();
              if (String2.isSomething(tpName)) fullName = tpName + ", " + fullName;
              tParentDataset = tParentDataset.getParentDataset();
            }
            String2.ifSomethingConcat(
                title, "",
                fullName); // 2020-01-17 in netcdfjava 4.6, there was dataset.getFullName()
            String2.ifSomethingConcat(history, "\n", dataset.getHistory());

            list = dataset.getKeywords();
            if (list != null) {
              StringBuilder sb = new StringBuilder();
              for (int i = 0; i < list.size(); i++) {
                Vocab v = (Vocab) list.get(i);
                // if (i == 0) String2.listMethods(v);
                sb.append(v.getText() + ", ");
              }
              if (sb.length() > 0) atts.add("keywords", sb.toString());
            }

            // list = dataset.getMetadata();
            // if (list != null)
            //    String2.log("* Metadata:      " + String2.toNewlineString(list.toArray()));

            // String2.log("* Name:          " + dataset.getName());  //1day

            String2.ifSomethingConcat(history, "\n", dataset.getProcessing());

            atts.add("id", dataset.getID());
            atts.add("naming_authority", dataset.getAuthority());

            list = dataset.getPublishers();
            if (list != null && list.size() > 0) {
              Source source = (Source) list.get(0);
              atts.add("publisher_name", source.getName());
              atts.add("publisher_email", source.getEmail());
              atts.add("publisher_url", source.getUrl());
            }

            // suggest title
            title = new StringBuilder(removeExtensionsFromTitle(title.toString())); // e.g., .grib
            String2.replaceAll(title, "/", ", ");
            String2.replaceAll(title, "_.", " ");
            String2.replaceAll(title, '_', ' ');
            String2.replaceAll(title, '\n', ' ');
            String2.replaceAll(title, "avhrr AVHRR", "AVHRR");
            String2.replaceAllIgnoreCase(title, "aggregate", "");
            String2.replaceAllIgnoreCase(title, "aggregation", "");
            String2.replaceAllIgnoreCase(title, "ghrsst", "GHRSST");
            String2.replaceAllIgnoreCase(title, "ncml", "");
            String2.replaceAllIgnoreCase(title, "Data iridl.ldeo.columbia.edu SOURCES ", "");
            String2.replaceAllIgnoreCase(title, "data opendap.jpl.nasa.gov opendap ", "");
            String2.whitespacesToSpace(title);
            int dpo = title.lastIndexOf(" dodsC ");
            if (dpo >= 0) title.delete(0, dpo + 7);
            atts.add("title", title.toString());

            if (tAck.length() > 0) atts.add("acknowledgement", tAck.toString());
            if (history.length() > 0) atts.add("history", history.toString());
            if (String2.isSomething(infoUrl)) atts.add("infoUrl", infoUrl);
            if (tLicense.length() > 0) atts.add("license", tLicense.toString());
            if (tSummary.length() > 0) atts.add("summary", tSummary.toString());
            // String2.pressEnterToContinue("atts=\n" + atts.toString());

            safelyGenerateDatasetsXml(
                baseUrl,
                -1, // tReloadEveryNMinutes,
                atts,
                writer,
                summary,
                "",
                datasetSuccessTimes,
                datasetFailureTimes);
          }
        } else {
          if (verbose)
            String2.log(
                "  reject opendap baseUrl="
                    + baseUrl
                    + " because it doesn't match "
                    + datasetNameRegex);
        }
      }

      // has nested datasets?
      List<Dataset> datasets = dataset.getDatasetsLogical();
      if (datasets != null) {
        if (reallyVerbose) String2.log("  processing " + datasets.size() + " nested datasets...");
        for (int i = 0; i < datasets.size(); i++) {
          processThreddsDataset(
              datasets.get(i),
              set,
              datasetNameRegex,
              pathRegex,
              negativePathRegex,
              writer,
              summary,
              datasetSuccessTimes,
              datasetFailureTimes); // recursive
        }
      }
      if (debugMode) String2.log("  exit");
    } catch (Exception e) {
      try {
        String msg =
            "\n"
                + String2.ERROR
                + " in processThreddsDataset "
                + dataset.toString()
                + "\n"
                + "catUrl="
                + catUrl
                + "\n"
                + MustBe.throwableToString(e);
        summary.append(msg);
        String2.log(msg);
      } catch (Exception e2) {
        String2.log("second Exception!");
      }
    }
  }
}
