/*
 * EDDGridLonPM180 Copyright 2015, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.IntArray;
import com.cohort.array.PAOne;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.handlers.EDDGridLonPM180Handler;
import gov.noaa.pfel.erddap.handlers.SaxHandlerClass;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;
import java.io.BufferedReader;
import java.text.MessageFormat;

/**
 * This class creates an EDDGrid with longitude values in the range -180 to 180 from the enclosed
 * dataset with ascending longitude values, at least some of which are &gt; 180.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2015-08-05
 */
@SaxHandlerClass(EDDGridLonPM180Handler.class)
public class EDDGridLonPM180 extends EDDGrid {

  private EDDGrid childDataset;
  // If childDataset is a fromErddap dataset from this ERDDAP,
  // childDataset will be kept as null and always refreshed from
  // erddap.gridDatasetHashMap.get(localChildDatasetID).
  private Erddap erddap;
  private String localChildDatasetID;

  private int sloni0, sloni179, sloni180, sloni359; // source lon indices
  // corresponding destination lon indices, after the 2 parts are reordered
  // i.e., where are sloni in the destination lon array?
  // If dloni0/179 are -1, there are no values in that range in this dataset.
  private int dloni0, dloni179, dloni180, dloni359;

  // dInsert359/0 are used if there is a big gap between lon359 and lon0 (e.g., 300/-120...120)
  // !!! Need to insert lots of missing values (at average spacing)
  // !!! so if user requests stride >1, user will always get a reasonable set of lon values
  // !!! and an appropriate string of missing values in the lon gap.
  // If the 'i'ndex values are -1, the system is not active for this dataset.
  private int insert359i = -1, insert0i = -1;

  /**
   * This constructs an EDDGridLonPM180 based on the information in an .xml file.
   *
   * @param erddap if known in this context, else null
   * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDGridLonPM180"&gt; having
   *     just been read.
   * @return an EDDGridLonPM180. When this returns, xmlReader will have just read
   *     &lt;erddapDatasets&gt;&lt;/dataset&gt; .
   * @throws Throwable if trouble
   */
  @EDDFromXmlMethod
  public static EDDGridLonPM180 fromXml(Erddap erddap, SimpleXMLReader xmlReader) throws Throwable {

    if (verbose) String2.log("\n*** constructing EDDGridLonPM180(xmlReader)...");
    String tDatasetID = xmlReader.attributeValue("datasetID");

    // data to be obtained while reading xml
    EDDGrid tChildDataset = null;
    String tAccessibleTo = null;
    String tGraphsAccessibleTo = null;
    boolean tAccessibleViaWMS = true;
    boolean tAccessibleViaFiles = EDStatic.defaultAccessibleViaFiles;
    StringArray tOnChange = new StringArray();
    String tFgdcFile = null;
    String tIso19115File = null;
    String tDefaultDataQuery = null;
    String tDefaultGraphQuery = null;
    int tReloadEveryNMinutes = Integer.MAX_VALUE; // unusual
    int tUpdateEveryNMillis = Integer.MAX_VALUE; // unusual
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
      if (xmlReader.stackSize() == startOfTagsN) break; // the </dataset> tag
      String localTags = tags.substring(startOfTagsLength);

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
          if (tChildDataset == null) {
            EDD edd = EDD.fromXml(erddap, xmlReader.attributeValue("type"), xmlReader);
            if (edd instanceof EDDGrid eddGrid) {
              tChildDataset = eddGrid;
            } else {
              throw new RuntimeException(
                  "Datasets.xml error: "
                      + "The dataset defined in an "
                      + "EDDGridLonPM180 must be a subclass of EDDGrid.");
            }
          } else {
            throw new RuntimeException(
                "Datasets.xml error: "
                    + "There can be only one <dataset> defined within an "
                    + "EDDGridLonPM180 <dataset>.");
          }
        }

      } else if (localTags.equals("<reloadEveryNMinutes>")) {
      } else if (localTags.equals("</reloadEveryNMinutes>"))
        tReloadEveryNMinutes = String2.parseInt(content);
      else if (localTags.equals("<updateEveryNMillis>")) {
      } else if (localTags.equals("</updateEveryNMillis>"))
        tUpdateEveryNMillis = String2.parseInt(content);
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

    // make the main dataset based on the information gathered
    return new EDDGridLonPM180(
        erddap,
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
        tReloadEveryNMinutes,
        tUpdateEveryNMillis,
        tChildDataset,
        tnThreads,
        tDimensionValuesInMemory);
  }

  /**
   * The constructor. The axisVariables must be the same and in the same order for each
   * dataVariable.
   *
   * @param erddap if known in this context, else null
   * @param tDatasetID
   * @param tAccessibleTo is a space separated list of 0 or more roles which will have access to
   *     this dataset. <br>
   *     If null, everyone will have access to this dataset (even if not logged in). <br>
   *     If "", no one will have access to this dataset.
   * @param tOnChange 0 or more actions (starting with http://, https://, or mailto: ) to be done
   *     whenever the dataset changes significantly
   * @param tFgdcFile This should be the fullname of a file with the FGDC that should be used for
   *     this dataset, or "" (to cause ERDDAP not to try to generate FGDC metadata for this
   *     dataset), or null (to allow ERDDAP to try to generate FGDC metadata for this dataset).
   * @param tIso19115 This is like tFgdcFile, but for the ISO 19119-2/19139 metadata.
   * @param tChildDataset
   * @throws Throwable if trouble
   */
  public EDDGridLonPM180(
      Erddap tErddap,
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
      int tReloadEveryNMinutes,
      int tUpdateEveryNMillis,
      EDDGrid oChildDataset,
      int tnThreads,
      boolean tDimensionValuesInMemory)
      throws Throwable {

    if (verbose) String2.log("\n*** constructing EDDGridLonPM180 " + tDatasetID);
    long constructionStartMillis = System.currentTimeMillis();
    String errorInMethod = "Error in EDDGridLonPM180(" + tDatasetID + ") constructor:\n";

    // save some of the parameters
    className = "EDDGridLonPM180";
    datasetID = tDatasetID;
    Test.ensureFileNameSafe(datasetID, "datasetID");
    setAccessibleTo(tAccessibleTo);
    setGraphsAccessibleTo(tGraphsAccessibleTo);
    if (!tAccessibleViaWMS)
      accessibleViaWMS = String2.canonical(MessageFormat.format(EDStatic.noXxxAr[0], "WMS"));
    onChange = tOnChange;
    fgdcFile = tFgdcFile;
    iso19115File = tIso19115File;
    defaultDataQuery = tDefaultDataQuery;
    defaultGraphQuery = tDefaultGraphQuery;
    nThreads = tnThreads; // interpret invalid values (like -1) as EDStatic.nGridThreads
    dimensionValuesInMemory = tDimensionValuesInMemory;

    if (oChildDataset == null) throw new RuntimeException(errorInMethod + "childDataset=null!");
    if (datasetID.equals(oChildDataset.datasetID()))
      throw new RuntimeException(
          errorInMethod
              + "This dataset's datasetID must not be the same as the child's datasetID.");

    // is oChildDataset a fromErddap from this erddap?
    // Get childDataset or localChildDataset. Work with stable local reference.
    EDDGrid tChildDataset = null;
    if (tErddap != null && oChildDataset instanceof EDDGridFromErddap tFromErddap) {
      String tSourceUrl = tFromErddap.getPublicSourceErddapUrl();
      if (EDStatic.urlIsThisComputer(tSourceUrl)) {
        String lcdid = File2.getNameNoExtension(tSourceUrl);
        if (datasetID.equals(lcdid))
          throw new RuntimeException(
              errorInMethod
                  + "This dataset's datasetID must not be the same as the localChild's datasetID.");
        EDDGrid lcd = tErddap.gridDatasetHashMap.get(lcdid);
        if (lcd != null) {
          // yes, use child dataset from this erddap
          // The local dataset will be re-gotten from erddap.gridDatasetHashMap
          //  whenever it is needed.
          if (reallyVerbose) String2.log("  found/using localChildDatasetID=" + lcdid);
          tChildDataset = lcd;
          erddap = tErddap;
          localChildDatasetID = lcdid;
        }
      }
    }
    if (tChildDataset == null) {
      // no, use the oChildDataset supplied to this constructor
      childDataset = oChildDataset; // set instance variable
      tChildDataset = oChildDataset;
    }
    // for rest of constructor, use temporary, stable tChildDataset reference.
    // String2.log(">> accessibleViaFiles " + EDStatic.filesActive + " " + tAccessibleViaFiles + " "
    // + tChildDataset.accessibleViaFiles);
    accessibleViaFiles =
        EDStatic.filesActive && tAccessibleViaFiles && tChildDataset.accessibleViaFiles;

    // UNUSUAL: if valid value not specified, copy from childDataset
    setReloadEveryNMinutes(
        tReloadEveryNMinutes < Integer.MAX_VALUE
            ? tReloadEveryNMinutes
            : tChildDataset.getReloadEveryNMinutes());
    setUpdateEveryNMillis(
        tUpdateEveryNMillis < Integer.MAX_VALUE
            ? tUpdateEveryNMillis
            : tChildDataset.updateEveryNMillis);

    // make/copy the local globalAttributes
    localSourceUrl = tChildDataset.localSourceUrl();
    sourceGlobalAttributes = (Attributes) tChildDataset.sourceGlobalAttributes().clone();
    addGlobalAttributes = (Attributes) tChildDataset.addGlobalAttributes().clone();
    combinedGlobalAttributes = (Attributes) tChildDataset.combinedGlobalAttributes().clone();
    combinedGlobalAttributes.set(
        "title", combinedGlobalAttributes.getString("title").trim() + ", Lon+/-180");

    // make/copy the local axisVariables
    int nAv = tChildDataset.axisVariables.length;
    axisVariables = new EDVGridAxis[nAv];
    System.arraycopy(tChildDataset.axisVariables, 0, axisVariables, 0, nAv);
    lonIndex = tChildDataset.lonIndex;
    latIndex = tChildDataset.latIndex;
    altIndex = tChildDataset.altIndex;
    depthIndex = tChildDataset.depthIndex;
    timeIndex = tChildDataset.timeIndex;
    if (lonIndex < 0)
      throw new RuntimeException(
          errorInMethod + "The child dataset doesn't have a longitude axis variable.");

    // lonIndex not rightmost isn't tested. So throw exception and ask admin for sample.
    if (lonIndex < nAv - 1)
      throw new RuntimeException(
          errorInMethod
              + "The longitude dimension isn't the rightmost dimension. This is untested. "
              + "Please send a sample file to erd.data@noaa.gov .");

    // make/copy the local dataVariables
    int nDv = tChildDataset.dataVariables.length;
    dataVariables = new EDV[nDv];
    System.arraycopy(tChildDataset.dataVariables, 0, dataVariables, 0, nDv);

    // figure out the newLonValues
    EDVLonGridAxis childLon = (EDVLonGridAxis) axisVariables[lonIndex];
    if (!childLon.isAscending())
      throw new RuntimeException(errorInMethod + "The child longitude axis has descending values!");
    if (childLon.destinationMaxDouble() <= 180)
      throw new RuntimeException(
          errorInMethod
              + "The child longitude axis has no values >180 (max="
              + childLon.destinationMaxDouble()
              + ")!");
    PrimitiveArray childLonValues = childLon.destinationValues();
    int nChildLonValues = childLonValues.size();

    // old:           || sloni0, sloni179 || sloni180,  sloni359           ||
    // old: [ignored] ||      0,  90, 179 ||      180, 270,  359           || [ignored]
    // new:                                      -180, -90,   -1 insert359 || insert0,  0, 90, 179
    // all of the searches use EXACT math
    PAOne clvPAOne = new PAOne(childLonValues);
    if (childLon.destinationMinDouble() < 180) {
      sloni0 =
          childLonValues.binaryFindFirstGE(
              0, nChildLonValues - 1, clvPAOne.setDouble(0)); // first index >=0
      sloni179 =
          childLonValues.binaryFindLastLE(
              sloni0, nChildLonValues - 1, clvPAOne.setDouble(180)); // last index <180
      if (childLonValues.getDouble(sloni179) == 180) sloni179--;
    } else {
      sloni0 = -1; // no values <180
      sloni179 = -1;
    }
    sloni180 = sloni179 + 1; // first index >=180
    if (childLonValues.getDouble(sloni180) > 360)
      throw new RuntimeException(
          errorInMethod + "There are no child longitude values in the range 180 to 360!");
    sloni359 =
        childLonValues.binaryFindLastLE(
            sloni180, nChildLonValues - 1, clvPAOne.setDouble(360)); // last index <=360
    if (childLonValues.getDouble(sloni359) == 360
        && // there is a value=360
        sloni359 > sloni180
        && // and it isn't the only value in the range 180 to 360...
        sloni0 >= 0
        && // and if there is a sloni0
        childLonValues.getDouble(sloni0) == 0) // with value == 0
    sloni359--; // 360 is a duplicate of 0, so ignore it
    PrimitiveArray newLonValues =
        childLonValues.subset(sloni180, 1, sloni359); // always a new backing array
    newLonValues.addOffsetScale(-360, 1); // 180 ... 359 -> -180 ... -1

    // set dloni: where are sloni after source values are rearranged to dest values
    dloni180 = 0;
    dloni359 = sloni359 - sloni180;

    if (sloni0 >= 0) {
      // create dInsert if there's a big gap
      double spacing = childLon.averageSpacing(); // avg is more reliable than any single value
      double lon359 = newLonValues.getDouble(newLonValues.size() - 1);
      double lon0 = childLonValues.getDouble(sloni0);
      // e.g., gap of 2.1*spacing --floor--> 2 -> insert 1 value
      int insertN = Math2.roundToInt(Math.floor((lon0 - lon359) / spacing)) - 1;
      if (insertN >= 1) { // enough space to insert 1+ regularly spaced lon values

        insert359i = newLonValues.size();
        for (int i = 1; i <= insertN; i++) {
          double td = Math2.niceDouble(lon359 + i * spacing, 12);
          if (Math.abs(td) < 1e-8) // be extra careful to catch 0
          td = 0;
          newLonValues.addDouble(td);
        }
        insert0i = newLonValues.size() - 1;
        if (verbose)
          String2.log(
              "  'insert' is active: lon["
                  + insert359i
                  + "]="
                  + newLonValues.getDouble(insert359i)
                  + " lon["
                  + insert0i
                  + "]="
                  + newLonValues.getDouble(insert0i));
      }

      // append the 0 ... 179 lon values
      dloni0 = newLonValues.size();
      newLonValues.append(childLonValues.subset(sloni0, 1, sloni179));
      dloni179 = newLonValues.size() - 1;

    } else {
      dloni0 = -1; // no values <180
      dloni179 = -1;
    }

    // make the new lon axis
    EDVLonGridAxis newEDVLon =
        new EDVLonGridAxis(
            tDatasetID,
            EDV.LON_NAME,
            new Attributes(childLon.combinedAttributes()),
            new Attributes(),
            newLonValues);
    newEDVLon.combinedAttributes().remove("valid_min");
    newEDVLon.combinedAttributes().remove("valid_max");
    axisVariables[lonIndex] = newEDVLon;

    // ensure the setup is valid
    ensureValid();

    // If the original childDataset is a FromErddap, try to subscribe to the remote dataset.
    // Don't use tChildDataset. It may be the local EDDGrid that the fromErddap points to.
    if (oChildDataset instanceof FromErddap) tryToSubscribeToChildFromErddap(oChildDataset);

    // finally
    long cTime = System.currentTimeMillis() - constructionStartMillis;
    if (verbose)
      String2.log(
          (reallyVerbose
                  ? "\n"
                      + toString()
                      + "dloni180="
                      + dloni180
                      + " dloni359="
                      + dloni359
                      + " insert359i="
                      + insert359i
                      + " insert0i="
                      + insert0i
                      + " dloni0="
                      + dloni0
                      + " dloni179="
                      + dloni179
                      + "\n"
                  : "")
              + "localChildDatasetID="
              + localChildDatasetID
              + "\n"
              + "\n*** EDDGridLonPM180 "
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
   * This returns the childDataset (if not null) or the localChildDataset.
   *
   * @param language the index of the selected language
   */
  public EDDGrid getChildDataset(int language) {
    // Get childDataset or localChildDataset. Work with stable local reference.
    EDDGrid tChildDataset = childDataset;
    if (tChildDataset == null) {
      tChildDataset = erddap.gridDatasetHashMap.get(localChildDatasetID);
      if (tChildDataset == null) {
        EDD.requestReloadASAP(localChildDatasetID);
        throw new WaitThenTryAgainException(
            EDStatic.simpleBilingual(language, EDStatic.waitThenTryAgainAr)
                + "\n(underlying local datasetID="
                + localChildDatasetID
                + " not found)");
      }
    }
    return tChildDataset;
  }

  /**
   * This returns a list of childDatasetIDs. Most dataset types don't have any children. A few, like
   * EDDGridSideBySide do, so they overwrite this method to return the IDs.
   *
   * @return a new list of childDatasetIDs.
   */
  @Override
  public StringArray childDatasetIDs() {
    StringArray sa = new StringArray();
    try {
      sa.add(childDataset == null ? localChildDatasetID : childDataset.datasetID());
    } catch (Exception e) {
      String2.log("Error caught in edd.childDatasetIDs(): " + MustBe.throwableToString(e));
    }
    return sa;
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
    throw new SimpleException("Error: EDDGridLonPM180 doesn't support method=\"sibling\".");
  }

  /**
   * This returns a fileTable with valid files (or null if unavailable or any trouble). This is a
   * copy of any internal data, so client can modify the contents.
   *
   * @param language the index of the selected language
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
    EDDGrid tChildDataset = getChildDataset(language);
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
    EDDGrid tChildDataset = getChildDataset(language);
    return tChildDataset.accessibleViaFilesGetLocal(language, relativeFileName);
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

    // Get childDataset or localChildDataset. Work with stable local reference.
    EDDGrid tChildDataset = getChildDataset(language);

    boolean changed = tChildDataset.lowUpdate(language, msg, startUpdateMillis);
    // catch and deal with time axis changes: by far the most common
    if (changed && timeIndex >= 0) {
      axisVariables[timeIndex] = tChildDataset.axisVariables[timeIndex];
      combinedGlobalAttributes()
          .set("time_coverage_start", axisVariables[timeIndex].destinationMinString());
      combinedGlobalAttributes()
          .set("time_coverage_end", axisVariables[timeIndex].destinationMaxString());
    }

    return changed;
  }

  /**
   * This gets data (not yet standardized) from the data source for this EDDGrid. Because this is
   * called by GridDataAccessor, the request won't be the full user's request, but will be a partial
   * request (for less than EDStatic.partialRequestMaxBytes).
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

    // Get childDataset or localChildDataset. Work with stable local reference.
    // This isn't ideal (better if GridDataAccessor had stable reference to childDataset,
    // but it needs this dataset), but it is pretty good.
    EDDGrid tChildDataset = getChildDataset(language);
    tDirTable = tChildDataset.getDirTable();
    tFileTable = tChildDataset.getFileTable();

    // get the request lon start/stride/stop
    int li3 = lonIndex * 3;
    int rStart = tConstraints.get(li3 + 0);
    int rStride = tConstraints.get(li3 + 1);
    int rStop = tConstraints.get(li3 + 2);
    int nAV = axisVariables.length;
    int tnDV = tDataVariables.length;

    // find exact stop (so tests below are true tests)
    rStop -= (rStop - rStart) % rStride;

    // map:
    // dest is: dloni180...dloni359, [insert359...insert0,] [dloni0...dloni179]
    // becomes      -180...-1,       [insert359...insert0,] [     0...     179]

    // really easy: all the requested lons are from the new right: >=dloni0
    if (dloni0 != -1 && rStart >= dloni0) {
      // 1 request, and lon values are already correct
      IntArray newConstraints = (IntArray) tConstraints.subset(0, 1, tConstraints.size() - 1);
      newConstraints.set(li3 + 0, rStart - (dloni0 - sloni0));
      newConstraints.set(li3 + 2, rStop - (dloni0 - sloni0));
      return tChildDataset.getSourceData(
          language, tDirTable, tFileTable, tDataVariables, newConstraints);
    }

    // easy: all the requested lons are from the left: <=dloni359
    if (rStop <= dloni359) {
      // 1 request, but need to adjust lon values
      IntArray newConstraints = (IntArray) tConstraints.subset(0, 1, tConstraints.size() - 1);
      newConstraints.set(li3 + 0, rStart + sloni180); // (sloni180 - dloni180), but dloni180 is 0
      newConstraints.set(li3 + 2, rStop + sloni180); // (sloni180 - dloni180), but dloni180 is 0
      PrimitiveArray results[] =
          tChildDataset.getSourceData(
              language, tDirTable, tFileTable, tDataVariables, newConstraints);
      results[lonIndex] = (PrimitiveArray) results[lonIndex].clone();
      results[lonIndex].addOffsetScale(-360, 1);
      return results;
    }

    // map:
    // dest is: dloni180...dloni359, [insert359i...insert0i,]  [dloni0...dloni179]
    // becomes      -180...-1,       [insert359i...insert0i,]  [     0...     179]

    // Harder: need to make 2 requests and merge them.
    // If this is a big request, this takes a lot of memory for the *short* time
    //  when merging in memory.
    if (debugMode) String2.log(">> lon request= " + rStart + ":" + rStride + ":" + rStop);

    // find request for lon 180-359: <=dloni359
    IntArray newConstraints = (IntArray) tConstraints.subset(0, 1, tConstraints.size() - 1);
    PrimitiveArray results180[] = null;
    if (rStart <= dloni359) {
      // some values are from source loni180 to loni359
      newConstraints.set(li3 + 0, rStart + sloni180); // (sloni180 - dloni180), but dloni180 is 0
      newConstraints.set(li3 + 2, sloni359);
      results180 =
          tChildDataset.getSourceData(
              language, tDirTable, tFileTable, tDataVariables, newConstraints);
      results180[lonIndex] = (PrimitiveArray) results180[lonIndex].clone();
      results180[lonIndex].addOffsetScale(-360, 1);
      if (debugMode)
        String2.log(
            ">> got results180 from source lon["
                + (rStart + sloni180)
                + ":"
                + rStride
                + ":"
                + sloni359
                + "]");
    }

    // find out if 'insert' is active and relevant (i.e., some values are requested)
    boolean insertActive = insert359i >= 0; // basic system is active
    if (insertActive && (rStop < insert359i || rStart > insert0i))
      insertActive = false; // shouldn't happen because should have been dealt with above
    int firstInserti = -1;
    if (insertActive) {
      // Find first loni >= insert359i which is among actually requested lon values (given rStride).
      firstInserti = Math.max(insert359i, rStart);
      if (rStride > 1) {
        int mod = (firstInserti - rStart) % rStride;
        if (mod > 0) firstInserti += rStride - mod;
      }
      if (firstInserti > insert0i) {
        firstInserti = -1;
        insertActive = false; // stride walks over (skips) all inserted lons
      }
    }
    int lastInserti = -1;
    int insertNLon = 0;
    if (insertActive) {
      lastInserti = Math.min(rStop, insert0i);
      lastInserti -= (lastInserti - rStart) % rStride; // perhaps last=first
      insertNLon = OpendapHelper.calculateNValues(firstInserti, rStride, lastInserti);
      if (debugMode)
        String2.log(">> will insert lon[" + firstInserti + ":" + rStride + ":" + lastInserti + "]");
    }

    // find request for lon 0-179: >=dloni0
    PrimitiveArray results0[] = null;
    if (rStop >= dloni0) {
      // Find first loni >=dloni0 which is among actually requested lon values (given rStride).
      int floni = dloni0;
      if (rStride > 1) {
        int mod = (floni - rStart) % rStride;
        if (mod > 0) floni += rStride - mod;
      }

      // some values are from source loni0 to loni179
      int tStart = floni - (dloni0 - sloni0);
      int tStop = rStop - (dloni0 - sloni0);
      newConstraints.set(li3 + 0, tStart);
      newConstraints.set(li3 + 2, tStop);
      results0 =
          tChildDataset.getSourceData(
              language, tDirTable, tFileTable, tDataVariables, newConstraints);
      if (debugMode)
        String2.log(
            ">> got results0 from source lon[" + tStart + ":" + rStride + ":" + tStop + "]");
    }

    // map:
    // dest is: dloni180...dloni359, [insert359i...insert0i,]  [dloni0...dloni179]
    // becomes      -180...-1,       [insert359i...insert0i,]  [     0...     179]

    // now build results[] by reading chunks alternately from the 3 sources
    // what is chunk size from 180 request, from insert, and from 0 request?
    int chunk180 = results180 == null ? 0 : 1;
    int chunkInsert = insertNLon <= 0 ? 0 : 1;
    int chunk0 = results0 == null ? 0 : 1;
    // what is total nValues for dataVariables
    int nValues = 1;
    PrimitiveArray results[] = new PrimitiveArray[nAV + tnDV];
    for (int avi = 0; avi < nAV; avi++) {
      int avi3 = avi * 3;
      int avStart = tConstraints.get(avi3 + 0);
      int avStride = tConstraints.get(avi3 + 1);
      int avStop = tConstraints.get(avi3 + 2);

      results[avi] =
          axisVariables[avi]
              .sourceValues()
              .subset( // source=dest
                  avStart, avStride, avStop);
      int avN = results[avi].size();

      if (avi > lonIndex) {
        chunk180 *= avN;
        chunkInsert *= avN;
        chunk0 *= avN;
      }
      if (avi == lonIndex) {
        if (results180 != null) chunk180 *= results180[avi].size();
        if (insertNLon > 0) chunkInsert *= insertNLon;
        if (results0 != null) chunk0 *= results0[avi].size();
      }
      // all axes:
      nValues *= avN;
    }

    // and make the results data variables
    for (int tdv = 0; tdv < tnDV; tdv++)
      results[nAV + tdv] =
          PrimitiveArray.factory(tDataVariables[tdv].sourceDataPAType(), nValues, false);

    // dest is: dloni180...dloni359, [insert359i...insert0i,]  [dloni0...dloni179]
    // becomes      -180...-1,       [insert359i...insert0i,]  [     0...     179]

    // copy the dataVariables values into place
    int po180 = 0;
    int po0 = 0;
    int nextDest = 0;
    while (nextDest < nValues) {
      for (int tdv = 0; tdv < tnDV; tdv++) {
        if (results180 != null)
          results[nAV + tdv].addFromPA(results180[nAV + tdv], po180, chunk180);
        if (chunkInsert > 0)
          results[nAV + tdv].addNDoubles(
              chunkInsert, tDataVariables[tdv].safeDestinationMissingValue());
        if (results0 != null) results[nAV + tdv].addFromPA(results0[nAV + tdv], po0, chunk0);
      }
      po180 += chunk180; // may be 0
      po0 += chunk0; // may be 0
      nextDest += chunk180 + chunkInsert + chunk0;

      if (debugMode && nextDest >= nValues) {
        if (results180 != null) Test.ensureEqual(results180[nAV].size(), po180, "po180");
        if (results0 != null) Test.ensureEqual(results0[nAV].size(), po0, "po0");
        Test.ensureEqual(results[nAV].size(), nValues, "nValues");
      }
    }
    return results;
  }

  /**
   * This runs generateDatasetsXmlFromErddapCatalog.
   *
   * @param oPublicSourceUrl A complete URL of an ERDDAP (ending with "/erddap/").
   * @param datasetIDRegex E.g., ".*" for all dataset Names.
   * @return a String with the results
   */
  public static String generateDatasetsXmlFromErddapCatalog(
      String oPublicSourceUrl, String datasetIDRegex) throws Throwable {

    String2.log(
        "*** EDDGridLonPM180.generateDatasetsXmlFromErddapCatalog(" + oPublicSourceUrl + ")");

    if (oPublicSourceUrl == null)
      throw new RuntimeException(String2.ERROR + ": 'sourceUrl' is null.");
    if (!oPublicSourceUrl.endsWith("/erddap/"))
      throw new RuntimeException(String2.ERROR + ": 'sourceUrl' doesn't end with '/erddap/'.");

    // get table with datasetID minLon, maxLon
    String query =
        oPublicSourceUrl
            + "tabledap/allDatasets.csv"
            + "?datasetID,title,griddap,minLongitude,maxLongitude"
            + "&dataStructure="
            + SSR.minimalPercentEncode("\"grid\"")
            + "&maxLongitude%3E180"
            + (String2.isSomething(datasetIDRegex)
                ? "&datasetID=" + SSR.minimalPercentEncode("~\"" + datasetIDRegex + "\"")
                : "")
            + "&orderBy(%22datasetID%22)";
    BufferedReader br = null;
    try {
      br = SSR.getBufferedUrlReader(query);
    } catch (Throwable t) {
      if (t.toString().indexOf("no data") >= 0)
        return "<!-- No griddap datasets at that ERDDAP match that regex\n"
            + "     and have longitude values &gt;180. -->\n";
      throw t;
    }
    Table table = new Table();
    table.readASCII(query, br, "", "", 0, 2, "", null, null, null, null, false); // simplify
    PrimitiveArray datasetIDPA = table.findColumn("datasetID");
    PrimitiveArray titlePA = table.findColumn("title");
    PrimitiveArray minLonPA = table.findColumn("minLongitude");
    PrimitiveArray maxLonPA = table.findColumn("maxLongitude");
    PrimitiveArray griddapPA = table.findColumn("griddap");
    StringBuilder sb = new StringBuilder();
    int nRows = table.nRows();
    for (int row = 0; row < nRows; row++) {
      String id = datasetIDPA.getString(row);
      if (datasetIDPA.getString(row).equals("etopo360") || id.endsWith("_Lon0360")) continue;
      sb.append(
          "<dataset type=\"EDDGridLonPM180\" datasetID=\""
              + id
              + "_LonPM180\" active=\"true\">\n"
              + "    <dataset type=\"EDDGridFromErddap\" datasetID=\""
              + id
              + "_LonPM180Child\">\n"
              + "        <!-- "
              + XML.encodeAsXML(titlePA.getString(row))
              + "\n"
              + "             minLon="
              + minLonPA.getString(row)
              + " maxLon="
              + maxLonPA.getString(row)
              + " -->\n"
              +
              // set updateEveryNMillis?  frequent if local ERDDAP, less frequent if remote?
              "        <sourceUrl>"
              + XML.encodeAsXML(griddapPA.getString(row))
              + "</sourceUrl>\n"
              + "    </dataset>\n"
              + "</dataset>\n\n");
    }
    return sb.toString();
  }
}
