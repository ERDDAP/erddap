/* 
 * EDDTableFromErddap Copyright 2008, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.LongArray;
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
import com.cohort.util.XML;

/** The Java DAP classes.  */
import dods.dap.*;

import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.util.Subscriptions;
import gov.noaa.pfel.erddap.variable.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Enumeration;

/**
 * NcHelper and ucar classes only used for testing netcdf-java.
 * Get netcdfAll-......jar from ftp://ftp.unidata.ucar.edu/pub
 * and copy it to <context>/WEB-INF/lib renamed as netcdf-latest.jar.
 * Put it in the classpath for the compiler and for Java.
 */
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
//import ucar.nc2.dods.*;
import ucar.nc2.util.*;
import ucar.ma2.*;  

/** 
 * This class represents a table of data from an opendap sequence source.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2007-06-08
 */
public class EDDTableFromErddap extends EDDTable implements FromErddap { 

    protected double sourceErddapVersion = 1.22; //default = last version before /version service was added
    boolean useNccsv; //when requesting data from the remote ERDDAP

    /** Indicates if data can be transmitted in a compressed form.
     * It is unlikely anyone would want to change this. */
    public static boolean acceptDeflate = true;

    protected String publicSourceErddapUrl;
    protected boolean subscribeToRemoteErddapDataset;
    private boolean redirect = true;

    /**
     * This constructs an EDDTableFromErddap based on the information in an .xml file.
     * 
     * @param erddap if known in this context, else null
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableFromErddap"&gt; 
     *    having just been read.  
     * @return an EDDTableFromErddap.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDTableFromErddap fromXml(Erddap erddap, SimpleXMLReader xmlReader) throws Throwable {

        //data to be obtained (or not)
        if (verbose) String2.log("\n*** constructing EDDTableFromErddap(xmlReader)...");
        String tDatasetID = xmlReader.attributeValue("datasetID"); 
        int tReloadEveryNMinutes = Integer.MAX_VALUE;
        String tAccessibleTo = null;
        String tGraphsAccessibleTo = null;
        boolean tAccessibleViaFiles = EDStatic.defaultAccessibleViaFiles;
        StringArray tOnChange = new StringArray();
        boolean tSubscribeToRemoteErddapDataset = EDStatic.subscribeToRemoteErddapDataset;
        boolean tRedirect = true;
        String tFgdcFile = null;
        String tIso19115File = null;
        String tSosOfferingPrefix = null;
        String tLocalSourceUrl = null;
        String tDefaultDataQuery = null;
        String tDefaultGraphQuery = null;
        String tAddVariablesWhere = null;

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
            else if (localTags.equals( "<accessibleTo>")) {}
            else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
            else if (localTags.equals( "<graphsAccessibleTo>")) {}
            else if (localTags.equals("</graphsAccessibleTo>")) tGraphsAccessibleTo = content;
            else if (localTags.equals( "<accessibleViaFiles>")) {}
            else if (localTags.equals("</accessibleViaFiles>")) tAccessibleViaFiles = String2.parseBoolean(content); 
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
            else if (localTags.equals( "<addVariablesWhere>")) {}
            else if (localTags.equals("</addVariablesWhere>")) tAddVariablesWhere = content; 
            else if (localTags.equals( "<subscribeToRemoteErddapDataset>")) {}
            else if (localTags.equals("</subscribeToRemoteErddapDataset>")) 
                tSubscribeToRemoteErddapDataset = String2.parseBoolean(content);
            else if (localTags.equals( "<redirect>")) {}
            else if (localTags.equals("</redirect>")) 
                tRedirect = String2.parseBoolean(content);

            else xmlReader.unexpectedTagException();
        }

        return new EDDTableFromErddap(tDatasetID, 
            tAccessibleTo, tGraphsAccessibleTo, tAccessibleViaFiles, 
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
            tDefaultDataQuery, tDefaultGraphQuery, tAddVariablesWhere, 
            tReloadEveryNMinutes, 
            tLocalSourceUrl, tSubscribeToRemoteErddapDataset, tRedirect);
    }

    /**
     * The constructor.
     *
     * @param tDatasetID is a very short string identifier 
     *  (recommended: [A-Za-z][A-Za-z0-9_]* )
     *   for this dataset. See EDD.datasetID().
     * @param tAccessibleTo is a comma separated list of 0 or more
     *    roles which will have access to this dataset.
     *    <br>If null, everyone will have access to this dataset (even if not logged in).
     *    <br>If "", no one will have access to this dataset.
     * @param tOnChange 0 or more actions (starting with http://, https://, or mailto: )
     *    to be done whenever the dataset changes significantly
     * @param tFgdcFile This should be the fullname of a file with the FGDC
     *    that should be used for this dataset, or "" (to cause ERDDAP not
     *    to try to generate FGDC metadata for this dataset), or null (to allow
     *    ERDDAP to try to generate FGDC metadata for this dataset).
     * @param tIso19115File This is like tFgdcFile, but for the ISO 19119-2/19139 metadata.
     * @param tReloadEveryNMinutes indicates how often the source should
     *    be checked for new data.
     * @param tLocalSourceUrl the url to which .das or .dds or ... can be added
     * @throws Throwable if trouble
     */
    public EDDTableFromErddap(String tDatasetID, 
        String tAccessibleTo, String tGraphsAccessibleTo, boolean tAccessibleViaFiles,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tSosOfferingPrefix,
        String tDefaultDataQuery, String tDefaultGraphQuery, String tAddVariablesWhere, 
        int tReloadEveryNMinutes, 
        String tLocalSourceUrl, boolean tSubscribeToRemoteErddapDataset,
        boolean tRedirect) throws Throwable {

        if (verbose) String2.log(
            "\n*** constructing EDDTableFromErddap " + tDatasetID); 
        long constructionStartMillis = System.currentTimeMillis();
        String errorInMethod = "Error in EDDTableFromErddap(" + 
            tDatasetID + ") constructor:\n";
            
        //save some of the parameters
        className = "EDDTableFromErddap"; 
        datasetID = tDatasetID;
        setAccessibleTo(tAccessibleTo); 
        setGraphsAccessibleTo(tGraphsAccessibleTo); 
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
        publicSourceErddapUrl = convertToPublicSourceUrl(localSourceUrl);
        subscribeToRemoteErddapDataset = tSubscribeToRemoteErddapDataset;
        redirect = tRedirect;
        accessibleViaFiles = EDStatic.filesActive && tAccessibleViaFiles;
        if (accessibleViaFiles) {
            try {
                //this will only work if remote ERDDAP is v2.10+
                int po = localSourceUrl.indexOf("/tabledap/");
                Test.ensureTrue(po > 0, "localSourceUrl doesn't have /tabledap/.");
                InputStream is = SSR.getUrlBufferedInputStream(
                    String2.replaceAll(localSourceUrl, "/tabledap/", "/files/") + 
                    "/.csv");
                try {is.close();} catch (Exception e2) {}
            } catch (Exception e) {
                String2.log("accessibleViaFiles=false because remote ERDDAP dataset isn't accessible via /files/ (or is <v2.10 so no support for /files/.csv):\n" +
                    MustBe.throwableToString(e));
                accessibleViaFiles = false;
            }
        }

        //erddap support all constraints:
        sourceNeedsExpandedFP_EQ = false;
        sourceCanConstrainNumericData = CONSTRAIN_YES;
        sourceCanConstrainStringData  = CONSTRAIN_YES;
        sourceCanConstrainStringRegex = PrimitiveArray.REGEX_OP;

        //try quickRestart?
        Table sourceTable = new Table(); 
        sourceGlobalAttributes = sourceTable.globalAttributes();
        boolean qrMode = EDStatic.quickRestart && 
            EDStatic.initialLoadDatasets() && 
            File2.isFile(quickRestartFullFileName()); //goofy: name is .nc but contents are NCCSV
        if (qrMode) {
            //try to do quick initialLoadDatasets()
            //If this fails anytime during construction, the dataset will be loaded 
            //  during the next major loadDatasets,
            //  which is good because it allows quick loading of other datasets to continue.
            //This will fail (good) if dataset has changed significantly and
            //  quickRestart file has outdated information.

            if (verbose)
                String2.log("  using info from quickRestartFile");

            //starting with 1.76, use nccsv for quick restart info
            sourceTable.readNccsv(quickRestartFullFileName(), false); //goofy: name is .nc but contents are NCCSV

            //set creationTimeMillis to time of previous creation, so next time
            //to be reloaded will be same as if ERDDAP hadn't been restarted.
            creationTimeMillis = sourceGlobalAttributes.getLong("creationTimeMillis");
            sourceGlobalAttributes.remove("creationTimeMillis");

            sourceErddapVersion = sourceGlobalAttributes.getDouble("sourceErddapVersion");
            sourceGlobalAttributes.remove("sourceErddapVersion");
            if (Double.isNaN(sourceErddapVersion))
                sourceErddapVersion = 1.22;
            useNccsv = intSourceErddapVersion() >= 176;

        } else {
            // !qrMode

            sourceErddapVersion = getRemoteErddapVersion(localSourceUrl);

            //For version 1.76+, this uses .nccsv to communicate
            //For version 1.75-, this uses DAP 
            useNccsv = intSourceErddapVersion() >= 176;

            if (useNccsv) {
                //get sourceTable from remote ERDDAP nccsv
                if (verbose)
                    String2.log("  using info from remote dataset's .nccsvMetadata");

                sourceTable.readNccsv(localSourceUrl + ".nccsvMetadata", false); //readData?

            } else { //if !useNccsv
                //get sourceTable from remote DAP
                if (verbose)
                    String2.log("  using info from remote dataset's DAP services");

                DAS das = new DAS();
                das.parse(new ByteArrayInputStream(SSR.getUrlResponseBytes(
                    localSourceUrl + ".das"))); //has timeout and descriptive error 
                DDS dds = new DDS();
                dds.parse(new ByteArrayInputStream(SSR.getUrlResponseBytes(
                    localSourceUrl + ".dds"))); //has timeout and descriptive error 

                //get global attributes
                OpendapHelper.getAttributes(das, "GLOBAL", sourceGlobalAttributes);

                //delve into the outerSequence 
                BaseType outerVariable = (BaseType)dds.getVariable(SEQUENCE_NAME);
                if (!(outerVariable instanceof DSequence)) 
                    throw new IllegalArgumentException(errorInMethod + "outerVariable not a DSequence: name=" + 
                        outerVariable.getName() + " type=" + outerVariable.getTypeName());
                DSequence outerSequence = (DSequence)outerVariable;
                int nOuterColumns = outerSequence.elementCount();
                AttributeTable outerAttributeTable = das.getAttributeTable(SEQUENCE_NAME);
                for (int outerCol = 0; outerCol < nOuterColumns; outerCol++) {

                    //look at the variables in the outer sequence
                    BaseType obt = (BaseType)outerSequence.getVar(outerCol);
                    String tSourceName = obt.getName();

                    //get the data sourcePAType
                    PAType tSourcePAType = OpendapHelper.getElementPAType(obt.newPrimitiveVector());

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

                    sourceTable.addColumn(outerCol, tSourceName, 
                        PrimitiveArray.factory(tSourcePAType, 8, false), tSourceAtt);
                }
            }
        }

        combinedGlobalAttributes = new Attributes(addGlobalAttributes, sourceGlobalAttributes); //order is important
        combinedGlobalAttributes.removeValue("\"null\"");

        //make the dataVariables
        ArrayList<EDV> tDataVariables = new ArrayList();
        for (int col = 0; col < sourceTable.nColumns(); col++) {

            String     tSourceName = sourceTable.getColumnName(col);
            Attributes tSourceAtt  = sourceTable.columnAttributes(col);
            String     tSourceType = sourceTable.getColumn(col).elementTypeString();

            //deal with remote not having ioos_category, but this ERDDAP requiring it
            Attributes tAddAtt = new Attributes();
            if (EDStatic.variablesMustHaveIoosCategory &&
                tSourceAtt.getString("ioos_category") == null) {

                //guess ioos_category   (alternative is always assign "Unknown")
                Attributes tAtts = EDD.makeReadyToUseAddVariableAttributesForDatasetsXml(
                    sourceGlobalAttributes, tSourceAtt, null, tSourceName, 
                    false, //tryToAddStandardName since just getting ioos_category
                    false, false); //tryToAddColorBarMinMax, tryToFindLLAT
                //if put it in tSourceAtt, it will be available for quick restart 
                tSourceAtt.add("ioos_category", tAtts.getString("ioos_category"));
            }

            //make the variable
            EDV edv = null;
            if (EDV.LON_NAME.equals(tSourceName)) {
                lonIndex = tDataVariables.size();
                edv = new EDVLon(datasetID, tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType, PAOne.fromDouble(Double.NaN), PAOne.fromDouble(Double.NaN)); 
            } else if (EDV.LAT_NAME.equals(tSourceName)) {
                latIndex = tDataVariables.size();
                edv = new EDVLat(datasetID, tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType, PAOne.fromDouble(Double.NaN), PAOne.fromDouble(Double.NaN)); 
            } else if (EDV.ALT_NAME.equals(tSourceName)) {
                altIndex = tDataVariables.size();
                edv = new EDVAlt(datasetID, tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType, PAOne.fromDouble(Double.NaN), PAOne.fromDouble(Double.NaN));
            } else if (EDV.DEPTH_NAME.equals(tSourceName)) {
                depthIndex = tDataVariables.size();
                edv = new EDVDepth(datasetID, tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType, PAOne.fromDouble(Double.NaN), PAOne.fromDouble(Double.NaN)); 
            } else if (EDV.TIME_NAME.equals(tSourceName)) {  //look for TIME_NAME before check hasTimeUnits (next)
                timeIndex = tDataVariables.size();
                edv = new EDVTime(datasetID, tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType);//this constructor gets source / sets destination actual_range
            } else if (EDVTimeStamp.hasTimeUnits(tSourceAtt, tAddAtt)) {
                edv = new EDVTimeStamp(datasetID, tSourceName, tSourceName, 
                    tSourceAtt, tAddAtt,
                    tSourceType); //this constructor gets source / sets destination actual_range
            } else {
                edv = new EDV(datasetID, tSourceName, tSourceName, 
                    tSourceAtt, tAddAtt,
                    tSourceType); //the constructor that reads actual_range
                edv.setActualRangeFromDestinationMinMax();
            }
            tDataVariables.add(edv); 

        }
        dataVariables = new EDV[tDataVariables.size()];
        for (int dv = 0; dv < tDataVariables.size(); dv++)
            dataVariables[dv] = tDataVariables.get(dv);

        //make addVariablesWhereAttNames and addVariablesWhereAttValues
        makeAddVariablesWhereAttNamesAndValues(tAddVariablesWhere);

        //ensure the setup is valid
        ensureValid(); //this ensures many things are set, e.g., sourceUrl

        //save quickRestart info
        if (!qrMode) { //i.e., there is new info
            try {
                File2.makeDirectory(File2.getDirectory(quickRestartFullFileName())); //goofy: name is .nc but contents are NCCSV
                sourceGlobalAttributes.set("creationTimeMillis", "" + creationTimeMillis);
                sourceGlobalAttributes.set("sourceErddapVersion", sourceErddapVersion);
                sourceTable.saveAsNccsvFile(false, true, 0, Integer.MAX_VALUE, quickRestartFullFileName()); //goofy: name is .nc but contents are NCCSV
            } catch (Throwable t) {
                String2.log(MustBe.throwableToString(t));
            }
        }

        //try to subscribe to the remote ERDDAP dataset
        tryToSubscribeToRemoteErddapDataset(subscribeToRemoteErddapDataset, localSourceUrl);

        //finally
        long cTime = System.currentTimeMillis() - constructionStartMillis;
        if (verbose) String2.log(
            (debugMode? "\n" + toString() : "") +
            "\n*** EDDTableFromErddap " + datasetID + " constructor finished. TIME=" + 
            cTime + "ms" + (cTime >= 10000? "  (>10s!)" : "") + "\n"); 

    }

    /** This returns the source ERDDAP's version number, e.g., 1.22 */
    public double sourceErddapVersion() {return sourceErddapVersion;}
    public int intSourceErddapVersion() {return Math2.roundToInt(sourceErddapVersion * 100);}

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
     * This indicates whether user requests should be redirected.
     */
    public boolean redirect() {
        return redirect;
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
        Table table = new Table();
        String udq = String2.isSomething(userDapQuery)? "?" + userDapQuery : "";

        if (useNccsv) {
            //FUTURE: could repeatedly: read part/ write part
            table.readNccsv(localSourceUrl + ".nccsv" + udq, true); // readData?

        } else {
            //Very unfortunate: JDAP reads all rows when it deserializes 
            //(see java docs for DSequence)
            //(that's why it can return getRowCount)
            //so there is no real way to read an opendapSequence in chunks (or row by row).
            //I can't split into subsets because I don't know which variable 
            //  to constrain or how to constrain it (it would change with different
            //  userDapQuery's).
            //I could write my own procedure to read DSequence (eek!).
            table.readOpendapSequence(localSourceUrl + udq, false);
        }

        //String2.log(table.toString());
        standardizeResultsTable(requestUrl, userDapQuery, table); //not necessary?
        tableWriter.writeAllAndFinish(table);
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
        //almost identical code in EDDGridFromFiles and EDDTableFromFiles ("grid" vs "table")
        if (!accessibleViaFiles)
            return null;
        try {

            //get the .csv table from remote fromErddap dataset
            String url = String2.replaceAll(localSourceUrl, "/tabledap/", "/files/") + "/" + nextPath + ".csv";
            BufferedReader reader = SSR.getBufferedUrlReader(url);
            Table table = new Table();
            table.readASCII(url, reader, "", "", 0, 1, ",", 
                null, null, null, null, false); //testColumns[], testMin[], testMax[], loadColumns[], simplify)
            String colNames = table.getColumnNamesCSVString();
            Test.ensureEqual(colNames, "Name,Last modified,Size,Description", "");
            table.setColumn(1, new LongArray(table.getColumn(1)));
            table.setColumn(2, new LongArray(table.getColumn(2)));

            //separate out the subdirs
            StringArray subdirs = new StringArray();
            BitSet keep = new BitSet();  //all false
            int nRows = table.nRows();
            StringArray names = (StringArray)table.getColumn(0);
            for (int row = 0; row < nRows; row++) {
                String name = names.get(row);
                if (name.endsWith("/")) {
                    subdirs.add(name.substring(0, name.length() - 1));
                } else {
                    keep.set(row);
                }
            }
            table.justKeep(keep);
            return new Object[]{table, subdirs.toStringArray(), null}; //not a local dir

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
        //almost identical code in EDDGridFromFiles and EDDTableFromFiles ("grid" vs "table")
        if (!accessibleViaFiles)
             return null;
        return String2.replaceAll(publicSourceErddapUrl, "/tabledap/", "/files/") + "/" + relativeFileName;
    }


    /** 
     * This generates datasets.xml entries for all EDDTable from a remote ERDDAP.
     * The XML can then be edited by hand and added to the datasets.xml file.
     *
     * @param tLocalSourceUrl the base url for the dataset, e.g., 
     *   "https://coastwatch.pfeg.noaa.gov/erddap".
     *   This is a localSourceUrl since it has to be accessible, but usually it is also a publicSourceUrl.
     * @param keepOriginalDatasetIDs
     * @return a suggested chunk of xml for this dataset for use in datasets.xml 
     * @throws Throwable if trouble, e.g., if no Grid or Array variables are found.
     *    If no trouble, then a valid dataset.xml chunk has been returned.
     */
    public static String generateDatasetsXml(String tLocalSourceUrl, boolean keepOriginalDatasetIDs) 
        throws Throwable {

        tLocalSourceUrl = EDStatic.updateUrls(tLocalSourceUrl); //http: to https:
        String2.log("\n*** EDDTableFromErddap.generateDatasetsXml" +
            "\ntLocalSourceUrl=" + tLocalSourceUrl + 
            " keepOriginalDatasetIDs=" + keepOriginalDatasetIDs);

        //make the StringBuilder to hold the results and add documentation
        StringBuilder sb = new StringBuilder();
/*        sb.append(  //there is very similar text in EDDGridFromErddap
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
"   https://coastwatch.pfeg.noaa.gov/erddap/download/setupDatasetsXml.html#EDDTableFromErddap .\n" +
"   If you want to alter a dataset's metadata or make other changes to a dataset,\n" +
"   use EDDTableFromDapSequence to access the dataset instead of EDDTableFromErddap.\n" +
" * If the remote ERDDAP is version 1.12 or below, this will generate incorrect, useless results.\n" +
"-->\n");
*/
        //get the tabledap datasets in a json table
        String jsonUrl = tLocalSourceUrl + "/tabledap/index.json";
        String sourceInfo = SSR.getUrlResponseStringUnchanged(jsonUrl);
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
            sourceInfo = SSR.getUrlResponseStringUnchanged(jsonUrl);
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
        int po;

        //test local generateDatasetsXml.  In tests, always use non-https url.
        String results = generateDatasetsXml(EDStatic.erddapUrl, true) + "\n"; 
        String2.log("results=\n" + results);

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDTableFromErddap",
            EDStatic.erddapUrl,
            "true", "-1"},  //keep original names?, defaultStandardizeWhat
          false); //doIt loop?
        Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

String expected = 
"<dataset type=\"EDDTableFromErddap\" datasetID=\"erdGlobecBottle\" active=\"true\">\n" +
"    <!-- GLOBEC NEP Rosette Bottle Data (2002) -->\n" +
"    <sourceUrl>http://localhost:8080/cwexperimental/tabledap/erdGlobecBottle</sourceUrl>\n" +
"</dataset>\n";
String fragment = expected;
            String2.log("\nresults=\n" + results);
            po = results.indexOf(expected.substring(0, 70));
        try {
            Test.ensureEqual(results.substring(po, po + expected.length()), expected, "");
        } catch (Throwable t) {
            throw new RuntimeException("Unexpected error. This test requires erdGlobecBottle in localhost ERDDAP.", t);  
        }

expected = 
"<!-- Of the datasets above, the following datasets are EDDTableFromErddap's at the remote ERDDAP.\n";
            po = results.indexOf(expected.substring(0, 20));
            Test.ensureEqual(results.substring(po, po + expected.length()), expected, "results=\n" + results);
        try {
            Test.ensureTrue(results.indexOf("rGlobecBottle", po) > 0, "results=\n" + results);  
        } catch (Throwable t) {
            throw new RuntimeException("Unexpected error. This test requires rGlobecBottle in localhost ERDDAP.", t);  
        }


        /*
        //ensure it is ready-to-use by making a dataset from it       
        //NO - don't mess with existing erdGlobecBottle
        String tDatasetID = "erdGlobecBottle";
        EDD.deleteCachedDatasetInfo(tDatasetID);
        EDD edd = oneFromXmlFragment(null, fragment);
        Test.ensureEqual(edd.title(), "GLOBEC NEP Rosette Bottle Data (2002)", "");
        Test.ensureEqual(edd.datasetID(), tDatasetID, "");
        Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
            "cruise_id, ship, cast, longitude, latitude, time, bottle_posn, chl_a_total, chl_a_10um, phaeo_total, phaeo_10um, sal00, sal11, temperature0, temperature1, fluor_v, xmiss_v, PO4, N_N, NO3, Si, NO2, NH4, oxygen, par", 
            "");
        */
    }


    /**
     * The basic tests of this class (erdGlobecBottle).
     */
    public static void testBasic(boolean tRedirect) throws Throwable {
        String2.log("\n****************** EDDTableFromErddap.testBasic(" + 
            tRedirect + ")\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        int tPo;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10); //just 10 till 1.40 released, then 14
        String mapDapQuery = "status,testLong,sst&.draw=markers";
        String dir = EDStatic.fullTestCacheDirectory;
        String tID = tRedirect? "rTestNccsvScalar" : "rTestNccsvScalarNoRedirect";
        String url = "http://localhost:8080/cwexperimental/tabledap/" + tID;



        //*** test getting das for entire dataset
        results = SSR.getUrlResponseStringUnchanged(url + ".nccsvMetadata"); 
        expected = 
"*GLOBAL*,Conventions,\"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.1\"\n" +
"*GLOBAL*,cdm_data_type,Trajectory\n" +
"*GLOBAL*,cdm_trajectory_variables,ship\n" +
"*GLOBAL*,creator_email,bob.simons@noaa.gov\n" +
"*GLOBAL*,creator_name,Bob Simons\n" +
"*GLOBAL*,creator_type,person\n" +
"*GLOBAL*,creator_url,https://www.pfeg.noaa.gov\n" +
"*GLOBAL*,Easternmost_Easting,-130.2576d\n" +
"*GLOBAL*,featureType,Trajectory\n" +
"*GLOBAL*,geospatial_lat_max,28.0003d\n" +
"*GLOBAL*,geospatial_lat_min,27.9998d\n" +
"*GLOBAL*,geospatial_lat_units,degrees_north\n" +
"*GLOBAL*,geospatial_lon_max,-130.2576d\n" +
"*GLOBAL*,geospatial_lon_min,-132.1591d\n" +
"*GLOBAL*,geospatial_lon_units,degrees_east\n" +
"*GLOBAL*,infoUrl,https://coastwatch.pfeg.noaa.gov/erddap/download/NCCSV.html\n" +
"*GLOBAL*,institution,\"NOAA NMFS SWFSC ERD, NOAA PMEL\"\n" +
"*GLOBAL*,keywords,\"center, data, demonstration, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, environmental, erd, fisheries, identifier, laboratory, latitude, long, longitude, marine, national, nccsv, nmfs, noaa, ocean, oceans, pacific, pmel, science, sea, sea_surface_temperature, service, ship, southwest, sst, status, surface, swfsc, temperature, test, testLong, time, trajectory\"\n" +
"*GLOBAL*,keywords_vocabulary,GCMD Science Keywords\n" +
"*GLOBAL*,license,\"\"\"NCCSV Demonstration\"\" by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .\"\n" +
"*GLOBAL*,Northernmost_Northing,28.0003d\n" +
"*GLOBAL*,sourceUrl,(local files)\n" +
"*GLOBAL*,Southernmost_Northing,27.9998d\n" +
"*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v55\n" +
"*GLOBAL*,subsetVariables,\"ship, status, testLong\"\n" +
"*GLOBAL*,summary,This is a paragraph or two describing the dataset.\n" +
"*GLOBAL*,time_coverage_end,2017-03-23T23:45:00Z\n" +
"*GLOBAL*,time_coverage_start,2017-03-23T00:45:00Z\n" +
"*GLOBAL*,title,NCCSV Demonstration\n" +
"*GLOBAL*,Westernmost_Easting,-132.1591d\n" +
"ship,*DATA_TYPE*,String\n" +
"ship,cf_role,trajectory_id\n" +
"ship,ioos_category,Identifier\n" +
"ship,long_name,Ship\n" +
"time,*DATA_TYPE*,String\n" +
"time,_CoordinateAxisType,Time\n" +
"time,actual_range,2017-03-23T00:45:00Z\\n2017-03-23T23:45:00Z\n" +
"time,axis,T\n" +
"time,ioos_category,Time\n" +
"time,long_name,Time\n" +
"time,standard_name,time\n" +
"time,time_origin,01-JAN-1970 00:00:00\n" +
"time,units,yyyy-MM-dd'T'HH:mm:ssZ\n" +
"latitude,*DATA_TYPE*,double\n" +
"latitude,_CoordinateAxisType,Lat\n" +
"latitude,actual_range,27.9998d,28.0003d\n" +
"latitude,axis,Y\n" +
"latitude,colorBarMaximum,90.0d\n" +
"latitude,colorBarMinimum,-90.0d\n" +
"latitude,ioos_category,Location\n" +
"latitude,long_name,Latitude\n" +
"latitude,standard_name,latitude\n" +
"latitude,units,degrees_north\n" +
"longitude,*DATA_TYPE*,double\n" +
"longitude,_CoordinateAxisType,Lon\n" +
"longitude,actual_range,-132.1591d,-130.2576d\n" +
"longitude,axis,X\n" +
"longitude,colorBarMaximum,180.0d\n" +
"longitude,colorBarMinimum,-180.0d\n" +
"longitude,ioos_category,Location\n" +
"longitude,long_name,Longitude\n" +
"longitude,standard_name,longitude\n" +
"longitude,units,degrees_east\n" +
"status,*DATA_TYPE*,char\n" +
"status,actual_range,\"'\\t'\",\"'\\u20ac'\"\n" +
"status,comment,\"From http://some.url.gov/someProjectDocument , Table C\"\n" +
"status,ioos_category,Unknown\n" +
"status,long_name,Status\n" +
"testByte,*DATA_TYPE*,byte\n" +
"testByte,_FillValue,127b\n" +
"testByte,actual_range,-128b,126b\n" +
"testByte,ioos_category,Unknown\n" +
"testByte,units,\"1\"\n" +
"testUByte,*DATA_TYPE*,ubyte\n" +
"testUByte,_FillValue,255ub\n" +
"testUByte,actual_range,0ub,254ub\n" +
"testUByte,ioos_category,Unknown\n" +
"testUByte,units,\"1\"\n" +
"testLong,*DATA_TYPE*,long\n" +
"testLong,_FillValue,9223372036854775807L\n" +
"testLong,actual_range,-9223372036854775808L,9223372036854775806L\n" + 
"testLong,ioos_category,Unknown\n" +
"testLong,long_name,Test of Longs\n" +
"testLong,units,\"1\"\n" +
"testULong,*DATA_TYPE*,ulong\n" +
"testULong,_FillValue,18446744073709551615uL\n" +
"testULong,actual_range,0uL,18446744073709551614uL\n" +
"testULong,ioos_category,Unknown\n" +
"testULong,long_name,Test ULong\n" +
"testULong,units,\"1\"\n" +
"sst,*DATA_TYPE*,float\n" +
"sst,actual_range,10.0f,10.9f\n" +
"sst,colorBarMaximum,32.0d\n" +
"sst,colorBarMinimum,0.0d\n" +
"sst,ioos_category,Temperature\n" +
"sst,long_name,Sea Surface Temperature\n" +
"sst,missing_value,99.0f\n" +
"sst,standard_name,sea_surface_temperature\n" +
"sst,testBytes,-128b,0b,127b\n" +
"sst,testChars,\"','\",\"'\"\"'\",\"'\\u20ac'\"\n" +
"sst,testDoubles,-1.7976931348623157E308d,0.0d,1.7976931348623157E308d\n" +
"sst,testFloats,-3.4028235E38f,0.0f,3.4028235E38f\n" +
"sst,testInts,-2147483648i,0i,2147483647i\n" +
"sst,testLongs,-9223372036854775808L,-9007199254740992L,9007199254740992L,9223372036854775806L,9223372036854775807L\n" +
"sst,testShorts,-32768s,0s,32767s\n" +
"sst,testStrings,\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\"\n" +
"sst,testUBytes,0ub,127ub,255ub\n" +
"sst,testUInts,0ui,2147483647ui,4294967295ui\n" +
"sst,testULongs,0uL,9223372036854775807uL,18446744073709551615uL\n" +
"sst,testUShorts,0us,32767us,65535us\n" +
"sst,units,degree_C\n" +
"\n" +
"*END_METADATA*\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.nccsv all
        userDapQuery = "";
        results = SSR.getUrlResponseStringUnchanged(url + ".nccsv"); 
        //String2.log(results);
        expected = 
"*GLOBAL*,Conventions,\"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.1\"\n" +
"*GLOBAL*,cdm_data_type,Trajectory\n" +
"*GLOBAL*,cdm_trajectory_variables,ship\n" +
"*GLOBAL*,creator_email,bob.simons@noaa.gov\n" +
"*GLOBAL*,creator_name,Bob Simons\n" +
"*GLOBAL*,creator_type,person\n" +
"*GLOBAL*,creator_url,https://www.pfeg.noaa.gov\n" +
"*GLOBAL*,Easternmost_Easting,-130.2576d\n" +
"*GLOBAL*,featureType,Trajectory\n" +
"*GLOBAL*,geospatial_lat_max,28.0003d\n" +
"*GLOBAL*,geospatial_lat_min,27.9998d\n" +
"*GLOBAL*,geospatial_lat_units,degrees_north\n" +
"*GLOBAL*,geospatial_lon_max,-130.2576d\n" +
"*GLOBAL*,geospatial_lon_min,-132.1591d\n" +
"*GLOBAL*,geospatial_lon_units,degrees_east\n" +
"*GLOBAL*,history," + today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

expected =        
//T17:35:08Z (local files)\\n2017-04-18T17:35:08Z  
"http://localhost:8080/cwexperimental/tabledap/" + 
    (tRedirect? "testNccsvScalar" : tID) + 
    ".nccsv\n" +
"*GLOBAL*,infoUrl,https://coastwatch.pfeg.noaa.gov/erddap/download/NCCSV.html\n" +
"*GLOBAL*,institution,\"NOAA NMFS SWFSC ERD, NOAA PMEL\"\n" +
"*GLOBAL*,keywords,\"center, data, demonstration, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, environmental, erd, fisheries, identifier, laboratory, latitude, long, longitude, marine, national, nccsv, nmfs, noaa, ocean, oceans, pacific, pmel, science, sea, sea_surface_temperature, service, ship, southwest, sst, status, surface, swfsc, temperature, test, testLong, time, trajectory\"\n" +
"*GLOBAL*,keywords_vocabulary,GCMD Science Keywords\n" +
"*GLOBAL*,license,\"\"\"NCCSV Demonstration\"\" by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .\"\n" +
"*GLOBAL*,Northernmost_Northing,28.0003d\n" +
"*GLOBAL*,sourceUrl,(local files)\n" +
"*GLOBAL*,Southernmost_Northing,27.9998d\n" +
"*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v55\n" +
"*GLOBAL*,subsetVariables,\"ship, status, testLong\"\n" +
"*GLOBAL*,summary,This is a paragraph or two describing the dataset.\n" +
"*GLOBAL*,time_coverage_end,2017-03-23T23:45:00Z\n" +
"*GLOBAL*,time_coverage_start,2017-03-23T00:45:00Z\n" +
"*GLOBAL*,title,NCCSV Demonstration\n" +
"*GLOBAL*,Westernmost_Easting,-132.1591d\n" +
"ship,*DATA_TYPE*,String\n" +
"ship,cf_role,trajectory_id\n" +
"ship,ioos_category,Identifier\n" +
"ship,long_name,Ship\n" +
"time,*DATA_TYPE*,String\n" +
"time,_CoordinateAxisType,Time\n" +
"time,axis,T\n" +
"time,ioos_category,Time\n" +
"time,long_name,Time\n" +
"time,standard_name,time\n" +
"time,time_origin,01-JAN-1970 00:00:00\n" +
"time,units,yyyy-MM-dd'T'HH:mm:ssZ\n" +
"latitude,*DATA_TYPE*,double\n" +
"latitude,_CoordinateAxisType,Lat\n" +
"latitude,axis,Y\n" +
"latitude,colorBarMaximum,90.0d\n" +
"latitude,colorBarMinimum,-90.0d\n" +
"latitude,ioos_category,Location\n" +
"latitude,long_name,Latitude\n" +
"latitude,standard_name,latitude\n" +
"latitude,units,degrees_north\n" +
"longitude,*DATA_TYPE*,double\n" +
"longitude,_CoordinateAxisType,Lon\n" +
"longitude,axis,X\n" +
"longitude,colorBarMaximum,180.0d\n" +
"longitude,colorBarMinimum,-180.0d\n" +
"longitude,ioos_category,Location\n" +
"longitude,long_name,Longitude\n" +
"longitude,standard_name,longitude\n" +
"longitude,units,degrees_east\n" +
"status,*DATA_TYPE*,char\n" +
"status,comment,\"From http://some.url.gov/someProjectDocument , Table C\"\n" +
"status,ioos_category,Unknown\n" +
"status,long_name,Status\n" +
"testByte,*DATA_TYPE*,byte\n" +
"testByte,_FillValue,127b\n" +
"testByte,ioos_category,Unknown\n" +
"testByte,units,\"1\"\n" +
"testUByte,*DATA_TYPE*,ubyte\n" +
"testUByte,_FillValue,255ub\n" +
"testUByte,ioos_category,Unknown\n" +
"testUByte,units,\"1\"\n" +
"testLong,*DATA_TYPE*,long\n" +
"testLong,_FillValue,9223372036854775807L\n" +
"testLong,ioos_category,Unknown\n" +
"testLong,long_name,Test of Longs\n" +
"testLong,units,\"1\"\n" +
"testULong,*DATA_TYPE*,ulong\n" +
"testULong,_FillValue,18446744073709551615uL\n" +
"testULong,ioos_category,Unknown\n" +
"testULong,long_name,Test ULong\n" +
"testULong,units,\"1\"\n" +
"sst,*DATA_TYPE*,float\n" +
"sst,colorBarMaximum,32.0d\n" +
"sst,colorBarMinimum,0.0d\n" +
"sst,ioos_category,Temperature\n" +
"sst,long_name,Sea Surface Temperature\n" +
"sst,missing_value,99.0f\n" +
"sst,standard_name,sea_surface_temperature\n" +
"sst,testBytes,-128b,0b,127b\n" +
"sst,testChars,\"','\",\"'\"\"'\",\"'\\u20ac'\"\n" +
"sst,testDoubles,-1.7976931348623157E308d,0.0d,1.7976931348623157E308d\n" +
"sst,testFloats,-3.4028235E38f,0.0f,3.4028235E38f\n" +
"sst,testInts,-2147483648i,0i,2147483647i\n" +
"sst,testLongs,-9223372036854775808L,-9007199254740992L,9007199254740992L,9223372036854775806L,9223372036854775807L\n" +
"sst,testShorts,-32768s,0s,32767s\n" +
"sst,testStrings,\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\"\n" +
"sst,testUBytes,0ub,127ub,255ub\n" +
"sst,testUInts,0ui,2147483647ui,4294967295ui\n" +
"sst,testULongs,0uL,9223372036854775807uL,18446744073709551615uL\n" +
"sst,testUShorts,0us,32767us,65535us\n" +
"sst,units,degree_C\n" +
"\n" +
"*END_METADATA*\n" +
"ship,time,latitude,longitude,status,testByte,testUByte,testLong,testULong,sst\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T00:45:00Z,28.0002,-130.2576,A,-128,0,-9223372036854775808L,0uL,10.9\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T01:45:00Z,28.0003,-130.3472,\\u20ac,0,127,-9007199254740992L,9223372036854775807uL,10.0\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T02:45:00Z,28.0001,-130.4305,\\t,126,254,9223372036854775806L,18446744073709551614uL,99.0\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T12:45:00Z,27.9998,-131.5578,\"\"\"\",,,,,\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T21:45:00Z,28.0003,-132.0014,\\u00fc,,,,,\n" +
"\" a\\t~\\u00fc,\\n'z\"\"\\u20ac\",2017-03-23T23:45:00Z,28.0002,-132.1591,?,,,,,\n" +
"*END_DATA*\n";
        tPo = results.indexOf(expected.substring(0, 40));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);

            //test .png
            tName = "EDDTableFromErddap_GraphM_" + tRedirect + ".png"; 
            SSR.downloadFile(url + ".png?" + mapDapQuery, dir + tName, true);
            SSR.displayInBrowser("file://" + dir + tName);

    } //end of testBasic

    /** 
     * This tests making a fromErddap from a fromErddap on coastwatch. 
     */
    public static void testFromErddapFromErddap() throws Throwable {
        String2.log("\n*** EDDTableFromErddap.testFromErddapFromErddap");
        EDDTable edd = (EDDTableFromErddap)oneFromDatasetsXml(null, "testFromErddapFromErddap"); 
        String2.log(edd.toString());
    }

    public static void testDegreesSignAttribute() throws Throwable {
        String2.log("\n*** EDDTableFromErddap.testDegreesSignAttribute");
        String url = 
            //"http://localhost:8080/cwexperimental/tabledap/erdCalcofiSur";
            "https://coastwatch.pfeg.noaa.gov/erddap/tabledap/erdCalcofiSur";
        DConnect dConnect = new DConnect(url, true, 1, 1);
        DAS das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
        String results = OpendapHelper.getDasString(das);
        //String expected = "zztop";
        //Test.ensureEqual(results, expected, "results=\n" + results);

        EDDTable edd = (EDDTableFromErddap)oneFromDatasetsXml(null, "testCalcofiSurFromErddap"); 
        String2.log(edd.toString());

    }


    /** This tests dealing with remote not having ioos_category, but local requiring it. */
    public static void testTableNoIoosCat() throws Throwable {
        String2.log("\n*** EDDTableFromErddap.testTableNoIoosCat");

        //this failed because trajectory didn't have ioos_category
        //EDDTable edd = (EDDTable)oneFromDatasetsXml(null, "testTableNoIoosCat"); 
        String url = "http://localhost:8080/cwexperimental/tabledap/testTableNoIoosCat";
        String results, expected;
        String query = "?&time%3E=2008-12-10T19%3A41%3A00Z"; //"?&time>2008-12-10T19:41:00Z";


        //*** test getting csv
        results = SSR.getUrlResponseStringUnchanged(url + ".csv" + query); 
        expected = 
"trajectory,time,depth,latitude,longitude,temperature,conductivity,salinity,density,pressure\n" +
",UTC,m,degrees_north,degrees_east,Celsius,S m-1,1e-3,kg m-3,dbar\n" +
"sg114_3,2008-12-10T19:41:02Z,-7.02,21.238798,-157.86617,25.356133,5.337507,34.952133,1023.1982,7.065868\n" +
"sg114_3,2008-12-10T19:41:08Z,-6.39,21.238808,-157.86618,25.353163,5.337024,34.951065,1023.1983,6.4317517\n" +
"sg114_3,2008-12-10T19:41:14Z,-5.7,21.238813,-157.86618,25.352034,5.337048,34.95233,1023.1996,5.737243\n" +
"sg114_3,2008-12-10T19:41:19Z,-5.04,21.238823,-157.8662,25.354284,5.336977,34.950283,1023.1973,5.072931\n" +
"sg114_3,2008-12-10T19:41:25Z,-4.24,21.238829,-157.86621,25.353346,5.337251,34.95328,1023.1999,4.2677035\n" +
"sg114_3,2008-12-10T19:41:30Z,-3.55,21.238836,-157.86621,25.353527,5.3372197,34.953125,1023.1997,3.5731952\n" +
"sg114_3,2008-12-10T19:41:36Z,-2.65,21.238846,-157.86623,25.351152,5.336866,34.952633,1023.2001,2.6673148\n" +
"sg114_3,2008-12-10T19:41:42Z,-1.83,21.238852,-157.86624,25.355568,5.3372297,34.95217,1023.19836,1.841957\n" +
"sg114_3,2008-12-10T19:41:47Z,-1.16,21.23886,-157.86624,25.352736,5.3364573,34.948875,1023.1968,1.1675793\n" +
"sg114_3,2008-12-10T19:41:53Z,-0.8,21.238855,-157.86624,25.330637,5.30179,34.71056,1023.02356,0.8052271\n" +
"sg114_3,2008-12-10T19:41:59Z,-0.75,21.238853,-157.86624,25.2926,2.8720038,17.5902,1010.1601,0.7549004\n" +
"sg114_3,2008-12-10T19:42:04Z,-0.72,21.23885,-157.86623,25.25033,3.0869908,19.06109,1011.27466,0.7247044\n" +
"sg114_3,2008-12-10T19:42:10Z,-0.7,21.238853,-157.86624,25.225939,-3.494945,21.86908,1013.3882,0.70457375\n";
        String2.log(results);
        Test.ensureEqual(results, expected, "");

        //*** test getting jsonlCSV when (until they update) they don't offer it
        results = SSR.getUrlResponseStringUnchanged(url + ".jsonlCSV" + query); 
        expected = 
"[\"sg114_3\", \"2008-12-10T19:41:02Z\", -7.02, 21.238798, -157.86617, 25.356133, 5.337507, 34.952133, 1023.1982, 7.065868]\n" +
"[\"sg114_3\", \"2008-12-10T19:41:08Z\", -6.39, 21.238808, -157.86618, 25.353163, 5.337024, 34.951065, 1023.1983, 6.4317517]\n" +
"[\"sg114_3\", \"2008-12-10T19:41:14Z\", -5.7, 21.238813, -157.86618, 25.352034, 5.337048, 34.95233, 1023.1996, 5.737243]\n" +
"[\"sg114_3\", \"2008-12-10T19:41:19Z\", -5.04, 21.238823, -157.8662, 25.354284, 5.336977, 34.950283, 1023.1973, 5.072931]\n" +
"[\"sg114_3\", \"2008-12-10T19:41:25Z\", -4.24, 21.238829, -157.86621, 25.353346, 5.337251, 34.95328, 1023.1999, 4.2677035]\n" +
"[\"sg114_3\", \"2008-12-10T19:41:30Z\", -3.55, 21.238836, -157.86621, 25.353527, 5.3372197, 34.953125, 1023.1997, 3.5731952]\n" +
"[\"sg114_3\", \"2008-12-10T19:41:36Z\", -2.65, 21.238846, -157.86623, 25.351152, 5.336866, 34.952633, 1023.2001, 2.6673148]\n" +
"[\"sg114_3\", \"2008-12-10T19:41:42Z\", -1.83, 21.238852, -157.86624, 25.355568, 5.3372297, 34.95217, 1023.19836, 1.841957]\n" +
"[\"sg114_3\", \"2008-12-10T19:41:47Z\", -1.16, 21.23886, -157.86624, 25.352736, 5.3364573, 34.948875, 1023.1968, 1.1675793]\n" +
"[\"sg114_3\", \"2008-12-10T19:41:53Z\", -0.8, 21.238855, -157.86624, 25.330637, 5.30179, 34.71056, 1023.02356, 0.8052271]\n" +
"[\"sg114_3\", \"2008-12-10T19:41:59Z\", -0.75, 21.238853, -157.86624, 25.2926, 2.8720038, 17.5902, 1010.1601, 0.7549004]\n" +
"[\"sg114_3\", \"2008-12-10T19:42:04Z\", -0.72, 21.23885, -157.86623, 25.25033, 3.0869908, 19.06109, 1011.27466, 0.7247044]\n" +
"[\"sg114_3\", \"2008-12-10T19:42:10Z\", -0.7, 21.238853, -157.86624, 25.225939, -3.494945, 21.86908, 1013.3882, 0.70457375]\n";
        String2.log(results);
        Test.ensureEqual(results, expected, "");

    }

    /** This tests quotes in an attribute. */
    public static void testQuotes() throws Throwable {
        String2.log("\n*** EDDTableFromErddap.testQuotes");

        EDDTable edd = (EDDTableFromErddap)oneFromDatasetsXml(null, "testQuotes"); 
        String results = edd.defaultGraphQuery;
        String expected =  
//backslash was actual character in the string, now just encoding here
"longitude,latitude,time&scientific_name=\"Sardinops sagax\"&.draw=markers&.marker=5|5&.color=0x000000&.colorBar=|||||";
        Test.ensureEqual(results, expected, "\nresults=\n" + results); 
    }

    /**
     * This tests dataset from Kevin O'Brien's erddap:
     *  &lt;dataset type="EDDTableFromErddap" datasetID="ChukchiSea_454a_037a_fcf4" active="true"&gt;
     * where DConnect in local ERDDAP complained: connection reset, 
     * but server said everything was fine.
     * I made changes to DConnect 2016-10-03 to deal with this problem.
     */
    public static void testChukchiSea() throws Throwable {
        testVerboseOn();
        String name, tName, results, tResults, expected, expected2, expected3, userDapQuery, tQuery;
        String error = "";
        int epo, tPo;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10); 

        EDDTable eddTable = (EDDTableFromErddap)oneFromDatasetsXml(null, 
            "ChukchiSea_454a_037a_fcf4"); //should work

        //*** test getting das for entire dataset
        String2.log("\n*** EDDTableFromErddap.testChukchiSea das dds for entire dataset\n");
        tName = eddTable.makeNewFileForDapQuery(null, null, "", 
            EDStatic.fullTestCacheDirectory, eddTable.className() + "_Entire", ".das"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = //see OpendapHelper.EOL for comments
"Attributes {\n" +
" s {\n" +
"  prof {\n" +
"    Float64 actual_range 1.0, 1.0;\n" +
"    String axis \"E\";\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Prof\";\n" +
"    String point_spacing \"even\";\n" +
"  }\n" +
"  id {\n" +
"    String cf_role \"profile_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"profile id\";\n" +
"  }\n" +
"  cast {\n" +
"    Float64 colorBarMaximum 100.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Statistics\";\n" +
"    String long_name \"cast number\";\n" +
"  }\n" +
"  cruise {\n" +
"    String ioos_category \"Unknown\";\n" +
"    String long_name \"Cruise name\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, 
            "\nresults=\n" + results);


        
        //*** test getting dds for entire dataset
        tName = eddTable.makeNewFileForDapQuery(null, null, "", 
            EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Entire", ".dds"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    Float64 prof;\n" +
"    String id;\n" +
"    String cast;\n" +
"    String cruise;\n" +
"    Float64 time;\n" +
"    Float32 longitude;\n" +
"    Float32 lon360;\n" +
"    Float32 latitude;\n" +
"    Float32 depth;\n" +
"    Float32 ocean_temperature_1;\n" +
"    Float32 ocean_temperature_2;\n" +
"    Float32 ocean_dissolved_oxygen_concentration_1_mLperL;\n" +
"    Float32 ocean_dissolved_oxygen_concentration_2_mLperL;\n" +
"    Float32 photosynthetically_active_radiation;\n" +
"    Float32 ocean_chlorophyll_a_concentration_factoryCal;\n" +
"    Float32 ocean_chlorophyll_fluorescence_raw;\n" +
"    Float32 ocean_practical_salinity_1;\n" +
"    Float32 ocean_practical_salinity_2;\n" +
"    Float32 ocean_sigma_t;\n" +
"    Float32 sea_water_nutrient_bottle_number;\n" +
"    Float32 sea_water_phosphate_concentration;\n" +
"    Float32 sea_water_silicate_concentration;\n" +
"    Float32 sea_water_nitrate_concentration;\n" +
"    Float32 sea_water_nitrite_concentration;\n" +
"    Float32 sea_water_ammonium_concentration;\n" +
"    Float32 ocean_dissolved_oxygen_concentration_1_mMperkg;\n" +
"    Float32 ocean_dissolved_oxygen_concentration_2_mMperkg;\n" +
"    Float32 ocean_oxygen_saturation_1;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //*** test make data files
        String2.log("\n*** EDDTableFromErddap.testChukchiSea make DATA FILES\n");       

        //.asc
        tName = eddTable.makeNewFileForDapQuery(null, null, 
            "&id=%22ae1001c011%22", //"&id=\"ae1001c011\"", 
            EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data", ".csv"); 
        results = String2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        expected = 
"prof,id,cast,cruise,time,longitude,lon360,latitude,depth,ocean_temperature_1,ocean_temperature_2,ocean_dissolved_oxygen_concentration_1_mLperL,ocean_dissolved_oxygen_concentration_2_mLperL,photosynthetically_active_radiation,ocean_chlorophyll_a_concentration_factoryCal,ocean_chlorophyll_fluorescence_raw,ocean_practical_salinity_1,ocean_practical_salinity_2,ocean_sigma_t,sea_water_nutrient_bottle_number,sea_water_phosphate_concentration,sea_water_silicate_concentration,sea_water_nitrate_concentration,sea_water_nitrite_concentration,sea_water_ammonium_concentration,ocean_dissolved_oxygen_concentration_1_mMperkg,ocean_dissolved_oxygen_concentration_2_mMperkg,ocean_oxygen_saturation_1\n" +
",,,,UTC,degrees_east,degrees_east,degrees_north,m,Degree_C,Degree_C,mL/L,mL/L,microEin cm-2 s-1,micrograms/L,volts,PSU,PSU,kg m-3,number,micromoles/kg,micromoles/kg,micromoles/kg,micromoles/kg,micromoles/kg,micromoles/kg,micromoles/kg,percent saturation\n" +
"1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,0.0,9.5301,NaN,NaN,NaN,NaN,NaN,NaN,31.4801,NaN,24.2852,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,1.0,9.5301,NaN,NaN,NaN,NaN,NaN,NaN,31.4801,NaN,24.2852,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,2.0,9.3234,NaN,NaN,NaN,NaN,NaN,NaN,31.2654,NaN,24.15,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,3.0,9.3112,NaN,NaN,NaN,NaN,NaN,NaN,31.2056,NaN,24.1052,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,4.0,9.3096,NaN,NaN,NaN,NaN,NaN,NaN,31.1971,NaN,24.0988,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,5.0,9.3091,NaN,NaN,NaN,NaN,NaN,NaN,31.177,NaN,24.0831,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,6.0,9.3095,NaN,NaN,NaN,NaN,NaN,NaN,31.1736,NaN,24.0804,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,7.0,9.3,NaN,NaN,NaN,NaN,NaN,NaN,31.1547,NaN,24.0671,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,8.0,9.277,NaN,NaN,NaN,NaN,NaN,NaN,31.1131,NaN,24.0382,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,9.0,8.9942,NaN,NaN,NaN,NaN,NaN,NaN,31.1465,NaN,24.1077,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,10.0,8.5791,NaN,NaN,NaN,NaN,NaN,NaN,31.2294,NaN,24.2349,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,11.0,8.446,NaN,NaN,NaN,NaN,NaN,NaN,31.2322,NaN,24.2567,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,12.0,8.3966,NaN,NaN,NaN,NaN,NaN,NaN,31.2179,NaN,24.2527,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,13.0,8.3742,NaN,NaN,NaN,NaN,NaN,NaN,31.2205,NaN,24.258,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,14.0,8.3406,NaN,NaN,NaN,NaN,NaN,NaN,31.2084,NaN,24.2534,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,15.0,8.218,NaN,NaN,NaN,NaN,NaN,NaN,31.2141,NaN,24.2756,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,16.0,8.0487,NaN,NaN,NaN,NaN,NaN,NaN,31.2508,NaN,24.3285,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,17.0,7.889,NaN,NaN,NaN,NaN,NaN,NaN,31.2932,NaN,24.3844,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,18.0,7.789,NaN,NaN,NaN,NaN,NaN,NaN,31.3088,NaN,24.4106,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,19.0,7.716,NaN,NaN,NaN,NaN,NaN,NaN,31.3123,NaN,24.4235,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,20.0,7.6077,NaN,NaN,NaN,NaN,NaN,NaN,31.3387,NaN,24.4592,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,21.0,7.5372,NaN,NaN,NaN,NaN,NaN,NaN,31.3458,NaN,24.4744,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,22.0,7.4847,NaN,NaN,NaN,NaN,NaN,NaN,31.3587,NaN,24.4917,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,23.0,7.4694,NaN,NaN,NaN,NaN,NaN,NaN,31.3592,NaN,24.4942,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,24.0,7.4452,NaN,NaN,NaN,NaN,NaN,NaN,31.3635,NaN,24.5008,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n" +
"1.0,ae1001c011,011,Ch2010,2010-09-05T11:22:00Z,-168.452,191.548,65.633,25.0,7.4487,NaN,NaN,NaN,NaN,NaN,NaN,31.3765,NaN,24.5106,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

    }
    
    /**
     * This tests the /files/ "files" system.
     * This requires testTableAscii and testTableFromErddap in the localhost ERDDAP.
     */
    public static void testFiles() throws Throwable {

        String2.log("\n*** EDDTableFromErddap.testFiles()\n");
        String tDir = EDStatic.fullTestCacheDirectory;
        String dapQuery, tName, start, query, results, expected;
        int po;

        try {
            //get /files/datasetID/.csv
            results = SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/testTableFromErddap/.csv");
            expected = 
"Name,Last modified,Size,Description\n" +
"subdir/,NaN,NaN,\n" +
"31201_2009.csv,1576697736354,201320,\n" +
"46026_2005.csv,1576697015884,621644,\n" +
"46028_2005.csv,1576697015900,623250,\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //get /files/datasetID/
            results = SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/testTableFromErddap/");
            Test.ensureTrue(results.indexOf("subdir&#x2f;")             > 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("subdir/")                  > 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf("31201&#x5f;2009&#x2e;csv") > 0, "results=\n" + results);
            Test.ensureTrue(results.indexOf(">201320<")                 > 0, "results=\n" + results);

            //get /files/datasetID/subdir/.csv
            results = SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/testTableFromErddap/subdir/.csv");
            expected = 
"Name,Last modified,Size,Description\n" +
"46012_2005.csv,1576697015869,622197,\n" +
"46012_2006.csv,1576697015884,621812,\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //download a file in root
            results = SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/testTableFromErddap/31201_2009.csv");
            expected = 
"This is a header line.\n" +
"*** END OF HEADER\n" +
"# a comment line\n" +
"longitude, latitude, altitude, time, station, wd, wspd, atmp, wtmp\n" +
"# a comment line\n" +
"degrees_east, degrees_north, m, UTC, , degrees_true, m s-1, degree_C, degree_C\n" +
"# a comment line\n" +
"-48.13, -27.7, 0.0, 2005-04-19T00:00:00Z, 31201, NaN, NaN, NaN, 24.4\n" +
"# a comment line\n" +
"#-48.13, -27.7, 0.0, 2005-04-19T01:00:00Z, 31201, NaN, NaN, NaN, 24.4\n" +
"-48.13, -27.7, 0.0, 2005-04-19T01:00:00Z, 31201, NaN, NaN, NaN, 24.4\n" +
"-48.13, -27.7, 0.0, 2005-04-19T02:00:00Z, 31201, NaN, NaN, NaN, 24.3\n"; 
            Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

            //download a file in subdir
            results = SSR.getUrlResponseStringNewline(
                "http://localhost:8080/cwexperimental/files/testTableFromErddap/subdir/46012_2005.csv");
            expected = 
"This is a header line.\n" +
"*** END OF HEADER\n" +
"# a comment line\n" +
"longitude, latitude, altitude, time, station, wd, wspd, atmp, wtmp\n" +
"# a comment line\n" +
"degrees_east, degrees_north, m, UTC, , degrees_true, m s-1, degree_C, degree_C\n" +
"# a comment line\n" +
"-122.88, 37.36, 0.0, 2005-01-01T00:00:00Z, 46012, 190, 8.2, 11.8, 12.5\n" +
"# a comment line\n" +
"# a comment line\n" +
"-122.88, 37.36, 0.0, 2005-01-01T01:00:00Z, 46012, 214, 8.4, 10.4, 12.5\n" +
"-122.88, 37.36, 0.0, 2005-01-01T02:00:00Z, 46012, 210, 7.3, 9.6, 12.5\n"; 
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
                    "http://localhost:8080/cwexperimental/files/testTableFromErddap/gibberish/");
            } catch (Exception e) { 
                results = e.toString();
            }
            expected = 
"java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:8080/cwexperimental/files/testTableFromErddap/gibberish/\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: Resource not found: directory=gibberish/\";\n" +
"})";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //try to download a non-existent file
            try {
                results = SSR.getUrlResponseStringNewline(
                    "http://localhost:8080/cwexperimental/files/testTableFromErddap/gibberish.csv");
            } catch (Exception e) { 
                results = e.toString();
            }
            expected = 
"java.io.IOException: ERROR from url=http://localhost:8080/cwexperimental/files/testTableFromErddap/gibberish.csv : " +
    "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: " +
    "http://localhost:8080/cwexperimental/files/testTableAscii/gibberish.csv\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: File not found: gibberish.csv .\";\n" +
"})";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //try to download a non-existent file in existant subdir
            try {
                results = SSR.getUrlResponseStringNewline(
                    "http://localhost:8080/cwexperimental/files/testTableFromErddap/subdir/gibberish.csv");
            } catch (Exception e) { 
                results = e.toString();
            }
            expected = 
"java.io.IOException: ERROR from url=http://localhost:8080/cwexperimental/files/testTableFromErddap/subdir/gibberish.csv : " +
    "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: " +
    "http://localhost:8080/cwexperimental/files/testTableAscii/subdir/gibberish.csv\n" +
"(Error {\n" +
"    code=404;\n" +
"    message=\"Not Found: File not found: gibberish.csv .\";\n" +
"})";
            Test.ensureEqual(results, expected, "results=\n" + results);

 

        } catch (Throwable t) {
            throw new RuntimeException("This test requires testTableAscii and testTableFromErddap in the localhost ERDDAP.\n" +
                "Unexpected error.", t); 
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
            lastTest = interactive? -1 : 6;
        String msg = "\n^^^ EDDTableFromErddap.test(" + interactive + ") test=";

        for (int test = firstTest; test <= lastTest; test++) {
            try {
                long time = System.currentTimeMillis();
                String2.log(msg + test);
            
                if (interactive) {
                    //if (test ==  0) ...;

                } else {
                    if (test ==  0) testBasic(true);   //rTestNccsvScalar
                    if (test ==  1) testBasic(false);  //rTestNccsvScalarNoRedirect
                    if (test ==  2) testGenerateDatasetsXml();
                    if (test ==  3) testTableNoIoosCat();
                    if (test ==  4) testQuotes();
                    if (test ==  5) testChukchiSea();
                    if (test ==  6) testFiles();
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
