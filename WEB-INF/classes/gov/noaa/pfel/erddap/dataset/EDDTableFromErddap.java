/* 
 * EDDTableFromErddap Copyright 2008, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

/** The Java DAP classes.  */
import dods.dap.*;

import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.util.Subscriptions;
import gov.noaa.pfel.erddap.variable.*;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * NcHelper and ucar classes only used for testing netcdf-java.
 * Get netcdf-X.X.XX.jar from http://www.unidata.ucar.edu/software/netcdf-java/index.htm
 * and copy it to <context>/WEB-INF/lib renamed as netcdf-latest.jar.
 * Get slf4j-jdk14.jar from 
 * ftp://ftp.unidata.ucar.edu/pub/netcdf-java/slf4j-jdk14.jar
 * and copy it to <context>/WEB-INF/lib.
 * Put both of these .jar files in the classpath for the compiler and for Java.
 */
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dods.*;
import ucar.nc2.util.*;
import ucar.ma2.*;  

/** 
 * This class represents a table of data from an opendap sequence source.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2007-06-08
 */
public class EDDTableFromErddap extends EDDTable implements FromErddap { 

    protected double sourceErddapVersion = 1.22; //default = last version before /version service was added

    /** Indicates if data can be transmitted in a compressed form.
     * It is unlikely anyone would want to change this. */
    public static boolean acceptDeflate = true;

    protected String publicSourceErddapUrl;

    /**
     * This constructs an EDDTableFromErddap based on the information in an .xml file.
     * 
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableFromErddap"&gt; 
     *    having just been read.  
     * @return an EDDTableFromErddap.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDTableFromErddap fromXml(SimpleXMLReader xmlReader) throws Throwable {

        //data to be obtained (or not)
        if (verbose) String2.log("\n*** constructing EDDTableFromErddap(xmlReader)...");
        String tDatasetID = xmlReader.attributeValue("datasetID"); 
        int tReloadEveryNMinutes = Integer.MAX_VALUE;
        String tAccessibleTo = null;
        StringArray tOnChange = new StringArray();
        String tFgdcFile = null;
        String tIso19115File = null;
        String tSosOfferingPrefix = null;
        String tLocalSourceUrl = null;
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
            if      (localTags.equals( "<reloadEveryNMinutes>")) {}
            else if (localTags.equals("</reloadEveryNMinutes>")) tReloadEveryNMinutes = String2.parseInt(content); 

            //Since this erddap can never be logged in to the remote ERDDAP, 
            //it can never get dataset info from the remote erddap dataset (which should have restricted access).
            //Plus there is no way to pass accessibleTo info between ERDDAP's (but not to users).
            //So there is currently no way to make this work. 
            //So it is disabled.    See below, too.
            //else if (localTags.equals( "<accessibleTo>")) {}
            //else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;

            else if (localTags.equals( "<sourceUrl>")) {}
            else if (localTags.equals("</sourceUrl>")) tLocalSourceUrl = content; 
            else if (localTags.equals( "<onChange>")) {}
            else if (localTags.equals("</onChange>")) tOnChange.add(content); 
            else if (localTags.equals( "<fgdcFile>")) {}
            else if (localTags.equals("</fgdcFile>"))     tFgdcFile = content; 
            else if (localTags.equals( "<iso19115File>")) {}
            else if (localTags.equals("</iso19115File>")) tIso19115File = content; 
            else if (localTags.equals( "<sosOfferingPrefix>")) {}
            else if (localTags.equals("</sosOfferingPrefix>")) tSosOfferingPrefix = content; 
            else if (localTags.equals( "<defaultDataQuery>")) {}
            else if (localTags.equals("</defaultDataQuery>")) tDefaultDataQuery = content; 
            else if (localTags.equals( "<defaultGraphQuery>")) {}
            else if (localTags.equals("</defaultGraphQuery>")) tDefaultGraphQuery = content; 

            else xmlReader.unexpectedTagException();
        }

        return new EDDTableFromErddap(tDatasetID, tAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
            tDefaultDataQuery, tDefaultGraphQuery, tReloadEveryNMinutes, tLocalSourceUrl);
    }

    /**
     * The constructor.
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
     * @param tReloadEveryNMinutes indicates how often the source should
     *    be checked for new data.
     * @param tLocalSourceUrl the url to which .das or .dds or ... can be added
     * @throws Throwable if trouble
     */
    public EDDTableFromErddap(String tDatasetID, String tAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tSosOfferingPrefix,
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        int tReloadEveryNMinutes, 
        String tLocalSourceUrl) throws Throwable {

        if (verbose) String2.log(
            "\n*** constructing EDDTableFromErddap " + tDatasetID); 
        long constructionStartMillis = System.currentTimeMillis();
        String errorInMethod = "Error in EDDTableFromErddap(" + 
            tDatasetID + ") constructor:\n";
            
        //save some of the parameters
        className = "EDDTableFromErddap"; 
        datasetID = tDatasetID;
        //setAccessibleTo(tAccessibleTo);  disabled. see above.
        onChange = tOnChange;
        fgdcFile = tFgdcFile;
        iso19115File = tIso19115File;
        sosOfferingPrefix = tSosOfferingPrefix;
        defaultDataQuery = tDefaultDataQuery;
        defaultGraphQuery = tDefaultGraphQuery;
        addGlobalAttributes = new Attributes();
        setReloadEveryNMinutes(tReloadEveryNMinutes);
        localSourceUrl = tLocalSourceUrl;
        if (tLocalSourceUrl.indexOf("/griddap/") > 0)
            throw new RuntimeException(
                "For datasetID=" + tDatasetID + 
                ", use type=\"EDDGridFromErddap\", not EDDTableFromErddap, in datasets.xml.");
        publicSourceErddapUrl = convertToPublicSourceUrl(tLocalSourceUrl);

        //erddap support all constraints:
        sourceNeedsExpandedFP_EQ = false;
        sourceCanConstrainNumericData = CONSTRAIN_YES;
        sourceCanConstrainStringData  = CONSTRAIN_YES;
        sourceCanConstrainStringRegex = PrimitiveArray.REGEX_OP;

        //Design decision: this doesn't use e.g., ucar.nc2.dt.StationDataSet 
        //  because it determines axes via _CoordinateAxisType (or similar) metadata
        //  which most datasets we use don't have yet.
        //  One could certainly write another class that did use ucar.nc2.dt.StationDataSet.

        //quickRestart
        Attributes quickRestartAttributes = null;       
        if (EDStatic.quickRestart && 
            EDStatic.initialLoadDatasets() && 
            File2.isFile(quickRestartFullFileName())) {
            //try to do quick initialLoadDatasets()
            //If this fails anytime during construction, the dataset will be loaded 
            //  during the next major loadDatasets,
            //  which is good because it allows quick loading of other datasets to continue.
            //This will fail (good) if dataset has changed significantly and
            //  quickRestart file has outdated information.
            quickRestartAttributes = NcHelper.readAttributesFromNc(quickRestartFullFileName());

            if (verbose)
                String2.log("  using info from quickRestartFile");

            //set creationTimeMillis to time of previous creation, so next time
            //to be reloaded will be same as if ERDDAP hadn't been restarted.
            creationTimeMillis = quickRestartAttributes.getLong("creationTimeMillis");

            //Ensure quickRestart information is recent.
            //If too old: abandon construction, delete quickRestart file, flag dataset reloadASAP
            ensureQuickRestartInfoIsRecent(datasetID, getReloadEveryNMinutes(), 
                creationTimeMillis, quickRestartFullFileName());
        }
     
        //DAS
        byte dasBytes[] = quickRestartAttributes == null?
            SSR.getUrlResponseBytes(localSourceUrl + ".das") : //has timeout and descriptive error 
            ((ByteArray)quickRestartAttributes.get("dasBytes")).toArray();
        DAS das = new DAS();
        das.parse(new ByteArrayInputStream(dasBytes));

        //DDS
        byte ddsBytes[] = quickRestartAttributes == null?
            SSR.getUrlResponseBytes(localSourceUrl + ".dds") : //has timeout and descriptive error 
            ((ByteArray)quickRestartAttributes.get("ddsBytes")).toArray();
        DDS dds = new DDS();
        dds.parse(new ByteArrayInputStream(ddsBytes));

        //get global attributes
        sourceGlobalAttributes = new Attributes();
        OpendapHelper.getAttributes(das, "GLOBAL", sourceGlobalAttributes);
        combinedGlobalAttributes = new Attributes(addGlobalAttributes, sourceGlobalAttributes); //order is important
        combinedGlobalAttributes.removeValue("null");

        //delve into the outerSequence 
        BaseType outerVariable = (BaseType)dds.getVariable(SEQUENCE_NAME);
        if (!(outerVariable instanceof DSequence)) 
            throw new IllegalArgumentException(errorInMethod + "outerVariable not a DSequence: name=" + 
                outerVariable.getName() + " type=" + outerVariable.getTypeName());
        DSequence outerSequence = (DSequence)outerVariable;
        int nOuterColumns = outerSequence.elementCount();
        AttributeTable outerAttributeTable = das.getAttributeTable(SEQUENCE_NAME);
        ArrayList tDataVariables = new ArrayList();
        for (int outerCol = 0; outerCol < nOuterColumns; outerCol++) {

            //look at the variables in the outer sequence
            BaseType obt = (BaseType)outerSequence.getVar(outerCol);
            String tSourceName = obt.getName();

            //get the data sourceType
            String tSourceType = PrimitiveArray.elementClassToString( 
                OpendapHelper.getElementClass(obt.newPrimitiveVector()));

            //get the attributes
            Attributes tSourceAtt = new Attributes();
            //note use of getName in this section
            //if (reallyVerbose) String2.log("try getting attributes for outer " + tSourceName);
            dods.dap.Attribute attribute = outerAttributeTable.getAttribute(tSourceName);
            //it should be a container with the attributes for this column
            if (attribute == null) {
                String2.log("WARNING!!! Unexpected: no attribute for outerVar=" + 
                    tSourceName + ".");
            } else if (attribute.isContainer()) { 
                OpendapHelper.getAttributes(attribute.getContainer(), tSourceAtt);
            } else {
                String2.log("WARNING!!! Unexpected: attribute for outerVar=" + 
                    tSourceName + " not a container: " + 
                    attribute.getName() + "=" + attribute.getValueAt(0));
            }

            //deal with remote not having ioos_category, but this ERDDAP requiring it
            Attributes tAddAtt = new Attributes();
            if (EDStatic.variablesMustHaveIoosCategory &&
                tSourceAtt.getString("ioos_category") == null) {

                //guess ioos_category   (alternative is always assign "Unknown")
                Attributes tAtts = EDD.makeReadyToUseAddVariableAttributesForDatasetsXml(
                    tSourceAtt, tSourceName, false, false); //tryToAddColorBarMinMax, tryToFindLLAT
                tAddAtt.add("ioos_category", tAtts.getString("ioos_category"));
            }

            //make the variable
            if (EDV.LON_NAME.equals(tSourceName)) {
                lonIndex = tDataVariables.size();
                tDataVariables.add(new EDVLon(tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType, Double.NaN, Double.NaN)); 
            } else if (EDV.LAT_NAME.equals(tSourceName)) {
                latIndex = tDataVariables.size();
                tDataVariables.add(new EDVLat(tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType, Double.NaN, Double.NaN)); 
            } else if (EDV.ALT_NAME.equals(tSourceName)) {
                altIndex = tDataVariables.size();
                tDataVariables.add(new EDVAlt(tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType, Double.NaN, Double.NaN));
            } else if (EDV.DEPTH_NAME.equals(tSourceName)) {
                depthIndex = tDataVariables.size();
                tDataVariables.add(new EDVDepth(tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType, Double.NaN, Double.NaN)); 
            } else if (EDV.TIME_NAME.equals(tSourceName)) {  //look for TIME_NAME before check hasTimeUnits (next)
                timeIndex = tDataVariables.size();
                tDataVariables.add(new EDVTime(tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType));//this constructor gets source / sets destination actual_range
            } else if (EDVTimeStamp.hasTimeUnits(tSourceAtt, tAddAtt)) {
                EDVTimeStamp edv = new EDVTimeStamp(tSourceName, tSourceName, 
                    tSourceAtt, tAddAtt,
                    tSourceType); //this constructor gets source / sets destination actual_range
                tDataVariables.add(edv);
            } else {
                EDV edv = new EDV(tSourceName, tSourceName, 
                    tSourceAtt, tAddAtt,
                    tSourceType); //the constructor that reads actual_range
                edv.setActualRangeFromDestinationMinMax();
                tDataVariables.add(edv); 
            }
        }
        dataVariables = new EDV[tDataVariables.size()];
        for (int dv = 0; dv < tDataVariables.size(); dv++)
            dataVariables[dv] = (EDV)tDataVariables.get(dv);

        //ensure the setup is valid
        ensureValid(); //this ensures many things are set, e.g., sourceUrl

        //try to get sourceErddapVersion
        byte versionBytes[] = new byte[]{(byte)32};
        try {
            int po = localSourceUrl.indexOf("/erddap/");            
            String vUrl = localSourceUrl.substring(0, po + 8) + "version";
            versionBytes = quickRestartAttributes == null?
                SSR.getUrlResponseBytes(vUrl) : //has timeout and descriptive error 
                ((ByteArray)quickRestartAttributes.get("versionBytes")).toArray();
            String response[] = String2.split(new String(versionBytes, "UTF-8"), '\n');
            if (response[0].startsWith("ERDDAP_version=")) {
                sourceErddapVersion = String2.parseDouble(response[0].substring(15));
                if (Double.isNaN(sourceErddapVersion))
                    sourceErddapVersion = 1.22;
            }
        } catch (Throwable t) {
            //leave as default: 1.22
        }

        //try to subscribe to the remote dataset
        //It's ok that this is done every time. 
        //  emailIfAlreadyValid=false so there won't be excess email confirmations 
        //  and if flagKeyKey changes, the new tFlagUrl will be sent.
        //There is analogous code in EDDGridFromErddap.
        try {
            if (String2.isSomething(EDStatic.emailSubscriptionsFrom)) {
                int gpo = tLocalSourceUrl.indexOf("/tabledap/");
                String subscriptionUrl = tLocalSourceUrl.substring(0, gpo + 1) + Subscriptions.ADD_HTML + "?" +
                    "datasetID=" + File2.getNameNoExtension(tLocalSourceUrl) + 
                    "&email=" + EDStatic.emailSubscriptionsFrom +
                    "&emailIfAlreadyValid=false" + 
                    "&action=" + SSR.minimalPercentEncode(flagUrl(datasetID)); // %encode deals with & within flagUrl
                //String2.log("subscriptionUrl=" + subscriptionUrl); //don't normally display; flags are ~confidential
                SSR.touchUrl(subscriptionUrl, 60000);
            } else {
                String2.log(
                    String2.ERROR + ": Subscribing to the remote ERDDAP dataset failed because " +
                    "emailEverythingTo wasn't specified in setup.xml.");
            }
        } catch (Throwable st) {
            String2.log(
                "\n" + String2.ERROR + ": an exception occurred while trying to subscribe to the remote ERDDAP dataset.\n" + 
                "If the subscription hasn't been set up already, you may need to\n" + 
                "use a small reloadEveryNMinutes, or have the remote ERDDAP admin add onChange.\n\n" 
                //+ MustBe.throwableToString(st) //don't display; flags are ~confidential
                );
        }

        //save quickRestart info
        if (quickRestartAttributes == null) { //i.e., there is new info
            try {
                quickRestartAttributes = new Attributes();
                quickRestartAttributes.set("creationTimeMillis", "" + creationTimeMillis);
                quickRestartAttributes.set("dasBytes",     new ByteArray(dasBytes));
                quickRestartAttributes.set("ddsBytes",     new ByteArray(ddsBytes));
                quickRestartAttributes.set("versionBytes", new ByteArray(versionBytes));
                File2.makeDirectory(File2.getDirectory(quickRestartFullFileName()));
                NcHelper.writeAttributesToNc(quickRestartFullFileName(), 
                    quickRestartAttributes);
            } catch (Throwable t) {
                String2.log(MustBe.throwableToString(t));
            }
        }

        //finally
        if (verbose) String2.log(
            (reallyVerbose? "\n" + toString() : "") +
            "\n*** EDDTableFromErddap " + datasetID + " constructor finished. TIME=" + 
            (System.currentTimeMillis() - constructionStartMillis) + "\n"); 

    }

    /** This returns the source ERDDAP's version number, e.g., 1.22 */
    public double sourceErddapVersion() {return sourceErddapVersion;}

    /**
     * This returns the local version of the source ERDDAP's url.
     */
    public String getLocalSourceErddapUrl() {
        return localSourceUrl;
    }

    /**
     * This returns the public version of the source ERDDAP's url.
     */
    public String getPublicSourceErddapUrl() {
        return publicSourceErddapUrl;
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
     * @throws Throwable if trouble (notably, WaitThenTryAgainException)
     */
    public void getDataForDapQuery(String loggedInAs, String requestUrl, 
        String userDapQuery, TableWriter tableWriter) throws Throwable {

        //don't getSourceQueryFromDapQuery
        //in order to bypass removal of numeric regex.
        //ERDDAP can handle anything (by definition).

        //Read all data, then write to tableWriter.
        //Very unfortunate: JDAP reads all rows when it deserializes 
        //(see java docs for DSequence)
        //(that's why it can return getRowCount)
        //so there is no real way to read an opendapSequence in chunks (or row by row).
        //I can't split into subsets because I don't know which variable 
        //  to constrain or how to constrain it (it would change with different
        //  userDapQuery's).
        //I could write my own procedure to read DSequence (eek!).
        Table table = new Table();
        table.readOpendapSequence(localSourceUrl + 
            (userDapQuery == null || userDapQuery.length() == 0? "" : "?" + userDapQuery), 
            false);
        //String2.log(table.toString());
        standardizeResultsTable(requestUrl, userDapQuery, table); //not necessary?
        tableWriter.writeAllAndFinish(table);
    }

    /** 
     * This generates datasets.xml entries for all EDDTable from a remote ERDDAP.
     * The XML can then be edited by hand and added to the datasets.xml file.
     *
     * @param tLocalSourceUrl the base url for the dataset, e.g., 
     *   "http://coastwatch.pfeg.noaa.gov/erddap".
     *   This is a localSourceUrl since it has to be accessible, but usually it is also a publicSourceUrl.
     * @param keepOriginalDatasetIDs
     * @return a suggested chunk of xml for this dataset for use in datasets.xml 
     * @throws Throwable if trouble, e.g., if no Grid or Array variables are found.
     *    If no trouble, then a valid dataset.xml chunk has been returned.
     */
    public static String generateDatasetsXml(String tLocalSourceUrl, boolean keepOriginalDatasetIDs) 
        throws Throwable {

        String2.log("EDDTableFromErddap.generateDatasetsXml\n  tLocalSourceUrl=" + tLocalSourceUrl);

        //make the StringBuilder to hold the results and add documentation
        StringBuilder sb = new StringBuilder();
        sb.append(  //there is very similar text in EDDGridFromErddap
"<!-- Directions:\n" +
" * The ready-to-use XML below includes information for all of the EDDTable datasets\n" +
"   at the remote ERDDAP " + XML.encodeAsXML(tLocalSourceUrl) + "\n" +
" * If you want to add all of these datasets to your ERDDAP, just paste the XML\n" +
"   into your datasets.xml file.\n" +
" * The datasetIDs listed below are not the same as the remote datasets' datasetIDs.\n" +
"   They are generated automatically from the sourceURLs in a way that ensures that they are unique.\n" +
" * !!!reloadEveryNMinutes is left as the default 10080=oncePerWeek on the assumption\n" +
"   that the remote ERDDAP will accept your ERDDAP's request to subscribe to the dataset.\n" +
"   If you don't get emails from the remote ERDDAP asking you to validate your subscription\n" +
"   requests (perhaps because the remote ERDDAP has the subscription system turned off),\n" +
"   send an email to the admin asking that s/he add onChange tags to the datasets.\n" +
"   See the EDDTableFromErddap documentation.\n" + 
" * The XML needed for EDDTableFromErddap in datasets.xml has few options.  See\n" +
"   http://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html#EDDTableFromErddap .\n" +
"   If you want to alter a dataset's metadata or make other changes to a dataset,\n" +
"   use EDDTableFromDapSequence to access the dataset instead of EDDTableFromErddap.\n" +
" * If the remote ERDDAP is version 1.12 or below, this will generate incorrect, useless results.\n" +
"-->\n");

        //get the tabledap datasets in a json table
        String jsonUrl = tLocalSourceUrl + "/tabledap/index.json";
        String sourceInfo = SSR.getUrlResponseString(jsonUrl);
        if (reallyVerbose) String2.log(sourceInfo.substring(0, Math.min(sourceInfo.length(), 2000)));
        if (sourceInfo.indexOf("\"table\"") > 0) {
            Table table = new Table();
            table.readJson(jsonUrl, sourceInfo);   //they are sorted by title
            if (keepOriginalDatasetIDs)
                table.ascendingSort(new String[]{"Dataset ID"});

            PrimitiveArray urlCol = table.findColumn("tabledap");
            PrimitiveArray titleCol = table.findColumn("Title");
            PrimitiveArray datasetIdCol = table.findColumn("Dataset ID");

            //go through the rows of the table
            int nRows = table.nRows();
            for (int row = 0; row < nRows; row++) {
                String id = datasetIdCol.getString(row);
                if (EDDTableFromAllDatasets.DATASET_ID.equals(id))
                    continue;
                //localSourceUrl isn't available (and we generally don't want it)
                String tPublicSourceUrl = urlCol.getString(row);
                //Use unchanged tPublicSourceUrl or via suggestDatasetID?
                //I guess suggestDatasetID because it ensures a unique name for use in local ERDDAP.
                //?? Does it cause trouble to use a different datasetID here?
                String newID = keepOriginalDatasetIDs? id : suggestDatasetID(tPublicSourceUrl);
                sb.append(
"<dataset type=\"EDDTableFromErddap\" datasetID=\"" + newID + "\" active=\"true\">\n" +
"    <!-- " + XML.encodeAsXML(String2.replaceAll(titleCol.getString(row), "--", "- - ")) + " -->\n" +
"    <sourceUrl>" + XML.encodeAsXML(tPublicSourceUrl) + "</sourceUrl>\n" +
"</dataset>\n");
            }
        }

        //get the EDDTableFromErddap datasets 
        jsonUrl = tLocalSourceUrl + "/search/index.json?searchFor=EDDTableFromErddap";
        sourceInfo = "";
        try {
            sourceInfo = SSR.getUrlResponseString(jsonUrl);
        } catch (Throwable t) {
            //error if remote erddap has no EDDTableFromErddap's
        }
        if (reallyVerbose) String2.log(sourceInfo.substring(0, Math.min(sourceInfo.length(), 2000)));
        PrimitiveArray datasetIdCol;
        if (sourceInfo.indexOf("\"table\"") > 0) {
            if (reallyVerbose) String2.log("searchFor=eddGridFromErddap: " + sourceInfo);
            Table table = new Table();
            table.readJson(jsonUrl, sourceInfo);   //they are sorted by title
            datasetIdCol = table.findColumn("Dataset ID");
        } else {
            datasetIdCol = new StringArray();
        }

        sb.append(
            "\n<!-- Of the datasets above, the following datasets are EDDTableFromErddap's at the remote ERDDAP.\n" +
            "It would be best if you contacted the remote ERDDAP's administrator and requested the dataset XML\n" +
            "that is being using for these datasets so your ERDDAP can access the original ERDDAP source.\n" +
            "The remote EDDTableFromErddap datasets are:\n");
        if (datasetIdCol.size() == 0)
            sb.append("(none)");
        else sb.append(String2.noLongLinesAtSpace(datasetIdCol.toString(), 80, ""));
        sb.append("\n-->\n");

        String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
        return sb.toString();
    }

    /**
     * testGenerateDatasetsXml
     */
    public static void testGenerateDatasetsXml() throws Throwable {
        testVerboseOn();

        //test local generateDatasetsXml.  In tests, always use non-https url.
        try { 
            String results = generateDatasetsXml(EDStatic.erddapUrl, false) + "\n"; 
            String2.log("results=\n" + results);

            //GenerateDatasetsXml
            String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
                "EDDTableFromErddap",
                EDStatic.erddapUrl,
                "false"}, //keep original names?
              false); //doIt loop?
            Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

String expected = 
"<!-- Directions:\n" +
" * The ready-to-use XML below includes information for all of the EDDTable datasets\n";

            Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), 
                expected, "");

expected = 
"<dataset type=\"EDDTableFromErddap\" datasetID=\"0_0_4b4a_d1d6_068b\" active=\"true\">\n" +
"    <!-- GLOBEC NEP Rosette Bottle Data (2002) -->\n" +
"    <sourceUrl>http://127.0.0.1:8080/cwexperimental/tabledap/erdGlobecBottle</sourceUrl>\n" +
"</dataset>\n";

            int po = results.indexOf(expected.substring(0, 80));
            Test.ensureEqual(results.substring(po, po + expected.length()), expected, "");

expected = 
"<!-- Of the datasets above, the following datasets are EDDTableFromErddap's at the remote ERDDAP.\n";
            po = results.indexOf(expected.substring(0, 20));
            Test.ensureEqual(results.substring(po, po + expected.length()), expected, "results=\n" + results);
try {
            Test.ensureTrue(results.indexOf("rGlobecBottle", po) > 0, "results=\n" + results);
} catch (Throwable t) {
    String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
        "\n!!! This will fail until release 1.30.\nPress ^C to stop or Enter to continue..."); 
}


            //ensure it is ready-to-use by making a dataset from it
            //BUT THE DATASET VARIES depending on what is running in ERDDAP localhost
            EDD edd = oneFromXmlFragment(results);
            if (edd.title().equals("GLOBEC NEP Rosette Bottle Data (2002)")) {
                Test.ensureEqual(edd.datasetID(), "0_0_4b4a_d1d6_068b", "");
                Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
                    "cruise_id, ship, cast, longitude, latitude, time, bottle_posn, chl_a_total, chl_a_10um, phaeo_total, phaeo_10um, sal00, sal11, temperature0, temperature1, fluor_v, xmiss_v, PO4, N_N, NO3, Si, NO2, NH4, oxygen, par", 
                    "");
            } else if (edd.title().equals("CalCOFI Fish Larvae Count")) {
                Test.ensureEqual(edd.datasetID(), "0_0_0e34_4614_10bc", "");
                Test.ensureTrue(String2.toCSSVString(edd.dataVariableDestinationNames()).startsWith( 
"longitude, latitude, altitude, time, ID, line_number, station_number, ship, TotalFishEggs, " +
"TotalFishLarvae, TotalPlanktonVolume, Argyropelecus_spp_Larvae, Arrow_goby_Larvae, " +
"Artedius_spp_Larvae, Atlantic_fangjaw_Larvae, Aurora_rockfish_Larvae, Barradcudinas_Larvae"),
                    "");
            }



        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nError using generateDatasetsXml on " + 
                EDStatic.erddapUrl + //in tests, always use non-https url                
                "\nPress ^C to stop or Enter to continue..."); 
        }

    }


    /**
     * The basic tests of this class (erdGlobecBottle).
     */
    public static void testBasic(boolean testLocalErddapToo) throws Throwable {
        testVerboseOn();
        String name, tName, results, tResults, expected, expected2, expected3, userDapQuery, tQuery;
        String error = "";
        int epo, tPo;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10); //just 10 till 1.40 released, then 14
        String mapDapQuery = "longitude,latitude,NO3,time&latitude>0&altitude>-5&time>=2002-08-03";
        userDapQuery = "longitude,NO3,time,ship&latitude>0&time>=2002-08-03";
        String localUrl = EDStatic.erddapUrl + //in tests, always use non-https url                
            "/tabledap/rGlobecBottle";

        try {

            EDDTable globecBottle = (EDDTableFromErddap)oneFromDatasetXml("rGlobecBottle"); //should work

            //*** test getting das for entire dataset
            String2.log("\n****************** EDDTableFromErddap.test das dds for entire dataset\n");
            tName = globecBottle.makeNewFileForDapQuery(null, null, "", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_Entire", ".das"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = //see OpendapHelper.EOL for comments
"Attributes {\n" +
" s {\n" +
"  cruise_id {\n" +
"    String cf_role \"trajectory_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Cruise ID\";\n" +
"  }\n" +
"  ship {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Ship\";\n" +
"  }\n" +
"  cast {\n" +
"    Int16 _FillValue 32767;\n" +
"    Int16 actual_range 1, 127;\n" +
"    Float64 colorBarMaximum 140.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Cast Number\";\n" +
"    Int16 missing_value 32767;\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 _FillValue NaN;\n" +
"    Float32 actual_range -126.2, -124.1;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 _FillValue NaN;\n" +
"    Float32 actual_range 41.9, 44.65;\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 _FillValue NaN;\n" +
"    Float64 actual_range 1.02272886e+9, 1.02978828e+9;\n" +
"    String axis \"T\";\n" +
"    String cf_role \"profile_id\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    Float64 missing_value NaN;\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  bottle_posn {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    Byte _FillValue 127;\n" +
"    Byte actual_range 0, 12;\n" +
"    String axis \"Z\";\n" +
"    Float64 colorBarMaximum 12.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Bottle Number\";\n" +
"    Byte missing_value -128;\n" +
"  }\n" +
"  chl_a_total {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range -2.602, 40.17;\n" +
"    Float64 colorBarMaximum 30.0;\n" +
"    Float64 colorBarMinimum 0.03;\n" +
"    String colorBarScale \"Log\";\n" +
"    String ioos_category \"Ocean Color\";\n" +
"    String long_name \"Chlorophyll-a\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"concentration_of_chlorophyll_in_sea_water\";\n" +
"    String units \"ug L-1\";\n" +
"  }\n" +
"  chl_a_10um {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 0.21, 11.495;\n" +
"    Float64 colorBarMaximum 30.0;\n" +
"    Float64 colorBarMinimum 0.03;\n" +
"    String colorBarScale \"Log\";\n" +
"    String ioos_category \"Ocean Color\";\n" +
"    String long_name \"Chlorophyll-a after passing 10um screen\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"concentration_of_chlorophyll_in_sea_water\";\n" +
"    String units \"ug L-1\";\n" +
"  }\n" +
"  phaeo_total {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range -3.111, 33.821;\n" +
"    Float64 colorBarMaximum 30.0;\n" +
"    Float64 colorBarMinimum 0.03;\n" +
"    String colorBarScale \"Log\";\n" +
"    String ioos_category \"Ocean Color\";\n" +
"    String long_name \"Total Phaeopigments\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String units \"ug L-1\";\n" +
"  }\n" +
"  phaeo_10um {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 0.071, 5.003;\n" +
"    Float64 colorBarMaximum 30.0;\n" +
"    Float64 colorBarMinimum 0.03;\n" +
"    String colorBarScale \"Log\";\n" +
"    String ioos_category \"Ocean Color\";\n" +
"    String long_name \"Phaeopigments 10um\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String units \"ug L-1\";\n" +
"  }\n" +
"  sal00 {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 28.3683, 34.406;\n" +
"    Float64 colorBarMaximum 37.0;\n" +
"    Float64 colorBarMinimum 32.0;\n" +
"    String ioos_category \"Salinity\";\n" +
"    String long_name \"Salinity from T0 and C0 Sensors\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"sea_water_salinity\";\n" +
"    String units \"PSU\";\n" +
"  }\n" +
"  sal11 {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 0.0, 34.4076;\n" +
"    Float64 colorBarMaximum 37.0;\n" +
"    Float64 colorBarMinimum 32.0;\n" +
"    String ioos_category \"Salinity\";\n" +
"    String long_name \"Salinity from T1 and C1 Sensors\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"sea_water_salinity\";\n" +
"    String units \"PSU\";\n" +
"  }\n" +
"  temperature0 {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 3.6186, 16.871;\n" +
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Sea Water Temperature from T0 Sensor\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"sea_water_temperature\";\n" +
"    String units \"degree_C\";\n" +
"  }\n" +
"  temperature1 {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 3.6179, 16.863;\n" +
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Sea Water Temperature from T1 Sensor\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"sea_water_temperature\";\n" +
"    String units \"degree_C\";\n" +
"  }\n" +
"  fluor_v {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 0.046, 5.0;\n" +
"    Float64 colorBarMaximum 5.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Ocean Color\";\n" +
"    String long_name \"Fluorescence Voltage\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String units \"volts\";\n" +
"  }\n" +
"  xmiss_v {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 0.493, 4.638;\n" +
"    Float64 colorBarMaximum 5.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Optical Properties\";\n" +
"    String long_name \"Transmissivity Voltage\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String units \"volts\";\n" +
"  }\n" +
"  PO4 {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 0.07, 3.237;\n" +
"    Float64 colorBarMaximum 4.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Dissolved Nutrients\";\n" +
"    String long_name \"Phosphate\";\n" +  
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"mole_concentration_of_phosphate_in_sea_water\";\n" +
"    String units \"micromoles L-1\";\n" +
"  }\n" +
"  N_N {\n" +
"    Float32 _FillValue -99.0;\n" +
"    Float32 actual_range -0.1, 43.47;\n" +
"    Float64 colorBarMaximum 50.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Dissolved Nutrients\";\n" +
"    String long_name \"Nitrate plus Nitrite\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"moles_of_nitrate_and_nitrite_per_unit_mass_in_sea_water\";\n" +
"    String units \"micromoles L-1\";\n" +
"  }\n" +
"  NO3 {\n" +
"    Float32 _FillValue -99.0;\n" +
"    Float32 actual_range 0.0, 99.79;\n" +
"    Float64 colorBarMaximum 50.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Dissolved Nutrients\";\n" +
"    String long_name \"Nitrate\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"mole_concentration_of_nitrate_in_sea_water\";\n" +
"    String units \"micromoles L-1\";\n" +
"  }\n" +
"  Si {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range -0.08, 117.12;\n" +
"    Float64 colorBarMaximum 50.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Dissolved Nutrients\";\n" +
"    String long_name \"Silicate\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"mole_concentration_of_silicate_in_sea_water\";\n" +
"    String units \"micromoles L-1\";\n" +
"  }\n" +
"  NO2 {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range -0.03, 0.757;\n" +
"    Float64 colorBarMaximum 1.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Dissolved Nutrients\";\n" +
"    String long_name \"Nitrite\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"mole_concentration_of_nitrite_in_sea_water\";\n" +
"    String units \"micromoles L-1\";\n" +
"  }\n" +
"  NH4 {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range -0.14, 4.93;\n" +
"    Float64 colorBarMaximum 5.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Dissolved Nutrients\";\n" +
"    String long_name \"Ammonium\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"mole_concentration_of_ammonium_in_sea_water\";\n" +
"    String units \"micromoles L-1\";\n" +
"  }\n" +
"  oxygen {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 0.07495, 9.93136;\n" +
"    Float64 colorBarMaximum 10.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Dissolved O2\";\n" +
"    String long_name \"Oxygen\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"volume_fraction_of_oxygen_in_sea_water\";\n" +
"    String units \"mL L-1\";\n" +
"  }\n" +
"  par {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 0.1515, 3.261;\n" +
"    Float64 colorBarMaximum 3.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Ocean Color\";\n" +
"    String long_name \"Photosynthetically Active Radiation\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String units \"volts\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_altitude_proxy \"bottle_posn\";\n" +
"    String cdm_data_type \"TrajectoryProfile\";\n" +
"    String cdm_profile_variables \"cast, longitude, latitude, time\";\n" +
"    String cdm_trajectory_variables \"cruise_id, ship\";\n" +
"    String Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    Float64 Easternmost_Easting -124.1;\n" +
"    String featureType \"TrajectoryProfile\";\n" +
"    Float64 geospatial_lat_max 44.65;\n" +
"    Float64 geospatial_lat_min 41.9;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max -124.1;\n" +
"    Float64 geospatial_lon_min -126.2;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String history \"" + today;
            Test.ensureEqual(results.substring(0, expected.length()), expected, 
                "\nresults=\n" + results);
    
// +" http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle\n";
//this part varies local vs. remote
//today + " http://coastwatch.pfeg.noaa.gov/erddap/tabledap/erdGlobecBottle.das\n" +
//today + " http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle\n" +
//today + " http://127.0.0.1:8080/cwexperimental/tabledap/rGlobecBottle.das\";\n" +
expected2 = 
"    String infoUrl \"http://www.globec.org/\";\n" +
"    String institution \"GLOBEC\";\n" +
"    String keywords \"10um,\n" +
"Biosphere > Vegetation > Photosynthetically Active Radiation,\n" +
"Oceans > Ocean Chemistry > Ammonia,\n" +
"Oceans > Ocean Chemistry > Chlorophyll,\n" +
"Oceans > Ocean Chemistry > Nitrate,\n" +
"Oceans > Ocean Chemistry > Nitrite,\n" +
"Oceans > Ocean Chemistry > Nitrogen,\n" +
"Oceans > Ocean Chemistry > Oxygen,\n" +
"Oceans > Ocean Chemistry > Phosphate,\n" +
"Oceans > Ocean Chemistry > Pigments,\n" +
"Oceans > Ocean Chemistry > Silicate,\n" +
"Oceans > Ocean Optics > Attenuation/Transmission,\n" +
"Oceans > Ocean Temperature > Water Temperature,\n" +
"Oceans > Salinity/Density > Salinity,\n" +
"active, after, ammonia, ammonium, attenuation, biosphere, bottle, cast, chemistry, chlorophyll, chlorophyll-a, color, concentration, concentration_of_chlorophyll_in_sea_water, cruise, data, density, dissolved, dissolved nutrients, dissolved o2, fluorescence, fraction, from, globec, identifier, mass, mole, mole_concentration_of_ammonium_in_sea_water, mole_concentration_of_nitrate_in_sea_water, mole_concentration_of_nitrite_in_sea_water, mole_concentration_of_phosphate_in_sea_water, mole_concentration_of_silicate_in_sea_water, moles, moles_of_nitrate_and_nitrite_per_unit_mass_in_sea_water, n02, nep, nh4, nitrate, nitrite, nitrogen, no3, number, nutrients, o2, ocean, ocean color, oceans, optical, optical properties, optics, oxygen, passing, per, phaeopigments, phosphate, photosynthetically, pigments, plus, po4, properties, radiation, rosette, salinity, screen, sea, sea_water_salinity, sea_water_temperature, seawater, sensor, sensors, ship, silicate, temperature, time, total, transmission, transmissivity, unit, vegetation, voltage, volume, volume_fraction_of_oxygen_in_sea_water, water\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String Metadata_Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    Float64 Northernmost_Northing 44.65;\n" +
"    String sourceUrl \"(local files; contact erd.data@noaa.gov)\";\n" +
"    Float64 Southernmost_Northing 41.9;\n" +
"    String standard_name_vocabulary \"CF-12\";\n" +
"    String subsetVariables \"cruise_id, ship, cast, longitude, latitude, time\";\n" +
"    String summary \"GLOBEC (GLOBal Ocean ECosystems Dynamics) NEP (Northeast Pacific)\n" +
"Rosette Bottle Data from New Horizon Cruise (NH0207: 1-19 August 2002).\n" +
"Notes:\n" +
"Physical data processed by Jane Fleischbein (OSU).\n" +
"Chlorophyll readings done by Leah Feinberg (OSU).\n" +
"Nutrient analysis done by Burke Hales (OSU).\n" +
"Sal00 - salinity calculated from primary sensors (C0,T0).\n" +
"Sal11 - salinity calculated from secondary sensors (C1,T1).\n" +
"secondary sensor pair was used in final processing of CTD data for\n" +
"most stations because the primary had more noise and spikes. The\n" +
"primary pair were used for cast #9, 24, 48, 111 and 150 due to\n" +
"multiple spikes or offsets in the secondary pair.\n" +
"Nutrient samples were collected from most bottles; all nutrient data\n" +
"developed from samples frozen during the cruise and analyzed ashore;\n" +
"data developed by Burke Hales (OSU).\n" +
"Operation Detection Limits for Nutrient Concentrations\n" +
"Nutrient  Range         Mean    Variable         Units\n" +
"PO4       0.003-0.004   0.004   Phosphate        micromoles per liter\n" +
"N+N       0.04-0.08     0.06    Nitrate+Nitrite  micromoles per liter\n" +
"Si        0.13-0.24     0.16    Silicate         micromoles per liter\n" +
"NO2       0.003-0.004   0.003   Nitrite          micromoles per liter\n" +
"Dates and Times are UTC.\n" +
"\n" +
"For more information, see\n" +
"http://cis.whoi.edu/science/bcodmo/dataset.cfm?id=10180&flag=view\n" +
"\n" +
"Inquiries about how to access this data should be directed to\n" +
"Dr. Hal Batchelder (hbatchelder@coas.oregonstate.edu).\";\n" +
"    String time_coverage_end \"2002-08-19T20:18:00Z\";\n" +
"    String time_coverage_start \"2002-05-30T03:21:00Z\";\n" +
"    String title \"GLOBEC NEP Rosette Bottle Data (2002)\";\n" +
"    Float64 Westernmost_Easting -126.2;\n" +
"  }\n" +
"}\n";
            tPo = results.indexOf(expected2.substring(0, 17));
            Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
            Test.ensureEqual(results.substring(tPo, 
                Math.min(results.length(), tPo + expected2.length())), 
                expected2, "results=\n" + results);

            if (testLocalErddapToo) {
                results = SSR.getUrlResponseString(localUrl + ".das");
                Test.ensureEqual(results.substring(0, expected.length()), 
                    expected, "\nresults=\n" + results);

                tPo = results.indexOf(expected2.substring(0, 17));
                Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
                Test.ensureEqual(results.substring(tPo, 
                    Math.min(results.length(), tPo + expected2.length())), 
                    expected2, "results=\n" + results);
            }
            
            //*** test getting dds for entire dataset
            tName = globecBottle.makeNewFileForDapQuery(null, null, "", 
                EDStatic.fullTestCacheDirectory, 
                globecBottle.className() + "_Entire", ".dds"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String cruise_id;\n" +
"    String ship;\n" +
"    Int16 cast;\n" +
"    Float32 longitude;\n" +
"    Float32 latitude;\n" +
"    Float64 time;\n" +
"    Byte bottle_posn;\n" +
"    Float32 chl_a_total;\n" +
"    Float32 chl_a_10um;\n" +
"    Float32 phaeo_total;\n" +
"    Float32 phaeo_10um;\n" +
"    Float32 sal00;\n" +
"    Float32 sal11;\n" +
"    Float32 temperature0;\n" +
"    Float32 temperature1;\n" +
"    Float32 fluor_v;\n" +
"    Float32 xmiss_v;\n" +
"    Float32 PO4;\n" +
"    Float32 N_N;\n" +
"    Float32 NO3;\n" +
"    Float32 Si;\n" +
"    Float32 NO2;\n" +
"    Float32 NH4;\n" +
"    Float32 oxygen;\n" +
"    Float32 par;\n" +
"  } s;\n" +
"} s;\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);

            if (testLocalErddapToo) {
                results = SSR.getUrlResponseString(localUrl + ".dds");
                Test.ensureEqual(results, expected, "\nresults=\n" + results);
            }

            //*** test DAP data access form
            tName = globecBottle.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                globecBottle.className() + "_Entire", ".html"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = "<option>.png - View a standard, medium-sized .png image file with a graph or map.";
            expected2 = "    String _CoordinateAxisType &quot;Lon&quot;;";
            Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
            Test.ensureTrue(results.indexOf(expected2) > 0, "\nresults=\n" + results);
            //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            if (testLocalErddapToo) {
                results = SSR.getUrlResponseString(localUrl + ".html");
                Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
                Test.ensureTrue(results.indexOf(expected2) > 0, "\nresults=\n" + results);
            }

            //*** test make data files
            String2.log("\n****************** EDDTableFromErddap.test make DATA FILES\n");       

            //.asc
            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
                globecBottle.className() + "_Data", ".asc"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
    "Dataset {\n" +
    "  Sequence {\n" +
    "    Float32 longitude;\n" +
    "    Float32 NO3;\n" +
    "    Float64 time;\n" +
    "    String ship;\n" +
    "  } s;\n" +
    "} s;\n" +
    "---------------------------------------------\n" +
    "s.longitude, s.NO3, s.time, s.ship\n" +
    "-124.4, 35.7, 1.02833814E9, \"New_Horizon\"\n";
            expected2 = "-124.8, -9999.0, 1.02835902E9, \"New_Horizon\"\n"; //row with missing value  has source missing value
            expected3 = "-124.1, 24.45, 1.02978828E9, \"New_Horizon\"\n"; //last row
            Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
            Test.ensureTrue(results.indexOf(expected2) > 0, "\nresults=\n" + results);
            Test.ensureTrue(results.endsWith(expected3), "\nresults=\n" + results);

            if (testLocalErddapToo) {
                results = SSR.getUrlResponseString(localUrl + ".asc?" + userDapQuery);
                Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
                Test.ensureTrue(results.indexOf(expected2) > 0, "\nresults=\n" + results);
                Test.ensureTrue(results.endsWith(expected3), "\nresults=\n" + results);
            }

            //.csv
            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
                globecBottle.className() + "_Data", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
"longitude,NO3,time,ship\n" +
"degrees_east,micromoles L-1,UTC,\n" +
"-124.4,35.7,2002-08-03T01:29:00Z,New_Horizon\n" +
"-124.4,35.48,2002-08-03T01:29:00Z,New_Horizon\n" +
"-124.4,31.61,2002-08-03T01:29:00Z,New_Horizon\n";
            expected2 = "-124.8,NaN,2002-08-03T07:17:00Z,New_Horizon\n"; //row with missing value  has source missing value
            expected3 = "-124.1,24.45,2002-08-19T20:18:00Z,New_Horizon\n"; //last row
            Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
            Test.ensureTrue(results.indexOf(expected2) > 0, "\nresults=\n" + results);
            Test.ensureTrue(results.endsWith(expected3), "\nresults=\n" + results);

            if (testLocalErddapToo) {
                try {
                    results = SSR.getUrlResponseString(localUrl + ".csv?" + userDapQuery);
                    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
                    Test.ensureTrue(results.indexOf(expected2) > 0, "\nresults=\n" + results);
                    Test.ensureTrue(results.endsWith(expected3), "\nresults=\n" + results);
                } catch (Throwable t) {
                    String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                        "\nUnexpected error.\nPress ^C to stop or Enter to continue...");
                }
            }

            //.das     das isn't affected by userDapQuery
            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
                globecBottle.className() + "_Data", ".das"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
"Attributes {\n" +
" s {\n" +
"  cruise_id {\n" +
"    String cf_role \"trajectory_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Cruise ID\";\n" +
"  }\n" +
"  ship {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Ship\";\n" +
"  }\n" +
"  cast {\n" +
"    Int16 _FillValue 32767;\n" +
"    Int16 actual_range 1, 127;\n" +
"    Float64 colorBarMaximum 140.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Cast Number\";\n" +
"    Int16 missing_value 32767;\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 _FillValue NaN;\n" +
"    Float32 actual_range -126.2, -124.1;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n";
            Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), 
                expected, "\nresults=\n" + results);
            expected = 
"  par {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float32 actual_range 0.1515, 3.261;\n" +
"    Float64 colorBarMaximum 3.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Ocean Color\";\n" +
"    String long_name \"Photosynthetically Active Radiation\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String units \"volts\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_altitude_proxy \"bottle_posn\";\n" +
"    String cdm_data_type \"TrajectoryProfile\";\n" +
"    String cdm_profile_variables \"cast, longitude, latitude, time\";\n" +
"    String cdm_trajectory_variables \"cruise_id, ship\";\n" +
"    String Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" + 
"    Float64 Easternmost_Easting -124.1;\n" +
"    String featureType \"TrajectoryProfile\";\n" +
"    Float64 geospatial_lat_max 44.65;\n" +
"    Float64 geospatial_lat_min 41.9;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max -124.1;\n" +
"    Float64 geospatial_lon_min -126.2;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String history \"" + today;
        tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);
            
//+ " http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle\n" +
//today + " http://coastwatch.pfeg.noaa.gov/erddap/tabledap/erdGlobecBottle.das\n" +
//today + " http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle\n" +
//today + " http://127.0.0.1:8080/cwexperimental/
expected =
"tabledap/rGlobecBottle.das\";\n" +
"    String id \"Globec_bottle_data_2002\";\n" +
"    String infoUrl \"http://www.globec.org/\";\n" +
"    String institution \"GLOBEC\";\n" +
"    String keywords \"10um,\n" +
"Biosphere > Vegetation > Photosynthetically Active Radiation,\n" +
"Oceans > Ocean Chemistry > Ammonia,\n" +
"Oceans > Ocean Chemistry > Chlorophyll,\n" +
"Oceans > Ocean Chemistry > Nitrate,\n" +
"Oceans > Ocean Chemistry > Nitrite,\n" +
"Oceans > Ocean Chemistry > Nitrogen,\n" +
"Oceans > Ocean Chemistry > Oxygen,\n" +
"Oceans > Ocean Chemistry > Phosphate,\n" +
"Oceans > Ocean Chemistry > Pigments,\n" +
"Oceans > Ocean Chemistry > Silicate,\n" +
"Oceans > Ocean Optics > Attenuation/Transmission,\n" +
"Oceans > Ocean Temperature > Water Temperature,\n" +
"Oceans > Salinity/Density > Salinity,\n" +
"active, after, ammonia, ammonium, attenuation, biosphere, bottle, cast, chemistry, chlorophyll, chlorophyll-a, color, concentration, concentration_of_chlorophyll_in_sea_water, cruise, data, density, dissolved, dissolved nutrients, dissolved o2, fluorescence, fraction, from, globec, identifier, mass, mole, mole_concentration_of_ammonium_in_sea_water, mole_concentration_of_nitrate_in_sea_water, mole_concentration_of_nitrite_in_sea_water, mole_concentration_of_phosphate_in_sea_water, mole_concentration_of_silicate_in_sea_water, moles, moles_of_nitrate_and_nitrite_per_unit_mass_in_sea_water, n02, nep, nh4, nitrate, nitrite, nitrogen, no3, number, nutrients, o2, ocean, ocean color, oceans, optical, optical properties, optics, oxygen, passing, per, phaeopigments, phosphate, photosynthetically, pigments, plus, po4, properties, radiation, rosette, salinity, screen, sea, sea_water_salinity, sea_water_temperature, seawater, sensor, sensors, ship, silicate, temperature, time, total, transmission, transmissivity, unit, vegetation, voltage, volume, volume_fraction_of_oxygen_in_sea_water, water\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String Metadata_Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    Float64 Northernmost_Northing 44.65;\n" +
"    String sourceUrl \"(local files; contact erd.data@noaa.gov)\";\n" +
"    Float64 Southernmost_Northing 41.9;\n" +
"    String standard_name_vocabulary \"CF-12\";\n" +
"    String subsetVariables \"cruise_id, ship, cast, longitude, latitude, time\";\n" +
"    String summary \"GLOBEC (GLOBal Ocean ECosystems Dynamics) NEP (Northeast Pacific)\n" +
"Rosette Bottle Data from New Horizon Cruise (NH0207: 1-19 August 2002).\n" +
"Notes:\n" +
"Physical data processed by Jane Fleischbein (OSU).\n" +
"Chlorophyll readings done by Leah Feinberg (OSU).\n" +
"Nutrient analysis done by Burke Hales (OSU).\n" +
"Sal00 - salinity calculated from primary sensors (C0,T0).\n" +
"Sal11 - salinity calculated from secondary sensors (C1,T1).\n" +
"secondary sensor pair was used in final processing of CTD data for\n" +
"most stations because the primary had more noise and spikes. The\n" +
"primary pair were used for cast #9, 24, 48, 111 and 150 due to\n" +
"multiple spikes or offsets in the secondary pair.\n" +
"Nutrient samples were collected from most bottles; all nutrient data\n" +
"developed from samples frozen during the cruise and analyzed ashore;\n" +
"data developed by Burke Hales (OSU).\n" +
"Operation Detection Limits for Nutrient Concentrations\n" +
"Nutrient  Range         Mean    Variable         Units\n" +
"PO4       0.003-0.004   0.004   Phosphate        micromoles per liter\n" +
"N+N       0.04-0.08     0.06    Nitrate+Nitrite  micromoles per liter\n" +
"Si        0.13-0.24     0.16    Silicate         micromoles per liter\n" +
"NO2       0.003-0.004   0.003   Nitrite          micromoles per liter\n" +
"Dates and Times are UTC.\n" +
"\n" +
"For more information, see\n" +
"http://cis.whoi.edu/science/bcodmo/dataset.cfm?id=10180&flag=view\n" +
"\n" +
"Inquiries about how to access this data should be directed to\n" +
"Dr. Hal Batchelder (hbatchelder@coas.oregonstate.edu).\";\n" +
"    String time_coverage_end \"2002-08-19T20:18:00Z\";\n" +
"    String time_coverage_start \"2002-05-30T03:21:00Z\";\n" +
"    String title \"GLOBEC NEP Rosette Bottle Data (2002)\";\n" +
"    Float64 Westernmost_Easting -126.2;\n" +
"  }\n" +
"}\n";
            tPo = results.indexOf(expected.substring(0, 17));
            Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
            Test.ensureEqual(results.substring(tPo), expected, "results=\n" + results);

            //.dds 
            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
                globecBottle.className() + "_Data", ".dds"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
    "Dataset {\n" +
    "  Sequence {\n" +
    "    Float32 longitude;\n" +
    "    Float32 NO3;\n" +
    "    Float64 time;\n" +
    "    String ship;\n" +
    "  } s;\n" +
    "} s;\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);

            if (testLocalErddapToo) {
                results = SSR.getUrlResponseString(localUrl + ".dds?" + userDapQuery);
                Test.ensureEqual(results, expected, "\nresults=\n" + results);
            }

            //.dods
            //tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            //    globecBottle.className() + "_Data", ".dods"); 
            //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
            try {
                String2.log("\ndo .dods test");
                String tUrl = EDStatic.erddapUrl + //in tests, always use non-https url
                    "/tabledap/" + globecBottle.datasetID();
                //for diagnosing during development:
                //String2.log(String2.annotatedString(SSR.getUrlResponseString(
                //    "http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_vpt.dods?stn_id&unique()")));
                //String2.log("\nDAS RESPONSE=" + SSR.getUrlResponseString(tUrl + ".das?" + userDapQuery));
                //String2.log("\nDODS RESPONSE=" + String2.annotatedString(SSR.getUrlResponseString(tUrl + ".dods?" + userDapQuery)));

                //test if table.readOpendapSequence works with Erddap opendap server
                //!!!THIS READS DATA FROM ERDDAP SERVER RUNNING ON EDStatic.erddapUrl!!! //in tests, always use non-https url                
                //!!!THIS IS NOT JUST A LOCAL TEST!!!
                Table tTable = new Table();
                tTable.readOpendapSequence(tUrl + "?" + userDapQuery, false);
                Test.ensureEqual(tTable.globalAttributes().getString("title"), "GLOBEC NEP Rosette Bottle Data (2002)", "");
                Test.ensureEqual(tTable.columnAttributes(2).getString("units"), EDV.TIME_UNITS, "");
                Test.ensureEqual(tTable.getColumnNames(), new String[]{"longitude", "NO3", "time", "ship"}, "");
                Test.ensureEqual(tTable.getFloatData(0, 0), -124.4f, "");
                Test.ensureEqual(tTable.getFloatData(1, 0), 35.7f, "");
                Test.ensureEqual(tTable.getDoubleData(2, 0), 1.02833814E9, "");
                Test.ensureEqual(tTable.getStringData(3, 0), "New_Horizon", "");
                String2.log("  .dods test succeeded");
            } catch (Throwable t) {
                String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                    "\nError accessing " + EDStatic.erddapUrl + //in tests, always use non-https url
                    " and reading erddap as a data source." +
                    "\nPress ^C to stop or Enter to continue..."); 
            }

            //.nc    
            //!!! This is also a test of missing_value and _FillValue both active
            String tUserDapQuery = "longitude,NO3,time,ship&latitude>0&time>=2002-08-14&time<=2002-08-15";
            tName = globecBottle.makeNewFileForDapQuery(null, null, tUserDapQuery, EDStatic.fullTestCacheDirectory, 
                globecBottle.className() + "_Data", ".nc"); 
            results = NcHelper.dumpString(EDStatic.fullTestCacheDirectory + tName, true);
            String tHeader = 
"netcdf EDDTableFromErddap_Data.nc {\n" +
"  dimensions:\n" +
"    row = 100;\n" +
"    ship_strlen = 11;\n" +
"  variables:\n" +
"    float longitude(row=100);\n" +
"      :_CoordinateAxisType = \"Lon\";\n" +
"      :_FillValue = NaNf; // float\n" +
"      :actual_range = -125.67f, -124.8f; // float\n" +
"      :axis = \"X\";\n" +
"      :ioos_category = \"Location\";\n" +
"      :long_name = \"Longitude\";\n" +
"      :missing_value = NaNf; // float\n" +
"      :standard_name = \"longitude\";\n" +
"      :units = \"degrees_east\";\n" +
"\n" +
"    float NO3(row=100);\n" +
"      :_FillValue = -99.0f; // float\n" +
"      :actual_range = 0.46f, 34.09f; // float\n" +
"      :colorBarMaximum = 50.0; // double\n" +
"      :colorBarMinimum = 0.0; // double\n" +
"      :ioos_category = \"Dissolved Nutrients\";\n" +
"      :long_name = \"Nitrate\";\n" +
"      :missing_value = -9999.0f; // float\n" +
"      :standard_name = \"mole_concentration_of_nitrate_in_sea_water\";\n" +
"      :units = \"micromoles L-1\";\n" +
"\n" +
"    double time(row=100);\n" +
"      :_CoordinateAxisType = \"Time\";\n" +
"      :_FillValue = NaN; // double\n" +
"      :actual_range = 1.02928674E9, 1.02936804E9; // double\n" +
"      :axis = \"T\";\n" +
"      :cf_role = \"profile_id\";\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Time\";\n" +
"      :missing_value = NaN; // double\n" +
"      :standard_name = \"time\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    char ship(row=100, ship_strlen=11);\n" +
"      :ioos_category = \"Identifier\";\n" +
"      :long_name = \"Ship\";\n" +
"\n" +
"  // global attributes:\n" +
"  :cdm_altitude_proxy = \"bottle_posn\";\n" +
"  :cdm_data_type = \"TrajectoryProfile\";\n" +
"  :cdm_profile_variables = \"cast, longitude, latitude, time\";\n" +
"  :cdm_trajectory_variables = \"cruise_id, ship\";\n" +
"  :Conventions = \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" + 
//goofy lat=double  lon=float!
"  :Easternmost_Easting = -124.8f; // float\n" +
"  :featureType = \"TrajectoryProfile\";\n" +
"  :geospatial_lat_units = \"degrees_north\";\n" +
"  :geospatial_lon_max = -124.8f; // float\n" +
"  :geospatial_lon_min = -125.67f; // float\n" +
"  :geospatial_lon_units = \"degrees_east\";\n" +
"  :history = \"" + today;
        tResults = results.substring(0, tHeader.length());
        Test.ensureEqual(tResults, tHeader, "\nresults=\n" + results);


//+ " http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle\n" +
//today + " http://coastwatch.pfeg.noaa.gov/erddap/tabledap/erdGlobecBottle.das\n" +
//today + " http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle\n" +
//today + " http://127.0.0.1:8080/cwexperimental/
String tHeader2 =
"tabledap/rGlobecBottle.nc?longitude,NO3,time,ship&latitude>0&time>=2002-08-14&time<=2002-08-15\";\n" +
"  :id = \"Globec_bottle_data_2002\";\n" +
"  :infoUrl = \"http://www.globec.org/\";\n" +
"  :institution = \"GLOBEC\";\n" +
"  :keywords = \"10um,\n" +
"Biosphere > Vegetation > Photosynthetically Active Radiation,\n" +
"Oceans > Ocean Chemistry > Ammonia,\n" +
"Oceans > Ocean Chemistry > Chlorophyll,\n" +
"Oceans > Ocean Chemistry > Nitrate,\n" +
"Oceans > Ocean Chemistry > Nitrite,\n" +
"Oceans > Ocean Chemistry > Nitrogen,\n" +
"Oceans > Ocean Chemistry > Oxygen,\n" +
"Oceans > Ocean Chemistry > Phosphate,\n" +
"Oceans > Ocean Chemistry > Pigments,\n" +
"Oceans > Ocean Chemistry > Silicate,\n" +
"Oceans > Ocean Optics > Attenuation/Transmission,\n" +
"Oceans > Ocean Temperature > Water Temperature,\n" +
"Oceans > Salinity/Density > Salinity,\n" +
"active, after, ammonia, ammonium, attenuation, biosphere, bottle, cast, chemistry, chlorophyll, chlorophyll-a, color, concentration, concentration_of_chlorophyll_in_sea_water, cruise, data, density, dissolved, dissolved nutrients, dissolved o2, fluorescence, fraction, from, globec, identifier, mass, mole, mole_concentration_of_ammonium_in_sea_water, mole_concentration_of_nitrate_in_sea_water, mole_concentration_of_nitrite_in_sea_water, mole_concentration_of_phosphate_in_sea_water, mole_concentration_of_silicate_in_sea_water, moles, moles_of_nitrate_and_nitrite_per_unit_mass_in_sea_water, n02, nep, nh4, nitrate, nitrite, nitrogen, no3, number, nutrients, o2, ocean, ocean color, oceans, optical, optical properties, optics, oxygen, passing, per, phaeopigments, phosphate, photosynthetically, pigments, plus, po4, properties, radiation, rosette, salinity, screen, sea, sea_water_salinity, sea_water_temperature, seawater, sensor, sensors, ship, silicate, temperature, time, total, transmission, transmissivity, unit, vegetation, voltage, volume, volume_fraction_of_oxygen_in_sea_water, water\";\n" +
"  :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
"  :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"  :Metadata_Conventions = \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"  :sourceUrl = \"(local files; contact erd.data@noaa.gov)\";\n" +
"  :standard_name_vocabulary = \"CF-12\";\n" +
"  :subsetVariables = \"cruise_id, ship, cast, longitude, latitude, time\";\n" +
"  :summary = \"GLOBEC (GLOBal Ocean ECosystems Dynamics) NEP (Northeast Pacific)\n" +
"Rosette Bottle Data from New Horizon Cruise (NH0207: 1-19 August 2002).\n" +
"Notes:\n" +
"Physical data processed by Jane Fleischbein (OSU).\n" +
"Chlorophyll readings done by Leah Feinberg (OSU).\n" +
"Nutrient analysis done by Burke Hales (OSU).\n" +
"Sal00 - salinity calculated from primary sensors (C0,T0).\n" +
"Sal11 - salinity calculated from secondary sensors (C1,T1).\n" +
"secondary sensor pair was used in final processing of CTD data for\n" +
"most stations because the primary had more noise and spikes. The\n" +
"primary pair were used for cast #9, 24, 48, 111 and 150 due to\n" +
"multiple spikes or offsets in the secondary pair.\n" +
"Nutrient samples were collected from most bottles; all nutrient data\n" +
"developed from samples frozen during the cruise and analyzed ashore;\n" +
"data developed by Burke Hales (OSU).\n" +
"Operation Detection Limits for Nutrient Concentrations\n" +
"Nutrient  Range         Mean    Variable         Units\n" +
"PO4       0.003-0.004   0.004   Phosphate        micromoles per liter\n" +
"N+N       0.04-0.08     0.06    Nitrate+Nitrite  micromoles per liter\n" +
"Si        0.13-0.24     0.16    Silicate         micromoles per liter\n" +
"NO2       0.003-0.004   0.003   Nitrite          micromoles per liter\n" +
"Dates and Times are UTC.\n" +
"\n" +
"For more information, see\n" +
"http://cis.whoi.edu/science/bcodmo/dataset.cfm?id=10180&flag=view\n" +
"\n" +
"Inquiries about how to access this data should be directed to\n" +
"Dr. Hal Batchelder (hbatchelder@coas.oregonstate.edu).\";\n" +
"  :time_coverage_end = \"2002-08-14T23:34:00Z\";\n" +
"  :time_coverage_start = \"2002-08-14T00:59:00Z\";\n" +
"  :title = \"GLOBEC NEP Rosette Bottle Data (2002)\";\n" +
"  :Westernmost_Easting = -125.67f; // float\n" +
" data:\n";

            expected = tHeader2 +
    "longitude =\n" +
    "  {-124.8, -124.8, -124.8, -124.8, -124.8, -124.8, -124.8, -124.8, -124.9, -124.9, -124.9, -124.9, -124.9, -124.9, -124.9, -124.9, -124.9, -125.0, -125.0, -125.0, -125.0, -125.0, -125.0, -125.0, -125.0, -125.0, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2, -125.43, -125.43, -125.43, -125.43, -125.43, -125.43, -125.43, -125.43, -125.43, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.66, -125.66, -125.66, -125.66, -125.66, -125.66, -125.66, -125.66, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.5, -125.5, -125.5, -125.5, -125.5, -125.5, -125.5, -125.5, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2}\n" +
    "NO3 =\n" +
    "  {33.66, 30.43, 28.22, 26.4, 25.63, 23.54, 22.38, 20.15, 33.55, 31.48, 24.93, -99.0, 21.21, 20.54, 17.87, -9999.0, 16.32, 33.61, 33.48, 30.7, 27.05, 25.13, 24.5, 23.95, 16.0, 14.42, 33.28, 28.3, 26.74, 24.96, 23.78, 20.76, 17.72, 16.01, 31.22, 27.47, 13.28, 10.66, 9.61, 8.36, 6.53, 2.86, 0.96, 34.05, 29.47, 18.87, 15.17, 13.84, 9.61, 4.95, 3.46, 34.09, 23.29, 16.01, 10.35, 7.72, 4.37, 2.97, 27.25, 29.98, 22.56, 9.82, 9.19, 6.57, 5.23, 3.81, 0.96, 30.08, 19.88, 8.44, 4.59, 2.67, 1.53, 0.94, 0.47, 30.73, 20.28, 10.61, 7.48, 6.53, 4.51, 3.04, 1.36, 0.89, 32.21, 23.75, 12.04, 7.67, 5.73, 1.14, 1.02, 0.46, 33.16, 27.33, 15.16, 9.7, 9.47, 8.66, 7.65, 4.84}\n" +
    "time =\n" +
    "  {1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936804E9, 1.02936804E9, 1.02936804E9, 1.02936804E9, 1.02936804E9, 1.02936804E9, 1.02936804E9, 1.02936804E9}\n" +
    "ship =\"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\"\n" +
    "}\n";
            tPo = results.indexOf(tHeader2.substring(0, 17));
            Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
            Test.ensureEqual(results.substring(tPo, tPo + tHeader2.length()), tHeader2, 
                "results=\n" + results);

            //.ncHeader
            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
                globecBottle.className() + "_Data", ".ncHeader"); 
            //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            String2.log(results);
            tResults = results.substring(0, tHeader.length());
            Test.ensureEqual(tResults, tHeader, "\nresults=\n" + results);

            expected = tHeader2 + "}\n";
            tPo = results.indexOf(expected.substring(0, 17));
            Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
            Test.ensureEqual(
                results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
                expected, "results=\n" + results);

            //test .png
            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
                globecBottle.className() + "_GraphM", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\n*** This EDDTableFromErddap test requires erdGlobecBottle on coastwatch's erddap" +
                (testLocalErddapToo? "\n    AND rGlobecBottle on localhost's erddap." : "") +
//"\n!!! This won't work till release 1.30 because of cdm_type issues." +
//"\n!!! The space at end of line issue will be fixed at next release." +
                "\nPress ^C to stop or Enter to continue..."); 
        }


    } //end of testBasic

    /** This tests making a fromErddap from a fromErddap on coastwatch. 
     * I don't completely understand: making this dataset (sourceUrl on coastwatch) 
     * works here, but fails on POST's server.
     * Making the similar dataset (sourceUrl on upwell) works on POST's server.
     * The only thing I can think of is: the server isn't allowing the redirect
     * from coastwatch to upwell for the .das (which times out) or .dds.
     * Future: pursue possible solution: serve .das and .dds from sourceUrl 
     * (don't redirect).
     */
    public static void testFromErddapFromErddap() throws Throwable {
        String2.log("\n*** testFromErddapFromErddap");
        EDDTable edd = (EDDTableFromErddap)oneFromDatasetXml("testFromErddapFromErddap"); 
        String2.log(edd.toString());
    }

    public static void testDegreesSignAttribute() throws Throwable {
        String2.log("\n*** EDDTableFromErddap.testDegreesSignAttribute");
        String url = 
            //"http://127.0.0.1:8080/cwexperimental/tabledap/erdCalcofiSur";
            "http://coastwatch.pfeg.noaa.gov/erddap/tabledap/erdCalcofiSur";
        DConnect dConnect = new DConnect(url, true, 1, 1);
        DAS das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
        String results = OpendapHelper.getDasString(das);
        //String expected = "zztop";
        //Test.ensureEqual(results, expected, "results=\n" + results);

        EDDTable edd = (EDDTableFromErddap)oneFromDatasetXml("testCalcofiSurFromErddap"); 
        String2.log(edd.toString());

    }

    /** This tests apostrophe in title appearing in graph legend. */
    public static void testApostrophe() throws Throwable {
        String2.log("\n*** EDDTableFromErddap.testApostrophe");

        EDDTable edd = (EDDTableFromErddap)oneFromDatasetXml("testWTEY"); 
        String2.log("title=" + edd.title());
        String tName = edd.makeNewFileForDapQuery(null, null, 
            "longitude,latitude,platformSpeed_kts&time%3E=2013-05-30T00:00:00Z" +
            "&time%3C=2013-06-06T00:00:00Z&.draw=markers&.marker=5|5&.color=0x000000&.colorBar=|||||",
            EDStatic.fullTestCacheDirectory, edd.className() + "_Apos", ".png"); 
        SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    }

    /** This tests dealing with remote not having ioos_category, but local requiring it. */
    public static void testTableNoIoosCat() throws Throwable {
        String2.log("\n*** EDDTableFromErddap.testTableNoIoosCat");

        //this failed because trajectory didn't have ioos_category
        EDDTable edd = (EDDTable)oneFromDatasetXml("testTableNoIoosCat"); 

    }

    /** This tests quotes in an attribute. */
    public static void testQuotes() throws Throwable {
        String2.log("\n*** EDDTableFromErddap.testQuotes");

        EDDTable edd = (EDDTableFromErddap)oneFromDatasetXml("testQuotes"); 
        String results = edd.defaultGraphQuery;
        String expected =  
//backslash was actual character in the string, now just encoding here
"longitude,latitude,time&scientific_name=\"Sardinops sagax\"&.draw=markers&.marker=5|5&.color=0x000000&.colorBar=|||||";
        Test.ensureEqual(results, expected, "\nresults=\n" + results); 
    }


    
    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {
        String2.log("\n****************** EDDTableFromErddap.test() *****************\n");
        testVerboseOn();
        
        //always done
        /* */
        testBasic(false);
        testBasic(true);
        testGenerateDatasetsXml();
        testApostrophe();
        testTableNoIoosCat();
        testQuotes();

        //not usually done

    }

}
