/* 
 * EDDTableFromFileNames Copyright 2015, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.LongArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.array.StringComparatorIgnoreCase;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/** 
 * This class represents a table of file names.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2015-01-06
 */
public class EDDTableFromFileNames extends EDDTable{ 

    protected String fileDir; //has forward slashes and trailing slash 
    protected String fileNameRegex;
    protected boolean recursive;
    protected String extractRegex[];        
    protected byte extractGroup[];        
    protected boolean useCachedDNLSInfo = false; //DNLS info with directoriesToo=false

    //standard variable names
    public final static String URL          = FileVisitorDNLS.URL;          //"url";
    public final static String DIRECTORY    = FileVisitorDNLS.DIRECTORY;    //"directory";
    public final static String NAME         = FileVisitorDNLS.NAME;         //"name";
    public final static String LASTMODIFIED = FileVisitorDNLS.LASTMODIFIED; //"lastModified";
    public final static String SIZE         = FileVisitorDNLS.SIZE;         //"size";



    /**
     * This constructs an EDDTableFromFileNames based on the information in an .xml file.
     * 
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableFromFileNames"&gt; 
     *    having just been read.  
     * @return an EDDTableFromFileNames.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDTableFromFileNames fromXml(SimpleXMLReader xmlReader) throws Throwable {

        //data to be obtained (or not)
        if (verbose) String2.log("\n*** constructing EDDTableFromFileNames(xmlReader)...");
        String tDatasetID = xmlReader.attributeValue("datasetID"); 
        Attributes tGlobalAttributes = null;
        ArrayList tDataVariables = new ArrayList();
        int tReloadEveryNMinutes = Integer.MAX_VALUE;
        String tAccessibleTo = null;
        String tFileDir = null;
        String tFileNameRegex = null;
        boolean tRecursive = false;
        StringArray tOnChange = new StringArray();
        String tFgdcFile = null;
        String tIso19115File = null;
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
            if      (localTags.equals("<addAttributes>"))
                tGlobalAttributes = getAttributesFromXml(xmlReader);
            else if (localTags.equals( "<altitudeMetersPerSourceUnit>")) 
                throw new SimpleException(EDVAlt.stopUsingAltitudeMetersPerSourceUnit);
            else if (localTags.equals( "<dataVariable>")) 
                tDataVariables.add(getSDADVariableFromXml(xmlReader));           
            else if (localTags.equals( "<accessibleTo>")) {}
            else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
            else if (localTags.equals( "<reloadEveryNMinutes>")) {}
            else if (localTags.equals("</reloadEveryNMinutes>")) tReloadEveryNMinutes = String2.parseInt(content); 
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
            else if (localTags.equals( "<fileDir>")) {} 
            else if (localTags.equals("</fileDir>")) tFileDir = content; 
            else if (localTags.equals( "<fileNameRegex>")) {}
            else if (localTags.equals("</fileNameRegex>")) tFileNameRegex = content; 
            else if (localTags.equals( "<recursive>")) {}
            else if (localTags.equals("</recursive>")) tRecursive = String2.parseBoolean(content); 

            else xmlReader.unexpectedTagException();
        }
        int ndv = tDataVariables.size();
        Object ttDataVariables[][] = new Object[ndv][];
        for (int i = 0; i < tDataVariables.size(); i++)
            ttDataVariables[i] = (Object[])tDataVariables.get(i);

        return new EDDTableFromFileNames(tDatasetID, tAccessibleTo,
            tOnChange, tFgdcFile, tIso19115File, 
            tDefaultDataQuery, tDefaultGraphQuery, tGlobalAttributes,
            ttDataVariables,
            tReloadEveryNMinutes, tFileDir, tFileNameRegex, tRecursive);

    }

    /**
     * The constructor.
     *
     * <p>Assumptions about what constraints the source can handle:
     * <br>Numeric variables: outer sequence (any operator except regex), inner sequence (no constraints).
     * <br>String variables:
     * <br>    outer sequence 
     * <br>        support for = and != is set by sourceCanConstrainStringEQNE (default=true)
     * <br>        support for &lt; &lt;= &gt; &gt;= is set by sourceCanConstrainStringGTLT (default=true), 
     * <br>        regex support set by sourceCanConstrainStringRegex (default ""), 
     * <br>    inner sequence (no constraints ever). 
     *
     * <p>Yes, lots of detailed information must be supplied here
     * that is sometimes available in metadata. If it is in metadata,
     * make a subclass that extracts info from metadata and calls this 
     * constructor.
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
     * @param tAddGlobalAttributes are global attributes which will
     *   be added to (and take precedence over) the data source's global attributes.
     *   This may be null if you have nothing to add.
     *   The combined global attributes must include:
     *   <ul>
     *   <li> "title" - the short (&lt; 80 characters) description of the dataset 
     *   <li> "summary" - the longer description of the dataset.
     *      It may have newline characters (usually at &lt;= 72 chars per line). 
     *   <li> "institution" - the source of the data 
     *      (best if &lt; 50 characters so it fits in a graph's legend).
     *   <li> "infoUrl" - the url with information about this data set 
     *   <li> "cdm_data_type" - one of the EDD.CDM_xxx options
     *   </ul>
     *   Special case: value="null" causes that item to be removed from combinedGlobalAttributes.
     *   Special case: if combinedGlobalAttributes name="license", any instance of value="[standard]"
     *     will be converted to the EDStatic.standardLicense.
     * @param tDataVariables is an Object[nDataVariables][3]: 
     *    <br>[0]=String sourceName (the name of the data variable in the dataset source, 
     *         without the outer or inner sequence name),
     *    <br>[1]=String destinationName (the name to be presented to the ERDDAP user, 
     *        or null to use the sourceName),
     *    <br>[2]=Attributes addAttributes (at ERD, this must have "ioos_category" - 
     *        a category from EDV.ioosCategories). 
     *        Special case: value="null" causes that item to be removed from combinedAttributes.
     *    <br>The order of variables you define doesn't have to match the
     *       order in the source.
     *    <p>If there is a time variable,  
     *      either tAddAttributes (read first) or tSourceAttributes must have "units"
     *      which is either <ul>
     *      <li> a UDUunits string (containing " since ")
     *        describing how to interpret source time values 
     *        (which should always be numeric since they are a dimension of a grid)
     *        (e.g., "seconds since 1970-01-01T00:00:00").
     *      <li> a org.joda.time.format.DateTimeFormat string
     *        (which is compatible with java.text.SimpleDateFormat) describing how to interpret 
     *        string times  (e.g., the ISO8601TZ_FORMAT "yyyy-MM-dd'T'HH:mm:ssZ", see 
     *        http://joda-time.sourceforge.net/api-release/org/joda/time/format/DateTimeFormat.html or 
     *        http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html).
     *      </ul>
     * @param tReloadEveryNMinutes indicates how often the source should
     *    be checked for new data.
     * @throws Throwable if trouble
     */
    public EDDTableFromFileNames(String tDatasetID, String tAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes,
        String tFileDir, String tFileNameRegex, boolean tRecursive) throws Throwable {

        if (verbose) String2.log(
            "\n*** constructing EDDTableFromFileNames " + tDatasetID); 
        long constructionStartMillis = System.currentTimeMillis();
        String errorInMethod = "Error in EDDTableFromFileNames(" + 
            tDatasetID + ") constructor:\n";
            
        //save some of the parameters
        className = "EDDTableFromFileNames"; 
        datasetID = tDatasetID;
        setAccessibleTo(tAccessibleTo);
        onChange = tOnChange;
        fgdcFile = tFgdcFile;
        iso19115File = tIso19115File;
        defaultDataQuery = tDefaultDataQuery;
        defaultGraphQuery = tDefaultGraphQuery;
        if (tAddGlobalAttributes == null)
            tAddGlobalAttributes = new Attributes();
        addGlobalAttributes = tAddGlobalAttributes;
        addGlobalAttributes.setIfNotAlreadySet("sourceUrl", "(local files)");
        setReloadEveryNMinutes(tReloadEveryNMinutes);
        fileDir = tFileDir;
        fileNameRegex = tFileNameRegex;
        Test.ensureNotNull(fileDir,       "fileDir");
        Test.ensureNotNull(fileNameRegex, "fileNameRegex");
        fileDir = File2.addSlash(String2.replaceAll(fileDir, '\\', '/')); 
        recursive = tRecursive;

        //let standardizeResultsTable handle all constraints
        sourceCanConstrainNumericData = CONSTRAIN_NO; 
        sourceCanConstrainStringData  = CONSTRAIN_NO; 
        sourceCanConstrainStringRegex = ""; 

        //quickRestart isn't needed       
        
        //get global attributes
        sourceGlobalAttributes = new Attributes();
        combinedGlobalAttributes = new Attributes(addGlobalAttributes, sourceGlobalAttributes); //order is important
        String tLicense = combinedGlobalAttributes.getString("license");
        if (tLicense != null)
            combinedGlobalAttributes.set("license", 
                String2.replaceAll(tLicense, "[standard]", EDStatic.standardLicense));
        combinedGlobalAttributes.removeValue("null");

        //useCachedInfo?
        Table tCachedDNLSTable = null;
        useCachedDNLSInfo = File2.isRemote(fileDir);
        if (useCachedDNLSInfo) {
            String qrName = quickRestartFullFileName();

            if (EDStatic.quickRestart && 
                EDStatic.initialLoadDatasets() && 
                File2.isFile(qrName)) {

                //try to do quickRestart
                //set creationTimeMillis to time of previous creation, so next time
                //to be reloaded will be same as if ERDDAP hadn't been restarted.
                long tCreationTime = File2.getLastModified(qrName); //0 if trouble
                if (verbose)
                    String2.log("  quickRestart " + tDatasetID + " previous=" + 
                        Calendar2.millisToIsoZuluString(tCreationTime) + "Z");

                //Ensure quickRestart information is recent.
                //If too old: abandon construction, delete quickRestart file, flag dataset reloadASAP
                ensureQuickRestartInfoIsRecent(tDatasetID, 
                    tReloadEveryNMinutes == Integer.MAX_VALUE? DEFAULT_RELOAD_EVERY_N_MINUTES : 
                        tReloadEveryNMinutes, 
                    tCreationTime, qrName);

                //use cached info
                tCachedDNLSTable = getCachedDNLSTable();
            } 

            if (tCachedDNLSTable == null) {

                //get the info to be cached
                tCachedDNLSTable = FileVisitorDNLS.oneStep(fileDir, fileNameRegex,
                    recursive, false); //tDirectoriesToo
                tCachedDNLSTable.setColumn(2, new DoubleArray(tCachedDNLSTable.getColumn(2))); //long -> double
                tCachedDNLSTable.setColumn(3, new DoubleArray(tCachedDNLSTable.getColumn(3))); //long -> double
                if (tCachedDNLSTable.nRows() == 0)
                    throw new SimpleException(MustBe.THERE_IS_NO_DATA + 
                        " (0 matching files)");

                //store it
                File2.makeDirectory(datasetDir());
                tCachedDNLSTable.saveAsFlatNc(qrName, "row"); //throws exceptions

                //prepare for below
                tCachedDNLSTable.setColumn(2, new LongArray(tCachedDNLSTable.getColumn(2))); //double -> long
                tCachedDNLSTable.setColumn(3, new LongArray(tCachedDNLSTable.getColumn(3))); //double -> long
            }
        }

        //get a source table (this also ensures there are valid files in fileDir)
        Table sourceBasicTable;
        if (useCachedDNLSInfo) {
            //use tCachedDNLSTable since I just got it (above) 
            //and perhaps time consuming to get it
            sourceBasicTable = FileVisitorDNLS.oneStepDoubleWithUrlsNotDirs(
                FileVisitorDNLS.oneStepDouble(tCachedDNLSTable), 
                fileDir, 
                EDStatic.erddapUrl(null) + "/files/" + datasetID + "/"); //loggedInAs=null doesn't matter here
        } else {
            sourceBasicTable = getBasicTable(fileDir, fileNameRegex, recursive, 
                null, datasetID); //loggedInAs - irrelevant since just getting this for metadata, not data
        }

        //create dataVariables[]
        int ndv = tDataVariables.length;
        dataVariables = new EDV[ndv];
        extractRegex = new String[ndv];
        extractGroup = new byte[ndv];
        for (int dv = 0; dv < ndv; dv++) {
            String sourceName = (String)tDataVariables[dv][0];
            String destName = (String)tDataVariables[dv][1];
            if (destName == null || destName.trim().length() == 0)
                destName = sourceName;
            int scol = sourceBasicTable.findColumnNumber(sourceName);
            Attributes sourceAtt = scol >= 0? sourceBasicTable.columnAttributes(scol) : 
                new Attributes();
            Attributes addAtt = (Attributes)tDataVariables[dv][2];
            String sourceType = (String)tDataVariables[dv][3];
            //if (reallyVerbose) String2.log("  dv=" + dv + " sourceName=" + tSourceName + " sourceType=" + tSourceType);
            extractRegex[dv] = addAtt.getString("extractRegex");
            extractGroup[dv] = Math2.narrowToByte(addAtt.getInt("extractGroup"));
            addAtt.remove("extractRegex");
            addAtt.remove("extractGroup");
            if (extractGroup[dv] == Byte.MAX_VALUE)
                extractGroup[dv] = 1; //default
            if (!sourceName.startsWith("=") &&
                !sourceName.equals(URL) &&
                !sourceName.equals(NAME) &&
                !sourceName.equals(LASTMODIFIED) &&
                !sourceName.equals(SIZE)) {
                Test.ensureTrue(String2.isSomething(extractRegex[dv]),
                    "'extractRegex' attribute wasn't specified for sourceName=" + 
                    sourceName);
                if (extractRegex[dv].indexOf('(') < 0 ||
                    extractRegex[dv].indexOf(')') < 0)
                    throw new RuntimeException(
                        "extractRegex=" + extractRegex[dv] + " for sourceName=" + 
                        sourceName + 
                        " must have the form: \"preRegex(groupRegex)postRegex\".");
                if (extractGroup[dv] < 1)
                    throw new RuntimeException(
                        "extractGroup=" + extractGroup[dv] + " for sourceName=" + 
                        sourceName + " must be >= 1.");
            }

            if (sourceType == null) 
                throw new IllegalArgumentException(errorInMethod + 
                    "dataVariable#" + dv + " <sourceType> wasn't specified.");

            if (EDV.LON_NAME.equals(destName)) {
                dataVariables[dv] = new EDVLon(sourceName,
                    sourceAtt, addAtt, 
                    sourceType, Double.NaN, Double.NaN); 
                lonIndex = dv;
            } else if (EDV.LAT_NAME.equals(destName)) {
                dataVariables[dv] = new EDVLat(sourceName,
                    sourceAtt, addAtt, 
                    sourceType, Double.NaN, Double.NaN); 
                latIndex = dv;
            } else if (EDV.ALT_NAME.equals(destName)) {
                dataVariables[dv] = new EDVAlt(sourceName,
                    sourceAtt, addAtt, 
                    sourceType, Double.NaN, Double.NaN);
                altIndex = dv;
            } else if (EDV.DEPTH_NAME.equals(destName)) {
                dataVariables[dv] = new EDVDepth(sourceName,
                    sourceAtt, addAtt, 
                    sourceType, Double.NaN, Double.NaN);
                depthIndex = dv;
            } else if (EDV.TIME_NAME.equals(destName)) {  //look for TIME_NAME before check hasTimeUnits (next)
                dataVariables[dv] = new EDVTime(sourceName,
                    sourceAtt, addAtt, 
                    sourceType); //this constructor gets source / sets destination actual_range
                timeIndex = dv;
            } else if (EDVTimeStamp.hasTimeUnits(sourceAtt, addAtt)) {
                dataVariables[dv] = new EDVTimeStamp(sourceName, destName, 
                    sourceAtt, addAtt,
                    sourceType); //this constructor gets source / sets destination actual_range
            } else {
                dataVariables[dv] = new EDV(sourceName, destName, 
                    sourceAtt, addAtt,
                    sourceType); //the constructor that reads actual_range
                //dataVariables[dv].setActualRangeFromDestinationMinMax();
            }
        }

        //get the data and set variable min and max
        //this also verifies that fileDir is accessible.
        File2.makeDirectory(cacheDirectory()); 
        TableWriterAllWithMetadata twawm = new TableWriterAllWithMetadata(
            cacheDirectory(), "constructor");
        getDataForDapQuery( //throws exception if 0 files
            EDStatic.loggedInAsSuperuser, 
            "/erddap/tabledap/" + datasetID + ".nc", "", //userDapQuery
            twawm);
        for (int dv = 0; dv < ndv; dv++) {
            EDV edv = dataVariables[dv];
            if (edv.destinationDataTypeClass() != String.class) {
                //Min and max were gathered as the data was written.
                //How cool is that?!
                edv.setDestinationMinMax(twawm.columnMinValue(dv), twawm.columnMaxValue(dv));
            }
        }
        twawm.releaseResources();        

        //accessibleViaFiles
        if (EDStatic.filesActive) {
            accessibleViaFilesDir = fileDir;
            accessibleViaFilesRegex = fileNameRegex;
            accessibleViaFilesRecursive = recursive;
        }

        //ensure the setup is valid
        ensureValid();

        //finally
        if (verbose) String2.log(
            (reallyVerbose? "\n" + toString() : "") +
            "\n*** EDDTableFromFileNames " + datasetID + " constructor finished. TIME=" + 
            (System.currentTimeMillis() - constructionStartMillis) + "\n"); 

    }

    /** fileDir has forward slashes and trailing slash */
    public String fileDir() {return fileDir;}
    public String fileNameRegex() {return fileNameRegex;}
    public boolean recursive() {return recursive;}

    /** 
     * This gets the cached DNLS file table (last_mod and size are longs). 
     * Only call this if useCachedDNLSInfo==true.
     *
     * @throws Exception
     */
    public Table getCachedDNLSTable() throws Exception {
        Table table = new Table();
        table.readFlatNc(quickRestartFullFileName(), null, 0); 
        table.setColumn(2, new LongArray(table.getColumn(2))); //double -> long
        table.setColumn(3, new LongArray(table.getColumn(3))); //double -> long
        return table;
    }

    /** 
     * This returns a fileTable (formatted like 
     * FileVisitorDNLS.oneStep(tDirectoriesToo=false, last_mod is LongArray,
     * and size is LongArray of epochMillis)
     * with valid files (or null if unavailable or any trouble).
     * This is a copy of any internal data, so client can modify the contents.
     */
    public Table accessibleViaFilesFileTable() {
        try {
            return useCachedDNLSInfo?
                getCachedDNLSTable() :
                FileVisitorDNLS.oneStep(fileDir, fileNameRegex, recursive, false); //dirToo=false
        } catch (Exception e) {
            String2.log(MustBe.throwableToString(e));
            return null;
        }
    }

    /**
     * Get low level data: URL, NAME, LASTMODIFIED (as double epoch seconds), SIZE (as double)
     *
     * @throws Exception if trouble, e.g., 0 matching files
     */
    public static Table getBasicTable(String fileDir, String fileNameRegex, 
        boolean recursive, String loggedInAs, String datasetID) throws Exception {
        Table table = FileVisitorDNLS.oneStepDoubleWithUrlsNotDirs(
            fileDir, fileNameRegex, recursive, 
            EDStatic.erddapUrl(loggedInAs) + "/files/" + datasetID + "/");
        int nRows = table.nRows();
        if (nRows == 0)
            throw new SimpleException(MustBe.THERE_IS_NO_DATA + 
                " (0 matching files)");
        return table;
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

        //get the sourceDapQuery (a query that the source can handle)
        StringArray resultsVariables    = new StringArray();
        StringArray constraintVariables = new StringArray();
        StringArray constraintOps       = new StringArray();
        StringArray constraintValues    = new StringArray();
        getSourceQueryFromDapQuery(userDapQuery, resultsVariables,
            constraintVariables, constraintOps, constraintValues); //timeStamp constraints other than regex are epochSeconds

        //get low level data: URL, NAME, LASTMODIFIED (as double epoch seconds), SIZE (as double)
        Table table;
        if (useCachedDNLSInfo) {
            table = FileVisitorDNLS.oneStepDoubleWithUrlsNotDirs(
                FileVisitorDNLS.oneStepDouble(getCachedDNLSTable()), 
                fileDir, 
                EDStatic.erddapUrl(loggedInAs) + "/files/" + datasetID + "/");
        } else {
            table = getBasicTable(fileDir, fileNameRegex, recursive, 
                loggedInAs, datasetID); //loggedInAs - irrelevant since just getting this for metadata, not data
        }
        int nRows = table.nRows();

        //create other results variables as needed
        int namei = table.findColumnNumber(NAME);
        StringArray namePA = (StringArray)table.getColumn(namei);
        Attributes atts = table.columnAttributes(namei);
        int nrv = resultsVariables.size();
        for (int rvi = 0; rvi < nrv; rvi++) {
            //create this variable by extracting info from file name
            String sourceName = resultsVariables.get(rvi);
            if (sourceName.equals(URL) ||
                sourceName.equals(NAME) ||
                sourceName.equals(LASTMODIFIED) ||
                sourceName.equals(SIZE))
                continue;
            int dvi = String2.indexOf(dataVariableSourceNames(), sourceName);
            if (dvi < 0)
                throw new SimpleException(String2.ERROR + 
                    ": Unexpected resultsVariable sourceName=" + sourceName);
            EDV edv = dataVariables[dvi];

            //create this source variable
            PrimitiveArray pa = PrimitiveArray.factory(edv.sourceDataTypeClass(),
                nRows, false); 
            table.addColumn(sourceName, pa);
            String regex = extractRegex[dvi];
            Pattern pat = Pattern.compile(regex);
            for (int row = 0; row < nRows; row++) {
                Matcher matcher = pat.matcher(namePA.get(row));
                pa.addString(matcher.matches()? matcher.group(extractGroup[dvi]) : ""); 
            }
        }

        //String2.log(table.toString());
        standardizeResultsTable(requestUrl, userDapQuery, table);
        tableWriter.writeAllAndFinish(table);
    }


    /** 
     * This does its best to generate a read-to-use datasets.xml entry for an
     * EDDTableFromFileNames.
     *
     * @param tFileDir
     * @param tFileNameRegex
     * @param tRecursive
     * @param externalGlobalAttributes globalAttributes gleaned from external 
     *    sources, e.g., a THREDDS catalog.xml file.
     *    These have priority over other sourceGlobalAttributes.
     *    Okay to use null if none.
     * @return a suggested chunk of xml for this dataset for use in datasets.xml 
     * @throws Throwable if trouble.
     *   If no trouble, then a valid dataset.xml chunk has been returned.
     */
    public static String generateDatasetsXml(
        String tFileDir, String tFileNameRegex, boolean tRecursive,
        int tReloadEveryNMinutes, 
        String tInfoUrl, String tInstitution, String tSummary, String tTitle,
        Attributes externalGlobalAttributes) throws Throwable {

        String2.log("EDDTableFromFileNames.generateDatasetsXml" +
            "\n  tFileDir=" + tFileDir);
        tFileDir = File2.addSlash(String2.replaceAll(tFileDir, '\\', '/'));
        String tDatasetID = suggestDatasetID(
            //if awsS3, important that it start with tFileDir
            File2.addSlash(tFileDir) + tFileNameRegex +
            //distinguish from e.g., EDDGridFromNcFiles for same files
            "(EDDTableFromFileNames)"); 
        boolean remoteFiles = File2.isRemote(tFileDir);
        if (tReloadEveryNMinutes < suggestReloadEveryNMinutesMin ||
            tReloadEveryNMinutes > suggestReloadEveryNMinutesMax) 
            tReloadEveryNMinutes = remoteFiles? 120 : DEFAULT_RELOAD_EVERY_N_MINUTES;

        //make the sourceTable and addTable
        Table sourceTable = FileVisitorDNLS.oneStepDoubleWithUrlsNotDirs(
            tFileDir, tFileNameRegex, tRecursive, 
            EDStatic.erddapUrl(null) + "/files/" + tDatasetID + "/");

        Table addTable = new Table();
        addTable.globalAttributes()
            .add("cdm_data_type", "Other")
            .add("creator_name", "null")
            .add("creator_email", "null")
            .add("creator_url", "null")
            .add("history", "null")
            .add("infoUrl",     String2.isSomething(tInfoUrl    )? tInfoUrl     : "???")
            .add("institution", String2.isSomething(tInstitution)? tInstitution : "???")
            .add("sourceUrl", "(" + (remoteFiles? "remote" : "local") + " files)")
            .add("summary",     String2.isSomething(tSummary    )? tSummary     : "???")
            .add("title",       String2.isSomething(tTitle      )? tTitle       : "???");
        int nCols = sourceTable.nColumns();
        for (int col = 0; col < nCols; col++) {
            String sourceName = sourceTable.getColumnName(col);
            Attributes sourceAtts = sourceTable.columnAttributes(col);
            Attributes addAtts = new Attributes();
            addTable.addColumn(col, sourceName, 
                (PrimitiveArray)sourceTable.getColumn(col).clone(), addAtts);
        }
        HashSet<String> keywords = suggestKeywords(sourceTable, addTable);
        cleanSuggestedKeywords(keywords);
        String keywordSar[] = (String[])keywords.toArray(new String[0]); 
        Arrays.sort(keywordSar, new StringComparatorIgnoreCase()); 
        addTable.globalAttributes().add("keywords", String2.toCSSVString(keywordSar));


        //write the information
        StringBuilder sb = new StringBuilder();
        sb.append(
            directionsForGenerateDatasetsXml() +
            "-->\n\n" +
            "<dataset type=\"EDDTableFromFileNames\" datasetID=\"" + tDatasetID + 
                "\" active=\"true\">\n" +
            "    <fileDir>" + tFileDir + "</fileDir>\n" +
            "    <fileNameRegex>" + XML.encodeAsXML(tFileNameRegex) + "</fileNameRegex>\n" +
            "    <recursive>" + tRecursive + "</recursive>\n" +
            "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n");
        sb.append(writeAttsForDatasetsXml(false, sourceTable.globalAttributes(), "    "));
        sb.append(writeAttsForDatasetsXml(true,     addTable.globalAttributes(), "    "));
        sb.append(writeVariablesForDatasetsXml(sourceTable, addTable, 
            "dataVariable", 
            true, false, false)); //includeDataType, tryToFindLLAT, questionDestinationName
        sb.append(
"    <!-- You can create other variables which are derived from extracts\n" +
"         from the file names.  Use an extractRegex attribute to specify a\n" +
"         regular expression with a capturing group (in parentheses). The\n" +
"         part of the file name which matches the specified capturing group\n" +
"         (usually group #1) will be extracted to make the new data variable.\n" +
"         Below are examples showing how to extract a date and how to extract\n" +
"         an integer.\n" +
"    <dataVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <addAttributes>\n" +
"            <att name=\"extractRegex\">jplMURSST(.*)\\.png</att>\n" +
"            <att name=\"extractGroup\" type=\"int\">1</att>\n" +
"            <att name=\"units\">yyyyMMddHHmmss</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>day</sourceName>\n" +
"        <destinationName>day</destinationName>\n" +
"        <dataType>int</dataType>\n" +
"        <addAttributes>\n" +
"            <att name=\"extractRegex\">jplMURSST.{6}(..).{6}\\.png</att>\n" +
"            <att name=\"extractGroup\" type=\"int\">1</att>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    -->\n");
        sb.append(
            "</dataset>\n" +
            "\n");

        String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
        return sb.toString();
    }

    /**
     * testGenerateDatasetsXml
     */
    public static void testGenerateDatasetsXml() throws Throwable {
        String2.log("\n*** EDDTableFromFileNames.testGenerateDatasetsXml()");
        testVerboseOn();

        String tDir = EDStatic.unitTestDataDir + "fileNames";
        String tRegex = ".*\\.png";
        boolean tRecursive = true;
        String tInfoUrl = "http://mur.jpl.nasa.gov/";
        String tInstitution = "NASA JPL";
        String tSummary = "Images from JPL MUR SST Daily.";
        String tTitle = "JPL MUR SST Images";
        //datasetID changes with different unitTestDataDir
        String tDatasetID = "fileNames_e21d_ef79_13da";
String expected = 
directionsForGenerateDatasetsXml() +
"-->\n" +
"\n" +
"<dataset type=\"EDDTableFromFileNames\" datasetID=\"" + tDatasetID + "\" active=\"true\">\n" +
"    <fileDir>" + tDir + "/</fileDir>\n" +
"    <fileNameRegex>.*\\.png</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" +
"    <!-- sourceAttributes>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Other</att>\n" +
"        <att name=\"creator_email\">null</att>\n" +
"        <att name=\"creator_name\">null</att>\n" +
"        <att name=\"creator_url\">null</att>\n" +
"        <att name=\"history\">null</att>\n" +
"        <att name=\"infoUrl\">" + tInfoUrl + "</att>\n" +
"        <att name=\"institution\">" + tInstitution + "</att>\n" +
"        <att name=\"keywords\">data, file, high, identifier, images, jet, jpl, laboratory, lastModified, modified, multi, multi-scale, mur, name, nasa, propulsion, resolution, scale, sea, size, sst, surface, temperature, time, ultra, ultra-high</att>\n" +
"        <att name=\"sourceUrl\">(local files)</att>\n" +
"        <att name=\"summary\">" + tSummary + "</att>\n" +
"        <att name=\"title\">" + tTitle + "</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>url</sourceName>\n" +
"        <destinationName>url</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">URL</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>name</sourceName>\n" +
"        <destinationName>name</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">File Name</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>lastModified</sourceName>\n" +
"        <destinationName>lastModified</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Last Modified</att>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>size</sourceName>\n" +
"        <destinationName>size</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"ioos_category\">Other</att>\n" +
"            <att name=\"long_name\">Size</att>\n" +
"            <att name=\"units\">bytes</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <!-- You can create other variables which are derived from extracts\n" +
"         from the file names.  Use an extractRegex attribute to specify a\n" +
"         regular expression with a capturing group (in parentheses). The\n" +
"         part of the file name which matches the specified capturing group\n" +
"         (usually group #1) will be extracted to make the new data variable.\n" +
"         Below are examples showing how to extract a date and how to extract\n" +
"         an integer.\n" +
"    <dataVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <addAttributes>\n" +
"            <att name=\"extractRegex\">jplMURSST(.*)\\.png</att>\n" +
"            <att name=\"extractGroup\" type=\"int\">1</att>\n" +
"            <att name=\"units\">yyyyMMddHHmmss</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>day</sourceName>\n" +
"        <destinationName>day</destinationName>\n" +
"        <dataType>int</dataType>\n" +
"        <addAttributes>\n" +
"            <att name=\"extractRegex\">jplMURSST.{6}(..).{6}\\.png</att>\n" +
"            <att name=\"extractGroup\" type=\"int\">1</att>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    -->\n" +
"</dataset>\n\n\n";
        String results = generateDatasetsXml(tDir, tRegex, tRecursive, -1, 
            tInfoUrl, tInstitution, tSummary, tTitle, null) + "\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDTableFromFileNames",
            tDir, tRegex, "" + tRecursive, "-1", 
            tInfoUrl, tInstitution, tSummary, tTitle},
            false); //doIt loop?
        Test.ensureEqual(gdxResults, results, 
            "Unexpected results from GenerateDatasetsXml.doIt.");

        //ensure it is ready-to-use by making a dataset from it
        String2.log("results=\n" + results);
        EDD edd = oneFromXmlFragment(results);
        Test.ensureEqual(edd.datasetID(), tDatasetID, "");
        Test.ensureEqual(edd.title(), tTitle, "");
        Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
            "url, name, lastModified, size",
            "");
        String2.log("\n*** EDDTableFromFileNames.testGenerateDatasetsXml() finished successfully.");
    }

    /**
     * testGenerateDatasetsXmlAwsS3
     * Your S3 credentials must be in 
     * <br> ~/.aws/credentials on Linux, OS X, or Unix
     * <br> C:\Users\USERNAME\.aws\credentials on Windows
     * See http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/java-dg-setup.html .
     */
    public static void testGenerateDatasetsXmlAwsS3() throws Throwable {
        String2.log("\n*** EDDTableFromFileNames.testGenerateDatasetsXmlAwsS3()");
        try {

        testVerboseOn();

        String tDir = "http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS";
        //tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_CESM1-CAM5_200601-201012.nc
        String tRegex = ".*_CESM1-CAM5_.*\\.nc";
        boolean tRecursive = true;
        String tInfoUrl = "https://nex.nasa.gov/nex/";
        String tInstitution = "NASA Earth Exchange";
        String tSummary = "My great summary";
        String tTitle = "My Great Title";
        String tDatasetID = "s3nasanex_803b_6c09_f004";
String expected = 
directionsForGenerateDatasetsXml() +
"-->\n" +
"\n" +
"<dataset type=\"EDDTableFromFileNames\" datasetID=\"" + tDatasetID + "\" active=\"true\">\n" +
"    <fileDir>" + tDir + "/</fileDir>\n" +
"    <fileNameRegex>" + tRegex + "</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <reloadEveryNMinutes>120</reloadEveryNMinutes>\n" +
"    <!-- sourceAttributes>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Other</att>\n" +
"        <att name=\"creator_email\">null</att>\n" +
"        <att name=\"creator_name\">null</att>\n" +
"        <att name=\"creator_url\">null</att>\n" +
"        <att name=\"history\">null</att>\n" +
"        <att name=\"infoUrl\">" + tInfoUrl + "</att>\n" +
"        <att name=\"institution\">" + tInstitution + "</att>\n" +
"        <att name=\"keywords\">data, earth, exchange, file, great, identifier, lastModified, modified, name, nasa, size, time, title</att>\n" +
"        <att name=\"sourceUrl\">(remote files)</att>\n" +
"        <att name=\"summary\">" + tSummary + "</att>\n" +
"        <att name=\"title\">" + tTitle + "</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>url</sourceName>\n" +
"        <destinationName>url</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">URL</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>name</sourceName>\n" +
"        <destinationName>name</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">File Name</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>lastModified</sourceName>\n" +
"        <destinationName>lastModified</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Last Modified</att>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>size</sourceName>\n" +
"        <destinationName>size</destinationName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"ioos_category\">Other</att>\n" +
"            <att name=\"long_name\">Size</att>\n" +
"            <att name=\"units\">bytes</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <!-- You can create other variables which are derived from extracts\n" +
"         from the file names.  Use an extractRegex attribute to specify a\n" +
"         regular expression with a capturing group (in parentheses). The\n" +
"         part of the file name which matches the specified capturing group\n" +
"         (usually group #1) will be extracted to make the new data variable.\n" +
"         Below are examples showing how to extract a date and how to extract\n" +
"         an integer.\n" +
"    <dataVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <dataType>String</dataType>\n" +
"        <addAttributes>\n" +
"            <att name=\"extractRegex\">jplMURSST(.*)\\.png</att>\n" +
"            <att name=\"extractGroup\" type=\"int\">1</att>\n" +
"            <att name=\"units\">yyyyMMddHHmmss</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>day</sourceName>\n" +
"        <destinationName>day</destinationName>\n" +
"        <dataType>int</dataType>\n" +
"        <addAttributes>\n" +
"            <att name=\"extractRegex\">jplMURSST.{6}(..).{6}\\.png</att>\n" +
"            <att name=\"extractGroup\" type=\"int\">1</att>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    -->\n" +
"</dataset>\n\n\n";
        String results = generateDatasetsXml(tDir, tRegex, tRecursive, -1, 
            tInfoUrl, tInstitution, tSummary, tTitle, null) + "\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDTableFromFileNames",
            tDir, tRegex, "" + tRecursive, "-1", 
            tInfoUrl, tInstitution, tSummary, tTitle},
            false); //doIt loop?
        Test.ensureEqual(gdxResults, results, 
            "Unexpected results from GenerateDatasetsXml.doIt.");

        //ensure it is ready-to-use by making a dataset from it
        String2.log("results=\n" + results);
        EDD edd = oneFromXmlFragment(results);
        Test.ensureEqual(edd.datasetID(), tDatasetID, "");
        Test.ensureEqual(edd.title(), tTitle, "");
        Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
            "url, name, lastModified, size",
            "");

        String2.log("\n*** EDDTableFromFileNames.testGenerateDatasetsXmlAwsS3() finished successfully.");

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nUnexpected error.  (Did you create your AWS S3 credentials file?)"); 
        }
    }

    /**
     * Do tests of local file system.
     */
    public static void testLocal() throws Throwable {
        String2.log("\n*** EDDTableFromFileNames.testLocal\n");
        testVerboseOn();
        String dir = EDStatic.fullTestCacheDirectory;
        String results, expected, query, tName;

        EDDTable tedd = (EDDTable)oneFromDatasetXml("testFileNames");

        //.dds
        tName = tedd.makeNewFileForDapQuery(null, null, "", dir, 
            tedd.className() + "_all", ".dds"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    Float32 five;\n" +
"    String url;\n" +
"    String name;\n" +
"    Float64 time;\n" +
"    Int32 day;\n" +
"    Float64 lastModified;\n" +
"    Float64 size;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //.das
        tName = tedd.makeNewFileForDapQuery(null, null, "", dir, 
            tedd.className() + "_all", ".das"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"Attributes \\{\n" +
" s \\{\n" +
"  five \\{\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Five\";\n" +
"    String units \"m\";\n" +
"  \\}\n" +
"  url \\{\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"URL\";\n" +
"  \\}\n" +
"  name \\{\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"File Name\";\n" +
"  \\}\n" +
"  time \\{\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  \\}\n" +
"  day \\{\n" +
"    String ioos_category \"Time\";\n" +
"  \\}\n" +
"  lastModified \\{\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Last Modified\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  \\}\n" +
"  size \\{\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Size\";\n" +
"    String units \"bytes\";\n" +
"  \\}\n" +
" \\}\n" +
"  NC_GLOBAL \\{\n" +
"    String cdm_data_type \"Other\";\n" +
"    String history \".{19}Z \\(local files\\)\n" +
".{19}Z http://127.0.0.1:8080/cwexperimental/tabledap/testFileNames.das\";\n" +
"    String infoUrl \"http://www.pfeg.noaa.gov/\";\n" +
"    String institution \"NASA JPL\";\n" +
"    String keywords \"file, images, jpl, modified, mur, name, nasa, size, sst, time, URL\";\n" +
"    String sourceUrl \"\\(local files\\)\";\n" +
"    String summary \"Images from JPL MUR SST Daily.\";\n" +
"    String title \"JPL MUR SST Images\";\n" +
"  \\}\n" +
"\\}\n";
        Test.ensureLinesMatch(results, expected, "results=\n" + results);

        //get all as .csv
        tName = tedd.makeNewFileForDapQuery(null, null, "", dir, 
            tedd.className() + "_all", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"five,url,name,time,day,lastModified,size\n" +
"m,,,UTC,,UTC,bytes\n" +
"5.0,http://127.0.0.1:8080/cwexperimental/files/testFileNames/jplMURSST20150103090000.png,jplMURSST20150103090000.png,2015-01-03T09:00:00Z,3,2015-01-14T22:54:04Z,46482.0\n" +
"5.0,http://127.0.0.1:8080/cwexperimental/files/testFileNames/jplMURSST20150104090000.png,jplMURSST20150104090000.png,2015-01-04T09:00:00Z,4,2015-01-07T22:22:18Z,46586.0\n" +
"5.0,http://127.0.0.1:8080/cwexperimental/files/testFileNames/sub/jplMURSST20150105090000.png,jplMURSST20150105090000.png,2015-01-05T09:00:00Z,5,2015-01-07T22:21:44Z,46549.0\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test that min and max are being set by the constructor
        EDV edv = tedd.findVariableByDestinationName("time");
        Test.ensureEqual(edv.destinationMinString(), "2015-01-03T09:00:00Z", "min");
        Test.ensureEqual(edv.destinationMaxString(), "2015-01-05T09:00:00Z", "max");

        edv = tedd.findVariableByDestinationName("day");
        Test.ensureEqual(edv.destinationMin(), 3, "min");
        Test.ensureEqual(edv.destinationMax(), 5, "max");

        edv = tedd.findVariableByDestinationName("lastModified");
        Test.ensureEqual(edv.destinationMinString(), "2015-01-07T22:21:44Z", "min");
        Test.ensureEqual(edv.destinationMaxString(), "2015-01-14T22:54:04Z", "max");

        edv = tedd.findVariableByDestinationName("size");
        Test.ensureEqual(edv.destinationMin(), 46482, "min");
        Test.ensureEqual(edv.destinationMax(), 46586, "max");

        //a constraint on an extracted variable, and fewer results variables
        tName = tedd.makeNewFileForDapQuery(null, null, "name,day,size&day=4", dir, 
            tedd.className() + "_all", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"name,day,size\n" +
",,bytes\n" +
"jplMURSST20150104090000.png,4,46586.0\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        String2.log("\n EDDTableFromFileNames.testLocal finished successfully");
    }

    /**
     * Do tests of an Amazon AWS S3 file system.
     * Your S3 credentials must be in 
     * <br> ~/.aws/credentials on Linux, OS X, or Unix
     * <br> C:\Users\USERNAME\.aws\credentials on Windows
     * See http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/java-dg-setup.html .
     */
    public static void testAwsS3() throws Throwable {
        try {
        String2.log("\n*** EDDTableFromFileNames.testAwsS3\n");
        testVerboseOn();
        String dir = EDStatic.fullTestCacheDirectory;
        String results, expected, query, tName;

        EDDTable tedd = (EDDTable)oneFromDatasetXml("testFileNamesAwsS3");

        //.dds
        tName = tedd.makeNewFileForDapQuery(null, null, "", dir, 
            tedd.className() + "_all", ".dds"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    Float32 five;\n" +
"    String url;\n" +
"    String name;\n" +
"    Float64 startMonth;\n" +
"    Float64 endMonth;\n" +
"    Float64 lastModified;\n" +
"    Float64 size;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //.das
        tName = tedd.makeNewFileForDapQuery(null, null, "", dir, 
            tedd.className() + "_all", ".das"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"Attributes \\{\n" +
" s \\{\n" +
"  five \\{\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Five\";\n" +
"    String units \"m\";\n" +
"  \\}\n" +
"  url \\{\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"URL\";\n" +
"  \\}\n" +
"  name \\{\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"File Name\";\n" +
"  \\}\n" +
"  startMonth \\{\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Start Month\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  \\}\n" +
"  endMonth \\{\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"End Month\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  \\}\n" +
"  lastModified \\{\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Last Modified\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  \\}\n" +
"  size \\{\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Size\";\n" +
"    String units \"bytes\";\n" +
"  \\}\n" +
" \\}\n" +
"  NC_GLOBAL \\{\n" +
"    String cdm_data_type \"Other\";\n" +
"    String creator_name \"NASA Earth Exchange\";\n" +
"    String creator_url \"https://nex.nasa.gov/nex/\";\n" +
"    String history \".{19}Z \\(remote files\\)\n" +
".{19}Z http://127.0.0.1:8080/cwexperimental/tabledap/testFileNamesAwsS3.das\";\n" +
"    String infoUrl \"https://nex.nasa.gov/nex/\";\n" +
"    String institution \"NASA Earth Exchange\";\n" +
"    String keywords \"data, earth, exchange, file, great, identifier, lastModified, modified, name, nasa, size, time, title\";\n" +
"    String sourceUrl \"\\(remote files\\)\";\n" +
"    String summary \"File Names from http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/\";\n" +
"    String title \"File Names from Amazon AWS S3 NASA NEX tasmin Files\";\n" +
"  \\}\n" +
"\\}\n";
        Test.ensureLinesMatch(results, expected, "results=\n" + results);

        //get all as .csv
        tName = tedd.makeNewFileForDapQuery(null, null, "", dir, 
            tedd.className() + "_all", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"five,url,name,startMonth,endMonth,lastModified,size\n" +
"m,,,UTC,UTC,UTC,bytes\n" +
"5.0,http://127.0.0.1:8080/cwexperimental/files/testFileNamesAwsS3/tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_CESM1-CAM5_200601-201012.nc,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_CESM1-CAM5_200601-201012.nc,2006-01-01T00:00:00Z,2010-12-01T00:00:00Z,2013-10-25T20:46:53Z,1.372730447E9\n" +
"5.0,http://127.0.0.1:8080/cwexperimental/files/testFileNamesAwsS3/tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_CESM1-CAM5_201101-201512.nc,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_CESM1-CAM5_201101-201512.nc,2011-01-01T00:00:00Z,2015-12-01T00:00:00Z,2013-10-25T20:47:18Z,1.373728987E9\n" +
"5.0,http://127.0.0.1:8080/cwexperimental/files/testFileNamesAwsS3/tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_CESM1-CAM5_201601-202012.nc,tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_CESM1-CAM5_201601-202012.nc,2016-01-01T00:00:00Z,2020-12-01T00:00:00Z,2013-10-25T20:51:23Z,1.373747344E9\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

        //test that min and max are being set by the constructor
        EDV edv = tedd.findVariableByDestinationName("startMonth");
        Test.ensureEqual(edv.destinationMinString(), "2006-01-01T00:00:00Z", "min");
        Test.ensureEqual(edv.destinationMaxString(), "2096-01-01T00:00:00Z", "max");

        edv = tedd.findVariableByDestinationName("endMonth");
        Test.ensureEqual(edv.destinationMinString(), "2010-12-01T00:00:00Z", "min");
        Test.ensureEqual(edv.destinationMaxString(), "2099-12-01T00:00:00Z", "max");

        edv = tedd.findVariableByDestinationName("lastModified");
        Test.ensureEqual(edv.destinationMinString(), "2013-10-25T20:45:24Z", "min");
        Test.ensureEqual(edv.destinationMaxString(), "2013-10-25T20:54:20Z", "max");

        edv = tedd.findVariableByDestinationName("size");
        Test.ensureEqual(""+edv.destinationMin(), "1.098815646E9", "min"); //exact test
        Test.ensureEqual(""+edv.destinationMax(), "1.373941204E9", "max");

        //a constraint on an extracted variable, and fewer results variables
        tName = tedd.makeNewFileForDapQuery(null, null, "name,startMonth,size&size=1098815646", dir, 
            tedd.className() + "_subset", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        expected = 
"name,startMonth,size\n" +
",UTC,bytes\n" +
"tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_CESM1-CAM5_209601-209912.nc,2096-01-01T00:00:00Z,1.098815646E9\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        String2.log("\n EDDTableFromFileNames.testAwsS3 finished successfully");

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nUnexpected error.  (Did you create your AWS S3 credentials file?)"); 
        }

    }

     
    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {
        String2.log("\n****************** EDDTableFromFileNames.test() *****************\n");
        testVerboseOn();
/* */
        //always done        
        testGenerateDatasetsXml();
        testGenerateDatasetsXmlAwsS3();
        testLocal();
        testAwsS3();
    }

}
