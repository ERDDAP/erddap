/* 
 * EDDGridAggregateExistingDimension Copyright 2008, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

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
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDGridAggregateExistingDimension"&gt; 
     *    having just been read.  
     * @return an EDDGridAggregateExistingDimension.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDGridAggregateExistingDimension fromXml(SimpleXMLReader xmlReader) throws Throwable {

        if (verbose) String2.log("\n*** constructing EDDGridAggregateExistingDimension(xmlReader)...");
        String tDatasetID = xmlReader.attributeValue("datasetID"); 

        //data to be obtained while reading xml
        EDDGrid firstChild = null;
        StringArray tLocalSourceUrls = new StringArray();
        String tAccessibleTo = null;
        StringArray tOnChange = new StringArray();
        String tFgdcFile = null;
        String tIso19115File = null;
        int tMatchAxisNDigits = DEFAULT_MATCH_AXIS_N_DIGITS;
        String tDefaultDataQuery = null;
        String tDefaultGraphQuery = null;

        String tSUServerType = null;
        String tSURegex = null;
        boolean tSURecursive = true;
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
                if (firstChild == null) {
                    EDD edd = EDD.fromXml(xmlReader.attributeValue("type"), xmlReader);
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
            else if (localTags.equals( "<sourceUrl>")) {}
            else if (localTags.equals("</sourceUrl>")) 
                tLocalSourceUrls.add(content);

            //<sourceUrls serverType="thredds" regex=".*\\.nc" recursive="true"
            //  >http://ourocean.jpl.nasa.gov:8080/thredds/dodsC/g1sst/catalog.xml</sourceUrl>
            else if (localTags.equals( "<sourceUrls>")) {
                tSUServerType = xmlReader.attributeValue("serverType");
                tSURegex      = xmlReader.attributeValue("regex");
                String tr     = xmlReader.attributeValue("recursive");
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

        //make the main dataset based on the information gathered
        return new EDDGridAggregateExistingDimension(tDatasetID, tAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File,
            tDefaultDataQuery, tDefaultGraphQuery,
            firstChild, tLocalSourceUrls.toArray(),
            tSUServerType, tSURegex, tSURecursive, tSU, 
            tMatchAxisNDigits);

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
     * @param tOnChange 0 or more actions (starting with "http://" or "mailto:")
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
        String tAccessibleTo, StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tDefaultDataQuery, String tDefaultGraphQuery,
        EDDGrid firstChild, String tLocalSourceUrls[], 
        String tSUServerType, String tSURegex, boolean tSURecursive, String tSU,
        int tMatchAxisNDigits) throws Throwable {

        if (verbose) String2.log(
            "\n*** constructing EDDGridAggregateExistingDimension " + tDatasetID); 
        long constructionStartMillis = System.currentTimeMillis();
        String errorInMethod = "Error in EDDGridGridAggregateExistingDimension(" + 
            tDatasetID + ") constructor:\n";
            
        //save some of the parameters
        className = "EDDGridAggregateExistingDimension"; 
        datasetID = tDatasetID;
        setAccessibleTo(tAccessibleTo);
        onChange = tOnChange;
        fgdcFile = tFgdcFile;
        iso19115File = tIso19115File;
        defaultDataQuery = tDefaultDataQuery;
        defaultGraphQuery = tDefaultGraphQuery;
        matchAxisNDigits = tMatchAxisNDigits;

        //if no tLocalSourceURLs, generate from hyrax, thredds, or dodsindex catalog?
        if (tLocalSourceUrls.length == 0 && tSU != null && tSU.length() > 0) {
            if (tSURegex == null || tSURegex.length() == 0)
                tSURegex = ".*";

            StringArray tsa = new StringArray();
            if (tSUServerType == null)
                throw new RuntimeException(errorInMethod + "<sourceUrls> serverType is null.");
            else if (tSUServerType.toLowerCase().equals("hyrax"))
                tsa.add(FileVisitorDNLS.getUrlsFromHyraxCatalog(tSU, tSURegex, tSURecursive));
            else if (tSUServerType.toLowerCase().equals("thredds"))
                tsa.add(EDDGridFromDap.getUrlsFromThreddsCatalog(tSU, tSURegex, tSURecursive));
            else if (tSUServerType.toLowerCase().equals("dodsindex"))
                getDodsIndexUrls(tSU, tSURegex, tSURecursive, tsa);
            else throw new RuntimeException(errorInMethod + 
                "<sourceUrls> serverType=" + tSUServerType + " must be \"hyrax\", \"thredds\", or \"dodsindex\".");
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
        }


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
        axisVariables[0] = makeAxisVariable(0, sn, dn, sa, aa, cumSV); 

        //make the local dataVariables
        int nDv = firstChild.dataVariables.length;
        dataVariables = new EDV[nDv];
        System.arraycopy(firstChild.dataVariables, 0, dataVariables, 0, nDv);

        //ensure the setup is valid
        ensureValid();

        //finally
        if (verbose) String2.log(
            (reallyVerbose? "\n" + toString() : "") +
            "\n*** EDDGridAggregateExistingDimension " + datasetID + " constructor finished. TIME=" + 
            (System.currentTimeMillis() - constructionStartMillis) + "\n"); 

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
     *   E.g., http://www.marine.csiro.au/dods/nph-dods/dods-data/bl/BRAN2.1/bodas/
     *   It MUST end with a / or a file name (e.g., catalog.html).
     * @param regex The regex is used to test file names, not directory names.
     *   E.g. .*\.nc to catch all .nc files, or use .* to catch all files.
     * @param recursive if true, this method will pursue subdirectories
     * @param sourceUrls The results will be placed here.
     * @throws Exception if trouble.  However, it isn't an error if a URL returns
     *   an error message.
     */
    public static void getDodsIndexUrls(String startUrl, String regex, 
        boolean recursive, StringArray sourceUrls) throws Exception {

        //get the document as one string per line
        String baseDir = File2.getDirectory(startUrl);
        int onSourceUrls = sourceUrls.size();
        String regexHtml = regex + "\\.html"; //link href will have .html at end
        String lines[] = null;
        try {
            lines = SSR.getUrlResponse(startUrl);
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
            return;
        }
        int nextLine = 0;
        int nLines = lines.length;
        if (nLines == 0) {
            String2.log("WARNING: The document has 0 lines.");
            return;
        }

        //look for <pre>
        String line = lines[nextLine++];
        String lcLine = line.toLowerCase();
        while (lcLine.indexOf("<pre>") < 0 && nextLine < nLines) {
            line = lines[nextLine++];
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
            line = lines[nextLine++];
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
                        if (recursive) {                            
                            getDodsIndexUrls(
                                tName.toLowerCase().startsWith("http")? tName : baseDir + tName,
                                regex, recursive, sourceUrls);
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
    public PrimitiveArray[] getSourceData(EDV tDataVariables[], IntArray tConstraints) 
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
                PrimitiveArray[] tResults = childDatasets[currentDataset].getSourceData(
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
     * This generates datasets.xml for an aggregated dataset from a Hyrax or 
     * THREDDS catalog.
     *
     * @param serverType Currently, only "hyrax" and "thredds" are supported  (case insensitive)
     * @param startUrl the url of the current web page (with a hyrax catalog) e.g.,
     *   <br>hyrax: "http://dods.jpl.nasa.gov/opendap/ocean_wind/ccmp/L3.5a/data/flk/1988/contents.html" 
     *   <br>thredds: "http://thredds1.pfeg.noaa.gov/thredds/catalog/Satellite/aggregsatMH/chla/catalog.xml" 
     * @param fileNameRegex e.g.,
     *   <br>hyrax: "pentad.*flk\\.nc\\.gz"
     *   <br>thredds: ".*"
     * @param recursive
     * @return a suggested chunk of xml for this dataset for use in datasets.xml 
     * @throws Throwable if trouble, e.g., if no Grid or Array variables are found.
     *    If no trouble, then a valid dataset.xml chunk has been returned.
     */
    public static String generateDatasetsXml(String serverType, String startUrl, 
        String fileNameRegex, boolean recursive, int tReloadEveryNMinutes) throws Throwable {

        long time = System.currentTimeMillis();

        StringBuilder sb = new StringBuilder();
        String sa[];
        serverType = serverType.toLowerCase();
        if ("hyrax".equals(serverType)) {
            Test.ensureTrue(startUrl.endsWith("/contents.html"),
                "startUrl must end with '/contents.html'.");
            sa = FileVisitorDNLS.getUrlsFromHyraxCatalog(
                File2.getDirectory(startUrl), fileNameRegex, recursive);
        } else if ("thredds".equals(serverType)) {
            Test.ensureTrue(startUrl.endsWith("/catalog.xml"),
                "startUrl must end with '/catalog.xml'.");
            sa = EDDGridFromDap.getUrlsFromThreddsCatalog(
                startUrl, fileNameRegex, recursive);
        } else {
            throw new RuntimeException("ERROR: serverType must be \"hyrax\" or \"thredds\".");
        }

        if (sa.length == 0)
            throw new RuntimeException("ERROR: No matching URLs were found.");

        if (sa.length == 1) 
            return EDDGridFromDap.generateDatasetsXml(true, //writeDirections, 
                sa[0], null, null, null,
                tReloadEveryNMinutes, null);

        //multiple URLs, so aggregate them
        //outer EDDGridAggregateExistingDimension
        sb.append(
            directionsForGenerateDatasetsXml() +
            "-->\n\n" +
            "<dataset type=\"EDDGridAggregateExistingDimension\" datasetID=\"" + 
                suggestDatasetID(startUrl + "?" + fileNameRegex) + "\" active=\"true\">\n" +
            "\n");

        //first child -- fully defined
        sb.append(EDDGridFromDap.generateDatasetsXml(false, //writeDirections, 
            sa[0], null, null, null,
            tReloadEveryNMinutes, null));

        //other children
        //for (int i = 1; i < sa.length; i++) 
        //    sb.append("<sourceUrl>" + sa[i] + "</sourceUrl>\n");

        sb.append("<sourceUrls serverType=\"" + serverType + 
            "\" regex=\"" + fileNameRegex + "\" recursive=\"" + recursive + "\">" + 
            startUrl + "</sourceUrls>\n");     
        
        //end
        sb.append("\n" +
            "</dataset>\n" +
            "\n");

        String2.log("\n\n*** generateDatasetsXml finished successfully; time=" +
            (System.currentTimeMillis() - time) + "\n\n");
        return sb.toString();
    }

    /**
     */
    public static void testGenerateDatasetsXml() throws Throwable {
        String2.log("\n*** EDDGridAggregateExistingDimension.testGenerateDatasetsXml()\n");
        String results, expected;

        //******* test thredds -- lame test: the datasets can't actually be aggregated
        results = generateDatasetsXml("thredds",
            "http://thredds1.pfeg.noaa.gov/thredds/catalog/Satellite/aggregsatMH/chla/catalog.xml", 
            ".*", 
            true, 1440); //recursive
        expected = 
"<!--\n" +
" DISCLAIMER:\n" +
"   The chunk of datasets.xml made by GenerageDatasetsXml isn't perfect.\n" +
"   YOU MUST READ AND EDIT THE XML BEFORE USING IT IN A PUBLIC ERDDAP.\n" +
"   GenerateDatasetsXml relies on a lot of rules-of-thumb which aren't always\n" +
"   correct.  *YOU* ARE RESPONSIBLE FOR ENSURING THE CORRECTNESS OF THE XML\n" +
"   THAT YOU ADD TO ERDDAP'S datasets.xml FILE.\n" +
"\n" +
" DIRECTIONS:\n" +
" * Read about this type of dataset in\n" +
"   http://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html .\n" +
" * Read http://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html#addAttributes\n" +
"   so that you understand about sourceAttributes and addAttributes.\n" +
" * Note: Global sourceAttributes and variable sourceAttributes are listed\n" +
"   below as comments, for informational purposes only.\n" +
"   ERDDAP combines sourceAttributes and addAttributes (which have\n" +
"   precedence) to make the combinedAttributes that are shown to the user.\n" +
"   (And other attributes are automatically added to longitude, latitude,\n" +
"   altitude, depth, and time variables).\n" +
" * If you don't like a sourceAttribute, override it by adding an\n" +
"   addAttribute with the same name but a different value\n" +
"   (or no value, if you want to remove it).\n" +
" * All of the addAttributes are computer-generated suggestions. Edit them!\n" +
"   If you don't like an addAttribute, change it.\n" +
" * If you want to add other addAttributes, add them.\n" +
" * If you want to change a destinationName, change it.\n" +
"   But don't change sourceNames.\n" +
" * You can change the order of the dataVariables or remove any of them.\n" +
"-->\n" +
"\n" +
"<dataset type=\"EDDGridAggregateExistingDimension\" datasetID=\"noaa_pfeg_8d37_4596_befd\" active=\"true\">\n" +
"\n" +
"<dataset type=\"EDDGridFromDap\" datasetID=\"noaa_pfeg_adfb_8ff5_678c\" active=\"true\">\n" +
"    <sourceUrl>http://thredds1.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/1day</sourceUrl>\n" +
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
"        <att name=\"cwhdf_version\">null</att>\n" +
"        <att name=\"et_affine\">null</att>\n" +
"        <att name=\"gctp_datum\">null</att>\n" +
"        <att name=\"gctp_parm\">null</att>\n" +
"        <att name=\"gctp_sys\">null</att>\n" +
"        <att name=\"gctp_zone\">null</att>\n" +
"        <att name=\"infoUrl\">http://thredds1.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/1day.html</att>\n" +
"        <att name=\"institution\">NOAA CoastWatch WCN</att>\n" +
"        <att name=\"keywords\">1day, altitude, aqua, chemistry, chla, chlorophyll, chlorophyll-a, coast, coastwatch, color, concentration, concentration_of_chlorophyll_in_sea_water, daily, data, day, degrees, global, imaging, MHchla, moderate, modis, national, noaa, node, npp, ocean, ocean color, oceans,\n" +
"Oceans &gt; Ocean Chemistry &gt; Chlorophyll,\n" +
"orbiting, partnership, polar, polar-orbiting, quality, resolution, science, science quality, sea, seawater, spectroradiometer, time, water, wcn, west</att>\n" +
"        <att name=\"pass_date\">null</att>\n" +
"        <att name=\"polygon_latitude\">null</att>\n" +
"        <att name=\"polygon_longitude\">null</att>\n" +
"        <att name=\"rows\">null</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v29</att>\n" +
"        <att name=\"start_time\">null</att>\n" +
"        <att name=\"title\">Chlorophyll-a, Aqua MODIS, NPP, 0.05 degrees, Global, Science Quality (1day)</att>\n" +
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
"<sourceUrls serverType=\"thredds\" regex=\".*\" recursive=\"true\">http://thredds1.pfeg.noaa.gov/thredds/catalog/Satellite/aggregsatMH/chla/catalog.xml</sourceUrls>\n" +
"\n" +
"</dataset>\n" +
"\n";
        Test.ensureEqual(results, expected, "results=\n" + results);



        //****** test HYRAX
        results = generateDatasetsXml("hyrax",
            "http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/contents.html", 
            "month_[0-9]{8}_v11l35flk\\.nc\\.gz", //note: v one one L
            true, 1440); //recursive
        expected = 
"<!--\n" +
" DISCLAIMER:\n" +
"   The chunk of datasets.xml made by GenerageDatasetsXml isn't perfect.\n" +
"   YOU MUST READ AND EDIT THE XML BEFORE USING IT IN A PUBLIC ERDDAP.\n" +
"   GenerateDatasetsXml relies on a lot of rules-of-thumb which aren't always\n" +
"   correct.  *YOU* ARE RESPONSIBLE FOR ENSURING THE CORRECTNESS OF THE XML\n" +
"   THAT YOU ADD TO ERDDAP'S datasets.xml FILE.\n" +
"\n" +
" DIRECTIONS:\n" +
" * Read about this type of dataset in\n" +
"   http://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html .\n" +
" * Read http://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html#addAttributes\n" +
"   so that you understand about sourceAttributes and addAttributes.\n" +
" * Note: Global sourceAttributes and variable sourceAttributes are listed\n" +
"   below as comments, for informational purposes only.\n" +
"   ERDDAP combines sourceAttributes and addAttributes (which have\n" +
"   precedence) to make the combinedAttributes that are shown to the user.\n" +
"   (And other attributes are automatically added to longitude, latitude,\n" +
"   altitude, depth, and time variables).\n" +
" * If you don't like a sourceAttribute, override it by adding an\n" +
"   addAttribute with the same name but a different value\n" +
"   (or no value, if you want to remove it).\n" +
" * All of the addAttributes are computer-generated suggestions. Edit them!\n" +
"   If you don't like an addAttribute, change it.\n" +
" * If you want to add other addAttributes, add them.\n" +
" * If you want to change a destinationName, change it.\n" +
"   But don't change sourceNames.\n" +
" * You can change the order of the dataVariables or remove any of them.\n" +
"-->\n" +
"\n" +
"<dataset type=\"EDDGridAggregateExistingDimension\" datasetID=\"nasa_jpl_790b_dd75_9ec2\" active=\"true\">\n" +
"\n" +
"<dataset type=\"EDDGridFromDap\" datasetID=\"nasa_jpl_03ee_a693_74ed\" active=\"true\">\n" +
"    <sourceUrl>http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/month_19880101_v11l35flk.nc.gz</sourceUrl>\n" +
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
"        <att name=\"creator_email\">podaac@podaac.jpl.nasa.gov</att>\n" +
"        <att name=\"creator_name\">NASA GSFC MEaSUREs, NOAA</att>\n" +
"        <att name=\"creator_url\">http://podaac.jpl.nasa.gov/dataset/CCMP_MEASURES_ATLAS_L4_OW_L3_0_WIND_VECTORS_FLK</att>\n" +
"        <att name=\"infoUrl\">http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/contents.html</att>\n" +
"        <att name=\"institution\">NASA GSFC, NOAA</att>\n" +
"        <att name=\"keywords\">atlas, atmosphere,\n" +
"Atmosphere &gt; Atmospheric Winds &gt; Surface Winds,\n" +
"Atmosphere &gt; Atmospheric Winds &gt; Wind Stress,\n" +
"atmospheric, center, component, data, derived, downward, eastward, eastward_wind, flight, flk, goddard, gsfc, level, meters, month, nasa, noaa, nobs, northward, northward_wind, number, observations, oceanography, physical, physical oceanography, pseudostress, space, speed, statistics, stress, surface, surface_downward_eastward_stress, surface_downward_northward_stress, time, u-component, u-wind, upstr, uwnd, v-component, v-wind, v1.1, v11l35flk, vpstr, vwnd, wind, wind_speed, winds, wspd</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v29</att>\n" +
"        <att name=\"summary\">Time average of level3.0 products for the period: 1988-01-01 to 1988-01-31</att>\n" +
"        <att name=\"title\">Atlas FLK v1.1 derived surface winds (level 3.5) (month 19880101 v11l35flk)</att>\n" +
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
"<sourceUrls serverType=\"hyrax\" regex=\"month_[0-9]{8}_v11l35flk\\.nc\\.gz\" recursive=\"true\">http://podaac-opendap.jpl.nasa.gov/opendap/allData/ccmp/L3.5a/monthly/flk/1988/contents.html</sourceUrls>\n" +
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
        StringArray sourceUrls = new StringArray();
        String dir = "http://www.marine.csiro.au/dods/nph-dods/dods-data/bl/BRAN2.1/bodas/";
        getDodsIndexUrls(dir, "[0-9]{8}\\.bodas_ts\\.nc", true, sourceUrls);

        Test.ensureEqual(sourceUrls.get(0),   dir + "19921014.bodas_ts.nc", "");
        Test.ensureEqual(sourceUrls.size(), 741, "");
        Test.ensureEqual(sourceUrls.get(740), dir + "20061227.bodas_ts.nc", "");
        String2.log("EDDGridAggregateExistingDimension.testGetDodsIndexUrls finished successfully.");
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

        //one time

        //*** NDBC  is also IMPORTANT UNIQUE TEST of >1 variable in a file
        EDDGrid gridDataset = (EDDGrid)oneFromDatasetXml("ndbcCWind41002");       

        //min max
        EDV edv = gridDataset.findAxisVariableByDestinationName("longitude");
        Test.ensureEqual(edv.destinationMin(), -75.42, "");
        Test.ensureEqual(edv.destinationMax(), -75.42, "");

        String ndbcDapQuery = "wind_speed[1:5][0][0]";
        tName = gridDataset.makeNewFileForDapQuery(null, null, ndbcDapQuery, 
            EDStatic.fullTestCacheDirectory, gridDataset.className(), ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"time, latitude, longitude, wind_speed\n" +
"UTC, degrees_north, degrees_east, m s-1\n" +
"1989-06-13T16:10:00Z, 32.27, -75.42, 15.7\n" +
"1989-06-13T16:20:00Z, 32.27, -75.42, 15.0\n" +
"1989-06-13T16:30:00Z, 32.27, -75.42, 14.4\n" +
"1989-06-13T16:40:00Z, 32.27, -75.42, 14.0\n" +
"1989-06-13T16:50:00Z, 32.27, -75.42, 13.6\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        ndbcDapQuery = "wind_speed[1:5][0][0]";
        tName = gridDataset.makeNewFileForDapQuery(null, null, ndbcDapQuery, EDStatic.fullTestCacheDirectory, 
            gridDataset.className(), ".nc"); 
        results = NcHelper.dumpString(EDStatic.fullTestCacheDirectory + tName, true);
        int dataPo = results.indexOf("data:");
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);
        expected = 
"netcdf EDDGridAggregateExistingDimension.nc {\n" +
" dimensions:\n" +
"   time = 5;\n" +   // (has coord.var)\n" +   //changed when switched to netcdf-java 4.0, 2009-02-23
"   latitude = 1;\n" +   // (has coord.var)\n" +
"   longitude = 1;\n" +   // (has coord.var)\n" +
" variables:\n" +
"   double time(time=5);\n" +
"     :_CoordinateAxisType = \"Time\";\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

//BUG???!!!
//"     :actual_range = 6.137568E8, 7.258458E8; // double\n" +  //this changes depending on how many files aggregated

expected = 
":axis = \"T\";\n" +
"     :ioos_category = \"Time\";\n" +
"     :long_name = \"Epoch Time\";\n" +
"     :short_name = \"time\";\n" +
"     :standard_name = \"time\";\n" +
"     :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"     :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"   float latitude(latitude=1);\n" +
"     :_CoordinateAxisType = \"Lat\";\n" +
"     :actual_range = 32.27f, 32.27f; // float\n" +
"     :axis = \"Y\";\n" +
"     :ioos_category = \"Location\";\n" +
"     :long_name = \"Latitude\";\n" +
"     :short_name = \"latitude\";\n" +
"     :standard_name = \"latitude\";\n" +
"     :units = \"degrees_north\";\n" +
"   float longitude(longitude=1);\n" +
"     :_CoordinateAxisType = \"Lon\";\n" +
"     :actual_range = -75.42f, -75.42f; // float\n" +
"     :axis = \"X\";\n" +
"     :ioos_category = \"Location\";\n" +
"     :long_name = \"Longitude\";\n" +
"     :short_name = \"longitude\";\n" +
"     :standard_name = \"longitude\";\n" +
"     :units = \"degrees_east\";\n" +
"   float wind_speed(time=5, latitude=1, longitude=1);\n" +
"     :_FillValue = 99.0f; // float\n" +
"     :ioos_category = \"Wind\";\n" +
"     :long_name = \"Wind Speed\";\n" +
"     :missing_value = 99.0f; // float\n" +
"     :short_name = \"wspd\";\n" +
"     :standard_name = \"wind_speed\";\n" +
"     :units = \"m s-1\";\n" +
"\n" +
" :cdm_data_type = \"Grid\";\n" +
" :comment = \"S HATTERAS - 250 NM East of Charleston, SC\";\n" +
" :contributor_name = \"NOAA NDBC\";\n" +
" :contributor_role = \"Source of data.\";\n" +
" :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
" :Easternmost_Easting = -75.42f; // float\n" +
" :geospatial_lat_max = 32.27f; // float\n" +
" :geospatial_lat_min = 32.27f; // float\n" +
" :geospatial_lon_max = -75.42f; // float\n" +
" :geospatial_lon_min = -75.42f; // float\n" +
" :history = \"" + today + " http://dods.ndbc.noaa.gov/thredds/dodsC/data/cwind/41002/41002c1989.nc\n" +
today + " " + EDStatic.erddapUrl + //in tests, always non-https url
"/griddap/ndbcCWind41002.nc?wind_speed[1:5][0][0]\";\n" +
" :infoUrl = \"http://www.ndbc.noaa.gov/cwind.shtml\";\n" +
" :institution = \"NOAA NDBC\";\n" +
" :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
" :location = \"32.27 N 75.42 W \";\n" +
" :Northernmost_Northing = 32.27f; // float\n" +
" :quality = \"Automated QC checks with manual editing and comprehensive monthly QC\";\n" +
" :sourceUrl = \"http://dods.ndbc.noaa.gov/thredds/dodsC/data/cwind/41002/41002c1989.nc\";\n" +
" :Southernmost_Northing = 32.27f; // float\n" +
" :station = \"41002\";\n" +
" :summary = \"These continuous wind measurements from the NOAA National Data Buoy Center (NDBC) stations are 10-minute average values of wind speed (in m/s) and direction (in degrees clockwise from North).\";\n" +
" :time_coverage_end = \"1989-06-13T16:50:00Z\";\n" +
" :time_coverage_start = \"1989-06-13T16:10:00Z\";\n" +
" :title = \"Wind Data from NDBC 41002\";\n" +
" :url = \"http://dods.ndbc.noaa.gov\";\n" +
" :Westernmost_Easting = -75.42f; // float\n" +
" data:\n" +
"time =\n" +
"  {6.137574E8, 6.13758E8, 6.137586E8, 6.137592E8, 6.137598E8}\n" +
"latitude =\n" +
"  {32.27}\n" +
"longitude =\n" +
"  {-75.42}\n" +
"wind_speed =\n" +
"  {\n" +
"    {\n" +
"      {15.7}\n" +
"    },\n" +
"    {\n" +
"      {15.0}\n" +
"    },\n" +
"    {\n" +
"      {14.4}\n" +
"    },\n" +
"    {\n" +
"      {14.0}\n" +
"    },\n" +
"    {\n" +
"      {13.6}\n" +
"    }\n" +
"  }\n" +
"}\n";
        int po = results.indexOf(":axis =");
        Test.ensureEqual(results.substring(po), expected, "results=\n" + results);

        ndbcDapQuery = "wind_direction[1:5][0][0]";
        tName = gridDataset.makeNewFileForDapQuery(null, null, ndbcDapQuery, EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "2", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"time, latitude, longitude, wind_direction\n" +
"UTC, degrees_north, degrees_east, degrees_true\n" +
"1989-06-13T16:10:00Z, 32.27, -75.42, 234\n" +
"1989-06-13T16:20:00Z, 32.27, -75.42, 233\n" +
"1989-06-13T16:30:00Z, 32.27, -75.42, 233\n" +
"1989-06-13T16:40:00Z, 32.27, -75.42, 235\n" +
"1989-06-13T16:50:00Z, 32.27, -75.42, 235\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        ndbcDapQuery = "wind_speed[1:5][0][0],wind_direction[1:5][0][0]";
        tName = gridDataset.makeNewFileForDapQuery(null, null, ndbcDapQuery, EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "3", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"time, latitude, longitude, wind_speed, wind_direction\n" +
"UTC, degrees_north, degrees_east, m s-1, degrees_true\n" +
"1989-06-13T16:10:00Z, 32.27, -75.42, 15.7, 234\n" +
"1989-06-13T16:20:00Z, 32.27, -75.42, 15.0, 233\n" +
"1989-06-13T16:30:00Z, 32.27, -75.42, 14.4, 233\n" +
"1989-06-13T16:40:00Z, 32.27, -75.42, 14.0, 235\n" +
"1989-06-13T16:50:00Z, 32.27, -75.42, 13.6, 235\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        ndbcDapQuery = "wind_direction[1:5][0][0],wind_speed[1:5][0][0]";
        tName = gridDataset.makeNewFileForDapQuery(null, null, ndbcDapQuery, EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "4", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"time, latitude, longitude, wind_direction, wind_speed\n" +
"UTC, degrees_north, degrees_east, degrees_true, m s-1\n" +
"1989-06-13T16:10:00Z, 32.27, -75.42, 234, 15.7\n" +
"1989-06-13T16:20:00Z, 32.27, -75.42, 233, 15.0\n" +
"1989-06-13T16:30:00Z, 32.27, -75.42, 233, 14.4\n" +
"1989-06-13T16:40:00Z, 32.27, -75.42, 235, 14.0\n" +
"1989-06-13T16:50:00Z, 32.27, -75.42, 235, 13.6\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        tName = gridDataset.makeNewFileForDapQuery(null, null, ndbcDapQuery, EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "4", ".nc"); 
        results = NcHelper.dumpString(EDStatic.fullTestCacheDirectory + tName, true);
        dataPo = results.indexOf("data:");
        results = results.substring(dataPo);
        expected = 
"data:\n" +
"time =\n" +
"  {6.137574E8, 6.13758E8, 6.137586E8, 6.137592E8, 6.137598E8}\n" +
"latitude =\n" +
"  {32.27}\n" +
"longitude =\n" +
"  {-75.42}\n" +
"wind_direction =\n" +
"  {\n" +
"    {\n" +
"      {234}\n" +
"    },\n" +
"    {\n" +
"      {233}\n" +
"    },\n" +
"    {\n" +
"      {233}\n" +
"    },\n" +
"    {\n" +
"      {235}\n" +
"    },\n" +
"    {\n" +
"      {235}\n" +
"    }\n" +
"  }\n" +
"wind_speed =\n" +
"  {\n" +
"    {\n" +
"      {15.7}\n" +
"    },\n" +
"    {\n" +
"      {15.0}\n" +
"    },\n" +
"    {\n" +
"      {14.4}\n" +
"    },\n" +
"    {\n" +
"      {14.0}\n" +
"    },\n" +
"    {\n" +
"      {13.6}\n" +
"    }\n" +
"  }\n" +
"}\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test seam of two datasets
        ndbcDapQuery = "wind_direction[22232:22239][0][0],wind_speed[22232:22239][0][0]";
        tName = gridDataset.makeNewFileForDapQuery(null, null, ndbcDapQuery, EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "5", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"time, latitude, longitude, wind_direction, wind_speed\n" +
"UTC, degrees_north, degrees_east, degrees_true, m s-1\n" +
"1989-12-05T05:20:00Z, 32.27, -75.42, 232, 23.7\n" +
"1989-12-05T05:30:00Z, 32.27, -75.42, 230, 24.1\n" +
"1989-12-05T05:40:00Z, 32.27, -75.42, 225, 23.5\n" +
"1989-12-05T05:50:00Z, 32.27, -75.42, 233, 23.3\n" +
"1992-04-28T23:00:00Z, 32.27, -75.42, 335, 29.4\n" +
"1992-04-28T23:10:00Z, 32.27, -75.42, 335, 30.5\n" +
"1992-04-28T23:20:00Z, 32.27, -75.42, 330, 32.3\n" +
"1992-04-28T23:30:00Z, 32.27, -75.42, 331, 33.2\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test seam of two datasets   with stride
        ndbcDapQuery = "wind_direction[22232:2:22239][0][0],wind_speed[22232:2:22239][0][0]";
        tName = gridDataset.makeNewFileForDapQuery(null, null, ndbcDapQuery, EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "6", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"time, latitude, longitude, wind_direction, wind_speed\n" +
"UTC, degrees_north, degrees_east, degrees_true, m s-1\n" +
"1989-12-05T05:20:00Z, 32.27, -75.42, 232, 23.7\n" +
"1989-12-05T05:40:00Z, 32.27, -75.42, 225, 23.5\n" +
"1992-04-28T23:00:00Z, 32.27, -75.42, 335, 29.4\n" +
"1992-04-28T23:20:00Z, 32.27, -75.42, 330, 32.3\n";
        Test.ensureEqual(results, expected, "results=\n" + results);
        // */

        //test last data value (it causes a few problems -- including different axis values)
        ndbcDapQuery = "wind_direction[(2007-12-31T22:40:00):1:(2007-12-31T22:55:00)][0][0]," +
                           "wind_speed[(2007-12-31T22:40:00):1:(2007-12-31T22:55:00)][0][0]";
        tName = gridDataset.makeNewFileForDapQuery(null, null, ndbcDapQuery, EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "7", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"time, latitude, longitude, wind_direction, wind_speed\n" +
"UTC, degrees_north, degrees_east, degrees_true, m s-1\n" +
"2007-12-31T22:40:00Z, 32.27, -75.42, 36, 4.0\n" +
"2007-12-31T22:50:00Z, 32.27, -75.42, 37, 4.3\n";
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
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetXml("RTOFSWOC1");
        tName = eddGrid.makeNewFileForDapQuery(null, null, 
            "time", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_rtofs", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"time\n" +
"UTC\n" +
"2009-07-01T00:00:00Z\n" +
"2009-07-02T00:00:00Z\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
        
    }


    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {

        String2.log("\n****************** EDDGridAggregateExistingDimension.test() *****************\n");
        testGenerateDatasetsXml();
        testBasic();

        //not usually run
        //testRtofs();  //worked but needs to be updated; datasets are removed after ~1 month
    }



}
