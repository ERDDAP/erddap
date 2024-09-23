/*
 * EDDGridAggregateExistingDimension Copyright 2008, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.IntArray;
import com.cohort.array.LongArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.handlers.EDDGridAggregateExistingDimensionHandler;
import gov.noaa.pfel.erddap.handlers.SaxHandlerClass;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;
import java.text.MessageFormat;
import java.util.ArrayList;

/**
 * This class represents a grid dataset created by aggregating for first existing dimension of other
 * datasets. Each child holds a contiguous group of values for the aggregated dimension.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2008-02-04
 */
@SaxHandlerClass(EDDGridAggregateExistingDimensionHandler.class)
public class EDDGridAggregateExistingDimension extends EDDGrid {

  protected EDDGrid childDatasets[];
  protected int childStopsAt[]; // the last valid axisVar0 index for each childDataset

  /**
   * This is used to test equality of axis values. 0=no testing (not recommended). &gt;18 does exact
   * test. Default=20. 1-18 tests that many digits for doubles and hidiv(n,2) for floats.
   */
  protected int matchAxisNDigits = DEFAULT_MATCH_AXIS_N_DIGITS;

  /**
   * This constructs an EDDGridAggregateExistingDimension based on the information in an .xml file.
   * Only the attributes from the first dataset are used for the composite dataset.
   *
   * @param erddap if known in this context, else null
   * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset
   *     type="EDDGridAggregateExistingDimension"&gt; having just been read.
   * @return an EDDGridAggregateExistingDimension. When this returns, xmlReader will have just read
   *     &lt;erddapDatasets&gt;&lt;/dataset&gt; .
   * @throws Throwable if trouble
   */
  @EDDFromXmlMethod
  public static EDDGridAggregateExistingDimension fromXml(Erddap erddap, SimpleXMLReader xmlReader)
      throws Throwable {

    if (verbose) String2.log("\n*** constructing EDDGridAggregateExistingDimension(xmlReader)...");
    String tDatasetID = xmlReader.attributeValue("datasetID");

    // data to be obtained while reading xml
    EDDGrid firstChild = null;
    StringArray tLocalSourceUrls = new StringArray();
    String tAccessibleTo = null;
    String tGraphsAccessibleTo = null;
    boolean tAccessibleViaWMS = true;
    boolean tAccessibleViaFiles = EDStatic.defaultAccessibleViaFiles;
    StringArray tOnChange = new StringArray();
    String tFgdcFile = null;
    String tIso19115File = null;
    int tMatchAxisNDigits = DEFAULT_MATCH_AXIS_N_DIGITS;
    String tDefaultDataQuery = null;
    String tDefaultGraphQuery = null;
    int tnThreads = -1; // interpret invalid values (like -1) as EDStatic.nGridThreads
    boolean tDimensionValuesInMemory = true;

    String tSUServerType = null;
    String tSURegex = null;
    boolean tSURecursive = true;
    String tSUPathRegex = ".*";
    String tSU = null;

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
          if (firstChild == null) {
            EDD edd = EDD.fromXml(erddap, xmlReader.attributeValue("type"), xmlReader);
            if (edd instanceof EDDGrid eddGrid) {
              firstChild = eddGrid;
            } else {
              throw new RuntimeException(
                  "Datasets.xml error: "
                      + "The dataset defined in an "
                      + "EDDGridAggregateExistingDimension must be a subclass of EDDGrid.");
            }
          } else {
            throw new RuntimeException(
                "Datasets.xml error: "
                    + "There can be only one <dataset> defined within an "
                    + "EDDGridAggregateExistingDimension <dataset>.");
          }
        }

      } else if (localTags.equals("<sourceUrl>")) {
      } else if (localTags.equals("</sourceUrl>")) tLocalSourceUrls.add(content);

      // <sourceUrls serverType="thredds" regex=".*\\.nc" recursive="true"
      //  >http://ourocean.jpl.nasa.gov:8080/thredds/dodsC/g1sst/catalog.xml</sourceUrl>
      else if (localTags.equals("<sourceUrls>")) {
        tSUServerType = xmlReader.attributeValue("serverType");
        tSURegex = xmlReader.attributeValue("regex");
        String tr = xmlReader.attributeValue("recursive");
        tSUPathRegex = xmlReader.attributeValue("pathRegex");
        tSURecursive = tr == null ? true : String2.parseBoolean(tr);
      } else if (localTags.equals("</sourceUrls>")) tSU = content;
      else if (localTags.equals("<matchAxisNDigits>")) {
      } else if (localTags.equals("</matchAxisNDigits>"))
        tMatchAxisNDigits = String2.parseInt(content, DEFAULT_MATCH_AXIS_N_DIGITS);
      else if (localTags.equals("<ensureAxisValuesAreEqual>")) {
      } // deprecated
      else if (localTags.equals("</ensureAxisValuesAreEqual>"))
        tMatchAxisNDigits = String2.parseBoolean(content) ? 20 : 0;
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
    return new EDDGridAggregateExistingDimension(
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
        firstChild,
        tLocalSourceUrls.toArray(),
        tSUServerType,
        tSURegex,
        tSURecursive,
        tSUPathRegex,
        tSU,
        tMatchAxisNDigits,
        tnThreads,
        tDimensionValuesInMemory);
  }

  /**
   * The constructor. The axisVariables must be the same and in the same order for each
   * dataVariable.
   *
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
   * @param firstChild
   * @param tLocalSourceUrls the sourceUrls for the other siblings
   * @param tMatchAxisNDigits 0=no checking, 1-18 checks n digits, &gt;18 does exact checking.
   *     Default is 20. This ensures that the axis sourceValues for axisVar 1+ are almostEqual.
   * @throws Throwable if trouble
   */
  public EDDGridAggregateExistingDimension(
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
      EDDGrid firstChild,
      String tLocalSourceUrls[],
      String tSUServerType,
      String tSURegex,
      boolean tSURecursive,
      String tSUPathRegex,
      String tSU,
      int tMatchAxisNDigits,
      int tnThreads,
      boolean tDimensionValuesInMemory)
      throws Throwable {

    if (verbose) String2.log("\n*** constructing EDDGridAggregateExistingDimension " + tDatasetID);
    long constructionStartMillis = System.currentTimeMillis();
    String errorInMethod =
        "Error in EDDGridGridAggregateExistingDimension(" + tDatasetID + ") constructor:\n";

    // save some of the parameters
    className = "EDDGridAggregateExistingDimension";
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
    matchAxisNDigits = tMatchAxisNDigits;
    nThreads = tnThreads; // interpret invalid values (like -1) as EDStatic.nGridThreads
    dimensionValuesInMemory = tDimensionValuesInMemory;

    // if no tLocalSourceURLs, generate from hyrax, thredds, waf, or dodsindex catalog?
    if (tLocalSourceUrls.length == 0 && tSU != null && tSU.length() > 0) {
      if (tSURegex == null || tSURegex.length() == 0) tSURegex = ".*";

      StringArray tsa = new StringArray();
      if (tSUServerType == null)
        throw new RuntimeException(errorInMethod + "<sourceUrls> serverType is null.");
      else if (tSUServerType.toLowerCase().equals("hyrax"))
        tsa.add(FileVisitorDNLS.getUrlsFromHyraxCatalog(tSU, tSURegex, tSURecursive, tSUPathRegex));
      else if (tSUServerType.toLowerCase().equals("waf"))
        tsa.add(FileVisitorDNLS.getUrlsFromWAF(tSU, tSURegex, tSURecursive, tSUPathRegex));
      else if (tSUServerType.toLowerCase().equals("thredds"))
        tsa.add(
            EDDGridFromDap.getUrlsFromThreddsCatalog(tSU, tSURegex, tSUPathRegex, null)
                .toStringArray());
      else if (tSUServerType.toLowerCase().equals("dodsindex"))
        getDodsIndexUrls(tSU, tSURegex, tSURecursive, tSUPathRegex, tsa);
      else
        throw new RuntimeException(
            errorInMethod
                + "<sourceUrls> serverType="
                + tSUServerType
                + " must be \"hyrax\", \"thredds\", \"dodsindex\", or \"waf\".");
      // String2.log("\nchildren=\n" + tsa.toNewlineString());

      // remove firstChild's sourceUrl
      int fc = tsa.indexOf(firstChild.localSourceUrl());
      if (verbose)
        String2.log(
            "firstChild sourceUrl=" + firstChild.localSourceUrl() + "\nis in children at po=" + fc);
      if (fc >= 0) tsa.remove(fc);

      tsa.sort();
      tLocalSourceUrls = tsa.toArray();
    }

    int nChildren = tLocalSourceUrls.length + 1;
    childDatasets = new EDDGrid[nChildren];
    childDatasets[0] = firstChild;
    PrimitiveArray cumSV = firstChild.axisVariables()[0].sourceValues();
    childStopsAt = new int[nChildren];
    childStopsAt[0] = cumSV.size() - 1;

    // create the siblings
    boolean childAccessibleViaFiles =
        childDatasets[0].accessibleViaFiles; // is at least one 'true'?
    for (int sib = 0; sib < nChildren - 1; sib++) {
      if (reallyVerbose) String2.log("\n+++ Creating childDatasets[" + (sib + 1) + "]\n");
      EDDGrid sibling = firstChild.sibling(tLocalSourceUrls[sib], 1, matchAxisNDigits, true);
      childDatasets[sib + 1] = sibling;
      PrimitiveArray sourceValues0 = sibling.axisVariables()[0].sourceValues();
      cumSV.append(sourceValues0);
      childStopsAt[sib + 1] = cumSV.size() - 1;
      if (reallyVerbose)
        String2.log(
            "\n+++ childDatasets["
                + (sib + 1)
                + "] #0="
                + sourceValues0.getString(0)
                + " #"
                + sourceValues0.size()
                + "="
                + sourceValues0.getString(sourceValues0.size() - 1));
      // sourceValues0=" + sourceValues0 + "\n");
      if (childDatasets[sib + 1].accessibleViaFiles) childAccessibleViaFiles = true;
    }
    accessibleViaFiles = EDStatic.filesActive && tAccessibleViaFiles && childAccessibleViaFiles;

    // create the aggregate dataset
    if (reallyVerbose) String2.log("\n+++ Creating the aggregate dataset\n");
    setReloadEveryNMinutes(firstChild.getReloadEveryNMinutes());

    localSourceUrl = firstChild.localSourceUrl();
    addGlobalAttributes = firstChild.addGlobalAttributes();
    sourceGlobalAttributes = firstChild.sourceGlobalAttributes();
    combinedGlobalAttributes = firstChild.combinedGlobalAttributes();
    // and clear searchString of children?

    // make the local axisVariables
    int nAv = firstChild.axisVariables.length;
    axisVariables = new EDVGridAxis[nAv];
    System.arraycopy(firstChild.axisVariables, 0, axisVariables, 0, nAv);
    lonIndex = firstChild.lonIndex;
    latIndex = firstChild.latIndex;
    altIndex = firstChild.altIndex;
    depthIndex = firstChild.depthIndex;
    timeIndex = firstChild.timeIndex;
    // make the combined axisVariable[0]
    EDVGridAxis av0 = axisVariables[0];
    String sn = av0.sourceName();
    String dn = av0.destinationName();
    Attributes sa = av0.sourceAttributes();
    Attributes aa = av0.addAttributes();
    axisVariables[0] = makeAxisVariable(tDatasetID, 0, sn, dn, sa, aa, cumSV);

    // make the local dataVariables
    int nDv = firstChild.dataVariables.length;
    dataVariables = new EDV[nDv];
    System.arraycopy(firstChild.dataVariables, 0, dataVariables, 0, nDv);

    // ensure the setup is valid
    ensureValid();

    // If any child is a FromErddap, try to subscribe to the remote dataset.
    for (int c = 0; c < childDatasets.length; c++)
      if (childDatasets[c] instanceof FromErddap) tryToSubscribeToChildFromErddap(childDatasets[c]);

    // finally
    long cTime = System.currentTimeMillis() - constructionStartMillis;
    if (verbose)
      String2.log(
          (debugMode ? "\n" + toString() : "")
              + "\n*** EDDGridAggregateExistingDimension "
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
   * This returns a list of childDatasetIDs. Most dataset types don't have any children. A few, like
   * EDDGridSideBySide do, so they overwrite this method to return the IDs.
   *
   * @return a new list of childDatasetIDs.
   */
  @Override
  public StringArray childDatasetIDs() {
    StringArray sa = new StringArray();
    try {
      for (int i = 0; i < childDatasets.length; i++) sa.add(childDatasets[i].datasetID());
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
    throw new SimpleException(
        "Error: EDDGridAggregateExistingDimension doesn't support method=\"sibling\".");
  }

  /**
   * This gets a list of URLs from the sourceUrl, perhaps recursively.
   *
   * @param startUrl is the first URL to look at. E.g.,
   *     https://opendap.jpl.nasa.gov/opendap/GeodeticsGravity/tellus/L3/ocean_mass/RL05/netcdf/contents.html
   *     [was http://www.marine.csiro.au/dods/nph-dods/dods-data/bl/BRAN2.1/bodas/ ] It MUST end
   *     with a / or a file name (e.g., catalog.html).
   * @param regex The regex is used to test file names, not directory names. E.g. .*\.nc to catch
   *     all .nc files, or use .* to catch all files.
   * @param recursive if true, this method will pursue subdirectories
   * @param sourceUrls The results will be placed here.
   * @throws Exception if trouble. However, it isn't an error if a URL returns an error message.
   */
  public static void getDodsIndexUrls(
      String startUrl, String regex, boolean recursive, String pathRegex, StringArray sourceUrls)
      throws Exception {

    // get the document as one string per line
    String baseDir = File2.getDirectory(startUrl);
    if (pathRegex == null || pathRegex.length() == 0) pathRegex = ".*";
    int onSourceUrls = sourceUrls.size();
    String regexHtml = regex + "\\.html"; // link href will have .html at end
    ArrayList<String> lines = null;
    try {
      lines = SSR.getUrlResponseArrayList(startUrl);
    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      return;
    }
    int nextLine = 0;
    int nLines = lines.size();
    if (nLines == 0) {
      String2.log("WARNING: The document has 0 lines.");
      return;
    }

    // look for <pre>
    String line = lines.get(nextLine++);
    String lcLine = line.toLowerCase();
    while (lcLine.indexOf("<pre>") < 0 && nextLine < nLines) {
      line = lines.get(nextLine++);
      lcLine = line.toLowerCase();
    }
    if (nextLine >= nLines) {
      String2.log("WARNING: <pre> and <PRE> not found in the document.");
      return;
    }

    /*
    <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 3.2 Final//EN">
    <html>
     <head>
      <title>DODS Index of /dods-data/bl/BRAN2.1/bodas</title>
     </head>
     <body>
    <h1>DODS Index of /dods-data/bl/BRAN2.1/bodas</h1>
    <pre><img src="/icons/blank.gif" alt="Icon "> <A HREF=?C=N;O=D>Name</a>                    <A HREF=?C=M;O=A>Last modified</a>      <A HREF=?C=S;O=A>Size</a>  <A HREF=?C=D;O=A>Description</a><hr><img src="/icons/back.gif" alt="[DIR]"> <A HREF=../>Parent Directory</a>                             -
    <img src="/icons/unknown.gif" alt="[   ]"> <A HREF=19921014.bodas_eta.nc.html>19921014.bodas_eta.nc</a>   17-Apr-2007 11:43  8.8M
    ...
    <hr></pre>
    */

    // extract url from each subsequent line, until </pre> or </PRE>
    while (nextLine < nLines) {
      line = lines.get(nextLine++);
      lcLine = line.toLowerCase();
      if (lcLine.indexOf("</pre>") >= 0) break;
      int po = lcLine.indexOf("<a href="); // No quotes around link!  Who wrote that server?!
      if (po >= 0) {
        int po2 = line.indexOf('>', po + 8);
        if (po2 >= 0) {
          // if quotes around link, remove them
          String tName = line.substring(po + 8, po2).trim();
          if (tName.startsWith("\"")) tName = tName.substring(1).trim();
          if (tName.endsWith("\"")) tName = tName.substring(0, tName.length() - 1).trim();

          if (tName.startsWith(".")) {
            // skip parent dir (../)

          } else if (tName.endsWith("/")) {
            // it's a directory
            String tUrl = tName.toLowerCase().startsWith("http") ? tName : baseDir + tName;
            if (recursive && tUrl.matches(pathRegex)) {
              getDodsIndexUrls(tUrl, regex, recursive, pathRegex, sourceUrls);
            }
          } else if (tName.matches(regexHtml)) {
            // it's a matching URL
            tName = tName.substring(0, tName.length() - 5); // remove .html from end
            sourceUrls.add(tName.toLowerCase().startsWith("http") ? tName : baseDir + tName);
          }
        }
      }
    }
    if (verbose)
      String2.log("  getDodsIndexUrls just found " + (sourceUrls.size() - onSourceUrls) + " URLs.");
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

    // parcel first dimension's constraints to appropriate datasets
    int nChildren = childStopsAt.length;
    int nAv = axisVariables.length;
    int nDv = tDataVariables.length;
    PrimitiveArray[] cumResults = null;
    int index = tConstraints.get(0);
    int stride = tConstraints.get(1);
    int stop = tConstraints.get(2);
    // find currentDataset (associated with index)
    int currentStart = index;
    int currentDataset = 0;
    while (index > childStopsAt[currentDataset]) currentDataset++;
    IntArray childConstraints = (IntArray) tConstraints.clone();

    // walk through the requested index values
    while (index <= stop) {
      // find nextDataset (associated with next iteration's index)
      int nextDataset = currentDataset;
      while (nextDataset < nChildren && index + stride > childStopsAt[nextDataset])
        nextDataset++; // ok if >= nDatasets

      // get a chunk of data related to current chunk of indexes?
      if (nextDataset != currentDataset
          || // next iteration will be a different dataset
          index + stride > stop) { // this is last iteration
        // get currentStart:stride:index
        int currentDatasetStartsAt = currentDataset == 0 ? 0 : childStopsAt[currentDataset - 1] + 1;
        childConstraints.set(0, currentStart - currentDatasetStartsAt);
        childConstraints.set(2, index - currentDatasetStartsAt);
        if (reallyVerbose)
          String2.log(
              "  currentDataset="
                  + currentDataset
                  + "  datasetStartsAt="
                  + currentDatasetStartsAt
                  + "  localStart="
                  + childConstraints.get(0)
                  + "  localStop="
                  + childConstraints.get(2));
        PrimitiveArray[] tResults =
            childDatasets[currentDataset].getSourceData(
                language, null, null, tDataVariables, childConstraints);
        // childDataset has already checked that axis values are as *it* expects
        if (cumResults == null) {
          if (matchAxisNDigits <= 0) {
            // make axis values exactly as expected by aggregate dataset
            for (int av = 1; av < nAv; av++)
              tResults[av] =
                  axisVariables[av]
                      .sourceValues()
                      .subset(
                          childConstraints.get(av * 3 + 0),
                          childConstraints.get(av * 3 + 1),
                          childConstraints.get(av * 3 + 2));
          }
          cumResults = tResults;
        } else {
          cumResults[0].append(tResults[0]);
          for (int dv = 0; dv < nDv; dv++) cumResults[nAv + dv].append(tResults[nAv + dv]);
        }

        currentDataset = nextDataset;
        currentStart = index + stride;
      }

      // increment index
      index += stride;
    }

    return cumResults;
  }

  /**
   * This returns a fileTable with valid files (or null if unavailable or any trouble). This is a
   * copy of any internal data, so client can modify the contents.
   *
   * @param language the index of the selected language
   * @param nextPath is the partial path (with trailing slash) to be appended onto the local fileDir
   *     (or wherever files are, even url).
   * @return null if trouble, or Object[3] where [0] is a sorted table with file "Name" (String),
   *     "Last modified" (long millis), "Size" (long), and "Description" (String, but usually no
   *     content), [1] is a sorted String[] with the short names of directories that are 1 level
   *     lower, and [2] is the local directory corresponding to this (or null, if not a local dir).
   */
  @Override
  public Object[] accessibleViaFilesFileTable(int language, String nextPath) {
    if (!accessibleViaFiles) return null;
    try {
      int nChild = childDatasets.length;

      // if nextPath is nothing, return list of child id's as directories
      if (nextPath.length() == 0) {
        Table table = new Table();
        table.addColumn("Name", new StringArray());
        table.addColumn("Last modified", new LongArray().setMaxIsMV(true));
        table.addColumn("Size", new LongArray().setMaxIsMV(true));
        table.addColumn("Description", new StringArray());
        StringArray subDirs = new StringArray();
        for (int child = 0; child < nChild; child++) {
          if (childDatasets[child].accessibleViaFiles)
            subDirs.add(childDatasets[child].datasetID());
        }
        subDirs.sortIgnoreCase();
        return new Object[] {table, subDirs.toStringArray(), null};
      }

      // ensure start of nextPath is a child datasetID
      int po = nextPath.indexOf('/');
      if (po < 0) {
        String2.log("ERROR: no slash in nextPath.");
        return null;
      }

      // start of nextPath is a child datasetID
      String tID = nextPath.substring(0, po);
      nextPath = nextPath.substring(po + 1);
      for (int child = 0; child < nChild; child++) {
        EDD edd = childDatasets[child];
        if (tID.equals(edd.datasetID())) return edd.accessibleViaFilesFileTable(language, nextPath);
      }
      // or it isn't
      String2.log("ERROR: " + tID + " isn't a child's datasetID.");
      return null;

    } catch (Exception e) {
      String2.log(MustBe.throwableToString(e));
      return null;
    }
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
    String msg = datasetID() + " accessibleViaFilesGetLocal(" + relativeFileName + "): ";

    try {

      // first dir -> childDatasetName
      int po = relativeFileName.indexOf('/');
      if (po <= 0) {
        String2.log(msg + "no '/' in relatveFileName, so no child datasetID.");
        return null;
      }
      String childID = relativeFileName.substring(0, po);
      String relativeFileName2 = relativeFileName.substring(po + 1);

      // which child?
      int nChild = childDatasets.length;
      for (int c = 0; c < nChild; c++) {
        if (childID.equals(childDatasets[c].datasetID())) {
          // then redirect request to that child
          return childDatasets[c].accessibleViaFilesGetLocal(language, relativeFileName2);
        }
      }
      String2.log(msg + "childID=" + childID + " not found.");
      return null;

    } catch (Exception e) {
      String2.log(msg + ":\n" + MustBe.throwableToString(e));
      return null;
    }
  }

  /**
   * This generates datasets.xml for an aggregated dataset from a Hyrax or THREDDS catalog.
   *
   * @param serverType Currently, only "hyrax", "thredds", and "waf" are supported (case
   *     insensitive)
   * @param startUrl the url of the current web page (with a hyrax catalog) e.g., <br>
   *     hyrax: "http://dods.jpl.nasa.gov/opendap/ocean_wind/ccmp/L3.5a/data/flk/1988/contents.html"
   *     <br>
   *     thredds:
   *     "https://thredds1.pfeg.noaa.gov/thredds/catalog/Satellite/aggregsatMH/chla/catalog.xml"
   * @param fileNameRegex e.g., <br>
   *     hyrax: "pentad.*flk\\.nc\\.gz" <br>
   *     thredds: ".*"
   * @return a suggested chunk of xml for this dataset for use in datasets.xml
   * @throws Throwable if trouble, e.g., if no Grid or Array variables are found. If no trouble,
   *     then a valid dataset.xml chunk has been returned.
   */
  public static String generateDatasetsXml(
      String serverType, String startUrl, String fileNameRegex, int tReloadEveryNMinutes)
      throws Throwable {

    startUrl = EDStatic.updateUrls(startUrl); // http: to https:
    String2.log(
        "\n*** EDDGridAggregateExistingDimension.generateDatasetsXml"
            + "\nserverType="
            + serverType
            + " startUrl="
            + startUrl
            + "\nfileNameRegex="
            + fileNameRegex
            + " reloadEveryNMinutes="
            + tReloadEveryNMinutes);
    long time = System.currentTimeMillis();

    StringBuilder sb = new StringBuilder();
    String sa[];
    serverType = serverType.toLowerCase();
    boolean recursive = true;
    String pathRegex = ".*";
    if ("hyrax".equals(serverType)) {
      Test.ensureTrue(
          startUrl.endsWith("/contents.html"), "startUrl must end with '/contents.html'.");
      sa =
          FileVisitorDNLS.getUrlsFromHyraxCatalog(
              File2.getDirectory(startUrl), fileNameRegex, recursive, pathRegex);
    } else if ("thredds".equals(serverType)) {
      Test.ensureTrue(startUrl.endsWith("/catalog.xml"), "startUrl must end with '/catalog.xml'.");
      sa =
          EDDGridFromDap.getUrlsFromThreddsCatalog(startUrl, fileNameRegex, pathRegex, null)
              .toStringArray();
    } else if ("waf".equals(serverType)) {
      sa = FileVisitorDNLS.getUrlsFromWAF(startUrl, fileNameRegex, recursive, pathRegex);
    } else {
      throw new RuntimeException("ERROR: serverType must be \"hyrax\", \"thredds\", or \"waf\".");
    }

    if (sa.length == 0) throw new RuntimeException("ERROR: No matching URLs were found.");

    if (sa.length == 1)
      return EDDGridFromDap.generateDatasetsXml(
          sa[0], null, null, null, tReloadEveryNMinutes, null);

    // multiple URLs, so aggregate them
    // outer EDDGridAggregateExistingDimension
    sb.append(
        "<dataset type=\"EDDGridAggregateExistingDimension\" datasetID=\""
            + suggestDatasetID(startUrl + "?" + fileNameRegex)
            + "\" active=\"true\">\n"
            + "\n");

    // first child -- fully defined
    sb.append(
        EDDGridFromDap.generateDatasetsXml(sa[0], null, null, null, tReloadEveryNMinutes, null));

    // other children
    // for (int i = 1; i < sa.length; i++)
    //    sb.append("<sourceUrl>" + sa[i] + "</sourceUrl>\n");

    sb.append(
        "<sourceUrls serverType=\""
            + serverType
            + "\" regex=\""
            + XML.encodeAsXML(fileNameRegex)
            + "\" recursive=\""
            + recursive
            + "\" pathRegex=\""
            + XML.encodeAsXML(pathRegex)
            + "\">"
            + startUrl
            + "</sourceUrls>\n");

    // end
    sb.append("\n" + "</dataset>\n" + "\n");

    String2.log(
        "\n\n*** generateDatasetsXml finished successfully; time="
            + (System.currentTimeMillis() - time)
            + "ms\n\n");
    return sb.toString();
  }
}
