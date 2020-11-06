/* 
 * EDDGridAggregateExistingDimension Copyright 2008, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.IntArray;
import com.cohort.array.LongArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.text.MessageFormat;
import java.util.ArrayList;


/** 
 * This class represents a grid dataset created by aggregating 
 * for first existing dimension of other datasets.
 * Each child holds a contiguous group of values for the aggregated dimension.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2008-02-04
 */
public class EDDGridAggregateExistingDimension extends EDDGrid { 

    protected EDDGrid childDatasets[];
    protected int childStopsAt[]; //the last valid axisVar0 index for each childDataset

    /** 
     * This is used to test equality of axis values. 
     * 0=no testing (not recommended). 
     * &gt;18 does exact test. Default=20.
     * 1-18 tests that many digits for doubles and hidiv(n,2) for floats.
     */
    protected int matchAxisNDigits = DEFAULT_MATCH_AXIS_N_DIGITS;

    /**
     * This constructs an EDDGridAggregateExistingDimension based on the information in an .xml file.
     * Only the attributes from the first dataset are used for the composite
     * dataset.
     * 
     * @param erddap if known in this context, else null
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDGridAggregateExistingDimension"&gt; 
     *    having just been read.  
     * @return an EDDGridAggregateExistingDimension.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDGridAggregateExistingDimension fromXml(Erddap erddap, 
        SimpleXMLReader xmlReader) throws Throwable {

        if (verbose) String2.log("\n*** constructing EDDGridAggregateExistingDimension(xmlReader)...");
        String tDatasetID = xmlReader.attributeValue("datasetID"); 

        //data to be obtained while reading xml
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
        int tnThreads = -1; //interpret invalid values (like -1) as EDStatic.nGridThreads
        boolean tDimensionValuesInMemory = true;

        String tSUServerType = null;
        String tSURegex = null;
        boolean tSURecursive = true;
        String tSUPathRegex = ".*";
        String tSU = null; 

        //process the tags
        String startOfTags = xmlReader.allTags();
        int startOfTagsN = xmlReader.stackSize();
        int startOfTagsLength = startOfTags.length();

        while (true) {
            xmlReader.nextTag();
            String tags = xmlReader.allTags();
            String content = xmlReader.content();
            if (xmlReader.stackSize() == startOfTagsN) 
                break; //the </dataset> tag
            String localTags = tags.substring(startOfTagsLength);

            //try to make the tag names as consistent, descriptive and readable as possible
            if (localTags.equals("<dataset>")) {
                if ("false".equals(xmlReader.attributeValue("active"))) {
                    //skip it - read to </dataset>
                    if (verbose) String2.log("  skipping datasetID=" + xmlReader.attributeValue("datasetID") + 
                        " because active=\"false\".");
                    while (xmlReader.stackSize() != startOfTagsN + 1 ||
                           !xmlReader.allTags().substring(startOfTagsLength).equals("</dataset>")) {
                        xmlReader.nextTag();
                        //String2.log("  skippping tags: " + xmlReader.allTags());
                    }

                } else {
                    if (firstChild == null) {
                        EDD edd = EDD.fromXml(erddap, xmlReader.attributeValue("type"), xmlReader);
                        if (edd instanceof EDDGrid) {
                            firstChild = (EDDGrid)edd;
                        } else {
                            throw new RuntimeException("Datasets.xml error: " +
                                "The dataset defined in an " +
                                "EDDGridAggregateExistingDimension must be a subclass of EDDGrid.");
                        }
                    } else {
                        throw new RuntimeException("Datasets.xml error: " +
                            "There can be only one <dataset> defined within an " +
                            "EDDGridAggregateExistingDimension <dataset>.");
                    }
                }

            } 
            else if (localTags.equals( "<sourceUrl>")) {}
            else if (localTags.equals("</sourceUrl>")) 
                tLocalSourceUrls.add(content);

            //<sourceUrls serverType="thredds" regex=".*\\.nc" recursive="true"
            //  >http://ourocean.jpl.nasa.gov:8080/thredds/dodsC/g1sst/catalog.xml</sourceUrl>
            else if (localTags.equals( "<sourceUrls>")) {
                tSUServerType = xmlReader.attributeValue("serverType");
                tSURegex      = xmlReader.attributeValue("regex");
                String tr     = xmlReader.attributeValue("recursive");
                tSUPathRegex  = xmlReader.attributeValue("pathRegex");
                tSURecursive  = tr == null? true : String2.parseBoolean(tr);
            }
            else if (localTags.equals("</sourceUrls>")) tSU = content; 
            else if (localTags.equals( "<matchAxisNDigits>")) {}
            else if (localTags.equals("</matchAxisNDigits>")) 
                tMatchAxisNDigits = String2.parseInt(content, DEFAULT_MATCH_AXIS_N_DIGITS); 
            else if (localTags.equals( "<ensureAxisValuesAreEqual>")) {} //deprecated
            else if (localTags.equals("</ensureAxisValuesAreEqual>")) 
                tMatchAxisNDigits = String2.parseBoolean(content)? 20 : 0;
            else if (localTags.equals( "<accessibleTo>")) {}
            else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
            else if (localTags.equals( "<graphsAccessibleTo>")) {}
            else if (localTags.equals("</graphsAccessibleTo>")) tGraphsAccessibleTo = content;
            else if (localTags.equals( "<accessibleViaWMS>")) {}
            else if (localTags.equals("</accessibleViaWMS>")) tAccessibleViaWMS = String2.parseBoolean(content);
            else if (localTags.equals( "<accessibleViaFiles>")) {}
            else if (localTags.equals("</accessibleViaFiles>")) tAccessibleViaFiles = String2.parseBoolean(content); 
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
            else if (localTags.equals( "<nThreads>")) {}
            else if (localTags.equals("</nThreads>")) tnThreads = String2.parseInt(content); 
            else if (localTags.equals( "<dimensionValuesInMemory>")) {}
            else if (localTags.equals("</dimensionValuesInMemory>")) tDimensionValuesInMemory = String2.parseBoolean(content);

            else xmlReader.unexpectedTagException();
        }

        //make the main dataset based on the information gathered
        return new EDDGridAggregateExistingDimension(tDatasetID, 
            tAccessibleTo, tGraphsAccessibleTo, tAccessibleViaWMS,
            tAccessibleViaFiles, 
            tOnChange, tFgdcFile, tIso19115File,
            tDefaultDataQuery, tDefaultGraphQuery,
            firstChild, tLocalSourceUrls.toArray(),
            tSUServerType, tSURegex, tSURecursive, tSUPathRegex, tSU, 
            tMatchAxisNDigits, tnThreads, tDimensionValuesInMemory);

    }

    /**
     * The constructor.
     * The axisVariables must be the same and in the same
     * order for each dataVariable.
     *
     * @param tDatasetID
     * @param tAccessibleTo is a space separated list of 0 or more
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
     * @param firstChild
     * @param tLocalSourceUrls the sourceUrls for the other siblings
     * @param tMatchAxisNDigits 0=no checking, 1-18 checks n digits,
     *   &gt;18 does exact checking.  Default is 20. 
     *   This ensures that the axis sourceValues for axisVar 1+ are almostEqual.   
     * @throws Throwable if trouble
     */
    public EDDGridAggregateExistingDimension(String tDatasetID, 
        String tAccessibleTo, String tGraphsAccessibleTo, boolean tAccessibleViaWMS, 
        boolean tAccessibleViaFiles, 
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tDefaultDataQuery, String tDefaultGraphQuery,
        EDDGrid firstChild, String tLocalSourceUrls[], 
        String tSUServerType, String tSURegex, boolean tSURecursive, 
        String tSUPathRegex, String tSU, int tMatchAxisNDigits, 
        int tnThreads, boolean tDimensionValuesInMemory) 
        throws Throwable {

        if (verbose) String2.log(
            "\n*** constructing EDDGridAggregateExistingDimension " + tDatasetID); 
        long constructionStartMillis = System.currentTimeMillis();
        String errorInMethod = "Error in EDDGridGridAggregateExistingDimension(" + 
            tDatasetID + ") constructor:\n";
            
        //save some of the parameters
        className = "EDDGridAggregateExistingDimension"; 
        datasetID = tDatasetID;
        setAccessibleTo(tAccessibleTo);
        setGraphsAccessibleTo(tGraphsAccessibleTo);
        if (!tAccessibleViaWMS) 
            accessibleViaWMS = String2.canonical(
                MessageFormat.format(EDStatic.noXxx, "WMS"));
        onChange = tOnChange;
        fgdcFile = tFgdcFile;
        iso19115File = tIso19115File;
        defaultDataQuery = tDefaultDataQuery;
        defaultGraphQuery = tDefaultGraphQuery;
        matchAxisNDigits = tMatchAxisNDigits;
        nThreads = tnThreads; //interpret invalid values (like -1) as EDStatic.nGridThreads
        dimensionValuesInMemory = tDimensionValuesInMemory; 

        //if no tLocalSourceURLs, generate from hyrax, thredds, waf, or dodsindex catalog?
        if (tLocalSourceUrls.length == 0 && tSU != null && tSU.length() > 0) {
            if (tSURegex == null || tSURegex.length() == 0)
                tSURegex = ".*";

            StringArray tsa = new StringArray();
            if (tSUServerType == null)
                throw new RuntimeException(errorInMethod + "<sourceUrls> serverType is null.");
            else if (tSUServerType.toLowerCase().equals("hyrax"))
                tsa.add(FileVisitorDNLS.getUrlsFromHyraxCatalog(tSU, tSURegex, tSURecursive, tSUPathRegex));
            else if (tSUServerType.toLowerCase().equals("waf"))
                tsa.add(FileVisitorDNLS.getUrlsFromWAF(tSU, tSURegex, tSURecursive, tSUPathRegex));
            else if (tSUServerType.toLowerCase().equals("thredds"))
                tsa.add(EDDGridFromDap.getUrlsFromThreddsCatalog(tSU, tSURegex, tSUPathRegex, null).toStringArray());
            else if (tSUServerType.toLowerCase().equals("dodsindex"))
                getDodsIndexUrls(tSU, tSURegex, tSURecursive, tSUPathRegex, tsa);
            else throw new RuntimeException(errorInMethod + 
                "<sourceUrls> serverType=" + tSUServerType + " must be \"hyrax\", \"thredds\", \"dodsindex\", or \"waf\".");
            //String2.log("\nchildren=\n" + tsa.toNewlineString());

            //remove firstChild's sourceUrl
            int fc = tsa.indexOf(firstChild.localSourceUrl());
            if (verbose) String2.log("firstChild sourceUrl=" + firstChild.localSourceUrl() + 
                "\nis in children at po=" + fc);
            if (fc >= 0)
                tsa.remove(fc);

            tsa.sort();
            tLocalSourceUrls = tsa.toArray();
        }

        int nChildren = tLocalSourceUrls.length + 1;
        childDatasets = new EDDGrid[nChildren];
        childDatasets[0] = firstChild;
        PrimitiveArray cumSV = firstChild.axisVariables()[0].sourceValues();
        childStopsAt = new int[nChildren];
        childStopsAt[0] = cumSV.size() - 1;

        //create the siblings
        boolean childAccessibleViaFiles = childDatasets[0].accessibleViaFiles; //is at least one 'true'?
        for (int sib = 0; sib < nChildren - 1; sib++) {
            if (reallyVerbose) String2.log("\n+++ Creating childDatasets[" + (sib+1) + "]\n");
            EDDGrid sibling = firstChild.sibling(tLocalSourceUrls[sib], 
                1, matchAxisNDigits, true);
            childDatasets[sib + 1] = sibling;
            PrimitiveArray sourceValues0 = sibling.axisVariables()[0].sourceValues();
            cumSV.append(sourceValues0);
            childStopsAt[sib + 1] = cumSV.size() - 1;
            if (reallyVerbose) String2.log("\n+++ childDatasets[" + (sib+1) + 
                "] #0=" + sourceValues0.getString(0) +
                " #" + sourceValues0.size() + "=" + sourceValues0.getString(sourceValues0.size() - 1));
                //sourceValues0=" + sourceValues0 + "\n");
            if (childDatasets[sib + 1].accessibleViaFiles) 
                childAccessibleViaFiles = true;
        }
        accessibleViaFiles = EDStatic.filesActive && tAccessibleViaFiles && childAccessibleViaFiles;

        //create the aggregate dataset
        if (reallyVerbose) String2.log("\n+++ Creating the aggregate dataset\n");
        setReloadEveryNMinutes(firstChild.getReloadEveryNMinutes());
      
        localSourceUrl = firstChild.localSourceUrl();
        addGlobalAttributes = firstChild.addGlobalAttributes();
        sourceGlobalAttributes = firstChild.sourceGlobalAttributes();
        combinedGlobalAttributes = firstChild.combinedGlobalAttributes();
        //and clear searchString of children?

        //make the local axisVariables
        int nAv = firstChild.axisVariables.length;
        axisVariables = new EDVGridAxis[nAv];
        System.arraycopy(firstChild.axisVariables, 0, axisVariables, 0, nAv);
        lonIndex   = firstChild.lonIndex;
        latIndex   = firstChild.latIndex;
        altIndex   = firstChild.altIndex;
        depthIndex = firstChild.depthIndex;
        timeIndex  = firstChild.timeIndex;
        //make the combined axisVariable[0]
        EDVGridAxis av0 = axisVariables[0];
        String sn = av0.sourceName();
        String dn = av0.destinationName();
        Attributes sa = av0.sourceAttributes();
        Attributes aa = av0.addAttributes();
            axisVariables[0] = makeAxisVariable(tDatasetID, 0, sn, dn, sa, aa, cumSV); 

        //make the local dataVariables
        int nDv = firstChild.dataVariables.length;
        dataVariables = new EDV[nDv];
        System.arraycopy(firstChild.dataVariables, 0, dataVariables, 0, nDv);

        //ensure the setup is valid
        ensureValid();

        //If any child is a FromErddap, try to subscribe to the remote dataset.
        for (int c = 0; c < childDatasets.length; c++)
            if (childDatasets[c] instanceof FromErddap) 
                tryToSubscribeToChildFromErddap(childDatasets[c]);

        //finally
        long cTime = System.currentTimeMillis() - constructionStartMillis;
        if (verbose) String2.log(
            (debugMode? "\n" + toString() : "") +
            "\n*** EDDGridAggregateExistingDimension " + datasetID + " constructor finished. TIME=" + 
            cTime + "ms" + (cTime >= 10000? "  (>10s!)" : "") + "\n"); 

        //very last thing: saveDimensionValuesInFile
        if (!dimensionValuesInMemory)
            saveDimensionValuesInFile();
    }

    /**
     * This returns a list of childDatasetIDs.
     * Most dataset types don't have any children. A few, like
     * EDDGridSideBySide do, so they overwrite this method to return the IDs.
     *
     * @return a new list of childDatasetIDs. 
     */
    public StringArray childDatasetIDs() {
        StringArray sa = new StringArray();
        try {
            for (int i = 0; i < childDatasets.length; i++)  
                sa.add(childDatasets[i].datasetID());
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
    public EDDGrid sibling(String tLocalSourceUrl, int firstAxisToMatch, 
        int matchAxisNDigits, boolean shareInfo) throws Throwable {
        throw new SimpleException( 
            "Error: EDDGridAggregateExistingDimension doesn't support method=\"sibling\".");
    }

    /**
     * This gets a list of URLs from the sourceUrl, perhaps recursively.
     *
     * @param startUrl is the first URL to look at.
     *   E.g., https://opendap.jpl.nasa.gov/opendap/GeodeticsGravity/tellus/L3/ocean_mass/RL05/netcdf/contents.html
     *   [was http://www.marine.csiro.au/dods/nph-dods/dods-data/bl/BRAN2.1/bodas/ ]
     *   It MUST end with a / or a file name (e.g., catalog.html).
     * @param regex The regex is used to test file names, not directory names.
     *   E.g. .*\.nc to catch all .nc files, or use .* to catch all files.
     * @param recursive if true, this method will pursue subdirectories
     * @param sourceUrls The results will be placed here.
     * @throws Exception if trouble.  However, it isn't an error if a URL returns
     *   an error message.
     */
    public static void getDodsIndexUrls(String startUrl, String regex, 
        boolean recursive, String pathRegex, StringArray sourceUrls) throws Exception {

        //get the document as one string per line
        String baseDir = File2.getDirectory(startUrl);
        if (pathRegex == null || pathRegex.length() == 0)
            pathRegex = ".*";
        int onSourceUrls = sourceUrls.size();
        String regexHtml = regex + "\\.html"; //link href will have .html at end
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

        //look for <pre>
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

        //extract url from each subsequent line, until </pre> or </PRE>
        while (nextLine < nLines) {
            line = lines.get(nextLine++);
            lcLine = line.toLowerCase();
            if (lcLine.indexOf("</pre>") >= 0)
                break;
            int po = lcLine.indexOf("<a href=");  //No quotes around link!  Who wrote that server?!
            if (po >= 0) {
                int po2 = line.indexOf('>', po + 8);
                if (po2 >= 0) {
                    //if quotes around link, remove them
                    String tName = line.substring(po + 8, po2).trim();
                    if (tName.startsWith("\"")) 
                        tName = tName.substring(1).trim();
                    if (tName.endsWith("\"")) 
                        tName = tName.substring(0, tName.length() - 1).trim();
                    
                    if (tName.startsWith(".")) {
                        //skip parent dir (../) 

                    } else if (tName.endsWith("/")) {
                        //it's a directory
                        String tUrl = tName.toLowerCase().startsWith("http")? tName : baseDir + tName;
                        if (recursive && tUrl.matches(pathRegex)) {                            
                            getDodsIndexUrls(tUrl, regex, recursive, pathRegex, sourceUrls);
                        }
                    } else if (tName.matches(regexHtml)) {
                        //it's a matching URL
                        tName = tName.substring(0, tName.length() - 5); //remove .html from end
                        sourceUrls.add(tName.toLowerCase().startsWith("http")? tName : baseDir + tName);
                    }
                }
            }
        }
        if (verbose)
            String2.log("  getDodsIndexUrls just found " + (sourceUrls.size() - onSourceUrls) + " URLs.");
    }

    /** 
     * This gets data (not yet standardized) from the data 
     * source for this EDDGrid.     
     * Because this is called by GridDataAccessor, the request won't be the 
     * full user's request, but will be a partial request (for less than
     * EDStatic.partialRequestMaxBytes).
     * 
     * @param tDirTable If EDDGridFromFiles, this MAY be the dirTable, else null. 
     * @param tFileTable If EDDGridFromFiles, this MAY be the fileTable, else null. 
     * @param tDataVariables EDV[] with just the requested data variables
     * @param tConstraints  int[nAxisVariables*3] 
     *   where av*3+0=startIndex, av*3+1=stride, av*3+2=stopIndex.
     *   AxisVariables are counted left to right, e.g., sst[0=time][1=lat][2=lon].
     * @return a PrimitiveArray[] where the first axisVariables.length elements
     *   are the axisValues and the next tDataVariables.length elements
     *   are the dataValues.
     *   Both the axisValues and dataValues are straight from the source,
     *   not modified.
     * @throws Throwable if trouble (notably, WaitThenTryAgainException)
     */
    public PrimitiveArray[] getSourceData(Table tDirTable, Table tFileTable,
        EDV tDataVariables[], IntArray tConstraints) 
        throws Throwable {

        //parcel first dimension's constraints to appropriate datasets
        int nChildren = childStopsAt.length;
        int nAv = axisVariables.length;
        int nDv = tDataVariables.length;
        PrimitiveArray[] cumResults = null;
        int index = tConstraints.get(0);
        int stride = tConstraints.get(1);
        int stop = tConstraints.get(2);
        //find currentDataset (associated with index)
        int currentStart = index;
        int currentDataset = 0;  
        while (index > childStopsAt[currentDataset])
            currentDataset++;
        IntArray childConstraints = (IntArray)tConstraints.clone();

        //walk through the requested index values
        while (index <= stop) {
            //find nextDataset (associated with next iteration's index)
            int nextDataset = currentDataset; 
            while (nextDataset < nChildren && index + stride > childStopsAt[nextDataset])
                nextDataset++; //ok if >= nDatasets

            //get a chunk of data related to current chunk of indexes?
            if (nextDataset != currentDataset ||   //next iteration will be a different dataset
                index + stride > stop) {           //this is last iteration
                //get currentStart:stride:index
                int currentDatasetStartsAt = currentDataset == 0? 0 : childStopsAt[currentDataset - 1] + 1;
                childConstraints.set(0, currentStart - currentDatasetStartsAt);
                childConstraints.set(2, index - currentDatasetStartsAt);
                if (reallyVerbose) String2.log("  currentDataset=" + currentDataset +
                    "  datasetStartsAt=" + currentDatasetStartsAt + 
                    "  localStart=" + childConstraints.get(0) +
                    "  localStop=" + childConstraints.get(2));
                PrimitiveArray[] tResults = childDatasets[currentDataset].getSourceData(null, null,
                    tDataVariables, childConstraints);
                //childDataset has already checked that axis values are as *it* expects          
                if (cumResults == null) {
                    if (matchAxisNDigits <= 0) {
                        //make axis values exactly as expected by aggregate dataset
                        for (int av = 1; av < nAv; av++)
                            tResults[av] = axisVariables[av].sourceValues().subset(
                                childConstraints.get(av * 3 + 0),
                                childConstraints.get(av * 3 + 1),
                                childConstraints.get(av * 3 + 2));
                    }
                    cumResults = tResults;
                } else {
                    cumResults[0].append(tResults[0]);
                    for (int dv = 0; dv < nDv; dv++)
                        cumResults[nAv + dv].append(tResults[nAv + dv]);
                }

                currentDataset = nextDataset;
                currentStart = index + stride;            
            }

            //increment index
            index += stride;
        }

        return cumResults;
    }

    /** 
     * This returns a fileTable 
     * with valid files (or null if unavailable or any trouble).
     * This is a copy of any internal data, so client can modify the contents.
     *
     * @param nextPath is the partial path (with trailing slash) to be appended 
     *   onto the local fileDir (or wherever files are, even url).
     * @return null if trouble,
     *   or Object[3] where 
     *   [0] is a sorted table with file "Name" (String), "Last modified" (long millis), 
     *     "Size" (long), and "Description" (String, but usually no content),
     *   [1] is a sorted String[] with the short names of directories that are 1 level lower, and
     *   [2] is the local directory corresponding to this (or null, if not a local dir).
     */
    public Object[] accessibleViaFilesFileTable(String nextPath) {
        if (!accessibleViaFiles)
            return null;
        try {
            int nChild = childDatasets.length;

            //if nextPath is nothing, return list of child id's as directories
            if (nextPath.length() == 0) {
                Table table = new Table();
                table.addColumn("Name",          new StringArray());
                table.addColumn("Last modified", new LongArray().setMaxIsMV(true));
                table.addColumn("Size",          new LongArray().setMaxIsMV(true));            
                table.addColumn("Description",   new StringArray());
                StringArray subDirs = new StringArray();
                for (int child = 0; child < nChild; child++) {
                    if (childDatasets[child].accessibleViaFiles)
                        subDirs.add(childDatasets[child].datasetID());
                }
                subDirs.sortIgnoreCase();
                return new Object[]{table, subDirs.toStringArray(), null};
            }

            //ensure start of nextPath is a child datasetID
            int po = nextPath.indexOf('/');
            if (po < 0) {
                String2.log("ERROR: no slash in nextPath.");
                return null;
            }

            //start of nextPath is a child datasetID
            String tID = nextPath.substring(0, po);
            nextPath = nextPath.substring(po + 1);
            for (int child = 0; child < nChild; child++) {
                EDD edd = childDatasets[child];
                if (tID.equals(edd.datasetID())) 
                    return edd.accessibleViaFilesFileTable(nextPath);
            }
            //or it isn't
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
     * @param relativeFileName (for most EDDTypes, just offset by fileDir)
     * @return full localFileName or null if any error (including, file isn't in
     *    list of valid files for this dataset)
     */
    public String accessibleViaFilesGetLocal(String relativeFileName) {
        if (!accessibleViaFiles)
            return null;
        String msg = datasetID() + " accessibleViaFilesGetLocal(" + relativeFileName + "): ";

        try {

            //first dir -> childDatasetName
            int po = relativeFileName.indexOf('/');
            if (po <= 0) {
                String2.log(msg + "no '/' in relatveFileName, so no child datasetID.");
                return null;
            }
            String childID           = relativeFileName.substring(0, po);
            String relativeFileName2 = relativeFileName.substring(po + 1);
 
            //which child?
            int nChild = childDatasets.length;
            for (int c = 0; c < nChild; c++) {
                if (childID.equals(childDatasets[c].datasetID())) {
                    //then redirect request to that child
                    return childDatasets[c].accessibleViaFilesGetLocal(relativeFileName2);
                }
            }
            String2.log(msg + "childID=" + childID + " not found.");
            return null;

        } catch (Exception e) {
            String2.log(msg + ":\n" +
                MustBe.throwableToString(e));
            return null;
        }
         
    }


    /**
     * This generates datasets.xml for an aggregated dataset from a Hyrax or 
     * THREDDS catalog.
     *
     * @param serverType Currently, only "hyrax", "thredds", and "waf" are supported  (case insensitive)
     * @param startUrl the url of the current web page (with a hyrax catalog) e.g.,
     *   <br>hyrax: "http://dods.jpl.nasa.gov/opendap/ocean_wind/ccmp/L3.5a/data/flk/1988/contents.html" 
     *   <br>thredds: "https://thredds1.pfeg.noaa.gov/thredds/catalog/Satellite/aggregsatMH/chla/catalog.xml" 
     * @param fileNameRegex e.g.,
     *   <br>hyrax: "pentad.*flk\\.nc\\.gz"
     *   <br>thredds: ".*"
     * @return a suggested chunk of xml for this dataset for use in datasets.xml 
     * @throws Throwable if trouble, e.g., if no Grid or Array variables are found.
     *    If no trouble, then a valid dataset.xml chunk has been returned.
     */
    public static String generateDatasetsXml(String serverType, String startUrl, 
        String fileNameRegex, int tReloadEveryNMinutes) throws Throwable {

        startUrl = EDStatic.updateUrls(startUrl); //http: to https:
        String2.log("\n*** EDDGridAggregateExistingDimension.generateDatasetsXml" +
            "\nserverType=" + serverType + " startUrl=" + startUrl + 
            "\nfileNameRegex=" + fileNameRegex + 
            " reloadEveryNMinutes=" + tReloadEveryNMinutes);
        long time = System.currentTimeMillis();

        StringBuilder sb = new StringBuilder();
        String sa[];
        serverType = serverType.toLowerCase();
        boolean recursive = true;
        String pathRegex = ".*";
        if ("hyrax".equals(serverType)) {
            Test.ensureTrue(startUrl.endsWith("/contents.html"),
                "startUrl must end with '/contents.html'.");
            sa = FileVisitorDNLS.getUrlsFromHyraxCatalog(
                File2.getDirectory(startUrl), fileNameRegex, recursive, pathRegex);
        } else if ("thredds".equals(serverType)) {
            Test.ensureTrue(startUrl.endsWith("/catalog.xml"),
                "startUrl must end with '/catalog.xml'.");
            sa = EDDGridFromDap.getUrlsFromThreddsCatalog(
                startUrl, fileNameRegex, pathRegex, null).toStringArray();
        } else if ("waf".equals(serverType)) {
            sa = FileVisitorDNLS.getUrlsFromWAF(
                startUrl, fileNameRegex, recursive, pathRegex);
        } else {
            throw new RuntimeException("ERROR: serverType must be \"hyrax\", \"thredds\", or \"waf\".");
        }

        if (sa.length == 0)
            throw new RuntimeException("ERROR: No matching URLs were found.");

        if (sa.length == 1) 
            return EDDGridFromDap.generateDatasetsXml(
                sa[0], null, null, null,
                tReloadEveryNMinutes, null);

        //multiple URLs, so aggregate them
        //outer EDDGridAggregateExistingDimension
        sb.append(
            "<dataset type=\"EDDGridAggregateExistingDimension\" datasetID=\"" + 
                suggestDatasetID(startUrl + "?" + fileNameRegex) + "\" active=\"true\">\n" +
            "\n");

        //first child -- fully defined
        sb.append(EDDGridFromDap.generateDatasetsXml(
            sa[0], null, null, null,
            tReloadEveryNMinutes, null));

        //other children
        //for (int i = 1; i < sa.length; i++) 
        //    sb.append("<sourceUrl>" + sa[i] + "</sourceUrl>\n");

        sb.append("<sourceUrls serverType=\"" + serverType + 
            "\" regex=\"" + XML.encodeAsXML(fileNameRegex) + 
            "\" recursive=\"" + recursive + 
            "\" pathRegex=\"" + XML.encodeAsXML(pathRegex) + "\">" + 
            startUrl + "</sourceUrls>\n");     
        
        //end
        sb.append("\n" +
            "</dataset>\n" +
            "\n");

        String2.log("\n\n*** generateDatasetsXml finished successfully; time=" +
            (System.currentTimeMillis() - time) + "ms\n\n");
        return sb.toString();
    }

    /**
     */
    public static void testGenerateDatasetsXml() throws Throwable {
        String2.log("\n*** EDDGridAggregateExistingDimension.testGenerateDatasetsXml()\n");
        String results, expected;

        //******* test thredds -- lame test: the datasets can't actually be aggregated
        results = generateDatasetsXml("thredds",
            "https://thredds1.pfeg.noaa.gov/thredds/catalog/Satellite/aggregsatMH/chla/catalog.xml", 
            ".*", 1440); //recursive
        expected = 
"<dataset type=\"EDDGridAggregateExistingDimension\" datasetID=\"noaa_pfeg_56a4_10ee_2221\" active=\"true\">\n" +
"\n" +
"<dataset type=\"EDDGridFromDap\" datasetID=\"noaa_pfeg_077f_0be4_21d3\" active=\"true\">\n" +
"    <sourceUrl>https://thredds1.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/1day</sourceUrl>\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"acknowledgement\">NOAA NESDIS COASTWATCH, NOAA SWFSC ERD</att>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"cols\" type=\"int\">8640</att>\n" +
"        <att name=\"composite\">true</att>\n" +
"        <att name=\"contributor_name\">NASA GSFC (OBPG)</att>\n" +
"        <att name=\"contributor_role\">Source of level 2 data.</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.0, Unidata Dataset Discovery v1.0, CWHDF</att>\n" +
"        <att name=\"creator_email\">dave.foley@noaa.gov</att>\n" +
"        <att name=\"creator_name\">NOAA CoastWatch, West Coast Node</att>\n" +
"        <att name=\"creator_url\">http://coastwatch.pfel.noaa.gov</att>\n" +
"        <att name=\"cwhdf_version\">3.4</att>\n" +
"        <att name=\"date_created\">2013-04-18Z</att>\n" + //changes
"        <att name=\"date_issued\">2013-04-18Z</att>\n" +  //changes
"        <att name=\"Easternmost_Easting\" type=\"double\">360.0</att>\n" +
"        <att name=\"et_affine\" type=\"doubleList\">0.0 0.041676313961565174 0.04167148975575877 0.0 0.0 -90.0</att>\n" +
"        <att name=\"gctp_datum\" type=\"int\">12</att>\n" +
"        <att name=\"gctp_parm\" type=\"doubleList\">0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0</att>\n" +
"        <att name=\"gctp_sys\" type=\"int\">0</att>\n" +
"        <att name=\"gctp_zone\" type=\"int\">0</att>\n" +
"        <att name=\"geospatial_lat_max\" type=\"double\">90.0</att>\n" +
"        <att name=\"geospatial_lat_min\" type=\"double\">-90.0</att>\n" +
"        <att name=\"geospatial_lat_resolution\" type=\"double\">0.041676313961565174</att>\n" +
"        <att name=\"geospatial_lat_units\">degrees_north</att>\n" +
"        <att name=\"geospatial_lon_max\" type=\"double\">360.0</att>\n" +
"        <att name=\"geospatial_lon_min\" type=\"double\">0.0</att>\n" +
"        <att name=\"geospatial_lon_resolution\" type=\"double\">0.04167148975575877</att>\n" +
"        <att name=\"geospatial_lon_units\">degrees_east</att>\n" +
"        <att name=\"geospatial_vertical_max\" type=\"double\">0.0</att>\n" +
"        <att name=\"geospatial_vertical_min\" type=\"double\">0.0</att>\n" +
"        <att name=\"geospatial_vertical_positive\">up</att>\n" +
"        <att name=\"geospatial_vertical_units\">m</att>\n" +
"        <att name=\"history\">NASA GSFC (OBPG)\n" +
"2013-04-18T13:37:12Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD</att>\n" + //changes
"        <att name=\"id\">LMHchlaS1day_20130415120000</att>\n" +  //changes
"        <att name=\"institution\">NOAA CoastWatch, West Coast Node</att>\n" +
"        <att name=\"keywords\">EARTH SCIENCE &gt; Oceans &gt; Ocean Chemistry &gt; Chlorophyll</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.</att>\n" +
"        <att name=\"naming_authority\">gov.noaa.pfel.coastwatch</att>\n" +
"        <att name=\"Northernmost_Northing\" type=\"double\">90.0</att>\n" +
"        <att name=\"origin\">NASA GSFC (OBPG)</att>\n" +
"        <att name=\"pass_date\" type=\"int\">15810</att>\n" +  //changes
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
"        <att name=\"start_time\" type=\"double\">0.0</att>\n" +
"        <att name=\"summary\">NOAA CoastWatch distributes chlorophyll-a concentration data from NASA&#39;s Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.   This is Science Quality data.</att>\n" +
"        <att name=\"time_coverage_end\">2013-04-16T00:00:00Z</att>\n" +   //changes
"        <att name=\"time_coverage_start\">2013-04-15T00:00:00Z</att>\n" + //changes
"        <att name=\"title\">Chlorophyll-a, Aqua MODIS, NPP, 0.05 degrees, Global, Science Quality</att>\n" +
"        <att name=\"Westernmost_Easting\" type=\"double\">0.0</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cols\">null</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"creator_email\">erd.data@noaa.gov</att>\n" +
"        <att name=\"creator_type\">institution</att>\n" +
"        <att name=\"creator_url\">https://coastwatch.pfeg.noaa.gov</att>\n" +
"        <att name=\"cwhdf_version\">null</att>\n" +
"        <att name=\"date_created\">2013-04-18</att>\n" + //changes
"        <att name=\"date_issued\">2013-04-18</att>\n" +  //changes
"        <att name=\"et_affine\">null</att>\n" +
"        <att name=\"gctp_datum\">null</att>\n" +
"        <att name=\"gctp_parm\">null</att>\n" +
"        <att name=\"gctp_sys\">null</att>\n" +
"        <att name=\"gctp_zone\">null</att>\n" +
"        <att name=\"infoUrl\">https://thredds1.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/1day.html</att>\n" +
"        <att name=\"institution\">NOAA CoastWatch WCN</att>\n" +
"        <att name=\"keywords\">1day, altitude, aqua, chemistry, chla, chlorophyll, chlorophyll-a, coast, coastwatch, color, concentration, concentration_of_chlorophyll_in_sea_water, daily, data, day, degrees, earth, Earth Science &gt; Oceans &gt; Ocean Chemistry &gt; Chlorophyll, global, imaging, latitude, longitude, MHchla, moderate, modis, national, noaa, node, npp, ocean, ocean color, oceans, orbiting, partnership, polar, polar-orbiting, quality, resolution, science, science quality, sea, seawater, spectroradiometer, time, water, wcn, west</att>\n" +
"        <att name=\"naming_authority\">gov.noaa.pfeg.coastwatch</att>\n" +
"        <att name=\"pass_date\">null</att>\n" +
"        <att name=\"polygon_latitude\">null</att>\n" +
"        <att name=\"polygon_longitude\">null</att>\n" +
"        <att name=\"project\">CoastWatch (https://coastwatch.noaa.gov/)</att>\n" +
"        <att name=\"references\">Aqua/MODIS information: https://oceancolor.gsfc.nasa.gov/ . MODIS information: https://coastwatch.noaa.gov/modis_ocolor_overview.html .</att>\n" +
"        <att name=\"rows\">null</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" +
"        <att name=\"start_time\">null</att>\n" +
"        <att name=\"title\">Chlorophyll-a, Aqua MODIS, NPP, 0.05 degrees, Global, Science Quality (1day), 2003-2013</att>\n" +
"    </addAttributes>\n" +
"    <axisVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Time</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">1.3660272E9 1.3660272E9</att>\n" + //both change
"            <att name=\"axis\">T</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">0</att>\n" +
"            <att name=\"long_name\">Centered Time</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>altitude</sourceName>\n" +
"        <destinationName>altitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Height</att>\n" +
"            <att name=\"_CoordinateZisPositive\">up</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">0.0 0.0</att>\n" +
"            <att name=\"axis\">Z</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">0</att>\n" +
"            <att name=\"long_name\">Altitude</att>\n" +
"            <att name=\"positive\">up</att>\n" +
"            <att name=\"standard_name\">altitude</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>lat</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Lat</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">-90.0 90.0</att>\n" +
"            <att name=\"axis\">Y</att>\n" +
"            <att name=\"coordsys\">geographic</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">4</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"            <att name=\"point_spacing\">even</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>lon</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Lon</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">0.0 360.0</att>\n" +
"            <att name=\"axis\">X</att>\n" +
"            <att name=\"coordsys\">geographic</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">4</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"            <att name=\"point_spacing\">even</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>MHchla</sourceName>\n" +
"        <destinationName>MHchla</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.001" + 
            //trailing 0, an odd difference between Java 1.6 and Java 1.7
            (System.getProperty("java.version").startsWith("1.6")? "0" : "") + 
            " 63.914</att>\n" +  //changes was 63.845, was 63.624, was 63.848
"            <att name=\"coordsys\">geographic</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">2</att>\n" +
"            <att name=\"long_name\">Chlorophyll-a, Aqua MODIS, NPP, 0.05 degrees, Global, Science Quality</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"numberOfObservations\" type=\"int\">1952349</att>\n" + //changes
"            <att name=\"percentCoverage\" type=\"double\">0.05230701838991769</att>\n" + //changes
"            <att name=\"standard_name\">concentration_of_chlorophyll_in_sea_water</att>\n" +
"            <att name=\"units\">mg m-3</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">30.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.03</att>\n" +
"            <att name=\"colorBarScale\">Log</att>\n" +
"            <att name=\"ioos_category\">Ocean Color</att>\n" +
"            <att name=\"numberOfObservations\">null</att>\n" +
"            <att name=\"percentCoverage\">null</att>\n" + 
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n" +
"<sourceUrls serverType=\"thredds\" regex=\".*\" recursive=\"true\" pathRegex=\".*\">https://thredds1.pfeg.noaa.gov/thredds/catalog/Satellite/aggregsatMH/chla/catalog.xml</sourceUrls>\n" +
"\n" +
"</dataset>\n" +
"\n";
        Test.ensureEqual(results, expected, "results=\n" + results);



        //****** test HYRAX
        results = generateDatasetsXml("hyrax",
            "https://opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/contents.html", 
            "month_[0-9]{8}_v11l35flk\\.nc\\.gz", //note: v one one L
            1440); 
        expected = 
"<dataset type=\"EDDGridAggregateExistingDimension\" datasetID=\"nasa_jpl_2458_4e43_c52a\" active=\"true\">\n" +
"\n" +
"<dataset type=\"EDDGridFromDap\" datasetID=\"nasa_jpl_ef57_7a3b_e14d\" active=\"true\">\n" +
"    <sourceUrl>https://opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880101_v11l35flk.nc.gz</sourceUrl>\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"base_date\" type=\"shortList\">1988 1 1</att>\n" +
"        <att name=\"Conventions\">COARDS</att>\n" +
"        <att name=\"description\">Time average of level3.0 products for the period: 1988-01-01 to 1988-01-31</att>\n" +
"        <att name=\"history\">Created by NASA Goddard Space Flight Center under the NASA REASoN CAN: A Cross-Calibrated, Multi-Platform Ocean Surface Wind Velocity Product for Meteorological and Oceanographic Applications</att>\n" +
"        <att name=\"title\">Atlas FLK v1.1 derived surface winds (level 3.5)</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"creator_email\">support-podaac@earthdata.nasa.gov</att>\n" +
"        <att name=\"creator_name\">NASA GSFC MEaSUREs, NOAA</att>\n" +
"        <att name=\"creator_type\">group</att>\n" +
"        <att name=\"creator_url\">https://podaac.jpl.nasa.gov/dataset/CCMP_MEASURES_ATLAS_L4_OW_L3_0_WIND_VECTORS_FLK</att>\n" +
"        <att name=\"infoUrl\">https://opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/contents.html</att>\n" +
"        <att name=\"institution\">NASA GSFC, NOAA</att>\n" +
"        <att name=\"keywords\">atlas, atmosphere, atmospheric, center, component, data, derived, downward, earth, Earth Science &gt; Atmosphere &gt; Atmospheric Winds &gt; Surface Winds, Earth Science &gt; Atmosphere &gt; Atmospheric Winds &gt; Wind Stress, eastward, eastward_wind, flight, flk, goddard, gsfc, latitude, level, longitude, meters, month, nasa, noaa, nobs, northward, northward_wind, number, observations, oceanography, physical, physical oceanography, pseudostress, science, space, speed, statistics, stress, surface, surface_downward_eastward_stress, surface_downward_northward_stress, time, u-component, u-wind, upstr, uwnd, v-component, v-wind, v1.1, v11l35flk, vpstr, vwnd, wind, wind_speed, winds, wspd</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" +
"        <att name=\"summary\">Time average of level3.0 products for the period: 1988-01-01 to 1988-01-31</att>\n" +
"        <att name=\"title\">Atlas FLK v1.1 derived surface winds (level 3.5) (month 19880101 v11l35flk), 0.25&#xb0;</att>\n" +
"    </addAttributes>\n" +
"    <axisVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">8760.0 8760.0</att>\n" +
"            <att name=\"avg_period\">0000-01-00 00:00:00</att>\n" +
"            <att name=\"delta_t\">0000-01-00 00:00:00</att>\n" +
"            <att name=\"long_name\">Time</att>\n" +
"            <att name=\"units\">hours since 1987-01-01 00:00:0.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"units\">hours since 1987-01-01T00:00:00.000Z</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>lat</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-78.375 78.375</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>lon</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.125 359.875</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>uwnd</sourceName>\n" +
"        <destinationName>uwnd</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-20.97617 21.307398</att>\n" +
"            <att name=\"add_offset\" type=\"float\">0.0</att>\n" +
"            <att name=\"long_name\">u-wind at 10 meters</att>\n" +
"            <att name=\"missing_value\" type=\"short\">-32767</att>\n" +
"            <att name=\"scale_factor\" type=\"float\">0.001525972</att>\n" +
"            <att name=\"units\">m/s</att>\n" +
"            <att name=\"valid_range\" type=\"floatList\">-50.0 50.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-15.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"standard_name\">eastward_wind</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>vwnd</sourceName>\n" +
"        <destinationName>vwnd</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-18.43927 19.008484</att>\n" +
"            <att name=\"add_offset\" type=\"float\">0.0</att>\n" +
"            <att name=\"long_name\">v-wind at 10 meters</att>\n" +
"            <att name=\"missing_value\" type=\"short\">-32767</att>\n" +
"            <att name=\"scale_factor\" type=\"float\">0.001525972</att>\n" +
"            <att name=\"units\">m/s</att>\n" +
"            <att name=\"valid_range\" type=\"floatList\">-50.0 50.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-15.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"standard_name\">northward_wind</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>wspd</sourceName>\n" +
"        <destinationName>wspd</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.10927376 22.478634</att>\n" +
"            <att name=\"add_offset\" type=\"float\">37.5</att>\n" +
"            <att name=\"long_name\">wind speed at 10 meters</att>\n" +
"            <att name=\"missing_value\" type=\"short\">-32767</att>\n" +
"            <att name=\"scale_factor\" type=\"float\">0.001144479</att>\n" +
"            <att name=\"units\">m/s</att>\n" +
"            <att name=\"valid_range\" type=\"floatList\">0.0 75.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"standard_name\">wind_speed</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>upstr</sourceName>\n" +
"        <destinationName>upstr</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-446.93918 478.96118</att>\n" +
"            <att name=\"add_offset\" type=\"float\">0.0</att>\n" +
"            <att name=\"long_name\">u-component of pseudostress at 10 meters</att>\n" +
"            <att name=\"missing_value\" type=\"short\">-32767</att>\n" +
"            <att name=\"scale_factor\" type=\"float\">0.03051944</att>\n" +
"            <att name=\"units\">m2/s2</att>\n" +
"            <att name=\"valid_range\" type=\"floatList\">-1000.0 1000.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">0.5</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-0.5</att>\n" +
"            <att name=\"ioos_category\">Physical Oceanography</att>\n" +
"            <att name=\"standard_name\">surface_downward_eastward_stress</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>vpstr</sourceName>\n" +
"        <destinationName>vpstr</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"actual_range\" type=\"floatList\">-356.3739 407.52307</att>\n" +
"            <att name=\"add_offset\" type=\"float\">0.0</att>\n" +
"            <att name=\"long_name\">v-component of pseudostress at 10 meters</att>\n" +
"            <att name=\"missing_value\" type=\"short\">-32767</att>\n" +
"            <att name=\"scale_factor\" type=\"float\">0.03051944</att>\n" +
"            <att name=\"units\">m2/s2</att>\n" +
"            <att name=\"valid_range\" type=\"floatList\">-1000.0 1000.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">0.5</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-0.5</att>\n" +
"            <att name=\"ioos_category\">Physical Oceanography</att>\n" +
"            <att name=\"standard_name\">surface_downward_northward_stress</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>nobs</sourceName>\n" +
"        <destinationName>nobs</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"actual_range\" type=\"floatList\">0.0 124.0</att>\n" +
"            <att name=\"add_offset\" type=\"float\">32766.0</att>\n" +
"            <att name=\"long_name\">number of observations</att>\n" +
"            <att name=\"missing_value\" type=\"short\">-32767</att>\n" +
"            <att name=\"scale_factor\" type=\"float\">1.0</att>\n" +
"            <att name=\"units\">count</att>\n" +
"            <att name=\"valid_range\" type=\"floatList\">0.0 65532.0</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Statistics</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n" +
"<sourceUrls serverType=\"hyrax\" regex=\"month_[0-9]{8}_v11l35flk\\.nc\\.gz\" recursive=\"true\" pathRegex=\".*\">https://opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/contents.html</sourceUrls>\n" +
"\n" +
"</dataset>\n" +
"\n";
        Test.ensureEqual(results, expected, "results=\n" + results);


        String2.log("\n*** EDDGridAggregateExistingDimension.testGenerateDatasetsXml() finished successfully.\n");

    }

    /** This tests getDodsIndexUrls.
     */
    public static void testGetDodsIndexUrls() throws Exception {
        String2.log("\nEDDGridAggregateExistingDimension.testGetDodsIndexUrls");

        try {
        StringArray sourceUrls = new StringArray();
        String dir = "https://opendap.jpl.nasa.gov/opendap/GeodeticsGravity/tellus/L3/ocean_mass/RL05/netcdf/contents.html";
            //"http://www.marine.csiro.au/dods/nph-dods/dods-data/bl/BRAN2.1/bodas/"; //now requires authorization
            //"http://dods.marine.usf.edu/dods-bin/nph-dods/modis/";  //but it has no files
        getDodsIndexUrls(dir, "[0-9]{8}\\.bodas_ts\\.nc", true, "", sourceUrls);

        Test.ensureEqual(sourceUrls.get(0),   dir + "19921014.bodas_ts.nc", "");
        Test.ensureEqual(sourceUrls.size(), 741, "");
        Test.ensureEqual(sourceUrls.get(740), dir + "20061227.bodas_ts.nc", "");
        String2.log("EDDGridAggregateExistingDimension.testGetDodsIndexUrls finished successfully.");
        } catch (Throwable t) {
            Test.knownProblem("CSIRO bodas now requires authorization.", 
                "No known examples now.", t); 
        }
    }

    /**
     * This tests the basic methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void testBasic() throws Throwable {

        String2.log("\n****************** EDDGridAggregateExistingDimension.testBasic() *****************\n");
        testVerboseOn();
        String name, tName, userDapQuery, results, expected, error;


        //*** NDBC  is also IMPORTANT UNIQUE TEST of >1 variable in a file
        EDDGrid gridDataset = (EDDGrid)oneFromDatasetsXml(null, "ndbcCWind41002");       

        //min max
        EDV edv = gridDataset.findAxisVariableByDestinationName("longitude");
        Test.ensureEqual(edv.destinationMinDouble(), -75.42, "");
        Test.ensureEqual(edv.destinationMaxDouble(), -75.42, "");

        String ndbcDapQuery = "wind_speed[1:5][0][0]";
        tName = gridDataset.makeNewFileForDapQuery(null, null, ndbcDapQuery, 
            EDStatic.fullTestCacheDirectory, gridDataset.className(), ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"time,latitude,longitude,wind_speed\n"+
"UTC,degrees_north,degrees_east,m s-1\n"+
"1989-06-13T16:10:00Z,32.27,-75.42,15.7\n"+
"1989-06-13T16:20:00Z,32.27,-75.42,15.0\n"+
"1989-06-13T16:30:00Z,32.27,-75.42,14.4\n"+
"1989-06-13T16:40:00Z,32.27,-75.42,14.0\n"+
"1989-06-13T16:50:00Z,32.27,-75.42,13.6\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        ndbcDapQuery = "wind_speed[1:5][0][0]";
        tName = gridDataset.makeNewFileForDapQuery(null, null, ndbcDapQuery, EDStatic.fullTestCacheDirectory, 
            gridDataset.className(), ".nc"); 
        results = NcHelper.ncdump(EDStatic.fullTestCacheDirectory + tName, "");
        int dataPo = results.indexOf("data:");
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
        expected = 
"netcdf EDDGridAggregateExistingDimension.nc {\n" +
"  dimensions:\n" +
"    time = 5;\n" +   // (has coord.var)\n" +   //changed when switched to netcdf-java 4.0, 2009-02-23
"    latitude = 1;\n" +   // (has coord.var)\n" +
"    longitude = 1;\n" +   // (has coord.var)\n" +
"  variables:\n" +
"    double time(time=5);\n" +
"      :_CoordinateAxisType = \"Time\";\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

//BUG???!!!
//"     :actual_range = 6.137568E8, 7.258458E8; // double\n" +  //this changes depending on how many files aggregated

expected = 
":axis = \"T\";\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Epoch Time\";\n" +
"      :short_name = \"time\";\n" +
"      :standard_name = \"time\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    float latitude(latitude=1);\n" +
"      :_CoordinateAxisType = \"Lat\";\n" +
"      :actual_range = 32.27f, 32.27f; // float\n" +
"      :axis = \"Y\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Latitude\";\n" +
"      :short_name = \"latitude\";\n" +
"      :standard_name = \"latitude\";\n" +
"      :units = \"degrees_north\";\n" +
"\n" +
"    float longitude(longitude=1);\n" +
"      :_CoordinateAxisType = \"Lon\";\n" +
"      :actual_range = -75.42f, -75.42f; // float\n" +
"      :axis = \"X\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Longitude\";\n" +
"      :short_name = \"longitude\";\n" +
"      :standard_name = \"longitude\";\n" +
"      :units = \"degrees_east\";\n" +
"\n" +
"    float wind_speed(time=5, latitude=1, longitude=1);\n" +
"      :_FillValue = 99.0f; // float\n" +
"      :ioos_category = \"Wind\";\n" +
"      :long_name = \"Wind Speed\";\n" +
"      :missing_value = 99.0f; // float\n" +
"      :short_name = \"wspd\";\n" +
"      :standard_name = \"wind_speed\";\n" +
"      :units = \"m s-1\";\n" +
"\n" +
"  // global attributes:\n" +
"  :cdm_data_type = \"Grid\";\n" +
"  :comment = \"S HATTERAS - 250 NM East of Charleston, SC\";\n" +
"  :contributor_name = \"NOAA NDBC\";\n" +
"  :contributor_role = \"Source of data.\";\n" +
"  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"  :Easternmost_Easting = -75.42f; // float\n" +
"  :geospatial_lat_max = 32.27f; // float\n" +
"  :geospatial_lat_min = 32.27f; // float\n" +
"  :geospatial_lat_units = \"degrees_north\";\n" +
"  :geospatial_lon_max = -75.42f; // float\n" +
"  :geospatial_lon_min = -75.42f; // float\n" +
"  :geospatial_lon_units = \"degrees_east\";\n" +
"  :history = \"";
//today + " https://dods.ndbc.noaa.gov/thredds/dodsC/data/cwind/41002/41002c1989.nc\n" +
//today + " " + EDStatic.erddapUrl + //in tests, always non-https url
        int po = results.indexOf(":axis =");
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, "results=\n" + results);

expected = 
"/griddap/ndbcCWind41002.nc?wind_speed[1:5][0][0]\";\n" +
"  :infoUrl = \"https://www.ndbc.noaa.gov/cwind.shtml\";\n" +
"  :institution = \"NOAA NDBC\";\n" +
"  :keywords = \"Earth Science > Atmosphere > Atmospheric Winds > Surface Winds\";\n" +
"  :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
"  :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"  :location = \"32.27 N 75.42 W\";\n" +
"  :Northernmost_Northing = 32.27f; // float\n" +
"  :quality = \"Automated QC checks with manual editing and comprehensive monthly QC\";\n" +
"  :sourceUrl = \"https://dods.ndbc.noaa.gov/thredds/dodsC/data/cwind/41002/41002c1989.nc\";\n" +
"  :Southernmost_Northing = 32.27f; // float\n" +
"  :station = \"41002\";\n" +
"  :summary = \"These continuous wind measurements from the NOAA National Data Buoy Center (NDBC) stations are 10-minute average values of wind speed (in m/s) and direction (in degrees clockwise from North).\";\n" +
"  :time_coverage_end = \"1989-06-13T16:50:00Z\";\n" +
"  :time_coverage_start = \"1989-06-13T16:10:00Z\";\n" +
"  :title = \"Wind Data from NDBC 41002\";\n" +
"  :url = \"https://dods.ndbc.noaa.gov\";\n" +
"  :Westernmost_Easting = -75.42f; // float\n" +
"\n" +
"  data:\n" +
"    time = \n" +
"      {6.137574E8, 6.13758E8, 6.137586E8, 6.137592E8, 6.137598E8}\n" +
"    latitude = \n" +
"      {32.27}\n" +
"    longitude = \n" +
"      {-75.42}\n" +
"    wind_speed = \n" +
"      {\n" +
"        {\n" +
"          {15.7}\n" +
"        },\n" +
"        {\n" +
"          {15.0}\n" +
"        },\n" +
"        {\n" +
"          {14.4}\n" +
"        },\n" +
"        {\n" +
"          {14.0}\n" +
"        },\n" +
"        {\n" +
"          {13.6}\n" +
"        }\n" +
"      }\n" +
"}\n";
        po = results.indexOf(expected.substring(0, 50));
        Test.ensureEqual(results.substring(po), expected, "results=\n" + results);

        ndbcDapQuery = "wind_direction[1:5][0][0]";
        tName = gridDataset.makeNewFileForDapQuery(null, null, ndbcDapQuery, EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "2", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"time,latitude,longitude,wind_direction\n"+
"UTC,degrees_north,degrees_east,degrees_true\n"+
"1989-06-13T16:10:00Z,32.27,-75.42,234\n"+
"1989-06-13T16:20:00Z,32.27,-75.42,233\n"+
"1989-06-13T16:30:00Z,32.27,-75.42,233\n"+
"1989-06-13T16:40:00Z,32.27,-75.42,235\n"+
"1989-06-13T16:50:00Z,32.27,-75.42,235\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        ndbcDapQuery = "wind_speed[1:5][0][0],wind_direction[1:5][0][0]";
        tName = gridDataset.makeNewFileForDapQuery(null, null, ndbcDapQuery, EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "3", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"time,latitude,longitude,wind_speed,wind_direction\n"+
"UTC,degrees_north,degrees_east,m s-1,degrees_true\n"+
"1989-06-13T16:10:00Z,32.27,-75.42,15.7,234\n"+
"1989-06-13T16:20:00Z,32.27,-75.42,15.0,233\n"+
"1989-06-13T16:30:00Z,32.27,-75.42,14.4,233\n"+
"1989-06-13T16:40:00Z,32.27,-75.42,14.0,235\n"+
"1989-06-13T16:50:00Z,32.27,-75.42,13.6,235\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        ndbcDapQuery = "wind_direction[1:5][0][0],wind_speed[1:5][0][0]";
        tName = gridDataset.makeNewFileForDapQuery(null, null, ndbcDapQuery, EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "4", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"time,latitude,longitude,wind_direction,wind_speed\n"+
"UTC,degrees_north,degrees_east,degrees_true,m s-1\n"+
"1989-06-13T16:10:00Z,32.27,-75.42,234,15.7\n"+
"1989-06-13T16:20:00Z,32.27,-75.42,233,15.0\n"+
"1989-06-13T16:30:00Z,32.27,-75.42,233,14.4\n"+
"1989-06-13T16:40:00Z,32.27,-75.42,235,14.0\n"+
"1989-06-13T16:50:00Z,32.27,-75.42,235,13.6\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        tName = gridDataset.makeNewFileForDapQuery(null, null, ndbcDapQuery, EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "4", ".nc"); 
        results = NcHelper.ncdump(EDStatic.fullTestCacheDirectory + tName, "");
        dataPo = results.indexOf("data:");
        results = results.substring(dataPo);
        expected = 
"data:\n" +
"    time = \n" +
"      {6.137574E8, 6.13758E8, 6.137586E8, 6.137592E8, 6.137598E8}\n" +
"    latitude = \n" +
"      {32.27}\n" +
"    longitude = \n" +
"      {-75.42}\n" +
"    wind_direction = \n" +
"      {\n" +
"        {\n" +
"          {234}\n" +
"        },\n" +
"        {\n" +
"          {233}\n" +
"        },\n" +
"        {\n" +
"          {233}\n" +
"        },\n" +
"        {\n" +
"          {235}\n" +
"        },\n" +
"        {\n" +
"          {235}\n" +
"        }\n" +
"      }\n" +
"    wind_speed = \n" +
"      {\n" +
"        {\n" +
"          {15.7}\n" +
"        },\n" +
"        {\n" +
"          {15.0}\n" +
"        },\n" +
"        {\n" +
"          {14.4}\n" +
"        },\n" +
"        {\n" +
"          {14.0}\n" +
"        },\n" +
"        {\n" +
"          {13.6}\n" +
"        }\n" +
"      }\n" +
"}\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test seam of two datasets
        ndbcDapQuery = "wind_direction[22232:22239][0][0],wind_speed[22232:22239][0][0]";
        tName = gridDataset.makeNewFileForDapQuery(null, null, ndbcDapQuery, EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "5", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"time,latitude,longitude,wind_direction,wind_speed\n"+
"UTC,degrees_north,degrees_east,degrees_true,m s-1\n"+
"1989-12-05T05:20:00Z,32.27,-75.42,232,23.7\n"+
"1989-12-05T05:30:00Z,32.27,-75.42,230,24.1\n"+
"1989-12-05T05:40:00Z,32.27,-75.42,225,23.5\n"+
"1989-12-05T05:50:00Z,32.27,-75.42,233,23.3\n"+
"1992-04-28T23:00:00Z,32.27,-75.42,335,29.4\n"+
"1992-04-28T23:10:00Z,32.27,-75.42,335,30.5\n"+
"1992-04-28T23:20:00Z,32.27,-75.42,330,32.3\n"+
"1992-04-28T23:30:00Z,32.27,-75.42,331,33.2\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test seam of two datasets   with stride
        ndbcDapQuery = "wind_direction[22232:2:22239][0][0],wind_speed[22232:2:22239][0][0]";
        tName = gridDataset.makeNewFileForDapQuery(null, null, ndbcDapQuery, EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "6", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"time,latitude,longitude,wind_direction,wind_speed\n"+
"UTC,degrees_north,degrees_east,degrees_true,m s-1\n"+
"1989-12-05T05:20:00Z,32.27,-75.42,232,23.7\n"+
"1989-12-05T05:40:00Z,32.27,-75.42,225,23.5\n"+
"1992-04-28T23:00:00Z,32.27,-75.42,335,29.4\n"+
"1992-04-28T23:20:00Z,32.27,-75.42,330,32.3\n";
        Test.ensureEqual(results, expected, "results=\n" + results);
        // */

        //test last data value (it causes a few problems -- including different axis values)
        ndbcDapQuery = "wind_direction[(2007-12-31T22:40:00):1:(2007-12-31T22:55:00)][0][0]," +
                           "wind_speed[(2007-12-31T22:40:00):1:(2007-12-31T22:55:00)][0][0]";
        tName = gridDataset.makeNewFileForDapQuery(null, null, ndbcDapQuery, EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "7", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"time,latitude,longitude,wind_direction,wind_speed\n"+
"UTC,degrees_north,degrees_east,degrees_true,m s-1\n"+
"2007-12-31T22:40:00Z,32.27,-75.42,36,4.0\n"+
"2007-12-31T22:50:00Z,32.27,-75.42,37,4.3\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        String2.log("\n*** EDDGridAggregateExistingDimension.test finished.");

    }

    /** This tests rtofs - where the aggregated dimension, time, has sourceValues != destValues
     *  since sourceTimeFormat isn't "seconds since 1970-01-01". 
     *  !!!THIS USED TO WORK, BUT NEEDS TO BE UPDATED; datasets are removed after ~1 month
     */
    public static void testRtofs() throws Throwable {
        String2.log("\n****************** EDDGridAggregateExistingDimension.testRtofs() *****************\n");
        testVerboseOn();
        String tName, results, expected;
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, "RTOFSWOC1");
        tName = eddGrid.makeNewFileForDapQuery(null, null, 
            "time", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_rtofs", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        expected = 
"time\n" +
"UTC\n" +
"2009-07-01T00:00:00Z\n" +
"2009-07-02T00:00:00Z\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
    }

    /**
     * This tests the /files/ "files" system.
     * This requires nceiOisst2Agg in the localhost ERDDAP.
     */
    public static void testFiles() throws Throwable {

        String2.log("\n*** EDDGridSideBySide.testFiles()\n");
        String tDir = EDStatic.fullTestCacheDirectory;
        String dapQuery, tName, start, query, results, expected;
        int po;

        try {
            //get /files/datasetID/.csv
            results = SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/nceiOisst2Agg/.csv");
            expected = 
"Name,Last modified,Size,Description\n" +
"nceiOisst2Agg_nc3/,NaN,NaN,\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //get /files/datasetID/
            results = SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/nceiOisst2Agg/");
            Test.ensureTrue(results.indexOf("nceiOisst2Agg&#x5f;nc3&#x2f;") > 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("nceiOisst2Agg_nc3/")      > 0, "results=\n" + results);

            //get /files/datasetID/subdir/.csv
            results = SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/nceiOisst2Agg/nceiOisst2Agg_nc3/.csv");
            expected = 
"Name,Last modified,Size,Description\n" +
"avhrr-only-v2.20170330.nc,1493422520000,8305496,\n" +
"avhrr-only-v2.20170331.nc,1493422536000,8305496,\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //download a file in root
 
            //download a file in subdir
            results = String2.annotatedString(SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/nceiOisst2Agg/nceiOisst2Agg_nc3/avhrr-only-v2.20170331.nc").substring(0, 50));
            expected = 
"CDF[1][0][0][0][0][0][0][0][10]\n" +
"[0][0][0][4][0][0][0][4]time[0][0][0][1][0][0][0][4]zlev[0][0][0][1][0][0][0][3]lat[0][0][0][end]"; 
            Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

            //try to download a non-existent dataset
            try {
                results = SSR.getUrlResponseStringNewline(
                    "http://localhost:8080/cwexperimental/files/gibberish/");
            } catch (Exception e) { 
                results = e.toString();
            }
            expected = 
"java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/gibberish/\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: Currently unknown datasetID=gibberish\";\n" +
"})";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //try to download a non-existent directory
            try {
                results = SSR.getUrlResponseStringNewline(
                    "http://localhost:8080/cwexperimental/files/nceiOisst2Agg/gibberish/");
            } catch (Exception e) { 
                results = e.toString();
            }
            expected = 
"java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/nceiOisst2Agg/gibberish/\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: Resource not found: directory=gibberish/\";\n" +
"})";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //try to download a non-existent file
            try {
                results = SSR.getUrlResponseStringNewline(
                    "http://localhost:8080/cwexperimental/files/nceiOisst2Agg/gibberish.csv");
            } catch (Exception e) { 
                results = e.toString();
            }
            expected = 
"java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/nceiOisst2Agg/gibberish.csv\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: File not found: gibberish.csv .\";\n" +
"})";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //try to download a non-existent file in existant subdir
            try {
                results = SSR.getUrlResponseStringNewline(
                    "http://localhost:8080/cwexperimental/files/nceiOisst2Agg/subdir/gibberish.csv");
            } catch (Exception e) { 
                results = e.toString();
            }
            expected = 
"java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/nceiOisst2Agg/subdir/gibberish.csv\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: File not found: gibberish.csv .\";\n" +
"})";
            Test.ensureEqual(results, expected, "results=\n" + results);

 

        } catch (Throwable t) {
            throw new Exception("Unexpected error. This test requires nceiOisst2Agg in the localhost ERDDAP.", t); 
        } 
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
            lastTest = interactive? -1 : 2;
        String msg = "\n^^^ EDDGridAggregateExistingDimensione.test(" + interactive + ") test=";

        for (int test = firstTest; test <= lastTest; test++) {
            try {
                long time = System.currentTimeMillis();
                String2.log(msg + test);
            
                if (interactive) {
                    //if (test ==  0) ...;

                } else {
                    if (test ==  0) testGenerateDatasetsXml();
                    if (test ==  1) testBasic();  //slow!
                    if (test ==  2) testFiles();  //slow!

                    //not usually run
                    //if (test == 1000) testRtofs();  //worked but needs to be updated; datasets are removed after ~1 month
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
